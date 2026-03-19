package android.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionSet;
import android.transition.Visibility;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.GhostView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import com.android.internal.view.OneShotPreDrawListener;
import java.util.ArrayList;
import java.util.Collection;

abstract class ActivityTransitionCoordinator extends ResultReceiver {
    protected static final String KEY_ELEVATION = "shared_element:elevation";
    protected static final String KEY_IMAGE_MATRIX = "shared_element:imageMatrix";
    static final String KEY_REMOTE_RECEIVER = "android:remoteReceiver";
    protected static final String KEY_SCALE_TYPE = "shared_element:scaleType";
    protected static final String KEY_SCREEN_BOTTOM = "shared_element:screenBottom";
    protected static final String KEY_SCREEN_LEFT = "shared_element:screenLeft";
    protected static final String KEY_SCREEN_RIGHT = "shared_element:screenRight";
    protected static final String KEY_SCREEN_TOP = "shared_element:screenTop";
    protected static final String KEY_SNAPSHOT = "shared_element:bitmap";
    protected static final String KEY_TRANSLATION_Z = "shared_element:translationZ";
    public static final int MSG_CANCEL = 106;
    public static final int MSG_EXIT_TRANSITION_COMPLETE = 104;
    public static final int MSG_HIDE_SHARED_ELEMENTS = 101;
    public static final int MSG_SET_REMOTE_RECEIVER = 100;
    public static final int MSG_SHARED_ELEMENT_DESTINATION = 107;
    public static final int MSG_START_EXIT_TRANSITION = 105;
    public static final int MSG_TAKE_SHARED_ELEMENTS = 103;
    protected static final ImageView.ScaleType[] SCALE_TYPE_VALUES = ImageView.ScaleType.values();
    private static final String TAG = "ActivityTransitionCoordinator";
    protected final ArrayList<String> mAllSharedElementNames;
    private boolean mBackgroundAnimatorComplete;
    private final FixedEpicenterCallback mEpicenterCallback;
    private ArrayList<GhostViewListeners> mGhostViewListeners;
    protected final boolean mIsReturning;
    private boolean mIsStartingTransition;
    protected SharedElementCallback mListener;
    private ArrayMap<View, Float> mOriginalAlphas;
    private Runnable mPendingTransition;
    protected ResultReceiver mResultReceiver;
    protected final ArrayList<String> mSharedElementNames;
    private ArrayList<Matrix> mSharedElementParentMatrices;
    private boolean mSharedElementTransitionComplete;
    protected final ArrayList<View> mSharedElements;
    private ArrayList<View> mStrippedTransitioningViews;
    protected ArrayList<View> mTransitioningViews;
    private boolean mViewsTransitionComplete;
    private Window mWindow;

    protected abstract Transition getViewsTransition();

    public ActivityTransitionCoordinator(Window window, ArrayList<String> arrayList, SharedElementCallback sharedElementCallback, boolean z) {
        super(new Handler());
        this.mSharedElements = new ArrayList<>();
        this.mSharedElementNames = new ArrayList<>();
        this.mTransitioningViews = new ArrayList<>();
        this.mEpicenterCallback = new FixedEpicenterCallback();
        this.mGhostViewListeners = new ArrayList<>();
        this.mOriginalAlphas = new ArrayMap<>();
        this.mStrippedTransitioningViews = new ArrayList<>();
        this.mWindow = window;
        this.mListener = sharedElementCallback;
        this.mAllSharedElementNames = arrayList;
        this.mIsReturning = z;
    }

    protected void viewsReady(ArrayMap<String, View> arrayMap) {
        arrayMap.retainAll(this.mAllSharedElementNames);
        if (this.mListener != null) {
            this.mListener.onMapSharedElements(this.mAllSharedElementNames, arrayMap);
        }
        setSharedElements(arrayMap);
        if (getViewsTransition() != null && this.mTransitioningViews != null) {
            ViewGroup decor = getDecor();
            if (decor != null) {
                decor.captureTransitioningViews(this.mTransitioningViews);
            }
            this.mTransitioningViews.removeAll(this.mSharedElements);
        }
        setEpicenter();
    }

    private void setSharedElements(ArrayMap<String, View> arrayMap) {
        boolean z = true;
        while (!arrayMap.isEmpty()) {
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                View viewValueAt = arrayMap.valueAt(size);
                String strKeyAt = arrayMap.keyAt(size);
                if (z && (viewValueAt == null || !viewValueAt.isAttachedToWindow() || strKeyAt == null)) {
                    arrayMap.removeAt(size);
                } else if (!isNested(viewValueAt, arrayMap)) {
                    this.mSharedElementNames.add(strKeyAt);
                    this.mSharedElements.add(viewValueAt);
                    arrayMap.removeAt(size);
                }
            }
            z = false;
        }
    }

    private static boolean isNested(View view, ArrayMap<String, View> arrayMap) {
        Object parent = view.getParent();
        while (parent instanceof View) {
            View view2 = (View) parent;
            if (arrayMap.containsValue(view2)) {
                return true;
            }
            parent = view2.getParent();
        }
        return false;
    }

    protected void stripOffscreenViews() {
        if (this.mTransitioningViews == null) {
            return;
        }
        Rect rect = new Rect();
        for (int size = this.mTransitioningViews.size() - 1; size >= 0; size--) {
            View view = this.mTransitioningViews.get(size);
            if (!view.getGlobalVisibleRect(rect)) {
                this.mTransitioningViews.remove(size);
                this.mStrippedTransitioningViews.add(view);
            }
        }
    }

    protected Window getWindow() {
        return this.mWindow;
    }

    public ViewGroup getDecor() {
        if (this.mWindow == null) {
            return null;
        }
        return (ViewGroup) this.mWindow.getDecorView();
    }

    protected void setEpicenter() {
        View view;
        int iIndexOf;
        if (!this.mAllSharedElementNames.isEmpty() && !this.mSharedElementNames.isEmpty() && (iIndexOf = this.mSharedElementNames.indexOf(this.mAllSharedElementNames.get(0))) >= 0) {
            view = this.mSharedElements.get(iIndexOf);
        } else {
            view = null;
        }
        setEpicenter(view);
    }

    private void setEpicenter(View view) {
        if (view == null) {
            this.mEpicenterCallback.setEpicenter(null);
            return;
        }
        Rect rect = new Rect();
        view.getBoundsOnScreen(rect);
        this.mEpicenterCallback.setEpicenter(rect);
    }

    public ArrayList<String> getAcceptedNames() {
        return this.mSharedElementNames;
    }

    public ArrayList<String> getMappedNames() {
        ArrayList<String> arrayList = new ArrayList<>(this.mSharedElements.size());
        for (int i = 0; i < this.mSharedElements.size(); i++) {
            arrayList.add(this.mSharedElements.get(i).getTransitionName());
        }
        return arrayList;
    }

    public ArrayList<View> copyMappedViews() {
        return new ArrayList<>(this.mSharedElements);
    }

    public ArrayList<String> getAllSharedElementNames() {
        return this.mAllSharedElementNames;
    }

    protected Transition setTargets(Transition transition, boolean z) {
        if (transition != null) {
            if (z && (this.mTransitioningViews == null || this.mTransitioningViews.isEmpty())) {
                return null;
            }
            TransitionSet transitionSet = new TransitionSet();
            if (this.mTransitioningViews != null) {
                for (int size = this.mTransitioningViews.size() - 1; size >= 0; size--) {
                    View view = this.mTransitioningViews.get(size);
                    if (z) {
                        transitionSet.addTarget(view);
                    } else {
                        transitionSet.excludeTarget(view, true);
                    }
                }
            }
            if (this.mStrippedTransitioningViews != null) {
                for (int size2 = this.mStrippedTransitioningViews.size() - 1; size2 >= 0; size2--) {
                    transitionSet.excludeTarget(this.mStrippedTransitioningViews.get(size2), true);
                }
            }
            transitionSet.addTransition(transition);
            if (!z && this.mTransitioningViews != null && !this.mTransitioningViews.isEmpty()) {
                return new TransitionSet().addTransition(transitionSet);
            }
            return transitionSet;
        }
        return null;
    }

    protected Transition configureTransition(Transition transition, boolean z) {
        if (transition != null) {
            Transition transitionMo30clone = transition.mo30clone();
            transitionMo30clone.setEpicenterCallback(this.mEpicenterCallback);
            transition = setTargets(transitionMo30clone, z);
        }
        noLayoutSuppressionForVisibilityTransitions(transition);
        return transition;
    }

    protected static void removeExcludedViews(Transition transition, ArrayList<View> arrayList) {
        ArraySet arraySet = new ArraySet();
        findIncludedViews(transition, arrayList, arraySet);
        arrayList.clear();
        arrayList.addAll(arraySet);
    }

    private static void findIncludedViews(Transition transition, ArrayList<View> arrayList, ArraySet<View> arraySet) {
        int i = 0;
        if (transition instanceof TransitionSet) {
            TransitionSet transitionSet = (TransitionSet) transition;
            ArrayList arrayList2 = new ArrayList();
            int size = arrayList.size();
            for (int i2 = 0; i2 < size; i2++) {
                View view = arrayList.get(i2);
                if (transition.isValidTarget(view)) {
                    arrayList2.add(view);
                }
            }
            int transitionCount = transitionSet.getTransitionCount();
            while (i < transitionCount) {
                findIncludedViews(transitionSet.getTransitionAt(i), arrayList2, arraySet);
                i++;
            }
            return;
        }
        int size2 = arrayList.size();
        while (i < size2) {
            View view2 = arrayList.get(i);
            if (transition.isValidTarget(view2)) {
                arraySet.add(view2);
            }
            i++;
        }
    }

    protected static Transition mergeTransitions(Transition transition, Transition transition2) {
        if (transition == null) {
            return transition2;
        }
        if (transition2 == null) {
            return transition;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(transition);
        transitionSet.addTransition(transition2);
        return transitionSet;
    }

    protected ArrayMap<String, View> mapSharedElements(ArrayList<String> arrayList, ArrayList<View> arrayList2) {
        ArrayMap<String, View> arrayMap = new ArrayMap<>();
        if (arrayList != null) {
            for (int i = 0; i < arrayList.size(); i++) {
                arrayMap.put(arrayList.get(i), arrayList2.get(i));
            }
        } else {
            ViewGroup decor = getDecor();
            if (decor != null) {
                decor.findNamedViews(arrayMap);
            }
        }
        return arrayMap;
    }

    protected void setResultReceiver(ResultReceiver resultReceiver) {
        this.mResultReceiver = resultReceiver;
    }

    private void setSharedElementState(View view, String str, Bundle bundle, Matrix matrix, RectF rectF, int[] iArr) {
        float f;
        float f2;
        float f3;
        float f4;
        int i;
        Bundle bundle2 = bundle.getBundle(str);
        if (bundle2 == null) {
            return;
        }
        if ((view instanceof ImageView) && (i = bundle2.getInt(KEY_SCALE_TYPE, -1)) >= 0) {
            ImageView imageView = (ImageView) view;
            ImageView.ScaleType scaleType = SCALE_TYPE_VALUES[i];
            imageView.setScaleType(scaleType);
            if (scaleType == ImageView.ScaleType.MATRIX) {
                matrix.setValues(bundle2.getFloatArray(KEY_IMAGE_MATRIX));
                imageView.setImageMatrix(matrix);
            }
        }
        view.setTranslationZ(bundle2.getFloat(KEY_TRANSLATION_Z));
        view.setElevation(bundle2.getFloat(KEY_ELEVATION));
        float f5 = bundle2.getFloat(KEY_SCREEN_LEFT);
        float f6 = bundle2.getFloat(KEY_SCREEN_TOP);
        float f7 = bundle2.getFloat(KEY_SCREEN_RIGHT);
        float f8 = bundle2.getFloat(KEY_SCREEN_BOTTOM);
        if (iArr != null) {
            float f9 = f5 - iArr[0];
            f2 = f6 - iArr[1];
            f3 = f7 - iArr[0];
            f4 = f8 - iArr[1];
            f = f9;
        } else {
            getSharedElementParentMatrix(view, matrix);
            rectF.set(f5, f6, f7, f8);
            matrix.mapRect(rectF);
            float f10 = rectF.left;
            float f11 = rectF.top;
            view.getInverseMatrix().mapRect(rectF);
            float fWidth = rectF.width();
            float fHeight = rectF.height();
            view.setLeft(0);
            view.setTop(0);
            view.setRight(Math.round(fWidth));
            view.setBottom(Math.round(fHeight));
            rectF.set(0.0f, 0.0f, fWidth, fHeight);
            view.getMatrix().mapRect(rectF);
            f = f10 - rectF.left;
            f2 = f11 - rectF.top;
            f3 = f + fWidth;
            f4 = f2 + fHeight;
        }
        int iRound = Math.round(f);
        int iRound2 = Math.round(f2);
        int iRound3 = Math.round(f3) - iRound;
        int iRound4 = Math.round(f4) - iRound2;
        view.measure(View.MeasureSpec.makeMeasureSpec(iRound3, 1073741824), View.MeasureSpec.makeMeasureSpec(iRound4, 1073741824));
        view.layout(iRound, iRound2, iRound3 + iRound, iRound4 + iRound2);
    }

    private void setSharedElementMatrices() {
        int size = this.mSharedElements.size();
        if (size > 0) {
            this.mSharedElementParentMatrices = new ArrayList<>(size);
        }
        for (int i = 0; i < size; i++) {
            ViewGroup viewGroup = (ViewGroup) this.mSharedElements.get(i).getParent();
            Matrix matrix = new Matrix();
            if (viewGroup != null) {
                viewGroup.transformMatrixToLocal(matrix);
                matrix.postTranslate(viewGroup.getScrollX(), viewGroup.getScrollY());
            }
            this.mSharedElementParentMatrices.add(matrix);
        }
    }

    private void getSharedElementParentMatrix(View view, Matrix matrix) {
        int iIndexOf = this.mSharedElementParentMatrices == null ? -1 : this.mSharedElements.indexOf(view);
        if (iIndexOf < 0) {
            matrix.reset();
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).transformMatrixToLocal(matrix);
                matrix.postTranslate(r2.getScrollX(), r2.getScrollY());
                return;
            }
            return;
        }
        matrix.set(this.mSharedElementParentMatrices.get(iIndexOf));
    }

    protected ArrayList<SharedElementOriginalState> setSharedElementState(Bundle bundle, ArrayList<View> arrayList) {
        ArrayList<SharedElementOriginalState> arrayList2 = new ArrayList<>();
        if (bundle != null) {
            Matrix matrix = new Matrix();
            RectF rectF = new RectF();
            int size = this.mSharedElements.size();
            for (int i = 0; i < size; i++) {
                View view = this.mSharedElements.get(i);
                String str = this.mSharedElementNames.get(i);
                arrayList2.add(getOldSharedElementState(view, str, bundle));
                setSharedElementState(view, str, bundle, matrix, rectF, null);
            }
        }
        if (this.mListener != null) {
            this.mListener.onSharedElementStart(this.mSharedElementNames, this.mSharedElements, arrayList);
        }
        return arrayList2;
    }

    protected void notifySharedElementEnd(ArrayList<View> arrayList) {
        if (this.mListener != null) {
            this.mListener.onSharedElementEnd(this.mSharedElementNames, this.mSharedElements, arrayList);
        }
    }

    protected void scheduleSetSharedElementEnd(final ArrayList<View> arrayList) {
        ViewGroup decor = getDecor();
        if (decor != null) {
            OneShotPreDrawListener.add(decor, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.notifySharedElementEnd(arrayList);
                }
            });
        }
    }

    private static SharedElementOriginalState getOldSharedElementState(View view, String str, Bundle bundle) {
        Bundle bundle2;
        SharedElementOriginalState sharedElementOriginalState = new SharedElementOriginalState();
        sharedElementOriginalState.mLeft = view.getLeft();
        sharedElementOriginalState.mTop = view.getTop();
        sharedElementOriginalState.mRight = view.getRight();
        sharedElementOriginalState.mBottom = view.getBottom();
        sharedElementOriginalState.mMeasuredWidth = view.getMeasuredWidth();
        sharedElementOriginalState.mMeasuredHeight = view.getMeasuredHeight();
        sharedElementOriginalState.mTranslationZ = view.getTranslationZ();
        sharedElementOriginalState.mElevation = view.getElevation();
        if (!(view instanceof ImageView) || (bundle2 = bundle.getBundle(str)) == null || bundle2.getInt(KEY_SCALE_TYPE, -1) < 0) {
            return sharedElementOriginalState;
        }
        ImageView imageView = (ImageView) view;
        sharedElementOriginalState.mScaleType = imageView.getScaleType();
        if (sharedElementOriginalState.mScaleType == ImageView.ScaleType.MATRIX) {
            sharedElementOriginalState.mMatrix = new Matrix(imageView.getImageMatrix());
        }
        return sharedElementOriginalState;
    }

    protected ArrayList<View> createSnapshots(Bundle bundle, Collection<String> collection) {
        View viewOnCreateSnapshotView;
        int size = collection.size();
        ArrayList<View> arrayList = new ArrayList<>(size);
        if (size == 0) {
            return arrayList;
        }
        Context context = getWindow().getContext();
        int[] iArr = new int[2];
        ViewGroup decor = getDecor();
        if (decor != null) {
            decor.getLocationOnScreen(iArr);
        }
        Matrix matrix = new Matrix();
        for (String str : collection) {
            Bundle bundle2 = bundle.getBundle(str);
            if (bundle2 != null) {
                Parcelable parcelable = bundle2.getParcelable(KEY_SNAPSHOT);
                if (parcelable == null || this.mListener == null) {
                    viewOnCreateSnapshotView = null;
                } else {
                    viewOnCreateSnapshotView = this.mListener.onCreateSnapshotView(context, parcelable);
                }
                if (viewOnCreateSnapshotView != null) {
                    setSharedElementState(viewOnCreateSnapshotView, str, bundle, matrix, null, iArr);
                }
            } else {
                viewOnCreateSnapshotView = null;
            }
            arrayList.add(viewOnCreateSnapshotView);
        }
        return arrayList;
    }

    protected static void setOriginalSharedElementState(ArrayList<View> arrayList, ArrayList<SharedElementOriginalState> arrayList2) {
        for (int i = 0; i < arrayList2.size(); i++) {
            View view = arrayList.get(i);
            SharedElementOriginalState sharedElementOriginalState = arrayList2.get(i);
            if ((view instanceof ImageView) && sharedElementOriginalState.mScaleType != null) {
                ImageView imageView = (ImageView) view;
                imageView.setScaleType(sharedElementOriginalState.mScaleType);
                if (sharedElementOriginalState.mScaleType == ImageView.ScaleType.MATRIX) {
                    imageView.setImageMatrix(sharedElementOriginalState.mMatrix);
                }
            }
            view.setElevation(sharedElementOriginalState.mElevation);
            view.setTranslationZ(sharedElementOriginalState.mTranslationZ);
            view.measure(View.MeasureSpec.makeMeasureSpec(sharedElementOriginalState.mMeasuredWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(sharedElementOriginalState.mMeasuredHeight, 1073741824));
            view.layout(sharedElementOriginalState.mLeft, sharedElementOriginalState.mTop, sharedElementOriginalState.mRight, sharedElementOriginalState.mBottom);
        }
    }

    protected Bundle captureSharedElementState() {
        Bundle bundle = new Bundle();
        RectF rectF = new RectF();
        Matrix matrix = new Matrix();
        for (int i = 0; i < this.mSharedElements.size(); i++) {
            captureSharedElementState(this.mSharedElements.get(i), this.mSharedElementNames.get(i), bundle, matrix, rectF);
        }
        return bundle;
    }

    protected void clearState() {
        this.mWindow = null;
        this.mSharedElements.clear();
        this.mTransitioningViews = null;
        this.mStrippedTransitioningViews = null;
        this.mOriginalAlphas.clear();
        this.mResultReceiver = null;
        this.mPendingTransition = null;
        this.mListener = null;
        this.mSharedElementParentMatrices = null;
    }

    protected long getFadeDuration() {
        return getWindow().getTransitionBackgroundFadeDuration();
    }

    protected void hideViews(ArrayList<View> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            View view = arrayList.get(i);
            if (!this.mOriginalAlphas.containsKey(view)) {
                this.mOriginalAlphas.put(view, Float.valueOf(view.getAlpha()));
            }
            view.setAlpha(0.0f);
        }
    }

    protected void showViews(ArrayList<View> arrayList, boolean z) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            showView(arrayList.get(i), z);
        }
    }

    private void showView(View view, boolean z) {
        Float fRemove = this.mOriginalAlphas.remove(view);
        if (fRemove != null) {
            view.setAlpha(fRemove.floatValue());
        }
        if (z) {
            view.setTransitionAlpha(1.0f);
        }
    }

    protected void captureSharedElementState(View view, String str, Bundle bundle, Matrix matrix, RectF rectF) {
        Parcelable parcelableOnCaptureSharedElementSnapshot;
        Bundle bundle2 = new Bundle();
        matrix.reset();
        view.transformMatrixToGlobal(matrix);
        rectF.set(0.0f, 0.0f, view.getWidth(), view.getHeight());
        matrix.mapRect(rectF);
        bundle2.putFloat(KEY_SCREEN_LEFT, rectF.left);
        bundle2.putFloat(KEY_SCREEN_RIGHT, rectF.right);
        bundle2.putFloat(KEY_SCREEN_TOP, rectF.top);
        bundle2.putFloat(KEY_SCREEN_BOTTOM, rectF.bottom);
        bundle2.putFloat(KEY_TRANSLATION_Z, view.getTranslationZ());
        bundle2.putFloat(KEY_ELEVATION, view.getElevation());
        if (this.mListener != null) {
            parcelableOnCaptureSharedElementSnapshot = this.mListener.onCaptureSharedElementSnapshot(view, matrix, rectF);
        } else {
            parcelableOnCaptureSharedElementSnapshot = null;
        }
        if (parcelableOnCaptureSharedElementSnapshot != null) {
            bundle2.putParcelable(KEY_SNAPSHOT, parcelableOnCaptureSharedElementSnapshot);
        }
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            bundle2.putInt(KEY_SCALE_TYPE, scaleTypeToInt(imageView.getScaleType()));
            if (imageView.getScaleType() == ImageView.ScaleType.MATRIX) {
                float[] fArr = new float[9];
                imageView.getImageMatrix().getValues(fArr);
                bundle2.putFloatArray(KEY_IMAGE_MATRIX, fArr);
            }
        }
        bundle.putBundle(str, bundle2);
    }

    protected void startTransition(Runnable runnable) {
        if (this.mIsStartingTransition) {
            this.mPendingTransition = runnable;
        } else {
            this.mIsStartingTransition = true;
            runnable.run();
        }
    }

    protected void transitionStarted() {
        this.mIsStartingTransition = false;
    }

    protected boolean cancelPendingTransitions() {
        this.mPendingTransition = null;
        return this.mIsStartingTransition;
    }

    protected void moveSharedElementsToOverlay() {
        if (this.mWindow == null || !this.mWindow.getSharedElementsUseOverlay()) {
            return;
        }
        setSharedElementMatrices();
        int size = this.mSharedElements.size();
        ViewGroup decor = getDecor();
        if (decor != null) {
            boolean zMoveSharedElementWithParent = moveSharedElementWithParent();
            Matrix matrix = new Matrix();
            for (int i = 0; i < size; i++) {
                View view = this.mSharedElements.get(i);
                if (view.isAttachedToWindow()) {
                    matrix.reset();
                    this.mSharedElementParentMatrices.get(i).invert(matrix);
                    GhostView.addGhost(view, decor, matrix);
                    ViewGroup viewGroup = (ViewGroup) view.getParent();
                    if (zMoveSharedElementWithParent && !isInTransitionGroup(viewGroup, decor)) {
                        GhostViewListeners ghostViewListeners = new GhostViewListeners(view, viewGroup, decor);
                        viewGroup.getViewTreeObserver().addOnPreDrawListener(ghostViewListeners);
                        viewGroup.addOnAttachStateChangeListener(ghostViewListeners);
                        this.mGhostViewListeners.add(ghostViewListeners);
                    }
                }
            }
        }
    }

    protected boolean moveSharedElementWithParent() {
        return true;
    }

    public static boolean isInTransitionGroup(ViewParent viewParent, ViewGroup viewGroup) {
        if (viewParent == viewGroup || !(viewParent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup viewGroup2 = (ViewGroup) viewParent;
        if (viewGroup2.isTransitionGroup()) {
            return true;
        }
        return isInTransitionGroup(viewGroup2.getParent(), viewGroup);
    }

    protected void moveSharedElementsFromOverlay() {
        ViewGroup decor;
        int size = this.mGhostViewListeners.size();
        for (int i = 0; i < size; i++) {
            this.mGhostViewListeners.get(i).removeListener();
        }
        this.mGhostViewListeners.clear();
        if (this.mWindow != null && this.mWindow.getSharedElementsUseOverlay() && (decor = getDecor()) != null) {
            decor.getOverlay();
            int size2 = this.mSharedElements.size();
            for (int i2 = 0; i2 < size2; i2++) {
                GhostView.removeGhost(this.mSharedElements.get(i2));
            }
        }
    }

    protected void setGhostVisibility(int i) {
        int size = this.mSharedElements.size();
        for (int i2 = 0; i2 < size; i2++) {
            GhostView ghost = GhostView.getGhost(this.mSharedElements.get(i2));
            if (ghost != null) {
                ghost.setVisibility(i);
            }
        }
    }

    protected void scheduleGhostVisibilityChange(final int i) {
        ViewGroup decor = getDecor();
        if (decor != null) {
            OneShotPreDrawListener.add(decor, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.setGhostVisibility(i);
                }
            });
        }
    }

    protected boolean isViewsTransitionComplete() {
        return this.mViewsTransitionComplete;
    }

    protected void viewsTransitionComplete() {
        this.mViewsTransitionComplete = true;
        startInputWhenTransitionsComplete();
    }

    protected void backgroundAnimatorComplete() {
        this.mBackgroundAnimatorComplete = true;
    }

    protected void sharedElementTransitionComplete() {
        this.mSharedElementTransitionComplete = true;
        startInputWhenTransitionsComplete();
    }

    private void startInputWhenTransitionsComplete() {
        ViewRootImpl viewRootImpl;
        if (this.mViewsTransitionComplete && this.mSharedElementTransitionComplete) {
            ViewGroup decor = getDecor();
            if (decor != null && (viewRootImpl = decor.getViewRootImpl()) != null) {
                viewRootImpl.setPausedForTransition(false);
            }
            onTransitionsComplete();
        }
    }

    protected void pauseInput() {
        ViewGroup decor = getDecor();
        ViewRootImpl viewRootImpl = decor == null ? null : decor.getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.setPausedForTransition(true);
        }
    }

    protected void onTransitionsComplete() {
    }

    protected class ContinueTransitionListener extends TransitionListenerAdapter {
        protected ContinueTransitionListener() {
        }

        @Override
        public void onTransitionStart(Transition transition) {
            ActivityTransitionCoordinator.this.mIsStartingTransition = false;
            Runnable runnable = ActivityTransitionCoordinator.this.mPendingTransition;
            ActivityTransitionCoordinator.this.mPendingTransition = null;
            if (runnable != null) {
                ActivityTransitionCoordinator.this.startTransition(runnable);
            }
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            transition.removeListener(this);
        }
    }

    private static int scaleTypeToInt(ImageView.ScaleType scaleType) {
        for (int i = 0; i < SCALE_TYPE_VALUES.length; i++) {
            if (scaleType == SCALE_TYPE_VALUES[i]) {
                return i;
            }
        }
        return -1;
    }

    protected void setTransitioningViewsVisiblity(int i, boolean z) {
        int size;
        if (this.mTransitioningViews != null) {
            size = this.mTransitioningViews.size();
        } else {
            size = 0;
        }
        for (int i2 = 0; i2 < size; i2++) {
            View view = this.mTransitioningViews.get(i2);
            if (z) {
                view.setVisibility(i);
            } else {
                view.setTransitionVisibility(i);
            }
        }
    }

    private static void noLayoutSuppressionForVisibilityTransitions(Transition transition) {
        if (transition instanceof Visibility) {
            ((Visibility) transition).setSuppressLayout(false);
            return;
        }
        if (transition instanceof TransitionSet) {
            TransitionSet transitionSet = (TransitionSet) transition;
            int transitionCount = transitionSet.getTransitionCount();
            for (int i = 0; i < transitionCount; i++) {
                noLayoutSuppressionForVisibilityTransitions(transitionSet.getTransitionAt(i));
            }
        }
    }

    public boolean isTransitionRunning() {
        return (this.mViewsTransitionComplete && this.mSharedElementTransitionComplete && this.mBackgroundAnimatorComplete) ? false : true;
    }

    private static class FixedEpicenterCallback extends Transition.EpicenterCallback {
        private Rect mEpicenter;

        private FixedEpicenterCallback() {
        }

        public void setEpicenter(Rect rect) {
            this.mEpicenter = rect;
        }

        @Override
        public Rect onGetEpicenter(Transition transition) {
            return this.mEpicenter;
        }
    }

    private static class GhostViewListeners implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        private ViewGroup mDecor;
        private Matrix mMatrix = new Matrix();
        private View mParent;
        private View mView;
        private ViewTreeObserver mViewTreeObserver;

        public GhostViewListeners(View view, View view2, ViewGroup viewGroup) {
            this.mView = view;
            this.mParent = view2;
            this.mDecor = viewGroup;
            this.mViewTreeObserver = view2.getViewTreeObserver();
        }

        public View getView() {
            return this.mView;
        }

        @Override
        public boolean onPreDraw() {
            GhostView ghost = GhostView.getGhost(this.mView);
            if (ghost == null || !this.mView.isAttachedToWindow()) {
                removeListener();
                return true;
            }
            GhostView.calculateMatrix(this.mView, this.mDecor, this.mMatrix);
            ghost.setMatrix(this.mMatrix);
            return true;
        }

        public void removeListener() {
            if (this.mViewTreeObserver.isAlive()) {
                this.mViewTreeObserver.removeOnPreDrawListener(this);
            } else {
                this.mParent.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            this.mParent.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            this.mViewTreeObserver = view.getViewTreeObserver();
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            removeListener();
        }
    }

    static class SharedElementOriginalState {
        int mBottom;
        float mElevation;
        int mLeft;
        Matrix mMatrix;
        int mMeasuredHeight;
        int mMeasuredWidth;
        int mRight;
        ImageView.ScaleType mScaleType;
        int mTop;
        float mTranslationZ;

        SharedElementOriginalState() {
        }
    }
}
