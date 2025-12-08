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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmbeddedSlicePreferenceTest {
    private lateinit var context: Context
    private lateinit var preference: EmbeddedSlicePreference
    private val testUri = "content://test/slice"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = EmbeddedSlicePreference(context, testUri)
    }

    @Test
    fun testConstructor_createsHelper() {
        assertNotNull(preference.mHelper)
    }

    @Test
    fun testSetUri_updatesHelper() {
        val newUri = "content://test/new_slice"
        preference.setUri(newUri)
        assertEquals(newUri, preference.getUri())
        assertNotNull(preference.mHelper)
        // Since we can't easily check the URI inside the helper without more reflection,
        // we rely on the fact that a new helper is created or the old one is detached.
    }

    @Test
    fun testSetUri_null_clearsHelper() {
        preference.setUri(null)
        assertNull(preference.getUri())
        assertNull(preference.mHelper)
    }

    @Test
    fun testUpdate_updatesProperties() {
        val helper = preference.mHelper!!
        val newPref = Preference(context)
        newPref.title = "New Title"
        newPref.summary = "New Summary"
        newPref.isEnabled = false

        helper.mNewPref = newPref

        preference.update()

        assertEquals("New Title", preference.title)
        assertEquals("New Summary", preference.summary)
        assertEquals(false, preference.isEnabled)
    }

    @Test
    fun testAddListener_setsListenerOnHelper() {
        val listener = mock(SlicePreferenceListener::class.java)
        preference.addListener(listener)
        val helper = preference.mHelper!!
        assertEquals(listener, helper.mListener)
    }

    @Test
    fun testRemoveListener_clearsListenerOnHelper() {
        val listener = mock(SlicePreferenceListener::class.java)
        preference.addListener(listener)
        preference.removeListener(listener)
        val helper = preference.mHelper!!
        assertNull(helper.mListener)
    }
}