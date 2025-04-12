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

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settingslib.RestrictedPreferenceHelperProvider
import java.lang.reflect.Constructor
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Locale

class NonSlicePreferenceBuilder private constructor(className: String) {
    private val cls : Class<*> = Class.forName(className)
    private val factory: Constructor<*>
    private val setters: MutableMap<String, MutableList<Method>> = mutableMapOf()

    init {
        if (!Preference::class.java.isAssignableFrom(cls)) {
            throw IllegalArgumentException("Not a preference")
        }

        factory = try {
            cls.getConstructor(Context::class.java)
        } catch (e: Exception) {
            cls.getConstructor(Context::class.java, AttributeSet::class.java)
        }
    }


    fun create(context: Context, bundle: Bundle?) : Preference {
        synchronized(this) {
            return createInternal(context, bundle)
        }
    }

    @Suppress("DEPRECATION") // Types can not be determined statically.
    private fun createInternal(context: Context, bundle: Bundle?): Preference {
        val preference: Preference =
            (if (factory.parameters.size == 1)
                factory.newInstance(context) else factory.newInstance(context, null)) as Preference
        bundle ?: return preference

        properties@ for (property in bundle.keySet()) {
            val value = bundle[property]!!

            if (preference is ListPreference && property == DEFAULT_VALUE_PROPERTY &&
                value !is CharSequence) {
                preference.setDefaultValue(value.toString())
                continue
            } else if (property == IS_PREFERENCE_VISIBLE_PROPERTY && value is Boolean) {
                preference.isVisible = value
                continue
            } else if (preference is RestrictedPreferenceHelperProvider) {
                if (property == USER_RESTRICTION_PROPERTY && value is String) {
                    preference.getRestrictedPreferenceHelper().setUserRestriction(value)
                    continue
                } else if (property == USE_ADMIN_DISABLED_SUMMARY_PROPERTY && value is Boolean) {
                    preference.getRestrictedPreferenceHelper().useAdminDisabledSummary(value)
                    continue
                }
            }

            val setterList = setters[property] ?: mutableListOf()
            for (setter in setterList) {
                try {
                    setter.invoke(preference, value)
                    continue@properties;
                } catch (_: Exception) {
                }
            }

            val setterName = "set" + property.substring(0..<1).uppercase(Locale.US) +
                    property.substring(1)
            val setter = findSetter(setterName, value::class.java)
            if (setter != null) {
                setter.invoke(preference, value)
                setterList += setter
                setters[property] = setterList
            } else {
                Log.e(
                    TAG,
                    "Can't find $setterName in ${cls.name} of type ${value::class.java.name}"
                );
            }
        }

        return preference
    }

    private fun findSetter(name: String, type: Class<*>) : Method? {
        try {
            return cls.getMethod(name, type)
        } catch (_: Exception) {}

        if (type.isArray) {
            val componentType = type.componentType
            val baseSuperClass = componentType.superclass
            if (baseSuperClass != null) {
                findSetter(name, Array.newInstance(baseSuperClass, 0)::class.java)?.let { return it }
            }

            for (baseInterface in componentType.interfaces) {
                findSetter(name, Array.newInstance(baseInterface, 0)::class.java)?.let { return it }
            }

            return null
        }

        try {
            val primitiveField = type.getField("TYPE")
            val primitiveType = primitiveField.get(null)
            if (primitiveType is Class<*>) {
                findSetter(name, primitiveType)?.let { return it }
            }
        } catch(_: Exception) {}

        val superclass = type.superclass
        if (superclass != null) {
            findSetter(name, superclass)?.let { return it }
        }

        for (iface in type.interfaces) {
            findSetter(name, iface)?.let { return it }
        }

        return null
    }

    companion object {
        private const val TAG = "NonSlicePreferenceBld"

        private const val DEFAULT_VALUE_PROPERTY = "defaultValue"
        private const val IS_PREFERENCE_VISIBLE_PROPERTY = "isPreferenceVisible"
        private const val USER_RESTRICTION_PROPERTY = "userRestriction"
        private const val USE_ADMIN_DISABLED_SUMMARY_PROPERTY = "useAdminDisabledSummary"

        private val builders: MutableMap<String, NonSlicePreferenceBuilder> = mutableMapOf()

        fun forClassName(className: String): NonSlicePreferenceBuilder {
            synchronized(builders) {
                var builder = builders[className]
                if (builder == null) {
                    builder = NonSlicePreferenceBuilder(className)
                    builders[className] = builder
                }
                return builder
            }
        }
    }
}
