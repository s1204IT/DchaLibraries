package android.support.v4.view;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import java.util.WeakHashMap;

public class ViewCompat {
    static final ViewCompatImpl IMPL;

    interface ViewCompatImpl {
        boolean canScrollHorizontally(View view, int i);

        boolean getFitsSystemWindows(View view);

        int getImportantForAccessibility(View view);

        int getLayerType(View view);

        int getLayoutDirection(View view);

        int getOverScrollMode(View view);

        boolean isOpaque(View view);

        void postInvalidateOnAnimation(View view);

        void postInvalidateOnAnimation(View view, int i, int i2, int i3, int i4);

        void postOnAnimation(View view, Runnable runnable);

        void setAccessibilityDelegate(View view, AccessibilityDelegateCompat accessibilityDelegateCompat);

        void setImportantForAccessibility(View view, int i);

        void setLayerPaint(View view, Paint paint);

        void setLayerType(View view, int i, Paint paint);

        void setSaveFromParentEnabled(View view, boolean z);
    }

    static class BaseViewCompatImpl implements ViewCompatImpl {
        WeakHashMap mViewPropertyAnimatorCompatMap = null;

        BaseViewCompatImpl() {
        }

        @Override
        public boolean canScrollHorizontally(View v, int direction) {
            return false;
        }

        @Override
        public int getOverScrollMode(View v) {
            return 2;
        }

        @Override
        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        }

        @Override
        public void postInvalidateOnAnimation(View view) {
            view.invalidate();
        }

        @Override
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            view.invalidate(left, top, right, bottom);
        }

        @Override
        public void postOnAnimation(View view, Runnable action) {
            view.postDelayed(action, getFrameTime());
        }

        long getFrameTime() {
            return 10L;
        }

        @Override
        public int getImportantForAccessibility(View view) {
            return 0;
        }

        @Override
        public void setImportantForAccessibility(View view, int mode) {
        }

        @Override
        public void setLayerType(View view, int layerType, Paint paint) {
        }

        @Override
        public int getLayerType(View view) {
            return 0;
        }

        @Override
        public void setLayerPaint(View view, Paint p) {
        }

        @Override
        public int getLayoutDirection(View view) {
            return 0;
        }

        @Override
        public boolean isOpaque(View view) {
            Drawable bg = view.getBackground();
            return bg != null && bg.getOpacity() == -1;
        }

        @Override
        public boolean getFitsSystemWindows(View view) {
            return false;
        }

        @Override
        public void setSaveFromParentEnabled(View v, boolean enabled) {
        }
    }

    static class EclairMr1ViewCompatImpl extends BaseViewCompatImpl {
        EclairMr1ViewCompatImpl() {
        }

        @Override
        public boolean isOpaque(View view) {
            return ViewCompatEclairMr1.isOpaque(view);
        }
    }

    static class GBViewCompatImpl extends EclairMr1ViewCompatImpl {
        GBViewCompatImpl() {
        }

        @Override
        public int getOverScrollMode(View v) {
            return ViewCompatGingerbread.getOverScrollMode(v);
        }
    }

    static class HCViewCompatImpl extends GBViewCompatImpl {
        HCViewCompatImpl() {
        }

        @Override
        long getFrameTime() {
            return ViewCompatHC.getFrameTime();
        }

        @Override
        public void setLayerType(View view, int layerType, Paint paint) {
            ViewCompatHC.setLayerType(view, layerType, paint);
        }

        @Override
        public int getLayerType(View view) {
            return ViewCompatHC.getLayerType(view);
        }

        @Override
        public void setLayerPaint(View view, Paint paint) {
            setLayerType(view, getLayerType(view), paint);
            view.invalidate();
        }

        @Override
        public void setSaveFromParentEnabled(View view, boolean enabled) {
            ViewCompatHC.setSaveFromParentEnabled(view, enabled);
        }
    }

    static class ICSViewCompatImpl extends HCViewCompatImpl {
        static boolean accessibilityDelegateCheckFailed = false;

        ICSViewCompatImpl() {
        }

        @Override
        public boolean canScrollHorizontally(View v, int direction) {
            return ViewCompatICS.canScrollHorizontally(v, direction);
        }

        @Override
        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
            ViewCompatICS.setAccessibilityDelegate(v, delegate == null ? null : delegate.getBridge());
        }
    }

    static class JBViewCompatImpl extends ICSViewCompatImpl {
        JBViewCompatImpl() {
        }

        @Override
        public void postInvalidateOnAnimation(View view) {
            ViewCompatJB.postInvalidateOnAnimation(view);
        }

        @Override
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            ViewCompatJB.postInvalidateOnAnimation(view, left, top, right, bottom);
        }

        @Override
        public void postOnAnimation(View view, Runnable action) {
            ViewCompatJB.postOnAnimation(view, action);
        }

        @Override
        public int getImportantForAccessibility(View view) {
            return ViewCompatJB.getImportantForAccessibility(view);
        }

        @Override
        public void setImportantForAccessibility(View view, int mode) {
            if (mode == 4) {
                mode = 2;
            }
            ViewCompatJB.setImportantForAccessibility(view, mode);
        }

        @Override
        public boolean getFitsSystemWindows(View view) {
            return ViewCompatJB.getFitsSystemWindows(view);
        }
    }

    static class JbMr1ViewCompatImpl extends JBViewCompatImpl {
        JbMr1ViewCompatImpl() {
        }

        @Override
        public void setLayerPaint(View view, Paint paint) {
            ViewCompatJellybeanMr1.setLayerPaint(view, paint);
        }

        @Override
        public int getLayoutDirection(View view) {
            return ViewCompatJellybeanMr1.getLayoutDirection(view);
        }
    }

    static class KitKatViewCompatImpl extends JbMr1ViewCompatImpl {
        KitKatViewCompatImpl() {
        }

        @Override
        public void setImportantForAccessibility(View view, int mode) {
            ViewCompatJB.setImportantForAccessibility(view, mode);
        }
    }

    static class Api21ViewCompatImpl extends KitKatViewCompatImpl {
        Api21ViewCompatImpl() {
        }
    }

    static {
        int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            IMPL = new Api21ViewCompatImpl();
            return;
        }
        if (version >= 19) {
            IMPL = new KitKatViewCompatImpl();
            return;
        }
        if (version >= 17) {
            IMPL = new JbMr1ViewCompatImpl();
            return;
        }
        if (version >= 16) {
            IMPL = new JBViewCompatImpl();
            return;
        }
        if (version >= 14) {
            IMPL = new ICSViewCompatImpl();
            return;
        }
        if (version >= 11) {
            IMPL = new HCViewCompatImpl();
            return;
        }
        if (version >= 9) {
            IMPL = new GBViewCompatImpl();
        } else if (version >= 7) {
            IMPL = new EclairMr1ViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    public static boolean canScrollHorizontally(View v, int direction) {
        return IMPL.canScrollHorizontally(v, direction);
    }

    public static int getOverScrollMode(View v) {
        return IMPL.getOverScrollMode(v);
    }

    public static void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        IMPL.setAccessibilityDelegate(v, delegate);
    }

    public static void postInvalidateOnAnimation(View view) {
        IMPL.postInvalidateOnAnimation(view);
    }

    public static void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
        IMPL.postInvalidateOnAnimation(view, left, top, right, bottom);
    }

    public static void postOnAnimation(View view, Runnable action) {
        IMPL.postOnAnimation(view, action);
    }

    public static int getImportantForAccessibility(View view) {
        return IMPL.getImportantForAccessibility(view);
    }

    public static void setImportantForAccessibility(View view, int mode) {
        IMPL.setImportantForAccessibility(view, mode);
    }

    public static void setLayerType(View view, int layerType, Paint paint) {
        IMPL.setLayerType(view, layerType, paint);
    }

    public static int getLayerType(View view) {
        return IMPL.getLayerType(view);
    }

    public static void setLayerPaint(View view, Paint paint) {
        IMPL.setLayerPaint(view, paint);
    }

    public static int getLayoutDirection(View view) {
        return IMPL.getLayoutDirection(view);
    }

    public static boolean isOpaque(View view) {
        return IMPL.isOpaque(view);
    }

    public static boolean getFitsSystemWindows(View v) {
        return IMPL.getFitsSystemWindows(v);
    }

    public static void setSaveFromParentEnabled(View v, boolean enabled) {
        IMPL.setSaveFromParentEnabled(v, enabled);
    }
}
