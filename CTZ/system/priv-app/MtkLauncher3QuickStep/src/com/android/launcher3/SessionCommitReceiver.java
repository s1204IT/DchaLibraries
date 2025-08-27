package com.android.launcher3;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.LauncherAppsCompat;
import java.util.List;

@TargetApi(26)
/* loaded from: classes.dex */
public class SessionCommitReceiver extends BroadcastReceiver {
    public static final String ADD_ICON_PREFERENCE_INITIALIZED_KEY = "pref_add_icon_to_home_initialized";
    public static final String ADD_ICON_PREFERENCE_KEY = "pref_add_icon_to_home";
    private static final String MARKER_PROVIDER_PREFIX = ".addtohomescreen";
    private static final String TAG = "SessionCommitReceiver";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (!isEnabled(context) || !Utilities.ATLEAST_OREO) {
            return;
        }
        PackageInstaller.SessionInfo sessionInfo = (PackageInstaller.SessionInfo) intent.getParcelableExtra("android.content.pm.extra.SESSION");
        UserHandle userHandle = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
        if (TextUtils.isEmpty(sessionInfo.getAppPackageName()) || sessionInfo.getInstallReason() != 4) {
            return;
        }
        queueAppIconAddition(context, sessionInfo.getAppPackageName(), userHandle);
    }

    public static void queueAppIconAddition(Context context, String str, UserHandle userHandle) {
        List<LauncherActivityInfo> activityList = LauncherAppsCompat.getInstance(context).getActivityList(str, userHandle);
        if (activityList == null || activityList.isEmpty()) {
            return;
        }
        InstallShortcutReceiver.queueActivityInfo(activityList.get(0), context);
    }

    public static boolean isEnabled(Context context) {
        return Utilities.getPrefs(context).getBoolean(ADD_ICON_PREFERENCE_KEY, true);
    }

    public static void applyDefaultUserPrefs(Context context) {
        if (!Utilities.ATLEAST_OREO) {
            return;
        }
        SharedPreferences prefs = Utilities.getPrefs(context);
        if (prefs.getAll().isEmpty()) {
            prefs.edit().putBoolean(ADD_ICON_PREFERENCE_KEY, true).apply();
        } else if (!prefs.contains(ADD_ICON_PREFERENCE_INITIALIZED_KEY)) {
            new PrefInitTask(context).executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    private static class PrefInitTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;

        PrefInitTask(Context context) {
            this.mContext = context;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Void doInBackground(Void... voidArr) throws Throwable {
            Utilities.getPrefs(this.mContext).edit().putBoolean(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY, readValueFromMarketApp()).putBoolean(SessionCommitReceiver.ADD_ICON_PREFERENCE_INITIALIZED_KEY, true).apply();
            return null;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [148=6, 149=4] */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:60:0x0072 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:62:0x0074 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:71:0x001e */
        public boolean readValueFromMarketApp() throws Throwable {
            Cursor cursorQuery;
            boolean zMoveToNext;
            ResolveInfo resolveInfoResolveActivity = this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.APP_MARKET"), 1114112);
            if (resolveInfoResolveActivity == null) {
                return true;
            }
            Cursor cursor = null;
            Cursor cursor2 = null;
            try {
                try {
                    cursorQuery = this.mContext.getContentResolver().query(Uri.parse("content://" + resolveInfoResolveActivity.activityInfo.packageName + SessionCommitReceiver.MARKER_PROVIDER_PREFIX), null, null, null, null);
                    try {
                        zMoveToNext = cursorQuery.moveToNext();
                        cursor = zMoveToNext;
                    } catch (Exception e) {
                        cursor2 = cursorQuery;
                        e = e;
                        Log.d(SessionCommitReceiver.TAG, "Error reading add to homescreen preference", e);
                        cursor = cursor2;
                        if (cursor2 != null) {
                            cursor2.close();
                            cursor = cursor2;
                        }
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                } catch (Exception e2) {
                    e = e2;
                }
                if (zMoveToNext) {
                    boolean z = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow(LauncherSettings.Settings.EXTRA_VALUE)) != 0;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return z;
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                    cursor = zMoveToNext;
                }
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }
}
