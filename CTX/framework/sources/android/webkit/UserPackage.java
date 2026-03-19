package android.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;
import java.util.ArrayList;
import java.util.List;

public class UserPackage {
    public static final int MINIMUM_SUPPORTED_SDK = 28;
    private final PackageInfo mPackageInfo;
    private final UserInfo mUserInfo;

    public UserPackage(UserInfo userInfo, PackageInfo packageInfo) {
        this.mUserInfo = userInfo;
        this.mPackageInfo = packageInfo;
    }

    public static List<UserPackage> getPackageInfosAllUsers(Context context, String str, int i) throws PackageManager.NameNotFoundException {
        List<UserInfo> allUsers = getAllUsers(context);
        ArrayList arrayList = new ArrayList(allUsers.size());
        for (UserInfo userInfo : allUsers) {
            PackageInfo packageInfoAsUser = null;
            try {
                packageInfoAsUser = context.getPackageManager().getPackageInfoAsUser(str, i, userInfo.id);
            } catch (PackageManager.NameNotFoundException e) {
            }
            arrayList.add(new UserPackage(userInfo, packageInfoAsUser));
        }
        return arrayList;
    }

    public boolean isEnabledPackage() {
        if (this.mPackageInfo == null) {
            return false;
        }
        return this.mPackageInfo.applicationInfo.enabled;
    }

    public boolean isInstalledPackage() {
        return (this.mPackageInfo == null || (this.mPackageInfo.applicationInfo.flags & 8388608) == 0 || (this.mPackageInfo.applicationInfo.privateFlags & 1) != 0) ? false : true;
    }

    public static boolean hasCorrectTargetSdkVersion(PackageInfo packageInfo) {
        return packageInfo.applicationInfo.targetSdkVersion >= 28;
    }

    public UserInfo getUserInfo() {
        return this.mUserInfo;
    }

    public PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    private static List<UserInfo> getAllUsers(Context context) {
        return ((UserManager) context.getSystemService("user")).getUsers(false);
    }
}
