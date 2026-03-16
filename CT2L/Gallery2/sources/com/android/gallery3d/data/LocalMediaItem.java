package com.android.gallery3d.data;

import android.database.Cursor;
import com.android.gallery3d.util.GalleryUtils;
import java.text.DateFormat;
import java.util.Date;

public abstract class LocalMediaItem extends MediaItem {
    public int bucketId;
    public String caption;
    public long dateAddedInSec;
    public long dateModifiedInSec;
    public long dateTakenInMs;
    public String filePath;
    public long fileSize;
    public int height;
    public int id;
    public double latitude;
    public double longitude;
    public String mimeType;
    public int width;

    protected abstract boolean updateFromCursor(Cursor cursor);

    public LocalMediaItem(Path path, long version) {
        super(path, version);
        this.latitude = 0.0d;
        this.longitude = 0.0d;
    }

    @Override
    public long getDateInMs() {
        return this.dateTakenInMs;
    }

    @Override
    public String getName() {
        return this.caption;
    }

    @Override
    public void getLatLong(double[] latLong) {
        latLong[0] = this.latitude;
        latLong[1] = this.longitude;
    }

    public int getBucketId() {
        return this.bucketId;
    }

    protected void updateContent(Cursor cursor) {
        if (updateFromCursor(cursor)) {
            this.mDataVersion = nextVersionNumber();
        }
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(200, this.filePath);
        details.addDetail(1, this.caption);
        DateFormat formater = DateFormat.getDateTimeInstance();
        details.addDetail(3, formater.format(new Date(this.dateModifiedInSec * 1000)));
        details.addDetail(5, Integer.valueOf(this.width));
        details.addDetail(6, Integer.valueOf(this.height));
        if (GalleryUtils.isValidLocation(this.latitude, this.longitude)) {
            details.addDetail(4, new double[]{this.latitude, this.longitude});
        }
        if (this.fileSize > 0) {
            details.addDetail(10, Long.valueOf(this.fileSize));
        }
        return details;
    }

    @Override
    public String getMimeType() {
        return this.mimeType;
    }

    @Override
    public long getSize() {
        return this.fileSize;
    }
}
