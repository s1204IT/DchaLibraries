package android.view;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.app.Instrumentation;
import android.bluetooth.mesh.MeshConstants;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.media.TtmlUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.SettingsStringUtil;
import android.security.keystore.KeyProperties;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.LongSparseLongArray;
import android.util.Pools;
import android.util.Property;
import android.util.SparseArray;
import android.util.StateSet;
import android.util.SuperNotCalledException;
import android.util.TypedValue;
import android.view.AccessibilityIterators;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.SurfaceControl;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ScrollBarDrawable;
import com.android.internal.R;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.view.TooltipPopup;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.widget.ScrollBarUtils;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.mediatek.view.ViewDebugManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class View implements Drawable.Callback, KeyEvent.Callback, AccessibilityEventSource {
    public static final int ACCESSIBILITY_CURSOR_POSITION_UNDEFINED = -1;
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 2;
    static final int ACCESSIBILITY_LIVE_REGION_DEFAULT = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    static final int ALL_RTL_PROPERTIES_RESOLVED = 1610678816;
    public static final int AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 1;
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE = "creditCardExpirationDate";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY = "creditCardExpirationDay";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH = "creditCardExpirationMonth";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR = "creditCardExpirationYear";
    public static final String AUTOFILL_HINT_CREDIT_CARD_NUMBER = "creditCardNumber";
    public static final String AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE = "creditCardSecurityCode";
    public static final String AUTOFILL_HINT_EMAIL_ADDRESS = "emailAddress";
    public static final String AUTOFILL_HINT_NAME = "name";
    public static final String AUTOFILL_HINT_PASSWORD = "password";
    public static final String AUTOFILL_HINT_PHONE = "phone";
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS = "postalAddress";
    public static final String AUTOFILL_HINT_POSTAL_CODE = "postalCode";
    public static final String AUTOFILL_HINT_USERNAME = "username";
    public static final int AUTOFILL_TYPE_DATE = 4;
    public static final int AUTOFILL_TYPE_LIST = 3;
    public static final int AUTOFILL_TYPE_NONE = 0;
    public static final int AUTOFILL_TYPE_TEXT = 1;
    public static final int AUTOFILL_TYPE_TOGGLE = 2;
    static final int CLICKABLE = 16384;
    static final int CONTEXT_CLICKABLE = 8388608;
    private static final boolean DBG = false;
    static final int DEBUG_CORNERS_SIZE_DIP = 8;
    public static final String DEBUG_LAYOUT_PROPERTY = "debug.layout";
    static final int DISABLED = 32;
    public static final int DRAG_FLAG_GLOBAL = 256;
    public static final int DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION = 64;
    public static final int DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION = 128;
    public static final int DRAG_FLAG_GLOBAL_URI_READ = 1;
    public static final int DRAG_FLAG_GLOBAL_URI_WRITE = 2;
    public static final int DRAG_FLAG_OPAQUE = 512;
    static final int DRAG_MASK = 3;
    static final int DRAWING_CACHE_ENABLED = 32768;

    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_AUTO = 0;

    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_HIGH = 1048576;

    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_LOW = 524288;
    static final int DRAWING_CACHE_QUALITY_MASK = 1572864;
    static final int DRAW_MASK = 128;
    static final int DUPLICATE_PARENT_STATE = 4194304;
    static final int ENABLED = 0;
    static final int ENABLED_MASK = 32;
    static final int FADING_EDGE_HORIZONTAL = 4096;
    static final int FADING_EDGE_MASK = 12288;
    static final int FADING_EDGE_NONE = 0;
    static final int FADING_EDGE_VERTICAL = 8192;
    static final int FILTER_TOUCHES_WHEN_OBSCURED = 1024;
    public static final int FIND_VIEWS_WITH_ACCESSIBILITY_NODE_PROVIDERS = 4;
    public static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 2;
    public static final int FIND_VIEWS_WITH_TEXT = 1;
    private static final int FITS_SYSTEM_WINDOWS = 2;
    public static final int FOCUSABLE = 1;
    public static final int FOCUSABLES_ALL = 0;
    public static final int FOCUSABLES_TOUCH_MODE = 1;
    public static final int FOCUSABLE_AUTO = 16;
    static final int FOCUSABLE_IN_TOUCH_MODE = 262144;
    private static final int FOCUSABLE_MASK = 17;
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
    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0;
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 2;
    public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 8;
    public static final int IMPORTANT_FOR_AUTOFILL_YES = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int INVISIBLE = 4;
    public static final int KEEP_SCREEN_ON = 67108864;
    public static final int LAST_APP_AUTOFILL_ID = 1073741823;
    public static final int LAYER_TYPE_HARDWARE = 2;
    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    private static final int LAYOUT_DIRECTION_DEFAULT = 2;
    public static final int LAYOUT_DIRECTION_INHERIT = 2;
    public static final int LAYOUT_DIRECTION_LOCALE = 3;
    public static final int LAYOUT_DIRECTION_LTR = 0;
    static final int LAYOUT_DIRECTION_RESOLVED_DEFAULT = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public static final int LAYOUT_DIRECTION_UNDEFINED = -1;
    static final int LONG_CLICKABLE = 2097152;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_SIZE_MASK = 16777215;
    public static final int MEASURED_STATE_MASK = -16777216;
    public static final int MEASURED_STATE_TOO_SMALL = 16777216;
    public static final int NAVIGATION_BAR_TRANSIENT = 134217728;
    public static final int NAVIGATION_BAR_TRANSLUCENT = Integer.MIN_VALUE;
    public static final int NAVIGATION_BAR_TRANSPARENT = 32768;
    public static final int NAVIGATION_BAR_UNHIDE = 536870912;
    public static final int NOT_FOCUSABLE = 0;
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
    static final int PFLAG2_TEXT_ALIGNMENT_MASK = 57344;
    static final int PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT = 13;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED = 65536;
    private static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT = 131072;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK = 917504;
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT = 17;
    static final int PFLAG2_TEXT_DIRECTION_MASK = 448;
    static final int PFLAG2_TEXT_DIRECTION_MASK_SHIFT = 6;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED = 512;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT = 1024;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK = 7168;
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT = 10;
    static final int PFLAG2_VIEW_QUICK_REJECTED = 268435456;
    private static final int PFLAG3_ACCESSIBILITY_HEADING = Integer.MIN_VALUE;
    private static final int PFLAG3_AGGREGATED_VISIBLE = 536870912;
    static final int PFLAG3_APPLYING_INSETS = 32;
    static final int PFLAG3_ASSIST_BLOCKED = 16384;
    private static final int PFLAG3_AUTOFILLID_EXPLICITLY_SET = 1073741824;
    static final int PFLAG3_CALLED_SUPER = 16;
    private static final int PFLAG3_CLUSTER = 32768;
    private static final int PFLAG3_FINGER_DOWN = 131072;
    static final int PFLAG3_FITTING_SYSTEM_WINDOWS = 64;
    private static final int PFLAG3_FOCUSED_BY_DEFAULT = 262144;
    private static final int PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED = 16777216;
    static final int PFLAG3_IMPORTANT_FOR_AUTOFILL_MASK = 7864320;
    static final int PFLAG3_IMPORTANT_FOR_AUTOFILL_SHIFT = 19;
    private static final int PFLAG3_IS_AUTOFILLED = 65536;
    static final int PFLAG3_IS_LAID_OUT = 4;
    static final int PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT = 8;
    static final int PFLAG3_NESTED_SCROLLING_ENABLED = 128;
    static final int PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT = 134217728;
    private static final int PFLAG3_NO_REVEAL_ON_FOCUS = 67108864;
    private static final int PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE = 8388608;
    private static final int PFLAG3_SCREEN_READER_FOCUSABLE = 268435456;
    static final int PFLAG3_SCROLL_INDICATOR_BOTTOM = 512;
    static final int PFLAG3_SCROLL_INDICATOR_END = 8192;
    static final int PFLAG3_SCROLL_INDICATOR_LEFT = 1024;
    static final int PFLAG3_SCROLL_INDICATOR_RIGHT = 2048;
    static final int PFLAG3_SCROLL_INDICATOR_START = 4096;
    static final int PFLAG3_SCROLL_INDICATOR_TOP = 256;
    static final int PFLAG3_TEMPORARY_DETACH = 33554432;
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
    private static final int PFLAG_NOTIFY_AUTOFILL_MANAGER_ON_CLICK = 536870912;
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
    private static final int PROVIDER_BACKGROUND = 0;
    private static final int PROVIDER_BOUNDS = 2;
    private static final int PROVIDER_NONE = 1;
    private static final int PROVIDER_PADDED_BOUNDS = 3;
    public static final int PUBLIC_STATUS_BAR_VISIBILITY_MASK = 16375;
    static final int SAVE_DISABLED = 65536;
    static final int SAVE_DISABLED_MASK = 65536;
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
    static final int SCROLL_INDICATORS_NONE = 0;
    static final int SCROLL_INDICATORS_PFLAG3_MASK = 16128;
    static final int SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT = 8;
    public static final int SCROLL_INDICATOR_BOTTOM = 2;
    public static final int SCROLL_INDICATOR_END = 32;
    public static final int SCROLL_INDICATOR_LEFT = 4;
    public static final int SCROLL_INDICATOR_RIGHT = 8;
    public static final int SCROLL_INDICATOR_START = 16;
    public static final int SCROLL_INDICATOR_TOP = 1;
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

    @Deprecated
    public static final int STATUS_BAR_HIDDEN = 1;
    public static final int STATUS_BAR_TRANSIENT = 67108864;
    public static final int STATUS_BAR_TRANSLUCENT = 1073741824;
    public static final int STATUS_BAR_TRANSPARENT = 8;
    public static final int STATUS_BAR_UNHIDE = 268435456;

    @Deprecated
    public static final int STATUS_BAR_VISIBLE = 0;
    public static final int SYSTEM_UI_CLEARABLE_FLAGS = 7;
    public static final int SYSTEM_UI_FLAG_FORCE_HIDE_NAVIGATION = 0;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 2048;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_GESTURE_ISOLATED = 16777216;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 16;
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 8192;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_LAYOUT_FLAGS = 1536;
    private static final int SYSTEM_UI_RESERVED_LEGACY1 = 16384;
    private static final int SYSTEM_UI_RESERVED_LEGACY2 = 65536;
    public static final int SYSTEM_UI_TRANSPARENT = 32776;
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
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 6;
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 7;
    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_LOCALE = 5;
    public static final int TEXT_DIRECTION_LTR = 3;
    static final int TEXT_DIRECTION_RESOLVED_DEFAULT = 1;
    public static final int TEXT_DIRECTION_RTL = 4;
    static final int TOOLTIP = 1073741824;
    private static final int UNDEFINED_PADDING = Integer.MIN_VALUE;
    protected static final String VIEW_LOG_TAG = "View";
    static final int VISIBILITY_MASK = 12;
    public static final int VISIBLE = 0;
    static final int WILL_NOT_CACHE_DRAWING = 131072;
    static final int WILL_NOT_DRAW = 128;
    private static SparseArray<String> mAttributeMap;
    private static boolean sAcceptZeroSizeDragShadow;
    private static boolean sAlwaysAssignFocus;
    private static boolean sAutoFocusableOffUIThreadWontNotifyParents;
    private static boolean sCanFocusZeroSized;
    static boolean sCascadedDragDrop;
    private static Paint sDebugPaint;
    static boolean sHasFocusableExcludeAutoFocusable;
    private static int sNextAccessibilityViewId;
    protected static boolean sPreserveMarginParamsInLayoutParamConversion;
    private static boolean sThrowOnInvalidFloatProperties;
    private static boolean sUseDefaultFocusHighlight;
    private int mAccessibilityCursorPosition;
    AccessibilityDelegate mAccessibilityDelegate;
    private CharSequence mAccessibilityPaneTitle;
    private int mAccessibilityTraversalAfterId;
    private int mAccessibilityTraversalBeforeId;
    private int mAccessibilityViewId;
    private ViewPropertyAnimator mAnimator;
    AttachInfo mAttachInfo;

    @ViewDebug.ExportedProperty(category = "attributes", hasAdjacentMapping = true)
    public String[] mAttributes;
    private String[] mAutofillHints;
    private AutofillId mAutofillId;
    private int mAutofillViewId;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "bg_")
    private Drawable mBackground;
    private RenderNode mBackgroundRenderNode;
    private int mBackgroundResource;
    private boolean mBackgroundSizeChanged;
    private TintInfo mBackgroundTint;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mBottom;
    public boolean mCachingFailed;

    @ViewDebug.ExportedProperty(category = "drawing")
    Rect mClipBounds;
    private CharSequence mContentDescription;

    @ViewDebug.ExportedProperty(deepExport = true)
    protected Context mContext;
    protected Animation mCurrentAnimation;
    private Drawable mDefaultFocusHighlight;
    private Drawable mDefaultFocusHighlightCache;
    boolean mDefaultFocusHighlightEnabled;
    private boolean mDefaultFocusHighlightSizeChanged;
    private int[] mDrawableState;
    private Bitmap mDrawingCache;
    private int mDrawingCacheBackgroundColor;
    private ViewTreeObserver mFloatingTreeObserver;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "fg_")
    private ForegroundInfo mForegroundInfo;
    private ArrayList<FrameMetricsObserver> mFrameMetricsObservers;
    GhostView mGhostView;
    private boolean mHasPerformedLongPress;

    @ViewDebug.ExportedProperty(resolveId = true)
    int mID;
    private boolean mIgnoreNextUpEvent;
    private boolean mInContextButtonPress;
    protected final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    private SparseArray<Object> mKeyedTags;
    private int mLabelForId;
    private boolean mLastIsOpaque;
    Paint mLayerPaint;

    @ViewDebug.ExportedProperty(category = "drawing", mapping = {@ViewDebug.IntToString(from = 0, to = KeyProperties.DIGEST_NONE), @ViewDebug.IntToString(from = 1, to = "SOFTWARE"), @ViewDebug.IntToString(from = 2, to = "HARDWARE")})
    int mLayerType;
    private Insets mLayoutInsets;
    protected ViewGroup.LayoutParams mLayoutParams;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mLeft;
    private boolean mLeftPaddingDefined;
    ListenerInfo mListenerInfo;
    private float mLongClickX;
    private float mLongClickY;
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
    int mNextClusterForwardId;
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
    private PointerIcon mPointerIcon;

    @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 4096, mask = 4096, name = "FORCE_LAYOUT"), @ViewDebug.FlagToString(equals = 8192, mask = 8192, name = "LAYOUT_REQUIRED"), @ViewDebug.FlagToString(equals = 32768, mask = 32768, name = "DRAWING_CACHE_INVALID", outputIf = false), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "DRAWN", outputIf = true), @ViewDebug.FlagToString(equals = 32, mask = 32, name = "NOT_DRAWN", outputIf = false), @ViewDebug.FlagToString(equals = 4194304, mask = 6291456, name = "DIRTY_OPAQUE"), @ViewDebug.FlagToString(equals = 2097152, mask = 6291456, name = "DIRTY")}, formatToHexString = true)
    public int mPrivateFlags;
    int mPrivateFlags2;
    int mPrivateFlags3;
    boolean mRecreateDisplayList;
    final RenderNode mRenderNode;
    private final Resources mResources;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mRight;
    private boolean mRightPaddingDefined;
    private RoundScrollbarRenderer mRoundScrollbarRenderer;
    private HandlerActionQueue mRunQueue;
    private ScrollabilityCache mScrollCache;
    private Drawable mScrollIndicatorDrawable;

    @ViewDebug.ExportedProperty(category = "scrolling")
    protected int mScrollX;

    @ViewDebug.ExportedProperty(category = "scrolling")
    protected int mScrollY;
    private SendViewScrolledAccessibilityEvent mSendViewScrolledAccessibilityEvent;
    private boolean mSendingHoverAccessibilityEvents;
    String mStartActivityRequestWho;
    private StateListAnimator mStateListAnimator;

    @ViewDebug.ExportedProperty(flagMapping = {@ViewDebug.FlagToString(equals = 1, mask = 1, name = "LOW_PROFILE"), @ViewDebug.FlagToString(equals = 2, mask = 2, name = "HIDE_NAVIGATION"), @ViewDebug.FlagToString(equals = 4, mask = 4, name = "FULLSCREEN"), @ViewDebug.FlagToString(equals = 256, mask = 256, name = "LAYOUT_STABLE"), @ViewDebug.FlagToString(equals = 512, mask = 512, name = "LAYOUT_HIDE_NAVIGATION"), @ViewDebug.FlagToString(equals = 1024, mask = 1024, name = "LAYOUT_FULLSCREEN"), @ViewDebug.FlagToString(equals = 2048, mask = 2048, name = "IMMERSIVE"), @ViewDebug.FlagToString(equals = 4096, mask = 4096, name = "IMMERSIVE_STICKY"), @ViewDebug.FlagToString(equals = 8192, mask = 8192, name = "LIGHT_STATUS_BAR"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "LIGHT_NAVIGATION_BAR"), @ViewDebug.FlagToString(equals = 65536, mask = 65536, name = "STATUS_BAR_DISABLE_EXPAND"), @ViewDebug.FlagToString(equals = 131072, mask = 131072, name = "STATUS_BAR_DISABLE_NOTIFICATION_ICONS"), @ViewDebug.FlagToString(equals = 262144, mask = 262144, name = "STATUS_BAR_DISABLE_NOTIFICATION_ALERTS"), @ViewDebug.FlagToString(equals = 524288, mask = 524288, name = "STATUS_BAR_DISABLE_NOTIFICATION_TICKER"), @ViewDebug.FlagToString(equals = 1048576, mask = 1048576, name = "STATUS_BAR_DISABLE_SYSTEM_INFO"), @ViewDebug.FlagToString(equals = 2097152, mask = 2097152, name = "STATUS_BAR_DISABLE_HOME"), @ViewDebug.FlagToString(equals = 4194304, mask = 4194304, name = "STATUS_BAR_DISABLE_BACK"), @ViewDebug.FlagToString(equals = 8388608, mask = 8388608, name = "STATUS_BAR_DISABLE_CLOCK"), @ViewDebug.FlagToString(equals = 16777216, mask = 16777216, name = "STATUS_BAR_DISABLE_RECENT"), @ViewDebug.FlagToString(equals = 33554432, mask = 33554432, name = "STATUS_BAR_DISABLE_SEARCH"), @ViewDebug.FlagToString(equals = 67108864, mask = 67108864, name = "STATUS_BAR_TRANSIENT"), @ViewDebug.FlagToString(equals = 134217728, mask = 134217728, name = "NAVIGATION_BAR_TRANSIENT"), @ViewDebug.FlagToString(equals = 268435456, mask = 268435456, name = "STATUS_BAR_UNHIDE"), @ViewDebug.FlagToString(equals = 536870912, mask = 536870912, name = "NAVIGATION_BAR_UNHIDE"), @ViewDebug.FlagToString(equals = 1073741824, mask = 1073741824, name = "STATUS_BAR_TRANSLUCENT"), @ViewDebug.FlagToString(equals = Integer.MIN_VALUE, mask = Integer.MIN_VALUE, name = "NAVIGATION_BAR_TRANSLUCENT"), @ViewDebug.FlagToString(equals = 32768, mask = 32768, name = "NAVIGATION_BAR_TRANSPARENT"), @ViewDebug.FlagToString(equals = 8, mask = 8, name = "STATUS_BAR_TRANSPARENT")}, formatToHexString = true)
    int mSystemUiVisibility;
    protected Object mTag;
    private int[] mTempNestedScrollConsumed;
    TooltipInfo mTooltipInfo;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    protected int mTop;
    private TouchDelegate mTouchDelegate;
    private int mTouchSlop;
    public TransformationInfo mTransformationInfo;
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
    private Handler mVisibilityChangeForAutofillHandler;
    int mWindowAttachCount;
    public static boolean DEBUG_DRAW = false;
    public static boolean mDebugViewAttributes = false;
    private static final int[] AUTOFILL_HIGHLIGHT_ATTR = {16844136};
    private static boolean sCompatibilityDone = false;
    private static boolean sUseBrokenMakeMeasureSpec = false;
    static boolean sUseZeroUnspecifiedMeasureSpec = false;
    private static boolean sIgnoreMeasureCache = false;
    private static boolean sAlwaysRemeasureExactly = false;
    private static boolean sLayoutParamsAlwaysChanged = false;
    static boolean sTextureViewIgnoresDrawableSetters = false;
    private static final int[] VISIBILITY_FLAGS = {0, 4, 8};
    private static final int[] DRAWING_CACHE_QUALITY_FLAGS = {0, 524288, 1048576};
    protected static final int[] EMPTY_STATE_SET = StateSet.get(0);
    protected static final int[] WINDOW_FOCUSED_STATE_SET = StateSet.get(1);
    protected static final int[] SELECTED_STATE_SET = StateSet.get(2);
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(3);
    protected static final int[] FOCUSED_STATE_SET = StateSet.get(4);
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(5);
    protected static final int[] FOCUSED_SELECTED_STATE_SET = StateSet.get(6);
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(7);
    protected static final int[] ENABLED_STATE_SET = StateSet.get(8);
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(9);
    protected static final int[] ENABLED_SELECTED_STATE_SET = StateSet.get(10);
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(11);
    protected static final int[] ENABLED_FOCUSED_STATE_SET = StateSet.get(12);
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(13);
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(14);
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(15);
    protected static final int[] PRESSED_STATE_SET = StateSet.get(16);
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(17);
    protected static final int[] PRESSED_SELECTED_STATE_SET = StateSet.get(18);
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(19);
    protected static final int[] PRESSED_FOCUSED_STATE_SET = StateSet.get(20);
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(21);
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET = StateSet.get(22);
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(23);
    protected static final int[] PRESSED_ENABLED_STATE_SET = StateSet.get(24);
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(25);
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET = StateSet.get(26);
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(27);
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET = StateSet.get(28);
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(29);
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(30);
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(31);
    static final int DEBUG_CORNERS_COLOR = Color.rgb(63, 127, 255);
    static final ThreadLocal<Rect> sThreadLocal = new ThreadLocal<>();
    private static final int[] LAYOUT_DIRECTION_FLAGS = {0, 1, 2, 3};
    private static final int[] PFLAG2_TEXT_DIRECTION_FLAGS = {0, 64, 128, 192, 256, 320, MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, 448};
    private static final int[] PFLAG2_TEXT_ALIGNMENT_FLAGS = {0, 8192, 16384, 24576, 32768, 40960, 49152};
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    public static final Property<View, Float> ALPHA = new FloatProperty<View>("alpha") {
        @Override
        public void setValue(View view, float f) {
            view.setAlpha(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getAlpha());
        }
    };
    public static final Property<View, Float> TRANSLATION_X = new FloatProperty<View>("translationX") {
        @Override
        public void setValue(View view, float f) {
            view.setTranslationX(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getTranslationX());
        }
    };
    public static final Property<View, Float> TRANSLATION_Y = new FloatProperty<View>("translationY") {
        @Override
        public void setValue(View view, float f) {
            view.setTranslationY(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getTranslationY());
        }
    };
    public static final Property<View, Float> TRANSLATION_Z = new FloatProperty<View>("translationZ") {
        @Override
        public void setValue(View view, float f) {
            view.setTranslationZ(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getTranslationZ());
        }
    };
    public static final Property<View, Float> X = new FloatProperty<View>("x") {
        @Override
        public void setValue(View view, float f) {
            view.setX(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getX());
        }
    };
    public static final Property<View, Float> Y = new FloatProperty<View>("y") {
        @Override
        public void setValue(View view, float f) {
            view.setY(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getY());
        }
    };
    public static final Property<View, Float> Z = new FloatProperty<View>("z") {
        @Override
        public void setValue(View view, float f) {
            view.setZ(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getZ());
        }
    };
    public static final Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
        @Override
        public void setValue(View view, float f) {
            view.setRotation(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getRotation());
        }
    };
    public static final Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
        @Override
        public void setValue(View view, float f) {
            view.setRotationX(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getRotationX());
        }
    };
    public static final Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
        @Override
        public void setValue(View view, float f) {
            view.setRotationY(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getRotationY());
        }
    };
    public static final Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
        @Override
        public void setValue(View view, float f) {
            view.setScaleX(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getScaleX());
        }
    };
    public static final Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
        @Override
        public void setValue(View view, float f) {
            view.setScaleY(f);
        }

        @Override
        public Float get(View view) {
            return Float.valueOf(view.getScaleY());
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillImportance {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface DrawingCacheQuality {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FindViewFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusDirection {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRealDirection {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Focusable {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusableMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutDir {
    }

    public interface OnApplyWindowInsetsListener {
        WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets);
    }

    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View view);

        void onViewDetachedFromWindow(View view);
    }

    public interface OnCapturedPointerListener {
        boolean onCapturedPointer(View view, MotionEvent motionEvent);
    }

    public interface OnClickListener {
        void onClick(View view);
    }

    public interface OnContextClickListener {
        boolean onContextClick(View view);
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

    public interface OnUnhandledKeyEventListener {
        boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolvedLayoutDir {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollBarStyle {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollIndicators {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Visibility {
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

    static class TintInfo {
        boolean mHasTintList;
        boolean mHasTintMode;
        ColorStateList mTintList;
        PorterDuff.Mode mTintMode;

        TintInfo() {
        }
    }

    private static class ForegroundInfo {
        private boolean mBoundsChanged;
        private Drawable mDrawable;
        private int mGravity;
        private boolean mInsidePadding;
        private final Rect mOverlayBounds;
        private final Rect mSelfBounds;
        private TintInfo mTintInfo;

        private ForegroundInfo() {
            this.mGravity = 119;
            this.mInsidePadding = true;
            this.mBoundsChanged = true;
            this.mSelfBounds = new Rect();
            this.mOverlayBounds = new Rect();
        }
    }

    static class ListenerInfo {
        OnApplyWindowInsetsListener mOnApplyWindowInsetsListener;
        private CopyOnWriteArrayList<OnAttachStateChangeListener> mOnAttachStateChangeListeners;
        OnCapturedPointerListener mOnCapturedPointerListener;
        public OnClickListener mOnClickListener;
        protected OnContextClickListener mOnContextClickListener;
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
        private ArrayList<OnUnhandledKeyEventListener> mUnhandledKeyListeners;

        ListenerInfo() {
        }
    }

    private static class TooltipInfo {
        int mAnchorX;
        int mAnchorY;
        Runnable mHideTooltipRunnable;
        int mHoverSlop;
        Runnable mShowTooltipRunnable;
        boolean mTooltipFromLongClick;
        TooltipPopup mTooltipPopup;
        CharSequence mTooltipText;

        private TooltipInfo() {
        }

        private boolean updateAnchorPos(MotionEvent motionEvent) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            if (Math.abs(x - this.mAnchorX) <= this.mHoverSlop && Math.abs(y - this.mAnchorY) <= this.mHoverSlop) {
                return false;
            }
            this.mAnchorX = x;
            this.mAnchorY = y;
            return true;
        }

        private void clearAnchorPos() {
            this.mAnchorX = Integer.MAX_VALUE;
            this.mAnchorY = Integer.MAX_VALUE;
        }
    }

    public View(Context context) {
        InputEventConsistencyVerifier inputEventConsistencyVerifier;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        this.mCurrentAnimation = null;
        this.mRecreateDisplayList = false;
        this.mID = -1;
        this.mAutofillViewId = -1;
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
        this.mLongClickX = Float.NaN;
        this.mLongClickY = Float.NaN;
        this.mDrawableState = null;
        this.mOutlineProvider = ViewOutlineProvider.BACKGROUND;
        this.mNextFocusLeftId = -1;
        this.mNextFocusRightId = -1;
        this.mNextFocusUpId = -1;
        this.mNextFocusDownId = -1;
        this.mNextFocusForwardId = -1;
        this.mNextClusterForwardId = -1;
        this.mDefaultFocusHighlightEnabled = true;
        this.mPendingCheckForTap = null;
        this.mTouchDelegate = null;
        this.mDrawingCacheBackgroundColor = 0;
        this.mAnimator = null;
        this.mLayerType = 0;
        if (!InputEventConsistencyVerifier.isInstrumentationEnabled()) {
            inputEventConsistencyVerifier = null;
        } else {
            inputEventConsistencyVerifier = new InputEventConsistencyVerifier(this, 0);
        }
        this.mInputEventConsistencyVerifier = inputEventConsistencyVerifier;
        this.mContext = context;
        this.mResources = context != null ? context.getResources() : null;
        this.mViewFlags = 402653200;
        this.mPrivateFlags2 = 140296;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOverScrollMode(1);
        this.mUserPaddingStart = Integer.MIN_VALUE;
        this.mUserPaddingEnd = Integer.MIN_VALUE;
        this.mRenderNode = RenderNode.create(getClass().getName(), this);
        if (!sCompatibilityDone && context != null) {
            int i = context.getApplicationInfo().targetSdkVersion;
            if (i > 17) {
                z = false;
            } else {
                z = true;
            }
            sUseBrokenMakeMeasureSpec = z;
            if (i >= 19) {
                z2 = false;
            } else {
                z2 = true;
            }
            sIgnoreMeasureCache = z2;
            if (i >= 23) {
                z3 = false;
            } else {
                z3 = true;
            }
            Canvas.sCompatibilityRestore = z3;
            if (i >= 26) {
                z4 = false;
            } else {
                z4 = true;
            }
            Canvas.sCompatibilitySetBitmap = z4;
            Canvas.setCompatibilityVersion(i);
            if (i >= 23) {
                z5 = false;
            } else {
                z5 = true;
            }
            sUseZeroUnspecifiedMeasureSpec = z5;
            if (i > 23) {
                z6 = false;
            } else {
                z6 = true;
            }
            sAlwaysRemeasureExactly = z6;
            if (i > 23) {
                z7 = false;
            } else {
                z7 = true;
            }
            sLayoutParamsAlwaysChanged = z7;
            if (i > 23) {
                z8 = false;
            } else {
                z8 = true;
            }
            sTextureViewIgnoresDrawableSetters = z8;
            if (i < 24) {
                z9 = false;
            } else {
                z9 = true;
            }
            sPreserveMarginParamsInLayoutParamConversion = z9;
            if (i >= 24) {
                z10 = false;
            } else {
                z10 = true;
            }
            sCascadedDragDrop = z10;
            if (i >= 26) {
                z11 = false;
            } else {
                z11 = true;
            }
            sHasFocusableExcludeAutoFocusable = z11;
            if (i >= 26) {
                z12 = false;
            } else {
                z12 = true;
            }
            sAutoFocusableOffUIThreadWontNotifyParents = z12;
            sUseDefaultFocusHighlight = context.getResources().getBoolean(R.bool.config_useDefaultFocusHighlight);
            if (i < 28) {
                z13 = false;
            } else {
                z13 = true;
            }
            sThrowOnInvalidFloatProperties = z13;
            if (i >= 28) {
                z14 = false;
            } else {
                z14 = true;
            }
            sCanFocusZeroSized = z14;
            if (i >= 28) {
                z15 = false;
            } else {
                z15 = true;
            }
            sAlwaysAssignFocus = z15;
            sAcceptZeroSizeDragShadow = i < 28;
            sCompatibilityDone = true;
        }
    }

    public View(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public View(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public View(Context context, AttributeSet attributeSet, int i, int i2) {
        int i3;
        int i4;
        AnonymousClass1 anonymousClass1;
        ?? r9;
        boolean z;
        int focusableAttribute;
        int i5;
        int i6;
        boolean z2;
        ?? r11;
        int i7;
        ?? Split;
        ?? string;
        this(context);
        ?? ObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.View, i, i2);
        if (mDebugViewAttributes) {
            saveAttributeData(attributeSet, ObtainStyledAttributes);
        }
        int i8 = this.mOverScrollMode;
        int i9 = context.getApplicationInfo().targetSdkVersion;
        int indexCount = ObtainStyledAttributes.getIndexCount();
        AnonymousClass1 anonymousClass12 = null;
        ?? r12 = 0;
        int i10 = i8;
        int i11 = 16;
        int i12 = 16;
        Drawable drawable = null;
        int i13 = 0;
        boolean z3 = false;
        boolean z4 = false;
        boolean z5 = false;
        boolean z6 = false;
        boolean z7 = false;
        boolean z8 = false;
        int i14 = 0;
        int dimensionPixelOffset = 0;
        int dimensionPixelOffset2 = 0;
        boolean z9 = false;
        boolean z10 = false;
        int i15 = Integer.MIN_VALUE;
        int i16 = Integer.MIN_VALUE;
        int i17 = -1;
        int i18 = -1;
        int dimensionPixelSize = -1;
        int i19 = -1;
        int i20 = -1;
        int dimensionPixelSize2 = -1;
        int dimensionPixelSize3 = -1;
        float dimension = 0.0f;
        float dimension2 = 0.0f;
        float dimension3 = 0.0f;
        float dimension4 = 0.0f;
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 1.0f;
        float f5 = 1.0f;
        while (i13 < indexCount) {
            int index = ObtainStyledAttributes.getIndex(i13);
            switch (index) {
                case 8:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    int i21 = ObtainStyledAttributes.getInt(index, 0);
                    if (i21 != 0) {
                        i11 |= 50331648 & i21;
                        i12 |= 50331648;
                    }
                    i14 = i21;
                    i13++;
                    AnonymousClass1 anonymousClass13 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13;
                    break;
                case 9:
                    anonymousClass1 = anonymousClass12;
                    this.mID = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132;
                    break;
                case 10:
                    anonymousClass1 = anonymousClass12;
                    this.mTag = ObtainStyledAttributes.getText(index);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322;
                    break;
                case 11:
                    ?? r45 = r12;
                    anonymousClass1 = anonymousClass12;
                    dimensionPixelOffset = ObtainStyledAttributes.getDimensionPixelOffset(index, r45 == true ? 1 : 0);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222;
                    break;
                case 12:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    dimensionPixelOffset2 = ObtainStyledAttributes.getDimensionPixelOffset(index, 0);
                    i13++;
                    AnonymousClass1 anonymousClass132222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222;
                    break;
                case 13:
                    anonymousClass1 = anonymousClass12;
                    drawable = ObtainStyledAttributes.getDrawable(index);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222;
                    break;
                case 14:
                    anonymousClass1 = anonymousClass12;
                    int dimensionPixelSize4 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    this.mUserPaddingLeftInitial = dimensionPixelSize4;
                    this.mUserPaddingRightInitial = dimensionPixelSize4;
                    i17 = dimensionPixelSize4;
                    z = false;
                    z3 = true;
                    r9 = z;
                    z4 = true;
                    i13++;
                    AnonymousClass1 anonymousClass13222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222;
                    break;
                case 15:
                    anonymousClass1 = anonymousClass12;
                    int dimensionPixelSize5 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    this.mUserPaddingLeftInitial = dimensionPixelSize5;
                    i19 = dimensionPixelSize5;
                    r9 = 0;
                    z3 = true;
                    i13++;
                    AnonymousClass1 anonymousClass132222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222;
                    break;
                case 16:
                    anonymousClass1 = anonymousClass12;
                    dimensionPixelSize2 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222;
                    break;
                case 17:
                    anonymousClass1 = anonymousClass12;
                    int dimensionPixelSize6 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    this.mUserPaddingRightInitial = dimensionPixelSize6;
                    i20 = dimensionPixelSize6;
                    r9 = 0;
                    z4 = true;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222;
                    break;
                case 18:
                    anonymousClass1 = anonymousClass12;
                    dimensionPixelSize3 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222;
                    break;
                case 19:
                    anonymousClass1 = anonymousClass12;
                    focusableAttribute = (i11 & (-18)) | getFocusableAttribute(ObtainStyledAttributes);
                    if ((focusableAttribute & 16) == 0) {
                        i5 = i12 | 17;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222;
                    } else {
                        i11 = focusableAttribute;
                        r9 = 0;
                        i13++;
                        AnonymousClass1 anonymousClass13222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass13222222222222;
                    }
                    break;
                case 20:
                    anonymousClass1 = anonymousClass12;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        focusableAttribute = (i11 & (-17)) | 262145;
                        i5 = 262161 | i12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222;
                    break;
                case 21:
                    ?? r452 = r12;
                    anonymousClass1 = anonymousClass12;
                    int i22 = ObtainStyledAttributes.getInt(index, r452 == true ? 1 : 0);
                    if (i22 != 0) {
                        focusableAttribute = VISIBILITY_FLAGS[i22] | i11;
                        i5 = i12 | 12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass13222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass13222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222;
                    break;
                case 22:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    z2 = false;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        focusableAttribute = i11 | 2;
                        i6 = i12 | 2;
                        i12 = i6;
                        r9 = z2;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222222222;
                    } else {
                        i13++;
                        AnonymousClass1 anonymousClass13222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass13222222222222222222;
                    }
                    break;
                case 23:
                    anonymousClass1 = anonymousClass12;
                    int i23 = ObtainStyledAttributes.getInt(index, 0);
                    if (i23 != 0) {
                        focusableAttribute = i23 | i11;
                        i12 |= 768;
                        r9 = 0;
                        z7 = true;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222;
                    break;
                case 24:
                    anonymousClass1 = anonymousClass12;
                    if (i9 < 14) {
                        i7 = ObtainStyledAttributes.getInt(index, 0);
                        if (i7 != 0) {
                            focusableAttribute = i7 | i11;
                            i5 = i12 | 12288;
                            initializeFadingEdgeInternal(ObtainStyledAttributes);
                            i12 = i5;
                            r9 = 0;
                            i11 = focusableAttribute;
                            i13++;
                            AnonymousClass1 anonymousClass13222222222222222222222 = anonymousClass1;
                            r12 = r9;
                            anonymousClass12 = anonymousClass13222222222222222222222;
                        }
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222;
                    break;
                case 25:
                case 45:
                case 46:
                case 47:
                case 102:
                case 103:
                case 104:
                default:
                    ?? r453 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r453 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222;
                    break;
                case 26:
                    anonymousClass1 = anonymousClass12;
                    this.mNextFocusLeftId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222;
                    break;
                case 27:
                    anonymousClass1 = anonymousClass12;
                    this.mNextFocusRightId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222;
                    break;
                case 28:
                    anonymousClass1 = anonymousClass12;
                    this.mNextFocusUpId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222;
                    break;
                case 29:
                    anonymousClass1 = anonymousClass12;
                    this.mNextFocusDownId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222;
                    break;
                case 30:
                    ?? r454 = r12;
                    anonymousClass1 = anonymousClass12;
                    if (ObtainStyledAttributes.getBoolean(index, r454)) {
                        focusableAttribute = i11 | 16384;
                        i5 = i12 | 16384;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222;
                    break;
                case 31:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    z2 = false;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        focusableAttribute = 2097152 | i11;
                        i6 = 2097152 | i12;
                        i12 = i6;
                        r9 = z2;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass13222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass13222222222222222222222222222222;
                    } else {
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222222222222222222222;
                    }
                    break;
                case 32:
                    anonymousClass1 = anonymousClass12;
                    if (!ObtainStyledAttributes.getBoolean(index, true)) {
                        focusableAttribute = 65536 | i11;
                        i5 = 65536 | i12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222;
                    break;
                case 33:
                    ?? r455 = r12;
                    anonymousClass1 = anonymousClass12;
                    int i24 = ObtainStyledAttributes.getInt(index, r455 == true ? 1 : 0);
                    if (i24 != 0) {
                        focusableAttribute = DRAWING_CACHE_QUALITY_FLAGS[i24] | i11;
                        i5 = DRAWING_CACHE_QUALITY_MASK | i12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222;
                    break;
                case 34:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    r9 = 0;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        i11 |= 4194304;
                        i12 = 4194304 | i12;
                    }
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222;
                    break;
                case 35:
                    anonymousClass1 = anonymousClass12;
                    if (i9 >= 23 || (this instanceof FrameLayout)) {
                        setForeground(ObtainStyledAttributes.getDrawable(index));
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222;
                    break;
                case 36:
                    ?? r456 = r12;
                    anonymousClass1 = anonymousClass12;
                    this.mMinWidth = ObtainStyledAttributes.getDimensionPixelSize(index, r456 == true ? 1 : 0);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222;
                    break;
                case 37:
                    ?? r457 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r457 == true ? 1 : 0;
                    this.mMinHeight = ObtainStyledAttributes.getDimensionPixelSize(index, r9 == true ? 1 : 0);
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222;
                    break;
                case 38:
                    anonymousClass1 = anonymousClass12;
                    if (i9 >= 23 || (this instanceof FrameLayout)) {
                        r9 = 0;
                        setForegroundGravity(ObtainStyledAttributes.getInt(index, 0));
                    } else {
                        r9 = 0;
                    }
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222;
                    break;
                case 39:
                    anonymousClass1 = anonymousClass12;
                    if (!ObtainStyledAttributes.getBoolean(index, true)) {
                        focusableAttribute = (-134217729) & i11;
                        i5 = 134217728 | i12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222;
                    break;
                case 40:
                    ?? r458 = r12;
                    anonymousClass1 = anonymousClass12;
                    if (ObtainStyledAttributes.getBoolean(index, r458)) {
                        i11 |= 67108864;
                        i12 = 67108864 | i12;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222;
                    break;
                case 41:
                    anonymousClass1 = anonymousClass12;
                    r9 = 0;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        setScrollContainer(true);
                    }
                    z10 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222;
                    break;
                case 42:
                    anonymousClass1 = anonymousClass12;
                    if (!ObtainStyledAttributes.getBoolean(index, true)) {
                        focusableAttribute = (-268435457) & i11;
                        i5 = 268435456 | i12;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222;
                    break;
                case 43:
                    anonymousClass1 = anonymousClass12;
                    if (context.isRestricted()) {
                        throw new IllegalStateException("The android:onClick attribute cannot be used within a restricted context");
                    }
                    String string2 = ObtainStyledAttributes.getString(index);
                    if (string2 != null) {
                        setOnClickListener(new DeclaredOnClickListener(this, string2));
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222;
                    break;
                    break;
                case 44:
                    anonymousClass1 = anonymousClass12;
                    setContentDescription(ObtainStyledAttributes.getString(index));
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222;
                    break;
                case 48:
                    anonymousClass1 = anonymousClass12;
                    i10 = ObtainStyledAttributes.getInt(index, 1);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222;
                    break;
                case 49:
                    anonymousClass1 = anonymousClass12;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        focusableAttribute = i11 | 1024;
                        i5 = i12 | 1024;
                        i12 = i5;
                        r9 = 0;
                        i11 = focusableAttribute;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222;
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222;
                    break;
                case 50:
                    anonymousClass1 = anonymousClass12;
                    setAlpha(ObtainStyledAttributes.getFloat(index, 1.0f));
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222;
                    break;
                case 51:
                    anonymousClass1 = anonymousClass12;
                    setPivotX(ObtainStyledAttributes.getDimension(index, 0.0f));
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222;
                    break;
                case 52:
                    anonymousClass1 = anonymousClass12;
                    setPivotY(ObtainStyledAttributes.getDimension(index, 0.0f));
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222;
                    break;
                case 53:
                    anonymousClass1 = anonymousClass12;
                    dimension = ObtainStyledAttributes.getDimension(index, 0.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222;
                    break;
                case 54:
                    anonymousClass1 = anonymousClass12;
                    dimension2 = ObtainStyledAttributes.getDimension(index, 0.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222;
                    break;
                case 55:
                    anonymousClass1 = anonymousClass12;
                    f4 = ObtainStyledAttributes.getFloat(index, 1.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222;
                    break;
                case 56:
                    anonymousClass1 = anonymousClass12;
                    f5 = ObtainStyledAttributes.getFloat(index, 1.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222;
                    break;
                case 57:
                    anonymousClass1 = anonymousClass12;
                    f = ObtainStyledAttributes.getFloat(index, 0.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222;
                    break;
                case 58:
                    anonymousClass1 = anonymousClass12;
                    f2 = ObtainStyledAttributes.getFloat(index, 0.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 59:
                    anonymousClass1 = anonymousClass12;
                    f3 = ObtainStyledAttributes.getFloat(index, 0.0f);
                    r9 = 0;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 60:
                    ?? r112 = r12;
                    anonymousClass1 = anonymousClass12;
                    this.mVerticalScrollbarPosition = ObtainStyledAttributes.getInt(index, r112 == true ? 1 : 0);
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 61:
                    r11 = r12;
                    anonymousClass1 = anonymousClass12;
                    this.mNextFocusForwardId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = r11;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 62:
                    ?? r113 = r12;
                    anonymousClass1 = null;
                    setLayerType(ObtainStyledAttributes.getInt(index, r113 == true ? 1 : 0), null);
                    r11 = r113;
                    r9 = r11;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 63:
                    anonymousClass1 = null;
                    i7 = ObtainStyledAttributes.getInt(index, 0);
                    if (i7 != 0) {
                    }
                    r9 = 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 64:
                    setImportantForAccessibility(ObtainStyledAttributes.getInt(index, 0));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 65:
                    this.mPrivateFlags2 &= -449;
                    int i25 = ObtainStyledAttributes.getInt(index, -1);
                    if (i25 != -1) {
                        this.mPrivateFlags2 = PFLAG2_TEXT_DIRECTION_FLAGS[i25] | this.mPrivateFlags2;
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 66:
                    this.mPrivateFlags2 &= -57345;
                    this.mPrivateFlags2 = PFLAG2_TEXT_ALIGNMENT_FLAGS[ObtainStyledAttributes.getInt(index, 1)] | this.mPrivateFlags2;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 67:
                    this.mPrivateFlags2 &= -61;
                    int i26 = ObtainStyledAttributes.getInt(index, -1);
                    this.mPrivateFlags2 = ((i26 != -1 ? LAYOUT_DIRECTION_FLAGS[i26] : 2) << 2) | this.mPrivateFlags2;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 68:
                    int dimensionPixelSize7 = ObtainStyledAttributes.getDimensionPixelSize(index, Integer.MIN_VALUE);
                    i15 = dimensionPixelSize7;
                    z5 = dimensionPixelSize7 != Integer.MIN_VALUE;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 69:
                    int dimensionPixelSize8 = ObtainStyledAttributes.getDimensionPixelSize(index, Integer.MIN_VALUE);
                    i16 = dimensionPixelSize8;
                    z6 = dimensionPixelSize8 != Integer.MIN_VALUE;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 70:
                    setLabelFor(ObtainStyledAttributes.getResourceId(index, -1));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 71:
                    setAccessibilityLiveRegion(ObtainStyledAttributes.getInt(index, 0));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 72:
                    dimension3 = ObtainStyledAttributes.getDimension(index, 0.0f);
                    r9 = 0;
                    anonymousClass1 = null;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 73:
                    setTransitionName(ObtainStyledAttributes.getString(index));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 74:
                    setNestedScrollingEnabled(ObtainStyledAttributes.getBoolean(index, false));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 75:
                    dimension4 = ObtainStyledAttributes.getDimension(index, 0.0f);
                    r9 = 0;
                    anonymousClass1 = null;
                    z9 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 76:
                    setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, ObtainStyledAttributes.getResourceId(index, 0)));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 77:
                    if (this.mBackgroundTint == null) {
                        this.mBackgroundTint = new TintInfo();
                    }
                    this.mBackgroundTint.mTintList = ObtainStyledAttributes.getColorStateList(77);
                    this.mBackgroundTint.mHasTintList = true;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 78:
                    if (this.mBackgroundTint == null) {
                        this.mBackgroundTint = new TintInfo();
                    }
                    this.mBackgroundTint.mTintMode = Drawable.parseTintMode(ObtainStyledAttributes.getInt(78, -1), null);
                    this.mBackgroundTint.mHasTintMode = true;
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 79:
                    if (i9 >= 23 || (this instanceof FrameLayout)) {
                        setForegroundTintList(ObtainStyledAttributes.getColorStateList(index));
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 80:
                    if (i9 >= 23 || (this instanceof FrameLayout)) {
                        setForegroundTintMode(Drawable.parseTintMode(ObtainStyledAttributes.getInt(index, -1), null));
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 81:
                    setOutlineProviderFromAttribute(ObtainStyledAttributes.getInt(81, 0));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 82:
                    setAccessibilityTraversalBefore(ObtainStyledAttributes.getResourceId(index, -1));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 83:
                    setAccessibilityTraversalAfter(ObtainStyledAttributes.getResourceId(index, -1));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 84:
                    int i27 = (ObtainStyledAttributes.getInt(index, r12 == true ? 1 : 0) << 8) & SCROLL_INDICATORS_PFLAG3_MASK;
                    if (i27 != 0) {
                        this.mPrivateFlags3 = i27 | this.mPrivateFlags3;
                        r9 = 0;
                        anonymousClass1 = null;
                        z8 = true;
                        i13++;
                        AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 85:
                    r9 = 0;
                    r9 = 0;
                    if (ObtainStyledAttributes.getBoolean(index, false)) {
                        i11 |= 8388608;
                        i12 = 8388608 | i12;
                    }
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 86:
                    int resourceId = ObtainStyledAttributes.getResourceId(index, 0);
                    if (resourceId != 0) {
                        setPointerIcon(PointerIcon.load(context.getResources(), resourceId));
                    } else {
                        int i28 = ObtainStyledAttributes.getInt(index, 1);
                        if (i28 != 1) {
                            setPointerIcon(PointerIcon.getSystemIcon(context, i28));
                        }
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 87:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        forceHasOverlappingRendering(ObtainStyledAttributes.getBoolean(index, true));
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 88:
                    setTooltipText(ObtainStyledAttributes.getText(index));
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 89:
                    int dimensionPixelSize9 = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    this.mUserPaddingLeftInitial = dimensionPixelSize9;
                    this.mUserPaddingRightInitial = dimensionPixelSize9;
                    i18 = dimensionPixelSize9;
                    z = false;
                    anonymousClass1 = null;
                    z3 = true;
                    r9 = z;
                    z4 = true;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 90:
                    dimensionPixelSize = ObtainStyledAttributes.getDimensionPixelSize(index, -1);
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 91:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setKeyboardNavigationCluster(ObtainStyledAttributes.getBoolean(index, true));
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 92:
                    this.mNextClusterForwardId = ObtainStyledAttributes.getResourceId(index, -1);
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 93:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setFocusedByDefault(ObtainStyledAttributes.getBoolean(index, true));
                    }
                    r9 = 0;
                    anonymousClass1 = null;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 94:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        if (ObtainStyledAttributes.getType(index) == 1) {
                            int resourceId2 = ObtainStyledAttributes.getResourceId(index, r12);
                            try {
                                CharSequence[] textArray = ObtainStyledAttributes.getTextArray(index);
                                string = anonymousClass12;
                                Split = textArray;
                            } catch (Resources.NotFoundException e) {
                                string = getResources().getString(resourceId2);
                                Split = anonymousClass12;
                            }
                        } else {
                            Split = anonymousClass12;
                            string = ObtainStyledAttributes.getString(index);
                        }
                        if (Split == 0) {
                            if (string == 0) {
                                throw new IllegalArgumentException("Could not resolve autofillHints");
                            }
                            Split = string.split(",");
                        }
                        String[] strArr = new String[Split.length];
                        int length = Split.length;
                        for (?? r92 = r12; r92 < length; r92++) {
                            strArr[r92] = Split[r92].toString().trim();
                        }
                        setAutofillHints(strArr);
                        r9 = 0;
                        anonymousClass1 = null;
                        i13++;
                        AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                        r12 = r9;
                        anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    }
                    ?? r4532 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r4532 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 95:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setImportantForAutofill(ObtainStyledAttributes.getInt(index, r12));
                    }
                    ?? r45322 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r45322 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 96:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setDefaultFocusHighlightEnabled(ObtainStyledAttributes.getBoolean(index, true));
                    }
                    ?? r453222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r453222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 97:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setScreenReaderFocusable(ObtainStyledAttributes.getBoolean(index, r12));
                    }
                    ?? r4532222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r4532222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 98:
                    if (ObtainStyledAttributes.peekValue(index) != null) {
                        setAccessibilityPaneTitle(ObtainStyledAttributes.getString(index));
                    }
                    ?? r45322222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r45322222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 99:
                    setAccessibilityHeading(ObtainStyledAttributes.getBoolean(index, r12));
                    ?? r453222222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r453222222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 100:
                    setOutlineSpotShadowColor(ObtainStyledAttributes.getColor(index, -16777216));
                    ?? r4532222222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r4532222222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass1322222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 101:
                    setOutlineAmbientShadowColor(ObtainStyledAttributes.getColor(index, -16777216));
                    ?? r45322222222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r45322222222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass13222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
                case 105:
                    if (i9 >= 23 || (this instanceof FrameLayout)) {
                        if (this.mForegroundInfo == null) {
                            this.mForegroundInfo = new ForegroundInfo();
                        }
                        this.mForegroundInfo.mInsidePadding = ObtainStyledAttributes.getBoolean(index, this.mForegroundInfo.mInsidePadding);
                    }
                    ?? r453222222222 = r12;
                    anonymousClass1 = anonymousClass12;
                    r9 = r453222222222 == true ? 1 : 0;
                    i13++;
                    AnonymousClass1 anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222 = anonymousClass1;
                    r12 = r9;
                    anonymousClass12 = anonymousClass132222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222;
                    break;
            }
        }
        ?? r93 = r12;
        setOverScrollMode(i10);
        this.mUserPaddingStart = i15;
        this.mUserPaddingEnd = i16;
        Drawable drawable2 = drawable;
        if (drawable2 != null) {
            setBackground(drawable2);
        }
        this.mLeftPaddingDefined = z3;
        this.mRightPaddingDefined = z4;
        int i29 = i17;
        if (i29 >= 0) {
            this.mUserPaddingLeftInitial = i29;
            this.mUserPaddingRightInitial = i29;
            i3 = i29;
            dimensionPixelSize2 = i3;
            dimensionPixelSize3 = dimensionPixelSize2;
        } else {
            int i30 = i18;
            if (i30 >= 0) {
                this.mUserPaddingLeftInitial = i30;
                this.mUserPaddingRightInitial = i30;
                i19 = i30;
                i20 = i19;
            }
            if (dimensionPixelSize >= 0) {
                dimensionPixelSize2 = dimensionPixelSize;
                dimensionPixelSize3 = dimensionPixelSize2;
            }
            i29 = i19;
            i3 = i20;
        }
        if (isRtlCompatibilityMode()) {
            if (!this.mLeftPaddingDefined && z5) {
                i29 = i15;
            }
            this.mUserPaddingLeftInitial = i29 < 0 ? this.mUserPaddingLeftInitial : i29;
            i16 = (this.mRightPaddingDefined || !z6) ? i3 : i16;
            this.mUserPaddingRightInitial = i16 < 0 ? this.mUserPaddingRightInitial : i16;
        } else {
            r93 = (z5 || z6) ? 1 : r93;
            if (this.mLeftPaddingDefined && r93 == 0) {
                this.mUserPaddingLeftInitial = i29;
            }
            if (this.mRightPaddingDefined && r93 == 0) {
                this.mUserPaddingRightInitial = i3;
            }
        }
        internalSetPadding(this.mUserPaddingLeftInitial, dimensionPixelSize2 >= 0 ? dimensionPixelSize2 : this.mPaddingTop, this.mUserPaddingRightInitial, dimensionPixelSize3 >= 0 ? dimensionPixelSize3 : this.mPaddingBottom);
        if (i12 != 0) {
            setFlags(i11, i12);
        }
        if (z7) {
            initializeScrollbarsInternal(ObtainStyledAttributes);
        }
        if (z8) {
            initializeScrollIndicatorsInternal();
        }
        ObtainStyledAttributes.recycle();
        if (i14 != 0) {
            recomputePadding();
        }
        int i31 = dimensionPixelOffset;
        if (i31 == 0) {
            i4 = dimensionPixelOffset2;
            if (i4 != 0) {
            }
            if (z9) {
                setTranslationX(dimension);
                setTranslationY(dimension2);
                setTranslationZ(dimension3);
                setElevation(dimension4);
                setRotation(f);
                setRotationX(f2);
                setRotationY(f3);
                setScaleX(f4);
                setScaleY(f5);
            }
            if (!z10 && (i11 & 512) != 0) {
                setScrollContainer(true);
            }
            computeOpaqueFlags();
        }
        i4 = dimensionPixelOffset2;
        scrollTo(i31, i4);
        if (z9) {
        }
        if (!z10) {
            setScrollContainer(true);
        }
        computeOpaqueFlags();
    }

    private static class DeclaredOnClickListener implements OnClickListener {
        private final View mHostView;
        private final String mMethodName;
        private Context mResolvedContext;
        private Method mResolvedMethod;

        public DeclaredOnClickListener(View view, String str) {
            this.mHostView = view;
            this.mMethodName = str;
        }

        @Override
        public void onClick(View view) {
            if (this.mResolvedMethod == null) {
                resolveMethod(this.mHostView.getContext(), this.mMethodName);
            }
            try {
                this.mResolvedMethod.invoke(this.mResolvedContext, view);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not execute non-public method for android:onClick", e);
            } catch (InvocationTargetException e2) {
                throw new IllegalStateException("Could not execute method for android:onClick", e2);
            }
        }

        private void resolveMethod(Context context, String str) {
            String str2;
            Method method;
            while (context != null) {
                try {
                    if (!context.isRestricted() && (method = context.getClass().getMethod(this.mMethodName, View.class)) != null) {
                        this.mResolvedMethod = method;
                        this.mResolvedContext = context;
                        return;
                    }
                } catch (NoSuchMethodException e) {
                }
                if (context instanceof ContextWrapper) {
                    context = ((ContextWrapper) context).getBaseContext();
                } else {
                    context = null;
                }
            }
            int id = this.mHostView.getId();
            if (id != -1) {
                str2 = " with id '" + this.mHostView.getContext().getResources().getResourceEntryName(id) + "'";
            } else {
                str2 = "";
            }
            throw new IllegalStateException("Could not find method " + this.mMethodName + "(View) in a parent or ancestor Context for android:onClick attribute defined on view " + this.mHostView.getClass() + str2);
        }
    }

    View() {
        InputEventConsistencyVerifier inputEventConsistencyVerifier;
        this.mCurrentAnimation = null;
        this.mRecreateDisplayList = false;
        this.mID = -1;
        this.mAutofillViewId = -1;
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
        this.mLongClickX = Float.NaN;
        this.mLongClickY = Float.NaN;
        this.mDrawableState = null;
        this.mOutlineProvider = ViewOutlineProvider.BACKGROUND;
        this.mNextFocusLeftId = -1;
        this.mNextFocusRightId = -1;
        this.mNextFocusUpId = -1;
        this.mNextFocusDownId = -1;
        this.mNextFocusForwardId = -1;
        this.mNextClusterForwardId = -1;
        this.mDefaultFocusHighlightEnabled = true;
        this.mPendingCheckForTap = null;
        this.mTouchDelegate = null;
        this.mDrawingCacheBackgroundColor = 0;
        this.mAnimator = null;
        this.mLayerType = 0;
        if (!InputEventConsistencyVerifier.isInstrumentationEnabled()) {
            inputEventConsistencyVerifier = null;
        } else {
            inputEventConsistencyVerifier = new InputEventConsistencyVerifier(this, 0);
        }
        this.mInputEventConsistencyVerifier = inputEventConsistencyVerifier;
        this.mResources = null;
        this.mRenderNode = RenderNode.create(getClass().getName(), this);
    }

    final boolean debugDraw() {
        return DEBUG_DRAW || (this.mAttachInfo != null && this.mAttachInfo.mDebugLayout);
    }

    private static SparseArray<String> getAttributeMap() {
        if (mAttributeMap == null) {
            mAttributeMap = new SparseArray<>();
        }
        return mAttributeMap;
    }

    private void saveAttributeData(AttributeSet attributeSet, TypedArray typedArray) {
        int attributeCount;
        int resourceId;
        if (attributeSet != null) {
            attributeCount = attributeSet.getAttributeCount();
        } else {
            attributeCount = 0;
        }
        int indexCount = typedArray.getIndexCount();
        String[] strArr = new String[(attributeCount + indexCount) * 2];
        int i = 0;
        for (int i2 = 0; i2 < attributeCount; i2++) {
            strArr[i] = attributeSet.getAttributeName(i2);
            strArr[i + 1] = attributeSet.getAttributeValue(i2);
            i += 2;
        }
        Resources resources = typedArray.getResources();
        SparseArray<String> attributeMap = getAttributeMap();
        for (int i3 = 0; i3 < indexCount; i3++) {
            int index = typedArray.getIndex(i3);
            if (typedArray.hasValueOrEmpty(index) && (resourceId = typedArray.getResourceId(index, 0)) != 0) {
                String resourceName = attributeMap.get(resourceId);
                if (resourceName == null) {
                    try {
                        resourceName = resources.getResourceName(resourceId);
                    } catch (Resources.NotFoundException e) {
                        resourceName = "0x" + Integer.toHexString(resourceId);
                    }
                    attributeMap.put(resourceId, resourceName);
                }
                strArr[i] = resourceName;
                strArr[i + 1] = typedArray.getString(index);
                i += 2;
            }
        }
        String[] strArr2 = new String[i];
        System.arraycopy(strArr, 0, strArr2, 0, i);
        this.mAttributes = strArr2;
    }

    public String toString() {
        String resourcePackageName;
        StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getName());
        sb.append('{');
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        int i = this.mViewFlags & 12;
        if (i == 0) {
            sb.append('V');
        } else if (i == 4) {
            sb.append('I');
        } else if (i == 8) {
            sb.append('G');
        } else {
            sb.append('.');
        }
        sb.append((this.mViewFlags & 1) == 1 ? 'F' : '.');
        sb.append((this.mViewFlags & 32) == 0 ? DateFormat.DAY : '.');
        sb.append((this.mViewFlags & 128) == 128 ? '.' : 'D');
        sb.append((this.mViewFlags & 256) != 0 ? 'H' : '.');
        sb.append((this.mViewFlags & 512) == 0 ? '.' : 'V');
        sb.append((this.mViewFlags & 16384) != 0 ? 'C' : '.');
        sb.append((this.mViewFlags & 2097152) != 0 ? DateFormat.STANDALONE_MONTH : '.');
        sb.append((this.mViewFlags & 8388608) != 0 ? 'X' : '.');
        sb.append(' ');
        sb.append((this.mPrivateFlags & 8) != 0 ? 'R' : '.');
        sb.append((this.mPrivateFlags & 2) == 0 ? '.' : 'F');
        sb.append((this.mPrivateFlags & 4) != 0 ? 'S' : '.');
        if ((this.mPrivateFlags & 33554432) != 0) {
            sb.append('p');
        } else {
            sb.append((this.mPrivateFlags & 16384) != 0 ? 'P' : '.');
        }
        sb.append((this.mPrivateFlags & 268435456) == 0 ? '.' : 'H');
        sb.append((this.mPrivateFlags & 1073741824) != 0 ? DateFormat.CAPITAL_AM_PM : '.');
        sb.append((this.mPrivateFlags & Integer.MIN_VALUE) == 0 ? '.' : 'I');
        sb.append((this.mPrivateFlags & 6291456) != 0 ? 'D' : '.');
        sb.append(' ');
        sb.append(this.mLeft);
        sb.append(',');
        sb.append(this.mTop);
        sb.append('-');
        sb.append(this.mRight);
        sb.append(',');
        sb.append(this.mBottom);
        int id = getId();
        if (id != -1) {
            sb.append(" #");
            sb.append(Integer.toHexString(id));
            Resources resources = this.mResources;
            if (id > 0 && Resources.resourceHasPackage(id) && resources != null) {
                int i2 = (-16777216) & id;
                if (i2 == 16777216) {
                    resourcePackageName = ZenModeConfig.SYSTEM_AUTHORITY;
                } else if (i2 == 2130706432) {
                    resourcePackageName = "app";
                } else {
                    try {
                        resourcePackageName = resources.getResourcePackageName(id);
                    } catch (Resources.NotFoundException e) {
                    }
                }
                String resourceTypeName = resources.getResourceTypeName(id);
                String resourceEntryName = resources.getResourceEntryName(id);
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(resourcePackageName);
                sb.append(SettingsStringUtil.DELIMITER);
                sb.append(resourceTypeName);
                sb.append("/");
                sb.append(resourceEntryName);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    protected void initializeFadingEdge(TypedArray typedArray) {
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(R.styleable.View);
        initializeFadingEdgeInternal(typedArrayObtainStyledAttributes);
        typedArrayObtainStyledAttributes.recycle();
    }

    protected void initializeFadingEdgeInternal(TypedArray typedArray) {
        initScrollCache();
        this.mScrollCache.fadingEdgeLength = typedArray.getDimensionPixelSize(25, ViewConfiguration.get(this.mContext).getScaledFadingEdgeLength());
    }

    public int getVerticalFadingEdgeLength() {
        ScrollabilityCache scrollabilityCache;
        if (isVerticalFadingEdgeEnabled() && (scrollabilityCache = this.mScrollCache) != null) {
            return scrollabilityCache.fadingEdgeLength;
        }
        return 0;
    }

    public void setFadingEdgeLength(int i) {
        initScrollCache();
        this.mScrollCache.fadingEdgeLength = i;
    }

    public int getHorizontalFadingEdgeLength() {
        ScrollabilityCache scrollabilityCache;
        if (isHorizontalFadingEdgeEnabled() && (scrollabilityCache = this.mScrollCache) != null) {
            return scrollabilityCache.fadingEdgeLength;
        }
        return 0;
    }

    public int getVerticalScrollbarWidth() {
        ScrollBarDrawable scrollBarDrawable;
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache == null || (scrollBarDrawable = scrollabilityCache.scrollBar) == null) {
            return 0;
        }
        int size = scrollBarDrawable.getSize(true);
        if (size <= 0) {
            return scrollabilityCache.scrollBarSize;
        }
        return size;
    }

    protected int getHorizontalScrollbarHeight() {
        ScrollBarDrawable scrollBarDrawable;
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache == null || (scrollBarDrawable = scrollabilityCache.scrollBar) == null) {
            return 0;
        }
        int size = scrollBarDrawable.getSize(false);
        if (size <= 0) {
            return scrollabilityCache.scrollBarSize;
        }
        return size;
    }

    protected void initializeScrollbars(TypedArray typedArray) {
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(R.styleable.View);
        initializeScrollbarsInternal(typedArrayObtainStyledAttributes);
        typedArrayObtainStyledAttributes.recycle();
    }

    protected void initializeScrollbarsInternal(TypedArray typedArray) {
        initScrollCache();
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache.scrollBar == null) {
            scrollabilityCache.scrollBar = new ScrollBarDrawable();
            scrollabilityCache.scrollBar.setState(getDrawableState());
            scrollabilityCache.scrollBar.setCallback(this);
        }
        boolean z = typedArray.getBoolean(47, true);
        if (!z) {
            scrollabilityCache.state = 1;
        }
        scrollabilityCache.fadeScrollBars = z;
        scrollabilityCache.scrollBarFadeDuration = typedArray.getInt(45, ViewConfiguration.getScrollBarFadeDuration());
        scrollabilityCache.scrollBarDefaultDelayBeforeFade = typedArray.getInt(46, ViewConfiguration.getScrollDefaultDelay());
        scrollabilityCache.scrollBarSize = typedArray.getDimensionPixelSize(1, ViewConfiguration.get(this.mContext).getScaledScrollBarSize());
        scrollabilityCache.scrollBar.setHorizontalTrackDrawable(typedArray.getDrawable(4));
        Drawable drawable = typedArray.getDrawable(2);
        if (drawable != null) {
            scrollabilityCache.scrollBar.setHorizontalThumbDrawable(drawable);
        }
        if (typedArray.getBoolean(6, false)) {
            scrollabilityCache.scrollBar.setAlwaysDrawHorizontalTrack(true);
        }
        Drawable drawable2 = typedArray.getDrawable(5);
        scrollabilityCache.scrollBar.setVerticalTrackDrawable(drawable2);
        Drawable drawable3 = typedArray.getDrawable(3);
        if (drawable3 != null) {
            scrollabilityCache.scrollBar.setVerticalThumbDrawable(drawable3);
        }
        if (typedArray.getBoolean(7, false)) {
            scrollabilityCache.scrollBar.setAlwaysDrawVerticalTrack(true);
        }
        int layoutDirection = getLayoutDirection();
        if (drawable2 != null) {
            drawable2.setLayoutDirection(layoutDirection);
        }
        if (drawable3 != null) {
            drawable3.setLayoutDirection(layoutDirection);
        }
        resolvePadding();
    }

    private void initializeScrollIndicatorsInternal() {
        if (this.mScrollIndicatorDrawable == null) {
            this.mScrollIndicatorDrawable = this.mContext.getDrawable(R.drawable.scroll_indicator_material);
        }
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

    public void setVerticalScrollbarPosition(int i) {
        if (this.mVerticalScrollbarPosition != i) {
            this.mVerticalScrollbarPosition = i;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    public int getVerticalScrollbarPosition() {
        return this.mVerticalScrollbarPosition;
    }

    boolean isOnScrollbar(float f, float f2) {
        if (this.mScrollCache == null) {
            return false;
        }
        float scrollX = f + getScrollX();
        float scrollY = f2 + getScrollY();
        if (isVerticalScrollBarEnabled() && !isVerticalScrollBarHidden()) {
            Rect rect = this.mScrollCache.mScrollBarTouchBounds;
            getVerticalScrollBarBounds(null, rect);
            if (rect.contains((int) scrollX, (int) scrollY)) {
                return true;
            }
        }
        if (isHorizontalScrollBarEnabled()) {
            Rect rect2 = this.mScrollCache.mScrollBarTouchBounds;
            getHorizontalScrollBarBounds(null, rect2);
            if (rect2.contains((int) scrollX, (int) scrollY)) {
                return true;
            }
        }
        return false;
    }

    boolean isOnScrollbarThumb(float f, float f2) {
        return isOnVerticalScrollbarThumb(f, f2) || isOnHorizontalScrollbarThumb(f, f2);
    }

    private boolean isOnVerticalScrollbarThumb(float f, float f2) {
        if (this.mScrollCache != null && isVerticalScrollBarEnabled() && !isVerticalScrollBarHidden()) {
            float scrollX = f + getScrollX();
            float scrollY = f2 + getScrollY();
            Rect rect = this.mScrollCache.mScrollBarBounds;
            getVerticalScrollBarBounds(rect, this.mScrollCache.mScrollBarTouchBounds);
            int iComputeVerticalScrollRange = computeVerticalScrollRange();
            int iComputeVerticalScrollOffset = computeVerticalScrollOffset();
            int iComputeVerticalScrollExtent = computeVerticalScrollExtent();
            int thumbLength = ScrollBarUtils.getThumbLength(rect.height(), rect.width(), iComputeVerticalScrollExtent, iComputeVerticalScrollRange);
            int thumbOffset = rect.top + ScrollBarUtils.getThumbOffset(rect.height(), thumbLength, iComputeVerticalScrollExtent, iComputeVerticalScrollRange, iComputeVerticalScrollOffset);
            int iMax = Math.max(this.mScrollCache.scrollBarMinTouchTarget - thumbLength, 0) / 2;
            if (scrollX >= r2.left && scrollX <= r2.right && scrollY >= thumbOffset - iMax && scrollY <= thumbOffset + thumbLength + iMax) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnHorizontalScrollbarThumb(float f, float f2) {
        if (this.mScrollCache != null && isHorizontalScrollBarEnabled()) {
            float scrollX = f + getScrollX();
            float scrollY = f2 + getScrollY();
            Rect rect = this.mScrollCache.mScrollBarBounds;
            getHorizontalScrollBarBounds(rect, this.mScrollCache.mScrollBarTouchBounds);
            int iComputeHorizontalScrollRange = computeHorizontalScrollRange();
            int iComputeHorizontalScrollOffset = computeHorizontalScrollOffset();
            int iComputeHorizontalScrollExtent = computeHorizontalScrollExtent();
            int thumbLength = ScrollBarUtils.getThumbLength(rect.width(), rect.height(), iComputeHorizontalScrollExtent, iComputeHorizontalScrollRange);
            int thumbOffset = rect.left + ScrollBarUtils.getThumbOffset(rect.width(), thumbLength, iComputeHorizontalScrollExtent, iComputeHorizontalScrollRange, iComputeHorizontalScrollOffset);
            int iMax = Math.max(this.mScrollCache.scrollBarMinTouchTarget - thumbLength, 0) / 2;
            if (scrollX >= thumbOffset - iMax && scrollX <= thumbOffset + thumbLength + iMax && scrollY >= r2.top && scrollY <= r2.bottom) {
                return true;
            }
        }
        return false;
    }

    boolean isDraggingScrollBar() {
        return (this.mScrollCache == null || this.mScrollCache.mScrollBarDraggingState == 0) ? false : true;
    }

    public void setScrollIndicators(int i) {
        setScrollIndicators(i, 63);
    }

    public void setScrollIndicators(int i, int i2) {
        int i3 = (i2 << 8) & SCROLL_INDICATORS_PFLAG3_MASK;
        int i4 = (i << 8) & i3;
        int i5 = ((~i3) & this.mPrivateFlags3) | i4;
        if (this.mPrivateFlags3 != i5) {
            this.mPrivateFlags3 = i5;
            if (i4 != 0) {
                initializeScrollIndicatorsInternal();
            }
            invalidate();
        }
    }

    public int getScrollIndicators() {
        return (this.mPrivateFlags3 & SCROLL_INDICATORS_PFLAG3_MASK) >>> 8;
    }

    ListenerInfo getListenerInfo() {
        if (this.mListenerInfo != null) {
            return this.mListenerInfo;
        }
        this.mListenerInfo = new ListenerInfo();
        return this.mListenerInfo;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        getListenerInfo().mOnScrollChangeListener = onScrollChangeListener;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
        getListenerInfo().mOnFocusChangeListener = onFocusChangeListener;
    }

    public void addOnLayoutChangeListener(OnLayoutChangeListener onLayoutChangeListener) {
        ListenerInfo listenerInfo = getListenerInfo();
        if (listenerInfo.mOnLayoutChangeListeners == null) {
            listenerInfo.mOnLayoutChangeListeners = new ArrayList();
        }
        if (!listenerInfo.mOnLayoutChangeListeners.contains(onLayoutChangeListener)) {
            listenerInfo.mOnLayoutChangeListeners.add(onLayoutChangeListener);
        }
    }

    public void removeOnLayoutChangeListener(OnLayoutChangeListener onLayoutChangeListener) {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnLayoutChangeListeners != null) {
            listenerInfo.mOnLayoutChangeListeners.remove(onLayoutChangeListener);
        }
    }

    public void addOnAttachStateChangeListener(OnAttachStateChangeListener onAttachStateChangeListener) {
        ListenerInfo listenerInfo = getListenerInfo();
        if (listenerInfo.mOnAttachStateChangeListeners == null) {
            listenerInfo.mOnAttachStateChangeListeners = new CopyOnWriteArrayList();
        }
        listenerInfo.mOnAttachStateChangeListeners.add(onAttachStateChangeListener);
    }

    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener onAttachStateChangeListener) {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnAttachStateChangeListeners != null) {
            listenerInfo.mOnAttachStateChangeListeners.remove(onAttachStateChangeListener);
        }
    }

    public OnFocusChangeListener getOnFocusChangeListener() {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null) {
            return listenerInfo.mOnFocusChangeListener;
        }
        return null;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        if (!isClickable()) {
            setClickable(true);
        }
        getListenerInfo().mOnClickListener = onClickListener;
    }

    public boolean hasOnClickListeners() {
        ListenerInfo listenerInfo = this.mListenerInfo;
        return (listenerInfo == null || listenerInfo.mOnClickListener == null) ? false : true;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnLongClickListener = onLongClickListener;
    }

    public void setOnContextClickListener(OnContextClickListener onContextClickListener) {
        if (!isContextClickable()) {
            setContextClickable(true);
        }
        getListenerInfo().mOnContextClickListener = onContextClickListener;
    }

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener onCreateContextMenuListener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnCreateContextMenuListener = onCreateContextMenuListener;
    }

    public void addFrameMetricsListener(Window window, Window.OnFrameMetricsAvailableListener onFrameMetricsAvailableListener, Handler handler) {
        if (this.mAttachInfo != null) {
            if (this.mAttachInfo.mThreadedRenderer != null) {
                if (this.mFrameMetricsObservers == null) {
                    this.mFrameMetricsObservers = new ArrayList<>();
                }
                FrameMetricsObserver frameMetricsObserver = new FrameMetricsObserver(window, handler.getLooper(), onFrameMetricsAvailableListener);
                this.mFrameMetricsObservers.add(frameMetricsObserver);
                this.mAttachInfo.mThreadedRenderer.addFrameMetricsObserver(frameMetricsObserver);
                return;
            }
            Log.w(VIEW_LOG_TAG, "View not hardware-accelerated. Unable to observe frame stats");
            return;
        }
        if (this.mFrameMetricsObservers == null) {
            this.mFrameMetricsObservers = new ArrayList<>();
        }
        this.mFrameMetricsObservers.add(new FrameMetricsObserver(window, handler.getLooper(), onFrameMetricsAvailableListener));
    }

    public void removeFrameMetricsListener(Window.OnFrameMetricsAvailableListener onFrameMetricsAvailableListener) {
        ThreadedRenderer threadedRenderer = getThreadedRenderer();
        FrameMetricsObserver frameMetricsObserverFindFrameMetricsObserver = findFrameMetricsObserver(onFrameMetricsAvailableListener);
        if (frameMetricsObserverFindFrameMetricsObserver == null) {
            throw new IllegalArgumentException("attempt to remove OnFrameMetricsAvailableListener that was never added");
        }
        if (this.mFrameMetricsObservers != null) {
            this.mFrameMetricsObservers.remove(frameMetricsObserverFindFrameMetricsObserver);
            if (threadedRenderer != null) {
                threadedRenderer.removeFrameMetricsObserver(frameMetricsObserverFindFrameMetricsObserver);
            }
        }
    }

    private void registerPendingFrameMetricsObservers() {
        if (this.mFrameMetricsObservers != null) {
            ThreadedRenderer threadedRenderer = getThreadedRenderer();
            if (threadedRenderer != null) {
                Iterator<FrameMetricsObserver> it = this.mFrameMetricsObservers.iterator();
                while (it.hasNext()) {
                    threadedRenderer.addFrameMetricsObserver(it.next());
                }
                return;
            }
            Log.w(VIEW_LOG_TAG, "View not hardware-accelerated. Unable to observe frame stats");
        }
    }

    private FrameMetricsObserver findFrameMetricsObserver(Window.OnFrameMetricsAvailableListener onFrameMetricsAvailableListener) {
        for (int i = 0; i < this.mFrameMetricsObservers.size(); i++) {
            FrameMetricsObserver frameMetricsObserver = this.mFrameMetricsObservers.get(i);
            if (frameMetricsObserver.mListener == onFrameMetricsAvailableListener) {
                return frameMetricsObserver;
            }
        }
        return null;
    }

    public void setNotifyAutofillManagerOnClick(boolean z) {
        if (z) {
            this.mPrivateFlags |= 536870912;
        } else {
            this.mPrivateFlags &= -536870913;
        }
    }

    private void notifyAutofillManagerOnClick() {
        if ((this.mPrivateFlags & 536870912) != 0) {
            try {
                getAutofillManager().notifyViewClicked(this);
            } finally {
                this.mPrivateFlags = (-536870913) & this.mPrivateFlags;
            }
        }
    }

    private boolean performClickInternal() {
        notifyAutofillManagerOnClick();
        return performClick();
    }

    public boolean performClick() {
        notifyAutofillManagerOnClick();
        ListenerInfo listenerInfo = this.mListenerInfo;
        boolean z = false;
        if (listenerInfo != null && listenerInfo.mOnClickListener != null) {
            playSoundEffect(0);
            if (ViewDebugManager.DEBUG_TOUCH) {
                Log.d(VIEW_LOG_TAG, "(View)performClick, listener = " + listenerInfo.mOnClickListener + ",this = " + this);
            }
            listenerInfo.mOnClickListener.onClick(this);
            z = true;
        }
        sendAccessibilityEvent(1);
        notifyEnterOrExitForAutoFillIfNeeded(true);
        return z;
    }

    public boolean callOnClick() {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnClickListener != null) {
            listenerInfo.mOnClickListener.onClick(this);
            return true;
        }
        return false;
    }

    public boolean performLongClick() {
        return performLongClickInternal(this.mLongClickX, this.mLongClickY);
    }

    public boolean performLongClick(float f, float f2) {
        this.mLongClickX = f;
        this.mLongClickY = f2;
        boolean zPerformLongClick = performLongClick();
        this.mLongClickX = Float.NaN;
        this.mLongClickY = Float.NaN;
        return zPerformLongClick;
    }

    private boolean performLongClickInternal(float f, float f2) {
        boolean zShowLongClickTooltip;
        sendAccessibilityEvent(2);
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnLongClickListener != null) {
            zShowLongClickTooltip = listenerInfo.mOnLongClickListener.onLongClick(this);
        } else {
            zShowLongClickTooltip = false;
        }
        if (!zShowLongClickTooltip) {
            zShowLongClickTooltip = !Float.isNaN(f) && !Float.isNaN(f2) ? showContextMenu(f, f2) : showContextMenu();
        }
        if ((this.mViewFlags & 1073741824) == 1073741824 && !zShowLongClickTooltip) {
            zShowLongClickTooltip = showLongClickTooltip((int) f, (int) f2);
        }
        if (zShowLongClickTooltip) {
            performHapticFeedback(0);
        }
        return zShowLongClickTooltip;
    }

    public boolean performContextClick(float f, float f2) {
        return performContextClick();
    }

    public boolean performContextClick() {
        boolean zOnContextClick;
        sendAccessibilityEvent(8388608);
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnContextClickListener != null) {
            zOnContextClick = listenerInfo.mOnContextClickListener.onContextClick(this);
        } else {
            zOnContextClick = false;
        }
        if (zOnContextClick) {
            performHapticFeedback(6);
        }
        return zOnContextClick;
    }

    protected boolean performButtonActionOnTouchDown(MotionEvent motionEvent) {
        if (motionEvent.isFromSource(8194) && (motionEvent.getButtonState() & 2) != 0) {
            showContextMenu(motionEvent.getX(), motionEvent.getY());
            this.mPrivateFlags |= 67108864;
            return true;
        }
        return false;
    }

    public boolean showContextMenu() {
        return getParent().showContextMenuForChild(this);
    }

    public boolean showContextMenu(float f, float f2) {
        return getParent().showContextMenuForChild(this, f, f2);
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        return startActionMode(callback, 0);
    }

    public ActionMode startActionMode(ActionMode.Callback callback, int i) {
        ViewParent parent = getParent();
        if (parent == null) {
            return null;
        }
        try {
            return parent.startActionModeForChild(this, callback, i);
        } catch (AbstractMethodError e) {
            return parent.startActionModeForChild(this, callback);
        }
    }

    public void startActivityForResult(Intent intent, int i) {
        this.mStartActivityRequestWho = "@android:view:" + System.identityHashCode(this);
        getContext().startActivityForResult(this.mStartActivityRequestWho, intent, i, null);
    }

    public boolean dispatchActivityResult(String str, int i, int i2, Intent intent) {
        if (this.mStartActivityRequestWho != null && this.mStartActivityRequestWho.equals(str)) {
            onActivityResult(i, i2, intent);
            this.mStartActivityRequestWho = null;
            return true;
        }
        return false;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
    }

    public void setOnKeyListener(OnKeyListener onKeyListener) {
        getListenerInfo().mOnKeyListener = onKeyListener;
    }

    public void setOnTouchListener(OnTouchListener onTouchListener) {
        getListenerInfo().mOnTouchListener = onTouchListener;
    }

    public void setOnGenericMotionListener(OnGenericMotionListener onGenericMotionListener) {
        getListenerInfo().mOnGenericMotionListener = onGenericMotionListener;
    }

    public void setOnHoverListener(OnHoverListener onHoverListener) {
        getListenerInfo().mOnHoverListener = onHoverListener;
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        getListenerInfo().mOnDragListener = onDragListener;
    }

    void handleFocusGainInternal(int i, Rect rect) {
        if (ViewDebugManager.DEBUG_FOCUS) {
            System.out.println(this + " requestFocus()");
            Log.d(VIEW_LOG_TAG, "handleFocusGainInternal: this = " + this + ", callstack = ", new Throwable("ViewFocus"));
        }
        if ((this.mPrivateFlags & 2) == 0) {
            this.mPrivateFlags |= 2;
            View viewFindFocus = this.mAttachInfo != null ? getRootView().findFocus() : null;
            if (this.mParent != null) {
                this.mParent.requestChildFocus(this, this);
                updateFocusedInCluster(viewFindFocus, i);
            }
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(viewFindFocus, this);
            }
            onFocusChanged(true, i, rect);
            refreshDrawableState();
        }
    }

    public final void setRevealOnFocusHint(boolean z) {
        if (z) {
            this.mPrivateFlags3 &= -67108865;
        } else {
            this.mPrivateFlags3 |= 67108864;
        }
    }

    public final boolean getRevealOnFocusHint() {
        return (this.mPrivateFlags3 & 67108864) == 0;
    }

    public void getHotspotBounds(Rect rect) {
        Drawable background = getBackground();
        if (background != null) {
            background.getHotspotBounds(rect);
        } else {
            getBoundsOnScreen(rect);
        }
    }

    public boolean requestRectangleOnScreen(Rect rect) {
        return requestRectangleOnScreen(rect, false);
    }

    public boolean requestRectangleOnScreen(Rect rect, boolean z) {
        boolean zRequestChildRectangleOnScreen = false;
        if (this.mParent == null) {
            return false;
        }
        RectF rectF = this.mAttachInfo != null ? this.mAttachInfo.mTmpTransformRect : new RectF();
        rectF.set(rect);
        ViewParent parent = this.mParent;
        View view = this;
        while (parent != null) {
            rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            zRequestChildRectangleOnScreen |= parent.requestChildRectangleOnScreen(view, rect, z);
            if (!(parent instanceof View)) {
                break;
            }
            rectF.offset(view.mLeft - view.getScrollX(), view.mTop - view.getScrollY());
            view = (View) parent;
            parent = view.getParent();
        }
        return zRequestChildRectangleOnScreen;
    }

    public void clearFocus() {
        if (ViewDebugManager.DEBUG_FOCUS) {
            System.out.println(this + " clearFocus()");
        }
        clearFocusInternal(null, true, sAlwaysAssignFocus || !isInTouchMode());
    }

    void clearFocusInternal(View view, boolean z, boolean z2) {
        if ((this.mPrivateFlags & 2) != 0) {
            this.mPrivateFlags &= -3;
            clearParentsWantFocus();
            if (z && this.mParent != null) {
                this.mParent.clearChildFocus(this);
            }
            onFocusChanged(false, 0, null);
            refreshDrawableState();
            if (z) {
                if (!z2 || !rootViewRequestFocus()) {
                    notifyGlobalFocusCleared(this);
                }
            }
        }
    }

    void notifyGlobalFocusCleared(View view) {
        if (view != null && this.mAttachInfo != null) {
            this.mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(view, null);
        }
    }

    boolean rootViewRequestFocus() {
        View rootView = getRootView();
        return rootView != null && rootView.requestFocus();
    }

    void unFocus(View view) {
        if (ViewDebugManager.DEBUG_FOCUS) {
            System.out.println(this + " unFocus()");
        }
        clearFocusInternal(view, false, false);
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public boolean hasFocus() {
        return (this.mPrivateFlags & 2) != 0;
    }

    public boolean hasFocusable() {
        return hasFocusable(!sHasFocusableExcludeAutoFocusable, false);
    }

    public boolean hasExplicitFocusable() {
        return hasFocusable(false, true);
    }

    boolean hasFocusable(boolean z, boolean z2) {
        if (!isFocusableInTouchMode()) {
            for (ViewParent parent = this.mParent; parent instanceof ViewGroup; parent = parent.getParent()) {
                if (((ViewGroup) parent).shouldBlockFocusForTouchscreen()) {
                    return false;
                }
            }
        }
        if ((this.mViewFlags & 12) == 0 && (this.mViewFlags & 32) == 0) {
            return (z || getFocusable() != 16) && isFocusable();
        }
        return false;
    }

    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (z) {
            sendAccessibilityEvent(8);
        } else {
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
        switchDefaultFocusHighlight();
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (ViewDebugManager.DEBUG_FOCUS) {
            Log.d(VIEW_LOG_TAG, "onFocusChanged: gainFocus = " + z + ",direction = " + i + ",imm = " + inputMethodManagerPeekInstance + ",this = " + this);
        }
        if (!z) {
            if (isPressed()) {
                setPressed(false);
            }
            if (inputMethodManagerPeekInstance != null && this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus) {
                inputMethodManagerPeekInstance.focusOut(this);
            }
            onFocusLost();
        } else if (inputMethodManagerPeekInstance != null && this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus) {
            inputMethodManagerPeekInstance.focusIn(this);
        }
        invalidate(true);
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnFocusChangeListener != null) {
            listenerInfo.mOnFocusChangeListener.onFocusChange(this, z);
        }
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mKeyDispatchState.reset(this);
        }
        notifyEnterOrExitForAutoFillIfNeeded(z);
    }

    public void notifyEnterOrExitForAutoFillIfNeeded(boolean z) {
        AutofillManager autofillManager;
        if (canNotifyAutofillEnterExitEvent() && (autofillManager = getAutofillManager()) != null) {
            if (z && isFocused()) {
                if (!isLaidOut()) {
                    this.mPrivateFlags3 |= 134217728;
                    return;
                } else {
                    if (isVisibleToUser()) {
                        autofillManager.notifyViewEntered(this);
                        return;
                    }
                    return;
                }
            }
            if (!z && !isFocused()) {
                autofillManager.notifyViewExited(this);
            }
        }
    }

    public void setAccessibilityPaneTitle(CharSequence charSequence) {
        if (!TextUtils.equals(charSequence, this.mAccessibilityPaneTitle)) {
            this.mAccessibilityPaneTitle = charSequence;
            notifyViewAccessibilityStateChangedIfNeeded(8);
        }
    }

    public CharSequence getAccessibilityPaneTitle() {
        return this.mAccessibilityPaneTitle;
    }

    private boolean isAccessibilityPane() {
        return this.mAccessibilityPaneTitle != null;
    }

    @Override
    public void sendAccessibilityEvent(int i) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.sendAccessibilityEvent(this, i);
        } else {
            sendAccessibilityEventInternal(i);
        }
    }

    public void announceForAccessibility(CharSequence charSequence) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled() && this.mParent != null) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(16384);
            onInitializeAccessibilityEvent(accessibilityEventObtain);
            accessibilityEventObtain.getText().add(charSequence);
            accessibilityEventObtain.setContentDescription(null);
            this.mParent.requestSendAccessibilityEvent(this, accessibilityEventObtain);
        }
    }

    public void sendAccessibilityEventInternal(int i) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(i));
        }
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent accessibilityEvent) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.sendAccessibilityEventUnchecked(this, accessibilityEvent);
        } else {
            sendAccessibilityEventUncheckedInternal(accessibilityEvent);
        }
    }

    public void sendAccessibilityEventUncheckedInternal(AccessibilityEvent accessibilityEvent) {
        boolean z = false;
        if ((accessibilityEvent.getEventType() == 32) && (accessibilityEvent.getContentChangeTypes() & 32) != 0) {
            z = true;
        }
        if (!isShown() && !z) {
            return;
        }
        onInitializeAccessibilityEvent(accessibilityEvent);
        if ((accessibilityEvent.getEventType() & POPULATING_ACCESSIBILITY_EVENT_TYPES) != 0) {
            dispatchPopulateAccessibilityEvent(accessibilityEvent);
        }
        if (getParent() != null) {
            getParent().requestSendAccessibilityEvent(this, accessibilityEvent);
        }
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (this.mAccessibilityDelegate != null) {
            return this.mAccessibilityDelegate.dispatchPopulateAccessibilityEvent(this, accessibilityEvent);
        }
        return dispatchPopulateAccessibilityEventInternal(accessibilityEvent);
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        onPopulateAccessibilityEvent(accessibilityEvent);
        return false;
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onPopulateAccessibilityEvent(this, accessibilityEvent);
        } else {
            onPopulateAccessibilityEventInternal(accessibilityEvent);
        }
    }

    public void onPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == 32 && !TextUtils.isEmpty(getAccessibilityPaneTitle())) {
            accessibilityEvent.getText().add(getAccessibilityPaneTitle());
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onInitializeAccessibilityEvent(this, accessibilityEvent);
        } else {
            onInitializeAccessibilityEventInternal(accessibilityEvent);
        }
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        CharSequence iterableTextForAccessibility;
        accessibilityEvent.setSource(this);
        accessibilityEvent.setClassName(getAccessibilityClassName());
        accessibilityEvent.setPackageName(getContext().getPackageName());
        accessibilityEvent.setEnabled(isEnabled());
        accessibilityEvent.setContentDescription(this.mContentDescription);
        int eventType = accessibilityEvent.getEventType();
        if (eventType == 8) {
            ArrayList<View> arrayList = this.mAttachInfo != null ? this.mAttachInfo.mTempArrayList : new ArrayList<>();
            getRootView().addFocusables(arrayList, 2, 0);
            accessibilityEvent.setItemCount(arrayList.size());
            accessibilityEvent.setCurrentItemIndex(arrayList.indexOf(this));
            if (this.mAttachInfo != null) {
                arrayList.clear();
                return;
            }
            return;
        }
        if (eventType == 8192 && (iterableTextForAccessibility = getIterableTextForAccessibility()) != null && iterableTextForAccessibility.length() > 0) {
            accessibilityEvent.setFromIndex(getAccessibilitySelectionStart());
            accessibilityEvent.setToIndex(getAccessibilitySelectionEnd());
            accessibilityEvent.setItemCount(iterableTextForAccessibility.length());
        }
    }

    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        if (this.mAccessibilityDelegate != null) {
            return this.mAccessibilityDelegate.createAccessibilityNodeInfo(this);
        }
        return createAccessibilityNodeInfoInternal();
    }

    public AccessibilityNodeInfo createAccessibilityNodeInfoInternal() {
        AccessibilityNodeProvider accessibilityNodeProvider = getAccessibilityNodeProvider();
        if (accessibilityNodeProvider != null) {
            return accessibilityNodeProvider.createAccessibilityNodeInfo(-1);
        }
        AccessibilityNodeInfo accessibilityNodeInfoObtain = AccessibilityNodeInfo.obtain(this);
        onInitializeAccessibilityNodeInfo(accessibilityNodeInfoObtain);
        return accessibilityNodeInfoObtain;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (this.mAccessibilityDelegate != null) {
            this.mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(this, accessibilityNodeInfo);
        } else {
            onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        }
    }

    public void getBoundsOnScreen(Rect rect) {
        getBoundsOnScreen(rect, false);
    }

    public void getBoundsOnScreen(Rect rect, boolean z) {
        if (this.mAttachInfo == null) {
            return;
        }
        RectF rectF = this.mAttachInfo.mTmpTransformRect;
        rectF.set(0.0f, 0.0f, this.mRight - this.mLeft, this.mBottom - this.mTop);
        mapRectFromViewToScreenCoords(rectF, z);
        rect.set(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    public void mapRectFromViewToScreenCoords(RectF rectF, boolean z) {
        if (!hasIdentityMatrix()) {
            getMatrix().mapRect(rectF);
        }
        rectF.offset(this.mLeft, this.mTop);
        Object obj = this.mParent;
        while (obj instanceof View) {
            View view = (View) obj;
            rectF.offset(-view.mScrollX, -view.mScrollY);
            if (z) {
                rectF.left = Math.max(rectF.left, 0.0f);
                rectF.top = Math.max(rectF.top, 0.0f);
                rectF.right = Math.min(rectF.right, view.getWidth());
                rectF.bottom = Math.min(rectF.bottom, view.getHeight());
            }
            if (!view.hasIdentityMatrix()) {
                view.getMatrix().mapRect(rectF);
            }
            rectF.offset(view.mLeft, view.mTop);
            obj = view.mParent;
        }
        if (obj instanceof ViewRootImpl) {
            rectF.offset(0.0f, -((ViewRootImpl) obj).mCurScrollY);
        }
        rectF.offset(this.mAttachInfo.mWindowLeft, this.mAttachInfo.mWindowTop);
    }

    public CharSequence getAccessibilityClassName() {
        return View.class.getName();
    }

    public void onProvideStructure(ViewStructure viewStructure) {
        onProvideStructureForAssistOrAutofill(viewStructure, false, 0);
    }

    public void onProvideAutofillStructure(ViewStructure viewStructure, int i) {
        onProvideStructureForAssistOrAutofill(viewStructure, true, i);
    }

    private void onProvideStructureForAssistOrAutofill(ViewStructure viewStructure, boolean z, int i) {
        int i2;
        String resourcePackageName;
        String resourceEntryName;
        String resourceTypeName;
        int i3 = this.mID;
        View view = null;
        if (i3 != -1 && !isViewIdGenerated(i3)) {
            try {
                Resources resources = getResources();
                resourceEntryName = resources.getResourceEntryName(i3);
                resourceTypeName = resources.getResourceTypeName(i3);
                resourcePackageName = resources.getResourcePackageName(i3);
            } catch (Resources.NotFoundException e) {
                resourcePackageName = null;
                resourceEntryName = null;
                resourceTypeName = null;
            }
            viewStructure.setId(i3, resourcePackageName, resourceTypeName, resourceEntryName);
        } else {
            viewStructure.setId(i3, null, null, null);
        }
        if (z) {
            int autofillType = getAutofillType();
            if (autofillType != 0) {
                viewStructure.setAutofillType(autofillType);
                viewStructure.setAutofillHints(getAutofillHints());
                viewStructure.setAutofillValue(getAutofillValue());
            }
            viewStructure.setImportantForAutofill(getImportantForAutofill());
        }
        int i4 = 0;
        if (z && (i & 1) == 0) {
            Object parent = getParent();
            if (parent instanceof View) {
                view = (View) parent;
            }
            i2 = 0;
            while (view != null && !view.isImportantForAutofill()) {
                i4 += view.mLeft;
                i2 += view.mTop;
                Object parent2 = view.getParent();
                if (!(parent2 instanceof View)) {
                    break;
                } else {
                    view = (View) parent2;
                }
            }
        } else {
            i2 = 0;
        }
        viewStructure.setDimens(i4 + this.mLeft, i2 + this.mTop, this.mScrollX, this.mScrollY, this.mRight - this.mLeft, this.mBottom - this.mTop);
        if (!z) {
            if (!hasIdentityMatrix()) {
                viewStructure.setTransformation(getMatrix());
            }
            viewStructure.setElevation(getZ());
        }
        viewStructure.setVisibility(getVisibility());
        viewStructure.setEnabled(isEnabled());
        if (isClickable()) {
            viewStructure.setClickable(true);
        }
        if (isFocusable()) {
            viewStructure.setFocusable(true);
        }
        if (isFocused()) {
            viewStructure.setFocused(true);
        }
        if (isAccessibilityFocused()) {
            viewStructure.setAccessibilityFocused(true);
        }
        if (isSelected()) {
            viewStructure.setSelected(true);
        }
        if (isActivated()) {
            viewStructure.setActivated(true);
        }
        if (isLongClickable()) {
            viewStructure.setLongClickable(true);
        }
        if (this instanceof Checkable) {
            viewStructure.setCheckable(true);
            if (((Checkable) this).isChecked()) {
                viewStructure.setChecked(true);
            }
        }
        if (isOpaque()) {
            viewStructure.setOpaque(true);
        }
        if (isContextClickable()) {
            viewStructure.setContextClickable(true);
        }
        viewStructure.setClassName(getAccessibilityClassName().toString());
        viewStructure.setContentDescription(getContentDescription());
    }

    public void onProvideVirtualStructure(ViewStructure viewStructure) {
        onProvideVirtualStructureCompat(viewStructure, false);
    }

    private void onProvideVirtualStructureCompat(ViewStructure viewStructure, boolean z) {
        AccessibilityNodeProvider accessibilityNodeProvider = getAccessibilityNodeProvider();
        if (accessibilityNodeProvider != null) {
            if (Helper.sVerbose && z) {
                Log.v(VIEW_LOG_TAG, "onProvideVirtualStructureCompat() for " + this);
            }
            AccessibilityNodeInfo accessibilityNodeInfoCreateAccessibilityNodeInfo = createAccessibilityNodeInfo();
            viewStructure.setChildCount(1);
            populateVirtualStructure(viewStructure.newChild(0), accessibilityNodeProvider, accessibilityNodeInfoCreateAccessibilityNodeInfo, z);
            accessibilityNodeInfoCreateAccessibilityNodeInfo.recycle();
        }
    }

    public void onProvideAutofillVirtualStructure(ViewStructure viewStructure, int i) {
        if (this.mContext.isAutofillCompatibilityEnabled()) {
            onProvideVirtualStructureCompat(viewStructure, true);
        }
    }

    public void autofill(AutofillValue autofillValue) {
    }

    public void autofill(SparseArray<AutofillValue> sparseArray) {
        AccessibilityNodeProvider accessibilityNodeProvider;
        if (!this.mContext.isAutofillCompatibilityEnabled() || (accessibilityNodeProvider = getAccessibilityNodeProvider()) == null) {
            return;
        }
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            AutofillValue autofillValueValueAt = sparseArray.valueAt(i);
            if (autofillValueValueAt.isText()) {
                int iKeyAt = sparseArray.keyAt(i);
                CharSequence textValue = autofillValueValueAt.getTextValue();
                Bundle bundle = new Bundle();
                bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textValue);
                accessibilityNodeProvider.performAction(iKeyAt, 2097152, bundle);
            }
        }
    }

    public final AutofillId getAutofillId() {
        if (this.mAutofillId == null) {
            this.mAutofillId = new AutofillId(getAutofillViewId());
        }
        return this.mAutofillId;
    }

    public void setAutofillId(AutofillId autofillId) {
        if (Helper.sVerbose) {
            Log.v(VIEW_LOG_TAG, "setAutofill(): from " + this.mAutofillId + " to " + autofillId);
        }
        if (isAttachedToWindow()) {
            throw new IllegalStateException("Cannot set autofill id when view is attached");
        }
        if (autofillId != null && autofillId.isVirtual()) {
            throw new IllegalStateException("Cannot set autofill id assigned to virtual views");
        }
        if (autofillId == null && (this.mPrivateFlags3 & 1073741824) == 0) {
            return;
        }
        this.mAutofillId = autofillId;
        if (autofillId != null) {
            this.mAutofillViewId = autofillId.getViewId();
            this.mPrivateFlags3 |= 1073741824;
        } else {
            this.mAutofillViewId = -1;
            this.mPrivateFlags3 &= -1073741825;
        }
    }

    public int getAutofillType() {
        return 0;
    }

    @ViewDebug.ExportedProperty
    public String[] getAutofillHints() {
        return this.mAutofillHints;
    }

    public boolean isAutofilled() {
        return (this.mPrivateFlags3 & 65536) != 0;
    }

    public AutofillValue getAutofillValue() {
        return null;
    }

    @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = 0, to = "auto"), @ViewDebug.IntToString(from = 1, to = "yes"), @ViewDebug.IntToString(from = 2, to = "no"), @ViewDebug.IntToString(from = 4, to = "yesExcludeDescendants"), @ViewDebug.IntToString(from = 8, to = "noExcludeDescendants")})
    public int getImportantForAutofill() {
        return (this.mPrivateFlags3 & PFLAG3_IMPORTANT_FOR_AUTOFILL_MASK) >> 19;
    }

    public void setImportantForAutofill(int i) {
        this.mPrivateFlags3 &= -7864321;
        this.mPrivateFlags3 = ((i << 19) & PFLAG3_IMPORTANT_FOR_AUTOFILL_MASK) | this.mPrivateFlags3;
    }

    public final boolean isImportantForAutofill() {
        String resourceEntryName;
        String resourcePackageName;
        for (ViewParent parent = this.mParent; parent instanceof View; parent = parent.getParent()) {
            int importantForAutofill = ((View) parent).getImportantForAutofill();
            if (importantForAutofill == 8 || importantForAutofill == 4) {
                return false;
            }
        }
        int importantForAutofill2 = getImportantForAutofill();
        if (importantForAutofill2 == 4 || importantForAutofill2 == 1) {
            return true;
        }
        if (importantForAutofill2 == 8 || importantForAutofill2 == 2) {
            return false;
        }
        int i = this.mID;
        if (i != -1 && !isViewIdGenerated(i)) {
            Resources resources = getResources();
            try {
                resourceEntryName = resources.getResourceEntryName(i);
                try {
                    resourcePackageName = resources.getResourcePackageName(i);
                } catch (Resources.NotFoundException e) {
                    resourcePackageName = null;
                }
            } catch (Resources.NotFoundException e2) {
                resourceEntryName = null;
            }
            if (resourceEntryName != null && resourcePackageName != null && resourcePackageName.equals(this.mContext.getPackageName())) {
                return true;
            }
        }
        return getAutofillHints() != null;
    }

    private AutofillManager getAutofillManager() {
        return (AutofillManager) this.mContext.getSystemService(AutofillManager.class);
    }

    private boolean isAutofillable() {
        return getAutofillType() != 0 && isImportantForAutofill() && getAutofillViewId() > 1073741823;
    }

    public boolean canNotifyAutofillEnterExitEvent() {
        return isAutofillable() && isAttachedToWindow();
    }

    private void populateVirtualStructure(ViewStructure viewStructure, AccessibilityNodeProvider accessibilityNodeProvider, AccessibilityNodeInfo accessibilityNodeInfo, boolean z) {
        viewStructure.setId(AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfo.getSourceNodeId()), null, null, accessibilityNodeInfo.getViewIdResourceName());
        Rect tempRect = viewStructure.getTempRect();
        accessibilityNodeInfo.getBoundsInParent(tempRect);
        viewStructure.setDimens(tempRect.left, tempRect.top, 0, 0, tempRect.width(), tempRect.height());
        viewStructure.setVisibility(0);
        viewStructure.setEnabled(accessibilityNodeInfo.isEnabled());
        if (accessibilityNodeInfo.isClickable()) {
            viewStructure.setClickable(true);
        }
        if (accessibilityNodeInfo.isFocusable()) {
            viewStructure.setFocusable(true);
        }
        if (accessibilityNodeInfo.isFocused()) {
            viewStructure.setFocused(true);
        }
        if (accessibilityNodeInfo.isAccessibilityFocused()) {
            viewStructure.setAccessibilityFocused(true);
        }
        if (accessibilityNodeInfo.isSelected()) {
            viewStructure.setSelected(true);
        }
        if (accessibilityNodeInfo.isLongClickable()) {
            viewStructure.setLongClickable(true);
        }
        if (accessibilityNodeInfo.isCheckable()) {
            viewStructure.setCheckable(true);
            if (accessibilityNodeInfo.isChecked()) {
                viewStructure.setChecked(true);
            }
        }
        if (accessibilityNodeInfo.isContextClickable()) {
            viewStructure.setContextClickable(true);
        }
        if (z) {
            viewStructure.setAutofillId(new AutofillId(getAutofillId(), AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfo.getSourceNodeId())));
        }
        CharSequence className = accessibilityNodeInfo.getClassName();
        viewStructure.setClassName(className != null ? className.toString() : null);
        viewStructure.setContentDescription(accessibilityNodeInfo.getContentDescription());
        if (z) {
            int maxTextLength = accessibilityNodeInfo.getMaxTextLength();
            if (maxTextLength != -1) {
                viewStructure.setMaxTextLength(maxTextLength);
            }
            viewStructure.setHint(accessibilityNodeInfo.getHintText());
        }
        CharSequence text = accessibilityNodeInfo.getText();
        boolean z2 = (text == null && accessibilityNodeInfo.getError() == null) ? false : true;
        if (z2) {
            viewStructure.setText(text, accessibilityNodeInfo.getTextSelectionStart(), accessibilityNodeInfo.getTextSelectionEnd());
        }
        if (z) {
            if (accessibilityNodeInfo.isEditable()) {
                viewStructure.setDataIsSensitive(true);
                if (z2) {
                    viewStructure.setAutofillType(1);
                    viewStructure.setAutofillValue(AutofillValue.forText(text));
                }
                int inputType = accessibilityNodeInfo.getInputType();
                if (inputType == 0 && accessibilityNodeInfo.isPassword()) {
                    inputType = 129;
                }
                viewStructure.setInputType(inputType);
            } else {
                viewStructure.setDataIsSensitive(false);
            }
        }
        int childCount = accessibilityNodeInfo.getChildCount();
        if (childCount > 0) {
            viewStructure.setChildCount(childCount);
            for (int i = 0; i < childCount; i++) {
                if (AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfo.getChildNodeIds().get(i)) == -1) {
                    Log.e(VIEW_LOG_TAG, "Virtual view pointing to its host. Ignoring");
                } else {
                    AccessibilityNodeInfo accessibilityNodeInfoCreateAccessibilityNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeInfo.getChildId(i)));
                    populateVirtualStructure(viewStructure.newChild(i), accessibilityNodeProvider, accessibilityNodeInfoCreateAccessibilityNodeInfo, z);
                    accessibilityNodeInfoCreateAccessibilityNodeInfo.recycle();
                }
            }
        }
    }

    public void dispatchProvideStructure(ViewStructure viewStructure) {
        dispatchProvideStructureForAssistOrAutofill(viewStructure, false, 0);
    }

    public void dispatchProvideAutofillStructure(ViewStructure viewStructure, int i) {
        dispatchProvideStructureForAssistOrAutofill(viewStructure, true, i);
    }

    private void dispatchProvideStructureForAssistOrAutofill(ViewStructure viewStructure, boolean z, int i) {
        if (z) {
            viewStructure.setAutofillId(getAutofillId());
            onProvideAutofillStructure(viewStructure, i);
            onProvideAutofillVirtualStructure(viewStructure, i);
        } else if (!isAssistBlocked()) {
            onProvideStructure(viewStructure);
            onProvideVirtualStructure(viewStructure);
        } else {
            viewStructure.setClassName(getAccessibilityClassName().toString());
            viewStructure.setAssistBlocked(true);
        }
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        AccessibilityNodeInfo.AccessibilityAction accessibilityAction;
        if (this.mAttachInfo == null) {
            return;
        }
        Rect rect = this.mAttachInfo.mTmpInvalRect;
        getDrawingRect(rect);
        accessibilityNodeInfo.setBoundsInParent(rect);
        getBoundsOnScreen(rect, true);
        accessibilityNodeInfo.setBoundsInScreen(rect);
        Object parentForAccessibility = getParentForAccessibility();
        if (parentForAccessibility instanceof View) {
            accessibilityNodeInfo.setParent((View) parentForAccessibility);
        }
        if (this.mID != -1) {
            View rootView = getRootView();
            if (rootView == null) {
                rootView = this;
            }
            View viewFindLabelForView = rootView.findLabelForView(this, this.mID);
            if (viewFindLabelForView != null) {
                accessibilityNodeInfo.setLabeledBy(viewFindLabelForView);
            }
            if ((this.mAttachInfo.mAccessibilityFetchFlags & 16) != 0 && Resources.resourceHasPackage(this.mID)) {
                try {
                    accessibilityNodeInfo.setViewIdResourceName(getResources().getResourceName(this.mID));
                } catch (Resources.NotFoundException e) {
                }
            }
        }
        if (this.mLabelForId != -1) {
            View rootView2 = getRootView();
            if (rootView2 == null) {
                rootView2 = this;
            }
            View viewFindViewInsideOutShouldExist = rootView2.findViewInsideOutShouldExist(this, this.mLabelForId);
            if (viewFindViewInsideOutShouldExist != null) {
                accessibilityNodeInfo.setLabelFor(viewFindViewInsideOutShouldExist);
            }
        }
        if (this.mAccessibilityTraversalBeforeId != -1) {
            View rootView3 = getRootView();
            if (rootView3 == null) {
                rootView3 = this;
            }
            View viewFindViewInsideOutShouldExist2 = rootView3.findViewInsideOutShouldExist(this, this.mAccessibilityTraversalBeforeId);
            if (viewFindViewInsideOutShouldExist2 != null && viewFindViewInsideOutShouldExist2.includeForAccessibility()) {
                accessibilityNodeInfo.setTraversalBefore(viewFindViewInsideOutShouldExist2);
            }
        }
        if (this.mAccessibilityTraversalAfterId != -1) {
            View rootView4 = getRootView();
            if (rootView4 == null) {
                rootView4 = this;
            }
            View viewFindViewInsideOutShouldExist3 = rootView4.findViewInsideOutShouldExist(this, this.mAccessibilityTraversalAfterId);
            if (viewFindViewInsideOutShouldExist3 != null && viewFindViewInsideOutShouldExist3.includeForAccessibility()) {
                accessibilityNodeInfo.setTraversalAfter(viewFindViewInsideOutShouldExist3);
            }
        }
        accessibilityNodeInfo.setVisibleToUser(isVisibleToUser());
        accessibilityNodeInfo.setImportantForAccessibility(isImportantForAccessibility());
        accessibilityNodeInfo.setPackageName(this.mContext.getPackageName());
        accessibilityNodeInfo.setClassName(getAccessibilityClassName());
        accessibilityNodeInfo.setContentDescription(getContentDescription());
        accessibilityNodeInfo.setEnabled(isEnabled());
        accessibilityNodeInfo.setClickable(isClickable());
        accessibilityNodeInfo.setFocusable(isFocusable());
        accessibilityNodeInfo.setScreenReaderFocusable(isScreenReaderFocusable());
        accessibilityNodeInfo.setFocused(isFocused());
        accessibilityNodeInfo.setAccessibilityFocused(isAccessibilityFocused());
        accessibilityNodeInfo.setSelected(isSelected());
        accessibilityNodeInfo.setLongClickable(isLongClickable());
        accessibilityNodeInfo.setContextClickable(isContextClickable());
        accessibilityNodeInfo.setLiveRegion(getAccessibilityLiveRegion());
        if (this.mTooltipInfo != null && this.mTooltipInfo.mTooltipText != null) {
            accessibilityNodeInfo.setTooltipText(this.mTooltipInfo.mTooltipText);
            if (this.mTooltipInfo.mTooltipPopup == null) {
                accessibilityAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_TOOLTIP;
            } else {
                accessibilityAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_HIDE_TOOLTIP;
            }
            accessibilityNodeInfo.addAction(accessibilityAction);
        }
        accessibilityNodeInfo.addAction(4);
        accessibilityNodeInfo.addAction(8);
        if (isFocusable()) {
            if (isFocused()) {
                accessibilityNodeInfo.addAction(2);
            } else {
                accessibilityNodeInfo.addAction(1);
            }
        }
        if (!isAccessibilityFocused()) {
            accessibilityNodeInfo.addAction(64);
        } else {
            accessibilityNodeInfo.addAction(128);
        }
        if (isClickable() && isEnabled()) {
            accessibilityNodeInfo.addAction(16);
        }
        if (isLongClickable() && isEnabled()) {
            accessibilityNodeInfo.addAction(32);
        }
        if (isContextClickable() && isEnabled()) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK);
        }
        CharSequence iterableTextForAccessibility = getIterableTextForAccessibility();
        if (iterableTextForAccessibility != null && iterableTextForAccessibility.length() > 0) {
            accessibilityNodeInfo.setTextSelection(getAccessibilitySelectionStart(), getAccessibilitySelectionEnd());
            accessibilityNodeInfo.addAction(131072);
            accessibilityNodeInfo.addAction(256);
            accessibilityNodeInfo.addAction(512);
            accessibilityNodeInfo.setMovementGranularities(11);
        }
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN);
        populateAccessibilityNodeInfoDrawingOrderInParent(accessibilityNodeInfo);
        accessibilityNodeInfo.setPaneTitle(this.mAccessibilityPaneTitle);
        accessibilityNodeInfo.setHeading(isAccessibilityHeading());
    }

    public void addExtraDataToAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo, String str, Bundle bundle) {
    }

    private void populateAccessibilityNodeInfoDrawingOrderInParent(AccessibilityNodeInfo accessibilityNodeInfo) {
        int childDrawingOrder;
        int i = 0;
        if ((this.mPrivateFlags & 16) == 0) {
            accessibilityNodeInfo.setDrawingOrder(0);
            return;
        }
        ViewParent parentForAccessibility = getParentForAccessibility();
        View view = this;
        int i2 = 1;
        while (true) {
            if (view != parentForAccessibility) {
                Object parent = view.getParent();
                if (!(parent instanceof ViewGroup)) {
                    break;
                }
                ViewGroup viewGroup = (ViewGroup) parent;
                int childCount = viewGroup.getChildCount();
                if (childCount > 1) {
                    ArrayList<View> arrayListBuildOrderedChildList = viewGroup.buildOrderedChildList();
                    if (arrayListBuildOrderedChildList != null) {
                        int iIndexOf = arrayListBuildOrderedChildList.indexOf(view);
                        int iNumViewsForAccessibility = i2;
                        for (int i3 = 0; i3 < iIndexOf; i3++) {
                            iNumViewsForAccessibility += numViewsForAccessibility(arrayListBuildOrderedChildList.get(i3));
                        }
                        i2 = iNumViewsForAccessibility;
                    } else {
                        int iIndexOfChild = viewGroup.indexOfChild(view);
                        boolean zIsChildrenDrawingOrderEnabled = viewGroup.isChildrenDrawingOrderEnabled();
                        if (iIndexOfChild >= 0 && zIsChildrenDrawingOrderEnabled) {
                            iIndexOfChild = viewGroup.getChildDrawingOrder(childCount, iIndexOfChild);
                        }
                        int i4 = zIsChildrenDrawingOrderEnabled ? childCount : iIndexOfChild;
                        if (iIndexOfChild != 0) {
                            int iNumViewsForAccessibility2 = i2;
                            for (int i5 = 0; i5 < i4; i5++) {
                                if (zIsChildrenDrawingOrderEnabled) {
                                    childDrawingOrder = viewGroup.getChildDrawingOrder(childCount, i5);
                                } else {
                                    childDrawingOrder = i5;
                                }
                                if (childDrawingOrder < iIndexOfChild) {
                                    iNumViewsForAccessibility2 += numViewsForAccessibility(viewGroup.getChildAt(i5));
                                }
                            }
                            i2 = iNumViewsForAccessibility2;
                        }
                    }
                }
                view = (View) parent;
            } else {
                i = i2;
                break;
            }
        }
        accessibilityNodeInfo.setDrawingOrder(i);
    }

    private static int numViewsForAccessibility(View view) {
        if (view != null) {
            if (view.includeForAccessibility()) {
                return 1;
            }
            if (view instanceof ViewGroup) {
                return ((ViewGroup) view).getNumChildrenForAccessibility();
            }
            return 0;
        }
        return 0;
    }

    private View findLabelForView(View view, int i) {
        if (this.mMatchLabelForPredicate == null) {
            this.mMatchLabelForPredicate = new MatchLabelForPredicate();
        }
        this.mMatchLabelForPredicate.mLabeledId = i;
        return findViewByPredicateInsideOut(view, this.mMatchLabelForPredicate);
    }

    public boolean isVisibleToUserForAutofill(int i) {
        if (this.mContext.isAutofillCompatibilityEnabled()) {
            AccessibilityNodeProvider accessibilityNodeProvider = getAccessibilityNodeProvider();
            if (accessibilityNodeProvider != null) {
                AccessibilityNodeInfo accessibilityNodeInfoCreateAccessibilityNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(i);
                if (accessibilityNodeInfoCreateAccessibilityNodeInfo != null) {
                    return accessibilityNodeInfoCreateAccessibilityNodeInfo.isVisibleToUser();
                }
                return false;
            }
            Log.w(VIEW_LOG_TAG, "isVisibleToUserForAutofill(" + i + "): no provider");
            return false;
        }
        return true;
    }

    public boolean isVisibleToUser() {
        return isVisibleToUser(null);
    }

    protected boolean isVisibleToUser(Rect rect) {
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
        Rect rect2 = this.mAttachInfo.mTmpInvalRect;
        Point point = this.mAttachInfo.mPoint;
        if (!getGlobalVisibleRect(rect2, point)) {
            return false;
        }
        if (rect != null) {
            rect2.offset(-point.x, -point.y);
            return rect.intersect(rect2);
        }
        return true;
    }

    public AccessibilityDelegate getAccessibilityDelegate() {
        return this.mAccessibilityDelegate;
    }

    public void setAccessibilityDelegate(AccessibilityDelegate accessibilityDelegate) {
        this.mAccessibilityDelegate = accessibilityDelegate;
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

    public int getAutofillViewId() {
        if (this.mAutofillViewId == -1) {
            this.mAutofillViewId = this.mContext.getNextAutofillId();
        }
        return this.mAutofillViewId;
    }

    public int getAccessibilityWindowId() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mAccessibilityWindowId;
        }
        return -1;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE)
    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    @RemotableViewMethod
    public void setContentDescription(CharSequence charSequence) {
        boolean z;
        if (this.mContentDescription == null) {
            if (charSequence == null) {
                return;
            }
        } else if (this.mContentDescription.equals(charSequence)) {
            return;
        }
        this.mContentDescription = charSequence;
        if (charSequence == null || charSequence.length() <= 0) {
            z = false;
        } else {
            z = true;
        }
        if (z && getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
            notifySubtreeAccessibilityStateChangedIfNeeded();
        } else {
            notifyViewAccessibilityStateChangedIfNeeded(4);
        }
    }

    @RemotableViewMethod
    public void setAccessibilityTraversalBefore(int i) {
        if (this.mAccessibilityTraversalBeforeId == i) {
            return;
        }
        this.mAccessibilityTraversalBeforeId = i;
        notifyViewAccessibilityStateChangedIfNeeded(0);
    }

    public int getAccessibilityTraversalBefore() {
        return this.mAccessibilityTraversalBeforeId;
    }

    @RemotableViewMethod
    public void setAccessibilityTraversalAfter(int i) {
        if (this.mAccessibilityTraversalAfterId == i) {
            return;
        }
        this.mAccessibilityTraversalAfterId = i;
        notifyViewAccessibilityStateChangedIfNeeded(0);
    }

    public int getAccessibilityTraversalAfter() {
        return this.mAccessibilityTraversalAfterId;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE)
    public int getLabelFor() {
        return this.mLabelForId;
    }

    @RemotableViewMethod
    public void setLabelFor(int i) {
        if (this.mLabelForId == i) {
            return;
        }
        this.mLabelForId = i;
        if (this.mLabelForId != -1 && this.mID == -1) {
            this.mID = generateViewId();
        }
        notifyViewAccessibilityStateChangedIfNeeded(0);
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

    public void setScrollContainer(boolean z) {
        if (!z) {
            if ((this.mPrivateFlags & 1048576) != 0) {
                this.mAttachInfo.mScrollContainers.remove(this);
            }
            this.mPrivateFlags &= -1572865;
        } else {
            if (this.mAttachInfo != null && (this.mPrivateFlags & 1048576) == 0) {
                this.mAttachInfo.mScrollContainers.add(this);
                this.mPrivateFlags |= 1048576;
            }
            this.mPrivateFlags |= 524288;
        }
    }

    @Deprecated
    public int getDrawingCacheQuality() {
        return this.mViewFlags & DRAWING_CACHE_QUALITY_MASK;
    }

    @Deprecated
    public void setDrawingCacheQuality(int i) {
        setFlags(i, DRAWING_CACHE_QUALITY_MASK);
    }

    public boolean getKeepScreenOn() {
        return (this.mViewFlags & 67108864) != 0;
    }

    public void setKeepScreenOn(boolean z) {
        setFlags(z ? 67108864 : 0, 67108864);
    }

    public int getNextFocusLeftId() {
        return this.mNextFocusLeftId;
    }

    public void setNextFocusLeftId(int i) {
        this.mNextFocusLeftId = i;
    }

    public int getNextFocusRightId() {
        return this.mNextFocusRightId;
    }

    public void setNextFocusRightId(int i) {
        this.mNextFocusRightId = i;
    }

    public int getNextFocusUpId() {
        return this.mNextFocusUpId;
    }

    public void setNextFocusUpId(int i) {
        this.mNextFocusUpId = i;
    }

    public int getNextFocusDownId() {
        return this.mNextFocusDownId;
    }

    public void setNextFocusDownId(int i) {
        this.mNextFocusDownId = i;
    }

    public int getNextFocusForwardId() {
        return this.mNextFocusForwardId;
    }

    public void setNextFocusForwardId(int i) {
        this.mNextFocusForwardId = i;
    }

    public int getNextClusterForwardId() {
        return this.mNextClusterForwardId;
    }

    public void setNextClusterForwardId(int i) {
        this.mNextClusterForwardId = i;
    }

    public boolean isShown() {
        Object obj;
        View view = this;
        while ((view.mViewFlags & 12) == 0 && (obj = view.mParent) != null) {
            if (!(obj instanceof View)) {
                return true;
            }
            view = (View) obj;
            if (view == null) {
                return false;
            }
        }
        return false;
    }

    @Deprecated
    protected boolean fitSystemWindows(Rect rect) {
        if ((this.mPrivateFlags3 & 32) == 0) {
            if (rect == null) {
                return false;
            }
            try {
                this.mPrivateFlags3 |= 64;
                return dispatchApplyWindowInsets(new WindowInsets(rect)).isConsumed();
            } finally {
                this.mPrivateFlags3 &= -65;
            }
        }
        return fitSystemWindowsInt(rect);
    }

    private boolean fitSystemWindowsInt(Rect rect) {
        if ((this.mViewFlags & 2) == 2) {
            this.mUserPaddingStart = Integer.MIN_VALUE;
            this.mUserPaddingEnd = Integer.MIN_VALUE;
            Rect rect2 = sThreadLocal.get();
            if (rect2 == null) {
                rect2 = new Rect();
                sThreadLocal.set(rect2);
            }
            boolean zComputeFitSystemWindows = computeFitSystemWindows(rect, rect2);
            this.mUserPaddingLeftInitial = rect2.left;
            this.mUserPaddingRightInitial = rect2.right;
            internalSetPadding(rect2.left, rect2.top, rect2.right, rect2.bottom);
            return zComputeFitSystemWindows;
        }
        return false;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if ((this.mPrivateFlags3 & 64) == 0) {
            if (fitSystemWindows(windowInsets.getSystemWindowInsets())) {
                return windowInsets.consumeSystemWindowInsets();
            }
        } else if (fitSystemWindowsInt(windowInsets.getSystemWindowInsets())) {
            return windowInsets.consumeSystemWindowInsets();
        }
        return windowInsets;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener onApplyWindowInsetsListener) {
        getListenerInfo().mOnApplyWindowInsetsListener = onApplyWindowInsetsListener;
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets windowInsets) {
        try {
            this.mPrivateFlags3 |= 32;
            if (this.mListenerInfo != null && this.mListenerInfo.mOnApplyWindowInsetsListener != null) {
                return this.mListenerInfo.mOnApplyWindowInsetsListener.onApplyWindowInsets(this, windowInsets);
            }
            return onApplyWindowInsets(windowInsets);
        } finally {
            this.mPrivateFlags3 &= -33;
        }
    }

    public void getLocationInSurface(int[] iArr) {
        getLocationInWindow(iArr);
        if (this.mAttachInfo != null && this.mAttachInfo.mViewRootImpl != null) {
            iArr[0] = iArr[0] + this.mAttachInfo.mViewRootImpl.mWindowAttributes.surfaceInsets.left;
            iArr[1] = iArr[1] + this.mAttachInfo.mViewRootImpl.mWindowAttributes.surfaceInsets.top;
        }
    }

    public WindowInsets getRootWindowInsets() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mViewRootImpl.getWindowInsets(false);
        }
        return null;
    }

    @Deprecated
    protected boolean computeFitSystemWindows(Rect rect, Rect rect2) {
        WindowInsets windowInsetsComputeSystemWindowInsets = computeSystemWindowInsets(new WindowInsets(rect), rect2);
        rect.set(windowInsetsComputeSystemWindowInsets.getSystemWindowInsets());
        return windowInsetsComputeSystemWindowInsets.isSystemWindowInsetsConsumed();
    }

    public WindowInsets computeSystemWindowInsets(WindowInsets windowInsets, Rect rect) {
        if ((this.mViewFlags & 2048) == 0 || this.mAttachInfo == null || ((this.mAttachInfo.mSystemUiVisibility & 1536) == 0 && !this.mAttachInfo.mOverscanRequested)) {
            rect.set(windowInsets.getSystemWindowInsets());
            return windowInsets.consumeSystemWindowInsets().inset(rect);
        }
        rect.set(this.mAttachInfo.mOverscanInsets);
        return windowInsets.inset(rect);
    }

    public void setFitsSystemWindows(boolean z) {
        setFlags(z ? 2 : 0, 2);
    }

    @ViewDebug.ExportedProperty
    public boolean getFitsSystemWindows() {
        return (this.mViewFlags & 2) == 2;
    }

    public boolean fitsSystemWindows() {
        return getFitsSystemWindows();
    }

    @Deprecated
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

    public void getOutsets(Rect rect) {
        if (this.mAttachInfo != null) {
            rect.set(this.mAttachInfo.mOutsets);
        } else {
            rect.setEmpty();
        }
    }

    @ViewDebug.ExportedProperty(mapping = {@ViewDebug.IntToString(from = 0, to = "VISIBLE"), @ViewDebug.IntToString(from = 4, to = "INVISIBLE"), @ViewDebug.IntToString(from = 8, to = "GONE")})
    public int getVisibility() {
        return this.mViewFlags & 12;
    }

    @RemotableViewMethod
    public void setVisibility(int i) {
        setFlags(i, 12);
    }

    @ViewDebug.ExportedProperty
    public boolean isEnabled() {
        return (this.mViewFlags & 32) == 0;
    }

    @RemotableViewMethod
    public void setEnabled(boolean z) {
        if (z == isEnabled()) {
            return;
        }
        setFlags(z ? 0 : 32, 32);
        refreshDrawableState();
        invalidate(true);
        if (!z) {
            cancelPendingInputEvents();
        }
    }

    public void setFocusable(boolean z) {
        setFocusable(z ? 1 : 0);
    }

    public void setFocusable(int i) {
        if ((i & 17) == 0) {
            setFlags(0, 262144);
        }
        setFlags(i, 17);
    }

    public void setFocusableInTouchMode(boolean z) {
        setFlags(z ? 262144 : 0, 262144);
        if (z) {
            setFlags(1, 17);
        }
    }

    public void setAutofillHints(String... strArr) {
        if (strArr == null || strArr.length == 0) {
            this.mAutofillHints = null;
        } else {
            this.mAutofillHints = strArr;
        }
    }

    public void setAutofilled(boolean z) {
        if (z != isAutofilled()) {
            if (z) {
                this.mPrivateFlags3 |= 65536;
            } else {
                this.mPrivateFlags3 &= -65537;
            }
            invalidate();
        }
    }

    public void setSoundEffectsEnabled(boolean z) {
        setFlags(z ? 134217728 : 0, 134217728);
    }

    @ViewDebug.ExportedProperty
    public boolean isSoundEffectsEnabled() {
        return 134217728 == (this.mViewFlags & 134217728);
    }

    public void setHapticFeedbackEnabled(boolean z) {
        setFlags(z ? 268435456 : 0, 268435456);
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
    public void setLayoutDirection(int i) {
        if (getRawLayoutDirection() != i) {
            this.mPrivateFlags2 &= -13;
            resetRtlProperties();
            this.mPrivateFlags2 = ((i << 2) & 12) | this.mPrivateFlags2;
            resolveRtlPropertiesIfNeeded();
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = 0, to = "RESOLVED_DIRECTION_LTR"), @ViewDebug.IntToString(from = 1, to = "RESOLVED_DIRECTION_RTL")})
    public int getLayoutDirection() {
        if (getContext().getApplicationInfo().targetSdkVersion < 17) {
            this.mPrivateFlags2 |= 32;
            return 0;
        }
        if ((this.mPrivateFlags2 & 16) != 16) {
            return 0;
        }
        return 1;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public boolean hasTransientState() {
        return (this.mPrivateFlags2 & Integer.MIN_VALUE) == Integer.MIN_VALUE;
    }

    public void setHasTransientState(boolean z) {
        boolean zHasTransientState = hasTransientState();
        this.mTransientStateCount = z ? this.mTransientStateCount + 1 : this.mTransientStateCount - 1;
        if (this.mTransientStateCount < 0) {
            this.mTransientStateCount = 0;
            Log.e(VIEW_LOG_TAG, "hasTransientState decremented below 0: unmatched pair of setHasTransientState calls");
            return;
        }
        if ((z && this.mTransientStateCount == 1) || (!z && this.mTransientStateCount == 0)) {
            this.mPrivateFlags2 = (this.mPrivateFlags2 & Integer.MAX_VALUE) | (z ? Integer.MIN_VALUE : 0);
            boolean zHasTransientState2 = hasTransientState();
            if (this.mParent != null && zHasTransientState2 != zHasTransientState) {
                try {
                    this.mParent.childHasTransientStateChanged(this, zHasTransientState2);
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

    boolean isLayoutValid() {
        return isLaidOut() && (this.mPrivateFlags & 4096) == 0;
    }

    public void setWillNotDraw(boolean z) {
        setFlags(z ? 128 : 0, 128);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean willNotDraw() {
        return (this.mViewFlags & 128) == 128;
    }

    @Deprecated
    public void setWillNotCacheDrawing(boolean z) {
        setFlags(z ? 131072 : 0, 131072);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    @Deprecated
    public boolean willNotCacheDrawing() {
        return (this.mViewFlags & 131072) == 131072;
    }

    @ViewDebug.ExportedProperty
    public boolean isClickable() {
        return (this.mViewFlags & 16384) == 16384;
    }

    public void setClickable(boolean z) {
        setFlags(z ? 16384 : 0, 16384);
    }

    public boolean isLongClickable() {
        return (this.mViewFlags & 2097152) == 2097152;
    }

    public void setLongClickable(boolean z) {
        setFlags(z ? 2097152 : 0, 2097152);
    }

    public boolean isContextClickable() {
        return (this.mViewFlags & 8388608) == 8388608;
    }

    public void setContextClickable(boolean z) {
        setFlags(z ? 8388608 : 0, 8388608);
    }

    private void setPressed(boolean z, float f, float f2) {
        if (z) {
            drawableHotspotChanged(f, f2);
        }
        setPressed(z);
    }

    public void setPressed(boolean z) {
        boolean z2 = z != ((this.mPrivateFlags & 16384) == 16384);
        if (z) {
            this.mPrivateFlags |= 16384;
        } else {
            this.mPrivateFlags &= -16385;
        }
        if (z2) {
            refreshDrawableState();
        }
        dispatchSetPressed(z);
    }

    protected void dispatchSetPressed(boolean z) {
    }

    @ViewDebug.ExportedProperty
    public boolean isPressed() {
        return (this.mPrivateFlags & 16384) == 16384;
    }

    public boolean isAssistBlocked() {
        return (this.mPrivateFlags3 & 16384) != 0;
    }

    public void setAssistBlocked(boolean z) {
        if (z) {
            this.mPrivateFlags3 |= 16384;
        } else {
            this.mPrivateFlags3 &= -16385;
        }
    }

    public boolean isSaveEnabled() {
        return (this.mViewFlags & 65536) != 65536;
    }

    public void setSaveEnabled(boolean z) {
        setFlags(z ? 0 : 65536, 65536);
    }

    @ViewDebug.ExportedProperty
    public boolean getFilterTouchesWhenObscured() {
        return (this.mViewFlags & 1024) != 0;
    }

    public void setFilterTouchesWhenObscured(boolean z) {
        setFlags(z ? 1024 : 0, 1024);
    }

    public boolean isSaveFromParentEnabled() {
        return (this.mViewFlags & 536870912) != 536870912;
    }

    public void setSaveFromParentEnabled(boolean z) {
        setFlags(z ? 0 : 536870912, 536870912);
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean isFocusable() {
        return 1 == (this.mViewFlags & 1);
    }

    @ViewDebug.ExportedProperty(category = "focus", mapping = {@ViewDebug.IntToString(from = 0, to = "NOT_FOCUSABLE"), @ViewDebug.IntToString(from = 1, to = "FOCUSABLE"), @ViewDebug.IntToString(from = 16, to = "FOCUSABLE_AUTO")})
    public int getFocusable() {
        if ((this.mViewFlags & 16) > 0) {
            return 16;
        }
        return this.mViewFlags & 1;
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean isFocusableInTouchMode() {
        return 262144 == (this.mViewFlags & 262144);
    }

    public boolean isScreenReaderFocusable() {
        return (this.mPrivateFlags3 & 268435456) != 0;
    }

    public void setScreenReaderFocusable(boolean z) {
        updatePflags3AndNotifyA11yIfChanged(268435456, z);
    }

    public boolean isAccessibilityHeading() {
        return (this.mPrivateFlags3 & Integer.MIN_VALUE) != 0;
    }

    public void setAccessibilityHeading(boolean z) {
        updatePflags3AndNotifyA11yIfChanged(Integer.MIN_VALUE, z);
    }

    private void updatePflags3AndNotifyA11yIfChanged(int i, boolean z) {
        int i2;
        int i3 = this.mPrivateFlags3;
        if (z) {
            i2 = i | i3;
        } else {
            i2 = (~i) & i3;
        }
        if (i2 != this.mPrivateFlags3) {
            this.mPrivateFlags3 = i2;
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    public View focusSearch(int i) {
        if (this.mParent != null) {
            return this.mParent.focusSearch(this, i);
        }
        return null;
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean isKeyboardNavigationCluster() {
        return (this.mPrivateFlags3 & 32768) != 0;
    }

    View findKeyboardNavigationCluster() {
        if (this.mParent instanceof View) {
            View viewFindKeyboardNavigationCluster = ((View) this.mParent).findKeyboardNavigationCluster();
            if (viewFindKeyboardNavigationCluster != null) {
                return viewFindKeyboardNavigationCluster;
            }
            if (isKeyboardNavigationCluster()) {
                return this;
            }
            return null;
        }
        return null;
    }

    public void setKeyboardNavigationCluster(boolean z) {
        if (z) {
            this.mPrivateFlags3 |= 32768;
        } else {
            this.mPrivateFlags3 &= -32769;
        }
    }

    public final void setFocusedInCluster() {
        setFocusedInCluster(findKeyboardNavigationCluster());
    }

    private void setFocusedInCluster(View view) {
        if (this instanceof ViewGroup) {
            ((ViewGroup) this).mFocusedInCluster = null;
        }
        if (view == this) {
            return;
        }
        View view2 = this;
        for (ViewParent parent = this.mParent; parent instanceof ViewGroup; parent = parent.getParent()) {
            ((ViewGroup) parent).mFocusedInCluster = view2;
            if (parent != view) {
                view2 = (View) parent;
            } else {
                return;
            }
        }
    }

    private void updateFocusedInCluster(View view, int i) {
        View viewFindKeyboardNavigationCluster;
        if (view != null && (viewFindKeyboardNavigationCluster = view.findKeyboardNavigationCluster()) != findKeyboardNavigationCluster()) {
            view.setFocusedInCluster(viewFindKeyboardNavigationCluster);
            if (!(view.mParent instanceof ViewGroup)) {
                return;
            }
            if (i == 2 || i == 1) {
                ((ViewGroup) view.mParent).clearFocusedInCluster(view);
            } else if ((view instanceof ViewGroup) && ((ViewGroup) view).getDescendantFocusability() == 262144 && ViewRootImpl.isViewDescendantOf(this, view)) {
                ((ViewGroup) view.mParent).clearFocusedInCluster(view);
            }
        }
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean isFocusedByDefault() {
        return (this.mPrivateFlags3 & 262144) != 0;
    }

    public void setFocusedByDefault(boolean z) {
        if (z == ((this.mPrivateFlags3 & 262144) != 0)) {
            return;
        }
        if (z) {
            this.mPrivateFlags3 |= 262144;
        } else {
            this.mPrivateFlags3 &= -262145;
        }
        if (this.mParent instanceof ViewGroup) {
            if (z) {
                ((ViewGroup) this.mParent).setDefaultFocus(this);
            } else {
                ((ViewGroup) this.mParent).clearDefaultFocus(this);
            }
        }
    }

    boolean hasDefaultFocus() {
        return isFocusedByDefault();
    }

    public View keyboardNavigationClusterSearch(View view, int i) {
        if (isKeyboardNavigationCluster()) {
            view = this;
        }
        if (isRootNamespace()) {
            return FocusFinder.getInstance().findNextKeyboardNavigationCluster(this, view, i);
        }
        if (this.mParent != null) {
            return this.mParent.keyboardNavigationClusterSearch(view, i);
        }
        return null;
    }

    public boolean dispatchUnhandledMove(View view, int i) {
        return false;
    }

    public void setDefaultFocusHighlightEnabled(boolean z) {
        this.mDefaultFocusHighlightEnabled = z;
    }

    @ViewDebug.ExportedProperty(category = "focus")
    public final boolean getDefaultFocusHighlightEnabled() {
        return this.mDefaultFocusHighlightEnabled;
    }

    View findUserSetNextFocus(View view, int i) {
        if (i == 17) {
            if (this.mNextFocusLeftId == -1) {
                return null;
            }
            return findViewInsideOutShouldExist(view, this.mNextFocusLeftId);
        }
        if (i == 33) {
            if (this.mNextFocusUpId == -1) {
                return null;
            }
            return findViewInsideOutShouldExist(view, this.mNextFocusUpId);
        }
        if (i == 66) {
            if (this.mNextFocusRightId == -1) {
                return null;
            }
            return findViewInsideOutShouldExist(view, this.mNextFocusRightId);
        }
        if (i == 130) {
            if (this.mNextFocusDownId == -1) {
                return null;
            }
            return findViewInsideOutShouldExist(view, this.mNextFocusDownId);
        }
        switch (i) {
            case 1:
                if (this.mID != -1) {
                    final int i2 = this.mID;
                    break;
                }
                break;
            case 2:
                if (this.mNextFocusForwardId != -1) {
                    break;
                }
                break;
        }
        return null;
    }

    View findUserSetNextKeyboardNavigationCluster(View view, int i) {
        switch (i) {
            case 1:
                if (this.mID != -1) {
                    final int i2 = this.mID;
                    break;
                }
                break;
            case 2:
                if (this.mNextClusterForwardId != -1) {
                    break;
                }
                break;
        }
        return null;
    }

    static boolean lambda$findUserSetNextKeyboardNavigationCluster$0(int i, View view) {
        return view.mNextClusterForwardId == i;
    }

    private View findViewInsideOutShouldExist(View view, int i) {
        if (this.mMatchIdPredicate == null) {
            this.mMatchIdPredicate = new MatchIdPredicate();
        }
        this.mMatchIdPredicate.mId = i;
        View viewFindViewByPredicateInsideOut = view.findViewByPredicateInsideOut(this, this.mMatchIdPredicate);
        if (viewFindViewByPredicateInsideOut == null) {
            Log.w(VIEW_LOG_TAG, "couldn't find view with id " + i);
        }
        return viewFindViewByPredicateInsideOut;
    }

    public ArrayList<View> getFocusables(int i) {
        ArrayList<View> arrayList = new ArrayList<>(24);
        addFocusables(arrayList, i);
        return arrayList;
    }

    public void addFocusables(ArrayList<View> arrayList, int i) {
        addFocusables(arrayList, i, isInTouchMode() ? 1 : 0);
    }

    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        if (arrayList == null || !canTakeFocus()) {
            return;
        }
        if ((i2 & 1) == 1 && !isFocusableInTouchMode()) {
            return;
        }
        arrayList.add(this);
    }

    public void addKeyboardNavigationClusters(Collection<View> collection, int i) {
        if (!isKeyboardNavigationCluster() || !hasFocusable()) {
            return;
        }
        collection.add(this);
    }

    public void findViewsWithText(ArrayList<View> arrayList, CharSequence charSequence, int i) {
        if (getAccessibilityNodeProvider() != null) {
            if ((i & 4) != 0) {
                arrayList.add(this);
            }
        } else if ((i & 2) != 0 && charSequence != null && charSequence.length() > 0 && this.mContentDescription != null && this.mContentDescription.length() > 0) {
            if (this.mContentDescription.toString().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                arrayList.add(this);
            }
        }
    }

    public ArrayList<View> getTouchables() {
        ArrayList<View> arrayList = new ArrayList<>();
        addTouchables(arrayList);
        return arrayList;
    }

    public void addTouchables(ArrayList<View> arrayList) {
        int i = this.mViewFlags;
        if (((i & 16384) == 16384 || (i & 2097152) == 2097152 || (i & 8388608) == 8388608) && (i & 32) == 0) {
            arrayList.add(this);
        }
    }

    public boolean isAccessibilityFocused() {
        return (this.mPrivateFlags2 & 67108864) != 0;
    }

    public boolean requestAccessibilityFocus() {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
        if (!accessibilityManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled() || (this.mViewFlags & 12) != 0 || (this.mPrivateFlags2 & 67108864) != 0) {
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
        View accessibilityFocusedHost;
        clearAccessibilityFocusNoCallbacks(0);
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null && (accessibilityFocusedHost = viewRootImpl.getAccessibilityFocusedHost()) != null && ViewRootImpl.isViewDescendantOf(accessibilityFocusedHost, this)) {
            viewRootImpl.setAccessibilityFocus(null, null);
        }
    }

    private void sendAccessibilityHoverEvent(int i) {
        View view = this;
        while (!view.includeForAccessibility()) {
            Object parent = view.getParent();
            if (parent instanceof View) {
                view = (View) parent;
            } else {
                return;
            }
        }
        view.sendAccessibilityEvent(i);
    }

    void clearAccessibilityFocusNoCallbacks(int i) {
        if ((this.mPrivateFlags2 & 67108864) != 0) {
            this.mPrivateFlags2 &= -67108865;
            invalidate();
            if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(65536);
                accessibilityEventObtain.setAction(i);
                if (this.mAccessibilityDelegate != null) {
                    this.mAccessibilityDelegate.sendAccessibilityEventUnchecked(this, accessibilityEventObtain);
                } else {
                    sendAccessibilityEventUnchecked(accessibilityEventObtain);
                }
            }
        }
    }

    public final boolean requestFocus() {
        return requestFocus(130);
    }

    public boolean restoreFocusInCluster(int i) {
        if (restoreDefaultFocus()) {
            return true;
        }
        return requestFocus(i);
    }

    public boolean restoreFocusNotInCluster() {
        return requestFocus(130);
    }

    public boolean restoreDefaultFocus() {
        return requestFocus(130);
    }

    public final boolean requestFocus(int i) {
        return requestFocus(i, null);
    }

    public boolean requestFocus(int i, Rect rect) {
        return requestFocusNoSearch(i, rect);
    }

    private boolean requestFocusNoSearch(int i, Rect rect) {
        if (!canTakeFocus()) {
            return false;
        }
        if ((isInTouchMode() && 262144 != (this.mViewFlags & 262144)) || hasAncestorThatBlocksDescendantFocus()) {
            return false;
        }
        if (!isLayoutValid()) {
            this.mPrivateFlags |= 1;
        } else {
            clearParentsWantFocus();
        }
        handleFocusGainInternal(i, rect);
        return true;
    }

    void clearParentsWantFocus() {
        if (this.mParent instanceof View) {
            ((View) this.mParent).mPrivateFlags &= -2;
            ((View) this.mParent).clearParentsWantFocus();
        }
    }

    public final boolean requestFocusFromTouch() {
        ViewRootImpl viewRootImpl;
        if (isInTouchMode() && (viewRootImpl = getViewRootImpl()) != null) {
            viewRootImpl.ensureTouchMode(false);
        }
        return requestFocus(130);
    }

    private boolean hasAncestorThatBlocksDescendantFocus() {
        boolean zIsFocusableInTouchMode = isFocusableInTouchMode();
        ViewParent parent = this.mParent;
        while (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            if (viewGroup.getDescendantFocusability() != 393216) {
                if (!zIsFocusableInTouchMode && viewGroup.shouldBlockFocusForTouchscreen()) {
                    return true;
                }
                parent = viewGroup.getParent();
            } else {
                return true;
            }
        }
        return false;
    }

    @ViewDebug.ExportedProperty(category = Context.ACCESSIBILITY_SERVICE, mapping = {@ViewDebug.IntToString(from = 0, to = "auto"), @ViewDebug.IntToString(from = 1, to = "yes"), @ViewDebug.IntToString(from = 2, to = "no"), @ViewDebug.IntToString(from = 4, to = "noHideDescendants")})
    public int getImportantForAccessibility() {
        return (this.mPrivateFlags2 & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK) >> 20;
    }

    public void setAccessibilityLiveRegion(int i) {
        if (i != getAccessibilityLiveRegion()) {
            this.mPrivateFlags2 &= -25165825;
            this.mPrivateFlags2 = ((i << 23) & 25165824) | this.mPrivateFlags2;
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
    }

    public int getAccessibilityLiveRegion() {
        return (this.mPrivateFlags2 & 25165824) >> 23;
    }

    public void setImportantForAccessibility(int i) {
        View viewFindAccessibilityFocusHost;
        int importantForAccessibility = getImportantForAccessibility();
        if (i != importantForAccessibility) {
            boolean z = i == 4;
            if ((i == 2 || z) && (viewFindAccessibilityFocusHost = findAccessibilityFocusHost(z)) != null) {
                viewFindAccessibilityFocusHost.clearAccessibilityFocus();
            }
            boolean z2 = importantForAccessibility == 0 || i == 0;
            boolean z3 = z2 && includeForAccessibility();
            this.mPrivateFlags2 &= -7340033;
            this.mPrivateFlags2 = ((i << 20) & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK) | this.mPrivateFlags2;
            if (!z2 || z3 != includeForAccessibility()) {
                notifySubtreeAccessibilityStateChangedIfNeeded();
            } else {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    private View findAccessibilityFocusHost(boolean z) {
        ViewRootImpl viewRootImpl;
        View accessibilityFocusedHost;
        if (isAccessibilityFocusedViewOrHost()) {
            return this;
        }
        if (z && (viewRootImpl = getViewRootImpl()) != null && (accessibilityFocusedHost = viewRootImpl.getAccessibilityFocusedHost()) != null && ViewRootImpl.isViewDescendantOf(accessibilityFocusedHost, this)) {
            return accessibilityFocusedHost;
        }
        return null;
    }

    public boolean isImportantForAccessibility() {
        int i = (this.mPrivateFlags2 & PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK) >> 20;
        if (i == 2 || i == 4) {
            return false;
        }
        for (ViewParent parent = this.mParent; parent instanceof View; parent = parent.getParent()) {
            if (((View) parent).getImportantForAccessibility() == 4) {
                return false;
            }
        }
        if (i != 1 && !isActionableForAccessibility() && !hasListenersForAccessibility() && getAccessibilityNodeProvider() == null && getAccessibilityLiveRegion() == 0 && !isAccessibilityPane()) {
            return false;
        }
        return true;
    }

    public ViewParent getParentForAccessibility() {
        if (this.mParent instanceof View) {
            if (((View) this.mParent).includeForAccessibility()) {
                return this.mParent;
            }
            return this.mParent.getParentForAccessibility();
        }
        return null;
    }

    View getSelfOrParentImportantForA11y() {
        if (isImportantForAccessibility()) {
            return this;
        }
        Object parentForAccessibility = getParentForAccessibility();
        if (parentForAccessibility instanceof View) {
            return (View) parentForAccessibility;
        }
        return null;
    }

    public void addChildrenForAccessibility(ArrayList<View> arrayList) {
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
        ListenerInfo listenerInfo = getListenerInfo();
        return (this.mTouchDelegate == null && listenerInfo.mOnKeyListener == null && listenerInfo.mOnTouchListener == null && listenerInfo.mOnGenericMotionListener == null && listenerInfo.mOnHoverListener == null && listenerInfo.mOnDragListener == null) ? false : true;
    }

    public void notifyViewAccessibilityStateChangedIfNeeded(int i) {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled() || this.mAttachInfo == null) {
            return;
        }
        if (i != 1 && isAccessibilityPane() && (getVisibility() == 0 || i == 32)) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
            accessibilityEventObtain.setEventType(32);
            accessibilityEventObtain.setContentChangeTypes(i);
            accessibilityEventObtain.setSource(this);
            onPopulateAccessibilityEvent(accessibilityEventObtain);
            if (this.mParent != null) {
                try {
                    this.mParent.requestSendAccessibilityEvent(this, accessibilityEventObtain);
                    return;
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    return;
                }
            }
            return;
        }
        if (getAccessibilityLiveRegion() != 0) {
            AccessibilityEvent accessibilityEventObtain2 = AccessibilityEvent.obtain();
            accessibilityEventObtain2.setEventType(2048);
            accessibilityEventObtain2.setContentChangeTypes(i);
            sendAccessibilityEventUnchecked(accessibilityEventObtain2);
            return;
        }
        if (this.mParent != null) {
            try {
                this.mParent.notifySubtreeAccessibilityStateChanged(this, this, i);
            } catch (AbstractMethodError e2) {
                Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e2);
            }
        }
    }

    public void notifySubtreeAccessibilityStateChangedIfNeeded() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled() && this.mAttachInfo != null && (this.mPrivateFlags2 & 134217728) == 0) {
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

    public void setTransitionVisibility(int i) {
        this.mViewFlags = i | (this.mViewFlags & (-13));
    }

    void resetSubtreeAccessibilityStateChanged() {
        this.mPrivateFlags2 &= -134217729;
    }

    public boolean dispatchNestedPrePerformAccessibilityAction(int i, Bundle bundle) {
        for (ViewParent parent = getParent(); parent != null; parent = parent.getParent()) {
            if (parent.onNestedPrePerformAccessibilityAction(this, i, bundle)) {
                return true;
            }
        }
        return false;
    }

    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (this.mAccessibilityDelegate != null) {
            return this.mAccessibilityDelegate.performAccessibilityAction(this, i, bundle);
        }
        return performAccessibilityActionInternal(i, bundle);
    }

    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        int i2;
        if (isNestedScrollingEnabled() && ((i == 8192 || i == 4096 || i == 16908344 || i == 16908345 || i == 16908346 || i == 16908347) && dispatchNestedPrePerformAccessibilityAction(i, bundle))) {
            return true;
        }
        switch (i) {
            case 1:
                if (!hasFocus()) {
                    getViewRootImpl().ensureTouchMode(false);
                    return requestFocus();
                }
                return false;
            case 2:
                if (hasFocus()) {
                    clearFocus();
                    return !isFocused();
                }
                return false;
            case 4:
                if (!isSelected()) {
                    setSelected(true);
                    return isSelected();
                }
                return false;
            case 8:
                if (isSelected()) {
                    setSelected(false);
                    return !isSelected();
                }
                return false;
            case 16:
                if (isClickable()) {
                    performClickInternal();
                    return true;
                }
                return false;
            case 32:
                if (isLongClickable()) {
                    performLongClick();
                    return true;
                }
                return false;
            case 64:
                if (!isAccessibilityFocused()) {
                    return requestAccessibilityFocus();
                }
                return false;
            case 128:
                if (isAccessibilityFocused()) {
                    clearAccessibilityFocus();
                    return true;
                }
                return false;
            case 256:
                if (bundle != null) {
                    return traverseAtGranularity(bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT), true, bundle.getBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN));
                }
                return false;
            case 512:
                if (bundle != null) {
                    return traverseAtGranularity(bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT), false, bundle.getBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN));
                }
                return false;
            case 131072:
                if (getIterableTextForAccessibility() == null) {
                    return false;
                }
                if (bundle != null) {
                    i2 = bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1);
                } else {
                    i2 = -1;
                }
                int i3 = bundle != null ? bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1) : -1;
                if ((getAccessibilitySelectionStart() != i2 || getAccessibilitySelectionEnd() != i3) && i2 == i3) {
                    setAccessibilitySelection(i2, i3);
                    notifyViewAccessibilityStateChangedIfNeeded(0);
                    return true;
                }
                return false;
            case 16908342:
                if (this.mAttachInfo != null) {
                    Rect rect = this.mAttachInfo.mTmpInvalRect;
                    getDrawingRect(rect);
                    return requestRectangleOnScreen(rect, true);
                }
                return false;
            case 16908348:
                if (isContextClickable()) {
                    performContextClick();
                    return true;
                }
                return false;
            case 16908356:
                if (this.mTooltipInfo != null && this.mTooltipInfo.mTooltipPopup != null) {
                    return false;
                }
                return showLongClickTooltip(0, 0);
            case 16908357:
                if (this.mTooltipInfo == null || this.mTooltipInfo.mTooltipPopup == null) {
                    return false;
                }
                hideTooltip();
                return true;
            default:
                return false;
        }
    }

    private boolean traverseAtGranularity(int i, boolean z, boolean z2) {
        AccessibilityIterators.TextSegmentIterator iteratorForGranularity;
        int accessibilitySelectionStart;
        int i2;
        CharSequence iterableTextForAccessibility = getIterableTextForAccessibility();
        if (iterableTextForAccessibility == null || iterableTextForAccessibility.length() == 0 || (iteratorForGranularity = getIteratorForGranularity(i)) == null) {
            return false;
        }
        int accessibilitySelectionEnd = getAccessibilitySelectionEnd();
        if (accessibilitySelectionEnd == -1) {
            if (!z) {
                accessibilitySelectionEnd = iterableTextForAccessibility.length();
            } else {
                accessibilitySelectionEnd = 0;
            }
        }
        int[] iArrFollowing = z ? iteratorForGranularity.following(accessibilitySelectionEnd) : iteratorForGranularity.preceding(accessibilitySelectionEnd);
        if (iArrFollowing == null) {
            return false;
        }
        int i3 = iArrFollowing[0];
        int i4 = iArrFollowing[1];
        if (z2 && isAccessibilitySelectionExtendable()) {
            accessibilitySelectionStart = getAccessibilitySelectionStart();
            if (accessibilitySelectionStart == -1) {
                accessibilitySelectionStart = z ? i3 : i4;
            }
            i2 = z ? i4 : i3;
        } else {
            accessibilitySelectionStart = z ? i4 : i3;
            i2 = accessibilitySelectionStart;
        }
        setAccessibilitySelection(accessibilitySelectionStart, i2);
        sendViewTextTraversedAtGranularityEvent(z ? 256 : 512, i, i3, i4);
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

    public void setAccessibilitySelection(int i, int i2) {
        if (i == i2 && i2 == this.mAccessibilityCursorPosition) {
            return;
        }
        if (i >= 0 && i == i2 && i2 <= getIterableTextForAccessibility().length()) {
            this.mAccessibilityCursorPosition = i;
        } else {
            this.mAccessibilityCursorPosition = -1;
        }
        sendAccessibilityEvent(8192);
    }

    private void sendViewTextTraversedAtGranularityEvent(int i, int i2, int i3, int i4) {
        if (this.mParent == null) {
            return;
        }
        AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(131072);
        onInitializeAccessibilityEvent(accessibilityEventObtain);
        onPopulateAccessibilityEvent(accessibilityEventObtain);
        accessibilityEventObtain.setFromIndex(i3);
        accessibilityEventObtain.setToIndex(i4);
        accessibilityEventObtain.setAction(i);
        accessibilityEventObtain.setMovementGranularity(i2);
        this.mParent.requestSendAccessibilityEvent(this, accessibilityEventObtain);
    }

    public AccessibilityIterators.TextSegmentIterator getIteratorForGranularity(int i) {
        if (i != 8) {
            switch (i) {
                case 1:
                    CharSequence iterableTextForAccessibility = getIterableTextForAccessibility();
                    if (iterableTextForAccessibility != null && iterableTextForAccessibility.length() > 0) {
                        AccessibilityIterators.CharacterTextSegmentIterator characterTextSegmentIterator = AccessibilityIterators.CharacterTextSegmentIterator.getInstance(this.mContext.getResources().getConfiguration().locale);
                        characterTextSegmentIterator.initialize(iterableTextForAccessibility.toString());
                        return characterTextSegmentIterator;
                    }
                    return null;
                case 2:
                    CharSequence iterableTextForAccessibility2 = getIterableTextForAccessibility();
                    if (iterableTextForAccessibility2 != null && iterableTextForAccessibility2.length() > 0) {
                        AccessibilityIterators.WordTextSegmentIterator wordTextSegmentIterator = AccessibilityIterators.WordTextSegmentIterator.getInstance(this.mContext.getResources().getConfiguration().locale);
                        wordTextSegmentIterator.initialize(iterableTextForAccessibility2.toString());
                        return wordTextSegmentIterator;
                    }
                    return null;
                default:
                    return null;
            }
        }
        CharSequence iterableTextForAccessibility3 = getIterableTextForAccessibility();
        if (iterableTextForAccessibility3 != null && iterableTextForAccessibility3.length() > 0) {
            AccessibilityIterators.ParagraphTextSegmentIterator paragraphTextSegmentIterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance();
            paragraphTextSegmentIterator.initialize(iterableTextForAccessibility3.toString());
            return paragraphTextSegmentIterator;
        }
        return null;
    }

    public final boolean isTemporarilyDetached() {
        return (this.mPrivateFlags3 & 33554432) != 0;
    }

    public void dispatchStartTemporaryDetach() {
        this.mPrivateFlags3 |= 33554432;
        notifyEnterOrExitForAutoFillIfNeeded(false);
        onStartTemporaryDetach();
    }

    public void onStartTemporaryDetach() {
        removeUnsetPressCallback();
        this.mPrivateFlags |= 67108864;
    }

    public void dispatchFinishTemporaryDetach() {
        this.mPrivateFlags3 &= -33554433;
        onFinishTemporaryDetach();
        if (hasWindowFocus() && hasFocus()) {
            InputMethodManager.getInstance().focusIn(this);
        }
        notifyEnterOrExitForAutoFillIfNeeded(true);
    }

    public void onFinishTemporaryDetach() {
    }

    public KeyEvent.DispatcherState getKeyDispatcherState() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mKeyDispatchState;
        }
        return null;
    }

    public boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        return onKeyPreIme(keyEvent.getKeyCode(), keyEvent);
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onKeyEvent(keyEvent, 0);
        }
        if (ViewDebugManager.DEBUG_KEY || ViewDebugManager.DEBUG_ENG) {
            ViewDebugManager.getInstance().debugKeyDispatch(this, keyEvent);
        }
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnKeyListener != null && (this.mViewFlags & 32) == 0 && listenerInfo.mOnKeyListener.onKey(this, keyEvent.getKeyCode(), keyEvent)) {
            if (ViewDebugManager.DEBUG_KEY) {
                ViewDebugManager.getInstance().debugEventHandled(this, keyEvent, listenerInfo.toString());
            }
            return true;
        }
        if (keyEvent.dispatch(this, this.mAttachInfo != null ? this.mAttachInfo.mKeyDispatchState : null, this)) {
            if (ViewDebugManager.DEBUG_KEY) {
                ViewDebugManager.getInstance().debugEventHandled(this, keyEvent, "onKeyXXX");
            }
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(keyEvent, 0);
        }
        return false;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return onKeyShortcut(keyEvent.getKeyCode(), keyEvent);
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (motionEvent.isTargetAccessibilityFocus()) {
            if (!isAccessibilityFocusedViewOrHost()) {
                return false;
            }
            motionEvent.setTargetAccessibilityFocus(false);
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(motionEvent, 0);
        }
        if (ViewDebugManager.DEBUG_MOTION || ViewDebugManager.DEBUG_ENG) {
            ViewDebugManager.getInstance().debugTouchDispatched(this, motionEvent);
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            stopNestedScroll();
        }
        if (onFilterTouchEventForSecurity(motionEvent)) {
            z = (this.mViewFlags & 32) == 0 && handleScrollBarDragging(motionEvent);
            ListenerInfo listenerInfo = this.mListenerInfo;
            if (listenerInfo != null && listenerInfo.mOnTouchListener != null && (this.mViewFlags & 32) == 0 && listenerInfo.mOnTouchListener.onTouch(this, motionEvent)) {
                if (ViewDebugManager.DEBUG_TOUCH) {
                    ViewDebugManager.getInstance().debugEventHandled(this, motionEvent, listenerInfo.toString());
                }
                z = true;
            }
            if (!z && onTouchEvent(motionEvent)) {
                if (ViewDebugManager.DEBUG_TOUCH) {
                    ViewDebugManager.getInstance().debugEventHandled(this, motionEvent, "onTouchEvent");
                }
                z = true;
            }
        } else {
            z = false;
        }
        if (!z && this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 0);
        }
        if (actionMasked == 1 || actionMasked == 3 || (actionMasked == 0 && !z)) {
            stopNestedScroll();
        }
        return z;
    }

    boolean isAccessibilityFocusedViewOrHost() {
        return isAccessibilityFocused() || (getViewRootImpl() != null && getViewRootImpl().getAccessibilityFocusedHost() == this);
    }

    public boolean onFilterTouchEventForSecurity(MotionEvent motionEvent) {
        return (this.mViewFlags & 1024) == 0 || (motionEvent.getFlags() & 1) == 0;
    }

    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTrackballEvent(motionEvent, 0);
        }
        return onTrackballEvent(motionEvent);
    }

    public boolean dispatchCapturedPointerEvent(MotionEvent motionEvent) {
        if (!hasPointerCapture()) {
            return false;
        }
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnCapturedPointerListener != null && listenerInfo.mOnCapturedPointerListener.onCapturedPointer(this, motionEvent)) {
            return true;
        }
        return onCapturedPointerEvent(motionEvent);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onGenericMotionEvent(motionEvent, 0);
        }
        if ((motionEvent.getSource() & 2) != 0) {
            int action = motionEvent.getAction();
            if (action == 9 || action == 7 || action == 10) {
                if (dispatchHoverEvent(motionEvent)) {
                    return true;
                }
            } else if (dispatchGenericPointerEvent(motionEvent)) {
                return true;
            }
        } else if (dispatchGenericFocusedEvent(motionEvent)) {
            return true;
        }
        if (dispatchGenericMotionEventInternal(motionEvent)) {
            return true;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 0);
        }
        return false;
    }

    private boolean dispatchGenericMotionEventInternal(MotionEvent motionEvent) {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if ((listenerInfo != null && listenerInfo.mOnGenericMotionListener != null && (this.mViewFlags & 32) == 0 && listenerInfo.mOnGenericMotionListener.onGenericMotion(this, motionEvent)) || onGenericMotionEvent(motionEvent)) {
            return true;
        }
        int actionButton = motionEvent.getActionButton();
        switch (motionEvent.getActionMasked()) {
            case 11:
                if (isContextClickable() && !this.mInContextButtonPress && !this.mHasPerformedLongPress && ((actionButton == 32 || actionButton == 2) && performContextClick(motionEvent.getX(), motionEvent.getY()))) {
                    this.mInContextButtonPress = true;
                    setPressed(true, motionEvent.getX(), motionEvent.getY());
                    removeTapCallback();
                    removeLongPressCallback();
                    return true;
                }
                break;
            case 12:
                if (this.mInContextButtonPress && (actionButton == 32 || actionButton == 2)) {
                    this.mInContextButtonPress = false;
                    this.mIgnoreNextUpEvent = true;
                }
                break;
        }
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(motionEvent, 0);
        }
        return false;
    }

    protected boolean dispatchHoverEvent(MotionEvent motionEvent) {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnHoverListener != null && (this.mViewFlags & 32) == 0 && listenerInfo.mOnHoverListener.onHover(this, motionEvent)) {
            return true;
        }
        return onHoverEvent(motionEvent);
    }

    protected boolean hasHoveredChild() {
        return false;
    }

    protected boolean dispatchGenericPointerEvent(MotionEvent motionEvent) {
        return false;
    }

    protected boolean dispatchGenericFocusedEvent(MotionEvent motionEvent) {
        return false;
    }

    public final boolean dispatchPointerEvent(MotionEvent motionEvent) {
        if (motionEvent.isTouchEvent()) {
            return dispatchTouchEvent(motionEvent);
        }
        return dispatchGenericMotionEvent(motionEvent);
    }

    public void dispatchWindowFocusChanged(boolean z) {
        onWindowFocusChanged(z);
    }

    public void onWindowFocusChanged(boolean z) {
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (ViewDebugManager.DEBUG_FOCUS) {
            Log.d(VIEW_LOG_TAG, "onWindowFocusChanged: hasWindowFocus = " + z + ",imm = " + inputMethodManagerPeekInstance + ",this = " + this);
        }
        if (!z) {
            if (isPressed()) {
                setPressed(false);
            }
            this.mPrivateFlags3 &= -131073;
            if (inputMethodManagerPeekInstance != null && (this.mPrivateFlags & 2) != 0) {
                inputMethodManagerPeekInstance.focusOut(this);
            }
            removeLongPressCallback();
            removeTapCallback();
            onFocusLost();
        } else if (inputMethodManagerPeekInstance != null && (this.mPrivateFlags & 2) != 0) {
            inputMethodManagerPeekInstance.focusIn(this);
        }
        refreshDrawableState();
    }

    public boolean hasWindowFocus() {
        return this.mAttachInfo != null && this.mAttachInfo.mHasWindowFocus;
    }

    protected void dispatchVisibilityChanged(View view, int i) {
        onVisibilityChanged(view, i);
    }

    protected void onVisibilityChanged(View view, int i) {
    }

    public void dispatchDisplayHint(int i) {
        onDisplayHint(i);
    }

    protected void onDisplayHint(int i) {
    }

    public void dispatchWindowVisibilityChanged(int i) {
        onWindowVisibilityChanged(i);
    }

    protected void onWindowVisibilityChanged(int i) {
        if (i == 0) {
            initialAwakenScrollBars();
        }
    }

    boolean dispatchVisibilityAggregated(boolean z) {
        boolean z2 = getVisibility() == 0;
        if (z2 || !z) {
            onVisibilityAggregated(z);
        }
        return z2 && z;
    }

    public void onVisibilityAggregated(boolean z) {
        Drawable drawable;
        int i;
        AutofillManager autofillManager;
        boolean z2 = (this.mPrivateFlags3 & 536870912) != 0;
        this.mPrivateFlags3 = z ? 536870912 | this.mPrivateFlags3 : this.mPrivateFlags3 & (-536870913);
        if (z && this.mAttachInfo != null) {
            initialAwakenScrollBars();
        }
        Drawable drawable2 = this.mBackground;
        if (drawable2 != null && z != drawable2.isVisible()) {
            drawable2.setVisible(z, false);
        }
        Drawable drawable3 = this.mDefaultFocusHighlight;
        if (drawable3 != null && z != drawable3.isVisible()) {
            drawable3.setVisible(z, false);
        }
        if (this.mForegroundInfo != null) {
            drawable = this.mForegroundInfo.mDrawable;
        } else {
            drawable = null;
        }
        if (drawable != null && z != drawable.isVisible()) {
            drawable.setVisible(z, false);
        }
        if (isAutofillable() && (autofillManager = getAutofillManager()) != null && getAutofillViewId() > 1073741823) {
            if (this.mVisibilityChangeForAutofillHandler != null) {
                this.mVisibilityChangeForAutofillHandler.removeMessages(0);
            }
            if (z) {
                autofillManager.notifyViewVisibilityChanged(this, true);
            } else {
                if (this.mVisibilityChangeForAutofillHandler == null) {
                    this.mVisibilityChangeForAutofillHandler = new VisibilityChangeForAutofillHandler(autofillManager, this);
                }
                this.mVisibilityChangeForAutofillHandler.obtainMessage(0, this).sendToTarget();
            }
        }
        if (!TextUtils.isEmpty(getAccessibilityPaneTitle()) && z != z2) {
            if (z) {
                i = 16;
            } else {
                i = 32;
            }
            notifyViewAccessibilityStateChangedIfNeeded(i);
        }
    }

    public int getWindowVisibility() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mWindowVisibility;
        }
        return 8;
    }

    public void getWindowVisibleDisplayFrame(Rect rect) {
        if (this.mAttachInfo != null) {
            try {
                this.mAttachInfo.mSession.getDisplayFrame(this.mAttachInfo.mWindow, rect);
                Rect rect2 = this.mAttachInfo.mVisibleInsets;
                rect.left += rect2.left;
                rect.top += rect2.top;
                rect.right -= rect2.right;
                rect.bottom -= rect2.bottom;
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        DisplayManagerGlobal.getInstance().getRealDisplay(0).getRectSize(rect);
    }

    public void getWindowDisplayFrame(Rect rect) {
        if (this.mAttachInfo != null) {
            try {
                this.mAttachInfo.mSession.getDisplayFrame(this.mAttachInfo.mWindow, rect);
            } catch (RemoteException e) {
            }
        } else {
            DisplayManagerGlobal.getInstance().getRealDisplay(0).getRectSize(rect);
        }
    }

    public void dispatchConfigurationChanged(Configuration configuration) {
        onConfigurationChanged(configuration);
    }

    protected void onConfigurationChanged(Configuration configuration) {
    }

    void dispatchCollectViewAttributes(AttachInfo attachInfo, int i) {
        performCollectViewAttributes(attachInfo, i);
    }

    void performCollectViewAttributes(AttachInfo attachInfo, int i) {
        if ((i & 12) == 0) {
            if ((this.mViewFlags & 67108864) == 67108864) {
                attachInfo.mKeepScreenOn = true;
            }
            attachInfo.mSystemUiVisibility |= this.mSystemUiVisibility;
            ListenerInfo listenerInfo = this.mListenerInfo;
            if (listenerInfo != null && listenerInfo.mOnSystemUiVisibilityChangeListener != null) {
                attachInfo.mHasSystemUiListeners = true;
            }
        }
    }

    void needGlobalAttributesUpdate(boolean z) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null && !attachInfo.mRecomputeGlobalAttributes) {
            if (z || attachInfo.mKeepScreenOn || attachInfo.mSystemUiVisibility != 0 || attachInfo.mHasSystemUiListeners) {
                attachInfo.mRecomputeGlobalAttributes = true;
            }
        }
    }

    @ViewDebug.ExportedProperty
    public boolean isInTouchMode() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mInTouchMode;
        }
        return ViewRootImpl.isInTouchMode();
    }

    @ViewDebug.CapturedViewProperty
    public final Context getContext() {
        return this.mContext;
    }

    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            if ((this.mViewFlags & 32) == 32) {
                return true;
            }
            if (keyEvent.getRepeatCount() == 0) {
                boolean z = (this.mViewFlags & 16384) == 16384 || (this.mViewFlags & 2097152) == 2097152;
                if (z || (this.mViewFlags & 1073741824) == 1073741824) {
                    float width = getWidth() / 2.0f;
                    float height = getHeight() / 2.0f;
                    if (z) {
                        setPressed(true, width, height);
                    }
                    checkForLongClick(0, width, height);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            if ((this.mViewFlags & 32) == 32) {
                return true;
            }
            if ((this.mViewFlags & 16384) == 16384 && isPressed()) {
                setPressed(false);
                if (!this.mHasPerformedLongPress) {
                    removeLongPressCallback();
                    if (!keyEvent.isCanceled()) {
                        return performClickInternal();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        return false;
    }

    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        return false;
    }

    public boolean onCheckIsTextEditor() {
        return false;
    }

    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        return null;
    }

    public boolean checkInputConnectionProxy(View view) {
        return false;
    }

    public void createContextMenu(ContextMenu contextMenu) {
        ContextMenu.ContextMenuInfo contextMenuInfo = getContextMenuInfo();
        MenuBuilder menuBuilder = (MenuBuilder) contextMenu;
        menuBuilder.setCurrentMenuInfo(contextMenuInfo);
        onCreateContextMenu(contextMenu);
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnCreateContextMenuListener != null) {
            listenerInfo.mOnCreateContextMenuListener.onCreateContextMenu(contextMenu, this, contextMenuInfo);
        }
        menuBuilder.setCurrentMenuInfo(null);
        if (this.mParent != null) {
            this.mParent.createContextMenu(contextMenu);
        }
    }

    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return null;
    }

    protected void onCreateContextMenu(ContextMenu contextMenu) {
    }

    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }

    public boolean onHoverEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (!this.mSendingHoverAccessibilityEvents) {
            if ((actionMasked == 9 || actionMasked == 7) && !hasHoveredChild() && pointInView(motionEvent.getX(), motionEvent.getY())) {
                sendAccessibilityHoverEvent(128);
                this.mSendingHoverAccessibilityEvents = true;
            }
        } else if (actionMasked == 10 || (actionMasked == 2 && !pointInView(motionEvent.getX(), motionEvent.getY()))) {
            this.mSendingHoverAccessibilityEvents = false;
            sendAccessibilityHoverEvent(256);
        }
        if ((actionMasked == 9 || actionMasked == 7) && motionEvent.isFromSource(8194) && isOnScrollbar(motionEvent.getX(), motionEvent.getY())) {
            awakenScrollBars();
        }
        if (!isHoverable() && !isHovered()) {
            return false;
        }
        switch (actionMasked) {
            case 9:
                setHovered(true);
                break;
            case 10:
                setHovered(false);
                break;
        }
        dispatchGenericMotionEventInternal(motionEvent);
        return true;
    }

    private boolean isHoverable() {
        int i = this.mViewFlags;
        if ((i & 32) == 32) {
            return false;
        }
        return (i & 16384) == 16384 || (i & 2097152) == 2097152 || (i & 8388608) == 8388608;
    }

    @ViewDebug.ExportedProperty
    public boolean isHovered() {
        return (this.mPrivateFlags & 268435456) != 0;
    }

    public void setHovered(boolean z) {
        if (z) {
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

    public void onHoverChanged(boolean z) {
    }

    protected boolean handleScrollBarDragging(MotionEvent motionEvent) {
        int iRound;
        int iRound2;
        if (this.mScrollCache == null) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int action = motionEvent.getAction();
        if ((this.mScrollCache.mScrollBarDraggingState == 0 && action != 0) || !motionEvent.isFromSource(8194) || !motionEvent.isButtonPressed(1)) {
            this.mScrollCache.mScrollBarDraggingState = 0;
            return false;
        }
        if (action != 0) {
            if (action == 2) {
                if (this.mScrollCache.mScrollBarDraggingState == 0) {
                    return false;
                }
                if (this.mScrollCache.mScrollBarDraggingState != 1) {
                    if (this.mScrollCache.mScrollBarDraggingState == 2) {
                        Rect rect = this.mScrollCache.mScrollBarBounds;
                        getHorizontalScrollBarBounds(rect, null);
                        int iComputeHorizontalScrollRange = computeHorizontalScrollRange();
                        int iComputeHorizontalScrollOffset = computeHorizontalScrollOffset();
                        int iComputeHorizontalScrollExtent = computeHorizontalScrollExtent();
                        int thumbLength = ScrollBarUtils.getThumbLength(rect.width(), rect.height(), iComputeHorizontalScrollExtent, iComputeHorizontalScrollRange);
                        int thumbOffset = ScrollBarUtils.getThumbOffset(rect.width(), thumbLength, iComputeHorizontalScrollExtent, iComputeHorizontalScrollRange, iComputeHorizontalScrollOffset);
                        float f = x - this.mScrollCache.mScrollBarDraggingPos;
                        float fWidth = rect.width() - thumbLength;
                        float fMin = Math.min(Math.max(thumbOffset + f, 0.0f), fWidth);
                        int width = getWidth();
                        if (Math.round(fMin) != thumbOffset && fWidth > 0.0f && width > 0 && iComputeHorizontalScrollExtent > 0 && (iRound = Math.round(((iComputeHorizontalScrollRange - iComputeHorizontalScrollExtent) / (iComputeHorizontalScrollExtent / width)) * (fMin / fWidth))) != getScrollX()) {
                            this.mScrollCache.mScrollBarDraggingPos = x;
                            setScrollX(iRound);
                        }
                        return true;
                    }
                    if (this.mScrollCache.state != 0) {
                    }
                } else {
                    Rect rect2 = this.mScrollCache.mScrollBarBounds;
                    getVerticalScrollBarBounds(rect2, null);
                    int iComputeVerticalScrollRange = computeVerticalScrollRange();
                    int iComputeVerticalScrollOffset = computeVerticalScrollOffset();
                    int iComputeVerticalScrollExtent = computeVerticalScrollExtent();
                    int thumbLength2 = ScrollBarUtils.getThumbLength(rect2.height(), rect2.width(), iComputeVerticalScrollExtent, iComputeVerticalScrollRange);
                    int thumbOffset2 = ScrollBarUtils.getThumbOffset(rect2.height(), thumbLength2, iComputeVerticalScrollExtent, iComputeVerticalScrollRange, iComputeVerticalScrollOffset);
                    float f2 = y - this.mScrollCache.mScrollBarDraggingPos;
                    float fHeight = rect2.height() - thumbLength2;
                    float fMin2 = Math.min(Math.max(thumbOffset2 + f2, 0.0f), fHeight);
                    int height = getHeight();
                    if (Math.round(fMin2) != thumbOffset2 && fHeight > 0.0f && height > 0 && iComputeVerticalScrollExtent > 0 && (iRound2 = Math.round(((iComputeVerticalScrollRange - iComputeVerticalScrollExtent) / (iComputeVerticalScrollExtent / height)) * (fMin2 / fHeight))) != getScrollY()) {
                        this.mScrollCache.mScrollBarDraggingPos = y;
                        setScrollY(iRound2);
                    }
                    return true;
                }
            }
        } else {
            if (this.mScrollCache.state != 0) {
                return false;
            }
            if (isOnVerticalScrollbarThumb(x, y)) {
                this.mScrollCache.mScrollBarDraggingState = 1;
                this.mScrollCache.mScrollBarDraggingPos = y;
                return true;
            }
            if (isOnHorizontalScrollbarThumb(x, y)) {
                this.mScrollCache.mScrollBarDraggingState = 2;
                this.mScrollCache.mScrollBarDraggingPos = x;
                return true;
            }
        }
        this.mScrollCache.mScrollBarDraggingState = 0;
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zRequestFocus;
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int i = this.mViewFlags;
        int action = motionEvent.getAction();
        boolean z = (i & 16384) == 16384 || (i & 2097152) == 2097152 || (i & 8388608) == 8388608;
        if ((i & 32) == 32) {
            if (action == 1 && (this.mPrivateFlags & 16384) != 0) {
                setPressed(false);
            }
            this.mPrivateFlags3 &= -131073;
            return z;
        }
        if (this.mTouchDelegate != null && this.mTouchDelegate.onTouchEvent(motionEvent)) {
            return true;
        }
        if (!z && (i & 1073741824) != 1073741824) {
            return false;
        }
        switch (action) {
            case 0:
                if (motionEvent.getSource() == 4098) {
                    this.mPrivateFlags3 |= 131072;
                }
                this.mHasPerformedLongPress = false;
                if (!z) {
                    checkForLongClick(0, x, y);
                } else if (!performButtonActionOnTouchDown(motionEvent)) {
                    if (isInScrollingContainer()) {
                        this.mPrivateFlags |= 33554432;
                        if (this.mPendingCheckForTap == null) {
                            this.mPendingCheckForTap = new CheckForTap();
                        }
                        this.mPendingCheckForTap.x = motionEvent.getX();
                        this.mPendingCheckForTap.y = motionEvent.getY();
                        postDelayed(this.mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    } else {
                        setPressed(true, x, y);
                        checkForLongClick(0, x, y);
                    }
                }
                return true;
            case 1:
                this.mPrivateFlags3 &= -131073;
                if ((i & 1073741824) == 1073741824) {
                    handleTooltipUp();
                }
                if (!z) {
                    removeTapCallback();
                    removeLongPressCallback();
                    this.mInContextButtonPress = false;
                    this.mHasPerformedLongPress = false;
                    this.mIgnoreNextUpEvent = false;
                } else {
                    boolean z2 = (this.mPrivateFlags & 33554432) != 0;
                    if ((this.mPrivateFlags & 16384) != 0 || z2) {
                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                            zRequestFocus = requestFocus();
                        } else {
                            zRequestFocus = false;
                        }
                        if (z2) {
                            setPressed(true, x, y);
                        }
                        if (!this.mHasPerformedLongPress && !this.mIgnoreNextUpEvent) {
                            removeLongPressCallback();
                            if (!zRequestFocus) {
                                if (this.mPerformClick == null) {
                                    this.mPerformClick = new PerformClick();
                                }
                                if (ViewDebugManager.DEBUG_TOUCH) {
                                    Log.d(VIEW_LOG_TAG, "(View)Touch up: post perfomrClick runnable, this = " + this);
                                }
                                if (!post(this.mPerformClick)) {
                                    performClickInternal();
                                }
                            }
                        }
                        if (this.mUnsetPressedState == null) {
                            this.mUnsetPressedState = new UnsetPressedState();
                        }
                        if (z2) {
                            postDelayed(this.mUnsetPressedState, ViewConfiguration.getPressedStateDuration());
                        } else if (!post(this.mUnsetPressedState)) {
                            this.mUnsetPressedState.run();
                        }
                        removeTapCallback();
                    }
                    this.mIgnoreNextUpEvent = false;
                }
                return true;
            case 2:
                if (ViewDebugManager.DEBUG_MOTION) {
                    Log.d(VIEW_LOG_TAG, "(View)Touch move: x = " + x + ",y = " + y + ",mTouchSlop = " + this.mTouchSlop + ",this = " + this);
                }
                if (z) {
                    drawableHotspotChanged(x, y);
                }
                if (!pointInView(x, y, this.mTouchSlop)) {
                    removeTapCallback();
                    removeLongPressCallback();
                    if ((this.mPrivateFlags & 16384) != 0) {
                        setPressed(false);
                    }
                    this.mPrivateFlags3 &= -131073;
                }
                return true;
            case 3:
                if (ViewDebugManager.DEBUG_MOTION) {
                    Log.d(VIEW_LOG_TAG, "(View)Touch cancel: this = " + this);
                }
                if (z) {
                    setPressed(false);
                }
                removeTapCallback();
                removeLongPressCallback();
                this.mInContextButtonPress = false;
                this.mHasPerformedLongPress = false;
                this.mIgnoreNextUpEvent = false;
                this.mPrivateFlags3 &= -131073;
                return true;
            default:
                return true;
        }
    }

    public boolean isInScrollingContainer() {
        for (ViewParent parent = getParent(); parent != null && (parent instanceof ViewGroup); parent = parent.getParent()) {
            if (((ViewGroup) parent).shouldDelayChildPressedState()) {
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

    public void setTouchDelegate(TouchDelegate touchDelegate) {
        this.mTouchDelegate = touchDelegate;
    }

    public TouchDelegate getTouchDelegate() {
        return this.mTouchDelegate;
    }

    public final void requestUnbufferedDispatch(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (this.mAttachInfo != null) {
            if ((action != 0 && action != 2) || !motionEvent.isTouchEvent()) {
                return;
            }
            this.mAttachInfo.mUnbufferedDispatchRequested = true;
        }
    }

    private boolean hasSize() {
        return this.mBottom > this.mTop && this.mRight > this.mLeft;
    }

    private boolean canTakeFocus() {
        return (this.mViewFlags & 12) == 0 && (this.mViewFlags & 1) == 1 && (this.mViewFlags & 32) == 0 && (sCanFocusZeroSized || !isLayoutValid() || hasSize());
    }

    void setFlags(int i, int i2) {
        int i3;
        boolean zCanTakeFocus;
        boolean zIsEnabled = AccessibilityManager.getInstance(this.mContext).isEnabled();
        boolean z = zIsEnabled && includeForAccessibility();
        int i4 = this.mViewFlags;
        this.mViewFlags = (i2 & i) | (this.mViewFlags & (~i2));
        int i5 = this.mViewFlags ^ i4;
        if (i5 == 0) {
            return;
        }
        int i6 = this.mPrivateFlags;
        if ((this.mViewFlags & 16) != 0 && (i5 & BatteryStats.HistoryItem.EVENT_TEMP_WHITELIST_FINISH) != 0) {
            int i7 = (this.mViewFlags & 16384) != 0 ? 1 : 0;
            this.mViewFlags = (this.mViewFlags & (-2)) | i7;
            i3 = (i7 & 1) ^ (i4 & 1);
            i5 = (i5 & (-2)) | i3;
        } else {
            i3 = 0;
        }
        if ((i5 & 1) != 0 && (i6 & 16) != 0) {
            int i8 = i4 & 1;
            if (i8 == 1 && (i6 & 2) != 0) {
                clearFocus();
                if (this.mParent instanceof ViewGroup) {
                    ((ViewGroup) this.mParent).clearFocusedInCluster();
                }
            } else if (i8 == 0 && (i6 & 2) == 0 && this.mParent != null) {
                ViewRootImpl viewRootImpl = getViewRootImpl();
                if (!sAutoFocusableOffUIThreadWontNotifyParents || i3 == 0 || viewRootImpl == null || viewRootImpl.mThread == Thread.currentThread()) {
                    zCanTakeFocus = canTakeFocus();
                }
            }
            zCanTakeFocus = false;
        } else {
            zCanTakeFocus = false;
        }
        int i9 = i & 12;
        if (i9 == 0 && (i5 & 12) != 0) {
            this.mPrivateFlags |= 32;
            invalidate(true);
            needGlobalAttributesUpdate(true);
            zCanTakeFocus = hasSize();
        }
        if ((i5 & 32) != 0) {
            if ((this.mViewFlags & 32) == 0) {
                zCanTakeFocus = canTakeFocus();
            } else if (isFocused()) {
                clearFocus();
            }
        }
        if (zCanTakeFocus && this.mParent != null) {
            this.mParent.focusableViewAvailable(this);
        }
        if ((i5 & 8) != 0) {
            needGlobalAttributesUpdate(false);
            requestLayout();
            if ((this.mViewFlags & 12) == 8) {
                if (hasFocus()) {
                    clearFocus();
                    if (this.mParent instanceof ViewGroup) {
                        ((ViewGroup) this.mParent).clearFocusedInCluster();
                    }
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
        if ((i5 & 4) != 0) {
            needGlobalAttributesUpdate(false);
            this.mPrivateFlags |= 32;
            if ((this.mViewFlags & 12) == 4 && getRootView() != this) {
                if (hasFocus()) {
                    clearFocus();
                    if (this.mParent instanceof ViewGroup) {
                        ((ViewGroup) this.mParent).clearFocusedInCluster();
                    }
                }
                clearAccessibilityFocus();
            }
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mViewVisibilityChanged = true;
            }
        }
        int i10 = i5 & 12;
        if (i10 != 0) {
            if (i9 != 0 && this.mAttachInfo != null) {
                cleanupDraw();
            }
            if (this.mParent instanceof ViewGroup) {
                ((ViewGroup) this.mParent).onChildVisibilityChanged(this, i10, i9);
                ((View) this.mParent).invalidate(true);
            } else if (this.mParent != null) {
                this.mParent.invalidateChild(this, null);
            }
            if (this.mAttachInfo != null) {
                dispatchVisibilityChanged(this, i9);
                if (this.mParent != null && getWindowVisibility() == 0 && (!(this.mParent instanceof ViewGroup) || ((ViewGroup) this.mParent).isShown())) {
                    dispatchVisibilityAggregated(i9 == 0);
                }
                notifySubtreeAccessibilityStateChangedIfNeeded();
            }
        }
        if ((131072 & i5) != 0) {
            destroyDrawingCache();
        }
        if ((32768 & i5) != 0) {
            destroyDrawingCache();
            this.mPrivateFlags &= -32769;
            invalidateParentCaches();
        }
        if ((DRAWING_CACHE_QUALITY_MASK & i5) != 0) {
            destroyDrawingCache();
            this.mPrivateFlags &= -32769;
        }
        if ((i5 & 128) != 0) {
            if ((this.mViewFlags & 128) == 0 || this.mBackground != null || this.mDefaultFocusHighlight != null || (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null)) {
                this.mPrivateFlags &= -129;
            } else {
                this.mPrivateFlags |= 128;
            }
            requestLayout();
            invalidate(true);
        }
        if ((67108864 & i5) != 0 && this.mParent != null && this.mAttachInfo != null && !this.mAttachInfo.mRecomputeGlobalAttributes) {
            this.mParent.recomputeViewAttributes(this);
        }
        if (zIsEnabled) {
            if (isAccessibilityPane()) {
                i5 &= -13;
            }
            if ((i5 & 1) != 0 || (i5 & 12) != 0 || (i5 & 16384) != 0 || (2097152 & i5) != 0 || (8388608 & i5) != 0) {
                if (z != includeForAccessibility()) {
                    notifySubtreeAccessibilityStateChangedIfNeeded();
                    return;
                } else {
                    notifyViewAccessibilityStateChangedIfNeeded(0);
                    return;
                }
            }
            if ((i5 & 32) != 0) {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    public void bringToFront() {
        if (this.mParent != null) {
            this.mParent.bringChildToFront(this);
        }
    }

    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        notifySubtreeAccessibilityStateChangedIfNeeded();
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            postSendViewScrolledAccessibilityEventCallback(i - i3, i2 - i4);
        }
        this.mBackgroundSizeChanged = true;
        this.mDefaultFocusHighlightSizeChanged = true;
        if (this.mForegroundInfo != null) {
            this.mForegroundInfo.mBoundsChanged = true;
        }
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewScrollChanged = true;
        }
        if (this.mListenerInfo != null && this.mListenerInfo.mOnScrollChangeListener != null) {
            this.mListenerInfo.mOnScrollChangeListener.onScrollChange(this, i, i2, i3, i4);
        }
    }

    protected void onSizeChanged(int i, int i2, int i3, int i4) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public final ViewParent getParent() {
        return this.mParent;
    }

    public void setScrollX(int i) {
        scrollTo(i, this.mScrollY);
    }

    public void setScrollY(int i) {
        scrollTo(this.mScrollX, i);
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

    public void getDrawingRect(Rect rect) {
        rect.left = this.mScrollX;
        rect.top = this.mScrollY;
        rect.right = this.mScrollX + (this.mRight - this.mLeft);
        rect.bottom = this.mScrollY + (this.mBottom - this.mTop);
    }

    public final int getMeasuredWidth() {
        return this.mMeasuredWidth & 16777215;
    }

    @ViewDebug.ExportedProperty(category = "measurement", flagMapping = {@ViewDebug.FlagToString(equals = 16777216, mask = -16777216, name = "MEASURED_STATE_TOO_SMALL")})
    public final int getMeasuredWidthAndState() {
        return this.mMeasuredWidth;
    }

    public final int getMeasuredHeight() {
        return this.mMeasuredHeight & 16777215;
    }

    @ViewDebug.ExportedProperty(category = "measurement", flagMapping = {@ViewDebug.FlagToString(equals = 16777216, mask = -16777216, name = "MEASURED_STATE_TOO_SMALL")})
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
        return -(this.mRenderNode.getCameraDistance() * this.mResources.getDisplayMetrics().densityDpi);
    }

    public void setCameraDistance(float f) {
        float f2 = this.mResources.getDisplayMetrics().densityDpi;
        invalidateViewProperty(true, false);
        this.mRenderNode.setCameraDistance((-Math.abs(f)) / f2);
        invalidateViewProperty(false, false);
        invalidateParentIfNeededAndWasQuickRejected();
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotation() {
        return this.mRenderNode.getRotation();
    }

    public void setRotation(float f) {
        if (f != getRotation()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotation(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotationY() {
        return this.mRenderNode.getRotationY();
    }

    public void setRotationY(float f) {
        if (f != getRotationY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotationY(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getRotationX() {
        return this.mRenderNode.getRotationX();
    }

    public void setRotationX(float f) {
        if (f != getRotationX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setRotationX(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getScaleX() {
        return this.mRenderNode.getScaleX();
    }

    public void setScaleX(float f) {
        if (f != getScaleX()) {
            float fSanitizeFloatPropertyValue = sanitizeFloatPropertyValue(f, "scaleX");
            invalidateViewProperty(true, false);
            this.mRenderNode.setScaleX(fSanitizeFloatPropertyValue);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getScaleY() {
        return this.mRenderNode.getScaleY();
    }

    public void setScaleY(float f) {
        if (f != getScaleY()) {
            float fSanitizeFloatPropertyValue = sanitizeFloatPropertyValue(f, "scaleY");
            invalidateViewProperty(true, false);
            this.mRenderNode.setScaleY(fSanitizeFloatPropertyValue);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getPivotX() {
        return this.mRenderNode.getPivotX();
    }

    public void setPivotX(float f) {
        if (!this.mRenderNode.isPivotExplicitlySet() || f != getPivotX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setPivotX(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getPivotY() {
        return this.mRenderNode.getPivotY();
    }

    public void setPivotY(float f) {
        if (!this.mRenderNode.isPivotExplicitlySet() || f != getPivotY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setPivotY(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    public boolean isPivotSet() {
        return this.mRenderNode.isPivotExplicitlySet();
    }

    public void resetPivot() {
        if (this.mRenderNode.resetPivot()) {
            invalidateViewProperty(false, false);
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getAlpha() {
        if (this.mTransformationInfo != null) {
            return this.mTransformationInfo.mAlpha;
        }
        return 1.0f;
    }

    public void forceHasOverlappingRendering(boolean z) {
        this.mPrivateFlags3 |= 16777216;
        if (z) {
            this.mPrivateFlags3 |= 8388608;
        } else {
            this.mPrivateFlags3 &= -8388609;
        }
    }

    public final boolean getHasOverlappingRendering() {
        if ((this.mPrivateFlags3 & 16777216) != 0) {
            return (this.mPrivateFlags3 & 8388608) != 0;
        }
        return hasOverlappingRendering();
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean hasOverlappingRendering() {
        return true;
    }

    public void setAlpha(float f) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mAlpha != f) {
            setAlphaInternal(f);
            if (onSetAlpha((int) (f * 255.0f))) {
                this.mPrivateFlags |= 262144;
                invalidateParentCaches();
                invalidate(true);
            } else {
                this.mPrivateFlags &= -262145;
                invalidateViewProperty(true, false);
                this.mRenderNode.setAlpha(getFinalAlpha());
            }
        }
    }

    boolean setAlphaNoInvalidation(float f) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mAlpha != f) {
            setAlphaInternal(f);
            if (onSetAlpha((int) (f * 255.0f))) {
                this.mPrivateFlags |= 262144;
                return true;
            }
            this.mPrivateFlags &= -262145;
            this.mRenderNode.setAlpha(getFinalAlpha());
            return false;
        }
        return false;
    }

    private void setAlphaInternal(float f) {
        float f2 = this.mTransformationInfo.mAlpha;
        this.mTransformationInfo.mAlpha = f;
        if ((f == 0.0f) ^ (f2 == 0.0f)) {
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    public void setTransitionAlpha(float f) {
        ensureTransformationInfo();
        if (this.mTransformationInfo.mTransitionAlpha != f) {
            this.mTransformationInfo.mTransitionAlpha = f;
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

    public final void setTop(int i) {
        int i2;
        int i3;
        if (i != this.mTop) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (zHasIdentityMatrix) {
                if (this.mAttachInfo != null) {
                    if (i < this.mTop) {
                        i3 = i - this.mTop;
                        i2 = i;
                    } else {
                        i2 = this.mTop;
                        i3 = 0;
                    }
                    invalidate(0, i3, this.mRight - this.mLeft, this.mBottom - i2);
                }
            } else {
                invalidate(true);
            }
            int i4 = this.mRight - this.mLeft;
            int i5 = this.mBottom - this.mTop;
            this.mTop = i;
            this.mRenderNode.setTop(this.mTop);
            sizeChange(i4, this.mBottom - this.mTop, i4, i5);
            if (!zHasIdentityMatrix) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            this.mDefaultFocusHighlightSizeChanged = true;
            if (this.mForegroundInfo != null) {
                this.mForegroundInfo.mBoundsChanged = true;
            }
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

    public final void setBottom(int i) {
        int i2;
        if (i != this.mBottom) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (zHasIdentityMatrix) {
                if (this.mAttachInfo != null) {
                    if (i < this.mBottom) {
                        i2 = this.mBottom;
                    } else {
                        i2 = i;
                    }
                    invalidate(0, 0, this.mRight - this.mLeft, i2 - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int i3 = this.mRight - this.mLeft;
            int i4 = this.mBottom - this.mTop;
            this.mBottom = i;
            this.mRenderNode.setBottom(this.mBottom);
            sizeChange(i3, this.mBottom - this.mTop, i3, i4);
            if (!zHasIdentityMatrix) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            this.mDefaultFocusHighlightSizeChanged = true;
            if (this.mForegroundInfo != null) {
                this.mForegroundInfo.mBoundsChanged = true;
            }
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

    public final void setLeft(int i) {
        int i2;
        int i3;
        if (i != this.mLeft) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (zHasIdentityMatrix) {
                if (this.mAttachInfo != null) {
                    if (i < this.mLeft) {
                        i3 = i - this.mLeft;
                        i2 = i;
                    } else {
                        i2 = this.mLeft;
                        i3 = 0;
                    }
                    invalidate(i3, 0, this.mRight - i2, this.mBottom - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int i4 = this.mRight - this.mLeft;
            int i5 = this.mBottom - this.mTop;
            this.mLeft = i;
            this.mRenderNode.setLeft(i);
            sizeChange(this.mRight - this.mLeft, i5, i4, i5);
            if (!zHasIdentityMatrix) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            this.mDefaultFocusHighlightSizeChanged = true;
            if (this.mForegroundInfo != null) {
                this.mForegroundInfo.mBoundsChanged = true;
            }
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

    public final void setRight(int i) {
        int i2;
        if (i != this.mRight) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (zHasIdentityMatrix) {
                if (this.mAttachInfo != null) {
                    if (i < this.mRight) {
                        i2 = this.mRight;
                    } else {
                        i2 = i;
                    }
                    invalidate(0, 0, i2 - this.mLeft, this.mBottom - this.mTop);
                }
            } else {
                invalidate(true);
            }
            int i3 = this.mRight - this.mLeft;
            int i4 = this.mBottom - this.mTop;
            this.mRight = i;
            this.mRenderNode.setRight(this.mRight);
            sizeChange(this.mRight - this.mLeft, i4, i3, i4);
            if (!zHasIdentityMatrix) {
                this.mPrivateFlags |= 32;
                invalidate(true);
            }
            this.mBackgroundSizeChanged = true;
            this.mDefaultFocusHighlightSizeChanged = true;
            if (this.mForegroundInfo != null) {
                this.mForegroundInfo.mBoundsChanged = true;
            }
            invalidateParentIfNeeded();
            if ((this.mPrivateFlags2 & 268435456) == 268435456) {
                invalidateParentIfNeeded();
            }
        }
    }

    private static float sanitizeFloatPropertyValue(float f, String str) {
        return sanitizeFloatPropertyValue(f, str, -3.4028235E38f, Float.MAX_VALUE);
    }

    private static float sanitizeFloatPropertyValue(float f, String str, float f2, float f3) {
        if (f >= f2 && f <= f3) {
            return f;
        }
        if (f < f2 || f == Float.NEGATIVE_INFINITY) {
            if (sThrowOnInvalidFloatProperties) {
                throw new IllegalArgumentException("Cannot set '" + str + "' to " + f + ", the value must be >= " + f2);
            }
            return f2;
        }
        if (f > f3 || f == Float.POSITIVE_INFINITY) {
            if (sThrowOnInvalidFloatProperties) {
                throw new IllegalArgumentException("Cannot set '" + str + "' to " + f + ", the value must be <= " + f3);
            }
            return f3;
        }
        if (Float.isNaN(f)) {
            if (sThrowOnInvalidFloatProperties) {
                throw new IllegalArgumentException("Cannot set '" + str + "' to Float.NaN");
            }
            return 0.0f;
        }
        throw new IllegalStateException("How do you get here?? " + f);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getX() {
        return this.mLeft + getTranslationX();
    }

    public void setX(float f) {
        setTranslationX(f - this.mLeft);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getY() {
        return this.mTop + getTranslationY();
    }

    public void setY(float f) {
        setTranslationY(f - this.mTop);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getZ() {
        return getElevation() + getTranslationZ();
    }

    public void setZ(float f) {
        setTranslationZ(f - getElevation());
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getElevation() {
        return this.mRenderNode.getElevation();
    }

    public void setElevation(float f) {
        if (f != getElevation()) {
            float fSanitizeFloatPropertyValue = sanitizeFloatPropertyValue(f, "elevation");
            invalidateViewProperty(true, false);
            this.mRenderNode.setElevation(fSanitizeFloatPropertyValue);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationX() {
        return this.mRenderNode.getTranslationX();
    }

    public void setTranslationX(float f) {
        if (f != getTranslationX()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationX(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationY() {
        return this.mRenderNode.getTranslationY();
    }

    public void setTranslationY(float f) {
        if (f != getTranslationY()) {
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationY(f);
            invalidateViewProperty(false, true);
            invalidateParentIfNeededAndWasQuickRejected();
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public float getTranslationZ() {
        return this.mRenderNode.getTranslationZ();
    }

    public void setTranslationZ(float f) {
        if (f != getTranslationZ()) {
            float fSanitizeFloatPropertyValue = sanitizeFloatPropertyValue(f, "translationZ");
            invalidateViewProperty(true, false);
            this.mRenderNode.setTranslationZ(fSanitizeFloatPropertyValue);
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
        if (this.mStateListAnimator == stateListAnimator) {
            return;
        }
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

    public final boolean getClipToOutline() {
        return this.mRenderNode.getClipToOutline();
    }

    public void setClipToOutline(boolean z) {
        damageInParent();
        if (getClipToOutline() != z) {
            this.mRenderNode.setClipToOutline(z);
        }
    }

    private void setOutlineProviderFromAttribute(int i) {
        switch (i) {
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

    public void setOutlineProvider(ViewOutlineProvider viewOutlineProvider) {
        this.mOutlineProvider = viewOutlineProvider;
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
        if (this.mAttachInfo == null) {
            return;
        }
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

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean hasShadow() {
        return this.mRenderNode.hasShadow();
    }

    public void setOutlineSpotShadowColor(int i) {
        if (this.mRenderNode.setSpotShadowColor(i)) {
            invalidateViewProperty(true, true);
        }
    }

    public int getOutlineSpotShadowColor() {
        return this.mRenderNode.getSpotShadowColor();
    }

    public void setOutlineAmbientShadowColor(int i) {
        if (this.mRenderNode.setAmbientShadowColor(i)) {
            invalidateViewProperty(true, true);
        }
    }

    public int getOutlineAmbientShadowColor() {
        return this.mRenderNode.getAmbientShadowColor();
    }

    public void setRevealClip(boolean z, float f, float f2, float f3) {
        this.mRenderNode.setRevealClip(z, f, f2, f3);
        invalidateViewProperty(false, false);
    }

    public void getHitRect(Rect rect) {
        if (hasIdentityMatrix() || this.mAttachInfo == null) {
            rect.set(this.mLeft, this.mTop, this.mRight, this.mBottom);
            return;
        }
        RectF rectF = this.mAttachInfo.mTmpTransformRect;
        rectF.set(0.0f, 0.0f, getWidth(), getHeight());
        getMatrix().mapRect(rectF);
        rect.set(((int) rectF.left) + this.mLeft, ((int) rectF.top) + this.mTop, ((int) rectF.right) + this.mLeft, ((int) rectF.bottom) + this.mTop);
    }

    final boolean pointInView(float f, float f2) {
        return pointInView(f, f2, 0.0f);
    }

    public boolean pointInView(float f, float f2, float f3) {
        float f4 = -f3;
        return f >= f4 && f2 >= f4 && f < ((float) (this.mRight - this.mLeft)) + f3 && f2 < ((float) (this.mBottom - this.mTop)) + f3;
    }

    public void getFocusedRect(Rect rect) {
        getDrawingRect(rect);
    }

    public boolean getGlobalVisibleRect(Rect rect, Point point) {
        int i = this.mRight - this.mLeft;
        int i2 = this.mBottom - this.mTop;
        if (i <= 0 || i2 <= 0) {
            return false;
        }
        rect.set(0, 0, i, i2);
        if (point != null) {
            point.set(-this.mScrollX, -this.mScrollY);
        }
        return this.mParent == null || this.mParent.getChildVisibleRect(this, rect, point);
    }

    public final boolean getGlobalVisibleRect(Rect rect) {
        return getGlobalVisibleRect(rect, null);
    }

    public final boolean getLocalVisibleRect(Rect rect) {
        Point point = this.mAttachInfo != null ? this.mAttachInfo.mPoint : new Point();
        if (getGlobalVisibleRect(rect, point)) {
            rect.offset(-point.x, -point.y);
            return true;
        }
        return false;
    }

    public void offsetTopAndBottom(int i) {
        int i2;
        int i3;
        int i4;
        if (i != 0) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (!zHasIdentityMatrix || isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                ViewParent viewParent = this.mParent;
                if (viewParent != null && this.mAttachInfo != null) {
                    Rect rect = this.mAttachInfo.mTmpInvalRect;
                    if (i < 0) {
                        int i5 = this.mTop + i;
                        i2 = this.mBottom;
                        i3 = i5;
                        i4 = i;
                    } else {
                        int i6 = this.mTop;
                        i2 = this.mBottom + i;
                        i3 = i6;
                        i4 = 0;
                    }
                    rect.set(0, i4, this.mRight - this.mLeft, i2 - i3);
                    viewParent.invalidateChild(this, rect);
                }
            }
            this.mTop += i;
            this.mBottom += i;
            this.mRenderNode.offsetTopAndBottom(i);
            if (isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
                invalidateParentIfNeededAndWasQuickRejected();
            } else {
                if (!zHasIdentityMatrix) {
                    invalidateViewProperty(false, true);
                }
                invalidateParentIfNeeded();
            }
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
    }

    public void offsetLeftAndRight(int i) {
        int i2;
        int i3;
        if (i != 0) {
            boolean zHasIdentityMatrix = hasIdentityMatrix();
            if (!zHasIdentityMatrix || isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
            } else {
                ViewParent viewParent = this.mParent;
                if (viewParent != null && this.mAttachInfo != null) {
                    Rect rect = this.mAttachInfo.mTmpInvalRect;
                    if (i < 0) {
                        i2 = this.mLeft + i;
                        i3 = this.mRight;
                    } else {
                        i2 = this.mLeft;
                        i3 = this.mRight + i;
                    }
                    rect.set(0, 0, i3 - i2, this.mBottom - this.mTop);
                    viewParent.invalidateChild(this, rect);
                }
            }
            this.mLeft += i;
            this.mRight += i;
            this.mRenderNode.offsetLeftAndRight(i);
            if (isHardwareAccelerated()) {
                invalidateViewProperty(false, false);
                invalidateParentIfNeededAndWasQuickRejected();
            } else {
                if (!zHasIdentityMatrix) {
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

    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (layoutParams == null) {
            throw new NullPointerException("Layout parameters cannot be null");
        }
        this.mLayoutParams = layoutParams;
        resolveLayoutParams();
        if (this.mParent instanceof ViewGroup) {
            ((ViewGroup) this.mParent).onSetLayoutParams(this, layoutParams);
        }
        requestLayout();
    }

    public void resolveLayoutParams() {
        if (this.mLayoutParams != null) {
            this.mLayoutParams.resolveLayoutDirection(getLayoutDirection());
        }
    }

    public void scrollTo(int i, int i2) {
        if (this.mScrollX != i || this.mScrollY != i2) {
            int i3 = this.mScrollX;
            int i4 = this.mScrollY;
            this.mScrollX = i;
            this.mScrollY = i2;
            invalidateParentCaches();
            onScrollChanged(this.mScrollX, this.mScrollY, i3, i4);
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }

    public void scrollBy(int i, int i2) {
        scrollTo(this.mScrollX + i, this.mScrollY + i2);
    }

    protected boolean awakenScrollBars() {
        return this.mScrollCache != null && awakenScrollBars(this.mScrollCache.scrollBarDefaultDelayBeforeFade, true);
    }

    private boolean initialAwakenScrollBars() {
        return this.mScrollCache != null && awakenScrollBars(this.mScrollCache.scrollBarDefaultDelayBeforeFade * 4, true);
    }

    protected boolean awakenScrollBars(int i) {
        return awakenScrollBars(i, true);
    }

    protected boolean awakenScrollBars(int i, boolean z) {
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache == null || !scrollabilityCache.fadeScrollBars) {
            return false;
        }
        if (scrollabilityCache.scrollBar == null) {
            scrollabilityCache.scrollBar = new ScrollBarDrawable();
            scrollabilityCache.scrollBar.setState(getDrawableState());
            scrollabilityCache.scrollBar.setCallback(this);
        }
        if (!isHorizontalScrollBarEnabled() && !isVerticalScrollBarEnabled()) {
            return false;
        }
        if (z) {
            postInvalidateOnAnimation();
        }
        if (scrollabilityCache.state == 0) {
            i = Math.max(MetricsProto.MetricsEvent.SETTINGS_LANGUAGE_CATEGORY, i);
        }
        long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() + ((long) i);
        scrollabilityCache.fadeStartTime = jCurrentAnimationTimeMillis;
        scrollabilityCache.state = 1;
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mHandler.removeCallbacks(scrollabilityCache);
            this.mAttachInfo.mHandler.postAtTime(scrollabilityCache, jCurrentAnimationTimeMillis);
        }
        return true;
    }

    private boolean skipInvalidate() {
        return ((this.mViewFlags & 12) == 0 || this.mCurrentAnimation != null || ((this.mParent instanceof ViewGroup) && ((ViewGroup) this.mParent).isViewTransitioning(this))) ? false : true;
    }

    @Deprecated
    public void invalidate(Rect rect) {
        int i = this.mScrollX;
        int i2 = this.mScrollY;
        invalidateInternal(rect.left - i, rect.top - i2, rect.right - i, rect.bottom - i2, true, false);
    }

    @Deprecated
    public void invalidate(int i, int i2, int i3, int i4) {
        int i5 = this.mScrollX;
        int i6 = this.mScrollY;
        invalidateInternal(i - i5, i2 - i6, i3 - i5, i4 - i6, true, false);
    }

    public void invalidate() {
        invalidate(true);
    }

    public void invalidate(boolean z) {
        invalidateInternal(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop, z, true);
    }

    void invalidateInternal(int i, int i2, int i3, int i4, boolean z, boolean z2) {
        View projectionReceiver;
        if (this.mGhostView != null) {
            this.mGhostView.invalidate(true);
            return;
        }
        if (skipInvalidate()) {
            if (ViewDebugManager.DEBUG_DRAW) {
                Log.d(VIEW_LOG_TAG, "View invalidate: skipInvalidate , this = " + this);
                return;
            }
            return;
        }
        if ((this.mPrivateFlags & 48) == 48 || ((z && (this.mPrivateFlags & 32768) == 32768) || (this.mPrivateFlags & Integer.MIN_VALUE) != Integer.MIN_VALUE || (z2 && isOpaque() != this.mLastIsOpaque))) {
            if (z2) {
                this.mLastIsOpaque = isOpaque();
                this.mPrivateFlags &= -33;
            }
            this.mPrivateFlags |= 2097152;
            if (z) {
                this.mPrivateFlags |= Integer.MIN_VALUE;
                this.mPrivateFlags &= -32769;
            }
            AttachInfo attachInfo = this.mAttachInfo;
            ViewParent viewParent = this.mParent;
            if (viewParent != null && attachInfo != null && i < i3 && i2 < i4) {
                Rect rect = attachInfo.mTmpInvalRect;
                rect.set(i, i2, i3, i4);
                viewParent.invalidateChild(this, rect);
            }
            if (this.mBackground != null && this.mBackground.isProjected() && (projectionReceiver = getProjectionReceiver()) != null) {
                projectionReceiver.damageInParent();
            }
        }
    }

    private View getProjectionReceiver() {
        for (ViewParent parent = getParent(); parent != null && (parent instanceof View); parent = parent.getParent()) {
            View view = (View) parent;
            if (view.isProjectionReceiver()) {
                return view;
            }
        }
        return null;
    }

    private boolean isProjectionReceiver() {
        return this.mBackground != null;
    }

    void invalidateViewProperty(boolean z, boolean z2) {
        if (!isHardwareAccelerated() || !this.mRenderNode.isValid() || (this.mPrivateFlags & 64) != 0) {
            if (z) {
                invalidateParentCaches();
            }
            if (z2) {
                this.mPrivateFlags |= 32;
            }
            invalidate(false);
            return;
        }
        damageInParent();
    }

    protected void damageInParent() {
        if (this.mParent != null && this.mAttachInfo != null) {
            this.mParent.onDescendantInvalidated(this, this);
        }
    }

    void transformRect(Rect rect) {
        if (!getMatrix().isIdentity()) {
            RectF rectF = this.mAttachInfo.mTmpTransformRect;
            rectF.set(rect);
            getMatrix().mapRect(rectF);
            rect.set((int) Math.floor(rectF.left), (int) Math.floor(rectF.top), (int) Math.ceil(rectF.right), (int) Math.ceil(rectF.bottom));
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
        int i;
        if (this.mBackground != null && this.mBackground.getOpacity() == -1) {
            this.mPrivateFlags |= 8388608;
        } else {
            this.mPrivateFlags &= -8388609;
        }
        int i2 = this.mViewFlags;
        if (((i2 & 512) == 0 && (i2 & 256) == 0) || (i = i2 & 50331648) == 0 || i == 33554432) {
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

    private HandlerActionQueue getRunQueue() {
        if (this.mRunQueue == null) {
            this.mRunQueue = new HandlerActionQueue();
        }
        return this.mRunQueue;
    }

    public ViewRootImpl getViewRootImpl() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mViewRootImpl;
        }
        return null;
    }

    public ThreadedRenderer getThreadedRenderer() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mThreadedRenderer;
        }
        return null;
    }

    public boolean post(Runnable runnable) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(runnable);
        }
        if (ViewDebugManager.DEBUG_MOTION) {
            Log.w(VIEW_LOG_TAG, "(View)post runnable but AttachInfo = null, this = " + this);
        }
        getRunQueue().post(runnable);
        return true;
    }

    public boolean postDelayed(Runnable runnable, long j) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.postDelayed(runnable, j);
        }
        getRunQueue().postDelayed(runnable, j);
        return true;
    }

    public void postOnAnimation(Runnable runnable) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.mChoreographer.postCallback(1, runnable, null);
        } else {
            getRunQueue().post(runnable);
        }
    }

    public void postOnAnimationDelayed(Runnable runnable, long j) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.mChoreographer.postCallbackDelayed(1, runnable, null, j);
        } else {
            getRunQueue().postDelayed(runnable, j);
        }
    }

    public boolean removeCallbacks(Runnable runnable) {
        if (runnable != null) {
            AttachInfo attachInfo = this.mAttachInfo;
            if (attachInfo != null) {
                attachInfo.mHandler.removeCallbacks(runnable);
                attachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, runnable, null);
            }
            getRunQueue().removeCallbacks(runnable);
        }
        return true;
    }

    public void postInvalidate() {
        postInvalidateDelayed(0L);
    }

    public void postInvalidate(int i, int i2, int i3, int i4) {
        postInvalidateDelayed(0L, i, i2, i3, i4);
    }

    public void postInvalidateDelayed(long j) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.dispatchInvalidateDelayed(this, j);
        }
    }

    public void postInvalidateDelayed(long j, int i, int i2, int i3, int i4) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            AttachInfo.InvalidateInfo invalidateInfoObtain = AttachInfo.InvalidateInfo.obtain();
            invalidateInfoObtain.target = this;
            invalidateInfoObtain.left = i;
            invalidateInfoObtain.top = i2;
            invalidateInfoObtain.right = i3;
            invalidateInfoObtain.bottom = i4;
            attachInfo.mViewRootImpl.dispatchInvalidateRectDelayed(invalidateInfoObtain, j);
        }
    }

    public void postInvalidateOnAnimation() {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRootImpl.dispatchInvalidateOnAnimation(this);
        }
    }

    public void postInvalidateOnAnimation(int i, int i2, int i3, int i4) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            AttachInfo.InvalidateInfo invalidateInfoObtain = AttachInfo.InvalidateInfo.obtain();
            invalidateInfoObtain.target = this;
            invalidateInfoObtain.left = i;
            invalidateInfoObtain.top = i2;
            invalidateInfoObtain.right = i3;
            invalidateInfoObtain.bottom = i4;
            attachInfo.mViewRootImpl.dispatchInvalidateRectOnAnimation(invalidateInfoObtain);
        }
    }

    private void postSendViewScrolledAccessibilityEventCallback(int i, int i2) {
        if (this.mSendViewScrolledAccessibilityEvent == null) {
            this.mSendViewScrolledAccessibilityEvent = new SendViewScrolledAccessibilityEvent();
        }
        this.mSendViewScrolledAccessibilityEvent.post(i, i2);
    }

    public void computeScroll() {
    }

    public boolean isHorizontalFadingEdgeEnabled() {
        return (this.mViewFlags & 4096) == 4096;
    }

    public void setHorizontalFadingEdgeEnabled(boolean z) {
        if (isHorizontalFadingEdgeEnabled() != z) {
            if (z) {
                initScrollCache();
            }
            this.mViewFlags ^= 4096;
        }
    }

    public boolean isVerticalFadingEdgeEnabled() {
        return (this.mViewFlags & 8192) == 8192;
    }

    public void setVerticalFadingEdgeEnabled(boolean z) {
        if (isVerticalFadingEdgeEnabled() != z) {
            if (z) {
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

    public void setHorizontalScrollBarEnabled(boolean z) {
        if (isHorizontalScrollBarEnabled() != z) {
            this.mViewFlags ^= 256;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    public boolean isVerticalScrollBarEnabled() {
        return (this.mViewFlags & 512) == 512;
    }

    public void setVerticalScrollBarEnabled(boolean z) {
        if (isVerticalScrollBarEnabled() != z) {
            this.mViewFlags ^= 512;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    protected void recomputePadding() {
        internalSetPadding(this.mUserPaddingLeft, this.mPaddingTop, this.mUserPaddingRight, this.mUserPaddingBottom);
    }

    public void setScrollbarFadingEnabled(boolean z) {
        initScrollCache();
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        scrollabilityCache.fadeScrollBars = z;
        if (z) {
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

    public void setScrollBarDefaultDelayBeforeFade(int i) {
        getScrollCache().scrollBarDefaultDelayBeforeFade = i;
    }

    public int getScrollBarFadeDuration() {
        return this.mScrollCache == null ? ViewConfiguration.getScrollBarFadeDuration() : this.mScrollCache.scrollBarFadeDuration;
    }

    public void setScrollBarFadeDuration(int i) {
        getScrollCache().scrollBarFadeDuration = i;
    }

    public int getScrollBarSize() {
        return this.mScrollCache == null ? ViewConfiguration.get(this.mContext).getScaledScrollBarSize() : this.mScrollCache.scrollBarSize;
    }

    public void setScrollBarSize(int i) {
        getScrollCache().scrollBarSize = i;
    }

    public void setScrollBarStyle(int i) {
        if (i != (this.mViewFlags & 50331648)) {
            this.mViewFlags = (i & 50331648) | (this.mViewFlags & (-50331649));
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

    public boolean canScrollHorizontally(int i) {
        int iComputeHorizontalScrollOffset = computeHorizontalScrollOffset();
        int iComputeHorizontalScrollRange = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (iComputeHorizontalScrollRange == 0) {
            return false;
        }
        if (i < 0) {
            if (iComputeHorizontalScrollOffset <= 0) {
                return false;
            }
            return true;
        }
        if (iComputeHorizontalScrollOffset >= iComputeHorizontalScrollRange - 1) {
            return false;
        }
        return true;
    }

    public boolean canScrollVertically(int i) {
        int iComputeVerticalScrollOffset = computeVerticalScrollOffset();
        int iComputeVerticalScrollRange = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (iComputeVerticalScrollRange == 0) {
            return false;
        }
        if (i < 0) {
            if (iComputeVerticalScrollOffset <= 0) {
                return false;
            }
            return true;
        }
        if (iComputeVerticalScrollOffset >= iComputeVerticalScrollRange - 1) {
            return false;
        }
        return true;
    }

    void getScrollIndicatorBounds(Rect rect) {
        rect.left = this.mScrollX;
        rect.right = (this.mScrollX + this.mRight) - this.mLeft;
        rect.top = this.mScrollY;
        rect.bottom = (this.mScrollY + this.mBottom) - this.mTop;
    }

    private void onDrawScrollIndicators(Canvas canvas) {
        Drawable drawable;
        if ((this.mPrivateFlags3 & SCROLL_INDICATORS_PFLAG3_MASK) == 0 || (drawable = this.mScrollIndicatorDrawable) == null) {
            return;
        }
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        Rect rect = this.mAttachInfo.mTmpInvalRect;
        getScrollIndicatorBounds(rect);
        if ((this.mPrivateFlags3 & 256) != 0 && canScrollVertically(-1)) {
            drawable.setBounds(rect.left, rect.top, rect.right, rect.top + intrinsicHeight);
            drawable.draw(canvas);
        }
        if ((this.mPrivateFlags3 & 512) != 0 && canScrollVertically(1)) {
            drawable.setBounds(rect.left, rect.bottom - intrinsicHeight, rect.right, rect.bottom);
            drawable.draw(canvas);
        }
        int i = 4096;
        int i2 = 8192;
        if (getLayoutDirection() == 1) {
            i2 = 4096;
            i = 8192;
        }
        if (((1024 | i) & this.mPrivateFlags3) != 0 && canScrollHorizontally(-1)) {
            drawable.setBounds(rect.left, rect.top, rect.left + intrinsicWidth, rect.bottom);
            drawable.draw(canvas);
        }
        if (((2048 | i2) & this.mPrivateFlags3) != 0 && canScrollHorizontally(1)) {
            drawable.setBounds(rect.right - intrinsicWidth, rect.top, rect.right, rect.bottom);
            drawable.draw(canvas);
        }
    }

    private void getHorizontalScrollBarBounds(Rect rect, Rect rect2) {
        if (rect == null) {
            rect = rect2;
        }
        if (rect == null) {
            return;
        }
        int i = (this.mViewFlags & 33554432) == 0 ? -1 : 0;
        boolean z = isVerticalScrollBarEnabled() && !isVerticalScrollBarHidden();
        int horizontalScrollbarHeight = getHorizontalScrollbarHeight();
        int verticalScrollbarWidth = z ? getVerticalScrollbarWidth() : 0;
        int i2 = this.mRight - this.mLeft;
        int i3 = this.mBottom - this.mTop;
        rect.top = ((this.mScrollY + i3) - horizontalScrollbarHeight) - (this.mUserPaddingBottom & i);
        rect.left = this.mScrollX + (this.mPaddingLeft & i);
        rect.right = ((this.mScrollX + i2) - (i & this.mUserPaddingRight)) - verticalScrollbarWidth;
        rect.bottom = rect.top + horizontalScrollbarHeight;
        if (rect2 == null) {
            return;
        }
        if (rect2 != rect) {
            rect2.set(rect);
        }
        int i4 = this.mScrollCache.scrollBarMinTouchTarget;
        if (rect2.height() < i4) {
            rect2.bottom = Math.min(rect2.bottom + ((i4 - rect2.height()) / 2), this.mScrollY + i3);
            rect2.top = rect2.bottom - i4;
        }
        if (rect2.width() < i4) {
            rect2.left -= (i4 - rect2.width()) / 2;
            rect2.right = rect2.left + i4;
        }
    }

    private void getVerticalScrollBarBounds(Rect rect, Rect rect2) {
        if (this.mRoundScrollbarRenderer == null) {
            getStraightVerticalScrollBarBounds(rect, rect2);
            return;
        }
        if (rect == null) {
            rect = rect2;
        }
        getRoundVerticalScrollBarBounds(rect);
    }

    private void getRoundVerticalScrollBarBounds(Rect rect) {
        int i = this.mRight - this.mLeft;
        int i2 = this.mBottom - this.mTop;
        rect.left = this.mScrollX;
        rect.top = this.mScrollY;
        rect.right = rect.left + i;
        rect.bottom = this.mScrollY + i2;
    }

    private void getStraightVerticalScrollBarBounds(Rect rect, Rect rect2) {
        if (rect == null) {
            rect = rect2;
        }
        if (rect == null) {
            return;
        }
        int i = (this.mViewFlags & 33554432) == 0 ? -1 : 0;
        int verticalScrollbarWidth = getVerticalScrollbarWidth();
        int i2 = this.mVerticalScrollbarPosition;
        if (i2 == 0) {
            i2 = isLayoutRtl() ? 1 : 2;
        }
        int i3 = this.mRight - this.mLeft;
        int i4 = this.mBottom - this.mTop;
        if (i2 != 1) {
            rect.left = ((this.mScrollX + i3) - verticalScrollbarWidth) - (this.mUserPaddingRight & i);
        } else {
            rect.left = this.mScrollX + (this.mUserPaddingLeft & i);
        }
        rect.top = this.mScrollY + (this.mPaddingTop & i);
        rect.right = rect.left + verticalScrollbarWidth;
        rect.bottom = (this.mScrollY + i4) - (i & this.mUserPaddingBottom);
        if (rect2 == null) {
            return;
        }
        if (rect2 != rect) {
            rect2.set(rect);
        }
        int i5 = this.mScrollCache.scrollBarMinTouchTarget;
        if (rect2.width() < i5) {
            int iWidth = (i5 - rect2.width()) / 2;
            if (i2 == 2) {
                rect2.right = Math.min(rect2.right + iWidth, this.mScrollX + i3);
                rect2.left = rect2.right - i5;
            } else {
                rect2.left = Math.max(rect2.left + iWidth, this.mScrollX);
                rect2.right = rect2.left + i5;
            }
        }
        if (rect2.height() < i5) {
            rect2.top -= (i5 - rect2.height()) / 2;
            rect2.bottom = rect2.top + i5;
        }
    }

    protected final void onDrawScrollBars(Canvas canvas) {
        int i;
        boolean z;
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        if (scrollabilityCache == null || (i = scrollabilityCache.state) == 0) {
            return;
        }
        if (i == 2) {
            if (scrollabilityCache.interpolatorValues == null) {
                scrollabilityCache.interpolatorValues = new float[1];
            }
            float[] fArr = scrollabilityCache.interpolatorValues;
            if (scrollabilityCache.scrollBarInterpolator.timeToValues(fArr) == Interpolator.Result.FREEZE_END) {
                scrollabilityCache.state = 0;
            } else {
                scrollabilityCache.scrollBar.mutate().setAlpha(Math.round(fArr[0]));
            }
            z = true;
        } else {
            scrollabilityCache.scrollBar.mutate().setAlpha(255);
            z = false;
        }
        boolean zIsHorizontalScrollBarEnabled = isHorizontalScrollBarEnabled();
        boolean z2 = isVerticalScrollBarEnabled() && !isVerticalScrollBarHidden();
        if (this.mRoundScrollbarRenderer != null) {
            if (z2) {
                Rect rect = scrollabilityCache.mScrollBarBounds;
                getVerticalScrollBarBounds(rect, null);
                this.mRoundScrollbarRenderer.drawRoundScrollbars(canvas, scrollabilityCache.scrollBar.getAlpha() / 255.0f, rect);
                if (z) {
                    invalidate();
                    return;
                }
                return;
            }
            return;
        }
        if (z2 || zIsHorizontalScrollBarEnabled) {
            ScrollBarDrawable scrollBarDrawable = scrollabilityCache.scrollBar;
            if (zIsHorizontalScrollBarEnabled) {
                scrollBarDrawable.setParameters(computeHorizontalScrollRange(), computeHorizontalScrollOffset(), computeHorizontalScrollExtent(), false);
                Rect rect2 = scrollabilityCache.mScrollBarBounds;
                getHorizontalScrollBarBounds(rect2, null);
                onDrawHorizontalScrollBar(canvas, scrollBarDrawable, rect2.left, rect2.top, rect2.right, rect2.bottom);
                if (z) {
                    invalidate(rect2);
                }
            }
            if (z2) {
                scrollBarDrawable.setParameters(computeVerticalScrollRange(), computeVerticalScrollOffset(), computeVerticalScrollExtent(), true);
                Rect rect3 = scrollabilityCache.mScrollBarBounds;
                getVerticalScrollBarBounds(rect3, null);
                onDrawVerticalScrollBar(canvas, scrollBarDrawable, rect3.left, rect3.top, rect3.right, rect3.bottom);
                if (z) {
                    invalidate(rect3);
                }
            }
        }
    }

    protected boolean isVerticalScrollBarHidden() {
        return false;
    }

    protected void onDrawHorizontalScrollBar(Canvas canvas, Drawable drawable, int i, int i2, int i3, int i4) {
        drawable.setBounds(i, i2, i3, i4);
        drawable.draw(canvas);
    }

    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable drawable, int i, int i2, int i3, int i4) {
        drawable.setBounds(i, i2, i3, i4);
        drawable.draw(canvas);
    }

    protected void onDraw(Canvas canvas) {
    }

    void assignParent(ViewParent viewParent) {
        if (this.mParent == null) {
            this.mParent = viewParent;
            return;
        }
        if (viewParent == null) {
            ViewDebugManager.getInstance().warningParentToNull(this);
            this.mParent = null;
        } else {
            throw new RuntimeException("view " + this + " being added, but it already has a parent");
        }
    }

    protected void onAttachedToWindow() {
        InputMethodManager inputMethodManagerPeekInstance;
        if ((this.mPrivateFlags & 512) != 0) {
            this.mParent.requestTransparentRegion(this);
        }
        this.mPrivateFlags3 &= -5;
        jumpDrawablesToCurrentState();
        resetSubtreeAccessibilityStateChanged();
        rebuildOutline();
        if (isFocused() && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null) {
            inputMethodManagerPeekInstance.focusIn(this);
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

    void dispatchScreenStateChanged(int i) {
        onScreenStateChanged(i);
    }

    public void onScreenStateChanged(int i) {
    }

    void dispatchMovedToDisplay(Display display, Configuration configuration) {
        this.mAttachInfo.mDisplay = display;
        this.mAttachInfo.mDisplayState = display.getState();
        onMovedToDisplay(display.getDisplayId(), configuration);
    }

    public void onMovedToDisplay(int i, Configuration configuration) {
    }

    private boolean hasRtlSupport() {
        return this.mContext.getApplicationInfo().hasRtlSupport();
    }

    private boolean isRtlCompatibilityMode() {
        return getContext().getApplicationInfo().targetSdkVersion < 17 || !hasRtlSupport();
    }

    private boolean needRtlPropertiesResolution() {
        return (this.mPrivateFlags2 & ALL_RTL_PROPERTIES_RESOLVED) != ALL_RTL_PROPERTIES_RESOLVED;
    }

    public void onRtlPropertiesChanged(int i) {
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
        if (getRawLayoutDirection() == 2) {
            if (this.mParent != null) {
                try {
                    return this.mParent.canResolveLayoutDirection();
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    return false;
                }
            }
            return false;
        }
        return true;
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
        int layoutDirection = getLayoutDirection();
        if (!isRtlCompatibilityMode()) {
            if (this.mBackground != null && (!this.mLeftPaddingDefined || !this.mRightPaddingDefined)) {
                Rect rect = sThreadLocal.get();
                if (rect == null) {
                    rect = new Rect();
                    sThreadLocal.set(rect);
                }
                this.mBackground.getPadding(rect);
                if (!this.mLeftPaddingDefined) {
                    this.mUserPaddingLeftInitial = rect.left;
                }
                if (!this.mRightPaddingDefined) {
                    this.mUserPaddingRightInitial = rect.right;
                }
            }
            if (layoutDirection == 1) {
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
            } else {
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
            }
            this.mUserPaddingBottom = this.mUserPaddingBottom >= 0 ? this.mUserPaddingBottom : this.mPaddingBottom;
        }
        internalSetPadding(this.mUserPaddingLeft, this.mPaddingTop, this.mUserPaddingRight, this.mUserPaddingBottom);
        onRtlPropertiesChanged(layoutDirection);
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
        this.mPrivateFlags3 &= -33554433;
        removeUnsetPressCallback();
        removeLongPressCallback();
        removePerformClickCallback();
        cancel(this.mSendViewScrolledAccessibilityEvent);
        stopNestedScroll();
        jumpDrawablesToCurrentState();
        destroyDrawingCache();
        cleanupDraw();
        this.mCurrentAnimation = null;
        if ((this.mViewFlags & 1073741824) == 1073741824) {
            hideTooltip();
        }
    }

    private void cleanupDraw() {
        resetDisplayList();
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mViewRootImpl.cancelInvalidate(this);
        }
    }

    void invalidateInheritedLayoutMode(int i) {
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
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo == null) {
            return null;
        }
        if (attachInfo.mWindowId == null) {
            try {
                attachInfo.mIWindowId = attachInfo.mSession.getWindowId(attachInfo.mWindowToken);
                if (attachInfo.mIWindowId != null) {
                    attachInfo.mWindowId = new WindowId(attachInfo.mIWindowId);
                }
            } catch (RemoteException e) {
            }
        }
        return attachInfo.mWindowId;
    }

    public IBinder getApplicationWindowToken() {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            IBinder iBinder = attachInfo.mPanelParentWindowToken;
            if (iBinder == null) {
                return attachInfo.mWindowToken;
            }
            return iBinder;
        }
        return null;
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

    protected IWindow getWindow() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mWindow;
        }
        return null;
    }

    int combineVisibility(int i, int i2) {
        return Math.max(i, i2);
    }

    void dispatchAttachedToWindow(AttachInfo attachInfo, int i) {
        this.mAttachInfo = attachInfo;
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.d(VIEW_LOG_TAG, "dispatchAttachedToWindow: this = " + this + ", mAttachInfo = " + this.mAttachInfo + ", callstack = ", new Throwable());
        }
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().dispatchAttachedToWindow(attachInfo, i);
        }
        this.mWindowAttachCount++;
        this.mPrivateFlags |= 1024;
        if (this.mFloatingTreeObserver != null) {
            attachInfo.mTreeObserver.merge(this.mFloatingTreeObserver);
            this.mFloatingTreeObserver = null;
        }
        registerPendingFrameMetricsObservers();
        if ((this.mPrivateFlags & 524288) != 0) {
            this.mAttachInfo.mScrollContainers.add(this);
            this.mPrivateFlags |= 1048576;
        }
        if (this.mRunQueue != null) {
            this.mRunQueue.executeActions(attachInfo.mHandler);
            this.mRunQueue = null;
        }
        performCollectViewAttributes(this.mAttachInfo, i);
        onAttachedToWindow();
        ListenerInfo listenerInfo = this.mListenerInfo;
        CopyOnWriteArrayList copyOnWriteArrayList = listenerInfo != null ? listenerInfo.mOnAttachStateChangeListeners : null;
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            Iterator it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                ((OnAttachStateChangeListener) it.next()).onViewAttachedToWindow(this);
            }
        }
        int i2 = attachInfo.mWindowVisibility;
        if (i2 != 8) {
            onWindowVisibilityChanged(i2);
            if (isShown()) {
                onVisibilityAggregated(i2 == 0);
            }
        }
        onVisibilityChanged(this, i);
        if ((this.mPrivateFlags & 1024) != 0) {
            refreshDrawableState();
        }
        needGlobalAttributesUpdate(false);
        notifyEnterOrExitForAutoFillIfNeeded(true);
    }

    void dispatchDetachedFromWindow() {
        CopyOnWriteArrayList copyOnWriteArrayList;
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null && attachInfo.mWindowVisibility != 8) {
            onWindowVisibilityChanged(8);
            if (isShown()) {
                onVisibilityAggregated(false);
            }
        }
        onDetachedFromWindow();
        onDetachedFromWindowInternal();
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            inputMethodManagerPeekInstance.onViewDetachedFromWindow(this);
        }
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null) {
            copyOnWriteArrayList = listenerInfo.mOnAttachStateChangeListeners;
        } else {
            copyOnWriteArrayList = null;
        }
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            Iterator it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                ((OnAttachStateChangeListener) it.next()).onViewDetachedFromWindow(this);
            }
        }
        if ((this.mPrivateFlags & 1048576) != 0) {
            this.mAttachInfo.mScrollContainers.remove(this);
            this.mPrivateFlags &= -1048577;
        }
        if (ViewDebugManager.DEBUG_LIFECYCLE) {
            Log.d(VIEW_LOG_TAG, "dispatchDetachedFromWindow: this = " + this + ", mAttachInfo = " + this.mAttachInfo + " will be null, callstack = ", new Throwable());
        }
        this.mAttachInfo = null;
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().dispatchDetachedFromWindow();
        }
        notifyEnterOrExitForAutoFillIfNeeded(false);
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

    public void saveHierarchyState(SparseArray<Parcelable> sparseArray) {
        dispatchSaveInstanceState(sparseArray);
    }

    protected void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        if (this.mID != -1 && (this.mViewFlags & 65536) == 0) {
            this.mPrivateFlags &= -131073;
            Parcelable parcelableOnSaveInstanceState = onSaveInstanceState();
            if ((this.mPrivateFlags & 131072) == 0) {
                throw new IllegalStateException("Derived class did not call super.onSaveInstanceState()");
            }
            if (parcelableOnSaveInstanceState != null) {
                sparseArray.put(this.mID, parcelableOnSaveInstanceState);
            }
        }
    }

    protected Parcelable onSaveInstanceState() {
        this.mPrivateFlags |= 131072;
        if (this.mStartActivityRequestWho != null || isAutofilled() || this.mAutofillViewId > 1073741823) {
            BaseSavedState baseSavedState = new BaseSavedState(AbsSavedState.EMPTY_STATE);
            if (this.mStartActivityRequestWho != null) {
                baseSavedState.mSavedData |= 1;
            }
            if (isAutofilled()) {
                baseSavedState.mSavedData |= 2;
            }
            if (this.mAutofillViewId > 1073741823) {
                baseSavedState.mSavedData |= 4;
            }
            baseSavedState.mStartActivityRequestWhoSaved = this.mStartActivityRequestWho;
            baseSavedState.mIsAutofilled = isAutofilled();
            baseSavedState.mAutofillViewId = this.mAutofillViewId;
            return baseSavedState;
        }
        return BaseSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(SparseArray<Parcelable> sparseArray) {
        dispatchRestoreInstanceState(sparseArray);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        Parcelable parcelable;
        if (this.mID != -1 && (parcelable = sparseArray.get(this.mID)) != null) {
            this.mPrivateFlags &= -131073;
            onRestoreInstanceState(parcelable);
            if ((this.mPrivateFlags & 131072) == 0) {
                throw new IllegalStateException("Derived class did not call super.onRestoreInstanceState()");
            }
        }
    }

    protected void onRestoreInstanceState(Parcelable parcelable) {
        this.mPrivateFlags |= 131072;
        if (parcelable != null && !(parcelable instanceof AbsSavedState)) {
            throw new IllegalArgumentException("Wrong state class, expecting View State but received " + parcelable.getClass().toString() + " instead. This usually happens when two views of different type have the same id in the same hierarchy. This view's id is " + ViewDebug.resolveId(this.mContext, getId()) + ". Make sure other views do not use the same id.");
        }
        if (parcelable != null && (parcelable instanceof BaseSavedState)) {
            BaseSavedState baseSavedState = (BaseSavedState) parcelable;
            if ((baseSavedState.mSavedData & 1) != 0) {
                this.mStartActivityRequestWho = baseSavedState.mStartActivityRequestWhoSaved;
            }
            if ((baseSavedState.mSavedData & 2) != 0) {
                setAutofilled(baseSavedState.mIsAutofilled);
            }
            if ((baseSavedState.mSavedData & 4) != 0) {
                baseSavedState.mSavedData &= -5;
                if ((this.mPrivateFlags3 & 1073741824) != 0) {
                    if (Helper.sDebug) {
                        Log.d(VIEW_LOG_TAG, "onRestoreInstanceState(): not setting autofillId to " + baseSavedState.mAutofillViewId + " because view explicitly set it to " + this.mAutofillId);
                        return;
                    }
                    return;
                }
                this.mAutofillViewId = baseSavedState.mAutofillViewId;
                this.mAutofillId = null;
            }
        }
    }

    public long getDrawingTime() {
        if (this.mAttachInfo != null) {
            return this.mAttachInfo.mDrawingTime;
        }
        return 0L;
    }

    public void setDuplicateParentStateEnabled(boolean z) {
        setFlags(z ? 4194304 : 0, 4194304);
    }

    public boolean isDuplicateParentStateEnabled() {
        return (this.mViewFlags & 4194304) == 4194304;
    }

    public void setLayerType(int i, Paint paint) {
        if (i < 0 || i > 2) {
            throw new IllegalArgumentException("Layer type can only be one of: LAYER_TYPE_NONE, LAYER_TYPE_SOFTWARE or LAYER_TYPE_HARDWARE");
        }
        if (ViewDebugManager.DEBUG_DRAW) {
            Log.d(VIEW_LOG_TAG, "setLayerType, this =" + this + ", layerType = " + i + ", paint = " + paint, new Throwable());
            i = ViewDebugManager.getInstance().debugForceHWLayer(i);
        }
        if (!this.mRenderNode.setLayerType(i)) {
            setLayerPaint(paint);
            return;
        }
        if (i != 1) {
            destroyDrawingCache();
        }
        this.mLayerType = i;
        if (this.mLayerType == 0) {
            paint = null;
        }
        this.mLayerPaint = paint;
        this.mRenderNode.setLayerPaint(this.mLayerPaint);
        invalidateParentCaches();
        invalidate(true);
    }

    public void setLayerPaint(Paint paint) {
        int layerType = getLayerType();
        if (layerType != 0) {
            this.mLayerPaint = paint;
            if (layerType == 2) {
                if (this.mRenderNode.setLayerPaint(paint)) {
                    invalidateViewProperty(false, false);
                    return;
                }
                return;
            }
            invalidate();
        }
    }

    public int getLayerType() {
        return this.mLayerType;
    }

    public void buildLayer() {
        if (this.mLayerType == 0) {
            return;
        }
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo == null) {
            throw new IllegalStateException("This view must be attached to a window first");
        }
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        switch (this.mLayerType) {
            case 1:
                buildDrawingCache(true);
                return;
            case 2:
                updateDisplayListIfDirty();
                if (attachInfo.mThreadedRenderer != null && this.mRenderNode.isValid()) {
                    attachInfo.mThreadedRenderer.buildLayer(this.mRenderNode);
                    return;
                }
                return;
            default:
                return;
        }
    }

    protected void destroyHardwareResources() {
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().destroyHardwareResources();
        }
        if (this.mGhostView != null) {
            this.mGhostView.destroyHardwareResources();
        }
    }

    @Deprecated
    public void setDrawingCacheEnabled(boolean z) {
        int i = 0;
        this.mCachingFailed = false;
        if (z) {
            i = 32768;
        }
        setFlags(i, 32768);
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    @Deprecated
    public boolean isDrawingCacheEnabled() {
        return (this.mViewFlags & 32768) == 32768;
    }

    public void outputDirtyFlags(String str, boolean z, int i) {
        Log.d(VIEW_LOG_TAG, str + this + "             DIRTY(" + (this.mPrivateFlags & 6291456) + ") DRAWN(" + (this.mPrivateFlags & 32) + ") CACHE_VALID(" + (this.mPrivateFlags & 32768) + ") INVALIDATED(" + (this.mPrivateFlags & Integer.MIN_VALUE) + ")");
        if (z) {
            this.mPrivateFlags &= i;
        }
        if (this instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) this;
            int childCount = viewGroup.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                viewGroup.getChildAt(i2).outputDirtyFlags(str + "  ", z, i);
            }
        }
    }

    protected void dispatchGetDisplayList() {
    }

    public boolean canHaveDisplayList() {
        return (this.mAttachInfo == null || this.mAttachInfo.mThreadedRenderer == null) ? false : true;
    }

    public RenderNode updateDisplayListIfDirty() {
        RenderNode renderNode = this.mRenderNode;
        if (!canHaveDisplayList()) {
            return renderNode;
        }
        if ((this.mPrivateFlags & 32768) == 0 || !renderNode.isValid() || this.mRecreateDisplayList) {
            if (renderNode.isValid() && !this.mRecreateDisplayList) {
                this.mPrivateFlags |= MeshConstants.MESH_MSG_CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_ADD;
                this.mPrivateFlags &= -6291457;
                if (ViewDebugManager.DEBUG_DRAW) {
                    Log.d(VIEW_LOG_TAG, "getDisplayList : do not dirty itself only dispatch getDisplaylist to child,this = " + this);
                }
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceBegin(8L, "HWUI:" + getClass().getName());
                }
                dispatchGetDisplayList();
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceEnd(8L);
                }
                return renderNode;
            }
            this.mRecreateDisplayList = true;
            int i = this.mRight - this.mLeft;
            int i2 = this.mBottom - this.mTop;
            int layerType = getLayerType();
            if (Trace.isTagEnabled(8L)) {
                Trace.traceBegin(8L, "HWUI:" + getClass().getName());
            }
            DisplayListCanvas displayListCanvasStart = renderNode.start(i, i2);
            try {
                if (layerType == 1) {
                    buildDrawingCache(true);
                    Bitmap drawingCache = getDrawingCache(true);
                    if (drawingCache != null) {
                        displayListCanvasStart.drawBitmap(drawingCache, 0.0f, 0.0f, this.mLayerPaint);
                    }
                } else {
                    computeScroll();
                    displayListCanvasStart.translate(-this.mScrollX, -this.mScrollY);
                    this.mPrivateFlags |= MeshConstants.MESH_MSG_CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_ADD;
                    this.mPrivateFlags &= -6291457;
                    if (ViewDebugManager.DEBUG_DRAW) {
                        Log.d(VIEW_LOG_TAG, "getDisplayList : calling dispatchDraw or draw,this =" + this + ", private flags = " + this.mPrivateFlags);
                    }
                    if ((this.mPrivateFlags & 128) == 128) {
                        dispatchDraw(displayListCanvasStart);
                        drawAutofilledHighlight(displayListCanvasStart);
                        if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                            this.mOverlay.getOverlayView().draw(displayListCanvasStart);
                        }
                        if (debugDraw()) {
                            debugDrawFocus(displayListCanvasStart);
                        }
                    } else {
                        draw(displayListCanvasStart);
                    }
                }
                renderNode.end(displayListCanvasStart);
                setDisplayListProperties(renderNode);
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceEnd(8L);
                }
            } catch (Throwable th) {
                renderNode.end(displayListCanvasStart);
                setDisplayListProperties(renderNode);
                throw th;
            }
        } else {
            this.mPrivateFlags |= MeshConstants.MESH_MSG_CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_ADD;
            this.mPrivateFlags &= -6291457;
        }
        if (ViewDebugManager.DEBUG_DRAW) {
            Log.d(VIEW_LOG_TAG, "updateDisplayListIfDirty : return renderNode,this = " + this);
        }
        return renderNode;
    }

    private void resetDisplayList() {
        this.mRenderNode.discardDisplayList();
        if (this.mBackgroundRenderNode != null) {
            this.mBackgroundRenderNode.discardDisplayList();
        }
    }

    @Deprecated
    public Bitmap getDrawingCache() {
        return getDrawingCache(false);
    }

    @Deprecated
    public Bitmap getDrawingCache(boolean z) {
        if ((this.mViewFlags & 131072) == 131072) {
            return null;
        }
        if ((this.mViewFlags & 32768) == 32768) {
            buildDrawingCache(z);
        }
        return z ? this.mDrawingCache : this.mUnscaledDrawingCache;
    }

    @Deprecated
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

    @Deprecated
    public void setDrawingCacheBackgroundColor(int i) {
        if (i != this.mDrawingCacheBackgroundColor) {
            this.mDrawingCacheBackgroundColor = i;
            this.mPrivateFlags &= -32769;
        }
    }

    @Deprecated
    public int getDrawingCacheBackgroundColor() {
        return this.mDrawingCacheBackgroundColor;
    }

    @Deprecated
    public void buildDrawingCache() {
        buildDrawingCache(false);
    }

    @Deprecated
    public void buildDrawingCache(boolean z) {
        if ((this.mPrivateFlags & 32768) != 0) {
            if (z) {
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
            buildDrawingCacheImpl(z);
        } finally {
            Trace.traceEnd(8L);
        }
    }

    private void buildDrawingCacheImpl(boolean z) {
        Bitmap.Config config;
        Canvas canvas;
        this.mCachingFailed = false;
        int i = this.mRight - this.mLeft;
        int i2 = this.mBottom - this.mTop;
        AttachInfo attachInfo = this.mAttachInfo;
        boolean z2 = true;
        boolean z3 = attachInfo != null && attachInfo.mScalingRequired;
        if (z && z3) {
            i = (int) ((i * attachInfo.mApplicationScale) + 0.5f);
            i2 = (int) ((i2 * attachInfo.mApplicationScale) + 0.5f);
        }
        int i3 = this.mDrawingCacheBackgroundColor;
        boolean z4 = i3 != 0 || isOpaque();
        boolean z5 = attachInfo != null && attachInfo.mUse32BitDrawingCache;
        long j = i * i2 * ((!z4 || z5) ? 4 : 2);
        long scaledMaximumDrawingCacheSize = ViewConfiguration.get(this.mContext).getScaledMaximumDrawingCacheSize();
        if (i <= 0 || i2 <= 0 || j > scaledMaximumDrawingCacheSize) {
            if (i > 0 && i2 > 0) {
                Log.w(VIEW_LOG_TAG, getClass().getSimpleName() + " not displayed because it is too large to fit into a software layer (or drawing cache), needs " + j + " bytes, only " + scaledMaximumDrawingCacheSize + " available");
            }
            destroyDrawingCache();
            this.mCachingFailed = true;
            return;
        }
        Bitmap bitmapCreateBitmap = z ? this.mDrawingCache : this.mUnscaledDrawingCache;
        if (bitmapCreateBitmap == null || bitmapCreateBitmap.getWidth() != i || bitmapCreateBitmap.getHeight() != i2) {
            if (!z4) {
                int i4 = this.mViewFlags;
                config = Bitmap.Config.ARGB_8888;
            } else {
                config = z5 ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            }
            if (bitmapCreateBitmap != null) {
                bitmapCreateBitmap.recycle();
            }
            try {
                bitmapCreateBitmap = Bitmap.createBitmap(this.mResources.getDisplayMetrics(), i, i2, config);
                bitmapCreateBitmap.setDensity(getResources().getDisplayMetrics().densityDpi);
                if (z) {
                    this.mDrawingCache = bitmapCreateBitmap;
                } else {
                    this.mUnscaledDrawingCache = bitmapCreateBitmap;
                }
                if (z4 && z5) {
                    bitmapCreateBitmap.setHasAlpha(false);
                }
                if (i3 == 0) {
                    z2 = false;
                }
            } catch (OutOfMemoryError e) {
                if (z) {
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
            canvas.setBitmap(bitmapCreateBitmap);
            attachInfo.mCanvas = null;
        } else {
            canvas = new Canvas(bitmapCreateBitmap);
        }
        if (z2) {
            bitmapCreateBitmap.eraseColor(i3);
        }
        computeScroll();
        int iSave = canvas.save();
        if (z && z3) {
            float f = attachInfo.mApplicationScale;
            canvas.scale(f, f);
        }
        canvas.translate(-this.mScrollX, -this.mScrollY);
        this.mPrivateFlags |= 32;
        if (this.mAttachInfo == null || !this.mAttachInfo.mHardwareAccelerated || this.mLayerType != 0) {
            this.mPrivateFlags |= 32768;
        }
        if (ViewDebugManager.DEBUG_DRAW) {
            Log.d(VIEW_LOG_TAG, "view buildDrawingCache : calling dispatchDraw/draw,this = " + this + ", private flags = " + this.mPrivateFlags);
        }
        if ((this.mPrivateFlags & 128) == 128) {
            this.mPrivateFlags &= -6291457;
            dispatchDraw(canvas);
            drawAutofilledHighlight(canvas);
            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                this.mOverlay.getOverlayView().draw(canvas);
            }
        } else {
            draw(canvas);
        }
        canvas.restoreToCount(iSave);
        canvas.setBitmap(null);
        if (attachInfo != null) {
            attachInfo.mCanvas = canvas;
        }
    }

    public Bitmap createSnapshot(ViewDebug.CanvasProvider canvasProvider, boolean z) throws Throwable {
        int i = this.mRight - this.mLeft;
        int i2 = this.mBottom - this.mTop;
        AttachInfo attachInfo = this.mAttachInfo;
        float f = attachInfo != null ? attachInfo.mApplicationScale : 1.0f;
        int i3 = (int) ((i * f) + 0.5f);
        int i4 = (int) ((i2 * f) + 0.5f);
        if (i3 <= 0) {
            i3 = 1;
        }
        if (i4 <= 0) {
            i4 = 1;
        }
        Canvas canvas = null;
        try {
            Canvas canvas2 = canvasProvider.getCanvas(this, i3, i4);
            if (attachInfo != null) {
                Canvas canvas3 = attachInfo.mCanvas;
                try {
                    attachInfo.mCanvas = null;
                    canvas = canvas3;
                } catch (Throwable th) {
                    th = th;
                    canvas = canvas3;
                    if (canvas != null) {
                        attachInfo.mCanvas = canvas;
                    }
                    throw th;
                }
            }
            computeScroll();
            int iSave = canvas2.save();
            canvas2.scale(f, f);
            canvas2.translate(-this.mScrollX, -this.mScrollY);
            int i5 = this.mPrivateFlags;
            this.mPrivateFlags &= -6291457;
            if ((this.mPrivateFlags & 128) == 128) {
                dispatchDraw(canvas2);
                drawAutofilledHighlight(canvas2);
                if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                    this.mOverlay.getOverlayView().draw(canvas2);
                }
            } else {
                draw(canvas2);
            }
            this.mPrivateFlags = i5;
            canvas2.restoreToCount(iSave);
            Bitmap bitmapCreateBitmap = canvasProvider.createBitmap();
            if (canvas != null) {
                attachInfo.mCanvas = canvas;
            }
            return bitmapCreateBitmap;
        } catch (Throwable th2) {
            th = th2;
        }
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

    protected int getFadeTop(boolean z) {
        int i = this.mPaddingTop;
        return z ? i + getTopPaddingOffset() : i;
    }

    protected int getFadeHeight(boolean z) {
        int topPaddingOffset = this.mPaddingTop;
        if (z) {
            topPaddingOffset += getTopPaddingOffset();
        }
        return ((this.mBottom - this.mTop) - this.mPaddingBottom) - topPaddingOffset;
    }

    @ViewDebug.ExportedProperty(category = "drawing")
    public boolean isHardwareAccelerated() {
        return this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerated;
    }

    public void setClipBounds(Rect rect) {
        if (rect != this.mClipBounds) {
            if (rect != null && rect.equals(this.mClipBounds)) {
                return;
            }
            if (rect != null) {
                if (this.mClipBounds == null) {
                    this.mClipBounds = new Rect(rect);
                } else {
                    this.mClipBounds.set(rect);
                }
            } else {
                this.mClipBounds = null;
            }
            this.mRenderNode.setClipBounds(this.mClipBounds);
            invalidateViewProperty(false, false);
        }
    }

    public Rect getClipBounds() {
        if (this.mClipBounds != null) {
            return new Rect(this.mClipBounds);
        }
        return null;
    }

    public boolean getClipBounds(Rect rect) {
        if (this.mClipBounds != null) {
            rect.set(this.mClipBounds);
            return true;
        }
        return false;
    }

    private boolean applyLegacyAnimation(ViewGroup viewGroup, long j, Animation animation, boolean z) {
        int i = viewGroup.mGroupFlags;
        if (!animation.isInitialized()) {
            animation.initialize(this.mRight - this.mLeft, this.mBottom - this.mTop, viewGroup.getWidth(), viewGroup.getHeight());
            animation.initializeInvalidateRegion(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
            if (this.mAttachInfo != null) {
                animation.setListenerHandler(this.mAttachInfo.mHandler);
            }
            onAnimationStart();
        }
        Transformation childTransformation = viewGroup.getChildTransformation();
        boolean transformation = animation.getTransformation(j, childTransformation, 1.0f);
        if (z && this.mAttachInfo.mApplicationScale != 1.0f) {
            if (viewGroup.mInvalidationTransformation == null) {
                viewGroup.mInvalidationTransformation = new Transformation();
            }
            childTransformation = viewGroup.mInvalidationTransformation;
            animation.getTransformation(j, childTransformation, 1.0f);
        }
        Transformation transformation2 = childTransformation;
        if (transformation) {
            if (!animation.willChangeBounds()) {
                if ((i & 144) == 128) {
                    viewGroup.mGroupFlags |= 4;
                } else if ((i & 4) == 0) {
                    viewGroup.mPrivateFlags |= 64;
                    viewGroup.invalidate(this.mLeft, this.mTop, this.mRight, this.mBottom);
                }
            } else {
                if (viewGroup.mInvalidateRegion == null) {
                    viewGroup.mInvalidateRegion = new RectF();
                }
                RectF rectF = viewGroup.mInvalidateRegion;
                animation.getInvalidateRegion(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop, rectF, transformation2);
                viewGroup.mPrivateFlags |= 64;
                int i2 = this.mLeft + ((int) rectF.left);
                int i3 = this.mTop + ((int) rectF.top);
                viewGroup.invalidate(i2, i3, ((int) (rectF.width() + 0.5f)) + i2, ((int) (rectF.height() + 0.5f)) + i3);
            }
        }
        return transformation;
    }

    void setDisplayListProperties(RenderNode renderNode) {
        boolean z;
        float alpha;
        int transformationType;
        if (renderNode != null) {
            renderNode.setHasOverlappingRendering(getHasOverlappingRendering());
            if ((this.mParent instanceof ViewGroup) && ((ViewGroup) this.mParent).getClipChildren()) {
                z = true;
            } else {
                z = false;
            }
            renderNode.setClipToBounds(z);
            if ((this.mParent instanceof ViewGroup) && (((ViewGroup) this.mParent).mGroupFlags & 2048) != 0) {
                ViewGroup viewGroup = (ViewGroup) this.mParent;
                Transformation childTransformation = viewGroup.getChildTransformation();
                if (viewGroup.getChildStaticTransformation(this, childTransformation) && (transformationType = childTransformation.getTransformationType()) != 0) {
                    if ((transformationType & 1) != 0) {
                        alpha = childTransformation.getAlpha();
                    } else {
                        alpha = 1.0f;
                    }
                    if ((transformationType & 2) != 0) {
                        renderNode.setStaticMatrix(childTransformation.getMatrix());
                    }
                }
            } else {
                alpha = 1.0f;
            }
            if (this.mTransformationInfo == null) {
                if (alpha < 1.0f) {
                    renderNode.setAlpha(alpha);
                }
            } else {
                float finalAlpha = getFinalAlpha() * alpha;
                if (finalAlpha < 1.0f && onSetAlpha((int) (255.0f * finalAlpha))) {
                    finalAlpha = 1.0f;
                }
                renderNode.setAlpha(finalAlpha);
            }
        }
    }

    boolean draw(Canvas canvas, ViewGroup viewGroup, long j) {
        Animation animation;
        RenderNode renderNode;
        Object childTransformation;
        boolean z;
        boolean z2;
        Object obj;
        RenderNode renderNode2;
        int i;
        Bitmap drawingCache;
        RenderNode renderNodeUpdateDisplayListIfDirty;
        int i2;
        int i3;
        Object obj2;
        int iSave;
        Transformation transformation;
        Bitmap bitmap;
        float alpha;
        Animation animation2;
        int i4;
        int i5;
        int i6;
        boolean z3;
        float f;
        int i7;
        int i8;
        RenderNode renderNode3;
        float f2;
        Bitmap bitmap2;
        boolean z4;
        Animation animation3;
        boolean zIsHardwareAccelerated = canvas.isHardwareAccelerated();
        boolean z5 = this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerated && zIsHardwareAccelerated;
        boolean zHasIdentityMatrix = hasIdentityMatrix();
        int i9 = viewGroup.mGroupFlags;
        if ((i9 & 256) != 0) {
            viewGroup.getChildTransformation().clear();
            viewGroup.mGroupFlags &= -257;
        }
        boolean z6 = this.mAttachInfo != null && this.mAttachInfo.mScalingRequired;
        if (ViewDebugManager.DEBUG_DRAW) {
            Log.d(VIEW_LOG_TAG, "view draw1, this =" + this + ", drawingWithRenderNode = " + z5 + ", childHasIdentityMatrix = " + zHasIdentityMatrix + ", scalingRequired = " + z6);
        }
        Animation animation4 = getAnimation();
        if (animation4 != null) {
            animation = animation4;
            boolean zApplyLegacyAnimation = applyLegacyAnimation(viewGroup, j, animation4, z6);
            boolean zWillChangeTransformationMatrix = animation.willChangeTransformationMatrix();
            if (zWillChangeTransformationMatrix) {
                this.mPrivateFlags3 |= 1;
            }
            childTransformation = viewGroup.getChildTransformation();
            z2 = zApplyLegacyAnimation;
            z = zWillChangeTransformationMatrix;
            renderNode = null;
        } else {
            animation = animation4;
            if ((this.mPrivateFlags3 & 1) != 0) {
                renderNode = null;
                this.mRenderNode.setAnimationMatrix(null);
                this.mPrivateFlags3 &= -2;
            } else {
                renderNode = null;
            }
            if (!z5 && (i9 & 2048) != 0) {
                Transformation childTransformation2 = viewGroup.getChildTransformation();
                if (viewGroup.getChildStaticTransformation(this, childTransformation2)) {
                    int transformationType = childTransformation2.getTransformationType();
                    childTransformation = transformationType != 0 ? childTransformation2 : renderNode;
                    z = (transformationType & 2) != 0;
                    z2 = false;
                }
                z2 = false;
            } else {
                childTransformation = renderNode;
                z2 = false;
            }
        }
        boolean z7 = z | (!zHasIdentityMatrix);
        this.mPrivateFlags |= 32;
        if (!z7 && (i9 & 2049) == 1) {
            obj = childTransformation;
            renderNode2 = renderNode;
            if (canvas.quickReject(this.mLeft, this.mTop, this.mRight, this.mBottom, Canvas.EdgeType.BW) && (this.mPrivateFlags & 64) == 0) {
                this.mPrivateFlags2 |= 268435456;
                if (ViewDebugManager.DEBUG_DRAW) {
                    Log.d(VIEW_LOG_TAG, "view draw1 quickReject, this =" + this);
                }
                return z2;
            }
        } else {
            obj = childTransformation;
            renderNode2 = renderNode;
        }
        this.mPrivateFlags2 &= -268435457;
        if (zIsHardwareAccelerated) {
            this.mRecreateDisplayList = (this.mPrivateFlags & Integer.MIN_VALUE) != 0;
            this.mPrivateFlags &= Integer.MAX_VALUE;
        }
        int layerType = getLayerType();
        if (layerType == 1 || !z5) {
            if (layerType != 0) {
                buildDrawingCache(true);
                layerType = 1;
            }
            i = layerType;
            drawingCache = getDrawingCache(true);
        } else {
            i = layerType;
            drawingCache = renderNode2;
        }
        if (z5) {
            renderNodeUpdateDisplayListIfDirty = updateDisplayListIfDirty();
            if (!renderNodeUpdateDisplayListIfDirty.isValid()) {
                renderNodeUpdateDisplayListIfDirty = renderNode2;
                z5 = false;
            }
        } else {
            renderNodeUpdateDisplayListIfDirty = renderNode2;
        }
        if (z5) {
            i2 = 0;
            i3 = 0;
        } else {
            computeScroll();
            i3 = this.mScrollX;
            i2 = this.mScrollY;
        }
        boolean z8 = (drawingCache == null || z5) ? false : true;
        boolean z9 = drawingCache == null && !z5;
        if (!z5) {
            obj2 = obj;
        } else {
            Object obj3 = obj;
            obj2 = obj3;
            if (obj3 == null) {
                iSave = -1;
                transformation = obj3;
            }
            if (!z9) {
                bitmap = drawingCache;
                canvas.translate(this.mLeft - i3, this.mTop - i2);
            } else {
                bitmap = drawingCache;
                if (!z5) {
                    canvas.translate(this.mLeft, this.mTop);
                }
                if (z6) {
                    if (z5) {
                        iSave = canvas.save();
                    }
                    float f3 = 1.0f / this.mAttachInfo.mApplicationScale;
                    canvas.scale(f3, f3);
                }
            }
            int i10 = iSave;
            if (z5) {
                alpha = getAlpha() * getTransitionAlpha();
            } else {
                alpha = 1.0f;
            }
            if (transformation == 0 || alpha < 1.0f || !hasIdentityMatrix() || (this.mPrivateFlags3 & 2) != 0) {
                if (transformation == 0 || !zHasIdentityMatrix) {
                    if (z9) {
                        animation2 = animation;
                        i4 = 0;
                        i5 = 0;
                    } else {
                        animation2 = animation;
                        i5 = -i2;
                        i4 = -i3;
                    }
                    if (transformation == 0) {
                        if (z7) {
                            if (z5) {
                                i6 = i10;
                                renderNodeUpdateDisplayListIfDirty.setAnimationMatrix(transformation.getMatrix());
                                z3 = z6;
                            } else {
                                i6 = i10;
                                z3 = z6;
                                canvas.translate(-i4, -i5);
                                canvas.concat(transformation.getMatrix());
                                canvas.translate(i4, i5);
                            }
                            viewGroup.mGroupFlags |= 256;
                        } else {
                            i6 = i10;
                            z3 = z6;
                        }
                        float alpha2 = transformation.getAlpha();
                        if (alpha2 < 1.0f) {
                            alpha *= alpha2;
                            viewGroup.mGroupFlags |= 256;
                        }
                    } else {
                        i6 = i10;
                        z3 = z6;
                    }
                    if (!zHasIdentityMatrix && !z5) {
                        canvas.translate(-i4, -i5);
                        canvas.concat(getMatrix());
                        canvas.translate(i4, i5);
                    }
                    f = alpha;
                } else {
                    f = alpha;
                    i6 = i10;
                    animation2 = animation;
                    z3 = z6;
                }
                if (f >= 1.0f || (this.mPrivateFlags3 & 2) != 0) {
                    if (f >= 1.0f) {
                        this.mPrivateFlags3 |= 2;
                    } else {
                        this.mPrivateFlags3 &= -3;
                    }
                    viewGroup.mGroupFlags |= 256;
                    if (z8) {
                        int i11 = (int) (255.0f * f);
                        if (!onSetAlpha(i11)) {
                            if (z5) {
                                renderNodeUpdateDisplayListIfDirty.setAlpha(getAlpha() * f * getTransitionAlpha());
                            } else if (i == 0) {
                                f2 = f;
                                z4 = z8;
                                i7 = i2;
                                i8 = i3;
                                renderNode3 = renderNodeUpdateDisplayListIfDirty;
                                bitmap2 = bitmap;
                                canvas.saveLayerAlpha(i3, i2, getWidth() + i3, getHeight() + i2, i11);
                            }
                            i7 = i2;
                            i8 = i3;
                            renderNode3 = renderNodeUpdateDisplayListIfDirty;
                            f2 = f;
                            bitmap2 = bitmap;
                            z4 = z8;
                        } else {
                            i7 = i2;
                            i8 = i3;
                            renderNode3 = renderNodeUpdateDisplayListIfDirty;
                            f2 = f;
                            bitmap2 = bitmap;
                            z4 = z8;
                            this.mPrivateFlags |= 262144;
                        }
                    } else {
                        i7 = i2;
                        i8 = i3;
                        renderNode3 = renderNodeUpdateDisplayListIfDirty;
                        f2 = f;
                        bitmap2 = bitmap;
                        z4 = z8;
                    }
                }
            } else {
                if ((this.mPrivateFlags & 262144) == 262144) {
                    onSetAlpha(255);
                    this.mPrivateFlags &= -262145;
                }
                z4 = z8;
                renderNode3 = renderNodeUpdateDisplayListIfDirty;
                f2 = alpha;
                i6 = i10;
                animation2 = animation;
                z3 = z6;
                bitmap2 = bitmap;
                i7 = i2;
                i8 = i3;
            }
            if (!z5) {
                if ((i9 & 1) != 0 && bitmap2 == null) {
                    if (z9) {
                        canvas.clipRect(i8, i7, i8 + getWidth(), i7 + getHeight());
                    } else if (!z3 || bitmap2 == null) {
                        canvas.clipRect(0, 0, getWidth(), getHeight());
                    } else {
                        canvas.clipRect(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
                    }
                }
                if (this.mClipBounds != null) {
                    canvas.clipRect(this.mClipBounds);
                }
            }
            if (ViewDebugManager.DEBUG_DRAW) {
                StringBuilder sb = new StringBuilder();
                sb.append("view draw1 : calling dispatchDraw,this = ");
                sb.append(this);
                sb.append(", with cache = ");
                sb.append(z4);
                sb.append(", with render node = ");
                sb.append(z5);
                sb.append(", cache=");
                sb.append(bitmap2 == null ? "Null" : "withCache");
                sb.append(", private flags = ");
                sb.append(this.mPrivateFlags);
                Log.d(VIEW_LOG_TAG, sb.toString());
            }
            if (z4) {
                if (z5) {
                    this.mPrivateFlags = (-6291457) & this.mPrivateFlags;
                    ((DisplayListCanvas) canvas).drawRenderNode(renderNode3);
                } else if ((this.mPrivateFlags & 128) == 128) {
                    this.mPrivateFlags = (-6291457) & this.mPrivateFlags;
                    dispatchDraw(canvas);
                } else {
                    draw(canvas);
                }
            } else if (bitmap2 != null) {
                this.mPrivateFlags = (-6291457) & this.mPrivateFlags;
                if (i == 0 || this.mLayerPaint == null) {
                    Paint paint = viewGroup.mCachePaint;
                    if (paint == null) {
                        paint = new Paint();
                        paint.setDither(false);
                        viewGroup.mCachePaint = paint;
                    }
                    paint.setAlpha((int) (f2 * 255.0f));
                    canvas.drawBitmap(bitmap2, 0.0f, 0.0f, paint);
                } else {
                    int alpha3 = this.mLayerPaint.getAlpha();
                    if (f2 < 1.0f) {
                        this.mLayerPaint.setAlpha((int) (alpha3 * f2));
                    }
                    canvas.drawBitmap(bitmap2, 0.0f, 0.0f, this.mLayerPaint);
                    if (f2 < 1.0f) {
                        this.mLayerPaint.setAlpha(alpha3);
                    }
                }
            }
            if (i6 >= 0) {
                canvas.restoreToCount(i6);
            }
            if (animation2 == null && !z2) {
                if (zIsHardwareAccelerated) {
                    animation3 = animation2;
                } else {
                    animation3 = animation2;
                    if (!animation3.getFillAfter()) {
                        onSetAlpha(255);
                    }
                }
                viewGroup.finishAnimatingView(this, animation3);
            } else {
                animation3 = animation2;
            }
            if (z2 && zIsHardwareAccelerated && animation3.hasAlpha() && (this.mPrivateFlags & 262144) == 262144) {
                invalidate(true);
            }
            this.mRecreateDisplayList = false;
            return z2;
        }
        iSave = canvas.save();
        transformation = obj2;
        if (!z9) {
        }
        int i102 = iSave;
        if (z5) {
        }
        if (transformation == 0) {
            if (transformation == 0) {
                if (z9) {
                }
                if (transformation == 0) {
                }
                if (!zHasIdentityMatrix) {
                    canvas.translate(-i4, -i5);
                    canvas.concat(getMatrix());
                    canvas.translate(i4, i5);
                }
                f = alpha;
                if (f >= 1.0f) {
                    if (f >= 1.0f) {
                    }
                    viewGroup.mGroupFlags |= 256;
                    if (z8) {
                    }
                }
            }
        }
        if (!z5) {
        }
        if (ViewDebugManager.DEBUG_DRAW) {
        }
        if (z4) {
        }
        if (i6 >= 0) {
        }
        if (animation2 == null) {
            animation3 = animation2;
        }
        if (z2) {
            invalidate(true);
        }
        this.mRecreateDisplayList = false;
        return z2;
    }

    static Paint getDebugPaint() {
        if (sDebugPaint == null) {
            sDebugPaint = new Paint();
            sDebugPaint.setAntiAlias(false);
        }
        return sDebugPaint;
    }

    final int dipsToPixels(int i) {
        return (int) ((i * getContext().getResources().getDisplayMetrics().density) + 0.5f);
    }

    private final void debugDrawFocus(Canvas canvas) {
        if (isFocused()) {
            int iDipsToPixels = dipsToPixels(8);
            int i = this.mScrollX;
            int i2 = (this.mRight + i) - this.mLeft;
            int i3 = this.mScrollY;
            int i4 = (this.mBottom + i3) - this.mTop;
            Paint debugPaint = getDebugPaint();
            debugPaint.setColor(DEBUG_CORNERS_COLOR);
            debugPaint.setStyle(Paint.Style.FILL);
            float f = i;
            float f2 = i3;
            float f3 = i + iDipsToPixels;
            float f4 = i3 + iDipsToPixels;
            canvas.drawRect(f, f2, f3, f4, debugPaint);
            float f5 = i2 - iDipsToPixels;
            float f6 = i2;
            canvas.drawRect(f5, f2, f6, f4, debugPaint);
            float f7 = i4 - iDipsToPixels;
            float f8 = i4;
            canvas.drawRect(f, f7, f3, f8, debugPaint);
            canvas.drawRect(f5, f7, f6, f8, debugPaint);
            debugPaint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(f, f2, f6, f8, debugPaint);
            canvas.drawLine(f, f8, f6, f2, debugPaint);
        }
    }

    public void draw(Canvas canvas) {
        float fMax;
        float fMax2;
        boolean z;
        boolean z2;
        float f;
        float f2;
        boolean z3;
        boolean z4;
        int i;
        Matrix matrix;
        Paint paint;
        Shader shader;
        int i2;
        int i3;
        float f3;
        Paint paint2;
        Shader shader2;
        int i4;
        int i5 = this.mPrivateFlags;
        boolean z5 = (6291456 & i5) == 4194304 && (this.mAttachInfo == null || !this.mAttachInfo.mIgnoreDirtyState);
        this.mPrivateFlags = (i5 & (-6291457)) | 32;
        if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
            Trace.traceBegin(8L, "draw:" + getClass().getName());
        }
        if (!z5) {
            drawBackground(canvas);
        }
        int i6 = this.mViewFlags;
        boolean z6 = (i6 & 4096) != 0;
        boolean z7 = (i6 & 8192) != 0;
        if (!z7 && !z6) {
            if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
                Trace.traceBegin(8L, "onDraw:" + getClass().getName());
            }
            if (!z5) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                onDraw(canvas);
                ViewDebugManager.getInstance().debugOnDrawDone(this, jCurrentTimeMillis);
            }
            if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
                Trace.traceEnd(8L);
            }
            dispatchDraw(canvas);
            drawAutofilledHighlight(canvas);
            if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
                this.mOverlay.getOverlayView().dispatchDraw(canvas);
            }
            onDrawForeground(canvas);
            drawDefaultFocusHighlight(canvas);
            if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
                Trace.traceEnd(8L);
            }
            if (debugDraw()) {
                debugDrawFocus(canvas);
                return;
            }
            return;
        }
        int leftPaddingOffset = this.mPaddingLeft;
        boolean zIsPaddingOffsetRequired = isPaddingOffsetRequired();
        if (zIsPaddingOffsetRequired) {
            leftPaddingOffset += getLeftPaddingOffset();
        }
        int i7 = this.mScrollX + leftPaddingOffset;
        int rightPaddingOffset = (((this.mRight + i7) - this.mLeft) - this.mPaddingRight) - leftPaddingOffset;
        int fadeTop = getFadeTop(zIsPaddingOffsetRequired) + this.mScrollY;
        int fadeHeight = getFadeHeight(zIsPaddingOffsetRequired) + fadeTop;
        if (zIsPaddingOffsetRequired) {
            rightPaddingOffset += getRightPaddingOffset();
            fadeHeight += getBottomPaddingOffset();
        }
        int i8 = fadeHeight;
        ScrollabilityCache scrollabilityCache = this.mScrollCache;
        float f4 = scrollabilityCache.fadingEdgeLength;
        int i9 = (int) f4;
        if (z7 && fadeTop + i9 > i8 - i9) {
            i9 = (i8 - fadeTop) / 2;
        }
        if (z6 && i7 + i9 > rightPaddingOffset - i9) {
            i9 = (rightPaddingOffset - i7) / 2;
        }
        if (z7) {
            fMax = Math.max(0.0f, Math.min(1.0f, getTopFadingEdgeStrength()));
            z2 = fMax * f4 > 1.0f;
            fMax2 = Math.max(0.0f, Math.min(1.0f, getBottomFadingEdgeStrength()));
            z = fMax2 * f4 > 1.0f;
        } else {
            fMax = 0.0f;
            fMax2 = 0.0f;
            z = false;
            z2 = false;
        }
        if (z6) {
            float fMax3 = Math.max(0.0f, Math.min(1.0f, getLeftFadingEdgeStrength()));
            boolean z8 = fMax3 * f4 > 1.0f;
            f2 = fMax3;
            float fMax4 = Math.max(0.0f, Math.min(1.0f, getRightFadingEdgeStrength()));
            z3 = fMax4 * f4 > 1.0f;
            z4 = z8;
            f = fMax4;
        } else {
            f = 0.0f;
            f2 = 0.0f;
            z3 = false;
            z4 = false;
        }
        int saveCount = canvas.getSaveCount();
        int solidColor = getSolidColor();
        if (solidColor == 0) {
            if (z2) {
                canvas.saveUnclippedLayer(i7, fadeTop, rightPaddingOffset, fadeTop + i9);
            }
            if (z) {
                canvas.saveUnclippedLayer(i7, i8 - i9, rightPaddingOffset, i8);
            }
            if (z4) {
                canvas.saveUnclippedLayer(i7, fadeTop, i7 + i9, i8);
            }
            if (z3) {
                canvas.saveUnclippedLayer(rightPaddingOffset - i9, fadeTop, rightPaddingOffset, i8);
            }
        } else {
            scrollabilityCache.setFadeColor(solidColor);
        }
        if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
            i = saveCount;
            Trace.traceBegin(8L, "onDraw:" + getClass().getName());
        } else {
            i = saveCount;
        }
        if (!z5) {
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            onDraw(canvas);
            ViewDebugManager.getInstance().debugOnDrawDone(this, jCurrentTimeMillis2);
        }
        if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
            Trace.traceEnd(8L);
        }
        dispatchDraw(canvas);
        Paint paint3 = scrollabilityCache.paint;
        Matrix matrix2 = scrollabilityCache.matrix;
        Shader shader3 = scrollabilityCache.shader;
        if (z2) {
            matrix2.setScale(1.0f, fMax * f4);
            float f5 = i7;
            float f6 = fadeTop;
            matrix2.postTranslate(f5, f6);
            shader3.setLocalMatrix(matrix2);
            paint3.setShader(shader3);
            float f7 = fadeTop + i9;
            i2 = fadeTop;
            matrix = matrix2;
            f3 = 1.0f;
            paint = paint3;
            i3 = i;
            shader = shader3;
            canvas.drawRect(f5, f6, rightPaddingOffset, f7, paint);
        } else {
            matrix = matrix2;
            paint = paint3;
            shader = shader3;
            i2 = fadeTop;
            i3 = i;
            f3 = 1.0f;
        }
        if (z) {
            matrix.setScale(f3, fMax2 * f4);
            matrix.postRotate(180.0f);
            float f8 = i7;
            float f9 = i8;
            matrix.postTranslate(f8, f9);
            shader2 = shader;
            shader2.setLocalMatrix(matrix);
            paint2 = paint;
            paint2.setShader(shader2);
            canvas.drawRect(f8, i8 - i9, rightPaddingOffset, f9, paint2);
        } else {
            paint2 = paint;
            shader2 = shader;
        }
        if (z4) {
            matrix.setScale(f3, f4 * f2);
            matrix.postRotate(-90.0f);
            float f10 = i7;
            int i10 = i2;
            float f11 = i10;
            matrix.postTranslate(f10, f11);
            shader2.setLocalMatrix(matrix);
            paint2.setShader(shader2);
            i4 = i10;
            canvas.drawRect(f10, f11, i7 + i9, i8, paint2);
        } else {
            i4 = i2;
        }
        if (z3) {
            matrix.setScale(f3, f4 * f);
            matrix.postRotate(90.0f);
            float f12 = rightPaddingOffset;
            float f13 = i4;
            matrix.postTranslate(f12, f13);
            shader2.setLocalMatrix(matrix);
            paint2.setShader(shader2);
            canvas.drawRect(rightPaddingOffset - i9, f13, f12, i8, paint2);
        }
        canvas.restoreToCount(i3);
        drawAutofilledHighlight(canvas);
        if (this.mOverlay != null && !this.mOverlay.isEmpty()) {
            this.mOverlay.getOverlayView().dispatchDraw(canvas);
        }
        onDrawForeground(canvas);
        if (debugDraw()) {
            debugDrawFocus(canvas);
        }
        if (ViewDebugManager.DEBUG_SYSTRACE_DRAW) {
            Trace.traceEnd(8L);
        }
    }

    private void drawBackground(Canvas canvas) {
        Drawable drawable = this.mBackground;
        if (drawable == null) {
            return;
        }
        setBackgroundBounds();
        if (canvas.isHardwareAccelerated() && this.mAttachInfo != null && this.mAttachInfo.mThreadedRenderer != null) {
            this.mBackgroundRenderNode = getDrawableRenderNode(drawable, this.mBackgroundRenderNode);
            RenderNode renderNode = this.mBackgroundRenderNode;
            if (renderNode != null && renderNode.isValid()) {
                setBackgroundRenderNodeProperties(renderNode);
                ((DisplayListCanvas) canvas).drawRenderNode(renderNode);
                return;
            }
        }
        int i = this.mScrollX;
        int i2 = this.mScrollY;
        if ((i | i2) == 0) {
            drawable.draw(canvas);
            return;
        }
        canvas.translate(i, i2);
        drawable.draw(canvas);
        canvas.translate(-i, -i2);
    }

    void setBackgroundBounds() {
        if (this.mBackgroundSizeChanged && this.mBackground != null) {
            this.mBackground.setBounds(0, 0, this.mRight - this.mLeft, this.mBottom - this.mTop);
            this.mBackgroundSizeChanged = false;
            rebuildOutline();
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
        DisplayListCanvas displayListCanvasStart = renderNode.start(bounds.width(), bounds.height());
        displayListCanvasStart.translate(-bounds.left, -bounds.top);
        try {
            drawable.draw(displayListCanvasStart);
            renderNode.end(displayListCanvasStart);
            renderNode.setLeftTopRightBottom(bounds.left, bounds.top, bounds.right, bounds.bottom);
            renderNode.setProjectBackwards(drawable.isProjected());
            renderNode.setProjectionReceiver(true);
            renderNode.setClipToBounds(false);
            return renderNode;
        } catch (Throwable th) {
            renderNode.end(displayListCanvasStart);
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

    private static String printFlags(int i) {
        String str = "";
        char c = 1;
        if ((i & 1) == 1) {
            str = "TAKES_FOCUS";
        } else {
            c = 0;
        }
        int i2 = i & 12;
        if (i2 == 4) {
            if (c > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            return str + "INVISIBLE";
        }
        if (i2 == 8) {
            if (c > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            return str + "GONE";
        }
        return str;
    }

    private static String printPrivateFlags(int i) {
        String str = "";
        int i2 = 1;
        if ((i & 1) == 1) {
            str = "WANTS_FOCUS";
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            if (i2 > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            str = str + "FOCUSED";
            i2++;
        }
        if ((i & 4) == 4) {
            if (i2 > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            str = str + "SELECTED";
            i2++;
        }
        if ((i & 8) == 8) {
            if (i2 > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            str = str + "IS_ROOT_NAMESPACE";
            i2++;
        }
        if ((i & 16) == 16) {
            if (i2 > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            str = str + "HAS_BOUNDS";
            i2++;
        }
        if ((i & 32) == 32) {
            if (i2 > 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            return str + "DRAWN";
        }
        return str;
    }

    public boolean isLayoutRequested() {
        return (this.mPrivateFlags & 4096) == 4096;
    }

    public static boolean isLayoutModeOptical(Object obj) {
        return (obj instanceof ViewGroup) && ((ViewGroup) obj).isLayoutModeOptical();
    }

    private boolean setOpticalFrame(int i, int i2, int i3, int i4) {
        Insets opticalInsets = this.mParent instanceof View ? ((View) this.mParent).getOpticalInsets() : Insets.NONE;
        Insets opticalInsets2 = getOpticalInsets();
        return setFrame((i + opticalInsets.left) - opticalInsets2.left, (i2 + opticalInsets.top) - opticalInsets2.top, i3 + opticalInsets.left + opticalInsets2.right, i4 + opticalInsets.top + opticalInsets2.bottom);
    }

    public void layout(int i, int i2, int i3, int i4) {
        boolean z;
        if (ViewDebugManager.DEBUG_SYSTRACE_LAYOUT) {
            Trace.traceBegin(8L, "layout : " + getClass().getSimpleName());
        }
        if ((this.mPrivateFlags3 & 8) != 0) {
            long jDebugOnMeasureStart = ViewDebugManager.getInstance().debugOnMeasureStart(this, this.mOldWidthMeasureSpec, this.mOldWidthMeasureSpec, this.mOldWidthMeasureSpec, this.mOldHeightMeasureSpec);
            onMeasure(this.mOldWidthMeasureSpec, this.mOldHeightMeasureSpec);
            ViewDebugManager.getInstance().debugOnMeasureEnd(this, jDebugOnMeasureStart);
            this.mPrivateFlags3 &= -9;
        }
        int i5 = this.mLeft;
        int i6 = this.mTop;
        int i7 = this.mBottom;
        int i8 = this.mRight;
        boolean opticalFrame = isLayoutModeOptical(this.mParent) ? setOpticalFrame(i, i2, i3, i4) : setFrame(i, i2, i3, i4);
        boolean z2 = false;
        Object obj = null;
        if (opticalFrame || (this.mPrivateFlags & 8192) == 8192) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            onLayout(opticalFrame, i, i2, i3, i4);
            ViewDebugManager.getInstance().debugOnLayoutEnd(this, jCurrentTimeMillis);
            if (shouldDrawRoundScrollbar()) {
                if (this.mRoundScrollbarRenderer == null) {
                    this.mRoundScrollbarRenderer = new RoundScrollbarRenderer(this);
                }
            } else {
                this.mRoundScrollbarRenderer = null;
            }
            this.mPrivateFlags &= -8193;
            ListenerInfo listenerInfo = this.mListenerInfo;
            if (listenerInfo != null && listenerInfo.mOnLayoutChangeListeners != null) {
                ArrayList arrayList = (ArrayList) listenerInfo.mOnLayoutChangeListeners.clone();
                int size = arrayList.size();
                int i9 = 0;
                while (i9 < size) {
                    ((OnLayoutChangeListener) arrayList.get(i9)).onLayoutChange(this, i, i2, i3, i4, i5, i6, i8, i7);
                    i9++;
                    z2 = z2;
                    arrayList = arrayList;
                    i8 = i8;
                    obj = null;
                }
            }
            z = z2;
        } else {
            if (ViewDebugManager.DEBUG_LAYOUT) {
                Log.d(VIEW_LOG_TAG, "view layout end 2 (use previous layout), this = " + this);
            }
            z = false;
        }
        boolean zIsLayoutValid = isLayoutValid();
        this.mPrivateFlags &= -4097;
        this.mPrivateFlags3 |= 4;
        if (ViewDebugManager.DEBUG_SYSTRACE_LAYOUT) {
            Trace.traceEnd(8L);
        }
        if (zIsLayoutValid || !isFocused()) {
            if ((this.mPrivateFlags & 1) != 0) {
                this.mPrivateFlags &= -2;
                View viewFindFocus = findFocus();
                if (viewFindFocus != null && !restoreDefaultFocus() && !hasParentWantsFocus()) {
                    viewFindFocus.clearFocusInternal(null, true, z);
                }
            }
        } else {
            this.mPrivateFlags &= -2;
            if (canTakeFocus()) {
                clearParentsWantFocus();
            } else if (getViewRootImpl() == null || !getViewRootImpl().isInLayout()) {
                clearFocusInternal(null, true, z);
                clearParentsWantFocus();
            } else if (!hasParentWantsFocus()) {
                clearFocusInternal(null, true, z);
            }
        }
        if ((this.mPrivateFlags3 & 134217728) != 0) {
            this.mPrivateFlags3 &= -134217729;
            notifyEnterOrExitForAutoFillIfNeeded(true);
        }
    }

    private boolean hasParentWantsFocus() {
        ViewParent viewParent = this.mParent;
        while (viewParent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) viewParent;
            if ((viewGroup.mPrivateFlags & 1) != 0) {
                return true;
            }
            viewParent = viewGroup.mParent;
        }
        return false;
    }

    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
    }

    protected boolean setFrame(int i, int i2, int i3, int i4) {
        if (ViewDebugManager.DEBUG_LAYOUT) {
            Log.d(VIEW_LOG_TAG, this + " View.setFrame(" + i + "," + i2 + "," + i3 + "," + i4 + ")");
        }
        if (this.mLeft == i && this.mRight == i3 && this.mTop == i2 && this.mBottom == i4) {
            return false;
        }
        int i5 = this.mPrivateFlags & 32;
        int i6 = this.mRight - this.mLeft;
        int i7 = this.mBottom - this.mTop;
        int i8 = i3 - i;
        int i9 = i4 - i2;
        boolean z = (i8 == i6 && i9 == i7) ? false : true;
        invalidate(z);
        this.mLeft = i;
        this.mTop = i2;
        this.mRight = i3;
        this.mBottom = i4;
        this.mRenderNode.setLeftTopRightBottom(this.mLeft, this.mTop, this.mRight, this.mBottom);
        this.mPrivateFlags |= 16;
        if (z) {
            sizeChange(i8, i9, i6, i7);
        }
        if ((this.mViewFlags & 12) == 0 || this.mGhostView != null) {
            this.mPrivateFlags |= 32;
            invalidate(z);
            invalidateParentCaches();
        }
        this.mPrivateFlags |= i5;
        this.mBackgroundSizeChanged = true;
        this.mDefaultFocusHighlightSizeChanged = true;
        if (this.mForegroundInfo != null) {
            this.mForegroundInfo.mBoundsChanged = true;
        }
        notifySubtreeAccessibilityStateChangedIfNeeded();
        return true;
    }

    public void setLeftTopRightBottom(int i, int i2, int i3, int i4) {
        setFrame(i, i2, i3, i4);
    }

    private void sizeChange(int i, int i2, int i3, int i4) {
        onSizeChanged(i, i2, i3, i4);
        if (this.mOverlay != null) {
            this.mOverlay.getOverlayView().setRight(i);
            this.mOverlay.getOverlayView().setBottom(i2);
        }
        if (!sCanFocusZeroSized && isLayoutValid() && (!(this.mParent instanceof ViewGroup) || !((ViewGroup) this.mParent).isLayoutSuppressed())) {
            if (i <= 0 || i2 <= 0) {
                if (hasFocus()) {
                    clearFocus();
                    if (this.mParent instanceof ViewGroup) {
                        ((ViewGroup) this.mParent).clearFocusedInCluster();
                    }
                }
                clearAccessibilityFocus();
            } else if ((i3 <= 0 || i4 <= 0) && this.mParent != null && canTakeFocus()) {
                this.mParent.focusableViewAvailable(this);
            }
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
            Rect dirtyBounds = drawable.getDirtyBounds();
            int i = this.mScrollX;
            int i2 = this.mScrollY;
            invalidate(dirtyBounds.left + i, dirtyBounds.top + i2, dirtyBounds.right + i, dirtyBounds.bottom + i2);
            rebuildOutline();
        }
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
        if (verifyDrawable(drawable) && runnable != null) {
            long jUptimeMillis = j - SystemClock.uptimeMillis();
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mViewRootImpl.mChoreographer.postCallbackDelayed(1, runnable, drawable, Choreographer.subtractFrameDelay(jUptimeMillis));
            } else {
                getRunQueue().postDelayed(runnable, jUptimeMillis);
            }
        }
    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
        if (verifyDrawable(drawable) && runnable != null) {
            if (this.mAttachInfo != null) {
                this.mAttachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, runnable, drawable);
            }
            getRunQueue().removeCallbacks(runnable);
        }
    }

    public void unscheduleDrawable(Drawable drawable) {
        if (this.mAttachInfo != null && drawable != null) {
            this.mAttachInfo.mViewRootImpl.mChoreographer.removeCallbacks(1, null, drawable);
        }
    }

    protected void resolveDrawables() {
        if (!isLayoutDirectionResolved() && getRawLayoutDirection() == 2) {
            return;
        }
        int layoutDirection = isLayoutDirectionResolved() ? getLayoutDirection() : getRawLayoutDirection();
        if (this.mBackground != null) {
            this.mBackground.setLayoutDirection(layoutDirection);
        }
        if (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null) {
            this.mForegroundInfo.mDrawable.setLayoutDirection(layoutDirection);
        }
        if (this.mDefaultFocusHighlight != null) {
            this.mDefaultFocusHighlight.setLayoutDirection(layoutDirection);
        }
        this.mPrivateFlags2 |= 1073741824;
        onResolveDrawables(layoutDirection);
    }

    boolean areDrawablesResolved() {
        return (this.mPrivateFlags2 & 1073741824) == 1073741824;
    }

    public void onResolveDrawables(int i) {
    }

    protected void resetResolvedDrawables() {
        resetResolvedDrawablesInternal();
    }

    void resetResolvedDrawablesInternal() {
        this.mPrivateFlags2 &= -1073741825;
    }

    protected boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mBackground || (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable == drawable) || this.mDefaultFocusHighlight == drawable;
    }

    protected void drawableStateChanged() {
        boolean state;
        ScrollBarDrawable scrollBarDrawable;
        int[] drawableState = getDrawableState();
        Drawable drawable = this.mBackground;
        boolean z = false;
        if (drawable != null && drawable.isStateful()) {
            state = drawable.setState(drawableState) | false;
        } else {
            state = false;
        }
        Drawable drawable2 = this.mDefaultFocusHighlight;
        if (drawable2 != null && drawable2.isStateful()) {
            state |= drawable2.setState(drawableState);
        }
        Drawable drawable3 = this.mForegroundInfo != null ? this.mForegroundInfo.mDrawable : null;
        if (drawable3 != null && drawable3.isStateful()) {
            state |= drawable3.setState(drawableState);
        }
        if (this.mScrollCache != null && (scrollBarDrawable = this.mScrollCache.scrollBar) != null && scrollBarDrawable.isStateful()) {
            if (scrollBarDrawable.setState(drawableState) && this.mScrollCache.state != 0) {
                z = true;
            }
            state |= z;
        }
        if (this.mStateListAnimator != null) {
            this.mStateListAnimator.setState(drawableState);
        }
        if (state) {
            invalidate();
        }
    }

    public void drawableHotspotChanged(float f, float f2) {
        if (this.mBackground != null) {
            this.mBackground.setHotspot(f, f2);
        }
        if (this.mDefaultFocusHighlight != null) {
            this.mDefaultFocusHighlight.setHotspot(f, f2);
        }
        if (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null) {
            this.mForegroundInfo.mDrawable.setHotspot(f, f2);
        }
        dispatchDrawableHotspotChanged(f, f2);
    }

    public void dispatchDrawableHotspotChanged(float f, float f2) {
    }

    public void refreshDrawableState() {
        this.mPrivateFlags |= 1024;
        drawableStateChanged();
        ViewParent viewParent = this.mParent;
        if (viewParent != null) {
            viewParent.childDrawableStateChanged(this);
        }
    }

    private Drawable getDefaultFocusHighlightDrawable() {
        if (this.mDefaultFocusHighlightCache == null && this.mContext != null) {
            TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{16843534});
            this.mDefaultFocusHighlightCache = typedArrayObtainStyledAttributes.getDrawable(0);
            typedArrayObtainStyledAttributes.recycle();
        }
        return this.mDefaultFocusHighlightCache;
    }

    private void setDefaultFocusHighlight(Drawable drawable) {
        this.mDefaultFocusHighlight = drawable;
        this.mDefaultFocusHighlightSizeChanged = true;
        if (drawable != null) {
            if ((this.mPrivateFlags & 128) != 0) {
                this.mPrivateFlags &= -129;
            }
            drawable.setLayoutDirection(getLayoutDirection());
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
            if (isAttachedToWindow()) {
                drawable.setVisible(getWindowVisibility() == 0 && isShown(), false);
            }
            drawable.setCallback(this);
        } else if ((this.mViewFlags & 128) != 0 && this.mBackground == null && (this.mForegroundInfo == null || this.mForegroundInfo.mDrawable == null)) {
            this.mPrivateFlags |= 128;
        }
        invalidate();
    }

    public boolean isDefaultFocusHighlightNeeded(Drawable drawable, Drawable drawable2) {
        return !isInTouchMode() && getDefaultFocusHighlightEnabled() && ((drawable == null || !drawable.isStateful() || !drawable.hasFocusStateSpecified()) && (drawable2 == null || !drawable2.isStateful() || !drawable2.hasFocusStateSpecified())) && isAttachedToWindow() && sUseDefaultFocusHighlight;
    }

    private void switchDefaultFocusHighlight() {
        Drawable drawable;
        if (isFocused()) {
            Drawable drawable2 = this.mBackground;
            if (this.mForegroundInfo != null) {
                drawable = this.mForegroundInfo.mDrawable;
            } else {
                drawable = null;
            }
            boolean zIsDefaultFocusHighlightNeeded = isDefaultFocusHighlightNeeded(drawable2, drawable);
            boolean z = this.mDefaultFocusHighlight != null;
            if (zIsDefaultFocusHighlightNeeded && !z) {
                setDefaultFocusHighlight(getDefaultFocusHighlightDrawable());
            } else if (!zIsDefaultFocusHighlightNeeded && z) {
                setDefaultFocusHighlight(null);
            }
        }
    }

    private void drawDefaultFocusHighlight(Canvas canvas) {
        if (this.mDefaultFocusHighlight != null) {
            if (this.mDefaultFocusHighlightSizeChanged) {
                this.mDefaultFocusHighlightSizeChanged = false;
                int i = this.mScrollX;
                int i2 = (this.mRight + i) - this.mLeft;
                int i3 = this.mScrollY;
                this.mDefaultFocusHighlight.setBounds(i, i3, i2, (this.mBottom + i3) - this.mTop);
            }
            this.mDefaultFocusHighlight.draw(canvas);
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

    protected int[] onCreateDrawableState(int i) {
        if ((this.mViewFlags & 4194304) == 4194304 && (this.mParent instanceof View)) {
            return ((View) this.mParent).onCreateDrawableState(i);
        }
        int i2 = this.mPrivateFlags;
        int i3 = (i2 & 16384) != 0 ? 16 : 0;
        if ((this.mViewFlags & 32) == 0) {
            i3 |= 8;
        }
        if (isFocused()) {
            i3 |= 4;
        }
        if ((i2 & 4) != 0) {
            i3 |= 2;
        }
        if (hasWindowFocus()) {
            i3 |= 1;
        }
        if ((1073741824 & i2) != 0) {
            i3 |= 32;
        }
        if (this.mAttachInfo != null && this.mAttachInfo.mHardwareAccelerationRequested && ThreadedRenderer.isAvailable()) {
            i3 |= 64;
        }
        if ((i2 & 268435456) != 0) {
            i3 |= 128;
        }
        int i4 = this.mPrivateFlags2;
        if ((i4 & 1) != 0) {
            i3 |= 256;
        }
        if ((i4 & 2) != 0) {
            i3 |= 512;
        }
        int[] iArr = StateSet.get(i3);
        if (i == 0) {
            return iArr;
        }
        if (iArr != null) {
            int[] iArr2 = new int[iArr.length + i];
            System.arraycopy(iArr, 0, iArr2, 0, iArr.length);
            return iArr2;
        }
        return new int[i];
    }

    protected static int[] mergeDrawableStates(int[] iArr, int[] iArr2) {
        int length = iArr.length - 1;
        while (length >= 0 && iArr[length] == 0) {
            length--;
        }
        System.arraycopy(iArr2, 0, iArr, length + 1, iArr2.length);
        return iArr;
    }

    public void jumpDrawablesToCurrentState() {
        if (this.mBackground != null) {
            this.mBackground.jumpToCurrentState();
        }
        if (this.mStateListAnimator != null) {
            this.mStateListAnimator.jumpToCurrentState();
        }
        if (this.mDefaultFocusHighlight != null) {
            this.mDefaultFocusHighlight.jumpToCurrentState();
        }
        if (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null) {
            this.mForegroundInfo.mDrawable.jumpToCurrentState();
        }
    }

    @RemotableViewMethod
    public void setBackgroundColor(int i) {
        if (this.mBackground instanceof ColorDrawable) {
            ((ColorDrawable) this.mBackground.mutate()).setColor(i);
            computeOpaqueFlags();
            this.mBackgroundResource = 0;
            return;
        }
        setBackground(new ColorDrawable(i));
    }

    @RemotableViewMethod
    public void setBackgroundResource(int i) {
        if (i != 0 && i == this.mBackgroundResource) {
            return;
        }
        Drawable drawable = null;
        if (i != 0) {
            drawable = this.mContext.getDrawable(i);
        }
        setBackground(drawable);
        this.mBackgroundResource = i;
    }

    public void setBackground(Drawable drawable) {
        setBackgroundDrawable(drawable);
    }

    @Deprecated
    public void setBackgroundDrawable(Drawable drawable) {
        boolean z;
        boolean z2;
        computeOpaqueFlags();
        if (drawable == this.mBackground) {
            return;
        }
        this.mBackgroundResource = 0;
        if (this.mBackground != null) {
            if (isAttachedToWindow()) {
                this.mBackground.setVisible(false, false);
            }
            this.mBackground.setCallback(null);
            unscheduleDrawable(this.mBackground);
        }
        if (drawable != null) {
            Rect rect = sThreadLocal.get();
            if (rect == null) {
                rect = new Rect();
                sThreadLocal.set(rect);
            }
            resetResolvedDrawablesInternal();
            drawable.setLayoutDirection(getLayoutDirection());
            if (drawable.getPadding(rect)) {
                resetResolvedPaddingInternal();
                if (drawable.getLayoutDirection() == 1) {
                    this.mUserPaddingLeftInitial = rect.right;
                    this.mUserPaddingRightInitial = rect.left;
                    internalSetPadding(rect.right, rect.top, rect.left, rect.bottom);
                } else {
                    this.mUserPaddingLeftInitial = rect.left;
                    this.mUserPaddingRightInitial = rect.right;
                    internalSetPadding(rect.left, rect.top, rect.right, rect.bottom);
                }
                this.mLeftPaddingDefined = false;
                this.mRightPaddingDefined = false;
            }
            z = (this.mBackground != null && this.mBackground.getMinimumHeight() == drawable.getMinimumHeight() && this.mBackground.getMinimumWidth() == drawable.getMinimumWidth()) ? false : true;
            this.mBackground = drawable;
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
            if (isAttachedToWindow()) {
                if (getWindowVisibility() != 0 || !isShown()) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                drawable.setVisible(z2, false);
            }
            applyBackgroundTint();
            drawable.setCallback(this);
            if ((this.mPrivateFlags & 128) != 0) {
                this.mPrivateFlags &= -129;
                z = true;
            }
        } else {
            this.mBackground = null;
            if ((this.mViewFlags & 128) != 0 && this.mDefaultFocusHighlight == null && (this.mForegroundInfo == null || this.mForegroundInfo.mDrawable == null)) {
                this.mPrivateFlags |= 128;
            }
            z = true;
        }
        computeOpaqueFlags();
        if (z) {
            requestLayout();
        }
        this.mBackgroundSizeChanged = true;
        invalidate(true);
        invalidateOutline();
    }

    public Drawable getBackground() {
        return this.mBackground;
    }

    public void setBackgroundTintList(ColorStateList colorStateList) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintList = colorStateList;
        this.mBackgroundTint.mHasTintList = true;
        applyBackgroundTint();
    }

    public ColorStateList getBackgroundTintList() {
        if (this.mBackgroundTint != null) {
            return this.mBackgroundTint.mTintList;
        }
        return null;
    }

    public void setBackgroundTintMode(PorterDuff.Mode mode) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintMode = mode;
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

    public Drawable getForeground() {
        if (this.mForegroundInfo != null) {
            return this.mForegroundInfo.mDrawable;
        }
        return null;
    }

    public void setForeground(Drawable drawable) {
        if (this.mForegroundInfo == null) {
            if (drawable == null) {
                return;
            } else {
                this.mForegroundInfo = new ForegroundInfo();
            }
        }
        if (drawable != this.mForegroundInfo.mDrawable) {
            if (this.mForegroundInfo.mDrawable != null) {
                if (isAttachedToWindow()) {
                    this.mForegroundInfo.mDrawable.setVisible(false, false);
                }
                this.mForegroundInfo.mDrawable.setCallback(null);
                unscheduleDrawable(this.mForegroundInfo.mDrawable);
            }
            this.mForegroundInfo.mDrawable = drawable;
            this.mForegroundInfo.mBoundsChanged = true;
            if (drawable != null) {
                if ((this.mPrivateFlags & 128) != 0) {
                    this.mPrivateFlags &= -129;
                }
                drawable.setLayoutDirection(getLayoutDirection());
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                applyForegroundTint();
                if (isAttachedToWindow()) {
                    drawable.setVisible(getWindowVisibility() == 0 && isShown(), false);
                }
                drawable.setCallback(this);
            } else if ((this.mViewFlags & 128) != 0 && this.mBackground == null && this.mDefaultFocusHighlight == null) {
                this.mPrivateFlags |= 128;
            }
            requestLayout();
            invalidate();
        }
    }

    public boolean isForegroundInsidePadding() {
        if (this.mForegroundInfo != null) {
            return this.mForegroundInfo.mInsidePadding;
        }
        return true;
    }

    public int getForegroundGravity() {
        if (this.mForegroundInfo != null) {
            return this.mForegroundInfo.mGravity;
        }
        return 8388659;
    }

    public void setForegroundGravity(int i) {
        if (this.mForegroundInfo == null) {
            this.mForegroundInfo = new ForegroundInfo();
        }
        if (this.mForegroundInfo.mGravity != i) {
            if ((8388615 & i) == 0) {
                i |= Gravity.START;
            }
            if ((i & 112) == 0) {
                i |= 48;
            }
            this.mForegroundInfo.mGravity = i;
            requestLayout();
        }
    }

    public void setForegroundTintList(ColorStateList colorStateList) {
        if (this.mForegroundInfo == null) {
            this.mForegroundInfo = new ForegroundInfo();
        }
        if (this.mForegroundInfo.mTintInfo == null) {
            this.mForegroundInfo.mTintInfo = new TintInfo();
        }
        this.mForegroundInfo.mTintInfo.mTintList = colorStateList;
        this.mForegroundInfo.mTintInfo.mHasTintList = true;
        applyForegroundTint();
    }

    public ColorStateList getForegroundTintList() {
        if (this.mForegroundInfo == null || this.mForegroundInfo.mTintInfo == null) {
            return null;
        }
        return this.mForegroundInfo.mTintInfo.mTintList;
    }

    public void setForegroundTintMode(PorterDuff.Mode mode) {
        if (this.mForegroundInfo == null) {
            this.mForegroundInfo = new ForegroundInfo();
        }
        if (this.mForegroundInfo.mTintInfo == null) {
            this.mForegroundInfo.mTintInfo = new TintInfo();
        }
        this.mForegroundInfo.mTintInfo.mTintMode = mode;
        this.mForegroundInfo.mTintInfo.mHasTintMode = true;
        applyForegroundTint();
    }

    public PorterDuff.Mode getForegroundTintMode() {
        if (this.mForegroundInfo == null || this.mForegroundInfo.mTintInfo == null) {
            return null;
        }
        return this.mForegroundInfo.mTintInfo.mTintMode;
    }

    private void applyForegroundTint() {
        if (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null && this.mForegroundInfo.mTintInfo != null) {
            TintInfo tintInfo = this.mForegroundInfo.mTintInfo;
            if (tintInfo.mHasTintList || tintInfo.mHasTintMode) {
                this.mForegroundInfo.mDrawable = this.mForegroundInfo.mDrawable.mutate();
                if (tintInfo.mHasTintList) {
                    this.mForegroundInfo.mDrawable.setTintList(tintInfo.mTintList);
                }
                if (tintInfo.mHasTintMode) {
                    this.mForegroundInfo.mDrawable.setTintMode(tintInfo.mTintMode);
                }
                if (this.mForegroundInfo.mDrawable.isStateful()) {
                    this.mForegroundInfo.mDrawable.setState(getDrawableState());
                }
            }
        }
    }

    private Drawable getAutofilledDrawable() {
        if (this.mAttachInfo == null) {
            return null;
        }
        if (this.mAttachInfo.mAutofilledDrawable == null) {
            Context context = getRootView().getContext();
            TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(AUTOFILL_HIGHLIGHT_ATTR);
            int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
            this.mAttachInfo.mAutofilledDrawable = context.getDrawable(resourceId);
            typedArrayObtainStyledAttributes.recycle();
        }
        return this.mAttachInfo.mAutofilledDrawable;
    }

    private void drawAutofilledHighlight(Canvas canvas) {
        Drawable autofilledDrawable;
        if (isAutofilled() && (autofilledDrawable = getAutofilledDrawable()) != null) {
            autofilledDrawable.setBounds(0, 0, getWidth(), getHeight());
            autofilledDrawable.draw(canvas);
        }
    }

    public void onDrawForeground(Canvas canvas) {
        onDrawScrollIndicators(canvas);
        onDrawScrollBars(canvas);
        Drawable drawable = this.mForegroundInfo != null ? this.mForegroundInfo.mDrawable : null;
        if (drawable != null) {
            if (this.mForegroundInfo.mBoundsChanged) {
                this.mForegroundInfo.mBoundsChanged = false;
                Rect rect = this.mForegroundInfo.mSelfBounds;
                Rect rect2 = this.mForegroundInfo.mOverlayBounds;
                if (this.mForegroundInfo.mInsidePadding) {
                    rect.set(0, 0, getWidth(), getHeight());
                } else {
                    rect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
                }
                Gravity.apply(this.mForegroundInfo.mGravity, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), rect, rect2, getLayoutDirection());
                drawable.setBounds(rect2);
            }
            drawable.draw(canvas);
        }
    }

    public void setPadding(int i, int i2, int i3, int i4) {
        resetResolvedPaddingInternal();
        this.mUserPaddingStart = Integer.MIN_VALUE;
        this.mUserPaddingEnd = Integer.MIN_VALUE;
        this.mUserPaddingLeftInitial = i;
        this.mUserPaddingRightInitial = i3;
        this.mLeftPaddingDefined = true;
        this.mRightPaddingDefined = true;
        internalSetPadding(i, i2, i3, i4);
    }

    protected void internalSetPadding(int i, int i2, int i3, int i4) {
        int verticalScrollbarWidth;
        this.mUserPaddingLeft = i;
        this.mUserPaddingRight = i3;
        this.mUserPaddingBottom = i4;
        int i5 = this.mViewFlags;
        boolean z = false;
        if ((i5 & 768) != 0) {
            if ((i5 & 512) != 0) {
                if ((i5 & 16777216) != 0) {
                    verticalScrollbarWidth = getVerticalScrollbarWidth();
                } else {
                    verticalScrollbarWidth = 0;
                }
                switch (this.mVerticalScrollbarPosition) {
                    case 0:
                        if (isLayoutRtl()) {
                            i += verticalScrollbarWidth;
                        } else {
                            i3 += verticalScrollbarWidth;
                        }
                        break;
                    case 1:
                        i += verticalScrollbarWidth;
                        break;
                    case 2:
                        i3 += verticalScrollbarWidth;
                        break;
                }
            }
            if ((i5 & 256) != 0) {
                i4 += (i5 & 16777216) == 0 ? 0 : getHorizontalScrollbarHeight();
            }
        }
        boolean z2 = true;
        if (this.mPaddingLeft != i) {
            this.mPaddingLeft = i;
            z = true;
        }
        if (this.mPaddingTop != i2) {
            this.mPaddingTop = i2;
            z = true;
        }
        if (this.mPaddingRight != i3) {
            this.mPaddingRight = i3;
            z = true;
        }
        if (this.mPaddingBottom != i4) {
            this.mPaddingBottom = i4;
        } else {
            z2 = z;
        }
        if (z2) {
            requestLayout();
            invalidateOutline();
        }
    }

    public void setPaddingRelative(int i, int i2, int i3, int i4) {
        resetResolvedPaddingInternal();
        this.mUserPaddingStart = i;
        this.mUserPaddingEnd = i3;
        this.mLeftPaddingDefined = true;
        this.mRightPaddingDefined = true;
        if (getLayoutDirection() == 1) {
            this.mUserPaddingLeftInitial = i3;
            this.mUserPaddingRightInitial = i;
            internalSetPadding(i3, i2, i, i4);
        } else {
            this.mUserPaddingLeftInitial = i;
            this.mUserPaddingRightInitial = i3;
            internalSetPadding(i, i2, i3, i4);
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

    public void setSelected(boolean z) {
        if (((this.mPrivateFlags & 4) != 0) != z) {
            this.mPrivateFlags = (this.mPrivateFlags & (-5)) | (z ? 4 : 0);
            if (!z) {
                resetPressedState();
            }
            invalidate(true);
            refreshDrawableState();
            dispatchSetSelected(z);
            if (z) {
                sendAccessibilityEvent(4);
            } else {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    protected void dispatchSetSelected(boolean z) {
    }

    @ViewDebug.ExportedProperty
    public boolean isSelected() {
        return (this.mPrivateFlags & 4) != 0;
    }

    public void setActivated(boolean z) {
        if (((this.mPrivateFlags & 1073741824) != 0) != z) {
            this.mPrivateFlags = (this.mPrivateFlags & (-1073741825)) | (z ? 1073741824 : 0);
            invalidate(true);
            refreshDrawableState();
            dispatchSetActivated(z);
        }
    }

    protected void dispatchSetActivated(boolean z) {
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
            this.mFloatingTreeObserver = new ViewTreeObserver(this.mContext);
        }
        return this.mFloatingTreeObserver;
    }

    public View getRootView() {
        View view;
        if (this.mAttachInfo != null && (view = this.mAttachInfo.mRootView) != null) {
            return view;
        }
        View view2 = this;
        while (view2.mParent != null && (view2.mParent instanceof View)) {
            view2 = (View) view2.mParent;
        }
        return view2;
    }

    public boolean toGlobalMotionEvent(MotionEvent motionEvent) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo == null) {
            return false;
        }
        Matrix matrix = attachInfo.mTmpMatrix;
        matrix.set(Matrix.IDENTITY_MATRIX);
        transformMatrixToGlobal(matrix);
        motionEvent.transform(matrix);
        return true;
    }

    public boolean toLocalMotionEvent(MotionEvent motionEvent) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo == null) {
            return false;
        }
        Matrix matrix = attachInfo.mTmpMatrix;
        matrix.set(Matrix.IDENTITY_MATRIX);
        transformMatrixToLocal(matrix);
        motionEvent.transform(matrix);
        return true;
    }

    public void transformMatrixToGlobal(Matrix matrix) {
        Object obj = this.mParent;
        if (obj instanceof View) {
            ((View) obj).transformMatrixToGlobal(matrix);
            matrix.preTranslate(-r0.mScrollX, -r0.mScrollY);
        } else if (obj instanceof ViewRootImpl) {
            ((ViewRootImpl) obj).transformMatrixToGlobal(matrix);
            matrix.preTranslate(0.0f, -r0.mCurScrollY);
        }
        matrix.preTranslate(this.mLeft, this.mTop);
        if (!hasIdentityMatrix()) {
            matrix.preConcat(getMatrix());
        }
    }

    public void transformMatrixToLocal(Matrix matrix) {
        Object obj = this.mParent;
        if (obj instanceof View) {
            ((View) obj).transformMatrixToLocal(matrix);
            matrix.postTranslate(r0.mScrollX, r0.mScrollY);
        } else if (obj instanceof ViewRootImpl) {
            ((ViewRootImpl) obj).transformMatrixToLocal(matrix);
            matrix.postTranslate(0.0f, r0.mCurScrollY);
        }
        matrix.postTranslate(-this.mLeft, -this.mTop);
        if (!hasIdentityMatrix()) {
            matrix.postConcat(getInverseMatrix());
        }
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, indexMapping = {@ViewDebug.IntToString(from = 0, to = "x"), @ViewDebug.IntToString(from = 1, to = "y")})
    public int[] getLocationOnScreen() {
        int[] iArr = new int[2];
        getLocationOnScreen(iArr);
        return iArr;
    }

    public void getLocationOnScreen(int[] iArr) {
        getLocationInWindow(iArr);
        AttachInfo attachInfo = this.mAttachInfo;
        if (attachInfo != null) {
            iArr[0] = iArr[0] + attachInfo.mWindowLeft;
            iArr[1] = iArr[1] + attachInfo.mWindowTop;
        }
    }

    public void getLocationInWindow(int[] iArr) {
        if (iArr == null || iArr.length < 2) {
            throw new IllegalArgumentException("outLocation must be an array of two integers");
        }
        iArr[0] = 0;
        iArr[1] = 0;
        transformFromViewToWindowSpace(iArr);
    }

    public void transformFromViewToWindowSpace(int[] iArr) {
        if (iArr == null || iArr.length < 2) {
            throw new IllegalArgumentException("inOutLocation must be an array of two integers");
        }
        if (this.mAttachInfo == null) {
            iArr[1] = 0;
            iArr[0] = 0;
            return;
        }
        float[] fArr = this.mAttachInfo.mTmpTransformLocation;
        fArr[0] = iArr[0];
        fArr[1] = iArr[1];
        if (!hasIdentityMatrix()) {
            getMatrix().mapPoints(fArr);
        }
        fArr[0] = fArr[0] + this.mLeft;
        fArr[1] = fArr[1] + this.mTop;
        Object obj = this.mParent;
        while (obj instanceof View) {
            View view = (View) obj;
            fArr[0] = fArr[0] - view.mScrollX;
            fArr[1] = fArr[1] - view.mScrollY;
            if (!view.hasIdentityMatrix()) {
                view.getMatrix().mapPoints(fArr);
            }
            fArr[0] = fArr[0] + view.mLeft;
            fArr[1] = fArr[1] + view.mTop;
            obj = view.mParent;
        }
        if (obj instanceof ViewRootImpl) {
            fArr[1] = fArr[1] - ((ViewRootImpl) obj).mCurScrollY;
        }
        iArr[0] = Math.round(fArr[0]);
        iArr[1] = Math.round(fArr[1]);
    }

    protected <T extends View> T findViewTraversal(int i) {
        if (i == this.mID) {
            return this;
        }
        return null;
    }

    protected <T extends View> T findViewWithTagTraversal(Object obj) {
        if (obj != null && obj.equals(this.mTag)) {
            return this;
        }
        return null;
    }

    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View view) {
        if (predicate.test(this)) {
            return this;
        }
        return null;
    }

    public final <T extends View> T findViewById(int i) {
        if (i == -1) {
            return null;
        }
        return (T) findViewTraversal(i);
    }

    public final <T extends View> T requireViewById(int i) {
        T t = (T) findViewById(i);
        if (t == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return t;
    }

    final <T extends View> T findViewByAccessibilityId(int i) {
        T t;
        if (i >= 0 && (t = (T) findViewByAccessibilityIdTraversal(i)) != null && t.includeForAccessibility()) {
            return t;
        }
        return null;
    }

    public <T extends View> T findViewByAccessibilityIdTraversal(int i) {
        if (getAccessibilityViewId() == i) {
            return this;
        }
        return null;
    }

    public <T extends View> T findViewByAutofillIdTraversal(int i) {
        if (getAutofillViewId() == i) {
            return this;
        }
        return null;
    }

    public final <T extends View> T findViewWithTag(Object obj) {
        if (obj == null) {
            return null;
        }
        return (T) findViewWithTagTraversal(obj);
    }

    public final <T extends View> T findViewByPredicate(Predicate<View> predicate) {
        return (T) findViewByPredicateTraversal(predicate, null);
    }

    public final <T extends View> T findViewByPredicateInsideOut(View view, Predicate<View> predicate) {
        View view2 = null;
        while (true) {
            T t = (T) view.findViewByPredicateTraversal(predicate, view2);
            if (t != null || view == this) {
                break;
            }
            Object parent = view.getParent();
            if (parent == null || !(parent instanceof View)) {
                break;
            }
            View view3 = (View) parent;
            view2 = view;
            view = view3;
        }
        return null;
    }

    public void setId(int i) {
        this.mID = i;
        if (this.mID == -1 && this.mLabelForId != -1) {
            this.mID = generateViewId();
        }
    }

    public void setIsRootNamespace(boolean z) {
        if (z) {
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

    public void setTag(Object obj) {
        this.mTag = obj;
    }

    public Object getTag(int i) {
        if (this.mKeyedTags != null) {
            return this.mKeyedTags.get(i);
        }
        return null;
    }

    public void setTag(int i, Object obj) {
        if ((i >>> 24) < 2) {
            throw new IllegalArgumentException("The key must be an application-specific resource id.");
        }
        setKeyedTag(i, obj);
    }

    public void setTagInternal(int i, Object obj) {
        if ((i >>> 24) != 1) {
            throw new IllegalArgumentException("The key must be a framework-specific resource id.");
        }
        setKeyedTag(i, obj);
    }

    private void setKeyedTag(int i, Object obj) {
        if (this.mKeyedTags == null) {
            this.mKeyedTags = new SparseArray<>(2);
        }
        this.mKeyedTags.put(i, obj);
    }

    public void debug() {
        debug(0);
    }

    protected void debug(int i) {
        String strDebug;
        String str = debugIndent(i - 1) + "+ " + this;
        int id = getId();
        if (id != -1) {
            str = str + " (id=" + id + ")";
        }
        Object tag = getTag();
        if (tag != null) {
            str = str + " (tag=" + tag + ")";
        }
        Log.d(VIEW_LOG_TAG, str);
        if ((this.mPrivateFlags & 2) != 0) {
            Log.d(VIEW_LOG_TAG, debugIndent(i) + " FOCUSED");
        }
        Log.d(VIEW_LOG_TAG, debugIndent(i) + "frame={" + this.mLeft + ", " + this.mTop + ", " + this.mRight + ", " + this.mBottom + "} scroll={" + this.mScrollX + ", " + this.mScrollY + "} ");
        if (this.mPaddingLeft != 0 || this.mPaddingTop != 0 || this.mPaddingRight != 0 || this.mPaddingBottom != 0) {
            Log.d(VIEW_LOG_TAG, debugIndent(i) + "padding={" + this.mPaddingLeft + ", " + this.mPaddingTop + ", " + this.mPaddingRight + ", " + this.mPaddingBottom + "}");
        }
        Log.d(VIEW_LOG_TAG, debugIndent(i) + "mMeasureWidth=" + this.mMeasuredWidth + " mMeasureHeight=" + this.mMeasuredHeight);
        String strDebugIndent = debugIndent(i);
        if (this.mLayoutParams == null) {
            strDebug = strDebugIndent + "BAD! no layout params";
        } else {
            strDebug = this.mLayoutParams.debug(strDebugIndent);
        }
        Log.d(VIEW_LOG_TAG, strDebug);
        Log.d(VIEW_LOG_TAG, ((debugIndent(i) + "flags={") + printFlags(this.mViewFlags)) + "}");
        Log.d(VIEW_LOG_TAG, ((debugIndent(i) + "privateFlags={") + printPrivateFlags(this.mPrivateFlags)) + "}");
    }

    protected static String debugIndent(int i) {
        int i2 = (i * 2) + 3;
        StringBuilder sb = new StringBuilder(i2 * 2);
        for (int i3 = 0; i3 < i2; i3++) {
            sb.append(' ');
            sb.append(' ');
        }
        return sb.toString();
    }

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public int getBaseline() {
        return -1;
    }

    public boolean isInLayout() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        return viewRootImpl != null && viewRootImpl.isInLayout();
    }

    public void requestLayout() {
        if (this.mMeasureCache != null) {
            this.mMeasureCache.clear();
        }
        if (this.mAttachInfo != null && this.mAttachInfo.mViewRequestingLayout == null) {
            ViewRootImpl viewRootImpl = getViewRootImpl();
            if (viewRootImpl != null && viewRootImpl.isInLayout() && !viewRootImpl.requestLayoutDuringLayout(this)) {
                return;
            } else {
                this.mAttachInfo.mViewRequestingLayout = this;
            }
        }
        this.mPrivateFlags |= 4096;
        this.mPrivateFlags |= Integer.MIN_VALUE;
        if (ViewDebugManager.DEBUG_LAYOUT) {
            Log.d(VIEW_LOG_TAG, "view request layout, this =" + this);
        }
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

    public final void measure(int i, int i2) {
        int iAdjust;
        int iAdjust2;
        if (ViewDebugManager.DEBUG_SYSTRACE_MEASURE) {
            Trace.traceBegin(8L, "measure : " + getClass().getSimpleName());
        }
        boolean zIsLayoutModeOptical = isLayoutModeOptical(this);
        if (zIsLayoutModeOptical != isLayoutModeOptical(this.mParent)) {
            Insets opticalInsets = getOpticalInsets();
            int i3 = opticalInsets.left + opticalInsets.right;
            int i4 = opticalInsets.top + opticalInsets.bottom;
            if (zIsLayoutModeOptical) {
                i3 = -i3;
            }
            iAdjust = MeasureSpec.adjust(i, i3);
            if (zIsLayoutModeOptical) {
                i4 = -i4;
            }
            iAdjust2 = MeasureSpec.adjust(i2, i4);
        } else {
            iAdjust = i;
            iAdjust2 = i2;
        }
        int i5 = iAdjust2;
        int i6 = iAdjust;
        ViewDebugManager.getInstance().debugOnMeasureStart(this, i6, i5, this.mOldWidthMeasureSpec, this.mOldHeightMeasureSpec);
        long j = (((long) i6) << 32) | (((long) i5) & 4294967295L);
        if (this.mMeasureCache == null) {
            this.mMeasureCache = new LongSparseLongArray(2);
        }
        boolean z = true;
        boolean z2 = (this.mPrivateFlags & 4096) == 4096;
        boolean z3 = (i6 == this.mOldWidthMeasureSpec && i5 == this.mOldHeightMeasureSpec) ? false : true;
        boolean z4 = MeasureSpec.getMode(i6) == 1073741824 && MeasureSpec.getMode(i5) == 1073741824;
        boolean z5 = getMeasuredWidth() == MeasureSpec.getSize(i6) && getMeasuredHeight() == MeasureSpec.getSize(i5);
        if (!z3 || (!sAlwaysRemeasureExactly && z4 && z5)) {
            z = false;
        }
        if (z2 || z) {
            this.mPrivateFlags &= -2049;
            resolveRtlPropertiesIfNeeded();
            int iIndexOfKey = z2 ? -1 : this.mMeasureCache.indexOfKey(j);
            if (iIndexOfKey < 0 || sIgnoreMeasureCache) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                onMeasure(i6, i5);
                ViewDebugManager.getInstance().debugOnMeasureEnd(this, jCurrentTimeMillis);
                this.mPrivateFlags3 &= -9;
            } else {
                long jValueAt = this.mMeasureCache.valueAt(iIndexOfKey);
                setMeasuredDimensionRaw((int) (jValueAt >> 32), (int) jValueAt);
                this.mPrivateFlags3 |= 8;
            }
            if ((this.mPrivateFlags & 2048) != 2048) {
                throw new IllegalStateException("View with id " + getId() + ": " + getClass().getName() + "#onMeasure() did not set the measured dimension by calling setMeasuredDimension()");
            }
            this.mPrivateFlags |= 8192;
        }
        this.mOldWidthMeasureSpec = i6;
        this.mOldHeightMeasureSpec = i5;
        this.mMeasureCache.put(j, (((long) this.mMeasuredWidth) << 32) | (((long) this.mMeasuredHeight) & 4294967295L));
        if (ViewDebugManager.DEBUG_SYSTRACE_MEASURE) {
            Trace.traceEnd(8L);
        }
    }

    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), i), getDefaultSize(getSuggestedMinimumHeight(), i2));
    }

    protected final void setMeasuredDimension(int i, int i2) {
        boolean zIsLayoutModeOptical = isLayoutModeOptical(this);
        if (zIsLayoutModeOptical != isLayoutModeOptical(this.mParent)) {
            Insets opticalInsets = getOpticalInsets();
            int i3 = opticalInsets.left + opticalInsets.right;
            int i4 = opticalInsets.top + opticalInsets.bottom;
            if (!zIsLayoutModeOptical) {
                i3 = -i3;
            }
            i += i3;
            if (!zIsLayoutModeOptical) {
                i4 = -i4;
            }
            i2 += i4;
        }
        setMeasuredDimensionRaw(i, i2);
    }

    private void setMeasuredDimensionRaw(int i, int i2) {
        this.mMeasuredWidth = i;
        this.mMeasuredHeight = i2;
        this.mPrivateFlags |= 2048;
    }

    public static int combineMeasuredStates(int i, int i2) {
        return i | i2;
    }

    public static int resolveSize(int i, int i2) {
        return resolveSizeAndState(i, i2, 0) & 16777215;
    }

    public static int resolveSizeAndState(int i, int i2, int i3) {
        int mode = MeasureSpec.getMode(i2);
        int size = MeasureSpec.getSize(i2);
        if (mode != Integer.MIN_VALUE) {
            if (mode == 1073741824) {
                i = size;
            }
        } else if (size < i) {
            i = 16777216 | size;
        }
        return i | ((-16777216) & i3);
    }

    public static int getDefaultSize(int i, int i2) {
        int mode = MeasureSpec.getMode(i2);
        return (mode == Integer.MIN_VALUE || (mode != 0 && mode == 1073741824)) ? MeasureSpec.getSize(i2) : i;
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

    @RemotableViewMethod
    public void setMinimumHeight(int i) {
        this.mMinHeight = i;
        requestLayout();
    }

    public int getMinimumWidth() {
        return this.mMinWidth;
    }

    public void setMinimumWidth(int i) {
        this.mMinWidth = i;
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

    protected boolean onSetAlpha(int i) {
        return false;
    }

    public boolean gatherTransparentRegion(Region region) {
        AttachInfo attachInfo = this.mAttachInfo;
        if (region != null && attachInfo != null) {
            if ((this.mPrivateFlags & 128) == 0) {
                int[] iArr = attachInfo.mTransparentLocation;
                getLocationInWindow(iArr);
                int z = getZ() > 0.0f ? (int) getZ() : 0;
                if (ViewDebugManager.DBG_TRANSP) {
                    Log.d(VIEW_LOG_TAG, "gatherTransparentRegion by view this=" + this + " region=" + region + ", location0=" + iArr[0] + ", location1=" + iArr[1] + ", shadowOffset=" + z);
                }
                region.op(iArr[0] - z, iArr[1] - z, ((iArr[0] + this.mRight) - this.mLeft) + z, ((iArr[1] + this.mBottom) - this.mTop) + (z * 3), Region.Op.DIFFERENCE);
            } else {
                if (this.mBackground != null && this.mBackground.getOpacity() != -2) {
                    applyDrawableToTransparentRegion(this.mBackground, region);
                }
                if (this.mForegroundInfo != null && this.mForegroundInfo.mDrawable != null && this.mForegroundInfo.mDrawable.getOpacity() != -2) {
                    applyDrawableToTransparentRegion(this.mForegroundInfo.mDrawable, region);
                }
                if (this.mDefaultFocusHighlight != null && this.mDefaultFocusHighlight.getOpacity() != -2) {
                    applyDrawableToTransparentRegion(this.mDefaultFocusHighlight, region);
                }
            }
        }
        if (ViewDebugManager.DBG_TRANSP) {
            Log.d(VIEW_LOG_TAG, "gatherTransparentRegion by view end this=" + this + " region=" + region);
        }
        return true;
    }

    public void playSoundEffect(int i) {
        if (this.mAttachInfo == null || this.mAttachInfo.mRootCallbacks == null || !isSoundEffectsEnabled()) {
            return;
        }
        this.mAttachInfo.mRootCallbacks.playSoundEffect(i);
    }

    public boolean performHapticFeedback(int i) {
        return performHapticFeedback(i, 0);
    }

    public boolean performHapticFeedback(int i, int i2) {
        if (this.mAttachInfo == null) {
            return false;
        }
        if ((i2 & 1) != 0 || isHapticFeedbackEnabled()) {
            return this.mAttachInfo.mRootCallbacks.performHapticFeedback(i, (i2 & 2) != 0);
        }
        return false;
    }

    public void setSystemUiVisibility(int i) {
        if (i != this.mSystemUiVisibility) {
            this.mSystemUiVisibility = i;
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

    public void onWindowSystemUiVisibilityChanged(int i) {
    }

    public void dispatchWindowSystemUiVisiblityChanged(int i) {
        onWindowSystemUiVisibilityChanged(i);
    }

    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener onSystemUiVisibilityChangeListener) {
        getListenerInfo().mOnSystemUiVisibilityChangeListener = onSystemUiVisibilityChangeListener;
        if (this.mParent != null && this.mAttachInfo != null && !this.mAttachInfo.mRecomputeGlobalAttributes) {
            this.mParent.recomputeViewAttributes(this);
        }
    }

    public void dispatchSystemUiVisibilityChanged(int i) {
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnSystemUiVisibilityChangeListener != null) {
            listenerInfo.mOnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange(i & PUBLIC_STATUS_BAR_VISIBILITY_MASK);
        }
    }

    boolean updateLocalSystemUiVisibility(int i, int i2) {
        int i3 = (i & i2) | (this.mSystemUiVisibility & (~i2));
        if (i3 != this.mSystemUiVisibility) {
            setSystemUiVisibility(i3);
            return true;
        }
        return false;
    }

    public void setDisabledSystemUiVisibility(int i) {
        if (this.mAttachInfo != null && this.mAttachInfo.mDisabledSystemUiVisibility != i) {
            this.mAttachInfo.mDisabledSystemUiVisibility = i;
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

        public void onProvideShadowMetrics(Point point, Point point2) {
            View view = this.mView.get();
            if (view != null) {
                point.set(view.getWidth(), view.getHeight());
                point2.set(point.x / 2, point.y / 2);
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

    @Deprecated
    public final boolean startDrag(ClipData clipData, DragShadowBuilder dragShadowBuilder, Object obj, int i) {
        return startDragAndDrop(clipData, dragShadowBuilder, obj, i);
    }

    public final boolean startDragAndDrop(ClipData clipData, DragShadowBuilder dragShadowBuilder, Object obj, int i) throws Throwable {
        int i2;
        SurfaceSession surfaceSession = null;
        if (this.mAttachInfo == null) {
            Log.w(VIEW_LOG_TAG, "startDragAndDrop called on a detached view.");
            return false;
        }
        if (clipData != null) {
            i2 = i;
            clipData.prepareToLeaveProcess((i2 & 256) != 0);
        } else {
            i2 = i;
        }
        Point point = new Point();
        Point point2 = new Point();
        dragShadowBuilder.onProvideShadowMetrics(point, point2);
        if (point.x < 0 || point.y < 0 || point2.x < 0 || point2.y < 0) {
            throw new IllegalStateException("Drag shadow dimensions must not be negative");
        }
        if (point.x == 0 || point.y == 0) {
            if (!sAcceptZeroSizeDragShadow) {
                throw new IllegalStateException("Drag shadow dimensions must be positive");
            }
            point.x = 1;
            point.y = 1;
        }
        if (this.mAttachInfo.mDragSurface != null) {
            this.mAttachInfo.mDragSurface.release();
        }
        this.mAttachInfo.mDragSurface = new Surface();
        this.mAttachInfo.mDragToken = null;
        ViewRootImpl viewRootImpl = this.mAttachInfo.mViewRootImpl;
        SurfaceSession surfaceSession2 = new SurfaceSession(viewRootImpl.mSurface);
        SurfaceControl surfaceControlBuild = new SurfaceControl.Builder(surfaceSession2).setName("drag surface").setSize(point.x, point.y).setFormat(-3).build();
        try {
            try {
                try {
                    this.mAttachInfo.mDragSurface.copyFrom(surfaceControlBuild);
                    Canvas canvasLockCanvas = this.mAttachInfo.mDragSurface.lockCanvas(null);
                    try {
                        canvasLockCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        dragShadowBuilder.onDrawShadow(canvasLockCanvas);
                        this.mAttachInfo.mDragSurface.unlockCanvasAndPost(canvasLockCanvas);
                        viewRootImpl.setLocalDragState(obj);
                        viewRootImpl.getLastTouchPoint(point);
                        this.mAttachInfo.mDragToken = this.mAttachInfo.mSession.performDrag(this.mAttachInfo.mWindow, i2, surfaceControlBuild, viewRootImpl.getLastTouchSource(), point.x, point.y, point2.x, point2.y, clipData);
                        boolean z = this.mAttachInfo.mDragToken != null;
                        if (this.mAttachInfo.mDragToken == null) {
                            this.mAttachInfo.mDragSurface.destroy();
                            this.mAttachInfo.mDragSurface = null;
                            viewRootImpl.setLocalDragState(null);
                        }
                        surfaceSession2.kill();
                        return z;
                    } catch (Throwable th) {
                        this.mAttachInfo.mDragSurface.unlockCanvasAndPost(canvasLockCanvas);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (this.mAttachInfo.mDragToken == null) {
                        this.mAttachInfo.mDragSurface.destroy();
                        this.mAttachInfo.mDragSurface = null;
                        viewRootImpl.setLocalDragState(null);
                    }
                    surfaceSession.kill();
                    throw th;
                }
            } catch (Exception e) {
                e = e;
                Log.e(VIEW_LOG_TAG, "Unable to initiate drag", e);
                if (this.mAttachInfo.mDragToken == null) {
                    this.mAttachInfo.mDragSurface.destroy();
                    this.mAttachInfo.mDragSurface = null;
                    viewRootImpl.setLocalDragState(null);
                }
                surfaceSession.kill();
                return false;
            }
        } catch (Exception e2) {
            e = e2;
            surfaceSession = surfaceSession2;
            Log.e(VIEW_LOG_TAG, "Unable to initiate drag", e);
            if (this.mAttachInfo.mDragToken == null) {
            }
            surfaceSession.kill();
            return false;
        } catch (Throwable th3) {
            th = th3;
            surfaceSession = surfaceSession2;
            if (this.mAttachInfo.mDragToken == null) {
            }
            surfaceSession.kill();
            throw th;
        }
    }

    public final void cancelDragAndDrop() {
        if (this.mAttachInfo == null) {
            Log.w(VIEW_LOG_TAG, "cancelDragAndDrop called on a detached view.");
            return;
        }
        if (this.mAttachInfo.mDragToken != null) {
            try {
                this.mAttachInfo.mSession.cancelDragAndDrop(this.mAttachInfo.mDragToken);
            } catch (Exception e) {
                Log.e(VIEW_LOG_TAG, "Unable to cancel drag", e);
            }
            this.mAttachInfo.mDragToken = null;
            return;
        }
        Log.e(VIEW_LOG_TAG, "No active drag to cancel");
    }

    public final void updateDragShadow(DragShadowBuilder dragShadowBuilder) {
        if (this.mAttachInfo == null) {
            Log.w(VIEW_LOG_TAG, "updateDragShadow called on a detached view.");
            return;
        }
        if (this.mAttachInfo.mDragToken != null) {
            try {
                Canvas canvasLockCanvas = this.mAttachInfo.mDragSurface.lockCanvas(null);
                try {
                    canvasLockCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    dragShadowBuilder.onDrawShadow(canvasLockCanvas);
                    this.mAttachInfo.mDragSurface.unlockCanvasAndPost(canvasLockCanvas);
                    return;
                } catch (Throwable th) {
                    this.mAttachInfo.mDragSurface.unlockCanvasAndPost(canvasLockCanvas);
                    throw th;
                }
            } catch (Exception e) {
                Log.e(VIEW_LOG_TAG, "Unable to update drag shadow", e);
                return;
            }
        }
        Log.e(VIEW_LOG_TAG, "No active drag");
    }

    public final boolean startMovingTask(float f, float f2) {
        try {
            return this.mAttachInfo.mSession.startMovingTask(this.mAttachInfo.mWindow, f, f2);
        } catch (RemoteException e) {
            Log.e(VIEW_LOG_TAG, "Unable to start moving", e);
            return false;
        }
    }

    public boolean onDragEvent(DragEvent dragEvent) {
        return false;
    }

    boolean dispatchDragEnterExitInPreN(DragEvent dragEvent) {
        return callDragEventHandler(dragEvent);
    }

    public boolean dispatchDragEvent(DragEvent dragEvent) {
        dragEvent.mEventHandlerWasCalled = true;
        if (dragEvent.mAction == 2 || dragEvent.mAction == 3) {
            getViewRootImpl().setDragFocus(this, dragEvent);
        }
        return callDragEventHandler(dragEvent);
    }

    final boolean callDragEventHandler(DragEvent dragEvent) {
        boolean zOnDragEvent;
        ListenerInfo listenerInfo = this.mListenerInfo;
        if (listenerInfo != null && listenerInfo.mOnDragListener != null && (this.mViewFlags & 32) == 0 && listenerInfo.mOnDragListener.onDrag(this, dragEvent)) {
            zOnDragEvent = true;
        } else {
            zOnDragEvent = onDragEvent(dragEvent);
        }
        switch (dragEvent.mAction) {
            case 4:
                this.mPrivateFlags2 &= -4;
                refreshDrawableState();
                return zOnDragEvent;
            case 5:
                this.mPrivateFlags2 |= 2;
                refreshDrawableState();
                return zOnDragEvent;
            case 6:
                this.mPrivateFlags2 &= -3;
                refreshDrawableState();
                return zOnDragEvent;
            default:
                return zOnDragEvent;
        }
    }

    boolean canAcceptDrag() {
        return (this.mPrivateFlags2 & 1) != 0;
    }

    public void onCloseSystemDialogs(String str) {
    }

    public void applyDrawableToTransparentRegion(Drawable drawable, Region region) {
        if (ViewDebugManager.DEBUG_DRAW) {
            Log.i(VIEW_LOG_TAG, "Getting transparent region for: " + this);
        }
        Region transparentRegion = drawable.getTransparentRegion();
        Rect bounds = drawable.getBounds();
        AttachInfo attachInfo = this.mAttachInfo;
        if (transparentRegion != null && attachInfo != null) {
            int right = getRight() - getLeft();
            int bottom = getBottom() - getTop();
            if (bounds.left > 0) {
                transparentRegion.op(0, 0, bounds.left, bottom, Region.Op.UNION);
            }
            if (bounds.right < right) {
                transparentRegion.op(bounds.right, 0, right, bottom, Region.Op.UNION);
            }
            if (bounds.top > 0) {
                transparentRegion.op(0, 0, right, bounds.top, Region.Op.UNION);
            }
            if (bounds.bottom < bottom) {
                transparentRegion.op(0, bounds.bottom, right, bottom, Region.Op.UNION);
            }
            int[] iArr = attachInfo.mTransparentLocation;
            getLocationInWindow(iArr);
            transparentRegion.translate(iArr[0], iArr[1]);
            region.op(transparentRegion, Region.Op.INTERSECT);
            return;
        }
        region.op(bounds, Region.Op.DIFFERENCE);
    }

    private void checkForLongClick(int i, float f, float f2) {
        if ((this.mViewFlags & 2097152) == 2097152 || (this.mViewFlags & 1073741824) == 1073741824) {
            this.mHasPerformedLongPress = false;
            if (this.mPendingCheckForLongPress == null) {
                this.mPendingCheckForLongPress = new CheckForLongPress();
            }
            this.mPendingCheckForLongPress.setAnchor(f, f2);
            this.mPendingCheckForLongPress.rememberWindowAttachCount();
            this.mPendingCheckForLongPress.rememberPressedState();
            postDelayed(this.mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout() - i);
        }
    }

    public static View inflate(Context context, int i, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(i, viewGroup);
    }

    protected boolean overScrollBy(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, boolean z) {
        boolean z2;
        boolean z3;
        int i9 = this.mOverScrollMode;
        boolean z4 = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        boolean z5 = computeVerticalScrollRange() > computeVerticalScrollExtent();
        boolean z6 = i9 == 0 || (i9 == 1 && z4);
        boolean z7 = i9 == 0 || (i9 == 1 && z5);
        int i10 = i3 + i;
        if (!z6) {
            i7 = 0;
        }
        int i11 = i4 + i2;
        if (!z7) {
            i8 = 0;
        }
        int i12 = -i7;
        int i13 = i7 + i5;
        int i14 = -i8;
        int i15 = i6 + i8;
        if (i10 <= i13) {
            if (i10 >= i12) {
                i12 = i10;
                z2 = false;
            }
            if (i11 > i15) {
                if (i11 >= i14) {
                    z3 = false;
                    onOverScrolled(i12, i11, z2, z3);
                    return !z2 || z3;
                }
                i11 = i14;
            } else {
                i11 = i15;
            }
            z3 = true;
            onOverScrolled(i12, i11, z2, z3);
            if (z2) {
            }
        }
        i12 = i13;
        z2 = true;
        if (i11 > i15) {
        }
        z3 = true;
        onOverScrolled(i12, i11, z2, z3);
        if (z2) {
        }
    }

    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
    }

    public int getOverScrollMode() {
        return this.mOverScrollMode;
    }

    public void setOverScrollMode(int i) {
        if (i != 0 && i != 1 && i != 2) {
            throw new IllegalArgumentException("Invalid overscroll mode " + i);
        }
        this.mOverScrollMode = i;
    }

    public void setNestedScrollingEnabled(boolean z) {
        if (z) {
            this.mPrivateFlags3 |= 128;
        } else {
            stopNestedScroll();
            this.mPrivateFlags3 &= -129;
        }
    }

    public boolean isNestedScrollingEnabled() {
        return (this.mPrivateFlags3 & 128) == 128;
    }

    public boolean startNestedScroll(int i) {
        if (hasNestedScrollingParent()) {
            return true;
        }
        if (isNestedScrollingEnabled()) {
            View view = this;
            for (ViewParent parent = getParent(); parent != null; parent = parent.getParent()) {
                try {
                    if (parent.onStartNestedScroll(view, this, i)) {
                        this.mNestedScrollingParent = parent;
                        parent.onNestedScrollAccepted(view, this, i);
                        return true;
                    }
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, "ViewParent " + parent + " does not implement interface method onStartNestedScroll", e);
                }
                if (parent instanceof View) {
                    view = parent;
                }
            }
            return false;
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

    public boolean dispatchNestedScroll(int i, int i2, int i3, int i4, int[] iArr) {
        int i5;
        int i6;
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            if (i != 0 || i2 != 0 || i3 != 0 || i4 != 0) {
                if (iArr != null) {
                    getLocationInWindow(iArr);
                    i5 = iArr[0];
                    i6 = iArr[1];
                } else {
                    i5 = 0;
                    i6 = 0;
                }
                this.mNestedScrollingParent.onNestedScroll(this, i, i2, i3, i4);
                if (iArr != null) {
                    getLocationInWindow(iArr);
                    iArr[0] = iArr[0] - i5;
                    iArr[1] = iArr[1] - i6;
                }
                return true;
            }
            if (iArr != null) {
                iArr[0] = 0;
                iArr[1] = 0;
            }
        }
        return false;
    }

    public boolean dispatchNestedPreScroll(int i, int i2, int[] iArr, int[] iArr2) {
        int i3;
        int i4;
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            if (i != 0 || i2 != 0) {
                if (iArr2 != null) {
                    getLocationInWindow(iArr2);
                    i3 = iArr2[0];
                    i4 = iArr2[1];
                } else {
                    i3 = 0;
                    i4 = 0;
                }
                if (iArr == null) {
                    if (this.mTempNestedScrollConsumed == null) {
                        this.mTempNestedScrollConsumed = new int[2];
                    }
                    iArr = this.mTempNestedScrollConsumed;
                }
                iArr[0] = 0;
                iArr[1] = 0;
                this.mNestedScrollingParent.onNestedPreScroll(this, i, i2, iArr);
                if (iArr2 != null) {
                    getLocationInWindow(iArr2);
                    iArr2[0] = iArr2[0] - i3;
                    iArr2[1] = iArr2[1] - i4;
                }
                return (iArr[0] == 0 && iArr[1] == 0) ? false : true;
            }
            if (iArr2 != null) {
                iArr2[0] = 0;
                iArr2[1] = 0;
            }
        }
        return false;
    }

    public boolean dispatchNestedFling(float f, float f2, boolean z) {
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            return this.mNestedScrollingParent.onNestedFling(this, f, f2, z);
        }
        return false;
    }

    public boolean dispatchNestedPreFling(float f, float f2) {
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            return this.mNestedScrollingParent.onNestedPreFling(this, f, f2);
        }
        return false;
    }

    protected float getVerticalScrollFactor() {
        if (this.mVerticalScrollFactor == 0.0f) {
            TypedValue typedValue = new TypedValue();
            if (!this.mContext.getTheme().resolveAttribute(16842829, typedValue, true)) {
                throw new IllegalStateException("Expected theme to define listPreferredItemHeight.");
            }
            this.mVerticalScrollFactor = typedValue.getDimension(this.mContext.getResources().getDisplayMetrics());
        }
        return this.mVerticalScrollFactor;
    }

    protected float getHorizontalScrollFactor() {
        return getVerticalScrollFactor();
    }

    @ViewDebug.ExportedProperty(category = "text", mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "FIRST_STRONG"), @ViewDebug.IntToString(from = 2, to = "ANY_RTL"), @ViewDebug.IntToString(from = 3, to = "LTR"), @ViewDebug.IntToString(from = 4, to = "RTL"), @ViewDebug.IntToString(from = 5, to = "LOCALE"), @ViewDebug.IntToString(from = 6, to = "FIRST_STRONG_LTR"), @ViewDebug.IntToString(from = 7, to = "FIRST_STRONG_RTL")})
    public int getRawTextDirection() {
        return (this.mPrivateFlags2 & 448) >> 6;
    }

    public void setTextDirection(int i) {
        if (getRawTextDirection() != i) {
            this.mPrivateFlags2 &= -449;
            resetResolvedTextDirection();
            this.mPrivateFlags2 = ((i << 6) & 448) | this.mPrivateFlags2;
            resolveTextDirection();
            onRtlPropertiesChanged(getLayoutDirection());
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = "text", mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "FIRST_STRONG"), @ViewDebug.IntToString(from = 2, to = "ANY_RTL"), @ViewDebug.IntToString(from = 3, to = "LTR"), @ViewDebug.IntToString(from = 4, to = "RTL"), @ViewDebug.IntToString(from = 5, to = "LOCALE"), @ViewDebug.IntToString(from = 6, to = "FIRST_STRONG_LTR"), @ViewDebug.IntToString(from = 7, to = "FIRST_STRONG_RTL")})
    public int getTextDirection() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_DIRECTION_RESOLVED_MASK) >> 10;
    }

    public boolean resolveTextDirection() {
        int textDirection;
        this.mPrivateFlags2 &= -7681;
        if (hasRtlSupport()) {
            int rawTextDirection = getRawTextDirection();
            switch (rawTextDirection) {
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
                            textDirection = this.mParent.getTextDirection();
                        } catch (AbstractMethodError e) {
                            Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                            textDirection = 3;
                        }
                        switch (textDirection) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                                this.mPrivateFlags2 = (textDirection << 10) | this.mPrivateFlags2;
                                break;
                            default:
                                this.mPrivateFlags2 |= 1024;
                                break;
                        }
                    } catch (AbstractMethodError e2) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e2);
                        this.mPrivateFlags2 = this.mPrivateFlags2 | 1536;
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
                case 7:
                    this.mPrivateFlags2 = (rawTextDirection << 10) | this.mPrivateFlags2;
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
        if (getRawTextDirection() == 0) {
            if (this.mParent != null) {
                try {
                    return this.mParent.canResolveTextDirection();
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    return false;
                }
            }
            return false;
        }
        return true;
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

    @ViewDebug.ExportedProperty(category = "text", mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "GRAVITY"), @ViewDebug.IntToString(from = 2, to = "TEXT_START"), @ViewDebug.IntToString(from = 3, to = "TEXT_END"), @ViewDebug.IntToString(from = 4, to = "CENTER"), @ViewDebug.IntToString(from = 5, to = "VIEW_START"), @ViewDebug.IntToString(from = 6, to = "VIEW_END")})
    public int getRawTextAlignment() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_MASK) >> 13;
    }

    public void setTextAlignment(int i) {
        if (i != getRawTextAlignment()) {
            this.mPrivateFlags2 &= -57345;
            resetResolvedTextAlignment();
            this.mPrivateFlags2 = ((i << 13) & PFLAG2_TEXT_ALIGNMENT_MASK) | this.mPrivateFlags2;
            resolveTextAlignment();
            onRtlPropertiesChanged(getLayoutDirection());
            requestLayout();
            invalidate(true);
        }
    }

    @ViewDebug.ExportedProperty(category = "text", mapping = {@ViewDebug.IntToString(from = 0, to = "INHERIT"), @ViewDebug.IntToString(from = 1, to = "GRAVITY"), @ViewDebug.IntToString(from = 2, to = "TEXT_START"), @ViewDebug.IntToString(from = 3, to = "TEXT_END"), @ViewDebug.IntToString(from = 4, to = "CENTER"), @ViewDebug.IntToString(from = 5, to = "VIEW_START"), @ViewDebug.IntToString(from = 6, to = "VIEW_END")})
    public int getTextAlignment() {
        return (this.mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK) >> 17;
    }

    public boolean resolveTextAlignment() {
        int textAlignment;
        this.mPrivateFlags2 &= -983041;
        if (hasRtlSupport()) {
            int rawTextAlignment = getRawTextAlignment();
            switch (rawTextAlignment) {
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
                            textAlignment = this.mParent.getTextAlignment();
                        } catch (AbstractMethodError e) {
                            Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                            textAlignment = 1;
                        }
                        switch (textAlignment) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                                this.mPrivateFlags2 = (textAlignment << 17) | this.mPrivateFlags2;
                                break;
                            default:
                                this.mPrivateFlags2 |= 131072;
                                break;
                        }
                    } catch (AbstractMethodError e2) {
                        Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e2);
                        this.mPrivateFlags2 = this.mPrivateFlags2 | 196608;
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
                    this.mPrivateFlags2 = (rawTextAlignment << 17) | this.mPrivateFlags2;
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
        if (getRawTextAlignment() == 0) {
            if (this.mParent != null) {
                try {
                    return this.mParent.canResolveTextAlignment();
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, this.mParent.getClass().getSimpleName() + " does not fully implement ViewParent", e);
                    return false;
                }
            }
            return false;
        }
        return true;
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
        int i;
        int i2;
        do {
            i = sNextGeneratedId.get();
            i2 = i + 1;
            if (i2 > 16777215) {
                i2 = 1;
            }
        } while (!sNextGeneratedId.compareAndSet(i, i2));
        return i;
    }

    private static boolean isViewIdGenerated(int i) {
        return ((-16777216) & i) == 0 && (i & 16777215) != 0;
    }

    public void captureTransitioningViews(List<View> list) {
        if (getVisibility() == 0) {
            list.add(this);
        }
    }

    public void findNamedViews(Map<String, View> map) {
        String transitionName;
        if ((getVisibility() == 0 || this.mGhostView != null) && (transitionName = getTransitionName()) != null) {
            map.put(transitionName, this);
        }
    }

    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        float x = motionEvent.getX(i);
        float y = motionEvent.getY(i);
        if (isDraggingScrollBar() || isOnScrollbarThumb(x, y)) {
            return PointerIcon.getSystemIcon(this.mContext, 1000);
        }
        return this.mPointerIcon;
    }

    public void setPointerIcon(PointerIcon pointerIcon) {
        this.mPointerIcon = pointerIcon;
        if (this.mAttachInfo == null || this.mAttachInfo.mHandlingPointerEvent) {
            return;
        }
        try {
            this.mAttachInfo.mSession.updatePointerIcon(this.mAttachInfo.mWindow);
        } catch (RemoteException e) {
        }
    }

    public PointerIcon getPointerIcon() {
        return this.mPointerIcon;
    }

    public boolean hasPointerCapture() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl == null) {
            return false;
        }
        return viewRootImpl.hasPointerCapture();
    }

    public void requestPointerCapture() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.requestPointerCapture(true);
        }
    }

    public void releasePointerCapture() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.requestPointerCapture(false);
        }
    }

    public void onPointerCaptureChange(boolean z) {
    }

    public void dispatchPointerCaptureChanged(boolean z) {
        onPointerCaptureChange(z);
    }

    public boolean onCapturedPointerEvent(MotionEvent motionEvent) {
        return false;
    }

    public void setOnCapturedPointerListener(OnCapturedPointerListener onCapturedPointerListener) {
        getListenerInfo().mOnCapturedPointerListener = onCapturedPointerListener;
    }

    public static class MeasureSpec {
        public static final int AT_MOST = Integer.MIN_VALUE;
        public static final int EXACTLY = 1073741824;
        private static final int MODE_MASK = -1073741824;
        private static final int MODE_SHIFT = 30;
        public static final int UNSPECIFIED = 0;

        @Retention(RetentionPolicy.SOURCE)
        public @interface MeasureSpecMode {
        }

        public static int makeMeasureSpec(int i, int i2) {
            if (View.sUseBrokenMakeMeasureSpec) {
                return i + i2;
            }
            return (i & View.LAST_APP_AUTOFILL_ID) | (i2 & (-1073741824));
        }

        public static int makeSafeMeasureSpec(int i, int i2) {
            if (View.sUseZeroUnspecifiedMeasureSpec && i2 == 0) {
                return 0;
            }
            return makeMeasureSpec(i, i2);
        }

        public static int getMode(int i) {
            return i & (-1073741824);
        }

        public static int getSize(int i) {
            return i & View.LAST_APP_AUTOFILL_ID;
        }

        static int adjust(int i, int i2) {
            int mode = getMode(i);
            int size = getSize(i);
            if (mode == 0) {
                return makeMeasureSpec(size, 0);
            }
            int i3 = size + i2;
            if (i3 < 0) {
                Log.e(View.VIEW_LOG_TAG, "MeasureSpec.adjust: new size would be negative! (" + i3 + ") spec: " + toString(i) + " delta: " + i2);
                i3 = 0;
            }
            return makeMeasureSpec(i3, mode);
        }

        public static String toString(int i) {
            int mode = getMode(i);
            int size = getSize(i);
            StringBuilder sb = new StringBuilder("MeasureSpec: ");
            if (mode == 0) {
                sb.append("UNSPECIFIED ");
            } else if (mode == 1073741824) {
                sb.append("EXACTLY ");
            } else if (mode == Integer.MIN_VALUE) {
                sb.append("AT_MOST ");
            } else {
                sb.append(mode);
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            }
            sb.append(size);
            return sb.toString();
        }
    }

    private final class CheckForLongPress implements Runnable {
        private boolean mOriginalPressedState;
        private int mOriginalWindowAttachCount;
        private float mX;
        private float mY;

        private CheckForLongPress() {
        }

        @Override
        public void run() {
            if (this.mOriginalPressedState == View.this.isPressed() && View.this.mParent != null && this.mOriginalWindowAttachCount == View.this.mWindowAttachCount && View.this.performLongClick(this.mX, this.mY)) {
                View.this.mHasPerformedLongPress = true;
            }
        }

        public void setAnchor(float f, float f2) {
            this.mX = f;
            this.mY = f2;
        }

        public void rememberWindowAttachCount() {
            this.mOriginalWindowAttachCount = View.this.mWindowAttachCount;
        }

        public void rememberPressedState() {
            this.mOriginalPressedState = View.this.isPressed();
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
            View.this.checkForLongClick(ViewConfiguration.getTapTimeout(), this.x, this.y);
        }
    }

    private final class PerformClick implements Runnable {
        private PerformClick() {
        }

        @Override
        public void run() {
            View.this.performClickInternal();
        }
    }

    public ViewPropertyAnimator animate() {
        if (this.mAnimator == null) {
            this.mAnimator = new ViewPropertyAnimator(this);
        }
        return this.mAnimator;
    }

    public final void setTransitionName(String str) {
        this.mTransitionName = str;
    }

    @ViewDebug.ExportedProperty
    public String getTransitionName() {
        return this.mTransitionName;
    }

    public void requestKeyboardShortcuts(List<KeyboardShortcutGroup> list, int i) {
    }

    private final class UnsetPressedState implements Runnable {
        private UnsetPressedState() {
        }

        @Override
        public void run() {
            View.this.setPressed(false);
        }
    }

    private static class VisibilityChangeForAutofillHandler extends Handler {
        private final AutofillManager mAfm;
        private final View mView;

        private VisibilityChangeForAutofillHandler(AutofillManager autofillManager, View view) {
            this.mAfm = autofillManager;
            this.mView = view;
        }

        @Override
        public void handleMessage(Message message) {
            this.mAfm.notifyViewVisibilityChanged(this.mView, this.mView.isShown());
        }
    }

    public static class BaseSavedState extends AbsSavedState {
        static final int AUTOFILL_ID = 4;
        public static final Parcelable.Creator<BaseSavedState> CREATOR = new Parcelable.ClassLoaderCreator<BaseSavedState>() {
            @Override
            public BaseSavedState createFromParcel(Parcel parcel) {
                return new BaseSavedState(parcel);
            }

            @Override
            public BaseSavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new BaseSavedState(parcel, classLoader);
            }

            @Override
            public BaseSavedState[] newArray(int i) {
                return new BaseSavedState[i];
            }
        };
        static final int IS_AUTOFILLED = 2;
        static final int START_ACTIVITY_REQUESTED_WHO_SAVED = 1;
        int mAutofillViewId;
        boolean mIsAutofilled;
        int mSavedData;
        String mStartActivityRequestWhoSaved;

        public BaseSavedState(Parcel parcel) {
            this(parcel, null);
        }

        public BaseSavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.mSavedData = parcel.readInt();
            this.mStartActivityRequestWhoSaved = parcel.readString();
            this.mIsAutofilled = parcel.readBoolean();
            this.mAutofillViewId = parcel.readInt();
        }

        public BaseSavedState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mSavedData);
            parcel.writeString(this.mStartActivityRequestWhoSaved);
            parcel.writeBoolean(this.mIsAutofilled);
            parcel.writeInt(this.mAutofillViewId);
        }
    }

    static final class AttachInfo {
        int mAccessibilityFetchFlags;
        Drawable mAccessibilityFocusDrawable;
        boolean mAlwaysConsumeNavBar;
        float mApplicationScale;
        Drawable mAutofilledDrawable;
        Canvas mCanvas;
        int mDisabledSystemUiVisibility;
        Display mDisplay;
        public Surface mDragSurface;
        IBinder mDragToken;
        long mDrawingTime;
        List<View> mEmptyPartialLayoutViews;
        boolean mForceReportNewAttributes;
        final Handler mHandler;
        boolean mHandlingPointerEvent;
        boolean mHardwareAccelerated;
        boolean mHardwareAccelerationRequested;
        boolean mHasNonEmptyGivenInternalInsets;
        boolean mHasSystemUiListeners;
        boolean mHasWindowFocus;
        IWindowId mIWindowId;
        boolean mIgnoreDirtyState;
        boolean mInTouchMode;
        boolean mKeepScreenOn;
        boolean mNeedsUpdateLightCenter;
        boolean mOverscanRequested;
        IBinder mPanelParentWindowToken;
        List<RenderNode> mPendingAnimatingRenderNodes;
        boolean mRecomputeGlobalAttributes;
        final Callbacks mRootCallbacks;
        View mRootView;
        boolean mScalingRequired;
        final IWindowSession mSession;
        int mSystemUiVisibility;
        ThreadedRenderer mThreadedRenderer;
        View mTooltipHost;
        final ViewTreeObserver mTreeObserver;
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
        final DisplayCutout.ParcelableWrapper mDisplayCutout = new DisplayCutout.ParcelableWrapper(DisplayCutout.NO_CUTOUT);
        final Rect mOutsets = new Rect();
        final ViewTreeObserver.InternalInsetsInfo mGivenInternalInsets = new ViewTreeObserver.InternalInsetsInfo();
        final ArrayList<View> mScrollContainers = new ArrayList<>();
        final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();
        boolean mSetIgnoreDirtyState = false;
        int mGlobalSystemUiVisibility = -1;
        final int[] mTransparentLocation = new int[2];
        final int[] mInvalidateChildLocation = new int[2];
        final int[] mTmpLocation = new int[2];
        final float[] mTmpTransformLocation = new float[2];
        final Rect mTmpInvalRect = new Rect();
        final RectF mTmpTransformRect = new RectF();
        final RectF mTmpTransformRect1 = new RectF();
        final List<RectF> mTmpRectList = new ArrayList();
        final Matrix mTmpMatrix = new Matrix();
        final Transformation mTmpTransformation = new Transformation();
        final Outline mTmpOutline = new Outline();
        final ArrayList<View> mTempArrayList = new ArrayList<>(24);
        int mAccessibilityWindowId = -1;
        boolean mDebugLayout = SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
        final Point mPoint = new Point();
        List<View> mPartialLayoutViews = new ArrayList();

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
                InvalidateInfo invalidateInfoAcquire = sPool.acquire();
                return invalidateInfoAcquire != null ? invalidateInfoAcquire : new InvalidateInfo();
            }

            public void recycle() {
                this.target = null;
                sPool.release(this);
            }
        }

        AttachInfo(IWindowSession iWindowSession, IWindow iWindow, Display display, ViewRootImpl viewRootImpl, Handler handler, Callbacks callbacks, Context context) {
            this.mSession = iWindowSession;
            this.mWindow = iWindow;
            this.mWindowToken = iWindow.asBinder();
            this.mDisplay = display;
            this.mViewRootImpl = viewRootImpl;
            this.mHandler = handler;
            this.mRootCallbacks = callbacks;
            this.mTreeObserver = new ViewTreeObserver(context);
        }
    }

    private static class ScrollabilityCache implements Runnable {
        public static final int DRAGGING_HORIZONTAL_SCROLL_BAR = 2;
        public static final int DRAGGING_VERTICAL_SCROLL_BAR = 1;
        public static final int FADING = 2;
        public static final int NOT_DRAGGING = 0;
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
        public int scrollBarMinTouchTarget;
        public int scrollBarSize;
        public final Interpolator scrollBarInterpolator = new Interpolator(1, 2);
        public int state = 0;
        public final Rect mScrollBarBounds = new Rect();
        public final Rect mScrollBarTouchBounds = new Rect();
        public int mScrollBarDraggingState = 0;
        public float mScrollBarDraggingPos = 0.0f;
        public int scrollBarDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
        public int scrollBarFadeDuration = ViewConfiguration.getScrollBarFadeDuration();
        public final Paint paint = new Paint();
        public final Matrix matrix = new Matrix();
        public Shader shader = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -16777216, 0, Shader.TileMode.CLAMP);

        public ScrollabilityCache(ViewConfiguration viewConfiguration, View view) {
            this.fadingEdgeLength = viewConfiguration.getScaledFadingEdgeLength();
            this.scrollBarSize = viewConfiguration.getScaledScrollBarSize();
            this.scrollBarMinTouchTarget = viewConfiguration.getScaledMinScrollbarTouchTarget();
            this.paint.setShader(this.shader);
            this.paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            this.host = view;
        }

        public void setFadeColor(int i) {
            if (i != this.mLastColor) {
                this.mLastColor = i;
                if (i != 0) {
                    this.shader = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, i | (-16777216), i & 16777215, Shader.TileMode.CLAMP);
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
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
            if (jCurrentAnimationTimeMillis >= this.fadeStartTime) {
                int i = (int) jCurrentAnimationTimeMillis;
                Interpolator interpolator = this.scrollBarInterpolator;
                interpolator.setKeyFrame(0, i, OPAQUE);
                interpolator.setKeyFrame(1, i + this.scrollBarFadeDuration, TRANSPARENT);
                this.state = 2;
                this.host.invalidate(true);
            }
        }
    }

    private class SendViewScrolledAccessibilityEvent implements Runnable {
        public int mDeltaX;
        public int mDeltaY;
        public volatile boolean mIsPending;

        private SendViewScrolledAccessibilityEvent() {
        }

        public void post(int i, int i2) {
            this.mDeltaX += i;
            this.mDeltaY += i2;
            if (!this.mIsPending) {
                this.mIsPending = true;
                View.this.postDelayed(this, ViewConfiguration.getSendRecurringAccessibilityEventsInterval());
            }
        }

        @Override
        public void run() {
            if (AccessibilityManager.getInstance(View.this.mContext).isEnabled()) {
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(4096);
                accessibilityEventObtain.setScrollDeltaX(this.mDeltaX);
                accessibilityEventObtain.setScrollDeltaY(this.mDeltaY);
                View.this.sendAccessibilityEventUnchecked(accessibilityEventObtain);
            }
            reset();
        }

        private void reset() {
            this.mIsPending = false;
            this.mDeltaX = 0;
            this.mDeltaY = 0;
        }
    }

    private void cancel(SendViewScrolledAccessibilityEvent sendViewScrolledAccessibilityEvent) {
        if (sendViewScrolledAccessibilityEvent == null || !sendViewScrolledAccessibilityEvent.mIsPending) {
            return;
        }
        removeCallbacks(sendViewScrolledAccessibilityEvent);
        sendViewScrolledAccessibilityEvent.reset();
    }

    public static class AccessibilityDelegate {
        public void sendAccessibilityEvent(View view, int i) {
            view.sendAccessibilityEventInternal(i);
        }

        public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
            return view.performAccessibilityActionInternal(i, bundle);
        }

        public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
            view.sendAccessibilityEventUncheckedInternal(accessibilityEvent);
        }

        public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            return view.dispatchPopulateAccessibilityEventInternal(accessibilityEvent);
        }

        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            view.onPopulateAccessibilityEventInternal(accessibilityEvent);
        }

        public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            view.onInitializeAccessibilityEventInternal(accessibilityEvent);
        }

        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            view.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        }

        public void addExtraDataToAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo, String str, Bundle bundle) {
            view.addExtraDataToAccessibilityNodeInfo(accessibilityNodeInfo, str, bundle);
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            return viewGroup.onRequestSendAccessibilityEventInternal(view, accessibilityEvent);
        }

        public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
            return null;
        }

        public AccessibilityNodeInfo createAccessibilityNodeInfo(View view) {
            return view.createAccessibilityNodeInfoInternal();
        }
    }

    private static class MatchIdPredicate implements Predicate<View> {
        public int mId;

        private MatchIdPredicate() {
        }

        @Override
        public boolean test(View view) {
            return view.mID == this.mId;
        }
    }

    private static class MatchLabelForPredicate implements Predicate<View> {
        private int mLabeledId;

        private MatchLabelForPredicate() {
        }

        @Override
        public boolean test(View view) {
            return view.mLabelForId == this.mLabeledId;
        }
    }

    private static void dumpFlags() {
        HashMap mapNewHashMap = Maps.newHashMap();
        try {
            for (Field field : View.class.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    if (field.getType().equals(Integer.TYPE)) {
                        dumpFlag(mapNewHashMap, field.getName(), field.getInt(null));
                    } else if (field.getType().equals(int[].class)) {
                        int[] iArr = (int[]) field.get(null);
                        for (int i = 0; i < iArr.length; i++) {
                            dumpFlag(mapNewHashMap, field.getName() + "[" + i + "]", iArr[i]);
                        }
                    }
                }
            }
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            arrayListNewArrayList.addAll(mapNewHashMap.keySet());
            Collections.sort(arrayListNewArrayList);
            Iterator it = arrayListNewArrayList.iterator();
            while (it.hasNext()) {
                Log.d(VIEW_LOG_TAG, (String) mapNewHashMap.get((String) it.next()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void dumpFlag(HashMap<String, String> map, String str, int i) {
        String strReplace = String.format("%32s", Integer.toBinaryString(i)).replace('0', ' ');
        int iIndexOf = str.indexOf(95);
        StringBuilder sb = new StringBuilder();
        sb.append(iIndexOf > 0 ? str.substring(0, iIndexOf) : str);
        sb.append(strReplace);
        sb.append(str);
        map.put(sb.toString(), strReplace + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str);
    }

    public void encode(ViewHierarchyEncoder viewHierarchyEncoder) {
        viewHierarchyEncoder.beginObject(this);
        encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.endObject();
    }

    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        Object objResolveId = ViewDebug.resolveId(getContext(), this.mID);
        if (objResolveId instanceof String) {
            viewHierarchyEncoder.addProperty(Instrumentation.REPORT_KEY_IDENTIFIER, (String) objResolveId);
        } else {
            viewHierarchyEncoder.addProperty(Instrumentation.REPORT_KEY_IDENTIFIER, this.mID);
        }
        viewHierarchyEncoder.addProperty("misc:transformation.alpha", this.mTransformationInfo != null ? this.mTransformationInfo.mAlpha : 0.0f);
        viewHierarchyEncoder.addProperty("misc:transitionName", getTransitionName());
        viewHierarchyEncoder.addProperty("layout:left", this.mLeft);
        viewHierarchyEncoder.addProperty("layout:right", this.mRight);
        viewHierarchyEncoder.addProperty("layout:top", this.mTop);
        viewHierarchyEncoder.addProperty("layout:bottom", this.mBottom);
        viewHierarchyEncoder.addProperty("layout:width", getWidth());
        viewHierarchyEncoder.addProperty("layout:height", getHeight());
        viewHierarchyEncoder.addProperty("layout:layoutDirection", getLayoutDirection());
        viewHierarchyEncoder.addProperty("layout:layoutRtl", isLayoutRtl());
        viewHierarchyEncoder.addProperty("layout:hasTransientState", hasTransientState());
        viewHierarchyEncoder.addProperty("layout:baseline", getBaseline());
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            viewHierarchyEncoder.addPropertyKey("layoutParams");
            layoutParams.encode(viewHierarchyEncoder);
        }
        viewHierarchyEncoder.addProperty("scrolling:scrollX", this.mScrollX);
        viewHierarchyEncoder.addProperty("scrolling:scrollY", this.mScrollY);
        viewHierarchyEncoder.addProperty("padding:paddingLeft", this.mPaddingLeft);
        viewHierarchyEncoder.addProperty("padding:paddingRight", this.mPaddingRight);
        viewHierarchyEncoder.addProperty("padding:paddingTop", this.mPaddingTop);
        viewHierarchyEncoder.addProperty("padding:paddingBottom", this.mPaddingBottom);
        viewHierarchyEncoder.addProperty("padding:userPaddingRight", this.mUserPaddingRight);
        viewHierarchyEncoder.addProperty("padding:userPaddingLeft", this.mUserPaddingLeft);
        viewHierarchyEncoder.addProperty("padding:userPaddingBottom", this.mUserPaddingBottom);
        viewHierarchyEncoder.addProperty("padding:userPaddingStart", this.mUserPaddingStart);
        viewHierarchyEncoder.addProperty("padding:userPaddingEnd", this.mUserPaddingEnd);
        viewHierarchyEncoder.addProperty("measurement:minHeight", this.mMinHeight);
        viewHierarchyEncoder.addProperty("measurement:minWidth", this.mMinWidth);
        viewHierarchyEncoder.addProperty("measurement:measuredWidth", this.mMeasuredWidth);
        viewHierarchyEncoder.addProperty("measurement:measuredHeight", this.mMeasuredHeight);
        viewHierarchyEncoder.addProperty("drawing:elevation", getElevation());
        viewHierarchyEncoder.addProperty("drawing:translationX", getTranslationX());
        viewHierarchyEncoder.addProperty("drawing:translationY", getTranslationY());
        viewHierarchyEncoder.addProperty("drawing:translationZ", getTranslationZ());
        viewHierarchyEncoder.addProperty("drawing:rotation", getRotation());
        viewHierarchyEncoder.addProperty("drawing:rotationX", getRotationX());
        viewHierarchyEncoder.addProperty("drawing:rotationY", getRotationY());
        viewHierarchyEncoder.addProperty("drawing:scaleX", getScaleX());
        viewHierarchyEncoder.addProperty("drawing:scaleY", getScaleY());
        viewHierarchyEncoder.addProperty("drawing:pivotX", getPivotX());
        viewHierarchyEncoder.addProperty("drawing:pivotY", getPivotY());
        viewHierarchyEncoder.addProperty("drawing:clipBounds", this.mClipBounds == null ? null : this.mClipBounds.toString());
        viewHierarchyEncoder.addProperty("drawing:opaque", isOpaque());
        viewHierarchyEncoder.addProperty("drawing:alpha", getAlpha());
        viewHierarchyEncoder.addProperty("drawing:transitionAlpha", getTransitionAlpha());
        viewHierarchyEncoder.addProperty("drawing:shadow", hasShadow());
        viewHierarchyEncoder.addProperty("drawing:solidColor", getSolidColor());
        viewHierarchyEncoder.addProperty("drawing:layerType", this.mLayerType);
        viewHierarchyEncoder.addProperty("drawing:willNotDraw", willNotDraw());
        viewHierarchyEncoder.addProperty("drawing:hardwareAccelerated", isHardwareAccelerated());
        viewHierarchyEncoder.addProperty("drawing:willNotCacheDrawing", willNotCacheDrawing());
        viewHierarchyEncoder.addProperty("drawing:drawingCacheEnabled", isDrawingCacheEnabled());
        viewHierarchyEncoder.addProperty("drawing:overlappingRendering", hasOverlappingRendering());
        viewHierarchyEncoder.addProperty("drawing:outlineAmbientShadowColor", getOutlineAmbientShadowColor());
        viewHierarchyEncoder.addProperty("drawing:outlineSpotShadowColor", getOutlineSpotShadowColor());
        viewHierarchyEncoder.addProperty("focus:hasFocus", hasFocus());
        viewHierarchyEncoder.addProperty("focus:isFocused", isFocused());
        viewHierarchyEncoder.addProperty("focus:focusable", getFocusable());
        viewHierarchyEncoder.addProperty("focus:isFocusable", isFocusable());
        viewHierarchyEncoder.addProperty("focus:isFocusableInTouchMode", isFocusableInTouchMode());
        viewHierarchyEncoder.addProperty("misc:clickable", isClickable());
        viewHierarchyEncoder.addProperty("misc:pressed", isPressed());
        viewHierarchyEncoder.addProperty("misc:selected", isSelected());
        viewHierarchyEncoder.addProperty("misc:touchMode", isInTouchMode());
        viewHierarchyEncoder.addProperty("misc:hovered", isHovered());
        viewHierarchyEncoder.addProperty("misc:activated", isActivated());
        viewHierarchyEncoder.addProperty("misc:visibility", getVisibility());
        viewHierarchyEncoder.addProperty("misc:fitsSystemWindows", getFitsSystemWindows());
        viewHierarchyEncoder.addProperty("misc:filterTouchesWhenObscured", getFilterTouchesWhenObscured());
        viewHierarchyEncoder.addProperty("misc:enabled", isEnabled());
        viewHierarchyEncoder.addProperty("misc:soundEffectsEnabled", isSoundEffectsEnabled());
        viewHierarchyEncoder.addProperty("misc:hapticFeedbackEnabled", isHapticFeedbackEnabled());
        Resources.Theme theme = getContext().getTheme();
        if (theme != null) {
            viewHierarchyEncoder.addPropertyKey("theme");
            theme.encode(viewHierarchyEncoder);
        }
        int length = this.mAttributes != null ? this.mAttributes.length : 0;
        viewHierarchyEncoder.addProperty("meta:__attrCount__", length / 2);
        for (int i = 0; i < length; i += 2) {
            viewHierarchyEncoder.addProperty("meta:__attr__" + this.mAttributes[i], this.mAttributes[i + 1]);
        }
        viewHierarchyEncoder.addProperty("misc:scrollBarStyle", getScrollBarStyle());
        viewHierarchyEncoder.addProperty("text:textDirection", getTextDirection());
        viewHierarchyEncoder.addProperty("text:textAlignment", getTextAlignment());
        CharSequence contentDescription = getContentDescription();
        viewHierarchyEncoder.addProperty("accessibility:contentDescription", contentDescription == null ? "" : contentDescription.toString());
        viewHierarchyEncoder.addProperty("accessibility:labelFor", getLabelFor());
        viewHierarchyEncoder.addProperty("accessibility:importantForAccessibility", getImportantForAccessibility());
    }

    boolean shouldDrawRoundScrollbar() {
        if (!this.mResources.getConfiguration().isScreenRound() || this.mAttachInfo == null) {
            return false;
        }
        View rootView = getRootView();
        WindowInsets rootWindowInsets = getRootWindowInsets();
        int height = getHeight();
        int width = getWidth();
        int height2 = rootView.getHeight();
        int width2 = rootView.getWidth();
        if (height != height2 || width != width2) {
            return false;
        }
        getLocationInWindow(this.mAttachInfo.mTmpLocation);
        return this.mAttachInfo.mTmpLocation[0] == rootWindowInsets.getStableInsetLeft() && this.mAttachInfo.mTmpLocation[1] == rootWindowInsets.getStableInsetTop();
    }

    public void setTooltipText(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            setFlags(0, 1073741824);
            hideTooltip();
            this.mTooltipInfo = null;
            return;
        }
        setFlags(1073741824, 1073741824);
        if (this.mTooltipInfo == null) {
            this.mTooltipInfo = new TooltipInfo();
            this.mTooltipInfo.mShowTooltipRunnable = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.showHoverTooltip();
                }
            };
            this.mTooltipInfo.mHideTooltipRunnable = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.hideTooltip();
                }
            };
            this.mTooltipInfo.mHoverSlop = ViewConfiguration.get(this.mContext).getScaledHoverSlop();
            this.mTooltipInfo.clearAnchorPos();
        }
        this.mTooltipInfo.mTooltipText = charSequence;
    }

    public void setTooltip(CharSequence charSequence) {
        setTooltipText(charSequence);
    }

    public CharSequence getTooltipText() {
        if (this.mTooltipInfo != null) {
            return this.mTooltipInfo.mTooltipText;
        }
        return null;
    }

    public CharSequence getTooltip() {
        return getTooltipText();
    }

    private boolean showTooltip(int i, int i2, boolean z) {
        if (this.mAttachInfo == null || this.mTooltipInfo == null) {
            return false;
        }
        if ((z && (this.mViewFlags & 32) != 0) || TextUtils.isEmpty(this.mTooltipInfo.mTooltipText)) {
            return false;
        }
        hideTooltip();
        this.mTooltipInfo.mTooltipFromLongClick = z;
        this.mTooltipInfo.mTooltipPopup = new TooltipPopup(getContext());
        this.mTooltipInfo.mTooltipPopup.show(this, i, i2, (this.mPrivateFlags3 & 131072) == 131072, this.mTooltipInfo.mTooltipText);
        this.mAttachInfo.mTooltipHost = this;
        notifyViewAccessibilityStateChangedIfNeeded(0);
        return true;
    }

    void hideTooltip() {
        if (this.mTooltipInfo == null) {
            return;
        }
        removeCallbacks(this.mTooltipInfo.mShowTooltipRunnable);
        if (this.mTooltipInfo.mTooltipPopup == null) {
            return;
        }
        this.mTooltipInfo.mTooltipPopup.hide();
        this.mTooltipInfo.mTooltipPopup = null;
        this.mTooltipInfo.mTooltipFromLongClick = false;
        this.mTooltipInfo.clearAnchorPos();
        if (this.mAttachInfo != null) {
            this.mAttachInfo.mTooltipHost = null;
        }
        notifyViewAccessibilityStateChangedIfNeeded(0);
    }

    private boolean showLongClickTooltip(int i, int i2) {
        removeCallbacks(this.mTooltipInfo.mShowTooltipRunnable);
        removeCallbacks(this.mTooltipInfo.mHideTooltipRunnable);
        return showTooltip(i, i2, true);
    }

    private boolean showHoverTooltip() {
        return showTooltip(this.mTooltipInfo.mAnchorX, this.mTooltipInfo.mAnchorY, false);
    }

    boolean dispatchTooltipHoverEvent(MotionEvent motionEvent) {
        int hoverTooltipHideTimeout;
        if (this.mTooltipInfo == null) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action != 7) {
            if (action == 10) {
                this.mTooltipInfo.clearAnchorPos();
                if (!this.mTooltipInfo.mTooltipFromLongClick) {
                    hideTooltip();
                }
            }
        } else if ((this.mViewFlags & 1073741824) == 1073741824) {
            if (!this.mTooltipInfo.mTooltipFromLongClick && this.mTooltipInfo.updateAnchorPos(motionEvent)) {
                if (this.mTooltipInfo.mTooltipPopup == null) {
                    removeCallbacks(this.mTooltipInfo.mShowTooltipRunnable);
                    postDelayed(this.mTooltipInfo.mShowTooltipRunnable, ViewConfiguration.getHoverTooltipShowTimeout());
                }
                if ((getWindowSystemUiVisibility() & 1) == 1) {
                    hoverTooltipHideTimeout = ViewConfiguration.getHoverTooltipHideShortTimeout();
                } else {
                    hoverTooltipHideTimeout = ViewConfiguration.getHoverTooltipHideTimeout();
                }
                removeCallbacks(this.mTooltipInfo.mHideTooltipRunnable);
                postDelayed(this.mTooltipInfo.mHideTooltipRunnable, hoverTooltipHideTimeout);
            }
            return true;
        }
        return false;
    }

    void handleTooltipKey(KeyEvent keyEvent) {
        switch (keyEvent.getAction()) {
            case 0:
                if (keyEvent.getRepeatCount() == 0) {
                    hideTooltip();
                }
                break;
            case 1:
                handleTooltipUp();
                break;
        }
    }

    private void handleTooltipUp() {
        if (this.mTooltipInfo == null || this.mTooltipInfo.mTooltipPopup == null) {
            return;
        }
        removeCallbacks(this.mTooltipInfo.mHideTooltipRunnable);
        postDelayed(this.mTooltipInfo.mHideTooltipRunnable, ViewConfiguration.getLongPressTooltipHideTimeout());
    }

    private int getFocusableAttribute(TypedArray typedArray) {
        TypedValue typedValue = new TypedValue();
        if (typedArray.getValue(19, typedValue)) {
            if (typedValue.type == 18) {
                return typedValue.data == 0 ? 0 : 1;
            }
            return typedValue.data;
        }
        return 16;
    }

    public View getTooltipView() {
        if (this.mTooltipInfo == null || this.mTooltipInfo.mTooltipPopup == null) {
            return null;
        }
        return this.mTooltipInfo.mTooltipPopup.getContentView();
    }

    public static boolean isDefaultFocusHighlightEnabled() {
        return sUseDefaultFocusHighlight;
    }

    View dispatchUnhandledKeyEvent(KeyEvent keyEvent) {
        if (onUnhandledKeyEvent(keyEvent)) {
            return this;
        }
        return null;
    }

    boolean onUnhandledKeyEvent(KeyEvent keyEvent) {
        if (this.mListenerInfo != null && this.mListenerInfo.mUnhandledKeyListeners != null) {
            for (int size = this.mListenerInfo.mUnhandledKeyListeners.size() - 1; size >= 0; size--) {
                if (((OnUnhandledKeyEventListener) this.mListenerInfo.mUnhandledKeyListeners.get(size)).onUnhandledKeyEvent(this, keyEvent)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    boolean hasUnhandledKeyListener() {
        return (this.mListenerInfo == null || this.mListenerInfo.mUnhandledKeyListeners == null || this.mListenerInfo.mUnhandledKeyListeners.isEmpty()) ? false : true;
    }

    public void addOnUnhandledKeyEventListener(OnUnhandledKeyEventListener onUnhandledKeyEventListener) {
        ArrayList arrayList = getListenerInfo().mUnhandledKeyListeners;
        if (arrayList == null) {
            arrayList = new ArrayList();
            getListenerInfo().mUnhandledKeyListeners = arrayList;
        }
        arrayList.add(onUnhandledKeyEventListener);
        if (arrayList.size() == 1 && (this.mParent instanceof ViewGroup)) {
            ((ViewGroup) this.mParent).incrementChildUnhandledKeyListeners();
        }
    }

    public void removeOnUnhandledKeyEventListener(OnUnhandledKeyEventListener onUnhandledKeyEventListener) {
        if (this.mListenerInfo != null && this.mListenerInfo.mUnhandledKeyListeners != null && !this.mListenerInfo.mUnhandledKeyListeners.isEmpty()) {
            this.mListenerInfo.mUnhandledKeyListeners.remove(onUnhandledKeyEventListener);
            if (this.mListenerInfo.mUnhandledKeyListeners.isEmpty()) {
                this.mListenerInfo.mUnhandledKeyListeners = null;
                if (this.mParent instanceof ViewGroup) {
                    ((ViewGroup) this.mParent).decrementChildUnhandledKeyListeners();
                }
            }
        }
    }
}
