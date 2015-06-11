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

package com.android.tv.settings.device.storage;

import android.annotation.Nullable;
import android.os.Bundle;
import android.view.View;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.ProgressDialogFragment;

public class UnmountProgressFragment extends ProgressDialogFragment {

    private static final String ARG_DESCRIPTION = "description";

    public static UnmountProgressFragment newInstance(CharSequence description) {
        final Bundle b = new Bundle(1);
        b.putCharSequence(ARG_DESCRIPTION, description);
        final UnmountProgressFragment fragment = new UnmountProgressFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final CharSequence description = getArguments().getCharSequence(ARG_DESCRIPTION);
        setTitle(getActivity().getString(R.string.sotrage_wizard_eject_progress_title,
                description));
    }
}
