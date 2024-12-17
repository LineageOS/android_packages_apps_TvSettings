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
package com.android.tv.twopanelsettings.slices;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.app.slice.Slice.HINT_PARTIAL;

import static com.android.tv.twopanelsettings.slices.InstrumentationUtils.logEntrySelected;
import static com.android.tv.twopanelsettings.slices.InstrumentationUtils.logToggleInteracted;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_INFO_STATUS;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_PREFERENCE_KEY;
import static com.android.tv.twopanelsettings.slices.SlicesConstants.EXTRA_SLICE_FOLLOWUP;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;
import com.android.tv.twopanelsettings.slices.compat.Slice;
import com.android.tv.twopanelsettings.slices.compat.SliceItem;
import com.android.tv.twopanelsettings.slices.compat.widget.ListContent;
import com.android.tv.twopanelsettings.slices.compat.widget.SliceContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides functionality for a fragment to display slice data.
 */
public class SliceShard {
    private static final String TAG = "SliceShard";

    private static final String KEY_PREFERENCE_FOLLOWUP_INTENT =
            "slice_key_preference_followup_intent";
    private static final String KEY_PREFERENCE_FOLLOWUP_RESULT_CODE =
            "slice_key_preference_followup_result_code";
    private static final String KEY_SCREEN_TITLE = "slice_key_screen_title";
    private static final String KEY_SCREEN_SUBTITLE = "slice_key_screen_subtitle";
    private static final String KEY_SCREEN_ICON = "slice_key_screen_icon";
    private static final String KEY_LAST_PREFERENCE = "slice_key_last_preference";
    private static final String KEY_URI_STRING = "slice_key_uri_string";

    private final LeanbackPreferenceFragmentCompat mFragment;
    private final Callbacks mCallbacks;
    private final CharSequence mInitialTitle;

    private String mUriString;
    private Slice mSlice;
    private Context mPrefContext;
    private CharSequence mScreenTitle;
    private CharSequence mScreenSubtitle;
    private Icon mScreenIcon;
    private Parcelable mPreferenceFollowupIntent;
    private int mFollowupPendingIntentResultCode;
    private Intent mFollowupPendingIntentExtras;
    private Intent mFollowupPendingIntentExtrasCopy;
    private String mLastFocusedPreferenceKey;
    private boolean mIsMainPanelReady = true;
    private int mCurrentPageId;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<IntentSenderRequest> mActivityResultLauncher;
    private final ActivityResultLauncher<Intent> mActivityResultLauncherIntent;
    private final ActivityResultLauncher<Intent> mActivityResultLauncherIntentFollowup;
    private final Observer<Slice> mSliceObserver;
    private final ContentObserver mContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            handleUri(uri);
        }
    };

    public SliceShard(LeanbackPreferenceFragmentCompat fragment, String uriString,
            Callbacks callbacks, CharSequence initialTitle, Context prefContext) {
        mFragment = fragment;
        setUri(uriString);
        mCallbacks = callbacks;
        mInitialTitle = initialTitle;
        mActivityResultLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                this::processActionResult);
        mActivityResultLauncherIntent = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::processActionResult);
        mActivityResultLauncherIntentFollowup = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
        mScreenTitle = initialTitle;
        mPrefContext = prefContext;

        fragment.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                resume();
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                pause();
            }
        });

        mSliceObserver = slice -> {
            mSlice = slice;
            // Make TvSettings guard against the case that slice provider is not set up correctly
            if (slice == null || slice.getHints() == null) {
                return;
            }

            if (slice.getHints().contains(HINT_PARTIAL)) {
                mCallbacks.showProgressBar(true);
            } else {
                mCallbacks.showProgressBar(false);
            }
            mIsMainPanelReady = false;
            update();

        };
    }

    private void resume() {
        if (TextUtils.isEmpty(mScreenTitle)) {
            mScreenTitle = mInitialTitle;
        }

        mCallbacks.setTitle(mScreenTitle);
        mCallbacks.setSubtitle(mScreenSubtitle);
        mCallbacks.setIcon(mScreenIcon != null ? mScreenIcon.loadDrawable(mPrefContext) : null);
        mCallbacks.showProgressBar(true);

        if (!TextUtils.isEmpty(mUriString)) {
            getSliceLiveData().observeForever(mSliceObserver);
            mFragment.requireContext().getContentResolver().registerContentObserver(
                    SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver);
        }
        fireFollowupPendingIntent();
    }

    private void pause() {
        mCallbacks.showProgressBar(false);
        requireContext().getContentResolver().unregisterContentObserver(mContentObserver);
        getSliceLiveData().removeObserver(mSliceObserver);
    }

    private PreferenceSliceLiveData.SliceLiveDataImpl getSliceLiveData() {
        return ContextSingleton.getInstance()
                .getSliceLiveData(mFragment.requireActivity(), Uri.parse(mUriString));
    }

    private void processActionResult(ActivityResult result) {
        Intent data = result.getData();
        mFollowupPendingIntentExtras = data;
        mFollowupPendingIntentExtrasCopy = data == null ? null : new Intent(
                data);
        mFollowupPendingIntentResultCode = result.getResultCode();
    }

    private void fireFollowupPendingIntent() {
        if (mFollowupPendingIntentExtras == null) {
            return;
        }
        // If there is followup pendingIntent returned from initial activity, send it.
        // Otherwise send the followup pendingIntent provided by slice api.
        Parcelable followupPendingIntent;
        try {
            followupPendingIntent = mFollowupPendingIntentExtrasCopy.getParcelableExtra(
                    EXTRA_SLICE_FOLLOWUP);
        } catch (Throwable ex) {
            // unable to parse, the Intent has custom Parcelable, fallback
            followupPendingIntent = null;
        }
        if (followupPendingIntent instanceof PendingIntent) {
            try {
                ((PendingIntent) followupPendingIntent).send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e);
            }
        } else {
            if (mPreferenceFollowupIntent == null) {
                return;
            }
            if (mPreferenceFollowupIntent instanceof Intent) {
                Intent filledIn = new Intent((Intent) mPreferenceFollowupIntent);
                filledIn.fillIn(mFollowupPendingIntentExtras, 0);
                if (requireContext().getPackageManager().resolveActivity(filledIn, 0) != null) {
                    mActivityResultLauncherIntentFollowup.launch(filledIn);
                } else {
                    requireContext().sendBroadcast(filledIn);
                }
            } else {
                try {
                    ((PendingIntent) mPreferenceFollowupIntent).send(requireContext(),
                            mFollowupPendingIntentResultCode, mFollowupPendingIntentExtras);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Followup PendingIntent for slice cannot be sent", e);
                }
            }
            mPreferenceFollowupIntent = null;
        }
    }

    private Context requireContext() {
        return mFragment.requireContext();
    }

    private boolean isUriValid(String uri) {
        if (uri == null) {
            return false;
        }
        ContentProviderClient client =
                requireContext().getContentResolver().acquireContentProviderClient(Uri.parse(uri));
        if (client != null) {
            client.close();
            return true;
        } else {
            return false;
        }
    }

    private void update() {
        ListContent listContent = new ListContent(mSlice);
        PreferenceScreen preferenceScreen =
                mFragment.getPreferenceManager().getPreferenceScreen();

        if (preferenceScreen == null) {
            return;
        }

        List<SliceContent> items = listContent.getRowItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        SliceItem redirectSliceItem = SlicePreferencesUtil.getRedirectSlice(items);
        String redirectSlice = null;
        if (redirectSliceItem != null) {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(redirectSliceItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                redirectSlice = title.toString();
            }
        }
        if (isUriValid(redirectSlice)) {
            getSliceLiveData().removeObserver(mSliceObserver);
            requireContext().getContentResolver().unregisterContentObserver(mContentObserver);
            setUri(redirectSlice);
            getSliceLiveData().observeForever(mSliceObserver);
            requireContext().getContentResolver().registerContentObserver(
                    SlicePreferencesUtil.getStatusPath(mUriString), false, mContentObserver);
        }

        SliceItem screenTitleItem = SlicePreferencesUtil.getScreenTitleItem(items);
        if (screenTitleItem == null) {
            mCallbacks.setTitle(mScreenTitle);
        } else {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(screenTitleItem);
            mCurrentPageId = SlicePreferencesUtil.getPageId(screenTitleItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                mCallbacks.setTitle(title);
                mScreenTitle = title;
            } else {
                mCallbacks.setTitle(mScreenTitle);
            }

            CharSequence subtitle = SlicePreferencesUtil.getText(data.mSubtitleItem);
            mScreenSubtitle = subtitle;
            mCallbacks.setSubtitle(subtitle);

            Icon icon = SlicePreferencesUtil.getIcon(data.mStartItem);
            mScreenIcon = icon;
            mCallbacks.setIcon(icon != null ? icon.loadDrawable(mPrefContext) : null);
        }

        SliceItem focusedPrefItem = SlicePreferencesUtil.getFocusedPreferenceItem(items);
        CharSequence defaultFocusedKey = null;
        if (focusedPrefItem != null) {
            SlicePreferencesUtil.Data data = SlicePreferencesUtil.extract(focusedPrefItem);
            CharSequence title = SlicePreferencesUtil.getText(data.mTitleItem);
            if (!TextUtils.isEmpty(title)) {
                defaultFocusedKey = title;
            }
        }

        List<Preference> newPrefs = new ArrayList<>();
        for (SliceContent contentItem : items) {
            SliceItem item = contentItem.getSliceItem();
            if (SlicesConstants.TYPE_PREFERENCE.equals(item.getSubType())
                    || SlicesConstants.TYPE_PREFERENCE_CATEGORY.equals(item.getSubType())
                    || SlicesConstants.TYPE_PREFERENCE_EMBEDDED_PLACEHOLDER.equals(
                    item.getSubType())) {
                Preference preference =
                        SlicePreferencesUtil.getPreference(
                                item, mPrefContext, SliceFragment.class.getCanonicalName(),
                                isTwoPanel(),
                                mFragment.getPreferenceScreen());
                if (preference != null) {
                    newPrefs.add(preference);
                }
            }
        }
        updatePreferenceGroup(preferenceScreen, newPrefs);

        removeAnimationClipping(mFragment.getView());

        if (defaultFocusedKey != null) {
            mFragment.scrollToPreference(defaultFocusedKey.toString());
        } else if (mLastFocusedPreferenceKey != null) {
            mFragment.scrollToPreference(mLastFocusedPreferenceKey);
        }

        if (isTwoPanel()) {
            ((TwoPanelSettingsFragment) mFragment.getParentFragment()).refocusPreference(mFragment);
        }
        mIsMainPanelReady = true;
    }

    private void updatePreferenceGroup(PreferenceGroup group, List<Preference> newPrefs) {
        // Remove all the preferences in the screen that satisfy such three cases:
        // (a) Preference without key
        // (b) Preference with key which does not appear in the new list.
        // (c) Preference with key which does appear in the new list, but the preference has changed
        // ability to handle slices and needs to be replaced instead of re-used.
        int index = 0;
        IdentityHashMap<Preference, Preference> newToOld = new IdentityHashMap<>();
        while (index < group.getPreferenceCount()) {
            boolean needToRemoveCurrentPref = true;
            Preference oldPref = group.getPreference(index);
            for (Preference newPref : newPrefs) {
                if (isSamePreference(oldPref, newPref)) {
                    needToRemoveCurrentPref = false;
                    newToOld.put(newPref, oldPref);
                    break;
                }
            }

            if (needToRemoveCurrentPref) {
                group.removePreference(oldPref);
            } else {
                index++;
            }
        }

        Map<Integer, Boolean> twoStatePreferenceIsCheckedByOrder = new HashMap<>();
        for (int i = 0; i < newPrefs.size(); i++) {
            if (newPrefs.get(i) instanceof TwoStatePreference) {
                twoStatePreferenceIsCheckedByOrder.put(
                        i, ((TwoStatePreference) newPrefs.get(i)).isChecked());
            }
        }

        //Iterate the new preferences list and give each preference a correct order
        for (int i = 0; i < newPrefs.size(); i++) {
            Preference newPref = newPrefs.get(i);
            // If the newPref has a key and has a corresponding old preference, update the old
            // preference and give it a new order.

            Preference oldPref = newToOld.get(newPref);
            if (oldPref == null) {
                newPref.setOrder(i);
                group.addPreference(newPref);
                continue;
            }

            oldPref.setOrder(i);
            if (oldPref instanceof EmbeddedSlicePreference) {
                // EmbeddedSlicePreference has its own slice observer
                // (EmbeddedSlicePreferenceHelper). Should therefore not be updated by
                // slice observer in SliceFragment.
                // The order will however still need to be updated, as this can not be handled
                // by EmbeddedSlicePreferenceHelper.
                continue;
            }

            oldPref.setIcon(newPref.getIcon());
            oldPref.setTitle(newPref.getTitle());
            oldPref.setSummary(newPref.getSummary());
            oldPref.setEnabled(newPref.isEnabled());
            oldPref.setSelectable(newPref.isSelectable());
            oldPref.setFragment(newPref.getFragment());
            oldPref.getExtras().putAll(newPref.getExtras());
            if ((oldPref instanceof HasSliceAction)
                    && (newPref instanceof HasSliceAction)) {
                ((HasSliceAction) oldPref)
                        .setSliceAction(
                                ((HasSliceAction) newPref).getSliceAction());
            }
            if ((oldPref instanceof HasSliceUri)
                    && (newPref instanceof HasSliceUri)) {
                ((HasSliceUri) oldPref)
                        .setUri(((HasSliceUri) newPref).getUri());
            }
            if ((oldPref instanceof HasCustomContentDescription)
                    && (newPref instanceof HasCustomContentDescription)) {
                ((HasCustomContentDescription) oldPref).setContentDescription(
                        ((HasCustomContentDescription) newPref)
                                .getContentDescription());
            }

            if (oldPref instanceof PreferenceGroup && newPref instanceof PreferenceGroup) {
                PreferenceGroup newGroup = (PreferenceGroup) newPref;
                List<Preference> newChildren = new ArrayList<>();
                for (int j = 0; j < newGroup.getPreferenceCount(); j++) {
                    newChildren.add(newGroup.getPreference(j));
                }
                newGroup.removeAll();
                updatePreferenceGroup((PreferenceGroup) oldPref, newChildren);
            }
        }

        //addPreference will reset the checked status of TwoStatePreference.
        //So we need to add them back
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference screenPref = group.getPreference(i);
            if (screenPref instanceof TwoStatePreference
                    && twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder()) != null) {
                ((TwoStatePreference) screenPref)
                        .setChecked(twoStatePreferenceIsCheckedByOrder.get(screenPref.getOrder()));
            }
        }
    }

    private static boolean isSamePreference(Preference oldPref, Preference newPref) {
        if (oldPref == null || newPref == null) {
            return false;
        }

        if (newPref instanceof HasSliceUri != oldPref instanceof HasSliceUri) {
            return false;
        }

        if (newPref instanceof PreferenceGroup != oldPref instanceof PreferenceGroup) {
            return false;
        }

        if (newPref instanceof EmbeddedSlicePreference) {
            return oldPref instanceof EmbeddedSlicePreference
                    && Objects.equals(((EmbeddedSlicePreference) newPref).getUri(),
                    ((EmbeddedSlicePreference) oldPref).getUri());
        } else if (oldPref instanceof EmbeddedSlicePreference) {
            return false;
        }

        return newPref.getKey() != null && newPref.getKey().equals(oldPref.getKey());
    }

    public void onPreferenceFocused(Preference preference) {
        mLastFocusedPreferenceKey = preference.getKey();
    }

    public void onSeekbarPreferenceChanged(SliceSeekbarPreference preference, int addValue) {
        int curValue = preference.getValue();
        if ((addValue > 0 && curValue < preference.getMax()) ||
                (addValue < 0 && curValue > preference.getMin())) {
            preference.setValue(curValue + addValue);

            try {
                Intent fillInIntent =
                        new Intent()
                                .putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
                firePendingIntent(preference, fillInIntent);
            } catch (Exception e) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof SliceRadioPreference) {
            SliceRadioPreference radioPref = (SliceRadioPreference) preference;
            if (!radioPref.isChecked()) {
                radioPref.setChecked(true);
                if (TextUtils.isEmpty(radioPref.getUri())) {
                    return true;
                }
            }

            logEntrySelected(getPreferenceActionId(preference));
            Intent fillInIntent = new Intent().putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());

            boolean result = firePendingIntent(radioPref, fillInIntent);
            radioPref.clearOtherRadioPreferences(mFragment.getPreferenceScreen());
            if (result) {
                return true;
            }
        } else if (preference instanceof TwoStatePreference
                && preference instanceof HasSliceAction) {
            boolean isChecked = ((TwoStatePreference) preference).isChecked();
            preference.getExtras().putBoolean(EXTRA_PREFERENCE_INFO_STATUS, isChecked);
            if (isTwoPanel()) {
                ((TwoPanelSettingsFragment) mFragment.getParentFragment()).refocusPreference(
                        mFragment);
            }
            logToggleInteracted(getPreferenceActionId(preference), isChecked);
            Intent fillInIntent =
                    new Intent()
                            .putExtra(EXTRA_TOGGLE_STATE, isChecked)
                            .putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
            if (firePendingIntent((HasSliceAction) preference, fillInIntent)) {
                return true;
            }
            return true;
        } else if (preference instanceof SlicePreference) {
            // In this case, we may intentionally ignore this entry selection to avoid double
            // logging as the action should result in a PAGE_FOCUSED event being logged.
            if (getPreferenceActionId(preference) != TvSettingsEnums.ENTRY_DEFAULT) {
                logEntrySelected(getPreferenceActionId(preference));
            }
            Intent fillInIntent =
                    new Intent().putExtra(EXTRA_PREFERENCE_KEY, preference.getKey());
            if (firePendingIntent((HasSliceAction) preference, fillInIntent)) {
                return true;
            }
        }

        return false;
    }

    private void removeAnimationClipping(View v) {
        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
            for (int index = 0; index < ((ViewGroup) v).getChildCount(); index++) {
                View child = ((ViewGroup) v).getChildAt(index);
                removeAnimationClipping(child);
            }
        }
    }

    private boolean isTwoPanel() {
        return mFragment.getParentFragment() instanceof TwoPanelSettingsFragment;
    }

    private boolean firePendingIntent(@NonNull HasSliceAction preference, Intent fillInIntent) {
        if (preference.getSliceAction() == null) {
            return false;
        }

        Intent intent = preference.getSliceAction().getActionIntent();
        if (intent != null) {
            Intent filledIn = new Intent(intent);
            if (fillInIntent != null) {
                filledIn.fillIn(fillInIntent, 0);
            }

            if (requireContext().getPackageManager().resolveActivity(filledIn, 0) != null) {
                mActivityResultLauncherIntent.launch(filledIn);
            } else {
                requireContext().sendBroadcast(intent);
            }
        } else {
            IntentSender intentSender = preference.getSliceAction().getAction().getIntentSender();
            mActivityResultLauncher.launch(
                    new IntentSenderRequest.Builder(intentSender).setFillInIntent(
                            fillInIntent).build());
        }
        if (preference.getFollowupSliceAction() != null) {
            mPreferenceFollowupIntent = preference.getFollowupSliceAction().getAction();
            if (mPreferenceFollowupIntent == null) {
                mPreferenceFollowupIntent = preference.getFollowupSliceAction().getActionIntent();
            }
        }

        return true;
    }

    private void back() {
        if (isTwoPanel()) {
            TwoPanelSettingsFragment parentFragment =
                    (TwoPanelSettingsFragment) mFragment.getCallbackFragment();
            if (parentFragment.isFragmentInTheMainPanel(mFragment)) {
                parentFragment.navigateBack();
            }
        } else if (mFragment.getCallbackFragment() instanceof OnePanelSliceFragmentContainer) {
            ((OnePanelSliceFragmentContainer) mFragment.getCallbackFragment()).navigateBack();
        }
    }

    private void forward() {
        if (mIsMainPanelReady) {
            if (isTwoPanel()) {
                TwoPanelSettingsFragment parentFragment =
                        (TwoPanelSettingsFragment) mFragment.getCallbackFragment();
                Preference chosenPreference = TwoPanelSettingsFragment.getChosenPreference(
                        mFragment);
                if (chosenPreference == null && mLastFocusedPreferenceKey != null) {
                    chosenPreference = mFragment.findPreference(mLastFocusedPreferenceKey);
                }
                if (chosenPreference != null && chosenPreference instanceof HasSliceUri
                        && ((HasSliceUri) chosenPreference).getUri() != null) {
                    chosenPreference.setFragment(SliceFragment.class.getCanonicalName());
                    parentFragment.refocusPreferenceForceRefresh(chosenPreference, mFragment);
                }
                if (parentFragment.isFragmentInTheMainPanel(mFragment)) {
                    parentFragment.navigateToPreviewFragment();
                }
            }
        } else {
            mHandler.post(this::forward);
        }
    }


    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT, mPreferenceFollowupIntent);
        outState.putInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE, mFollowupPendingIntentResultCode);
        outState.putCharSequence(KEY_SCREEN_TITLE, mScreenTitle);
        outState.putCharSequence(KEY_SCREEN_SUBTITLE, mScreenSubtitle);
        outState.putParcelable(KEY_SCREEN_ICON, mScreenIcon);
        outState.putString(KEY_LAST_PREFERENCE, mLastFocusedPreferenceKey);
        outState.putString(KEY_URI_STRING, mUriString);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mPreferenceFollowupIntent =
                savedInstanceState.getParcelable(KEY_PREFERENCE_FOLLOWUP_INTENT);
        mFollowupPendingIntentResultCode =
                savedInstanceState.getInt(KEY_PREFERENCE_FOLLOWUP_RESULT_CODE);
        mScreenTitle = savedInstanceState.getCharSequence(KEY_SCREEN_TITLE);
        mScreenSubtitle = savedInstanceState.getCharSequence(KEY_SCREEN_SUBTITLE);
        mScreenIcon = savedInstanceState.getParcelable(KEY_SCREEN_ICON);
        mLastFocusedPreferenceKey = savedInstanceState.getString(KEY_LAST_PREFERENCE);
        setUri(savedInstanceState.getString(KEY_URI_STRING));
    }

    private void handleUri(Uri uri) {
        String uriString = uri.getQueryParameter(SlicesConstants.PARAMETER_URI);
        String errorMessage = uri.getQueryParameter(SlicesConstants.PARAMETER_ERROR);
        // Display the errorMessage based upon two different scenarios:
        // a) If the provided uri string matches with current page slice uri(usually happens
        // when the data fails to correctly load), show the errors in the current panel using
        // InfoFragment UI.
        // b) If the provided uri string does not match with current page slice uri(usually happens
        // when the data fails to save), show the error message as the toast.
        if (uriString != null && errorMessage != null) {
            if (!uriString.equals(mUriString)) {
                showErrorMessageAsToast(errorMessage);
            } else {
                showErrorMessage(errorMessage);
            }
        }
        // Provider should provide the correct slice uri in the parameter if it wants to do certain
        // action(includes go back, forward), otherwise TvSettings would ignore it.
        if (uriString == null || !uriString.equals(mUriString)) {
            return;
        }
        String direction = uri.getQueryParameter(SlicesConstants.PARAMETER_DIRECTION);
        if (direction != null) {
            if (direction.equals(SlicesConstants.FORWARD)) {
                forward();
            } else if (direction.equals(SlicesConstants.BACKWARD)) {
                back();
            } else if (direction.equals(SlicesConstants.EXIT)) {
                mFragment.requireActivity().setResult(Activity.RESULT_OK);
                mFragment.requireActivity().finish();
            }
        }
    }

    private void showErrorMessage(String errorMessage) {
        if (isTwoPanel()) {
            ((TwoPanelSettingsFragment) mFragment.getCallbackFragment()).showErrorMessage(
                    errorMessage, mFragment);
        }
    }

    private void showErrorMessageAsToast(String errorMessage) {
        Toast.makeText(mFragment.requireActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    private int getPreferenceActionId(Preference preference) {
        if (preference instanceof HasSliceAction) {
            return ((HasSliceAction) preference).getActionId() != 0
                    ? ((HasSliceAction) preference).getActionId()
                    : TvSettingsEnums.ENTRY_DEFAULT;
        }
        return TvSettingsEnums.ENTRY_DEFAULT;
    }

    private void setUri(String uriString) {
        mUriString = uriString;
        if (!TextUtils.isEmpty(uriString)) {
            ContextSingleton.getInstance().grantFullAccess(mFragment.requireContext(),
                    Uri.parse(uriString));
        }
    }

    public int getPageId() {
        return mCurrentPageId != 0 ? mCurrentPageId : TvSettingsEnums.PAGE_SLICE_DEFAULT;
    }

    public CharSequence getScreenTitle() {
        return mScreenTitle;
    }

    public interface Callbacks {
        void showProgressBar(boolean toShow);

        void setTitle(CharSequence title);

        void setSubtitle(CharSequence subtitle);

        void setIcon(Drawable icon);
    }

    /** Callback for one panel settings fragment **/
    public interface OnePanelSliceFragmentContainer {
        void navigateBack();
    }
}
