package android.widget;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.UndoManager;
import android.content.res.ColorStateList;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.BaseCanvas;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.icu.text.DecimalFormatSymbols;
import android.mtp.MtpConstants;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelableParcel;
import android.os.SystemClock;
import android.os.UserHandle;
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
import android.text.PrecomputedText;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
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
import android.util.DisplayMetrics;
import android.util.IntArray;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.AccessibilityIterators;
import android.view.ActionMode;
import android.view.Choreographer;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewHierarchyEncoder;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.ViewStructure;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinks;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.AccessibilityIterators;
import android.widget.Editor;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.FastMath;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.EditableInputConnection;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;

@RemoteViews.RemoteView
public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {
    static final int ACCESSIBILITY_ACTION_PROCESS_TEXT_START_ID = 268435712;
    private static final int ACCESSIBILITY_ACTION_SHARE = 268435456;
    private static final int ANIMATED_SCROLL_GAP = 250;
    public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0;
    public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;
    private static final int CHANGE_WATCHER_PRIORITY = 100;
    static final boolean DEBUG_EXTRACT = false;
    private static final int DECIMAL = 4;
    private static final int DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX = 1;
    private static final int DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP = 112;
    private static final int DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP = 12;
    private static final int DEFAULT_TYPEFACE = -1;
    private static final int DEVICE_PROVISIONED_NO = 1;
    private static final int DEVICE_PROVISIONED_UNKNOWN = 0;
    private static final int DEVICE_PROVISIONED_YES = 2;
    private static final int ELLIPSIZE_END = 3;
    private static final int ELLIPSIZE_MARQUEE = 4;
    private static final int ELLIPSIZE_MIDDLE = 2;
    private static final int ELLIPSIZE_NONE = 0;
    private static final int ELLIPSIZE_NOT_SET = -1;
    private static final int ELLIPSIZE_START = 1;
    private static final int EMS = 1;
    private static final int FLOATING_TOOLBAR_SELECT_ALL_REFRESH_DELAY = 500;
    static final int ID_ASSIST = 16908353;
    static final int ID_AUTOFILL = 16908355;
    static final int ID_COPY = 16908321;
    static final int ID_CUT = 16908320;
    static final int ID_PASTE = 16908322;
    static final int ID_PASTE_AS_PLAIN_TEXT = 16908337;
    static final int ID_REDO = 16908339;
    static final int ID_REPLACE = 16908340;
    static final int ID_SELECT_ALL = 16908319;
    static final int ID_SHARE = 16908341;
    static final int ID_UNDO = 16908338;
    private static final int KEY_DOWN_HANDLED_BY_KEY_LISTENER = 1;
    private static final int KEY_DOWN_HANDLED_BY_MOVEMENT_METHOD = 2;
    private static final int KEY_EVENT_HANDLED = -1;
    private static final int KEY_EVENT_NOT_HANDLED = 0;
    private static final int LINES = 1;
    static final String LOG_TAG = "TextView";
    private static final int MARQUEE_FADE_NORMAL = 0;
    private static final int MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS = 1;
    private static final int MARQUEE_FADE_SWITCH_SHOW_FADE = 2;
    private static final int MONOSPACE = 3;
    private static final int PIXELS = 2;
    static final int PROCESS_TEXT_REQUEST_CODE = 100;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int SIGNED = 2;

    @VisibleForTesting
    public static final BoringLayout.Metrics UNKNOWN_BORING;
    private static final float UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE = -1.0f;
    static final int VERY_WIDE = 1048576;
    static long sLastCutCopyOrTextChangedTime;
    private boolean mAllowTransformationLengthChange;
    private int mAutoLinkMask;
    private float mAutoSizeMaxTextSizeInPx;
    private float mAutoSizeMinTextSizeInPx;
    private float mAutoSizeStepGranularityInPx;
    private int[] mAutoSizeTextSizesInPx;
    private int mAutoSizeTextType;
    private BoringLayout.Metrics mBoring;
    private int mBreakStrategy;
    private BufferType mBufferType;
    private ChangeWatcher mChangeWatcher;
    private CharWrapper mCharWrapper;
    private int mCurHintTextColor;

    @ViewDebug.ExportedProperty(category = "text")
    private int mCurTextColor;
    private volatile Locale mCurrentSpellCheckerLocaleCache;
    int mCursorDrawableRes;
    private int mDeferScroll;
    private int mDesiredHeightAtMeasure;
    private int mDeviceProvisionedState;
    Drawables mDrawables;
    private Editable.Factory mEditableFactory;
    private Editor mEditor;
    private TextUtils.TruncateAt mEllipsize;
    private InputFilter[] mFilters;
    private boolean mFreezesText;

    @ViewDebug.ExportedProperty(category = "text")
    private int mGravity;
    private boolean mHasPresetAutoSizeValues;
    int mHighlightColor;
    private final Paint mHighlightPaint;
    private Path mHighlightPath;
    private boolean mHighlightPathBogus;
    private CharSequence mHint;
    private BoringLayout.Metrics mHintBoring;
    private Layout mHintLayout;
    private ColorStateList mHintTextColor;
    private boolean mHorizontallyScrolling;
    private int mHyphenationFrequency;
    private boolean mIncludePad;
    private boolean mIsRestrictedAcrossUser;
    private int mJustificationMode;
    private int mLastLayoutDirection;
    private long mLastScroll;
    private CharSequence mLastValueSentToAutofillManager;
    private Layout mLayout;
    private ColorStateList mLinkTextColor;
    private boolean mLinksClickable;
    private boolean mListenerChanged;
    private ArrayList<TextWatcher> mListeners;
    private boolean mLocalesChanged;
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
    private boolean mNeedsAutoSizeText;
    private int mOldMaxMode;
    private int mOldMaximum;
    private boolean mPreDrawListenerDetached;
    private boolean mPreDrawRegistered;
    private PrecomputedText mPrecomputed;
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
    private Spannable mSpannable;
    private Spannable.Factory mSpannableFactory;
    private Rect mTempRect;
    private TextPaint mTempTextPaint;

    @ViewDebug.ExportedProperty(category = "text")
    private CharSequence mText;
    private TextClassifier mTextClassificationSession;
    private TextClassifier mTextClassifier;
    private ColorStateList mTextColor;
    private TextDirectionHeuristic mTextDir;
    int mTextEditSuggestionContainerLayout;
    int mTextEditSuggestionHighlightStyle;
    int mTextEditSuggestionItemLayout;
    private int mTextId;
    private final TextPaint mTextPaint;
    int mTextSelectHandleLeftRes;
    int mTextSelectHandleRes;
    int mTextSelectHandleRightRes;
    private boolean mTextSetFromXmlOrResourceId;
    private boolean mTextViewDiscardNextActionUp;
    private TransformationMethod mTransformation;
    private CharSequence mTransformed;
    boolean mUseFallbackLineSpacing;
    private final boolean mUseInternationalizedInput;
    private boolean mUserSetTextScaleX;
    private static final float[] TEMP_POSITION = new float[2];
    private static final RectF TEMP_RECTF = new RectF();
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");
    private static final int[] MULTILINE_STATE_SET = {16843597};
    private static final SparseIntArray sAppearanceValues = new SparseIntArray();

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutoSizeTextType {
    }

    public enum BufferType {
        NORMAL,
        SPANNABLE,
        EDITABLE
    }

    public interface OnEditorActionListener {
        boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface XMLTypefaceAttr {
    }

    static {
        sAppearanceValues.put(6, 4);
        sAppearanceValues.put(5, 3);
        sAppearanceValues.put(7, 5);
        sAppearanceValues.put(8, 6);
        sAppearanceValues.put(2, 0);
        sAppearanceValues.put(3, 1);
        sAppearanceValues.put(75, 12);
        sAppearanceValues.put(4, 2);
        sAppearanceValues.put(94, 17);
        sAppearanceValues.put(72, 11);
        sAppearanceValues.put(36, 7);
        sAppearanceValues.put(37, 8);
        sAppearanceValues.put(38, 9);
        sAppearanceValues.put(39, 10);
        sAppearanceValues.put(76, 13);
        sAppearanceValues.put(90, 16);
        sAppearanceValues.put(77, 14);
        sAppearanceValues.put(78, 15);
        UNKNOWN_BORING = new BoringLayout.Metrics();
    }

    static class Drawables {
        static final int BOTTOM = 3;
        static final int DRAWABLE_LEFT = 1;
        static final int DRAWABLE_NONE = -1;
        static final int DRAWABLE_RIGHT = 0;
        static final int LEFT = 0;
        static final int RIGHT = 2;
        static final int TOP = 1;
        Drawable mDrawableEnd;
        Drawable mDrawableError;
        int mDrawableHeightEnd;
        int mDrawableHeightError;
        int mDrawableHeightLeft;
        int mDrawableHeightRight;
        int mDrawableHeightStart;
        int mDrawableHeightTemp;
        Drawable mDrawableLeftInitial;
        int mDrawablePadding;
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
        int mDrawableWidthBottom;
        int mDrawableWidthTop;
        boolean mHasTint;
        boolean mHasTintMode;
        boolean mIsRtlCompatibilityMode;
        boolean mOverride;
        ColorStateList mTintList;
        PorterDuff.Mode mTintMode;
        final Rect mCompoundRect = new Rect();
        final Drawable[] mShowing = new Drawable[4];
        int mDrawableSaved = -1;

        public Drawables(Context context) {
            this.mIsRtlCompatibilityMode = context.getApplicationInfo().targetSdkVersion < 17 || !context.getApplicationInfo().hasRtlSupport();
            this.mOverride = false;
        }

        public boolean hasMetadata() {
            return this.mDrawablePadding != 0 || this.mHasTintMode || this.mHasTint;
        }

        public boolean resolveWithLayoutDirection(int i) {
            Drawable drawable = this.mShowing[0];
            Drawable drawable2 = this.mShowing[2];
            this.mShowing[0] = this.mDrawableLeftInitial;
            this.mShowing[2] = this.mDrawableRightInitial;
            if (this.mIsRtlCompatibilityMode) {
                if (this.mDrawableStart != null && this.mShowing[0] == null) {
                    this.mShowing[0] = this.mDrawableStart;
                    this.mDrawableSizeLeft = this.mDrawableSizeStart;
                    this.mDrawableHeightLeft = this.mDrawableHeightStart;
                }
                if (this.mDrawableEnd != null && this.mShowing[2] == null) {
                    this.mShowing[2] = this.mDrawableEnd;
                    this.mDrawableSizeRight = this.mDrawableSizeEnd;
                    this.mDrawableHeightRight = this.mDrawableHeightEnd;
                }
            } else if (i == 1) {
                if (this.mOverride) {
                    this.mShowing[2] = this.mDrawableStart;
                    this.mDrawableSizeRight = this.mDrawableSizeStart;
                    this.mDrawableHeightRight = this.mDrawableHeightStart;
                    this.mShowing[0] = this.mDrawableEnd;
                    this.mDrawableSizeLeft = this.mDrawableSizeEnd;
                    this.mDrawableHeightLeft = this.mDrawableHeightEnd;
                }
            } else if (this.mOverride) {
                this.mShowing[0] = this.mDrawableStart;
                this.mDrawableSizeLeft = this.mDrawableSizeStart;
                this.mDrawableHeightLeft = this.mDrawableHeightStart;
                this.mShowing[2] = this.mDrawableEnd;
                this.mDrawableSizeRight = this.mDrawableSizeEnd;
                this.mDrawableHeightRight = this.mDrawableHeightEnd;
            }
            applyErrorDrawableIfNeeded(i);
            return (this.mShowing[0] == drawable && this.mShowing[2] == drawable2) ? false : true;
        }

        public void setErrorDrawable(Drawable drawable, TextView textView) {
            if (this.mDrawableError != drawable && this.mDrawableError != null) {
                this.mDrawableError.setCallback(null);
            }
            this.mDrawableError = drawable;
            if (this.mDrawableError != null) {
                Rect rect = this.mCompoundRect;
                this.mDrawableError.setState(textView.getDrawableState());
                this.mDrawableError.copyBounds(rect);
                this.mDrawableError.setCallback(textView);
                this.mDrawableSizeError = rect.width();
                this.mDrawableHeightError = rect.height();
                return;
            }
            this.mDrawableHeightError = 0;
            this.mDrawableSizeError = 0;
        }

        private void applyErrorDrawableIfNeeded(int i) {
            switch (this.mDrawableSaved) {
                case 0:
                    this.mShowing[2] = this.mDrawableTemp;
                    this.mDrawableSizeRight = this.mDrawableSizeTemp;
                    this.mDrawableHeightRight = this.mDrawableHeightTemp;
                    break;
                case 1:
                    this.mShowing[0] = this.mDrawableTemp;
                    this.mDrawableSizeLeft = this.mDrawableSizeTemp;
                    this.mDrawableHeightLeft = this.mDrawableHeightTemp;
                    break;
            }
            if (this.mDrawableError != null) {
                if (i == 1) {
                    this.mDrawableSaved = 1;
                    this.mDrawableTemp = this.mShowing[0];
                    this.mDrawableSizeTemp = this.mDrawableSizeLeft;
                    this.mDrawableHeightTemp = this.mDrawableHeightLeft;
                    this.mShowing[0] = this.mDrawableError;
                    this.mDrawableSizeLeft = this.mDrawableSizeError;
                    this.mDrawableHeightLeft = this.mDrawableHeightError;
                    return;
                }
                this.mDrawableSaved = 0;
                this.mDrawableTemp = this.mShowing[2];
                this.mDrawableSizeTemp = this.mDrawableSizeRight;
                this.mDrawableHeightTemp = this.mDrawableHeightRight;
                this.mShowing[2] = this.mDrawableError;
                this.mDrawableSizeRight = this.mDrawableSizeError;
                this.mDrawableHeightRight = this.mDrawableHeightError;
            }
        }
    }

    public static void preloadFontCache() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.DEFAULT);
        paint.measureText("H");
    }

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842884);
    }

    public TextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TextView(Context context, AttributeSet attributeSet, int i, int i2) {
        boolean z;
        BufferType bufferType;
        ColorStateList colorStateList;
        Context context2;
        boolean z2;
        int i3;
        boolean z3;
        TextAppearanceAttributes textAppearanceAttributes;
        int i4;
        CharSequence charSequence;
        int focusable;
        int indexCount;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        TextKeyListener.Capitalize capitalize;
        int i11;
        int i12;
        TextAppearanceAttributes textAppearanceAttributes2;
        int i13;
        int i14;
        boolean z4;
        boolean z5;
        int i15;
        boolean z6;
        int i16;
        boolean z7;
        boolean z8;
        int i17;
        PorterDuff.Mode mode;
        int i18;
        super(context, attributeSet, i, i2);
        this.mEditableFactory = Editable.Factory.getInstance();
        this.mSpannableFactory = Spannable.Factory.getInstance();
        this.mMarqueeRepeatLimit = 3;
        int i19 = -1;
        this.mLastLayoutDirection = -1;
        ?? r8 = 0;
        this.mMarqueeFadeMode = 0;
        this.mBufferType = BufferType.NORMAL;
        this.mLocalesChanged = false;
        this.mListenerChanged = false;
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
        this.mDeviceProvisionedState = 0;
        this.mAutoSizeTextType = 0;
        this.mNeedsAutoSizeText = false;
        float f = -1.0f;
        this.mAutoSizeStepGranularityInPx = -1.0f;
        this.mAutoSizeMinTextSizeInPx = -1.0f;
        this.mAutoSizeMaxTextSizeInPx = -1.0f;
        this.mAutoSizeTextSizesInPx = EmptyArray.INT;
        this.mHasPresetAutoSizeValues = false;
        this.mTextSetFromXmlOrResourceId = false;
        this.mTextId = 0;
        this.mTextViewDiscardNextActionUp = false;
        if (getImportantForAutofill() == 0) {
            setImportantForAutofill(1);
        }
        setTextInternal("");
        Resources resources = getResources();
        CompatibilityInfo compatibilityInfo = resources.getCompatibilityInfo();
        this.mTextPaint = new TextPaint(1);
        this.mTextPaint.density = resources.getDisplayMetrics().density;
        this.mTextPaint.setCompatibilityScaling(compatibilityInfo.applicationScale);
        this.mHighlightPaint = new Paint(1);
        this.mHighlightPaint.setCompatibilityScaling(compatibilityInfo.applicationScale);
        this.mMovement = getDefaultMovementMethod();
        this.mTransformation = null;
        TextAppearanceAttributes textAppearanceAttributes3 = new TextAppearanceAttributes();
        textAppearanceAttributes3.mTextColor = ColorStateList.valueOf(-16777216);
        textAppearanceAttributes3.mTextSize = 15;
        this.mBreakStrategy = 0;
        this.mHyphenationFrequency = 0;
        this.mJustificationMode = 0;
        Resources.Theme theme = context.getTheme();
        TypedArray typedArrayObtainStyledAttributes = theme.obtainStyledAttributes(attributeSet, R.styleable.TextViewAppearance, i, i2);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, -1);
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = resourceId != -1 ? theme.obtainStyledAttributes(resourceId, R.styleable.TextAppearance) : null;
        if (typedArrayObtainStyledAttributes2 != null) {
            readTextAppearance(context, typedArrayObtainStyledAttributes2, textAppearanceAttributes3, false);
            textAppearanceAttributes3.mFontFamilyExplicit = false;
            typedArrayObtainStyledAttributes2.recycle();
        }
        boolean defaultEditable = getDefaultEditable();
        TypedArray typedArrayObtainStyledAttributes3 = theme.obtainStyledAttributes(attributeSet, R.styleable.TextView, i, i2);
        readTextAppearance(context, typedArrayObtainStyledAttributes3, textAppearanceAttributes3, true);
        int indexCount2 = typedArrayObtainStyledAttributes3.getIndexCount();
        int i20 = -1;
        int i21 = -1;
        int i22 = -1;
        int dimensionPixelSize = -1;
        int dimensionPixelSize2 = -1;
        int dimensionPixelSize3 = -1;
        boolean z9 = false;
        int i23 = 0;
        boolean z10 = false;
        boolean z11 = false;
        int i24 = 0;
        boolean z12 = false;
        boolean z13 = false;
        int dimensionPixelSize4 = 0;
        boolean z14 = false;
        boolean z15 = defaultEditable;
        float fApplyDimension = -1.0f;
        float dimension = -1.0f;
        float dimension2 = -1.0f;
        CharSequence text = "";
        PorterDuff.Mode tintMode = null;
        CharSequence text2 = null;
        CharSequence text3 = null;
        Drawable drawable = null;
        Drawable drawable2 = null;
        Drawable drawable3 = null;
        Drawable drawable4 = null;
        Drawable drawable5 = null;
        Drawable drawable6 = null;
        ColorStateList colorStateList2 = null;
        CharSequence text4 = null;
        int i25 = 0;
        int inputType = 0;
        while (i25 < indexCount2) {
            int index = typedArrayObtainStyledAttributes3.getIndex(i25);
            if (index == 0) {
                i12 = indexCount2;
                textAppearanceAttributes2 = textAppearanceAttributes3;
                i13 = i20;
                i14 = i23;
                z4 = z10;
                z5 = z11;
                i15 = i24;
                z6 = z12;
                i16 = i21;
                z7 = z13;
                z8 = z15;
                i17 = dimensionPixelSize4;
                mode = tintMode;
                setEnabled(typedArrayObtainStyledAttributes3.getBoolean(index, isEnabled()));
            } else if (index != 67) {
                switch (index) {
                    case 9:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i20 = typedArrayObtainStyledAttributes3.getInt(index, i20);
                        break;
                    case 10:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i14 = i23;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        setGravity(typedArrayObtainStyledAttributes3.getInt(index, -1));
                        i13 = i20;
                        break;
                    case 11:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i14 = i23;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        this.mAutoLinkMask = typedArrayObtainStyledAttributes3.getInt(index, 0);
                        i13 = i20;
                        break;
                    case 12:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i14 = i23;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        this.mLinksClickable = typedArrayObtainStyledAttributes3.getBoolean(index, true);
                        i13 = i20;
                        break;
                    case 13:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        int i26 = i19;
                        i14 = i23;
                        setMaxWidth(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i26));
                        i13 = i20;
                        break;
                    case 14:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        int i27 = i19;
                        i14 = i23;
                        setMaxHeight(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i27));
                        i13 = i20;
                        break;
                    case 15:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        int i28 = i19;
                        i14 = i23;
                        setMinWidth(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i28));
                        i13 = i20;
                        break;
                    case 16:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i14 = i23;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i17 = dimensionPixelSize4;
                        mode = tintMode;
                        setMinHeight(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, -1));
                        i13 = i20;
                        break;
                    case 17:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i23 = typedArrayObtainStyledAttributes3.getInt(index, i23);
                        break;
                    case 18:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        this.mTextId = typedArrayObtainStyledAttributes3.getResourceId(index, 0);
                        text = typedArrayObtainStyledAttributes3.getText(index);
                        dimensionPixelSize4 = dimensionPixelSize4;
                        z14 = true;
                        break;
                    case 19:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        text4 = typedArrayObtainStyledAttributes3.getText(index);
                        break;
                    case 20:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i18 = dimensionPixelSize4;
                        mode = tintMode;
                        setTextScaleX(typedArrayObtainStyledAttributes3.getFloat(index, 1.0f));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 21:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i18 = dimensionPixelSize4;
                        mode = tintMode;
                        if (!typedArrayObtainStyledAttributes3.getBoolean(index, true)) {
                            setCursorVisible(false);
                        }
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 22:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i29 = i19;
                        i18 = dimensionPixelSize4;
                        setMaxLines(typedArrayObtainStyledAttributes3.getInt(index, i29));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 23:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i30 = i19;
                        i18 = dimensionPixelSize4;
                        setLines(typedArrayObtainStyledAttributes3.getInt(index, i30));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 24:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i31 = i19;
                        i18 = dimensionPixelSize4;
                        setHeight(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i31));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 25:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i32 = i19;
                        i18 = dimensionPixelSize4;
                        setMinLines(typedArrayObtainStyledAttributes3.getInt(index, i32));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 26:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i33 = i19;
                        i18 = dimensionPixelSize4;
                        setMaxEms(typedArrayObtainStyledAttributes3.getInt(index, i33));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 27:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i34 = i19;
                        i18 = dimensionPixelSize4;
                        setEms(typedArrayObtainStyledAttributes3.getInt(index, i34));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 28:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        mode = tintMode;
                        int i35 = i19;
                        i18 = dimensionPixelSize4;
                        setWidth(typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i35));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 29:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z4 = z10;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i18 = dimensionPixelSize4;
                        mode = tintMode;
                        setMinEms(typedArrayObtainStyledAttributes3.getInt(index, -1));
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 30:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z5 = z11;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        i18 = dimensionPixelSize4;
                        mode = tintMode;
                        z4 = z10;
                        if (typedArrayObtainStyledAttributes3.getBoolean(index, false)) {
                            setHorizontallyScrolling(true);
                        }
                        i17 = i18;
                        i13 = i20;
                        i14 = i23;
                        break;
                    case 31:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z9 = typedArrayObtainStyledAttributes3.getBoolean(index, z9);
                        break;
                    case 32:
                        i12 = indexCount2;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        z10 = typedArrayObtainStyledAttributes3.getBoolean(index, z10);
                        break;
                    case 33:
                        i12 = indexCount2;
                        z11 = typedArrayObtainStyledAttributes3.getBoolean(index, z11);
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        break;
                    case 34:
                        i12 = indexCount2;
                        i15 = i24;
                        z6 = z12;
                        i16 = i21;
                        z7 = z13;
                        z8 = z15;
                        int i36 = dimensionPixelSize4;
                        mode = tintMode;
                        if (!typedArrayObtainStyledAttributes3.getBoolean(index, true)) {
                            setIncludeFontPadding(false);
                        }
                        i17 = i36;
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        i13 = i20;
                        i14 = i23;
                        z4 = z10;
                        z5 = z11;
                        break;
                    case 35:
                        i12 = indexCount2;
                        i22 = typedArrayObtainStyledAttributes3.getInt(index, -1);
                        textAppearanceAttributes2 = textAppearanceAttributes3;
                        break;
                    default:
                        switch (index) {
                            case 40:
                                i12 = indexCount2;
                                i24 = typedArrayObtainStyledAttributes3.getInt(index, i24);
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 41:
                                i12 = indexCount2;
                                text3 = typedArrayObtainStyledAttributes3.getText(index);
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 42:
                                i12 = indexCount2;
                                z12 = typedArrayObtainStyledAttributes3.getBoolean(index, z12);
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 43:
                                i12 = indexCount2;
                                text2 = typedArrayObtainStyledAttributes3.getText(index);
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 44:
                                i12 = indexCount2;
                                i21 = typedArrayObtainStyledAttributes3.getInt(index, i21);
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 45:
                                z13 = typedArrayObtainStyledAttributes3.getBoolean(index, z13);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 46:
                                z15 = typedArrayObtainStyledAttributes3.getBoolean(index, z15);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 47:
                                mode = tintMode;
                                this.mFreezesText = typedArrayObtainStyledAttributes3.getBoolean(index, r8);
                                i12 = indexCount2;
                                i17 = dimensionPixelSize4;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                break;
                            case 48:
                                drawable2 = typedArrayObtainStyledAttributes3.getDrawable(index);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 49:
                                drawable4 = typedArrayObtainStyledAttributes3.getDrawable(index);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 50:
                                drawable = typedArrayObtainStyledAttributes3.getDrawable(index);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 51:
                                drawable3 = typedArrayObtainStyledAttributes3.getDrawable(index);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 52:
                                dimensionPixelSize4 = typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, dimensionPixelSize4);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 53:
                                mode = tintMode;
                                this.mSpacingAdd = typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, (int) this.mSpacingAdd);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 54:
                                mode = tintMode;
                                this.mSpacingMult = typedArrayObtainStyledAttributes3.getFloat(index, this.mSpacingMult);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 55:
                                mode = tintMode;
                                setMarqueeRepeatLimit(typedArrayObtainStyledAttributes3.getInt(index, this.mMarqueeRepeatLimit));
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 56:
                                inputType = typedArrayObtainStyledAttributes3.getInt(index, r8);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                break;
                            case 57:
                                mode = tintMode;
                                setPrivateImeOptions(typedArrayObtainStyledAttributes3.getString(index));
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 58:
                                mode = tintMode;
                                try {
                                    setInputExtras(typedArrayObtainStyledAttributes3.getResourceId(index, r8));
                                } catch (IOException e) {
                                    Log.w(LOG_TAG, "Failure reading input extras", e);
                                } catch (XmlPullParserException e2) {
                                    Log.w(LOG_TAG, "Failure reading input extras", e2);
                                }
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 59:
                                mode = tintMode;
                                createEditorIfNeeded();
                                this.mEditor.createInputContentTypeIfNeeded();
                                this.mEditor.mInputContentType.imeOptions = typedArrayObtainStyledAttributes3.getInt(index, this.mEditor.mInputContentType.imeOptions);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 60:
                                mode = tintMode;
                                createEditorIfNeeded();
                                this.mEditor.createInputContentTypeIfNeeded();
                                this.mEditor.mInputContentType.imeActionLabel = typedArrayObtainStyledAttributes3.getText(index);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 61:
                                mode = tintMode;
                                createEditorIfNeeded();
                                this.mEditor.createInputContentTypeIfNeeded();
                                this.mEditor.mInputContentType.imeActionId = typedArrayObtainStyledAttributes3.getInt(index, this.mEditor.mInputContentType.imeActionId);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 62:
                                mode = tintMode;
                                this.mTextSelectHandleLeftRes = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 63:
                                mode = tintMode;
                                this.mTextSelectHandleRightRes = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            case 64:
                                mode = tintMode;
                                this.mTextSelectHandleRes = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                i12 = indexCount2;
                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                i13 = i20;
                                i14 = i23;
                                z4 = z10;
                                z5 = z11;
                                i15 = i24;
                                z6 = z12;
                                i16 = i21;
                                z7 = z13;
                                z8 = z15;
                                i17 = dimensionPixelSize4;
                                break;
                            default:
                                switch (index) {
                                    case 70:
                                        mode = tintMode;
                                        this.mCursorDrawableRes = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                        i12 = indexCount2;
                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                        i13 = i20;
                                        i14 = i23;
                                        z4 = z10;
                                        z5 = z11;
                                        i15 = i24;
                                        z6 = z12;
                                        i16 = i21;
                                        z7 = z13;
                                        z8 = z15;
                                        i17 = dimensionPixelSize4;
                                        break;
                                    case 71:
                                        mode = tintMode;
                                        this.mTextEditSuggestionItemLayout = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                        i12 = indexCount2;
                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                        i13 = i20;
                                        i14 = i23;
                                        z4 = z10;
                                        z5 = z11;
                                        i15 = i24;
                                        z6 = z12;
                                        i16 = i21;
                                        z7 = z13;
                                        z8 = z15;
                                        i17 = dimensionPixelSize4;
                                        break;
                                    default:
                                        switch (index) {
                                            case 73:
                                                drawable5 = typedArrayObtainStyledAttributes3.getDrawable(index);
                                                i12 = indexCount2;
                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                break;
                                            case 74:
                                                drawable6 = typedArrayObtainStyledAttributes3.getDrawable(index);
                                                i12 = indexCount2;
                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                break;
                                            default:
                                                switch (index) {
                                                    case 79:
                                                        colorStateList2 = typedArrayObtainStyledAttributes3.getColorStateList(index);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        break;
                                                    case 80:
                                                        tintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes3.getInt(index, i19), tintMode);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        break;
                                                    case 81:
                                                        this.mBreakStrategy = typedArrayObtainStyledAttributes3.getInt(index, r8);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    case 82:
                                                        this.mHyphenationFrequency = typedArrayObtainStyledAttributes3.getInt(index, r8);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    case 83:
                                                        createEditorIfNeeded();
                                                        this.mEditor.mAllowUndo = typedArrayObtainStyledAttributes3.getBoolean(index, true);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    case 84:
                                                        this.mAutoSizeTextType = typedArrayObtainStyledAttributes3.getInt(index, r8);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    case 85:
                                                        dimension2 = typedArrayObtainStyledAttributes3.getDimension(index, f);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        break;
                                                    case 86:
                                                        int resourceId2 = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                                        if (resourceId2 > 0) {
                                                            TypedArray typedArrayObtainTypedArray = typedArrayObtainStyledAttributes3.getResources().obtainTypedArray(resourceId2);
                                                            setupAutoSizeUniformPresetSizes(typedArrayObtainTypedArray);
                                                            typedArrayObtainTypedArray.recycle();
                                                        }
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    case 87:
                                                        fApplyDimension = typedArrayObtainStyledAttributes3.getDimension(index, f);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        break;
                                                    case 88:
                                                        dimension = typedArrayObtainStyledAttributes3.getDimension(index, f);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        break;
                                                    case 89:
                                                        this.mJustificationMode = typedArrayObtainStyledAttributes3.getInt(index, r8);
                                                        i12 = indexCount2;
                                                        textAppearanceAttributes2 = textAppearanceAttributes3;
                                                        i13 = i20;
                                                        i14 = i23;
                                                        z4 = z10;
                                                        z5 = z11;
                                                        i15 = i24;
                                                        z6 = z12;
                                                        i16 = i21;
                                                        z7 = z13;
                                                        z8 = z15;
                                                        i17 = dimensionPixelSize4;
                                                        mode = tintMode;
                                                        break;
                                                    default:
                                                        switch (index) {
                                                            case 91:
                                                                dimensionPixelSize = typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i19);
                                                                i12 = indexCount2;
                                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                                break;
                                                            case 92:
                                                                dimensionPixelSize2 = typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i19);
                                                                i12 = indexCount2;
                                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                                break;
                                                            case 93:
                                                                dimensionPixelSize3 = typedArrayObtainStyledAttributes3.getDimensionPixelSize(index, i19);
                                                                i12 = indexCount2;
                                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                                break;
                                                            default:
                                                                switch (index) {
                                                                    case 95:
                                                                        this.mTextEditSuggestionContainerLayout = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                                                        break;
                                                                    case 96:
                                                                        this.mTextEditSuggestionHighlightStyle = typedArrayObtainStyledAttributes3.getResourceId(index, r8);
                                                                        break;
                                                                }
                                                                i12 = indexCount2;
                                                                textAppearanceAttributes2 = textAppearanceAttributes3;
                                                                i13 = i20;
                                                                i14 = i23;
                                                                z4 = z10;
                                                                z5 = z11;
                                                                i15 = i24;
                                                                z6 = z12;
                                                                i16 = i21;
                                                                z7 = z13;
                                                                z8 = z15;
                                                                i17 = dimensionPixelSize4;
                                                                mode = tintMode;
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                i25++;
                indexCount2 = i12;
                textAppearanceAttributes3 = textAppearanceAttributes2;
                i19 = -1;
                r8 = 0;
                f = -1.0f;
            } else {
                i12 = indexCount2;
                textAppearanceAttributes2 = textAppearanceAttributes3;
                i14 = i23;
                z4 = z10;
                z5 = z11;
                i15 = i24;
                z6 = z12;
                i16 = i21;
                z7 = z13;
                z8 = z15;
                i17 = dimensionPixelSize4;
                mode = tintMode;
                i13 = i20;
                setTextIsSelectable(typedArrayObtainStyledAttributes3.getBoolean(index, false));
            }
            z12 = z6;
            i24 = i15;
            z11 = z5;
            i21 = i16;
            i23 = i14;
            z13 = z7;
            tintMode = mode;
            z15 = z8;
            z10 = z4;
            dimensionPixelSize4 = i17;
            i20 = i13;
            i25++;
            indexCount2 = i12;
            textAppearanceAttributes3 = textAppearanceAttributes2;
            i19 = -1;
            r8 = 0;
            f = -1.0f;
        }
        TextAppearanceAttributes textAppearanceAttributes4 = textAppearanceAttributes3;
        int i37 = i20;
        int i38 = i23;
        boolean z16 = z10;
        boolean z17 = z11;
        int i39 = i24;
        boolean z18 = z12;
        int i40 = i21;
        boolean z19 = z13;
        boolean z20 = z15;
        int i41 = dimensionPixelSize4;
        PorterDuff.Mode mode2 = tintMode;
        typedArrayObtainStyledAttributes3.recycle();
        BufferType bufferType2 = BufferType.EDITABLE;
        int i42 = inputType & FileObserver.ALL_EVENTS;
        boolean z21 = i42 == 129;
        boolean z22 = i42 == 225;
        boolean z23 = i42 == 18;
        int i43 = context.getApplicationInfo().targetSdkVersion;
        this.mUseInternationalizedInput = i43 >= 26;
        this.mUseFallbackLineSpacing = i43 >= 28;
        CharSequence charSequence2 = text2;
        if (charSequence2 != null) {
            try {
                Class<?> cls = Class.forName(charSequence2.toString());
                try {
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = (KeyListener) cls.newInstance();
                    try {
                        Editor editor = this.mEditor;
                        if (inputType == 0) {
                            inputType = this.mEditor.mKeyListener.getInputType();
                        }
                        editor.mInputType = inputType;
                    } catch (IncompatibleClassChangeError e3) {
                        this.mEditor.mInputType = 1;
                    }
                } catch (IllegalAccessException e4) {
                    throw new RuntimeException(e4);
                } catch (InstantiationException e5) {
                    throw new RuntimeException(e5);
                }
            } catch (ClassNotFoundException e6) {
                throw new RuntimeException(e6);
            }
        } else {
            CharSequence charSequence3 = text3;
            if (charSequence3 != null) {
                createEditorIfNeeded();
                this.mEditor.mKeyListener = DigitsKeyListener.getInstance(charSequence3.toString());
                this.mEditor.mInputType = inputType == 0 ? 1 : inputType;
            } else {
                if (inputType != 0) {
                    setInputType(inputType, true);
                    z = !isMultilineInputType(inputType);
                    bufferType = bufferType2;
                    if (this.mEditor != null) {
                        this.mEditor.adjustInputType(z9, z21, z22, z23);
                    }
                    if (z17) {
                        createEditorIfNeeded();
                        this.mEditor.mSelectAllOnFocus = true;
                        if (bufferType == BufferType.NORMAL) {
                            bufferType = BufferType.SPANNABLE;
                        }
                    }
                    colorStateList = colorStateList2;
                    if (colorStateList == null || mode2 != null) {
                        if (this.mDrawables != null) {
                            context2 = context;
                            this.mDrawables = new Drawables(context2);
                        } else {
                            context2 = context;
                        }
                        if (colorStateList == null) {
                            this.mDrawables.mTintList = colorStateList;
                            z2 = true;
                            this.mDrawables.mHasTint = true;
                        } else {
                            z2 = true;
                        }
                        if (mode2 != null) {
                            this.mDrawables.mTintMode = mode2;
                            this.mDrawables.mHasTintMode = z2;
                        }
                    } else {
                        context2 = context;
                    }
                    setCompoundDrawablesWithIntrinsicBounds(drawable, drawable2, drawable3, drawable4);
                    setRelativeDrawablesIfNeeded(drawable5, drawable6);
                    setCompoundDrawablePadding(i41);
                    setInputTypeSingleLine(z);
                    applySingleLine(z, z, z);
                    if (z || getKeyListener() != null) {
                        i3 = i37;
                    } else {
                        i3 = i37;
                        if (i3 == -1) {
                            i3 = 3;
                        }
                    }
                    switch (i3) {
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
                    z3 = !z9 || z21 || z22 || z23;
                    if (!z3 || (this.mEditor != null && (this.mEditor.mInputType & FileObserver.ALL_EVENTS) == 129)) {
                        textAppearanceAttributes = textAppearanceAttributes4;
                    } else {
                        textAppearanceAttributes = textAppearanceAttributes4;
                        textAppearanceAttributes.mTypefaceIndex = 3;
                    }
                    applyTextAppearance(textAppearanceAttributes);
                    if (z3) {
                        setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                    i4 = i22;
                    if (i4 < 0) {
                        setFilters(new InputFilter[]{new InputFilter.LengthFilter(i4)});
                    } else {
                        setFilters(NO_FILTERS);
                    }
                    setText(text, bufferType);
                    if (z14) {
                        this.mTextSetFromXmlOrResourceId = true;
                    }
                    charSequence = text4;
                    if (charSequence != null) {
                        setHint(charSequence);
                    }
                    TypedArray typedArrayObtainStyledAttributes4 = context2.obtainStyledAttributes(attributeSet, R.styleable.View, i, i2);
                    boolean z24 = this.mMovement == null || getKeyListener() != null;
                    boolean z25 = !z24 || isClickable();
                    boolean z26 = !z24 || isLongClickable();
                    focusable = getFocusable();
                    indexCount = typedArrayObtainStyledAttributes4.getIndexCount();
                    boolean z27 = z26;
                    for (i5 = 0; i5 < indexCount; i5++) {
                        int index2 = typedArrayObtainStyledAttributes4.getIndex(i5);
                        if (index2 != 19) {
                            switch (index2) {
                                case 30:
                                    z25 = typedArrayObtainStyledAttributes4.getBoolean(index2, z25);
                                    break;
                                case 31:
                                    z27 = typedArrayObtainStyledAttributes4.getBoolean(index2, z27);
                                    break;
                            }
                        } else {
                            TypedValue typedValue = new TypedValue();
                            focusable = typedArrayObtainStyledAttributes4.getValue(index2, typedValue) ? typedValue.type == 18 ? typedValue.data == 0 ? 0 : 1 : typedValue.data : focusable;
                        }
                    }
                    typedArrayObtainStyledAttributes4.recycle();
                    if (focusable != getFocusable()) {
                        setFocusable(focusable);
                    }
                    setClickable(z25);
                    setLongClickable(z27);
                    if (this.mEditor != null) {
                        this.mEditor.prepareCursorControllers();
                    }
                    if (getImportantForAccessibility() != 0) {
                        i6 = 1;
                        setImportantForAccessibility(1);
                    } else {
                        i6 = 1;
                    }
                    if (supportsAutoSizeText()) {
                        this.mAutoSizeTextType = 0;
                    } else if (this.mAutoSizeTextType == i6) {
                        if (!this.mHasPresetAutoSizeValues) {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            if (fApplyDimension == -1.0f) {
                                i10 = 2;
                                fApplyDimension = TypedValue.applyDimension(2, 12.0f, displayMetrics);
                            } else {
                                i10 = 2;
                            }
                            validateAndSetAutoSizeTextTypeUniformConfiguration(fApplyDimension, dimension == -1.0f ? TypedValue.applyDimension(i10, 112.0f, displayMetrics) : dimension, dimension2 == -1.0f ? 1.0f : dimension2);
                        }
                        setupAutoSizeText();
                    }
                    i7 = dimensionPixelSize;
                    if (i7 >= 0) {
                        setFirstBaselineToTopHeight(i7);
                    }
                    i8 = dimensionPixelSize2;
                    if (i8 >= 0) {
                        setLastBaselineToBottomHeight(i8);
                    }
                    i9 = dimensionPixelSize3;
                    if (i9 < 0) {
                        setLineHeight(i9);
                        return;
                    }
                    return;
                }
                if (z18) {
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = DialerKeyListener.getInstance();
                    this.mEditor.mInputType = 3;
                } else if (i39 != 0) {
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = DigitsKeyListener.getInstance(null, (i39 & 2) != 0, (i39 & 4) != 0);
                    this.mEditor.mInputType = this.mEditor.mKeyListener.getInputType();
                } else if (z19 || i40 != -1) {
                    switch (i40) {
                        case 1:
                            capitalize = TextKeyListener.Capitalize.SENTENCES;
                            i11 = 16385;
                            break;
                        case 2:
                            capitalize = TextKeyListener.Capitalize.WORDS;
                            i11 = MtpConstants.RESPONSE_OK;
                            break;
                        case 3:
                            capitalize = TextKeyListener.Capitalize.CHARACTERS;
                            i11 = 4097;
                            break;
                        default:
                            capitalize = TextKeyListener.Capitalize.NONE;
                            i11 = 1;
                            break;
                    }
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = TextKeyListener.getInstance(z19, capitalize);
                    this.mEditor.mInputType = i11;
                } else if (z20) {
                    createEditorIfNeeded();
                    this.mEditor.mKeyListener = TextKeyListener.getInstance();
                    this.mEditor.mInputType = 1;
                } else {
                    if (isTextSelectable()) {
                        if (this.mEditor != null) {
                            this.mEditor.mKeyListener = null;
                            this.mEditor.mInputType = 0;
                        }
                        bufferType = BufferType.SPANNABLE;
                        setMovementMethod(ArrowKeyMovementMethod.getInstance());
                    } else {
                        if (this.mEditor != null) {
                            this.mEditor.mKeyListener = null;
                        }
                        switch (i38) {
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
                        if (this.mEditor != null) {
                        }
                        if (z17) {
                        }
                        colorStateList = colorStateList2;
                        if (colorStateList == null) {
                            if (this.mDrawables != null) {
                            }
                            if (colorStateList == null) {
                            }
                            if (mode2 != null) {
                            }
                        }
                        setCompoundDrawablesWithIntrinsicBounds(drawable, drawable2, drawable3, drawable4);
                        setRelativeDrawablesIfNeeded(drawable5, drawable6);
                        setCompoundDrawablePadding(i41);
                        setInputTypeSingleLine(z);
                        applySingleLine(z, z, z);
                        if (z) {
                            i3 = i37;
                        }
                        switch (i3) {
                        }
                        if (z9) {
                        }
                        if (!z3 || (this.mEditor != null && (this.mEditor.mInputType & FileObserver.ALL_EVENTS) == 129)) {
                        }
                        applyTextAppearance(textAppearanceAttributes);
                        if (z3) {
                        }
                        i4 = i22;
                        if (i4 < 0) {
                        }
                        setText(text, bufferType);
                        if (z14) {
                        }
                        charSequence = text4;
                        if (charSequence != null) {
                        }
                        TypedArray typedArrayObtainStyledAttributes42 = context2.obtainStyledAttributes(attributeSet, R.styleable.View, i, i2);
                        if (this.mMovement == null) {
                        }
                        if (z24) {
                        }
                        if (z24) {
                        }
                        focusable = getFocusable();
                        indexCount = typedArrayObtainStyledAttributes42.getIndexCount();
                        boolean z272 = z26;
                        while (i5 < indexCount) {
                        }
                        typedArrayObtainStyledAttributes42.recycle();
                        if (focusable != getFocusable()) {
                        }
                        setClickable(z25);
                        setLongClickable(z272);
                        if (this.mEditor != null) {
                        }
                        if (getImportantForAccessibility() != 0) {
                        }
                        if (supportsAutoSizeText()) {
                        }
                        i7 = dimensionPixelSize;
                        if (i7 >= 0) {
                        }
                        i8 = dimensionPixelSize2;
                        if (i8 >= 0) {
                        }
                        i9 = dimensionPixelSize3;
                        if (i9 < 0) {
                        }
                    }
                    z = z16;
                    if (this.mEditor != null) {
                    }
                    if (z17) {
                    }
                    colorStateList = colorStateList2;
                    if (colorStateList == null) {
                    }
                    setCompoundDrawablesWithIntrinsicBounds(drawable, drawable2, drawable3, drawable4);
                    setRelativeDrawablesIfNeeded(drawable5, drawable6);
                    setCompoundDrawablePadding(i41);
                    setInputTypeSingleLine(z);
                    applySingleLine(z, z, z);
                    if (z) {
                    }
                    switch (i3) {
                    }
                    if (z9) {
                    }
                    if (!z3 || (this.mEditor != null && (this.mEditor.mInputType & FileObserver.ALL_EVENTS) == 129)) {
                    }
                    applyTextAppearance(textAppearanceAttributes);
                    if (z3) {
                    }
                    i4 = i22;
                    if (i4 < 0) {
                    }
                    setText(text, bufferType);
                    if (z14) {
                    }
                    charSequence = text4;
                    if (charSequence != null) {
                    }
                    TypedArray typedArrayObtainStyledAttributes422 = context2.obtainStyledAttributes(attributeSet, R.styleable.View, i, i2);
                    if (this.mMovement == null) {
                    }
                    if (z24) {
                    }
                    if (z24) {
                    }
                    focusable = getFocusable();
                    indexCount = typedArrayObtainStyledAttributes422.getIndexCount();
                    boolean z2722 = z26;
                    while (i5 < indexCount) {
                    }
                    typedArrayObtainStyledAttributes422.recycle();
                    if (focusable != getFocusable()) {
                    }
                    setClickable(z25);
                    setLongClickable(z2722);
                    if (this.mEditor != null) {
                    }
                    if (getImportantForAccessibility() != 0) {
                    }
                    if (supportsAutoSizeText()) {
                    }
                    i7 = dimensionPixelSize;
                    if (i7 >= 0) {
                    }
                    i8 = dimensionPixelSize2;
                    if (i8 >= 0) {
                    }
                    i9 = dimensionPixelSize3;
                    if (i9 < 0) {
                    }
                }
            }
        }
        z = z16;
        bufferType = bufferType2;
        if (this.mEditor != null) {
        }
        if (z17) {
        }
        colorStateList = colorStateList2;
        if (colorStateList == null) {
        }
        setCompoundDrawablesWithIntrinsicBounds(drawable, drawable2, drawable3, drawable4);
        setRelativeDrawablesIfNeeded(drawable5, drawable6);
        setCompoundDrawablePadding(i41);
        setInputTypeSingleLine(z);
        applySingleLine(z, z, z);
        if (z) {
        }
        switch (i3) {
        }
        if (z9) {
        }
        if (!z3 || (this.mEditor != null && (this.mEditor.mInputType & FileObserver.ALL_EVENTS) == 129)) {
        }
        applyTextAppearance(textAppearanceAttributes);
        if (z3) {
        }
        i4 = i22;
        if (i4 < 0) {
        }
        setText(text, bufferType);
        if (z14) {
        }
        charSequence = text4;
        if (charSequence != null) {
        }
        TypedArray typedArrayObtainStyledAttributes4222 = context2.obtainStyledAttributes(attributeSet, R.styleable.View, i, i2);
        if (this.mMovement == null) {
        }
        if (z24) {
        }
        if (z24) {
        }
        focusable = getFocusable();
        indexCount = typedArrayObtainStyledAttributes4222.getIndexCount();
        boolean z27222 = z26;
        while (i5 < indexCount) {
        }
        typedArrayObtainStyledAttributes4222.recycle();
        if (focusable != getFocusable()) {
        }
        setClickable(z25);
        setLongClickable(z27222);
        if (this.mEditor != null) {
        }
        if (getImportantForAccessibility() != 0) {
        }
        if (supportsAutoSizeText()) {
        }
        i7 = dimensionPixelSize;
        if (i7 >= 0) {
        }
        i8 = dimensionPixelSize2;
        if (i8 >= 0) {
        }
        i9 = dimensionPixelSize3;
        if (i9 < 0) {
        }
    }

    private void setTextInternal(CharSequence charSequence) {
        this.mText = charSequence;
        this.mSpannable = charSequence instanceof Spannable ? (Spannable) charSequence : null;
        this.mPrecomputed = charSequence instanceof PrecomputedText ? (PrecomputedText) charSequence : null;
    }

    public void setAutoSizeTextTypeWithDefaults(int i) {
        if (supportsAutoSizeText()) {
            switch (i) {
                case 0:
                    clearAutoSizeConfiguration();
                    return;
                case 1:
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    validateAndSetAutoSizeTextTypeUniformConfiguration(TypedValue.applyDimension(2, 12.0f, displayMetrics), TypedValue.applyDimension(2, 112.0f, displayMetrics), 1.0f);
                    if (setupAutoSizeText()) {
                        autoSizeText();
                        invalidate();
                        return;
                    }
                    return;
                default:
                    throw new IllegalArgumentException("Unknown auto-size text type: " + i);
            }
        }
    }

    public void setAutoSizeTextTypeUniformWithConfiguration(int i, int i2, int i3, int i4) {
        if (supportsAutoSizeText()) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            validateAndSetAutoSizeTextTypeUniformConfiguration(TypedValue.applyDimension(i4, i, displayMetrics), TypedValue.applyDimension(i4, i2, displayMetrics), TypedValue.applyDimension(i4, i3, displayMetrics));
            if (setupAutoSizeText()) {
                autoSizeText();
                invalidate();
            }
        }
    }

    public void setAutoSizeTextTypeUniformWithPresetSizes(int[] iArr, int i) {
        if (supportsAutoSizeText()) {
            int length = iArr.length;
            if (length > 0) {
                int[] iArrCopyOf = new int[length];
                if (i == 0) {
                    iArrCopyOf = Arrays.copyOf(iArr, length);
                } else {
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    for (int i2 = 0; i2 < length; i2++) {
                        iArrCopyOf[i2] = Math.round(TypedValue.applyDimension(i, iArr[i2], displayMetrics));
                    }
                }
                this.mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(iArrCopyOf);
                if (!setupAutoSizeUniformPresetSizesConfiguration()) {
                    throw new IllegalArgumentException("None of the preset sizes is valid: " + Arrays.toString(iArr));
                }
            } else {
                this.mHasPresetAutoSizeValues = false;
            }
            if (setupAutoSizeText()) {
                autoSizeText();
                invalidate();
            }
        }
    }

    public int getAutoSizeTextType() {
        return this.mAutoSizeTextType;
    }

    public int getAutoSizeStepGranularity() {
        return Math.round(this.mAutoSizeStepGranularityInPx);
    }

    public int getAutoSizeMinTextSize() {
        return Math.round(this.mAutoSizeMinTextSizeInPx);
    }

    public int getAutoSizeMaxTextSize() {
        return Math.round(this.mAutoSizeMaxTextSizeInPx);
    }

    public int[] getAutoSizeTextAvailableSizes() {
        return this.mAutoSizeTextSizesInPx;
    }

    private void setupAutoSizeUniformPresetSizes(TypedArray typedArray) {
        int length = typedArray.length();
        int[] iArr = new int[length];
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                iArr[i] = typedArray.getDimensionPixelSize(i, -1);
            }
            this.mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(iArr);
            setupAutoSizeUniformPresetSizesConfiguration();
        }
    }

    private boolean setupAutoSizeUniformPresetSizesConfiguration() {
        this.mHasPresetAutoSizeValues = this.mAutoSizeTextSizesInPx.length > 0;
        if (this.mHasPresetAutoSizeValues) {
            this.mAutoSizeTextType = 1;
            this.mAutoSizeMinTextSizeInPx = this.mAutoSizeTextSizesInPx[0];
            this.mAutoSizeMaxTextSizeInPx = this.mAutoSizeTextSizesInPx[r0 - 1];
            this.mAutoSizeStepGranularityInPx = -1.0f;
        }
        return this.mHasPresetAutoSizeValues;
    }

    private void validateAndSetAutoSizeTextTypeUniformConfiguration(float f, float f2, float f3) {
        if (f <= 0.0f) {
            throw new IllegalArgumentException("Minimum auto-size text size (" + f + "px) is less or equal to (0px)");
        }
        if (f2 <= f) {
            throw new IllegalArgumentException("Maximum auto-size text size (" + f2 + "px) is less or equal to minimum auto-size text size (" + f + "px)");
        }
        if (f3 <= 0.0f) {
            throw new IllegalArgumentException("The auto-size step granularity (" + f3 + "px) is less or equal to (0px)");
        }
        this.mAutoSizeTextType = 1;
        this.mAutoSizeMinTextSizeInPx = f;
        this.mAutoSizeMaxTextSizeInPx = f2;
        this.mAutoSizeStepGranularityInPx = f3;
        this.mHasPresetAutoSizeValues = false;
    }

    private void clearAutoSizeConfiguration() {
        this.mAutoSizeTextType = 0;
        this.mAutoSizeMinTextSizeInPx = -1.0f;
        this.mAutoSizeMaxTextSizeInPx = -1.0f;
        this.mAutoSizeStepGranularityInPx = -1.0f;
        this.mAutoSizeTextSizesInPx = EmptyArray.INT;
        this.mNeedsAutoSizeText = false;
    }

    private int[] cleanupAutoSizePresetSizes(int[] iArr) {
        int length = iArr.length;
        if (length == 0) {
            return iArr;
        }
        Arrays.sort(iArr);
        IntArray intArray = new IntArray();
        for (int i : iArr) {
            if (i > 0 && intArray.binarySearch(i) < 0) {
                intArray.add(i);
            }
        }
        if (length == intArray.size()) {
            return iArr;
        }
        return intArray.toArray();
    }

    private boolean setupAutoSizeText() {
        if (supportsAutoSizeText() && this.mAutoSizeTextType == 1) {
            if (!this.mHasPresetAutoSizeValues || this.mAutoSizeTextSizesInPx.length == 0) {
                int iFloor = ((int) Math.floor((this.mAutoSizeMaxTextSizeInPx - this.mAutoSizeMinTextSizeInPx) / this.mAutoSizeStepGranularityInPx)) + 1;
                int[] iArr = new int[iFloor];
                for (int i = 0; i < iFloor; i++) {
                    iArr[i] = Math.round(this.mAutoSizeMinTextSizeInPx + (i * this.mAutoSizeStepGranularityInPx));
                }
                this.mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(iArr);
            }
            this.mNeedsAutoSizeText = true;
        } else {
            this.mNeedsAutoSizeText = false;
        }
        return this.mNeedsAutoSizeText;
    }

    private int[] parseDimensionArray(TypedArray typedArray) {
        if (typedArray == null) {
            return null;
        }
        int[] iArr = new int[typedArray.length()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = typedArray.getDimensionPixelSize(i, 0);
        }
        return iArr;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 100) {
            if (i2 != -1 || intent == null) {
                if (this.mSpannable != null) {
                    Selection.setSelection(this.mSpannable, getSelectionEnd());
                    return;
                }
                return;
            }
            CharSequence charSequenceExtra = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            if (charSequenceExtra != null) {
                if (isTextEditable()) {
                    replaceSelectionWithText(charSequenceExtra);
                    if (this.mEditor != null) {
                        this.mEditor.refreshTextActionMode();
                        return;
                    }
                    return;
                }
                if (charSequenceExtra.length() > 0) {
                    Toast.makeText(getContext(), String.valueOf(charSequenceExtra), 1).show();
                }
            }
        }
    }

    private void setTypefaceFromAttrs(Typeface typeface, String str, int i, int i2, int i3) {
        if (typeface == null && str != null) {
            resolveStyleAndSetTypeface(Typeface.create(str, 0), i2, i3);
            return;
        }
        if (typeface != null) {
            resolveStyleAndSetTypeface(typeface, i2, i3);
            return;
        }
        switch (i) {
            case 1:
                resolveStyleAndSetTypeface(Typeface.SANS_SERIF, i2, i3);
                break;
            case 2:
                resolveStyleAndSetTypeface(Typeface.SERIF, i2, i3);
                break;
            case 3:
                resolveStyleAndSetTypeface(Typeface.MONOSPACE, i2, i3);
                break;
            default:
                resolveStyleAndSetTypeface(null, i2, i3);
                break;
        }
    }

    private void resolveStyleAndSetTypeface(Typeface typeface, int i, int i2) {
        if (i2 >= 0) {
            setTypeface(Typeface.create(typeface, Math.min(1000, i2), (i & 2) != 0));
        } else {
            setTypeface(typeface, i);
        }
    }

    private void setRelativeDrawablesIfNeeded(Drawable drawable, Drawable drawable2) {
        if ((drawable == null && drawable2 == null) ? false : true) {
            Drawables drawables = this.mDrawables;
            if (drawables == null) {
                drawables = new Drawables(getContext());
                this.mDrawables = drawables;
            }
            this.mDrawables.mOverride = true;
            Rect rect = drawables.mCompoundRect;
            int[] drawableState = getDrawableState();
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                drawable.setState(drawableState);
                drawable.copyBounds(rect);
                drawable.setCallback(this);
                drawables.mDrawableStart = drawable;
                drawables.mDrawableSizeStart = rect.width();
                drawables.mDrawableHeightStart = rect.height();
            } else {
                drawables.mDrawableHeightStart = 0;
                drawables.mDrawableSizeStart = 0;
            }
            if (drawable2 != null) {
                drawable2.setBounds(0, 0, drawable2.getIntrinsicWidth(), drawable2.getIntrinsicHeight());
                drawable2.setState(drawableState);
                drawable2.copyBounds(rect);
                drawable2.setCallback(this);
                drawables.mDrawableEnd = drawable2;
                drawables.mDrawableSizeEnd = rect.width();
                drawables.mDrawableHeightEnd = rect.height();
            } else {
                drawables.mDrawableHeightEnd = 0;
                drawables.mDrawableSizeEnd = 0;
            }
            resetResolvedDrawables();
            resolveDrawables();
            applyCompoundDrawableTint();
        }
    }

    @Override
    @RemotableViewMethod
    public void setEnabled(boolean z) {
        InputMethodManager inputMethodManagerPeekInstance;
        InputMethodManager inputMethodManagerPeekInstance2;
        if (z == isEnabled()) {
            return;
        }
        if (!z && (inputMethodManagerPeekInstance2 = InputMethodManager.peekInstance()) != null && inputMethodManagerPeekInstance2.isActive(this)) {
            inputMethodManagerPeekInstance2.hideSoftInputFromWindow(getWindowToken(), 0);
        }
        super.setEnabled(z);
        if (z && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null) {
            inputMethodManagerPeekInstance.restartInput(this);
        }
        if (this.mEditor != null) {
            this.mEditor.invalidateTextDisplayList();
            this.mEditor.prepareCursorControllers();
            this.mEditor.makeBlink();
        }
    }

    public void setTypeface(Typeface typeface, int i) {
        Typeface typefaceCreate;
        if (i > 0) {
            if (typeface == null) {
                typefaceCreate = Typeface.defaultFromStyle(i);
            } else {
                typefaceCreate = Typeface.create(typeface, i);
            }
            setTypeface(typefaceCreate);
            int i2 = (~(typefaceCreate != null ? typefaceCreate.getStyle() : 0)) & i;
            this.mTextPaint.setFakeBoldText((i2 & 1) != 0);
            this.mTextPaint.setTextSkewX((i2 & 2) != 0 ? -0.25f : 0.0f);
            return;
        }
        this.mTextPaint.setFakeBoldText(false);
        this.mTextPaint.setTextSkewX(0.0f);
        setTypeface(typeface);
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
        throw new UnsupportedOperationException("not implemented");
    }

    @VisibleForTesting
    public final Editor getEditorForTesting() {
        return this.mEditor;
    }

    public final void setUndoManager(UndoManager undoManager, String str) {
        throw new UnsupportedOperationException("not implemented");
    }

    public final KeyListener getKeyListener() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mKeyListener;
    }

    public void setKeyListener(KeyListener keyListener) {
        this.mListenerChanged = true;
        setKeyListenerOnly(keyListener);
        fixFocusableAndClickableSettings();
        if (keyListener != null) {
            createEditorIfNeeded();
            setInputTypeFromEditor();
        } else if (this.mEditor != null) {
            this.mEditor.mInputType = 0;
        }
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            inputMethodManagerPeekInstance.restartInput(this);
        }
    }

    private void setInputTypeFromEditor() {
        try {
            this.mEditor.mInputType = this.mEditor.mKeyListener.getInputType();
        } catch (IncompatibleClassChangeError e) {
            this.mEditor.mInputType = 1;
        }
        setInputTypeSingleLine(this.mSingleLine);
    }

    private void setKeyListenerOnly(KeyListener keyListener) {
        if (this.mEditor == null && keyListener == null) {
            return;
        }
        createEditorIfNeeded();
        if (this.mEditor.mKeyListener != keyListener) {
            this.mEditor.mKeyListener = keyListener;
            if (keyListener != null && !(this.mText instanceof Editable)) {
                setText(this.mText);
            }
            setFilters((Editable) this.mText, this.mFilters);
        }
    }

    public final MovementMethod getMovementMethod() {
        return this.mMovement;
    }

    public final void setMovementMethod(MovementMethod movementMethod) {
        if (this.mMovement != movementMethod) {
            this.mMovement = movementMethod;
            if (movementMethod != null && this.mSpannable == null) {
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
            setFocusable(1);
            setClickable(true);
            setLongClickable(true);
        } else {
            setFocusable(16);
            setClickable(false);
            setLongClickable(false);
        }
    }

    public final TransformationMethod getTransformationMethod() {
        return this.mTransformation;
    }

    public final void setTransformationMethod(TransformationMethod transformationMethod) {
        if (transformationMethod == this.mTransformation) {
            return;
        }
        if (this.mTransformation != null && this.mSpannable != null) {
            this.mSpannable.removeSpan(this.mTransformation);
        }
        this.mTransformation = transformationMethod;
        if (transformationMethod instanceof TransformationMethod2) {
            TransformationMethod2 transformationMethod2 = (TransformationMethod2) transformationMethod;
            this.mAllowTransformationLengthChange = (isTextSelectable() || (this.mText instanceof Editable)) ? false : true;
            transformationMethod2.setLengthChangesAllowed(this.mAllowTransformationLengthChange);
        } else {
            this.mAllowTransformationLengthChange = false;
        }
        setText(this.mText);
        if (hasPasswordTransformationMethod()) {
            notifyViewAccessibilityStateChangedIfNeeded(0);
        }
        this.mTextDir = getTextDirectionHeuristic();
    }

    public int getCompoundPaddingTop() {
        Drawables drawables = this.mDrawables;
        if (drawables == null || drawables.mShowing[1] == null) {
            return this.mPaddingTop;
        }
        return this.mPaddingTop + drawables.mDrawablePadding + drawables.mDrawableSizeTop;
    }

    public int getCompoundPaddingBottom() {
        Drawables drawables = this.mDrawables;
        if (drawables == null || drawables.mShowing[3] == null) {
            return this.mPaddingBottom;
        }
        return this.mPaddingBottom + drawables.mDrawablePadding + drawables.mDrawableSizeBottom;
    }

    public int getCompoundPaddingLeft() {
        Drawables drawables = this.mDrawables;
        if (drawables == null || drawables.mShowing[0] == null) {
            return this.mPaddingLeft;
        }
        return this.mPaddingLeft + drawables.mDrawablePadding + drawables.mDrawableSizeLeft;
    }

    public int getCompoundPaddingRight() {
        Drawables drawables = this.mDrawables;
        if (drawables == null || drawables.mShowing[2] == null) {
            return this.mPaddingRight;
        }
        return this.mPaddingRight + drawables.mDrawablePadding + drawables.mDrawableSizeRight;
    }

    public int getCompoundPaddingStart() {
        resolveDrawables();
        if (getLayoutDirection() != 1) {
            return getCompoundPaddingLeft();
        }
        return getCompoundPaddingRight();
    }

    public int getCompoundPaddingEnd() {
        resolveDrawables();
        if (getLayoutDirection() != 1) {
            return getCompoundPaddingRight();
        }
        return getCompoundPaddingLeft();
    }

    public int getExtendedPaddingTop() {
        int i;
        if (this.mMaxMode != 1) {
            return getCompoundPaddingTop();
        }
        if (this.mLayout == null) {
            assumeLayout();
        }
        if (this.mLayout.getLineCount() <= this.mMaximum) {
            return getCompoundPaddingTop();
        }
        int compoundPaddingTop = getCompoundPaddingTop();
        int height = (getHeight() - compoundPaddingTop) - getCompoundPaddingBottom();
        int lineTop = this.mLayout.getLineTop(this.mMaximum);
        if (lineTop >= height || (i = this.mGravity & 112) == 48) {
            return compoundPaddingTop;
        }
        if (i == 80) {
            return (compoundPaddingTop + height) - lineTop;
        }
        return compoundPaddingTop + ((height - lineTop) / 2);
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
        int compoundPaddingTop = getCompoundPaddingTop();
        int compoundPaddingBottom = getCompoundPaddingBottom();
        int height = (getHeight() - compoundPaddingTop) - compoundPaddingBottom;
        int lineTop = this.mLayout.getLineTop(this.mMaximum);
        if (lineTop >= height) {
            return compoundPaddingBottom;
        }
        int i = this.mGravity & 112;
        if (i == 48) {
            return (compoundPaddingBottom + height) - lineTop;
        }
        if (i == 80) {
            return compoundPaddingBottom;
        }
        return compoundPaddingBottom + ((height - lineTop) / 2);
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

    public void setCompoundDrawables(Drawable drawable, Drawable drawable2, Drawable drawable3, Drawable drawable4) {
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            if (drawables.mDrawableStart != null) {
                drawables.mDrawableStart.setCallback(null);
            }
            drawables.mDrawableStart = null;
            if (drawables.mDrawableEnd != null) {
                drawables.mDrawableEnd.setCallback(null);
            }
            drawables.mDrawableEnd = null;
            drawables.mDrawableHeightStart = 0;
            drawables.mDrawableSizeStart = 0;
            drawables.mDrawableHeightEnd = 0;
            drawables.mDrawableSizeEnd = 0;
        }
        if (!((drawable == null && drawable2 == null && drawable3 == null && drawable4 == null) ? false : true)) {
            if (drawables != null) {
                if (drawables.hasMetadata()) {
                    for (int length = drawables.mShowing.length - 1; length >= 0; length--) {
                        if (drawables.mShowing[length] != null) {
                            drawables.mShowing[length].setCallback(null);
                        }
                        drawables.mShowing[length] = null;
                    }
                    drawables.mDrawableHeightLeft = 0;
                    drawables.mDrawableSizeLeft = 0;
                    drawables.mDrawableHeightRight = 0;
                    drawables.mDrawableSizeRight = 0;
                    drawables.mDrawableWidthTop = 0;
                    drawables.mDrawableSizeTop = 0;
                    drawables.mDrawableWidthBottom = 0;
                    drawables.mDrawableSizeBottom = 0;
                } else {
                    this.mDrawables = null;
                }
            }
        } else {
            if (drawables == null) {
                drawables = new Drawables(getContext());
                this.mDrawables = drawables;
            }
            this.mDrawables.mOverride = false;
            if (drawables.mShowing[0] != drawable && drawables.mShowing[0] != null) {
                drawables.mShowing[0].setCallback(null);
            }
            drawables.mShowing[0] = drawable;
            if (drawables.mShowing[1] != drawable2 && drawables.mShowing[1] != null) {
                drawables.mShowing[1].setCallback(null);
            }
            drawables.mShowing[1] = drawable2;
            if (drawables.mShowing[2] != drawable3 && drawables.mShowing[2] != null) {
                drawables.mShowing[2].setCallback(null);
            }
            drawables.mShowing[2] = drawable3;
            if (drawables.mShowing[3] != drawable4 && drawables.mShowing[3] != null) {
                drawables.mShowing[3].setCallback(null);
            }
            drawables.mShowing[3] = drawable4;
            Rect rect = drawables.mCompoundRect;
            int[] drawableState = getDrawableState();
            if (drawable != null) {
                drawable.setState(drawableState);
                drawable.copyBounds(rect);
                drawable.setCallback(this);
                drawables.mDrawableSizeLeft = rect.width();
                drawables.mDrawableHeightLeft = rect.height();
            } else {
                drawables.mDrawableHeightLeft = 0;
                drawables.mDrawableSizeLeft = 0;
            }
            if (drawable3 != null) {
                drawable3.setState(drawableState);
                drawable3.copyBounds(rect);
                drawable3.setCallback(this);
                drawables.mDrawableSizeRight = rect.width();
                drawables.mDrawableHeightRight = rect.height();
            } else {
                drawables.mDrawableHeightRight = 0;
                drawables.mDrawableSizeRight = 0;
            }
            if (drawable2 != null) {
                drawable2.setState(drawableState);
                drawable2.copyBounds(rect);
                drawable2.setCallback(this);
                drawables.mDrawableSizeTop = rect.height();
                drawables.mDrawableWidthTop = rect.width();
            } else {
                drawables.mDrawableWidthTop = 0;
                drawables.mDrawableSizeTop = 0;
            }
            if (drawable4 != null) {
                drawable4.setState(drawableState);
                drawable4.copyBounds(rect);
                drawable4.setCallback(this);
                drawables.mDrawableSizeBottom = rect.height();
                drawables.mDrawableWidthBottom = rect.width();
            } else {
                drawables.mDrawableWidthBottom = 0;
                drawables.mDrawableSizeBottom = 0;
            }
        }
        if (drawables != null) {
            drawables.mDrawableLeftInitial = drawable;
            drawables.mDrawableRightInitial = drawable3;
        }
        resetResolvedDrawables();
        resolveDrawables();
        applyCompoundDrawableTint();
        invalidate();
        requestLayout();
    }

    @RemotableViewMethod
    public void setCompoundDrawablesWithIntrinsicBounds(int i, int i2, int i3, int i4) {
        Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(i != 0 ? context.getDrawable(i) : null, i2 != 0 ? context.getDrawable(i2) : null, i3 != 0 ? context.getDrawable(i3) : null, i4 != 0 ? context.getDrawable(i4) : null);
    }

    @RemotableViewMethod
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable drawable, Drawable drawable2, Drawable drawable3, Drawable drawable4) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        if (drawable3 != null) {
            drawable3.setBounds(0, 0, drawable3.getIntrinsicWidth(), drawable3.getIntrinsicHeight());
        }
        if (drawable2 != null) {
            drawable2.setBounds(0, 0, drawable2.getIntrinsicWidth(), drawable2.getIntrinsicHeight());
        }
        if (drawable4 != null) {
            drawable4.setBounds(0, 0, drawable4.getIntrinsicWidth(), drawable4.getIntrinsicHeight());
        }
        setCompoundDrawables(drawable, drawable2, drawable3, drawable4);
    }

    @RemotableViewMethod
    public void setCompoundDrawablesRelative(Drawable drawable, Drawable drawable2, Drawable drawable3, Drawable drawable4) {
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            if (drawables.mShowing[0] != null) {
                drawables.mShowing[0].setCallback(null);
            }
            Drawable[] drawableArr = drawables.mShowing;
            drawables.mDrawableLeftInitial = null;
            drawableArr[0] = null;
            if (drawables.mShowing[2] != null) {
                drawables.mShowing[2].setCallback(null);
            }
            Drawable[] drawableArr2 = drawables.mShowing;
            drawables.mDrawableRightInitial = null;
            drawableArr2[2] = null;
            drawables.mDrawableHeightLeft = 0;
            drawables.mDrawableSizeLeft = 0;
            drawables.mDrawableHeightRight = 0;
            drawables.mDrawableSizeRight = 0;
        }
        if (!((drawable == null && drawable2 == null && drawable3 == null && drawable4 == null) ? false : true)) {
            if (drawables != null) {
                if (!drawables.hasMetadata()) {
                    this.mDrawables = null;
                } else {
                    if (drawables.mDrawableStart != null) {
                        drawables.mDrawableStart.setCallback(null);
                    }
                    drawables.mDrawableStart = null;
                    if (drawables.mShowing[1] != null) {
                        drawables.mShowing[1].setCallback(null);
                    }
                    drawables.mShowing[1] = null;
                    if (drawables.mDrawableEnd != null) {
                        drawables.mDrawableEnd.setCallback(null);
                    }
                    drawables.mDrawableEnd = null;
                    if (drawables.mShowing[3] != null) {
                        drawables.mShowing[3].setCallback(null);
                    }
                    drawables.mShowing[3] = null;
                    drawables.mDrawableHeightStart = 0;
                    drawables.mDrawableSizeStart = 0;
                    drawables.mDrawableHeightEnd = 0;
                    drawables.mDrawableSizeEnd = 0;
                    drawables.mDrawableWidthTop = 0;
                    drawables.mDrawableSizeTop = 0;
                    drawables.mDrawableWidthBottom = 0;
                    drawables.mDrawableSizeBottom = 0;
                }
            }
        } else {
            if (drawables == null) {
                drawables = new Drawables(getContext());
                this.mDrawables = drawables;
            }
            this.mDrawables.mOverride = true;
            if (drawables.mDrawableStart != drawable && drawables.mDrawableStart != null) {
                drawables.mDrawableStart.setCallback(null);
            }
            drawables.mDrawableStart = drawable;
            if (drawables.mShowing[1] != drawable2 && drawables.mShowing[1] != null) {
                drawables.mShowing[1].setCallback(null);
            }
            drawables.mShowing[1] = drawable2;
            if (drawables.mDrawableEnd != drawable3 && drawables.mDrawableEnd != null) {
                drawables.mDrawableEnd.setCallback(null);
            }
            drawables.mDrawableEnd = drawable3;
            if (drawables.mShowing[3] != drawable4 && drawables.mShowing[3] != null) {
                drawables.mShowing[3].setCallback(null);
            }
            drawables.mShowing[3] = drawable4;
            Rect rect = drawables.mCompoundRect;
            int[] drawableState = getDrawableState();
            if (drawable != null) {
                drawable.setState(drawableState);
                drawable.copyBounds(rect);
                drawable.setCallback(this);
                drawables.mDrawableSizeStart = rect.width();
                drawables.mDrawableHeightStart = rect.height();
            } else {
                drawables.mDrawableHeightStart = 0;
                drawables.mDrawableSizeStart = 0;
            }
            if (drawable3 != null) {
                drawable3.setState(drawableState);
                drawable3.copyBounds(rect);
                drawable3.setCallback(this);
                drawables.mDrawableSizeEnd = rect.width();
                drawables.mDrawableHeightEnd = rect.height();
            } else {
                drawables.mDrawableHeightEnd = 0;
                drawables.mDrawableSizeEnd = 0;
            }
            if (drawable2 != null) {
                drawable2.setState(drawableState);
                drawable2.copyBounds(rect);
                drawable2.setCallback(this);
                drawables.mDrawableSizeTop = rect.height();
                drawables.mDrawableWidthTop = rect.width();
            } else {
                drawables.mDrawableWidthTop = 0;
                drawables.mDrawableSizeTop = 0;
            }
            if (drawable4 != null) {
                drawable4.setState(drawableState);
                drawable4.copyBounds(rect);
                drawable4.setCallback(this);
                drawables.mDrawableSizeBottom = rect.height();
                drawables.mDrawableWidthBottom = rect.width();
            } else {
                drawables.mDrawableWidthBottom = 0;
                drawables.mDrawableSizeBottom = 0;
            }
        }
        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    @RemotableViewMethod
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int i, int i2, int i3, int i4) {
        Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(i != 0 ? context.getDrawable(i) : null, i2 != 0 ? context.getDrawable(i2) : null, i3 != 0 ? context.getDrawable(i3) : null, i4 != 0 ? context.getDrawable(i4) : null);
    }

    @RemotableViewMethod
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable drawable, Drawable drawable2, Drawable drawable3, Drawable drawable4) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        if (drawable3 != null) {
            drawable3.setBounds(0, 0, drawable3.getIntrinsicWidth(), drawable3.getIntrinsicHeight());
        }
        if (drawable2 != null) {
            drawable2.setBounds(0, 0, drawable2.getIntrinsicWidth(), drawable2.getIntrinsicHeight());
        }
        if (drawable4 != null) {
            drawable4.setBounds(0, 0, drawable4.getIntrinsicWidth(), drawable4.getIntrinsicHeight());
        }
        setCompoundDrawablesRelative(drawable, drawable2, drawable3, drawable4);
    }

    public Drawable[] getCompoundDrawables() {
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            return (Drawable[]) drawables.mShowing.clone();
        }
        return new Drawable[]{null, null, null, null};
    }

    public Drawable[] getCompoundDrawablesRelative() {
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            return new Drawable[]{drawables.mDrawableStart, drawables.mShowing[1], drawables.mDrawableEnd, drawables.mShowing[3]};
        }
        return new Drawable[]{null, null, null, null};
    }

    @RemotableViewMethod
    public void setCompoundDrawablePadding(int i) {
        Drawables drawables = this.mDrawables;
        if (i == 0) {
            if (drawables != null) {
                drawables.mDrawablePadding = i;
            }
        } else {
            if (drawables == null) {
                drawables = new Drawables(getContext());
                this.mDrawables = drawables;
            }
            drawables.mDrawablePadding = i;
        }
        invalidate();
        requestLayout();
    }

    public int getCompoundDrawablePadding() {
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            return drawables.mDrawablePadding;
        }
        return 0;
    }

    public void setCompoundDrawableTintList(ColorStateList colorStateList) {
        if (this.mDrawables == null) {
            this.mDrawables = new Drawables(getContext());
        }
        this.mDrawables.mTintList = colorStateList;
        this.mDrawables.mHasTint = true;
        applyCompoundDrawableTint();
    }

    public ColorStateList getCompoundDrawableTintList() {
        if (this.mDrawables != null) {
            return this.mDrawables.mTintList;
        }
        return null;
    }

    public void setCompoundDrawableTintMode(PorterDuff.Mode mode) {
        if (this.mDrawables == null) {
            this.mDrawables = new Drawables(getContext());
        }
        this.mDrawables.mTintMode = mode;
        this.mDrawables.mHasTintMode = true;
        applyCompoundDrawableTint();
    }

    public PorterDuff.Mode getCompoundDrawableTintMode() {
        if (this.mDrawables != null) {
            return this.mDrawables.mTintMode;
        }
        return null;
    }

    private void applyCompoundDrawableTint() {
        if (this.mDrawables == null) {
            return;
        }
        if (this.mDrawables.mHasTint || this.mDrawables.mHasTintMode) {
            ColorStateList colorStateList = this.mDrawables.mTintList;
            PorterDuff.Mode mode = this.mDrawables.mTintMode;
            boolean z = this.mDrawables.mHasTint;
            boolean z2 = this.mDrawables.mHasTintMode;
            int[] drawableState = getDrawableState();
            for (Drawable drawable : this.mDrawables.mShowing) {
                if (drawable != null && drawable != this.mDrawables.mDrawableError) {
                    drawable.mutate();
                    if (z) {
                        drawable.setTintList(colorStateList);
                    }
                    if (z2) {
                        drawable.setTintMode(mode);
                    }
                    if (drawable.isStateful()) {
                        drawable.setState(drawableState);
                    }
                }
            }
        }
    }

    @Override
    public void setPadding(int i, int i2, int i3, int i4) {
        if (i != this.mPaddingLeft || i3 != this.mPaddingRight || i2 != this.mPaddingTop || i4 != this.mPaddingBottom) {
            nullLayouts();
        }
        super.setPadding(i, i2, i3, i4);
        invalidate();
    }

    @Override
    public void setPaddingRelative(int i, int i2, int i3, int i4) {
        if (i != getPaddingStart() || i3 != getPaddingEnd() || i2 != this.mPaddingTop || i4 != this.mPaddingBottom) {
            nullLayouts();
        }
        super.setPaddingRelative(i, i2, i3, i4);
        invalidate();
    }

    public void setFirstBaselineToTopHeight(int i) {
        int i2;
        Preconditions.checkArgumentNonnegative(i);
        Paint.FontMetricsInt fontMetricsInt = getPaint().getFontMetricsInt();
        if (getIncludeFontPadding()) {
            i2 = fontMetricsInt.top;
        } else {
            i2 = fontMetricsInt.ascent;
        }
        if (i > Math.abs(i2)) {
            setPadding(getPaddingLeft(), i - (-i2), getPaddingRight(), getPaddingBottom());
        }
    }

    public void setLastBaselineToBottomHeight(int i) {
        int i2;
        Preconditions.checkArgumentNonnegative(i);
        Paint.FontMetricsInt fontMetricsInt = getPaint().getFontMetricsInt();
        if (getIncludeFontPadding()) {
            i2 = fontMetricsInt.bottom;
        } else {
            i2 = fontMetricsInt.descent;
        }
        if (i > Math.abs(i2)) {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), i - i2);
        }
    }

    public int getFirstBaselineToTopHeight() {
        return getPaddingTop() - getPaint().getFontMetricsInt().top;
    }

    public int getLastBaselineToBottomHeight() {
        return getPaddingBottom() + getPaint().getFontMetricsInt().bottom;
    }

    public final int getAutoLinkMask() {
        return this.mAutoLinkMask;
    }

    public void setTextAppearance(int i) {
        setTextAppearance(this.mContext, i);
    }

    @Deprecated
    public void setTextAppearance(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(i, android.R.styleable.TextAppearance);
        TextAppearanceAttributes textAppearanceAttributes = new TextAppearanceAttributes();
        readTextAppearance(context, typedArrayObtainStyledAttributes, textAppearanceAttributes, false);
        typedArrayObtainStyledAttributes.recycle();
        applyTextAppearance(textAppearanceAttributes);
    }

    private static class TextAppearanceAttributes {
        boolean mAllCaps;
        boolean mElegant;
        boolean mFallbackLineSpacing;
        String mFontFamily;
        boolean mFontFamilyExplicit;
        String mFontFeatureSettings;
        Typeface mFontTypeface;
        int mFontWeight;
        boolean mHasElegant;
        boolean mHasFallbackLineSpacing;
        boolean mHasLetterSpacing;
        float mLetterSpacing;
        int mShadowColor;
        float mShadowDx;
        float mShadowDy;
        float mShadowRadius;
        int mStyleIndex;
        ColorStateList mTextColor;
        int mTextColorHighlight;
        ColorStateList mTextColorHint;
        ColorStateList mTextColorLink;
        int mTextSize;
        int mTypefaceIndex;

        private TextAppearanceAttributes() {
            this.mTextColorHighlight = 0;
            this.mTextColor = null;
            this.mTextColorHint = null;
            this.mTextColorLink = null;
            this.mTextSize = 0;
            this.mFontFamily = null;
            this.mFontTypeface = null;
            this.mFontFamilyExplicit = false;
            this.mTypefaceIndex = -1;
            this.mStyleIndex = -1;
            this.mFontWeight = -1;
            this.mAllCaps = false;
            this.mShadowColor = 0;
            this.mShadowDx = 0.0f;
            this.mShadowDy = 0.0f;
            this.mShadowRadius = 0.0f;
            this.mHasElegant = false;
            this.mElegant = false;
            this.mHasFallbackLineSpacing = false;
            this.mFallbackLineSpacing = false;
            this.mHasLetterSpacing = false;
            this.mLetterSpacing = 0.0f;
            this.mFontFeatureSettings = null;
        }

        public String toString() {
            return "TextAppearanceAttributes {\n    mTextColorHighlight:" + this.mTextColorHighlight + "\n    mTextColor:" + this.mTextColor + "\n    mTextColorHint:" + this.mTextColorHint + "\n    mTextColorLink:" + this.mTextColorLink + "\n    mTextSize:" + this.mTextSize + "\n    mFontFamily:" + this.mFontFamily + "\n    mFontTypeface:" + this.mFontTypeface + "\n    mFontFamilyExplicit:" + this.mFontFamilyExplicit + "\n    mTypefaceIndex:" + this.mTypefaceIndex + "\n    mStyleIndex:" + this.mStyleIndex + "\n    mFontWeight:" + this.mFontWeight + "\n    mAllCaps:" + this.mAllCaps + "\n    mShadowColor:" + this.mShadowColor + "\n    mShadowDx:" + this.mShadowDx + "\n    mShadowDy:" + this.mShadowDy + "\n    mShadowRadius:" + this.mShadowRadius + "\n    mHasElegant:" + this.mHasElegant + "\n    mElegant:" + this.mElegant + "\n    mHasFallbackLineSpacing:" + this.mHasFallbackLineSpacing + "\n    mFallbackLineSpacing:" + this.mFallbackLineSpacing + "\n    mHasLetterSpacing:" + this.mHasLetterSpacing + "\n    mLetterSpacing:" + this.mLetterSpacing + "\n    mFontFeatureSettings:" + this.mFontFeatureSettings + "\n}";
        }
    }

    private void readTextAppearance(Context context, TypedArray typedArray, TextAppearanceAttributes textAppearanceAttributes, boolean z) {
        int i;
        int indexCount = typedArray.getIndexCount();
        for (int i2 = 0; i2 < indexCount; i2++) {
            int index = typedArray.getIndex(i2);
            if (z) {
                i = sAppearanceValues.get(index, -1);
                if (i == -1) {
                }
            } else {
                i = index;
            }
            switch (i) {
                case 0:
                    textAppearanceAttributes.mTextSize = typedArray.getDimensionPixelSize(index, textAppearanceAttributes.mTextSize);
                    break;
                case 1:
                    textAppearanceAttributes.mTypefaceIndex = typedArray.getInt(index, textAppearanceAttributes.mTypefaceIndex);
                    if (textAppearanceAttributes.mTypefaceIndex != -1 && !textAppearanceAttributes.mFontFamilyExplicit) {
                        textAppearanceAttributes.mFontFamily = null;
                    }
                    break;
                case 2:
                    textAppearanceAttributes.mStyleIndex = typedArray.getInt(index, textAppearanceAttributes.mStyleIndex);
                    break;
                case 3:
                    textAppearanceAttributes.mTextColor = typedArray.getColorStateList(index);
                    break;
                case 4:
                    textAppearanceAttributes.mTextColorHighlight = typedArray.getColor(index, textAppearanceAttributes.mTextColorHighlight);
                    break;
                case 5:
                    textAppearanceAttributes.mTextColorHint = typedArray.getColorStateList(index);
                    break;
                case 6:
                    textAppearanceAttributes.mTextColorLink = typedArray.getColorStateList(index);
                    break;
                case 7:
                    textAppearanceAttributes.mShadowColor = typedArray.getInt(index, textAppearanceAttributes.mShadowColor);
                    break;
                case 8:
                    textAppearanceAttributes.mShadowDx = typedArray.getFloat(index, textAppearanceAttributes.mShadowDx);
                    break;
                case 9:
                    textAppearanceAttributes.mShadowDy = typedArray.getFloat(index, textAppearanceAttributes.mShadowDy);
                    break;
                case 10:
                    textAppearanceAttributes.mShadowRadius = typedArray.getFloat(index, textAppearanceAttributes.mShadowRadius);
                    break;
                case 11:
                    textAppearanceAttributes.mAllCaps = typedArray.getBoolean(index, textAppearanceAttributes.mAllCaps);
                    break;
                case 12:
                    if (!context.isRestricted() && context.canLoadUnsafeResources()) {
                        try {
                            textAppearanceAttributes.mFontTypeface = typedArray.getFont(index);
                            break;
                        } catch (Resources.NotFoundException | UnsupportedOperationException e) {
                        }
                    }
                    if (textAppearanceAttributes.mFontTypeface == null) {
                        textAppearanceAttributes.mFontFamily = typedArray.getString(index);
                    }
                    textAppearanceAttributes.mFontFamilyExplicit = true;
                    break;
                case 13:
                    textAppearanceAttributes.mHasElegant = true;
                    textAppearanceAttributes.mElegant = typedArray.getBoolean(index, textAppearanceAttributes.mElegant);
                    break;
                case 14:
                    textAppearanceAttributes.mHasLetterSpacing = true;
                    textAppearanceAttributes.mLetterSpacing = typedArray.getFloat(index, textAppearanceAttributes.mLetterSpacing);
                    break;
                case 15:
                    textAppearanceAttributes.mFontFeatureSettings = typedArray.getString(index);
                    break;
                case 16:
                    textAppearanceAttributes.mHasFallbackLineSpacing = true;
                    textAppearanceAttributes.mFallbackLineSpacing = typedArray.getBoolean(index, textAppearanceAttributes.mFallbackLineSpacing);
                    break;
                case 17:
                    textAppearanceAttributes.mFontWeight = typedArray.getInt(index, textAppearanceAttributes.mFontWeight);
                    break;
            }
        }
    }

    private void applyTextAppearance(TextAppearanceAttributes textAppearanceAttributes) {
        if (textAppearanceAttributes.mTextColor != null) {
            setTextColor(textAppearanceAttributes.mTextColor);
        }
        if (textAppearanceAttributes.mTextColorHint != null) {
            setHintTextColor(textAppearanceAttributes.mTextColorHint);
        }
        if (textAppearanceAttributes.mTextColorLink != null) {
            setLinkTextColor(textAppearanceAttributes.mTextColorLink);
        }
        if (textAppearanceAttributes.mTextColorHighlight != 0) {
            setHighlightColor(textAppearanceAttributes.mTextColorHighlight);
        }
        if (textAppearanceAttributes.mTextSize != 0) {
            setRawTextSize(textAppearanceAttributes.mTextSize, true);
        }
        if (textAppearanceAttributes.mTypefaceIndex != -1 && !textAppearanceAttributes.mFontFamilyExplicit) {
            textAppearanceAttributes.mFontFamily = null;
        }
        setTypefaceFromAttrs(textAppearanceAttributes.mFontTypeface, textAppearanceAttributes.mFontFamily, textAppearanceAttributes.mTypefaceIndex, textAppearanceAttributes.mStyleIndex, textAppearanceAttributes.mFontWeight);
        if (textAppearanceAttributes.mShadowColor != 0) {
            setShadowLayer(textAppearanceAttributes.mShadowRadius, textAppearanceAttributes.mShadowDx, textAppearanceAttributes.mShadowDy, textAppearanceAttributes.mShadowColor);
        }
        if (textAppearanceAttributes.mAllCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
        if (textAppearanceAttributes.mHasElegant) {
            setElegantTextHeight(textAppearanceAttributes.mElegant);
        }
        if (textAppearanceAttributes.mHasFallbackLineSpacing) {
            setFallbackLineSpacing(textAppearanceAttributes.mFallbackLineSpacing);
        }
        if (textAppearanceAttributes.mHasLetterSpacing) {
            setLetterSpacing(textAppearanceAttributes.mLetterSpacing);
        }
        if (textAppearanceAttributes.mFontFeatureSettings != null) {
            setFontFeatureSettings(textAppearanceAttributes.mFontFeatureSettings);
        }
    }

    public Locale getTextLocale() {
        return this.mTextPaint.getTextLocale();
    }

    public LocaleList getTextLocales() {
        return this.mTextPaint.getTextLocales();
    }

    private void changeListenerLocaleTo(Locale locale) {
        KeyListener dateTimeKeyListener;
        if (!this.mListenerChanged && this.mEditor != null) {
            KeyListener keyListener = this.mEditor.mKeyListener;
            if (keyListener instanceof DigitsKeyListener) {
                dateTimeKeyListener = DigitsKeyListener.getInstance(locale, (DigitsKeyListener) keyListener);
            } else if (keyListener instanceof DateKeyListener) {
                dateTimeKeyListener = DateKeyListener.getInstance(locale);
            } else if (keyListener instanceof TimeKeyListener) {
                dateTimeKeyListener = TimeKeyListener.getInstance(locale);
            } else if (keyListener instanceof DateTimeKeyListener) {
                dateTimeKeyListener = DateTimeKeyListener.getInstance(locale);
            } else {
                return;
            }
            boolean zIsPasswordInputType = isPasswordInputType(this.mEditor.mInputType);
            setKeyListenerOnly(dateTimeKeyListener);
            setInputTypeFromEditor();
            if (zIsPasswordInputType) {
                int i = this.mEditor.mInputType & 15;
                if (i == 1) {
                    this.mEditor.mInputType |= 128;
                } else if (i == 2) {
                    this.mEditor.mInputType |= 16;
                }
            }
        }
    }

    public void setTextLocale(Locale locale) {
        this.mLocalesChanged = true;
        this.mTextPaint.setTextLocale(locale);
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public void setTextLocales(LocaleList localeList) {
        this.mLocalesChanged = true;
        this.mTextPaint.setTextLocales(localeList);
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (!this.mLocalesChanged) {
            this.mTextPaint.setTextLocales(LocaleList.getDefault());
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    @ViewDebug.ExportedProperty(category = "text")
    public float getTextSize() {
        return this.mTextPaint.getTextSize();
    }

    @ViewDebug.ExportedProperty(category = "text")
    public float getScaledTextSize() {
        return this.mTextPaint.getTextSize() / this.mTextPaint.density;
    }

    @ViewDebug.ExportedProperty(category = "text", mapping = {@ViewDebug.IntToString(from = 0, to = "NORMAL"), @ViewDebug.IntToString(from = 1, to = "BOLD"), @ViewDebug.IntToString(from = 2, to = "ITALIC"), @ViewDebug.IntToString(from = 3, to = "BOLD_ITALIC")})
    public int getTypefaceStyle() {
        Typeface typeface = this.mTextPaint.getTypeface();
        if (typeface != null) {
            return typeface.getStyle();
        }
        return 0;
    }

    @RemotableViewMethod
    public void setTextSize(float f) {
        setTextSize(2, f);
    }

    public void setTextSize(int i, float f) {
        if (!isAutoSizeEnabled()) {
            setTextSizeInternal(i, f, true);
        }
    }

    private void setTextSizeInternal(int i, float f, boolean z) {
        Resources resources;
        Context context = getContext();
        if (context == null) {
            resources = Resources.getSystem();
        } else {
            resources = context.getResources();
        }
        setRawTextSize(TypedValue.applyDimension(i, f, resources.getDisplayMetrics()), z);
    }

    private void setRawTextSize(float f, boolean z) {
        if (f != this.mTextPaint.getTextSize()) {
            this.mTextPaint.setTextSize(f);
            if (z && this.mLayout != null) {
                this.mNeedsAutoSizeText = false;
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
    public void setTextScaleX(float f) {
        if (f != this.mTextPaint.getTextScaleX()) {
            this.mUserSetTextScaleX = true;
            this.mTextPaint.setTextScaleX(f);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setTypeface(Typeface typeface) {
        if (this.mTextPaint.getTypeface() != typeface) {
            this.mTextPaint.setTypeface(typeface);
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

    public void setElegantTextHeight(boolean z) {
        if (z != this.mTextPaint.isElegantTextHeight()) {
            this.mTextPaint.setElegantTextHeight(z);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setFallbackLineSpacing(boolean z) {
        if (this.mUseFallbackLineSpacing != z) {
            this.mUseFallbackLineSpacing = z;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public boolean isFallbackLineSpacing() {
        return this.mUseFallbackLineSpacing;
    }

    public boolean isElegantTextHeight() {
        return this.mTextPaint.isElegantTextHeight();
    }

    public float getLetterSpacing() {
        return this.mTextPaint.getLetterSpacing();
    }

    @RemotableViewMethod
    public void setLetterSpacing(float f) {
        if (f != this.mTextPaint.getLetterSpacing()) {
            this.mTextPaint.setLetterSpacing(f);
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

    public String getFontVariationSettings() {
        return this.mTextPaint.getFontVariationSettings();
    }

    public void setBreakStrategy(int i) {
        this.mBreakStrategy = i;
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public int getBreakStrategy() {
        return this.mBreakStrategy;
    }

    public void setHyphenationFrequency(int i) {
        this.mHyphenationFrequency = i;
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public int getHyphenationFrequency() {
        return this.mHyphenationFrequency;
    }

    public PrecomputedText.Params getTextMetricsParams() {
        return new PrecomputedText.Params(new TextPaint(this.mTextPaint), getTextDirectionHeuristic(), this.mBreakStrategy, this.mHyphenationFrequency);
    }

    public void setTextMetricsParams(PrecomputedText.Params params) {
        this.mTextPaint.set(params.getTextPaint());
        this.mUserSetTextScaleX = true;
        this.mTextDir = params.getTextDirection();
        this.mBreakStrategy = params.getBreakStrategy();
        this.mHyphenationFrequency = params.getHyphenationFrequency();
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public void setJustificationMode(int i) {
        this.mJustificationMode = i;
        if (this.mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public int getJustificationMode() {
        return this.mJustificationMode;
    }

    @RemotableViewMethod
    public void setFontFeatureSettings(String str) {
        if (str != this.mTextPaint.getFontFeatureSettings()) {
            this.mTextPaint.setFontFeatureSettings(str);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public boolean setFontVariationSettings(String str) {
        String fontVariationSettings = this.mTextPaint.getFontVariationSettings();
        if (str != fontVariationSettings) {
            if (str != null && str.equals(fontVariationSettings)) {
                return true;
            }
            boolean fontVariationSettings2 = this.mTextPaint.setFontVariationSettings(str);
            if (fontVariationSettings2 && this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
            return fontVariationSettings2;
        }
        return true;
    }

    @RemotableViewMethod
    public void setTextColor(int i) {
        this.mTextColor = ColorStateList.valueOf(i);
        updateTextColors();
    }

    @RemotableViewMethod
    public void setTextColor(ColorStateList colorStateList) {
        if (colorStateList == null) {
            throw new NullPointerException();
        }
        this.mTextColor = colorStateList;
        updateTextColors();
    }

    public final ColorStateList getTextColors() {
        return this.mTextColor;
    }

    public final int getCurrentTextColor() {
        return this.mCurTextColor;
    }

    @RemotableViewMethod
    public void setHighlightColor(int i) {
        if (this.mHighlightColor != i) {
            this.mHighlightColor = i;
            invalidate();
        }
    }

    public int getHighlightColor() {
        return this.mHighlightColor;
    }

    @RemotableViewMethod
    public final void setShowSoftInputOnFocus(boolean z) {
        createEditorIfNeeded();
        this.mEditor.mShowSoftInputOnFocus = z;
    }

    public final boolean getShowSoftInputOnFocus() {
        return this.mEditor == null || this.mEditor.mShowSoftInputOnFocus;
    }

    public void setShadowLayer(float f, float f2, float f3, int i) {
        this.mTextPaint.setShadowLayer(f, f2, f3, i);
        this.mShadowRadius = f;
        this.mShadowDx = f2;
        this.mShadowDy = f3;
        this.mShadowColor = i;
        if (this.mEditor != null) {
            this.mEditor.invalidateTextDisplayList();
            this.mEditor.invalidateHandlesAndActionMode();
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
    public final void setAutoLinkMask(int i) {
        this.mAutoLinkMask = i;
    }

    @RemotableViewMethod
    public final void setLinksClickable(boolean z) {
        this.mLinksClickable = z;
    }

    public final boolean getLinksClickable() {
        return this.mLinksClickable;
    }

    public URLSpan[] getUrls() {
        if (this.mText instanceof Spanned) {
            return (URLSpan[]) ((Spanned) this.mText).getSpans(0, this.mText.length(), URLSpan.class);
        }
        return new URLSpan[0];
    }

    @RemotableViewMethod
    public final void setHintTextColor(int i) {
        this.mHintTextColor = ColorStateList.valueOf(i);
        updateTextColors();
    }

    public final void setHintTextColor(ColorStateList colorStateList) {
        this.mHintTextColor = colorStateList;
        updateTextColors();
    }

    public final ColorStateList getHintTextColors() {
        return this.mHintTextColor;
    }

    public final int getCurrentHintTextColor() {
        return this.mHintTextColor != null ? this.mCurHintTextColor : this.mCurTextColor;
    }

    @RemotableViewMethod
    public final void setLinkTextColor(int i) {
        this.mLinkTextColor = ColorStateList.valueOf(i);
        updateTextColors();
    }

    public final void setLinkTextColor(ColorStateList colorStateList) {
        this.mLinkTextColor = colorStateList;
        updateTextColors();
    }

    public final ColorStateList getLinkTextColors() {
        return this.mLinkTextColor;
    }

    public void setGravity(int i) {
        boolean z;
        if ((i & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            i |= Gravity.START;
        }
        if ((i & 112) == 0) {
            i |= 48;
        }
        if ((i & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != (8388615 & this.mGravity)) {
            z = true;
        } else {
            z = false;
        }
        if (i != this.mGravity) {
            invalidate();
        }
        this.mGravity = i;
        if (this.mLayout != null && z) {
            makeNewLayout(this.mLayout.getWidth(), this.mHintLayout != null ? this.mHintLayout.getWidth() : 0, UNKNOWN_BORING, UNKNOWN_BORING, ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight(), true);
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public int getPaintFlags() {
        return this.mTextPaint.getFlags();
    }

    @RemotableViewMethod
    public void setPaintFlags(int i) {
        if (this.mTextPaint.getFlags() != i) {
            this.mTextPaint.setFlags(i);
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setHorizontallyScrolling(boolean z) {
        if (this.mHorizontallyScrolling != z) {
            this.mHorizontallyScrolling = z;
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
    public void setMinLines(int i) {
        this.mMinimum = i;
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
    public void setMinHeight(int i) {
        this.mMinimum = i;
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
    public void setMaxLines(int i) {
        this.mMaximum = i;
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
    public void setMaxHeight(int i) {
        this.mMaximum = i;
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
    public void setLines(int i) {
        this.mMinimum = i;
        this.mMaximum = i;
        this.mMinMode = 1;
        this.mMaxMode = 1;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setHeight(int i) {
        this.mMinimum = i;
        this.mMaximum = i;
        this.mMinMode = 2;
        this.mMaxMode = 2;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setMinEms(int i) {
        this.mMinWidth = i;
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
    public void setMinWidth(int i) {
        this.mMinWidth = i;
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
    public void setMaxEms(int i) {
        this.mMaxWidth = i;
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
    public void setMaxWidth(int i) {
        this.mMaxWidth = i;
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
    public void setEms(int i) {
        this.mMinWidth = i;
        this.mMaxWidth = i;
        this.mMinWidthMode = 1;
        this.mMaxWidthMode = 1;
        requestLayout();
        invalidate();
    }

    @RemotableViewMethod
    public void setWidth(int i) {
        this.mMinWidth = i;
        this.mMaxWidth = i;
        this.mMinWidthMode = 2;
        this.mMaxWidthMode = 2;
        requestLayout();
        invalidate();
    }

    public void setLineSpacing(float f, float f2) {
        if (this.mSpacingAdd != f || this.mSpacingMult != f2) {
            this.mSpacingAdd = f;
            this.mSpacingMult = f2;
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

    public void setLineHeight(int i) {
        Preconditions.checkArgumentNonnegative(i);
        if (i != getPaint().getFontMetricsInt(null)) {
            setLineSpacing(i - r0, 1.0f);
        }
    }

    public final void append(CharSequence charSequence) {
        append(charSequence, 0, charSequence.length());
    }

    public void append(CharSequence charSequence, int i, int i2) {
        if (!(this.mText instanceof Editable)) {
            setText(this.mText, BufferType.EDITABLE);
        }
        ((Editable) this.mText).append(charSequence, i, i2);
        if (this.mAutoLinkMask != 0 && Linkify.addLinks(this.mSpannable, this.mAutoLinkMask) && this.mLinksClickable && !textCanBeSelected()) {
            setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void updateTextColors() {
        boolean z;
        int colorForState;
        int colorForState2;
        int[] drawableState = getDrawableState();
        int colorForState3 = this.mTextColor.getColorForState(drawableState, 0);
        if (colorForState3 != this.mCurTextColor) {
            this.mCurTextColor = colorForState3;
            z = true;
        } else {
            z = false;
        }
        if (this.mLinkTextColor != null && (colorForState2 = this.mLinkTextColor.getColorForState(drawableState, 0)) != this.mTextPaint.linkColor) {
            this.mTextPaint.linkColor = colorForState2;
            z = true;
        }
        if (this.mHintTextColor != null && (colorForState = this.mHintTextColor.getColorForState(drawableState, 0)) != this.mCurHintTextColor) {
            this.mCurHintTextColor = colorForState;
            if (this.mText.length() == 0) {
                z = true;
            }
        }
        if (z) {
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
        if (this.mDrawables != null) {
            int[] drawableState = getDrawableState();
            for (Drawable drawable : this.mDrawables.mShowing) {
                if (drawable != null && drawable.isStateful() && drawable.setState(drawableState)) {
                    invalidateDrawable(drawable);
                }
            }
        }
    }

    @Override
    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        if (this.mDrawables != null) {
            for (Drawable drawable : this.mDrawables.mShowing) {
                if (drawable != null) {
                    drawable.setHotspot(f, f2);
                }
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        int selectionEnd;
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        boolean freezesText = getFreezesText();
        int selectionStart = -1;
        boolean z = false;
        if (this.mText != null) {
            selectionStart = getSelectionStart();
            selectionEnd = getSelectionEnd();
            if (selectionStart >= 0 || selectionEnd >= 0) {
                z = true;
            }
        } else {
            selectionEnd = -1;
        }
        if (freezesText || z) {
            SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
            if (freezesText) {
                if (this.mText instanceof Spanned) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(this.mText);
                    if (this.mEditor != null) {
                        removeMisspelledSpans(spannableStringBuilder);
                        spannableStringBuilder.removeSpan(this.mEditor.mSuggestionRangeSpan);
                    }
                    savedState.text = spannableStringBuilder;
                } else {
                    savedState.text = this.mText.toString();
                }
            }
            if (z) {
                savedState.selStart = selectionStart;
                savedState.selEnd = selectionEnd;
            }
            if (isFocused() && selectionStart >= 0 && selectionEnd >= 0) {
                savedState.frozenWithFocus = true;
            }
            savedState.error = getError();
            if (this.mEditor != null) {
                savedState.editorState = this.mEditor.saveInstanceState();
            }
            return savedState;
        }
        return parcelableOnSaveInstanceState;
    }

    void removeMisspelledSpans(Spannable spannable) {
        SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) spannable.getSpans(0, spannable.length(), SuggestionSpan.class);
        for (int i = 0; i < suggestionSpanArr.length; i++) {
            int flags = suggestionSpanArr[i].getFlags();
            if ((flags & 1) != 0 && (flags & 2) != 0) {
                spannable.removeSpan(suggestionSpanArr[i]);
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.text != null) {
            setText(savedState.text);
        }
        if (savedState.selStart >= 0 && savedState.selEnd >= 0 && this.mSpannable != null) {
            int length = this.mText.length();
            if (savedState.selStart > length || savedState.selEnd > length) {
                String str = "";
                if (savedState.text != null) {
                    str = "(restored) ";
                }
                Log.e(LOG_TAG, "Saved cursor position " + savedState.selStart + "/" + savedState.selEnd + " out of range for " + str + "text " + ((Object) this.mText));
            } else {
                Selection.setSelection(this.mSpannable, savedState.selStart, savedState.selEnd);
                if (savedState.frozenWithFocus) {
                    createEditorIfNeeded();
                    this.mEditor.mFrozenWithFocus = true;
                }
            }
        }
        if (savedState.error != null) {
            final CharSequence charSequence = savedState.error;
            post(new Runnable() {
                @Override
                public void run() {
                    if (TextView.this.mEditor == null || !TextView.this.mEditor.mErrorWasChanged) {
                        TextView.this.setError(charSequence);
                    }
                }
            });
        }
        if (savedState.editorState != null) {
            createEditorIfNeeded();
            this.mEditor.restoreInstanceState(savedState.editorState);
        }
    }

    @RemotableViewMethod
    public void setFreezesText(boolean z) {
        this.mFreezesText = z;
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
    public final void setText(CharSequence charSequence) {
        setText(charSequence, this.mBufferType);
    }

    @RemotableViewMethod
    public final void setTextKeepState(CharSequence charSequence) {
        setTextKeepState(charSequence, this.mBufferType);
    }

    public void setText(CharSequence charSequence, BufferType bufferType) {
        setText(charSequence, bufferType, true, 0);
        if (this.mCharWrapper == null) {
            return;
        }
        this.mCharWrapper.mChars = null;
    }

    private void setText(CharSequence charSequence, BufferType bufferType, boolean z, int i) {
        CharSequence charSequenceNewSpannable;
        Spannable spannableNewSpannable;
        this.mTextSetFromXmlOrResourceId = false;
        if (charSequence == null) {
            charSequence = "";
        }
        if (!isSuggestionsEnabled()) {
            charSequence = removeSuggestionSpans(charSequence);
        }
        if (!this.mUserSetTextScaleX) {
            this.mTextPaint.setTextScaleX(1.0f);
        }
        if ((charSequence instanceof Spanned) && ((Spanned) charSequence).getSpanStart(TextUtils.TruncateAt.MARQUEE) >= 0) {
            if (ViewConfiguration.get(this.mContext).isFadingMarqueeEnabled()) {
                setHorizontalFadingEdgeEnabled(true);
                this.mMarqueeFadeMode = 0;
            } else {
                setHorizontalFadingEdgeEnabled(false);
                this.mMarqueeFadeMode = 1;
            }
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        }
        int length = this.mFilters.length;
        CharSequence charSequence2 = charSequence;
        int i2 = 0;
        while (i2 < length) {
            CharSequence charSequenceFilter = this.mFilters[i2].filter(charSequence2, 0, charSequence2.length(), EMPTY_SPANNED, 0, 0);
            if (charSequenceFilter != null) {
                charSequence2 = charSequenceFilter;
            }
            i2++;
            charSequence2 = charSequence2;
        }
        if (z) {
            if (this.mText == null) {
                sendBeforeTextChanged("", 0, 0, charSequence2.length());
            } else {
                i = this.mText.length();
                sendBeforeTextChanged(this.mText, 0, i, charSequence2.length());
            }
        }
        boolean z2 = (this.mListeners == null || this.mListeners.size() == 0) ? false : true;
        PrecomputedText precomputedText = charSequence2 instanceof PrecomputedText ? (PrecomputedText) charSequence2 : null;
        if (bufferType == BufferType.EDITABLE || getKeyListener() != null || z2) {
            createEditorIfNeeded();
            this.mEditor.forgetUndoRedo();
            Editable editableNewEditable = this.mEditableFactory.newEditable(charSequence2);
            setFilters(editableNewEditable, this.mFilters);
            InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
            charSequenceNewSpannable = editableNewEditable;
            if (inputMethodManagerPeekInstance != null) {
                inputMethodManagerPeekInstance.restartInput(this);
                charSequenceNewSpannable = editableNewEditable;
            }
        } else if (precomputedText != null) {
            if (this.mTextDir == null) {
                this.mTextDir = getTextDirectionHeuristic();
            }
            charSequenceNewSpannable = charSequence2;
            if (!precomputedText.getParams().isSameTextMetricsInternal(getPaint(), this.mTextDir, this.mBreakStrategy, this.mHyphenationFrequency)) {
                throw new IllegalArgumentException("PrecomputedText's Parameters don't match the parameters of this TextView.Consider using setTextMetricsParams(precomputedText.getParams()) to override the settings of this TextView: PrecomputedText: " + precomputedText.getParams() + "TextView: " + getTextMetricsParams());
            }
        } else if (bufferType == BufferType.SPANNABLE || this.mMovement != null) {
            charSequenceNewSpannable = this.mSpannableFactory.newSpannable(charSequence2);
        } else {
            boolean z3 = charSequence2 instanceof CharWrapper;
            charSequenceNewSpannable = charSequence2;
            if (!z3) {
                charSequenceNewSpannable = TextUtils.stringOrSpannedString(charSequence2);
            }
        }
        CharSequence charSequence3 = charSequenceNewSpannable;
        if (this.mAutoLinkMask != 0) {
            if (bufferType == BufferType.EDITABLE || (charSequenceNewSpannable instanceof Spannable)) {
                spannableNewSpannable = (Spannable) charSequenceNewSpannable;
            } else {
                spannableNewSpannable = this.mSpannableFactory.newSpannable(charSequenceNewSpannable);
            }
            charSequence3 = charSequenceNewSpannable;
            if (Linkify.addLinks(spannableNewSpannable, this.mAutoLinkMask)) {
                bufferType = bufferType == BufferType.EDITABLE ? BufferType.EDITABLE : BufferType.SPANNABLE;
                setTextInternal(spannableNewSpannable);
                if (this.mLinksClickable && !textCanBeSelected()) {
                    setMovementMethod(LinkMovementMethod.getInstance());
                }
                charSequence3 = spannableNewSpannable;
            }
        }
        this.mBufferType = bufferType;
        setTextInternal(charSequence3);
        if (this.mTransformation == null) {
            this.mTransformed = charSequence3;
        } else {
            this.mTransformed = this.mTransformation.getTransformation(charSequence3, this);
        }
        int length2 = charSequence3.length();
        if ((charSequence3 instanceof Spannable) && !this.mAllowTransformationLengthChange) {
            Spannable spannable = (Spannable) charSequence3;
            for (ChangeWatcher changeWatcher : (ChangeWatcher[]) spannable.getSpans(0, spannable.length(), ChangeWatcher.class)) {
                spannable.removeSpan(changeWatcher);
            }
            if (this.mChangeWatcher == null) {
                this.mChangeWatcher = new ChangeWatcher();
            }
            spannable.setSpan(this.mChangeWatcher, 0, length2, 6553618);
            if (this.mEditor != null) {
                this.mEditor.addSpanWatchers(spannable);
            }
            if (this.mTransformation != null) {
                spannable.setSpan(this.mTransformation, 0, length2, 18);
            }
            if (this.mMovement != null) {
                this.mMovement.initialize(this, spannable);
                if (this.mEditor != null) {
                    this.mEditor.mSelectionMoved = false;
                }
            }
        }
        if (this.mLayout != null) {
            checkForRelayout();
        }
        sendOnTextChanged(charSequence3, 0, i, length2);
        onTextChanged(charSequence3, 0, i, length2);
        notifyViewAccessibilityStateChangedIfNeeded(2);
        if (z2) {
            sendAfterTextChanged((Editable) charSequence3);
        } else {
            notifyAutoFillManagerAfterTextChangedIfNeeded();
        }
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
    }

    public final void setText(char[] cArr, int i, int i2) {
        int length;
        if (i < 0 || i2 < 0 || i + i2 > cArr.length) {
            throw new IndexOutOfBoundsException(i + ", " + i2);
        }
        if (this.mText != null) {
            length = this.mText.length();
            sendBeforeTextChanged(this.mText, 0, length, i2);
        } else {
            sendBeforeTextChanged("", 0, 0, i2);
            length = 0;
        }
        if (this.mCharWrapper == null) {
            this.mCharWrapper = new CharWrapper(cArr, i, i2);
        } else {
            this.mCharWrapper.set(cArr, i, i2);
        }
        setText(this.mCharWrapper, this.mBufferType, false, length);
    }

    public final void setTextKeepState(CharSequence charSequence, BufferType bufferType) {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        int length = charSequence.length();
        setText(charSequence, bufferType);
        if ((selectionStart >= 0 || selectionEnd >= 0) && this.mSpannable != null) {
            Selection.setSelection(this.mSpannable, Math.max(0, Math.min(selectionStart, length)), Math.max(0, Math.min(selectionEnd, length)));
        }
    }

    @RemotableViewMethod
    public final void setText(int i) {
        setText(getContext().getResources().getText(i));
        this.mTextSetFromXmlOrResourceId = true;
        this.mTextId = i;
    }

    public final void setText(int i, BufferType bufferType) {
        setText(getContext().getResources().getText(i), bufferType);
        this.mTextSetFromXmlOrResourceId = true;
        this.mTextId = i;
    }

    @RemotableViewMethod
    public final void setHint(CharSequence charSequence) {
        setHintInternal(charSequence);
        if (this.mEditor != null && isInputMethodTarget()) {
            this.mEditor.reportExtractedText();
        }
    }

    private void setHintInternal(CharSequence charSequence) {
        this.mHint = TextUtils.stringOrSpannedString(charSequence);
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
    public final void setHint(int i) {
        setHint(getContext().getResources().getText(i));
    }

    @ViewDebug.CapturedViewProperty
    public CharSequence getHint() {
        return this.mHint;
    }

    boolean isSingleLine() {
        return this.mSingleLine;
    }

    private static boolean isMultilineInputType(int i) {
        return (i & 131087) == 131073;
    }

    CharSequence removeSuggestionSpans(CharSequence charSequence) {
        Spannable spannableNewSpannable;
        if (!(charSequence instanceof Spanned)) {
            return charSequence;
        }
        if (charSequence instanceof Spannable) {
            spannableNewSpannable = (Spannable) charSequence;
        } else {
            spannableNewSpannable = this.mSpannableFactory.newSpannable(charSequence);
        }
        SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) spannableNewSpannable.getSpans(0, charSequence.length(), SuggestionSpan.class);
        if (suggestionSpanArr.length == 0) {
            return charSequence;
        }
        for (SuggestionSpan suggestionSpan : suggestionSpanArr) {
            spannableNewSpannable.removeSpan(suggestionSpan);
        }
        return spannableNewSpannable;
    }

    public void setInputType(int i) {
        boolean zIsPasswordInputType = isPasswordInputType(getInputType());
        boolean zIsVisiblePasswordInputType = isVisiblePasswordInputType(getInputType());
        boolean z = false;
        setInputType(i, false);
        boolean zIsPasswordInputType2 = isPasswordInputType(i);
        boolean zIsVisiblePasswordInputType2 = isVisiblePasswordInputType(i);
        if (zIsPasswordInputType2) {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            setTypefaceFromAttrs(null, null, 3, 0, -1);
        } else if (zIsVisiblePasswordInputType2) {
            if (this.mTransformation == PasswordTransformationMethod.getInstance()) {
                z = true;
            }
            setTypefaceFromAttrs(null, null, 3, 0, -1);
        } else if (zIsPasswordInputType || zIsVisiblePasswordInputType) {
            setTypefaceFromAttrs(null, null, -1, 0, -1);
            if (this.mTransformation == PasswordTransformationMethod.getInstance()) {
                z = true;
            }
        }
        boolean z2 = !isMultilineInputType(i);
        if (this.mSingleLine != z2 || z) {
            applySingleLine(z2, !zIsPasswordInputType2, true);
        }
        if (!isSuggestionsEnabled()) {
            setTextInternal(removeSuggestionSpans(this.mText));
        }
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            inputMethodManagerPeekInstance.restartInput(this);
        }
    }

    boolean hasPasswordTransformationMethod() {
        return this.mTransformation instanceof PasswordTransformationMethod;
    }

    static boolean isPasswordInputType(int i) {
        int i2 = i & FileObserver.ALL_EVENTS;
        return i2 == 129 || i2 == 225 || i2 == 18;
    }

    private static boolean isVisiblePasswordInputType(int i) {
        return (i & FileObserver.ALL_EVENTS) == 145;
    }

    public void setRawInputType(int i) {
        if (i == 0 && this.mEditor == null) {
            return;
        }
        createEditorIfNeeded();
        this.mEditor.mInputType = i;
    }

    private Locale getCustomLocaleForKeyListenerOrNull() {
        LocaleList imeHintLocales;
        if (this.mUseInternationalizedInput && (imeHintLocales = getImeHintLocales()) != null) {
            return imeHintLocales.get(0);
        }
        return null;
    }

    private void setInputType(int i, boolean z) {
        KeyListener textKeyListener;
        TextKeyListener.Capitalize capitalize;
        int i2 = i & 15;
        if (i2 == 1) {
            boolean z2 = (32768 & i) != 0;
            if ((i & 4096) != 0) {
                capitalize = TextKeyListener.Capitalize.CHARACTERS;
            } else if ((i & 8192) != 0) {
                capitalize = TextKeyListener.Capitalize.WORDS;
            } else if ((i & 16384) != 0) {
                capitalize = TextKeyListener.Capitalize.SENTENCES;
            } else {
                capitalize = TextKeyListener.Capitalize.NONE;
            }
            textKeyListener = TextKeyListener.getInstance(z2, capitalize);
        } else if (i2 == 2) {
            Locale customLocaleForKeyListenerOrNull = getCustomLocaleForKeyListenerOrNull();
            DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(customLocaleForKeyListenerOrNull, (i & 4096) != 0, (i & 8192) != 0);
            if (customLocaleForKeyListenerOrNull != null) {
                int inputType = digitsKeyListener.getInputType();
                if ((inputType & 15) != 2) {
                    if ((i & 16) != 0) {
                        inputType |= 128;
                    }
                    i = inputType;
                }
            }
            textKeyListener = digitsKeyListener;
        } else if (i2 == 4) {
            Locale customLocaleForKeyListenerOrNull2 = getCustomLocaleForKeyListenerOrNull();
            int i3 = i & InputType.TYPE_MASK_VARIATION;
            if (i3 == 16) {
                textKeyListener = DateKeyListener.getInstance(customLocaleForKeyListenerOrNull2);
            } else if (i3 == 32) {
                textKeyListener = TimeKeyListener.getInstance(customLocaleForKeyListenerOrNull2);
            } else {
                textKeyListener = DateTimeKeyListener.getInstance(customLocaleForKeyListenerOrNull2);
            }
            if (this.mUseInternationalizedInput) {
                i = textKeyListener.getInputType();
            }
        } else if (i2 == 3) {
            textKeyListener = DialerKeyListener.getInstance();
        } else {
            textKeyListener = TextKeyListener.getInstance();
        }
        setRawInputType(i);
        this.mListenerChanged = false;
        if (z) {
            createEditorIfNeeded();
            this.mEditor.mKeyListener = textKeyListener;
        } else {
            setKeyListenerOnly(textKeyListener);
        }
    }

    public int getInputType() {
        if (this.mEditor == null) {
            return 0;
        }
        return this.mEditor.mInputType;
    }

    public void setImeOptions(int i) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.imeOptions = i;
    }

    public int getImeOptions() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return 0;
        }
        return this.mEditor.mInputContentType.imeOptions;
    }

    public void setImeActionLabel(CharSequence charSequence, int i) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.imeActionLabel = charSequence;
        this.mEditor.mInputContentType.imeActionId = i;
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

    public void setOnEditorActionListener(OnEditorActionListener onEditorActionListener) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.onEditorActionListener = onEditorActionListener;
    }

    public void onEditorAction(int i) {
        Editor.InputContentType inputContentType;
        if (this.mEditor != null) {
            inputContentType = this.mEditor.mInputContentType;
        } else {
            inputContentType = null;
        }
        if (inputContentType != null) {
            if (inputContentType.onEditorActionListener == null || !inputContentType.onEditorActionListener.onEditorAction(this, i, null)) {
                if (i != 5) {
                    if (i != 7) {
                        if (i == 6) {
                            InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                            if (inputMethodManagerPeekInstance != null && inputMethodManagerPeekInstance.isActive(this)) {
                                inputMethodManagerPeekInstance.hideSoftInputFromWindow(getWindowToken(), 0);
                                return;
                            }
                            return;
                        }
                    } else {
                        View viewFocusSearch = focusSearch(1);
                        if (viewFocusSearch != null && !viewFocusSearch.requestFocus(1)) {
                            throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                        }
                        return;
                    }
                } else {
                    View viewFocusSearch2 = focusSearch(2);
                    if (viewFocusSearch2 != null && !viewFocusSearch2.requestFocus(2)) {
                        throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                    }
                    return;
                }
            } else {
                return;
            }
        }
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            viewRootImpl.dispatchKeyFromIme(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, 66, 0, 0, -1, 0, 22));
            viewRootImpl.dispatchKeyFromIme(new KeyEvent(SystemClock.uptimeMillis(), jUptimeMillis, 1, 66, 0, 0, -1, 0, 22));
        }
    }

    public void setPrivateImeOptions(String str) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.privateImeOptions = str;
    }

    public String getPrivateImeOptions() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return null;
        }
        return this.mEditor.mInputContentType.privateImeOptions;
    }

    public void setInputExtras(int i) throws XmlPullParserException, IOException {
        createEditorIfNeeded();
        XmlResourceParser xml = getResources().getXml(i);
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.extras = new Bundle();
        getResources().parseBundleExtras(xml, this.mEditor.mInputContentType.extras);
    }

    public Bundle getInputExtras(boolean z) {
        if (this.mEditor == null && !z) {
            return null;
        }
        createEditorIfNeeded();
        if (this.mEditor.mInputContentType == null) {
            if (!z) {
                return null;
            }
            this.mEditor.createInputContentTypeIfNeeded();
        }
        if (this.mEditor.mInputContentType.extras == null) {
            if (!z) {
                return null;
            }
            this.mEditor.mInputContentType.extras = new Bundle();
        }
        return this.mEditor.mInputContentType.extras;
    }

    public void setImeHintLocales(LocaleList localeList) {
        createEditorIfNeeded();
        this.mEditor.createInputContentTypeIfNeeded();
        this.mEditor.mInputContentType.imeHintLocales = localeList;
        if (this.mUseInternationalizedInput) {
            changeListenerLocaleTo(localeList == null ? null : localeList.get(0));
        }
    }

    public LocaleList getImeHintLocales() {
        if (this.mEditor == null || this.mEditor.mInputContentType == null) {
            return null;
        }
        return this.mEditor.mInputContentType.imeHintLocales;
    }

    public CharSequence getError() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mError;
    }

    @RemotableViewMethod
    public void setError(CharSequence charSequence) {
        if (charSequence == null) {
            setError(null, null);
            return;
        }
        Drawable drawable = getContext().getDrawable(R.drawable.indicator_input_error);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        setError(charSequence, drawable);
    }

    public void setError(CharSequence charSequence, Drawable drawable) {
        createEditorIfNeeded();
        this.mEditor.setError(charSequence, drawable);
        notifyViewAccessibilityStateChangedIfNeeded(0);
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        if (this.mEditor != null) {
            this.mEditor.setFrame();
        }
        restartMarqueeIfNeeded();
        return frame;
    }

    private void restartMarqueeIfNeeded() {
        if (this.mRestartMarquee && this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            this.mRestartMarquee = false;
            startMarquee();
        }
    }

    public void setFilters(InputFilter[] inputFilterArr) {
        if (inputFilterArr == null) {
            throw new IllegalArgumentException();
        }
        this.mFilters = inputFilterArr;
        if (this.mText instanceof Editable) {
            setFilters((Editable) this.mText, inputFilterArr);
        }
    }

    private void setFilters(Editable editable, InputFilter[] inputFilterArr) {
        if (this.mEditor != null) {
            int i = 1;
            boolean z = this.mEditor.mUndoInputFilter != null;
            boolean z2 = this.mEditor.mKeyListener instanceof InputFilter;
            int i2 = z ? 1 : 0;
            if (z2) {
                i2++;
            }
            if (i2 > 0) {
                InputFilter[] inputFilterArr2 = new InputFilter[inputFilterArr.length + i2];
                System.arraycopy(inputFilterArr, 0, inputFilterArr2, 0, inputFilterArr.length);
                if (z) {
                    inputFilterArr2[inputFilterArr.length] = this.mEditor.mUndoInputFilter;
                } else {
                    i = 0;
                }
                if (z2) {
                    inputFilterArr2[inputFilterArr.length + i] = (InputFilter) this.mEditor.mKeyListener;
                }
                editable.setFilters(inputFilterArr2);
                return;
            }
        }
        editable.setFilters(inputFilterArr);
    }

    public InputFilter[] getFilters() {
        return this.mFilters;
    }

    private int getBoxHeight(Layout layout) {
        int extendedPaddingTop;
        Insets opticalInsets = isLayoutModeOptical(this.mParent) ? getOpticalInsets() : Insets.NONE;
        if (layout == this.mHintLayout) {
            extendedPaddingTop = getCompoundPaddingTop() + getCompoundPaddingBottom();
        } else {
            extendedPaddingTop = getExtendedPaddingTop() + getExtendedPaddingBottom();
        }
        return (getMeasuredHeight() - extendedPaddingTop) + opticalInsets.top + opticalInsets.bottom;
    }

    int getVerticalOffset(boolean z) {
        int boxHeight;
        int height;
        int i = this.mGravity & 112;
        Layout layout = this.mLayout;
        if (!z && this.mText.length() == 0 && this.mHintLayout != null) {
            layout = this.mHintLayout;
        }
        if (i != 48 && (height = layout.getHeight()) < (boxHeight = getBoxHeight(layout))) {
            if (i == 80) {
                return boxHeight - height;
            }
            return (boxHeight - height) >> 1;
        }
        return 0;
    }

    private int getBottomVerticalOffset(boolean z) {
        int boxHeight;
        int height;
        int i = this.mGravity & 112;
        Layout layout = this.mLayout;
        if (!z && this.mText.length() == 0 && this.mHintLayout != null) {
            layout = this.mHintLayout;
        }
        if (i != 80 && (height = layout.getHeight()) < (boxHeight = getBoxHeight(layout))) {
            if (i == 48) {
                return boxHeight - height;
            }
            return (boxHeight - height) >> 1;
        }
        return 0;
    }

    void invalidateCursorPath() {
        if (this.mHighlightPathBogus) {
            invalidateCursor();
            return;
        }
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int extendedPaddingTop = getExtendedPaddingTop() + getVerticalOffset(true);
        if (this.mEditor.mDrawableForCursor == null) {
            synchronized (TEMP_RECTF) {
                float fCeil = (float) Math.ceil(this.mTextPaint.getStrokeWidth());
                if (fCeil < 1.0f) {
                    fCeil = 1.0f;
                }
                float f = fCeil / 2.0f;
                this.mHighlightPath.computeBounds(TEMP_RECTF, false);
                float f2 = compoundPaddingLeft;
                float f3 = extendedPaddingTop;
                invalidate((int) Math.floor((TEMP_RECTF.left + f2) - f), (int) Math.floor((TEMP_RECTF.top + f3) - f), (int) Math.ceil(f2 + TEMP_RECTF.right + f), (int) Math.ceil(f3 + TEMP_RECTF.bottom + f));
            }
            return;
        }
        Rect bounds = this.mEditor.mDrawableForCursor.getBounds();
        invalidate(bounds.left + compoundPaddingLeft, bounds.top + extendedPaddingTop, bounds.right + compoundPaddingLeft, bounds.bottom + extendedPaddingTop);
    }

    void invalidateCursor() {
        int selectionEnd = getSelectionEnd();
        invalidateCursor(selectionEnd, selectionEnd, selectionEnd);
    }

    private void invalidateCursor(int i, int i2, int i3) {
        if (i >= 0 || i2 >= 0 || i3 >= 0) {
            invalidateRegion(Math.min(Math.min(i, i2), i3), Math.max(Math.max(i, i2), i3), true);
        }
    }

    void invalidateRegion(int i, int i2, boolean z) {
        int lineForOffset;
        int width;
        int primaryHorizontal;
        if (this.mLayout == null) {
            invalidate();
            return;
        }
        int lineForOffset2 = this.mLayout.getLineForOffset(i);
        int lineTop = this.mLayout.getLineTop(lineForOffset2);
        if (lineForOffset2 > 0) {
            lineTop -= this.mLayout.getLineDescent(lineForOffset2 - 1);
        }
        if (i != i2) {
            lineForOffset = this.mLayout.getLineForOffset(i2);
        } else {
            lineForOffset = lineForOffset2;
        }
        int lineBottom = this.mLayout.getLineBottom(lineForOffset);
        if (z && this.mEditor != null && this.mEditor.mDrawableForCursor != null) {
            Rect bounds = this.mEditor.mDrawableForCursor.getBounds();
            lineTop = Math.min(lineTop, bounds.top);
            lineBottom = Math.max(lineBottom, bounds.bottom);
        }
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int extendedPaddingTop = getExtendedPaddingTop() + getVerticalOffset(true);
        if (lineForOffset2 == lineForOffset && !z) {
            primaryHorizontal = ((int) this.mLayout.getPrimaryHorizontal(i)) + compoundPaddingLeft;
            width = ((int) (((double) this.mLayout.getPrimaryHorizontal(i2)) + 1.0d)) + compoundPaddingLeft;
        } else {
            width = getWidth() - getCompoundPaddingRight();
            primaryHorizontal = compoundPaddingLeft;
        }
        invalidate(this.mScrollX + primaryHorizontal, lineTop + extendedPaddingTop, this.mScrollX + width, extendedPaddingTop + lineBottom);
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
            int selectionEnd = getSelectionEnd();
            if (this.mEditor != null && this.mEditor.mSelectionModifierCursorController != null && this.mEditor.mSelectionModifierCursorController.isSelectionStartDragged()) {
                selectionEnd = getSelectionStart();
            }
            if (selectionEnd < 0 && (this.mGravity & 112) == 80) {
                selectionEnd = this.mText.length();
            }
            if (selectionEnd >= 0) {
                bringPointIntoView(selectionEnd);
            }
        } else {
            bringTextIntoView();
        }
        if (this.mEditor != null && this.mEditor.mCreatedWithASelection) {
            this.mEditor.refreshTextActionMode();
            this.mEditor.mCreatedWithASelection = false;
        }
        unregisterForPreDraw();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
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
    public void onScreenStateChanged(int i) {
        super.onScreenStateChanged(i);
        if (this.mEditor != null) {
            this.mEditor.onScreenStateChanged(i);
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

    @Override
    protected int getRightPaddingOffset() {
        return (-(getCompoundPaddingRight() - this.mPaddingRight)) + ((int) Math.max(0.0f, this.mShadowDx + this.mShadowRadius));
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        boolean zVerifyDrawable = super.verifyDrawable(drawable);
        if (!zVerifyDrawable && this.mDrawables != null) {
            for (Drawable drawable2 : this.mDrawables.mShowing) {
                if (drawable == drawable2) {
                    return true;
                }
            }
        }
        return zVerifyDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mDrawables != null) {
            for (Drawable drawable : this.mDrawables.mShowing) {
                if (drawable != null) {
                    drawable.jumpToCurrentState();
                }
            }
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        boolean z = false;
        if (verifyDrawable(drawable)) {
            Rect bounds = drawable.getBounds();
            int compoundPaddingRight = this.mScrollX;
            int i = this.mScrollY;
            Drawables drawables = this.mDrawables;
            if (drawables != null) {
                if (drawable == drawables.mShowing[0]) {
                    int compoundPaddingTop = getCompoundPaddingTop();
                    int compoundPaddingBottom = ((this.mBottom - this.mTop) - getCompoundPaddingBottom()) - compoundPaddingTop;
                    compoundPaddingRight += this.mPaddingLeft;
                    i += compoundPaddingTop + ((compoundPaddingBottom - drawables.mDrawableHeightLeft) / 2);
                } else if (drawable != drawables.mShowing[2]) {
                    if (drawable == drawables.mShowing[1]) {
                        int compoundPaddingLeft = getCompoundPaddingLeft();
                        compoundPaddingRight += compoundPaddingLeft + (((((this.mRight - this.mLeft) - getCompoundPaddingRight()) - compoundPaddingLeft) - drawables.mDrawableWidthTop) / 2);
                        i += this.mPaddingTop;
                    } else if (drawable == drawables.mShowing[3]) {
                        int compoundPaddingLeft2 = getCompoundPaddingLeft();
                        compoundPaddingRight += compoundPaddingLeft2 + (((((this.mRight - this.mLeft) - getCompoundPaddingRight()) - compoundPaddingLeft2) - drawables.mDrawableWidthBottom) / 2);
                        i += ((this.mBottom - this.mTop) - this.mPaddingBottom) - drawables.mDrawableSizeBottom;
                    }
                } else {
                    int compoundPaddingTop2 = getCompoundPaddingTop();
                    int compoundPaddingBottom2 = ((this.mBottom - this.mTop) - getCompoundPaddingBottom()) - compoundPaddingTop2;
                    compoundPaddingRight += ((this.mRight - this.mLeft) - this.mPaddingRight) - drawables.mDrawableSizeRight;
                    i += compoundPaddingTop2 + ((compoundPaddingBottom2 - drawables.mDrawableHeightRight) / 2);
                }
                z = true;
            }
            if (z) {
                invalidate(bounds.left + compoundPaddingRight, bounds.top + i, bounds.right + compoundPaddingRight, bounds.bottom + i);
            }
        }
        if (!z) {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return !(getBackground() == null || getBackground().getCurrent() == null) || this.mSpannable != null || hasSelection() || isHorizontalFadingEdgeEnabled();
    }

    public boolean isTextSelectable() {
        if (this.mEditor == null) {
            return false;
        }
        return this.mEditor.mTextIsSelectable;
    }

    public void setTextIsSelectable(boolean z) {
        if (z || this.mEditor != null) {
            createEditorIfNeeded();
            if (this.mEditor.mTextIsSelectable == z) {
                return;
            }
            this.mEditor.mTextIsSelectable = z;
            setFocusableInTouchMode(z);
            setFocusable(16);
            setClickable(z);
            setLongClickable(z);
            setMovementMethod(z ? ArrowKeyMovementMethod.getInstance() : null);
            setText(this.mText, z ? BufferType.SPANNABLE : BufferType.NORMAL);
            this.mEditor.prepareCursorControllers();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState;
        if (this.mSingleLine) {
            iArrOnCreateDrawableState = super.onCreateDrawableState(i);
        } else {
            iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
            mergeDrawableStates(iArrOnCreateDrawableState, MULTILINE_STATE_SET);
        }
        if (isTextSelectable()) {
            int length = iArrOnCreateDrawableState.length;
            for (int i2 = 0; i2 < length; i2++) {
                if (iArrOnCreateDrawableState[i2] == 16842919) {
                    int[] iArr = new int[length - 1];
                    System.arraycopy(iArrOnCreateDrawableState, 0, iArr, 0, i2);
                    System.arraycopy(iArrOnCreateDrawableState, i2 + 1, iArr, i2, (length - i2) - 1);
                    return iArr;
                }
            }
        }
        return iArrOnCreateDrawableState;
    }

    private Path getUpdatedHighlightPath() {
        Paint paint = this.mHighlightPaint;
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        if (this.mMovement != null && ((isFocused() || isPressed()) && selectionStart >= 0)) {
            if (selectionStart == selectionEnd) {
                if (this.mEditor != null && this.mEditor.shouldRenderCursor()) {
                    if (this.mHighlightPathBogus) {
                        if (this.mHighlightPath == null) {
                            this.mHighlightPath = new Path();
                        }
                        this.mHighlightPath.reset();
                        this.mLayout.getCursorPath(selectionStart, this.mHighlightPath, this.mText);
                        this.mEditor.updateCursorPosition();
                        this.mHighlightPathBogus = false;
                    }
                    paint.setColor(this.mCurTextColor);
                    paint.setStyle(Paint.Style.STROKE);
                    return this.mHighlightPath;
                }
            } else {
                if (this.mHighlightPathBogus) {
                    if (this.mHighlightPath == null) {
                        this.mHighlightPath = new Path();
                    }
                    this.mHighlightPath.reset();
                    this.mLayout.getSelectionPath(selectionStart, selectionEnd, this.mHighlightPath);
                    this.mHighlightPathBogus = false;
                }
                paint.setColor(this.mHighlightColor);
                paint.setStyle(Paint.Style.FILL);
                return this.mHighlightPath;
            }
        }
        return null;
    }

    public int getHorizontalOffsetForDrawables() {
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float fMin;
        int verticalOffset;
        int verticalOffset2;
        restartMarqueeIfNeeded();
        super.onDraw(canvas);
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int compoundPaddingTop = getCompoundPaddingTop();
        int compoundPaddingRight = getCompoundPaddingRight();
        int compoundPaddingBottom = getCompoundPaddingBottom();
        int i = this.mScrollX;
        int i2 = this.mScrollY;
        int i3 = this.mRight;
        int i4 = this.mLeft;
        int i5 = this.mBottom;
        int i6 = this.mTop;
        boolean zIsLayoutRtl = isLayoutRtl();
        int horizontalOffsetForDrawables = getHorizontalOffsetForDrawables();
        int i7 = zIsLayoutRtl ? 0 : horizontalOffsetForDrawables;
        if (!zIsLayoutRtl) {
            horizontalOffsetForDrawables = 0;
        }
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            int i8 = ((i5 - i6) - compoundPaddingBottom) - compoundPaddingTop;
            int i9 = ((i3 - i4) - compoundPaddingRight) - compoundPaddingLeft;
            if (drawables.mShowing[0] != null) {
                canvas.save();
                canvas.translate(this.mPaddingLeft + i + i7, i2 + compoundPaddingTop + ((i8 - drawables.mDrawableHeightLeft) / 2));
                drawables.mShowing[0].draw(canvas);
                canvas.restore();
            }
            if (drawables.mShowing[2] != null) {
                canvas.save();
                canvas.translate(((((i + i3) - i4) - this.mPaddingRight) - drawables.mDrawableSizeRight) - horizontalOffsetForDrawables, i2 + compoundPaddingTop + ((i8 - drawables.mDrawableHeightRight) / 2));
                drawables.mShowing[2].draw(canvas);
                canvas.restore();
            }
            if (drawables.mShowing[1] != null) {
                canvas.save();
                canvas.translate(i + compoundPaddingLeft + ((i9 - drawables.mDrawableWidthTop) / 2), this.mPaddingTop + i2);
                drawables.mShowing[1].draw(canvas);
                canvas.restore();
            }
            if (drawables.mShowing[3] != null) {
                canvas.save();
                canvas.translate(i + compoundPaddingLeft + ((i9 - drawables.mDrawableWidthBottom) / 2), (((i2 + i5) - i6) - this.mPaddingBottom) - drawables.mDrawableSizeBottom);
                drawables.mShowing[3].draw(canvas);
                canvas.restore();
            }
        }
        int i10 = this.mCurTextColor;
        if (this.mLayout == null) {
            assumeLayout();
        }
        Layout layout = this.mLayout;
        if (this.mHint != null && this.mText.length() == 0) {
            if (this.mHintTextColor != null) {
                i10 = this.mCurHintTextColor;
            }
            layout = this.mHintLayout;
        }
        this.mTextPaint.setColor(i10);
        this.mTextPaint.drawableState = getDrawableState();
        canvas.save();
        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();
        int height = this.mLayout.getHeight() - (((this.mBottom - this.mTop) - compoundPaddingBottom) - compoundPaddingTop);
        float fMin2 = compoundPaddingLeft + i;
        if (i2 != 0) {
            fMin = extendedPaddingTop + i2;
        } else {
            fMin = 0.0f;
        }
        float compoundPaddingRight2 = ((i3 - i4) - getCompoundPaddingRight()) + i;
        int i11 = (i5 - i6) + i2;
        if (i2 == height) {
            extendedPaddingBottom = 0;
        }
        float fMax = i11 - extendedPaddingBottom;
        if (this.mShadowRadius != 0.0f) {
            fMin2 += Math.min(0.0f, this.mShadowDx - this.mShadowRadius);
            compoundPaddingRight2 += Math.max(0.0f, this.mShadowDx + this.mShadowRadius);
            fMin += Math.min(0.0f, this.mShadowDy - this.mShadowRadius);
            fMax += Math.max(0.0f, this.mShadowDy + this.mShadowRadius);
        }
        canvas.clipRect(fMin2, fMin, compoundPaddingRight2, fMax);
        if ((this.mGravity & 112) != 48) {
            verticalOffset2 = getVerticalOffset(false);
            verticalOffset = getVerticalOffset(true);
        } else {
            verticalOffset = 0;
            verticalOffset2 = 0;
        }
        canvas.translate(compoundPaddingLeft, extendedPaddingTop + verticalOffset2);
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, getLayoutDirection());
        if (isMarqueeFadeEnabled()) {
            if (!this.mSingleLine && getLineCount() == 1 && canMarquee() && (absoluteGravity & 7) != 3) {
                canvas.translate(layout.getParagraphDirection(0) * (this.mLayout.getLineRight(0) - ((this.mRight - this.mLeft) - (getCompoundPaddingLeft() + getCompoundPaddingRight()))), 0.0f);
            }
            if (this.mMarquee != null && this.mMarquee.isRunning()) {
                canvas.translate(layout.getParagraphDirection(0) * (-this.mMarquee.getScroll()), 0.0f);
            }
        }
        int i12 = verticalOffset - verticalOffset2;
        Path updatedHighlightPath = getUpdatedHighlightPath();
        if (this.mEditor != null) {
            this.mEditor.onDraw(canvas, layout, updatedHighlightPath, this.mHighlightPaint, i12);
        } else {
            layout.draw(canvas, updatedHighlightPath, this.mHighlightPaint, i12);
        }
        if (this.mMarquee != null && this.mMarquee.shouldDrawGhost()) {
            canvas.translate(layout.getParagraphDirection(0) * this.mMarquee.getGhostOffset(), 0.0f);
            layout.draw(canvas, updatedHighlightPath, this.mHighlightPaint, i12);
        }
        canvas.restore();
    }

    @Override
    public void getFocusedRect(Rect rect) {
        if (this.mLayout == null) {
            super.getFocusedRect(rect);
            return;
        }
        int selectionEnd = getSelectionEnd();
        if (selectionEnd < 0) {
            super.getFocusedRect(rect);
            return;
        }
        int selectionStart = getSelectionStart();
        if (selectionStart < 0 || selectionStart >= selectionEnd) {
            int lineForOffset = this.mLayout.getLineForOffset(selectionEnd);
            rect.top = this.mLayout.getLineTop(lineForOffset);
            rect.bottom = this.mLayout.getLineBottom(lineForOffset);
            rect.left = ((int) this.mLayout.getPrimaryHorizontal(selectionEnd)) - 2;
            rect.right = rect.left + 4;
        } else {
            int lineForOffset2 = this.mLayout.getLineForOffset(selectionStart);
            int lineForOffset3 = this.mLayout.getLineForOffset(selectionEnd);
            rect.top = this.mLayout.getLineTop(lineForOffset2);
            rect.bottom = this.mLayout.getLineBottom(lineForOffset3);
            if (lineForOffset2 == lineForOffset3) {
                rect.left = (int) this.mLayout.getPrimaryHorizontal(selectionStart);
                rect.right = (int) this.mLayout.getPrimaryHorizontal(selectionEnd);
            } else {
                if (this.mHighlightPathBogus) {
                    if (this.mHighlightPath == null) {
                        this.mHighlightPath = new Path();
                    }
                    this.mHighlightPath.reset();
                    this.mLayout.getSelectionPath(selectionStart, selectionEnd, this.mHighlightPath);
                    this.mHighlightPathBogus = false;
                }
                synchronized (TEMP_RECTF) {
                    this.mHighlightPath.computeBounds(TEMP_RECTF, true);
                    rect.left = ((int) TEMP_RECTF.left) - 1;
                    rect.right = ((int) TEMP_RECTF.right) + 1;
                }
            }
        }
        int compoundPaddingLeft = getCompoundPaddingLeft();
        int extendedPaddingTop = getExtendedPaddingTop();
        if ((this.mGravity & 112) != 48) {
            extendedPaddingTop += getVerticalOffset(false);
        }
        rect.offset(compoundPaddingLeft, extendedPaddingTop);
        rect.bottom += getExtendedPaddingBottom();
    }

    public int getLineCount() {
        if (this.mLayout != null) {
            return this.mLayout.getLineCount();
        }
        return 0;
    }

    public int getLineBounds(int i, Rect rect) {
        if (this.mLayout == null) {
            if (rect != null) {
                rect.set(0, 0, 0, 0);
            }
            return 0;
        }
        int lineBounds = this.mLayout.getLineBounds(i, rect);
        int extendedPaddingTop = getExtendedPaddingTop();
        if ((this.mGravity & 112) != 48) {
            extendedPaddingTop += getVerticalOffset(true);
        }
        if (rect != null) {
            rect.offset(getCompoundPaddingLeft(), extendedPaddingTop);
        }
        return lineBounds + extendedPaddingTop;
    }

    @Override
    public int getBaseline() {
        if (this.mLayout == null) {
            return super.getBaseline();
        }
        return getBaselineOffset() + this.mLayout.getLineBaseline(0);
    }

    int getBaselineOffset() {
        int verticalOffset;
        if ((this.mGravity & 112) != 48) {
            verticalOffset = getVerticalOffset(true);
        } else {
            verticalOffset = 0;
        }
        if (isLayoutModeOptical(this.mParent)) {
            verticalOffset -= getOpticalInsets().top;
        }
        return getExtendedPaddingTop() + verticalOffset;
    }

    @Override
    protected int getFadeTop(boolean z) {
        if (this.mLayout == null) {
            return 0;
        }
        int verticalOffset = (this.mGravity & 112) != 48 ? getVerticalOffset(true) : 0;
        if (z) {
            verticalOffset += getTopPaddingOffset();
        }
        return getExtendedPaddingTop() + verticalOffset;
    }

    @Override
    protected int getFadeHeight(boolean z) {
        if (this.mLayout != null) {
            return this.mLayout.getHeight();
        }
        return 0;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        if (this.mSpannable != null && this.mLinksClickable) {
            int offsetForPosition = getOffsetForPosition(motionEvent.getX(i), motionEvent.getY(i));
            if (((ClickableSpan[]) this.mSpannable.getSpans(offsetForPosition, offsetForPosition, ClickableSpan.class)).length > 0) {
                return PointerIcon.getSystemIcon(this.mContext, 1002);
            }
        }
        if (isTextSelectable() || isTextEditable()) {
            return PointerIcon.getSystemIcon(this.mContext, 1008);
        }
        return super.onResolvePointerIcon(motionEvent, i);
    }

    @Override
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (i == 4 && handleBackInTextActionModeIfNeeded(keyEvent)) {
            return true;
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    public boolean handleBackInTextActionModeIfNeeded(KeyEvent keyEvent) {
        if (this.mEditor == null || this.mEditor.getTextActionMode() == null) {
            return false;
        }
        if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
            KeyEvent.DispatcherState keyDispatcherState = getKeyDispatcherState();
            if (keyDispatcherState != null) {
                keyDispatcherState.startTracking(keyEvent, this);
            }
            return true;
        }
        if (keyEvent.getAction() == 1) {
            KeyEvent.DispatcherState keyDispatcherState2 = getKeyDispatcherState();
            if (keyDispatcherState2 != null) {
                keyDispatcherState2.handleUpEvent(keyEvent);
            }
            if (keyEvent.isTracking() && !keyEvent.isCanceled()) {
                stopTextActionMode();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (doKeyDown(i, keyEvent, null) == 0) {
            return super.onKeyDown(i, keyEvent);
        }
        return true;
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        KeyEvent keyEventChangeAction = KeyEvent.changeAction(keyEvent, 0);
        int iDoKeyDown = doKeyDown(i, keyEventChangeAction, keyEvent);
        if (iDoKeyDown == 0) {
            return super.onKeyMultiple(i, i2, keyEvent);
        }
        if (iDoKeyDown == -1) {
            return true;
        }
        int i3 = i2 - 1;
        KeyEvent keyEventChangeAction2 = KeyEvent.changeAction(keyEvent, 1);
        if (iDoKeyDown == 1) {
            this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, i, keyEventChangeAction2);
            while (true) {
                i3--;
                if (i3 <= 0) {
                    break;
                }
                this.mEditor.mKeyListener.onKeyDown(this, (Editable) this.mText, i, keyEventChangeAction);
                this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, i, keyEventChangeAction2);
            }
            hideErrorIfUnchanged();
        } else if (iDoKeyDown == 2) {
            this.mMovement.onKeyUp(this, this.mSpannable, i, keyEventChangeAction2);
            while (true) {
                i3--;
                if (i3 <= 0) {
                    break;
                }
                this.mMovement.onKeyDown(this, this.mSpannable, i, keyEventChangeAction);
                this.mMovement.onKeyUp(this, this.mSpannable, i, keyEventChangeAction2);
            }
        }
        return true;
    }

    private boolean shouldAdvanceFocusOnEnter() {
        int i;
        if (getKeyListener() == null) {
            return false;
        }
        if (this.mSingleLine) {
            return true;
        }
        return this.mEditor != null && (this.mEditor.mInputType & 15) == 1 && ((i = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION) == 32 || i == 48);
    }

    private boolean shouldAdvanceFocusOnTab() {
        int i;
        return getKeyListener() == null || this.mSingleLine || this.mEditor == null || (this.mEditor.mInputType & 15) != 1 || !((i = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION) == 262144 || i == 131072);
    }

    private boolean isDirectionalNavigationKey(int i) {
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
                return true;
            default:
                return false;
        }
    }

    private int doKeyDown(int i, KeyEvent keyEvent, KeyEvent keyEvent2) {
        boolean z;
        boolean zOnKeyOther;
        boolean z2;
        if (!isEnabled()) {
            return 0;
        }
        if (keyEvent.getRepeatCount() == 0 && !KeyEvent.isModifierKey(i)) {
            this.mPreventDefaultMovement = false;
        }
        if (i != 4) {
            if (i != 23) {
                if (i != 61) {
                    if (i != 66) {
                        switch (i) {
                            case 277:
                                if (keyEvent.hasNoModifiers() && canCut() && onTextContextMenuItem(16908320)) {
                                    return -1;
                                }
                                break;
                            case 278:
                                if (keyEvent.hasNoModifiers() && canCopy() && onTextContextMenuItem(16908321)) {
                                    return -1;
                                }
                                break;
                            case 279:
                                if (keyEvent.hasNoModifiers() && canPaste() && onTextContextMenuItem(16908322)) {
                                    return -1;
                                }
                                break;
                        }
                    } else if (keyEvent.hasNoModifiers()) {
                        if (this.mEditor != null && this.mEditor.mInputContentType != null && this.mEditor.mInputContentType.onEditorActionListener != null && this.mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, 0, keyEvent)) {
                            this.mEditor.mInputContentType.enterDown = true;
                            return -1;
                        }
                        if ((keyEvent.getFlags() & 16) != 0 || shouldAdvanceFocusOnEnter()) {
                            return hasOnClickListeners() ? 0 : -1;
                        }
                    }
                } else if ((keyEvent.hasNoModifiers() || keyEvent.hasModifiers(1)) && shouldAdvanceFocusOnTab()) {
                    return 0;
                }
            } else if (keyEvent.hasNoModifiers() && shouldAdvanceFocusOnEnter()) {
                return 0;
            }
        } else if (this.mEditor != null && this.mEditor.getTextActionMode() != null) {
            stopTextActionMode();
            return -1;
        }
        if (this.mEditor != null && this.mEditor.mKeyListener != null) {
            if (keyEvent2 != null) {
                try {
                    beginBatchEdit();
                    zOnKeyOther = this.mEditor.mKeyListener.onKeyOther(this, (Editable) this.mText, keyEvent2);
                    hideErrorIfUnchanged();
                } catch (AbstractMethodError e) {
                    z2 = true;
                } finally {
                    endBatchEdit();
                }
                if (zOnKeyOther) {
                    return -1;
                }
                endBatchEdit();
                z2 = false;
                if (z2) {
                    beginBatchEdit();
                    boolean zOnKeyDown = this.mEditor.mKeyListener.onKeyDown(this, (Editable) this.mText, i, keyEvent);
                    endBatchEdit();
                    hideErrorIfUnchanged();
                    if (zOnKeyDown) {
                        return 1;
                    }
                }
            } else {
                z2 = true;
                if (z2) {
                }
            }
        }
        if (this.mMovement != null && this.mLayout != null) {
            if (keyEvent2 != null) {
                try {
                } catch (AbstractMethodError e2) {
                    z = true;
                }
                if (this.mMovement.onKeyOther(this, this.mSpannable, keyEvent2)) {
                    return -1;
                }
                z = false;
                if (!z && this.mMovement.onKeyDown(this, this.mSpannable, i, keyEvent)) {
                    if (keyEvent.getRepeatCount() != 0 || KeyEvent.isModifierKey(i)) {
                        return 2;
                    }
                    this.mPreventDefaultMovement = true;
                    return 2;
                }
                if (keyEvent.getSource() == 257 && isDirectionalNavigationKey(i)) {
                    return -1;
                }
            } else {
                z = true;
                if (!z) {
                }
                if (keyEvent.getSource() == 257) {
                    return -1;
                }
            }
        }
        return (!this.mPreventDefaultMovement || KeyEvent.isModifierKey(i)) ? 0 : -1;
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
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        InputMethodManager inputMethodManagerPeekInstance;
        if (!isEnabled()) {
            return super.onKeyUp(i, keyEvent);
        }
        if (!KeyEvent.isModifierKey(i)) {
            this.mPreventDefaultMovement = false;
        }
        if (i == 23) {
            if (keyEvent.hasNoModifiers() && !hasOnClickListeners() && this.mMovement != null && (this.mText instanceof Editable) && this.mLayout != null && onCheckIsTextEditor()) {
                InputMethodManager inputMethodManagerPeekInstance2 = InputMethodManager.peekInstance();
                viewClicked(inputMethodManagerPeekInstance2);
                if (inputMethodManagerPeekInstance2 != null && getShowSoftInputOnFocus()) {
                    inputMethodManagerPeekInstance2.showSoftInput(this, 0);
                }
            }
            return super.onKeyUp(i, keyEvent);
        }
        if (i == 66 && keyEvent.hasNoModifiers()) {
            if (this.mEditor != null && this.mEditor.mInputContentType != null && this.mEditor.mInputContentType.onEditorActionListener != null && this.mEditor.mInputContentType.enterDown) {
                this.mEditor.mInputContentType.enterDown = false;
                if (this.mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, 0, keyEvent)) {
                    return true;
                }
            }
            if (((keyEvent.getFlags() & 16) != 0 || shouldAdvanceFocusOnEnter()) && !hasOnClickListeners()) {
                View viewFocusSearch = focusSearch(130);
                if (viewFocusSearch != null) {
                    if (!viewFocusSearch.requestFocus(130)) {
                        throw new IllegalStateException("focus search returned a view that wasn't able to take focus!");
                    }
                    super.onKeyUp(i, keyEvent);
                    return true;
                }
                if ((keyEvent.getFlags() & 16) != 0 && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null && inputMethodManagerPeekInstance.isActive(this)) {
                    inputMethodManagerPeekInstance.hideSoftInputFromWindow(getWindowToken(), 0);
                }
            }
            return super.onKeyUp(i, keyEvent);
        }
        if (this.mEditor != null && this.mEditor.mKeyListener != null && this.mEditor.mKeyListener.onKeyUp(this, (Editable) this.mText, i, keyEvent)) {
            return true;
        }
        if (this.mMovement == null || this.mLayout == null || !this.mMovement.onKeyUp(this, this.mSpannable, i, keyEvent)) {
            return super.onKeyUp(i, keyEvent);
        }
        return true;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return (this.mEditor == null || this.mEditor.mInputType == 0) ? false : true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        if (onCheckIsTextEditor() && isEnabled()) {
            this.mEditor.createInputMethodStateIfNeeded();
            editorInfo.inputType = getInputType();
            if (this.mEditor.mInputContentType != null) {
                editorInfo.imeOptions = this.mEditor.mInputContentType.imeOptions;
                editorInfo.privateImeOptions = this.mEditor.mInputContentType.privateImeOptions;
                editorInfo.actionLabel = this.mEditor.mInputContentType.imeActionLabel;
                editorInfo.actionId = this.mEditor.mInputContentType.imeActionId;
                editorInfo.extras = this.mEditor.mInputContentType.extras;
                editorInfo.hintLocales = this.mEditor.mInputContentType.imeHintLocales;
            } else {
                editorInfo.imeOptions = 0;
                editorInfo.hintLocales = null;
            }
            if (focusSearch(130) != null) {
                editorInfo.imeOptions |= 134217728;
            }
            if (focusSearch(33) != null) {
                editorInfo.imeOptions |= 67108864;
            }
            if ((editorInfo.imeOptions & 255) == 0) {
                if ((editorInfo.imeOptions & 134217728) != 0) {
                    editorInfo.imeOptions |= 5;
                } else {
                    editorInfo.imeOptions |= 6;
                }
                if (!shouldAdvanceFocusOnEnter()) {
                    editorInfo.imeOptions |= 1073741824;
                }
            }
            if (isMultilineInputType(editorInfo.inputType)) {
                editorInfo.imeOptions |= 1073741824;
            }
            editorInfo.hintText = this.mHint;
            if (this.mText instanceof Editable) {
                EditableInputConnection editableInputConnection = new EditableInputConnection(this);
                editorInfo.initialSelStart = getSelectionStart();
                editorInfo.initialSelEnd = getSelectionEnd();
                editorInfo.initialCapsMode = editableInputConnection.getCursorCapsMode(getInputType());
                return editableInputConnection;
            }
        }
        return null;
    }

    public boolean extractText(ExtractedTextRequest extractedTextRequest, ExtractedText extractedText) {
        createEditorIfNeeded();
        return this.mEditor.extractText(extractedTextRequest, extractedText);
    }

    static void removeParcelableSpans(Spannable spannable, int i, int i2) {
        Object[] spans = spannable.getSpans(i, i2, ParcelableSpan.class);
        int length = spans.length;
        while (length > 0) {
            length--;
            spannable.removeSpan(spans[length]);
        }
    }

    public void setExtractedText(ExtractedText extractedText) {
        int i;
        Editable editableText = getEditableText();
        if (extractedText.text != null) {
            if (editableText == null) {
                setText(extractedText.text, BufferType.EDITABLE);
            } else {
                int length = editableText.length();
                if (extractedText.partialStartOffset >= 0) {
                    length = editableText.length();
                    int i2 = extractedText.partialStartOffset;
                    if (i2 > length) {
                        i2 = length;
                    }
                    int i3 = extractedText.partialEndOffset;
                    if (i3 > length) {
                        i = i2;
                    } else {
                        i = i2;
                        length = i3;
                    }
                } else {
                    i = 0;
                }
                removeParcelableSpans(editableText, i, length);
                if (TextUtils.equals(editableText.subSequence(i, length), extractedText.text)) {
                    if (extractedText.text instanceof Spanned) {
                        TextUtils.copySpansFrom((Spanned) extractedText.text, 0, length - i, Object.class, editableText, i);
                    }
                } else {
                    editableText.replace(i, length, extractedText.text);
                }
            }
        }
        Spannable spannable = (Spannable) getText();
        int length2 = spannable.length();
        int i4 = extractedText.selectionStart;
        if (i4 >= 0) {
            if (i4 > length2) {
                i4 = length2;
            }
        } else {
            i4 = 0;
        }
        int i5 = extractedText.selectionEnd;
        if (i5 >= 0) {
            if (i5 <= length2) {
                length2 = i5;
            }
        } else {
            length2 = 0;
        }
        Selection.setSelection(spannable, i4, length2);
        if ((extractedText.flags & 2) != 0) {
            MetaKeyKeyListener.startSelecting(this, spannable);
        } else {
            MetaKeyKeyListener.stopSelecting(this, spannable);
        }
        setHintInternal(extractedText.hint);
    }

    public void setExtracting(ExtractedTextRequest extractedTextRequest) {
        if (this.mEditor.mInputMethodState != null) {
            this.mEditor.mInputMethodState.mExtractedTextRequest = extractedTextRequest;
        }
        this.mEditor.hideCursorAndSpanControllers();
        stopTextActionMode();
        if (this.mEditor.mSelectionModifierCursorController != null) {
            this.mEditor.mSelectionModifierCursorController.resetTouchOffsets();
        }
    }

    public void onCommitCompletion(CompletionInfo completionInfo) {
    }

    public void onCommitCorrection(CorrectionInfo correctionInfo) {
        if (this.mEditor != null) {
            this.mEditor.onCommitCorrection(correctionInfo);
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

    public boolean onPrivateIMECommand(String str, Bundle bundle) {
        return false;
    }

    @VisibleForTesting
    public void nullLayouts() {
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
        int compoundPaddingLeft = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        if (compoundPaddingLeft < 1) {
            compoundPaddingLeft = 0;
        }
        int i = compoundPaddingLeft;
        makeNewLayout(this.mHorizontallyScrolling ? 1048576 : i, i, UNKNOWN_BORING, UNKNOWN_BORING, i, false);
    }

    private Layout.Alignment getLayoutAlignment() {
        switch (getTextAlignment()) {
            case 1:
                int i = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
                if (i == 1) {
                    return Layout.Alignment.ALIGN_CENTER;
                }
                if (i == 3) {
                    return Layout.Alignment.ALIGN_LEFT;
                }
                if (i == 5) {
                    return Layout.Alignment.ALIGN_RIGHT;
                }
                if (i == 8388611) {
                    return Layout.Alignment.ALIGN_NORMAL;
                }
                if (i == 8388613) {
                    return Layout.Alignment.ALIGN_OPPOSITE;
                }
                return Layout.Alignment.ALIGN_NORMAL;
            case 2:
                return Layout.Alignment.ALIGN_NORMAL;
            case 3:
                return Layout.Alignment.ALIGN_OPPOSITE;
            case 4:
                return Layout.Alignment.ALIGN_CENTER;
            case 5:
                return getLayoutDirection() == 1 ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
            case 6:
                return getLayoutDirection() == 1 ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
            default:
                return Layout.Alignment.ALIGN_NORMAL;
        }
    }

    @VisibleForTesting
    public void makeNewLayout(int i, int i2, BoringLayout.Metrics metrics, BoringLayout.Metrics metrics2, int i3, boolean z) {
        boolean z2;
        int i4;
        boolean z3;
        int i5;
        int i6;
        BoringLayout.Metrics metrics3;
        Layout.Alignment alignment;
        int i7;
        stopMarquee();
        this.mOldMaximum = this.mMaximum;
        this.mOldMaxMode = this.mMaxMode;
        this.mHighlightPathBogus = true;
        int i8 = i < 0 ? 0 : i;
        int i9 = i2 < 0 ? 0 : i2;
        Layout.Alignment layoutAlignment = getLayoutAlignment();
        boolean z4 = this.mSingleLine && this.mLayout != null && (layoutAlignment == Layout.Alignment.ALIGN_NORMAL || layoutAlignment == Layout.Alignment.ALIGN_OPPOSITE);
        int paragraphDirection = z4 ? this.mLayout.getParagraphDirection(0) : 0;
        boolean z5 = this.mEllipsize != null && getKeyListener() == null;
        boolean z6 = this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 0;
        TextUtils.TruncateAt truncateAt = this.mEllipsize;
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode == 1) {
            truncateAt = TextUtils.TruncateAt.END_SMALL;
        }
        TextUtils.TruncateAt truncateAt2 = truncateAt;
        if (this.mTextDir == null) {
            this.mTextDir = getTextDirectionHeuristic();
        }
        if (truncateAt2 == this.mEllipsize) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mLayout = makeSingleLayout(i8, metrics, i3, layoutAlignment, z5, truncateAt2, z2);
        if (z6) {
            this.mSavedMarqueeModeLayout = makeSingleLayout(i8, metrics, i3, layoutAlignment, z5, truncateAt2 == TextUtils.TruncateAt.MARQUEE ? TextUtils.TruncateAt.END : TextUtils.TruncateAt.MARQUEE, truncateAt2 != this.mEllipsize);
        }
        boolean z7 = this.mEllipsize != null;
        this.mHintLayout = null;
        if (this.mHint == null) {
            i4 = paragraphDirection;
            z3 = true;
            i5 = i3;
            i6 = 0;
        } else {
            int i10 = z7 ? i8 : i9;
            if (metrics2 == UNKNOWN_BORING) {
                BoringLayout.Metrics metricsIsBoring = BoringLayout.isBoring(this.mHint, this.mTextPaint, this.mTextDir, this.mHintBoring);
                if (metricsIsBoring != null) {
                    this.mHintBoring = metricsIsBoring;
                }
                metrics3 = metricsIsBoring;
            } else {
                metrics3 = metrics2;
            }
            if (metrics3 != null) {
                if (metrics3.width <= i10 && (!z7 || metrics3.width <= i3)) {
                    if (this.mSavedHintLayout != null) {
                        this.mHintLayout = this.mSavedHintLayout.replaceOrMake(this.mHint, this.mTextPaint, i10, layoutAlignment, this.mSpacingMult, this.mSpacingAdd, metrics3, this.mIncludePad);
                    } else {
                        this.mHintLayout = BoringLayout.make(this.mHint, this.mTextPaint, i10, layoutAlignment, this.mSpacingMult, this.mSpacingAdd, metrics3, this.mIncludePad);
                    }
                    this.mSavedHintLayout = (BoringLayout) this.mHintLayout;
                } else {
                    if (z7 && metrics3.width <= i10) {
                        if (this.mSavedHintLayout != null) {
                            alignment = layoutAlignment;
                            i4 = paragraphDirection;
                            i7 = i10;
                            i6 = 0;
                            i5 = i3;
                            this.mHintLayout = this.mSavedHintLayout.replaceOrMake(this.mHint, this.mTextPaint, i10, alignment, this.mSpacingMult, this.mSpacingAdd, metrics3, this.mIncludePad, this.mEllipsize, i5);
                        } else {
                            i4 = paragraphDirection;
                            alignment = layoutAlignment;
                            i7 = i10;
                            i5 = i3;
                            i6 = 0;
                            this.mHintLayout = BoringLayout.make(this.mHint, this.mTextPaint, i7, layoutAlignment, this.mSpacingMult, this.mSpacingAdd, metrics3, this.mIncludePad, this.mEllipsize, i5);
                        }
                    }
                    if (this.mHintLayout == null) {
                    }
                }
                i4 = paragraphDirection;
                alignment = layoutAlignment;
                i7 = i10;
                i5 = i3;
                i6 = 0;
                if (this.mHintLayout == null) {
                }
            } else {
                i4 = paragraphDirection;
                alignment = layoutAlignment;
                i7 = i10;
                i5 = i3;
                i6 = 0;
                if (this.mHintLayout == null) {
                    z3 = true;
                    StaticLayout.Builder maxLines = StaticLayout.Builder.obtain(this.mHint, i6, this.mHint.length(), this.mTextPaint, i7).setAlignment(alignment).setTextDirection(this.mTextDir).setLineSpacing(this.mSpacingAdd, this.mSpacingMult).setIncludePad(this.mIncludePad).setUseLineSpacingFromFallbacks(this.mUseFallbackLineSpacing).setBreakStrategy(this.mBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency).setJustificationMode(this.mJustificationMode).setMaxLines(this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
                    if (z7) {
                        maxLines.setEllipsize(this.mEllipsize).setEllipsizedWidth(i5);
                    }
                    this.mHintLayout = maxLines.build();
                } else {
                    z3 = true;
                }
            }
        }
        if (z || (z4 && i4 != this.mLayout.getParagraphDirection(i6))) {
            registerForPreDraw();
        }
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE && !compressText(i5)) {
            int i11 = this.mLayoutParams.height;
            if (i11 != -2 && i11 != -1) {
                startMarquee();
            } else {
                this.mRestartMarquee = z3;
            }
        }
        if (this.mEditor != null) {
            this.mEditor.prepareCursorControllers();
        }
    }

    @VisibleForTesting
    public boolean useDynamicLayout() {
        return isTextSelectable() || (this.mSpannable != null && this.mPrecomputed == null);
    }

    protected Layout makeSingleLayout(int i, BoringLayout.Metrics metrics, int i2, Layout.Alignment alignment, boolean z, TextUtils.TruncateAt truncateAt, boolean z2) {
        BoringLayout.Metrics metrics2;
        BoringLayout boringLayoutMake;
        Layout layoutMake = null;
        layoutMake = null;
        layoutMake = null;
        if (useDynamicLayout()) {
            layoutMake = DynamicLayout.Builder.obtain(this.mText, this.mTextPaint, i).setDisplayText(this.mTransformed).setAlignment(alignment).setTextDirection(this.mTextDir).setLineSpacing(this.mSpacingAdd, this.mSpacingMult).setIncludePad(this.mIncludePad).setUseLineSpacingFromFallbacks(this.mUseFallbackLineSpacing).setBreakStrategy(this.mBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency).setJustificationMode(this.mJustificationMode).setEllipsize(getKeyListener() == null ? truncateAt : null).setEllipsizedWidth(i2).build();
        } else {
            if (metrics == UNKNOWN_BORING) {
                BoringLayout.Metrics metricsIsBoring = BoringLayout.isBoring(this.mTransformed, this.mTextPaint, this.mTextDir, this.mBoring);
                if (metricsIsBoring != null) {
                    this.mBoring = metricsIsBoring;
                }
                metrics2 = metricsIsBoring;
            } else {
                metrics2 = metrics;
            }
            if (metrics2 != null) {
                if (metrics2.width <= i && (truncateAt == null || metrics2.width <= i2)) {
                    if (z2 && this.mSavedLayout != null) {
                        boringLayoutMake = this.mSavedLayout.replaceOrMake(this.mTransformed, this.mTextPaint, i, alignment, this.mSpacingMult, this.mSpacingAdd, metrics2, this.mIncludePad);
                    } else {
                        boringLayoutMake = BoringLayout.make(this.mTransformed, this.mTextPaint, i, alignment, this.mSpacingMult, this.mSpacingAdd, metrics2, this.mIncludePad);
                    }
                    layoutMake = boringLayoutMake;
                    if (z2) {
                        this.mSavedLayout = (BoringLayout) layoutMake;
                    }
                } else if (z && metrics2.width <= i) {
                    layoutMake = (!z2 || this.mSavedLayout == null) ? BoringLayout.make(this.mTransformed, this.mTextPaint, i, alignment, this.mSpacingMult, this.mSpacingAdd, metrics2, this.mIncludePad, truncateAt, i2) : this.mSavedLayout.replaceOrMake(this.mTransformed, this.mTextPaint, i, alignment, this.mSpacingMult, this.mSpacingAdd, metrics2, this.mIncludePad, truncateAt, i2);
                }
            }
        }
        if (layoutMake == null) {
            StaticLayout.Builder maxLines = StaticLayout.Builder.obtain(this.mTransformed, 0, this.mTransformed.length(), this.mTextPaint, i).setAlignment(alignment).setTextDirection(this.mTextDir).setLineSpacing(this.mSpacingAdd, this.mSpacingMult).setIncludePad(this.mIncludePad).setUseLineSpacingFromFallbacks(this.mUseFallbackLineSpacing).setBreakStrategy(this.mBreakStrategy).setHyphenationFrequency(this.mHyphenationFrequency).setJustificationMode(this.mJustificationMode).setMaxLines(this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE);
            if (z) {
                maxLines.setEllipsize(truncateAt).setEllipsizedWidth(i2);
            }
            return maxLines.build();
        }
        return layoutMake;
    }

    private boolean compressText(float f) {
        if (!isHardwareAccelerated() && f > 0.0f && this.mLayout != null && getLineCount() == 1 && !this.mUserSetTextScaleX && this.mTextPaint.getTextScaleX() == 1.0f) {
            float lineWidth = ((this.mLayout.getLineWidth(0) + 1.0f) - f) / f;
            if (lineWidth > 0.0f && lineWidth <= 0.07f) {
                this.mTextPaint.setTextScaleX((1.0f - lineWidth) - 0.005f);
                post(new Runnable() {
                    @Override
                    public void run() {
                        TextView.this.requestLayout();
                    }
                });
                return true;
            }
        }
        return false;
    }

    private static int desired(Layout layout) {
        int lineCount = layout.getLineCount();
        CharSequence text = layout.getText();
        for (int i = 0; i < lineCount - 1; i++) {
            if (text.charAt(layout.getLineEnd(i) - 1) != '\n') {
                return -1;
            }
        }
        float fMax = 0.0f;
        for (int i2 = 0; i2 < lineCount; i2++) {
            fMax = Math.max(fMax, layout.getLineWidth(i2));
        }
        return (int) Math.ceil(fMax);
    }

    public void setIncludeFontPadding(boolean z) {
        if (this.mIncludePad != z) {
            this.mIncludePad = z;
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
    protected void onMeasure(int i, int i2) {
        int iCeil;
        boolean z;
        int iMax;
        int i3;
        int i4;
        int iMin;
        int iMax2;
        int iMin2;
        int i5;
        BoringLayout.Metrics metrics;
        int iCeil2;
        int width;
        int i6;
        int i7;
        int i8;
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        BoringLayout.Metrics metricsIsBoring = UNKNOWN_BORING;
        BoringLayout.Metrics metricsIsBoring2 = UNKNOWN_BORING;
        if (this.mTextDir == null) {
            this.mTextDir = getTextDirectionHeuristic();
        }
        float f = mode == Integer.MIN_VALUE ? size : Float.MAX_VALUE;
        if (mode != 1073741824) {
            if (this.mLayout != null && this.mEllipsize == null) {
                iCeil = desired(this.mLayout);
            } else {
                iCeil = -1;
            }
            if (iCeil < 0) {
                metricsIsBoring = BoringLayout.isBoring(this.mTransformed, this.mTextPaint, this.mTextDir, this.mBoring);
                if (metricsIsBoring != null) {
                    this.mBoring = metricsIsBoring;
                }
                z = false;
            } else {
                z = true;
            }
            if (metricsIsBoring == null || metricsIsBoring == UNKNOWN_BORING) {
                if (iCeil < 0) {
                    iCeil = (int) Math.ceil(Layout.getDesiredWidthWithLimit(this.mTransformed, 0, this.mTransformed.length(), this.mTextPaint, this.mTextDir, f));
                }
                iMax = iCeil;
                i3 = iMax;
            } else {
                iMax = metricsIsBoring.width;
                i3 = iCeil;
            }
            Drawables drawables = this.mDrawables;
            if (drawables != null) {
                iMax = Math.max(Math.max(iMax, drawables.mDrawableWidthTop), drawables.mDrawableWidthBottom);
            }
            int i9 = iMax;
            if (this.mHint != null) {
                if (this.mHintLayout != null && this.mEllipsize == null) {
                    iCeil2 = desired(this.mHintLayout);
                } else {
                    iCeil2 = -1;
                }
                if (iCeil2 < 0 && (metricsIsBoring2 = BoringLayout.isBoring(this.mHint, this.mTextPaint, this.mTextDir, this.mHintBoring)) != null) {
                    this.mHintBoring = metricsIsBoring2;
                }
                if (metricsIsBoring2 == null || metricsIsBoring2 == UNKNOWN_BORING) {
                    if (iCeil2 < 0) {
                        i4 = i9;
                        iCeil2 = (int) Math.ceil(Layout.getDesiredWidthWithLimit(this.mHint, 0, this.mHint.length(), this.mTextPaint, this.mTextDir, f));
                    } else {
                        i4 = i9;
                    }
                    i9 = iCeil2;
                } else {
                    i4 = i9;
                    i9 = metricsIsBoring2.width;
                }
                if (i9 > i4) {
                }
                int compoundPaddingLeft = i4 + getCompoundPaddingLeft() + getCompoundPaddingRight();
                if (this.mMaxWidthMode != 1) {
                }
                if (this.mMinWidthMode != 1) {
                }
                int iMax3 = Math.max(iMax2, getSuggestedMinimumWidth());
                if (mode != Integer.MIN_VALUE) {
                }
            } else {
                i4 = i9;
                int compoundPaddingLeft2 = i4 + getCompoundPaddingLeft() + getCompoundPaddingRight();
                if (this.mMaxWidthMode != 1) {
                    iMin = Math.min(compoundPaddingLeft2, this.mMaxWidth * getLineHeight());
                } else {
                    iMin = Math.min(compoundPaddingLeft2, this.mMaxWidth);
                }
                if (this.mMinWidthMode != 1) {
                    iMax2 = Math.max(iMin, this.mMinWidth * getLineHeight());
                } else {
                    iMax2 = Math.max(iMin, this.mMinWidth);
                }
                int iMax32 = Math.max(iMax2, getSuggestedMinimumWidth());
                if (mode != Integer.MIN_VALUE) {
                    iMin2 = Math.min(size, iMax32);
                    metrics = metricsIsBoring2;
                    i5 = i3;
                } else {
                    iMin2 = iMax32;
                    i5 = i3;
                    metrics = metricsIsBoring2;
                }
            }
        } else {
            iMin2 = size;
            metrics = metricsIsBoring2;
            i5 = -1;
            z = false;
        }
        BoringLayout.Metrics metrics2 = metricsIsBoring;
        int compoundPaddingLeft3 = (iMin2 - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int i10 = this.mHorizontallyScrolling ? 1048576 : compoundPaddingLeft3;
        if (this.mHintLayout != null) {
            width = this.mHintLayout.getWidth();
        } else {
            width = i10;
        }
        if (this.mLayout == null) {
            i6 = -1;
            i7 = 0;
            i8 = 1073741824;
            makeNewLayout(i10, i10, metrics2, metrics, (iMin2 - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
        } else {
            i6 = -1;
            i7 = 0;
            i8 = 1073741824;
            boolean z2 = (this.mLayout.getWidth() == i10 && width == i10 && this.mLayout.getEllipsizedWidth() == (iMin2 - getCompoundPaddingLeft()) - getCompoundPaddingRight()) ? false : true;
            boolean z3 = this.mHint == null && this.mEllipsize == null && i10 > this.mLayout.getWidth() && ((this.mLayout instanceof BoringLayout) || (z && i5 >= 0 && i5 <= i10));
            boolean z4 = (this.mMaxMode == this.mOldMaxMode && this.mMaximum == this.mOldMaximum) ? false : true;
            if (z2 || z4) {
                if (!z4 && z3) {
                    this.mLayout.increaseWidthTo(i10);
                } else {
                    makeNewLayout(i10, i10, metrics2, metrics, (iMin2 - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
                }
            }
        }
        if (mode2 == i8) {
            this.mDesiredHeightAtMeasure = i6;
        } else {
            int desiredHeight = getDesiredHeight();
            this.mDesiredHeightAtMeasure = desiredHeight;
            if (mode2 == Integer.MIN_VALUE) {
                size2 = Math.min(desiredHeight, size2);
            } else {
                size2 = desiredHeight;
            }
        }
        int compoundPaddingTop = (size2 - getCompoundPaddingTop()) - getCompoundPaddingBottom();
        if (this.mMaxMode == 1 && this.mLayout.getLineCount() > this.mMaximum) {
            compoundPaddingTop = Math.min(compoundPaddingTop, this.mLayout.getLineTop(this.mMaximum));
        }
        if (this.mMovement != null || this.mLayout.getWidth() > compoundPaddingLeft3 || this.mLayout.getHeight() > compoundPaddingTop) {
            registerForPreDraw();
        } else {
            scrollTo(i7, i7);
        }
        setMeasuredDimension(iMin2, size2);
    }

    private void autoSizeText() {
        int measuredWidth;
        if (!isAutoSizeEnabled()) {
            return;
        }
        if (this.mNeedsAutoSizeText) {
            if (getMeasuredWidth() <= 0 || getMeasuredHeight() <= 0) {
                return;
            }
            if (this.mHorizontallyScrolling) {
                measuredWidth = 1048576;
            } else {
                measuredWidth = (getMeasuredWidth() - getTotalPaddingLeft()) - getTotalPaddingRight();
            }
            int i = measuredWidth;
            int measuredHeight = (getMeasuredHeight() - getExtendedPaddingBottom()) - getExtendedPaddingTop();
            if (i <= 0 || measuredHeight <= 0) {
                return;
            }
            synchronized (TEMP_RECTF) {
                TEMP_RECTF.setEmpty();
                TEMP_RECTF.right = i;
                TEMP_RECTF.bottom = measuredHeight;
                float fFindLargestTextSizeWhichFits = findLargestTextSizeWhichFits(TEMP_RECTF);
                if (fFindLargestTextSizeWhichFits != getTextSize()) {
                    setTextSizeInternal(0, fFindLargestTextSizeWhichFits, false);
                    makeNewLayout(i, 0, UNKNOWN_BORING, UNKNOWN_BORING, ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
                }
            }
        }
        this.mNeedsAutoSizeText = true;
    }

    private int findLargestTextSizeWhichFits(RectF rectF) {
        int i;
        int length = this.mAutoSizeTextSizesInPx.length;
        if (length == 0) {
            throw new IllegalStateException("No available text sizes to choose from.");
        }
        int i2 = 0;
        int i3 = 1;
        int i4 = length - 1;
        while (true) {
            int i5 = i3;
            int i6 = i2;
            i2 = i5;
            while (i2 <= i4) {
                i = (i2 + i4) / 2;
                if (suggestedSizeFitsInSpace(this.mAutoSizeTextSizesInPx[i], rectF)) {
                    break;
                }
                i6 = i - 1;
                i4 = i6;
            }
            return this.mAutoSizeTextSizesInPx[i6];
            i3 = i + 1;
        }
    }

    private boolean suggestedSizeFitsInSpace(int i, RectF rectF) {
        CharSequence text;
        if (this.mTransformed != null) {
            text = this.mTransformed;
        } else {
            text = getText();
        }
        int maxLines = getMaxLines();
        if (this.mTempTextPaint == null) {
            this.mTempTextPaint = new TextPaint();
        } else {
            this.mTempTextPaint.reset();
        }
        this.mTempTextPaint.set(getPaint());
        this.mTempTextPaint.setTextSize(i);
        StaticLayout.Builder builderObtain = StaticLayout.Builder.obtain(text, 0, text.length(), this.mTempTextPaint, Math.round(rectF.right));
        builderObtain.setAlignment(getLayoutAlignment()).setLineSpacing(getLineSpacingExtra(), getLineSpacingMultiplier()).setIncludePad(getIncludeFontPadding()).setUseLineSpacingFromFallbacks(this.mUseFallbackLineSpacing).setBreakStrategy(getBreakStrategy()).setHyphenationFrequency(getHyphenationFrequency()).setJustificationMode(getJustificationMode()).setMaxLines(this.mMaxMode == 1 ? this.mMaximum : Integer.MAX_VALUE).setTextDirection(getTextDirectionHeuristic());
        StaticLayout staticLayoutBuild = builderObtain.build();
        return (maxLines == -1 || staticLayoutBuild.getLineCount() <= maxLines) && ((float) staticLayoutBuild.getHeight()) <= rectF.bottom;
    }

    private int getDesiredHeight() {
        return Math.max(getDesiredHeight(this.mLayout, true), getDesiredHeight(this.mHintLayout, this.mEllipsize != null));
    }

    private int getDesiredHeight(Layout layout, boolean z) {
        if (layout == null) {
            return 0;
        }
        int height = layout.getHeight(z);
        Drawables drawables = this.mDrawables;
        if (drawables != null) {
            height = Math.max(Math.max(height, drawables.mDrawableHeightLeft), drawables.mDrawableHeightRight);
        }
        int lineCount = layout.getLineCount();
        int compoundPaddingTop = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int iMax = height + compoundPaddingTop;
        if (this.mMaxMode != 1) {
            iMax = Math.min(iMax, this.mMaximum);
        } else if (z && lineCount > this.mMaximum && ((layout instanceof DynamicLayout) || (layout instanceof BoringLayout))) {
            int lineTop = layout.getLineTop(this.mMaximum);
            if (drawables != null) {
                lineTop = Math.max(Math.max(lineTop, drawables.mDrawableHeightLeft), drawables.mDrawableHeightRight);
            }
            iMax = lineTop + compoundPaddingTop;
            lineCount = this.mMaximum;
        }
        if (this.mMinMode == 1) {
            if (lineCount < this.mMinimum) {
                iMax += getLineHeight() * (this.mMinimum - lineCount);
            }
        } else {
            iMax = Math.max(iMax, this.mMinimum);
        }
        return Math.max(iMax, getSuggestedMinimumHeight());
    }

    private void checkForResize() {
        boolean z = true;
        boolean z2 = false;
        if (this.mLayout != null) {
            if (this.mLayoutParams.width == -2) {
                invalidate();
                z2 = true;
            }
            if (this.mLayoutParams.height == -2) {
                if (getDesiredHeight() == getHeight()) {
                    z = z2;
                }
            } else if (this.mLayoutParams.height != -1 || this.mDesiredHeightAtMeasure < 0 || getDesiredHeight() == this.mDesiredHeightAtMeasure) {
            }
        } else {
            z = z2;
        }
        if (z) {
            requestLayout();
        }
    }

    private void checkForRelayout() {
        if ((this.mLayoutParams.width != -2 || (this.mMaxWidthMode == this.mMinWidthMode && this.mMaxWidth == this.mMinWidth)) && ((this.mHint == null || this.mHintLayout != null) && ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight() > 0)) {
            int height = this.mLayout.getHeight();
            makeNewLayout(this.mLayout.getWidth(), this.mHintLayout == null ? 0 : this.mHintLayout.getWidth(), UNKNOWN_BORING, UNKNOWN_BORING, ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight(), false);
            if (this.mEllipsize != TextUtils.TruncateAt.MARQUEE) {
                if (this.mLayoutParams.height != -2 && this.mLayoutParams.height != -1) {
                    autoSizeText();
                    invalidate();
                    return;
                } else if (this.mLayout.getHeight() == height && (this.mHintLayout == null || this.mHintLayout.getHeight() == height)) {
                    autoSizeText();
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
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mDeferScroll >= 0) {
            int i5 = this.mDeferScroll;
            this.mDeferScroll = -1;
            bringPointIntoView(Math.min(i5, this.mText.length()));
        }
        autoSizeText();
    }

    private boolean isShowingHint() {
        return TextUtils.isEmpty(this.mText) && !TextUtils.isEmpty(this.mHint);
    }

    private boolean bringTextIntoView() {
        int lineCount;
        int iFloor;
        int i;
        Layout layout = isShowingHint() ? this.mHintLayout : this.mLayout;
        if ((this.mGravity & 112) == 80) {
            lineCount = layout.getLineCount() - 1;
        } else {
            lineCount = 0;
        }
        Layout.Alignment paragraphAlignment = layout.getParagraphAlignment(lineCount);
        int paragraphDirection = layout.getParagraphDirection(lineCount);
        int compoundPaddingLeft = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int extendedPaddingTop = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        int height = layout.getHeight();
        if (paragraphAlignment == Layout.Alignment.ALIGN_NORMAL) {
            paragraphAlignment = paragraphDirection == 1 ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
        } else if (paragraphAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
            paragraphAlignment = paragraphDirection == 1 ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
        }
        if (paragraphAlignment == Layout.Alignment.ALIGN_CENTER) {
            iFloor = (int) Math.floor(layout.getLineLeft(lineCount));
            int iCeil = (int) Math.ceil(layout.getLineRight(lineCount));
            if (iCeil - iFloor < compoundPaddingLeft) {
                iFloor = ((iCeil + iFloor) / 2) - (compoundPaddingLeft / 2);
            } else if (paragraphDirection < 0) {
                iFloor = iCeil - compoundPaddingLeft;
            }
        } else if (paragraphAlignment == Layout.Alignment.ALIGN_RIGHT) {
            iFloor = ((int) Math.ceil(layout.getLineRight(lineCount))) - compoundPaddingLeft;
        } else {
            iFloor = (int) Math.floor(layout.getLineLeft(lineCount));
        }
        if (height >= extendedPaddingTop && (this.mGravity & 112) == 80) {
            i = height - extendedPaddingTop;
        } else {
            i = 0;
        }
        if (iFloor == this.mScrollX && i == this.mScrollY) {
            return false;
        }
        scrollTo(iFloor, i);
        return true;
    }

    public boolean bringPointIntoView(int i) {
        int paragraphDirection;
        boolean z;
        if (isLayoutRequested()) {
            this.mDeferScroll = i;
            return false;
        }
        Layout layout = isShowingHint() ? this.mHintLayout : this.mLayout;
        if (layout == null) {
            return false;
        }
        int lineForOffset = layout.getLineForOffset(i);
        switch (layout.getParagraphAlignment(lineForOffset)) {
            case ALIGN_LEFT:
                paragraphDirection = 1;
                break;
            case ALIGN_RIGHT:
                paragraphDirection = -1;
                break;
            case ALIGN_NORMAL:
                paragraphDirection = layout.getParagraphDirection(lineForOffset);
                break;
            case ALIGN_OPPOSITE:
                paragraphDirection = -layout.getParagraphDirection(lineForOffset);
                break;
            default:
                paragraphDirection = 0;
                break;
        }
        int primaryHorizontal = (int) layout.getPrimaryHorizontal(i, paragraphDirection > 0);
        int lineTop = layout.getLineTop(lineForOffset);
        int lineTop2 = layout.getLineTop(lineForOffset + 1);
        int iFloor = (int) Math.floor(layout.getLineLeft(lineForOffset));
        int iCeil = (int) Math.ceil(layout.getLineRight(lineForOffset));
        int height = layout.getHeight();
        int compoundPaddingLeft = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int extendedPaddingTop = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        if (!this.mHorizontallyScrolling && iCeil - iFloor > compoundPaddingLeft && iCeil > primaryHorizontal) {
            iCeil = Math.max(primaryHorizontal, iFloor + compoundPaddingLeft);
        }
        int i2 = (lineTop2 - lineTop) / 2;
        int i3 = extendedPaddingTop / 4;
        if (i2 <= i3) {
            i3 = i2;
        }
        int i4 = compoundPaddingLeft / 4;
        if (i2 > i4) {
            i2 = i4;
        }
        int i5 = this.mScrollX;
        int i6 = this.mScrollY;
        if (lineTop - i6 < i3) {
            i6 = lineTop - i3;
        }
        int i7 = extendedPaddingTop - i3;
        if (lineTop2 - i6 > i7) {
            i6 = lineTop2 - i7;
        }
        int i8 = height - i6 < extendedPaddingTop ? height - extendedPaddingTop : i6;
        if (0 - i8 > 0) {
            i8 = 0;
        }
        if (paragraphDirection != 0) {
            if (primaryHorizontal - i5 < i2) {
                i5 = primaryHorizontal - i2;
            }
            int i9 = compoundPaddingLeft - i2;
            if (primaryHorizontal - i5 > i9) {
                i5 = primaryHorizontal - i9;
            }
        }
        if (paragraphDirection < 0) {
            if (iFloor - i5 <= 0) {
                iFloor = i5;
            }
            if (iCeil - iFloor < compoundPaddingLeft) {
                iFloor = iCeil - compoundPaddingLeft;
            }
        } else if (paragraphDirection > 0) {
            if (iCeil - i5 < compoundPaddingLeft) {
                i5 = iCeil - compoundPaddingLeft;
            }
            if (iFloor - i5 <= 0) {
                iFloor = i5;
            }
        } else {
            int i10 = iCeil - iFloor;
            if (i10 <= compoundPaddingLeft) {
                iFloor -= (compoundPaddingLeft - i10) / 2;
            } else if (primaryHorizontal > iCeil - i2) {
                iFloor = iCeil - compoundPaddingLeft;
            } else if (primaryHorizontal >= iFloor + i2 && iFloor <= i5) {
                if (iCeil < i5 + compoundPaddingLeft) {
                    iFloor = iCeil - compoundPaddingLeft;
                } else {
                    iFloor = primaryHorizontal - i5 < i2 ? primaryHorizontal - i2 : i5;
                    int i11 = compoundPaddingLeft - i2;
                    if (primaryHorizontal - iFloor > i11) {
                        iFloor = primaryHorizontal - i11;
                    }
                }
            }
        }
        if (iFloor != this.mScrollX || i8 != this.mScrollY) {
            if (this.mScroller == null) {
                scrollTo(iFloor, i8);
            } else {
                long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll;
                int i12 = iFloor - this.mScrollX;
                int i13 = i8 - this.mScrollY;
                if (jCurrentAnimationTimeMillis > 250) {
                    this.mScroller.startScroll(this.mScrollX, this.mScrollY, i12, i13);
                    awakenScrollBars(this.mScroller.getDuration());
                    invalidate();
                } else {
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.abortAnimation();
                    }
                    scrollBy(i12, i13);
                }
                this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
            }
            z = true;
        } else {
            z = false;
        }
        if (isFocused()) {
            if (this.mTempRect == null) {
                this.mTempRect = new Rect();
            }
            this.mTempRect.set(primaryHorizontal - 2, lineTop, primaryHorizontal + 2, lineTop2);
            getInterestingRect(this.mTempRect, lineForOffset);
            this.mTempRect.offset(this.mScrollX, this.mScrollY);
            if (requestRectangleOnScreen(this.mTempRect)) {
                return true;
            }
        }
        return z;
    }

    public boolean moveCursorToVisibleOffset() {
        int selectionStart;
        if (!(this.mText instanceof Spannable) || (selectionStart = getSelectionStart()) != getSelectionEnd()) {
            return false;
        }
        int lineForOffset = this.mLayout.getLineForOffset(selectionStart);
        int lineTop = this.mLayout.getLineTop(lineForOffset);
        int lineTop2 = this.mLayout.getLineTop(lineForOffset + 1);
        int extendedPaddingTop = ((this.mBottom - this.mTop) - getExtendedPaddingTop()) - getExtendedPaddingBottom();
        int i = lineTop2 - lineTop;
        int i2 = i / 2;
        int i3 = extendedPaddingTop / 4;
        if (i2 > i3) {
            i2 = i3;
        }
        int i4 = this.mScrollY;
        int i5 = i4 + i2;
        if (lineTop < i5) {
            lineForOffset = this.mLayout.getLineForVertical(i5 + i);
        } else {
            int i6 = (extendedPaddingTop + i4) - i2;
            if (lineTop2 > i6) {
                lineForOffset = this.mLayout.getLineForVertical(i6 - i);
            }
        }
        int compoundPaddingLeft = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        int offsetForHorizontal = this.mLayout.getOffsetForHorizontal(lineForOffset, this.mScrollX);
        int offsetForHorizontal2 = this.mLayout.getOffsetForHorizontal(lineForOffset, compoundPaddingLeft + r4);
        int i7 = offsetForHorizontal < offsetForHorizontal2 ? offsetForHorizontal : offsetForHorizontal2;
        if (offsetForHorizontal > offsetForHorizontal2) {
            offsetForHorizontal2 = offsetForHorizontal;
        }
        if (selectionStart >= i7) {
            if (selectionStart <= offsetForHorizontal2) {
                offsetForHorizontal2 = selectionStart;
            }
        } else {
            offsetForHorizontal2 = i7;
        }
        if (offsetForHorizontal2 == selectionStart) {
            return false;
        }
        Selection.setSelection(this.mSpannable, offsetForHorizontal2);
        return true;
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

    private void getInterestingRect(Rect rect, int i) {
        convertFromViewportToContentCoordinates(rect);
        if (i == 0) {
            rect.top -= getExtendedPaddingTop();
        }
        if (i == this.mLayout.getLineCount() - 1) {
            rect.bottom += getExtendedPaddingBottom();
        }
    }

    private void convertFromViewportToContentCoordinates(Rect rect) {
        int iViewportToContentHorizontalOffset = viewportToContentHorizontalOffset();
        rect.left += iViewportToContentHorizontalOffset;
        rect.right += iViewportToContentHorizontalOffset;
        int iViewportToContentVerticalOffset = viewportToContentVerticalOffset();
        rect.top += iViewportToContentVerticalOffset;
        rect.bottom += iViewportToContentVerticalOffset;
    }

    int viewportToContentHorizontalOffset() {
        return getCompoundPaddingLeft() - this.mScrollX;
    }

    int viewportToContentVerticalOffset() {
        int extendedPaddingTop = getExtendedPaddingTop() - this.mScrollY;
        if ((this.mGravity & 112) != 48) {
            return extendedPaddingTop + getVerticalOffset(false);
        }
        return extendedPaddingTop;
    }

    @Override
    public void debug(int i) {
        String str;
        super.debug(i);
        String str2 = debugIndent(i) + "frame={" + this.mLeft + ", " + this.mTop + ", " + this.mRight + ", " + this.mBottom + "} scroll={" + this.mScrollX + ", " + this.mScrollY + "} ";
        if (this.mText != null) {
            str = str2 + "mText=\"" + ((Object) this.mText) + "\" ";
            if (this.mLayout != null) {
                str = str + "mLayout width=" + this.mLayout.getWidth() + " height=" + this.mLayout.getHeight();
            }
        } else {
            str = str2 + "mText=NULL";
        }
        Log.d("View", str);
    }

    @ViewDebug.ExportedProperty(category = "text")
    public int getSelectionStart() {
        return Selection.getSelectionStart(getText());
    }

    @ViewDebug.ExportedProperty(category = "text")
    public int getSelectionEnd() {
        return Selection.getSelectionEnd(getText());
    }

    public boolean hasSelection() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        return selectionStart >= 0 && selectionEnd > 0 && selectionStart != selectionEnd;
    }

    String getSelectedText() {
        if (!hasSelection()) {
            return null;
        }
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        return String.valueOf(selectionStart > selectionEnd ? this.mText.subSequence(selectionEnd, selectionStart) : this.mText.subSequence(selectionStart, selectionEnd));
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public void setAllCaps(boolean z) {
        if (z) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        } else {
            setTransformationMethod(null);
        }
    }

    public boolean isAllCaps() {
        TransformationMethod transformationMethod = getTransformationMethod();
        return transformationMethod != null && (transformationMethod instanceof AllCapsTransformationMethod);
    }

    @RemotableViewMethod
    public void setSingleLine(boolean z) {
        setInputTypeSingleLine(z);
        applySingleLine(z, true, true);
    }

    private void setInputTypeSingleLine(boolean z) {
        if (this.mEditor != null && (this.mEditor.mInputType & 15) == 1) {
            if (z) {
                this.mEditor.mInputType &= -131073;
            } else {
                this.mEditor.mInputType |= 131072;
            }
        }
    }

    private void applySingleLine(boolean z, boolean z2, boolean z3) {
        this.mSingleLine = z;
        if (z) {
            setLines(1);
            setHorizontallyScrolling(true);
            if (z2) {
                setTransformationMethod(SingleLineTransformationMethod.getInstance());
                return;
            }
            return;
        }
        if (z3) {
            setMaxLines(Integer.MAX_VALUE);
        }
        setHorizontallyScrolling(false);
        if (z2) {
            setTransformationMethod(null);
        }
    }

    public void setEllipsize(TextUtils.TruncateAt truncateAt) {
        if (this.mEllipsize != truncateAt) {
            this.mEllipsize = truncateAt;
            if (this.mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    public void setMarqueeRepeatLimit(int i) {
        this.mMarqueeRepeatLimit = i;
    }

    public int getMarqueeRepeatLimit() {
        return this.mMarqueeRepeatLimit;
    }

    @ViewDebug.ExportedProperty
    public TextUtils.TruncateAt getEllipsize() {
        return this.mEllipsize;
    }

    @RemotableViewMethod
    public void setSelectAllOnFocus(boolean z) {
        createEditorIfNeeded();
        this.mEditor.mSelectAllOnFocus = z;
        if (z && !(this.mText instanceof Spannable)) {
            setText(this.mText, BufferType.SPANNABLE);
        }
    }

    @RemotableViewMethod
    public void setCursorVisible(boolean z) {
        if (z && this.mEditor == null) {
            return;
        }
        createEditorIfNeeded();
        if (this.mEditor.mCursorVisible != z) {
            this.mEditor.mCursorVisible = z;
            invalidate();
            this.mEditor.makeBlink();
            this.mEditor.prepareCursorControllers();
        }
    }

    public boolean isCursorVisible() {
        if (this.mEditor == null) {
            return true;
        }
        return this.mEditor.mCursorVisible;
    }

    private boolean canMarquee() {
        int compoundPaddingLeft = ((this.mRight - this.mLeft) - getCompoundPaddingLeft()) - getCompoundPaddingRight();
        if (compoundPaddingLeft <= 0) {
            return false;
        }
        float f = compoundPaddingLeft;
        return this.mLayout.getLineWidth(0) > f || !(this.mMarqueeFadeMode == 0 || this.mSavedMarqueeModeLayout == null || this.mSavedMarqueeModeLayout.getLineWidth(0) <= f);
    }

    private void startMarquee() {
        if (getKeyListener() != null || compressText((getWidth() - getCompoundPaddingLeft()) - getCompoundPaddingRight())) {
            return;
        }
        if (this.mMarquee == null || this.mMarquee.isStopped()) {
            if ((isFocused() || isSelected()) && getLineCount() == 1 && canMarquee()) {
                if (this.mMarqueeFadeMode == 1) {
                    this.mMarqueeFadeMode = 2;
                    Layout layout = this.mLayout;
                    this.mLayout = this.mSavedMarqueeModeLayout;
                    this.mSavedMarqueeModeLayout = layout;
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

    private void stopMarquee() {
        if (this.mMarquee != null && !this.mMarquee.isStopped()) {
            this.mMarquee.stop();
        }
        if (this.mMarqueeFadeMode == 2) {
            this.mMarqueeFadeMode = 1;
            Layout layout = this.mSavedMarqueeModeLayout;
            this.mSavedMarqueeModeLayout = this.mLayout;
            this.mLayout = layout;
            setHorizontalFadingEdgeEnabled(false);
            requestLayout();
            invalidate();
        }
    }

    private void startStopMarquee(boolean z) {
        if (this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (z) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }

    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    protected void onSelectionChanged(int i, int i2) {
        sendAccessibilityEvent(8192);
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        if (this.mListeners == null) {
            this.mListeners = new ArrayList<>();
        }
        this.mListeners.add(textWatcher);
    }

    public void removeTextChangedListener(TextWatcher textWatcher) {
        int iIndexOf;
        if (this.mListeners != null && (iIndexOf = this.mListeners.indexOf(textWatcher)) >= 0) {
            this.mListeners.remove(iIndexOf);
        }
    }

    private void sendBeforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> arrayList = this.mListeners;
            int size = arrayList.size();
            for (int i4 = 0; i4 < size; i4++) {
                arrayList.get(i4).beforeTextChanged(charSequence, i, i2, i3);
            }
        }
        int i5 = i2 + i;
        removeIntersectingNonAdjacentSpans(i, i5, SpellCheckSpan.class);
        removeIntersectingNonAdjacentSpans(i, i5, SuggestionSpan.class);
    }

    private <T> void removeIntersectingNonAdjacentSpans(int i, int i2, Class<T> cls) {
        if (this.mText instanceof Editable) {
            Editable editable = (Editable) this.mText;
            Object[] spans = editable.getSpans(i, i2, cls);
            int length = spans.length;
            for (int i3 = 0; i3 < length; i3++) {
                int spanStart = editable.getSpanStart(spans[i3]);
                if (editable.getSpanEnd(spans[i3]) != i && spanStart != i2) {
                    editable.removeSpan(spans[i3]);
                } else {
                    return;
                }
            }
        }
    }

    void removeAdjacentSuggestionSpans(int i) {
        if (this.mText instanceof Editable) {
            Editable editable = (Editable) this.mText;
            SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) editable.getSpans(i, i, SuggestionSpan.class);
            int length = suggestionSpanArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                int spanStart = editable.getSpanStart(suggestionSpanArr[i2]);
                int spanEnd = editable.getSpanEnd(suggestionSpanArr[i2]);
                if ((spanEnd == i || spanStart == i) && SpellChecker.haveWordBoundariesChanged(editable, i, i, spanStart, spanEnd)) {
                    editable.removeSpan(suggestionSpanArr[i2]);
                }
            }
        }
    }

    void sendOnTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> arrayList = this.mListeners;
            int size = arrayList.size();
            for (int i4 = 0; i4 < size; i4++) {
                arrayList.get(i4).onTextChanged(charSequence, i, i2, i3);
            }
        }
        if (this.mEditor != null) {
            this.mEditor.sendOnTextChanged(i, i2, i3);
        }
    }

    void sendAfterTextChanged(Editable editable) {
        if (this.mListeners != null) {
            ArrayList<TextWatcher> arrayList = this.mListeners;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                arrayList.get(i).afterTextChanged(editable);
            }
        }
        notifyAutoFillManagerAfterTextChangedIfNeeded();
        hideErrorIfUnchanged();
    }

    private void notifyAutoFillManagerAfterTextChangedIfNeeded() {
        AutofillManager autofillManager;
        if (!isAutofillable() || (autofillManager = (AutofillManager) this.mContext.getSystemService(AutofillManager.class)) == null) {
            return;
        }
        if (this.mLastValueSentToAutofillManager == null || !this.mLastValueSentToAutofillManager.equals(this.mText)) {
            if (Helper.sVerbose) {
                Log.v(LOG_TAG, "notifying AFM after text changed");
            }
            autofillManager.notifyValueChanged(this);
            this.mLastValueSentToAutofillManager = this.mText;
            return;
        }
        if (Helper.sVerbose) {
            Log.v(LOG_TAG, "not notifying AFM on unchanged text");
        }
    }

    private boolean isAutofillable() {
        return getAutofillType() != 0;
    }

    void updateAfterEdit() {
        invalidate();
        int selectionStart = getSelectionStart();
        if (selectionStart >= 0 || (this.mGravity & 112) == 80) {
            registerForPreDraw();
        }
        checkForResize();
        if (selectionStart >= 0) {
            this.mHighlightPathBogus = true;
            if (this.mEditor != null) {
                this.mEditor.makeBlink();
            }
            bringPointIntoView(selectionStart);
        }
    }

    void handleTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        sLastCutCopyOrTextChangedTime = 0L;
        Editor.InputMethodState inputMethodState = this.mEditor == null ? null : this.mEditor.mInputMethodState;
        if (inputMethodState == null || inputMethodState.mBatchEditNesting == 0) {
            updateAfterEdit();
        }
        if (inputMethodState != null) {
            inputMethodState.mContentChanged = true;
            if (inputMethodState.mChangedStart < 0) {
                inputMethodState.mChangedStart = i;
                inputMethodState.mChangedEnd = i + i2;
            } else {
                inputMethodState.mChangedStart = Math.min(inputMethodState.mChangedStart, i);
                inputMethodState.mChangedEnd = Math.max(inputMethodState.mChangedEnd, (i + i2) - inputMethodState.mChangedDelta);
            }
            inputMethodState.mChangedDelta += i3 - i2;
        }
        resetErrorChangedFlag();
        sendOnTextChanged(charSequence, i, i2, i3);
        onTextChanged(charSequence, i, i2, i3);
    }

    void spanChange(Spanned spanned, Object obj, int i, int i2, int i3, int i4) {
        int selectionEnd;
        boolean z;
        Editor.InputMethodState inputMethodState = this.mEditor == null ? null : this.mEditor.mInputMethodState;
        int selectionStart = -1;
        if (obj == Selection.SELECTION_END) {
            if (i >= 0 || i2 >= 0) {
                invalidateCursor(Selection.getSelectionStart(spanned), i, i2);
                checkForResize();
                registerForPreDraw();
                if (this.mEditor != null) {
                    this.mEditor.makeBlink();
                }
            }
            selectionEnd = i2;
            z = true;
        } else {
            selectionEnd = -1;
            z = false;
        }
        if (obj == Selection.SELECTION_START) {
            if (i >= 0 || i2 >= 0) {
                invalidateCursor(Selection.getSelectionEnd(spanned), i, i2);
            }
            selectionStart = i2;
            z = true;
        }
        if (z) {
            this.mHighlightPathBogus = true;
            if (this.mEditor != null && !isFocused()) {
                this.mEditor.mSelectionMoved = true;
            }
            if ((spanned.getSpanFlags(obj) & 512) == 0) {
                if (selectionStart < 0) {
                    selectionStart = Selection.getSelectionStart(spanned);
                }
                if (selectionEnd < 0) {
                    selectionEnd = Selection.getSelectionEnd(spanned);
                }
                if (this.mEditor != null) {
                    this.mEditor.refreshTextActionMode();
                    if (!hasSelection() && this.mEditor.getTextActionMode() == null && hasTransientState()) {
                        setHasTransientState(false);
                    }
                }
                onSelectionChanged(selectionStart, selectionEnd);
            }
        }
        if ((obj instanceof UpdateAppearance) || (obj instanceof ParagraphStyle) || (obj instanceof CharacterStyle)) {
            if (inputMethodState == null || inputMethodState.mBatchEditNesting == 0) {
                invalidate();
                this.mHighlightPathBogus = true;
                checkForResize();
            } else {
                inputMethodState.mContentChanged = true;
            }
            if (this.mEditor != null) {
                if (i >= 0) {
                    this.mEditor.invalidateTextDisplayList(this.mLayout, i, i3);
                }
                if (i2 >= 0) {
                    this.mEditor.invalidateTextDisplayList(this.mLayout, i2, i4);
                }
                this.mEditor.invalidateHandlesAndActionMode();
            }
        }
        if (MetaKeyKeyListener.isMetaTracker(spanned, obj)) {
            this.mHighlightPathBogus = true;
            if (inputMethodState != null && MetaKeyKeyListener.isSelectingMetaTracker(spanned, obj)) {
                inputMethodState.mSelectionModeChanged = true;
            }
            if (Selection.getSelectionStart(spanned) >= 0) {
                if (inputMethodState == null || inputMethodState.mBatchEditNesting == 0) {
                    invalidateCursor();
                } else {
                    inputMethodState.mCursorChanged = true;
                }
            }
        }
        if ((obj instanceof ParcelableSpan) && inputMethodState != null && inputMethodState.mExtractedTextRequest != null) {
            if (inputMethodState.mBatchEditNesting != 0) {
                if (i >= 0) {
                    if (inputMethodState.mChangedStart > i) {
                        inputMethodState.mChangedStart = i;
                    }
                    if (inputMethodState.mChangedStart > i3) {
                        inputMethodState.mChangedStart = i3;
                    }
                }
                if (i2 >= 0) {
                    if (inputMethodState.mChangedStart > i2) {
                        inputMethodState.mChangedStart = i2;
                    }
                    if (inputMethodState.mChangedStart > i4) {
                        inputMethodState.mChangedStart = i4;
                    }
                }
            } else {
                inputMethodState.mContentChanged = true;
            }
        }
        if (this.mEditor != null && this.mEditor.mSpellChecker != null && i2 < 0 && (obj instanceof SpellCheckSpan)) {
            this.mEditor.mSpellChecker.onSpellCheckSpanRemoved((SpellCheckSpan) obj);
        }
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (isTemporarilyDetached()) {
            super.onFocusChanged(z, i, rect);
            return;
        }
        if (this.mEditor != null) {
            this.mEditor.onFocusChanged(z, i);
        }
        if (z && this.mSpannable != null) {
            MetaKeyKeyListener.resetMetaState(this.mSpannable);
        }
        startStopMarquee(z);
        if (this.mTransformation != null) {
            this.mTransformation.onFocusChanged(this, this.mText, z, i, rect);
        }
        super.onFocusChanged(z, i, rect);
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (this.mEditor != null) {
            this.mEditor.onWindowFocusChanged(z);
        }
        startStopMarquee(z);
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (this.mEditor != null && i != 0) {
            this.mEditor.hideCursorAndSpanControllers();
            stopTextActionMode();
        }
    }

    public void clearComposingText() {
        if (this.mText instanceof Spannable) {
            BaseInputConnection.removeComposingSpans(this.mSpannable);
        }
    }

    @Override
    public void setSelected(boolean z) {
        boolean zIsSelected = isSelected();
        super.setSelected(z);
        if (z != zIsSelected && this.mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (z) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent;
        int actionMasked = motionEvent.getActionMasked();
        if (this.mEditor != null) {
            this.mEditor.onTouchEvent(motionEvent);
            if (this.mEditor.mSelectionModifierCursorController != null && this.mEditor.mSelectionModifierCursorController.isDragAcceleratorActive()) {
                return true;
            }
        }
        boolean zOnTouchEvent2 = super.onTouchEvent(motionEvent);
        if (this.mEditor != null && this.mEditor.mDiscardNextActionUp && actionMasked == 1) {
            this.mEditor.mDiscardNextActionUp = false;
            if (this.mEditor.mIsInsertionActionModeStartPending) {
                this.mEditor.startInsertionActionMode();
                this.mEditor.mIsInsertionActionModeStartPending = false;
            }
            return zOnTouchEvent2;
        }
        if (this.mEditor == null && this.mTextViewDiscardNextActionUp && actionMasked == 1) {
            this.mTextViewDiscardNextActionUp = false;
            return zOnTouchEvent2;
        }
        boolean z = actionMasked == 1 && (this.mEditor == null || !this.mEditor.mIgnoreActionUpEvent) && isFocused();
        if ((this.mMovement != null || onCheckIsTextEditor()) && isEnabled() && (this.mText instanceof Spannable) && this.mLayout != null) {
            if (this.mMovement != null) {
                zOnTouchEvent = this.mMovement.onTouchEvent(this, this.mSpannable, motionEvent) | false;
            } else {
                zOnTouchEvent = false;
            }
            boolean zIsTextSelectable = isTextSelectable();
            if (z && this.mLinksClickable && this.mAutoLinkMask != 0 && zIsTextSelectable) {
                ClickableSpan[] clickableSpanArr = (ClickableSpan[]) this.mSpannable.getSpans(getSelectionStart(), getSelectionEnd(), ClickableSpan.class);
                if (clickableSpanArr.length > 0) {
                    clickableSpanArr[0].onClick(this);
                    zOnTouchEvent = true;
                }
            }
            if (z && (isTextEditable() || zIsTextSelectable)) {
                InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                viewClicked(inputMethodManagerPeekInstance);
                if (isTextEditable() && this.mEditor.mShowSoftInputOnFocus && inputMethodManagerPeekInstance != null) {
                    inputMethodManagerPeekInstance.showSoftInput(this, 0);
                }
                this.mEditor.onTouchUpEvent(motionEvent);
                zOnTouchEvent = true;
            }
            if (zOnTouchEvent) {
                return true;
            }
        }
        return zOnTouchEvent2;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (this.mMovement != null && (this.mText instanceof Spannable) && this.mLayout != null) {
            try {
                if (this.mMovement.onGenericMotionEvent(this, this.mSpannable, motionEvent)) {
                    return true;
                }
            } catch (AbstractMethodError e) {
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    @Override
    protected void onCreateContextMenu(ContextMenu contextMenu) {
        if (this.mEditor != null) {
            this.mEditor.onCreateContextMenu(contextMenu);
        }
    }

    @Override
    public boolean showContextMenu() {
        if (this.mEditor != null) {
            this.mEditor.setContextMenuAnchor(Float.NaN, Float.NaN);
        }
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenu(float f, float f2) {
        if (this.mEditor != null) {
            this.mEditor.setContextMenuAnchor(f, f2);
        }
        return super.showContextMenu(f, f2);
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
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        if (this.mMovement != null && this.mSpannable != null && this.mLayout != null && this.mMovement.onTrackballEvent(this, this.mSpannable, motionEvent)) {
            return true;
        }
        return super.onTrackballEvent(motionEvent);
    }

    public void setScroller(Scroller scroller) {
        this.mScroller = scroller;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (isMarqueeFadeEnabled() && this.mMarquee != null && !this.mMarquee.isStopped()) {
            Marquee marquee = this.mMarquee;
            if (marquee.shouldDrawLeftFade()) {
                return getHorizontalFadingEdgeStrength(marquee.getScroll(), 0.0f);
            }
            return 0.0f;
        }
        if (getLineCount() == 1) {
            float lineLeft = getLayout().getLineLeft(0);
            if (lineLeft > this.mScrollX) {
                return 0.0f;
            }
            return getHorizontalFadingEdgeStrength(this.mScrollX, lineLeft);
        }
        return super.getLeftFadingEdgeStrength();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (isMarqueeFadeEnabled() && this.mMarquee != null && !this.mMarquee.isStopped()) {
            Marquee marquee = this.mMarquee;
            return getHorizontalFadingEdgeStrength(marquee.getMaxFadeScroll(), marquee.getScroll());
        }
        if (getLineCount() == 1) {
            float width = this.mScrollX + ((getWidth() - getCompoundPaddingLeft()) - getCompoundPaddingRight());
            float lineRight = getLayout().getLineRight(0);
            if (lineRight < width) {
                return 0.0f;
            }
            return getHorizontalFadingEdgeStrength(width, lineRight);
        }
        return super.getRightFadingEdgeStrength();
    }

    private float getHorizontalFadingEdgeStrength(float f, float f2) {
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
        if (horizontalFadingEdgeLength == 0) {
            return 0.0f;
        }
        float fAbs = Math.abs(f - f2);
        float f3 = horizontalFadingEdgeLength;
        if (fAbs > f3) {
            return 1.0f;
        }
        return fAbs / f3;
    }

    private boolean isMarqueeFadeEnabled() {
        return this.mEllipsize == TextUtils.TruncateAt.MARQUEE && this.mMarqueeFadeMode != 1;
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
        if (this.mLayout != null) {
            return this.mLayout.getHeight();
        }
        return super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return (getHeight() - getCompoundPaddingTop()) - getCompoundPaddingBottom();
    }

    @Override
    public void findViewsWithText(ArrayList<View> arrayList, CharSequence charSequence, int i) {
        super.findViewsWithText(arrayList, charSequence, i);
        if (!arrayList.contains(this) && (i & 1) != 0 && !TextUtils.isEmpty(charSequence) && !TextUtils.isEmpty(this.mText)) {
            if (this.mText.toString().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                arrayList.add(this);
            }
        }
    }

    public static ColorStateList getTextColors(Context context, TypedArray typedArray) {
        int resourceId;
        if (typedArray == null) {
            throw new NullPointerException();
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(android.R.styleable.TextView);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(5);
        if (colorStateList == null && (resourceId = typedArrayObtainStyledAttributes.getResourceId(1, 0)) != 0) {
            TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(resourceId, android.R.styleable.TextAppearance);
            colorStateList = typedArrayObtainStyledAttributes2.getColorStateList(3);
            typedArrayObtainStyledAttributes2.recycle();
        }
        typedArrayObtainStyledAttributes.recycle();
        return colorStateList;
    }

    public static int getTextColor(Context context, TypedArray typedArray, int i) {
        ColorStateList textColors = getTextColors(context, typedArray);
        if (textColors == null) {
            return i;
        }
        return textColors.getDefaultColor();
    }

    @Override
    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        if (keyEvent.hasModifiers(4096)) {
            if (i != 29) {
                if (i != 31) {
                    if (i != 50) {
                        if (i != 52) {
                            if (i == 54 && canUndo()) {
                                return onTextContextMenuItem(16908338);
                            }
                        } else if (canCut()) {
                            return onTextContextMenuItem(16908320);
                        }
                    } else if (canPaste()) {
                        return onTextContextMenuItem(16908322);
                    }
                } else if (canCopy()) {
                    return onTextContextMenuItem(16908321);
                }
            } else if (canSelectText()) {
                return onTextContextMenuItem(16908319);
            }
        } else if (keyEvent.hasModifiers(4097)) {
            if (i != 50) {
                if (i == 54 && canRedo()) {
                    return onTextContextMenuItem(16908339);
                }
            } else if (canPaste()) {
                return onTextContextMenuItem(16908337);
            }
        }
        return super.onKeyShortcut(i, keyEvent);
    }

    boolean canSelectText() {
        return (this.mText.length() == 0 || this.mEditor == null || !this.mEditor.hasSelectionController()) ? false : true;
    }

    boolean textCanBeSelected() {
        if (this.mMovement == null || !this.mMovement.canSelectArbitrarily()) {
            return false;
        }
        return isTextEditable() || (isTextSelectable() && (this.mText instanceof Spannable) && isEnabled());
    }

    private Locale getTextServicesLocale(boolean z) {
        updateTextServicesLocaleAsync();
        return (this.mCurrentSpellCheckerLocaleCache != null || z) ? this.mCurrentSpellCheckerLocaleCache : Locale.getDefault();
    }

    public final void setRestrictedAcrossUser(boolean z) {
        this.mIsRestrictedAcrossUser = z;
    }

    public Locale getTextServicesLocale() {
        return getTextServicesLocale(false);
    }

    public boolean isInExtractedMode() {
        return false;
    }

    private boolean isAutoSizeEnabled() {
        return supportsAutoSizeText() && this.mAutoSizeTextType != 0;
    }

    protected boolean supportsAutoSizeText() {
        return true;
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
        Locale localeObject;
        SpellCheckerSubtype currentSpellCheckerSubtype = ((TextServicesManager) this.mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)).getCurrentSpellCheckerSubtype(true);
        if (currentSpellCheckerSubtype != null) {
            localeObject = currentSpellCheckerSubtype.getLocaleObject();
        } else {
            localeObject = null;
        }
        this.mCurrentSpellCheckerLocaleCache = localeObject;
    }

    void onLocaleChanged() {
        this.mEditor.onLocaleChanged();
    }

    public WordIterator getWordIterator() {
        if (this.mEditor != null) {
            return this.mEditor.getWordIterator();
        }
        return null;
    }

    @Override
    public void onPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onPopulateAccessibilityEventInternal(accessibilityEvent);
        CharSequence textForAccessibility = getTextForAccessibility();
        if (!TextUtils.isEmpty(textForAccessibility)) {
            accessibilityEvent.getText().add(textForAccessibility);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TextView.class.getName();
    }

    @Override
    public void onProvideStructure(ViewStructure viewStructure) {
        super.onProvideStructure(viewStructure);
        onProvideAutoStructureForAssistOrAutofill(viewStructure, false);
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure viewStructure, int i) {
        super.onProvideAutofillStructure(viewStructure, i);
        onProvideAutoStructureForAssistOrAutofill(viewStructure, true);
    }

    private void onProvideAutoStructureForAssistOrAutofill(ViewStructure viewStructure, boolean z) {
        int lineAtCoordinateUnclamped;
        int lineAtCoordinateUnclamped2;
        boolean z2 = hasPasswordTransformationMethod() || isPasswordInputType(getInputType());
        if (z) {
            viewStructure.setDataIsSensitive(!this.mTextSetFromXmlOrResourceId);
            if (this.mTextId != 0) {
                try {
                    viewStructure.setTextIdEntry(getResources().getResourceEntryName(this.mTextId));
                } catch (Resources.NotFoundException e) {
                    if (Helper.sVerbose) {
                        Log.v(LOG_TAG, "onProvideAutofillStructure(): cannot set name for text id " + this.mTextId + ": " + e.getMessage());
                    }
                }
            }
        }
        if (!z2 || z) {
            if (this.mLayout == null) {
                assumeLayout();
            }
            Layout layout = this.mLayout;
            int lineCount = layout.getLineCount();
            if (lineCount <= 1) {
                CharSequence text = getText();
                if (z) {
                    viewStructure.setText(text);
                } else {
                    viewStructure.setText(text, getSelectionStart(), getSelectionEnd());
                }
            } else {
                int[] iArr = new int[2];
                getLocationInWindow(iArr);
                int i = iArr[1];
                ViewParent parent = getParent();
                View view = this;
                while (parent instanceof View) {
                    view = (View) parent;
                    parent = view.getParent();
                }
                int height = view.getHeight();
                if (i >= 0) {
                    lineAtCoordinateUnclamped2 = getLineAtCoordinateUnclamped(0.0f);
                    lineAtCoordinateUnclamped = getLineAtCoordinateUnclamped(height - 1);
                } else {
                    int lineAtCoordinateUnclamped3 = getLineAtCoordinateUnclamped(-i);
                    lineAtCoordinateUnclamped = getLineAtCoordinateUnclamped((height - 1) - i);
                    lineAtCoordinateUnclamped2 = lineAtCoordinateUnclamped3;
                }
                int i2 = lineAtCoordinateUnclamped - lineAtCoordinateUnclamped2;
                int i3 = i2 / 2;
                int i4 = lineAtCoordinateUnclamped2 - i3;
                if (i4 < 0) {
                    i4 = 0;
                }
                int i5 = i3 + lineAtCoordinateUnclamped;
                if (i5 >= lineCount) {
                    i5 = lineCount - 1;
                }
                int lineStart = layout.getLineStart(i4);
                int lineEnd = layout.getLineEnd(i5);
                int selectionStart = getSelectionStart();
                int selectionEnd = getSelectionEnd();
                if (selectionStart < selectionEnd) {
                    if (selectionStart < lineStart) {
                        lineStart = selectionStart;
                    }
                    if (selectionEnd > lineEnd) {
                        lineEnd = selectionEnd;
                    }
                }
                CharSequence text2 = getText();
                if (lineStart > 0 || lineEnd < text2.length()) {
                    text2 = text2.subSequence(lineStart, lineEnd);
                }
                if (z) {
                    viewStructure.setText(text2);
                } else {
                    viewStructure.setText(text2, selectionStart - lineStart, selectionEnd - lineStart);
                    int i6 = i2 + 1;
                    int[] iArr2 = new int[i6];
                    int[] iArr3 = new int[i6];
                    int baselineOffset = getBaselineOffset();
                    for (int i7 = lineAtCoordinateUnclamped2; i7 <= lineAtCoordinateUnclamped; i7++) {
                        int i8 = i7 - lineAtCoordinateUnclamped2;
                        iArr2[i8] = layout.getLineStart(i7);
                        iArr3[i8] = layout.getLineBaseline(i7) + baselineOffset;
                    }
                    viewStructure.setTextLines(iArr2, iArr3);
                }
            }
            if (!z) {
                int typefaceStyle = getTypefaceStyle();
                i = (typefaceStyle & 1) != 0 ? 1 : 0;
                if ((typefaceStyle & 2) != 0) {
                    i |= 2;
                }
                int flags = this.mTextPaint.getFlags();
                if ((flags & 32) != 0) {
                    i |= 1;
                }
                if ((flags & 8) != 0) {
                    i |= 4;
                }
                if ((flags & 16) != 0) {
                    i |= 8;
                }
                viewStructure.setTextStyle(getTextSize(), getCurrentTextColor(), 1, i);
            } else {
                viewStructure.setMinTextEms(getMinEms());
                viewStructure.setMaxTextEms(getMaxEms());
                int max = -1;
                InputFilter[] filters = getFilters();
                int length = filters.length;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    InputFilter inputFilter = filters[i];
                    if (!(inputFilter instanceof InputFilter.LengthFilter)) {
                        i++;
                    } else {
                        max = ((InputFilter.LengthFilter) inputFilter).getMax();
                        break;
                    }
                }
                viewStructure.setMaxTextLength(max);
            }
        }
        viewStructure.setHint(getHint());
        viewStructure.setInputType(getInputType());
    }

    boolean canRequestAutofill() {
        AutofillManager autofillManager;
        if (isAutofillable() && (autofillManager = (AutofillManager) this.mContext.getSystemService(AutofillManager.class)) != null) {
            return autofillManager.isEnabled();
        }
        return false;
    }

    private void requestAutofill() {
        AutofillManager autofillManager = (AutofillManager) this.mContext.getSystemService(AutofillManager.class);
        if (autofillManager != null) {
            autofillManager.requestAutofill(this);
        }
    }

    @Override
    public void autofill(AutofillValue autofillValue) {
        if (!autofillValue.isText() || !isTextEditable()) {
            Log.w(LOG_TAG, autofillValue + " could not be autofilled into " + this);
            return;
        }
        setText(autofillValue.getTextValue(), this.mBufferType, true, 0);
        CharSequence text = getText();
        if (text instanceof Spannable) {
            Selection.setSelection((Spannable) text, text.length());
        }
    }

    @Override
    public int getAutofillType() {
        return isTextEditable() ? 1 : 0;
    }

    @Override
    public AutofillValue getAutofillValue() {
        if (isTextEditable()) {
            return AutofillValue.forText(TextUtils.trimToParcelableSize(getText()));
        }
        return null;
    }

    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setPassword(hasPasswordTransformationMethod());
        if (accessibilityEvent.getEventType() == 8192) {
            accessibilityEvent.setFromIndex(Selection.getSelectionStart(this.mText));
            accessibilityEvent.setToIndex(Selection.getSelectionEnd(this.mText));
            accessibilityEvent.setItemCount(this.mText.length());
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.setPassword(hasPasswordTransformationMethod());
        accessibilityNodeInfo.setText(getTextForAccessibility());
        accessibilityNodeInfo.setHintText(this.mHint);
        accessibilityNodeInfo.setShowingHintText(isShowingHint());
        if (this.mBufferType == BufferType.EDITABLE) {
            accessibilityNodeInfo.setEditable(true);
            if (isEnabled()) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT);
            }
        }
        if (this.mEditor != null) {
            accessibilityNodeInfo.setInputType(this.mEditor.mInputType);
            if (this.mEditor.mError != null) {
                accessibilityNodeInfo.setContentInvalid(true);
                accessibilityNodeInfo.setError(this.mEditor.mError);
            }
        }
        if (!TextUtils.isEmpty(this.mText)) {
            accessibilityNodeInfo.addAction(256);
            accessibilityNodeInfo.addAction(512);
            accessibilityNodeInfo.setMovementGranularities(31);
            accessibilityNodeInfo.addAction(131072);
            accessibilityNodeInfo.setAvailableExtraData(Arrays.asList(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY));
        }
        if (isFocused()) {
            if (canCopy()) {
                accessibilityNodeInfo.addAction(16384);
            }
            if (canPaste()) {
                accessibilityNodeInfo.addAction(32768);
            }
            if (canCut()) {
                accessibilityNodeInfo.addAction(65536);
            }
            if (canShare()) {
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(268435456, getResources().getString(R.string.share)));
            }
            if (canProcessText()) {
                this.mEditor.mProcessTextIntentActionsHandler.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
            }
        }
        int length = this.mFilters.length;
        for (int i = 0; i < length; i++) {
            InputFilter inputFilter = this.mFilters[i];
            if (inputFilter instanceof InputFilter.LengthFilter) {
                accessibilityNodeInfo.setMaxTextLength(((InputFilter.LengthFilter) inputFilter).getMax());
            }
        }
        if (!isSingleLine()) {
            accessibilityNodeInfo.setMultiLine(true);
        }
    }

    @Override
    public void addExtraDataToAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo, String str, Bundle bundle) {
        RectF characterBounds;
        if (bundle != null && str.equals(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)) {
            int i = bundle.getInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, -1);
            int i2 = bundle.getInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, -1);
            if (i2 <= 0 || i < 0 || i >= this.mText.length()) {
                Log.e(LOG_TAG, "Invalid arguments for accessibility character locations");
                return;
            }
            RectF[] rectFArr = new RectF[i2];
            CursorAnchorInfo.Builder builder = new CursorAnchorInfo.Builder();
            populateCharacterBounds(builder, i, i + i2, viewportToContentHorizontalOffset(), viewportToContentVerticalOffset());
            CursorAnchorInfo cursorAnchorInfoBuild = builder.setMatrix(null).build();
            for (int i3 = 0; i3 < i2; i3++) {
                int i4 = i + i3;
                if ((cursorAnchorInfoBuild.getCharacterBoundsFlags(i4) & 1) == 1 && (characterBounds = cursorAnchorInfoBuild.getCharacterBounds(i4)) != null) {
                    mapRectFromViewToScreenCoords(characterBounds, true);
                    rectFArr[i3] = characterBounds;
                }
            }
            accessibilityNodeInfo.getExtras().putParcelableArray(str, rectFArr);
        }
    }

    public void populateCharacterBounds(CursorAnchorInfo.Builder builder, int i, int i2, float f, float f2) {
        int i3 = i;
        int i4 = i2;
        int lineForOffset = this.mLayout.getLineForOffset(i3);
        int lineForOffset2 = this.mLayout.getLineForOffset(i4 - 1);
        while (lineForOffset <= lineForOffset2) {
            int lineStart = this.mLayout.getLineStart(lineForOffset);
            int lineEnd = this.mLayout.getLineEnd(lineForOffset);
            int iMax = Math.max(lineStart, i3);
            int iMin = Math.min(lineEnd, i4);
            boolean z = this.mLayout.getParagraphDirection(lineForOffset) == 1;
            float[] fArr = new float[iMin - iMax];
            this.mLayout.getPaint().getTextWidths(this.mTransformed, iMax, iMin, fArr);
            float lineTop = this.mLayout.getLineTop(lineForOffset);
            float lineBottom = this.mLayout.getLineBottom(lineForOffset);
            int i5 = iMax;
            while (i5 < iMin) {
                float f3 = fArr[i5 - iMax];
                boolean zIsRtlCharAt = this.mLayout.isRtlCharAt(i5);
                float primaryHorizontal = this.mLayout.getPrimaryHorizontal(i5);
                float secondaryHorizontal = this.mLayout.getSecondaryHorizontal(i5);
                if (z) {
                    if (zIsRtlCharAt) {
                        primaryHorizontal = secondaryHorizontal - f3;
                    } else {
                        secondaryHorizontal = primaryHorizontal + f3;
                    }
                } else {
                    if (!zIsRtlCharAt) {
                        primaryHorizontal = secondaryHorizontal + f3;
                    } else {
                        secondaryHorizontal = primaryHorizontal - f3;
                    }
                    float f4 = primaryHorizontal;
                    primaryHorizontal = secondaryHorizontal;
                    secondaryHorizontal = f4;
                }
                float f5 = primaryHorizontal + f;
                float f6 = secondaryHorizontal + f;
                float f7 = lineTop + f2;
                float f8 = lineBottom + f2;
                boolean zIsPositionVisible = isPositionVisible(f5, f7);
                boolean zIsPositionVisible2 = isPositionVisible(f6, f8);
                int i6 = (zIsPositionVisible || zIsPositionVisible2) ? 1 : 0;
                if (!zIsPositionVisible || !zIsPositionVisible2) {
                    i6 |= 2;
                }
                int i7 = zIsRtlCharAt ? i6 | 4 : i6;
                int i8 = i5;
                builder.addCharacterBounds(i8, f5, f7, f6, f8, i7);
                i5 = i8 + 1;
            }
            lineForOffset++;
            i3 = i;
            i4 = i2;
        }
    }

    public boolean isPositionVisible(float f, float f2) {
        synchronized (TEMP_POSITION) {
            float[] fArr = TEMP_POSITION;
            fArr[0] = f;
            fArr[1] = f2;
            View view = this;
            while (view != null) {
                if (view != this) {
                    fArr[0] = fArr[0] - view.getScrollX();
                    fArr[1] = fArr[1] - view.getScrollY();
                }
                if (fArr[0] >= 0.0f && fArr[1] >= 0.0f && fArr[0] <= view.getWidth() && fArr[1] <= view.getHeight()) {
                    if (!view.getMatrix().isIdentity()) {
                        view.getMatrix().mapPoints(fArr);
                    }
                    fArr[0] = fArr[0] + view.getLeft();
                    fArr[1] = fArr[1] + view.getTop();
                    Object parent = view.getParent();
                    if (parent instanceof View) {
                        view = (View) parent;
                    } else {
                        view = null;
                    }
                }
                return false;
            }
            return true;
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        int i2;
        int i3;
        int length;
        if (this.mEditor != null && this.mEditor.mProcessTextIntentActionsHandler.performAccessibilityAction(i)) {
            return true;
        }
        if (i == 16) {
            return performAccessibilityActionClick(bundle);
        }
        if (i == 256 || i == 512) {
            ensureIterableTextForAccessibilitySelectable();
            return super.performAccessibilityActionInternal(i, bundle);
        }
        if (i == 16384) {
            return isFocused() && canCopy() && onTextContextMenuItem(16908321);
        }
        if (i == 32768) {
            return isFocused() && canPaste() && onTextContextMenuItem(16908322);
        }
        if (i == 65536) {
            return isFocused() && canCut() && onTextContextMenuItem(16908320);
        }
        if (i != 131072) {
            if (i != 2097152) {
                if (i != 268435456) {
                    return super.performAccessibilityActionInternal(i, bundle);
                }
                return isFocused() && canShare() && onTextContextMenuItem(16908341);
            }
            if (!isEnabled() || this.mBufferType != BufferType.EDITABLE) {
                return false;
            }
            setText(bundle != null ? bundle.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE) : null);
            if (this.mText != null && (length = this.mText.length()) > 0) {
                Selection.setSelection(this.mSpannable, length);
            }
            return true;
        }
        ensureIterableTextForAccessibilitySelectable();
        CharSequence iterableTextForAccessibility = getIterableTextForAccessibility();
        if (iterableTextForAccessibility == null) {
            return false;
        }
        if (bundle != null) {
            i2 = bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, -1);
        } else {
            i2 = -1;
        }
        if (bundle != null) {
            i3 = bundle.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, -1);
        } else {
            i3 = -1;
        }
        if (getSelectionStart() != i2 || getSelectionEnd() != i3) {
            if (i2 == i3 && i3 == -1) {
                Selection.removeSelection((Spannable) iterableTextForAccessibility);
                return true;
            }
            if (i2 >= 0 && i2 <= i3 && i3 <= iterableTextForAccessibility.length()) {
                Selection.setSelection((Spannable) iterableTextForAccessibility, i2, i3);
                if (this.mEditor != null) {
                    this.mEditor.startSelectionActionModeAsync(false);
                }
                return true;
            }
        }
        return false;
    }

    private boolean performAccessibilityActionClick(Bundle bundle) {
        boolean z;
        if (!isEnabled()) {
            return false;
        }
        if (isClickable() || isLongClickable()) {
            if (isFocusable() && !isFocused()) {
                requestFocus();
            }
            performClick();
            z = true;
        } else {
            z = false;
        }
        if ((this.mMovement != null || onCheckIsTextEditor()) && hasSpannableText() && this.mLayout != null) {
            if ((isTextEditable() || isTextSelectable()) && isFocused()) {
                InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
                viewClicked(inputMethodManagerPeekInstance);
                if (!isTextSelectable() && this.mEditor.mShowSoftInputOnFocus && inputMethodManagerPeekInstance != null) {
                    return z | inputMethodManagerPeekInstance.showSoftInput(this, 0);
                }
                return z;
            }
            return z;
        }
        return z;
    }

    private boolean hasSpannableText() {
        return this.mText != null && (this.mText instanceof Spannable);
    }

    @Override
    public void sendAccessibilityEventInternal(int i) {
        if (i == 32768 && this.mEditor != null) {
            this.mEditor.mProcessTextIntentActionsHandler.initializeAccessibilityActions();
        }
        super.sendAccessibilityEventInternal(i);
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == 4096) {
            return;
        }
        super.sendAccessibilityEventUnchecked(accessibilityEvent);
    }

    private CharSequence getTextForAccessibility() {
        if (TextUtils.isEmpty(this.mText)) {
            return this.mHint;
        }
        return TextUtils.trimToParcelableSize(this.mTransformed);
    }

    void sendAccessibilityEventTypeViewTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(16);
        accessibilityEventObtain.setFromIndex(i);
        accessibilityEventObtain.setRemovedCount(i2);
        accessibilityEventObtain.setAddedCount(i3);
        accessibilityEventObtain.setBeforeText(charSequence);
        sendAccessibilityEventUnchecked(accessibilityEventObtain);
    }

    public boolean isInputMethodTarget() {
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        return inputMethodManagerPeekInstance != null && inputMethodManagerPeekInstance.isActive(this);
    }

    public boolean onTextContextMenuItem(int i) {
        int iMax;
        int length = this.mText.length();
        if (isFocused()) {
            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();
            iMax = Math.max(0, Math.min(selectionStart, selectionEnd));
            length = Math.max(0, Math.max(selectionStart, selectionEnd));
        } else {
            iMax = 0;
        }
        if (i != 16908355) {
            switch (i) {
                case 16908319:
                    boolean zHasSelection = hasSelection();
                    selectAllText();
                    if (this.mEditor != null && zHasSelection) {
                        this.mEditor.invalidateActionModeAsync();
                    }
                    return true;
                case 16908320:
                    if (setPrimaryClip(ClipData.newPlainText(null, getTransformedText(iMax, length)))) {
                        deleteText_internal(iMax, length);
                    } else {
                        Toast.makeText(getContext(), R.string.failed_to_copy_to_clipboard, 0).show();
                    }
                    return true;
                case 16908321:
                    int selectionStart2 = getSelectionStart();
                    int selectionEnd2 = getSelectionEnd();
                    if (setPrimaryClip(ClipData.newPlainText(null, getTransformedText(Math.max(0, Math.min(selectionStart2, selectionEnd2)), Math.max(0, Math.max(selectionStart2, selectionEnd2)))))) {
                        stopTextActionMode();
                    } else {
                        Toast.makeText(getContext(), R.string.failed_to_copy_to_clipboard, 0).show();
                    }
                    return true;
                case 16908322:
                    paste(iMax, length, true);
                    return true;
                default:
                    switch (i) {
                        case 16908337:
                            paste(iMax, length, false);
                            return true;
                        case 16908338:
                            if (this.mEditor != null) {
                                this.mEditor.undo();
                            }
                            return true;
                        case 16908339:
                            if (this.mEditor != null) {
                                this.mEditor.redo();
                            }
                            return true;
                        case 16908340:
                            if (this.mEditor != null) {
                                this.mEditor.replace();
                            }
                            return true;
                        case 16908341:
                            shareSelectedText();
                            return true;
                        default:
                            return false;
                    }
            }
        }
        requestAutofill();
        stopTextActionMode();
        return true;
    }

    CharSequence getTransformedText(int i, int i2) {
        return removeSuggestionSpans(this.mTransformed.subSequence(i, i2));
    }

    @Override
    public boolean performLongClick() {
        boolean zPerformLongClick;
        if (this.mEditor != null) {
            this.mEditor.mIsBeingLongClicked = true;
        }
        if (super.performLongClick()) {
            this.mTextViewDiscardNextActionUp = true;
            zPerformLongClick = true;
        } else {
            zPerformLongClick = false;
        }
        boolean z = zPerformLongClick;
        if (this.mEditor != null) {
            zPerformLongClick |= this.mEditor.performLongClick(zPerformLongClick);
            this.mEditor.mIsBeingLongClicked = false;
        }
        if (zPerformLongClick) {
            if (!z) {
                performHapticFeedback(0);
            }
            if (this.mEditor != null) {
                this.mEditor.mDiscardNextActionUp = true;
            }
            this.mTextViewDiscardNextActionUp = true;
        } else {
            MetricsLogger.action(this.mContext, MetricsProto.MetricsEvent.TEXT_LONGPRESS, 0);
        }
        return zPerformLongClick;
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (this.mEditor != null) {
            this.mEditor.onScrollChanged();
        }
    }

    public boolean isSuggestionsEnabled() {
        if (this.mEditor == null || (this.mEditor.mInputType & 15) != 1 || (this.mEditor.mInputType & 524288) > 0) {
            return false;
        }
        int i = this.mEditor.mInputType & InputType.TYPE_MASK_VARIATION;
        return i == 0 || i == 48 || i == 80 || i == 64 || i == 160;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback callback) {
        createEditorIfNeeded();
        this.mEditor.mCustomSelectionActionModeCallback = callback;
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mCustomSelectionActionModeCallback;
    }

    public void setCustomInsertionActionModeCallback(ActionMode.Callback callback) {
        createEditorIfNeeded();
        this.mEditor.mCustomInsertionActionModeCallback = callback;
    }

    public ActionMode.Callback getCustomInsertionActionModeCallback() {
        if (this.mEditor == null) {
            return null;
        }
        return this.mEditor.mCustomInsertionActionModeCallback;
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        this.mTextClassifier = textClassifier;
    }

    public TextClassifier getTextClassifier() {
        if (this.mTextClassifier == null) {
            TextClassificationManager textClassificationManager = (TextClassificationManager) this.mContext.getSystemService(TextClassificationManager.class);
            if (textClassificationManager != null) {
                return textClassificationManager.getTextClassifier();
            }
            return TextClassifier.NO_OP;
        }
        return this.mTextClassifier;
    }

    TextClassifier getTextClassificationSession() {
        String str;
        if (this.mTextClassificationSession == null || this.mTextClassificationSession.isDestroyed()) {
            TextClassificationManager textClassificationManager = (TextClassificationManager) this.mContext.getSystemService(TextClassificationManager.class);
            if (textClassificationManager != null) {
                if (isTextEditable()) {
                    str = TextClassifier.WIDGET_TYPE_EDITTEXT;
                } else if (isTextSelectable()) {
                    str = TextClassifier.WIDGET_TYPE_TEXTVIEW;
                } else {
                    str = TextClassifier.WIDGET_TYPE_UNSELECTABLE_TEXTVIEW;
                }
                TextClassificationContext textClassificationContextBuild = new TextClassificationContext.Builder(this.mContext.getPackageName(), str).build();
                if (this.mTextClassifier != null) {
                    this.mTextClassificationSession = textClassificationManager.createTextClassificationSession(textClassificationContextBuild, this.mTextClassifier);
                } else {
                    this.mTextClassificationSession = textClassificationManager.createTextClassificationSession(textClassificationContextBuild);
                }
            } else {
                this.mTextClassificationSession = TextClassifier.NO_OP;
            }
        }
        return this.mTextClassificationSession;
    }

    boolean usesNoOpTextClassifier() {
        return getTextClassifier() == TextClassifier.NO_OP;
    }

    public boolean requestActionMode(TextLinks.TextLinkSpan textLinkSpan) {
        Preconditions.checkNotNull(textLinkSpan);
        if (!(this.mText instanceof Spanned)) {
            return false;
        }
        int spanStart = ((Spanned) this.mText).getSpanStart(textLinkSpan);
        int spanEnd = ((Spanned) this.mText).getSpanEnd(textLinkSpan);
        if (spanStart < 0 || spanEnd > this.mText.length() || spanStart >= spanEnd) {
            return false;
        }
        createEditorIfNeeded();
        this.mEditor.startLinkActionModeAsync(spanStart, spanEnd);
        return true;
    }

    public boolean handleClick(TextLinks.TextLinkSpan textLinkSpan) {
        Preconditions.checkNotNull(textLinkSpan);
        if (this.mText instanceof Spanned) {
            Spanned spanned = (Spanned) this.mText;
            int spanStart = spanned.getSpanStart(textLinkSpan);
            int spanEnd = spanned.getSpanEnd(textLinkSpan);
            if (spanStart >= 0 && spanEnd <= this.mText.length() && spanStart < spanEnd) {
                final TextClassification.Request requestBuild = new TextClassification.Request.Builder(this.mText, spanStart, spanEnd).setDefaultLocales(getTextLocales()).build();
                Supplier supplier = new Supplier() {
                    @Override
                    public final Object get() {
                        return this.f$0.getTextClassifier().classifyText(requestBuild);
                    }
                };
                CompletableFuture.supplyAsync(supplier).completeOnTimeout(null, 1L, TimeUnit.SECONDS).thenAccept((Consumer) new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        TextView.lambda$handleClick$1((TextClassification) obj);
                    }
                });
                return true;
            }
            return false;
        }
        return false;
    }

    static void lambda$handleClick$1(TextClassification textClassification) {
        if (textClassification != null) {
            if (!textClassification.getActions().isEmpty()) {
                try {
                    textClassification.getActions().get(0).getActionIntent().send();
                    return;
                } catch (PendingIntent.CanceledException e) {
                    Log.e(LOG_TAG, "Error sending PendingIntent", e);
                    return;
                }
            }
            Log.d(LOG_TAG, "No link action to perform");
            return;
        }
        Log.d(LOG_TAG, "Timeout while classifying text");
    }

    protected void stopTextActionMode() {
        if (this.mEditor != null) {
            this.mEditor.stopTextActionMode();
        }
    }

    public void hideFloatingToolbar(int i) {
        if (this.mEditor != null) {
            this.mEditor.hideFloatingToolbar(i);
        }
    }

    boolean canUndo() {
        return this.mEditor != null && this.mEditor.canUndo();
    }

    boolean canRedo() {
        return this.mEditor != null && this.mEditor.canRedo();
    }

    boolean canCut() {
        return (!this.mIsRestrictedAcrossUser || UserHandle.myUserId() == ActivityManager.getCurrentUser()) && !hasPasswordTransformationMethod() && this.mText.length() > 0 && hasSelection() && (this.mText instanceof Editable) && this.mEditor != null && this.mEditor.mKeyListener != null;
    }

    boolean canCopy() {
        return (!this.mIsRestrictedAcrossUser || UserHandle.myUserId() == ActivityManager.getCurrentUser()) && !hasPasswordTransformationMethod() && this.mText.length() > 0 && hasSelection() && this.mEditor != null;
    }

    boolean canShare() {
        if (!getContext().canStartActivityForResult() || !isDeviceProvisioned()) {
            return false;
        }
        return canCopy();
    }

    boolean isDeviceProvisioned() {
        if (this.mDeviceProvisionedState == 0) {
            this.mDeviceProvisionedState = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0 ? 2 : 1;
        }
        return this.mDeviceProvisionedState == 2;
    }

    boolean canPaste() {
        return (!this.mIsRestrictedAcrossUser || UserHandle.myUserId() == ActivityManager.getCurrentUser()) && (this.mText instanceof Editable) && this.mEditor != null && this.mEditor.mKeyListener != null && getSelectionStart() >= 0 && getSelectionEnd() >= 0 && ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).hasPrimaryClip();
    }

    boolean canPasteAsPlainText() {
        if (!canPaste()) {
            return false;
        }
        ClipData primaryClip = ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
        ClipDescription description = primaryClip.getDescription();
        boolean zHasMimeType = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        CharSequence text = primaryClip.getItemAt(0).getText();
        if (zHasMimeType && (text instanceof Spanned) && TextUtils.hasStyleSpan((Spanned) text)) {
            return true;
        }
        return description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML);
    }

    boolean canProcessText() {
        if (getId() == -1) {
            return false;
        }
        return canShare();
    }

    boolean canSelectAllText() {
        return (!canSelectText() || hasPasswordTransformationMethod() || (getSelectionStart() == 0 && getSelectionEnd() == this.mText.length())) ? false : true;
    }

    boolean selectAllText() {
        if (this.mEditor != null) {
            hideFloatingToolbar(500);
        }
        int length = this.mText.length();
        Selection.setSelection(this.mSpannable, 0, length);
        return length > 0;
    }

    void replaceSelectionWithText(CharSequence charSequence) {
        ((Editable) this.mText).replace(getSelectionStart(), getSelectionEnd(), charSequence);
    }

    private void paste(int i, int i2, boolean z) {
        CharSequence charSequenceCoerceToText;
        ClipData primaryClip = ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
        if (primaryClip != null) {
            boolean z2 = false;
            for (int i3 = 0; i3 < primaryClip.getItemCount(); i3++) {
                if (z) {
                    charSequenceCoerceToText = primaryClip.getItemAt(i3).coerceToStyledText(getContext());
                } else {
                    charSequenceCoerceToText = primaryClip.getItemAt(i3).coerceToText(getContext());
                    if (charSequenceCoerceToText instanceof Spanned) {
                        charSequenceCoerceToText = charSequenceCoerceToText.toString();
                    }
                }
                if (charSequenceCoerceToText != null) {
                    if (!z2) {
                        Selection.setSelection(this.mSpannable, i2);
                        ((Editable) this.mText).replace(i, i2, charSequenceCoerceToText);
                        z2 = true;
                    } else {
                        ((Editable) this.mText).insert(getSelectionEnd(), "\n");
                        ((Editable) this.mText).insert(getSelectionEnd(), charSequenceCoerceToText);
                    }
                }
            }
            sLastCutCopyOrTextChangedTime = 0L;
        }
    }

    private void shareSelectedText() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            intent.removeExtra(Intent.EXTRA_TEXT);
            intent.putExtra(Intent.EXTRA_TEXT, (String) TextUtils.trimToParcelableSize(selectedText));
            getContext().startActivity(Intent.createChooser(intent, null));
            Selection.setSelection(this.mSpannable, getSelectionEnd());
        }
    }

    private boolean setPrimaryClip(ClipData clipData) {
        try {
            ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clipData);
            sLastCutCopyOrTextChangedTime = SystemClock.uptimeMillis();
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public int getOffsetForPosition(float f, float f2) {
        if (getLayout() == null) {
            return -1;
        }
        return getOffsetAtCoordinate(getLineAtCoordinate(f2), f);
    }

    float convertToLocalHorizontalCoordinate(float f) {
        return Math.min((getWidth() - getTotalPaddingRight()) - 1, Math.max(0.0f, f - getTotalPaddingLeft())) + getScrollX();
    }

    int getLineAtCoordinate(float f) {
        return getLayout().getLineForVertical((int) (Math.min((getHeight() - getTotalPaddingBottom()) - 1, Math.max(0.0f, f - getTotalPaddingTop())) + getScrollY()));
    }

    int getLineAtCoordinateUnclamped(float f) {
        return getLayout().getLineForVertical((int) ((f - getTotalPaddingTop()) + getScrollY()));
    }

    int getOffsetAtCoordinate(int i, float f) {
        return getLayout().getOffsetForHorizontal(i, convertToLocalHorizontalCoordinate(f));
    }

    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        int action = dragEvent.getAction();
        if (action != 5) {
            switch (action) {
                case 1:
                    if (this.mEditor == null || !this.mEditor.hasInsertionController()) {
                    }
                    break;
                case 2:
                    if (this.mText instanceof Spannable) {
                        Selection.setSelection(this.mSpannable, getOffsetForPosition(dragEvent.getX(), dragEvent.getY()));
                    }
                    break;
                case 3:
                    if (this.mEditor != null) {
                        this.mEditor.onDrop(dragEvent);
                    }
                    break;
            }
            return true;
        }
        requestFocus();
        return true;
    }

    boolean isInBatchEditMode() {
        if (this.mEditor == null) {
            return false;
        }
        Editor.InputMethodState inputMethodState = this.mEditor.mInputMethodState;
        if (inputMethodState != null) {
            return inputMethodState.mBatchEditNesting > 0;
        }
        return this.mEditor.mInBatchEditControllers;
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        TextDirectionHeuristic textDirectionHeuristic = getTextDirectionHeuristic();
        if (this.mTextDir != textDirectionHeuristic) {
            this.mTextDir = textDirectionHeuristic;
            if (this.mLayout != null) {
                checkForRelayout();
            }
        }
    }

    protected TextDirectionHeuristic getTextDirectionHeuristic() {
        if (hasPasswordTransformationMethod()) {
            return TextDirectionHeuristics.LTR;
        }
        if (this.mEditor != null && (this.mEditor.mInputType & 15) == 3) {
            byte directionality = Character.getDirectionality(DecimalFormatSymbols.getInstance(getTextLocale()).getDigitStrings()[0].codePointAt(0));
            if (directionality == 1 || directionality == 2) {
                return TextDirectionHeuristics.RTL;
            }
            return TextDirectionHeuristics.LTR;
        }
        boolean z = getLayoutDirection() == 1;
        switch (getTextDirection()) {
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            default:
                if (!z) {
                }
                break;
        }
        return TextDirectionHeuristics.LTR;
    }

    @Override
    public void onResolveDrawables(int i) {
        if (this.mLastLayoutDirection == i) {
            return;
        }
        this.mLastLayoutDirection = i;
        if (this.mDrawables != null && this.mDrawables.resolveWithLayoutDirection(i)) {
            prepareDrawableForDisplay(this.mDrawables.mShowing[0]);
            prepareDrawableForDisplay(this.mDrawables.mShowing[2]);
            applyCompoundDrawableTint();
        }
    }

    private void prepareDrawableForDisplay(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        drawable.setLayoutDirection(getLayoutDirection());
        if (drawable.isStateful()) {
            drawable.setState(getDrawableState());
            drawable.jumpToCurrentState();
        }
    }

    @Override
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();
        this.mLastLayoutDirection = -1;
    }

    protected void viewClicked(InputMethodManager inputMethodManager) {
        if (inputMethodManager != null) {
            inputMethodManager.viewClicked(this);
        }
    }

    protected void deleteText_internal(int i, int i2) {
        ((Editable) this.mText).delete(i, i2);
    }

    protected void replaceText_internal(int i, int i2, CharSequence charSequence) {
        ((Editable) this.mText).replace(i, i2, charSequence);
    }

    protected void setSpan_internal(Object obj, int i, int i2, int i3) {
        ((Editable) this.mText).setSpan(obj, i, i2, i3);
    }

    protected void setCursorPosition_internal(int i, int i2) {
        Selection.setSelection((Editable) this.mText, i, i2);
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
    public AccessibilityIterators.TextSegmentIterator getIteratorForGranularity(int i) {
        if (i == 4) {
            Spannable spannable = (Spannable) getIterableTextForAccessibility();
            if (!TextUtils.isEmpty(spannable) && getLayout() != null) {
                AccessibilityIterators.LineTextSegmentIterator lineTextSegmentIterator = AccessibilityIterators.LineTextSegmentIterator.getInstance();
                lineTextSegmentIterator.initialize(spannable, getLayout());
                return lineTextSegmentIterator;
            }
        } else if (i == 16 && !TextUtils.isEmpty((Spannable) getIterableTextForAccessibility()) && getLayout() != null) {
            AccessibilityIterators.PageTextSegmentIterator pageTextSegmentIterator = AccessibilityIterators.PageTextSegmentIterator.getInstance();
            pageTextSegmentIterator.initialize(this);
            return pageTextSegmentIterator;
        }
        return super.getIteratorForGranularity(i);
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
    public void setAccessibilitySelection(int i, int i2) {
        if (getAccessibilitySelectionStart() == i && getAccessibilitySelectionEnd() == i2) {
            return;
        }
        CharSequence iterableTextForAccessibility = getIterableTextForAccessibility();
        if (Math.min(i, i2) >= 0 && Math.max(i, i2) <= iterableTextForAccessibility.length()) {
            Selection.setSelection((Spannable) iterableTextForAccessibility, i, i2);
        } else {
            Selection.removeSelection((Spannable) iterableTextForAccessibility);
        }
        if (this.mEditor != null) {
            this.mEditor.hideCursorAndSpanControllers();
            this.mEditor.stopTextActionMode();
        }
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        TextUtils.TruncateAt ellipsize = getEllipsize();
        viewHierarchyEncoder.addProperty("text:ellipsize", ellipsize == null ? null : ellipsize.name());
        viewHierarchyEncoder.addProperty("text:textSize", getTextSize());
        viewHierarchyEncoder.addProperty("text:scaledTextSize", getScaledTextSize());
        viewHierarchyEncoder.addProperty("text:typefaceStyle", getTypefaceStyle());
        viewHierarchyEncoder.addProperty("text:selectionStart", getSelectionStart());
        viewHierarchyEncoder.addProperty("text:selectionEnd", getSelectionEnd());
        viewHierarchyEncoder.addProperty("text:curTextColor", this.mCurTextColor);
        viewHierarchyEncoder.addProperty("text:text", this.mText != null ? this.mText.toString() : null);
        viewHierarchyEncoder.addProperty("text:gravity", this.mGravity);
    }

    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        ParcelableParcel editorState;
        CharSequence error;
        boolean frozenWithFocus;
        int selEnd;
        int selStart;
        CharSequence text;

        SavedState(Parcelable parcelable) {
            super(parcelable);
            this.selStart = -1;
            this.selEnd = -1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.selStart);
            parcel.writeInt(this.selEnd);
            parcel.writeInt(this.frozenWithFocus ? 1 : 0);
            TextUtils.writeToParcel(this.text, parcel, i);
            if (this.error == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                TextUtils.writeToParcel(this.error, parcel, i);
            }
            if (this.editorState == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                this.editorState.writeToParcel(parcel, i);
            }
        }

        public String toString() {
            String str = "TextView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " start=" + this.selStart + " end=" + this.selEnd;
            if (this.text != null) {
                str = str + " text=" + ((Object) this.text);
            }
            return str + "}";
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.selStart = -1;
            this.selEnd = -1;
            this.selStart = parcel.readInt();
            this.selEnd = parcel.readInt();
            this.frozenWithFocus = parcel.readInt() != 0;
            this.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            if (parcel.readInt() != 0) {
                this.error = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            }
            if (parcel.readInt() != 0) {
                this.editorState = ParcelableParcel.CREATOR.createFromParcel(parcel);
            }
        }
    }

    private static class CharWrapper implements CharSequence, GetChars, GraphicsOperations {
        private char[] mChars;
        private int mLength;
        private int mStart;

        public CharWrapper(char[] cArr, int i, int i2) {
            this.mChars = cArr;
            this.mStart = i;
            this.mLength = i2;
        }

        void set(char[] cArr, int i, int i2) {
            this.mChars = cArr;
            this.mStart = i;
            this.mLength = i2;
        }

        @Override
        public int length() {
            return this.mLength;
        }

        @Override
        public char charAt(int i) {
            return this.mChars[i + this.mStart];
        }

        @Override
        public String toString() {
            return new String(this.mChars, this.mStart, this.mLength);
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            if (i < 0 || i2 < 0 || i > this.mLength || i2 > this.mLength) {
                throw new IndexOutOfBoundsException(i + ", " + i2);
            }
            return new String(this.mChars, this.mStart + i, i2 - i);
        }

        @Override
        public void getChars(int i, int i2, char[] cArr, int i3) {
            if (i < 0 || i2 < 0 || i > this.mLength || i2 > this.mLength) {
                throw new IndexOutOfBoundsException(i + ", " + i2);
            }
            System.arraycopy(this.mChars, this.mStart + i, cArr, i3, i2 - i);
        }

        @Override
        public void drawText(BaseCanvas baseCanvas, int i, int i2, float f, float f2, Paint paint) {
            baseCanvas.drawText(this.mChars, i + this.mStart, i2 - i, f, f2, paint);
        }

        @Override
        public void drawTextRun(BaseCanvas baseCanvas, int i, int i2, int i3, int i4, float f, float f2, boolean z, Paint paint) {
            baseCanvas.drawTextRun(this.mChars, this.mStart + i, i2 - i, i3 + this.mStart, i4 - i3, f, f2, z, paint);
        }

        @Override
        public float measureText(int i, int i2, Paint paint) {
            return paint.measureText(this.mChars, this.mStart + i, i2 - i);
        }

        @Override
        public int getTextWidths(int i, int i2, float[] fArr, Paint paint) {
            return paint.getTextWidths(this.mChars, this.mStart + i, i2 - i, fArr);
        }

        @Override
        public float getTextRunAdvances(int i, int i2, int i3, int i4, boolean z, float[] fArr, int i5, Paint paint) {
            return paint.getTextRunAdvances(this.mChars, this.mStart + i, i2 - i, i3 + this.mStart, i4 - i3, z, fArr, i5);
        }

        @Override
        public int getTextRunCursor(int i, int i2, int i3, int i4, int i5, Paint paint) {
            return paint.getTextRunCursor(this.mChars, i + this.mStart, i2 - i, i3, i4 + this.mStart, i5);
        }
    }

    private static final class Marquee {
        private static final int MARQUEE_DELAY = 1200;
        private static final float MARQUEE_DELTA_MAX = 0.07f;
        private static final int MARQUEE_DP_PER_SECOND = 30;
        private static final byte MARQUEE_RUNNING = 2;
        private static final byte MARQUEE_STARTING = 1;
        private static final byte MARQUEE_STOPPED = 0;
        private float mFadeStop;
        private float mGhostOffset;
        private float mGhostStart;
        private long mLastAnimationMs;
        private float mMaxFadeScroll;
        private float mMaxScroll;
        private final float mPixelsPerMs;
        private int mRepeatLimit;
        private float mScroll;
        private final WeakReference<TextView> mView;
        private byte mStatus = 0;
        private Choreographer.FrameCallback mTickCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long j) {
                Marquee.this.tick();
            }
        };
        private Choreographer.FrameCallback mStartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long j) {
                Marquee.this.mStatus = (byte) 2;
                Marquee.this.mLastAnimationMs = Marquee.this.mChoreographer.getFrameTime();
                Marquee.this.tick();
            }
        };
        private Choreographer.FrameCallback mRestartCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long j) {
                if (Marquee.this.mStatus == 2) {
                    if (Marquee.this.mRepeatLimit >= 0) {
                        Marquee.access$910(Marquee.this);
                    }
                    Marquee.this.start(Marquee.this.mRepeatLimit);
                }
            }
        };
        private final Choreographer mChoreographer = Choreographer.getInstance();

        static int access$910(Marquee marquee) {
            int i = marquee.mRepeatLimit;
            marquee.mRepeatLimit = i - 1;
            return i;
        }

        Marquee(TextView textView) {
            this.mPixelsPerMs = (30.0f * textView.getContext().getResources().getDisplayMetrics().density) / 1000.0f;
            this.mView = new WeakReference<>(textView);
        }

        void tick() {
            if (this.mStatus != 2) {
                return;
            }
            this.mChoreographer.removeFrameCallback(this.mTickCallback);
            TextView textView = this.mView.get();
            if (textView != null) {
                if (textView.isFocused() || textView.isSelected()) {
                    long frameTime = this.mChoreographer.getFrameTime();
                    long j = frameTime - this.mLastAnimationMs;
                    this.mLastAnimationMs = frameTime;
                    this.mScroll += j * this.mPixelsPerMs;
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

        void start(int i) {
            if (i == 0) {
                stop();
                return;
            }
            this.mRepeatLimit = i;
            TextView textView = this.mView.get();
            if (textView != null && textView.mLayout != null) {
                this.mStatus = (byte) 1;
                this.mScroll = 0.0f;
                int width = (textView.getWidth() - textView.getCompoundPaddingLeft()) - textView.getCompoundPaddingRight();
                float lineWidth = textView.mLayout.getLineWidth(0);
                float f = width;
                float f2 = f / 3.0f;
                this.mGhostStart = (lineWidth - f) + f2;
                this.mMaxScroll = this.mGhostStart + f;
                this.mGhostOffset = f2 + lineWidth;
                this.mFadeStop = (f / 6.0f) + lineWidth;
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
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            if (AccessibilityManager.getInstance(TextView.this.mContext).isEnabled() && TextView.this.mTransformed != null) {
                this.mBeforeText = TextView.this.mTransformed.toString();
            }
            TextView.this.sendBeforeTextChanged(charSequence, i, i2, i3);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            TextView.this.handleTextChanged(charSequence, i, i2, i3);
            if (AccessibilityManager.getInstance(TextView.this.mContext).isEnabled()) {
                if (TextView.this.isFocused() || (TextView.this.isSelected() && TextView.this.isShown())) {
                    TextView.this.sendAccessibilityEventTypeViewTextChanged(this.mBeforeText, i, i2, i3);
                    this.mBeforeText = null;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            TextView.this.sendAfterTextChanged(editable);
            if (MetaKeyKeyListener.getMetaState(editable, 2048) != 0) {
                MetaKeyKeyListener.stopSelecting(TextView.this, editable);
            }
        }

        @Override
        public void onSpanChanged(Spannable spannable, Object obj, int i, int i2, int i3, int i4) {
            TextView.this.spanChange(spannable, obj, i, i3, i2, i4);
        }

        @Override
        public void onSpanAdded(Spannable spannable, Object obj, int i, int i2) {
            TextView.this.spanChange(spannable, obj, -1, i, -1, i2);
        }

        @Override
        public void onSpanRemoved(Spannable spannable, Object obj, int i, int i2) {
            TextView.this.spanChange(spannable, obj, i, -1, i2, -1);
        }
    }
}
