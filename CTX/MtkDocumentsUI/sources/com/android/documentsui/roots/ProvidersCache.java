package com.android.documentsui.roots;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.R;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.internal.annotations.GuardedBy;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class ProvidersCache implements ProvidersAccess {
    static final boolean $assertionsDisabled = false;
    private static final List<String> PERMIT_EMPTY_CACHE = new ArrayList<String>() {
        {
            add("com.android.mtp.documents");
            add("com.android.documentsui.archives");
        }
    };

    @GuardedBy("mLock")
    private BroadcastReceiver.PendingResult mBootCompletedResult;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mFirstLoadDone;
    private final Object mLock = new Object();
    private final CountDownLatch mFirstLoad = new CountDownLatch(1);

    @GuardedBy("mLock")
    private Multimap<String, RootInfo> mRoots = ArrayListMultimap.create();

    @GuardedBy("mLock")
    private HashSet<String> mStoppedAuthorities = new HashSet<>();

    @GuardedBy("mObservedAuthoritiesDetails")
    private final Map<String, PackageDetails> mObservedAuthoritiesDetails = new HashMap();
    private final ContentObserver mObserver = new RootsChangedObserver();
    private final RootInfo mRecentsRoot = new RootInfo() {
        {
            this.derivedIcon = R.drawable.ic_root_recent;
            this.derivedType = 4;
            this.flags = 18;
            this.title = ProvidersCache.this.mContext.getString(R.string.root_recent);
            this.availableBytes = -1L;
        }
    };

    public ProvidersCache(Context context) {
        this.mContext = context;
    }

    private class RootsChangedObserver extends ContentObserver {
        public RootsChangedObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (uri == null) {
                Log.w("ProvidersCache", "Received onChange event for null uri. Skipping.");
                return;
            }
            if (SharedMinimal.DEBUG) {
                Log.i("ProvidersCache", "Updating roots due to change at " + uri);
            }
            ProvidersCache.this.updateAuthorityAsync(uri.getAuthority());
        }
    }

    public String getApplicationName(String str) {
        return this.mObservedAuthoritiesDetails.get(str).applicationName;
    }

    @Override
    public String getPackageName(String str) {
        return this.mObservedAuthoritiesDetails.get(str).packageName;
    }

    public void updateAsync(boolean z) {
        this.mRecentsRoot.title = this.mContext.getString(R.string.root_recent);
        new UpdateTask(z, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void updatePackageAsync(String str) {
        new UpdateTask(false, str).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void updateAuthorityAsync(String str) {
        ProviderInfo providerInfoResolveContentProvider = this.mContext.getPackageManager().resolveContentProvider(str, 0);
        if (providerInfoResolveContentProvider != null) {
            updatePackageAsync(providerInfoResolveContentProvider.packageName);
        }
    }

    void setBootCompletedResult(BroadcastReceiver.PendingResult pendingResult) {
        synchronized (this.mLock) {
            if (this.mFirstLoadDone) {
                pendingResult.finish();
            } else {
                this.mBootCompletedResult = pendingResult;
            }
        }
    }

    private boolean waitForFirstLoad() {
        boolean zAwait;
        try {
            zAwait = this.mFirstLoad.await(15L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            zAwait = false;
        }
        if (!zAwait) {
            Log.w("ProvidersCache", "Timeout waiting for first update");
        }
        return zAwait;
    }

    private void loadStoppedAuthorities() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        synchronized (this.mLock) {
            for (String str : this.mStoppedAuthorities) {
                this.mRoots.replaceValues(str, loadRootsForAuthority(contentResolver, str, true));
            }
            this.mStoppedAuthorities.clear();
        }
    }

    private Collection<RootInfo> loadRootsForAuthority(ContentResolver contentResolver, String str, boolean z) throws Throwable {
        Cursor cursorQuery;
        Bundle cache;
        if (SharedMinimal.VERBOSE) {
            Log.v("ProvidersCache", "Loading roots for " + str);
        }
        ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
        PackageManager packageManager = this.mContext.getPackageManager();
        ProviderInfo providerInfoResolveContentProvider = packageManager.resolveContentProvider(str, 128);
        if (providerInfoResolveContentProvider == null) {
            Log.w("ProvidersCache", "Failed to get provider info for " + str);
            return arrayList;
        }
        if (!providerInfoResolveContentProvider.exported) {
            Log.w("ProvidersCache", "Provider is not exported. Failed to load roots for " + str);
            return arrayList;
        }
        if (!providerInfoResolveContentProvider.grantUriPermissions) {
            Log.w("ProvidersCache", "Provider doesn't grantUriPermissions. Failed to load roots for " + str);
            return arrayList;
        }
        if (!"android.permission.MANAGE_DOCUMENTS".equals(providerInfoResolveContentProvider.readPermission) || !"android.permission.MANAGE_DOCUMENTS".equals(providerInfoResolveContentProvider.writePermission)) {
            Log.w("ProvidersCache", "Provider is not protected by MANAGE_DOCUMENTS. Failed to load roots for " + str);
            return arrayList;
        }
        synchronized (this.mObservedAuthoritiesDetails) {
            if (!this.mObservedAuthoritiesDetails.containsKey(str)) {
                this.mObservedAuthoritiesDetails.put(str, new PackageDetails(packageManager.getApplicationLabel(providerInfoResolveContentProvider.applicationInfo).toString(), providerInfoResolveContentProvider.applicationInfo.packageName));
                this.mContext.getContentResolver().registerContentObserver(DocumentsContract.buildRootsUri(str), true, this.mObserver);
            }
        }
        Uri uriBuildRootsUri = DocumentsContract.buildRootsUri(str);
        if (!z && (cache = contentResolver.getCache(uriBuildRootsUri)) != null) {
            ArrayList parcelableArrayList = cache.getParcelableArrayList("ProvidersCache");
            if (!parcelableArrayList.isEmpty() || PERMIT_EMPTY_CACHE.contains(str)) {
                if (SharedMinimal.VERBOSE) {
                    Log.v("ProvidersCache", "System cache hit for " + str);
                }
                return parcelableArrayList;
            }
            Log.w("ProvidersCache", "Ignoring empty system cache hit for " + str);
        }
        ContentProviderClient contentProviderClient = null;
        try {
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(contentResolver, str);
            try {
                cursorQuery = contentProviderClientAcquireUnstableProviderOrThrow.query(uriBuildRootsUri, null, null, null, null);
                while (cursorQuery.moveToNext()) {
                    try {
                        arrayList.add(RootInfo.fromRootsCursor(str, cursorQuery));
                    } catch (Exception e) {
                        e = e;
                        contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                        try {
                            Log.w("ProvidersCache", "Failed to load some roots from " + str, e);
                            IoUtils.closeQuietly(cursorQuery);
                            ContentProviderClient.releaseQuietly(contentProviderClient);
                            return arrayList;
                        } catch (Throwable th) {
                            th = th;
                            IoUtils.closeQuietly(cursorQuery);
                            ContentProviderClient.releaseQuietly(contentProviderClient);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                        IoUtils.closeQuietly(cursorQuery);
                        ContentProviderClient.releaseQuietly(contentProviderClient);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(cursorQuery);
                ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                Bundle bundle = new Bundle();
                if (!arrayList.isEmpty() || PERMIT_EMPTY_CACHE.contains(str)) {
                    bundle.putParcelableArrayList("ProvidersCache", arrayList);
                    contentResolver.putCache(uriBuildRootsUri, bundle);
                } else {
                    Log.i("ProvidersCache", "Provider returned no roots. Possibly naughty: " + str);
                }
                return arrayList;
            } catch (Exception e2) {
                e = e2;
                cursorQuery = null;
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        } catch (Exception e3) {
            e = e3;
            cursorQuery = null;
        } catch (Throwable th4) {
            th = th4;
            cursorQuery = null;
        }
    }

    @Override
    public RootInfo getRootOneshot(String str, String str2) {
        return getRootOneshot(str, str2, false);
    }

    public RootInfo getRootOneshot(String str, String str2, boolean z) {
        RootInfo rootLocked;
        synchronized (this.mLock) {
            rootLocked = z ? null : getRootLocked(str, str2);
            if (rootLocked == null) {
                this.mRoots.replaceValues(str, loadRootsForAuthority(this.mContext.getContentResolver(), str, z));
                rootLocked = getRootLocked(str, str2);
            }
        }
        return rootLocked;
    }

    public RootInfo getRootBlocking(String str, String str2) {
        RootInfo rootLocked;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            rootLocked = getRootLocked(str, str2);
        }
        return rootLocked;
    }

    private RootInfo getRootLocked(String str, String str2) {
        for (RootInfo rootInfo : this.mRoots.get(str)) {
            if (Objects.equals(rootInfo.rootId, str2)) {
                return rootInfo;
            }
        }
        return null;
    }

    @Override
    public RootInfo getRecentsRoot() {
        return this.mRecentsRoot;
    }

    public boolean isRecentsRoot(RootInfo rootInfo) {
        return this.mRecentsRoot.equals(rootInfo);
    }

    @Override
    public Collection<RootInfo> getRootsBlocking() {
        Collection<RootInfo> collectionValues;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            collectionValues = this.mRoots.values();
        }
        return collectionValues;
    }

    @Override
    public Collection<RootInfo> getMatchingRootsBlocking(State state) {
        List<RootInfo> matchingRoots;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            matchingRoots = ProvidersAccess.getMatchingRoots(this.mRoots.values(), state);
        }
        return matchingRoots;
    }

    @Override
    public RootInfo getDefaultRootBlocking(State state) {
        for (RootInfo rootInfo : ProvidersAccess.getMatchingRoots(getRootsBlocking(), state)) {
            if (rootInfo.isDownloads()) {
                return rootInfo;
            }
        }
        return this.mRecentsRoot;
    }

    public void logCache() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        StringBuilder sb = new StringBuilder();
        for (String str : this.mObservedAuthoritiesDetails.keySet()) {
            ArrayList arrayList = new ArrayList();
            Bundle cache = contentResolver.getCache(DocumentsContract.buildRootsUri(str));
            if (cache != null) {
                Iterator it = cache.getParcelableArrayList("ProvidersCache").iterator();
                while (it.hasNext()) {
                    arrayList.add(((RootInfo) it.next()).toDebugString());
                }
            }
            sb.append(sb.length() == 0 ? "System cache: " : ", ");
            sb.append(str);
            sb.append("=");
            sb.append(arrayList);
        }
        Log.i("ProvidersCache", sb.toString());
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private final boolean mForceRefreshAll;
        private final String mForceRefreshPackage;
        private final Multimap<String, RootInfo> mTaskRoots = ArrayListMultimap.create();
        private final HashSet<String> mTaskStoppedAuthorities = new HashSet<>();

        public UpdateTask(boolean z, String str) {
            this.mForceRefreshAll = z;
            this.mForceRefreshPackage = str;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            this.mTaskRoots.put(ProvidersCache.this.mRecentsRoot.authority, ProvidersCache.this.mRecentsRoot);
            Iterator<ResolveInfo> it = ProvidersCache.this.mContext.getPackageManager().queryIntentContentProviders(new Intent("android.content.action.DOCUMENTS_PROVIDER"), 0).iterator();
            while (it.hasNext()) {
                ProviderInfo providerInfo = it.next().providerInfo;
                if (providerInfo.authority != null) {
                    handleDocumentsProvider(providerInfo);
                }
            }
            long jElapsedRealtime2 = SystemClock.elapsedRealtime() - jElapsedRealtime;
            if (SharedMinimal.VERBOSE) {
                Log.v("ProvidersCache", "Update found " + this.mTaskRoots.size() + " roots in " + jElapsedRealtime2 + "ms");
            }
            synchronized (ProvidersCache.this.mLock) {
                ProvidersCache.this.mFirstLoadDone = true;
                if (ProvidersCache.this.mBootCompletedResult != null) {
                    ProvidersCache.this.mBootCompletedResult.finish();
                    ProvidersCache.this.mBootCompletedResult = null;
                }
                ProvidersCache.this.mRoots = this.mTaskRoots;
                ProvidersCache.this.mStoppedAuthorities = this.mTaskStoppedAuthorities;
            }
            ProvidersCache.this.mFirstLoad.countDown();
            LocalBroadcastManager.getInstance(ProvidersCache.this.mContext).sendBroadcast(new Intent("com.android.documentsui.action.ROOT_CHANGED"));
            return null;
        }

        private void handleDocumentsProvider(ProviderInfo providerInfo) {
            if ((providerInfo.applicationInfo.flags & 2097152) == 0) {
                this.mTaskRoots.putAll(providerInfo.authority, ProvidersCache.this.loadRootsForAuthority(ProvidersCache.this.mContext.getContentResolver(), providerInfo.authority, this.mForceRefreshAll || Objects.equals(providerInfo.packageName, this.mForceRefreshPackage)));
                return;
            }
            if (SharedMinimal.VERBOSE) {
                Log.v("ProvidersCache", "Ignoring stopped authority " + providerInfo.authority);
            }
            this.mTaskStoppedAuthorities.add(providerInfo.authority);
        }
    }

    private static class PackageDetails {
        private String applicationName;
        private String packageName;

        public PackageDetails(String str, String str2) {
            this.applicationName = str;
            this.packageName = str2;
        }
    }
}
