package com.mediatek.gallerygif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.platform.PlatformHelper;
import com.mediatek.gallerybasic.util.BitmapUtils;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.gallerybasic.util.Utils;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class GifItem extends ExtItem {
    private static final String TAG = "MtkGallery2/GifItem";

    public GifItem(MediaData mediaData, Context context) {
        super(context, mediaData);
    }

    public GifItem(MediaData mediaData) {
        super(mediaData);
    }

    @Override
    public ExtItem.Thumbnail getThumbnail(ThumbType thumbType) throws Throwable {
        Bitmap bitmapDecodeGifThumbnail = null;
        if (PlatformHelper.isOutOfDecodeSpec(this.mMediaData.fileSize, this.mMediaData.width, this.mMediaData.height, this.mMediaData.mimeType)) {
            Log.d(TAG, "<getThumbnail> " + this.mMediaData.filePath + ", out of spec limit, abort generate thumbnail!");
            return new ExtItem.Thumbnail(null, false);
        }
        if (this.mMediaData.filePath != null && !this.mMediaData.filePath.equals("")) {
            bitmapDecodeGifThumbnail = decodeGifThumbnail(this.mMediaData.filePath);
        } else if (this.mMediaData.uri != null) {
            bitmapDecodeGifThumbnail = decodeGifThumbnail(this.mMediaData.uri);
        }
        return new ExtItem.Thumbnail(BitmapUtils.replaceBackgroundColor(bitmapDecodeGifThumbnail, true), true);
    }

    @Override
    public Bitmap decodeBitmap(BitmapFactory.Options options) {
        Bitmap bitmapDecodeBitmap = super.decodeBitmap(options);
        if (bitmapDecodeBitmap != null) {
            return BitmapUtils.replaceBackgroundColor(bitmapDecodeBitmap, true);
        }
        return bitmapDecodeBitmap;
    }

    @Override
    public ArrayList<ExtItem.SupportOperation> getNotSupportedOperations() {
        ArrayList<ExtItem.SupportOperation> arrayList = new ArrayList<>();
        arrayList.add(ExtItem.SupportOperation.FULL_IMAGE);
        arrayList.add(ExtItem.SupportOperation.EDIT);
        return arrayList;
    }

    private static Bitmap decodeGifThumbnail(String str) throws Throwable {
        GifDecoderWrapper gifDecoderWrapper = null;
        try {
            GifDecoderWrapper gifDecoderWrapperCreateGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(str);
            if (gifDecoderWrapperCreateGifDecoderWrapper != null) {
                try {
                    Bitmap frameBitmap = gifDecoderWrapperCreateGifDecoderWrapper.getFrameBitmap(0);
                    if (gifDecoderWrapperCreateGifDecoderWrapper != null) {
                        gifDecoderWrapperCreateGifDecoderWrapper.close();
                    }
                    return frameBitmap;
                } catch (Throwable th) {
                    gifDecoderWrapper = gifDecoderWrapperCreateGifDecoderWrapper;
                    th = th;
                    if (gifDecoderWrapper != null) {
                        gifDecoderWrapper.close();
                    }
                    throw th;
                }
            }
            if (gifDecoderWrapperCreateGifDecoderWrapper != null) {
                gifDecoderWrapperCreateGifDecoderWrapper.close();
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private Bitmap decodeGifThumbnail(Uri uri) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor;
        GifDecoderWrapper gifDecoderWrapper;
        GifDecoderWrapper gifDecoderWrapper2 = null;
        try {
            parcelFileDescriptorOpenFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(uri, "r");
            try {
                if (parcelFileDescriptorOpenFileDescriptor == null) {
                    Log.w(TAG, "<decodeGifThumbnail>, pdf is null");
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    return null;
                }
                FileDescriptor fileDescriptor = parcelFileDescriptorOpenFileDescriptor.getFileDescriptor();
                if (fileDescriptor == null) {
                    Log.w(TAG, "<decodeGifThumbnail>, fd is null");
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    return null;
                }
                GifDecoderWrapper gifDecoderWrapperCreateGifDecoderWrapper = GifDecoderWrapper.createGifDecoderWrapper(fileDescriptor);
                if (gifDecoderWrapperCreateGifDecoderWrapper == null) {
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    if (gifDecoderWrapperCreateGifDecoderWrapper != null) {
                        gifDecoderWrapperCreateGifDecoderWrapper.close();
                    }
                    return null;
                }
                try {
                    Bitmap frameBitmap = gifDecoderWrapperCreateGifDecoderWrapper.getFrameBitmap(0);
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    if (gifDecoderWrapperCreateGifDecoderWrapper != null) {
                        gifDecoderWrapperCreateGifDecoderWrapper.close();
                    }
                    return frameBitmap;
                } catch (FileNotFoundException e) {
                    gifDecoderWrapper = gifDecoderWrapperCreateGifDecoderWrapper;
                    e = e;
                    try {
                        Log.w(TAG, "<decodeGifThumbnail>, FileNotFoundException", e);
                        Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                        if (gifDecoderWrapper != null) {
                            gifDecoderWrapper.close();
                        }
                        return null;
                    } catch (Throwable th) {
                        th = th;
                        gifDecoderWrapper2 = gifDecoderWrapper;
                        Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                        if (gifDecoderWrapper2 != null) {
                            gifDecoderWrapper2.close();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    gifDecoderWrapper2 = gifDecoderWrapperCreateGifDecoderWrapper;
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    if (gifDecoderWrapper2 != null) {
                    }
                    throw th;
                }
            } catch (FileNotFoundException e2) {
                e = e2;
                gifDecoderWrapper = null;
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (FileNotFoundException e3) {
            e = e3;
            parcelFileDescriptorOpenFileDescriptor = null;
            gifDecoderWrapper = null;
        } catch (Throwable th4) {
            th = th4;
            parcelFileDescriptorOpenFileDescriptor = null;
        }
    }

    @Override
    public boolean supportHighQuality() {
        return false;
    }
}
