/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.twopanelsettings.slices

import android.annotation.SuppressLint
import android.app.slice.SliceItem.FORMAT_SLICE
import android.content.res.Configuration
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.android.tv.twopanelsettings.slices.compat.Slice
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createParentDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages access to cached slice data.
 */
class SliceCacheManager internal constructor(val context : Context) {
    suspend fun getCachedSlice(uri : Uri, configuration : Configuration) : Slice? {
        return withContext(Dispatchers.IO) {
            val providerInfo = context.packageManager.resolveContentProvider(uri.authority!!, 0)
                ?: return@withContext null
            val fileName = getFileName(uri, configuration)
            val path = FileSystems.getDefault().getPath(context.filesDir.path, SLICE_CACHE_DIR, fileName)
            val data : ByteArray
            try {
                data = path.toFile().readBytes()
            } catch (e : FileNotFoundException) {
                return@withContext null
            }
            val parcel = Parcel.obtain()
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            val bundle = Bundle.CREATOR.createFromParcel(parcel)
            parcel.recycle()

            if (bundle.getInt(SLICE_FORMAT_VERSION) != FORMAT_VERSION ||
                bundle.getString(SYSTEM_BUILD_FINGERPRINT) != Build.FINGERPRINT) {
                return@withContext null
            }

            val packageNames =
                bundle.getStringArray(SLICE_PACKAGE_NAMES) ?: return@withContext null
            val packageVersions =
                bundle.getLongArray(SLICE_PACKAGE_VERSIONS) ?: return@withContext null
            val packageUpdateTimes =
                bundle.getLongArray(SLICE_PACKAGE_UPDATE_TIMES) ?: return@withContext null

            if (packageNames.size != packageVersions.size ||
                packageNames.size != packageUpdateTimes.size ||
                context.packageName !in packageNames ||
                providerInfo.packageName !in packageNames) {
                return@withContext null
            }

            for (i in packageNames.indices) {
                val packageInfo = context.packageManager.getPackageInfo(packageNames[i], 0)
                if (packageInfo.longVersionCode != packageVersions[i] ||
                    getUpdateTime(packageInfo) != packageUpdateTimes[i]) {
                    return@withContext null
                }
            }

            return@withContext Slice(bundle)
        }
    }

    suspend fun saveCachedSlice(uri: Uri, configuration: Configuration, slice: Slice) {

        withContext(Dispatchers.IO) {
            val providerInfo = context.packageManager.resolveContentProvider(uri.authority!!, 0)
            if (providerInfo == null) {
                Log.e(TAG, "No provider for $uri")
                return@withContext
            }

            val bundle = slice.toBundle()
            bundle.putInt(SLICE_FORMAT_VERSION, FORMAT_VERSION)
            bundle.putString(SYSTEM_BUILD_FINGERPRINT, Build.FINGERPRINT)

            val packageNames = mutableSetOf(context.packageName, providerInfo.packageName)
            val authorities = mutableSetOf(uri.authority)

            for (item in slice.items.filter { it.format == FORMAT_SLICE} ) {
                val childAuthority = item.slice?.uri?.authority ?: continue
                if (childAuthority !in authorities) {
                    authorities.add(childAuthority)
                    context.packageManager.resolveContentProvider(childAuthority, 0)?.let {
                        packageNames.add(it.packageName)
                    }
                }
            }

            val packages = mutableListOf<String>()
            val versions = mutableListOf<Long>()
            val updateTimes = mutableListOf<Long>()

            for (packageName in packageNames) {
                val packageInfo =
                    try {
                        context.packageManager.getPackageInfo(packageName, 0)
                    } catch (_ : NameNotFoundException) {
                        continue
                    }
                packages.add(packageName)
                versions.add(packageInfo.longVersionCode)
                updateTimes.add(getUpdateTime(packageInfo))
            }

            bundle.putStringArray(SLICE_PACKAGE_NAMES, packages.toTypedArray())
            bundle.putLongArray(SLICE_PACKAGE_VERSIONS, versions.toLongArray())
            bundle.putLongArray(SLICE_PACKAGE_UPDATE_TIMES,
                updateTimes.toLongArray())

            val parcel = Parcel.obtain()
            bundle.writeToParcel(parcel, 0)
            val data = parcel.marshall()
            parcel.recycle()
            val fileName = getFileName(uri, configuration)
            val tempName = "${TEMP_PREFIX}_${sequence.getAndIncrement()}_$fileName"

            val path = FileSystems.getDefault().getPath(context.filesDir.path, SLICE_CACHE_DIR, fileName)
            val tempPath = FileSystems.getDefault().getPath(context.filesDir.path, SLICE_CACHE_DIR, tempName)
            tempPath.createParentDirectories()

            FileOutputStream(tempPath.toFile()).use {
                it.write(data)
                it.fd.sync()
            }

            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
        }

    }

    suspend fun clearCachedSlices() {
        withContext(Dispatchers.IO) {
            val cache = FileSystems.getDefault().getPath(context.filesDir.path, SLICE_CACHE_DIR)
            val files = cache.toFile().listFiles()?: return@withContext
            for (file in files) {
                file.delete()
            }
        }
    }

    @SuppressLint("StaticFieldLeak") // Uses application context.
    companion object {
        const val TAG = "SliceCacheManager"
        const val TEMP_PREFIX = "temp|_"
        const val SLICE_CACHE_DIR = "slicecache"
        const val SLICE_FORMAT_VERSION = "SLICE_FORMAT_VERSION"
        const val SLICE_PACKAGE_NAMES = "SLICE_PACKAGE_NAMES"
        const val SLICE_PACKAGE_VERSIONS = "SLICE_PACKAGE_VERSIONS"
        const val SLICE_PACKAGE_UPDATE_TIMES = "SLICE_PACKAGE_UPDATE_TIMES"
        const val SYSTEM_BUILD_FINGERPRINT = "SLICE_SYSTEM_BUILD_FINGERPRINT"

        const val FORMAT_VERSION = 1

        private var cacheManager : SliceCacheManager? = null
        private val sequence = AtomicInteger()

        fun getInstance(context : Context) : SliceCacheManager {
            var manager = cacheManager;
            if (manager == null) {
                manager = SliceCacheManager(context.applicationContext)
                cacheManager = manager
            }
            return manager
        }

        private fun getFileName(uri : Uri, configuration: Configuration) : String {
            return "${configuration.densityDpi}|_" +
                    "${configuration.getLocales().get(0).toLanguageTag()}|_" + encodeUri(uri);
        }

        /* Make URI safe to use as filename. */
        private fun encodeUri(uri: Uri) : String {
            val builder = StringBuilder()
            for (c in uri.toString()) {
                builder.append(when (c) {
                    '/' -> "|."
                    '|' -> "||"
                    else -> c.toString()
                })
            }
            return builder.toString()
        }

        private fun getUpdateTime(packageInfo: PackageInfo) : Long =
            maxOf(packageInfo.lastUpdateTime, packageInfo.firstInstallTime)
    }

}