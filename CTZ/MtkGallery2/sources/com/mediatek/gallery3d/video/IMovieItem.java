package com.mediatek.gallery3d.video;

import android.net.Uri;

public interface IMovieItem {
    boolean canBeRetrieved();

    boolean canShare();

    long getBuckedId();

    int getCurId();

    String getDisplayName();

    String getMimeType();

    String getTitle();

    Uri getUri();

    String getVideoPath();

    int getVideoType();

    boolean isDrm();

    void setMimeType(String str);

    void setTitle(String str);

    void setUri(Uri uri);

    void setVideoType(int i);
}
