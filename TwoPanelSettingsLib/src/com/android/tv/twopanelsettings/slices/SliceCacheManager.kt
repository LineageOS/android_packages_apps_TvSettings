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
import android.content.res.Configuration
import android.content.Context
import android.content.pm.PackageInfo
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
        val providerInfo = context.packageManager.resolveContentProvider(uri.authority!!, 0)
            ?: return null
        return withContext(Dispatchers.IO) {
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
                bundle.getString(SLICE_PACKAGE_NAME) != providerInfo.packageName ||
                bundle.getString(SYSTEM_BUILD_FINGERPRINT) != Build.FINGERPRINT) {
                return@withContext null
            }

            val packageInfo = context.packageManager.getPackageInfo(providerInfo.packageName, 0)
            if (bundle.getLong(SLICE_PACKAGE_VERSION) != packageInfo.longVersionCode ||
                        bundle.getLong(SLICE_PACKAGE_UPDATE_TIME) != getUpdateTime(packageInfo)) {
                return@withContext null
            }

            val hostInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (bundle.getLong(HOST_PACKAGE_VERSION) != hostInfo.longVersionCode ||
                    bundle.getLong(HOST_PACKAGE_UPDATE_TIME) != getUpdateTime(hostInfo)) {
                return@withContext null
            }

            return@withContext Slice(bundle)
        }
    }

    suspend fun saveCachedSlice(uri: Uri, configuration: Configuration, slice: Slice) {
        val providerInfo = context.packageManager.resolveContentProvider(uri.authority!!, 0)
        if (providerInfo == null) {
            Log.e(TAG, "No provider for $uri")
            return
        }

        withContext(Dispatchers.IO) {
            val bundle = slice.toBundle()
            bundle.putInt(SLICE_FORMAT_VERSION, FORMAT_VERSION)
            bundle.putString(SLICE_PACKAGE_NAME, providerInfo.packageName)
            bundle.putString(SYSTEM_BUILD_FINGERPRINT, Build.FINGERPRINT)
            val packageInfo = context.packageManager.getPackageInfo(providerInfo.packageName, 0)
            bundle.putLong(SLICE_PACKAGE_VERSION, packageInfo.longVersionCode)
            bundle.putLong(SLICE_PACKAGE_UPDATE_TIME, getUpdateTime(packageInfo))
            val hostInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            bundle.putLong(HOST_PACKAGE_VERSION, hostInfo.longVersionCode)
            bundle.putLong(HOST_PACKAGE_UPDATE_TIME, getUpdateTime(hostInfo))
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
        const val SLICE_PACKAGE_NAME = "SLICE_PACKAGE_NAME"
        const val SLICE_PACKAGE_VERSION = "SLICE_PACKAGE_VERSION"
        const val SLICE_PACKAGE_UPDATE_TIME = "SLICE_PACKAGE_UPDATE_TIME"
        const val HOST_PACKAGE_VERSION = "SLICE_HOST_PACKAGE_VERSION"
        const val HOST_PACKAGE_UPDATE_TIME = "SLICE_HOST_PACKAGE_UPDATE_TIME"
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