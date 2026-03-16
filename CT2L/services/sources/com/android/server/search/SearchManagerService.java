package com.android.server.search;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.ISearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class SearchManagerService extends ISearchManager.Stub {
    private static final String TAG = "SearchManagerService";
    private final Context mContext;
    private final SparseArray<Searchables> mSearchables = new SparseArray<>();

    public SearchManagerService(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new BootCompletedReceiver(), filter);
        this.mContext.registerReceiver(new UserReceiver(), new IntentFilter("android.intent.action.USER_REMOVED"));
        new MyPackageMonitor().register(context, null, UserHandle.ALL, true);
    }

    private Searchables getSearchables(int userId) {
        Searchables searchables;
        long origId = Binder.clearCallingIdentity();
        try {
            boolean userExists = ((UserManager) this.mContext.getSystemService("user")).getUserInfo(userId) != null;
            if (!userExists) {
                return null;
            }
            Binder.restoreCallingIdentity(origId);
            synchronized (this.mSearchables) {
                searchables = this.mSearchables.get(userId);
                if (searchables == null) {
                    searchables = new Searchables(this.mContext, userId);
                    searchables.buildSearchableList();
                    this.mSearchables.append(userId, searchables);
                }
            }
            return searchables;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void onUserRemoved(int userId) {
        if (userId != 0) {
            synchronized (this.mSearchables) {
                this.mSearchables.remove(userId);
            }
        }
    }

    private final class BootCompletedReceiver extends BroadcastReceiver {
        private BootCompletedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread() {
                @Override
                public void run() {
                    Process.setThreadPriority(10);
                    SearchManagerService.this.mContext.unregisterReceiver(BootCompletedReceiver.this);
                    SearchManagerService.this.getSearchables(0);
                }
            }.start();
        }
    }

    private final class UserReceiver extends BroadcastReceiver {
        private UserReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SearchManagerService.this.onUserRemoved(intent.getIntExtra("android.intent.extra.user_handle", 0));
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onSomePackagesChanged() {
            updateSearchables();
        }

        public void onPackageModified(String pkg) {
            updateSearchables();
        }

        private void updateSearchables() {
            int changingUserId = getChangingUserId();
            synchronized (SearchManagerService.this.mSearchables) {
                int i = 0;
                while (true) {
                    if (i >= SearchManagerService.this.mSearchables.size()) {
                        break;
                    }
                    if (changingUserId == SearchManagerService.this.mSearchables.keyAt(i)) {
                        SearchManagerService.this.getSearchables(SearchManagerService.this.mSearchables.keyAt(i)).buildSearchableList();
                        break;
                    }
                    i++;
                }
            }
            Intent intent = new Intent("android.search.action.SEARCHABLES_CHANGED");
            intent.addFlags(603979776);
            SearchManagerService.this.mContext.sendBroadcastAsUser(intent, new UserHandle(changingUserId));
        }
    }

    class GlobalSearchProviderObserver extends ContentObserver {
        private final ContentResolver mResolver;

        public GlobalSearchProviderObserver(ContentResolver resolver) {
            super(null);
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("search_global_search_activity"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            synchronized (SearchManagerService.this.mSearchables) {
                for (int i = 0; i < SearchManagerService.this.mSearchables.size(); i++) {
                    SearchManagerService.this.getSearchables(SearchManagerService.this.mSearchables.keyAt(i)).buildSearchableList();
                }
            }
            Intent intent = new Intent("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
            intent.addFlags(536870912);
            SearchManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public SearchableInfo getSearchableInfo(ComponentName launchActivity) {
        if (launchActivity != null) {
            return getSearchables(UserHandle.getCallingUserId()).getSearchableInfo(launchActivity);
        }
        Log.e(TAG, "getSearchableInfo(), activity == null");
        return null;
    }

    public List<SearchableInfo> getSearchablesInGlobalSearch() {
        return getSearchables(UserHandle.getCallingUserId()).getSearchablesInGlobalSearchList();
    }

    public List<ResolveInfo> getGlobalSearchActivities() {
        return getSearchables(UserHandle.getCallingUserId()).getGlobalSearchActivities();
    }

    public ComponentName getGlobalSearchActivity() {
        return getSearchables(UserHandle.getCallingUserId()).getGlobalSearchActivity();
    }

    public ComponentName getWebSearchActivity() {
        return getSearchables(UserHandle.getCallingUserId()).getWebSearchActivity();
    }

    public ComponentName getAssistIntent(int userHandle) {
        try {
            int userHandle2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userHandle, true, false, "getAssistIntent", null);
            IPackageManager pm = AppGlobals.getPackageManager();
            Intent assistIntent = new Intent("android.intent.action.ASSIST");
            ResolveInfo info = pm.resolveIntent(assistIntent, assistIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 65536, userHandle2);
            if (info != null) {
                return new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
            }
        } catch (RemoteException re) {
            Log.e(TAG, "RemoteException in getAssistIntent: " + re);
        } catch (Exception e) {
            Log.e(TAG, "Exception in getAssistIntent: " + e);
        }
        return null;
    }

    public boolean launchAssistAction(int requestType, String hint, int userHandle) {
        ComponentName comp = getAssistIntent(userHandle);
        if (comp == null) {
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.intent.action.ASSIST");
            intent.setComponent(comp);
            IActivityManager am = ActivityManagerNative.getDefault();
            boolean zLaunchAssistIntent = am.launchAssistIntent(intent, requestType, hint, userHandle);
            Binder.restoreCallingIdentity(ident);
            return zLaunchAssistIntent;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(ident);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(pw, "  ");
        synchronized (this.mSearchables) {
            for (int i = 0; i < this.mSearchables.size(); i++) {
                indentingPrintWriter.print("\nUser: ");
                indentingPrintWriter.println(this.mSearchables.keyAt(i));
                indentingPrintWriter.increaseIndent();
                this.mSearchables.valueAt(i).dump(fd, indentingPrintWriter, args);
                indentingPrintWriter.decreaseIndent();
            }
        }
    }
}
