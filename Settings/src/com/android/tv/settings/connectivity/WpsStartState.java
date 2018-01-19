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
import android.content.Context;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.setup.WifiConnectivityGuidedStepFragment;
import com.android.tv.settings.connectivity.util.State;

import java.util.List;

/**
 * State responsible for choosing two different WPS methods.
 */
public class WpsStartState implements State {
    private Fragment mFragment;
    private FragmentActivity mActivity;

    public WpsStartState(FragmentActivity activity) {
        mActivity = activity;
    }

    @Override
    public void processForward() {
        mFragment = new WpsStartFragment();
        FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, true);
        }
    }

    @Override
    public void processBackward() {
        mFragment = new WpsStartFragment();
        FragmentChangeListener listener = (FragmentChangeListener) mActivity;
        if (listener != null) {
            listener.onFragmentChange(mFragment, false);
        }
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    /**
     * Fragment for showing two different methods of WPS.
     */
    public static class WpsStartFragment extends WifiConnectivityGuidedStepFragment {
        private WpsFlowInfo mWpsFlowInfo;

        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.wifi_wps_start_title),
                    null,
                    null,
                    null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mWpsFlowInfo = ViewModelProviders
                    .of(getActivity())
                    .get(WpsFlowInfo.class);
            mWpsFlowInfo.getWifiManager().cancelWps(null);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_wps_action_push_button)
                    .id(WpsInfo.PBC)
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .title(R.string.wifi_wps_action_pin_entry)
                    .id(WpsInfo.DISPLAY)
                    .build());
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (getActions() ==  null) return;
            for (int i = 0; i < getActions().size(); i++) {
                if (getActions().get(i).getId() == mWpsFlowInfo.getWpsMethod()) {
                    setSelectedActionPosition(i);
                    break;
                }
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            mWpsFlowInfo.setWpsComplete(false);
            int wpsMethod = (int) action.getId();
            WpsInfo wpsConfig = new WpsInfo();
            wpsConfig.setup = wpsMethod;
            mWpsFlowInfo.setWpsMethod(wpsMethod);
            mWpsFlowInfo.getWifiManager().startWps(wpsConfig, mWpsFlowInfo.getWpsCallback());
        }
    }
}
