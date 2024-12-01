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
import android.os.Bundle
import androidx.preference.Preference
import java.util.Locale
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions

class NonSlicePreferenceBuilder private constructor(className: String) {
    private val factory: KFunction<*>
    private val setters: Map<String, KFunction<*>>

    init {
        val cls = Class.forName(className).kotlin

        if (!cls.isSubclassOf(Preference::class)) {
            throw IllegalArgumentException("Not a preference")
        }

        var contextConstructor: KFunction<*>? = null
        for (cons in cls.constructors) {
            if (cons.parameters.size == 1 && cons.parameters[0].type.classifier == Context::class) {
                contextConstructor = cons
                break
            }
        }
        contextConstructor
            ?: throw IllegalArgumentException("Class doesn't have context constructor")

        factory = contextConstructor
        setters = mutableMapOf()
        for (function in cls.memberFunctions) {
            if (function.name.startsWith("set")
                && function.name.length > 3
                && function.parameters.size == 2
            ) {
                val property = function.name.substring(3..3).lowercase(Locale.US)
                    .plus(function.name.substring(4..<function.name.length))
                setters[property] = function
            }
        }
    }

    @Suppress("DEPRECATION") // Types can not be determined statically.
    fun create(context: Context, bundle: Bundle?): Preference {
        val preference: Preference = factory.call(context) as Preference
        bundle ?: return preference

        for (property in bundle.keySet()) {
            setters[property]?.call(preference, bundle[property])
        }

        return preference
    }

    companion object {
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