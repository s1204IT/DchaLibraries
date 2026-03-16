package com.android.documentsui;

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
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.model.RootInfo;
import com.android.internal.annotations.GuardedBy;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class RootsCache {
    public static final Uri sNotificationUri = Uri.parse("content://com.android.documentsui.roots/");
    private final Context mContext;
    private final RootInfo mRecentsRoot = new RootInfo();
    private final Object mLock = new Object();
    private final CountDownLatch mFirstLoad = new CountDownLatch(1);

    @GuardedBy("mLock")
    private Multimap<String, RootInfo> mRoots = ArrayListMultimap.create();

    @GuardedBy("mLock")
    private HashSet<String> mStoppedAuthorities = Sets.newHashSet();

    @GuardedBy("mObservedAuthorities")
    private final HashSet<String> mObservedAuthorities = Sets.newHashSet();
    private final ContentObserver mObserver = new RootsChangedObserver();

    public RootsCache(Context context) {
        this.mContext = context;
    }

    private class RootsChangedObserver extends ContentObserver {
        public RootsChangedObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            RootsCache.this.updateAuthorityAsync(uri.getAuthority());
        }
    }

    public void updateAsync() {
        this.mRecentsRoot.authority = null;
        this.mRecentsRoot.rootId = null;
        this.mRecentsRoot.derivedIcon = R.drawable.ic_root_recent;
        this.mRecentsRoot.flags = 19;
        this.mRecentsRoot.title = this.mContext.getString(R.string.root_recent);
        this.mRecentsRoot.availableBytes = -1L;
        new UpdateTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void updatePackageAsync(String packageName) {
        new UpdateTask(packageName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void updateAuthorityAsync(String authority) {
        ProviderInfo info = this.mContext.getPackageManager().resolveContentProvider(authority, 0);
        if (info != null) {
            updatePackageAsync(info.packageName);
        }
    }

    private void waitForFirstLoad() {
        boolean success = false;
        try {
            success = this.mFirstLoad.await(15L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!success) {
            Log.w("Documents", "Timeout waiting for first update");
        }
    }

    private void loadStoppedAuthorities() {
        ContentResolver resolver = this.mContext.getContentResolver();
        synchronized (this.mLock) {
            for (String authority : this.mStoppedAuthorities) {
                this.mRoots.putAll(authority, loadRootsForAuthority(resolver, authority));
            }
            this.mStoppedAuthorities.clear();
        }
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private final String mFilterPackage;
        private final Multimap<String, RootInfo> mTaskRoots;
        private final HashSet<String> mTaskStoppedAuthorities;

        public UpdateTask(RootsCache rootsCache) {
            this(null);
        }

        public UpdateTask(String filterPackage) {
            this.mTaskRoots = ArrayListMultimap.create();
            this.mTaskStoppedAuthorities = Sets.newHashSet();
            this.mFilterPackage = filterPackage;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long start = SystemClock.elapsedRealtime();
            if (this.mFilterPackage != null) {
                RootsCache.this.waitForFirstLoad();
            }
            this.mTaskRoots.put(RootsCache.this.mRecentsRoot.authority, RootsCache.this.mRecentsRoot);
            ContentResolver resolver = RootsCache.this.mContext.getContentResolver();
            PackageManager pm = RootsCache.this.mContext.getPackageManager();
            Intent intent = new Intent("android.content.action.DOCUMENTS_PROVIDER");
            List<ResolveInfo> providers = pm.queryIntentContentProviders(intent, 0);
            for (ResolveInfo info : providers) {
                handleDocumentsProvider(info.providerInfo);
            }
            long delta = SystemClock.elapsedRealtime() - start;
            Log.d("Documents", "Update found " + this.mTaskRoots.size() + " roots in " + delta + "ms");
            synchronized (RootsCache.this.mLock) {
                RootsCache.this.mRoots = this.mTaskRoots;
                RootsCache.this.mStoppedAuthorities = this.mTaskStoppedAuthorities;
            }
            RootsCache.this.mFirstLoad.countDown();
            resolver.notifyChange(RootsCache.sNotificationUri, (ContentObserver) null, false);
            return null;
        }

        private void handleDocumentsProvider(ProviderInfo info) {
            if ((info.applicationInfo.flags & 2097152) != 0) {
                this.mTaskStoppedAuthorities.add(info.authority);
                return;
            }
            boolean cacheHit = false;
            if (this.mFilterPackage != null && !this.mFilterPackage.equals(info.packageName)) {
                synchronized (RootsCache.this.mLock) {
                    if (this.mTaskRoots.putAll(info.authority, RootsCache.this.mRoots.get(info.authority))) {
                        cacheHit = true;
                    }
                }
            }
            if (!cacheHit) {
                this.mTaskRoots.putAll(info.authority, RootsCache.this.loadRootsForAuthority(RootsCache.this.mContext.getContentResolver(), info.authority));
            }
        }
    }

    private Collection<RootInfo> loadRootsForAuthority(ContentResolver resolver, String authority) {
        synchronized (this.mObservedAuthorities) {
            if (this.mObservedAuthorities.add(authority)) {
                Uri rootsUri = DocumentsContract.buildRootsUri(authority);
                this.mContext.getContentResolver().registerContentObserver(rootsUri, true, this.mObserver);
            }
        }
        List<RootInfo> roots = Lists.newArrayList();
        Uri rootsUri2 = DocumentsContract.buildRootsUri(authority);
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, authority);
            cursor = client.query(rootsUri2, null, null, null, null);
            while (cursor.moveToNext()) {
                RootInfo root = RootInfo.fromRootsCursor(authority, cursor);
                roots.add(root);
            }
        } catch (Exception e) {
            Log.w("Documents", "Failed to load some roots from " + authority + ": " + e);
        } finally {
            IoUtils.closeQuietly(cursor);
            ContentProviderClient.releaseQuietly(client);
        }
        return roots;
    }

    public RootInfo getRootOneshot(String authority, String rootId) {
        RootInfo root;
        synchronized (this.mLock) {
            root = getRootLocked(authority, rootId);
            if (root == null) {
                this.mRoots.putAll(authority, loadRootsForAuthority(this.mContext.getContentResolver(), authority));
                root = getRootLocked(authority, rootId);
            }
        }
        return root;
    }

    public RootInfo getRootBlocking(String authority, String rootId) {
        RootInfo rootLocked;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            rootLocked = getRootLocked(authority, rootId);
        }
        return rootLocked;
    }

    private RootInfo getRootLocked(String authority, String rootId) {
        for (RootInfo root : this.mRoots.get(authority)) {
            if (Objects.equals(root.rootId, rootId)) {
                return root;
            }
        }
        return null;
    }

    public boolean isIconUniqueBlocking(RootInfo root) {
        boolean z;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            int rootIcon = root.derivedIcon != 0 ? root.derivedIcon : root.icon;
            Iterator<RootInfo> it = this.mRoots.get(root.authority).iterator();
            while (true) {
                if (it.hasNext()) {
                    RootInfo test = it.next();
                    if (!Objects.equals(test.rootId, root.rootId)) {
                        int testIcon = test.derivedIcon != 0 ? test.derivedIcon : test.icon;
                        if (testIcon == rootIcon) {
                            z = false;
                            break;
                        }
                    }
                } else {
                    z = true;
                    break;
                }
            }
        }
        return z;
    }

    public RootInfo getRecentsRoot() {
        return this.mRecentsRoot;
    }

    public boolean isRecentsRoot(RootInfo root) {
        return this.mRecentsRoot == root;
    }

    public Collection<RootInfo> getMatchingRootsBlocking(DocumentsActivity.State state) {
        List<RootInfo> matchingRoots;
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (this.mLock) {
            matchingRoots = getMatchingRoots(this.mRoots.values(), state);
        }
        return matchingRoots;
    }

    static List<RootInfo> getMatchingRoots(Collection<RootInfo> roots, DocumentsActivity.State state) {
        List<RootInfo> matching = Lists.newArrayList();
        for (RootInfo root : roots) {
            boolean supportsCreate = (root.flags & 1) != 0;
            boolean supportsIsChild = (root.flags & 16) != 0;
            boolean advanced = (root.flags & 131072) != 0;
            boolean localOnly = (root.flags & 2) != 0;
            boolean empty = (root.flags & 65536) != 0;
            if (state.action != 2 || supportsCreate) {
                if (state.action != 4 || supportsIsChild) {
                    if (state.showAdvanced || !advanced) {
                        if (!state.localOnly || localOnly) {
                            if (state.action == 2 || !empty) {
                                boolean overlap = MimePredicate.mimeMatches(root.derivedMimeTypes, state.acceptMimes) || MimePredicate.mimeMatches(state.acceptMimes, root.derivedMimeTypes);
                                if (overlap) {
                                    matching.add(root);
                                }
                            }
                        }
                    }
                }
            }
        }
        return matching;
    }
}
