package androidx.slice;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Pair;
import androidx.slice.SliceViewManager;
import androidx.slice.widget.SliceLiveData;
import java.util.concurrent.Executor;

public abstract class SliceViewManagerBase extends SliceViewManager {
    protected final Context mContext;
    private final ArrayMap<Pair<Uri, SliceViewManager.SliceCallback>, SliceListenerImpl> mListenerLookup = new ArrayMap<>();

    SliceViewManagerBase(Context context) {
        this.mContext = context;
    }

    @Override
    public void registerSliceCallback(Uri uri, SliceViewManager.SliceCallback callback) {
        final Handler h = new Handler(Looper.getMainLooper());
        registerSliceCallback(uri, new Executor() {
            @Override
            public void execute(Runnable command) {
                h.post(command);
            }
        }, callback);
    }

    public void registerSliceCallback(Uri uri, Executor executor, SliceViewManager.SliceCallback callback) {
        getListener(uri, callback, new SliceListenerImpl(uri, executor, callback)).startListening();
    }

    @Override
    public void unregisterSliceCallback(Uri uri, SliceViewManager.SliceCallback callback) {
        synchronized (this.mListenerLookup) {
            SliceListenerImpl impl = this.mListenerLookup.remove(new Pair(uri, callback));
            if (impl != null) {
                impl.stopListening();
            }
        }
    }

    private SliceListenerImpl getListener(Uri uri, SliceViewManager.SliceCallback callback, SliceListenerImpl listener) {
        Pair<Uri, SliceViewManager.SliceCallback> key = new Pair<>(uri, callback);
        synchronized (this.mListenerLookup) {
            if (this.mListenerLookup.containsKey(key)) {
                this.mListenerLookup.get(key).stopListening();
            }
            this.mListenerLookup.put(key, listener);
        }
        return listener;
    }

    private class SliceListenerImpl {
        private final SliceViewManager.SliceCallback mCallback;
        private final Executor mExecutor;
        private boolean mPinned;
        private Uri mUri;
        private final Runnable mUpdateSlice = new Runnable() {
            @Override
            public void run() {
                SliceListenerImpl.this.tryPin();
                final Slice s = Slice.bindSlice(SliceViewManagerBase.this.mContext, SliceListenerImpl.this.mUri, SliceLiveData.SUPPORTED_SPECS);
                SliceListenerImpl.this.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        SliceListenerImpl.this.mCallback.onSliceUpdated(s);
                    }
                });
            }
        };
        private final ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                AsyncTask.execute(SliceListenerImpl.this.mUpdateSlice);
            }
        };

        SliceListenerImpl(Uri uri, Executor executor, SliceViewManager.SliceCallback callback) {
            this.mUri = uri;
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        void startListening() {
            SliceViewManagerBase.this.mContext.getContentResolver().registerContentObserver(this.mUri, true, this.mObserver);
            tryPin();
        }

        private void tryPin() {
            if (!this.mPinned) {
                try {
                    SliceViewManagerBase.this.pinSlice(this.mUri);
                    this.mPinned = true;
                } catch (SecurityException e) {
                }
            }
        }

        void stopListening() {
            SliceViewManagerBase.this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            if (this.mPinned) {
                SliceViewManagerBase.this.unpinSlice(this.mUri);
                this.mPinned = false;
            }
        }
    }
}
