package com.android.tv.settings.connectivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settingslib.wifi.AccessPoint;

public class WifiUtils {
    /**
     * Gets security value from ScanResult.
     *
     * Duplicated method from {@link AccessPoint#getSecurity(ScanResult)}.
     * TODO(b/120827021): Should be removed if the there is have a common one in shared place (e.g.
     * SettingsLib).
     *
     * @param result ScanResult
     * @return Related security value based on {@link AccessPoint}.
     */
    public static int getAccessPointSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return AccessPoint.SECURITY_WEP;
        } else if (result.capabilities.contains("SAE")) {
            return AccessPoint.SECURITY_SAE;
        } else if (result.capabilities.contains("PSK")) {
            return AccessPoint.SECURITY_PSK;
        } else if (result.capabilities.contains("EAP_SUITE_B_192")) {
            return AccessPoint.SECURITY_EAP_SUITE_B;
        } else if (result.capabilities.contains("EAP")) {
            return AccessPoint.SECURITY_EAP;
        } else if (result.capabilities.contains("OWE")) {
            return AccessPoint.SECURITY_OWE;
        }

        return AccessPoint.SECURITY_NONE;
    }

    /**
     * Provides a simple way to generate a new {@link WifiConfiguration} obj from
     * {@link ScanResult} or {@link AccessPoint}. Either {@code accessPoint} or {@code scanResult
     * } input should be not null for retrieving information, otherwise will throw
     * IllegalArgumentException.
     * This method prefers to take {@link AccessPoint} input in priority. Therefore this method
     * will take {@link AccessPoint} input as preferred data extraction source when you input
     * both {@link AccessPoint} and {@link ScanResult}, and ignore {@link ScanResult} input.
     *
     * Duplicated and simplified method from {@link WifiConfigController#getConfig()}.
     * TODO(b/120827021): Should be removed if the there is have a common one in shared place (e.g.
     * SettingsLib).
     *
     * @param accessPoint Input data for retrieving WifiConfiguration.
     * @param scanResult  Input data for retrieving WifiConfiguration.
     * @return WifiConfiguration obj based on input.
     */
    public static WifiConfiguration getWifiConfig(AccessPoint accessPoint, ScanResult scanResult,
            String password) {
        if (accessPoint == null && scanResult == null) {
            throw new IllegalArgumentException(
                    "At least one of AccessPoint and ScanResult input is required.");
        }

        final WifiConfiguration config = new WifiConfiguration();
        final int security;

        if (accessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(scanResult.SSID);
            security = getAccessPointSecurity(scanResult);
        } else {
            if (!accessPoint.isSaved()) {
                config.SSID = AccessPoint.convertToQuotedString(
                        accessPoint.getSsidStr());
            } else {
                config.networkId = accessPoint.getConfig().networkId;
                config.hiddenSSID = accessPoint.getConfig().hiddenSSID;
            }
            security = accessPoint.getSecurity();
        }

        switch (security) {
            case AccessPoint.SECURITY_NONE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                break;

            case AccessPoint.SECURITY_WEP:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);
                if (!TextUtils.isEmpty(password)) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (!TextUtils.isEmpty(password)) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
            case AccessPoint.SECURITY_EAP_SUITE_B:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                if (security == AccessPoint.SECURITY_EAP_SUITE_B) {
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SUITE_B_192);
                    config.requirePMF = true;
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.GCMP_256);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.GCMP_256);
                    config.allowedGroupManagementCiphers.set(WifiConfiguration.GroupMgmtCipher
                            .BIP_GMAC_256);
                    // allowedSuiteBCiphers will be set according to certificate type
                }

                if (!TextUtils.isEmpty(password)) {
                    config.enterpriseConfig.setPassword(password);
                }
                break;
            case AccessPoint.SECURITY_SAE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE);
                config.requirePMF = true;
                if (!TextUtils.isEmpty(password)) {
                    config.preSharedKey = '"' + password + '"';
                }
                break;

            case AccessPoint.SECURITY_OWE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.OWE);
                config.requirePMF = true;
                break;

            default:
                break;
        }

        return config;
    }
}
