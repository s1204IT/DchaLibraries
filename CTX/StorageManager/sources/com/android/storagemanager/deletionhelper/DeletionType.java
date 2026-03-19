package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.os.Bundle;

public interface DeletionType {

    public interface FreeableChangedListener {
        void onFreeableChanged(int i, long j);
    }

    void clearFreeableData(Activity activity);

    int getContentCount();

    int getLoadingStatus();

    void onPause();

    void onResume();

    void onSaveInstanceStateBundle(Bundle bundle);

    void registerFreeableChangedListener(FreeableChangedListener freeableChangedListener);

    void setLoadingStatus(int i);

    default boolean isEmpty() {
        return getLoadingStatus() == 2;
    }

    default void updateLoadingStatus() {
        if (getContentCount() == 0) {
            setLoadingStatus(2);
        } else {
            setLoadingStatus(1);
        }
    }
}
