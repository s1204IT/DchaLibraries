package android.widget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.internal.widget.IRemoteViewsFactory;
import java.util.HashMap;

public abstract class RemoteViewsService extends Service {
    private static final String LOG_TAG = "RemoteViewsService";
    private static final HashMap<Intent.FilterComparison, RemoteViewsFactory> sRemoteViewFactories = new HashMap<>();
    private static final Object sLock = new Object();

    public interface RemoteViewsFactory {
        int getCount();

        long getItemId(int i);

        RemoteViews getLoadingView();

        RemoteViews getViewAt(int i);

        int getViewTypeCount();

        boolean hasStableIds();

        void onCreate();

        void onDataSetChanged();

        void onDestroy();
    }

    public abstract RemoteViewsFactory onGetViewFactory(Intent intent);

    private static class RemoteViewsFactoryAdapter extends IRemoteViewsFactory.Stub {
        private RemoteViewsFactory mFactory;
        private boolean mIsCreated;

        public RemoteViewsFactoryAdapter(RemoteViewsFactory remoteViewsFactory, boolean z) {
            this.mFactory = remoteViewsFactory;
            this.mIsCreated = z;
        }

        @Override
        public synchronized boolean isCreated() {
            return this.mIsCreated;
        }

        @Override
        public synchronized void onDataSetChanged() {
            try {
                this.mFactory.onDataSetChanged();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }

        @Override
        public synchronized void onDataSetChangedAsync() {
            onDataSetChanged();
        }

        @Override
        public synchronized int getCount() {
            int count;
            count = 0;
            try {
                count = this.mFactory.getCount();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
            return count;
        }

        @Override
        public synchronized RemoteViews getViewAt(int i) {
            RemoteViews viewAt;
            RemoteViews remoteViews = null;
            try {
                viewAt = this.mFactory.getViewAt(i);
                if (viewAt != null) {
                    try {
                        viewAt.setIsWidgetCollectionChild(true);
                    } catch (Exception e) {
                        remoteViews = viewAt;
                        e = e;
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                        viewAt = remoteViews;
                    }
                }
            } catch (Exception e2) {
                e = e2;
            }
            return viewAt;
        }

        @Override
        public synchronized RemoteViews getLoadingView() {
            RemoteViews loadingView;
            loadingView = null;
            try {
                loadingView = this.mFactory.getLoadingView();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
            return loadingView;
        }

        @Override
        public synchronized int getViewTypeCount() {
            int viewTypeCount;
            viewTypeCount = 0;
            try {
                viewTypeCount = this.mFactory.getViewTypeCount();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
            return viewTypeCount;
        }

        @Override
        public synchronized long getItemId(int i) {
            long itemId;
            itemId = 0;
            try {
                itemId = this.mFactory.getItemId(i);
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
            return itemId;
        }

        @Override
        public synchronized boolean hasStableIds() {
            boolean zHasStableIds;
            zHasStableIds = false;
            try {
                zHasStableIds = this.mFactory.hasStableIds();
            } catch (Exception e) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
            return zHasStableIds;
        }

        @Override
        public void onDestroy(Intent intent) {
            synchronized (RemoteViewsService.sLock) {
                Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
                if (RemoteViewsService.sRemoteViewFactories.containsKey(filterComparison)) {
                    try {
                        ((RemoteViewsFactory) RemoteViewsService.sRemoteViewFactories.get(filterComparison)).onDestroy();
                    } catch (Exception e) {
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                    }
                    RemoteViewsService.sRemoteViewFactories.remove(filterComparison);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        RemoteViewsFactory remoteViewsFactoryOnGetViewFactory;
        boolean z;
        RemoteViewsFactoryAdapter remoteViewsFactoryAdapter;
        synchronized (sLock) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            if (!sRemoteViewFactories.containsKey(filterComparison)) {
                remoteViewsFactoryOnGetViewFactory = onGetViewFactory(intent);
                sRemoteViewFactories.put(filterComparison, remoteViewsFactoryOnGetViewFactory);
                remoteViewsFactoryOnGetViewFactory.onCreate();
                z = false;
            } else {
                remoteViewsFactoryOnGetViewFactory = sRemoteViewFactories.get(filterComparison);
                z = true;
            }
            remoteViewsFactoryAdapter = new RemoteViewsFactoryAdapter(remoteViewsFactoryOnGetViewFactory, z);
        }
        return remoteViewsFactoryAdapter;
    }
}
