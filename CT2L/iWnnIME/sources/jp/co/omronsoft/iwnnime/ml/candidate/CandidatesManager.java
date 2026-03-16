package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.ExtractEditText;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Selection;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiCategoryInfo;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardManager;
import jp.co.omronsoft.iwnnime.ml.KeyboardResourcesDataManager;
import jp.co.omronsoft.iwnnime.ml.KeyboardSkinData;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnEngine;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class CandidatesManager {
    public static final int CATEGORY_ID_HISTORY = 0;
    public static String CATEGORY_NAME_HISTORY = "history";
    public static final int GET_CANDIDATE_CURRENT_HEIGHT = 1;
    public static final int GET_CANDIDATE_OFFSET_HEIGHT = 2;
    public static final int GET_CANDIDATE_ONE_ROW_HEIGHT = 0;
    private static final int ID_BASE_CATEGORY = -100000;
    private static final int ID_BASE_TAB = -10000;
    public static final int LINE_NUM_HARDWARE = 5;
    public static final int LINE_NUM_LANDSCAPE = 1;
    public static final int LINE_NUM_PORTRAIT = 2;
    private static final int MSG_START_CREATE_CANDIDATES_LIST = 0;
    private static final int MSG_SYMBOL_KEY_REPEAT = 1;
    public static final int VIEW_TYPE_FULL = 1;
    public static final int VIEW_TYPE_NORMAL = 0;
    private ColorStateList mCategoryLabelColor;
    private WnnEngine mConverter;
    private int mDisplayHeightMax;
    private int mDisplayWidth;
    private int mDisplayWidthMax;
    private LinearLayout mViewCandidateListTab;
    private ViewGroup mViewTabBase;
    private TextView mViewTabDecoEmoji;
    private TextView mViewTabEmoticon;
    private TextView mViewTabPictgram;
    private HorizontalScrollView mViewTabScroll;
    private TextView mViewTabSymbol;
    private int mViewType;
    private IWnnIME mWnn;
    private KeyboardManager mKeyboardManager = null;
    private ViewGroup mViewBody = null;
    private ArrayList<TextView> mAddSymbolTabList = new ArrayList<>();
    private boolean mEnableCandidateLongClick = true;
    private boolean mAutoHideMode = true;
    private boolean mEnableVibrate = false;
    private boolean mEnablePlaySound = false;
    private boolean mEnableMushroom = false;
    private int mPortraitNumberOfLine = 2;
    private int mLandscapeNumberOfLine = 1;
    private boolean mIsSymbolMode = false;
    private int mSymbolMode = -1;
    protected boolean mSubtypeEmojiInput = false;
    private boolean mHardKeyboardHidden = true;
    private int mCandidateMinimumHeight = 0;
    private float mCandScaleFactor = 0.0f;
    private float mCandScaleFactorHW = 0.0f;
    private Rect mInputCharRect = null;
    private float mSystemFontScale = 1.0f;
    private int mCurrentCategoryForAccessibility = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    CandidatesManager.this.displayCandidatesList((CandidatesList) msg.obj);
                    break;
                case 1:
                    CandidatesManager.this.mWnn.processKeyEventDel();
                    Message repeat = Message.obtain(this, 1);
                    sendMessageDelayed(repeat, 50L);
                    break;
            }
        }
    };
    private View.OnClickListener mTabOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ((v instanceof TextView) && v.isShown() && CandidatesManager.this.mWnn != null) {
                CandidatesManager.this.playSoundAndVibration();
                TextView text = (TextView) v;
                switch (text.getId()) {
                    case R.id.candview_pictgram:
                        if (CandidatesManager.this.mSymbolMode != 3) {
                            CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_SYMBOL_EMOJI));
                        }
                        break;
                    case R.id.candview_symbol:
                        if (CandidatesManager.this.mSymbolMode != 1) {
                            CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_SYMBOL_SYMBOL));
                        }
                        break;
                    case R.id.candview_emoticon:
                        if (CandidatesManager.this.mSymbolMode != 2) {
                            CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_SYMBOL_KAO_MOJI));
                        }
                        break;
                    case R.id.candview_decoemojilist:
                        if (CandidatesManager.this.mSymbolMode != 6) {
                            CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_SYMBOL_DECOEMOJI));
                        }
                        break;
                    default:
                        IWnnSymbolEngine symbolEngine = (IWnnSymbolEngine) CandidatesManager.this.mConverter;
                        int index = CandidatesManager.this.mAddSymbolTabList.indexOf(text);
                        int current = symbolEngine.getAdditionalSymbolIndex();
                        if (index >= 0) {
                            if (index != current || CandidatesManager.this.mSymbolMode != 7) {
                                symbolEngine.setAdditionalSymbolIndex(index);
                                CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_SYMBOL_ADD_SYMBOL));
                            }
                        }
                        break;
                }
            }
        }
    };
    private HashMap<Integer, CandidatesList> mCandidatesListMap = new HashMap<>();
    private CandidatesPagerAdapter mPagerAdapter = null;
    private CandidatesViewPager mViewPager = null;
    private ArrayList<Object> mCategoryList = new ArrayList<>();
    private ArrayList<DecoEmojiCategoryInfo> mDecoEmojiCategoryList = null;
    private TabHost mCategoryBarHost = null;
    private View mCurrentTabWidgetView = null;
    private HorizontalScrollView mCategoryBarScroll = null;
    private TabHost.TabContentFactory mDummyContent = null;
    private boolean mIsDelayUpdateHistory = false;
    private ImageButton mSymbolKeyClose = null;
    private ImageButton mSymbolKeySwitchIme = null;
    private ImageButton mSymbolKeyDel = null;
    private ArrayList<Integer> mArrayWidthCategoryImage = new ArrayList<>();
    private View mCurrentTabView = null;
    private boolean mIsAccessibility = false;
    private int mPageOfViewPager = 1;
    private View.OnTouchListener mCategoryBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    CandidatesManager.this.mCategoryBarScroll.setHorizontalScrollBarEnabled(true);
                    break;
                case 1:
                    CandidatesManager.this.playSoundAndVibration();
                    break;
            }
            return false;
        }
    };
    private TabHost.OnTabChangeListener mCategoryBarChange = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            CandidatesManager.this.changePosition(Integer.valueOf(tabId).intValue());
        }
    };

    private void onPositionChanged() {
        updateCategoryBarStatus();
        if (this.mIsDelayUpdateHistory) {
            this.mIsDelayUpdateHistory = false;
            updateHistorySymbolList();
        }
        if (!this.mHardKeyboardHidden && this.mIsSymbolMode) {
            processMoveKeyEvent(122);
        }
    }

    private int getCategoryId(int position) {
        if (this.mSymbolMode != 6 || position <= 0 || position > this.mDecoEmojiCategoryList.size()) {
            return position;
        }
        int ret = this.mDecoEmojiCategoryList.get(position - 1).getCategoryId();
        return ret;
    }

    private void setScaleFactor(float scaleFactor) {
        if (this.mWnn != null) {
            if (this.mWnn.isHwCandWindow()) {
                scaleFactor = this.mCandScaleFactorHW;
            }
            Resources res = this.mWnn.getResources();
            int candHeight = (int) (res.getDimensionPixelSize(R.dimen.cand_minimum_height) * scaleFactor);
            if (candHeight != this.mCandidateMinimumHeight) {
                this.mCandidateMinimumHeight = candHeight;
                this.mCandScaleFactor = scaleFactor;
                setViewType(this.mViewType);
            }
        }
    }

    private void setScaleFactor() {
        int defaultKeyboardHeight = this.mKeyboardManager.getDefaultKeyboardHeight();
        float scaleFactor = this.mKeyboardManager.getKeyboardSize(false).y / defaultKeyboardHeight;
        setScaleFactor(scaleFactor);
    }

    private CandidatesList createInstanceOfCandidatesList(int position) {
        TextCandidatesList temp;
        CandidatesList target;
        if (this.mIsSymbolMode) {
            SymbolCandidatesList temp2 = new SymbolCandidatesList();
            int category = getCategoryId(getCurrentCategory(position));
            temp2.initView(this.mWnn, (IWnnSymbolEngine) this.mConverter, this.mDisplayWidth, this, this.mSymbolMode, category);
            temp2.updateCandidateList();
            target = temp2;
        } else {
            if (this.mWnn.isHwCandWindow()) {
                temp = new TextCandidatesListHW();
            } else {
                temp = new TextCandidatesList();
            }
            temp.initView(this.mWnn, (iWnnEngine) this.mConverter, this.mDisplayWidth);
            target = temp;
        }
        target.setPreferences(this.mEnableVibrate, this.mEnablePlaySound, this.mEnableMushroom);
        target.setNumberOfDisplayLines(this.mPortraitNumberOfLine, this.mLandscapeNumberOfLine);
        target.setEnableCandidateLongClick(this.mEnableCandidateLongClick);
        target.setViewType(this.mViewType, getCandidatesAreaHeight(1, true));
        target.setScaleFactor(this.mCandScaleFactor, true);
        if (!this.mIsSymbolMode) {
            target.setDummyCandidateView(getNumberOfLine());
        }
        this.mCandidatesListMap.put(Integer.valueOf(position), target);
        return target;
    }

    public class CandidatesPagerAdapter extends PagerAdapter {
        public CandidatesPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            CandidatesList target = (CandidatesList) CandidatesManager.this.mCandidatesListMap.get(Integer.valueOf(position));
            if (target == null) {
                target = CandidatesManager.this.createInstanceOfCandidatesList(position);
                Message msg = CandidatesManager.this.mHandler.obtainMessage(0, target);
                CandidatesManager.this.mHandler.sendMessage(msg);
            }
            View addView = target.getCandidatesListView();
            container.addView(addView);
            return addView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return CandidatesManager.this.mPageOfViewPager;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            boolean ret = view.equals(object);
            return ret;
        }

        @Override
        public int getItemPosition(Object object) {
            return -2;
        }
    }

    public CandidatesManager(IWnnIME wnn) {
        this.mWnn = wnn;
    }

    public void setAutoHide(boolean hide) {
        this.mAutoHideMode = hide;
    }

    public View initView(int width) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "CandidatesManager::initView() width=" + width);
        }
        if (this.mWnn == null) {
            return null;
        }
        this.mKeyboardManager = this.mWnn.getCurrentKeyboardManager();
        EmojiAssist assist = EmojiAssist.getInstance();
        assist.clearView();
        ExtractEditText extractEditText = this.mWnn.getExtractEditText();
        if (extractEditText != null) {
            CharSequence text = extractEditText.getText();
            int selStart = Selection.getSelectionStart(text);
            int selEnd = Selection.getSelectionEnd(text);
            if (selStart >= 0 && selEnd >= 0) {
                Selection.setSelection(extractEditText.getText(), selStart, selEnd);
            }
        }
        Resources res = this.mWnn.getResources();
        this.mCandScaleFactorHW = res.getFraction(R.fraction.cand_view_scale_factor_hw, 1, 1);
        this.mDisplayWidth = width;
        if (this.mViewBody != null) {
            removeAllViewRecursive(this.mViewBody);
        }
        LayoutInflater inflater = this.mWnn.getLayoutInflater();
        this.mViewBody = (ViewGroup) inflater.inflate(R.layout.candidates, (ViewGroup) null);
        cancelCreateCandidates();
        this.mCandidatesListMap.clear();
        int holdPosition = 0;
        boolean preAccessibility = this.mIsAccessibility;
        this.mIsAccessibility = WnnAccessibility.isAccessibility(this.mWnn);
        if (this.mViewPager != null) {
            holdPosition = this.mViewPager.getCurrentItem();
            if (this.mIsSymbolMode) {
                if (!preAccessibility && this.mIsAccessibility) {
                    this.mPageOfViewPager = 1;
                    this.mCurrentCategoryForAccessibility = holdPosition;
                } else if (preAccessibility && !this.mIsAccessibility) {
                    this.mPageOfViewPager = this.mCategoryList.size();
                    holdPosition = this.mCurrentCategoryForAccessibility;
                }
            }
        }
        this.mPagerAdapter = new CandidatesPagerAdapter();
        this.mViewPager = (CandidatesViewPager) this.mViewBody.findViewById(R.id.candidate_viewpager);
        this.mViewPager.setDisplayWidth(this.mDisplayWidth);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffset == 0.0f && positionOffsetPixels == 0) {
                    CandidatesManager.this.onPositionChanged();
                }
                CandidatesManager.this.mViewPager.onPageScrolled(position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 2) {
                    CandidatesManager.this.onPositionChanged();
                }
            }
        });
        if (holdPosition != 0) {
            this.mViewPager.setCurrentItem(holdPosition, false);
        }
        this.mCategoryBarHost = (TabHost) this.mViewBody.findViewById(R.id.candview_category_tabhost);
        this.mCategoryBarHost.setup();
        this.mCategoryBarScroll = (HorizontalScrollView) this.mViewBody.findViewById(R.id.candview_category_scroll_view);
        this.mCategoryBarScroll.setHorizontalScrollBarEnabled(false);
        this.mDummyContent = new SymbolCandidatesCategoryListTabContentView(this.mWnn);
        KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
        FrameLayout masterLayout = (FrameLayout) this.mViewBody.findViewById(R.id.candidate_master_layout);
        Drawable skin = resMan.getDrawable("CandidateBlankBackground");
        if (skin != null) {
            masterLayout.setBackgroundDrawable(skin);
            this.mCategoryBarScroll.setBackgroundDrawable(skin);
        }
        ColorStateList skinColorState = resMan.getCategoryColorStateList();
        Resources r = this.mWnn.getResources();
        if (skinColorState != null) {
            this.mCategoryLabelColor = skinColorState;
        } else {
            this.mCategoryLabelColor = r.getColorStateList(R.color.symbol_category_label_color);
        }
        if (this.mIsSymbolMode) {
            createSymbolCategoryBar();
            this.mCategoryBarHost.setCurrentTabByTag(String.valueOf(getCurrentCategory(this.mViewPager.getCurrentItem())));
        }
        this.mSymbolKeyClose = (ImageButton) this.mViewBody.findViewById(R.id.candview_close);
        this.mSymbolKeyClose.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mSymbolKeyClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CandidatesManager.this.mWnn != null) {
                    CandidatesManager.this.mHandler.removeMessages(1);
                    CandidatesManager.this.playSoundAndVibration();
                    CandidatesManager.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_KEY, new KeyEvent(0, 4)));
                }
            }
        });
        this.mSymbolKeySwitchIme = (ImageButton) this.mViewBody.findViewById(R.id.candview_switch_ime);
        this.mSymbolKeySwitchIme.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mSymbolKeySwitchIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CandidatesManager.this.mWnn != null) {
                    CandidatesManager.this.playSoundAndVibration();
                    boolean success = CandidatesManager.this.mWnn.switchToNextInputMethod();
                    if (!success) {
                        WnnUtility.showInputMethodPicker(CandidatesManager.this.mWnn);
                    }
                }
            }
        });
        this.mSymbolKeySwitchIme.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!WnnAccessibility.isAccessibility(CandidatesManager.this.mWnn)) {
                    CandidatesManager.this.playSoundAndVibration();
                    WnnUtility.showInputMethodPicker(CandidatesManager.this.mWnn);
                }
                return true;
            }
        });
        this.mSymbolKeyDel = (ImageButton) this.mViewBody.findViewById(R.id.candview_delete);
        this.mSymbolKeyDel.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mSymbolKeyDel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (CandidatesManager.this.mWnn != null) {
                    int action = event.getActionMasked();
                    boolean isAccessibility = WnnAccessibility.isAccessibility(CandidatesManager.this.mWnn);
                    switch (action) {
                        case 0:
                        case 5:
                            if (!isAccessibility) {
                                Message msg = CandidatesManager.this.mHandler.obtainMessage(1);
                                CandidatesManager.this.mHandler.sendMessageDelayed(msg, 400L);
                                CandidatesManager.this.playSoundAndVibration();
                                CandidatesManager.this.mWnn.processKeyEventDel();
                            }
                            break;
                        case 1:
                        case 3:
                        case 6:
                            if (isAccessibility) {
                                CandidatesManager.this.mWnn.processKeyEventDel();
                            } else {
                                CandidatesManager.this.mHandler.removeMessages(1);
                            }
                            break;
                        case 2:
                            if (!isAccessibility && (event.getX() < 0.0f || event.getY() < 0.0f || event.getX() > v.getWidth() || event.getY() > v.getHeight())) {
                            }
                            break;
                    }
                }
                return false;
            }
        });
        this.mViewTabScroll = (HorizontalScrollView) this.mViewBody.findViewById(R.id.tab_scroll);
        this.mViewTabBase = (ViewGroup) this.mViewBody.findViewById(R.id.tab_base);
        this.mViewTabPictgram = (TextView) this.mViewBody.findViewById(R.id.candview_pictgram);
        this.mViewTabSymbol = (TextView) this.mViewBody.findViewById(R.id.candview_symbol);
        this.mViewTabEmoticon = (TextView) this.mViewBody.findViewById(R.id.candview_emoticon);
        this.mViewTabDecoEmoji = (TextView) this.mViewBody.findViewById(R.id.candview_decoemojilist);
        this.mViewTabPictgram.setOnClickListener(this.mTabOnClick);
        this.mViewTabSymbol.setOnClickListener(this.mTabOnClick);
        this.mViewTabEmoticon.setOnClickListener(this.mTabOnClick);
        this.mViewTabDecoEmoji.setOnClickListener(this.mTabOnClick);
        this.mViewTabPictgram.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mViewTabSymbol.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mViewTabEmoticon.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mViewTabDecoEmoji.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
        this.mViewCandidateListTab = (LinearLayout) this.mViewBody.findViewById(R.id.candview_tab);
        Drawable skinData = resMan.getKeyboardBg();
        if (skinData != null) {
            this.mViewCandidateListTab.setBackgroundDrawable(skinData);
        }
        Drawable skinData2 = resMan.getKeyboardBg1Line();
        if (skinData2 != null) {
            this.mViewCandidateListTab.setBackgroundDrawable(skinData2);
        }
        setScaleFactor();
        setSymbolViewType();
        return this.mViewBody;
    }

    public View getCurrentView() {
        return this.mViewBody;
    }

    public void setViewType(int type) {
        if (IWnnIME.isDebugging()) {
            Log.d("IWnnIME", "CandidatesManager:setViewType(" + type + ")");
        }
        switch (type) {
            case 0:
                if (this.mViewType == 1) {
                    clearFocusCandidate();
                }
                break;
            case 1:
                break;
            default:
                return;
        }
        this.mViewType = type;
        int height = getCandidatesAreaHeight(1, true);
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(-1, height);
        this.mViewPager.setLayoutParams(params);
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            entry.setViewType(type, height);
        }
    }

    private void setSymbolViewType() {
        Resources res;
        Configuration config;
        if (this.mWnn != null && this.mViewCandidateListTab != null && this.mCategoryBarHost != null && this.mWnn != null && (res = this.mWnn.getResources()) != null && (config = res.getConfiguration()) != null) {
            this.mSystemFontScale = config.fontScale;
            this.mSubtypeEmojiInput = this.mWnn.isSubtypeEmojiInput();
            if (this.mIsSymbolMode) {
                ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(-1, Keyboard.getOneRowKeyboardHeight());
                this.mViewCandidateListTab.setVisibility(0);
                this.mViewCandidateListTab.setLayoutParams(params);
                this.mCategoryBarHost.setVisibility(0);
                this.mCategoryBarHost.setLayoutParams(params);
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_FULL));
                createSymbolCategoryBar();
                createAdditionalTab();
                updateSymbolType();
                updateKey();
                return;
            }
            this.mViewCandidateListTab.setVisibility(8);
            this.mCategoryBarHost.setVisibility(8);
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_NORMAL));
        }
    }

    public int getViewType() {
        return this.mViewType;
    }

    public void displayCandidates() {
        if (!this.mIsSymbolMode) {
            displayCandidatesList(this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem())));
        }
        if (!this.mViewBody.isShown()) {
            this.mKeyboardManager.setCandidatesViewShown(true);
        }
    }

    private void displayCandidatesList(CandidatesList target) {
        if (target != null) {
            int line = getNumberOfLine();
            target.initCandidatesList(line, false);
            target.createCandidatesListView(line);
            onPositionChanged();
            this.mKeyboardManager.updateCandidateAndMenuLayout();
            if (!hasCandidates()) {
                if (this.mAutoHideMode) {
                    this.mKeyboardManager.setCandidatesViewShown(false);
                } else {
                    target.setDummyCandidateView(line);
                }
            }
            Resources res = this.mWnn.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Rect rect = new Rect(0, 0, 0, 0);
            this.mWnn.getWindow().getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            this.mDisplayWidthMax = dm.widthPixels;
            this.mDisplayHeightMax = dm.heightPixels - rect.top;
            updateCandidateListLayout(true);
        }
    }

    public int getNumberOfLine() {
        if (this.mWnn == null) {
            return 1;
        }
        Resources res = this.mWnn.getResources();
        boolean isPortrait = res.getConfiguration().orientation != 2;
        return isPortrait ? this.mPortraitNumberOfLine : this.mLandscapeNumberOfLine;
    }

    public void clearCandidates() {
        if (this.mWnn != null && this.mViewPager != null) {
            clearFocusCandidate();
            cancelCreateCandidates();
            closeDialogCheck();
            int line = getNumberOfLine();
            for (CandidatesList entry : this.mCandidatesListMap.values()) {
                entry.initCandidatesList(line, true);
            }
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CANCEL_WEBAPI));
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_NORMAL));
            if (this.mAutoHideMode) {
                setViewType(0);
            }
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INIT_CONVERTER));
            this.mKeyboardManager.updateCandidateAndMenuLayout();
            if (this.mAutoHideMode && this.mViewBody.isShown()) {
                this.mKeyboardManager.setCandidatesViewShown(false);
                this.mKeyboardManager.setCandidatesViewShown(true);
                this.mKeyboardManager.setCandidatesViewShown(false);
            }
        }
    }

    public void addCandidatesWebAPI() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.addCandidatesWebAPI();
            updateCandidateListLayout(false);
        }
    }

    public void setPreferences(SharedPreferences pref) {
        if (this.mWnn != null) {
            Resources res = this.mWnn.getResources();
            this.mEnableVibrate = pref.getBoolean(ControlPanelPrefFragment.VIBRATION_KEY, res.getBoolean(R.bool.key_vibration_default_value));
            this.mEnablePlaySound = pref.getBoolean(ControlPanelPrefFragment.KEY_SOUND_KEY, res.getBoolean(R.bool.key_sound_default_value));
            this.mEnableMushroom = !pref.getString(ControlPanelPrefFragment.MUSHROOM_KEY, res.getString(R.string.mushroom_id_default)).equals("notuse");
            for (CandidatesList entry : this.mCandidatesListMap.values()) {
                entry.setPreferences(this.mEnableVibrate, this.mEnablePlaySound, this.mEnableMushroom);
            }
        }
    }

    public void setMode(boolean enable, int mode, WnnEngine convert) {
        if (this.mIsSymbolMode && !enable && (this.mConverter instanceof IWnnSymbolEngine)) {
            ((IWnnSymbolEngine) this.mConverter).setLastSymbollist(this.mSymbolMode);
        }
        boolean isKeepCategory = false;
        if (this.mSymbolMode == mode && this.mIsSymbolMode == enable) {
            isKeepCategory = true;
        }
        this.mSymbolMode = mode;
        this.mIsSymbolMode = enable;
        this.mConverter = convert;
        this.mDecoEmojiCategoryList = null;
        this.mIsDelayUpdateHistory = false;
        this.mCategoryList = this.mConverter.getCategoryList(this.mSymbolMode);
        this.mPageOfViewPager = this.mCategoryList.size();
        this.mIsAccessibility = WnnAccessibility.isAccessibility(this.mWnn);
        if (this.mIsAccessibility) {
            this.mPageOfViewPager = 1;
        }
        if (this.mConverter instanceof IWnnSymbolEngine) {
            this.mDecoEmojiCategoryList = ((IWnnSymbolEngine) this.mConverter).getDecoEmojiCategoryInfoList();
        }
        setSymbolViewType();
        if (this.mPagerAdapter != null) {
            cancelCreateCandidates();
            this.mCandidatesListMap.clear();
            int position = 0;
            if (isKeepCategory) {
                position = this.mViewPager.getCurrentItem();
            } else {
                this.mCurrentCategoryForAccessibility = 0;
                if (this.mIsSymbolMode && !this.mConverter.hasHistory()) {
                    if (this.mIsAccessibility) {
                        this.mCurrentCategoryForAccessibility = 1;
                    } else {
                        position = 1;
                    }
                }
            }
            this.mPagerAdapter.notifyDataSetChanged();
            this.mViewPager.setCurrentItem(position, false);
            if (this.mIsSymbolMode && this.mIsAccessibility && !isKeepCategory) {
                updateCategoryBarStatus();
                announceDescriptionOfSymbolList();
            }
        }
    }

    private void createSymbolCategoryBar() {
        View view;
        if (this.mWnn != null) {
            this.mCategoryBarHost.clearAllTabs();
            this.mCategoryBarHost.setOnTabChangedListener(null);
            if (hasCategories()) {
                LayoutInflater inflater = this.mWnn.getLayoutInflater();
                Resources res = this.mWnn.getResources();
                this.mArrayWidthCategoryImage.clear();
                int index = 0;
                int[] description = WnnAccessibility.getCategoryDescriptionList(this.mSymbolMode);
                for (Object data : this.mCategoryList) {
                    Drawable icon = null;
                    String text = null;
                    if (data instanceof Integer) {
                        int resId = ((Integer) data).intValue();
                        try {
                            icon = res.getDrawable(resId);
                        } catch (Resources.NotFoundException e) {
                            text = res.getString(resId);
                        }
                    } else if (data instanceof Drawable) {
                        icon = (Drawable) data;
                    } else if (data instanceof String) {
                        text = (String) data;
                    }
                    TabHost.TabSpec spec = this.mCategoryBarHost.newTabSpec(String.valueOf(index));
                    spec.setContent(this.mDummyContent);
                    KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                    Drawable skinData = resMan.getStateListDrawable(R.drawable.category_background);
                    int imageWidth = 0;
                    if (icon != null) {
                        Drawable icon2 = getStateListDrawableWithColorFilter(icon, this.mCategoryLabelColor);
                        view = inflater.inflate(R.layout.symbol_category_tab_icon, (ViewGroup) null);
                        ImageView imageView = (ImageView) view;
                        imageView.setImageDrawable(icon2);
                        if (icon2 instanceof StateListDrawable) {
                            icon2 = ((StateListDrawable) icon2).getCurrent();
                        }
                        if (icon2 instanceof BitmapDrawable) {
                            Bitmap iconBitmap = ((BitmapDrawable) icon2).getBitmap();
                            imageWidth = iconBitmap.getWidth();
                            this.mArrayWidthCategoryImage.add(Integer.valueOf(imageWidth));
                            if (description != null && index < description.length) {
                                view.setContentDescription(this.mWnn.getString(description[index]));
                            }
                        }
                    } else {
                        view = inflater.inflate(R.layout.symbol_category_tab_label, (ViewGroup) null);
                        TextView textView = (TextView) view;
                        textView.setText(text);
                        textView.setTextColor(this.mCategoryLabelColor);
                        textView.setContentDescription(text);
                        this.mArrayWidthCategoryImage.add(0);
                    }
                    if (skinData != null) {
                        view.setBackgroundDrawable(skinData);
                    } else {
                        view.setBackgroundResource(R.drawable.category_background);
                    }
                    view.setOnTouchListener(this.mCategoryBarTouchListener);
                    view.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
                    view.setId(ID_BASE_CATEGORY - index);
                    setupCategoryView(view, imageWidth);
                    spec.setIndicator(view);
                    this.mCategoryBarHost.addTab(spec);
                    index++;
                }
                this.mCategoryBarHost.setOnTabChangedListener(this.mCategoryBarChange);
            }
        }
    }

    private Drawable getStateListDrawableWithColorFilter(Drawable drawable, final ColorStateList color) {
        StateListDrawable stateDrawable = new StateListDrawable() {
            @Override
            protected boolean onStateChange(int[] states) {
                setColorFilter(color.getColorForState(states, color.getDefaultColor()), PorterDuff.Mode.SRC_IN);
                return super.onStateChange(states);
            }
        };
        stateDrawable.addState(new int[0], drawable);
        return stateDrawable;
    }

    private int getTextSize(int size) {
        float textScaleRate = getScaleRate();
        return (int) (size * textScaleRate);
    }

    private float getScaleRate() {
        PointF tmpRate = this.mKeyboardManager.getKeyboardScaleRate();
        float rate = Math.min(tmpRate.x, tmpRate.y);
        return rate;
    }

    private void updateCategoryBarStatus() {
        if (this.mCategoryBarHost != null && this.mCategoryBarHost.getVisibility() == 0) {
            if (this.mCurrentTabWidgetView instanceof TextView) {
                ((TextView) this.mCurrentTabWidgetView).setEllipsize(TextUtils.TruncateAt.END);
            }
            this.mCategoryBarHost.setCurrentTabByTag(String.valueOf(getCurrentCategory(this.mViewPager.getCurrentItem())));
            this.mCurrentTabWidgetView = this.mCategoryBarHost.getCurrentTabView();
            if (this.mCurrentTabWidgetView != null) {
                if (this.mCurrentTabWidgetView instanceof TextView) {
                    ((TextView) this.mCurrentTabWidgetView).setEllipsize(TextUtils.TruncateAt.MARQUEE);
                }
                int displayWidth = this.mCategoryBarScroll.getWidth();
                int targetLeftPos = this.mCurrentTabWidgetView.getLeft();
                int targetWidth = this.mCurrentTabWidgetView.getWidth();
                int centerDispLeftPos = (displayWidth - targetWidth) / 2;
                this.mCategoryBarScroll.setHorizontalScrollBarEnabled(false);
                this.mCategoryBarScroll.scrollTo(targetLeftPos - centerDispLeftPos, 0);
            }
        }
    }

    public void changePosition(boolean next) {
        int position;
        int position2 = getCurrentCategory(this.mViewPager.getCurrentItem());
        if (next) {
            position = position2 + 1;
        } else {
            position = position2 - 1;
        }
        changePosition(position);
    }

    private void changePosition(int position) {
        if (position >= 0 && this.mCategoryList != null && position < this.mCategoryList.size()) {
            if (WnnAccessibility.isAccessibility(this.mWnn)) {
                if (position != this.mCurrentCategoryForAccessibility) {
                    this.mCurrentCategoryForAccessibility = position;
                    updateCategoryBarStatus();
                    cancelCreateCandidates();
                    this.mCandidatesListMap.clear();
                    this.mPagerAdapter.notifyDataSetChanged();
                    this.mViewPager.setCurrentItem(0, false);
                    announceDescriptionOfSymbolList();
                    return;
                }
                return;
            }
            this.mViewPager.setCurrentItem(position, false);
        }
    }

    public void updateHistorySymbolList() {
        if (!WnnAccessibility.isAccessibility(this.mWnn)) {
            CandidatesList target = this.mCandidatesListMap.get(0);
            if (target instanceof SymbolCandidatesList) {
                if (this.mViewPager.getCurrentItem() == 0) {
                    this.mIsDelayUpdateHistory = true;
                    return;
                }
                SymbolCandidatesList symbolList = (SymbolCandidatesList) target;
                symbolList.updateCandidateList();
                int line = getNumberOfLine();
                symbolList.initCandidatesList(line, false);
                symbolList.createCandidatesListView(line);
            }
        }
    }

    private void playSoundAndVibration() {
        if (this.mWnn != null) {
            Context context = this.mWnn.getApplicationContext();
            if (!WnnAccessibility.isAccessibility(context)) {
                if (this.mEnableVibrate) {
                    WnnUtility.vibrate(context);
                }
                if (this.mEnablePlaySound) {
                    WnnUtility.playSoundEffect(context);
                }
            }
        }
    }

    private void updateSymbolType() {
        switch (this.mSymbolMode) {
            case 1:
                updateTabStatus(this.mViewTabPictgram, ((IWnnSymbolEngine) this.mConverter).isEnableEmoji(), false);
                updateTabStatus(this.mViewTabSymbol, true, true);
                updateTabStatus(this.mViewTabEmoticon, ((IWnnSymbolEngine) this.mConverter).isEnableEmoticon(), false);
                updateTabStatus(this.mViewTabDecoEmoji, ((IWnnSymbolEngine) this.mConverter).isEnableDecoEmoji(), false);
                break;
            case 2:
                updateTabStatus(this.mViewTabPictgram, ((IWnnSymbolEngine) this.mConverter).isEnableEmoji(), false);
                updateTabStatus(this.mViewTabSymbol, true, false);
                updateTabStatus(this.mViewTabEmoticon, ((IWnnSymbolEngine) this.mConverter).isEnableEmoticon(), true);
                updateTabStatus(this.mViewTabDecoEmoji, ((IWnnSymbolEngine) this.mConverter).isEnableDecoEmoji(), false);
                break;
            case 3:
            case 4:
            case 5:
            default:
                updateTabStatus(this.mViewTabPictgram, ((IWnnSymbolEngine) this.mConverter).isEnableEmoji(), true);
                if (this.mSubtypeEmojiInput) {
                    updateTabStatus(this.mViewTabSymbol, false, false);
                    updateTabStatus(this.mViewTabEmoticon, false, false);
                    updateTabStatus(this.mViewTabDecoEmoji, false, false);
                } else {
                    updateTabStatus(this.mViewTabSymbol, true, false);
                    updateTabStatus(this.mViewTabEmoticon, ((IWnnSymbolEngine) this.mConverter).isEnableEmoticon(), false);
                    updateTabStatus(this.mViewTabDecoEmoji, ((IWnnSymbolEngine) this.mConverter).isEnableDecoEmoji(), false);
                }
                break;
            case 6:
                updateTabStatus(this.mViewTabPictgram, ((IWnnSymbolEngine) this.mConverter).isEnableEmoji(), false);
                updateTabStatus(this.mViewTabSymbol, true, false);
                updateTabStatus(this.mViewTabEmoticon, ((IWnnSymbolEngine) this.mConverter).isEnableEmoticon(), false);
                updateTabStatus(this.mViewTabDecoEmoji, ((IWnnSymbolEngine) this.mConverter).isEnableDecoEmoji(), true);
                break;
            case 7:
                updateTabStatus(this.mViewTabPictgram, ((IWnnSymbolEngine) this.mConverter).isEnableEmoji(), false);
                updateTabStatus(this.mViewTabSymbol, true, false);
                updateTabStatus(this.mViewTabEmoticon, ((IWnnSymbolEngine) this.mConverter).isEnableEmoticon(), false);
                updateTabStatus(this.mViewTabDecoEmoji, ((IWnnSymbolEngine) this.mConverter).isEnableDecoEmoji(), false);
                break;
        }
        updateAdditionalSymbolTabStatus();
    }

    private void updateTabStatus(TextView tab, boolean visibled, boolean selected) {
        int backgroundId;
        int colorId;
        Drawable background;
        if (this.mWnn != null) {
            if (visibled) {
                tab.setVisibility(0);
                KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                if (selected) {
                    backgroundId = R.drawable.cand_tab;
                    colorId = R.color.tab_textcolor_select;
                    background = resMan.getTab();
                    this.mViewTabScroll.requestChildFocus(this.mViewTabBase, tab);
                    this.mCurrentTabView = tab;
                } else {
                    backgroundId = R.drawable.cand_tab_noselect;
                    colorId = R.color.tab_textcolor_no_select;
                    background = resMan.getTabNoSelect();
                }
                if (background != null) {
                    tab.setBackgroundDrawable(background);
                } else {
                    tab.setBackgroundResource(backgroundId);
                }
                tab.setTextColor(resMan.getColor(this.mWnn, colorId));
            } else {
                tab.setVisibility(8);
            }
            Resources res = this.mWnn.getResources();
            int textSize = getTextSize(res.getDimensionPixelSize(R.dimen.tab_text_size));
            int xPadding = (int) res.getFraction(R.fraction.cand_view_category_x_padding, textSize, textSize);
            tab.setPadding(xPadding, 0, xPadding, 0);
            tab.setTextSize(0, textSize * this.mSystemFontScale);
            tab.setHeight(Keyboard.getOneRowKeyboardHeight());
        }
    }

    private void createAdditionalTab() {
        String[] tabNames;
        for (TextView tab : this.mAddSymbolTabList) {
            tab.setOnClickListener(null);
            this.mViewTabBase.removeView(tab);
        }
        this.mAddSymbolTabList.clear();
        if (!this.mSubtypeEmojiInput && (tabNames = ((IWnnSymbolEngine) this.mConverter).getAdditionalSymbolTabNames()) != null) {
            for (int index = 0; index < tabNames.length; index++) {
                String tabName = tabNames[index];
                TextView tab2 = createTab(tabName, (-10000) - index);
                this.mViewTabBase.addView(tab2, this.mViewTabSymbol.getLayoutParams());
                this.mAddSymbolTabList.add(tab2);
            }
        }
    }

    private TextView createTab(String name, int id) {
        TextView tab = new TextView(this.mViewBody.getContext());
        if (this.mWnn != null) {
            tab.setTextSize(0, this.mWnn.getResources().getDimensionPixelSize(R.dimen.tab_text_size) * this.mSystemFontScale);
            tab.setText(name);
            tab.setContentDescription(name);
            tab.setTextColor(-1);
            tab.setBackgroundResource(R.drawable.cand_tab);
            tab.setGravity(17);
            tab.setSingleLine();
            tab.setLayoutParams(new LinearLayout.LayoutParams(-1, -2, 1.0f));
            tab.setSoundEffectsEnabled(false);
            tab.setEllipsize(TextUtils.TruncateAt.END);
            tab.setOnClickListener(this.mTabOnClick);
            tab.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
            tab.setImportantForAccessibility(2);
            tab.setId(id);
        }
        return tab;
    }

    private void updateAdditionalSymbolTabStatus() {
        int current = ((IWnnSymbolEngine) this.mConverter).getAdditionalSymbolIndex();
        int i = 0;
        while (i < this.mAddSymbolTabList.size()) {
            boolean selected = this.mSymbolMode == 7 && i == current;
            updateTabStatus(this.mAddSymbolTabList.get(i), true, selected);
            i++;
        }
    }

    private void updateKey() {
        if (this.mWnn != null) {
            Resources res = this.mWnn.getResources();
            float rate = getScaleRate();
            int height = Keyboard.getOneRowKeyboardHeight();
            ViewGroup.LayoutParams params = this.mSymbolKeyClose.getLayoutParams();
            params.height = height;
            int keyWidthWeight = res.getInteger(R.integer.cand_view_key_weight);
            int tabWidthWeight = res.getInteger(R.integer.cand_view_tab_weight);
            int keyWidth = (this.mDisplayWidth / (keyWidthWeight + tabWidthWeight)) * keyWidthWeight;
            int keyXPadding = (int) (res.getFraction(R.fraction.cand_view_key_x_padding, keyWidth, keyWidth) / rate);
            int keyYPadding = (int) res.getFraction(R.fraction.cand_view_key_y_padding, height, height);
            int iconWidth = keyWidth - (keyXPadding * 2);
            int iconHeight = height - (keyYPadding * 2);
            setButtonParams(params, iconWidth, iconHeight, this.mSymbolKeyDel, R.drawable.key_del_symbol, DefaultSoftKeyboard.KEYCODE_4KEY_CLEAR, keyWidth);
            if (this.mSubtypeEmojiInput) {
                setButtonParams(params, iconWidth, iconHeight, this.mSymbolKeySwitchIme, R.drawable.sym_keyboard_language_switch, DefaultSoftKeyboard.KEYCODE_LANGUAGE_SWITCH, keyWidth);
                this.mSymbolKeyClose.setVisibility(8);
                this.mSymbolKeySwitchIme.setVisibility(0);
            } else {
                setButtonParams(params, iconWidth, iconHeight, this.mSymbolKeyClose, R.drawable.ic_ime_switcher_dark, DefaultSoftKeyboard.KEYCODE_4KEY_KEYBOAD, keyWidth);
                this.mSymbolKeyClose.setVisibility(0);
                this.mSymbolKeySwitchIme.setVisibility(8);
            }
        }
    }

    private void setButtonParams(ViewGroup.LayoutParams params, int iconWidth, int iconHeight, ImageButton button, int id, int keyCode, int keyWidth) {
        Resources res;
        Drawable icon;
        if (this.mWnn != null && (res = this.mWnn.getResources()) != null) {
            StringBuffer key = new StringBuffer(KeyboardSkinData.STRING_KEY);
            key.append(String.valueOf(keyCode));
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            Drawable skinData = resMan.getDrawable(keyCode, (Keyboard) null);
            if (skinData != null) {
                icon = resizeDrawable(skinData, this.mSystemFontScale);
            } else {
                Drawable icon2 = res.getDrawable(id);
                if (icon2 != null) {
                    int[] color = resMan.getTextColor(this.mWnn, R.color.key_text_color_2nd);
                    LayerDrawable layerDraw = (LayerDrawable) icon2;
                    if (color.length - 1 >= layerDraw.getNumberOfLayers()) {
                        int layerNum = layerDraw.getNumberOfLayers();
                        Drawable[] resizeDraw = new Drawable[layerNum];
                        for (int layerIndex = 0; layerIndex < layerNum; layerIndex++) {
                            Drawable draw = layerDraw.getDrawable(layerIndex);
                            resizeDraw[layerIndex] = resizeDrawable(draw, this.mSystemFontScale);
                            if (resMan.isEnableColorFilter(keyCode, false, true, layerIndex + 1)) {
                                resizeDraw[layerIndex].setColorFilter(color[layerIndex + 1], PorterDuff.Mode.SRC_IN);
                            } else {
                                resizeDraw[layerIndex].clearColorFilter();
                            }
                        }
                        icon = new LayerDrawable(resizeDraw);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
            button.setLayoutParams(params);
            int keyXPadding = (keyWidth - ((int) (iconWidth * this.mSystemFontScale))) / 2;
            int keyYPadding = (params.height - ((int) (iconHeight * this.mSystemFontScale))) / 2;
            button.setPadding(keyXPadding, keyYPadding, keyXPadding, keyYPadding);
            button.setImageDrawable(icon);
            Drawable skinData2 = resMan.getKeyBg(this.mViewBody.getContext(), null, keyCode, true);
            if (skinData2 == null) {
                skinData2 = resMan.getKeyBg2nd();
            }
            if (skinData2 != null) {
                button.setBackgroundDrawable(skinData2);
            }
        }
    }

    private Drawable resizeDrawable(Drawable drawable, float scaleFactor) {
        Bitmap defaultBitmap = ((BitmapDrawable) drawable).getBitmap();
        int width = (int) (defaultBitmap.getWidth() * scaleFactor);
        int height = (int) (defaultBitmap.getHeight() * scaleFactor);
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(defaultBitmap, width, height, false);
        return new BitmapDrawable(this.mWnn.getResources(), scaleBitmap);
    }

    public void processMoveKeyEvent(int key) {
        CandidatesList target;
        if (this.mViewBody.isShown() && (target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()))) != null) {
            target.processMoveKeyEvent(key);
            updateCandidateListLayout(false);
        }
    }

    public boolean isFocusCandidate() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target == null) {
            return false;
        }
        boolean ret = target.isFocusCandidate();
        return ret;
    }

    public void clearFocusCandidate() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.clearFocusCandidate();
        }
    }

    public WnnWord getFocusedWnnWord() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target == null) {
            return null;
        }
        WnnWord ret = target.getFocusedWnnWord();
        return ret;
    }

    public void selectFocusCandidate() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.selectFocusCandidate();
        }
    }

    public void scrollPageAndUpdateFocus(boolean scrollDown) {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.scrollPageAndUpdateFocus(scrollDown);
            updateCandidateListLayout(false);
        }
    }

    public void setNumberOfDisplayLines(boolean isConverte) {
        if (this.mWnn != null) {
            Resources res = this.mWnn.getResources();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            if (isConverte) {
                this.mPortraitNumberOfLine = Integer.parseInt(pref.getString(ControlPanelPrefFragment.CANDIDATE_LINES_PORTRAIT_KEY, res.getString(R.string.setting_portrait_default_value)));
                this.mLandscapeNumberOfLine = Integer.parseInt(pref.getString(ControlPanelPrefFragment.CANDIDATE_LINES_LANDSCAPE_KEY, res.getString(R.string.setting_landscape_default_value)));
            } else {
                this.mPortraitNumberOfLine = 1;
                this.mLandscapeNumberOfLine = 1;
            }
            int line = getNumberOfLine();
            for (CandidatesList entry : this.mCandidatesListMap.values()) {
                entry.setNumberOfDisplayLines(this.mPortraitNumberOfLine, this.mLandscapeNumberOfLine);
                if (!this.mAutoHideMode && !hasCandidates()) {
                    entry.setDummyCandidateView(line);
                }
            }
            setViewType(this.mViewType);
        }
    }

    public boolean isSymbolMode() {
        return this.mIsSymbolMode;
    }

    public void onWebApiError(String errorCode) {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.onWebApiError();
            if (errorCode != null) {
                Toast.makeText(this.mWnn, errorCode, 0).show();
            }
        }
    }

    public void setHardKeyboardHidden(boolean hardKeyboardHidden) {
        this.mHardKeyboardHidden = hardKeyboardHidden;
    }

    public boolean getCanReadMore() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target == null) {
            return false;
        }
        boolean ret = target.getCanReadMore();
        return ret;
    }

    public boolean isReadMoreButtonPressed() {
        CandidatesList target;
        if (this.mViewPager == null || (target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()))) == null) {
            return false;
        }
        boolean ret = target.isReadMoreButtonPressed();
        return ret;
    }

    public void setEnableCandidateLongClick(boolean enable) {
        this.mEnableCandidateLongClick = enable;
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            entry.setEnableCandidateLongClick(enable);
        }
    }

    public boolean checkDecoEmoji() {
        boolean ret = false;
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            ret = entry.checkDecoEmoji();
        }
        return ret;
    }

    private void removeAllViewRecursive(ViewGroup viewGroup) {
        int conut = viewGroup.getChildCount();
        for (int i = 0; i < conut; i++) {
            View view = viewGroup.getChildAt(i);
            unbindView(view);
            if (view instanceof ViewGroup) {
                removeAllViewRecursive((ViewGroup) view);
            }
        }
        viewGroup.removeAllViews();
    }

    private void unbindView(View view) {
        view.setOnLongClickListener(null);
        view.setOnClickListener(null);
        view.setOnTouchListener(null);
        view.setBackgroundDrawable(null);
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            imageView.setImageDrawable(null);
        }
    }

    public void closeDialog() {
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            entry.closeDialog();
        }
    }

    public void closeDialogCheck() {
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            if (!entry.isDelayDialogClose()) {
                entry.closeDialog();
            }
        }
    }

    public TextView getFirstTextView() {
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target == null) {
            return null;
        }
        TextView ret = target.getFirstTextView();
        return ret;
    }

    public void cancelCreateCandidates() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        for (CandidatesList entry : this.mCandidatesListMap.values()) {
            entry.cancelCreateCandidates();
        }
    }

    public boolean hasCategories() {
        return this.mCategoryList.size() > 1;
    }

    public boolean hasCandidates() {
        CandidatesList target;
        if (this.mViewPager == null || (target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()))) == null) {
            return false;
        }
        boolean ret = target.hasCandidates();
        return ret;
    }

    private float convertCandHeightToScaleFactor(int height) {
        if (this.mWnn == null) {
            return 0.0f;
        }
        Resources res = this.mWnn.getResources();
        int defaultHeight = res.getDimensionPixelSize(R.dimen.cand_minimum_height);
        return height / defaultHeight;
    }

    public int convertScaleFactorToCandHeight(float scaleFactor) {
        if (this.mWnn == null) {
            return 0;
        }
        Resources res = this.mWnn.getResources();
        int defaultHeight = res.getDimensionPixelSize(R.dimen.cand_minimum_height);
        int candHeight = (int) (defaultHeight * scaleFactor);
        float candManScaleFactor = convertCandHeightToScaleFactor(candHeight);
        int candHeight2 = (int) (defaultHeight * candManScaleFactor);
        return candHeight2;
    }

    public void updateParameters(int width, int height) {
        this.mDisplayWidth = width;
        this.mViewPager.setDisplayWidth(this.mDisplayWidth);
        float scaleFactor = convertCandHeightToScaleFactor(height);
        setScaleFactor(scaleFactor);
        CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
        if (target != null) {
            target.setScaleFactor(this.mCandScaleFactor, false);
            target.setViewWidth(width);
            target.createTemplateCandidateView();
            if (!this.mAutoHideMode) {
                target.setDummyCandidateView(getNumberOfLine());
            }
        }
    }

    public void resizeSymbolListPreparation() {
        if (this.mIsSymbolMode) {
            CandidatesList target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()));
            target.initCandidatesList(0, false);
        }
    }

    public void resizeSymbolList() {
        if (this.mIsSymbolMode) {
            ViewGroup tabs = (ViewGroup) this.mCategoryBarHost.findViewById(android.R.id.tabs);
            for (int i = 0; i < tabs.getChildCount(); i++) {
                setupCategoryView(tabs.getChildAt(i), this.mArrayWidthCategoryImage.get(i).intValue());
            }
            LinearLayout.LayoutParams linearParam = (LinearLayout.LayoutParams) this.mCategoryBarHost.getLayoutParams();
            linearParam.height = Keyboard.getOneRowKeyboardHeight();
            this.mCategoryBarHost.setLayoutParams(linearParam);
            updateSymbolType();
            updateKey();
            setViewType(this.mViewType);
        }
    }

    private void setupCategoryView(View view, int imageWidth) {
        if (view != null && this.mWnn != null) {
            int height = Keyboard.getOneRowKeyboardHeight();
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) view.getLayoutParams();
            if (param == null) {
                param = new LinearLayout.LayoutParams(0, 0);
            }
            Resources res = this.mWnn.getResources();
            int textSize = getTextSize(res.getDimensionPixelSize(R.dimen.cand_category_text_size));
            int xPadding = (int) res.getFraction(R.fraction.cand_view_category_x_padding, textSize, textSize);
            int yPadding = (int) res.getFraction(R.fraction.cand_view_category_y_padding, height, height);
            DisplayMetrics dm = res.getDisplayMetrics();
            View parent = (View) this.mCategoryBarHost.getTabWidget().getParent();
            int parentWidth = (dm.widthPixels - parent.getPaddingLeft()) - parent.getPaddingRight();
            int maxWidth = parentWidth / 2;
            if (view instanceof ImageView) {
                param.width = (int) ((imageWidth * this.mKeyboardManager.getKeyboardScaleRate().y) + (xPadding * 2));
            } else if (view instanceof TextView) {
                TextView textView = (TextView) view;
                textView.setTextSize(0, textSize);
                textView.setMaxWidth(maxWidth);
                param.width = -2;
                yPadding = 0;
            }
            view.setPadding(xPadding, yPadding, xPadding, yPadding);
            param.setMargins(0, 0, 0, 0);
            param.height = height;
            view.setLayoutParams(param);
        }
    }

    public int getCandidatesAreaHeight(int area, boolean checkVisibility) {
        if (this.mWnn == null) {
            return 0;
        }
        switch (area) {
            case 0:
                int ret = this.mCandidateMinimumHeight;
                return ret;
            case 1:
                if (this.mViewType == 0) {
                    int ret2 = this.mCandidateMinimumHeight * getNumberOfLine();
                    return ret2;
                }
                int keyboardHeight = this.mKeyboardManager.getKeyboardSize(false).y;
                int rowHeight = Keyboard.getOneRowKeyboardHeight();
                int ret3 = keyboardHeight + (this.mCandidateMinimumHeight * getNumberOfLine());
                if (checkVisibility) {
                    if (this.mIsSymbolMode) {
                        ret3 -= this.mKeyboardManager.getKeyboardMenuHeight();
                    }
                    if (this.mCategoryBarHost.getVisibility() == 0) {
                        ret3 -= rowHeight;
                    }
                    if (this.mViewCandidateListTab.getVisibility() == 0) {
                        return ret3 - rowHeight;
                    }
                    return ret3;
                }
                return ret3;
            case 2:
                if (checkVisibility && this.mViewBody != null && this.mViewBody.getVisibility() != 0) {
                    return 0;
                }
                IWnnImeBase base = this.mWnn.getCurrentIWnnIme();
                if (base != null) {
                    if (!this.mIsSymbolMode && !base.isEnableConverter()) {
                        return 0;
                    }
                    if (!base.isHardKeyboardHidden()) {
                        int ret4 = getCandidatesAreaHeight(1, false);
                        return ret4;
                    }
                }
                int ret5 = this.mCandidateMinimumHeight * getNumberOfLine();
                return ret5;
            default:
                return 0;
        }
    }

    private void updateCandidateListLayout(boolean isPosChanged) {
        AbsoluteLayout.LayoutParams params;
        CandidatesList target;
        Size listSize;
        if (this.mViewBody != null && this.mViewPager != null && this.mKeyboardManager != null && this.mWnn != null && this.mWnn.isHwCandWindow() && this.mInputCharRect != null && (params = (AbsoluteLayout.LayoutParams) this.mViewBody.getLayoutParams()) != null && (target = this.mCandidatesListMap.get(Integer.valueOf(this.mViewPager.getCurrentItem()))) != null && (listSize = target.getListSize()) != null) {
            if (isPosChanged || params.width != listSize.getWidth() || params.height != listSize.getHeight()) {
                params.width = listSize.getWidth();
                params.height = listSize.getHeight();
                params.x = this.mInputCharRect.left;
                params.y = this.mInputCharRect.bottom;
                if (params.x + params.width > this.mDisplayWidthMax) {
                    params.x = this.mDisplayWidthMax - params.width;
                }
                if (params.x < 0) {
                    params.x = 0;
                }
                boolean isUpperPosition = false;
                int IndicatorHeight = this.mKeyboardManager.getKeyboardViewHeight();
                int maxHeight = this.mDisplayHeightMax - IndicatorHeight;
                if (params.y > maxHeight / 2 && params.y + params.height > maxHeight && params.y - params.height >= 0) {
                    params.y = this.mInputCharRect.top - params.height;
                    isUpperPosition = true;
                }
                int maxShowSize = isUpperPosition ? params.height + params.y : maxHeight - params.y;
                if (maxShowSize < params.height) {
                    target.setMaxViewHeight(maxShowSize);
                    Size listSize2 = target.getListSize();
                    if (listSize2 != null) {
                        params.width = listSize2.getWidth();
                        int newHeight = listSize2.getHeight();
                        if (isUpperPosition) {
                            params.y += params.height - newHeight;
                        }
                        params.height = newHeight;
                    } else {
                        return;
                    }
                }
                this.mViewBody.setLayoutParams(params);
                ViewGroup.LayoutParams vpParams = new LinearLayout.LayoutParams(params.width, params.height);
                this.mViewPager.setLayoutParams(vpParams);
            }
        }
    }

    public void setInputCharRect(Rect rect) {
        boolean isPosChanged = true;
        rect.right = 0;
        if (this.mInputCharRect != null && this.mInputCharRect.equals(rect)) {
            isPosChanged = false;
        }
        this.mInputCharRect = rect;
        updateCandidateListLayout(isPosChanged);
    }

    public Point getCandidateListPos() {
        AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) this.mViewBody.getLayoutParams();
        if (params == null) {
            return null;
        }
        return new Point(params.x, params.y);
    }

    public Rect getHwCandWindowRect() {
        AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) this.mViewBody.getLayoutParams();
        if (params == null) {
            return null;
        }
        return new Rect(params.x, params.y, params.x + params.width, params.y + params.height);
    }

    private int getCurrentCategory(int position) {
        if (WnnAccessibility.isAccessibility(this.mWnn) && this.mIsSymbolMode) {
            return this.mCurrentCategoryForAccessibility;
        }
        return position;
    }

    private void announceDescriptionOfSymbolList() {
        if (this.mCurrentTabView != null && this.mCurrentTabWidgetView != null) {
            CharSequence description = WnnAccessibility.getDescriptionSymbolList(this.mWnn, this.mCurrentTabView.getContentDescription(), this.mCurrentTabWidgetView.getContentDescription());
            WnnAccessibility.announceForAccessibility(this.mWnn, description, this.mViewBody);
        }
    }

    public boolean isRecreateHWCandidateList(int editorTargetSdkVersion) {
        CandidatesList currentList = this.mCandidatesListMap.get(0);
        if (currentList == null) {
            return false;
        }
        if (editorTargetSdkVersion >= 4) {
            if (currentList.getClass() != TextCandidatesList.class) {
                return false;
            }
            return true;
        }
        if (currentList.getClass() != TextCandidatesListHW.class) {
            return false;
        }
        return true;
    }
}
