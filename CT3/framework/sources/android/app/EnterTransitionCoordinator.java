package android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityTransitionCoordinator;
import android.app.SharedElementCallback;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.ArrayMap;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewTreeObserver;
import android.view.Window;
import java.util.ArrayList;

class EnterTransitionCoordinator extends ActivityTransitionCoordinator {
    private static final int MIN_ANIMATION_FRAMES = 2;
    private static final String TAG = "EnterTransitionCoordinator";
    private Activity mActivity;
    private boolean mAreViewsReady;
    private ObjectAnimator mBackgroundAnimator;
    private Transition mEnterViewsTransition;
    private boolean mHasStopped;
    private boolean mIsCanceled;
    private boolean mIsExitTransitionComplete;
    private boolean mIsReadyForTransition;
    private boolean mIsViewsTransitionStarted;
    private boolean mSharedElementTransitionStarted;
    private Bundle mSharedElementsBundle;
    private ViewTreeObserver.OnPreDrawListener mViewsReadyListener;
    private boolean mWasOpaque;

    public EnterTransitionCoordinator(Activity activity, ResultReceiver resultReceiver, ArrayList<String> sharedElementNames, boolean isReturning) {
        super(activity.getWindow(), sharedElementNames, getListener(activity, isReturning), isReturning);
        this.mActivity = activity;
        setResultReceiver(resultReceiver);
        prepareEnter();
        Bundle resultReceiverBundle = new Bundle();
        resultReceiverBundle.putParcelable("android:remoteReceiver", this);
        this.mResultReceiver.send(100, resultReceiverBundle);
        final View decorView = getDecor();
        if (decorView == null) {
            return;
        }
        decorView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (EnterTransitionCoordinator.this.mIsReadyForTransition) {
                    decorView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return EnterTransitionCoordinator.this.mIsReadyForTransition;
            }
        });
    }

    public void viewInstancesReady(ArrayList<String> accepted, ArrayList<String> localNames, ArrayList<View> localViews) {
        boolean remap = false;
        for (int i = 0; i < localViews.size(); i++) {
            View view = localViews.get(i);
            if (!TextUtils.equals(view.getTransitionName(), localNames.get(i)) || !view.isAttachedToWindow()) {
                remap = true;
                break;
            }
        }
        if (remap) {
            triggerViewsReady(mapNamedElements(accepted, localNames));
        } else {
            triggerViewsReady(mapSharedElements(accepted, localViews));
        }
    }

    public void namedViewsReady(ArrayList<String> accepted, ArrayList<String> localNames) {
        triggerViewsReady(mapNamedElements(accepted, localNames));
    }

    public Transition getEnterViewsTransition() {
        return this.mEnterViewsTransition;
    }

    @Override
    protected void viewsReady(ArrayMap<String, View> sharedElements) {
        super.viewsReady(sharedElements);
        this.mIsReadyForTransition = true;
        hideViews(this.mSharedElements);
        if (getViewsTransition() != null && this.mTransitioningViews != null) {
            hideViews(this.mTransitioningViews);
        }
        if (this.mIsReturning) {
            sendSharedElementDestination();
        } else {
            moveSharedElementsToOverlay();
        }
        if (this.mSharedElementsBundle == null) {
            return;
        }
        onTakeSharedElements();
    }

    private void triggerViewsReady(final ArrayMap<String, View> sharedElements) {
        if (this.mAreViewsReady) {
            return;
        }
        this.mAreViewsReady = true;
        final ViewGroup decor = getDecor();
        if (decor == null || (decor.isAttachedToWindow() && (sharedElements.isEmpty() || !sharedElements.valueAt(0).isLayoutRequested()))) {
            viewsReady(sharedElements);
            return;
        }
        this.mViewsReadyListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                EnterTransitionCoordinator.this.mViewsReadyListener = null;
                decor.getViewTreeObserver().removeOnPreDrawListener(this);
                EnterTransitionCoordinator.this.viewsReady(sharedElements);
                return true;
            }
        };
        decor.getViewTreeObserver().addOnPreDrawListener(this.mViewsReadyListener);
        decor.invalidate();
    }

    private ArrayMap<String, View> mapNamedElements(ArrayList<String> accepted, ArrayList<String> localNames) {
        View view;
        ArrayMap<String, View> sharedElements = new ArrayMap<>();
        ViewGroup decorView = getDecor();
        if (decorView != null) {
            decorView.findNamedViews(sharedElements);
        }
        if (accepted != null) {
            for (int i = 0; i < localNames.size(); i++) {
                String localName = localNames.get(i);
                String acceptedName = accepted.get(i);
                if (localName != null && !localName.equals(acceptedName) && (view = sharedElements.remove(localName)) != null) {
                    sharedElements.put(acceptedName, view);
                }
            }
        }
        return sharedElements;
    }

    private void sendSharedElementDestination() {
        boolean allReady;
        final View decorView = getDecor();
        if (allowOverlappingTransitions() && getEnterViewsTransition() != null) {
            allReady = false;
        } else if (decorView == null) {
            allReady = true;
        } else {
            allReady = !decorView.isLayoutRequested();
            if (allReady) {
                int i = 0;
                while (true) {
                    if (i >= this.mSharedElements.size()) {
                        break;
                    }
                    if (!this.mSharedElements.get(i).isLayoutRequested()) {
                        i++;
                    } else {
                        allReady = false;
                        break;
                    }
                }
            }
        }
        if (allReady) {
            Bundle state = captureSharedElementState();
            moveSharedElementsToOverlay();
            this.mResultReceiver.send(107, state);
        } else if (decorView != null) {
            decorView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    decorView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (EnterTransitionCoordinator.this.mResultReceiver != null) {
                        Bundle state2 = EnterTransitionCoordinator.this.captureSharedElementState();
                        EnterTransitionCoordinator.this.moveSharedElementsToOverlay();
                        EnterTransitionCoordinator.this.mResultReceiver.send(107, state2);
                        return true;
                    }
                    return true;
                }
            });
        }
        if (!allowOverlappingTransitions()) {
            return;
        }
        startEnterTransitionOnly();
    }

    private static SharedElementCallback getListener(Activity activity, boolean isReturning) {
        return isReturning ? activity.mExitTransitionListener : activity.mEnterTransitionListener;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case 103:
                if (!this.mIsCanceled) {
                    this.mSharedElementsBundle = resultData;
                    onTakeSharedElements();
                }
                break;
            case 104:
                if (!this.mIsCanceled) {
                    this.mIsExitTransitionComplete = true;
                    if (this.mSharedElementTransitionStarted) {
                        onRemoteExitTransitionComplete();
                    }
                }
                break;
            case 106:
                cancel();
                break;
        }
    }

    public boolean isWaitingForRemoteExit() {
        return this.mIsReturning && this.mResultReceiver != null;
    }

    public void forceViewsToAppear() {
        if (!this.mIsReturning) {
            return;
        }
        if (!this.mIsReadyForTransition) {
            this.mIsReadyForTransition = true;
            ViewGroup decor = getDecor();
            if (decor != null && this.mViewsReadyListener != null) {
                decor.getViewTreeObserver().removeOnPreDrawListener(this.mViewsReadyListener);
                this.mViewsReadyListener = null;
            }
            showViews(this.mTransitioningViews, true);
            setTransitioningViewsVisiblity(0, true);
            this.mSharedElements.clear();
            this.mAllSharedElementNames.clear();
            this.mTransitioningViews.clear();
            this.mIsReadyForTransition = true;
            viewsTransitionComplete();
            sharedElementTransitionComplete();
        } else {
            if (!this.mSharedElementTransitionStarted) {
                moveSharedElementsFromOverlay();
                this.mSharedElementTransitionStarted = true;
                showViews(this.mSharedElements, true);
                this.mSharedElements.clear();
                sharedElementTransitionComplete();
            }
            if (!this.mIsViewsTransitionStarted) {
                this.mIsViewsTransitionStarted = true;
                showViews(this.mTransitioningViews, true);
                setTransitioningViewsVisiblity(0, true);
                this.mTransitioningViews.clear();
                viewsTransitionComplete();
            }
            cancelPendingTransitions();
        }
        this.mAreViewsReady = true;
        if (this.mResultReceiver == null) {
            return;
        }
        this.mResultReceiver.send(106, null);
        this.mResultReceiver = null;
    }

    private void cancel() {
        if (this.mIsCanceled) {
            return;
        }
        this.mIsCanceled = true;
        if (getViewsTransition() == null || this.mIsViewsTransitionStarted) {
            showViews(this.mSharedElements, true);
        } else if (this.mTransitioningViews != null) {
            this.mTransitioningViews.addAll(this.mSharedElements);
        }
        moveSharedElementsFromOverlay();
        this.mSharedElementNames.clear();
        this.mSharedElements.clear();
        this.mAllSharedElementNames.clear();
        startSharedElementTransition(null);
        onRemoteExitTransitionComplete();
    }

    public boolean isReturning() {
        return this.mIsReturning;
    }

    protected void prepareEnter() {
        ViewGroup decorView = getDecor();
        if (this.mActivity == null || decorView == null) {
            return;
        }
        this.mActivity.overridePendingTransition(0, 0);
        if (!this.mIsReturning) {
            this.mWasOpaque = this.mActivity.convertToTranslucent(null, null);
            Drawable background = decorView.getBackground();
            if (background == null) {
                return;
            }
            getWindow().setBackgroundDrawable(null);
            Drawable background2 = background.mutate();
            background2.setAlpha(0);
            getWindow().setBackgroundDrawable(background2);
            return;
        }
        this.mActivity = null;
    }

    @Override
    protected Transition getViewsTransition() {
        Window window = getWindow();
        if (window == null) {
            return null;
        }
        if (this.mIsReturning) {
            return window.getReenterTransition();
        }
        return window.getEnterTransition();
    }

    protected Transition getSharedElementTransition() {
        Window window = getWindow();
        if (window == null) {
            return null;
        }
        if (this.mIsReturning) {
            return window.getSharedElementReenterTransition();
        }
        return window.getSharedElementEnterTransition();
    }

    private void startSharedElementTransition(Bundle sharedElementState) {
        ViewGroup decorView = getDecor();
        if (decorView == null) {
            return;
        }
        ArrayList<String> rejectedNames = new ArrayList<>(this.mAllSharedElementNames);
        rejectedNames.removeAll(this.mSharedElementNames);
        ArrayList<View> rejectedSnapshots = createSnapshots(sharedElementState, rejectedNames);
        if (this.mListener != null) {
            this.mListener.onRejectSharedElements(rejectedSnapshots);
        }
        removeNullViews(rejectedSnapshots);
        startRejectedAnimations(rejectedSnapshots);
        ArrayList<View> sharedElementSnapshots = createSnapshots(sharedElementState, this.mSharedElementNames);
        showViews(this.mSharedElements, true);
        scheduleSetSharedElementEnd(sharedElementSnapshots);
        ArrayList<ActivityTransitionCoordinator.SharedElementOriginalState> originalImageViewState = setSharedElementState(sharedElementState, sharedElementSnapshots);
        requestLayoutForSharedElements();
        boolean startEnterTransition = allowOverlappingTransitions() && !this.mIsReturning;
        setGhostVisibility(4);
        scheduleGhostVisibilityChange(4);
        pauseInput();
        Transition transition = beginTransition(decorView, startEnterTransition, true);
        scheduleGhostVisibilityChange(0);
        setGhostVisibility(0);
        if (startEnterTransition) {
            startEnterTransition(transition);
        }
        setOriginalSharedElementState(this.mSharedElements, originalImageViewState);
        if (this.mResultReceiver == null) {
            return;
        }
        decorView.postOnAnimation(new Runnable() {
            int mAnimations;

            @Override
            public void run() {
                int i = this.mAnimations;
                this.mAnimations = i + 1;
                if (i < 2) {
                    View decorView2 = EnterTransitionCoordinator.this.getDecor();
                    if (decorView2 == null) {
                        return;
                    }
                    decorView2.postOnAnimation(this);
                    return;
                }
                if (EnterTransitionCoordinator.this.mResultReceiver == null) {
                    return;
                }
                EnterTransitionCoordinator.this.mResultReceiver.send(101, null);
                EnterTransitionCoordinator.this.mResultReceiver = null;
            }
        });
    }

    private static void removeNullViews(ArrayList<View> views) {
        if (views == null) {
            return;
        }
        for (int i = views.size() - 1; i >= 0; i--) {
            if (views.get(i) == null) {
                views.remove(i);
            }
        }
    }

    private void onTakeSharedElements() {
        if (!this.mIsReadyForTransition || this.mSharedElementsBundle == null) {
            return;
        }
        Bundle sharedElementState = this.mSharedElementsBundle;
        this.mSharedElementsBundle = null;
        SharedElementCallback.OnSharedElementsReadyListener listener = new AnonymousClass5(sharedElementState);
        if (this.mListener == null) {
            listener.onSharedElementsReady();
        } else {
            this.mListener.onSharedElementsArrived(this.mSharedElementNames, this.mSharedElements, listener);
        }
    }

    class AnonymousClass5 implements SharedElementCallback.OnSharedElementsReadyListener {
        final Bundle val$sharedElementState;

        AnonymousClass5(Bundle val$sharedElementState) {
            this.val$sharedElementState = val$sharedElementState;
        }

        @Override
        public void onSharedElementsReady() {
            final View decorView = EnterTransitionCoordinator.this.getDecor();
            if (decorView == null) {
                return;
            }
            ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
            final Bundle bundle = this.val$sharedElementState;
            viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    decorView.getViewTreeObserver().removeOnPreDrawListener(this);
                    EnterTransitionCoordinator enterTransitionCoordinator = EnterTransitionCoordinator.this;
                    final Bundle bundle2 = bundle;
                    enterTransitionCoordinator.startTransition(new Runnable() {
                        @Override
                        public void run() {
                            EnterTransitionCoordinator.this.startSharedElementTransition(bundle2);
                        }
                    });
                    return false;
                }
            });
            decorView.invalidate();
        }
    }

    private void requestLayoutForSharedElements() {
        int numSharedElements = this.mSharedElements.size();
        for (int i = 0; i < numSharedElements; i++) {
            this.mSharedElements.get(i).requestLayout();
        }
    }

    private Transition beginTransition(ViewGroup decorView, boolean startEnterTransition, boolean startSharedElementTransition) {
        Transition sharedElementTransition = null;
        if (startSharedElementTransition) {
            if (!this.mSharedElementNames.isEmpty()) {
                sharedElementTransition = configureTransition(getSharedElementTransition(), false);
            }
            if (sharedElementTransition == null) {
                sharedElementTransitionStarted();
                sharedElementTransitionComplete();
            } else {
                sharedElementTransition.addListener(new Transition.TransitionListenerAdapter() {
                    public void onTransitionStart(Transition transition) {
                        EnterTransitionCoordinator.this.sharedElementTransitionStarted();
                    }

                    public void onTransitionEnd(Transition transition) {
                        transition.removeListener(this);
                        EnterTransitionCoordinator.this.sharedElementTransitionComplete();
                    }
                });
            }
        }
        Transition transitionConfigureTransition = null;
        if (startEnterTransition) {
            this.mIsViewsTransitionStarted = true;
            if (this.mTransitioningViews != null && !this.mTransitioningViews.isEmpty() && (transitionConfigureTransition = configureTransition(getViewsTransition(), true)) != null && !this.mIsReturning) {
                stripOffscreenViews();
            }
            if (transitionConfigureTransition == null) {
                viewsTransitionComplete();
            } else {
                final ArrayList<View> transitioningViews = this.mTransitioningViews;
                transitionConfigureTransition.addListener(new ActivityTransitionCoordinator.ContinueTransitionListener(this) {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        EnterTransitionCoordinator.this.mEnterViewsTransition = transition;
                        if (transitioningViews != null) {
                            EnterTransitionCoordinator.this.showViews(transitioningViews, false);
                        }
                        super.onTransitionStart(transition);
                    }

                    public void onTransitionEnd(Transition transition) {
                        EnterTransitionCoordinator.this.mEnterViewsTransition = null;
                        transition.removeListener(this);
                        EnterTransitionCoordinator.this.viewsTransitionComplete();
                        super.onTransitionEnd(transition);
                    }
                });
            }
        }
        Transition transitionMergeTransitions = mergeTransitions(sharedElementTransition, transitionConfigureTransition);
        if (transitionMergeTransitions != null) {
            transitionMergeTransitions.addListener(new ActivityTransitionCoordinator.ContinueTransitionListener());
            if (startEnterTransition) {
                setTransitioningViewsVisiblity(4, false);
            }
            TransitionManager.beginDelayedTransition(decorView, transitionMergeTransitions);
            if (startEnterTransition) {
                setTransitioningViewsVisiblity(0, false);
            }
            decorView.invalidate();
        } else {
            transitionStarted();
        }
        return transitionMergeTransitions;
    }

    @Override
    protected void onTransitionsComplete() {
        moveSharedElementsFromOverlay();
        ViewGroup decorView = getDecor();
        if (decorView == null) {
            return;
        }
        decorView.sendAccessibilityEvent(2048);
    }

    private void sharedElementTransitionStarted() {
        this.mSharedElementTransitionStarted = true;
        if (!this.mIsExitTransitionComplete) {
            return;
        }
        send(104, null);
    }

    private void startEnterTransition(Transition transition) {
        ViewGroup decorView = getDecor();
        if (this.mIsReturning || decorView == null) {
            return;
        }
        Drawable background = decorView.getBackground();
        if (background != null) {
            Drawable background2 = background.mutate();
            getWindow().setBackgroundDrawable(background2);
            this.mBackgroundAnimator = ObjectAnimator.ofInt(background2, "alpha", 255);
            this.mBackgroundAnimator.setDuration(getFadeDuration());
            this.mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    EnterTransitionCoordinator.this.makeOpaque();
                }
            });
            this.mBackgroundAnimator.start();
            return;
        }
        if (transition != null) {
            transition.addListener(new Transition.TransitionListenerAdapter() {
                public void onTransitionEnd(Transition transition2) {
                    transition2.removeListener(this);
                    EnterTransitionCoordinator.this.makeOpaque();
                }
            });
        } else {
            makeOpaque();
        }
    }

    public void stop() {
        ViewGroup decorView;
        Drawable drawable;
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.end();
            this.mBackgroundAnimator = null;
        } else if (this.mWasOpaque && (decorView = getDecor()) != null && (drawable = decorView.getBackground()) != null) {
            drawable.setAlpha(1);
        }
        makeOpaque();
        this.mIsCanceled = true;
        this.mResultReceiver = null;
        this.mActivity = null;
        moveSharedElementsFromOverlay();
        if (this.mTransitioningViews != null) {
            showViews(this.mTransitioningViews, true);
            setTransitioningViewsVisiblity(0, true);
        }
        showViews(this.mSharedElements, true);
        clearState();
    }

    public boolean cancelEnter() {
        setGhostVisibility(4);
        this.mHasStopped = true;
        this.mIsCanceled = true;
        clearState();
        return super.cancelPendingTransitions();
    }

    @Override
    protected void clearState() {
        this.mSharedElementsBundle = null;
        this.mEnterViewsTransition = null;
        this.mResultReceiver = null;
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.cancel();
            this.mBackgroundAnimator = null;
        }
        super.clearState();
    }

    private void makeOpaque() {
        if (this.mHasStopped || this.mActivity == null) {
            return;
        }
        if (this.mWasOpaque) {
            this.mActivity.convertFromTranslucent();
        }
        this.mActivity = null;
    }

    private boolean allowOverlappingTransitions() {
        return this.mIsReturning ? getWindow().getAllowReturnTransitionOverlap() : getWindow().getAllowEnterTransitionOverlap();
    }

    private void startRejectedAnimations(final ArrayList<View> rejectedSnapshots) {
        final ViewGroup decorView;
        if (rejectedSnapshots == null || rejectedSnapshots.isEmpty() || (decorView = getDecor()) == null) {
            return;
        }
        ViewGroupOverlay overlay = decorView.getOverlay();
        ObjectAnimator animator = null;
        int numRejected = rejectedSnapshots.size();
        for (int i = 0; i < numRejected; i++) {
            View snapshot = rejectedSnapshots.get(i);
            overlay.add(snapshot);
            animator = ObjectAnimator.ofFloat(snapshot, (Property<View, Float>) View.ALPHA, 1.0f, 0.0f);
            animator.start();
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewGroupOverlay overlay2 = decorView.getOverlay();
                int numRejected2 = rejectedSnapshots.size();
                for (int i2 = 0; i2 < numRejected2; i2++) {
                    overlay2.remove((View) rejectedSnapshots.get(i2));
                }
            }
        });
    }

    protected void onRemoteExitTransitionComplete() {
        if (allowOverlappingTransitions()) {
            return;
        }
        startEnterTransitionOnly();
    }

    private void startEnterTransitionOnly() {
        startTransition(new Runnable() {
            @Override
            public void run() {
                ViewGroup decorView = EnterTransitionCoordinator.this.getDecor();
                if (decorView == null) {
                    return;
                }
                Transition transition = EnterTransitionCoordinator.this.beginTransition(decorView, true, false);
                EnterTransitionCoordinator.this.startEnterTransition(transition);
            }
        });
    }
}
