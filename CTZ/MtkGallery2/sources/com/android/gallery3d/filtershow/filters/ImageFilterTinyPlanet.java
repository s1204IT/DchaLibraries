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

public class ImageFilterTinyPlanet extends SimpleImageFilter {
    private static final String LOGTAG = ImageFilterTinyPlanet.class.getSimpleName();
    FilterTinyPlanetRepresentation mParameters = new FilterTinyPlanetRepresentation();

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, Bitmap bitmap2, int i3, float f, float f2);

    public ImageFilterTinyPlanet() {
        this.mName = "TinyPlanet";
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterTinyPlanetRepresentation) filterRepresentation;
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterTinyPlanetRepresentation();
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        XMPMeta xmpObject;
        int width = bitmap.getWidth();
        bitmap.getHeight();
        int i2 = (int) (width / 2.0f);
        if (getEnvironment().getImagePreset() != null && (xmpObject = ImageLoader.getXmpObject(MasterImage.getImage().getActivity())) != null) {
            bitmap = applyXmp(bitmap, xmpObject, width);
        }
        Bitmap bitmap2 = bitmap;
        Bitmap bitmap3 = null;
        int i3 = i2;
        while (bitmap3 == null) {
            try {
                bitmap3 = getEnvironment().getBitmap(i3, i3, 10);
            } catch (OutOfMemoryError e) {
                System.gc();
                i3 /= 2;
                Log.v(LOGTAG, "No memory to create Full Tiny Planet create half");
            }
        }
        nativeApplyFilter(bitmap2, bitmap2.getWidth(), bitmap2.getHeight(), bitmap3, i3, this.mParameters.getZoom() / 100.0f, this.mParameters.getAngle());
        return bitmap3;
    }

    private Bitmap applyXmp(Bitmap bitmap, XMPMeta xMPMeta, int i) {
        try {
            int i2 = getInt(xMPMeta, "CroppedAreaImageWidthPixels");
            int i3 = getInt(xMPMeta, "CroppedAreaImageHeightPixels");
            int i4 = getInt(xMPMeta, "FullPanoWidthPixels");
            int i5 = getInt(xMPMeta, "FullPanoHeightPixels");
            int i6 = getInt(xMPMeta, "CroppedAreaLeftPixels");
            int i7 = getInt(xMPMeta, "CroppedAreaTopPixels");
            if (i4 == 0 || i5 == 0) {
                return bitmap;
            }
            float f = i4;
            float f2 = i / f;
            Bitmap bitmapCreateBitmap = null;
            while (bitmapCreateBitmap == null) {
                try {
                    bitmapCreateBitmap = Bitmap.createBitmap((int) (f * f2), (int) (i5 * f2), Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    System.gc();
                    f2 /= 2.0f;
                }
            }
            new Canvas(bitmapCreateBitmap).drawBitmap(bitmap, (Rect) null, new RectF(i6 * f2, i7 * f2, (i2 + i6) * f2, (i3 + i7) * f2), (Paint) null);
            return bitmapCreateBitmap;
        } catch (XMPException e2) {
            return bitmap;
        }
    }

    private static int getInt(XMPMeta xMPMeta, String str) throws XMPException {
        if (xMPMeta.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", str)) {
            return xMPMeta.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", str).intValue();
        }
        return 0;
    }
}
