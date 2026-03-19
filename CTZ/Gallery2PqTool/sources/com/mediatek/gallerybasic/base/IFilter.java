package com.mediatek.gallerybasic.base;

import android.content.Intent;

public interface IFilter {
    String getDeleteWhereClauseForImage(int i, int i2);

    String getDeleteWhereClauseForVideo(int i, int i2);

    String getWhereClause(int i, int i2);

    String getWhereClauseForImage(int i, int i2);

    String getWhereClauseForVideo(int i, int i2);

    void setDefaultFlag(MediaFilter mediaFilter);

    void setFlagFromIntent(Intent intent, MediaFilter mediaFilter);
}
