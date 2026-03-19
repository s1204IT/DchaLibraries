package android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityTransitionCoordinator;
import android.app.EnterTransitionCoordinator;
import android.app.SharedElementCallback;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewTreeObserver;
import android.view.Window;
import com.android.internal.view.OneShotPreDrawListener;
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
    private final boolean mIsCrossTask;
    private boolean mIsExitTransitionComplete;
    private boolean mIsReadyForTransition;
    private boolean mIsViewsTransitionStarted;
    private Drawable mReplacedBackground;
    private boolean mSharedElementTransitionStarted;
    private Bundle mSharedElementsBundle;
    private OneShotPreDrawListener mViewsReadyListener;
    private boolean mWasOpaque;

    public EnterTransitionCoordinator(Activity activity, ResultReceiver resultReceiver, ArrayList<String> arrayList, boolean z, boolean z2) {
        super(activity.getWindow(), arrayList, getListener(activity, z && !z2), z);
        this.mActivity = activity;
        this.mIsCrossTask = z2;
        setResultReceiver(resultReceiver);
        prepareEnter();
        Bundle bundle = new Bundle();
        bundle.putParcelable("android:remoteReceiver", this);
        this.mResultReceiver.send(100, bundle);
        final ViewGroup decor = getDecor();
        if (decor != null) {
            final ViewTreeObserver viewTreeObserver = decor.getViewTreeObserver();
            viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (EnterTransitionCoordinator.this.mIsReadyForTransition) {
                        if (viewTreeObserver.isAlive()) {
                            viewTreeObserver.removeOnPreDrawListener(this);
                            return false;
                        }
                        decor.getViewTreeObserver().removeOnPreDrawListener(this);
                        return false;
                    }
                    return false;
                }
            });
        }
    }

    boolean isCrossTask() {
        return this.mIsCrossTask;
    }

    public void viewInstancesReady(ArrayList<String> arrayList, ArrayList<String> arrayList2, ArrayList<View> arrayList3) {
        boolean z = false;
        for (int i = 0; i < arrayList3.size(); i++) {
            View view = arrayList3.get(i);
            if (!TextUtils.equals(view.getTransitionName(), arrayList2.get(i)) || !view.isAttachedToWindow()) {
                z = true;
                break;
            }
        }
        if (z) {
            triggerViewsReady(mapNamedElements(arrayList, arrayList2));
        } else {
            triggerViewsReady(mapSharedElements(arrayList, arrayList3));
        }
    }

    public void namedViewsReady(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        triggerViewsReady(mapNamedElements(arrayList, arrayList2));
    }

    public Transition getEnterViewsTransition() {
        return this.mEnterViewsTransition;
    }

    @Override
    protected void viewsReady(ArrayMap<String, View> arrayMap) {
        super.viewsReady(arrayMap);
        this.mIsReadyForTransition = true;
        hideViews(this.mSharedElements);
        Transition viewsTransition = getViewsTransition();
        if (viewsTransition != null && this.mTransitioningViews != null) {
            removeExcludedViews(viewsTransition, this.mTransitioningViews);
            stripOffscreenViews();
            hideViews(this.mTransitioningViews);
        }
        if (this.mIsReturning) {
            sendSharedElementDestination();
        } else {
            moveSharedElementsToOverlay();
        }
        if (this.mSharedElementsBundle != null) {
            onTakeSharedElements();
        }
    }

    private void triggerViewsReady(final ArrayMap<String, View> arrayMap) {
        if (this.mAreViewsReady) {
            return;
        }
        this.mAreViewsReady = true;
        ViewGroup decor = getDecor();
        if (decor == null || (decor.isAttachedToWindow() && (arrayMap.isEmpty() || !arrayMap.valueAt(0).isLayoutRequested()))) {
            viewsReady(arrayMap);
        } else {
            this.mViewsReadyListener = OneShotPreDrawListener.add(decor, new Runnable() {
                @Override
                public final void run() {
                    EnterTransitionCoordinator.lambda$triggerViewsReady$0(this.f$0, arrayMap);
                }
            });
            decor.invalidate();
        }
    }

    public static void lambda$triggerViewsReady$0(EnterTransitionCoordinator enterTransitionCoordinator, ArrayMap arrayMap) {
        enterTransitionCoordinator.mViewsReadyListener = null;
        enterTransitionCoordinator.viewsReady(arrayMap);
    }

    private ArrayMap<String, View> mapNamedElements(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        View view;
        ArrayMap<String, View> arrayMap = new ArrayMap<>();
        ViewGroup decor = getDecor();
        if (decor != null) {
            decor.findNamedViews(arrayMap);
        }
        if (arrayList != null) {
            for (int i = 0; i < arrayList2.size(); i++) {
                String str = arrayList2.get(i);
                String str2 = arrayList.get(i);
                if (str != null && !str.equals(str2) && (view = arrayMap.get(str)) != null) {
                    arrayMap.put(str2, view);
                }
            }
        }
        return arrayMap;
    }

    private void sendSharedElementDestination() {
        ViewGroup decor = getDecor();
        boolean zIsLayoutRequested = true;
        if (!allowOverlappingTransitions() || getEnterViewsTransition() == null) {
            if (decor != null && ((zIsLayoutRequested = true ^ decor.isLayoutRequested()))) {
                for (int i = 0; i < this.mSharedElements.size(); i++) {
                    if (this.mSharedElements.get(i).isLayoutRequested()) {
                        zIsLayoutRequested = false;
                        break;
                    }
                }
            }
        } else {
            zIsLayoutRequested = false;
            break;
        }
        if (zIsLayoutRequested) {
            Bundle bundleCaptureSharedElementState = captureSharedElementState();
            moveSharedElementsToOverlay();
            this.mResultReceiver.send(107, bundleCaptureSharedElementState);
        } else if (decor != null) {
            OneShotPreDrawListener.add(decor, new Runnable() {
                @Override
                public final void run() {
                    EnterTransitionCoordinator.lambda$sendSharedElementDestination$1(this.f$0);
                }
            });
        }
        if (allowOverlappingTransitions()) {
            startEnterTransitionOnly();
        }
    }

    public static void lambda$sendSharedElementDestination$1(EnterTransitionCoordinator enterTransitionCoordinator) {
        if (enterTransitionCoordinator.mResultReceiver != null) {
            Bundle bundleCaptureSharedElementState = enterTransitionCoordinator.captureSharedElementState();
            enterTransitionCoordinator.moveSharedElementsToOverlay();
            enterTransitionCoordinator.mResultReceiver.send(107, bundleCaptureSharedElementState);
        }
    }

    private static SharedElementCallback getListener(Activity activity, boolean z) {
        return z ? activity.mExitTransitionListener : activity.mEnterTransitionListener;
    }

    @Override
    protected void onReceiveResult(int i, Bundle bundle) {
        switch (i) {
            case 103:
                if (!this.mIsCanceled) {
                    this.mSharedElementsBundle = bundle;
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
            if (getDecor() != null && this.mViewsReadyListener != null) {
                this.mViewsReadyListener.removeListener();
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
        if (this.mResultReceiver != null) {
            this.mResultReceiver.send(106, null);
            this.mResultReceiver = null;
        }
    }

    private void cancel() {
        if (!this.mIsCanceled) {
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
    }

    public boolean isReturning() {
        return this.mIsReturning;
    }

    protected void prepareEnter() {
        Drawable drawableMutate;
        ViewGroup decor = getDecor();
        if (this.mActivity == null || decor == null) {
            return;
        }
        if (!isCrossTask()) {
            this.mActivity.overridePendingTransition(0, 0);
        }
        if (!this.mIsReturning) {
            this.mWasOpaque = this.mActivity.convertToTranslucent(null, null);
            Drawable background = decor.getBackground();
            if (background == null) {
                drawableMutate = new ColorDrawable(0);
                this.mReplacedBackground = drawableMutate;
            } else {
                getWindow().setBackgroundDrawable(null);
                drawableMutate = background.mutate();
                drawableMutate.setAlpha(0);
            }
            getWindow().setBackgroundDrawable(drawableMutate);
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

    private void startSharedElementTransition(Bundle bundle) {
        ViewGroup decor = getDecor();
        if (decor == null) {
            return;
        }
        ArrayList arrayList = new ArrayList(this.mAllSharedElementNames);
        arrayList.removeAll(this.mSharedElementNames);
        ArrayList<View> arrayListCreateSnapshots = createSnapshots(bundle, arrayList);
        if (this.mListener != null) {
            this.mListener.onRejectSharedElements(arrayListCreateSnapshots);
        }
        removeNullViews(arrayListCreateSnapshots);
        startRejectedAnimations(arrayListCreateSnapshots);
        ArrayList<View> arrayListCreateSnapshots2 = createSnapshots(bundle, this.mSharedElementNames);
        showViews(this.mSharedElements, true);
        scheduleSetSharedElementEnd(arrayListCreateSnapshots2);
        ArrayList<ActivityTransitionCoordinator.SharedElementOriginalState> sharedElementState = setSharedElementState(bundle, arrayListCreateSnapshots2);
        requestLayoutForSharedElements();
        boolean z = allowOverlappingTransitions() && !this.mIsReturning;
        setGhostVisibility(4);
        scheduleGhostVisibilityChange(4);
        pauseInput();
        Transition transitionBeginTransition = beginTransition(decor, z, true);
        scheduleGhostVisibilityChange(0);
        setGhostVisibility(0);
        if (z) {
            startEnterTransition(transitionBeginTransition);
        }
        setOriginalSharedElementState(this.mSharedElements, sharedElementState);
        if (this.mResultReceiver != null) {
            decor.postOnAnimation(new Runnable() {
                int mAnimations;

                @Override
                public void run() {
                    int i = this.mAnimations;
                    this.mAnimations = i + 1;
                    if (i < 2) {
                        ViewGroup decor2 = EnterTransitionCoordinator.this.getDecor();
                        if (decor2 != null) {
                            decor2.postOnAnimation(this);
                            return;
                        }
                        return;
                    }
                    if (EnterTransitionCoordinator.this.mResultReceiver != null) {
                        EnterTransitionCoordinator.this.mResultReceiver.send(101, null);
                        EnterTransitionCoordinator.this.mResultReceiver = null;
                    }
                }
            });
        }
    }

    private static void removeNullViews(ArrayList<View> arrayList) {
        if (arrayList != null) {
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                if (arrayList.get(size) == null) {
                    arrayList.remove(size);
                }
            }
        }
    }

    private void onTakeSharedElements() {
        if (!this.mIsReadyForTransition || this.mSharedElementsBundle == null) {
            return;
        }
        Bundle bundle = this.mSharedElementsBundle;
        this.mSharedElementsBundle = null;
        AnonymousClass3 anonymousClass3 = new AnonymousClass3(bundle);
        if (this.mListener == null) {
            anonymousClass3.onSharedElementsReady();
        } else {
            this.mListener.onSharedElementsArrived(this.mSharedElementNames, this.mSharedElements, anonymousClass3);
        }
    }

    class AnonymousClass3 implements SharedElementCallback.OnSharedElementsReadyListener {
        final Bundle val$sharedElementState;

        AnonymousClass3(Bundle bundle) {
            this.val$sharedElementState = bundle;
        }

        @Override
        public void onSharedElementsReady() {
            ViewGroup decor = EnterTransitionCoordinator.this.getDecor();
            if (decor != null) {
                final Bundle bundle = this.val$sharedElementState;
                OneShotPreDrawListener.add(decor, false, new Runnable() {
                    @Override
                    public final void run() {
                        EnterTransitionCoordinator.AnonymousClass3 anonymousClass3 = this.f$0;
                        EnterTransitionCoordinator.this.startTransition(new Runnable() {
                            @Override
                            public final void run() {
                                EnterTransitionCoordinator.this.startSharedElementTransition(bundle);
                            }
                        });
                    }
                });
                decor.invalidate();
            }
        }
    }

    private void requestLayoutForSharedElements() {
        int size = this.mSharedElements.size();
        for (int i = 0; i < size; i++) {
            this.mSharedElements.get(i).requestLayout();
        }
    }

    private Transition beginTransition(ViewGroup viewGroup, boolean z, boolean z2) {
        Transition transitionConfigureTransition;
        Transition transitionConfigureTransition2 = null;
        if (z2) {
            if (!this.mSharedElementNames.isEmpty()) {
                transitionConfigureTransition = configureTransition(getSharedElementTransition(), false);
            } else {
                transitionConfigureTransition = null;
            }
            if (transitionConfigureTransition == null) {
                sharedElementTransitionStarted();
                sharedElementTransitionComplete();
            } else {
                transitionConfigureTransition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        EnterTransitionCoordinator.this.sharedElementTransitionStarted();
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        transition.removeListener(this);
                        EnterTransitionCoordinator.this.sharedElementTransitionComplete();
                    }
                });
            }
        } else {
            transitionConfigureTransition = null;
        }
        if (z) {
            this.mIsViewsTransitionStarted = true;
            if (this.mTransitioningViews != null && !this.mTransitioningViews.isEmpty()) {
                transitionConfigureTransition2 = configureTransition(getViewsTransition(), true);
            }
            if (transitionConfigureTransition2 == null) {
                viewsTransitionComplete();
            } else {
                final ArrayList<View> arrayList = this.mTransitioningViews;
                transitionConfigureTransition2.addListener(new ActivityTransitionCoordinator.ContinueTransitionListener() {
                    {
                        super();
                    }

                    @Override
                    public void onTransitionStart(Transition transition) {
                        EnterTransitionCoordinator.this.mEnterViewsTransition = transition;
                        if (arrayList != null) {
                            EnterTransitionCoordinator.this.showViews(arrayList, false);
                        }
                        super.onTransitionStart(transition);
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        EnterTransitionCoordinator.this.mEnterViewsTransition = null;
                        transition.removeListener(this);
                        EnterTransitionCoordinator.this.viewsTransitionComplete();
                        super.onTransitionEnd(transition);
                    }
                });
            }
        }
        Transition transitionMergeTransitions = mergeTransitions(transitionConfigureTransition, transitionConfigureTransition2);
        if (transitionMergeTransitions != null) {
            transitionMergeTransitions.addListener(new ActivityTransitionCoordinator.ContinueTransitionListener());
            if (z) {
                setTransitioningViewsVisiblity(4, false);
            }
            TransitionManager.beginDelayedTransition(viewGroup, transitionMergeTransitions);
            if (z) {
                setTransitioningViewsVisiblity(0, false);
            }
            viewGroup.invalidate();
        } else {
            transitionStarted();
        }
        return transitionMergeTransitions;
    }

    @Override
    protected void onTransitionsComplete() {
        moveSharedElementsFromOverlay();
        ViewGroup decor = getDecor();
        if (decor != null) {
            decor.sendAccessibilityEvent(2048);
            Window window = getWindow();
            if (window != null && this.mReplacedBackground == decor.getBackground()) {
                window.setBackgroundDrawable(null);
            }
        }
    }

    private void sharedElementTransitionStarted() {
        this.mSharedElementTransitionStarted = true;
        if (this.mIsExitTransitionComplete) {
            send(104, null);
        }
    }

    private void startEnterTransition(Transition transition) {
        ViewGroup decor = getDecor();
        if (!this.mIsReturning && decor != null) {
            Drawable background = decor.getBackground();
            if (background != null) {
                Drawable drawableMutate = background.mutate();
                getWindow().setBackgroundDrawable(drawableMutate);
                this.mBackgroundAnimator = ObjectAnimator.ofInt(drawableMutate, "alpha", 255);
                this.mBackgroundAnimator.setDuration(getFadeDuration());
                this.mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        EnterTransitionCoordinator.this.makeOpaque();
                        EnterTransitionCoordinator.this.backgroundAnimatorComplete();
                    }
                });
                this.mBackgroundAnimator.start();
                return;
            }
            if (transition != null) {
                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition2) {
                        transition2.removeListener(this);
                        EnterTransitionCoordinator.this.makeOpaque();
                    }
                });
                backgroundAnimatorComplete();
                return;
            } else {
                makeOpaque();
                backgroundAnimatorComplete();
                return;
            }
        }
        backgroundAnimatorComplete();
    }

    public void stop() {
        ViewGroup decor;
        Drawable background;
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.end();
            this.mBackgroundAnimator = null;
        } else if (this.mWasOpaque && (decor = getDecor()) != null && (background = decor.getBackground()) != null) {
            background.setAlpha(1);
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
        if (!this.mHasStopped && this.mActivity != null) {
            if (this.mWasOpaque) {
                this.mActivity.convertFromTranslucent();
            }
            this.mActivity = null;
        }
    }

    private boolean allowOverlappingTransitions() {
        return this.mIsReturning ? getWindow().getAllowReturnTransitionOverlap() : getWindow().getAllowEnterTransitionOverlap();
    }

    private void startRejectedAnimations(final ArrayList<View> arrayList) {
        final ViewGroup decor;
        if (arrayList != null && !arrayList.isEmpty() && (decor = getDecor()) != null) {
            ViewGroupOverlay overlay = decor.getOverlay();
            ObjectAnimator objectAnimatorOfFloat = null;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                View view = arrayList.get(i);
                overlay.add(view);
                objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f);
                objectAnimatorOfFloat.start();
            }
            objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    ViewGroupOverlay overlay2 = decor.getOverlay();
                    int size2 = arrayList.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        overlay2.remove((View) arrayList.get(i2));
                    }
                }
            });
        }
    }

    protected void onRemoteExitTransitionComplete() {
        if (!allowOverlappingTransitions()) {
            startEnterTransitionOnly();
        }
    }

    private void startEnterTransitionOnly() {
        startTransition(new Runnable() {
            @Override
            public void run() {
                ViewGroup decor = EnterTransitionCoordinator.this.getDecor();
                if (decor != null) {
                    EnterTransitionCoordinator.this.startEnterTransition(EnterTransitionCoordinator.this.beginTransition(decor, true, false));
                }
            }
        });
    }
}
