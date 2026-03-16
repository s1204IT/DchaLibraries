package jp.co.omronsoft.iwnnime.ml.candidate;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.TextView;
import java.util.ArrayList;
import jp.co.omronsoft.android.text.EmojiDrawable;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.KeyboardResourcesDataManager;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class TextCandidatesListHW extends TextCandidatesList {
    private static final float HW_CAND_VIEW_PADDING = 0.041666668f;
    private static final int NUM_OF_INDEX_BAR = 1;
    private int mCandIndexBarColor = 0;
    private AbsoluteLayout mViewIndexBar = null;
    private Drawable mCandIndexBarBgDrawable = null;
    protected ArrayList<WnnWord> mAllWordArray = new ArrayList<>();
    private int mCurrentPage = 0;
    private int mMaxLineNum = 5;
    private TextView mIndexBarView = null;

    @Override
    public void initView(IWnnIME parent, iWnnEngine converter, int displayWidth) {
        super.initView(parent, converter, displayWidth);
        this.mIndexBarView = createCandidateView();
        this.mIndexBarView.setGravity(17);
        this.mViewIndexBar = (AbsoluteLayout) this.mViewlistBody.findViewById(R.id.index_bar);
        this.mViewIndexBar.addView(this.mIndexBarView);
    }

    @Override
    public void initCandidatesList(int line, boolean isDummy) {
        super.initCandidatesList(line, isDummy);
        this.mAllWordArray.clear();
        this.mCurrentPage = 0;
        this.mMaxTextViewWidth = this.mViewWidth / 4;
    }

    @Override
    public void createCandidatesListView(int maxLine) {
        WnnWord word;
        if (IWnnIME.isPerformanceDebugging()) {
            Log.d("iwnn", "TextCandidatesListHW::createCandidatesListView  Start");
        }
        if (this.mWnn != null) {
            String prevCandidate = null;
            while (this.mAllWordArray.size() < 500 && (word = this.mConverter.getNextCandidate()) != null) {
                if (!word.candidate.equals(prevCandidate)) {
                    prevCandidate = word.candidate;
                    boolean ret = setAttributeWord(word);
                    if (ret) {
                        this.mAllWordArray.add(this.mAllWordArray.size(), word);
                    }
                }
            }
            this.mMaxLineNum = 5;
            displayCurrentPage();
            if (this.mWebApiHasStarted) {
                this.mWebApiHasStarted = false;
                if (this.mPreFocus != -1) {
                    this.mCurrentFocusIndex = this.mPreFocus;
                    setViewStatusOfFocusedCandidate();
                }
            }
            if (IWnnIME.isPerformanceDebugging()) {
                Log.d("iwnn", "TextCandidatesListHW::createCandidatesListView  End");
            }
        }
    }

    @Override
    protected int setCandidateLayout(WnnWord word, TextView candView, TextView hintView, boolean isDummy) {
        if (word == null || candView == null || this.mWnn == null) {
            return 0;
        }
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
        int totalHintSize = (hintLabelWidth + hintLabelPadding) * 2;
        ViewGroup.LayoutParams candParams = buildLayoutParams(this.mViewCandidateList, this.mViewWidth, this.mCandidateMinimumHeight);
        this.mViewCandidateList.updateViewLayout(candView, candParams);
        int maxWidth = this.mViewWidth;
        int hwCandPadding = 0;
        if (this.mWnn.isHwCandWindow()) {
            hwCandPadding = (int) (this.mViewWidth * HW_CAND_VIEW_PADDING);
        }
        int paddingL = hwCandPadding;
        int paddingR = hwCandPadding;
        int totalViewWidth = textViewWidth + totalHintSize + paddingL + paddingR;
        if (totalViewWidth > maxWidth) {
            totalViewWidth = maxWidth;
            candView.setEllipsize(TextUtils.TruncateAt.END);
            candView.setWidth(maxWidth);
            paddingR += hintLabelWidth + hintLabelPadding;
        } else {
            candView.setEllipsize(null);
            candView.setWidth(textViewWidth);
        }
        candView.setPadding(paddingL, 0, paddingR, 0);
        if (hintView != null) {
            ViewGroup.LayoutParams hintParams = buildLayoutParams(this.mViewCandidateHintChar, this.mViewWidth, this.mCandidateMinimumHeight);
            this.mViewCandidateHintChar.updateViewLayout(hintView, hintParams);
            hintView.setPadding(0, 0, hintLabelPadding + hwCandPadding, hintLabelPadding / 2);
        }
        this.mLineCount++;
        return totalViewWidth;
    }

    @Override
    protected void initCandidateView(TextView textView) {
        if (textView != null) {
            super.initCandidateView(textView);
            setCandidateBackGround(textView, R.drawable.cand_back_noline);
            textView.setGravity(19);
            KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
            this.mCandIndexBarColor = resMan.getColor(this.mWnn, R.color.candidate_index_bar_color);
            this.mIndexBarView.setTextColor(this.mCandIndexBarColor);
            this.mCandIndexBarBgDrawable = resMan.getDrawable(this.mWnn, R.drawable.cand_index_bar);
            this.mIndexBarView.setBackgroundDrawable(this.mCandIndexBarBgDrawable);
            this.mIndexBarView.setTextSize(0, this.mCandNormalTextSize);
            this.mIndexBarView.setHeight(this.mCandidateMinimumHeight);
        }
    }

    @Override
    public void scrollPageAndUpdateFocus(boolean scrollDown) {
        if (!isFocusCandidate()) {
            moveFocus(1, true);
        } else {
            int direction = scrollDown ? -998 : 998;
            moveFocus(direction, true);
        }
    }

    @Override
    protected boolean moveFocus(int direction, boolean updown) {
        int index;
        int size;
        boolean isStart = this.mCurrentFocusIndex == -1;
        if (direction == 0) {
            setViewStatusOfFocusedCandidate();
        }
        int prevPageNum = this.mCurrentPage;
        int lastPage = (this.mAllWordArray.size() - 1) / this.mMaxLineNum;
        int size2 = this.mWnnWordArray.size();
        switch (direction) {
            case EmojiDrawable.ANIMATION_INFINITE:
                int size3 = ((this.mAllWordArray.size() - 1) % this.mMaxLineNum) + 1;
                index = size3 - 1;
                this.mCurrentPage = lastPage;
                break;
            case -998:
                index = 0;
                this.mCurrentPage++;
                if (this.mCurrentPage > lastPage) {
                    this.mCurrentPage = 0;
                }
                break;
            case 998:
                index = 0;
                this.mCurrentPage--;
                if (this.mCurrentPage < 0) {
                    this.mCurrentPage = lastPage;
                }
                break;
            case 999:
                index = 0;
                this.mCurrentPage = 0;
                break;
            default:
                index = this.mCurrentFocusIndex + direction;
                if (index < 0) {
                    this.mCurrentPage--;
                    if (this.mCurrentPage < 0) {
                        this.mCurrentPage = lastPage;
                        size = ((this.mAllWordArray.size() - 1) % this.mMaxLineNum) + 1;
                    } else {
                        size = this.mMaxLineNum;
                    }
                    index = size - 1;
                } else if (index >= size2) {
                    index = 0;
                    this.mCurrentPage++;
                    if (this.mCurrentPage > lastPage) {
                        this.mCurrentPage = 0;
                    }
                }
                break;
        }
        if (index >= 0) {
            this.mCurrentFocusIndex = index;
            setViewStatusOfFocusedCandidate();
        }
        if (prevPageNum != this.mCurrentPage) {
            displayCurrentPage();
        } else {
            showIndexBar();
        }
        if (isStart) {
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.FOCUS_CANDIDATE_START));
        }
        return false;
    }

    @Override
    public void setMaxViewHeight(int maxHeight) {
        int prevMaxItemNum = this.mMaxLineNum;
        if (this.mCandidateMinimumHeight > 0) {
            this.mMaxLineNum = (maxHeight / this.mCandidateMinimumHeight) - 1;
        }
        if (this.mMaxLineNum <= 0) {
            this.mMaxLineNum = 1;
        } else if (this.mMaxLineNum > 5) {
            this.mMaxLineNum = 5;
        }
        if (prevMaxItemNum != this.mMaxLineNum) {
            int currentItem = 0;
            if (this.mCurrentFocusIndex != -1) {
                int firstItem = this.mCurrentPage * prevMaxItemNum;
                currentItem = firstItem + this.mCurrentFocusIndex;
            }
            this.mCurrentPage = currentItem / this.mMaxLineNum;
            if (this.mCurrentFocusIndex != -1) {
                this.mCurrentFocusIndex = currentItem - (this.mCurrentPage * this.mMaxLineNum);
            }
            displayCurrentPage();
        }
    }

    public int getNumberOfLine() {
        int firstItem = this.mCurrentPage * this.mMaxLineNum;
        int numberOfLine = this.mMaxLineNum;
        if (firstItem + numberOfLine > this.mAllWordArray.size()) {
            return this.mAllWordArray.size() - firstItem;
        }
        return numberOfLine;
    }

    private void displayCurrentPage() {
        if (this.mAllWordArray.size() > 0) {
            int firstItem = this.mCurrentPage * this.mMaxLineNum;
            int index = this.mCurrentFocusIndex;
            int numberOfLine = getNumberOfLine();
            super.initCandidatesList(numberOfLine, false);
            this.mCurrentFocusIndex = index;
            for (int i = 0; i < numberOfLine && i < this.mAllWordArray.size(); i++) {
                WnnWord word = this.mAllWordArray.get(firstItem + i);
                if (word != null) {
                    setCandidate(word);
                }
            }
            int hintNum = this.mViewCandidateHintChar.getChildCount();
            for (int i2 = 0; i2 < hintNum; i2++) {
                View v = this.mViewCandidateHintChar.getChildAt(i2);
                AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) v.getLayoutParams();
                params.width = this.mMaxTextViewWidth;
                this.mViewCandidateHintChar.updateViewLayout(v, params);
            }
            showIndexBar();
            setViewStatusOfFocusedCandidate();
            this.mListSize = new Size(this.mMaxTextViewWidth, this.mCandidateMinimumHeight * (numberOfLine + 1));
        }
    }

    private void showIndexBar() {
        String number;
        if (this.mAllWordArray.size() > 0) {
            if (this.mCurrentFocusIndex != -1) {
                int firstItem = this.mCurrentPage * this.mMaxLineNum;
                number = String.valueOf(this.mCurrentFocusIndex + firstItem + 1);
            } else {
                number = "-";
            }
            String denom = String.valueOf(this.mAllWordArray.size());
            int paddingNum = denom.length() - number.length();
            StringBuffer padding = new StringBuffer();
            for (int i = 0; i < paddingNum; i++) {
                padding.append(" ");
            }
            String indexLabel = padding.toString() + number + "/" + denom;
            this.mIndexBarView.setText(indexLabel);
            TextView candView = getCandidateView(this.mWnnWordArray.size() - 1);
            if (candView != null) {
                AbsoluteLayout.LayoutParams candParams = (AbsoluteLayout.LayoutParams) candView.getLayoutParams();
                AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) this.mIndexBarView.getLayoutParams();
                params.width = this.mMaxTextViewWidth;
                params.height = candParams.height;
                params.x = candParams.x;
                params.y = candParams.y + candParams.height;
                this.mViewIndexBar.updateViewLayout(this.mIndexBarView, params);
            }
        }
    }

    @Override
    public void setViewType(int type, int height) {
    }

    @Override
    public boolean useReadMoreButton() {
        return false;
    }

    @Override
    public void addCandidatesWebAPI() {
        int size = this.mAllWordArray.size();
        if (size > 0) {
            this.mAllWordArray.remove(size - 1);
        }
        int currentLineNum = this.mMaxLineNum;
        super.addCandidatesWebAPI();
        this.mMaxLineNum = currentLineNum;
        displayCurrentPage();
    }

    @Override
    public int setDummyCandidateView(int line) {
        return 0;
    }
}
