package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class ImageFilterTinyPlanet extends SimpleImageFilter {
    private static final String LOGTAG = ImageFilterTinyPlanet.class.getSimpleName();
    FilterTinyPlanetRepresentation mParameters = new FilterTinyPlanetRepresentation();

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, Bitmap bitmap2, int i3, float f, float f2);

    public ImageFilterTinyPlanet() {
        this.mName = "TinyPlanet";
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterTinyPlanetRepresentation parameters = (FilterTinyPlanetRepresentation) representation;
        this.mParameters = parameters;
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterTinyPlanetRepresentation();
    }

    @Override
    public Bitmap apply(Bitmap bitmapIn, float scaleFactor, int quality) {
        XMPMeta xmp;
        int w = bitmapIn.getWidth();
        bitmapIn.getHeight();
        int outputSize = (int) (w / 2.0f);
        ImagePreset preset = getEnvironment().getImagePreset();
        Bitmap mBitmapOut = null;
        if (preset != null && (xmp = ImageLoader.getXmpObject(MasterImage.getImage().getActivity())) != null) {
            bitmapIn = applyXmp(bitmapIn, xmp, w);
        }
        if (0 != 0 && outputSize != mBitmapOut.getHeight()) {
            mBitmapOut = null;
        }
        while (mBitmapOut == null) {
            try {
                mBitmapOut = getEnvironment().getBitmap(outputSize, outputSize, 10);
            } catch (OutOfMemoryError e) {
                System.gc();
                outputSize /= 2;
                Log.v(LOGTAG, "No memory to create Full Tiny Planet create half");
            }
        }
        nativeApplyFilter(bitmapIn, bitmapIn.getWidth(), bitmapIn.getHeight(), mBitmapOut, outputSize, this.mParameters.getZoom() / 100.0f, this.mParameters.getAngle());
        return mBitmapOut;
    }

    private Bitmap applyXmp(Bitmap bitmapIn, XMPMeta xmp, int intermediateWidth) {
        int croppedAreaWidth;
        int croppedAreaHeight;
        int fullPanoWidth;
        int fullPanoHeight;
        int left;
        int top;
        try {
            croppedAreaWidth = getInt(xmp, "CroppedAreaImageWidthPixels");
            croppedAreaHeight = getInt(xmp, "CroppedAreaImageHeightPixels");
            fullPanoWidth = getInt(xmp, "FullPanoWidthPixels");
            fullPanoHeight = getInt(xmp, "FullPanoHeightPixels");
            left = getInt(xmp, "CroppedAreaLeftPixels");
            top = getInt(xmp, "CroppedAreaTopPixels");
        } catch (XMPException e) {
        }
        if (fullPanoWidth == 0 || fullPanoHeight == 0) {
            return bitmapIn;
        }
        Bitmap paddedBitmap = null;
        float scale = intermediateWidth / fullPanoWidth;
        while (paddedBitmap == null) {
            try {
                paddedBitmap = Bitmap.createBitmap((int) (fullPanoWidth * scale), (int) (fullPanoHeight * scale), Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e2) {
                System.gc();
                scale /= 2.0f;
            }
        }
        Canvas paddedCanvas = new Canvas(paddedBitmap);
        int right = left + croppedAreaWidth;
        int bottom = top + croppedAreaHeight;
        RectF destRect = new RectF(left * scale, top * scale, right * scale, bottom * scale);
        paddedCanvas.drawBitmap(bitmapIn, (Rect) null, destRect, (Paint) null);
        bitmapIn = paddedBitmap;
        return bitmapIn;
    }

    private static int getInt(XMPMeta xmp, String key) throws XMPException {
        if (xmp.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", key)) {
            return xmp.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", key).intValue();
        }
        return 0;
    }
}
