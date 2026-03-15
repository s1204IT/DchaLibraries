package com.android.launcher3;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.Preconditions;
import com.android.launcher3.util.Provider;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String APPS_PENDING_INSTALL = "apps_to_install";
    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final String APP_WIDGET_TYPE_KEY = "isAppWidget";
    private static final boolean DBG = false;
    private static final String DEEPSHORTCUT_TYPE_KEY = "isDeepShortcut";
    public static final int FLAG_ACTIVITY_PAUSED = 1;
    public static final int FLAG_BULK_ADD = 4;
    public static final int FLAG_DRAG_AND_DROP = 4;
    public static final int FLAG_LOADER_RUNNING = 2;
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";
    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final int MSG_ADD_TO_QUEUE = 1;
    private static final int MSG_FLUSH_QUEUE = 2;
    private static final String NAME_KEY = "name";
    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;
    private static final String TAG = "InstallShortcutReceiver";
    private static final String USER_HANDLE_KEY = "userHandle";
    private static int sInstallQueueDisabledFlags = 0;
    private static final Handler sHandler = new Handler(LauncherModel.getWorkerLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Pair pair = (Pair) message.obj;
                    String strEncodeToString = ((PendingInstallShortcutInfo) pair.second).encodeToString();
                    SharedPreferences prefs = Utilities.getPrefs((Context) pair.first);
                    Set<String> stringSet = prefs.getStringSet(InstallShortcutReceiver.APPS_PENDING_INSTALL, null);
                    HashSet hashSet = stringSet != null ? new HashSet(stringSet) : new HashSet(1);
                    hashSet.add(strEncodeToString);
                    prefs.edit().putStringSet(InstallShortcutReceiver.APPS_PENDING_INSTALL, hashSet).apply();
                    break;
                case 2:
                    Context context = (Context) message.obj;
                    LauncherModel model = LauncherAppState.getInstance(context).getModel();
                    if (model.getCallback() != null) {
                        ArrayList arrayList = new ArrayList();
                        SharedPreferences prefs2 = Utilities.getPrefs(context);
                        Set<String> stringSet2 = prefs2.getStringSet(InstallShortcutReceiver.APPS_PENDING_INSTALL, null);
                        if (stringSet2 != null) {
                            LauncherAppsCompat launcherAppsCompat = LauncherAppsCompat.getInstance(context);
                            Iterator<String> it = stringSet2.iterator();
                            while (it.hasNext()) {
                                PendingInstallShortcutInfo pendingInstallShortcutInfoDecode = InstallShortcutReceiver.decode(it.next(), context);
                                if (pendingInstallShortcutInfoDecode != null) {
                                    String intentPackage = InstallShortcutReceiver.getIntentPackage(pendingInstallShortcutInfoDecode.launchIntent);
                                    if (TextUtils.isEmpty(intentPackage) || launcherAppsCompat.isPackageEnabledForProfile(intentPackage, pendingInstallShortcutInfoDecode.user)) {
                                        arrayList.add(pendingInstallShortcutInfoDecode.getItemInfo());
                                    }
                                }
                            }
                            prefs2.edit().remove(InstallShortcutReceiver.APPS_PENDING_INSTALL).apply();
                            if (!arrayList.isEmpty()) {
                                model.addAndBindAddedWorkspaceItems(arrayList);
                            }
                            break;
                        }
                    }
                    break;
            }
        }
    };

    public static void removeFromInstallQueue(Context context, HashSet<String> hashSet, UserHandle userHandle) {
        if (hashSet.isEmpty()) {
            return;
        }
        Preconditions.assertWorkerThread();
        SharedPreferences prefs = Utilities.getPrefs(context);
        Set<String> stringSet = prefs.getStringSet(APPS_PENDING_INSTALL, null);
        if (Utilities.isEmpty(stringSet)) {
            return;
        }
        HashSet hashSet2 = new HashSet(stringSet);
        Iterator<String> it = hashSet2.iterator();
        while (it.hasNext()) {
            try {
                Decoder decoder = new Decoder(it.next(), context);
                if (hashSet.contains(getIntentPackage(decoder.launcherIntent)) && userHandle.equals(decoder.user)) {
                    it.remove();
                }
            } catch (URISyntaxException | JSONException e) {
                Log.d(TAG, "Exception reading shortcut to add: " + e);
                it.remove();
            }
        }
        prefs.edit().putStringSet(APPS_PENDING_INSTALL, hashSet2).apply();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingInstallShortcutInfo pendingInstallShortcutInfoCreatePendingInfo;
        if (ACTION_INSTALL_SHORTCUT.equals(intent.getAction()) && (pendingInstallShortcutInfoCreatePendingInfo = createPendingInfo(context, intent)) != null) {
            if (!pendingInstallShortcutInfoCreatePendingInfo.isLauncherActivity() && !new PackageManagerHelper(context).hasPermissionForActivity(pendingInstallShortcutInfoCreatePendingInfo.launchIntent, null)) {
                Log.e(TAG, "Ignoring malicious intent " + pendingInstallShortcutInfoCreatePendingInfo.launchIntent.toUri(0));
                return;
            }
            queuePendingShortcutInfo(pendingInstallShortcutInfoCreatePendingInfo, context);
        }
    }

    private static boolean isValidExtraType(Intent intent, String str, Class cls) {
        Parcelable parcelableExtra = intent.getParcelableExtra(str);
        return parcelableExtra == null || cls.isInstance(parcelableExtra);
    }

    private static PendingInstallShortcutInfo createPendingInfo(Context context, Intent intent) {
        if (!isValidExtraType(intent, "android.intent.extra.shortcut.INTENT", Intent.class) || !isValidExtraType(intent, "android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.class) || !isValidExtraType(intent, "android.intent.extra.shortcut.ICON", Bitmap.class)) {
            return null;
        }
        PendingInstallShortcutInfo pendingInstallShortcutInfo = new PendingInstallShortcutInfo(intent, Process.myUserHandle(), context);
        if (pendingInstallShortcutInfo.launchIntent == null || pendingInstallShortcutInfo.label == null) {
            return null;
        }
        return convertToLauncherActivityIfPossible(pendingInstallShortcutInfo);
    }

    public static ShortcutInfo fromShortcutIntent(Context context, Intent intent) {
        PendingInstallShortcutInfo pendingInstallShortcutInfoCreatePendingInfo = createPendingInfo(context, intent);
        if (pendingInstallShortcutInfoCreatePendingInfo == null) {
            return null;
        }
        return (ShortcutInfo) pendingInstallShortcutInfoCreatePendingInfo.getItemInfo().first;
    }

    public static ShortcutInfo fromActivityInfo(LauncherActivityInfo launcherActivityInfo, Context context) {
        return (ShortcutInfo) new PendingInstallShortcutInfo(launcherActivityInfo, context).getItemInfo().first;
    }

    public static void queueShortcut(ShortcutInfoCompat shortcutInfoCompat, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(shortcutInfoCompat, context), context);
    }

    public static void queueWidget(AppWidgetProviderInfo appWidgetProviderInfo, int i, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(appWidgetProviderInfo, i, context), context);
    }

    public static void queueActivityInfo(LauncherActivityInfo launcherActivityInfo, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(launcherActivityInfo, context), context);
    }

    public static HashSet<ShortcutKey> getPendingShortcuts(Context context) {
        HashSet<ShortcutKey> hashSet = new HashSet<>();
        Set<String> stringSet = Utilities.getPrefs(context).getStringSet(APPS_PENDING_INSTALL, null);
        if (Utilities.isEmpty(stringSet)) {
            return hashSet;
        }
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            try {
                Decoder decoder = new Decoder(it.next(), context);
                if (decoder.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                    hashSet.add(ShortcutKey.fromIntent(decoder.launcherIntent, decoder.user));
                }
            } catch (URISyntaxException | JSONException e) {
                Log.d(TAG, "Exception reading shortcut to add: " + e);
            }
        }
        return hashSet;
    }

    private static void queuePendingShortcutInfo(PendingInstallShortcutInfo pendingInstallShortcutInfo, Context context) {
        Message.obtain(sHandler, 1, Pair.create(context, pendingInstallShortcutInfo)).sendToTarget();
        flushInstallQueue(context);
    }

    public static void enableInstallQueue(int i) {
        sInstallQueueDisabledFlags = i | sInstallQueueDisabledFlags;
    }

    public static void disableAndFlushInstallQueue(int i, Context context) {
        sInstallQueueDisabledFlags = (~i) & sInstallQueueDisabledFlags;
        flushInstallQueue(context);
    }

    static void flushInstallQueue(Context context) {
        if (sInstallQueueDisabledFlags != 0) {
            return;
        }
        Message.obtain(sHandler, 2, context.getApplicationContext()).sendToTarget();
    }

    static CharSequence ensureValidName(Context context, Intent intent, CharSequence charSequence) {
        if (charSequence == null) {
            try {
                PackageManager packageManager = context.getPackageManager();
                return packageManager.getActivityInfo(intent.getComponent(), 0).loadLabel(packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                return "";
            }
        }
        return charSequence;
    }

    private static class PendingInstallShortcutInfo {
        final LauncherActivityInfo activityInfo;
        final Intent data;
        final String label;
        final Intent launchIntent;
        final Context mContext;
        final AppWidgetProviderInfo providerInfo;
        final ShortcutInfoCompat shortcutInfo;
        final UserHandle user;

        public PendingInstallShortcutInfo(Intent intent, UserHandle userHandle, Context context) {
            this.activityInfo = null;
            this.shortcutInfo = null;
            this.providerInfo = null;
            this.data = intent;
            this.user = userHandle;
            this.mContext = context;
            this.launchIntent = (Intent) intent.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            this.label = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        }

        public PendingInstallShortcutInfo(LauncherActivityInfo launcherActivityInfo, Context context) {
            this.activityInfo = launcherActivityInfo;
            this.shortcutInfo = null;
            this.providerInfo = null;
            this.data = null;
            this.user = launcherActivityInfo.getUser();
            this.mContext = context;
            this.launchIntent = AppInfo.makeLaunchIntent(launcherActivityInfo);
            this.label = launcherActivityInfo.getLabel().toString();
        }

        public PendingInstallShortcutInfo(ShortcutInfoCompat shortcutInfoCompat, Context context) {
            this.activityInfo = null;
            this.shortcutInfo = shortcutInfoCompat;
            this.providerInfo = null;
            this.data = null;
            this.mContext = context;
            this.user = shortcutInfoCompat.getUserHandle();
            this.launchIntent = shortcutInfoCompat.makeIntent();
            this.label = shortcutInfoCompat.getShortLabel().toString();
        }

        public PendingInstallShortcutInfo(AppWidgetProviderInfo appWidgetProviderInfo, int i, Context context) {
            this.activityInfo = null;
            this.shortcutInfo = null;
            this.providerInfo = appWidgetProviderInfo;
            this.data = null;
            this.mContext = context;
            this.user = appWidgetProviderInfo.getProfile();
            this.launchIntent = new Intent().setComponent(appWidgetProviderInfo.provider).putExtra(LauncherSettings.Favorites.APPWIDGET_ID, i);
            this.label = appWidgetProviderInfo.label;
        }

        public String encodeToString() {
            try {
                if (this.activityInfo != null) {
                    return new JSONStringer().object().key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.launchIntent.toUri(0)).key(InstallShortcutReceiver.APP_SHORTCUT_TYPE_KEY).value(true).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                }
                if (this.shortcutInfo != null) {
                    return new JSONStringer().object().key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.launchIntent.toUri(0)).key(InstallShortcutReceiver.DEEPSHORTCUT_TYPE_KEY).value(true).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                }
                if (this.providerInfo != null) {
                    return new JSONStringer().object().key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.launchIntent.toUri(0)).key(InstallShortcutReceiver.APP_WIDGET_TYPE_KEY).value(true).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                }
                if (this.launchIntent.getAction() == null) {
                    this.launchIntent.setAction("android.intent.action.VIEW");
                } else if (this.launchIntent.getAction().equals("android.intent.action.MAIN") && this.launchIntent.getCategories() != null && this.launchIntent.getCategories().contains("android.intent.category.LAUNCHER")) {
                    this.launchIntent.addFlags(270532608);
                }
                String string = InstallShortcutReceiver.ensureValidName(this.mContext, this.launchIntent, this.label).toString();
                Bitmap bitmap = (Bitmap) this.data.getParcelableExtra("android.intent.extra.shortcut.ICON");
                Intent.ShortcutIconResource shortcutIconResource = (Intent.ShortcutIconResource) this.data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
                JSONStringer jSONStringerValue = new JSONStringer().object().key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.launchIntent.toUri(0)).key(InstallShortcutReceiver.NAME_KEY).value(string);
                if (bitmap != null) {
                    byte[] bArrFlattenBitmap = Utilities.flattenBitmap(bitmap);
                    jSONStringerValue = jSONStringerValue.key("icon").value(Base64.encodeToString(bArrFlattenBitmap, 0, bArrFlattenBitmap.length, 0));
                }
                if (shortcutIconResource != null) {
                    jSONStringerValue = jSONStringerValue.key("iconResource").value(shortcutIconResource.resourceName).key(InstallShortcutReceiver.ICON_RESOURCE_PACKAGE_NAME_KEY).value(shortcutIconResource.packageName);
                }
                return jSONStringerValue.endObject().toString();
            } catch (JSONException e) {
                Log.d(InstallShortcutReceiver.TAG, "Exception when adding shortcut: " + e);
                return null;
            }
        }

        public Pair<ItemInfo, Object> getItemInfo() {
            if (this.activityInfo != null) {
                AppInfo appInfo = new AppInfo(this.mContext, this.activityInfo, this.user);
                final LauncherAppState launcherAppState = LauncherAppState.getInstance(this.mContext);
                appInfo.title = "";
                launcherAppState.getIconCache().getDefaultIcon(this.user).applyTo(appInfo);
                final ShortcutInfo shortcutInfoMakeShortcut = appInfo.makeShortcut();
                if (Looper.myLooper() == LauncherModel.getWorkerLooper()) {
                    launcherAppState.getIconCache().getTitleAndIcon(shortcutInfoMakeShortcut, this.activityInfo, false);
                } else {
                    launcherAppState.getModel().updateAndBindShortcutInfo(new Provider<ShortcutInfo>() {
                        @Override
                        public ShortcutInfo get() {
                            launcherAppState.getIconCache().getTitleAndIcon(shortcutInfoMakeShortcut, PendingInstallShortcutInfo.this.activityInfo, false);
                            return shortcutInfoMakeShortcut;
                        }
                    });
                }
                return Pair.create(shortcutInfoMakeShortcut, this.activityInfo);
            }
            if (this.shortcutInfo != null) {
                ShortcutInfo shortcutInfo = new ShortcutInfo(this.shortcutInfo, this.mContext);
                LauncherIcons launcherIconsObtain = LauncherIcons.obtain(this.mContext);
                launcherIconsObtain.createShortcutIcon(this.shortcutInfo).applyTo(shortcutInfo);
                launcherIconsObtain.recycle();
                return Pair.create(shortcutInfo, this.shortcutInfo);
            }
            if (this.providerInfo != null) {
                LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfoFromProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, this.providerInfo);
                LauncherAppWidgetInfo launcherAppWidgetInfo = new LauncherAppWidgetInfo(this.launchIntent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, 0), launcherAppWidgetProviderInfoFromProviderInfo.provider);
                InvariantDeviceProfile idp = LauncherAppState.getIDP(this.mContext);
                launcherAppWidgetInfo.minSpanX = launcherAppWidgetProviderInfoFromProviderInfo.minSpanX;
                launcherAppWidgetInfo.minSpanY = launcherAppWidgetProviderInfoFromProviderInfo.minSpanY;
                launcherAppWidgetInfo.spanX = Math.min(launcherAppWidgetProviderInfoFromProviderInfo.spanX, idp.numColumns);
                launcherAppWidgetInfo.spanY = Math.min(launcherAppWidgetProviderInfoFromProviderInfo.spanY, idp.numRows);
                return Pair.create(launcherAppWidgetInfo, this.providerInfo);
            }
            return Pair.create(InstallShortcutReceiver.createShortcutInfo(this.data, LauncherAppState.getInstance(this.mContext)), null);
        }

        public boolean isLauncherActivity() {
            return this.activityInfo != null;
        }
    }

    private static String getIntentPackage(Intent intent) {
        return intent.getComponent() == null ? intent.getPackage() : intent.getComponent().getPackageName();
    }

    private static PendingInstallShortcutInfo decode(String str, Context context) {
        try {
            Decoder decoder = new Decoder(str, context);
            if (decoder.optBoolean(APP_SHORTCUT_TYPE_KEY)) {
                LauncherActivityInfo launcherActivityInfoResolveActivity = LauncherAppsCompat.getInstance(context).resolveActivity(decoder.launcherIntent, decoder.user);
                if (launcherActivityInfoResolveActivity == null) {
                    return null;
                }
                return new PendingInstallShortcutInfo(launcherActivityInfoResolveActivity, context);
            }
            if (decoder.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                List<ShortcutInfoCompat> listQueryForFullDetails = DeepShortcutManager.getInstance(context).queryForFullDetails(decoder.launcherIntent.getPackage(), Arrays.asList(decoder.launcherIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID)), decoder.user);
                if (listQueryForFullDetails.isEmpty()) {
                    return null;
                }
                return new PendingInstallShortcutInfo(listQueryForFullDetails.get(0), context);
            }
            if (decoder.optBoolean(APP_WIDGET_TYPE_KEY)) {
                int intExtra = decoder.launcherIntent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, 0);
                AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(intExtra);
                if (appWidgetInfo != null && appWidgetInfo.provider.equals(decoder.launcherIntent.getComponent()) && appWidgetInfo.getProfile().equals(decoder.user)) {
                    return new PendingInstallShortcutInfo(appWidgetInfo, intExtra, context);
                }
                return null;
            }
            Intent intent = new Intent();
            intent.putExtra("android.intent.extra.shortcut.INTENT", decoder.launcherIntent);
            intent.putExtra("android.intent.extra.shortcut.NAME", decoder.getString(NAME_KEY));
            String strOptString = decoder.optString("icon");
            String strOptString2 = decoder.optString("iconResource");
            String strOptString3 = decoder.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
            if (strOptString != null && !strOptString.isEmpty()) {
                byte[] bArrDecode = Base64.decode(strOptString, 0);
                intent.putExtra("android.intent.extra.shortcut.ICON", BitmapFactory.decodeByteArray(bArrDecode, 0, bArrDecode.length));
            } else if (strOptString2 != null && !strOptString2.isEmpty()) {
                Intent.ShortcutIconResource shortcutIconResource = new Intent.ShortcutIconResource();
                shortcutIconResource.resourceName = strOptString2;
                shortcutIconResource.packageName = strOptString3;
                intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", shortcutIconResource);
            }
            return new PendingInstallShortcutInfo(intent, decoder.user, context);
        } catch (URISyntaxException | JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
            return null;
        }
    }

    private static class Decoder extends JSONObject {
        public final Intent launcherIntent;
        public final UserHandle user;

        private Decoder(String str, Context context) throws JSONException, URISyntaxException {
            UserHandle userHandleMyUserHandle;
            super(str);
            this.launcherIntent = Intent.parseUri(getString(InstallShortcutReceiver.LAUNCH_INTENT_KEY), 0);
            if (has(InstallShortcutReceiver.USER_HANDLE_KEY)) {
                userHandleMyUserHandle = UserManagerCompat.getInstance(context).getUserForSerialNumber(getLong(InstallShortcutReceiver.USER_HANDLE_KEY));
            } else {
                userHandleMyUserHandle = Process.myUserHandle();
            }
            this.user = userHandleMyUserHandle;
            if (this.user == null) {
                throw new JSONException("Invalid user");
            }
        }
    }

    private static PendingInstallShortcutInfo convertToLauncherActivityIfPossible(PendingInstallShortcutInfo pendingInstallShortcutInfo) {
        LauncherActivityInfo launcherActivityInfoResolveActivity;
        if (pendingInstallShortcutInfo.isLauncherActivity() || !Utilities.isLauncherAppTarget(pendingInstallShortcutInfo.launchIntent) || (launcherActivityInfoResolveActivity = LauncherAppsCompat.getInstance(pendingInstallShortcutInfo.mContext).resolveActivity(pendingInstallShortcutInfo.launchIntent, pendingInstallShortcutInfo.user)) == null) {
            return pendingInstallShortcutInfo;
        }
        return new PendingInstallShortcutInfo(launcherActivityInfoResolveActivity, pendingInstallShortcutInfo.mContext);
    }

    private static ShortcutInfo createShortcutInfo(Intent intent, LauncherAppState launcherAppState) {
        Intent intent2 = (Intent) intent.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String stringExtra = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        Parcelable parcelableExtra = intent.getParcelableExtra("android.intent.extra.shortcut.ICON");
        BitmapInfo bitmapInfoCreateIconBitmap = null;
        if (intent2 == null) {
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.user = Process.myUserHandle();
        LauncherIcons launcherIconsObtain = LauncherIcons.obtain(launcherAppState.getContext());
        if (parcelableExtra instanceof Bitmap) {
            bitmapInfoCreateIconBitmap = launcherIconsObtain.createIconBitmap((Bitmap) parcelableExtra);
        } else {
            Parcelable parcelableExtra2 = intent.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (parcelableExtra2 instanceof Intent.ShortcutIconResource) {
                shortcutInfo.iconResource = (Intent.ShortcutIconResource) parcelableExtra2;
                bitmapInfoCreateIconBitmap = launcherIconsObtain.createIconBitmap(shortcutInfo.iconResource);
            }
        }
        launcherIconsObtain.recycle();
        if (bitmapInfoCreateIconBitmap == null) {
            bitmapInfoCreateIconBitmap = launcherAppState.getIconCache().getDefaultIcon(shortcutInfo.user);
        }
        bitmapInfoCreateIconBitmap.applyTo(shortcutInfo);
        shortcutInfo.title = Utilities.trim(stringExtra);
        shortcutInfo.contentDescription = UserManagerCompat.getInstance(launcherAppState.getContext()).getBadgedLabelForUser(shortcutInfo.title, shortcutInfo.user);
        shortcutInfo.intent = intent2;
        return shortcutInfo;
    }
}
