package android.graphics;

import android.graphics.PorterDuff;

public class ComposeShader extends Shader {
    private long mNativeInstanceShaderA;
    private long mNativeInstanceShaderB;
    private int mPorterDuffMode;
    Shader mShaderA;
    Shader mShaderB;

    private static native long nativeCreate(long j, long j2, long j3, int i);

    public ComposeShader(Shader shader, Shader shader2, Xfermode xfermode) {
        this(shader, shader2, xfermode.porterDuffMode);
    }

    public ComposeShader(Shader shader, Shader shader2, PorterDuff.Mode mode) {
        this(shader, shader2, mode.nativeInt);
    }

    private ComposeShader(Shader shader, Shader shader2, int i) {
        if (shader == null || shader2 == null) {
            throw new IllegalArgumentException("Shader parameters must not be null");
        }
        this.mShaderA = shader;
        this.mShaderB = shader2;
        this.mPorterDuffMode = i;
    }

    @Override
    long createNativeInstance(long j) {
        this.mNativeInstanceShaderA = this.mShaderA.getNativeInstance();
        this.mNativeInstanceShaderB = this.mShaderB.getNativeInstance();
        return nativeCreate(j, this.mShaderA.getNativeInstance(), this.mShaderB.getNativeInstance(), this.mPorterDuffMode);
    }

    @Override
    protected void verifyNativeInstance() {
        if (this.mShaderA.getNativeInstance() != this.mNativeInstanceShaderA || this.mShaderB.getNativeInstance() != this.mNativeInstanceShaderB) {
            discardNativeInstance();
        }
    }

    @Override
    protected Shader copy() {
        ComposeShader composeShader = new ComposeShader(this.mShaderA.copy(), this.mShaderB.copy(), this.mPorterDuffMode);
        copyLocalMatrix(composeShader);
        return composeShader;
    }
}
