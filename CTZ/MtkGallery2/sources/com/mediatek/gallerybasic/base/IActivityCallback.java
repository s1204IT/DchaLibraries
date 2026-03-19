package com.mediatek.gallerybasic.base;

import android.app.Activity;

public interface IActivityCallback {
    void onCreate(Activity activity);

    void onDestroy(Activity activity);

    void onPause(Activity activity);

    void onResume(Activity activity);

    void onStart(Activity activity);

    void onStop(Activity activity);
}
