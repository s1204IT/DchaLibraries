package jp.co.omronsoft.iwnnime.ml;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;

public class KeyboardManager {
    private static final float DEFAULT_ALPHA_LEVEL = 1.0f;
    private static final String FLOATING_ALPHA = "floating_alpha";
    private static final String FLOATING_HEIGHT_LAND = "floating_height_land";
    private static final String FLOATING_HEIGHT_PORT = "floating_height_port";
    private static final String FLOATING_OFF_HEIGHT_LAND = "keyboard_height_landscape";
    private static final String FLOATING_OFF_HEIGHT_PORT = "keyboard_height_portrait";
    private static final String FLOATING_OFF_POSX_LAND = "keyboard_posx_landscape";
    private static final String FLOATING_OFF_POSX_PORT = "keyboard_posx_portrait";
    private static final String FLOATING_OFF_POSY_LAND = "keyboard_posy_landscape";
    private static final String FLOATING_OFF_POSY_PORT = "keyboard_posy_portrait";
    private static final String FLOATING_OFF_WIDTH_LAND = "keyboard_width_landscape";
    private static final String FLOATING_OFF_WIDTH_PORT = "keyboard_width_portrait";
    private static final String FLOATING_POSX_LAND = "floating_posx_land";
    private static final String FLOATING_POSX_PORT = "floating_posx_port";
    private static final String FLOATING_POSY_LAND = "floating_posy_land";
    private static final String FLOATING_POSY_PORT = "floating_posy_port";
    private static final String FLOATING_STATUS = "floating_status";
    private static final String FLOATING_WIDTH_LAND = "floating_width_land";
    private static final String FLOATING_WIDTH_PORT = "floating_width_port";
    private static final String KEYBOARD_MENU_STATE = "keyboard_menu_state";
    private static final String KEY_HEIGHT_LANDSCAPE_KEY = "key_height_landscape";
    private static final String KEY_HEIGHT_PORTRAIT_KEY = "key_height_portrait";
    private static final int MAX_PERCENT = 100;
    private static final int MSG_CHANGE_FLOATING = 2;
    private static final int MSG_RESHOW = 3;
    private static final int MSG_START_RESIZE = 1;
    private static final int NOT_EXIST_VIEWGROUP = -1;
    private static final int NOT_SET_POSITION = -99999;
    private static final int PARAMETER_NOT_MOVE = -1;
    private static final int PARAMETER_NOT_RESIZE = -1;
    private AbsoluteLayout mCandidatesAndMenuLayout;
    private CandidatesManager mCandidatesManager;
    private int mDisableColorFilter;
    private FrameLayout mKeyboardMenuLayout;
    private LinearLayout mMasterLayout;
    private IWnnIME mWnn;
    private View mDecorView = null;
    private View mCandidatesView = null;
    private View mKeyboardView = null;
    private TextView mKeyboardMenuOverlay = null;
    private DefaultSoftKeyboard mKeyboard = null;
    private int mKeyboardMenuDisableIconHeight = 0;
    private Point mOffsetPointer = new Point(0, 0);
    private Point mSizeOfStartProcess = new Point(0, 0);
    private Point mSizeOfResize = new Point(-1, -1);
    private Point mSizeOfMaxResize = new Point(0, 0);
    private Point mSizeOfMinResize = new Point(0, 0);
    private Point mPopupLocation = new Point(0, 0);
    private Point mPositionOfMove = new Point(-1, -1);
    private int mResizeStartTotalHeight = 0;
    private boolean mIsProcessingKeyboardMenu = false;
    private int mOrientation = 0;
    private int mMaxAlphaLevel = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean isSymbolMode = ((Boolean) msg.obj).booleanValue();
                    if (isSymbolMode) {
                        KeyboardManager.this.mCandidatesManager.resizeSymbolListPreparation();
                    } else {
                        KeyboardManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_FLOATING));
                    }
                    break;
                case 2:
                    KeyboardManager.this.setFloatingToPref(((Boolean) msg.obj).booleanValue());
                    KeyboardManager.this.mWnn.replaceKeyboard();
                    break;
                case 3:
                    KeyboardManager.this.mWnn.replaceKeyboard();
                    break;
            }
        }
    };
    private boolean mIsDrawDone = false;
    ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            ViewTreeObserver viewTreeObserver;
            KeyboardManager.this.mIsDrawDone = true;
            if (KeyboardManager.this.mDecorView != null && (viewTreeObserver = KeyboardManager.this.mDecorView.getViewTreeObserver()) != null) {
                viewTreeObserver.removeOnPreDrawListener(KeyboardManager.this.mOnPreDrawListener);
            }
            KeyboardManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INIT_KEYBOARD_DONE));
            return true;
        }
    };
    ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            ViewTreeObserver viewTreeObserver;
            if (KeyboardManager.this.mKeyboardView != null && (viewTreeObserver = KeyboardManager.this.mKeyboardView.getViewTreeObserver()) != null) {
                viewTreeObserver.removeOnGlobalLayoutListener(KeyboardManager.this.mOnGlobalLayoutListener);
            }
            KeyboardManager.this.setCandidatesViewShown(KeyboardManager.this.isCandidatesViewVisible());
        }
    };

    public KeyboardManager(IWnnIME wnn) {
        this.mWnn = null;
        this.mCandidatesManager = null;
        this.mMasterLayout = null;
        this.mCandidatesAndMenuLayout = null;
        this.mKeyboardMenuLayout = null;
        this.mDisableColorFilter = 0;
        this.mWnn = wnn;
        if (this.mWnn != null) {
            LayoutInflater inflater = this.mWnn.getLayoutInflater();
            this.mMasterLayout = (LinearLayout) inflater.inflate(R.layout.floating_layout, (ViewGroup) null);
            this.mCandidatesAndMenuLayout = (AbsoluteLayout) inflater.inflate(R.layout.cand_menu_layout, (ViewGroup) null);
            this.mKeyboardMenuLayout = (FrameLayout) inflater.inflate(R.layout.floatingbar, (ViewGroup) null);
            this.mDisableColorFilter = this.mWnn.getResources().getColor(R.color.floating_bar_disable_icon_color_filter);
            this.mCandidatesManager = this.mWnn.getCurrentCandidatesManager();
            transferKeysize();
        }
    }

    public boolean isProcessingKeyboardMenu() {
        return this.mIsProcessingKeyboardMenu;
    }

    public void setFloatingToPref(boolean isFloating) {
        if (this.mWnn != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(FLOATING_STATUS, isFloating);
            editor.commit();
        }
    }

    public boolean getFloatingFromPref() {
        if (this.mWnn == null || !isHardKeyboardHidden() || WnnAccessibility.isAccessibility(this.mWnn)) {
            return false;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        return pref.getBoolean(FLOATING_STATUS, false);
    }

    private void setKeyboardSize(Point size) {
        if (this.mWnn != null) {
            int orientation = this.mWnn.getResources().getConfiguration().orientation;
            setKeyboardSize(size, orientation);
        }
    }

    private void setKeyboardSize(Point size, int orientation) {
        if (this.mWnn != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor editor = pref.edit();
            if (getFloatingFromPref()) {
                if (orientation == 1) {
                    editor.putInt(FLOATING_WIDTH_PORT, size.x);
                    editor.putInt(FLOATING_HEIGHT_PORT, size.y);
                } else if (orientation == 2) {
                    editor.putInt(FLOATING_WIDTH_LAND, size.x);
                    editor.putInt(FLOATING_HEIGHT_LAND, size.y);
                } else {
                    return;
                }
            } else if (orientation == 1) {
                editor.putInt(FLOATING_OFF_WIDTH_PORT, size.x);
                editor.putInt(FLOATING_OFF_HEIGHT_PORT, size.y);
            } else if (orientation == 2) {
                editor.putInt(FLOATING_OFF_WIDTH_LAND, size.x);
                editor.putInt(FLOATING_OFF_HEIGHT_LAND, size.y);
            } else {
                return;
            }
            editor.commit();
        }
    }

    public Point getKeyboardSize(boolean checkHidden) {
        Point size = new Point(0, 0);
        if (this.mWnn != null) {
            if (this.mSizeOfResize.x > -1) {
                return this.mSizeOfResize;
            }
            Resources res = this.mWnn.getResources();
            int defaultHeight = getDefaultKeyboardHeight();
            size.y = defaultHeight;
            DisplayMetrics dm = res.getDisplayMetrics();
            int defaultWidth = dm.widthPixels;
            if (checkHidden && !isHardKeyboardHidden()) {
                size.x = defaultWidth;
                if (this.mKeyboardView != null) {
                    size.y = this.mKeyboardView.getMeasuredHeight();
                    return size;
                }
                return size;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            int orientation = res.getConfiguration().orientation;
            if (getFloatingFromPref()) {
                size.x = (int) res.getFraction(R.fraction.floating_default_width_rate, defaultWidth, defaultWidth);
                if (orientation == 1) {
                    size.x = pref.getInt(FLOATING_WIDTH_PORT, size.x);
                    size.y = pref.getInt(FLOATING_HEIGHT_PORT, size.y);
                } else if (orientation == 2) {
                    size.x = pref.getInt(FLOATING_WIDTH_LAND, size.x);
                    size.y = pref.getInt(FLOATING_HEIGHT_LAND, size.y);
                }
            } else {
                size.x = defaultWidth;
                if (orientation == 1) {
                    size.x = pref.getInt(FLOATING_OFF_WIDTH_PORT, size.x);
                    size.y = pref.getInt(FLOATING_OFF_HEIGHT_PORT, size.y);
                } else if (orientation == 2) {
                    size.x = pref.getInt(FLOATING_OFF_WIDTH_LAND, size.x);
                    size.y = pref.getInt(FLOATING_OFF_HEIGHT_LAND, size.y);
                }
            }
            int correct = getCorrectKeyboardHeight(size.y, defaultHeight, getDisplaySize().y - getVerticalOffset(), true);
            if (size.y != correct) {
                size.y = correct;
                setKeyboardSize(size);
                IWnnImeEvent ev = new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW);
                this.mWnn.onEvent(ev);
                return size;
            }
            return size;
        }
        return size;
    }

    public PointF getKeyboardScaleRate() {
        Point size;
        PointF rate = new PointF(DEFAULT_ALPHA_LEVEL, DEFAULT_ALPHA_LEVEL);
        if (this.mWnn != null) {
            int defaultHeight = getMaxKeyboardHeight();
            Resources res = this.mWnn.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            int defaultWidth = dm.widthPixels;
            if (this.mSizeOfResize.x > -1) {
                size = this.mSizeOfResize;
            } else {
                size = getKeyboardSize(false);
            }
            rate.x = size.x / defaultWidth;
            rate.y = size.y / defaultHeight;
        }
        return rate;
    }

    private boolean isCandidatesViewVisible() {
        return this.mCandidatesView != null && this.mCandidatesView.getVisibility() == 0;
    }

    private int getVerticalOffset() {
        if (this.mWnn == null) {
            return 0;
        }
        if (getFloatingFromPref()) {
            if (!isEnableConverter()) {
                return 0;
            }
            int ret = this.mWnn.getHeightOfFunFunWindow();
            return ret;
        }
        int ret2 = this.mWnn.getResources().getDimensionPixelSize(R.dimen.floatingoff_move_vertical_offset);
        return ret2;
    }

    public int getKeyboardMenuHeight() {
        if (this.mCandidatesManager == null || !isHardKeyboardHidden() || WnnAccessibility.isAccessibility(this.mWnn)) {
            return 0;
        }
        if (!getKeyboardMenuState()) {
            return this.mKeyboardMenuDisableIconHeight;
        }
        return this.mCandidatesManager.getCandidatesAreaHeight(0, true);
    }

    private void setKeyboardPosition(int posX, int posY) {
        if (this.mWnn != null) {
            int orientation = this.mWnn.getResources().getConfiguration().orientation;
            setKeyboardPosition(posX, posY, orientation);
        }
    }

    private void setKeyboardPosition(int posX, int posY, int orientation) {
        String keyX;
        String keyY;
        if (this.mWnn != null) {
            if (getFloatingFromPref()) {
                if (orientation == 1) {
                    keyX = FLOATING_POSX_PORT;
                    keyY = FLOATING_POSY_PORT;
                } else if (orientation == 2) {
                    keyX = FLOATING_POSX_LAND;
                    keyY = FLOATING_POSY_LAND;
                } else {
                    return;
                }
            } else if (orientation == 1) {
                keyX = FLOATING_OFF_POSX_PORT;
                keyY = FLOATING_OFF_POSY_PORT;
            } else if (orientation == 2) {
                keyX = FLOATING_OFF_POSX_LAND;
                keyY = FLOATING_OFF_POSY_LAND;
            } else {
                return;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(keyX, posX);
            editor.putInt(keyY, posY);
            editor.commit();
        }
    }

    public Point getKeyboardPosition() {
        Point position = new Point(0, 0);
        if (this.mWnn != null) {
            if (!isHardKeyboardHidden()) {
                position.x = 0;
                if (this.mKeyboardView != null) {
                    position.y = getDisplaySize().y - this.mKeyboardView.getMeasuredHeight();
                }
            } else {
                int popupPosX = -99999;
                int popupPosY = -99999;
                Resources res = this.mWnn.getResources();
                int orientation = res.getConfiguration().orientation;
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
                boolean isFloatingMode = getFloatingFromPref();
                if (isFloatingMode) {
                    if (orientation == 1) {
                        popupPosX = pref.getInt(FLOATING_POSX_PORT, -99999);
                        popupPosY = pref.getInt(FLOATING_POSY_PORT, -99999);
                    } else if (orientation == 2) {
                        popupPosX = pref.getInt(FLOATING_POSX_LAND, -99999);
                        popupPosY = pref.getInt(FLOATING_POSY_LAND, -99999);
                    }
                } else if (orientation == 1) {
                    popupPosX = pref.getInt(FLOATING_OFF_POSX_PORT, -99999);
                    popupPosY = pref.getInt(FLOATING_OFF_POSY_PORT, -99999);
                } else if (orientation == 2) {
                    popupPosX = pref.getInt(FLOATING_OFF_POSX_LAND, -99999);
                    popupPosY = pref.getInt(FLOATING_OFF_POSY_LAND, -99999);
                }
                Point size = getKeyboardSize(true);
                Point displaySize = getDisplaySize();
                if (popupPosX == -99999 && popupPosY == -99999) {
                    int offsetYPos = 0;
                    if (isFloatingMode) {
                        offsetYPos = res.getDimensionPixelSize(R.dimen.floating_default_position_y_offset);
                    }
                    popupPosX = (displaySize.x - size.x) / 2;
                    popupPosY = (displaySize.y - size.y) - offsetYPos;
                    setKeyboardPosition(popupPosX, popupPosY);
                } else {
                    int imeTop = getImeTopPosition(popupPosY, false, false);
                    int offset = getVerticalOffset();
                    if (imeTop < offset) {
                        popupPosY += offset - imeTop;
                    }
                    if (displaySize.y < size.y + popupPosY) {
                        popupPosY = displaySize.y - size.y;
                    }
                }
                position.x = popupPosX;
                position.y = popupPosY;
            }
        }
        return position;
    }

    public void initKeyboard() {
        if (this.mDecorView == null) {
            this.mDecorView = this.mWnn.getWindow().getWindow().getDecorView();
            this.mIsDrawDone = false;
            ViewTreeObserver viewTreeObserver = this.mDecorView.getViewTreeObserver();
            if (viewTreeObserver != null) {
                viewTreeObserver.addOnPreDrawListener(this.mOnPreDrawListener);
            }
        }
    }

    public boolean isInitialized() {
        return this.mIsDrawDone;
    }

    public void showKeyboard() {
        AbsoluteLayout targetLayout;
        if (this.mWnn != null && this.mIsDrawDone) {
            if (this.mWnn.onEvaluateFullscreenMode() && getFloatingFromPref()) {
                targetLayout = this.mWnn.getFullScreenViewBaseLayout();
            } else {
                targetLayout = this.mWnn.getLayoutOfSetKeyboard();
            }
            if (targetLayout != null && targetLayout.indexOfChild(this.mMasterLayout) == -1) {
                removeMessages();
                ViewGroup parent = (ViewGroup) this.mMasterLayout.getParent();
                if (parent != null) {
                    parent.removeView(this.mMasterLayout);
                }
                this.mCandidatesAndMenuLayout.removeAllViews();
                this.mMasterLayout.removeAllViews();
                createViews();
                updateMenuIcons();
                targetLayout.addView(this.mMasterLayout);
                setCandidatesViewShown(!this.mWnn.isAutoHideMode());
            }
        }
    }

    private void createViews() {
        AbsoluteLayout topLayout;
        if (this.mWnn != null && this.mCandidatesManager != null) {
            this.mCandidatesView = this.mCandidatesManager.getCurrentView();
            if (this.mCandidatesView != null) {
                this.mKeyboard = this.mWnn.getCurrentDefaultSoftKeyboard();
                if (this.mKeyboard != null) {
                    this.mKeyboardView = this.mKeyboard.getCurrentView();
                    if (this.mKeyboardView != null) {
                        this.mIsProcessingKeyboardMenu = false;
                        ViewGroup parent = (ViewGroup) this.mCandidatesAndMenuLayout.getParent();
                        if (parent != null) {
                            parent.removeView(this.mCandidatesAndMenuLayout);
                        }
                        ViewGroup layout = this.mMasterLayout;
                        if (this.mWnn.isHwCandWindow() && (topLayout = this.mWnn.getLayoutTop()) != null) {
                            layout = topLayout;
                        }
                        layout.addView(this.mCandidatesAndMenuLayout);
                        if (this.mCandidatesView.getParent() != null) {
                            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW));
                            this.mCandidatesView = this.mCandidatesManager.getCurrentView();
                        }
                        this.mCandidatesView.setVisibility(8);
                        Point size = getKeyboardSize(true);
                        int paramWidth = size.x;
                        int paramHeight = size.y;
                        if (!isHardKeyboardHidden()) {
                            paramWidth = -1;
                            paramHeight = -2;
                        }
                        AbsoluteLayout.LayoutParams candLayoutParam = new AbsoluteLayout.LayoutParams(paramWidth, -2, 0, 0);
                        this.mCandidatesAndMenuLayout.addView(this.mCandidatesView, candLayoutParam);
                        LinearLayout layoutBar = (LinearLayout) this.mKeyboardMenuLayout.findViewById(R.id.floating_bar_layout);
                        FrameLayout.LayoutParams barParam = (FrameLayout.LayoutParams) layoutBar.getLayoutParams();
                        barParam.width = paramWidth;
                        barParam.height = this.mCandidatesManager.getCandidatesAreaHeight(0, true);
                        layoutBar.setLayoutParams(barParam);
                        setFloatingOnOffIcon();
                        setSeekIcon();
                        setMenuOffIcon();
                        setMenuOnArea();
                        setResizeIcon();
                        setMoveIcon();
                        setOverlay();
                        updateKeyboardMenu();
                        AbsoluteLayout.LayoutParams barLayoutParam = new AbsoluteLayout.LayoutParams(paramWidth, -2, 0, 0);
                        this.mCandidatesAndMenuLayout.addView(this.mKeyboardMenuLayout, barLayoutParam);
                        this.mKeyboardView.setVisibility(0);
                        this.mKeyboardView.setLayoutParams(new LinearLayout.LayoutParams(paramWidth, paramHeight));
                        this.mMasterLayout.addView(this.mKeyboardView);
                    }
                }
            }
        }
    }

    private void setMoveIcon() {
        ImageView moveView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_move);
        moveView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!KeyboardManager.this.checkEnableEvent(event) || KeyboardManager.this.mWnn == null) {
                    return true;
                }
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                int action = event.getAction();
                switch (action) {
                    case 0:
                        KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                        KeyboardManager.this.mOrientation = KeyboardManager.this.mWnn.getResources().getConfiguration().orientation;
                        KeyboardManager.this.mPopupLocation = KeyboardManager.this.getKeyboardPosition();
                        KeyboardManager.this.mSizeOfStartProcess = KeyboardManager.this.getKeyboardSize(true);
                        KeyboardManager.this.mOffsetPointer.x = x;
                        KeyboardManager.this.mOffsetPointer.y = y;
                        KeyboardManager.this.mPositionOfMove.x = -1;
                        KeyboardManager.this.mPositionOfMove.y = -1;
                        return true;
                    case 1:
                    case 2:
                        int popX = KeyboardManager.this.mPopupLocation.x + (x - KeyboardManager.this.mOffsetPointer.x);
                        int popY = KeyboardManager.this.mPopupLocation.y + (y - KeyboardManager.this.mOffsetPointer.y);
                        boolean isFloatingMode = KeyboardManager.this.getFloatingFromPref();
                        Point displaySize = KeyboardManager.this.getDisplaySize();
                        int imeTop = KeyboardManager.this.getImeTopPosition(popY, false, false);
                        int candHeight = popY - imeTop;
                        KeyboardManager.this.mPositionOfMove.x = WnnUtility.adjustValue(popX, 0, displaySize.x - KeyboardManager.this.mSizeOfStartProcess.x);
                        KeyboardManager.this.mPositionOfMove.y = WnnUtility.adjustValue(popY, KeyboardManager.this.getVerticalOffset() + candHeight, displaySize.y - KeyboardManager.this.mSizeOfStartProcess.y);
                        if (!isFloatingMode || action != 2) {
                            popX = KeyboardManager.this.mPositionOfMove.x;
                            popY = KeyboardManager.this.mPositionOfMove.y;
                        }
                        int paramY = KeyboardManager.this.getImeTopPosition(popY, true, true);
                        KeyboardManager.this.mWnn.updateInputBackView(paramY);
                        if (!KeyboardManager.this.mWnn.onEvaluateFullscreenMode() || !isFloatingMode) {
                            paramY = 0;
                        }
                        AbsoluteLayout.LayoutParams param = (AbsoluteLayout.LayoutParams) KeyboardManager.this.mMasterLayout.getLayoutParams();
                        param.x = popX;
                        param.y = paramY;
                        KeyboardManager.this.mMasterLayout.setLayoutParams(param);
                        if (action != 2) {
                            if (KeyboardManager.this.mPositionOfMove.x > -1 && KeyboardManager.this.mPositionOfMove.y > -1) {
                                KeyboardManager.this.setKeyboardPosition(KeyboardManager.this.mPositionOfMove.x, KeyboardManager.this.mPositionOfMove.y, KeyboardManager.this.mOrientation);
                            }
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            KeyboardManager.this.mPositionOfMove.x = -1;
                            KeyboardManager.this.mPositionOfMove.y = -1;
                        }
                        return true;
                    case 3:
                        break;
                    default:
                        return true;
                }
            }
        });
    }

    private void setResizeIcon() {
        if (this.mWnn != null) {
            final Resources res = this.mWnn.getResources();
            View resizeView = this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_resize);
            resizeView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!KeyboardManager.this.checkEnableEvent(event)) {
                        return true;
                    }
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();
                    if (KeyboardManager.this.mCandidatesManager != null && KeyboardManager.this.mKeyboard != null && KeyboardManager.this.mKeyboardView != null && KeyboardManager.this.mCandidatesView != null) {
                        boolean isSymbolMode = KeyboardManager.this.mCandidatesManager.isSymbolMode();
                        int defaultHeight = KeyboardManager.this.getDefaultKeyboardHeight();
                        int action = event.getAction();
                        switch (action) {
                            case 0:
                                KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                                KeyboardManager.this.mOrientation = KeyboardManager.this.mWnn.getResources().getConfiguration().orientation;
                                KeyboardManager.this.mSizeOfResize.x = -1;
                                KeyboardManager.this.mSizeOfResize.y = -1;
                                KeyboardManager.this.mPopupLocation = KeyboardManager.this.getKeyboardPosition();
                                KeyboardManager.this.mSizeOfStartProcess = KeyboardManager.this.getKeyboardSize(true);
                                KeyboardManager.this.mOffsetPointer.x = x;
                                KeyboardManager.this.mOffsetPointer.y = y;
                                int bottom = KeyboardManager.this.mPopupLocation.y + KeyboardManager.this.mSizeOfStartProcess.y;
                                KeyboardManager.this.mSizeOfMaxResize.y = KeyboardManager.this.getCorrectKeyboardHeight(KeyboardManager.this.getMaxKeyboardHeight(), defaultHeight, bottom - KeyboardManager.this.getVerticalOffset(), true);
                                int displayWidth = KeyboardManager.this.getDisplaySize().x;
                                KeyboardManager.this.mSizeOfMaxResize.x = displayWidth - KeyboardManager.this.mPopupLocation.x;
                                KeyboardManager.this.mSizeOfMinResize.y = (int) res.getFraction(R.fraction.keyboard_min_height_rate, defaultHeight, defaultHeight);
                                KeyboardManager.this.mSizeOfMinResize.x = (int) res.getFraction(R.fraction.keyboard_min_width_rate, displayWidth, displayWidth);
                                KeyboardManager.this.mResizeStartTotalHeight = bottom - KeyboardManager.this.getImeTopPosition(KeyboardManager.this.mPopupLocation.y, true, true);
                                KeyboardManager.this.mHandler.sendMessage(KeyboardManager.this.mHandler.obtainMessage(1, Boolean.valueOf(isSymbolMode)));
                                return true;
                            case 1:
                            case 2:
                                int x2 = x - KeyboardManager.this.mOffsetPointer.x;
                                int y2 = KeyboardManager.this.mOffsetPointer.y - y;
                                KeyboardManager.this.mSizeOfResize.set(KeyboardManager.this.mSizeOfStartProcess.x, KeyboardManager.this.mSizeOfStartProcess.y);
                                KeyboardManager.this.mSizeOfResize.x += x2;
                                KeyboardManager.this.mSizeOfResize.y = KeyboardManager.this.getCorrectKeyboardHeight(KeyboardManager.this.getMaxKeyboardHeight(), defaultHeight, KeyboardManager.this.mResizeStartTotalHeight + y2, KeyboardManager.this.isCandidatesViewVisible());
                                KeyboardManager.this.mSizeOfResize.x = WnnUtility.adjustValue(KeyboardManager.this.mSizeOfResize.x, KeyboardManager.this.mSizeOfMinResize.x, KeyboardManager.this.mSizeOfMaxResize.x);
                                KeyboardManager.this.mSizeOfResize.y = WnnUtility.adjustValue(KeyboardManager.this.mSizeOfResize.y, KeyboardManager.this.mSizeOfMinResize.y, KeyboardManager.this.mSizeOfMaxResize.y);
                                int candHeight = KeyboardManager.this.mCandidatesManager.convertScaleFactorToCandHeight(KeyboardManager.this.mSizeOfResize.y / defaultHeight);
                                KeyboardManager.this.mCandidatesManager.updateParameters(KeyboardManager.this.mSizeOfResize.x, candHeight);
                                int menuHeight = KeyboardManager.this.getKeyboardMenuHeight();
                                AbsoluteLayout.LayoutParams AbsoluteParam = (AbsoluteLayout.LayoutParams) KeyboardManager.this.mCandidatesView.getLayoutParams();
                                AbsoluteParam.width = KeyboardManager.this.mSizeOfResize.x;
                                if (isSymbolMode) {
                                    AbsoluteParam.y = menuHeight;
                                }
                                KeyboardManager.this.mCandidatesView.setLayoutParams(AbsoluteParam);
                                LinearLayout layoutBar = (LinearLayout) KeyboardManager.this.mKeyboardMenuLayout.findViewById(R.id.floating_bar_layout);
                                FrameLayout.LayoutParams frameParam = (FrameLayout.LayoutParams) layoutBar.getLayoutParams();
                                frameParam.width = KeyboardManager.this.mSizeOfResize.x;
                                frameParam.height = menuHeight;
                                layoutBar.setLayoutParams(frameParam);
                                AbsoluteLayout.LayoutParams AbsoluteParam2 = (AbsoluteLayout.LayoutParams) KeyboardManager.this.mKeyboardMenuLayout.getLayoutParams();
                                AbsoluteParam2.width = KeyboardManager.this.mSizeOfResize.x;
                                KeyboardManager.this.mKeyboardMenuLayout.setLayoutParams(AbsoluteParam2);
                                LinearLayout.LayoutParams linearParam = (LinearLayout.LayoutParams) KeyboardManager.this.mKeyboardView.getLayoutParams();
                                linearParam.width = KeyboardManager.this.mSizeOfResize.x;
                                linearParam.height = KeyboardManager.this.mSizeOfResize.y;
                                KeyboardManager.this.mKeyboardView.setLayoutParams(linearParam);
                                if (isSymbolMode) {
                                    KeyboardManager.this.mCandidatesManager.resizeSymbolList();
                                } else {
                                    KeyboardView keyboardView = KeyboardManager.this.mKeyboard.getKeyboardView();
                                    if (keyboardView != null) {
                                        keyboardView.getKeyboard().changeKeyboardSize(KeyboardManager.this.mSizeOfResize.x, KeyboardManager.this.mSizeOfResize.y);
                                        keyboardView.requestLayout();
                                    }
                                }
                                int posY = KeyboardManager.this.mPopupLocation.y - (KeyboardManager.this.mSizeOfResize.y - KeyboardManager.this.mSizeOfStartProcess.y);
                                int paramY = KeyboardManager.this.getImeTopPosition(posY, true, true);
                                KeyboardManager.this.mWnn.updateInputBackView(paramY);
                                if (action == 2) {
                                    if (!KeyboardManager.this.mWnn.onEvaluateFullscreenMode() || !KeyboardManager.this.getFloatingFromPref()) {
                                        paramY = 0;
                                    }
                                    AbsoluteLayout.LayoutParams param = (AbsoluteLayout.LayoutParams) KeyboardManager.this.mMasterLayout.getLayoutParams();
                                    param.y = paramY;
                                    KeyboardManager.this.mMasterLayout.setLayoutParams(param);
                                } else {
                                    if (KeyboardManager.this.mSizeOfResize.x > -1 && KeyboardManager.this.mSizeOfResize.y > -1) {
                                        KeyboardManager.this.setKeyboardSize(KeyboardManager.this.mSizeOfResize, KeyboardManager.this.mOrientation);
                                        KeyboardManager.this.setKeyboardPosition(KeyboardManager.this.mPopupLocation.x, KeyboardManager.this.mPopupLocation.y - (KeyboardManager.this.mSizeOfResize.y - KeyboardManager.this.mSizeOfStartProcess.y), KeyboardManager.this.mOrientation);
                                        KeyboardManager.this.mHandler.sendMessage(KeyboardManager.this.mHandler.obtainMessage(3));
                                    }
                                    KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                                    KeyboardManager.this.mSizeOfResize.x = -1;
                                    KeyboardManager.this.mSizeOfResize.y = -1;
                                }
                                return true;
                            case 3:
                                break;
                            default:
                                return true;
                        }
                    } else {
                        return true;
                    }
                }
            });
        }
    }

    private void setSeekIcon() {
        if (this.mKeyboardView != null && this.mCandidatesView != null) {
            final SeekBar seekBar = (SeekBar) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_seekbar);
            ImageView switchView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_set_seekbar);
            final boolean isFloatingMode = getFloatingFromPref();
            switchView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (KeyboardManager.this.checkEnableEvent(event) && isFloatingMode) {
                        int action = event.getAction();
                        switch (action) {
                            case 0:
                                KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                                break;
                            case 1:
                                if (seekBar.getVisibility() == 8) {
                                    KeyboardManager.this.setSeekBar();
                                } else {
                                    KeyboardManager.this.updateMenuIcons();
                                }
                                KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                                break;
                            case 3:
                                KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                                break;
                        }
                    }
                    return true;
                }
            });
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            float alpha = DEFAULT_ALPHA_LEVEL;
            if (isFloatingMode) {
                alpha = pref.getFloat(FLOATING_ALPHA, DEFAULT_ALPHA_LEVEL);
            }
            this.mCandidatesView.setAlpha(alpha);
            this.mKeyboardView.setAlpha(alpha);
            Resources res = this.mWnn.getResources();
            this.mMaxAlphaLevel = res.getInteger(R.integer.floating_max_alpha);
            seekBar.setMax(this.mMaxAlphaLevel);
            seekBar.setProgress(((int) (100.0f * alpha)) - (100 - this.mMaxAlphaLevel));
            seekBar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return !KeyboardManager.this.checkEnableEvent(event);
                }
            });
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar2, int progress, boolean fromTouch) {
                    float alpha2 = (100 - (KeyboardManager.this.mMaxAlphaLevel - progress)) / 100.0f;
                    KeyboardManager.this.mKeyboardView.setAlpha(alpha2);
                    KeyboardManager.this.mCandidatesView.setAlpha(alpha2);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar2) {
                    KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar2) {
                    int progress = seekBar2.getProgress();
                    float alpha2 = (100 - (KeyboardManager.this.mMaxAlphaLevel - progress)) / 100.0f;
                    KeyboardManager.this.mKeyboardView.setAlpha(alpha2);
                    KeyboardManager.this.mCandidatesView.setAlpha(alpha2);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putFloat(KeyboardManager.FLOATING_ALPHA, alpha2);
                    editor.commit();
                    KeyboardManager.this.updateMenuIcons();
                    KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                }
            });
        }
    }

    private void setOverlay() {
        this.mKeyboardMenuOverlay = (TextView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_overlay);
        this.mKeyboardMenuOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                KeyboardView keyview = KeyboardManager.this.mKeyboard.getKeyboardView();
                if (keyview != null && keyview.isMiniKeyboardOnScreen()) {
                    keyview.dismissPopupKeyboard();
                    return true;
                }
                return true;
            }
        });
    }

    private void setFloatingOnOffIcon() {
        ImageView floatingOnView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_floating_on);
        ImageView floatingOffView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_floating_off);
        floatingOnView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (KeyboardManager.this.checkEnableEvent(event)) {
                    int action = event.getAction();
                    switch (action) {
                        case 0:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                            break;
                        case 1:
                            KeyboardManager.this.mHandler.sendMessage(KeyboardManager.this.mHandler.obtainMessage(2, true));
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                        case 3:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                    }
                }
                return true;
            }
        });
        floatingOffView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (KeyboardManager.this.checkEnableEvent(event)) {
                    int action = event.getAction();
                    switch (action) {
                        case 0:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                            break;
                        case 1:
                            KeyboardManager.this.mHandler.sendMessage(KeyboardManager.this.mHandler.obtainMessage(2, false));
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                        case 3:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                    }
                }
                return true;
            }
        });
    }

    private void setMenuOffIcon() {
        ImageView barOffView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_bar_close);
        barOffView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (KeyboardManager.this.checkEnableEvent(event)) {
                    int action = event.getAction();
                    switch (action) {
                        case 0:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                            break;
                        case 1:
                            KeyboardManager.this.setKeyboardMenuState(false);
                            KeyboardManager.this.setCandidatesViewShown(KeyboardManager.this.isCandidatesViewVisible());
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                        case 3:
                            KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                            break;
                    }
                }
                return true;
            }
        });
    }

    private void setMenuOnArea() {
        if (this.mWnn != null) {
            ImageView barOnView = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_on);
            FrameLayout.LayoutParams barParam = (FrameLayout.LayoutParams) barOnView.getLayoutParams();
            Resources res = this.mWnn.getResources();
            this.mKeyboardMenuDisableIconHeight = res.getDimensionPixelSize(R.dimen.cand_minimum_height);
            this.mKeyboardMenuDisableIconHeight = (int) res.getFraction(R.fraction.floatingbar_state_off_height_ratio, this.mKeyboardMenuDisableIconHeight, this.mKeyboardMenuDisableIconHeight);
            barParam.height = this.mKeyboardMenuDisableIconHeight;
            barOnView.setLayoutParams(barParam);
            barOnView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (KeyboardManager.this.checkEnableEvent(event)) {
                        int action = event.getAction();
                        switch (action) {
                            case 0:
                                KeyboardManager.this.mIsProcessingKeyboardMenu = true;
                                break;
                            case 1:
                                KeyboardManager.this.setKeyboardMenuState(true);
                                KeyboardManager.this.setCandidatesViewShown(KeyboardManager.this.isCandidatesViewVisible());
                                KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                                break;
                            case 3:
                                KeyboardManager.this.mIsProcessingKeyboardMenu = false;
                                break;
                        }
                    }
                    return true;
                }
            });
        }
    }

    private boolean checkEnableEvent(MotionEvent event) {
        return event.getPointerId(event.getActionIndex()) <= 0;
    }

    private void setSeekBar() {
        setVisibilityOfMenuIcon(R.id.floatingbar_move, 8);
        setVisibilityOfMenuIcon(R.id.floatingbar_resize, 8);
        setVisibilityOfMenuIcon(R.id.floatingbar_floating_on, 4);
        setVisibilityOfMenuIcon(R.id.floatingbar_floating_off, 8);
        setVisibilityOfMenuIcon(R.id.floatingbar_bar_close, 8);
        setVisibilityOfMenuIcon(R.id.floatingbar_seekbar, 0);
    }

    private void updateMenuIcons() {
        setVisibilityOfMenuIcon(R.id.floatingbar_seekbar, 8);
        setVisibilityOfMenuIcon(R.id.floatingbar_move, 0);
        setVisibilityOfMenuIcon(R.id.floatingbar_set_seekbar, 0);
        setVisibilityOfMenuIcon(R.id.floatingbar_bar_close, 0);
        setVisibilityOfMenuIcon(R.id.floatingbar_resize, 0);
        boolean isFloatingMode = getFloatingFromPref();
        if (isFloatingMode) {
            setVisibilityOfMenuIcon(R.id.floatingbar_floating_on, 8);
            setVisibilityOfMenuIcon(R.id.floatingbar_floating_off, 0);
        } else {
            setVisibilityOfMenuIcon(R.id.floatingbar_floating_on, 0);
            setVisibilityOfMenuIcon(R.id.floatingbar_floating_off, 8);
        }
        ImageView seekBar = (ImageView) this.mKeyboardMenuLayout.findViewById(R.id.floatingbar_set_seekbar);
        if (seekBar != null) {
            if (!isFloatingMode) {
                seekBar.setColorFilter(this.mDisableColorFilter, PorterDuff.Mode.SRC_IN);
            } else {
                seekBar.clearColorFilter();
            }
        }
    }

    private void setVisibilityOfMenuIcon(int targetId, int visibility) {
        View targetView = this.mKeyboardMenuLayout.findViewById(targetId);
        if (targetView != null) {
            targetView.setVisibility(visibility);
        }
    }

    public void removeMessages() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
    }

    public int getKeyboardViewHeight() {
        if (this.mKeyboardView == null) {
            return 0;
        }
        return this.mKeyboardView.getMeasuredHeight();
    }

    public void setCandidatesViewShown(boolean shown) {
        Point position;
        if (this.mWnn != null && this.mKeyboardView != null && this.mCandidatesView != null) {
            updateCandidateAndMenuLayout();
            if (shown) {
                this.mCandidatesView.setVisibility(0);
            } else {
                this.mCandidatesView.setVisibility(8);
            }
            int paramWidth = -2;
            if (isHardKeyboardHidden()) {
                position = getKeyboardPosition();
            } else {
                int keyboardViewHeight = 0;
                if (!this.mWnn.isSubtypeEmojiInput() && (keyboardViewHeight = this.mKeyboardView.getMeasuredHeight()) == 0) {
                    keyboardViewHeight = getDefaultKeyboardHeight();
                    ViewTreeObserver viewTreeObserver = this.mKeyboardView.getViewTreeObserver();
                    if (viewTreeObserver != null) {
                        viewTreeObserver.addOnGlobalLayoutListener(this.mOnGlobalLayoutListener);
                    }
                }
                position = new Point(0, getDisplaySize().y - keyboardViewHeight);
                paramWidth = -1;
            }
            int paramY = getImeTopPosition(position.y, true, true);
            this.mWnn.updateInputBackView(paramY);
            if (!this.mWnn.onEvaluateFullscreenMode() || !getFloatingFromPref()) {
                paramY = 0;
            }
            AbsoluteLayout.LayoutParams param = (AbsoluteLayout.LayoutParams) this.mMasterLayout.getLayoutParams();
            param.width = paramWidth;
            param.height = -2;
            param.x = position.x;
            param.y = paramY;
            this.mMasterLayout.setLayoutParams(param);
            this.mWnn.setCandidatesViewShown(shown);
        }
    }

    public void updateCandidateAndMenuLayout() {
        if (this.mWnn != null && !this.mWnn.isHwCandWindow() && this.mCandidatesView != null) {
            boolean isSymbolMode = false;
            if (this.mCandidatesManager != null) {
                isSymbolMode = this.mCandidatesManager.isSymbolMode();
            }
            AbsoluteLayout.LayoutParams param = (AbsoluteLayout.LayoutParams) this.mCandidatesView.getLayoutParams();
            if (isSymbolMode) {
                this.mCandidatesManager.setViewType(1);
                param.y = getKeyboardMenuHeight();
            } else {
                param.y = 0;
            }
            updateMenuIcons();
            updateKeyboardMenu();
            this.mCandidatesView.setLayoutParams(param);
        }
    }

    public void setColorOverlay(int color) {
        if (this.mKeyboard != null && this.mKeyboardMenuOverlay != null) {
            KeyboardView keyview = this.mKeyboard.getKeyboardView();
            boolean enable = false;
            if (keyview != null) {
                enable = keyview.isMiniKeyboardOnScreen();
            }
            if (enable) {
                this.mKeyboardMenuOverlay.setBackgroundColor(color);
                this.mKeyboardMenuOverlay.setVisibility(0);
            } else {
                this.mKeyboardMenuOverlay.setVisibility(8);
            }
        }
    }

    public Point getDisplaySize() {
        Rect rect = new Rect(0, 0, 0, 0);
        if (this.mDecorView != null) {
            this.mDecorView.getWindowVisibleDisplayFrame(rect);
        }
        Point ret = new Point(0, 0);
        DisplayMetrics dm = this.mWnn.getResources().getDisplayMetrics();
        ret.x = dm.widthPixels;
        ret.y = dm.heightPixels - rect.top;
        return ret;
    }

    private void setKeyboardMenuState(boolean state) {
        if (this.mWnn != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(KEYBOARD_MENU_STATE, state);
            editor.commit();
        }
    }

    private boolean getKeyboardMenuState() {
        if (this.mWnn == null || !isHardKeyboardHidden() || WnnAccessibility.isAccessibility(this.mWnn)) {
            return false;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        boolean state = this.mWnn.getResources().getBoolean(R.bool.floatingbar_default_state);
        return pref.getBoolean(KEYBOARD_MENU_STATE, state);
    }

    private void updateKeyboardMenu() {
        if (this.mWnn != null && this.mKeyboardMenuOverlay != null) {
            if (!isHardKeyboardHidden()) {
                this.mKeyboardMenuLayout.setVisibility(8);
                return;
            }
            if (WnnAccessibility.isAccessibility(this.mWnn)) {
                this.mKeyboardMenuLayout.setVisibility(8);
                return;
            }
            if (this.mCandidatesManager != null && !this.mCandidatesManager.isSymbolMode() && this.mCandidatesManager.hasCandidates()) {
                this.mKeyboardMenuLayout.setVisibility(8);
                return;
            }
            this.mKeyboardMenuLayout.setVisibility(0);
            LinearLayout layoutBar = (LinearLayout) this.mKeyboardMenuLayout.findViewById(R.id.floating_bar_layout);
            if (getKeyboardMenuState()) {
                layoutBar.setVisibility(0);
            } else {
                layoutBar.setVisibility(8);
            }
            FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) this.mKeyboardMenuOverlay.getLayoutParams();
            param.height = getKeyboardMenuHeight();
            this.mKeyboardMenuOverlay.setLayoutParams(param);
        }
    }

    private int getCorrectKeyboardHeight(int keyboardHeight, int defaultHeight, int maxHeight, boolean isCurrentLine) {
        if (this.mWnn != null && this.mCandidatesManager != null) {
            int candLine = 1;
            if (isCurrentLine) {
                candLine = this.mCandidatesManager.getNumberOfLine();
            }
            int candHeight = this.mCandidatesManager.convertScaleFactorToCandHeight(keyboardHeight / defaultHeight) * candLine;
            int tempTotalHeight = keyboardHeight + candHeight;
            while (maxHeight < tempTotalHeight) {
                keyboardHeight--;
                int candHeight2 = this.mCandidatesManager.convertScaleFactorToCandHeight(keyboardHeight / defaultHeight) * candLine;
                tempTotalHeight = keyboardHeight + candHeight2;
            }
            return keyboardHeight;
        }
        return keyboardHeight;
    }

    public int getImeTopPosition(int keyboardPosY, boolean checkVisibility, boolean checkLessThan) {
        if (this.mCandidatesManager == null) {
            return 0;
        }
        int offset = 0;
        if (!this.mWnn.isHwCandWindow()) {
            offset = this.mCandidatesManager.getCandidatesAreaHeight(2, checkVisibility);
        }
        if (offset == 0) {
            offset = getKeyboardMenuHeight();
        }
        int ret = keyboardPosY - offset;
        if (checkLessThan && ret < 0) {
            return 0;
        }
        return ret;
    }

    public int getDefaultKeyboardHeight() {
        float checkHeight;
        if (this.mWnn == null) {
            return 0;
        }
        Resources res = this.mWnn.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        String keyboardHeightString = getDeviceOverrideValue(R.array.keyboard_heights);
        if (TextUtils.isEmpty(keyboardHeightString)) {
            checkHeight = res.getDimension(R.dimen.keyboardHeight);
        } else {
            checkHeight = Float.parseFloat(keyboardHeightString) * res.getDisplayMetrics().density;
        }
        float maxKeyboardHeight = res.getFraction(R.fraction.maxKeyboardHeight, dm.heightPixels, dm.heightPixels);
        float minKeyboardHeight = res.getFraction(R.fraction.minKeyboardHeight, dm.heightPixels, dm.heightPixels);
        if (minKeyboardHeight < 0.0f) {
            minKeyboardHeight = -res.getFraction(R.fraction.minKeyboardHeight, dm.widthPixels, dm.widthPixels);
        }
        return (int) Math.max(Math.min(checkHeight, maxKeyboardHeight), minKeyboardHeight);
    }

    public int getMaxKeyboardHeight() {
        if (this.mWnn == null) {
            return 0;
        }
        int ret = getDefaultKeyboardHeight();
        return (int) this.mWnn.getResources().getFraction(R.fraction.keyboard_max_height_rate, ret, ret);
    }

    private void transferKeysize() {
        if (this.mWnn != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor editor = pref.edit();
            if (pref.contains(KEY_HEIGHT_PORTRAIT_KEY)) {
                editor.remove(KEY_HEIGHT_PORTRAIT_KEY);
                editor.putInt(FLOATING_OFF_HEIGHT_PORT, calculateKeyHeightToKeyboardHeight(1, pref.getInt(KEY_HEIGHT_PORTRAIT_KEY, 0)));
            }
            if (pref.contains(KEY_HEIGHT_LANDSCAPE_KEY)) {
                editor.remove(KEY_HEIGHT_LANDSCAPE_KEY);
                editor.putInt(FLOATING_OFF_HEIGHT_LAND, calculateKeyHeightToKeyboardHeight(2, pref.getInt(KEY_HEIGHT_LANDSCAPE_KEY, 0)));
            }
            editor.commit();
        }
    }

    private int calculateKeyHeightToKeyboardHeight(int orientation, int keyheight) {
        if (this.mWnn == null) {
            return 0;
        }
        Resources res = this.mWnn.getResources();
        Configuration nowConfig = res.getConfiguration();
        int nowOrientation = nowConfig.orientation;
        nowConfig.orientation = orientation;
        res.updateConfiguration(nowConfig, res.getDisplayMetrics());
        int keyboardHeight = keyheight * 4;
        int tempDefaultVerticalGap = res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_vertical_gap);
        int keyboardTopPadding = res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_top_padding);
        int lastRowVerticalGap = res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_row4_vertical_gap);
        int defaultVerticalGap = (tempDefaultVerticalGap * 3) / 4;
        int keyboardHeight2 = ((keyboardHeight - keyboardTopPadding) - lastRowVerticalGap) + defaultVerticalGap;
        nowConfig.orientation = nowOrientation;
        res.updateConfiguration(nowConfig, res.getDisplayMetrics());
        return keyboardHeight2;
    }

    private String getDeviceOverrideValue(int id) {
        if (this.mWnn == null) {
            return null;
        }
        Resources res = this.mWnn.getResources();
        String[] overrideArray = res.getStringArray(id);
        if (overrideArray == null) {
            return null;
        }
        for (String conditionConstant : overrideArray) {
            int posComma = conditionConstant.indexOf(44);
            if (posComma >= 0) {
                String condition = conditionConstant.substring(0, posComma);
                if (condition.equals(Build.HARDWARE)) {
                    return conditionConstant.substring(posComma + 1);
                }
            }
        }
        return null;
    }

    private boolean isHardKeyboardHidden() {
        IWnnImeBase base = this.mWnn.getCurrentIWnnIme();
        if (base == null) {
            return false;
        }
        return base.isHardKeyboardHidden();
    }

    private boolean isEnableConverter() {
        IWnnImeBase base = this.mWnn.getCurrentIWnnIme();
        if (base == null) {
            return false;
        }
        return base.isEnableConverter();
    }

    public void removeKeyboardFromParent() {
        ViewGroup parent = (ViewGroup) this.mMasterLayout.getParent();
        if (parent != null) {
            parent.removeView(this.mMasterLayout);
        }
    }
}
