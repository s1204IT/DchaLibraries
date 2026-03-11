package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.PackageManagerHelper;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final Object sLock = new Object();
    private static boolean mUseInstallQueue = false;

    private static void addToInstallQueue(SharedPreferences sharedPrefs, PendingInstallShortcutInfo info) {
        Set<String> strings;
        synchronized (sLock) {
            String encoded = info.encodeToString();
            if (encoded != null) {
                Set<String> strings2 = sharedPrefs.getStringSet("apps_to_install", null);
                if (strings2 == null) {
                    strings = new HashSet<>(1);
                } else {
                    strings = new HashSet<>(strings2);
                }
                strings.add(encoded);
                sharedPrefs.edit().putStringSet("apps_to_install", strings).apply();
            }
        }
    }

    public static void removeFromInstallQueue(Context context, HashSet<String> packageNames, UserHandleCompat user) {
        if (packageNames.isEmpty()) {
            return;
        }
        SharedPreferences sp = Utilities.getPrefs(context);
        synchronized (sLock) {
            Set<String> strings = sp.getStringSet("apps_to_install", null);
            if (strings != null) {
                Set<String> newStrings = new HashSet<>(strings);
                Iterator<String> newStringsIter = newStrings.iterator();
                while (newStringsIter.hasNext()) {
                    String encoded = newStringsIter.next();
                    PendingInstallShortcutInfo info = decode(encoded, context);
                    if (info == null || (packageNames.contains(info.getTargetPackage()) && user.equals(info.user))) {
                        newStringsIter.remove();
                    }
                }
                sp.edit().putStringSet("apps_to_install", newStrings).apply();
            }
        }
    }

    private static ArrayList<PendingInstallShortcutInfo> getAndClearInstallQueue(SharedPreferences sharedPrefs, Context context) {
        synchronized (sLock) {
            Set<String> strings = sharedPrefs.getStringSet("apps_to_install", null);
            if (strings == null) {
                return new ArrayList<>();
            }
            ArrayList<PendingInstallShortcutInfo> infos = new ArrayList<>();
            for (String encoded : strings) {
                PendingInstallShortcutInfo info = decode(encoded, context);
                if (info != null) {
                    infos.add(info);
                }
            }
            sharedPrefs.edit().putStringSet("apps_to_install", new HashSet()).apply();
            return infos;
        }
    }

    @Override
    public void onReceive(Context context, Intent data) {
        PendingInstallShortcutInfo info;
        if (!"com.android.launcher.action.INSTALL_SHORTCUT".equals(data.getAction()) || (info = createPendingInfo(context, data)) == null) {
            return;
        }
        if (!info.isLauncherActivity() && !PackageManagerHelper.hasPermissionForActivity(context, info.launchIntent, null)) {
            Log.e("InstallShortcutReceiver", "Ignoring malicious intent " + info.launchIntent.toUri(0));
        } else {
            queuePendingShortcutInfo(info, context);
        }
    }

    private static boolean isValidExtraType(Intent intent, String key, Class type) {
        Object extra = intent.getParcelableExtra(key);
        if (extra != null) {
            return type.isInstance(extra);
        }
        return true;
    }

    private static PendingInstallShortcutInfo createPendingInfo(Context context, Intent data) {
        if (!isValidExtraType(data, "android.intent.extra.shortcut.INTENT", Intent.class) || !isValidExtraType(data, "android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.class) || !isValidExtraType(data, "android.intent.extra.shortcut.ICON", Bitmap.class)) {
            return null;
        }
        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context);
        if (info.launchIntent == null || info.label == null) {
            return null;
        }
        return convertToLauncherActivityIfPossible(info);
    }

    public static ShortcutInfo fromShortcutIntent(Context context, Intent data) {
        PendingInstallShortcutInfo info = createPendingInfo(context, data);
        if (info == null) {
            return null;
        }
        return info.getShortcutInfo();
    }

    private static void queuePendingShortcutInfo(PendingInstallShortcutInfo info, Context context) {
        LauncherAppState app = LauncherAppState.getInstance();
        boolean launcherNotLoaded = app.getModel().getCallback() == null;
        addToInstallQueue(Utilities.getPrefs(context), info);
        if (mUseInstallQueue || launcherNotLoaded) {
            return;
        }
        flushInstallQueue(context);
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }

    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }

    static void flushInstallQueue(Context context) {
        SharedPreferences sp = Utilities.getPrefs(context);
        ArrayList<PendingInstallShortcutInfo> installQueue = getAndClearInstallQueue(sp, context);
        if (installQueue.isEmpty()) {
            return;
        }
        ArrayList<ItemInfo> addShortcuts = new ArrayList<>();
        for (PendingInstallShortcutInfo pendingInfo : installQueue) {
            String packageName = pendingInfo.getTargetPackage();
            if (!TextUtils.isEmpty(packageName)) {
                UserHandleCompat myUserHandle = UserHandleCompat.myUserHandle();
                if (LauncherModel.isValidPackage(context, packageName, myUserHandle)) {
                }
            }
            addShortcuts.add(pendingInfo.getShortcutInfo());
        }
        if (addShortcuts.isEmpty()) {
            return;
        }
        LauncherAppState app = LauncherAppState.getInstance();
        app.getModel().addAndBindAddedWorkspaceItems(context, addShortcuts);
    }

    static CharSequence ensureValidName(Context context, Intent intent, CharSequence name) {
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
                return info.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                return "";
            }
        }
        return name;
    }

    private static class PendingInstallShortcutInfo {
        final LauncherActivityInfoCompat activityInfo;
        final Intent data;
        final String label;
        final Intent launchIntent;
        final Context mContext;
        final UserHandleCompat user;

        public PendingInstallShortcutInfo(Intent data, Context context) {
            this.data = data;
            this.mContext = context;
            this.launchIntent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            this.label = data.getStringExtra("android.intent.extra.shortcut.NAME");
            this.user = UserHandleCompat.myUserHandle();
            this.activityInfo = null;
        }

        public PendingInstallShortcutInfo(LauncherActivityInfoCompat info, Context context) {
            this.data = null;
            this.mContext = context;
            this.activityInfo = info;
            this.user = info.getUser();
            this.launchIntent = AppInfo.makeLaunchIntent(context, info, this.user);
            this.label = info.getLabel().toString();
        }

        public String encodeToString() {
            if (this.activityInfo != null) {
                try {
                    return new JSONStringer().object().key("intent.launch").value(this.launchIntent.toUri(0)).key("isAppShortcut").value(true).key("userHandle").value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                } catch (JSONException e) {
                    Log.d("InstallShortcutReceiver", "Exception when adding shortcut: " + e);
                    return null;
                }
            }
            if (this.launchIntent.getAction() == null) {
                this.launchIntent.setAction("android.intent.action.VIEW");
            } else if (this.launchIntent.getAction().equals("android.intent.action.MAIN") && this.launchIntent.getCategories() != null && this.launchIntent.getCategories().contains("android.intent.category.LAUNCHER")) {
                this.launchIntent.addFlags(270532608);
            }
            String name = InstallShortcutReceiver.ensureValidName(this.mContext, this.launchIntent, this.label).toString();
            Bitmap icon = (Bitmap) this.data.getParcelableExtra("android.intent.extra.shortcut.ICON");
            Intent.ShortcutIconResource iconResource = (Intent.ShortcutIconResource) this.data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            try {
                JSONStringer json = new JSONStringer().object().key("intent.launch").value(this.launchIntent.toUri(0)).key("name").value(name);
                if (icon != null) {
                    byte[] iconByteArray = Utilities.flattenBitmap(icon);
                    json = json.key("icon").value(Base64.encodeToString(iconByteArray, 0, iconByteArray.length, 0));
                }
                if (iconResource != null) {
                    json = json.key("iconResource").value(iconResource.resourceName).key("iconResourcePackage").value(iconResource.packageName);
                }
                return json.endObject().toString();
            } catch (JSONException e2) {
                Log.d("InstallShortcutReceiver", "Exception when adding shortcut: " + e2);
                return null;
            }
        }

        public ShortcutInfo getShortcutInfo() {
            if (this.activityInfo != null) {
                return ShortcutInfo.fromActivityInfo(this.activityInfo, this.mContext);
            }
            return LauncherAppState.getInstance().getModel().infoFromShortcutIntent(this.mContext, this.data);
        }

        public String getTargetPackage() {
            String packageName = this.launchIntent.getPackage();
            if (packageName == null) {
                if (this.launchIntent.getComponent() == null) {
                    return null;
                }
                return this.launchIntent.getComponent().getPackageName();
            }
            return packageName;
        }

        public boolean isLauncherActivity() {
            return this.activityInfo != null;
        }
    }

    private static PendingInstallShortcutInfo decode(String encoded, Context context) {
        LauncherActivityInfoCompat info;
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent launcherIntent = Intent.parseUri(object.getString("intent.launch"), 0);
            if (object.optBoolean("isAppShortcut")) {
                UserHandleCompat user = UserManagerCompat.getInstance(context).getUserForSerialNumber(object.getLong("userHandle"));
                if (user == null || (info = LauncherAppsCompat.getInstance(context).resolveActivity(launcherIntent, user)) == null) {
                    return null;
                }
                return new PendingInstallShortcutInfo(info, context);
            }
            Intent data = new Intent();
            data.putExtra("android.intent.extra.shortcut.INTENT", launcherIntent);
            data.putExtra("android.intent.extra.shortcut.NAME", object.getString("name"));
            String iconBase64 = object.optString("icon");
            String iconResourceName = object.optString("iconResource");
            String iconResourcePackageName = object.optString("iconResourcePackage");
            if (iconBase64 != null && !iconBase64.isEmpty()) {
                byte[] iconArray = Base64.decode(iconBase64, 0);
                Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                data.putExtra("android.intent.extra.shortcut.ICON", b);
            } else if (iconResourceName != null && !iconResourceName.isEmpty()) {
                Intent.ShortcutIconResource iconResource = new Intent.ShortcutIconResource();
                iconResource.resourceName = iconResourceName;
                iconResource.packageName = iconResourcePackageName;
                data.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", iconResource);
            }
            return new PendingInstallShortcutInfo(data, context);
        } catch (URISyntaxException | JSONException e) {
            Log.d("InstallShortcutReceiver", "Exception reading shortcut to add: " + e);
            return null;
        }
    }

    private static PendingInstallShortcutInfo convertToLauncherActivityIfPossible(PendingInstallShortcutInfo original) {
        if (original.isLauncherActivity() || !Utilities.isLauncherAppTarget(original.launchIntent) || !original.user.equals(UserHandleCompat.myUserHandle())) {
            return original;
        }
        PackageManager pm = original.mContext.getPackageManager();
        ResolveInfo info = pm.resolveActivity(original.launchIntent, 0);
        if (info == null) {
            return original;
        }
        LauncherActivityInfoCompat launcherInfo = LauncherActivityInfoCompat.fromResolveInfo(info, original.mContext);
        return new PendingInstallShortcutInfo(launcherInfo, original.mContext);
    }
}
