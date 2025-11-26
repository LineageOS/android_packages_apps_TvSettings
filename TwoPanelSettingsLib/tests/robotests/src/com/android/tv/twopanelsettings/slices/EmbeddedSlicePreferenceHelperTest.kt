/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.tv.twopanelsettings.slices.EmbeddedSlicePreferenceHelper.SlicePreferenceListener
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmbeddedSlicePreferenceHelperTest {
    private lateinit var context: Context
    private lateinit var preference: Preference
    private lateinit var helper: EmbeddedSlicePreferenceHelper
    private val testUri = "content://test/slice"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = Preference(context)
        helper = EmbeddedSlicePreferenceHelper(preference, testUri)
    }

    @Test
    fun testOnChanged_nullSlice_hidesPreference() {
        helper.onChanged(null)
        assertFalse(preference.isVisible)
    }

    @Test
    fun testOnChanged_nullSlice_notifiesListener() {
        val listener = mock(SlicePreferenceListener::class.java)
        helper.mListener = listener
        helper.onChanged(null)
        verify(listener).onChangeVisibility()
    }
}
