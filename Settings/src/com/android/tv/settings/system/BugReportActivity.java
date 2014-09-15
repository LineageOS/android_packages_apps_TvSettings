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

package com.android.tv.settings.system;

import com.android.tv.settings.R;

import com.android.tv.settings.connectivity.TimedMessageWizardFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import android.app.ActivityManagerNative;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Takes bug reports.
 */
public class BugReportActivity extends DialogActivity
        implements TimedMessageWizardFragment.Listener {

    private static final String TAG = "BugReportActivity";
    static final String EXTRA_BUG_REPORT_COMPLETE = "bug_report_complete";
    static final String EXTRA_BUG_REPORT_URI = "bug_report_complete_uri";
    static final String EXTRA_BUG_REPORT_SCREEN_SHOT_URI = "bug_report_screen_shot";
    private static final String CATEGORY_TAG = "canvas-ui";

    private static final int TIMED_MESSAGE_TIME_OUT_MS = 5 * 1000;

    /**
     * Returns {@code true} if bug reporting via Google feedback is enabled.
     */
    public static boolean isBugReportEnabled() {
        return !Build.TYPE.equals("user");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isBugReportEnabled()) {
            finish();
        } else {
            boolean calledInternally = TextUtils.isEmpty(getIntent().getAction());

            if (calledInternally) {
                setLayoutProperties(R.layout.setup_auth_activity, R.id.description, R.id.action);
                super.onCreate(savedInstanceState);
                findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
            }

            boolean bugReportInitiated = false;
            try {
                ActivityManagerNative.getDefault().requestBugReport();
                bugReportInitiated = true;
            } catch (RemoteException e) {
                Log.e(TAG, "Could not execute bug report command " + e);
            }

            if (bugReportInitiated) {
                if (calledInternally) {
                    displayFragment(TimedMessageWizardFragment.newInstance(
                            getString(R.string.system_collecting_bug_report),
                            TIMED_MESSAGE_TIME_OUT_MS));
                } else {
                    Toast.makeText(this, getString(R.string.system_collecting_bug_report),
                            Toast.LENGTH_LONG).show();
                }
            } else {
                if (calledInternally) {
                    displayFragment(TimedMessageWizardFragment.newInstance(
                            getString(R.string.system_collecting_bug_report_error),
                            TIMED_MESSAGE_TIME_OUT_MS));
                } else {
                    Toast.makeText(this, getString(R.string.system_collecting_bug_report_error),
                            Toast.LENGTH_LONG).show();
                }
            }

            if (!calledInternally) {
                finish();
                super.onCreate(savedInstanceState);
                getContentView().setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onTimedMessageCompleted() {
        finish();
    }

    private void displayFragment(Fragment f) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(android.R.id.content, f).commit();
    }
}
