package jp.co.omronsoft.iwnnime.ml.candidate;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.android.text.EmojiDrawable;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnDialog;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.KeyboardResourcesDataManager;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.jajp.DefaultSoftKeyboardJAJP;

public class TextCandidatesList extends CandidatesList {
    private static int INDEX_DOES_NOT_EXIST = -1;
    private ImageView mReadMoreButton;
    private Dialog mDialog = null;
    protected View mViewLongPressDialog = null;
    private TextView mViewCandidateWord = null;
    protected WnnWord mWord = null;
    protected boolean mIsDelayDialogClose = false;
    private TextView mReadMoreButtonBackground = null;
    private int mWidthReadMoreButton = 0;
    private boolean mReadMorePressCancel = false;
    protected boolean mIsFullView = false;
    protected int mPreFocus = -1;
    protected iWnnEngine mConverter = null;
    protected boolean mWebApiHasStarted = false;

    public void initView(IWnnIME parent, iWnnEngine converter, int displayWidth) {
        initView(parent, displayWidth);
        this.mConverter = converter;
        KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
        this.mWebAPIKeyTextColor = resMan.getColor(this.mWnn, R.color.webapi_text_key);
        this.mWebAPICandTextColor = resMan.getColor(this.mWnn, R.color.webapi_text_candidate);
        this.mNoCandidateTextColor = resMan.getColor(this.mWnn, R.color.webapi_text_nocandidate);
        LayoutInflater inflater = this.mWnn.getLayoutInflater();
        this.mViewlistBody = (ViewGroup) inflater.inflate(R.layout.candidates_list_text, (ViewGroup) null);
        this.mViewBodyScroll = (ScrollView) this.mViewlistBody.findViewById(R.id.candview_scroll);
        this.mViewBodyScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DefaultSoftKeyboard keyboard;
                if (TextCandidatesList.this.mWnn != null && (keyboard = TextCandidatesList.this.mWnn.getCurrentDefaultSoftKeyboard()) != null && keyboard.isPopupKeyboard().booleanValue()) {
                    keyboard.closePopupKeyboard();
                    return false;
                }
                return false;
            }
        });
        Drawable skin = resMan.getDrawable("CandidateBlankBackground");
        if (skin != null) {
            this.mViewBodyScroll.setBackgroundDrawable(skin);
        }
        this.mViewCandidateList = (AbsoluteLayout) this.mViewlistBody.findViewById(R.id.candidates_view);
        this.mViewCandidateHintChar = (AbsoluteLayout) this.mViewlistBody.findViewById(R.id.candidates_hint_view);
        this.mCandidateDefaultMinimumHeight = this.mWnn.getResources().getDimensionPixelSize(R.dimen.cand_minimum_height);
        if (useReadMoreButton()) {
            this.mReadMoreButton = (ImageView) this.mViewlistBody.findViewById(R.id.read_more_button);
            this.mReadMoreButton.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
            Drawable button = resMan.getDrawable(this.mWnn, R.drawable.cand_down);
            if (button != null) {
                Bitmap buttonBitmap = ((BitmapDrawable) button).getBitmap();
                this.mWidthReadMoreButton = buttonBitmap.getWidth() + this.mReadMoreButton.getPaddingLeft() + this.mReadMoreButton.getPaddingRight();
                this.mReadMoreButton.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        DefaultSoftKeyboard keyboard;
                        if (event.getPointerId(event.getActionIndex()) > 0) {
                            return true;
                        }
                        if (TextCandidatesList.this.mWnn == null || (keyboard = TextCandidatesList.this.mWnn.getCurrentDefaultSoftKeyboard()) == null || !keyboard.isPopupKeyboard().booleanValue()) {
                            switch (event.getAction()) {
                                case 0:
                                    TextCandidatesList.this.mReadMorePressCancel = false;
                                    break;
                                case 2:
                                    if (event.getX() < 0.0f || event.getY() <= 0.0f || event.getX() > v.getWidth() || event.getY() > v.getHeight()) {
                                        TextCandidatesList.this.mReadMorePressCancel = true;
                                    }
                                    break;
                            }
                            return false;
                        }
                        keyboard.closePopupKeyboard();
                        return true;
                    }
                });
                this.mReadMoreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v.isShown() && !TextCandidatesList.this.mReadMorePressCancel && TextCandidatesList.this.mWnn != null) {
                            TextCandidatesList.this.playSoundAndVibration();
                            WnnAccessibility.announceForAccessibility(TextCandidatesList.this.mWnn, WnnAccessibility.getDescriptionSwitchCandidatesViewState(TextCandidatesList.this.mWnn, TextCandidatesList.this.mIsFullView), v);
                            if (TextCandidatesList.this.mIsFullView) {
                                TextCandidatesList.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_NORMAL));
                            } else {
                                TextCandidatesList.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_FULL));
                            }
                        }
                    }
                });
                this.mReadMoreButtonBackground = createCandidateView();
                setCandidateBackGround(this.mReadMoreButtonBackground, R.drawable.cand_back);
            } else {
                return;
            }
        }
        this.mViewLongPressDialog = inflater.inflate(R.layout.candidate_longpress_dialog, (ViewGroup) null);
        Button longPressDialogButton = (Button) this.mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_select);
        longPressDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextCandidatesList.this.playSoundAndVibration();
                TextCandidatesList.this.clearFocusCandidate();
                TextCandidatesList.this.selectNormalCandidate(TextCandidatesList.this.mWord);
                TextCandidatesList.this.closeDialog();
            }
        });
        Button longPressDialogButton2 = (Button) this.mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_mashup);
        longPressDialogButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextCandidatesList.this.mIsDelayDialogClose = true;
                TextCandidatesList.this.playSoundAndVibration();
                TextCandidatesList.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CALL_MUSHROOM, TextCandidatesList.this.mWord));
            }
        });
        Button longPressDialogButton3 = (Button) this.mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_delete);
        longPressDialogButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextCandidatesList.this.playSoundAndVibration();
                TextCandidatesList.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_NORMAL));
                TextCandidatesList.this.mConverter.deleteWord(TextCandidatesList.this.mWord);
                TextCandidatesList.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.UPDATE_CANDIDATE));
                TextCandidatesList.this.closeDialog();
            }
        });
        createTemplateCandidateView();
    }

    @Override
    public void initCandidatesList(int line, boolean isDummy) {
        clearFocusCandidate();
        super.initCandidatesList(line, isDummy);
        if (useReadMoreButton()) {
            this.mViewCandidateList.removeView(this.mReadMoreButtonBackground);
            this.mReadMoreButtonBackground.setVisibility(0);
            Resources res = this.mWnn.getResources();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, R.drawable.cand_down, options);
            this.mWidthReadMoreButton = (this.mCandidateMinimumHeight * options.outWidth) / options.outHeight;
            this.mReadMoreButton.setLayoutParams(new LinearLayout.LayoutParams(this.mWidthReadMoreButton, this.mCandidateMinimumHeight));
        }
    }

    protected boolean setAttributeWord(WnnWord word) {
        if (word == null || this.mWnn == null) {
            return false;
        }
        boolean ret = true;
        Resources res = this.mWnn.getResources();
        if ((word.attribute & 512) != 0) {
            word.candidate = res.getString(R.string.ti_webapi_button_txt);
        } else if ((word.attribute & 2048) != 0) {
            word.candidate = res.getString(R.string.ti_webapi_no_candidate_txt);
        } else if ((word.attribute & 16384) != 0) {
            word.candidate = res.getString(R.string.ti_webapi_get_again_txt);
        } else if ((word.attribute & 1024) != 0 && LoggingEvents.EXTRA_CALLING_APP_NAME.equals(word.candidate)) {
            ret = false;
        } else {
            DefaultSoftKeyboard keyboard = this.mWnn.getCurrentDefaultSoftKeyboard();
            if ((keyboard instanceof DefaultSoftKeyboardJAJP) && keyboard.getKeyMode() == 3) {
                setHintLabel(word);
            }
        }
        return ret;
    }

    @Override
    public void createCandidatesListView(int maxLine) {
        if (IWnnIME.isPerformanceDebugging()) {
            Log.d("iwnn", "TextCandidatesList::createCandidatesListView  Start");
        }
        if (this.mWnn != null) {
            boolean isCompleted = true;
            String prevCandidate = null;
            int cnt = 0;
            while (true) {
                if (this.mWnnWordArray.size() < 500) {
                    cnt++;
                    if (cnt > 20 && maxLine == NON_LIMITED_DISPLAY_LINE_COUNT && !this.mWebApiHasStarted) {
                        isCompleted = false;
                        break;
                    }
                    WnnWord word = this.mConverter.getNextCandidate();
                    if (word == null) {
                        break;
                    }
                    if (!word.candidate.equals(prevCandidate)) {
                        prevCandidate = word.candidate;
                        boolean ret = setAttributeWord(word);
                        if (ret) {
                            boolean ret2 = setCandidate(word);
                            if (ret2 && maxLine < this.mLineCount) {
                                isCompleted = false;
                                break;
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    break;
                }
            }
            if (!isCompleted) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, NON_LIMITED_DISPLAY_LINE_COUNT, 0), 0L);
            } else if (this.mWebApiHasStarted) {
                this.mWebApiHasStarted = false;
                if (this.mPreFocus != -1) {
                    this.mCurrentFocusIndex = this.mPreFocus;
                    setViewStatusOfFocusedCandidate();
                }
            }
            if (this.mViewCandidateList.indexOfChild(this.mReadMoreButtonBackground) == INDEX_DOES_NOT_EXIST && getCanReadMore() && this.mFirstLineLastView != null) {
                AbsoluteLayout.LayoutParams params = (AbsoluteLayout.LayoutParams) this.mFirstLineLastView.getLayoutParams();
                params.width -= this.mWidthReadMoreButton;
                this.mViewCandidateList.updateViewLayout(this.mFirstLineLastView, params);
                if (this.mFirstLineLastViewHint != null) {
                    AbsoluteLayout.LayoutParams params2 = (AbsoluteLayout.LayoutParams) this.mFirstLineLastViewHint.getLayoutParams();
                    params2.width -= this.mWidthReadMoreButton;
                    this.mViewCandidateHintChar.updateViewLayout(this.mFirstLineLastViewHint, params2);
                }
                this.mReadMoreButtonBackground.setLayoutParams(new AbsoluteLayout.LayoutParams(this.mWidthReadMoreButton, this.mCandidateMinimumHeight, this.mViewWidth - this.mWidthReadMoreButton, 0));
                this.mViewCandidateList.addView(this.mReadMoreButtonBackground);
            }
            setReadMore();
            if (IWnnIME.isPerformanceDebugging()) {
                Log.d("iwnn", "TextCandidatesList::createCandidatesListView  End");
            }
        }
    }

    private int getNumberOfLine() {
        if (!this.mPortrait) {
            int line = this.mLandscapeNumberOfLine;
            return line;
        }
        int line2 = this.mPortraitNumberOfLine;
        return line2;
    }

    @Override
    protected TextView getCandidateView(int index) {
        int readMoreIndex = this.mViewCandidateList.indexOfChild(this.mReadMoreButtonBackground);
        if (readMoreIndex != INDEX_DOES_NOT_EXIST && readMoreIndex <= index) {
            index++;
        }
        return super.getCandidateView(index);
    }

    @Override
    protected int getCandidateDivisionNum() {
        return this.mCandViewStandardDiv;
    }

    @Override
    protected void onClickCandidate(TextView text) {
        DefaultSoftKeyboard keyboard = this.mWnn.getCurrentDefaultSoftKeyboard();
        if (keyboard != null && keyboard.isPopupKeyboard().booleanValue()) {
            keyboard.closePopupKeyboard();
            return;
        }
        playSoundAndVibration();
        int wordcount = text.getId();
        if (wordcount >= 0 && this.mWnnWordArray.size() > wordcount) {
            WnnWord word = this.mWnnWordArray.get(wordcount);
            clearFocusCandidate();
            selectCandidate(word);
        }
    }

    @Override
    protected boolean onLongClickCandidate(TextView text) {
        int wordcount;
        DefaultSoftKeyboard keyboard = this.mWnn.getCurrentDefaultSoftKeyboard();
        if (keyboard != null && keyboard.isPopupKeyboard().booleanValue()) {
            keyboard.closePopupKeyboard();
            return false;
        }
        if (this.mViewLongPressDialog == null || WnnAccessibility.isAccessibility(this.mWnn) || (wordcount = text.getId()) < 0 || this.mWnnWordArray.size() <= wordcount) {
            return false;
        }
        this.mWord = this.mWnnWordArray.get(wordcount);
        clearFocusCandidate();
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CANCEL_WEBAPI));
        displayDialog(text, this.mWord);
        return true;
    }

    private void selectCandidate(WnnWord word) {
        if (word != null) {
            if ((word.attribute & 512) != 0) {
                selectWebApiButton();
            } else if ((word.attribute & 16384) != 0) {
                selectWebApiGetAgainButton();
            } else {
                selectNormalCandidate(word);
            }
        }
    }

    protected void selectNormalCandidate(WnnWord word) {
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_NORMAL));
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.SELECT_CANDIDATE, word));
    }

    @Override
    public void onWebApiError() {
        this.mWebApiButton.setText(this.mWnn.getResources().getString(R.string.ti_webapi_button_txt));
    }

    private void selectWebApiButton() {
        this.mWebApiButton.setText(this.mWnn.getResources().getString(R.string.ti_webapi_connect_txt));
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.SELECT_WEBAPI));
    }

    private void selectWebApiGetAgainButton() {
        this.mWebApiButton.setText(this.mWnn.getResources().getString(R.string.ti_webapi_connect_txt));
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.SELECT_WEBAPI_GET_AGAIN));
    }

    @Override
    public void selectFocusCandidate() {
        if (this.mCurrentFocusIndex != -1) {
            WnnWord word = getFocusedWnnWord();
            if (word == null || (word.attribute & 2048) == 0) {
                selectCandidate(word);
            }
        }
    }

    @Override
    public boolean isDelayDialogClose() {
        return this.mIsDelayDialogClose;
    }

    private void displayDialog(View view, WnnWord word) {
        Bundle bundle;
        if ((view instanceof CandidateTextView) && this.mViewLongPressDialog != null) {
            closeDialog();
            this.mDialog = new IWnnDialog(view.getContext(), R.style.Dialog);
            LinearLayout linearLayout = new LinearLayout(view.getContext());
            linearLayout.setOrientation(1);
            linearLayout.setGravity(17);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
            EmojiAssist emojiIns = EmojiAssist.getInstance();
            if (this.mViewCandidateWord != null) {
                emojiIns.removeView(this.mViewCandidateWord);
                this.mViewCandidateWord = null;
            }
            this.mViewCandidateWord = new TextView(view.getContext());
            word.candidate = " " + word.candidate + " ";
            CharSequence candidate = DecoEmojiUtil.getSpannedCandidate(word);
            if (candidate != null) {
                this.mViewCandidateWord.setText(candidate);
                Resources res = this.mWnn.getResources();
                this.mViewCandidateWord.setTextColor(res.getColor(R.color.candidate_text));
                this.mViewCandidateWord.setGravity(17);
                if (EmojiDrawable.isEmoji(word.candidate) || DecoEmojiUtil.isDecoEmoji(word.candidate)) {
                    this.mViewCandidateWord.setTextSize(1, res.getInteger(R.integer.candidate_longpress_dialog_candidate_emoji_word_size));
                    LinearLayout sublayout = new LinearLayout(view.getContext());
                    sublayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
                    sublayout.setGravity(17);
                    HorizontalScrollView horizonalView = new HorizontalScrollView(view.getContext());
                    horizonalView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
                    horizonalView.addView(this.mViewCandidateWord);
                    sublayout.addView(horizonalView);
                    linearLayout.addView(sublayout);
                } else {
                    this.mViewCandidateWord.setTextSize(1, res.getInteger(R.integer.candidate_longpress_dialog_candidate_word_size));
                    linearLayout.addView(this.mViewCandidateWord);
                }
                EditorInfo editorInfo = this.mWnn.getEditorInfo();
                if (editorInfo != null && (bundle = editorInfo.extras) != null) {
                    boolean allowEmoji = bundle.getBoolean("allowEmoji");
                    boolean allowDecoEmoji = bundle.getBoolean("allowDecoEmoji");
                    if (allowEmoji || allowDecoEmoji) {
                        Bundle extraBundle = this.mViewCandidateWord.getInputExtras(true);
                        extraBundle.putBoolean("allowEmoji", allowEmoji);
                        extraBundle.putBoolean("allowDecoEmoji", allowDecoEmoji);
                        emojiIns.addView(this.mViewCandidateWord, false);
                    }
                }
                word.candidate = word.candidate.substring(1, word.candidate.length() - 1);
                View mashup = this.mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_mashup);
                if (this.mEnableMushroom) {
                    mashup.setVisibility(0);
                } else {
                    mashup.setVisibility(8);
                }
                View delete = this.mViewLongPressDialog.findViewById(R.id.candidate_longpress_dialog_delete);
                if ((word.attribute & 2) != 0) {
                    delete.setVisibility(0);
                } else {
                    delete.setVisibility(8);
                }
                linearLayout.addView(this.mViewLongPressDialog);
                this.mDialog.setContentView(linearLayout);
                ((CandidateTextView) view).displayCandidateDialog(this.mDialog);
            }
        }
    }

    @Override
    public void closeDialog() {
        ViewGroup parent;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
            if (this.mViewLongPressDialog != null && (parent = (ViewGroup) this.mViewLongPressDialog.getParent()) != null) {
                parent.removeView(this.mViewLongPressDialog);
            }
        }
        this.mIsDelayDialogClose = false;
    }

    @Override
    public void setViewType(int type, int height) {
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(-1, height);
        this.mViewBodyScroll.setLayoutParams(params);
        switch (type) {
            case 0:
                ((ScrollView) this.mViewBodyScroll).scrollTo(0, 0);
                this.mIsFullView = false;
                setReadMore();
                break;
            case 1:
                this.mIsFullView = true;
                setReadMore();
                break;
        }
    }

    private void setReadMore() {
        Resources res;
        if (this.mReadMoreButton != null) {
            int visibility = 0;
            if (!this.mIsFullView && !getCanReadMore()) {
                visibility = 8;
            }
            this.mReadMoreButton.setVisibility(visibility);
            this.mReadMoreButtonBackground.setVisibility(visibility);
            if (visibility == 0 && this.mWnn != null && (res = this.mWnn.getResources()) != null) {
                KeyboardResourcesDataManager resMan = KeyboardResourcesDataManager.getInstance();
                int iconResId = this.mIsFullView ? R.drawable.cand_up : R.drawable.cand_down;
                String iconResKey = this.mIsFullView ? "cand_up" : "cand_down";
                this.mReadMoreButton.clearColorFilter();
                Drawable skin = resMan.getDrawable(iconResKey);
                if (skin != null) {
                    this.mReadMoreButton.setImageDrawable(skin);
                    this.mReadMoreButton.setBackgroundColor(0);
                } else {
                    this.mReadMoreButton.setImageDrawable(res.getDrawable(iconResId));
                    int iconColor = resMan.getColor(this.mWnn, R.color.read_more_button_color);
                    this.mReadMoreButton.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                    int bgColor = resMan.getColor(this.mWnn, R.color.read_more_button_background_color);
                    this.mReadMoreButton.setBackgroundColor(bgColor);
                }
                this.mReadMoreButton.setContentDescription(WnnAccessibility.getDescriptionReadMoreButton(this.mWnn, this.mIsFullView));
            }
        }
    }

    protected boolean useReadMoreButton() {
        return true;
    }

    @Override
    public boolean getCanReadMore() {
        return useReadMoreButton() && getNumberOfLine() < this.mLineCount;
    }

    @Override
    public int getReadMoreButtonWidth() {
        if (useReadMoreButton()) {
            return this.mWidthReadMoreButton;
        }
        return 0;
    }

    @Override
    public boolean isReadMoreButtonPressed() {
        if (this.mReadMoreButton == null) {
            return false;
        }
        boolean ret = this.mReadMoreButton.isPressed();
        return ret;
    }

    @Override
    public void addCandidatesWebAPI() {
        if ((this instanceof TextCandidatesListHW) || this.mWnnWordArray.size() - 2 >= 0) {
            this.mPreFocus = this.mCurrentFocusIndex;
            int buttonIndex = this.mWnnWordArray.size() - 1;
            TextView button = getCandidateView(buttonIndex);
            if (button != null) {
                this.mViewCandidateList.removeView(button);
                this.mWnnWordArray.remove(buttonIndex);
                this.mConverter.startWebAPIWords();
                this.mWebApiHasStarted = true;
                createCandidatesListView(NON_LIMITED_DISPLAY_LINE_COUNT);
            }
        }
    }
}
