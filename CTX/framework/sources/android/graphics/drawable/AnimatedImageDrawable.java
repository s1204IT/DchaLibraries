package android.graphics.drawable;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import com.android.internal.R;
import dalvik.annotation.optimization.FastNative;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import libcore.util.NativeAllocationRegistry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatedImageDrawable extends Drawable implements Animatable2 {
    private static final int FINISHED = -1;

    @Deprecated
    public static final int LOOP_INFINITE = -1;
    public static final int REPEAT_INFINITE = -1;
    private static final int REPEAT_UNDEFINED = -2;
    private ArrayList<Animatable2.AnimationCallback> mAnimationCallbacks;
    private ColorFilter mColorFilter;
    private Handler mHandler;
    private int mIntrinsicHeight;
    private int mIntrinsicWidth;
    private Runnable mRunnable;
    private boolean mStarting;
    private State mState;

    private static native long nCreate(long j, ImageDecoder imageDecoder, int i, int i2, Rect rect) throws IOException;

    private static native long nDraw(long j, long j2);

    @FastNative
    private static native int nGetAlpha(long j);

    @FastNative
    private static native long nGetNativeFinalizer();

    @FastNative
    private static native int nGetRepeatCount(long j);

    @FastNative
    private static native boolean nIsRunning(long j);

    @FastNative
    private static native long nNativeByteSize(long j);

    @FastNative
    private static native void nSetAlpha(long j, int i);

    @FastNative
    private static native void nSetColorFilter(long j, long j2);

    @FastNative
    private static native void nSetMirrored(long j, boolean z);

    private static native void nSetOnAnimationEndListener(long j, AnimatedImageDrawable animatedImageDrawable);

    @FastNative
    private static native void nSetRepeatCount(long j, int i);

    @FastNative
    private static native boolean nStart(long j);

    @FastNative
    private static native boolean nStop(long j);

    private class State {
        private final AssetFileDescriptor mAssetFd;
        private final InputStream mInputStream;
        final long mNativePtr;
        int[] mThemeAttrs = null;
        boolean mAutoMirrored = false;
        int mRepeatCount = -2;

        State(long j, InputStream inputStream, AssetFileDescriptor assetFileDescriptor) {
            this.mNativePtr = j;
            this.mInputStream = inputStream;
            this.mAssetFd = assetFileDescriptor;
        }
    }

    public void setRepeatCount(int i) {
        if (i < -1) {
            throw new IllegalArgumentException("invalid value passed to setRepeatCount" + i);
        }
        if (this.mState.mRepeatCount != i) {
            this.mState.mRepeatCount = i;
            if (this.mState.mNativePtr != 0) {
                nSetRepeatCount(this.mState.mNativePtr, i);
            }
        }
    }

    @Deprecated
    public void setLoopCount(int i) {
        setRepeatCount(i);
    }

    public int getRepeatCount() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called getRepeatCount on empty AnimatedImageDrawable");
        }
        if (this.mState.mRepeatCount == -2) {
            this.mState.mRepeatCount = nGetRepeatCount(this.mState.mNativePtr);
        }
        return this.mState.mRepeatCount;
    }

    @Deprecated
    public int getLoopCount(int i) {
        return getRepeatCount();
    }

    public AnimatedImageDrawable() {
        this.mAnimationCallbacks = null;
        this.mState = new State(0L, null, null);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        updateStateFromTypedArray(obtainAttributes(resources, theme, attributeSet, R.styleable.AnimatedImageDrawable), this.mSrcDensityOverride);
    }

    private void updateStateFromTypedArray(TypedArray typedArray, int i) throws XmlPullParserException {
        int i2;
        State state = this.mState;
        Resources resources = typedArray.getResources();
        int resourceId = typedArray.getResourceId(0, 0);
        if (resourceId != 0) {
            TypedValue typedValue = new TypedValue();
            resources.getValueForDensity(resourceId, i, typedValue, true);
            if (i > 0 && typedValue.density > 0 && typedValue.density != 65535) {
                if (typedValue.density == i) {
                    typedValue.density = resources.getDisplayMetrics().densityDpi;
                } else {
                    typedValue.density = (typedValue.density * resources.getDisplayMetrics().densityDpi) / i;
                }
            }
            if (typedValue.density != 0) {
                if (typedValue.density != 65535) {
                    i2 = typedValue.density;
                } else {
                    i2 = 0;
                }
            } else {
                i2 = 160;
            }
            try {
                Drawable drawableDecodeDrawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, resources.openRawResource(resourceId, typedValue), i2), new ImageDecoder.OnHeaderDecodedListener() {
                    @Override
                    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                        AnimatedImageDrawable.lambda$updateStateFromTypedArray$0(imageDecoder, imageInfo, source);
                    }
                });
                if (!(drawableDecodeDrawable instanceof AnimatedImageDrawable)) {
                    throw new XmlPullParserException(typedArray.getPositionDescription() + ": <animated-image> did not decode animated");
                }
                int i3 = this.mState.mRepeatCount;
                AnimatedImageDrawable animatedImageDrawable = (AnimatedImageDrawable) drawableDecodeDrawable;
                this.mState = animatedImageDrawable.mState;
                animatedImageDrawable.mState = null;
                this.mIntrinsicWidth = animatedImageDrawable.mIntrinsicWidth;
                this.mIntrinsicHeight = animatedImageDrawable.mIntrinsicHeight;
                if (i3 != -2) {
                    setRepeatCount(i3);
                }
            } catch (IOException e) {
                throw new XmlPullParserException(typedArray.getPositionDescription() + ": <animated-image> requires a valid 'src' attribute", null, e);
            }
        }
        this.mState.mThemeAttrs = typedArray.extractThemeAttrs();
        if (this.mState.mNativePtr == 0 && (this.mState.mThemeAttrs == null || this.mState.mThemeAttrs[0] == 0)) {
            throw new XmlPullParserException(typedArray.getPositionDescription() + ": <animated-image> requires a valid 'src' attribute");
        }
        this.mState.mAutoMirrored = typedArray.getBoolean(3, state.mAutoMirrored);
        int i4 = typedArray.getInt(1, -2);
        if (i4 != -2) {
            setRepeatCount(i4);
        }
        if (typedArray.getBoolean(2, false) && this.mState.mNativePtr != 0) {
            start();
        }
    }

    static void lambda$updateStateFromTypedArray$0(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
        if (!imageInfo.isAnimated()) {
            throw new IllegalArgumentException("image is not animated");
        }
    }

    public AnimatedImageDrawable(long j, ImageDecoder imageDecoder, int i, int i2, int i3, int i4, Rect rect, InputStream inputStream, AssetFileDescriptor assetFileDescriptor) throws IOException {
        this.mAnimationCallbacks = null;
        int iScaleFromDensity = Bitmap.scaleFromDensity(i, i3, i4);
        int iScaleFromDensity2 = Bitmap.scaleFromDensity(i2, i3, i4);
        if (rect == null) {
            this.mIntrinsicWidth = iScaleFromDensity;
            this.mIntrinsicHeight = iScaleFromDensity2;
        } else {
            rect.set(Bitmap.scaleFromDensity(rect.left, i3, i4), Bitmap.scaleFromDensity(rect.top, i3, i4), Bitmap.scaleFromDensity(rect.right, i3, i4), Bitmap.scaleFromDensity(rect.bottom, i3, i4));
            this.mIntrinsicWidth = rect.width();
            this.mIntrinsicHeight = rect.height();
        }
        this.mState = new State(nCreate(j, imageDecoder, iScaleFromDensity, iScaleFromDensity2, rect), inputStream, assetFileDescriptor);
        new NativeAllocationRegistry(AnimatedImageDrawable.class.getClassLoader(), nGetNativeFinalizer(), nNativeByteSize(this.mState.mNativePtr)).registerNativeAllocation(this.mState, this.mState.mNativePtr);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called draw on empty AnimatedImageDrawable");
        }
        if (this.mStarting) {
            this.mStarting = false;
            postOnAnimationStart();
        }
        long jNDraw = nDraw(this.mState.mNativePtr, canvas.getNativeCanvasWrapper());
        if (jNDraw > 0) {
            if (this.mRunnable == null) {
                this.mRunnable = new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.invalidateSelf();
                    }
                };
            }
            scheduleSelf(this.mRunnable, jNDraw + SystemClock.uptimeMillis());
        } else if (jNDraw == -1) {
            postOnAnimationEnd();
        }
    }

    @Override
    public void setAlpha(int i) {
        if (i < 0 || i > 255) {
            throw new IllegalArgumentException("Alpha must be between 0 and 255! provided " + i);
        }
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called setAlpha on empty AnimatedImageDrawable");
        }
        nSetAlpha(this.mState.mNativePtr, i);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called getAlpha on empty AnimatedImageDrawable");
        }
        return nGetAlpha(this.mState.mNativePtr);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called setColorFilter on empty AnimatedImageDrawable");
        }
        if (colorFilter != this.mColorFilter) {
            this.mColorFilter = colorFilter;
            nSetColorFilter(this.mState.mNativePtr, colorFilter != null ? colorFilter.getNativeInstance() : 0L);
            invalidateSelf();
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAutoMirrored(boolean z) {
        if (this.mState.mAutoMirrored != z) {
            this.mState.mAutoMirrored = z;
            if (getLayoutDirection() == 1 && this.mState.mNativePtr != 0) {
                nSetMirrored(this.mState.mNativePtr, z);
                invalidateSelf();
            }
        }
    }

    @Override
    public boolean onLayoutDirectionChanged(int i) {
        if (!this.mState.mAutoMirrored || this.mState.mNativePtr == 0) {
            return false;
        }
        nSetMirrored(this.mState.mNativePtr, i == 1);
        return true;
    }

    @Override
    public final boolean isAutoMirrored() {
        return this.mState.mAutoMirrored;
    }

    @Override
    public boolean isRunning() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called isRunning on empty AnimatedImageDrawable");
        }
        return nIsRunning(this.mState.mNativePtr);
    }

    @Override
    public void start() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called start on empty AnimatedImageDrawable");
        }
        if (nStart(this.mState.mNativePtr)) {
            this.mStarting = true;
            invalidateSelf();
        }
    }

    @Override
    public void stop() {
        if (this.mState.mNativePtr == 0) {
            throw new IllegalStateException("called stop on empty AnimatedImageDrawable");
        }
        if (nStop(this.mState.mNativePtr)) {
            postOnAnimationEnd();
        }
    }

    @Override
    public void registerAnimationCallback(Animatable2.AnimationCallback animationCallback) {
        if (animationCallback == null) {
            return;
        }
        if (this.mAnimationCallbacks == null) {
            this.mAnimationCallbacks = new ArrayList<>();
            nSetOnAnimationEndListener(this.mState.mNativePtr, this);
        }
        if (!this.mAnimationCallbacks.contains(animationCallback)) {
            this.mAnimationCallbacks.add(animationCallback);
        }
    }

    @Override
    public boolean unregisterAnimationCallback(Animatable2.AnimationCallback animationCallback) {
        if (animationCallback == null || this.mAnimationCallbacks == null || !this.mAnimationCallbacks.remove(animationCallback)) {
            return false;
        }
        if (this.mAnimationCallbacks.isEmpty()) {
            clearAnimationCallbacks();
            return true;
        }
        return true;
    }

    @Override
    public void clearAnimationCallbacks() {
        if (this.mAnimationCallbacks != null) {
            this.mAnimationCallbacks = null;
            nSetOnAnimationEndListener(this.mState.mNativePtr, null);
        }
    }

    private void postOnAnimationStart() {
        if (this.mAnimationCallbacks == null) {
            return;
        }
        getHandler().post(new Runnable() {
            @Override
            public final void run() {
                AnimatedImageDrawable.lambda$postOnAnimationStart$1(this.f$0);
            }
        });
    }

    public static void lambda$postOnAnimationStart$1(AnimatedImageDrawable animatedImageDrawable) {
        Iterator<Animatable2.AnimationCallback> it = animatedImageDrawable.mAnimationCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onAnimationStart(animatedImageDrawable);
        }
    }

    private void postOnAnimationEnd() {
        if (this.mAnimationCallbacks == null) {
            return;
        }
        getHandler().post(new Runnable() {
            @Override
            public final void run() {
                AnimatedImageDrawable.lambda$postOnAnimationEnd$2(this.f$0);
            }
        });
    }

    public static void lambda$postOnAnimationEnd$2(AnimatedImageDrawable animatedImageDrawable) {
        Iterator<Animatable2.AnimationCallback> it = animatedImageDrawable.mAnimationCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onAnimationEnd(animatedImageDrawable);
        }
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
        return this.mHandler;
    }

    private void onAnimationEnd() {
        if (this.mAnimationCallbacks != null) {
            Iterator<Animatable2.AnimationCallback> it = this.mAnimationCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onAnimationEnd(this);
            }
        }
    }
}
