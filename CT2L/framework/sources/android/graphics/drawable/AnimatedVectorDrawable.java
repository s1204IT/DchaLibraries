package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatedVectorDrawable extends Drawable implements Animatable {
    private static final String ANIMATED_VECTOR = "animated-vector";
    private static final boolean DBG_ANIMATION_VECTOR_DRAWABLE = false;
    private static final String LOGTAG = AnimatedVectorDrawable.class.getSimpleName();
    private static final String TARGET = "target";
    private AnimatedVectorDrawableState mAnimatedVectorState;
    private final Drawable.Callback mCallback;
    private boolean mMutated;

    public AnimatedVectorDrawable() {
        this(null, null);
    }

    private AnimatedVectorDrawable(AnimatedVectorDrawableState state, Resources res) {
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
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mAnimatedVectorState = new AnimatedVectorDrawableState(this.mAnimatedVectorState, this.mCallback, null);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mAnimatedVectorState.mVectorDrawable.clearMutated();
        this.mMutated = false;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mAnimatedVectorState.mChangingConfigurations = getChangingConfigurations();
        return this.mAnimatedVectorState;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mAnimatedVectorState.mChangingConfigurations;
    }

    @Override
    public void draw(Canvas canvas) {
        this.mAnimatedVectorState.mVectorDrawable.draw(canvas);
        if (isStarted()) {
            invalidateSelf();
        }
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
        this.mAnimatedVectorState.mVectorDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        this.mAnimatedVectorState.mVectorDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    public boolean isStateful() {
        return this.mAnimatedVectorState.mVectorDrawable.isStateful();
    }

    @Override
    public int getOpacity() {
        return this.mAnimatedVectorState.mVectorDrawable.getOpacity();
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
    public void inflate(Resources res, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
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
                        if (this.mAnimatedVectorState.mVectorDrawable != null) {
                            this.mAnimatedVectorState.mVectorDrawable.setCallback(null);
                        }
                        this.mAnimatedVectorState.mVectorDrawable = vectorDrawable;
                    }
                    a.recycle();
                } else if (TARGET.equals(tagName)) {
                    TypedArray a2 = obtainAttributes(res, theme, attrs, R.styleable.AnimatedVectorDrawableTarget);
                    String target = a2.getString(0);
                    int id = a2.getResourceId(1, 0);
                    if (id != 0) {
                        Animator objectAnimator = AnimatorInflater.loadAnimator(res, theme, id, pathErrorScale);
                        setupAnimatorsForTarget(target, objectAnimator);
                    }
                    a2.recycle();
                }
            }
            eventType = parser.next();
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mAnimatedVectorState != null && this.mAnimatedVectorState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        VectorDrawable vectorDrawable = this.mAnimatedVectorState.mVectorDrawable;
        if (vectorDrawable != null && vectorDrawable.canApplyTheme()) {
            vectorDrawable.applyTheme(t);
        }
    }

    private static class AnimatedVectorDrawableState extends Drawable.ConstantState {
        ArrayList<Animator> mAnimators;
        int mChangingConfigurations;
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
                    int numAnimators = copy.mAnimators.size();
                    this.mAnimators = new ArrayList<>(numAnimators);
                    this.mTargetNameMap = new ArrayMap<>(numAnimators);
                    for (int i = 0; i < numAnimators; i++) {
                        Animator anim = copy.mAnimators.get(i);
                        Animator animClone = anim.mo0clone();
                        String targetName = copy.mTargetNameMap.get(anim);
                        Object targetObject = this.mVectorDrawable.getTargetByName(targetName);
                        animClone.setTarget(targetObject);
                        this.mAnimators.add(animClone);
                        this.mTargetNameMap.put(animClone, targetName);
                    }
                    return;
                }
                return;
            }
            this.mVectorDrawable = new VectorDrawable();
        }

        @Override
        public boolean canApplyTheme() {
            return (this.mVectorDrawable != null && this.mVectorDrawable.canApplyTheme()) || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new AnimatedVectorDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new AnimatedVectorDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }
    }

    private void setupAnimatorsForTarget(String name, Animator animator) {
        Object target = this.mAnimatedVectorState.mVectorDrawable.getTargetByName(name);
        animator.setTarget(target);
        if (this.mAnimatedVectorState.mAnimators == null) {
            this.mAnimatedVectorState.mAnimators = new ArrayList<>();
            this.mAnimatedVectorState.mTargetNameMap = new ArrayMap<>();
        }
        this.mAnimatedVectorState.mAnimators.add(animator);
        this.mAnimatedVectorState.mTargetNameMap.put(animator, name);
    }

    @Override
    public boolean isRunning() {
        ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
        int size = animators.size();
        for (int i = 0; i < size; i++) {
            Animator animator = animators.get(i);
            if (animator.isRunning()) {
                return true;
            }
        }
        return false;
    }

    private boolean isStarted() {
        ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
        int size = animators.size();
        for (int i = 0; i < size; i++) {
            Animator animator = animators.get(i);
            if (animator.isStarted()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (!isStarted()) {
            ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
            int size = animators.size();
            for (int i = 0; i < size; i++) {
                Animator animator = animators.get(i);
                animator.start();
            }
            invalidateSelf();
        }
    }

    @Override
    public void stop() {
        ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
        int size = animators.size();
        for (int i = 0; i < size; i++) {
            Animator animator = animators.get(i);
            animator.end();
        }
    }

    public void reverse() {
        if (!canReverse()) {
            Log.w(LOGTAG, "AnimatedVectorDrawable can't reverse()");
            return;
        }
        ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
        int size = animators.size();
        for (int i = 0; i < size; i++) {
            Animator animator = animators.get(i);
            animator.reverse();
        }
    }

    public boolean canReverse() {
        ArrayList<Animator> animators = this.mAnimatedVectorState.mAnimators;
        int size = animators.size();
        for (int i = 0; i < size; i++) {
            Animator animator = animators.get(i);
            if (!animator.canReverse()) {
                return false;
            }
        }
        return true;
    }
}
