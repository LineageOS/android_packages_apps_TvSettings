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

package com.android.tv.settings.device.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.storage.DiskInfo;

import com.android.tv.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class FormatActivityFormatReceiverTest {

    @Mock
    private FormatActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn("resource string").when(mActivity).getString(anyInt());
        doReturn("resource string").when(mActivity).getString(anyInt(), any());
    }

    @Test
    public void testFormatAsPrivate_successResumed() {
        doReturn(true).when(mActivity).isResumed();
        mActivity.mFormatAsPrivateDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PRIVATE);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "asdf");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, true);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, atLeastOnce()).handleFormatAsPrivateComplete(123L, 456L);
    }

    @Test
    public void testFormatAsPrivate_successNotResumed() {
        doReturn(false).when(mActivity).isResumed();
        mActivity.mFormatAsPrivateDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PRIVATE);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "asdf");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, true);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, never()).handleFormatAsPrivateComplete(123L, 456L);
    }

    @Test
    public void testFormatAsPrivate_failure() {
        doReturn(true).when(mActivity).isResumed();
        mActivity.mFormatAsPrivateDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PRIVATE);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "asdf");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, false);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, never()).handleFormatAsPrivateComplete(123L, 456L);
        verify(mActivity, atLeastOnce()).finish();
    }

    @Test
    public void testFormatAsPrivate_wrongDisk() {
        doReturn(true).when(mActivity).isResumed();
        mActivity.mFormatAsPrivateDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PRIVATE);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "jkl;");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, true);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, never()).handleFormatAsPrivateComplete(123L, 456L);
        verify(mActivity, never()).finish();
    }

    @Test
    public void testFormatAsPublic_success() {
        mActivity.mFormatAsPublicDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PUBLIC);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "asdf");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, true);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, atLeastOnce()).finish();
    }

    @Test
    public void testFormatAsPublic_wrongDisk() {
        mActivity.mFormatAsPublicDiskId = "asdf";

        final Intent intent = new Intent(SettingsStorageService.ACTION_FORMAT_AS_PUBLIC);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, "jkl;");
        intent.putExtra(SettingsStorageService.EXTRA_PRIVATE_BENCH, 123L);
        intent.putExtra(SettingsStorageService.EXTRA_INTERNAL_BENCH, 456L);
        intent.putExtra(SettingsStorageService.EXTRA_SUCCESS, true);

        new FormatActivity.FormatReceiver(mActivity).onReceive(mActivity, intent);

        verify(mActivity, never()).finish();
    }
}
