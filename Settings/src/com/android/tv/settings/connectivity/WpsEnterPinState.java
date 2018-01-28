/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings.connectivity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.util.State;
import com.android.tv.settings.dialog.ProgressDialogSupportFragment;

/**
 * State responsible for showing "enter pin" page.
 */
public class WpsEnterPinState implements State {
    private Fragment mFragment;
    private FragmentActivity mActivity;
    public WpsEnterPinState(FragmentActivity activity) {
        mActivity = activity;
    }

    @Override
    public void processForward() {
        WpsFlowInfo wpsInfo = ViewModelProviders.of(mActivity).get(WpsFlowInfo.class);
        mFragment = WpsPinFragment.newInstance(wpsInfo.getPin());
        State.FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, true);
        }
    }

    @Override
    public void processBackward() {
        WpsFlowInfo wpsInfo = ViewModelProviders.of(mActivity).get(WpsFlowInfo.class);
        mFragment = WpsPinFragment.newInstance(wpsInfo.getPin());
        State.FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, false);
        }
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    /**
     * Displays a UI for showing that the user must enter a PIN for WPS to continue
     */
    public static class WpsPinFragment extends ProgressDialogSupportFragment {

        private static final String KEY_PIN = "pin";

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            FrameLayout holder = new FrameLayout(getActivity());
            holder.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            holder.addView(view);
            return holder;
        }

        public static WpsPinFragment newInstance(String pin) {
            WpsPinFragment fragment = new WpsPinFragment();
            Bundle args = new Bundle(1);
            args.putString(KEY_PIN, pin);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setTitle(getString(R.string.wifi_wps_onstart_pin,
                    getArguments().getString(KEY_PIN)));
            setSummary(R.string.wifi_wps_onstart_pin_description);
            setIcon(R.drawable.setup_wps);
        }
    }
}
