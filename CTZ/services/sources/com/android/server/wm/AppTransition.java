package com.android.server.wm;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ResourceId;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.RemoteAnimationAdapter;
import android.view.WindowManager;
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
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
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
    static final int MAX_APP_TRANSITION_DURATION = 3000;
    private static final int MAX_CLIP_REVEAL_TRANSITION_DURATION = 420;
    private static final int NEXT_TRANSIT_TYPE_CLIP_REVEAL = 8;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM = 1;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE = 7;
    private static final int NEXT_TRANSIT_TYPE_NONE = 0;
    private static final int NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS = 9;
    private static final int NEXT_TRANSIT_TYPE_REMOTE = 10;
    private static final int NEXT_TRANSIT_TYPE_SCALE_UP = 2;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN = 6;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP = 5;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN = 4;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP = 3;
    private static final float RECENTS_THUMBNAIL_FADEIN_FRACTION = 0.5f;
    private static final float RECENTS_THUMBNAIL_FADEOUT_FRACTION = 0.5f;
    private static final String TAG = "WindowManager";
    private static final int THUMBNAIL_APP_TRANSITION_DURATION = 336;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_DOWN = 2;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_UP = 0;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_DOWN = 3;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_UP = 1;
    private IRemoteCallback mAnimationFinishedCallback;
    private final int mClipRevealTranslationY;
    private final int mConfigShortAnimTime;
    private final Context mContext;
    private final Interpolator mDecelerateInterpolator;
    private AppTransitionAnimationSpec mDefaultNextAppTransitionAnimationSpec;
    private final Interpolator mFastOutLinearInInterpolator;
    private final Interpolator mFastOutSlowInInterpolator;
    private int mLastClipRevealMaxTranslation;
    private String mLastClosingApp;
    private boolean mLastHadClipReveal;
    private String mLastOpeningApp;
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
    private RemoteAnimationController mRemoteAnimationController;
    private final WindowManagerService mService;
    static final Interpolator TOUCH_RESPONSE_INTERPOLATOR = new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
    private static final Interpolator THUMBNAIL_DOCK_INTERPOLATOR = new PathInterpolator(0.85f, 0.0f, 1.0f, 1.0f);
    private int mNextAppTransition = -1;
    private int mNextAppTransitionFlags = 0;
    private int mLastUsedAppTransition = -1;
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
        public float getInterpolation(float f) {
            if (f >= 0.5f) {
                return AppTransition.this.mFastOutLinearInInterpolator.getInterpolation((f - 0.5f) / 0.5f);
            }
            return 0.0f;
        }
    };
    private final Interpolator mThumbnailFadeOutInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float f) {
            if (f < 0.5f) {
                return AppTransition.this.mLinearOutSlowInInterpolator.getInterpolation(f / 0.5f);
            }
            return 1.0f;
        }
    };
    private final boolean mGridLayoutRecentsEnabled = SystemProperties.getBoolean("ro.recents.grid", false);
    private final boolean mLowRamRecentsEnabled = ActivityManager.isLowRamDeviceStatic();

    AppTransition(Context context, WindowManagerService windowManagerService) {
        this.mContext = context;
        this.mService = windowManagerService;
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.mConfigShortAnimTime = context.getResources().getInteger(R.integer.config_shortAnimTime);
        this.mDecelerateInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.decelerate_cubic);
        this.mClipRevealTranslationY = (int) (8.0f * this.mContext.getResources().getDisplayMetrics().density);
    }

    boolean isTransitionSet() {
        return this.mNextAppTransition != -1;
    }

    boolean isTransitionEqual(int i) {
        return this.mNextAppTransition == i;
    }

    int getAppTransition() {
        return this.mNextAppTransition;
    }

    private void setAppTransition(int i, int i2) {
        this.mNextAppTransition = i;
        this.mNextAppTransitionFlags |= i2;
        setLastAppTransition(-1, null, null);
        updateBooster();
    }

    void setLastAppTransition(int i, AppWindowToken appWindowToken, AppWindowToken appWindowToken2) {
        this.mLastUsedAppTransition = i;
        this.mLastOpeningApp = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + appWindowToken;
        this.mLastClosingApp = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + appWindowToken2;
    }

    boolean isReady() {
        return this.mAppTransitionState == 1 || this.mAppTransitionState == 3;
    }

    void setReady() {
        setAppTransitionState(1);
        fetchAppTransitionSpecsFromFuture();
    }

    boolean isRunning() {
        return this.mAppTransitionState == 2;
    }

    void setIdle() {
        setAppTransitionState(0);
    }

    boolean isTimeout() {
        return this.mAppTransitionState == 3;
    }

    void setTimeout() {
        setAppTransitionState(3);
    }

    GraphicBuffer getAppTransitionThumbnailHeader(int i) {
        AppTransitionAnimationSpec appTransitionAnimationSpec = this.mNextAppTransitionAnimationsSpecs.get(i);
        if (appTransitionAnimationSpec == null) {
            appTransitionAnimationSpec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (appTransitionAnimationSpec != null) {
            return appTransitionAnimationSpec.buffer;
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

    boolean isNextAppTransitionOpenCrossProfileApps() {
        return this.mNextAppTransitionType == 9;
    }

    boolean isFetchingAppTransitionsSpecs() {
        return this.mNextAppTransitionAnimationsSpecsPending;
    }

    private boolean prepare() {
        if (isRunning()) {
            return false;
        }
        setAppTransitionState(0);
        notifyAppTransitionPendingLocked();
        this.mLastHadClipReveal = false;
        this.mLastClipRevealMaxTranslation = 0;
        this.mLastClipRevealTransitionDuration = 336L;
        return true;
    }

    int goodToGo(int i, AppWindowToken appWindowToken, AppWindowToken appWindowToken2, ArraySet<AppWindowToken> arraySet, ArraySet<AppWindowToken> arraySet2) {
        AnimationAdapter animation;
        this.mNextAppTransition = -1;
        this.mNextAppTransitionFlags = 0;
        setAppTransitionState(2);
        if (appWindowToken != null) {
            animation = appWindowToken.getAnimation();
        } else {
            animation = null;
        }
        int iNotifyAppTransitionStartingLocked = notifyAppTransitionStartingLocked(i, appWindowToken != null ? appWindowToken.token : null, appWindowToken2 != null ? appWindowToken2.token : null, animation != null ? animation.getDurationHint() : 0L, animation != null ? animation.getStatusBarTransitionsStartTime() : SystemClock.uptimeMillis(), 120L);
        this.mService.getDefaultDisplayContentLocked().getDockedDividerController().notifyAppTransitionStarting(arraySet, i);
        if (this.mRemoteAnimationController != null) {
            this.mRemoteAnimationController.goodToGo();
        }
        return iNotifyAppTransitionStartingLocked;
    }

    void clear() {
        this.mNextAppTransitionType = 0;
        this.mNextAppTransitionPackage = null;
        this.mNextAppTransitionAnimationsSpecs.clear();
        this.mRemoteAnimationController = null;
        this.mNextAppTransitionAnimationsSpecsFuture = null;
        this.mDefaultNextAppTransitionAnimationSpec = null;
        this.mAnimationFinishedCallback = null;
    }

    void freeze() {
        int i = this.mNextAppTransition;
        setAppTransition(-1, 0);
        clear();
        setReady();
        notifyAppTransitionCancelledLocked(i);
    }

    private void setAppTransitionState(int i) {
        this.mAppTransitionState = i;
        updateBooster();
    }

    void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setAppTransitionRunning(needsBoosting());
    }

    private boolean needsBoosting() {
        return this.mNextAppTransition != -1 || this.mAppTransitionState == 1 || this.mAppTransitionState == 2 || (this.mService.getRecentsAnimationController() != null);
    }

    void registerListenerLocked(WindowManagerInternal.AppTransitionListener appTransitionListener) {
        this.mListeners.add(appTransitionListener);
    }

    public void notifyAppTransitionFinishedLocked(IBinder iBinder) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionFinishedLocked(iBinder);
        }
    }

    private void notifyAppTransitionPendingLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onAppTransitionPendingLocked();
        }
    }

    private void notifyAppTransitionCancelledLocked(int i) {
        for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
            this.mListeners.get(i2).onAppTransitionCancelledLocked(i);
        }
    }

    private int notifyAppTransitionStartingLocked(int i, IBinder iBinder, IBinder iBinder2, long j, long j2, long j3) {
        int iOnAppTransitionStartingLocked = 0;
        for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
            iOnAppTransitionStartingLocked |= this.mListeners.get(i2).onAppTransitionStartingLocked(i, iBinder, iBinder2, j, j2, j3);
        }
        return iOnAppTransitionStartingLocked;
    }

    private AttributeCache.Entry getCachedAnimations(WindowManager.LayoutParams layoutParams) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            StringBuilder sb = new StringBuilder();
            sb.append("Loading animations: layout params pkg=");
            sb.append(layoutParams != null ? layoutParams.packageName : null);
            sb.append(" resId=0x");
            sb.append(layoutParams != null ? Integer.toHexString(layoutParams.windowAnimations) : null);
            Slog.v("WindowManager", sb.toString());
        }
        if (layoutParams == null || layoutParams.windowAnimations == 0) {
            return null;
        }
        String str = layoutParams.packageName != null ? layoutParams.packageName : PackageManagerService.PLATFORM_PACKAGE_NAME;
        int i = layoutParams.windowAnimations;
        if (((-16777216) & i) == 16777216) {
            str = PackageManagerService.PLATFORM_PACKAGE_NAME;
        }
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "Loading animations: picked package=" + str);
        }
        return AttributeCache.instance().get(str, i, com.android.internal.R.styleable.WindowAnimation, this.mCurrentUserId);
    }

    private AttributeCache.Entry getCachedAnimations(String str, int i) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v("WindowManager", "Loading animations: package=" + str + " resId=0x" + Integer.toHexString(i));
        }
        if (str != null) {
            if (((-16777216) & i) == 16777216) {
                str = PackageManagerService.PLATFORM_PACKAGE_NAME;
            }
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                Slog.v("WindowManager", "Loading animations: picked package=" + str);
            }
            return AttributeCache.instance().get(str, i, com.android.internal.R.styleable.WindowAnimation, this.mCurrentUserId);
        }
        return null;
    }

    Animation loadAnimationAttr(WindowManager.LayoutParams layoutParams, int i, int i2) {
        AttributeCache.Entry cachedAnimations;
        Context context = this.mContext;
        int resourceId = 0;
        if (i >= 0 && (cachedAnimations = getCachedAnimations(layoutParams)) != null) {
            context = cachedAnimations.context;
            resourceId = cachedAnimations.array.getResourceId(i, 0);
        }
        int iUpdateToTranslucentAnimIfNeeded = updateToTranslucentAnimIfNeeded(resourceId, i2);
        if (ResourceId.isValid(iUpdateToTranslucentAnimIfNeeded)) {
            return AnimationUtils.loadAnimation(context, iUpdateToTranslucentAnimIfNeeded);
        }
        return null;
    }

    Animation loadAnimationRes(WindowManager.LayoutParams layoutParams, int i) {
        Context context = this.mContext;
        if (ResourceId.isValid(i)) {
            AttributeCache.Entry cachedAnimations = getCachedAnimations(layoutParams);
            if (cachedAnimations != null) {
                context = cachedAnimations.context;
            }
            return AnimationUtils.loadAnimation(context, i);
        }
        return null;
    }

    private Animation loadAnimationRes(String str, int i) {
        AttributeCache.Entry cachedAnimations;
        if (ResourceId.isValid(i) && (cachedAnimations = getCachedAnimations(str, i)) != null) {
            return AnimationUtils.loadAnimation(cachedAnimations.context, i);
        }
        return null;
    }

    private int updateToTranslucentAnimIfNeeded(int i, int i2) {
        if (i2 == 24 && i == 17432591) {
            return R.anim.activity_translucent_open_enter;
        }
        if (i2 == 25 && i == 17432590) {
            return R.anim.activity_translucent_close_exit;
        }
        return i;
    }

    private static float computePivot(int i, float f) {
        float f2 = f - 1.0f;
        if (Math.abs(f2) < 1.0E-4f) {
            return i;
        }
        return (-i) / f2;
    }

    private Animation createScaleUpAnimationLocked(int i, boolean z, Rect rect) {
        Animation alphaAnimation;
        long j;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int iWidth = rect.width();
        int iHeight = rect.height();
        if (z) {
            float fWidth = this.mTmpRect.width() / iWidth;
            float fHeight = this.mTmpRect.height() / iHeight;
            ScaleAnimation scaleAnimation = new ScaleAnimation(fWidth, 1.0f, fHeight, 1.0f, computePivot(this.mTmpRect.left, fWidth), computePivot(this.mTmpRect.top, fHeight));
            scaleAnimation.setInterpolator(this.mDecelerateInterpolator);
            AlphaAnimation alphaAnimation2 = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation2.setInterpolator(this.mThumbnailFadeOutInterpolator);
            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(scaleAnimation);
            animationSet.addAnimation(alphaAnimation2);
            animationSet.setDetachWallpaper(true);
            alphaAnimation = animationSet;
        } else if (i == 14 || i == 15) {
            AlphaAnimation alphaAnimation3 = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation3.setDetachWallpaper(true);
            alphaAnimation = alphaAnimation3;
        } else {
            alphaAnimation = new AlphaAnimation(1.0f, 1.0f);
        }
        switch (i) {
            case 6:
            case 7:
                j = this.mConfigShortAnimTime;
                break;
            default:
                j = 336;
                break;
        }
        alphaAnimation.setDuration(j);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setInterpolator(this.mDecelerateInterpolator);
        alphaAnimation.initialize(iWidth, iHeight, iWidth, iHeight);
        return alphaAnimation;
    }

    private void getDefaultNextAppTransitionStartRect(Rect rect) {
        if (this.mDefaultNextAppTransitionAnimationSpec == null || this.mDefaultNextAppTransitionAnimationSpec.rect == null) {
            Slog.e("WindowManager", "Starting rect for app requested, but none available", new Throwable());
            rect.setEmpty();
        } else {
            rect.set(this.mDefaultNextAppTransitionAnimationSpec.rect);
        }
    }

    void getNextAppTransitionStartRect(int i, Rect rect) {
        AppTransitionAnimationSpec appTransitionAnimationSpec = this.mNextAppTransitionAnimationsSpecs.get(i);
        if (appTransitionAnimationSpec == null) {
            appTransitionAnimationSpec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (appTransitionAnimationSpec == null || appTransitionAnimationSpec.rect == null) {
            Slog.e("WindowManager", "Starting rect for task: " + i + " requested, but not available", new Throwable());
            rect.setEmpty();
            return;
        }
        rect.set(appTransitionAnimationSpec.rect);
    }

    private void putDefaultNextAppTransitionCoordinates(int i, int i2, int i3, int i4, GraphicBuffer graphicBuffer) {
        this.mDefaultNextAppTransitionAnimationSpec = new AppTransitionAnimationSpec(-1, graphicBuffer, new Rect(i, i2, i3 + i, i4 + i2));
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

    private long calculateClipRevealTransitionDuration(boolean z, float f, float f2, Rect rect) {
        if (!z) {
            return 336L;
        }
        return (long) (336.0f + (Math.max(Math.abs(f) / rect.width(), Math.abs(f2) / rect.height()) * 84.0f));
    }

    private Animation createClipRevealAnimationLocked(int i, boolean z, Rect rect, Rect rect2) {
        long j;
        boolean z2;
        AlphaAnimation alphaAnimation;
        Animation animation;
        float fHeight;
        int i2;
        int i3;
        boolean z3;
        int i4;
        boolean z4;
        int iWidth;
        if (z) {
            int iWidth2 = rect.width();
            int iHeight = rect.height();
            getDefaultNextAppTransitionStartRect(this.mTmpRect);
            if (iHeight > 0) {
                fHeight = this.mTmpRect.top / rect2.height();
            } else {
                fHeight = 0.0f;
            }
            int iHeight2 = this.mClipRevealTranslationY + ((int) ((rect2.height() / 7.0f) * fHeight));
            int iCenterX = this.mTmpRect.centerX();
            int iCenterY = this.mTmpRect.centerY();
            int iWidth3 = this.mTmpRect.width() / 2;
            int iHeight3 = this.mTmpRect.height() / 2;
            int i5 = iCenterX - iWidth3;
            int i6 = i5 - rect.left;
            int i7 = iCenterY - iHeight3;
            int i8 = i7 - rect.top;
            if (rect.top > i7) {
                i2 = i7 - rect.top;
                i8 = 0;
                i3 = 0;
                z3 = true;
            } else {
                i2 = iHeight2;
                i3 = i2;
                z3 = false;
            }
            if (rect.left > i5) {
                i4 = i5 - rect.left;
                i6 = 0;
                z3 = true;
            } else {
                i4 = 0;
            }
            int i9 = iCenterX + iWidth3;
            if (rect.right < i9) {
                i4 = i9 - rect.right;
                iWidth = iWidth2 - this.mTmpRect.width();
                z4 = true;
            } else {
                z4 = z3;
                iWidth = i6;
            }
            int i10 = i4;
            float f = i10;
            float f2 = i2;
            int i11 = i2;
            long jCalculateClipRevealTransitionDuration = calculateClipRevealTransitionDuration(z4, f, f2, rect2);
            ClipRectAnimation clipRectLRAnimation = new ClipRectLRAnimation(iWidth, this.mTmpRect.width() + iWidth, 0, iWidth2);
            clipRectLRAnimation.setInterpolator(this.mClipHorizontalInterpolator);
            clipRectLRAnimation.setDuration((long) (jCalculateClipRevealTransitionDuration / 2.5f));
            TranslateAnimation translateAnimation = new TranslateAnimation(f, 0.0f, f2, 0.0f);
            translateAnimation.setInterpolator(z4 ? TOUCH_RESPONSE_INTERPOLATOR : this.mLinearOutSlowInInterpolator);
            translateAnimation.setDuration(jCalculateClipRevealTransitionDuration);
            ClipRectAnimation clipRectTBAnimation = new ClipRectTBAnimation(i8, i8 + this.mTmpRect.height(), 0, iHeight, i3, 0, this.mLinearOutSlowInInterpolator);
            clipRectTBAnimation.setInterpolator(TOUCH_RESPONSE_INTERPOLATOR);
            clipRectTBAnimation.setDuration(jCalculateClipRevealTransitionDuration);
            AlphaAnimation alphaAnimation2 = new AlphaAnimation(0.5f, 1.0f);
            alphaAnimation2.setDuration(jCalculateClipRevealTransitionDuration / 4);
            alphaAnimation2.setInterpolator(this.mLinearOutSlowInInterpolator);
            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(clipRectLRAnimation);
            animationSet.addAnimation(clipRectTBAnimation);
            animationSet.addAnimation(translateAnimation);
            animationSet.addAnimation(alphaAnimation2);
            animationSet.setZAdjustment(1);
            animationSet.initialize(iWidth2, iHeight, iWidth2, iHeight);
            this.mLastHadClipReveal = true;
            this.mLastClipRevealTransitionDuration = jCalculateClipRevealTransitionDuration;
            this.mLastClipRevealMaxTranslation = z4 ? Math.max(Math.abs(i11), Math.abs(i10)) : 0;
            animation = animationSet;
        } else {
            switch (i) {
                case 6:
                case 7:
                    j = this.mConfigShortAnimTime;
                    break;
                default:
                    j = 336;
                    break;
            }
            if (i == 14 || i == 15) {
                AlphaAnimation alphaAnimation3 = new AlphaAnimation(1.0f, 0.0f);
                z2 = true;
                alphaAnimation3.setDetachWallpaper(true);
                alphaAnimation = alphaAnimation3;
            } else {
                alphaAnimation = new AlphaAnimation(1.0f, 1.0f);
                z2 = true;
            }
            alphaAnimation.setInterpolator(this.mDecelerateInterpolator);
            alphaAnimation.setDuration(j);
            alphaAnimation.setFillAfter(z2);
            animation = alphaAnimation;
        }
        return animation;
    }

    Animation prepareThumbnailAnimationWithDuration(Animation animation, int i, int i2, long j, Interpolator interpolator) {
        if (j > 0) {
            animation.setDuration(j);
        }
        animation.setFillAfter(true);
        if (interpolator != null) {
            animation.setInterpolator(interpolator);
        }
        animation.initialize(i, i2, i, i2);
        return animation;
    }

    Animation prepareThumbnailAnimation(Animation animation, int i, int i2, int i3) {
        int i4;
        switch (i3) {
            case 6:
            case 7:
                i4 = this.mConfigShortAnimTime;
                break;
            default:
                i4 = 336;
                break;
        }
        return prepareThumbnailAnimationWithDuration(animation, i, i2, i4, this.mDecelerateInterpolator);
    }

    int getThumbnailTransitionState(boolean z) {
        if (z) {
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

    GraphicBuffer createCrossProfileAppsThumbnail(int i, Rect rect) {
        int iWidth = rect.width();
        int iHeight = rect.height();
        Picture picture = new Picture();
        Canvas canvasBeginRecording = picture.beginRecording(iWidth, iHeight);
        canvasBeginRecording.drawColor(Color.argb(0.6f, 0.0f, 0.0f, 0.0f));
        int dimensionPixelSize = this.mService.mContext.getResources().getDimensionPixelSize(R.dimen.autofill_dialog_max_width);
        Drawable drawable = this.mService.mContext.getDrawable(i);
        drawable.setBounds((iWidth - dimensionPixelSize) / 2, (iHeight - dimensionPixelSize) / 2, (iWidth + dimensionPixelSize) / 2, (iHeight + dimensionPixelSize) / 2);
        drawable.setTint(this.mContext.getColor(R.color.white));
        drawable.draw(canvasBeginRecording);
        picture.endRecording();
        return Bitmap.createBitmap(picture).createGraphicBufferHandle();
    }

    Animation createCrossProfileAppsThumbnailAnimationLocked(Rect rect) {
        return prepareThumbnailAnimationWithDuration(loadAnimationRes(PackageManagerService.PLATFORM_PACKAGE_NAME, R.anim.cross_profile_apps_thumbnail_enter), rect.width(), rect.height(), 0L, null);
    }

    Animation createThumbnailAspectScaleAnimationLocked(Rect rect, Rect rect2, GraphicBuffer graphicBuffer, int i, int i2, int i3) {
        float f;
        float fHeight;
        float f2;
        float f3;
        float f4;
        float f5;
        int i4;
        AnimationSet animationSet;
        long j;
        int width = graphicBuffer.getWidth();
        float f6 = width > 0 ? width : 1.0f;
        int height = graphicBuffer.getHeight();
        int iWidth = rect.width();
        float f7 = iWidth / f6;
        getNextAppTransitionStartRect(i, this.mTmpRect);
        if (shouldScaleDownThumbnailTransition(i2, i3)) {
            float f8 = this.mTmpRect.left;
            f = this.mTmpRect.top;
            float fWidth = ((this.mTmpRect.width() / 2) * (f7 - 1.0f)) + rect.left;
            fHeight = ((rect.height() / 2) * (1.0f - (1.0f / f7))) + rect.top;
            float fWidth2 = this.mTmpRect.width() / 2;
            float fHeight2 = (rect.height() / 2) / f7;
            if (this.mGridLayoutRecentsEnabled) {
                float f9 = height;
                f -= f9;
                fHeight -= f9 * f7;
            }
            f5 = fWidth2;
            f2 = f8;
            f4 = fHeight2;
            f3 = fWidth;
        } else {
            float f10 = this.mTmpRect.left;
            f = this.mTmpRect.top;
            float f11 = rect.left;
            fHeight = rect.top;
            f2 = f10;
            f3 = f11;
            f4 = 0.0f;
            f5 = 0.0f;
        }
        float f12 = f;
        float f13 = f2;
        float f14 = f3;
        long aspectScaleDuration = getAspectScaleDuration();
        Interpolator aspectScaleInterpolator = getAspectScaleInterpolator();
        if (this.mNextAppTransitionScaleUp) {
            float f15 = fHeight;
            i4 = iWidth;
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, f7, 1.0f, f7, f5, f4);
            scaleAnimation.setInterpolator(aspectScaleInterpolator);
            scaleAnimation.setDuration(aspectScaleDuration);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setInterpolator(this.mNextAppTransition == 19 ? THUMBNAIL_DOCK_INTERPOLATOR : this.mThumbnailFadeOutInterpolator);
            if (this.mNextAppTransition == 19) {
                j = aspectScaleDuration / 2;
            } else {
                j = aspectScaleDuration;
            }
            alphaAnimation.setDuration(j);
            Animation animationCreateCurvedMotion = createCurvedMotion(f13, f14, f12, f15);
            animationCreateCurvedMotion.setInterpolator(aspectScaleInterpolator);
            animationCreateCurvedMotion.setDuration(aspectScaleDuration);
            this.mTmpFromClipRect.set(0, 0, width, height);
            this.mTmpToClipRect.set(rect);
            this.mTmpToClipRect.offsetTo(0, 0);
            this.mTmpToClipRect.right = (int) (this.mTmpToClipRect.right / f7);
            this.mTmpToClipRect.bottom = (int) (this.mTmpToClipRect.bottom / f7);
            if (rect2 != null) {
                this.mTmpToClipRect.inset((int) ((-rect2.left) * f7), (int) ((-rect2.top) * f7), (int) ((-rect2.right) * f7), (int) ((-rect2.bottom) * f7));
            }
            ClipRectAnimation clipRectAnimation = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
            clipRectAnimation.setInterpolator(aspectScaleInterpolator);
            clipRectAnimation.setDuration(aspectScaleDuration);
            AnimationSet animationSet2 = new AnimationSet(false);
            animationSet2.addAnimation(scaleAnimation);
            if (!this.mGridLayoutRecentsEnabled) {
                animationSet2.addAnimation(alphaAnimation);
            }
            animationSet2.addAnimation(animationCreateCurvedMotion);
            animationSet2.addAnimation(clipRectAnimation);
            animationSet = animationSet2;
        } else {
            i4 = iWidth;
            ScaleAnimation scaleAnimation2 = new ScaleAnimation(f7, 1.0f, f7, 1.0f, f5, f4);
            scaleAnimation2.setInterpolator(aspectScaleInterpolator);
            scaleAnimation2.setDuration(aspectScaleDuration);
            AlphaAnimation alphaAnimation2 = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation2.setInterpolator(this.mThumbnailFadeInInterpolator);
            alphaAnimation2.setDuration(aspectScaleDuration);
            Animation animationCreateCurvedMotion2 = createCurvedMotion(f14, f13, fHeight, f12);
            animationCreateCurvedMotion2.setInterpolator(aspectScaleInterpolator);
            animationCreateCurvedMotion2.setDuration(aspectScaleDuration);
            animationSet = new AnimationSet(false);
            animationSet.addAnimation(scaleAnimation2);
            if (!this.mGridLayoutRecentsEnabled) {
                animationSet.addAnimation(alphaAnimation2);
            }
            animationSet.addAnimation(animationCreateCurvedMotion2);
        }
        return prepareThumbnailAnimationWithDuration(animationSet, i4, rect.height(), 0L, null);
    }

    private Animation createCurvedMotion(float f, float f2, float f3, float f4) {
        if (Math.abs(f2 - f) < 1.0f || this.mNextAppTransition != 19) {
            return new TranslateAnimation(f, f2, f3, f4);
        }
        return new CurvedTranslateAnimation(createCurvedPath(f, f2, f3, f4));
    }

    private Path createCurvedPath(float f, float f2, float f3, float f4) {
        Path path = new Path();
        path.moveTo(f, f3);
        if (f3 > f4) {
            path.cubicTo(f, f3, f2, (0.9f * f3) + (0.1f * f4), f2, f4);
        } else {
            path.cubicTo(f, f3, f, (0.1f * f3) + (0.9f * f4), f2, f4);
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

    Animation createAspectScaledThumbnailEnterExitAnimationLocked(int i, int i2, int i3, int i4, Rect rect, Rect rect2, Rect rect3, Rect rect4, boolean z, int i5) {
        ClipRectAnimation clipRectAnimation;
        Animation animationCreateCurvedMotion;
        Animation animationCreateAspectScaledThumbnailExitFreeformAnimationLocked;
        ClipRectAnimation clipRectAnimation2;
        Animation animationCreateCurvedMotion2;
        int iWidth = rect.width();
        int iHeight = rect.height();
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int iWidth2 = this.mTmpRect.width();
        float f = iWidth2 > 0 ? iWidth2 : 1.0f;
        int iHeight2 = this.mTmpRect.height();
        float f2 = iHeight2 > 0 ? iHeight2 : 1.0f;
        int i6 = (this.mTmpRect.left - rect.left) - rect2.left;
        int i7 = this.mTmpRect.top - rect.top;
        switch (i) {
            case 0:
            case 3:
                boolean z2 = i == 0;
                if (z && z2) {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = createAspectScaledThumbnailEnterFreeformAnimationLocked(rect, rect3, i5);
                } else if (z) {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = createAspectScaledThumbnailExitFreeformAnimationLocked(rect, rect3, i5);
                } else {
                    AnimationSet animationSet = new AnimationSet(true);
                    this.mTmpFromClipRect.set(rect);
                    this.mTmpToClipRect.set(rect);
                    this.mTmpFromClipRect.offsetTo(0, 0);
                    this.mTmpToClipRect.offsetTo(0, 0);
                    this.mTmpFromClipRect.inset(rect2);
                    this.mNextAppTransitionInsets.set(rect2);
                    if (shouldScaleDownThumbnailTransition(i2, i3)) {
                        float f3 = f / ((iWidth - rect2.left) - rect2.right);
                        if (!this.mGridLayoutRecentsEnabled) {
                            this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + ((int) (f2 / f3));
                        }
                        this.mNextAppTransitionInsets.set(rect2);
                        ScaleAnimation scaleAnimation = new ScaleAnimation(z2 ? f3 : 1.0f, z2 ? 1.0f : f3, z2 ? f3 : 1.0f, z2 ? 1.0f : f3, rect.width() / 2.0f, (rect.height() / 2.0f) + rect2.top);
                        float f4 = this.mTmpRect.left - rect.left;
                        float fWidth = (rect.width() / 2.0f) - ((rect.width() / 2.0f) * f3);
                        float f5 = this.mTmpRect.top - rect.top;
                        float fHeight = (rect.height() / 2.0f) - ((rect.height() / 2.0f) * f3);
                        if (this.mLowRamRecentsEnabled && rect2.top == 0 && z2) {
                            this.mTmpFromClipRect.top += rect4.top;
                            fHeight += rect4.top;
                        }
                        float f6 = f4 - fWidth;
                        float f7 = f5 - fHeight;
                        if (z2) {
                            clipRectAnimation2 = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                        } else {
                            clipRectAnimation2 = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                        }
                        if (z2) {
                            animationCreateCurvedMotion2 = createCurvedMotion(f6, 0.0f, f7 - rect2.top, 0.0f);
                        } else {
                            animationCreateCurvedMotion2 = createCurvedMotion(0.0f, f6, 0.0f, f7 - rect2.top);
                        }
                        animationSet.addAnimation(clipRectAnimation2);
                        animationSet.addAnimation(scaleAnimation);
                        animationSet.addAnimation(animationCreateCurvedMotion2);
                    } else {
                        this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + iHeight2;
                        this.mTmpFromClipRect.right = this.mTmpFromClipRect.left + iWidth2;
                        if (z2) {
                            clipRectAnimation = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                        } else {
                            clipRectAnimation = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                        }
                        if (z2) {
                            animationCreateCurvedMotion = createCurvedMotion(i6, 0.0f, i7 - rect2.top, 0.0f);
                        } else {
                            animationCreateCurvedMotion = createCurvedMotion(0.0f, i6, 0.0f, i7 - rect2.top);
                        }
                        animationSet.addAnimation(clipRectAnimation);
                        animationSet.addAnimation(animationCreateCurvedMotion);
                    }
                    animationSet.setZAdjustment(1);
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = animationSet;
                }
                break;
            case 1:
                if (i4 == 14) {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = new AlphaAnimation(1.0f, 0.0f);
                } else {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            case 2:
                if (i4 == 14) {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = new AlphaAnimation(0.0f, 1.0f);
                } else {
                    animationCreateAspectScaledThumbnailExitFreeformAnimationLocked = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        return prepareThumbnailAnimationWithDuration(animationCreateAspectScaledThumbnailExitFreeformAnimationLocked, iWidth, iHeight, getAspectScaleDuration(), getAspectScaleInterpolator());
    }

    private Animation createAspectScaledThumbnailEnterFreeformAnimationLocked(Rect rect, Rect rect2, int i) {
        getNextAppTransitionStartRect(i, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(this.mTmpRect, rect, rect2, true);
    }

    private Animation createAspectScaledThumbnailExitFreeformAnimationLocked(Rect rect, Rect rect2, int i) {
        getNextAppTransitionStartRect(i, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(rect, this.mTmpRect, rect2, false);
    }

    private AnimationSet createAspectScaledThumbnailFreeformAnimationLocked(Rect rect, Rect rect2, Rect rect3, boolean z) {
        int i;
        ScaleAnimation scaleAnimation;
        TranslateAnimation translateAnimation;
        float fWidth = rect.width();
        float fHeight = rect.height();
        float fWidth2 = rect2.width();
        float fHeight2 = rect2.height();
        float f = z ? fWidth / fWidth2 : fWidth2 / fWidth;
        float f2 = z ? fHeight / fHeight2 : fHeight2 / fHeight;
        AnimationSet animationSet = new AnimationSet(true);
        if (rect3 != null) {
            i = rect3.left + rect3.right;
        } else {
            i = 0;
        }
        int i2 = rect3 != null ? rect3.top + rect3.bottom : 0;
        if (z) {
            fWidth = fWidth2;
        }
        float f3 = (fWidth + i) / 2.0f;
        if (z) {
            fHeight = fHeight2;
        }
        float f4 = (fHeight + i2) / 2.0f;
        if (z) {
            scaleAnimation = new ScaleAnimation(f, 1.0f, f2, 1.0f, f3, f4);
        } else {
            scaleAnimation = new ScaleAnimation(1.0f, f, 1.0f, f2, f3, f4);
        }
        int iWidth = rect.left + (rect.width() / 2);
        int iHeight = rect.top + (rect.height() / 2);
        int iWidth2 = rect2.left + (rect2.width() / 2);
        int iHeight2 = rect2.top + (rect2.height() / 2);
        int i3 = z ? iWidth - iWidth2 : iWidth2 - iWidth;
        int i4 = z ? iHeight - iHeight2 : iHeight2 - iHeight;
        if (z) {
            translateAnimation = new TranslateAnimation(i3, 0.0f, i4, 0.0f);
        } else {
            translateAnimation = new TranslateAnimation(0.0f, i3, 0.0f, i4);
        }
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        final IRemoteCallback iRemoteCallback = this.mAnimationFinishedCallback;
        if (iRemoteCallback != null) {
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    AppTransition.this.mService.mH.obtainMessage(26, iRemoteCallback).sendToTarget();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        return animationSet;
    }

    Animation createThumbnailScaleAnimationLocked(int i, int i2, int i3, GraphicBuffer graphicBuffer) {
        Animation scaleAnimation;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int width = graphicBuffer.getWidth();
        float f = width > 0 ? width : 1.0f;
        int height = graphicBuffer.getHeight();
        float f2 = height > 0 ? height : 1.0f;
        if (this.mNextAppTransitionScaleUp) {
            float f3 = i / f;
            float f4 = i2 / f2;
            ScaleAnimation scaleAnimation2 = new ScaleAnimation(1.0f, f3, 1.0f, f4, computePivot(this.mTmpRect.left, 1.0f / f3), computePivot(this.mTmpRect.top, 1.0f / f4));
            scaleAnimation2.setInterpolator(this.mDecelerateInterpolator);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setInterpolator(this.mThumbnailFadeOutInterpolator);
            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(scaleAnimation2);
            animationSet.addAnimation(alphaAnimation);
            scaleAnimation = animationSet;
        } else {
            float f5 = i / f;
            float f6 = i2 / f2;
            scaleAnimation = new ScaleAnimation(f5, 1.0f, f6, 1.0f, computePivot(this.mTmpRect.left, 1.0f / f5), computePivot(this.mTmpRect.top, 1.0f / f6));
        }
        return prepareThumbnailAnimation(scaleAnimation, i, i2, i3);
    }

    Animation createThumbnailEnterExitAnimationLocked(int i, Rect rect, int i2, int i3) {
        Animation scaleAnimation;
        int iWidth = rect.width();
        int iHeight = rect.height();
        GraphicBuffer appTransitionThumbnailHeader = getAppTransitionThumbnailHeader(i3);
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int width = appTransitionThumbnailHeader != null ? appTransitionThumbnailHeader.getWidth() : iWidth;
        float f = width > 0 ? width : 1.0f;
        int height = appTransitionThumbnailHeader != null ? appTransitionThumbnailHeader.getHeight() : iHeight;
        float f2 = height > 0 ? height : 1.0f;
        switch (i) {
            case 0:
                float f3 = f / iWidth;
                float f4 = f2 / iHeight;
                scaleAnimation = new ScaleAnimation(f3, 1.0f, f4, 1.0f, computePivot(this.mTmpRect.left, f3), computePivot(this.mTmpRect.top, f4));
                break;
            case 1:
                if (i2 == 14) {
                    scaleAnimation = new AlphaAnimation(1.0f, 0.0f);
                } else {
                    scaleAnimation = new AlphaAnimation(1.0f, 1.0f);
                }
                break;
            case 2:
                scaleAnimation = new AlphaAnimation(1.0f, 1.0f);
                break;
            case 3:
                float f5 = f / iWidth;
                float f6 = f2 / iHeight;
                ScaleAnimation scaleAnimation2 = new ScaleAnimation(1.0f, f5, 1.0f, f6, computePivot(this.mTmpRect.left, f5), computePivot(this.mTmpRect.top, f6));
                AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(scaleAnimation2);
                animationSet.addAnimation(alphaAnimation);
                animationSet.setZAdjustment(1);
                scaleAnimation = animationSet;
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        return prepareThumbnailAnimation(scaleAnimation, iWidth, iHeight, i2);
    }

    private Animation createRelaunchAnimation(Rect rect, Rect rect2) {
        getDefaultNextAppTransitionStartRect(this.mTmpFromClipRect);
        this.mTmpFromClipRect.offset(-this.mTmpFromClipRect.left, -this.mTmpFromClipRect.top);
        int i = 0;
        this.mTmpToClipRect.set(0, 0, rect.width(), rect.height());
        AnimationSet animationSet = new AnimationSet(true);
        float fWidth = this.mTmpFromClipRect.width();
        float fWidth2 = this.mTmpToClipRect.width();
        float fHeight = this.mTmpFromClipRect.height();
        float fHeight2 = (this.mTmpToClipRect.height() - rect2.top) - rect2.bottom;
        if (fWidth <= fWidth2 && fHeight <= fHeight2) {
            animationSet.addAnimation(new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect));
        } else {
            animationSet.addAnimation(new ScaleAnimation(fWidth / fWidth2, 1.0f, fHeight / fHeight2, 1.0f));
            i = (int) ((rect2.top * fHeight) / fHeight2);
        }
        animationSet.addAnimation(new TranslateAnimation(r0 - rect.left, 0.0f, (r1 - rect.top) - i, 0.0f));
        animationSet.setDuration(336L);
        animationSet.setZAdjustment(1);
        return animationSet;
    }

    boolean canSkipFirstFrame() {
        return (this.mNextAppTransitionType == 1 || this.mNextAppTransitionType == 7 || this.mNextAppTransitionType == 8 || this.mNextAppTransition == 20) ? false : true;
    }

    RemoteAnimationController getRemoteAnimationController() {
        return this.mRemoteAnimationController;
    }

    Animation loadAnimation(WindowManager.LayoutParams layoutParams, int i, boolean z, int i2, int i3, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, boolean z2, boolean z3, int i4) {
        if (isKeyguardGoingAwayTransit(i) && z) {
            return loadKeyguardExitAnimation(i);
        }
        if (i == 22) {
            return null;
        }
        if (i == 23 && !z) {
            return loadAnimationRes(layoutParams, R.anim.task_fragment_clear_top_open_enter);
        }
        if (i == 26) {
            return null;
        }
        if (z2 && (i == 6 || i == 8 || i == 10)) {
            Animation animationLoadAnimationRes = loadAnimationRes(layoutParams, z ? R.anim.slide_in_up : R.anim.slide_out_down);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationLoadAnimationRes;
            }
            Slog.v("WindowManager", "applyAnimation voice: anim=" + animationLoadAnimationRes + " transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationLoadAnimationRes;
        }
        if (z2 && (i == 7 || i == 9 || i == 11)) {
            Animation animationLoadAnimationRes2 = loadAnimationRes(layoutParams, z ? R.anim.slide_in_exit_micro : R.anim.slide_in_right);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationLoadAnimationRes2;
            }
            Slog.v("WindowManager", "applyAnimation voice: anim=" + animationLoadAnimationRes2 + " transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationLoadAnimationRes2;
        }
        if (i == 18) {
            Animation animationCreateRelaunchAnimation = createRelaunchAnimation(rect, rect3);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationCreateRelaunchAnimation;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationCreateRelaunchAnimation + " nextAppTransition=" + this.mNextAppTransition + " transit=" + appTransitionToString(i) + " Callers=" + Debug.getCallers(3));
            return animationCreateRelaunchAnimation;
        }
        if (this.mNextAppTransitionType == 1) {
            Animation animationLoadAnimationRes3 = loadAnimationRes(this.mNextAppTransitionPackage, z ? this.mNextAppTransitionEnter : this.mNextAppTransitionExit);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationLoadAnimationRes3;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationLoadAnimationRes3 + " nextAppTransition=ANIM_CUSTOM transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationLoadAnimationRes3;
        }
        if (this.mNextAppTransitionType == 7) {
            Animation animationLoadAnimationRes4 = loadAnimationRes(this.mNextAppTransitionPackage, this.mNextAppTransitionInPlace);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationLoadAnimationRes4;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationLoadAnimationRes4 + " nextAppTransition=ANIM_CUSTOM_IN_PLACE transit=" + appTransitionToString(i) + " Callers=" + Debug.getCallers(3));
            return animationLoadAnimationRes4;
        }
        if (this.mNextAppTransitionType == 8) {
            Animation animationCreateClipRevealAnimationLocked = createClipRevealAnimationLocked(i, z, rect, rect2);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationCreateClipRevealAnimationLocked;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationCreateClipRevealAnimationLocked + " nextAppTransition=ANIM_CLIP_REVEAL transit=" + appTransitionToString(i) + " Callers=" + Debug.getCallers(3));
            return animationCreateClipRevealAnimationLocked;
        }
        if (this.mNextAppTransitionType == 2) {
            Animation animationCreateScaleUpAnimationLocked = createScaleUpAnimationLocked(i, z, rect);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationCreateScaleUpAnimationLocked;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationCreateScaleUpAnimationLocked + " nextAppTransition=ANIM_SCALE_UP transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationCreateScaleUpAnimationLocked;
        }
        if (this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 4) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 3;
            Animation animationCreateThumbnailEnterExitAnimationLocked = createThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(z), rect, i, i4);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationCreateThumbnailEnterExitAnimationLocked;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationCreateThumbnailEnterExitAnimationLocked + " nextAppTransition=" + (this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_SCALE_UP" : "ANIM_THUMBNAIL_SCALE_DOWN") + " transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationCreateThumbnailEnterExitAnimationLocked;
        }
        int i5 = 5;
        if (this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6) {
            this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 5;
            Animation animationCreateAspectScaledThumbnailEnterExitAnimationLocked = createAspectScaledThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(z), i2, i3, i, rect, rect3, rect4, rect5, z3, i4);
            if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
                return animationCreateAspectScaledThumbnailEnterExitAnimationLocked;
            }
            Slog.v("WindowManager", "applyAnimation: anim=" + animationCreateAspectScaledThumbnailEnterExitAnimationLocked + " nextAppTransition=" + (this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_ASPECT_SCALE_UP" : "ANIM_THUMBNAIL_ASPECT_SCALE_DOWN") + " transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
            return animationCreateAspectScaledThumbnailEnterExitAnimationLocked;
        }
        if (this.mNextAppTransitionType == 9 && z) {
            Animation animationLoadAnimationRes5 = loadAnimationRes(PackageManagerService.PLATFORM_PACKAGE_NAME, R.anim.search_bar_exit);
            Slog.v("WindowManager", "applyAnimation NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS: anim=" + animationLoadAnimationRes5 + " transit=" + appTransitionToString(i) + " isEntrance=true Callers=" + Debug.getCallers(3));
            return animationLoadAnimationRes5;
        }
        if (i != 19) {
            switch (i) {
                case 6:
                    if (z) {
                        i5 = 4;
                    }
                    break;
                case 7:
                    i5 = !z ? 7 : 6;
                    break;
                case 8:
                    i5 = !z ? 9 : 8;
                    break;
                case 9:
                    i5 = !z ? 11 : 10;
                    break;
                case 10:
                    i5 = z ? 12 : 13;
                    break;
                case 11:
                    i5 = z ? 14 : 15;
                    break;
                case 12:
                    i5 = !z ? 19 : 18;
                    break;
                case 13:
                    i5 = z ? 16 : 17;
                    break;
                case 14:
                    i5 = z ? 20 : 21;
                    break;
                case 15:
                    i5 = !z ? 23 : 22;
                    break;
                case 16:
                    i5 = z ? 25 : 24;
                    break;
                default:
                    switch (i) {
                        case 24:
                            break;
                        case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                            break;
                        default:
                            i5 = 0;
                            break;
                    }
                    break;
            }
        }
        Animation animationLoadAnimationAttr = i5 != 0 ? loadAnimationAttr(layoutParams, i5, i) : null;
        if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS && !WindowManagerDebugConfig.DEBUG_ANIM) {
            return animationLoadAnimationAttr;
        }
        Slog.v("WindowManager", "applyAnimation: anim=" + animationLoadAnimationAttr + " animAttr=0x" + Integer.toHexString(i5) + " transit=" + appTransitionToString(i) + " isEntrance=" + z + " Callers=" + Debug.getCallers(3));
        return animationLoadAnimationAttr;
    }

    private Animation loadKeyguardExitAnimation(int i) {
        if ((this.mNextAppTransitionFlags & 2) != 0) {
            return null;
        }
        return this.mService.mPolicy.createHiddenByKeyguardExit(i == 21, (this.mNextAppTransitionFlags & 1) != 0);
    }

    int getAppStackClipMode() {
        if (this.mNextAppTransition == 20 || this.mNextAppTransition == 21) {
            return 1;
        }
        if (this.mNextAppTransition == 18 || this.mNextAppTransition == 19 || this.mNextAppTransitionType == 8) {
            return 2;
        }
        return 0;
    }

    public int getTransitFlags() {
        return this.mNextAppTransitionFlags;
    }

    void postAnimationCallback() {
        if (this.mNextAppTransitionCallback != null) {
            this.mService.mH.sendMessage(this.mService.mH.obtainMessage(26, this.mNextAppTransitionCallback));
            this.mNextAppTransitionCallback = null;
        }
    }

    void overridePendingAppTransition(String str, int i, int i2, IRemoteCallback iRemoteCallback) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 1;
            this.mNextAppTransitionPackage = str;
            this.mNextAppTransitionEnter = i;
            this.mNextAppTransitionExit = i2;
            postAnimationCallback();
            this.mNextAppTransitionCallback = iRemoteCallback;
        }
    }

    void overridePendingAppTransitionScaleUp(int i, int i2, int i3, int i4) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 2;
            putDefaultNextAppTransitionCoordinates(i, i2, i3, i4, null);
            postAnimationCallback();
        }
    }

    void overridePendingAppTransitionClipReveal(int i, int i2, int i3, int i4) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 8;
            putDefaultNextAppTransitionCoordinates(i, i2, i3, i4, null);
            postAnimationCallback();
        }
    }

    void overridePendingAppTransitionThumb(GraphicBuffer graphicBuffer, int i, int i2, IRemoteCallback iRemoteCallback, boolean z) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = z ? 3 : 4;
            this.mNextAppTransitionScaleUp = z;
            putDefaultNextAppTransitionCoordinates(i, i2, 0, 0, graphicBuffer);
            postAnimationCallback();
            this.mNextAppTransitionCallback = iRemoteCallback;
        }
    }

    void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer graphicBuffer, int i, int i2, int i3, int i4, IRemoteCallback iRemoteCallback, boolean z) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = z ? 5 : 6;
            this.mNextAppTransitionScaleUp = z;
            putDefaultNextAppTransitionCoordinates(i, i2, i3, i4, graphicBuffer);
            postAnimationCallback();
            this.mNextAppTransitionCallback = iRemoteCallback;
        }
    }

    void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, IRemoteCallback iRemoteCallback, IRemoteCallback iRemoteCallback2, boolean z) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = z ? 5 : 6;
            this.mNextAppTransitionScaleUp = z;
            if (appTransitionAnimationSpecArr != null) {
                for (int i = 0; i < appTransitionAnimationSpecArr.length; i++) {
                    AppTransitionAnimationSpec appTransitionAnimationSpec = appTransitionAnimationSpecArr[i];
                    if (appTransitionAnimationSpec != null) {
                        this.mNextAppTransitionAnimationsSpecs.put(appTransitionAnimationSpec.taskId, appTransitionAnimationSpec);
                        if (i == 0) {
                            Rect rect = appTransitionAnimationSpec.rect;
                            putDefaultNextAppTransitionCoordinates(rect.left, rect.top, rect.width(), rect.height(), appTransitionAnimationSpec.buffer);
                        }
                    }
                }
            }
            postAnimationCallback();
            this.mNextAppTransitionCallback = iRemoteCallback;
            this.mAnimationFinishedCallback = iRemoteCallback2;
        }
    }

    void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, IRemoteCallback iRemoteCallback, boolean z) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = z ? 5 : 6;
            this.mNextAppTransitionAnimationsSpecsFuture = iAppTransitionAnimationSpecsFuture;
            this.mNextAppTransitionScaleUp = z;
            this.mNextAppTransitionFutureCallback = iRemoteCallback;
        }
    }

    void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = 10;
            this.mRemoteAnimationController = new RemoteAnimationController(this.mService, remoteAnimationAdapter, this.mService.mH);
        }
    }

    void overrideInPlaceAppTransition(String str, int i) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 7;
            this.mNextAppTransitionPackage = str;
            this.mNextAppTransitionInPlace = i;
        }
    }

    void overridePendingAppTransitionStartCrossProfileApps() {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 9;
            postAnimationCallback();
        }
    }

    private boolean canOverridePendingAppTransition() {
        return isTransitionSet() && this.mNextAppTransitionType != 10;
    }

    private void fetchAppTransitionSpecsFromFuture() {
        if (this.mNextAppTransitionAnimationsSpecsFuture != null) {
            this.mNextAppTransitionAnimationsSpecsPending = true;
            final IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture = this.mNextAppTransitionAnimationsSpecsFuture;
            this.mNextAppTransitionAnimationsSpecsFuture = null;
            this.mDefaultExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    AppTransition.lambda$fetchAppTransitionSpecsFromFuture$0(this.f$0, iAppTransitionAnimationSpecsFuture);
                }
            });
        }
    }

    public static void lambda$fetchAppTransitionSpecsFromFuture$0(AppTransition appTransition, IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture) {
        AppTransitionAnimationSpec[] appTransitionAnimationSpecArr;
        try {
            Binder.allowBlocking(iAppTransitionAnimationSpecsFuture.asBinder());
            appTransitionAnimationSpecArr = iAppTransitionAnimationSpecsFuture.get();
        } catch (RemoteException e) {
            Slog.w("WindowManager", "Failed to fetch app transition specs: " + e);
            appTransitionAnimationSpecArr = null;
        }
        synchronized (appTransition.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                appTransition.mNextAppTransitionAnimationsSpecsPending = false;
                appTransition.overridePendingAppTransitionMultiThumb(appTransitionAnimationSpecArr, appTransition.mNextAppTransitionFutureCallback, null, appTransition.mNextAppTransitionScaleUp);
                appTransition.mNextAppTransitionFutureCallback = null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        appTransition.mService.requestTraversal();
    }

    public String toString() {
        return "mNextAppTransition=" + appTransitionToString(this.mNextAppTransition);
    }

    public static String appTransitionToString(int i) {
        switch (i) {
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
                return "<UNKNOWN: " + i + ">";
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
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                return "TRANSIT_DOCK_TASK_FROM_RECENTS";
            case 20:
                return "TRANSIT_KEYGUARD_GOING_AWAY";
            case BackupHandler.MSG_OP_COMPLETE:
                return "TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER";
            case WindowManagerService.H.REPORT_HARD_KEYBOARD_STATUS_CHANGE:
                return "TRANSIT_KEYGUARD_OCCLUDE";
            case WindowManagerService.H.BOOT_TIMEOUT:
                return "TRANSIT_KEYGUARD_UNOCCLUDE";
            case 24:
                return "TRANSIT_TRANSLUCENT_ACTIVITY_OPEN";
            case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                return "TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE";
            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                return "TRANSIT_CRASHING_ACTIVITY_CLOSE";
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
            case 8:
            default:
                return "unknown type=" + this.mNextAppTransitionType;
            case 9:
                return "NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS";
        }
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1159641169921L, this.mAppTransitionState);
        protoOutputStream.write(1159641169922L, this.mLastUsedAppTransition);
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.println(this);
        printWriter.print(str);
        printWriter.print("mAppTransitionState=");
        printWriter.println(appStateToString());
        if (this.mNextAppTransitionType != 0) {
            printWriter.print(str);
            printWriter.print("mNextAppTransitionType=");
            printWriter.println(transitTypeToString());
        }
        switch (this.mNextAppTransitionType) {
            case 1:
                printWriter.print(str);
                printWriter.print("mNextAppTransitionPackage=");
                printWriter.println(this.mNextAppTransitionPackage);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionEnter=0x");
                printWriter.print(Integer.toHexString(this.mNextAppTransitionEnter));
                printWriter.print(" mNextAppTransitionExit=0x");
                printWriter.println(Integer.toHexString(this.mNextAppTransitionExit));
                break;
            case 2:
                getDefaultNextAppTransitionStartRect(this.mTmpRect);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionStartX=");
                printWriter.print(this.mTmpRect.left);
                printWriter.print(" mNextAppTransitionStartY=");
                printWriter.println(this.mTmpRect.top);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionStartWidth=");
                printWriter.print(this.mTmpRect.width());
                printWriter.print(" mNextAppTransitionStartHeight=");
                printWriter.println(this.mTmpRect.height());
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                printWriter.print(str);
                printWriter.print("mDefaultNextAppTransitionAnimationSpec=");
                printWriter.println(this.mDefaultNextAppTransitionAnimationSpec);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionAnimationsSpecs=");
                printWriter.println(this.mNextAppTransitionAnimationsSpecs);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionScaleUp=");
                printWriter.println(this.mNextAppTransitionScaleUp);
                break;
            case 7:
                printWriter.print(str);
                printWriter.print("mNextAppTransitionPackage=");
                printWriter.println(this.mNextAppTransitionPackage);
                printWriter.print(str);
                printWriter.print("mNextAppTransitionInPlace=0x");
                printWriter.print(Integer.toHexString(this.mNextAppTransitionInPlace));
                break;
        }
        if (this.mNextAppTransitionCallback != null) {
            printWriter.print(str);
            printWriter.print("mNextAppTransitionCallback=");
            printWriter.println(this.mNextAppTransitionCallback);
        }
        if (this.mLastUsedAppTransition != 0) {
            printWriter.print(str);
            printWriter.print("mLastUsedAppTransition=");
            printWriter.println(appTransitionToString(this.mLastUsedAppTransition));
            printWriter.print(str);
            printWriter.print("mLastOpeningApp=");
            printWriter.println(this.mLastOpeningApp);
            printWriter.print(str);
            printWriter.print("mLastClosingApp=");
            printWriter.println(this.mLastClosingApp);
        }
    }

    public void setCurrentUser(int i) {
        this.mCurrentUserId = i;
    }

    boolean prepareAppTransitionLocked(int i, boolean z, int i2, boolean z2) {
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            Slog.v("WindowManager", "Prepare app transition: transit=" + appTransitionToString(i) + " " + this + " alwaysKeepCurrent=" + z + " Callers=" + Debug.getCallers(3));
        }
        boolean z3 = !isKeyguardTransit(this.mNextAppTransition) && i == 26;
        if (z2 || isKeyguardTransit(i) || !isTransitionSet() || this.mNextAppTransition == 0 || z3) {
            setAppTransition(i, i2);
        } else if (!z && !isKeyguardTransit(this.mNextAppTransition) && this.mNextAppTransition != 26) {
            if (i == 8 && isTransitionEqual(9)) {
                setAppTransition(i, i2);
            } else if (i == 6 && isTransitionEqual(7)) {
                setAppTransition(i, i2);
            } else if (isTaskTransit(i) && isActivityTransit(this.mNextAppTransition)) {
                setAppTransition(i, i2);
            }
        }
        boolean zPrepare = prepare();
        if (isTransitionSet()) {
            this.mService.mH.removeMessages(13);
            this.mService.mH.sendEmptyMessageDelayed(13, APP_TRANSITION_TIMEOUT_MS);
        }
        return zPrepare;
    }

    public static boolean isKeyguardGoingAwayTransit(int i) {
        return i == 20 || i == 21;
    }

    private static boolean isKeyguardTransit(int i) {
        return isKeyguardGoingAwayTransit(i) || i == 22 || i == 23;
    }

    static boolean isTaskTransit(int i) {
        return isTaskOpenTransit(i) || i == 9 || i == 11 || i == 17;
    }

    private static boolean isTaskOpenTransit(int i) {
        return i == 8 || i == 16 || i == 10;
    }

    static boolean isActivityTransit(int i) {
        return i == 6 || i == 7 || i == 18;
    }

    private boolean shouldScaleDownThumbnailTransition(int i, int i2) {
        return this.mGridLayoutRecentsEnabled || i2 == 1;
    }
}
