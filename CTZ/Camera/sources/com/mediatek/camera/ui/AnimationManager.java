package com.mediatek.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.ui.preview.PreviewSurfaceView;

class AnimationManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AnimationManager.class.getSimpleName());
    private final ViewGroup mAnimationRoot;
    private AnimationTask mAnimationTask;
    private final ImageView mAnimationView;
    private final IApp mApp;
    private final CameraAppUI mAppUI;
    private final View mCoverView;
    private AnimatorSet mSwitchCameraAnimator;

    private static final class AsyncData {
        public IAppUi.AnimationData mData;
        public IAppUi.AnimationType mType;

        private AsyncData() {
        }
    }

    public AnimationManager(IApp iApp, CameraAppUI cameraAppUI) {
        this.mApp = iApp;
        this.mAppUI = cameraAppUI;
        this.mAnimationRoot = (ViewGroup) this.mApp.getActivity().findViewById(R.id.animation_root);
        this.mAnimationView = (ImageView) this.mApp.getActivity().findViewById(R.id.animation_view);
        this.mCoverView = this.mApp.getActivity().findViewById(R.id.camera_cover);
    }

    static class AnonymousClass10 {
        static final int[] $SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType = new int[IAppUi.AnimationType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[IAppUi.AnimationType.TYPE_SWITCH_CAMERA.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[IAppUi.AnimationType.TYPE_CAPTURE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[IAppUi.AnimationType.TYPE_SWITCH_MODE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public void animationStart(IAppUi.AnimationType animationType, IAppUi.AnimationData animationData) {
        LogHelper.d(TAG, "Start animation type: " + animationType);
        switch (AnonymousClass10.$SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[animationType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mCoverView.setVisibility(0);
                playCaptureAnimation();
                break;
        }
    }

    public void animationEnd(IAppUi.AnimationType animationType) {
        LogHelper.d(TAG, "End animation type: " + animationType);
        switch (AnonymousClass10.$SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[animationType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mCoverView.setVisibility(8);
                break;
        }
    }

    public void onPreviewStarted() {
        if (this.mAnimationTask == null) {
            this.mAppUI.removeTopSurface();
        }
    }

    private class AnimationTask extends AsyncTask<AsyncData, Void, Bitmap> {
        private AsyncData mData;
        final AnimationManager this$0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PreviewSurfaceView previewSurfaceView = (PreviewSurfaceView) this.this$0.mApp.getActivity().findViewById(R.id.preview_surface);
            int iMax = Math.max(previewSurfaceView.getWidth(), previewSurfaceView.getHeight());
            int iMin = Math.min(previewSurfaceView.getWidth(), previewSurfaceView.getHeight());
            LogHelper.d(AnimationManager.TAG, "onPreExecute width " + iMax + " height " + iMin);
            ViewGroup.LayoutParams layoutParams = this.this$0.mAnimationView.getLayoutParams();
            layoutParams.width = iMin;
            layoutParams.height = iMax;
            this.this$0.mAnimationView.setLayoutParams(layoutParams);
        }

        @Override
        protected Bitmap doInBackground(AsyncData... asyncDataArr) throws Throwable {
            this.mData = asyncDataArr[0];
            IAppUi.AnimationData animationData = this.mData.mData;
            LogHelper.d(AnimationManager.TAG, "doInBackground format " + animationData.mFormat + " width " + animationData.mWidth + " height " + animationData.mHeight);
            if (animationData.mFormat == 17) {
                return this.this$0.blurBitmap(this.this$0.createAnimationBitmap(this.this$0.convertYuvToRGB(AnimationManager.halveYUV420(animationData.mData, animationData.mWidth, animationData.mHeight, 2), animationData.mWidth / 2, animationData.mHeight / 2), animationData.mOrientation, animationData.mIsMirror));
            }
            LogHelper.e(AnimationManager.TAG, "Now just support NV21 format.");
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            LogHelper.d(AnimationManager.TAG, "onPostExecute type " + this.mData.mType);
            if (bitmap == null) {
                LogHelper.e(AnimationManager.TAG, "The result bitmap is null!");
                return;
            }
            int i = AnonymousClass10.$SwitchMap$com$mediatek$camera$common$IAppUi$AnimationType[this.mData.mType.ordinal()];
            if (i == 1) {
                this.this$0.mAnimationRoot.setVisibility(0);
                this.this$0.mAnimationView.setImageBitmap(bitmap);
                this.this$0.mAnimationView.setVisibility(0);
                this.this$0.playSwitchCameraAnimation();
            } else if (i == 3) {
                this.this$0.mAnimationRoot.setVisibility(0);
                this.this$0.mAnimationView.setImageBitmap(bitmap);
                this.this$0.mAnimationView.setVisibility(0);
                this.this$0.playSlideAnimation();
            }
            this.this$0.mAnimationTask = null;
        }
    }

    private void playSlideAnimation() {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mAnimationRoot, "alpha", 0.8f, 1.0f);
        objectAnimatorOfFloat.setInterpolator(new AccelerateInterpolator());
        objectAnimatorOfFloat.setDuration(100L);
        objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "fade in animation end");
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mAnimationRoot, "alpha", 1.0f, 0.8f);
        objectAnimatorOfFloat2.setDuration(100L);
        objectAnimatorOfFloat2.setInterpolator(new AccelerateInterpolator());
        objectAnimatorOfFloat2.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "fade out animation end");
                AnimationManager.this.mAnimationRoot.setVisibility(8);
                AnimationManager.this.mAnimationView.setVisibility(8);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(this.mAnimationRoot, "alpha", 0.4f, 0.0f);
        objectAnimatorOfFloat3.setDuration(200L);
        objectAnimatorOfFloat3.setInterpolator(new AccelerateInterpolator());
        objectAnimatorOfFloat3.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "fade out animation end");
                AnimationManager.this.mAnimationRoot.setVisibility(8);
                AnimationManager.this.mAnimationView.setVisibility(8);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(objectAnimatorOfFloat, objectAnimatorOfFloat2);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "switch animation end");
                AnimationManager.this.mAppUI.removeTopSurface();
                AnimationManager.this.mAnimationView.setVisibility(8);
                AnimationManager.this.mAnimationRoot.setVisibility(8);
                AnimationManager.this.mAnimationRoot.setAlpha(0.0f);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animatorSet.start();
    }

    private void playSwitchCameraAnimation() {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mAnimationRoot, "alpha", 0.4f, 1.0f);
        objectAnimatorOfFloat.setInterpolator(new AccelerateInterpolator());
        objectAnimatorOfFloat.setDuration(200L);
        objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "fade in animation end");
                AnimationManager.this.mAppUI.removeTopSurface();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(this.mApp.getActivity(), R.animator.flip_anim);
        animatorSet.setTarget(this.mAnimationView);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "flip animation end");
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mAnimationRoot, "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat2.setDuration(400L);
        objectAnimatorOfFloat2.setInterpolator(new AccelerateInterpolator());
        objectAnimatorOfFloat2.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "fade out animation end");
                AnimationManager.this.mAnimationRoot.setVisibility(8);
                AnimationManager.this.mAnimationView.setVisibility(8);
                AnimatorSet animatorSet2 = (AnimatorSet) AnimatorInflater.loadAnimator(AnimationManager.this.mApp.getActivity(), R.animator.flip_anim_reset);
                animatorSet2.setTarget(AnimationManager.this.mAnimationView);
                animatorSet2.start();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mSwitchCameraAnimator = new AnimatorSet();
        this.mSwitchCameraAnimator.playSequentially(objectAnimatorOfFloat, animatorSet, objectAnimatorOfFloat2);
        this.mSwitchCameraAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                LogHelper.d(AnimationManager.TAG, "switch animation end");
                AnimationManager.this.mAppUI.removeTopSurface();
                AnimationManager.this.mAnimationRoot.setAlpha(0.0f);
                AnimationManager.this.mApp.getAppUi().applyAllUIEnabled(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mSwitchCameraAnimator.start();
    }

    private void playCaptureAnimation() {
        LogHelper.d(TAG, "playCaptureAnimation +");
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(this.mApp.getActivity(), R.animator.cature_anim);
        animatorSet.setTarget(this.mCoverView);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                AnimationManager.this.mCoverView.setVisibility(8);
            }
        });
        animatorSet.start();
        LogHelper.d(TAG, "playCaptureAnimation -");
    }

    private Bitmap convertYuvToRGB(byte[] bArr, int i, int i2) throws Throwable {
        LogHelper.d(TAG, "convertYuvToRGB +");
        RenderScript renderScriptCreate = RenderScript.create(this.mApp.getActivity().getApplicationContext());
        ScriptIntrinsicYuvToRGB scriptIntrinsicYuvToRGBCreate = ScriptIntrinsicYuvToRGB.create(renderScriptCreate, Element.RGBA_8888(renderScriptCreate));
        Type.Builder builder = new Type.Builder(renderScriptCreate, Element.createPixel(renderScriptCreate, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV));
        builder.setX(i);
        builder.setY(i2);
        builder.setMipmaps(false);
        builder.setYuvFormat(17);
        Allocation allocationCreateTyped = Allocation.createTyped(renderScriptCreate, builder.create(), 1);
        allocationCreateTyped.copyFrom(bArr);
        Type.Builder builder2 = new Type.Builder(renderScriptCreate, Element.RGBA_8888(renderScriptCreate));
        builder2.setX(i);
        builder2.setY(i2);
        builder2.setMipmaps(false);
        Allocation allocationCreateTyped2 = Allocation.createTyped(renderScriptCreate, builder2.create(), 1);
        scriptIntrinsicYuvToRGBCreate.setInput(allocationCreateTyped);
        scriptIntrinsicYuvToRGBCreate.forEach(allocationCreateTyped2);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        allocationCreateTyped2.copyTo(bitmapCreateBitmap);
        renderScriptCreate.destroy();
        LogHelper.d(TAG, "convertYuvToRGB -");
        return bitmapCreateBitmap;
    }

    private Bitmap createAnimationBitmap(Bitmap bitmap, int i, boolean z) {
        LogHelper.d(TAG, "createAnimationBitmap + isMirror " + z + " rotation " + i);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        if (z) {
            matrix.postScale(1.0f, -1.0f);
        }
        matrix.postRotate(i);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        LogHelper.d(TAG, "createAnimationBitmap -");
        return bitmapCreateBitmap;
    }

    private Bitmap blurBitmap(Bitmap bitmap) {
        LogHelper.d(TAG, "blurBitmap +");
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript renderScriptCreate = RenderScript.create(this.mApp.getActivity().getApplicationContext());
        ScriptIntrinsicBlur scriptIntrinsicBlurCreate = ScriptIntrinsicBlur.create(renderScriptCreate, Element.U8_4(renderScriptCreate));
        Allocation allocationCreateFromBitmap = Allocation.createFromBitmap(renderScriptCreate, bitmap);
        Allocation allocationCreateFromBitmap2 = Allocation.createFromBitmap(renderScriptCreate, bitmapCreateBitmap);
        scriptIntrinsicBlurCreate.setRadius(25.0f);
        scriptIntrinsicBlurCreate.setInput(allocationCreateFromBitmap);
        scriptIntrinsicBlurCreate.forEach(allocationCreateFromBitmap2);
        allocationCreateFromBitmap2.copyTo(bitmapCreateBitmap);
        bitmap.recycle();
        renderScriptCreate.destroy();
        bitmap.recycle();
        LogHelper.d(TAG, "blurBitmap -");
        return bitmapCreateBitmap;
    }

    private static byte[] halveYUV420(byte[] bArr, int i, int i2, int i3) {
        LogHelper.d(TAG, "halveYUV420 +");
        byte[] bArr2 = new byte[((((i / i3) * i2) / i3) * 3) / 2];
        int i4 = 0;
        int i5 = 0;
        while (i4 < i2) {
            int i6 = i5;
            int i7 = 0;
            while (i7 < i) {
                bArr2[i6] = bArr[(i4 * i) + i7];
                i6++;
                i7 += i3;
            }
            i4 += i3;
            i5 = i6;
        }
        int i8 = 0;
        while (i8 < i2 / 2) {
            int i9 = i5;
            int i10 = 0;
            while (i10 < i) {
                int i11 = (i * i2) + (i8 * i);
                bArr2[i9] = bArr[i11 + i10];
                int i12 = i9 + 1;
                bArr2[i12] = bArr[i11 + i10 + 1];
                i9 = i12 + 1;
                i10 += 2 * i3;
            }
            i8 += i3;
            i5 = i9;
        }
        LogHelper.d(TAG, "halveYUV420 -");
        return bArr2;
    }
}
