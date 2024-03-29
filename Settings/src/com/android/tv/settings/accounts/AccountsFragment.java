/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tv.settings.accounts;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.widget.FooterPreference;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.FlavorUtils;
import com.android.tv.settings.system.SecurityFragment;

import java.util.ArrayList;
import java.util.Set;

/**
 * The "Accounts and Sign in" screen in TV settings.
 */
@Keep
public class AccountsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AccountsFragment";
    private static final String KEY_ADD_ACCOUNT = "add_account";
    private static final String KEY_DEVICE_OWNER_FOOTER = "do_org_footer";
    private static final int ORDER_ADD_ACCOUNT = Integer.MAX_VALUE - 2;
    private static final int ORDER_FOOTER = Integer.MAX_VALUE - 1;
    private AuthenticatorHelper mAuthenticatorHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mAuthenticatorHelper = new AuthenticatorHelper(getContext(),
                new UserHandle(UserHandle.myUserId()), userHandle -> updateAccounts());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.accounts, null);
        refreshAllPreferences();
    }

    private void refreshAllPreferences() {
        final PreferenceScreen prefScreen = getPreferenceScreen();
        for (int i = 0; i < prefScreen.getPreferenceCount();) {
            final Preference preference = prefScreen.getPreference(i);
            final String key = preference.getKey();
            if (TextUtils.equals(KEY_ADD_ACCOUNT, key)
                    || TextUtils.equals(KEY_DEVICE_OWNER_FOOTER, key)) {
                i++;
            } else {
                prefScreen.removePreference(preference);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuthenticatorHelper.listenToAccountUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccounts();
    }

    private void updateAccounts() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        final Set<String> touchedAccounts = new ArraySet<>(prefScreen.getPreferenceCount());

        final AccountManager am = AccountManager.get(getContext());
        final AuthenticatorDescription[] authTypes = am.getAuthenticatorTypes();
        final ArrayList<String> allowableAccountTypes = new ArrayList<>(authTypes.length);
        final Context themedContext = getPreferenceManager().getContext();

        for (AuthenticatorDescription authDesc : authTypes) {
            Context targetContext = getTargetContext(getContext(), authDesc);
            if (targetContext == null) {
                continue;
            }

            String authTitle = getAuthTitle(targetContext, authDesc);


            Account[] accounts = am.getAccountsByType(authDesc.type);
            if (accounts == null || accounts.length == 0) {
                continue;  // No point in continuing; there aren't any accounts to show.
            }

            Drawable authImage = getAuthImage(targetContext, authDesc);

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

                prefScreen.addPreference(preference);
            }
        }

        for (int i = 0; i < prefScreen.getPreferenceCount();) {
            final Preference preference = prefScreen.getPreference(i);
            final String key = preference.getKey();
            if (touchedAccounts.contains(key) || TextUtils.equals(KEY_ADD_ACCOUNT, key)
                    || TextUtils.equals(KEY_DEVICE_OWNER_FOOTER, key)) {
                i++;
            } else {
                prefScreen.removePreference(preference);
            }
        }

        // Never allow restricted profile to add accounts.
        final Preference addAccountPref = findPreference(KEY_ADD_ACCOUNT);
        if (addAccountPref != null) {
            addAccountPref.setOrder(ORDER_ADD_ACCOUNT);
            if (!AccountsUtil.isAdminRestricted(getContext())) {
                if (isRestricted()) {
                    addAccountPref.setVisible(false);
                } else {
                    setUpAddAccountPrefIntent(addAccountPref, getContext());
                }
            }
        }

        // Show device managed footer information if DO active
        final FooterPreference footerPref = findPreference(KEY_DEVICE_OWNER_FOOTER);
        if (footerPref != null) {
            final CharSequence deviceOwnerDisclosure = FlavorUtils.getFeatureFactory(
                    getContext()).getEnterprisePrivacyFeatureProvider(
                    getContext()).getDeviceOwnerDisclosure();
            footerPref.setTitle(deviceOwnerDisclosure);
            footerPref.setOrder(ORDER_FOOTER);
            final Context context = getContext();
            footerPref.setLearnMoreAction(view ->
                context.startActivity(new Intent(Settings.ACTION_ENTERPRISE_PRIVACY_SETTINGS))
            );
            final String learnMoreText = context.getString(R.string.learn_more);
            footerPref.setLearnMoreText(learnMoreText);
            footerPref.setVisible(deviceOwnerDisclosure != null);
        }
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext()) || UserManager.get(
                getContext()).hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS);
    }

    /**
     * Set up the intent and visibility for the given preference based on the information from
     * AccountManager.
     */
    public static void setUpAddAccountPrefIntent(Preference preference, Context context) {
        final AccountManager am = AccountManager.get(context);
        final AuthenticatorDescription[] authTypes = am.getAuthenticatorTypes();
        final ArrayList<String> allowableAccountTypes = new ArrayList<>(authTypes.length);
        for (AuthenticatorDescription authDesc : authTypes) {
            final Context targetContext = getTargetContext(context, authDesc);
            if (targetContext == null) {
                continue;
            }
            String authTitle = getAuthTitle(targetContext, authDesc);
            if (authTitle != null || authDesc.iconId != 0) {
                allowableAccountTypes.add(authDesc.type);
            }
        }

        Intent i = new Intent().setComponent(new ComponentName("com.android.tv.settings",
                "com.android.tv.settings.accounts.AddAccountWithTypeActivity"));
        i.putExtra(AddAccountWithTypeActivity.EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY,
                allowableAccountTypes.toArray(new String[allowableAccountTypes.size()]));

                // If there are available account types, show the "add account" button.
        preference.setVisible(!allowableAccountTypes.isEmpty());
        preference.setIntent(i);
        preference.setOnPreferenceClickListener(
                preference1 -> {
                    logEntrySelected(TvSettingsEnums.ACCOUNT_CLASSIC_ADD_ACCOUNT);
                    return false;
                });
    }

    private static Context getTargetContext(Context context, AuthenticatorDescription authDesc) {
        Context targetContext = null;
        try {
            targetContext = context.createPackageContext(authDesc.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Authenticator description with bad package name", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception loading package resources", e);
        }
        return targetContext;
    }

    private static String getAuthTitle(Context targetContext, AuthenticatorDescription authDesc) {
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
        return authTitle;
    }

    private static Drawable getAuthImage(Context targetContext, AuthenticatorDescription authDesc) {
        // Icon URI to be displayed for each account is based on the type of authenticator.
        Drawable authImage = null;
        try {
            authImage = targetContext.getDrawable(authDesc.iconId);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Authenticator has bad resources", e);
        }
        return authImage;
    }

}
