package com.android.settings.enterprise;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import com.android.settings.R;
import com.android.settings.vpn2.VpnUtils;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {
    private static final int MY_USER_ID = UserHandle.myUserId();
    private final ConnectivityManager mCm;
    private final Context mContext;
    private final DevicePolicyManager mDpm;
    private final PackageManagerWrapper mPm;
    private final Resources mResources;
    private final UserManager mUm;

    public EnterprisePrivacyFeatureProviderImpl(Context context, DevicePolicyManager devicePolicyManager, PackageManagerWrapper packageManagerWrapper, UserManager userManager, ConnectivityManager connectivityManager, Resources resources) {
        this.mContext = context.getApplicationContext();
        this.mDpm = devicePolicyManager;
        this.mPm = packageManagerWrapper;
        this.mUm = userManager;
        this.mCm = connectivityManager;
        this.mResources = resources;
    }

    @Override
    public boolean hasDeviceOwner() {
        return this.mPm.hasSystemFeature("android.software.device_admin") && this.mDpm.getDeviceOwnerComponentOnAnyUser() != null;
    }

    private int getManagedProfileUserId() {
        for (UserInfo userInfo : this.mUm.getProfiles(MY_USER_ID)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -10000;
    }

    @Override
    public boolean isInCompMode() {
        return hasDeviceOwner() && getManagedProfileUserId() != -10000;
    }

    @Override
    public String getDeviceOwnerOrganizationName() {
        CharSequence deviceOwnerOrganizationName = this.mDpm.getDeviceOwnerOrganizationName();
        if (deviceOwnerOrganizationName == null) {
            return null;
        }
        return deviceOwnerOrganizationName.toString();
    }

    @Override
    public CharSequence getDeviceOwnerDisclosure() {
        if (!hasDeviceOwner()) {
            return null;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        CharSequence deviceOwnerOrganizationName = this.mDpm.getDeviceOwnerOrganizationName();
        if (deviceOwnerOrganizationName != null) {
            spannableStringBuilder.append((CharSequence) this.mResources.getString(R.string.do_disclosure_with_name, deviceOwnerOrganizationName));
        } else {
            spannableStringBuilder.append((CharSequence) this.mResources.getString(R.string.do_disclosure_generic));
        }
        spannableStringBuilder.append((CharSequence) this.mResources.getString(R.string.do_disclosure_learn_more_separator));
        spannableStringBuilder.append(this.mResources.getString(R.string.learn_more), new EnterprisePrivacySpan(this.mContext), 0);
        return spannableStringBuilder;
    }

    @Override
    public Date getLastSecurityLogRetrievalTime() {
        long lastSecurityLogRetrievalTime = this.mDpm.getLastSecurityLogRetrievalTime();
        if (lastSecurityLogRetrievalTime < 0) {
            return null;
        }
        return new Date(lastSecurityLogRetrievalTime);
    }

    @Override
    public Date getLastBugReportRequestTime() {
        long lastBugReportRequestTime = this.mDpm.getLastBugReportRequestTime();
        if (lastBugReportRequestTime < 0) {
            return null;
        }
        return new Date(lastBugReportRequestTime);
    }

    @Override
    public Date getLastNetworkLogRetrievalTime() {
        long lastNetworkLogRetrievalTime = this.mDpm.getLastNetworkLogRetrievalTime();
        if (lastNetworkLogRetrievalTime < 0) {
            return null;
        }
        return new Date(lastNetworkLogRetrievalTime);
    }

    @Override
    public boolean isSecurityLoggingEnabled() {
        return this.mDpm.isSecurityLoggingEnabled(null);
    }

    @Override
    public boolean isNetworkLoggingEnabled() {
        return this.mDpm.isNetworkLoggingEnabled(null);
    }

    @Override
    public boolean isAlwaysOnVpnSetInCurrentUser() {
        return VpnUtils.isAlwaysOnVpnSet(this.mCm, MY_USER_ID);
    }

    @Override
    public boolean isAlwaysOnVpnSetInManagedProfile() {
        int managedProfileUserId = getManagedProfileUserId();
        return managedProfileUserId != -10000 && VpnUtils.isAlwaysOnVpnSet(this.mCm, managedProfileUserId);
    }

    @Override
    public boolean isGlobalHttpProxySet() {
        return this.mCm.getGlobalProxy() != null;
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInCurrentUser() {
        ComponentName deviceOwnerComponentOnCallingUser = this.mDpm.getDeviceOwnerComponentOnCallingUser();
        if (deviceOwnerComponentOnCallingUser == null) {
            deviceOwnerComponentOnCallingUser = this.mDpm.getProfileOwnerAsUser(MY_USER_ID);
        }
        if (deviceOwnerComponentOnCallingUser == null) {
            return 0;
        }
        return this.mDpm.getMaximumFailedPasswordsForWipe(deviceOwnerComponentOnCallingUser, MY_USER_ID);
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInManagedProfile() {
        ComponentName profileOwnerAsUser;
        int managedProfileUserId = getManagedProfileUserId();
        if (managedProfileUserId == -10000 || (profileOwnerAsUser = this.mDpm.getProfileOwnerAsUser(managedProfileUserId)) == null) {
            return 0;
        }
        return this.mDpm.getMaximumFailedPasswordsForWipe(profileOwnerAsUser, managedProfileUserId);
    }

    @Override
    public String getImeLabelIfOwnerSet() {
        String stringForUser;
        if (!this.mDpm.isCurrentInputMethodSetByOwner() || (stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "default_input_method", MY_USER_ID)) == null) {
            return null;
        }
        try {
            return this.mPm.getApplicationInfoAsUser(stringForUser, 0, MY_USER_ID).loadLabel(this.mPm.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForCurrentUser() {
        List ownerInstalledCaCerts = this.mDpm.getOwnerInstalledCaCerts(new UserHandle(MY_USER_ID));
        if (ownerInstalledCaCerts == null) {
            return 0;
        }
        return ownerInstalledCaCerts.size();
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForManagedProfile() {
        List ownerInstalledCaCerts;
        int managedProfileUserId = getManagedProfileUserId();
        if (managedProfileUserId == -10000 || (ownerInstalledCaCerts = this.mDpm.getOwnerInstalledCaCerts(new UserHandle(managedProfileUserId))) == null) {
            return 0;
        }
        return ownerInstalledCaCerts.size();
    }

    @Override
    public int getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile() {
        Iterator it = this.mUm.getProfiles(MY_USER_ID).iterator();
        int size = 0;
        while (it.hasNext()) {
            List activeAdminsAsUser = this.mDpm.getActiveAdminsAsUser(((UserInfo) it.next()).id);
            if (activeAdminsAsUser != null) {
                size += activeAdminsAsUser.size();
            }
        }
        return size;
    }

    @Override
    public boolean areBackupsMandatory() {
        return this.mDpm.getMandatoryBackupTransport() != null;
    }

    protected static class EnterprisePrivacySpan extends ClickableSpan {
        private final Context mContext;

        public EnterprisePrivacySpan(Context context) {
            this.mContext = context;
        }

        @Override
        public void onClick(View view) {
            this.mContext.startActivity(new Intent("android.settings.ENTERPRISE_PRIVACY_SETTINGS").addFlags(268435456));
        }

        public boolean equals(Object obj) {
            return (obj instanceof EnterprisePrivacySpan) && ((EnterprisePrivacySpan) obj).mContext == this.mContext;
        }
    }
}
