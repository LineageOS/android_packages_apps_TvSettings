package com.android.tv.settings.util;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.android.tv.settings.R;
import vendor.nvidia.hardware.convertibleport.V1_0.IConvertiblePort;
import vendor.nvidia.hardware.convertibleport.V1_0.ConvPortModeStatus;

public final class ConvertiblePort {
    private static final String TAG = "ConvertiblePortUtil";
    private static IConvertiblePort mConvertiblePortHal = null;

    private ConvertiblePort() {
    }

    public static final String getConvertiblePortSetting(Context context) {
        getHalService();
        if (mConvertiblePortHal == null) {
            return "";
        }
        if (!context.getResources().getBoolean(R.bool.has_convertible_port)) {
            Log.d(TAG, "getConvertiblePortSetting: Non-valid platform");
            return "";
        }
        try {
            return mConvertiblePortHal.readConvPortMode();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get port mode", e);
            return "";
        }
    }

    public static final int setConvertiblePortSetting(Context context, String mode) {
        getHalService();
        if (mConvertiblePortHal == null) {
            return ConvPortModeStatus.ERROR;
        }
        if (!context.getResources().getBoolean(R.bool.has_convertible_port)) {
            Log.d(TAG, "setConvertiblePortSetting: Non-valid platform");
            return ConvPortModeStatus.ERROR;
        }
        try {
            return mConvertiblePortHal.writeConvPortMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set port mode", e);
            return ConvPortModeStatus.ERROR;
        }
    }

    private static void getHalService() {
        try {
            if (mConvertiblePortHal == null) {
                mConvertiblePortHal = IConvertiblePort.getService();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get convertible port hal. ", ex);
        }
    }
}
