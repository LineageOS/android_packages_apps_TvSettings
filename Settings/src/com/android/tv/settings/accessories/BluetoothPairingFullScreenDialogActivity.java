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

package com.android.tv.settings.accessories;

import static com.android.tv.settings.accessories.AccessoryUtils.getHtmlEscapedDeviceName;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.tv.settings.FullScreenDialogFragment;
import com.android.tv.settings.R;
import java.util.Locale;

/**
 * BluetoothPairingDialog asks the user to enter a PIN / Passkey / simple confirmation for pairing
 * with a remote Bluetooth device.
 */
public class BluetoothPairingFullScreenDialogActivity extends FragmentActivity {

  private static final String KEY_PAIR = "action_pair";
  private static final String KEY_CANCEL = "action_cancel";

  private static final String TAG = "BluetoothPairingDialog";
  private static final boolean DEBUG = false;

  private static final int BLUETOOTH_PIN_MAX_LENGTH = 16;
  private static final int BLUETOOTH_PASSKEY_MAX_LENGTH = 6;

  @SuppressWarnings("unused")
  private LocalBluetoothManager mLocalBtManager;

  private BluetoothDevice mDevice;
  private int mType;
  private String mPairingKey;
  private boolean mPairingInProgress = false;

  /**
   * Dismiss the dialog if the bond state changes to bonded or none, or if pairing was canceled for
   * {@link #mDevice}.
   */
  private final BroadcastReceiver mReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          if (DEBUG) {
            Log.d(TAG, "onReceive. Broadcast Intent = " + intent.toString());
          }
          if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            int bondState =
                intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (bondState == BluetoothDevice.BOND_BONDED
                || bondState == BluetoothDevice.BOND_NONE) {
              dismiss();
            }
          } else if (BluetoothDevice.ACTION_PAIRING_CANCEL.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || device.equals(mDevice)) {
              dismiss();
            }
          }
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();
    if (!BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
      Log.e(
          TAG,
          "Error: this activity may be started only with intent "
              + BluetoothDevice.ACTION_PAIRING_REQUEST);
      finish();
      return;
    }

    // LocalBluetoothManager monitors UUIDs and triggers HID host connection.
    mLocalBtManager = AccessoryUtils.getLocalBluetoothManager(this);

    mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

    if (DEBUG) {
      Log.d(TAG, "Requested pairing Type = " + mType + " , Device = " + mDevice);
    }

    switch (mType) {
      case BluetoothDevice.PAIRING_VARIANT_PIN:
      case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
        createUserEntryDialog();
        break;

      case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
        int passkey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
        if (passkey == BluetoothDevice.ERROR) {
          Log.e(TAG, "Invalid Confirmation Passkey received, not showing any dialog");
          finish();
          return;
        }
        mPairingKey = String.format(Locale.US, "%06d", passkey);
        createConfirmationDialog();
        break;

      case BluetoothDevice.PAIRING_VARIANT_CONSENT:
      case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
        createConfirmationDialog();
        break;

      case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
      case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
        int pairingKey =
            intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
        if (pairingKey == BluetoothDevice.ERROR) {
          Log.e(TAG, "Invalid Confirmation Passkey or PIN received, not showing any dialog");
          finish();
          return;
        }
        if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
          mPairingKey = String.format("%06d", pairingKey);
        } else {
          mPairingKey = String.format("%04d", pairingKey);
        }
        createConfirmationDialog();
        break;

      default:
        Log.e(TAG, "Incorrect pairing type received, not showing any dialog");
        finish();
        return;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_PAIRING_CANCEL);
    filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    registerReceiver(mReceiver, filter);
  }

  @Override
  protected void onPause() {
    unregisterReceiver(mReceiver);

    // Finish the activity if we get placed in the background and cancel pairing
    if (!mPairingInProgress) {
      cancelPairing();
    }
    dismiss();

    super.onPause();
  }

  @Override
  public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      cancelPairing();
    }
    return super.onKeyDown(keyCode, event);
  }

  private void dismiss() {
    finish();
  }

  private void cancelPairing() {
    if (DEBUG) {
      Log.d(TAG, "cancelPairing");
    }
    mDevice.cancelBondProcess();
  }

  private void createUserEntryDialog() {
    getFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, EntryDialogFragment.newInstance(mDevice, mType))
        .commit();
  }

  public void onActionClicked(String action) {
    if (KEY_PAIR.equals(action)) {
      onPair(null);
      dismiss();
    } else if (KEY_CANCEL.equals(action)) {
      cancelPairing();
    }
  }

  private void createConfirmationDialog() {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(
            android.R.id.content,
            BluetoothPairingConfirmationDialogFragment.newInstance(
                this, mDevice, mPairingKey, mType))
        .commit();
  }

  public void onPair(String value) {
    if (DEBUG) {
      Log.d(TAG, "onPair: " + value);
    }
    switch (mType) {
      case BluetoothDevice.PAIRING_VARIANT_PIN:
        mDevice.setPin(value);
        mPairingInProgress = true;
        break;

      case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
        try {
          int passkey = Integer.parseInt(value);
          mPairingInProgress = true;
        } catch (NumberFormatException e) {
          Log.d(TAG, "pass key " + value + " is not an integer");
        }
        break;

      case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
      case BluetoothDevice.PAIRING_VARIANT_CONSENT:
        mDevice.setPairingConfirmation(true);
        mPairingInProgress = true;
        break;

      case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
      case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
        // Do nothing.
        break;

      case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
        mPairingInProgress = true;
        break;

      default:
        Log.e(TAG, "Incorrect pairing type received");
    }
  }

  // TODO: This is currently the same as BluetoothPairing dialog. We need to update the style.
  public static class EntryDialogFragment extends Fragment {
    private static final String ARG_DEVICE = "ConfirmationDialogFragment.DEVICE";
    private static final String ARG_TYPE = "ConfirmationDialogFragment.TYPE";

    private BluetoothDevice mDevice;
    private int mType;

    public static EntryDialogFragment newInstance(BluetoothDevice device, int type) {
      final EntryDialogFragment fragment = new EntryDialogFragment();
      final Bundle b = new Bundle(2);
      fragment.setArguments(b);
      b.putParcelable(ARG_DEVICE, device);
      b.putInt(ARG_TYPE, type);
      return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      final Bundle args = getArguments();
      mDevice = args.getParcelable(ARG_DEVICE);
      mType = args.getInt(ARG_TYPE);
    }

    @Override
    public @Nullable View onCreateView(
        LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
      final View v = inflater.inflate(R.layout.bt_pairing_passkey_entry, container, false);

      final TextView titleText = (TextView) v.findViewById(R.id.title_text);
      final EditText textInput = (EditText) v.findViewById(R.id.text_input);

      textInput.setOnEditorActionListener(
          new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
              String value = textInput.getText().toString();
              if (actionId == EditorInfo.IME_ACTION_NEXT
                  || (actionId == EditorInfo.IME_NULL
                      && event.getAction() == KeyEvent.ACTION_DOWN)) {
                ((BluetoothPairingFullScreenDialogActivity) getActivity()).onPair(value);
              }
              return true;
            }
          });

      final String instructions;
      final int maxLength;
      switch (mType) {
        case BluetoothDevice.PAIRING_VARIANT_PIN:
          instructions =
              getString(R.string.bluetooth_enter_pin_msg, getHtmlEscapedDeviceName(mDevice));
          final TextView instructionText = (TextView) v.findViewById(R.id.hint_text);
          instructionText.setText(getString(R.string.bluetooth_pin_values_hint));
          // Maximum of 16 characters in a PIN
          maxLength = BLUETOOTH_PIN_MAX_LENGTH;
          textInput.setInputType(InputType.TYPE_CLASS_NUMBER);
          break;

        case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
          instructions =
              getString(R.string.bluetooth_enter_passkey_msg, getHtmlEscapedDeviceName(mDevice));
          // Maximum of 6 digits for passkey
          maxLength = BLUETOOTH_PASSKEY_MAX_LENGTH;
          textInput.setInputType(InputType.TYPE_CLASS_TEXT);
          break;

        default:
          throw new IllegalStateException(
              "Incorrect pairing type for" + " createPinEntryView: " + mType);
      }

      titleText.setText(Html.fromHtml(instructions));

      textInput.setFilters(new InputFilter[] {new LengthFilter(maxLength)});

      return v;
    }
  }

  public static class BluetoothPairingConfirmationDialogFragment extends FullScreenDialogFragment {
    private BluetoothDevice mDevice;
    private String mPairingKey;
    private int mType;

    public static BluetoothPairingConfirmationDialogFragment newInstance(
        Context context, BluetoothDevice bluetoothDevice, String pairingKey, int type) {
      DialogBuilder argsBuilder =
          new DialogBuilder()
              .setIcon(Icon.createWithResource(context, R.drawable.ic_info_outline))
              .setTitle(context.getString(R.string.bluetooth_pairing_request));

      switch (type) {
        case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
        case BluetoothDevice.PAIRING_VARIANT_CONSENT:
        case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
          argsBuilder.setPositiveButton(context.getString(R.string.bluetooth_pair));
          argsBuilder.setNegativeButton(context.getString(R.string.bluetooth_cancel));
          break;
        case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
        case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
          argsBuilder.setNegativeButton(context.getString(R.string.bluetooth_cancel));
          break;
      }

      Bundle args = argsBuilder.build();
      BluetoothPairingConfirmationDialogFragment fragment =
          new BluetoothPairingConfirmationDialogFragment(bluetoothDevice, pairingKey, type);
      fragment.setArguments(args);
      return fragment;
    }

    private BluetoothPairingConfirmationDialogFragment(
        BluetoothDevice bluetoothDevice, String pairingKey, int type) {
      mDevice = bluetoothDevice;
      mPairingKey = pairingKey;
      mType = type;
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
    public void onButtonPressed(int action) {
      if (action == ACTION_POSITIVE) {
        ((BluetoothPairingFullScreenDialogActivity) getActivity()).onActionClicked(KEY_PAIR);
      } else if (action == ACTION_NEGATIVE) {
        ((BluetoothPairingFullScreenDialogActivity) getActivity()).onActionClicked(KEY_CANCEL);
      }
    }

    @Override
    public CharSequence getMessage() {
      final String instructions;

      switch (mType) {
        case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
        case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
          instructions =
              getString(
                  R.string.bluetooth_display_passkey_pin_msg,
                  getHtmlEscapedDeviceName(mDevice),
                  mPairingKey);

          // Since its only a notification, send an OK to the framework,
          // indicating that the dialog has been displayed.
          if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
            mDevice.setPairingConfirmation(true);
          } else if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
            mDevice.setPin(mPairingKey);
          }
          break;

        case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
          instructions =
              getString(
                  R.string.bluetooth_confirm_passkey_msg,
                  getHtmlEscapedDeviceName(mDevice),
                  mPairingKey);
          break;

        case BluetoothDevice.PAIRING_VARIANT_CONSENT:
        case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
          instructions =
              getString(R.string.bluetooth_incoming_pairing_msg, getHtmlEscapedDeviceName(mDevice));
          break;
        default:
          instructions = "";
      }

      return Html.fromHtml(instructions);
    }
  }
}
