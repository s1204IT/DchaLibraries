package androidx.slice;

import android.content.Context;
import android.net.Uri;
import android.support.v4.os.BuildCompat;
import java.util.List;

public abstract class SliceManager {
    public abstract List<Uri> getPinnedSlices();

    public static SliceManager getInstance(Context context) {
        if (BuildCompat.isAtLeastP()) {
            return new SliceManagerWrapper(context);
        }
        return new SliceManagerCompat(context);
    }

    SliceManager() {
    }
}
