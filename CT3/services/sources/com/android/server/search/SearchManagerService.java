package com.android.server.search;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.ISearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class SearchManagerService extends ISearchManager.Stub {
    private static final String TAG = "SearchManagerService";
    private final Context mContext;

    @GuardedBy("mSearchables")
    private final SparseArray<Searchables> mSearchables = new SparseArray<>();

    public static class Lifecycle extends SystemService {
        private SearchManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new SearchManagerService(getContext());
            publishBinderService("search", this.mService);
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }

        @Override
        public void onCleanupUser(int userHandle) {
            this.mService.onCleanupUser(userHandle);
        }
    }

    public SearchManagerService(Context context) {
        this.mContext = context;
        new MyPackageMonitor().register(context, null, UserHandle.ALL, true);
        new GlobalSearchProviderObserver(context.getContentResolver());
    }

    private Searchables getSearchables(int userId) {
        return getSearchables(userId, false);
    }

    private Searchables getSearchables(int userId, boolean forceUpdate) {
        Searchables searchables;
        long token = Binder.clearCallingIdentity();
        try {
            UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
            if (um.getUserInfo(userId) == null) {
                throw new IllegalStateException("User " + userId + " doesn't exist");
            }
            if (!um.isUserUnlockingOrUnlocked(userId)) {
                throw new IllegalStateException("User " + userId + " isn't unlocked");
            }
            Binder.restoreCallingIdentity(token);
            synchronized (this.mSearchables) {
                searchables = this.mSearchables.get(userId);
                if (searchables == null) {
                    searchables = new Searchables(this.mContext, userId);
                    searchables.updateSearchableList();
                    this.mSearchables.append(userId, searchables);
                } else if (forceUpdate) {
                    searchables.updateSearchableList();
                }
            }
            return searchables;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private void onUnlockUser(int userId) {
        getSearchables(userId, true);
    }

    private void onCleanupUser(int userId) {
        synchronized (this.mSearchables) {
            this.mSearchables.remove(userId);
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
                    } else if (changingUserId == SearchManagerService.this.mSearchables.keyAt(i)) {
                        break;
                    } else {
                        i++;
                    }
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
                    ((Searchables) SearchManagerService.this.mSearchables.valueAt(i)).updateSearchableList();
                }
            }
            Intent intent = new Intent("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
            intent.addFlags(536870912);
            SearchManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public SearchableInfo getSearchableInfo(ComponentName launchActivity) {
        if (launchActivity == null) {
            Log.e(TAG, "getSearchableInfo(), activity == null");
            return null;
        }
        return getSearchables(UserHandle.getCallingUserId()).getSearchableInfo(launchActivity);
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

    public void launchAssist(Bundle args) {
        StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        if (statusBarManager == null) {
            return;
        }
        statusBarManager.startAssist(args);
    }

    private ComponentName getLegacyAssistComponent(int userHandle) {
        try {
            int userHandle2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userHandle, true, false, "getLegacyAssistComponent", null);
            IPackageManager pm = AppGlobals.getPackageManager();
            Intent assistIntent = new Intent("android.intent.action.ASSIST");
            ResolveInfo info = pm.resolveIntent(assistIntent, assistIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), PackageManagerService.DumpState.DUMP_INSTALLS, userHandle2);
            if (info != null) {
                return new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
            }
        } catch (RemoteException re) {
            Log.e(TAG, "RemoteException in getLegacyAssistComponent: " + re);
        } catch (Exception e) {
            Log.e(TAG, "Exception in getLegacyAssistComponent: " + e);
        }
        return null;
    }

    public boolean launchLegacyAssist(String hint, int userHandle, Bundle args) {
        ComponentName comp = getLegacyAssistComponent(userHandle);
        if (comp == null) {
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.intent.action.ASSIST");
            intent.setComponent(comp);
            IActivityManager am = ActivityManagerNative.getDefault();
            boolean zLaunchAssistIntent = am.launchAssistIntent(intent, 0, hint, userHandle, args);
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
