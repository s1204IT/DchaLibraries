package android.app;

import android.app.ExitTransitionCoordinator;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.util.Pair;
import android.util.Slog;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.RemoteAnimationAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.android.internal.R;
import java.util.ArrayList;

public class ActivityOptions {
    public static final int ANIM_CLIP_REVEAL = 11;
    public static final int ANIM_CUSTOM = 1;
    public static final int ANIM_CUSTOM_IN_PLACE = 10;
    public static final int ANIM_DEFAULT = 6;
    public static final int ANIM_LAUNCH_TASK_BEHIND = 7;
    public static final int ANIM_NONE = 0;
    public static final int ANIM_OPEN_CROSS_PROFILE_APPS = 12;
    public static final int ANIM_REMOTE_ANIMATION = 13;
    public static final int ANIM_SCALE_UP = 2;
    public static final int ANIM_SCENE_TRANSITION = 5;
    public static final int ANIM_THUMBNAIL_ASPECT_SCALE_DOWN = 9;
    public static final int ANIM_THUMBNAIL_ASPECT_SCALE_UP = 8;
    public static final int ANIM_THUMBNAIL_SCALE_DOWN = 4;
    public static final int ANIM_THUMBNAIL_SCALE_UP = 3;
    public static final String EXTRA_USAGE_TIME_REPORT = "android.activity.usage_time";
    public static final String EXTRA_USAGE_TIME_REPORT_PACKAGES = "android.usage_time_packages";
    private static final String KEY_ANIMATION_FINISHED_LISTENER = "android:activity.animationFinishedListener";
    public static final String KEY_ANIM_ENTER_RES_ID = "android:activity.animEnterRes";
    public static final String KEY_ANIM_EXIT_RES_ID = "android:activity.animExitRes";
    public static final String KEY_ANIM_HEIGHT = "android:activity.animHeight";
    public static final String KEY_ANIM_IN_PLACE_RES_ID = "android:activity.animInPlaceRes";
    private static final String KEY_ANIM_SPECS = "android:activity.animSpecs";
    public static final String KEY_ANIM_START_LISTENER = "android:activity.animStartListener";
    public static final String KEY_ANIM_START_X = "android:activity.animStartX";
    public static final String KEY_ANIM_START_Y = "android:activity.animStartY";
    public static final String KEY_ANIM_THUMBNAIL = "android:activity.animThumbnail";
    public static final String KEY_ANIM_TYPE = "android:activity.animType";
    public static final String KEY_ANIM_WIDTH = "android:activity.animWidth";
    private static final String KEY_AVOID_MOVE_TO_FRONT = "android.activity.avoidMoveToFront";
    private static final String KEY_DISALLOW_ENTER_PICTURE_IN_PICTURE_WHILE_LAUNCHING = "android:activity.disallowEnterPictureInPictureWhileLaunching";
    private static final String KEY_EXIT_COORDINATOR_INDEX = "android:activity.exitCoordinatorIndex";
    private static final String KEY_INSTANT_APP_VERIFICATION_BUNDLE = "android:instantapps.installerbundle";
    private static final String KEY_LAUNCH_ACTIVITY_TYPE = "android.activity.activityType";
    public static final String KEY_LAUNCH_BOUNDS = "android:activity.launchBounds";
    private static final String KEY_LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId";
    private static final String KEY_LAUNCH_TASK_ID = "android.activity.launchTaskId";
    private static final String KEY_LAUNCH_WINDOWING_MODE = "android.activity.windowingMode";
    private static final String KEY_LOCK_TASK_MODE = "android:activity.lockTaskMode";
    public static final String KEY_PACKAGE_NAME = "android:activity.packageName";
    private static final String KEY_REMOTE_ANIMATION_ADAPTER = "android:activity.remoteAnimationAdapter";
    private static final String KEY_RESULT_CODE = "android:activity.resultCode";
    private static final String KEY_RESULT_DATA = "android:activity.resultData";
    private static final String KEY_ROTATION_ANIMATION_HINT = "android:activity.rotationAnimationHint";
    private static final String KEY_SPECS_FUTURE = "android:activity.specsFuture";
    private static final String KEY_SPLIT_SCREEN_CREATE_MODE = "android:activity.splitScreenCreateMode";
    private static final String KEY_TASK_OVERLAY = "android.activity.taskOverlay";
    private static final String KEY_TASK_OVERLAY_CAN_RESUME = "android.activity.taskOverlayCanResume";
    private static final String KEY_TRANSITION_COMPLETE_LISTENER = "android:activity.transitionCompleteListener";
    private static final String KEY_TRANSITION_IS_RETURNING = "android:activity.transitionIsReturning";
    private static final String KEY_TRANSITION_SHARED_ELEMENTS = "android:activity.sharedElementNames";
    private static final String KEY_USAGE_TIME_REPORT = "android:activity.usageTimeReport";
    private static final String TAG = "ActivityOptions";
    private AppTransitionAnimationSpec[] mAnimSpecs;
    private IRemoteCallback mAnimationFinishedListener;
    private IRemoteCallback mAnimationStartedListener;
    private int mAnimationType;
    private Bundle mAppVerificationBundle;
    private boolean mAvoidMoveToFront;
    private int mCustomEnterResId;
    private int mCustomExitResId;
    private int mCustomInPlaceResId;
    private boolean mDisallowEnterPictureInPictureWhileLaunching;
    private int mExitCoordinatorIndex;
    private int mHeight;
    private boolean mIsReturning;

    @WindowConfiguration.ActivityType
    private int mLaunchActivityType;
    private Rect mLaunchBounds;
    private int mLaunchDisplayId;
    private int mLaunchTaskId;

    @WindowConfiguration.WindowingMode
    private int mLaunchWindowingMode;
    private boolean mLockTaskMode;
    private String mPackageName;
    private RemoteAnimationAdapter mRemoteAnimationAdapter;
    private int mResultCode;
    private Intent mResultData;
    private int mRotationAnimationHint;
    private ArrayList<String> mSharedElementNames;
    private IAppTransitionAnimationSpecsFuture mSpecsFuture;
    private int mSplitScreenCreateMode;
    private int mStartX;
    private int mStartY;
    private boolean mTaskOverlay;
    private boolean mTaskOverlayCanResume;
    private Bitmap mThumbnail;
    private ResultReceiver mTransitionReceiver;
    private PendingIntent mUsageTimeReport;
    private int mWidth;

    public interface OnAnimationFinishedListener {
        void onAnimationFinished();
    }

    public interface OnAnimationStartedListener {
        void onAnimationStarted();
    }

    public static ActivityOptions makeCustomAnimation(Context context, int i, int i2) {
        return makeCustomAnimation(context, i, i2, null, null);
    }

    public static ActivityOptions makeCustomAnimation(Context context, int i, int i2, Handler handler, OnAnimationStartedListener onAnimationStartedListener) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = context.getPackageName();
        activityOptions.mAnimationType = 1;
        activityOptions.mCustomEnterResId = i;
        activityOptions.mCustomExitResId = i2;
        activityOptions.setOnAnimationStartedListener(handler, onAnimationStartedListener);
        return activityOptions;
    }

    public static ActivityOptions makeCustomInPlaceAnimation(Context context, int i) {
        if (i == 0) {
            throw new RuntimeException("You must specify a valid animation.");
        }
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = context.getPackageName();
        activityOptions.mAnimationType = 10;
        activityOptions.mCustomInPlaceResId = i;
        return activityOptions;
    }

    private void setOnAnimationStartedListener(final Handler handler, final OnAnimationStartedListener onAnimationStartedListener) {
        if (onAnimationStartedListener != null) {
            this.mAnimationStartedListener = new IRemoteCallback.Stub() {
                @Override
                public void sendResult(Bundle bundle) throws RemoteException {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onAnimationStartedListener.onAnimationStarted();
                        }
                    });
                }
            };
        }
    }

    private void setOnAnimationFinishedListener(final Handler handler, final OnAnimationFinishedListener onAnimationFinishedListener) {
        if (onAnimationFinishedListener != null) {
            this.mAnimationFinishedListener = new IRemoteCallback.Stub() {
                @Override
                public void sendResult(Bundle bundle) throws RemoteException {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onAnimationFinishedListener.onAnimationFinished();
                        }
                    });
                }
            };
        }
    }

    public static ActivityOptions makeScaleUpAnimation(View view, int i, int i2, int i3, int i4) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = view.getContext().getPackageName();
        activityOptions.mAnimationType = 2;
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        activityOptions.mStartX = iArr[0] + i;
        activityOptions.mStartY = iArr[1] + i2;
        activityOptions.mWidth = i3;
        activityOptions.mHeight = i4;
        return activityOptions;
    }

    public static ActivityOptions makeClipRevealAnimation(View view, int i, int i2, int i3, int i4) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mAnimationType = 11;
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        activityOptions.mStartX = iArr[0] + i;
        activityOptions.mStartY = iArr[1] + i2;
        activityOptions.mWidth = i3;
        activityOptions.mHeight = i4;
        return activityOptions;
    }

    public static ActivityOptions makeOpenCrossProfileAppsAnimation() {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mAnimationType = 12;
        return activityOptions;
    }

    public static ActivityOptions makeThumbnailScaleUpAnimation(View view, Bitmap bitmap, int i, int i2) {
        return makeThumbnailScaleUpAnimation(view, bitmap, i, i2, null);
    }

    private static ActivityOptions makeThumbnailScaleUpAnimation(View view, Bitmap bitmap, int i, int i2, OnAnimationStartedListener onAnimationStartedListener) {
        return makeThumbnailAnimation(view, bitmap, i, i2, onAnimationStartedListener, true);
    }

    private static ActivityOptions makeThumbnailAnimation(View view, Bitmap bitmap, int i, int i2, OnAnimationStartedListener onAnimationStartedListener, boolean z) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = view.getContext().getPackageName();
        activityOptions.mAnimationType = z ? 3 : 4;
        activityOptions.mThumbnail = bitmap;
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        activityOptions.mStartX = iArr[0] + i;
        activityOptions.mStartY = iArr[1] + i2;
        activityOptions.setOnAnimationStartedListener(view.getHandler(), onAnimationStartedListener);
        return activityOptions;
    }

    public static ActivityOptions makeMultiThumbFutureAspectScaleAnimation(Context context, Handler handler, IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, OnAnimationStartedListener onAnimationStartedListener, boolean z) {
        int i;
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = context.getPackageName();
        if (z) {
            i = 8;
        } else {
            i = 9;
        }
        activityOptions.mAnimationType = i;
        activityOptions.mSpecsFuture = iAppTransitionAnimationSpecsFuture;
        activityOptions.setOnAnimationStartedListener(handler, onAnimationStartedListener);
        return activityOptions;
    }

    public static ActivityOptions makeThumbnailAspectScaleDownAnimation(View view, Bitmap bitmap, int i, int i2, int i3, int i4, Handler handler, OnAnimationStartedListener onAnimationStartedListener) {
        return makeAspectScaledThumbnailAnimation(view, bitmap, i, i2, i3, i4, handler, onAnimationStartedListener, false);
    }

    private static ActivityOptions makeAspectScaledThumbnailAnimation(View view, Bitmap bitmap, int i, int i2, int i3, int i4, Handler handler, OnAnimationStartedListener onAnimationStartedListener, boolean z) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = view.getContext().getPackageName();
        activityOptions.mAnimationType = z ? 8 : 9;
        activityOptions.mThumbnail = bitmap;
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        activityOptions.mStartX = iArr[0] + i;
        activityOptions.mStartY = iArr[1] + i2;
        activityOptions.mWidth = i3;
        activityOptions.mHeight = i4;
        activityOptions.setOnAnimationStartedListener(handler, onAnimationStartedListener);
        return activityOptions;
    }

    public static ActivityOptions makeThumbnailAspectScaleDownAnimation(View view, AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, Handler handler, OnAnimationStartedListener onAnimationStartedListener, OnAnimationFinishedListener onAnimationFinishedListener) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mPackageName = view.getContext().getPackageName();
        activityOptions.mAnimationType = 9;
        activityOptions.mAnimSpecs = appTransitionAnimationSpecArr;
        activityOptions.setOnAnimationStartedListener(handler, onAnimationStartedListener);
        activityOptions.setOnAnimationFinishedListener(handler, onAnimationFinishedListener);
        return activityOptions;
    }

    public static ActivityOptions makeSceneTransitionAnimation(Activity activity, View view, String str) {
        return makeSceneTransitionAnimation(activity, Pair.create(view, str));
    }

    @SafeVarargs
    public static ActivityOptions makeSceneTransitionAnimation(Activity activity, Pair<View, String>... pairArr) {
        ActivityOptions activityOptions = new ActivityOptions();
        makeSceneTransitionAnimation(activity, activity.getWindow(), activityOptions, activity.mExitTransitionListener, pairArr);
        return activityOptions;
    }

    @SafeVarargs
    public static ActivityOptions startSharedElementAnimation(Window window, Pair<View, String>... pairArr) {
        ExitTransitionCoordinator exitTransitionCoordinatorMakeSceneTransitionAnimation;
        ActivityOptions activityOptions = new ActivityOptions();
        if (window.getDecorView() != null && (exitTransitionCoordinatorMakeSceneTransitionAnimation = makeSceneTransitionAnimation((Activity) null, window, activityOptions, (SharedElementCallback) null, pairArr)) != null) {
            exitTransitionCoordinatorMakeSceneTransitionAnimation.setHideSharedElementsCallback(new HideWindowListener(window, exitTransitionCoordinatorMakeSceneTransitionAnimation));
            exitTransitionCoordinatorMakeSceneTransitionAnimation.startExit();
        }
        return activityOptions;
    }

    public static void stopSharedElementAnimation(Window window) {
        ExitTransitionCoordinator exitTransitionCoordinator;
        View decorView = window.getDecorView();
        if (decorView != null && (exitTransitionCoordinator = (ExitTransitionCoordinator) decorView.getTag(R.id.cross_task_transition)) != null) {
            exitTransitionCoordinator.cancelPendingTransitions();
            decorView.setTagInternal(R.id.cross_task_transition, null);
            TransitionManager.endTransitions((ViewGroup) decorView);
            exitTransitionCoordinator.resetViews();
            exitTransitionCoordinator.clearState();
            decorView.setVisibility(0);
        }
    }

    static ExitTransitionCoordinator makeSceneTransitionAnimation(Activity activity, Window window, ActivityOptions activityOptions, SharedElementCallback sharedElementCallback, Pair<View, String>[] pairArr) {
        if (!window.hasFeature(13)) {
            activityOptions.mAnimationType = 6;
            return null;
        }
        activityOptions.mAnimationType = 5;
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList arrayList2 = new ArrayList();
        if (pairArr != null) {
            for (Pair<View, String> pair : pairArr) {
                String str = pair.second;
                if (str == null) {
                    throw new IllegalArgumentException("Shared element name must not be null");
                }
                arrayList.add(str);
                if (pair.first == null) {
                    throw new IllegalArgumentException("Shared element must not be null");
                }
                arrayList2.add(pair.first);
            }
        }
        ExitTransitionCoordinator exitTransitionCoordinator = new ExitTransitionCoordinator(activity, window, sharedElementCallback, arrayList, arrayList, arrayList2, false);
        activityOptions.mTransitionReceiver = exitTransitionCoordinator;
        activityOptions.mSharedElementNames = arrayList;
        activityOptions.mIsReturning = activity == null;
        if (activity == null) {
            activityOptions.mExitCoordinatorIndex = -1;
        } else {
            activityOptions.mExitCoordinatorIndex = activity.mActivityTransitionState.addExitTransitionCoordinator(exitTransitionCoordinator);
        }
        return exitTransitionCoordinator;
    }

    static ActivityOptions makeSceneTransitionAnimation(Activity activity, ExitTransitionCoordinator exitTransitionCoordinator, ArrayList<String> arrayList, int i, Intent intent) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mAnimationType = 5;
        activityOptions.mSharedElementNames = arrayList;
        activityOptions.mTransitionReceiver = exitTransitionCoordinator;
        activityOptions.mIsReturning = true;
        activityOptions.mResultCode = i;
        activityOptions.mResultData = intent;
        activityOptions.mExitCoordinatorIndex = activity.mActivityTransitionState.addExitTransitionCoordinator(exitTransitionCoordinator);
        return activityOptions;
    }

    public static ActivityOptions makeTaskLaunchBehind() {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mAnimationType = 7;
        return activityOptions;
    }

    public static ActivityOptions makeBasic() {
        return new ActivityOptions();
    }

    public static ActivityOptions makeRemoteAnimation(RemoteAnimationAdapter remoteAnimationAdapter) {
        ActivityOptions activityOptions = new ActivityOptions();
        activityOptions.mRemoteAnimationAdapter = remoteAnimationAdapter;
        activityOptions.mAnimationType = 13;
        return activityOptions;
    }

    public boolean getLaunchTaskBehind() {
        return this.mAnimationType == 7;
    }

    private ActivityOptions() {
        this.mAnimationType = 0;
        this.mLockTaskMode = false;
        this.mLaunchDisplayId = -1;
        this.mLaunchWindowingMode = 0;
        this.mLaunchActivityType = 0;
        this.mLaunchTaskId = -1;
        this.mSplitScreenCreateMode = 0;
        this.mRotationAnimationHint = -1;
    }

    public ActivityOptions(Bundle bundle) {
        this.mAnimationType = 0;
        this.mLockTaskMode = false;
        this.mLaunchDisplayId = -1;
        this.mLaunchWindowingMode = 0;
        this.mLaunchActivityType = 0;
        this.mLaunchTaskId = -1;
        this.mSplitScreenCreateMode = 0;
        this.mRotationAnimationHint = -1;
        bundle.setDefusable(true);
        this.mPackageName = bundle.getString(KEY_PACKAGE_NAME);
        try {
            this.mUsageTimeReport = (PendingIntent) bundle.getParcelable(KEY_USAGE_TIME_REPORT);
        } catch (RuntimeException e) {
            Slog.w(TAG, e);
        }
        this.mLaunchBounds = (Rect) bundle.getParcelable(KEY_LAUNCH_BOUNDS);
        this.mAnimationType = bundle.getInt(KEY_ANIM_TYPE);
        switch (this.mAnimationType) {
            case 1:
                this.mCustomEnterResId = bundle.getInt(KEY_ANIM_ENTER_RES_ID, 0);
                this.mCustomExitResId = bundle.getInt(KEY_ANIM_EXIT_RES_ID, 0);
                this.mAnimationStartedListener = IRemoteCallback.Stub.asInterface(bundle.getBinder(KEY_ANIM_START_LISTENER));
                break;
            case 2:
            case 11:
                this.mStartX = bundle.getInt(KEY_ANIM_START_X, 0);
                this.mStartY = bundle.getInt(KEY_ANIM_START_Y, 0);
                this.mWidth = bundle.getInt(KEY_ANIM_WIDTH, 0);
                this.mHeight = bundle.getInt(KEY_ANIM_HEIGHT, 0);
                break;
            case 3:
            case 4:
            case 8:
            case 9:
                GraphicBuffer graphicBuffer = (GraphicBuffer) bundle.getParcelable(KEY_ANIM_THUMBNAIL);
                if (graphicBuffer != null) {
                    this.mThumbnail = Bitmap.createHardwareBitmap(graphicBuffer);
                }
                this.mStartX = bundle.getInt(KEY_ANIM_START_X, 0);
                this.mStartY = bundle.getInt(KEY_ANIM_START_Y, 0);
                this.mWidth = bundle.getInt(KEY_ANIM_WIDTH, 0);
                this.mHeight = bundle.getInt(KEY_ANIM_HEIGHT, 0);
                this.mAnimationStartedListener = IRemoteCallback.Stub.asInterface(bundle.getBinder(KEY_ANIM_START_LISTENER));
                break;
            case 5:
                this.mTransitionReceiver = (ResultReceiver) bundle.getParcelable(KEY_TRANSITION_COMPLETE_LISTENER);
                this.mIsReturning = bundle.getBoolean(KEY_TRANSITION_IS_RETURNING, false);
                this.mSharedElementNames = bundle.getStringArrayList(KEY_TRANSITION_SHARED_ELEMENTS);
                this.mResultData = (Intent) bundle.getParcelable(KEY_RESULT_DATA);
                this.mResultCode = bundle.getInt(KEY_RESULT_CODE);
                this.mExitCoordinatorIndex = bundle.getInt(KEY_EXIT_COORDINATOR_INDEX);
                break;
            case 10:
                this.mCustomInPlaceResId = bundle.getInt(KEY_ANIM_IN_PLACE_RES_ID, 0);
                break;
        }
        this.mLockTaskMode = bundle.getBoolean(KEY_LOCK_TASK_MODE, false);
        this.mLaunchDisplayId = bundle.getInt(KEY_LAUNCH_DISPLAY_ID, -1);
        this.mLaunchWindowingMode = bundle.getInt(KEY_LAUNCH_WINDOWING_MODE, 0);
        this.mLaunchActivityType = bundle.getInt(KEY_LAUNCH_ACTIVITY_TYPE, 0);
        this.mLaunchTaskId = bundle.getInt(KEY_LAUNCH_TASK_ID, -1);
        this.mTaskOverlay = bundle.getBoolean(KEY_TASK_OVERLAY, false);
        this.mTaskOverlayCanResume = bundle.getBoolean(KEY_TASK_OVERLAY_CAN_RESUME, false);
        this.mAvoidMoveToFront = bundle.getBoolean(KEY_AVOID_MOVE_TO_FRONT, false);
        this.mSplitScreenCreateMode = bundle.getInt(KEY_SPLIT_SCREEN_CREATE_MODE, 0);
        this.mDisallowEnterPictureInPictureWhileLaunching = bundle.getBoolean(KEY_DISALLOW_ENTER_PICTURE_IN_PICTURE_WHILE_LAUNCHING, false);
        if (bundle.containsKey(KEY_ANIM_SPECS)) {
            Parcelable[] parcelableArray = bundle.getParcelableArray(KEY_ANIM_SPECS);
            this.mAnimSpecs = new AppTransitionAnimationSpec[parcelableArray.length];
            for (int length = parcelableArray.length - 1; length >= 0; length--) {
                this.mAnimSpecs[length] = (AppTransitionAnimationSpec) parcelableArray[length];
            }
        }
        if (bundle.containsKey(KEY_ANIMATION_FINISHED_LISTENER)) {
            this.mAnimationFinishedListener = IRemoteCallback.Stub.asInterface(bundle.getBinder(KEY_ANIMATION_FINISHED_LISTENER));
        }
        this.mRotationAnimationHint = bundle.getInt(KEY_ROTATION_ANIMATION_HINT);
        this.mAppVerificationBundle = bundle.getBundle(KEY_INSTANT_APP_VERIFICATION_BUNDLE);
        if (bundle.containsKey(KEY_SPECS_FUTURE)) {
            this.mSpecsFuture = IAppTransitionAnimationSpecsFuture.Stub.asInterface(bundle.getBinder(KEY_SPECS_FUTURE));
        }
        this.mRemoteAnimationAdapter = (RemoteAnimationAdapter) bundle.getParcelable(KEY_REMOTE_ANIMATION_ADAPTER);
    }

    public ActivityOptions setLaunchBounds(Rect rect) {
        this.mLaunchBounds = rect != null ? new Rect(rect) : null;
        return this;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public Rect getLaunchBounds() {
        return this.mLaunchBounds;
    }

    public int getAnimationType() {
        return this.mAnimationType;
    }

    public int getCustomEnterResId() {
        return this.mCustomEnterResId;
    }

    public int getCustomExitResId() {
        return this.mCustomExitResId;
    }

    public int getCustomInPlaceResId() {
        return this.mCustomInPlaceResId;
    }

    public GraphicBuffer getThumbnail() {
        if (this.mThumbnail != null) {
            return this.mThumbnail.createGraphicBufferHandle();
        }
        return null;
    }

    public int getStartX() {
        return this.mStartX;
    }

    public int getStartY() {
        return this.mStartY;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public IRemoteCallback getOnAnimationStartListener() {
        return this.mAnimationStartedListener;
    }

    public IRemoteCallback getAnimationFinishedListener() {
        return this.mAnimationFinishedListener;
    }

    public int getExitCoordinatorKey() {
        return this.mExitCoordinatorIndex;
    }

    public void abort() {
        if (this.mAnimationStartedListener != null) {
            try {
                this.mAnimationStartedListener.sendResult(null);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isReturning() {
        return this.mIsReturning;
    }

    boolean isCrossTask() {
        return this.mExitCoordinatorIndex < 0;
    }

    public ArrayList<String> getSharedElementNames() {
        return this.mSharedElementNames;
    }

    public ResultReceiver getResultReceiver() {
        return this.mTransitionReceiver;
    }

    public int getResultCode() {
        return this.mResultCode;
    }

    public Intent getResultData() {
        return this.mResultData;
    }

    public PendingIntent getUsageTimeReport() {
        return this.mUsageTimeReport;
    }

    public AppTransitionAnimationSpec[] getAnimSpecs() {
        return this.mAnimSpecs;
    }

    public IAppTransitionAnimationSpecsFuture getSpecsFuture() {
        return this.mSpecsFuture;
    }

    public RemoteAnimationAdapter getRemoteAnimationAdapter() {
        return this.mRemoteAnimationAdapter;
    }

    public void setRemoteAnimationAdapter(RemoteAnimationAdapter remoteAnimationAdapter) {
        this.mRemoteAnimationAdapter = remoteAnimationAdapter;
    }

    public static ActivityOptions fromBundle(Bundle bundle) {
        if (bundle != null) {
            return new ActivityOptions(bundle);
        }
        return null;
    }

    public static void abort(ActivityOptions activityOptions) {
        if (activityOptions != null) {
            activityOptions.abort();
        }
    }

    public boolean getLockTaskMode() {
        return this.mLockTaskMode;
    }

    public ActivityOptions setLockTaskEnabled(boolean z) {
        this.mLockTaskMode = z;
        return this;
    }

    public int getLaunchDisplayId() {
        return this.mLaunchDisplayId;
    }

    public ActivityOptions setLaunchDisplayId(int i) {
        this.mLaunchDisplayId = i;
        return this;
    }

    public int getLaunchWindowingMode() {
        return this.mLaunchWindowingMode;
    }

    public void setLaunchWindowingMode(int i) {
        this.mLaunchWindowingMode = i;
    }

    public int getLaunchActivityType() {
        return this.mLaunchActivityType;
    }

    public void setLaunchActivityType(int i) {
        this.mLaunchActivityType = i;
    }

    public void setLaunchTaskId(int i) {
        this.mLaunchTaskId = i;
    }

    public int getLaunchTaskId() {
        return this.mLaunchTaskId;
    }

    public void setTaskOverlay(boolean z, boolean z2) {
        this.mTaskOverlay = z;
        this.mTaskOverlayCanResume = z2;
    }

    public boolean getTaskOverlay() {
        return this.mTaskOverlay;
    }

    public boolean canTaskOverlayResume() {
        return this.mTaskOverlayCanResume;
    }

    public void setAvoidMoveToFront() {
        this.mAvoidMoveToFront = true;
    }

    public boolean getAvoidMoveToFront() {
        return this.mAvoidMoveToFront;
    }

    public int getSplitScreenCreateMode() {
        return this.mSplitScreenCreateMode;
    }

    public void setSplitScreenCreateMode(int i) {
        this.mSplitScreenCreateMode = i;
    }

    public void setDisallowEnterPictureInPictureWhileLaunching(boolean z) {
        this.mDisallowEnterPictureInPictureWhileLaunching = z;
    }

    public boolean disallowEnterPictureInPictureWhileLaunching() {
        return this.mDisallowEnterPictureInPictureWhileLaunching;
    }

    public void update(ActivityOptions activityOptions) {
        if (activityOptions.mPackageName != null) {
            this.mPackageName = activityOptions.mPackageName;
        }
        this.mUsageTimeReport = activityOptions.mUsageTimeReport;
        this.mTransitionReceiver = null;
        this.mSharedElementNames = null;
        this.mIsReturning = false;
        this.mResultData = null;
        this.mResultCode = 0;
        this.mExitCoordinatorIndex = 0;
        this.mAnimationType = activityOptions.mAnimationType;
        switch (activityOptions.mAnimationType) {
            case 1:
                this.mCustomEnterResId = activityOptions.mCustomEnterResId;
                this.mCustomExitResId = activityOptions.mCustomExitResId;
                this.mThumbnail = null;
                if (this.mAnimationStartedListener != null) {
                    try {
                        this.mAnimationStartedListener.sendResult(null);
                        break;
                    } catch (RemoteException e) {
                    }
                }
                this.mAnimationStartedListener = activityOptions.mAnimationStartedListener;
                break;
            case 2:
                this.mStartX = activityOptions.mStartX;
                this.mStartY = activityOptions.mStartY;
                this.mWidth = activityOptions.mWidth;
                this.mHeight = activityOptions.mHeight;
                if (this.mAnimationStartedListener != null) {
                    try {
                        this.mAnimationStartedListener.sendResult(null);
                        break;
                    } catch (RemoteException e2) {
                    }
                }
                this.mAnimationStartedListener = null;
                break;
            case 3:
            case 4:
            case 8:
            case 9:
                this.mThumbnail = activityOptions.mThumbnail;
                this.mStartX = activityOptions.mStartX;
                this.mStartY = activityOptions.mStartY;
                this.mWidth = activityOptions.mWidth;
                this.mHeight = activityOptions.mHeight;
                if (this.mAnimationStartedListener != null) {
                    try {
                        this.mAnimationStartedListener.sendResult(null);
                        break;
                    } catch (RemoteException e3) {
                    }
                }
                this.mAnimationStartedListener = activityOptions.mAnimationStartedListener;
                break;
            case 5:
                this.mTransitionReceiver = activityOptions.mTransitionReceiver;
                this.mSharedElementNames = activityOptions.mSharedElementNames;
                this.mIsReturning = activityOptions.mIsReturning;
                this.mThumbnail = null;
                this.mAnimationStartedListener = null;
                this.mResultData = activityOptions.mResultData;
                this.mResultCode = activityOptions.mResultCode;
                this.mExitCoordinatorIndex = activityOptions.mExitCoordinatorIndex;
                break;
            case 10:
                this.mCustomInPlaceResId = activityOptions.mCustomInPlaceResId;
                break;
        }
        this.mLockTaskMode = activityOptions.mLockTaskMode;
        this.mAnimSpecs = activityOptions.mAnimSpecs;
        this.mAnimationFinishedListener = activityOptions.mAnimationFinishedListener;
        this.mSpecsFuture = activityOptions.mSpecsFuture;
        this.mRemoteAnimationAdapter = activityOptions.mRemoteAnimationAdapter;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (this.mPackageName != null) {
            bundle.putString(KEY_PACKAGE_NAME, this.mPackageName);
        }
        if (this.mLaunchBounds != null) {
            bundle.putParcelable(KEY_LAUNCH_BOUNDS, this.mLaunchBounds);
        }
        bundle.putInt(KEY_ANIM_TYPE, this.mAnimationType);
        if (this.mUsageTimeReport != null) {
            bundle.putParcelable(KEY_USAGE_TIME_REPORT, this.mUsageTimeReport);
        }
        switch (this.mAnimationType) {
            case 1:
                bundle.putInt(KEY_ANIM_ENTER_RES_ID, this.mCustomEnterResId);
                bundle.putInt(KEY_ANIM_EXIT_RES_ID, this.mCustomExitResId);
                bundle.putBinder(KEY_ANIM_START_LISTENER, this.mAnimationStartedListener != null ? this.mAnimationStartedListener.asBinder() : null);
                break;
            case 2:
            case 11:
                bundle.putInt(KEY_ANIM_START_X, this.mStartX);
                bundle.putInt(KEY_ANIM_START_Y, this.mStartY);
                bundle.putInt(KEY_ANIM_WIDTH, this.mWidth);
                bundle.putInt(KEY_ANIM_HEIGHT, this.mHeight);
                break;
            case 3:
            case 4:
            case 8:
            case 9:
                if (this.mThumbnail != null) {
                    Bitmap bitmapCopy = this.mThumbnail.copy(Bitmap.Config.HARDWARE, false);
                    if (bitmapCopy != null) {
                        bundle.putParcelable(KEY_ANIM_THUMBNAIL, bitmapCopy.createGraphicBufferHandle());
                    } else {
                        Slog.w(TAG, "Failed to copy thumbnail");
                    }
                }
                bundle.putInt(KEY_ANIM_START_X, this.mStartX);
                bundle.putInt(KEY_ANIM_START_Y, this.mStartY);
                bundle.putInt(KEY_ANIM_WIDTH, this.mWidth);
                bundle.putInt(KEY_ANIM_HEIGHT, this.mHeight);
                bundle.putBinder(KEY_ANIM_START_LISTENER, this.mAnimationStartedListener != null ? this.mAnimationStartedListener.asBinder() : null);
                break;
            case 5:
                if (this.mTransitionReceiver != null) {
                    bundle.putParcelable(KEY_TRANSITION_COMPLETE_LISTENER, this.mTransitionReceiver);
                }
                bundle.putBoolean(KEY_TRANSITION_IS_RETURNING, this.mIsReturning);
                bundle.putStringArrayList(KEY_TRANSITION_SHARED_ELEMENTS, this.mSharedElementNames);
                bundle.putParcelable(KEY_RESULT_DATA, this.mResultData);
                bundle.putInt(KEY_RESULT_CODE, this.mResultCode);
                bundle.putInt(KEY_EXIT_COORDINATOR_INDEX, this.mExitCoordinatorIndex);
                break;
            case 10:
                bundle.putInt(KEY_ANIM_IN_PLACE_RES_ID, this.mCustomInPlaceResId);
                break;
        }
        bundle.putBoolean(KEY_LOCK_TASK_MODE, this.mLockTaskMode);
        bundle.putInt(KEY_LAUNCH_DISPLAY_ID, this.mLaunchDisplayId);
        bundle.putInt(KEY_LAUNCH_WINDOWING_MODE, this.mLaunchWindowingMode);
        bundle.putInt(KEY_LAUNCH_ACTIVITY_TYPE, this.mLaunchActivityType);
        bundle.putInt(KEY_LAUNCH_TASK_ID, this.mLaunchTaskId);
        bundle.putBoolean(KEY_TASK_OVERLAY, this.mTaskOverlay);
        bundle.putBoolean(KEY_TASK_OVERLAY_CAN_RESUME, this.mTaskOverlayCanResume);
        bundle.putBoolean(KEY_AVOID_MOVE_TO_FRONT, this.mAvoidMoveToFront);
        bundle.putInt(KEY_SPLIT_SCREEN_CREATE_MODE, this.mSplitScreenCreateMode);
        bundle.putBoolean(KEY_DISALLOW_ENTER_PICTURE_IN_PICTURE_WHILE_LAUNCHING, this.mDisallowEnterPictureInPictureWhileLaunching);
        if (this.mAnimSpecs != null) {
            bundle.putParcelableArray(KEY_ANIM_SPECS, this.mAnimSpecs);
        }
        if (this.mAnimationFinishedListener != null) {
            bundle.putBinder(KEY_ANIMATION_FINISHED_LISTENER, this.mAnimationFinishedListener.asBinder());
        }
        if (this.mSpecsFuture != null) {
            bundle.putBinder(KEY_SPECS_FUTURE, this.mSpecsFuture.asBinder());
        }
        bundle.putInt(KEY_ROTATION_ANIMATION_HINT, this.mRotationAnimationHint);
        if (this.mAppVerificationBundle != null) {
            bundle.putBundle(KEY_INSTANT_APP_VERIFICATION_BUNDLE, this.mAppVerificationBundle);
        }
        if (this.mRemoteAnimationAdapter != null) {
            bundle.putParcelable(KEY_REMOTE_ANIMATION_ADAPTER, this.mRemoteAnimationAdapter);
        }
        return bundle;
    }

    public void requestUsageTimeReport(PendingIntent pendingIntent) {
        this.mUsageTimeReport = pendingIntent;
    }

    public ActivityOptions forTargetActivity() {
        if (this.mAnimationType == 5) {
            ActivityOptions activityOptions = new ActivityOptions();
            activityOptions.update(this);
            return activityOptions;
        }
        return null;
    }

    public int getRotationAnimationHint() {
        return this.mRotationAnimationHint;
    }

    public void setRotationAnimationHint(int i) {
        this.mRotationAnimationHint = i;
    }

    public Bundle popAppVerificationBundle() {
        Bundle bundle = this.mAppVerificationBundle;
        this.mAppVerificationBundle = null;
        return bundle;
    }

    public ActivityOptions setAppVerificationBundle(Bundle bundle) {
        this.mAppVerificationBundle = bundle;
        return this;
    }

    public String toString() {
        return "ActivityOptions(" + hashCode() + "), mPackageName=" + this.mPackageName + ", mAnimationType=" + this.mAnimationType + ", mStartX=" + this.mStartX + ", mStartY=" + this.mStartY + ", mWidth=" + this.mWidth + ", mHeight=" + this.mHeight;
    }

    private static class HideWindowListener extends TransitionListenerAdapter implements ExitTransitionCoordinator.HideSharedElementsCallback {
        private final ExitTransitionCoordinator mExit;
        private boolean mSharedElementHidden;
        private ArrayList<View> mSharedElements;
        private boolean mTransitionEnded;
        private final boolean mWaitingForTransition;
        private final Window mWindow;

        public HideWindowListener(Window window, ExitTransitionCoordinator exitTransitionCoordinator) {
            this.mWindow = window;
            this.mExit = exitTransitionCoordinator;
            this.mSharedElements = new ArrayList<>(exitTransitionCoordinator.mSharedElements);
            Transition exitTransition = this.mWindow.getExitTransition();
            if (exitTransition != null) {
                exitTransition.addListener(this);
                this.mWaitingForTransition = true;
            } else {
                this.mWaitingForTransition = false;
            }
            View decorView = this.mWindow.getDecorView();
            if (decorView != null) {
                if (decorView.getTag(R.id.cross_task_transition) != null) {
                    throw new IllegalStateException("Cannot start a transition while one is running");
                }
                decorView.setTagInternal(R.id.cross_task_transition, exitTransitionCoordinator);
            }
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            this.mTransitionEnded = true;
            hideWhenDone();
            transition.removeListener(this);
        }

        @Override
        public void hideSharedElements() {
            this.mSharedElementHidden = true;
            hideWhenDone();
        }

        private void hideWhenDone() {
            if (this.mSharedElementHidden) {
                if (!this.mWaitingForTransition || this.mTransitionEnded) {
                    this.mExit.resetViews();
                    int size = this.mSharedElements.size();
                    for (int i = 0; i < size; i++) {
                        this.mSharedElements.get(i).requestLayout();
                    }
                    View decorView = this.mWindow.getDecorView();
                    if (decorView != null) {
                        decorView.setTagInternal(R.id.cross_task_transition, null);
                        decorView.setVisibility(8);
                    }
                }
            }
        }
    }
}
