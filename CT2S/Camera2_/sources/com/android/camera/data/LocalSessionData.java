package com.android.camera.data;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.android.camera.Storage;
import com.android.camera.data.LocalData;
import com.android.camera2.R;
import com.bumptech.glide.Glide;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LocalSessionData implements LocalData {
    private int mHeight;
    private final Uri mUri;
    private int mWidth;
    protected final Bundle mMetaData = new Bundle();
    private final long mDateTaken = new Date().getTime();

    public LocalSessionData(Uri uri) {
        this.mUri = uri;
        refreshSize(uri);
    }

    private void refreshSize(Uri uri) {
        Point size = Storage.getSizeForSession(uri);
        this.mWidth = size.x;
        this.mHeight = size.y;
    }

    @Override
    public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeholderResourcedId, LocalDataAdapter adapter, boolean isInProgress, LocalData.ActionCallback actionCallback) {
        ImageView imageView;
        if (recycled != null) {
            imageView = (ImageView) recycled;
        } else {
            imageView = new ImageView(context);
            imageView.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
        }
        byte[] jpegData = Storage.getJpegForSession(this.mUri);
        int currentVersion = Storage.getJpegVersionForSession(this.mUri);
        Glide.with(context).loadFromImage(jpegData, this.mUri.toString() + currentVersion).skipDiskCache(true).fitCenter().into(imageView);
        imageView.setContentDescription(context.getResources().getString(R.string.media_processing_content_description));
        return imageView;
    }

    @Override
    public LocalDataViewType getItemViewType() {
        return LocalDataViewType.SESSION;
    }

    @Override
    public void loadFullImage(Context context, int width, int height, View view, LocalDataAdapter adapter) {
    }

    @Override
    public long getDateTaken() {
        return this.mDateTaken;
    }

    @Override
    public long getDateModified() {
        return TimeUnit.MILLISECONDS.toSeconds(this.mDateTaken);
    }

    @Override
    public String getTitle() {
        return this.mUri.toString();
    }

    @Override
    public boolean isDataActionSupported(int actions) {
        return false;
    }

    @Override
    public boolean delete(Context c) {
        return false;
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return true;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        return null;
    }

    @Override
    public int getLocalDataType() {
        return 5;
    }

    @Override
    public long getSizeInBytes() {
        return 0L;
    }

    @Override
    public LocalData refresh(Context context) {
        refreshSize(this.mUri);
        return this;
    }

    @Override
    public long getContentId() {
        return 0L;
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

    @Override
    public int getRotation() {
        return 0;
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
    public int getViewType() {
        return 2;
    }

    @Override
    public double[] getLatLong() {
        return null;
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void recycle(View view) {
        Glide.clear(view);
    }

    @Override
    public Uri getUri() {
        return this.mUri;
    }
}
