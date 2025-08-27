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

/* loaded from: classes.dex */
public class UrlInputView extends AutoCompleteTextView implements TextWatcher, AdapterView.OnItemClickListener, TextView.OnEditorActionListener, SuggestionsAdapter.CompletionListener {
    private static final boolean DEBUG = Browser.DEBUG;
    private SuggestionsAdapter mAdapter;
    private View mContainer;
    private boolean mIgnore;
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

        @Override // android.view.ActionMode.Callback
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (UrlInputView.this.mState != 0) {
                return true;
            }
            return false;
        }

        @Override // android.view.ActionMode.Callback
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        @Override // android.view.ActionMode.Callback
        public void onDestroyActionMode(ActionMode actionMode) {
        }

        @Override // android.view.ActionMode.Callback
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }
    }

    public UrlInputView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public UrlInputView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.autoCompleteTextViewStyle);
    }

    public UrlInputView(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        this.mInputManager = (InputMethodManager) context.getSystemService("input_method");
        setOnEditorActionListener(this);
        this.mAdapter = new SuggestionsAdapter(context, this);
        setAdapter(this.mAdapter);
        setSelectAllOnFocus(true);
        onConfigurationChanged(context.getResources().getConfiguration());
        setThreshold(1);
        setOnItemClickListener(this);
        this.mNeedsUpdate = false;
        addTextChangedListener(this);
        setDropDownAnchor(R.id.taburlbar);
        this.mState = 0;
        this.mIgnore = false;
    }

    @Override // android.widget.AutoCompleteTextView, android.widget.TextView, android.view.View
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        final int i2;
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onFocusChanged()--->focused = " + z + ", direction = " + i + ", prevRect = " + rect + " mIgnore = " + this.mIgnore);
        }
        super.onFocusChanged(z, i, rect);
        if (z) {
            if (hasSelection()) {
                i2 = 1;
            } else {
                i2 = 2;
            }
            showIME();
        } else {
            i2 = 0;
            hideIME();
        }
        post(new Runnable() { // from class: com.android.browser.UrlInputView.1
            @Override // java.lang.Runnable
            public void run() {
                UrlInputView.this.changeState(i2);
            }
        });
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zHasSelection = hasSelection();
        boolean zHasFocus = hasFocus();
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        if (motionEvent.getActionMasked() == 0 && zHasSelection) {
            postDelayed(new Runnable() { // from class: com.android.browser.UrlInputView.2
                @Override // java.lang.Runnable
                public void run() {
                    UrlInputView.this.changeState(2);
                }
            }, 100L);
        }
        if (!zHasFocus && hasFocus()) {
            selectAll();
        }
        return zOnTouchEvent;
    }

    boolean needsUpdate() {
        return this.mNeedsUpdate;
    }

    void clearNeedsUpdate() {
        this.mNeedsUpdate = false;
    }

    void ignoreIME(boolean z) {
        this.mIgnore = z;
    }

    void setController(UiController uiController) {
        this.mUiController = uiController;
        setCustomSelectionActionModeCallback(new UrlSelectionActionMode(uiController));
        setCustomInsertionActionModeCallback(new UrlInsertActionMode());
    }

    void setContainer(View view) {
        this.mContainer = view;
    }

    public void setUrlInputListener(UrlInputListener urlInputListener) {
        this.mListener = urlInputListener;
    }

    public void setStateListener(StateListener stateListener) {
        this.mStateListener = stateListener;
        changeState(this.mState);
    }

    private void changeState(int i) {
        if (DEBUG) {
            Log.d("browser", "UrlInputView.changeState()--->newState = " + i);
        }
        this.mState = i;
        if (this.mStateListener != null) {
            this.mStateListener.onStateChanged(this.mState);
        }
    }

    int getState() {
        return this.mState;
    }

    @Override // android.widget.TextView, android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mLandscape = (configuration.orientation & 2) != 0;
        this.mAdapter.setLandscapeMode(this.mLandscape);
        if (isPopupShowing() && getVisibility() == 0) {
            dismissDropDown();
            showDropDown();
            performFiltering(getText(), 0);
        }
    }

    @Override // android.widget.AutoCompleteTextView
    public void showDropDown() {
        if (getVisibility() == 8) {
            return;
        }
        super.showDropDown();
    }

    @Override // android.widget.AutoCompleteTextView
    public void dismissDropDown() {
        super.dismissDropDown();
        this.mAdapter.clearCache();
    }

    @Override // android.widget.TextView.OnEditorActionListener
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        finishInput(getText().toString(), null, "browser-type");
        return true;
    }

    void hideIME() {
        this.mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    void showIME() {
        if ((this.mUiController != null && !this.mUiController.getUi().isWebShowing()) || this.mIgnore) {
            return;
        }
        this.mInputManager.restartInput(this);
        this.mInputManager.showSoftInput(this, 0);
    }

    private void finishInput(String str, String str2, String str3) {
        SearchEngine searchEngineInfo;
        if (DEBUG) {
            Log.d("browser", "UrlInputView.finishInput()--->url = " + str + ", extra = " + str2 + ", source = " + str3);
        }
        this.mNeedsUpdate = true;
        dismissDropDown();
        this.mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
        if (TextUtils.isEmpty(str)) {
            this.mListener.onDismiss();
            return;
        }
        if (this.mIncognitoMode && isSearch(str)) {
            com.android.browser.search.SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
            if (searchEngine == null || (searchEngineInfo = SearchEngines.getSearchEngineInfo(this.mContext, searchEngine.getName())) == null) {
                return;
            } else {
                str = searchEngineInfo.getSearchUriForQuery(str);
            }
        }
        this.mListener.onAction(str, str2, str3);
    }

    boolean isSearch(String str) {
        String strTrim = UrlUtils.fixUrl(str).trim();
        return (TextUtils.isEmpty(strTrim) || Patterns.WEB_URL.matcher(strTrim).matches() || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(strTrim).matches()) ? false : true;
    }

    @Override // com.android.browser.SuggestionsAdapter.CompletionListener
    public void onSearch(String str) {
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onSearch()--->search = " + str);
        }
        this.mListener.onCopySuggestion(str);
    }

    @Override // com.android.browser.SuggestionsAdapter.CompletionListener
    public void onSelect(String str, int i, String str2) {
        finishInput(str, str2, "browser-suggest");
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        SuggestionsAdapter.SuggestItem item = this.mAdapter.getItem(i);
        onSelect(SuggestionsAdapter.getSuggestionUrl(item), item.type, item.extra);
    }

    public void setIncognitoMode(boolean z) {
        this.mIncognitoMode = z;
        this.mAdapter.setIncognitoMode(this.mIncognitoMode);
    }

    @Override // android.widget.AutoCompleteTextView, android.widget.TextView, android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 111 && !isInTouchMode()) {
            finishInput(null, null, null);
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    /* JADX DEBUG: Method merged with bridge method: getAdapter()Landroid/widget/ListAdapter; */
    @Override // android.widget.AutoCompleteTextView
    public SuggestionsAdapter getAdapter() {
        return this.mAdapter;
    }

    @Override // android.view.View
    public boolean requestRectangleOnScreen(Rect rect, boolean z) {
        return false;
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.TextView, android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (DEBUG) {
            Log.d("browser", "UrlInputView.onTextChanged()--->new string : " + ((Object) charSequence));
        }
        if (1 == this.mState) {
            changeState(2);
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
    }
}
