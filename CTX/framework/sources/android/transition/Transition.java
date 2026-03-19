package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowId;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public abstract class Transition implements Cloneable {
    static final boolean DBG = false;
    private static final String LOG_TAG = "Transition";
    private static final int MATCH_FIRST = 1;
    public static final int MATCH_ID = 3;
    private static final String MATCH_ID_STR = "id";
    public static final int MATCH_INSTANCE = 1;
    private static final String MATCH_INSTANCE_STR = "instance";
    public static final int MATCH_ITEM_ID = 4;
    private static final String MATCH_ITEM_ID_STR = "itemId";
    private static final int MATCH_LAST = 4;
    public static final int MATCH_NAME = 2;
    private static final String MATCH_NAME_STR = "name";
    private static final String MATCH_VIEW_NAME_STR = "viewName";
    ArrayList<TransitionValues> mEndValuesList;
    EpicenterCallback mEpicenterCallback;
    ArrayMap<String, String> mNameOverrides;
    TransitionPropagation mPropagation;
    ArrayList<TransitionValues> mStartValuesList;
    private static final int[] DEFAULT_MATCH_ORDER = {2, 1, 3, 4};
    private static final PathMotion STRAIGHT_PATH_MOTION = new PathMotion() {
        @Override
        public Path getPath(float f, float f2, float f3, float f4) {
            Path path = new Path();
            path.moveTo(f, f2);
            path.lineTo(f3, f4);
            return path;
        }
    };
    private static ThreadLocal<ArrayMap<Animator, AnimationInfo>> sRunningAnimators = new ThreadLocal<>();
    private String mName = getClass().getName();
    long mStartDelay = -1;
    long mDuration = -1;
    TimeInterpolator mInterpolator = null;
    ArrayList<Integer> mTargetIds = new ArrayList<>();
    ArrayList<View> mTargets = new ArrayList<>();
    ArrayList<String> mTargetNames = null;
    ArrayList<Class> mTargetTypes = null;
    ArrayList<Integer> mTargetIdExcludes = null;
    ArrayList<View> mTargetExcludes = null;
    ArrayList<Class> mTargetTypeExcludes = null;
    ArrayList<String> mTargetNameExcludes = null;
    ArrayList<Integer> mTargetIdChildExcludes = null;
    ArrayList<View> mTargetChildExcludes = null;
    ArrayList<Class> mTargetTypeChildExcludes = null;
    private TransitionValuesMaps mStartValues = new TransitionValuesMaps();
    private TransitionValuesMaps mEndValues = new TransitionValuesMaps();
    TransitionSet mParent = null;
    int[] mMatchOrder = DEFAULT_MATCH_ORDER;
    ViewGroup mSceneRoot = null;
    boolean mCanRemoveViews = false;
    private ArrayList<Animator> mCurrentAnimators = new ArrayList<>();
    int mNumInstances = 0;
    boolean mPaused = false;
    private boolean mEnded = false;
    ArrayList<TransitionListener> mListeners = null;
    ArrayList<Animator> mAnimators = new ArrayList<>();
    PathMotion mPathMotion = STRAIGHT_PATH_MOTION;

    public static abstract class EpicenterCallback {
        public abstract Rect onGetEpicenter(Transition transition);
    }

    public interface TransitionListener {
        void onTransitionCancel(Transition transition);

        void onTransitionEnd(Transition transition);

        void onTransitionPause(Transition transition);

        void onTransitionResume(Transition transition);

        void onTransitionStart(Transition transition);
    }

    public abstract void captureEndValues(TransitionValues transitionValues);

    public abstract void captureStartValues(TransitionValues transitionValues);

    public Transition() {
    }

    public Transition(Context context, AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Transition);
        long j = typedArrayObtainStyledAttributes.getInt(1, -1);
        if (j >= 0) {
            setDuration(j);
        }
        long j2 = typedArrayObtainStyledAttributes.getInt(2, -1);
        if (j2 > 0) {
            setStartDelay(j2);
        }
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        if (resourceId > 0) {
            setInterpolator(AnimationUtils.loadInterpolator(context, resourceId));
        }
        String string = typedArrayObtainStyledAttributes.getString(3);
        if (string != null) {
            setMatchOrder(parseMatchOrder(string));
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    private static int[] parseMatchOrder(String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str, ",");
        int[] iArr = new int[stringTokenizer.countTokens()];
        int i = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String strTrim = stringTokenizer.nextToken().trim();
            if ("id".equalsIgnoreCase(strTrim)) {
                iArr[i] = 3;
            } else if (MATCH_INSTANCE_STR.equalsIgnoreCase(strTrim)) {
                iArr[i] = 1;
            } else if ("name".equalsIgnoreCase(strTrim)) {
                iArr[i] = 2;
            } else if (MATCH_VIEW_NAME_STR.equalsIgnoreCase(strTrim)) {
                iArr[i] = 2;
            } else if (MATCH_ITEM_ID_STR.equalsIgnoreCase(strTrim)) {
                iArr[i] = 4;
            } else if (strTrim.isEmpty()) {
                int[] iArr2 = new int[iArr.length - 1];
                System.arraycopy(iArr, 0, iArr2, 0, i);
                i--;
                iArr = iArr2;
            } else {
                throw new InflateException("Unknown match type in matchOrder: '" + strTrim + "'");
            }
            i++;
        }
        return iArr;
    }

    public Transition setDuration(long j) {
        this.mDuration = j;
        return this;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public Transition setStartDelay(long j) {
        this.mStartDelay = j;
        return this;
    }

    public long getStartDelay() {
        return this.mStartDelay;
    }

    public Transition setInterpolator(TimeInterpolator timeInterpolator) {
        this.mInterpolator = timeInterpolator;
        return this;
    }

    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    public String[] getTransitionProperties() {
        return null;
    }

    public Animator createAnimator(ViewGroup viewGroup, TransitionValues transitionValues, TransitionValues transitionValues2) {
        return null;
    }

    public void setMatchOrder(int... iArr) {
        if (iArr == null || iArr.length == 0) {
            this.mMatchOrder = DEFAULT_MATCH_ORDER;
            return;
        }
        for (int i = 0; i < iArr.length; i++) {
            if (!isValidMatch(iArr[i])) {
                throw new IllegalArgumentException("matches contains invalid value");
            }
            if (alreadyContains(iArr, i)) {
                throw new IllegalArgumentException("matches contains a duplicate value");
            }
        }
        this.mMatchOrder = (int[]) iArr.clone();
    }

    private static boolean isValidMatch(int i) {
        return i >= 1 && i <= 4;
    }

    private static boolean alreadyContains(int[] iArr, int i) {
        int i2 = iArr[i];
        for (int i3 = 0; i3 < i; i3++) {
            if (iArr[i3] == i2) {
                return true;
            }
        }
        return false;
    }

    private void matchInstances(ArrayMap<View, TransitionValues> arrayMap, ArrayMap<View, TransitionValues> arrayMap2) {
        TransitionValues transitionValuesRemove;
        for (int size = arrayMap.size() - 1; size >= 0; size--) {
            View viewKeyAt = arrayMap.keyAt(size);
            if (viewKeyAt != null && isValidTarget(viewKeyAt) && (transitionValuesRemove = arrayMap2.remove(viewKeyAt)) != null && transitionValuesRemove.view != null && isValidTarget(transitionValuesRemove.view)) {
                this.mStartValuesList.add(arrayMap.removeAt(size));
                this.mEndValuesList.add(transitionValuesRemove);
            }
        }
    }

    private void matchItemIds(ArrayMap<View, TransitionValues> arrayMap, ArrayMap<View, TransitionValues> arrayMap2, LongSparseArray<View> longSparseArray, LongSparseArray<View> longSparseArray2) {
        View view;
        int size = longSparseArray.size();
        for (int i = 0; i < size; i++) {
            View viewValueAt = longSparseArray.valueAt(i);
            if (viewValueAt != null && isValidTarget(viewValueAt) && (view = longSparseArray2.get(longSparseArray.keyAt(i))) != null && isValidTarget(view)) {
                TransitionValues transitionValues = arrayMap.get(viewValueAt);
                TransitionValues transitionValues2 = arrayMap2.get(view);
                if (transitionValues != null && transitionValues2 != null) {
                    this.mStartValuesList.add(transitionValues);
                    this.mEndValuesList.add(transitionValues2);
                    arrayMap.remove(viewValueAt);
                    arrayMap2.remove(view);
                }
            }
        }
    }

    private void matchIds(ArrayMap<View, TransitionValues> arrayMap, ArrayMap<View, TransitionValues> arrayMap2, SparseArray<View> sparseArray, SparseArray<View> sparseArray2) {
        View view;
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            View viewValueAt = sparseArray.valueAt(i);
            if (viewValueAt != null && isValidTarget(viewValueAt) && (view = sparseArray2.get(sparseArray.keyAt(i))) != null && isValidTarget(view)) {
                TransitionValues transitionValues = arrayMap.get(viewValueAt);
                TransitionValues transitionValues2 = arrayMap2.get(view);
                if (transitionValues != null && transitionValues2 != null) {
                    this.mStartValuesList.add(transitionValues);
                    this.mEndValuesList.add(transitionValues2);
                    arrayMap.remove(viewValueAt);
                    arrayMap2.remove(view);
                }
            }
        }
    }

    private void matchNames(ArrayMap<View, TransitionValues> arrayMap, ArrayMap<View, TransitionValues> arrayMap2, ArrayMap<String, View> arrayMap3, ArrayMap<String, View> arrayMap4) {
        View view;
        int size = arrayMap3.size();
        for (int i = 0; i < size; i++) {
            View viewValueAt = arrayMap3.valueAt(i);
            if (viewValueAt != null && isValidTarget(viewValueAt) && (view = arrayMap4.get(arrayMap3.keyAt(i))) != null && isValidTarget(view)) {
                TransitionValues transitionValues = arrayMap.get(viewValueAt);
                TransitionValues transitionValues2 = arrayMap2.get(view);
                if (transitionValues != null && transitionValues2 != null) {
                    this.mStartValuesList.add(transitionValues);
                    this.mEndValuesList.add(transitionValues2);
                    arrayMap.remove(viewValueAt);
                    arrayMap2.remove(view);
                }
            }
        }
    }

    private void addUnmatched(ArrayMap<View, TransitionValues> arrayMap, ArrayMap<View, TransitionValues> arrayMap2) {
        for (int i = 0; i < arrayMap.size(); i++) {
            TransitionValues transitionValuesValueAt = arrayMap.valueAt(i);
            if (isValidTarget(transitionValuesValueAt.view)) {
                this.mStartValuesList.add(transitionValuesValueAt);
                this.mEndValuesList.add(null);
            }
        }
        for (int i2 = 0; i2 < arrayMap2.size(); i2++) {
            TransitionValues transitionValuesValueAt2 = arrayMap2.valueAt(i2);
            if (isValidTarget(transitionValuesValueAt2.view)) {
                this.mEndValuesList.add(transitionValuesValueAt2);
                this.mStartValuesList.add(null);
            }
        }
    }

    private void matchStartAndEnd(TransitionValuesMaps transitionValuesMaps, TransitionValuesMaps transitionValuesMaps2) {
        ArrayMap<View, TransitionValues> arrayMap = new ArrayMap<>(transitionValuesMaps.viewValues);
        ArrayMap<View, TransitionValues> arrayMap2 = new ArrayMap<>(transitionValuesMaps2.viewValues);
        for (int i = 0; i < this.mMatchOrder.length; i++) {
            switch (this.mMatchOrder[i]) {
                case 1:
                    matchInstances(arrayMap, arrayMap2);
                    break;
                case 2:
                    matchNames(arrayMap, arrayMap2, transitionValuesMaps.nameValues, transitionValuesMaps2.nameValues);
                    break;
                case 3:
                    matchIds(arrayMap, arrayMap2, transitionValuesMaps.idValues, transitionValuesMaps2.idValues);
                    break;
                case 4:
                    matchItemIds(arrayMap, arrayMap2, transitionValuesMaps.itemIdValues, transitionValuesMaps2.itemIdValues);
                    break;
            }
        }
        addUnmatched(arrayMap, arrayMap2);
    }

    protected void createAnimators(ViewGroup viewGroup, TransitionValuesMaps transitionValuesMaps, TransitionValuesMaps transitionValuesMaps2, ArrayList<TransitionValues> arrayList, ArrayList<TransitionValues> arrayList2) {
        Animator animatorCreateAnimator;
        int i;
        int i2;
        View view;
        Animator animator;
        TransitionValues transitionValues;
        Animator animator2;
        TransitionValues transitionValues2;
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        this.mAnimators.size();
        SparseLongArray sparseLongArray = new SparseLongArray();
        int size = arrayList.size();
        long jMin = Long.MAX_VALUE;
        int i3 = 0;
        while (i3 < size) {
            TransitionValues transitionValues3 = arrayList.get(i3);
            TransitionValues transitionValues4 = arrayList2.get(i3);
            if (transitionValues3 != null && !transitionValues3.targetedTransitions.contains(this)) {
                transitionValues3 = null;
            }
            if (transitionValues4 != null && !transitionValues4.targetedTransitions.contains(this)) {
                transitionValues4 = null;
            }
            if (transitionValues3 != null || transitionValues4 != null) {
                if (!(transitionValues3 == null || transitionValues4 == null || isTransitionRequired(transitionValues3, transitionValues4)) || (animatorCreateAnimator = createAnimator(viewGroup, transitionValues3, transitionValues4)) == null) {
                    i = size;
                    i2 = i3;
                } else {
                    if (transitionValues4 != null) {
                        view = transitionValues4.view;
                        String[] transitionProperties = getTransitionProperties();
                        if (view != null && transitionProperties != null && transitionProperties.length > 0) {
                            transitionValues2 = new TransitionValues();
                            transitionValues2.view = view;
                            i = size;
                            TransitionValues transitionValues5 = transitionValuesMaps2.viewValues.get(view);
                            if (transitionValues5 != null) {
                                int i4 = 0;
                                while (i4 < transitionProperties.length) {
                                    transitionValues2.values.put(transitionProperties[i4], transitionValues5.values.get(transitionProperties[i4]));
                                    i4++;
                                    i3 = i3;
                                    transitionValues5 = transitionValues5;
                                }
                            }
                            i2 = i3;
                            int size2 = runningAnimators.size();
                            int i5 = 0;
                            while (true) {
                                if (i5 < size2) {
                                    AnimationInfo animationInfo = runningAnimators.get(runningAnimators.keyAt(i5));
                                    if (animationInfo.values == null || animationInfo.view != view || (((animationInfo.name != null || getName() != null) && !animationInfo.name.equals(getName())) || !animationInfo.values.equals(transitionValues2))) {
                                        i5++;
                                    } else {
                                        animator2 = null;
                                        break;
                                    }
                                } else {
                                    animator2 = animatorCreateAnimator;
                                    break;
                                }
                            }
                        } else {
                            i = size;
                            i2 = i3;
                            animator2 = animatorCreateAnimator;
                            transitionValues2 = null;
                        }
                        animator = animator2;
                        transitionValues = transitionValues2;
                    } else {
                        i = size;
                        i2 = i3;
                        view = transitionValues3 != null ? transitionValues3.view : null;
                        animator = animatorCreateAnimator;
                        transitionValues = null;
                    }
                    if (animator != null) {
                        if (this.mPropagation != null) {
                            long startDelay = this.mPropagation.getStartDelay(viewGroup, this, transitionValues3, transitionValues4);
                            sparseLongArray.put(this.mAnimators.size(), startDelay);
                            jMin = Math.min(startDelay, jMin);
                        }
                        runningAnimators.put(animator, new AnimationInfo(view, getName(), this, viewGroup.getWindowId(), transitionValues));
                        this.mAnimators.add(animator);
                        jMin = jMin;
                    }
                }
            }
            i3 = i2 + 1;
            size = i;
        }
        if (sparseLongArray.size() != 0) {
            for (int i6 = 0; i6 < sparseLongArray.size(); i6++) {
                Animator animator3 = this.mAnimators.get(sparseLongArray.keyAt(i6));
                animator3.setStartDelay((sparseLongArray.valueAt(i6) - jMin) + animator3.getStartDelay());
            }
        }
    }

    public boolean isValidTarget(View view) {
        if (view == null) {
            return false;
        }
        int id = view.getId();
        if (this.mTargetIdExcludes != null && this.mTargetIdExcludes.contains(Integer.valueOf(id))) {
            return false;
        }
        if (this.mTargetExcludes != null && this.mTargetExcludes.contains(view)) {
            return false;
        }
        if (this.mTargetTypeExcludes != null && view != null) {
            int size = this.mTargetTypeExcludes.size();
            for (int i = 0; i < size; i++) {
                if (this.mTargetTypeExcludes.get(i).isInstance(view)) {
                    return false;
                }
            }
        }
        if (this.mTargetNameExcludes != null && view != null && view.getTransitionName() != null && this.mTargetNameExcludes.contains(view.getTransitionName())) {
            return false;
        }
        if ((this.mTargetIds.size() == 0 && this.mTargets.size() == 0 && ((this.mTargetTypes == null || this.mTargetTypes.isEmpty()) && (this.mTargetNames == null || this.mTargetNames.isEmpty()))) || this.mTargetIds.contains(Integer.valueOf(id)) || this.mTargets.contains(view)) {
            return true;
        }
        if (this.mTargetNames != null && this.mTargetNames.contains(view.getTransitionName())) {
            return true;
        }
        if (this.mTargetTypes != null) {
            for (int i2 = 0; i2 < this.mTargetTypes.size(); i2++) {
                if (this.mTargetTypes.get(i2).isInstance(view)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayMap<Animator, AnimationInfo> getRunningAnimators() {
        ArrayMap<Animator, AnimationInfo> arrayMap = sRunningAnimators.get();
        if (arrayMap == null) {
            ArrayMap<Animator, AnimationInfo> arrayMap2 = new ArrayMap<>();
            sRunningAnimators.set(arrayMap2);
            return arrayMap2;
        }
        return arrayMap;
    }

    protected void runAnimators() {
        start();
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        for (Animator animator : this.mAnimators) {
            if (runningAnimators.containsKey(animator)) {
                start();
                runAnimator(animator, runningAnimators);
            }
        }
        this.mAnimators.clear();
        end();
    }

    private void runAnimator(Animator animator, final ArrayMap<Animator, AnimationInfo> arrayMap) {
        if (animator != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator2) {
                    Transition.this.mCurrentAnimators.add(animator2);
                }

                @Override
                public void onAnimationEnd(Animator animator2) {
                    arrayMap.remove(animator2);
                    Transition.this.mCurrentAnimators.remove(animator2);
                }
            });
            animate(animator);
        }
    }

    public Transition addTarget(int i) {
        if (i > 0) {
            this.mTargetIds.add(Integer.valueOf(i));
        }
        return this;
    }

    public Transition addTarget(String str) {
        if (str != null) {
            if (this.mTargetNames == null) {
                this.mTargetNames = new ArrayList<>();
            }
            this.mTargetNames.add(str);
        }
        return this;
    }

    public Transition addTarget(Class cls) {
        if (cls != null) {
            if (this.mTargetTypes == null) {
                this.mTargetTypes = new ArrayList<>();
            }
            this.mTargetTypes.add(cls);
        }
        return this;
    }

    public Transition removeTarget(int i) {
        if (i > 0) {
            this.mTargetIds.remove(Integer.valueOf(i));
        }
        return this;
    }

    public Transition removeTarget(String str) {
        if (str != null && this.mTargetNames != null) {
            this.mTargetNames.remove(str);
        }
        return this;
    }

    public Transition excludeTarget(int i, boolean z) {
        if (i >= 0) {
            this.mTargetIdExcludes = excludeObject(this.mTargetIdExcludes, Integer.valueOf(i), z);
        }
        return this;
    }

    public Transition excludeTarget(String str, boolean z) {
        this.mTargetNameExcludes = excludeObject(this.mTargetNameExcludes, str, z);
        return this;
    }

    public Transition excludeChildren(int i, boolean z) {
        if (i >= 0) {
            this.mTargetIdChildExcludes = excludeObject(this.mTargetIdChildExcludes, Integer.valueOf(i), z);
        }
        return this;
    }

    public Transition excludeTarget(View view, boolean z) {
        this.mTargetExcludes = excludeObject(this.mTargetExcludes, view, z);
        return this;
    }

    public Transition excludeChildren(View view, boolean z) {
        this.mTargetChildExcludes = excludeObject(this.mTargetChildExcludes, view, z);
        return this;
    }

    private static <T> ArrayList<T> excludeObject(ArrayList<T> arrayList, T t, boolean z) {
        if (t != null) {
            if (z) {
                return ArrayListManager.add(arrayList, t);
            }
            return ArrayListManager.remove(arrayList, t);
        }
        return arrayList;
    }

    public Transition excludeTarget(Class cls, boolean z) {
        this.mTargetTypeExcludes = excludeObject(this.mTargetTypeExcludes, cls, z);
        return this;
    }

    public Transition excludeChildren(Class cls, boolean z) {
        this.mTargetTypeChildExcludes = excludeObject(this.mTargetTypeChildExcludes, cls, z);
        return this;
    }

    public Transition addTarget(View view) {
        this.mTargets.add(view);
        return this;
    }

    public Transition removeTarget(View view) {
        if (view != null) {
            this.mTargets.remove(view);
        }
        return this;
    }

    public Transition removeTarget(Class cls) {
        if (cls != null) {
            this.mTargetTypes.remove(cls);
        }
        return this;
    }

    public List<Integer> getTargetIds() {
        return this.mTargetIds;
    }

    public List<View> getTargets() {
        return this.mTargets;
    }

    public List<String> getTargetNames() {
        return this.mTargetNames;
    }

    public List<String> getTargetViewNames() {
        return this.mTargetNames;
    }

    public List<Class> getTargetTypes() {
        return this.mTargetTypes;
    }

    void captureValues(ViewGroup viewGroup, boolean z) {
        clearValues(z);
        if ((this.mTargetIds.size() > 0 || this.mTargets.size() > 0) && ((this.mTargetNames == null || this.mTargetNames.isEmpty()) && (this.mTargetTypes == null || this.mTargetTypes.isEmpty()))) {
            for (int i = 0; i < this.mTargetIds.size(); i++) {
                View viewFindViewById = viewGroup.findViewById(this.mTargetIds.get(i).intValue());
                if (viewFindViewById != null) {
                    TransitionValues transitionValues = new TransitionValues();
                    transitionValues.view = viewFindViewById;
                    if (z) {
                        captureStartValues(transitionValues);
                    } else {
                        captureEndValues(transitionValues);
                    }
                    transitionValues.targetedTransitions.add(this);
                    capturePropagationValues(transitionValues);
                    if (z) {
                        addViewValues(this.mStartValues, viewFindViewById, transitionValues);
                    } else {
                        addViewValues(this.mEndValues, viewFindViewById, transitionValues);
                    }
                }
            }
            for (int i2 = 0; i2 < this.mTargets.size(); i2++) {
                View view = this.mTargets.get(i2);
                TransitionValues transitionValues2 = new TransitionValues();
                transitionValues2.view = view;
                if (z) {
                    captureStartValues(transitionValues2);
                } else {
                    captureEndValues(transitionValues2);
                }
                transitionValues2.targetedTransitions.add(this);
                capturePropagationValues(transitionValues2);
                if (z) {
                    addViewValues(this.mStartValues, view, transitionValues2);
                } else {
                    addViewValues(this.mEndValues, view, transitionValues2);
                }
            }
        } else {
            captureHierarchy(viewGroup, z);
        }
        if (!z && this.mNameOverrides != null) {
            int size = this.mNameOverrides.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i3 = 0; i3 < size; i3++) {
                arrayList.add(this.mStartValues.nameValues.remove(this.mNameOverrides.keyAt(i3)));
            }
            for (int i4 = 0; i4 < size; i4++) {
                View view2 = (View) arrayList.get(i4);
                if (view2 != null) {
                    this.mStartValues.nameValues.put(this.mNameOverrides.valueAt(i4), view2);
                }
            }
        }
    }

    static void addViewValues(TransitionValuesMaps transitionValuesMaps, View view, TransitionValues transitionValues) {
        transitionValuesMaps.viewValues.put(view, transitionValues);
        int id = view.getId();
        if (id >= 0) {
            if (transitionValuesMaps.idValues.indexOfKey(id) >= 0) {
                transitionValuesMaps.idValues.put(id, null);
            } else {
                transitionValuesMaps.idValues.put(id, view);
            }
        }
        String transitionName = view.getTransitionName();
        if (transitionName != null) {
            if (transitionValuesMaps.nameValues.containsKey(transitionName)) {
                transitionValuesMaps.nameValues.put(transitionName, null);
            } else {
                transitionValuesMaps.nameValues.put(transitionName, view);
            }
        }
        if (view.getParent() instanceof ListView) {
            ListView listView = (ListView) view.getParent();
            if (listView.getAdapter().hasStableIds()) {
                long itemIdAtPosition = listView.getItemIdAtPosition(listView.getPositionForView(view));
                if (transitionValuesMaps.itemIdValues.indexOfKey(itemIdAtPosition) >= 0) {
                    View view2 = transitionValuesMaps.itemIdValues.get(itemIdAtPosition);
                    if (view2 != null) {
                        view2.setHasTransientState(false);
                        transitionValuesMaps.itemIdValues.put(itemIdAtPosition, null);
                        return;
                    }
                    return;
                }
                view.setHasTransientState(true);
                transitionValuesMaps.itemIdValues.put(itemIdAtPosition, view);
            }
        }
    }

    void clearValues(boolean z) {
        if (z) {
            this.mStartValues.viewValues.clear();
            this.mStartValues.idValues.clear();
            this.mStartValues.itemIdValues.clear();
            this.mStartValues.nameValues.clear();
            this.mStartValuesList = null;
            return;
        }
        this.mEndValues.viewValues.clear();
        this.mEndValues.idValues.clear();
        this.mEndValues.itemIdValues.clear();
        this.mEndValues.nameValues.clear();
        this.mEndValuesList = null;
    }

    private void captureHierarchy(View view, boolean z) {
        if (view == null) {
            return;
        }
        int id = view.getId();
        if (this.mTargetIdExcludes != null && this.mTargetIdExcludes.contains(Integer.valueOf(id))) {
            return;
        }
        if (this.mTargetExcludes != null && this.mTargetExcludes.contains(view)) {
            return;
        }
        if (this.mTargetTypeExcludes != null && view != null) {
            int size = this.mTargetTypeExcludes.size();
            for (int i = 0; i < size; i++) {
                if (this.mTargetTypeExcludes.get(i).isInstance(view)) {
                    return;
                }
            }
        }
        if (view.getParent() instanceof ViewGroup) {
            TransitionValues transitionValues = new TransitionValues();
            transitionValues.view = view;
            if (z) {
                captureStartValues(transitionValues);
            } else {
                captureEndValues(transitionValues);
            }
            transitionValues.targetedTransitions.add(this);
            capturePropagationValues(transitionValues);
            if (z) {
                addViewValues(this.mStartValues, view, transitionValues);
            } else {
                addViewValues(this.mEndValues, view, transitionValues);
            }
        }
        if (view instanceof ViewGroup) {
            if (this.mTargetIdChildExcludes != null && this.mTargetIdChildExcludes.contains(Integer.valueOf(id))) {
                return;
            }
            if (this.mTargetChildExcludes != null && this.mTargetChildExcludes.contains(view)) {
                return;
            }
            if (this.mTargetTypeChildExcludes != null) {
                int size2 = this.mTargetTypeChildExcludes.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    if (this.mTargetTypeChildExcludes.get(i2).isInstance(view)) {
                        return;
                    }
                }
            }
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                captureHierarchy(viewGroup.getChildAt(i3), z);
            }
        }
    }

    public TransitionValues getTransitionValues(View view, boolean z) {
        if (this.mParent != null) {
            return this.mParent.getTransitionValues(view, z);
        }
        return (z ? this.mStartValues : this.mEndValues).viewValues.get(view);
    }

    TransitionValues getMatchedTransitionValues(View view, boolean z) {
        if (this.mParent != null) {
            return this.mParent.getMatchedTransitionValues(view, z);
        }
        ArrayList<TransitionValues> arrayList = z ? this.mStartValuesList : this.mEndValuesList;
        if (arrayList == null) {
            return null;
        }
        int size = arrayList.size();
        int i = -1;
        int i2 = 0;
        while (true) {
            if (i2 >= size) {
                break;
            }
            TransitionValues transitionValues = arrayList.get(i2);
            if (transitionValues == null) {
                return null;
            }
            if (transitionValues.view != view) {
                i2++;
            } else {
                i = i2;
                break;
            }
        }
    }

    public void pause(View view) {
        if (!this.mEnded) {
            ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
            int size = runningAnimators.size();
            if (view != null) {
                WindowId windowId = view.getWindowId();
                for (int i = size - 1; i >= 0; i--) {
                    AnimationInfo animationInfoValueAt = runningAnimators.valueAt(i);
                    if (animationInfoValueAt.view != null && windowId != null && windowId.equals(animationInfoValueAt.windowId)) {
                        runningAnimators.keyAt(i).pause();
                    }
                }
            }
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList arrayList = (ArrayList) this.mListeners.clone();
                int size2 = arrayList.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    ((TransitionListener) arrayList.get(i2)).onTransitionPause(this);
                }
            }
            this.mPaused = true;
        }
    }

    public void resume(View view) {
        if (this.mPaused) {
            if (!this.mEnded) {
                ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
                int size = runningAnimators.size();
                WindowId windowId = view.getWindowId();
                for (int i = size - 1; i >= 0; i--) {
                    AnimationInfo animationInfoValueAt = runningAnimators.valueAt(i);
                    if (animationInfoValueAt.view != null && windowId != null && windowId.equals(animationInfoValueAt.windowId)) {
                        runningAnimators.keyAt(i).resume();
                    }
                }
                if (this.mListeners != null && this.mListeners.size() > 0) {
                    ArrayList arrayList = (ArrayList) this.mListeners.clone();
                    int size2 = arrayList.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        ((TransitionListener) arrayList.get(i2)).onTransitionResume(this);
                    }
                }
            }
            this.mPaused = false;
        }
    }

    void playTransition(ViewGroup viewGroup) {
        AnimationInfo animationInfo;
        boolean z;
        this.mStartValuesList = new ArrayList<>();
        this.mEndValuesList = new ArrayList<>();
        matchStartAndEnd(this.mStartValues, this.mEndValues);
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        int size = runningAnimators.size();
        WindowId windowId = viewGroup.getWindowId();
        for (int i = size - 1; i >= 0; i--) {
            Animator animatorKeyAt = runningAnimators.keyAt(i);
            if (animatorKeyAt != null && (animationInfo = runningAnimators.get(animatorKeyAt)) != null && animationInfo.view != null && animationInfo.windowId == windowId) {
                TransitionValues transitionValues = animationInfo.values;
                View view = animationInfo.view;
                TransitionValues transitionValues2 = getTransitionValues(view, true);
                TransitionValues matchedTransitionValues = getMatchedTransitionValues(view, true);
                if (transitionValues2 == null && matchedTransitionValues == null) {
                    matchedTransitionValues = this.mEndValues.viewValues.get(view);
                }
                if ((transitionValues2 == null && matchedTransitionValues == null) || !animationInfo.transition.isTransitionRequired(transitionValues, matchedTransitionValues)) {
                    z = false;
                } else {
                    z = true;
                }
                if (z) {
                    if (animatorKeyAt.isRunning() || animatorKeyAt.isStarted()) {
                        animatorKeyAt.cancel();
                    } else {
                        runningAnimators.remove(animatorKeyAt);
                    }
                }
            }
        }
        createAnimators(viewGroup, this.mStartValues, this.mEndValues, this.mStartValuesList, this.mEndValuesList);
        runAnimators();
    }

    public boolean isTransitionRequired(TransitionValues transitionValues, TransitionValues transitionValues2) {
        if (transitionValues == null || transitionValues2 == null) {
            return false;
        }
        String[] transitionProperties = getTransitionProperties();
        if (transitionProperties != null) {
            for (String str : transitionProperties) {
                if (isValueChanged(transitionValues, transitionValues2, str)) {
                    return true;
                }
            }
            return false;
        }
        Iterator<String> it = transitionValues.values.keySet().iterator();
        while (it.hasNext()) {
            if (isValueChanged(transitionValues, transitionValues2, it.next())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValueChanged(TransitionValues transitionValues, TransitionValues transitionValues2, String str) {
        if (transitionValues.values.containsKey(str) != transitionValues2.values.containsKey(str)) {
            return false;
        }
        Object obj = transitionValues.values.get(str);
        Object obj2 = transitionValues2.values.get(str);
        if (obj == null && obj2 == null) {
            return false;
        }
        if (obj == null || obj2 == null) {
            return true;
        }
        return !obj.equals(obj2);
    }

    protected void animate(Animator animator) {
        if (animator == null) {
            end();
            return;
        }
        if (getDuration() >= 0) {
            animator.setDuration(getDuration());
        }
        if (getStartDelay() >= 0) {
            animator.setStartDelay(getStartDelay() + animator.getStartDelay());
        }
        if (getInterpolator() != null) {
            animator.setInterpolator(getInterpolator());
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator2) {
                Transition.this.end();
                animator2.removeListener(this);
            }
        });
        animator.start();
    }

    protected void start() {
        if (this.mNumInstances == 0) {
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList arrayList = (ArrayList) this.mListeners.clone();
                int size = arrayList.size();
                for (int i = 0; i < size; i++) {
                    ((TransitionListener) arrayList.get(i)).onTransitionStart(this);
                }
            }
            this.mEnded = false;
        }
        this.mNumInstances++;
    }

    protected void end() {
        this.mNumInstances--;
        if (this.mNumInstances == 0) {
            if (this.mListeners != null && this.mListeners.size() > 0) {
                ArrayList arrayList = (ArrayList) this.mListeners.clone();
                int size = arrayList.size();
                for (int i = 0; i < size; i++) {
                    ((TransitionListener) arrayList.get(i)).onTransitionEnd(this);
                }
            }
            for (int i2 = 0; i2 < this.mStartValues.itemIdValues.size(); i2++) {
                View viewValueAt = this.mStartValues.itemIdValues.valueAt(i2);
                if (viewValueAt != null) {
                    viewValueAt.setHasTransientState(false);
                }
            }
            for (int i3 = 0; i3 < this.mEndValues.itemIdValues.size(); i3++) {
                View viewValueAt2 = this.mEndValues.itemIdValues.valueAt(i3);
                if (viewValueAt2 != null) {
                    viewValueAt2.setHasTransientState(false);
                }
            }
            this.mEnded = true;
        }
    }

    void forceToEnd(ViewGroup viewGroup) {
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        int size = runningAnimators.size();
        if (viewGroup != null) {
            WindowId windowId = viewGroup.getWindowId();
            for (int i = size - 1; i >= 0; i--) {
                AnimationInfo animationInfoValueAt = runningAnimators.valueAt(i);
                if (animationInfoValueAt.view != null && windowId != null && windowId.equals(animationInfoValueAt.windowId)) {
                    runningAnimators.keyAt(i).end();
                }
            }
        }
    }

    protected void cancel() {
        for (int size = this.mCurrentAnimators.size() - 1; size >= 0; size--) {
            this.mCurrentAnimators.get(size).cancel();
        }
        if (this.mListeners != null && this.mListeners.size() > 0) {
            ArrayList arrayList = (ArrayList) this.mListeners.clone();
            int size2 = arrayList.size();
            for (int i = 0; i < size2; i++) {
                ((TransitionListener) arrayList.get(i)).onTransitionCancel(this);
            }
        }
    }

    public Transition addListener(TransitionListener transitionListener) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList<>();
        }
        this.mListeners.add(transitionListener);
        return this;
    }

    public Transition removeListener(TransitionListener transitionListener) {
        if (this.mListeners == null) {
            return this;
        }
        this.mListeners.remove(transitionListener);
        if (this.mListeners.size() == 0) {
            this.mListeners = null;
        }
        return this;
    }

    public void setEpicenterCallback(EpicenterCallback epicenterCallback) {
        this.mEpicenterCallback = epicenterCallback;
    }

    public EpicenterCallback getEpicenterCallback() {
        return this.mEpicenterCallback;
    }

    public Rect getEpicenter() {
        if (this.mEpicenterCallback == null) {
            return null;
        }
        return this.mEpicenterCallback.onGetEpicenter(this);
    }

    public void setPathMotion(PathMotion pathMotion) {
        if (pathMotion == null) {
            this.mPathMotion = STRAIGHT_PATH_MOTION;
        } else {
            this.mPathMotion = pathMotion;
        }
    }

    public PathMotion getPathMotion() {
        return this.mPathMotion;
    }

    public void setPropagation(TransitionPropagation transitionPropagation) {
        this.mPropagation = transitionPropagation;
    }

    public TransitionPropagation getPropagation() {
        return this.mPropagation;
    }

    void capturePropagationValues(TransitionValues transitionValues) {
        String[] propagationProperties;
        if (this.mPropagation == null || transitionValues.values.isEmpty() || (propagationProperties = this.mPropagation.getPropagationProperties()) == null) {
            return;
        }
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < propagationProperties.length) {
                if (!transitionValues.values.containsKey(propagationProperties[i])) {
                    break;
                } else {
                    i++;
                }
            } else {
                z = true;
                break;
            }
        }
        if (!z) {
            this.mPropagation.captureValues(transitionValues);
        }
    }

    Transition setSceneRoot(ViewGroup viewGroup) {
        this.mSceneRoot = viewGroup;
        return this;
    }

    void setCanRemoveViews(boolean z) {
        this.mCanRemoveViews = z;
    }

    public boolean canRemoveViews() {
        return this.mCanRemoveViews;
    }

    public void setNameOverrides(ArrayMap<String, String> arrayMap) {
        this.mNameOverrides = arrayMap;
    }

    public ArrayMap<String, String> getNameOverrides() {
        return this.mNameOverrides;
    }

    public String toString() {
        return toString("");
    }

    @Override
    public Transition mo30clone() {
        try {
            Transition transition = (Transition) super.clone();
            try {
                transition.mAnimators = new ArrayList<>();
                transition.mStartValues = new TransitionValuesMaps();
                transition.mEndValues = new TransitionValuesMaps();
                transition.mStartValuesList = null;
                transition.mEndValuesList = null;
                return transition;
            } catch (CloneNotSupportedException e) {
                return transition;
            }
        } catch (CloneNotSupportedException e2) {
            return null;
        }
    }

    public String getName() {
        return this.mName;
    }

    String toString(String str) {
        String str2 = str + getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": ";
        if (this.mDuration != -1) {
            str2 = str2 + "dur(" + this.mDuration + ") ";
        }
        if (this.mStartDelay != -1) {
            str2 = str2 + "dly(" + this.mStartDelay + ") ";
        }
        if (this.mInterpolator != null) {
            str2 = str2 + "interp(" + this.mInterpolator + ") ";
        }
        if (this.mTargetIds.size() > 0 || this.mTargets.size() > 0) {
            String str3 = str2 + "tgts(";
            if (this.mTargetIds.size() > 0) {
                String str4 = str3;
                for (int i = 0; i < this.mTargetIds.size(); i++) {
                    if (i > 0) {
                        str4 = str4 + ", ";
                    }
                    str4 = str4 + this.mTargetIds.get(i);
                }
                str3 = str4;
            }
            if (this.mTargets.size() > 0) {
                for (int i2 = 0; i2 < this.mTargets.size(); i2++) {
                    if (i2 > 0) {
                        str3 = str3 + ", ";
                    }
                    str3 = str3 + this.mTargets.get(i2);
                }
            }
            return str3 + ")";
        }
        return str2;
    }

    public static class AnimationInfo {
        String name;
        Transition transition;
        TransitionValues values;
        public View view;
        WindowId windowId;

        AnimationInfo(View view, String str, Transition transition, WindowId windowId, TransitionValues transitionValues) {
            this.view = view;
            this.name = str;
            this.values = transitionValues;
            this.windowId = windowId;
            this.transition = transition;
        }
    }

    private static class ArrayListManager {
        private ArrayListManager() {
        }

        static <T> ArrayList<T> add(ArrayList<T> arrayList, T t) {
            if (arrayList == null) {
                arrayList = new ArrayList<>();
            }
            if (!arrayList.contains(t)) {
                arrayList.add(t);
            }
            return arrayList;
        }

        static <T> ArrayList<T> remove(ArrayList<T> arrayList, T t) {
            if (arrayList != null) {
                arrayList.remove(t);
                if (arrayList.isEmpty()) {
                    return null;
                }
                return arrayList;
            }
            return arrayList;
        }
    }
}
