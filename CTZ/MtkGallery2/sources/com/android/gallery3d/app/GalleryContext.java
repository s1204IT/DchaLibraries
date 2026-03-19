package com.android.gallery3d.app;

import android.content.Context;
import com.android.gallery3d.util.ThreadPool;

public interface GalleryContext {
    Context getAndroidContext();

    ThreadPool getThreadPool();
}
