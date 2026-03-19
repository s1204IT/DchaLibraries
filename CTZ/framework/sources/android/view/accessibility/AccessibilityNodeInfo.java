package android.view.accessibility;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AccessibilityClickableSpan;
import android.text.style.AccessibilityURLSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.ArraySet;
import android.util.LongArray;
import android.util.Pools;
import android.view.View;
import com.android.internal.util.BitUtils;
import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessibilityNodeInfo implements Parcelable {
    public static final int ACTION_ACCESSIBILITY_FOCUS = 64;
    public static final String ACTION_ARGUMENT_ACCESSIBLE_CLICKABLE_SPAN = "android.view.accessibility.action.ACTION_ARGUMENT_ACCESSIBLE_CLICKABLE_SPAN";
    public static final String ACTION_ARGUMENT_COLUMN_INT = "android.view.accessibility.action.ARGUMENT_COLUMN_INT";
    public static final String ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN = "ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN";
    public static final String ACTION_ARGUMENT_HTML_ELEMENT_STRING = "ACTION_ARGUMENT_HTML_ELEMENT_STRING";
    public static final String ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT = "ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_X = "ACTION_ARGUMENT_MOVE_WINDOW_X";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_Y = "ACTION_ARGUMENT_MOVE_WINDOW_Y";
    public static final String ACTION_ARGUMENT_PROGRESS_VALUE = "android.view.accessibility.action.ARGUMENT_PROGRESS_VALUE";
    public static final String ACTION_ARGUMENT_ROW_INT = "android.view.accessibility.action.ARGUMENT_ROW_INT";
    public static final String ACTION_ARGUMENT_SELECTION_END_INT = "ACTION_ARGUMENT_SELECTION_END_INT";
    public static final String ACTION_ARGUMENT_SELECTION_START_INT = "ACTION_ARGUMENT_SELECTION_START_INT";
    public static final String ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE = "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE";
    public static final int ACTION_CLEAR_ACCESSIBILITY_FOCUS = 128;
    public static final int ACTION_CLEAR_FOCUS = 2;
    public static final int ACTION_CLEAR_SELECTION = 8;
    public static final int ACTION_CLICK = 16;
    public static final int ACTION_COLLAPSE = 524288;
    public static final int ACTION_COPY = 16384;
    public static final int ACTION_CUT = 65536;
    public static final int ACTION_DISMISS = 1048576;
    public static final int ACTION_EXPAND = 262144;
    public static final int ACTION_FOCUS = 1;
    public static final int ACTION_LONG_CLICK = 32;
    public static final int ACTION_NEXT_AT_MOVEMENT_GRANULARITY = 256;
    public static final int ACTION_NEXT_HTML_ELEMENT = 1024;
    public static final int ACTION_PASTE = 32768;
    public static final int ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = 512;
    public static final int ACTION_PREVIOUS_HTML_ELEMENT = 2048;
    public static final int ACTION_SCROLL_BACKWARD = 8192;
    public static final int ACTION_SCROLL_FORWARD = 4096;
    public static final int ACTION_SELECT = 4;
    public static final int ACTION_SET_SELECTION = 131072;
    public static final int ACTION_SET_TEXT = 2097152;
    private static final int ACTION_TYPE_MASK = -16777216;
    private static final int BOOLEAN_PROPERTY_ACCESSIBILITY_FOCUSED = 1024;
    private static final int BOOLEAN_PROPERTY_CHECKABLE = 1;
    private static final int BOOLEAN_PROPERTY_CHECKED = 2;
    private static final int BOOLEAN_PROPERTY_CLICKABLE = 32;
    private static final int BOOLEAN_PROPERTY_CONTENT_INVALID = 65536;
    private static final int BOOLEAN_PROPERTY_CONTEXT_CLICKABLE = 131072;
    private static final int BOOLEAN_PROPERTY_DISMISSABLE = 16384;
    private static final int BOOLEAN_PROPERTY_EDITABLE = 4096;
    private static final int BOOLEAN_PROPERTY_ENABLED = 128;
    private static final int BOOLEAN_PROPERTY_FOCUSABLE = 4;
    private static final int BOOLEAN_PROPERTY_FOCUSED = 8;
    private static final int BOOLEAN_PROPERTY_IMPORTANCE = 262144;
    private static final int BOOLEAN_PROPERTY_IS_HEADING = 2097152;
    private static final int BOOLEAN_PROPERTY_IS_SHOWING_HINT = 1048576;
    private static final int BOOLEAN_PROPERTY_LONG_CLICKABLE = 64;
    private static final int BOOLEAN_PROPERTY_MULTI_LINE = 32768;
    private static final int BOOLEAN_PROPERTY_OPENS_POPUP = 8192;
    private static final int BOOLEAN_PROPERTY_PASSWORD = 256;
    private static final int BOOLEAN_PROPERTY_SCREEN_READER_FOCUSABLE = 524288;
    private static final int BOOLEAN_PROPERTY_SCROLLABLE = 512;
    private static final int BOOLEAN_PROPERTY_SELECTED = 16;
    private static final int BOOLEAN_PROPERTY_VISIBLE_TO_USER = 2048;
    private static final boolean DEBUG = false;
    public static final String EXTRA_DATA_REQUESTED_KEY = "android.view.accessibility.AccessibilityNodeInfo.extra_data_requested";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_KEY";
    public static final int FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 8;
    public static final int FLAG_PREFETCH_DESCENDANTS = 4;
    public static final int FLAG_PREFETCH_PREDECESSORS = 1;
    public static final int FLAG_PREFETCH_SIBLINGS = 2;
    public static final int FLAG_REPORT_VIEW_IDS = 16;
    public static final int FOCUS_ACCESSIBILITY = 2;
    public static final int FOCUS_INPUT = 1;
    public static final int LAST_LEGACY_STANDARD_ACTION = 2097152;
    private static final int MAX_POOL_SIZE = 50;
    public static final int MOVEMENT_GRANULARITY_CHARACTER = 1;
    public static final int MOVEMENT_GRANULARITY_LINE = 4;
    public static final int MOVEMENT_GRANULARITY_PAGE = 16;
    public static final int MOVEMENT_GRANULARITY_PARAGRAPH = 8;
    public static final int MOVEMENT_GRANULARITY_WORD = 2;
    public static final int ROOT_ITEM_ID = 2147483646;
    public static final int UNDEFINED_CONNECTION_ID = -1;
    public static final int UNDEFINED_ITEM_ID = Integer.MAX_VALUE;
    public static final int UNDEFINED_SELECTION_INDEX = -1;
    private static final long VIRTUAL_DESCENDANT_ID_MASK = -4294967296L;
    private static final int VIRTUAL_DESCENDANT_ID_SHIFT = 32;
    private static AtomicInteger sNumInstancesInUse;
    private ArrayList<AccessibilityAction> mActions;
    private int mBooleanProperties;
    private LongArray mChildNodeIds;
    private CharSequence mClassName;
    private CollectionInfo mCollectionInfo;
    private CollectionItemInfo mCollectionItemInfo;
    private CharSequence mContentDescription;
    private int mDrawingOrderInParent;
    private CharSequence mError;
    private ArrayList<String> mExtraDataKeys;
    private Bundle mExtras;
    private CharSequence mHintText;
    private int mMovementGranularities;
    private CharSequence mOriginalText;
    private CharSequence mPackageName;
    private CharSequence mPaneTitle;
    private RangeInfo mRangeInfo;
    private boolean mSealed;
    private CharSequence mText;
    private CharSequence mTooltipText;
    private String mViewIdResourceName;
    public static final long UNDEFINED_NODE_ID = makeNodeId(Integer.MAX_VALUE, Integer.MAX_VALUE);
    public static final long ROOT_NODE_ID = makeNodeId(2147483646, -1);
    private static final Pools.SynchronizedPool<AccessibilityNodeInfo> sPool = new Pools.SynchronizedPool<>(50);
    private static final AccessibilityNodeInfo DEFAULT = new AccessibilityNodeInfo();
    public static final Parcelable.Creator<AccessibilityNodeInfo> CREATOR = new Parcelable.Creator<AccessibilityNodeInfo>() {
        @Override
        public AccessibilityNodeInfo createFromParcel(Parcel parcel) {
            AccessibilityNodeInfo accessibilityNodeInfoObtain = AccessibilityNodeInfo.obtain();
            accessibilityNodeInfoObtain.initFromParcel(parcel);
            return accessibilityNodeInfoObtain;
        }

        @Override
        public AccessibilityNodeInfo[] newArray(int i) {
            return new AccessibilityNodeInfo[i];
        }
    };
    private int mWindowId = -1;
    private long mSourceNodeId = UNDEFINED_NODE_ID;
    private long mParentNodeId = UNDEFINED_NODE_ID;
    private long mLabelForId = UNDEFINED_NODE_ID;
    private long mLabeledById = UNDEFINED_NODE_ID;
    private long mTraversalBefore = UNDEFINED_NODE_ID;
    private long mTraversalAfter = UNDEFINED_NODE_ID;
    private final Rect mBoundsInParent = new Rect();
    private final Rect mBoundsInScreen = new Rect();
    private int mMaxTextLength = -1;
    private int mTextSelectionStart = -1;
    private int mTextSelectionEnd = -1;
    private int mInputType = 0;
    private int mLiveRegion = 0;
    private int mConnectionId = -1;

    public static int getAccessibilityViewId(long j) {
        return (int) j;
    }

    public static int getVirtualDescendantId(long j) {
        return (int) ((j & VIRTUAL_DESCENDANT_ID_MASK) >> 32);
    }

    public static long makeNodeId(int i, int i2) {
        return ((long) i) | (((long) i2) << 32);
    }

    private AccessibilityNodeInfo() {
    }

    public void setSource(View view) {
        setSource(view, -1);
    }

    public void setSource(View view, int i) {
        enforceNotSealed();
        this.mWindowId = view != null ? view.getAccessibilityWindowId() : Integer.MAX_VALUE;
        this.mSourceNodeId = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public AccessibilityNodeInfo findFocus(int i) {
        enforceSealed();
        enforceValidFocusType(i);
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, this.mWindowId, this.mSourceNodeId, i);
    }

    public AccessibilityNodeInfo focusSearch(int i) {
        enforceSealed();
        enforceValidFocusDirection(i);
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().focusSearch(this.mConnectionId, this.mWindowId, this.mSourceNodeId, i);
    }

    public int getWindowId() {
        return this.mWindowId;
    }

    public boolean refresh(Bundle bundle, boolean z) {
        AccessibilityNodeInfo accessibilityNodeInfoFindAccessibilityNodeInfoByAccessibilityId;
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId) || (accessibilityNodeInfoFindAccessibilityNodeInfoByAccessibilityId = AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, this.mSourceNodeId, z, 0, bundle)) == null) {
            return false;
        }
        enforceSealed();
        init(accessibilityNodeInfoFindAccessibilityNodeInfoByAccessibilityId);
        accessibilityNodeInfoFindAccessibilityNodeInfoByAccessibilityId.recycle();
        return true;
    }

    public boolean refresh() {
        return refresh(null, true);
    }

    public boolean refreshWithExtraData(String str, Bundle bundle) {
        bundle.putString(EXTRA_DATA_REQUESTED_KEY, str);
        return refresh(bundle, true);
    }

    public LongArray getChildNodeIds() {
        return this.mChildNodeIds;
    }

    public long getChildId(int i) {
        if (this.mChildNodeIds == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.mChildNodeIds.get(i);
    }

    public int getChildCount() {
        if (this.mChildNodeIds == null) {
            return 0;
        }
        return this.mChildNodeIds.size();
    }

    public AccessibilityNodeInfo getChild(int i) {
        enforceSealed();
        if (this.mChildNodeIds == null || !canPerformRequestOverConnection(this.mSourceNodeId)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, this.mChildNodeIds.get(i), false, 4, null);
    }

    public void addChild(View view) {
        addChildInternal(view, -1, true);
    }

    public void addChildUnchecked(View view) {
        addChildInternal(view, -1, false);
    }

    public boolean removeChild(View view) {
        return removeChild(view, -1);
    }

    public void addChild(View view, int i) {
        addChildInternal(view, i, true);
    }

    private void addChildInternal(View view, int i, boolean z) {
        enforceNotSealed();
        if (this.mChildNodeIds == null) {
            this.mChildNodeIds = new LongArray();
        }
        long jMakeNodeId = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
        if (z && this.mChildNodeIds.indexOf(jMakeNodeId) >= 0) {
            return;
        }
        this.mChildNodeIds.add(jMakeNodeId);
    }

    public boolean removeChild(View view, int i) {
        enforceNotSealed();
        LongArray longArray = this.mChildNodeIds;
        if (longArray == null) {
            return false;
        }
        int iIndexOf = longArray.indexOf(makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i));
        if (iIndexOf < 0) {
            return false;
        }
        longArray.remove(iIndexOf);
        return true;
    }

    public List<AccessibilityAction> getActionList() {
        return CollectionUtils.emptyIfNull(this.mActions);
    }

    @Deprecated
    public int getActions() {
        if (this.mActions == null) {
            return 0;
        }
        int size = this.mActions.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            int id = this.mActions.get(i2).getId();
            if (id <= 2097152) {
                i |= id;
            }
        }
        return i;
    }

    public void addAction(AccessibilityAction accessibilityAction) {
        enforceNotSealed();
        addActionUnchecked(accessibilityAction);
    }

    private void addActionUnchecked(AccessibilityAction accessibilityAction) {
        if (accessibilityAction == null) {
            return;
        }
        if (this.mActions == null) {
            this.mActions = new ArrayList<>();
        }
        this.mActions.remove(accessibilityAction);
        this.mActions.add(accessibilityAction);
    }

    @Deprecated
    public void addAction(int i) {
        enforceNotSealed();
        if (((-16777216) & i) != 0) {
            throw new IllegalArgumentException("Action is not a combination of the standard actions: " + i);
        }
        addStandardActions(i);
    }

    @Deprecated
    public void removeAction(int i) {
        enforceNotSealed();
        removeAction(getActionSingleton(i));
    }

    public boolean removeAction(AccessibilityAction accessibilityAction) {
        enforceNotSealed();
        if (this.mActions == null || accessibilityAction == null) {
            return false;
        }
        return this.mActions.remove(accessibilityAction);
    }

    public void removeAllActions() {
        if (this.mActions != null) {
            this.mActions.clear();
        }
    }

    public AccessibilityNodeInfo getTraversalBefore() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mTraversalBefore);
    }

    public void setTraversalBefore(View view) {
        setTraversalBefore(view, -1);
    }

    public void setTraversalBefore(View view, int i) {
        enforceNotSealed();
        this.mTraversalBefore = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public AccessibilityNodeInfo getTraversalAfter() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mTraversalAfter);
    }

    public void setTraversalAfter(View view) {
        setTraversalAfter(view, -1);
    }

    public void setTraversalAfter(View view, int i) {
        enforceNotSealed();
        this.mTraversalAfter = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public List<String> getAvailableExtraData() {
        if (this.mExtraDataKeys != null) {
            return Collections.unmodifiableList(this.mExtraDataKeys);
        }
        return Collections.EMPTY_LIST;
    }

    public void setAvailableExtraData(List<String> list) {
        enforceNotSealed();
        this.mExtraDataKeys = new ArrayList<>(list);
    }

    public void setMaxTextLength(int i) {
        enforceNotSealed();
        this.mMaxTextLength = i;
    }

    public int getMaxTextLength() {
        return this.mMaxTextLength;
    }

    public void setMovementGranularities(int i) {
        enforceNotSealed();
        this.mMovementGranularities = i;
    }

    public int getMovementGranularities() {
        return this.mMovementGranularities;
    }

    public boolean performAction(int i) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return false;
        }
        return AccessibilityInteractionClient.getInstance().performAccessibilityAction(this.mConnectionId, this.mWindowId, this.mSourceNodeId, i, null);
    }

    public boolean performAction(int i, Bundle bundle) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return false;
        }
        return AccessibilityInteractionClient.getInstance().performAccessibilityAction(this.mConnectionId, this.mWindowId, this.mSourceNodeId, i, bundle);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String str) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return Collections.emptyList();
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByText(this.mConnectionId, this.mWindowId, this.mSourceNodeId, str);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(String str) {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return Collections.emptyList();
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByViewId(this.mConnectionId, this.mWindowId, this.mSourceNodeId, str);
    }

    public AccessibilityWindowInfo getWindow() {
        enforceSealed();
        if (!canPerformRequestOverConnection(this.mSourceNodeId)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().getWindow(this.mConnectionId, this.mWindowId);
    }

    public AccessibilityNodeInfo getParent() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mParentNodeId);
    }

    public long getParentNodeId() {
        return this.mParentNodeId;
    }

    public void setParent(View view) {
        setParent(view, -1);
    }

    public void setParent(View view, int i) {
        enforceNotSealed();
        this.mParentNodeId = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public void getBoundsInParent(Rect rect) {
        rect.set(this.mBoundsInParent.left, this.mBoundsInParent.top, this.mBoundsInParent.right, this.mBoundsInParent.bottom);
    }

    public void setBoundsInParent(Rect rect) {
        enforceNotSealed();
        this.mBoundsInParent.set(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void getBoundsInScreen(Rect rect) {
        rect.set(this.mBoundsInScreen.left, this.mBoundsInScreen.top, this.mBoundsInScreen.right, this.mBoundsInScreen.bottom);
    }

    public Rect getBoundsInScreen() {
        return this.mBoundsInScreen;
    }

    public void setBoundsInScreen(Rect rect) {
        enforceNotSealed();
        this.mBoundsInScreen.set(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean isCheckable() {
        return getBooleanProperty(1);
    }

    public void setCheckable(boolean z) {
        setBooleanProperty(1, z);
    }

    public boolean isChecked() {
        return getBooleanProperty(2);
    }

    public void setChecked(boolean z) {
        setBooleanProperty(2, z);
    }

    public boolean isFocusable() {
        return getBooleanProperty(4);
    }

    public void setFocusable(boolean z) {
        setBooleanProperty(4, z);
    }

    public boolean isFocused() {
        return getBooleanProperty(8);
    }

    public void setFocused(boolean z) {
        setBooleanProperty(8, z);
    }

    public boolean isVisibleToUser() {
        return getBooleanProperty(2048);
    }

    public void setVisibleToUser(boolean z) {
        setBooleanProperty(2048, z);
    }

    public boolean isAccessibilityFocused() {
        return getBooleanProperty(1024);
    }

    public void setAccessibilityFocused(boolean z) {
        setBooleanProperty(1024, z);
    }

    public boolean isSelected() {
        return getBooleanProperty(16);
    }

    public void setSelected(boolean z) {
        setBooleanProperty(16, z);
    }

    public boolean isClickable() {
        return getBooleanProperty(32);
    }

    public void setClickable(boolean z) {
        setBooleanProperty(32, z);
    }

    public boolean isLongClickable() {
        return getBooleanProperty(64);
    }

    public void setLongClickable(boolean z) {
        setBooleanProperty(64, z);
    }

    public boolean isEnabled() {
        return getBooleanProperty(128);
    }

    public void setEnabled(boolean z) {
        setBooleanProperty(128, z);
    }

    public boolean isPassword() {
        return getBooleanProperty(256);
    }

    public void setPassword(boolean z) {
        setBooleanProperty(256, z);
    }

    public boolean isScrollable() {
        return getBooleanProperty(512);
    }

    public void setScrollable(boolean z) {
        setBooleanProperty(512, z);
    }

    public boolean isEditable() {
        return getBooleanProperty(4096);
    }

    public void setEditable(boolean z) {
        setBooleanProperty(4096, z);
    }

    public void setPaneTitle(CharSequence charSequence) {
        enforceNotSealed();
        this.mPaneTitle = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public CharSequence getPaneTitle() {
        return this.mPaneTitle;
    }

    public int getDrawingOrder() {
        return this.mDrawingOrderInParent;
    }

    public void setDrawingOrder(int i) {
        enforceNotSealed();
        this.mDrawingOrderInParent = i;
    }

    public CollectionInfo getCollectionInfo() {
        return this.mCollectionInfo;
    }

    public void setCollectionInfo(CollectionInfo collectionInfo) {
        enforceNotSealed();
        this.mCollectionInfo = collectionInfo;
    }

    public CollectionItemInfo getCollectionItemInfo() {
        return this.mCollectionItemInfo;
    }

    public void setCollectionItemInfo(CollectionItemInfo collectionItemInfo) {
        enforceNotSealed();
        this.mCollectionItemInfo = collectionItemInfo;
    }

    public RangeInfo getRangeInfo() {
        return this.mRangeInfo;
    }

    public void setRangeInfo(RangeInfo rangeInfo) {
        enforceNotSealed();
        this.mRangeInfo = rangeInfo;
    }

    public boolean isContentInvalid() {
        return getBooleanProperty(65536);
    }

    public void setContentInvalid(boolean z) {
        setBooleanProperty(65536, z);
    }

    public boolean isContextClickable() {
        return getBooleanProperty(131072);
    }

    public void setContextClickable(boolean z) {
        setBooleanProperty(131072, z);
    }

    public int getLiveRegion() {
        return this.mLiveRegion;
    }

    public void setLiveRegion(int i) {
        enforceNotSealed();
        this.mLiveRegion = i;
    }

    public boolean isMultiLine() {
        return getBooleanProperty(32768);
    }

    public void setMultiLine(boolean z) {
        setBooleanProperty(32768, z);
    }

    public boolean canOpenPopup() {
        return getBooleanProperty(8192);
    }

    public void setCanOpenPopup(boolean z) {
        enforceNotSealed();
        setBooleanProperty(8192, z);
    }

    public boolean isDismissable() {
        return getBooleanProperty(16384);
    }

    public void setDismissable(boolean z) {
        setBooleanProperty(16384, z);
    }

    public boolean isImportantForAccessibility() {
        return getBooleanProperty(262144);
    }

    public void setImportantForAccessibility(boolean z) {
        setBooleanProperty(262144, z);
    }

    public boolean isScreenReaderFocusable() {
        return getBooleanProperty(524288);
    }

    public void setScreenReaderFocusable(boolean z) {
        setBooleanProperty(524288, z);
    }

    public boolean isShowingHintText() {
        return getBooleanProperty(1048576);
    }

    public void setShowingHintText(boolean z) {
        setBooleanProperty(1048576, z);
    }

    public boolean isHeading() {
        if (getBooleanProperty(2097152)) {
            return true;
        }
        CollectionItemInfo collectionItemInfo = getCollectionItemInfo();
        return collectionItemInfo != null && collectionItemInfo.mHeading;
    }

    public void setHeading(boolean z) {
        setBooleanProperty(2097152, z);
    }

    public CharSequence getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(CharSequence charSequence) {
        enforceNotSealed();
        this.mPackageName = charSequence;
    }

    public CharSequence getClassName() {
        return this.mClassName;
    }

    public void setClassName(CharSequence charSequence) {
        enforceNotSealed();
        this.mClassName = charSequence;
    }

    public CharSequence getText() {
        if (this.mText instanceof Spanned) {
            Spanned spanned = (Spanned) this.mText;
            for (AccessibilityClickableSpan accessibilityClickableSpan : (AccessibilityClickableSpan[]) spanned.getSpans(0, this.mText.length(), AccessibilityClickableSpan.class)) {
                accessibilityClickableSpan.copyConnectionDataFrom(this);
            }
            for (AccessibilityURLSpan accessibilityURLSpan : (AccessibilityURLSpan[]) spanned.getSpans(0, this.mText.length(), AccessibilityURLSpan.class)) {
                accessibilityURLSpan.copyConnectionDataFrom(this);
            }
        }
        return this.mText;
    }

    public CharSequence getOriginalText() {
        return this.mOriginalText;
    }

    public void setText(CharSequence charSequence) {
        Object accessibilityClickableSpan;
        enforceNotSealed();
        this.mOriginalText = charSequence;
        if (charSequence instanceof Spanned) {
            ClickableSpan[] clickableSpanArr = (ClickableSpan[]) ((Spanned) charSequence).getSpans(0, charSequence.length(), ClickableSpan.class);
            if (clickableSpanArr.length > 0) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
                for (ClickableSpan clickableSpan : clickableSpanArr) {
                    if ((clickableSpan instanceof AccessibilityClickableSpan) || (clickableSpan instanceof AccessibilityURLSpan)) {
                        break;
                    }
                    int spanStart = spannableStringBuilder.getSpanStart(clickableSpan);
                    int spanEnd = spannableStringBuilder.getSpanEnd(clickableSpan);
                    int spanFlags = spannableStringBuilder.getSpanFlags(clickableSpan);
                    spannableStringBuilder.removeSpan(clickableSpan);
                    if (clickableSpan instanceof URLSpan) {
                        accessibilityClickableSpan = new AccessibilityURLSpan((URLSpan) clickableSpan);
                    } else {
                        accessibilityClickableSpan = new AccessibilityClickableSpan(clickableSpan.getId());
                    }
                    spannableStringBuilder.setSpan(accessibilityClickableSpan, spanStart, spanEnd, spanFlags);
                }
                this.mText = spannableStringBuilder;
                return;
            }
        }
        this.mText = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public CharSequence getHintText() {
        return this.mHintText;
    }

    public void setHintText(CharSequence charSequence) {
        enforceNotSealed();
        this.mHintText = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public void setError(CharSequence charSequence) {
        enforceNotSealed();
        this.mError = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public CharSequence getError() {
        return this.mError;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    public void setContentDescription(CharSequence charSequence) {
        enforceNotSealed();
        this.mContentDescription = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public CharSequence getTooltipText() {
        return this.mTooltipText;
    }

    public void setTooltipText(CharSequence charSequence) {
        enforceNotSealed();
        this.mTooltipText = charSequence == null ? null : charSequence.subSequence(0, charSequence.length());
    }

    public void setLabelFor(View view) {
        setLabelFor(view, -1);
    }

    public void setLabelFor(View view, int i) {
        enforceNotSealed();
        this.mLabelForId = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public AccessibilityNodeInfo getLabelFor() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mLabelForId);
    }

    public void setLabeledBy(View view) {
        setLabeledBy(view, -1);
    }

    public void setLabeledBy(View view, int i) {
        enforceNotSealed();
        this.mLabeledById = makeNodeId(view != null ? view.getAccessibilityViewId() : Integer.MAX_VALUE, i);
    }

    public AccessibilityNodeInfo getLabeledBy() {
        enforceSealed();
        return getNodeForAccessibilityId(this.mLabeledById);
    }

    public void setViewIdResourceName(String str) {
        enforceNotSealed();
        this.mViewIdResourceName = str;
    }

    public String getViewIdResourceName() {
        return this.mViewIdResourceName;
    }

    public int getTextSelectionStart() {
        return this.mTextSelectionStart;
    }

    public int getTextSelectionEnd() {
        return this.mTextSelectionEnd;
    }

    public void setTextSelection(int i, int i2) {
        enforceNotSealed();
        this.mTextSelectionStart = i;
        this.mTextSelectionEnd = i2;
    }

    public int getInputType() {
        return this.mInputType;
    }

    public void setInputType(int i) {
        enforceNotSealed();
        this.mInputType = i;
    }

    public Bundle getExtras() {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        return this.mExtras;
    }

    public boolean hasExtras() {
        return this.mExtras != null;
    }

    private boolean getBooleanProperty(int i) {
        return (i & this.mBooleanProperties) != 0;
    }

    private void setBooleanProperty(int i, boolean z) {
        enforceNotSealed();
        if (z) {
            this.mBooleanProperties = i | this.mBooleanProperties;
        } else {
            this.mBooleanProperties = (~i) & this.mBooleanProperties;
        }
    }

    public void setConnectionId(int i) {
        enforceNotSealed();
        this.mConnectionId = i;
    }

    public int getConnectionId() {
        return this.mConnectionId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setSourceNodeId(long j, int i) {
        enforceNotSealed();
        this.mSourceNodeId = j;
        this.mWindowId = i;
    }

    public long getSourceNodeId() {
        return this.mSourceNodeId;
    }

    public void setSealed(boolean z) {
        this.mSealed = z;
    }

    public boolean isSealed() {
        return this.mSealed;
    }

    protected void enforceSealed() {
        if (!isSealed()) {
            throw new IllegalStateException("Cannot perform this action on a not sealed instance.");
        }
    }

    private void enforceValidFocusDirection(int i) {
        if (i == 17 || i == 33 || i == 66 || i == 130) {
            return;
        }
        switch (i) {
            case 1:
            case 2:
                return;
            default:
                throw new IllegalArgumentException("Unknown direction: " + i);
        }
    }

    private void enforceValidFocusType(int i) {
        switch (i) {
            case 1:
            case 2:
                return;
            default:
                throw new IllegalArgumentException("Unknown focus type: " + i);
        }
    }

    protected void enforceNotSealed() {
        if (isSealed()) {
            throw new IllegalStateException("Cannot perform this action on a sealed instance.");
        }
    }

    public static AccessibilityNodeInfo obtain(View view) {
        AccessibilityNodeInfo accessibilityNodeInfoObtain = obtain();
        accessibilityNodeInfoObtain.setSource(view);
        return accessibilityNodeInfoObtain;
    }

    public static AccessibilityNodeInfo obtain(View view, int i) {
        AccessibilityNodeInfo accessibilityNodeInfoObtain = obtain();
        accessibilityNodeInfoObtain.setSource(view, i);
        return accessibilityNodeInfoObtain;
    }

    public static AccessibilityNodeInfo obtain() {
        AccessibilityNodeInfo accessibilityNodeInfoAcquire = sPool.acquire();
        if (sNumInstancesInUse != null) {
            sNumInstancesInUse.incrementAndGet();
        }
        return accessibilityNodeInfoAcquire != null ? accessibilityNodeInfoAcquire : new AccessibilityNodeInfo();
    }

    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo accessibilityNodeInfo) {
        AccessibilityNodeInfo accessibilityNodeInfoObtain = obtain();
        accessibilityNodeInfoObtain.init(accessibilityNodeInfo);
        return accessibilityNodeInfoObtain;
    }

    public void recycle() {
        clear();
        sPool.release(this);
        if (sNumInstancesInUse != null) {
            sNumInstancesInUse.decrementAndGet();
        }
    }

    public static void setNumInstancesInUseCounter(AtomicInteger atomicInteger) {
        sNumInstancesInUse = atomicInteger;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcelNoRecycle(parcel, i);
        recycle();
    }

    public void writeToParcelNoRecycle(Parcel parcel, int i) {
        long jBitAt = isSealed() != DEFAULT.isSealed() ? BitUtils.bitAt(0) | 0 : 0L;
        if (this.mSourceNodeId != DEFAULT.mSourceNodeId) {
            jBitAt |= BitUtils.bitAt(1);
        }
        if (this.mWindowId != DEFAULT.mWindowId) {
            jBitAt |= BitUtils.bitAt(2);
        }
        if (this.mParentNodeId != DEFAULT.mParentNodeId) {
            jBitAt |= BitUtils.bitAt(3);
        }
        if (this.mLabelForId != DEFAULT.mLabelForId) {
            jBitAt |= BitUtils.bitAt(4);
        }
        if (this.mLabeledById != DEFAULT.mLabeledById) {
            jBitAt |= BitUtils.bitAt(5);
        }
        if (this.mTraversalBefore != DEFAULT.mTraversalBefore) {
            jBitAt |= BitUtils.bitAt(6);
        }
        if (this.mTraversalAfter != DEFAULT.mTraversalAfter) {
            jBitAt |= BitUtils.bitAt(7);
        }
        if (this.mConnectionId != DEFAULT.mConnectionId) {
            jBitAt |= BitUtils.bitAt(8);
        }
        if (!LongArray.elementsEqual(this.mChildNodeIds, DEFAULT.mChildNodeIds)) {
            jBitAt |= BitUtils.bitAt(9);
        }
        if (!Objects.equals(this.mBoundsInParent, DEFAULT.mBoundsInParent)) {
            jBitAt |= BitUtils.bitAt(10);
        }
        if (!Objects.equals(this.mBoundsInScreen, DEFAULT.mBoundsInScreen)) {
            jBitAt |= BitUtils.bitAt(11);
        }
        if (!Objects.equals(this.mActions, DEFAULT.mActions)) {
            jBitAt |= BitUtils.bitAt(12);
        }
        if (this.mMaxTextLength != DEFAULT.mMaxTextLength) {
            jBitAt |= BitUtils.bitAt(13);
        }
        if (this.mMovementGranularities != DEFAULT.mMovementGranularities) {
            jBitAt |= BitUtils.bitAt(14);
        }
        if (this.mBooleanProperties != DEFAULT.mBooleanProperties) {
            jBitAt |= BitUtils.bitAt(15);
        }
        if (!Objects.equals(this.mPackageName, DEFAULT.mPackageName)) {
            jBitAt |= BitUtils.bitAt(16);
        }
        if (!Objects.equals(this.mClassName, DEFAULT.mClassName)) {
            jBitAt |= BitUtils.bitAt(17);
        }
        if (!Objects.equals(this.mText, DEFAULT.mText)) {
            jBitAt |= BitUtils.bitAt(18);
        }
        if (!Objects.equals(this.mHintText, DEFAULT.mHintText)) {
            jBitAt |= BitUtils.bitAt(19);
        }
        if (!Objects.equals(this.mError, DEFAULT.mError)) {
            jBitAt |= BitUtils.bitAt(20);
        }
        if (!Objects.equals(this.mContentDescription, DEFAULT.mContentDescription)) {
            jBitAt |= BitUtils.bitAt(21);
        }
        if (!Objects.equals(this.mPaneTitle, DEFAULT.mPaneTitle)) {
            jBitAt |= BitUtils.bitAt(22);
        }
        if (!Objects.equals(this.mTooltipText, DEFAULT.mTooltipText)) {
            jBitAt |= BitUtils.bitAt(23);
        }
        if (!Objects.equals(this.mViewIdResourceName, DEFAULT.mViewIdResourceName)) {
            jBitAt |= BitUtils.bitAt(24);
        }
        if (this.mTextSelectionStart != DEFAULT.mTextSelectionStart) {
            jBitAt |= BitUtils.bitAt(25);
        }
        if (this.mTextSelectionEnd != DEFAULT.mTextSelectionEnd) {
            jBitAt |= BitUtils.bitAt(26);
        }
        if (this.mInputType != DEFAULT.mInputType) {
            jBitAt |= BitUtils.bitAt(27);
        }
        if (this.mLiveRegion != DEFAULT.mLiveRegion) {
            jBitAt |= BitUtils.bitAt(28);
        }
        if (this.mDrawingOrderInParent != DEFAULT.mDrawingOrderInParent) {
            jBitAt |= BitUtils.bitAt(29);
        }
        if (!Objects.equals(this.mExtraDataKeys, DEFAULT.mExtraDataKeys)) {
            jBitAt |= BitUtils.bitAt(30);
        }
        if (!Objects.equals(this.mExtras, DEFAULT.mExtras)) {
            jBitAt |= BitUtils.bitAt(31);
        }
        if (!Objects.equals(this.mRangeInfo, DEFAULT.mRangeInfo)) {
            jBitAt |= BitUtils.bitAt(32);
        }
        if (!Objects.equals(this.mCollectionInfo, DEFAULT.mCollectionInfo)) {
            jBitAt |= BitUtils.bitAt(33);
        }
        if (!Objects.equals(this.mCollectionItemInfo, DEFAULT.mCollectionItemInfo)) {
            jBitAt |= BitUtils.bitAt(34);
        }
        parcel.writeLong(jBitAt);
        if (BitUtils.isBitSet(jBitAt, 0)) {
            parcel.writeInt(isSealed() ? 1 : 0);
        }
        if (BitUtils.isBitSet(jBitAt, 1)) {
            parcel.writeLong(this.mSourceNodeId);
        }
        if (BitUtils.isBitSet(jBitAt, 2)) {
            parcel.writeInt(this.mWindowId);
        }
        if (BitUtils.isBitSet(jBitAt, 3)) {
            parcel.writeLong(this.mParentNodeId);
        }
        if (BitUtils.isBitSet(jBitAt, 4)) {
            parcel.writeLong(this.mLabelForId);
        }
        if (BitUtils.isBitSet(jBitAt, 5)) {
            parcel.writeLong(this.mLabeledById);
        }
        if (BitUtils.isBitSet(jBitAt, 6)) {
            parcel.writeLong(this.mTraversalBefore);
        }
        if (BitUtils.isBitSet(jBitAt, 7)) {
            parcel.writeLong(this.mTraversalAfter);
        }
        if (BitUtils.isBitSet(jBitAt, 8)) {
            parcel.writeInt(this.mConnectionId);
        }
        if (BitUtils.isBitSet(jBitAt, 9)) {
            LongArray longArray = this.mChildNodeIds;
            if (longArray == null) {
                parcel.writeInt(0);
            } else {
                int size = longArray.size();
                parcel.writeInt(size);
                for (int i2 = 0; i2 < size; i2++) {
                    parcel.writeLong(longArray.get(i2));
                }
            }
        }
        if (BitUtils.isBitSet(jBitAt, 10)) {
            parcel.writeInt(this.mBoundsInParent.top);
            parcel.writeInt(this.mBoundsInParent.bottom);
            parcel.writeInt(this.mBoundsInParent.left);
            parcel.writeInt(this.mBoundsInParent.right);
        }
        if (BitUtils.isBitSet(jBitAt, 11)) {
            parcel.writeInt(this.mBoundsInScreen.top);
            parcel.writeInt(this.mBoundsInScreen.bottom);
            parcel.writeInt(this.mBoundsInScreen.left);
            parcel.writeInt(this.mBoundsInScreen.right);
        }
        if (BitUtils.isBitSet(jBitAt, 12)) {
            if (this.mActions != null && !this.mActions.isEmpty()) {
                int size2 = this.mActions.size();
                int i3 = 0;
                long j = 0;
                for (int i4 = 0; i4 < size2; i4++) {
                    AccessibilityAction accessibilityAction = this.mActions.get(i4);
                    if (isDefaultStandardAction(accessibilityAction)) {
                        j |= accessibilityAction.mSerializationFlag;
                    } else {
                        i3++;
                    }
                }
                parcel.writeLong(j);
                parcel.writeInt(i3);
                for (int i5 = 0; i5 < size2; i5++) {
                    AccessibilityAction accessibilityAction2 = this.mActions.get(i5);
                    if (!isDefaultStandardAction(accessibilityAction2)) {
                        parcel.writeInt(accessibilityAction2.getId());
                        parcel.writeCharSequence(accessibilityAction2.getLabel());
                    }
                }
            } else {
                parcel.writeLong(0L);
                parcel.writeInt(0);
            }
        }
        if (BitUtils.isBitSet(jBitAt, 13)) {
            parcel.writeInt(this.mMaxTextLength);
        }
        if (BitUtils.isBitSet(jBitAt, 14)) {
            parcel.writeInt(this.mMovementGranularities);
        }
        if (BitUtils.isBitSet(jBitAt, 15)) {
            parcel.writeInt(this.mBooleanProperties);
        }
        if (BitUtils.isBitSet(jBitAt, 16)) {
            parcel.writeCharSequence(this.mPackageName);
        }
        if (BitUtils.isBitSet(jBitAt, 17)) {
            parcel.writeCharSequence(this.mClassName);
        }
        if (BitUtils.isBitSet(jBitAt, 18)) {
            parcel.writeCharSequence(this.mText);
        }
        if (BitUtils.isBitSet(jBitAt, 19)) {
            parcel.writeCharSequence(this.mHintText);
        }
        if (BitUtils.isBitSet(jBitAt, 20)) {
            parcel.writeCharSequence(this.mError);
        }
        if (BitUtils.isBitSet(jBitAt, 21)) {
            parcel.writeCharSequence(this.mContentDescription);
        }
        if (BitUtils.isBitSet(jBitAt, 22)) {
            parcel.writeCharSequence(this.mPaneTitle);
        }
        if (BitUtils.isBitSet(jBitAt, 23)) {
            parcel.writeCharSequence(this.mTooltipText);
        }
        if (BitUtils.isBitSet(jBitAt, 24)) {
            parcel.writeString(this.mViewIdResourceName);
        }
        if (BitUtils.isBitSet(jBitAt, 25)) {
            parcel.writeInt(this.mTextSelectionStart);
        }
        if (BitUtils.isBitSet(jBitAt, 26)) {
            parcel.writeInt(this.mTextSelectionEnd);
        }
        if (BitUtils.isBitSet(jBitAt, 27)) {
            parcel.writeInt(this.mInputType);
        }
        if (BitUtils.isBitSet(jBitAt, 28)) {
            parcel.writeInt(this.mLiveRegion);
        }
        if (BitUtils.isBitSet(jBitAt, 29)) {
            parcel.writeInt(this.mDrawingOrderInParent);
        }
        if (BitUtils.isBitSet(jBitAt, 30)) {
            parcel.writeStringList(this.mExtraDataKeys);
        }
        if (BitUtils.isBitSet(jBitAt, 31)) {
            parcel.writeBundle(this.mExtras);
        }
        if (BitUtils.isBitSet(jBitAt, 32)) {
            parcel.writeInt(this.mRangeInfo.getType());
            parcel.writeFloat(this.mRangeInfo.getMin());
            parcel.writeFloat(this.mRangeInfo.getMax());
            parcel.writeFloat(this.mRangeInfo.getCurrent());
        }
        if (BitUtils.isBitSet(jBitAt, 33)) {
            parcel.writeInt(this.mCollectionInfo.getRowCount());
            parcel.writeInt(this.mCollectionInfo.getColumnCount());
            parcel.writeInt(this.mCollectionInfo.isHierarchical() ? 1 : 0);
            parcel.writeInt(this.mCollectionInfo.getSelectionMode());
        }
        if (BitUtils.isBitSet(jBitAt, 34)) {
            parcel.writeInt(this.mCollectionItemInfo.getRowIndex());
            parcel.writeInt(this.mCollectionItemInfo.getRowSpan());
            parcel.writeInt(this.mCollectionItemInfo.getColumnIndex());
            parcel.writeInt(this.mCollectionItemInfo.getColumnSpan());
            parcel.writeInt(this.mCollectionItemInfo.isHeading() ? 1 : 0);
            parcel.writeInt(this.mCollectionItemInfo.isSelected() ? 1 : 0);
        }
    }

    private void init(AccessibilityNodeInfo accessibilityNodeInfo) {
        this.mSealed = accessibilityNodeInfo.mSealed;
        this.mSourceNodeId = accessibilityNodeInfo.mSourceNodeId;
        this.mParentNodeId = accessibilityNodeInfo.mParentNodeId;
        this.mLabelForId = accessibilityNodeInfo.mLabelForId;
        this.mLabeledById = accessibilityNodeInfo.mLabeledById;
        this.mTraversalBefore = accessibilityNodeInfo.mTraversalBefore;
        this.mTraversalAfter = accessibilityNodeInfo.mTraversalAfter;
        this.mWindowId = accessibilityNodeInfo.mWindowId;
        this.mConnectionId = accessibilityNodeInfo.mConnectionId;
        this.mBoundsInParent.set(accessibilityNodeInfo.mBoundsInParent);
        this.mBoundsInScreen.set(accessibilityNodeInfo.mBoundsInScreen);
        this.mPackageName = accessibilityNodeInfo.mPackageName;
        this.mClassName = accessibilityNodeInfo.mClassName;
        this.mText = accessibilityNodeInfo.mText;
        this.mOriginalText = accessibilityNodeInfo.mOriginalText;
        this.mHintText = accessibilityNodeInfo.mHintText;
        this.mError = accessibilityNodeInfo.mError;
        this.mContentDescription = accessibilityNodeInfo.mContentDescription;
        this.mPaneTitle = accessibilityNodeInfo.mPaneTitle;
        this.mTooltipText = accessibilityNodeInfo.mTooltipText;
        this.mViewIdResourceName = accessibilityNodeInfo.mViewIdResourceName;
        if (this.mActions != null) {
            this.mActions.clear();
        }
        ArrayList<AccessibilityAction> arrayList = accessibilityNodeInfo.mActions;
        if (arrayList != null && arrayList.size() > 0) {
            if (this.mActions == null) {
                this.mActions = new ArrayList<>(arrayList);
            } else {
                this.mActions.addAll(accessibilityNodeInfo.mActions);
            }
        }
        this.mBooleanProperties = accessibilityNodeInfo.mBooleanProperties;
        this.mMaxTextLength = accessibilityNodeInfo.mMaxTextLength;
        this.mMovementGranularities = accessibilityNodeInfo.mMovementGranularities;
        if (this.mChildNodeIds != null) {
            this.mChildNodeIds.clear();
        }
        LongArray longArray = accessibilityNodeInfo.mChildNodeIds;
        if (longArray != null && longArray.size() > 0) {
            if (this.mChildNodeIds == null) {
                this.mChildNodeIds = longArray.m32clone();
            } else {
                this.mChildNodeIds.addAll(longArray);
            }
        }
        this.mTextSelectionStart = accessibilityNodeInfo.mTextSelectionStart;
        this.mTextSelectionEnd = accessibilityNodeInfo.mTextSelectionEnd;
        this.mInputType = accessibilityNodeInfo.mInputType;
        this.mLiveRegion = accessibilityNodeInfo.mLiveRegion;
        this.mDrawingOrderInParent = accessibilityNodeInfo.mDrawingOrderInParent;
        this.mExtraDataKeys = accessibilityNodeInfo.mExtraDataKeys;
        this.mExtras = accessibilityNodeInfo.mExtras != null ? new Bundle(accessibilityNodeInfo.mExtras) : null;
        if (this.mRangeInfo != null) {
            this.mRangeInfo.recycle();
        }
        this.mRangeInfo = accessibilityNodeInfo.mRangeInfo != null ? RangeInfo.obtain(accessibilityNodeInfo.mRangeInfo) : null;
        if (this.mCollectionInfo != null) {
            this.mCollectionInfo.recycle();
        }
        this.mCollectionInfo = accessibilityNodeInfo.mCollectionInfo != null ? CollectionInfo.obtain(accessibilityNodeInfo.mCollectionInfo) : null;
        if (this.mCollectionItemInfo != null) {
            this.mCollectionItemInfo.recycle();
        }
        this.mCollectionItemInfo = accessibilityNodeInfo.mCollectionItemInfo != null ? CollectionItemInfo.obtain(accessibilityNodeInfo.mCollectionItemInfo) : null;
    }

    private void initFromParcel(Parcel parcel) {
        boolean z;
        ArrayList<String> arrayListCreateStringArrayList;
        Bundle bundle;
        RangeInfo rangeInfoObtain;
        CollectionInfo collectionInfoObtain;
        long j = parcel.readLong();
        if (!BitUtils.isBitSet(j, 0)) {
            z = DEFAULT.mSealed;
        } else {
            z = parcel.readInt() == 1;
        }
        if (BitUtils.isBitSet(j, 1)) {
            this.mSourceNodeId = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 2)) {
            this.mWindowId = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 3)) {
            this.mParentNodeId = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 4)) {
            this.mLabelForId = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 5)) {
            this.mLabeledById = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 6)) {
            this.mTraversalBefore = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 7)) {
            this.mTraversalAfter = parcel.readLong();
        }
        if (BitUtils.isBitSet(j, 8)) {
            this.mConnectionId = parcel.readInt();
        }
        CollectionItemInfo collectionItemInfoObtain = null;
        if (BitUtils.isBitSet(j, 9)) {
            int i = parcel.readInt();
            if (i <= 0) {
                this.mChildNodeIds = null;
            } else {
                this.mChildNodeIds = new LongArray(i);
                for (int i2 = 0; i2 < i; i2++) {
                    this.mChildNodeIds.add(parcel.readLong());
                }
            }
        }
        if (BitUtils.isBitSet(j, 10)) {
            this.mBoundsInParent.top = parcel.readInt();
            this.mBoundsInParent.bottom = parcel.readInt();
            this.mBoundsInParent.left = parcel.readInt();
            this.mBoundsInParent.right = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 11)) {
            this.mBoundsInScreen.top = parcel.readInt();
            this.mBoundsInScreen.bottom = parcel.readInt();
            this.mBoundsInScreen.left = parcel.readInt();
            this.mBoundsInScreen.right = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 12)) {
            addStandardActions(parcel.readLong());
            int i3 = parcel.readInt();
            for (int i4 = 0; i4 < i3; i4++) {
                addActionUnchecked(new AccessibilityAction(parcel.readInt(), parcel.readCharSequence()));
            }
        }
        if (BitUtils.isBitSet(j, 13)) {
            this.mMaxTextLength = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 14)) {
            this.mMovementGranularities = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 15)) {
            this.mBooleanProperties = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 16)) {
            this.mPackageName = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 17)) {
            this.mClassName = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 18)) {
            this.mText = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 19)) {
            this.mHintText = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 20)) {
            this.mError = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 21)) {
            this.mContentDescription = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 22)) {
            this.mPaneTitle = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 23)) {
            this.mTooltipText = parcel.readCharSequence();
        }
        if (BitUtils.isBitSet(j, 24)) {
            this.mViewIdResourceName = parcel.readString();
        }
        if (BitUtils.isBitSet(j, 25)) {
            this.mTextSelectionStart = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 26)) {
            this.mTextSelectionEnd = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 27)) {
            this.mInputType = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 28)) {
            this.mLiveRegion = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 29)) {
            this.mDrawingOrderInParent = parcel.readInt();
        }
        if (BitUtils.isBitSet(j, 30)) {
            arrayListCreateStringArrayList = parcel.createStringArrayList();
        } else {
            arrayListCreateStringArrayList = null;
        }
        this.mExtraDataKeys = arrayListCreateStringArrayList;
        if (BitUtils.isBitSet(j, 31)) {
            bundle = parcel.readBundle();
        } else {
            bundle = null;
        }
        this.mExtras = bundle;
        if (this.mRangeInfo != null) {
            this.mRangeInfo.recycle();
        }
        if (BitUtils.isBitSet(j, 32)) {
            rangeInfoObtain = RangeInfo.obtain(parcel.readInt(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat());
        } else {
            rangeInfoObtain = null;
        }
        this.mRangeInfo = rangeInfoObtain;
        if (this.mCollectionInfo != null) {
            this.mCollectionInfo.recycle();
        }
        if (BitUtils.isBitSet(j, 33)) {
            collectionInfoObtain = CollectionInfo.obtain(parcel.readInt(), parcel.readInt(), parcel.readInt() == 1, parcel.readInt());
        } else {
            collectionInfoObtain = null;
        }
        this.mCollectionInfo = collectionInfoObtain;
        if (this.mCollectionItemInfo != null) {
            this.mCollectionItemInfo.recycle();
        }
        if (BitUtils.isBitSet(j, 34)) {
            collectionItemInfoObtain = CollectionItemInfo.obtain(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt() == 1, parcel.readInt() == 1);
        }
        this.mCollectionItemInfo = collectionItemInfoObtain;
        this.mSealed = z;
    }

    private void clear() {
        init(DEFAULT);
    }

    private static boolean isDefaultStandardAction(AccessibilityAction accessibilityAction) {
        return accessibilityAction.mSerializationFlag != -1 && TextUtils.isEmpty(accessibilityAction.getLabel());
    }

    private static AccessibilityAction getActionSingleton(int i) {
        int size = AccessibilityAction.sStandardActions.size();
        for (int i2 = 0; i2 < size; i2++) {
            AccessibilityAction accessibilityActionValueAt = AccessibilityAction.sStandardActions.valueAt(i2);
            if (i == accessibilityActionValueAt.getId()) {
                return accessibilityActionValueAt;
            }
        }
        return null;
    }

    private static AccessibilityAction getActionSingletonBySerializationFlag(long j) {
        int size = AccessibilityAction.sStandardActions.size();
        for (int i = 0; i < size; i++) {
            AccessibilityAction accessibilityActionValueAt = AccessibilityAction.sStandardActions.valueAt(i);
            if (j == accessibilityActionValueAt.mSerializationFlag) {
                return accessibilityActionValueAt;
            }
        }
        return null;
    }

    private void addStandardActions(long j) {
        while (j > 0) {
            long jNumberOfTrailingZeros = 1 << Long.numberOfTrailingZeros(j);
            j &= ~jNumberOfTrailingZeros;
            addAction(getActionSingletonBySerializationFlag(jNumberOfTrailingZeros));
        }
    }

    private static String getActionSymbolicName(int i) {
        switch (i) {
            case 1:
                return "ACTION_FOCUS";
            case 2:
                return "ACTION_CLEAR_FOCUS";
            default:
                switch (i) {
                    case 16908342:
                        return "ACTION_SHOW_ON_SCREEN";
                    case 16908343:
                        return "ACTION_SCROLL_TO_POSITION";
                    case 16908344:
                        return "ACTION_SCROLL_UP";
                    case 16908345:
                        return "ACTION_SCROLL_LEFT";
                    case 16908346:
                        return "ACTION_SCROLL_DOWN";
                    case 16908347:
                        return "ACTION_SCROLL_RIGHT";
                    case 16908348:
                        return "ACTION_CONTEXT_CLICK";
                    case 16908349:
                        return "ACTION_SET_PROGRESS";
                    default:
                        switch (i) {
                            case 16908356:
                                return "ACTION_SHOW_TOOLTIP";
                            case 16908357:
                                return "ACTION_HIDE_TOOLTIP";
                            default:
                                switch (i) {
                                    case 4:
                                        return "ACTION_SELECT";
                                    case 8:
                                        return "ACTION_CLEAR_SELECTION";
                                    case 16:
                                        return "ACTION_CLICK";
                                    case 32:
                                        return "ACTION_LONG_CLICK";
                                    case 64:
                                        return "ACTION_ACCESSIBILITY_FOCUS";
                                    case 128:
                                        return "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
                                    case 256:
                                        return "ACTION_NEXT_AT_MOVEMENT_GRANULARITY";
                                    case 512:
                                        return "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY";
                                    case 1024:
                                        return "ACTION_NEXT_HTML_ELEMENT";
                                    case 2048:
                                        return "ACTION_PREVIOUS_HTML_ELEMENT";
                                    case 4096:
                                        return "ACTION_SCROLL_FORWARD";
                                    case 8192:
                                        return "ACTION_SCROLL_BACKWARD";
                                    case 16384:
                                        return "ACTION_COPY";
                                    case 32768:
                                        return "ACTION_PASTE";
                                    case 65536:
                                        return "ACTION_CUT";
                                    case 131072:
                                        return "ACTION_SET_SELECTION";
                                    case 262144:
                                        return "ACTION_EXPAND";
                                    case 524288:
                                        return "ACTION_COLLAPSE";
                                    case 1048576:
                                        return "ACTION_DISMISS";
                                    case 2097152:
                                        return "ACTION_SET_TEXT";
                                    default:
                                        return "ACTION_UNKNOWN";
                                }
                        }
                }
        }
    }

    private static String getMovementGranularitySymbolicName(int i) {
        if (i == 4) {
            return "MOVEMENT_GRANULARITY_LINE";
        }
        if (i == 8) {
            return "MOVEMENT_GRANULARITY_PARAGRAPH";
        }
        if (i != 16) {
            switch (i) {
                case 1:
                    return "MOVEMENT_GRANULARITY_CHARACTER";
                case 2:
                    return "MOVEMENT_GRANULARITY_WORD";
                default:
                    throw new IllegalArgumentException("Unknown movement granularity: " + i);
            }
        }
        return "MOVEMENT_GRANULARITY_PAGE";
    }

    private boolean canPerformRequestOverConnection(long j) {
        return (this.mWindowId == -1 || getAccessibilityViewId(j) == Integer.MAX_VALUE || this.mConnectionId == -1) ? false : true;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) obj;
        if (this.mSourceNodeId == accessibilityNodeInfo.mSourceNodeId && this.mWindowId == accessibilityNodeInfo.mWindowId) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((getAccessibilityViewId(this.mSourceNodeId) + 31) * 31) + getVirtualDescendantId(this.mSourceNodeId))) + this.mWindowId;
    }

    public String toString() {
        return super.toString() + "; boundsInParent: " + this.mBoundsInParent + "; boundsInScreen: " + this.mBoundsInScreen + "; packageName: " + this.mPackageName + "; className: " + this.mClassName + "; text: " + this.mText + "; error: " + this.mError + "; maxTextLength: " + this.mMaxTextLength + "; contentDescription: " + this.mContentDescription + "; tooltipText: " + this.mTooltipText + "; viewIdResName: " + this.mViewIdResourceName + "; checkable: " + isCheckable() + "; checked: " + isChecked() + "; focusable: " + isFocusable() + "; focused: " + isFocused() + "; selected: " + isSelected() + "; clickable: " + isClickable() + "; longClickable: " + isLongClickable() + "; contextClickable: " + isContextClickable() + "; enabled: " + isEnabled() + "; password: " + isPassword() + "; scrollable: " + isScrollable() + "; importantForAccessibility: " + isImportantForAccessibility() + "; visible: " + isVisibleToUser() + "; actions: " + this.mActions;
    }

    private AccessibilityNodeInfo getNodeForAccessibilityId(long j) {
        if (!canPerformRequestOverConnection(j)) {
            return null;
        }
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(this.mConnectionId, this.mWindowId, j, false, 7, null);
    }

    public static String idToString(long j) {
        int accessibilityViewId = getAccessibilityViewId(j);
        int virtualDescendantId = getVirtualDescendantId(j);
        if (virtualDescendantId == -1) {
            return idItemToString(accessibilityViewId);
        }
        return idItemToString(accessibilityViewId) + SettingsStringUtil.DELIMITER + idItemToString(virtualDescendantId);
    }

    private static String idItemToString(int i) {
        if (i != -1) {
            switch (i) {
                case 2147483646:
                    return "ROOT";
                case Integer.MAX_VALUE:
                    return "UNDEFINED";
                default:
                    return "" + i;
            }
        }
        return "HOST";
    }

    public static final class AccessibilityAction {
        private final int mActionId;
        private final CharSequence mLabel;
        public long mSerializationFlag;
        public static final ArraySet<AccessibilityAction> sStandardActions = new ArraySet<>();
        public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(1);
        public static final AccessibilityAction ACTION_CLEAR_FOCUS = new AccessibilityAction(2);
        public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(4);
        public static final AccessibilityAction ACTION_CLEAR_SELECTION = new AccessibilityAction(8);
        public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(16);
        public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(32);
        public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS = new AccessibilityAction(64);
        public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS = new AccessibilityAction(128);
        public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(256);
        public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(512);
        public static final AccessibilityAction ACTION_NEXT_HTML_ELEMENT = new AccessibilityAction(1024);
        public static final AccessibilityAction ACTION_PREVIOUS_HTML_ELEMENT = new AccessibilityAction(2048);
        public static final AccessibilityAction ACTION_SCROLL_FORWARD = new AccessibilityAction(4096);
        public static final AccessibilityAction ACTION_SCROLL_BACKWARD = new AccessibilityAction(8192);
        public static final AccessibilityAction ACTION_COPY = new AccessibilityAction(16384);
        public static final AccessibilityAction ACTION_PASTE = new AccessibilityAction(32768);
        public static final AccessibilityAction ACTION_CUT = new AccessibilityAction(65536);
        public static final AccessibilityAction ACTION_SET_SELECTION = new AccessibilityAction(131072);
        public static final AccessibilityAction ACTION_EXPAND = new AccessibilityAction(262144);
        public static final AccessibilityAction ACTION_COLLAPSE = new AccessibilityAction(524288);
        public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(1048576);
        public static final AccessibilityAction ACTION_SET_TEXT = new AccessibilityAction(2097152);
        public static final AccessibilityAction ACTION_SHOW_ON_SCREEN = new AccessibilityAction(16908342);
        public static final AccessibilityAction ACTION_SCROLL_TO_POSITION = new AccessibilityAction(16908343);
        public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(16908344);
        public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(16908345);
        public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(16908346);
        public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(16908347);
        public static final AccessibilityAction ACTION_CONTEXT_CLICK = new AccessibilityAction(16908348);
        public static final AccessibilityAction ACTION_SET_PROGRESS = new AccessibilityAction(16908349);
        public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(16908354);
        public static final AccessibilityAction ACTION_SHOW_TOOLTIP = new AccessibilityAction(16908356);
        public static final AccessibilityAction ACTION_HIDE_TOOLTIP = new AccessibilityAction(16908357);

        public AccessibilityAction(int i, CharSequence charSequence) {
            this.mSerializationFlag = -1L;
            if (((-16777216) & i) == 0 && Integer.bitCount(i) != 1) {
                throw new IllegalArgumentException("Invalid standard action id");
            }
            this.mActionId = i;
            this.mLabel = charSequence;
        }

        private AccessibilityAction(int i) {
            this(i, null);
            this.mSerializationFlag = BitUtils.bitAt(sStandardActions.size());
            sStandardActions.add(this);
        }

        public int getId() {
            return this.mActionId;
        }

        public CharSequence getLabel() {
            return this.mLabel;
        }

        public int hashCode() {
            return this.mActionId;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (getClass() != obj.getClass() || this.mActionId != ((AccessibilityAction) obj).mActionId) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "AccessibilityAction: " + AccessibilityNodeInfo.getActionSymbolicName(this.mActionId) + " - " + ((Object) this.mLabel);
        }
    }

    public static final class RangeInfo {
        private static final int MAX_POOL_SIZE = 10;
        public static final int RANGE_TYPE_FLOAT = 1;
        public static final int RANGE_TYPE_INT = 0;
        public static final int RANGE_TYPE_PERCENT = 2;
        private static final Pools.SynchronizedPool<RangeInfo> sPool = new Pools.SynchronizedPool<>(10);
        private float mCurrent;
        private float mMax;
        private float mMin;
        private int mType;

        public static RangeInfo obtain(RangeInfo rangeInfo) {
            return obtain(rangeInfo.mType, rangeInfo.mMin, rangeInfo.mMax, rangeInfo.mCurrent);
        }

        public static RangeInfo obtain(int i, float f, float f2, float f3) {
            RangeInfo rangeInfoAcquire = sPool.acquire();
            if (rangeInfoAcquire == null) {
                return new RangeInfo(i, f, f2, f3);
            }
            rangeInfoAcquire.mType = i;
            rangeInfoAcquire.mMin = f;
            rangeInfoAcquire.mMax = f2;
            rangeInfoAcquire.mCurrent = f3;
            return rangeInfoAcquire;
        }

        private RangeInfo(int i, float f, float f2, float f3) {
            this.mType = i;
            this.mMin = f;
            this.mMax = f2;
            this.mCurrent = f3;
        }

        public int getType() {
            return this.mType;
        }

        public float getMin() {
            return this.mMin;
        }

        public float getMax() {
            return this.mMax;
        }

        public float getCurrent() {
            return this.mCurrent;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mType = 0;
            this.mMin = 0.0f;
            this.mMax = 0.0f;
            this.mCurrent = 0.0f;
        }
    }

    public static final class CollectionInfo {
        private static final int MAX_POOL_SIZE = 20;
        public static final int SELECTION_MODE_MULTIPLE = 2;
        public static final int SELECTION_MODE_NONE = 0;
        public static final int SELECTION_MODE_SINGLE = 1;
        private static final Pools.SynchronizedPool<CollectionInfo> sPool = new Pools.SynchronizedPool<>(20);
        private int mColumnCount;
        private boolean mHierarchical;
        private int mRowCount;
        private int mSelectionMode;

        public static CollectionInfo obtain(CollectionInfo collectionInfo) {
            return obtain(collectionInfo.mRowCount, collectionInfo.mColumnCount, collectionInfo.mHierarchical, collectionInfo.mSelectionMode);
        }

        public static CollectionInfo obtain(int i, int i2, boolean z) {
            return obtain(i, i2, z, 0);
        }

        public static CollectionInfo obtain(int i, int i2, boolean z, int i3) {
            CollectionInfo collectionInfoAcquire = sPool.acquire();
            if (collectionInfoAcquire == null) {
                return new CollectionInfo(i, i2, z, i3);
            }
            collectionInfoAcquire.mRowCount = i;
            collectionInfoAcquire.mColumnCount = i2;
            collectionInfoAcquire.mHierarchical = z;
            collectionInfoAcquire.mSelectionMode = i3;
            return collectionInfoAcquire;
        }

        private CollectionInfo(int i, int i2, boolean z, int i3) {
            this.mRowCount = i;
            this.mColumnCount = i2;
            this.mHierarchical = z;
            this.mSelectionMode = i3;
        }

        public int getRowCount() {
            return this.mRowCount;
        }

        public int getColumnCount() {
            return this.mColumnCount;
        }

        public boolean isHierarchical() {
            return this.mHierarchical;
        }

        public int getSelectionMode() {
            return this.mSelectionMode;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mRowCount = 0;
            this.mColumnCount = 0;
            this.mHierarchical = false;
            this.mSelectionMode = 0;
        }
    }

    public static final class CollectionItemInfo {
        private static final int MAX_POOL_SIZE = 20;
        private static final Pools.SynchronizedPool<CollectionItemInfo> sPool = new Pools.SynchronizedPool<>(20);
        private int mColumnIndex;
        private int mColumnSpan;
        private boolean mHeading;
        private int mRowIndex;
        private int mRowSpan;
        private boolean mSelected;

        public static CollectionItemInfo obtain(CollectionItemInfo collectionItemInfo) {
            return obtain(collectionItemInfo.mRowIndex, collectionItemInfo.mRowSpan, collectionItemInfo.mColumnIndex, collectionItemInfo.mColumnSpan, collectionItemInfo.mHeading, collectionItemInfo.mSelected);
        }

        public static CollectionItemInfo obtain(int i, int i2, int i3, int i4, boolean z) {
            return obtain(i, i2, i3, i4, z, false);
        }

        public static CollectionItemInfo obtain(int i, int i2, int i3, int i4, boolean z, boolean z2) {
            CollectionItemInfo collectionItemInfoAcquire = sPool.acquire();
            if (collectionItemInfoAcquire == null) {
                return new CollectionItemInfo(i, i2, i3, i4, z, z2);
            }
            collectionItemInfoAcquire.mRowIndex = i;
            collectionItemInfoAcquire.mRowSpan = i2;
            collectionItemInfoAcquire.mColumnIndex = i3;
            collectionItemInfoAcquire.mColumnSpan = i4;
            collectionItemInfoAcquire.mHeading = z;
            collectionItemInfoAcquire.mSelected = z2;
            return collectionItemInfoAcquire;
        }

        private CollectionItemInfo(int i, int i2, int i3, int i4, boolean z, boolean z2) {
            this.mRowIndex = i;
            this.mRowSpan = i2;
            this.mColumnIndex = i3;
            this.mColumnSpan = i4;
            this.mHeading = z;
            this.mSelected = z2;
        }

        public int getColumnIndex() {
            return this.mColumnIndex;
        }

        public int getRowIndex() {
            return this.mRowIndex;
        }

        public int getColumnSpan() {
            return this.mColumnSpan;
        }

        public int getRowSpan() {
            return this.mRowSpan;
        }

        public boolean isHeading() {
            return this.mHeading;
        }

        public boolean isSelected() {
            return this.mSelected;
        }

        void recycle() {
            clear();
            sPool.release(this);
        }

        private void clear() {
            this.mColumnIndex = 0;
            this.mColumnSpan = 0;
            this.mRowIndex = 0;
            this.mRowSpan = 0;
            this.mHeading = false;
            this.mSelected = false;
        }
    }
}
