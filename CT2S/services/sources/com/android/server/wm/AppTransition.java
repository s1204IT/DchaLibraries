package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ClipRectAnimation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.internal.util.DumpUtils;
import com.android.server.AttributeCache;
import java.io.PrintWriter;

public class AppTransition implements DumpUtils.Dump {
    private static final int APP_STATE_IDLE = 0;
    private static final int APP_STATE_READY = 1;
    private static final int APP_STATE_RUNNING = 2;
    private static final int APP_STATE_TIMEOUT = 3;
    private static final boolean DEBUG_ANIM = false;
    private static final boolean DEBUG_APP_TRANSITIONS = false;
    private static final int DEFAULT_APP_TRANSITION_DURATION = 250;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM = 1;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE = 7;
    private static final int NEXT_TRANSIT_TYPE_NONE = 0;
    private static final int NEXT_TRANSIT_TYPE_SCALE_UP = 2;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN = 6;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP = 5;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN = 4;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP = 3;
    private static final float RECENTS_THUMBNAIL_FADEIN_FRACTION = 0.7f;
    private static final float RECENTS_THUMBNAIL_FADEOUT_FRACTION = 0.3f;
    private static final String TAG = "AppTransition";
    private static final int THUMBNAIL_APP_TRANSITION_ALPHA_DURATION = 325;
    private static final int THUMBNAIL_APP_TRANSITION_DURATION = 325;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_DOWN = 2;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_UP = 0;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_DOWN = 3;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_UP = 1;
    public static final int TRANSIT_ACTIVITY_CLOSE = 7;
    public static final int TRANSIT_ACTIVITY_OPEN = 6;
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
    private final int mConfigShortAnimTime;
    private final Context mContext;
    private final Interpolator mDecelerateInterpolator;
    private final Handler mH;
    private IRemoteCallback mNextAppTransitionCallback;
    private int mNextAppTransitionEnter;
    private int mNextAppTransitionExit;
    private int mNextAppTransitionInPlace;
    private String mNextAppTransitionPackage;
    private boolean mNextAppTransitionScaleUp;
    private int mNextAppTransitionStartHeight;
    private int mNextAppTransitionStartWidth;
    private int mNextAppTransitionStartX;
    private int mNextAppTransitionStartY;
    private Bitmap mNextAppTransitionThumbnail;
    private final Interpolator mThumbnailFastOutSlowInInterpolator;
    private int mNextAppTransition = -1;
    private int mNextAppTransitionType = 0;
    private Rect mNextAppTransitionInsets = new Rect();
    private Rect mTmpFromClipRect = new Rect();
    private Rect mTmpToClipRect = new Rect();
    private int mAppTransitionState = 0;
    private int mCurrentUserId = 0;
    private final Interpolator mThumbnailFadeInInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if (input < AppTransition.RECENTS_THUMBNAIL_FADEIN_FRACTION) {
                return 0.0f;
            }
            return (input - AppTransition.RECENTS_THUMBNAIL_FADEIN_FRACTION) / AppTransition.RECENTS_THUMBNAIL_FADEOUT_FRACTION;
        }
    };
    private final Interpolator mThumbnailFadeOutInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            if (input < AppTransition.RECENTS_THUMBNAIL_FADEOUT_FRACTION) {
                return input / AppTransition.RECENTS_THUMBNAIL_FADEOUT_FRACTION;
            }
            return 1.0f;
        }
    };

    AppTransition(Context context, Handler h) {
        this.mContext = context;
        this.mH = h;
        this.mConfigShortAnimTime = context.getResources().getInteger(R.integer.config_shortAnimTime);
        this.mDecelerateInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.decelerate_cubic);
        this.mThumbnailFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
    }

    boolean isTransitionSet() {
        return this.mNextAppTransition != -1;
    }

    boolean isTransitionNone() {
        return this.mNextAppTransition == 0;
    }

    boolean isTransitionEqual(int transit) {
        return this.mNextAppTransition == transit;
    }

    int getAppTransition() {
        return this.mNextAppTransition;
    }

    void setAppTransition(int transit) {
        this.mNextAppTransition = transit;
    }

    boolean isReady() {
        return this.mAppTransitionState == 1 || this.mAppTransitionState == 3;
    }

    void setReady() {
        this.mAppTransitionState = 1;
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

    Bitmap getNextAppTransitionThumbnail() {
        return this.mNextAppTransitionThumbnail;
    }

    boolean isNextThumbnailTransitionAspectScaled() {
        return this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6;
    }

    boolean isNextThumbnailTransitionScaleUp() {
        return this.mNextAppTransitionScaleUp;
    }

    int getStartingX() {
        return this.mNextAppTransitionStartX;
    }

    int getStartingY() {
        return this.mNextAppTransitionStartY;
    }

    void prepare() {
        if (!isRunning()) {
            this.mAppTransitionState = 0;
        }
    }

    void goodToGo() {
        this.mNextAppTransition = -1;
        this.mAppTransitionState = 2;
    }

    void clear() {
        this.mNextAppTransitionType = 0;
        this.mNextAppTransitionPackage = null;
        this.mNextAppTransitionThumbnail = null;
    }

    void freeze() {
        setAppTransition(-1);
        clear();
        setReady();
    }

    private AttributeCache.Entry getCachedAnimations(WindowManager.LayoutParams lp) {
        if (lp != null && lp.windowAnimations != 0) {
            String packageName = lp.packageName != null ? lp.packageName : "android";
            int resId = lp.windowAnimations;
            if (((-16777216) & resId) == 16777216) {
                packageName = "android";
            }
            return AttributeCache.instance().get(packageName, resId, com.android.internal.R.styleable.WindowAnimation, this.mCurrentUserId);
        }
        return null;
    }

    private AttributeCache.Entry getCachedAnimations(String packageName, int resId) {
        if (packageName == null) {
            return null;
        }
        if (((-16777216) & resId) == 16777216) {
            packageName = "android";
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
        return Math.abs(denom) < 1.0E-4f ? startPos : (-startPos) / denom;
    }

    private Animation createScaleUpAnimationLocked(int transit, boolean enter, int appWidth, int appHeight) {
        Animation a;
        long duration;
        if (enter) {
            float scaleW = this.mNextAppTransitionStartWidth / appWidth;
            float scaleH = this.mNextAppTransitionStartHeight / appHeight;
            Animation scale = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mNextAppTransitionStartX, scaleW), computePivot(this.mNextAppTransitionStartY, scaleH));
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
                duration = 250;
                break;
        }
        a.setDuration(duration);
        a.setFillAfter(true);
        a.setInterpolator(this.mDecelerateInterpolator);
        a.initialize(appWidth, appHeight, appWidth, appHeight);
        return a;
    }

    Animation prepareThumbnailAnimationWithDuration(Animation a, int appWidth, int appHeight, int duration, Interpolator interpolator) {
        if (duration > 0) {
            a.setDuration(duration);
        }
        a.setFillAfter(true);
        a.setInterpolator(interpolator);
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
                duration = DEFAULT_APP_TRANSITION_DURATION;
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

    Animation createThumbnailAspectScaleAnimationLocked(int appWidth, int appHeight, int deviceWidth, int transit) {
        Animation a;
        int thumbWidthI = this.mNextAppTransitionThumbnail.getWidth();
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = this.mNextAppTransitionThumbnail.getHeight();
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        float scaleW = deviceWidth / thumbWidth;
        float f = deviceWidth;
        float unscaledHeight = thumbHeight * scaleW;
        float unscaledStartY = this.mNextAppTransitionStartY - ((unscaledHeight - thumbHeight) / 2.0f);
        if (this.mNextAppTransitionScaleUp) {
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleW, this.mNextAppTransitionStartX + (thumbWidth / 2.0f), this.mNextAppTransitionStartY + (thumbHeight / 2.0f));
            scale.setInterpolator(this.mThumbnailFastOutSlowInInterpolator);
            scale.setDuration(325L);
            Animation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setInterpolator(this.mThumbnailFadeOutInterpolator);
            alpha.setDuration(325L);
            Animation translate = new TranslateAnimation(0.0f, 0.0f, 0.0f, (-unscaledStartY) + this.mNextAppTransitionInsets.top);
            translate.setInterpolator(this.mThumbnailFastOutSlowInInterpolator);
            translate.setDuration(325L);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            set.addAnimation(translate);
            a = set;
        } else {
            Animation scale2 = new ScaleAnimation(scaleW, 1.0f, scaleW, 1.0f, (thumbWidth / 2.0f) + this.mNextAppTransitionStartX, (thumbHeight / 2.0f) + this.mNextAppTransitionStartY);
            scale2.setInterpolator(this.mThumbnailFastOutSlowInInterpolator);
            scale2.setDuration(325L);
            Animation alpha2 = new AlphaAnimation(0.0f, 1.0f);
            alpha2.setInterpolator(this.mThumbnailFadeInInterpolator);
            alpha2.setDuration(325L);
            Animation translate2 = new TranslateAnimation(0.0f, 0.0f, (-unscaledStartY) + this.mNextAppTransitionInsets.top, 0.0f);
            translate2.setInterpolator(this.mThumbnailFastOutSlowInInterpolator);
            translate2.setDuration(325L);
            AnimationSet set2 = new AnimationSet(false);
            set2.addAnimation(scale2);
            set2.addAnimation(alpha2);
            set2.addAnimation(translate2);
            a = set2;
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appHeight, 0, this.mThumbnailFastOutSlowInInterpolator);
    }

    Animation createAspectScaledThumbnailEnterExitAnimationLocked(int thumbTransitState, int appWidth, int appHeight, int orientation, int transit, Rect containingFrame, Rect contentInsets, boolean isFullScreen) {
        float scale;
        int scaledTopDecor;
        Animation a;
        float scale2;
        int scaledTopDecor2;
        int thumbWidthI = this.mNextAppTransitionStartWidth;
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = this.mNextAppTransitionStartHeight;
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        switch (thumbTransitState) {
            case 0:
                if (orientation == 1) {
                    scale2 = thumbWidth / appWidth;
                    scaledTopDecor2 = (int) (contentInsets.top * scale2);
                    int unscaledThumbHeight = (int) (thumbHeight / scale2);
                    this.mTmpFromClipRect.set(containingFrame);
                    if (isFullScreen) {
                        this.mTmpFromClipRect.top = contentInsets.top;
                    }
                    this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + unscaledThumbHeight;
                    this.mTmpToClipRect.set(containingFrame);
                } else {
                    scale2 = thumbHeight / (appHeight - contentInsets.top);
                    scaledTopDecor2 = (int) (contentInsets.top * scale2);
                    int unscaledThumbWidth = (int) (thumbWidth / scale2);
                    int unscaledThumbHeight2 = (int) (thumbHeight / scale2);
                    this.mTmpFromClipRect.set(containingFrame);
                    if (isFullScreen) {
                        this.mTmpFromClipRect.top = contentInsets.top;
                        this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + unscaledThumbHeight2;
                    }
                    this.mTmpFromClipRect.right = this.mTmpFromClipRect.left + unscaledThumbWidth;
                    this.mTmpToClipRect.set(containingFrame);
                }
                this.mNextAppTransitionInsets.set(contentInsets);
                Animation scaleAnim = new ScaleAnimation(scale2, 1.0f, scale2, 1.0f, computePivot(this.mNextAppTransitionStartX, scale2), computePivot(this.mNextAppTransitionStartY, scale2));
                Animation clipAnim = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                Animation translateAnim = new TranslateAnimation(0.0f, 0.0f, -scaledTopDecor2, 0.0f);
                AnimationSet set = new AnimationSet(true);
                set.addAnimation(clipAnim);
                set.addAnimation(scaleAnim);
                set.addAnimation(translateAnim);
                a = set;
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
            case 3:
                if (orientation == 1) {
                    scale = thumbWidth / appWidth;
                    scaledTopDecor = (int) (contentInsets.top * scale);
                    int unscaledThumbHeight3 = (int) (thumbHeight / scale);
                    this.mTmpFromClipRect.set(containingFrame);
                    this.mTmpToClipRect.set(containingFrame);
                    if (isFullScreen) {
                        this.mTmpToClipRect.top = contentInsets.top;
                    }
                    this.mTmpToClipRect.bottom = this.mTmpToClipRect.top + unscaledThumbHeight3;
                } else {
                    scale = thumbHeight / (appHeight - contentInsets.top);
                    scaledTopDecor = (int) (contentInsets.top * scale);
                    int unscaledThumbWidth2 = (int) (thumbWidth / scale);
                    int unscaledThumbHeight4 = (int) (thumbHeight / scale);
                    this.mTmpFromClipRect.set(containingFrame);
                    this.mTmpToClipRect.set(containingFrame);
                    if (isFullScreen) {
                        this.mTmpToClipRect.top = contentInsets.top;
                        this.mTmpToClipRect.bottom = this.mTmpToClipRect.top + unscaledThumbHeight4;
                    }
                    this.mTmpToClipRect.right = this.mTmpToClipRect.left + unscaledThumbWidth2;
                }
                this.mNextAppTransitionInsets.set(contentInsets);
                Animation scaleAnim2 = new ScaleAnimation(1.0f, scale, 1.0f, scale, computePivot(this.mNextAppTransitionStartX, scale), computePivot(this.mNextAppTransitionStartY, scale));
                Animation clipAnim2 = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                Animation translateAnim2 = new TranslateAnimation(0.0f, 0.0f, 0.0f, -scaledTopDecor);
                AnimationSet set2 = new AnimationSet(true);
                set2.addAnimation(clipAnim2);
                set2.addAnimation(scaleAnim2);
                set2.addAnimation(translateAnim2);
                a = set2;
                a.setZAdjustment(1);
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        int duration = Math.max(325, 325);
        return prepareThumbnailAnimationWithDuration(a, appWidth, appHeight, duration, this.mThumbnailFastOutSlowInInterpolator);
    }

    Animation createThumbnailScaleAnimationLocked(int appWidth, int appHeight, int transit) {
        Animation a;
        int thumbWidthI = this.mNextAppTransitionThumbnail.getWidth();
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = this.mNextAppTransitionThumbnail.getHeight();
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        if (this.mNextAppTransitionScaleUp) {
            float scaleW = appWidth / thumbWidth;
            float scaleH = appHeight / thumbHeight;
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH, computePivot(this.mNextAppTransitionStartX, 1.0f / scaleW), computePivot(this.mNextAppTransitionStartY, 1.0f / scaleH));
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
            a = new ScaleAnimation(scaleW2, 1.0f, scaleH2, 1.0f, computePivot(this.mNextAppTransitionStartX, 1.0f / scaleW2), computePivot(this.mNextAppTransitionStartY, 1.0f / scaleH2));
        }
        return prepareThumbnailAnimation(a, appWidth, appHeight, transit);
    }

    Animation createThumbnailEnterExitAnimationLocked(int thumbTransitState, int appWidth, int appHeight, int transit) {
        Animation a;
        int thumbWidthI = this.mNextAppTransitionThumbnail.getWidth();
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = this.mNextAppTransitionThumbnail.getHeight();
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        switch (thumbTransitState) {
            case 0:
                float scaleW = thumbWidth / appWidth;
                float scaleH = thumbHeight / appHeight;
                a = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mNextAppTransitionStartX, scaleW), computePivot(this.mNextAppTransitionStartY, scaleH));
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
                Animation scale = new ScaleAnimation(1.0f, scaleW2, 1.0f, scaleH2, computePivot(this.mNextAppTransitionStartX, scaleW2), computePivot(this.mNextAppTransitionStartY, scaleH2));
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

    Animation loadAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, int appWidth, int appHeight, int orientation, Rect containingFrame, Rect contentInsets, boolean isFullScreen, boolean isVoiceInteraction) {
        if (isVoiceInteraction && (transit == 6 || transit == 8 || transit == 10)) {
            Animation a = loadAnimationRes(lp, enter ? R.anim.popup_exit_material : R.anim.progress_indeterminate_horizontal_rect1);
            return a;
        }
        if (isVoiceInteraction && (transit == 7 || transit == 9 || transit == 11)) {
            Animation a2 = loadAnimationRes(lp, enter ? R.anim.overlay_task_fragment_open_from_top : R.anim.popup_enter_material);
            return a2;
        }
        if (this.mNextAppTransitionType == 1) {
            Animation a3 = loadAnimationRes(this.mNextAppTransitionPackage, enter ? this.mNextAppTransitionEnter : this.mNextAppTransitionExit);
            return a3;
        }
        if (this.mNextAppTransitionType == 7) {
            Animation a4 = loadAnimationRes(this.mNextAppTransitionPackage, this.mNextAppTransitionInPlace);
            return a4;
        }
        if (this.mNextAppTransitionType == 2) {
            Animation a5 = createScaleUpAnimationLocked(transit, enter, appWidth, appHeight);
            return a5;
        }
        if (this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 4) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 3;
            Animation a6 = createThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(enter), appWidth, appHeight, transit);
            return a6;
        }
        if (this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 5;
            Animation a7 = createAspectScaledThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(enter), appWidth, appHeight, orientation, transit, containingFrame, contentInsets, isFullScreen);
            return a7;
        }
        int animAttr = 0;
        switch (transit) {
            case 6:
                animAttr = !enter ? 5 : 4;
                break;
            case 7:
                animAttr = !enter ? 7 : 6;
                break;
            case 8:
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
        if (animAttr == 0) {
            return null;
        }
        Animation a8 = loadAnimationAttr(lp, animAttr);
        return a8;
    }

    void postAnimationCallback() {
        if (this.mNextAppTransitionCallback != null) {
            this.mH.sendMessage(this.mH.obtainMessage(26, this.mNextAppTransitionCallback));
            this.mNextAppTransitionCallback = null;
        }
    }

    void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) {
        if (isTransitionSet()) {
            this.mNextAppTransitionType = 1;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionThumbnail = null;
            this.mNextAppTransitionEnter = enterAnim;
            this.mNextAppTransitionExit = exitAnim;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        if (isTransitionSet()) {
            this.mNextAppTransitionType = 2;
            this.mNextAppTransitionPackage = null;
            this.mNextAppTransitionThumbnail = null;
            this.mNextAppTransitionStartX = startX;
            this.mNextAppTransitionStartY = startY;
            this.mNextAppTransitionStartWidth = startWidth;
            this.mNextAppTransitionStartHeight = startHeight;
            postAnimationCallback();
            this.mNextAppTransitionCallback = null;
        }
    }

    void overridePendingAppTransitionThumb(Bitmap srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        if (isTransitionSet()) {
            this.mNextAppTransitionType = scaleUp ? 3 : 4;
            this.mNextAppTransitionPackage = null;
            this.mNextAppTransitionThumbnail = srcThumb;
            this.mNextAppTransitionScaleUp = scaleUp;
            this.mNextAppTransitionStartX = startX;
            this.mNextAppTransitionStartY = startY;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overridePendingAppTransitionAspectScaledThumb(Bitmap srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        if (isTransitionSet()) {
            this.mNextAppTransitionType = scaleUp ? 5 : 6;
            this.mNextAppTransitionPackage = null;
            this.mNextAppTransitionThumbnail = srcThumb;
            this.mNextAppTransitionScaleUp = scaleUp;
            this.mNextAppTransitionStartX = startX;
            this.mNextAppTransitionStartY = startY;
            this.mNextAppTransitionStartWidth = targetWidth;
            this.mNextAppTransitionStartHeight = targetHeight;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
            return;
        }
        postAnimationCallback();
    }

    void overrideInPlaceAppTransition(String packageName, int anim) {
        if (isTransitionSet()) {
            this.mNextAppTransitionType = 7;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionInPlace = anim;
            return;
        }
        postAnimationCallback();
    }

    public String toString() {
        return "mNextAppTransition=0x" + Integer.toHexString(this.mNextAppTransition);
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

    public void dump(PrintWriter pw) {
        pw.print(" " + this);
        pw.print("  mAppTransitionState=");
        pw.println(appStateToString());
        if (this.mNextAppTransitionType != 0) {
            pw.print("  mNextAppTransitionType=");
            pw.println(transitTypeToString());
        }
        switch (this.mNextAppTransitionType) {
            case 1:
                pw.print("  mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print("  mNextAppTransitionEnter=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionEnter));
                pw.print(" mNextAppTransitionExit=0x");
                pw.println(Integer.toHexString(this.mNextAppTransitionExit));
                break;
            case 2:
                pw.print("  mNextAppTransitionStartX=");
                pw.print(this.mNextAppTransitionStartX);
                pw.print(" mNextAppTransitionStartY=");
                pw.println(this.mNextAppTransitionStartY);
                pw.print("  mNextAppTransitionStartWidth=");
                pw.print(this.mNextAppTransitionStartWidth);
                pw.print(" mNextAppTransitionStartHeight=");
                pw.println(this.mNextAppTransitionStartHeight);
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                pw.print("  mNextAppTransitionThumbnail=");
                pw.print(this.mNextAppTransitionThumbnail);
                pw.print(" mNextAppTransitionStartX=");
                pw.print(this.mNextAppTransitionStartX);
                pw.print(" mNextAppTransitionStartY=");
                pw.println(this.mNextAppTransitionStartY);
                pw.print(" mNextAppTransitionStartWidth=");
                pw.print(this.mNextAppTransitionStartWidth);
                pw.print(" mNextAppTransitionStartHeight=");
                pw.println(this.mNextAppTransitionStartHeight);
                pw.print("  mNextAppTransitionScaleUp=");
                pw.println(this.mNextAppTransitionScaleUp);
                break;
            case 7:
                pw.print("  mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print("  mNextAppTransitionInPlace=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionInPlace));
                break;
        }
        if (this.mNextAppTransitionCallback != null) {
            pw.print("  mNextAppTransitionCallback=");
            pw.println(this.mNextAppTransitionCallback);
        }
    }

    public void setCurrentUser(int newUserId) {
        this.mCurrentUserId = newUserId;
    }
}
