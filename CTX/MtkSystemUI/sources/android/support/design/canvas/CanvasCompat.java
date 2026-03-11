package android.support.design.canvas;

import android.graphics.Canvas;
import android.os.Build;

public class CanvasCompat {
    public static int saveLayerAlpha(Canvas canvas, float left, float top, float right, float bottom, int alpha) {
        if (Build.VERSION.SDK_INT > 21) {
            return canvas.saveLayerAlpha(left, top, right, bottom, alpha);
        }
        return canvas.saveLayerAlpha(left, top, right, bottom, alpha, 31);
    }
}
