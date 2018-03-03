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

import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.telephony.SignalStrength;

import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;
import com.android.tv.settings.connectivity.ConnectivityListener;
import com.android.tv.settings.suggestions.SuggestionPreference;
import com.android.tv.settings.system.SecurityFragment;

import java.util.List;

/**
 * The fragment where all good things begin. Evil is handled elsewhere.
 */
public class MainFragment extends SettingsPreferenceFragment implements
        SuggestionControllerMixin.SuggestionControllerHost {
    private static final String TAG = "MainFragment";

    private static final String KEY_SUGGESTIONS_LIST = "suggestions";
    @VisibleForTesting
    static final String KEY_ACCOUNTS_AND_SIGN_IN = "accounts_and_sign_in";
    private static final String KEY_APPLICATIONS = "applications";
    @VisibleForTesting
    static final String KEY_NETWORK = "network";

    @VisibleForTesting
    ConnectivityListener mConnectivityListener;
    private PreferenceCategory mSuggestionsList;
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private IconCache mIconCache;

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
        mIconCache = new IconCache(getContext());
        mConnectivityListener =
                new ConnectivityListener(getContext(), this::updateWifi, getLifecycle());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_prefs, null);
        if (isRestricted()) {
            Preference appPref = findPreference(KEY_APPLICATIONS);
            if (appPref != null) {
                appPref.setVisible(false);
            }
            Preference accountsPref = findPreference(KEY_ACCOUNTS_AND_SIGN_IN);
            if (accountsPref != null) {
                accountsPref.setVisible(false);
            }
        }
    }

    @VisibleForTesting
    void updateWifi() {
        final Preference networkPref = findPreference(KEY_NETWORK);
        if (networkPref == null) {
            return;
        }

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
            networkPref.setIcon(R.drawable.ic_wifi_not_connected);
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
                SuggestionPreference newSuggPref = new SuggestionPreference(suggestion,
                            this.getPreferenceManager().getContext(), mSuggestionControllerMixin);
                newSuggPref.setIcon(mIconCache.getIcon(suggestion.getIcon()));
                newSuggPref.setTitle(suggestion.getTitle());
                newSuggPref.setSummary(suggestion.getSummary());
                mSuggestionsList.addPreference(newSuggPref);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccountIcon();
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext());
    }

    @VisibleForTesting
    void updateAccountIcon() {
        final Preference accountsPref = findPreference(KEY_ACCOUNTS_AND_SIGN_IN);
        if (accountsPref != null && accountsPref.isVisible()) {
            final AccountManager am = AccountManager.get(getContext());
            if (am.getAccounts().length > 0) {
                accountsPref.setIcon(R.drawable.ic_accounts_and_sign_in);
            } else {
                accountsPref.setIcon(R.drawable.ic_add_an_account);
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
