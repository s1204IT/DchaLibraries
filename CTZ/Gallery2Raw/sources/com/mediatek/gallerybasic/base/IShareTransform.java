package com.mediatek.gallerybasic.base;

import android.app.Activity;
import android.net.Uri;
import java.util.ArrayList;

public interface IShareTransform {
    void onStartTransform(Activity activity);

    void onStopTransform(Activity activity);

    ArrayList<Uri> onTransform(Activity activity, MediaData[] mediaDataArr);
}
