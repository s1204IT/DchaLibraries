package com.mediatek.gallery3d.adapter;

import android.database.Cursor;
import android.provider.MediaStore;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.UriImage;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.IDataParserCallback;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.Utils;

public class MediaDataParser {
    public static MediaData parseLocalImageMediaData(Cursor cursor) {
        MediaData mediaData = new MediaData();
        mediaData.width = cursor.getInt(12);
        mediaData.height = cursor.getInt(13);
        mediaData.orientation = cursor.getInt(9);
        mediaData.caption = cursor.getString(1);
        mediaData.mimeType = cursor.getString(2);
        mediaData.filePath = cursor.getString(8);
        mediaData.bucketId = cursor.getInt(10);
        mediaData.id = cursor.getLong(0);
        mediaData.fileSize = cursor.getLong(11);
        mediaData.dateModifiedInSec = cursor.getLong(7);
        mediaData.uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(mediaData.id)).build();
        mediaData.extFileds = new ExtFields(cursor, true);
        for (IDataParserCallback iDataParserCallback : (IDataParserCallback[]) FeatureManager.getInstance().getImplement(IDataParserCallback.class, new Object[0])) {
            if (iDataParserCallback != null) {
                iDataParserCallback.onPostParse(mediaData);
            }
        }
        return mediaData;
    }

    public static MediaData parseLocalVideoMediaData(LocalVideo localVideo, Cursor cursor) {
        MediaData mediaData = new MediaData();
        mediaData.width = localVideo.width;
        mediaData.height = localVideo.height;
        mediaData.mimeType = cursor.getString(2);
        mediaData.filePath = cursor.getString(8);
        mediaData.bucketId = cursor.getInt(10);
        mediaData.isVideo = true;
        mediaData.duration = cursor.getInt(9);
        mediaData.caption = cursor.getString(1);
        mediaData.dateModifiedInSec = cursor.getLong(7);
        mediaData.id = cursor.getLong(0);
        mediaData.uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(mediaData.id)).build();
        mediaData.extFileds = new ExtFields(cursor, false);
        return mediaData;
    }

    public static MediaData parseUriImageMediaData(UriImage uriImage) {
        MediaData mediaData = new MediaData();
        mediaData.mimeType = uriImage.getMimeType();
        mediaData.uri = uriImage.getContentUri();
        if (mediaData.uri != null && "file".equals(mediaData.uri.getScheme())) {
            if (Utils.hasSpecialCharaters(mediaData.uri)) {
                mediaData.filePath = mediaData.uri.toString().substring(7);
            } else {
                mediaData.filePath = mediaData.uri.getPath();
            }
        }
        mediaData.width = uriImage.getWidth();
        mediaData.height = uriImage.getHeight();
        mediaData.orientation = uriImage.getRotation();
        mediaData.extFileds = new ExtFields();
        return mediaData;
    }
}
