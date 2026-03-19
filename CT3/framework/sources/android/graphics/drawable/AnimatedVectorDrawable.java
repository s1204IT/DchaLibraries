package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityThread;
import android.app.Application;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.util.LongArray;
import android.util.PathParser;
import android.view.Choreographer;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.RenderNodeAnimatorSetHelper;
import com.android.internal.R;
import com.android.internal.util.VirtualRefBasePtr;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatedVectorDrawable extends Drawable implements Animatable2 {
    private static final String ANIMATED_VECTOR = "animated-vector";
    private static final boolean DBG_ANIMATION_VECTOR_DRAWABLE = false;
    private static final String LOGTAG = "AnimatedVectorDrawable";
    private static final String TARGET = "target";
    private AnimatedVectorDrawableState mAnimatedVectorState;
    private ArrayList<Animatable2.AnimationCallback> mAnimationCallbacks;
    private Animator.AnimatorListener mAnimatorListener;
    private VectorDrawableAnimator mAnimatorSet;
    private AnimatorSet mAnimatorSetFromXml;
    private final Drawable.Callback mCallback;
    private boolean mMutated;
    private Resources mRes;

    private interface VectorDrawableAnimator {
        boolean canReverse();

        void end();

        void init(AnimatorSet animatorSet);

        boolean isInfinite();

        boolean isRunning();

        boolean isStarted();

        void onDraw(Canvas canvas);

        void pause();

        void removeListener(Animator.AnimatorListener animatorListener);

        void reset();

        void resume();

        void reverse();

        void setListener(Animator.AnimatorListener animatorListener);

        void start();
    }

    AnimatedVectorDrawable(AnimatedVectorDrawableState state, Resources res, AnimatedVectorDrawable animatedVectorDrawable) {
        this(state, res);
    }

    private static native void nAddAnimator(long j, long j2, long j3, long j4, long j5, int i);

    private static native long nCreateAnimatorSet();

    private static native long nCreateGroupPropertyHolder(long j, int i, float f, float f2);

    private static native long nCreatePathColorPropertyHolder(long j, int i, int i2, int i3);

    private static native long nCreatePathDataPropertyHolder(long j, long j2, long j3);

    private static native long nCreatePathPropertyHolder(long j, int i, float f, float f2);

    private static native long nCreateRootAlphaPropertyHolder(long j, float f, float f2);

    private static native void nEnd(long j);

    private static native void nReset(long j);

    private static native void nReverse(long j, VectorDrawableAnimatorRT vectorDrawableAnimatorRT, int i);

    private static native void nSetPropertyHolderData(long j, float[] fArr, int i);

    private static native void nStart(long j, VectorDrawableAnimatorRT vectorDrawableAnimatorRT, int i);

    public AnimatedVectorDrawable() {
        this(null, null);
    }

    private AnimatedVectorDrawable(AnimatedVectorDrawableState state, Resources res) {
        this.mAnimatorSet = new VectorDrawableAnimatorUI(this);
        this.mAnimatorSetFromXml = null;
        this.mAnimationCallbacks = null;
        this.mAnimatorListener = null;
        this.mCallback = new Drawable.Callback() {
            @Override
            public void invalidateDrawable(Drawable who) {
                AnimatedVectorDrawable.this.invalidateSelf();
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {
                AnimatedVectorDrawable.this.scheduleSelf(what, when);
            }

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {
                AnimatedVectorDrawable.this.unscheduleSelf(what);
            }
        };
        this.mAnimatedVectorState = new AnimatedVectorDrawableState(state, this.mCallback, res);
        this.mRes = res;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mAnimatedVectorState = new AnimatedVectorDrawableState(this.mAnimatedVectorState, this.mCallback, this.mRes);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        if (this.mAnimatedVectorState.mVectorDrawable != null) {
            this.mAnimatedVectorState.mVectorDrawable.clearMutated();
        }
        this.mMutated = false;
    }

    private static boolean shouldIgnoreInvalidAnimation() {
        Application app = ActivityThread.currentApplication();
        return app == null || app.getApplicationInfo() == null || app.getApplicationInfo().targetSdkVersion < 24;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mAnimatedVectorState.mChangingConfigurations = getChangingConfigurations();
        return this.mAnimatedVectorState;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mAnimatedVectorState.getChangingConfigurations();
    }

    @Override
    public void draw(Canvas canvas) {
        this.mAnimatorSet.onDraw(canvas);
        this.mAnimatedVectorState.mVectorDrawable.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mAnimatedVectorState.mVectorDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return this.mAnimatedVectorState.mVectorDrawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return this.mAnimatedVectorState.mVectorDrawable.setLevel(level);
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection) {
        return this.mAnimatedVectorState.mVectorDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    public int getAlpha() {
        return this.mAnimatedVectorState.mVectorDrawable.getAlpha();
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAnimatedVectorState.mVectorDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mAnimatedVectorState.mVectorDrawable.setColorFilter(colorFilter);
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mAnimatedVectorState.mVectorDrawable.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mAnimatedVectorState.mVectorDrawable.setTintList(tint);
    }

    @Override
    public void setHotspot(float x, float y) {
        this.mAnimatedVectorState.mVectorDrawable.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        this.mAnimatedVectorState.mVectorDrawable.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mAnimatedVectorState.mVectorDrawable.setTintMode(tintMode);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (this.mAnimatorSet.isInfinite() && this.mAnimatorSet.isStarted()) {
            if (visible) {
                this.mAnimatorSet.resume();
            } else {
                this.mAnimatorSet.pause();
            }
        }
        this.mAnimatedVectorState.mVectorDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
    }

    @Override
    public boolean isStateful() {
        return this.mAnimatedVectorState.mVectorDrawable.isStateful();
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mAnimatedVectorState.mVectorDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mAnimatedVectorState.mVectorDrawable.getIntrinsicHeight();
    }

    @Override
    public void getOutline(Outline outline) {
        this.mAnimatedVectorState.mVectorDrawable.getOutline(outline);
    }

    @Override
    public Insets getOpticalInsets() {
        return this.mAnimatedVectorState.mVectorDrawable.getOpticalInsets();
    }

    @Override
    public void inflate(Resources res, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        AnimatedVectorDrawableState state = this.mAnimatedVectorState;
        int eventType = parser.getEventType();
        float pathErrorScale = 1.0f;
        while (eventType != 1) {
            if (eventType == 2) {
                String tagName = parser.getName();
                if (ANIMATED_VECTOR.equals(tagName)) {
                    TypedArray a = obtainAttributes(res, theme, attrs, R.styleable.AnimatedVectorDrawable);
                    int drawableRes = a.getResourceId(0, 0);
                    if (drawableRes != 0) {
                        VectorDrawable vectorDrawable = (VectorDrawable) res.getDrawable(drawableRes, theme).mutate();
                        vectorDrawable.setAllowCaching(false);
                        vectorDrawable.setCallback(this.mCallback);
                        pathErrorScale = vectorDrawable.getPixelSize();
                        if (state.mVectorDrawable != null) {
                            state.mVectorDrawable.setCallback(null);
                        }
                        state.mVectorDrawable = vectorDrawable;
                    }
                    a.recycle();
                } else if (TARGET.equals(tagName)) {
                    TypedArray a2 = obtainAttributes(res, theme, attrs, R.styleable.AnimatedVectorDrawableTarget);
                    String target = a2.getString(0);
                    int animResId = a2.getResourceId(1, 0);
                    if (animResId != 0) {
                        if (theme != null) {
                            Animator objectAnimator = AnimatorInflater.loadAnimator(res, theme, animResId, pathErrorScale);
                            state.addTargetAnimator(target, objectAnimator);
                        } else {
                            state.addPendingAnimator(animResId, pathErrorScale, target);
                        }
                    }
                    a2.recycle();
                }
            }
            eventType = parser.next();
        }
        if (state.mPendingAnims == null) {
            res = null;
        }
        this.mRes = res;
    }

    public void forceAnimationOnUI() {
        if (!(this.mAnimatorSet instanceof VectorDrawableAnimatorRT)) {
            return;
        }
        VectorDrawableAnimatorRT animator = (VectorDrawableAnimatorRT) this.mAnimatorSet;
        if (animator.isRunning()) {
            throw new UnsupportedOperationException("Cannot force Animated Vector Drawable to run on UI thread when the animation has started on RenderThread.");
        }
        this.mAnimatorSet = new VectorDrawableAnimatorUI(this);
        if (this.mAnimatorSetFromXml == null) {
            return;
        }
        this.mAnimatorSet.init(this.mAnimatorSetFromXml);
    }

    @Override
    public boolean canApplyTheme() {
        if (this.mAnimatedVectorState == null || !this.mAnimatedVectorState.canApplyTheme()) {
            return super.canApplyTheme();
        }
        return true;
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        VectorDrawable vectorDrawable = this.mAnimatedVectorState.mVectorDrawable;
        if (vectorDrawable != null && vectorDrawable.canApplyTheme()) {
            vectorDrawable.applyTheme(t);
        }
        if (t != null) {
            this.mAnimatedVectorState.inflatePendingAnimators(t.getResources(), t);
        }
        if (this.mAnimatedVectorState.mPendingAnims != null) {
            return;
        }
        this.mRes = null;
    }

    private static class AnimatedVectorDrawableState extends Drawable.ConstantState {
        ArrayList<Animator> mAnimators;
        int mChangingConfigurations;
        ArrayList<PendingAnimator> mPendingAnims;
        ArrayMap<Animator, String> mTargetNameMap;
        VectorDrawable mVectorDrawable;

        public AnimatedVectorDrawableState(AnimatedVectorDrawableState copy, Drawable.Callback owner, Resources res) {
            if (copy != null) {
                this.mChangingConfigurations = copy.mChangingConfigurations;
                if (copy.mVectorDrawable != null) {
                    Drawable.ConstantState cs = copy.mVectorDrawable.getConstantState();
                    if (res != null) {
                        this.mVectorDrawable = (VectorDrawable) cs.newDrawable(res);
                    } else {
                        this.mVectorDrawable = (VectorDrawable) cs.newDrawable();
                    }
                    this.mVectorDrawable = (VectorDrawable) this.mVectorDrawable.mutate();
                    this.mVectorDrawable.setCallback(owner);
                    this.mVectorDrawable.setLayoutDirection(copy.mVectorDrawable.getLayoutDirection());
                    this.mVectorDrawable.setBounds(copy.mVectorDrawable.getBounds());
                    this.mVectorDrawable.setAllowCaching(false);
                }
                if (copy.mAnimators != null) {
                    this.mAnimators = new ArrayList<>(copy.mAnimators);
                }
                if (copy.mTargetNameMap != null) {
                    this.mTargetNameMap = new ArrayMap<>(copy.mTargetNameMap);
                }
                if (copy.mPendingAnims == null) {
                    return;
                }
                this.mPendingAnims = new ArrayList<>(copy.mPendingAnims);
                return;
            }
            this.mVectorDrawable = new VectorDrawable();
        }

        @Override
        public boolean canApplyTheme() {
            if ((this.mVectorDrawable != null && this.mVectorDrawable.canApplyTheme()) || this.mPendingAnims != null) {
                return true;
            }
            return super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new AnimatedVectorDrawable(this, null, 0 == true ? 1 : 0);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new AnimatedVectorDrawable(this, res, null);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        public void addPendingAnimator(int resId, float pathErrorScale, String target) {
            if (this.mPendingAnims == null) {
                this.mPendingAnims = new ArrayList<>(1);
            }
            this.mPendingAnims.add(new PendingAnimator(resId, pathErrorScale, target));
        }

        public void addTargetAnimator(String targetName, Animator animator) {
            if (this.mAnimators == null) {
                this.mAnimators = new ArrayList<>(1);
                this.mTargetNameMap = new ArrayMap<>(1);
            }
            this.mAnimators.add(animator);
            this.mTargetNameMap.put(animator, targetName);
        }

        public void prepareLocalAnimators(AnimatorSet animatorSet, Resources res) {
            if (this.mPendingAnims != null) {
                if (res != null) {
                    inflatePendingAnimators(res, null);
                } else {
                    Log.e(AnimatedVectorDrawable.LOGTAG, "Failed to load animators. Either the AnimatedVectorDrawable must be created using a Resources object or applyTheme() must be called with a non-null Theme object.");
                }
                this.mPendingAnims = null;
            }
            int count = this.mAnimators == null ? 0 : this.mAnimators.size();
            if (count <= 0) {
                return;
            }
            Animator firstAnim = prepareLocalAnimator(0);
            AnimatorSet.Builder builder = animatorSet.play(firstAnim);
            for (int i = 1; i < count; i++) {
                Animator nextAnim = prepareLocalAnimator(i);
                builder.with(nextAnim);
            }
        }

        private Animator prepareLocalAnimator(int index) {
            Animator animator = this.mAnimators.get(index);
            Animator localAnimator = animator.m35clone();
            String targetName = this.mTargetNameMap.get(animator);
            Object target = this.mVectorDrawable.getTargetByName(targetName);
            localAnimator.setTarget(target);
            return localAnimator;
        }

        public void inflatePendingAnimators(Resources res, Resources.Theme t) {
            ArrayList<PendingAnimator> pendingAnims = this.mPendingAnims;
            if (pendingAnims == null) {
                return;
            }
            this.mPendingAnims = null;
            int count = pendingAnims.size();
            for (int i = 0; i < count; i++) {
                PendingAnimator pendingAnimator = pendingAnims.get(i);
                Animator objectAnimator = pendingAnimator.newInstance(res, t);
                addTargetAnimator(pendingAnimator.target, objectAnimator);
            }
        }

        private static class PendingAnimator {
            public final int animResId;
            public final float pathErrorScale;
            public final String target;

            public PendingAnimator(int animResId, float pathErrorScale, String target) {
                this.animResId = animResId;
                this.pathErrorScale = pathErrorScale;
                this.target = target;
            }

            public Animator newInstance(Resources res, Resources.Theme theme) {
                return AnimatorInflater.loadAnimator(res, theme, this.animResId, this.pathErrorScale);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return this.mAnimatorSet.isRunning();
    }

    public void reset() {
        ensureAnimatorSet();
        this.mAnimatorSet.reset();
    }

    @Override
    public void start() {
        ensureAnimatorSet();
        this.mAnimatorSet.start();
    }

    private void ensureAnimatorSet() {
        if (this.mAnimatorSetFromXml != null) {
            return;
        }
        this.mAnimatorSetFromXml = new AnimatorSet();
        this.mAnimatedVectorState.prepareLocalAnimators(this.mAnimatorSetFromXml, this.mRes);
        this.mAnimatorSet.init(this.mAnimatorSetFromXml);
        this.mRes = null;
    }

    @Override
    public void stop() {
        this.mAnimatorSet.end();
    }

    public void reverse() {
        ensureAnimatorSet();
        if (!canReverse()) {
            Log.w(LOGTAG, "AnimatedVectorDrawable can't reverse()");
        } else {
            this.mAnimatorSet.reverse();
        }
    }

    public boolean canReverse() {
        return this.mAnimatorSet.canReverse();
    }

    @Override
    public void registerAnimationCallback(Animatable2.AnimationCallback callback) {
        if (callback == null) {
            return;
        }
        if (this.mAnimationCallbacks == null) {
            this.mAnimationCallbacks = new ArrayList<>();
        }
        this.mAnimationCallbacks.add(callback);
        if (this.mAnimatorListener == null) {
            this.mAnimatorListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ArrayList<Animatable2.AnimationCallback> tmpCallbacks = new ArrayList<>(AnimatedVectorDrawable.this.mAnimationCallbacks);
                    int size = tmpCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        tmpCallbacks.get(i).onAnimationStart(AnimatedVectorDrawable.this);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ArrayList<Animatable2.AnimationCallback> tmpCallbacks = new ArrayList<>(AnimatedVectorDrawable.this.mAnimationCallbacks);
                    int size = tmpCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        tmpCallbacks.get(i).onAnimationEnd(AnimatedVectorDrawable.this);
                    }
                }
            };
        }
        this.mAnimatorSet.setListener(this.mAnimatorListener);
    }

    private void removeAnimatorSetListener() {
        if (this.mAnimatorListener == null) {
            return;
        }
        this.mAnimatorSet.removeListener(this.mAnimatorListener);
        this.mAnimatorListener = null;
    }

    @Override
    public boolean unregisterAnimationCallback(Animatable2.AnimationCallback callback) {
        if (this.mAnimationCallbacks == null || callback == null) {
            return false;
        }
        boolean removed = this.mAnimationCallbacks.remove(callback);
        if (this.mAnimationCallbacks.size() == 0) {
            removeAnimatorSetListener();
        }
        return removed;
    }

    @Override
    public void clearAnimationCallbacks() {
        removeAnimatorSetListener();
        if (this.mAnimationCallbacks == null) {
            return;
        }
        this.mAnimationCallbacks.clear();
    }

    private static class VectorDrawableAnimatorUI implements VectorDrawableAnimator {
        private final Drawable mDrawable;
        private AnimatorSet mSet = null;
        private ArrayList<Animator.AnimatorListener> mListenerArray = null;
        private boolean mIsInfinite = false;

        VectorDrawableAnimatorUI(AnimatedVectorDrawable drawable) {
            this.mDrawable = drawable;
        }

        @Override
        public void init(AnimatorSet set) {
            if (this.mSet != null) {
                throw new UnsupportedOperationException("VectorDrawableAnimator cannot be re-initialized");
            }
            this.mSet = set.m35clone();
            this.mIsInfinite = this.mSet.getTotalDuration() == -1;
            if (this.mListenerArray == null || this.mListenerArray.isEmpty()) {
                return;
            }
            for (int i = 0; i < this.mListenerArray.size(); i++) {
                this.mSet.addListener(this.mListenerArray.get(i));
            }
            this.mListenerArray.clear();
            this.mListenerArray = null;
        }

        @Override
        public void start() {
            if (this.mSet == null || this.mSet.isStarted()) {
                return;
            }
            this.mSet.start();
            invalidateOwningView();
        }

        @Override
        public void end() {
            if (this.mSet == null) {
                return;
            }
            this.mSet.end();
        }

        @Override
        public void reset() {
            if (this.mSet == null) {
                return;
            }
            start();
            this.mSet.cancel();
        }

        @Override
        public void reverse() {
            if (this.mSet == null) {
                return;
            }
            this.mSet.reverse();
            invalidateOwningView();
        }

        @Override
        public boolean canReverse() {
            if (this.mSet != null) {
                return this.mSet.canReverse();
            }
            return false;
        }

        @Override
        public void setListener(Animator.AnimatorListener listener) {
            if (this.mSet == null) {
                if (this.mListenerArray == null) {
                    this.mListenerArray = new ArrayList<>();
                }
                this.mListenerArray.add(listener);
                return;
            }
            this.mSet.addListener(listener);
        }

        @Override
        public void removeListener(Animator.AnimatorListener listener) {
            if (this.mSet == null) {
                if (this.mListenerArray == null) {
                    return;
                }
                this.mListenerArray.remove(listener);
                return;
            }
            this.mSet.removeListener(listener);
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (this.mSet == null || !this.mSet.isStarted()) {
                return;
            }
            invalidateOwningView();
        }

        @Override
        public boolean isStarted() {
            if (this.mSet != null) {
                return this.mSet.isStarted();
            }
            return false;
        }

        @Override
        public boolean isRunning() {
            if (this.mSet != null) {
                return this.mSet.isRunning();
            }
            return false;
        }

        @Override
        public boolean isInfinite() {
            return this.mIsInfinite;
        }

        @Override
        public void pause() {
            if (this.mSet == null) {
                return;
            }
            this.mSet.pause();
        }

        @Override
        public void resume() {
            if (this.mSet == null) {
                return;
            }
            this.mSet.resume();
        }

        private void invalidateOwningView() {
            this.mDrawable.invalidateSelf();
        }
    }

    public static class VectorDrawableAnimatorRT implements VectorDrawableAnimator {
        private static final int END_ANIMATION = 4;
        private static final int RESET_ANIMATION = 3;
        private static final int REVERSE_ANIMATION = 2;
        private static final int START_ANIMATION = 1;
        private final Drawable mDrawable;
        private long mSetPtr;
        private final VirtualRefBasePtr mSetRefBasePtr;
        private boolean mShouldIgnoreInvalidAnim;
        private Animator.AnimatorListener mListener = null;
        private final LongArray mStartDelays = new LongArray();
        private PropertyValuesHolder.PropertyValues mTmpValues = new PropertyValuesHolder.PropertyValues();
        private boolean mContainsSequentialAnimators = false;
        private boolean mStarted = false;
        private boolean mInitialized = false;
        private boolean mIsReversible = false;
        private boolean mIsInfinite = false;
        private WeakReference<RenderNode> mLastSeenTarget = null;
        private int mLastListenerId = 0;
        private final IntArray mPendingAnimationActions = new IntArray();

        VectorDrawableAnimatorRT(AnimatedVectorDrawable drawable) {
            this.mSetPtr = 0L;
            this.mDrawable = drawable;
            this.mSetPtr = AnimatedVectorDrawable.nCreateAnimatorSet();
            this.mSetRefBasePtr = new VirtualRefBasePtr(this.mSetPtr);
        }

        @Override
        public void init(AnimatorSet set) {
            if (this.mInitialized) {
                throw new UnsupportedOperationException("VectorDrawableAnimator cannot be re-initialized");
            }
            this.mShouldIgnoreInvalidAnim = AnimatedVectorDrawable.shouldIgnoreInvalidAnimation();
            parseAnimatorSet(set, 0L);
            this.mInitialized = true;
            this.mIsInfinite = set.getTotalDuration() == -1;
            this.mIsReversible = true;
            if (this.mContainsSequentialAnimators) {
                this.mIsReversible = false;
                return;
            }
            for (int i = 0; i < this.mStartDelays.size(); i++) {
                if (this.mStartDelays.get(i) > 0) {
                    this.mIsReversible = false;
                    return;
                }
            }
        }

        private void parseAnimatorSet(AnimatorSet set, long startTime) {
            ArrayList<Animator> animators = set.getChildAnimations();
            boolean playTogether = set.shouldPlayTogether();
            for (int i = 0; i < animators.size(); i++) {
                Animator animator = animators.get(i);
                if (animator instanceof AnimatorSet) {
                    parseAnimatorSet((AnimatorSet) animator, startTime);
                } else if (animator instanceof ObjectAnimator) {
                    createRTAnimator((ObjectAnimator) animator, startTime);
                }
                if (!playTogether) {
                    startTime += animator.getTotalDuration();
                    this.mContainsSequentialAnimators = true;
                }
            }
        }

        private void createRTAnimator(ObjectAnimator animator, long startTime) {
            PropertyValuesHolder[] values = animator.getValues();
            Object target = animator.getTarget();
            if (target instanceof VectorDrawable.VGroup) {
                createRTAnimatorForGroup(values, animator, (VectorDrawable.VGroup) target, startTime);
                return;
            }
            if (target instanceof VectorDrawable.VPath) {
                for (PropertyValuesHolder propertyValuesHolder : values) {
                    propertyValuesHolder.getPropertyValues(this.mTmpValues);
                    if ((this.mTmpValues.endValue instanceof PathParser.PathData) && this.mTmpValues.propertyName.equals("pathData")) {
                        createRTAnimatorForPath(animator, (VectorDrawable.VPath) target, startTime);
                    } else if (target instanceof VectorDrawable.VFullPath) {
                        createRTAnimatorForFullPath(animator, (VectorDrawable.VFullPath) target, startTime);
                    } else if (!this.mShouldIgnoreInvalidAnim) {
                        throw new IllegalArgumentException("ClipPath only supports PathData property");
                    }
                }
                return;
            }
            if (target instanceof VectorDrawable.VectorDrawableState) {
                createRTAnimatorForRootGroup(values, animator, (VectorDrawable.VectorDrawableState) target, startTime);
            } else {
                if (!this.mShouldIgnoreInvalidAnim) {
                    throw new UnsupportedOperationException(new StringBuilder().append("Target should be either VGroup, VPath, or ConstantState, ").append(target).toString() == null ? "Null target" : target.getClass() + " is not supported");
                }
            }
        }

        private void createRTAnimatorForGroup(PropertyValuesHolder[] values, ObjectAnimator animator, VectorDrawable.VGroup target, long startTime) {
            long nativePtr = target.getNativePtr();
            for (PropertyValuesHolder propertyValuesHolder : values) {
                propertyValuesHolder.getPropertyValues(this.mTmpValues);
                int propertyId = VectorDrawable.VGroup.getPropertyIndex(this.mTmpValues.propertyName);
                if ((this.mTmpValues.type == Float.class || this.mTmpValues.type == Float.TYPE) && propertyId >= 0) {
                    long propertyPtr = AnimatedVectorDrawable.nCreateGroupPropertyHolder(nativePtr, propertyId, ((Float) this.mTmpValues.startValue).floatValue(), ((Float) this.mTmpValues.endValue).floatValue());
                    if (this.mTmpValues.dataSource != null) {
                        float[] dataPoints = createDataPoints(this.mTmpValues.dataSource, animator.getDuration());
                        AnimatedVectorDrawable.nSetPropertyHolderData(propertyPtr, dataPoints, dataPoints.length);
                    }
                    createNativeChildAnimator(propertyPtr, startTime, animator);
                }
            }
        }

        private void createRTAnimatorForPath(ObjectAnimator animator, VectorDrawable.VPath target, long startTime) {
            long nativePtr = target.getNativePtr();
            long startPathDataPtr = ((PathParser.PathData) this.mTmpValues.startValue).getNativePtr();
            long endPathDataPtr = ((PathParser.PathData) this.mTmpValues.endValue).getNativePtr();
            long propertyPtr = AnimatedVectorDrawable.nCreatePathDataPropertyHolder(nativePtr, startPathDataPtr, endPathDataPtr);
            createNativeChildAnimator(propertyPtr, startTime, animator);
        }

        private void createRTAnimatorForFullPath(ObjectAnimator animator, VectorDrawable.VFullPath target, long startTime) {
            long propertyPtr;
            int propertyId = target.getPropertyIndex(this.mTmpValues.propertyName);
            long nativePtr = target.getNativePtr();
            if (this.mTmpValues.type == Float.class || this.mTmpValues.type == Float.TYPE) {
                if (propertyId < 0) {
                    if (!this.mShouldIgnoreInvalidAnim) {
                        throw new IllegalArgumentException("Property: " + this.mTmpValues.propertyName + " is not supported for FullPath");
                    }
                    return;
                }
                propertyPtr = AnimatedVectorDrawable.nCreatePathPropertyHolder(nativePtr, propertyId, ((Float) this.mTmpValues.startValue).floatValue(), ((Float) this.mTmpValues.endValue).floatValue());
            } else {
                if (this.mTmpValues.type != Integer.class && this.mTmpValues.type != Integer.TYPE) {
                    if (!this.mShouldIgnoreInvalidAnim) {
                        throw new UnsupportedOperationException("Unsupported type: " + this.mTmpValues.type + ". Only float, int or PathData value is supported for Paths.");
                    }
                    return;
                }
                propertyPtr = AnimatedVectorDrawable.nCreatePathColorPropertyHolder(nativePtr, propertyId, ((Integer) this.mTmpValues.startValue).intValue(), ((Integer) this.mTmpValues.endValue).intValue());
            }
            if (this.mTmpValues.dataSource != null) {
                float[] dataPoints = createDataPoints(this.mTmpValues.dataSource, animator.getDuration());
                AnimatedVectorDrawable.nSetPropertyHolderData(propertyPtr, dataPoints, dataPoints.length);
            }
            createNativeChildAnimator(propertyPtr, startTime, animator);
        }

        private void createRTAnimatorForRootGroup(PropertyValuesHolder[] values, ObjectAnimator animator, VectorDrawable.VectorDrawableState target, long startTime) {
            long nativePtr = target.getNativeRenderer();
            if (!animator.getPropertyName().equals("alpha")) {
                if (this.mShouldIgnoreInvalidAnim) {
                    return;
                } else {
                    throw new UnsupportedOperationException("Only alpha is supported for root group");
                }
            }
            Float startValue = null;
            Float endValue = null;
            int i = 0;
            while (true) {
                if (i >= values.length) {
                    break;
                }
                values[i].getPropertyValues(this.mTmpValues);
                if (!this.mTmpValues.propertyName.equals("alpha")) {
                    i++;
                } else {
                    startValue = (Float) this.mTmpValues.startValue;
                    endValue = (Float) this.mTmpValues.endValue;
                    break;
                }
            }
            if (startValue == null && endValue == null) {
                if (this.mShouldIgnoreInvalidAnim) {
                } else {
                    throw new UnsupportedOperationException("No alpha values are specified");
                }
            } else {
                long propertyPtr = AnimatedVectorDrawable.nCreateRootAlphaPropertyHolder(nativePtr, startValue.floatValue(), endValue.floatValue());
                createNativeChildAnimator(propertyPtr, startTime, animator);
            }
        }

        private static float[] createDataPoints(PropertyValuesHolder.PropertyValues.DataSource dataSource, long duration) {
            long frameIntervalNanos = Choreographer.getInstance().getFrameIntervalNanos();
            int animIntervalMs = (int) (frameIntervalNanos / 1000000);
            int numAnimFrames = (int) Math.ceil(duration / ((double) animIntervalMs));
            float[] values = new float[numAnimFrames];
            float lastFrame = numAnimFrames - 1;
            for (int i = 0; i < numAnimFrames; i++) {
                float fraction = i / lastFrame;
                values[i] = ((Float) dataSource.getValueAtFraction(fraction)).floatValue();
            }
            return values;
        }

        private void createNativeChildAnimator(long propertyPtr, long extraDelay, ObjectAnimator animator) {
            long duration = animator.getDuration();
            int repeatCount = animator.getRepeatCount();
            long startDelay = extraDelay + animator.getStartDelay();
            TimeInterpolator interpolator = animator.getInterpolator();
            long nativeInterpolator = RenderNodeAnimatorSetHelper.createNativeInterpolator(interpolator, duration);
            long startDelay2 = (long) (startDelay * ValueAnimator.getDurationScale());
            long duration2 = (long) (duration * ValueAnimator.getDurationScale());
            this.mStartDelays.add(startDelay2);
            AnimatedVectorDrawable.nAddAnimator(this.mSetPtr, propertyPtr, nativeInterpolator, startDelay2, duration2, repeatCount);
        }

        protected void recordLastSeenTarget(DisplayListCanvas canvas) {
            this.mLastSeenTarget = new WeakReference<>(RenderNodeAnimatorSetHelper.getTarget(canvas));
            if (this.mPendingAnimationActions.size() <= 0 || !useLastSeenTarget()) {
                return;
            }
            for (int i = 0; i < this.mPendingAnimationActions.size(); i++) {
                handlePendingAction(this.mPendingAnimationActions.get(i));
            }
            this.mPendingAnimationActions.clear();
        }

        private void handlePendingAction(int pendingAnimationAction) {
            if (pendingAnimationAction == 1) {
                startAnimation();
                return;
            }
            if (pendingAnimationAction == 2) {
                reverseAnimation();
            } else if (pendingAnimationAction == 3) {
                resetAnimation();
            } else {
                if (pendingAnimationAction == 4) {
                    endAnimation();
                    return;
                }
                throw new UnsupportedOperationException("Animation action " + pendingAnimationAction + "is not supported");
            }
        }

        private boolean useLastSeenTarget() {
            RenderNode target;
            if (this.mLastSeenTarget != null && (target = this.mLastSeenTarget.get()) != null && target.isAttached()) {
                target.addAnimator(this);
                return true;
            }
            return false;
        }

        private void invalidateOwningView() {
            this.mDrawable.invalidateSelf();
        }

        private void addPendingAction(int pendingAnimationAction) {
            invalidateOwningView();
            this.mPendingAnimationActions.add(pendingAnimationAction);
        }

        @Override
        public void start() {
            if (!this.mInitialized) {
                return;
            }
            if (useLastSeenTarget()) {
                startAnimation();
            } else {
                addPendingAction(1);
            }
        }

        @Override
        public void end() {
            if (!this.mInitialized) {
                return;
            }
            if (useLastSeenTarget()) {
                endAnimation();
            } else {
                addPendingAction(4);
            }
        }

        @Override
        public void reset() {
            if (!this.mInitialized) {
                return;
            }
            if (useLastSeenTarget()) {
                resetAnimation();
            } else {
                addPendingAction(3);
            }
        }

        @Override
        public void reverse() {
            if (!this.mIsReversible || !this.mInitialized) {
                return;
            }
            if (useLastSeenTarget()) {
                reverseAnimation();
            } else {
                addPendingAction(2);
            }
        }

        private void startAnimation() {
            this.mStarted = true;
            long j = this.mSetPtr;
            int i = this.mLastListenerId + 1;
            this.mLastListenerId = i;
            AnimatedVectorDrawable.nStart(j, this, i);
            invalidateOwningView();
            if (this.mListener == null) {
                return;
            }
            this.mListener.onAnimationStart(null);
        }

        private void endAnimation() {
            AnimatedVectorDrawable.nEnd(this.mSetPtr);
            invalidateOwningView();
        }

        private void resetAnimation() {
            AnimatedVectorDrawable.nReset(this.mSetPtr);
            invalidateOwningView();
        }

        private void reverseAnimation() {
            this.mStarted = true;
            long j = this.mSetPtr;
            int i = this.mLastListenerId + 1;
            this.mLastListenerId = i;
            AnimatedVectorDrawable.nReverse(j, this, i);
            invalidateOwningView();
            if (this.mListener == null) {
                return;
            }
            this.mListener.onAnimationStart(null);
        }

        public long getAnimatorNativePtr() {
            return this.mSetPtr;
        }

        @Override
        public boolean canReverse() {
            return this.mIsReversible;
        }

        @Override
        public boolean isStarted() {
            return this.mStarted;
        }

        @Override
        public boolean isRunning() {
            if (!this.mInitialized) {
                return false;
            }
            return this.mStarted;
        }

        @Override
        public void setListener(Animator.AnimatorListener listener) {
            this.mListener = listener;
        }

        @Override
        public void removeListener(Animator.AnimatorListener listener) {
            this.mListener = null;
        }

        @Override
        public void onDraw(Canvas canvas) {
            if (!canvas.isHardwareAccelerated()) {
                return;
            }
            recordLastSeenTarget((DisplayListCanvas) canvas);
        }

        @Override
        public boolean isInfinite() {
            return this.mIsInfinite;
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }

        private void onAnimationEnd(int listenerId) {
            if (listenerId != this.mLastListenerId) {
                return;
            }
            this.mStarted = false;
            invalidateOwningView();
            if (this.mListener == null) {
                return;
            }
            this.mListener.onAnimationEnd(null);
        }

        private static void callOnFinished(VectorDrawableAnimatorRT set, int id) {
            set.onAnimationEnd(id);
        }
    }
}
