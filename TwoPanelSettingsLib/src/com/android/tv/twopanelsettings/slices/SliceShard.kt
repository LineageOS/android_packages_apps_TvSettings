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

import android.app.Activity
import android.app.PendingIntent
import android.app.slice.Slice.HINT_PARTIAL
import android.app.tvsettings.TvSettingsEnums
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment
import com.android.tv.twopanelsettings.slices.compat.Slice
import com.android.tv.twopanelsettings.slices.compat.SliceItem
import com.android.tv.twopanelsettings.slices.compat.SliceViewManager
import com.android.tv.twopanelsettings.slices.compat.widget.ListContent
import com.android.tv.twopanelsettings.slices.compat.widget.SliceContent
import java.util.IdentityHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides functionality for a fragment to display slice data.
 */
class SliceShard(
    private val mFragment: LeanbackPreferenceFragmentCompat, uriString: String?,
    callbacks: Callbacks, initialTitle: CharSequence, prefContext: Context,
    private val supportedKeys : Set<String> = setOf(),
    private val isCached: Boolean = false
) {
    private val mCallbacks: Callbacks
    private val mInitialTitle: CharSequence

    private var mUriString: String? = null
    private var mSlice: Slice? = null
    private val mPrefContext: Context
    private val mSliceCacheManager = SliceCacheManager.getInstance(prefContext)
    var screenTitle: CharSequence?
        private set
    private var mScreenSubtitle: CharSequence? = null
    private var mScreenIcon: Icon? = null
    private var mPreferenceFollowupIntent: Parcelable? = null
    private var mFollowupPendingIntentResultCode: Int = 0
    private var mFollowupPendingIntentExtras: Intent? = null
    private var mFollowupPendingIntentExtrasCopy: Intent? = null
    private var mLastFocusedPreferenceKey: String? = null
    private var mIsMainPanelReady: Boolean = true
    private var mCurrentPageId: Int = 0

    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mActivityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val mActivityResultLauncherIntent: ActivityResultLauncher<Intent>
    private val mActivityResultLauncherIntentFollowup: ActivityResultLauncher<Intent>
    private val mSliceObserver: Observer<Slice>
    private val mContentObserver: ContentObserver = object : ContentObserver(mHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            handleUri(uri!!)
        }
    }

    init {
        setUri(uriString)
        mCallbacks = callbacks
        mInitialTitle = initialTitle
        mActivityResultLauncher =
            mFragment.registerForActivityResult<IntentSenderRequest, ActivityResult>(
                ActivityResultContracts.StartIntentSenderForResult(),
                { result: ActivityResult -> this.processActionResult(result) })
        mActivityResultLauncherIntent = mFragment.registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult(),
            { result: ActivityResult -> this.processActionResult(result) })
        mActivityResultLauncherIntentFollowup =
            mFragment.registerForActivityResult<Intent, ActivityResult>(
                ActivityResultContracts.StartActivityForResult(),
                { result: ActivityResult? -> })
        screenTitle = initialTitle
        mPrefContext = prefContext

        mFragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                pause()
            }
        })

        if (isCached) {
            mFragment.viewLifecycleOwner.lifecycleScope.launch {
                mFragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mCallbacks.showProgressBar(true)
                    val slice = try {
                        loadCachedSlice(mFragment.resources.configuration)
                    } catch (e : Exception) {
                        Log.e(TAG, "Unable to load $mUriString", e)
                        null
                    }
                    if (slice != null) {
                        mIsMainPanelReady = false
                        mSlice = slice
                        update()
                    } else {
                        mCallbacks.showProgressBar(false)
                        mCallbacks.onSlice(null)
                    }
                }
            }
        }

        mSliceObserver = Observer { slice: Slice? ->
            mSlice = slice
            // Make TvSettings guard against the case that slice provider is not set up correctly
            if (slice == null || slice.hints == null) {
                return@Observer
            }

            if (slice.hints.contains(HINT_PARTIAL)) {
                mCallbacks.showProgressBar(true)
            } else {
                mCallbacks.showProgressBar(false)
            }
            mIsMainPanelReady = false
            update()
        }
    }

    private fun resume() {
        if (TextUtils.isEmpty(screenTitle)) {
            screenTitle = mInitialTitle
        }

        mCallbacks.setTitle(screenTitle)
        mCallbacks.setSubtitle(mScreenSubtitle)
        mCallbacks.setIcon(if (mScreenIcon != null) mScreenIcon!!.loadDrawable(mPrefContext) else null)

        if (!isCached && !TextUtils.isEmpty(mUriString)) {
            mCallbacks.showProgressBar(true)
            sliceLiveData.observeForever(mSliceObserver)
            mFragment.requireContext().contentResolver.registerContentObserver(
                SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver
            )
        }
        fireFollowupPendingIntent()
    }

    private fun pause() {
        mCallbacks.showProgressBar(false)
        requireContext().contentResolver.unregisterContentObserver(mContentObserver)
        sliceLiveData.removeObserver(mSliceObserver)
    }

    private suspend fun loadCachedSlice(configuration: Configuration) : Slice? {
        val uri = Uri.parse(mUriString)
        val cachedSlice = mSliceCacheManager.getCachedSlice(uri, configuration)
        if (cachedSlice != null) {
            return cachedSlice
        }

        mCallbacks.showProgressBar(false) // Show fallback while loading slice.

        return withContext(Dispatchers.IO) {
            val viewManager = SliceViewManager.getInstance(mPrefContext)
            val slice = viewManager.bindSlice(uri)
            if (slice != null && !slice.hints.contains(HINT_PARTIAL)) {
                mSliceCacheManager.saveCachedSlice(uri, configuration, slice)
                return@withContext slice;
            }
            return@withContext null
        }
    }

    private val sliceLiveData: PreferenceSliceLiveData.SliceLiveDataImpl
        get() = ContextSingleton.getInstance()
            .getSliceLiveData(mFragment.requireActivity(), Uri.parse(mUriString))

    private fun processActionResult(result: ActivityResult) {
        val data: Intent? = result.data
        mFollowupPendingIntentExtras = data
        mFollowupPendingIntentExtrasCopy = if (data == null) null else Intent(
            data
        )
        mFollowupPendingIntentResultCode = result.resultCode
    }

    private fun fireFollowupPendingIntent() {
        if (mFollowupPendingIntentExtras == null) {
            return
        }
        // If there is followup pendingIntent returned from initial activity, send it.
        // Otherwise send the followup pendingIntent provided by slice api.
        var followupPendingIntent: Parcelable?
        try {
            followupPendingIntent = mFollowupPendingIntentExtrasCopy!!.getParcelableExtra(
                SlicesConstants.EXTRA_SLICE_FOLLOWUP
            )
        } catch (ex: Throwable) {
            // unable to parse, the Intent has custom Parcelable, fallback
            followupPendingIntent = null
        }
        if (followupPendingIntent is PendingIntent) {
            try {
                followupPendingIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e)
            }
        } else {
            if (mPreferenceFollowupIntent == null) {
                return
            }
            if (mPreferenceFollowupIntent is Intent) {
                val filledIn: Intent = Intent(mPreferenceFollowupIntent as Intent)
                filledIn.fillIn(mFollowupPendingIntentExtras!!, 0)
                if (requireContext().packageManager.resolveActivity(filledIn, 0) != null) {
                    mActivityResultLauncherIntentFollowup.launch(filledIn)
                } else {
                    requireContext().sendBroadcast(filledIn)
                }
            } else {
                try {
                    (mPreferenceFollowupIntent as PendingIntent).send(
                        requireContext(),
                        mFollowupPendingIntentResultCode, mFollowupPendingIntentExtras
                    )
                } catch (e: PendingIntent.CanceledException) {
                    Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e)
                }
            }
            mPreferenceFollowupIntent = null
        }
    }

    private fun requireContext(): Context {
        return mFragment.requireContext()
    }

    private fun isUriValid(uri: String?): Boolean {
        if (uri == null) {
            return false
        }
        val client: ContentProviderClient? =
            requireContext().contentResolver.acquireContentProviderClient(Uri.parse(uri))
        if (client != null) {
            client.close()
            return true
        } else {
            return false
        }
    }

    private fun update() {
        val listContent: ListContent = ListContent(
            mSlice!!
        )
        val preferenceScreen: PreferenceScreen? =
            mFragment.preferenceManager.preferenceScreen

        if (preferenceScreen == null) {
            return
        }

        val items: List<SliceContent> = listContent.rowItems
        if (items == null || items.isEmpty()) {
            return
        }

        val redirectSliceItem: SliceItem? = SlicePreferencesUtil.getRedirectSlice(items)
        var redirectSlice: String? = null
        if (redirectSliceItem != null) {
            val data: SlicePreferencesUtil.Data = SlicePreferencesUtil.extract(redirectSliceItem)
            val title: CharSequence = SlicePreferencesUtil.getText(data.mTitleItem)
            if (!TextUtils.isEmpty(title)) {
                redirectSlice = title.toString()
            }
        }
        if (isUriValid(redirectSlice)) {
            sliceLiveData.removeObserver(mSliceObserver)
            requireContext().contentResolver.unregisterContentObserver(mContentObserver)
            setUri(redirectSlice)
            sliceLiveData.observeForever(mSliceObserver)
            requireContext().contentResolver.registerContentObserver(
                SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver
            )
        }

        val screenTitleItem: SliceItem? = SlicePreferencesUtil.getScreenTitleItem(items)
        if (screenTitleItem == null) {
            mCallbacks.setTitle(screenTitle)
        } else {
            val data: SlicePreferencesUtil.Data = SlicePreferencesUtil.extract(screenTitleItem)
            mCurrentPageId = SlicePreferencesUtil.getPageId(screenTitleItem)
            val title: CharSequence = SlicePreferencesUtil.getText(data.mTitleItem)
            if (!TextUtils.isEmpty(title)) {
                mCallbacks.setTitle(title)
                screenTitle = title
            } else {
                mCallbacks.setTitle(screenTitle)
            }

            val subtitle: CharSequence = SlicePreferencesUtil.getText(data.mSubtitleItem)
            mScreenSubtitle = subtitle
            mCallbacks.setSubtitle(subtitle)

            val icon: Icon? = SlicePreferencesUtil.getIcon(data.mStartItem)
            mScreenIcon = icon
            mCallbacks.setIcon(if (icon != null) icon.loadDrawable(mPrefContext) else null)
        }

        val focusedPrefItem: SliceItem? = SlicePreferencesUtil.getFocusedPreferenceItem(items)
        var defaultFocusedKey: CharSequence? = null
        if (focusedPrefItem != null) {
            val data: SlicePreferencesUtil.Data = SlicePreferencesUtil.extract(focusedPrefItem)
            val title: CharSequence = SlicePreferencesUtil.getText(data.mTitleItem)
            if (!TextUtils.isEmpty(title)) {
                defaultFocusedKey = title
            }
        }

        val newPrefs: MutableList<Preference> = ArrayList()
        for (contentItem: SliceContent in items) {
            val item: SliceItem? = contentItem.sliceItem
            if (SlicesConstants.TYPE_PREFERENCE == item!!.subType
                || SlicesConstants.TYPE_PREFERENCE_CATEGORY == item.subType
                || SlicesConstants.TYPE_PREFERENCE_EMBEDDED_PLACEHOLDER == item.subType
            ) {
                val preference: Preference? =
                    SlicePreferencesUtil.getPreference(
                        item, mPrefContext, SliceFragment::class.java.canonicalName,
                        isTwoPanel,
                        mFragment.preferenceScreen
                    )
                if (preference != null) {
                    newPrefs.add(preference)
                }
            }
        }
        updatePreferenceGroup(preferenceScreen, newPrefs)

        removeAnimationClipping(mFragment.view)

        if (defaultFocusedKey != null) {
            mFragment.scrollToPreference(defaultFocusedKey.toString())
        } else if (mLastFocusedPreferenceKey != null) {
            mFragment.scrollToPreference(mLastFocusedPreferenceKey!!)
        }

        if (isTwoPanel) {
            (mFragment.parentFragment as TwoPanelSettingsFragment).refocusPreference(mFragment)
        }
        mIsMainPanelReady = true
        mCallbacks.onSlice(mSlice)
    }

    private fun isPreferenceSupported(preference : Preference) : Boolean {
        return preference is InfoPreference || preference is HasSliceAction
                || (preference is PreferenceCategory && preference.preferenceCount > 0)
                || (preference.key != null && supportedKeys.contains(preference.key))
    }

    private fun updatePreferenceGroup(group: PreferenceGroup, newPrefs: List<Preference>) {
        // Remove all the preferences in the screen that satisfy such three cases:
        // (a) Preference without key
        // (b) Preference with key which does not appear in the new list.
        // (c) Preference with key which does appear in the new list, but the preference has changed
        // ability to handle slices and needs to be replaced instead of re-used.
        var index: Int = 0
        val newToOld: IdentityHashMap<Preference, Preference> = IdentityHashMap()
        while (index < group.preferenceCount) {
            var needToRemoveCurrentPref: Boolean = true
            val oldPref: Preference = group.getPreference(index)
            for (newPref: Preference in newPrefs) {
                if (isSamePreference(oldPref, newPref)) {
                    needToRemoveCurrentPref = false
                    newToOld[newPref] = oldPref
                    break
                }
            }

            if (needToRemoveCurrentPref) {
                group.removePreference(oldPref)
            } else {
                index++
            }
        }

        val twoStatePreferenceIsCheckedByOrder: MutableMap<Int, Boolean?> = HashMap()
        for (i in newPrefs.indices) {
            if (newPrefs.get(i) is TwoStatePreference) {
                twoStatePreferenceIsCheckedByOrder[i] =
                    (newPrefs.get(i) as TwoStatePreference).isChecked
            }
        }

        //Iterate the new preferences list and give each preference a correct order
        for (i in newPrefs.indices) {
            val newPref: Preference = newPrefs[i]
            if (!isPreferenceSupported(newPref)) {
                continue
            }

            // If the newPref has a key and has a corresponding old preference, update the old
            // preference and give it a new order.
            val oldPref: Preference? = newToOld[newPref]
            if (oldPref == null) {
                newPref.order = i
                group.addPreference(newPref)
                continue
            }

            oldPref.order = i
            if (oldPref is EmbeddedSlicePreference) {
                // EmbeddedSlicePreference has its own slice observer
                // (EmbeddedSlicePreferenceHelper). Should therefore not be updated by
                // slice observer in SliceFragment.
                // The order will however still need to be updated, as this can not be handled
                // by EmbeddedSlicePreferenceHelper.
                continue
            }

            oldPref.icon = newPref.icon
            oldPref.title = newPref.title
            oldPref.summary = newPref.summary
            oldPref.isEnabled = newPref.isEnabled
            oldPref.isSelectable = newPref.isSelectable
            oldPref.fragment = newPref.fragment
            oldPref.extras.putAll(newPref.extras)
            if ((oldPref is HasSliceAction)
                && (newPref is HasSliceAction)
            ) {
                (oldPref as HasSliceAction).sliceAction = (newPref as HasSliceAction).sliceAction
            }
            if ((oldPref is HasSliceUri)
                && (newPref is HasSliceUri)
            ) {
                (oldPref as HasSliceUri).uri = (newPref as HasSliceUri).uri
            }
            if ((oldPref is HasCustomContentDescription)
                && (newPref is HasCustomContentDescription)
            ) {
                (oldPref as HasCustomContentDescription).contentDescription =
                    (newPref as HasCustomContentDescription)
                        .contentDescription
            }

            if (oldPref is PreferenceGroup && newPref is PreferenceGroup) {
                val newGroup: PreferenceGroup = newPref
                val newChildren: MutableList<Preference> = ArrayList()
                for (j in 0 until newGroup.preferenceCount) {
                    newChildren.add(newGroup.getPreference(j))
                }
                newGroup.removeAll()
                updatePreferenceGroup(oldPref, newChildren)
            }
        }

        //addPreference will reset the checked status of TwoStatePreference.
        //So we need to add them back
        for (i in 0 until group.preferenceCount) {
            val screenPref: Preference = group.getPreference(i)
            if (screenPref is TwoStatePreference
                && twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder()) != null
            ) {
                screenPref.isChecked =
                    twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder())!!
            }
        }
    }

    fun onPreferenceFocused(preference: Preference) {
        mLastFocusedPreferenceKey = preference.key
    }

    fun onSeekbarPreferenceChanged(preference: SliceSeekbarPreference, addValue: Int) {
        val curValue: Int = preference.value
        if ((addValue > 0 && curValue < preference.max) ||
            (addValue < 0 && curValue > preference.min)
        ) {
            preference.value = curValue + addValue

            try {
                val fillInIntent: Intent =
                    Intent()
                        .putExtra(SlicesConstants.EXTRA_PREFERENCE_KEY, preference.key)
                firePendingIntent(preference, fillInIntent)
            } catch (e: Exception) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e)
            }
        }
    }

    fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference is SliceRadioPreference) {
            val radioPref: SliceRadioPreference = preference
            if (!radioPref.isChecked) {
                radioPref.isChecked = true
                if (TextUtils.isEmpty(radioPref.uri)) {
                    return true
                }
            }

            InstrumentationUtils.logEntrySelected(getPreferenceActionId(preference))
            val fillInIntent: Intent =
                Intent().putExtra(SlicesConstants.EXTRA_PREFERENCE_KEY, preference.getKey())

            val result: Boolean = firePendingIntent(radioPref, fillInIntent)
            radioPref.clearOtherRadioPreferences(mFragment.preferenceScreen)
            if (result) {
                return true
            }
        } else if (preference is TwoStatePreference
            && preference is HasSliceAction
        ) {
            val isChecked: Boolean = (preference as TwoStatePreference).isChecked
            preference.getExtras()
                .putBoolean(SlicesConstants.EXTRA_PREFERENCE_INFO_STATUS, isChecked)
            if (isTwoPanel) {
                (mFragment.parentFragment as TwoPanelSettingsFragment).refocusPreference(
                    mFragment
                )
            }
            InstrumentationUtils.logToggleInteracted(getPreferenceActionId(preference), isChecked)
            val fillInIntent: Intent =
                Intent()
                    .putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, isChecked)
                    .putExtra(SlicesConstants.EXTRA_PREFERENCE_KEY, preference.getKey())
            if (firePendingIntent(preference as HasSliceAction, fillInIntent)) {
                return true
            }
            return true
        } else if (preference is SlicePreference) {
            // In this case, we may intentionally ignore this entry selection to avoid double
            // logging as the action should result in a PAGE_FOCUSED event being logged.
            if (getPreferenceActionId(preference) != TvSettingsEnums.ENTRY_DEFAULT) {
                InstrumentationUtils.logEntrySelected(getPreferenceActionId(preference))
            }
            val fillInIntent: Intent =
                Intent().putExtra(SlicesConstants.EXTRA_PREFERENCE_KEY, preference.getKey())
            if (firePendingIntent(preference as HasSliceAction, fillInIntent)) {
                return true
            }
        }

        return false
    }

    private fun removeAnimationClipping(v: View) {
        if (v is ViewGroup) {
            v.clipChildren = false
            v.clipToPadding = false
            for (index in 0 until v.childCount) {
                val child: View = v.getChildAt(index)
                removeAnimationClipping(child)
            }
        }
    }

    private val isTwoPanel: Boolean
        get() {
            return mFragment.parentFragment is TwoPanelSettingsFragment
        }

    private fun firePendingIntent(preference: HasSliceAction, fillInIntent: Intent?): Boolean {
        if (preference.sliceAction == null) {
            return false
        }

        val intent: Intent? = preference.sliceAction.actionIntent
        if (intent != null) {
            val filledIn: Intent = Intent(intent)
            if (fillInIntent != null) {
                filledIn.fillIn(fillInIntent, 0)
            }

            if (requireContext().packageManager.resolveActivity(filledIn, 0) != null) {
                mActivityResultLauncherIntent.launch(filledIn)
            } else {
                requireContext().sendBroadcast(intent)
            }
        } else {
            val intentSender: IntentSender = preference.sliceAction.action!!
                .intentSender
            mActivityResultLauncher.launch(
                IntentSenderRequest.Builder(intentSender).setFillInIntent(
                    fillInIntent
                ).build()
            )
        }
        if (preference.followupSliceAction != null) {
            mPreferenceFollowupIntent = preference.followupSliceAction.action
            if (mPreferenceFollowupIntent == null) {
                mPreferenceFollowupIntent = preference.followupSliceAction.actionIntent
            }
        }

        return true
    }

    private fun back() {
        if (isTwoPanel) {
            val parentFragment: TwoPanelSettingsFragment? =
                mFragment.callbackFragment as TwoPanelSettingsFragment?
            if (parentFragment!!.isFragmentInTheMainPanel(mFragment)) {
                parentFragment.navigateBack()
            }
        } else if (mFragment.callbackFragment is OnePanelSliceFragmentContainer) {
            (mFragment.callbackFragment as OnePanelSliceFragmentContainer).navigateBack()
        }
    }

    private fun forward() {
        if (mIsMainPanelReady) {
            if (isTwoPanel) {
                val parentFragment: TwoPanelSettingsFragment? =
                    mFragment.callbackFragment as TwoPanelSettingsFragment?
                var chosenPreference: Preference? = TwoPanelSettingsFragment.getChosenPreference(
                    mFragment
                )
                if (chosenPreference == null && mLastFocusedPreferenceKey != null) {
                    chosenPreference = mFragment.findPreference(
                        mLastFocusedPreferenceKey!!
                    )
                }
                if (chosenPreference != null && chosenPreference is HasSliceUri
                    && (chosenPreference as HasSliceUri).uri != null
                ) {
                    chosenPreference.fragment = SliceFragment::class.java.canonicalName
                    parentFragment!!.refocusPreferenceForceRefresh(chosenPreference, mFragment)
                }
                if (parentFragment!!.isFragmentInTheMainPanel(mFragment)) {
                    parentFragment.navigateToPreviewFragment()
                }
            }
        } else {
            mHandler.post({ this.forward() })
        }
    }


    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT, mPreferenceFollowupIntent)
        outState.putInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE, mFollowupPendingIntentResultCode)
        outState.putCharSequence(
            KEY_SCREEN_TITLE,
            screenTitle
        )
        outState.putCharSequence(KEY_SCREEN_SUBTITLE, mScreenSubtitle)
        outState.putParcelable(KEY_SCREEN_ICON, mScreenIcon)
        outState.putString(KEY_LAST_PREFERENCE, mLastFocusedPreferenceKey)
        outState.putString(KEY_URI_STRING, mUriString)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mPreferenceFollowupIntent =
            savedInstanceState.getParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT)
        mFollowupPendingIntentResultCode =
            savedInstanceState.getInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE)
        screenTitle = savedInstanceState.getCharSequence(KEY_SCREEN_TITLE)
        mScreenSubtitle = savedInstanceState.getCharSequence(KEY_SCREEN_SUBTITLE)
        mScreenIcon = savedInstanceState.getParcelable(KEY_SCREEN_ICON)
        mLastFocusedPreferenceKey = savedInstanceState.getString(KEY_LAST_PREFERENCE)
        setUri(savedInstanceState.getString(KEY_URI_STRING))
    }

    private fun handleUri(uri: Uri) {
        val uriString: String? = uri.getQueryParameter(SlicesConstants.PARAMETER_URI)
        val errorMessage: String? = uri.getQueryParameter(SlicesConstants.PARAMETER_ERROR)
        // Display the errorMessage based upon two different scenarios:
        // a) If the provided uri string matches with current page slice uri(usually happens
        // when the data fails to correctly load), show the errors in the current panel using
        // InfoFragment UI.
        // b) If the provided uri string does not match with current page slice uri(usually happens
        // when the data fails to save), show the error message as the toast.
        if (uriString != null && errorMessage != null) {
            if (uriString != mUriString) {
                showErrorMessageAsToast(errorMessage)
            } else {
                showErrorMessage(errorMessage)
            }
        }
        // Provider should provide the correct slice uri in the parameter if it wants to do certain
        // action(includes go back, forward), otherwise TvSettings would ignore it.
        if (uriString == null || uriString != mUriString) {
            return
        }
        val direction: String? = uri.getQueryParameter(SlicesConstants.PARAMETER_DIRECTION)
        if (direction != null) {
            if (direction == SlicesConstants.FORWARD) {
                forward()
            } else if (direction == SlicesConstants.BACKWARD) {
                back()
            } else if (direction == SlicesConstants.EXIT) {
                mFragment.requireActivity().setResult(Activity.RESULT_OK)
                mFragment.requireActivity().finish()
            }
        }
    }

    private fun showErrorMessage(errorMessage: String) {
        if (isTwoPanel) {
            (mFragment.callbackFragment as TwoPanelSettingsFragment).showErrorMessage(
                errorMessage, mFragment
            )
        }
    }

    private fun showErrorMessageAsToast(errorMessage: String) {
        Toast.makeText(mFragment.requireActivity(), errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun getPreferenceActionId(preference: Preference): Int {
        if (preference is HasSliceAction) {
            return if ((preference as HasSliceAction).actionId != 0)
                (preference as HasSliceAction).actionId
            else
                TvSettingsEnums.ENTRY_DEFAULT
        }
        return TvSettingsEnums.ENTRY_DEFAULT
    }

    private fun setUri(uriString: String?) {
        mUriString = uriString
        if (!TextUtils.isEmpty(uriString)) {
            ContextSingleton.getInstance().grantFullAccess(
                mFragment.requireContext(),
                Uri.parse(uriString)
            )
        }
    }

    val pageId: Int
        get() {
            return if (mCurrentPageId != 0) mCurrentPageId else TvSettingsEnums.PAGE_SLICE_DEFAULT
        }

    interface Callbacks {
        fun showProgressBar(toShow: Boolean)

        fun setTitle(title: CharSequence?)

        fun setSubtitle(subtitle: CharSequence?)

        fun setIcon(icon: Drawable?)

        fun onSlice(slice: Slice?)
    }

    /** Callback for one panel settings fragment  */
    interface OnePanelSliceFragmentContainer {
        fun navigateBack()
    }

    companion object {
        private const val TAG: String = "SliceShard"

        private const val KEY_PREFERENCE_FOLLOWUP_INTENT: String =
            "slice_key_preference_followup_intent"
        private const val KEY_PREFERENCE_FOLLOWUP_RESULT_CODE: String =
            "slice_key_preference_followup_result_code"
        private const val KEY_SCREEN_TITLE: String = "slice_key_screen_title"
        private const val KEY_SCREEN_SUBTITLE: String = "slice_key_screen_subtitle"
        private const val KEY_SCREEN_ICON: String = "slice_key_screen_icon"
        private const val KEY_LAST_PREFERENCE: String = "slice_key_last_preference"
        private const val KEY_URI_STRING: String = "slice_key_uri_string"

        private fun isSamePreference(oldPref: Preference?, newPref: Preference?): Boolean {
            if (oldPref == null || newPref == null) {
                return false
            }

            if (newPref is HasSliceUri != oldPref is HasSliceUri) {
                return false
            }

            if (newPref is PreferenceGroup != oldPref is PreferenceGroup) {
                return false
            }

            if (newPref is EmbeddedSlicePreference) {
                return oldPref is EmbeddedSlicePreference
                        && newPref.uri == oldPref.uri
            } else if (oldPref is EmbeddedSlicePreference) {
                return false
            }

            return newPref.key != null && newPref.key == oldPref.key
        }
    }
}