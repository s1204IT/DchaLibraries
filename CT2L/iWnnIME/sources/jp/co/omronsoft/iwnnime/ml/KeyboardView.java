package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.ExploreByTouchHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class KeyboardView extends View implements View.OnClickListener {
    protected static final int BITMAP_MIN_SIZE = 1;
    private static final String CRITERIA_KEY_WIDTH = "M";
    private static final String CRITERIA_KEY_WIDTH_10KEY_ALPHA = "WXYZ";
    private static final String CRITERIA_KEY_WIDTH_10KEY_NUMBER = "＜＄￥＞";
    private static final int DEBOUNCE_TIME = 70;
    protected static final boolean DEBUG = false;
    private static final float DEFAULT_KEY_SIZE_RATE = 1.0f;
    private static final int DELAY_AFTER_PREVIEW = 70;
    private static final int DELAY_BEFORE_PREVIEW = 0;
    protected static final int DUMMY_POPUP_RESID = 1;
    private static final int INPUT_MODE_KEY_DOWN_SCALE_LENGTH_THRESHOLD = 3;
    private static final float INPUT_MODE_KEY_DOWN_SCALE_RATE = 0.8f;
    private static final float KEY10_HINT_AREA_WIDTH_RATIO = 3.5f;
    private static final float KEY10_HINT_OFFSET_Y_RATIO = 0.125f;
    private static final float KEY10_LABEL_MIN_OFFSET_Y_RATIO = 0.083333336f;
    public static final int KEYTOP_DRAWABLE_LAYER_NUMBER = 3;
    public static final int KEYTOP_LAYER_INDEX_BACKGROUND = 0;
    public static final int KEYTOP_LAYER_INDEX_CENTER = 2;
    public static final int KEYTOP_LAYER_INDEX_STROKE = 1;
    private static final float KEY_TOP_ALPHA_TEXT_RATE = 0.8f;
    public static final int MODE_LABEL_MAX_NUM = 3;
    private static final int MSG_LONGPRESS = 4;
    private static final int MSG_REMOVE_PREVIEW = 2;
    private static final int MSG_REPEAT = 3;
    private static final int MSG_SHOW_PREVIEW = 1;
    private static final int MULTITAP_INTERVAL = 800;
    private static final int NONSELECTED_MODE_STROKE_WITH = 1;
    protected static final int NOT_A_KEY = -1;
    protected static final int POPUP_OFFSET = 20;
    protected static final float RATE_HEIGHT_KEY_PREVIEW = 1.5f;
    public static final int REPEAT_INTERVAL = 50;
    public static final int REPEAT_START_DELAY = 400;
    private static final int SELECTED_MODE_STROKE_WITH = 2;
    public static final int SLIDE_POPUP_ID = 1;
    public static final int SLIDE_POPUP_NORMAL = -1;
    public static final int SLIDE_POPUP_RESOURCE_ID = 0;
    protected static final String SPLIT_KEYWORD_NEW_LINE = "\n";
    protected static final String SPLIT_KEYWORD_OFFSET = "\\\\offset";
    protected static final String SPLIT_KEYWORD_SCALE = "\\\\scale";
    private static final double VERSION_KEYTOPIMAGE_PREMIT_SCALE = 2.3d;
    private float m50KeyScaleRatio;
    private boolean mAbortKey;
    private float mBackgroundDimAmount;
    private Bitmap mBuffer;
    private Canvas mCanvas;
    private Rect mClipRegion;
    private int mCurrentKey;
    private int mCurrentKeyIndex;
    private long mCurrentKeyTime;
    protected ArrayList<Path> mDebugFlickArea;
    private float mDefaultScaleRate;
    private Rect mDirtyRect;
    private boolean mDisambiguateSwipe;
    private int[] mDistances;
    private int mDownKey;
    private long mDownTime;
    private ArrayList<Drawable> mDrawList;
    private boolean mDrawPending;
    protected boolean mEnableMushroom;
    private GestureDetector mGestureDetector;
    private Handler mHandler;
    private Paint mHintPaint;
    public boolean mIgnoreTouchEvent;
    private boolean mInMultiTap;
    private Keyboard.Key mInvalidatedKey;
    protected boolean mIsInputTypeNull;
    private Drawable mKeyBackground;
    private Drawable mKeyBackground2nd;
    private int mKeyBackgroundColorEnter;
    private Drawable mKeyBackgroundEnter;
    private Drawable mKeyBackgroundSpace;
    private int[] mKeyCloseTextColor;
    private String mKeyHintEllipsisChar;
    private int[] mKeyHintEllipsisColor;
    private float mKeyHintEllipsisPadding;
    private float mKeyHintEllipsisRatio;
    private int[] mKeyHintLetterColor;
    private float mKeyHintLetterPaddingRightBg1st;
    private float mKeyHintLetterPaddingRightBg2nd;
    private float mKeyHintLetterPaddingTopBg1st;
    private float mKeyHintLetterPaddingTopBg2nd;
    private float mKeyHintLetterRatio;
    private int[] mKeyIndices;
    private int[] mKeyTextColor;
    private int[] mKeyTextColor2nd;
    private int mKeyTextSize;
    private float mKeyTextStrokeRatio;
    protected Keyboard mKeyboard;
    private OnKeyboardActionListener mKeyboardActionListener;
    private Drawable mKeyboardBackground;
    private boolean mKeyboardChanged;
    private Keyboard.Key[] mKeys;
    private int mLabelTextSize;
    private int mLastCodeX;
    private int mLastCodeY;
    private int mLastKey;
    private long mLastKeyTime;
    private long mLastMoveTime;
    private int mLastSentIndex;
    private long mLastTapTime;
    private int mLastX;
    private int mLastY;
    private KeyboardView mMiniKeyboard;
    private Map<Keyboard.Key, View> mMiniKeyboardCache;
    private View mMiniKeyboardContainer;
    private int mMiniKeyboardOffsetX;
    private int mMiniKeyboardOffsetY;
    protected boolean mMiniKeyboardOnScreen;
    private int[] mOffsetInWindow;
    private int mOldPointerCount;
    private Paint mPaint;
    private PopupWindow mPopupKeyboard;
    private int mPopupLayout;
    protected View mPopupParent;
    private int mPopupPreviewX;
    private int mPopupPreviewY;
    private boolean mPossiblePoly;
    private boolean mPreviewCentered;
    protected int mPreviewHeight;
    private StringBuilder mPreviewLabel;
    protected int mPreviewOffset;
    private PopupWindow mPreviewPopup;
    private TextView mPreviewText;
    private int mPreviewTextSizeLarge;
    private boolean mProximityCorrectOn;
    private int mProximityThreshold;
    private int mRepeatKeyIndex;
    private boolean mShowPreview;
    private int mSlidePopupBackgroundColorFocused;
    private int mSlidePopupBackgroundColorNormal;
    private int mSlidePopupColorFocused;
    private int mSlidePopupColorNormal;
    private ArrayList<View> mSlidePopupDisplayList;
    protected LinearLayout mSlidePopupLayout;
    protected int mSlidePopupWidth;
    private int mStartX;
    private int mStartY;
    private int mSwipeThreshold;
    private SwipeTracker mSwipeTracker;
    private int mTapCount;
    protected float mTextScaleRate;
    protected int mTouchKeyCode;
    protected int mVerticalCorrection;
    public static final int[][] SLIDE_POPUP_TABLE = {new int[]{R.string.ti_slide_popup_mode_normal_txt, -1}, new int[]{R.string.ti_slide_popup_mode_hira_txt, 3}, new int[]{R.string.ti_slide_popup_mode_half_alpha_txt, 0}, new int[]{R.string.ti_slide_popup_mode_half_num_txt, 1}};
    private static final int[] LONG_PRESSABLE_STATE_SET = {android.R.attr.state_long_pressable};
    private static final HashMap<Integer, Float> sTextWidthCache = new HashMap<>();
    private static final char[] KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR = {'8'};
    public static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static int MAX_NEARBY_KEYS = 12;
    private static final Rect sTextBounds = new Rect();

    public interface OnKeyboardActionListener {
        boolean onKey(int i, int[] iArr);

        void onPress(int i);

        void onRelease(int i);

        void onText(CharSequence charSequence);

        void swipeDown();

        void swipeLeft();

        void swipeRight();

        void swipeUp();
    }

    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyle) {
        int tempLabelTextSize;
        int tempKeyTextSize;
        super(context, attrs, defStyle);
        this.mCurrentKeyIndex = -1;
        this.mPreviewCentered = DEBUG;
        this.mShowPreview = true;
        this.mCurrentKey = -1;
        this.mDownKey = -1;
        this.mKeyIndices = new int[12];
        this.mRepeatKeyIndex = -1;
        this.mClipRegion = new Rect(0, 0, 0, 0);
        this.mSwipeTracker = new SwipeTracker();
        this.mOldPointerCount = 1;
        this.mDistances = new int[MAX_NEARBY_KEYS];
        this.mPreviewLabel = new StringBuilder(1);
        this.mDirtyRect = new Rect();
        this.mKeyboardBackground = null;
        this.mTouchKeyCode = -1;
        this.mEnableMushroom = DEBUG;
        this.mIsInputTypeNull = DEBUG;
        this.mDebugFlickArea = new ArrayList<>();
        this.mIgnoreTouchEvent = DEBUG;
        this.mSlidePopupDisplayList = new ArrayList<>();
        this.mDrawList = new ArrayList<>();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        KeyboardView.this.showKey(msg.arg1);
                        break;
                    case 2:
                        KeyboardView.this.mPreviewText.setVisibility(4);
                        break;
                    case 3:
                        if (KeyboardView.this.repeatKey()) {
                            Message repeat = Message.obtain(this, 3);
                            sendMessageDelayed(repeat, 50L);
                        }
                        break;
                    case 4:
                        KeyboardView.this.openPopupIfRequired((MotionEvent) msg.obj);
                        break;
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WnnKeyboardView, defStyle, R.style.WnnKeyboardView);
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        int previewLayout = 0;
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    this.mKeyBackground = a.getDrawable(attr);
                    break;
                case 1:
                    this.mVerticalCorrection = a.getDimensionPixelSize(attr, 0);
                    break;
                case 2:
                    previewLayout = a.getResourceId(attr, 0);
                    break;
                case 3:
                    this.mPreviewOffset = a.getDimensionPixelSize(attr, 0);
                    break;
                case 4:
                    this.mPreviewHeight = a.getDimensionPixelSize(attr, 80);
                    break;
                case 5:
                    this.mKeyTextSize = a.getDimensionPixelSize(attr, 18);
                    if (langPack.isValid() && (tempKeyTextSize = langPack.getDimen(R.dimen.key_text_size_default)) != 0) {
                        this.mKeyTextSize = tempKeyTextSize;
                    }
                    break;
                case 7:
                    this.mLabelTextSize = a.getDimensionPixelSize(attr, 14);
                    if (langPack.isValid() && (tempLabelTextSize = langPack.getDimen(R.dimen.key_label_text_size)) != 0) {
                        this.mLabelTextSize = tempLabelTextSize;
                    }
                    break;
                case 8:
                    this.mPopupLayout = a.getResourceId(attr, 0);
                    break;
            }
        }
        a.recycle();
        TypedArray a2 = context.obtainStyledAttributes(attrs, R.styleable.WnnKeyboardView, 0, 0);
        this.mKeyBackground2nd = a2.getDrawable(29);
        this.mBackgroundDimAmount = a2.getFloat(28, 0.5f);
        a2.recycle();
        Resources res = context.getResources();
        if (res != null) {
            this.mKeyBackgroundSpace = res.getDrawable(R.drawable.key_space_keytop_back);
            float keyHintLetterPadding = res.getDimensionPixelSize(R.dimen.key_hint_letter_padding);
            this.mKeyHintLetterPaddingRightBg1st = keyHintLetterPadding;
            this.mKeyHintLetterPaddingRightBg2nd = keyHintLetterPadding;
            float keyHintLetterTopPadding = res.getDimensionPixelSize(R.dimen.key_hint_letter_top_padding);
            this.mKeyHintLetterPaddingTopBg1st = keyHintLetterTopPadding;
            this.mKeyHintLetterPaddingTopBg2nd = keyHintLetterTopPadding;
            this.mKeyHintLetterRatio = res.getFraction(R.fraction.key_hint_letter_ratio, IWnnSymbolEngine.MAX_ITEM_IN_PAGE, IWnnSymbolEngine.MAX_ITEM_IN_PAGE) / 1000.0f;
            this.mKeyHintEllipsisPadding = res.getFraction(R.fraction.key_hint_ellipsis_padding_ratio, IWnnSymbolEngine.MAX_ITEM_IN_PAGE, IWnnSymbolEngine.MAX_ITEM_IN_PAGE) / 1000.0f;
            this.mKeyHintEllipsisRatio = res.getFraction(R.fraction.key_hint_ellipsis_ratio, IWnnSymbolEngine.MAX_ITEM_IN_PAGE, IWnnSymbolEngine.MAX_ITEM_IN_PAGE) / 1000.0f;
            this.mKeyHintEllipsisChar = context.getString(R.string.ti_key_hint_ellipsis_txt);
            this.mKeyTextStrokeRatio = res.getFraction(R.fraction.key_text_stroke_ratio, IWnnSymbolEngine.MAX_ITEM_IN_PAGE, IWnnSymbolEngine.MAX_ITEM_IN_PAGE) / 1000.0f;
            this.m50KeyScaleRatio = res.getFraction(R.fraction.keyboard_50key_scale_ratio, IWnnSymbolEngine.MAX_ITEM_IN_PAGE, IWnnSymbolEngine.MAX_ITEM_IN_PAGE) / 1000.0f;
            this.mPreviewPopup = new PopupWindow(context);
            if (previewLayout != 0) {
                LayoutInflater inflate = (LayoutInflater) context.getSystemService("layout_inflater");
                if (inflate != null) {
                    this.mPreviewText = (TextView) inflate.inflate(previewLayout, (ViewGroup) null);
                    this.mPreviewTextSizeLarge = (int) this.mPreviewText.getTextSize();
                    this.mPreviewPopup.setContentView(this.mPreviewText);
                    this.mPreviewPopup.setBackgroundDrawable(null);
                }
            } else {
                this.mShowPreview = DEBUG;
            }
            this.mPreviewPopup.setTouchable(DEBUG);
            this.mPopupKeyboard = new PopupWindow(context);
            this.mPopupKeyboard.setBackgroundDrawable(null);
            this.mPopupParent = this;
            this.mPaint = new Paint();
            this.mPaint.setAntiAlias(true);
            this.mPaint.setTextSize(0);
            this.mPaint.setTextAlign(Paint.Align.CENTER);
            this.mPaint.setAlpha(255);
            this.mHintPaint = new Paint();
            this.mHintPaint.setAntiAlias(true);
            this.mHintPaint.setTypeface(Typeface.DEFAULT_BOLD);
            this.mMiniKeyboardCache = new HashMap();
            this.mSwipeThreshold = (int) (500.0f * getResources().getDisplayMetrics().density);
            this.mDisambiguateSwipe = true;
            resetMultiTap();
            initGestureDetector();
            if (getId() == R.id.popup_keyboard_view) {
                this.mKeyTextColor = new int[3];
                this.mKeyTextColor[2] = res.getColor(R.color.key_text_color_mini_keyboard);
                this.mKeyTextColor[1] = res.getColor(R.color.key_text_color_mini_keyboard_stroke);
                return;
            }
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            if (resMan != null) {
                Drawable keyboardbg = resMan.getKeyboardBg();
                if (keyboardbg != null) {
                    setBackgroundDrawable(keyboardbg);
                    if (Build.VERSION.SDK_INT >= 21 && (keyboardbg instanceof AnimatedStateListDrawable)) {
                        AnimatedStateListDrawable background = (AnimatedStateListDrawable) keyboardbg;
                        AnimationDrawable anime = (AnimationDrawable) background.getCurrent();
                        anime.start();
                    }
                }
                Drawable keybg = resMan.getKeyBg();
                if (keybg != null) {
                    this.mKeyBackground = keybg;
                    this.mKeyBackground2nd = keybg;
                    this.mKeyHintLetterPaddingRightBg1st = 0.0f;
                    this.mKeyHintLetterPaddingRightBg2nd = 0.0f;
                    this.mKeyHintLetterPaddingTopBg1st = 0.0f;
                    this.mKeyHintLetterPaddingTopBg2nd = 0.0f;
                }
                Drawable keybg2nd = resMan.getKeyBg2nd();
                if (keybg2nd != null) {
                    this.mKeyBackground2nd = keybg2nd;
                    this.mKeyHintLetterPaddingRightBg2nd = 0.0f;
                    this.mKeyHintLetterPaddingTopBg2nd = 0.0f;
                }
                this.mKeyTextColor = resMan.getTextColor(context, R.color.key_text_color);
                this.mKeyTextColor2nd = resMan.getTextColor(context, R.color.key_text_color_2nd);
                this.mKeyCloseTextColor = resMan.getTextColor(context, R.color.key_text_color_close);
                this.mKeyHintLetterColor = resMan.getTextColor(context, R.color.key_hint_letter);
                this.mKeyHintEllipsisColor = resMan.getTextColor(context, R.color.key_hint_ellipsis_color);
                this.mSlidePopupBackgroundColorNormal = resMan.getColor(context, R.color.slide_popup_background_color);
                this.mSlidePopupBackgroundColorFocused = resMan.getColor(context, R.color.slide_popup_background_color_focused);
                this.mSlidePopupColorNormal = resMan.getColor(context, R.color.slide_popup_top_color);
                this.mSlidePopupColorFocused = resMan.getColor(context, R.color.slide_popup_top_color_focused);
                this.mKeyBackgroundColorEnter = resMan.getColor(context, R.color.key_background_color_enter);
                int spaceColor = resMan.getColor(context, R.color.key_background_color_space);
                this.mKeyBackgroundSpace.setColorFilter(spaceColor, PorterDuff.Mode.SRC_IN);
                this.mKeyBackgroundEnter = res.getDrawable(R.drawable.key_enter_keytop_back);
            }
        }
    }

    private void initGestureDetector() {
        this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
                if (KeyboardView.this.mPossiblePoly) {
                    return KeyboardView.DEBUG;
                }
                float absX = Math.abs(velocityX);
                float absY = Math.abs(velocityY);
                float deltaX = me2.getX() - me1.getX();
                float deltaY = me2.getY() - me1.getY();
                int travelX = KeyboardView.this.getWidth() / 2;
                int travelY = KeyboardView.this.getHeight() / 2;
                KeyboardView.this.mSwipeTracker.computeCurrentVelocity(IWnnSymbolEngine.MAX_ITEM_IN_PAGE);
                float endingVelocityX = KeyboardView.this.mSwipeTracker.getXVelocity();
                float endingVelocityY = KeyboardView.this.mSwipeTracker.getYVelocity();
                boolean sendDownKey = KeyboardView.DEBUG;
                if (velocityX <= KeyboardView.this.mSwipeThreshold || absY >= absX || deltaX <= travelX) {
                    if (velocityX >= (-KeyboardView.this.mSwipeThreshold) || absY >= absX || deltaX >= (-travelX)) {
                        if (velocityY >= (-KeyboardView.this.mSwipeThreshold) || absX >= absY || deltaY >= (-travelY)) {
                            if (velocityY > KeyboardView.this.mSwipeThreshold && absX < absY / 2.0f && deltaY > travelY) {
                                if (KeyboardView.this.mDisambiguateSwipe && endingVelocityY < velocityY / 4.0f) {
                                    sendDownKey = true;
                                } else {
                                    KeyboardView.this.swipeDown();
                                    return true;
                                }
                            }
                        } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityY > velocityY / 4.0f) {
                            sendDownKey = true;
                        } else {
                            KeyboardView.this.swipeUp();
                            return true;
                        }
                    } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityX > velocityX / 4.0f) {
                        sendDownKey = true;
                    } else {
                        KeyboardView.this.swipeLeft();
                        return true;
                    }
                } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityX < velocityX / 4.0f) {
                    sendDownKey = true;
                } else {
                    KeyboardView.this.swipeRight();
                    return true;
                }
                if (sendDownKey) {
                    KeyboardView.this.detectAndSendKey(KeyboardView.this.mDownKey, KeyboardView.this.mStartX, KeyboardView.this.mStartY, me1.getEventTime());
                }
                return KeyboardView.DEBUG;
            }
        });
        this.mGestureDetector.setIsLongpressEnabled(DEBUG);
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        this.mKeyboardActionListener = listener;
    }

    protected OnKeyboardActionListener getOnKeyboardActionListener() {
        return this.mKeyboardActionListener;
    }

    public void setKeyboard(Keyboard keyboard) {
        int keyIndex;
        int oldRepeatKeyCode = -1;
        if (this.mKeyboard != null) {
            showPreview(-1);
            if (this.mRepeatKeyIndex != -1 && this.mRepeatKeyIndex < this.mKeys.length) {
                oldRepeatKeyCode = this.mKeys[this.mRepeatKeyIndex].codes[0];
            }
        }
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(1);
        Keyboard lastKeyboard = this.mKeyboard;
        this.mKeyboard = keyboard;
        List<Keyboard.Key> keys = this.mKeyboard.getKeys();
        this.mKeys = (Keyboard.Key[]) keys.toArray(new Keyboard.Key[keys.size()]);
        requestLayout();
        this.mKeyboardChanged = true;
        invalidateAllKeys();
        computeProximityThreshold(keyboard);
        this.mMiniKeyboardCache.clear();
        boolean abort = true;
        if (oldRepeatKeyCode != -1 && (keyIndex = getKeyIndices(this.mStartX, this.mStartY, null)) != -1 && keyIndex < this.mKeys.length && oldRepeatKeyCode == this.mKeys[keyIndex].codes[0]) {
            abort = DEBUG;
            this.mRepeatKeyIndex = keyIndex;
        }
        if (abort) {
            this.mHandler.removeMessages(3);
        }
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn == null) {
            this.mAbortKey = abort;
        } else {
            DefaultSoftKeyboard softKeyboard = wnn.getCurrentDefaultSoftKeyboard();
            Keyboard noInput = softKeyboard.getKeyboardInputted(DEBUG);
            Keyboard inputted = softKeyboard.getKeyboardInputted(true);
            if ((keyboard != noInput && keyboard != inputted) || (lastKeyboard != noInput && lastKeyboard != inputted)) {
                this.mAbortKey = abort;
            }
        }
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        int keyboard_4key = R.xml.keyboard_4key;
        if (langPack.isValid()) {
            keyboard_4key = 12;
        }
        if (keyboard.getXmlLayoutResId() == keyboard_4key) {
            if (lastKeyboard == null || lastKeyboard.getXmlLayoutResId() != keyboard_4key) {
                this.mKeyboardBackground = null;
                clearWindowInfo();
                KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                Drawable background = resMan.getKeyboardBg1Line();
                if (background != null) {
                    this.mKeyboardBackground = getBackground();
                    setBackgroundDrawable(background);
                    return;
                }
                return;
            }
            return;
        }
        if (lastKeyboard != null && lastKeyboard.getXmlLayoutResId() == keyboard_4key) {
            clearWindowInfo();
        }
        if (this.mKeyboardBackground != null) {
            setBackgroundDrawable(this.mKeyboardBackground);
            this.mKeyboardBackground = null;
        }
    }

    public Keyboard getKeyboard() {
        return this.mKeyboard;
    }

    public boolean setShifted(boolean shifted) {
        if (this.mKeyboard == null || !this.mKeyboard.setShifted(shifted)) {
            return DEBUG;
        }
        invalidateAllKeys();
        return true;
    }

    public boolean isShifted() {
        return this.mKeyboard != null ? this.mKeyboard.isShifted() : DEBUG;
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        this.mShowPreview = previewEnabled;
    }

    protected boolean isPreviewEnabled() {
        return this.mShowPreview;
    }

    public boolean isParentPreviewEnabled() {
        return (this.mPopupParent == null || this.mPopupParent == this || !(this.mPopupParent instanceof KeyboardView)) ? this.mShowPreview : ((KeyboardView) this.mPopupParent).isParentPreviewEnabled();
    }

    public void setVerticalCorrection(int verticalOffset) {
    }

    public void setPopupParent(View v) {
        this.mPopupParent = v;
    }

    public void setPopupOffset(int x, int y) {
        this.mMiniKeyboardOffsetX = x;
        this.mMiniKeyboardOffsetY = y;
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
    }

    public void setProximityCorrectionEnabled(boolean enabled) {
        this.mProximityCorrectOn = enabled;
    }

    public boolean isProximityCorrectionEnabled() {
        return this.mProximityCorrectOn;
    }

    @Override
    public void onClick(View v) {
        dismissPopupKeyboard();
    }

    private CharSequence adjustCase(CharSequence label) {
        if (this.mKeyboard.isShifted() && label != null && label.length() < 3 && Character.isLowerCase(label.charAt(0))) {
            return iWnnEngine.getEngine().toUpperCase(label.toString());
        }
        return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int kbdPaddingLeft = getPaddingLeft();
        if (this.mKeyboard == null) {
            setMeasuredDimension(getPaddingRight() + kbdPaddingLeft, getPaddingTop() + getPaddingBottom());
            return;
        }
        if (this.mKeyboard.getMiniKeyboardWidth() > 0) {
            width = this.mKeyboard.getMiniKeyboardWidth() + kbdPaddingLeft + getPaddingRight();
        } else {
            width = View.MeasureSpec.getSize(widthMeasureSpec);
        }
        setMeasuredDimension(width, this.mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
    }

    private void computeProximityThreshold(Keyboard keyboard) {
        Keyboard.Key[] keys;
        if (keyboard != null && (keys = this.mKeys) != null) {
            int length = keys.length;
            int dimensionSum = 0;
            for (Keyboard.Key key : keys) {
                dimensionSum += Math.min(key.width, key.height) + key.gap;
            }
            if (dimensionSum >= 0 && length != 0) {
                this.mProximityThreshold = (int) ((dimensionSum * 1.4f) / length);
                this.mProximityThreshold *= this.mProximityThreshold;
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mBuffer = null;
        clearWindowInfo();
    }

    @Override
    public void onDraw(Canvas canvas) {
        this.mIgnoreTouchEvent = DEBUG;
        super.onDraw(canvas);
        if (this.mDrawPending || this.mBuffer == null || this.mKeyboardChanged) {
            onBufferDraw();
        }
        if (this.mBuffer != null) {
            canvas.drawBitmap(this.mBuffer, 0.0f, 0.0f, (Paint) null);
        }
    }

    private void onBufferDraw() {
        Context context;
        Resources res;
        Configuration config;
        IWnnIME wnn;
        Drawable keyBackground;
        float keyHintLetterPaddingRight;
        float keyHintLetterPaddingTop;
        Drawable skinBackGround;
        if (this.mKeyboard != null && this.mPaint != null && (context = getContext()) != null && (res = context.getResources()) != null && (config = res.getConfiguration()) != null && (wnn = IWnnIME.getCurrentIme()) != null) {
            boolean isBufferNull = this.mBuffer == null ? true : DEBUG;
            if (isBufferNull || this.mKeyboardChanged) {
                if (isBufferNull || (this.mKeyboardChanged && (this.mBuffer.getWidth() != getWidth() || this.mBuffer.getHeight() != getHeight()))) {
                    int width = Math.max(1, getWidth());
                    int height = Math.max(1, getHeight());
                    this.mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    this.mCanvas = new Canvas(this.mBuffer);
                }
                invalidateAllKeys();
                this.mKeyboardChanged = DEBUG;
            }
            Canvas canvas = this.mCanvas;
            canvas.clipRect(this.mDirtyRect, Region.Op.REPLACE);
            Paint paint = this.mPaint;
            Paint hintPaint = this.mHintPaint;
            Rect clipRegion = this.mClipRegion;
            int kbdPaddingLeft = getPaddingLeft();
            int kbdPaddingTop = getPaddingTop();
            Keyboard.Key[] keys = this.mKeys;
            Keyboard.Key invalidKey = this.mInvalidatedKey;
            boolean drawSingleKey = DEBUG;
            if (invalidKey != null && canvas.getClipBounds(clipRegion) && (invalidKey.x + kbdPaddingLeft) - 1 <= clipRegion.left && (invalidKey.y + kbdPaddingTop) - 1 <= clipRegion.top && invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right && invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true;
            }
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            KeyboardManager km = wnn.getCurrentKeyboardManager();
            int maxHeight = km.getMaxKeyboardHeight();
            int defaultHeight = km.getDefaultKeyboardHeight();
            float baseScaleRate = config.fontScale;
            int keyboardType = this.mKeyboard.getKeyboardType();
            if (keyboardType == 3) {
                baseScaleRate *= this.m50KeyScaleRatio;
            }
            PointF rate = km.getKeyboardScaleRate();
            float scaleRate = Math.min(rate.x, rate.y) * baseScaleRate;
            float keyPaddingRatioX = rate.x;
            float keyPaddingRatioY = rate.y;
            this.mDefaultScaleRate = defaultHeight / maxHeight;
            KeyboardSkinData skin = KeyboardSkinData.getInstance();
            boolean isSkinNotPermitScale = (!skin.isValid() || skin.getTargetVersion() >= VERSION_KEYTOPIMAGE_PREMIT_SCALE) ? DEBUG : true;
            float textScaleRate = scaleRate;
            if (isSkinNotPermitScale) {
                textScaleRate = baseScaleRate * this.mDefaultScaleRate;
            }
            this.mTextScaleRate = textScaleRate;
            canvas.translate(kbdPaddingLeft, kbdPaddingTop);
            for (Keyboard.Key key : keys) {
                if (key != null && (!drawSingleKey || invalidKey == key)) {
                    boolean isSpaceKeyBackground = DEBUG;
                    if (key.isSecondKey) {
                        keyBackground = this.mKeyBackground2nd;
                        keyHintLetterPaddingRight = this.mKeyHintLetterPaddingRightBg2nd;
                        keyHintLetterPaddingTop = this.mKeyHintLetterPaddingTopBg2nd;
                    } else {
                        keyBackground = this.mKeyBackground;
                        keyHintLetterPaddingRight = this.mKeyHintLetterPaddingRightBg1st;
                        keyHintLetterPaddingTop = this.mKeyHintLetterPaddingTopBg1st;
                    }
                    int keycode = 0;
                    if (key.codes.length > 0) {
                        keycode = key.codes[0];
                        if (skin.isValid() && (skinBackGround = skin.getKeyBg(context, this.mKeyboard, keycode, key.isSecondKey)) != null) {
                            keyBackground = skinBackGround;
                        }
                        if ((keycode == -121 || keycode == 32) && keyboardType == 0 && key.width >= this.mKeyboard.getKeyWidth() * 2 && this.mKeyBackgroundSpace != null) {
                            isSpaceKeyBackground = true;
                        }
                    }
                    if (keyBackground != null) {
                        int[] drawableState = new int[0];
                        if (!isPreviewEnabled() || WnnUtility.isFunctionKey(key)) {
                            drawableState = key.getCurrentDrawableState();
                        }
                        keyBackground.setState(drawableState);
                        Rect padding = new Rect();
                        keyBackground.getPadding(padding);
                        key.contentWidth = (key.width - padding.left) - padding.right;
                        if (key.contentWidth < 1) {
                            key.contentWidth = key.width;
                        }
                        key.contentHeight = (key.height - padding.top) - padding.bottom;
                        if (key.contentHeight < 1) {
                            key.contentHeight = key.height;
                        }
                        String label = key.label == null ? null : key.label.toString();
                        Rect bounds = keyBackground.getBounds();
                        if (key.width != bounds.right || key.height != bounds.bottom) {
                            keyBackground.setBounds(0, 0, key.width, key.height);
                            if (isSpaceKeyBackground) {
                                this.mKeyBackgroundSpace.setBounds(0, 0, key.width, key.height);
                            }
                        }
                        canvas.translate(key.x, key.y);
                        keyBackground.draw(canvas);
                        if (isSpaceKeyBackground) {
                            this.mKeyBackgroundSpace.draw(canvas);
                        }
                        canvas.translate(padding.left, padding.top);
                        int[] color = getColor(key);
                        updateKeyPopupResId(key);
                        boolean isKeyHintNormal = DEBUG;
                        boolean isKeyHintEllipsis = DEBUG;
                        if (key.hintLabel != null) {
                            if (key.hintLabel.equals(this.mKeyHintEllipsisChar)) {
                                isKeyHintEllipsis = true;
                            } else {
                                isKeyHintNormal = true;
                            }
                        } else if (WnnUtility.isFunctionKey(key) && key.popupResId != 0) {
                            isKeyHintEllipsis = true;
                        }
                        boolean isDrawTextInputModeKey = DEBUG;
                        if (keycode == -114) {
                            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                            Drawable skinIcon = resMan.getDrawable(keycode, this.mKeyboard);
                            if (skinIcon != null) {
                                key.icon = skinIcon;
                                key.mIsIconSkin = true;
                            } else {
                                isDrawTextInputModeKey = true;
                            }
                        }
                        if (!isSpaceKeyBackground) {
                            if (isDrawTextInputModeKey) {
                                float ellipsisLabelHeight = isKeyHintEllipsis ? key.contentHeight * this.mKeyHintEllipsisPadding * keyPaddingRatioY : 0.0f;
                                drawTextInputModeKey(canvas, key, this.mLabelTextSize * scaleRate, ellipsisLabelHeight);
                            } else if (key.icon != null) {
                                float drawableScaleRate = scaleRate;
                                if (isSkinNotPermitScale) {
                                    if (key.mIsIconSkin) {
                                        drawableScaleRate = DEFAULT_KEY_SIZE_RATE;
                                    } else {
                                        drawableScaleRate = this.mDefaultScaleRate;
                                    }
                                }
                                drawKeyIcon(canvas, key, keycode, drawableScaleRate, res, color);
                            } else if (label != null) {
                                Bitmap bitmap = Bitmap.createBitmap(key.contentWidth, key.contentHeight, Bitmap.Config.ARGB_8888);
                                Canvas keyTopCanvas = new Canvas(bitmap);
                                float textSize = this.mKeyTextSize;
                                if (WnnUtility.isFunctionKey(key)) {
                                    textSize = this.mLabelTextSize;
                                }
                                paint.setTextSize(textSize * textScaleRate);
                                float hintSize = this.mLabelTextSize * this.mKeyHintLetterRatio * textScaleRate;
                                hintPaint.setTextSize(hintSize);
                                if (keyboardType == 1) {
                                    draw12Key(keyTopCanvas, key, label, textScaleRate);
                                } else {
                                    drawQwerty(keyTopCanvas, key, label, isKeyHintNormal, keyHintLetterPaddingRight, keyHintLetterPaddingTop, keyPaddingRatioX, keyPaddingRatioY);
                                }
                                Drawable keyTop = new BitmapDrawable(res, bitmap);
                                keyTop.setBounds(0, 0, keyTop.getIntrinsicWidth(), keyTop.getIntrinsicHeight());
                                keyTop.draw(canvas);
                                if (keyboardType == 1 && !key.mIsPreviewSkin) {
                                    key.iconPreview = keyTop;
                                }
                            }
                        }
                        if (isKeyHintEllipsis) {
                            hintPaint.setTextAlign(Paint.Align.CENTER);
                            float hintSize2 = this.mLabelTextSize * this.mKeyHintEllipsisRatio * textScaleRate;
                            hintPaint.setTextSize(hintSize2);
                            float posX = (key.contentWidth - ((key.contentWidth * this.mKeyHintEllipsisPadding) * keyPaddingRatioX)) - (getCharWidth(KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR, hintPaint) / 2.0f);
                            float posY = key.contentHeight - ((key.contentHeight * this.mKeyHintEllipsisPadding) * keyPaddingRatioY);
                            CharSequence hint = this.mKeyHintEllipsisChar;
                            drawTextWithStroke(hint.toString(), posX, posY, hintPaint, this.mKeyHintEllipsisColor, canvas);
                        }
                        canvas.translate(-padding.left, -padding.top);
                        canvas.translate(-key.x, -key.y);
                    } else {
                        return;
                    }
                }
            }
            canvas.translate(-kbdPaddingLeft, -kbdPaddingTop);
            this.mInvalidatedKey = null;
            int overlayColor = ((int) (this.mBackgroundDimAmount * 255.0f)) << 24;
            if (this.mMiniKeyboardOnScreen) {
                paint.setColor(overlayColor);
                canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), paint);
            }
            km.setColorOverlay(overlayColor);
            this.mDrawPending = DEBUG;
            this.mDirtyRect.setEmpty();
        }
    }

    protected boolean draw2line(Canvas canvas, Keyboard.Key key, String label, Paint paint, int[] color) {
        if (canvas == null || key == null || label == null || paint == null) {
            return DEBUG;
        }
        String[] strAry = label.split(SPLIT_KEYWORD_NEW_LINE);
        if (strAry.length < 2) {
            return DEBUG;
        }
        StringBuilder upperStringBuilder = new StringBuilder(strAry[0]);
        draw2line(canvas, key, upperStringBuilder, paint, color, true);
        StringBuilder lowerStringBuilder = new StringBuilder(strAry[1]);
        draw2line(canvas, key, lowerStringBuilder, paint, color, DEBUG);
        return true;
    }

    private void draw2line(Canvas canvas, Keyboard.Key key, StringBuilder label, Paint paint, int[] color, boolean isUpper) {
        if (canvas != null && key != null && label != null && paint != null) {
            float offsetScale = getScaleFromString(label, SPLIT_KEYWORD_OFFSET);
            float textScale = getScaleFromString(label, SPLIT_KEYWORD_SCALE);
            paint.setTextSize(paint.getTextSize() * textScale);
            resizeKeyTopWidth(paint, null, key, paint.measureText(label.toString()));
            float totalLabelHeight = (-paint.ascent()) + paint.descent();
            resizeKeyTopHeight(paint, null, key, totalLabelHeight, true);
            float offset = (isUpper ? -paint.descent() : -paint.ascent()) * offsetScale;
            String text = label.toString();
            if (color != null) {
                drawTextWithStroke(text, 0.0f, offset, paint, color, canvas, key.contentWidth, true);
            } else {
                canvas.drawText(text, 0.0f, offset, paint);
            }
        }
    }

    private float getScaleFromString(StringBuilder label, String regularExpression) {
        float scale = DEFAULT_KEY_SIZE_RATE;
        if (label == null || regularExpression == null) {
            return DEFAULT_KEY_SIZE_RATE;
        }
        String[] strAry = label.toString().split(regularExpression);
        if (strAry != null && strAry.length >= 2) {
            scale = Float.parseFloat(strAry[1]);
            label.delete(0, label.length());
            label.append(strAry[0]);
        }
        return scale;
    }

    private void draw12Key(Canvas canvas, Keyboard.Key key, String label, float textScaleRate) {
        if (canvas != null && key != null && label != null && this.mKeyboard != null && this.mPaint != null && this.mHintPaint != null) {
            Resources res = getResources();
            if (res != null) {
                Paint paint = this.mPaint;
                Paint hintPaint = this.mHintPaint;
                int[] color = getColor(key);
                float keyTopCenterX = key.contentWidth / 2.0f;
                float keyTopCenterY = key.contentHeight / 2.0f;
                float textSize = paint.getTextSize();
                float criteriaSize = paint.measureText(label);
                if (!WnnUtility.isFunctionKey(key)) {
                    int keyMode = this.mKeyboard.getKeyboardMode();
                    switch (keyMode) {
                        case 0:
                        case 6:
                            textSize *= 0.8f;
                            criteriaSize = paint.measureText(CRITERIA_KEY_WIDTH_10KEY_ALPHA);
                            break;
                        case 1:
                        case 7:
                            criteriaSize = hintPaint.measureText(CRITERIA_KEY_WIDTH_10KEY_NUMBER);
                            break;
                        case 3:
                            criteriaSize = hintPaint.getTextSize() * KEY10_HINT_AREA_WIDTH_RATIO;
                            break;
                        case 4:
                        case 5:
                            criteriaSize = paint.measureText(CRITERIA_KEY_WIDTH);
                            break;
                    }
                }
                paint.setTextSize(textSize);
                canvas.translate(keyTopCenterX, keyTopCenterY);
                if (!draw2line(canvas, key, label, paint, color)) {
                    resizeKeyTopWidth(paint, hintPaint, key, criteriaSize);
                    float mainLabelHeight = (-paint.ascent()) + paint.descent();
                    float hintLabelHeight = (-hintPaint.ascent()) + hintPaint.descent();
                    float totalLabelHeight = mainLabelHeight + hintLabelHeight;
                    resizeKeyTopHeight(paint, hintPaint, key, totalLabelHeight, DEBUG);
                    float labelPadding = 0.0f;
                    float gapY = 0.0f;
                    if (key.hintLabel != null || (key.hintLabelLeft != null && key.hintLabelRight != null)) {
                        gapY = key.contentHeight * KEY10_LABEL_MIN_OFFSET_Y_RATIO;
                        labelPadding = key.contentHeight * KEY10_HINT_OFFSET_Y_RATIO;
                        float labelBottomHeight = paint.descent() + labelPadding + hintPaint.getFontMetrics(null);
                        if (labelBottomHeight > keyTopCenterY + gapY) {
                            gapY = labelBottomHeight - keyTopCenterY;
                        }
                    }
                    drawTextWithStroke(label, 0.0f, -gapY, paint, color, canvas, key.contentWidth, DEBUG);
                    canvas.translate(0.0f, (((-gapY) + paint.descent()) + labelPadding) - hintPaint.ascent());
                    if (key.hintLabel != null) {
                        hintPaint.setTextAlign(Paint.Align.CENTER);
                        drawTextWithStroke(key.hintLabel.toString(), 0.0f, 0.0f, hintPaint, this.mKeyHintLetterColor, canvas);
                        return;
                    }
                    if (key.hintLabelLeft != null && key.hintLabelRight != null) {
                        canvas.translate(-keyTopCenterX, 0.0f);
                        float labelOffset = (key.contentWidth - (hintPaint.getTextSize() * KEY10_HINT_AREA_WIDTH_RATIO)) / 2.0f;
                        if (labelOffset < 0.0f) {
                            labelOffset = 0.0f;
                        }
                        hintPaint.setTextAlign(Paint.Align.LEFT);
                        drawTextWithStroke(key.hintLabelLeft.toString(), labelOffset, 0.0f, hintPaint, this.mKeyHintLetterColor, canvas);
                        hintPaint.setTextAlign(Paint.Align.RIGHT);
                        drawTextWithStroke(key.hintLabelRight.toString(), key.contentWidth - labelOffset, 0.0f, hintPaint, this.mKeyHintLetterColor, canvas);
                    }
                }
            }
        }
    }

    private void drawQwerty(Canvas canvas, Keyboard.Key key, String label, boolean isKeyHintNormal, float keyHintLetterPaddingRight, float keyHintLetterPaddingTop, float keyPaddingRatioX, float keyPaddingRatioY) {
        if (canvas != null && key != null && label != null && this.mPaint != null && this.mHintPaint != null) {
            Paint paint = this.mPaint;
            Paint hintPaint = this.mHintPaint;
            int[] color = getColor(key);
            float keyTopCenterX = key.contentWidth / 2.0f;
            float keyTopCenterY = key.contentHeight / 2.0f;
            canvas.translate(keyTopCenterX, keyTopCenterY);
            if (!draw2line(canvas, key, label, paint, color)) {
                float criteriaSize = paint.measureText(CRITERIA_KEY_WIDTH);
                if (WnnUtility.isFunctionKey(key)) {
                    criteriaSize = paint.measureText(label);
                }
                resizeKeyTopWidth(paint, hintPaint, key, criteriaSize);
                float mainLabelHeight = (-paint.ascent()) + paint.descent();
                float hintLabelHeight = (-hintPaint.ascent()) + hintPaint.descent();
                float totalLabelHeight = mainLabelHeight + hintLabelHeight;
                resizeKeyTopHeight(paint, hintPaint, key, totalLabelHeight, DEBUG);
                drawTextWithStroke(label, 0.0f, 0.0f, paint, color, canvas, key.contentWidth, DEBUG);
                if (key.hintLabel != null && isKeyHintNormal) {
                    hintPaint.setTextAlign(Paint.Align.CENTER);
                    float posX = ((key.contentWidth - keyTopCenterX) - (keyHintLetterPaddingRight * keyPaddingRatioX)) - (getCharWidth(KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR, hintPaint) / 2.0f);
                    float posY = ((keyHintLetterPaddingTop * keyPaddingRatioY) - keyTopCenterY) - hintPaint.ascent();
                    drawTextWithStroke(key.hintLabel.toString(), posX, posY, hintPaint, this.mKeyHintLetterColor, canvas);
                }
            }
        }
    }

    private int getKeyIndices(int x, int y, int[] allKeys) {
        Keyboard.Key[] keys = this.mKeys;
        int primaryIndex = -1;
        int closestKey = -1;
        int closestKeyDist = this.mProximityThreshold + 1;
        Arrays.fill(this.mDistances, Integer.MAX_VALUE);
        int[] nearestKeyIndices = this.mKeyboard.getNearestKeys(x, y);
        int keyCount = nearestKeyIndices.length;
        for (int i = 0; i < keyCount; i++) {
            Keyboard.Key key = keys[nearestKeyIndices[i]];
            int dist = 0;
            boolean isInside = key.isInside(x, y);
            if (isInside) {
                primaryIndex = nearestKeyIndices[i];
            }
            if (((this.mProximityCorrectOn && (dist = key.squaredDistanceFrom(x, y)) < this.mProximityThreshold) || isInside) && key.codes[0] > 32) {
                int nCodes = key.codes.length;
                if (dist < closestKeyDist) {
                    closestKeyDist = dist;
                    closestKey = nearestKeyIndices[i];
                }
                if (allKeys != null) {
                    int j = 0;
                    while (true) {
                        if (j >= this.mDistances.length) {
                            break;
                        }
                        if (this.mDistances[j] <= dist) {
                            j++;
                        } else {
                            System.arraycopy(this.mDistances, j, this.mDistances, j + nCodes, (this.mDistances.length - j) - nCodes);
                            System.arraycopy(allKeys, j, allKeys, j + nCodes, (allKeys.length - j) - nCodes);
                            for (int c = 0; c < nCodes; c++) {
                                allKeys[j + c] = key.codes[c];
                                this.mDistances[j + c] = dist;
                            }
                        }
                    }
                }
            }
        }
        if (primaryIndex == -1) {
            int primaryIndex2 = closestKey;
            return primaryIndex2;
        }
        return primaryIndex;
    }

    private void detectAndSendKey(int index, int x, int y, long eventTime) {
        if (index != -1 && index < this.mKeys.length) {
            Keyboard.Key key = this.mKeys[index];
            if (key.text != null) {
                this.mKeyboardActionListener.onText(key.text);
                this.mKeyboardActionListener.onRelease(-1);
            } else {
                int code = key.codes[0];
                int[] codes = new int[MAX_NEARBY_KEYS];
                Arrays.fill(codes, -1);
                getKeyIndices(x, y, codes);
                if (this.mInMultiTap) {
                    if (this.mTapCount == -1) {
                        this.mTapCount = 0;
                    }
                    code = key.codes[this.mTapCount];
                }
                this.mKeyboardActionListener.onKey(code, codes);
                this.mKeyboardActionListener.onRelease(code);
            }
            this.mLastSentIndex = index;
            this.mLastTapTime = eventTime;
        }
    }

    private CharSequence getPreviewText(Keyboard.Key key) {
        if (this.mInMultiTap) {
            this.mPreviewLabel.setLength(0);
            this.mPreviewLabel.append((char) key.codes[this.mTapCount >= 0 ? this.mTapCount : 0]);
            return adjustCase(this.mPreviewLabel);
        }
        if (key.label == null) {
            return key.keyPreviewLabel;
        }
        return key.label;
    }

    private void showPreview(int keyIndex) {
        int oldKeyIndex = this.mCurrentKeyIndex;
        PopupWindow previewPopup = this.mPreviewPopup;
        this.mCurrentKeyIndex = keyIndex;
        Keyboard.Key[] keys = this.mKeys;
        if (oldKeyIndex != this.mCurrentKeyIndex) {
            if (oldKeyIndex != -1 && keys.length > oldKeyIndex) {
                keys[oldKeyIndex].onReleased(this.mCurrentKeyIndex == -1);
                invalidateKey(oldKeyIndex);
            }
            if (this.mCurrentKeyIndex != -1 && keys.length > this.mCurrentKeyIndex) {
                keys[this.mCurrentKeyIndex].onPressed();
                invalidateKey(this.mCurrentKeyIndex);
            }
        }
        if (oldKeyIndex != this.mCurrentKeyIndex && this.mShowPreview && isParentPreviewEnabled()) {
            this.mHandler.removeMessages(1);
            if (previewPopup.isShowing() && keyIndex == -1) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 70L);
            }
            if (keyIndex != -1) {
                if (previewPopup.isShowing() && this.mPreviewText.getVisibility() == 0) {
                    showKey(keyIndex);
                } else {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, keyIndex, 0), 0L);
                }
            }
        }
    }

    private void showKey(int keyIndex) {
        Keyboard.Key key;
        IWnnIME wnn;
        Context context;
        Resources res;
        Drawable previewDrawable;
        boolean isSkin;
        PopupWindow previewPopup = this.mPreviewPopup;
        previewPopup.dismiss();
        Keyboard.Key[] keys = this.mKeys;
        if (keyIndex >= 0 && keyIndex < this.mKeys.length && (key = keys[keyIndex]) != null && key.codes[0] != -99999 && (wnn = IWnnIME.getCurrentIme()) != null) {
            if ((!IWnnIME.isTabletMode() || ((key.codes[0] != -121 && key.codes[0] != 32) || this.mKeyboard.getKeyboardType() != 0 || wnn.getCurrentDefaultSoftKeyboard().isPhoneMode().booleanValue())) && !WnnUtility.isFunctionKey(key) && (context = getContext()) != null && (res = context.getResources()) != null && this.mPreviewText != null) {
                this.mPreviewText.setBackgroundDrawable(res.getDrawable(R.drawable.keyboard_key_feedback));
                setKeyPreviewBackground(this.mPreviewText, key);
                int color = getKeyPreviewColor(key);
                this.mPreviewText.setTextColor(color);
                CharSequence keyText = null;
                int popupHeight = (int) (key.height * RATE_HEIGHT_KEY_PREVIEW);
                int popupWidth = key.width;
                int bottom = this.mPreviewText.getPaddingBottom();
                int paddingLR = this.mPreviewText.getPaddingLeft() + this.mPreviewText.getPaddingRight();
                if (key.iconPreview != null || (key.icon != null && key.keyPreviewLabel == null)) {
                    if (key.iconPreview != null) {
                        previewDrawable = key.iconPreview;
                        isSkin = key.mIsPreviewSkin;
                    } else {
                        previewDrawable = key.icon;
                        isSkin = key.mIsIconSkin;
                    }
                    previewDrawable.clearColorFilter();
                    if (!isSkin) {
                        previewDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                    this.mPreviewText.setCompoundDrawables(null, null, null, previewDrawable);
                    this.mPreviewText.setText((CharSequence) null);
                    int height = previewDrawable.getMinimumHeight();
                    popupHeight = Math.max(popupHeight, height);
                    bottom = (popupHeight - previewDrawable.getMinimumHeight()) / 2;
                    int width = previewDrawable.getMinimumWidth() + paddingLR;
                    popupWidth = Math.max(popupWidth, width);
                } else {
                    this.mPreviewText.setCompoundDrawables(null, null, null, null);
                    keyText = getPreviewText(key);
                    this.mPreviewText.setText(keyText);
                    this.mPreviewText.setTextSize(0, this.mPreviewTextSizeLarge * this.mTextScaleRate);
                    this.mPreviewText.setTypeface(Typeface.DEFAULT);
                }
                this.mPreviewText.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
                if (keyText != null) {
                    int height2 = this.mPreviewText.getMeasuredHeight();
                    popupHeight = Math.max(popupHeight, height2);
                    Paint paint = new Paint();
                    paint.setTextSize(this.mPreviewText.getTextSize());
                    int width2 = ((int) (paint.measureText(keyText.toString()) + DEFAULT_KEY_SIZE_RATE)) + paddingLR;
                    popupWidth = Math.max(popupWidth, width2);
                }
                ViewGroup.LayoutParams lp = this.mPreviewText.getLayoutParams();
                if (lp != null) {
                    lp.width = popupWidth;
                    lp.height = popupHeight;
                } else {
                    this.mPreviewText.setLayoutParams(new ViewGroup.LayoutParams(popupWidth, popupHeight));
                }
                if (!this.mPreviewCentered) {
                    this.mPopupPreviewX = (key.x + (key.width / 2)) - (popupWidth / 2);
                    this.mPopupPreviewY = (key.y - popupHeight) + this.mPreviewOffset;
                } else {
                    this.mPopupPreviewX = 160 - (this.mPreviewText.getMeasuredWidth() / 2);
                    this.mPopupPreviewY = -this.mPreviewText.getMeasuredHeight();
                }
                this.mPopupPreviewY += 20;
                this.mHandler.removeMessages(2);
                if (this.mOffsetInWindow == null) {
                    this.mOffsetInWindow = new int[2];
                }
                KeyboardManager km = wnn.getCurrentKeyboardManager();
                Point pos = km.getKeyboardPosition();
                this.mOffsetInWindow[0] = pos.x + this.mMiniKeyboardOffsetX;
                this.mOffsetInWindow[1] = pos.y + this.mMiniKeyboardOffsetY;
                this.mPopupPreviewX += this.mOffsetInWindow[0] + getPaddingLeft();
                this.mPopupPreviewY += this.mOffsetInWindow[1] + getPaddingTop();
                int tempPreviewPosBottom = this.mPopupPreviewY + popupHeight;
                int keyPosBottom = key.y + key.height + this.mOffsetInWindow[1];
                int previewKeyGap = keyPosBottom - tempPreviewPosBottom;
                int popupHeight2 = popupHeight + previewKeyGap;
                this.mPreviewText.setPadding(this.mPreviewText.getPaddingLeft(), this.mPreviewText.getPaddingTop(), this.mPreviewText.getPaddingRight(), bottom + previewKeyGap);
                if (key.popupResId == 0) {
                    this.mPreviewText.getBackground().setState(EMPTY_STATE_SET);
                } else {
                    this.mPreviewText.getBackground().setState(LONG_PRESSABLE_STATE_SET);
                }
                if (previewPopup.isShowing()) {
                    previewPopup.update(this.mPopupPreviewX, this.mPopupPreviewY, popupWidth, popupHeight2, true);
                } else {
                    previewPopup.setWidth(popupWidth);
                    previewPopup.setHeight(popupHeight2);
                    previewPopup.showAtLocation(this.mPopupParent, 0, this.mPopupPreviewX, this.mPopupPreviewY);
                    WnnUtility.addFlagsForPopupWindow(wnn, previewPopup);
                }
                this.mPreviewText.setVisibility(0);
            }
        }
    }

    public void invalidateAllKeys() {
        this.mDirtyRect.union(0, 0, getWidth(), getHeight());
        this.mDrawPending = true;
        invalidate();
    }

    public void invalidateKey(int keyIndex) {
        if (this.mKeys != null && keyIndex >= 0 && keyIndex < this.mKeys.length) {
            int kbdPaddingLeft = getPaddingLeft();
            Keyboard.Key key = this.mKeys[keyIndex];
            this.mInvalidatedKey = key;
            this.mDirtyRect.union(key.x + kbdPaddingLeft, key.y + getPaddingTop(), key.x + key.width + kbdPaddingLeft, key.y + key.height + getPaddingTop());
            onBufferDraw();
            invalidate(key.x + kbdPaddingLeft, key.y + getPaddingTop(), key.x + key.width + kbdPaddingLeft, key.y + key.height + getPaddingTop());
        }
    }

    private boolean openPopupIfRequired(MotionEvent me) {
        boolean result = DEBUG;
        if (this.mPopupLayout != 0 && this.mCurrentKey >= 0 && this.mCurrentKey < this.mKeys.length) {
            Keyboard.Key popupKey = this.mKeys[this.mCurrentKey];
            result = onLongPress(popupKey, me);
            if (result) {
                this.mAbortKey = true;
                showPreview(-1);
            }
        }
        return result;
    }

    protected boolean onLongPress(Keyboard.Key popupKey, MotionEvent me) {
        Keyboard keyboard;
        int popupKeyboardId = popupKey.popupResId;
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null && popupKeyboardId != 0) {
            this.mMiniKeyboardContainer = this.mMiniKeyboardCache.get(popupKey);
            if (this.mMiniKeyboardContainer == null) {
                Context context = getContext();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
                if (inflater == null) {
                    return DEBUG;
                }
                this.mMiniKeyboardContainer = inflater.inflate(this.mPopupLayout, (ViewGroup) null);
                this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(R.id.popup_keyboard_view);
                this.mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
                    @Override
                    public boolean onKey(int primaryCode, int[] keyCodes) {
                        KeyboardView.this.mKeyboardActionListener.onKey(primaryCode, keyCodes);
                        KeyboardView.this.dismissPopupKeyboard();
                        return true;
                    }

                    @Override
                    public void onText(CharSequence text) {
                        KeyboardView.this.mKeyboardActionListener.onText(text);
                        KeyboardView.this.dismissPopupKeyboard();
                    }

                    @Override
                    public void swipeLeft() {
                    }

                    @Override
                    public void swipeRight() {
                    }

                    @Override
                    public void swipeUp() {
                    }

                    @Override
                    public void swipeDown() {
                    }

                    @Override
                    public void onPress(int primaryCode) {
                        KeyboardView.this.mKeyboardActionListener.onPress(primaryCode);
                    }

                    @Override
                    public void onRelease(int primaryCode) {
                        KeyboardView.this.mKeyboardActionListener.onRelease(primaryCode);
                    }
                });
                int condition = this.mKeyboard.getKeyboardCondition();
                if (popupKey.popupCharacters != null) {
                    keyboard = new Keyboard(getContext(), popupKeyboardId, popupKey.popupCharacters, condition);
                } else {
                    keyboard = new Keyboard(getContext(), popupKeyboardId, condition);
                }
                this.mMiniKeyboard.setKeyboard(keyboard);
                this.mMiniKeyboard.setPopupParent(this);
                DisplayMetrics dm = getResources().getDisplayMetrics();
                this.mMiniKeyboardContainer.measure(View.MeasureSpec.makeMeasureSpec(getWidth(), ExploreByTouchHelper.INVALID_ID), View.MeasureSpec.makeMeasureSpec(dm.heightPixels, ExploreByTouchHelper.INVALID_ID));
                this.mMiniKeyboardCache.put(popupKey, this.mMiniKeyboardContainer);
            } else {
                this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(R.id.popup_keyboard_view);
            }
            int x = popupKey.x;
            int y = popupKey.y;
            KeyboardManager km = wnn.getCurrentKeyboardManager();
            Point pos = km.getKeyboardPosition();
            int[] windowOffset = {pos.x, pos.y};
            int y2 = y - this.mMiniKeyboardContainer.getMeasuredHeight();
            int x2 = (x - this.mMiniKeyboardContainer.getPaddingRight()) + windowOffset[0];
            int y3 = (y2 - this.mMiniKeyboardContainer.getPaddingBottom()) + windowOffset[1];
            this.mMiniKeyboard.setPopupOffset(x2 < 0 ? 0 : x2, y3);
            this.mMiniKeyboard.setShifted(isShifted());
            this.mPopupKeyboard.setContentView(this.mMiniKeyboardContainer);
            this.mPopupKeyboard.setWidth(this.mMiniKeyboardContainer.getMeasuredWidth());
            this.mPopupKeyboard.setHeight(this.mMiniKeyboardContainer.getMeasuredHeight());
            this.mPopupKeyboard.showAtLocation(this.mPopupParent, 0, x2, y3);
            WnnUtility.addFlagsForPopupWindow(wnn, this.mPopupKeyboard);
            this.mMiniKeyboardOnScreen = true;
            invalidateAllKeys();
            return true;
        }
        return DEBUG;
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (WnnAccessibility.isAccessibility(IWnnIME.getCurrentIme())) {
            return true;
        }
        return handleTouchEvent(me);
    }

    public boolean handleTouchEvent(MotionEvent me) {
        if (IWnnIME.getCurrentIme() == null || !isShown()) {
            return true;
        }
        int pointerCount = me.getPointerCount();
        int action = me.getActionMasked();
        boolean result = DEBUG;
        long now = me.getEventTime();
        boolean isPointerCountOne = pointerCount == 1 ? true : DEBUG;
        boolean isMultiTouchKeyboardView = this instanceof MultiTouchKeyboardView;
        if (pointerCount != this.mOldPointerCount && isMultiTouchKeyboardView) {
            if (isPointerCountOne) {
                MotionEvent down = MotionEvent.obtain(now, now, 0, me.getX(), me.getY(), me.getMetaState());
                result = onModifiedTouchEvent(down, DEBUG);
                down.recycle();
                if (action == 1 || action == 6) {
                    result = onModifiedTouchEvent(me, true);
                }
            }
        } else if (isPointerCountOne || !isMultiTouchKeyboardView) {
            result = onModifiedTouchEvent(me, DEBUG);
        } else {
            result = true;
        }
        this.mOldPointerCount = pointerCount;
        return result;
    }

    private boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
        int touchX = ((int) me.getX()) - getPaddingLeft();
        int touchY = (((int) me.getY()) + this.mVerticalCorrection) - getPaddingTop();
        int positionY1stRow = this.mKeyboard.getKeys().get(0).y;
        if (touchY < positionY1stRow) {
            touchY = positionY1stRow;
        }
        int action = me.getActionMasked();
        long eventTime = me.getEventTime();
        int keyIndex = getKeyIndices(touchX, touchY, null);
        this.mPossiblePoly = possiblePoly;
        if (action == 0 || action == 5) {
            this.mSwipeTracker.clear();
        }
        this.mSwipeTracker.addMovement(me);
        if (this.mAbortKey && action != 0 && action != 5 && action != 3) {
            return true;
        }
        if (this.mGestureDetector.onTouchEvent(me)) {
            showPreview(-1);
            this.mHandler.removeMessages(3);
            this.mHandler.removeMessages(4);
            return true;
        }
        if (this.mMiniKeyboardOnScreen && action != 3) {
            return true;
        }
        switch (action) {
            case 0:
            case 5:
                this.mAbortKey = DEBUG;
                this.mStartX = touchX;
                this.mStartY = touchY;
                this.mLastCodeX = touchX;
                this.mLastCodeY = touchY;
                this.mLastKeyTime = 0L;
                this.mCurrentKeyTime = 0L;
                this.mLastKey = -1;
                this.mCurrentKey = keyIndex;
                this.mDownKey = keyIndex;
                this.mDownTime = me.getEventTime();
                this.mLastMoveTime = this.mDownTime;
                checkMultiTap(eventTime, keyIndex);
                if (this.mCurrentKey >= 0 && this.mKeys[this.mCurrentKey].repeatable) {
                    this.mRepeatKeyIndex = this.mCurrentKey;
                    Message msg = this.mHandler.obtainMessage(3);
                    this.mHandler.sendMessageDelayed(msg, 400L);
                    repeatKey();
                    if (this.mAbortKey) {
                        this.mRepeatKeyIndex = -1;
                        this.mHandler.removeMessages(3);
                    }
                } else {
                    this.mRepeatKeyIndex = -1;
                    this.mHandler.removeMessages(3);
                }
                this.mHandler.removeMessages(4);
                if (this.mCurrentKey != -1) {
                    Message msg2 = this.mHandler.obtainMessage(4, me);
                    this.mHandler.sendMessageDelayed(msg2, LONGPRESS_TIMEOUT);
                }
                showPreview(keyIndex);
                break;
            case 1:
            case 6:
                removeMessages();
                if (keyIndex == this.mCurrentKey) {
                    this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                } else {
                    resetMultiTap();
                    this.mLastKey = this.mCurrentKey;
                    this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                    this.mCurrentKey = keyIndex;
                    this.mCurrentKeyTime = 0L;
                }
                if (this.mCurrentKeyTime < this.mLastKeyTime && this.mCurrentKeyTime < 70 && this.mLastKey != -1) {
                    this.mCurrentKey = this.mLastKey;
                    touchX = this.mLastCodeX;
                    touchY = this.mLastCodeY;
                }
                showPreview(-1);
                Arrays.fill(this.mKeyIndices, -1);
                if (this.mRepeatKeyIndex == -1 && !this.mMiniKeyboardOnScreen && !this.mAbortKey) {
                    detectAndSendKey(this.mCurrentKey, touchX, touchY, eventTime);
                }
                invalidateKey(keyIndex);
                this.mRepeatKeyIndex = -1;
                this.mHandler.removeMessages(3);
                break;
            case 2:
                boolean continueLongPress = DEBUG;
                if (keyIndex != -1) {
                    if (this.mCurrentKey == -1) {
                        this.mCurrentKey = keyIndex;
                        this.mCurrentKeyTime = eventTime - this.mDownTime;
                    } else if (keyIndex == this.mCurrentKey) {
                        this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                        continueLongPress = true;
                    } else if (this.mRepeatKeyIndex == -1) {
                        resetMultiTap();
                        this.mLastKey = this.mCurrentKey;
                        this.mLastCodeX = this.mLastX;
                        this.mLastCodeY = this.mLastY;
                        this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                        this.mCurrentKey = keyIndex;
                        this.mCurrentKeyTime = 0L;
                    }
                }
                if (!continueLongPress) {
                    this.mHandler.removeMessages(4);
                    if (this.mCurrentKey != -1) {
                        Message msg3 = this.mHandler.obtainMessage(4, me);
                        this.mHandler.sendMessageDelayed(msg3, LONGPRESS_TIMEOUT);
                    }
                }
                showPreview(this.mCurrentKey);
                this.mLastMoveTime = eventTime;
                break;
            case 3:
                removeMessages();
                dismissPopupKeyboard();
                this.mAbortKey = true;
                showPreview(-1);
                invalidateKey(this.mCurrentKey);
                break;
        }
        this.mLastX = touchX;
        this.mLastY = touchY;
        return true;
    }

    private boolean repeatKey() {
        if (this.mRepeatKeyIndex == -1) {
            return DEBUG;
        }
        Keyboard.Key key = this.mKeys[this.mRepeatKeyIndex];
        detectAndSendKey(this.mCurrentKey, key.x, key.y, this.mLastTapTime);
        return true;
    }

    protected void swipeRight() {
        this.mKeyboardActionListener.swipeRight();
    }

    protected void swipeLeft() {
        this.mKeyboardActionListener.swipeLeft();
    }

    protected void swipeUp() {
        this.mKeyboardActionListener.swipeUp();
    }

    protected void swipeDown() {
        this.mKeyboardActionListener.swipeDown();
    }

    public void closing() {
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
        removeMessages();
        dismissPopupKeyboard();
        this.mBuffer = null;
        this.mCanvas = null;
        this.mMiniKeyboardCache.clear();
    }

    private void removeMessages() {
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(1);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    @Override
    public int getPaddingLeft() {
        if (this.mKeyboard != null) {
            int paddingLeft = this.mKeyboard.getPaddingLeft();
            return paddingLeft;
        }
        int paddingLeft2 = super.getPaddingLeft();
        return paddingLeft2;
    }

    public void dismissPopupKeyboard() {
        if (this.mPopupKeyboard.isShowing()) {
            this.mPopupKeyboard.dismiss();
            this.mMiniKeyboardOnScreen = DEBUG;
            invalidateAllKeys();
        }
    }

    public boolean isMiniKeyboardOnScreen() {
        return this.mMiniKeyboardOnScreen;
    }

    public boolean handleBack() {
        if (!this.mPopupKeyboard.isShowing()) {
            return DEBUG;
        }
        dismissPopupKeyboard();
        return true;
    }

    private void resetMultiTap() {
        this.mLastSentIndex = -1;
        this.mTapCount = 0;
        this.mLastTapTime = -1L;
        this.mInMultiTap = DEBUG;
    }

    private void checkMultiTap(long eventTime, int keyIndex) {
        if (keyIndex != -1) {
            Keyboard.Key key = this.mKeys[keyIndex];
            if (key.codes.length <= 1) {
                if (eventTime > this.mLastTapTime + 800 || keyIndex != this.mLastSentIndex) {
                    resetMultiTap();
                    return;
                }
                return;
            }
            this.mInMultiTap = true;
            if (eventTime < this.mLastTapTime + 800 && keyIndex == this.mLastSentIndex) {
                this.mTapCount = (this.mTapCount + 1) % key.codes.length;
            } else {
                this.mTapCount = -1;
            }
        }
    }

    private static class SwipeTracker {
        static final int LONGEST_PAST_TIME = 200;
        static final int NUM_PAST = 4;
        final long[] mPastTime;
        final float[] mPastX;
        final float[] mPastY;
        float mXVelocity;
        float mYVelocity;

        private SwipeTracker() {
            this.mPastX = new float[4];
            this.mPastY = new float[4];
            this.mPastTime = new long[4];
        }

        public void clear() {
            this.mPastTime[0] = 0;
        }

        public void addMovement(MotionEvent ev) {
            long time = ev.getEventTime();
            int N = ev.getHistorySize();
            for (int i = 0; i < N; i++) {
                addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i));
            }
            addPoint(ev.getX(), ev.getY(), time);
        }

        private void addPoint(float x, float y, long time) {
            int drop = -1;
            long[] pastTime = this.mPastTime;
            int i = 0;
            while (i < 4 && pastTime[i] != 0) {
                if (pastTime[i] < time - 200) {
                    drop = i;
                }
                i++;
            }
            if (i == 4 && drop < 0) {
                drop = 0;
            }
            if (drop == i) {
                drop--;
            }
            float[] pastX = this.mPastX;
            float[] pastY = this.mPastY;
            if (drop >= 0) {
                int start = drop + 1;
                int count = (4 - drop) - 1;
                System.arraycopy(pastX, start, pastX, 0, count);
                System.arraycopy(pastY, start, pastY, 0, count);
                System.arraycopy(pastTime, start, pastTime, 0, count);
                i -= drop + 1;
            }
            pastX[i] = x;
            pastY[i] = y;
            pastTime[i] = time;
            int i2 = i + 1;
            if (i2 < 4) {
                pastTime[i2] = 0;
            }
        }

        public void computeCurrentVelocity(int units) {
            computeCurrentVelocity(units, Float.MAX_VALUE);
        }

        public void computeCurrentVelocity(int units, float maxVelocity) {
            float[] pastX = this.mPastX;
            float[] pastY = this.mPastY;
            long[] pastTime = this.mPastTime;
            float oldestX = pastX[0];
            float oldestY = pastY[0];
            long oldestTime = pastTime[0];
            float accumX = 0.0f;
            float accumY = 0.0f;
            int N = 0;
            while (N < 4 && pastTime[N] != 0) {
                N++;
            }
            for (int i = 1; i < N; i++) {
                int dur = (int) (pastTime[i] - oldestTime);
                if (dur != 0) {
                    float dist = pastX[i] - oldestX;
                    float vel = (dist / dur) * units;
                    accumX = accumX == 0.0f ? vel : (accumX + vel) * 0.5f;
                    float dist2 = pastY[i] - oldestY;
                    float vel2 = (dist2 / dur) * units;
                    accumY = accumY == 0.0f ? vel2 : (accumY + vel2) * 0.5f;
                }
            }
            this.mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity) : Math.min(accumX, maxVelocity);
            this.mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity) : Math.min(accumY, maxVelocity);
        }

        public float getXVelocity() {
            return this.mXVelocity;
        }

        public float getYVelocity() {
            return this.mYVelocity;
        }
    }

    public void clearWindowInfo() {
        this.mOffsetInWindow = null;
    }

    private static int getCharGeometryCacheKey(char reference, Paint paint) {
        int labelSize = (int) paint.getTextSize();
        Typeface face = paint.getTypeface();
        int codePointOffset = reference << 15;
        if (face == Typeface.DEFAULT) {
            return codePointOffset + labelSize;
        }
        if (face == Typeface.DEFAULT_BOLD) {
            return codePointOffset + labelSize + 4096;
        }
        if (face == Typeface.MONOSPACE) {
            return codePointOffset + labelSize + 8192;
        }
        return codePointOffset + labelSize;
    }

    private static float getCharWidth(char[] character, Paint paint) {
        Integer key = Integer.valueOf(getCharGeometryCacheKey(character[0], paint));
        Float cachedValue = sTextWidthCache.get(key);
        if (cachedValue != null) {
            return cachedValue.floatValue();
        }
        paint.getTextBounds(character, 0, 1, sTextBounds);
        float width = sTextBounds.width();
        sTextWidthCache.put(key, Float.valueOf(width));
        return width;
    }

    public void setEnableMushroom(boolean enableMushroom) {
        this.mEnableMushroom = enableMushroom;
    }

    public void setIsInputTypeNull(boolean isInputTypeNull) {
        this.mIsInputTypeNull = isInputTypeNull;
    }

    public void dismissKeyPreview() {
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewText.setVisibility(4);
        }
    }

    protected void setKeyPreviewBackground(TextView textView, Keyboard.Key key) {
        int resourceId;
        Context context = getContext();
        KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
        if (key.popupResId != 0) {
            resourceId = R.drawable.keyboard_key_feedback_more_background;
        } else {
            resourceId = R.drawable.keyboard_key_feedback_background;
        }
        Drawable backgournd = resMan.getKeyPreviewDrawable(context, resourceId);
        if (backgournd != null) {
            textView.setBackgroundDrawable(backgournd);
        }
    }

    protected void createSlidePopup(IWnnIME parent) {
        if (parent != null) {
            LayoutInflater inflate = parent.getLayoutInflater();
            this.mSlidePopupLayout = (LinearLayout) inflate.inflate(R.layout.keyboard_key_preview_slide, (ViewGroup) null);
            this.mSlidePopupLayout.setBackgroundColor(this.mSlidePopupBackgroundColorNormal);
            Resources res = parent.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            int keyWidth = Math.round(res.getFraction(R.fraction.keyboard_qwerty_mode_change_keywidth, dm.widthPixels, dm.widthPixels));
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(keyWidth, -1);
            Paint paint = new Paint();
            paint.setTextSize(this.mLabelTextSize);
            String str = res.getString(SLIDE_POPUP_TABLE[0][0]);
            int textWidth = (int) paint.measureText(str);
            if (textWidth > keyWidth) {
                textWidth = keyWidth;
            }
            int textSize = textWidth / 2;
            for (int i = 0; i < SLIDE_POPUP_TABLE.length; i++) {
                TextView textView = new TextView(parent);
                textView.setLayoutParams(params);
                textView.setGravity(17);
                textView.setTextColor(this.mSlidePopupColorNormal);
                textView.setTextSize(0, textSize);
                textView.setText(SLIDE_POPUP_TABLE[i][0]);
                textView.setId(SLIDE_POPUP_TABLE[i][1]);
                this.mSlidePopupLayout.addView(textView);
            }
            LinearLayout linearLayout = new LinearLayout(parent);
            linearLayout.setLayoutParams(params);
            linearLayout.setId(8);
            linearLayout.setGravity(17);
            Drawable image = res.getDrawable(R.drawable.key_12key_voice2);
            ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(-2, -2);
            ImageView voice = new ImageView(parent);
            voice.setLayoutParams(params2);
            voice.setImageDrawable(image);
            voice.setColorFilter(this.mSlidePopupColorNormal, PorterDuff.Mode.SRC_IN);
            linearLayout.addView(voice);
            this.mSlidePopupLayout.addView(linearLayout);
        }
    }

    protected void setSlidePopupFocused(IWnnIME parent, int index) {
        if (parent != null && this.mSlidePopupLayout != null && index >= 0 && index <= this.mSlidePopupDisplayList.size()) {
            View view = this.mSlidePopupDisplayList.get(index);
            view.setBackgroundColor(this.mSlidePopupBackgroundColorFocused);
            setSlidePopupTopColor(view, this.mSlidePopupColorFocused);
        }
    }

    protected void clearSlidePopupFocused() {
        if (this.mSlidePopupLayout != null) {
            for (int i = 0; i < this.mSlidePopupLayout.getChildCount(); i++) {
                View view = this.mSlidePopupLayout.getChildAt(i);
                view.setBackground(null);
                setSlidePopupTopColor(view, this.mSlidePopupColorNormal);
            }
        }
    }

    private void setSlidePopupTopColor(View view, int color) {
        if (view.getId() == 8) {
            ImageView voice = (ImageView) ((LinearLayout) view).getChildAt(0);
            voice.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } else {
            ((TextView) view).setTextColor(color);
        }
    }

    protected void setSlidePopupDisplayList(int[] enableKeyMode) {
        if (this.mSlidePopupLayout != null) {
            ArrayList<Integer> enableKeyModeList = new ArrayList<>();
            for (int i : enableKeyMode) {
                enableKeyModeList.add(Integer.valueOf(i));
            }
            this.mSlidePopupDisplayList = new ArrayList<>();
            for (int i2 = 0; i2 < this.mSlidePopupLayout.getChildCount(); i2++) {
                View view = this.mSlidePopupLayout.getChildAt(i2);
                int id = view.getId();
                if (id == -1 || enableKeyModeList.contains(Integer.valueOf(id))) {
                    view.setVisibility(0);
                    this.mSlidePopupDisplayList.add(view);
                } else {
                    view.setVisibility(8);
                }
            }
        }
    }

    protected int[] getColor(Keyboard.Key key) {
        if (key.codes[0] == -417) {
            return this.mKeyCloseTextColor;
        }
        return key.isSecondKey ? this.mKeyTextColor2nd : this.mKeyTextColor;
    }

    protected int getKeyPreviewColor(Keyboard.Key key) {
        KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
        Integer keyPreviewColor = resMan.getKeyPreviewColor();
        return keyPreviewColor != null ? keyPreviewColor.intValue() : getColor(key)[2];
    }

    private void updateKeyPopupResId(Keyboard.Key key) {
        IWnnIME wnn;
        if (key != null && key.codes != null && key.codes.length >= 1 && (wnn = IWnnIME.getCurrentIme()) != null) {
            int keyCode = key.codes[0];
            if (keyCode == -222 || keyCode == -106 || keyCode == -305) {
                boolean isScreenLock = wnn.isScreenLock();
                if (this.mEnableMushroom && !isScreenLock) {
                    key.popupResId = 1;
                } else {
                    key.popupResId = 0;
                }
            }
        }
    }

    private void drawTextWithStroke(String text, float x, float y, Paint paint, int[] color, Canvas canvas, int strokeWidth) {
        if (canvas != null && text != null && paint != null && color != null && color.length >= 2) {
            paint.setColor(color[1]);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(strokeWidth);
            canvas.drawText(text, x, y, paint);
            paint.setColor(color[2]);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(text, x, y, paint);
        }
    }

    private void drawTextWithStroke(String text, float x, float y, Paint paint, int[] color, Canvas canvas) {
        if (paint != null) {
            int strokeWidth = (int) (paint.getTextSize() * this.mKeyTextStrokeRatio);
            drawTextWithStroke(text, x, y, paint, color, canvas, strokeWidth);
        }
    }

    private void drawTextWithStroke(String text, float x, float y, Paint paint, int[] color, Canvas canvas, int drawLimitSize, boolean is2Line) {
        float textSize = paint.getTextSize();
        if (!is2Line) {
            y += (textSize - paint.descent()) / 2.0f;
        }
        drawTextWithStroke(text, x, y, paint, color, canvas);
    }

    private void drawKeyIcon(Canvas canvas, Keyboard.Key key, int keycode, float scaleRate, Resources res, int[] color) {
        if (canvas != null && key != null && res != null && color != null) {
            this.mDrawList.clear();
            if (!key.mIsIconSkin && keycode == -101) {
                this.mDrawList.add(this.mKeyBackgroundEnter);
                color[0] = this.mKeyBackgroundColorEnter;
            } else {
                this.mDrawList.add(null);
            }
            if (key.icon instanceof LayerDrawable) {
                LayerDrawable layerDraw = (LayerDrawable) key.icon;
                for (int layerIndex = 0; layerIndex < layerDraw.getNumberOfLayers(); layerIndex++) {
                    this.mDrawList.add(layerDraw.getDrawable(layerIndex));
                }
            } else {
                this.mDrawList.add(null);
                this.mDrawList.add(key.icon);
            }
            for (int drawableIndex = 0; drawableIndex < this.mDrawList.size(); drawableIndex++) {
                Drawable draw = this.mDrawList.get(drawableIndex);
                if (draw != null) {
                    if ((draw instanceof BitmapDrawable) && scaleRate != DEFAULT_KEY_SIZE_RATE) {
                        Bitmap tempBitmap = ((BitmapDrawable) draw).getBitmap();
                        float maxRateX = key.contentWidth / tempBitmap.getWidth();
                        if (maxRateX < scaleRate) {
                            scaleRate = maxRateX;
                        }
                        float maxRateY = key.contentHeight / tempBitmap.getHeight();
                        if (maxRateY < scaleRate) {
                            scaleRate = maxRateY;
                        }
                        Matrix matrix = new Matrix();
                        matrix.setScale(scaleRate, scaleRate);
                        try {
                            Bitmap scaleBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);
                            draw = new BitmapDrawable(res, scaleBitmap);
                        } catch (IllegalArgumentException e) {
                        }
                    }
                    int drawableX = (key.contentWidth - draw.getIntrinsicWidth()) / 2;
                    int drawableY = (key.contentHeight - draw.getIntrinsicHeight()) / 2;
                    canvas.translate(drawableX, drawableY);
                    draw.setBounds(0, 0, draw.getIntrinsicWidth(), draw.getIntrinsicHeight());
                    draw.clearColorFilter();
                    KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                    if (resMan.isEnableColorFilter(key.codes[0], key.mIsIconSkin, key.isSecondKey, drawableIndex)) {
                        draw.setColorFilter(color[drawableIndex], PorterDuff.Mode.SRC_IN);
                    }
                    draw.draw(canvas);
                    canvas.translate(-drawableX, -drawableY);
                }
            }
        }
    }

    private void drawTextInputModeKey(Canvas canvas, Keyboard.Key key, float textSize, float ellipsisLabelHeight) {
        Context context;
        IWnnIME wnn;
        DefaultSoftKeyboard softKeyboard;
        if (key != null && (context = getContext()) != null && (wnn = IWnnIME.getCurrentIme()) != null && (softKeyboard = wnn.getCurrentDefaultSoftKeyboard()) != null) {
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            int[] mainTextColor = resMan.getTextColor(context, R.color.input_mode_key_main_text_color);
            int[] modeTextColor = resMan.getTextColor(context, R.color.input_mode_key_mode_text_color);
            int[] selectedTextColor = resMan.getTextColor(context, R.color.input_mode_key_mode_selected_text_color);
            String mainLabel = softKeyboard.getInputModeKeyMainLabel();
            if (mainLabel != null) {
                Paint paint = new Paint();
                paint.setTextSize(textSize);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setAntiAlias(true);
                Paint paintHint = new Paint(paint);
                float textSizeHint = (2.0f * textSize) / 3.0f;
                paintHint.setTextSize(textSizeHint);
                paintHint.setStyle(Paint.Style.FILL_AND_STROKE);
                resizeKeyTopWidth(paint, paintHint, key, paint.measureText(mainLabel));
                float mainLabelHeight = (-paint.ascent()) + paint.descent();
                float hintLabelHeight = (-paintHint.ascent()) + paintHint.descent();
                float totalLabelHeight = mainLabelHeight + hintLabelHeight + (2.0f * ellipsisLabelHeight);
                resizeKeyTopHeight(paint, paintHint, key, totalLabelHeight, DEBUG);
                float mainLabelHeight2 = (-paint.ascent()) + paint.descent();
                float hintLabelHeight2 = (-paintHint.ascent()) + paintHint.descent();
                float posX = key.contentWidth / 2.0f;
                float posY = (((key.contentHeight - mainLabelHeight2) - hintLabelHeight2) / 2.0f) - paint.ascent();
                float textSize2 = paint.getTextSize();
                if (mainLabel.length() >= 3) {
                    paint.setTextScaleX(0.8f);
                }
                drawTextWithStroke(mainLabel, posX, posY, paint, mainTextColor, canvas);
                String[] modeLabel = new String[3];
                int[] offset = new int[3];
                boolean[] isCurrentMode = new boolean[3];
                int modeNum = softKeyboard.getInputModeKeyModeLabelList(modeLabel, offset, isCurrentMode, (int) textSizeHint);
                float layoutWidth = (2.0f * textSize2) / modeNum;
                float posYhint = (paintHint.descent() + posY) - paintHint.ascent();
                for (int index = 0; index < modeNum; index++) {
                    String hintLabel = modeLabel[index];
                    float posXhint = (posX - textSize2) + (index * layoutWidth) + (layoutWidth / 2.0f) + offset[index];
                    if (isCurrentMode[index]) {
                        drawTextWithStroke(hintLabel, posXhint, posYhint, paintHint, selectedTextColor, canvas, 2);
                    } else {
                        drawTextWithStroke(hintLabel, posXhint, posYhint, paintHint, modeTextColor, canvas, 1);
                    }
                }
            }
        }
    }

    public Keyboard.Key getKey(int positionX, int positionY) {
        Keyboard.Key positionKey = null;
        Keyboard keyboard = getKeyboard();
        int keyboardX = positionX - getPaddingLeft();
        int keyboardY = positionY - getPaddingTop();
        int[] keyIndices = keyboard.getNearestKeys(keyboardX, keyboardY);
        int keyCount = keyIndices.length;
        if (keyCount > 0) {
            List<Keyboard.Key> keys = keyboard.getKeys();
            for (int i : keyIndices) {
                Keyboard.Key key = keys.get(i);
                if (key.isInside(keyboardX, keyboardY)) {
                    positionKey = key;
                }
            }
        }
        return positionKey;
    }

    private void resizeKeyTopWidth(Paint paint, Paint hintPaint, Keyboard.Key key, float textWidth) {
        if (paint != null && key != null && textWidth > key.contentWidth) {
            paint.setTextSize((paint.getTextSize() * key.contentWidth) / textWidth);
            if (hintPaint != null) {
                hintPaint.setTextSize((hintPaint.getTextSize() * key.contentWidth) / textWidth);
            }
        }
    }

    private void resizeKeyTopHeight(Paint paint, Paint hintPaint, Keyboard.Key key, float textHeight, boolean is2line) {
        if (paint != null && key != null) {
            float keyHeight = key.contentHeight;
            if (is2line) {
                keyHeight /= 2.0f;
            }
            if (textHeight > keyHeight) {
                paint.setTextSize((paint.getTextSize() * keyHeight) / textHeight);
                if (hintPaint != null) {
                    hintPaint.setTextSize((hintPaint.getTextSize() * keyHeight) / textHeight);
                }
            }
        }
    }
}
