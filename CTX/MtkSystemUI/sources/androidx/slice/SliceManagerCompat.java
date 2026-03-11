package androidx.slice;

import android.content.Context;
import android.net.Uri;
import androidx.slice.compat.SliceProviderCompat;
import java.util.List;

class SliceManagerCompat extends SliceManager {
    private final Context mContext;

    SliceManagerCompat(Context context) {
        this.mContext = context;
    }

    @Override
    public List<Uri> getPinnedSlices() {
        return SliceProviderCompat.getPinnedSlices(this.mContext);
    }
}
