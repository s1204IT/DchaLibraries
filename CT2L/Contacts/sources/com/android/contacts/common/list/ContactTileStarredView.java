package com.android.contacts.common.list;

import android.content.Context;
import android.util.AttributeSet;
import com.android.contacts.common.ContactPhotoManager;

public class ContactTileStarredView extends ContactTileView {
    public ContactTileStarredView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean isDarkTheme() {
        return false;
    }

    @Override
    protected int getApproximateImageSize() {
        return this.mListener.getApproximateTileWidth();
    }

    @Override
    protected ContactPhotoManager.DefaultImageRequest getDefaultImageRequest(String displayName, String lookupKey) {
        return new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, 1, 0.8f, 0.0f, true);
    }
}
