package android.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.widget.IRemoteViewsAdapterConnection;
import com.android.internal.widget.IRemoteViewsFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class RemoteViewsAdapter extends BaseAdapter implements Handler.Callback {
    private static final String MULTI_USER_PERM = "android.permission.INTERACT_ACROSS_USERS_FULL";
    private static final int REMOTE_VIEWS_CACHE_DURATION = 5000;
    private static final String TAG = "RemoteViewsAdapter";
    private static Handler sCacheRemovalQueue = null;
    private static HandlerThread sCacheRemovalThread = null;
    private static final int sDefaultCacheSize = 40;
    private static final int sDefaultLoadingViewHeight = 50;
    private static final int sDefaultMessageType = 0;
    private static final int sUnbindServiceDelay = 5000;
    private static final int sUnbindServiceMessageType = 1;
    private final int mAppWidgetId;
    private FixedSizeRemoteViewsCache mCache;
    private WeakReference<RemoteAdapterConnectionCallback> mCallback;
    private final Context mContext;
    private boolean mDataReady;
    private final Intent mIntent;
    private LayoutInflater mLayoutInflater;
    private Handler mMainQueue;
    private boolean mNotifyDataSetChangedAfterOnServiceConnected = false;
    private RemoteViews.OnClickHandler mRemoteViewsOnClickHandler;
    private RemoteViewsFrameLayoutRefSet mRequestedViews;
    private RemoteViewsAdapterServiceConnection mServiceConnection;
    private int mVisibleWindowLowerBound;
    private int mVisibleWindowUpperBound;
    private Handler mWorkerQueue;
    private HandlerThread mWorkerThread;
    private static final HashMap<RemoteViewsCacheKey, FixedSizeRemoteViewsCache> sCachedRemoteViewsCaches = new HashMap<>();
    private static final HashMap<RemoteViewsCacheKey, Runnable> sRemoteViewsCacheRemoveRunnables = new HashMap<>();

    public interface RemoteAdapterConnectionCallback {
        void deferNotifyDataSetChanged();

        boolean onRemoteAdapterConnected();

        void onRemoteAdapterDisconnected();
    }

    private static class RemoteViewsAdapterServiceConnection extends IRemoteViewsAdapterConnection.Stub {
        private WeakReference<RemoteViewsAdapter> mAdapter;
        private boolean mIsConnected;
        private boolean mIsConnecting;
        private IRemoteViewsFactory mRemoteViewsFactory;

        public RemoteViewsAdapterServiceConnection(RemoteViewsAdapter adapter) {
            this.mAdapter = new WeakReference<>(adapter);
        }

        public synchronized void bind(Context context, int appWidgetId, Intent intent) {
            if (!this.mIsConnecting) {
                try {
                    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    RemoteViewsAdapter adapter = this.mAdapter.get();
                    if (adapter != null) {
                        mgr.bindRemoteViewsService(context.getOpPackageName(), appWidgetId, intent, asBinder());
                    } else {
                        Slog.w(RemoteViewsAdapter.TAG, "bind: adapter was null");
                    }
                    this.mIsConnecting = true;
                } catch (Exception e) {
                    Log.e("RemoteViewsAdapterServiceConnection", "bind(): " + e.getMessage());
                    this.mIsConnecting = false;
                    this.mIsConnected = false;
                }
            }
        }

        public synchronized void unbind(Context context, int appWidgetId, Intent intent) {
            try {
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                RemoteViewsAdapter adapter = this.mAdapter.get();
                if (adapter != null) {
                    mgr.unbindRemoteViewsService(context.getOpPackageName(), appWidgetId, intent);
                } else {
                    Slog.w(RemoteViewsAdapter.TAG, "unbind: adapter was null");
                }
                this.mIsConnecting = false;
            } catch (Exception e) {
                Log.e("RemoteViewsAdapterServiceConnection", "unbind(): " + e.getMessage());
                this.mIsConnecting = false;
                this.mIsConnected = false;
            }
        }

        @Override
        public synchronized void onServiceConnected(IBinder service) {
            this.mRemoteViewsFactory = IRemoteViewsFactory.Stub.asInterface(service);
            final RemoteViewsAdapter adapter = this.mAdapter.get();
            if (adapter != null) {
                adapter.mWorkerQueue.post(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.mNotifyDataSetChangedAfterOnServiceConnected) {
                            adapter.onNotifyDataSetChanged();
                        } else {
                            IRemoteViewsFactory factory = adapter.mServiceConnection.getRemoteViewsFactory();
                            try {
                                if (!factory.isCreated()) {
                                    factory.onDataSetChanged();
                                }
                            } catch (RemoteException e) {
                                Log.e(RemoteViewsAdapter.TAG, "Error notifying factory of data set changed in onServiceConnected(): " + e.getMessage());
                                return;
                            } catch (RuntimeException e2) {
                                Log.e(RemoteViewsAdapter.TAG, "Error notifying factory of data set changed in onServiceConnected(): " + e2.getMessage());
                            }
                            adapter.updateTemporaryMetaData();
                            adapter.mMainQueue.post(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (adapter.mCache) {
                                        adapter.mCache.commitTemporaryMetaData();
                                    }
                                    RemoteAdapterConnectionCallback callback = (RemoteAdapterConnectionCallback) adapter.mCallback.get();
                                    if (callback != null) {
                                        callback.onRemoteAdapterConnected();
                                    }
                                }
                            });
                        }
                        adapter.enqueueDeferredUnbindServiceMessage();
                        RemoteViewsAdapterServiceConnection.this.mIsConnected = true;
                        RemoteViewsAdapterServiceConnection.this.mIsConnecting = false;
                    }
                });
            }
        }

        @Override
        public synchronized void onServiceDisconnected() {
            this.mIsConnected = false;
            this.mIsConnecting = false;
            this.mRemoteViewsFactory = null;
            final RemoteViewsAdapter adapter = this.mAdapter.get();
            if (adapter != null) {
                adapter.mMainQueue.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.mMainQueue.removeMessages(1);
                        RemoteAdapterConnectionCallback callback = (RemoteAdapterConnectionCallback) adapter.mCallback.get();
                        if (callback != null) {
                            callback.onRemoteAdapterDisconnected();
                        }
                    }
                });
            }
        }

        public synchronized IRemoteViewsFactory getRemoteViewsFactory() {
            return this.mRemoteViewsFactory;
        }

        public synchronized boolean isConnected() {
            return this.mIsConnected;
        }
    }

    private static class RemoteViewsFrameLayout extends FrameLayout {
        public RemoteViewsFrameLayout(Context context) {
            super(context);
        }

        public void onRemoteViewsLoaded(RemoteViews view, RemoteViews.OnClickHandler handler) {
            try {
                removeAllViews();
                addView(view.apply(getContext(), this, handler));
            } catch (Exception e) {
                Log.e(RemoteViewsAdapter.TAG, "Failed to apply RemoteViews.");
            }
        }
    }

    private class RemoteViewsFrameLayoutRefSet {
        private HashMap<Integer, LinkedList<RemoteViewsFrameLayout>> mReferences = new HashMap<>();
        private HashMap<RemoteViewsFrameLayout, LinkedList<RemoteViewsFrameLayout>> mViewToLinkedList = new HashMap<>();

        public RemoteViewsFrameLayoutRefSet() {
        }

        public void add(int position, RemoteViewsFrameLayout layout) {
            LinkedList<RemoteViewsFrameLayout> refs;
            Integer pos = Integer.valueOf(position);
            if (this.mReferences.containsKey(pos)) {
                refs = this.mReferences.get(pos);
            } else {
                refs = new LinkedList<>();
                this.mReferences.put(pos, refs);
            }
            this.mViewToLinkedList.put(layout, refs);
            refs.add(layout);
        }

        public void notifyOnRemoteViewsLoaded(int position, RemoteViews view) {
            if (view != null) {
                Integer pos = Integer.valueOf(position);
                if (this.mReferences.containsKey(pos)) {
                    LinkedList<RemoteViewsFrameLayout> refs = this.mReferences.get(pos);
                    for (RemoteViewsFrameLayout ref : refs) {
                        ref.onRemoteViewsLoaded(view, RemoteViewsAdapter.this.mRemoteViewsOnClickHandler);
                        if (this.mViewToLinkedList.containsKey(ref)) {
                            this.mViewToLinkedList.remove(ref);
                        }
                    }
                    refs.clear();
                    this.mReferences.remove(pos);
                }
            }
        }

        public void removeView(RemoteViewsFrameLayout rvfl) {
            if (this.mViewToLinkedList.containsKey(rvfl)) {
                this.mViewToLinkedList.get(rvfl).remove(rvfl);
                this.mViewToLinkedList.remove(rvfl);
            }
        }

        public void clear() {
            this.mReferences.clear();
            this.mViewToLinkedList.clear();
        }
    }

    private static class RemoteViewsMetaData {
        int count;
        boolean hasStableIds;
        RemoteViews mFirstView;
        int mFirstViewHeight;
        private final HashMap<Integer, Integer> mTypeIdIndexMap = new HashMap<>();
        RemoteViews mUserLoadingView;
        int viewTypeCount;

        public RemoteViewsMetaData() {
            reset();
        }

        public void set(RemoteViewsMetaData d) {
            synchronized (d) {
                this.count = d.count;
                this.viewTypeCount = d.viewTypeCount;
                this.hasStableIds = d.hasStableIds;
                setLoadingViewTemplates(d.mUserLoadingView, d.mFirstView);
            }
        }

        public void reset() {
            this.count = 0;
            this.viewTypeCount = 1;
            this.hasStableIds = true;
            this.mUserLoadingView = null;
            this.mFirstView = null;
            this.mFirstViewHeight = 0;
            this.mTypeIdIndexMap.clear();
        }

        public void setLoadingViewTemplates(RemoteViews loadingView, RemoteViews firstView) {
            this.mUserLoadingView = loadingView;
            if (firstView != null) {
                this.mFirstView = firstView;
                this.mFirstViewHeight = -1;
            }
        }

        public int getMappedViewType(int typeId) {
            if (this.mTypeIdIndexMap.containsKey(Integer.valueOf(typeId))) {
                return this.mTypeIdIndexMap.get(Integer.valueOf(typeId)).intValue();
            }
            int incrementalTypeId = this.mTypeIdIndexMap.size() + 1;
            this.mTypeIdIndexMap.put(Integer.valueOf(typeId), Integer.valueOf(incrementalTypeId));
            return incrementalTypeId;
        }

        public boolean isViewTypeInRange(int typeId) {
            int mappedType = getMappedViewType(typeId);
            return mappedType < this.viewTypeCount;
        }

        private RemoteViewsFrameLayout createLoadingView(int position, View convertView, ViewGroup parent, Object lock, LayoutInflater layoutInflater, RemoteViews.OnClickHandler handler) {
            Context context = parent.getContext();
            RemoteViewsFrameLayout layout = new RemoteViewsFrameLayout(context);
            synchronized (lock) {
                boolean customLoadingViewAvailable = false;
                if (this.mUserLoadingView != null) {
                    try {
                        View loadingView = this.mUserLoadingView.apply(parent.getContext(), parent, handler);
                        loadingView.setTagInternal(R.id.rowTypeId, new Integer(0));
                        layout.addView(loadingView);
                        customLoadingViewAvailable = true;
                    } catch (Exception e) {
                        Log.w(RemoteViewsAdapter.TAG, "Error inflating custom loading view, using default loadingview instead", e);
                    }
                    if (!customLoadingViewAvailable) {
                        if (this.mFirstViewHeight < 0) {
                            try {
                                View firstView = this.mFirstView.apply(parent.getContext(), parent, handler);
                                firstView.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
                                this.mFirstViewHeight = firstView.getMeasuredHeight();
                                this.mFirstView = null;
                            } catch (Exception e2) {
                                float density = context.getResources().getDisplayMetrics().density;
                                this.mFirstViewHeight = Math.round(50.0f * density);
                                this.mFirstView = null;
                                Log.w(RemoteViewsAdapter.TAG, "Error inflating first RemoteViews" + e2);
                            }
                        }
                        TextView loadingTextView = (TextView) layoutInflater.inflate(R.layout.remote_views_adapter_default_loading_view, (ViewGroup) layout, false);
                        loadingTextView.setHeight(this.mFirstViewHeight);
                        loadingTextView.setTag(new Integer(0));
                        layout.addView(loadingTextView);
                    }
                } else if (!customLoadingViewAvailable) {
                }
            }
            return layout;
        }
    }

    private static class RemoteViewsIndexMetaData {
        long itemId;
        int typeId;

        public RemoteViewsIndexMetaData(RemoteViews v, long itemId) {
            set(v, itemId);
        }

        public void set(RemoteViews v, long id) {
            this.itemId = id;
            if (v != null) {
                this.typeId = v.getLayoutId();
            } else {
                this.typeId = 0;
            }
        }
    }

    private static class FixedSizeRemoteViewsCache {
        private static final String TAG = "FixedSizeRemoteViewsCache";
        private static final float sMaxCountSlackPercent = 0.75f;
        private static final int sMaxMemoryLimitInBytes = 2097152;
        private int mMaxCount;
        private int mMaxCountSlack;
        private int mPreloadLowerBound = 0;
        private int mPreloadUpperBound = -1;
        private final RemoteViewsMetaData mMetaData = new RemoteViewsMetaData();
        private final RemoteViewsMetaData mTemporaryMetaData = new RemoteViewsMetaData();
        private HashMap<Integer, RemoteViewsIndexMetaData> mIndexMetaData = new HashMap<>();
        private HashMap<Integer, RemoteViews> mIndexRemoteViews = new HashMap<>();
        private HashSet<Integer> mRequestedIndices = new HashSet<>();
        private int mLastRequestedIndex = -1;
        private HashSet<Integer> mLoadIndices = new HashSet<>();

        public FixedSizeRemoteViewsCache(int maxCacheSize) {
            this.mMaxCount = maxCacheSize;
            this.mMaxCountSlack = Math.round(sMaxCountSlackPercent * (this.mMaxCount / 2));
        }

        public void insert(int position, RemoteViews v, long itemId, ArrayList<Integer> visibleWindow) {
            if (this.mIndexRemoteViews.size() >= this.mMaxCount) {
                this.mIndexRemoteViews.remove(Integer.valueOf(getFarthestPositionFrom(position, visibleWindow)));
            }
            int pruneFromPosition = this.mLastRequestedIndex > -1 ? this.mLastRequestedIndex : position;
            while (getRemoteViewsBitmapMemoryUsage() >= 2097152) {
                this.mIndexRemoteViews.remove(Integer.valueOf(getFarthestPositionFrom(pruneFromPosition, visibleWindow)));
            }
            if (this.mIndexMetaData.containsKey(Integer.valueOf(position))) {
                RemoteViewsIndexMetaData metaData = this.mIndexMetaData.get(Integer.valueOf(position));
                metaData.set(v, itemId);
            } else {
                this.mIndexMetaData.put(Integer.valueOf(position), new RemoteViewsIndexMetaData(v, itemId));
            }
            this.mIndexRemoteViews.put(Integer.valueOf(position), v);
        }

        public RemoteViewsMetaData getMetaData() {
            return this.mMetaData;
        }

        public RemoteViewsMetaData getTemporaryMetaData() {
            return this.mTemporaryMetaData;
        }

        public RemoteViews getRemoteViewsAt(int position) {
            if (this.mIndexRemoteViews.containsKey(Integer.valueOf(position))) {
                return this.mIndexRemoteViews.get(Integer.valueOf(position));
            }
            return null;
        }

        public RemoteViewsIndexMetaData getMetaDataAt(int position) {
            if (this.mIndexMetaData.containsKey(Integer.valueOf(position))) {
                return this.mIndexMetaData.get(Integer.valueOf(position));
            }
            return null;
        }

        public void commitTemporaryMetaData() {
            synchronized (this.mTemporaryMetaData) {
                synchronized (this.mMetaData) {
                    this.mMetaData.set(this.mTemporaryMetaData);
                }
            }
        }

        private int getRemoteViewsBitmapMemoryUsage() {
            int mem = 0;
            for (Integer i : this.mIndexRemoteViews.keySet()) {
                RemoteViews v = this.mIndexRemoteViews.get(i);
                if (v != null) {
                    mem += v.estimateMemoryUsage();
                }
            }
            return mem;
        }

        private int getFarthestPositionFrom(int pos, ArrayList<Integer> visibleWindow) {
            int maxDist = 0;
            int maxDistIndex = -1;
            int maxDistNotVisible = 0;
            int maxDistIndexNotVisible = -1;
            Iterator<Integer> it = this.mIndexRemoteViews.keySet().iterator();
            while (it.hasNext()) {
                int i = it.next().intValue();
                int dist = Math.abs(i - pos);
                if (dist > maxDistNotVisible && !visibleWindow.contains(Integer.valueOf(i))) {
                    maxDistIndexNotVisible = i;
                    maxDistNotVisible = dist;
                }
                if (dist >= maxDist) {
                    maxDistIndex = i;
                    maxDist = dist;
                }
            }
            if (maxDistIndexNotVisible > -1) {
                return maxDistIndexNotVisible;
            }
            int maxDistIndexNotVisible2 = maxDistIndex;
            return maxDistIndexNotVisible2;
        }

        public void queueRequestedPositionToLoad(int position) {
            this.mLastRequestedIndex = position;
            synchronized (this.mLoadIndices) {
                this.mRequestedIndices.add(Integer.valueOf(position));
                this.mLoadIndices.add(Integer.valueOf(position));
            }
        }

        public boolean queuePositionsToBePreloadedFromRequestedPosition(int position) {
            int count;
            if (this.mPreloadLowerBound <= position && position <= this.mPreloadUpperBound) {
                int center = (this.mPreloadUpperBound + this.mPreloadLowerBound) / 2;
                if (Math.abs(position - center) < this.mMaxCountSlack) {
                    return false;
                }
            }
            synchronized (this.mMetaData) {
                count = this.mMetaData.count;
            }
            synchronized (this.mLoadIndices) {
                this.mLoadIndices.clear();
                this.mLoadIndices.addAll(this.mRequestedIndices);
                int halfMaxCount = this.mMaxCount / 2;
                this.mPreloadLowerBound = position - halfMaxCount;
                this.mPreloadUpperBound = position + halfMaxCount;
                int effectiveLowerBound = Math.max(0, this.mPreloadLowerBound);
                int effectiveUpperBound = Math.min(this.mPreloadUpperBound, count - 1);
                for (int i = effectiveLowerBound; i <= effectiveUpperBound; i++) {
                    this.mLoadIndices.add(Integer.valueOf(i));
                }
                this.mLoadIndices.removeAll(this.mIndexRemoteViews.keySet());
            }
            return true;
        }

        public int[] getNextIndexToLoad() {
            int[] iArr;
            synchronized (this.mLoadIndices) {
                if (!this.mRequestedIndices.isEmpty()) {
                    Integer i = this.mRequestedIndices.iterator().next();
                    this.mRequestedIndices.remove(i);
                    this.mLoadIndices.remove(i);
                    iArr = new int[]{i.intValue(), 1};
                } else if (!this.mLoadIndices.isEmpty()) {
                    Integer i2 = this.mLoadIndices.iterator().next();
                    this.mLoadIndices.remove(i2);
                    iArr = new int[]{i2.intValue(), 0};
                } else {
                    iArr = new int[]{-1, 0};
                }
            }
            return iArr;
        }

        public boolean containsRemoteViewAt(int position) {
            return this.mIndexRemoteViews.containsKey(Integer.valueOf(position));
        }

        public boolean containsMetaDataAt(int position) {
            return this.mIndexMetaData.containsKey(Integer.valueOf(position));
        }

        public void reset() {
            this.mPreloadLowerBound = 0;
            this.mPreloadUpperBound = -1;
            this.mLastRequestedIndex = -1;
            this.mIndexRemoteViews.clear();
            this.mIndexMetaData.clear();
            synchronized (this.mLoadIndices) {
                this.mRequestedIndices.clear();
                this.mLoadIndices.clear();
            }
        }
    }

    static class RemoteViewsCacheKey {
        final Intent.FilterComparison filter;
        final int widgetId;

        RemoteViewsCacheKey(Intent.FilterComparison filter, int widgetId) {
            this.filter = filter;
            this.widgetId = widgetId;
        }

        public boolean equals(Object o) {
            if (!(o instanceof RemoteViewsCacheKey)) {
                return false;
            }
            RemoteViewsCacheKey other = (RemoteViewsCacheKey) o;
            return other.filter.equals(this.filter) && other.widgetId == this.widgetId;
        }

        public int hashCode() {
            return (this.filter == null ? 0 : this.filter.hashCode()) ^ (this.widgetId << 2);
        }
    }

    public RemoteViewsAdapter(Context context, Intent intent, RemoteAdapterConnectionCallback callback) {
        this.mDataReady = false;
        this.mContext = context;
        this.mIntent = intent;
        this.mAppWidgetId = intent.getIntExtra("remoteAdapterAppWidgetId", -1);
        this.mLayoutInflater = LayoutInflater.from(context);
        if (this.mIntent == null) {
            throw new IllegalArgumentException("Non-null Intent must be specified.");
        }
        this.mRequestedViews = new RemoteViewsFrameLayoutRefSet();
        if (intent.hasExtra("remoteAdapterAppWidgetId")) {
            intent.removeExtra("remoteAdapterAppWidgetId");
        }
        this.mWorkerThread = new HandlerThread("RemoteViewsCache-loader");
        this.mWorkerThread.start();
        this.mWorkerQueue = new Handler(this.mWorkerThread.getLooper());
        this.mMainQueue = new Handler(Looper.myLooper(), this);
        if (sCacheRemovalThread == null) {
            sCacheRemovalThread = new HandlerThread("RemoteViewsAdapter-cachePruner");
            sCacheRemovalThread.start();
            sCacheRemovalQueue = new Handler(sCacheRemovalThread.getLooper());
        }
        this.mCallback = new WeakReference<>(callback);
        this.mServiceConnection = new RemoteViewsAdapterServiceConnection(this);
        RemoteViewsCacheKey key = new RemoteViewsCacheKey(new Intent.FilterComparison(this.mIntent), this.mAppWidgetId);
        synchronized (sCachedRemoteViewsCaches) {
            if (sCachedRemoteViewsCaches.containsKey(key)) {
                this.mCache = sCachedRemoteViewsCaches.get(key);
                synchronized (this.mCache.mMetaData) {
                    if (this.mCache.mMetaData.count > 0) {
                        this.mDataReady = true;
                    }
                }
            } else {
                this.mCache = new FixedSizeRemoteViewsCache(40);
            }
            if (!this.mDataReady) {
                requestBindService();
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mWorkerThread != null) {
                this.mWorkerThread.quit();
            }
        } finally {
            super.finalize();
        }
    }

    public boolean isDataReady() {
        return this.mDataReady;
    }

    public void setRemoteViewsOnClickHandler(RemoteViews.OnClickHandler handler) {
        this.mRemoteViewsOnClickHandler = handler;
    }

    public void saveRemoteViewsCache() {
        int metaDataCount;
        int numRemoteViewsCached;
        final RemoteViewsCacheKey key = new RemoteViewsCacheKey(new Intent.FilterComparison(this.mIntent), this.mAppWidgetId);
        synchronized (sCachedRemoteViewsCaches) {
            if (sRemoteViewsCacheRemoveRunnables.containsKey(key)) {
                sCacheRemovalQueue.removeCallbacks(sRemoteViewsCacheRemoveRunnables.get(key));
                sRemoteViewsCacheRemoveRunnables.remove(key);
            }
            synchronized (this.mCache.mMetaData) {
                metaDataCount = this.mCache.mMetaData.count;
            }
            synchronized (this.mCache) {
                numRemoteViewsCached = this.mCache.mIndexRemoteViews.size();
            }
            if (metaDataCount > 0 && numRemoteViewsCached > 0) {
                sCachedRemoteViewsCaches.put(key, this.mCache);
            }
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    synchronized (RemoteViewsAdapter.sCachedRemoteViewsCaches) {
                        if (RemoteViewsAdapter.sCachedRemoteViewsCaches.containsKey(key)) {
                            RemoteViewsAdapter.sCachedRemoteViewsCaches.remove(key);
                        }
                        if (RemoteViewsAdapter.sRemoteViewsCacheRemoveRunnables.containsKey(key)) {
                            RemoteViewsAdapter.sRemoteViewsCacheRemoveRunnables.remove(key);
                        }
                    }
                }
            };
            sRemoteViewsCacheRemoveRunnables.put(key, r);
            sCacheRemovalQueue.postDelayed(r, TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
        }
    }

    private void loadNextIndexInBackground() {
        this.mWorkerQueue.post(new Runnable() {
            @Override
            public void run() {
                int position;
                if (RemoteViewsAdapter.this.mServiceConnection.isConnected()) {
                    synchronized (RemoteViewsAdapter.this.mCache) {
                        int[] res = RemoteViewsAdapter.this.mCache.getNextIndexToLoad();
                        position = res[0];
                    }
                    if (position > -1) {
                        RemoteViewsAdapter.this.updateRemoteViews(position, true);
                        RemoteViewsAdapter.this.loadNextIndexInBackground();
                    } else {
                        RemoteViewsAdapter.this.enqueueDeferredUnbindServiceMessage();
                    }
                }
            }
        });
    }

    private void processException(String method, Exception e) {
        Log.e(TAG, "Error in " + method + ": " + e.getMessage());
        RemoteViewsMetaData metaData = this.mCache.getMetaData();
        synchronized (metaData) {
            metaData.reset();
        }
        synchronized (this.mCache) {
            this.mCache.reset();
        }
        this.mMainQueue.post(new Runnable() {
            @Override
            public void run() {
                RemoteViewsAdapter.this.superNotifyDataSetChanged();
            }
        });
    }

    private void updateTemporaryMetaData() {
        IRemoteViewsFactory factory = this.mServiceConnection.getRemoteViewsFactory();
        try {
            boolean hasStableIds = factory.hasStableIds();
            int viewTypeCount = factory.getViewTypeCount();
            int count = factory.getCount();
            RemoteViews loadingView = factory.getLoadingView();
            RemoteViews firstView = null;
            if (count > 0 && loadingView == null) {
                firstView = factory.getViewAt(0);
            }
            RemoteViewsMetaData tmpMetaData = this.mCache.getTemporaryMetaData();
            synchronized (tmpMetaData) {
                tmpMetaData.hasStableIds = hasStableIds;
                tmpMetaData.viewTypeCount = viewTypeCount + 1;
                tmpMetaData.count = count;
                tmpMetaData.setLoadingViewTemplates(loadingView, firstView);
            }
        } catch (RemoteException e) {
            processException("updateMetaData", e);
        } catch (RuntimeException e2) {
            processException("updateMetaData", e2);
        }
    }

    private void updateRemoteViews(final int position, boolean notifyWhenLoaded) {
        boolean viewTypeInRange;
        int cacheCount;
        IRemoteViewsFactory factory = this.mServiceConnection.getRemoteViewsFactory();
        try {
            final RemoteViews remoteViews = factory.getViewAt(position);
            long itemId = factory.getItemId(position);
            if (remoteViews == null) {
                Log.e(TAG, "Error in updateRemoteViews(" + position + "):  null RemoteViews returned from RemoteViewsFactory.");
                return;
            }
            int layoutId = remoteViews.getLayoutId();
            RemoteViewsMetaData metaData = this.mCache.getMetaData();
            synchronized (metaData) {
                viewTypeInRange = metaData.isViewTypeInRange(layoutId);
                cacheCount = this.mCache.mMetaData.count;
            }
            synchronized (this.mCache) {
                if (viewTypeInRange) {
                    ArrayList<Integer> visibleWindow = getVisibleWindow(this.mVisibleWindowLowerBound, this.mVisibleWindowUpperBound, cacheCount);
                    this.mCache.insert(position, remoteViews, itemId, visibleWindow);
                    if (notifyWhenLoaded) {
                        this.mMainQueue.post(new Runnable() {
                            @Override
                            public void run() {
                                RemoteViewsAdapter.this.mRequestedViews.notifyOnRemoteViewsLoaded(position, remoteViews);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Error: widget's RemoteViewsFactory returns more view types than  indicated by getViewTypeCount() ");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error in updateRemoteViews(" + position + "): " + e.getMessage());
        } catch (RuntimeException e2) {
            Log.e(TAG, "Error in updateRemoteViews(" + position + "): " + e2.getMessage());
        }
    }

    public Intent getRemoteViewsServiceIntent() {
        return this.mIntent;
    }

    @Override
    public int getCount() {
        int i;
        RemoteViewsMetaData metaData = this.mCache.getMetaData();
        synchronized (metaData) {
            i = metaData.count;
        }
        return i;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        long j;
        synchronized (this.mCache) {
            j = this.mCache.containsMetaDataAt(position) ? this.mCache.getMetaDataAt(position).itemId : 0L;
        }
        return j;
    }

    @Override
    public int getItemViewType(int position) {
        int mappedViewType;
        synchronized (this.mCache) {
            if (this.mCache.containsMetaDataAt(position)) {
                int typeId = this.mCache.getMetaDataAt(position).typeId;
                RemoteViewsMetaData metaData = this.mCache.getMetaData();
                synchronized (metaData) {
                    mappedViewType = metaData.getMappedViewType(typeId);
                }
            } else {
                mappedViewType = 0;
            }
        }
        return mappedViewType;
    }

    private int getConvertViewTypeId(View convertView) {
        Object tag;
        if (convertView == null || (tag = convertView.getTag(R.id.rowTypeId)) == null) {
            return -1;
        }
        int typeId = ((Integer) tag).intValue();
        return typeId;
    }

    public void setVisibleRangeHint(int lowerBound, int upperBound) {
        this.mVisibleWindowLowerBound = lowerBound;
        this.mVisibleWindowUpperBound = upperBound;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RemoteViewsFrameLayout loadingView;
        RemoteViewsFrameLayout layout;
        RemoteViewsFrameLayout loadingView2;
        RemoteViewsFrameLayout layout2;
        synchronized (this.mCache) {
            boolean isInCache = this.mCache.containsRemoteViewAt(position);
            boolean isConnected = this.mServiceConnection.isConnected();
            boolean hasNewItems = false;
            if (convertView != null && (convertView instanceof RemoteViewsFrameLayout)) {
                this.mRequestedViews.removeView((RemoteViewsFrameLayout) convertView);
            }
            if (isInCache || isConnected) {
                hasNewItems = this.mCache.queuePositionsToBePreloadedFromRequestedPosition(position);
            } else {
                requestBindService();
            }
            if (!isInCache) {
                RemoteViewsMetaData metaData = this.mCache.getMetaData();
                synchronized (metaData) {
                    loadingView = metaData.createLoadingView(position, convertView, parent, this.mCache, this.mLayoutInflater, this.mRemoteViewsOnClickHandler);
                }
                this.mRequestedViews.add(position, loadingView);
                this.mCache.queueRequestedPositionToLoad(position);
                loadNextIndexInBackground();
                return loadingView;
            }
            View convertViewChild = null;
            int convertViewTypeId = 0;
            if (convertView instanceof RemoteViewsFrameLayout) {
                RemoteViewsFrameLayout layout3 = (RemoteViewsFrameLayout) convertView;
                convertViewChild = layout3.getChildAt(0);
                convertViewTypeId = getConvertViewTypeId(convertViewChild);
                layout = layout3;
            } else {
                layout = null;
            }
            Context context = parent.getContext();
            RemoteViews rv = this.mCache.getRemoteViewsAt(position);
            RemoteViewsIndexMetaData indexMetaData = this.mCache.getMetaDataAt(position);
            int typeId = indexMetaData.typeId;
            try {
                try {
                    if (layout == null) {
                        layout2 = new RemoteViewsFrameLayout(context);
                    } else {
                        if (convertViewTypeId == typeId) {
                            rv.reapply(context, convertViewChild, this.mRemoteViewsOnClickHandler);
                            if (hasNewItems) {
                                loadNextIndexInBackground();
                            }
                            return layout;
                        }
                        layout.removeAllViews();
                        layout2 = layout;
                    }
                    try {
                        View newView = rv.apply(context, parent, this.mRemoteViewsOnClickHandler);
                        newView.setTagInternal(R.id.rowTypeId, new Integer(typeId));
                        layout2.addView(newView);
                        if (hasNewItems) {
                            loadNextIndexInBackground();
                        }
                        return layout2;
                    } catch (Exception e) {
                        e = e;
                        Log.w(TAG, "Error inflating RemoteViews at position: " + position + ", usingloading view instead" + e);
                        RemoteViewsMetaData metaData2 = this.mCache.getMetaData();
                        synchronized (metaData2) {
                            loadingView2 = metaData2.createLoadingView(position, convertView, parent, this.mCache, this.mLayoutInflater, this.mRemoteViewsOnClickHandler);
                        }
                        if (hasNewItems) {
                            loadNextIndexInBackground();
                        }
                        return loadingView2;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (hasNewItems) {
                        loadNextIndexInBackground();
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
            } catch (Throwable th2) {
                th = th2;
                if (hasNewItems) {
                }
                throw th;
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        int i;
        RemoteViewsMetaData metaData = this.mCache.getMetaData();
        synchronized (metaData) {
            i = metaData.viewTypeCount;
        }
        return i;
    }

    @Override
    public boolean hasStableIds() {
        boolean z;
        RemoteViewsMetaData metaData = this.mCache.getMetaData();
        synchronized (metaData) {
            z = metaData.hasStableIds;
        }
        return z;
    }

    @Override
    public boolean isEmpty() {
        return getCount() <= 0;
    }

    private void onNotifyDataSetChanged() {
        int newCount;
        ArrayList<Integer> visibleWindow;
        IRemoteViewsFactory factory = this.mServiceConnection.getRemoteViewsFactory();
        try {
            factory.onDataSetChanged();
            synchronized (this.mCache) {
                this.mCache.reset();
            }
            updateTemporaryMetaData();
            synchronized (this.mCache.getTemporaryMetaData()) {
                newCount = this.mCache.getTemporaryMetaData().count;
                visibleWindow = getVisibleWindow(this.mVisibleWindowLowerBound, this.mVisibleWindowUpperBound, newCount);
            }
            Iterator<Integer> it = visibleWindow.iterator();
            while (it.hasNext()) {
                int i = it.next().intValue();
                if (i < newCount) {
                    updateRemoteViews(i, false);
                }
            }
            this.mMainQueue.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (RemoteViewsAdapter.this.mCache) {
                        RemoteViewsAdapter.this.mCache.commitTemporaryMetaData();
                    }
                    RemoteViewsAdapter.this.superNotifyDataSetChanged();
                    RemoteViewsAdapter.this.enqueueDeferredUnbindServiceMessage();
                }
            });
            this.mNotifyDataSetChangedAfterOnServiceConnected = false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error in updateNotifyDataSetChanged(): " + e.getMessage());
        } catch (RuntimeException e2) {
            Log.e(TAG, "Error in updateNotifyDataSetChanged(): " + e2.getMessage());
        }
    }

    private ArrayList<Integer> getVisibleWindow(int lower, int upper, int count) {
        ArrayList<Integer> window = new ArrayList<>();
        if ((lower != 0 || upper != 0) && lower >= 0 && upper >= 0) {
            if (lower <= upper) {
                for (int i = lower; i <= upper; i++) {
                    window.add(Integer.valueOf(i));
                }
            } else {
                for (int i2 = lower; i2 < count; i2++) {
                    window.add(Integer.valueOf(i2));
                }
                for (int i3 = 0; i3 <= upper; i3++) {
                    window.add(Integer.valueOf(i3));
                }
            }
        }
        return window;
    }

    @Override
    public void notifyDataSetChanged() {
        this.mMainQueue.removeMessages(1);
        if (!this.mServiceConnection.isConnected()) {
            if (!this.mNotifyDataSetChangedAfterOnServiceConnected) {
                this.mNotifyDataSetChangedAfterOnServiceConnected = true;
                requestBindService();
                return;
            }
            return;
        }
        this.mWorkerQueue.post(new Runnable() {
            @Override
            public void run() {
                RemoteViewsAdapter.this.onNotifyDataSetChanged();
            }
        });
    }

    void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                if (this.mServiceConnection.isConnected()) {
                    this.mServiceConnection.unbind(this.mContext, this.mAppWidgetId, this.mIntent);
                }
                return true;
            default:
                return false;
        }
    }

    private void enqueueDeferredUnbindServiceMessage() {
        this.mMainQueue.removeMessages(1);
        this.mMainQueue.sendEmptyMessageDelayed(1, TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
    }

    private boolean requestBindService() {
        if (!this.mServiceConnection.isConnected()) {
            this.mServiceConnection.bind(this.mContext, this.mAppWidgetId, this.mIntent);
        }
        this.mMainQueue.removeMessages(1);
        return this.mServiceConnection.isConnected();
    }
}
