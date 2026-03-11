package androidx.slice;

import android.content.Context;
import android.net.Uri;
import androidx.slice.widget.SliceLiveData;
import java.util.Collection;
import java.util.Set;

class SliceViewManagerWrapper extends SliceViewManagerBase {
    private final android.app.slice.SliceManager mManager;
    private final Set<android.app.slice.SliceSpec> mSpecs;

    SliceViewManagerWrapper(Context context) {
        this(context, (android.app.slice.SliceManager) context.getSystemService(android.app.slice.SliceManager.class));
    }

    SliceViewManagerWrapper(Context context, android.app.slice.SliceManager manager) {
        super(context);
        this.mManager = manager;
        this.mSpecs = SliceConvert.unwrap(SliceLiveData.SUPPORTED_SPECS);
    }

    @Override
    public void pinSlice(Uri uri) {
        this.mManager.pinSlice(uri, this.mSpecs);
    }

    @Override
    public void unpinSlice(Uri uri) {
        this.mManager.unpinSlice(uri);
    }

    @Override
    public Slice bindSlice(Uri uri) {
        return SliceConvert.wrap(this.mManager.bindSlice(uri, this.mSpecs));
    }

    @Override
    public Collection<Uri> getSliceDescendants(Uri uri) {
        return this.mManager.getSliceDescendants(uri);
    }
}
