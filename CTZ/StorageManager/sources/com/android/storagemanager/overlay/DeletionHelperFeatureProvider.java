package com.android.storagemanager.overlay;

import android.content.Context;
import com.android.storagemanager.deletionhelper.DeletionType;

public interface DeletionHelperFeatureProvider {
    DeletionType createPhotoVideoDeletionType(Context context, int i);

    int getDaysToKeep(int i);
}
