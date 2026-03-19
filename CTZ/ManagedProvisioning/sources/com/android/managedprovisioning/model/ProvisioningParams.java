package com.android.managedprovisioning.model;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.AtomicFile;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.PersistableBundlable;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class ProvisioningParams extends PersistableBundlable {
    public final Account accountToMigrate;
    public final PersistableBundle adminExtrasBundle;
    public final ComponentName deviceAdminComponentName;
    public final PackageDownloadInfo deviceAdminDownloadInfo;
    public final String deviceAdminIconFilePath;
    public final String deviceAdminLabel;

    @Deprecated
    public final String deviceAdminPackageName;
    public final DisclaimersParam disclaimersParam;
    public final boolean isNfc;
    public final boolean keepAccountMigrated;
    public final boolean leaveAllSystemAppsEnabled;
    public final long localTime;
    public final Locale locale;
    public final Integer mainColor;
    public final String organizationName;
    public final String provisioningAction;
    public final long provisioningId;
    public final boolean skipEncryption;
    public final boolean skipUserConsent;
    public final boolean skipUserSetup;
    public final boolean startedByTrustedSource;
    public final String supportUrl;
    public final String timeZone;
    public final boolean useMobileData;
    public final WifiInfo wifiInfo;
    public static final Integer DEFAULT_MAIN_COLOR = null;
    public static final Parcelable.Creator<ProvisioningParams> CREATOR = new Parcelable.Creator<ProvisioningParams>() {
        @Override
        public ProvisioningParams createFromParcel(Parcel parcel) {
            return new ProvisioningParams(parcel);
        }

        @Override
        public ProvisioningParams[] newArray(int i) {
            return new ProvisioningParams[i];
        }
    };

    public static String inferStaticDeviceAdminPackageName(ComponentName componentName, String str) {
        if (componentName != null) {
            return componentName.getPackageName();
        }
        return str;
    }

    public String inferDeviceAdminPackageName() {
        return inferStaticDeviceAdminPackageName(this.deviceAdminComponentName, this.deviceAdminPackageName);
    }

    public ComponentName inferDeviceAdminComponentName(Utils utils, Context context, int i) throws IllegalProvisioningArgumentException {
        if (this.deviceAdminComponentName != null) {
            return this.deviceAdminComponentName;
        }
        return utils.findDeviceAdmin(this.deviceAdminPackageName, this.deviceAdminComponentName, context, i);
    }

    private ProvisioningParams(Builder builder) {
        this.provisioningId = builder.mProvisioningId;
        this.timeZone = builder.mTimeZone;
        this.localTime = builder.mLocalTime;
        this.locale = builder.mLocale;
        this.wifiInfo = builder.mWifiInfo;
        this.useMobileData = builder.mUseMobileData;
        this.deviceAdminComponentName = builder.mDeviceAdminComponentName;
        this.deviceAdminPackageName = builder.mDeviceAdminPackageName;
        this.deviceAdminLabel = builder.mDeviceAdminLabel;
        this.organizationName = builder.mOrganizationName;
        this.supportUrl = builder.mSupportUrl;
        this.deviceAdminIconFilePath = builder.mDeviceAdminIconFilePath;
        this.deviceAdminDownloadInfo = builder.mDeviceAdminDownloadInfo;
        this.disclaimersParam = builder.mDisclaimersParam;
        this.adminExtrasBundle = builder.mAdminExtrasBundle;
        this.startedByTrustedSource = builder.mStartedByTrustedSource;
        this.isNfc = builder.mIsNfc;
        this.leaveAllSystemAppsEnabled = builder.mLeaveAllSystemAppsEnabled;
        this.skipEncryption = builder.mSkipEncryption;
        this.accountToMigrate = builder.mAccountToMigrate;
        this.provisioningAction = (String) Preconditions.checkNotNull(builder.mProvisioningAction);
        this.mainColor = builder.mMainColor;
        this.skipUserConsent = builder.mSkipUserConsent;
        this.skipUserSetup = builder.mSkipUserSetup;
        this.keepAccountMigrated = builder.mKeepAccountMigrated;
        validateFields();
    }

    private ProvisioningParams(Parcel parcel) {
        this(createBuilderFromPersistableBundle(PersistableBundlable.getPersistableBundleFromParcel(parcel)));
    }

    private void validateFields() {
        Preconditions.checkArgument((this.deviceAdminPackageName == null && this.deviceAdminComponentName == null) ? false : true);
    }

    @Override
    public PersistableBundle toPersistableBundle() {
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putLong("provisioning-id", this.provisioningId);
        persistableBundle.putString("android.app.extra.PROVISIONING_TIME_ZONE", this.timeZone);
        persistableBundle.putLong("android.app.extra.PROVISIONING_LOCAL_TIME", this.localTime);
        persistableBundle.putString("android.app.extra.PROVISIONING_LOCALE", StoreUtils.localeToString(this.locale));
        StoreUtils.putPersistableBundlableIfNotNull(persistableBundle, "wifi-info", this.wifiInfo);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_USE_MOBILE_DATA", this.useMobileData);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME", this.deviceAdminPackageName);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", StoreUtils.componentNameToString(this.deviceAdminComponentName));
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL", this.deviceAdminLabel);
        persistableBundle.putString("android.app.extra.PROVISIONING_ORGANIZATION_NAME", this.organizationName);
        persistableBundle.putString("android.app.extra.PROVISIONING_SUPPORT_URL", this.supportUrl);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI", this.deviceAdminIconFilePath);
        persistableBundle.putPersistableBundle("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE", this.accountToMigrate == null ? null : StoreUtils.accountToPersistableBundle(this.accountToMigrate));
        persistableBundle.putString("provisioning-action", this.provisioningAction);
        StoreUtils.putIntegerIfNotNull(persistableBundle, "android.app.extra.PROVISIONING_MAIN_COLOR", this.mainColor);
        StoreUtils.putPersistableBundlableIfNotNull(persistableBundle, "download-info", this.deviceAdminDownloadInfo);
        StoreUtils.putPersistableBundlableIfNotNull(persistableBundle, "android.app.extra.PROVISIONING_DISCLAIMERS", this.disclaimersParam);
        persistableBundle.putPersistableBundle("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", this.adminExtrasBundle);
        persistableBundle.putBoolean("started-by-trusted-source", this.startedByTrustedSource);
        persistableBundle.putBoolean("started-is-nfc", this.isNfc);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", this.leaveAllSystemAppsEnabled);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", this.skipEncryption);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_SKIP_USER_SETUP", this.skipUserSetup);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_SKIP_USER_CONSENT", this.skipUserConsent);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION", this.keepAccountMigrated);
        return persistableBundle;
    }

    private static Builder createBuilderFromPersistableBundle(PersistableBundle persistableBundle) {
        Builder builder = new Builder();
        builder.setProvisioningId(persistableBundle.getLong("provisioning-id", 0L));
        builder.setTimeZone(persistableBundle.getString("android.app.extra.PROVISIONING_TIME_ZONE"));
        builder.setLocalTime(persistableBundle.getLong("android.app.extra.PROVISIONING_LOCAL_TIME"));
        builder.setLocale((Locale) StoreUtils.getStringAttrFromPersistableBundle(persistableBundle, "android.app.extra.PROVISIONING_LOCALE", new Function() {
            @Override
            public final Object apply(Object obj) {
                return StoreUtils.stringToLocale((String) obj);
            }
        }));
        builder.setUseMobileData(persistableBundle.getBoolean("android.app.extra.PROVISIONING_USE_MOBILE_DATA"));
        builder.setWifiInfo((WifiInfo) StoreUtils.getObjectAttrFromPersistableBundle(persistableBundle, "wifi-info", new Function() {
            @Override
            public final Object apply(Object obj) {
                return WifiInfo.fromPersistableBundle((PersistableBundle) obj);
            }
        }));
        builder.setDeviceAdminPackageName(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME"));
        builder.setDeviceAdminComponentName((ComponentName) StoreUtils.getStringAttrFromPersistableBundle(persistableBundle, "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", new Function() {
            @Override
            public final Object apply(Object obj) {
                return StoreUtils.stringToComponentName((String) obj);
            }
        }));
        builder.setDeviceAdminLabel(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL"));
        builder.setOrganizationName(persistableBundle.getString("android.app.extra.PROVISIONING_ORGANIZATION_NAME"));
        builder.setSupportUrl(persistableBundle.getString("android.app.extra.PROVISIONING_SUPPORT_URL"));
        builder.setDeviceAdminIconFilePath(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI"));
        builder.setAccountToMigrate((Account) StoreUtils.getObjectAttrFromPersistableBundle(persistableBundle, "android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE", new Function() {
            @Override
            public final Object apply(Object obj) {
                return StoreUtils.persistableBundleToAccount((PersistableBundle) obj);
            }
        }));
        builder.setProvisioningAction(persistableBundle.getString("provisioning-action"));
        builder.setMainColor(StoreUtils.getIntegerAttrFromPersistableBundle(persistableBundle, "android.app.extra.PROVISIONING_MAIN_COLOR"));
        builder.setDeviceAdminDownloadInfo((PackageDownloadInfo) StoreUtils.getObjectAttrFromPersistableBundle(persistableBundle, "download-info", new Function() {
            @Override
            public final Object apply(Object obj) {
                return PackageDownloadInfo.fromPersistableBundle((PersistableBundle) obj);
            }
        }));
        builder.setDisclaimersParam((DisclaimersParam) StoreUtils.getObjectAttrFromPersistableBundle(persistableBundle, "android.app.extra.PROVISIONING_DISCLAIMERS", new Function() {
            @Override
            public final Object apply(Object obj) {
                return DisclaimersParam.fromPersistableBundle((PersistableBundle) obj);
            }
        }));
        builder.setAdminExtrasBundle(persistableBundle.getPersistableBundle("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"));
        builder.setStartedByTrustedSource(persistableBundle.getBoolean("started-by-trusted-source"));
        builder.setIsNfc(persistableBundle.getBoolean("started-is-nfc"));
        builder.setSkipEncryption(persistableBundle.getBoolean("android.app.extra.PROVISIONING_SKIP_ENCRYPTION"));
        builder.setLeaveAllSystemAppsEnabled(persistableBundle.getBoolean("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"));
        builder.setSkipUserSetup(persistableBundle.getBoolean("android.app.extra.PROVISIONING_SKIP_USER_SETUP"));
        builder.setSkipUserConsent(persistableBundle.getBoolean("android.app.extra.PROVISIONING_SKIP_USER_CONSENT"));
        builder.setKeepAccountMigrated(persistableBundle.getBoolean("android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION"));
        return builder;
    }

    public String toString() {
        return "ProvisioningParams values: " + toPersistableBundle().toString();
    }

    public void save(File file) {
        FileOutputStream fileOutputStreamStartWrite;
        Throwable e;
        AtomicFile atomicFile;
        ProvisionLogger.logd("Saving ProvisioningParams to " + file);
        try {
            atomicFile = new AtomicFile(file);
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
                try {
                    XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "provisioning-params");
                    toPersistableBundle().saveToXml(fastXmlSerializer);
                    fastXmlSerializer.endTag(null, "provisioning-params");
                    fastXmlSerializer.endDocument();
                    atomicFile.finishWrite(fileOutputStreamStartWrite);
                } catch (IOException | XmlPullParserException e2) {
                    e = e2;
                    ProvisionLogger.loge("Caught exception while trying to save Provisioning Params to  file " + file, e);
                    file.delete();
                    if (atomicFile != null) {
                        atomicFile.failWrite(fileOutputStreamStartWrite);
                    }
                }
            } catch (IOException | XmlPullParserException e3) {
                fileOutputStreamStartWrite = null;
                e = e3;
            }
        } catch (IOException | XmlPullParserException e4) {
            fileOutputStreamStartWrite = null;
            e = e4;
            atomicFile = null;
        }
    }

    public void cleanUp() {
        if (this.disclaimersParam != null) {
            this.disclaimersParam.cleanUp();
        }
        if (this.deviceAdminIconFilePath != null) {
            new File(this.deviceAdminIconFilePath).delete();
        }
    }

    public static ProvisioningParams load(File file) {
        Throwable th;
        Throwable th2;
        if (!file.exists()) {
            return null;
        }
        ProvisionLogger.logd("Loading ProvisioningParams from " + file);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, null);
                ProvisioningParams provisioningParamsLoad = load(xmlPullParserNewPullParser);
                fileInputStream.close();
                return provisioningParamsLoad;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    if (th != null) {
                        fileInputStream.close();
                        throw th2;
                    }
                    try {
                        fileInputStream.close();
                        throw th2;
                    } catch (Throwable th5) {
                        th.addSuppressed(th5);
                        throw th2;
                    }
                }
            }
        } catch (IOException | XmlPullParserException e) {
            ProvisionLogger.loge("Caught exception while trying to load the provisioning params from file " + file, e);
            return null;
        }
    }

    private static ProvisioningParams load(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                byte b = -1;
                if (name.hashCode() == -1336812442 && name.equals("provisioning-params")) {
                    b = 0;
                }
                if (b == 0) {
                    return createBuilderFromPersistableBundle(PersistableBundle.restoreFromXml(xmlPullParser)).build();
                }
            }
        }
    }

    public static final class Builder {
        private Account mAccountToMigrate;
        private PersistableBundle mAdminExtrasBundle;
        private ComponentName mDeviceAdminComponentName;
        private PackageDownloadInfo mDeviceAdminDownloadInfo;
        private String mDeviceAdminIconFilePath;
        private String mDeviceAdminLabel;
        private String mDeviceAdminPackageName;
        private DisclaimersParam mDisclaimersParam;
        private Locale mLocale;
        private String mOrganizationName;
        private String mProvisioningAction;
        private long mProvisioningId;
        private String mSupportUrl;
        private String mTimeZone;
        private WifiInfo mWifiInfo;
        private long mLocalTime = -1;
        private Integer mMainColor = ProvisioningParams.DEFAULT_MAIN_COLOR;
        private boolean mStartedByTrustedSource = false;
        private boolean mIsNfc = false;
        private boolean mLeaveAllSystemAppsEnabled = false;
        private boolean mSkipEncryption = false;
        private boolean mSkipUserConsent = false;
        private boolean mSkipUserSetup = true;
        private boolean mKeepAccountMigrated = false;
        private boolean mUseMobileData = false;

        public Builder setProvisioningId(long j) {
            this.mProvisioningId = j;
            return this;
        }

        public Builder setTimeZone(String str) {
            this.mTimeZone = str;
            return this;
        }

        public Builder setLocalTime(long j) {
            this.mLocalTime = j;
            return this;
        }

        public Builder setLocale(Locale locale) {
            this.mLocale = locale;
            return this;
        }

        public Builder setWifiInfo(WifiInfo wifiInfo) {
            this.mWifiInfo = wifiInfo;
            return this;
        }

        @Deprecated
        public Builder setDeviceAdminPackageName(String str) {
            this.mDeviceAdminPackageName = str;
            return this;
        }

        public Builder setDeviceAdminComponentName(ComponentName componentName) {
            this.mDeviceAdminComponentName = componentName;
            return this;
        }

        public Builder setDeviceAdminLabel(String str) {
            this.mDeviceAdminLabel = str;
            return this;
        }

        public Builder setOrganizationName(String str) {
            this.mOrganizationName = str;
            return this;
        }

        public Builder setSupportUrl(String str) {
            this.mSupportUrl = str;
            return this;
        }

        public Builder setDeviceAdminIconFilePath(String str) {
            this.mDeviceAdminIconFilePath = str;
            return this;
        }

        public Builder setAccountToMigrate(Account account) {
            this.mAccountToMigrate = account;
            return this;
        }

        public Builder setProvisioningAction(String str) {
            this.mProvisioningAction = str;
            return this;
        }

        public Builder setMainColor(Integer num) {
            this.mMainColor = num;
            return this;
        }

        public Builder setDeviceAdminDownloadInfo(PackageDownloadInfo packageDownloadInfo) {
            this.mDeviceAdminDownloadInfo = packageDownloadInfo;
            return this;
        }

        public Builder setDisclaimersParam(DisclaimersParam disclaimersParam) {
            this.mDisclaimersParam = disclaimersParam;
            return this;
        }

        public Builder setAdminExtrasBundle(PersistableBundle persistableBundle) {
            this.mAdminExtrasBundle = persistableBundle;
            return this;
        }

        public Builder setStartedByTrustedSource(boolean z) {
            this.mStartedByTrustedSource = z;
            return this;
        }

        public Builder setIsNfc(boolean z) {
            this.mIsNfc = z;
            return this;
        }

        public Builder setLeaveAllSystemAppsEnabled(boolean z) {
            this.mLeaveAllSystemAppsEnabled = z;
            return this;
        }

        public Builder setSkipEncryption(boolean z) {
            this.mSkipEncryption = z;
            return this;
        }

        public Builder setSkipUserConsent(boolean z) {
            this.mSkipUserConsent = z;
            return this;
        }

        public Builder setSkipUserSetup(boolean z) {
            this.mSkipUserSetup = z;
            return this;
        }

        public Builder setKeepAccountMigrated(boolean z) {
            this.mKeepAccountMigrated = z;
            return this;
        }

        public Builder setUseMobileData(boolean z) {
            this.mUseMobileData = z;
            return this;
        }

        public ProvisioningParams build() {
            return new ProvisioningParams(this);
        }

        public static Builder builder() {
            return new Builder();
        }
    }
}
