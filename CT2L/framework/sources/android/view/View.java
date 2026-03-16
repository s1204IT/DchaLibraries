package android.view;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Interpolator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerGlobal;
import android.media.AudioSystem;
import android.media.TtmlUtils;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.powerhint.PowerHintManager;
import android.provider.ContactsContract;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatProperty;
import android.util.Log;
import android.util.LongSparseLongArray;
import android.util.Pools;
import android.util.Property;
import android.util.SparseArray;
import android.util.SuperNotCalledException;
import android.util.TypedValue;
import android.view.AccessibilityIterators;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.ScrollBarDrawable;
import com.android.internal.R;
import com.android.internal.util.Predicate;
import com.android.internal.view.menu.MenuBuilder;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class View implements Drawable.Callback, KeyEvent.Callback, AccessibilityEventSource {
    public static final int ACCESSIBILITY_CURSOR_POSITION_UNDEFINED = -1;
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 2;
    static final int ACCESSIBILITY_LIVE_REGION_DEFAULT = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    static final int ALL_RTL_PROPERTIES_RESOLVED = 1610678816;
    public static final Property<View, Float> ALPHA;
    static final int CLICKABLE = 16384;
    private static final boolean DBG = false;
    public static final String DEBUG_LAYOUT_PROPERTY = "debug.layout";
    static final int DISABLED = 32;
    public static final int DRAG_FLAG_GLOBAL = 1;
    static final int DRAG_MASK = 3;
    static final int DRAWING_CACHE_ENABLED = 32768;
    public static final int DRAWING_CACHE_QUALITY_AUTO = 0;
    public static final int DRAWING_CACHE_QUALITY_HIGH = 1048576;
    public static final int DRAWING_CACHE_QUALITY_LOW = 524288;
    static final int DRAWING_CACHE_QUALITY_MASK = 1572864;
    static final int DRAW_MASK = 128;
    static final int DUPLICATE_PARENT_STATE = 4194304;
    protected static final int[] EMPTY_STATE_SET;
    static final int ENABLED = 0;
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET;
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] ENABLED_FOCUSED_STATE_SET;
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    static final int ENABLED_MASK = 32;
    protected static final int[] ENABLED_SELECTED_STATE_SET;
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] ENABLED_STATE_SET;
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET;
    static final int FADING_EDGE_HORIZONTAL = 4096;
    static final int FADING_EDGE_MASK = 12288;
    static final int FADING_EDGE_NONE = 0;
    static final int FADING_EDGE_VERTICAL = 8192;
    static final int FILTER_TOUCHES_WHEN_OBSCURED = 1024;
    public static final int FIND_VIEWS_WITH_ACCESSIBILITY_NODE_PROVIDERS = 4;
    public static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 2;
    public static final int FIND_VIEWS_WITH_TEXT = 1;
    private static final int FITS_SYSTEM_WINDOWS = 2;
    private static final int FOCUSABLE = 1;
    public static final int FOCUSABLES_ALL = 0;
    public static final int FOCUSABLES_TOUCH_MODE = 1;
    static final int FOCUSABLE_IN_TOUCH_MODE = 262144;
    private static final int FOCUSABLE_MASK = 1;
    protected static final int[] FOCUSED_SELECTED_STATE_SET;
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] FOCUSED_STATE_SET;
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET;
    public static final int FOCUS_BACKWARD = 1;
    public static final int FOCUS_DOWN = 130;
    public static final int FOCUS_FORWARD = 2;
    public static final int FOCUS_LEFT = 17;
    public static final int FOCUS_RIGHT = 66;
    public static final int FOCUS_UP = 33;
    public static final int GONE = 8;
    public static final int HAPTIC_FEEDBACK_ENABLED = 268435456;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    static final int IMPORTANT_FOR_ACCESSIBILITY_DEFAULT = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int INVISIBLE = 4;
    public static final int KEEP_SCREEN_ON = 67108864;
    public static final int LAYER_TYPE_HARDWARE = 2;
    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    private static final int LAYOUT_DIRECTION_DEFAULT = 2;
    private static final int[] LAYOUT_DIRECTION_FLAGS;
    public static final int LAYOUT_DIRECTION_INHERIT = 2;
    public static final int LAYOUT_DIRECTION_LOCALE = 3;
    public static final int LAYOUT_DIRECTION_LTR = 0;
    static final int LAYOUT_DIRECTION_RESOLVED_DEFAULT = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    static final int LONG_CLICKABLE = 2097152;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_SIZE_MASK = 16777215;
    public static final int MEASURED_STATE_MASK = -16777216;
    public static final int MEASURED_STATE_TOO_SMALL = 16777216;
    public static final int NAVIGATION_BAR_TRANSIENT = 134217728;
    public static final int NAVIGATION_BAR_TRANSLUCENT = Integer.MIN_VALUE;
    public static final int NAVIGATION_BAR_UNHIDE = 536870912;
    private static final int NOT_FOCUSABLE = 0;
    public static final int NO_ID = -1;
    static final int OPTIONAL_FITS_SYSTEM_WINDOWS = 2048;
    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;
    static final int PARENT_SAVE_DISABLED = 536870912;
    static final int PARENT_SAVE_DISABLED_MASK = 536870912;
    static final int PFLAG2_ACCESSIBILITY_FOCUSED = 67108864;
    static final int PFLAG2_ACCESSIBILITY_LIVE_REGION_MASK = 25165824;
    static final int PFLAG2_ACCESSIBILITY_LIVE_REGION_SHIFT = 23;
    static final int PFLAG2_DRAG_CAN_ACCEPT = 1;
    static final int PFLAG2_DRAG_HOVERED = 2;
    static final int PFLAG2_DRAWABLE_RESOLVED = 1073741824;
    static final int PFLAG2_HAS_TRANSIENT_STATE = Integer.MIN_VALUE;
    static final int PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK = 7340032;
    static final int PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_SHIFT = 20;
    static final int PFLAG2_LAYOUT_DIRECTION_MASK = 12;
    static final int PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT = 2;
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED = 32;
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK = 48;
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL = 16;
    static final int PFLAG2_PADDING_RESOLVED = 536870912;
    static final int PFLAG2_SUBTREE_ACCESSIBILITY_STATE_CHANGED = 134217728;
    private static final int[] PFLAG2_TEXT_ALIGNMENT_FLAGS;
    static final int PFLAG2_TEXT_ALIGNMENT_MASK = 57344;
    static final int PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT = 13;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED = 65536;
    private static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT = 131072;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK = 917504;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT = 17;
    private static final int[] PFLAG2_TEXT_DIRECTION_FLAGS;
    static final int PFLAG2_TEXT_DIRECTION_MASK = 448;
    static final int PFLAG2_TEXT_DIRECTION_MASK_SHIFT = 6;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED = 512;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT = 1024;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK = 7168;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT = 10;
    static final int PFLAG2_VIEW_QUICK_REJECTED = 268435456;
    static final int PFLAG3_APPLYING_INSETS = 32;
    static final int PFLAG3_CALLED_SUPER = 16;
    static final int PFLAG3_FITTING_SYSTEM_WINDOWS = 64;
    static final int PFLAG3_IS_LAID_OUT = 4;
    static final int PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT = 8;
    static final int PFLAG3_NESTED_SCROLLING_ENABLED = 128;
    static final int PFLAG3_VIEW_IS_ANIMATING_ALPHA = 2;
    static final int PFLAG3_VIEW_IS_ANIMATING_TRANSFORM = 1;
    static final int PFLAG_ACTIVATED = 1073741824;
    static final int PFLAG_ALPHA_SET = 262144;
    static final int PFLAG_ANIMATION_STARTED = 65536;
    private static final int PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH = 134217728;
    static final int PFLAG_CANCEL_NEXT_UP_EVENT = 67108864;
    static final int PFLAG_DIRTY = 2097152;
    static final int PFLAG_DIRTY_MASK = 6291456;
    static final int PFLAG_DIRTY_OPAQUE = 4194304;
    private static final int PFLAG_DOES_NOTHING_REUSE_PLEASE = 536870912;
    static final int PFLAG_DRAWABLE_STATE_DIRTY = 1024;
    static final int PFLAG_DRAWING_CACHE_VALID = 32768;
    static final int PFLAG_DRAWN = 32;
    static final int PFLAG_DRAW_ANIMATION = 64;
    static final int PFLAG_FOCUSED = 2;
    static final int PFLAG_FORCE_LAYOUT = 4096;
    static final int PFLAG_HAS_BOUNDS = 16;
    private static final int PFLAG_HOVERED = 268435456;
    static final int PFLAG_INVALIDATED = Integer.MIN_VALUE;
    static final int PFLAG_IS_ROOT_NAMESPACE = 8;
    static final int PFLAG_LAYOUT_REQUIRED = 8192;
    static final int PFLAG_MEASURED_DIMENSION_SET = 2048;
    static final int PFLAG_ONLY_DRAWS_BACKGROUND = 256;
    static final int PFLAG_OPAQUE_BACKGROUND = 8388608;
    static final int PFLAG_OPAQUE_MASK = 25165824;
    static final int PFLAG_OPAQUE_SCROLLBARS = 16777216;
    private static final int PFLAG_PREPRESSED = 33554432;
    private static final int PFLAG_PRESSED = 16384;
    static final int PFLAG_REQUEST_TRANSPARENT_REGIONS = 512;
    private static final int PFLAG_SAVE_STATE_CALLED = 131072;
    static final int PFLAG_SCROLL_CONTAINER = 524288;
    static final int PFLAG_SCROLL_CONTAINER_ADDED = 1048576;
    static final int PFLAG_SELECTED = 4;
    static final int PFLAG_SKIP_DRAW = 128;
    static final int PFLAG_WANTS_FOCUS = 1;
    private static final int POPULATING_ACCESSIBILITY_EVENT_TYPES = 172479;
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_STATE_SET;
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET;
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_SELECTED_STATE_SET;
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    protected static final int[] PRESSED_STATE_SET;
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET;
    private static final int PROVIDER_BACKGROUND = 0;
    private static final int PROVIDER_BOUNDS = 2;
    private static final int PROVIDER_NONE = 1;
    private static final int PROVIDER_PADDED_BOUNDS = 3;
    public static final int PUBLIC_STATUS_BAR_VISIBILITY_MASK = 16383;
    public static final int RECENT_APPS_VISIBLE = 16384;
    public static final Property<View, Float> ROTATION;
    public static final Property<View, Float> ROTATION_X;
    public static final Property<View, Float> ROTATION_Y;
    static final int SAVE_DISABLED = 65536;
    static final int SAVE_DISABLED_MASK = 65536;
    public static final Property<View, Float> SCALE_X;
    public static final Property<View, Float> SCALE_Y;
    public static final int SCREEN_STATE_OFF = 0;
    public static final int SCREEN_STATE_ON = 1;
    static final int SCROLLBARS_HORIZONTAL = 256;
    static final int SCROLLBARS_INSET_MASK = 16777216;
    public static final int SCROLLBARS_INSIDE_INSET = 16777216;
    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;
    static final int SCROLLBARS_MASK = 768;
    static final int SCROLLBARS_NONE = 0;
    public static final int SCROLLBARS_OUTSIDE_INSET = 50331648;
    static final int SCROLLBARS_OUTSIDE_MASK = 33554432;
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 33554432;
    static final int SCROLLBARS_STYLE_MASK = 50331648;
    static final int SCROLLBARS_VERTICAL = 512;
    public static final int SCROLLBAR_POSITION_DEFAULT = 0;
    public static final int SCROLLBAR_POSITION_LEFT = 1;
    public static final int SCROLLBAR_POSITION_RIGHT = 2;
    public static final int SCROLL_AXIS_HORIZONTAL = 1;
    public static final int SCROLL_AXIS_NONE = 0;
    public static final int SCROLL_AXIS_VERTICAL = 2;
    protected static final int[] SELECTED_STATE_SET;
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET;
    public static final int SOUND_EFFECTS_ENABLED = 134217728;
    public static final int STATUS_BAR_DISABLE_BACK = 4194304;
    public static final int STATUS_BAR_DISABLE_CLOCK = 8388608;
    public static final int STATUS_BAR_DISABLE_EXPAND = 65536;
    public static final int STATUS_BAR_DISABLE_HOME = 2097152;
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_ALERTS = 262144;
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_ICONS = 131072;
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_TICKER = 524288;
    public static final int STATUS_BAR_DISABLE_RECENT = 16777216;
    public static final int STATUS_BAR_DISABLE_SEARCH = 33554432;
    public static final int STATUS_BAR_DISABLE_SYSTEM_INFO = 1048576;
    public static final int STATUS_BAR_HIDDEN = 1;
    public static final int STATUS_BAR_TRANSIENT = 67108864;
    public static final int STATUS_BAR_TRANSLUCENT = 1073741824;
    public static final int STATUS_BAR_UNHIDE = 268435456;
    public static final int STATUS_BAR_VISIBLE = 0;
    public static final int SYSTEM_UI_CLEARABLE_FLAGS = 7;
    public static final int SYSTEM_UI_FLAG_FORCE_HIDE_NAVIGATION = 0;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 2048;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_LAYOUT_FLAGS = 1536;
    public static final int SYSTEM_UI_TRANSPARENT = 32768;
    public static final int TEXT_ALIGNMENT_CENTER = 4;
    private static final int TEXT_ALIGNMENT_DEFAULT = 1;
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;
    public static final int TEXT_ALIGNMENT_INHERIT = 0;
    static final int TEXT_ALIGNMENT_RESOLVED_DEFAULT = 1;
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;
    public static final int TEXT_DIRECTION_ANY_RTL = 2;
    private static final int TEXT_DIRECTION_DEFAULT = 0;
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;
    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_LOCALE = 5;
    public static final int TEXT_DIRECTION_LTR = 3;
    static final int TEXT_DIRECTION_RESOLVED_DEFAULT = 1;
    public static final int TEXT_DIRECTION_RTL = 4;
    public static final Property<View, Float> TRANSLATION_X;
    public static final Property<View, Float> TRANSLATION_Y;
    public static final Property<View, Float> TRANSLATION_Z;
    private static final int UNDEFINED_PADDING = Integer.MIN_VALUE;
    protected static final String VIEW_LOG_TAG = "View";
    static final int VIEW_STATE_ACCELERATED = 64;
    static final int VIEW_STATE_ACTIVATED = 32;
    static final int VIEW_STATE_DRAG_CAN_ACCEPT = 256;
    static final int VIEW_STATE_DRAG_HOVERED = 512;
    static final int VIEW_STATE_ENABLED = 8;
    static final int VIEW_STATE_FOCUSED = 4;
    static final int VIEW_STATE_HOVERED = 128;
    static final int VIEW_STATE_PRESSED = 16;
    static final int VIEW_STATE_SELECTED = 2;
    private static final int[][] VIEW_STATE_SETS;
    static final int VIEW_STATE_WINDOW_FOCUSED = 1;
    static final int VISIBILITY_MASK = 12;
    public static final int VISIBLE = 0;
    static final int WILL_NOT_CACHE_DRAWING = 131072;
    static final int WILL_NOT_DRAW = 128;
    protected static final int[] WINDOW_FOCUSED_STATE_SET;
    public static final Property<View, Float> X;
    public static final Property<View, Float> Y;
    public static final Property<View, Float> Z;
    private static SparseArray<String> mAttributeMap;
    private static int sNextAccessibilityViewId;
    private static final AtomicInteger sNextGeneratedId;
    static final ThreadLocal<Rect> sThreadLocal;
    private int mAccessibilityCursorPosition;
    AccessibilityDelegate mAccessibilityDelegate;
    private int mAccessibilityTraversalAfterId;
    private int mAccessibilityTraversalBeforeId;
    int mAccessibilityViewId;
    private ViewPropertyAnimator mAnimator;
    AttachInfo mAttachInfo;

    @ViewDebug.ExportedProperty(category = "attributes", hasAdjacentMapping = true)
    public String[] mAttributes;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "bg_")
    private Drawable mBackground;
    private RenderNode mBackgroundRenderNode;
    private int mBackgroundResource;
    private boolean mBackgroundSizeChanged;
    private TintInfo mBackgroundTint;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mBottom;
    public boolean mCachingFailed;
    Rect mClipBounds;
    private CharSequence mContentDescription;

    @ViewDebug.ExportedProperty(deepExport = true)
    protected Context mContext;
    protected Animation mCurrentAnimation;
    private int[] mDrawableState;
    private Bitmap mDrawingCache;
    private int mDrawingCacheBackgroundColor;
    private ViewTreeObserver mFloatingTreeObserver;
    GhostView mGhostView;
    private boolean mHasPerformedLongPress;

    @ViewDebug.ExportedProperty(resolveId = true)
    int mID;
    protected final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    private SparseArray<Object> mKeyedTags;
    private int mLabelForId;
    private boolean mLastIsOpaque;
    Paint mLayerPaint;

    @ViewDebug.ExportedProperty(category = "drawing", mapping = {@ViewDebug.IntToString(from = 0, to = "NONE"), @ViewDebug.IntToString(from = 1, to = "SOFTWARE"), @ViewDebug.IntToString(from = 2, to = "HARDWARE")})
    int mLayerType;
    private Insets mLayoutInsets;
    protected ViewGroup.LayoutParams mLayoutParams;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mLeft;
    private boolean mLeftPaddingDefined;
    ListenerInfo mListenerInfo;
    private MatchIdPredicate mMatchIdPredicate;
    private MatchLabelForPredicate mMatchLabelForPredicate;
    private LongSparseLongArray mMeasureCache;

    @ViewDebug.ExportedProperty(category = "measurement")
    int mMeasuredHeight;

    @ViewDebug.ExportedProperty(category = "measurement")
    int mMeasuredWidth;

    @ViewDebug.ExportedProperty(category = "measurement")
    private int mMinHeight;

    @ViewDebug.ExportedProperty(category = "measurement")
    private int mMinWidth;
    private ViewParent mNestedScrollingParent;
    private int mNextFocusDownId;
    int mNextFocusForwardId;
    private int mNextFocusLeftId;
    private int mNextFocusRightId;
    private int mNextFocusUpId;
    int mOldHeightMeasureSpec;
    int mOldWidthMeasureSpec;
    ViewOutlineProvider mOutlineProvider;
    private int mOverScrollMode;
    ViewOverlay mOverlay;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mPaddingBottom;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mPaddingLeft;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mPaddingRight;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mPaddingTop;
    protected ViewParent mParent;
    private CheckForLongPress mPendingCheckForLongPress;
    private CheckForTap mPendingCheckForTap;
    private PerformClick mPerformClick;

    @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 4096, mask = 4096, name = "FORCE_LAYOUT"), @ViewDebug.FlagToString(equals = 8192, mask = 8192, name = "LAYOUT_REQUIRED"), @ViewDebug.FlagToString(equals = 32768, mask = 32768, name = "DRAWING_CACHE_INVALID", outputIf = false), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "DRAWN", outputIf = true), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "NOT_DRAWN", outputIf = false), @ViewDebug.FlagToString(equals = 4194304, mask = 6291456, name = "DIRTY_OPAQUE"), @ViewDebug.FlagToString(equals = 2097152, mask = 6291456, name = "DIRTY")}, formatToHexString = true)
    int mPrivateFlags;
    int mPrivateFlags2;
    int mPrivateFlags3;
    boolean mRecreateDisplayList;
    final RenderNode mRenderNode;
    private final Resources mResources;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mRight;
    private boolean mRightPaddingDefined;
    private ScrollabilityCache mScrollCache;

    @ViewDebug.ExportedProperty(category = "scrolling")
    protected int mScrollX;

    @ViewDebug.ExportedProperty(category = "scrolling")
    protected int mScrollY;
    private SendViewScrolledAccessibilityEvent mSendViewScrolledAccessibilityEvent;
    SendViewStateChangedAccessibilityEvent mSendViewStateChangedAccessibilityEvent;
    private boolean mSendingHoverAccessibilityEvents;
    private StateListAnimator mStateListAnimator;

    @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 1, mask = 1, name = "SYSTEM_UI_FLAG_LOW_PROFILE", outputIf = true), @ViewDebug.FlagToString(equals = 2, mask = 2, name = "SYSTEM_UI_FLAG_HIDE_NAVIGATION", outputIf = true), @ViewDebug.FlagToString(equals = 0, mask = PUBLIC_STATUS_BAR_VISIBILITY_MASK, name = "SYSTEM_UI_FLAG_VISIBLE", outputIf = true)}, formatToHexString = true)
    int mSystemUiVisibility;
    protected Object mTag;
    private int[] mTempNestedScrollConsumed;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mTop;
    private TouchDelegate mTouchDelegate;
    private int mTouchSlop;
    TransformationInfo mTransformationInfo;
    int mTransientStateCount;
    private String mTransitionName;
    private Bitmap mUnscaledDrawingCache;
    private UnsetPressedState mUnsetPressedState;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mUserPaddingBottom;

    @ViewDebug.ExportedProperty(category = "padding")
    int mUserPaddingEnd;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mUserPaddingLeft;
    int mUserPaddingLeftInitial;

    @ViewDebug.ExportedProperty(category = "padding")
    protected int mUserPaddingRight;
    int mUserPaddingRightInitial;

    @ViewDebug.ExportedProperty(category = "padding")
    int mUserPaddingStart;
    private float mVerticalScrollFactor;
    private int mVerticalScrollbarPosition;

    @ViewDebug.ExportedProperty(formatToHexString = true)
    int mViewFlags;
    int mWindowAttachCount;
    public static boolean mDebugViewAttributes = false;
    private static boolean sCompatibilityDone = false;
    private static boolean sUseBrokenMakeMeasureSpec = false;
    private static boolean sIgnoreMeasureCache = false;
    private static final int[] VISIBILITY_FLAGS = {0, 4, 8};
    private static final int[] DRAWING_CACHE_QUALITY_FLAGS = {0, 524288, 1048576};
    static final int[] VIEW_STATE_IDS = {16842909, 1, 16842913, 2, 16842908, 4, 16842910, 8, 16842919, 16, 16843518, 32, 16843547, 64, 16843623, 128, 16843624, 256, 16843625, 512};

    public interface OnApplyWindowInsetsListener {
        WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets);
    }

    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);

        void onViewDetachedFromWindow(View view);
    }

    public interface OnClickListener {
        void onClick(View view);
    }

    public interface OnCreateContextMenuListener {
        void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo);
    }

    public interface OnDragListener {
        boolean onDrag(View view, DragEvent dragEvent);
    }

    public interface OnFocusChangeListener {
        void onFocusChange(View view, boolean z);
    }

    public interface OnGenericMotionListener {
        boolean onGenericMotion(View view, MotionEvent motionEvent);
    }

    public interface OnHoverListener {
        boolean onHover(View view, MotionEvent motionEvent);
    }

    public interface OnKeyListener {
        boolean onKey(View view, int i, KeyEvent keyEvent);
    }

    public interface OnLayoutChangeListener {
        void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);
    }

    public interface OnLongClickListener {
        boolean onLongClick(View view);
    }

    public interface OnScrollChangeListener {
        void onScrollChange(View view, int i, int i2, int i3, int i4);
    }

    public interface OnSystemUiVisibilityChangeListener {
        void onSystemUiVisibilityChange(int i);
    }

    public interface OnTouchListener {
        boolean onTouch(View view, MotionEvent motionEvent);
    }

    static {
        if (VIEW_STATE_IDS.length / 2 != R.styleable.ViewDrawableStates.length) {
            throw new IllegalStateException("VIEW_STATE_IDs array length does not match ViewDrawableStates style array");
        }
        int[] orderedIds = new int[VIEW_STATE_IDS.length];
        for (int i = 0; i < R.styleable.ViewDrawableStates.length; i++) {
            int viewState = R.styleable.ViewDrawableStates[i];
            for (int j = 0; j < VIEW_STATE_IDS.length; j += 2) {
                if (VIEW_STATE_IDS[j] == viewState) {
                    orderedIds[i * 2] = viewState;
                    orderedIds[(i * 2) + 1] = VIEW_STATE_IDS[j + 1];
                }
            }
        }
        int NUM_BITS = VIEW_STATE_IDS.length / 2;
        VIEW_STATE_SETS = new int[1 << NUM_BITS][];
        for (int i2 = 0; i2 < VIEW_STATE_SETS.length; i2++) {
            int numBits = Integer.bitCount(i2);
            int[] set = new int[numBits];
            int pos = 0;
            for (int j2 = 0; j2 < orderedIds.length; j2 += 2) {
                if ((orderedIds[j2 + 1] & i2) != 0) {
                    set[pos] = orderedIds[j2];
                    pos++;
                }
            }
            VIEW_STATE_SETS[i2] = set;
        }
        EMPTY_STATE_SET = VIEW_STATE_SETS[0];
        WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[1];
        SELECTED_STATE_SET = VIEW_STATE_SETS[2];
        SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[3];
        FOCUSED_STATE_SET = VIEW_STATE_SETS[4];
        FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[5];
        FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[6];
        FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[7];
        ENABLED_STATE_SET = VIEW_STATE_SETS[8];
        ENABLED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[9];
        ENABLED_SELECTED_STATE_SET = VIEW_STATE_SETS[10];
        ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[11];
        ENABLED_FOCUSED_STATE_SET = VIEW_STATE_SETS[12];
        ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[13];
        ENABLED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[14];
        ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[15];
        PRESSED_STATE_SET = VIEW_STATE_SETS[16];
        PRESSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[17];
        PRESSED_SELECTED_STATE_SET = VIEW_STATE_SETS[18];
        PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[19];
        PRESSED_FOCUSED_STATE_SET = VIEW_STATE_SETS[20];
        PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[21];
        PRESSED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[22];
        PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[23];
        PRESSED_ENABLED_STATE_SET = VIEW_STATE_SETS[24];
        PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[25];
        PRESSED_ENABLED_SELECTED_STATE_SET = VIEW_STATE_SETS[26];
        PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[27];
        PRESSED_ENABLED_FOCUSED_STATE_SET = VIEW_STATE_SETS[28];
        PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[29];
        PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[30];
        PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[31];
        sThreadLocal = new ThreadLocal<>();
        LAYOUT_DIRECTION_FLAGS = new int[]{0, 1, 2, 3};
        PFLAG2_TEXT_DIRECTION_FLAGS = new int[]{0, 64, 128, 192, 256, 320};
        PFLAG2_TEXT_ALIGNMENT_FLAGS = new int[]{0, 8192, 16384, AudioSystem.DEVICE_OUT_ALL_USB, 32768, 40960, 49152};
        sNextGeneratedId = new AtomicInteger(1);
        ALPHA = new FloatProperty<View>("alpha") {
            @Override
            public void setValue(View object, float value) {
                object.setAlpha(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getAlpha());
            }
        };
        TRANSLATION_X = new FloatProperty<View>("translationX") {
            @Override
            public void setValue(View object, float value) {
                object.setTranslationX(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getTranslationX());
            }
        };
        TRANSLATION_Y = new FloatProperty<View>("translationY") {
            @Override
            public void setValue(View object, float value) {
                object.setTranslationY(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getTranslationY());
            }
        };
        TRANSLATION_Z = new FloatProperty<View>("translationZ") {
            @Override
            public void setValue(View object, float value) {
                object.setTranslationZ(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getTranslationZ());
            }
        };
        X = new FloatProperty<View>("x") {
            @Override
            public void setValue(View object, float value) {
                object.setX(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getX());
            }
        };
        Y = new FloatProperty<View>("y") {
            @Override
            public void setValue(View object, float value) {
                object.setY(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getY());
            }
        };
        Z = new FloatProperty<View>("z") {
            @Override
            public void setValue(View object, float value) {
                object.setZ(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getZ());
            }
        };
        ROTATION = new FloatProperty<View>(PowerHintManager.HINT_ROTATION) {
            @Override
            public void setValue(View object, float value) {
                object.setRotation(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getRotation());
            }
        };
        ROTATION_X = new FloatProperty<View>("rotationX") {
            @Override
            public void setValue(View object, float value) {
                object.setRotationX(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getRotationX());
            }
        };
        ROTATION_Y = new FloatProperty<View>("rotationY") {
            @Override
            public void setValue(View object, float value) {
                object.setRotationY(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getRotationY());
            }
        };
        SCALE_X = new FloatProperty<View>("scaleX") {
            @Override
            public void setValue(View object, float value) {
                object.setScaleX(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getScaleX());
            }
        };
        SCALE_Y = new FloatProperty<View>("scaleY") {
            @Override
            public void setValue(View object, float value) {
                object.setScaleY(value);
            }

            @Override
            public Float get(View object) {
                return Float.valueOf(object.getScaleY());
            }
        };
    }

    static class TransformationInfo {
        private Matrix mInverseMatrix;
        private final Matrix mMatrix = new Matrix();

        @ViewDebug.ExportedProperty
        float mAlpha = 1.0f;
        float mTransitionAlpha = 1.0f;

        TransformationInfo() {
        }
    }

    private static class TintInfo {
        boolean mHasTintList;
        boolean mHasTintMode;
        ColorStateList mTintList;
        PorterDuff.Mode mTintMode;

        private TintInfo() {
        }
    }

    static class ListenerInfo {
        OnApplyWindowInsetsListener mOnApplyWindowInsetsListener;
        private CopyOnWriteArrayList<OnAttachStateChangeListener> mOnAttachStateChangeListeners;
        public OnClickListener mOnClickListener;
        protected OnCreateContextMenuListener mOnCreateContextMenuListener;
        private OnDragListener mOnDragListener;
        protected OnFocusChangeListener mOnFocusChangeListener;
        private OnGenericMotionListener mOnGenericMotionListener;
        private OnHoverListener mOnHoverListener;
        private OnKeyListener mOnKeyListener;
        private ArrayList<OnLayoutChangeListener> mOnLayoutChangeListeners;
        protected OnLongClickListener mOnLongClickListener;
        protected OnScrollChangeListener mOnScrollChangeListener;
        private OnSystemUiVisibilityChangeListener mOnSystemUiVisibilityChangeListener;
        private OnTouchListener mOnTouchListener;

        ListenerInfo() {
        }
    }

    public View(Context context) {
        this.mCurrentAnimation = null;
        this.mRecreateDisplayList = false;
        this.mID = -1;
        this.mAccessibilityViewId = -1;
        this.mAccessibilityCursorPosition = -1;
        this.mTag = null;
        this.mTransientStateCount = 0;
        this.mClipBounds = null;
        this.mPaddingLeft = 0;
        this.mPaddingRight = 0;
        this.mLabelForId = -1;
        this.mAccessibilityTraversalBeforeId = -1;
        this.mAccessibilityTraversalAfterId = -1;
        this.mLeftPaddingDefined = false;
        this.mRightPaddingDefined = false;
        this.mOldWidthMeasureSpec = Integer.MIN_VALUE;
        this.mOldHeightMeasureSpec = Integer.MIN_VALUE;
        this.mDrawableState = null;
        this.mOutlineProvider = ViewOutlineProvider.BACKGROUND;
        this.mNextFocusLeftId = -1;
        this.mNextFocusRightId = -1;
        this.mNextFocusUpId = -1;
        this.mNextFocusDownId = -1;
        this.mNextFocusForwardId = -1;
        this.mPendingCheckForTap = null;
        this.mTouchDelegate = null;
        this.mDrawingCacheBackgroundColor = 0;
        this.mAnimator = null;
        this.mLayerType = 0;
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        this.mContext = context;
        this.mResources = context != null ? context.getResources() : null;
        this.mViewFlags = 402653184;
        this.mPrivateFlags2 = 140296;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOverScrollMode(1);
        this.mUserPaddingStart = Integer.MIN_VALUE;
        this.mUserPaddingEnd = Integer.MIN_VALUE;
        this.mRenderNode = RenderNode.create(getClass().getName(), this);
        if (!sCompatibilityDone && context != null) {
            int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
            sUseBrokenMakeMeasureSpec = targetSdkVersion <= 17;
            sIgnoreMeasureCache = targetSdkVersion < 19;
            sCompatibilityDone = true;
        }
    }

    public View(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.View, defStyleAttr, defStyleRes);
        if (mDebugViewAttributes) {
            saveAttributeData(attrs, a);
        }
        Drawable background = null;
        int leftPadding = -1;
        int topPadding = -1;
        int rightPadding = -1;
        int bottomPadding = -1;
        int startPadding = Integer.MIN_VALUE;
        int endPadding = Integer.MIN_VALUE;
        int padding = -1;
        int viewFlagValues = 0;
        int viewFlagMasks = 0;
        boolean setScrollContainer = false;
        int x = 0;
        int y = 0;
        float tx = 0.0f;
        float ty = 0.0f;
        float tz = 0.0f;
        float elevation = 0.0f;
        float rotation = 0.0f;
        float rotationX = 0.0f;
        float rotationY = 0.0f;
        float sx = 1.0f;
        float sy = 1.0f;
        boolean transformSet = false;
        int scrollbarStyle = 0;
        int overScrollMode = this.mOverScrollMode;
        boolean initializeScrollbars = false;
        boolean startPaddingDefined = false;
        boolean endPaddingDefined = false;
        boolean leftPaddingDefined = false;
        boolean rightPaddingDefined = false;
        int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 8:
                    scrollbarStyle = a.getInt(attr, 0);
                    if (scrollbarStyle != 0) {
                        viewFlagValues |= 50331648 & scrollbarStyle;
                        viewFlagMasks |= 50331648;
                    } else {
                        continue;
                    }
                    break;
                case 9:
                    this.mID = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 10:
                    this.mTag = a.getText(attr);
                    continue;
                    break;
                case 11:
                    x = a.getDimensionPixelOffset(attr, 0);
                    continue;
                    break;
                case 12:
                    y = a.getDimensionPixelOffset(attr, 0);
                    continue;
                    break;
                case 13:
                    background = a.getDrawable(attr);
                    continue;
                    break;
                case 14:
                    padding = a.getDimensionPixelSize(attr, -1);
                    this.mUserPaddingLeftInitial = padding;
                    this.mUserPaddingRightInitial = padding;
                    leftPaddingDefined = true;
                    rightPaddingDefined = true;
                    continue;
                    break;
                case 15:
                    leftPadding = a.getDimensionPixelSize(attr, -1);
                    this.mUserPaddingLeftInitial = leftPadding;
                    leftPaddingDefined = true;
                    continue;
                    break;
                case 16:
                    topPadding = a.getDimensionPixelSize(attr, -1);
                    continue;
                    break;
                case 17:
                    rightPadding = a.getDimensionPixelSize(attr, -1);
                    this.mUserPaddingRightInitial = rightPadding;
                    rightPaddingDefined = true;
                    continue;
                    break;
                case 18:
                    bottomPadding = a.getDimensionPixelSize(attr, -1);
                    continue;
                    break;
                case 19:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 1;
                        viewFlagMasks |= 1;
                    } else {
                        continue;
                    }
                    break;
                case 20:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 262145;
                        viewFlagMasks |= 262145;
                    } else {
                        continue;
                    }
                    break;
                case 21:
                    int visibility = a.getInt(attr, 0);
                    if (visibility != 0) {
                        viewFlagValues |= VISIBILITY_FLAGS[visibility];
                        viewFlagMasks |= 12;
                    } else {
                        continue;
                    }
                    break;
                case 22:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 2;
                        viewFlagMasks |= 2;
                    } else {
                        continue;
                    }
                    break;
                case 23:
                    int scrollbars = a.getInt(attr, 0);
                    if (scrollbars != 0) {
                        viewFlagValues |= scrollbars;
                        viewFlagMasks |= 768;
                        initializeScrollbars = true;
                    } else {
                        continue;
                    }
                    break;
                case 24:
                    if (targetSdkVersion < 14) {
                    }
                    break;
                case 25:
                case 43:
                case 44:
                case 45:
                default:
                    break;
                case 26:
                    this.mNextFocusLeftId = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 27:
                    this.mNextFocusRightId = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 28:
                    this.mNextFocusUpId = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 29:
                    this.mNextFocusDownId = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 30:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 16384;
                        viewFlagMasks |= 16384;
                    } else {
                        continue;
                    }
                    break;
                case 31:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 2097152;
                        viewFlagMasks |= 2097152;
                    } else {
                        continue;
                    }
                    break;
                case 32:
                    if (a.getBoolean(attr, true)) {
                        continue;
                    } else {
                        viewFlagValues |= 65536;
                        viewFlagMasks |= 65536;
                    }
                    break;
                case 33:
                    int cacheQuality = a.getInt(attr, 0);
                    if (cacheQuality != 0) {
                        viewFlagValues |= DRAWING_CACHE_QUALITY_FLAGS[cacheQuality];
                        viewFlagMasks |= DRAWING_CACHE_QUALITY_MASK;
                    } else {
                        continue;
                    }
                    break;
                case 34:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 4194304;
                        viewFlagMasks |= 4194304;
                    } else {
                        continue;
                    }
                    break;
                case 35:
                    this.mMinWidth = a.getDimensionPixelSize(attr, 0);
                    continue;
                    break;
                case 36:
                    this.mMinHeight = a.getDimensionPixelSize(attr, 0);
                    continue;
                    break;
                case 37:
                    if (a.getBoolean(attr, true)) {
                        continue;
                    } else {
                        viewFlagValues &= -134217729;
                        viewFlagMasks |= 134217728;
                    }
                    break;
                case 38:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 67108864;
                        viewFlagMasks |= 67108864;
                    } else {
                        continue;
                    }
                    break;
                case 39:
                    setScrollContainer = true;
                    if (a.getBoolean(attr, false)) {
                        setScrollContainer(true);
                    } else {
                        continue;
                    }
                    break;
                case 40:
                    if (a.getBoolean(attr, true)) {
                        continue;
                    } else {
                        viewFlagValues &= -268435457;
                        viewFlagMasks |= 268435456;
                    }
                    break;
                case 41:
                    if (context.isRestricted()) {
                        throw new IllegalStateException("The android:onClick attribute cannot be used within a restricted context");
                    }
                    final String handlerName = a.getString(attr);
                    if (handlerName != null) {
                        setOnClickListener(new OnClickListener() {
                            private Method mHandler;

                            @Override
                            public void onClick(View v) {
                                if (this.mHandler == null) {
                                    try {
                                        this.mHandler = View.this.getContext().getClass().getMethod(handlerName, View.class);
                                    } catch (NoSuchMethodException e) {
                                        int id = View.this.getId();
                                        String idText = id == -1 ? ProxyInfo.LOCAL_EXCL_LIST : " with id '" + View.this.getContext().getResources().getResourceEntryName(id) + "'";
                                        throw new IllegalStateException("Could not find a method " + handlerName + "(View) in the activity " + View.this.getContext().getClass() + " for onClick handler on view " + View.this.getClass() + idText, e);
                                    }
                                }
                                try {
                                    this.mHandler.invoke(View.this.getContext(), View.this);
                                } catch (IllegalAccessException e2) {
                                    throw new IllegalStateException("Could not execute non public method of the activity", e2);
                                } catch (InvocationTargetException e3) {
                                    throw new IllegalStateException("Could not execute method of the activity", e3);
                                }
                            }
                        });
                    } else {
                        continue;
                    }
                    break;
                    break;
                case 42:
                    setContentDescription(a.getString(attr));
                    continue;
                    break;
                case 46:
                    overScrollMode = a.getInt(attr, 1);
                    continue;
                    break;
                case 47:
                    if (a.getBoolean(attr, false)) {
                        viewFlagValues |= 1024;
                        viewFlagMasks |= 1024;
                    } else {
                        continue;
                    }
                    break;
                case 48:
                    setAlpha(a.getFloat(attr, 1.0f));
                    continue;
                    break;
                case 49:
                    setPivotX(a.getDimensionPixelOffset(attr, 0));
                    continue;
                    break;
                case 50:
                    setPivotY(a.getDimensionPixelOffset(attr, 0));
                    continue;
                    break;
                case 51:
                    tx = a.getDimensionPixelOffset(attr, 0);
                    transformSet = true;
                    continue;
                    break;
                case 52:
                    ty = a.getDimensionPixelOffset(attr, 0);
                    transformSet = true;
                    continue;
                    break;
                case 53:
                    sx = a.getFloat(attr, 1.0f);
                    transformSet = true;
                    continue;
                    break;
                case 54:
                    sy = a.getFloat(attr, 1.0f);
                    transformSet = true;
                    continue;
                    break;
                case 55:
                    rotation = a.getFloat(attr, 0.0f);
                    transformSet = true;
                    continue;
                    break;
                case 56:
                    rotationX = a.getFloat(attr, 0.0f);
                    transformSet = true;
                    continue;
                    break;
                case 57:
                    rotationY = a.getFloat(attr, 0.0f);
                    transformSet = true;
                    continue;
                    break;
                case 58:
                    this.mVerticalScrollbarPosition = a.getInt(attr, 0);
                    continue;
                    break;
                case 59:
                    this.mNextFocusForwardId = a.getResourceId(attr, -1);
                    continue;
                    break;
                case 60:
                    setLayerType(a.getInt(attr, 0), null);
                    continue;
                    break;
                case 61:
                    break;
                case 62:
                    setImportantForAccessibility(a.getInt(attr, 0));
                    continue;
                    break;
                case 63:
                    this.mPrivateFlags2 &= -449;
                    int textDirection = a.getInt(attr, -1);
                    if (textDirection != -1) {
                        this.mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_FLAGS[textDirection];
                    } else {
                        continue;
                    }
                    break;
                case 64:
                    this.mPrivateFlags2 &= -57345;
                    int textAlignment = a.getInt(attr, 1);
                    this.mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_FLAGS[textAlignment];
                    continue;
                    break;
                case 65:
                    this.mPrivateFlags2 &= -61;
                    int layoutDirection = a.getInt(attr, -1);
                    int value = layoutDirection != -1 ? LAYOUT_DIRECTION_FLAGS[layoutDirection] : 2;
                    this.mPrivateFlags2 |= value << 2;
                    continue;
                    break;
                case 66:
                    startPadding = a.getDimensionPixelSize(attr, Integer.MIN_VALUE);
                    if (startPadding != Integer.MIN_VALUE) {
                        startPaddingDefined = true;
                        continue;
                    } else {
                        startPaddingDefined = false;
                    }
                    break;
                case 67:
                    endPadding = a.getDimensionPixelSize(attr, Integer.MIN_VALUE);
                    if (endPadding != Integer.MIN_VALUE) {
                        endPaddingDefined = true;
                        continue;
                    } else {
                        endPaddingDefined = false;
                    }
                    break;
                case 68:
                    setLabelFor(a.getResourceId(attr, -1));
                    continue;
                    break;
                case 69:
                    setAccessibilityLiveRegion(a.getInt(attr, 0));
                    continue;
                    break;
                case 70:
                    tz = a.getDimensionPixelOffset(attr, 0);
                    transformSet = true;
                    continue;
                    break;
                case 71:
                    setTransitionName(a.getString(attr));
                    continue;
                    break;
                case 72:
                    setNestedScrollingEnabled(a.getBoolean(attr, false));
                    continue;
                    break;
                case 73:
                    elevation = a.getDimensionPixelOffset(attr, 0);
                    transformSet = true;
                    continue;
                    break;
                case 74:
                    setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, a.getResourceId(attr, 0)));
                    continue;
                    break;
                case 75:
                    if (this.mBackgroundTint == null) {
                        this.mBackgroundTint = new TintInfo();
                    }
                    this.mBackgroundTint.mTintList = a.getColorStateList(75);
                    this.mBackgroundTint.mHasTintList = true;
                    continue;
                    break;
                case 76:
                    if (this.mBackgroundTint == null) {
                        this.mBackgroundTint = new TintInfo();
                    }
                    this.mBackgroundTint.mTintMode = Drawable.parseTintMode(a.getInt(76, -1), null);
                    this.mBackgroundTint.mHasTintMode = true;
                    continue;
                    break;
                case 77:
                    setOutlineProviderFromAttribute(a.getInt(77, 0));
                    continue;
                    break;
                case 78:
                    setAccessibilityTraversalBefore(a.getResourceId(attr, -1));
                    continue;
                    break;
                case 79:
                    setAccessibilityTraversalAfter(a.getResourceId(attr, -1));
                    continue;
                    break;
            }
            int fadingEdge = a.getInt(attr, 0);
            if (fadingEdge != 0) {
                viewFlagValues |= fadingEdge;
                viewFlagMasks |= 12288;
                initializeFadingEdgeInternal(a);
            }
        }
        setOverScrollMode(overScrollMode);
        this.mUserPaddingStart = startPadding;
        this.mUserPaddingEnd = endPadding;
        if (background != null) {
            setBackground(background);
        }
        this.mLeftPaddingDefined = leftPaddingDefined;
        this.mRightPaddingDefined = rightPaddingDefined;
        if (padding >= 0) {
            leftPadding = padding;
            topPadding = padding;
            rightPadding = padding;
            bottomPadding = padding;
            this.mUserPaddingLeftInitial = padding;
            this.mUserPaddingRightInitial = padding;
        }
        if (isRtlCompatibilityMode()) {
            if (!this.mLeftPaddingDefined && startPaddingDefined) {
                leftPadding = startPadding;
            }
            this.mUserPaddingLeftInitial = leftPadding >= 0 ? leftPadding : this.mUserPaddingLeftInitial;
            if (!this.mRightPaddingDefined && endPaddingDefined) {
                rightPadding = endPadding;
            }
            this.mUserPaddingRightInitial = rightPadding >= 0 ? rightPadding : this.mUserPaddingRightInitial;
        } else {
            boolean hasRelativePadding = startPaddingDefined || endPaddingDefined;
            if (this.mLeftPaddingDefined && !hasRelativePadding) {
                this.mUserPaddingLeftInitial = leftPadding;
            }
            if (this.mRightPaddingDefined && !hasRelativePadding) {
                this.mUserPaddingRightInitial = rightPadding;
            }
        }
        internalSetPadding(this.mUserPaddingLeftInitial, topPadding < 0 ? this.mPaddingTop : topPadding, this.mUserPaddingRightInitial, bottomPadding < 0 ? this.mPaddingBottom : bottomPadding);
        if (viewFlagMasks != 0) {
            setFlags(viewFlagValues, viewFlagMasks);
        }
        if (initializeScrollbars) {
            initializeScrollbarsInternal(a);
        }
        a.recycle();
        if (scrollbarStyle != 0) {
            recomputePadding();
        }
        if (x != 0 || y != 0) {
            scrollTo(x, y);
        }
        if (transformSet) {
            setTranslationX(tx);
            setTranslationY(ty);
            setTranslationZ(tz);
            setElevation(elevation);
            setRotation(rotation);
            setRotationX(rotationX);
            setRotationY(rotationY);
            setScaleX(sx);
            setScaleY(sy);
        }
        if (!setScrollContainer && (viewFlagValues & 512) != 0) {
            setScrollContainer(true);
        }
        computeOpaqueFlags();
    }

    View() {
        this.mCurrentAnimation = null;
        this.mRecreateDisplayList = false;
        this.mID = -1;
        this.mAccessibilityViewId = -1;
        this.mAccessibilityCursorPosition = -1;
        this.mTag = null;
        this.mTransientStateCount = 0;
        this.mClipBounds = null;
        this.mPaddingLeft = 0;
        this.mPaddingRight = 0;
        this.mLabelForId = -1;
        this.mAccessibilityTraversalBeforeId = -1;
        this.mAccessibilityTraversalAfterId = -1;
        this.mLeftPaddingDefined = false;
        this.mRightPaddingDefined = false;
        this.mOldWidthMeasureSpec = Integer.MIN_VALUE;
        this.mOldHeightMeasureSpec = Integer.MIN_VALUE;
        this.mDrawableState = null;
        this.mOutlineProvider = ViewOutlineProvider.BACKGROUND;
        this.mNextFocusLeftId = -1;
        this.mNextFocusRightId = -1;
        this.mNextFocusUpId = -1;
        this.mNextFocusDownId = -1;
        this.mNextFocusForwardId = -1;
        this.mPendingCheckForTap = null;
        this.mTouchDelegate = null;
        this.mDrawingCacheBackgroundColor = 0;
        this.mAnimator = null;
        this.mLayerType = 0;
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        this.mResources = null;
        this.mRenderNode = RenderNode.create(getClass().getName(), this);
    }

    private static SparseArray<String> getAttributeMap() {
        if (mAttributeMap == null) {
            mAttributeMap = new SparseArray<>();
        }
        return mAttributeMap;
    }

    private void saveAttributeData(AttributeSet attrs, TypedArray a) {
        int length = ((attrs != null ? attrs.getAttributeCount() : 0) + a.getIndexCount()) * 2;
        this.mAttributes = new String[length];
        int i = 0;
        if (attrs != null) {
            i = 0;
            while (i < attrs.getAttributeCount()) {
                this.mAttributes[i] = attrs.getAttributeName(i);
                this.mAttributes[i + 1] = attrs.getAttributeValue(i);
                i += 2;
            }
        }
        SparseArray<String> attributeMap = getAttributeMap();
        for (int j = 0; j < a.length(); j++) {
            if (a.hasValue(j)) {
                try {
                    int resourceId = a.getResourceId(j, 0);
                    if (resourceId != 0) {
                        String resourceName = attributeMap.get(resourceId);
                        if (resourceName == null) {
                            resourceName = a.getResources().getResourceName(resourceId);
                            attributeMap.put(resourceId, resourceName);
                        }
                        this.mAttributes[i] = resourceName;
                        this.mAttributes[i + 1] = a.getText(j).toString();
                        i += 2;
                    }
                } catch (Resources.NotFoundException e) {
                }
            }
        }
    }

    public String toString() {
        String pkgname;
        StringBuilder out = new StringBuilder(128);
        out.append(getClass().getName());
        out.append('{');
        out.append(Integer.toHexString(System.identityHashCode(this)));
        out.append(' ');
        switch (this.mViewFlags & 12) {
            case 0:
                out.append('V');
                break;
            case 4:
                out.append('I');
                break;
            case 8:
                out.append('G');
                break;
            default:
                out.append('.');
                break;
        }
        out.append((this.mViewFlags & 1) == 1 ? 'F' : '.');
        out.append((this.mViewFlags & 32) == 0 ? DateFormat.DAY : '.');
        out.append((this.mViewFlags & 128) == 128 ? '.' : 'D');
        out.append((this.mViewFlags & 256) != 0 ? 'H' : '.');
        out.append((this.mViewFlags & 512) != 0 ? 'V' : '.');
        out.append((this.mViewFlags & 16384) != 0 ? 'C' : '.');
        out.append((this.mViewFlags & 2097152) != 0 ? DateFormat.STANDALONE_MONTH : '.');
        out.append(' ');
        out.append((this.mPrivateFlags & 8) != 0 ? 'R' : '.');
        out.append((this.mPrivateFlags & 2) == 0 ? '.' : 'F');
        out.append((this.mPrivateFlags & 4) != 0 ? 'S' : '.');
        if ((this.mPrivateFlags & 33554432) != 0) {
            out.append('p');
        } else {
            out.append((this.mPrivateFlags & 16384) != 0 ? 'P' : '.');
        }
        out.append((this.mPrivateFlags & 268435456) != 0 ? 'H' : '.');
        out.append((this.mPrivateFlags & 1073741824) != 0 ? DateFormat.CAPITAL_AM_PM : '.');
        out.append((this.mPrivateFlags & Integer.MIN_VALUE) != 0 ? 'I' : '.');
        out.append((this.mPrivateFlags & 6291456) == 0 ? '.' : 'D');
        out.append(' ');
        out.append(this.mLeft);
        out.append(',');
        out.append(this.mTop);
        out.append('-');
        out.append(this.mRight);
        out.append(',');
        out.append(this.mBottom);
        int id = getId();
        if (id != -1) {
            out.append(" #");
            out.append(Integer.toHexString(id));
            Resources r = this.mResources;
            if (Resources.resourceHasPackage(id) && r != null) {
                switch ((-16777216) & id) {
                    case 16777216:
                        pkgname = ZenModeConfig.SYSTEM_AUTHORITY;
                        String typename = r.getResourceTypeName(id);
                        String entryname = r.getResourceEntryName(id);
                        out.append(" ");
                        out.append(pkgname);
                        out.append(":");
                        out.append(typename);
                        out.append("/");
                        out.append(entryname);
                        break;
                    case 2130706432:
                        pkgname = "app";
                        String typename2 = r.getResourceTypeName(id);
                        String entryname2 = r.getResourceEntryName(id);
                        out.append(" ");
                        out.append(pkgname);
                        out.append(":");
                        out.append(typename2);
                        out.append("/");
                        out.append(entryname2);
                        break;
                    default:
                        try {
                            pkgname = r.getResourcePackageName(id);
                            String typename22 = r.getResourceTypeName(id);
                            String entryname22 = r.getResourceEntryName(id);
                            out.append(" ");
                            out.append(pkgname);
                            out.append(":");
                            out.append(typename22);
                            out.append("/");
                            out.append(entryname22);
                        } catch (Resources.NotFoundException e) {
                        }
                        break;
                }
            }
        }
        out.append("}");
        return out.toString();
    }

    protected void initializeFadingEdge(TypedArray a) {
        TypedArray arr = this.mContext.obtainStyledAttributes(R.styleable.View);
        initializeFadingEdgeInternal(arr);
        arr.recycle();
    }

    protected void initializeFadingEdgeInternal(TypedArray a) {
        initScrollCache();
        this.mScrollCache.fadingEdgeLength = a.getDimensionPixelSize(25, ViewConfiguration.get(this.mContext).getScaledFadingEdgeLength());
    }

    public int getVerticalFadingEdgeLength() {
        ScrollabilityCache cache;
        if (!isVerticalFadingEdgeEnabled() || (cache = this.mScrollCache) == null) {
            return 0;
        }
        return cache.fadingEdgeLength;
    }

    public void setFadingEdgeLength(int length) {
        initScrollCache();
        this.mScrollCache.fadingEdgeLength = length;
    }

    public int getHorizontalFadingEdgeLength() {
        ScrollabilityCache cache;
        if (!isHorizontalFadingEdgeEnabled() || (cache = this.mScrollCache) == null) {
            return 0;
        }
        return cache.fadingEdgeLength;
    }

    public int getVerticalScrollbarWidth() {
        ScrollBarDrawable scrollBar;
        ScrollabilityCache cache = this.mScrollCache;
        if (cache == null || (scrollBar = cache.scrollBar) == null) {
            return 0;
        }
        int size = scrollBar.getSize(true);
        if (size <= 0) {
            return cache.scrollBarSize;
        }
        return size;
    }

    protected int getHorizontalScrollbarHeight() {
        ScrollBarDrawable scrollBar;
        ScrollabilityCache cache = this.mScrollCache;
        if (cache == null || (scrollBar = cache.scrollBar) == null) {
            return 0;
        }
        int size = scrollBar.getSize(false);
        if (size <= 0) {
            return cache.scrollBarSize;
        }
        return size;
    }

    protected void initializeScrollbars(TypedArray a) {
        TypedArray arr = this.mContext.obtainStyledAttributes(R.styleable.View);
        initializeScrollbarsInternal(arr);
        arr.recycle();
    }

    protected void initializeScrollbarsInternal(TypedArray a) {
        initScrollCache();
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache.scrollBar == null) {
            scrollabilityCache.scrollBar = new ScrollBarDrawable();
        }
        boolean fadeScrollbars = a.getBoolean(45, true);
        if (!fadeScrollbars) {
            scrollabilityCache.state = 1;
        }
        scrollabilityCache.fadeScrollBars = fadeScrollbars;
        scrollabilityCache.scrollBarFadeDuration = a.getInt(43, ViewConfiguration.getScrollBarFadeDuration());
        scrollabilityCache.scrollBarDefaultDelayBeforeFade = a.getInt(44, ViewConfiguration.getScrollDefaultDelay());
        scrollabilityCache.scrollBarSize = a.getDimensionPixelSize(1, ViewConfiguration.get(this.mContext).getScaledScrollBarSize());
        scrollabilityCache.scrollBar.setHorizontalTrackDrawable(a.getDrawable(4));
        Drawable thumb = a.getDrawable(2);
        if (thumb != null) {
            scrollabilityCache.scrollBar.setHorizontalThumbDrawable(thumb);
        }
        boolean alwaysDraw = a.getBoolean(6, false);
        if (alwaysDraw) {
            scrollabilityCache.scrollBar.setAlwaysDrawHorizontalTrack(true);
        }
        Drawable track = a.getDrawable(5);
        scrollabilityCache.scrollBar.setVerticalTrackDrawable(track);
        Drawable thumb2 = a.getDrawable(3);
        if (thumb2 != null) {
            scrollabilityCache.scrollBar.setVerticalThumbDrawable(thumb2);
        }
        boolean alwaysDraw2 = a.getBoolean(7, false);
        if (alwaysDraw2) {
            scrollabilityCache.scrollBar.setAlwaysDrawVerticalTrack(true);
        }
        int layoutDirection = getLayoutDirection();
        if (track != null) {
            track.setLayoutDirection(layoutDirection);
        }
        if (thumb2 != null) {
            thumb2.setLayoutDirection(layoutDirection);
        }
        resolvePadding();
    }

    private void initScrollCache() {
        if (this.mScrollCache == null) {
            this.mScrollCache = new ScrollabilityCache(ViewConfiguration.get(this.mContext), this);
        }
    }

    private ScrollabilityCache getScrollCache() {
        initScrollCache();
        return this.mScrollCache;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (this.mVerticalScrollbarPosition != position) {
            this.mVerticalScrollbarPosition = position;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    public int getVerticalScrollbarPosition() {
        return this.mVerticalScrollbarPosition;
    }

    ListenerInfo getListenerInfo() {
        if (this.mListenerInfo != null) {
            return this.mListenerInfo;
        }
        this.mListenerInfo = new ListenerInfo();
        return this.mListenerInfo;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        getListenerInfo().mOnScrollChangeListener = l;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        getListenerInfo().mOnFocusChangeListener = l;
    }

    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnLayoutChangeListeners == null) {
            li.mOnLayoutChangeListeners = new ArrayList();
        }
        if (!li.mOnLayoutChangeListeners.contains(listener)) {
            li.mOnLayoutChangeListeners.add(listener);
        }
    }

    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnLayoutChangeListeners != null) {
            li.mOnLayoutChangeListeners.remove(listener);
        }
    }

    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnAttachStateChangeListeners == null) {
            li.mOnAttachStateChangeListeners = new CopyOnWriteArrayList();
        }
        li.mOnAttachStateChangeListeners.add(listener);
    }

    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnAttachStateChangeListeners != null) {
            li.mOnAttachStateChangeListeners.remove(listener);
        }
    }

    public OnFocusChangeListener getOnFocusChangeListener() {
        ListenerInfo li = this.mListenerInfo;
        if (li != null) {
            return li.mOnFocusChangeListener;
        }
        return null;
    }

    public void setOnClickListener(OnClickListener l) {
        if (!isClickable()) {
            setClickable(true);
        }
        getListenerInfo().mOnClickListener = l;
    }

    public boolean hasOnClickListeners() {
        ListenerInfo li = this.mListenerInfo;
        return (li == null || li.mOnClickListener == null) ? false : true;
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnLongClickListener = l;
    }

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnCreateContextMenuListener = l;
    }

    public boolean performClick() {
        boolean result;
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            playSoundEffect(0);
            li.mOnClickListener.onClick(this);
            result = true;
        } else {
            result = false;
        }
        sendAccessibilityEvent(1);
        return result;
    }

    public boolean callOnClick() {
        ListenerInfo li = this.mListenerInfo;
        if (li == null || li.mOnClickListener == null) {
            return false;
        }
        li.mOnClickListener.onClick(this);
        return true;
    }

    public boolean performLongClick() {
        sendAccessibilityEvent(2);
        boolean handled = false;
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnLongClickListener != null) {
            handled = li.mOnLongClickListener.onLongClick(this);
        }
        if (!handled) {
            handled = showContextMenu();
        }
        if (handled) {
            performHapticFeedback(0);
        }
        return handled;
    }

    protected boolean performButtonActionOnTouchDown(MotionEvent event) {
        return (event.getButtonState() & 2) != 0 && showContextMenu(event.getX(), event.getY(), event.getMetaState());
    }

    public boolean showContextMenu() {
        return getParent().showContextMenuForChild(this);
    }

    public boolean showContextMenu(float x, float y, int metaState) {
        return showContextMenu();
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        ViewParent parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.startActionModeForChild(this, callback);
    }

    public void setOnKeyListener(OnKeyListener l) {
        getListenerInfo().mOnKeyListener = l;
    }

    public void setOnTouchListener(OnTouchListener l) {
        getListenerInfo().mOnTouchListener = l;
    }

    public void setOnGenericMotionListener(OnGenericMotionListener l) {
        getListenerInfo().mOnGenericMotionListener = l;
    }

    public void setOnHoverListener(OnHoverListener l) {
        getListenerInfo().mOnHoverListener = l;
    }

    public void setOnDragListener(OnDragListener l) {
        getListenerInfo().mOnDragListener = l;
    }

    void handleFocusGainInternal(int direction, Rect previouslyFocusedRect) {
        if ((this.mPrivateFlags & 2) == 0) {
            this.mPrivateFlags |= 2;
            View oldFocus = this.mAttachInfo != null ? getRootView().findFocus() : null;
            if (this.mParent != null) {
                this.mParent.requestChildFocus(this, this);
            }
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, this);
            }
            onFocusChanged(true, direction, previouslyFocusedRect);
            refreshDrawableState();
        }
    }

    public void getHotspotBounds(Rect outRect) {
        Drawable background = getBackground();
        if (background != null) {
            background.getHotspotBounds(outRect);
        } else {
            getBoundsOnScreen(outRect);
        }
    }

    public boolean requestRectangleOnScreen(Rect rectangle) {
        return requestRectangleOnScreen(rectangle, false);
    }

    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        if (this.mParent == null) {
            return false;
        }
        View child = this;
        RectF position = this.mAttachInfo != null ? this.mAttachInfo.mTmpTransformRect : new RectF();
        position.set(rectangle);
        ViewParent parent = this.mParent;
        boolean scrolled = false;
        while (parent != null) {
            rectangle.set((int) position.left, (int) position.top, (int) position.right, (int) position.bottom);
            scrolled |= parent.requestChildRectangleOnScreen(child, rectangle, immediate);
            if (!child.hasIdentityMatrix()) {
                child.getMatrix().mapRect(position);
            }
            position.offset(child.mLeft, child.mTop);
            if (parent instanceof View) {
                View parentView = (View) parent;
                position.offset(-parentView.getScrollX(), -parentView.getScrollY());
                child = parentView;
                parent = child.getParent();
            } else {
                return scrolled;
            }
        }
        return scrolled;
    }

    public void clearFocus() {
        clearFocusInternal(null, true, true);
    }

    void clearFocusInternal(View focused, boolean propagate, boolean refocus) {
        if ((this.mPrivateFlags & 2) != 0) {
            this.mPrivateFlags &= -3;
            if (propagate && this.mParent != null) {
                this.mParent.clearChildFocus(this);
            }
            onFocusChanged(false, 0, null);
            refreshDrawableState();
            if (propagate) {
                if (!refocus || !rootViewRequestFocus()) {
                    notifyGlobalFocusCleared(this);
                }
            }
        }
    }

    void notifyGlobalFocusCleared(View oldFocus) {
        if (oldFocus != null && this.mAttachInfo != null) {
            this.mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, null);
        }
    }

    boolean rootViewRequestFocus() {
        View root = getRootView();
        return root != null && root.requestFocus();
    }

    void unFocus(View focused) {
        clearFocusInternal(focused, false, false);
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public boolean hasFocus() {
        return (this.mPrivateFlags & 2) != 0;
    }

    public boolean hasFocusable() {
        if (!isFocusableInTouchMode()) {
            for (ViewParent p = this.mParent; p instanceof ViewGroup; p = p.getParent()) {
                ViewGroup g = (ViewGroup) p;
                if (g.shouldBlockFocusForTouchscreen()) {
                    return false;
                }
            }
        }
        return (this.mViewFlags & 12) == 0 && isFocusable();
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            sendAccessibilityEvent(8);
        } else {
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
        InputMethodManager imm = InputMethodManager.peekInstance();
        if (!gainFocus) {
            if (isPressed()) {
                setPressed(false);
            }
            if (imm != null && this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus) {
                imm.focusOut(this);
            }
            onFocusLost();
        } else if (imm != null && this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus) {
            imm.focusIn(this);
        }
        invalidate(true);
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnFocusChangeListener != null) {
            li.mOnFocusChangeListener.onFocusChange(this, gainFocus);
        }
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mKeyDispatchState.reset(this);
        }
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.sendAccessibilityEvent(this, eventType);
        } else {
            sendAccessibilityEventInternal(eventType);
        }
    }

    public void announceForAccessibility(CharSequence text) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled() && this.mParent != null) {
            AccessibilityEvent event = AccessibilityEvent.obtain(16384);
            onInitializeAccessibilityEvent(event);
            event.getText().add(text);
            event.setContentDescription(null);
            this.mParent.requestSendAccessibilityEvent(this, event);
        }
    }

    void sendAccessibilityEventInternal(int eventType) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(eventType));
        }
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.sendAccessibilityEventUnchecked(this, event);
        } else {
            sendAccessibilityEventUncheckedInternal(event);
        }
    }

    void sendAccessibilityEventUncheckedInternal(AccessibilityEvent event) {
        if (isShown()) {
            onInitializeAccessibilityEvent(event);
            if ((event.getEventType() & POPULATING_ACCESSIBILITY_EVENT_TYPES) != 0) {
                dispatchPopulateAccessibilityEvent(event);
            }
            getParent().requestSendAccessibilityEvent(this, event);
        }
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return this.mAccessibilityDelegate != null ? this.mAccessibilityDelegate.dispatchPopulateAccessibilityEvent(this, event) : dispatchPopulateAccessibilityEventInternal(event);
    }

    boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return false;
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onPopulateAccessibilityEvent(this, event);
        } else {
            onPopulateAccessibilityEventInternal(event);
        }
    }

    void onPopulateAccessibilityEventInternal(AccessibilityEvent event) {
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onInitializeAccessibilityEvent(this, event);
        } else {
            onInitializeAccessibilityEventInternal(event);
        }
    }

    void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        event.setSource(this);
        event.setClassName(View.class.getName());
        event.setPackageName(getContext().getPackageName());
        event.setEnabled(isEnabled());
        event.setContentDescription(this.mContentDescription);
        switch (event.getEventType()) {
            case 8:
                ArrayList<View> focusablesTempList = this.mAttachInfo != null ? this.mAttachInfo.mTempArrayList : new ArrayList<>();
                getRootView().addFocusables(focusablesTempList, 2, 0);
                event.setItemCount(focusablesTempList.size());
                event.setCurrentItemIndex(focusablesTempList.indexOf(this));
                if (this.mAttachInfo != null) {
                    focusablesTempList.clear();
                }
                break;
            case 8192:
                CharSequence text = getIterableTextForAccessibility();
                if (text != null && text.length() > 0) {
                    event.setFromIndex(getAccessibilitySelectionStart());
                    event.setToIndex(getAccessibilitySelectionEnd());
                    event.setItemCount(text.length());
                    break;
                }
                break;
        }
    }

    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        return this.mAccessibilityDelegate != null ? this.mAccessibilityDelegate.createAccessibilityNodeInfo(this) : createAccessibilityNodeInfoInternal();
    }

    AccessibilityNodeInfo createAccessibilityNodeInfoInternal() {
        AccessibilityNodeProvider provider = getAccessibilityNodeProvider();
        if (provider != null) {
            return provider.createAccessibilityNodeInfo(-1);
        }
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(this);
        onInitializeAccessibilityNodeInfo(info);
        return info;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(this, info);
        } else {
            onInitializeAccessibilityNodeInfoInternal(info);
        }
    }

    public void getBoundsOnScreen(Rect outRect) {
        getBoundsOnScreen(outRect, false);
    }

    public void getBoundsOnScreen(Rect outRect, boolean clipToParent) {
        if (this.mAttachInfo != null) {
            RectF position = this.mAttachInfo.mTmpTransformRect;
            position.set(0.0f, 0.0f, this.mRight - this.mLeft, this.mBottom - this.mTop);
            if (!hasIdentityMatrix()) {
                getMatrix().mapRect(position);
            }
            position.offset(this.mLeft, this.mTop);
            ViewParent parent = this.mParent;
            while (parent instanceof View) {
                View parentView = (View) parent;
                position.offset(-parentView.mScrollX, -parentView.mScrollY);
                if (clipToParent) {
                    position.left = Math.max(position.left, 0.0f);
                    position.top = Math.max(position.top, 0.0f);
                    position.right = Math.min(position.right, parentView.getWidth());
                    position.bottom = Math.min(position.bottom, parentView.getHeight());
                }
                if (!parentView.hasIdentityMatrix()) {
                    parentView.getMatrix().mapRect(position);
                }
                position.offset(parentView.mLeft, parentView.mTop);
                parent = parentView.mParent;
            }
            if (parent instanceof ViewRootImpl) {
                ViewRootImpl viewRootImpl = (ViewRootImpl) parent;
                position.offset(0.0f, -viewRootImpl.mCurScrollY);
            }
            position.offset(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
            outRect.set((int) (position.left + 0.5f), (int) (position.top + 0.5f), (int) (position.right + 0.5f), (int) (position.bottom + 0.5f));
        }
    }

    void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        Rect bounds = this.mAttachInfo.mTmpInvalRect;
        getDrawingRect(bounds);
        info.setBoundsInParent(bounds);
        getBoundsOnScreen(bounds, true);
        info.setBoundsInScreen(bounds);
        Object parentForAccessibility = getParentForAccessibility();
        if (parentForAccessibility instanceof View) {
            info.setParent((View) parentForAccessibility);
        }
        if (this.mID != -1) {
            View rootView = getRootView();
            if (rootView == null) {
                rootView = this;
            }
            View label = rootView.findLabelForView(this, this.mID);
            if (label != null) {
                info.setLabeledBy(label);
            }
            if ((this.mAttachInfo.mAccessibilityFetchFlags & 16) != 0 && Resources.resourceHasPackage(this.mID)) {
                try {
                    String viewId = getResources().getResourceName(this.mID);
                    info.setViewIdResourceName(viewId);
                } catch (Resources.NotFoundException e) {
                }
            }
        }
        if (this.mLabelForId != -1) {
            View rootView2 = getRootView();
            if (rootView2 == null) {
                rootView2 = this;
            }
            View labeled = rootView2.findViewInsideOutShouldExist(this, this.mLabelForId);
            if (labeled != null) {
                info.setLabelFor(labeled);
            }
        }
        if (this.mAccessibilityTraversalBeforeId != -1) {
            View rootView3 = getRootView();
            if (rootView3 == null) {
                rootView3 = this;
            }
            View next = rootView3.findViewInsideOutShouldExist(this, this.mAccessibilityTraversalBeforeId);
            if (next != null) {
                info.setTraversalBefore(next);
            }
        }
        if (this.mAccessibilityTraversalAfterId != -1) {
            View rootView4 = getRootView();
            if (rootView4 == null) {
                rootView4 = this;
            }
            View next2 = rootView4.findViewInsideOutShouldExist(this, this.mAccessibilityTraversalAfterId);
            if (next2 != null) {
                info.setTraversalAfter(next2);
            }
        }
        info.setVisibleToUser(isVisibleToUser());
        info.setPackageName(this.mContext.getPackageName());
        info.setClassName(View.class.getName());
        info.setContentDescription(getContentDescription());
        info.setEnabled(isEnabled());
        info.setClickable(isClickable());
        info.setFocusable(isFocusable());
        info.setFocused(isFocused());
        info.setAccessibilityFocused(isAccessibilityFocused());
        info.setSelected(isSelected());
        info.setLongClickable(isLongClickable());
        info.setLiveRegion(getAccessibilityLiveRegion());
        info.addAction(4);
        info.addAction(8);
        if (isFocusable()) {
            if (isFocused()) {
                info.addAction(2);
            } else {
                info.addAction(1);
            }
        }
        if (!isAccessibilityFocused()) {
            info.addAction(64);
        } else {
            info.addAction(128);
        }
        if (isClickable() && isEnabled()) {
            info.addAction(16);
        }
        if (isLongClickable() && isEnabled()) {
            info.addAction(32);
        }
        CharSequence text = getIterableTextForAccessibility();
        if (text != null && text.length() > 0) {
            info.setTextSelection(getAccessibilitySelectionStart(), getAccessibilitySelectionEnd());
            info.addAction(131072);
            info.addAction(256);
            info.addAction(512);
            info.setMovementGranularities(11);
        }
    }

    private View findLabelForView(View view, int labeledId) {
        if (this.mMatchLabelForPredicate == null) {
            this.mMatchLabelForPredicate = new MatchLabelForPredicate();
        }
        this.mMatchLabelForPredicate.mLabeledId = labeledId;
        return findViewByPredicateInsideOut(view, this.mMatchLabelForPredicate);
    }

    protected boolean isVisibleToUser() {
        return isVisibleToUser(null);
    }

    protected boolean isVisibleToUser(Rect boundInView) {
        if (this.mAttachInfo == null || this.mAttachInfo.mWindowVisibility != 0) {
            return false;
        }
        Object obj = this;
        while (obj instanceof View) {
            View view = (View) obj;
            if (view.getAlpha() <= 0.0f || view.getTransitionAlpha() <= 0.0f || view.getVisibility() != 0) {
                return false;
            }
            obj = view.mParent;
        }
        Rect visibleRect = this.mAttachInfo.mTmpInvalRect;
        Point offset = this.mAttachInfo.mPoint;
        if (!getGlobalVisibleRect(visibleRect, offset)) {
            return false;
        }
        if (boundInView != null) {
            visibleRect.offset(-offset.x, -offset.y);
            return boundInView.intersect(visibleRect);
        }
        return true;
    }

    public AccessibilityDelegate getAccessibilityDelegate() {
        return this.mAccessibilityDelegate;
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        this.mAccessibilityDelegate = delegate;
    }

    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (this.mAccessibilityDelegate != null) {
            return this.mAccessibilityDelegate.getAccessibilityNodeProvider(this);
        }
        return null;
    }

    public int getAccessibilityViewId() {
        if (this.mAccessibilityViewId == -1) {
            int i = sNextAccessibilityViewId;
            sNextAccessibilityViewId = i + 1;
            this.mAccessibilityViewId = i;
        }
        return this.mAccessibilityViewId;
    }

    public int getAccessibilityWindowId() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mAccessibilityWindowId;
        }
        return Integer.MAX_VALUE;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE)
    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    @RemotableViewMethod
    public void setContentDescription(CharSequence contentDescription) {
        if (this.mContentDescription == null) {
            if (contentDescription == null) {
                return;
            }
        } else if (this.mContentDescription.equals(contentDescription)) {
            return;
        }
        this.mContentDescription = contentDescription;
        boolean nonEmptyDesc = contentDescription != null && contentDescription.length() > 0;
        if (nonEmptyDesc && getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
            notifySubtreeAccessibilityStateChangedIfNeeded();
        } else {
            notifyViewAccessibilityStateChangedIfNeeded(4);
        }
    }

    @RemotableViewMethod
    public void setAccessibilityTraversalBefore(int beforeId) {
        if (this.mAccessibilityTraversalBeforeId != beforeId) {
            this.mAccessibilityTraversalBeforeId = beforeId;
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    public int getAccessibilityTraversalBefore() {
        return this.mAccessibilityTraversalBeforeId;
    }

    @RemotableViewMethod
    public void setAccessibilityTraversalAfter(int afterId) {
        if (this.mAccessibilityTraversalAfterId != afterId) {
            this.mAccessibilityTraversalAfterId = afterId;
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    public int getAccessibilityTraversalAfter() {
        return this.mAccessibilityTraversalAfterId;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE)
    public int getLabelFor() {
        return this.mLabelForId;
    }

    @RemotableViewMethod
    public void setLabelFor(int id) {
        if (this.mLabelForId != id) {
            this.mLabelForId = id;
            if (this.mLabelForId != -1 && this.mID == -1) {
                this.mID = generateViewId();
            }
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    protected void onFocusLost() {
        resetPressedState();
    }

    private void resetPressedState() {
        if ((this.mViewFlags & 32) != 32 && isPressed()) {
            setPressed(false);
            if (!this.mHasPerformedLongPress) {
                removeLongPressCallback();
            }
        }
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public boolean isFocused() {
        return (this.mPrivateFlags & 2) != 0;
    }

    public View findFocus() {
        if ((this.mPrivateFlags & 2) != 0) {
            return this;
        }
        return null;
    }

    public boolean isScrollContainer() {
        return (this.mPrivateFlags & 1048576) != 0;
    }

    public void setScrollContainer(boolean isScrollContainer) {
        if (isScrollContainer) {
            if (this.mAttachInfo != null && (this.mPrivateFlags & 1048576) == 0) {
                this.mAttachInfo.mScrollContainers.add(this);
                this.mPrivateFlags |= 1048576;
            }
            this.mPrivateFlags |= 524288;
            return;
        }
        if ((this.mPrivateFlags & 1048576) != 0) {
            this.mAttachInfo.mScrollContainers.remove(this);
        }
        this.mPrivateFlags &= -1572865;
    }

    public int getDrawingCacheQuality() {
        return this.mViewFlags & DRAWING_CACHE_QUALITY_MASK;
    }

    public void setDrawingCacheQuality(int quality) {
        setFlags(quality, DRAWING_CACHE_QUALITY_MASK);
    }

    public boolean getKeepScreenOn() {
        return (this.mViewFlags & 67108864) != 0;
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        setFlags(keepScreenOn ? 67108864 : 0, 67108864);
    }

    public int getNextFocusLeftId() {
        return this.mNextFocusLeftId;
    }

    public void setNextFocusLeftId(int nextFocusLeftId) {
        this.mNextFocusLeftId = nextFocusLeftId;
    }

    public int getNextFocusRightId() {
        return this.mNextFocusRightId;
    }

    public void setNextFocusRightId(int nextFocusRightId) {
        this.mNextFocusRightId = nextFocusRightId;
    }

    public int getNextFocusUpId() {
        return this.mNextFocusUpId;
    }

    public void setNextFocusUpId(int nextFocusUpId) {
        this.mNextFocusUpId = nextFocusUpId;
    }

    public int getNextFocusDownId() {
        return this.mNextFocusDownId;
    }

    public void setNextFocusDownId(int nextFocusDownId) {
        this.mNextFocusDownId = nextFocusDownId;
    }

    public int getNextFocusForwardId() {
        return this.mNextFocusForwardId;
    }

    public void setNextFocusForwardId(int nextFocusForwardId) {
        this.mNextFocusForwardId = nextFocusForwardId;
    }

    public boolean isShown() {
        Object obj;
        View current = this;
        while ((current.mViewFlags & 12) == 0 && (obj = current.mParent) != null) {
            if (!(obj instanceof View)) {
                return true;
            }
            current = (View) obj;
            if (current == null) {
                return false;
            }
        }
        return false;
    }

    protected boolean fitSystemWindows(Rect insets) {
        if ((this.mPrivateFlags3 & 32) == 0) {
            if (insets == null) {
                return false;
            }
            try {
                this.mPrivateFlags3 |= 64;
                return dispatchApplyWindowInsets(new WindowInsets(insets)).isConsumed();
            } finally {
                this.mPrivateFlags3 &= -65;
            }
        }
        return fitSystemWindowsInt(insets);
    }

    private boolean fitSystemWindowsInt(Rect insets) {
        if ((this.mViewFlags & 2) != 2) {
            return false;
        }
        this.mUserPaddingStart = Integer.MIN_VALUE;
        this.mUserPaddingEnd = Integer.MIN_VALUE;
        Rect localInsets = sThreadLocal.get();
        if (localInsets == null) {
            localInsets = new Rect();
            sThreadLocal.set(localInsets);
        }
        boolean res = computeFitSystemWindows(insets, localInsets);
        this.mUserPaddingLeftInitial = localInsets.left;
        this.mUserPaddingRightInitial = localInsets.right;
        internalSetPadding(localInsets.left, localInsets.top, localInsets.right, localInsets.bottom);
        return res;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if ((this.mPrivateFlags3 & 64) == 0) {
            if (fitSystemWindows(insets.getSystemWindowInsets())) {
                return insets.consumeSystemWindowInsets();
            }
            return insets;
        }
        if (fitSystemWindowsInt(insets.getSystemWindowInsets())) {
            return insets.consumeSystemWindowInsets();
        }
        return insets;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener) {
        getListenerInfo().mOnApplyWindowInsetsListener = listener;
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        WindowInsets windowInsetsOnApplyWindowInsets;
        try {
            this.mPrivateFlags3 |= 32;
            if (this.mListenerInfo != null && this.mListenerInfo.mOnApplyWindowInsetsListener != null) {
                windowInsetsOnApplyWindowInsets = this.mListenerInfo.mOnApplyWindowInsetsListener.onApplyWindowInsets(this, insets);
            } else {
                windowInsetsOnApplyWindowInsets = onApplyWindowInsets(insets);
            }
            return windowInsetsOnApplyWindowInsets;
        } finally {
            this.mPrivateFlags3 &= -33;
        }
    }

    protected boolean computeFitSystemWindows(Rect inoutInsets, Rect outLocalInsets) {
        if ((this.mViewFlags & 2048) == 0 || this.mAttachInfo == null || ((this.mAttachInfo.mSystemUiVisibility & 1536) == 0 && !this.mAttachInfo.mOverscanRequested)) {
            outLocalInsets.set(inoutInsets);
            inoutInsets.set(0, 0, 0, 0);
            return true;
        }
        Rect overscan = this.mAttachInfo.mOverscanInsets;
        outLocalInsets.set(overscan);
        inoutInsets.left -= overscan.left;
        inoutInsets.top -= overscan.top;
        inoutInsets.right -= overscan.right;
        inoutInsets.bottom -= overscan.bottom;
        return false;
    }

    public WindowInsets computeSystemWindowInsets(WindowInsets in, Rect outLocalInsets) {
        if ((this.mViewFlags & 2048) == 0 || this.mAttachInfo == null || (this.mAttachInfo.mSystemUiVisibility & 1536) == 0) {
            outLocalInsets.set(in.getSystemWindowInsets());
            return in.consumeSystemWindowInsets();
        }
        outLocalInsets.set(0, 0, 0, 0);
        return in;
    }

    public void setFitsSystemWindows(boolean fitSystemWindows) {
        setFlags(fitSystemWindows ? 2 : 0, 2);
    }

    @ViewDebug.ExportedProperty
    public boolean getFitsSystemWindows() {
        return (this.mViewFlags & 2) == 2;
    }

    public boolean fitsSystemWindows() {
        return getFitsSystemWindows();
    }

    public void requestFitSystemWindows() {
        if (this.mParent != null) {
            this.mParent.requestFitSystemWindows();
        }
    }

    public void requestApplyInsets() {
        requestFitSystemWindows();
    }

    public void makeOptionalFitsSystemWindows() {
        setFlags(2048, 2048);
    }

    @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = 0, to = "VISIBLE"), @ViewDebug.IntToString(from = 4, to = "INVISIBLE"), @ViewDebug.IntToString(from = 8, to = "GONE")})
    public int getVisibility() {
        return this.mViewFlags & 12;
    }

    @RemotableViewMethod
    public void setVisibility(int visibility) {
        setFlags(visibility, 12);
        if (this.mBackground != null) {
            this.mBackground.setVisible(visibility == 0, false);
        }
    }

    @ViewDebug.ExportedProperty
    public boolean isEnabled() {
        return (this.mViewFlags & 32) == 0;
    }

    @RemotableViewMethod
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            setFlags(enabled ? 0 : 32, 32);
            refreshDrawableState();
            invalidate(true);
            if (!enabled) {
                cancelPendingInputEvents();
            }
        }
    }

    public void setFocusable(boolean focusable) {
        if (!focusable) {
            setFlags(0, 262144);
        }
        setFlags(focusable ? 1 : 0, 1);
    }

    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        setFlags(focusableInTouchMode ? 262144 : 0, 262144);
        if (focusableInTouchMode) {
            setFlags(1, 1);
        }
    }

    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        setFlags(soundEffectsEnabled ? 134217728 : 0, 134217728);
    }

    @ViewDebug.ExportedProperty
    public boolean isSoundEffectsEnabled() {
        return 134217728 == (this.mViewFlags & 134217728);
    }

    public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
        setFlags(hapticFeedbackEnabled ? 268435456 : 0, 268435456);
    }

    @ViewDebug.ExportedProperty
    public boolean isHapticFeedbackEnabled() {
        return 268435456 == (this.mViewFlags & 268435456);
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = 0, to = "LTR"), @ViewDebug.IntToString(from = 1, to = "RTL"), @ViewDebug.IntToString(from = 2, to = "INHERIT"), @ViewDebug.IntToString(from = 3, to = "LOCALE")})
    public int getRawLayoutDirection() {
        return (this.mPrivateFlags2 & 12) >> 2;
    }

    @RemotableViewMethod
    public void setLayoutDirection(int layoutDirection) {
        if (getRawLayoutDirection() != layoutDirection) {
            this.mPrivateFlags2 &= -13;
            resetRtlProperties();
            this.mPrivateFlags2 |= (layoutDirection << 2) & 12;
            resolveRtlPropertiesIfNeeded();
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = 0, to = "RESOLVED_DIRECTION_LTR"), @ViewDebug.IntToString(from = 1, to = "RESOLVED_DIRECTION_RTL")})
    public int getLayoutDirection() {
        int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
        if (targetSdkVersion >= 17) {
            return (this.mPrivateFlags2 & 16) == 16 ? 1 : 0;
        }
        this.mPrivateFlags2 |= 32;
        return 0;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public boolean hasTransientState() {
        return (this.mPrivateFlags2 & Integer.MIN_VALUE) == Integer.MIN_VALUE;
    }

    public void setHasTransientState(boolean hasTransientState) {
        this.mTransientStateCount = hasTransientState ? this.mTransientStateCount + 1 : this.mTransientStateCount - 1;
        if (this.mTransientStateCount < 0) {
            this.mTransientStateCount = 0;
            Log.e(VIEW_LOG_TAG, "hasTransientState decremented below 0: unmatched pair of setHasTransientState calls");
            return;
        }
        if ((hasTransientState && this.mTransientStateCount == 1) || (!hasTransientState && this.mTransientStateCount == 0)) {
            this.mPrivateFlags2 = (hasTransientState ? Integer.MIN_VALUE : 0) | (Integer.MAX_VALUE & this.mPrivateFlags2);
            if (this.mParent != null) {
                try {
                    this.mParent.childHasTransientStateChanged(this, hasTransientState);
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                }
            }
        }
    }

    public boolean isAttachedToWindow() {
        return this.mAttachInfo != null;
    }

    public boolean isLaidOut() {
        return (this.mPrivateFlags3 & 4) == 4;
    }

    public void setWillNotDraw(boolean willNotDraw) {
        setFlags(willNotDraw ? 128 : 0, 128);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean willNotDraw() {
        return (this.mViewFlags & 128) == 128;
    }

    public void setWillNotCacheDrawing(boolean willNotCacheDrawing) {
        setFlags(willNotCacheDrawing ? 131072 : 0, 131072);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean willNotCacheDrawing() {
        return (this.mViewFlags & 131072) == 131072;
    }

    @ViewDebug.ExportedProperty
    public boolean isClickable() {
        return (this.mViewFlags & 16384) == 16384;
    }

    public void setClickable(boolean clickable) {
        setFlags(clickable ? 16384 : 0, 16384);
    }

    public boolean isLongClickable() {
        return (this.mViewFlags & 2097152) == 2097152;
    }

    public void setLongClickable(boolean longClickable) {
        setFlags(longClickable ? 2097152 : 0, 2097152);
    }

    private void setPressed(boolean pressed, float x, float y) {
        if (pressed) {
            drawableHotspotChanged(x, y);
        }
        setPressed(pressed);
    }

    public void setPressed(boolean pressed) {
        boolean needsRefresh = pressed != ((this.mPrivateFlags & 16384) == 16384);
        if (pressed) {
            this.mPrivateFlags |= 16384;
        } else {
            this.mPrivateFlags &= -16385;
        }
        if (needsRefresh) {
            refreshDrawableState();
        }
        dispatchSetPressed(pressed);
    }

    protected void dispatchSetPressed(boolean pressed) {
    }

    @ViewDebug.ExportedProperty
    public boolean isPressed() {
        return (this.mPrivateFlags & 16384) == 16384;
    }

    public boolean isSaveEnabled() {
        return (this.mViewFlags & 65536) != 65536;
    }

    public void setSaveEnabled(boolean enabled) {
        setFlags(enabled ? 0 : 65536, 65536);
    }

    @ViewDebug.ExportedProperty
    public boolean getFilterTouchesWhenObscured() {
        return (this.mViewFlags & 1024) != 0;
    }

    public void setFilterTouchesWhenObscured(boolean enabled) {
        setFlags(enabled ? 1024 : 0, 1024);
    }

    public boolean isSaveFromParentEnabled() {
        return (this.mViewFlags & 536870912) != 536870912;
    }

    public void setSaveFromParentEnabled(boolean enabled) {
        setFlags(enabled ? 0 : 536870912, 536870912);
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean isFocusable() {
        return 1 == (this.mViewFlags & 1);
    }

    @ViewDebug.ExportedProperty
    public final boolean isFocusableInTouchMode() {
        return 262144 == (this.mViewFlags & 262144);
    }

    public View focusSearch(int direction) {
        if (this.mParent != null) {
            return this.mParent.focusSearch(this, direction);
        }
        return null;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return false;
    }

    View findUserSetNextFocus(View root, int direction) {
        switch (direction) {
            case 1:
                if (this.mID != -1) {
                    final int id = this.mID;
                }
                break;
            case 2:
                if (this.mNextFocusForwardId != -1) {
                }
                break;
            case 17:
                if (this.mNextFocusLeftId != -1) {
                }
                break;
            case 33:
                if (this.mNextFocusUpId != -1) {
                }
                break;
            case 66:
                if (this.mNextFocusRightId != -1) {
                }
                break;
            case 130:
                if (this.mNextFocusDownId != -1) {
                }
                break;
        }
        return null;
    }

    private View findViewInsideOutShouldExist(View root, int id) {
        if (this.mMatchIdPredicate == null) {
            this.mMatchIdPredicate = new MatchIdPredicate();
        }
        this.mMatchIdPredicate.mId = id;
        View result = root.findViewByPredicateInsideOut(this, this.mMatchIdPredicate);
        if (result == null) {
            Log.w(VIEW_LOG_TAG, "couldn't find view with id " + id);
        }
        return result;
    }

    public ArrayList<View> getFocusables(int direction) {
        ArrayList<View> result = new ArrayList<>(24);
        addFocusables(result, direction);
        return result;
    }

    public void addFocusables(ArrayList<View> views, int direction) {
        addFocusables(views, direction, 1);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (views != null && isFocusable()) {
            if ((focusableMode & 1) != 1 || !isInTouchMode() || isFocusableInTouchMode()) {
                views.add(this);
            }
        }
    }

    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
        if (getAccessibilityNodeProvider() != null) {
            if ((flags & 4) != 0) {
                outViews.add(this);
            }
        } else if ((flags & 2) != 0 && searched != null && searched.length() > 0 && this.mContentDescription != null && this.mContentDescription.length() > 0) {
            String searchedLowerCase = searched.toString().toLowerCase();
            String contentDescriptionLowerCase = this.mContentDescription.toString().toLowerCase();
            if (contentDescriptionLowerCase.contains(searchedLowerCase)) {
                outViews.add(this);
            }
        }
    }

    public ArrayList<View> getTouchables() {
        ArrayList<View> result = new ArrayList<>();
        addTouchables(result);
        return result;
    }

    public void addTouchables(ArrayList<View> views) {
        int viewFlags = this.mViewFlags;
        if (((viewFlags & 16384) == 16384 || (viewFlags & 2097152) == 2097152) && (viewFlags & 32) == 0) {
            views.add(this);
        }
    }

    public boolean isAccessibilityFocused() {
        return (this.mPrivateFlags2 & 67108864) != 0;
    }

    public boolean requestAccessibilityFocus() {
        AccessibilityManager manager = AccessibilityManager.getInstance(this.mContext);
        if (!manager.isEnabled() || !manager.isTouchExplorationEnabled() || (this.mViewFlags & 12) != 0 || (this.mPrivateFlags2 & 67108864) != 0) {
            return false;
        }
        this.mPrivateFlags2 |= 67108864;
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.setAccessibilityFocus(this, null);
        }
        invalidate();
        sendAccessibilityEvent(32768);
        return true;
    }

    public void clearAccessibilityFocus() {
        View focusHost;
        clearAccessibilityFocusNoCallbacks();
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null && (focusHost = viewRootImpl.getAccessibilityFocusedHost()) != null && ViewRootImpl.isViewDescendantOf(focusHost, this)) {
            viewRootImpl.setAccessibilityFocus(null, null);
        }
    }

    private void sendAccessibilityHoverEvent(int eventType) {
        View source = this;
        while (!source.includeForAccessibility()) {
            Object parent = source.getParent();
            if (parent instanceof View) {
                source = (View) parent;
            } else {
                return;
            }
        }
        source.sendAccessibilityEvent(eventType);
    }

    void clearAccessibilityFocusNoCallbacks() {
        if ((this.mPrivateFlags2 & 67108864) != 0) {
            this.mPrivateFlags2 &= -67108865;
            invalidate();
            sendAccessibilityEvent(65536);
        }
    }

    public final boolean requestFocus() {
        return requestFocus(130);
    }

    public final boolean requestFocus(int direction) {
        return requestFocus(direction, null);
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return requestFocusNoSearch(direction, previouslyFocusedRect);
    }

    private boolean requestFocusNoSearch(int direction, Rect previouslyFocusedRect) {
        if ((this.mViewFlags & 1) != 1 || (this.mViewFlags & 12) != 0) {
            return false;
        }
        if ((isInTouchMode() && 262144 != (this.mViewFlags & 262144)) || hasAncestorThatBlocksDescendantFocus()) {
            return false;
        }
        handleFocusGainInternal(direction, previouslyFocusedRect);
        return true;
    }

    public final boolean requestFocusFromTouch() {
        ViewRootImpl viewRoot;
        if (isInTouchMode() && (viewRoot = getViewRootImpl()) != null) {
            viewRoot.ensureTouchMode(false);
        }
        return requestFocus(130);
    }

    private boolean hasAncestorThatBlocksDescendantFocus() {
        boolean focusableInTouchMode = isFocusableInTouchMode();
        ViewParent ancestor = this.mParent;
        while (ancestor instanceof ViewGroup) {
            ViewGroup vgAncestor = (ViewGroup) ancestor;
            if (vgAncestor.getDescendantFocusability() == 393216 || (!focusableInTouchMode && vgAncestor.shouldBlockFocusForTouchscreen())) {
                return true;
            }
            ancestor = vgAncestor.getParent();
        }
        return false;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE, mapping = {@ViewDebug.IntToString(from = 0, to = "auto"), @ViewDebug.IntToString(from = 1, to = "yes"), @ViewDebug.IntToString(from = 2, to = "no"), @ViewDebug.IntToString(from = 4, to = "noHideDescendants")})
    public int getImportantForAccessibility() {
        return (this.mPrivateFlags2 & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK) >> 20;
    }

    public void setAccessibilityLiveRegion(int mode) {
        if (mode != getAccessibilityLiveRegion()) {
            this.mPrivateFlags2 &= -25165825;
            this.mPrivateFlags2 |= (mode << 23) & 25165824;
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    public int getAccessibilityLiveRegion() {
        return (this.mPrivateFlags2 & 25165824) >> 23;
    }

    public void setImportantForAccessibility(int mode) {
        int oldMode = getImportantForAccessibility();
        if (mode != oldMode) {
            boolean maySkipNotify = oldMode == 0 || mode == 0;
            boolean oldIncludeForAccessibility = maySkipNotify && includeForAccessibility();
            this.mPrivateFlags2 &= -7340033;
            this.mPrivateFlags2 |= (mode << 20) & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK;
            if (!maySkipNotify || oldIncludeForAccessibility != includeForAccessibility()) {
                notifySubtreeAccessibilityStateChangedIfNeeded();
            } else {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    public boolean isImportantForAccessibility() {
        int mode = (this.mPrivateFlags2 & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK) >> 20;
        if (mode == 2 || mode == 4) {
            return false;
        }
        for (ViewParent parent = this.mParent; parent instanceof View; parent = parent.getParent()) {
            if (((View) parent).getImportantForAccessibility() == 4) {
                return false;
            }
        }
        return mode == 1 || isActionableForAccessibility() || hasListenersForAccessibility() || getAccessibilityNodeProvider() != null || getAccessibilityLiveRegion() != 0;
    }

    public ViewParent getParentForAccessibility() {
        if (this.mParent instanceof View) {
            View parentView = (View) this.mParent;
            if (parentView.includeForAccessibility()) {
                return this.mParent;
            }
            return this.mParent.getParentForAccessibility();
        }
        return null;
    }

    public void addChildrenForAccessibility(ArrayList<View> children) {
    }

    public boolean includeForAccessibility() {
        if (this.mAttachInfo != null) {
            return (this.mAttachInfo.mAccessibilityFetchFlags & 8) != 0 || isImportantForAccessibility();
        }
        return false;
    }

    public boolean isActionableForAccessibility() {
        return isClickable() || isLongClickable() || isFocusable();
    }

    private boolean hasListenersForAccessibility() {
        ListenerInfo info = getListenerInfo();
        return (this.mTouchDelegate == null && info.mOnKeyListener == null && info.mOnTouchListener == null && info.mOnGenericMotionListener == null && info.mOnHoverListener == null && info.mOnDragListener == null) ? false : true;
    }

    public void notifyViewAccessibilityStateChangedIfNeeded(int changeType) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mSendViewStateChangedAccessibilityEvent == null) {
                this.mSendViewStateChangedAccessibilityEvent = new SendViewStateChangedAccessibilityEvent();
            }
            this.mSendViewStateChangedAccessibilityEvent.runOrPost(changeType);
        }
    }

    public void notifySubtreeAccessibilityStateChangedIfNeeded() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled() && (this.mPrivateFlags2 & 134217728) == 0) {
            this.mPrivateFlags2 |= 134217728;
            if (this.mParent != null) {
                try {
                    this.mParent.notifySubtreeAccessibilityStateChanged(this, this, 1);
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                }
            }
        }
    }

    void resetSubtreeAccessibilityStateChanged() {
        this.mPrivateFlags2 &= -134217729;
    }

    public boolean dispatchNestedPrePerformAccessibilityAction(int action, Bundle arguments) {
        for (ViewParent p = getParent(); p != null; p = p.getParent()) {
            if (p.onNestedPrePerformAccessibilityAction(this, action, arguments)) {
                return true;
            }
        }
        return false;
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        return this.mAccessibilityDelegate != null ? this.mAccessibilityDelegate.performAccessibilityAction(this, action, arguments) : performAccessibilityActionInternal(action, arguments);
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (isNestedScrollingEnabled() && ((action == 8192 || action == 4096) && dispatchNestedPrePerformAccessibilityAction(action, arguments))) {
            return true;
        }
        switch (action) {
            case 1:
                if (hasFocus()) {
                    return false;
                }
                getViewRootImpl().ensureTouchMode(false);
                return requestFocus();
            case 2:
                if (!hasFocus()) {
                    return false;
                }
                clearFocus();
                return !isFocused();
            case 4:
                if (isSelected()) {
                    return false;
                }
                setSelected(true);
                return isSelected();
            case 8:
                if (!isSelected()) {
                    return false;
                }
                setSelected(false);
                return !isSelected();
            case 16:
                if (!isClickable()) {
                    return false;
                }
                performClick();
                return true;
            case 32:
                if (!isLongClickable()) {
                    return false;
                }
                performLongClick();
                return true;
            case 64:
                if (isAccessibilityFocused()) {
                    return false;
                }
                return requestAccessibilityFocus();
            case 128:
                if (!isAccessibilityFocused()) {
                    return false;
                }
                clearAccessibilityFocus();
                return true;
            case 256:
                if (arguments == null) {
                    return false;
                }
                int granularity = arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT);
                boolean extendSelection = arguments.getBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN);
                return traverseAtGranularity(granularity, true, extendSelection);
            case 512:
                if (arguments == null) {
                    return false;
                }
                int granularity2 = arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT);
                boolean extendSelection2 = arguments.getBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN);
                return traverseAtGranularity(granularity2, false, extendSelection2);
            case 131072:
                CharSequence text = getIterableTextForAccessibility();
                if (text == null) {
                    return false;
                }
                int start = arguments != null ? arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1) : -1;
                int end = arguments != null ? arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1) : -1;
                if ((getAccessibilitySelectionStart() == start && getAccessibilitySelectionEnd() == end) || start != end) {
                    return false;
                }
                setAccessibilitySelection(start, end);
                notifyViewAccessibilityStateChangedIfNeeded(0);
                return true;
            default:
                return false;
        }
    }

    private boolean traverseAtGranularity(int granularity, boolean forward, boolean extendSelection) {
        AccessibilityIterators.TextSegmentIterator iterator;
        int selectionEnd;
        int selectionStart;
        CharSequence text = getIterableTextForAccessibility();
        if (text == null || text.length() == 0 || (iterator = getIteratorForGranularity(granularity)) == null) {
            return false;
        }
        int current = getAccessibilitySelectionEnd();
        if (current == -1) {
            current = forward ? 0 : text.length();
        }
        int[] range = forward ? iterator.following(current) : iterator.preceding(current);
        if (range == null) {
            return false;
        }
        int segmentStart = range[0];
        int segmentEnd = range[1];
        if (extendSelection && isAccessibilitySelectionExtendable()) {
            selectionStart = getAccessibilitySelectionStart();
            if (selectionStart == -1) {
                selectionStart = forward ? segmentStart : segmentEnd;
            }
            selectionEnd = forward ? segmentEnd : segmentStart;
        } else {
            selectionEnd = forward ? segmentEnd : segmentStart;
            selectionStart = selectionEnd;
        }
        setAccessibilitySelection(selectionStart, selectionEnd);
        int action = forward ? 256 : 512;
        sendViewTextTraversedAtGranularityEvent(action, granularity, segmentStart, segmentEnd);
        return true;
    }

    public CharSequence getIterableTextForAccessibility() {
        return getContentDescription();
    }

    public boolean isAccessibilitySelectionExtendable() {
        return false;
    }

    public int getAccessibilitySelectionStart() {
        return this.mAccessibilityCursorPosition;
    }

    public int getAccessibilitySelectionEnd() {
        return getAccessibilitySelectionStart();
    }

    public void setAccessibilitySelection(int start, int end) {
        if (start != end || end != this.mAccessibilityCursorPosition) {
            if (start >= 0 && start == end && end <= getIterableTextForAccessibility().length()) {
                this.mAccessibilityCursorPosition = start;
            } else {
                this.mAccessibilityCursorPosition = -1;
            }
            sendAccessibilityEvent(8192);
        }
    }

    private void sendViewTextTraversedAtGranularityEvent(int action, int granularity, int fromIndex, int toIndex) {
        if (this.mParent != null) {
            AccessibilityEvent event = AccessibilityEvent.obtain(131072);
            onInitializeAccessibilityEvent(event);
            onPopulateAccessibilityEvent(event);
            event.setFromIndex(fromIndex);
            event.setToIndex(toIndex);
            event.setAction(action);
            event.setMovementGranularity(granularity);
            this.mParent.requestSendAccessibilityEvent(this, event);
        }
    }

    public AccessibilityIterators.TextSegmentIterator getIteratorForGranularity(int granularity) {
        switch (granularity) {
            case 1:
                CharSequence text = getIterableTextForAccessibility();
                if (text != null && text.length() > 0) {
                    AccessibilityIterators.CharacterTextSegmentIterator iterator = AccessibilityIterators.CharacterTextSegmentIterator.getInstance(this.mContext.getResources().getConfiguration().locale);
                    iterator.initialize(text.toString());
                    return iterator;
                }
                return null;
            case 2:
                CharSequence text2 = getIterableTextForAccessibility();
                if (text2 != null && text2.length() > 0) {
                    AccessibilityIterators.WordTextSegmentIterator iterator2 = AccessibilityIterators.WordTextSegmentIterator.getInstance(this.mContext.getResources().getConfiguration().locale);
                    iterator2.initialize(text2.toString());
                    return iterator2;
                }
                return null;
            case 8:
                CharSequence text3 = getIterableTextForAccessibility();
                if (text3 != null && text3.length() > 0) {
                    AccessibilityIterators.ParagraphTextSegmentIterator iterator3 = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance();
                    iterator3.initialize(text3.toString());
                    return iterator3;
                }
                return null;
            default:
                return null;
        }
    }

    public void dispatchStartTemporaryDetach() {
        onStartTemporaryDetach();
    }

    public void onStartTemporaryDetach() {
        removeUnsetPressCallback();
        this.mPrivateFlags |= 67108864;
    }

    public void dispatchFinishTemporaryDetach() {
        onFinishTemporaryDetach();
    }

    public void onFinishTemporaryDetach() {
    }

    public KeyEvent.DispatcherState getKeyDispatcherState() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mKeyDispatchState;
        }
        return null;
    }

    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        return onKeyPreIme(event.getKeyCode(), event);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onKeyEvent(event, 0);
        }
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnKeyListener != null && (this.mViewFlags & 32) == 0 && li.mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }
        if (event.dispatch(this, this.mAttachInfo != null ? this.mAttachInfo.mKeyDispatchState : null, this)) {
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(event, 0);
        }
        return false;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return onKeyShortcut(event.getKeyCode(), event);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.isTargetAccessibilityFocus()) {
            if (!isAccessibilityFocusedViewOrHost()) {
                return false;
            }
            event.setTargetAccessibilityFocus(false);
        }
        boolean result = false;
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(event, 0);
        }
        int actionMasked = event.getActionMasked();
        if (actionMasked == 0) {
            stopNestedScroll();
        }
        if (onFilterTouchEventForSecurity(event)) {
            ListenerInfo li = this.mListenerInfo;
            if (li != null && li.mOnTouchListener != null && (this.mViewFlags & 32) == 0 && li.mOnTouchListener.onTouch(this, event)) {
                result = true;
            }
            if (!result && onTouchEvent(event)) {
                result = true;
            }
        }
        if (!result && this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(event, 0);
        }
        if (actionMasked == 1 || actionMasked == 3 || (actionMasked == 0 && !result)) {
            stopNestedScroll();
            return result;
        }
        return result;
    }

    boolean isAccessibilityFocusedViewOrHost() {
        return isAccessibilityFocused() || (getViewRootImpl() != null && getViewRootImpl().getAccessibilityFocusedHost() == this);
    }

    public boolean onFilterTouchEventForSecurity(MotionEvent event) {
        return (this.mViewFlags & 1024) == 0 || (event.getFlags() & 1) == 0;
    }

    public boolean dispatchTrackballEvent(MotionEvent event) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTrackballEvent(event, 0);
        }
        return onTrackballEvent(event);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onGenericMotionEvent(event, 0);
        }
        int source = event.getSource();
        if ((source & 2) != 0) {
            int action = event.getAction();
            if (action == 9 || action == 7 || action == 10) {
                if (dispatchHoverEvent(event)) {
                    return true;
                }
            } else if (dispatchGenericPointerEvent(event)) {
                return true;
            }
        } else if (dispatchGenericFocusedEvent(event)) {
            return true;
        }
        if (dispatchGenericMotionEventInternal(event)) {
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(event, 0);
        }
        return false;
    }

    private boolean dispatchGenericMotionEventInternal(MotionEvent event) {
        ListenerInfo li = this.mListenerInfo;
        if ((li != null && li.mOnGenericMotionListener != null && (this.mViewFlags & 32) == 0 && li.mOnGenericMotionListener.onGenericMotion(this, event)) || onGenericMotionEvent(event)) {
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(event, 0);
        }
        return false;
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        ListenerInfo li = this.mListenerInfo;
        if (li == null || li.mOnHoverListener == null || (this.mViewFlags & 32) != 0 || !li.mOnHoverListener.onHover(this, event)) {
            return onHoverEvent(event);
        }
        return true;
    }

    protected boolean hasHoveredChild() {
        return false;
    }

    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        return false;
    }

    protected boolean dispatchGenericFocusedEvent(MotionEvent event) {
        return false;
    }

    public final boolean dispatchPointerEvent(MotionEvent event) {
        return event.isTouchEvent() ? dispatchTouchEvent(event) : dispatchGenericMotionEvent(event);
    }

    public void dispatchWindowFocusChanged(boolean hasFocus) {
        onWindowFocusChanged(hasFocus);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        InputMethodManager imm = InputMethodManager.peekInstance();
        if (!hasWindowFocus) {
            if (isPressed()) {
                setPressed(false);
            }
            if (imm != null && (this.mPrivateFlags & 2) != 0) {
                imm.focusOut(this);
            }
            removeLongPressCallback();
            removeTapCallback();
            onFocusLost();
        } else if (imm != null && (this.mPrivateFlags & 2) != 0) {
            imm.focusIn(this);
        }
        refreshDrawableState();
    }

    public boolean hasWindowFocus() {
        return this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus;
    }

    protected void dispatchVisibilityChanged(View changedView, int visibility) {
        onVisibilityChanged(changedView, visibility);
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == 0) {
            if (this.mAttachInfo != null) {
                initialAwakenScrollBars();
            } else {
                this.mPrivateFlags |= 134217728;
            }
        }
    }

    public void dispatchDisplayHint(int hint) {
        onDisplayHint(hint);
    }

    protected void onDisplayHint(int hint) {
    }

    public void dispatchWindowVisibilityChanged(int visibility) {
        onWindowVisibilityChanged(visibility);
    }

    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == 0) {
            initialAwakenScrollBars();
        }
    }

    public int getWindowVisibility() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mWindowVisibility;
        }
        return 8;
    }

    public void getWindowVisibleDisplayFrame(Rect outRect) {
        if (this.mAttachInfo != null) {
            try {
                this.mAttachInfo.mSession.getDisplayFrame(this.mAttachInfo.mWindow, outRect);
                Rect insets = this.mAttachInfo.mVisibleInsets;
                outRect.left += insets.left;
                outRect.top += insets.top;
                outRect.right -= insets.right;
                outRect.bottom -= insets.bottom;
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Display d = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        d.getRectSize(outRect);
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        onConfigurationChanged(newConfig);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
    }

    void dispatchCollectViewAttributes(AttachInfo attachInfo, int visibility) {
        performCollectViewAttributes(attachInfo, visibility);
    }

    void performCollectViewAttributes(AttachInfo attachInfo, int visibility) {
        if ((visibility & 12) == 0) {
            if ((this.mViewFlags & 67108864) == 67108864) {
                attachInfo.mKeepScreenOn = true;
            }
            attachInfo.mSystemUiVisibility |= this.mSystemUiVisibility;
            ListenerInfo li = this.mListenerInfo;
            if (li != null && li.mOnSystemUiVisibilityChangeListener != null) {
                attachInfo.mHasSystemUiListeners = true;
            }
        }
    }

    void needGlobalAttributesUpdate(boolean force) {
        AttachInfo ai = this.mAttachInfo;
        if (ai != null && !ai.mRecomputeGlobalAttributes) {
            if (force || ai.mKeepScreenOn || ai.mSystemUiVisibility != 0 || ai.mHasSystemUiListeners) {
                ai.mRecomputeGlobalAttributes = true;
            }
        }
    }

    @ViewDebug.ExportedProperty
    public boolean isInTouchMode() {
        return this.mAttachInfo != null ? this.mAttachInfo.mInTouchMode : ViewRootImpl.isInTouchMode();
    }

    @ViewDebug.CapturedViewProperty
    public final Context getContext() {
        return this.mContext;
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!KeyEvent.isConfirmKey(keyCode)) {
            return false;
        }
        if ((this.mViewFlags & 32) == 32) {
            return true;
        }
        if (((this.mViewFlags & 16384) != 16384 && (this.mViewFlags & 2097152) != 2097152) || event.getRepeatCount() != 0) {
            return false;
        }
        setPressed(true);
        checkForLongClick(0);
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!KeyEvent.isConfirmKey(keyCode)) {
            return false;
        }
        if ((this.mViewFlags & 32) == 32) {
            return true;
        }
        if ((this.mViewFlags & 16384) != 16384 || !isPressed()) {
            return false;
        }
        setPressed(false);
        if (this.mHasPerformedLongPress) {
            return false;
        }
        removeLongPressCallback();
        return performClick();
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return false;
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onCheckIsTextEditor() {
        return false;
    }

    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

    public boolean checkInputConnectionProxy(View view) {
        return false;
    }

    public void createContextMenu(ContextMenu contextMenu) {
        ContextMenu.ContextMenuInfo menuInfo = getContextMenuInfo();
        ((MenuBuilder) contextMenu).setCurrentMenuInfo(menuInfo);
        onCreateContextMenu(contextMenu);
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnCreateContextMenuListener != null) {
            li.mOnCreateContextMenuListener.onCreateContextMenu(contextMenu, this, menuInfo);
        }
        ((MenuBuilder) contextMenu).setCurrentMenuInfo(null);
        if (this.mParent != null) {
            this.mParent.createContextMenu(contextMenu);
        }
    }

    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return null;
    }

    protected void onCreateContextMenu(ContextMenu menu) {
    }

    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    public boolean onHoverEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (!this.mSendingHoverAccessibilityEvents) {
            if ((action == 9 || action == 7) && !hasHoveredChild() && pointInView(event.getX(), event.getY())) {
                sendAccessibilityHoverEvent(128);
                this.mSendingHoverAccessibilityEvents = true;
            }
        } else if (action == 10 || (action == 2 && !pointInView(event.getX(), event.getY()))) {
            this.mSendingHoverAccessibilityEvents = false;
            sendAccessibilityHoverEvent(256);
        }
        if (!isHoverable()) {
            return false;
        }
        switch (action) {
            case 9:
                setHovered(true);
                break;
            case 10:
                setHovered(false);
                break;
        }
        dispatchGenericMotionEventInternal(event);
        return true;
    }

    private boolean isHoverable() {
        int viewFlags = this.mViewFlags;
        if ((viewFlags & 32) == 32) {
            return false;
        }
        return (viewFlags & 16384) == 16384 || (viewFlags & 2097152) == 2097152;
    }

    @ViewDebug.ExportedProperty
    public boolean isHovered() {
        return (this.mPrivateFlags & 268435456) != 0;
    }

    public void setHovered(boolean hovered) {
        if (hovered) {
            if ((this.mPrivateFlags & 268435456) == 0) {
                this.mPrivateFlags |= 268435456;
                refreshDrawableState();
                onHoverChanged(true);
                return;
            }
            return;
        }
        if ((this.mPrivateFlags & 268435456) != 0) {
            this.mPrivateFlags &= -268435457;
            refreshDrawableState();
            onHoverChanged(false);
        }
    }

    public void onHoverChanged(boolean hovered) {
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int viewFlags = this.mViewFlags;
        if ((viewFlags & 32) == 32) {
            if (event.getAction() == 1 && (this.mPrivateFlags & 16384) != 0) {
                setPressed(false);
            }
            return (viewFlags & 16384) == 16384 || (viewFlags & 2097152) == 2097152;
        }
        if (this.mTouchDelegate != null && this.mTouchDelegate.onTouchEvent(event)) {
            return true;
        }
        if ((viewFlags & 16384) != 16384 && (viewFlags & 2097152) != 2097152) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                this.mHasPerformedLongPress = false;
                if (!performButtonActionOnTouchDown(event)) {
                    boolean isInScrollingContainer = isInScrollingContainer();
                    if (isInScrollingContainer) {
                        this.mPrivateFlags |= 33554432;
                        if (this.mPendingCheckForTap == null) {
                            this.mPendingCheckForTap = new CheckForTap();
                        }
                        this.mPendingCheckForTap.x = event.getX();
                        this.mPendingCheckForTap.y = event.getY();
                        postDelayed(this.mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    } else {
                        setPressed(true, x, y);
                        checkForLongClick(0);
                    }
                }
                break;
            case 1:
                boolean prepressed = (this.mPrivateFlags & 33554432) != 0;
                if ((this.mPrivateFlags & 16384) != 0 || prepressed) {
                    boolean focusTaken = false;
                    if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                        focusTaken = requestFocus();
                    }
                    if (prepressed) {
                        setPressed(true, x, y);
                    }
                    if (!this.mHasPerformedLongPress) {
                        removeLongPressCallback();
                        if (!focusTaken) {
                            if (this.mPerformClick == null) {
                                this.mPerformClick = new PerformClick();
                            }
                            if (!post(this.mPerformClick)) {
                                performClick();
                            }
                        }
                    }
                    if (this.mUnsetPressedState == null) {
                        this.mUnsetPressedState = new UnsetPressedState();
                    }
                    if (prepressed) {
                        postDelayed(this.mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
                    } else if (!post(this.mUnsetPressedState)) {
                        this.mUnsetPressedState.run();
                    }
                    removeTapCallback();
                }
                break;
            case 2:
                drawableHotspotChanged(x, y);
                if (!pointInView(x, y, this.mTouchSlop)) {
                    removeTapCallback();
                    if ((this.mPrivateFlags & 16384) != 0) {
                        removeLongPressCallback();
                        setPressed(false);
                    }
                }
                break;
            case 3:
                setPressed(false);
                removeTapCallback();
                removeLongPressCallback();
                break;
        }
        return true;
    }

    public boolean isInScrollingContainer() {
        for (ViewParent p = getParent(); p != null && (p instanceof ViewGroup); p = p.getParent()) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
        }
        return false;
    }

    private void removeLongPressCallback() {
        if (this.mPendingCheckForLongPress != null) {
            removeCallbacks(this.mPendingCheckForLongPress);
        }
    }

    private void removePerformClickCallback() {
        if (this.mPerformClick != null) {
            removeCallbacks(this.mPerformClick);
        }
    }

    private void removeUnsetPressCallback() {
        if ((this.mPrivateFlags & 16384) != 0 && this.mUnsetPressedState != null) {
            setPressed(false);
            removeCallbacks(this.mUnsetPressedState);
        }
    }

    private void removeTapCallback() {
        if (this.mPendingCheckForTap != null) {
            this.mPrivateFlags &= -33554433;
            removeCallbacks(this.mPendingCheckForTap);
        }
    }

    public void cancelLongPress() {
        removeLongPressCallback();
        removeTapCallback();
    }

    private void removeSendViewScrolledAccessibilityEventCallback() {
        if (this.mSendViewScrolledAccessibilityEvent != null) {
            removeCallbacks(this.mSendViewScrolledAccessibilityEvent);
            this.mSendViewScrolledAccessibilityEvent.mIsPending = false;
        }
    }

    public void setTouchDelegate(TouchDelegate delegate) {
        this.mTouchDelegate = delegate;
    }

    public TouchDelegate getTouchDelegate() {
        return this.mTouchDelegate;
    }

    public final void requestUnbufferedDispatch(MotionEvent event) {
        int action = event.getAction();
        if (this.mAttachInfo != null) {
            if ((action == 0 || action == 2) && event.isTouchEvent()) {
                this.mAttachInfo.mUnbufferedDispatchRequested = true;
            }
        }
    }

    void setFlags(int flags, int mask) {
        boolean accessibilityEnabled = AccessibilityManager.getInstance(this.mContext).isEnabled();
        boolean oldIncludeForAccessibility = accessibilityEnabled && includeForAccessibility();
        int old = this.mViewFlags;
        this.mViewFlags = (this.mViewFlags & (mask ^ (-1))) | (flags & mask);
        int changed = this.mViewFlags ^ old;
        if (changed != 0) {
            int privateFlags = this.mPrivateFlags;
            if ((changed & 1) != 0 && (privateFlags & 16) != 0) {
                if ((old & 1) == 1 && (privateFlags & 2) != 0) {
                    clearFocus();
                } else if ((old & 1) == 0 && (privateFlags & 2) == 0 && this.mParent != null) {
                    this.mParent.focusableViewAvailable(this);
                }
            }
            int newVisibility = flags & 12;
            if (newVisibility == 0 && (changed & 12) != 0) {
                this.mPrivateFlags |= 32;
                invalidate(true);
                needGlobalAttributesUpdate(true);
                if (this.mParent != null && this.mBottom > this.mTop && this.mRight > this.mLeft) {
                    this.mParent.focusableViewAvailable(this);
                }
            }
            if ((changed & 8) != 0) {
                needGlobalAttributesUpdate(false);
                requestLayout();
                if ((this.mViewFlags & 12) == 8) {
                    if (hasFocus()) {
                        clearFocus();
                    }
                    clearAccessibilityFocus();
                    destroyDrawingCache();
                    if (this.mParent instanceof View) {
                        ((View) this.mParent).invalidate(true);
                    }
                    this.mPrivateFlags |= 32;
                }
                if (this.mAttachInfo != null) {
                    this.mAttachInfo.mViewVisibilityChanged = true;
                }
            }
            if ((changed & 4) != 0) {
                needGlobalAttributesUpdate(false);
                this.mPrivateFlags |= 32;
                if ((this.mViewFlags & 12) == 4 && getRootView() != this) {
                    if (hasFocus()) {
                        clearFocus();
                    }
                    clearAccessibilityFocus();
                }
                if (this.mAttachInfo != null) {
                    this.mAttachInfo.mViewVisibilityChanged = true;
                }
            }
            if ((changed & 12) != 0) {
                if (newVisibility != 0 && this.mAttachInfo != null) {
                    cleanupDraw();
                }
                if (this.mParent instanceof ViewGroup) {
                    ((ViewGroup) this.mParent).onChildVisibilityChanged(this, changed & 12, newVisibility);
                    ((View) this.mParent).invalidate(true);
                } else if (this.mParent != null) {
                    this.mParent.invalidateChild(this, null);
                }
                dispatchVisibilityChanged(this, newVisibility);
                notifySubtreeAccessibilityStateChangedIfNeeded();
            }
            if ((131072 & changed) != 0) {
                destroyDrawingCache();
            }
            if ((32768 & changed) != 0) {
                destroyDrawingCache();
                this.mPrivateFlags &= -32769;
                invalidateParentCaches();
            }
            if ((DRAWING_CACHE_QUALITY_MASK & changed) != 0) {
                destroyDrawingCache();
                this.mPrivateFlags &= -32769;
            }
            if ((changed & 128) != 0) {
                if ((this.mViewFlags & 128) != 0) {
                    if (this.mBackground != null) {
                        this.mPrivateFlags &= -129;
                        this.mPrivateFlags |= 256;
                    } else {
                        this.mPrivateFlags |= 128;
                    }
                } else {
                    this.mPrivateFlags &= -129;
                }
                requestLayout();
                invalidate(true);
            }
            if ((67108864 & changed) != 0 && this.mParent != null && this.mAttachInfo != null && !this.mAttachInfo.mRecomputeGlobalAttributes) {
                this.mParent.recomputeViewAttributes(this);
            }
            if (accessibilityEnabled) {
                if ((changed & 1) != 0 || (changed & 12) != 0 || (changed & 16384) != 0 || (2097152 & changed) != 0) {
                    if (oldIncludeForAccessibility != includeForAccessibility()) {
                        notifySubtreeAccessibilityStateChangedIfNeeded();
                        return;
                    } else {
                        notifyViewAccessibilityStateChangedIfNeeded(0);
                        return;
                    }
                }
                if ((changed & 32) != 0) {
                    notifyViewAccessibilityStateChangedIfNeeded(0);
                }
            }
        }
    }

    public void bringToFront() {
        if (this.mParent != null) {
            this.mParent.bringChildToFront(this);
        }
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            postSendViewScrolledAccessibilityEventCallback();
        }
        this.mBackgroundSizeChanged = true;
        AttachInfo ai = this.mAttachInfo;
        if (ai != null) {
            ai.mViewScrollChanged = true;
        }
        if (this.mListenerInfo != null && this.mListenerInfo.mOnScrollChangeListener != null) {
            this.mListenerInfo.mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public final ViewParent getParent() {
        return this.mParent;
    }

    public void setScrollX(int value) {
        scrollTo(value, this.mScrollY);
    }

    public void setScrollY(int value) {
        scrollTo(this.mScrollX, value);
    }

    public final int getScrollX() {
        return this.mScrollX;
    }

    public final int getScrollY() {
        return this.mScrollY;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public final int getWidth() {
        return this.mRight - this.mLeft;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public final int getHeight() {
        return this.mBottom - this.mTop;
    }

    public void getDrawingRect(Rect outRect) {
        outRect.left = this.mScrollX;
        outRect.top = this.mScrollY;
        outRect.right = this.mScrollX + (this.mRight - this.mLeft);
        outRect.bottom = this.mScrollY + (this.mBottom - this.mTop);
    }

    public final int getMeasuredWidth() {
        return this.mMeasuredWidth & 16777215;
    }

    public final int getMeasuredWidthAndState() {
        return this.mMeasuredWidth;
    }

    public final int getMeasuredHeight() {
        return this.mMeasuredHeight & 16777215;
    }

    public final int getMeasuredHeightAndState() {
        return this.mMeasuredHeight;
    }

    public final int getMeasuredState() {
        return (this.mMeasuredWidth & (-16777216)) | ((this.mMeasuredHeight >> 16) & (-256));
    }

    public Matrix getMatrix() {
        ensureTransformationInfo();
        Matrix matrix = this.mTransformationInfo.mMatrix;
        this.mRenderNode.getMatrix(matrix);
        return matrix;
    }

    final boolean hasIdentityMatrix() {
        return this.mRenderNode.hasIdentityMatrix();
    }

    void ensureTransformationInfo() {
        if (this.mTransformationInfo == null) {
            this.mTransformationInfo = new TransformationInfo();
        }
    }

    public final Matrix getInverseMatrix() {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mInverseMatrix == null) {
            this.mTransformationInfo.mInverseMatrix = new Matrix();
        }
        Matrix matrix = this.mTransformationInfo.mInverseMatrix;
        this.mRenderNode.getInverseMatrix(matrix);
        return matrix;
    }

    public float getCameraDistance() {
        float dpi = this.mResources.getDisplayMetrics().densityDpi;
        return -(this.mRenderNode.getCameraDistance() * dpi);
    }

    public void setCameraDistance(float distance) {
        float dpi = this.mResources.getDisplayMetrics().densityDpi;
        invalidateViewProperty(true, false);
        this.mRenderNode.setCameraDistance((-Math.abs(distance)) / dpi);
        invalidateViewProperty(false, false);
        invalidateParentIfNeededAndWasQuickRejected();
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotation() {
        return this.mRenderNode.getRotation();
    }

    public void setRotation(float rotation) {
        if (rotation != getRotation()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotation(rotation);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotationY() {
        return this.mRenderNode.getRotationY();
    }

    public void setRotationY(float rotationY) {
        if (rotationY != getRotationY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotationY(rotationY);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotationX() {
        return this.mRenderNode.getRotationX();
    }

    public void setRotationX(float rotationX) {
        if (rotationX != getRotationX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotationX(rotationX);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getScaleX() {
        return this.mRenderNode.getScaleX();
    }

    public void setScaleX(float scaleX) {
        if (scaleX != getScaleX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setScaleX(scaleX);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getScaleY() {
        return this.mRenderNode.getScaleY();
    }

    public void setScaleY(float scaleY) {
        if (scaleY != getScaleY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setScaleY(scaleY);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getPivotX() {
        return this.mRenderNode.getPivotX();
    }

    public void setPivotX(float pivotX) {
        if (!this.mRenderNode.isPivotExplicitlySet() || pivotX != getPivotX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setPivotX(pivotX);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getPivotY() {
        return this.mRenderNode.getPivotY();
    }

    public void setPivotY(float pivotY) {
        if (!this.mRenderNode.isPivotExplicitlySet() || pivotY != getPivotY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setPivotY(pivotY);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getAlpha() {
        if (this.mTransformationInfo != null) {
            return this.mTransformationInfo.mAlpha;
        }
        return 1.0f;
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean hasOverlappingRendering() {
        return true;
    }

    public void setAlpha(float alpha) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mAlpha != alpha) {
            this.mTransformationInfo.mAlpha = alpha;
            if (onSetAlpha((int) (255.0f * alpha))) {
                this.mPrivateFlags |= 262144;
                invalidateParentCaches();
                invalidate(true);
            } else {
                this.mPrivateFlags &= -262145;
                invalidateViewProperty(true, false);
                this.mRenderNode.setAlpha(getFinalAlpha());
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    boolean setAlphaNoInvalidation(float alpha) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mAlpha != alpha) {
            this.mTransformationInfo.mAlpha = alpha;
            boolean subclassHandlesAlpha = onSetAlpha((int) (255.0f * alpha));
            if (subclassHandlesAlpha) {
                this.mPrivateFlags |= 262144;
                return true;
            }
            this.mPrivateFlags &= -262145;
            this.mRenderNode.setAlpha(getFinalAlpha());
        }
        return false;
    }

    public void setTransitionAlpha(float alpha) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mTransitionAlpha != alpha) {
            this.mTransformationInfo.mTransitionAlpha = alpha;
            this.mPrivateFlags &= -262145;
            invalidateViewProperty(true, false);
            this.mRenderNode.setAlpha(getFinalAlpha());
        }
    }

    private float getFinalAlpha() {
        if (this.mTransformationInfo != null) {
            return this.mTransformationInfo.mAlpha * this.mTransformationInfo.mTransitionAlpha;
        }
        return 1.0f;
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTransitionAlpha() {
        if (this.mTransformationInfo != null) {
            return this.mTransformationInfo.mTransitionAlpha;
        }
        return 1.0f;
    }

    @ViewDebug.CapturedViewProperty
    public final int getTop() {
        return this.mTop;
    }

    public final void setTop(int top) {
        int minTop;
        int yLoc;
        if (top != this.mTop) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (matrixIsIdentity) {
                if (this.mAttachInfo != null) {
                    if (top < this.mTop) {
                        minTop = top;
                        yLoc = top - this.mTop;
                    } else {
                        minTop = this.mTop;
                        yLoc = 0;
                    }
                    invalidate(0, yLoc, this.mRight - this.mLeft, this.mBottom - minTop);
                }
            } else {
                invalidate(true);
            }
            int width = this.mRight - this.mLeft;
            int oldHeight = this.mBottom - this.mTop;
            this.mTop = top;
            this.mRenderNode.setTop(this.mTop);
            sizeChange(width, this.mBottom - this.mTop, width, oldHeight);
            if (!matrixIsIdentity) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
            if ((this.mPrivateFlags2 & 268435456) == 268435456) {
                invalidateParentIfNeeded();
            }
        }
    }

    @ViewDebug.CapturedViewProperty
    public final int getBottom() {
        return this.mBottom;
    }

    public boolean isDirty() {
        return (this.mPrivateFlags & 6291456) != 0;
    }

    public final void setBottom(int bottom) {
        int maxBottom;
        if (bottom != this.mBottom) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (matrixIsIdentity) {
                if (this.mAttachInfo != null) {
                    if (bottom < this.mBottom) {
                        maxBottom = this.mBottom;
                    } else {
                        maxBottom = bottom;
                    }
                    invalidate(0, 0, this.mRight - this.mLeft, maxBottom - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int width = this.mRight - this.mLeft;
            int oldHeight = this.mBottom - this.mTop;
            this.mBottom = bottom;
            this.mRenderNode.setBottom(this.mBottom);
            sizeChange(width, this.mBottom - this.mTop, width, oldHeight);
            if (!matrixIsIdentity) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
            if ((this.mPrivateFlags2 & 268435456) == 268435456) {
                invalidateParentIfNeeded();
            }
        }
    }

    @ViewDebug.CapturedViewProperty
    public final int getLeft() {
        return this.mLeft;
    }

    public final void setLeft(int left) {
        int minLeft;
        int xLoc;
        if (left != this.mLeft) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (matrixIsIdentity) {
                if (this.mAttachInfo != null) {
                    if (left < this.mLeft) {
                        minLeft = left;
                        xLoc = left - this.mLeft;
                    } else {
                        minLeft = this.mLeft;
                        xLoc = 0;
                    }
                    invalidate(xLoc, 0, this.mRight - minLeft, this.mBottom - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int oldWidth = this.mRight - this.mLeft;
            int height = this.mBottom - this.mTop;
            this.mLeft = left;
            this.mRenderNode.setLeft(left);
            sizeChange(this.mRight - this.mLeft, height, oldWidth, height);
            if (!matrixIsIdentity) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
            if ((this.mPrivateFlags2 & 268435456) == 268435456) {
                invalidateParentIfNeeded();
            }
        }
    }

    @ViewDebug.CapturedViewProperty
    public final int getRight() {
        return this.mRight;
    }

    public final void setRight(int right) {
        int maxRight;
        if (right != this.mRight) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (matrixIsIdentity) {
                if (this.mAttachInfo != null) {
                    if (right < this.mRight) {
                        maxRight = this.mRight;
                    } else {
                        maxRight = right;
                    }
                    invalidate(0, 0, maxRight - this.mLeft, this.mBottom - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int oldWidth = this.mRight - this.mLeft;
            int height = this.mBottom - this.mTop;
            this.mRight = right;
            this.mRenderNode.setRight(this.mRight);
            sizeChange(this.mRight - this.mLeft, height, oldWidth, height);
            if (!matrixIsIdentity) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
            if ((this.mPrivateFlags2 & 268435456) == 268435456) {
                invalidateParentIfNeeded();
            }
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getX() {
        return this.mLeft + getTranslationX();
    }

    public void setX(float x) {
        setTranslationX(x - this.mLeft);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getY() {
        return this.mTop + getTranslationY();
    }

    public void setY(float y) {
        setTranslationY(y - this.mTop);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getZ() {
        return getElevation() + getTranslationZ();
    }

    public void setZ(float z) {
        setTranslationZ(z - getElevation());
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getElevation() {
        return this.mRenderNode.getElevation();
    }

    public void setElevation(float elevation) {
        if (elevation != getElevation()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setElevation(elevation);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationX() {
        return this.mRenderNode.getTranslationX();
    }

    public void setTranslationX(float translationX) {
        if (translationX != getTranslationX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationX(translationX);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationY() {
        return this.mRenderNode.getTranslationY();
    }

    public void setTranslationY(float translationY) {
        if (translationY != getTranslationY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationY(translationY);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationZ() {
        return this.mRenderNode.getTranslationZ();
    }

    public void setTranslationZ(float translationZ) {
        if (translationZ != getTranslationZ()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationZ(translationZ);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    public void setAnimationMatrix(Matrix matrix) {
        invalidateViewProperty(true, false);
        this.mRenderNode.setAnimationMatrix(matrix);
        invalidateViewProperty(false, true);
        invalidateParentIfNeededAndWasQuickRejected();
    }

    public StateListAnimator getStateListAnimator() {
        return this.mStateListAnimator;
    }

    public void setStateListAnimator(StateListAnimator stateListAnimator) {
        if (this.mStateListAnimator != stateListAnimator) {
            if (this.mStateListAnimator != null) {
                this.mStateListAnimator.setTarget(null);
            }
            this.mStateListAnimator = stateListAnimator;
            if (stateListAnimator != null) {
                stateListAnimator.setTarget(this);
                if (isAttachedToWindow()) {
                    stateListAnimator.setState(getDrawableState());
                }
            }
        }
    }

    public final boolean getClipToOutline() {
        return this.mRenderNode.getClipToOutline();
    }

    public void setClipToOutline(boolean clipToOutline) {
        damageInParent();
        if (getClipToOutline() != clipToOutline) {
            this.mRenderNode.setClipToOutline(clipToOutline);
        }
    }

    private void setOutlineProviderFromAttribute(int providerInt) {
        switch (providerInt) {
            case 0:
                setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                break;
            case 1:
                setOutlineProvider(null);
                break;
            case 2:
                setOutlineProvider(ViewOutlineProvider.BOUNDS);
                break;
            case 3:
                setOutlineProvider(ViewOutlineProvider.PADDED_BOUNDS);
                break;
        }
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
        this.mOutlineProvider = provider;
        invalidateOutline();
    }

    public ViewOutlineProvider getOutlineProvider() {
        return this.mOutlineProvider;
    }

    public void invalidateOutline() {
        rebuildOutline();
        notifySubtreeAccessibilityStateChangedIfNeeded();
        invalidateViewProperty(false, false);
    }

    private void rebuildOutline() {
        if (this.mAttachInfo != null) {
            if (this.mOutlineProvider == null) {
                this.mRenderNode.setOutline(null);
                return;
            }
            Outline outline = this.mAttachInfo.mTmpOutline;
            outline.setEmpty();
            outline.setAlpha(1.0f);
            this.mOutlineProvider.getOutline(this, outline);
            this.mRenderNode.setOutline(outline);
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean hasShadow() {
        return this.mRenderNode.hasShadow();
    }

    public void setRevealClip(boolean shouldClip, float x, float y, float radius) {
        this.mRenderNode.setRevealClip(shouldClip, x, y, radius);
        invalidateViewProperty(false, false);
    }

    public void getHitRect(Rect outRect) {
        if (hasIdentityMatrix() || this.mAttachInfo == null) {
            outRect.set(this.mLeft, this.mTop, this.mRight, this.mBottom);
            return;
        }
        RectF tmpRect = this.mAttachInfo.mTmpTransformRect;
        tmpRect.set(0.0f, 0.0f, getWidth(), getHeight());
        getMatrix().mapRect(tmpRect);
        outRect.set(((int) tmpRect.left) + this.mLeft, ((int) tmpRect.top) + this.mTop, ((int) tmpRect.right) + this.mLeft, ((int) tmpRect.bottom) + this.mTop);
    }

    final boolean pointInView(float localX, float localY) {
        return localX >= 0.0f && localX < ((float) (this.mRight - this.mLeft)) && localY >= 0.0f && localY < ((float) (this.mBottom - this.mTop));
    }

    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= (-slop) && localY >= (-slop) && localX < ((float) (this.mRight - this.mLeft)) + slop && localY < ((float) (this.mBottom - this.mTop)) + slop;
    }

    public void getFocusedRect(Rect r) {
        getDrawingRect(r);
    }

    public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
        int width = this.mRight - this.mLeft;
        int height = this.mBottom - this.mTop;
        if (width <= 0 || height <= 0) {
            return false;
        }
        r.set(0, 0, width, height);
        if (globalOffset != null) {
            globalOffset.set(-this.mScrollX, -this.mScrollY);
        }
        return this.mParent == null || this.mParent.getChildVisibleRect(this, r, globalOffset);
    }

    public final boolean getGlobalVisibleRect(Rect r) {
        return getGlobalVisibleRect(r, null);
    }

    public final boolean getLocalVisibleRect(Rect r) {
        Point offset = this.mAttachInfo != null ? this.mAttachInfo.mPoint : new Point();
        if (getGlobalVisibleRect(r, offset)) {
            r.offset(-offset.x, -offset.y);
            return true;
        }
        return false;
    }

    public void offsetTopAndBottom(int offset) {
        int minTop;
        int maxBottom;
        int yLoc;
        if (offset != 0) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (!matrixIsIdentity || isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                ViewParent p = this.mParent;
                if (p != null && this.mAttachInfo != null) {
                    Rect r = this.mAttachInfo.mTmpInvalRect;
                    if (offset < 0) {
                        minTop = this.mTop + offset;
                        maxBottom = this.mBottom;
                        yLoc = offset;
                    } else {
                        minTop = this.mTop;
                        maxBottom = this.mBottom + offset;
                        yLoc = 0;
                    }
                    r.set(0, yLoc, this.mRight - this.mLeft, maxBottom - minTop);
                    p.invalidateChild(this, r);
                }
            }
            this.mTop += offset;
            this.mBottom += offset;
            this.mRenderNode.offsetTopAndBottom(offset);
            if (isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                if (!matrixIsIdentity) {
                    invalidateViewProperty(false, true);
                }
                invalidateParentIfNeeded();
            }
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    public void offsetLeftAndRight(int offset) {
        int minLeft;
        int maxRight;
        if (offset != 0) {
            boolean matrixIsIdentity = hasIdentityMatrix();
            if (!matrixIsIdentity || isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                ViewParent p = this.mParent;
                if (p != null && this.mAttachInfo != null) {
                    Rect r = this.mAttachInfo.mTmpInvalRect;
                    if (offset < 0) {
                        minLeft = this.mLeft + offset;
                        maxRight = this.mRight;
                    } else {
                        minLeft = this.mLeft;
                        maxRight = this.mRight + offset;
                    }
                    r.set(0, 0, maxRight - minLeft, this.mBottom - this.mTop);
                    p.invalidateChild(this, r);
                }
            }
            this.mLeft += offset;
            this.mRight += offset;
            this.mRenderNode.offsetLeftAndRight(offset);
            if (isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                if (!matrixIsIdentity) {
                    invalidateViewProperty(false, true);
                }
                invalidateParentIfNeeded();
            }
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
    public ViewGroup.LayoutParams getLayoutParams() {
        return this.mLayoutParams;
    }

    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params == null) {
            throw new NullPointerException("Layout parameters cannot be null");
        }
        this.mLayoutParams = params;
        resolveLayoutParams();
        if (this.mParent instanceof ViewGroup) {
            ((ViewGroup) this.mParent).onSetLayoutParams(this, params);
        }
        requestLayout();
    }

    public void resolveLayoutParams() {
        if (this.mLayoutParams != null) {
            this.mLayoutParams.resolveLayoutDirection(getLayoutDirection());
        }
    }

    public void scrollTo(int x, int y) {
        if (this.mScrollX != x || this.mScrollY != y) {
            int oldX = this.mScrollX;
            int oldY = this.mScrollY;
            this.mScrollX = x;
            this.mScrollY = y;
            invalidateParentCaches();
            onScrollChanged(this.mScrollX, this.mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }

    public void scrollBy(int x, int y) {
        scrollTo(this.mScrollX + x, this.mScrollY + y);
    }

    protected boolean awakenScrollBars() {
        return this.mScrollCache != null && awakenScrollBars(this.mScrollCache.scrollBarDefaultDelayBeforeFade, true);
    }

    private boolean initialAwakenScrollBars() {
        return this.mScrollCache != null && awakenScrollBars(this.mScrollCache.scrollBarDefaultDelayBeforeFade * 4, true);
    }

    protected boolean awakenScrollBars(int startDelay) {
        return awakenScrollBars(startDelay, true);
    }

    protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
        ScrollabilityCache scrollCache = this.mScrollCache;
        if (scrollCache == null || !scrollCache.fadeScrollBars) {
            return false;
        }
        if (scrollCache.scrollBar == null) {
            scrollCache.scrollBar = new ScrollBarDrawable();
        }
        if (!isHorizontalScrollBarEnabled() && !isVerticalScrollBarEnabled()) {
            return false;
        }
        if (invalidate) {
            postInvalidateOnAnimation();
        }
        if (scrollCache.state == 0) {
            startDelay = Math.max(750, startDelay);
        }
        long fadeStartTime = AnimationUtils.currentAnimationTimeMillis() + ((long) startDelay);
        scrollCache.fadeStartTime = fadeStartTime;
        scrollCache.state = 1;
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mHandler.removeCallbacks(scrollCache);
            this.mAttachInfo.mHandler.postAtTime(scrollCache, fadeStartTime);
        }
        return true;
    }

    private boolean skipInvalidate() {
        return ((this.mViewFlags & 12) == 0 || this.mCurrentAnimation != null || ((this.mParent instanceof ViewGroup) && ((ViewGroup) this.mParent).isViewTransitioning(this))) ? false : true;
    }

    public void invalidate(Rect dirty) {
        int scrollX = this.mScrollX;
        int scrollY = this.mScrollY;
        invalidateInternal(dirty.left - scrollX, dirty.top - scrollY, dirty.right - scrollX, dirty.bottom - scrollY, true, false);
    }

    public void invalidate(int l, int t, int r, int b) {
        int scrollX = this.mScrollX;
        int scrollY = this.mScrollY;
        invalidateInternal(l - scrollX, t - scrollY, r - scrollX, b - scrollY, true, false);
    }

    public void invalidate() {
        invalidate(true);
    }

    void invalidate(boolean invalidateCache) {
        invalidateInternal(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop, invalidateCache, true);
    }

    void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache, boolean fullInvalidate) {
        View receiver;
        if (this.mGhostView != null) {
            this.mGhostView.invalidate(true);
            return;
        }
        if (!skipInvalidate()) {
            if ((this.mPrivateFlags & 48) == 48 || ((invalidateCache && (this.mPrivateFlags & 32768) == 32768) || (this.mPrivateFlags & Integer.MIN_VALUE) != Integer.MIN_VALUE || (fullInvalidate && isOpaque() != this.mLastIsOpaque))) {
                if (fullInvalidate) {
                    this.mLastIsOpaque = isOpaque();
                    this.mPrivateFlags &= -33;
                }
                this.mPrivateFlags |= 2097152;
                if (invalidateCache) {
                    this.mPrivateFlags |= Integer.MIN_VALUE;
                    this.mPrivateFlags &= -32769;
                }
                AttachInfo ai = this.mAttachInfo;
                ViewParent p = this.mParent;
                if (p != null && ai != null && l < r && t < b) {
                    Rect damage = ai.mTmpInvalRect;
                    damage.set(l, t, r, b);
                    p.invalidateChild(this, damage);
                }
                if (this.mBackground != null && this.mBackground.isProjected() && (receiver = getProjectionReceiver()) != null) {
                    receiver.damageInParent();
                }
                if (isHardwareAccelerated() && getZ() != 0.0f) {
                    damageShadowReceiver();
                }
            }
        }
    }

    private View getProjectionReceiver() {
        for (ViewParent p = getParent(); p != null && (p instanceof View); p = p.getParent()) {
            View v = (View) p;
            if (v.isProjectionReceiver()) {
                return v;
            }
        }
        return null;
    }

    private boolean isProjectionReceiver() {
        return this.mBackground != null;
    }

    private void damageShadowReceiver() {
        ViewParent p;
        AttachInfo ai = this.mAttachInfo;
        if (ai != null && (p = getParent()) != null && (p instanceof ViewGroup)) {
            ViewGroup vg = (ViewGroup) p;
            vg.damageInParent();
        }
    }

    void invalidateViewProperty(boolean invalidateParent, boolean forceRedraw) {
        if (!isHardwareAccelerated() || !this.mRenderNode.isValid() || (this.mPrivateFlags & 64) != 0) {
            if (invalidateParent) {
                invalidateParentCaches();
            }
            if (forceRedraw) {
                this.mPrivateFlags |= 32;
            }
            invalidate(false);
        } else {
            damageInParent();
        }
        if (isHardwareAccelerated() && invalidateParent && getZ() != 0.0f) {
            damageShadowReceiver();
        }
    }

    protected void damageInParent() {
        AttachInfo ai = this.mAttachInfo;
        ViewParent p = this.mParent;
        if (p != null && ai != null) {
            Rect r = ai.mTmpInvalRect;
            r.set(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
            if (this.mParent instanceof ViewGroup) {
                ((ViewGroup) this.mParent).damageChild(this, r);
            } else {
                this.mParent.invalidateChild(this, r);
            }
        }
    }

    void transformRect(Rect rect) {
        if (!getMatrix().isIdentity()) {
            RectF boundingRect = this.mAttachInfo.mTmpTransformRect;
            boundingRect.set(rect);
            getMatrix().mapRect(boundingRect);
            rect.set((int) Math.floor(boundingRect.left), (int) Math.floor(boundingRect.top), (int) Math.ceil(boundingRect.right), (int) Math.ceil(boundingRect.bottom));
        }
    }

    protected void invalidateParentCaches() {
        if (this.mParent instanceof View) {
            ((View) this.mParent).mPrivateFlags |= Integer.MIN_VALUE;
        }
    }

    protected void invalidateParentIfNeeded() {
        if (isHardwareAccelerated() && (this.mParent instanceof View)) {
            ((View) this.mParent).invalidate(true);
        }
    }

    protected void invalidateParentIfNeededAndWasQuickRejected() {
        if ((this.mPrivateFlags2 & 268435456) != 0) {
            invalidateParentIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean isOpaque() {
        return (this.mPrivateFlags & 25165824) == 25165824 && getFinalAlpha() >= 1.0f;
    }

    protected void computeOpaqueFlags() {
        if (this.mBackground != null && this.mBackground.getOpacity() == -1) {
            this.mPrivateFlags |= 8388608;
        } else {
            this.mPrivateFlags &= -8388609;
        }
        int flags = this.mViewFlags;
        if (((flags & 512) == 0 && (flags & 256) == 0) || (flags & 50331648) == 0 || (flags & 50331648) == 33554432) {
            this.mPrivateFlags |= 16777216;
        } else {
            this.mPrivateFlags &= -16777217;
        }
    }

    protected boolean hasOpaqueScrollbars() {
        return (this.mPrivateFlags & 16777216) == 16777216;
    }

    public Handler getHandler() {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler;
        }
        return null;
    }

    public ViewRootImpl getViewRootImpl() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mViewRootImpl;
        }
        return null;
    }

    public HardwareRenderer getHardwareRenderer() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mHardwareRenderer;
        }
        return null;
    }

    public boolean post(Runnable action) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(action);
        }
        ViewRootImpl.getRunQueue().post(action);
        return true;
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.postDelayed(action, delayMillis);
        }
        ViewRootImpl.getRunQueue().postDelayed(action, delayMillis);
        return true;
    }

    public void postOnAnimation(Runnable action) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.mChoreographer.postCallback(1, action, null);
        } else {
            ViewRootImpl.getRunQueue().post(action);
        }
    }

    public void postOnAnimationDelayed(Runnable action, long delayMillis) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.mChoreographer.postCallbackDelayed(1, action, null, delayMillis);
        } else {
            ViewRootImpl.getRunQueue().postDelayed(action, delayMillis);
        }
    }

    public boolean removeCallbacks(Runnable action) {
        if (action != null) {
            AttachInfo attachInfo = this.mAttachInfo;
            if (attachInfo != null) {
                attachInfo.mHandler.removeCallbacks(action);
                attachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, action, null);
            }
            ViewRootImpl.getRunQueue().removeCallbacks(action);
        }
        return true;
    }

    public void postInvalidate() {
        postInvalidateDelayed(0L);
    }

    public void postInvalidate(int left, int top, int right, int bottom) {
        postInvalidateDelayed(0L, left, top, right, bottom);
    }

    public void postInvalidateDelayed(long delayMilliseconds) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.dispatchInvalidateDelayed(this, delayMilliseconds);
        }
    }

    public void postInvalidateDelayed(long delayMilliseconds, int left, int top, int right, int bottom) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            AttachInfo.InvalidateInfo info = AttachInfo.InvalidateInfo.obtain();
            info.target = this;
            info.left = left;
            info.top = top;
            info.right = right;
            info.bottom = bottom;
            attachInfo.mViewRootImpl.dispatchInvalidateRectDelayed(info, delayMilliseconds);
        }
    }

    public void postInvalidateOnAnimation() {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.dispatchInvalidateOnAnimation(this);
        }
    }

    public void postInvalidateOnAnimation(int left, int top, int right, int bottom) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            AttachInfo.InvalidateInfo info = AttachInfo.InvalidateInfo.obtain();
            info.target = this;
            info.left = left;
            info.top = top;
            info.right = right;
            info.bottom = bottom;
            attachInfo.mViewRootImpl.dispatchInvalidateRectOnAnimation(info);
        }
    }

    private void postSendViewScrolledAccessibilityEventCallback() {
        if (this.mSendViewScrolledAccessibilityEvent == null) {
            this.mSendViewScrolledAccessibilityEvent = new SendViewScrolledAccessibilityEvent();
        }
        if (!this.mSendViewScrolledAccessibilityEvent.mIsPending) {
            this.mSendViewScrolledAccessibilityEvent.mIsPending = true;
            postDelayed(this.mSendViewScrolledAccessibilityEvent, ViewConfiguration.getSendRecurringAccessibilityEventsInterval());
        }
    }

    public void computeScroll() {
    }

    public boolean isHorizontalFadingEdgeEnabled() {
        return (this.mViewFlags & 4096) == 4096;
    }

    public void setHorizontalFadingEdgeEnabled(boolean horizontalFadingEdgeEnabled) {
        if (isHorizontalFadingEdgeEnabled() != horizontalFadingEdgeEnabled) {
            if (horizontalFadingEdgeEnabled) {
                initScrollCache();
            }
            this.mViewFlags ^= 4096;
        }
    }

    public boolean isVerticalFadingEdgeEnabled() {
        return (this.mViewFlags & 8192) == 8192;
    }

    public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
        if (isVerticalFadingEdgeEnabled() != verticalFadingEdgeEnabled) {
            if (verticalFadingEdgeEnabled) {
                initScrollCache();
            }
            this.mViewFlags ^= 8192;
        }
    }

    protected float getTopFadingEdgeStrength() {
        return computeVerticalScrollOffset() > 0 ? 1.0f : 0.0f;
    }

    protected float getBottomFadingEdgeStrength() {
        return computeVerticalScrollOffset() + computeVerticalScrollExtent() < computeVerticalScrollRange() ? 1.0f : 0.0f;
    }

    protected float getLeftFadingEdgeStrength() {
        return computeHorizontalScrollOffset() > 0 ? 1.0f : 0.0f;
    }

    protected float getRightFadingEdgeStrength() {
        return computeHorizontalScrollOffset() + computeHorizontalScrollExtent() < computeHorizontalScrollRange() ? 1.0f : 0.0f;
    }

    public boolean isHorizontalScrollBarEnabled() {
        return (this.mViewFlags & 256) == 256;
    }

    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        if (isHorizontalScrollBarEnabled() != horizontalScrollBarEnabled) {
            this.mViewFlags ^= 256;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    public boolean isVerticalScrollBarEnabled() {
        return (this.mViewFlags & 512) == 512;
    }

    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        if (isVerticalScrollBarEnabled() != verticalScrollBarEnabled) {
            this.mViewFlags ^= 512;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    protected void recomputePadding() {
        internalSetPadding(this.mUserPaddingLeft, this.mPaddingTop, this.mUserPaddingRight, this.mUserPaddingBottom);
    }

    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        initScrollCache();
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        scrollabilityCache.fadeScrollBars = fadeScrollbars;
        if (fadeScrollbars) {
            scrollabilityCache.state = 0;
        } else {
            scrollabilityCache.state = 1;
        }
    }

    public boolean isScrollbarFadingEnabled() {
        return this.mScrollCache != null && this.mScrollCache.fadeScrollBars;
    }

    public int getScrollBarDefaultDelayBeforeFade() {
        return this.mScrollCache == null ? ViewConfiguration.getScrollDefaultDelay() : this.mScrollCache.scrollBarDefaultDelayBeforeFade;
    }

    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
        getScrollCache().scrollBarDefaultDelayBeforeFade = scrollBarDefaultDelayBeforeFade;
    }

    public int getScrollBarFadeDuration() {
        return this.mScrollCache == null ? ViewConfiguration.getScrollBarFadeDuration() : this.mScrollCache.scrollBarFadeDuration;
    }

    public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
        getScrollCache().scrollBarFadeDuration = scrollBarFadeDuration;
    }

    public int getScrollBarSize() {
        return this.mScrollCache == null ? ViewConfiguration.get(this.mContext).getScaledScrollBarSize() : this.mScrollCache.scrollBarSize;
    }

    public void setScrollBarSize(int scrollBarSize) {
        getScrollCache().scrollBarSize = scrollBarSize;
    }

    public void setScrollBarStyle(int style) {
        if (style != (this.mViewFlags & 50331648)) {
            this.mViewFlags = (this.mViewFlags & (-50331649)) | (style & 50331648);
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = 0, to = "INSIDE_OVERLAY"), @ViewDebug.IntToString(from = 16777216, to = "INSIDE_INSET"), @ViewDebug.IntToString(from = 33554432, to = "OUTSIDE_OVERLAY"), @ViewDebug.IntToString(from = 50331648, to = "OUTSIDE_INSET")})
    public int getScrollBarStyle() {
        return this.mViewFlags & 50331648;
    }

    protected int computeHorizontalScrollRange() {
        return getWidth();
    }

    protected int computeHorizontalScrollOffset() {
        return this.mScrollX;
    }

    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    protected int computeVerticalScrollRange() {
        return getHeight();
    }

    protected int computeVerticalScrollOffset() {
        return this.mScrollY;
    }

    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    public boolean canScrollHorizontally(int direction) {
        int offset = computeHorizontalScrollOffset();
        int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (range == 0) {
            return false;
        }
        return direction < 0 ? offset > 0 : offset < range + (-1);
    }

    public boolean canScrollVertically(int direction) {
        int offset = computeVerticalScrollOffset();
        int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) {
            return false;
        }
        return direction < 0 ? offset > 0 : offset < range + (-1);
    }

    protected final void onDrawScrollBars(Canvas canvas) {
        int state;
        int left;
        ScrollabilityCache cache = this.mScrollCache;
        if (cache != null && (state = cache.state) != 0) {
            boolean invalidate = false;
            if (state == 2) {
                if (cache.interpolatorValues == null) {
                    cache.interpolatorValues = new float[1];
                }
                float[] values = cache.interpolatorValues;
                if (cache.scrollBarInterpolator.timeToValues(values) == Interpolator.Result.FREEZE_END) {
                    cache.state = 0;
                } else {
                    cache.scrollBar.mutate().setAlpha(Math.round(values[0]));
                }
                invalidate = true;
            } else {
                cache.scrollBar.mutate().setAlpha(255);
            }
            int viewFlags = this.mViewFlags;
            boolean drawHorizontalScrollBar = (viewFlags & 256) == 256;
            boolean drawVerticalScrollBar = (viewFlags & 512) == 512 && !isVerticalScrollBarHidden();
            if (drawVerticalScrollBar || drawHorizontalScrollBar) {
                int width = this.mRight - this.mLeft;
                int height = this.mBottom - this.mTop;
                ScrollBarDrawable scrollBar = cache.scrollBar;
                int scrollX = this.mScrollX;
                int scrollY = this.mScrollY;
                int inside = (33554432 & viewFlags) == 0 ? -1 : 0;
                if (drawHorizontalScrollBar) {
                    int size = scrollBar.getSize(false);
                    if (size <= 0) {
                        size = cache.scrollBarSize;
                    }
                    scrollBar.setParameters(computeHorizontalScrollRange(), computeHorizontalScrollOffset(), computeHorizontalScrollExtent(), false);
                    int verticalScrollBarGap = drawVerticalScrollBar ? getVerticalScrollbarWidth() : 0;
                    int top = ((scrollY + height) - size) - (this.mUserPaddingBottom & inside);
                    int left2 = scrollX + (this.mPaddingLeft & inside);
                    int right = ((scrollX + width) - (this.mUserPaddingRight & inside)) - verticalScrollBarGap;
                    int bottom = top + size;
                    onDrawHorizontalScrollBar(canvas, scrollBar, left2, top, right, bottom);
                    if (invalidate) {
                        invalidate(left2, top, right, bottom);
                    }
                }
                if (drawVerticalScrollBar) {
                    int size2 = scrollBar.getSize(true);
                    if (size2 <= 0) {
                        size2 = cache.scrollBarSize;
                    }
                    scrollBar.setParameters(computeVerticalScrollRange(), computeVerticalScrollOffset(), computeVerticalScrollExtent(), true);
                    int verticalScrollbarPosition = this.mVerticalScrollbarPosition;
                    if (verticalScrollbarPosition == 0) {
                        verticalScrollbarPosition = isLayoutRtl() ? 1 : 2;
                    }
                    switch (verticalScrollbarPosition) {
                        case 1:
                            left = scrollX + (this.mUserPaddingLeft & inside);
                            break;
                        default:
                            left = ((scrollX + width) - size2) - (this.mUserPaddingRight & inside);
                            break;
                    }
                    int top2 = scrollY + (this.mPaddingTop & inside);
                    int right2 = left + size2;
                    int bottom2 = (scrollY + height) - (this.mUserPaddingBottom & inside);
                    onDrawVerticalScrollBar(canvas, scrollBar, left, top2, right2, bottom2);
                    if (invalidate) {
                        invalidate(left, top2, right2, bottom2);
                    }
                }
            }
        }
    }

    protected boolean isVerticalScrollBarHidden() {
        return false;
    }

    protected void onDrawHorizontalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    protected void onDraw(Canvas canvas) {
    }

    void assignParent(ViewParent parent) {
        if (this.mParent == null) {
            this.mParent = parent;
        } else {
            if (parent == null) {
                this.mParent = null;
                return;
            }
            throw new RuntimeException("view " + this + " being added, but it already has a parent");
        }
    }

    protected void onAttachedToWindow() {
        if ((this.mPrivateFlags & 512) != 0) {
            this.mParent.requestTransparentRegion(this);
        }
        if ((this.mPrivateFlags & 134217728) != 0) {
            initialAwakenScrollBars();
            this.mPrivateFlags &= -134217729;
        }
        this.mPrivateFlags3 &= -5;
        jumpDrawablesToCurrentState();
        resetSubtreeAccessibilityStateChanged();
        rebuildOutline();
        if (isFocused()) {
            InputMethodManager imm = InputMethodManager.peekInstance();
            imm.focusIn(this);
        }
    }

    public boolean resolveRtlPropertiesIfNeeded() {
        if (!needRtlPropertiesResolution()) {
            return false;
        }
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
            resolveLayoutParams();
        }
        if (!isTextDirectionResolved()) {
            resolveTextDirection();
        }
        if (!isTextAlignmentResolved()) {
            resolveTextAlignment();
        }
        if (!areDrawablesResolved()) {
            resolveDrawables();
        }
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        onRtlPropertiesChanged(getLayoutDirection());
        return true;
    }

    public void resetRtlProperties() {
        resetResolvedLayoutDirection();
        resetResolvedTextDirection();
        resetResolvedTextAlignment();
        resetResolvedPadding();
        resetResolvedDrawables();
    }

    void dispatchScreenStateChanged(int screenState) {
        onScreenStateChanged(screenState);
    }

    public void onScreenStateChanged(int screenState) {
    }

    private boolean hasRtlSupport() {
        return this.mContext.getApplicationInfo().hasRtlSupport();
    }

    private boolean isRtlCompatibilityMode() {
        int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
        return targetSdkVersion < 17 || !hasRtlSupport();
    }

    private boolean needRtlPropertiesResolution() {
        return (this.mPrivateFlags2 & ALL_RTL_PROPERTIES_RESOLVED) != ALL_RTL_PROPERTIES_RESOLVED;
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
    }

    public boolean resolveLayoutDirection() {
        this.mPrivateFlags2 &= -49;
        if (hasRtlSupport()) {
            switch ((this.mPrivateFlags2 & 12) >> 2) {
                case 1:
                    this.mPrivateFlags2 |= 16;
                    break;
                case 2:
                    if (!canResolveLayoutDirection()) {
                        return false;
                    }
                    try {
                        if (!this.mParent.isLayoutDirectionResolved()) {
                            return false;
                        }
                        if (this.mParent.getLayoutDirection() == 1) {
                            this.mPrivateFlags2 |= 16;
                        }
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    }
                    break;
                    break;
                case 3:
                    if (1 == TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())) {
                        this.mPrivateFlags2 |= 16;
                    }
                    break;
            }
        }
        this.mPrivateFlags2 |= 32;
        return true;
    }

    public boolean canResolveLayoutDirection() {
        switch (getRawLayoutDirection()) {
            case 2:
                if (this.mParent != null) {
                    try {
                        return this.mParent.canResolveLayoutDirection();
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    }
                }
                return false;
            default:
                return true;
        }
    }

    public void resetResolvedLayoutDirection() {
        this.mPrivateFlags2 &= -49;
    }

    public boolean isLayoutDirectionInherited() {
        return getRawLayoutDirection() == 2;
    }

    public boolean isLayoutDirectionResolved() {
        return (this.mPrivateFlags2 & 32) == 32;
    }

    boolean isPaddingResolved() {
        return (this.mPrivateFlags2 & 536870912) == 536870912;
    }

    public void resolvePadding() {
        int resolvedLayoutDirection = getLayoutDirection();
        if (!isRtlCompatibilityMode()) {
            if (this.mBackground != null && (!this.mLeftPaddingDefined || !this.mRightPaddingDefined)) {
                Rect padding = sThreadLocal.get();
                if (padding == null) {
                    padding = new Rect();
                    sThreadLocal.set(padding);
                }
                this.mBackground.getPadding(padding);
                if (!this.mLeftPaddingDefined) {
                    this.mUserPaddingLeftInitial = padding.left;
                }
                if (!this.mRightPaddingDefined) {
                    this.mUserPaddingRightInitial = padding.right;
                }
            }
            switch (resolvedLayoutDirection) {
                case 1:
                    if (this.mUserPaddingStart != Integer.MIN_VALUE) {
                        this.mUserPaddingRight = this.mUserPaddingStart;
                    } else {
                        this.mUserPaddingRight = this.mUserPaddingRightInitial;
                    }
                    if (this.mUserPaddingEnd != Integer.MIN_VALUE) {
                        this.mUserPaddingLeft = this.mUserPaddingEnd;
                    } else {
                        this.mUserPaddingLeft = this.mUserPaddingLeftInitial;
                    }
                    break;
                default:
                    if (this.mUserPaddingStart != Integer.MIN_VALUE) {
                        this.mUserPaddingLeft = this.mUserPaddingStart;
                    } else {
                        this.mUserPaddingLeft = this.mUserPaddingLeftInitial;
                    }
                    if (this.mUserPaddingEnd != Integer.MIN_VALUE) {
                        this.mUserPaddingRight = this.mUserPaddingEnd;
                    } else {
                        this.mUserPaddingRight = this.mUserPaddingRightInitial;
                    }
                    break;
            }
            this.mUserPaddingBottom = this.mUserPaddingBottom >= 0 ? this.mUserPaddingBottom : this.mPaddingBottom;
        }
        internalSetPadding(this.mUserPaddingLeft, this.mPaddingTop, this.mUserPaddingRight, this.mUserPaddingBottom);
        onRtlPropertiesChanged(resolvedLayoutDirection);
        this.mPrivateFlags2 |= 536870912;
    }

    public void resetResolvedPadding() {
        resetResolvedPaddingInternal();
    }

    void resetResolvedPaddingInternal() {
        this.mPrivateFlags2 &= -536870913;
    }

    protected void onDetachedFromWindow() {
    }

    protected void onDetachedFromWindowInternal() {
        this.mPrivateFlags &= -67108865;
        this.mPrivateFlags3 &= -5;
        removeUnsetPressCallback();
        removeLongPressCallback();
        removePerformClickCallback();
        removeSendViewScrolledAccessibilityEventCallback();
        stopNestedScroll();
        jumpDrawablesToCurrentState();
        destroyDrawingCache();
        cleanupDraw();
        this.mCurrentAnimation = null;
    }

    private void cleanupDraw() {
        resetDisplayList();
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mViewRootImpl.cancelInvalidate(this);
        }
    }

    void invalidateInheritedLayoutMode(int layoutModeOfRoot) {
    }

    protected int getWindowAttachCount() {
        return this.mWindowAttachCount;
    }

    public IBinder getWindowToken() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mWindowToken;
        }
        return null;
    }

    public WindowId getWindowId() {
        if (this.mAttachInfo == null) {
            return null;
        }
        if (this.mAttachInfo.mWindowId == null) {
            try {
                this.mAttachInfo.mIWindowId = this.mAttachInfo.mSession.getWindowId(this.mAttachInfo.mWindowToken);
                this.mAttachInfo.mWindowId = new WindowId(this.mAttachInfo.mIWindowId);
            } catch (RemoteException e) {
            }
        }
        return this.mAttachInfo.mWindowId;
    }

    public IBinder getApplicationWindowToken() {
        AttachInfo ai = this.mAttachInfo;
        if (ai == null) {
            return null;
        }
        IBinder appWindowToken = ai.mPanelParentWindowToken;
        if (appWindowToken == null) {
            return ai.mWindowToken;
        }
        return appWindowToken;
    }

    public Display getDisplay() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mDisplay;
        }
        return null;
    }

    IWindowSession getWindowSession() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mSession;
        }
        return null;
    }

    void dispatchAttachedToWindow(AttachInfo info, int visibility) {
        this.mAttachInfo = info;
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().dispatchAttachedToWindow(info, visibility);
        }
        this.mWindowAttachCount++;
        this.mPrivateFlags |= 1024;
        if (this.mFloatingTreeObserver != null) {
            info.mTreeObserver.merge(this.mFloatingTreeObserver);
            this.mFloatingTreeObserver = null;
        }
        if ((this.mPrivateFlags & 524288) != 0) {
            this.mAttachInfo.mScrollContainers.add(this);
            this.mPrivateFlags |= 1048576;
        }
        performCollectViewAttributes(this.mAttachInfo, visibility);
        onAttachedToWindow();
        ListenerInfo li = this.mListenerInfo;
        CopyOnWriteArrayList<OnAttachStateChangeListener> listeners = li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewAttachedToWindow(this);
            }
        }
        int vis = info.mWindowVisibility;
        if (vis != 8) {
            onWindowVisibilityChanged(vis);
        }
        if ((this.mPrivateFlags & 1024) != 0) {
            refreshDrawableState();
        }
        needGlobalAttributesUpdate(false);
    }

    void dispatchDetachedFromWindow() {
        AttachInfo info = this.mAttachInfo;
        if (info != null) {
            int vis = info.mWindowVisibility;
            if (vis != 8) {
                onWindowVisibilityChanged(8);
            }
        }
        onDetachedFromWindow();
        onDetachedFromWindowInternal();
        ListenerInfo li = this.mListenerInfo;
        CopyOnWriteArrayList<OnAttachStateChangeListener> listeners = li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewDetachedFromWindow(this);
            }
        }
        if ((this.mPrivateFlags & 1048576) != 0) {
            this.mAttachInfo.mScrollContainers.remove(this);
            this.mPrivateFlags &= -1048577;
        }
        this.mAttachInfo = null;
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().dispatchDetachedFromWindow();
        }
    }

    public final void cancelPendingInputEvents() {
        dispatchCancelPendingInputEvents();
    }

    void dispatchCancelPendingInputEvents() {
        this.mPrivateFlags3 &= -17;
        onCancelPendingInputEvents();
        if ((this.mPrivateFlags3 & 16) != 16) {
            throw new SuperNotCalledException("View " + getClass().getSimpleName() + " did not call through to super.onCancelPendingInputEvents()");
        }
    }

    public void onCancelPendingInputEvents() {
        removePerformClickCallback();
        cancelLongPress();
        this.mPrivateFlags3 |= 16;
    }

    public void saveHierarchyState(SparseArray<Parcelable> container) {
        dispatchSaveInstanceState(container);
    }

    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        if (this.mID != -1 && (this.mViewFlags & 65536) == 0) {
            this.mPrivateFlags &= -131073;
            Parcelable state = onSaveInstanceState();
            if ((this.mPrivateFlags & 131072) == 0) {
                throw new IllegalStateException("Derived class did not call super.onSaveInstanceState()");
            }
            if (state != null) {
                container.put(this.mID, state);
            }
        }
    }

    protected Parcelable onSaveInstanceState() {
        this.mPrivateFlags |= 131072;
        return BaseSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(SparseArray<Parcelable> container) {
        dispatchRestoreInstanceState(container);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        Parcelable state;
        if (this.mID != -1 && (state = container.get(this.mID)) != null) {
            this.mPrivateFlags &= -131073;
            onRestoreInstanceState(state);
            if ((this.mPrivateFlags & 131072) == 0) {
                throw new IllegalStateException("Derived class did not call super.onRestoreInstanceState()");
            }
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        this.mPrivateFlags |= 131072;
        if (state != BaseSavedState.EMPTY_STATE && state != null) {
            throw new IllegalArgumentException("Wrong state class, expecting View State but received " + state.getClass().toString() + " instead. This usually happens when two views of different type have the same id in the same hierarchy. This view's id is " + ViewDebug.resolveId(this.mContext, getId()) + ". Make sure other views do not use the same id.");
        }
    }

    public long getDrawingTime() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mDrawingTime;
        }
        return 0L;
    }

    public void setDuplicateParentStateEnabled(boolean enabled) {
        setFlags(enabled ? 4194304 : 0, 4194304);
    }

    public boolean isDuplicateParentStateEnabled() {
        return (this.mViewFlags & 4194304) == 4194304;
    }

    public void setLayerType(int layerType, Paint paint) {
        if (layerType < 0 || layerType > 2) {
            throw new IllegalArgumentException("Layer type can only be one of: LAYER_TYPE_NONE, LAYER_TYPE_SOFTWARE or LAYER_TYPE_HARDWARE");
        }
        boolean typeChanged = this.mRenderNode.setLayerType(layerType);
        if (!typeChanged) {
            setLayerPaint(paint);
            return;
        }
        if (this.mLayerType == 1) {
            destroyDrawingCache();
        }
        this.mLayerType = layerType;
        boolean layerDisabled = this.mLayerType == 0;
        if (layerDisabled) {
            paint = null;
        } else if (paint == null) {
            paint = new Paint();
        }
        this.mLayerPaint = paint;
        this.mRenderNode.setLayerPaint(this.mLayerPaint);
        invalidateParentCaches();
        invalidate(true);
    }

    public void setLayerPaint(Paint paint) {
        int layerType = getLayerType();
        if (layerType != 0) {
            if (paint == null) {
                paint = new Paint();
            }
            this.mLayerPaint = paint;
            if (layerType == 2) {
                if (this.mRenderNode.setLayerPaint(this.mLayerPaint)) {
                    invalidateViewProperty(false, false);
                    return;
                }
                return;
            }
            invalidate();
        }
    }

    boolean hasStaticLayer() {
        return true;
    }

    public int getLayerType() {
        return this.mLayerType;
    }

    public void buildLayer() {
        if (this.mLayerType != 0) {
            AttachInfo attachInfo = this.mAttachInfo;
            if (attachInfo == null) {
                throw new IllegalStateException("This view must be attached to a window first");
            }
            if (getWidth() != 0 && getHeight() != 0) {
                switch (this.mLayerType) {
                    case 1:
                        buildDrawingCache(true);
                        return;
                    case 2:
                        updateDisplayListIfDirty();
                        if (attachInfo.mHardwareRenderer != null && this.mRenderNode.isValid()) {
                            attachInfo.mHardwareRenderer.buildLayer(this.mRenderNode);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        }
    }

    HardwareLayer getHardwareLayer() {
        return null;
    }

    protected void destroyHardwareResources() {
        resetDisplayList();
    }

    public void setDrawingCacheEnabled(boolean enabled) {
        this.mCachingFailed = false;
        setFlags(enabled ? 32768 : 0, 32768);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean isDrawingCacheEnabled() {
        return (this.mViewFlags & 32768) == 32768;
    }

    public void outputDirtyFlags(String indent, boolean clear, int clearMask) {
        Log.d(VIEW_LOG_TAG, indent + this + "             DIRTY(" + (this.mPrivateFlags & 6291456) + ") DRAWN(" + (this.mPrivateFlags & 32) + ") CACHE_VALID(" + (this.mPrivateFlags & 32768) + ") INVALIDATED(" + (this.mPrivateFlags & Integer.MIN_VALUE) + ")");
        if (clear) {
            this.mPrivateFlags &= clearMask;
        }
        if (this instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) this;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = parent.getChildAt(i);
                child.outputDirtyFlags(indent + "  ", clear, clearMask);
            }
        }
    }

    protected void dispatchGetDisplayList() {
    }

    public boolean canHaveDisplayList() {
        return (this.mAttachInfo == null || this.mAttachInfo.mHardwareRenderer == null) ? false : true;
    }

    private void updateDisplayListIfDirty() {
        RenderNode renderNode = this.mRenderNode;
        if (canHaveDisplayList()) {
            if ((this.mPrivateFlags & 32768) == 0 || !renderNode.isValid() || this.mRecreateDisplayList) {
                if (renderNode.isValid() && !this.mRecreateDisplayList) {
                    this.mPrivateFlags |= 32800;
                    this.mPrivateFlags &= -6291457;
                    dispatchGetDisplayList();
                    return;
                }
                this.mRecreateDisplayList = true;
                int width = this.mRight - this.mLeft;
                int height = this.mBottom - this.mTop;
                int layerType = getLayerType();
                HardwareCanvas canvas = renderNode.start(width, height);
                canvas.setHighContrastText(this.mAttachInfo.mHighContrastText);
                try {
                    HardwareLayer layer = getHardwareLayer();
                    if (layer != null && layer.isValid()) {
                        canvas.drawHardwareLayer(layer, 0.0f, 0.0f, this.mLayerPaint);
                    } else if (layerType == 1) {
                        buildDrawingCache(true);
                        Bitmap cache = getDrawingCache(true);
                        if (cache != null) {
                            canvas.drawBitmap(cache, 0.0f, 0.0f, this.mLayerPaint);
                        }
                    } else {
                        computeScroll();
                        canvas.translate(-this.mScrollX, -this.mScrollY);
                        this.mPrivateFlags |= 32800;
                        this.mPrivateFlags &= -6291457;
                        if ((this.mPrivateFlags & 128) == 128) {
                            dispatchDraw(canvas);
                            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                                this.mOverlay.getOverlayView().draw(canvas);
                            }
                        } else {
                            draw(canvas);
                        }
                    }
                    return;
                } finally {
                    renderNode.end(canvas);
                    setDisplayListProperties(renderNode);
                }
            }
            this.mPrivateFlags |= 32800;
            this.mPrivateFlags &= -6291457;
        }
    }

    public RenderNode getDisplayList() {
        updateDisplayListIfDirty();
        return this.mRenderNode;
    }

    private void resetDisplayList() {
        if (this.mRenderNode.isValid()) {
            this.mRenderNode.destroyDisplayListData();
        }
        if (this.mBackgroundRenderNode != null && this.mBackgroundRenderNode.isValid()) {
            this.mBackgroundRenderNode.destroyDisplayListData();
        }
    }

    public Bitmap getDrawingCache() {
        return getDrawingCache(false);
    }

    public Bitmap getDrawingCache(boolean autoScale) {
        if ((this.mViewFlags & 131072) == 131072) {
            return null;
        }
        if ((this.mViewFlags & 32768) == 32768) {
            buildDrawingCache(autoScale);
        }
        return autoScale ? this.mDrawingCache : this.mUnscaledDrawingCache;
    }

    public void destroyDrawingCache() {
        if (this.mDrawingCache != null) {
            this.mDrawingCache.recycle();
            this.mDrawingCache = null;
        }
        if (this.mUnscaledDrawingCache != null) {
            this.mUnscaledDrawingCache.recycle();
            this.mUnscaledDrawingCache = null;
        }
    }

    public void setDrawingCacheBackgroundColor(int color) {
        if (color != this.mDrawingCacheBackgroundColor) {
            this.mDrawingCacheBackgroundColor = color;
            this.mPrivateFlags &= -32769;
        }
    }

    public int getDrawingCacheBackgroundColor() {
        return this.mDrawingCacheBackgroundColor;
    }

    public void buildDrawingCache() {
        buildDrawingCache(false);
    }

    public void buildDrawingCache(boolean autoScale) {
        if ((this.mPrivateFlags & 32768) != 0) {
            if (autoScale) {
                if (this.mDrawingCache != null) {
                    return;
                }
            } else if (this.mUnscaledDrawingCache != null) {
                return;
            }
        }
        if (Trace.isTagEnabled(8L)) {
            Trace.traceBegin(8L, "buildDrawingCache/SW Layer for " + getClass().getSimpleName());
        }
        try {
            buildDrawingCacheImpl(autoScale);
        } finally {
            Trace.traceEnd(8L);
        }
    }

    private void buildDrawingCacheImpl(boolean autoScale) {
        Bitmap.Config quality;
        Canvas canvas;
        this.mCachingFailed = false;
        int width = this.mRight - this.mLeft;
        int height = this.mBottom - this.mTop;
        AttachInfo attachInfo = this.mAttachInfo;
        boolean scalingRequired = attachInfo != null && attachInfo.mScalingRequired;
        if (autoScale && scalingRequired) {
            width = (int) ((width * attachInfo.mApplicationScale) + 0.5f);
            height = (int) ((height * attachInfo.mApplicationScale) + 0.5f);
        }
        int drawingCacheBackgroundColor = this.mDrawingCacheBackgroundColor;
        boolean opaque = drawingCacheBackgroundColor != 0 || isOpaque();
        boolean use32BitCache = attachInfo != null && attachInfo.mUse32BitDrawingCache;
        long projectedBitmapSize = ((!opaque || use32BitCache) ? 4 : 2) * width * height;
        long drawingCacheSize = ViewConfiguration.get(this.mContext).getScaledMaximumDrawingCacheSize();
        if (width <= 0 || height <= 0 || projectedBitmapSize > drawingCacheSize) {
            if (width > 0 && height > 0) {
                Log.w(VIEW_LOG_TAG, "View too large to fit into drawing cache, needs " + projectedBitmapSize + " bytes, only " + drawingCacheSize + " available");
            }
            destroyDrawingCache();
            this.mCachingFailed = true;
            return;
        }
        boolean clear = true;
        Bitmap bitmap = autoScale ? this.mDrawingCache : this.mUnscaledDrawingCache;
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            if (!opaque) {
                int i = this.mViewFlags & DRAWING_CACHE_QUALITY_MASK;
                quality = Bitmap.Config.ARGB_8888;
            } else {
                quality = use32BitCache ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            }
            if (bitmap != null) {
                bitmap.recycle();
            }
            try {
                bitmap = Bitmap.createBitmap(this.mResources.getDisplayMetrics(), width, height, quality);
                bitmap.setDensity(getResources().getDisplayMetrics().densityDpi);
                if (autoScale) {
                    this.mDrawingCache = bitmap;
                } else {
                    this.mUnscaledDrawingCache = bitmap;
                }
                if (opaque && use32BitCache) {
                    bitmap.setHasAlpha(false);
                }
                clear = drawingCacheBackgroundColor != 0;
            } catch (OutOfMemoryError e) {
                if (autoScale) {
                    this.mDrawingCache = null;
                } else {
                    this.mUnscaledDrawingCache = null;
                }
                this.mCachingFailed = true;
                return;
            }
        }
        if (attachInfo != null) {
            canvas = attachInfo.mCanvas;
            if (canvas == null) {
                canvas = new Canvas();
            }
            canvas.setBitmap(bitmap);
            attachInfo.mCanvas = null;
        } else {
            canvas = new Canvas(bitmap);
        }
        if (clear) {
            bitmap.eraseColor(drawingCacheBackgroundColor);
        }
        computeScroll();
        int restoreCount = canvas.save();
        if (autoScale && scalingRequired) {
            float scale = attachInfo.mApplicationScale;
            canvas.scale(scale, scale);
        }
        canvas.translate(-this.mScrollX, -this.mScrollY);
        this.mPrivateFlags |= 32;
        if (this.mAttachInfo == null || !this.mAttachInfo.mHardwareAccelerated || this.mLayerType != 0) {
            this.mPrivateFlags |= 32768;
        }
        if ((this.mPrivateFlags & 128) == 128) {
            this.mPrivateFlags &= -6291457;
            dispatchDraw(canvas);
            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                this.mOverlay.getOverlayView().draw(canvas);
            }
        } else {
            draw(canvas);
        }
        canvas.restoreToCount(restoreCount);
        canvas.setBitmap(null);
        if (attachInfo != null) {
            attachInfo.mCanvas = canvas;
        }
    }

    Bitmap createSnapshot(Bitmap.Config quality, int backgroundColor, boolean skipChildren) {
        Canvas canvas;
        int width = this.mRight - this.mLeft;
        int height = this.mBottom - this.mTop;
        AttachInfo attachInfo = this.mAttachInfo;
        float scale = attachInfo != null ? attachInfo.mApplicationScale : 1.0f;
        int width2 = (int) ((width * scale) + 0.5f);
        int height2 = (int) ((height * scale) + 0.5f);
        DisplayMetrics displayMetrics = this.mResources.getDisplayMetrics();
        if (width2 <= 0) {
            width2 = 1;
        }
        if (height2 <= 0) {
            height2 = 1;
        }
        Bitmap bitmap = Bitmap.createBitmap(displayMetrics, width2, height2, quality);
        if (bitmap == null) {
            throw new OutOfMemoryError();
        }
        Resources resources = getResources();
        if (resources != null) {
            bitmap.setDensity(resources.getDisplayMetrics().densityDpi);
        }
        if (attachInfo != null) {
            canvas = attachInfo.mCanvas;
            if (canvas == null) {
                canvas = new Canvas();
            }
            canvas.setBitmap(bitmap);
            attachInfo.mCanvas = null;
        } else {
            canvas = new Canvas(bitmap);
        }
        if (((-16777216) & backgroundColor) != 0) {
            bitmap.eraseColor(backgroundColor);
        }
        computeScroll();
        int restoreCount = canvas.save();
        canvas.scale(scale, scale);
        canvas.translate(-this.mScrollX, -this.mScrollY);
        int flags = this.mPrivateFlags;
        this.mPrivateFlags &= -6291457;
        if ((this.mPrivateFlags & 128) == 128) {
            dispatchDraw(canvas);
            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                this.mOverlay.getOverlayView().draw(canvas);
            }
        } else {
            draw(canvas);
        }
        this.mPrivateFlags = flags;
        canvas.restoreToCount(restoreCount);
        canvas.setBitmap(null);
        if (attachInfo != null) {
            attachInfo.mCanvas = canvas;
        }
        return bitmap;
    }

    public boolean isInEditMode() {
        return false;
    }

    protected boolean isPaddingOffsetRequired() {
        return false;
    }

    protected int getLeftPaddingOffset() {
        return 0;
    }

    protected int getRightPaddingOffset() {
        return 0;
    }

    protected int getTopPaddingOffset() {
        return 0;
    }

    protected int getBottomPaddingOffset() {
        return 0;
    }

    protected int getFadeTop(boolean offsetRequired) {
        int top = this.mPaddingTop;
        return offsetRequired ? top + getTopPaddingOffset() : top;
    }

    protected int getFadeHeight(boolean offsetRequired) {
        int padding = this.mPaddingTop;
        if (offsetRequired) {
            padding += getTopPaddingOffset();
        }
        return ((this.mBottom - this.mTop) - this.mPaddingBottom) - padding;
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean isHardwareAccelerated() {
        return this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerated;
    }

    public void setClipBounds(Rect clipBounds) {
        if (clipBounds != this.mClipBounds) {
            if (clipBounds == null || !clipBounds.equals(this.mClipBounds)) {
                if (clipBounds != null) {
                    if (this.mClipBounds == null) {
                        this.mClipBounds = new Rect(clipBounds);
                    } else {
                        this.mClipBounds.set(clipBounds);
                    }
                } else {
                    this.mClipBounds = null;
                }
                this.mRenderNode.setClipBounds(this.mClipBounds);
                invalidateViewProperty(false, false);
            }
        }
    }

    public Rect getClipBounds() {
        if (this.mClipBounds != null) {
            return new Rect(this.mClipBounds);
        }
        return null;
    }

    private boolean drawAnimation(ViewGroup parent, long drawingTime, Animation a, boolean scalingRequired) {
        Transformation invalidationTransform;
        int flags = parent.mGroupFlags;
        boolean initialized = a.isInitialized();
        if (!initialized) {
            a.initialize(this.mRight - this.mLeft, this.mBottom - this.mTop, parent.getWidth(), parent.getHeight());
            a.initializeInvalidateRegion(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
            if (this.mAttachInfo != null) {
                a.setListenerHandler(this.mAttachInfo.mHandler);
            }
            onAnimationStart();
        }
        Transformation t = parent.getChildTransformation();
        boolean more = a.getTransformation(drawingTime, t, 1.0f);
        if (scalingRequired && this.mAttachInfo.mApplicationScale != 1.0f) {
            if (parent.mInvalidationTransformation == null) {
                parent.mInvalidationTransformation = new Transformation();
            }
            invalidationTransform = parent.mInvalidationTransformation;
            a.getTransformation(drawingTime, invalidationTransform, 1.0f);
        } else {
            invalidationTransform = t;
        }
        if (more) {
            if (!a.willChangeBounds()) {
                if ((flags & 144) == 128) {
                    parent.mGroupFlags |= 4;
                } else if ((flags & 4) == 0) {
                    parent.mPrivateFlags |= 64;
                    parent.invalidate(this.mLeft, this.mTop, this.mRight, this.mBottom);
                }
            } else {
                if (parent.mInvalidateRegion == null) {
                    parent.mInvalidateRegion = new RectF();
                }
                RectF region = parent.mInvalidateRegion;
                a.getInvalidateRegion(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop, region, invalidationTransform);
                parent.mPrivateFlags |= 64;
                int left = this.mLeft + ((int) region.left);
                int top = this.mTop + ((int) region.top);
                parent.invalidate(left, top, ((int) (region.width() + 0.5f)) + left, ((int) (region.height() + 0.5f)) + top);
            }
        }
        return more;
    }

    void setDisplayListProperties(RenderNode renderNode) {
        int transformType;
        if (renderNode != null) {
            renderNode.setHasOverlappingRendering(hasOverlappingRendering());
            if (this.mParent instanceof ViewGroup) {
                renderNode.setClipToBounds((((ViewGroup) this.mParent).mGroupFlags & 1) != 0);
            }
            float alpha = 1.0f;
            if ((this.mParent instanceof ViewGroup) && (((ViewGroup) this.mParent).mGroupFlags & 2048) != 0) {
                ViewGroup parentVG = (ViewGroup) this.mParent;
                Transformation t = parentVG.getChildTransformation();
                if (parentVG.getChildStaticTransformation(this, t) && (transformType = t.getTransformationType()) != 0) {
                    if ((transformType & 1) != 0) {
                        alpha = t.getAlpha();
                    }
                    if ((transformType & 2) != 0) {
                        renderNode.setStaticMatrix(t.getMatrix());
                    }
                }
            }
            if (this.mTransformationInfo == null) {
                if (alpha < 1.0f) {
                    renderNode.setAlpha(alpha);
                }
            } else {
                float alpha2 = alpha * getFinalAlpha();
                if (alpha2 < 1.0f) {
                    int multipliedAlpha = (int) (255.0f * alpha2);
                    if (onSetAlpha(multipliedAlpha)) {
                        alpha2 = 1.0f;
                    }
                }
                renderNode.setAlpha(alpha2);
            }
        }
    }

    boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
        boolean caching;
        Paint cachePaint;
        boolean usingRenderNodeProperties = this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerated;
        boolean more = false;
        boolean childHasIdentityMatrix = hasIdentityMatrix();
        int flags = parent.mGroupFlags;
        if ((flags & 256) == 256) {
            parent.getChildTransformation().clear();
            parent.mGroupFlags &= -257;
        }
        Transformation transformToApply = null;
        boolean concatMatrix = false;
        boolean scalingRequired = false;
        int layerType = getLayerType();
        boolean hardwareAccelerated = canvas.isHardwareAccelerated();
        if ((32768 & flags) != 0 || (flags & 16384) != 0) {
            caching = true;
            if (this.mAttachInfo != null) {
                scalingRequired = this.mAttachInfo.mScalingRequired;
            }
        } else {
            caching = layerType != 0 || hardwareAccelerated;
        }
        Animation a = getAnimation();
        if (a != null) {
            more = drawAnimation(parent, drawingTime, a, scalingRequired);
            concatMatrix = a.willChangeTransformationMatrix();
            if (concatMatrix) {
                this.mPrivateFlags3 |= 1;
            }
            transformToApply = parent.getChildTransformation();
        } else {
            if ((this.mPrivateFlags3 & 1) != 0) {
                this.mRenderNode.setAnimationMatrix(null);
                this.mPrivateFlags3 &= -2;
            }
            if (!usingRenderNodeProperties && (flags & 2048) != 0) {
                Transformation t = parent.getChildTransformation();
                boolean hasTransform = parent.getChildStaticTransformation(this, t);
                if (hasTransform) {
                    int transformType = t.getTransformationType();
                    transformToApply = transformType != 0 ? t : null;
                    concatMatrix = (transformType & 2) != 0;
                }
            }
        }
        boolean concatMatrix2 = concatMatrix | (!childHasIdentityMatrix);
        this.mPrivateFlags |= 32;
        if (!concatMatrix2 && (flags & 2049) == 1 && canvas.quickReject(this.mLeft, this.mTop, this.mRight, this.mBottom, Canvas.EdgeType.BW) && (this.mPrivateFlags & 64) == 0) {
            this.mPrivateFlags2 |= 268435456;
        } else {
            this.mPrivateFlags2 &= -268435457;
            if (hardwareAccelerated) {
                this.mRecreateDisplayList = (this.mPrivateFlags & Integer.MIN_VALUE) == Integer.MIN_VALUE;
                this.mPrivateFlags &= Integer.MAX_VALUE;
            }
            RenderNode renderNode = null;
            Bitmap cache = null;
            boolean hasDisplayList = false;
            if (caching) {
                if (!hardwareAccelerated) {
                    if (layerType != 0) {
                        layerType = 1;
                        buildDrawingCache(true);
                    }
                    cache = getDrawingCache(true);
                } else {
                    switch (layerType) {
                        case 0:
                            hasDisplayList = canHaveDisplayList();
                            break;
                        case 1:
                            if (usingRenderNodeProperties) {
                                hasDisplayList = canHaveDisplayList();
                            } else {
                                buildDrawingCache(true);
                                cache = getDrawingCache(true);
                            }
                            break;
                        case 2:
                            if (usingRenderNodeProperties) {
                                hasDisplayList = canHaveDisplayList();
                            }
                            break;
                    }
                }
            }
            boolean usingRenderNodeProperties2 = usingRenderNodeProperties & hasDisplayList;
            if (usingRenderNodeProperties2) {
                renderNode = getDisplayList();
                if (!renderNode.isValid()) {
                    renderNode = null;
                    hasDisplayList = false;
                    usingRenderNodeProperties2 = false;
                }
            }
            int sx = 0;
            int sy = 0;
            if (!hasDisplayList) {
                computeScroll();
                sx = this.mScrollX;
                sy = this.mScrollY;
            }
            boolean hasNoCache = cache == null || hasDisplayList;
            boolean offsetForScroll = (cache != null || hasDisplayList || layerType == 2) ? false : true;
            int restoreTo = -1;
            if (!usingRenderNodeProperties2 || transformToApply != null) {
                restoreTo = canvas.save();
            }
            if (offsetForScroll) {
                canvas.translate(this.mLeft - sx, this.mTop - sy);
            } else {
                if (!usingRenderNodeProperties2) {
                    canvas.translate(this.mLeft, this.mTop);
                }
                if (scalingRequired) {
                    if (usingRenderNodeProperties2) {
                        restoreTo = canvas.save();
                    }
                    float scale = 1.0f / this.mAttachInfo.mApplicationScale;
                    canvas.scale(scale, scale);
                }
            }
            float alpha = usingRenderNodeProperties2 ? 1.0f : getAlpha() * getTransitionAlpha();
            if (transformToApply != null || alpha < 1.0f || !hasIdentityMatrix() || (this.mPrivateFlags3 & 2) == 2) {
                if (transformToApply != null || !childHasIdentityMatrix) {
                    int transX = 0;
                    int transY = 0;
                    if (offsetForScroll) {
                        transX = -sx;
                        transY = -sy;
                    }
                    if (transformToApply != null) {
                        if (concatMatrix2) {
                            if (usingRenderNodeProperties2) {
                                renderNode.setAnimationMatrix(transformToApply.getMatrix());
                            } else {
                                canvas.translate(-transX, -transY);
                                canvas.concat(transformToApply.getMatrix());
                                canvas.translate(transX, transY);
                            }
                            parent.mGroupFlags |= 256;
                        }
                        float transformAlpha = transformToApply.getAlpha();
                        if (transformAlpha < 1.0f) {
                            alpha *= transformAlpha;
                            parent.mGroupFlags |= 256;
                        }
                    }
                    if (!childHasIdentityMatrix && !usingRenderNodeProperties2) {
                        canvas.translate(-transX, -transY);
                        canvas.concat(getMatrix());
                        canvas.translate(transX, transY);
                    }
                }
                if (alpha < 1.0f || (this.mPrivateFlags3 & 2) == 2) {
                    if (alpha < 1.0f) {
                        this.mPrivateFlags3 |= 2;
                    } else {
                        this.mPrivateFlags3 &= -3;
                    }
                    parent.mGroupFlags |= 256;
                    if (hasNoCache) {
                        int multipliedAlpha = (int) (255.0f * alpha);
                        if (onSetAlpha(multipliedAlpha)) {
                            this.mPrivateFlags |= 262144;
                        } else {
                            int layerFlags = 4;
                            if ((flags & 1) != 0 || layerType != 0) {
                                layerFlags = 4 | 16;
                            }
                            if (usingRenderNodeProperties2) {
                                renderNode.setAlpha(getAlpha() * alpha * getTransitionAlpha());
                            } else if (layerType == 0) {
                                int scrollX = hasDisplayList ? 0 : sx;
                                int scrollY = hasDisplayList ? 0 : sy;
                                canvas.saveLayerAlpha(scrollX, scrollY, (this.mRight - this.mLeft) + scrollX, (this.mBottom - this.mTop) + scrollY, multipliedAlpha, layerFlags);
                            }
                        }
                    }
                }
            } else if ((this.mPrivateFlags & 262144) == 262144) {
                onSetAlpha(255);
                this.mPrivateFlags &= -262145;
            }
            if (!usingRenderNodeProperties2) {
                if ((flags & 1) == 1 && cache == null) {
                    if (offsetForScroll) {
                        canvas.clipRect(sx, sy, (this.mRight - this.mLeft) + sx, (this.mBottom - this.mTop) + sy);
                    } else if (!scalingRequired || cache == null) {
                        canvas.clipRect(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
                    } else {
                        canvas.clipRect(0, 0, cache.getWidth(), cache.getHeight());
                    }
                }
                if (this.mClipBounds != null) {
                    canvas.clipRect(this.mClipBounds);
                }
            }
            if (!usingRenderNodeProperties2 && hasDisplayList) {
                renderNode = getDisplayList();
                if (!renderNode.isValid()) {
                    renderNode = null;
                    hasDisplayList = false;
                }
            }
            if (hasNoCache) {
                boolean layerRendered = false;
                if (layerType == 2 && !usingRenderNodeProperties2) {
                    HardwareLayer layer = getHardwareLayer();
                    if (layer != null && layer.isValid()) {
                        int restoreAlpha = this.mLayerPaint.getAlpha();
                        this.mLayerPaint.setAlpha((int) (255.0f * alpha));
                        ((HardwareCanvas) canvas).drawHardwareLayer(layer, 0.0f, 0.0f, this.mLayerPaint);
                        this.mLayerPaint.setAlpha(restoreAlpha);
                        layerRendered = true;
                    } else {
                        int scrollX2 = hasDisplayList ? 0 : sx;
                        int scrollY2 = hasDisplayList ? 0 : sy;
                        canvas.saveLayer(scrollX2, scrollY2, (this.mRight + scrollX2) - this.mLeft, (this.mBottom + scrollY2) - this.mTop, this.mLayerPaint, 20);
                    }
                }
                if (!layerRendered) {
                    if (!hasDisplayList) {
                        if ((this.mPrivateFlags & 128) == 128) {
                            this.mPrivateFlags &= -6291457;
                            dispatchDraw(canvas);
                        } else {
                            draw(canvas);
                        }
                    } else {
                        this.mPrivateFlags &= -6291457;
                        ((HardwareCanvas) canvas).drawRenderNode(renderNode, null, flags);
                    }
                }
            } else if (cache != null) {
                this.mPrivateFlags &= -6291457;
                int restoreAlpha2 = 0;
                if (layerType == 0) {
                    cachePaint = parent.mCachePaint;
                    if (cachePaint == null) {
                        cachePaint = new Paint();
                        cachePaint.setDither(false);
                        parent.mCachePaint = cachePaint;
                    }
                } else {
                    cachePaint = this.mLayerPaint;
                    restoreAlpha2 = this.mLayerPaint.getAlpha();
                }
                cachePaint.setAlpha((int) (255.0f * alpha));
                canvas.drawBitmap(cache, 0.0f, 0.0f, cachePaint);
                cachePaint.setAlpha(restoreAlpha2);
            }
            if (restoreTo >= 0) {
                canvas.restoreToCount(restoreTo);
            }
            if (a != null && !more) {
                if (!hardwareAccelerated && !a.getFillAfter()) {
                    onSetAlpha(255);
                }
                parent.finishAnimatingView(this, a);
            }
            if (more && hardwareAccelerated && a.hasAlpha() && (this.mPrivateFlags & 262144) == 262144) {
                invalidate(true);
            }
            this.mRecreateDisplayList = false;
        }
        return more;
    }

    public void draw(Canvas canvas) {
        int privateFlags = this.mPrivateFlags;
        boolean dirtyOpaque = (6291456 & privateFlags) == 4194304 && (this.mAttachInfo == null || !this.mAttachInfo.mIgnoreDirtyState);
        this.mPrivateFlags = ((-6291457) & privateFlags) | 32;
        if (!dirtyOpaque) {
            drawBackground(canvas);
        }
        int viewFlags = this.mViewFlags;
        boolean horizontalEdges = (viewFlags & 4096) != 0;
        boolean verticalEdges = (viewFlags & 8192) != 0;
        if (!verticalEdges && !horizontalEdges) {
            if (!dirtyOpaque) {
                onDraw(canvas);
            }
            dispatchDraw(canvas);
            onDrawScrollBars(canvas);
            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                this.mOverlay.getOverlayView().dispatchDraw(canvas);
                return;
            }
            return;
        }
        boolean drawTop = false;
        boolean drawBottom = false;
        boolean drawLeft = false;
        boolean drawRight = false;
        float topFadeStrength = 0.0f;
        float bottomFadeStrength = 0.0f;
        float leftFadeStrength = 0.0f;
        float rightFadeStrength = 0.0f;
        int paddingLeft = this.mPaddingLeft;
        boolean offsetRequired = isPaddingOffsetRequired();
        if (offsetRequired) {
            paddingLeft += getLeftPaddingOffset();
        }
        int left = this.mScrollX + paddingLeft;
        int right = (((this.mRight + left) - this.mLeft) - this.mPaddingRight) - paddingLeft;
        int top = this.mScrollY + getFadeTop(offsetRequired);
        int bottom = top + getFadeHeight(offsetRequired);
        if (offsetRequired) {
            right += getRightPaddingOffset();
            bottom += getBottomPaddingOffset();
        }
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        float fadeHeight = scrollabilityCache.fadingEdgeLength;
        int length = (int) fadeHeight;
        if (verticalEdges && top + length > bottom - length) {
            length = (bottom - top) / 2;
        }
        if (horizontalEdges && left + length > right - length) {
            length = (right - left) / 2;
        }
        if (verticalEdges) {
            topFadeStrength = Math.max(0.0f, Math.min(1.0f, getTopFadingEdgeStrength()));
            drawTop = topFadeStrength * fadeHeight > 1.0f;
            bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, getBottomFadingEdgeStrength()));
            drawBottom = bottomFadeStrength * fadeHeight > 1.0f;
        }
        if (horizontalEdges) {
            leftFadeStrength = Math.max(0.0f, Math.min(1.0f, getLeftFadingEdgeStrength()));
            drawLeft = leftFadeStrength * fadeHeight > 1.0f;
            rightFadeStrength = Math.max(0.0f, Math.min(1.0f, getRightFadingEdgeStrength()));
            drawRight = rightFadeStrength * fadeHeight > 1.0f;
        }
        int saveCount = canvas.getSaveCount();
        int solidColor = getSolidColor();
        if (solidColor == 0) {
            if (drawTop) {
                canvas.saveLayer(left, top, right, top + length, null, 4);
            }
            if (drawBottom) {
                canvas.saveLayer(left, bottom - length, right, bottom, null, 4);
            }
            if (drawLeft) {
                canvas.saveLayer(left, top, left + length, bottom, null, 4);
            }
            if (drawRight) {
                canvas.saveLayer(right - length, top, right, bottom, null, 4);
            }
        } else {
            scrollabilityCache.setFadeColor(solidColor);
        }
        if (!dirtyOpaque) {
            onDraw(canvas);
        }
        dispatchDraw(canvas);
        Paint p = scrollabilityCache.paint;
        Matrix matrix = scrollabilityCache.matrix;
        Shader fade = scrollabilityCache.shader;
        if (drawTop) {
            matrix.setScale(1.0f, fadeHeight * topFadeStrength);
            matrix.postTranslate(left, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            canvas.drawRect(left, top, right, top + length, p);
        }
        if (drawBottom) {
            matrix.setScale(1.0f, fadeHeight * bottomFadeStrength);
            matrix.postRotate(180.0f);
            matrix.postTranslate(left, bottom);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            canvas.drawRect(left, bottom - length, right, bottom, p);
        }
        if (drawLeft) {
            matrix.setScale(1.0f, fadeHeight * leftFadeStrength);
            matrix.postRotate(-90.0f);
            matrix.postTranslate(left, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            canvas.drawRect(left, top, left + length, bottom, p);
        }
        if (drawRight) {
            matrix.setScale(1.0f, fadeHeight * rightFadeStrength);
            matrix.postRotate(90.0f);
            matrix.postTranslate(right, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            canvas.drawRect(right - length, top, right, bottom, p);
        }
        canvas.restoreToCount(saveCount);
        onDrawScrollBars(canvas);
        if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
            this.mOverlay.getOverlayView().dispatchDraw(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        Drawable background = this.mBackground;
        if (background != null) {
            if (this.mBackgroundSizeChanged) {
                background.setBounds(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
                this.mBackgroundSizeChanged = false;
                rebuildOutline();
            }
            if (canvas.isHardwareAccelerated() && this.mAttachInfo != null && this.mAttachInfo.mHardwareRenderer != null) {
                this.mBackgroundRenderNode = getDrawableRenderNode(background, this.mBackgroundRenderNode);
                RenderNode renderNode = this.mBackgroundRenderNode;
                if (renderNode != null && renderNode.isValid()) {
                    setBackgroundRenderNodeProperties(renderNode);
                    ((HardwareCanvas) canvas).drawRenderNode(renderNode);
                    return;
                }
            }
            int scrollX = this.mScrollX;
            int scrollY = this.mScrollY;
            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
                return;
            }
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.translate(-scrollX, -scrollY);
        }
    }

    private void setBackgroundRenderNodeProperties(RenderNode renderNode) {
        renderNode.setTranslationX(this.mScrollX);
        renderNode.setTranslationY(this.mScrollY);
    }

    private RenderNode getDrawableRenderNode(Drawable drawable, RenderNode renderNode) {
        if (renderNode == null) {
            renderNode = RenderNode.create(drawable.getClass().getName(), this);
        }
        Rect bounds = drawable.getBounds();
        int width = bounds.width();
        int height = bounds.height();
        HardwareCanvas canvas = renderNode.start(width, height);
        canvas.translate(-bounds.left, -bounds.top);
        try {
            drawable.draw(canvas);
            renderNode.end(canvas);
            renderNode.setLeftTopRightBottom(bounds.left, bounds.top, bounds.right, bounds.bottom);
            renderNode.setProjectBackwards(drawable.isProjected());
            renderNode.setProjectionReceiver(true);
            renderNode.setClipToBounds(false);
            return renderNode;
        } catch (Throwable th) {
            renderNode.end(canvas);
            throw th;
        }
    }

    public ViewOverlay getOverlay() {
        if (this.mOverlay == null) {
            this.mOverlay = new ViewOverlay(this.mContext, this);
        }
        return this.mOverlay;
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public int getSolidColor() {
        return 0;
    }

    private static String printFlags(int flags) {
        String output = ProxyInfo.LOCAL_EXCL_LIST;
        int numFlags = 0;
        if ((flags & 1) == 1) {
            output = ProxyInfo.LOCAL_EXCL_LIST + "TAKES_FOCUS";
            numFlags = 0 + 1;
        }
        switch (flags & 12) {
            case 4:
                if (numFlags > 0) {
                    output = output + " ";
                }
                return output + "INVISIBLE";
            case 8:
                if (numFlags > 0) {
                    output = output + " ";
                }
                return output + "GONE";
            default:
                return output;
        }
    }

    private static String printPrivateFlags(int privateFlags) {
        String output = ProxyInfo.LOCAL_EXCL_LIST;
        int numFlags = 0;
        if ((privateFlags & 1) == 1) {
            output = ProxyInfo.LOCAL_EXCL_LIST + "WANTS_FOCUS";
            numFlags = 0 + 1;
        }
        if ((privateFlags & 2) == 2) {
            if (numFlags > 0) {
                output = output + " ";
            }
            output = output + "FOCUSED";
            numFlags++;
        }
        if ((privateFlags & 4) == 4) {
            if (numFlags > 0) {
                output = output + " ";
            }
            output = output + "SELECTED";
            numFlags++;
        }
        if ((privateFlags & 8) == 8) {
            if (numFlags > 0) {
                output = output + " ";
            }
            output = output + "IS_ROOT_NAMESPACE";
            numFlags++;
        }
        if ((privateFlags & 16) == 16) {
            if (numFlags > 0) {
                output = output + " ";
            }
            output = output + "HAS_BOUNDS";
            numFlags++;
        }
        if ((privateFlags & 32) == 32) {
            if (numFlags > 0) {
                output = output + " ";
            }
            return output + "DRAWN";
        }
        return output;
    }

    public boolean isLayoutRequested() {
        return (this.mPrivateFlags & 4096) == 4096;
    }

    public static boolean isLayoutModeOptical(Object o) {
        return (o instanceof ViewGroup) && ((ViewGroup) o).isLayoutModeOptical();
    }

    private boolean setOpticalFrame(int left, int top, int right, int bottom) {
        Insets parentInsets = this.mParent instanceof View ? ((View) this.mParent).getOpticalInsets() : Insets.NONE;
        Insets childInsets = getOpticalInsets();
        return setFrame((parentInsets.left + left) - childInsets.left, (parentInsets.top + top) - childInsets.top, parentInsets.left + right + childInsets.right, parentInsets.top + bottom + childInsets.bottom);
    }

    public void layout(int l, int t, int r, int b) {
        if ((this.mPrivateFlags3 & 8) != 0) {
            onMeasure(this.mOldWidthMeasureSpec, this.mOldHeightMeasureSpec);
            this.mPrivateFlags3 &= -9;
        }
        int oldL = this.mLeft;
        int oldT = this.mTop;
        int oldB = this.mBottom;
        int oldR = this.mRight;
        boolean changed = isLayoutModeOptical(this.mParent) ? setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
        if (changed || (this.mPrivateFlags & 8192) == 8192) {
            onLayout(changed, l, t, r, b);
            this.mPrivateFlags &= -8193;
            ListenerInfo li = this.mListenerInfo;
            if (li != null && li.mOnLayoutChangeListeners != null) {
                ArrayList<OnLayoutChangeListener> listenersCopy = (ArrayList) li.mOnLayoutChangeListeners.clone();
                int numListeners = listenersCopy.size();
                for (int i = 0; i < numListeners; i++) {
                    listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
                }
            }
        }
        this.mPrivateFlags &= -4097;
        this.mPrivateFlags3 |= 4;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean changed = false;
        if (this.mLeft != left || this.mRight != right || this.mTop != top || this.mBottom != bottom) {
            changed = true;
            int drawn = this.mPrivateFlags & 32;
            int oldWidth = this.mRight - this.mLeft;
            int oldHeight = this.mBottom - this.mTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            boolean sizeChanged = (newWidth == oldWidth && newHeight == oldHeight) ? false : true;
            invalidate(sizeChanged);
            this.mLeft = left;
            this.mTop = top;
            this.mRight = right;
            this.mBottom = bottom;
            this.mRenderNode.setLeftTopRightBottom(this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mPrivateFlags |= 16;
            if (sizeChanged) {
                sizeChange(newWidth, newHeight, oldWidth, oldHeight);
            }
            if ((this.mViewFlags & 12) == 0 || this.mGhostView != null) {
                this.mPrivateFlags |= 32;
                invalidate(sizeChanged);
                invalidateParentCaches();
            }
            this.mPrivateFlags |= drawn;
            this.mBackgroundSizeChanged = true;
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
        return changed;
    }

    public void setLeftTopRightBottom(int left, int top, int right, int bottom) {
        setFrame(left, top, right, bottom);
    }

    private void sizeChange(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().setRight(newWidth);
            this.mOverlay.getOverlayView().setBottom(newHeight);
        }
        rebuildOutline();
    }

    protected void onFinishInflate() {
    }

    public Resources getResources() {
        return this.mResources;
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (verifyDrawable(drawable)) {
            Rect dirty = drawable.getDirtyBounds();
            int scrollX = this.mScrollX;
            int scrollY = this.mScrollY;
            invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
            rebuildOutline();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (verifyDrawable(who) && what != null) {
            long delay = when - SystemClock.uptimeMillis();
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mViewRootImpl.mChoreographer.postCallbackDelayed(1, what, who, Choreographer.subtractFrameDelay(delay));
            } else {
                ViewRootImpl.getRunQueue().postDelayed(what, delay);
            }
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (verifyDrawable(who) && what != null) {
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, what, who);
            }
            ViewRootImpl.getRunQueue().removeCallbacks(what);
        }
    }

    public void unscheduleDrawable(Drawable who) {
        if (this.mAttachInfo != null && who != null) {
            this.mAttachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, null, who);
        }
    }

    protected void resolveDrawables() {
        if (isLayoutDirectionResolved() || getRawLayoutDirection() != 2) {
            int layoutDirection = isLayoutDirectionResolved() ? getLayoutDirection() : getRawLayoutDirection();
            if (this.mBackground != null) {
                this.mBackground.setLayoutDirection(layoutDirection);
            }
            this.mPrivateFlags2 |= 1073741824;
            onResolveDrawables(layoutDirection);
        }
    }

    boolean areDrawablesResolved() {
        return (this.mPrivateFlags2 & 1073741824) == 1073741824;
    }

    public void onResolveDrawables(int layoutDirection) {
    }

    protected void resetResolvedDrawables() {
        resetResolvedDrawablesInternal();
    }

    void resetResolvedDrawablesInternal() {
        this.mPrivateFlags2 &= -1073741825;
    }

    protected boolean verifyDrawable(Drawable who) {
        return who == this.mBackground;
    }

    protected void drawableStateChanged() {
        Drawable d = this.mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        if (this.mStateListAnimator != null) {
            this.mStateListAnimator.setState(getDrawableState());
        }
    }

    public void drawableHotspotChanged(float x, float y) {
        if (this.mBackground != null) {
            this.mBackground.setHotspot(x, y);
        }
        dispatchDrawableHotspotChanged(x, y);
    }

    public void dispatchDrawableHotspotChanged(float x, float y) {
    }

    public void refreshDrawableState() {
        this.mPrivateFlags |= 1024;
        drawableStateChanged();
        ViewParent parent = this.mParent;
        if (parent != null) {
            parent.childDrawableStateChanged(this);
        }
    }

    public final int[] getDrawableState() {
        if (this.mDrawableState != null && (this.mPrivateFlags & 1024) == 0) {
            return this.mDrawableState;
        }
        this.mDrawableState = onCreateDrawableState(0);
        this.mPrivateFlags &= -1025;
        return this.mDrawableState;
    }

    protected int[] onCreateDrawableState(int extraSpace) {
        int[] fullState;
        if ((this.mViewFlags & 4194304) == 4194304 && (this.mParent instanceof View)) {
            return ((View) this.mParent).onCreateDrawableState(extraSpace);
        }
        int privateFlags = this.mPrivateFlags;
        int viewStateIndex = (privateFlags & 16384) != 0 ? 0 | 16 : 0;
        if ((this.mViewFlags & 32) == 0) {
            viewStateIndex |= 8;
        }
        if (isFocused()) {
            viewStateIndex |= 4;
        }
        if ((privateFlags & 4) != 0) {
            viewStateIndex |= 2;
        }
        if (hasWindowFocus()) {
            viewStateIndex |= 1;
        }
        if ((1073741824 & privateFlags) != 0) {
            viewStateIndex |= 32;
        }
        if (this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerationRequested && HardwareRenderer.isAvailable()) {
            viewStateIndex |= 64;
        }
        if ((268435456 & privateFlags) != 0) {
            viewStateIndex |= 128;
        }
        int privateFlags2 = this.mPrivateFlags2;
        if ((privateFlags2 & 1) != 0) {
            viewStateIndex |= 256;
        }
        if ((privateFlags2 & 2) != 0) {
            viewStateIndex |= 512;
        }
        int[] drawableState = VIEW_STATE_SETS[viewStateIndex];
        if (extraSpace != 0) {
            if (drawableState != null) {
                fullState = new int[drawableState.length + extraSpace];
                System.arraycopy(drawableState, 0, fullState, 0, drawableState.length);
            } else {
                fullState = new int[extraSpace];
            }
            return fullState;
        }
        return drawableState;
    }

    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        int N = baseState.length;
        int i = N - 1;
        while (i >= 0 && baseState[i] == 0) {
            i--;
        }
        System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
        return baseState;
    }

    public void jumpDrawablesToCurrentState() {
        if (this.mBackground != null) {
            this.mBackground.jumpToCurrentState();
        }
        if (this.mStateListAnimator != null) {
            this.mStateListAnimator.jumpToCurrentState();
        }
    }

    @RemotableViewMethod
    public void setBackgroundColor(int color) {
        if (this.mBackground instanceof ColorDrawable) {
            ((ColorDrawable) this.mBackground.mutate()).setColor(color);
            computeOpaqueFlags();
            this.mBackgroundResource = 0;
            return;
        }
        setBackground(new ColorDrawable(color));
    }

    @RemotableViewMethod
    public void setBackgroundResource(int resid) {
        if (resid == 0 || resid != this.mBackgroundResource) {
            Drawable d = null;
            if (resid != 0) {
                d = this.mContext.getDrawable(resid);
            }
            setBackground(d);
            this.mBackgroundResource = resid;
        }
    }

    public void setBackground(Drawable background) {
        setBackgroundDrawable(background);
    }

    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        computeOpaqueFlags();
        if (background != this.mBackground) {
            boolean requestLayout = false;
            this.mBackgroundResource = 0;
            if (this.mBackground != null) {
                this.mBackground.setCallback(null);
                unscheduleDrawable(this.mBackground);
            }
            if (background != null) {
                Rect padding = sThreadLocal.get();
                if (padding == null) {
                    padding = new Rect();
                    sThreadLocal.set(padding);
                }
                resetResolvedDrawablesInternal();
                background.setLayoutDirection(getLayoutDirection());
                if (background.getPadding(padding)) {
                    resetResolvedPaddingInternal();
                    switch (background.getLayoutDirection()) {
                        case 1:
                            this.mUserPaddingLeftInitial = padding.right;
                            this.mUserPaddingRightInitial = padding.left;
                            internalSetPadding(padding.right, padding.top, padding.left, padding.bottom);
                            break;
                        default:
                            this.mUserPaddingLeftInitial = padding.left;
                            this.mUserPaddingRightInitial = padding.right;
                            internalSetPadding(padding.left, padding.top, padding.right, padding.bottom);
                            break;
                    }
                    this.mLeftPaddingDefined = false;
                    this.mRightPaddingDefined = false;
                }
                if (this.mBackground == null || this.mBackground.getMinimumHeight() != background.getMinimumHeight() || this.mBackground.getMinimumWidth() != background.getMinimumWidth()) {
                    requestLayout = true;
                }
                background.setCallback(this);
                if (background.isStateful()) {
                    background.setState(getDrawableState());
                }
                background.setVisible(getVisibility() == 0, false);
                this.mBackground = background;
                applyBackgroundTint();
                if ((this.mPrivateFlags & 128) != 0) {
                    this.mPrivateFlags &= -129;
                    this.mPrivateFlags |= 256;
                    requestLayout = true;
                }
            } else {
                this.mBackground = null;
                if ((this.mPrivateFlags & 256) != 0) {
                    this.mPrivateFlags &= -257;
                    this.mPrivateFlags |= 128;
                }
                requestLayout = true;
            }
            computeOpaqueFlags();
            if (requestLayout) {
                requestLayout();
            }
            this.mBackgroundSizeChanged = true;
            invalidate(true);
        }
    }

    public Drawable getBackground() {
        return this.mBackground;
    }

    public void setBackgroundTintList(ColorStateList tint) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintList = tint;
        this.mBackgroundTint.mHasTintList = true;
        applyBackgroundTint();
    }

    public ColorStateList getBackgroundTintList() {
        if (this.mBackgroundTint != null) {
            return this.mBackgroundTint.mTintList;
        }
        return null;
    }

    public void setBackgroundTintMode(PorterDuff.Mode tintMode) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintMode = tintMode;
        this.mBackgroundTint.mHasTintMode = true;
        applyBackgroundTint();
    }

    public PorterDuff.Mode getBackgroundTintMode() {
        if (this.mBackgroundTint != null) {
            return this.mBackgroundTint.mTintMode;
        }
        return null;
    }

    private void applyBackgroundTint() {
        if (this.mBackground != null && this.mBackgroundTint != null) {
            TintInfo tintInfo = this.mBackgroundTint;
            if (tintInfo.mHasTintList || tintInfo.mHasTintMode) {
                this.mBackground = this.mBackground.mutate();
                if (tintInfo.mHasTintList) {
                    this.mBackground.setTintList(tintInfo.mTintList);
                }
                if (tintInfo.mHasTintMode) {
                    this.mBackground.setTintMode(tintInfo.mTintMode);
                }
                if (this.mBackground.isStateful()) {
                    this.mBackground.setState(getDrawableState());
                }
            }
        }
    }

    public void setPadding(int left, int top, int right, int bottom) {
        resetResolvedPaddingInternal();
        this.mUserPaddingStart = Integer.MIN_VALUE;
        this.mUserPaddingEnd = Integer.MIN_VALUE;
        this.mUserPaddingLeftInitial = left;
        this.mUserPaddingRightInitial = right;
        this.mLeftPaddingDefined = true;
        this.mRightPaddingDefined = true;
        internalSetPadding(left, top, right, bottom);
    }

    protected void internalSetPadding(int left, int top, int right, int bottom) {
        this.mUserPaddingLeft = left;
        this.mUserPaddingRight = right;
        this.mUserPaddingBottom = bottom;
        int viewFlags = this.mViewFlags;
        boolean changed = false;
        if ((viewFlags & 768) != 0) {
            if ((viewFlags & 512) != 0) {
                int offset = (viewFlags & 16777216) == 0 ? 0 : getVerticalScrollbarWidth();
                switch (this.mVerticalScrollbarPosition) {
                    case 0:
                        if (isLayoutRtl()) {
                            left += offset;
                        } else {
                            right += offset;
                        }
                        break;
                    case 1:
                        left += offset;
                        break;
                    case 2:
                        right += offset;
                        break;
                }
            }
            if ((viewFlags & 256) != 0) {
                bottom += (viewFlags & 16777216) != 0 ? getHorizontalScrollbarHeight() : 0;
            }
        }
        if (this.mPaddingLeft != left) {
            changed = true;
            this.mPaddingLeft = left;
        }
        if (this.mPaddingTop != top) {
            changed = true;
            this.mPaddingTop = top;
        }
        if (this.mPaddingRight != right) {
            changed = true;
            this.mPaddingRight = right;
        }
        if (this.mPaddingBottom != bottom) {
            changed = true;
            this.mPaddingBottom = bottom;
        }
        if (changed) {
            requestLayout();
            invalidateOutline();
        }
    }

    public void setPaddingRelative(int start, int top, int end, int bottom) {
        resetResolvedPaddingInternal();
        this.mUserPaddingStart = start;
        this.mUserPaddingEnd = end;
        this.mLeftPaddingDefined = true;
        this.mRightPaddingDefined = true;
        switch (getLayoutDirection()) {
            case 1:
                this.mUserPaddingLeftInitial = end;
                this.mUserPaddingRightInitial = start;
                internalSetPadding(end, top, start, bottom);
                break;
            default:
                this.mUserPaddingLeftInitial = start;
                this.mUserPaddingRightInitial = end;
                internalSetPadding(start, top, end, bottom);
                break;
        }
    }

    public int getPaddingTop() {
        return this.mPaddingTop;
    }

    public int getPaddingBottom() {
        return this.mPaddingBottom;
    }

    public int getPaddingLeft() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return this.mPaddingLeft;
    }

    public int getPaddingStart() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return getLayoutDirection() == 1 ? this.mPaddingRight : this.mPaddingLeft;
    }

    public int getPaddingRight() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return this.mPaddingRight;
    }

    public int getPaddingEnd() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return getLayoutDirection() == 1 ? this.mPaddingLeft : this.mPaddingRight;
    }

    public boolean isPaddingRelative() {
        return (this.mUserPaddingStart == Integer.MIN_VALUE && this.mUserPaddingEnd == Integer.MIN_VALUE) ? false : true;
    }

    Insets computeOpticalInsets() {
        return this.mBackground == null ? Insets.NONE : this.mBackground.getOpticalInsets();
    }

    public void resetPaddingToInitialValues() {
        if (isRtlCompatibilityMode()) {
            this.mPaddingLeft = this.mUserPaddingLeftInitial;
            this.mPaddingRight = this.mUserPaddingRightInitial;
        } else if (isLayoutRtl()) {
            this.mPaddingLeft = this.mUserPaddingEnd >= 0 ? this.mUserPaddingEnd : this.mUserPaddingLeftInitial;
            this.mPaddingRight = this.mUserPaddingStart >= 0 ? this.mUserPaddingStart : this.mUserPaddingRightInitial;
        } else {
            this.mPaddingLeft = this.mUserPaddingStart >= 0 ? this.mUserPaddingStart : this.mUserPaddingLeftInitial;
            this.mPaddingRight = this.mUserPaddingEnd >= 0 ? this.mUserPaddingEnd : this.mUserPaddingRightInitial;
        }
    }

    public Insets getOpticalInsets() {
        if (this.mLayoutInsets == null) {
            this.mLayoutInsets = computeOpticalInsets();
        }
        return this.mLayoutInsets;
    }

    public void setOpticalInsets(Insets insets) {
        this.mLayoutInsets = insets;
    }

    public void setSelected(boolean selected) {
        if (((this.mPrivateFlags & 4) != 0) != selected) {
            this.mPrivateFlags = (selected ? 4 : 0) | (this.mPrivateFlags & (-5));
            if (!selected) {
                resetPressedState();
            }
            invalidate(true);
            refreshDrawableState();
            dispatchSetSelected(selected);
            if (selected) {
                sendAccessibilityEvent(4);
            } else {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    protected void dispatchSetSelected(boolean selected) {
    }

    @ViewDebug.ExportedProperty
    public boolean isSelected() {
        return (this.mPrivateFlags & 4) != 0;
    }

    public void setActivated(boolean activated) {
        if (((this.mPrivateFlags & 1073741824) != 0) != activated) {
            this.mPrivateFlags = (activated ? 1073741824 : 0) | (this.mPrivateFlags & (-1073741825));
            invalidate(true);
            refreshDrawableState();
            dispatchSetActivated(activated);
        }
    }

    protected void dispatchSetActivated(boolean activated) {
    }

    @ViewDebug.ExportedProperty
    public boolean isActivated() {
        return (this.mPrivateFlags & 1073741824) != 0;
    }

    public ViewTreeObserver getViewTreeObserver() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mTreeObserver;
        }
        if (this.mFloatingTreeObserver == null) {
            this.mFloatingTreeObserver = new ViewTreeObserver();
        }
        return this.mFloatingTreeObserver;
    }

    public View getRootView() {
        View v;
        if (this.mAttachInfo == null || (v = this.mAttachInfo.mRootView) == null) {
            View parent = this;
            while (parent.mParent != null && (parent.mParent instanceof View)) {
                parent = (View) parent.mParent;
            }
            return parent;
        }
        return v;
    }

    public boolean toGlobalMotionEvent(MotionEvent ev) {
        AttachInfo info = this.mAttachInfo;
        if (info == null) {
            return false;
        }
        Matrix m = info.mTmpMatrix;
        m.set(Matrix.IDENTITY_MATRIX);
        transformMatrixToGlobal(m);
        ev.transform(m);
        return true;
    }

    public boolean toLocalMotionEvent(MotionEvent ev) {
        AttachInfo info = this.mAttachInfo;
        if (info == null) {
            return false;
        }
        Matrix m = info.mTmpMatrix;
        m.set(Matrix.IDENTITY_MATRIX);
        transformMatrixToLocal(m);
        ev.transform(m);
        return true;
    }

    public void transformMatrixToGlobal(Matrix m) {
        Object obj = this.mParent;
        if (obj instanceof View) {
            View vp = (View) obj;
            vp.transformMatrixToGlobal(m);
            m.preTranslate(-vp.mScrollX, -vp.mScrollY);
        } else if (obj instanceof ViewRootImpl) {
            ViewRootImpl vr = (ViewRootImpl) obj;
            vr.transformMatrixToGlobal(m);
            m.preTranslate(0.0f, -vr.mCurScrollY);
        }
        m.preTranslate(this.mLeft, this.mTop);
        if (!hasIdentityMatrix()) {
            m.preConcat(getMatrix());
        }
    }

    public void transformMatrixToLocal(Matrix m) {
        Object obj = this.mParent;
        if (obj instanceof View) {
            View vp = (View) obj;
            vp.transformMatrixToLocal(m);
            m.postTranslate(vp.mScrollX, vp.mScrollY);
        } else if (obj instanceof ViewRootImpl) {
            ViewRootImpl vr = (ViewRootImpl) obj;
            vr.transformMatrixToLocal(m);
            m.postTranslate(0.0f, vr.mCurScrollY);
        }
        m.postTranslate(-this.mLeft, -this.mTop);
        if (!hasIdentityMatrix()) {
            m.postConcat(getInverseMatrix());
        }
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, indexMapping = {@ViewDebug.IntToString(from = 0, to = "x"), @ViewDebug.IntToString(from = 1, to = "y")})
    public int[] getLocationOnScreen() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        return location;
    }

    public void getLocationOnScreen(int[] location) {
        getLocationInWindow(location);
        AttachInfo info = this.mAttachInfo;
        if (info != null) {
            location[0] = location[0] + info.mWindowLeft;
            location[1] = location[1] + info.mWindowTop;
        }
    }

    public void getLocationInWindow(int[] location) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException("location must be an array of two integers");
        }
        if (this.mAttachInfo == null) {
            location[1] = 0;
            location[0] = 0;
            return;
        }
        float[] position = this.mAttachInfo.mTmpTransformLocation;
        position[1] = 0.0f;
        position[0] = 0.0f;
        if (!hasIdentityMatrix()) {
            getMatrix().mapPoints(position);
        }
        position[0] = position[0] + this.mLeft;
        position[1] = position[1] + this.mTop;
        ViewParent viewParent = this.mParent;
        while (viewParent instanceof View) {
            View view = (View) viewParent;
            position[0] = position[0] - view.mScrollX;
            position[1] = position[1] - view.mScrollY;
            if (!view.hasIdentityMatrix()) {
                view.getMatrix().mapPoints(position);
            }
            position[0] = position[0] + view.mLeft;
            position[1] = position[1] + view.mTop;
            viewParent = view.mParent;
        }
        if (viewParent instanceof ViewRootImpl) {
            ViewRootImpl vr = (ViewRootImpl) viewParent;
            position[1] = position[1] - vr.mCurScrollY;
        }
        location[0] = (int) (position[0] + 0.5f);
        location[1] = (int) (position[1] + 0.5f);
    }

    protected View findViewTraversal(int id) {
        if (id == this.mID) {
            return this;
        }
        return null;
    }

    protected View findViewWithTagTraversal(Object tag) {
        if (tag == null || !tag.equals(this.mTag)) {
            return null;
        }
        return this;
    }

    protected View findViewByPredicateTraversal(Predicate<View> predicate, View childToSkip) {
        if (predicate.apply(this)) {
            return this;
        }
        return null;
    }

    public final View findViewById(int id) {
        if (id < 0) {
            return null;
        }
        return findViewTraversal(id);
    }

    final View findViewByAccessibilityId(int accessibilityId) {
        if (accessibilityId < 0) {
            return null;
        }
        return findViewByAccessibilityIdTraversal(accessibilityId);
    }

    public View findViewByAccessibilityIdTraversal(int accessibilityId) {
        if (getAccessibilityViewId() == accessibilityId) {
            return this;
        }
        return null;
    }

    public final View findViewWithTag(Object tag) {
        if (tag == null) {
            return null;
        }
        return findViewWithTagTraversal(tag);
    }

    public final View findViewByPredicate(Predicate<View> predicate) {
        return findViewByPredicateTraversal(predicate, null);
    }

    public final View findViewByPredicateInsideOut(View start, Predicate<View> predicate) {
        View childToSkip = null;
        while (true) {
            View view = start.findViewByPredicateTraversal(predicate, childToSkip);
            if (view == null && start != this) {
                Object parent = start.getParent();
                if (parent == null || !(parent instanceof View)) {
                    break;
                }
                childToSkip = start;
                start = (View) parent;
            } else {
                return view;
            }
        }
        return null;
    }

    public void setId(int id) {
        this.mID = id;
        if (this.mID == -1 && this.mLabelForId != -1) {
            this.mID = generateViewId();
        }
    }

    public void setIsRootNamespace(boolean isRoot) {
        if (isRoot) {
            this.mPrivateFlags |= 8;
        } else {
            this.mPrivateFlags &= -9;
        }
    }

    public boolean isRootNamespace() {
        return (this.mPrivateFlags & 8) != 0;
    }

    @ViewDebug.CapturedViewProperty
    public int getId() {
        return this.mID;
    }

    @ViewDebug.ExportedProperty
    public Object getTag() {
        return this.mTag;
    }

    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public Object getTag(int key) {
        if (this.mKeyedTags != null) {
            return this.mKeyedTags.get(key);
        }
        return null;
    }

    public void setTag(int key, Object tag) {
        if ((key >>> 24) < 2) {
            throw new IllegalArgumentException("The key must be an application-specific resource id.");
        }
        setKeyedTag(key, tag);
    }

    public void setTagInternal(int key, Object tag) {
        if ((key >>> 24) != 1) {
            throw new IllegalArgumentException("The key must be a framework-specific resource id.");
        }
        setKeyedTag(key, tag);
    }

    private void setKeyedTag(int key, Object tag) {
        if (this.mKeyedTags == null) {
            this.mKeyedTags = new SparseArray<>(2);
        }
        this.mKeyedTags.put(key, tag);
    }

    public void debug() {
        debug(0);
    }

    protected void debug(int depth) {
        String output;
        String output2 = debugIndent(depth - 1) + "+ " + this;
        int id = getId();
        if (id != -1) {
            output2 = output2 + " (id=" + id + ")";
        }
        Object tag = getTag();
        if (tag != null) {
            output2 = output2 + " (tag=" + tag + ")";
        }
        Log.d(VIEW_LOG_TAG, output2);
        if ((this.mPrivateFlags & 2) != 0) {
            Log.d(VIEW_LOG_TAG, debugIndent(depth) + " FOCUSED");
        }
        Log.d(VIEW_LOG_TAG, debugIndent(depth) + "frame={" + this.mLeft + ", " + this.mTop + ", " + this.mRight + ", " + this.mBottom + "} scroll={" + this.mScrollX + ", " + this.mScrollY + "} ");
        if (this.mPaddingLeft != 0 || this.mPaddingTop != 0 || this.mPaddingRight != 0 || this.mPaddingBottom != 0) {
            Log.d(VIEW_LOG_TAG, debugIndent(depth) + "padding={" + this.mPaddingLeft + ", " + this.mPaddingTop + ", " + this.mPaddingRight + ", " + this.mPaddingBottom + "}");
        }
        Log.d(VIEW_LOG_TAG, debugIndent(depth) + "mMeasureWidth=" + this.mMeasuredWidth + " mMeasureHeight=" + this.mMeasuredHeight);
        String output3 = debugIndent(depth);
        if (this.mLayoutParams == null) {
            output = output3 + "BAD! no layout params";
        } else {
            output = this.mLayoutParams.debug(output3);
        }
        Log.d(VIEW_LOG_TAG, output);
        Log.d(VIEW_LOG_TAG, ((debugIndent(depth) + "flags={") + printFlags(this.mViewFlags)) + "}");
        Log.d(VIEW_LOG_TAG, ((debugIndent(depth) + "privateFlags={") + printPrivateFlags(this.mPrivateFlags)) + "}");
    }

    protected static String debugIndent(int depth) {
        StringBuilder spaces = new StringBuilder(((depth * 2) + 3) * 2);
        for (int i = 0; i < (depth * 2) + 3; i++) {
            spaces.append(' ').append(' ');
        }
        return spaces.toString();
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public int getBaseline() {
        return -1;
    }

    public boolean isInLayout() {
        ViewRootImpl viewRoot = getViewRootImpl();
        return viewRoot != null && viewRoot.isInLayout();
    }

    public void requestLayout() {
        if (this.mMeasureCache != null) {
            this.mMeasureCache.clear();
        }
        if (this.mAttachInfo != null && this.mAttachInfo.mViewRequestingLayout == null) {
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot == null || !viewRoot.isInLayout() || viewRoot.requestLayoutDuringLayout(this)) {
                this.mAttachInfo.mViewRequestingLayout = this;
            } else {
                return;
            }
        }
        this.mPrivateFlags |= 4096;
        this.mPrivateFlags |= Integer.MIN_VALUE;
        if (this.mParent != null && !this.mParent.isLayoutRequested()) {
            this.mParent.requestLayout();
        }
        if (this.mAttachInfo != null && this.mAttachInfo.mViewRequestingLayout == this) {
            this.mAttachInfo.mViewRequestingLayout = null;
        }
    }

    public void forceLayout() {
        if (this.mMeasureCache != null) {
            this.mMeasureCache.clear();
        }
        this.mPrivateFlags |= 4096;
        this.mPrivateFlags |= Integer.MIN_VALUE;
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean optical = isLayoutModeOptical(this);
        if (optical != isLayoutModeOptical(this.mParent)) {
            Insets insets = getOpticalInsets();
            int oWidth = insets.left + insets.right;
            int oHeight = insets.top + insets.bottom;
            if (optical) {
                oWidth = -oWidth;
            }
            widthMeasureSpec = MeasureSpec.adjust(widthMeasureSpec, oWidth);
            if (optical) {
                oHeight = -oHeight;
            }
            heightMeasureSpec = MeasureSpec.adjust(heightMeasureSpec, oHeight);
        }
        long key = (((long) widthMeasureSpec) << 32) | (((long) heightMeasureSpec) & ExpandableListView.PACKED_POSITION_VALUE_NULL);
        if (this.mMeasureCache == null) {
            this.mMeasureCache = new LongSparseLongArray(2);
        }
        boolean forceLayout = (this.mPrivateFlags & 4096) == 4096;
        boolean isExactly = MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && MeasureSpec.getMode(heightMeasureSpec) == 1073741824;
        boolean matchingSize = isExactly && getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec) && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
        if (forceLayout || (!matchingSize && (widthMeasureSpec != this.mOldWidthMeasureSpec || heightMeasureSpec != this.mOldHeightMeasureSpec))) {
            this.mPrivateFlags &= -2049;
            resolveRtlPropertiesIfNeeded();
            int cacheIndex = forceLayout ? -1 : this.mMeasureCache.indexOfKey(key);
            if (cacheIndex < 0 || sIgnoreMeasureCache) {
                onMeasure(widthMeasureSpec, heightMeasureSpec);
                this.mPrivateFlags3 &= -9;
            } else {
                long value = this.mMeasureCache.valueAt(cacheIndex);
                setMeasuredDimensionRaw((int) (value >> 32), (int) value);
                this.mPrivateFlags3 |= 8;
            }
            if ((this.mPrivateFlags & 2048) != 2048) {
                throw new IllegalStateException("onMeasure() did not set the measured dimension by calling setMeasuredDimension()");
            }
            this.mPrivateFlags |= 8192;
        }
        this.mOldWidthMeasureSpec = widthMeasureSpec;
        this.mOldHeightMeasureSpec = heightMeasureSpec;
        this.mMeasureCache.put(key, (((long) this.mMeasuredWidth) << 32) | (((long) this.mMeasuredHeight) & ExpandableListView.PACKED_POSITION_VALUE_NULL));
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        boolean optical = isLayoutModeOptical(this);
        if (optical != isLayoutModeOptical(this.mParent)) {
            Insets insets = getOpticalInsets();
            int opticalWidth = insets.left + insets.right;
            int opticalHeight = insets.top + insets.bottom;
            if (!optical) {
                opticalWidth = -opticalWidth;
            }
            measuredWidth += opticalWidth;
            if (!optical) {
                opticalHeight = -opticalHeight;
            }
            measuredHeight += opticalHeight;
        }
        setMeasuredDimensionRaw(measuredWidth, measuredHeight);
    }

    private void setMeasuredDimensionRaw(int measuredWidth, int measuredHeight) {
        this.mMeasuredWidth = measuredWidth;
        this.mMeasuredHeight = measuredHeight;
        this.mPrivateFlags |= 2048;
    }

    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }

    public static int resolveSize(int size, int measureSpec) {
        return resolveSizeAndState(size, measureSpec, 0) & 16777215;
    }

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case Integer.MIN_VALUE:
                if (specSize < size) {
                    result = specSize | 16777216;
                } else {
                    result = size;
                }
                break;
            case 0:
                result = size;
                break;
            case 1073741824:
                result = specSize;
                break;
        }
        return ((-16777216) & childMeasuredState) | result;
    }

    public static int getDefaultSize(int size, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case Integer.MIN_VALUE:
            case 1073741824:
                break;
            case 0:
                break;
        }
        return size;
    }

    protected int getSuggestedMinimumHeight() {
        return this.mBackground == null ? this.mMinHeight : Math.max(this.mMinHeight, this.mBackground.getMinimumHeight());
    }

    protected int getSuggestedMinimumWidth() {
        return this.mBackground == null ? this.mMinWidth : Math.max(this.mMinWidth, this.mBackground.getMinimumWidth());
    }

    public int getMinimumHeight() {
        return this.mMinHeight;
    }

    public void setMinimumHeight(int minHeight) {
        this.mMinHeight = minHeight;
        requestLayout();
    }

    public int getMinimumWidth() {
        return this.mMinWidth;
    }

    public void setMinimumWidth(int minWidth) {
        this.mMinWidth = minWidth;
        requestLayout();
    }

    public Animation getAnimation() {
        return this.mCurrentAnimation;
    }

    public void startAnimation(Animation animation) {
        animation.setStartTime(-1L);
        setAnimation(animation);
        invalidateParentCaches();
        invalidate(true);
    }

    public void clearAnimation() {
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.detach();
        }
        this.mCurrentAnimation = null;
        invalidateParentIfNeeded();
    }

    public void setAnimation(Animation animation) {
        this.mCurrentAnimation = animation;
        if (animation != null) {
            if (this.mAttachInfo != null && this.mAttachInfo.mDisplayState == 1 && animation.getStartTime() == -1) {
                animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());
            }
            animation.reset();
        }
    }

    protected void onAnimationStart() {
        this.mPrivateFlags |= 65536;
    }

    protected void onAnimationEnd() {
        this.mPrivateFlags &= -65537;
    }

    protected boolean onSetAlpha(int alpha) {
        return false;
    }

    public boolean gatherTransparentRegion(Region region) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (region != null && attachInfo != null) {
            int pflags = this.mPrivateFlags;
            if ((pflags & 128) == 0) {
                int[] location = attachInfo.mTransparentLocation;
                getLocationInWindow(location);
                region.op(location[0], location[1], (location[0] + this.mRight) - this.mLeft, (location[1] + this.mBottom) - this.mTop, Region.Op.DIFFERENCE);
            } else if ((pflags & 256) != 0 && this.mBackground != null && this.mBackground.getOpacity() != -2) {
                applyDrawableToTransparentRegion(this.mBackground, region);
            }
        }
        return true;
    }

    public void playSoundEffect(int soundConstant) {
        if (this.mAttachInfo != null && this.mAttachInfo.mRootCallbacks != null && isSoundEffectsEnabled()) {
            this.mAttachInfo.mRootCallbacks.playSoundEffect(soundConstant);
        }
    }

    public boolean performHapticFeedback(int feedbackConstant) {
        return performHapticFeedback(feedbackConstant, 0);
    }

    public boolean performHapticFeedback(int feedbackConstant, int flags) {
        if (this.mAttachInfo == null) {
            return false;
        }
        if ((flags & 1) != 0 || isHapticFeedbackEnabled()) {
            return this.mAttachInfo.mRootCallbacks.performHapticFeedback(feedbackConstant, (flags & 2) != 0);
        }
        return false;
    }

    public void setSystemUiVisibility(int visibility) {
        if (visibility != this.mSystemUiVisibility) {
            this.mSystemUiVisibility = visibility;
            if (this.mParent != null && this.mAttachInfo != null && !this.mAttachInfo.mRecomputeGlobalAttributes) {
                this.mParent.recomputeViewAttributes(this);
            }
        }
    }

    public int getSystemUiVisibility() {
        return this.mSystemUiVisibility;
    }

    public int getWindowSystemUiVisibility() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mSystemUiVisibility;
        }
        return 0;
    }

    public void onWindowSystemUiVisibilityChanged(int visible) {
    }

    public void dispatchWindowSystemUiVisiblityChanged(int visible) {
        onWindowSystemUiVisibilityChanged(visible);
    }

    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {
        getListenerInfo().mOnSystemUiVisibilityChangeListener = l;
        if (this.mParent != null && this.mAttachInfo != null && !this.mAttachInfo.mRecomputeGlobalAttributes) {
            this.mParent.recomputeViewAttributes(this);
        }
    }

    public void dispatchSystemUiVisibilityChanged(int visibility) {
        ListenerInfo li = this.mListenerInfo;
        if (li != null && li.mOnSystemUiVisibilityChangeListener != null) {
            li.mOnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange(visibility & PUBLIC_STATUS_BAR_VISIBILITY_MASK);
        }
    }

    boolean updateLocalSystemUiVisibility(int localValue, int localChanges) {
        int val = (this.mSystemUiVisibility & (localChanges ^ (-1))) | (localValue & localChanges);
        if (val == this.mSystemUiVisibility) {
            return false;
        }
        setSystemUiVisibility(val);
        return true;
    }

    public void setDisabledSystemUiVisibility(int flags) {
        if (this.mAttachInfo != null && this.mAttachInfo.mDisabledSystemUiVisibility != flags) {
            this.mAttachInfo.mDisabledSystemUiVisibility = flags;
            if (this.mParent != null) {
                this.mParent.recomputeViewAttributes(this);
            }
        }
    }

    public static class DragShadowBuilder {
        private final WeakReference<View> mView;

        public DragShadowBuilder(View view) {
            this.mView = new WeakReference<>(view);
        }

        public DragShadowBuilder() {
            this.mView = new WeakReference<>(null);
        }

        public final View getView() {
            return this.mView.get();
        }

        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            View view = this.mView.get();
            if (view != null) {
                shadowSize.set(view.getWidth(), view.getHeight());
                shadowTouchPoint.set(shadowSize.x / 2, shadowSize.y / 2);
            } else {
                Log.e(View.VIEW_LOG_TAG, "Asked for drag thumb metrics but no view");
            }
        }

        public void onDrawShadow(Canvas canvas) {
            View view = this.mView.get();
            if (view != null) {
                view.draw(canvas);
            } else {
                Log.e(View.VIEW_LOG_TAG, "Asked to draw drag shadow but no view");
            }
        }
    }

    public final boolean startDrag(ClipData data, DragShadowBuilder shadowBuilder, Object myLocalState, int flags) {
        Point shadowSize = new Point();
        Point shadowTouchPoint = new Point();
        shadowBuilder.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
        if (shadowSize.x < 0 || shadowSize.y < 0 || shadowTouchPoint.x < 0 || shadowTouchPoint.y < 0) {
            throw new IllegalStateException("Drag shadow dimensions must not be negative");
        }
        Surface surface = new Surface();
        try {
            IBinder token = this.mAttachInfo.mSession.prepareDrag(this.mAttachInfo.mWindow, flags, shadowSize.x, shadowSize.y, surface);
            if (token == null) {
                return false;
            }
            Canvas canvas = surface.lockCanvas(null);
            try {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                shadowBuilder.onDrawShadow(canvas);
                surface.unlockCanvasAndPost(canvas);
                ViewRootImpl root = getViewRootImpl();
                root.setLocalDragState(myLocalState);
                root.getLastTouchPoint(shadowSize);
                boolean okay = this.mAttachInfo.mSession.performDrag(this.mAttachInfo.mWindow, token, shadowSize.x, shadowSize.y, shadowTouchPoint.x, shadowTouchPoint.y, data);
                surface.release();
                return okay;
            } catch (Throwable th) {
                surface.unlockCanvasAndPost(canvas);
                throw th;
            }
        } catch (Exception e) {
            Log.e(VIEW_LOG_TAG, "Unable to initiate drag", e);
            surface.destroy();
            return false;
        }
    }

    public boolean onDragEvent(DragEvent event) {
        return false;
    }

    public boolean dispatchDragEvent(DragEvent event) {
        ListenerInfo li = this.mListenerInfo;
        if (li == null || li.mOnDragListener == null || (this.mViewFlags & 32) != 0 || !li.mOnDragListener.onDrag(this, event)) {
            return onDragEvent(event);
        }
        return true;
    }

    boolean canAcceptDrag() {
        return (this.mPrivateFlags2 & 1) != 0;
    }

    public void onCloseSystemDialogs(String reason) {
    }

    public void applyDrawableToTransparentRegion(Drawable dr, Region region) {
        Region r = dr.getTransparentRegion();
        Rect db = dr.getBounds();
        AttachInfo attachInfo = this.mAttachInfo;
        if (r != null && attachInfo != null) {
            int w = getRight() - getLeft();
            int h = getBottom() - getTop();
            if (db.left > 0) {
                r.op(0, 0, db.left, h, Region.Op.UNION);
            }
            if (db.right < w) {
                r.op(db.right, 0, w, h, Region.Op.UNION);
            }
            if (db.top > 0) {
                r.op(0, 0, w, db.top, Region.Op.UNION);
            }
            if (db.bottom < h) {
                r.op(0, db.bottom, w, h, Region.Op.UNION);
            }
            int[] location = attachInfo.mTransparentLocation;
            getLocationInWindow(location);
            r.translate(location[0], location[1]);
            region.op(r, Region.Op.INTERSECT);
            return;
        }
        region.op(db, Region.Op.DIFFERENCE);
    }

    private void checkForLongClick(int delayOffset) {
        if ((this.mViewFlags & 2097152) == 2097152) {
            this.mHasPerformedLongPress = false;
            if (this.mPendingCheckForLongPress == null) {
                this.mPendingCheckForLongPress = new CheckForLongPress();
            }
            this.mPendingCheckForLongPress.rememberWindowAttachCount();
            postDelayed(this.mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout() - delayOffset);
        }
    }

    public static View inflate(Context context, int resource, ViewGroup root) {
        LayoutInflater factory = LayoutInflater.from(context);
        return factory.inflate(resource, root);
    }

    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        int overScrollMode = this.mOverScrollMode;
        boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
        boolean overScrollHorizontal = overScrollMode == 0 || (overScrollMode == 1 && canScrollHorizontal);
        boolean overScrollVertical = overScrollMode == 0 || (overScrollMode == 1 && canScrollVertical);
        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }
        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }
        int left = -maxOverScrollX;
        int right = maxOverScrollX + scrollRangeX;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;
        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }
        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }
        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);
        return clampedX || clampedY;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
    }

    public int getOverScrollMode() {
        return this.mOverScrollMode;
    }

    public void setOverScrollMode(int overScrollMode) {
        if (overScrollMode != 0 && overScrollMode != 1 && overScrollMode != 2) {
            throw new IllegalArgumentException("Invalid overscroll mode " + overScrollMode);
        }
        this.mOverScrollMode = overScrollMode;
    }

    public void setNestedScrollingEnabled(boolean enabled) {
        if (enabled) {
            this.mPrivateFlags3 |= 128;
        } else {
            stopNestedScroll();
            this.mPrivateFlags3 &= -129;
        }
    }

    public boolean isNestedScrollingEnabled() {
        return (this.mPrivateFlags3 & 128) == 128;
    }

    public boolean startNestedScroll(int axes) {
        if (hasNestedScrollingParent()) {
            return true;
        }
        if (isNestedScrollingEnabled()) {
            View child = this;
            for (ViewParent p = getParent(); p != null; p = p.getParent()) {
                try {
                    if (p.onStartNestedScroll(child, this, axes)) {
                        this.mNestedScrollingParent = p;
                        p.onNestedScrollAccepted(child, this, axes);
                        return true;
                    }
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, "ViewParent " + p + " does not implement interface method onStartNestedScroll", e);
                }
                if (p instanceof View) {
                    View child2 = p;
                    child = child2;
                }
            }
        }
        return false;
    }

    public void stopNestedScroll() {
        if (this.mNestedScrollingParent != null) {
            this.mNestedScrollingParent.onStopNestedScroll(this);
            this.mNestedScrollingParent = null;
        }
    }

    public boolean hasNestedScrollingParent() {
        return this.mNestedScrollingParent != null;
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            if (dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }
                this.mNestedScrollingParent.onNestedScroll(this, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] = offsetInWindow[0] - startX;
                    offsetInWindow[1] = offsetInWindow[1] - startY;
                }
                return true;
            }
            if (offsetInWindow != null) {
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        if (!isNestedScrollingEnabled() || this.mNestedScrollingParent == null) {
            return false;
        }
        if (dx != 0 || dy != 0) {
            int startX = 0;
            int startY = 0;
            if (offsetInWindow != null) {
                getLocationInWindow(offsetInWindow);
                startX = offsetInWindow[0];
                startY = offsetInWindow[1];
            }
            if (consumed == null) {
                if (this.mTempNestedScrollConsumed == null) {
                    this.mTempNestedScrollConsumed = new int[2];
                }
                consumed = this.mTempNestedScrollConsumed;
            }
            consumed[0] = 0;
            consumed[1] = 0;
            this.mNestedScrollingParent.onNestedPreScroll(this, dx, dy, consumed);
            if (offsetInWindow != null) {
                getLocationInWindow(offsetInWindow);
                offsetInWindow[0] = offsetInWindow[0] - startX;
                offsetInWindow[1] = offsetInWindow[1] - startY;
            }
            return (consumed[0] == 0 && consumed[1] == 0) ? false : true;
        }
        if (offsetInWindow == null) {
            return false;
        }
        offsetInWindow[0] = 0;
        offsetInWindow[1] = 0;
        return false;
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (!isNestedScrollingEnabled() || this.mNestedScrollingParent == null) {
            return false;
        }
        return this.mNestedScrollingParent.onNestedFling(this, velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (!isNestedScrollingEnabled() || this.mNestedScrollingParent == null) {
            return false;
        }
        return this.mNestedScrollingParent.onNestedPreFling(this, velocityX, velocityY);
    }

    protected float getVerticalScrollFactor() {
        if (this.mVerticalScrollFactor == 0.0f) {
            TypedValue outValue = new TypedValue();
            if (!this.mContext.getTheme().resolveAttribute(16842829, outValue, true)) {
                throw new IllegalStateException("Expected theme to define listPreferredItemHeight.");
            }
            this.mVerticalScrollFactor = outValue.getDimension(this.mContext.getResources().getDisplayMetrics());
        }
        return this.mVerticalScrollFactor;
    }

    protected float getHorizontalScrollFactor() {
        return getVerticalScrollFactor();
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT, mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "FIRST_STRONG"), @ViewDebug.IntToString(from = 2, to = "ANY_RTL"), @ViewDebug.IntToString(from = 3, to = "LTR"), @ViewDebug.IntToString(from = 4, to = "RTL"), @ViewDebug.IntToString(from = 5, to = "LOCALE")})
    public int getRawTextDirection() {
        return (this.mPrivateFlags2 & 448) >> 6;
    }

    public void setTextDirection(int textDirection) {
        if (getRawTextDirection() != textDirection) {
            this.mPrivateFlags2 &= -449;
            resetResolvedTextDirection();
            this.mPrivateFlags2 |= (textDirection << 6) & 448;
            resolveTextDirection();
            onRtlPropertiesChanged(getLayoutDirection());
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT, mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "FIRST_STRONG"), @ViewDebug.IntToString(from = 2, to = "ANY_RTL"), @ViewDebug.IntToString(from = 3, to = "LTR"), @ViewDebug.IntToString(from = 4, to = "RTL"), @ViewDebug.IntToString(from = 5, to = "LOCALE")})
    public int getTextDirection() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_DIRECTION_RESOLVED_MASK) >> 10;
    }

    public boolean resolveTextDirection() {
        int parentResolvedDirection;
        this.mPrivateFlags2 &= -7681;
        if (hasRtlSupport()) {
            int textDirection = getRawTextDirection();
            switch (textDirection) {
                case 0:
                    if (!canResolveTextDirection()) {
                        this.mPrivateFlags2 |= 1024;
                        return false;
                    }
                    try {
                        if (!this.mParent.isTextDirectionResolved()) {
                            this.mPrivateFlags2 |= 1024;
                            return false;
                        }
                        try {
                            parentResolvedDirection = this.mParent.getTextDirection();
                        } catch (AbstractMethodError e) {
                            Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                            parentResolvedDirection = 3;
                        }
                        switch (parentResolvedDirection) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                this.mPrivateFlags2 |= parentResolvedDirection << 10;
                                break;
                            default:
                                this.mPrivateFlags2 |= 1024;
                                break;
                        }
                    } catch (AbstractMethodError e2) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e2);
                        this.mPrivateFlags2 |= 1536;
                        return true;
                    }
                    break;
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    this.mPrivateFlags2 |= textDirection << 10;
                    break;
                default:
                    this.mPrivateFlags2 |= 1024;
                    break;
            }
        } else {
            this.mPrivateFlags2 |= 1024;
        }
        this.mPrivateFlags2 |= 512;
        return true;
    }

    public boolean canResolveTextDirection() {
        switch (getRawTextDirection()) {
            case 0:
                if (this.mParent != null) {
                    try {
                        return this.mParent.canResolveTextDirection();
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    }
                }
                return false;
            default:
                return true;
        }
    }

    public void resetResolvedTextDirection() {
        this.mPrivateFlags2 &= -7681;
        this.mPrivateFlags2 |= 1024;
    }

    public boolean isTextDirectionInherited() {
        return getRawTextDirection() == 0;
    }

    public boolean isTextDirectionResolved() {
        return (this.mPrivateFlags2 & 512) == 512;
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT, mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "GRAVITY"), @ViewDebug.IntToString(from = 2, to = "TEXT_START"), @ViewDebug.IntToString(from = 3, to = "TEXT_END"), @ViewDebug.IntToString(from = 4, to = "CENTER"), @ViewDebug.IntToString(from = 5, to = "VIEW_START"), @ViewDebug.IntToString(from = 6, to = "VIEW_END")})
    public int getRawTextAlignment() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_MASK) >> 13;
    }

    public void setTextAlignment(int textAlignment) {
        if (textAlignment != getRawTextAlignment()) {
            this.mPrivateFlags2 &= -57345;
            resetResolvedTextAlignment();
            this.mPrivateFlags2 |= (textAlignment << 13) & PFLAG2_TEXT_ALIGNMENT_MASK;
            resolveTextAlignment();
            onRtlPropertiesChanged(getLayoutDirection());
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT, mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "GRAVITY"), @ViewDebug.IntToString(from = 2, to = "TEXT_START"), @ViewDebug.IntToString(from = 3, to = "TEXT_END"), @ViewDebug.IntToString(from = 4, to = "CENTER"), @ViewDebug.IntToString(from = 5, to = "VIEW_START"), @ViewDebug.IntToString(from = 6, to = "VIEW_END")})
    public int getTextAlignment() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK) >> 17;
    }

    public boolean resolveTextAlignment() {
        int parentResolvedTextAlignment;
        this.mPrivateFlags2 &= -983041;
        if (hasRtlSupport()) {
            int textAlignment = getRawTextAlignment();
            switch (textAlignment) {
                case 0:
                    if (!canResolveTextAlignment()) {
                        this.mPrivateFlags2 |= 131072;
                        return false;
                    }
                    try {
                        if (!this.mParent.isTextAlignmentResolved()) {
                            this.mPrivateFlags2 |= 131072;
                            return false;
                        }
                        try {
                            parentResolvedTextAlignment = this.mParent.getTextAlignment();
                        } catch (AbstractMethodError e) {
                            Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                            parentResolvedTextAlignment = 1;
                        }
                        switch (parentResolvedTextAlignment) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                                this.mPrivateFlags2 |= parentResolvedTextAlignment << 17;
                                break;
                            default:
                                this.mPrivateFlags2 |= 131072;
                                break;
                        }
                    } catch (AbstractMethodError e2) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e2);
                        this.mPrivateFlags2 |= 196608;
                        return true;
                    }
                    break;
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    this.mPrivateFlags2 |= textAlignment << 17;
                    break;
                default:
                    this.mPrivateFlags2 |= 131072;
                    break;
            }
        } else {
            this.mPrivateFlags2 |= 131072;
        }
        this.mPrivateFlags2 |= 65536;
        return true;
    }

    public boolean canResolveTextAlignment() {
        switch (getRawTextAlignment()) {
            case 0:
                if (this.mParent != null) {
                    try {
                        return this.mParent.canResolveTextAlignment();
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    }
                }
                return false;
            default:
                return true;
        }
    }

    public void resetResolvedTextAlignment() {
        this.mPrivateFlags2 &= -983041;
        this.mPrivateFlags2 |= 131072;
    }

    public boolean isTextAlignmentInherited() {
        return getRawTextAlignment() == 0;
    }

    public boolean isTextAlignmentResolved() {
        return (this.mPrivateFlags2 & 65536) == 65536;
    }

    public static int generateViewId() {
        int result;
        int newValue;
        do {
            result = sNextGeneratedId.get();
            newValue = result + 1;
            if (newValue > 16777215) {
                newValue = 1;
            }
        } while (!sNextGeneratedId.compareAndSet(result, newValue));
        return result;
    }

    public void captureTransitioningViews(List<View> transitioningViews) {
        if (getVisibility() == 0) {
            transitioningViews.add(this);
        }
    }

    public void findNamedViews(Map<String, View> namedElements) {
        String transitionName;
        if ((getVisibility() == 0 || this.mGhostView != null) && (transitionName = getTransitionName()) != null) {
            namedElements.put(transitionName, this);
        }
    }

    public static class MeasureSpec {
        public static final int AT_MOST = Integer.MIN_VALUE;
        public static final int EXACTLY = 1073741824;
        private static final int MODE_MASK = -1073741824;
        private static final int MODE_SHIFT = 30;
        public static final int UNSPECIFIED = 0;

        public static int makeMeasureSpec(int size, int mode) {
            return View.sUseBrokenMakeMeasureSpec ? size + mode : (1073741823 & size) | ((-1073741824) & mode);
        }

        public static int getMode(int measureSpec) {
            return (-1073741824) & measureSpec;
        }

        public static int getSize(int measureSpec) {
            return 1073741823 & measureSpec;
        }

        static int adjust(int measureSpec, int delta) {
            int mode = getMode(measureSpec);
            if (mode == 0) {
                return makeMeasureSpec(0, 0);
            }
            int size = getSize(measureSpec) + delta;
            if (size < 0) {
                Log.e(View.VIEW_LOG_TAG, "MeasureSpec.adjust: new size would be negative! (" + size + ") spec: " + toString(measureSpec) + " delta: " + delta);
                size = 0;
            }
            return makeMeasureSpec(size, mode);
        }

        public static String toString(int measureSpec) {
            int mode = getMode(measureSpec);
            int size = getSize(measureSpec);
            StringBuilder sb = new StringBuilder("MeasureSpec: ");
            if (mode == 0) {
                sb.append("UNSPECIFIED ");
            } else if (mode == 1073741824) {
                sb.append("EXACTLY ");
            } else if (mode == Integer.MIN_VALUE) {
                sb.append("AT_MOST ");
            } else {
                sb.append(mode).append(" ");
            }
            sb.append(size);
            return sb.toString();
        }
    }

    private final class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        private CheckForLongPress() {
        }

        @Override
        public void run() {
            if (View.this.isPressed() && View.this.mParent != null && this.mOriginalWindowAttachCount == View.this.mWindowAttachCount && View.this.performLongClick()) {
                View.this.mHasPerformedLongPress = true;
            }
        }

        public void rememberWindowAttachCount() {
            this.mOriginalWindowAttachCount = View.this.mWindowAttachCount;
        }
    }

    private final class CheckForTap implements Runnable {
        public float x;
        public float y;

        private CheckForTap() {
        }

        @Override
        public void run() {
            View.this.mPrivateFlags &= -33554433;
            View.this.setPressed(true, this.x, this.y);
            View.this.checkForLongClick(ViewConfiguration.getTapTimeout());
        }
    }

    private final class PerformClick implements Runnable {
        private PerformClick() {
        }

        @Override
        public void run() {
            View.this.performClick();
        }
    }

    public void hackTurnOffWindowResizeAnim(boolean off) {
        this.mAttachInfo.mTurnOffWindowResizeAnim = off;
    }

    public ViewPropertyAnimator animate() {
        if (this.mAnimator == null) {
            this.mAnimator = new ViewPropertyAnimator(this);
        }
        return this.mAnimator;
    }

    public final void setTransitionName(String transitionName) {
        this.mTransitionName = transitionName;
    }

    @ViewDebug.ExportedProperty
    public String getTransitionName() {
        return this.mTransitionName;
    }

    private final class UnsetPressedState implements Runnable {
        private UnsetPressedState() {
        }

        @Override
        public void run() {
            View.this.setPressed(false);
        }
    }

    public static class BaseSavedState extends AbsSavedState {
        public static final Parcelable.Creator<BaseSavedState> CREATOR = new Parcelable.Creator<BaseSavedState>() {
            @Override
            public BaseSavedState createFromParcel(Parcel in) {
                return new BaseSavedState(in);
            }

            @Override
            public BaseSavedState[] newArray(int size) {
                return new BaseSavedState[size];
            }
        };

        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }
    }

    static final class AttachInfo {
        int mAccessibilityFetchFlags;
        Drawable mAccessibilityFocusDrawable;
        float mApplicationScale;
        Canvas mCanvas;
        int mDisabledSystemUiVisibility;
        final Display mDisplay;
        long mDrawingTime;
        boolean mForceReportNewAttributes;
        int mGlobalSystemUiVisibility;
        final Handler mHandler;
        boolean mHardwareAccelerated;
        boolean mHardwareAccelerationRequested;
        HardwareRenderer mHardwareRenderer;
        boolean mHasNonEmptyGivenInternalInsets;
        boolean mHasSystemUiListeners;
        boolean mHasWindowFocus;
        boolean mHighContrastText;
        IWindowId mIWindowId;
        boolean mIgnoreDirtyState;
        boolean mInTouchMode;
        boolean mKeepScreenOn;
        boolean mOverscanRequested;
        IBinder mPanelParentWindowToken;
        List<RenderNode> mPendingAnimatingRenderNodes;
        boolean mRecomputeGlobalAttributes;
        final Callbacks mRootCallbacks;
        View mRootView;
        boolean mScalingRequired;
        final IWindowSession mSession;
        int mSystemUiVisibility;
        boolean mTurnOffWindowResizeAnim;
        boolean mUnbufferedDispatchRequested;
        boolean mUse32BitDrawingCache;
        View mViewRequestingLayout;
        final ViewRootImpl mViewRootImpl;
        boolean mViewScrollChanged;
        boolean mViewVisibilityChanged;
        final IWindow mWindow;
        WindowId mWindowId;
        int mWindowLeft;
        final IBinder mWindowToken;
        int mWindowTop;
        int mWindowVisibility;
        int mDisplayState = 0;
        final Rect mOverscanInsets = new Rect();
        final Rect mContentInsets = new Rect();
        final Rect mVisibleInsets = new Rect();
        final Rect mStableInsets = new Rect();
        final ViewTreeObserver.InternalInsetsInfo mGivenInternalInsets = new ViewTreeObserver.InternalInsetsInfo();
        final ArrayList<View> mScrollContainers = new ArrayList<>();
        final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();
        boolean mSetIgnoreDirtyState = false;
        final int[] mTransparentLocation = new int[2];
        final int[] mInvalidateChildLocation = new int[2];
        final int[] mTmpLocation = new int[2];
        final float[] mTmpTransformLocation = new float[2];
        final ViewTreeObserver mTreeObserver = new ViewTreeObserver();
        final Rect mTmpInvalRect = new Rect();
        final RectF mTmpTransformRect = new RectF();
        final RectF mTmpTransformRect1 = new RectF();
        final List<RectF> mTmpRectList = new ArrayList();
        final Matrix mTmpMatrix = new Matrix();
        final Transformation mTmpTransformation = new Transformation();
        final Outline mTmpOutline = new Outline();
        final ArrayList<View> mTempArrayList = new ArrayList<>(24);
        int mAccessibilityWindowId = Integer.MAX_VALUE;
        boolean mDebugLayout = SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
        final Point mPoint = new Point();

        interface Callbacks {
            boolean performHapticFeedback(int i, boolean z);

            void playSoundEffect(int i);
        }

        static class InvalidateInfo {
            private static final int POOL_LIMIT = 10;
            private static final Pools.SynchronizedPool<InvalidateInfo> sPool = new Pools.SynchronizedPool<>(10);
            int bottom;
            int left;
            int right;
            View target;
            int top;

            InvalidateInfo() {
            }

            public static InvalidateInfo obtain() {
                InvalidateInfo instance = sPool.acquire();
                return instance != null ? instance : new InvalidateInfo();
            }

            public void recycle() {
                this.target = null;
                sPool.release(this);
            }
        }

        AttachInfo(IWindowSession session, IWindow window, Display display, ViewRootImpl viewRootImpl, Handler handler, Callbacks effectPlayer) {
            this.mSession = session;
            this.mWindow = window;
            this.mWindowToken = window.asBinder();
            this.mDisplay = display;
            this.mViewRootImpl = viewRootImpl;
            this.mHandler = handler;
            this.mRootCallbacks = effectPlayer;
        }
    }

    private static class ScrollabilityCache implements Runnable {
        public static final int FADING = 2;
        public static final int OFF = 0;
        public static final int ON = 1;
        private static final float[] OPAQUE = {255.0f};
        private static final float[] TRANSPARENT = {0.0f};
        public boolean fadeScrollBars;
        public long fadeStartTime;
        public int fadingEdgeLength;
        public View host;
        public float[] interpolatorValues;
        private int mLastColor;
        public ScrollBarDrawable scrollBar;
        public int scrollBarSize;
        public final Interpolator scrollBarInterpolator = new Interpolator(1, 2);
        public int state = 0;
        public int scrollBarDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
        public int scrollBarFadeDuration = ViewConfiguration.getScrollBarFadeDuration();
        public final Paint paint = new Paint();
        public final Matrix matrix = new Matrix();
        public Shader shader = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -16777216, 0, Shader.TileMode.CLAMP);

        public ScrollabilityCache(ViewConfiguration configuration, View host) {
            this.fadingEdgeLength = configuration.getScaledFadingEdgeLength();
            this.scrollBarSize = configuration.getScaledScrollBarSize();
            this.paint.setShader(this.shader);
            this.paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            this.host = host;
        }

        public void setFadeColor(int color) {
            if (color != this.mLastColor) {
                this.mLastColor = color;
                if (color != 0) {
                    this.shader = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, (-16777216) | color, color & 16777215, Shader.TileMode.CLAMP);
                    this.paint.setShader(this.shader);
                    this.paint.setXfermode(null);
                } else {
                    this.shader = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -16777216, 0, Shader.TileMode.CLAMP);
                    this.paint.setShader(this.shader);
                    this.paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                }
            }
        }

        @Override
        public void run() {
            long now = AnimationUtils.currentAnimationTimeMillis();
            if (now >= this.fadeStartTime) {
                int nextFrame = (int) now;
                Interpolator interpolator = this.scrollBarInterpolator;
                int framesCount = 0 + 1;
                interpolator.setKeyFrame(0, nextFrame, OPAQUE);
                interpolator.setKeyFrame(framesCount, nextFrame + this.scrollBarFadeDuration, TRANSPARENT);
                this.state = 2;
                this.host.invalidate(true);
            }
        }
    }

    private class SendViewScrolledAccessibilityEvent implements Runnable {
        public volatile boolean mIsPending;

        private SendViewScrolledAccessibilityEvent() {
        }

        @Override
        public void run() {
            View.this.sendAccessibilityEvent(4096);
            this.mIsPending = false;
        }
    }

    public static class AccessibilityDelegate {
        public void sendAccessibilityEvent(View host, int eventType) {
            host.sendAccessibilityEventInternal(eventType);
        }

        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            return host.performAccessibilityActionInternal(action, args);
        }

        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
            host.sendAccessibilityEventUncheckedInternal(event);
        }

        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            return host.dispatchPopulateAccessibilityEventInternal(event);
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            host.onPopulateAccessibilityEventInternal(event);
        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            host.onInitializeAccessibilityEventInternal(event);
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            host.onInitializeAccessibilityNodeInfoInternal(info);
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            return host.onRequestSendAccessibilityEventInternal(child, event);
        }

        public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
            return null;
        }

        public AccessibilityNodeInfo createAccessibilityNodeInfo(View host) {
            return host.createAccessibilityNodeInfoInternal();
        }
    }

    private class MatchIdPredicate implements Predicate<View> {
        public int mId;

        private MatchIdPredicate() {
        }

        @Override
        public boolean apply(View view) {
            return view.mID == this.mId;
        }
    }

    private class MatchLabelForPredicate implements Predicate<View> {
        private int mLabeledId;

        private MatchLabelForPredicate() {
        }

        @Override
        public boolean apply(View view) {
            return view.mLabelForId == this.mLabeledId;
        }
    }

    private class SendViewStateChangedAccessibilityEvent implements Runnable {
        private int mChangeTypes;
        private long mLastEventTimeMillis;
        private boolean mPosted;
        private boolean mPostedWithDelay;

        private SendViewStateChangedAccessibilityEvent() {
            this.mChangeTypes = 0;
        }

        @Override
        public void run() {
            this.mPosted = false;
            this.mPostedWithDelay = false;
            this.mLastEventTimeMillis = SystemClock.uptimeMillis();
            if (AccessibilityManager.getInstance(View.this.mContext).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain();
                event.setEventType(2048);
                event.setContentChangeTypes(this.mChangeTypes);
                View.this.sendAccessibilityEventUnchecked(event);
            }
            this.mChangeTypes = 0;
        }

        public void runOrPost(int changeType) {
            this.mChangeTypes |= changeType;
            if (View.this.inLiveRegion()) {
                if (this.mPostedWithDelay) {
                    View.this.removeCallbacks(this);
                    this.mPostedWithDelay = false;
                }
                if (!this.mPosted) {
                    View.this.post(this);
                    this.mPosted = true;
                    return;
                }
                return;
            }
            if (!this.mPosted) {
                long timeSinceLastMillis = SystemClock.uptimeMillis() - this.mLastEventTimeMillis;
                long minEventIntevalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();
                if (timeSinceLastMillis >= minEventIntevalMillis) {
                    View.this.removeCallbacks(this);
                    run();
                } else {
                    View.this.postDelayed(this, minEventIntevalMillis - timeSinceLastMillis);
                    this.mPostedWithDelay = true;
                }
            }
        }
    }

    private boolean inLiveRegion() {
        if (getAccessibilityLiveRegion() != 0) {
            return true;
        }
        for (ViewParent parent = getParent(); parent instanceof View; parent = parent.getParent()) {
            if (((View) parent).getAccessibilityLiveRegion() != 0) {
                return true;
            }
        }
        return false;
    }

    private static void dumpFlags() {
        HashMap<String, String> found = Maps.newHashMap();
        try {
            Field[] arr$ = View.class.getDeclaredFields();
            for (Field field : arr$) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    if (field.getType().equals(Integer.TYPE)) {
                        int value = field.getInt(null);
                        dumpFlag(found, field.getName(), value);
                    } else if (field.getType().equals(int[].class)) {
                        int[] values = (int[]) field.get(null);
                        for (int i = 0; i < values.length; i++) {
                            dumpFlag(found, field.getName() + "[" + i + "]", values[i]);
                        }
                    }
                }
            }
            ArrayList<String> keys = Lists.newArrayList();
            keys.addAll(found.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                Log.d(VIEW_LOG_TAG, found.get(key));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void dumpFlag(HashMap<String, String> found, String name, int value) {
        String bits = String.format("%32s", Integer.toBinaryString(value)).replace('0', ' ');
        int prefix = name.indexOf(95);
        String key = (prefix > 0 ? name.substring(0, prefix) : name) + bits + name;
        String output = bits + " " + name;
        found.put(key, output);
    }
}
