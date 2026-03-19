package com.mediatek.galleryfeature.pq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.mediatek.gallerybasic.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class BitmapDecoder {
    private static final float SCALE_THRESHOLD = 0.5f;
    private static final String TAG = "MtkGallery2/BitmapDecoder";
    private Context mContext;
    private BitmapFactory.Options mOptions = new BitmapFactory.Options();
    private String mPqUri;
    private int mSampleSize;
    private int mTargetSize;

    public BitmapDecoder(Context context, String str, int i) {
        this.mPqUri = str;
        this.mContext = context;
        this.mTargetSize = i;
        this.mSampleSize = PQUtils.calculateInSampleSize(context, str, i);
    }

    public Bitmap decodeScreenNailBitmap() throws Throwable {
        Throwable th;
        FileInputStream fileInputStream;
        this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        BitmapFactory.Options options = this.mOptions;
        ?? r1 = this.mSampleSize;
        options.inSampleSize = r1;
        PQUtils.initOptions(this.mOptions);
        Bitmap bitmapDecodeFileDescriptor = null;
        try {
            try {
                fileInputStream = PQUtils.getFileInputStream(this.mContext, this.mPqUri);
                if (fileInputStream != null) {
                    try {
                        FileDescriptor fd = fileInputStream.getFD();
                        if (fd != null) {
                            bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fd, null, this.mOptions);
                        }
                    } catch (IOException e) {
                        e = e;
                        Log.w(TAG, "<decodeScreenNailBitmap> exception occur, " + e.getMessage());
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                if (bitmapDecodeFileDescriptor != null) {
                    float fMax = this.mTargetSize / Math.max(bitmapDecodeFileDescriptor.getWidth(), bitmapDecodeFileDescriptor.getHeight());
                    if (fMax <= SCALE_THRESHOLD) {
                        return PQUtils.resizeBitmapByScale(bitmapDecodeFileDescriptor, fMax, true);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (r1 != 0) {
                    try {
                        r1.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            e = e5;
            fileInputStream = null;
        } catch (Throwable th3) {
            r1 = 0;
            th = th3;
            if (r1 != 0) {
            }
            throw th;
        }
        return bitmapDecodeFileDescriptor;
    }
}
