package androidx.slice;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.os.BuildCompat;

public abstract class SliceViewManager {

    public interface SliceCallback {
        void onSliceUpdated(Slice slice);
    }

    public abstract Slice bindSlice(Intent intent);

    public abstract Slice bindSlice(Uri uri);

    public abstract void pinSlice(Uri uri);

    public abstract void registerSliceCallback(Uri uri, SliceCallback sliceCallback);

    public abstract void unpinSlice(Uri uri);

    public abstract void unregisterSliceCallback(Uri uri, SliceCallback sliceCallback);

    public static SliceViewManager getInstance(Context context) {
        if (BuildCompat.isAtLeastP()) {
            return new SliceViewManagerWrapper(context);
        }
        return new SliceViewManagerCompat(context);
    }

    SliceViewManager() {
    }
}
