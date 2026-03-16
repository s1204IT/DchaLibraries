package com.android.settings;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.util.UserIcons;
import com.android.settings.UserSpinnerAdapter;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.drawable.CircleFramedDrawable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils {
    public static final int[] BADNESS_COLORS = {0, -3917784, -1750760, -754944, -344276, -9986505, -16089278};
    private static Signature[] sSystemSignature;

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
                    }
                    return true;
                }
            }
        }
        parentPreferenceGroup.removePreference(preference);
        return false;
    }

    public static boolean updateTileToSpecificActivityFromMetaDataOrRemove(Context context, DashboardTile tile) {
        Intent intent = tile.intent;
        if (intent != null) {
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 128);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    String title = null;
                    String summary = null;
                    try {
                        Resources res = pm.getResourcesForApplication(resolveInfo.activityInfo.packageName);
                        Bundle metaData = resolveInfo.activityInfo.metaData;
                        if (res != null && metaData != null) {
                            res.getDrawable(metaData.getInt("com.android.settings.icon"), null);
                            title = res.getString(metaData.getInt("com.android.settings.title"));
                            summary = res.getString(metaData.getInt("com.android.settings.summary"));
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    } catch (Resources.NotFoundException e2) {
                    }
                    if (TextUtils.isEmpty(title)) {
                        title = resolveInfo.loadLabel(pm).toString();
                    }
                    tile.title = title;
                    tile.summary = summary;
                    tile.intent = new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        return telephony != null && telephony.isVoiceCapable();
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
        String addresses = null;
        if (prop != null) {
            Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
            if (iter.hasNext()) {
                addresses = "";
                while (iter.hasNext()) {
                    addresses = addresses + iter.next().getHostAddress();
                    if (iter.hasNext()) {
                        addresses = addresses + "\n";
                    }
                }
            }
        }
        return addresses;
    }

    public static String getImsStatus(Context context) {
        String retval = context.getString(R.string.ims_not_registered);
        ImsPhone imsPhone = null;
        try {
            boolean isSim2Master = Dsds.isSim2Master();
            int masterSimId = isSim2Master ? PhoneConstants.SimId.SIM2.ordinal() : PhoneConstants.SimId.SIM1.ordinal();
            imsPhone = (ImsPhone) PhoneFactory.getPhone(masterSimId).getImsPhone();
        } catch (IllegalStateException e) {
            Log.w("Settings", "IMS phone hasn't been made yet!", e);
        }
        if (imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            return context.getString(R.string.ims_registered);
        }
        return retval;
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

    public static String formatPercentage(long amount, long total) {
        return formatPercentage(amount / total);
    }

    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0d);
    }

    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra("present", true);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    public static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        int scale = batteryChangedIntent.getIntExtra("scale", 100);
        return (level * 100) / scale;
    }

    public static String getBatteryStatus(Resources res, Intent batteryChangedIntent) {
        int resId;
        int plugType = batteryChangedIntent.getIntExtra("plugged", 0);
        int status = batteryChangedIntent.getIntExtra("status", 1);
        if (status == 2) {
            if (plugType == 1) {
                resId = R.string.battery_info_status_charging_ac;
            } else if (plugType == 2) {
                resId = R.string.battery_info_status_charging_usb;
            } else if (plugType == 4) {
                resId = R.string.battery_info_status_charging_wireless;
            } else {
                resId = R.string.battery_info_status_charging;
            }
            String statusString = res.getString(resId);
            return statusString;
        }
        if (status == 3) {
            String statusString2 = res.getString(R.string.battery_info_status_discharging);
            return statusString2;
        }
        if (status == 4) {
            String statusString3 = res.getString(R.string.battery_info_status_not_charging);
            return statusString3;
        }
        if (status == 5) {
            String statusString4 = res.getString(R.string.battery_info_status_full);
            return statusString4;
        }
        String statusString5 = res.getString(R.string.battery_info_status_unknown);
        return statusString5;
    }

    public static boolean isCharging(Intent intent) {
        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", 1);
        return plugType == 1 && (status == 2 || status == 5);
    }

    public static void forcePrepareCustomPreferencesList(ViewGroup parent, View child, ListView list, boolean ignoreSidePadding) {
        list.setScrollBarStyle(33554432);
        list.setClipToPadding(false);
        prepareCustomPreferencesList(parent, child, list, ignoreSidePadding);
    }

    public static void prepareCustomPreferencesList(ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        boolean movePadding = list.getScrollBarStyle() == 33554432;
        if (movePadding) {
            Resources res = list.getResources();
            int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
            int paddingBottom = res.getDimensionPixelSize(android.R.dimen.action_bar_margin);
            if (parent instanceof PreferenceFrameLayout) {
                child.getLayoutParams().removeBorders = true;
                int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
                list.setPaddingRelative(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
                return;
            }
            list.setPaddingRelative(paddingSide, 0, paddingSide, paddingBottom);
        }
    }

    public static void forceCustomPadding(View view, boolean additive) {
        Resources res = view.getResources();
        int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
        int paddingStart = paddingSide + (additive ? view.getPaddingStart() : 0);
        int paddingEnd = paddingSide + (additive ? view.getPaddingEnd() : 0);
        int paddingBottom = res.getDimensionPixelSize(android.R.dimen.action_bar_margin);
        view.setPaddingRelative(paddingStart, 0, paddingEnd, paddingBottom);
    }

    public static int getTetheringLabel(ConnectivityManager cm) {
        String[] usbRegexs = cm.getTetherableUsbRegexs();
        String[] wifiRegexs = cm.getTetherableWifiRegexs();
        String[] bluetoothRegexs = cm.getTetherableBluetoothRegexs();
        boolean usbAvailable = usbRegexs.length != 0;
        boolean wifiAvailable = wifiRegexs.length != 0;
        boolean bluetoothAvailable = bluetoothRegexs.length != 0;
        if (wifiAvailable && usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable && usbAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_all;
        }
        if (wifiAvailable) {
            return R.string.tether_settings_title_wifi;
        }
        if (usbAvailable && bluetoothAvailable) {
            return R.string.tether_settings_title_usb_bluetooth;
        }
        if (usbAvailable) {
            return R.string.tether_settings_title_usb;
        }
        return R.string.tether_settings_title_bluetooth;
    }

    public static boolean copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = ContactsContract.Profile.CONTENT_URI;
        InputStream avatarDataStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
        if (avatarDataStream == null) {
            return false;
        }
        int userId = user != null ? user.id : UserHandle.myUserId();
        UserManager um = (UserManager) context.getSystemService("user");
        Bitmap icon = BitmapFactory.decodeStream(avatarDataStream);
        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    public static String getMeProfileName(Context context, boolean full) {
        return full ? getProfileDisplayName(context) : getShorterNameIfPossible(context);
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
                structuredName.close();
                return partialName;
            } finally {
            }
        } finally {
        }
    }

    private static final String getProfileDisplayName(Context context) {
        String string = null;
        ContentResolver cr = context.getContentResolver();
        Cursor profile = cr.query(ContactsContract.Profile.CONTENT_URI, new String[]{"display_name"}, null, null, null);
        if (profile != null) {
            try {
                if (profile.moveToFirst()) {
                    string = profile.getString(0);
                }
            } finally {
                profile.close();
            }
        }
        return string;
    }

    public static Dialog buildGlobalChangeWarningDialog(Context context, int titleResId, final Runnable positiveAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleResId);
        builder.setMessage(R.string.global_change_warning);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveAction.run();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        return builder.create();
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
        UserInfo currentUser = userManager.getUserInfo(userManager.getUserHandle());
        return currentUser.isManagedProfile();
    }

    public static UserSpinnerAdapter createUserSpinnerAdapter(UserManager userManager, Context context) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        if (userProfiles.size() < 2) {
            return null;
        }
        UserHandle myUserHandle = new UserHandle(UserHandle.myUserId());
        userProfiles.remove(myUserHandle);
        userProfiles.add(0, myUserHandle);
        ArrayList<UserSpinnerAdapter.UserDetails> userDetails = new ArrayList<>(userProfiles.size());
        int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            userDetails.add(new UserSpinnerAdapter.UserDetails(userProfiles.get(i), userManager, context));
        }
        return new UserSpinnerAdapter(context, userDetails);
    }

    public static UserHandle getSecureTargetUser(IBinder activityToken, UserManager um, Bundle arguments, Bundle intentExtras) {
        UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            String launchedFromPackage = am.getLaunchedFromPackage(activityToken);
            boolean launchedFromSettingsApp = "com.android.settings".equals(launchedFromPackage);
            UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser == null || launchedFromUser.equals(currentUser) || !isProfileOf(um, launchedFromUser)) {
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
            } else {
                return launchedFromUser;
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
        return UserHandle.myUserId() == otherUser.getIdentifier() || um.getUserProfiles().contains(otherUser);
    }

    public static Dialog createRemoveConfirmationDialog(Context context, int removingUserId, DialogInterface.OnClickListener onConfirmListener) {
        int titleResId;
        int messageResId;
        UserManager um = (UserManager) context.getSystemService("user");
        UserInfo userInfo = um.getUserInfo(removingUserId);
        if (UserHandle.myUserId() == removingUserId) {
            titleResId = R.string.user_confirm_remove_self_title;
            messageResId = R.string.user_confirm_remove_self_message;
        } else if (userInfo.isRestricted()) {
            titleResId = R.string.user_profile_confirm_remove_title;
            messageResId = R.string.user_profile_confirm_remove_message;
        } else if (userInfo.isManagedProfile()) {
            titleResId = R.string.work_profile_confirm_remove_title;
            messageResId = R.string.work_profile_confirm_remove_message;
        } else {
            titleResId = R.string.user_confirm_remove_title;
            messageResId = R.string.user_confirm_remove_message;
        }
        Dialog dlg = new AlertDialog.Builder(context).setTitle(titleResId).setMessage(messageResId).setPositiveButton(R.string.user_delete_button, onConfirmListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        return dlg;
    }

    static boolean isOemUnlockEnabled(Context context) {
        PersistentDataBlockManager manager = (PersistentDataBlockManager) context.getSystemService("persistent_data_block");
        return manager.getOemUnlockEnabled();
    }

    static void setOemUnlockEnabled(Context context, boolean enabled) {
        PersistentDataBlockManager manager = (PersistentDataBlockManager) context.getSystemService("persistent_data_block");
        manager.setOemUnlockEnabled(enabled);
    }

    public static Drawable getUserIcon(Context context, UserManager um, UserInfo user) {
        Bitmap icon;
        if (user.isManagedProfile()) {
            Bitmap b = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.emo_im_wtf);
            return CircleFramedDrawable.getInstance(context, b);
        }
        if (user.iconPath != null && (icon = um.getUserIcon(user.id)) != null) {
            return CircleFramedDrawable.getInstance(context, icon);
        }
        return UserIcons.getDefaultUserIcon(user.id, false);
    }

    public static String getUserLabel(Context context, UserInfo info) {
        if (info.isManagedProfile()) {
            return context.getString(R.string.managed_user_title);
        }
        String name = info != null ? info.name : null;
        if (name == null && info != null) {
            name = Integer.toString(info.id);
        } else if (info == null) {
            name = context.getString(R.string.unknown);
        }
        return context.getResources().getString(R.string.running_process_item_user_label, name);
    }

    public static boolean showSimCardTile(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        return tm.getSimCount() > 1;
    }

    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(pm)};
        }
        return sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(pkg));
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg == null || pkg.signatures == null || pkg.signatures.length <= 0) {
            return null;
        }
        return pkg.signatures[0];
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            PackageInfo sys = pm.getPackageInfo("android", 64);
            return getFirstSignature(sys);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
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

    public static SubscriptionInfo findRecordBySubId(Context context, int subId) {
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            int subInfoLength = subInfoList.size();
            for (int i = 0; i < subInfoLength; i++) {
                SubscriptionInfo sir = subInfoList.get(i);
                if (sir != null && sir.getSubscriptionId() == subId) {
                    return sir;
                }
            }
        }
        return null;
    }

    public static SubscriptionInfo findRecordBySlotId(Context context, int slotId) {
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            int subInfoLength = subInfoList.size();
            for (int i = 0; i < subInfoLength; i++) {
                SubscriptionInfo sir = subInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    return sir;
                }
            }
        }
        return null;
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
}
