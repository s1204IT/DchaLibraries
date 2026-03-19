package com.android.server.search;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.ISearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.DumpState;
import com.android.server.statusbar.StatusBarManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class SearchManagerService extends ISearchManager.Stub {
    private static final String TAG = "SearchManagerService";
    private final Context mContext;
    final Handler mHandler;

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
        public void onUnlockUser(final int i) {
            this.mService.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Lifecycle.this.mService.onUnlockUser(i);
                }
            });
        }

        @Override
        public void onCleanupUser(int i) {
            this.mService.onCleanupUser(i);
        }
    }

    public SearchManagerService(Context context) {
        this.mContext = context;
        new MyPackageMonitor().register(context, null, UserHandle.ALL, true);
        new GlobalSearchProviderObserver(context.getContentResolver());
        this.mHandler = BackgroundThread.getHandler();
    }

    private Searchables getSearchables(int i) {
        return getSearchables(i, false);
    }

    private Searchables getSearchables(int i, boolean z) {
        Searchables searchables;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
            if (userManager.getUserInfo(i) == null) {
                throw new IllegalStateException("User " + i + " doesn't exist");
            }
            if (!userManager.isUserUnlockingOrUnlocked(i)) {
                throw new IllegalStateException("User " + i + " isn't unlocked");
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            synchronized (this.mSearchables) {
                searchables = this.mSearchables.get(i);
                if (searchables == null) {
                    searchables = new Searchables(this.mContext, i);
                    searchables.updateSearchableList();
                    this.mSearchables.append(i, searchables);
                } else if (z) {
                    searchables.updateSearchableList();
                }
            }
            return searchables;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private void onUnlockUser(int i) {
        try {
            getSearchables(i, true);
        } catch (IllegalStateException e) {
        }
    }

    private void onCleanupUser(int i) {
        synchronized (this.mSearchables) {
            this.mSearchables.remove(i);
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        public void onSomePackagesChanged() {
            updateSearchables();
        }

        public void onPackageModified(String str) {
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
                        ((Searchables) SearchManagerService.this.mSearchables.valueAt(i)).updateSearchableList();
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

        public GlobalSearchProviderObserver(ContentResolver contentResolver) {
            super(null);
            this.mResolver = contentResolver;
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("search_global_search_activity"), false, this);
        }

        @Override
        public void onChange(boolean z) {
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

    public SearchableInfo getSearchableInfo(ComponentName componentName) {
        if (componentName == null) {
            Log.e(TAG, "getSearchableInfo(), activity == null");
            return null;
        }
        return getSearchables(UserHandle.getCallingUserId()).getSearchableInfo(componentName);
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

    public void launchAssist(Bundle bundle) {
        StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.startAssist(bundle);
        }
    }

    private ComponentName getLegacyAssistComponent(int i) {
        try {
            List listQueryIntentServicesAsUser = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.voice.VoiceInteractionService"), DumpState.DUMP_DEXOPT, ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, true, false, "getLegacyAssistComponent", null));
            if (listQueryIntentServicesAsUser != null && !listQueryIntentServicesAsUser.isEmpty()) {
                ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(0);
                return new ComponentName(resolveInfo.serviceInfo.applicationInfo.packageName, resolveInfo.serviceInfo.name);
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getLegacyAssistComponent: " + e);
            return null;
        }
    }

    public boolean launchLegacyAssist(String str, int i, Bundle bundle) {
        ComponentName legacyAssistComponent = getLegacyAssistComponent(i);
        if (legacyAssistComponent == null) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.service.voice.VoiceInteractionService");
            intent.setComponent(legacyAssistComponent);
            IActivityManager service = ActivityManager.getService();
            if (bundle != null) {
                bundle.putInt("android.intent.extra.KEY_EVENT", 219);
            }
            intent.putExtras(bundle);
            boolean zLaunchAssistIntent = service.launchAssistIntent(intent, 0, str, i, bundle);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return zLaunchAssistIntent;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            synchronized (this.mSearchables) {
                for (int i = 0; i < this.mSearchables.size(); i++) {
                    indentingPrintWriter.print("\nUser: ");
                    indentingPrintWriter.println(this.mSearchables.keyAt(i));
                    indentingPrintWriter.increaseIndent();
                    this.mSearchables.valueAt(i).dump(fileDescriptor, indentingPrintWriter, strArr);
                    indentingPrintWriter.decreaseIndent();
                }
            }
        }
    }
}
