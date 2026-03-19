package android.widget;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimedRemoteCaller;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.widget.IRemoteViewsFactory;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executor;

public class RemoteViewsAdapter extends BaseAdapter implements Handler.Callback {
    private static final int DEFAULT_CACHE_SIZE = 40;
    private static final int DEFAULT_LOADING_VIEW_HEIGHT = 50;
    static final int MSG_LOAD_NEXT_ITEM = 3;
    private static final int MSG_MAIN_HANDLER_COMMIT_METADATA = 1;
    private static final int MSG_MAIN_HANDLER_REMOTE_ADAPTER_CONNECTED = 3;
    private static final int MSG_MAIN_HANDLER_REMOTE_ADAPTER_DISCONNECTED = 4;
    private static final int MSG_MAIN_HANDLER_REMOTE_VIEWS_LOADED = 5;
    private static final int MSG_MAIN_HANDLER_SUPER_NOTIFY_DATA_SET_CHANGED = 2;
    static final int MSG_NOTIFY_DATA_SET_CHANGED = 2;
    static final int MSG_REQUEST_BIND = 1;
    static final int MSG_UNBIND_SERVICE = 4;
    private static final int REMOTE_VIEWS_CACHE_DURATION = 5000;
    private static final String TAG = "RemoteViewsAdapter";
    private static final int UNBIND_SERVICE_DELAY = 5000;
    private static Handler sCacheRemovalQueue;
    private static HandlerThread sCacheRemovalThread;
    private static final HashMap<RemoteViewsCacheKey, FixedSizeRemoteViewsCache> sCachedRemoteViewsCaches = new HashMap<>();
    private static final HashMap<RemoteViewsCacheKey, Runnable> sRemoteViewsCacheRemoveRunnables = new HashMap<>();
    private final int mAppWidgetId;
    private final Executor mAsyncViewLoadExecutor;
    private final FixedSizeRemoteViewsCache mCache;
    private final RemoteAdapterConnectionCallback mCallback;
    private final Context mContext;
    private boolean mDataReady;
    private final Intent mIntent;
    private ApplicationInfo mLastRemoteViewAppInfo;
    private final Handler mMainHandler;
    private RemoteViews.OnClickHandler mRemoteViewsOnClickHandler;
    private RemoteViewsFrameLayoutRefSet mRequestedViews;
    private final RemoteServiceHandler mServiceHandler;
    private int mVisibleWindowLowerBound;
    private int mVisibleWindowUpperBound;
    private final HandlerThread mWorkerThread;

    public interface RemoteAdapterConnectionCallback {
        void deferNotifyDataSetChanged();

        boolean onRemoteAdapterConnected();

        void onRemoteAdapterDisconnected();

        void setRemoteViewsAdapter(Intent intent, boolean z);
    }

    public static class AsyncRemoteAdapterAction implements Runnable {
        private final RemoteAdapterConnectionCallback mCallback;
        private final Intent mIntent;

        public AsyncRemoteAdapterAction(RemoteAdapterConnectionCallback remoteAdapterConnectionCallback, Intent intent) {
            this.mCallback = remoteAdapterConnectionCallback;
            this.mIntent = intent;
        }

        @Override
        public void run() {
            this.mCallback.setRemoteViewsAdapter(this.mIntent, true);
        }
    }

    private static class RemoteServiceHandler extends Handler implements ServiceConnection {
        private final WeakReference<RemoteViewsAdapter> mAdapter;
        private boolean mBindRequested;
        private final Context mContext;
        private boolean mNotifyDataSetChangedPending;
        private IRemoteViewsFactory mRemoteViewsFactory;

        RemoteServiceHandler(Looper looper, RemoteViewsAdapter remoteViewsAdapter, Context context) {
            super(looper);
            this.mNotifyDataSetChangedPending = false;
            this.mBindRequested = false;
            this.mAdapter = new WeakReference<>(remoteViewsAdapter);
            this.mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            this.mRemoteViewsFactory = IRemoteViewsFactory.Stub.asInterface(iBinder);
            enqueueDeferredUnbindServiceMessage();
            RemoteViewsAdapter remoteViewsAdapter = this.mAdapter.get();
            if (remoteViewsAdapter == null) {
                return;
            }
            if (this.mNotifyDataSetChangedPending) {
                this.mNotifyDataSetChangedPending = false;
                Message messageObtain = Message.obtain(this, 2);
                handleMessage(messageObtain);
                messageObtain.recycle();
                return;
            }
            if (sendNotifyDataSetChange(false)) {
                remoteViewsAdapter.updateTemporaryMetaData(this.mRemoteViewsFactory);
                remoteViewsAdapter.mMainHandler.sendEmptyMessage(1);
                remoteViewsAdapter.mMainHandler.sendEmptyMessage(3);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            this.mRemoteViewsFactory = null;
            RemoteViewsAdapter remoteViewsAdapter = this.mAdapter.get();
            if (remoteViewsAdapter != null) {
                remoteViewsAdapter.mMainHandler.sendEmptyMessage(4);
            }
        }

        @Override
        public void handleMessage(Message message) {
            int i;
            int[] visibleWindow;
            RemoteViewsAdapter remoteViewsAdapter = this.mAdapter.get();
            switch (message.what) {
                case 1:
                    if (remoteViewsAdapter == null || this.mRemoteViewsFactory != null) {
                        enqueueDeferredUnbindServiceMessage();
                    }
                    if (this.mBindRequested) {
                        return;
                    }
                    this.mBindRequested = AppWidgetManager.getInstance(this.mContext).bindRemoteViewsService(this.mContext, message.arg1, (Intent) message.obj, this.mContext.getServiceDispatcher(this, this, InputDevice.SOURCE_HDMI), InputDevice.SOURCE_HDMI);
                    return;
                case 2:
                    enqueueDeferredUnbindServiceMessage();
                    if (remoteViewsAdapter == null) {
                        return;
                    }
                    if (this.mRemoteViewsFactory == null) {
                        this.mNotifyDataSetChangedPending = true;
                        remoteViewsAdapter.requestBindService();
                        return;
                    }
                    if (sendNotifyDataSetChange(true)) {
                        synchronized (remoteViewsAdapter.mCache) {
                            remoteViewsAdapter.mCache.reset();
                            break;
                        }
                        remoteViewsAdapter.updateTemporaryMetaData(this.mRemoteViewsFactory);
                        synchronized (remoteViewsAdapter.mCache.getTemporaryMetaData()) {
                            i = remoteViewsAdapter.mCache.getTemporaryMetaData().count;
                            visibleWindow = remoteViewsAdapter.getVisibleWindow(i);
                            break;
                        }
                        for (int i2 : visibleWindow) {
                            if (i2 < i) {
                                remoteViewsAdapter.updateRemoteViews(this.mRemoteViewsFactory, i2, false);
                            }
                        }
                        remoteViewsAdapter.mMainHandler.sendEmptyMessage(1);
                        remoteViewsAdapter.mMainHandler.sendEmptyMessage(2);
                        return;
                    }
                    return;
                case 3:
                    if (remoteViewsAdapter == null || this.mRemoteViewsFactory == null) {
                        return;
                    }
                    removeMessages(4);
                    int nextIndexToLoad = remoteViewsAdapter.mCache.getNextIndexToLoad();
                    if (nextIndexToLoad > -1) {
                        remoteViewsAdapter.updateRemoteViews(this.mRemoteViewsFactory, nextIndexToLoad, true);
                        sendEmptyMessage(3);
                        return;
                    } else {
                        enqueueDeferredUnbindServiceMessage();
                        return;
                    }
                case 4:
                    unbindNow();
                    return;
                default:
                    return;
            }
        }

        protected void unbindNow() {
            if (this.mBindRequested) {
                this.mBindRequested = false;
                this.mContext.unbindService(this);
            }
            this.mRemoteViewsFactory = null;
        }

        private boolean sendNotifyDataSetChange(boolean z) {
            if (!z) {
                try {
                    if (this.mRemoteViewsFactory.isCreated()) {
                        return true;
                    }
                } catch (RemoteException | RuntimeException e) {
                    Log.e(RemoteViewsAdapter.TAG, "Error in updateNotifyDataSetChanged(): " + e.getMessage());
                    return false;
                }
            }
            this.mRemoteViewsFactory.onDataSetChanged();
            return true;
        }

        private void enqueueDeferredUnbindServiceMessage() {
            removeMessages(4);
            sendEmptyMessageDelayed(4, TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
        }
    }

    static class RemoteViewsFrameLayout extends AppWidgetHostView {
        public int cacheIndex;
        private final FixedSizeRemoteViewsCache mCache;

        public RemoteViewsFrameLayout(Context context, FixedSizeRemoteViewsCache fixedSizeRemoteViewsCache) {
            super(context);
            this.cacheIndex = -1;
            this.mCache = fixedSizeRemoteViewsCache;
        }

        public void onRemoteViewsLoaded(RemoteViews remoteViews, RemoteViews.OnClickHandler onClickHandler, boolean z) {
            setOnClickHandler(onClickHandler);
            applyRemoteViews(remoteViews, z || (remoteViews != null && remoteViews.prefersAsyncApply()));
        }

        @Override
        protected View getDefaultView() {
            int i = this.mCache.getMetaData().getLoadingTemplate(getContext()).defaultHeight;
            TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.remote_views_adapter_default_loading_view, (ViewGroup) this, false);
            textView.setHeight(i);
            return textView;
        }

        @Override
        protected Context getRemoteContext() {
            return null;
        }

        @Override
        protected View getErrorView() {
            return getDefaultView();
        }
    }

    private class RemoteViewsFrameLayoutRefSet extends SparseArray<LinkedList<RemoteViewsFrameLayout>> {
        private RemoteViewsFrameLayoutRefSet() {
        }

        public void add(int i, RemoteViewsFrameLayout remoteViewsFrameLayout) {
            LinkedList<RemoteViewsFrameLayout> linkedList = get(i);
            if (linkedList == null) {
                linkedList = new LinkedList<>();
                put(i, linkedList);
            }
            remoteViewsFrameLayout.cacheIndex = i;
            linkedList.add(remoteViewsFrameLayout);
        }

        public void notifyOnRemoteViewsLoaded(int i, RemoteViews remoteViews) {
            LinkedList<RemoteViewsFrameLayout> linkedListRemoveReturnOld;
            if (remoteViews != null && (linkedListRemoveReturnOld = removeReturnOld(i)) != null) {
                Iterator<RemoteViewsFrameLayout> it = linkedListRemoveReturnOld.iterator();
                while (it.hasNext()) {
                    it.next().onRemoteViewsLoaded(remoteViews, RemoteViewsAdapter.this.mRemoteViewsOnClickHandler, true);
                }
            }
        }

        public void removeView(RemoteViewsFrameLayout remoteViewsFrameLayout) {
            if (remoteViewsFrameLayout.cacheIndex < 0) {
                return;
            }
            LinkedList<RemoteViewsFrameLayout> linkedList = get(remoteViewsFrameLayout.cacheIndex);
            if (linkedList != null) {
                linkedList.remove(remoteViewsFrameLayout);
            }
            remoteViewsFrameLayout.cacheIndex = -1;
        }
    }

    private static class RemoteViewsMetaData {
        int count;
        boolean hasStableIds;
        LoadingViewTemplate loadingTemplate;
        private final SparseIntArray mTypeIdIndexMap = new SparseIntArray();
        int viewTypeCount;

        public RemoteViewsMetaData() {
            reset();
        }

        public void set(RemoteViewsMetaData remoteViewsMetaData) {
            synchronized (remoteViewsMetaData) {
                this.count = remoteViewsMetaData.count;
                this.viewTypeCount = remoteViewsMetaData.viewTypeCount;
                this.hasStableIds = remoteViewsMetaData.hasStableIds;
                this.loadingTemplate = remoteViewsMetaData.loadingTemplate;
            }
        }

        public void reset() {
            this.count = 0;
            this.viewTypeCount = 1;
            this.hasStableIds = true;
            this.loadingTemplate = null;
            this.mTypeIdIndexMap.clear();
        }

        public int getMappedViewType(int i) {
            int i2 = this.mTypeIdIndexMap.get(i, -1);
            if (i2 == -1) {
                int size = this.mTypeIdIndexMap.size() + 1;
                this.mTypeIdIndexMap.put(i, size);
                return size;
            }
            return i2;
        }

        public boolean isViewTypeInRange(int i) {
            return getMappedViewType(i) < this.viewTypeCount;
        }

        public synchronized LoadingViewTemplate getLoadingTemplate(Context context) {
            if (this.loadingTemplate == null) {
                this.loadingTemplate = new LoadingViewTemplate(null, context);
            }
            return this.loadingTemplate;
        }
    }

    private static class RemoteViewsIndexMetaData {
        long itemId;
        int typeId;

        public RemoteViewsIndexMetaData(RemoteViews remoteViews, long j) {
            set(remoteViews, j);
        }

        public void set(RemoteViews remoteViews, long j) {
            this.itemId = j;
            if (remoteViews != null) {
                this.typeId = remoteViews.getLayoutId();
            } else {
                this.typeId = 0;
            }
        }
    }

    private static class FixedSizeRemoteViewsCache {
        private static final float sMaxCountSlackPercent = 0.75f;
        private static final int sMaxMemoryLimitInBytes = 2097152;
        private final int mMaxCount;
        private final int mMaxCountSlack;
        private final RemoteViewsMetaData mMetaData = new RemoteViewsMetaData();
        private final RemoteViewsMetaData mTemporaryMetaData = new RemoteViewsMetaData();
        private final SparseArray<RemoteViewsIndexMetaData> mIndexMetaData = new SparseArray<>();
        private final SparseArray<RemoteViews> mIndexRemoteViews = new SparseArray<>();
        private final SparseBooleanArray mIndicesToLoad = new SparseBooleanArray();
        private int mPreloadLowerBound = 0;
        private int mPreloadUpperBound = -1;
        private int mLastRequestedIndex = -1;

        public FixedSizeRemoteViewsCache(int i) {
            this.mMaxCount = i;
            this.mMaxCountSlack = Math.round(sMaxCountSlackPercent * (this.mMaxCount / 2));
        }

        public void insert(int i, RemoteViews remoteViews, long j, int[] iArr) {
            int farthestPositionFrom;
            if (this.mIndexRemoteViews.size() >= this.mMaxCount) {
                this.mIndexRemoteViews.remove(getFarthestPositionFrom(i, iArr));
            }
            int i2 = this.mLastRequestedIndex > -1 ? this.mLastRequestedIndex : i;
            while (getRemoteViewsBitmapMemoryUsage() >= 2097152 && (farthestPositionFrom = getFarthestPositionFrom(i2, iArr)) >= 0) {
                this.mIndexRemoteViews.remove(farthestPositionFrom);
            }
            RemoteViewsIndexMetaData remoteViewsIndexMetaData = this.mIndexMetaData.get(i);
            if (remoteViewsIndexMetaData != null) {
                remoteViewsIndexMetaData.set(remoteViews, j);
            } else {
                this.mIndexMetaData.put(i, new RemoteViewsIndexMetaData(remoteViews, j));
            }
            this.mIndexRemoteViews.put(i, remoteViews);
        }

        public RemoteViewsMetaData getMetaData() {
            return this.mMetaData;
        }

        public RemoteViewsMetaData getTemporaryMetaData() {
            return this.mTemporaryMetaData;
        }

        public RemoteViews getRemoteViewsAt(int i) {
            return this.mIndexRemoteViews.get(i);
        }

        public RemoteViewsIndexMetaData getMetaDataAt(int i) {
            return this.mIndexMetaData.get(i);
        }

        public void commitTemporaryMetaData() {
            synchronized (this.mTemporaryMetaData) {
                synchronized (this.mMetaData) {
                    this.mMetaData.set(this.mTemporaryMetaData);
                }
            }
        }

        private int getRemoteViewsBitmapMemoryUsage() {
            int iEstimateMemoryUsage = 0;
            for (int size = this.mIndexRemoteViews.size() - 1; size >= 0; size--) {
                RemoteViews remoteViewsValueAt = this.mIndexRemoteViews.valueAt(size);
                if (remoteViewsValueAt != null) {
                    iEstimateMemoryUsage += remoteViewsValueAt.estimateMemoryUsage();
                }
            }
            return iEstimateMemoryUsage;
        }

        private int getFarthestPositionFrom(int i, int[] iArr) {
            int i2 = 0;
            int i3 = 0;
            int i4 = -1;
            int i5 = -1;
            for (int size = this.mIndexRemoteViews.size() - 1; size >= 0; size--) {
                int iKeyAt = this.mIndexRemoteViews.keyAt(size);
                int iAbs = Math.abs(iKeyAt - i);
                if (iAbs > i2 && Arrays.binarySearch(iArr, iKeyAt) < 0) {
                    i4 = iKeyAt;
                    i2 = iAbs;
                }
                if (iAbs >= i3) {
                    i5 = iKeyAt;
                    i3 = iAbs;
                }
            }
            if (i4 > -1) {
                return i4;
            }
            return i5;
        }

        public void queueRequestedPositionToLoad(int i) {
            this.mLastRequestedIndex = i;
            synchronized (this.mIndicesToLoad) {
                this.mIndicesToLoad.put(i, true);
            }
        }

        public boolean queuePositionsToBePreloadedFromRequestedPosition(int i) {
            int i2;
            if (this.mPreloadLowerBound <= i && i <= this.mPreloadUpperBound && Math.abs(i - ((this.mPreloadUpperBound + this.mPreloadLowerBound) / 2)) < this.mMaxCountSlack) {
                return false;
            }
            synchronized (this.mMetaData) {
                i2 = this.mMetaData.count;
            }
            synchronized (this.mIndicesToLoad) {
                for (int size = this.mIndicesToLoad.size() - 1; size >= 0; size--) {
                    if (!this.mIndicesToLoad.valueAt(size)) {
                        this.mIndicesToLoad.removeAt(size);
                    }
                }
                int i3 = this.mMaxCount / 2;
                this.mPreloadLowerBound = i - i3;
                this.mPreloadUpperBound = i + i3;
                int iMin = Math.min(this.mPreloadUpperBound, i2 - 1);
                for (int iMax = Math.max(0, this.mPreloadLowerBound); iMax <= iMin; iMax++) {
                    if (this.mIndexRemoteViews.indexOfKey(iMax) < 0 && !this.mIndicesToLoad.get(iMax)) {
                        this.mIndicesToLoad.put(iMax, false);
                    }
                }
            }
            return true;
        }

        public int getNextIndexToLoad() {
            synchronized (this.mIndicesToLoad) {
                int iIndexOfValue = this.mIndicesToLoad.indexOfValue(true);
                if (iIndexOfValue < 0) {
                    iIndexOfValue = this.mIndicesToLoad.indexOfValue(false);
                }
                if (iIndexOfValue < 0) {
                    return -1;
                }
                int iKeyAt = this.mIndicesToLoad.keyAt(iIndexOfValue);
                this.mIndicesToLoad.removeAt(iIndexOfValue);
                return iKeyAt;
            }
        }

        public boolean containsRemoteViewAt(int i) {
            return this.mIndexRemoteViews.indexOfKey(i) >= 0;
        }

        public boolean containsMetaDataAt(int i) {
            return this.mIndexMetaData.indexOfKey(i) >= 0;
        }

        public void reset() {
            this.mPreloadLowerBound = 0;
            this.mPreloadUpperBound = -1;
            this.mLastRequestedIndex = -1;
            this.mIndexRemoteViews.clear();
            this.mIndexMetaData.clear();
            synchronized (this.mIndicesToLoad) {
                this.mIndicesToLoad.clear();
            }
        }
    }

    static class RemoteViewsCacheKey {
        final Intent.FilterComparison filter;
        final int widgetId;

        RemoteViewsCacheKey(Intent.FilterComparison filterComparison, int i) {
            this.filter = filterComparison;
            this.widgetId = i;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof RemoteViewsCacheKey)) {
                return false;
            }
            RemoteViewsCacheKey remoteViewsCacheKey = (RemoteViewsCacheKey) obj;
            return remoteViewsCacheKey.filter.equals(this.filter) && remoteViewsCacheKey.widgetId == this.widgetId;
        }

        public int hashCode() {
            return (this.filter == null ? 0 : this.filter.hashCode()) ^ (this.widgetId << 2);
        }
    }

    public RemoteViewsAdapter(Context context, Intent intent, RemoteAdapterConnectionCallback remoteAdapterConnectionCallback, boolean z) {
        this.mDataReady = false;
        this.mContext = context;
        this.mIntent = intent;
        if (this.mIntent == null) {
            throw new IllegalArgumentException("Non-null Intent must be specified.");
        }
        this.mAppWidgetId = intent.getIntExtra("remoteAdapterAppWidgetId", -1);
        this.mRequestedViews = new RemoteViewsFrameLayoutRefSet();
        if (intent.hasExtra("remoteAdapterAppWidgetId")) {
            intent.removeExtra("remoteAdapterAppWidgetId");
        }
        this.mWorkerThread = new HandlerThread("RemoteViewsCache-loader");
        this.mWorkerThread.start();
        this.mMainHandler = new Handler(Looper.myLooper(), this);
        this.mServiceHandler = new RemoteServiceHandler(this.mWorkerThread.getLooper(), this, context.getApplicationContext());
        this.mAsyncViewLoadExecutor = z ? new HandlerThreadExecutor(this.mWorkerThread) : null;
        this.mCallback = remoteAdapterConnectionCallback;
        if (sCacheRemovalThread == null) {
            sCacheRemovalThread = new HandlerThread("RemoteViewsAdapter-cachePruner");
            sCacheRemovalThread.start();
            sCacheRemovalQueue = new Handler(sCacheRemovalThread.getLooper());
        }
        RemoteViewsCacheKey remoteViewsCacheKey = new RemoteViewsCacheKey(new Intent.FilterComparison(this.mIntent), this.mAppWidgetId);
        synchronized (sCachedRemoteViewsCaches) {
            if (sCachedRemoteViewsCaches.containsKey(remoteViewsCacheKey)) {
                this.mCache = sCachedRemoteViewsCaches.get(remoteViewsCacheKey);
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
            this.mServiceHandler.unbindNow();
            this.mWorkerThread.quit();
        } finally {
            super.finalize();
        }
    }

    public boolean isDataReady() {
        return this.mDataReady;
    }

    public void setRemoteViewsOnClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mRemoteViewsOnClickHandler = onClickHandler;
    }

    public void saveRemoteViewsCache() {
        int i;
        int size;
        final RemoteViewsCacheKey remoteViewsCacheKey = new RemoteViewsCacheKey(new Intent.FilterComparison(this.mIntent), this.mAppWidgetId);
        synchronized (sCachedRemoteViewsCaches) {
            if (sRemoteViewsCacheRemoveRunnables.containsKey(remoteViewsCacheKey)) {
                sCacheRemovalQueue.removeCallbacks(sRemoteViewsCacheRemoveRunnables.get(remoteViewsCacheKey));
                sRemoteViewsCacheRemoveRunnables.remove(remoteViewsCacheKey);
            }
            synchronized (this.mCache.mMetaData) {
                i = this.mCache.mMetaData.count;
            }
            synchronized (this.mCache) {
                size = this.mCache.mIndexRemoteViews.size();
            }
            if (i > 0 && size > 0) {
                sCachedRemoteViewsCaches.put(remoteViewsCacheKey, this.mCache);
            }
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    RemoteViewsAdapter.lambda$saveRemoteViewsCache$0(remoteViewsCacheKey);
                }
            };
            sRemoteViewsCacheRemoveRunnables.put(remoteViewsCacheKey, runnable);
            sCacheRemovalQueue.postDelayed(runnable, TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
        }
    }

    static void lambda$saveRemoteViewsCache$0(RemoteViewsCacheKey remoteViewsCacheKey) {
        synchronized (sCachedRemoteViewsCaches) {
            if (sCachedRemoteViewsCaches.containsKey(remoteViewsCacheKey)) {
                sCachedRemoteViewsCaches.remove(remoteViewsCacheKey);
            }
            if (sRemoteViewsCacheRemoveRunnables.containsKey(remoteViewsCacheKey)) {
                sRemoteViewsCacheRemoveRunnables.remove(remoteViewsCacheKey);
            }
        }
    }

    private void updateTemporaryMetaData(IRemoteViewsFactory iRemoteViewsFactory) {
        RemoteViews viewAt;
        try {
            boolean zHasStableIds = iRemoteViewsFactory.hasStableIds();
            int viewTypeCount = iRemoteViewsFactory.getViewTypeCount();
            int count = iRemoteViewsFactory.getCount();
            LoadingViewTemplate loadingViewTemplate = new LoadingViewTemplate(iRemoteViewsFactory.getLoadingView(), this.mContext);
            if (count > 0 && loadingViewTemplate.remoteViews == null && (viewAt = iRemoteViewsFactory.getViewAt(0)) != null) {
                loadingViewTemplate.loadFirstViewHeight(viewAt, this.mContext, new HandlerThreadExecutor(this.mWorkerThread));
            }
            RemoteViewsMetaData temporaryMetaData = this.mCache.getTemporaryMetaData();
            synchronized (temporaryMetaData) {
                temporaryMetaData.hasStableIds = zHasStableIds;
                temporaryMetaData.viewTypeCount = viewTypeCount + 1;
                temporaryMetaData.count = count;
                temporaryMetaData.loadingTemplate = loadingViewTemplate;
            }
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Error in updateMetaData: " + e.getMessage());
            synchronized (this.mCache.getMetaData()) {
                this.mCache.getMetaData().reset();
                synchronized (this.mCache) {
                    this.mCache.reset();
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    private void updateRemoteViews(IRemoteViewsFactory iRemoteViewsFactory, int i, boolean z) {
        boolean zIsViewTypeInRange;
        int i2;
        try {
            RemoteViews viewAt = iRemoteViewsFactory.getViewAt(i);
            long itemId = iRemoteViewsFactory.getItemId(i);
            if (viewAt == null) {
                throw new RuntimeException("Null remoteViews");
            }
            if (viewAt.mApplication != null) {
                if (this.mLastRemoteViewAppInfo != null && viewAt.hasSameAppInfo(this.mLastRemoteViewAppInfo)) {
                    viewAt.mApplication = this.mLastRemoteViewAppInfo;
                } else {
                    this.mLastRemoteViewAppInfo = viewAt.mApplication;
                }
            }
            int layoutId = viewAt.getLayoutId();
            RemoteViewsMetaData metaData = this.mCache.getMetaData();
            synchronized (metaData) {
                zIsViewTypeInRange = metaData.isViewTypeInRange(layoutId);
                i2 = this.mCache.mMetaData.count;
            }
            synchronized (this.mCache) {
                try {
                    if (zIsViewTypeInRange) {
                        this.mCache.insert(i, viewAt, itemId, getVisibleWindow(i2));
                        if (z) {
                            Message.obtain(this.mMainHandler, 5, i, 0, viewAt).sendToTarget();
                        }
                    } else {
                        Log.e(TAG, "Error: widget's RemoteViewsFactory returns more view types than  indicated by getViewTypeCount() ");
                    }
                } finally {
                }
            }
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Error in updateRemoteViews(" + i + "): " + e.getMessage());
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
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        synchronized (this.mCache) {
            if (this.mCache.containsMetaDataAt(i)) {
                return this.mCache.getMetaDataAt(i).itemId;
            }
            return 0L;
        }
    }

    @Override
    public int getItemViewType(int i) {
        int mappedViewType;
        synchronized (this.mCache) {
            if (this.mCache.containsMetaDataAt(i)) {
                int i2 = this.mCache.getMetaDataAt(i).typeId;
                RemoteViewsMetaData metaData = this.mCache.getMetaData();
                synchronized (metaData) {
                    mappedViewType = metaData.getMappedViewType(i2);
                }
                return mappedViewType;
            }
            return 0;
        }
    }

    public void setVisibleRangeHint(int i, int i2) {
        this.mVisibleWindowLowerBound = i;
        this.mVisibleWindowUpperBound = i2;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        boolean zQueuePositionsToBePreloadedFromRequestedPosition;
        RemoteViewsFrameLayout remoteViewsFrameLayout;
        synchronized (this.mCache) {
            RemoteViews remoteViewsAt = this.mCache.getRemoteViewsAt(i);
            boolean z = remoteViewsAt != null;
            if (view != null && (view instanceof RemoteViewsFrameLayout)) {
                this.mRequestedViews.removeView((RemoteViewsFrameLayout) view);
            }
            if (!z) {
                requestBindService();
                zQueuePositionsToBePreloadedFromRequestedPosition = false;
            } else {
                zQueuePositionsToBePreloadedFromRequestedPosition = this.mCache.queuePositionsToBePreloadedFromRequestedPosition(i);
            }
            if (view instanceof RemoteViewsFrameLayout) {
                remoteViewsFrameLayout = (RemoteViewsFrameLayout) view;
            } else {
                remoteViewsFrameLayout = new RemoteViewsFrameLayout(viewGroup.getContext(), this.mCache);
                remoteViewsFrameLayout.setExecutor(this.mAsyncViewLoadExecutor);
            }
            if (z) {
                remoteViewsFrameLayout.onRemoteViewsLoaded(remoteViewsAt, this.mRemoteViewsOnClickHandler, false);
                if (zQueuePositionsToBePreloadedFromRequestedPosition) {
                    this.mServiceHandler.sendEmptyMessage(3);
                }
            } else {
                remoteViewsFrameLayout.onRemoteViewsLoaded(this.mCache.getMetaData().getLoadingTemplate(this.mContext).remoteViews, this.mRemoteViewsOnClickHandler, false);
                this.mRequestedViews.add(i, remoteViewsFrameLayout);
                this.mCache.queueRequestedPositionToLoad(i);
                this.mServiceHandler.sendEmptyMessage(3);
            }
        }
        return remoteViewsFrameLayout;
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

    private int[] getVisibleWindow(int i) {
        int i2 = this.mVisibleWindowLowerBound;
        int i3 = this.mVisibleWindowUpperBound;
        int i4 = 0;
        if ((i2 == 0 && i3 == 0) || i2 < 0 || i3 < 0) {
            return new int[0];
        }
        if (i2 <= i3) {
            int[] iArr = new int[(i3 + 1) - i2];
            while (i2 <= i3) {
                iArr[i4] = i2;
                i2++;
                i4++;
            }
            return iArr;
        }
        int iMax = Math.max(i, i2);
        int[] iArr2 = new int[(iMax - i2) + i3 + 1];
        int i5 = 0;
        while (i4 <= i3) {
            iArr2[i5] = i4;
            i4++;
            i5++;
        }
        while (i2 < iMax) {
            iArr2[i5] = i2;
            i2++;
            i5++;
        }
        return iArr2;
    }

    @Override
    public void notifyDataSetChanged() {
        this.mServiceHandler.removeMessages(4);
        this.mServiceHandler.sendEmptyMessage(2);
    }

    void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 1:
                this.mCache.commitTemporaryMetaData();
                break;
            case 2:
                superNotifyDataSetChanged();
                break;
            case 3:
                if (this.mCallback != null) {
                    this.mCallback.onRemoteAdapterConnected();
                }
                break;
            case 4:
                if (this.mCallback != null) {
                    this.mCallback.onRemoteAdapterDisconnected();
                }
                break;
            case 5:
                this.mRequestedViews.notifyOnRemoteViewsLoaded(message.arg1, (RemoteViews) message.obj);
                break;
        }
        return true;
    }

    private void requestBindService() {
        this.mServiceHandler.removeMessages(4);
        Message.obtain(this.mServiceHandler, 1, this.mAppWidgetId, 0, this.mIntent).sendToTarget();
    }

    private static class HandlerThreadExecutor implements Executor {
        private final HandlerThread mThread;

        HandlerThreadExecutor(HandlerThread handlerThread) {
            this.mThread = handlerThread;
        }

        @Override
        public void execute(Runnable runnable) {
            if (Thread.currentThread().getId() == this.mThread.getId()) {
                runnable.run();
            } else {
                new Handler(this.mThread.getLooper()).post(runnable);
            }
        }
    }

    private static class LoadingViewTemplate {
        public int defaultHeight;
        public final RemoteViews remoteViews;

        LoadingViewTemplate(RemoteViews remoteViews, Context context) {
            this.remoteViews = remoteViews;
            this.defaultHeight = Math.round(50.0f * context.getResources().getDisplayMetrics().density);
        }

        public void loadFirstViewHeight(RemoteViews remoteViews, Context context, Executor executor) {
            remoteViews.applyAsync(context, new RemoteViewsFrameLayout(context, null), executor, new RemoteViews.OnViewAppliedListener() {
                @Override
                public void onViewApplied(View view) {
                    try {
                        view.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
                        LoadingViewTemplate.this.defaultHeight = view.getMeasuredHeight();
                    } catch (Exception e) {
                        onError(e);
                    }
                }

                @Override
                public void onError(Exception exc) {
                    Log.w(RemoteViewsAdapter.TAG, "Error inflating first RemoteViews", exc);
                }
            });
        }
    }
}
