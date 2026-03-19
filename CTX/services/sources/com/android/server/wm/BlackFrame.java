package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;

public class BlackFrame {
    final boolean mForceDefaultOrientation;
    final Rect mInnerRect;
    final Rect mOuterRect;
    final Matrix mTmpMatrix = new Matrix();
    final float[] mTmpFloats = new float[9];
    final BlackSurface[] mBlackSurfaces = new BlackSurface[4];

    class BlackSurface {
        final int layer;
        final int left;
        final SurfaceControl surface;
        final int top;

        BlackSurface(SurfaceControl.Transaction transaction, int i, int i2, int i3, int i4, int i5, DisplayContent displayContent) throws Surface.OutOfResourcesException {
            this.left = i2;
            this.top = i3;
            this.layer = i;
            this.surface = displayContent.makeOverlay().setName("BlackSurface").setSize(i4 - i2, i5 - i3).setColorLayer(true).setParent(null).build();
            transaction.setAlpha(this.surface, 1.0f);
            transaction.setLayer(this.surface, i);
            transaction.show(this.surface);
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                Slog.i(WmsExt.TAG, "  BLACK " + this.surface + ": CREATE layer=" + i);
            }
        }

        void setAlpha(SurfaceControl.Transaction transaction, float f) {
            transaction.setAlpha(this.surface, f);
        }

        void setMatrix(SurfaceControl.Transaction transaction, Matrix matrix) {
            BlackFrame.this.mTmpMatrix.setTranslate(this.left, this.top);
            BlackFrame.this.mTmpMatrix.postConcat(matrix);
            BlackFrame.this.mTmpMatrix.getValues(BlackFrame.this.mTmpFloats);
            transaction.setPosition(this.surface, BlackFrame.this.mTmpFloats[2], BlackFrame.this.mTmpFloats[5]);
            transaction.setMatrix(this.surface, BlackFrame.this.mTmpFloats[0], BlackFrame.this.mTmpFloats[3], BlackFrame.this.mTmpFloats[1], BlackFrame.this.mTmpFloats[4]);
        }

        void clearMatrix(SurfaceControl.Transaction transaction) {
            transaction.setMatrix(this.surface, 1.0f, 0.0f, 0.0f, 1.0f);
        }
    }

    public void printTo(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("Outer: ");
        this.mOuterRect.printShortString(printWriter);
        printWriter.print(" / Inner: ");
        this.mInnerRect.printShortString(printWriter);
        printWriter.println();
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            BlackSurface blackSurface = this.mBlackSurfaces[i];
            printWriter.print(str);
            printWriter.print("#");
            printWriter.print(i);
            printWriter.print(": ");
            printWriter.print(blackSurface.surface);
            printWriter.print(" left=");
            printWriter.print(blackSurface.left);
            printWriter.print(" top=");
            printWriter.println(blackSurface.top);
        }
    }

    public BlackFrame(SurfaceControl.Transaction transaction, Rect rect, Rect rect2, int i, DisplayContent displayContent, boolean z) throws Surface.OutOfResourcesException {
        this.mForceDefaultOrientation = z;
        this.mOuterRect = new Rect(rect);
        this.mInnerRect = new Rect(rect2);
        try {
            if (rect.top < rect2.top) {
                this.mBlackSurfaces[0] = new BlackSurface(transaction, i, rect.left, rect.top, rect2.right, rect2.top, displayContent);
            }
            if (rect.left < rect2.left) {
                this.mBlackSurfaces[1] = new BlackSurface(transaction, i, rect.left, rect2.top, rect2.left, rect.bottom, displayContent);
            }
            if (rect.bottom > rect2.bottom) {
                this.mBlackSurfaces[2] = new BlackSurface(transaction, i, rect2.left, rect2.bottom, rect.right, rect.bottom, displayContent);
            }
            if (rect.right > rect2.right) {
                this.mBlackSurfaces[3] = new BlackSurface(transaction, i, rect2.right, rect.top, rect.right, rect2.bottom, displayContent);
            }
        } catch (Throwable th) {
            kill();
            throw th;
        }
    }

    public void kill() {
        if (this.mBlackSurfaces != null) {
            for (int i = 0; i < this.mBlackSurfaces.length; i++) {
                if (this.mBlackSurfaces[i] != null) {
                    if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                        Slog.i(WmsExt.TAG, "  BLACK " + this.mBlackSurfaces[i].surface + ": DESTROY");
                    }
                    this.mBlackSurfaces[i].surface.destroy();
                    this.mBlackSurfaces[i] = null;
                }
            }
        }
    }

    public void hide(SurfaceControl.Transaction transaction) {
        if (this.mBlackSurfaces != null) {
            for (int i = 0; i < this.mBlackSurfaces.length; i++) {
                if (this.mBlackSurfaces[i] != null) {
                    transaction.hide(this.mBlackSurfaces[i].surface);
                }
            }
        }
    }

    public void setAlpha(SurfaceControl.Transaction transaction, float f) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].setAlpha(transaction, f);
            }
        }
    }

    public void setMatrix(SurfaceControl.Transaction transaction, Matrix matrix) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].setMatrix(transaction, matrix);
            }
        }
    }

    public void clearMatrix(SurfaceControl.Transaction transaction) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].clearMatrix(transaction);
            }
        }
    }
}
