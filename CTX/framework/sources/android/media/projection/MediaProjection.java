package android.media.projection;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioRecord;
import android.media.projection.IMediaProjectionCallback;
import android.os.Handler;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;
import java.util.Iterator;
import java.util.Map;

public final class MediaProjection {
    private static final String TAG = "MediaProjection";
    private final Map<Callback, CallbackRecord> mCallbacks = new ArrayMap();
    private final Context mContext;
    private final IMediaProjection mImpl;

    public MediaProjection(Context context, IMediaProjection iMediaProjection) {
        this.mContext = context;
        this.mImpl = iMediaProjection;
        try {
            this.mImpl.start(new MediaProjectionCallback());
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to start media projection", e);
        }
    }

    public void registerCallback(Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        this.mCallbacks.put(callback, new CallbackRecord(callback, handler));
    }

    public void unregisterCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null");
        }
        this.mCallbacks.remove(callback);
    }

    public VirtualDisplay createVirtualDisplay(String str, int i, int i2, int i3, boolean z, Surface surface, VirtualDisplay.Callback callback, Handler handler) {
        return ((DisplayManager) this.mContext.getSystemService(Context.DISPLAY_SERVICE)).createVirtualDisplay(this, str, i, i2, i3, surface, (z ? 4 : 0) | 16 | 2, callback, handler, null);
    }

    public VirtualDisplay createVirtualDisplay(String str, int i, int i2, int i3, int i4, Surface surface, VirtualDisplay.Callback callback, Handler handler) {
        return ((DisplayManager) this.mContext.getSystemService(Context.DISPLAY_SERVICE)).createVirtualDisplay(this, str, i, i2, i3, surface, i4, callback, handler, null);
    }

    public AudioRecord createAudioRecord(int i, int i2, int i3, int i4) {
        return null;
    }

    public void stop() {
        try {
            this.mImpl.stop();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to stop projection", e);
        }
    }

    public IMediaProjection getProjection() {
        return this.mImpl;
    }

    public static abstract class Callback {
        public void onStop() {
        }
    }

    private final class MediaProjectionCallback extends IMediaProjectionCallback.Stub {
        private MediaProjectionCallback() {
        }

        @Override
        public void onStop() {
            Iterator it = MediaProjection.this.mCallbacks.values().iterator();
            while (it.hasNext()) {
                ((CallbackRecord) it.next()).onStop();
            }
        }
    }

    private static final class CallbackRecord {
        private final Callback mCallback;
        private final Handler mHandler;

        public CallbackRecord(Callback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public void onStop() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CallbackRecord.this.mCallback.onStop();
                }
            });
        }
    }
}
