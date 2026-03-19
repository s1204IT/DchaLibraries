package com.mediatek.galleryfeature.pq;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class TiledBitmapDecoder {
    private static final float MIN_SCALE_LIMIT = 0.01f;
    private static final int SCALE_LIMIT = 4;
    private static final String TAG = "MtkGallery2/TiledBitmapDecoder";
    private static final int TILE_BORDER = 1;
    private static final int TILE_SIZE = 255;
    private static final int TILE_SIZE_HEIGHT_RESOLUTION = 511;
    public static int sViewHeight;
    public static int sViewWidth;
    private Context mContext;
    private BitmapRegionDecoder mDecoder;
    private int mLevel;
    private int mLevelCount;
    private int mOriginalImageHeight;
    private int mOriginalImageWidth;
    private int mTileSize;
    private String mUri;
    public static final boolean DEBUG_PQTOOL = SystemPropertyUtils.get("Debug_PQTool").equals("1");
    private static Object sLock = new Object();

    public class Tile {
        Bitmap mTile;
        public int x;
        public int y;

        public Tile(int i, int i2, Bitmap bitmap) {
            this.mTile = null;
            this.x = i;
            this.y = i2;
            this.mTile = bitmap;
        }
    }

    public TiledBitmapDecoder(Context context, String str) throws Throwable {
        this.mUri = null;
        this.mDecoder = null;
        this.mContext = context;
        this.mUri = str;
        Bundle extras = ((Activity) context).getIntent().getExtras();
        if (extras != null) {
            extras.getString("PQUri");
        }
        if (PQUtils.isHighResolution(this.mContext)) {
            this.mTileSize = TILE_SIZE_HEIGHT_RESOLUTION;
        } else {
            this.mTileSize = TILE_SIZE;
        }
        Log.d(TAG, "<DecoderTiledBitmap> mTileSize=" + this.mTileSize);
        init();
        this.mLevelCount = PQUtils.calculateLevelCount(this.mOriginalImageWidth, PictureQualityActivity.sTargetWidth);
        this.mLevel = PQUtils.clamp(PQUtils.floorLog2(1.0f / getScaleMin()), 0, this.mLevelCount);
        Log.d(TAG, "<DecoderTiledBitmap> mLevel=" + this.mLevel + " mLevelCount=" + this.mLevelCount);
        synchronized (sLock) {
            this.mDecoder = getBitmapRegionDecoder(this.mUri);
        }
    }

    private void init() throws Throwable {
        FileInputStream fileInputStream;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        ?? r1 = 0;
        FileInputStream fileInputStream2 = null;
        FileInputStream fileInputStream3 = null;
        try {
            try {
                try {
                    fileInputStream = PQUtils.getFileInputStream(this.mContext, this.mUri);
                    if (fileInputStream != null) {
                        try {
                            FileDescriptor fd = fileInputStream.getFD();
                            if (fd != null) {
                                BitmapFactory.decodeFileDescriptor(fd, null, options);
                            }
                        } catch (FileNotFoundException e) {
                            fileInputStream2 = fileInputStream;
                            Log.e(TAG, "<init>bitmapfactory decodestream fail");
                            if (fileInputStream2 != null) {
                                fileInputStream2.close();
                            }
                            r1 = options.outWidth;
                            if (r1 <= 0) {
                                return;
                            } else {
                                return;
                            }
                        } catch (IOException e2) {
                            fileInputStream3 = fileInputStream;
                            Log.e(TAG, "<init>bitmapfactory decodestream fail");
                            if (fileInputStream3 != null) {
                                fileInputStream3.close();
                            }
                            r1 = options.outWidth;
                            if (r1 <= 0) {
                            }
                        } catch (Throwable th) {
                            th = th;
                            r1 = fileInputStream;
                            if (r1 != 0) {
                                try {
                                    r1.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (FileNotFoundException e4) {
            } catch (IOException e5) {
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e6) {
            e6.printStackTrace();
        }
        r1 = options.outWidth;
        if (r1 <= 0 || options.outHeight <= 0) {
            return;
        }
        this.mOriginalImageWidth = options.outWidth;
        this.mOriginalImageHeight = options.outHeight;
    }

    private float getScaleMin() {
        if (this.mOriginalImageWidth <= 0 || this.mOriginalImageHeight <= 0) {
            return 4.0f;
        }
        float fMin = Math.min(sViewWidth / this.mOriginalImageWidth, sViewHeight / this.mOriginalImageHeight);
        Log.d(TAG, " <getScaleMin>viewW=" + sViewWidth + " viewH=" + sViewHeight + " mOriginalImageWidth=" + this.mOriginalImageWidth + " mOriginalImageHeight=" + this.mOriginalImageHeight);
        return Math.max(Math.min(4.0f, fMin), MIN_SCALE_LIMIT);
    }

    private BitmapRegionDecoder getBitmapRegionDecoder(String e) throws Throwable {
        BitmapRegionDecoder bitmapRegionDecoder = null;
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                e = this.mContext.getContentResolver().openInputStream(Uri.parse(e));
                try {
                    BitmapRegionDecoder bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance((InputStream) e, false);
                    e = e;
                    if (e != 0) {
                        try {
                            e.close();
                            e = e;
                        } catch (IOException e2) {
                            e2.printStackTrace();
                            e = e2;
                        }
                    }
                    bitmapRegionDecoder = bitmapRegionDecoderNewInstance;
                } catch (FileNotFoundException e3) {
                    e = e3;
                    Log.d(TAG, "<getBitmapRegionDecoder>FileNotFoundException:" + e.toString());
                    e.printStackTrace();
                    if (e != 0) {
                        e.close();
                        e = e;
                    }
                } catch (IOException e4) {
                    e = e4;
                    Log.d(TAG, "<getBitmapRegionDecoder>IOException:" + e.toString());
                    e.printStackTrace();
                    if (e != 0) {
                        e.close();
                        e = e;
                    }
                }
            } catch (IOException e5) {
                e = e5;
                e.printStackTrace();
            }
        } catch (FileNotFoundException e6) {
            e = e6;
            e = 0;
        } catch (IOException e7) {
            e = e7;
            e = 0;
        } catch (Throwable th2) {
            th = th2;
            e = 0;
            if (e != 0) {
                try {
                    e.close();
                } catch (IOException e8) {
                    e8.printStackTrace();
                }
            }
            throw th;
        }
        return bitmapRegionDecoder;
    }

    public Bitmap decodeBitmap() {
        return decodeTileImage(1.0f, this.mLevel);
    }

    private Bitmap decodeTileImage(float f, int i) {
        synchronized (sLock) {
            if (this.mDecoder == null) {
                return null;
            }
            int width = this.mDecoder.getWidth();
            int height = this.mDecoder.getHeight();
            Log.d(TAG, "<decodeTileImage> scale =" + f);
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(((int) (((float) width) * f)) >> i, ((int) (((float) height) * f)) >> i, Bitmap.Config.ARGB_8888);
            drawInTiles(new Canvas(bitmapCreateBitmap), this.mDecoder, new Rect(0, 0, this.mDecoder.getWidth(), this.mDecoder.getHeight()), new Rect(0, 0, bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight()), i);
            return bitmapCreateBitmap;
        }
    }

    private void drawInTiles(Canvas canvas, BitmapRegionDecoder bitmapRegionDecoder, Rect rect, Rect rect2, int i) {
        int i2 = this.mTileSize << i;
        int i3 = 1 << i;
        Rect rect3 = new Rect();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        PQUtils.initOptions(options);
        options.inSampleSize = i3;
        Log.v(TAG, "<drawInTiles>sample = " + i);
        ArrayList arrayList = new ArrayList();
        int i4 = rect.left;
        boolean z = true;
        int i5 = 0;
        while (i4 < rect.right) {
            int i6 = rect.top;
            int i7 = 0;
            while (i6 < rect.bottom) {
                int i8 = i6 + i2;
                rect3.set(i4, i6, i4 + i2 + i3, i8 + i3);
                if (rect3.intersect(rect)) {
                    if (bitmapRegionDecoder != null) {
                        try {
                            if (!bitmapRegionDecoder.isRecycled()) {
                                arrayList.add(new Tile(i5, i7, bitmapRegionDecoder.decodeRegion(rect3, options)));
                            }
                        } catch (IllegalArgumentException e) {
                            Log.w(TAG, " <drawInTiles>drawInTiles:got exception:" + e);
                        }
                    }
                    z = false;
                    break;
                }
                i7 += this.mTileSize;
                i6 = i8;
            }
            i4 += i2;
            i5 += this.mTileSize;
        }
        if (z) {
            Paint paint = new Paint();
            paint.setColor(-1);
            for (int size = arrayList.size() - 1; size >= 0; size += -1) {
                Bitmap bitmap = ((Tile) arrayList.get(size)).mTile;
                int i9 = ((Tile) arrayList.get(size)).x;
                int i10 = ((Tile) arrayList.get(size)).y;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float f = i9;
                float f2 = i10;
                canvas.drawBitmap(bitmap, f, f2, (Paint) null);
                if (DEBUG_PQTOOL) {
                    float f3 = width + i9;
                    canvas.drawLine(f, f2, f3, f2, paint);
                    float f4 = height + i10;
                    canvas.drawLine(f3, f2, f3, f4, paint);
                    canvas.drawLine(f3, f4, f, f4, paint);
                    canvas.drawLine(f, f4, f, f2, paint);
                }
                Log.d(TAG, "<drawInTiles>pixelX=" + ((Tile) arrayList.get(size)).x + " pixelY=" + ((Tile) arrayList.get(size)).y);
                ((Tile) arrayList.get(size)).mTile.recycle();
            }
        }
    }

    public void recycle() {
        synchronized (sLock) {
            if (this.mDecoder != null) {
                this.mDecoder.recycle();
                this.mDecoder = null;
            }
        }
    }
}
