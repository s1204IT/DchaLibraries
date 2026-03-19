package android.view;

import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import libcore.util.NativeAllocationRegistry;

public class RenderNode {
    final long mNativeRenderNode;
    private final View mOwningView;

    private static native void nAddAnimator(long j, long j2);

    private static native long nCreate(String str);

    private static native void nEndAllAnimators(long j);

    @CriticalNative
    private static native float nGetAlpha(long j);

    @CriticalNative
    private static native int nGetAmbientShadowColor(long j);

    @CriticalNative
    private static native float nGetCameraDistance(long j);

    @CriticalNative
    private static native boolean nGetClipToOutline(long j);

    private static native int nGetDebugSize(long j);

    @CriticalNative
    private static native float nGetElevation(long j);

    @CriticalNative
    private static native void nGetInverseTransformMatrix(long j, long j2);

    private static native long nGetNativeFinalizer();

    @CriticalNative
    private static native float nGetPivotX(long j);

    @CriticalNative
    private static native float nGetPivotY(long j);

    @CriticalNative
    private static native float nGetRotation(long j);

    @CriticalNative
    private static native float nGetRotationX(long j);

    @CriticalNative
    private static native float nGetRotationY(long j);

    @CriticalNative
    private static native float nGetScaleX(long j);

    @CriticalNative
    private static native float nGetScaleY(long j);

    @CriticalNative
    private static native int nGetSpotShadowColor(long j);

    @CriticalNative
    private static native void nGetTransformMatrix(long j, long j2);

    @CriticalNative
    private static native float nGetTranslationX(long j);

    @CriticalNative
    private static native float nGetTranslationY(long j);

    @CriticalNative
    private static native float nGetTranslationZ(long j);

    @CriticalNative
    private static native boolean nHasIdentityMatrix(long j);

    @CriticalNative
    private static native boolean nHasOverlappingRendering(long j);

    @CriticalNative
    private static native boolean nHasShadow(long j);

    @CriticalNative
    private static native boolean nIsPivotExplicitlySet(long j);

    @CriticalNative
    private static native boolean nIsValid(long j);

    @CriticalNative
    private static native boolean nOffsetLeftAndRight(long j, int i);

    @CriticalNative
    private static native boolean nOffsetTopAndBottom(long j, int i);

    private static native void nOutput(long j);

    private static native void nRequestPositionUpdates(long j, SurfaceView surfaceView);

    @CriticalNative
    private static native boolean nResetPivot(long j);

    @CriticalNative
    private static native boolean nSetAlpha(long j, float f);

    @CriticalNative
    private static native boolean nSetAmbientShadowColor(long j, int i);

    @CriticalNative
    private static native boolean nSetAnimationMatrix(long j, long j2);

    @CriticalNative
    private static native boolean nSetBottom(long j, int i);

    @CriticalNative
    private static native boolean nSetCameraDistance(long j, float f);

    @CriticalNative
    private static native boolean nSetClipBounds(long j, int i, int i2, int i3, int i4);

    @CriticalNative
    private static native boolean nSetClipBoundsEmpty(long j);

    @CriticalNative
    private static native boolean nSetClipToBounds(long j, boolean z);

    @CriticalNative
    private static native boolean nSetClipToOutline(long j, boolean z);

    @FastNative
    private static native void nSetDisplayList(long j, long j2);

    @CriticalNative
    private static native boolean nSetElevation(long j, float f);

    @CriticalNative
    private static native boolean nSetHasOverlappingRendering(long j, boolean z);

    @CriticalNative
    private static native boolean nSetLayerPaint(long j, long j2);

    @CriticalNative
    private static native boolean nSetLayerType(long j, int i);

    @CriticalNative
    private static native boolean nSetLeft(long j, int i);

    @CriticalNative
    private static native boolean nSetLeftTopRightBottom(long j, int i, int i2, int i3, int i4);

    @CriticalNative
    private static native boolean nSetOutlineConvexPath(long j, long j2, float f);

    @CriticalNative
    private static native boolean nSetOutlineEmpty(long j);

    @CriticalNative
    private static native boolean nSetOutlineNone(long j);

    @CriticalNative
    private static native boolean nSetOutlineRoundRect(long j, int i, int i2, int i3, int i4, float f, float f2);

    @CriticalNative
    private static native boolean nSetPivotX(long j, float f);

    @CriticalNative
    private static native boolean nSetPivotY(long j, float f);

    @CriticalNative
    private static native boolean nSetProjectBackwards(long j, boolean z);

    @CriticalNative
    private static native boolean nSetProjectionReceiver(long j, boolean z);

    @CriticalNative
    private static native boolean nSetRevealClip(long j, boolean z, float f, float f2, float f3);

    @CriticalNative
    private static native boolean nSetRight(long j, int i);

    @CriticalNative
    private static native boolean nSetRotation(long j, float f);

    @CriticalNative
    private static native boolean nSetRotationX(long j, float f);

    @CriticalNative
    private static native boolean nSetRotationY(long j, float f);

    @CriticalNative
    private static native boolean nSetScaleX(long j, float f);

    @CriticalNative
    private static native boolean nSetScaleY(long j, float f);

    @CriticalNative
    private static native boolean nSetSpotShadowColor(long j, int i);

    @CriticalNative
    private static native boolean nSetStaticMatrix(long j, long j2);

    @CriticalNative
    private static native boolean nSetTop(long j, int i);

    @CriticalNative
    private static native boolean nSetTranslationX(long j, float f);

    @CriticalNative
    private static native boolean nSetTranslationY(long j, float f);

    @CriticalNative
    private static native boolean nSetTranslationZ(long j, float f);

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(RenderNode.class.getClassLoader(), RenderNode.nGetNativeFinalizer(), 1024);

        private NoImagePreloadHolder() {
        }
    }

    private RenderNode(String str, View view) {
        this.mNativeRenderNode = nCreate(str);
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeRenderNode);
        this.mOwningView = view;
    }

    private RenderNode(long j) {
        this.mNativeRenderNode = j;
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeRenderNode);
        this.mOwningView = null;
    }

    public void destroy() {
    }

    public static RenderNode create(String str, View view) {
        return new RenderNode(str, view);
    }

    public static RenderNode adopt(long j) {
        return new RenderNode(j);
    }

    public void requestPositionUpdates(SurfaceView surfaceView) {
        nRequestPositionUpdates(this.mNativeRenderNode, surfaceView);
    }

    public DisplayListCanvas start(int i, int i2) {
        return DisplayListCanvas.obtain(this, i, i2);
    }

    public void end(DisplayListCanvas displayListCanvas) {
        nSetDisplayList(this.mNativeRenderNode, displayListCanvas.finishRecording());
        displayListCanvas.recycle();
    }

    public void discardDisplayList() {
        nSetDisplayList(this.mNativeRenderNode, 0L);
    }

    public boolean isValid() {
        return nIsValid(this.mNativeRenderNode);
    }

    long getNativeDisplayList() {
        if (!isValid()) {
            throw new IllegalStateException("The display list is not valid.");
        }
        return this.mNativeRenderNode;
    }

    public boolean hasIdentityMatrix() {
        return nHasIdentityMatrix(this.mNativeRenderNode);
    }

    public void getMatrix(Matrix matrix) {
        nGetTransformMatrix(this.mNativeRenderNode, matrix.native_instance);
    }

    public void getInverseMatrix(Matrix matrix) {
        nGetInverseTransformMatrix(this.mNativeRenderNode, matrix.native_instance);
    }

    public boolean setLayerType(int i) {
        return nSetLayerType(this.mNativeRenderNode, i);
    }

    public boolean setLayerPaint(Paint paint) {
        return nSetLayerPaint(this.mNativeRenderNode, paint != null ? paint.getNativeInstance() : 0L);
    }

    public boolean setClipBounds(Rect rect) {
        if (rect == null) {
            return nSetClipBoundsEmpty(this.mNativeRenderNode);
        }
        return nSetClipBounds(this.mNativeRenderNode, rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean setClipToBounds(boolean z) {
        return nSetClipToBounds(this.mNativeRenderNode, z);
    }

    public boolean setProjectBackwards(boolean z) {
        return nSetProjectBackwards(this.mNativeRenderNode, z);
    }

    public boolean setProjectionReceiver(boolean z) {
        return nSetProjectionReceiver(this.mNativeRenderNode, z);
    }

    public boolean setOutline(Outline outline) {
        if (outline == null) {
            return nSetOutlineNone(this.mNativeRenderNode);
        }
        switch (outline.mMode) {
            case 0:
                return nSetOutlineEmpty(this.mNativeRenderNode);
            case 1:
                return nSetOutlineRoundRect(this.mNativeRenderNode, outline.mRect.left, outline.mRect.top, outline.mRect.right, outline.mRect.bottom, outline.mRadius, outline.mAlpha);
            case 2:
                return nSetOutlineConvexPath(this.mNativeRenderNode, outline.mPath.mNativePath, outline.mAlpha);
            default:
                throw new IllegalArgumentException("Unrecognized outline?");
        }
    }

    public boolean hasShadow() {
        return nHasShadow(this.mNativeRenderNode);
    }

    public boolean setSpotShadowColor(int i) {
        return nSetSpotShadowColor(this.mNativeRenderNode, i);
    }

    public boolean setAmbientShadowColor(int i) {
        return nSetAmbientShadowColor(this.mNativeRenderNode, i);
    }

    public int getSpotShadowColor() {
        return nGetSpotShadowColor(this.mNativeRenderNode);
    }

    public int getAmbientShadowColor() {
        return nGetAmbientShadowColor(this.mNativeRenderNode);
    }

    public boolean setClipToOutline(boolean z) {
        return nSetClipToOutline(this.mNativeRenderNode, z);
    }

    public boolean getClipToOutline() {
        return nGetClipToOutline(this.mNativeRenderNode);
    }

    public boolean setRevealClip(boolean z, float f, float f2, float f3) {
        return nSetRevealClip(this.mNativeRenderNode, z, f, f2, f3);
    }

    public boolean setStaticMatrix(Matrix matrix) {
        return nSetStaticMatrix(this.mNativeRenderNode, matrix.native_instance);
    }

    public boolean setAnimationMatrix(Matrix matrix) {
        return nSetAnimationMatrix(this.mNativeRenderNode, matrix != null ? matrix.native_instance : 0L);
    }

    public boolean setAlpha(float f) {
        return nSetAlpha(this.mNativeRenderNode, f);
    }

    public float getAlpha() {
        return nGetAlpha(this.mNativeRenderNode);
    }

    public boolean setHasOverlappingRendering(boolean z) {
        return nSetHasOverlappingRendering(this.mNativeRenderNode, z);
    }

    public boolean hasOverlappingRendering() {
        return nHasOverlappingRendering(this.mNativeRenderNode);
    }

    public boolean setElevation(float f) {
        return nSetElevation(this.mNativeRenderNode, f);
    }

    public float getElevation() {
        return nGetElevation(this.mNativeRenderNode);
    }

    public boolean setTranslationX(float f) {
        return nSetTranslationX(this.mNativeRenderNode, f);
    }

    public float getTranslationX() {
        return nGetTranslationX(this.mNativeRenderNode);
    }

    public boolean setTranslationY(float f) {
        return nSetTranslationY(this.mNativeRenderNode, f);
    }

    public float getTranslationY() {
        return nGetTranslationY(this.mNativeRenderNode);
    }

    public boolean setTranslationZ(float f) {
        return nSetTranslationZ(this.mNativeRenderNode, f);
    }

    public float getTranslationZ() {
        return nGetTranslationZ(this.mNativeRenderNode);
    }

    public boolean setRotation(float f) {
        return nSetRotation(this.mNativeRenderNode, f);
    }

    public float getRotation() {
        return nGetRotation(this.mNativeRenderNode);
    }

    public boolean setRotationX(float f) {
        return nSetRotationX(this.mNativeRenderNode, f);
    }

    public float getRotationX() {
        return nGetRotationX(this.mNativeRenderNode);
    }

    public boolean setRotationY(float f) {
        return nSetRotationY(this.mNativeRenderNode, f);
    }

    public float getRotationY() {
        return nGetRotationY(this.mNativeRenderNode);
    }

    public boolean setScaleX(float f) {
        return nSetScaleX(this.mNativeRenderNode, f);
    }

    public float getScaleX() {
        return nGetScaleX(this.mNativeRenderNode);
    }

    public boolean setScaleY(float f) {
        return nSetScaleY(this.mNativeRenderNode, f);
    }

    public float getScaleY() {
        return nGetScaleY(this.mNativeRenderNode);
    }

    public boolean setPivotX(float f) {
        return nSetPivotX(this.mNativeRenderNode, f);
    }

    public float getPivotX() {
        return nGetPivotX(this.mNativeRenderNode);
    }

    public boolean setPivotY(float f) {
        return nSetPivotY(this.mNativeRenderNode, f);
    }

    public float getPivotY() {
        return nGetPivotY(this.mNativeRenderNode);
    }

    public boolean isPivotExplicitlySet() {
        return nIsPivotExplicitlySet(this.mNativeRenderNode);
    }

    public boolean resetPivot() {
        return nResetPivot(this.mNativeRenderNode);
    }

    public boolean setCameraDistance(float f) {
        return nSetCameraDistance(this.mNativeRenderNode, f);
    }

    public float getCameraDistance() {
        return nGetCameraDistance(this.mNativeRenderNode);
    }

    public boolean setLeft(int i) {
        return nSetLeft(this.mNativeRenderNode, i);
    }

    public boolean setTop(int i) {
        return nSetTop(this.mNativeRenderNode, i);
    }

    public boolean setRight(int i) {
        return nSetRight(this.mNativeRenderNode, i);
    }

    public boolean setBottom(int i) {
        return nSetBottom(this.mNativeRenderNode, i);
    }

    public boolean setLeftTopRightBottom(int i, int i2, int i3, int i4) {
        return nSetLeftTopRightBottom(this.mNativeRenderNode, i, i2, i3, i4);
    }

    public boolean offsetLeftAndRight(int i) {
        return nOffsetLeftAndRight(this.mNativeRenderNode, i);
    }

    public boolean offsetTopAndBottom(int i) {
        return nOffsetTopAndBottom(this.mNativeRenderNode, i);
    }

    public void output() {
        nOutput(this.mNativeRenderNode);
    }

    public int getDebugSize() {
        return nGetDebugSize(this.mNativeRenderNode);
    }

    public void addAnimator(RenderNodeAnimator renderNodeAnimator) {
        if (this.mOwningView == null || this.mOwningView.mAttachInfo == null) {
            throw new IllegalStateException("Cannot start this animator on a detached view!");
        }
        nAddAnimator(this.mNativeRenderNode, renderNodeAnimator.getNativeAnimator());
        this.mOwningView.mAttachInfo.mViewRootImpl.registerAnimatingRenderNode(this);
    }

    public boolean isAttached() {
        return (this.mOwningView == null || this.mOwningView.mAttachInfo == null) ? false : true;
    }

    public void registerVectorDrawableAnimator(AnimatedVectorDrawable.VectorDrawableAnimatorRT vectorDrawableAnimatorRT) {
        if (this.mOwningView == null || this.mOwningView.mAttachInfo == null) {
            throw new IllegalStateException("Cannot start this animator on a detached view!");
        }
        this.mOwningView.mAttachInfo.mViewRootImpl.registerVectorDrawableAnimator(vectorDrawableAnimatorRT);
    }

    public void endAllAnimators() {
        nEndAllAnimators(this.mNativeRenderNode);
    }
}
