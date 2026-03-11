package com.android.browser;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import com.android.browser.SuggestionsAdapter;
import com.android.browser.search.SearchEngine;
import com.android.browser.search.SearchEngineInfo;
import com.android.browser.search.SearchEngines;

public class UrlInputView extends AutoCompleteTextView implements TextWatcher, AdapterView.OnItemClickListener, TextView.OnEditorActionListener, SuggestionsAdapter.CompletionListener {
    private SuggestionsAdapter mAdapter;
    private View mContainer;
    private boolean mIncognitoMode;
    private InputMethodManager mInputManager;
    private boolean mLandscape;
    private UrlInputListener mListener;
    private boolean mNeedsUpdate;
    private int mState;
    private StateListener mStateListener;

    interface StateListener {
        void onStateChanged(int i);
    }

    interface UrlInputListener {
        void onAction(String str, String str2, String str3);

        void onCopySuggestion(String str);

        void onDismiss();
    }

    public UrlInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public UrlInputView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public UrlInputView(Context context) {
        this(context, null);
    }

    private void init(Context ctx) {
        this.mInputManager = (InputMethodManager) ctx.getSystemService("input_method");
        setOnEditorActionListener(this);
        this.mAdapter = new SuggestionsAdapter(ctx, this);
        setAdapter(this.mAdapter);
        setSelectAllOnFocus(true);
        onConfigurationChanged(ctx.getResources().getConfiguration());
        setThreshold(1);
        setOnItemClickListener(this);
        this.mNeedsUpdate = false;
        addTextChangedListener(this);
        setDropDownAnchor(R.id.taburlbar);
        this.mState = 0;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect prevRect) {
        int state;
        super.onFocusChanged(focused, direction, prevRect);
        if (focused) {
            if (hasSelection()) {
                state = 1;
            } else {
                state = 2;
            }
        } else {
            state = 0;
        }
        final int s = state;
        post(new Runnable() {
            @Override
            public void run() {
                UrlInputView.this.changeState(s);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        boolean hasSelection = hasSelection();
        boolean res = super.onTouchEvent(evt);
        if (evt.getActionMasked() == 0 && hasSelection) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    UrlInputView.this.changeState(2);
                }
            }, 100L);
        }
        return res;
    }

    boolean needsUpdate() {
        return this.mNeedsUpdate;
    }

    void clearNeedsUpdate() {
        this.mNeedsUpdate = false;
    }

    void setController(UiController controller) {
        UrlSelectionActionMode urlSelectionMode = new UrlSelectionActionMode(controller);
        setCustomSelectionActionModeCallback(urlSelectionMode);
    }

    void setContainer(View container) {
        this.mContainer = container;
    }

    public void setUrlInputListener(UrlInputListener listener) {
        this.mListener = listener;
    }

    public void setStateListener(StateListener listener) {
        this.mStateListener = listener;
        changeState(this.mState);
    }

    public void changeState(int newState) {
        this.mState = newState;
        if (this.mStateListener != null) {
            this.mStateListener.onStateChanged(this.mState);
        }
    }

    int getState() {
        return this.mState;
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        this.mLandscape = (config.orientation & 2) != 0;
        this.mAdapter.setLandscapeMode(this.mLandscape);
        if (isPopupShowing() && getVisibility() == 0) {
            dismissDropDown();
            showDropDown();
            performFiltering(getText(), 0);
        }
    }

    @Override
    public void dismissDropDown() {
        super.dismissDropDown();
        this.mAdapter.clearCache();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        finishInput(getText().toString(), null, "browser-type");
        return true;
    }

    void hideIME() {
        this.mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    void showIME() {
        this.mInputManager.focusIn(this);
        this.mInputManager.showSoftInput(this, 0);
    }

    private void finishInput(String url, String extra, String source) {
        SearchEngineInfo engineInfo;
        this.mNeedsUpdate = true;
        dismissDropDown();
        this.mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
        if (TextUtils.isEmpty(url)) {
            this.mListener.onDismiss();
            return;
        }
        if (this.mIncognitoMode && isSearch(url)) {
            SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
            if (searchEngine != null && (engineInfo = SearchEngines.getSearchEngineInfo(this.mContext, searchEngine.getName())) != null) {
                url = engineInfo.getSearchUriForQuery(url);
            } else {
                return;
            }
        }
        this.mListener.onAction(url, extra, source);
    }

    boolean isSearch(String inUrl) {
        String url = UrlUtils.fixUrl(inUrl).trim();
        return (TextUtils.isEmpty(url) || Patterns.WEB_URL.matcher(url).matches() || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) ? false : true;
    }

    @Override
    public void onSearch(String search) {
        this.mListener.onCopySuggestion(search);
    }

    @Override
    public void onSelect(String url, int type, String extra) {
        finishInput(url, extra, "browser-suggest");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SuggestionsAdapter.SuggestItem item = this.mAdapter.getItem(position);
        onSelect(SuggestionsAdapter.getSuggestionUrl(item), item.type, item.extra);
    }

    public void setIncognitoMode(boolean incognito) {
        this.mIncognitoMode = incognito;
        this.mAdapter.setIncognitoMode(this.mIncognitoMode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode != 111 || isInTouchMode()) {
            return super.onKeyDown(keyCode, evt);
        }
        finishInput(null, null, null);
        return true;
    }

    @Override
    public SuggestionsAdapter getAdapter() {
        return this.mAdapter;
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rect, boolean immediate) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (1 == this.mState) {
            changeState(2);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
