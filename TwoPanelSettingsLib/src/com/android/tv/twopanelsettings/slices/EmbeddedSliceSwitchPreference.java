/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.SliceContent;

import com.android.tv.twopanelsettings.R;

import java.util.List;

/**
 * An embedded slice switch preference which would be embedded in common TvSettings preference
 * items, but will automatically update its status and communicates with external apps through
 * slice api.
 */
public class EmbeddedSliceSwitchPreference extends SliceSwitchPreference implements
        Observer<Slice> {
    private static final String TAG = "EmbeddedSliceSwitchPreference";
    private String mUri;
    private Slice mSlice;

    @Override
    public void onAttached() {
        super.onAttached();
        getSliceLiveData().observeForever(this);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        getSliceLiveData().removeObserver(this);
    }

    public EmbeddedSliceSwitchPreference(Context context) {
        super(context);
        init(null);
    }

    public EmbeddedSliceSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            initStyleAttributes(attrs);
        }
    }

    private PreferenceSliceLiveData.SliceLiveDataImpl getSliceLiveData() {
        return ContextSingleton.getInstance()
                .getSliceLiveData(getContext(), Uri.parse(mUri));
    }

    private void initStyleAttributes(AttributeSet attrs) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SlicePreference);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.SlicePreference_uri) {
                mUri = a.getString(attr);
                break;
            }
        }
    }

    @Override
    public void onChanged(Slice slice) {
        if (!getSliceLiveData().mUpdatePending.compareAndSet(true, false)) {
            return;
        }
        mSlice = slice;
        if (slice == null || slice.getHints() == null || slice.getHints().contains(HINT_PARTIAL)) {
            setVisible(false);
            return;
        }
        update();
    }

    private void update() {
        ListContent mListContent = new ListContent(mSlice);
        List<SliceContent> items = mListContent.getRowItems();
        if (items == null || items.size() == 0) {
            setVisible(false);
            return;
        }
        SliceItem embeddedItem = SlicePreferencesUtil.getEmbeddedItem(items);
        Preference newPref = SlicePreferencesUtil.getPreference(embeddedItem,
                (ContextThemeWrapper) getContext(), null);
        if (newPref == null) {
            setVisible(false);
            return;
        }
        setTitle(newPref.getTitle());
        setSummary(newPref.getSummary());
        setIcon(newPref.getIcon());
        if (newPref instanceof TwoStatePreference) {
            setChecked(((TwoStatePreference) newPref).isChecked());
        }
        if (newPref instanceof HasSliceAction) {
            setSliceAction(((HasSliceAction) newPref).getSliceAction());
        }
        setVisible(true);
    }

    @Override
    public void onClick() {
        boolean newValue = !isChecked();
        try {
            if (mAction == null) {
                return;
            }
            if (mAction.isToggle()) {
                // Update the intent extra state
                Intent i = new Intent().putExtra(EXTRA_TOGGLE_STATE, newValue);
                mAction.getActionItem().fireAction(getContext(), i);
            } else {
                mAction.getActionItem().fireAction(null, null);
            }
        } catch (PendingIntent.CanceledException e) {
            newValue = !newValue;
            Log.e(TAG, "PendingIntent for slice cannot be sent", e);
        }
        if (callChangeListener(newValue)) {
            setChecked(newValue);
        }
    }
}
