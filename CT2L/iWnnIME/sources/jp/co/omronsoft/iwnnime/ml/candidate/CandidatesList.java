package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.media.TransportMediator;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.KeyboardResourcesDataManager;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public abstract class CandidatesList {
    protected static final int CAND_DISP_MAX_CACHE = 2;
    protected static final int DIRECTION_PAGE_DOWN = -998;
    protected static final int DIRECTION_PAGE_UP = 998;
    protected static final int DIRECTION_TO_END = -999;
    protected static final int DIRECTION_TO_HOME = 999;
    protected static final int FOCUS_NONE = -1;
    protected static final int GET_CANDIDATE_MAX_COUNT = 500;
    private static final String HINT_LABEL_FULL_WIDTH = "全";
    private static final String HINT_LABEL_HALF_WIDTH = "半";
    protected static final int MSG_MOVE_FOCUS = 0;
    protected static final int MSG_SET_CANDIDATES = 1;
    protected static final int SEPARATE_CANDIDATES = 20;
    protected static final int SETTING_NUMBER_OF_LINEMAX = 5;
    protected static final int SET_CANDIDATE_DELAY_LINE_COUNT = 2;
    protected static final int SET_CANDIDATE_DELAY_SYMBOL = 100;
    protected static final int SET_CANDIDATE_DELAY_TEXT = 0;
    protected static final int SET_CANDIDATE_FIRST_LINE_COUNT = 5;
    protected ArrayList<TextView> mAddViewList;
    protected int mCandEmojiTextSize;
    protected int mCandNormalTextSize;
    protected int mCandidateDefaultMinimumHeight;
    protected int mCandidateMinimumHeight;
    protected int mCandidateMinimumWidth;
    protected TextView mFirstLineLastView;
    protected int mOccupyCount;
    protected boolean mPortrait;
    protected TextView mPrevView;
    protected int mViewWidth;
    protected IWnnIME mWnn;
    private static final int[][] COMBINING_CHAR_THRESHOLD_TABLE = {new int[]{768, 879}, new int[]{6832, 6911}, new int[]{7616, 7679}, new int[]{8400, 8447}, new int[]{65056, 65071}};
    private static final Integer ATTR_HALF_CHAR = 0;
    private static final Integer ATTR_HALF_NUMBER = 1;
    private static final Integer ATTR_FULL_CHAR = 2;
    private static final Integer ATTR_FULL_NUMBER = 3;
    private static final HashMap<Integer, Integer> SHOW_HINT_UNICODE_LIST = new HashMap<Integer, Integer>() {
        {
            put(32, CandidatesList.ATTR_HALF_CHAR);
            put(33, CandidatesList.ATTR_HALF_CHAR);
            put(34, CandidatesList.ATTR_HALF_CHAR);
            put(35, CandidatesList.ATTR_HALF_CHAR);
            put(36, CandidatesList.ATTR_HALF_CHAR);
            put(37, CandidatesList.ATTR_HALF_CHAR);
            put(38, CandidatesList.ATTR_HALF_CHAR);
            put(39, CandidatesList.ATTR_HALF_CHAR);
            put(40, CandidatesList.ATTR_HALF_CHAR);
            put(41, CandidatesList.ATTR_HALF_CHAR);
            put(42, CandidatesList.ATTR_HALF_CHAR);
            put(43, CandidatesList.ATTR_HALF_CHAR);
            put(44, CandidatesList.ATTR_HALF_CHAR);
            put(45, CandidatesList.ATTR_HALF_CHAR);
            put(46, CandidatesList.ATTR_HALF_CHAR);
            put(47, CandidatesList.ATTR_HALF_CHAR);
            put(48, CandidatesList.ATTR_HALF_NUMBER);
            put(49, CandidatesList.ATTR_HALF_NUMBER);
            put(50, CandidatesList.ATTR_HALF_NUMBER);
            put(51, CandidatesList.ATTR_HALF_NUMBER);
            put(52, CandidatesList.ATTR_HALF_NUMBER);
            put(53, CandidatesList.ATTR_HALF_NUMBER);
            put(54, CandidatesList.ATTR_HALF_NUMBER);
            put(55, CandidatesList.ATTR_HALF_NUMBER);
            put(56, CandidatesList.ATTR_HALF_NUMBER);
            put(57, CandidatesList.ATTR_HALF_NUMBER);
            put(58, CandidatesList.ATTR_HALF_CHAR);
            put(59, CandidatesList.ATTR_HALF_CHAR);
            put(60, CandidatesList.ATTR_HALF_CHAR);
            put(61, CandidatesList.ATTR_HALF_CHAR);
            put(62, CandidatesList.ATTR_HALF_CHAR);
            put(63, CandidatesList.ATTR_HALF_CHAR);
            put(64, CandidatesList.ATTR_HALF_CHAR);
            put(65, CandidatesList.ATTR_HALF_CHAR);
            put(66, CandidatesList.ATTR_HALF_CHAR);
            put(67, CandidatesList.ATTR_HALF_CHAR);
            put(68, CandidatesList.ATTR_HALF_CHAR);
            put(69, CandidatesList.ATTR_HALF_CHAR);
            put(70, CandidatesList.ATTR_HALF_CHAR);
            put(71, CandidatesList.ATTR_HALF_CHAR);
            put(72, CandidatesList.ATTR_HALF_CHAR);
            put(73, CandidatesList.ATTR_HALF_CHAR);
            put(74, CandidatesList.ATTR_HALF_CHAR);
            put(75, CandidatesList.ATTR_HALF_CHAR);
            put(76, CandidatesList.ATTR_HALF_CHAR);
            put(77, CandidatesList.ATTR_HALF_CHAR);
            put(78, CandidatesList.ATTR_HALF_CHAR);
            put(79, CandidatesList.ATTR_HALF_CHAR);
            put(80, CandidatesList.ATTR_HALF_CHAR);
            put(81, CandidatesList.ATTR_HALF_CHAR);
            put(82, CandidatesList.ATTR_HALF_CHAR);
            put(83, CandidatesList.ATTR_HALF_CHAR);
            put(84, CandidatesList.ATTR_HALF_CHAR);
            put(85, CandidatesList.ATTR_HALF_CHAR);
            put(86, CandidatesList.ATTR_HALF_CHAR);
            put(87, CandidatesList.ATTR_HALF_CHAR);
            put(88, CandidatesList.ATTR_HALF_CHAR);
            put(89, CandidatesList.ATTR_HALF_CHAR);
            put(90, CandidatesList.ATTR_HALF_CHAR);
            put(91, CandidatesList.ATTR_HALF_CHAR);
            put(92, CandidatesList.ATTR_HALF_CHAR);
            put(93, CandidatesList.ATTR_HALF_CHAR);
            put(94, CandidatesList.ATTR_HALF_CHAR);
            put(95, CandidatesList.ATTR_HALF_CHAR);
            put(96, CandidatesList.ATTR_HALF_CHAR);
            put(97, CandidatesList.ATTR_HALF_CHAR);
            put(98, CandidatesList.ATTR_HALF_CHAR);
            put(99, CandidatesList.ATTR_HALF_CHAR);
            put(100, CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_FULL_KATAKANA), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_HALF_KATAKANA), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_EISU_KANA), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_EMOJI), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_SYMBOL), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_KAO_MOJI), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_DECOEMOJI), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_ADD_SYMBOL), CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(IWnnImeBase.ENGINE_MODE_OPT_TYPE_50KEY), CandidatesList.ATTR_HALF_CHAR);
            put(113, CandidatesList.ATTR_HALF_CHAR);
            put(114, CandidatesList.ATTR_HALF_CHAR);
            put(115, CandidatesList.ATTR_HALF_CHAR);
            put(116, CandidatesList.ATTR_HALF_CHAR);
            put(117, CandidatesList.ATTR_HALF_CHAR);
            put(118, CandidatesList.ATTR_HALF_CHAR);
            put(119, CandidatesList.ATTR_HALF_CHAR);
            put(120, CandidatesList.ATTR_HALF_CHAR);
            put(121, CandidatesList.ATTR_HALF_CHAR);
            put(122, CandidatesList.ATTR_HALF_CHAR);
            put(123, CandidatesList.ATTR_HALF_CHAR);
            put(124, CandidatesList.ATTR_HALF_CHAR);
            put(125, CandidatesList.ATTR_HALF_CHAR);
            put(Integer.valueOf(TransportMediator.KEYCODE_MEDIA_PLAY), CandidatesList.ATTR_HALF_CHAR);
            put(65377, CandidatesList.ATTR_HALF_CHAR);
            put(65378, CandidatesList.ATTR_HALF_CHAR);
            put(65379, CandidatesList.ATTR_HALF_CHAR);
            put(65380, CandidatesList.ATTR_HALF_CHAR);
            put(65392, CandidatesList.ATTR_HALF_CHAR);
            put(162, CandidatesList.ATTR_HALF_CHAR);
            put(163, CandidatesList.ATTR_HALF_CHAR);
            put(172, CandidatesList.ATTR_HALF_CHAR);
            put(175, CandidatesList.ATTR_HALF_CHAR);
            put(165, CandidatesList.ATTR_HALF_CHAR);
            put(8361, CandidatesList.ATTR_HALF_CHAR);
            put(12288, CandidatesList.ATTR_FULL_CHAR);
            put(65281, CandidatesList.ATTR_FULL_CHAR);
            put(65282, CandidatesList.ATTR_FULL_CHAR);
            put(65283, CandidatesList.ATTR_FULL_CHAR);
            put(65284, CandidatesList.ATTR_FULL_CHAR);
            put(65285, CandidatesList.ATTR_FULL_CHAR);
            put(65286, CandidatesList.ATTR_FULL_CHAR);
            put(65287, CandidatesList.ATTR_FULL_CHAR);
            put(65288, CandidatesList.ATTR_FULL_CHAR);
            put(65289, CandidatesList.ATTR_FULL_CHAR);
            put(65290, CandidatesList.ATTR_FULL_CHAR);
            put(65291, CandidatesList.ATTR_FULL_CHAR);
            put(65292, CandidatesList.ATTR_FULL_CHAR);
            put(65293, CandidatesList.ATTR_FULL_CHAR);
            put(65294, CandidatesList.ATTR_FULL_CHAR);
            put(65295, CandidatesList.ATTR_FULL_CHAR);
            put(65296, CandidatesList.ATTR_FULL_NUMBER);
            put(65297, CandidatesList.ATTR_FULL_NUMBER);
            put(65298, CandidatesList.ATTR_FULL_NUMBER);
            put(65299, CandidatesList.ATTR_FULL_NUMBER);
            put(65300, CandidatesList.ATTR_FULL_NUMBER);
            put(65301, CandidatesList.ATTR_FULL_NUMBER);
            put(65302, CandidatesList.ATTR_FULL_NUMBER);
            put(65303, CandidatesList.ATTR_FULL_NUMBER);
            put(65304, CandidatesList.ATTR_FULL_NUMBER);
            put(65305, CandidatesList.ATTR_FULL_NUMBER);
            put(65306, CandidatesList.ATTR_FULL_CHAR);
            put(65307, CandidatesList.ATTR_FULL_CHAR);
            put(65308, CandidatesList.ATTR_FULL_CHAR);
            put(65309, CandidatesList.ATTR_FULL_CHAR);
            put(65310, CandidatesList.ATTR_FULL_CHAR);
            put(65311, CandidatesList.ATTR_FULL_CHAR);
            put(65312, CandidatesList.ATTR_FULL_CHAR);
            put(65313, CandidatesList.ATTR_FULL_CHAR);
            put(65314, CandidatesList.ATTR_FULL_CHAR);
            put(65315, CandidatesList.ATTR_FULL_CHAR);
            put(65316, CandidatesList.ATTR_FULL_CHAR);
            put(65317, CandidatesList.ATTR_FULL_CHAR);
            put(65318, CandidatesList.ATTR_FULL_CHAR);
            put(65319, CandidatesList.ATTR_FULL_CHAR);
            put(65320, CandidatesList.ATTR_FULL_CHAR);
            put(65321, CandidatesList.ATTR_FULL_CHAR);
            put(65322, CandidatesList.ATTR_FULL_CHAR);
            put(65323, CandidatesList.ATTR_FULL_CHAR);
            put(65324, CandidatesList.ATTR_FULL_CHAR);
            put(65325, CandidatesList.ATTR_FULL_CHAR);
            put(65326, CandidatesList.ATTR_FULL_CHAR);
            put(65327, CandidatesList.ATTR_FULL_CHAR);
            put(65328, CandidatesList.ATTR_FULL_CHAR);
            put(65329, CandidatesList.ATTR_FULL_CHAR);
            put(65330, CandidatesList.ATTR_FULL_CHAR);
            put(65331, CandidatesList.ATTR_FULL_CHAR);
            put(65332, CandidatesList.ATTR_FULL_CHAR);
            put(65333, CandidatesList.ATTR_FULL_CHAR);
            put(65334, CandidatesList.ATTR_FULL_CHAR);
            put(65335, CandidatesList.ATTR_FULL_CHAR);
            put(65336, CandidatesList.ATTR_FULL_CHAR);
            put(65337, CandidatesList.ATTR_FULL_CHAR);
            put(65338, CandidatesList.ATTR_FULL_CHAR);
            put(65339, CandidatesList.ATTR_FULL_CHAR);
            put(65340, CandidatesList.ATTR_FULL_CHAR);
            put(65341, CandidatesList.ATTR_FULL_CHAR);
            put(65342, CandidatesList.ATTR_FULL_CHAR);
            put(65343, CandidatesList.ATTR_FULL_CHAR);
            put(65344, CandidatesList.ATTR_FULL_CHAR);
            put(65345, CandidatesList.ATTR_FULL_CHAR);
            put(65346, CandidatesList.ATTR_FULL_CHAR);
            put(65347, CandidatesList.ATTR_FULL_CHAR);
            put(65348, CandidatesList.ATTR_FULL_CHAR);
            put(65349, CandidatesList.ATTR_FULL_CHAR);
            put(65350, CandidatesList.ATTR_FULL_CHAR);
            put(65351, CandidatesList.ATTR_FULL_CHAR);
            put(65352, CandidatesList.ATTR_FULL_CHAR);
            put(65353, CandidatesList.ATTR_FULL_CHAR);
            put(65354, CandidatesList.ATTR_FULL_CHAR);
            put(65355, CandidatesList.ATTR_FULL_CHAR);
            put(65356, CandidatesList.ATTR_FULL_CHAR);
            put(65357, CandidatesList.ATTR_FULL_CHAR);
            put(65358, CandidatesList.ATTR_FULL_CHAR);
            put(65359, CandidatesList.ATTR_FULL_CHAR);
            put(65360, CandidatesList.ATTR_FULL_CHAR);
            put(65361, CandidatesList.ATTR_FULL_CHAR);
            put(65362, CandidatesList.ATTR_FULL_CHAR);
            put(65363, CandidatesList.ATTR_FULL_CHAR);
            put(65364, CandidatesList.ATTR_FULL_CHAR);
            put(65365, CandidatesList.ATTR_FULL_CHAR);
            put(65366, CandidatesList.ATTR_FULL_CHAR);
            put(65367, CandidatesList.ATTR_FULL_CHAR);
            put(65368, CandidatesList.ATTR_FULL_CHAR);
            put(65369, CandidatesList.ATTR_FULL_CHAR);
            put(65370, CandidatesList.ATTR_FULL_CHAR);
            put(65371, CandidatesList.ATTR_FULL_CHAR);
            put(65372, CandidatesList.ATTR_FULL_CHAR);
            put(65373, CandidatesList.ATTR_FULL_CHAR);
            put(65374, CandidatesList.ATTR_FULL_CHAR);
            put(12290, CandidatesList.ATTR_FULL_CHAR);
            put(12300, CandidatesList.ATTR_FULL_CHAR);
            put(12301, CandidatesList.ATTR_FULL_CHAR);
            put(12289, CandidatesList.ATTR_FULL_CHAR);
            put(12540, CandidatesList.ATTR_FULL_CHAR);
            put(65504, CandidatesList.ATTR_FULL_CHAR);
            put(65505, CandidatesList.ATTR_FULL_CHAR);
            put(65506, CandidatesList.ATTR_FULL_CHAR);
            put(65507, CandidatesList.ATTR_FULL_CHAR);
            put(65509, CandidatesList.ATTR_FULL_CHAR);
            put(65510, CandidatesList.ATTR_FULL_CHAR);
        }
    };
    protected static int NON_LIMITED_DISPLAY_LINE_COUNT = 99999;
    protected ViewGroup mViewlistBody = null;
    protected ViewGroup mViewBodyScroll = null;
    protected AbsoluteLayout mViewCandidateList = null;
    protected AbsoluteLayout mViewCandidateHintChar = null;
    protected TextView mViewCandidateNothing = null;
    protected TextView mViewCandidateNoHistory = null;
    private TextView mViewCandidateTemplate = null;
    protected int mTextColor = 0;
    protected int mWebAPIKeyTextColor = 0;
    protected int mWebAPICandTextColor = 0;
    protected int mNoCandidateTextColor = 0;
    protected int mLineCount = 1;
    protected ArrayList<WnnWord> mWnnWordArray = new ArrayList<>();
    protected int mMaxTextViewWidth = 0;
    protected Size mListSize = null;
    protected View.OnClickListener mCandidateOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ((v instanceof TextView) && v.isShown() && CandidatesList.this.mWnn != null) {
                CandidatesList.this.onClickCandidate((TextView) v);
            }
        }
    };
    protected View.OnLongClickListener mCandidateOnLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            boolean ret = false;
            if (!CandidatesList.this.mEnableCandidateLongClick) {
                return false;
            }
            if (v instanceof TextView) {
                if (!((TextView) v).isShown()) {
                    return true;
                }
                ret = CandidatesList.this.onLongClickCandidate((TextView) v);
            }
            return ret;
        }
    };
    protected View.OnTouchListener mDummyCandidateOnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };
    protected int mPortraitNumberOfLine = 2;
    protected int mLandscapeNumberOfLine = 1;
    protected TextView mPrevViewHint = null;
    protected TextView mFirstLineLastViewHint = null;
    protected TextView mWebApiButton = null;
    protected int mEmojiKind = 4;
    protected int mPreviousEmojiKind = 4;
    protected int mSymbolMode = 1;
    protected int mCandViewStandardDiv = 0;
    protected boolean mEnableVibrate = false;
    protected boolean mEnablePlaySound = false;
    protected boolean mEnableCandidateLongClick = true;
    protected int mCurrentFocusIndex = -1;
    protected View mFocusedView = null;
    protected Drawable mFocusedViewBackground = null;
    protected int mFocusAxisX = 0;
    protected boolean mEnableMushroom = false;
    private WnnWord mDummyWnnWord = new WnnWord();
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    CandidatesList.this.moveFocus(msg.arg1, msg.arg2 == 1);
                    break;
                case 1:
                    CandidatesList.this.createCandidatesListView(msg.arg1);
                    break;
            }
        }
    };

    public void initView(IWnnIME parent, int displayWidth) {
        this.mWnn = parent;
        this.mViewWidth = displayWidth;
        this.mAddViewList = new ArrayList<>();
        if (this.mWnn != null) {
            Resources res = this.mWnn.getResources();
            this.mPortrait = res.getConfiguration().orientation != 2;
            this.mCandViewStandardDiv = res.getInteger(R.integer.cand_view_standard_div);
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            this.mTextColor = resMan.getColor(this.mWnn, R.color.candidate_text);
        }
    }

    public void setNumberOfDisplayLines(int portrait, int landscape) {
        this.mPortraitNumberOfLine = portrait;
        this.mLandscapeNumberOfLine = landscape;
    }

    public void initCandidatesList(int line, boolean isDummy) {
        this.mOccupyCount = 0;
        this.mPrevView = null;
        this.mPrevViewHint = null;
        this.mFirstLineLastView = null;
        this.mFirstLineLastViewHint = null;
        this.mLineCount = 1;
        this.mWnnWordArray.clear();
        this.mWebApiButton = null;
        int first = 0;
        if (isDummy) {
            first = setDummyCandidateView(line);
        }
        int size = this.mViewCandidateList.getChildCount();
        for (int i = first; i < size; i++) {
            View v = this.mViewCandidateList.getChildAt(i);
            v.setVisibility(8);
        }
        if (this.mAddViewList != null) {
            for (TextView v2 : this.mAddViewList) {
                EmojiAssist.getInstance().removeView(v2);
            }
            this.mAddViewList.clear();
        }
        this.mViewCandidateHintChar.removeAllViews();
        cancelCreateCandidates();
        ((ScrollView) this.mViewBodyScroll).scrollTo(0, 0);
        this.mWnn.endEmojiAssist();
        if (this.mWnn.isEmoji() && this.mWnn.getExtractEditText() != null && !this.mPortrait) {
            Spannable text = this.mWnn.getExtractEditText().getText();
            int ret = this.mWnn.getEmojiAssist().checkTextData(text);
            if (ret >= 2) {
                this.mWnn.startEmojiAssist();
            }
        }
    }

    public void createCandidatesListView(int maxLine) {
    }

    public void cancelCreateCandidates() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
    }

    public View getCandidatesListView() {
        return this.mViewlistBody;
    }

    public ScrollView getCandidatesListScrollView() {
        return (ScrollView) this.mViewBodyScroll;
    }

    public ArrayList<TextView> getAddViewList() {
        return this.mAddViewList;
    }

    public TextView getFirstTextView() {
        return getCandidateView(0);
    }

    public void setPreferences(boolean vibrate, boolean playSound, boolean mushEnable) {
        this.mEnableVibrate = vibrate;
        this.mEnablePlaySound = playSound;
        this.mEnableMushroom = mushEnable;
    }

    public WnnWord getFocusedWnnWord() {
        return getWnnWord(this.mCurrentFocusIndex);
    }

    public void closeDialog() {
    }

    public boolean isDelayDialogClose() {
        return false;
    }

    public void setMaxViewHeight(int maxHeight) {
    }

    public void setViewType(int type, int height) {
    }

    protected void createTemplateCandidateView() {
        this.mViewCandidateTemplate = createCandidateView();
        this.mViewCandidateTemplate.setPadding(0, 0, 0, 0);
        this.mViewCandidateTemplate.setVisibility(8);
    }

    protected int getCandidateDivisionNum() {
        return 0;
    }

    protected TextView createCandidateView() {
        TextView text = new CandidateTextView(this.mViewBodyScroll.getContext());
        text.setTextSize(0, this.mCandNormalTextSize);
        setCandidateBackGround(text, R.drawable.cand_back);
        text.setCompoundDrawablePadding(0);
        text.setSingleLine();
        text.setLayoutParams(new LinearLayout.LayoutParams(-2, -2, 1.0f));
        text.setMinHeight(this.mCandidateMinimumHeight);
        text.setMinimumWidth(this.mCandidateMinimumWidth);
        text.setSoundEffectsEnabled(false);
        ((CandidateTextView) text).setIWnnIME(this.mWnn);
        return text;
    }

    protected void setHintLabel(WnnWord word) {
        if (word != null && word.candidate != null && word.candidate.length() >= 1) {
            boolean foundOtherChar = false;
            boolean[] foundAttribute = {false, false, false, false};
            for (int i = 0; i < word.candidate.length(); i++) {
                Integer attribute = SHOW_HINT_UNICODE_LIST.get(Integer.valueOf(word.candidate.codePointAt(i)));
                if (attribute != null && (i + 1 >= word.candidate.length() || !isCombiningChar(word.candidate.codePointAt(i + 1)))) {
                    foundAttribute[attribute.intValue()] = true;
                } else {
                    foundOtherChar = true;
                }
            }
            if (foundAttribute[ATTR_HALF_NUMBER.intValue()] || foundAttribute[ATTR_FULL_NUMBER.intValue()] || !foundOtherChar) {
                if (!foundAttribute[ATTR_FULL_CHAR.intValue()] && !foundAttribute[ATTR_FULL_NUMBER.intValue()]) {
                    word.hint = HINT_LABEL_HALF_WIDTH;
                } else if (!foundAttribute[ATTR_HALF_CHAR.intValue()] && !foundAttribute[ATTR_HALF_NUMBER.intValue()]) {
                    word.hint = HINT_LABEL_FULL_WIDTH;
                }
            }
        }
    }

    private boolean isCombiningChar(int code) {
        int length = COMBINING_CHAR_THRESHOLD_TABLE.length;
        for (int i = 0; i < length; i++) {
            if (code >= COMBINING_CHAR_THRESHOLD_TABLE[i][0] && code <= COMBINING_CHAR_THRESHOLD_TABLE[i][1]) {
                return true;
            }
        }
        return false;
    }

    protected boolean setCandidate(WnnWord word) {
        TextView candView;
        if (this.mWnn == null || word == null || this.mWnnWordArray == null || (candView = getCandidateView()) == null) {
            return false;
        }
        this.mWnnWordArray.add(this.mWnnWordArray.size(), word);
        CharSequence candidate = DecoEmojiUtil.getSpannedCandidate(word);
        if (candidate == null || candidate.length() == 0) {
            return false;
        }
        candView.setText(candidate);
        if (WnnAccessibility.isAccessibility(this.mWnn)) {
            candView.setContentDescription(WnnAccessibility.getDescriptionCandidate(this.mWnn, word.stroke, word.candidate));
        }
        TextView hintView = null;
        if (word.hint != null && word.hint.length() > 0) {
            hintView = new TextView(this.mViewBodyScroll.getContext());
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            hintView.setText(word.hint);
            hintView.setTextSize(0, this.mCandNormalTextSize / 2.0f);
            hintView.setTextColor(resMan.getColor(this.mWnn, R.color.candidate_hint_color));
            hintView.setGravity(85);
            hintView.setImportantForAccessibility(2);
            this.mViewCandidateHintChar.addView(hintView);
        }
        if ((word.attribute & 512) != 0 || (word.attribute & 16384) != 0) {
            candView.setTextColor(this.mWebAPIKeyTextColor);
            candView.setOnLongClickListener(null);
            setCandidateBackGround(candView, R.drawable.cand_back_webapi);
            this.mWebApiButton = candView;
        } else if ((word.attribute & 1024) != 0) {
            candView.setTextColor(this.mWebAPICandTextColor);
        } else if ((word.attribute & 2048) != 0) {
            candView.setTextColor(this.mNoCandidateTextColor);
            candView.setOnLongClickListener(null);
            candView.setOnClickListener(null);
            setCandidateBackGround(candView, R.drawable.cand_back_webapi);
        }
        Drawable spanDraw = null;
        Resources res = this.mWnn.getResources();
        if (word.candidate.equals(" ") || word.candidate.equals("\u2005")) {
            spanDraw = res.getDrawable(R.drawable.word_quarter_space);
        } else if (word.candidate.equals("\u3000") || word.candidate.equals("\u2003")) {
            spanDraw = res.getDrawable(R.drawable.word_full_space);
        } else if (word.candidate.equals("\u2002")) {
            spanDraw = res.getDrawable(R.drawable.word_half_space);
        }
        if (spanDraw != null) {
            SpannableString spannable = new SpannableString("   ");
            spanDraw.setBounds(0, 0, spanDraw.getIntrinsicWidth(), spanDraw.getIntrinsicHeight());
            spanDraw.setColorFilter(candView.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
            ImageSpan span = new ImageSpan(spanDraw, 1);
            spannable.setSpan(span, 1, 2, 33);
            candView.setText(spannable);
            candView.setTextSize(0, this.mCandNormalTextSize);
        }
        Spannable text = new SpannableString(candView.getText());
        int ret = EmojiAssist.getInstance().checkTextData(text);
        if (ret >= 2) {
            boolean attrDecoEmoji = false;
            if ((word.attribute & iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI) != 0) {
                attrDecoEmoji = true;
            }
            EmojiAssist.getInstance().addView(candView, attrDecoEmoji);
            this.mAddViewList.add(candView);
            if (ret >= 2 && this.mWnn.isEmoji()) {
                this.mWnn.startEmojiAssist();
            }
        }
        int textWidth = setCandidateLayout(word, candView, hintView, false);
        if (textWidth == 0) {
            return false;
        }
        if (this.mMaxTextViewWidth < textWidth) {
            this.mMaxTextViewWidth = textWidth;
        }
        return true;
    }

    protected int calculateTextViewWidth(WnnWord word) {
        CharSequence candidate = DecoEmojiUtil.getSpannedCandidate(word);
        if (candidate == null || candidate.length() == 0) {
            return 0;
        }
        int decowidth = 0;
        boolean attrDecoEmoji = false;
        if ((word.attribute & iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI) != 0) {
            attrDecoEmoji = true;
        }
        if (DecoEmojiUtil.isDecoEmoji(word.candidate) || attrDecoEmoji) {
            StringBuffer buf = new StringBuffer();
            SpannableStringBuilder text = (SpannableStringBuilder) candidate;
            for (int i = 0; i < text.length(); i++) {
                Annotation[] annotation = (Annotation[]) text.getSpans(i, i + 1, Annotation.class);
                if (annotation != null && annotation.length > 0) {
                    if (annotation[0].getValue().indexOf(EmojiAssist.SPLIT_KEY) >= 0) {
                        String[] split = annotation[0].getValue().split(EmojiAssist.SPLIT_KEY, 0);
                        int temp_decowidth = Integer.parseInt(split[1]);
                        int height = Integer.parseInt(split[2]);
                        this.mEmojiKind = Integer.parseInt(split[3]);
                        if (height > 20) {
                            decowidth += (int) (temp_decowidth / (height / 20.0f));
                        } else {
                            decowidth += temp_decowidth;
                        }
                    }
                } else {
                    buf.append(candidate.subSequence(i, i + 1));
                }
            }
            String normal = buf.toString();
            decowidth += measureText(normal, 0, normal.length());
        }
        int iMeasureText = measureText(candidate, 0, candidate.length()) + this.mViewCandidateTemplate.getPaddingLeft() + this.mViewCandidateTemplate.getPaddingRight();
        if (decowidth != 0) {
            int textViewWidth = this.mViewCandidateTemplate.getPaddingLeft() + decowidth + this.mViewCandidateTemplate.getPaddingRight();
            TextPaint paint = this.mViewCandidateTemplate.getPaint();
            float textSize = paint.getTextSize();
            int textViewWidth2 = (int) (textViewWidth * (textSize / 20.0f));
            return (!attrDecoEmoji || textViewWidth2 <= this.mViewWidth) ? textViewWidth2 : this.mViewWidth;
        }
        return iMeasureText;
    }

    protected int setCandidateLayout(WnnWord word, TextView candView, TextView hintView, boolean isDummy) {
        if (word == null || candView == null || this.mWnn == null) {
            return 0;
        }
        this.mEmojiKind = 0;
        int divisionNum = getCandidateDivisionNum();
        int divisionSize = this.mViewWidth / divisionNum;
        int textViewWidth = 0;
        if (!isDummy) {
            textViewWidth = calculateTextViewWidth(word);
        }
        int hintLabelWidth = 0;
        int hintLabelPadding = 0;
        if (hintView != null) {
            Paint paint = new Paint();
            int textSize = (int) hintView.getTextSize();
            paint.setTextSize(textSize);
            hintLabelWidth = (int) paint.measureText(word.hint);
            hintLabelPadding = textSize;
        }
        int additionalPadding = this.mWnn.getResources().getDimensionPixelSize(R.dimen.cand_additional_padding);
        int totalHintSize = (hintLabelWidth + hintLabelPadding) * 2;
        int occupyCount = Math.min(((textViewWidth + divisionSize) + Math.max(additionalPadding, totalHintSize)) / divisionSize, divisionNum);
        boolean tmpEnter = false;
        if (this.mEmojiKind == 4 && this.mEmojiKind != this.mPreviousEmojiKind) {
            tmpEnter = true;
        }
        if (!isDummy && this.mLineCount == 1 && divisionNum <= this.mOccupyCount + occupyCount && (this instanceof TextCandidatesList) && this.mOccupyCount > 0) {
            tmpEnter = true;
            this.mFirstLineLastView = this.mPrevView;
            this.mFirstLineLastViewHint = this.mPrevViewHint;
        }
        boolean isWebAPIButton = (word.attribute & 16896) != 0;
        if (this.mOccupyCount > 0 && (divisionNum < this.mOccupyCount + occupyCount || tmpEnter || isWebAPIButton)) {
            if (this.mPrevView != null) {
                AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) this.mPrevView.getLayoutParams();
                int candWidth = this.mViewWidth - params.x;
                params.width = candWidth;
                this.mViewCandidateList.updateViewLayout(this.mPrevView, params);
                if (this.mPrevViewHint != null) {
                    AbsoluteLayout.LayoutParams params2 = (AbsoluteLayout.LayoutParams) this.mPrevViewHint.getLayoutParams();
                    params2.width = candWidth;
                    this.mViewCandidateHintChar.updateViewLayout(this.mPrevViewHint, params2);
                }
            }
            this.mOccupyCount = 0;
            this.mLineCount++;
        }
        int width = divisionSize * occupyCount;
        int height = this.mCandidateMinimumHeight;
        if (occupyCount == divisionNum) {
            width = this.mViewWidth;
        } else if (divisionNum == this.mOccupyCount + occupyCount) {
            width += this.mViewWidth % divisionNum;
        }
        ViewGroup.LayoutParams candParams = buildLayoutParams(this.mViewCandidateList, width, height);
        this.mViewCandidateList.updateViewLayout(candView, candParams);
        int maxWidth = this.mViewWidth;
        int rightPadding = 0;
        if (this.mLineCount == 1) {
            maxWidth -= getReadMoreButtonWidth();
        }
        if (maxWidth < textViewWidth + totalHintSize) {
            candView.setEllipsize(TextUtils.TruncateAt.END);
            candView.setWidth(maxWidth);
            rightPadding = hintLabelWidth + hintLabelPadding;
        } else {
            candView.setEllipsize(null);
            candView.setWidth(textViewWidth);
        }
        candView.setPadding(0, 0, rightPadding, 0);
        if (hintView != null) {
            ViewGroup.LayoutParams hintParams = buildLayoutParams(this.mViewCandidateHintChar, width, height);
            this.mViewCandidateHintChar.updateViewLayout(hintView, hintParams);
            hintView.setPadding(0, 0, hintLabelPadding, hintLabelPadding / 2);
        }
        if (!isWebAPIButton) {
            this.mPreviousEmojiKind = this.mEmojiKind;
            this.mOccupyCount += occupyCount;
            this.mPrevView = candView;
            this.mPrevViewHint = hintView;
            return textViewWidth;
        }
        return textViewWidth;
    }

    protected TextView getCandidateView(int index) {
        return (TextView) this.mViewCandidateList.getChildAt(index);
    }

    private TextView getCandidateView() {
        TextView candView = getCandidateView(this.mWnnWordArray.size());
        if (candView == null) {
            candView = createCandidateView();
            this.mViewCandidateList.addView(candView);
        }
        initCandidateView(candView);
        candView.setId(this.mWnnWordArray.size());
        return candView;
    }

    protected void initCandidateView(TextView textView) {
        if (textView != null) {
            textView.setOnClickListener(this.mCandidateOnClick);
            textView.setOnLongClickListener(this.mCandidateOnLongClick);
            textView.setOnTouchListener(null);
            if (this.mSymbolMode == 3) {
                textView.setTextSize(0, this.mCandEmojiTextSize);
            } else {
                textView.setTextSize(0, this.mCandNormalTextSize);
            }
            textView.setVisibility(0);
            textView.setPressed(false);
            textView.setFocusable(false);
            textView.setHeight(this.mCandidateMinimumHeight);
            textView.setContentDescription(null);
            textView.setImportantForAccessibility(2);
            textView.setTextColor(this.mTextColor);
            setCandidateBackGround(textView, R.drawable.cand_back);
            textView.setGravity(17);
        }
    }

    private int measureText(CharSequence text, int start, int end) {
        TextPaint paint = this.mViewCandidateTemplate.getPaint();
        return CandidateTextView.getTextWidths(text, paint);
    }

    protected ViewGroup.LayoutParams buildLayoutParams(AbsoluteLayout layout, int width, int height) {
        int viewDivision = getCandidateDivisionNum();
        int indentWidth = this.mViewWidth / viewDivision;
        int x = indentWidth * this.mOccupyCount;
        int y = (this.mLineCount - 1) * this.mCandidateMinimumHeight;
        ViewGroup.LayoutParams params = new AbsoluteLayout.LayoutParams(width, height, x, y);
        return params;
    }

    protected void setCandidateBackGround(View view, int resourceId) {
        WnnWord word;
        KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
        Drawable drawable = null;
        switch (resourceId) {
            case R.drawable.cand_back:
            case R.drawable.cand_back_noline:
                drawable = resMan.getCandidateBackground();
                break;
            case R.drawable.cand_back_focuse:
                if (isFocusCandidate() && (word = getFocusedWnnWord()) != null && ((word.attribute & 2048) != 0 || (word.attribute & 512) != 0 || (word.attribute & 16384) != 0)) {
                    drawable = resMan.getCandidateFocusBackgroundWebApi();
                }
                if (drawable == null) {
                    drawable = resMan.getCandidateFocusBackground();
                }
                break;
            case R.drawable.cand_back_webapi:
                drawable = resMan.getCandidateBackgroundWebApi();
                break;
        }
        if (drawable != null) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackgroundResource(resourceId);
        }
    }

    protected void onClickCandidate(TextView text) {
    }

    protected boolean onLongClickCandidate(TextView text) {
        return false;
    }

    protected void playSoundAndVibration() {
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

    private WnnWord getWnnWord(int index) {
        if (index < 0) {
            index = 0;
            this.mHandler.removeMessages(0);
            Log.i("iwnn", "CandidatesList::getWnnWord  index < 0 ");
        } else {
            int size = this.mWnnWordArray.size();
            if (index >= size) {
                index = size - 1;
                this.mHandler.removeMessages(0);
                Log.i("iwnn", "CandidatesList::getWnnWord  index > candidate max ");
            }
        }
        if (index < 0 || this.mWnnWordArray.size() <= index) {
            return null;
        }
        WnnWord word = this.mWnnWordArray.get(index);
        return word;
    }

    public void processMoveKeyEvent(int key) {
        switch (key) {
            case 19:
                moveFocus(-1, true);
                break;
            case 20:
                moveFocus(1, true);
                break;
            case 21:
                moveFocus(-1, false);
                break;
            case 22:
                moveFocus(1, false);
                break;
            case 122:
                moveFocus(DIRECTION_TO_HOME, false);
                break;
            case 123:
                moveFocus(-999, false);
                break;
        }
    }

    public boolean isFocusCandidate() {
        return this.mCurrentFocusIndex != -1;
    }

    public void clearFocusCandidate() {
        View view = this.mFocusedView;
        if (view != null) {
            int paddingL = view.getPaddingLeft();
            int paddingT = view.getPaddingTop();
            int paddingR = view.getPaddingRight();
            int paddingB = view.getPaddingBottom();
            view.setBackgroundDrawable(this.mFocusedViewBackground);
            view.setPadding(paddingL, paddingT, paddingR, paddingB);
            WnnAccessibility.sendAccessibilityEventHoverExit(this.mWnn, view.getContentDescription(), view, view.getId(), null);
            this.mFocusedView = null;
        }
        this.mFocusAxisX = 0;
        this.mCurrentFocusIndex = -1;
        this.mHandler.removeMessages(0);
        if (view != null) {
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.FOCUS_CANDIDATE_END));
        }
    }

    protected boolean moveFocus(int direction, boolean updown) {
        TextView tv;
        boolean hasReversed = false;
        boolean isStart = this.mCurrentFocusIndex == -1;
        if (direction == 0) {
            setViewStatusOfFocusedCandidate();
        }
        int size = this.mWnnWordArray.size();
        int index = -1;
        boolean hasChangedLine = false;
        if (direction != DIRECTION_TO_HOME && direction != -999) {
            int start = this.mCurrentFocusIndex == -1 ? 0 : this.mCurrentFocusIndex + direction;
            int i = start;
            while (true) {
                if (i >= 0 && i < size) {
                    TextView view = getCandidateView(i);
                    if (!view.isShown()) {
                        break;
                    }
                    if (updown) {
                        int left = view.getLeft();
                        if (left <= this.mFocusAxisX && this.mFocusAxisX < view.getRight()) {
                            index = i;
                            break;
                        }
                        if (left == 0) {
                            hasChangedLine = true;
                        }
                        i += direction;
                    } else {
                        index = i;
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (direction == DIRECTION_TO_HOME) {
            index = 0;
        } else if (direction == -999) {
            index = size - 1;
        }
        if (index < 0 && hasChangedLine && direction > 0) {
            index = size - 1;
        }
        if (index >= 0) {
            this.mCurrentFocusIndex = index;
            setViewStatusOfFocusedCandidate();
            if (!updown && (tv = getFocusedView()) != null) {
                this.mFocusAxisX = tv.getLeft();
            }
        } else {
            if (direction > 0) {
                this.mCurrentFocusIndex = -1;
            } else {
                this.mCurrentFocusIndex = size;
            }
            hasReversed = true;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(0, direction, 0));
        }
        if (isStart) {
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.FOCUS_CANDIDATE_START));
        }
        return hasReversed;
    }

    public Size getListSize() {
        return this.mListSize;
    }

    public void scrollPageAndUpdateFocus(boolean scrollDown) {
        if (!isFocusCandidate()) {
            moveFocus(1, true);
            return;
        }
        int direction = scrollDown ? 1 : -1;
        int pageSize = this.mViewBodyScroll.getHeight();
        int moveCnt = pageSize / this.mCandidateMinimumHeight;
        this.mHandler.removeMessages(0);
        this.mFocusAxisX = 0;
        TextView view = getFocusedView();
        if (!scrollDown && view != null && view.getLeft() != 0) {
            moveCnt++;
        }
        for (int i = 0; i < moveCnt; i++) {
            int preFocusIndex = this.mCurrentFocusIndex;
            boolean reverse = moveFocus(direction, true);
            if (this.mHandler.hasMessages(0)) {
                this.mHandler.removeMessages(0);
                moveFocus(direction, true);
            }
            if (reverse) {
                if (i > 0) {
                    this.mCurrentFocusIndex = preFocusIndex;
                    setViewStatusOfFocusedCandidate();
                    return;
                }
                return;
            }
        }
    }

    public void selectFocusCandidate() {
    }

    protected void setViewStatusOfFocusedCandidate() {
        View view = this.mFocusedView;
        if (view != null) {
            int paddingL = view.getPaddingLeft();
            int paddingT = view.getPaddingTop();
            int paddingR = view.getPaddingRight();
            int paddingB = view.getPaddingBottom();
            view.setBackgroundDrawable(this.mFocusedViewBackground);
            view.setPadding(paddingL, paddingT, paddingR, paddingB);
            WnnAccessibility.sendAccessibilityEventHoverExit(this.mWnn, view.getContentDescription(), view, view.getId(), null);
        }
        TextView v = getFocusedView();
        this.mFocusedView = v;
        if (v != null) {
            this.mFocusedViewBackground = v.getBackground();
            int paddingL2 = v.getPaddingLeft();
            int paddingT2 = v.getPaddingTop();
            int paddingR2 = v.getPaddingRight();
            int paddingB2 = v.getPaddingBottom();
            setCandidateBackGround(v, R.drawable.cand_back_focuse);
            v.setPadding(paddingL2, paddingT2, paddingR2, paddingB2);
            WnnAccessibility.sendAccessibilityEventHoverEnter(this.mWnn, v.getContentDescription(), v, v.getId(), null);
            int viewBodyTop = getViewTopOnScreen(this.mViewBodyScroll);
            int viewBodyBottom = viewBodyTop + this.mViewBodyScroll.getHeight();
            int focusedViewTop = getViewTopOnScreen(v);
            int focusedViewBottom = focusedViewTop + v.getHeight();
            if (focusedViewBottom > viewBodyBottom) {
                this.mViewBodyScroll.scrollBy(0, focusedViewBottom - viewBodyBottom);
            } else if (focusedViewTop < viewBodyTop) {
                this.mViewBodyScroll.scrollBy(0, focusedViewTop - viewBodyTop);
            }
        }
    }

    protected TextView getFocusedView() {
        if (this.mCurrentFocusIndex == -1 || this.mCurrentFocusIndex >= this.mWnnWordArray.size()) {
            return null;
        }
        TextView view = getCandidateView(this.mCurrentFocusIndex);
        return view;
    }

    private int getViewTopOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1];
    }

    public void setEnableCandidateLongClick(boolean enable) {
        this.mEnableCandidateLongClick = enable;
    }

    public boolean checkDecoEmoji() {
        for (TextView view : this.mAddViewList) {
            Spannable text = new SpannableString(view.getText());
            int ret = EmojiAssist.getInstance().checkTextData(text);
            if (ret >= 2 && this.mWnn.isEmoji()) {
                return true;
            }
        }
        return false;
    }

    public boolean getCanReadMore() {
        return false;
    }

    public int getReadMoreButtonWidth() {
        return 0;
    }

    public boolean isReadMoreButtonPressed() {
        return false;
    }

    public void setScaleFactor(float factor, boolean updateTextSize) {
        Configuration config;
        TextPaint paint;
        Resources res = this.mWnn.getResources();
        if (res != null && (config = res.getConfiguration()) != null) {
            this.mCandidateMinimumWidth = (int) (res.getDimensionPixelSize(R.dimen.cand_minimum_width) / factor);
            this.mCandidateMinimumHeight = (int) (this.mCandidateDefaultMinimumHeight * factor);
            this.mCandEmojiTextSize = (int) (res.getDimensionPixelSize(R.dimen.cand_emoji_text_size) * factor * config.fontScale);
            this.mCandEmojiTextSize = getMaxTextSize(this.mCandEmojiTextSize, this.mCandidateMinimumHeight);
            this.mCandNormalTextSize = (int) (res.getDimensionPixelSize(R.dimen.cand_normal_text_size) * factor * config.fontScale);
            if (this.mCandNormalTextSize > this.mCandEmojiTextSize) {
                this.mCandNormalTextSize = getMaxTextSize(this.mCandNormalTextSize, this.mCandidateMinimumHeight);
            }
            if (updateTextSize && (paint = this.mViewCandidateTemplate.getPaint()) != null) {
                paint.setTextSize(this.mCandNormalTextSize);
            }
        }
    }

    private int getMaxTextSize(int size, int candHeight) {
        Paint paint = new Paint();
        paint.setTextSize(size);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        if (textHeight > candHeight) {
            return (int) ((size * candHeight) / textHeight);
        }
        return size;
    }

    public void onWebApiError() {
    }

    public void addCandidatesWebAPI() {
    }

    public void setViewWidth(int width) {
        this.mViewWidth = width;
    }

    public int setDummyCandidateView(int line) {
        if (this.mWnnWordArray.size() > 0) {
            return this.mWnnWordArray.size();
        }
        int setViewCount = line * this.mCandViewStandardDiv;
        while (this.mWnnWordArray.size() < setViewCount) {
            TextView candView = getCandidateView();
            if (candView == null) {
                return 0;
            }
            candView.setOnTouchListener(this.mDummyCandidateOnTouch);
            candView.setText(this.mDummyWnnWord.candidate);
            setCandidateLayout(this.mDummyWnnWord, candView, null, true);
            this.mWnnWordArray.add(this.mWnnWordArray.size(), this.mDummyWnnWord);
        }
        this.mOccupyCount = 0;
        this.mPrevView = null;
        this.mPrevViewHint = null;
        this.mLineCount = 1;
        this.mWnnWordArray.clear();
        return setViewCount;
    }

    public boolean hasCandidates() {
        return this.mWnnWordArray.size() > 0;
    }
}
