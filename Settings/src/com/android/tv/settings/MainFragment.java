/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.tv.settings.accessories.AccessoryUtils;
import com.android.tv.settings.accessories.BluetoothAccessoryFragment;
import com.android.tv.settings.accounts.AccountSyncFragment;
import com.android.tv.settings.accounts.AddAccountWithTypeActivity;
import com.android.tv.settings.connectivity.ConnectivityListener;
import com.android.tv.settings.device.sound.SoundFragment;
import com.android.tv.settings.suggestions.SuggestionPreference;
import com.android.tv.settings.system.SecurityFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The fragment where all good things begin. Evil is handled elsewhere.
 */
public class MainFragment extends SettingsPreferenceFragment implements
        SuggestionControllerMixin.SuggestionControllerHost {
    private static final String TAG = "MainFragment";

    private static final String KEY_SUGGESTIONS_LIST = "suggestions";
    @VisibleForTesting
    static final String KEY_DEVELOPER = "developer";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_SECURITY = "security";
    private static final String KEY_USAGE = "usageAndDiag";
    private static final String KEY_ADD_ACCOUNT = "add_account";
    private static final String KEY_ACCESSORIES = "accessories";
    private static final String KEY_PERSONAL = "personal";
    private static final String KEY_ADD_ACCESSORY = "add_accessory";
    @VisibleForTesting
    static final String KEY_NETWORK = "network";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_SOUNDS = "sound_effects";
    private static final String KEY_GOOGLE_SETTINGS = "google_settings";
    private static final String KEY_HOME_SETTINGS = "home";
    @VisibleForTesting
    static final String KEY_CAST_SETTINGS = "cast";
    private static final String KEY_SPEECH_SETTINGS = "speech";
    private static final String KEY_SEARCH_SETTINGS = "search";
    private static final String KEY_ACCOUNTS_CATEGORY = "accounts";

    private AuthenticatorHelper mAuthenticatorHelper;
    private BluetoothAdapter mBtAdapter;
    @VisibleForTesting
    ConnectivityListener mConnectivityListener;

    private boolean mInputSettingNeeded;

    private PreferenceCategory mSuggestionsList;
    private PreferenceGroup mAccessoriesGroup;
    private PreferenceGroup mAccountsGroup;
    private Preference mAddAccessory;
    private Preference mSoundsPref;
    private SuggestionControllerMixin mSuggestionControllerMixin;

    private final BroadcastReceiver mBCMReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAccessories();
        }
    };

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public int getMetricsCategory() {
        // TODO(70572789): Finalize metrics categories.
        return 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mAuthenticatorHelper = new AuthenticatorHelper(getContext(),
                new UserHandle(UserHandle.myUserId()), userHandle -> updateAccounts());
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectivityListener =
                new ConnectivityListener(getContext(), this::updateWifi, getLifecycle());

        final TvInputManager manager = (TvInputManager) getContext().getSystemService(
                Context.TV_INPUT_SERVICE);
        if (manager != null) {
            for (final TvInputInfo input : manager.getTvInputList()) {
                if (input.isPassthroughInput()) {
                    mInputSettingNeeded = true;
                }
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (isRestricted()) {
            setPreferencesFromResource(R.xml.restricted_prefs, null);
        } else {
            setPreferencesFromResource(R.xml.main_prefs, null);
        }
        mAccessoriesGroup = (PreferenceGroup) findPreference(KEY_ACCESSORIES);
        mAddAccessory = findPreference(KEY_ADD_ACCESSORY);
        mSoundsPref = findPreference(KEY_SOUNDS);
        mAccountsGroup = (PreferenceGroup) findPreference(KEY_ACCOUNTS_CATEGORY);

        final Preference inputPref = findPreference(KEY_INPUTS);
        if (inputPref != null) {
            inputPref.setVisible(mInputSettingNeeded);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuthenticatorHelper.listenToAccountUpdates();

        IntentFilter btChangeFilter = new IntentFilter();
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btChangeFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(mBCMReceiver, btChangeFilter);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAccounts();
        updateAccessories();
        updateDeveloperOptions();
        updateSounds();
        updateGoogleSettings();
        updateCastSettings();

        hideIfIntentUnhandled(findPreference(KEY_HOME_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_CAST_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_USAGE));
        hideIfIntentUnhandled(findPreference(KEY_SPEECH_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_SEARCH_SETTINGS));
    }

    @Override
    public void onStop() {
        super.onStop();
        mAuthenticatorHelper.stopListeningToAccountUpdates();
        getContext().unregisterReceiver(mBCMReceiver);
    }

    private void hideIfIntentUnhandled(Preference preference) {
        if (preference == null) {
            return;
        }
        preference.setVisible(systemIntentIsHandled(getContext(), preference.getIntent()) != null);
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext());
    }

    private void updateAccounts() {
        if (mAccountsGroup == null) {
            return;
        }

        final Set<String> touchedAccounts = new ArraySet<>(mAccountsGroup.getPreferenceCount());

        final AccountManager am = AccountManager.get(getContext());
        final AuthenticatorDescription[] authTypes = am.getAuthenticatorTypes();
        final ArrayList<String> allowableAccountTypes = new ArrayList<>(authTypes.length);
        final Context themedContext = getPreferenceManager().getContext();

        for (AuthenticatorDescription authDesc : authTypes) {
            final Context targetContext;
            try {
                targetContext = getContext().createPackageContext(authDesc.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Authenticator description with bad package name", e);
                continue;
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception loading package resources", e);
                continue;
            }

            // Main title text comes from the authenticator description (e.g. "Google").
            String authTitle = null;
            try {
                authTitle = targetContext.getString(authDesc.labelId);
                if (TextUtils.isEmpty(authTitle)) {
                    authTitle = null;  // Handled later when we add the row.
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Authenticator description with bad label id", e);
            }

            // There exist some authenticators which aren't intended to be user-facing.
            // If the authenticator doesn't have a title or an icon, don't present it to
            // the user as an option.
            if (authTitle != null || authDesc.iconId != 0) {
                allowableAccountTypes.add(authDesc.type);
            }

            Account[] accounts = am.getAccountsByType(authDesc.type);
            if (accounts == null || accounts.length == 0) {
                continue;  // No point in continuing; there aren't any accounts to show.
            }

            // Icon URI to be displayed for each account is based on the type of authenticator.
            Drawable authImage = null;
            try {
                authImage = targetContext.getDrawable(authDesc.iconId);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Authenticator has bad resources", e);
            }

            // Display an entry for each installed account we have.
            for (final Account account : accounts) {
                final String key = "account_pref:" + account.type + ":" + account.name;
                Preference preference = findPreference(key);
                if (preference == null) {
                    preference = new Preference(themedContext);
                }
                preference.setTitle(authTitle != null ? authTitle : account.name);
                preference.setIcon(authImage);
                preference.setSummary(authTitle != null ? account.name : null);
                preference.setFragment(AccountSyncFragment.class.getName());
                AccountSyncFragment.prepareArgs(preference.getExtras(), account);

                touchedAccounts.add(key);
                preference.setKey(key);

                mAccountsGroup.addPreference(preference);
            }
        }

        for (int i = 0; i < mAccountsGroup.getPreferenceCount();) {
            final Preference preference = mAccountsGroup.getPreference(i);
            final String key = preference.getKey();
            if (touchedAccounts.contains(key) || TextUtils.equals(KEY_ADD_ACCOUNT, key)) {
                i++;
            } else {
                mAccountsGroup.removePreference(preference);
            }
        }

        // Never allow restricted profile to add accounts.
        final Preference addAccountPref = findPreference(KEY_ADD_ACCOUNT);
        if (addAccountPref != null) {
            addAccountPref.setOrder(Integer.MAX_VALUE);
            if (isRestricted()) {
                addAccountPref.setVisible(false);
            } else {
                Intent i = new Intent().setComponent(new ComponentName("com.android.tv.settings",
                        "com.android.tv.settings.accounts.AddAccountWithTypeActivity"));
                i.putExtra(AddAccountWithTypeActivity.EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY,
                        allowableAccountTypes.toArray(new String[allowableAccountTypes.size()]));

                // If there are available account types, show the "add account" button.
                addAccountPref.setVisible(!allowableAccountTypes.isEmpty());
                addAccountPref.setIntent(i);
            }
        }
    }

    private void updateAccessories() {
        if (mAccessoriesGroup == null) {
            return;
        }

        if (mBtAdapter == null) {
            mAccessoriesGroup.setVisible(false);
            mAccessoriesGroup.removeAll();
            return;
        }

        final Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        if (bondedDevices == null) {
            mAccessoriesGroup.setVisible(false);
            mAccessoriesGroup.removeAll();
            return;
        }

        final Context themedContext = getPreferenceManager().getContext();

        final Set<String> touchedKeys = new ArraySet<>(bondedDevices.size() + 1);
        if (mAddAccessory != null) {
            touchedKeys.add(mAddAccessory.getKey());
        }

        for (final BluetoothDevice device : bondedDevices) {
            final String deviceAddress = device.getAddress();
            if (TextUtils.isEmpty(deviceAddress)) {
                Log.w(TAG, "Skipping mysteriously empty bluetooth device");
                continue;
            }

            final String desc = device.isConnected() ? getString(R.string.accessory_connected) :
                    null;
            final String key = "BluetoothDevice:" + deviceAddress;
            touchedKeys.add(key);
            Preference preference = mAccessoriesGroup.findPreference(key);
            if (preference == null) {
                preference = new Preference(themedContext);
                preference.setKey(key);
            }
            final String deviceName = device.getAliasName();
            preference.setTitle(deviceName);
            preference.setSummary(desc);
            final int deviceImgId = AccessoryUtils.getImageIdForDevice(device);
            preference.setIcon(deviceImgId);
            preference.setFragment(BluetoothAccessoryFragment.class.getName());
            BluetoothAccessoryFragment.prepareArgs(
                    preference.getExtras(),
                    deviceAddress,
                    deviceName,
                    deviceImgId);
            mAccessoriesGroup.addPreference(preference);
        }

        for (int i = 0; i < mAccessoriesGroup.getPreferenceCount();) {
            final Preference preference = mAccessoriesGroup.getPreference(i);
            if (touchedKeys.contains(preference.getKey())) {
                i++;
            } else {
                mAccessoriesGroup.removePreference(preference);
            }
        }
    }

    @VisibleForTesting
    void updateDeveloperOptions() {
        final Preference developerPref = findPreference(KEY_DEVELOPER);
        if (developerPref == null) {
            return;
        }

        developerPref.setVisible(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(
                getContext()));
    }

    private void updateSounds() {
        if (mSoundsPref == null) {
            return;
        }

        mSoundsPref.setIcon(SoundFragment.getSoundEffectsEnabled(getContext().getContentResolver())
                ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
    }

    @VisibleForTesting
    void updateWifi() {
        final Preference networkPref = findPreference(KEY_NETWORK);
        if (networkPref == null) {
            return;
        }

        networkPref.setTitle(mConnectivityListener.isEthernetAvailable()
                ? R.string.connectivity_network : R.string.connectivity_wifi);

        if (mConnectivityListener.isCellConnected()) {
            final int signal = mConnectivityListener.getCellSignalStrength();
            switch (signal) {
                case SignalStrength.SIGNAL_STRENGTH_GREAT:
                    networkPref.setIcon(R.drawable.ic_cell_signal_4_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_GOOD:
                    networkPref.setIcon(R.drawable.ic_cell_signal_3_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_MODERATE:
                    networkPref.setIcon(R.drawable.ic_cell_signal_2_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_POOR:
                    networkPref.setIcon(R.drawable.ic_cell_signal_1_white);
                    break;
                case SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN:
                default:
                    networkPref.setIcon(R.drawable.ic_cell_signal_0_white);
                    break;
            }
        } else if (mConnectivityListener.isEthernetConnected()) {
            networkPref.setIcon(R.drawable.ic_ethernet_white);
        } else if (mConnectivityListener.isWifiEnabledOrEnabling()) {
            final int signal = mConnectivityListener.getWifiSignalStrength(5);
            switch (signal) {
                case 4:
                    networkPref.setIcon(R.drawable.ic_wifi_signal_4_white);
                    break;
                case 3:
                    networkPref.setIcon(R.drawable.ic_wifi_signal_3_white);
                    break;
                case 2:
                    networkPref.setIcon(R.drawable.ic_wifi_signal_2_white);
                    break;
                case 1:
                    networkPref.setIcon(R.drawable.ic_wifi_signal_1_white);
                    break;
                case 0:
                default:
                    networkPref.setIcon(R.drawable.ic_wifi_signal_0_white);
                    break;
            }
        } else {
            networkPref.setIcon(R.drawable.ic_wifi_signal_off_white);
        }
    }

    @VisibleForTesting
    void updateCastSettings() {
        final Preference castPref = findPreference(KEY_CAST_SETTINGS);
        if (castPref != null) {
            final ResolveInfo info = systemIntentIsHandled(getContext(), castPref.getIntent());
            if (info != null) {
                try {
                    final Context targetContext = getContext()
                            .createPackageContext(info.resolvePackageName != null
                                    ? info.resolvePackageName : info.activityInfo.packageName, 0);
                    castPref.setIcon(targetContext.getDrawable(info.iconResourceId));
                } catch (Resources.NotFoundException | PackageManager.NameNotFoundException
                        | SecurityException e) {
                    Log.e(TAG, "Cast settings icon not found", e);
                }
                castPref.setTitle(info.activityInfo.loadLabel(getContext().getPackageManager()));
            }
        }
    }

    private void updateGoogleSettings() {
        final Preference googleSettingsPref = findPreference(KEY_GOOGLE_SETTINGS);
        if (googleSettingsPref != null) {
            final ResolveInfo info = systemIntentIsHandled(getContext(),
                    googleSettingsPref.getIntent());
            googleSettingsPref.setVisible(info != null);
            if (info != null && info.activityInfo != null) {
                googleSettingsPref.setIcon(
                    info.activityInfo.loadIcon(getContext().getPackageManager()));
                googleSettingsPref.setTitle(
                    info.activityInfo.loadLabel(getContext().getPackageManager()));
            }

            final Preference speechPref = findPreference(KEY_SPEECH_SETTINGS);
            if (speechPref != null) {
                speechPref.setVisible(info == null);
            }
            final Preference searchPref = findPreference(KEY_SEARCH_SETTINGS);
            if (searchPref != null) {
                searchPref.setVisible(info == null);
            }
        }
    }

    /**
     * Returns the ResolveInfo for the system activity that matches given intent filter or null if
     * no such activity exists.
     * @param context Context of the caller
     * @param intent The intent matching the desired system app
     * @return ResolveInfo of the matching activity or null if no match exists
     */
    public static ResolveInfo systemIntentIsHandled(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }

        final PackageManager pm = context.getPackageManager();

        for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            if (info.activityInfo != null
                    && (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                    == ApplicationInfo.FLAG_SYSTEM) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void onSuggestionReady(List<Suggestion> data) {
        if (data == null || data.size() == 0) {
            if (mSuggestionsList != null) {
                getPreferenceScreen().removePreference(mSuggestionsList);
                mSuggestionsList = null;
            }
            return;
        }

        if (mSuggestionsList == null) {
            mSuggestionsList = new PreferenceCategory(this.getPreferenceManager().getContext());
            mSuggestionsList.setKey(KEY_SUGGESTIONS_LIST);
            mSuggestionsList.setTitle(R.string.header_category_suggestions);
            int firstOrder = getPreferenceScreen().getPreference(0).getOrder();
            mSuggestionsList.setOrder(firstOrder - 1);
            getPreferenceScreen().addPreference(mSuggestionsList);
        }
        updateSuggestionList(data);
    }

    private void updateSuggestionList(List<Suggestion> suggestions) {
        // Remove suggestions that are not in the new list.
        for (int i = 0; i < mSuggestionsList.getPreferenceCount(); i++) {
            SuggestionPreference pref = (SuggestionPreference) mSuggestionsList.getPreference(i);
            boolean isInNewSuggestionList = false;
            for (Suggestion suggestion : suggestions) {
                if (pref.getId().equals(suggestion.getId())) {
                    isInNewSuggestionList = true;
                    break;
                }
            }
            if (!isInNewSuggestionList) {
                mSuggestionsList.removePreference(pref);
            }
        }

        // Add suggestions that are not in the old list.
        for (Suggestion suggestion : suggestions) {
            Preference curPref = findPreference(
                        SuggestionPreference.SUGGESTION_PREFERENCE_KEY + suggestion.getId());
            if (curPref == null) {
                mSuggestionsList.addPreference(new SuggestionPreference(suggestion,
                            this.getPreferenceManager().getContext(), mSuggestionControllerMixin));
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ComponentName componentName = new ComponentName(
                "com.android.settings.intelligence",
                "com.android.settings.intelligence.suggestions.SuggestionService");
        mSuggestionControllerMixin = new SuggestionControllerMixin(
                                            context, this, getLifecycle(), componentName);
    }
}
