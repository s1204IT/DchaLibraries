package androidx.slice.widget;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.ArraySet;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import androidx.slice.SliceViewManager;
import java.util.Arrays;
import java.util.Set;

public final class SliceLiveData {
    public static final SliceSpec OLD_BASIC = new SliceSpec("androidx.app.slice.BASIC", 1);
    public static final SliceSpec OLD_LIST = new SliceSpec("androidx.app.slice.LIST", 1);
    public static final Set<SliceSpec> SUPPORTED_SPECS = new ArraySet(Arrays.asList(SliceSpecs.BASIC, SliceSpecs.LIST, OLD_BASIC, OLD_LIST));

    public static LiveData<Slice> fromUri(Context context, Uri uri) {
        return new SliceLiveDataImpl(context.getApplicationContext(), uri);
    }

    private static class SliceLiveDataImpl extends LiveData<Slice> {
        private final Intent mIntent;
        private final SliceViewManager.SliceCallback mSliceCallback;
        private final SliceViewManager mSliceViewManager;
        private final Runnable mUpdateSlice;
        private Uri mUri;

        private SliceLiveDataImpl(Context context, Uri uri) {
            this.mUpdateSlice = new Runnable() {
                @Override
                public void run() {
                    Slice s = SliceLiveDataImpl.this.mUri != null ? SliceLiveDataImpl.this.mSliceViewManager.bindSlice(SliceLiveDataImpl.this.mUri) : SliceLiveDataImpl.this.mSliceViewManager.bindSlice(SliceLiveDataImpl.this.mIntent);
                    if (SliceLiveDataImpl.this.mUri == null && s != null) {
                        SliceLiveDataImpl.this.mUri = s.getUri();
                        SliceLiveDataImpl.this.mSliceViewManager.registerSliceCallback(SliceLiveDataImpl.this.mUri, SliceLiveDataImpl.this.mSliceCallback);
                    }
                    SliceLiveDataImpl.this.postValue(s);
                }
            };
            this.mSliceCallback = new SliceViewManager.SliceCallback() {
                @Override
                public void onSliceUpdated(Slice s) {
                    SliceLiveDataImpl.this.postValue(s);
                }
            };
            this.mSliceViewManager = SliceViewManager.getInstance(context);
            this.mUri = uri;
            this.mIntent = null;
        }

        @Override
        protected void onActive() {
            AsyncTask.execute(this.mUpdateSlice);
            if (this.mUri != null) {
                this.mSliceViewManager.registerSliceCallback(this.mUri, this.mSliceCallback);
            }
        }

        @Override
        protected void onInactive() {
            if (this.mUri != null) {
                this.mSliceViewManager.unregisterSliceCallback(this.mUri, this.mSliceCallback);
            }
        }
    }
}
