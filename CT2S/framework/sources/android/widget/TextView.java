package android.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.UndoManager;
import android.content.res.ColorStateList;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.ExtractEditText;
import android.net.ProxyInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.BoringLayout;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.GetChars;
import android.text.GraphicsOperations;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.AllCapsTransformationMethod;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TextKeyListener;
import android.text.method.TimeKeyListener;
import android.text.method.TransformationMethod;
import android.text.method.TransformationMethod2;
import android.text.method.WordIterator;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ParagraphStyle;
import android.text.style.SpellCheckSpan;
import android.text.style.SuggestionSpan;
import android.text.style.URLSpan;
import android.text.style.UpdateAppearance;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.AccessibilityIterators;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.AccessibilityIterators;
import android.widget.Editor;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.util.FastMath;
import com.android.internal.widget.EditableInputConnection;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParserException;

@RemoteViews.RemoteView
public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {
    private static final int ANIMATED_SCROLL_GAP = 250;
    private static final int CHANGE_WATCHER_PRIORITY = 100;
    static final boolean DEBUG_EXTRACT = false;
    private static final int DECIMAL = 4;
    private static final int EMS = 1;
    static final int ID_COPY = 16908321;
    static final int ID_CUT = 16908320;
    static final int ID_PASTE = 16908322;
    static final int ID_SELECT_ALL = 16908319;
    static long LAST_CUT_OR_COPY_TIME = 0;
    private static final int LINES = 1;
    static final String LOG_TAG = "TextView";
    private static final int MARQUEE_FADE_NORMAL = 0;
    private static final int MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS = 1;
    private static final int MARQUEE_FADE_SWITCH_SHOW_FADE = 2;
    private static final int MONOSPACE = 3;
    private static final int PIXELS = 2;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int SIGNED = 2;
    private static final BoringLayout.Metrics UNKNOWN_BORING;
    private static final int VERY_WIDE = 1048576;
    private boolean mAllowTransformationLengthChange;
    private int mAutoLinkMask;
    private BoringLayout.Metrics mBoring;
    private BufferType mBufferType;
    private ChangeWatcher mChangeWatcher;
    private CharWrapper mCharWrapper;
    private int mCurHintTextColor;

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    private int mCurTextColor;
    private volatile Locale mCurrentSpellCheckerLocaleCache;
    int mCursorDrawableRes;
    private int mDeferScroll;
    private int mDesiredHeightAtMeasure;
    private boolean mDispatchTemporaryDetach;
    Drawables mDrawables;
    private Editable.Factory mEditableFactory;
    private Editor mEditor;
    private TextUtils.TruncateAt mEllipsize;
    private InputFilter[] mFilters;
    private boolean mFreezesText;
    private int mGravity;
    int mHighlightColor;
    private final Paint mHighlightPaint;
    private Path mHighlightPath;
    private boolean mHighlightPathBogus;
    private CharSequence mHint;
    private BoringLayout.Metrics mHintBoring;
    private Layout mHintLayout;
    private ColorStateList mHintTextColor;
    private boolean mHorizontallyScrolling;
    private boolean mIncludePad;
    private int mLastLayoutDirection;
    private long mLastScroll;
    private Layout mLayout;
    private ColorStateList mLinkTextColor;
    private boolean mLinksClickable;
    private ArrayList<TextWatcher> mListeners;
    private Marquee mMarquee;
    private int mMarqueeFadeMode;
    private int mMarqueeRepeatLimit;
    private int mMaxMode;
    private int mMaxWidth;
    private int mMaxWidthMode;
    private int mMaximum;
    private int mMinMode;
    private int mMinWidth;
    private int mMinWidthMode;
    private int mMinimum;
    private MovementMethod mMovement;
    private int mOldMaxMode;
    private int mOldMaximum;
    private boolean mPreDrawListenerDetached;
    private boolean mPreDrawRegistered;
    private boolean mPreventDefaultMovement;
    private boolean mRestartMarquee;
    private BoringLayout mSavedHintLayout;
    private BoringLayout mSavedLayout;
    private Layout mSavedMarqueeModeLayout;
    private Scroller mScroller;
    private int mShadowColor;
    private float mShadowDx;
    private float mShadowDy;
    private float mShadowRadius;
    private boolean mSingleLine;
    private float mSpacingAdd;
    private float mSpacingMult;
    private Spannable.Factory mSpannableFactory;
    private Rect mTempRect;
    boolean mTemporaryDetach;

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    private CharSequence mText;
    private ColorStateList mTextColor;
    private TextDirectionHeuristic mTextDir;
    int mTextEditSuggestionItemLayout;
    private final TextPaint mTextPaint;
    int mTextSelectHandleLeftRes;
    int mTextSelectHandleRes;
    int mTextSelectHandleRightRes;
    private TransformationMethod mTransformation;
    private CharSequence mTransformed;
    private boolean mUserSetTextScaleX;
    private static final RectF TEMP_RECTF = new RectF();
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString(ProxyInfo.LOCAL_EXCL_LIST);
    private static final int[] MULTILINE_STATE_SET = {16843597};

    public enum BufferType {
        NORMAL,
        SPANNABLE,
        EDITABLE
    }

    public interface OnEditorActionListener {
        boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent);
    }

    static {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.measureText("H");
        UNKNOWN_BORING = new BoringLayout.Metrics();
    }

    static class Drawables {
        static final int DRAWABLE_LEFT = 1;
        static final int DRAWABLE_NONE = -1;
        static final int DRAWABLE_RIGHT = 0;
        Drawable mDrawableBottom;
        Drawable mDrawableEnd;
        Drawable mDrawableError;
        int mDrawableHeightEnd;
        int mDrawableHeightError;
        int mDrawableHeightLeft;
        int mDrawableHeightRight;
        int mDrawableHeightStart;
        int mDrawableHeightTemp;
        Drawable mDrawableLeft;
        Drawable mDrawableLeftInitial;
        int mDrawablePadding;
        Drawable mDrawableRight;
        Drawable mDrawableRightInitial;
        int mDrawableSizeBottom;
        int mDrawableSizeEnd;
        int mDrawableSizeError;
        int mDrawableSizeLeft;
        int mDrawableSizeRight;
        int mDrawableSizeStart;
        int mDrawableSizeTemp;
        int mDrawableSizeTop;
        Drawable mDrawableStart;
        Drawable mDrawableTemp;
        Drawable mDrawableTop;
        int mDrawableWidthBottom;
        int mDrawableWidthTop;
        boolean mIsRtlCompatibilityMode;
        boolean mOverride;
        final Rect mCompoundRect = new Rect();
        int mDrawableSaved = -1;

        public Drawables(Context context) {
            int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
            this.mIsRtlCompatibilityMode = targetSdkVersion < 17 || !context.getApplicationInfo().hasRtlSupport();
            this.mOverride = false;
        }

        public void resolveWithLayoutDirection(int layoutDirection) {
            this.mDrawableLeft = this.mDrawableLeftInitial;
            this.mDrawableRight = this.mDrawableRightInitial;
            if (this.mIsRtlCompatibilityMode) {
                if (this.mDrawableStart != null && this.mDrawableLeft == null) {
                    this.mDrawableLeft = this.mDrawableStart;
                    this.mDrawableSizeLeft = this.mDrawableSizeStart;
                    this.mDrawableHeightLeft = this.mDrawableHeightStart;
                }
                if (this.mDrawableEnd != null && this.mDrawableRight == null) {
                    this.mDrawableRight = this.mDrawableEnd;
                    this.mDrawableSizeRight = this.mDrawableSizeEnd;
                    this.mDrawableHeightRight = this.mDrawableHeightEnd;
                }
            } else {
                switch (layoutDirection) {
                    case 1:
                        if (this.mOverride) {
                            this.mDrawableRight = this.mDrawableStart;
                            this.mDrawableSizeRight = this.mDrawableSizeStart;
                            this.mDrawableHeightRight = this.mDrawableHeightStart;
                            this.mDrawableLeft = this.mDrawableEnd;
                            this.mDrawableSizeLeft = this.mDrawableSizeEnd;
                            this.mDrawableHeightLeft = this.mDrawableHeightEnd;
                        }
                        break;
                    default:
                        if (this.mOverride) {
                            this.mDrawableLeft = this.mDrawableStart;
                            this.mDrawableSizeLeft = this.mDrawableSizeStart;
                            this.mDrawableHeightLeft = this.mDrawableHeightStart;
                            this.mDrawableRight = this.mDrawableEnd;
                            this.mDrawableSizeRight = this.mDrawableSizeEnd;
                            this.mDrawableHeightRight = this.mDrawableHeightEnd;
                        }
                        break;
                }
            }
            applyErrorDrawableIfNeeded(layoutDirection);
            updateDrawablesLayoutDirection(layoutDirection);
        }

        private void updateDrawablesLayoutDirection(int layoutDirection) {
            if (this.mDrawableLeft != null) {
                this.mDrawableLeft.setLayoutDirection(layoutDirection);
            }
            if (this.mDrawableRight != null) {
                this.mDrawableRight.setLayoutDirection(layoutDirection);
            }
            if (this.mDrawableTop != null) {
                this.mDrawableTop.setLayoutDirection(layoutDirection);
            }
            if (this.mDrawableBottom != null) {
                this.mDrawableBottom.setLayoutDirection(layoutDirection);
            }
        }

        public void setErrorDrawable(Drawable dr, TextView tv) {
            if (this.mDrawableError != dr && this.mDrawableError != null) {
                this.mDrawableError.setCallback(null);
            }
            this.mDrawableError = dr;
            Rect compoundRect = this.mCompoundRect;
            int[] state = tv.getDrawableState();
            if (this.mDrawableError != null) {
                this.mDrawableError.setState(state);
                this.mDrawableError.copyBounds(compoundRect);
                this.mDrawableError.setCallback(tv);
                this.mDrawableSizeError = compoundRect.width();
                this.mDrawableHeightError = compoundRect.height();
                return;
            }
            this.mDrawableHeightError = 0;
            this.mDrawableSizeError = 0;
        }

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            switch (this.mDrawableSaved) {
                case 0:
                    this.mDrawableRight = this.mDrawableTemp;
                    this.mDrawableSizeRight = this.mDrawableSizeTemp;
                    this.mDrawableHeightRight = this.mDrawableHeightTemp;
                    break;
                case 1:
                    this.mDrawableLeft = this.mDrawableTemp;
                    this.mDrawableSizeLeft = this.mDrawableSizeTemp;
                    this.mDrawableHeightLeft = this.mDrawableHeightTemp;
                    break;
            }
            if (this.mDrawableError != null) {
                switch (layoutDirection) {
                    case 1:
                        this.mDrawableSaved = 1;
                        this.mDrawableTemp = this.mDrawableLeft;
                        this.mDrawableSizeTemp = this.mDrawableSizeLeft;
                        this.mDrawableHeightTemp = this.mDrawableHeightLeft;
                        this.mDrawableLeft = this.mDrawableError;
                        this.mDrawableSizeLeft = this.mDrawableSizeError;
                        this.mDrawableHeightLeft = this.mDrawableHeightError;
                        break;
                    default:
                        this.mDrawableSaved = 0;
                        this.mDrawableTemp = this.mDrawableRight;
                        this.mDrawableSizeTemp = this.mDrawableSizeRight;
                        this.mDrawableHeightTemp = this.mDrawableHeightRight;
                        this.mDrawableRight = this.mDrawableError;
                        this.mDrawableSizeRight = this.mDrawableSizeError;
                        this.mDrawableHeightRight = this.mDrawableHeightError;
                        break;
                }
            }
        }
    }

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842884);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TextKeyListener.Capitalize cap;
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mEditableFactory = Editable.Factory.getInstance();
        this.mSpannableFactory = Spannable.Factory.getInstance();
        this.mMarqueeRepeatLimit = 3;
        this.mLastLayoutDirection = -1;
        this.mMarqueeFadeMode = 0;
        this.mBufferType = BufferType.NORMAL;
        this.mGravity = 8388659;
        this.mLinksClickable = true;
        this.mSpacingMult = 1.0f;
        this.mSpacingAdd = 0.0f;
        this.mMaximum = Integer.MAX_VALUE;
        this.mMaxMode = 1;
        this.mMinimum = 0;
        this.mMinMode = 1;
        this.mOldMaximum = this.mMaximum;
        this.mOldMaxMode = this.mMaxMode;
        this.mMaxWidth = Integer.MAX_VALUE;
        this.mMaxWidthMode = 2;
        this.mMinWidth = 0;
        this.mMinWidthMode = 2;
        this.mDesiredHeightAtMeasure = -1;
        this.mIncludePad = true;
        this.mDeferScroll = -1;
        this.mFilters = NO_FILTERS;
        this.mHighlightColor = 1714664933;
        this.mHighlightPathBogus = true;
        this.mText = ProxyInfo.LOCAL_EXCL_LIST;
        Resources res = getResources();
        CompatibilityInfo compat = res.getCompatibilityInfo();
        this.mTextPaint = new TextPaint(1);
        this.mTextPaint.density = res.getDisplayMetrics().density;
        this.mTextPaint.setCompatibilityScaling(compat.applicationScale);
        this.mHighlightPaint = new Paint(1);
        this.mHighlightPaint.setCompatibilityScaling(compat.applicationScale);
        this.mMovement = getDefaultMovementMethod();
        this.mTransformation = null;
        int textColorHighlight = 0;
        ColorStateList textColor = null;
        ColorStateList textColorHint = null;
        ColorStateList textColorLink = null;
        int textSize = 15;
        String fontFamily = null;
        boolean fontFamilyExplicit = false;
        int typefaceIndex = -1;
        int styleIndex = -1;
        boolean allCaps = false;
        int shadowcolor = 0;
        float dx = 0.0f;
        float dy = 0.0f;
        float r = 0.0f;
        boolean elegant = false;
        float letterSpacing = 0.0f;
        String fontFeatureSettings = null;
        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.TextViewAppearance, defStyleAttr, defStyleRes);
        int ap = a.getResourceId(0, -1);
        a.recycle();
        TypedArray appearance = ap != -1 ? theme.obtainStyledAttributes(ap, R.styleable.TextAppearance) : null;
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                switch (attr) {
                    case 0:
                        textSize = appearance.getDimensionPixelSize(attr, textSize);
                        break;
                    case 1:
                        typefaceIndex = appearance.getInt(attr, -1);
                        break;
                    case 2:
                        styleIndex = appearance.getInt(attr, -1);
                        break;
                    case 3:
                        textColor = appearance.getColorStateList(attr);
                        break;
                    case 4:
                        textColorHighlight = appearance.getColor(attr, textColorHighlight);
                        break;
                    case 5:
                        textColorHint = appearance.getColorStateList(attr);
                        break;
                    case 6:
                        textColorLink = appearance.getColorStateList(attr);
                        break;
                    case 7:
                        shadowcolor = appearance.getInt(attr, 0);
                        break;
                    case 8:
                        dx = appearance.getFloat(attr, 0.0f);
                        break;
                    case 9:
                        dy = appearance.getFloat(attr, 0.0f);
                        break;
                    case 10:
                        r = appearance.getFloat(attr, 0.0f);
                        break;
                    case 11:
                        allCaps = appearance.getBoolean(attr, false);
                        break;
                    case 12:
                        fontFamily = appearance.getString(attr);
                        break;
                    case 13:
                        elegant = appearance.getBoolean(attr, false);
                        break;
                    case 14:
                        letterSpacing = appearance.getFloat(attr, 0.0f);
                        break;
                    case 15:
                        fontFeatureSettings = appearance.getString(attr);
                        break;
                }
            }
            appearance.recycle();
        }
        boolean editable = getDefaultEditable();
        CharSequence inputMethod = null;
        int numeric = 0;
        CharSequence digits = null;
        boolean phone = false;
        boolean autotext = false;
        int autocap = -1;
        int buffertype = 0;
        boolean selectallonfocus = false;
        Drawable drawableLeft = null;
        Drawable drawableTop = null;
        Drawable drawableRight = null;
        Drawable drawableBottom = null;
        Drawable drawableStart = null;
        Drawable drawableEnd = null;
        int drawablePadding = 0;
        int ellipsize = -1;
        boolean singleLine = false;
        int maxlength = -1;
        CharSequence text = ProxyInfo.LOCAL_EXCL_LIST;
        CharSequence hint = null;
        boolean password = false;
        int inputType = 0;
        TypedArray a2 = theme.obtainStyledAttributes(attrs, R.styleable.TextView, defStyleAttr, defStyleRes);
        int n2 = a2.getIndexCount();
        for (int i2 = 0; i2 < n2; i2++) {
            int attr2 = a2.getIndex(i2);
            switch (attr2) {
                case 0:
                    setEnabled(a2.getBoolean(attr2, isEnabled()));
                    break;
                case 2:
                    textSize = a2.getDimensionPixelSize(attr2, textSize);
                    break;
                case 3:
                    typefaceIndex = a2.getInt(attr2, typefaceIndex);
                    break;
                case 4:
                    styleIndex = a2.getInt(attr2, styleIndex);
                    break;
                case 5:
                    textColor = a2.getColorStateList(attr2);
                    break;
                case 6:
                    textColorHighlight = a2.getColor(attr2, textColorHighlight);
                    break;
                case 7:
                    textColorHint = a2.getColorStateList(attr2);
                    break;
                case 8:
                    textColorLink = a2.getColorStateList(attr2);
                    break;
                case 9:
                    ellipsize = a2.getInt(attr2, ellipsize);
                    break;
                case 10:
                    setGravity(a2.getInt(attr2, -1));
                    break;
                case 11:
                    this.mAutoLinkMask = a2.getInt(attr2, 0);
                    break;
                case 12:
                    this.mLinksClickable = a2.getBoolean(attr2, true);
                    break;
                case 13:
                    setMaxWidth(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 14:
                    setMaxHeight(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 15:
                    setMinWidth(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 16:
                    setMinHeight(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 17:
                    buffertype = a2.getInt(attr2, buffertype);
                    break;
                case 18:
                    text = a2.getText(attr2);
                    break;
                case 19:
                    hint = a2.getText(attr2);
                    break;
                case 20:
                    setTextScaleX(a2.getFloat(attr2, 1.0f));
                    break;
                case 21:
                    if (!a2.getBoolean(attr2, true)) {
                        setCursorVisible(false);
                    }
                    break;
                case 22:
                    setMaxLines(a2.getInt(attr2, -1));
                    break;
                case 23:
                    setLines(a2.getInt(attr2, -1));
                    break;
                case 24:
                    setHeight(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 25:
                    setMinLines(a2.getInt(attr2, -1));
                    break;
                case 26:
                    setMaxEms(a2.getInt(attr2, -1));
                    break;
                case 27:
                    setEms(a2.getInt(attr2, -1));
                    break;
                case 28:
                    setWidth(a2.getDimensionPixelSize(attr2, -1));
                    break;
                case 29:
                    setMinEms(a2.getInt(attr2, -1));
                    break;
                case 30:
                    if (a2.getBoolean(attr2, false)) {
                        setHorizontallyScrolling(true);
                    }
                    break;
                case 31:
                    password = a2.getBoolean(attr2, password);
                    break;
                case 32:
                    singleLine = a2.getBoolean(attr2, singleLine);
                    break;
                case 33:
                    selectallonfocus = a2.getBoolean(attr2, selectallonfocus);
                    break;
                case 34:
                    if (!a2.getBoolean(attr2, true)) {
                        setIncludeFontPadding(false);
                    }
                    break;
                case 35:
                    maxlength = a2.getInt(attr2, -1);
                    break;
                case 36:
                    shadowcolor = a2.getInt(attr2, 0);
                    break;
                case 37:
                    dx = a2.getFloat(attr2, 0.0f);
                    break;
                case 38:
                    dy = a2.getFloat(attr2, 0.0f);
                    break;
                case 39:
                    r = a2.getFloat(attr2, 0.0f);
                    break;
                case 40:
                    numeric = a2.getInt(attr2, numeric);
                    break;
                case 41:
                    digits = a2.getText(attr2);
                    break;
                case 42:
                    phone = a2.getBoolean(attr2, phone);
                    break;
                case 43:
                    inputMethod = a2.getText(attr2);
                    break;
                case 44:
                    autocap = a2.getInt(attr2, autocap);
                    break;
                case 45:
                    autotext = a2.getBoolean(attr2, autotext);
                    break;
                case 46:
                    editable = a2.getBoolean(attr2, editable);
                    break;
                case 47:
                    this.mFreezesText = a2.getBoolean(attr2, false);
                    break;
                case 48:
                    drawableTop = a2.getDrawable(attr2);
                    break;
                case 49:
                    drawableBottom = a2.getDrawable(attr2);
                    break;
                case 50:
                    drawableLeft = a2.getDrawable(attr2);
                    break;
                case 51:
                    drawableRight = a2.getDrawable(attr2);
                    break;
                case 52:
                    drawablePadding = a2.getDimensionPixelSize(attr2, drawablePadding);
                    break;
                case 53:
                    this.mSpacingAdd = a2.getDimensionPixelSize(attr2, (int) this.mSpacingAdd);
                    break;
                case 54:
                    this.mSpacingMult = a2.getFloat(attr2, this.mSpacingMult);
                    break;
                case 55:
                    setMarqueeRepeatLimit(a2.getInt(attr2, this.mMarqueeRepeatLimit));
                    break;
                case 56:
                    inputType = a2.getInt(attr2, 0);
                    break;
                case 57:
                    setPrivateImeOptions(a2.getString(attr2));
                    break;
                case 58:
                    try {
                        setInputExtras(a2.getResourceId(attr2, 0));
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Failure reading input extras", e);
                    } catch (XmlPullParserException e2) {
                        Log.w(LOG_TAG, "Failure reading input extras", e2);
                    }
                    break;
                case 59:
                    createEditorIfNeeded();
                    this.mEditor.createInputContentTypeIfNeeded();
                    this.mEditor.mInputContentType.imeOptions = a2.getInt(attr2, this.mEditor.mInputContentType.imeOptions);
                    break;
                case 60:
                    createEditorIfNeeded();
                    this.mEditor.createInputContentTypeIfNeeded();
                    this.mEditor.mInputContentType.imeActionLabel = a2.getText(attr2);
                    break;
                case 61:
                    createEditorIfNeeded();
                    this.mEditor.createInputContentTypeIfNeeded();
                    this.mEditor.mInputContentType.imeActionId = a2.getInt(attr2, this.mEditor.mInputContentType.imeActionId);
                    break;
                case 62:
                    this.mTextSelectHandleLeftRes = a2.getResourceId(attr2, 0);
                    break;
                case 63:
                    this.mTextSelectHandleRightRes = a2.getResourceId(attr2, 0);
                    break;
                case 64:
                    this.mTextSelectHandleRes = a2.getResourceId(attr2, 0);
                    break;
                case 67:
                    setTextIsSelectable(a2.getBoolean(attr2, false));
                    break;
                case 70:
                    this.mCursorDrawableRes = a2.getResourceId(attr2, 0);
                    break;
                case 71:
                    this.mTextEditSuggestionItemLayout = a2.getResourceId(attr2, 0);
                    break;
                case 72:
                    allCaps = a2.getBoolean(attr2, false);
                    break;
                case 73:
                    drawableStart = a2.getDrawable(attr2);
                    break;
                case 74:
                    drawableEnd = a2.getDrawable(attr2);
                    break;
                case 75:
                    fontFamily = a2.getString(attr2);
                    fontFamilyExplicit = true;
                    break;
                case 76:
                    elegant = a2.getBoolean(attr2, false);
                    break;
                case 77:
                    letterSpacing = a2.getFloat(attr2, 0.0f);
                    break;
                case 78:
                    fontFeatureSettings = a2.getString(attr2);
                    break;
            }
        }
        a2.recycle();
        BufferType bufferType = BufferType.EDITABLE;
        int variation = inputType & FileObserver.ALL_EVENTS;
        boolean passwordInputType = variation == 129;
        boolean webPasswordInputType = variation == 225;
        boolean numberPasswordInputType = variation == 18;
        if (inputMethod != null) {
            try {
                Class<?> c = Class.forName(inputMethod.toString());
                try {
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = (KeyListener) c.newInstance();
                    try {
                        this.mEditor.mInputType = inputType != 0 ? inputType : this.mEditor.mKeyListener.getInputType();
                    } catch (IncompatibleClassChangeError e3) {
                        this.mEditor.mInputType = 1;
                    }
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InstantiationException ex2) {
                    throw new RuntimeException(ex2);
                }
            } catch (ClassNotFoundException ex3) {
                throw new RuntimeException(ex3);
            }
        } else if (digits != null) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = DigitsKeyListener.getInstance(digits.toString());
            this.mEditor.mInputType = inputType != 0 ? inputType : 1;
        } else if (inputType != 0) {
            setInputType(inputType, true);
            singleLine = !isMultilineInputType(inputType);
        } else if (phone) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = DialerKeyListener.getInstance();
            this.mEditor.mInputType = 3;
        } else if (numeric != 0) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = DigitsKeyListener.getInstance((numeric & 2) != 0, (numeric & 4) != 0);
            int inputType2 = (numeric & 2) != 0 ? 2 | 4096 : 2;
            this.mEditor.mInputType = (numeric & 4) != 0 ? inputType2 | 8192 : inputType2;
        } else if (autotext || autocap != -1) {
            int inputType3 = 1;
            switch (autocap) {
                case 1:
                    cap = TextKeyListener.Capitalize.SENTENCES;
                    inputType3 = 1 | 16384;
                    break;
                case 2:
                    cap = TextKeyListener.Capitalize.WORDS;
                    inputType3 = 1 | 8192;
                    break;
                case 3:
                    cap = TextKeyListener.Capitalize.CHARACTERS;
                    inputType3 = 1 | 4096;
                    break;
                default:
                    cap = TextKeyListener.Capitalize.NONE;
                    break;
            }
            createEditorIfNeeded();
            this.mEditor.mKeyListener = TextKeyListener.getInstance(autotext, cap);
            this.mEditor.mInputType = inputType3;
        } else if (isTextSelectable()) {
            if (this.mEditor != null) {
                this.mEditor.mKeyListener = null;
                this.mEditor.mInputType = 0;
            }
            bufferType = BufferType.SPANNABLE;
            setMovementMethod(ArrowKeyMovementMethod.getInstance());
        } else if (editable) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = TextKeyListener.getInstance();
            this.mEditor.mInputType = 1;
        } else {
            if (this.mEditor != null) {
                this.mEditor.mKeyListener = null;
            }
            switch (buffertype) {
                case 0:
                    bufferType = BufferType.NORMAL;
                    break;
                case 1:
                    bufferType = BufferType.SPANNABLE;
                    break;
                case 2:
                    bufferType = BufferType.EDITABLE;
                    break;
            }
        }
        if (this.mEditor != null) {
            this.mEditor.adjustInputType(password, passwordInputType, webPasswordInputType, numberPasswordInputType);
        }
        if (selectallonfocus) {
            createEditorIfNeeded();
            this.mEditor.mSelectAllOnFocus = true;
            if (bufferType == BufferType.NORMAL) {
                bufferType = BufferType.SPANNABLE;
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
        setRelativeDrawablesIfNeeded(drawableStart, drawableEnd);
        setCompoundDrawablePadding(drawablePadding);
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine, singleLine, singleLine);
        if (singleLine && getKeyListener() == null && ellipsize < 0) {
            ellipsize = 3;
        }
        switch (ellipsize) {
            case 1:
                setEllipsize(TextUtils.TruncateAt.START);
                break;
            case 2:
                setEllipsize(TextUtils.TruncateAt.MIDDLE);
                break;
            case 3:
                setEllipsize(TextUtils.TruncateAt.END);
                break;
            case 4:
                if (ViewConfiguration.get(context).isFadingMarqueeEnabled()) {
                    setHorizontalFadingEdgeEnabled(true);
                    this.mMarqueeFadeMode = 0;
                } else {
                    setHorizontalFadingEdgeEnabled(false);
                    this.mMarqueeFadeMode = 1;
                }
                setEllipsize(TextUtils.TruncateAt.MARQUEE);
                break;
        }
        setTextColor(textColor == null ? ColorStateList.valueOf(-16777216) : textColor);
        setHintTextColor(textColorHint);
        setLinkTextColor(textColorLink);
        if (textColorHighlight != 0) {
            setHighlightColor(textColorHighlight);
        }
        setRawTextSize(textSize);
        setElegantTextHeight(elegant);
        setLetterSpacing(letterSpacing);
        setFontFeatureSettings(fontFeatureSettings);
        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
        if (password || passwordInputType || webPasswordInputType || numberPasswordInputType) {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            typefaceIndex = 3;
        } else if (this.mEditor != null && (this.mEditor.mInputType & FileObserver.ALL_EVENTS) == 129) {
            typefaceIndex = 3;
        }
        if (typefaceIndex != -1 && !fontFamilyExplicit) {
            fontFamily = null;
        }
        setTypefaceFromAttrs(fontFamily, typefaceIndex, styleIndex);
        if (shadowcolor != 0) {
            setShadowLayer(r, dx, dy, shadowcolor);
        }
        if (maxlength >= 0) {
            setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxlength)});
        } else {
            setFilters(NO_FILTERS);
        }
        setText(text, bufferType);
        if (hint != null) {
            setHint(hint);
        }
        TypedArray a3 = context.obtainStyledAttributes(attrs, R.styleable.View, defStyleAttr, defStyleRes);
        boolean focusable = (this.mMovement == null && getKeyListener() == null) ? false : true;
        boolean clickable = focusable || isClickable();
        boolean longClickable = focusable || isLongClickable();
        int n3 = a3.getIndexCount();
        for (int i3 = 0; i3 < n3; i3++) {
            int attr3 = a3.getIndex(i3);
            switch (attr3) {
                case 19:
                    focusable = a3.getBoolean(attr3, focusable);
                    break;
                case 30:
                    clickable = a3.getBoolean(attr3, clickable);
                    break;
                case 31:
                    longClickable = a3.getBoolean(attr3, longClickable);
                    break;
            }
        }
        a3.recycle();
        setFocusable(focusable);
        setClickable(clickable);
        setLongClickable(longClickable);
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    private void setTypefaceFromAttrs(String familyName, int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        if (familyName != null && (tf = Typeface.create(familyName, styleIndex)) != null) {
            setTypeface(tf);
            return;
        }
        switch (typefaceIndex) {
            case 1:
                tf = Typeface.SANS_SERIF;
                break;
            case 2:
                tf = Typeface.SERIF;
                break;
            case 3:
                tf = Typeface.MONOSPACE;
                break;
        }
        setTypeface(tf, styleIndex);
    }

    private void setRelativeDrawablesIfNeeded(Drawable start, Drawable end) {
        boolean hasRelativeDrawables = (start == null && end == null) ? false : true;
        if (hasRelativeDrawables) {
            Drawables dr = this.mDrawables;
            if (dr == null) {
                dr = new Drawables(getContext());
                this.mDrawables = dr;
            }
            this.mDrawables.mOverride = true;
            Rect compoundRect = dr.mCompoundRect;
            int[] state = getDrawableState();
            if (start != null) {
                start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);
                dr.mDrawableStart = start;
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableHeightStart = 0;
                dr.mDrawableSizeStart = 0;
            }
            if (end != null) {
                end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);
                dr.mDrawableEnd = end;
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableHeightEnd = 0;
                dr.mDrawableSizeEnd = 0;
            }
            resetResolvedDrawables();
            resolveDrawables();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        InputMethodManager imm;
        InputMethodManager imm2;
        if (enabled != isEnabled()) {
            if (!enabled && (imm2 = InputMethodManager.peekInstance()) != null && imm2.isActive(this)) {
                imm2.hideSoftInputFromWindow(getWindowToken(), 0);
            }
            super.setEnabled(enabled);
            if (enabled && (imm = InputMethodManager.peekInstance()) != null) {
                imm.restartInput(this);
            }
            if (this.mEditor != null) {
                this.mEditor.invalidateTextDisplayList();
                this.mEditor.prepareCursorControllers();
                this.mEditor.makeBlink();
            }
        }
    }

    public void setTypeface(Typeface tf, int style) {
        Typeface tf2;
        if (style > 0) {
            if (tf == null) {
                tf2 = Typeface.defaultFromStyle(style);
            } else {
                tf2 = Typeface.create(tf, style);
            }
            setTypeface(tf2);
            int typefaceStyle = tf2 != null ? tf2.getStyle() : 0;
            int need = style & (typefaceStyle ^ (-1));
            this.mTextPaint.setFakeBoldText((need & 1) != 0);
            this.mTextPaint.setTextSkewX((need & 2) != 0 ? -0.25f : 0.0f);
            return;
        }
        this.mTextPaint.setFakeBoldText(false);
        this.mTextPaint.setTextSkewX(0.0f);
        setTypeface(tf);
    }

    protected boolean getDefaultEditable() {
        return false;
    }

    protected MovementMethod getDefaultMovementMethod() {
        return null;
    }

    @ViewDebug.CapturedViewProperty
    public CharSequence getText() {
        return this.mText;
    }

    public int length() {
        return this.mText.length();
    }

    public Editable getEditableText() {
        if (this.mText instanceof Editable) {
            return (Editable) this.mText;
        }
        return null;
    }

    public int getLineHeight() {
        return FastMath.round((this.mTextPaint.getFontMetricsInt(null) * this.mSpacingMult) + this.mSpacingAdd);
    }

    public final Layout getLayout() {
        return this.mLayout;
    }

    final Layout getHintLayout() {
        return this.mHintLayout;
    }

    public final UndoManager getUndoManager() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mUndoManager;
    }

    public final void setUndoManager(UndoManager undoManager, String tag) {
        if (undoManager != null) {
            createEditorIfNeeded();
            this.mEditor.mUndoManager = undoManager;
            this.mEditor.mUndoOwner = undoManager.getOwner(tag, this);
            this.mEditor.mUndoInputFilter = new Editor.UndoInputFilter(this.mEditor);
            if (!(this.mText instanceof Editable)) {
                setText(this.mText, BufferType.EDITABLE);
            }
            setFilters((Editable) this.mText, this.mFilters);
            return;
        }
        if (this.mEditor != null) {
            this.mEditor.mUndoManager = null;
            this.mEditor.mUndoOwner = null;
            this.mEditor.mUndoInputFilter = null;
        }
    }

    public final KeyListener getKeyListener() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mKeyListener;
    }

    public void setKeyListener(KeyListener input) {
        setKeyListenerOnly(input);
        fixFocusableAndClickableSettings();
        if (input != null) {
            createEditorIfNeeded();
            try {
                this.mEditor.mInputType = this.mEditor.mKeyListener.getInputType();
            } catch (IncompatibleClassChangeError e) {
                this.mEditor.mInputType = 1;
            }
            setInputTypeSingleLine(this.mSingleLine);
        } else if (this.mEditor != null) {
            this.mEditor.mInputType = 0;
        }
        InputMethodManager imm = InputMethodManager.peekInstance();
        if (imm != null) {
            imm.restartInput(this);
        }
    }

    private void setKeyListenerOnly(KeyListener input) {
        if (this.mEditor != null || input != null) {
            createEditorIfNeeded();
            if (this.mEditor.mKeyListener != input) {
                this.mEditor.mKeyListener = input;
                if (input != null && !(this.mText instanceof Editable)) {
                    setText(this.mText);
                }
                setFilters((Editable) this.mText, this.mFilters);
            }
        }
    }

    public final MovementMethod getMovementMethod() {
        return this.mMovement;
    }

    public final void setMovementMethod(MovementMethod movement) {
        if (this.mMovement != movement) {
            this.mMovement = movement;
            if (movement != null && !(this.mText instanceof Spannable)) {
                setText(this.mText);
            }
            fixFocusableAndClickableSettings();
            if (this.mEditor != null) {
                this.mEditor.prepareCursorControllers();
            }
        }
    }

    private void fixFocusableAndClickableSettings() {
        if (this.mMovement != null || (this.mEditor != null && this.mEditor.mKeyListener != null)) {
            setFocusable(true);
            setClickable(true);
            setLongClickable(true);
        } else {
            setFocusable(false);
            setClickable(false);
            setLongClickable(false);
        }
    }

    public final TransformationMethod getTransformationMethod() {
        return this.mTransformation;
    }

    public final void setTransformationMethod(TransformationMethod method) {
        if (method != this.mTransformation) {
            if (this.mTransformation != null && (this.mText instanceof Spannable)) {
                ((Spannable) this.mText).removeSpan(this.mTransformation);
            }
            this.mTransformation = method;
            if (method instanceof TransformationMethod2) {
                TransformationMethod2 method2 = (TransformationMethod2) method;
                this.mAllowTransformationLengthChange = (isTextSelectable() || (this.mText instanceof Editable)) ? false : true;
                method2.setLengthChangesAllowed(this.mAllowTransformationLengthChange);
            } else {
                this.mAllowTransformationLengthChange = false;
            }
            setText(this.mText);
            if (hasPasswordTransformationMethod()) {
                notifyViewAccessibilityStateChangedIfNeeded(0);
            }
        }
    }

    public int getCompoundPaddingTop() {
        Drawables dr = this.mDrawables;
        return (dr == null || dr.mDrawableTop == null) ? this.mPaddingTop : this.mPaddingTop + dr.mDrawablePadding + dr.mDrawableSizeTop;
    }

    public int getCompoundPaddingBottom() {
        Drawables dr = this.mDrawables;
        return (dr == null || dr.mDrawableBottom == null) ? this.mPaddingBottom : this.mPaddingBottom + dr.mDrawablePadding + dr.mDrawableSizeBottom;
    }

    public int getCompoundPaddingLeft() {
        Drawables dr = this.mDrawables;
        return (dr == null || dr.mDrawableLeft == null) ? this.mPaddingLeft : this.mPaddingLeft + dr.mDrawablePadding + dr.mDrawableSizeLeft;
    }

    public int getCompoundPaddingRight() {
        Drawables dr = this.mDrawables;
        return (dr == null || dr.mDrawableRight == null) ? this.mPaddingRight : this.mPaddingRight + dr.mDrawablePadding + dr.mDrawableSizeRight;
    }

    public int getCompoundPaddingStart() {
        resolveDrawables();
        switch (getLayoutDirection()) {
            case 1:
                return getCompoundPaddingRight();
            default:
                return getCompoundPaddingLeft();
        }
    }

    public int getCompoundPaddingEnd() {
        resolveDrawables();
        switch (getLayoutDirection()) {
            case 1:
                return getCompoundPaddingLeft();
            default:
                return getCompoundPaddingRight();
        }
    }

    public int getExtendedPaddingTop() {
        int gravity;
        if (this.mMaxMode != 1) {
            return getCompoundPaddingTop();
        }
        if (this.mLayout == null) {
            assumeLayout();
        }
        if (this.mLayout.getLineCount() <= this.mMaximum) {
            return getCompoundPaddingTop();
        }
        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = (getHeight() - top) - bottom;
        int layoutht = this.mLayout.getLineTop(this.mMaximum);
        if (layoutht < viewht && (gravity = this.mGravity & 112) != 48) {
            if (gravity == 80) {
                return (top + viewht) - layoutht;
            }
            return top + ((viewht - layoutht) / 2);
        }
        return top;
    }

    public int getExtendedPaddingBottom() {
        if (this.mMaxMode != 1) {
            return getCompoundPaddingBottom();
        }
        if (this.mLayout == null) {
            assumeLayout();
        }
        if (this.mLayout.getLineCount() <= this.mMaximum) {
            return getCompoundPaddingBottom();
        }
        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = (getHeight() - top) - bottom;
        int layoutht = this.mLayout.getLineTop(this.mMaximum);
        if (layoutht < viewht) {
            int gravity = this.mGravity & 112;
            if (gravity == 48) {
                return (bottom + viewht) - layoutht;
            }
            return gravity != 80 ? bottom + ((viewht - layoutht) / 2) : bottom;
        }
        return bottom;
    }

    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    public int getTotalPaddingStart() {
        return getCompoundPaddingStart();
    }

    public int getTotalPaddingEnd() {
        return getCompoundPaddingEnd();
    }

    public int getTotalPaddingTop() {
        return getExtendedPaddingTop() + getVerticalOffset(true);
    }

    public int getTotalPaddingBottom() {
        return getExtendedPaddingBottom() + getBottomVerticalOffset(true);
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        Drawables dr = this.mDrawables;
        if (dr != null) {
            if (dr.mDrawableStart != null) {
                dr.mDrawableStart.setCallback(null);
            }
            dr.mDrawableStart = null;
            if (dr.mDrawableEnd != null) {
                dr.mDrawableEnd.setCallback(null);
            }
            dr.mDrawableEnd = null;
            dr.mDrawableHeightStart = 0;
            dr.mDrawableSizeStart = 0;
            dr.mDrawableHeightEnd = 0;
            dr.mDrawableSizeEnd = 0;
        }
        boolean drawables = (left == null && top == null && right == null && bottom == null) ? false : true;
        if (!drawables) {
            if (dr != null) {
                if (dr.mDrawablePadding == 0) {
                    this.mDrawables = null;
                } else {
                    if (dr.mDrawableLeft != null) {
                        dr.mDrawableLeft.setCallback(null);
                    }
                    dr.mDrawableLeft = null;
                    if (dr.mDrawableTop != null) {
                        dr.mDrawableTop.setCallback(null);
                    }
                    dr.mDrawableTop = null;
                    if (dr.mDrawableRight != null) {
                        dr.mDrawableRight.setCallback(null);
                    }
                    dr.mDrawableRight = null;
                    if (dr.mDrawableBottom != null) {
                        dr.mDrawableBottom.setCallback(null);
                    }
                    dr.mDrawableBottom = null;
                    dr.mDrawableHeightLeft = 0;
                    dr.mDrawableSizeLeft = 0;
                    dr.mDrawableHeightRight = 0;
                    dr.mDrawableSizeRight = 0;
                    dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeTop = 0;
                    dr.mDrawableWidthBottom = 0;
                    dr.mDrawableSizeBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                dr = new Drawables(getContext());
                this.mDrawables = dr;
            }
            this.mDrawables.mOverride = false;
            if (dr.mDrawableLeft != left && dr.mDrawableLeft != null) {
                dr.mDrawableLeft.setCallback(null);
            }
            dr.mDrawableLeft = left;
            if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
                dr.mDrawableTop.setCallback(null);
            }
            dr.mDrawableTop = top;
            if (dr.mDrawableRight != right && dr.mDrawableRight != null) {
                dr.mDrawableRight.setCallback(null);
            }
            dr.mDrawableRight = right;
            if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setCallback(null);
            }
            dr.mDrawableBottom = bottom;
            Rect compoundRect = dr.mCompoundRect;
            int[] state = getDrawableState();
            if (left != null) {
                left.setState(state);
                left.copyBounds(compoundRect);
                left.setCallback(this);
                dr.mDrawableSizeLeft = compoundRect.width();
                dr.mDrawableHeightLeft = compoundRect.height();
            } else {
                dr.mDrawableHeightLeft = 0;
                dr.mDrawableSizeLeft = 0;
            }
            if (right != null) {
                right.setState(state);
                right.copyBounds(compoundRect);
                right.setCallback(this);
                dr.mDrawableSizeRight = compoundRect.width();
                dr.mDrawableHeightRight = compoundRect.height();
            } else {
                dr.mDrawableHeightRight = 0;
                dr.mDrawableSizeRight = 0;
            }
            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableWidthTop = 0;
                dr.mDrawableSizeTop = 0;
            }
            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableWidthBottom = 0;
                dr.mDrawableSizeBottom = 0;
            }
        }
        if (dr != null) {
            dr.mDrawableLeftInitial = left;
            dr.mDrawableRightInitial = right;
        }
        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    @RemotableViewMethod
    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(left != 0 ? context.getDrawable(left) : null, top != 0 ? context.getDrawable(top) : null, right != 0 ? context.getDrawable(right) : null, bottom != 0 ? context.getDrawable(bottom) : null);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawables(left, top, right, bottom);
    }

    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        Drawables dr = this.mDrawables;
        if (dr != null) {
            if (dr.mDrawableLeft != null) {
                dr.mDrawableLeft.setCallback(null);
            }
            dr.mDrawableLeftInitial = null;
            dr.mDrawableLeft = null;
            if (dr.mDrawableRight != null) {
                dr.mDrawableRight.setCallback(null);
            }
            dr.mDrawableRightInitial = null;
            dr.mDrawableRight = null;
            dr.mDrawableHeightLeft = 0;
            dr.mDrawableSizeLeft = 0;
            dr.mDrawableHeightRight = 0;
            dr.mDrawableSizeRight = 0;
        }
        boolean drawables = (start == null && top == null && end == null && bottom == null) ? false : true;
        if (!drawables) {
            if (dr != null) {
                if (dr.mDrawablePadding == 0) {
                    this.mDrawables = null;
                } else {
                    if (dr.mDrawableStart != null) {
                        dr.mDrawableStart.setCallback(null);
                    }
                    dr.mDrawableStart = null;
                    if (dr.mDrawableTop != null) {
                        dr.mDrawableTop.setCallback(null);
                    }
                    dr.mDrawableTop = null;
                    if (dr.mDrawableEnd != null) {
                        dr.mDrawableEnd.setCallback(null);
                    }
                    dr.mDrawableEnd = null;
                    if (dr.mDrawableBottom != null) {
                        dr.mDrawableBottom.setCallback(null);
                    }
                    dr.mDrawableBottom = null;
                    dr.mDrawableHeightStart = 0;
                    dr.mDrawableSizeStart = 0;
                    dr.mDrawableHeightEnd = 0;
                    dr.mDrawableSizeEnd = 0;
                    dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeTop = 0;
                    dr.mDrawableWidthBottom = 0;
                    dr.mDrawableSizeBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                dr = new Drawables(getContext());
                this.mDrawables = dr;
            }
            this.mDrawables.mOverride = true;
            if (dr.mDrawableStart != start && dr.mDrawableStart != null) {
                dr.mDrawableStart.setCallback(null);
            }
            dr.mDrawableStart = start;
            if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
                dr.mDrawableTop.setCallback(null);
            }
            dr.mDrawableTop = top;
            if (dr.mDrawableEnd != end && dr.mDrawableEnd != null) {
                dr.mDrawableEnd.setCallback(null);
            }
            dr.mDrawableEnd = end;
            if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setCallback(null);
            }
            dr.mDrawableBottom = bottom;
            Rect compoundRect = dr.mCompoundRect;
            int[] state = getDrawableState();
            if (start != null) {
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableHeightStart = 0;
                dr.mDrawableSizeStart = 0;
            }
            if (end != null) {
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableHeightEnd = 0;
                dr.mDrawableSizeEnd = 0;
            }
            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableWidthTop = 0;
                dr.mDrawableSizeTop = 0;
            }
            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableWidthBottom = 0;
                dr.mDrawableSizeBottom = 0;
            }
        }
        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    @RemotableViewMethod
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end, int bottom) {
        Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(start != 0 ? context.getDrawable(start) : null, top != 0 ? context.getDrawable(top) : null, end != 0 ? context.getDrawable(end) : null, bottom != 0 ? context.getDrawable(bottom) : null);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        if (start != null) {
            start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
        }
        if (end != null) {
            end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawablesRelative(start, top, end, bottom);
    }

    public Drawable[] getCompoundDrawables() {
        Drawables dr = this.mDrawables;
        return dr != null ? new Drawable[]{dr.mDrawableLeft, dr.mDrawableTop, dr.mDrawableRight, dr.mDrawableBottom} : new Drawable[]{null, null, null, null};
    }

    public Drawable[] getCompoundDrawablesRelative() {
        Drawables dr = this.mDrawables;
        return dr != null ? new Drawable[]{dr.mDrawableStart, dr.mDrawableTop, dr.mDrawableEnd, dr.mDrawableBottom} : new Drawable[]{null, null, null, null};
    }

    @RemotableViewMethod
    public void setCompoundDrawablePadding(int pad) {
        Drawables dr = this.mDrawables;
        if (pad == 0) {
            if (dr != null) {
                dr.mDrawablePadding = pad;
            }
        } else {
            if (dr == null) {
                dr = new Drawables(getContext());
                this.mDrawables = dr;
            }
            dr.mDrawablePadding = pad;
        }
        invalidate();
        requestLayout();
    }

    public int getCompoundDrawablePadding() {
        Drawables dr = this.mDrawables;
        if (dr != null) {
            return dr.mDrawablePadding;
        }
        return 0;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (left != this.mPaddingLeft || right != this.mPaddingRight || top != this.mPaddingTop || bottom != this.mPaddingBottom) {
            nullLayouts();
        }
        super.setPadding(left, top, right, bottom);
        invalidate();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        if (start != getPaddingStart() || end != getPaddingEnd() || top != this.mPaddingTop || bottom != this.mPaddingBottom) {
            nullLayouts();
        }
        super.setPaddingRelative(start, top, end, bottom);
        invalidate();
    }

    public final int getAutoLinkMask() {
        return this.mAutoLinkMask;
    }

    public void setTextAppearance(Context context, int resid) {
        TypedArray appearance = context.obtainStyledAttributes(resid, R.styleable.TextAppearance);
        int color = appearance.getColor(4, 0);
        if (color != 0) {
            setHighlightColor(color);
        }
        ColorStateList colors = appearance.getColorStateList(3);
        if (colors != null) {
            setTextColor(colors);
        }
        int ts = appearance.getDimensionPixelSize(0, 0);
        if (ts != 0) {
            setRawTextSize(ts);
        }
        ColorStateList colors2 = appearance.getColorStateList(5);
        if (colors2 != null) {
            setHintTextColor(colors2);
        }
        ColorStateList colors3 = appearance.getColorStateList(6);
        if (colors3 != null) {
            setLinkTextColor(colors3);
        }
        String familyName = appearance.getString(12);
        int typefaceIndex = appearance.getInt(1, -1);
        int styleIndex = appearance.getInt(2, -1);
        setTypefaceFromAttrs(familyName, typefaceIndex, styleIndex);
        int shadowcolor = appearance.getInt(7, 0);
        if (shadowcolor != 0) {
            float dx = appearance.getFloat(8, 0.0f);
            float dy = appearance.getFloat(9, 0.0f);
            float r = appearance.getFloat(10, 0.0f);
            setShadowLayer(r, dx, dy, shadowcolor);
        }
        if (appearance.getBoolean(11, false)) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
        if (appearance.hasValue(13)) {
            setElegantTextHeight(appearance.getBoolean(13, false));
        }
        if (appearance.hasValue(14)) {
            setLetterSpacing(appearance.getFloat(14, 0.0f));
        }
        if (appearance.hasValue(15)) {
            setFontFeatureSettings(appearance.getString(15));
        }
        appearance.recycle();
    }

    public Locale getTextLocale() {
        return this.mTextPaint.getTextLocale();
    }

    public void setTextLocale(Locale locale) {
        this.mTextPaint.setTextLocale(locale);
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    public float getTextSize() {
        return this.mTextPaint.getTextSize();
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    public float getScaledTextSize() {
        return this.mTextPaint.getTextSize() / this.mTextPaint.density;
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT, mapping = {@ViewDebug.IntToString(from = 0, to = "NORMAL"), @ViewDebug.IntToString(from = 1, to = "BOLD"), @ViewDebug.IntToString(from = 2, to = "ITALIC"), @ViewDebug.IntToString(from = 3, to = "BOLD_ITALIC")})
    public int getTypefaceStyle() {
        return this.mTextPaint.getTypeface().getStyle();
    }

    @RemotableViewMethod
    public void setTextSize(float size) {
        setTextSize(2, size);
    }

    public void setTextSize(int unit, float size) {
        Resources r;
        Context c = getContext();
        if (c == null) {
            r = Resources.getSystem();
        } else {
            r = c.getResources();
        }
        setRawTextSize(TypedValue.applyDimension(unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != this.mTextPaint.getTextSize()) {
            this.mTextPaint.setTextSize(size);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public float getTextScaleX() {
        return this.mTextPaint.getTextScaleX();
    }

    @RemotableViewMethod
    public void setTextScaleX(float size) {
        if (size != this.mTextPaint.getTextScaleX()) {
            this.mUserSetTextScaleX = true;
            this.mTextPaint.setTextScaleX(size);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setTypeface(Typeface tf) {
        if (this.mTextPaint.getTypeface() != tf) {
            this.mTextPaint.setTypeface(tf);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public Typeface getTypeface() {
        return this.mTextPaint.getTypeface();
    }

    public void setElegantTextHeight(boolean elegant) {
        this.mTextPaint.setElegantTextHeight(elegant);
    }

    public float getLetterSpacing() {
        return this.mTextPaint.getLetterSpacing();
    }

    @RemotableViewMethod
    public void setLetterSpacing(float letterSpacing) {
        if (letterSpacing != this.mTextPaint.getLetterSpacing()) {
            this.mTextPaint.setLetterSpacing(letterSpacing);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public String getFontFeatureSettings() {
        return this.mTextPaint.getFontFeatureSettings();
    }

    @RemotableViewMethod
    public void setFontFeatureSettings(String fontFeatureSettings) {
        if (fontFeatureSettings != this.mTextPaint.getFontFeatureSettings()) {
            this.mTextPaint.setFontFeatureSettings(fontFeatureSettings);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    @RemotableViewMethod
    public void setTextColor(int color) {
        this.mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }
        this.mTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getTextColors() {
        return this.mTextColor;
    }

    public final int getCurrentTextColor() {
        return this.mCurTextColor;
    }

    @RemotableViewMethod
    public void setHighlightColor(int color) {
        if (this.mHighlightColor != color) {
            this.mHighlightColor = color;
            invalidate();
        }
    }

    public int getHighlightColor() {
        return this.mHighlightColor;
    }

    @RemotableViewMethod
    public final void setShowSoftInputOnFocus(boolean show) {
        createEditorIfNeeded();
        this.mEditor.mShowSoftInputOnFocus = show;
    }

    public final boolean getShowSoftInputOnFocus() {
        return this.mEditor == null || this.mEditor.mShowSoftInputOnFocus;
    }

    public void setShadowLayer(float radius, float dx, float dy, int color) {
        this.mTextPaint.setShadowLayer(radius, dx, dy, color);
        this.mShadowRadius = radius;
        this.mShadowDx = dx;
        this.mShadowDy = dy;
        this.mShadowColor = color;
        if (this.mEditor != null) {
            this.mEditor.invalidateTextDisplayList();
        }
        invalidate();
    }

    public float getShadowRadius() {
        return this.mShadowRadius;
    }

    public float getShadowDx() {
        return this.mShadowDx;
    }

    public float getShadowDy() {
        return this.mShadowDy;
    }

    public int getShadowColor() {
        return this.mShadowColor;
    }

    public TextPaint getPaint() {
        return this.mTextPaint;
    }

    @RemotableViewMethod
    public final void setAutoLinkMask(int mask) {
        this.mAutoLinkMask = mask;
    }

    @RemotableViewMethod
    public final void setLinksClickable(boolean whether) {
        this.mLinksClickable = whether;
    }

    public final boolean getLinksClickable() {
        return this.mLinksClickable;
    }

    public URLSpan[] getUrls() {
        return this.mText instanceof Spanned ? (URLSpan[]) ((Spanned) this.mText).getSpans(0, this.mText.length(), URLSpan.class) : new URLSpan[0];
    }

    @RemotableViewMethod
    public final void setHintTextColor(int color) {
        this.mHintTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setHintTextColor(ColorStateList colors) {
        this.mHintTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getHintTextColors() {
        return this.mHintTextColor;
    }

    public final int getCurrentHintTextColor() {
        return this.mHintTextColor != null ? this.mCurHintTextColor : this.mCurTextColor;
    }

    @RemotableViewMethod
    public final void setLinkTextColor(int color) {
        this.mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setLinkTextColor(ColorStateList colors) {
        this.mLinkTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getLinkTextColors() {
        return this.mLinkTextColor;
    }

    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & 112) == 0) {
            gravity |= 48;
        }
        boolean newLayout = false;
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != (this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)) {
            newLayout = true;
        }
        if (gravity != this.mGravity) {
            invalidate();
        }
        this.mGravity = gravity;
        if (this.mLayout != null && newLayout) {
            int want = this.mLayout.getWidth();
            int hintWant = this.mHintLayout == null ? 0 : this.mHintLayout.getWidth();
            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING, ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight(), true);
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public int getPaintFlags() {
        return this.mTextPaint.getFlags();
    }

    @RemotableViewMethod
    public void setPaintFlags(int flags) {
        if (this.mTextPaint.getFlags() != flags) {
            this.mTextPaint.setFlags(flags);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setHorizontallyScrolling(boolean whether) {
        if (this.mHorizontallyScrolling != whether) {
            this.mHorizontallyScrolling = whether;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public boolean getHorizontallyScrolling() {
        return this.mHorizontallyScrolling;
    }

    @RemotableViewMethod
    public void setMinLines(int minlines) {
        this.mMinimum = minlines;
        this.mMinMode = 1;
        requestLayout();
        invalidate();
    }

    public int getMinLines() {
        if (this.mMinMode == 1) {
            return this.mMinimum;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMinHeight(int minHeight) {
        this.mMinimum = minHeight;
        this.mMinMode = 2;
        requestLayout();
        invalidate();
    }

    public int getMinHeight() {
        if (this.mMinMode == 2) {
            return this.mMinimum;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMaxLines(int maxlines) {
        this.mMaximum = maxlines;
        this.mMaxMode = 1;
        requestLayout();
        invalidate();
    }

    public int getMaxLines() {
        if (this.mMaxMode == 1) {
            return this.mMaximum;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMaxHeight(int maxHeight) {
        this.mMaximum = maxHeight;
        this.mMaxMode = 2;
        requestLayout();
        invalidate();
    }

    public int getMaxHeight() {
        if (this.mMaxMode == 2) {
            return this.mMaximum;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setLines(int lines) {
        this.mMinimum = lines;
        this.mMaximum = lines;
        this.mMinMode = 1;
        this.mMaxMode = 1;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setHeight(int pixels) {
        this.mMinimum = pixels;
        this.mMaximum = pixels;
        this.mMinMode = 2;
        this.mMaxMode = 2;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setMinEms(int minems) {
        this.mMinWidth = minems;
        this.mMinWidthMode = 1;
        requestLayout();
        invalidate();
    }

    public int getMinEms() {
        if (this.mMinWidthMode == 1) {
            return this.mMinWidth;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMinWidth(int minpixels) {
        this.mMinWidth = minpixels;
        this.mMinWidthMode = 2;
        requestLayout();
        invalidate();
    }

    public int getMinWidth() {
        if (this.mMinWidthMode == 2) {
            return this.mMinWidth;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMaxEms(int maxems) {
        this.mMaxWidth = maxems;
        this.mMaxWidthMode = 1;
        requestLayout();
        invalidate();
    }

    public int getMaxEms() {
        if (this.mMaxWidthMode == 1) {
            return this.mMaxWidth;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setMaxWidth(int maxpixels) {
        this.mMaxWidth = maxpixels;
        this.mMaxWidthMode = 2;
        requestLayout();
        invalidate();
    }

    public int getMaxWidth() {
        if (this.mMaxWidthMode == 2) {
            return this.mMaxWidth;
        }
        return -1;
    }

    @RemotableViewMethod
    public void setEms(int ems) {
        this.mMinWidth = ems;
        this.mMaxWidth = ems;
        this.mMinWidthMode = 1;
        this.mMaxWidthMode = 1;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setWidth(int pixels) {
        this.mMinWidth = pixels;
        this.mMaxWidth = pixels;
        this.mMinWidthMode = 2;
        this.mMaxWidthMode = 2;
        requestLayout();
        invalidate();
    }

    public void setLineSpacing(float add, float mult) {
        if (this.mSpacingAdd != add || this.mSpacingMult != mult) {
            this.mSpacingAdd = add;
            this.mSpacingMult = mult;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public float getLineSpacingMultiplier() {
        return this.mSpacingMult;
    }

    public float getLineSpacingExtra() {
        return this.mSpacingAdd;
    }

    public final void append(CharSequence text) {
        append(text, 0, text.length());
    }

    public void append(CharSequence text, int start, int end) {
        if (!(this.mText instanceof Editable)) {
            setText(this.mText, BufferType.EDITABLE);
        }
        ((Editable) this.mText).append(text, start, end);
    }

    private void updateTextColors() {
        int color;
        int color2;
        boolean inval = false;
        int color3 = this.mTextColor.getColorForState(getDrawableState(), 0);
        if (color3 != this.mCurTextColor) {
            this.mCurTextColor = color3;
            inval = true;
        }
        if (this.mLinkTextColor != null && (color2 = this.mLinkTextColor.getColorForState(getDrawableState(), 0)) != this.mTextPaint.linkColor) {
            this.mTextPaint.linkColor = color2;
            inval = true;
        }
        if (this.mHintTextColor != null && (color = this.mHintTextColor.getColorForState(getDrawableState(), 0)) != this.mCurHintTextColor) {
            this.mCurHintTextColor = color;
            if (this.mText.length() == 0) {
                inval = true;
            }
        }
        if (inval) {
            if (this.mEditor != null) {
                this.mEditor.invalidateTextDisplayList();
            }
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if ((this.mTextColor != null && this.mTextColor.isStateful()) || ((this.mHintTextColor != null && this.mHintTextColor.isStateful()) || (this.mLinkTextColor != null && this.mLinkTextColor.isStateful()))) {
            updateTextColors();
        }
        Drawables dr = this.mDrawables;
        if (dr != null) {
            int[] state = getDrawableState();
            if (dr.mDrawableTop != null && dr.mDrawableTop.isStateful()) {
                dr.mDrawableTop.setState(state);
            }
            if (dr.mDrawableBottom != null && dr.mDrawableBottom.isStateful()) {
                dr.mDrawableBottom.setState(state);
            }
            if (dr.mDrawableLeft != null && dr.mDrawableLeft.isStateful()) {
                dr.mDrawableLeft.setState(state);
            }
            if (dr.mDrawableRight != null && dr.mDrawableRight.isStateful()) {
                dr.mDrawableRight.setState(state);
            }
            if (dr.mDrawableStart != null && dr.mDrawableStart.isStateful()) {
                dr.mDrawableStart.setState(state);
            }
            if (dr.mDrawableEnd != null && dr.mDrawableEnd.isStateful()) {
                dr.mDrawableEnd.setState(state);
            }
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        Drawables dr = this.mDrawables;
        if (dr != null) {
            if (dr.mDrawableTop != null) {
                dr.mDrawableTop.setHotspot(x, y);
            }
            if (dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setHotspot(x, y);
            }
            if (dr.mDrawableLeft != null) {
                dr.mDrawableLeft.setHotspot(x, y);
            }
            if (dr.mDrawableRight != null) {
                dr.mDrawableRight.setHotspot(x, y);
            }
            if (dr.mDrawableStart != null) {
                dr.mDrawableStart.setHotspot(x, y);
            }
            if (dr.mDrawableEnd != null) {
                dr.mDrawableEnd.setHotspot(x, y);
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        boolean save = this.mFreezesText;
        int start = 0;
        int end = 0;
        if (this.mText != null) {
            start = getSelectionStart();
            end = getSelectionEnd();
            if (start >= 0 || end >= 0) {
                save = true;
            }
        }
        if (!save) {
            return superState;
        }
        SavedState ss = new SavedState(superState);
        ss.selStart = start;
        ss.selEnd = end;
        if (this.mText instanceof Spanned) {
            Spannable sp = new SpannableStringBuilder(this.mText);
            if (this.mEditor != null) {
                removeMisspelledSpans(sp);
                sp.removeSpan(this.mEditor.mSuggestionRangeSpan);
            }
            ss.text = sp;
        } else {
            ss.text = this.mText.toString();
        }
        if (isFocused() && start >= 0 && end >= 0) {
            ss.frozenWithFocus = true;
        }
        ss.error = getError();
        return ss;
    }

    void removeMisspelledSpans(Spannable spannable) {
        SuggestionSpan[] suggestionSpans = (SuggestionSpan[]) spannable.getSpans(0, spannable.length(), SuggestionSpan.class);
        for (int i = 0; i < suggestionSpans.length; i++) {
            int flags = suggestionSpans[i].getFlags();
            if ((flags & 1) != 0 && (flags & 2) != 0) {
                spannable.removeSpan(suggestionSpans[i]);
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.text != null) {
            setText(ss.text);
        }
        if (ss.selStart >= 0 && ss.selEnd >= 0 && (this.mText instanceof Spannable)) {
            int len = this.mText.length();
            if (ss.selStart > len || ss.selEnd > len) {
                String restored = ProxyInfo.LOCAL_EXCL_LIST;
                if (ss.text != null) {
                    restored = "(restored) ";
                }
                Log.e(LOG_TAG, "Saved cursor position " + ss.selStart + "/" + ss.selEnd + " out of range for " + restored + "text " + ((Object) this.mText));
            } else {
                Selection.setSelection((Spannable) this.mText, ss.selStart, ss.selEnd);
                if (ss.frozenWithFocus) {
                    createEditorIfNeeded();
                    this.mEditor.mFrozenWithFocus = true;
                }
            }
        }
        if (ss.error != null) {
            final CharSequence error = ss.error;
            post(new Runnable() {
                @Override
                public void run() {
                    TextView.this.setError(error);
                }
            });
        }
    }

    @RemotableViewMethod
    public void setFreezesText(boolean freezesText) {
        this.mFreezesText = freezesText;
    }

    public boolean getFreezesText() {
        return this.mFreezesText;
    }

    public final void setEditableFactory(Editable.Factory factory) {
        this.mEditableFactory = factory;
        setText(this.mText);
    }

    public final void setSpannableFactory(Spannable.Factory factory) {
        this.mSpannableFactory = factory;
        setText(this.mText);
    }

    @RemotableViewMethod
    public final void setText(CharSequence text) {
        setText(text, this.mBufferType);
    }

    @RemotableViewMethod
    public final void setTextKeepState(CharSequence text) {
        setTextKeepState(text, this.mBufferType);
    }

    public void setText(CharSequence text, BufferType type) {
        setText(text, type, true, 0);
        if (this.mCharWrapper == null) {
            return;
        }
        this.mCharWrapper.mChars = null;
    }

    private void setText(CharSequence text, BufferType type, boolean notifyBefore, int oldlen) {
        Spannable s2;
        if (text == null) {
            text = ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (!isSuggestionsEnabled()) {
            text = removeSuggestionSpans(text);
        }
        if (!this.mUserSetTextScaleX) {
            this.mTextPaint.setTextScaleX(1.0f);
        }
        if ((text instanceof Spanned) && ((Spanned) text).getSpanStart(TextUtils.TruncateAt.MARQUEE) >= 0) {
            if (ViewConfiguration.get(this.mContext).isFadingMarqueeEnabled()) {
                setHorizontalFadingEdgeEnabled(true);
                this.mMarqueeFadeMode = 0;
            } else {
                setHorizontalFadingEdgeEnabled(false);
                this.mMarqueeFadeMode = 1;
            }
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        }
        int n = this.mFilters.length;
        for (int i = 0; i < n; i++) {
            CharSequence out = this.mFilters[i].filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
            if (out != null) {
                text = out;
            }
        }
        if (notifyBefore) {
            if (this.mText != null) {
                oldlen = this.mText.length();
                sendBeforeTextChanged(this.mText, 0, oldlen, text.length());
            } else {
                sendBeforeTextChanged(ProxyInfo.LOCAL_EXCL_LIST, 0, 0, text.length());
            }
        }
        boolean needEditableForNotification = false;
        if (this.mListeners != null && this.mListeners.size() != 0) {
            needEditableForNotification = true;
        }
        if (type == BufferType.EDITABLE || getKeyListener() != null || needEditableForNotification) {
            createEditorIfNeeded();
            Editable t = this.mEditableFactory.newEditable(text);
            text = t;
            setFilters(t, this.mFilters);
            InputMethodManager imm = InputMethodManager.peekInstance();
            if (imm != null) {
                imm.restartInput(this);
            }
        } else if (type == BufferType.SPANNABLE || this.mMovement != null) {
            text = this.mSpannableFactory.newSpannable(text);
        } else if (!(text instanceof CharWrapper)) {
            text = TextUtils.stringOrSpannedString(text);
        }
        if (this.mAutoLinkMask != 0) {
            if (type == BufferType.EDITABLE || (text instanceof Spannable)) {
                s2 = (Spannable) text;
            } else {
                s2 = this.mSpannableFactory.newSpannable(text);
            }
            if (Linkify.addLinks(s2, this.mAutoLinkMask)) {
                text = s2;
                type = type == BufferType.EDITABLE ? BufferType.EDITABLE : BufferType.SPANNABLE;
                this.mText = text;
                if (this.mLinksClickable && !textCanBeSelected()) {
                    setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }
        this.mBufferType = type;
        this.mText = text;
        if (this.mTransformation == null) {
            this.mTransformed = text;
        } else {
            this.mTransformed = this.mTransformation.getTransformation(text, this);
        }
        int textLength = text.length();
        if ((text instanceof Spannable) && !this.mAllowTransformationLengthChange) {
            Spannable sp = (Spannable) text;
            ChangeWatcher[] watchers = (ChangeWatcher[]) sp.getSpans(0, sp.length(), ChangeWatcher.class);
            for (ChangeWatcher changeWatcher : watchers) {
                sp.removeSpan(changeWatcher);
            }
            if (this.mChangeWatcher == null) {
                this.mChangeWatcher = new ChangeWatcher();
            }
            sp.setSpan(this.mChangeWatcher, 0, textLength, 6553618);
            if (this.mEditor != null) {
                this.mEditor.addSpanWatchers(sp);
            }
            if (this.mTransformation != null) {
                sp.setSpan(this.mTransformation, 0, textLength, 18);
            }
            if (this.mMovement != null) {
                this.mMovement.initialize(this, (Spannable) text);
                if (this.mEditor != null) {
                    this.mEditor.mSelectionMoved = false;
                }
            }
        }
        if (this.mLayout != null) {
            checkForRelayout();
        }
        sendOnTextChanged(text, 0, oldlen, textLength);
        onTextChanged(text, 0, oldlen, textLength);
        notifyViewAccessibilityStateChangedIfNeeded(2);
        if (needEditableForNotification) {
            sendAfterTextChanged((Editable) text);
        }
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
    }

    public final void setText(char[] text, int start, int len) {
        int oldlen = 0;
        if (start < 0 || len < 0 || start + len > text.length) {
            throw new IndexOutOfBoundsException(start + ", " + len);
        }
        if (this.mText != null) {
            oldlen = this.mText.length();
            sendBeforeTextChanged(this.mText, 0, oldlen, len);
        } else {
            sendBeforeTextChanged(ProxyInfo.LOCAL_EXCL_LIST, 0, 0, len);
        }
        if (this.mCharWrapper == null) {
            this.mCharWrapper = new CharWrapper(text, start, len);
        } else {
            this.mCharWrapper.set(text, start, len);
        }
        setText(this.mCharWrapper, this.mBufferType, false, oldlen);
    }

    public final void setTextKeepState(CharSequence text, BufferType type) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int len = text.length();
        setText(text, type);
        if ((start >= 0 || end >= 0) && (this.mText instanceof Spannable)) {
            Selection.setSelection((Spannable) this.mText, Math.max(0, Math.min(start, len)), Math.max(0, Math.min(end, len)));
        }
    }

    @RemotableViewMethod
    public final void setText(int resid) {
        setText(getContext().getResources().getText(resid));
    }

    public final void setText(int resid, BufferType type) {
        setText(getContext().getResources().getText(resid), type);
    }

    @RemotableViewMethod
    public final void setHint(CharSequence hint) {
        this.mHint = TextUtils.stringOrSpannedString(hint);
        if (this.mLayout != null) {
            checkForRelayout();
        }
        if (this.mText.length() == 0) {
            invalidate();
        }
        if (this.mEditor != null && this.mText.length() == 0 && this.mHint != null) {
            this.mEditor.invalidateTextDisplayList();
        }
    }

    @RemotableViewMethod
    public final void setHint(int resid) {
        setHint(getContext().getResources().getText(resid));
    }

    @ViewDebug.CapturedViewProperty
    public CharSequence getHint() {
        return this.mHint;
    }

    boolean isSingleLine() {
        return this.mSingleLine;
    }

    private static boolean isMultilineInputType(int type) {
        return (131087 & type) == 131073;
    }

    CharSequence removeSuggestionSpans(CharSequence text) {
        Spannable spannable;
        if (text instanceof Spanned) {
            if (text instanceof Spannable) {
                spannable = (Spannable) text;
            } else {
                spannable = new SpannableString(text);
                text = spannable;
            }
            SuggestionSpan[] spans = (SuggestionSpan[]) spannable.getSpans(0, text.length(), SuggestionSpan.class);
            for (SuggestionSpan suggestionSpan : spans) {
                spannable.removeSpan(suggestionSpan);
            }
        }
        return text;
    }

    public void setInputType(int type) {
        boolean wasPassword = isPasswordInputType(getInputType());
        boolean wasVisiblePassword = isVisiblePasswordInputType(getInputType());
        setInputType(type, false);
        boolean isPassword = isPasswordInputType(type);
        boolean isVisiblePassword = isVisiblePasswordInputType(type);
        boolean forceUpdate = false;
        if (isPassword) {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            setTypefaceFromAttrs(null, 3, 0);
        } else if (isVisiblePassword) {
            if (this.mTransformation == PasswordTransformationMethod.getInstance()) {
                forceUpdate = true;
            }
            setTypefaceFromAttrs(null, 3, 0);
        } else if (wasPassword || wasVisiblePassword) {
            setTypefaceFromAttrs(null, -1, -1);
            if (this.mTransformation == PasswordTransformationMethod.getInstance()) {
                forceUpdate = true;
            }
        }
        boolean singleLine = !isMultilineInputType(type);
        if (this.mSingleLine != singleLine || forceUpdate) {
            applySingleLine(singleLine, isPassword ? false : true, true);
        }
        if (!isSuggestionsEnabled()) {
            this.mText = removeSuggestionSpans(this.mText);
        }
        InputMethodManager imm = InputMethodManager.peekInstance();
        if (imm != null) {
            imm.restartInput(this);
        }
    }

    private boolean hasPasswordTransformationMethod() {
        return this.mTransformation instanceof PasswordTransformationMethod;
    }

    private static boolean isPasswordInputType(int inputType) {
        int variation = inputType & FileObserver.ALL_EVENTS;
        return variation == 129 || variation == 225 || variation == 18;
    }

    private static boolean isVisiblePasswordInputType(int inputType) {
        int variation = inputType & FileObserver.ALL_EVENTS;
        return variation == 145;
    }

    public void setRawInputType(int type) {
        if (type != 0 || this.mEditor != null) {
            createEditorIfNeeded();
            this.mEditor.mInputType = type;
        }
    }

    private void setInputType(int type, boolean direct) {
        KeyListener input;
        TextKeyListener.Capitalize cap;
        int cls = type & 15;
        if (cls == 1) {
            boolean autotext = (32768 & type) != 0;
            if ((type & 4096) != 0) {
                cap = TextKeyListener.Capitalize.CHARACTERS;
            } else if ((type & 8192) != 0) {
                cap = TextKeyListener.Capitalize.WORDS;
            } else if ((type & 16384) != 0) {
                cap = TextKeyListener.Capitalize.SENTENCES;
            } else {
                cap = TextKeyListener.Capitalize.NONE;
            }
            input = TextKeyListener.getInstance(autotext, cap);
        } else if (cls == 2) {
            input = DigitsKeyListener.getInstance((type & 4096) != 0, (type & 8192) != 0);
        } else if (cls == 4) {
            switch (type & InputType.TYPE_MASK_VARIATION) {
                case 16:
                    input = DateKeyListener.getInstance();
                    break;
                case 32:
                    input = TimeKeyListener.getInstance();
                    break;
                default:
                    input = DateTimeKeyListener.getInstance();
                    break;
            }
        } else if (cls == 3) {
            input = DialerKeyListener.getInstance();
        } else {
            input = TextKeyListener.getInstance();
        }
        setRawInputType(type);
        if (direct) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = input;
        } else {
            setKeyListenerOnly(input);
        }
    }

    public int getInputType() {
        if (this.mEditor == null) {
            return 0;
        }
        return this.mEditor.mInputType;
    }

    public void setImeOptions(int imeOptions) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.imeOptions = imeOptions;
    }

    public int getImeOptions() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return 0;
        }
        return this.mEditor.mInputContentType.imeOptions;
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.imeActionLabel = label;
        this.mEditor.mInputContentType.imeActionId = actionId;
    }

    public CharSequence getImeActionLabel() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return null;
        }
        return this.mEditor.mInputContentType.imeActionLabel;
    }

    public int getImeActionId() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return 0;
        }
        return this.mEditor.mInputContentType.imeActionId;
    }

    public void setOnEditorActionListener(OnEditorActionListener l) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.onEditorActionListener = l;
    }

    public void onEditorAction(int actionCode) {
        Editor.InputContentType ict = this.mEditor == null ? null : this.mEditor.mInputContentType;
        if (ict != null) {
            if (ict.onEditorActionListener == null || !ict.onEditorActionListener.onEditorAction(this, actionCode, null)) {
                if (actionCode == 5) {
                    View v = focusSearch(2);
                    if (v != null && !v.requestFocus(2)) {
                        throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                    }
                    return;
                }
                if (actionCode == 7) {
                    View v2 = focusSearch(1);
                    if (v2 != null && !v2.requestFocus(1)) {
                        throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                    }
                    return;
                }
                if (actionCode == 6) {
                    InputMethodManager imm = InputMethodManager.peekInstance();
                    if (imm != null && imm.isActive(this)) {
                        imm.hideSoftInputFromWindow(getWindowToken(), 0);
                        return;
                    }
                    return;
                }
            } else {
                return;
            }
        }
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            long eventTime = SystemClock.uptimeMillis();
            viewRootImpl.dispatchKeyFromIme(new KeyEvent(eventTime, eventTime, 0, 66, 0, 0, -1, 0, 22));
            viewRootImpl.dispatchKeyFromIme(new KeyEvent(SystemClock.uptimeMillis(), eventTime, 1, 66, 0, 0, -1, 0, 22));
        }
    }

    public void setPrivateImeOptions(String type) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.privateImeOptions = type;
    }

    public String getPrivateImeOptions() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return null;
        }
        return this.mEditor.mInputContentType.privateImeOptions;
    }

    public void setInputExtras(int xmlResId) throws XmlPullParserException, IOException {
        createEditorIfNeeded();
        XmlResourceParser parser = getResources().getXml(xmlResId);
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.extras = new Bundle();
        getResources().parseBundleExtras(parser, this.mEditor.mInputContentType.extras);
    }

    public Bundle getInputExtras(boolean create) {
        if (this.mEditor == null && !create) {
            return null;
        }
        createEditorIfNeeded();
        if (this.mEditor.mInputContentType == null) {
            if (!create) {
                return null;
            }
            this.mEditor.createInputContentTypeIfNeeded();
        }
        if (this.mEditor.mInputContentType.extras == null) {
            if (!create) {
                return null;
            }
            this.mEditor.mInputContentType.extras = new Bundle();
        }
        return this.mEditor.mInputContentType.extras;
    }

    public CharSequence getError() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mError;
    }

    @RemotableViewMethod
    public void setError(CharSequence error) {
        if (error == null) {
            setError(null, null);
            return;
        }
        Drawable dr = getContext().getDrawable(R.drawable.indicator_input_error);
        dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
        setError(error, dr);
    }

    public void setError(CharSequence error, Drawable icon) {
        createEditorIfNeeded();
        this.mEditor.setError(error, icon);
        notifyViewAccessibilityStateChangedIfNeeded(0);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean result = super.setFrame(l, t, r, b);
        if (this.mEditor != null) {
            this.mEditor.setFrame();
        }
        restartMarqueeIfNeeded();
        return result;
    }

    private void restartMarqueeIfNeeded() {
        if (this.mRestartMarquee && this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            this.mRestartMarquee = false;
            startMarquee();
        }
    }

    public void setFilters(InputFilter[] filters) {
        if (filters == null) {
            throw new IllegalArgumentException();
        }
        this.mFilters = filters;
        if (this.mText instanceof Editable) {
            setFilters((Editable) this.mText, filters);
        }
    }

    private void setFilters(Editable e, InputFilter[] filters) {
        if (this.mEditor != null) {
            boolean undoFilter = this.mEditor.mUndoInputFilter != null;
            boolean keyFilter = this.mEditor.mKeyListener instanceof InputFilter;
            int num = undoFilter ? 0 + 1 : 0;
            if (keyFilter) {
                num++;
            }
            if (num > 0) {
                InputFilter[] nf = new InputFilter[filters.length + num];
                System.arraycopy(filters, 0, nf, 0, filters.length);
                int num2 = 0;
                if (undoFilter) {
                    nf[filters.length] = this.mEditor.mUndoInputFilter;
                    num2 = 0 + 1;
                }
                if (keyFilter) {
                    nf[filters.length + num2] = (InputFilter) this.mEditor.mKeyListener;
                }
                e.setFilters(nf);
                return;
            }
        }
        e.setFilters(filters);
    }

    public InputFilter[] getFilters() {
        return this.mFilters;
    }

    private int getBoxHeight(Layout l) {
        Insets opticalInsets = isLayoutModeOptical(this.mParent) ? getOpticalInsets() : Insets.NONE;
        int padding = l == this.mHintLayout ? getCompoundPaddingTop() + getCompoundPaddingBottom() : getExtendedPaddingTop() + getExtendedPaddingBottom();
        return (getMeasuredHeight() - padding) + opticalInsets.top + opticalInsets.bottom;
    }

    int getVerticalOffset(boolean forceNormal) {
        int boxht;
        int textht;
        int gravity = this.mGravity & 112;
        Layout l = this.mLayout;
        if (!forceNormal && this.mText.length() == 0 && this.mHintLayout != null) {
            l = this.mHintLayout;
        }
        if (gravity == 48 || (textht = l.getHeight()) >= (boxht = getBoxHeight(l))) {
            return 0;
        }
        if (gravity == 80) {
            int voffset = boxht - textht;
            return voffset;
        }
        int voffset2 = (boxht - textht) >> 1;
        return voffset2;
    }

    private int getBottomVerticalOffset(boolean forceNormal) {
        int boxht;
        int textht;
        int gravity = this.mGravity & 112;
        Layout l = this.mLayout;
        if (!forceNormal && this.mText.length() == 0 && this.mHintLayout != null) {
            l = this.mHintLayout;
        }
        if (gravity == 80 || (textht = l.getHeight()) >= (boxht = getBoxHeight(l))) {
            return 0;
        }
        if (gravity == 48) {
            int voffset = boxht - textht;
            return voffset;
        }
        int voffset2 = (boxht - textht) >> 1;
        return voffset2;
    }

    void invalidateCursorPath() {
        if (this.mHighlightPathBogus) {
            invalidateCursor();
            return;
        }
        int horizontalPadding = getCompoundPaddingLeft();
        int verticalPadding = getExtendedPaddingTop() + getVerticalOffset(true);
        if (this.mEditor.mCursorCount == 0) {
            synchronized (TEMP_RECTF) {
                float thick = FloatMath.ceil(this.mTextPaint.getStrokeWidth());
                if (thick < 1.0f) {
                    thick = 1.0f;
                }
                float thick2 = thick / 2.0f;
                this.mHighlightPath.computeBounds(TEMP_RECTF, false);
                invalidate((int) FloatMath.floor((horizontalPadding + TEMP_RECTF.left) - thick2), (int) FloatMath.floor((verticalPadding + TEMP_RECTF.top) - thick2), (int) FloatMath.ceil(horizontalPadding + TEMP_RECTF.right + thick2), (int) FloatMath.ceil(verticalPadding + TEMP_RECTF.bottom + thick2));
            }
            return;
        }
        for (int i = 0; i < this.mEditor.mCursorCount; i++) {
            Rect bounds = this.mEditor.mCursorDrawable[i].getBounds();
            invalidate(bounds.left + horizontalPadding, bounds.top + verticalPadding, bounds.right + horizontalPadding, bounds.bottom + verticalPadding);
        }
    }

    void invalidateCursor() {
        int where = getSelectionEnd();
        invalidateCursor(where, where, where);
    }

    private void invalidateCursor(int a, int b, int c) {
        if (a >= 0 || b >= 0 || c >= 0) {
            int start = Math.min(Math.min(a, b), c);
            int end = Math.max(Math.max(a, b), c);
            invalidateRegion(start, end, true);
        }
    }

    void invalidateRegion(int start, int end, boolean invalidateCursor) {
        int lineEnd;
        int left;
        int right;
        if (this.mLayout == null) {
            invalidate();
            return;
        }
        int lineStart = this.mLayout.getLineForOffset(start);
        int top = this.mLayout.getLineTop(lineStart);
        if (lineStart > 0) {
            top -= this.mLayout.getLineDescent(lineStart - 1);
        }
        if (start == end) {
            lineEnd = lineStart;
        } else {
            lineEnd = this.mLayout.getLineForOffset(end);
        }
        int bottom = this.mLayout.getLineBottom(lineEnd);
        if (invalidateCursor && this.mEditor != null) {
            for (int i = 0; i < this.mEditor.mCursorCount; i++) {
                Rect bounds = this.mEditor.mCursorDrawable[i].getBounds();
                top = Math.min(top, bounds.top);
                bottom = Math.max(bottom, bounds.bottom);
            }
        }
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int verticalPadding = getExtendedPaddingTop() + getVerticalOffset(true);
        if (lineStart == lineEnd && !invalidateCursor) {
            int left2 = (int) this.mLayout.getPrimaryHorizontal(start);
            int right2 = (int) (((double) this.mLayout.getPrimaryHorizontal(end)) + 1.0d);
            left = left2 + compoundPaddingLeft;
            right = right2 + compoundPaddingLeft;
        } else {
            left = compoundPaddingLeft;
            right = getWidth() - getCompoundPaddingRight();
        }
        invalidate(this.mScrollX + left, verticalPadding + top, this.mScrollX + right, verticalPadding + bottom);
    }

    private void registerForPreDraw() {
        if (!this.mPreDrawRegistered) {
            getViewTreeObserver().addOnPreDrawListener(this);
            this.mPreDrawRegistered = true;
        }
    }

    private void unregisterForPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        this.mPreDrawRegistered = false;
        this.mPreDrawListenerDetached = false;
    }

    @Override
    public boolean onPreDraw() {
        if (this.mLayout == null) {
            assumeLayout();
        }
        if (this.mMovement != null) {
            int curs = getSelectionEnd();
            if (this.mEditor != null && this.mEditor.mSelectionModifierCursorController != null && this.mEditor.mSelectionModifierCursorController.isSelectionStartDragged()) {
                curs = getSelectionStart();
            }
            if (curs < 0 && (this.mGravity & 112) == 80) {
                curs = this.mText.length();
            }
            if (curs >= 0) {
                bringPointIntoView(curs);
            }
        } else {
            bringTextIntoView();
        }
        if (this.mEditor != null && this.mEditor.mCreatedWithASelection) {
            this.mEditor.startSelectionActionMode();
            this.mEditor.mCreatedWithASelection = false;
        }
        if ((this instanceof ExtractEditText) && hasSelection() && this.mEditor != null) {
            this.mEditor.startSelectionActionMode();
        }
        unregisterForPreDraw();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mTemporaryDetach = false;
        if (this.mEditor != null) {
            this.mEditor.onAttachedToWindow();
        }
        if (this.mPreDrawListenerDetached) {
            getViewTreeObserver().addOnPreDrawListener(this);
            this.mPreDrawListenerDetached = false;
        }
    }

    @Override
    protected void onDetachedFromWindowInternal() {
        if (this.mPreDrawRegistered) {
            getViewTreeObserver().removeOnPreDrawListener(this);
            this.mPreDrawListenerDetached = true;
        }
        resetResolvedDrawables();
        if (this.mEditor != null) {
            this.mEditor.onDetachedFromWindow();
        }
        super.onDetachedFromWindowInternal();
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        if (this.mEditor != null) {
            this.mEditor.onScreenStateChanged(screenState);
        }
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return (this.mShadowRadius == 0.0f && this.mDrawables == null) ? false : true;
    }

    @Override
    protected int getLeftPaddingOffset() {
        return (getCompoundPaddingLeft() - this.mPaddingLeft) + ((int) Math.min(0.0f, this.mShadowDx - this.mShadowRadius));
    }

    @Override
    protected int getTopPaddingOffset() {
        return (int) Math.min(0.0f, this.mShadowDy - this.mShadowRadius);
    }

    @Override
    protected int getBottomPaddingOffset() {
        return (int) Math.max(0.0f, this.mShadowDy + this.mShadowRadius);
    }

    private int getFudgedPaddingRight() {
        int cursorWidth = ((int) this.mTextPaint.density) + 2;
        return Math.max(0, getCompoundPaddingRight() - (cursorWidth - 1));
    }

    @Override
    protected int getRightPaddingOffset() {
        return (-(getFudgedPaddingRight() - this.mPaddingRight)) + ((int) Math.max(0.0f, this.mShadowDx + this.mShadowRadius));
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean verified = super.verifyDrawable(who);
        return (verified || this.mDrawables == null) ? verified : who == this.mDrawables.mDrawableLeft || who == this.mDrawables.mDrawableTop || who == this.mDrawables.mDrawableRight || who == this.mDrawables.mDrawableBottom || who == this.mDrawables.mDrawableStart || who == this.mDrawables.mDrawableEnd;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mDrawables != null) {
            if (this.mDrawables.mDrawableLeft != null) {
                this.mDrawables.mDrawableLeft.jumpToCurrentState();
            }
            if (this.mDrawables.mDrawableTop != null) {
                this.mDrawables.mDrawableTop.jumpToCurrentState();
            }
            if (this.mDrawables.mDrawableRight != null) {
                this.mDrawables.mDrawableRight.jumpToCurrentState();
            }
            if (this.mDrawables.mDrawableBottom != null) {
                this.mDrawables.mDrawableBottom.jumpToCurrentState();
            }
            if (this.mDrawables.mDrawableStart != null) {
                this.mDrawables.mDrawableStart.jumpToCurrentState();
            }
            if (this.mDrawables.mDrawableEnd != null) {
                this.mDrawables.mDrawableEnd.jumpToCurrentState();
            }
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        boolean handled = false;
        if (verifyDrawable(drawable)) {
            Rect dirty = drawable.getBounds();
            int scrollX = this.mScrollX;
            int scrollY = this.mScrollY;
            Drawables drawables = this.mDrawables;
            if (drawables != null) {
                if (drawable == drawables.mDrawableLeft) {
                    int compoundPaddingTop = getCompoundPaddingTop();
                    int compoundPaddingBottom = getCompoundPaddingBottom();
                    int vspace = ((this.mBottom - this.mTop) - compoundPaddingBottom) - compoundPaddingTop;
                    scrollX += this.mPaddingLeft;
                    scrollY += ((vspace - drawables.mDrawableHeightLeft) / 2) + compoundPaddingTop;
                    handled = true;
                } else if (drawable == drawables.mDrawableRight) {
                    int compoundPaddingTop2 = getCompoundPaddingTop();
                    int compoundPaddingBottom2 = getCompoundPaddingBottom();
                    int vspace2 = ((this.mBottom - this.mTop) - compoundPaddingBottom2) - compoundPaddingTop2;
                    scrollX += ((this.mRight - this.mLeft) - this.mPaddingRight) - drawables.mDrawableSizeRight;
                    scrollY += ((vspace2 - drawables.mDrawableHeightRight) / 2) + compoundPaddingTop2;
                    handled = true;
                } else if (drawable == drawables.mDrawableTop) {
                    int compoundPaddingLeft = getCompoundPaddingLeft();
                    int compoundPaddingRight = getCompoundPaddingRight();
                    int hspace = ((this.mRight - this.mLeft) - compoundPaddingRight) - compoundPaddingLeft;
                    scrollX += ((hspace - drawables.mDrawableWidthTop) / 2) + compoundPaddingLeft;
                    scrollY += this.mPaddingTop;
                    handled = true;
                } else if (drawable == drawables.mDrawableBottom) {
                    int compoundPaddingLeft2 = getCompoundPaddingLeft();
                    int compoundPaddingRight2 = getCompoundPaddingRight();
                    int hspace2 = ((this.mRight - this.mLeft) - compoundPaddingRight2) - compoundPaddingLeft2;
                    scrollX += ((hspace2 - drawables.mDrawableWidthBottom) / 2) + compoundPaddingLeft2;
                    scrollY += ((this.mBottom - this.mTop) - this.mPaddingBottom) - drawables.mDrawableSizeBottom;
                    handled = true;
                }
            }
            if (handled) {
                invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
            }
        }
        if (!handled) {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return !(getBackground() == null || getBackground().getCurrent() == null) || (this.mText instanceof Spannable) || hasSelection() || isHorizontalFadingEdgeEnabled();
    }

    public boolean isTextSelectable() {
        if (this.mEditor == null) {
            return false;
        }
        return this.mEditor.mTextIsSelectable;
    }

    public void setTextIsSelectable(boolean selectable) {
        if (selectable || this.mEditor != null) {
            createEditorIfNeeded();
            if (this.mEditor.mTextIsSelectable != selectable) {
                this.mEditor.mTextIsSelectable = selectable;
                setFocusableInTouchMode(selectable);
                setFocusable(selectable);
                setClickable(selectable);
                setLongClickable(selectable);
                setMovementMethod(selectable ? ArrowKeyMovementMethod.getInstance() : null);
                setText(this.mText, selectable ? BufferType.SPANNABLE : BufferType.NORMAL);
                this.mEditor.prepareCursorControllers();
            }
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState;
        if (this.mSingleLine) {
            drawableState = super.onCreateDrawableState(extraSpace);
        } else {
            drawableState = super.onCreateDrawableState(extraSpace + 1);
            mergeDrawableStates(drawableState, MULTILINE_STATE_SET);
        }
        if (isTextSelectable()) {
            int length = drawableState.length;
            for (int i = 0; i < length; i++) {
                if (drawableState[i] == 16842919) {
                    int[] nonPressedState = new int[length - 1];
                    System.arraycopy(drawableState, 0, nonPressedState, 0, i);
                    System.arraycopy(drawableState, i + 1, nonPressedState, i, (length - i) - 1);
                    return nonPressedState;
                }
            }
        }
        return drawableState;
    }

    private Path getUpdatedHighlightPath() {
        Paint highlightPaint = this.mHighlightPaint;
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        if (this.mMovement == null) {
            return null;
        }
        if ((!isFocused() && !isPressed()) || selStart < 0) {
            return null;
        }
        if (selStart == selEnd) {
            if (this.mEditor == null || !this.mEditor.isCursorVisible() || (SystemClock.uptimeMillis() - this.mEditor.mShowCursor) % 1000 >= 500) {
                return null;
            }
            if (this.mHighlightPathBogus) {
                if (this.mHighlightPath == null) {
                    this.mHighlightPath = new Path();
                }
                this.mHighlightPath.reset();
                this.mLayout.getCursorPath(selStart, this.mHighlightPath, this.mText);
                this.mEditor.updateCursorsPositions();
                this.mHighlightPathBogus = false;
            }
            highlightPaint.setColor(this.mCurTextColor);
            highlightPaint.setStyle(Paint.Style.STROKE);
            Path highlight = this.mHighlightPath;
            return highlight;
        }
        if (this.mHighlightPathBogus) {
            if (this.mHighlightPath == null) {
                this.mHighlightPath = new Path();
            }
            this.mHighlightPath.reset();
            this.mLayout.getSelectionPath(selStart, selEnd, this.mHighlightPath);
            this.mHighlightPathBogus = false;
        }
        highlightPaint.setColor(this.mHighlightColor);
        highlightPaint.setStyle(Paint.Style.FILL);
        Path highlight2 = this.mHighlightPath;
        return highlight2;
    }

    public int getHorizontalOffsetForDrawables() {
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        restartMarqueeIfNeeded();
        super.onDraw(canvas);
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int compoundPaddingTop = getCompoundPaddingTop();
        int compoundPaddingRight = getCompoundPaddingRight();
        int compoundPaddingBottom = getCompoundPaddingBottom();
        int scrollX = this.mScrollX;
        int scrollY = this.mScrollY;
        int right = this.mRight;
        int left = this.mLeft;
        int bottom = this.mBottom;
        int top = this.mTop;
        boolean isLayoutRtl = isLayoutRtl();
        int offset = getHorizontalOffsetForDrawables();
        int leftOffset = isLayoutRtl ? 0 : offset;
        int rightOffset = isLayoutRtl ? offset : 0;
        Drawables dr = this.mDrawables;
        if (dr != null) {
            int vspace = ((bottom - top) - compoundPaddingBottom) - compoundPaddingTop;
            int hspace = ((right - left) - compoundPaddingRight) - compoundPaddingLeft;
            if (dr.mDrawableLeft != null) {
                canvas.save();
                canvas.translate(this.mPaddingLeft + scrollX + leftOffset, scrollY + compoundPaddingTop + ((vspace - dr.mDrawableHeightLeft) / 2));
                dr.mDrawableLeft.draw(canvas);
                canvas.restore();
            }
            if (dr.mDrawableRight != null) {
                canvas.save();
                canvas.translate(((((scrollX + right) - left) - this.mPaddingRight) - dr.mDrawableSizeRight) - rightOffset, scrollY + compoundPaddingTop + ((vspace - dr.mDrawableHeightRight) / 2));
                dr.mDrawableRight.draw(canvas);
                canvas.restore();
            }
            if (dr.mDrawableTop != null) {
                canvas.save();
                canvas.translate(scrollX + compoundPaddingLeft + ((hspace - dr.mDrawableWidthTop) / 2), this.mPaddingTop + scrollY);
                dr.mDrawableTop.draw(canvas);
                canvas.restore();
            }
            if (dr.mDrawableBottom != null) {
                canvas.save();
                canvas.translate(scrollX + compoundPaddingLeft + ((hspace - dr.mDrawableWidthBottom) / 2), (((scrollY + bottom) - top) - this.mPaddingBottom) - dr.mDrawableSizeBottom);
                dr.mDrawableBottom.draw(canvas);
                canvas.restore();
            }
        }
        int color = this.mCurTextColor;
        if (this.mLayout == null) {
            assumeLayout();
        }
        Layout layout = this.mLayout;
        if (this.mHint != null && this.mText.length() == 0) {
            if (this.mHintTextColor != null) {
                color = this.mCurHintTextColor;
            }
            layout = this.mHintLayout;
        }
        this.mTextPaint.setColor(color);
        this.mTextPaint.drawableState = getDrawableState();
        canvas.save();
        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();
        int maxScrollY = this.mLayout.getHeight() - (((this.mBottom - this.mTop) - compoundPaddingBottom) - compoundPaddingTop);
        float clipLeft = compoundPaddingLeft + scrollX;
        float clipTop = scrollY == 0 ? 0.0f : extendedPaddingTop + scrollY;
        float clipRight = ((right - left) - getFudgedPaddingRight()) + scrollX;
        int i = (bottom - top) + scrollY;
        if (scrollY == maxScrollY) {
            extendedPaddingBottom = 0;
        }
        float clipBottom = i - extendedPaddingBottom;
        if (this.mShadowRadius != 0.0f) {
            clipLeft += Math.min(0.0f, this.mShadowDx - this.mShadowRadius);
            clipRight += Math.max(0.0f, this.mShadowDx + this.mShadowRadius);
            clipTop += Math.min(0.0f, this.mShadowDy - this.mShadowRadius);
            clipBottom += Math.max(0.0f, this.mShadowDy + this.mShadowRadius);
        }
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);
        int voffsetText = 0;
        int voffsetCursor = 0;
        if ((this.mGravity & 112) != 48) {
            voffsetText = getVerticalOffset(false);
            voffsetCursor = getVerticalOffset(true);
        }
        canvas.translate(compoundPaddingLeft, extendedPaddingTop + voffsetText);
        int layoutDirection = getLayoutDirection();
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 1) {
            if (!this.mSingleLine && getLineCount() == 1 && canMarquee() && (absoluteGravity & 7) != 3) {
                int width = this.mRight - this.mLeft;
                int padding = getCompoundPaddingLeft() + getCompoundPaddingRight();
                float dx = this.mLayout.getLineRight(0) - (width - padding);
                canvas.translate(layout.getParagraphDirection(0) * dx, 0.0f);
            }
            if (this.mMarquee != null && this.mMarquee.isRunning()) {
                float dx2 = -this.mMarquee.getScroll();
                canvas.translate(layout.getParagraphDirection(0) * dx2, 0.0f);
            }
        }
        int cursorOffsetVertical = voffsetCursor - voffsetText;
        Path highlight = getUpdatedHighlightPath();
        if (this.mEditor != null) {
            this.mEditor.onDraw(canvas, layout, highlight, this.mHighlightPaint, cursorOffsetVertical);
        } else {
            layout.draw(canvas, highlight, this.mHighlightPaint, cursorOffsetVertical);
        }
        if (this.mMarquee != null && this.mMarquee.shouldDrawGhost()) {
            float dx3 = this.mMarquee.getGhostOffset();
            canvas.translate(layout.getParagraphDirection(0) * dx3, 0.0f);
            layout.draw(canvas, highlight, this.mHighlightPaint, cursorOffsetVertical);
        }
        canvas.restore();
    }

    @Override
    public void getFocusedRect(Rect r) {
        if (this.mLayout == null) {
            super.getFocusedRect(r);
            return;
        }
        int selEnd = getSelectionEnd();
        if (selEnd < 0) {
            super.getFocusedRect(r);
            return;
        }
        int selStart = getSelectionStart();
        if (selStart < 0 || selStart >= selEnd) {
            int line = this.mLayout.getLineForOffset(selEnd);
            r.top = this.mLayout.getLineTop(line);
            r.bottom = this.mLayout.getLineBottom(line);
            r.left = ((int) this.mLayout.getPrimaryHorizontal(selEnd)) - 2;
            r.right = r.left + 4;
        } else {
            int lineStart = this.mLayout.getLineForOffset(selStart);
            int lineEnd = this.mLayout.getLineForOffset(selEnd);
            r.top = this.mLayout.getLineTop(lineStart);
            r.bottom = this.mLayout.getLineBottom(lineEnd);
            if (lineStart == lineEnd) {
                r.left = (int) this.mLayout.getPrimaryHorizontal(selStart);
                r.right = (int) this.mLayout.getPrimaryHorizontal(selEnd);
            } else {
                if (this.mHighlightPathBogus) {
                    if (this.mHighlightPath == null) {
                        this.mHighlightPath = new Path();
                    }
                    this.mHighlightPath.reset();
                    this.mLayout.getSelectionPath(selStart, selEnd, this.mHighlightPath);
                    this.mHighlightPathBogus = false;
                }
                synchronized (TEMP_RECTF) {
                    this.mHighlightPath.computeBounds(TEMP_RECTF, true);
                    r.left = ((int) TEMP_RECTF.left) - 1;
                    r.right = ((int) TEMP_RECTF.right) + 1;
                }
            }
        }
        int paddingLeft = getCompoundPaddingLeft();
        int paddingTop = getExtendedPaddingTop();
        if ((this.mGravity & 112) != 48) {
            paddingTop += getVerticalOffset(false);
        }
        r.offset(paddingLeft, paddingTop);
        int paddingBottom = getExtendedPaddingBottom();
        r.bottom += paddingBottom;
    }

    public int getLineCount() {
        if (this.mLayout != null) {
            return this.mLayout.getLineCount();
        }
        return 0;
    }

    public int getLineBounds(int line, Rect bounds) {
        if (this.mLayout == null) {
            if (bounds == null) {
                return 0;
            }
            bounds.set(0, 0, 0, 0);
            return 0;
        }
        int baseline = this.mLayout.getLineBounds(line, bounds);
        int voffset = getExtendedPaddingTop();
        if ((this.mGravity & 112) != 48) {
            voffset += getVerticalOffset(true);
        }
        if (bounds != null) {
            bounds.offset(getCompoundPaddingLeft(), voffset);
        }
        return baseline + voffset;
    }

    @Override
    public int getBaseline() {
        if (this.mLayout == null) {
            return super.getBaseline();
        }
        int voffset = 0;
        if ((this.mGravity & 112) != 48) {
            voffset = getVerticalOffset(true);
        }
        if (isLayoutModeOptical(this.mParent)) {
            voffset -= getOpticalInsets().top;
        }
        return getExtendedPaddingTop() + voffset + this.mLayout.getLineBaseline(0);
    }

    @Override
    protected int getFadeTop(boolean offsetRequired) {
        if (this.mLayout == null) {
            return 0;
        }
        int voffset = 0;
        if ((this.mGravity & 112) != 48) {
            voffset = getVerticalOffset(true);
        }
        if (offsetRequired) {
            voffset += getTopPaddingOffset();
        }
        return getExtendedPaddingTop() + voffset;
    }

    @Override
    protected int getFadeHeight(boolean offsetRequired) {
        if (this.mLayout != null) {
            return this.mLayout.getHeight();
        }
        return 0;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            boolean isInSelectionMode = (this.mEditor == null || this.mEditor.mSelectionActionMode == null) ? false : true;
            if (isInSelectionMode) {
                if (event.getAction() == 0 && event.getRepeatCount() == 0) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state == null) {
                        return true;
                    }
                    state.startTracking(event, this);
                    return true;
                }
                if (event.getAction() == 1) {
                    KeyEvent.DispatcherState state2 = getKeyDispatcherState();
                    if (state2 != null) {
                        state2.handleUpEvent(event);
                    }
                    if (event.isTracking() && !event.isCanceled()) {
                        stopSelectionActionMode();
                        return true;
                    }
                }
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int which = doKeyDown(keyCode, event, null);
        if (which == 0) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        KeyEvent down = KeyEvent.changeAction(event, 0);
        int which = doKeyDown(keyCode, down, event);
        if (which == 0) {
            return super.onKeyMultiple(keyCode, repeatCount, event);
        }
        if (which == -1) {
            return true;
        }
        int repeatCount2 = repeatCount - 1;
        KeyEvent up = KeyEvent.changeAction(event, 1);
        if (which == 1) {
            this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, keyCode, up);
            while (true) {
                repeatCount2--;
                if (repeatCount2 <= 0) {
                    break;
                }
                this.mEditor.mKeyListener.onKeyDown(this, (Editable) this.mText, keyCode, down);
                this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, keyCode, up);
            }
            hideErrorIfUnchanged();
        } else if (which == 2) {
            this.mMovement.onKeyUp(this, (Spannable) this.mText, keyCode, up);
            while (true) {
                repeatCount2--;
                if (repeatCount2 <= 0) {
                    break;
                }
                this.mMovement.onKeyDown(this, (Spannable) this.mText, keyCode, down);
                this.mMovement.onKeyUp(this, (Spannable) this.mText, keyCode, up);
            }
        }
        return true;
    }

    private boolean shouldAdvanceFocusOnEnter() {
        if (getKeyListener() == null) {
            return false;
        }
        if (this.mSingleLine) {
            return true;
        }
        if (this.mEditor == null || (this.mEditor.mInputType & 15) != 1) {
            return false;
        }
        int variation = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION;
        return variation == 32 || variation == 48;
    }

    private boolean shouldAdvanceFocusOnTab() {
        if (getKeyListener() == null || this.mSingleLine || this.mEditor == null || (this.mEditor.mInputType & 15) != 1) {
            return true;
        }
        int variation = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION;
        return (variation == 262144 || variation == 131072) ? false : true;
    }

    private int doKeyDown(int keyCode, KeyEvent event, KeyEvent otherEvent) {
        if (!isEnabled()) {
            return 0;
        }
        if (event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(keyCode)) {
            this.mPreventDefaultMovement = false;
        }
        switch (keyCode) {
            case 4:
                if (this.mEditor != null && this.mEditor.mSelectionActionMode != null) {
                    stopSelectionActionMode();
                    return -1;
                }
                break;
            case 23:
                if (event.hasNoModifiers() && shouldAdvanceFocusOnEnter()) {
                    return 0;
                }
                break;
            case 61:
                if ((event.hasNoModifiers() || event.hasModifiers(1)) && shouldAdvanceFocusOnTab()) {
                    return 0;
                }
                break;
            case 66:
                if (event.hasNoModifiers()) {
                    if (this.mEditor != null && this.mEditor.mInputContentType != null && this.mEditor.mInputContentType.onEditorActionListener != null && this.mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, 0, event)) {
                        this.mEditor.mInputContentType.enterDown = true;
                        return -1;
                    }
                    if ((event.getFlags() & 16) != 0 || shouldAdvanceFocusOnEnter()) {
                        return hasOnClickListeners() ? 0 : -1;
                    }
                }
                break;
        }
        if (this.mEditor != null && this.mEditor.mKeyListener != null) {
            boolean doDown = true;
            if (otherEvent != null) {
                try {
                    beginBatchEdit();
                    boolean handled = this.mEditor.mKeyListener.onKeyOther(this, (Editable) this.mText, otherEvent);
                    hideErrorIfUnchanged();
                    doDown = false;
                    if (handled) {
                        return -1;
                    }
                } catch (AbstractMethodError e) {
                } finally {
                    endBatchEdit();
                }
            }
            if (doDown) {
                beginBatchEdit();
                boolean handled2 = this.mEditor.mKeyListener.onKeyDown(this, (Editable) this.mText, keyCode, event);
                endBatchEdit();
                hideErrorIfUnchanged();
                if (handled2) {
                    return 1;
                }
            }
        }
        if (this.mMovement != null && this.mLayout != null) {
            boolean doDown2 = true;
            if (otherEvent != null) {
                try {
                    boolean handled3 = this.mMovement.onKeyOther(this, (Spannable) this.mText, otherEvent);
                    doDown2 = false;
                    if (handled3) {
                        return -1;
                    }
                } catch (AbstractMethodError e2) {
                }
            }
            if (doDown2 && this.mMovement.onKeyDown(this, (Spannable) this.mText, keyCode, event)) {
                if (event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(keyCode)) {
                    this.mPreventDefaultMovement = true;
                }
                return 2;
            }
        }
        return (!this.mPreventDefaultMovement || KeyEvent.isModifierKey(keyCode)) ? 0 : -1;
    }

    public void resetErrorChangedFlag() {
        if (this.mEditor != null) {
            this.mEditor.mErrorWasChanged = false;
        }
    }

    public void hideErrorIfUnchanged() {
        if (this.mEditor != null && this.mEditor.mError != null && !this.mEditor.mErrorWasChanged) {
            setError(null, null);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        InputMethodManager imm;
        if (!isEnabled()) {
            return super.onKeyUp(keyCode, event);
        }
        if (!KeyEvent.isModifierKey(keyCode)) {
            this.mPreventDefaultMovement = false;
        }
        switch (keyCode) {
            case 23:
                if (event.hasNoModifiers() && !hasOnClickListeners() && this.mMovement != null && (this.mText instanceof Editable) && this.mLayout != null && onCheckIsTextEditor()) {
                    InputMethodManager imm2 = InputMethodManager.peekInstance();
                    viewClicked(imm2);
                    if (imm2 != null && getShowSoftInputOnFocus()) {
                        imm2.showSoftInput(this, 0);
                    }
                }
                return super.onKeyUp(keyCode, event);
            case 66:
                if (event.hasNoModifiers()) {
                    if (this.mEditor != null && this.mEditor.mInputContentType != null && this.mEditor.mInputContentType.onEditorActionListener != null && this.mEditor.mInputContentType.enterDown) {
                        this.mEditor.mInputContentType.enterDown = false;
                        if (this.mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, 0, event)) {
                            return true;
                        }
                    }
                    if (((event.getFlags() & 16) != 0 || shouldAdvanceFocusOnEnter()) && !hasOnClickListeners()) {
                        View v = focusSearch(130);
                        if (v != null) {
                            if (!v.requestFocus(130)) {
                                throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                            }
                            super.onKeyUp(keyCode, event);
                            return true;
                        }
                        if ((event.getFlags() & 16) != 0 && (imm = InputMethodManager.peekInstance()) != null && imm.isActive(this)) {
                            imm.hideSoftInputFromWindow(getWindowToken(), 0);
                        }
                    }
                    return super.onKeyUp(keyCode, event);
                }
                break;
        }
        if (this.mEditor != null && this.mEditor.mKeyListener != null && this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, keyCode, event)) {
            return true;
        }
        if (this.mMovement == null || this.mLayout == null || !this.mMovement.onKeyUp(this, (Spannable) this.mText, keyCode, event)) {
            return super.onKeyUp(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return (this.mEditor == null || this.mEditor.mInputType == 0) ? false : true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (onCheckIsTextEditor() && isEnabled()) {
            this.mEditor.createInputMethodStateIfNeeded();
            outAttrs.inputType = getInputType();
            if (this.mEditor.mInputContentType != null) {
                outAttrs.imeOptions = this.mEditor.mInputContentType.imeOptions;
                outAttrs.privateImeOptions = this.mEditor.mInputContentType.privateImeOptions;
                outAttrs.actionLabel = this.mEditor.mInputContentType.imeActionLabel;
                outAttrs.actionId = this.mEditor.mInputContentType.imeActionId;
                outAttrs.extras = this.mEditor.mInputContentType.extras;
            } else {
                outAttrs.imeOptions = 0;
            }
            if (focusSearch(130) != null) {
                outAttrs.imeOptions |= 134217728;
            }
            if (focusSearch(33) != null) {
                outAttrs.imeOptions |= 67108864;
            }
            if ((outAttrs.imeOptions & 255) == 0) {
                if ((outAttrs.imeOptions & 134217728) != 0) {
                    outAttrs.imeOptions |= 5;
                } else {
                    outAttrs.imeOptions |= 6;
                }
                if (!shouldAdvanceFocusOnEnter()) {
                    outAttrs.imeOptions |= 1073741824;
                }
            }
            if (isMultilineInputType(outAttrs.inputType)) {
                outAttrs.imeOptions |= 1073741824;
            }
            outAttrs.hintText = this.mHint;
            if (this.mText instanceof Editable) {
                InputConnection ic = new EditableInputConnection(this);
                outAttrs.initialSelStart = getSelectionStart();
                outAttrs.initialSelEnd = getSelectionEnd();
                outAttrs.initialCapsMode = ic.getCursorCapsMode(getInputType());
                return ic;
            }
        }
        return null;
    }

    public boolean extractText(ExtractedTextRequest request, ExtractedText outText) {
        createEditorIfNeeded();
        return this.mEditor.extractText(request, outText);
    }

    static void removeParcelableSpans(Spannable spannable, int start, int end) {
        Object[] spans = spannable.getSpans(start, end, ParcelableSpan.class);
        int i = spans.length;
        while (i > 0) {
            i--;
            spannable.removeSpan(spans[i]);
        }
    }

    public void setExtractedText(ExtractedText text) {
        Editable content = getEditableText();
        if (text.text != null) {
            if (content == null) {
                setText(text.text, BufferType.EDITABLE);
            } else if (text.partialStartOffset < 0) {
                removeParcelableSpans(content, 0, content.length());
                content.replace(0, content.length(), text.text);
            } else {
                int N = content.length();
                int start = text.partialStartOffset;
                if (start > N) {
                    start = N;
                }
                int end = text.partialEndOffset;
                if (end > N) {
                    end = N;
                }
                removeParcelableSpans(content, start, end);
                content.replace(start, end, text.text);
            }
        }
        Spannable sp = (Spannable) getText();
        int N2 = sp.length();
        int start2 = text.selectionStart;
        if (start2 < 0) {
            start2 = 0;
        } else if (start2 > N2) {
            start2 = N2;
        }
        int end2 = text.selectionEnd;
        if (end2 < 0) {
            end2 = 0;
        } else if (end2 > N2) {
            end2 = N2;
        }
        Selection.setSelection(sp, start2, end2);
        if ((text.flags & 2) != 0) {
            MetaKeyKeyListener.startSelecting(this, sp);
        } else {
            MetaKeyKeyListener.stopSelecting(this, sp);
        }
    }

    public void setExtracting(ExtractedTextRequest req) {
        if (this.mEditor.mInputMethodState != null) {
            this.mEditor.mInputMethodState.mExtractedTextRequest = req;
        }
        this.mEditor.hideControllers();
    }

    public void onCommitCompletion(CompletionInfo text) {
    }

    public void onCommitCorrection(CorrectionInfo info) {
        if (this.mEditor != null) {
            this.mEditor.onCommitCorrection(info);
        }
    }

    public void beginBatchEdit() {
        if (this.mEditor != null) {
            this.mEditor.beginBatchEdit();
        }
    }

    public void endBatchEdit() {
        if (this.mEditor != null) {
            this.mEditor.endBatchEdit();
        }
    }

    public void onBeginBatchEdit() {
    }

    public void onEndBatchEdit() {
    }

    public boolean onPrivateIMECommand(String action, Bundle data) {
        return false;
    }

    private void nullLayouts() {
        if ((this.mLayout instanceof BoringLayout) && this.mSavedLayout == null) {
            this.mSavedLayout = (BoringLayout) this.mLayout;
        }
        if ((this.mHintLayout instanceof BoringLayout) && this.mSavedHintLayout == null) {
            this.mSavedHintLayout = (BoringLayout) this.mHintLayout;
        }
        this.mHintLayout = null;
        this.mLayout = null;
        this.mSavedMarqueeModeLayout = null;
        this.mHintBoring = null;
        this.mBoring = null;
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
    }

    private void assumeLayout() {
        int width = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        if (width < 1) {
            width = 0;
        }
        int physicalWidth = width;
        if (this.mHorizontallyScrolling) {
            width = 1048576;
        }
        makeNewLayout(width, physicalWidth, UNKNOWN_BORING, UNKNOWN_BORING, physicalWidth, false);
    }

    private Layout.Alignment getLayoutAlignment() {
        switch (getTextAlignment()) {
            case 1:
                switch (this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
                    case 1:
                        Layout.Alignment alignment = Layout.Alignment.ALIGN_CENTER;
                        return alignment;
                    case 3:
                        Layout.Alignment alignment2 = Layout.Alignment.ALIGN_LEFT;
                        return alignment2;
                    case 5:
                        Layout.Alignment alignment3 = Layout.Alignment.ALIGN_RIGHT;
                        return alignment3;
                    case Gravity.START:
                        Layout.Alignment alignment4 = Layout.Alignment.ALIGN_NORMAL;
                        return alignment4;
                    case Gravity.END:
                        Layout.Alignment alignment5 = Layout.Alignment.ALIGN_OPPOSITE;
                        return alignment5;
                    default:
                        Layout.Alignment alignment6 = Layout.Alignment.ALIGN_NORMAL;
                        return alignment6;
                }
            case 2:
                Layout.Alignment alignment7 = Layout.Alignment.ALIGN_NORMAL;
                return alignment7;
            case 3:
                Layout.Alignment alignment8 = Layout.Alignment.ALIGN_OPPOSITE;
                return alignment8;
            case 4:
                Layout.Alignment alignment9 = Layout.Alignment.ALIGN_CENTER;
                return alignment9;
            case 5:
                if (getLayoutDirection() != 1) {
                    Layout.Alignment alignment10 = Layout.Alignment.ALIGN_LEFT;
                    return alignment10;
                }
                Layout.Alignment alignment11 = Layout.Alignment.ALIGN_RIGHT;
                return alignment11;
            case 6:
                if (getLayoutDirection() != 1) {
                    Layout.Alignment alignment12 = Layout.Alignment.ALIGN_RIGHT;
                    return alignment12;
                }
                Layout.Alignment alignment13 = Layout.Alignment.ALIGN_LEFT;
                return alignment13;
            default:
                Layout.Alignment alignment14 = Layout.Alignment.ALIGN_NORMAL;
                return alignment14;
        }
    }

    protected void makeNewLayout(int wantWidth, int hintWidth, BoringLayout.Metrics boring, BoringLayout.Metrics hintBoring, int ellipsisWidth, boolean bringIntoView) {
        stopMarquee();
        this.mOldMaximum = this.mMaximum;
        this.mOldMaxMode = this.mMaxMode;
        this.mHighlightPathBogus = true;
        if (wantWidth < 0) {
            wantWidth = 0;
        }
        if (hintWidth < 0) {
            hintWidth = 0;
        }
        Layout.Alignment alignment = getLayoutAlignment();
        boolean testDirChange = this.mSingleLine && this.mLayout != null && (alignment == Layout.Alignment.ALIGN_NORMAL || alignment == Layout.Alignment.ALIGN_OPPOSITE);
        int oldDir = testDirChange ? this.mLayout.getParagraphDirection(0) : 0;
        boolean shouldEllipsize = this.mEllipsize != null && getKeyListener() == null;
        boolean switchEllipsize = this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 0;
        TextUtils.TruncateAt effectiveEllipsize = this.mEllipsize;
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode == 1) {
            effectiveEllipsize = TextUtils.TruncateAt.END_SMALL;
        }
        if (this.mTextDir == null) {
            this.mTextDir = getTextDirectionHeuristic();
        }
        this.mLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment, shouldEllipsize, effectiveEllipsize, effectiveEllipsize == this.mEllipsize);
        if (switchEllipsize) {
            TextUtils.TruncateAt oppositeEllipsize = effectiveEllipsize == TextUtils.TruncateAt.MARQUEE ? TextUtils.TruncateAt.END : TextUtils.TruncateAt.MARQUEE;
            this.mSavedMarqueeModeLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment, shouldEllipsize, oppositeEllipsize, effectiveEllipsize != this.mEllipsize);
        }
        boolean shouldEllipsize2 = this.mEllipsize != null;
        this.mHintLayout = null;
        if (this.mHint != null) {
            if (shouldEllipsize2) {
                hintWidth = wantWidth;
            }
            if (hintBoring == UNKNOWN_BORING && (hintBoring = BoringLayout.isBoring(this.mHint, this.mTextPaint, this.mTextDir, this.mHintBoring)) != null) {
                this.mHintBoring = hintBoring;
            }
            if (hintBoring != null) {
                if (hintBoring.width <= hintWidth && (!shouldEllipsize2 || hintBoring.width <= ellipsisWidth)) {
                    if (this.mSavedHintLayout != null) {
                        this.mHintLayout = this.mSavedHintLayout.replaceOrMake(this.mHint, this.mTextPaint, hintWidth, alignment, this.mSpacingMult, this.mSpacingAdd, hintBoring, this.mIncludePad);
                    } else {
                        this.mHintLayout = BoringLayout.make(this.mHint, this.mTextPaint, hintWidth, alignment, this.mSpacingMult, this.mSpacingAdd, hintBoring, this.mIncludePad);
                    }
                    this.mSavedHintLayout = (BoringLayout) this.mHintLayout;
                } else if (shouldEllipsize2 && hintBoring.width <= hintWidth) {
                    if (this.mSavedHintLayout != null) {
                        this.mHintLayout = this.mSavedHintLayout.replaceOrMake(this.mHint, this.mTextPaint, hintWidth, alignment, this.mSpacingMult, this.mSpacingAdd, hintBoring, this.mIncludePad, this.mEllipsize, ellipsisWidth);
                    } else {
                        this.mHintLayout = BoringLayout.make(this.mHint, this.mTextPaint, hintWidth, alignment, this.mSpacingMult, this.mSpacingAdd, hintBoring, this.mIncludePad, this.mEllipsize, ellipsisWidth);
                    }
                } else if (shouldEllipsize2) {
                    this.mHintLayout = new StaticLayout(this.mHint, 0, this.mHint.length(), this.mTextPaint, hintWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad, this.mEllipsize, ellipsisWidth, this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
                } else {
                    this.mHintLayout = new StaticLayout(this.mHint, this.mTextPaint, hintWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad);
                }
            } else if (shouldEllipsize2) {
                this.mHintLayout = new StaticLayout(this.mHint, 0, this.mHint.length(), this.mTextPaint, hintWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad, this.mEllipsize, ellipsisWidth, this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
            } else {
                this.mHintLayout = new StaticLayout(this.mHint, this.mTextPaint, hintWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad);
            }
        }
        if (bringIntoView || (testDirChange && oldDir != this.mLayout.getParagraphDirection(0))) {
            registerForPreDraw();
        }
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && !compressText(ellipsisWidth)) {
            int height = this.mLayoutParams.height;
            if (height != -2 && height != -1) {
                startMarquee();
            } else {
                this.mRestartMarquee = true;
            }
        }
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
    }

    private Layout makeSingleLayout(int wantWidth, BoringLayout.Metrics boring, int ellipsisWidth, Layout.Alignment alignment, boolean shouldEllipsize, TextUtils.TruncateAt effectiveEllipsize, boolean useSaved) {
        Layout result;
        if (this.mText instanceof Spannable) {
            Layout result2 = new DynamicLayout(this.mText, this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad, getKeyListener() == null ? effectiveEllipsize : null, ellipsisWidth);
            return result2;
        }
        if (boring == UNKNOWN_BORING && (boring = BoringLayout.isBoring(this.mTransformed, this.mTextPaint, this.mTextDir, this.mBoring)) != null) {
            this.mBoring = boring;
        }
        if (boring != null) {
            if (boring.width <= wantWidth && (effectiveEllipsize == null || boring.width <= ellipsisWidth)) {
                if (useSaved && this.mSavedLayout != null) {
                    result = this.mSavedLayout.replaceOrMake(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mSpacingMult, this.mSpacingAdd, boring, this.mIncludePad);
                } else {
                    result = BoringLayout.make(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mSpacingMult, this.mSpacingAdd, boring, this.mIncludePad);
                }
                if (useSaved) {
                    this.mSavedLayout = (BoringLayout) result;
                    return result;
                }
                return result;
            }
            if (shouldEllipsize && boring.width <= wantWidth) {
                if (useSaved && this.mSavedLayout != null) {
                    Layout result3 = this.mSavedLayout.replaceOrMake(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mSpacingMult, this.mSpacingAdd, boring, this.mIncludePad, effectiveEllipsize, ellipsisWidth);
                    return result3;
                }
                Layout result4 = BoringLayout.make(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mSpacingMult, this.mSpacingAdd, boring, this.mIncludePad, effectiveEllipsize, ellipsisWidth);
                return result4;
            }
            if (shouldEllipsize) {
                Layout result5 = new StaticLayout(this.mTransformed, 0, this.mTransformed.length(), this.mTextPaint, wantWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad, effectiveEllipsize, ellipsisWidth, this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
                return result5;
            }
            Layout result6 = new StaticLayout(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad);
            return result6;
        }
        if (shouldEllipsize) {
            Layout result7 = new StaticLayout(this.mTransformed, 0, this.mTransformed.length(), this.mTextPaint, wantWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad, effectiveEllipsize, ellipsisWidth, this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
            return result7;
        }
        Layout result8 = new StaticLayout(this.mTransformed, this.mTextPaint, wantWidth, alignment, this.mTextDir, this.mSpacingMult, this.mSpacingAdd, this.mIncludePad);
        return result8;
    }

    private boolean compressText(float width) {
        if (isHardwareAccelerated() || width <= 0.0f || this.mLayout == null || getLineCount() != 1 || this.mUserSetTextScaleX || this.mTextPaint.getTextScaleX() != 1.0f) {
            return false;
        }
        float textWidth = this.mLayout.getLineWidth(0);
        float overflow = ((textWidth + 1.0f) - width) / width;
        if (overflow <= 0.0f || overflow > 0.07f) {
            return false;
        }
        this.mTextPaint.setTextScaleX((1.0f - overflow) - 0.005f);
        post(new Runnable() {
            @Override
            public void run() {
                TextView.this.requestLayout();
            }
        });
        return true;
    }

    private static int desired(Layout layout) {
        int n = layout.getLineCount();
        CharSequence text = layout.getText();
        float max = 0.0f;
        for (int i = 0; i < n - 1; i++) {
            if (text.charAt(layout.getLineEnd(i) - 1) != '\n') {
                return -1;
            }
        }
        for (int i2 = 0; i2 < n; i2++) {
            max = Math.max(max, layout.getLineWidth(i2));
        }
        return (int) FloatMath.ceil(max);
    }

    public void setIncludeFontPadding(boolean includepad) {
        if (this.mIncludePad != includepad) {
            this.mIncludePad = includepad;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public boolean getIncludeFontPadding() {
        return this.mIncludePad;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int width2;
        int width3;
        int width4;
        int hintWidth;
        int height;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        BoringLayout.Metrics boring = UNKNOWN_BORING;
        BoringLayout.Metrics hintBoring = UNKNOWN_BORING;
        if (this.mTextDir == null) {
            this.mTextDir = getTextDirectionHeuristic();
        }
        int des = -1;
        boolean fromexisting = false;
        if (widthMode == 1073741824) {
            width4 = widthSize;
        } else {
            if (this.mLayout != null && this.mEllipsize == null) {
                des = desired(this.mLayout);
            }
            if (des < 0) {
                boring = BoringLayout.isBoring(this.mTransformed, this.mTextPaint, this.mTextDir, this.mBoring);
                if (boring != null) {
                    this.mBoring = boring;
                }
            } else {
                fromexisting = true;
            }
            if (boring == null || boring == UNKNOWN_BORING) {
                if (des < 0) {
                    des = (int) FloatMath.ceil(Layout.getDesiredWidth(this.mTransformed, this.mTextPaint));
                }
                width = des;
            } else {
                width = boring.width;
            }
            Drawables dr = this.mDrawables;
            if (dr != null) {
                width = Math.max(Math.max(width, dr.mDrawableWidthTop), dr.mDrawableWidthBottom);
            }
            if (this.mHint != null) {
                int hintDes = -1;
                if (this.mHintLayout != null && this.mEllipsize == null) {
                    hintDes = desired(this.mHintLayout);
                }
                if (hintDes < 0 && (hintBoring = BoringLayout.isBoring(this.mHint, this.mTextPaint, this.mTextDir, this.mHintBoring)) != null) {
                    this.mHintBoring = hintBoring;
                }
                if (hintBoring == null || hintBoring == UNKNOWN_BORING) {
                    if (hintDes < 0) {
                        hintDes = (int) FloatMath.ceil(Layout.getDesiredWidth(this.mHint, this.mTextPaint));
                    }
                    hintWidth = hintDes;
                } else {
                    hintWidth = hintBoring.width;
                }
                if (hintWidth > width) {
                    width = hintWidth;
                }
            }
            int width5 = width + getCompoundPaddingLeft() + getCompoundPaddingRight();
            if (this.mMaxWidthMode == 1) {
                width2 = Math.min(width5, this.mMaxWidth * getLineHeight());
            } else {
                width2 = Math.min(width5, this.mMaxWidth);
            }
            if (this.mMinWidthMode == 1) {
                width3 = Math.max(width2, this.mMinWidth * getLineHeight());
            } else {
                width3 = Math.max(width2, this.mMinWidth);
            }
            width4 = Math.max(width3, getSuggestedMinimumWidth());
            if (widthMode == Integer.MIN_VALUE) {
                width4 = Math.min(widthSize, width4);
            }
        }
        int want = (width4 - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        if (this.mHorizontallyScrolling) {
            want = 1048576;
        }
        int hintWant = want;
        int hintWidth2 = this.mHintLayout == null ? hintWant : this.mHintLayout.getWidth();
        if (this.mLayout == null) {
            makeNewLayout(want, hintWant, boring, hintBoring, (width4 - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
        } else {
            boolean layoutChanged = (this.mLayout.getWidth() == want && hintWidth2 == hintWant && this.mLayout.getEllipsizedWidth() == (width4 - getCompoundPaddingLeft()) - getCompoundPaddingRight()) ? false : true;
            boolean widthChanged = this.mHint == null && this.mEllipsize == null && want > this.mLayout.getWidth() && ((this.mLayout instanceof BoringLayout) || (fromexisting && des >= 0 && des <= want));
            boolean maximumChanged = (this.mMaxMode == this.mOldMaxMode && this.mMaximum == this.mOldMaximum) ? false : true;
            if (layoutChanged || maximumChanged) {
                if (!maximumChanged && widthChanged) {
                    this.mLayout.increaseWidthTo(want);
                } else {
                    makeNewLayout(want, hintWant, boring, hintBoring, (width4 - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
                }
            }
        }
        if (heightMode == 1073741824) {
            height = heightSize;
            this.mDesiredHeightAtMeasure = -1;
        } else {
            int desired = getDesiredHeight();
            height = desired;
            this.mDesiredHeightAtMeasure = desired;
            if (heightMode == Integer.MIN_VALUE) {
                height = Math.min(desired, heightSize);
            }
        }
        int unpaddedHeight = (height - getCompoundPaddingTop()) - getCompoundPaddingBottom();
        if (this.mMaxMode == 1 && this.mLayout.getLineCount() > this.mMaximum) {
            unpaddedHeight = Math.min(unpaddedHeight, this.mLayout.getLineTop(this.mMaximum));
        }
        if (this.mMovement != null || this.mLayout.getWidth() > want || this.mLayout.getHeight() > unpaddedHeight) {
            registerForPreDraw();
        } else {
            scrollTo(0, 0);
        }
        setMeasuredDimension(width4, height);
    }

    private int getDesiredHeight() {
        return Math.max(getDesiredHeight(this.mLayout, true), getDesiredHeight(this.mHintLayout, this.mEllipsize != null));
    }

    private int getDesiredHeight(Layout layout, boolean cap) {
        if (layout == null) {
            return 0;
        }
        int linecount = layout.getLineCount();
        int pad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int desired = layout.getLineTop(linecount);
        Drawables dr = this.mDrawables;
        if (dr != null) {
            desired = Math.max(Math.max(desired, dr.mDrawableHeightLeft), dr.mDrawableHeightRight);
        }
        int desired2 = desired + pad;
        if (this.mMaxMode == 1) {
            if (cap && linecount > this.mMaximum) {
                int desired3 = layout.getLineTop(this.mMaximum);
                if (dr != null) {
                    desired3 = Math.max(Math.max(desired3, dr.mDrawableHeightLeft), dr.mDrawableHeightRight);
                }
                desired2 = desired3 + pad;
                linecount = this.mMaximum;
            }
        } else {
            desired2 = Math.min(desired2, this.mMaximum);
        }
        if (this.mMinMode == 1) {
            if (linecount < this.mMinimum) {
                desired2 += getLineHeight() * (this.mMinimum - linecount);
            }
        } else {
            desired2 = Math.max(desired2, this.mMinimum);
        }
        return Math.max(desired2, getSuggestedMinimumHeight());
    }

    private void checkForResize() {
        boolean sizeChanged = false;
        if (this.mLayout != null) {
            if (this.mLayoutParams.width == -2) {
                sizeChanged = true;
                invalidate();
            }
            if (this.mLayoutParams.height == -2) {
                int desiredHeight = getDesiredHeight();
                if (desiredHeight != getHeight()) {
                    sizeChanged = true;
                }
            } else if (this.mLayoutParams.height == -1 && this.mDesiredHeightAtMeasure >= 0) {
                int desiredHeight2 = getDesiredHeight();
                if (desiredHeight2 != this.mDesiredHeightAtMeasure) {
                    sizeChanged = true;
                }
            }
        }
        if (sizeChanged) {
            requestLayout();
        }
    }

    private void checkForRelayout() {
        if ((this.mLayoutParams.width != -2 || (this.mMaxWidthMode == this.mMinWidthMode && this.mMaxWidth == this.mMinWidth)) && ((this.mHint == null || this.mHintLayout != null) && ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight() > 0)) {
            int oldht = this.mLayout.getHeight();
            int want = this.mLayout.getWidth();
            int hintWant = this.mHintLayout == null ? 0 : this.mHintLayout.getWidth();
            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING, ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
            if (this.mEllipsize != TextUtils.TruncateAt.MARQUEE) {
                if (this.mLayoutParams.height != -2 && this.mLayoutParams.height != -1) {
                    invalidate();
                    return;
                } else if (this.mLayout.getHeight() == oldht && (this.mHintLayout == null || this.mHintLayout.getHeight() == oldht)) {
                    invalidate();
                    return;
                }
            }
            requestLayout();
            invalidate();
            return;
        }
        nullLayouts();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mDeferScroll >= 0) {
            int curs = this.mDeferScroll;
            this.mDeferScroll = -1;
            bringPointIntoView(Math.min(curs, this.mText.length()));
        }
    }

    private boolean isShowingHint() {
        return TextUtils.isEmpty(this.mText) && !TextUtils.isEmpty(this.mHint);
    }

    private boolean bringTextIntoView() {
        int scrollx;
        int scrolly;
        Layout layout = isShowingHint() ? this.mHintLayout : this.mLayout;
        int line = 0;
        if ((this.mGravity & 112) == 80) {
            line = layout.getLineCount() - 1;
        }
        Layout.Alignment a = layout.getParagraphAlignment(line);
        int dir = layout.getParagraphDirection(line);
        int hspace = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int vspace = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        int ht = layout.getHeight();
        if (a == Layout.Alignment.ALIGN_NORMAL) {
            a = dir == 1 ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
        } else if (a == Layout.Alignment.ALIGN_OPPOSITE) {
            a = dir == 1 ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
        }
        if (a == Layout.Alignment.ALIGN_CENTER) {
            int left = (int) FloatMath.floor(layout.getLineLeft(line));
            int right = (int) FloatMath.ceil(layout.getLineRight(line));
            if (right - left < hspace) {
                scrollx = ((right + left) / 2) - (hspace / 2);
            } else if (dir < 0) {
                scrollx = right - hspace;
            } else {
                scrollx = left;
            }
        } else if (a == Layout.Alignment.ALIGN_RIGHT) {
            scrollx = ((int) FloatMath.ceil(layout.getLineRight(line))) - hspace;
        } else {
            scrollx = (int) FloatMath.floor(layout.getLineLeft(line));
        }
        if (ht >= vspace && (this.mGravity & 112) == 80) {
            scrolly = ht - vspace;
        } else {
            scrolly = 0;
        }
        if (scrollx != this.mScrollX || scrolly != this.mScrollY) {
            scrollTo(scrollx, scrolly);
            return true;
        }
        return false;
    }

    public boolean bringPointIntoView(int offset) {
        int grav;
        if (isLayoutRequested()) {
            this.mDeferScroll = offset;
            return false;
        }
        boolean changed = false;
        Layout layout = isShowingHint() ? this.mHintLayout : this.mLayout;
        if (layout == null) {
            return false;
        }
        int line = layout.getLineForOffset(offset);
        switch (layout.getParagraphAlignment(line)) {
            case ALIGN_LEFT:
                grav = 1;
                break;
            case ALIGN_RIGHT:
                grav = -1;
                break;
            case ALIGN_NORMAL:
                grav = layout.getParagraphDirection(line);
                break;
            case ALIGN_OPPOSITE:
                grav = -layout.getParagraphDirection(line);
                break;
            default:
                grav = 0;
                break;
        }
        boolean clamped = grav > 0;
        int x = (int) layout.getPrimaryHorizontal(offset, clamped);
        int top = layout.getLineTop(line);
        int bottom = layout.getLineTop(line + 1);
        int left = (int) FloatMath.floor(layout.getLineLeft(line));
        int right = (int) FloatMath.ceil(layout.getLineRight(line));
        int ht = layout.getHeight();
        int hspace = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int vspace = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        if (!this.mHorizontallyScrolling && right - left > hspace && right > x) {
            right = Math.max(x, left + hspace);
        }
        int hslack = (bottom - top) / 2;
        int vslack = hslack;
        if (vslack > vspace / 4) {
            vslack = vspace / 4;
        }
        if (hslack > hspace / 4) {
            hslack = hspace / 4;
        }
        int hs = this.mScrollX;
        int vs = this.mScrollY;
        if (top - vs < vslack) {
            vs = top - vslack;
        }
        if (bottom - vs > vspace - vslack) {
            vs = bottom - (vspace - vslack);
        }
        if (ht - vs < vspace) {
            vs = ht - vspace;
        }
        if (0 - vs > 0) {
            vs = 0;
        }
        if (grav != 0) {
            if (x - hs < hslack) {
                hs = x - hslack;
            }
            if (x - hs > hspace - hslack) {
                hs = x - (hspace - hslack);
            }
        }
        if (grav < 0) {
            if (left - hs > 0) {
                hs = left;
            }
            if (right - hs < hspace) {
                hs = right - hspace;
            }
        } else if (grav > 0) {
            if (right - hs < hspace) {
                hs = right - hspace;
            }
            if (left - hs > 0) {
                hs = left;
            }
        } else if (right - left <= hspace) {
            hs = left - ((hspace - (right - left)) / 2);
        } else if (x > right - hslack) {
            hs = right - hspace;
        } else if (x < left + hslack || left > hs) {
            hs = left;
        } else if (right < hs + hspace) {
            hs = right - hspace;
        } else {
            if (x - hs < hslack) {
                hs = x - hslack;
            }
            if (x - hs > hspace - hslack) {
                hs = x - (hspace - hslack);
            }
        }
        if (hs != this.mScrollX || vs != this.mScrollY) {
            if (this.mScroller == null) {
                scrollTo(hs, vs);
            } else {
                long duration = AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll;
                int dx = hs - this.mScrollX;
                int dy = vs - this.mScrollY;
                if (duration > 250) {
                    this.mScroller.startScroll(this.mScrollX, this.mScrollY, dx, dy);
                    awakenScrollBars(this.mScroller.getDuration());
                    invalidate();
                } else {
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.abortAnimation();
                    }
                    scrollBy(dx, dy);
                }
                this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
            }
            changed = true;
        }
        if (isFocused()) {
            if (this.mTempRect == null) {
                this.mTempRect = new Rect();
            }
            this.mTempRect.set(x - 2, top, x + 2, bottom);
            getInterestingRect(this.mTempRect, line);
            this.mTempRect.offset(this.mScrollX, this.mScrollY);
            if (requestRectangleOnScreen(this.mTempRect)) {
                return true;
            }
            return changed;
        }
        return changed;
    }

    public boolean moveCursorToVisibleOffset() {
        if (!(this.mText instanceof Spannable)) {
            return false;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start != end) {
            return false;
        }
        int line = this.mLayout.getLineForOffset(start);
        int top = this.mLayout.getLineTop(line);
        int bottom = this.mLayout.getLineTop(line + 1);
        int vspace = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        int vslack = (bottom - top) / 2;
        if (vslack > vspace / 4) {
            vslack = vspace / 4;
        }
        int vs = this.mScrollY;
        if (top < vs + vslack) {
            line = this.mLayout.getLineForVertical(vs + vslack + (bottom - top));
        } else if (bottom > (vspace + vs) - vslack) {
            line = this.mLayout.getLineForVertical(((vspace + vs) - vslack) - (bottom - top));
        }
        int hspace = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int hs = this.mScrollX;
        int leftChar = this.mLayout.getOffsetForHorizontal(line, hs);
        int rightChar = this.mLayout.getOffsetForHorizontal(line, hspace + hs);
        int lowChar = leftChar < rightChar ? leftChar : rightChar;
        int highChar = leftChar > rightChar ? leftChar : rightChar;
        int newStart = start;
        if (newStart < lowChar) {
            newStart = lowChar;
        } else if (newStart > highChar) {
            newStart = highChar;
        }
        if (newStart != start) {
            Selection.setSelection((Spannable) this.mText, newStart);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (this.mScroller != null && this.mScroller.computeScrollOffset()) {
            this.mScrollX = this.mScroller.getCurrX();
            this.mScrollY = this.mScroller.getCurrY();
            invalidateParentCaches();
            postInvalidate();
        }
    }

    private void getInterestingRect(Rect r, int line) {
        convertFromViewportToContentCoordinates(r);
        if (line == 0) {
            r.top -= getExtendedPaddingTop();
        }
        if (line == this.mLayout.getLineCount() - 1) {
            r.bottom += getExtendedPaddingBottom();
        }
    }

    private void convertFromViewportToContentCoordinates(Rect r) {
        int horizontalOffset = viewportToContentHorizontalOffset();
        r.left += horizontalOffset;
        r.right += horizontalOffset;
        int verticalOffset = viewportToContentVerticalOffset();
        r.top += verticalOffset;
        r.bottom += verticalOffset;
    }

    int viewportToContentHorizontalOffset() {
        return getCompoundPaddingLeft() - this.mScrollX;
    }

    int viewportToContentVerticalOffset() {
        int offset = getExtendedPaddingTop() - this.mScrollY;
        if ((this.mGravity & 112) != 48) {
            return offset + getVerticalOffset(false);
        }
        return offset;
    }

    @Override
    public void debug(int depth) {
        String output;
        super.debug(depth);
        String output2 = debugIndent(depth) + "frame={" + this.mLeft + ", " + this.mTop + ", " + this.mRight + ", " + this.mBottom + "} scroll={" + this.mScrollX + ", " + this.mScrollY + "} ";
        if (this.mText != null) {
            output = output2 + "mText=\"" + ((Object) this.mText) + "\" ";
            if (this.mLayout != null) {
                output = output + "mLayout width=" + this.mLayout.getWidth() + " height=" + this.mLayout.getHeight();
            }
        } else {
            output = output2 + "mText=NULL";
        }
        Log.d("View", output);
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    public int getSelectionStart() {
        return Selection.getSelectionStart(getText());
    }

    @ViewDebug.ExportedProperty(category = ContactsContract.StreamItemsColumns.TEXT)
    public int getSelectionEnd() {
        return Selection.getSelectionEnd(getText());
    }

    public boolean hasSelection() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        return selectionStart >= 0 && selectionStart != selectionEnd;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public void setAllCaps(boolean allCaps) {
        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        } else {
            setTransformationMethod(null);
        }
    }

    @RemotableViewMethod
    public void setSingleLine(boolean singleLine) {
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine, true, true);
    }

    private void setInputTypeSingleLine(boolean singleLine) {
        if (this.mEditor != null && (this.mEditor.mInputType & 15) == 1) {
            if (singleLine) {
                this.mEditor.mInputType &= -131073;
            } else {
                this.mEditor.mInputType |= 131072;
            }
        }
    }

    private void applySingleLine(boolean singleLine, boolean applyTransformation, boolean changeMaxLines) {
        this.mSingleLine = singleLine;
        if (singleLine) {
            setLines(1);
            setHorizontallyScrolling(true);
            if (applyTransformation) {
                setTransformationMethod(SingleLineTransformationMethod.getInstance());
                return;
            }
            return;
        }
        if (changeMaxLines) {
            setMaxLines(Integer.MAX_VALUE);
        }
        setHorizontallyScrolling(false);
        if (applyTransformation) {
            setTransformationMethod(null);
        }
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        if (this.mEllipsize != where) {
            this.mEllipsize = where;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setMarqueeRepeatLimit(int marqueeLimit) {
        this.mMarqueeRepeatLimit = marqueeLimit;
    }

    public int getMarqueeRepeatLimit() {
        return this.mMarqueeRepeatLimit;
    }

    @ViewDebug.ExportedProperty
    public TextUtils.TruncateAt getEllipsize() {
        return this.mEllipsize;
    }

    @RemotableViewMethod
    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
        createEditorIfNeeded();
        this.mEditor.mSelectAllOnFocus = selectAllOnFocus;
        if (selectAllOnFocus && !(this.mText instanceof Spannable)) {
            setText(this.mText, BufferType.SPANNABLE);
        }
    }

    @RemotableViewMethod
    public void setCursorVisible(boolean visible) {
        if (!visible || this.mEditor != null) {
            createEditorIfNeeded();
            if (this.mEditor.mCursorVisible != visible) {
                this.mEditor.mCursorVisible = visible;
                invalidate();
                this.mEditor.makeBlink();
                this.mEditor.prepareCursorControllers();
            }
        }
    }

    public boolean isCursorVisible() {
        if (this.mEditor == null) {
            return true;
        }
        return this.mEditor.mCursorVisible;
    }

    private boolean canMarquee() {
        int width = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        if (width > 0) {
            return this.mLayout.getLineWidth(0) > ((float) width) || !(this.mMarqueeFadeMode == 0 || this.mSavedMarqueeModeLayout == null || this.mSavedMarqueeModeLayout.getLineWidth(0) <= ((float) width));
        }
        return false;
    }

    private void startMarquee() {
        if (getKeyListener() == null && !compressText((getWidth() - getCompoundPaddingLeft()) - getCompoundPaddingRight())) {
            if (this.mMarquee == null || this.mMarquee.isStopped()) {
                if ((isFocused() || isSelected()) && getLineCount() == 1 && canMarquee()) {
                    if (this.mMarqueeFadeMode == 1) {
                        this.mMarqueeFadeMode = 2;
                        Layout tmp = this.mLayout;
                        this.mLayout = this.mSavedMarqueeModeLayout;
                        this.mSavedMarqueeModeLayout = tmp;
                        setHorizontalFadingEdgeEnabled(true);
                        requestLayout();
                        invalidate();
                    }
                    if (this.mMarquee == null) {
                        this.mMarquee = new Marquee(this);
                    }
                    this.mMarquee.start(this.mMarqueeRepeatLimit);
                }
            }
        }
    }

    private void stopMarquee() {
        if (this.mMarquee != null && !this.mMarquee.isStopped()) {
            this.mMarquee.stop();
        }
        if (this.mMarqueeFadeMode == 2) {
            this.mMarqueeFadeMode = 1;
            Layout tmp = this.mSavedMarqueeModeLayout;
            this.mSavedMarqueeModeLayout = this.mLayout;
            this.mLayout = tmp;
            setHorizontalFadingEdgeEnabled(false);
            requestLayout();
            invalidate();
        }
    }

    private void startStopMarquee(boolean start) {
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (start) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }

    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    }

    protected void onSelectionChanged(int selStart, int selEnd) {
        sendAccessibilityEvent(8192);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList<>();
        }
        this.mListeners.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        int i;
        if (this.mListeners != null && (i = this.mListeners.indexOf(watcher)) >= 0) {
            this.mListeners.remove(i);
        }
    }

    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> list = this.mListeners;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).beforeTextChanged(text, start, before, after);
            }
        }
        removeIntersectingNonAdjacentSpans(start, start + before, SpellCheckSpan.class);
        removeIntersectingNonAdjacentSpans(start, start + before, SuggestionSpan.class);
    }

    private <T> void removeIntersectingNonAdjacentSpans(int start, int end, Class<T> type) {
        if (this.mText instanceof Editable) {
            Editable text = (Editable) this.mText;
            Object[] spans = text.getSpans(start, end, type);
            int length = spans.length;
            for (int i = 0; i < length; i++) {
                int spanStart = text.getSpanStart(spans[i]);
                int spanEnd = text.getSpanEnd(spans[i]);
                if (spanEnd != start && spanStart != end) {
                    text.removeSpan(spans[i]);
                } else {
                    return;
                }
            }
        }
    }

    void removeAdjacentSuggestionSpans(int pos) {
        if (this.mText instanceof Editable) {
            Editable text = (Editable) this.mText;
            SuggestionSpan[] spans = (SuggestionSpan[]) text.getSpans(pos, pos, SuggestionSpan.class);
            int length = spans.length;
            for (int i = 0; i < length; i++) {
                int spanStart = text.getSpanStart(spans[i]);
                int spanEnd = text.getSpanEnd(spans[i]);
                if ((spanEnd == pos || spanStart == pos) && SpellChecker.haveWordBoundariesChanged(text, pos, pos, spanStart, spanEnd)) {
                    text.removeSpan(spans[i]);
                }
            }
        }
    }

    void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> list = this.mListeners;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).onTextChanged(text, start, before, after);
            }
        }
        if (this.mEditor != null) {
            this.mEditor.sendOnTextChanged(start, after);
        }
    }

    void sendAfterTextChanged(Editable text) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> list = this.mListeners;
            int count = list.size();
            for (int i = 0; i < count; i++) {
                list.get(i).afterTextChanged(text);
            }
        }
        hideErrorIfUnchanged();
    }

    void updateAfterEdit() {
        invalidate();
        int curs = getSelectionStart();
        if (curs >= 0 || (this.mGravity & 112) == 80) {
            registerForPreDraw();
        }
        checkForResize();
        if (curs >= 0) {
            this.mHighlightPathBogus = true;
            if (this.mEditor != null) {
                this.mEditor.makeBlink();
            }
            bringPointIntoView(curs);
        }
    }

    void handleTextChanged(CharSequence buffer, int start, int before, int after) {
        Editor.InputMethodState ims = this.mEditor == null ? null : this.mEditor.mInputMethodState;
        if (ims == null || ims.mBatchEditNesting == 0) {
            updateAfterEdit();
        }
        if (ims != null) {
            ims.mContentChanged = true;
            if (ims.mChangedStart < 0) {
                ims.mChangedStart = start;
                ims.mChangedEnd = start + before;
            } else {
                ims.mChangedStart = Math.min(ims.mChangedStart, start);
                ims.mChangedEnd = Math.max(ims.mChangedEnd, (start + before) - ims.mChangedDelta);
            }
            ims.mChangedDelta += after - before;
        }
        resetErrorChangedFlag();
        sendOnTextChanged(buffer, start, before, after);
        onTextChanged(buffer, start, before, after);
    }

    void spanChange(Spanned buf, Object what, int oldStart, int newStart, int oldEnd, int newEnd) {
        boolean selChanged = false;
        int newSelStart = -1;
        int newSelEnd = -1;
        Editor.InputMethodState ims = this.mEditor == null ? null : this.mEditor.mInputMethodState;
        if (what == Selection.SELECTION_END) {
            selChanged = true;
            newSelEnd = newStart;
            if (oldStart >= 0 || newStart >= 0) {
                invalidateCursor(Selection.getSelectionStart(buf), oldStart, newStart);
                checkForResize();
                registerForPreDraw();
                if (this.mEditor != null) {
                    this.mEditor.makeBlink();
                }
            }
        }
        if (what == Selection.SELECTION_START) {
            selChanged = true;
            newSelStart = newStart;
            if (oldStart >= 0 || newStart >= 0) {
                int end = Selection.getSelectionEnd(buf);
                invalidateCursor(end, oldStart, newStart);
            }
        }
        if (selChanged) {
            this.mHighlightPathBogus = true;
            if (this.mEditor != null && !isFocused()) {
                this.mEditor.mSelectionMoved = true;
            }
            if ((buf.getSpanFlags(what) & 512) == 0) {
                if (newSelStart < 0) {
                    newSelStart = Selection.getSelectionStart(buf);
                }
                if (newSelEnd < 0) {
                    newSelEnd = Selection.getSelectionEnd(buf);
                }
                onSelectionChanged(newSelStart, newSelEnd);
            }
        }
        if ((what instanceof UpdateAppearance) || (what instanceof ParagraphStyle) || (what instanceof CharacterStyle)) {
            if (ims == null || ims.mBatchEditNesting == 0) {
                invalidate();
                this.mHighlightPathBogus = true;
                checkForResize();
            } else {
                ims.mContentChanged = true;
            }
            if (this.mEditor != null) {
                if (oldStart >= 0) {
                    this.mEditor.invalidateTextDisplayList(this.mLayout, oldStart, oldEnd);
                }
                if (newStart >= 0) {
                    this.mEditor.invalidateTextDisplayList(this.mLayout, newStart, newEnd);
                }
            }
        }
        if (MetaKeyKeyListener.isMetaTracker(buf, what)) {
            this.mHighlightPathBogus = true;
            if (ims != null && MetaKeyKeyListener.isSelectingMetaTracker(buf, what)) {
                ims.mSelectionModeChanged = true;
            }
            if (Selection.getSelectionStart(buf) >= 0) {
                if (ims == null || ims.mBatchEditNesting == 0) {
                    invalidateCursor();
                } else {
                    ims.mCursorChanged = true;
                }
            }
        }
        if ((what instanceof ParcelableSpan) && ims != null && ims.mExtractedTextRequest != null) {
            if (ims.mBatchEditNesting != 0) {
                if (oldStart >= 0) {
                    if (ims.mChangedStart > oldStart) {
                        ims.mChangedStart = oldStart;
                    }
                    if (ims.mChangedStart > oldEnd) {
                        ims.mChangedStart = oldEnd;
                    }
                }
                if (newStart >= 0) {
                    if (ims.mChangedStart > newStart) {
                        ims.mChangedStart = newStart;
                    }
                    if (ims.mChangedStart > newEnd) {
                        ims.mChangedStart = newEnd;
                    }
                }
            } else {
                ims.mContentChanged = true;
            }
        }
        if (this.mEditor != null && this.mEditor.mSpellChecker != null && newStart < 0 && (what instanceof SpellCheckSpan)) {
            this.mEditor.mSpellChecker.onSpellCheckSpanRemoved((SpellCheckSpan) what);
        }
    }

    @Override
    public void dispatchFinishTemporaryDetach() {
        this.mDispatchTemporaryDetach = true;
        super.dispatchFinishTemporaryDetach();
        this.mDispatchTemporaryDetach = false;
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
        if (!this.mDispatchTemporaryDetach) {
            this.mTemporaryDetach = true;
        }
        if (this.mEditor != null) {
            this.mEditor.mTemporaryDetach = true;
        }
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
        if (!this.mDispatchTemporaryDetach) {
            this.mTemporaryDetach = false;
        }
        if (this.mEditor != null) {
            this.mEditor.mTemporaryDetach = false;
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (this.mTemporaryDetach) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            return;
        }
        if (this.mEditor != null) {
            this.mEditor.onFocusChanged(focused, direction);
        }
        if (focused && (this.mText instanceof Spannable)) {
            Spannable sp = (Spannable) this.mText;
            MetaKeyKeyListener.resetMetaState(sp);
        }
        startStopMarquee(focused);
        if (this.mTransformation != null) {
            this.mTransformation.onFocusChanged(this, this.mText, focused, direction, previouslyFocusedRect);
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (this.mEditor != null) {
            this.mEditor.onWindowFocusChanged(hasWindowFocus);
        }
        startStopMarquee(hasWindowFocus);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (this.mEditor != null && visibility != 0) {
            this.mEditor.hideControllers();
        }
    }

    public void clearComposingText() {
        if (this.mText instanceof Spannable) {
            BaseInputConnection.removeComposingSpans((Spannable) this.mText);
        }
    }

    @Override
    public void setSelected(boolean selected) {
        boolean wasSelected = isSelected();
        super.setSelected(selected);
        if (selected != wasSelected && this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (selected) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean z = false;
        int action = event.getActionMasked();
        if (this.mEditor != null) {
            this.mEditor.onTouchEvent(event);
        }
        boolean superResult = super.onTouchEvent(event);
        if (this.mEditor != null && this.mEditor.mDiscardNextActionUp && action == 1) {
            this.mEditor.mDiscardNextActionUp = false;
            return superResult;
        }
        boolean touchIsFinished = action == 1 && (this.mEditor == null || !this.mEditor.mIgnoreActionUpEvent) && isFocused();
        if ((this.mMovement != null || onCheckIsTextEditor()) && isEnabled() && (this.mText instanceof Spannable) && this.mLayout != null) {
            boolean handled = this.mMovement != null ? false | this.mMovement.onTouchEvent(this, (Spannable) this.mText, event) : false;
            boolean textIsSelectable = isTextSelectable();
            if (touchIsFinished && this.mLinksClickable && this.mAutoLinkMask != 0 && textIsSelectable) {
                ClickableSpan[] links = (ClickableSpan[]) ((Spannable) this.mText).getSpans(getSelectionStart(), getSelectionEnd(), ClickableSpan.class);
                if (links.length > 0) {
                    links[0].onClick(this);
                    handled = true;
                }
            }
            if (touchIsFinished && (isTextEditable() || textIsSelectable)) {
                InputMethodManager imm = InputMethodManager.peekInstance();
                viewClicked(imm);
                if (!textIsSelectable && this.mEditor.mShowSoftInputOnFocus) {
                    if (imm != null && imm.showSoftInput(this, 0)) {
                        z = true;
                    }
                    boolean z2 = handled | z;
                }
                this.mEditor.onTouchUpEvent(event);
                handled = true;
            }
            if (handled) {
                return true;
            }
            return superResult;
        }
        return superResult;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (this.mMovement != null && (this.mText instanceof Spannable) && this.mLayout != null) {
            try {
                if (this.mMovement.onGenericMotionEvent(this, (Spannable) this.mText, event)) {
                    return true;
                }
            } catch (AbstractMethodError e) {
            }
        }
        return super.onGenericMotionEvent(event);
    }

    boolean isTextEditable() {
        return (this.mText instanceof Editable) && onCheckIsTextEditor() && isEnabled();
    }

    public boolean didTouchFocusSelect() {
        return this.mEditor != null && this.mEditor.mTouchFocusSelected;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        if (this.mEditor != null) {
            this.mEditor.mIgnoreActionUpEvent = true;
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (this.mMovement == null || !(this.mText instanceof Spannable) || this.mLayout == null || !this.mMovement.onTrackballEvent(this, (Spannable) this.mText, event)) {
            return super.onTrackballEvent(event);
        }
        return true;
    }

    public void setScroller(Scroller s) {
        this.mScroller = s;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 1) {
            if (this.mMarquee != null && !this.mMarquee.isStopped()) {
                Marquee marquee = this.mMarquee;
                if (!marquee.shouldDrawLeftFade()) {
                    return 0.0f;
                }
                float scroll = marquee.getScroll();
                return scroll / getHorizontalFadingEdgeLength();
            }
            if (getLineCount() == 1) {
                int layoutDirection = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
                switch (absoluteGravity & 7) {
                    case 1:
                    case 7:
                        int textDirection = this.mLayout.getParagraphDirection(0);
                        if (textDirection != 1) {
                            return ((((this.mLayout.getLineRight(0) - (this.mRight - this.mLeft)) - getCompoundPaddingLeft()) - getCompoundPaddingRight()) - this.mLayout.getLineLeft(0)) / getHorizontalFadingEdgeLength();
                        }
                        return 0.0f;
                    case 3:
                        return 0.0f;
                    case 5:
                        return ((((this.mLayout.getLineRight(0) - (this.mRight - this.mLeft)) - getCompoundPaddingLeft()) - getCompoundPaddingRight()) - this.mLayout.getLineLeft(0)) / getHorizontalFadingEdgeLength();
                }
            }
        }
        return super.getLeftFadingEdgeStrength();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 1) {
            if (this.mMarquee != null && !this.mMarquee.isStopped()) {
                Marquee marquee = this.mMarquee;
                float maxFadeScroll = marquee.getMaxFadeScroll();
                float scroll = marquee.getScroll();
                return (maxFadeScroll - scroll) / getHorizontalFadingEdgeLength();
            }
            if (getLineCount() == 1) {
                int layoutDirection = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
                switch (absoluteGravity & 7) {
                    case 1:
                    case 7:
                        int textDirection = this.mLayout.getParagraphDirection(0);
                        if (textDirection != -1) {
                            return (this.mLayout.getLineWidth(0) - (((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight())) / getHorizontalFadingEdgeLength();
                        }
                        return 0.0f;
                    case 3:
                        int textWidth = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
                        float lineWidth = this.mLayout.getLineWidth(0);
                        return (lineWidth - textWidth) / getHorizontalFadingEdgeLength();
                    case 5:
                        return 0.0f;
                }
            }
        }
        return super.getRightFadingEdgeStrength();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        if (this.mLayout != null) {
            return (this.mSingleLine && (this.mGravity & 7) == 3) ? (int) this.mLayout.getLineWidth(0) : this.mLayout.getWidth();
        }
        return super.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return this.mLayout != null ? this.mLayout.getHeight() : super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return (getHeight() - getCompoundPaddingTop()) - getCompoundPaddingBottom();
    }

    @Override
    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
        super.findViewsWithText(outViews, searched, flags);
        if (!outViews.contains(this) && (flags & 1) != 0 && !TextUtils.isEmpty(searched) && !TextUtils.isEmpty(this.mText)) {
            String searchedLowerCase = searched.toString().toLowerCase();
            String textLowerCase = this.mText.toString().toLowerCase();
            if (textLowerCase.contains(searchedLowerCase)) {
                outViews.add(this);
            }
        }
    }

    public static ColorStateList getTextColors(Context context, TypedArray attrs) {
        int ap;
        if (attrs == null) {
            throw new NullPointerException();
        }
        TypedArray a = context.obtainStyledAttributes(android.R.styleable.TextView);
        ColorStateList colors = a.getColorStateList(5);
        if (colors == null && (ap = a.getResourceId(1, 0)) != 0) {
            TypedArray appearance = context.obtainStyledAttributes(ap, android.R.styleable.TextAppearance);
            colors = appearance.getColorStateList(3);
            appearance.recycle();
        }
        a.recycle();
        return colors;
    }

    public static int getTextColor(Context context, TypedArray attrs, int def) {
        ColorStateList colors = getTextColors(context, attrs);
        if (colors == null) {
            return def;
        }
        int def2 = colors.getDefaultColor();
        return def2;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        int filteredMetaState = event.getMetaState() & (-28673);
        if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
            switch (keyCode) {
                case 29:
                    if (canSelectText()) {
                        return onTextContextMenuItem(16908319);
                    }
                    break;
                case 31:
                    if (canCopy()) {
                        return onTextContextMenuItem(16908321);
                    }
                    break;
                case 50:
                    if (canPaste()) {
                        return onTextContextMenuItem(16908322);
                    }
                    break;
                case 52:
                    if (canCut()) {
                        return onTextContextMenuItem(16908320);
                    }
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    private boolean canSelectText() {
        return (this.mText.length() == 0 || this.mEditor == null || !this.mEditor.hasSelectionController()) ? false : true;
    }

    boolean textCanBeSelected() {
        if (this.mMovement == null || !this.mMovement.canSelectArbitrarily()) {
            return false;
        }
        return isTextEditable() || (isTextSelectable() && (this.mText instanceof Spannable) && isEnabled());
    }

    private Locale getTextServicesLocale(boolean allowNullLocale) {
        updateTextServicesLocaleAsync();
        return (this.mCurrentSpellCheckerLocaleCache != null || allowNullLocale) ? this.mCurrentSpellCheckerLocaleCache : Locale.getDefault();
    }

    public Locale getTextServicesLocale() {
        return getTextServicesLocale(false);
    }

    public Locale getSpellCheckerLocale() {
        return getTextServicesLocale(true);
    }

    private void updateTextServicesLocaleAsync() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                TextView.this.updateTextServicesLocaleLocked();
            }
        });
    }

    private void updateTextServicesLocaleLocked() {
        Locale locale;
        TextServicesManager textServicesManager = (TextServicesManager) this.mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        SpellCheckerSubtype subtype = textServicesManager.getCurrentSpellCheckerSubtype(true);
        if (subtype != null) {
            locale = SpellCheckerSubtype.constructLocaleFromString(subtype.getLocale());
        } else {
            locale = null;
        }
        this.mCurrentSpellCheckerLocaleCache = locale;
    }

    void onLocaleChanged() {
        this.mEditor.mWordIterator = null;
    }

    public WordIterator getWordIterator() {
        if (this.mEditor != null) {
            return this.mEditor.getWordIterator();
        }
        return null;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        boolean isPassword = hasPasswordTransformationMethod();
        if (!isPassword || shouldSpeakPasswordsForAccessibility()) {
            CharSequence text = getTextForAccessibility();
            if (!TextUtils.isEmpty(text)) {
                event.getText().add(text);
            }
        }
    }

    private boolean shouldSpeakPasswordsForAccessibility() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0, -3) == 1;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TextView.class.getName());
        boolean isPassword = hasPasswordTransformationMethod();
        event.setPassword(isPassword);
        if (event.getEventType() == 8192) {
            event.setFromIndex(Selection.getSelectionStart(this.mText));
            event.setToIndex(Selection.getSelectionEnd(this.mText));
            event.setItemCount(this.mText.length());
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TextView.class.getName());
        boolean isPassword = hasPasswordTransformationMethod();
        info.setPassword(isPassword);
        if (!isPassword || shouldSpeakPasswordsForAccessibility()) {
            info.setText(getTextForAccessibility());
        }
        if (this.mBufferType == BufferType.EDITABLE) {
            info.setEditable(true);
        }
        if (this.mEditor != null) {
            info.setInputType(this.mEditor.mInputType);
            if (this.mEditor.mError != null) {
                info.setContentInvalid(true);
                info.setError(this.mEditor.mError);
            }
        }
        if (!TextUtils.isEmpty(this.mText)) {
            info.addAction(256);
            info.addAction(512);
            info.setMovementGranularities(31);
        }
        if (isFocused()) {
            if (canSelectText()) {
                info.addAction(131072);
            }
            if (canCopy()) {
                info.addAction(16384);
            }
            if (canPaste()) {
                info.addAction(32768);
            }
            if (canCut()) {
                info.addAction(65536);
            }
        }
        int numFilters = this.mFilters.length;
        for (int i = 0; i < numFilters; i++) {
            InputFilter filter = this.mFilters[i];
            if (filter instanceof InputFilter.LengthFilter) {
                info.setMaxTextLength(((InputFilter.LengthFilter) filter).getMax());
            }
        }
        if (!isSingleLine()) {
            info.setMultiLine(true);
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        switch (action) {
            case 16:
                boolean handled = false;
                if (isClickable() || isLongClickable()) {
                    if (isFocusable() && !isFocused()) {
                        requestFocus();
                    }
                    performClick();
                    handled = true;
                }
                if ((this.mMovement == null && !onCheckIsTextEditor()) || !isEnabled() || !(this.mText instanceof Spannable) || this.mLayout == null) {
                    return handled;
                }
                if ((!isTextEditable() && !isTextSelectable()) || !isFocused()) {
                    return handled;
                }
                InputMethodManager imm = InputMethodManager.peekInstance();
                viewClicked(imm);
                return (isTextSelectable() || !this.mEditor.mShowSoftInputOnFocus || imm == null) ? handled : handled | imm.showSoftInput(this, 0);
            case 256:
            case 512:
                ensureIterableTextForAccessibilitySelectable();
                return super.performAccessibilityActionInternal(action, arguments);
            case 16384:
                return isFocused() && canCopy() && onTextContextMenuItem(16908321);
            case 32768:
                return isFocused() && canPaste() && onTextContextMenuItem(16908322);
            case 65536:
                return isFocused() && canCut() && onTextContextMenuItem(16908320);
            case 131072:
                if (isFocused() && canSelectText()) {
                    ensureIterableTextForAccessibilitySelectable();
                    CharSequence text = getIterableTextForAccessibility();
                    if (text == null) {
                        return false;
                    }
                    int start = arguments != null ? arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1) : -1;
                    int end = arguments != null ? arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1) : -1;
                    if (getSelectionStart() != start || getSelectionEnd() != end) {
                        if (start == end && end == -1) {
                            Selection.removeSelection((Spannable) text);
                            return true;
                        }
                        if (start >= 0 && start <= end && end <= text.length()) {
                            Selection.setSelection((Spannable) text, start, end);
                            if (this.mEditor == null) {
                                return true;
                            }
                            this.mEditor.startSelectionActionMode();
                            return true;
                        }
                    }
                }
                return false;
            default:
                return super.performAccessibilityActionInternal(action, arguments);
        }
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
        if (eventType != 4096) {
            super.sendAccessibilityEvent(eventType);
        }
    }

    public CharSequence getTextForAccessibility() {
        CharSequence text = getText();
        if (TextUtils.isEmpty(text)) {
            return getHint();
        }
        return text;
    }

    void sendAccessibilityEventTypeViewTextChanged(CharSequence beforeText, int fromIndex, int removedCount, int addedCount) {
        AccessibilityEvent event = AccessibilityEvent.obtain(16);
        event.setFromIndex(fromIndex);
        event.setRemovedCount(removedCount);
        event.setAddedCount(addedCount);
        event.setBeforeText(beforeText);
        sendAccessibilityEventUnchecked(event);
    }

    public boolean isInputMethodTarget() {
        InputMethodManager imm = InputMethodManager.peekInstance();
        return imm != null && imm.isActive(this);
    }

    public boolean onTextContextMenuItem(int id) {
        int min = 0;
        int max = this.mText.length();
        if (isFocused()) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();
            min = Math.max(0, Math.min(selStart, selEnd));
            max = Math.max(0, Math.max(selStart, selEnd));
        }
        switch (id) {
            case 16908319:
                selectAllText();
                return true;
            case 16908320:
                setPrimaryClip(ClipData.newPlainText(null, getTransformedText(min, max)));
                deleteText_internal(min, max);
                stopSelectionActionMode();
                return true;
            case 16908321:
                setPrimaryClip(ClipData.newPlainText(null, getTransformedText(min, max)));
                stopSelectionActionMode();
                return true;
            case 16908322:
                paste(min, max);
                return true;
            default:
                return false;
        }
    }

    CharSequence getTransformedText(int start, int end) {
        return removeSuggestionSpans(this.mTransformed.subSequence(start, end));
    }

    @Override
    public boolean performLongClick() {
        boolean handled = false;
        if (super.performLongClick()) {
            handled = true;
        }
        if (this.mEditor != null) {
            handled |= this.mEditor.performLongClick(handled);
        }
        if (handled) {
            performHapticFeedback(0);
            if (this.mEditor != null) {
                this.mEditor.mDiscardNextActionUp = true;
            }
        }
        return handled;
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        if (this.mEditor != null) {
            this.mEditor.onScrollChanged();
        }
    }

    public boolean isSuggestionsEnabled() {
        if (this.mEditor == null || (this.mEditor.mInputType & 15) != 1 || (this.mEditor.mInputType & 524288) > 0) {
            return false;
        }
        int variation = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION;
        return variation == 0 || variation == 48 || variation == 80 || variation == 64 || variation == 160;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        createEditorIfNeeded();
        this.mEditor.mCustomSelectionActionModeCallback = actionModeCallback;
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mCustomSelectionActionModeCallback;
    }

    protected void stopSelectionActionMode() {
        this.mEditor.stopSelectionActionMode();
    }

    boolean canCut() {
        return !hasPasswordTransformationMethod() && this.mText.length() > 0 && hasSelection() && (this.mText instanceof Editable) && this.mEditor != null && this.mEditor.mKeyListener != null;
    }

    boolean canCopy() {
        return !hasPasswordTransformationMethod() && this.mText.length() > 0 && hasSelection() && this.mEditor != null;
    }

    boolean canPaste() {
        return (this.mText instanceof Editable) && this.mEditor != null && this.mEditor.mKeyListener != null && getSelectionStart() >= 0 && getSelectionEnd() >= 0 && ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).hasPrimaryClip();
    }

    boolean selectAllText() {
        int length = this.mText.length();
        Selection.setSelection((Spannable) this.mText, 0, length);
        return length > 0;
    }

    private void paste(int min, int max) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            boolean didFirst = false;
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).coerceToStyledText(getContext());
                if (paste != null) {
                    if (!didFirst) {
                        Selection.setSelection((Spannable) this.mText, max);
                        ((Editable) this.mText).replace(min, max, paste);
                        didFirst = true;
                    } else {
                        ((Editable) this.mText).insert(getSelectionEnd(), "\n");
                        ((Editable) this.mText).insert(getSelectionEnd(), paste);
                    }
                }
            }
            stopSelectionActionMode();
            LAST_CUT_OR_COPY_TIME = 0L;
        }
    }

    private void setPrimaryClip(ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
        LAST_CUT_OR_COPY_TIME = SystemClock.uptimeMillis();
    }

    public int getOffsetForPosition(float x, float y) {
        if (getLayout() == null) {
            return -1;
        }
        int line = getLineAtCoordinate(y);
        return getOffsetAtCoordinate(line, x);
    }

    float convertToLocalHorizontalCoordinate(float x) {
        return Math.min((getWidth() - getTotalPaddingRight()) - 1, Math.max(0.0f, x - getTotalPaddingLeft())) + getScrollX();
    }

    int getLineAtCoordinate(float y) {
        return getLayout().getLineForVertical((int) (Math.min((getHeight() - getTotalPaddingBottom()) - 1, Math.max(0.0f, y - getTotalPaddingTop())) + getScrollY()));
    }

    private int getOffsetAtCoordinate(int line, float x) {
        return getLayout().getOffsetForHorizontal(line, convertToLocalHorizontalCoordinate(x));
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case 1:
                return this.mEditor != null && this.mEditor.hasInsertionController();
            case 2:
                int offset = getOffsetForPosition(event.getX(), event.getY());
                Selection.setSelection((Spannable) this.mText, offset);
                return true;
            case 3:
                if (this.mEditor == null) {
                    return true;
                }
                this.mEditor.onDrop(event);
                return true;
            case 4:
            default:
                return true;
            case 5:
                requestFocus();
                return true;
        }
    }

    boolean isInBatchEditMode() {
        if (this.mEditor == null) {
            return false;
        }
        Editor.InputMethodState ims = this.mEditor.mInputMethodState;
        if (ims != null) {
            return ims.mBatchEditNesting > 0;
        }
        return this.mEditor.mInBatchEditControllers;
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        this.mTextDir = getTextDirectionHeuristic();
        if (this.mLayout != null) {
            checkForRelayout();
        }
    }

    TextDirectionHeuristic getTextDirectionHeuristic() {
        if (hasPasswordTransformationMethod()) {
            return TextDirectionHeuristics.LTR;
        }
        boolean defaultIsRtl = getLayoutDirection() == 1;
        switch (getTextDirection()) {
            case 2:
                return TextDirectionHeuristics.ANYRTL_LTR;
            case 3:
                return TextDirectionHeuristics.LTR;
            case 4:
                return TextDirectionHeuristics.RTL;
            case 5:
                return TextDirectionHeuristics.LOCALE;
            default:
                return defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL : TextDirectionHeuristics.FIRSTSTRONG_LTR;
        }
    }

    @Override
    public void onResolveDrawables(int layoutDirection) {
        if (this.mLastLayoutDirection != layoutDirection) {
            this.mLastLayoutDirection = layoutDirection;
            if (this.mDrawables != null) {
                this.mDrawables.resolveWithLayoutDirection(layoutDirection);
            }
        }
    }

    @Override
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();
        this.mLastLayoutDirection = -1;
    }

    protected void viewClicked(InputMethodManager imm) {
        if (imm != null) {
            imm.viewClicked(this);
        }
    }

    protected void deleteText_internal(int start, int end) {
        ((Editable) this.mText).delete(start, end);
    }

    protected void replaceText_internal(int start, int end, CharSequence text) {
        ((Editable) this.mText).replace(start, end, text);
    }

    protected void setSpan_internal(Object span, int start, int end, int flags) {
        ((Editable) this.mText).setSpan(span, start, end, flags);
    }

    protected void setCursorPosition_internal(int start, int end) {
        Selection.setSelection((Editable) this.mText, start, end);
    }

    private void createEditorIfNeeded() {
        if (this.mEditor == null) {
            this.mEditor = new Editor(this);
        }
    }

    @Override
    public CharSequence getIterableTextForAccessibility() {
        return this.mText;
    }

    private void ensureIterableTextForAccessibilitySelectable() {
        if (!(this.mText instanceof Spannable)) {
            setText(this.mText, BufferType.SPANNABLE);
        }
    }

    @Override
    public AccessibilityIterators.TextSegmentIterator getIteratorForGranularity(int granularity) {
        switch (granularity) {
            case 4:
                Spannable text = (Spannable) getIterableTextForAccessibility();
                if (!TextUtils.isEmpty(text) && getLayout() != null) {
                    AccessibilityIterators.LineTextSegmentIterator iterator = AccessibilityIterators.LineTextSegmentIterator.getInstance();
                    iterator.initialize(text, getLayout());
                    return iterator;
                }
                break;
            case 16:
                if (!TextUtils.isEmpty((Spannable) getIterableTextForAccessibility()) && getLayout() != null) {
                    AccessibilityIterators.PageTextSegmentIterator iterator2 = AccessibilityIterators.PageTextSegmentIterator.getInstance();
                    iterator2.initialize(this);
                    return iterator2;
                }
                break;
        }
        return super.getIteratorForGranularity(granularity);
    }

    @Override
    public int getAccessibilitySelectionStart() {
        return getSelectionStart();
    }

    @Override
    public boolean isAccessibilitySelectionExtendable() {
        return true;
    }

    @Override
    public int getAccessibilitySelectionEnd() {
        return getSelectionEnd();
    }

    @Override
    public void setAccessibilitySelection(int start, int end) {
        if (getAccessibilitySelectionStart() != start || getAccessibilitySelectionEnd() != end) {
            if (this.mEditor != null) {
                this.mEditor.hideControllers();
            }
            CharSequence text = getIterableTextForAccessibility();
            if (Math.min(start, end) >= 0 && Math.max(start, end) <= text.length()) {
                Selection.setSelection((Spannable) text, start, end);
            } else {
                Selection.removeSelection((Spannable) text);
            }
        }
    }

    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        CharSequence error;
        boolean frozenWithFocus;
        int selEnd;
        int selStart;
        CharSequence text;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.selStart);
            out.writeInt(this.selEnd);
            out.writeInt(this.frozenWithFocus ? 1 : 0);
            TextUtils.writeToParcel(this.text, out, flags);
            if (this.error == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                TextUtils.writeToParcel(this.error, out, flags);
            }
        }

        public String toString() {
            String str = "TextView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " start=" + this.selStart + " end=" + this.selEnd;
            if (this.text != null) {
                str = str + " text=" + ((Object) this.text);
            }
            return str + "}";
        }

        private SavedState(Parcel in) {
            super(in);
            this.selStart = in.readInt();
            this.selEnd = in.readInt();
            this.frozenWithFocus = in.readInt() != 0;
            this.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            if (in.readInt() != 0) {
                this.error = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            }
        }
    }

    private static class CharWrapper implements CharSequence, GetChars, GraphicsOperations {
        private char[] mChars;
        private int mLength;
        private int mStart;

        public CharWrapper(char[] chars, int start, int len) {
            this.mChars = chars;
            this.mStart = start;
            this.mLength = len;
        }

        void set(char[] chars, int start, int len) {
            this.mChars = chars;
            this.mStart = start;
            this.mLength = len;
        }

        @Override
        public int length() {
            return this.mLength;
        }

        @Override
        public char charAt(int off) {
            return this.mChars[this.mStart + off];
        }

        @Override
        public String toString() {
            return new String(this.mChars, this.mStart, this.mLength);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start < 0 || end < 0 || start > this.mLength || end > this.mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }
            return new String(this.mChars, this.mStart + start, end - start);
        }

        @Override
        public void getChars(int start, int end, char[] buf, int off) {
            if (start < 0 || end < 0 || start > this.mLength || end > this.mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }
            System.arraycopy(this.mChars, this.mStart + start, buf, off, end - start);
        }

        @Override
        public void drawText(Canvas c, int start, int end, float x, float y, Paint p) {
            c.drawText(this.mChars, start + this.mStart, end - start, x, y, p);
        }

        @Override
        public void drawTextRun(Canvas c, int start, int end, int contextStart, int contextEnd, float x, float y, boolean isRtl, Paint p) {
            int count = end - start;
            int contextCount = contextEnd - contextStart;
            c.drawTextRun(this.mChars, start + this.mStart, count, contextStart + this.mStart, contextCount, x, y, isRtl, p);
        }

        @Override
        public float measureText(int start, int end, Paint p) {
            return p.measureText(this.mChars, this.mStart + start, end - start);
        }

        @Override
        public int getTextWidths(int start, int end, float[] widths, Paint p) {
            return p.getTextWidths(this.mChars, this.mStart + start, end - start, widths);
        }

        @Override
        public float getTextRunAdvances(int start, int end, int contextStart, int contextEnd, boolean isRtl, float[] advances, int advancesIndex, Paint p) {
            int count = end - start;
            int contextCount = contextEnd - contextStart;
            return p.getTextRunAdvances(this.mChars, start + this.mStart, count, contextStart + this.mStart, contextCount, isRtl, advances, advancesIndex);
        }

        @Override
        public int getTextRunCursor(int contextStart, int contextEnd, int dir, int offset, int cursorOpt, Paint p) {
            int contextCount = contextEnd - contextStart;
            return p.getTextRunCursor(this.mChars, contextStart + this.mStart, contextCount, dir, offset + this.mStart, cursorOpt);
        }
    }

    private static final class Marquee {
        private static final int MARQUEE_DELAY = 1200;
        private static final float MARQUEE_DELTA_MAX = 0.07f;
        private static final int MARQUEE_DP_PER_SECOND = 30;
        private static final int MARQUEE_RESTART_DELAY = 1200;
        private static final byte MARQUEE_RUNNING = 2;
        private static final byte MARQUEE_STARTING = 1;
        private static final byte MARQUEE_STOPPED = 0;
        private final Choreographer mChoreographer;
        private float mFadeStop;
        private float mGhostOffset;
        private float mGhostStart;
        private long mLastAnimationMs;
        private float mMaxFadeScroll;
        private float mMaxScroll;
        private final float mPixelsPerSecond;
        private int mRepeatLimit;
        private float mScroll;
        private final WeakReference<TextView> mView;
        private byte mStatus = 0;
        private Choreographer.FrameCallback mTickCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                Marquee.this.tick();
            }
        };
        private Choreographer.FrameCallback mStartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                Marquee.this.mStatus = (byte) 2;
                Marquee.this.mLastAnimationMs = Marquee.this.mChoreographer.getFrameTime();
                Marquee.this.tick();
            }
        };
        private Choreographer.FrameCallback mRestartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (Marquee.this.mStatus == 2) {
                    if (Marquee.this.mRepeatLimit >= 0) {
                        Marquee.access$710(Marquee.this);
                    }
                    Marquee.this.start(Marquee.this.mRepeatLimit);
                }
            }
        };

        static int access$710(Marquee x0) {
            int i = x0.mRepeatLimit;
            x0.mRepeatLimit = i - 1;
            return i;
        }

        Marquee(TextView v) {
            float density = v.getContext().getResources().getDisplayMetrics().density;
            this.mPixelsPerSecond = 30.0f * density;
            this.mView = new WeakReference<>(v);
            this.mChoreographer = Choreographer.getInstance();
        }

        void tick() {
            if (this.mStatus == 2) {
                this.mChoreographer.removeFrameCallback(this.mTickCallback);
                TextView textView = this.mView.get();
                if (textView != null) {
                    if (textView.isFocused() || textView.isSelected()) {
                        long currentMs = this.mChoreographer.getFrameTime();
                        long deltaMs = currentMs - this.mLastAnimationMs;
                        this.mLastAnimationMs = currentMs;
                        float deltaPx = (deltaMs / 1000.0f) * this.mPixelsPerSecond;
                        this.mScroll += deltaPx;
                        if (this.mScroll > this.mMaxScroll) {
                            this.mScroll = this.mMaxScroll;
                            this.mChoreographer.postFrameCallbackDelayed(this.mRestartCallback, 1200L);
                        } else {
                            this.mChoreographer.postFrameCallback(this.mTickCallback);
                        }
                        textView.invalidate();
                    }
                }
            }
        }

        void stop() {
            this.mStatus = (byte) 0;
            this.mChoreographer.removeFrameCallback(this.mStartCallback);
            this.mChoreographer.removeFrameCallback(this.mRestartCallback);
            this.mChoreographer.removeFrameCallback(this.mTickCallback);
            resetScroll();
        }

        private void resetScroll() {
            this.mScroll = 0.0f;
            TextView textView = this.mView.get();
            if (textView != null) {
                textView.invalidate();
            }
        }

        void start(int repeatLimit) {
            if (repeatLimit == 0) {
                stop();
                return;
            }
            this.mRepeatLimit = repeatLimit;
            TextView textView = this.mView.get();
            if (textView != null && textView.mLayout != null) {
                this.mStatus = (byte) 1;
                this.mScroll = 0.0f;
                int textWidth = (textView.getWidth() - textView.getCompoundPaddingLeft()) - textView.getCompoundPaddingRight();
                float lineWidth = textView.mLayout.getLineWidth(0);
                float gap = textWidth / 3.0f;
                this.mGhostStart = (lineWidth - textWidth) + gap;
                this.mMaxScroll = this.mGhostStart + textWidth;
                this.mGhostOffset = lineWidth + gap;
                this.mFadeStop = (textWidth / 6.0f) + lineWidth;
                this.mMaxFadeScroll = this.mGhostStart + lineWidth + lineWidth;
                textView.invalidate();
                this.mChoreographer.postFrameCallback(this.mStartCallback);
            }
        }

        float getGhostOffset() {
            return this.mGhostOffset;
        }

        float getScroll() {
            return this.mScroll;
        }

        float getMaxFadeScroll() {
            return this.mMaxFadeScroll;
        }

        boolean shouldDrawLeftFade() {
            return this.mScroll <= this.mFadeStop;
        }

        boolean shouldDrawGhost() {
            return this.mStatus == 2 && this.mScroll > this.mGhostStart;
        }

        boolean isRunning() {
            return this.mStatus == 2;
        }

        boolean isStopped() {
            return this.mStatus == 0;
        }
    }

    private class ChangeWatcher implements TextWatcher, SpanWatcher {
        private CharSequence mBeforeText;

        private ChangeWatcher() {
        }

        @Override
        public void beforeTextChanged(CharSequence buffer, int start, int before, int after) {
            if (AccessibilityManager.getInstance(TextView.this.mContext).isEnabled() && ((!TextView.isPasswordInputType(TextView.this.getInputType()) && !TextView.this.hasPasswordTransformationMethod()) || TextView.this.shouldSpeakPasswordsForAccessibility())) {
                this.mBeforeText = buffer.toString();
            }
            TextView.this.sendBeforeTextChanged(buffer, start, before, after);
        }

        @Override
        public void onTextChanged(CharSequence buffer, int start, int before, int after) {
            TextView.this.handleTextChanged(buffer, start, before, after);
            if (AccessibilityManager.getInstance(TextView.this.mContext).isEnabled()) {
                if (TextView.this.isFocused() || (TextView.this.isSelected() && TextView.this.isShown())) {
                    TextView.this.sendAccessibilityEventTypeViewTextChanged(this.mBeforeText, start, before, after);
                    this.mBeforeText = null;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable buffer) {
            TextView.this.sendAfterTextChanged(buffer);
            if (MetaKeyKeyListener.getMetaState(buffer, 2048) != 0) {
                MetaKeyKeyListener.stopSelecting(TextView.this, buffer);
            }
        }

        @Override
        public void onSpanChanged(Spannable buf, Object what, int s, int e, int st, int en) {
            TextView.this.spanChange(buf, what, s, st, e, en);
        }

        @Override
        public void onSpanAdded(Spannable buf, Object what, int s, int e) {
            TextView.this.spanChange(buf, what, -1, s, -1, e);
        }

        @Override
        public void onSpanRemoved(Spannable buf, Object what, int s, int e) {
            TextView.this.spanChange(buf, what, s, -1, e, -1);
        }
    }
}
