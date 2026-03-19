package android.view;

import android.animation.LayoutTransition;
import android.bluetooth.mesh.MeshConstants;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.TtmlUtils;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.security.keystore.KeyProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pools;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.view.autofill.Helper;
import com.android.internal.R;
import com.mediatek.view.ViewDebugManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class ViewGroup extends View implements ViewParent, ViewManager {
    private static final int ARRAY_CAPACITY_INCREMENT = 12;
    private static final int ARRAY_INITIAL_CAPACITY = 12;
    private static final int CHILD_LEFT_INDEX = 0;
    private static final int CHILD_TOP_INDEX = 1;
    protected static final int CLIP_TO_PADDING_MASK = 34;
    private static final boolean DBG = false;
    private static final int FLAG_ADD_STATES_FROM_CHILDREN = 8192;

    @Deprecated
    private static final int FLAG_ALWAYS_DRAWN_WITH_CACHE = 16384;

    @Deprecated
    private static final int FLAG_ANIMATION_CACHE = 64;
    static final int FLAG_ANIMATION_DONE = 16;

    @Deprecated
    private static final int FLAG_CHILDREN_DRAWN_WITH_CACHE = 32768;
    static final int FLAG_CLEAR_TRANSFORMATION = 256;
    static final int FLAG_CLIP_CHILDREN = 1;
    private static final int FLAG_CLIP_TO_PADDING = 2;
    protected static final int FLAG_DISALLOW_INTERCEPT = 524288;
    static final int FLAG_INVALIDATE_REQUIRED = 4;
    static final int FLAG_IS_TRANSITION_GROUP = 16777216;
    static final int FLAG_IS_TRANSITION_GROUP_SET = 33554432;
    private static final int FLAG_LAYOUT_MODE_WAS_EXPLICITLY_SET = 8388608;
    private static final int FLAG_MASK_FOCUSABILITY = 393216;
    private static final int FLAG_NOTIFY_ANIMATION_LISTENER = 512;
    private static final int FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE = 65536;
    static final int FLAG_OPTIMIZE_INVALIDATE = 128;
    private static final int FLAG_PADDING_NOT_NULL = 32;
    private static final int FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW = 4194304;
    private static final int FLAG_RUN_ANIMATION = 8;
    private static final int FLAG_SHOW_CONTEXT_MENU_WITH_COORDS = 536870912;
    private static final int FLAG_SPLIT_MOTION_EVENTS = 2097152;
    private static final int FLAG_START_ACTION_MODE_FOR_CHILD_IS_NOT_TYPED = 268435456;
    private static final int FLAG_START_ACTION_MODE_FOR_CHILD_IS_TYPED = 134217728;
    protected static final int FLAG_SUPPORT_STATIC_TRANSFORMATIONS = 2048;
    static final int FLAG_TOUCHSCREEN_BLOCKS_FOCUS = 67108864;
    protected static final int FLAG_USE_CHILD_DRAWING_ORDER = 1024;
    public static final int FOCUS_AFTER_DESCENDANTS = 262144;
    public static final int FOCUS_BEFORE_DESCENDANTS = 131072;
    public static final int FOCUS_BLOCK_DESCENDANTS = 393216;
    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0;
    public static final int LAYOUT_MODE_OPTICAL_BOUNDS = 1;
    private static final int LAYOUT_MODE_UNDEFINED = -1;

    @Deprecated
    public static final int PERSISTENT_ALL_CACHES = 3;

    @Deprecated
    public static final int PERSISTENT_ANIMATION_CACHE = 1;

    @Deprecated
    public static final int PERSISTENT_NO_CACHE = 0;

    @Deprecated
    public static final int PERSISTENT_SCROLLING_CACHE = 2;
    private static final String TAG = "ViewGroup";
    private static float[] sDebugLines;
    private Animation.AnimationListener mAnimationListener;
    Paint mCachePaint;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private int mChildCountWithTransientState;
    private Transformation mChildTransformation;
    int mChildUnhandledKeyListeners;
    private View[] mChildren;
    private int mChildrenCount;
    private HashSet<View> mChildrenInterestedInDrag;
    private View mCurrentDragChild;
    private DragEvent mCurrentDragStartEvent;
    private View mDefaultFocus;
    protected ArrayList<View> mDisappearingChildren;
    private HoverTarget mFirstHoverTarget;
    private TouchTarget mFirstTouchTarget;
    private View mFocused;
    View mFocusedInCluster;

    @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 1, mask = 1, name = "CLIP_CHILDREN"), @ViewDebug.FlagToString(equals = 2, mask = 2, name = "CLIP_TO_PADDING"), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "PADDING_NOT_NULL")}, formatToHexString = true)
    protected int mGroupFlags;
    private boolean mHoveredSelf;
    RectF mInvalidateRegion;
    Transformation mInvalidationTransformation;
    private boolean mIsInterestedInDrag;

    @ViewDebug.ExportedProperty(category = "events")
    private int mLastTouchDownIndex;

    @ViewDebug.ExportedProperty(category = "events")
    private long mLastTouchDownTime;

    @ViewDebug.ExportedProperty(category = "events")
    private float mLastTouchDownX;

    @ViewDebug.ExportedProperty(category = "events")
    private float mLastTouchDownY;
    private LayoutAnimationController mLayoutAnimationController;
    private boolean mLayoutCalledWhileSuppressed;
    private int mLayoutMode;
    private LayoutTransition.TransitionListener mLayoutTransitionListener;
    private PointF mLocalPoint;
    private int mNestedScrollAxes;
    protected OnHierarchyChangeListener mOnHierarchyChangeListener;
    protected int mPersistentDrawingCache;
    private ArrayList<View> mPreSortedChildren;
    boolean mSuppressLayout;
    private float[] mTempPoint;
    private View mTooltipHoverTarget;
    private boolean mTooltipHoveredSelf;
    private List<Integer> mTransientIndices;
    private List<View> mTransientViews;
    private LayoutTransition mTransition;
    private ArrayList<View> mTransitioningViews;
    private ArrayList<View> mVisibilityChangingChildren;
    private static final int[] DESCENDANT_FOCUSABILITY_FLAGS = {131072, 262144, 393216};
    public static int LAYOUT_MODE_DEFAULT = 0;
    private static final ActionMode SENTINEL_ACTION_MODE = new ActionMode() {
        @Override
        public void setTitle(CharSequence charSequence) {
        }

        @Override
        public void setTitle(int i) {
        }

        @Override
        public void setSubtitle(CharSequence charSequence) {
        }

        @Override
        public void setSubtitle(int i) {
        }

        @Override
        public void setCustomView(View view) {
        }

        @Override
        public void invalidate() {
        }

        @Override
        public void finish() {
        }

        @Override
        public Menu getMenu() {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public CharSequence getSubtitle() {
            return null;
        }

        @Override
        public View getCustomView() {
            return null;
        }

        @Override
        public MenuInflater getMenuInflater() {
            return null;
        }
    };

    public interface OnHierarchyChangeListener {
        void onChildViewAdded(View view, View view2);

        void onChildViewRemoved(View view, View view2);
    }

    @Override
    protected abstract void onLayout(boolean z, int i, int i2, int i3, int i4);

    public ViewGroup(Context context) {
        this(context, null);
    }

    public ViewGroup(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ViewGroup(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ViewGroup(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mLastTouchDownIndex = -1;
        this.mLayoutMode = -1;
        this.mSuppressLayout = false;
        this.mLayoutCalledWhileSuppressed = false;
        this.mChildCountWithTransientState = 0;
        this.mTransientIndices = null;
        this.mTransientViews = null;
        this.mChildUnhandledKeyListeners = 0;
        this.mLayoutTransitionListener = new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i3) {
                if (i3 == 3) {
                    ViewGroup.this.startViewTransition(view);
                }
            }

            @Override
            public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i3) {
                if (ViewGroup.this.mLayoutCalledWhileSuppressed && !layoutTransition.isChangingLayout()) {
                    ViewGroup.this.requestLayout();
                    ViewGroup.this.mLayoutCalledWhileSuppressed = false;
                }
                if (i3 == 3 && ViewGroup.this.mTransitioningViews != null) {
                    ViewGroup.this.endViewTransition(view);
                }
            }
        };
        initViewGroup();
        initFromAttributes(context, attributeSet, i, i2);
    }

    private void initViewGroup() {
        if (!debugDraw()) {
            setFlags(128, 128);
        }
        this.mGroupFlags |= 1;
        this.mGroupFlags |= 2;
        this.mGroupFlags |= 16;
        this.mGroupFlags |= 64;
        this.mGroupFlags |= 16384;
        if (this.mContext.getApplicationInfo().targetSdkVersion >= 11) {
            this.mGroupFlags |= 2097152;
        }
        setDescendantFocusability(131072);
        this.mChildren = new View[12];
        this.mChildrenCount = 0;
        this.mPersistentDrawingCache = 2;
    }

    private void initFromAttributes(Context context, AttributeSet attributeSet, int i, int i2) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ViewGroup, i, i2);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i3 = 0; i3 < indexCount; i3++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i3);
            switch (index) {
                case 0:
                    setClipChildren(typedArrayObtainStyledAttributes.getBoolean(index, true));
                    break;
                case 1:
                    setClipToPadding(typedArrayObtainStyledAttributes.getBoolean(index, true));
                    break;
                case 2:
                    int resourceId = typedArrayObtainStyledAttributes.getResourceId(index, -1);
                    if (resourceId > 0) {
                        setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.mContext, resourceId));
                    }
                    break;
                case 3:
                    setAnimationCacheEnabled(typedArrayObtainStyledAttributes.getBoolean(index, true));
                    break;
                case 4:
                    setPersistentDrawingCache(typedArrayObtainStyledAttributes.getInt(index, 2));
                    break;
                case 5:
                    setAlwaysDrawnWithCacheEnabled(typedArrayObtainStyledAttributes.getBoolean(index, true));
                    break;
                case 6:
                    setAddStatesFromChildren(typedArrayObtainStyledAttributes.getBoolean(index, false));
                    break;
                case 7:
                    setDescendantFocusability(DESCENDANT_FOCUSABILITY_FLAGS[typedArrayObtainStyledAttributes.getInt(index, 0)]);
                    break;
                case 8:
                    setMotionEventSplittingEnabled(typedArrayObtainStyledAttributes.getBoolean(index, false));
                    break;
                case 9:
                    if (typedArrayObtainStyledAttributes.getBoolean(index, false)) {
                        setLayoutTransition(new LayoutTransition());
                    }
                    break;
                case 10:
                    setLayoutMode(typedArrayObtainStyledAttributes.getInt(index, -1));
                    break;
                case 11:
                    setTransitionGroup(typedArrayObtainStyledAttributes.getBoolean(index, false));
                    break;
                case 12:
                    setTouchscreenBlocksFocus(typedArrayObtainStyledAttributes.getBoolean(index, false));
                    break;
            }
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    @ViewDebug.ExportedProperty(category = "focus", mapping = {@ViewDebug.IntToString(from = 131072, to = "FOCUS_BEFORE_DESCENDANTS"), @ViewDebug.IntToString(from = 262144, to = "FOCUS_AFTER_DESCENDANTS"), @ViewDebug.IntToString(from = 393216, to = "FOCUS_BLOCK_DESCENDANTS")})
    public int getDescendantFocusability() {
        return this.mGroupFlags & 393216;
    }

    public void setDescendantFocusability(int i) {
        if (i != 131072 && i != 262144 && i != 393216) {
            throw new IllegalArgumentException("must be one of FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS");
        }
        this.mGroupFlags &= -393217;
        this.mGroupFlags = (i & 393216) | this.mGroupFlags;
    }

    @Override
    void handleFocusGainInternal(int i, Rect rect) {
        if (this.mFocused != null) {
            this.mFocused.unFocus(this);
            this.mFocused = null;
            this.mFocusedInCluster = null;
        }
        super.handleFocusGainInternal(i, rect);
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        if (ViewDebugManager.DBG) {
            System.out.println(this + " requestChildFocus()");
        }
        if (getDescendantFocusability() == 393216) {
            return;
        }
        super.unFocus(view2);
        if (this.mFocused != view) {
            if (this.mFocused != null) {
                this.mFocused.unFocus(view2);
            }
            this.mFocused = view;
        }
        if (this.mParent != null) {
            this.mParent.requestChildFocus(this, view2);
        }
    }

    void setDefaultFocus(View view) {
        if (this.mDefaultFocus != null && this.mDefaultFocus.isFocusedByDefault()) {
            return;
        }
        this.mDefaultFocus = view;
        if (this.mParent instanceof ViewGroup) {
            ((ViewGroup) this.mParent).setDefaultFocus(this);
        }
    }

    void clearDefaultFocus(View view) {
        if (this.mDefaultFocus != view && this.mDefaultFocus != null && this.mDefaultFocus.isFocusedByDefault()) {
            return;
        }
        this.mDefaultFocus = null;
        for (int i = 0; i < this.mChildrenCount; i++) {
            View view2 = this.mChildren[i];
            if (view2.isFocusedByDefault()) {
                this.mDefaultFocus = view2;
                return;
            }
            if (this.mDefaultFocus == null && view2.hasDefaultFocus()) {
                this.mDefaultFocus = view2;
            }
        }
        if (this.mParent instanceof ViewGroup) {
            ((ViewGroup) this.mParent).clearDefaultFocus(this);
        }
    }

    @Override
    boolean hasDefaultFocus() {
        return this.mDefaultFocus != null || super.hasDefaultFocus();
    }

    void clearFocusedInCluster(View view) {
        if (this.mFocusedInCluster != view) {
            return;
        }
        clearFocusedInCluster();
    }

    void clearFocusedInCluster() {
        View viewFindKeyboardNavigationCluster = findKeyboardNavigationCluster();
        ViewParent parent = this;
        do {
            ((ViewGroup) parent).mFocusedInCluster = null;
            if (parent != viewFindKeyboardNavigationCluster) {
                parent = parent.getParent();
            } else {
                return;
            }
        } while (parent instanceof ViewGroup);
    }

    @Override
    public void focusableViewAvailable(View view) {
        if (this.mParent != null && getDescendantFocusability() != 393216 && (this.mViewFlags & 12) == 0) {
            if (isFocusableInTouchMode() || !shouldBlockFocusForTouchscreen()) {
                if (!isFocused() || getDescendantFocusability() == 262144) {
                    this.mParent.focusableViewAvailable(view);
                }
            }
        }
    }

    @Override
    public boolean showContextMenuForChild(View view) {
        return (isShowingContextMenuWithCoords() || this.mParent == null || !this.mParent.showContextMenuForChild(view)) ? false : true;
    }

    public final boolean isShowingContextMenuWithCoords() {
        return (this.mGroupFlags & 536870912) != 0;
    }

    @Override
    public boolean showContextMenuForChild(View view, float f, float f2) {
        try {
            this.mGroupFlags |= 536870912;
            if (showContextMenuForChild(view)) {
                return true;
            }
            this.mGroupFlags = (-536870913) & this.mGroupFlags;
            return this.mParent != null && this.mParent.showContextMenuForChild(view, f, f2);
        } finally {
            this.mGroupFlags &= -536870913;
        }
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback) {
        if ((this.mGroupFlags & 134217728) == 0) {
            try {
                this.mGroupFlags |= 268435456;
                return startActionModeForChild(view, callback, 0);
            } finally {
                this.mGroupFlags &= -268435457;
            }
        }
        return SENTINEL_ACTION_MODE;
    }

    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback, int i) {
        if ((this.mGroupFlags & 268435456) == 0 && i == 0) {
            try {
                this.mGroupFlags |= 134217728;
                ActionMode actionModeStartActionModeForChild = startActionModeForChild(view, callback);
                this.mGroupFlags = (-134217729) & this.mGroupFlags;
                if (actionModeStartActionModeForChild != SENTINEL_ACTION_MODE) {
                    return actionModeStartActionModeForChild;
                }
            } catch (Throwable th) {
                this.mGroupFlags &= -134217729;
                throw th;
            }
        }
        if (this.mParent != null) {
            try {
                return this.mParent.startActionModeForChild(view, callback, i);
            } catch (AbstractMethodError e) {
                return this.mParent.startActionModeForChild(view, callback);
            }
        }
        return null;
    }

    @Override
    public boolean dispatchActivityResult(String str, int i, int i2, Intent intent) {
        if (super.dispatchActivityResult(str, i, i2, intent)) {
            return true;
        }
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            if (getChildAt(i3).dispatchActivityResult(str, i, i2, intent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View focusSearch(View view, int i) {
        if (isRootNamespace()) {
            return FocusFinder.getInstance().findNextFocus(this, view, i);
        }
        if (this.mParent != null) {
            return this.mParent.focusSearch(view, i);
        }
        return null;
    }

    @Override
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        return false;
    }

    @Override
    public boolean requestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        ViewParent viewParent = this.mParent;
        if (viewParent == null || !onRequestSendAccessibilityEvent(view, accessibilityEvent)) {
            return false;
        }
        return viewParent.requestSendAccessibilityEvent(this, accessibilityEvent);
    }

    public boolean onRequestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        if (this.mAccessibilityDelegate != null) {
            return this.mAccessibilityDelegate.onRequestSendAccessibilityEvent(this, view, accessibilityEvent);
        }
        return onRequestSendAccessibilityEventInternal(view, accessibilityEvent);
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        return true;
    }

    @Override
    public void childHasTransientStateChanged(View view, boolean z) {
        boolean zHasTransientState = hasTransientState();
        if (z) {
            this.mChildCountWithTransientState++;
        } else {
            this.mChildCountWithTransientState--;
        }
        boolean zHasTransientState2 = hasTransientState();
        if (this.mParent != null && zHasTransientState != zHasTransientState2) {
            try {
                this.mParent.childHasTransientStateChanged(this, zHasTransientState2);
            } catch (AbstractMethodError e) {
                Log.e(TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
            }
        }
    }

    @Override
    public boolean hasTransientState() {
        return this.mChildCountWithTransientState > 0 || super.hasTransientState();
    }

    @Override
    public boolean dispatchUnhandledMove(View view, int i) {
        return this.mFocused != null && this.mFocused.dispatchUnhandledMove(view, i);
    }

    @Override
    public void clearChildFocus(View view) {
        if (ViewDebugManager.DBG) {
            System.out.println(this + " clearChildFocus()");
        }
        this.mFocused = null;
        if (this.mParent != null) {
            this.mParent.clearChildFocus(this);
        }
    }

    @Override
    public void clearFocus() {
        if (ViewDebugManager.DBG) {
            System.out.println(this + " clearFocus()");
        }
        if (this.mFocused == null) {
            super.clearFocus();
            return;
        }
        View view = this.mFocused;
        this.mFocused = null;
        view.clearFocus();
    }

    @Override
    void unFocus(View view) {
        if (ViewDebugManager.DBG) {
            System.out.println(this + " unFocus()");
        }
        if (this.mFocused == null) {
            super.unFocus(view);
        } else {
            this.mFocused.unFocus(view);
            this.mFocused = null;
        }
    }

    public View getFocusedChild() {
        return this.mFocused;
    }

    View getDeepestFocusedChild() {
        View focusedChild = this;
        while (focusedChild != null) {
            if (focusedChild.isFocused()) {
                return focusedChild;
            }
            focusedChild = focusedChild instanceof ViewGroup ? ((ViewGroup) focusedChild).getFocusedChild() : null;
        }
        return null;
    }

    @Override
    public boolean hasFocus() {
        return ((this.mPrivateFlags & 2) == 0 && this.mFocused == null) ? false : true;
    }

    @Override
    public View findFocus() {
        if (ViewDebugManager.DBG) {
            System.out.println("Find focus in " + this + ": flags=" + isFocused() + ", child=" + this.mFocused);
        }
        if (isFocused()) {
            return this;
        }
        if (this.mFocused != null) {
            return this.mFocused.findFocus();
        }
        return null;
    }

    @Override
    boolean hasFocusable(boolean z, boolean z2) {
        if ((this.mViewFlags & 12) != 0) {
            return false;
        }
        if ((z || getFocusable() != 16) && isFocusable()) {
            return true;
        }
        if (getDescendantFocusability() != 393216) {
            return hasFocusableChild(z2);
        }
        return false;
    }

    boolean hasFocusableChild(boolean z) {
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if (!z || !view.hasExplicitFocusable()) {
                if (!z && view.hasFocusable()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        int size = arrayList.size();
        int descendantFocusability = getDescendantFocusability();
        boolean zShouldBlockFocusForTouchscreen = shouldBlockFocusForTouchscreen();
        boolean z = isFocusableInTouchMode() || !zShouldBlockFocusForTouchscreen;
        if (descendantFocusability == 393216) {
            if (z) {
                super.addFocusables(arrayList, i, i2);
                return;
            }
            return;
        }
        if (zShouldBlockFocusForTouchscreen) {
            i2 |= 1;
        }
        if (descendantFocusability == 131072 && z) {
            super.addFocusables(arrayList, i, i2);
        }
        View[] viewArr = new View[this.mChildrenCount];
        int i3 = 0;
        for (int i4 = 0; i4 < this.mChildrenCount; i4++) {
            View view = this.mChildren[i4];
            if ((view.mViewFlags & 12) == 0) {
                viewArr[i3] = view;
                i3++;
            }
        }
        FocusFinder.sort(viewArr, 0, i3, this, isLayoutRtl());
        for (int i5 = 0; i5 < i3; i5++) {
            viewArr[i5].addFocusables(arrayList, i, i2);
        }
        if (descendantFocusability == 262144 && z && size == arrayList.size()) {
            super.addFocusables(arrayList, i, i2);
        }
    }

    @Override
    public void addKeyboardNavigationClusters(Collection<View> collection, int i) {
        int size = collection.size();
        if (isKeyboardNavigationCluster()) {
            boolean touchscreenBlocksFocus = getTouchscreenBlocksFocus();
            try {
                setTouchscreenBlocksFocusNoRefocus(false);
                super.addKeyboardNavigationClusters(collection, i);
            } finally {
                setTouchscreenBlocksFocusNoRefocus(touchscreenBlocksFocus);
            }
        } else {
            super.addKeyboardNavigationClusters(collection, i);
        }
        if (size != collection.size() || getDescendantFocusability() == 393216) {
            return;
        }
        View[] viewArr = new View[this.mChildrenCount];
        int i2 = 0;
        for (int i3 = 0; i3 < this.mChildrenCount; i3++) {
            View view = this.mChildren[i3];
            if ((view.mViewFlags & 12) == 0) {
                viewArr[i2] = view;
                i2++;
            }
        }
        FocusFinder.sort(viewArr, 0, i2, this, isLayoutRtl());
        for (int i4 = 0; i4 < i2; i4++) {
            viewArr[i4].addKeyboardNavigationClusters(collection, i);
        }
    }

    public void setTouchscreenBlocksFocus(boolean z) {
        View viewFocusSearch;
        if (z) {
            this.mGroupFlags |= 67108864;
            if (hasFocus() && !isKeyboardNavigationCluster() && !getDeepestFocusedChild().isFocusableInTouchMode() && (viewFocusSearch = focusSearch(2)) != null) {
                viewFocusSearch.requestFocus();
                return;
            }
            return;
        }
        this.mGroupFlags &= -67108865;
    }

    private void setTouchscreenBlocksFocusNoRefocus(boolean z) {
        if (z) {
            this.mGroupFlags |= 67108864;
        } else {
            this.mGroupFlags &= -67108865;
        }
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public boolean getTouchscreenBlocksFocus() {
        return (this.mGroupFlags & 67108864) != 0;
    }

    boolean shouldBlockFocusForTouchscreen() {
        return getTouchscreenBlocksFocus() && this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) && (!isKeyboardNavigationCluster() || (!hasFocus() && findKeyboardNavigationCluster() == this));
    }

    @Override
    public void findViewsWithText(ArrayList<View> arrayList, CharSequence charSequence, int i) {
        super.findViewsWithText(arrayList, charSequence, i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            View view = viewArr[i3];
            if ((view.mViewFlags & 12) == 0 && (view.mPrivateFlags & 8) == 0) {
                view.findViewsWithText(arrayList, charSequence, i);
            }
        }
    }

    @Override
    public View findViewByAccessibilityIdTraversal(int i) {
        View viewFindViewByAccessibilityIdTraversal = super.findViewByAccessibilityIdTraversal(i);
        if (viewFindViewByAccessibilityIdTraversal != null) {
            return viewFindViewByAccessibilityIdTraversal;
        }
        if (getAccessibilityNodeProvider() != null) {
            return null;
        }
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            View viewFindViewByAccessibilityIdTraversal2 = viewArr[i3].findViewByAccessibilityIdTraversal(i);
            if (viewFindViewByAccessibilityIdTraversal2 != null) {
                return viewFindViewByAccessibilityIdTraversal2;
            }
        }
        return null;
    }

    @Override
    public View findViewByAutofillIdTraversal(int i) {
        View viewFindViewByAutofillIdTraversal = super.findViewByAutofillIdTraversal(i);
        if (viewFindViewByAutofillIdTraversal != null) {
            return viewFindViewByAutofillIdTraversal;
        }
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            View viewFindViewByAutofillIdTraversal2 = viewArr[i3].findViewByAutofillIdTraversal(i);
            if (viewFindViewByAutofillIdTraversal2 != null) {
                return viewFindViewByAutofillIdTraversal2;
            }
        }
        return null;
    }

    @Override
    public void dispatchWindowFocusChanged(boolean z) {
        super.dispatchWindowFocusChanged(z);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchWindowFocusChanged(z);
        }
    }

    @Override
    public void addTouchables(ArrayList<View> arrayList) {
        super.addTouchables(arrayList);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if ((view.mViewFlags & 12) == 0) {
                view.addTouchables(arrayList);
            }
        }
    }

    @Override
    public void makeOptionalFitsSystemWindows() {
        super.makeOptionalFitsSystemWindows();
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].makeOptionalFitsSystemWindows();
        }
    }

    @Override
    public void dispatchDisplayHint(int i) {
        super.dispatchDisplayHint(i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchDisplayHint(i);
        }
    }

    protected void onChildVisibilityChanged(View view, int i, int i2) {
        if (this.mTransition != null) {
            if (i2 == 0) {
                this.mTransition.showChild(this, view, i);
            } else {
                this.mTransition.hideChild(this, view, i2);
                if (this.mTransitioningViews != null && this.mTransitioningViews.contains(view)) {
                    if (this.mVisibilityChangingChildren == null) {
                        this.mVisibilityChangingChildren = new ArrayList<>();
                    }
                    this.mVisibilityChangingChildren.add(view);
                    addDisappearingView(view);
                }
            }
        }
        if (i2 == 0 && this.mCurrentDragStartEvent != null && !this.mChildrenInterestedInDrag.contains(view)) {
            notifyChildOfDragStart(view);
        }
    }

    @Override
    protected void dispatchVisibilityChanged(View view, int i) {
        super.dispatchVisibilityChanged(view, i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchVisibilityChanged(view, i);
        }
    }

    @Override
    public void dispatchWindowVisibilityChanged(int i) {
        super.dispatchWindowVisibilityChanged(i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchWindowVisibilityChanged(i);
        }
    }

    @Override
    boolean dispatchVisibilityAggregated(boolean z) {
        boolean zDispatchVisibilityAggregated = super.dispatchVisibilityAggregated(z);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (viewArr[i2].getVisibility() == 0) {
                viewArr[i2].dispatchVisibilityAggregated(zDispatchVisibilityAggregated);
            }
        }
        return zDispatchVisibilityAggregated;
    }

    @Override
    public void dispatchConfigurationChanged(Configuration configuration) {
        super.dispatchConfigurationChanged(configuration);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchConfigurationChanged(configuration);
        }
    }

    @Override
    public void recomputeViewAttributes(View view) {
        ViewParent viewParent;
        if (this.mAttachInfo == null || this.mAttachInfo.mRecomputeGlobalAttributes || (viewParent = this.mParent) == null) {
            return;
        }
        viewParent.recomputeViewAttributes(this);
    }

    @Override
    void dispatchCollectViewAttributes(View.AttachInfo attachInfo, int i) {
        if ((i & 12) == 0) {
            super.dispatchCollectViewAttributes(attachInfo, i);
            int i2 = this.mChildrenCount;
            View[] viewArr = this.mChildren;
            for (int i3 = 0; i3 < i2; i3++) {
                View view = viewArr[i3];
                view.dispatchCollectViewAttributes(attachInfo, (view.mViewFlags & 12) | i);
            }
        }
    }

    @Override
    public void bringChildToFront(View view) {
        int iIndexOfChild = indexOfChild(view);
        if (iIndexOfChild >= 0) {
            removeFromArray(iIndexOfChild);
            addInArray(view, this.mChildrenCount);
            view.mParent = this;
            requestLayout();
            invalidate();
        }
    }

    private PointF getLocalPoint() {
        if (this.mLocalPoint == null) {
            this.mLocalPoint = new PointF();
        }
        return this.mLocalPoint;
    }

    @Override
    boolean dispatchDragEnterExitInPreN(DragEvent dragEvent) {
        if (dragEvent.mAction == 6 && this.mCurrentDragChild != null) {
            this.mCurrentDragChild.dispatchDragEnterExitInPreN(dragEvent);
            this.mCurrentDragChild = null;
        }
        return this.mIsInterestedInDrag && super.dispatchDragEnterExitInPreN(dragEvent);
    }

    @Override
    public boolean dispatchDragEvent(DragEvent dragEvent) {
        boolean z;
        boolean z2;
        float f = dragEvent.mX;
        float f2 = dragEvent.mY;
        ClipData clipData = dragEvent.mClipData;
        PointF localPoint = getLocalPoint();
        switch (dragEvent.mAction) {
            case 1:
                this.mCurrentDragChild = null;
                this.mCurrentDragStartEvent = DragEvent.obtain(dragEvent);
                if (this.mChildrenInterestedInDrag == null) {
                    this.mChildrenInterestedInDrag = new HashSet<>();
                } else {
                    this.mChildrenInterestedInDrag.clear();
                }
                int i = this.mChildrenCount;
                View[] viewArr = this.mChildren;
                boolean z3 = false;
                for (int i2 = 0; i2 < i; i2++) {
                    View view = viewArr[i2];
                    view.mPrivateFlags2 &= -4;
                    if (view.getVisibility() == 0 && notifyChildOfDragStart(viewArr[i2])) {
                        z3 = true;
                    }
                }
                this.mIsInterestedInDrag = super.dispatchDragEvent(dragEvent);
                boolean z4 = this.mIsInterestedInDrag ? true : z3;
                if (!z4) {
                    this.mCurrentDragStartEvent.recycle();
                    this.mCurrentDragStartEvent = null;
                    return z4;
                }
                return z4;
            case 2:
            case 3:
                View viewFindFrontmostDroppableChildAt = findFrontmostDroppableChildAt(dragEvent.mX, dragEvent.mY, localPoint);
                if (viewFindFrontmostDroppableChildAt != this.mCurrentDragChild) {
                    if (sCascadedDragDrop) {
                        int i3 = dragEvent.mAction;
                        dragEvent.mX = 0.0f;
                        dragEvent.mY = 0.0f;
                        dragEvent.mClipData = null;
                        if (this.mCurrentDragChild != null) {
                            dragEvent.mAction = 6;
                            this.mCurrentDragChild.dispatchDragEnterExitInPreN(dragEvent);
                        }
                        if (viewFindFrontmostDroppableChildAt != null) {
                            dragEvent.mAction = 5;
                            viewFindFrontmostDroppableChildAt.dispatchDragEnterExitInPreN(dragEvent);
                        }
                        dragEvent.mAction = i3;
                        dragEvent.mX = f;
                        dragEvent.mY = f2;
                        dragEvent.mClipData = clipData;
                    }
                    this.mCurrentDragChild = viewFindFrontmostDroppableChildAt;
                }
                if (viewFindFrontmostDroppableChildAt == null && this.mIsInterestedInDrag) {
                    viewFindFrontmostDroppableChildAt = this;
                }
                if (viewFindFrontmostDroppableChildAt == null) {
                    return false;
                }
                if (viewFindFrontmostDroppableChildAt != this) {
                    dragEvent.mX = localPoint.x;
                    dragEvent.mY = localPoint.y;
                    boolean zDispatchDragEvent = viewFindFrontmostDroppableChildAt.dispatchDragEvent(dragEvent);
                    dragEvent.mX = f;
                    dragEvent.mY = f2;
                    if (this.mIsInterestedInDrag) {
                        if (!sCascadedDragDrop) {
                            z = dragEvent.mEventHandlerWasCalled;
                        } else {
                            z = zDispatchDragEvent;
                        }
                        if (!z) {
                            return super.dispatchDragEvent(dragEvent);
                        }
                        return zDispatchDragEvent;
                    }
                    return zDispatchDragEvent;
                }
                return super.dispatchDragEvent(dragEvent);
            case 4:
                HashSet<View> hashSet = this.mChildrenInterestedInDrag;
                if (hashSet != null) {
                    Iterator<View> it = hashSet.iterator();
                    z2 = false;
                    while (it.hasNext()) {
                        if (it.next().dispatchDragEvent(dragEvent)) {
                            z2 = true;
                        }
                    }
                    hashSet.clear();
                } else {
                    z2 = false;
                }
                if (this.mCurrentDragStartEvent != null) {
                    this.mCurrentDragStartEvent.recycle();
                    this.mCurrentDragStartEvent = null;
                }
                if (this.mIsInterestedInDrag) {
                    if (super.dispatchDragEvent(dragEvent)) {
                        z2 = true;
                    }
                    this.mIsInterestedInDrag = false;
                }
                return z2;
            default:
                return false;
        }
    }

    View findFrontmostDroppableChildAt(float f, float f2, PointF pointF) {
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = i - 1; i2 >= 0; i2--) {
            View view = viewArr[i2];
            if (view.canAcceptDrag() && isTransformedTouchPointInView(f, f2, view, pointF)) {
                return view;
            }
        }
        return null;
    }

    boolean notifyChildOfDragStart(View view) {
        float f = this.mCurrentDragStartEvent.mX;
        float f2 = this.mCurrentDragStartEvent.mY;
        float[] tempPoint = getTempPoint();
        tempPoint[0] = f;
        tempPoint[1] = f2;
        transformPointToViewLocal(tempPoint, view);
        this.mCurrentDragStartEvent.mX = tempPoint[0];
        this.mCurrentDragStartEvent.mY = tempPoint[1];
        boolean zDispatchDragEvent = view.dispatchDragEvent(this.mCurrentDragStartEvent);
        this.mCurrentDragStartEvent.mX = f;
        this.mCurrentDragStartEvent.mY = f2;
        this.mCurrentDragStartEvent.mEventHandlerWasCalled = false;
        if (zDispatchDragEvent) {
            this.mChildrenInterestedInDrag.add(view);
            if (!view.canAcceptDrag()) {
                view.mPrivateFlags2 |= 1;
                view.refreshDrawableState();
            }
        }
        return zDispatchDragEvent;
    }

    @Override
    public void dispatchWindowSystemUiVisiblityChanged(int i) {
        super.dispatchWindowSystemUiVisiblityChanged(i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchWindowSystemUiVisiblityChanged(i);
        }
    }

    @Override
    public void dispatchSystemUiVisibilityChanged(int i) {
        super.dispatchSystemUiVisibilityChanged(i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchSystemUiVisibilityChanged(i);
        }
    }

    @Override
    boolean updateLocalSystemUiVisibility(int i, int i2) {
        boolean zUpdateLocalSystemUiVisibility = super.updateLocalSystemUiVisibility(i, i2);
        int i3 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i4 = 0; i4 < i3; i4++) {
            zUpdateLocalSystemUiVisibility |= viewArr[i4].updateLocalSystemUiVisibility(i, i2);
        }
        return zUpdateLocalSystemUiVisibility;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        if ((this.mPrivateFlags & 18) == 18) {
            return super.dispatchKeyEventPreIme(keyEvent);
        }
        if (this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16) {
            return this.mFocused.dispatchKeyEventPreIme(keyEvent);
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onKeyEvent(keyEvent, 1);
        }
        if ((this.mPrivateFlags & 18) == 18) {
            if (super.dispatchKeyEvent(keyEvent)) {
                return true;
            }
        } else if (this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16) {
            if (ViewDebugManager.DEBUG_KEY) {
                Log.d(TAG, "dispatchKeyEvent to focus child event = " + keyEvent + ", mFocused = " + this.mFocused + ",this = " + this);
            }
            if (this.mFocused.dispatchKeyEvent(keyEvent)) {
                return true;
            }
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(keyEvent, 1);
            return false;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        if ((this.mPrivateFlags & 18) == 18) {
            return super.dispatchKeyShortcutEvent(keyEvent);
        }
        if (this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16) {
            return this.mFocused.dispatchKeyShortcutEvent(keyEvent);
        }
        return false;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTrackballEvent(motionEvent, 1);
        }
        if ((this.mPrivateFlags & 18) == 18) {
            if (super.dispatchTrackballEvent(motionEvent)) {
                return true;
            }
        } else if (this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16 && this.mFocused.dispatchTrackballEvent(motionEvent)) {
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 1);
            return false;
        }
        return false;
    }

    @Override
    public boolean dispatchCapturedPointerEvent(MotionEvent motionEvent) {
        return (this.mPrivateFlags & 18) == 18 ? super.dispatchCapturedPointerEvent(motionEvent) : this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16 && this.mFocused.dispatchCapturedPointerEvent(motionEvent);
    }

    @Override
    public void dispatchPointerCaptureChanged(boolean z) {
        exitHoverTargets();
        super.dispatchPointerCaptureChanged(z);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchPointerCaptureChanged(z);
        }
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        boolean z;
        PointerIcon pointerIconDispatchResolvePointerIcon;
        float x = motionEvent.getX(i);
        float y = motionEvent.getY(i);
        if (isOnScrollbarThumb(x, y) || isDraggingScrollBar()) {
            return PointerIcon.getSystemIcon(this.mContext, 1000);
        }
        int i2 = this.mChildrenCount;
        if (i2 != 0) {
            ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
            if (arrayListBuildOrderedChildList != null || !isChildrenDrawingOrderEnabled()) {
                z = false;
            } else {
                z = true;
            }
            View[] viewArr = this.mChildren;
            for (int i3 = i2 - 1; i3 >= 0; i3--) {
                View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i2, i3, z));
                if (canViewReceivePointerEvents(andVerifyPreorderedView) && isTransformedTouchPointInView(x, y, andVerifyPreorderedView, null) && (pointerIconDispatchResolvePointerIcon = dispatchResolvePointerIcon(motionEvent, i, andVerifyPreorderedView)) != null) {
                    if (arrayListBuildOrderedChildList != null) {
                        arrayListBuildOrderedChildList.clear();
                    }
                    return pointerIconDispatchResolvePointerIcon;
                }
            }
            if (arrayListBuildOrderedChildList != null) {
                arrayListBuildOrderedChildList.clear();
            }
        }
        return super.onResolvePointerIcon(motionEvent, i);
    }

    private PointerIcon dispatchResolvePointerIcon(MotionEvent motionEvent, int i, View view) {
        if (!view.hasIdentityMatrix()) {
            MotionEvent transformedMotionEvent = getTransformedMotionEvent(motionEvent, view);
            PointerIcon pointerIconOnResolvePointerIcon = view.onResolvePointerIcon(transformedMotionEvent, i);
            transformedMotionEvent.recycle();
            return pointerIconOnResolvePointerIcon;
        }
        float f = this.mScrollX - view.mLeft;
        float f2 = this.mScrollY - view.mTop;
        motionEvent.offsetLocation(f, f2);
        PointerIcon pointerIconOnResolvePointerIcon2 = view.onResolvePointerIcon(motionEvent, i);
        motionEvent.offsetLocation(-f, -f2);
        return pointerIconOnResolvePointerIcon2;
    }

    private int getAndVerifyPreorderedIndex(int i, int i2, boolean z) {
        if (z && (i2 = getChildDrawingOrder(i, i2)) >= i) {
            throw new IndexOutOfBoundsException("getChildDrawingOrder() returned invalid index " + i2 + " (child count is " + i + ")");
        }
        return i2;
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent motionEvent) {
        MotionEvent motionEventObtainMotionEventNoHistoryOrSelf;
        boolean zDispatchHoverEvent;
        boolean z;
        int action = motionEvent.getAction();
        boolean zOnInterceptHoverEvent = onInterceptHoverEvent(motionEvent);
        motionEvent.setAction(action);
        HoverTarget hoverTarget = this.mFirstHoverTarget;
        PointF pointF = null;
        this.mFirstHoverTarget = null;
        if (!zOnInterceptHoverEvent && action != 10) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            int i = this.mChildrenCount;
            if (i != 0) {
                ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
                boolean z2 = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
                View[] viewArr = this.mChildren;
                motionEventObtainMotionEventNoHistoryOrSelf = motionEvent;
                HoverTarget hoverTarget2 = null;
                boolean zDispatchTransformedGenericPointerEvent = false;
                HoverTarget hoverTarget3 = hoverTarget;
                for (int i2 = i - 1; i2 >= 0; i2--) {
                    View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i, i2, z2));
                    if (canViewReceivePointerEvents(andVerifyPreorderedView) && isTransformedTouchPointInView(x, y, andVerifyPreorderedView, pointF)) {
                        HoverTarget hoverTarget4 = pointF;
                        HoverTarget hoverTargetObtain = hoverTarget3;
                        while (true) {
                            if (hoverTargetObtain == null) {
                                hoverTargetObtain = HoverTarget.obtain(andVerifyPreorderedView);
                                z = false;
                                break;
                            }
                            if (hoverTargetObtain.child == andVerifyPreorderedView) {
                                if (hoverTarget4 != 0) {
                                    hoverTarget4.next = hoverTargetObtain.next;
                                } else {
                                    hoverTarget3 = hoverTargetObtain.next;
                                }
                                pointF = null;
                                hoverTargetObtain.next = null;
                                z = true;
                            } else {
                                pointF = null;
                                hoverTarget4 = hoverTargetObtain;
                                hoverTargetObtain = hoverTargetObtain.next;
                            }
                        }
                        if (hoverTarget2 != null) {
                            hoverTarget2.next = hoverTargetObtain;
                        } else {
                            this.mFirstHoverTarget = hoverTargetObtain;
                        }
                        if (action == 9) {
                            if (!z) {
                                zDispatchTransformedGenericPointerEvent |= dispatchTransformedGenericPointerEvent(motionEvent, andVerifyPreorderedView);
                            }
                        } else if (action == 7) {
                            if (!z) {
                                motionEventObtainMotionEventNoHistoryOrSelf = obtainMotionEventNoHistoryOrSelf(motionEventObtainMotionEventNoHistoryOrSelf);
                                motionEventObtainMotionEventNoHistoryOrSelf.setAction(9);
                                boolean zDispatchTransformedGenericPointerEvent2 = zDispatchTransformedGenericPointerEvent | dispatchTransformedGenericPointerEvent(motionEventObtainMotionEventNoHistoryOrSelf, andVerifyPreorderedView);
                                motionEventObtainMotionEventNoHistoryOrSelf.setAction(action);
                                zDispatchTransformedGenericPointerEvent = zDispatchTransformedGenericPointerEvent2 | dispatchTransformedGenericPointerEvent(motionEventObtainMotionEventNoHistoryOrSelf, andVerifyPreorderedView);
                            } else {
                                zDispatchTransformedGenericPointerEvent |= dispatchTransformedGenericPointerEvent(motionEvent, andVerifyPreorderedView);
                            }
                        }
                        if (zDispatchTransformedGenericPointerEvent) {
                            break;
                        }
                        hoverTarget2 = hoverTargetObtain;
                    }
                }
                hoverTarget = hoverTarget3;
                zDispatchHoverEvent = zDispatchTransformedGenericPointerEvent;
                if (arrayListBuildOrderedChildList != null) {
                    arrayListBuildOrderedChildList.clear();
                }
            }
        } else {
            motionEventObtainMotionEventNoHistoryOrSelf = motionEvent;
            zDispatchHoverEvent = false;
        }
        while (hoverTarget != null) {
            View view = hoverTarget.child;
            if (action == 10) {
                zDispatchHoverEvent = dispatchTransformedGenericPointerEvent(motionEvent, view) | zDispatchHoverEvent;
            } else {
                if (action == 7) {
                    boolean zIsHoverExitPending = motionEvent.isHoverExitPending();
                    motionEvent.setHoverExitPending(true);
                    dispatchTransformedGenericPointerEvent(motionEvent, view);
                    motionEvent.setHoverExitPending(zIsHoverExitPending);
                }
                MotionEvent motionEventObtainMotionEventNoHistoryOrSelf2 = obtainMotionEventNoHistoryOrSelf(motionEventObtainMotionEventNoHistoryOrSelf);
                motionEventObtainMotionEventNoHistoryOrSelf2.setAction(10);
                dispatchTransformedGenericPointerEvent(motionEventObtainMotionEventNoHistoryOrSelf2, view);
                motionEventObtainMotionEventNoHistoryOrSelf2.setAction(action);
                motionEventObtainMotionEventNoHistoryOrSelf = motionEventObtainMotionEventNoHistoryOrSelf2;
            }
            HoverTarget hoverTarget5 = hoverTarget.next;
            hoverTarget.recycle();
            hoverTarget = hoverTarget5;
        }
        boolean z3 = (zDispatchHoverEvent || action == 10 || motionEvent.isHoverExitPending()) ? false : true;
        if (z3 == this.mHoveredSelf) {
            if (z3) {
                zDispatchHoverEvent |= super.dispatchHoverEvent(motionEvent);
            }
        } else {
            if (this.mHoveredSelf) {
                if (action == 10) {
                    zDispatchHoverEvent = super.dispatchHoverEvent(motionEvent) | zDispatchHoverEvent;
                } else {
                    if (action == 7) {
                        super.dispatchHoverEvent(motionEvent);
                    }
                    MotionEvent motionEventObtainMotionEventNoHistoryOrSelf3 = obtainMotionEventNoHistoryOrSelf(motionEventObtainMotionEventNoHistoryOrSelf);
                    motionEventObtainMotionEventNoHistoryOrSelf3.setAction(10);
                    super.dispatchHoverEvent(motionEventObtainMotionEventNoHistoryOrSelf3);
                    motionEventObtainMotionEventNoHistoryOrSelf3.setAction(action);
                    motionEventObtainMotionEventNoHistoryOrSelf = motionEventObtainMotionEventNoHistoryOrSelf3;
                }
                this.mHoveredSelf = false;
            }
            if (z3) {
                if (action == 9) {
                    zDispatchHoverEvent |= super.dispatchHoverEvent(motionEvent);
                    this.mHoveredSelf = true;
                } else if (action == 7) {
                    motionEventObtainMotionEventNoHistoryOrSelf = obtainMotionEventNoHistoryOrSelf(motionEventObtainMotionEventNoHistoryOrSelf);
                    motionEventObtainMotionEventNoHistoryOrSelf.setAction(9);
                    boolean zDispatchHoverEvent2 = super.dispatchHoverEvent(motionEventObtainMotionEventNoHistoryOrSelf) | zDispatchHoverEvent;
                    motionEventObtainMotionEventNoHistoryOrSelf.setAction(action);
                    zDispatchHoverEvent = zDispatchHoverEvent2 | super.dispatchHoverEvent(motionEventObtainMotionEventNoHistoryOrSelf);
                    this.mHoveredSelf = true;
                }
            }
        }
        if (motionEventObtainMotionEventNoHistoryOrSelf != motionEvent) {
            motionEventObtainMotionEventNoHistoryOrSelf.recycle();
        }
        return zDispatchHoverEvent;
    }

    private void exitHoverTargets() {
        if (this.mHoveredSelf || this.mFirstHoverTarget != null) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 10, 0.0f, 0.0f, 0);
            motionEventObtain.setSource(4098);
            dispatchHoverEvent(motionEventObtain);
            motionEventObtain.recycle();
        }
    }

    private void cancelHoverTarget(View view) {
        HoverTarget hoverTarget = this.mFirstHoverTarget;
        HoverTarget hoverTarget2 = null;
        while (hoverTarget != null) {
            HoverTarget hoverTarget3 = hoverTarget.next;
            if (hoverTarget.child != view) {
                hoverTarget2 = hoverTarget;
                hoverTarget = hoverTarget3;
            } else {
                if (hoverTarget2 == null) {
                    this.mFirstHoverTarget = hoverTarget3;
                } else {
                    hoverTarget2.next = hoverTarget3;
                }
                hoverTarget.recycle();
                long jUptimeMillis = SystemClock.uptimeMillis();
                MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 10, 0.0f, 0.0f, 0);
                motionEventObtain.setSource(4098);
                view.dispatchHoverEvent(motionEventObtain);
                motionEventObtain.recycle();
                return;
            }
        }
    }

    @Override
    boolean dispatchTooltipHoverEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        View view = null;
        if (action == 7) {
            int i = this.mChildrenCount;
            if (i != 0) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
                boolean z = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
                View[] viewArr = this.mChildren;
                int i2 = i - 1;
                while (true) {
                    if (i2 < 0) {
                        break;
                    }
                    View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i, i2, z));
                    if (canViewReceivePointerEvents(andVerifyPreorderedView) && isTransformedTouchPointInView(x, y, andVerifyPreorderedView, null) && dispatchTooltipHoverEvent(motionEvent, andVerifyPreorderedView)) {
                        view = andVerifyPreorderedView;
                        break;
                    }
                    i2--;
                }
                if (arrayListBuildOrderedChildList != null) {
                    arrayListBuildOrderedChildList.clear();
                }
            }
            if (this.mTooltipHoverTarget != view) {
                if (this.mTooltipHoverTarget != null) {
                    motionEvent.setAction(10);
                    this.mTooltipHoverTarget.dispatchTooltipHoverEvent(motionEvent);
                    motionEvent.setAction(action);
                }
                this.mTooltipHoverTarget = view;
            }
            if (this.mTooltipHoverTarget != null) {
                if (this.mTooltipHoveredSelf) {
                    this.mTooltipHoveredSelf = false;
                    motionEvent.setAction(10);
                    super.dispatchTooltipHoverEvent(motionEvent);
                    motionEvent.setAction(action);
                }
                return true;
            }
            this.mTooltipHoveredSelf = super.dispatchTooltipHoverEvent(motionEvent);
            return this.mTooltipHoveredSelf;
        }
        switch (action) {
            case 10:
                if (this.mTooltipHoverTarget != null) {
                    this.mTooltipHoverTarget.dispatchTooltipHoverEvent(motionEvent);
                    this.mTooltipHoverTarget = null;
                    break;
                } else {
                    if (this.mTooltipHoveredSelf) {
                        super.dispatchTooltipHoverEvent(motionEvent);
                        this.mTooltipHoveredSelf = false;
                    }
                    break;
                }
            case 9:
            default:
                return false;
        }
    }

    private boolean dispatchTooltipHoverEvent(MotionEvent motionEvent, View view) {
        if (!view.hasIdentityMatrix()) {
            MotionEvent transformedMotionEvent = getTransformedMotionEvent(motionEvent, view);
            boolean zDispatchTooltipHoverEvent = view.dispatchTooltipHoverEvent(transformedMotionEvent);
            transformedMotionEvent.recycle();
            return zDispatchTooltipHoverEvent;
        }
        float f = this.mScrollX - view.mLeft;
        float f2 = this.mScrollY - view.mTop;
        motionEvent.offsetLocation(f, f2);
        boolean zDispatchTooltipHoverEvent2 = view.dispatchTooltipHoverEvent(motionEvent);
        motionEvent.offsetLocation(-f, -f2);
        return zDispatchTooltipHoverEvent2;
    }

    private void exitTooltipHoverTargets() {
        if (this.mTooltipHoveredSelf || this.mTooltipHoverTarget != null) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 10, 0.0f, 0.0f, 0);
            motionEventObtain.setSource(4098);
            dispatchTooltipHoverEvent(motionEventObtain);
            motionEventObtain.recycle();
        }
    }

    @Override
    protected boolean hasHoveredChild() {
        return this.mFirstHoverTarget != null;
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> arrayList) {
        if (getAccessibilityNodeProvider() != null) {
            return;
        }
        ChildListForAccessibility childListForAccessibilityObtain = ChildListForAccessibility.obtain(this, true);
        try {
            int childCount = childListForAccessibilityObtain.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = childListForAccessibilityObtain.getChildAt(i);
                if ((childAt.mViewFlags & 12) == 0) {
                    if (childAt.includeForAccessibility()) {
                        arrayList.add(childAt);
                    } else {
                        childAt.addChildrenForAccessibility(arrayList);
                    }
                }
            }
        } finally {
            childListForAccessibilityObtain.recycle();
        }
    }

    public boolean onInterceptHoverEvent(MotionEvent motionEvent) {
        if (motionEvent.isFromSource(8194)) {
            int action = motionEvent.getAction();
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if ((action == 7 || action == 9) && isOnScrollbar(x, y)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static MotionEvent obtainMotionEventNoHistoryOrSelf(MotionEvent motionEvent) {
        if (motionEvent.getHistorySize() == 0) {
            return motionEvent;
        }
        return MotionEvent.obtainNoHistory(motionEvent);
    }

    @Override
    protected boolean dispatchGenericPointerEvent(MotionEvent motionEvent) {
        boolean z;
        int i = this.mChildrenCount;
        if (i != 0) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
            if (arrayListBuildOrderedChildList != null || !isChildrenDrawingOrderEnabled()) {
                z = false;
            } else {
                z = true;
            }
            View[] viewArr = this.mChildren;
            for (int i2 = i - 1; i2 >= 0; i2--) {
                View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i, i2, z));
                if (canViewReceivePointerEvents(andVerifyPreorderedView) && isTransformedTouchPointInView(x, y, andVerifyPreorderedView, null) && dispatchTransformedGenericPointerEvent(motionEvent, andVerifyPreorderedView)) {
                    if (arrayListBuildOrderedChildList != null) {
                        arrayListBuildOrderedChildList.clear();
                    }
                    return true;
                }
            }
            if (arrayListBuildOrderedChildList != null) {
                arrayListBuildOrderedChildList.clear();
            }
        }
        return super.dispatchGenericPointerEvent(motionEvent);
    }

    @Override
    protected boolean dispatchGenericFocusedEvent(MotionEvent motionEvent) {
        if ((this.mPrivateFlags & 18) == 18) {
            return super.dispatchGenericFocusedEvent(motionEvent);
        }
        if (this.mFocused != null && (this.mFocused.mPrivateFlags & 16) == 16) {
            return this.mFocused.dispatchGenericMotionEvent(motionEvent);
        }
        return false;
    }

    private boolean dispatchTransformedGenericPointerEvent(MotionEvent motionEvent, View view) {
        if (!view.hasIdentityMatrix()) {
            MotionEvent transformedMotionEvent = getTransformedMotionEvent(motionEvent, view);
            boolean zDispatchGenericMotionEvent = view.dispatchGenericMotionEvent(transformedMotionEvent);
            transformedMotionEvent.recycle();
            return zDispatchGenericMotionEvent;
        }
        float f = this.mScrollX - view.mLeft;
        float f2 = this.mScrollY - view.mTop;
        motionEvent.offsetLocation(f, f2);
        boolean zDispatchGenericMotionEvent2 = view.dispatchGenericMotionEvent(motionEvent);
        motionEvent.offsetLocation(-f, -f2);
        return zDispatchGenericMotionEvent2;
    }

    private MotionEvent getTransformedMotionEvent(MotionEvent motionEvent, View view) {
        float f = this.mScrollX - view.mLeft;
        float f2 = this.mScrollY - view.mTop;
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
        motionEventObtain.offsetLocation(f, f2);
        if (!view.hasIdentityMatrix()) {
            motionEventObtain.transform(view.getInverseMatrix());
        }
        return motionEventObtain;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean zOnInterceptTouchEvent;
        boolean z;
        boolean z2;
        boolean z3;
        TouchTarget touchTarget;
        boolean zDispatchTransformedTouchEvent;
        boolean z4;
        View view;
        float f;
        float f2;
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(motionEvent, 1);
        }
        boolean z5 = false;
        if (motionEvent.isTargetAccessibilityFocus() && isAccessibilityFocusedViewOrHost()) {
            motionEvent.setTargetAccessibilityFocus(false);
        }
        if (ViewDebugManager.DEBUG_MOTION) {
            Log.d(TAG, "(ViewGroup)dispatchTouchEvent 1: ev = " + motionEvent + ",mFirstTouchTarget = " + this.mFirstTouchTarget + ",this = " + this);
        }
        if (onFilterTouchEventForSecurity(motionEvent)) {
            int action = motionEvent.getAction();
            int i = action & 255;
            if (i == 0) {
                cancelAndClearTouchTargets(motionEvent);
                resetTouchState();
            }
            if (i == 0 || this.mFirstTouchTarget != null) {
                if ((this.mGroupFlags & 524288) != 0) {
                    zOnInterceptTouchEvent = false;
                } else {
                    zOnInterceptTouchEvent = onInterceptTouchEvent(motionEvent);
                    if (zOnInterceptTouchEvent && ViewDebugManager.DEBUG_TOUCH) {
                        Log.d(TAG, "Touch event was intercepted event = " + motionEvent + ",this = " + this);
                    }
                    motionEvent.setAction(action);
                }
            } else {
                zOnInterceptTouchEvent = true;
            }
            if (zOnInterceptTouchEvent || this.mFirstTouchTarget != null) {
                motionEvent.setTargetAccessibilityFocus(false);
            }
            boolean z6 = resetCancelNextUpFlag(this) || i == 3;
            boolean z7 = (this.mGroupFlags & 2097152) != 0;
            if (z6 || zOnInterceptTouchEvent) {
                z = z7;
                z2 = false;
                z3 = false;
                touchTarget = null;
                if (this.mFirstTouchTarget != null) {
                    zDispatchTransformedTouchEvent = dispatchTransformedTouchEvent(motionEvent, z6, null, -1);
                } else {
                    TouchTarget touchTarget2 = this.mFirstTouchTarget;
                    TouchTarget touchTarget3 = null;
                    zDispatchTransformedTouchEvent = z2;
                    while (touchTarget2 != null) {
                        TouchTarget touchTarget4 = touchTarget2.next;
                        if (z3 && touchTarget2 == touchTarget) {
                            zDispatchTransformedTouchEvent = true;
                        } else {
                            boolean z8 = (resetCancelNextUpFlag(touchTarget2.child) || zOnInterceptTouchEvent) ? true : z2;
                            if (dispatchTransformedTouchEvent(motionEvent, z8, touchTarget2.child, touchTarget2.pointerIdBits)) {
                                zDispatchTransformedTouchEvent = true;
                            }
                            if (ViewDebugManager.DEBUG_MOTION) {
                                Log.d(TAG, "dispatchTouchEvent middle 5: cancelChild = " + z8 + ",mFirstTouchTarget = " + this.mFirstTouchTarget + ",target = " + touchTarget2 + ",predecessor = " + touchTarget3 + ",next = " + touchTarget4 + ",this = " + this);
                            }
                            if (z8) {
                                if (touchTarget3 == null) {
                                    this.mFirstTouchTarget = touchTarget4;
                                } else {
                                    touchTarget3.next = touchTarget4;
                                }
                                touchTarget2.recycle();
                            }
                            touchTarget2 = touchTarget4;
                        }
                        touchTarget3 = touchTarget2;
                        touchTarget2 = touchTarget4;
                    }
                }
                z5 = zDispatchTransformedTouchEvent;
                if (!z6 || i == 1 || i == 7) {
                    resetTouchState();
                } else if (z && i == 6) {
                    removePointersFromTouchTargets(1 << motionEvent.getPointerId(motionEvent.getActionIndex()));
                }
            } else {
                View viewFindChildWithAccessibilityFocus = motionEvent.isTargetAccessibilityFocus() ? findChildWithAccessibilityFocus() : null;
                if (i == 0 || ((z7 && i == 5) || i == 7)) {
                    int actionIndex = motionEvent.getActionIndex();
                    int pointerId = z7 ? 1 << motionEvent.getPointerId(actionIndex) : -1;
                    removePointersFromTouchTargets(pointerId);
                    int i2 = this.mChildrenCount;
                    if (i2 != 0) {
                        float x = motionEvent.getX(actionIndex);
                        float y = motionEvent.getY(actionIndex);
                        ArrayList<View> arrayListBuildTouchDispatchChildList = buildTouchDispatchChildList();
                        boolean z9 = arrayListBuildTouchDispatchChildList == null && isChildrenDrawingOrderEnabled();
                        View[] viewArr = this.mChildren;
                        int i3 = i2 - 1;
                        View view2 = viewFindChildWithAccessibilityFocus;
                        int i4 = i3;
                        TouchTarget touchTargetAddTouchTarget = null;
                        while (true) {
                            if (i4 < 0) {
                                z = z7;
                                z2 = false;
                                z4 = false;
                                break;
                            }
                            int andVerifyPreorderedIndex = getAndVerifyPreorderedIndex(i2, i4, z9);
                            boolean z10 = z9;
                            View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildTouchDispatchChildList, viewArr, andVerifyPreorderedIndex);
                            if (view2 == null) {
                                if (canViewReceivePointerEvents(andVerifyPreorderedView)) {
                                    view = view2;
                                } else {
                                    view = view2;
                                    if (isTransformedTouchPointInView(x, y, andVerifyPreorderedView, null)) {
                                        TouchTarget touchTarget5 = getTouchTarget(andVerifyPreorderedView);
                                        if (ViewDebugManager.DEBUG_MOTION) {
                                            f = y;
                                            f2 = x;
                                            StringBuilder sb = new StringBuilder();
                                            z = z7;
                                            sb.append("(ViewGroup)dispatchTouchEvent to child 3: child = ");
                                            sb.append(andVerifyPreorderedView);
                                            sb.append(",childrenCount = ");
                                            sb.append(i2);
                                            sb.append(",i = ");
                                            sb.append(i4);
                                            sb.append(",newTouchTarget = ");
                                            sb.append(touchTarget5);
                                            sb.append(",idBitsToAssign = ");
                                            sb.append(pointerId);
                                            Log.d(TAG, sb.toString());
                                        } else {
                                            z = z7;
                                            f = y;
                                            f2 = x;
                                        }
                                        if (touchTarget5 != null) {
                                            touchTarget5.pointerIdBits |= pointerId;
                                            touchTargetAddTouchTarget = touchTarget5;
                                            z2 = false;
                                            z4 = false;
                                            break;
                                        }
                                        resetCancelNextUpFlag(andVerifyPreorderedView);
                                        if (dispatchTransformedTouchEvent(motionEvent, false, andVerifyPreorderedView, pointerId)) {
                                            this.mLastTouchDownTime = motionEvent.getDownTime();
                                            if (arrayListBuildTouchDispatchChildList != null) {
                                                int i5 = 0;
                                                while (true) {
                                                    if (i5 >= i2) {
                                                        break;
                                                    }
                                                    if (viewArr[andVerifyPreorderedIndex] == this.mChildren[i5]) {
                                                        this.mLastTouchDownIndex = i5;
                                                        break;
                                                    }
                                                    i5++;
                                                }
                                            } else {
                                                this.mLastTouchDownIndex = andVerifyPreorderedIndex;
                                            }
                                            this.mLastTouchDownX = motionEvent.getX();
                                            this.mLastTouchDownY = motionEvent.getY();
                                            touchTargetAddTouchTarget = addTouchTarget(andVerifyPreorderedView, pointerId);
                                            z2 = false;
                                            z4 = true;
                                        } else {
                                            motionEvent.setTargetAccessibilityFocus(false);
                                            touchTargetAddTouchTarget = touchTarget5;
                                        }
                                    }
                                    view2 = view;
                                    i4--;
                                    z9 = z10;
                                    y = f;
                                    x = f2;
                                    z7 = z;
                                }
                                z = z7;
                                f = y;
                                f2 = x;
                                motionEvent.setTargetAccessibilityFocus(false);
                                view2 = view;
                                i4--;
                                z9 = z10;
                                y = f;
                                x = f2;
                                z7 = z;
                            } else if (view2 != andVerifyPreorderedView) {
                                z = z7;
                                f = y;
                                f2 = x;
                                i4--;
                                z9 = z10;
                                y = f;
                                x = f2;
                                z7 = z;
                            } else {
                                i4 = i3;
                                view2 = null;
                                if (canViewReceivePointerEvents(andVerifyPreorderedView)) {
                                }
                                z = z7;
                                f = y;
                                f2 = x;
                                motionEvent.setTargetAccessibilityFocus(false);
                                view2 = view;
                                i4--;
                                z9 = z10;
                                y = f;
                                x = f2;
                                z7 = z;
                            }
                        }
                        if (arrayListBuildTouchDispatchChildList != null) {
                            arrayListBuildTouchDispatchChildList.clear();
                        }
                        z3 = z4;
                        touchTarget = touchTargetAddTouchTarget;
                    } else {
                        z = z7;
                        z2 = false;
                        z3 = false;
                        touchTarget = null;
                    }
                    if (touchTarget == null && this.mFirstTouchTarget != null) {
                        touchTarget = this.mFirstTouchTarget;
                        while (touchTarget.next != null) {
                            touchTarget = touchTarget.next;
                        }
                        touchTarget.pointerIdBits |= pointerId;
                    }
                }
                if (this.mFirstTouchTarget != null) {
                }
                z5 = zDispatchTransformedTouchEvent;
                if (z6) {
                    resetTouchState();
                }
            }
        }
        if (!z5 && this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 1);
        }
        return z5;
    }

    public ArrayList<View> buildTouchDispatchChildList() {
        return buildOrderedChildList();
    }

    private View findChildWithAccessibilityFocus() {
        View accessibilityFocusedHost;
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl == null || (accessibilityFocusedHost = viewRootImpl.getAccessibilityFocusedHost()) == null) {
            return null;
        }
        ViewParent parent = accessibilityFocusedHost.getParent();
        while (parent instanceof View) {
            if (parent == this) {
                return accessibilityFocusedHost;
            }
            accessibilityFocusedHost = parent;
            parent = accessibilityFocusedHost.getParent();
        }
        return null;
    }

    private void resetTouchState() {
        clearTouchTargets();
        resetCancelNextUpFlag(this);
        this.mGroupFlags &= -524289;
        this.mNestedScrollAxes = 0;
    }

    private static boolean resetCancelNextUpFlag(View view) {
        if ((view.mPrivateFlags & 67108864) != 0) {
            view.mPrivateFlags &= -67108865;
            return true;
        }
        return false;
    }

    private void clearTouchTargets() {
        TouchTarget touchTarget = this.mFirstTouchTarget;
        if (touchTarget != null) {
            while (true) {
                TouchTarget touchTarget2 = touchTarget.next;
                touchTarget.recycle();
                if (touchTarget2 == null) {
                    break;
                } else {
                    touchTarget = touchTarget2;
                }
            }
            if (ViewDebugManager.DEBUG_MOTION) {
                Log.d(TAG, "clearTouchTargets, mFirstTouchTarget set to null, this = " + this);
            }
            this.mFirstTouchTarget = null;
        }
    }

    private void cancelAndClearTouchTargets(MotionEvent motionEvent) {
        if (this.mFirstTouchTarget != null) {
            boolean z = false;
            if (motionEvent == null) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                motionEvent = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 3, 0.0f, 0.0f, 0);
                motionEvent.setSource(4098);
                z = true;
            }
            for (TouchTarget touchTarget = this.mFirstTouchTarget; touchTarget != null; touchTarget = touchTarget.next) {
                resetCancelNextUpFlag(touchTarget.child);
                dispatchTransformedTouchEvent(motionEvent, true, touchTarget.child, touchTarget.pointerIdBits);
            }
            clearTouchTargets();
            if (z) {
                motionEvent.recycle();
            }
        }
    }

    private TouchTarget getTouchTarget(View view) {
        for (TouchTarget touchTarget = this.mFirstTouchTarget; touchTarget != null; touchTarget = touchTarget.next) {
            if (touchTarget.child == view) {
                return touchTarget;
            }
        }
        return null;
    }

    private TouchTarget addTouchTarget(View view, int i) {
        TouchTarget touchTargetObtain = TouchTarget.obtain(view, i);
        if (ViewDebugManager.DEBUG_MOTION) {
            Log.d(TAG, "addTouchTarget:child = " + view + ",pointerIdBits = " + i + ",target = " + touchTargetObtain + ",mFirstTouchTarget = " + this.mFirstTouchTarget + ",this = " + this);
        }
        touchTargetObtain.next = this.mFirstTouchTarget;
        this.mFirstTouchTarget = touchTargetObtain;
        return touchTargetObtain;
    }

    private void removePointersFromTouchTargets(int i) {
        TouchTarget touchTarget = this.mFirstTouchTarget;
        TouchTarget touchTarget2 = null;
        while (touchTarget != null) {
            TouchTarget touchTarget3 = touchTarget.next;
            if ((touchTarget.pointerIdBits & i) != 0) {
                touchTarget.pointerIdBits &= ~i;
                if (touchTarget.pointerIdBits != 0) {
                    touchTarget2 = touchTarget;
                } else {
                    if (touchTarget2 == null) {
                        if (ViewDebugManager.DEBUG_MOTION) {
                            Log.d(TAG, "removePointersFromTouchTargets, mFirstTouchTarget = " + this.mFirstTouchTarget + ", next = " + touchTarget3 + ",this = " + this);
                        }
                        this.mFirstTouchTarget = touchTarget3;
                    } else {
                        touchTarget2.next = touchTarget3;
                    }
                    touchTarget.recycle();
                }
            }
            touchTarget = touchTarget3;
        }
    }

    private void cancelTouchTarget(View view) {
        TouchTarget touchTarget = this.mFirstTouchTarget;
        TouchTarget touchTarget2 = null;
        while (touchTarget != null) {
            TouchTarget touchTarget3 = touchTarget.next;
            if (touchTarget.child != view) {
                touchTarget2 = touchTarget;
                touchTarget = touchTarget3;
            } else {
                if (touchTarget2 == null) {
                    if (ViewDebugManager.DEBUG_MOTION) {
                        Log.d(TAG, "cancelTouchTarget, mFirstTouchTarget = " + this.mFirstTouchTarget + ", next = " + touchTarget3 + ",this = " + this);
                    }
                    this.mFirstTouchTarget = touchTarget3;
                } else {
                    touchTarget2.next = touchTarget3;
                }
                touchTarget.recycle();
                long jUptimeMillis = SystemClock.uptimeMillis();
                MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 3, 0.0f, 0.0f, 0);
                motionEventObtain.setSource(4098);
                view.dispatchTouchEvent(motionEventObtain);
                motionEventObtain.recycle();
                return;
            }
        }
    }

    private static boolean canViewReceivePointerEvents(View view) {
        return (view.mViewFlags & 12) == 0 || view.getAnimation() != null;
    }

    private float[] getTempPoint() {
        if (this.mTempPoint == null) {
            this.mTempPoint = new float[2];
        }
        return this.mTempPoint;
    }

    protected boolean isTransformedTouchPointInView(float f, float f2, View view, PointF pointF) {
        float[] tempPoint = getTempPoint();
        tempPoint[0] = f;
        tempPoint[1] = f2;
        transformPointToViewLocal(tempPoint, view);
        boolean zPointInView = view.pointInView(tempPoint[0], tempPoint[1]);
        if (zPointInView && pointF != null) {
            pointF.set(tempPoint[0], tempPoint[1]);
        }
        return zPointInView;
    }

    public void transformPointToViewLocal(float[] fArr, View view) {
        fArr[0] = fArr[0] + (this.mScrollX - view.mLeft);
        fArr[1] = fArr[1] + (this.mScrollY - view.mTop);
        if (!view.hasIdentityMatrix()) {
            view.getInverseMatrix().mapPoints(fArr);
        }
    }

    private boolean dispatchTransformedTouchEvent(MotionEvent motionEvent, boolean z, View view, int i) {
        boolean zDispatchTouchEvent;
        MotionEvent motionEventSplit;
        boolean zDispatchTouchEvent2;
        int action = motionEvent.getAction();
        if (ViewDebugManager.DEBUG_MOTION) {
            Log.d(TAG, "dispatchTransformedTouchEvent 1: event = " + motionEvent + ",cancel = " + z + ",oldAction = " + action + ",desiredPointerIdBits = " + i + ",mFirstTouchTarget = " + this.mFirstTouchTarget + ",child = " + view + ",this = " + this);
        }
        if (z || action == 3) {
            motionEvent.setAction(3);
            if (view == null) {
                zDispatchTouchEvent = super.dispatchTouchEvent(motionEvent);
            } else {
                zDispatchTouchEvent = view.dispatchTouchEvent(motionEvent);
            }
            motionEvent.setAction(action);
            return zDispatchTouchEvent;
        }
        int pointerIdBits = motionEvent.getPointerIdBits();
        int i2 = i & pointerIdBits;
        if (i2 == 0) {
            Log.i(TAG, "Dispatch transformed touch event without pointers in " + this);
            return false;
        }
        if (i2 == pointerIdBits) {
            if (view == null || view.hasIdentityMatrix()) {
                if (view == null) {
                    return super.dispatchTouchEvent(motionEvent);
                }
                float f = this.mScrollX - view.mLeft;
                float f2 = this.mScrollY - view.mTop;
                motionEvent.offsetLocation(f, f2);
                boolean zDispatchTouchEvent3 = view.dispatchTouchEvent(motionEvent);
                motionEvent.offsetLocation(-f, -f2);
                return zDispatchTouchEvent3;
            }
            motionEventSplit = MotionEvent.obtain(motionEvent);
        } else {
            motionEventSplit = motionEvent.split(i2);
        }
        if (view == null) {
            zDispatchTouchEvent2 = super.dispatchTouchEvent(motionEventSplit);
        } else {
            motionEventSplit.offsetLocation(this.mScrollX - view.mLeft, this.mScrollY - view.mTop);
            if (!view.hasIdentityMatrix()) {
                motionEventSplit.transform(view.getInverseMatrix());
            }
            zDispatchTouchEvent2 = view.dispatchTouchEvent(motionEventSplit);
        }
        if (ViewDebugManager.DEBUG_MOTION) {
            Log.d(TAG, "dispatchTransformedTouchEvent 3 to child " + view + ",handled = " + zDispatchTouchEvent2 + ",mScrollX = " + this.mScrollX + ",mScrollY = " + this.mScrollY + ",mFirstTouchTarget = " + this.mFirstTouchTarget + ",transformedEvent = " + motionEventSplit + ",this = " + this);
        }
        motionEventSplit.recycle();
        return zDispatchTouchEvent2;
    }

    public void setMotionEventSplittingEnabled(boolean z) {
        if (z) {
            this.mGroupFlags |= 2097152;
        } else {
            this.mGroupFlags &= -2097153;
        }
    }

    public boolean isMotionEventSplittingEnabled() {
        return (this.mGroupFlags & 2097152) == 2097152;
    }

    public boolean isTransitionGroup() {
        if ((this.mGroupFlags & 33554432) != 0) {
            return (this.mGroupFlags & 16777216) != 0;
        }
        ViewOutlineProvider outlineProvider = getOutlineProvider();
        return (getBackground() == null && getTransitionName() == null && (outlineProvider == null || outlineProvider == ViewOutlineProvider.BACKGROUND)) ? false : true;
    }

    public void setTransitionGroup(boolean z) {
        this.mGroupFlags |= 33554432;
        if (z) {
            this.mGroupFlags |= 16777216;
        } else {
            this.mGroupFlags &= -16777217;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z) {
        if (z == ((this.mGroupFlags & 524288) != 0)) {
            return;
        }
        if (z) {
            this.mGroupFlags |= 524288;
        } else {
            this.mGroupFlags &= -524289;
        }
        if (this.mParent != null) {
            this.mParent.requestDisallowInterceptTouchEvent(z);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.isFromSource(8194) && motionEvent.getAction() == 0 && motionEvent.isButtonPressed(1) && isOnScrollbarThumb(motionEvent.getX(), motionEvent.getY())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean requestFocus(int i, Rect rect) {
        boolean zOnRequestFocusInDescendants;
        if (ViewDebugManager.DBG) {
            System.out.println(this + " ViewGroup.requestFocus direction=" + i);
        }
        int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability == 131072) {
            boolean zRequestFocus = super.requestFocus(i, rect);
            if (!zRequestFocus) {
                zOnRequestFocusInDescendants = onRequestFocusInDescendants(i, rect);
            } else {
                zOnRequestFocusInDescendants = zRequestFocus;
            }
        } else if (descendantFocusability == 262144) {
            boolean zOnRequestFocusInDescendants2 = onRequestFocusInDescendants(i, rect);
            if (!zOnRequestFocusInDescendants2) {
                zOnRequestFocusInDescendants = super.requestFocus(i, rect);
            } else {
                zOnRequestFocusInDescendants = zOnRequestFocusInDescendants2;
            }
        } else if (descendantFocusability == 393216) {
            zOnRequestFocusInDescendants = super.requestFocus(i, rect);
        } else {
            throw new IllegalStateException("descendant focusability must be one of FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS but is " + descendantFocusability);
        }
        if (zOnRequestFocusInDescendants && !isLayoutValid() && (this.mPrivateFlags & 1) == 0) {
            this.mPrivateFlags |= 1;
        }
        return zOnRequestFocusInDescendants;
    }

    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        int i2;
        int i3;
        int i4 = this.mChildrenCount;
        int i5 = -1;
        if ((i & 2) == 0) {
            i2 = i4 - 1;
            i3 = -1;
        } else {
            i3 = i4;
            i2 = 0;
            i5 = 1;
        }
        View[] viewArr = this.mChildren;
        while (i2 != i3) {
            View view = viewArr[i2];
            if ((view.mViewFlags & 12) == 0 && view.requestFocus(i, rect)) {
                return true;
            }
            i2 += i5;
        }
        return false;
    }

    @Override
    public boolean restoreDefaultFocus() {
        if (this.mDefaultFocus != null && getDescendantFocusability() != 393216 && (this.mDefaultFocus.mViewFlags & 12) == 0 && this.mDefaultFocus.restoreDefaultFocus()) {
            return true;
        }
        return super.restoreDefaultFocus();
    }

    @Override
    public boolean restoreFocusInCluster(int i) {
        if (isKeyboardNavigationCluster()) {
            boolean touchscreenBlocksFocus = getTouchscreenBlocksFocus();
            try {
                setTouchscreenBlocksFocusNoRefocus(false);
                return restoreFocusInClusterInternal(i);
            } finally {
                setTouchscreenBlocksFocusNoRefocus(touchscreenBlocksFocus);
            }
        }
        return restoreFocusInClusterInternal(i);
    }

    private boolean restoreFocusInClusterInternal(int i) {
        if (this.mFocusedInCluster != null && getDescendantFocusability() != 393216 && (this.mFocusedInCluster.mViewFlags & 12) == 0 && this.mFocusedInCluster.restoreFocusInCluster(i)) {
            return true;
        }
        return super.restoreFocusInCluster(i);
    }

    @Override
    public boolean restoreFocusNotInCluster() {
        if (this.mFocusedInCluster != null) {
            return restoreFocusInCluster(130);
        }
        if (isKeyboardNavigationCluster() || (this.mViewFlags & 12) != 0) {
            return false;
        }
        int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability == 393216) {
            return super.requestFocus(130, null);
        }
        if (descendantFocusability == 131072 && super.requestFocus(130, null)) {
            return true;
        }
        for (int i = 0; i < this.mChildrenCount; i++) {
            View view = this.mChildren[i];
            if (!view.isKeyboardNavigationCluster() && view.restoreFocusNotInCluster()) {
                return true;
            }
        }
        if (descendantFocusability != 262144 || hasFocusableChild(false)) {
            return false;
        }
        return super.requestFocus(130, null);
    }

    @Override
    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchStartTemporaryDetach();
        }
    }

    @Override
    public void dispatchFinishTemporaryDetach() {
        super.dispatchFinishTemporaryDetach();
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchFinishTemporaryDetach();
        }
    }

    @Override
    void dispatchAttachedToWindow(View.AttachInfo attachInfo, int i) {
        int size;
        this.mGroupFlags |= 4194304;
        super.dispatchAttachedToWindow(attachInfo, i);
        this.mGroupFlags &= -4194305;
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            View view = viewArr[i3];
            view.dispatchAttachedToWindow(attachInfo, combineVisibility(i, view.getVisibility()));
        }
        if (this.mTransientIndices != null) {
            size = this.mTransientIndices.size();
        } else {
            size = 0;
        }
        for (int i4 = 0; i4 < size; i4++) {
            View view2 = this.mTransientViews.get(i4);
            view2.dispatchAttachedToWindow(attachInfo, combineVisibility(i, view2.getVisibility()));
        }
    }

    @Override
    void dispatchScreenStateChanged(int i) {
        super.dispatchScreenStateChanged(i);
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            viewArr[i3].dispatchScreenStateChanged(i);
        }
    }

    @Override
    void dispatchMovedToDisplay(Display display, Configuration configuration) {
        super.dispatchMovedToDisplay(display, configuration);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchMovedToDisplay(display, configuration);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        boolean zDispatchPopulateAccessibilityEvent;
        boolean zDispatchPopulateAccessibilityEventInternal;
        if (includeForAccessibility() && (zDispatchPopulateAccessibilityEventInternal = super.dispatchPopulateAccessibilityEventInternal(accessibilityEvent))) {
            return zDispatchPopulateAccessibilityEventInternal;
        }
        ChildListForAccessibility childListForAccessibilityObtain = ChildListForAccessibility.obtain(this, true);
        try {
            int childCount = childListForAccessibilityObtain.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = childListForAccessibilityObtain.getChildAt(i);
                if ((childAt.mViewFlags & 12) == 0 && (zDispatchPopulateAccessibilityEvent = childAt.dispatchPopulateAccessibilityEvent(accessibilityEvent))) {
                    return zDispatchPopulateAccessibilityEvent;
                }
            }
            return false;
        } finally {
            childListForAccessibilityObtain.recycle();
        }
    }

    @Override
    public void dispatchProvideStructure(ViewStructure viewStructure) {
        int i;
        int andVerifyPreorderedIndex;
        super.dispatchProvideStructure(viewStructure);
        if (isAssistBlocked() || viewStructure.getChildCount() != 0 || (i = this.mChildrenCount) <= 0) {
            return;
        }
        if (!isLaidOut()) {
            if (Helper.sVerbose) {
                Log.v("View", "dispatchProvideStructure(): not laid out, ignoring " + i + " children of " + getAccessibilityViewId());
                return;
            }
            return;
        }
        viewStructure.setChildCount(i);
        ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
        boolean z = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
        ArrayList<View> arrayList = arrayListBuildOrderedChildList;
        for (int i2 = 0; i2 < i; i2++) {
            try {
                andVerifyPreorderedIndex = getAndVerifyPreorderedIndex(i, i2, z);
            } catch (IndexOutOfBoundsException e) {
                if (this.mContext.getApplicationInfo().targetSdkVersion < 23) {
                    Log.w(TAG, "Bad getChildDrawingOrder while collecting assist @ " + i2 + " of " + i, e);
                    if (i2 > 0) {
                        int[] iArr = new int[i];
                        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
                        for (int i3 = 0; i3 < i2; i3++) {
                            iArr[i3] = getChildDrawingOrder(i, i3);
                            sparseBooleanArray.put(iArr[i3], true);
                        }
                        int i4 = 0;
                        for (int i5 = i2; i5 < i; i5++) {
                            while (sparseBooleanArray.get(i4, false)) {
                                i4++;
                            }
                            iArr[i5] = i4;
                            i4++;
                        }
                        arrayList = new ArrayList<>(i);
                        for (int i6 = 0; i6 < i; i6++) {
                            arrayList.add(this.mChildren[iArr[i6]]);
                        }
                    }
                    andVerifyPreorderedIndex = i2;
                    z = false;
                } else {
                    throw e;
                }
            }
            getAndVerifyPreorderedView(arrayList, this.mChildren, andVerifyPreorderedIndex).dispatchProvideStructure(viewStructure.newChild(i2));
        }
        if (arrayList != null) {
            arrayList.clear();
        }
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure viewStructure, int i) {
        super.dispatchProvideAutofillStructure(viewStructure, i);
        if (viewStructure.getChildCount() != 0) {
            return;
        }
        if (!isLaidOut()) {
            if (Helper.sVerbose) {
                Log.v("View", "dispatchProvideAutofillStructure(): not laid out, ignoring " + this.mChildrenCount + " children of " + getAutofillId());
                return;
            }
            return;
        }
        ChildListForAutoFill childrenForAutofill = getChildrenForAutofill(i);
        int size = childrenForAutofill.size();
        viewStructure.setChildCount(size);
        for (int i2 = 0; i2 < size; i2++) {
            childrenForAutofill.get(i2).dispatchProvideAutofillStructure(viewStructure.newChild(i2), i);
        }
        childrenForAutofill.recycle();
    }

    private ChildListForAutoFill getChildrenForAutofill(int i) {
        ChildListForAutoFill childListForAutoFillObtain = ChildListForAutoFill.obtain();
        populateChildrenForAutofill(childListForAutoFillObtain, i);
        return childListForAutoFillObtain;
    }

    private void populateChildrenForAutofill(ArrayList<View> arrayList, int i) {
        int i2 = this.mChildrenCount;
        if (i2 <= 0) {
            return;
        }
        ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
        boolean z = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
        for (int i3 = 0; i3 < i2; i3++) {
            int andVerifyPreorderedIndex = getAndVerifyPreorderedIndex(i2, i3, z);
            View view = arrayListBuildOrderedChildList == null ? this.mChildren[andVerifyPreorderedIndex] : arrayListBuildOrderedChildList.get(andVerifyPreorderedIndex);
            if ((i & 1) != 0 || view.isImportantForAutofill()) {
                arrayList.add(view);
            } else if (view instanceof ViewGroup) {
                ((ViewGroup) view).populateChildrenForAutofill(arrayList, i);
            }
        }
    }

    private static View getAndVerifyPreorderedView(ArrayList<View> arrayList, View[] viewArr, int i) {
        if (arrayList != null) {
            View view = arrayList.get(i);
            if (view == null) {
                throw new RuntimeException("Invalid preorderedList contained null child at index " + i);
            }
            return view;
        }
        return viewArr[i];
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (getAccessibilityNodeProvider() == null && this.mAttachInfo != null) {
            ArrayList<View> arrayList = this.mAttachInfo.mTempArrayList;
            arrayList.clear();
            addChildrenForAccessibility(arrayList);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                accessibilityNodeInfo.addChildUnchecked(arrayList.get(i));
            }
            arrayList.clear();
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ViewGroup.class.getName();
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View view, View view2, int i) {
        if (getAccessibilityLiveRegion() != 0) {
            notifyViewAccessibilityStateChangedIfNeeded(1);
            return;
        }
        if (this.mParent != null) {
            try {
                this.mParent.notifySubtreeAccessibilityStateChanged(this, view2, i);
            } catch (AbstractMethodError e) {
                Log.e("View", this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
            }
        }
    }

    @Override
    public void notifySubtreeAccessibilityStateChangedIfNeeded() {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled() || this.mAttachInfo == null) {
            return;
        }
        if (getImportantForAccessibility() != 4 && !isImportantForAccessibility() && getChildCount() > 0) {
            Object parentForAccessibility = getParentForAccessibility();
            if (parentForAccessibility instanceof View) {
                ((View) parentForAccessibility).notifySubtreeAccessibilityStateChangedIfNeeded();
                return;
            }
        }
        super.notifySubtreeAccessibilityStateChangedIfNeeded();
    }

    @Override
    void resetSubtreeAccessibilityStateChanged() {
        super.resetSubtreeAccessibilityStateChanged();
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].resetSubtreeAccessibilityStateChanged();
        }
    }

    int getNumChildrenForAccessibility() {
        int numChildrenForAccessibility = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt.includeForAccessibility()) {
                numChildrenForAccessibility++;
            } else if (childAt instanceof ViewGroup) {
                numChildrenForAccessibility += ((ViewGroup) childAt).getNumChildrenForAccessibility();
            }
        }
        return numChildrenForAccessibility;
    }

    @Override
    public boolean onNestedPrePerformAccessibilityAction(View view, int i, Bundle bundle) {
        return false;
    }

    @Override
    void dispatchDetachedFromWindow() {
        int size;
        cancelAndClearTouchTargets(null);
        exitHoverTargets();
        exitTooltipHoverTargets();
        this.mLayoutCalledWhileSuppressed = false;
        this.mChildrenInterestedInDrag = null;
        this.mIsInterestedInDrag = false;
        if (this.mCurrentDragStartEvent != null) {
            this.mCurrentDragStartEvent.recycle();
            this.mCurrentDragStartEvent = null;
        }
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchDetachedFromWindow();
        }
        clearDisappearingChildren();
        if (this.mTransientViews != null) {
            size = this.mTransientIndices.size();
        } else {
            size = 0;
        }
        for (int i3 = 0; i3 < size; i3++) {
            this.mTransientViews.get(i3).dispatchDetachedFromWindow();
        }
        super.dispatchDetachedFromWindow();
    }

    @Override
    protected void internalSetPadding(int i, int i2, int i3, int i4) {
        super.internalSetPadding(i, i2, i3, i4);
        if ((this.mPaddingLeft | this.mPaddingTop | this.mPaddingRight | this.mPaddingBottom) != 0) {
            this.mGroupFlags |= 32;
        } else {
            this.mGroupFlags &= -33;
        }
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        super.dispatchSaveInstanceState(sparseArray);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if ((view.mViewFlags & 536870912) != 536870912) {
                view.dispatchSaveInstanceState(sparseArray);
            }
        }
    }

    protected void dispatchFreezeSelfOnly(SparseArray<Parcelable> sparseArray) {
        super.dispatchSaveInstanceState(sparseArray);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        super.dispatchRestoreInstanceState(sparseArray);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if (view != null && (view.mViewFlags & 536870912) != 536870912) {
                view.dispatchRestoreInstanceState(sparseArray);
            }
        }
    }

    protected void dispatchThawSelfOnly(SparseArray<Parcelable> sparseArray) {
        super.dispatchRestoreInstanceState(sparseArray);
    }

    @Deprecated
    protected void setChildrenDrawingCacheEnabled(boolean z) {
        if (z || (this.mPersistentDrawingCache & 3) != 3) {
            View[] viewArr = this.mChildren;
            int i = this.mChildrenCount;
            for (int i2 = 0; i2 < i; i2++) {
                viewArr[i2].setDrawingCacheEnabled(z);
            }
        }
    }

    @Override
    public Bitmap createSnapshot(ViewDebug.CanvasProvider canvasProvider, boolean z) {
        int[] iArr;
        int i = this.mChildrenCount;
        int i2 = 0;
        if (z) {
            iArr = new int[i];
            for (int i3 = 0; i3 < i; i3++) {
                View childAt = getChildAt(i3);
                iArr[i3] = childAt.getVisibility();
                if (iArr[i3] == 0) {
                    childAt.mViewFlags = (childAt.mViewFlags & (-13)) | 4;
                }
            }
        } else {
            iArr = null;
        }
        try {
            return super.createSnapshot(canvasProvider, z);
        } finally {
            if (z) {
                while (i2 < i) {
                    View childAt2 = getChildAt(i2);
                    childAt2.mViewFlags = (childAt2.mViewFlags & (-13)) | (iArr[i2] & 12);
                    i2++;
                }
            }
        }
    }

    boolean isLayoutModeOptical() {
        return this.mLayoutMode == 1;
    }

    @Override
    Insets computeOpticalInsets() {
        if (isLayoutModeOptical()) {
            int iMax = 0;
            int iMax2 = 0;
            int iMax3 = 0;
            int iMax4 = 0;
            for (int i = 0; i < this.mChildrenCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.getVisibility() == 0) {
                    Insets opticalInsets = childAt.getOpticalInsets();
                    iMax = Math.max(iMax, opticalInsets.left);
                    iMax2 = Math.max(iMax2, opticalInsets.top);
                    iMax3 = Math.max(iMax3, opticalInsets.right);
                    iMax4 = Math.max(iMax4, opticalInsets.bottom);
                }
            }
            return Insets.of(iMax, iMax2, iMax3, iMax4);
        }
        return Insets.NONE;
    }

    private static void fillRect(Canvas canvas, Paint paint, int i, int i2, int i3, int i4) {
        if (i != i3 && i2 != i4) {
            if (i > i3) {
                i3 = i;
                i = i3;
            }
            if (i2 > i4) {
                i4 = i2;
                i2 = i4;
            }
            canvas.drawRect(i, i2, i3, i4, paint);
        }
    }

    private static int sign(int i) {
        return i >= 0 ? 1 : -1;
    }

    private static void drawCorner(Canvas canvas, Paint paint, int i, int i2, int i3, int i4, int i5) {
        fillRect(canvas, paint, i, i2, i + i3, i2 + (sign(i4) * i5));
        fillRect(canvas, paint, i, i2, i + (i5 * sign(i3)), i2 + i4);
    }

    private static void drawRectCorners(Canvas canvas, int i, int i2, int i3, int i4, Paint paint, int i5, int i6) {
        drawCorner(canvas, paint, i, i2, i5, i5, i6);
        int i7 = -i5;
        drawCorner(canvas, paint, i, i4, i5, i7, i6);
        drawCorner(canvas, paint, i3, i2, i7, i5, i6);
        drawCorner(canvas, paint, i3, i4, i7, i7, i6);
    }

    private static void fillDifference(Canvas canvas, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, Paint paint) {
        int i9 = i - i5;
        int i10 = i3 + i7;
        fillRect(canvas, paint, i9, i2 - i6, i10, i2);
        fillRect(canvas, paint, i9, i2, i, i4);
        fillRect(canvas, paint, i3, i2, i10, i4);
        fillRect(canvas, paint, i9, i4, i10, i4 + i8);
    }

    protected void onDebugDrawMargins(Canvas canvas, Paint paint) {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            childAt.getLayoutParams().onDebugDraw(childAt, canvas, paint);
        }
    }

    protected void onDebugDraw(Canvas canvas) {
        Paint debugPaint = getDebugPaint();
        debugPaint.setColor(-65536);
        debugPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8) {
                Insets opticalInsets = childAt.getOpticalInsets();
                drawRect(canvas, debugPaint, childAt.getLeft() + opticalInsets.left, childAt.getTop() + opticalInsets.top, (childAt.getRight() - opticalInsets.right) - 1, (childAt.getBottom() - opticalInsets.bottom) - 1);
            }
        }
        debugPaint.setColor(Color.argb(63, 255, 0, 255));
        debugPaint.setStyle(Paint.Style.FILL);
        onDebugDrawMargins(canvas, debugPaint);
        debugPaint.setColor(DEBUG_CORNERS_COLOR);
        debugPaint.setStyle(Paint.Style.FILL);
        int iDipsToPixels = dipsToPixels(8);
        int iDipsToPixels2 = dipsToPixels(1);
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt2 = getChildAt(i2);
            if (childAt2.getVisibility() != 8) {
                drawRectCorners(canvas, childAt2.getLeft(), childAt2.getTop(), childAt2.getRight(), childAt2.getBottom(), debugPaint, iDipsToPixels, iDipsToPixels2);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int iSave;
        int size;
        boolean zIsRecordingFor = canvas.isRecordingFor(this.mRenderNode);
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        int i2 = this.mGroupFlags;
        int i3 = 0;
        if ((i2 & 8) != 0 && canAnimate()) {
            isHardwareAccelerated();
            for (int i4 = 0; i4 < i; i4++) {
                View view = viewArr[i4];
                if ((view.mViewFlags & 12) == 0) {
                    attachLayoutAnimationParameters(view, view.getLayoutParams(), i4, i);
                    bindLayoutAnimation(view);
                }
            }
            LayoutAnimationController layoutAnimationController = this.mLayoutAnimationController;
            if (layoutAnimationController.willOverlap()) {
                this.mGroupFlags |= 128;
            }
            layoutAnimationController.start();
            this.mGroupFlags &= -9;
            this.mGroupFlags &= -17;
            if (this.mAnimationListener != null) {
                this.mAnimationListener.onAnimationStart(layoutAnimationController.getAnimation());
            }
        }
        boolean z = (i2 & 34) == 34;
        if (z) {
            iSave = canvas.save(2);
            canvas.clipRect(this.mScrollX + this.mPaddingLeft, this.mScrollY + this.mPaddingTop, ((this.mScrollX + this.mRight) - this.mLeft) - this.mPaddingRight, ((this.mScrollY + this.mBottom) - this.mTop) - this.mPaddingBottom);
        } else {
            iSave = 0;
        }
        this.mPrivateFlags &= -65;
        this.mGroupFlags &= -5;
        long drawingTime = getDrawingTime();
        if (zIsRecordingFor) {
            canvas.insertReorderBarrier();
        }
        if (this.mTransientIndices != null) {
            size = this.mTransientIndices.size();
        } else {
            size = 0;
        }
        int i5 = size != 0 ? 0 : -1;
        ArrayList<View> arrayListBuildOrderedChildList = zIsRecordingFor ? null : buildOrderedChildList();
        boolean z2 = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
        int i6 = i5;
        boolean zDrawChild = false;
        while (i3 < i) {
            int i7 = i6;
            boolean zDrawChild2 = zDrawChild;
            int i8 = i7;
            while (i8 >= 0 && this.mTransientIndices.get(i8).intValue() == i3) {
                View view2 = this.mTransientViews.get(i8);
                if ((view2.mViewFlags & 12) == 0 || view2.getAnimation() != null) {
                    zDrawChild2 |= drawChild(canvas, view2, drawingTime);
                }
                int i9 = i8 + 1;
                i8 = i9 >= size ? -1 : i9;
            }
            View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i, i3, z2));
            if ((andVerifyPreorderedView.mViewFlags & 12) == 0 || andVerifyPreorderedView.getAnimation() != null) {
                zDrawChild2 |= drawChild(canvas, andVerifyPreorderedView, drawingTime);
            }
            i3++;
            boolean z3 = zDrawChild2;
            i6 = i8;
            zDrawChild = z3;
        }
        int i10 = i6;
        while (i10 >= 0) {
            View view3 = this.mTransientViews.get(i10);
            if ((view3.mViewFlags & 12) == 0 || view3.getAnimation() != null) {
                zDrawChild = drawChild(canvas, view3, drawingTime) | zDrawChild;
            }
            i10++;
            if (i10 >= size) {
                break;
            }
        }
        if (arrayListBuildOrderedChildList != null) {
            arrayListBuildOrderedChildList.clear();
        }
        if (this.mDisappearingChildren != null) {
            ArrayList<View> arrayList = this.mDisappearingChildren;
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                zDrawChild |= drawChild(canvas, arrayList.get(size2), drawingTime);
            }
        }
        if (zIsRecordingFor) {
            canvas.insertInorderBarrier();
        }
        if (debugDraw()) {
            onDebugDraw(canvas);
        }
        if (z) {
            canvas.restoreToCount(iSave);
        }
        int i11 = this.mGroupFlags;
        if ((i11 & 4) == 4) {
            invalidate(true);
        }
        if ((i11 & 16) == 0 && (i11 & 512) == 0 && this.mLayoutAnimationController.isDone() && !zDrawChild) {
            this.mGroupFlags |= 512;
            post(new Runnable() {
                @Override
                public void run() {
                    ViewGroup.this.notifyAnimationListener();
                }
            });
        }
    }

    @Override
    public ViewGroupOverlay getOverlay() {
        if (this.mOverlay == null) {
            this.mOverlay = new ViewGroupOverlay(this.mContext, this);
        }
        return (ViewGroupOverlay) this.mOverlay;
    }

    protected int getChildDrawingOrder(int i, int i2) {
        return i2;
    }

    private boolean hasChildWithZ() {
        for (int i = 0; i < this.mChildrenCount; i++) {
            if (this.mChildren[i].getZ() != 0.0f) {
                return true;
            }
        }
        return false;
    }

    ArrayList<View> buildOrderedChildList() {
        int i = this.mChildrenCount;
        if (i <= 1 || !hasChildWithZ()) {
            return null;
        }
        if (this.mPreSortedChildren == null) {
            this.mPreSortedChildren = new ArrayList<>(i);
        } else {
            this.mPreSortedChildren.clear();
            this.mPreSortedChildren.ensureCapacity(i);
        }
        boolean zIsChildrenDrawingOrderEnabled = isChildrenDrawingOrderEnabled();
        for (int i2 = 0; i2 < i; i2++) {
            View view = this.mChildren[getAndVerifyPreorderedIndex(i, i2, zIsChildrenDrawingOrderEnabled)];
            float z = view.getZ();
            int i3 = i2;
            while (i3 > 0 && this.mPreSortedChildren.get(i3 - 1).getZ() > z) {
                i3--;
            }
            this.mPreSortedChildren.add(i3, view);
        }
        return this.mPreSortedChildren;
    }

    private void notifyAnimationListener() {
        this.mGroupFlags &= -513;
        this.mGroupFlags |= 16;
        if (this.mAnimationListener != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    ViewGroup.this.mAnimationListener.onAnimationEnd(ViewGroup.this.mLayoutAnimationController.getAnimation());
                }
            });
        }
        invalidate(true);
    }

    @Override
    protected void dispatchGetDisplayList() {
        int size;
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if ((view.mViewFlags & 12) == 0 || view.getAnimation() != null) {
                recreateChildDisplayList(view);
            }
        }
        if (this.mTransientViews != null) {
            size = this.mTransientIndices.size();
        } else {
            size = 0;
        }
        for (int i3 = 0; i3 < size; i3++) {
            View view2 = this.mTransientViews.get(i3);
            if ((view2.mViewFlags & 12) == 0 || view2.getAnimation() != null) {
                recreateChildDisplayList(view2);
            }
        }
        if (this.mOverlay != null) {
            recreateChildDisplayList(this.mOverlay.getOverlayView());
        }
        if (this.mDisappearingChildren != null) {
            ArrayList<View> arrayList = this.mDisappearingChildren;
            int size2 = arrayList.size();
            for (int i4 = 0; i4 < size2; i4++) {
                recreateChildDisplayList(arrayList.get(i4));
            }
        }
    }

    private void recreateChildDisplayList(View view) {
        view.mRecreateDisplayList = (view.mPrivateFlags & Integer.MIN_VALUE) != 0;
        view.mPrivateFlags &= Integer.MAX_VALUE;
        view.updateDisplayListIfDirty();
        view.mRecreateDisplayList = false;
    }

    protected boolean drawChild(Canvas canvas, View view, long j) {
        return view.draw(canvas, this, j);
    }

    @Override
    void getScrollIndicatorBounds(Rect rect) {
        super.getScrollIndicatorBounds(rect);
        if ((this.mGroupFlags & 34) == 34) {
            rect.left += this.mPaddingLeft;
            rect.right -= this.mPaddingRight;
            rect.top += this.mPaddingTop;
            rect.bottom -= this.mPaddingBottom;
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean getClipChildren() {
        return (this.mGroupFlags & 1) != 0;
    }

    public void setClipChildren(boolean z) {
        if (z != ((this.mGroupFlags & 1) == 1)) {
            setBooleanFlag(1, z);
            for (int i = 0; i < this.mChildrenCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.mRenderNode != null) {
                    childAt.mRenderNode.setClipToBounds(z);
                }
            }
            invalidate(true);
        }
    }

    public void setClipToPadding(boolean z) {
        if (hasBooleanFlag(2) != z) {
            setBooleanFlag(2, z);
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean getClipToPadding() {
        return hasBooleanFlag(2);
    }

    @Override
    public void dispatchSetSelected(boolean z) {
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].setSelected(z);
        }
    }

    @Override
    public void dispatchSetActivated(boolean z) {
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].setActivated(z);
        }
    }

    @Override
    protected void dispatchSetPressed(boolean z) {
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if (!z || (!view.isClickable() && !view.isLongClickable())) {
                view.setPressed(z);
            }
        }
    }

    @Override
    public void dispatchDrawableHotspotChanged(float f, float f2) {
        int i = this.mChildrenCount;
        if (i == 0) {
            return;
        }
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            boolean z = (view.isClickable() || view.isLongClickable()) ? false : true;
            boolean z2 = (view.mViewFlags & 4194304) != 0;
            if (z || z2) {
                float[] tempPoint = getTempPoint();
                tempPoint[0] = f;
                tempPoint[1] = f2;
                transformPointToViewLocal(tempPoint, view);
                view.drawableHotspotChanged(tempPoint[0], tempPoint[1]);
            }
        }
    }

    @Override
    void dispatchCancelPendingInputEvents() {
        super.dispatchCancelPendingInputEvents();
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].dispatchCancelPendingInputEvents();
        }
    }

    protected void setStaticTransformationsEnabled(boolean z) {
        setBooleanFlag(2048, z);
    }

    protected boolean getChildStaticTransformation(View view, Transformation transformation) {
        return false;
    }

    Transformation getChildTransformation() {
        if (this.mChildTransformation == null) {
            this.mChildTransformation = new Transformation();
        }
        return this.mChildTransformation;
    }

    @Override
    protected <T extends View> T findViewTraversal(int i) {
        T t;
        if (i == this.mID) {
            return this;
        }
        View[] viewArr = this.mChildren;
        int i2 = this.mChildrenCount;
        for (int i3 = 0; i3 < i2; i3++) {
            View view = viewArr[i3];
            if ((view.mPrivateFlags & 8) == 0 && (t = (T) view.findViewById(i)) != null) {
                return t;
            }
        }
        return null;
    }

    @Override
    protected <T extends View> T findViewWithTagTraversal(Object obj) {
        T t;
        if (obj != null && obj.equals(this.mTag)) {
            return this;
        }
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            View view = viewArr[i2];
            if ((view.mPrivateFlags & 8) == 0 && (t = (T) view.findViewWithTag(obj)) != null) {
                return t;
            }
        }
        return null;
    }

    @Override
    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View view) {
        T t;
        if (predicate.test(this)) {
            return this;
        }
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            View view2 = viewArr[i2];
            if (view2 != view && (view2.mPrivateFlags & 8) == 0 && (t = (T) view2.findViewByPredicate(predicate)) != null) {
                return t;
            }
        }
        return null;
    }

    public void addTransientView(View view, int i) {
        if (i < 0) {
            return;
        }
        if (this.mTransientIndices == null) {
            this.mTransientIndices = new ArrayList();
            this.mTransientViews = new ArrayList();
        }
        int size = this.mTransientIndices.size();
        if (size > 0) {
            int i2 = 0;
            while (i2 < size && i >= this.mTransientIndices.get(i2).intValue()) {
                i2++;
            }
            this.mTransientIndices.add(i2, Integer.valueOf(i));
            this.mTransientViews.add(i2, view);
        } else {
            this.mTransientIndices.add(Integer.valueOf(i));
            this.mTransientViews.add(view);
        }
        view.mParent = this;
        view.dispatchAttachedToWindow(this.mAttachInfo, this.mViewFlags & 12);
        invalidate(true);
    }

    public void removeTransientView(View view) {
        if (this.mTransientViews == null) {
            return;
        }
        int size = this.mTransientViews.size();
        for (int i = 0; i < size; i++) {
            if (view == this.mTransientViews.get(i)) {
                this.mTransientViews.remove(i);
                this.mTransientIndices.remove(i);
                view.mParent = null;
                view.dispatchDetachedFromWindow();
                invalidate(true);
                return;
            }
        }
    }

    public int getTransientViewCount() {
        if (this.mTransientIndices == null) {
            return 0;
        }
        return this.mTransientIndices.size();
    }

    public int getTransientViewIndex(int i) {
        if (i < 0 || this.mTransientIndices == null || i >= this.mTransientIndices.size()) {
            return -1;
        }
        return this.mTransientIndices.get(i).intValue();
    }

    public View getTransientView(int i) {
        if (this.mTransientViews == null || i >= this.mTransientViews.size()) {
            return null;
        }
        return this.mTransientViews.get(i);
    }

    public void addView(View view) {
        addView(view, -1);
    }

    public void addView(View view, int i) {
        if (view == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }
        LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null && (layoutParams = generateDefaultLayoutParams()) == null) {
            throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
        }
        addView(view, i, layoutParams);
    }

    public void addView(View view, int i, int i2) {
        LayoutParams layoutParamsGenerateDefaultLayoutParams = generateDefaultLayoutParams();
        layoutParamsGenerateDefaultLayoutParams.width = i;
        layoutParamsGenerateDefaultLayoutParams.height = i2;
        addView(view, -1, layoutParamsGenerateDefaultLayoutParams);
    }

    @Override
    public void addView(View view, LayoutParams layoutParams) {
        addView(view, -1, layoutParams);
    }

    public void addView(View view, int i, LayoutParams layoutParams) {
        if (ViewDebugManager.DBG) {
            System.out.println(this + " addView");
        }
        if (view == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }
        requestLayout();
        invalidate(true);
        addViewInner(view, i, layoutParams, false);
    }

    @Override
    public void updateViewLayout(View view, LayoutParams layoutParams) {
        if (!checkLayoutParams(layoutParams)) {
            throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        }
        if (view.mParent != this) {
            throw new IllegalArgumentException("Given view not a child of " + this);
        }
        view.setLayoutParams(layoutParams);
    }

    protected boolean checkLayoutParams(LayoutParams layoutParams) {
        return layoutParams != null;
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mOnHierarchyChangeListener = onHierarchyChangeListener;
    }

    void dispatchViewAdded(View view) {
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.e(TAG, "dispatchViewAdded view to parent " + this + " view == " + view, new Throwable());
        }
        onViewAdded(view);
        if (this.mOnHierarchyChangeListener != null) {
            this.mOnHierarchyChangeListener.onChildViewAdded(this, view);
        }
    }

    public void onViewAdded(View view) {
    }

    void dispatchViewRemoved(View view) {
        ViewDebugManager.getInstance().debugViewRemoved(view, this, getViewRootImpl() != null ? getViewRootImpl().mThread : null);
        onViewRemoved(view);
        if (this.mOnHierarchyChangeListener != null) {
            this.mOnHierarchyChangeListener.onChildViewRemoved(this, view);
        }
    }

    public void onViewRemoved(View view) {
    }

    private void clearCachedLayoutMode() {
        if (!hasBooleanFlag(8388608)) {
            this.mLayoutMode = -1;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        clearCachedLayoutMode();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearCachedLayoutMode();
    }

    @Override
    protected void destroyHardwareResources() {
        super.destroyHardwareResources();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).destroyHardwareResources();
        }
    }

    protected boolean addViewInLayout(View view, int i, LayoutParams layoutParams) {
        return addViewInLayout(view, i, layoutParams, false);
    }

    protected boolean addViewInLayout(View view, int i, LayoutParams layoutParams, boolean z) {
        if (view == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }
        view.mParent = null;
        addViewInner(view, i, layoutParams, z);
        view.mPrivateFlags = (view.mPrivateFlags & (-6291457)) | 32;
        return true;
    }

    protected void cleanupLayoutState(View view) {
        view.mPrivateFlags &= -4097;
    }

    private void addViewInner(View view, int i, LayoutParams layoutParams, boolean z) {
        if (this.mTransition != null) {
            this.mTransition.cancel(3);
        }
        if (view.getParent() != null) {
            throw new IllegalStateException("The specified child already has a parent. You must call removeView() on the child's parent first.");
        }
        if (this.mTransition != null) {
            this.mTransition.addChild(this, view);
        }
        if (!checkLayoutParams(layoutParams)) {
            layoutParams = generateLayoutParams(layoutParams);
        }
        if (z) {
            view.mLayoutParams = layoutParams;
        } else {
            view.setLayoutParams(layoutParams);
        }
        if (i < 0) {
            i = this.mChildrenCount;
        }
        addInArray(view, i);
        if (z) {
            view.assignParent(this);
        } else {
            view.mParent = this;
        }
        if (view.hasUnhandledKeyListener()) {
            incrementChildUnhandledKeyListeners();
        }
        if (view.hasFocus()) {
            requestChildFocus(view, view.findFocus());
        }
        View.AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null && (this.mGroupFlags & 4194304) == 0) {
            boolean z2 = attachInfo.mKeepScreenOn;
            attachInfo.mKeepScreenOn = false;
            view.dispatchAttachedToWindow(this.mAttachInfo, this.mViewFlags & 12);
            if (attachInfo.mKeepScreenOn) {
                needGlobalAttributesUpdate(true);
            }
            attachInfo.mKeepScreenOn = z2;
        }
        if (view.isLayoutDirectionInherited()) {
            view.resetRtlProperties();
        }
        dispatchViewAdded(view);
        if ((view.mViewFlags & 4194304) == 4194304) {
            this.mGroupFlags |= 65536;
        }
        if (view.hasTransientState()) {
            childHasTransientStateChanged(view, true);
        }
        if (view.getVisibility() != 8) {
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
        if (this.mTransientIndices != null) {
            int size = this.mTransientIndices.size();
            for (int i2 = 0; i2 < size; i2++) {
                int iIntValue = this.mTransientIndices.get(i2).intValue();
                if (i <= iIntValue) {
                    this.mTransientIndices.set(i2, Integer.valueOf(iIntValue + 1));
                }
            }
        }
        if (this.mCurrentDragStartEvent != null && view.getVisibility() == 0) {
            notifyChildOfDragStart(view);
        }
        if (view.hasDefaultFocus()) {
            setDefaultFocus(view);
        }
        touchAccessibilityNodeProviderIfNeeded(view);
    }

    private void touchAccessibilityNodeProviderIfNeeded(View view) {
        if (this.mContext.isAutofillCompatibilityEnabled()) {
            view.getAccessibilityNodeProvider();
        }
    }

    private void addInArray(View view, int i) {
        View[] viewArr = this.mChildren;
        int i2 = this.mChildrenCount;
        int length = viewArr.length;
        if (i == i2) {
            if (length == i2) {
                this.mChildren = new View[length + 12];
                System.arraycopy(viewArr, 0, this.mChildren, 0, length);
                viewArr = this.mChildren;
            }
            int i3 = this.mChildrenCount;
            this.mChildrenCount = i3 + 1;
            viewArr[i3] = view;
            return;
        }
        if (i < i2) {
            if (length == i2) {
                this.mChildren = new View[length + 12];
                System.arraycopy(viewArr, 0, this.mChildren, 0, i);
                System.arraycopy(viewArr, i, this.mChildren, i + 1, i2 - i);
                viewArr = this.mChildren;
            } else {
                System.arraycopy(viewArr, i, viewArr, i + 1, i2 - i);
            }
            viewArr[i] = view;
            this.mChildrenCount++;
            if (this.mLastTouchDownIndex >= i) {
                this.mLastTouchDownIndex++;
                return;
            }
            return;
        }
        throw new IndexOutOfBoundsException("index=" + i + " count=" + i2);
    }

    private void removeFromArray(int i) {
        View[] viewArr = this.mChildren;
        if (this.mTransitioningViews == null || !this.mTransitioningViews.contains(viewArr[i])) {
            viewArr[i].mParent = null;
        }
        int i2 = this.mChildrenCount;
        if (i == i2 - 1) {
            int i3 = this.mChildrenCount - 1;
            this.mChildrenCount = i3;
            viewArr[i3] = null;
        } else if (i >= 0 && i < i2) {
            System.arraycopy(viewArr, i + 1, viewArr, i, (i2 - i) - 1);
            int i4 = this.mChildrenCount - 1;
            this.mChildrenCount = i4;
            viewArr[i4] = null;
        } else {
            throw new IndexOutOfBoundsException();
        }
        if (this.mLastTouchDownIndex == i) {
            this.mLastTouchDownTime = 0L;
            this.mLastTouchDownIndex = -1;
        } else if (this.mLastTouchDownIndex > i) {
            this.mLastTouchDownIndex--;
        }
    }

    private void removeFromArray(int i, int i2) {
        View[] viewArr = this.mChildren;
        int i3 = this.mChildrenCount;
        int iMax = Math.max(0, i);
        int iMin = Math.min(i3, i2 + iMax);
        if (iMax == iMin) {
            return;
        }
        if (iMin == i3) {
            for (int i4 = iMax; i4 < iMin; i4++) {
                viewArr[i4].mParent = null;
                viewArr[i4] = null;
            }
        } else {
            for (int i5 = iMax; i5 < iMin; i5++) {
                viewArr[i5].mParent = null;
            }
            System.arraycopy(viewArr, iMin, viewArr, iMax, i3 - iMin);
            for (int i6 = i3 - (iMin - iMax); i6 < i3; i6++) {
                viewArr[i6] = null;
            }
        }
        this.mChildrenCount -= iMin - iMax;
    }

    private void bindLayoutAnimation(View view) {
        view.setAnimation(this.mLayoutAnimationController.getAnimationForView(view));
    }

    protected void attachLayoutAnimationParameters(View view, LayoutParams layoutParams, int i, int i2) {
        LayoutAnimationController.AnimationParameters animationParameters = layoutParams.layoutAnimationParameters;
        if (animationParameters == null) {
            animationParameters = new LayoutAnimationController.AnimationParameters();
            layoutParams.layoutAnimationParameters = animationParameters;
        }
        animationParameters.count = i2;
        animationParameters.index = i;
    }

    @Override
    public void removeView(View view) {
        if (removeViewInternal(view)) {
            requestLayout();
            invalidate(true);
        }
    }

    public void removeViewInLayout(View view) {
        removeViewInternal(view);
    }

    public void removeViewsInLayout(int i, int i2) {
        removeViewsInternal(i, i2);
    }

    public void removeViewAt(int i) {
        removeViewInternal(i, getChildAt(i));
        requestLayout();
        invalidate(true);
    }

    public void removeViews(int i, int i2) {
        removeViewsInternal(i, i2);
        requestLayout();
        invalidate(true);
    }

    private boolean removeViewInternal(View view) {
        int iIndexOfChild = indexOfChild(view);
        if (iIndexOfChild >= 0) {
            removeViewInternal(iIndexOfChild, view);
            return true;
        }
        return false;
    }

    private void removeViewInternal(int i, View view) {
        boolean z;
        int size;
        if (this.mTransition != null) {
            this.mTransition.removeChild(this, view);
        }
        if (view == this.mFocused) {
            view.unFocus(null);
            z = true;
        } else {
            z = false;
        }
        if (view == this.mFocusedInCluster) {
            clearFocusedInCluster(view);
        }
        view.clearAccessibilityFocus();
        cancelTouchTarget(view);
        cancelHoverTarget(view);
        if (view.getAnimation() != null || (this.mTransitioningViews != null && this.mTransitioningViews.contains(view))) {
            addDisappearingView(view);
        } else if (view.mAttachInfo != null) {
            view.dispatchDetachedFromWindow();
        }
        if (view.hasTransientState()) {
            childHasTransientStateChanged(view, false);
        }
        needGlobalAttributesUpdate(false);
        removeFromArray(i);
        if (view.hasUnhandledKeyListener()) {
            decrementChildUnhandledKeyListeners();
        }
        if (view == this.mDefaultFocus) {
            clearDefaultFocus(view);
        }
        if (z) {
            clearChildFocus(view);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(this);
            }
        }
        dispatchViewRemoved(view);
        if (view.getVisibility() != 8) {
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
        if (this.mTransientIndices != null) {
            size = this.mTransientIndices.size();
        } else {
            size = 0;
        }
        for (int i2 = 0; i2 < size; i2++) {
            int iIntValue = this.mTransientIndices.get(i2).intValue();
            if (i < iIntValue) {
                this.mTransientIndices.set(i2, Integer.valueOf(iIntValue - 1));
            }
        }
        if (this.mCurrentDragStartEvent != null) {
            this.mChildrenInterestedInDrag.remove(view);
        }
    }

    public void setLayoutTransition(LayoutTransition layoutTransition) {
        if (this.mTransition != null) {
            LayoutTransition layoutTransition2 = this.mTransition;
            layoutTransition2.cancel();
            layoutTransition2.removeTransitionListener(this.mLayoutTransitionListener);
        }
        this.mTransition = layoutTransition;
        if (this.mTransition != null) {
            this.mTransition.addTransitionListener(this.mLayoutTransitionListener);
        }
    }

    public LayoutTransition getLayoutTransition() {
        return this.mTransition;
    }

    private void removeViewsInternal(int i, int i2) {
        int i3 = i + i2;
        if (i < 0 || i2 < 0 || i3 > this.mChildrenCount) {
            throw new IndexOutOfBoundsException();
        }
        View view = this.mFocused;
        boolean z = this.mAttachInfo != null;
        View[] viewArr = this.mChildren;
        boolean z2 = false;
        View view2 = null;
        for (int i4 = i; i4 < i3; i4++) {
            View view3 = viewArr[i4];
            if (this.mTransition != null) {
                this.mTransition.removeChild(this, view3);
            }
            if (view3 == view) {
                view3.unFocus(null);
                z2 = true;
            }
            if (view3 == this.mDefaultFocus) {
                view2 = view3;
            }
            if (view3 == this.mFocusedInCluster) {
                clearFocusedInCluster(view3);
            }
            view3.clearAccessibilityFocus();
            cancelTouchTarget(view3);
            cancelHoverTarget(view3);
            if (view3.getAnimation() != null || (this.mTransitioningViews != null && this.mTransitioningViews.contains(view3))) {
                addDisappearingView(view3);
            } else if (z) {
                view3.dispatchDetachedFromWindow();
            }
            if (view3.hasTransientState()) {
                childHasTransientStateChanged(view3, false);
            }
            needGlobalAttributesUpdate(false);
            dispatchViewRemoved(view3);
        }
        removeFromArray(i, i2);
        if (view2 != null) {
            clearDefaultFocus(view2);
        }
        if (z2) {
            clearChildFocus(view);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(view);
            }
        }
    }

    public void removeAllViews() {
        removeAllViewsInLayout();
        requestLayout();
        invalidate(true);
    }

    public void removeAllViewsInLayout() {
        int i = this.mChildrenCount;
        if (i <= 0) {
            return;
        }
        View[] viewArr = this.mChildren;
        this.mChildrenCount = 0;
        View view = this.mFocused;
        boolean z = this.mAttachInfo != null;
        needGlobalAttributesUpdate(false);
        boolean z2 = false;
        for (int i2 = i - 1; i2 >= 0; i2--) {
            View view2 = viewArr[i2];
            if (this.mTransition != null) {
                this.mTransition.removeChild(this, view2);
            }
            if (view2 == view) {
                view2.unFocus(null);
                z2 = true;
            }
            view2.clearAccessibilityFocus();
            cancelTouchTarget(view2);
            cancelHoverTarget(view2);
            if (view2.getAnimation() != null || (this.mTransitioningViews != null && this.mTransitioningViews.contains(view2))) {
                addDisappearingView(view2);
            } else if (z) {
                view2.dispatchDetachedFromWindow();
            }
            if (view2.hasTransientState()) {
                childHasTransientStateChanged(view2, false);
            }
            dispatchViewRemoved(view2);
            view2.mParent = null;
            viewArr[i2] = null;
        }
        if (this.mDefaultFocus != null) {
            clearDefaultFocus(this.mDefaultFocus);
        }
        if (this.mFocusedInCluster != null) {
            clearFocusedInCluster(this.mFocusedInCluster);
        }
        if (z2) {
            clearChildFocus(view);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(view);
            }
        }
    }

    protected void removeDetachedView(View view, boolean z) {
        if (this.mTransition != null) {
            this.mTransition.removeChild(this, view);
        }
        if (view == this.mFocused) {
            view.clearFocus();
        }
        if (view == this.mDefaultFocus) {
            clearDefaultFocus(view);
        }
        if (view == this.mFocusedInCluster) {
            clearFocusedInCluster(view);
        }
        view.clearAccessibilityFocus();
        cancelTouchTarget(view);
        cancelHoverTarget(view);
        if ((z && view.getAnimation() != null) || (this.mTransitioningViews != null && this.mTransitioningViews.contains(view))) {
            addDisappearingView(view);
        } else if (view.mAttachInfo != null) {
            view.dispatchDetachedFromWindow();
        }
        if (view.hasTransientState()) {
            childHasTransientStateChanged(view, false);
        }
        dispatchViewRemoved(view);
    }

    protected void attachViewToParent(View view, int i, LayoutParams layoutParams) {
        view.mLayoutParams = layoutParams;
        if (i < 0) {
            i = this.mChildrenCount;
        }
        addInArray(view, i);
        view.mParent = this;
        view.mPrivateFlags = (view.mPrivateFlags & (-6291457) & (-32769)) | 32 | Integer.MIN_VALUE;
        this.mPrivateFlags |= Integer.MIN_VALUE;
        if (view.hasFocus()) {
            requestChildFocus(view, view.findFocus());
        }
        dispatchVisibilityAggregated(isAttachedToWindow() && getWindowVisibility() == 0 && isShown());
        notifySubtreeAccessibilityStateChangedIfNeeded();
    }

    protected void detachViewFromParent(View view) {
        removeFromArray(indexOfChild(view));
    }

    protected void detachViewFromParent(int i) {
        removeFromArray(i);
    }

    protected void detachViewsFromParent(int i, int i2) {
        removeFromArray(i, i2);
    }

    protected void detachAllViewsFromParent() {
        int i = this.mChildrenCount;
        if (i <= 0) {
            return;
        }
        View[] viewArr = this.mChildren;
        this.mChildrenCount = 0;
        for (int i2 = i - 1; i2 >= 0; i2--) {
            viewArr[i2].mParent = null;
            viewArr[i2] = null;
        }
    }

    @Override
    public void onDescendantInvalidated(View view, View view2) {
        this.mPrivateFlags |= view2.mPrivateFlags & 64;
        if ((view2.mPrivateFlags & (-6291457)) != 0) {
            this.mPrivateFlags = (this.mPrivateFlags & (-6291457)) | 2097152;
            this.mPrivateFlags &= -32769;
        }
        if (this.mLayerType == 1) {
            this.mPrivateFlags |= -2145386496;
            view2 = this;
        }
        if (this.mParent != null) {
            this.mParent.onDescendantInvalidated(this, view2);
        }
    }

    @Override
    @Deprecated
    public final void invalidateChild(View view, Rect rect) {
        View.AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null && attachInfo.mHardwareAccelerated) {
            onDescendantInvalidated(view, view);
            return;
        }
        if (attachInfo != null) {
            boolean z = (view.mPrivateFlags & 64) != 0;
            Matrix matrix = view.getMatrix();
            int i = view.isOpaque() && !z && view.getAnimation() == null && matrix.isIdentity() ? 4194304 : 2097152;
            if (view.mLayerType != 0) {
                this.mPrivateFlags |= Integer.MIN_VALUE;
                this.mPrivateFlags &= -32769;
            }
            int[] iArr = attachInfo.mInvalidateChildLocation;
            iArr[0] = view.mLeft;
            iArr[1] = view.mTop;
            if (!matrix.isIdentity() || (this.mGroupFlags & 2048) != 0) {
                RectF rectF = attachInfo.mTmpTransformRect;
                rectF.set(rect);
                if ((this.mGroupFlags & 2048) != 0) {
                    Transformation transformation = attachInfo.mTmpTransformation;
                    if (getChildStaticTransformation(view, transformation)) {
                        Matrix matrix2 = attachInfo.mTmpMatrix;
                        matrix2.set(transformation.getMatrix());
                        if (!matrix.isIdentity()) {
                            matrix2.preConcat(matrix);
                        }
                        matrix = matrix2;
                    }
                }
                matrix.mapRect(rectF);
                rect.set((int) Math.floor(rectF.left), (int) Math.floor(rectF.top), (int) Math.ceil(rectF.right), (int) Math.ceil(rectF.bottom));
            }
            ViewParent viewParentInvalidateChildInParent = this;
            do {
                View view2 = viewParentInvalidateChildInParent instanceof View ? (View) viewParentInvalidateChildInParent : null;
                if (z) {
                    if (view2 != null) {
                        view2.mPrivateFlags |= 64;
                    } else if (viewParentInvalidateChildInParent instanceof ViewRootImpl) {
                        ((ViewRootImpl) viewParentInvalidateChildInParent).mIsAnimating = true;
                    }
                }
                if (view2 != null) {
                    if ((view2.mViewFlags & 12288) != 0 && view2.getSolidColor() == 0) {
                        i = 2097152;
                    }
                    if ((view2.mPrivateFlags & IntentFilter.MATCH_CATEGORY_TYPE) != 2097152) {
                        view2.mPrivateFlags = (view2.mPrivateFlags & (-6291457)) | i;
                    }
                }
                viewParentInvalidateChildInParent = viewParentInvalidateChildInParent.invalidateChildInParent(iArr, rect);
                if (view2 != null) {
                    Matrix matrix3 = view2.getMatrix();
                    if (!matrix3.isIdentity()) {
                        RectF rectF2 = attachInfo.mTmpTransformRect;
                        rectF2.set(rect);
                        matrix3.mapRect(rectF2);
                        rect.set((int) Math.floor(rectF2.left), (int) Math.floor(rectF2.top), (int) Math.ceil(rectF2.right), (int) Math.ceil(rectF2.bottom));
                    }
                }
            } while (viewParentInvalidateChildInParent != null);
        }
    }

    @Override
    @Deprecated
    public ViewParent invalidateChildInParent(int[] iArr, Rect rect) {
        if ((this.mPrivateFlags & MeshConstants.MESH_MSG_CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_ADD) != 0) {
            if ((this.mGroupFlags & 144) != 128) {
                rect.offset(iArr[0] - this.mScrollX, iArr[1] - this.mScrollY);
                if ((this.mGroupFlags & 1) == 0) {
                    rect.union(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
                }
                int i = this.mLeft;
                int i2 = this.mTop;
                if ((this.mGroupFlags & 1) == 1 && !rect.intersect(0, 0, this.mRight - i, this.mBottom - i2)) {
                    rect.setEmpty();
                }
                iArr[0] = i;
                iArr[1] = i2;
            } else {
                if ((this.mGroupFlags & 1) == 1) {
                    rect.set(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
                } else {
                    rect.union(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
                }
                iArr[0] = this.mLeft;
                iArr[1] = this.mTop;
                this.mPrivateFlags &= -33;
            }
            this.mPrivateFlags &= -32769;
            if (this.mLayerType != 0) {
                this.mPrivateFlags |= Integer.MIN_VALUE;
            }
            return this.mParent;
        }
        return null;
    }

    public final void offsetDescendantRectToMyCoords(View view, Rect rect) {
        offsetRectBetweenParentAndChild(view, rect, true, false);
    }

    public final void offsetRectIntoDescendantCoords(View view, Rect rect) {
        offsetRectBetweenParentAndChild(view, rect, false, false);
    }

    void offsetRectBetweenParentAndChild(View view, Rect rect, boolean z, boolean z2) {
        if (view == this) {
            return;
        }
        ViewParent viewParent = view.mParent;
        while (viewParent != null && (viewParent instanceof View) && viewParent != this) {
            if (z) {
                rect.offset(view.mLeft - view.mScrollX, view.mTop - view.mScrollY);
                if (z2) {
                    View view2 = (View) viewParent;
                    if (!rect.intersect(0, 0, view2.mRight - view2.mLeft, view2.mBottom - view2.mTop)) {
                        rect.setEmpty();
                    }
                }
            } else {
                if (z2) {
                    View view3 = (View) viewParent;
                    if (!rect.intersect(0, 0, view3.mRight - view3.mLeft, view3.mBottom - view3.mTop)) {
                        rect.setEmpty();
                    }
                }
                rect.offset(view.mScrollX - view.mLeft, view.mScrollY - view.mTop);
            }
            view = (View) viewParent;
            viewParent = view.mParent;
        }
        if (viewParent == this) {
            if (z) {
                rect.offset(view.mLeft - view.mScrollX, view.mTop - view.mScrollY);
                return;
            } else {
                rect.offset(view.mScrollX - view.mLeft, view.mScrollY - view.mTop);
                return;
            }
        }
        Log.e(TAG, "parameter must be a descendant of this view, this = " + this + " descendant = " + view + " theParent = " + viewParent);
        debug();
        throw new IllegalArgumentException("parameter must be a descendant of this view");
    }

    public void offsetChildrenTopAndBottom(int i) {
        int i2 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        boolean z = false;
        for (int i3 = 0; i3 < i2; i3++) {
            View view = viewArr[i3];
            view.mTop += i;
            view.mBottom += i;
            if (view.mRenderNode != null) {
                view.mRenderNode.offsetTopAndBottom(i);
                z = true;
            }
        }
        if (z) {
            invalidateViewProperty(false, false);
        }
        notifySubtreeAccessibilityStateChangedIfNeeded();
    }

    @Override
    public boolean getChildVisibleRect(View view, Rect rect, Point point) {
        return getChildVisibleRect(view, rect, point, false);
    }

    public boolean getChildVisibleRect(View view, Rect rect, Point point, boolean z) {
        RectF rectF = this.mAttachInfo != null ? this.mAttachInfo.mTmpTransformRect : new RectF();
        rectF.set(rect);
        if (!view.hasIdentityMatrix()) {
            view.getMatrix().mapRect(rectF);
        }
        int i = view.mLeft - this.mScrollX;
        int i2 = view.mTop - this.mScrollY;
        rectF.offset(i, i2);
        boolean zIntersect = true;
        if (point != null) {
            if (!view.hasIdentityMatrix()) {
                float[] fArr = this.mAttachInfo != null ? this.mAttachInfo.mTmpTransformLocation : new float[2];
                fArr[0] = point.x;
                fArr[1] = point.y;
                view.getMatrix().mapPoints(fArr);
                point.x = Math.round(fArr[0]);
                point.y = Math.round(fArr[1]);
            }
            point.x += i;
            point.y += i2;
        }
        int i3 = this.mRight - this.mLeft;
        int i4 = this.mBottom - this.mTop;
        if (this.mParent == null || ((this.mParent instanceof ViewGroup) && ((ViewGroup) this.mParent).getClipChildren())) {
            zIntersect = rectF.intersect(0.0f, 0.0f, i3, i4);
        }
        if ((z || zIntersect) && (this.mGroupFlags & 34) == 34) {
            zIntersect = rectF.intersect(this.mPaddingLeft, this.mPaddingTop, i3 - this.mPaddingRight, i4 - this.mPaddingBottom);
        }
        if ((z || zIntersect) && this.mClipBounds != null) {
            zIntersect = rectF.intersect(this.mClipBounds.left, this.mClipBounds.top, this.mClipBounds.right, this.mClipBounds.bottom);
        }
        rect.set((int) Math.floor(rectF.left), (int) Math.floor(rectF.top), (int) Math.ceil(rectF.right), (int) Math.ceil(rectF.bottom));
        if ((z || zIntersect) && this.mParent != null) {
            if (this.mParent instanceof ViewGroup) {
                return ((ViewGroup) this.mParent).getChildVisibleRect(this, rect, point, z);
            }
            return this.mParent.getChildVisibleRect(this, rect, point);
        }
        return zIntersect;
    }

    @Override
    public final void layout(int i, int i2, int i3, int i4) {
        if (!this.mSuppressLayout && (this.mTransition == null || !this.mTransition.isChangingLayout())) {
            if (this.mTransition != null) {
                this.mTransition.layoutChange(this);
            }
            super.layout(i, i2, i3, i4);
            return;
        }
        this.mLayoutCalledWhileSuppressed = true;
    }

    protected boolean canAnimate() {
        return this.mLayoutAnimationController != null;
    }

    public void startLayoutAnimation() {
        if (this.mLayoutAnimationController != null) {
            this.mGroupFlags |= 8;
            requestLayout();
        }
    }

    public void scheduleLayoutAnimation() {
        this.mGroupFlags |= 8;
    }

    public void setLayoutAnimation(LayoutAnimationController layoutAnimationController) {
        this.mLayoutAnimationController = layoutAnimationController;
        if (this.mLayoutAnimationController != null) {
            this.mGroupFlags |= 8;
        }
    }

    public LayoutAnimationController getLayoutAnimation() {
        return this.mLayoutAnimationController;
    }

    @Deprecated
    public boolean isAnimationCacheEnabled() {
        return (this.mGroupFlags & 64) == 64;
    }

    @Deprecated
    public void setAnimationCacheEnabled(boolean z) {
        setBooleanFlag(64, z);
    }

    @Deprecated
    public boolean isAlwaysDrawnWithCacheEnabled() {
        return (this.mGroupFlags & 16384) == 16384;
    }

    @Deprecated
    public void setAlwaysDrawnWithCacheEnabled(boolean z) {
        setBooleanFlag(16384, z);
    }

    @Deprecated
    protected boolean isChildrenDrawnWithCacheEnabled() {
        return (this.mGroupFlags & 32768) == 32768;
    }

    @Deprecated
    protected void setChildrenDrawnWithCacheEnabled(boolean z) {
        setBooleanFlag(32768, z);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    protected boolean isChildrenDrawingOrderEnabled() {
        return (this.mGroupFlags & 1024) == 1024;
    }

    protected void setChildrenDrawingOrderEnabled(boolean z) {
        setBooleanFlag(1024, z);
    }

    private boolean hasBooleanFlag(int i) {
        return (this.mGroupFlags & i) == i;
    }

    private void setBooleanFlag(int i, boolean z) {
        if (z) {
            this.mGroupFlags = i | this.mGroupFlags;
        } else {
            this.mGroupFlags = (~i) & this.mGroupFlags;
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing", mapping = {@ViewDebug.IntToString(from = 0, to = KeyProperties.DIGEST_NONE), @ViewDebug.IntToString(from = 1, to = "ANIMATION"), @ViewDebug.IntToString(from = 2, to = "SCROLLING"), @ViewDebug.IntToString(from = 3, to = "ALL")})
    @Deprecated
    public int getPersistentDrawingCache() {
        return this.mPersistentDrawingCache;
    }

    @Deprecated
    public void setPersistentDrawingCache(int i) {
        this.mPersistentDrawingCache = i & 3;
    }

    private void setLayoutMode(int i, boolean z) {
        this.mLayoutMode = i;
        setBooleanFlag(8388608, z);
    }

    @Override
    void invalidateInheritedLayoutMode(int i) {
        if (this.mLayoutMode == -1 || this.mLayoutMode == i || hasBooleanFlag(8388608)) {
            return;
        }
        setLayoutMode(-1, false);
        int childCount = getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            getChildAt(i2).invalidateInheritedLayoutMode(i);
        }
    }

    public int getLayoutMode() {
        if (this.mLayoutMode == -1) {
            setLayoutMode(this.mParent instanceof ViewGroup ? ((ViewGroup) this.mParent).getLayoutMode() : LAYOUT_MODE_DEFAULT, false);
        }
        return this.mLayoutMode;
    }

    public void setLayoutMode(int i) {
        if (this.mLayoutMode != i) {
            invalidateInheritedLayoutMode(i);
            setLayoutMode(i, i != -1);
            requestLayout();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    protected LayoutParams generateLayoutParams(LayoutParams layoutParams) {
        return layoutParams;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected void debug(int i) {
        super.debug(i);
        if (this.mFocused != null) {
            Log.d("View", debugIndent(i) + "mFocused");
            this.mFocused.debug(i + 1);
        }
        if (this.mDefaultFocus != null) {
            Log.d("View", debugIndent(i) + "mDefaultFocus");
            this.mDefaultFocus.debug(i + 1);
        }
        if (this.mFocusedInCluster != null) {
            Log.d("View", debugIndent(i) + "mFocusedInCluster");
            this.mFocusedInCluster.debug(i + 1);
        }
        if (this.mChildrenCount != 0) {
            Log.d("View", debugIndent(i) + "{");
        }
        int i2 = this.mChildrenCount;
        for (int i3 = 0; i3 < i2; i3++) {
            this.mChildren[i3].debug(i + 1);
        }
        if (this.mChildrenCount != 0) {
            Log.d("View", debugIndent(i) + "}");
        }
    }

    public int indexOfChild(View view) {
        int i = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (viewArr[i2] == view) {
                return i2;
            }
        }
        return -1;
    }

    public int getChildCount() {
        return this.mChildrenCount;
    }

    public View getChildAt(int i) {
        if (i < 0 || i >= this.mChildrenCount) {
            return null;
        }
        return this.mChildren[i];
    }

    protected void measureChildren(int i, int i2) {
        int i3 = this.mChildrenCount;
        View[] viewArr = this.mChildren;
        for (int i4 = 0; i4 < i3; i4++) {
            View view = viewArr[i4];
            if ((view.mViewFlags & 12) != 8) {
                measureChild(view, i, i2);
            }
        }
    }

    protected void measureChild(View view, int i, int i2) {
        LayoutParams layoutParams = view.getLayoutParams();
        int childMeasureSpec = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight, layoutParams.width);
        int childMeasureSpec2 = getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom, layoutParams.height);
        if (ViewDebugManager.DEBUG_LAYOUT) {
            ViewDebugManager.getInstance().debugViewGroupChildMeasure(view, this, layoutParams, -1, -1);
        }
        view.measure(childMeasureSpec, childMeasureSpec2);
    }

    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view.getLayoutParams();
        int childMeasureSpec = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + i2, marginLayoutParams.width);
        int childMeasureSpec2 = getChildMeasureSpec(i3, this.mPaddingTop + this.mPaddingBottom + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height);
        if (ViewDebugManager.DEBUG_LAYOUT) {
            ViewDebugManager.getInstance().debugViewGroupChildMeasure(view, (View) this, marginLayoutParams, i2, i4);
        }
        view.measure(childMeasureSpec, childMeasureSpec2);
    }

    public static int getChildMeasureSpec(int i, int i2, int i3) {
        int mode = View.MeasureSpec.getMode(i);
        int iMax = Math.max(0, View.MeasureSpec.getSize(i) - i2);
        int i4 = 1073741824;
        if (mode != Integer.MIN_VALUE) {
            if (mode != 0) {
                if (mode == 1073741824) {
                    if (i3 < 0) {
                        if (i3 == -1) {
                            i3 = iMax;
                        } else if (i3 == -2) {
                            i3 = iMax;
                            i4 = Integer.MIN_VALUE;
                        } else {
                            i3 = 0;
                            i4 = 0;
                        }
                    }
                }
            } else if (i3 < 0) {
                if (i3 == -1) {
                    i3 = View.sUseZeroUnspecifiedMeasureSpec ? 0 : iMax;
                } else {
                    if (i3 == -2) {
                        i3 = View.sUseZeroUnspecifiedMeasureSpec ? 0 : iMax;
                    }
                    i3 = 0;
                    i4 = 0;
                }
                i4 = 0;
            }
        } else if (i3 < 0) {
            if (i3 != -1 && i3 != -2) {
            }
        }
        return View.MeasureSpec.makeMeasureSpec(i3, i4);
    }

    public void clearDisappearingChildren() {
        ArrayList<View> arrayList = this.mDisappearingChildren;
        if (arrayList != null) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                View view = arrayList.get(i);
                if (view.mAttachInfo != null) {
                    view.dispatchDetachedFromWindow();
                }
                view.clearAnimation();
            }
            arrayList.clear();
            invalidate();
        }
    }

    private void addDisappearingView(View view) {
        ArrayList<View> arrayList = this.mDisappearingChildren;
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mDisappearingChildren = arrayList;
        }
        arrayList.add(view);
    }

    void finishAnimatingView(View view, Animation animation) {
        ArrayList<View> arrayList = this.mDisappearingChildren;
        if (arrayList != null && arrayList.contains(view)) {
            arrayList.remove(view);
            if (view.mAttachInfo != null) {
                view.dispatchDetachedFromWindow();
            }
            view.clearAnimation();
            this.mGroupFlags |= 4;
        }
        if (animation != null && !animation.getFillAfter()) {
            view.clearAnimation();
        }
        if ((view.mPrivateFlags & 65536) == 65536) {
            view.onAnimationEnd();
            view.mPrivateFlags &= -65537;
            this.mGroupFlags |= 4;
        }
    }

    boolean isViewTransitioning(View view) {
        return this.mTransitioningViews != null && this.mTransitioningViews.contains(view);
    }

    public void startViewTransition(View view) {
        if (view.mParent == this) {
            if (this.mTransitioningViews == null) {
                this.mTransitioningViews = new ArrayList<>();
            }
            this.mTransitioningViews.add(view);
        }
    }

    public void endViewTransition(View view) {
        if (this.mTransitioningViews != null) {
            this.mTransitioningViews.remove(view);
            ArrayList<View> arrayList = this.mDisappearingChildren;
            if (arrayList != null && arrayList.contains(view)) {
                arrayList.remove(view);
                if (this.mVisibilityChangingChildren != null && this.mVisibilityChangingChildren.contains(view)) {
                    this.mVisibilityChangingChildren.remove(view);
                } else {
                    if (view.mAttachInfo != null) {
                        view.dispatchDetachedFromWindow();
                    }
                    if (view.mParent != null) {
                        view.mParent = null;
                    }
                }
                invalidate();
            }
        }
    }

    public void suppressLayout(boolean z) {
        this.mSuppressLayout = z;
        if (!z && this.mLayoutCalledWhileSuppressed) {
            requestLayout();
            this.mLayoutCalledWhileSuppressed = false;
        }
    }

    public boolean isLayoutSuppressed() {
        return this.mSuppressLayout;
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        boolean z;
        boolean z2 = (this.mPrivateFlags & 512) == 0;
        if (z2 && region == null) {
            return true;
        }
        super.gatherTransparentRegion(region);
        int i = this.mChildrenCount;
        if (i > 0) {
            ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
            boolean z3 = arrayListBuildOrderedChildList == null && isChildrenDrawingOrderEnabled();
            View[] viewArr = this.mChildren;
            z = true;
            for (int i2 = 0; i2 < i; i2++) {
                View andVerifyPreorderedView = getAndVerifyPreorderedView(arrayListBuildOrderedChildList, viewArr, getAndVerifyPreorderedIndex(i, i2, z3));
                if (((andVerifyPreorderedView.mViewFlags & 12) == 0 || andVerifyPreorderedView.getAnimation() != null) && !andVerifyPreorderedView.gatherTransparentRegion(region)) {
                    z = false;
                }
            }
            if (arrayListBuildOrderedChildList != null) {
                arrayListBuildOrderedChildList.clear();
            }
        } else {
            z = true;
        }
        return z2 || z;
    }

    @Override
    public void requestTransparentRegion(View view) {
        if (view != null) {
            view.mPrivateFlags |= 512;
            if (this.mParent != null) {
                this.mParent.requestTransparentRegion(this);
            }
        }
    }

    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets windowInsets) {
        WindowInsets windowInsetsDispatchApplyWindowInsets = super.dispatchApplyWindowInsets(windowInsets);
        if (!windowInsetsDispatchApplyWindowInsets.isConsumed()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                windowInsetsDispatchApplyWindowInsets = getChildAt(i).dispatchApplyWindowInsets(windowInsetsDispatchApplyWindowInsets);
                if (windowInsetsDispatchApplyWindowInsets.isConsumed()) {
                    break;
                }
            }
        }
        return windowInsetsDispatchApplyWindowInsets;
    }

    public Animation.AnimationListener getLayoutAnimationListener() {
        return this.mAnimationListener;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if ((this.mGroupFlags & 65536) != 0) {
            if ((this.mGroupFlags & 8192) != 0) {
                throw new IllegalStateException("addStateFromChildren cannot be enabled if a child has duplicateParentState set to true");
            }
            View[] viewArr = this.mChildren;
            int i = this.mChildrenCount;
            for (int i2 = 0; i2 < i; i2++) {
                View view = viewArr[i2];
                if ((view.mViewFlags & 4194304) != 0) {
                    view.refreshDrawableState();
                }
            }
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        View[] viewArr = this.mChildren;
        int i = this.mChildrenCount;
        for (int i2 = 0; i2 < i; i2++) {
            viewArr[i2].jumpDrawablesToCurrentState();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        if ((this.mGroupFlags & 8192) == 0) {
            return super.onCreateDrawableState(i);
        }
        int childCount = getChildCount();
        int length = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            int[] drawableState = getChildAt(i2).getDrawableState();
            if (drawableState != null) {
                length += drawableState.length;
            }
        }
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + length);
        for (int i3 = 0; i3 < childCount; i3++) {
            int[] drawableState2 = getChildAt(i3).getDrawableState();
            if (drawableState2 != null) {
                iArrOnCreateDrawableState = mergeDrawableStates(iArrOnCreateDrawableState, drawableState2);
            }
        }
        return iArrOnCreateDrawableState;
    }

    public void setAddStatesFromChildren(boolean z) {
        if (z) {
            this.mGroupFlags |= 8192;
        } else {
            this.mGroupFlags &= -8193;
        }
        refreshDrawableState();
    }

    public boolean addStatesFromChildren() {
        return (this.mGroupFlags & 8192) != 0;
    }

    @Override
    public void childDrawableStateChanged(View view) {
        if ((this.mGroupFlags & 8192) != 0) {
            refreshDrawableState();
        }
    }

    public void setLayoutAnimationListener(Animation.AnimationListener animationListener) {
        this.mAnimationListener = animationListener;
    }

    public void requestTransitionStart(LayoutTransition layoutTransition) {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.requestTransitionStart(layoutTransition);
        }
    }

    @Override
    public boolean resolveRtlPropertiesIfNeeded() {
        boolean zResolveRtlPropertiesIfNeeded = super.resolveRtlPropertiesIfNeeded();
        if (zResolveRtlPropertiesIfNeeded) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.isLayoutDirectionInherited()) {
                    childAt.resolveRtlPropertiesIfNeeded();
                }
            }
        }
        return zResolveRtlPropertiesIfNeeded;
    }

    @Override
    public boolean resolveLayoutDirection() {
        boolean zResolveLayoutDirection = super.resolveLayoutDirection();
        if (zResolveLayoutDirection) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.isLayoutDirectionInherited()) {
                    childAt.resolveLayoutDirection();
                }
            }
        }
        return zResolveLayoutDirection;
    }

    @Override
    public boolean resolveTextDirection() {
        boolean zResolveTextDirection = super.resolveTextDirection();
        if (zResolveTextDirection) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.isTextDirectionInherited()) {
                    childAt.resolveTextDirection();
                }
            }
        }
        return zResolveTextDirection;
    }

    @Override
    public boolean resolveTextAlignment() {
        boolean zResolveTextAlignment = super.resolveTextAlignment();
        if (zResolveTextAlignment) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.isTextAlignmentInherited()) {
                    childAt.resolveTextAlignment();
                }
            }
        }
        return zResolveTextAlignment;
    }

    @Override
    public void resolvePadding() {
        super.resolvePadding();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isLayoutDirectionInherited() && !childAt.isPaddingResolved()) {
                childAt.resolvePadding();
            }
        }
    }

    @Override
    protected void resolveDrawables() {
        super.resolveDrawables();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isLayoutDirectionInherited() && !childAt.areDrawablesResolved()) {
                childAt.resolveDrawables();
            }
        }
    }

    @Override
    public void resolveLayoutParams() {
        super.resolveLayoutParams();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).resolveLayoutParams();
        }
    }

    @Override
    public void resetResolvedLayoutDirection() {
        super.resetResolvedLayoutDirection();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isLayoutDirectionInherited()) {
                childAt.resetResolvedLayoutDirection();
            }
        }
    }

    @Override
    public void resetResolvedTextDirection() {
        super.resetResolvedTextDirection();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isTextDirectionInherited()) {
                childAt.resetResolvedTextDirection();
            }
        }
    }

    @Override
    public void resetResolvedTextAlignment() {
        super.resetResolvedTextAlignment();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isTextAlignmentInherited()) {
                childAt.resetResolvedTextAlignment();
            }
        }
    }

    @Override
    public void resetResolvedPadding() {
        super.resetResolvedPadding();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isLayoutDirectionInherited()) {
                childAt.resetResolvedPadding();
            }
        }
    }

    @Override
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.isLayoutDirectionInherited()) {
                childAt.resetResolvedDrawables();
            }
        }
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    public boolean onStartNestedScroll(View view, View view2, int i) {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View view, View view2, int i) {
        this.mNestedScrollAxes = i;
    }

    @Override
    public void onStopNestedScroll(View view) {
        stopNestedScroll();
        this.mNestedScrollAxes = 0;
    }

    @Override
    public void onNestedScroll(View view, int i, int i2, int i3, int i4) {
        dispatchNestedScroll(i, i2, i3, i4, null);
    }

    @Override
    public void onNestedPreScroll(View view, int i, int i2, int[] iArr) {
        dispatchNestedPreScroll(i, i2, iArr, null);
    }

    @Override
    public boolean onNestedFling(View view, float f, float f2, boolean z) {
        return dispatchNestedFling(f, f2, z);
    }

    @Override
    public boolean onNestedPreFling(View view, float f, float f2) {
        return dispatchNestedPreFling(f, f2);
    }

    public int getNestedScrollAxes() {
        return this.mNestedScrollAxes;
    }

    protected void onSetLayoutParams(View view, LayoutParams layoutParams) {
        requestLayout();
    }

    @Override
    public void captureTransitioningViews(List<View> list) {
        if (getVisibility() != 0) {
            return;
        }
        if (isTransitionGroup()) {
            list.add(this);
            return;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).captureTransitioningViews(list);
        }
    }

    @Override
    public void findNamedViews(Map<String, View> map) {
        if (getVisibility() != 0 && this.mGhostView == null) {
            return;
        }
        super.findNamedViews(map);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).findNamedViews(map);
        }
    }

    @Override
    boolean hasUnhandledKeyListener() {
        return this.mChildUnhandledKeyListeners > 0 || super.hasUnhandledKeyListener();
    }

    void incrementChildUnhandledKeyListeners() {
        this.mChildUnhandledKeyListeners++;
        if (this.mChildUnhandledKeyListeners == 1 && (this.mParent instanceof ViewGroup)) {
            ((ViewGroup) this.mParent).incrementChildUnhandledKeyListeners();
        }
    }

    void decrementChildUnhandledKeyListeners() {
        this.mChildUnhandledKeyListeners--;
        if (this.mChildUnhandledKeyListeners == 0 && (this.mParent instanceof ViewGroup)) {
            ((ViewGroup) this.mParent).decrementChildUnhandledKeyListeners();
        }
    }

    @Override
    View dispatchUnhandledKeyEvent(KeyEvent keyEvent) {
        if (!hasUnhandledKeyListener()) {
            return null;
        }
        ArrayList<View> arrayListBuildOrderedChildList = buildOrderedChildList();
        if (arrayListBuildOrderedChildList != null) {
            try {
                for (int size = arrayListBuildOrderedChildList.size() - 1; size >= 0; size--) {
                    View viewDispatchUnhandledKeyEvent = arrayListBuildOrderedChildList.get(size).dispatchUnhandledKeyEvent(keyEvent);
                    if (viewDispatchUnhandledKeyEvent != null) {
                        return viewDispatchUnhandledKeyEvent;
                    }
                }
            } finally {
                arrayListBuildOrderedChildList.clear();
            }
        } else {
            for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
                View viewDispatchUnhandledKeyEvent2 = getChildAt(childCount).dispatchUnhandledKeyEvent(keyEvent);
                if (viewDispatchUnhandledKeyEvent2 != null) {
                    return viewDispatchUnhandledKeyEvent2;
                }
            }
        }
        if (onUnhandledKeyEvent(keyEvent)) {
            return this;
        }
        return null;
    }

    public static class LayoutParams {

        @Deprecated
        public static final int FILL_PARENT = -1;
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = -1, to = "MATCH_PARENT"), @ViewDebug.IntToString(from = -2, to = "WRAP_CONTENT")})
        public int height;
        public LayoutAnimationController.AnimationParameters layoutAnimationParameters;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = -1, to = "MATCH_PARENT"), @ViewDebug.IntToString(from = -2, to = "WRAP_CONTENT")})
        public int width;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ViewGroup_Layout);
            setBaseAttributes(typedArrayObtainStyledAttributes, 0, 1);
            typedArrayObtainStyledAttributes.recycle();
        }

        public LayoutParams(int i, int i2) {
            this.width = i;
            this.height = i2;
        }

        public LayoutParams(LayoutParams layoutParams) {
            this.width = layoutParams.width;
            this.height = layoutParams.height;
        }

        LayoutParams() {
        }

        protected void setBaseAttributes(TypedArray typedArray, int i, int i2) {
            this.width = typedArray.getLayoutDimension(i, "layout_width");
            this.height = typedArray.getLayoutDimension(i2, "layout_height");
        }

        public void resolveLayoutDirection(int i) {
        }

        public String debug(String str) {
            return str + "ViewGroup.LayoutParams={ width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " }";
        }

        public void onDebugDraw(View view, Canvas canvas, Paint paint) {
        }

        protected static String sizeToString(int i) {
            if (i == -2) {
                return "wrap-content";
            }
            if (i == -1) {
                return "match-parent";
            }
            return String.valueOf(i);
        }

        void encode(ViewHierarchyEncoder viewHierarchyEncoder) {
            viewHierarchyEncoder.beginObject(this);
            encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.endObject();
        }

        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            viewHierarchyEncoder.addProperty("width", this.width);
            viewHierarchyEncoder.addProperty("height", this.height);
        }
    }

    public static class MarginLayoutParams extends LayoutParams {
        public static final int DEFAULT_MARGIN_RELATIVE = Integer.MIN_VALUE;
        private static final int DEFAULT_MARGIN_RESOLVED = 0;
        private static final int LAYOUT_DIRECTION_MASK = 3;
        private static final int LEFT_MARGIN_UNDEFINED_MASK = 4;
        private static final int NEED_RESOLUTION_MASK = 32;
        private static final int RIGHT_MARGIN_UNDEFINED_MASK = 8;
        private static final int RTL_COMPATIBILITY_MODE_MASK = 16;
        private static final int UNDEFINED_MARGIN = Integer.MIN_VALUE;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int bottomMargin;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        private int endMargin;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int leftMargin;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, flagMapping = {@ViewDebug.FlagToString(equals = 3, mask = 3, name = "LAYOUT_DIRECTION"), @ViewDebug.FlagToString(equals = 4, mask = 4, name = "LEFT_MARGIN_UNDEFINED_MASK"), @ViewDebug.FlagToString(equals = 8, mask = 8, name = "RIGHT_MARGIN_UNDEFINED_MASK"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "RTL_COMPATIBILITY_MODE_MASK"), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "NEED_RESOLUTION_MASK")}, formatToHexString = true)
        byte mMarginFlags;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int rightMargin;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        private int startMargin;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
        public int topMargin;

        public MarginLayoutParams(Context context, AttributeSet attributeSet) {
            this.startMargin = Integer.MIN_VALUE;
            this.endMargin = Integer.MIN_VALUE;
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ViewGroup_MarginLayout);
            setBaseAttributes(typedArrayObtainStyledAttributes, 0, 1);
            int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(2, -1);
            if (dimensionPixelSize < 0) {
                int dimensionPixelSize2 = typedArrayObtainStyledAttributes.getDimensionPixelSize(9, -1);
                int dimensionPixelSize3 = typedArrayObtainStyledAttributes.getDimensionPixelSize(10, -1);
                if (dimensionPixelSize2 < 0) {
                    this.leftMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(3, Integer.MIN_VALUE);
                    if (this.leftMargin == Integer.MIN_VALUE) {
                        this.mMarginFlags = (byte) (this.mMarginFlags | 4);
                        this.leftMargin = 0;
                    }
                    this.rightMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(5, Integer.MIN_VALUE);
                    if (this.rightMargin == Integer.MIN_VALUE) {
                        this.mMarginFlags = (byte) (this.mMarginFlags | 8);
                        this.rightMargin = 0;
                    }
                } else {
                    this.leftMargin = dimensionPixelSize2;
                    this.rightMargin = dimensionPixelSize2;
                }
                this.startMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(7, Integer.MIN_VALUE);
                this.endMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(8, Integer.MIN_VALUE);
                if (dimensionPixelSize3 >= 0) {
                    this.topMargin = dimensionPixelSize3;
                    this.bottomMargin = dimensionPixelSize3;
                } else {
                    this.topMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(4, 0);
                    this.bottomMargin = typedArrayObtainStyledAttributes.getDimensionPixelSize(6, 0);
                }
                if (isMarginRelative()) {
                    this.mMarginFlags = (byte) (this.mMarginFlags | 32);
                }
            } else {
                this.leftMargin = dimensionPixelSize;
                this.topMargin = dimensionPixelSize;
                this.rightMargin = dimensionPixelSize;
                this.bottomMargin = dimensionPixelSize;
            }
            boolean zHasRtlSupport = context.getApplicationInfo().hasRtlSupport();
            if (context.getApplicationInfo().targetSdkVersion < 17 || !zHasRtlSupport) {
                this.mMarginFlags = (byte) (this.mMarginFlags | WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK);
            }
            this.mMarginFlags = (byte) (this.mMarginFlags | 0);
            typedArrayObtainStyledAttributes.recycle();
        }

        public MarginLayoutParams(int i, int i2) {
            super(i, i2);
            this.startMargin = Integer.MIN_VALUE;
            this.endMargin = Integer.MIN_VALUE;
            this.mMarginFlags = (byte) (this.mMarginFlags | 4);
            this.mMarginFlags = (byte) (this.mMarginFlags | 8);
            this.mMarginFlags = (byte) (this.mMarginFlags & (-33));
            this.mMarginFlags = (byte) (this.mMarginFlags & (-17));
        }

        public MarginLayoutParams(MarginLayoutParams marginLayoutParams) {
            this.startMargin = Integer.MIN_VALUE;
            this.endMargin = Integer.MIN_VALUE;
            this.width = marginLayoutParams.width;
            this.height = marginLayoutParams.height;
            this.leftMargin = marginLayoutParams.leftMargin;
            this.topMargin = marginLayoutParams.topMargin;
            this.rightMargin = marginLayoutParams.rightMargin;
            this.bottomMargin = marginLayoutParams.bottomMargin;
            this.startMargin = marginLayoutParams.startMargin;
            this.endMargin = marginLayoutParams.endMargin;
            this.mMarginFlags = marginLayoutParams.mMarginFlags;
        }

        public MarginLayoutParams(LayoutParams layoutParams) {
            super(layoutParams);
            this.startMargin = Integer.MIN_VALUE;
            this.endMargin = Integer.MIN_VALUE;
            this.mMarginFlags = (byte) (this.mMarginFlags | 4);
            this.mMarginFlags = (byte) (this.mMarginFlags | 8);
            this.mMarginFlags = (byte) (this.mMarginFlags & (-33));
            this.mMarginFlags = (byte) (this.mMarginFlags & (-17));
        }

        public final void copyMarginsFrom(MarginLayoutParams marginLayoutParams) {
            this.leftMargin = marginLayoutParams.leftMargin;
            this.topMargin = marginLayoutParams.topMargin;
            this.rightMargin = marginLayoutParams.rightMargin;
            this.bottomMargin = marginLayoutParams.bottomMargin;
            this.startMargin = marginLayoutParams.startMargin;
            this.endMargin = marginLayoutParams.endMargin;
            this.mMarginFlags = marginLayoutParams.mMarginFlags;
        }

        public void setMargins(int i, int i2, int i3, int i4) {
            this.leftMargin = i;
            this.topMargin = i2;
            this.rightMargin = i3;
            this.bottomMargin = i4;
            this.mMarginFlags = (byte) (this.mMarginFlags & (-5));
            this.mMarginFlags = (byte) (this.mMarginFlags & (-9));
            if (isMarginRelative()) {
                this.mMarginFlags = (byte) (this.mMarginFlags | 32);
            } else {
                this.mMarginFlags = (byte) (this.mMarginFlags & (-33));
            }
        }

        public void setMarginsRelative(int i, int i2, int i3, int i4) {
            this.startMargin = i;
            this.topMargin = i2;
            this.endMargin = i3;
            this.bottomMargin = i4;
            this.mMarginFlags = (byte) (this.mMarginFlags | 32);
        }

        public void setMarginStart(int i) {
            this.startMargin = i;
            this.mMarginFlags = (byte) (this.mMarginFlags | 32);
        }

        public int getMarginStart() {
            if (this.startMargin != Integer.MIN_VALUE) {
                return this.startMargin;
            }
            if ((this.mMarginFlags & 32) == 32) {
                doResolveMargins();
            }
            if ((this.mMarginFlags & 3) == 1) {
                return this.rightMargin;
            }
            return this.leftMargin;
        }

        public void setMarginEnd(int i) {
            this.endMargin = i;
            this.mMarginFlags = (byte) (this.mMarginFlags | 32);
        }

        public int getMarginEnd() {
            if (this.endMargin != Integer.MIN_VALUE) {
                return this.endMargin;
            }
            if ((this.mMarginFlags & 32) == 32) {
                doResolveMargins();
            }
            if ((this.mMarginFlags & 3) == 1) {
                return this.leftMargin;
            }
            return this.rightMargin;
        }

        public boolean isMarginRelative() {
            return (this.startMargin == Integer.MIN_VALUE && this.endMargin == Integer.MIN_VALUE) ? false : true;
        }

        public void setLayoutDirection(int i) {
            if ((i == 0 || i == 1) && i != (this.mMarginFlags & 3)) {
                this.mMarginFlags = (byte) (this.mMarginFlags & (-4));
                this.mMarginFlags = (byte) ((i & 3) | this.mMarginFlags);
                if (isMarginRelative()) {
                    this.mMarginFlags = (byte) (this.mMarginFlags | 32);
                } else {
                    this.mMarginFlags = (byte) (this.mMarginFlags & (-33));
                }
            }
        }

        public int getLayoutDirection() {
            return this.mMarginFlags & 3;
        }

        @Override
        public void resolveLayoutDirection(int i) {
            setLayoutDirection(i);
            if (!isMarginRelative() || (this.mMarginFlags & 32) != 32) {
                return;
            }
            doResolveMargins();
        }

        private void doResolveMargins() {
            if ((this.mMarginFlags & WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) == 16) {
                if ((this.mMarginFlags & 4) == 4 && this.startMargin > Integer.MIN_VALUE) {
                    this.leftMargin = this.startMargin;
                }
                if ((this.mMarginFlags & 8) == 8 && this.endMargin > Integer.MIN_VALUE) {
                    this.rightMargin = this.endMargin;
                }
            } else {
                if ((this.mMarginFlags & 3) == 1) {
                    this.leftMargin = this.endMargin > Integer.MIN_VALUE ? this.endMargin : 0;
                    this.rightMargin = this.startMargin > Integer.MIN_VALUE ? this.startMargin : 0;
                } else {
                    this.leftMargin = this.startMargin > Integer.MIN_VALUE ? this.startMargin : 0;
                    this.rightMargin = this.endMargin > Integer.MIN_VALUE ? this.endMargin : 0;
                }
            }
            this.mMarginFlags = (byte) (this.mMarginFlags & (-33));
        }

        public boolean isLayoutRtl() {
            return (this.mMarginFlags & 3) == 1;
        }

        @Override
        public void onDebugDraw(View view, Canvas canvas, Paint paint) {
            Insets opticalInsets = View.isLayoutModeOptical(view.mParent) ? view.getOpticalInsets() : Insets.NONE;
            ViewGroup.fillDifference(canvas, view.getLeft() + opticalInsets.left, view.getTop() + opticalInsets.top, view.getRight() - opticalInsets.right, view.getBottom() - opticalInsets.bottom, this.leftMargin, this.topMargin, this.rightMargin, this.bottomMargin, paint);
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("leftMargin", this.leftMargin);
            viewHierarchyEncoder.addProperty("topMargin", this.topMargin);
            viewHierarchyEncoder.addProperty("rightMargin", this.rightMargin);
            viewHierarchyEncoder.addProperty("bottomMargin", this.bottomMargin);
            viewHierarchyEncoder.addProperty("startMargin", this.startMargin);
            viewHierarchyEncoder.addProperty("endMargin", this.endMargin);
        }
    }

    private static final class TouchTarget {
        public static final int ALL_POINTER_IDS = -1;
        private static final int MAX_RECYCLED = 32;
        private static TouchTarget sRecycleBin;
        private static final Object sRecycleLock = new Object[0];
        private static int sRecycledCount;
        public View child;
        public TouchTarget next;
        public int pointerIdBits;

        private TouchTarget() {
        }

        public static TouchTarget obtain(View view, int i) {
            TouchTarget touchTarget;
            if (view == null) {
                throw new IllegalArgumentException("child must be non-null");
            }
            synchronized (sRecycleLock) {
                if (sRecycleBin == null) {
                    touchTarget = new TouchTarget();
                } else {
                    touchTarget = sRecycleBin;
                    sRecycleBin = touchTarget.next;
                    sRecycledCount--;
                    touchTarget.next = null;
                }
            }
            touchTarget.child = view;
            touchTarget.pointerIdBits = i;
            return touchTarget;
        }

        public void recycle() {
            if (this.child == null) {
                throw new IllegalStateException("already recycled once");
            }
            synchronized (sRecycleLock) {
                if (sRecycledCount < 32) {
                    this.next = sRecycleBin;
                    sRecycleBin = this;
                    sRecycledCount++;
                } else {
                    this.next = null;
                }
                this.child = null;
            }
        }
    }

    private static final class HoverTarget {
        private static final int MAX_RECYCLED = 32;
        private static HoverTarget sRecycleBin;
        private static final Object sRecycleLock = new Object[0];
        private static int sRecycledCount;
        public View child;
        public HoverTarget next;

        private HoverTarget() {
        }

        public static HoverTarget obtain(View view) {
            HoverTarget hoverTarget;
            if (view == null) {
                throw new IllegalArgumentException("child must be non-null");
            }
            synchronized (sRecycleLock) {
                if (sRecycleBin == null) {
                    hoverTarget = new HoverTarget();
                } else {
                    hoverTarget = sRecycleBin;
                    sRecycleBin = hoverTarget.next;
                    sRecycledCount--;
                    hoverTarget.next = null;
                }
            }
            hoverTarget.child = view;
            return hoverTarget;
        }

        public void recycle() {
            if (this.child == null) {
                throw new IllegalStateException("already recycled once");
            }
            synchronized (sRecycleLock) {
                if (sRecycledCount < 32) {
                    this.next = sRecycleBin;
                    sRecycleBin = this;
                    sRecycledCount++;
                } else {
                    this.next = null;
                }
                this.child = null;
            }
        }
    }

    static class ChildListForAutoFill extends ArrayList<View> {
        private static final int MAX_POOL_SIZE = 32;
        private static final Pools.SimplePool<ChildListForAutoFill> sPool = new Pools.SimplePool<>(32);

        ChildListForAutoFill() {
        }

        public static ChildListForAutoFill obtain() {
            ChildListForAutoFill childListForAutoFillAcquire = sPool.acquire();
            if (childListForAutoFillAcquire == null) {
                return new ChildListForAutoFill();
            }
            return childListForAutoFillAcquire;
        }

        public void recycle() {
            clear();
            sPool.release(this);
        }
    }

    static class ChildListForAccessibility {
        private static final int MAX_POOL_SIZE = 32;
        private static final Pools.SynchronizedPool<ChildListForAccessibility> sPool = new Pools.SynchronizedPool<>(32);
        private final ArrayList<View> mChildren = new ArrayList<>();
        private final ArrayList<ViewLocationHolder> mHolders = new ArrayList<>();

        ChildListForAccessibility() {
        }

        public static ChildListForAccessibility obtain(ViewGroup viewGroup, boolean z) {
            ChildListForAccessibility childListForAccessibilityAcquire = sPool.acquire();
            if (childListForAccessibilityAcquire == null) {
                childListForAccessibilityAcquire = new ChildListForAccessibility();
            }
            childListForAccessibilityAcquire.init(viewGroup, z);
            return childListForAccessibilityAcquire;
        }

        public void recycle() {
            clear();
            sPool.release(this);
        }

        public int getChildCount() {
            return this.mChildren.size();
        }

        public View getChildAt(int i) {
            return this.mChildren.get(i);
        }

        private void init(ViewGroup viewGroup, boolean z) {
            ArrayList<View> arrayList = this.mChildren;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                arrayList.add(viewGroup.getChildAt(i));
            }
            if (z) {
                ArrayList<ViewLocationHolder> arrayList2 = this.mHolders;
                for (int i2 = 0; i2 < childCount; i2++) {
                    arrayList2.add(ViewLocationHolder.obtain(viewGroup, arrayList.get(i2)));
                }
                sort(arrayList2);
                for (int i3 = 0; i3 < childCount; i3++) {
                    ViewLocationHolder viewLocationHolder = arrayList2.get(i3);
                    arrayList.set(i3, viewLocationHolder.mView);
                    viewLocationHolder.recycle();
                }
                arrayList2.clear();
            }
        }

        private void sort(ArrayList<ViewLocationHolder> arrayList) {
            try {
                ViewLocationHolder.setComparisonStrategy(1);
                Collections.sort(arrayList);
            } catch (IllegalArgumentException e) {
                ViewLocationHolder.setComparisonStrategy(2);
                Collections.sort(arrayList);
            }
        }

        private void clear() {
            this.mChildren.clear();
        }
    }

    static class ViewLocationHolder implements Comparable<ViewLocationHolder> {
        public static final int COMPARISON_STRATEGY_LOCATION = 2;
        public static final int COMPARISON_STRATEGY_STRIPE = 1;
        private static final int MAX_POOL_SIZE = 32;
        private int mLayoutDirection;
        private final Rect mLocation = new Rect();
        private ViewGroup mRoot;
        public View mView;
        private static final Pools.SynchronizedPool<ViewLocationHolder> sPool = new Pools.SynchronizedPool<>(32);
        private static int sComparisonStrategy = 1;

        ViewLocationHolder() {
        }

        public static ViewLocationHolder obtain(ViewGroup viewGroup, View view) {
            ViewLocationHolder viewLocationHolderAcquire = sPool.acquire();
            if (viewLocationHolderAcquire == null) {
                viewLocationHolderAcquire = new ViewLocationHolder();
            }
            viewLocationHolderAcquire.init(viewGroup, view);
            return viewLocationHolderAcquire;
        }

        public static void setComparisonStrategy(int i) {
            sComparisonStrategy = i;
        }

        public void recycle() {
            clear();
            sPool.release(this);
        }

        @Override
        public int compareTo(ViewLocationHolder viewLocationHolder) {
            if (viewLocationHolder == null) {
                return 1;
            }
            int iCompareBoundsOfTree = compareBoundsOfTree(this, viewLocationHolder);
            if (iCompareBoundsOfTree != 0) {
                return iCompareBoundsOfTree;
            }
            return this.mView.getAccessibilityViewId() - viewLocationHolder.mView.getAccessibilityViewId();
        }

        private static int compareBoundsOfTree(ViewLocationHolder viewLocationHolder, ViewLocationHolder viewLocationHolder2) {
            if (sComparisonStrategy == 1) {
                if (viewLocationHolder.mLocation.bottom - viewLocationHolder2.mLocation.top <= 0) {
                    return -1;
                }
                if (viewLocationHolder.mLocation.top - viewLocationHolder2.mLocation.bottom >= 0) {
                    return 1;
                }
            }
            if (viewLocationHolder.mLayoutDirection == 0) {
                int i = viewLocationHolder.mLocation.left - viewLocationHolder2.mLocation.left;
                if (i != 0) {
                    return i;
                }
            } else {
                int i2 = viewLocationHolder.mLocation.right - viewLocationHolder2.mLocation.right;
                if (i2 != 0) {
                    return -i2;
                }
            }
            int i3 = viewLocationHolder.mLocation.top - viewLocationHolder2.mLocation.top;
            if (i3 != 0) {
                return i3;
            }
            int iHeight = viewLocationHolder.mLocation.height() - viewLocationHolder2.mLocation.height();
            if (iHeight != 0) {
                return -iHeight;
            }
            int iWidth = viewLocationHolder.mLocation.width() - viewLocationHolder2.mLocation.width();
            if (iWidth != 0) {
                return -iWidth;
            }
            final Rect rect = new Rect();
            final Rect rect2 = new Rect();
            final Rect rect3 = new Rect();
            viewLocationHolder.mView.getBoundsOnScreen(rect, true);
            viewLocationHolder2.mView.getBoundsOnScreen(rect2, true);
            View viewFindViewByPredicateTraversal = viewLocationHolder.mView.findViewByPredicateTraversal(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ViewGroup.ViewLocationHolder.lambda$compareBoundsOfTree$0(rect3, rect, (View) obj);
                }
            }, null);
            View viewFindViewByPredicateTraversal2 = viewLocationHolder2.mView.findViewByPredicateTraversal(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ViewGroup.ViewLocationHolder.lambda$compareBoundsOfTree$1(rect3, rect2, (View) obj);
                }
            }, null);
            if (viewFindViewByPredicateTraversal != null && viewFindViewByPredicateTraversal2 != null) {
                return compareBoundsOfTree(obtain(viewLocationHolder.mRoot, viewFindViewByPredicateTraversal), obtain(viewLocationHolder.mRoot, viewFindViewByPredicateTraversal2));
            }
            if (viewFindViewByPredicateTraversal != null) {
                return 1;
            }
            return viewFindViewByPredicateTraversal2 != null ? -1 : 0;
        }

        static boolean lambda$compareBoundsOfTree$0(Rect rect, Rect rect2, View view) {
            view.getBoundsOnScreen(rect, true);
            return !rect.equals(rect2);
        }

        static boolean lambda$compareBoundsOfTree$1(Rect rect, Rect rect2, View view) {
            view.getBoundsOnScreen(rect, true);
            return !rect.equals(rect2);
        }

        private void init(ViewGroup viewGroup, View view) {
            Rect rect = this.mLocation;
            view.getDrawingRect(rect);
            viewGroup.offsetDescendantRectToMyCoords(view, rect);
            this.mView = view;
            this.mRoot = viewGroup;
            this.mLayoutDirection = viewGroup.getLayoutDirection();
        }

        private void clear() {
            this.mView = null;
            this.mLocation.set(0, 0, 0, 0);
        }
    }

    private static void drawRect(Canvas canvas, Paint paint, int i, int i2, int i3, int i4) {
        if (sDebugLines == null) {
            sDebugLines = new float[16];
        }
        float f = i;
        sDebugLines[0] = f;
        float f2 = i2;
        sDebugLines[1] = f2;
        float f3 = i3;
        sDebugLines[2] = f3;
        sDebugLines[3] = f2;
        sDebugLines[4] = f3;
        sDebugLines[5] = f2;
        sDebugLines[6] = f3;
        float f4 = i4;
        sDebugLines[7] = f4;
        sDebugLines[8] = f3;
        sDebugLines[9] = f4;
        sDebugLines[10] = f;
        sDebugLines[11] = f4;
        sDebugLines[12] = f;
        sDebugLines[13] = f4;
        sDebugLines[14] = f;
        sDebugLines[15] = f2;
        canvas.drawLines(sDebugLines, paint);
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("focus:descendantFocusability", getDescendantFocusability());
        viewHierarchyEncoder.addProperty("drawing:clipChildren", getClipChildren());
        viewHierarchyEncoder.addProperty("drawing:clipToPadding", getClipToPadding());
        viewHierarchyEncoder.addProperty("drawing:childrenDrawingOrderEnabled", isChildrenDrawingOrderEnabled());
        viewHierarchyEncoder.addProperty("drawing:persistentDrawingCache", getPersistentDrawingCache());
        int childCount = getChildCount();
        viewHierarchyEncoder.addProperty("meta:__childCount__", (short) childCount);
        for (int i = 0; i < childCount; i++) {
            viewHierarchyEncoder.addPropertyKey("meta:__child__" + i);
            getChildAt(i).encode(viewHierarchyEncoder);
        }
    }
}
