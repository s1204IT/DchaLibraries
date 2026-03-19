package com.mediatek.gallerybasic.base;

import android.net.Uri;

public class MediaData {
    private static final String TAG = "MtkGallery2/MediaData";
    public int bucketId;
    public long dateModifiedInSec;
    public int duration;
    public ExtFields extFileds;
    public long fileSize;
    public int height;
    public long id;
    public int orientation;
    public Uri uri;
    public int width;
    public MediaType mediaType = new MediaType();
    public String caption = "";
    public String mimeType = "";
    public String filePath = "";
    public boolean isVideo = false;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[mediaType = " + this.mediaType + ",");
        sb.append("width = " + this.width + ",");
        sb.append("height = " + this.height + ",");
        sb.append("orientation = " + this.orientation + ",");
        sb.append("caption = " + this.caption + ",");
        sb.append("mimeType = " + this.mimeType + ",");
        sb.append("filePath = " + this.filePath + ",");
        sb.append("uri = " + this.uri + ",");
        sb.append("isVideo = " + this.isVideo + ",");
        sb.append("bucketId = " + this.bucketId + ",");
        sb.append("id = " + this.id + ",");
        sb.append("fileSize = " + this.fileSize + ",");
        sb.append("duration = " + this.duration + ",");
        sb.append("dateModifiedInSec = " + this.dateModifiedInSec + ", ");
        return sb.toString();
    }

    public boolean equals(MediaData mediaData) {
        if (mediaData == null) {
            return false;
        }
        if (this == mediaData) {
            return true;
        }
        if ((this.mediaType == null && mediaData.mediaType != null) || ((this.mediaType != null && mediaData.mediaType == null) || ((this.mediaType != null && !this.mediaType.equals(mediaData.mediaType)) || this.width != mediaData.width || this.height != mediaData.height || this.orientation != mediaData.orientation || ((this.caption == null && mediaData.caption != null) || ((this.caption != null && mediaData.caption == null) || ((this.caption != null && !this.caption.equals(mediaData.caption)) || ((this.mimeType == null && mediaData.mimeType != null) || ((this.mimeType != null && mediaData.mimeType == null) || ((this.mimeType != null && !this.mimeType.equals(mediaData.mimeType)) || ((this.filePath == null && mediaData.filePath != null) || ((this.filePath != null && mediaData.filePath == null) || ((this.filePath != null && !this.filePath.equals(mediaData.filePath)) || ((this.uri == null && mediaData.uri != null) || ((this.uri != null && mediaData.uri == null) || ((this.uri != null && !this.uri.equals(mediaData.uri)) || this.isVideo != mediaData.isVideo || this.bucketId != mediaData.bucketId || this.id != mediaData.id || this.fileSize != mediaData.fileSize || this.duration != mediaData.duration || this.dateModifiedInSec != mediaData.dateModifiedInSec))))))))))))))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (31 * (((((((((((((((((((((((((527 + (this.mediaType == null ? 0 : this.mediaType.hashCode())) * 31) + this.width) * 31) + this.height) * 31) + this.orientation) * 31) + (this.caption == null ? 0 : this.caption.hashCode())) * 31) + (this.mimeType == null ? 0 : this.mimeType.hashCode())) * 31) + (this.filePath == null ? 0 : this.filePath.hashCode())) * 31) + (this.uri != null ? this.uri.hashCode() : 0)) * 31) + (!this.isVideo ? 1 : 0)) * 31) + this.bucketId) * 31) + ((int) (this.id ^ (this.id >>> 32)))) * 31) + ((int) (this.fileSize ^ (this.fileSize >>> 32)))) * 31) + this.duration)) + ((int) (this.dateModifiedInSec ^ (this.dateModifiedInSec >>> 32)));
    }
}
