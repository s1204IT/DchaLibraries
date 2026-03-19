package com.android.server.slice;

import android.app.AppOpsManager;
import android.app.slice.ISliceManager;
import android.app.slice.SliceSpec;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.Settings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class SliceManagerService extends ISliceManager.Stub {
    private static final String TAG = "SliceManagerService";
    private final AppOpsManager mAppOps;
    private final UsageStatsManagerInternal mAppUsageStats;
    private final AssistUtils mAssistUtils;

    @GuardedBy("mLock")
    private final SparseArray<PackageMatchingCache> mAssistantLookup;
    private final Context mContext;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private final SparseArray<PackageMatchingCache> mHomeLookup;
    private final Object mLock;
    private final PackageManagerInternal mPackageManagerInternal;
    private final SlicePermissionManager mPermissions;

    @GuardedBy("mLock")
    private final ArrayMap<Uri, PinnedSliceState> mPinnedSlicesByUri;
    private final BroadcastReceiver mReceiver;

    public SliceManagerService(Context context) {
        this(context, createHandler().getLooper());
    }

    @VisibleForTesting
    SliceManagerService(Context context, Looper looper) {
        this.mLock = new Object();
        this.mPinnedSlicesByUri = new ArrayMap<>();
        this.mAssistantLookup = new SparseArray<>();
        this.mHomeLookup = new SparseArray<>();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (intExtra == -10000) {
                    Slog.w(SliceManagerService.TAG, "Intent broadcast does not contain user handle: " + intent);
                }
                Uri data = intent.getData();
                String schemeSpecificPart = data != null ? data.getSchemeSpecificPart() : null;
                if (schemeSpecificPart == null) {
                    Slog.w(SliceManagerService.TAG, "Intent broadcast does not contain package name: " + intent);
                    return;
                }
                String action = intent.getAction();
                byte b = -1;
                int iHashCode = action.hashCode();
                if (iHashCode != 267468725) {
                    if (iHashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        b = 0;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            SliceManagerService.this.mPermissions.removePkg(schemeSpecificPart, intExtra);
                        }
                        break;
                    case 1:
                        SliceManagerService.this.mPermissions.removePkg(schemeSpecificPart, intExtra);
                        break;
                }
            }
        };
        this.mContext = context;
        this.mPackageManagerInternal = (PackageManagerInternal) Preconditions.checkNotNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAssistUtils = new AssistUtils(context);
        this.mHandler = new Handler(looper);
        this.mAppUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mPermissions = new SlicePermissionManager(this.mContext, looper);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
    }

    private void systemReady() {
    }

    private void onUnlockUser(int i) {
    }

    private void onStopUser(final int i) {
        synchronized (this.mLock) {
            this.mPinnedSlicesByUri.values().removeIf(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return SliceManagerService.lambda$onStopUser$0(i, (PinnedSliceState) obj);
                }
            });
        }
    }

    static boolean lambda$onStopUser$0(int i, PinnedSliceState pinnedSliceState) {
        return ContentProvider.getUserIdFromUri(pinnedSliceState.getUri()) == i;
    }

    public Uri[] getPinnedSlices(String str) {
        verifyCaller(str);
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        ArrayList arrayList = new ArrayList();
        synchronized (this.mLock) {
            for (PinnedSliceState pinnedSliceState : this.mPinnedSlicesByUri.values()) {
                if (Objects.equals(str, pinnedSliceState.getPkg())) {
                    Uri uri = pinnedSliceState.getUri();
                    if (ContentProvider.getUserIdFromUri(uri, identifier) == identifier) {
                        arrayList.add(ContentProvider.getUriWithoutUserId(uri));
                    }
                }
            }
        }
        return (Uri[]) arrayList.toArray(new Uri[arrayList.size()]);
    }

    public void pinSlice(final String str, Uri uri, SliceSpec[] sliceSpecArr, IBinder iBinder) throws RemoteException {
        verifyCaller(str);
        enforceAccess(str, uri);
        final int identifier = Binder.getCallingUserHandle().getIdentifier();
        Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(uri, identifier);
        final String providerPkg = getProviderPkg(uriMaybeAddUserId, identifier);
        getOrCreatePinnedSlice(uriMaybeAddUserId, providerPkg).pin(str, sliceSpecArr, iBinder);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                SliceManagerService.lambda$pinSlice$1(this.f$0, providerPkg, str, identifier);
            }
        });
    }

    public static void lambda$pinSlice$1(SliceManagerService sliceManagerService, String str, String str2, int i) {
        if (str != null && !Objects.equals(str2, str)) {
            sliceManagerService.mAppUsageStats.reportEvent(str, i, (sliceManagerService.isAssistant(str2, i) || sliceManagerService.isDefaultHomeApp(str2, i)) ? 13 : 14);
        }
    }

    public void unpinSlice(String str, Uri uri, IBinder iBinder) throws RemoteException {
        verifyCaller(str);
        enforceAccess(str, uri);
        Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(uri, Binder.getCallingUserHandle().getIdentifier());
        if (getPinnedSlice(uriMaybeAddUserId).unpin(str, iBinder)) {
            removePinnedSlice(uriMaybeAddUserId);
        }
    }

    public boolean hasSliceAccess(String str) throws RemoteException {
        verifyCaller(str);
        return hasFullSliceAccess(str, Binder.getCallingUserHandle().getIdentifier());
    }

    public SliceSpec[] getPinnedSpecs(Uri uri, String str) throws RemoteException {
        verifyCaller(str);
        enforceAccess(str, uri);
        return getPinnedSlice(uri).getSpecs();
    }

    public void grantSlicePermission(String str, String str2, Uri uri) throws RemoteException {
        verifyCaller(str);
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        enforceOwner(str, uri, identifier);
        this.mPermissions.grantSliceAccess(str2, identifier, str, identifier, uri);
    }

    public void revokeSlicePermission(String str, String str2, Uri uri) throws RemoteException {
        verifyCaller(str);
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        enforceOwner(str, uri, identifier);
        this.mPermissions.revokeSliceAccess(str2, identifier, str, identifier, uri);
    }

    public int checkSlicePermission(Uri uri, String str, int i, int i2, String[] strArr) {
        int userId = UserHandle.getUserId(i2);
        if (str == null) {
            for (String str2 : this.mContext.getPackageManager().getPackagesForUid(i2)) {
                if (checkSlicePermission(uri, str2, i, i2, strArr) == 0) {
                    return 0;
                }
            }
            return -1;
        }
        if (hasFullSliceAccess(str, userId) || this.mPermissions.hasPermission(str, userId, uri)) {
            return 0;
        }
        if (strArr != null) {
            enforceOwner(str, uri, userId);
            for (String str3 : strArr) {
                if (this.mContext.checkPermission(str3, i, i2) == 0) {
                    int userIdFromUri = ContentProvider.getUserIdFromUri(uri, userId);
                    this.mPermissions.grantSliceAccess(str, userId, getProviderPkg(uri, userIdFromUri), userIdFromUri, uri);
                    return 0;
                }
            }
        }
        return this.mContext.checkUriPermission(uri, i, i2, 2) == 0 ? 0 : -1;
    }

    public void grantPermissionFromUser(Uri uri, String str, String str2, boolean z) {
        verifyCaller(str2);
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_SLICE_PERMISSIONS", "Slice granting requires MANAGE_SLICE_PERMISSIONS");
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        if (z) {
            this.mPermissions.grantFullAccess(str, identifier);
        } else {
            Uri uriBuild = uri.buildUpon().path(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).build();
            int userIdFromUri = ContentProvider.getUserIdFromUri(uriBuild, identifier);
            this.mPermissions.grantSliceAccess(str, identifier, getProviderPkg(uriBuild, userIdFromUri), userIdFromUri, uriBuild);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.getContentResolver().notifyChange(uri, null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public byte[] getBackupPayload(int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Caller must be system");
        }
        if (i != 0) {
            Slog.w(TAG, "getBackupPayload: cannot backup policy for user " + i);
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            XmlSerializer xmlSerializerNewSerializer = XmlPullParserFactory.newInstance().newSerializer();
            xmlSerializerNewSerializer.setOutput(byteArrayOutputStream, Xml.Encoding.UTF_8.name());
            this.mPermissions.writeBackup(xmlSerializerNewSerializer);
            xmlSerializerNewSerializer.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException | XmlPullParserException e) {
            Slog.w(TAG, "getBackupPayload: error writing payload for user " + i, e);
            return null;
        }
    }

    public void applyRestore(byte[] bArr, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Caller must be system");
        }
        if (bArr == null) {
            Slog.w(TAG, "applyRestore: no payload to restore for user " + i);
            return;
        }
        if (i != 0) {
            Slog.w(TAG, "applyRestore: cannot restore policy for user " + i);
            return;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        try {
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParserNewPullParser.setInput(byteArrayInputStream, Xml.Encoding.UTF_8.name());
            this.mPermissions.readRestore(xmlPullParserNewPullParser);
        } catch (IOException | NumberFormatException | XmlPullParserException e) {
            Slog.w(TAG, "applyRestore: error reading payload", e);
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new SliceShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    private void enforceOwner(String str, Uri uri, int i) {
        if (!Objects.equals(getProviderPkg(uri, i), str) || str == null) {
            throw new SecurityException("Caller must own " + uri);
        }
    }

    protected void removePinnedSlice(Uri uri) {
        synchronized (this.mLock) {
            this.mPinnedSlicesByUri.remove(uri).destroy();
        }
    }

    private PinnedSliceState getPinnedSlice(Uri uri) {
        PinnedSliceState pinnedSliceState;
        synchronized (this.mLock) {
            pinnedSliceState = this.mPinnedSlicesByUri.get(uri);
            if (pinnedSliceState == null) {
                throw new IllegalStateException(String.format("Slice %s not pinned", uri.toString()));
            }
        }
        return pinnedSliceState;
    }

    private PinnedSliceState getOrCreatePinnedSlice(Uri uri, String str) {
        PinnedSliceState pinnedSliceStateCreatePinnedSlice;
        synchronized (this.mLock) {
            pinnedSliceStateCreatePinnedSlice = this.mPinnedSlicesByUri.get(uri);
            if (pinnedSliceStateCreatePinnedSlice == null) {
                pinnedSliceStateCreatePinnedSlice = createPinnedSlice(uri, str);
                this.mPinnedSlicesByUri.put(uri, pinnedSliceStateCreatePinnedSlice);
            }
        }
        return pinnedSliceStateCreatePinnedSlice;
    }

    @VisibleForTesting
    protected PinnedSliceState createPinnedSlice(Uri uri, String str) {
        return new PinnedSliceState(this, uri, str);
    }

    public Object getLock() {
        return this.mLock;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    protected int checkAccess(String str, Uri uri, int i, int i2) {
        return checkSlicePermission(uri, str, i2, i, null);
    }

    private String getProviderPkg(Uri uri, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mContext.getPackageManager().resolveContentProviderAsUser(ContentProvider.getUriWithoutUserId(uri).getAuthority(), 0, ContentProvider.getUserIdFromUri(uri, i)).packageName;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void enforceCrossUser(String str, Uri uri) {
        int identifier = Binder.getCallingUserHandle().getIdentifier();
        if (ContentProvider.getUserIdFromUri(uri, identifier) != identifier) {
            getContext().enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Slice interaction across users requires INTERACT_ACROSS_USERS_FULL");
        }
    }

    private void enforceAccess(String str, Uri uri) throws RemoteException {
        if (checkAccess(str, uri, Binder.getCallingUid(), Binder.getCallingPid()) != 0 && !Objects.equals(str, getProviderPkg(uri, ContentProvider.getUserIdFromUri(uri, Binder.getCallingUserHandle().getIdentifier())))) {
            throw new SecurityException("Access to slice " + uri + " is required");
        }
        enforceCrossUser(str, uri);
    }

    private void verifyCaller(String str) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
    }

    private boolean hasFullSliceAccess(String str, int i) {
        boolean z;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (isDefaultHomeApp(str, i) || isAssistant(str, i)) {
                z = true;
            } else if (!isGrantedFullAccess(str, i)) {
                z = false;
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isAssistant(String str, int i) {
        return getAssistantMatcher(i).matches(str);
    }

    private boolean isDefaultHomeApp(String str, int i) {
        return getHomeMatcher(i).matches(str);
    }

    private PackageMatchingCache getAssistantMatcher(final int i) {
        PackageMatchingCache packageMatchingCache = this.mAssistantLookup.get(i);
        if (packageMatchingCache == null) {
            PackageMatchingCache packageMatchingCache2 = new PackageMatchingCache(new Supplier() {
                @Override
                public final Object get() {
                    return this.f$0.getAssistant(i);
                }
            });
            this.mAssistantLookup.put(i, packageMatchingCache2);
            return packageMatchingCache2;
        }
        return packageMatchingCache;
    }

    private PackageMatchingCache getHomeMatcher(final int i) {
        PackageMatchingCache packageMatchingCache = this.mHomeLookup.get(i);
        if (packageMatchingCache == null) {
            PackageMatchingCache packageMatchingCache2 = new PackageMatchingCache(new Supplier() {
                @Override
                public final Object get() {
                    return this.f$0.getDefaultHome(i);
                }
            });
            this.mHomeLookup.put(i, packageMatchingCache2);
            return packageMatchingCache2;
        }
        return packageMatchingCache;
    }

    private String getAssistant(int i) {
        ComponentName assistComponentForUser = this.mAssistUtils.getAssistComponentForUser(i);
        if (assistComponentForUser == null) {
            return null;
        }
        return assistComponentForUser.getPackageName();
    }

    @VisibleForTesting
    protected String getDefaultHome(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ArrayList arrayList = new ArrayList();
            ComponentName homeActivitiesAsUser = this.mPackageManagerInternal.getHomeActivitiesAsUser(arrayList, i);
            if (homeActivitiesAsUser == null) {
                homeActivitiesAsUser = null;
            }
            if (homeActivitiesAsUser == null) {
                int size = arrayList.size();
                int i2 = Integer.MIN_VALUE;
                for (int i3 = 0; i3 < size; i3++) {
                    ResolveInfo resolveInfo = (ResolveInfo) arrayList.get(i3);
                    if (resolveInfo.activityInfo.applicationInfo.isSystemApp() && resolveInfo.priority >= i2) {
                        homeActivitiesAsUser = resolveInfo.activityInfo.getComponentName();
                        i2 = resolveInfo.priority;
                    }
                }
            }
            return homeActivitiesAsUser != null ? homeActivitiesAsUser.getPackageName() : null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isGrantedFullAccess(String str, int i) {
        return this.mPermissions.hasFullAccess(str, i);
    }

    private static ServiceThread createHandler() {
        ServiceThread serviceThread = new ServiceThread(TAG, 10, true);
        serviceThread.start();
        return serviceThread;
    }

    public String[] getAllPackagesGranted(String str) {
        return this.mPermissions.getAllPackagesGranted(getProviderPkg(new Uri.Builder().scheme("content").authority(str).build(), 0));
    }

    static class PackageMatchingCache {
        private String mCurrentPkg;
        private final Supplier<String> mPkgSource;

        public PackageMatchingCache(Supplier<String> supplier) {
            this.mPkgSource = supplier;
        }

        public boolean matches(String str) {
            if (str == null) {
                return false;
            }
            if (Objects.equals(str, this.mCurrentPkg)) {
                return true;
            }
            this.mCurrentPkg = this.mPkgSource.get();
            return Objects.equals(str, this.mCurrentPkg);
        }
    }

    public static class Lifecycle extends SystemService {
        private SliceManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new SliceManagerService(getContext());
            publishBinderService("slice", this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mService.systemReady();
            }
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.onUnlockUser(i);
        }

        @Override
        public void onStopUser(int i) {
            this.mService.onStopUser(i);
        }
    }

    private class SliceGrant {
        private final String mPkg;
        private final Uri mUri;
        private final int mUserId;

        public SliceGrant(Uri uri, String str, int i) {
            this.mUri = uri;
            this.mPkg = str;
            this.mUserId = i;
        }

        public int hashCode() {
            return this.mUri.hashCode() + this.mPkg.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SliceGrant)) {
                return false;
            }
            SliceGrant sliceGrant = (SliceGrant) obj;
            return Objects.equals(sliceGrant.mUri, this.mUri) && Objects.equals(sliceGrant.mPkg, this.mPkg) && sliceGrant.mUserId == this.mUserId;
        }
    }
}
