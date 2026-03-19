package com.android.managedprovisioning.parser;

import android.accounts.Account;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.UserHandle;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.LogoUtils;
import com.android.managedprovisioning.common.ManagedProvisioningSharedPreferences;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.DisclaimersParam;
import com.android.managedprovisioning.model.PackageDownloadInfo;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.model.WifiInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.Set;

public class ExtrasProvisioningDataParser implements ProvisioningDataParser {
    private static final Set<String> PROVISIONING_ACTIONS_SUPPORT_ALL_PROVISIONING_DATA = new HashSet(Collections.singletonList("android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE"));
    private static final Set<String> PROVISIONING_ACTIONS_SUPPORT_MIN_PROVISIONING_DATA = new HashSet(Arrays.asList("android.app.action.PROVISION_MANAGED_DEVICE", "android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE", "android.app.action.PROVISION_MANAGED_USER", "android.app.action.PROVISION_MANAGED_PROFILE", "android.app.action.PROVISION_MANAGED_DEVICE_SILENTLY"));
    private final Context mContext;
    private final ManagedProvisioningSharedPreferences mSharedPreferences;
    private final Utils mUtils;

    ExtrasProvisioningDataParser(Context context, Utils utils) {
        this(context, utils, new ManagedProvisioningSharedPreferences(context));
    }

    ExtrasProvisioningDataParser(Context context, Utils utils, ManagedProvisioningSharedPreferences managedProvisioningSharedPreferences) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mSharedPreferences = (ManagedProvisioningSharedPreferences) Preconditions.checkNotNull(managedProvisioningSharedPreferences);
    }

    @Override
    public ProvisioningParams parse(Intent intent) throws IllegalProvisioningArgumentException {
        String action = intent.getAction();
        if ("com.android.managedprovisioning.action.RESUME_PROVISIONING".equals(action)) {
            return (ProvisioningParams) intent.getParcelableExtra("provisioningParams");
        }
        if (PROVISIONING_ACTIONS_SUPPORT_MIN_PROVISIONING_DATA.contains(action)) {
            ProvisionLogger.logi("Processing mininalist extras intent.");
            return parseMinimalistSupportedProvisioningDataInternal(intent, this.mContext).build();
        }
        if (PROVISIONING_ACTIONS_SUPPORT_ALL_PROVISIONING_DATA.contains(action)) {
            return parseAllSupportedProvisioningData(intent, this.mContext);
        }
        throw new IllegalProvisioningArgumentException("Unsupported provisioning action: " + action);
    }

    private ProvisioningParams.Builder parseMinimalistSupportedProvisioningDataInternal(Intent intent, Context context) throws IllegalProvisioningArgumentException {
        boolean booleanExtra;
        String str;
        String stringExtra;
        String stringExtra2;
        String stringExtra3;
        boolean booleanExtra2;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        boolean zEquals = "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE".equals(intent.getAction());
        try {
            long jIncrementAndGetProvisioningId = this.mSharedPreferences.incrementAndGetProvisioningId();
            String strMapIntentToDpmAction = this.mUtils.mapIntentToDpmAction(intent);
            boolean zEquals2 = "android.app.action.PROVISION_MANAGED_PROFILE".equals(strMapIntentToDpmAction);
            ComponentName componentNameFindDeviceAdmin = (ComponentName) intent.getParcelableExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME");
            if (zEquals2) {
                componentNameFindDeviceAdmin = this.mUtils.findDeviceAdmin(intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME"), componentNameFindDeviceAdmin, context, UserHandle.myUserId());
            }
            boolean z = true;
            if (!zEquals && (strMapIntentToDpmAction.equals("android.app.action.PROVISION_MANAGED_USER") || strMapIntentToDpmAction.equals("android.app.action.PROVISION_MANAGED_DEVICE"))) {
                booleanExtra = intent.getBooleanExtra("android.app.extra.PROVISIONING_SKIP_USER_SETUP", true);
            } else {
                booleanExtra = true;
            }
            boolean z2 = zEquals2 && intent.getBooleanExtra("android.app.extra.PROVISIONING_SKIP_USER_CONSENT", false) && this.mUtils.isPackageDeviceOwner(devicePolicyManager, ProvisioningParams.inferStaticDeviceAdminPackageName(componentNameFindDeviceAdmin, null));
            if (!zEquals2 || !intent.getBooleanExtra("android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION", false)) {
                z = false;
            }
            Integer numValueOf = ProvisioningParams.DEFAULT_MAIN_COLOR;
            if (!zEquals) {
                if (intent.hasExtra("android.app.extra.PROVISIONING_MAIN_COLOR")) {
                    numValueOf = Integer.valueOf(intent.getIntExtra("android.app.extra.PROVISIONING_MAIN_COLOR", 0));
                }
                parseOrganizationLogoUrlFromExtras(context, intent);
            }
            DisclaimersParam disclaimersParam = new DisclaimersParser(context, jIncrementAndGetProvisioningId).parse(intent.getParcelableArrayExtra("android.app.extra.PROVISIONING_DISCLAIMERS"));
            if (zEquals) {
                stringExtra = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL");
                stringExtra3 = intent.getStringExtra("android.app.extra.PROVISIONING_ORGANIZATION_NAME");
                stringExtra2 = intent.getStringExtra("android.app.extra.PROVISIONING_SUPPORT_URL");
                str = new DeviceAdminIconParser(context, jIncrementAndGetProvisioningId).parse((Uri) intent.getParcelableExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI"));
            } else {
                str = null;
                stringExtra = null;
                stringExtra2 = null;
                stringExtra3 = null;
            }
            if (!zEquals2) {
                booleanExtra2 = intent.getBooleanExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", false);
            } else {
                booleanExtra2 = false;
            }
            return ProvisioningParams.Builder.builder().setProvisioningId(jIncrementAndGetProvisioningId).setProvisioningAction(strMapIntentToDpmAction).setDeviceAdminComponentName(componentNameFindDeviceAdmin).setDeviceAdminPackageName(null).setSkipEncryption(intent.getBooleanExtra("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", false)).setLeaveAllSystemAppsEnabled(booleanExtra2).setAdminExtrasBundle((PersistableBundle) intent.getParcelableExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE")).setMainColor(numValueOf).setDisclaimersParam(disclaimersParam).setSkipUserConsent(z2).setKeepAccountMigrated(z).setSkipUserSetup(booleanExtra).setAccountToMigrate((Account) intent.getParcelableExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE")).setDeviceAdminLabel(stringExtra).setOrganizationName(stringExtra3).setSupportUrl(stringExtra2).setDeviceAdminIconFilePath(str);
        } catch (ClassCastException e) {
            throw new IllegalProvisioningArgumentException("Extra has invalid type", e);
        } catch (IllegalArgumentException e2) {
            throw new IllegalProvisioningArgumentException("Invalid parameter found!", e2);
        } catch (NullPointerException e3) {
            throw new IllegalProvisioningArgumentException("Compulsory parameter not found!", e3);
        }
    }

    private ProvisioningParams parseAllSupportedProvisioningData(Intent intent, Context context) throws IllegalProvisioningArgumentException {
        try {
            ProvisionLogger.logi("Processing all supported extras intent: " + intent.getAction());
            return parseMinimalistSupportedProvisioningDataInternal(intent, context).setTimeZone(intent.getStringExtra("android.app.extra.PROVISIONING_TIME_ZONE")).setLocalTime(intent.getLongExtra("android.app.extra.PROVISIONING_LOCAL_TIME", -1L)).setLocale(StoreUtils.stringToLocale(intent.getStringExtra("android.app.extra.PROVISIONING_LOCALE"))).setUseMobileData(intent.getBooleanExtra("android.app.extra.PROVISIONING_USE_MOBILE_DATA", false)).setWifiInfo(parseWifiInfoFromExtras(intent)).setDeviceAdminDownloadInfo(parsePackageDownloadInfoFromExtras(intent)).setStartedByTrustedSource("android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE".equals(intent.getAction())).build();
        } catch (IllegalArgumentException e) {
            throw new IllegalProvisioningArgumentException("Invalid parameter found!", e);
        } catch (NullPointerException e2) {
            throw new IllegalProvisioningArgumentException("Compulsory parameter not found!", e2);
        } catch (IllformedLocaleException e3) {
            throw new IllegalProvisioningArgumentException("Invalid locale format!", e3);
        }
    }

    private WifiInfo parseWifiInfoFromExtras(Intent intent) {
        if (intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_SSID") == null) {
            return null;
        }
        return WifiInfo.Builder.builder().setSsid(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_SSID")).setSecurityType(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE")).setPassword(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PASSWORD")).setProxyHost(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PROXY_HOST")).setProxyBypassHosts(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS")).setPacUrl(intent.getStringExtra("android.app.extra.PROVISIONING_WIFI_PAC_URL")).setProxyPort(intent.getIntExtra("android.app.extra.PROVISIONING_WIFI_PROXY_PORT", 0)).setHidden(intent.getBooleanExtra("android.app.extra.PROVISIONING_WIFI_HIDDEN", false)).build();
    }

    private PackageDownloadInfo parsePackageDownloadInfoFromExtras(Intent intent) {
        if (intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION") == null) {
            return null;
        }
        PackageDownloadInfo.Builder cookieHeader = PackageDownloadInfo.Builder.builder().setMinVersion(intent.getIntExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE", Integer.MAX_VALUE)).setLocation(intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION")).setCookieHeader(intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER"));
        String stringExtra = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM");
        if (stringExtra != null) {
            cookieHeader.setPackageChecksum(StoreUtils.stringToByteArray(stringExtra));
        }
        String stringExtra2 = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM");
        if (stringExtra2 != null) {
            cookieHeader.setSignatureChecksum(StoreUtils.stringToByteArray(stringExtra2));
        }
        return cookieHeader.build();
    }

    private void parseOrganizationLogoUrlFromExtras(Context context, Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra("android.app.extra.PROVISIONING_LOGO_URI");
        if (uri != null) {
            LogoUtils.saveOrganisationLogo(context, uri);
        }
    }
}
