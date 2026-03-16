package com.android.ex.chips;

public interface PhotoManager {

    public interface PhotoManagerCallback {
        void onPhotoBytesAsyncLoadFailed();

        void onPhotoBytesAsynchronouslyPopulated();

        void onPhotoBytesPopulated();
    }

    void populatePhotoBytesAsync(RecipientEntry recipientEntry, PhotoManagerCallback photoManagerCallback);
}
