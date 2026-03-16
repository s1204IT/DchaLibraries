package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.HashMap;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiContract;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.KeyboardResourcesDataManager;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class SymbolCandidatesList extends CandidatesList {
    private CandidatesManager mCandidatesManager;
    private int mCandViewSymbolDiv = 0;
    private int mCandViewEmojiDiv = 0;
    private IWnnSymbolEngine mConverter = null;
    private String[] mSymbolList = null;
    private ArrayList<EmojiAssist.DecoEmojiTextInfo> mDecoList = null;
    private int mCategoryId = 0;
    private int mAddSymbolType = -1;
    private HashMap<String, EmojiAssist.DecoEmojiTextInfo> mAdditionalDecoEmojiInfoMap = new HashMap<>();

    public void initView(IWnnIME parent, IWnnSymbolEngine converter, int displayWidth, CandidatesManager manager, int mode, int category) {
        LayoutInflater inflater;
        initView(parent, displayWidth);
        this.mCandidatesManager = manager;
        this.mSymbolMode = mode;
        this.mCategoryId = category;
        this.mConverter = converter;
        if (this.mWnn != null && (inflater = this.mWnn.getLayoutInflater()) != null) {
            this.mViewlistBody = (ViewGroup) inflater.inflate(R.layout.candidates_list_symbol, (ViewGroup) null);
            this.mViewBodyScroll = (ScrollView) this.mViewlistBody.findViewById(R.id.candview_scroll);
            this.mViewCandidateList = (AbsoluteLayout) this.mViewlistBody.findViewById(R.id.candidates_view);
            this.mViewCandidateHintChar = (AbsoluteLayout) this.mViewlistBody.findViewById(R.id.candidates_hint_view);
            this.mViewCandidateNothing = (TextView) this.mViewlistBody.findViewById(R.id.candidates_are_nothing);
            this.mViewCandidateNoHistory = (TextView) this.mViewlistBody.findViewById(R.id.candidates_are_no_history);
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            if (resMan != null) {
                Drawable skin = resMan.getDrawable("CandidateBlankBackground");
                if (skin != null) {
                    this.mViewBodyScroll.setBackgroundDrawable(skin);
                }
                Resources res = this.mWnn.getResources();
                if (res != null) {
                    this.mCandViewSymbolDiv = res.getInteger(R.integer.cand_view_symbol_div);
                    this.mCandViewEmojiDiv = res.getInteger(R.integer.cand_view_emoji_div);
                    this.mCandidateDefaultMinimumHeight = res.getDimensionPixelSize(R.dimen.cand_symbol_minimum_height);
                    this.mTextColor = resMan.getColor(this.mWnn, R.color.candidate_text_symbol);
                    this.mViewCandidateNothing.setTextColor(this.mTextColor);
                    this.mViewCandidateNoHistory.setTextColor(this.mTextColor);
                    createTemplateCandidateView();
                }
            }
        }
    }

    public void updateCandidateList() {
        if (this.mConverter != null) {
            this.mConverter.loadSymbolItem(this.mCategoryId);
            this.mSymbolList = this.mConverter.getCandidatesList();
            this.mDecoList = this.mConverter.getDecoEmojiTextInfoList();
            this.mAddSymbolType = this.mConverter.getAdditionalSymbolType();
            this.mAdditionalDecoEmojiInfoMap = this.mConverter.getAdditionalDecoEmojiInfoMap();
        }
    }

    @Override
    public void createCandidatesListView(int maxLine) {
        WnnWord word;
        if (IWnnIME.isPerformanceDebugging()) {
            Log.d("iwnn", "SymbolCandidatesList::createCandidatesListView  Start");
        }
        boolean isCompleted = true;
        int i = 0;
        while (true) {
            if (i >= 500 || (word = getNextCandidate()) == null) {
                break;
            }
            boolean ret = setCandidate(word);
            if (!ret || maxLine >= this.mLineCount) {
                i++;
            } else {
                isCompleted = false;
                break;
            }
        }
        if (!isCompleted) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, this.mLineCount + 2, 0), 100L);
        } else if (this.mWnnWordArray.size() == 0) {
            if (this.mCandidatesManager.hasCategories() && this.mCategoryId == 0) {
                this.mViewCandidateNoHistory.setVisibility(0);
            } else {
                this.mViewCandidateNothing.setVisibility(0);
            }
        } else {
            this.mViewCandidateNoHistory.setVisibility(8);
            this.mViewCandidateNothing.setVisibility(8);
        }
        if (IWnnIME.isPerformanceDebugging()) {
            Log.d("iwnn", "SymbolCandidatesList::createCandidatesListView  End");
        }
    }

    private WnnWord getNextCandidate() {
        int size;
        int index;
        String candidate;
        if (this.mSymbolList == null || (size = this.mSymbolList.length) < 1 || size <= (index = this.mWnnWordArray.size()) || (candidate = this.mSymbolList[index]) == null) {
            return null;
        }
        int attribute = 16;
        if (this.mCategoryId == 0) {
            attribute = 16 | 1;
        }
        if (this.mSymbolMode == 6 || this.mAddSymbolType == 2) {
            attribute |= iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI;
        }
        EmojiAssist.DecoEmojiTextInfo info = null;
        if (this.mDecoList != null && (attribute & iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI) != 0) {
            info = this.mDecoList.get(index);
        }
        if (this.mAdditionalDecoEmojiInfoMap != null && this.mAddSymbolType == 2) {
            info = this.mAdditionalDecoEmojiInfoMap.get(candidate);
        }
        WnnWord word = new WnnWord(index, candidate, candidate, LoggingEvents.EXTRA_CALLING_APP_NAME, attribute, 0, info);
        word.setSymbolMode(this.mSymbolMode);
        return word;
    }

    @Override
    protected boolean setCandidate(WnnWord word) {
        if (this.mSymbolMode == 7 && (word.attribute & iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI) != 0 && (word.attribute & 1) != 0) {
            if (word.decoEmojiInfo == null) {
                return false;
            }
            int emojiKind = word.decoEmojiInfo.getKind();
            int emojiType = IWnnIME.getEmojiType();
            if (emojiType <= 0) {
                emojiType = 1;
            }
            int convEmojiType = DecoEmojiContract.convertEmojiType(emojiKind);
            if ((convEmojiType & emojiType) == 0) {
                return false;
            }
        }
        if (this.mSymbolMode == 1 || this.mSymbolMode == 7) {
            setHintLabel(word);
        }
        return super.setCandidate(word);
    }

    @Override
    protected int getCandidateDivisionNum() {
        int divisionNum = this.mCandViewStandardDiv;
        switch (this.mSymbolMode) {
            case 1:
            case 2:
            case 7:
                int divisionNum2 = this.mCandViewSymbolDiv;
                return divisionNum2;
            case 3:
            case 6:
                int divisionNum3 = this.mCandViewEmojiDiv;
                return divisionNum3;
            case 4:
            case 5:
            default:
                return divisionNum;
        }
    }

    @Override
    protected void onClickCandidate(TextView text) {
        playSoundAndVibration();
        int wordcount = text.getId();
        if (wordcount >= 0 && this.mWnnWordArray.size() > wordcount) {
            WnnWord word = this.mWnnWordArray.get(wordcount);
            clearFocusCandidate();
            selectCandidate(word);
        }
    }

    @Override
    public boolean onLongClickCandidate(TextView text) {
        int wordcount;
        if (this.mSymbolMode != 7 || this.mConverter == null || WnnAccessibility.isAccessibility(this.mWnn) || (wordcount = text.getId()) < 0 || this.mWnnWordArray.size() <= wordcount) {
            return false;
        }
        WnnWord word = this.mWnnWordArray.get(wordcount);
        return this.mConverter.startLongPressActionAdditionalSymbol(word);
    }

    private void selectCandidate(WnnWord word) {
        if (this.mWnn != null) {
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.SELECT_CANDIDATE, word));
            this.mCandidatesManager.updateHistorySymbolList();
        }
    }

    @Override
    public void selectFocusCandidate() {
        WnnWord word;
        if (this.mCurrentFocusIndex != -1 && (word = getFocusedWnnWord()) != null) {
            selectCandidate(word);
        }
    }

    @Override
    public void setViewType(int type, int height) {
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(-1, height);
        this.mViewBodyScroll.setLayoutParams(params);
    }

    @Override
    public boolean hasCandidates() {
        return true;
    }
}
