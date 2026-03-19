package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.WindowManager;
import android.view.WindowManagerInternal;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ClipRectAnimation;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.internal.util.DumpUtils;
import com.android.server.AttributeCache;
import com.android.server.wm.animation.ClipRectLRAnimation;
import com.android.server.wm.animation.ClipRectTBAnimation;
import com.android.server.wm.animation.CurvedTranslateAnimation;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppTransition implements DumpUtils.Dump {
    private static final int APP_STATE_IDLE = 0;
    private static final int APP_STATE_READY = 1;
    private static final int APP_STATE_RUNNING = 2;
    private static final int APP_STATE_TIMEOUT = 3;
    private static final long APP_TRANSITION_TIMEOUT_MS = 5000;
    private static final int CLIP_REVEAL_TRANSLATION_Y_DP = 8;
    static final int DEFAULT_APP_TRANSITION_DURATION = 336;
    private static final int MAX_CLIP_REVEAL_TRANSITION_DURATION = 420;
    private static final int NEXT_TRANSIT_TYPE_CLIP_REVEAL = 8;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM = 1;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE = 7;
    private static final int NEXT_TRANSIT_TYPE_NONE = 0;
    private static final int NEXT_TRANSIT_TYPE_SCALE_UP = 2;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN = 6;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP = 5;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN = 4;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP = 3;
    private static final float RECENTS_THUMBNAIL_FADEIN_FRACTION = 0.5f;
    private static final float RECENTS_THUMBNAIL_FADEOUT_FRACTION = 0.5f;
    private static final int THUMBNAIL_APP_TRANSITION_DURATION = 336;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_DOWN = 2;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_UP = 0;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_DOWN = 3;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_UP = 1;
    public static final int TRANSIT_ACTIVITY_CLOSE = 7;
    public static final int TRANSIT_ACTIVITY_OPEN = 6;
    public static final int TRANSIT_ACTIVITY_RELAUNCH = 18;
    public static final int TRANSIT_DOCK_TASK_FROM_RECENTS = 19;
    public static final int TRANSIT_NONE = 0;
    public static final int TRANSIT_TASK_CLOSE = 9;
    public static final int TRANSIT_TASK_IN_PLACE = 17;
    public static final int TRANSIT_TASK_OPEN = 8;
    public static final int TRANSIT_TASK_OPEN_BEHIND = 16;
    public static final int TRANSIT_TASK_TO_BACK = 11;
    public static final int TRANSIT_TASK_TO_FRONT = 10;
    public static final int TRANSIT_UNSET = -1;
    public static final int TRANSIT_WALLPAPER_CLOSE = 12;
    public static final int TRANSIT_WALLPAPER_INTRA_CLOSE = 15;
    public static final int TRANSIT_WALLPAPER_INTRA_OPEN = 14;
    public static final int TRANSIT_WALLPAPER_OPEN = 13;
    private IRemoteCallback mAnimationFinishedCallback;
    private final int mClipRevealTranslationY;
    private final int mConfigShortAnimTime;
    private final Context mContext;
    private final Interpolator mDecelerateInterpolator;
    private AppTransitionAnimationSpec mDefaultNextAppTransitionAnimationSpec;
    private final Interpolator mFastOutLinearInInterpolator;
    private final Interpolator mFastOutSlowInInterpolator;
    private int mLastClipRevealMaxTranslation;
    private boolean mLastHadClipReveal;
    private final Interpolator mLinearOutSlowInInterpolator;
    private IAppTransitionAnimationSpecsFuture mNextAppTransitionAnimationsSpecsFuture;
    private boolean mNextAppTransitionAnimationsSpecsPending;
    private IRemoteCallback mNextAppTransitionCallback;
    private int mNextAppTransitionEnter;
    private int mNextAppTransitionExit;
    private IRemoteCallback mNextAppTransitionFutureCallback;
    private int mNextAppTransitionInPlace;
    private String mNextAppTransitionPackage;
    private boolean mNextAppTransitionScaleUp;
    private boolean mProlongedAnimationsEnded;
    private final WindowManagerService mService;
    private static final String TAG = "WindowManager";
    static final Interpolator TOUCH_RESPONSE_INTERPOLATOR = new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
    private static final Interpolator THUMBNAIL_DOCK_INTERPOLATOR = new PathInterpolator(0.85f, 0.0f, 1.0f, 1.0f);
    private int mNextAppTransition = -1;
    private int mNextAppTransitionType = 0;
    private final SparseArray<AppTransitionAnimationSpec> mNextAppTransitionAnimationsSpecs = new SparseArray<>();
    private Rect mNextAppTransitionInsets = new Rect();
    private Rect mTmpFromClipRect = new Rect();
    private Rect mTmpToClipRect = new Rect();
    private final Rect mTmpRect = new Rect();
    private int mAppTransitionState = 0;
    private final Interpolator mClipHorizontalInterpolator = new PathInterpolator(0.0f, 0.0f, 0.4f, 1.0f);
    private int mCurrentUserId = 0;
    private long mLastClipRevealTransitionDuration = 336;
    private final ArrayList<WindowManagerInternal.AppTransitionListener> mListeners = new ArrayList<>();
    private final ExecutorService mDefaultExecutor = Executors.newSingleThreadExecutor();
    private final Interpolator mThumbnailFadeInInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if (input < 0.5f) {
                return 0.0f;
            }
            float t = (input - 0.5f) / 0.5f;
            return AppTransition.this.mFastOutLinearInInterpolator.getInterpolation(t);
        }
    };
    private final Interpolator mThumbnailFadeOutInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if (input < 0.5f) {
                float t = input / 0.5f;
                return AppTransition.this.mLinearOutSlowInInterpolator.getInterpolation(t);
            }
            return 1.0f;
        }
    };

    AppTransition(Context context, WindowManagerService service) {
        this.mContext = context;
        this.mService = service;
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.mConfigShortAnimTime = context.getResources().getInteger(R.integer.config_shortAnimTime);
        this.mDecelerateInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.decelerate_cubic);
        this.mClipRevealTranslationY = (int) (this.mContext.getResources().getDisplayMetrics().density * 8.0f);
    }

    boolean isTransitionSet() {
        return this.mNextAppTransition != -1;
    }

    boolean isTransitionEqual(int transit) {
        return this.mNextAppTransition == transit;
    }

    int getAppTransition() {
        return this.mNextAppTransition;
    }

    private void setAppTransition(int transit) {
        this.mNextAppTransition = transit;
    }

    boolean isReady() {
        return this.mAppTransitionState == 1 || this.mAppTransitionState == 3;
    }

    void setReady() {
        this.mAppTransitionState = 1;
        fetchAppTransitionSpecsFromFuture();
    }

    boolean isRunning() {
        return this.mAppTransitionState == 2;
    }

    void setIdle() {
        this.mAppTransitionState = 0;
    }

    boolean isTimeout() {
        return this.mAppTransitionState == 3;
    }

    void setTimeout() {
        this.mAppTransitionState = 3;
    }

    Bitmap getAppTransitionThumbnailHeader(int taskId) {
        AppTransitionAnimationSpec spec = this.mNextAppTransitionAnimationsSpecs.get(taskId);
        if (spec == null) {
            spec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (spec != null) {
            return spec.bitmap;
        }
        return null;
    }

    boolean isNextThumbnailTransitionAspectScaled() {
        return this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6;
    }

    boolean isNextThumbnailTransitionScaleUp() {
        return this.mNextAppTransitionScaleUp;
    }

    boolean isNextAppTransitionThumbnailUp() {
        return this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 5;
    }

    boolean isNextAppTransitionThumbnailDown() {
        return this.mNextAppTransitionType == 4 || this.mNextAppTransitionType == 6;
    }

    boolean isFetchingAppTransitionsSpecs() {
        return this.mNextAppTransitionAnimationsSpecsPending;
    }

    private boolean prepare() {
        if (isRunning()) {
            return false;
        }
        this.mAppTransitionState = 0;
        notifyAppTransitionPendingLocked();
        this.mLastHadClipReveal = false;
        this.mLastClipRevealMaxTranslation = 0;
        this.mLastClipRevealTransitionDuration = 336L;
        return true;
    }

    void goodToGo(AppWindowAnimator topOpeningAppAnimator, AppWindowAnimator topClosingAppAnimator, ArraySet<AppWindowToken> openingApps, ArraySet<AppWindowToken> closingApps) {
        this.mNextAppTransition = -1;
        this.mAppTransitionState = 2;
        notifyAppTransitionStartingLocked(topOpeningAppAnimator != null ? topOpeningAppAnimator.mAppToken.token : null, topClosingAppAnimator != null ? topClosingAppAnimator.mAppToken.token : null, topOpeningAppAnimator != null ? topOpeningAppAnimator.animation : null, topClosingAppAnimator != null ? topClosingAppAnimator.animation : null);
        this.mService.getDefaultDisplayContentLocked().getDockedDividerController().notifyAppTransitionStarting();
        if (this.mNextAppTransition != 19 || this.mProlongedAnimationsEnded) {
            return;
        }
        for (int i = openingApps.size() - 1; i >= 0; i--) {
            AppWindowAnimator appAnimator = openingApps.valueAt(i).mAppAnimator;
            appAnimator.startProlongAnimation(2);
        }
    }

    void notifyProlongedAnimationsEnded() {
        this.mProlongedAnimationsEnded = true;
    }

    void clear() {
        this.mNextAppTransitionType = 0;
        this.mNextAppTransitionPackage = null;
        this.mNextAppTransitionAnimationsSpecs.clear();
        this.mNextAppTransitionAnimationsSpecsFuture = null;
        this.mDefaultNextAppTransitionAnimationSpec = null;
        this.mAnimationFinishedCallback = null;
        this.mProlongedAnimationsEnded = false;
    }

    void freeze() {
        setAppTransition(-1);
        clear();
        setReady();
        notifyAppTransitionCancelledLocked();
    }

    void registerListenerLocked(WindowManagerInternal.AppTransitionListener listener) {
        this.mListeners.add(listener);
    }

    public void notifyAppTransitionFinishedLocked(IBinder token) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionFinishedLocked(token);
        }
    }

    private void notifyAppTransitionPendingLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionPendingLocked();
        }
    }

    private void notifyAppTransitionCancelledLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionCancelledLocked();
        }
    }

    private void notifyAppTransitionStartingLocked(IBinder openToken, IBinder closeToken, Animation openAnimation, Animation closeAnimation) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionStartingLocked(openToken, closeToken, openAnimation, closeAnimation);
        }
    }

    private AttributeCache.Entry getCachedAnimations(WindowManager.LayoutParams lp) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Loading animations: layout params pkg=" + (lp != null ? lp.packageName : null) + " resId=0x" + (lp != null ? Integer.toHexString(lp.windowAnimations) : null));
        }
        if (lp == null || lp.windowAnimations == 0) {
            return null;
        }
        String packageName = lp.packageName != null ? lp.packageName : "android";
        int resId = lp.windowAnimations;
        if (((-16777216) & resId) == 16777216) {
            packageName = "android";
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Loading animations: picked package=" + packageName);
        }
        return AttributeCache.instance().get(packageName, resId, com.android.internal.R.styleable.WindowAnimation, this.mCurrentUserId);
    }

    private AttributeCache.Entry getCachedAnimations(String packageName, int resId) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Loading animations: package=" + packageName + " resId=0x" + Integer.toHexString(resId));
        }
        if (packageName == null) {
            return null;
        }
        if (((-16777216) & resId) == 16777216) {
            packageName = "android";
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Loading animations: picked package=" + packageName);
        }
        return AttributeCache.instance().get(packageName, resId, com.android.internal.R.styleable.WindowAnimation, this.mCurrentUserId);
    }

    Animation loadAnimationAttr(WindowManager.LayoutParams lp, int animAttr) {
        AttributeCache.Entry ent;
        int anim = 0;
        Context context = this.mContext;
        if (animAttr >= 0 && (ent = getCachedAnimations(lp)) != null) {
            context = ent.context;
            anim = ent.array.getResourceId(animAttr, 0);
        }
        if (anim != 0) {
            return AnimationUtils.loadAnimation(context, anim);
        }
        return null;
    }

    Animation loadAnimationRes(WindowManager.LayoutParams lp, int resId) {
        Context context = this.mContext;
        if (resId < 0) {
            return null;
        }
        AttributeCache.Entry ent = getCachedAnimations(lp);
        if (ent != null) {
            context = ent.context;
        }
        return AnimationUtils.loadAnimation(context, resId);
    }

    private Animation loadAnimationRes(String packageName, int resId) {
        AttributeCache.Entry ent;
        int anim = 0;
        Context context = this.mContext;
        if (resId >= 0 && (ent = getCachedAnimations(packageName, resId)) != null) {
            context = ent.context;
            anim = resId;
        }
        if (anim != 0) {
            return AnimationUtils.loadAnimation(context, anim);
        }
        return null;
    }

    private static float computePivot(int startPos, float finalScale) {
        float denom = finalScale - 1.0f;
        if (Math.abs(denom) < 1.0E-4f) {
            return startPos;
        }
        return (-startPos) / denom;
    }

    private Animation createScaleUpAnimationLocked(int transit, boolean enter, Rect containingFrame) {
        Animation a;
        long duration;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        if (enter) {
            float scaleW = this.mTmpRect.width() / appWidth;
            float scaleH = this.mTmpRect.height() / appHeight;
            Animation scale = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mTmpRect.left, scaleW), computePivot(this.mTmpRect.right, scaleH));
            scale.setInterpolator(this.mDecelerateInterpolator);
            Animation alpha = new AlphaAnimation(0.0f, 1.0f);
            alpha.setInterpolator(this.mThumbnailFadeOutInterpolator);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            set.setDetachWallpaper(true);
            a = set;
        } else if (transit == 14 || transit == 15) {
            a = new AlphaAnimation(1.0f, 0.0f);
            a.setDetachWallpaper(true);
        } else {
            a = new AlphaAnimation(1.0f, 1.0f);
        }
        switch (transit) {
            case 6:
            case 7:
                duration = this.mConfigShortAnimTime;
                break;
            default:
                duration = 336;
                break;
        }
        a.setDuration(duration);
        a.setFillAfter(true);
        a.setInterpolator(this.mDecelerateInterpolator);
        a.initialize(appWidth, appHeight, appWidth, appHeight);
        return a;
    }

    private void getDefaultNextAppTransitionStartRect(Rect rect) {
        if (this.mDefaultNextAppTransitionAnimationSpec == null || this.mDefaultNextAppTransitionAnimationSpec.rect == null) {
            Slog.e(TAG, "Starting rect for app requested, but none available", new Throwable());
            rect.setEmpty();
        } else {
            rect.set(this.mDefaultNextAppTransitionAnimationSpec.rect);
        }
    }

    void getNextAppTransitionStartRect(int taskId, Rect rect) {
        AppTransitionAnimationSpec spec = this.mNextAppTransitionAnimationsSpecs.get(taskId);
        if (spec == null) {
            spec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (spec == null || spec.rect == null) {
            Slog.wtf(TAG, "Starting rect for task: " + taskId + " requested, but not available", new Throwable());
            rect.setEmpty();
        } else {
            rect.set(spec.rect);
        }
    }

    private void putDefaultNextAppTransitionCoordinates(int left, int top, int width, int height, Bitmap bitmap) {
        this.mDefaultNextAppTransitionAnimationSpec = new AppTransitionAnimationSpec(-1, bitmap, new Rect(left, top, left + width, top + height));
    }

    long getLastClipRevealTransitionDuration() {
        return this.mLastClipRevealTransitionDuration;
    }

    int getLastClipRevealMaxTranslation() {
        return this.mLastClipRevealMaxTranslation;
    }

    boolean hadClipRevealAnimation() {
        return this.mLastHadClipReveal;
    }

    private long calculateClipRevealTransitionDuration(boolean cutOff, float translationX, float translationY, Rect displayFrame) {
        if (!cutOff) {
            return 336L;
        }
        float fraction = Math.max(Math.abs(translationX) / displayFrame.width(), Math.abs(translationY) / displayFrame.height());
        return (long) ((84.0f * fraction) + 336.0f);
    }

    private Animation createClipRevealAnimationLocked(int transit, boolean enter, Rect appFrame, Rect displayFrame) {
        long duration;
        Animation alphaAnimation;
        if (enter) {
            int appWidth = appFrame.width();
            int appHeight = appFrame.height();
            getDefaultNextAppTransitionStartRect(this.mTmpRect);
            float t = 0.0f;
            if (appHeight > 0) {
                t = this.mTmpRect.top / displayFrame.height();
            }
            int translationY = this.mClipRevealTranslationY + ((int) ((displayFrame.height() / 7.0f) * t));
            int translationX = 0;
            int translationYCorrection = translationY;
            int centerX = this.mTmpRect.centerX();
            int centerY = this.mTmpRect.centerY();
            int halfWidth = this.mTmpRect.width() / 2;
            int halfHeight = this.mTmpRect.height() / 2;
            int clipStartX = (centerX - halfWidth) - appFrame.left;
            int clipStartY = (centerY - halfHeight) - appFrame.top;
            boolean cutOff = false;
            if (appFrame.top > centerY - halfHeight) {
                translationY = (centerY - halfHeight) - appFrame.top;
                translationYCorrection = 0;
                clipStartY = 0;
                cutOff = true;
            }
            if (appFrame.left > centerX - halfWidth) {
                translationX = (centerX - halfWidth) - appFrame.left;
                clipStartX = 0;
                cutOff = true;
            }
            if (appFrame.right < centerX + halfWidth) {
                translationX = (centerX + halfWidth) - appFrame.right;
                clipStartX = appWidth - this.mTmpRect.width();
                cutOff = true;
            }
            long duration2 = calculateClipRevealTransitionDuration(cutOff, translationX, translationY, displayFrame);
            ClipRectAnimation clipRectLRAnimation = new ClipRectLRAnimation(clipStartX, this.mTmpRect.width() + clipStartX, 0, appWidth);
            clipRectLRAnimation.setInterpolator(this.mClipHorizontalInterpolator);
            clipRectLRAnimation.setDuration((long) (duration2 / 2.5f));
            TranslateAnimation translate = new TranslateAnimation(translationX, 0.0f, translationY, 0.0f);
            translate.setInterpolator(cutOff ? TOUCH_RESPONSE_INTERPOLATOR : this.mLinearOutSlowInInterpolator);
            translate.setDuration(duration2);
            ClipRectAnimation clipRectTBAnimation = new ClipRectTBAnimation(clipStartY, this.mTmpRect.height() + clipStartY, 0, appHeight, translationYCorrection, 0, this.mLinearOutSlowInInterpolator);
            clipRectTBAnimation.setInterpolator(TOUCH_RESPONSE_INTERPOLATOR);
            clipRectTBAnimation.setDuration(duration2);
            long alphaDuration = duration2 / 4;
            AlphaAnimation alpha = new AlphaAnimation(0.5f, 1.0f);
            alpha.setDuration(alphaDuration);
            alpha.setInterpolator(this.mLinearOutSlowInInterpolator);
            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(clipRectLRAnimation);
            animationSet.addAnimation(clipRectTBAnimation);
            animationSet.addAnimation(translate);
            animationSet.addAnimation(alpha);
            animationSet.setZAdjustment(1);
            animationSet.initialize(appWidth, appHeight, appWidth, appHeight);
            alphaAnimation = animationSet;
            this.mLastHadClipReveal = true;
            this.mLastClipRevealTransitionDuration = duration2;
            this.mLastClipRevealMaxTranslation = cutOff ? Math.max(Math.abs(translationY), Math.abs(translationX)) : 0;
        } else {
            switch (transit) {
                case 6:
                case 7:
                    duration = this.mConfigShortAnimTime;
                    break;
                default:
                    duration = 336;
                    break;
            }
            if (transit == 14 || transit == 15) {
                alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
                alphaAnimation.setDetachWallpaper(true);
            } else {
                alphaAnimation = new AlphaAnimation(1.0f, 1.0f);
            }
            alphaAnimation.setInterpolator(this.mDecelerateInterpolator);
            alphaAnimation.setDuration(duration);
            alphaAnimation.setFillAfter(true);
        }
        return alphaAnimation;
    }

    Animation prepareThumbnailAnimationWithDuration(Animation a, int appWidth, int appHeight, long duration, Interpolator interpolator) {
        if (duration > 0) {
            a.setDuration(duration);
        }
        a.setFillAfter(true);
        if (interpolator != null) {
            a.setInterpolator(interpolator);
        }
        a.initialize(appWidth, appHeight, appWidth, appHeight);
        return a;
    }

    Animation prepareThumbnailAnimation(Animation a, int appWidth, int appHeight, int transit) {
        int duration;
        switch (transit) {
            case 6:
            case 7:
                duration = this.mConfigShortAnimTime;
                break;
            default:
                duration = 336;
                break;
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appHeight, duration, this.mDecelerateInterpolator);
    }

    int getThumbnailTransitionState(boolean enter) {
        if (enter) {
            if (this.mNextAppTransitionScaleUp) {
                return 0;
            }
            return 2;
        }
        if (this.mNextAppTransitionScaleUp) {
            return 1;
        }
        return 3;
    }

    Animation createThumbnailAspectScaleAnimationLocked(Rect appRect, Rect contentInsets, Bitmap thumbnailHeader, int taskId, int uiMode, int orientation) {
        float fromX;
        float fromY;
        float toX;
        float toY;
        float pivotX;
        float pivotY;
        Animation a;
        int thumbWidthI = thumbnailHeader.getWidth();
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1;
        int thumbHeightI = thumbnailHeader.getHeight();
        int appWidth = appRect.width();
        float scaleW = appWidth / thumbWidth;
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        if (isTvUiMode(uiMode) || orientation == 1) {
            fromX = this.mTmpRect.left;
            fromY = this.mTmpRect.top;
            toX = ((this.mTmpRect.width() / 2) * (scaleW - 1.0f)) + appRect.left;
            toY = ((appRect.height() / 2) * (1.0f - (1.0f / scaleW))) + appRect.top;
            pivotX = this.mTmpRect.width() / 2;
            pivotY = (appRect.height() / 2) / scaleW;
        } else {
            pivotX = 0.0f;
            pivotY = 0.0f;
            fromX = this.mTmpRect.left;
            fromY = this.mTmpRect.top;
            toX = appRect.left;
            toY = appRect.top;
        }
        long duration = getAspectScaleDuration();
        Interpolator interpolator = getAspectScaleInterpolator();
        if (this.mNextAppTransitionScaleUp) {
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleW, pivotX, pivotY);
            scale.setInterpolator(interpolator);
            scale.setDuration(duration);
            Animation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setInterpolator(this.mNextAppTransition == 19 ? THUMBNAIL_DOCK_INTERPOLATOR : this.mThumbnailFadeOutInterpolator);
            alpha.setDuration(this.mNextAppTransition == 19 ? duration / 2 : duration);
            Animation translate = createCurvedMotion(fromX, toX, fromY, toY);
            translate.setInterpolator(interpolator);
            translate.setDuration(duration);
            this.mTmpFromClipRect.set(0, 0, thumbWidthI, thumbHeightI);
            this.mTmpToClipRect.set(appRect);
            this.mTmpToClipRect.offsetTo(0, 0);
            this.mTmpToClipRect.right = (int) (this.mTmpToClipRect.right / scaleW);
            this.mTmpToClipRect.bottom = (int) (this.mTmpToClipRect.bottom / scaleW);
            if (contentInsets != null) {
                this.mTmpToClipRect.inset((int) ((-contentInsets.left) * scaleW), (int) ((-contentInsets.top) * scaleW), (int) ((-contentInsets.right) * scaleW), (int) ((-contentInsets.bottom) * scaleW));
            }
            ClipRectAnimation clipRectAnimation = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
            clipRectAnimation.setInterpolator(interpolator);
            clipRectAnimation.setDuration(duration);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            set.addAnimation(translate);
            set.addAnimation(clipRectAnimation);
            a = set;
        } else {
            Animation scale2 = new ScaleAnimation(scaleW, 1.0f, scaleW, 1.0f, pivotX, pivotY);
            scale2.setInterpolator(interpolator);
            scale2.setDuration(duration);
            Animation alpha2 = new AlphaAnimation(0.0f, 1.0f);
            alpha2.setInterpolator(this.mThumbnailFadeInInterpolator);
            alpha2.setDuration(duration);
            Animation translate2 = createCurvedMotion(toX, fromX, toY, fromY);
            translate2.setInterpolator(interpolator);
            translate2.setDuration(duration);
            AnimationSet set2 = new AnimationSet(false);
            set2.addAnimation(scale2);
            set2.addAnimation(alpha2);
            set2.addAnimation(translate2);
            a = set2;
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appRect.height(), 0L, null);
    }

    private Animation createCurvedMotion(float fromX, float toX, float fromY, float toY) {
        if (Math.abs(toX - fromX) < 1.0f || this.mNextAppTransition != 19) {
            return new TranslateAnimation(fromX, toX, fromY, toY);
        }
        Path path = createCurvedPath(fromX, toX, fromY, toY);
        return new CurvedTranslateAnimation(path);
    }

    private Path createCurvedPath(float fromX, float toX, float fromY, float toY) {
        Path path = new Path();
        path.moveTo(fromX, fromY);
        if (fromY > toY) {
            path.cubicTo(fromX, fromY, toX, (0.9f * fromY) + (0.1f * toY), toX, toY);
        } else {
            path.cubicTo(fromX, fromY, fromX, (0.1f * fromY) + (0.9f * toY), toX, toY);
        }
        return path;
    }

    private long getAspectScaleDuration() {
        if (this.mNextAppTransition == 19) {
            return 453L;
        }
        return 336L;
    }

    private Interpolator getAspectScaleInterpolator() {
        if (this.mNextAppTransition == 19) {
            return this.mFastOutSlowInInterpolator;
        }
        return TOUCH_RESPONSE_INTERPOLATOR;
    }

    Animation createAspectScaledThumbnailEnterExitAnimationLocked(int thumbTransitState, int uiMode, int orientation, int transit, Rect containingFrame, Rect contentInsets, Rect surfaceInsets, boolean freeform, int taskId) {
        Animation a;
        ClipRectAnimation clipRectAnimation;
        Animation translateAnim;
        ClipRectAnimation clipRectAnimation2;
        Animation translateAnim2;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = this.mTmpRect.width();
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1;
        int thumbHeightI = this.mTmpRect.height();
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1;
        int thumbStartX = this.mTmpRect.left - containingFrame.left;
        int thumbStartY = this.mTmpRect.top - containingFrame.top;
        switch (thumbTransitState) {
            case 0:
            case 3:
                boolean scaleUp = thumbTransitState == 0;
                if (freeform && scaleUp) {
                    a = createAspectScaledThumbnailEnterFreeformAnimationLocked(containingFrame, surfaceInsets, taskId);
                } else if (freeform) {
                    a = createAspectScaledThumbnailExitFreeformAnimationLocked(containingFrame, surfaceInsets, taskId);
                } else {
                    AnimationSet set = new AnimationSet(true);
                    this.mTmpFromClipRect.set(containingFrame);
                    this.mTmpToClipRect.set(containingFrame);
                    this.mTmpFromClipRect.offsetTo(0, 0);
                    this.mTmpToClipRect.offsetTo(0, 0);
                    this.mTmpFromClipRect.inset(contentInsets);
                    this.mNextAppTransitionInsets.set(contentInsets);
                    if (isTvUiMode(uiMode) || orientation == 1) {
                        float scale = thumbWidth / ((appWidth - contentInsets.left) - contentInsets.right);
                        int unscaledThumbHeight = (int) (thumbHeight / scale);
                        this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + unscaledThumbHeight;
                        this.mNextAppTransitionInsets.set(contentInsets);
                        Animation scaleAnim = new ScaleAnimation(scaleUp ? scale : 1.0f, scaleUp ? 1.0f : scale, scaleUp ? scale : 1.0f, scaleUp ? 1.0f : scale, containingFrame.width() / 2.0f, (containingFrame.height() / 2.0f) + contentInsets.top);
                        float targetX = this.mTmpRect.left - containingFrame.left;
                        float x = (containingFrame.width() / 2.0f) - ((containingFrame.width() / 2.0f) * scale);
                        float targetY = this.mTmpRect.top - containingFrame.top;
                        float y = (containingFrame.height() / 2.0f) - ((containingFrame.height() / 2.0f) * scale);
                        float startX = targetX - x;
                        float startY = targetY - y;
                        if (scaleUp) {
                            clipRectAnimation = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                        } else {
                            clipRectAnimation = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                        }
                        if (scaleUp) {
                            translateAnim = createCurvedMotion(startX, 0.0f, startY - contentInsets.top, 0.0f);
                        } else {
                            translateAnim = createCurvedMotion(0.0f, startX, 0.0f, startY - contentInsets.top);
                        }
                        set.addAnimation(clipRectAnimation);
                        set.addAnimation(scaleAnim);
                        set.addAnimation(translateAnim);
                    } else {
                        this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + thumbHeightI;
                        this.mTmpFromClipRect.right = this.mTmpFromClipRect.left + thumbWidthI;
                        if (scaleUp) {
                            clipRectAnimation2 = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                        } else {
                            clipRectAnimation2 = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                        }
                        if (scaleUp) {
                            translateAnim2 = createCurvedMotion(thumbStartX, 0.0f, thumbStartY - contentInsets.top, 0.0f);
                        } else {
                            translateAnim2 = createCurvedMotion(0.0f, thumbStartX, 0.0f, thumbStartY - contentInsets.top);
                        }
                        set.addAnimation(clipRectAnimation2);
                        set.addAnimation(translateAnim2);
                    }
                    a = set;
                    set.setZAdjustment(1);
                }
                break;
            case 1:
                if (transit == 14) {
                    a = new AlphaAnimation(1.0f, 0.0f);
                } else {
                    a = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            case 2:
                if (transit == 14) {
                    a = new AlphaAnimation(0.0f, 1.0f);
                } else {
                    a = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appHeight, getAspectScaleDuration(), getAspectScaleInterpolator());
    }

    private Animation createAspectScaledThumbnailEnterFreeformAnimationLocked(Rect frame, Rect surfaceInsets, int taskId) {
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(this.mTmpRect, frame, surfaceInsets, true);
    }

    private Animation createAspectScaledThumbnailExitFreeformAnimationLocked(Rect frame, Rect surfaceInsets, int taskId) {
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(frame, this.mTmpRect, surfaceInsets, false);
    }

    private AnimationSet createAspectScaledThumbnailFreeformAnimationLocked(Rect sourceFrame, Rect destFrame, Rect surfaceInsets, boolean enter) {
        ScaleAnimation scale;
        float sourceWidth = sourceFrame.width();
        float sourceHeight = sourceFrame.height();
        float destWidth = destFrame.width();
        float destHeight = destFrame.height();
        float scaleH = enter ? sourceWidth / destWidth : destWidth / sourceWidth;
        float scaleV = enter ? sourceHeight / destHeight : destHeight / sourceHeight;
        AnimationSet set = new AnimationSet(true);
        int surfaceInsetsH = surfaceInsets == null ? 0 : surfaceInsets.left + surfaceInsets.right;
        int surfaceInsetsV = surfaceInsets == null ? 0 : surfaceInsets.top + surfaceInsets.bottom;
        if (!enter) {
            destWidth = sourceWidth;
        }
        float scaleHCenter = (surfaceInsetsH + destWidth) / 2.0f;
        if (!enter) {
            destHeight = sourceHeight;
        }
        float scaleVCenter = (surfaceInsetsV + destHeight) / 2.0f;
        if (enter) {
            scale = new ScaleAnimation(scaleH, 1.0f, scaleV, 1.0f, scaleHCenter, scaleVCenter);
        } else {
            scale = new ScaleAnimation(1.0f, scaleH, 1.0f, scaleV, scaleHCenter, scaleVCenter);
        }
        int sourceHCenter = sourceFrame.left + (sourceFrame.width() / 2);
        int sourceVCenter = sourceFrame.top + (sourceFrame.height() / 2);
        int destHCenter = destFrame.left + (destFrame.width() / 2);
        int destVCenter = destFrame.top + (destFrame.height() / 2);
        int fromX = enter ? sourceHCenter - destHCenter : destHCenter - sourceHCenter;
        int fromY = enter ? sourceVCenter - destVCenter : destVCenter - sourceVCenter;
        TranslateAnimation translation = enter ? new TranslateAnimation(fromX, 0.0f, fromY, 0.0f) : new TranslateAnimation(0.0f, fromX, 0.0f, fromY);
        set.addAnimation(scale);
        set.addAnimation(translation);
        final IRemoteCallback callback = this.mAnimationFinishedCallback;
        if (callback != null) {
            set.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    AppTransition.this.mService.mH.obtainMessage(26, callback).sendToTarget();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        return set;
    }

    Animation createThumbnailScaleAnimationLocked(int appWidth, int appHeight, int transit, Bitmap thumbnailHeader) {
        Animation a;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader.getWidth();
        if (thumbWidthI <= 0) {
            thumbWidthI = 1;
        }
        float thumbWidth = thumbWidthI;
        int thumbHeightI = thumbnailHeader.getHeight();
        if (thumbHeightI <= 0) {
            thumbHeightI = 1;
        }
        float thumbHeight = thumbHeightI;
        if (this.mNextAppTransitionScaleUp) {
            float scaleW = appWidth / thumbWidth;
            float scaleH = appHeight / thumbHeight;
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH, computePivot(this.mTmpRect.left, 1.0f / scaleW), computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale.setInterpolator(this.mDecelerateInterpolator);
            Animation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setInterpolator(this.mThumbnailFadeOutInterpolator);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            a = set;
        } else {
            float scaleW2 = appWidth / thumbWidth;
            float scaleH2 = appHeight / thumbHeight;
            a = new ScaleAnimation(scaleW2, 1.0f, scaleH2, 1.0f, computePivot(this.mTmpRect.left, 1.0f / scaleW2), computePivot(this.mTmpRect.top, 1.0f / scaleH2));
        }
        return prepareThumbnailAnimation(a, appWidth, appHeight, transit);
    }

    Animation createThumbnailEnterExitAnimationLocked(int thumbTransitState, Rect containingFrame, int transit, int taskId) {
        Animation a;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        Bitmap thumbnailHeader = getAppTransitionThumbnailHeader(taskId);
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : appWidth;
        if (thumbWidthI <= 0) {
            thumbWidthI = 1;
        }
        float thumbWidth = thumbWidthI;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : appHeight;
        if (thumbHeightI <= 0) {
            thumbHeightI = 1;
        }
        float thumbHeight = thumbHeightI;
        switch (thumbTransitState) {
            case 0:
                float scaleW = thumbWidth / appWidth;
                float scaleH = thumbHeight / appHeight;
                a = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mTmpRect.left, scaleW), computePivot(this.mTmpRect.top, scaleH));
                break;
            case 1:
                if (transit == 14) {
                    a = new AlphaAnimation(1.0f, 0.0f);
                } else {
                    a = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            case 2:
                a = new AlphaAnimation(1.0f, 1.0f);
                break;
            case 3:
                float scaleW2 = thumbWidth / appWidth;
                float scaleH2 = thumbHeight / appHeight;
                Animation scale = new ScaleAnimation(1.0f, scaleW2, 1.0f, scaleH2, computePivot(this.mTmpRect.left, scaleW2), computePivot(this.mTmpRect.top, scaleH2));
                Animation alpha = new AlphaAnimation(1.0f, 0.0f);
                AnimationSet set = new AnimationSet(true);
                set.addAnimation(scale);
                set.addAnimation(alpha);
                set.setZAdjustment(1);
                a = set;
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        return prepareThumbnailAnimation(a, appWidth, appHeight, transit);
    }

    private Animation createRelaunchAnimation(Rect containingFrame, Rect contentInsets) {
        getDefaultNextAppTransitionStartRect(this.mTmpFromClipRect);
        int left = this.mTmpFromClipRect.left;
        int top = this.mTmpFromClipRect.top;
        this.mTmpFromClipRect.offset(-left, -top);
        this.mTmpToClipRect.set(0, 0, containingFrame.width(), containingFrame.height());
        AnimationSet set = new AnimationSet(true);
        float fromWidth = this.mTmpFromClipRect.width();
        float toWidth = this.mTmpToClipRect.width();
        float fromHeight = this.mTmpFromClipRect.height();
        float toHeight = (this.mTmpToClipRect.height() - contentInsets.top) - contentInsets.bottom;
        int translateAdjustment = 0;
        if (fromWidth <= toWidth && fromHeight <= toHeight) {
            set.addAnimation(new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect));
        } else {
            set.addAnimation(new ScaleAnimation(fromWidth / toWidth, 1.0f, fromHeight / toHeight, 1.0f));
            translateAdjustment = (int) ((contentInsets.top * fromHeight) / toHeight);
        }
        TranslateAnimation translate = new TranslateAnimation(left - containingFrame.left, 0.0f, (top - containingFrame.top) - translateAdjustment, 0.0f);
        set.addAnimation(translate);
        set.setDuration(336L);
        set.setZAdjustment(1);
        return set;
    }

    boolean canSkipFirstFrame() {
        if (this.mNextAppTransitionType == 1 || this.mNextAppTransitionType == 7) {
            return false;
        }
        return this.mNextAppTransitionType != 8;
    }

    Animation loadAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, int uiMode, int orientation, Rect frame, Rect displayFrame, Rect insets, Rect surfaceInsets, boolean isVoiceInteraction, boolean freeform, int taskId) {
        Animation a;
        if (isVoiceInteraction && (transit == 6 || transit == 8 || transit == 10)) {
            a = loadAnimationRes(lp, enter ? R.anim.seekbar_thumb_unpressed_to_pressed_thumb_0_animation : R.anim.shrink_fade_out);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation voice: anim=" + a + " transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else if (isVoiceInteraction && (transit == 7 || transit == 9 || transit == 11)) {
            a = loadAnimationRes(lp, enter ? R.anim.search_bar_exit : R.anim.seekbar_thumb_pressed_to_unpressed_thumb_animation);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation voice: anim=" + a + " transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else if (transit == 18) {
            a = createRelaunchAnimation(frame, insets);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=" + this.mNextAppTransition + " transit=" + appTransitionToString(transit) + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 1) {
            a = loadAnimationRes(this.mNextAppTransitionPackage, enter ? this.mNextAppTransitionEnter : this.mNextAppTransitionExit);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=ANIM_CUSTOM transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 7) {
            a = loadAnimationRes(this.mNextAppTransitionPackage, this.mNextAppTransitionInPlace);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=ANIM_CUSTOM_IN_PLACE transit=" + appTransitionToString(transit) + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 8) {
            a = createClipRevealAnimationLocked(transit, enter, frame, displayFrame);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=ANIM_CLIP_REVEAL transit=" + appTransitionToString(transit) + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 2) {
            a = createScaleUpAnimationLocked(transit, enter, frame);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=ANIM_SCALE_UP transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 4) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 3;
            a = createThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(enter), frame, transit, taskId);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                String animName = this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_SCALE_UP" : "ANIM_THUMBNAIL_SCALE_DOWN";
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=" + animName + " transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else if (this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 5;
            a = createAspectScaledThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(enter), uiMode, orientation, transit, frame, insets, surfaceInsets, freeform, taskId);
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                String animName2 = this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_ASPECT_SCALE_UP" : "ANIM_THUMBNAIL_ASPECT_SCALE_DOWN";
                Slog.v(TAG, "applyAnimation: anim=" + a + " nextAppTransition=" + animName2 + " transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        } else {
            int animAttr = 0;
            switch (transit) {
                case 6:
                    animAttr = !enter ? 5 : 4;
                    break;
                case 7:
                    animAttr = !enter ? 7 : 6;
                    break;
                case 8:
                case 19:
                    animAttr = !enter ? 9 : 8;
                    break;
                case 9:
                    animAttr = !enter ? 11 : 10;
                    break;
                case 10:
                    animAttr = !enter ? 13 : 12;
                    break;
                case 11:
                    animAttr = !enter ? 15 : 14;
                    break;
                case 12:
                    animAttr = !enter ? 19 : 18;
                    break;
                case 13:
                    animAttr = !enter ? 17 : 16;
                    break;
                case 14:
                    animAttr = !enter ? 21 : 20;
                    break;
                case 15:
                    animAttr = !enter ? 23 : 22;
                    break;
                case 16:
                    animAttr = !enter ? 24 : 25;
                    break;
            }
            a = animAttr != 0 ? loadAnimationAttr(lp, animAttr) : null;
            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS || WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v(TAG, "applyAnimation: anim=" + a + " animAttr=0x" + Integer.toHexString(animAttr) + " transit=" + appTransitionToString(transit) + " isEntrance=" + enter + " Callers=" + Debug.getCallers(3));
            }
        }
        return a;
    }

    int getAppStackClipMode() {
        if (this.mNextAppTransition == 18 || this.mNextAppTransition == 19 || this.mNextAppTransitionType == 8) {
            return 2;
        }
        return 0;
    }

    void postAnimationCallback() {
        if (this.mNextAppTransitionCallback == null) {
            return;
        }
        this.mService.mH.sendMessage(this.mService.mH.obtainMessage(26, this.mNextAppTransitionCallback));
        this.mNextAppTransitionCallback = null;
    }

    void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = 1;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionEnter = enterAnim;
            this.mNextAppTransitionExit = exitAnim;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        if (!isTransitionSet()) {
            return;
        }
        clear();
        this.mNextAppTransitionType = 2;
        putDefaultNextAppTransitionCoordinates(startX, startY, startX + startWidth, startY + startHeight, null);
        postAnimationCallback();
    }

    void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) {
        if (!isTransitionSet()) {
            return;
        }
        clear();
        this.mNextAppTransitionType = 8;
        putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, null);
        postAnimationCallback();
    }

    void overridePendingAppTransitionThumb(Bitmap srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 3 : 4;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, 0, 0, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overridePendingAppTransitionAspectScaledThumb(Bitmap srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, targetWidth, targetHeight, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback onAnimationStartedCallback, IRemoteCallback onAnimationFinishedCallback, boolean scaleUp) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionScaleUp = scaleUp;
            if (specs != null) {
                for (int i = 0; i < specs.length; i++) {
                    AppTransitionAnimationSpec spec = specs[i];
                    if (spec != null) {
                        this.mNextAppTransitionAnimationsSpecs.put(spec.taskId, spec);
                        if (i == 0) {
                            Rect rect = spec.rect;
                            putDefaultNextAppTransitionCoordinates(rect.left, rect.top, rect.width(), rect.height(), spec.bitmap);
                        }
                    }
                }
            }
            postAnimationCallback();
            this.mNextAppTransitionCallback = onAnimationStartedCallback;
            this.mAnimationFinishedCallback = onAnimationFinishedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp) {
        if (!isTransitionSet()) {
            return;
        }
        clear();
        this.mNextAppTransitionType = scaleUp ? 5 : 6;
        this.mNextAppTransitionAnimationsSpecsFuture = specsFuture;
        this.mNextAppTransitionScaleUp = scaleUp;
        this.mNextAppTransitionFutureCallback = callback;
    }

    void overrideInPlaceAppTransition(String packageName, int anim) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = 7;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionInPlace = anim;
            return;
        }
        postAnimationCallback();
    }

    private void fetchAppTransitionSpecsFromFuture() {
        if (this.mNextAppTransitionAnimationsSpecsFuture == null) {
            return;
        }
        this.mNextAppTransitionAnimationsSpecsPending = true;
        final IAppTransitionAnimationSpecsFuture future = this.mNextAppTransitionAnimationsSpecsFuture;
        this.mNextAppTransitionAnimationsSpecsFuture = null;
        this.mDefaultExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AppTransitionAnimationSpec[] specs = null;
                try {
                    specs = future.get();
                } catch (RemoteException e) {
                    Slog.w(AppTransition.TAG, "Failed to fetch app transition specs: " + e);
                }
                synchronized (AppTransition.this.mService.mWindowMap) {
                    AppTransition.this.mNextAppTransitionAnimationsSpecsPending = false;
                    AppTransition.this.overridePendingAppTransitionMultiThumb(specs, AppTransition.this.mNextAppTransitionFutureCallback, null, AppTransition.this.mNextAppTransitionScaleUp);
                    AppTransition.this.mNextAppTransitionFutureCallback = null;
                    if (specs != null) {
                        AppTransition.this.mService.prolongAnimationsFromSpecs(specs, AppTransition.this.mNextAppTransitionScaleUp);
                    }
                }
                AppTransition.this.mService.requestTraversal();
            }
        });
    }

    public String toString() {
        return "mNextAppTransition=" + appTransitionToString(this.mNextAppTransition);
    }

    public static String appTransitionToString(int transition) {
        switch (transition) {
            case -1:
                return "TRANSIT_UNSET";
            case 0:
                return "TRANSIT_NONE";
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 17:
            default:
                return "<UNKNOWN>";
            case 6:
                return "TRANSIT_ACTIVITY_OPEN";
            case 7:
                return "TRANSIT_ACTIVITY_CLOSE";
            case 8:
                return "TRANSIT_TASK_OPEN";
            case 9:
                return "TRANSIT_TASK_CLOSE";
            case 10:
                return "TRANSIT_TASK_TO_FRONT";
            case 11:
                return "TRANSIT_TASK_TO_BACK";
            case 12:
                return "TRANSIT_WALLPAPER_CLOSE";
            case 13:
                return "TRANSIT_WALLPAPER_OPEN";
            case 14:
                return "TRANSIT_WALLPAPER_INTRA_OPEN";
            case 15:
                return "TRANSIT_WALLPAPER_INTRA_CLOSE";
            case 16:
                return "TRANSIT_TASK_OPEN_BEHIND";
            case 18:
                return "TRANSIT_ACTIVITY_RELAUNCH";
            case 19:
                return "TRANSIT_DOCK_TASK_FROM_RECENTS";
        }
    }

    private String appStateToString() {
        switch (this.mAppTransitionState) {
            case 0:
                return "APP_STATE_IDLE";
            case 1:
                return "APP_STATE_READY";
            case 2:
                return "APP_STATE_RUNNING";
            case 3:
                return "APP_STATE_TIMEOUT";
            default:
                return "unknown state=" + this.mAppTransitionState;
        }
    }

    private String transitTypeToString() {
        switch (this.mNextAppTransitionType) {
            case 0:
                return "NEXT_TRANSIT_TYPE_NONE";
            case 1:
                return "NEXT_TRANSIT_TYPE_CUSTOM";
            case 2:
                return "NEXT_TRANSIT_TYPE_SCALE_UP";
            case 3:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP";
            case 4:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN";
            case 5:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP";
            case 6:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN";
            case 7:
                return "NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE";
            default:
                return "unknown type=" + this.mNextAppTransitionType;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(this);
        pw.print(prefix);
        pw.print("mAppTransitionState=");
        pw.println(appStateToString());
        if (this.mNextAppTransitionType != 0) {
            pw.print(prefix);
            pw.print("mNextAppTransitionType=");
            pw.println(transitTypeToString());
        }
        switch (this.mNextAppTransitionType) {
            case 1:
                pw.print(prefix);
                pw.print("mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print(prefix);
                pw.print("mNextAppTransitionEnter=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionEnter));
                pw.print(" mNextAppTransitionExit=0x");
                pw.println(Integer.toHexString(this.mNextAppTransitionExit));
                break;
            case 2:
                getDefaultNextAppTransitionStartRect(this.mTmpRect);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartX=");
                pw.print(this.mTmpRect.left);
                pw.print(" mNextAppTransitionStartY=");
                pw.println(this.mTmpRect.top);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartWidth=");
                pw.print(this.mTmpRect.width());
                pw.print(" mNextAppTransitionStartHeight=");
                pw.println(this.mTmpRect.height());
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                pw.print(prefix);
                pw.print("mDefaultNextAppTransitionAnimationSpec=");
                pw.println(this.mDefaultNextAppTransitionAnimationSpec);
                pw.print(prefix);
                pw.print("mNextAppTransitionAnimationsSpecs=");
                pw.println(this.mNextAppTransitionAnimationsSpecs);
                pw.print(prefix);
                pw.print("mNextAppTransitionScaleUp=");
                pw.println(this.mNextAppTransitionScaleUp);
                break;
            case 7:
                pw.print(prefix);
                pw.print("mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print(prefix);
                pw.print("mNextAppTransitionInPlace=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionInPlace));
                break;
        }
        if (this.mNextAppTransitionCallback == null) {
            return;
        }
        pw.print(prefix);
        pw.print("mNextAppTransitionCallback=");
        pw.println(this.mNextAppTransitionCallback);
    }

    public void setCurrentUser(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    boolean prepareAppTransitionLocked(int transit, boolean alwaysKeepCurrent) {
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v(TAG, "Prepare app transition: transit=" + appTransitionToString(transit) + " " + this + " alwaysKeepCurrent=" + alwaysKeepCurrent + " Callers=" + Debug.getCallers(3));
        }
        if (!isTransitionSet() || this.mNextAppTransition == 0) {
            setAppTransition(transit);
        } else if (!alwaysKeepCurrent) {
            if (transit == 8 && isTransitionEqual(9)) {
                setAppTransition(transit);
            } else if (transit == 6 && isTransitionEqual(7)) {
                setAppTransition(transit);
            }
        }
        boolean prepared = prepare();
        if (isTransitionSet()) {
            this.mService.mH.removeMessages(13);
            this.mService.mH.sendEmptyMessageDelayed(13, 5000L);
        }
        return prepared;
    }

    private boolean isTvUiMode(int uiMode) {
        return (uiMode & 4) > 0;
    }
}
