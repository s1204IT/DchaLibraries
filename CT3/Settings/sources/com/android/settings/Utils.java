package com.android.settings;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.preference.PreferenceFrameLayout;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.UserIcons;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils extends com.android.settingslib.Utils {
    public static final int[] BADNESS_COLORS = {0, -3917784, -1750760, -754944, -344276, -9986505, -16089278};
    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray<>();
    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context, PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {
        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }
        Intent intent = preference.getIntent();
        if (intent != null) {
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    preference.setIntent(new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    if ((flags & 1) != 0) {
                        preference.setTitle(resolveInfo.loadLabel(pm));
                        return true;
                    }
                    return true;
                }
            }
        }
        parentPreferenceGroup.removePreference(preference);
        return false;
    }

    public static UserManager getUserManager(Context context) {
        UserManager um = UserManager.get(context);
        if (um == null) {
            throw new IllegalStateException("Unable to load UserManager");
        }
        return um;
    }

    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        if (telephony != null) {
            return telephony.isVoiceCapable();
        }
        return false;
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }

    public static String getWifiIpAddresses(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        LinkProperties prop = cm.getLinkProperties(1);
        return formatIpAddresses(prop);
    }

    public static String getDefaultIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) {
            return null;
        }
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        if (!iter.hasNext()) {
            return null;
        }
        String addresses = "";
        while (iter.hasNext()) {
            addresses = addresses + iter.next().getHostAddress();
            if (iter.hasNext()) {
                addresses = addresses + "\n";
            }
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        if (localeStr == null) {
            return Locale.getDefault();
        }
        String[] brokenDownLocale = localeStr.split("_", 3);
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        }
        if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        }
        return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra("present", true);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    public static void forcePrepareCustomPreferencesList(ViewGroup parent, View child, ListView list, boolean ignoreSidePadding) {
        list.setScrollBarStyle(33554432);
        list.setClipToPadding(false);
        prepareCustomPreferencesList(parent, child, list, ignoreSidePadding);
    }

    public static void prepareCustomPreferencesList(ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        boolean movePadding = list.getScrollBarStyle() == 33554432;
        if (!movePadding) {
            return;
        }
        Resources res = list.getResources();
        int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
        int paddingBottom = res.getDimensionPixelSize(android.R.dimen.action_bar_title_text_size);
        if (parent instanceof PreferenceFrameLayout) {
            child.getLayoutParams().removeBorders = true;
            int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
            list.setPaddingRelative(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
            return;
        }
        list.setPaddingRelative(paddingSide, 0, paddingSide, paddingBottom);
    }

    public static void forceCustomPadding(View view, boolean additive) {
        Resources res = view.getResources();
        int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
        int paddingStart = paddingSide + (additive ? view.getPaddingStart() : 0);
        int paddingEnd = paddingSide + (additive ? view.getPaddingEnd() : 0);
        int paddingBottom = res.getDimensionPixelSize(android.R.dimen.action_bar_title_text_size);
        view.setPaddingRelative(paddingStart, 0, paddingEnd, paddingBottom);
    }

    public static void copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = ContactsContract.Profile.CONTENT_URI;
        int userId = user != null ? user.id : UserHandle.myUserId();
        InputStream avatarDataStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
        if (avatarDataStream == null) {
            assignDefaultPhoto(context, userId);
            return;
        }
        UserManager um = (UserManager) context.getSystemService("user");
        Bitmap icon = BitmapFactory.decodeStream(avatarDataStream);
        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
        } catch (IOException e) {
        }
    }

    public static void assignDefaultPhoto(Context context, int userId) {
        UserManager um = (UserManager) context.getSystemService("user");
        Bitmap bitmap = getDefaultUserIconAsBitmap(userId);
        um.setUserIcon(userId, bitmap);
    }

    public static String getMeProfileName(Context context, boolean full) {
        if (full) {
            return getProfileDisplayName(context);
        }
        return getShorterNameIfPossible(context);
    }

    private static String getShorterNameIfPossible(Context context) {
        String given = getLocalProfileGivenName(context);
        return !TextUtils.isEmpty(given) ? given : getProfileDisplayName(context);
    }

    private static String getLocalProfileGivenName(Context context) {
        ContentResolver cr = context.getContentResolver();
        Cursor structuredName = cr.query(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI, new String[]{"_id"}, "account_type IS NULL AND account_name IS NULL", null, null);
        if (structuredName == null) {
            return null;
        }
        try {
            if (!structuredName.moveToFirst()) {
                return null;
            }
            long localRowProfileId = structuredName.getLong(0);
            structuredName.close();
            structuredName = cr.query(ContactsContract.Profile.CONTENT_URI.buildUpon().appendPath("data").build(), new String[]{"data2", "data3"}, "raw_contact_id=" + localRowProfileId, null, null);
            if (structuredName == null) {
                return null;
            }
            try {
                if (!structuredName.moveToFirst()) {
                    return null;
                }
                String partialName = structuredName.getString(0);
                if (TextUtils.isEmpty(partialName)) {
                    partialName = structuredName.getString(1);
                }
                return partialName;
            } finally {
            }
        } finally {
        }
    }

    private static final String getProfileDisplayName(Context context) {
        ContentResolver cr = context.getContentResolver();
        Cursor profile = cr.query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
        if (profile == null) {
            return null;
        }
        try {
            if (profile.moveToFirst()) {
                return profile.getString(0);
            }
            return null;
        } finally {
            profile.close();
        }
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService("user")).getUsers().size() > 1;
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode, null, titleResId, title, false);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId, CharSequence title) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode, titleResPackageName, titleResId, title, false);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, null, titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResPackageName, titleResId, title, isShortcut);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragmentAsUser(Context context, String fragmentName, Bundle args, int titleResId, CharSequence title, boolean isShortcut, UserHandle userHandle) {
        if (userHandle.getIdentifier() == UserHandle.myUserId()) {
            startWithFragment(context, fragmentName, args, (Fragment) null, 0, titleResId, title, isShortcut);
            return;
        }
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, null, titleResId, title, isShortcut);
        intent.addFlags(268435456);
        intent.addFlags(32768);
        context.startActivityAsUser(intent, userHandle);
    }

    public static Intent onBuildStartFragmentIntent(Context context, String fragmentName, Bundle args, String titleResPackageName, int titleResId, CharSequence title, boolean isShortcut) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(context, SubSettings.class);
        intent.putExtra(":settings:show_fragment", fragmentName);
        intent.putExtra(":settings:show_fragment_args", args);
        intent.putExtra(":settings:show_fragment_title_res_package_name", titleResPackageName);
        intent.putExtra(":settings:show_fragment_title_resid", titleResId);
        intent.putExtra(":settings:show_fragment_title", title);
        intent.putExtra(":settings:show_fragment_as_shortcut", isShortcut);
        return intent;
    }

    public static UserHandle getManagedProfile(UserManager userManager) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            UserHandle profile = userProfiles.get(i);
            if (profile.getIdentifier() != userManager.getUserHandle()) {
                UserInfo userInfo = userManager.getUserInfo(profile.getIdentifier());
                if (userInfo.isManagedProfile()) {
                    return profile;
                }
            }
        }
        return null;
    }

    public static boolean isManagedProfile(UserManager userManager) {
        return isManagedProfile(userManager, UserHandle.myUserId());
    }

    public static int getManagedProfileId(UserManager um, int parentUserId) {
        int[] profileIds = um.getProfileIdsWithDisabled(parentUserId);
        for (int profileId : profileIds) {
            if (profileId != parentUserId) {
                return profileId;
            }
        }
        return -10000;
    }

    public static boolean isManagedProfile(UserManager userManager, int userId) {
        if (userManager == null) {
            throw new IllegalArgumentException("userManager must not be null");
        }
        UserInfo userInfo = userManager.getUserInfo(userId);
        if (userInfo != null) {
            return userInfo.isManagedProfile();
        }
        return false;
    }

    public static UserHandle getSecureTargetUser(IBinder activityToken, UserManager um, Bundle arguments, Bundle intentExtras) {
        UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            String launchedFromPackage = am.getLaunchedFromPackage(activityToken);
            boolean launchedFromSettingsApp = "com.android.settings".equals(launchedFromPackage);
            UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser != null && !launchedFromUser.equals(currentUser) && isProfileOf(um, launchedFromUser)) {
                return launchedFromUser;
            }
            UserHandle extrasUser = intentExtras != null ? (UserHandle) intentExtras.getParcelable("android.intent.extra.USER") : null;
            if (extrasUser != null && !extrasUser.equals(currentUser) && launchedFromSettingsApp && isProfileOf(um, extrasUser)) {
                return extrasUser;
            }
            UserHandle argumentsUser = arguments != null ? (UserHandle) arguments.getParcelable("android.intent.extra.USER") : null;
            if (argumentsUser != null && !argumentsUser.equals(currentUser) && launchedFromSettingsApp) {
                if (isProfileOf(um, argumentsUser)) {
                    return argumentsUser;
                }
            }
        } catch (RemoteException e) {
            Log.v("Settings", "Could not talk to activity manager.", e);
        }
        return currentUser;
    }

    private static boolean isProfileOf(UserManager um, UserHandle otherUser) {
        if (um == null || otherUser == null) {
            return false;
        }
        if (UserHandle.myUserId() != otherUser.getIdentifier()) {
            return um.getUserProfiles().contains(otherUser);
        }
        return true;
    }

    static boolean isOemUnlockEnabled(Context context) {
        PersistentDataBlockManager manager = (PersistentDataBlockManager) context.getSystemService("persistent_data_block");
        return manager.getOemUnlockEnabled();
    }

    static void setOemUnlockEnabled(Context context, boolean enabled) {
        PersistentDataBlockManager manager = (PersistentDataBlockManager) context.getSystemService("persistent_data_block");
        manager.setOemUnlockEnabled(enabled);
    }

    public static boolean showSimCardTile(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        return tm.getSimCount() > 1;
    }

    public static String formatElapsedTime(Context context, double millis, boolean withSeconds) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000.0d);
        if (!withSeconds) {
            seconds += 30;
        }
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (seconds >= 86400) {
            days = seconds / 86400;
            seconds -= 86400 * days;
        }
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds -= hours * 3600;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        if (withSeconds) {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days, Integer.valueOf(days), Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours, Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)));
            } else if (minutes > 0) {
                sb.append(context.getString(R.string.battery_history_minutes, Integer.valueOf(minutes), Integer.valueOf(seconds)));
            } else {
                sb.append(context.getString(R.string.battery_history_seconds, Integer.valueOf(seconds)));
            }
        } else if (days > 0) {
            sb.append(context.getString(R.string.battery_history_days_no_seconds, Integer.valueOf(days), Integer.valueOf(hours), Integer.valueOf(minutes)));
        } else if (hours > 0) {
            sb.append(context.getString(R.string.battery_history_hours_no_seconds, Integer.valueOf(hours), Integer.valueOf(minutes)));
        } else {
            sb.append(context.getString(R.string.battery_history_minutes_no_seconds, Integer.valueOf(minutes)));
        }
        return sb.toString();
    }

    public static UserInfo getExistingUser(UserManager userManager, UserHandle checkUser) {
        List<UserInfo> users = userManager.getUsers(true);
        int checkUserId = checkUser.getIdentifier();
        for (UserInfo user : users) {
            if (user.id == checkUserId) {
                return user;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
        TypedArray a = inflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
        int resId = a.getResourceId(3, 0);
        a.recycle();
        return inflater.inflate(resId, parent, false);
    }

    public static boolean isLowStorage(Context context) {
        StorageManager sm = StorageManager.from(context);
        return sm.getStorageBytesUntilLow(context.getFilesDir()) < 0;
    }

    public static Bitmap getDefaultUserIconAsBitmap(int userId) {
        Bitmap bitmap = sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap == null) {
            Bitmap bitmap2 = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(userId, false));
            sDarkDefaultUserBitmapCache.put(userId, bitmap2);
            return bitmap2;
        }
        return bitmap;
    }

    public static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        List<IntentFilterVerificationInfo> iviList = pm.getIntentFilterVerifications(packageName);
        List<IntentFilter> filters = pm.getAllIntentFilters(packageName);
        ArraySet<String> result = new ArraySet<>();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory("android.intent.category.BROWSABLE") && (filter.hasDataScheme("http") || filter.hasDataScheme("https"))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }

    public static void handleLoadingContainer(View loading, View doneLoading, boolean done, boolean animate) {
        setViewShown(loading, !done, animate);
        setViewShown(doneLoading, done, animate);
    }

    private static void setViewShown(final View view, boolean shown, boolean animate) {
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), shown ? android.R.anim.fade_in : android.R.anim.fade_out);
            if (shown) {
                view.setVisibility(0);
            } else {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation2) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation2) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation2) {
                        view.setVisibility(4);
                    }
                });
            }
            view.startAnimation(animation);
            return;
        }
        view.clearAnimation();
        view.setVisibility(shown ? 0 : 4);
    }

    public static ApplicationInfo getAdminApplicationInfo(Context context, int profileId) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        ComponentName mdmPackage = dpm.getProfileOwnerAsUser(profileId);
        if (mdmPackage == null) {
            return null;
        }
        String mdmPackageName = mdmPackage.getPackageName();
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo mdmApplicationInfo = ipm.getApplicationInfo(mdmPackageName, 0, profileId);
            return mdmApplicationInfo;
        } catch (RemoteException e) {
            Log.e("Settings", "Error while retrieving application info for package " + mdmPackageName + ", userId " + profileId, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        INetworkManagementService netManager = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        try {
            return netManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static SpannableString createAccessibleSequence(CharSequence displayText, String accessibileText) {
        SpannableString str = new SpannableString(displayText);
        str.setSpan(new TtsSpan.TextBuilder(accessibileText).build(), 0, displayText.length(), 18);
        return str;
    }

    public static int getUserIdFromBundle(Context context, Bundle bundle) {
        if (bundle == null) {
            return getCredentialOwnerUserId(context);
        }
        int userId = bundle.getInt("android.intent.extra.USER_ID", UserHandle.myUserId());
        return enforceSameOwner(context, userId);
    }

    public static int enforceSameOwner(Context context, int userId) {
        UserManager um = getUserManager(context);
        int[] profileIds = um.getProfileIdsWithDisabled(UserHandle.myUserId());
        if (ArrayUtils.contains(profileIds, userId)) {
            return userId;
        }
        throw new SecurityException("Given user id " + userId + " does not belong to user " + UserHandle.myUserId());
    }

    public static int getCredentialOwnerUserId(Context context) {
        return getCredentialOwnerUserId(context, UserHandle.myUserId());
    }

    public static int getCredentialOwnerUserId(Context context, int userId) {
        UserManager um = getUserManager(context);
        return um.getCredentialOwnerProfile(userId);
    }

    public static String formatDateRange(Context context, long start, long end) {
        String string;
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            string = DateUtils.formatDateRange(context, sFormatter, start, end, 65552, null).toString();
        }
        return string;
    }

    public static List<String> getNonIndexable(int xml, Context context) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        List<String> ret = new ArrayList<>();
        PreferenceManager manager = new PreferenceManager(context);
        PreferenceScreen screen = manager.inflateFromResource(context, xml, null);
        checkPrefs(screen, ret);
        return ret;
    }

    private static void checkPrefs(PreferenceGroup group, List<String> ret) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if ((preference instanceof SelfAvailablePreference) && !((SelfAvailablePreference) preference).isAvailable(group.getContext())) {
                ret.add(preference.getKey());
                if (preference instanceof PreferenceGroup) {
                    addAll((PreferenceGroup) preference, ret);
                }
            } else if (preference instanceof PreferenceGroup) {
                checkPrefs((PreferenceGroup) preference, ret);
            }
        }
    }

    private static void addAll(PreferenceGroup group, List<String> ret) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            ret.add(pref.getKey());
            if (pref instanceof PreferenceGroup) {
                addAll((PreferenceGroup) pref, ret);
            }
        }
    }

    public static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public static boolean startQuietModeDialogIfNecessary(Context context, UserManager um, int userId) {
        if (um.isQuietModeEnabled(UserHandle.of(userId))) {
            Intent intent = UnlaunchableAppActivity.createInQuietModeDialogIntent(userId);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static CharSequence getApplicationLabel(Context context, String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 8704);
            return appInfo.loadLabel(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Settings", "Unable to find info for package: " + packageName);
            return null;
        }
    }

    public static boolean isPackageEnabled(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isCharging(Intent intent) {
        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", 1);
        boolean present = intent.getBooleanExtra("present", false);
        if (present && plugType == 1) {
            return status == 2 || status == 5;
        }
        return false;
    }
}
