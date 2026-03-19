package android.support.design.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.ObjectsCompat;
import android.support.v4.util.Pools;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DirectedAcyclicGraph;
import android.support.v4.widget.ViewGroupUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent2 {
    static final Class<?>[] CONSTRUCTOR_PARAMS;
    static final Comparator<View> TOP_SORTED_CHILDREN_COMPARATOR;
    static final String WIDGET_PACKAGE_NAME;
    static final ThreadLocal<Map<String, Constructor<Behavior>>> sConstructors;
    private static final Pools.Pool<Rect> sRectPool;
    private OnApplyWindowInsetsListener mApplyWindowInsetsListener;
    private View mBehaviorTouchView;
    private final DirectedAcyclicGraph<View> mChildDag;
    private final List<View> mDependencySortedChildren;
    private boolean mDisallowInterceptReset;
    private boolean mDrawStatusBarBackground;
    private boolean mIsAttachedToWindow;
    private int[] mKeylines;
    private WindowInsetsCompat mLastInsets;
    private boolean mNeedsPreDrawListener;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private View mNestedScrollingTarget;
    ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener;
    private OnPreDrawListener mOnPreDrawListener;
    private Paint mScrimPaint;
    private Drawable mStatusBarBackground;
    private final List<View> mTempDependenciesList;
    private final int[] mTempIntPair;
    private final List<View> mTempList1;

    public interface AttachedBehavior {
        Behavior getBehavior();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Deprecated
    public @interface DefaultBehavior {
        Class<? extends Behavior> value();
    }

    static {
        Package pkg = CoordinatorLayout.class.getPackage();
        WIDGET_PACKAGE_NAME = pkg != null ? pkg.getName() : null;
        if (Build.VERSION.SDK_INT >= 21) {
            TOP_SORTED_CHILDREN_COMPARATOR = new ViewElevationComparator();
        } else {
            TOP_SORTED_CHILDREN_COMPARATOR = null;
        }
        CONSTRUCTOR_PARAMS = new Class[]{Context.class, AttributeSet.class};
        sConstructors = new ThreadLocal<>();
        sRectPool = new Pools.SynchronizedPool(12);
    }

    private static Rect acquireTempRect() {
        Rect rect = sRectPool.acquire();
        if (rect == null) {
            return new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

    public CoordinatorLayout(Context context) {
        this(context, null);
    }

    public CoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.coordinatorlayout.R.attr.coordinatorLayoutStyle);
    }

    public CoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a;
        super(context, attrs, defStyleAttr);
        this.mDependencySortedChildren = new ArrayList();
        this.mChildDag = new DirectedAcyclicGraph<>();
        this.mTempList1 = new ArrayList();
        this.mTempDependenciesList = new ArrayList();
        this.mTempIntPair = new int[2];
        this.mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        if (defStyleAttr == 0) {
            a = context.obtainStyledAttributes(attrs, android.support.coordinatorlayout.R.styleable.CoordinatorLayout, 0, android.support.coordinatorlayout.R.style.Widget_Support_CoordinatorLayout);
        } else {
            a = context.obtainStyledAttributes(attrs, android.support.coordinatorlayout.R.styleable.CoordinatorLayout, defStyleAttr, 0);
        }
        int keylineArrayRes = a.getResourceId(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_keylines, 0);
        if (keylineArrayRes != 0) {
            Resources res = context.getResources();
            this.mKeylines = res.getIntArray(keylineArrayRes);
            float density = res.getDisplayMetrics().density;
            int count = this.mKeylines.length;
            for (int i = 0; i < count; i++) {
                this.mKeylines[i] = (int) (this.mKeylines[i] * density);
            }
        }
        int i2 = android.support.coordinatorlayout.R.styleable.CoordinatorLayout_statusBarBackground;
        this.mStatusBarBackground = a.getDrawable(i2);
        a.recycle();
        setupForInsets();
        super.setOnHierarchyChangeListener(new HierarchyChangeListener());
    }

    @Override
    public void setOnHierarchyChangeListener(ViewGroup.OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mOnHierarchyChangeListener = onHierarchyChangeListener;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetTouchBehaviors(false);
        if (this.mNeedsPreDrawListener) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(this.mOnPreDrawListener);
        }
        if (this.mLastInsets == null && ViewCompat.getFitsSystemWindows(this)) {
            ViewCompat.requestApplyInsets(this);
        }
        this.mIsAttachedToWindow = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetTouchBehaviors(false);
        if (this.mNeedsPreDrawListener && this.mOnPreDrawListener != null) {
            ViewTreeObserver vto = getViewTreeObserver();
            vto.removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        if (this.mNestedScrollingTarget != null) {
            onStopNestedScroll(this.mNestedScrollingTarget);
        }
        this.mIsAttachedToWindow = false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        boolean changed = false;
        Drawable d = this.mStatusBarBackground;
        if (d != null && d.isStateful()) {
            changed = false | d.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mStatusBarBackground;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean visible = visibility == 0;
        if (this.mStatusBarBackground != null && this.mStatusBarBackground.isVisible() != visible) {
            this.mStatusBarBackground.setVisible(visible, false);
        }
    }

    final WindowInsetsCompat setWindowInsets(WindowInsetsCompat insets) {
        if (!ObjectsCompat.equals(this.mLastInsets, insets)) {
            this.mLastInsets = insets;
            boolean z = false;
            this.mDrawStatusBarBackground = insets != null && insets.getSystemWindowInsetTop() > 0;
            if (!this.mDrawStatusBarBackground && getBackground() == null) {
                z = true;
            }
            setWillNotDraw(z);
            WindowInsetsCompat insets2 = dispatchApplyWindowInsetsToBehaviors(insets);
            requestLayout();
            return insets2;
        }
        return insets;
    }

    public final WindowInsetsCompat getLastWindowInsets() {
        return this.mLastInsets;
    }

    private void resetTouchBehaviors(boolean notifyOnInterceptTouchEvent) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior b = lp.getBehavior();
            if (b != null) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                if (notifyOnInterceptTouchEvent) {
                    b.onInterceptTouchEvent(this, child, cancelEvent);
                } else {
                    b.onTouchEvent(this, child, cancelEvent);
                }
                cancelEvent.recycle();
            }
        }
        for (int i2 = 0; i2 < childCount; i2++) {
            LayoutParams lp2 = (LayoutParams) getChildAt(i2).getLayoutParams();
            lp2.resetTouchBehaviorTracking();
        }
        this.mBehaviorTouchView = null;
        this.mDisallowInterceptReset = false;
    }

    private void getTopSortedChildren(List<View> out) {
        out.clear();
        boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            int childIndex = useCustomOrder ? getChildDrawingOrder(childCount, i) : i;
            View child = getChildAt(childIndex);
            out.add(child);
        }
        if (TOP_SORTED_CHILDREN_COMPARATOR != null) {
            Collections.sort(out, TOP_SORTED_CHILDREN_COMPARATOR);
        }
    }

    private boolean performIntercept(MotionEvent ev, int type) {
        int action = ev.getActionMasked();
        List<View> topmostChildList = this.mTempList1;
        getTopSortedChildren(topmostChildList);
        int childCount = topmostChildList.size();
        MotionEvent cancelEvent = null;
        boolean newBlock = false;
        boolean intercepted = false;
        for (int i = 0; i < childCount; i++) {
            View child = topmostChildList.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior b = lp.getBehavior();
            if ((intercepted || newBlock) && action != 0) {
                if (b != null) {
                    if (cancelEvent == null) {
                        long now = SystemClock.uptimeMillis();
                        cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                    }
                    switch (type) {
                        case DialogFragment.STYLE_NORMAL:
                            b.onInterceptTouchEvent(this, child, cancelEvent);
                            break;
                        case DialogFragment.STYLE_NO_TITLE:
                            b.onTouchEvent(this, child, cancelEvent);
                            break;
                    }
                }
            } else {
                if (!intercepted && b != null) {
                    switch (type) {
                        case DialogFragment.STYLE_NORMAL:
                            intercepted = b.onInterceptTouchEvent(this, child, ev);
                            break;
                        case DialogFragment.STYLE_NO_TITLE:
                            intercepted = b.onTouchEvent(this, child, ev);
                            break;
                    }
                    if (intercepted) {
                        this.mBehaviorTouchView = child;
                    }
                }
                boolean wasBlocking = lp.didBlockInteraction();
                boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
                newBlock = isBlocking && !wasBlocking;
                if (isBlocking && !newBlock) {
                    topmostChildList.clear();
                    return intercepted;
                }
            }
        }
        topmostChildList.clear();
        return intercepted;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == 0) {
            resetTouchBehaviors(true);
        }
        boolean intercepted = performIntercept(ev, 0);
        if (action == 1 || action == 3) {
            resetTouchBehaviors(true);
        }
        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        boolean cancelSuper = false;
        MotionEvent cancelEvent = null;
        int action = ev.getActionMasked();
        if (this.mBehaviorTouchView == null) {
            boolean zPerformIntercept = performIntercept(ev, 1);
            cancelSuper = zPerformIntercept;
            if (zPerformIntercept) {
                LayoutParams lp = (LayoutParams) this.mBehaviorTouchView.getLayoutParams();
                Behavior b = lp.getBehavior();
                if (b != null) {
                    handled = b.onTouchEvent(this, this.mBehaviorTouchView, ev);
                }
            }
        }
        if (this.mBehaviorTouchView == null) {
            handled |= super.onTouchEvent(ev);
        } else if (cancelSuper) {
            if (0 == 0) {
                long now = SystemClock.uptimeMillis();
                cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
            }
            super.onTouchEvent(cancelEvent);
        }
        if (cancelEvent != null) {
            cancelEvent.recycle();
        }
        if (action == 1 || action == 3) {
            resetTouchBehaviors(false);
        }
        return handled;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept && !this.mDisallowInterceptReset) {
            resetTouchBehaviors(false);
            this.mDisallowInterceptReset = true;
        }
    }

    private int getKeyline(int index) {
        if (this.mKeylines == null) {
            Log.e("CoordinatorLayout", "No keylines defined for " + this + " - attempted index lookup " + index);
            return 0;
        }
        if (index < 0 || index >= this.mKeylines.length) {
            Log.e("CoordinatorLayout", "Keyline index " + index + " out of range for " + this);
            return 0;
        }
        return this.mKeylines[index];
    }

    static Behavior parseBehavior(Context context, AttributeSet attributeSet, String str) {
        String str2;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        if (str.startsWith(".")) {
            str2 = context.getPackageName() + str;
        } else if (str.indexOf(46) < 0 && !TextUtils.isEmpty(WIDGET_PACKAGE_NAME)) {
            str2 = WIDGET_PACKAGE_NAME + '.' + str;
        } else {
            str2 = str;
        }
        try {
            Map map = sConstructors.get();
            if (map == null) {
                map = new HashMap();
                sConstructors.set((Map<String, Constructor<Behavior>>) map);
            }
            Constructor<?> constructor = (Constructor) map.get(str2);
            if (constructor == null) {
                constructor = context.getClassLoader().loadClass(str2).getConstructor(CONSTRUCTOR_PARAMS);
                constructor.setAccessible(true);
                map.put(str2, constructor);
            }
            return (Behavior) constructor.newInstance(context, attributeSet);
        } catch (Exception e) {
            throw new RuntimeException("Could not inflate Behavior subclass " + str2, e);
        }
    }

    LayoutParams getResolvedLayoutParams(View view) {
        LayoutParams result = (LayoutParams) view.getLayoutParams();
        if (!result.mBehaviorResolved) {
            if (view instanceof AttachedBehavior) {
                Behavior attachedBehavior = ((AttachedBehavior) view).getBehavior();
                if (attachedBehavior == null) {
                    Log.e("CoordinatorLayout", "Attached behavior class is null");
                }
                result.setBehavior(attachedBehavior);
                result.mBehaviorResolved = true;
            } else {
                DefaultBehavior defaultBehavior = null;
                for (Class<?> childClass = view.getClass(); childClass != null; childClass = childClass.getSuperclass()) {
                    DefaultBehavior defaultBehavior2 = (DefaultBehavior) childClass.getAnnotation(DefaultBehavior.class);
                    defaultBehavior = defaultBehavior2;
                    if (defaultBehavior2 != null) {
                        break;
                    }
                }
                if (defaultBehavior != null) {
                    try {
                        result.setBehavior(defaultBehavior.value().getDeclaredConstructor(new Class[0]).newInstance(new Object[0]));
                    } catch (Exception e) {
                        Log.e("CoordinatorLayout", "Default behavior class " + defaultBehavior.value().getName() + " could not be instantiated. Did you forget a default constructor?", e);
                    }
                }
                result.mBehaviorResolved = true;
            }
        }
        return result;
    }

    private void prepareChildren() {
        this.mDependencySortedChildren.clear();
        this.mChildDag.clear();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            LayoutParams lp = getResolvedLayoutParams(view);
            lp.findAnchorView(this, view);
            this.mChildDag.addNode(view);
            for (int j = 0; j < count; j++) {
                if (j != i) {
                    View other = getChildAt(j);
                    if (lp.dependsOn(this, view, other)) {
                        if (!this.mChildDag.contains(other)) {
                            this.mChildDag.addNode(other);
                        }
                        this.mChildDag.addEdge(other, view);
                    }
                }
            }
        }
        this.mDependencySortedChildren.addAll(this.mChildDag.getSortedList());
        Collections.reverse(this.mDependencySortedChildren);
    }

    void getDescendantRect(View descendant, Rect out) {
        ViewGroupUtils.getDescendantRect(this, descendant, out);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(), getPaddingLeft() + getPaddingRight());
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom());
    }

    public void onMeasureChild(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightUsed;
        int childState;
        int widthUsed;
        View child;
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        int heightUsed2;
        int childState2;
        int widthUsed2;
        View child2;
        int childCount;
        int heightMode;
        prepareChildren();
        ensurePreDrawListener();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        boolean isRtl = layoutDirection == 1;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode2 = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = paddingLeft + paddingRight;
        int heightPadding = paddingTop + paddingBottom;
        int widthUsed3 = getSuggestedMinimumWidth();
        int heightUsed3 = getSuggestedMinimumHeight();
        int childState3 = 0;
        boolean applyInsets = this.mLastInsets != null && ViewCompat.getFitsSystemWindows(this);
        int childCount2 = this.mDependencySortedChildren.size();
        int widthUsed4 = widthUsed3;
        int childState4 = 0;
        while (true) {
            int widthUsed5 = childState4;
            if (widthUsed5 < childCount2) {
                View child3 = this.mDependencySortedChildren.get(widthUsed5);
                if (child3.getVisibility() == 8) {
                    childCount = childCount2;
                    heightMode = heightMode2;
                } else {
                    LayoutParams lp = (LayoutParams) child3.getLayoutParams();
                    int keylineWidthUsed = 0;
                    if (lp.keyline < 0 || widthMode == 0) {
                        heightUsed = heightUsed3;
                        childState = childState3;
                        widthUsed = widthUsed4;
                    } else {
                        int keylinePos = getKeyline(lp.keyline);
                        heightUsed = heightUsed3;
                        int heightUsed4 = lp.gravity;
                        int keylineGravity = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(heightUsed4), layoutDirection) & 7;
                        childState = childState3;
                        if ((keylineGravity == 3 && !isRtl) || (keylineGravity == 5 && isRtl)) {
                            widthUsed = widthUsed4;
                            keylineWidthUsed = Math.max(0, (widthSize - paddingRight) - keylinePos);
                        } else {
                            widthUsed = widthUsed4;
                            if ((keylineGravity == 5 && !isRtl) || (keylineGravity == 3 && isRtl)) {
                                keylineWidthUsed = Math.max(0, keylinePos - paddingLeft);
                            }
                        }
                    }
                    if (!applyInsets || ViewCompat.getFitsSystemWindows(child3)) {
                        child = child3;
                        childWidthMeasureSpec = widthMeasureSpec;
                        childHeightMeasureSpec = heightMeasureSpec;
                    } else {
                        int horizInsets = this.mLastInsets.getSystemWindowInsetLeft() + this.mLastInsets.getSystemWindowInsetRight();
                        child = child3;
                        int vertInsets = this.mLastInsets.getSystemWindowInsetTop() + this.mLastInsets.getSystemWindowInsetBottom();
                        int childWidthMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(widthSize - horizInsets, widthMode);
                        int childWidthMeasureSpec3 = heightSize - vertInsets;
                        int childHeightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(childWidthMeasureSpec3, heightMode2);
                        childWidthMeasureSpec = childWidthMeasureSpec2;
                        childHeightMeasureSpec = childHeightMeasureSpec2;
                    }
                    Behavior b = lp.getBehavior();
                    if (b == null) {
                        heightUsed2 = heightUsed;
                        childState2 = childState;
                        widthUsed2 = widthUsed;
                        child2 = child;
                        childCount = childCount2;
                        heightMode = heightMode2;
                    } else {
                        View child4 = child;
                        heightUsed2 = heightUsed;
                        childState2 = childState;
                        int heightUsed5 = childWidthMeasureSpec;
                        child2 = child4;
                        widthUsed2 = widthUsed;
                        childCount = childCount2;
                        int childCount3 = childHeightMeasureSpec;
                        heightMode = heightMode2;
                        if (!b.onMeasureChild(this, child4, heightUsed5, keylineWidthUsed, childCount3, 0)) {
                        }
                        View child5 = child2;
                        int widthUsed6 = Math.max(widthUsed2, widthPadding + child5.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                        heightUsed3 = Math.max(heightUsed2, heightPadding + child5.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                        childState3 = View.combineMeasuredStates(childState2, child5.getMeasuredState());
                        widthUsed4 = widthUsed6;
                    }
                    onMeasureChild(child2, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0);
                    View child52 = child2;
                    int widthUsed62 = Math.max(widthUsed2, widthPadding + child52.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                    heightUsed3 = Math.max(heightUsed2, heightPadding + child52.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                    childState3 = View.combineMeasuredStates(childState2, child52.getMeasuredState());
                    widthUsed4 = widthUsed62;
                }
                childState4 = widthUsed5 + 1;
                childCount2 = childCount;
                heightMode2 = heightMode;
            } else {
                int childState5 = childState3;
                int childState6 = heightUsed3;
                int heightUsed6 = widthUsed4;
                int width = View.resolveSizeAndState(heightUsed6, widthMeasureSpec, (-16777216) & childState5);
                int height = View.resolveSizeAndState(childState6, heightMeasureSpec, childState5 << 16);
                setMeasuredDimension(width, height);
                return;
            }
        }
    }

    private WindowInsetsCompat dispatchApplyWindowInsetsToBehaviors(WindowInsetsCompat insets) {
        if (insets.isConsumed()) {
            return insets;
        }
        int z = getChildCount();
        for (int i = 0; i < z; i++) {
            View child = getChildAt(i);
            if (ViewCompat.getFitsSystemWindows(child)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                Behavior b = lp.getBehavior();
                if (b != null) {
                    insets = b.onApplyWindowInsets(this, child, insets);
                    if (insets.isConsumed()) {
                        break;
                    }
                } else {
                    continue;
                }
            }
        }
        return insets;
    }

    public void onLayoutChild(View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.checkAnchorChanged()) {
            throw new IllegalStateException("An anchor may not be changed after CoordinatorLayout measurement begins before layout is complete.");
        }
        if (lp.mAnchorView != null) {
            layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
        } else if (lp.keyline >= 0) {
            layoutChildWithKeyline(child, lp.keyline, layoutDirection);
        } else {
            layoutChild(child, layoutDirection);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = this.mDependencySortedChildren.get(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                Behavior behavior = lp.getBehavior();
                if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
                    onLayoutChild(child, layoutDirection);
                }
            }
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (this.mDrawStatusBarBackground && this.mStatusBarBackground != null) {
            int inset = this.mLastInsets != null ? this.mLastInsets.getSystemWindowInsetTop() : 0;
            if (inset > 0) {
                this.mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                this.mStatusBarBackground.draw(c);
            }
        }
    }

    @Override
    public void setFitsSystemWindows(boolean fitSystemWindows) {
        super.setFitsSystemWindows(fitSystemWindows);
        setupForInsets();
    }

    void recordLastChildRect(View child, Rect r) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.setLastChildRect(r);
    }

    void getLastChildRect(View child, Rect out) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        out.set(lp.getLastChildRect());
    }

    void getChildRect(View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == 8) {
            out.setEmpty();
        } else if (transform) {
            getDescendantRect(child, out);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    private void getDesiredAnchoredChildRectWithoutConstraints(View child, int layoutDirection, Rect anchorRect, Rect out, LayoutParams lp, int childWidth, int childHeight) {
        int left;
        int top;
        int absGravity = GravityCompat.getAbsoluteGravity(resolveAnchoredChildGravity(lp.gravity), layoutDirection);
        int absAnchorGravity = GravityCompat.getAbsoluteGravity(resolveGravity(lp.anchorGravity), layoutDirection);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & 112;
        int anchorHgrav = absAnchorGravity & 7;
        int anchorVgrav = absAnchorGravity & 112;
        if (anchorHgrav != 1) {
            if (anchorHgrav != 5) {
                left = anchorRect.left;
            } else {
                left = anchorRect.right;
            }
        } else {
            int left2 = anchorRect.left;
            left = left2 + (anchorRect.width() / 2);
        }
        if (anchorVgrav != 16) {
            if (anchorVgrav != 80) {
                top = anchorRect.top;
            } else {
                top = anchorRect.bottom;
            }
        } else {
            int top2 = anchorRect.top;
            top = top2 + (anchorRect.height() / 2);
        }
        if (hgrav != 1) {
            if (hgrav != 5) {
                left -= childWidth;
            }
        } else {
            left -= childWidth / 2;
        }
        if (vgrav == 16) {
            top -= childHeight / 2;
        } else if (vgrav != 80) {
            top -= childHeight;
        }
        out.set(left, top, left + childWidth, top + childHeight);
    }

    private void constrainChildRect(LayoutParams lp, Rect out, int childWidth, int childHeight) {
        int width = getWidth();
        int height = getHeight();
        int left = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(out.left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        int top = Math.max(getPaddingTop() + lp.topMargin, Math.min(out.top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        out.set(left, top, left + childWidth, top + childHeight);
    }

    void getDesiredAnchoredChildRect(View child, int layoutDirection, Rect anchorRect, Rect out) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        getDesiredAnchoredChildRectWithoutConstraints(child, layoutDirection, anchorRect, out, lp, childWidth, childHeight);
        constrainChildRect(lp, out, childWidth, childHeight);
    }

    private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
        Rect anchorRect = acquireTempRect();
        Rect childRect = acquireTempRect();
        try {
            getDescendantRect(anchor, anchorRect);
            getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);
            child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
        } finally {
            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
        }
    }

    private void layoutChildWithKeyline(View child, int keyline, int layoutDirection) {
        int keyline2;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int absGravity = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & 112;
        int width = getWidth();
        int height = getHeight();
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        if (layoutDirection == 1) {
            keyline2 = width - keyline;
        } else {
            keyline2 = keyline;
        }
        int left = getKeyline(keyline2) - childWidth;
        int top = 0;
        if (hgrav == 1) {
            left += childWidth / 2;
        } else if (hgrav == 5) {
            left += childWidth;
        }
        if (vgrav == 16) {
            top = 0 + (childHeight / 2);
        } else if (vgrav == 80) {
            top = 0 + childHeight;
        }
        int left2 = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        int top2 = Math.max(getPaddingTop() + lp.topMargin, Math.min(top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        int top3 = left2 + childWidth;
        child.layout(left2, top2, top3, top2 + childHeight);
    }

    private void layoutChild(View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        Rect parent = acquireTempRect();
        parent.set(getPaddingLeft() + lp.leftMargin, getPaddingTop() + lp.topMargin, (getWidth() - getPaddingRight()) - lp.rightMargin, (getHeight() - getPaddingBottom()) - lp.bottomMargin);
        if (this.mLastInsets != null && ViewCompat.getFitsSystemWindows(this) && !ViewCompat.getFitsSystemWindows(child)) {
            parent.left += this.mLastInsets.getSystemWindowInsetLeft();
            parent.top += this.mLastInsets.getSystemWindowInsetTop();
            parent.right -= this.mLastInsets.getSystemWindowInsetRight();
            parent.bottom -= this.mLastInsets.getSystemWindowInsetBottom();
        }
        Rect out = acquireTempRect();
        GravityCompat.apply(resolveGravity(lp.gravity), child.getMeasuredWidth(), child.getMeasuredHeight(), parent, out, layoutDirection);
        child.layout(out.left, out.top, out.right, out.bottom);
        releaseTempRect(parent);
        releaseTempRect(out);
    }

    private static int resolveGravity(int gravity) {
        if ((gravity & 7) == 0) {
            gravity |= 8388611;
        }
        if ((gravity & 112) == 0) {
            return gravity | 48;
        }
        return gravity;
    }

    private static int resolveKeylineGravity(int gravity) {
        if (gravity == 0) {
            return 8388661;
        }
        return gravity;
    }

    private static int resolveAnchoredChildGravity(int gravity) {
        if (gravity == 0) {
            return 17;
        }
        return gravity;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mBehavior != null) {
            float scrimAlpha = lp.mBehavior.getScrimOpacity(this, child);
            if (scrimAlpha > 0.0f) {
                if (this.mScrimPaint == null) {
                    this.mScrimPaint = new Paint();
                }
                this.mScrimPaint.setColor(lp.mBehavior.getScrimColor(this, child));
                this.mScrimPaint.setAlpha(clamp(Math.round(255.0f * scrimAlpha), 0, 255));
                int saved = canvas.save();
                if (child.isOpaque()) {
                    canvas.clipRect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom(), Region.Op.DIFFERENCE);
                }
                canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), this.mScrimPaint);
                canvas.restoreToCount(saved);
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    final void onChildViewsChanged(int type) {
        int j;
        boolean handled;
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        Rect inset = acquireTempRect();
        Rect drawRect = acquireTempRect();
        Rect lastDrawRect = acquireTempRect();
        for (int i = 0; i < childCount; i++) {
            View child = this.mDependencySortedChildren.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (type != 0 || child.getVisibility() != 8) {
                for (int j2 = 0; j2 < i; j2++) {
                    if (lp.mAnchorDirectChild == this.mDependencySortedChildren.get(j2)) {
                        offsetChildToAnchor(child, layoutDirection);
                    }
                }
                int j3 = 1;
                getChildRect(child, true, drawRect);
                if (lp.insetEdge != 0 && !drawRect.isEmpty()) {
                    int absInsetEdge = GravityCompat.getAbsoluteGravity(lp.insetEdge, layoutDirection);
                    int i2 = absInsetEdge & 112;
                    if (i2 == 48) {
                        inset.top = Math.max(inset.top, drawRect.bottom);
                    } else if (i2 == 80) {
                        inset.bottom = Math.max(inset.bottom, getHeight() - drawRect.top);
                    }
                    int i3 = absInsetEdge & 7;
                    if (i3 == 3) {
                        inset.left = Math.max(inset.left, drawRect.right);
                    } else if (i3 == 5) {
                        inset.right = Math.max(inset.right, getWidth() - drawRect.left);
                    }
                }
                if (lp.dodgeInsetEdges != 0 && child.getVisibility() == 0) {
                    offsetChildByInset(child, inset, layoutDirection);
                }
                if (type != 2) {
                    getLastChildRect(child, lastDrawRect);
                    if (!lastDrawRect.equals(drawRect)) {
                        recordLastChildRect(child, drawRect);
                        j = i + 1;
                        while (j < childCount) {
                            View checkChild = this.mDependencySortedChildren.get(j);
                            LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
                            Behavior b = checkLp.getBehavior();
                            if (b != null && b.layoutDependsOn(this, checkChild, child)) {
                                if (type == 0 && checkLp.getChangedAfterNestedScroll()) {
                                    checkLp.resetChangedAfterNestedScroll();
                                } else {
                                    if (type == 2) {
                                        b.onDependentViewRemoved(this, checkChild, child);
                                        handled = true;
                                    } else {
                                        handled = b.onDependentViewChanged(this, checkChild, child);
                                    }
                                    boolean handled2 = handled;
                                    if (type == j3) {
                                        checkLp.setChangedAfterNestedScroll(handled2);
                                    }
                                }
                            }
                            j++;
                            j3 = 1;
                        }
                    }
                } else {
                    j = i + 1;
                    while (j < childCount) {
                    }
                }
            }
        }
        releaseTempRect(inset);
        releaseTempRect(drawRect);
        releaseTempRect(lastDrawRect);
    }

    private void offsetChildByInset(View child, Rect inset, int layoutDirection) {
        int distance;
        int distance2;
        int distance3;
        int distance4;
        if (!ViewCompat.isLaidOut(child) || child.getWidth() <= 0 || child.getHeight() <= 0) {
            return;
        }
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        Behavior behavior = lp.getBehavior();
        Rect dodgeRect = acquireTempRect();
        Rect bounds = acquireTempRect();
        bounds.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        if (behavior != null && behavior.getInsetDodgeRect(this, child, dodgeRect)) {
            if (!bounds.contains(dodgeRect)) {
                throw new IllegalArgumentException("Rect should be within the child's bounds. Rect:" + dodgeRect.toShortString() + " | Bounds:" + bounds.toShortString());
            }
        } else {
            dodgeRect.set(bounds);
        }
        releaseTempRect(bounds);
        if (dodgeRect.isEmpty()) {
            releaseTempRect(dodgeRect);
            return;
        }
        int absDodgeInsetEdges = GravityCompat.getAbsoluteGravity(lp.dodgeInsetEdges, layoutDirection);
        boolean offsetY = false;
        if ((absDodgeInsetEdges & 48) == 48 && (distance4 = (dodgeRect.top - lp.topMargin) - lp.mInsetOffsetY) < inset.top) {
            setInsetOffsetY(child, inset.top - distance4);
            offsetY = true;
        }
        int distance5 = absDodgeInsetEdges & 80;
        if (distance5 == 80 && (distance3 = ((getHeight() - dodgeRect.bottom) - lp.bottomMargin) + lp.mInsetOffsetY) < inset.bottom) {
            setInsetOffsetY(child, distance3 - inset.bottom);
            offsetY = true;
        }
        if (!offsetY) {
            setInsetOffsetY(child, 0);
        }
        boolean offsetX = false;
        if ((absDodgeInsetEdges & 3) == 3 && (distance2 = (dodgeRect.left - lp.leftMargin) - lp.mInsetOffsetX) < inset.left) {
            setInsetOffsetX(child, inset.left - distance2);
            offsetX = true;
        }
        int distance6 = absDodgeInsetEdges & 5;
        if (distance6 == 5 && (distance = ((getWidth() - dodgeRect.right) - lp.rightMargin) + lp.mInsetOffsetX) < inset.right) {
            setInsetOffsetX(child, distance - inset.right);
            offsetX = true;
        }
        if (!offsetX) {
            setInsetOffsetX(child, 0);
        }
        releaseTempRect(dodgeRect);
    }

    private void setInsetOffsetX(View child, int offsetX) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetX != offsetX) {
            int dx = offsetX - lp.mInsetOffsetX;
            ViewCompat.offsetLeftAndRight(child, dx);
            lp.mInsetOffsetX = offsetX;
        }
    }

    private void setInsetOffsetY(View child, int offsetY) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetY != offsetY) {
            int dy = offsetY - lp.mInsetOffsetY;
            ViewCompat.offsetTopAndBottom(child, dy);
            lp.mInsetOffsetY = offsetY;
        }
    }

    public void dispatchDependentViewsChanged(View view) {
        List<View> dependents = this.mChildDag.getIncomingEdges(view);
        if (dependents != null && !dependents.isEmpty()) {
            for (int i = 0; i < dependents.size(); i++) {
                View child = dependents.get(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                Behavior b = lp.getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(this, child, view);
                }
            }
        }
    }

    public List<View> getDependencies(View child) {
        List<View> dependencies = this.mChildDag.getOutgoingEdges(child);
        this.mTempDependenciesList.clear();
        if (dependencies != null) {
            this.mTempDependenciesList.addAll(dependencies);
        }
        return this.mTempDependenciesList;
    }

    public List<View> getDependents(View child) {
        List<View> edges = this.mChildDag.getIncomingEdges(child);
        this.mTempDependenciesList.clear();
        if (edges != null) {
            this.mTempDependenciesList.addAll(edges);
        }
        return this.mTempDependenciesList;
    }

    final List<View> getDependencySortedChildren() {
        prepareChildren();
        return Collections.unmodifiableList(this.mDependencySortedChildren);
    }

    void ensurePreDrawListener() {
        boolean hasDependencies = false;
        int childCount = getChildCount();
        int i = 0;
        while (true) {
            if (i >= childCount) {
                break;
            }
            View child = getChildAt(i);
            if (!hasDependencies(child)) {
                i++;
            } else {
                hasDependencies = true;
                break;
            }
        }
        if (hasDependencies != this.mNeedsPreDrawListener) {
            if (hasDependencies) {
                addPreDrawListener();
            } else {
                removePreDrawListener();
            }
        }
    }

    private boolean hasDependencies(View child) {
        return this.mChildDag.hasOutgoingEdges(child);
    }

    void addPreDrawListener() {
        if (this.mIsAttachedToWindow) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = true;
    }

    void removePreDrawListener() {
        if (this.mIsAttachedToWindow && this.mOnPreDrawListener != null) {
            ViewTreeObserver vto = getViewTreeObserver();
            vto.removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = false;
    }

    void offsetChildToAnchor(View child, int layoutDirection) {
        Behavior b;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            Rect anchorRect = acquireTempRect();
            Rect childRect = acquireTempRect();
            Rect desiredChildRect = acquireTempRect();
            getDescendantRect(lp.mAnchorView, anchorRect);
            getChildRect(child, false, childRect);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            getDesiredAnchoredChildRectWithoutConstraints(child, layoutDirection, anchorRect, desiredChildRect, lp, childWidth, childHeight);
            boolean changed = (desiredChildRect.left == childRect.left && desiredChildRect.top == childRect.top) ? false : true;
            constrainChildRect(lp, desiredChildRect, childWidth, childHeight);
            int dx = desiredChildRect.left - childRect.left;
            int dy = desiredChildRect.top - childRect.top;
            if (dx != 0) {
                ViewCompat.offsetLeftAndRight(child, dx);
            }
            if (dy != 0) {
                ViewCompat.offsetTopAndBottom(child, dy);
            }
            if (changed && (b = lp.getBehavior()) != null) {
                b.onDependentViewChanged(this, child, lp.mAnchorView);
            }
            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
            releaseTempRect(desiredChildRect);
        }
    }

    public boolean isPointInChildBounds(View child, int x, int y) {
        Rect r = acquireTempRect();
        getDescendantRect(child, r);
        try {
            return r.contains(x, y);
        } finally {
            releaseTempRect(r);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        }
        if (p instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, 0);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int axes, int type) {
        int childCount = getChildCount();
        boolean handled = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= childCount) {
                return handled;
            }
            View view = getChildAt(i2);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    boolean accepted = viewBehavior.onStartNestedScroll(this, view, child, target, axes, type);
                    lp.setNestedScrollAccepted(type, accepted);
                    handled |= accepted;
                } else {
                    lp.setNestedScrollAccepted(type, false);
                }
            }
            i = i2 + 1;
        }
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        onNestedScrollAccepted(child, target, nestedScrollAxes, 0);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes, int type) {
        Behavior viewBehavior;
        this.mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes, type);
        this.mNestedScrollingTarget = target;
        int childCount = getChildCount();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < childCount) {
                View view = getChildAt(i2);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(type) && (viewBehavior = lp.getBehavior()) != null) {
                    viewBehavior.onNestedScrollAccepted(this, view, child, target, nestedScrollAxes, type);
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    @Override
    public void onStopNestedScroll(View target) {
        onStopNestedScroll(target, 0);
    }

    @Override
    public void onStopNestedScroll(View target, int type) {
        this.mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted(type)) {
                Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    viewBehavior.onStopNestedScroll(this, view, target, type);
                }
                lp.resetNestedScroll(type);
                lp.resetChangedAfterNestedScroll();
            }
        }
        this.mNestedScrollingTarget = null;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, 0);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        Behavior viewBehavior;
        int childCount = getChildCount();
        int i = 0;
        boolean accepted = false;
        while (true) {
            int i2 = i;
            if (i2 >= childCount) {
                break;
            }
            View view = getChildAt(i2);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(type) && (viewBehavior = lp.getBehavior()) != null) {
                    viewBehavior.onNestedScroll(this, view, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
                    accepted = true;
                }
            }
            i = i2 + 1;
        }
        if (accepted) {
            onChildViewsChanged(1);
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, 0);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        Behavior viewBehavior;
        int xConsumed;
        int childCount = getChildCount();
        int xConsumed2 = 0;
        int yConsumed = 0;
        boolean accepted = false;
        int xConsumed3 = 0;
        while (true) {
            int i = xConsumed3;
            if (i >= childCount) {
                break;
            }
            View view = getChildAt(i);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(type) && (viewBehavior = lp.getBehavior()) != null) {
                    int[] iArr = this.mTempIntPair;
                    this.mTempIntPair[1] = 0;
                    iArr[0] = 0;
                    viewBehavior.onNestedPreScroll(this, view, target, dx, dy, this.mTempIntPair, type);
                    if (dx > 0) {
                        xConsumed = Math.max(xConsumed2, this.mTempIntPair[0]);
                    } else {
                        xConsumed = Math.min(xConsumed2, this.mTempIntPair[0]);
                    }
                    xConsumed2 = xConsumed;
                    yConsumed = dy > 0 ? Math.max(yConsumed, this.mTempIntPair[1]) : Math.min(yConsumed, this.mTempIntPair[1]);
                    accepted = true;
                }
            }
            int xConsumed4 = i + 1;
            xConsumed3 = xConsumed4;
        }
        consumed[0] = xConsumed2;
        consumed[1] = yConsumed;
        if (accepted) {
            onChildViewsChanged(1);
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Behavior viewBehavior;
        int childCount = getChildCount();
        boolean handled = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= childCount) {
                break;
            }
            View view = getChildAt(i2);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(0) && (viewBehavior = lp.getBehavior()) != null) {
                    handled = viewBehavior.onNestedFling(this, view, target, velocityX, velocityY, consumed) | handled;
                }
            }
            i = i2 + 1;
        }
        if (handled) {
            onChildViewsChanged(1);
        }
        return handled;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Behavior viewBehavior;
        int childCount = getChildCount();
        boolean handled = false;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(0) && (viewBehavior = lp.getBehavior()) != null) {
                    handled |= viewBehavior.onNestedPreFling(this, view, target, velocityX, velocityY);
                }
            }
        }
        return handled;
    }

    @Override
    public int getNestedScrollAxes() {
        return this.mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    class OnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        OnPreDrawListener() {
        }

        @Override
        public boolean onPreDraw() {
            CoordinatorLayout.this.onChildViewsChanged(0);
            return true;
        }
    }

    static class ViewElevationComparator implements Comparator<View> {
        ViewElevationComparator() {
        }

        @Override
        public int compare(View lhs, View rhs) {
            float lz = ViewCompat.getZ(lhs);
            float rz = ViewCompat.getZ(rhs);
            if (lz > rz) {
                return -1;
            }
            if (lz < rz) {
                return 1;
            }
            return 0;
        }
    }

    public static abstract class Behavior<V extends View> {
        public Behavior() {
        }

        public Behavior(Context context, AttributeSet attrs) {
        }

        public void onAttachedToLayoutParams(LayoutParams params) {
        }

        public void onDetachedFromLayoutParams() {
        }

        public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
            return false;
        }

        public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
            return false;
        }

        public int getScrimColor(CoordinatorLayout parent, V child) {
            return -16777216;
        }

        public float getScrimOpacity(CoordinatorLayout parent, V child) {
            return 0.0f;
        }

        public boolean blocksInteractionBelow(CoordinatorLayout parent, V child) {
            return getScrimOpacity(parent, child) > 0.0f;
        }

        public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
            return false;
        }

        public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) {
            return false;
        }

        public void onDependentViewRemoved(CoordinatorLayout parent, V child, View dependency) {
        }

        public boolean onMeasureChild(CoordinatorLayout parent, V child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            return false;
        }

        public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
            return false;
        }

        @Deprecated
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int axes) {
            return false;
        }

        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int axes, int type) {
            if (type == 0) {
                return onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes);
            }
            return false;
        }

        @Deprecated
        public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int axes) {
        }

        public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int axes, int type) {
            if (type == 0) {
                onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes);
            }
        }

        @Deprecated
        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        }

        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int type) {
            if (type == 0) {
                onStopNestedScroll(coordinatorLayout, child, target);
            }
        }

        @Deprecated
        public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        }

        public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
            if (type == 0) {
                onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
            }
        }

        @Deprecated
        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
        }

        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed, int type) {
            if (type == 0) {
                onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
            }
        }

        public boolean onNestedFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
            return false;
        }

        public WindowInsetsCompat onApplyWindowInsets(CoordinatorLayout coordinatorLayout, V child, WindowInsetsCompat insets) {
            return insets;
        }

        public boolean onRequestChildRectangleOnScreen(CoordinatorLayout coordinatorLayout, V child, Rect rectangle, boolean immediate) {
            return false;
        }

        public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        }

        public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
            return View.BaseSavedState.EMPTY_STATE;
        }

        public boolean getInsetDodgeRect(CoordinatorLayout parent, V child, Rect rect) {
            return false;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int anchorGravity;
        public int dodgeInsetEdges;
        public int gravity;
        public int insetEdge;
        public int keyline;
        View mAnchorDirectChild;
        int mAnchorId;
        View mAnchorView;
        Behavior mBehavior;
        boolean mBehaviorResolved;
        Object mBehaviorTag;
        private boolean mDidAcceptNestedScrollNonTouch;
        private boolean mDidAcceptNestedScrollTouch;
        private boolean mDidBlockInteraction;
        private boolean mDidChangeAfterNestedScroll;
        int mInsetOffsetX;
        int mInsetOffsetY;
        final Rect mLastChildRect;

        public LayoutParams(int width, int height) {
            super(width, height);
            this.mBehaviorResolved = false;
            this.gravity = 0;
            this.anchorGravity = 0;
            this.keyline = -1;
            this.mAnchorId = -1;
            this.insetEdge = 0;
            this.dodgeInsetEdges = 0;
            this.mLastChildRect = new Rect();
        }

        LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mBehaviorResolved = false;
            this.gravity = 0;
            this.anchorGravity = 0;
            this.keyline = -1;
            this.mAnchorId = -1;
            this.insetEdge = 0;
            this.dodgeInsetEdges = 0;
            this.mLastChildRect = new Rect();
            TypedArray a = context.obtainStyledAttributes(attrs, android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout);
            this.gravity = a.getInteger(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_android_layout_gravity, 0);
            this.mAnchorId = a.getResourceId(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_anchor, -1);
            this.anchorGravity = a.getInteger(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_anchorGravity, 0);
            this.keyline = a.getInteger(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_keyline, -1);
            this.insetEdge = a.getInt(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_insetEdge, 0);
            this.dodgeInsetEdges = a.getInt(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_dodgeInsetEdges, 0);
            this.mBehaviorResolved = a.hasValue(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_behavior);
            if (this.mBehaviorResolved) {
                this.mBehavior = CoordinatorLayout.parseBehavior(context, attrs, a.getString(android.support.coordinatorlayout.R.styleable.CoordinatorLayout_Layout_layout_behavior));
            }
            a.recycle();
            if (this.mBehavior != null) {
                this.mBehavior.onAttachedToLayoutParams(this);
            }
        }

        public LayoutParams(LayoutParams p) {
            super((ViewGroup.MarginLayoutParams) p);
            this.mBehaviorResolved = false;
            this.gravity = 0;
            this.anchorGravity = 0;
            this.keyline = -1;
            this.mAnchorId = -1;
            this.insetEdge = 0;
            this.dodgeInsetEdges = 0;
            this.mLastChildRect = new Rect();
        }

        public LayoutParams(ViewGroup.MarginLayoutParams p) {
            super(p);
            this.mBehaviorResolved = false;
            this.gravity = 0;
            this.anchorGravity = 0;
            this.keyline = -1;
            this.mAnchorId = -1;
            this.insetEdge = 0;
            this.dodgeInsetEdges = 0;
            this.mLastChildRect = new Rect();
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            this.mBehaviorResolved = false;
            this.gravity = 0;
            this.anchorGravity = 0;
            this.keyline = -1;
            this.mAnchorId = -1;
            this.insetEdge = 0;
            this.dodgeInsetEdges = 0;
            this.mLastChildRect = new Rect();
        }

        public int getAnchorId() {
            return this.mAnchorId;
        }

        public Behavior getBehavior() {
            return this.mBehavior;
        }

        public void setBehavior(Behavior behavior) {
            if (this.mBehavior != behavior) {
                if (this.mBehavior != null) {
                    this.mBehavior.onDetachedFromLayoutParams();
                }
                this.mBehavior = behavior;
                this.mBehaviorTag = null;
                this.mBehaviorResolved = true;
                if (behavior != null) {
                    behavior.onAttachedToLayoutParams(this);
                }
            }
        }

        void setLastChildRect(Rect r) {
            this.mLastChildRect.set(r);
        }

        Rect getLastChildRect() {
            return this.mLastChildRect;
        }

        boolean checkAnchorChanged() {
            return this.mAnchorView == null && this.mAnchorId != -1;
        }

        boolean didBlockInteraction() {
            if (this.mBehavior == null) {
                this.mDidBlockInteraction = false;
            }
            return this.mDidBlockInteraction;
        }

        boolean isBlockingInteractionBelow(CoordinatorLayout parent, View child) {
            if (this.mDidBlockInteraction) {
                return true;
            }
            boolean zBlocksInteractionBelow = this.mDidBlockInteraction | (this.mBehavior != null ? this.mBehavior.blocksInteractionBelow(parent, child) : false);
            this.mDidBlockInteraction = zBlocksInteractionBelow;
            return zBlocksInteractionBelow;
        }

        void resetTouchBehaviorTracking() {
            this.mDidBlockInteraction = false;
        }

        void resetNestedScroll(int type) {
            setNestedScrollAccepted(type, false);
        }

        void setNestedScrollAccepted(int type, boolean accept) {
            switch (type) {
                case DialogFragment.STYLE_NORMAL:
                    this.mDidAcceptNestedScrollTouch = accept;
                    break;
                case DialogFragment.STYLE_NO_TITLE:
                    this.mDidAcceptNestedScrollNonTouch = accept;
                    break;
            }
        }

        boolean isNestedScrollAccepted(int type) {
            switch (type) {
                case DialogFragment.STYLE_NORMAL:
                    return this.mDidAcceptNestedScrollTouch;
                case DialogFragment.STYLE_NO_TITLE:
                    return this.mDidAcceptNestedScrollNonTouch;
                default:
                    return false;
            }
        }

        boolean getChangedAfterNestedScroll() {
            return this.mDidChangeAfterNestedScroll;
        }

        void setChangedAfterNestedScroll(boolean changed) {
            this.mDidChangeAfterNestedScroll = changed;
        }

        void resetChangedAfterNestedScroll() {
            this.mDidChangeAfterNestedScroll = false;
        }

        boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency == this.mAnchorDirectChild || shouldDodge(dependency, ViewCompat.getLayoutDirection(parent)) || (this.mBehavior != null && this.mBehavior.layoutDependsOn(parent, child, dependency));
        }

        View findAnchorView(CoordinatorLayout parent, View forChild) {
            if (this.mAnchorId == -1) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
                return null;
            }
            if (this.mAnchorView == null || !verifyAnchorView(forChild, parent)) {
                resolveAnchorView(forChild, parent);
            }
            return this.mAnchorView;
        }

        private void resolveAnchorView(View forChild, CoordinatorLayout parent) {
            this.mAnchorView = parent.findViewById(this.mAnchorId);
            if (this.mAnchorView != null) {
                if (this.mAnchorView == parent) {
                    if (parent.isInEditMode()) {
                        this.mAnchorDirectChild = null;
                        this.mAnchorView = null;
                        return;
                    }
                    throw new IllegalStateException("View can not be anchored to the the parent CoordinatorLayout");
                }
                View directChild = this.mAnchorView;
                for (ViewParent p = this.mAnchorView.getParent(); p != parent && p != null; p = p.getParent()) {
                    if (p == forChild) {
                        if (parent.isInEditMode()) {
                            this.mAnchorDirectChild = null;
                            this.mAnchorView = null;
                            return;
                        }
                        throw new IllegalStateException("Anchor must not be a descendant of the anchored view");
                    }
                    if (p instanceof View) {
                        directChild = (View) p;
                    }
                }
                this.mAnchorDirectChild = directChild;
                return;
            }
            if (parent.isInEditMode()) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
                return;
            }
            throw new IllegalStateException("Could not find CoordinatorLayout descendant view with id " + parent.getResources().getResourceName(this.mAnchorId) + " to anchor view " + forChild);
        }

        private boolean verifyAnchorView(View forChild, CoordinatorLayout parent) {
            if (this.mAnchorView.getId() != this.mAnchorId) {
                return false;
            }
            View directChild = this.mAnchorView;
            for (ViewParent p = this.mAnchorView.getParent(); p != parent; p = p.getParent()) {
                if (p == null || p == forChild) {
                    this.mAnchorDirectChild = null;
                    this.mAnchorView = null;
                    return false;
                }
                if (p instanceof View) {
                    directChild = (View) p;
                }
            }
            this.mAnchorDirectChild = directChild;
            return true;
        }

        private boolean shouldDodge(View other, int layoutDirection) {
            LayoutParams lp = (LayoutParams) other.getLayoutParams();
            int absInset = GravityCompat.getAbsoluteGravity(lp.insetEdge, layoutDirection);
            return absInset != 0 && (GravityCompat.getAbsoluteGravity(this.dodgeInsetEdges, layoutDirection) & absInset) == absInset;
        }
    }

    private class HierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener {
        HierarchyChangeListener() {
        }

        @Override
        public void onChildViewAdded(View parent, View child) {
            if (CoordinatorLayout.this.mOnHierarchyChangeListener != null) {
                CoordinatorLayout.this.mOnHierarchyChangeListener.onChildViewAdded(parent, child);
            }
        }

        @Override
        public void onChildViewRemoved(View parent, View child) {
            CoordinatorLayout.this.onChildViewsChanged(2);
            if (CoordinatorLayout.this.mOnHierarchyChangeListener != null) {
                CoordinatorLayout.this.mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Parcelable savedState;
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        SparseArray<Parcelable> behaviorStates = ss.behaviorStates;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childId = child.getId();
            LayoutParams lp = getResolvedLayoutParams(child);
            Behavior b = lp.getBehavior();
            if (childId != -1 && b != null && (savedState = behaviorStates.get(childId)) != null) {
                b.onRestoreInstanceState(this, child, savedState);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable state;
        SavedState ss = new SavedState(super.onSaveInstanceState());
        SparseArray<Parcelable> behaviorStates = new SparseArray<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childId = child.getId();
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior b = lp.getBehavior();
            if (childId != -1 && b != null && (state = b.onSaveInstanceState(this, child)) != null) {
                behaviorStates.append(childId, state);
            }
        }
        ss.behaviorStates = behaviorStates;
        return ss;
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        Behavior behavior = lp.getBehavior();
        if (behavior != null && behavior.onRequestChildRectangleOnScreen(this, child, rectangle, immediate)) {
            return true;
        }
        return super.requestChildRectangleOnScreen(child, rectangle, immediate);
    }

    private void setupForInsets() {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        if (ViewCompat.getFitsSystemWindows(this)) {
            if (this.mApplyWindowInsetsListener == null) {
                this.mApplyWindowInsetsListener = new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                        return CoordinatorLayout.this.setWindowInsets(insets);
                    }
                };
            }
            ViewCompat.setOnApplyWindowInsetsListener(this, this.mApplyWindowInsetsListener);
            setSystemUiVisibility(1280);
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(this, null);
    }

    protected static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        SparseArray<Parcelable> behaviorStates;

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            int size = source.readInt();
            int[] ids = new int[size];
            source.readIntArray(ids);
            Parcelable[] states = source.readParcelableArray(loader);
            this.behaviorStates = new SparseArray<>(size);
            for (int i = 0; i < size; i++) {
                this.behaviorStates.append(ids[i], states[i]);
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            int size = this.behaviorStates != null ? this.behaviorStates.size() : 0;
            dest.writeInt(size);
            int[] ids = new int[size];
            Parcelable[] states = new Parcelable[size];
            for (int i = 0; i < size; i++) {
                ids[i] = this.behaviorStates.keyAt(i);
                states[i] = this.behaviorStates.valueAt(i);
            }
            dest.writeIntArray(ids);
            dest.writeParcelableArray(states, flags);
        }
    }
}
