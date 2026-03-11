package com.android.browser;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import com.android.browser.SuggestionsAdapter;
import com.android.browser.search.SearchEngines;
import com.mediatek.common.search.SearchEngine;

public class UrlInputView extends AutoCompleteTextView implements TextView.OnEditorActionListener, SuggestionsAdapter.CompletionListener, AdapterView.OnItemClickListener, TextWatcher {
    private static final boolean DEBUG = Browser.DEBUG;
    private SuggestionsAdapter mAdapter;
    private View mContainer;
    private boolean mIncognitoMode;
    private InputMethodManager mInputManager;
    private boolean mLandscape;
    private UrlInputListener mListener;
    private boolean mNeedsUpdate;
    private int mState;
    private StateListener mStateListener;
    private UiController mUiController;

    interface StateListener {
        void onStateChanged(int i);
    }

    interface UrlInputListener {
        void onAction(String str, String str2, String str3);

        void onCopySuggestion(String str);

        void onDismiss();
    }

    private class UrlInsertActionMode implements ActionMode.Callback {
        public UrlInsertActionMode() {
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return UrlInputView.this.mState != 0;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
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
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onFocusChanged()--->focused = " + focused + ", direction = " + direction + ", prevRect = " + prevRect);
        }
        super.onFocusChanged(focused, direction, prevRect);
        if (focused) {
            if (hasSelection()) {
                state = 1;
            } else {
                state = 2;
            }
            showIME();
        } else {
            state = 0;
            hideIME();
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
        boolean hasFocus = hasFocus();
        boolean res = super.onTouchEvent(evt);
        if (evt.getActionMasked() == 0 && hasSelection) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    UrlInputView.this.changeState(2);
                }
            }, 100L);
        }
        if (!hasFocus && hasFocus()) {
            selectAll();
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
        this.mUiController = controller;
        UrlSelectionActionMode urlSelectionMode = new UrlSelectionActionMode(controller);
        setCustomSelectionActionModeCallback(urlSelectionMode);
        UrlInsertActionMode urlInsertMode = new UrlInsertActionMode();
        setCustomInsertionActionModeCallback(urlInsertMode);
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
        if (DEBUG) {
            Log.d("browser", "UrlInputView.changeState()--->newState = " + newState);
        }
        this.mState = newState;
        if (this.mStateListener == null) {
            return;
        }
        this.mStateListener.onStateChanged(this.mState);
    }

    int getState() {
        return this.mState;
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        this.mLandscape = (config.orientation & 2) != 0;
        this.mAdapter.setLandscapeMode(this.mLandscape);
        if (!isPopupShowing() || getVisibility() != 0) {
            return;
        }
        dismissDropDown();
        showDropDown();
        performFiltering(getText(), 0);
    }

    @Override
    public void showDropDown() {
        if (getVisibility() == 8) {
            return;
        }
        super.showDropDown();
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
        if (this.mUiController != null && !this.mUiController.getUi().isWebShowing()) {
            return;
        }
        this.mInputManager.restartInput(this);
        this.mInputManager.showSoftInput(this, 0);
    }

    private void finishInput(String url, String extra, String source) {
        SearchEngine engineInfo;
        if (DEBUG) {
            Log.d("browser", "UrlInputView.finishInput()--->url = " + url + ", extra = " + extra + ", source = " + source);
        }
        this.mNeedsUpdate = true;
        dismissDropDown();
        this.mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
        if (TextUtils.isEmpty(url)) {
            this.mListener.onDismiss();
            return;
        }
        if (this.mIncognitoMode && isSearch(url)) {
            com.android.browser.search.SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
            if (searchEngine == null || (engineInfo = SearchEngines.getSearchEngineInfo(this.mContext, searchEngine.getName())) == null) {
                return;
            } else {
                url = engineInfo.getSearchUriForQuery(url);
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
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onSearch()--->search = " + search);
        }
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
        if (keyCode == 111 && !isInTouchMode()) {
            finishInput(null, null, null);
            return true;
        }
        return super.onKeyDown(keyCode, evt);
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
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onTextChanged()--->new string : " + s);
        }
        if (1 != this.mState) {
            return;
        }
        changeState(2);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
