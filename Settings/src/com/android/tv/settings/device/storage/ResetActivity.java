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
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.android.tv.settings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import java.util.List;

public class ResetActivity extends FragmentActivity {

  private static final String TAG = "ResetActivity";

  /**
   * Support for shutdown-after-reset. If our launch intent has a true value for the boolean extra
   * under the following key, then include it in the intent we use to trigger a factory reset. This
   * will cause us to shut down instead of restart after the reset.
   */
  private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      getSupportFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, FactoryResetDialogFragment.newInstance(this))
          .commitAllowingStateLoss();
    }
  }

  /** Confirmation dialog for Factory Reset */
  public static class FactoryResetDialogFragment extends FullScreenDialogFragment {
    static FactoryResetDialogFragment newInstance(Context context) {
      Bundle args =
          new DialogBuilder()
              .setIcon(
                  Icon.createWithResource(context, R.drawable.ic_settings_backup_restore_132dp))
              .setTitle(context.getString(R.string.device_reset))
              .setPositiveButton(context.getString(R.string.device_reset))
              .setNegativeButton(context.getString(R.string.settings_cancel))
              .build();

      FactoryResetDialogFragment fragment = new FactoryResetDialogFragment();
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
    }

    @Override
    public CharSequence getMessage() {
      return getContext().getString(R.string.factory_reset_description);
    }

    @Override
    public void onButtonPressed(int action) {
      if (action == ACTION_POSITIVE) {
        getActivity()
            .getSupportFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, ResetConfirmDialogFragment.newInstance(getActivity()))
            .commitAllowingStateLoss();
      } else {
        getActivity().finish();
      }
    }
  }

  public static class ResetConfirmDialogFragment extends FullScreenDialogFragment {
    public static ResetConfirmDialogFragment newInstance(Context context) {
      Bundle args =
          new DialogBuilder()
              .setIcon(
                  Icon.createWithResource(context, R.drawable.ic_settings_backup_restore_132dp))
              .setTitle(context.getString(R.string.device_reset))
              .setPositiveButton(context.getString(R.string.device_reset))
              .setNegativeButton(context.getString(R.string.settings_cancel))
              .build();
      ResetConfirmDialogFragment fragment = new ResetConfirmDialogFragment();
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      View positiveButton = view.findViewById(R.id.positive_button);
      positiveButton.requestFocus();
    }

    @Override
    public CharSequence getMessage() {
      return getContext().getString(R.string.confirm_factory_reset_description);
    }

    @Override
    public void onButtonPressed(int action) {
      if (action == ACTION_POSITIVE) {
        if (ActivityManager.isUserAMonkey()) {
          Log.v(TAG, "Monkey tried to erase the device. Bad monkey, bad!");
          getActivity().finish();
        } else {
          performFactoryReset();
        }
      } else if (action == ACTION_NEGATIVE) {
        getActivity().finish();
      } else {
        Log.wtf(TAG, "Unknown action clicked");
      }
    }

    private void performFactoryReset() {
      final PersistentDataBlockManager pdbManager =
          (PersistentDataBlockManager)
              getContext().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
      // Disable actions in the fragment as the wipe of the persistent data block
      // and ACTION_FACTORY_RESET broadcast can take some time on some devices.
      View positiveButton = getActivity().findViewById(R.id.positive_button);
      View negativeButton = getActivity().findViewById(R.id.negative_button);
      positiveButton.setFocusable(false);
      positiveButton.setEnabled(false);
      negativeButton.setEnabled(false);
      negativeButton.setFocusable(false);


      if (shouldWipePersistentDataBlock(pdbManager)) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            pdbManager.wipe();
            return null;
          }

          @Override
          protected void onPostExecute(Void aVoid) {
            doMainClear();
          }
        }.execute();
      } else {
        doMainClear();
      }
    }

    private boolean shouldWipePersistentDataBlock(PersistentDataBlockManager pdbManager) {
      if (pdbManager == null) {
        return false;
      }
      // If OEM unlock is allowed, the persistent data block will be wiped during FR.
      // If disabled, it will be wiped here instead.
      if (((OemLockManager) getActivity().getSystemService(Context.OEM_LOCK_SERVICE))
          .isOemUnlockAllowed()) {
        return false;
      }
      return true;
    }

    private void doMainClear() {
      if (getActivity() == null) {
        return;
      }
      Intent resetIntent = new Intent(Intent.ACTION_FACTORY_RESET);
      resetIntent.setPackage("android");
      resetIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
      resetIntent.putExtra(Intent.EXTRA_REASON, "ResetConfirmDialogFragment");
      if (getActivity().getIntent().getBooleanExtra(SHUTDOWN_INTENT_EXTRA, false)) {
        resetIntent.putExtra(SHUTDOWN_INTENT_EXTRA, true);
      }
      getActivity().sendBroadcastAsUser(resetIntent, UserHandle.SYSTEM);
    }
  }
}
