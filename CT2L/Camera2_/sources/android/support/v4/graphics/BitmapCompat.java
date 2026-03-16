package android.support.v4.graphics;

import android.graphics.Bitmap;
import android.os.Build;

public class BitmapCompat {
    static final BitmapImpl IMPL;

    interface BitmapImpl {
        int getAllocationByteCount(Bitmap bitmap);

        boolean hasMipMap(Bitmap bitmap);

        void setHasMipMap(Bitmap bitmap, boolean z);
    }

    static class BaseBitmapImpl implements BitmapImpl {
        BaseBitmapImpl() {
        }

        @Override
        public boolean hasMipMap(Bitmap bitmap) {
            return false;
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
        }

        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }

    static class HcMr1BitmapCompatImpl extends BaseBitmapImpl {
        HcMr1BitmapCompatImpl() {
        }

        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return BitmapCompatHoneycombMr1.getAllocationByteCount(bitmap);
        }
    }

    static class JbMr2BitmapCompatImpl extends HcMr1BitmapCompatImpl {
        JbMr2BitmapCompatImpl() {
        }

        @Override
        public boolean hasMipMap(Bitmap bitmap) {
            return BitmapCompatJellybeanMR2.hasMipMap(bitmap);
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
            BitmapCompatJellybeanMR2.setHasMipMap(bitmap, hasMipMap);
        }
    }

    static class KitKatBitmapCompatImpl extends JbMr2BitmapCompatImpl {
        KitKatBitmapCompatImpl() {
        }

        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return BitmapCompatKitKat.getAllocationByteCount(bitmap);
        }
    }

    static {
        int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            IMPL = new KitKatBitmapCompatImpl();
            return;
        }
        if (version >= 18) {
            IMPL = new JbMr2BitmapCompatImpl();
        } else if (version >= 12) {
            IMPL = new HcMr1BitmapCompatImpl();
        } else {
            IMPL = new BaseBitmapImpl();
        }
    }

    public static boolean hasMipMap(Bitmap bitmap) {
        return IMPL.hasMipMap(bitmap);
    }

    public static void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
        IMPL.setHasMipMap(bitmap, hasMipMap);
    }

    public static int getAllocationByteCount(Bitmap bitmap) {
        return IMPL.getAllocationByteCount(bitmap);
    }
}
