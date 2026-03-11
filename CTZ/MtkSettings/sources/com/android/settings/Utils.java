package com.android.settings;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceFrameLayout;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import java.net.InetAddress;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils extends com.android.settingslib.Utils {
    public static final int[] BADNESS_COLORS = {0, -3917784, -1750760, -754944, -344276, -9986505, -16089278};
    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context, PreferenceGroup preferenceGroup, String str, int i) {
        Preference preferenceFindPreference = preferenceGroup.findPreference(str);
        if (preferenceFindPreference == null) {
            return false;
        }
        Intent intent = preferenceFindPreference.getIntent();
        if (intent != null) {
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 0);
            int size = listQueryIntentActivities.size();
            for (int i2 = 0; i2 < size; i2++) {
                ResolveInfo resolveInfo = listQueryIntentActivities.get(i2);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    preferenceFindPreference.setIntent(new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    if ((i & 1) != 0) {
                        preferenceFindPreference.setTitle(resolveInfo.loadLabel(packageManager));
                    }
                    return true;
                }
            }
        }
        preferenceGroup.removePreference(preferenceFindPreference);
        return false;
    }

    public static UserManager getUserManager(Context context) {
        UserManager userManager = UserManager.get(context);
        if (userManager == null) {
            throw new IllegalStateException("Unable to load UserManager");
        }
        return userManager;
    }

    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return telephonyManager != null && telephonyManager.isVoiceCapable();
    }

    public static String getWifiIpAddresses(Context context) {
        Network currentNetwork = ((WifiManager) context.getSystemService(WifiManager.class)).getCurrentNetwork();
        if (currentNetwork != null) {
            return formatIpAddresses(((ConnectivityManager) context.getSystemService("connectivity")).getLinkProperties(currentNetwork));
        }
        return null;
    }

    private static String formatIpAddresses(LinkProperties linkProperties) {
        if (linkProperties == null) {
            return null;
        }
        Iterator it = linkProperties.getAllAddresses().iterator();
        if (!it.hasNext()) {
            return null;
        }
        String str = "";
        while (it.hasNext()) {
            str = str + ((InetAddress) it.next()).getHostAddress();
            if (it.hasNext()) {
                str = str + "\n";
            }
        }
        return str;
    }

    public static Locale createLocaleFromString(String str) {
        if (str == null) {
            return Locale.getDefault();
        }
        String[] strArrSplit = str.split("_", 3);
        if (1 == strArrSplit.length) {
            return new Locale(strArrSplit[0]);
        }
        if (2 == strArrSplit.length) {
            return new Locale(strArrSplit[0], strArrSplit[1]);
        }
        return new Locale(strArrSplit[0], strArrSplit[1], strArrSplit[2]);
    }

    public static boolean isBatteryPresent(Intent intent) {
        return intent.getBooleanExtra("present", true);
    }

    public static String getBatteryPercentage(Intent intent) {
        return formatPercentage(getBatteryLevel(intent));
    }

    public static void prepareCustomPreferencesList(ViewGroup viewGroup, View view, View view2, boolean z) {
        if (view2.getScrollBarStyle() == 33554432) {
            Resources resources = view2.getResources();
            int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.settings_side_margin);
            int dimensionPixelSize2 = resources.getDimensionPixelSize(android.R.dimen.dialog_fixed_height_major);
            if (viewGroup instanceof PreferenceFrameLayout) {
                view.getLayoutParams().removeBorders = true;
                if (z) {
                    dimensionPixelSize = 0;
                }
                view2.setPaddingRelative(dimensionPixelSize, 0, dimensionPixelSize, dimensionPixelSize2);
                return;
            }
            view2.setPaddingRelative(dimensionPixelSize, 0, dimensionPixelSize, dimensionPixelSize2);
        }
    }

    public static void forceCustomPadding(View view, boolean z) {
        Resources resources = view.getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.settings_side_margin);
        view.setPaddingRelative((z ? view.getPaddingStart() : 0) + dimensionPixelSize, 0, dimensionPixelSize + (z ? view.getPaddingEnd() : 0), resources.getDimensionPixelSize(android.R.dimen.dialog_fixed_height_major));
    }

    public static String getMeProfileName(Context context, boolean z) {
        if (z) {
            return getProfileDisplayName(context);
        }
        return getShorterNameIfPossible(context);
    }

    private static String getShorterNameIfPossible(Context context) {
        String localProfileGivenName = getLocalProfileGivenName(context);
        return !TextUtils.isEmpty(localProfileGivenName) ? localProfileGivenName : getProfileDisplayName(context);
    }

    private static String getLocalProfileGivenName(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursorQuery = contentResolver.query(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI, new String[]{"_id"}, "account_type IS NULL AND account_name IS NULL", null, null);
        if (cursorQuery == null) {
            return null;
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                return null;
            }
            long j = cursorQuery.getLong(0);
            cursorQuery.close();
            cursorQuery = contentResolver.query(ContactsContract.Profile.CONTENT_URI.buildUpon().appendPath("data").build(), new String[]{"data2", "data3"}, "raw_contact_id=" + j, null, null);
            if (cursorQuery == null) {
                return null;
            }
            try {
                if (!cursorQuery.moveToFirst()) {
                    return null;
                }
                String string = cursorQuery.getString(0);
                if (TextUtils.isEmpty(string)) {
                    string = cursorQuery.getString(1);
                }
                return string;
            } finally {
            }
        } finally {
        }
    }

    private static final String getProfileDisplayName(Context context) {
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
        if (cursorQuery == null) {
            return null;
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                return null;
            }
            return cursorQuery.getString(0);
        } finally {
            cursorQuery.close();
        }
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService("user")).getUsers().size() > 1;
    }

    public static UserHandle getManagedProfile(UserManager userManager) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        int size = userProfiles.size();
        for (int i = 0; i < size; i++) {
            UserHandle userHandle = userProfiles.get(i);
            if (userHandle.getIdentifier() != userManager.getUserHandle() && userManager.getUserInfo(userHandle.getIdentifier()).isManagedProfile()) {
                return userHandle;
            }
        }
        return null;
    }

    public static UserHandle getManagedProfileWithDisabled(UserManager userManager) {
        int iMyUserId = UserHandle.myUserId();
        List profiles = userManager.getProfiles(iMyUserId);
        int size = profiles.size();
        for (int i = 0; i < size; i++) {
            UserInfo userInfo = (UserInfo) profiles.get(i);
            if (userInfo.isManagedProfile() && userInfo.getUserHandle().getIdentifier() != iMyUserId) {
                return userInfo.getUserHandle();
            }
        }
        return null;
    }

    public static int getManagedProfileId(UserManager userManager, int i) {
        for (int i2 : userManager.getProfileIdsWithDisabled(i)) {
            if (i2 != i) {
                return i2;
            }
        }
        return -10000;
    }

    public static UserHandle getSecureTargetUser(IBinder iBinder, UserManager userManager, Bundle bundle, Bundle bundle2) {
        UserHandle userHandle = new UserHandle(UserHandle.myUserId());
        IActivityManager service = ActivityManager.getService();
        try {
            boolean zEquals = "com.android.settings".equals(service.getLaunchedFromPackage(iBinder));
            UserHandle userHandle2 = new UserHandle(UserHandle.getUserId(service.getLaunchedFromUid(iBinder)));
            if (!userHandle2.equals(userHandle) && isProfileOf(userManager, userHandle2)) {
                return userHandle2;
            }
            UserHandle userHandleFromBundle = getUserHandleFromBundle(bundle2);
            if (userHandleFromBundle != null && !userHandleFromBundle.equals(userHandle) && zEquals && isProfileOf(userManager, userHandleFromBundle)) {
                return userHandleFromBundle;
            }
            UserHandle userHandleFromBundle2 = getUserHandleFromBundle(bundle);
            if (userHandleFromBundle2 != null && !userHandleFromBundle2.equals(userHandle) && zEquals) {
                if (isProfileOf(userManager, userHandleFromBundle2)) {
                    return userHandleFromBundle2;
                }
            }
        } catch (RemoteException e) {
            Log.v("Settings", "Could not talk to activity manager.", e);
        }
        return userHandle;
    }

    private static UserHandle getUserHandleFromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        UserHandle userHandle = (UserHandle) bundle.getParcelable("android.intent.extra.USER");
        if (userHandle != null) {
            return userHandle;
        }
        int i = bundle.getInt("android.intent.extra.USER_ID", -1);
        if (i == -1) {
            return null;
        }
        return UserHandle.of(i);
    }

    private static boolean isProfileOf(UserManager userManager, UserHandle userHandle) {
        if (userManager == null || userHandle == null) {
            return false;
        }
        return UserHandle.myUserId() == userHandle.getIdentifier() || userManager.getUserProfiles().contains(userHandle);
    }

    public static boolean showSimCardTile(Context context) {
        return ((TelephonyManager) context.getSystemService("phone")).getSimCount() > 1;
    }

    public static UserInfo getExistingUser(UserManager userManager, UserHandle userHandle) {
        List<UserInfo> users = userManager.getUsers(true);
        int identifier = userHandle.getIdentifier();
        for (UserInfo userInfo : users) {
            if (userInfo.id == identifier) {
                return userInfo;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        TypedArray typedArrayObtainStyledAttributes = layoutInflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(3, 0);
        typedArrayObtainStyledAttributes.recycle();
        return layoutInflater.inflate(resourceId, viewGroup, false);
    }

    public static ArraySet<String> getHandledDomains(PackageManager packageManager, String str) {
        List intentFilterVerifications = packageManager.getIntentFilterVerifications(str);
        List<IntentFilter> allIntentFilters = packageManager.getAllIntentFilters(str);
        ArraySet<String> arraySet = new ArraySet<>();
        if (intentFilterVerifications != null && intentFilterVerifications.size() > 0) {
            Iterator it = intentFilterVerifications.iterator();
            while (it.hasNext()) {
                Iterator it2 = ((IntentFilterVerificationInfo) it.next()).getDomains().iterator();
                while (it2.hasNext()) {
                    arraySet.add((String) it2.next());
                }
            }
        }
        if (allIntentFilters != null && allIntentFilters.size() > 0) {
            for (IntentFilter intentFilter : allIntentFilters) {
                if (intentFilter.hasCategory("android.intent.category.BROWSABLE") && (intentFilter.hasDataScheme("http") || intentFilter.hasDataScheme("https"))) {
                    arraySet.addAll(intentFilter.getHostsList());
                }
            }
        }
        return arraySet;
    }

    public static ApplicationInfo getAdminApplicationInfo(Context context, int i) {
        ComponentName profileOwnerAsUser = ((DevicePolicyManager) context.getSystemService("device_policy")).getProfileOwnerAsUser(i);
        if (profileOwnerAsUser == null) {
            return null;
        }
        String packageName = profileOwnerAsUser.getPackageName();
        try {
            return AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, i);
        } catch (RemoteException e) {
            Log.e("Settings", "Error while retrieving application info for package " + packageName + ", userId " + i, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        try {
            return INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management")).isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static SpannableString createAccessibleSequence(CharSequence charSequence, String str) {
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new TtsSpan.TextBuilder(str).build(), 0, charSequence.length(), 18);
        return spannableString;
    }

    public static int getUserIdFromBundle(Context context, Bundle bundle) {
        return getUserIdFromBundle(context, bundle, false);
    }

    public static int getUserIdFromBundle(Context context, Bundle bundle, boolean z) {
        if (bundle == null) {
            return getCredentialOwnerUserId(context);
        }
        boolean z2 = false;
        if (z && bundle.getBoolean("allow_any_user", false)) {
            z2 = true;
        }
        int i = bundle.getInt("android.intent.extra.USER_ID", UserHandle.myUserId());
        return i == -9999 ? z2 ? i : enforceSystemUser(context, i) : z2 ? i : enforceSameOwner(context, i);
    }

    public static int enforceSystemUser(Context context, int i) {
        if (UserHandle.myUserId() == 0) {
            return i;
        }
        throw new SecurityException("Given user id " + i + " must only be used from USER_SYSTEM, but current user is " + UserHandle.myUserId());
    }

    public static int enforceSameOwner(Context context, int i) {
        if (ArrayUtils.contains(getUserManager(context).getProfileIdsWithDisabled(UserHandle.myUserId()), i)) {
            return i;
        }
        throw new SecurityException("Given user id " + i + " does not belong to user " + UserHandle.myUserId());
    }

    public static int getCredentialOwnerUserId(Context context) {
        return getCredentialOwnerUserId(context, UserHandle.myUserId());
    }

    public static int getCredentialOwnerUserId(Context context, int i) {
        return getUserManager(context).getCredentialOwnerProfile(i);
    }

    public static String formatDateRange(Context context, long j, long j2) {
        String string;
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            string = DateUtils.formatDateRange(context, sFormatter, j, j2, 65552, null).toString();
        }
        return string;
    }

    public static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public static boolean startQuietModeDialogIfNecessary(Context context, UserManager userManager, int i) {
        if (userManager.isQuietModeEnabled(UserHandle.of(i))) {
            context.startActivity(UnlaunchableAppActivity.createInQuietModeDialogIntent(i));
            return true;
        }
        return false;
    }

    public static boolean unlockWorkProfileIfNecessary(Context context, int i) {
        try {
            if (!ActivityManager.getService().isUserRunning(i, 2) || !new LockPatternUtils(context).isSecure(i)) {
                return false;
            }
            return confirmWorkProfileCredentials(context, i);
        } catch (RemoteException e) {
            return false;
        }
    }

    private static boolean confirmWorkProfileCredentials(Context context, int i) {
        Intent intentCreateConfirmDeviceCredentialIntent = ((KeyguardManager) context.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, i);
        if (intentCreateConfirmDeviceCredentialIntent != null) {
            context.startActivity(intentCreateConfirmDeviceCredentialIntent);
            return true;
        }
        return false;
    }

    public static CharSequence getApplicationLabel(Context context, String str) {
        try {
            return context.getPackageManager().getApplicationInfo(str, 4194816).loadLabel(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Settings", "Unable to find info for package: " + str);
            return null;
        }
    }

    public static boolean isPackageDirectBootAware(Context context, String str) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 0);
            if (!applicationInfo.isDirectBootAware()) {
                if (!applicationInfo.isPartiallyDirectBootAware()) {
                    return false;
                }
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Context createPackageContextAsUser(Context context, int i) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, UserHandle.of(i));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Settings", "Failed to create user context", e);
            return null;
        }
    }

    public static FingerprintManager getFingerprintManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            return (FingerprintManager) context.getSystemService("fingerprint");
        }
        return null;
    }

    public static boolean hasFingerprintHardware(Context context) {
        FingerprintManager fingerprintManagerOrNull = getFingerprintManagerOrNull(context);
        return fingerprintManagerOrNull != null && fingerprintManagerOrNull.isHardwareDetected();
    }

    public static void launchIntent(Fragment fragment, Intent intent) {
        try {
            int intExtra = intent.getIntExtra("android.intent.extra.USER_ID", -1);
            if (intExtra == -1) {
                fragment.startActivity(intent);
            } else {
                fragment.getActivity().startActivityAsUser(intent, new UserHandle(intExtra));
            }
        } catch (ActivityNotFoundException e) {
            Log.w("Settings", "No activity found for " + intent);
        }
    }

    public static boolean isDemoUser(Context context) {
        return UserManager.isDeviceInDemoMode(context) && getUserManager(context).isDemoUser();
    }

    public static ComponentName getDeviceOwnerComponent(Context context) {
        return ((DevicePolicyManager) context.getSystemService("device_policy")).getDeviceOwnerComponentOnAnyUser();
    }

    public static boolean isProfileOf(UserInfo userInfo, UserInfo userInfo2) {
        return userInfo.id == userInfo2.id || (userInfo.profileGroupId != -10000 && userInfo.profileGroupId == userInfo2.profileGroupId);
    }

    public static VolumeInfo maybeInitializeVolume(StorageManager storageManager, Bundle bundle) {
        VolumeInfo volumeInfoFindVolumeById = storageManager.findVolumeById(bundle.getString("android.os.storage.extra.VOLUME_ID", "private"));
        if (isVolumeValid(volumeInfoFindVolumeById)) {
            return volumeInfoFindVolumeById;
        }
        return null;
    }

    public static boolean isProfileOrDeviceOwner(UserManager userManager, DevicePolicyManager devicePolicyManager, String str) {
        List users = userManager.getUsers();
        if (devicePolicyManager.isDeviceOwnerAppOnAnyUser(str)) {
            return true;
        }
        int size = users.size();
        for (int i = 0; i < size; i++) {
            ComponentName profileOwnerAsUser = devicePolicyManager.getProfileOwnerAsUser(((UserInfo) users.get(i)).id);
            if (profileOwnerAsUser != null && profileOwnerAsUser.getPackageName().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static int getInstallationStatus(ApplicationInfo applicationInfo) {
        if ((applicationInfo.flags & 8388608) == 0) {
            return R.string.not_installed;
        }
        return applicationInfo.enabled ? R.string.installed : R.string.disabled;
    }

    private static boolean isVolumeValid(VolumeInfo volumeInfo) {
        return volumeInfo != null && volumeInfo.getType() == 1 && volumeInfo.isMountedReadable();
    }

    public static void setEditTextCursorPosition(EditText editText) {
        editText.setSelection(editText.getText().length());
    }

    public static void setSafeIcon(Preference preference, Drawable drawable) {
        if (drawable != null && !(drawable instanceof VectorDrawable)) {
            drawable = getSafeDrawable(drawable, 500, 500);
        }
        preference.setIcon(drawable);
    }

    public static Drawable getSafeDrawable(Drawable drawable, int i, int i2) {
        Bitmap bitmapCreateScaledBitmap;
        int minimumWidth = drawable.getMinimumWidth();
        int minimumHeight = drawable.getMinimumHeight();
        if (minimumWidth <= i && minimumHeight <= i2) {
            return drawable;
        }
        float f = minimumWidth;
        float f2 = minimumHeight;
        float fMin = Math.min(i / f, i2 / f2);
        int i3 = (int) (f * fMin);
        int i4 = (int) (f2 * fMin);
        if (drawable instanceof BitmapDrawable) {
            bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(), i3, i4, false);
        } else {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, i4, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            bitmapCreateScaledBitmap = bitmapCreateBitmap;
        }
        return new BitmapDrawable((Resources) null, bitmapCreateScaledBitmap);
    }

    public static Drawable getBadgedIcon(IconDrawableFactory iconDrawableFactory, PackageManager packageManager, String str, int i) {
        try {
            return iconDrawableFactory.getBadgedIcon(packageManager.getApplicationInfoAsUser(str, 128, i), i);
        } catch (PackageManager.NameNotFoundException e) {
            return packageManager.getDefaultActivityIcon();
        }
    }

    public static boolean isCharging(Intent intent) {
        int intExtra = intent.getIntExtra("plugged", 0);
        int intExtra2 = intent.getIntExtra("status", 1);
        if (intent.getBooleanExtra("present", false) && intExtra == 1) {
            return intExtra2 == 2 || intExtra2 == 5;
        }
        return false;
    }
}
