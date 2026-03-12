package com.android.systemui.recents.misc;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Utilities {
    private static Method sPropertyMethod;

    static {
        try {
            Class<?> c = Class.forName("android.view.GLES20Canvas");
            sPropertyMethod = c.getDeclaredMethod("setProperty", String.class, String.class);
            if (!sPropertyMethod.isAccessible()) {
                sPropertyMethod.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        }
    }

    public static void scaleRectAboutCenter(Rect r, float scale) {
        if (scale != 1.0f) {
            int cx = r.centerX();
            int cy = r.centerY();
            r.offset(-cx, -cy);
            r.left = (int) ((r.left * scale) + 0.5f);
            r.top = (int) ((r.top * scale) + 0.5f);
            r.right = (int) ((r.right * scale) + 0.5f);
            r.bottom = (int) ((r.bottom * scale) + 0.5f);
            r.offset(cx, cy);
        }
    }

    public static float mapCoordInDescendentToSelf(View descendant, View root, float[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<>();
        float[] pt = {coord[0], coord[1]};
        for (View v = descendant; v != root && v != null; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            if (v0 != descendant || includeRootScroll) {
                pt[0] = pt[0] - v0.getScrollX();
                pt[1] = pt[1] - v0.getScrollY();
            }
            v0.getMatrix().mapPoints(pt);
            pt[0] = pt[0] + v0.getLeft();
            pt[1] = pt[1] + v0.getTop();
            scale *= v0.getScaleX();
        }
        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    public static float mapCoordInSelfToDescendent(View descendant, View root, float[] coord, Matrix tmpInverseMatrix) {
        ArrayList<View> ancestorChain = new ArrayList<>();
        float[] pt = {coord[0], coord[1]};
        for (View v = descendant; v != root; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        int count = ancestorChain.size();
        tmpInverseMatrix.set(Matrix.IDENTITY_MATRIX);
        int i = count - 1;
        while (i >= 0) {
            View ancestor = ancestorChain.get(i);
            View next = i > 0 ? ancestorChain.get(i - 1) : null;
            pt[0] = pt[0] + ancestor.getScrollX();
            pt[1] = pt[1] + ancestor.getScrollY();
            if (next != null) {
                pt[0] = pt[0] - next.getLeft();
                pt[1] = pt[1] - next.getTop();
                next.getMatrix().invert(tmpInverseMatrix);
                tmpInverseMatrix.mapPoints(pt);
                scale *= next.getScaleX();
            }
            i--;
        }
        coord[0] = pt[0];
        coord[1] = pt[1];
        return scale;
    }

    public static float computeContrastBetweenColors(int bg, int fg) {
        float bgR = Color.red(bg) / 255.0f;
        float bgG = Color.green(bg) / 255.0f;
        float bgB = Color.blue(bg) / 255.0f;
        float bgL = (0.2126f * (bgR < 0.03928f ? bgR / 12.92f : (float) Math.pow((0.055f + bgR) / 1.055f, 2.4000000953674316d))) + (0.7152f * (bgG < 0.03928f ? bgG / 12.92f : (float) Math.pow((0.055f + bgG) / 1.055f, 2.4000000953674316d))) + (0.0722f * (bgB < 0.03928f ? bgB / 12.92f : (float) Math.pow((0.055f + bgB) / 1.055f, 2.4000000953674316d)));
        float fgR = Color.red(fg) / 255.0f;
        float fgG = Color.green(fg) / 255.0f;
        float fgB = Color.blue(fg) / 255.0f;
        float fgL = (0.2126f * (fgR < 0.03928f ? fgR / 12.92f : (float) Math.pow((0.055f + fgR) / 1.055f, 2.4000000953674316d))) + (0.7152f * (fgG < 0.03928f ? fgG / 12.92f : (float) Math.pow((0.055f + fgG) / 1.055f, 2.4000000953674316d))) + (0.0722f * (fgB < 0.03928f ? fgB / 12.92f : (float) Math.pow((0.055f + fgB) / 1.055f, 2.4000000953674316d)));
        return Math.abs((0.05f + fgL) / (0.05f + bgL));
    }

    public static int getColorWithOverlay(int baseColor, int overlayColor, float overlayAlpha) {
        return Color.rgb((int) ((Color.red(baseColor) * overlayAlpha) + ((1.0f - overlayAlpha) * Color.red(overlayColor))), (int) ((Color.green(baseColor) * overlayAlpha) + ((1.0f - overlayAlpha) * Color.green(overlayColor))), (int) ((Color.blue(baseColor) * overlayAlpha) + ((1.0f - overlayAlpha) * Color.blue(overlayColor))));
    }

    public static void setShadowProperty(String property, String value) throws IllegalAccessException, InvocationTargetException {
        sPropertyMethod.invoke(null, property, value);
    }

    public static void cancelAnimationWithoutCallbacks(Animator animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
    }
}
