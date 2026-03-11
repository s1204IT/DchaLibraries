package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class ThumbnailData {
    public Rect insets;
    public boolean isRealSnapshot;
    public boolean isTranslucent;
    public int orientation;
    public boolean reducedResolution;
    public float scale;
    public int systemUiVisibility;
    public final Bitmap thumbnail;
    public int windowingMode;

    public ThumbnailData() {
        this.thumbnail = null;
        this.orientation = 0;
        this.insets = new Rect();
        this.reducedResolution = false;
        this.scale = 1.0f;
        this.isRealSnapshot = true;
        this.isTranslucent = false;
        this.windowingMode = 0;
        this.systemUiVisibility = 0;
    }

    public ThumbnailData(ActivityManager.TaskSnapshot snapshot) {
        this.thumbnail = Bitmap.createHardwareBitmap(snapshot.getSnapshot());
        this.insets = new Rect(snapshot.getContentInsets());
        this.orientation = snapshot.getOrientation();
        this.reducedResolution = snapshot.isReducedResolution();
        this.scale = snapshot.getScale();
        this.isRealSnapshot = snapshot.isRealSnapshot();
        this.isTranslucent = snapshot.isTranslucent();
        this.windowingMode = snapshot.getWindowingMode();
        this.systemUiVisibility = snapshot.getSystemUiVisibility();
    }
}
