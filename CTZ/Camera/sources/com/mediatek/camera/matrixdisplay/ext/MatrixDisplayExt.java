package com.mediatek.camera.matrixdisplay.ext;

import android.view.Surface;
import com.mediatek.matrixeffect.MatrixEffect;

public class MatrixDisplayExt {
    private static MatrixDisplayExt sMatrixDisplayExt;
    private EffectAvailableCallback mCallback;
    private MatrixEffect mMatrixEffect = MatrixEffect.getInstance();

    public interface EffectAvailableCallback {
    }

    private MatrixDisplayExt() {
    }

    public static MatrixDisplayExt getInstance() {
        if (sMatrixDisplayExt == null) {
            sMatrixDisplayExt = new MatrixDisplayExt();
        }
        return sMatrixDisplayExt;
    }

    public void setCallback(EffectAvailableCallback effectAvailableCallback) {
        this.mCallback = effectAvailableCallback;
        if (this.mCallback == null) {
            this.mMatrixEffect.setCallback((MatrixEffect.EffectsCallback) null);
        } else {
            this.mMatrixEffect.setCallback(new MatrixEffect.EffectsCallback() {
            });
        }
    }

    public void initialize(int i, int i2, int i3, int i4) {
        this.mMatrixEffect.initialize(i, i2, i3, i4);
    }

    public void setSurface(Surface surface, int i) {
        this.mMatrixEffect.setSurface(surface, i);
    }

    public void setBuffers(int i, int i2, byte[][] bArr) {
        this.mMatrixEffect.setBuffers(i, i2, bArr);
    }

    public void process(byte[] bArr, int[] iArr) {
        this.mMatrixEffect.process(bArr, iArr);
    }

    public void release() {
        this.mMatrixEffect.release();
    }
}
