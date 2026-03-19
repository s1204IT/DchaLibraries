package com.android.managedprovisioning.parser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Parcelable;
import android.os.PersistableBundle;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.ManagedProvisioningSharedPreferences;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.PackageDownloadInfo;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.model.WifiInfo;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.IllformedLocaleException;
import java.util.Properties;

public class PropertiesProvisioningDataParser implements ProvisioningDataParser {
    private final Context mContext;
    private final ManagedProvisioningSharedPreferences mSharedPreferences;
    private final Utils mUtils;

    PropertiesProvisioningDataParser(Context context, Utils utils) {
        this(context, utils, new ManagedProvisioningSharedPreferences(context));
    }

    PropertiesProvisioningDataParser(Context context, Utils utils, ManagedProvisioningSharedPreferences managedProvisioningSharedPreferences) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mSharedPreferences = (ManagedProvisioningSharedPreferences) Preconditions.checkNotNull(managedProvisioningSharedPreferences);
    }

    @Override
    public ProvisioningParams parse(Intent intent) throws IllegalProvisioningArgumentException {
        if (!"android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            throw new IllegalProvisioningArgumentException("Only NFC action is supported in this parser.");
        }
        ProvisionLogger.logi("Processing Nfc Payload.");
        NdefRecord firstNdefRecord = getFirstNdefRecord(intent);
        if (firstNdefRecord != null) {
            try {
                Properties properties = new Properties();
                properties.load(new StringReader(new String(firstNdefRecord.getPayload(), StandardCharsets.UTF_8)));
                ProvisioningParams.Builder deviceAdminPackageName = ProvisioningParams.Builder.builder().setProvisioningId(this.mSharedPreferences.incrementAndGetProvisioningId()).setStartedByTrustedSource(true).setIsNfc(true).setProvisioningAction(this.mUtils.mapIntentToDpmAction(intent)).setDeviceAdminPackageName(properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME"));
                String property = properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME");
                if (property != null) {
                    deviceAdminPackageName.setDeviceAdminComponentName(ComponentName.unflattenFromString(property));
                }
                deviceAdminPackageName.setTimeZone(properties.getProperty("android.app.extra.PROVISIONING_TIME_ZONE")).setLocale(StoreUtils.stringToLocale(properties.getProperty("android.app.extra.PROVISIONING_LOCALE")));
                String property2 = properties.getProperty("android.app.extra.PROVISIONING_LOCAL_TIME");
                if (property2 != null) {
                    deviceAdminPackageName.setLocalTime(Long.parseLong(property2));
                }
                deviceAdminPackageName.setWifiInfo(parseWifiInfoFromProperties(properties)).setDeviceAdminDownloadInfo(parsePackageDownloadInfoFromProperties(properties)).setAdminExtrasBundle(deserializeExtrasBundle(properties, "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"));
                String property3 = properties.getProperty("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED");
                if (property3 != null) {
                    deviceAdminPackageName.setLeaveAllSystemAppsEnabled(Boolean.parseBoolean(property3));
                }
                String property4 = properties.getProperty("android.app.extra.PROVISIONING_SKIP_ENCRYPTION");
                if (property4 != null) {
                    deviceAdminPackageName.setSkipEncryption(Boolean.parseBoolean(property4));
                }
                String property5 = properties.getProperty("android.app.extra.PROVISIONING_USE_MOBILE_DATA");
                if (property5 != null) {
                    deviceAdminPackageName.setUseMobileData(Boolean.parseBoolean(property5));
                }
                ProvisionLogger.logi("End processing Nfc Payload.");
                return deviceAdminPackageName.build();
            } catch (IOException e) {
                throw new IllegalProvisioningArgumentException("Couldn't load payload", e);
            } catch (NumberFormatException e2) {
                throw new IllegalProvisioningArgumentException("Incorrect numberformat.", e2);
            } catch (IllegalArgumentException e3) {
                throw new IllegalProvisioningArgumentException("Invalid parameter found!", e3);
            } catch (NullPointerException e4) {
                throw new IllegalProvisioningArgumentException("Compulsory parameter not found!", e4);
            } catch (IllformedLocaleException e5) {
                throw new IllegalProvisioningArgumentException("Invalid locale.", e5);
            }
        }
        throw new IllegalProvisioningArgumentException("Intent does not contain NfcRecord with the correct MIME type.");
    }

    private WifiInfo parseWifiInfoFromProperties(Properties properties) {
        if (properties.getProperty("android.app.extra.PROVISIONING_WIFI_SSID") == null) {
            return null;
        }
        WifiInfo.Builder pacUrl = WifiInfo.Builder.builder().setSsid(properties.getProperty("android.app.extra.PROVISIONING_WIFI_SSID")).setSecurityType(properties.getProperty("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE")).setPassword(properties.getProperty("android.app.extra.PROVISIONING_WIFI_PASSWORD")).setProxyHost(properties.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_HOST")).setProxyBypassHosts(properties.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS")).setPacUrl(properties.getProperty("android.app.extra.PROVISIONING_WIFI_PAC_URL"));
        String property = properties.getProperty("android.app.extra.PROVISIONING_WIFI_PROXY_PORT");
        if (property != null) {
            pacUrl.setProxyPort(Integer.parseInt(property));
        }
        String property2 = properties.getProperty("android.app.extra.PROVISIONING_WIFI_HIDDEN");
        if (property2 != null) {
            pacUrl.setHidden(Boolean.parseBoolean(property2));
        }
        return pacUrl.build();
    }

    private PackageDownloadInfo parsePackageDownloadInfoFromProperties(Properties properties) {
        if (properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION") == null) {
            return null;
        }
        PackageDownloadInfo.Builder cookieHeader = PackageDownloadInfo.Builder.builder().setLocation(properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION")).setCookieHeader(properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER"));
        String property = properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE");
        if (property != null) {
            cookieHeader.setMinVersion(Integer.parseInt(property));
        }
        String property2 = properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM");
        if (property2 != null) {
            cookieHeader.setPackageChecksum(StoreUtils.stringToByteArray(property2)).setPackageChecksumSupportsSha1(true);
        }
        String property3 = properties.getProperty("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM");
        if (property3 != null) {
            cookieHeader.setSignatureChecksum(StoreUtils.stringToByteArray(property3));
        }
        return cookieHeader.build();
    }

    private PersistableBundle deserializeExtrasBundle(Properties properties, String str) throws IOException {
        String property = properties.getProperty(str);
        if (property != null) {
            Properties properties2 = new Properties();
            properties2.load(new StringReader(property));
            PersistableBundle persistableBundle = new PersistableBundle(properties2.size());
            for (String str2 : properties2.stringPropertyNames()) {
                persistableBundle.putString(str2, properties2.getProperty(str2));
            }
            return persistableBundle;
        }
        return null;
    }

    public static NdefRecord getFirstNdefRecord(Intent intent) {
        Parcelable[] parcelableArrayExtra = intent.getParcelableArrayExtra("android.nfc.extra.NDEF_MESSAGES");
        if (parcelableArrayExtra != null) {
            for (Parcelable parcelable : parcelableArrayExtra) {
                NdefRecord[] records = ((NdefMessage) parcelable).getRecords();
                if (records.length > 0) {
                    NdefRecord ndefRecord = records[0];
                    if ("application/com.android.managedprovisioning".equals(new String(ndefRecord.getType(), StandardCharsets.UTF_8))) {
                        return ndefRecord;
                    }
                }
            }
            return null;
        }
        return null;
    }
}
