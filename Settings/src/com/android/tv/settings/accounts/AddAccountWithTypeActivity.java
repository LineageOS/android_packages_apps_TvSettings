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

package com.android.tv.settings.accounts;

import com.google.android.gsf.GoogleLoginServiceConstants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.SystemProperties;
import android.util.Log;

import java.io.IOException;

public class AddAccountWithTypeActivity extends Activity {

    // Must match com.google.android.gms.common.AccountPicker.
    public static final String EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY = "allowableAccountTypes";

    private static final String TAG = "AddAccountWithType";

    private static final String CHOOSE_ACCOUNT_TYPE_ACTION =
            "com.google.android.gms.common.account.CHOOSE_ACCOUNT_TYPE";
    private static final String SETUP_WRAITH_INTENT_NAME =
            "com.google.android.tungsten.setupwraith.ADD_ACCOUNT_FLOW";
    private static final String EXTRA_IS_SETUP_WIZARD = "firstRun";
    private static final String EXTRA_USE_TRANSPARENT_THEME = "useTransparentTheme";
    private static final String EXTRA_SHOW_SUMMARY = "showSummary";
    private static final String EXTRA_SHOW_SKIP = "showSkip";
    private static final String EXTRA_OFFLINE = "isOffline";
    private static final String USE_SUW_ACCT_FLOW_PROPERTY = "useSuwAcctFlow";

    private static final int REQUEST_CHOOSE_ACCOUNT_TYPE = 0;
    private static final int REQUEST_ADD_ACCOUNT = 1;
    private static final int REQUEST_SETUP_WRAITH_ADD_ACCOUNT = 2;

    private final AccountManagerCallback<Bundle> mCallback = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Intent addAccountIntent = future.getResult()
                        .getParcelable(AccountManager.KEY_INTENT);
                if (addAccountIntent == null) {
                    Log.e(TAG, "Failed to retrieve add account intent from authenticator");
                    setResultAndFinish(Activity.RESULT_CANCELED);
                } else {
                    startActivityForResult(addAccountIntent, REQUEST_ADD_ACCOUNT);
                }
            } catch (IOException | AuthenticatorException | OperationCanceledException e) {
                Log.e(TAG, "Failed to get add account intent: ", e);
                setResultAndFinish(Activity.RESULT_CANCELED);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String accountType = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        if (accountType != null) {
            startAddAccount(accountType);
        } else {
            String[] allowedTypes = getIntent().getStringArrayExtra(
                    EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY);
            startAccountTypePicker(allowedTypes);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User selected an account type, so kick off the add account flow for that.
        if (requestCode == REQUEST_CHOOSE_ACCOUNT_TYPE && resultCode == Activity.RESULT_OK) {
            String accountType = data.getExtras().getString(AccountManager.KEY_ACCOUNT_TYPE);
            startAddAccount(accountType);
        } else {
            setResultAndFinish(resultCode);
        }
    }

    private void startAccountTypePicker(String[] allowedTypes) {
        Intent i = new Intent(CHOOSE_ACCOUNT_TYPE_ACTION);
        i.putExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY, allowedTypes);
        startActivityForResult(i, REQUEST_CHOOSE_ACCOUNT_TYPE);
    }

    private void startAddAccount(String accountType) {
        // If the account type is google account, then start up decision screen in SUW
        // otherwise call addAccount.
        if (accountType.equals(GoogleLoginServiceConstants.ACCOUNT_TYPE)) {
            // TODO(joshualambert): Remove SystemProperty conditional and use startActivityForResult
            // only when the MinuteMaid changes in GMSCore are available in master (to avoid calls
            // to NTAD).
            if (SystemProperties.getInt(USE_SUW_ACCT_FLOW_PROPERTY, 0) == 1) {
                // Build Intent, call to SUW
                Intent accountFlowIntent = buildSetupWraithIntent();
                startActivityForResult(accountFlowIntent, REQUEST_SETUP_WRAITH_ADD_ACCOUNT);
            } else {
                AccountManager.get(this).addAccount(
                        accountType,
                        null, /* authTokenType */
                        null, /* requiredFeatures */
                        null, /* accountOptions */
                        null, mCallback, null);
            }
        } else {
            AccountManager.get(this).addAccount(
                    accountType,
                    null, /* authTokenType */
                    null, /* requiredFeatures */
                    null, /* accountOptions */
                    null, mCallback, null);
        }
    }

    private Intent buildSetupWraithIntent() {
        Intent intent = new Intent(SETUP_WRAITH_INTENT_NAME);
        intent.putExtra(EXTRA_IS_SETUP_WIZARD, false);
        intent.putExtra(EXTRA_SHOW_SUMMARY, false);
        intent.putExtra(EXTRA_OFFLINE, false);
        intent.putExtra(EXTRA_SHOW_SKIP, false);
        return intent;
    }

    private void setResultAndFinish(int resultCode) {
        setResult(resultCode);
        finish();
    }
}
