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

package com.android.tv.settings.device.apps;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.android.settingslib.applications.PermissionsInfo;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.List;

public class PermissionsActivity extends SettingsLayoutActivity
        implements PermissionsInfo.Callback {

    private static final String TAG = "PermissionsActivity";

    private final PermissionsLayoutGetter mPermissionsLayoutGetter = new PermissionsLayoutGetter();
    private PermissionsInfo mPermissionsInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionsInfo = new PermissionsInfo(this, this);
    }

    @Override
    public Layout createLayout() {
        // TODO: different icon
        return new Layout()
                .breadcrumb(getString(R.string.system_apps))
                .add(new Layout.Header.Builder(getResources())
                        .title(R.string.device_apps_permissions)
                        .icon(R.drawable.ic_settings_security)
                        .build()
                        .add(mPermissionsLayoutGetter));
    }

    @Override
    public void onPermissionLoadComplete() {
        mPermissionsLayoutGetter.refreshView();
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        final Intent intent = action.getIntent();
        if (intent != null) {
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity not found", e);
            }
        }
    }

    private class PermissionsLayoutGetter extends Layout.LayoutGetter {

        @Override
        public Layout get() {
            if (mPermissionsInfo == null) {
                return new Layout();
            }

            final Resources res = getResources();
            final Layout layout = new Layout();
            List<PermissionsInfo.PermissionGroup> groups = mPermissionsInfo.getGroups();
            for (final PermissionsInfo.PermissionGroup group : groups) {
                if (group.possibleApps.size() == 0) {
                    continue;
                }
                final Drawable tintedIcon = group.icon.mutate();
                tintedIcon.setColorFilter(
                        new PorterDuffColorFilter(getColor(R.color.permissions_color_overlay),
                        PorterDuff.Mode.SRC_ATOP));
                final Intent intent = new Intent(Intent.ACTION_MANAGE_PERMISSION_APPS)
                        .putExtra(Intent.EXTRA_PERMISSION_NAME, group.name);
                layout.add(new Layout.Action.Builder(res, intent)
                        .title(group.label)
                        .icon(tintedIcon)
                        .description(getString(R.string.app_permissions_group_summary,
                                group.grantedApps.size(), group.possibleApps.size()))
                        .build());
            }
            return layout;
        }
    }
}
