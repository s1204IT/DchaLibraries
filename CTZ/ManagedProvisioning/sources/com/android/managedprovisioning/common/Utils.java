package com.android.managedprovisioning.common;

import android.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.TrampolineActivity;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {
    public Set<String> getCurrentSystemApps(IPackageManager iPackageManager, int i) {
        List<ApplicationInfo> list;
        HashSet hashSet = new HashSet();
        try {
            list = iPackageManager.getInstalledApplications(8192, i).getList();
        } catch (RemoteException e) {
            ProvisionLogger.loge("This should not happen.", e);
            list = null;
        }
        for (ApplicationInfo applicationInfo : list) {
            if ((applicationInfo.flags & 1) != 0) {
                hashSet.add(applicationInfo.packageName);
            }
        }
        return hashSet;
    }

    public void disableComponent(ComponentName componentName, int i) {
        setComponentEnabledSetting(IPackageManager.Stub.asInterface(ServiceManager.getService("package")), componentName, 2, i);
    }

    public void enableComponent(ComponentName componentName, int i) {
        setComponentEnabledSetting(IPackageManager.Stub.asInterface(ServiceManager.getService("package")), componentName, 1, i);
    }

    @VisibleForTesting
    void setComponentEnabledSetting(IPackageManager iPackageManager, ComponentName componentName, int i, int i2) {
        try {
            iPackageManager.setComponentEnabledSetting(componentName, i, 1, i2);
        } catch (RemoteException e) {
            ProvisionLogger.loge("This should not happen.", e);
        } catch (Exception e2) {
            ProvisionLogger.logw("Component not found, not changing enabled setting: " + componentName.toShortString());
        }
    }

    public ComponentName findDeviceAdmin(String str, ComponentName componentName, Context context, int i) throws IllegalProvisioningArgumentException {
        if (componentName != null) {
            str = componentName.getPackageName();
        }
        if (str == null) {
            throw new IllegalProvisioningArgumentException("Neither the package name nor the component name of the admin are supplied");
        }
        try {
            ComponentName componentNameFindDeviceAdminInPackageInfo = findDeviceAdminInPackageInfo(str, componentName, context.getPackageManager().getPackageInfoAsUser(str, 514, i));
            if (componentNameFindDeviceAdminInPackageInfo == null) {
                throw new IllegalProvisioningArgumentException("Cannot find any admin receiver in package " + str + " with component " + componentName);
            }
            return componentNameFindDeviceAdminInPackageInfo;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalProvisioningArgumentException("Dpc " + str + " is not installed. ", e);
        }
    }

    public ComponentName findDeviceAdminInPackageInfo(String str, ComponentName componentName, PackageInfo packageInfo) {
        if (componentName != null) {
            if (!isComponentInPackageInfo(componentName, packageInfo)) {
                ProvisionLogger.logw("The component " + componentName + " isn't registered in the apk");
                return null;
            }
            return componentName;
        }
        return findDeviceAdminInPackage(str, packageInfo);
    }

    private ComponentName findDeviceAdminInPackage(String str, PackageInfo packageInfo) {
        if (packageInfo == null || !TextUtils.equals(packageInfo.packageName, str)) {
            return null;
        }
        ComponentName componentName = null;
        for (ActivityInfo activityInfo : packageInfo.receivers) {
            if (TextUtils.equals(activityInfo.permission, "android.permission.BIND_DEVICE_ADMIN")) {
                if (componentName != null) {
                    ProvisionLogger.logw("more than 1 device admin component are found");
                    return null;
                }
                componentName = new ComponentName(str, activityInfo.name);
            }
        }
        return componentName;
    }

    private boolean isComponentInPackageInfo(ComponentName componentName, PackageInfo packageInfo) {
        for (ActivityInfo activityInfo : packageInfo.receivers) {
            if (componentName.getClassName().equals(activityInfo.name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPackageTestOnly(PackageManager packageManager, String str, int i) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            ApplicationInfo applicationInfoAsUser = packageManager.getApplicationInfoAsUser(str, 786432, i);
            if (applicationInfoAsUser != null) {
                return (applicationInfoAsUser.flags & 256) != 0;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public boolean packageRequiresUpdate(String str, int i, Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(str, 0);
            if (i != Integer.MAX_VALUE) {
                return packageInfo.versionCode < i;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    public UserHandle getManagedProfile(Context context) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        for (UserInfo userInfo : userManager.getProfiles(userManager.getUserHandle())) {
            if (userInfo.isManagedProfile()) {
                return new UserHandle(userInfo.id);
            }
        }
        return null;
    }

    public int alreadyHasManagedProfile(Context context) {
        UserHandle managedProfile = getManagedProfile(context);
        if (managedProfile != null) {
            return managedProfile.getIdentifier();
        }
        return -1;
    }

    public void removeAccount(Context context, Account account) {
        try {
            AccountManagerFuture<Bundle> accountManagerFutureRemoveAccount = ((AccountManager) context.getSystemService("account")).removeAccount(account, null, null, null);
            if (accountManagerFutureRemoveAccount.getResult().getBoolean("booleanResult", false)) {
                ProvisionLogger.logw("Account removed from the primary user.");
            } else {
                Intent intent = (Intent) accountManagerFutureRemoveAccount.getResult().getParcelable("intent");
                if (intent != null) {
                    ProvisionLogger.logi("Starting activity to remove account");
                    TrampolineActivity.startActivity(context, intent);
                } else {
                    ProvisionLogger.logw("Could not remove account from the primary user.");
                }
            }
        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
            ProvisionLogger.logw("Exception removing account from the primary user.", e);
        }
    }

    public String mapIntentToDpmAction(Intent intent) throws IllegalProvisioningArgumentException {
        if (intent == null || intent.getAction() == null) {
            throw new IllegalProvisioningArgumentException("Null intent action.");
        }
        switch (intent.getAction()) {
            case "android.app.action.PROVISION_MANAGED_DEVICE":
            case "android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE":
            case "android.app.action.PROVISION_MANAGED_USER":
            case "android.app.action.PROVISION_MANAGED_PROFILE":
                return intent.getAction();
            case "android.app.action.PROVISION_MANAGED_DEVICE_SILENTLY":
                return "android.app.action.PROVISION_MANAGED_DEVICE";
            case "android.nfc.action.NDEF_DISCOVERED":
                String type = intent.getType();
                if (type == null) {
                    throw new IllegalProvisioningArgumentException("Unknown NFC bump mime-type: " + type);
                }
                if (((type.hashCode() == 2066322017 && type.equals("application/com.android.managedprovisioning")) ? (byte) 0 : (byte) -1) == 0) {
                    return "android.app.action.PROVISION_MANAGED_DEVICE";
                }
                throw new IllegalProvisioningArgumentException("Unknown NFC bump mime-type: " + type);
            case "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE":
                return "android.app.action.PROVISION_MANAGED_DEVICE";
            default:
                throw new IllegalProvisioningArgumentException("Unknown intent action " + intent.getAction());
        }
    }

    public void sendFactoryResetBroadcast(Context context, String str) {
        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
        intent.setPackage("android");
        intent.addFlags(268435456);
        intent.putExtra("android.intent.extra.REASON", str);
        context.sendBroadcast(intent);
    }

    public final boolean isProfileOwnerAction(String str) {
        return "android.app.action.PROVISION_MANAGED_PROFILE".equals(str) || "android.app.action.PROVISION_MANAGED_USER".equals(str);
    }

    public final boolean isDeviceOwnerAction(String str) {
        return "android.app.action.PROVISION_MANAGED_DEVICE".equals(str) || "android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE".equals(str);
    }

    public boolean isConnectedToNetwork(Context context) {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public boolean isConnectedToWifi(Context context) {
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == 1;
    }

    public NetworkInfo getActiveNetworkInfo(Context context) {
        return ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
    }

    public boolean isEncryptionRequired() {
        return (isPhysicalDeviceEncrypted() || SystemProperties.getBoolean("persist.sys.no_req_encrypt", false)) ? false : true;
    }

    public boolean isPhysicalDeviceEncrypted() {
        return StorageManager.isEncrypted();
    }

    public Intent getWifiPickIntent() {
        Intent intent = new Intent("android.net.wifi.PICK_WIFI_NETWORK");
        intent.putExtra("extra_prefs_show_button_bar", true);
        intent.putExtra("wifi_enable_next_on_connect", true);
        return intent;
    }

    public boolean isSplitSystemUser() {
        return UserManager.isSplitSystemUser();
    }

    public boolean currentLauncherSupportsManagedProfiles(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, 65536);
        if (resolveInfoResolveActivity == null) {
            return false;
        }
        try {
            return versionNumberAtLeastL(packageManager.getApplicationInfo(resolveInfoResolveActivity.activityInfo.packageName, 0).targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int i) {
        return i >= 21;
    }

    public byte[] computeHashOfByteArray(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bArr);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            ProvisionLogger.loge("Hashing algorithm SHA-256 not supported.", e);
            return null;
        }
    }

    public byte[] computeHashOfFile(String str, String str2) throws Throwable {
        MessageDigest messageDigest;
        FileInputStream fileInputStream;
        try {
            try {
                messageDigest = MessageDigest.getInstance(str2);
            } catch (Throwable th) {
                th = th;
            }
            try {
                fileInputStream = new FileInputStream(str);
                try {
                    byte[] bArr = new byte[256];
                    int i = 0;
                    while (i != -1) {
                        i = fileInputStream.read(bArr);
                        if (i > 0) {
                            messageDigest.update(bArr, 0, i);
                        }
                    }
                    byte[] bArrDigest = messageDigest.digest();
                    try {
                        fileInputStream.close();
                        return bArrDigest;
                    } catch (IOException e) {
                        return bArrDigest;
                    }
                } catch (IOException e2) {
                    e = e2;
                    ProvisionLogger.loge("IO error.", e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return null;
                }
            } catch (IOException e4) {
                e = e4;
                fileInputStream = null;
            } catch (Throwable th2) {
                th = th2;
                str2 = 0;
                if (str2 != 0) {
                    try {
                        str2.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        } catch (NoSuchAlgorithmException e6) {
            ProvisionLogger.loge("Hashing algorithm " + ((String) str2) + " not supported.", e6);
            return null;
        }
    }

    public boolean isBrightColor(int i) {
        return ((Color.red(i) * 299) + (Color.green(i) * 587)) + (Color.blue(i) * 114) >= 190000;
    }

    public boolean canResolveIntentAsUser(Context context, Intent intent, int i) {
        return (intent == null || context.getPackageManager().resolveActivityAsUser(intent, 0, i) == null) ? false : true;
    }

    public boolean isPackageDeviceOwner(DevicePolicyManager devicePolicyManager, String str) {
        ComponentName deviceOwnerComponentOnCallingUser = devicePolicyManager.getDeviceOwnerComponentOnCallingUser();
        return deviceOwnerComponentOnCallingUser != null && deviceOwnerComponentOnCallingUser.getPackageName().equals(str);
    }

    public int getAccentColor(Context context) {
        return getAttrColor(context, R.attr.colorAccent);
    }

    private int getAttrColor(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return color;
    }
}
