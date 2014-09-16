/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.settings.about;

import com.android.tv.settings.R;

import com.android.tv.settings.PreferenceUtils;

import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;
import com.android.tv.settings.dialog.old.TosWebViewFragment;

import com.android.tv.settings.name.DeviceManager;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity which shows the build / model / legal info / etc.
 */
public class AboutActivity extends DialogActivity implements ActionAdapter.Listener,
        ActionAdapter.OnFocusListener {

    /**
     * Action key for legal actions container.
     */
    private static final String KEY_LEGAL_INFO = "about_legal_info";

    /**
     * Action key for legal open source license action.
     */
    private static final String KEY_LEGAL_INFO_OPEN_SOURCE_LICENSES =
            "about_legal_info_open_source_licenses";

    /**
     * Action keys for legal terms.
     */
    private static final String TERMS_OF_SERVICE = "terms_of_service";
    private static final String PRIVACY_POLICY = "privacy_policy";
    private static final String ADDITIONAL_TERMS = "additional_terms";

    private static final String KEY_BUILD = "build";

    /**
     * Intent action of SettingsLicenseActivity.
     */
    private static final String SETTINGS_LEGAL_LICENSE_INTENT_ACTION = "android.settings.LICENSE";

    /**
     * Intent action of device name activity.
     */
    private static final String SETTINGS_DEVICE_NAME_INTENT_ACTION =
        "android.settings.DEVICE_NAME";

    /**
     * Number of clicks it takes to be a developer.
     */
    private static final int NUM_DEVELOPER_CLICKS = 7;

    private int mDeveloperClickCount;
    private PreferenceUtils mPreferenceUtils;
    private Toast mToast;
    private int mSelectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferenceUtils = new PreferenceUtils(this);
        setContentAndActionFragments(ContentFragment.newInstance(
                        getString(R.string.about_preference), null, null, R.drawable.ic_settings_about,
                        getResources().getColor(R.color.icon_background)),
                ActionFragment.newInstance(getActions()));
        mSelectedIndex = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDeveloperClickCount = 0;
        setActionFragment(ActionFragment.newInstance(getActions(), mSelectedIndex), false);
    }

    @Override
    public void onActionFocused(Action action) {
        mSelectedIndex = getActions().indexOf(action);
    }

    @Override
    public void onActionClicked(Action action) {
        final String key = action.getKey();
        if (TextUtils.equals(key, KEY_BUILD)) {
            mDeveloperClickCount++;
            if (!mPreferenceUtils.isDeveloperEnabled()) {
                int numLeft = NUM_DEVELOPER_CLICKS - mDeveloperClickCount;
                if (numLeft < 3 && numLeft > 0) {
                    showToast(getResources().getQuantityString(
                            R.plurals.show_dev_countdown, numLeft, numLeft));
                }
                if (numLeft == 0) {
                    mPreferenceUtils.setDeveloperEnabled(true);
                    showToast(getString(R.string.show_dev_on));
                    mDeveloperClickCount = 0;
                }
            } else {
                if (mDeveloperClickCount > 3) {
                    showToast(getString(R.string.show_dev_already));
                }
            }
        } else if (TextUtils.equals(key, TERMS_OF_SERVICE)) {
            displayFragment(TosWebViewFragment.
                newInstance(TosWebViewFragment.SHOW_TERMS_OF_SERVICE));
        } else if (TextUtils.equals(key, PRIVACY_POLICY)) {
            displayFragment(TosWebViewFragment.newInstance(TosWebViewFragment.SHOW_PRIVACY_POLICY));
        } else if (TextUtils.equals(key, ADDITIONAL_TERMS)) {
            displayFragment(
                TosWebViewFragment.newInstance (TosWebViewFragment.SHOW_ADDITIONAL_TERMS));
        } else if (TextUtils.equals(key, KEY_LEGAL_INFO)) {
            ArrayList<Action> actions = getLegalActions();
            setContentAndActionFragments(ContentFragment.newInstance(
                    getString(R.string.about_legal_info), null, null),
                    ActionFragment.newInstance(actions));
        } else {
            Intent intent = action.getIntent();
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    private ArrayList<Action> getLegalActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        Intent licensesIntent = new Intent(SETTINGS_LEGAL_LICENSE_INTENT_ACTION);
        actions.add(new Action.Builder()
                .key(KEY_LEGAL_INFO_OPEN_SOURCE_LICENSES)
                .intent(licensesIntent)
                .title(getString(R.string.about_legal_license))
                .build());

        actions.add(new Action.Builder()
                .key(TERMS_OF_SERVICE)
                .title(getString(R.string.about_terms_of_service))
                .build());

        actions.add(new Action.Builder()
                .key(PRIVACY_POLICY)
                .title(getString(R.string.about_privacy_policy))
                .build());

        actions.add(new Action.Builder()
                .key(ADDITIONAL_TERMS)
                .title(getString(R.string.about_additional_terms))
                .build());

        return actions;
    }

    private ArrayList<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder()
                .key("update")
                .title(getString(R.string.about_system_update))
                .intent(new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"))
                .build());
        actions.add(new Action.Builder()
                .key("name")
                .title(getString(R.string.device_name))
                .description(DeviceManager.getDeviceName(this))
                .intent(new Intent(SETTINGS_DEVICE_NAME_INTENT_ACTION))
                .build());
        actions.add(new Action.Builder()
                .key(KEY_LEGAL_INFO)
                .title(getString(R.string.about_legal_info))
                .build());
        actions.add(new Action.Builder()
                .key("model")
                .title(getString(R.string.about_model))
                .description(Build.MODEL)
                .enabled(false)
                .build());
        actions.add(new Action.Builder()
                .key("version")
                .title(getString(R.string.about_version))
                .description(Build.VERSION.RELEASE)
                .enabled(false)
                .build());
        actions.add(new Action.Builder()
                .key("serial")
                .title(getString(R.string.about_serial))
                .description(Build.SERIAL)
                .enabled(false)
                .build());
        actions.add(new Action.Builder()
                .key(KEY_BUILD)
                .title(getString(R.string.about_build))
                .description(Build.DISPLAY)
                .enabled(true)
                .build());
        return actions;
    }

    private void displayFragment(Fragment fragment) {
        getFragmentManager()
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit();
    }

    private void showToast(String toastString) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this,  toastString, Toast.LENGTH_SHORT);
        mToast.show();
    }
}
