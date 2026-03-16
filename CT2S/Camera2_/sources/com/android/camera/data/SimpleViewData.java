package com.android.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.android.camera.data.LocalData;
import com.android.camera.debug.Log;
import java.util.UUID;

public class SimpleViewData implements LocalData {
    private static final String SIMPLE_VIEW_URI_SCHEME = "simple_view_data";
    private static final Log.Tag TAG = new Log.Tag("SimpleViewData");
    private final long mDateModified;
    private final long mDateTaken;
    private final int mHeight;
    private final LocalDataViewType mItemViewType;
    private final Bundle mMetaData = new Bundle();
    private final Uri mUri;
    private final View mView;
    private final int mWidth;

    public SimpleViewData(View v, LocalDataViewType viewType, int width, int height, int dateTaken, int dateModified) {
        this.mView = v;
        this.mItemViewType = viewType;
        this.mWidth = width;
        this.mHeight = height;
        this.mDateTaken = dateTaken;
        this.mDateModified = dateModified;
        Uri.Builder builder = new Uri.Builder();
        String uuid = UUID.randomUUID().toString();
        builder.scheme(SIMPLE_VIEW_URI_SCHEME).appendPath(uuid);
        this.mUri = builder.build();
    }

    @Override
    public long getDateTaken() {
        return this.mDateTaken;
    }

    @Override
    public long getDateModified() {
        return this.mDateModified;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public int getViewType() {
        return 2;
    }

    @Override
    public LocalDataViewType getItemViewType() {
        return this.mItemViewType;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public Uri getUri() {
        return this.mUri;
    }

    @Override
    public int getLocalDataType() {
        return 2;
    }

    @Override
    public LocalData refresh(Context context) {
        return this;
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public boolean isDataActionSupported(int action) {
        return false;
    }

    @Override
    public boolean delete(Context c) {
        return false;
    }

    @Override
    public View getView(Context context, View recycled, int width, int height, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgressSession, LocalData.ActionCallback actionCallback) {
        return this.mView;
    }

    @Override
    public void loadFullImage(Context context, int w, int h, View view, LocalDataAdapter adapter) {
    }

    @Override
    public void prepare() {
    }

    @Override
    public void recycle(View view) {
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return true;
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        return null;
    }

    @Override
    public double[] getLatLong() {
        return null;
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public long getSizeInBytes() {
        return 0L;
    }

    @Override
    public long getContentId() {
        return -1L;
    }

    @Override
    public Bundle getMetadata() {
        return this.mMetaData;
    }

    @Override
    public String getSignature() {
        return "";
    }

    @Override
    public boolean isMetadataUpdated() {
        return true;
    }
}
