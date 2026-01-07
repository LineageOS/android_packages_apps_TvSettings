/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.tv.settings;

import static com.google.common.truth.Truth.assertThat;

import android.view.KeyEvent;
import androidx.fragment.app.Fragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class TvSettingsActivityTest {

  @Test
  public void testBackKey_upWithoutDown_ignored() {
    ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class);
    TestActivity activity = controller.create().start().resume().get();

    // Send UP without preceding DOWN
    boolean handled =
        activity.onKeyUp(
            KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));

    // Our logic returns TRUE when ignoring the event
    assertThat(handled).isTrue();

    // Verify Log warning
    assertThat(
            ShadowLog.getLogsForTag("TvSettingsActivity").stream()
                .anyMatch(log -> log.msg.contains("Ignore back key up event")))
        .isTrue();
  }

  @Test
  public void testBackKey_downThenUp_processed() {
    ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class);
    TestActivity activity = controller.create().start().resume().get();

    // Send DOWN
    activity.onKeyDown(
        KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));

    // Send UP
    activity.onKeyUp(
        KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));

    // Verify Log warning is NOT present (meaning we passed through the check)
    assertThat(
            ShadowLog.getLogsForTag("TvSettingsActivity").stream()
                .anyMatch(log -> log.msg.contains("Ignore back key up event")))
        .isFalse();
  }

  private static class TestActivity extends TvSettingsActivity {
    @Override
    protected Fragment createSettingsFragment() {
      return new Fragment();
    }
  }
}
