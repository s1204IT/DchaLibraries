package com.android.managedprovisioning;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Base64;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Properties;

public class MessageParser {
    protected static final String[] DEVICE_OWNER_STRING_EXTRAS = {"android.app.extra.PROVISIONING_TIME_ZONE", "android.app.extra.PROVISIONING_LOCALE", "android.app.extra.PROVISIONING_WIFI_SSID", "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", "android.app.extra.PROVISIONING_WIFI_PASSWORD", "android.app.extra.PROVISIONING_WIFI_PROXY_HOST", "android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS", "android.app.extra.PROVISIONING_WIFI_PAC_URL", "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME", "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION", "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER", "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM"};
    protected static final String[] DEVICE_OWNER_LONG_EXTRAS = {"android.app.extra.PROVISIONING_LOCAL_TIME"};
    protected static final String[] DEVICE_OWNER_INT_EXTRAS = {"android.app.extra.PROVISIONING_WIFI_PROXY_PORT"};
    protected static final String[] DEVICE_OWNER_BOOLEAN_EXTRAS = {"android.app.extra.PROVISIONING_WIFI_HIDDEN", "com.android.managedprovisioning.extra.started_by_nfc", "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"};
    protected static final String[] DEVICE_OWNER_PERSISTABLE_BUNDLE_EXTRAS = {"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"};

    public void addProvisioningParamsToBundle(Bundle bundle, ProvisioningParams params) {
        bundle.putString("android.app.extra.PROVISIONING_TIME_ZONE", params.mTimeZone);
        bundle.putString("android.app.extra.PROVISIONING_LOCALE", params.getLocaleAsString());
        bundle.putString("android.app.extra.PROVISIONING_WIFI_SSID", params.mWifiSsid);
        bundle.putString("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", params.mWifiSecurityType);
        bundle.putString("android.app.extra.PROVISIONING_WIFI_PASSWORD", params.mWifiPassword);
        bundle.putString("android.app.extra.PROVISIONING_WIFI_PROXY_HOST", params.mWifiProxyHost);
        bundle.putString("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS", params.mWifiProxyBypassHosts);
        bundle.putString("android.app.extra.PROVISIONING_WIFI_PAC_URL", params.mWifiPacUrl);
        bundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME", params.mDeviceAdminPackageName);
        bundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION", params.mDeviceAdminPackageDownloadLocation);
        bundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER", params.mDeviceAdminPackageDownloadCookieHeader);
        bundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM", params.getDeviceAdminPackageChecksumAsString());
        bundle.putLong("android.app.extra.PROVISIONING_LOCAL_TIME", params.mLocalTime);
        bundle.putInt("android.app.extra.PROVISIONING_WIFI_PROXY_PORT", params.mWifiProxyPort);
        bundle.putBoolean("android.app.extra.PROVISIONING_WIFI_HIDDEN", params.mWifiHidden);
        bundle.putBoolean("com.android.managedprovisioning.extra.started_by_nfc", params.mStartedByNfc);
        bundle.putBoolean("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", params.mLeaveAllSystemAppsEnabled);
        bundle.putParcelable("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", params.mAdminExtrasBundle);
    }

    public ProvisioningParams parseIntent(Intent intent) throws ParseException {
        ProvisionLogger.logi("Processing intent.");
        return intent.hasExtra("android.nfc.extra.NDEF_MESSAGES") ? parseNfcIntent(intent) : parseNonNfcIntent(intent);
    }

    public ProvisioningParams parseNfcIntent(Intent nfcIntent) throws ParseException {
        ProvisionLogger.logi("Processing Nfc Payload.");
        Parcelable[] arr$ = nfcIntent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES");
        for (Parcelable rawMsg : arr$) {
            NdefMessage msg = (NdefMessage) rawMsg;
            NdefRecord firstRecord = msg.getRecords()[0];
            String mimeType = new String(firstRecord.getType(), StandardCharsets.UTF_8);
            if ("application/com.android.managedprovisioning".equals(mimeType)) {
                ProvisioningParams params = parseProperties(new String(firstRecord.getPayload(), StandardCharsets.UTF_8));
                params.mStartedByNfc = true;
                return params;
            }
        }
        throw new ParseException("Intent does not contain NfcRecord with the correct MIME type.", R.string.device_owner_error_general);
    }

    private ProvisioningParams parseProperties(String data) throws ParseException {
        ProvisioningParams params = new ProvisioningParams();
        try {
            Properties props = new Properties();
            props.load(new StringReader(data));
            params.mTimeZone = props.getProperty("android.app.extra.PROVISIONING_TIME_ZONE");
            String s = props.getProperty("android.app.extra.PROVISIONING_LOCALE");
            if (s != null) {
                params.mLocale = stringToLocale(s);
            }
            params.mWifiSsid = props.getProperty("android.app.extra.PROVISIONING_WIFI_SSID");
            params.mWifiSecurityType = props.getProperty("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE");
            params.mWifiPassword = props.getProperty("android.app.extra.PROVISIONING_WIFI_PASSWORD");
            params.mWifiProxyHost = props.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_HOST");
            params.mWifiProxyBypassHosts = props.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS");
            params.mWifiPacUrl = props.getProperty("android.app.extra.PROVISIONING_WIFI_PAC_URL");
            params.mDeviceAdminPackageName = props.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME");
            params.mDeviceAdminPackageDownloadLocation = props.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION");
            params.mDeviceAdminPackageDownloadCookieHeader = props.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER");
            String s2 = props.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM");
            if (s2 != null) {
                params.mDeviceAdminPackageChecksum = stringToByteArray(s2);
            }
            String s3 = props.getProperty("android.app.extra.PROVISIONING_LOCAL_TIME");
            if (s3 != null) {
                params.mLocalTime = Long.parseLong(s3);
            }
            String s4 = props.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_PORT");
            if (s4 != null) {
                params.mWifiProxyPort = Integer.parseInt(s4);
            }
            String s5 = props.getProperty("android.app.extra.PROVISIONING_WIFI_HIDDEN");
            if (s5 != null) {
                params.mWifiHidden = Boolean.parseBoolean(s5);
            }
            String s6 = props.getProperty("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED");
            if (s6 != null) {
                params.mLeaveAllSystemAppsEnabled = Boolean.parseBoolean(s6);
            }
            checkValidityOfProvisioningParams(params);
            return params;
        } catch (IOException e) {
            throw new ParseException("Couldn't load payload", R.string.device_owner_error_general, e);
        } catch (NumberFormatException e2) {
            throw new ParseException("Incorrect numberformat.", R.string.device_owner_error_general, e2);
        } catch (IllformedLocaleException e3) {
            throw new ParseException("Invalid locale.", R.string.device_owner_error_general, e3);
        }
    }

    public ProvisioningParams parseNonNfcIntent(Intent intent) throws ParseException {
        ProvisionLogger.logi("Processing intent.");
        ProvisioningParams params = new ProvisioningParams();
        params.mTimeZone = intent.getStringExtra("android.app.extra.PROVISIONING_TIME_ZONE");
        String localeString = intent.getStringExtra("android.app.extra.PROVISIONING_LOCALE");
        if (localeString != null) {
            params.mLocale = stringToLocale(localeString);
        }
        params.mWifiSsid = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_SSID");
        params.mWifiSecurityType = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE");
        params.mWifiPassword = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PASSWORD");
        params.mWifiProxyHost = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PROXY_HOST");
        params.mWifiProxyBypassHosts = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS");
        params.mWifiPacUrl = intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PAC_URL");
        params.mDeviceAdminPackageName = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME");
        params.mDeviceAdminPackageDownloadLocation = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION");
        params.mDeviceAdminPackageDownloadCookieHeader = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER");
        String hashString = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM");
        if (hashString != null) {
            params.mDeviceAdminPackageChecksum = stringToByteArray(hashString);
        }
        params.mLocalTime = intent.getLongExtra("android.app.extra.PROVISIONING_LOCAL_TIME", -1L);
        params.mWifiProxyPort = intent.getIntExtra("android.app.extra.PROVISIONING_WIFI_PROXY_PORT", 0);
        params.mWifiHidden = intent.getBooleanExtra("android.app.extra.PROVISIONING_WIFI_HIDDEN", false);
        params.mStartedByNfc = intent.getBooleanExtra("com.android.managedprovisioning.extra.started_by_nfc", false);
        params.mLeaveAllSystemAppsEnabled = intent.getBooleanExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", false);
        try {
            params.mAdminExtrasBundle = (PersistableBundle) intent.getParcelableExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE");
            checkValidityOfProvisioningParams(params);
            return params;
        } catch (ClassCastException e) {
            throw new ParseException("Extra android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE must be of type PersistableBundle.", R.string.device_owner_error_general, e);
        }
    }

    private void checkValidityOfProvisioningParams(ProvisioningParams params) throws ParseException {
        if (TextUtils.isEmpty(params.mDeviceAdminPackageName)) {
            throw new ParseException("Must provide the name of the device admin package.", R.string.device_owner_error_general);
        }
        if (!TextUtils.isEmpty(params.mDeviceAdminPackageDownloadLocation)) {
            if (params.mDeviceAdminPackageChecksum == null || params.mDeviceAdminPackageChecksum.length == 0) {
                throw new ParseException("Checksum of installer file is required for downloading device admin file, but not provided.", R.string.device_owner_error_general);
            }
        }
    }

    public static class ParseException extends Exception {
        private int mErrorMessageId;

        public ParseException(String message, int errorMessageId) {
            super(message);
            this.mErrorMessageId = errorMessageId;
        }

        public ParseException(String message, int errorMessageId, Throwable t) {
            super(message, t);
            this.mErrorMessageId = errorMessageId;
        }

        public int getErrorMessageId() {
            return this.mErrorMessageId;
        }
    }

    public static byte[] stringToByteArray(String s) throws NumberFormatException {
        try {
            return Base64.decode(s, 8);
        } catch (IllegalArgumentException e) {
            throw new NumberFormatException("Incorrect checksum format.");
        }
    }

    public static Locale stringToLocale(String s) throws IllformedLocaleException {
        return new Locale.Builder().setLanguageTag(s.replace("_", "-")).build();
    }
}
