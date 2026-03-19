package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.transition.Transition;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.android.internal.view.OneShotPreDrawListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class ActivityTransitionState {
    private static final String ENTERING_SHARED_ELEMENTS = "android:enteringSharedElements";
    private static final String EXITING_MAPPED_FROM = "android:exitingMappedFrom";
    private static final String EXITING_MAPPED_TO = "android:exitingMappedTo";
    private ExitTransitionCoordinator mCalledExitCoordinator;
    private ActivityOptions mEnterActivityOptions;
    private EnterTransitionCoordinator mEnterTransitionCoordinator;
    private ArrayList<String> mEnteringNames;
    private SparseArray<WeakReference<ExitTransitionCoordinator>> mExitTransitionCoordinators;
    private int mExitTransitionCoordinatorsKey = 1;
    private ArrayList<String> mExitingFrom;
    private ArrayList<String> mExitingTo;
    private ArrayList<View> mExitingToView;
    private boolean mHasExited;
    private boolean mIsEnterPostponed;
    private boolean mIsEnterTriggered;
    private ExitTransitionCoordinator mReturnExitCoordinator;

    public int addExitTransitionCoordinator(ExitTransitionCoordinator exitTransitionCoordinator) {
        if (this.mExitTransitionCoordinators == null) {
            this.mExitTransitionCoordinators = new SparseArray<>();
        }
        WeakReference<ExitTransitionCoordinator> weakReference = new WeakReference<>(exitTransitionCoordinator);
        for (int size = this.mExitTransitionCoordinators.size() - 1; size >= 0; size--) {
            if (this.mExitTransitionCoordinators.valueAt(size).get() == null) {
                this.mExitTransitionCoordinators.removeAt(size);
            }
        }
        int i = this.mExitTransitionCoordinatorsKey;
        this.mExitTransitionCoordinatorsKey = i + 1;
        this.mExitTransitionCoordinators.append(i, weakReference);
        return i;
    }

    public void readState(Bundle bundle) {
        if (bundle != null) {
            if (this.mEnterTransitionCoordinator == null || this.mEnterTransitionCoordinator.isReturning()) {
                this.mEnteringNames = bundle.getStringArrayList(ENTERING_SHARED_ELEMENTS);
            }
            if (this.mEnterTransitionCoordinator == null) {
                this.mExitingFrom = bundle.getStringArrayList(EXITING_MAPPED_FROM);
                this.mExitingTo = bundle.getStringArrayList(EXITING_MAPPED_TO);
            }
        }
    }

    public void saveState(Bundle bundle) {
        if (this.mEnteringNames != null) {
            bundle.putStringArrayList(ENTERING_SHARED_ELEMENTS, this.mEnteringNames);
        }
        if (this.mExitingFrom != null) {
            bundle.putStringArrayList(EXITING_MAPPED_FROM, this.mExitingFrom);
            bundle.putStringArrayList(EXITING_MAPPED_TO, this.mExitingTo);
        }
    }

    public void setEnterActivityOptions(Activity activity, ActivityOptions activityOptions) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        window.getDecorView();
        if (window.hasFeature(13) && activityOptions != null && this.mEnterActivityOptions == null && this.mEnterTransitionCoordinator == null && activityOptions.getAnimationType() == 5) {
            this.mEnterActivityOptions = activityOptions;
            this.mIsEnterTriggered = false;
            if (this.mEnterActivityOptions.isReturning()) {
                restoreExitedViews();
                int resultCode = this.mEnterActivityOptions.getResultCode();
                if (resultCode != 0) {
                    Intent resultData = this.mEnterActivityOptions.getResultData();
                    if (resultData != null) {
                        resultData.setExtrasClassLoader(activity.getClassLoader());
                    }
                    activity.onActivityReenter(resultCode, resultData);
                }
            }
        }
    }

    public void enterReady(Activity activity) {
        if (this.mEnterActivityOptions == null || this.mIsEnterTriggered) {
            return;
        }
        this.mIsEnterTriggered = true;
        this.mHasExited = false;
        ArrayList<String> sharedElementNames = this.mEnterActivityOptions.getSharedElementNames();
        ResultReceiver resultReceiver = this.mEnterActivityOptions.getResultReceiver();
        if (this.mEnterActivityOptions.isReturning()) {
            restoreExitedViews();
            activity.getWindow().getDecorView().setVisibility(0);
        }
        this.mEnterTransitionCoordinator = new EnterTransitionCoordinator(activity, resultReceiver, sharedElementNames, this.mEnterActivityOptions.isReturning(), this.mEnterActivityOptions.isCrossTask());
        if (this.mEnterActivityOptions.isCrossTask()) {
            this.mExitingFrom = new ArrayList<>(this.mEnterActivityOptions.getSharedElementNames());
            this.mExitingTo = new ArrayList<>(this.mEnterActivityOptions.getSharedElementNames());
        }
        if (!this.mIsEnterPostponed) {
            startEnter();
        }
    }

    public void postponeEnterTransition() {
        this.mIsEnterPostponed = true;
    }

    public void startPostponedEnterTransition() {
        if (this.mIsEnterPostponed) {
            this.mIsEnterPostponed = false;
            if (this.mEnterTransitionCoordinator != null) {
                startEnter();
            }
        }
    }

    private void startEnter() {
        if (this.mEnterTransitionCoordinator.isReturning()) {
            if (this.mExitingToView != null) {
                this.mEnterTransitionCoordinator.viewInstancesReady(this.mExitingFrom, this.mExitingTo, this.mExitingToView);
            } else {
                this.mEnterTransitionCoordinator.namedViewsReady(this.mExitingFrom, this.mExitingTo);
            }
        } else {
            this.mEnterTransitionCoordinator.namedViewsReady(null, null);
            this.mEnteringNames = this.mEnterTransitionCoordinator.getAllSharedElementNames();
        }
        this.mExitingFrom = null;
        this.mExitingTo = null;
        this.mExitingToView = null;
        this.mEnterActivityOptions = null;
    }

    public void onStop() {
        restoreExitedViews();
        if (this.mEnterTransitionCoordinator != null) {
            this.mEnterTransitionCoordinator.stop();
            this.mEnterTransitionCoordinator = null;
        }
        if (this.mReturnExitCoordinator != null) {
            this.mReturnExitCoordinator.stop();
            this.mReturnExitCoordinator = null;
        }
    }

    public void onResume(Activity activity, boolean z) {
        if (z || this.mEnterTransitionCoordinator == null) {
            restoreExitedViews();
            restoreReenteringViews();
        } else {
            activity.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityTransitionState.this.mEnterTransitionCoordinator == null || ActivityTransitionState.this.mEnterTransitionCoordinator.isWaitingForRemoteExit()) {
                        ActivityTransitionState.this.restoreExitedViews();
                        ActivityTransitionState.this.restoreReenteringViews();
                    }
                }
            }, 1000L);
        }
    }

    public void clear() {
        this.mEnteringNames = null;
        this.mExitingFrom = null;
        this.mExitingTo = null;
        this.mExitingToView = null;
        this.mCalledExitCoordinator = null;
        this.mEnterTransitionCoordinator = null;
        this.mEnterActivityOptions = null;
        this.mExitTransitionCoordinators = null;
    }

    private void restoreExitedViews() {
        if (this.mCalledExitCoordinator != null) {
            this.mCalledExitCoordinator.resetViews();
            this.mCalledExitCoordinator = null;
        }
    }

    private void restoreReenteringViews() {
        if (this.mEnterTransitionCoordinator != null && this.mEnterTransitionCoordinator.isReturning() && !this.mEnterTransitionCoordinator.isCrossTask()) {
            this.mEnterTransitionCoordinator.forceViewsToAppear();
            this.mExitingFrom = null;
            this.mExitingTo = null;
            this.mExitingToView = null;
        }
    }

    public boolean startExitBackTransition(final Activity activity) {
        boolean zCancelEnter;
        Transition enterViewsTransition;
        ViewGroup decor;
        if (this.mEnteringNames == null || this.mCalledExitCoordinator != null) {
            return false;
        }
        if (!this.mHasExited) {
            this.mHasExited = true;
            if (this.mEnterTransitionCoordinator != null) {
                enterViewsTransition = this.mEnterTransitionCoordinator.getEnterViewsTransition();
                decor = this.mEnterTransitionCoordinator.getDecor();
                zCancelEnter = this.mEnterTransitionCoordinator.cancelEnter();
                this.mEnterTransitionCoordinator = null;
                if (enterViewsTransition != null && decor != null) {
                    enterViewsTransition.pause(decor);
                }
            } else {
                zCancelEnter = false;
                enterViewsTransition = null;
                decor = null;
            }
            this.mReturnExitCoordinator = new ExitTransitionCoordinator(activity, activity.getWindow(), activity.mEnterTransitionListener, this.mEnteringNames, null, null, true);
            if (enterViewsTransition != null && decor != null) {
                enterViewsTransition.resume(decor);
            }
            if (zCancelEnter && decor != null) {
                OneShotPreDrawListener.add(decor, new Runnable() {
                    @Override
                    public final void run() {
                        ActivityTransitionState.lambda$startExitBackTransition$0(this.f$0, activity);
                    }
                });
            } else {
                this.mReturnExitCoordinator.startExit(activity.mResultCode, activity.mResultData);
            }
        }
        return true;
    }

    public static void lambda$startExitBackTransition$0(ActivityTransitionState activityTransitionState, Activity activity) {
        if (activityTransitionState.mReturnExitCoordinator != null) {
            activityTransitionState.mReturnExitCoordinator.startExit(activity.mResultCode, activity.mResultData);
        }
    }

    public boolean isTransitionRunning() {
        if (this.mEnterTransitionCoordinator != null && this.mEnterTransitionCoordinator.isTransitionRunning()) {
            return true;
        }
        if (this.mCalledExitCoordinator == null || !this.mCalledExitCoordinator.isTransitionRunning()) {
            return this.mReturnExitCoordinator != null && this.mReturnExitCoordinator.isTransitionRunning();
        }
        return true;
    }

    public void startExitOutTransition(Activity activity, Bundle bundle) {
        this.mEnterTransitionCoordinator = null;
        if (!activity.getWindow().hasFeature(13) || this.mExitTransitionCoordinators == null) {
            return;
        }
        ActivityOptions activityOptions = new ActivityOptions(bundle);
        if (activityOptions.getAnimationType() == 5) {
            int iIndexOfKey = this.mExitTransitionCoordinators.indexOfKey(activityOptions.getExitCoordinatorKey());
            if (iIndexOfKey >= 0) {
                this.mCalledExitCoordinator = this.mExitTransitionCoordinators.valueAt(iIndexOfKey).get();
                this.mExitTransitionCoordinators.removeAt(iIndexOfKey);
                if (this.mCalledExitCoordinator != null) {
                    this.mExitingFrom = this.mCalledExitCoordinator.getAcceptedNames();
                    this.mExitingTo = this.mCalledExitCoordinator.getMappedNames();
                    this.mExitingToView = this.mCalledExitCoordinator.copyMappedViews();
                    this.mCalledExitCoordinator.startExit();
                }
            }
        }
    }
}
