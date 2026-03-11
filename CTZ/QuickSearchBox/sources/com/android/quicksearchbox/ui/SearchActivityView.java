package com.android.quicksearchbox.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.SearchActivity;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.Suggestions;
import com.android.quicksearchbox.VoiceSearch;
import com.android.quicksearchbox.ui.QueryTextView;
import java.util.ArrayList;

public abstract class SearchActivityView extends RelativeLayout {
    protected ButtonsKeyListener mButtonsKeyListener;
    protected View.OnClickListener mExitClickListener;
    private QueryListener mQueryListener;
    protected Drawable mQueryTextEmptyBg;
    protected QueryTextView mQueryTextView;
    protected boolean mQueryWasEmpty;
    private SearchClickListener mSearchClickListener;
    protected ImageButton mSearchGoButton;
    protected SuggestionsAdapter<ListAdapter> mSuggestionsAdapter;
    protected SuggestionsListView<ListAdapter> mSuggestionsView;
    private boolean mUpdateSuggestions;
    protected ImageButton mVoiceSearchButton;

    private class ButtonsKeyListener implements View.OnKeyListener {
        final SearchActivityView this$0;

        private ButtonsKeyListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            return this.this$0.forwardKeyToQueryTextView(i, keyEvent);
        }
    }

    private class InputMethodCloser implements AbsListView.OnScrollListener {
        final SearchActivityView this$0;

        private InputMethodCloser(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
            this.this$0.considerHidingInputMethod();
        }
    }

    public interface QueryListener {
        void onQueryChanged();
    }

    private class QueryTextEditorActionListener implements TextView.OnEditorActionListener {
        final SearchActivityView this$0;

        private QueryTextEditorActionListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (keyEvent != null) {
                if (keyEvent.getAction() == 1) {
                    return this.this$0.onSearchClicked(1);
                }
                if (keyEvent.getAction() == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private class QueryTextViewFocusListener implements View.OnFocusChangeListener {
        final SearchActivityView this$0;

        private QueryTextViewFocusListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void onFocusChange(View view, boolean z) {
            if (z) {
                this.this$0.showInputMethodForQuery();
            }
        }
    }

    public interface SearchClickListener {
        boolean onSearchClicked(int i);
    }

    private class SearchGoButtonClickListener implements View.OnClickListener {
        final SearchActivityView this$0;

        private SearchGoButtonClickListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void onClick(View view) {
            this.this$0.onSearchClicked(0);
        }
    }

    private class SearchTextWatcher implements TextWatcher {
        final SearchActivityView this$0;

        private SearchTextWatcher(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            boolean z = editable.length() == 0;
            if (z != this.this$0.mQueryWasEmpty) {
                this.this$0.mQueryWasEmpty = z;
                this.this$0.updateUi(z);
            }
            if (!this.this$0.mUpdateSuggestions || this.this$0.mQueryListener == null) {
                return;
            }
            this.this$0.mQueryListener.onQueryChanged();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    private class SuggestListFocusListener implements View.OnFocusChangeListener {
        final SearchActivityView this$0;

        private SuggestListFocusListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void onFocusChange(View view, boolean z) {
            if (z) {
                this.this$0.considerHidingInputMethod();
            }
        }
    }

    protected class SuggestionsObserver extends DataSetObserver {
        final SearchActivityView this$0;

        protected SuggestionsObserver(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public void onChanged() {
            this.this$0.onSuggestionsChanged();
        }
    }

    protected class SuggestionsViewKeyListener implements View.OnKeyListener {
        final SearchActivityView this$0;

        protected SuggestionsViewKeyListener(SearchActivityView searchActivityView) {
            this.this$0 = searchActivityView;
        }

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 0 && (view instanceof SuggestionsListView)) {
                SuggestionsListView suggestionsListView = (SuggestionsListView) view;
                if (this.this$0.onSuggestionKeyDown(suggestionsListView.getSuggestionsAdapter(), suggestionsListView.getSelectedItemId(), i, keyEvent)) {
                    return true;
                }
            }
            return this.this$0.forwardKeyToQueryTextView(i, keyEvent);
        }
    }

    public SearchActivityView(Context context) {
        super(context);
        this.mQueryWasEmpty = true;
    }

    public SearchActivityView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mQueryWasEmpty = true;
    }

    public SearchActivityView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mQueryWasEmpty = true;
    }

    public boolean forwardKeyToQueryTextView(int i, KeyEvent keyEvent) {
        if (!keyEvent.isSystem() && shouldForwardToQueryTextView(i) && this.mQueryTextView.requestFocus()) {
            return this.mQueryTextView.dispatchKeyEvent(keyEvent);
        }
        return false;
    }

    private boolean shouldForwardToQueryTextView(int i) {
        if (i != 66 && i != 84) {
            switch (i) {
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    break;
                default:
                    return true;
            }
        }
        return false;
    }

    private void updateSearchGoButton(boolean z) {
        if (z) {
            this.mSearchGoButton.setVisibility(8);
        } else {
            this.mSearchGoButton.setVisibility(0);
        }
    }

    private CompletionInfo[] webSuggestionsToCompletions(Suggestions suggestions) {
        SourceResult webResult = suggestions.getWebResult();
        if (webResult == null) {
            return null;
        }
        int count = webResult.getCount();
        ArrayList arrayList = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            webResult.moveTo(i);
            arrayList.add(new CompletionInfo(i, i, webResult.getSuggestionText1()));
        }
        return (CompletionInfo[]) arrayList.toArray(new CompletionInfo[arrayList.size()]);
    }

    public void clearSuggestions() {
        this.mSuggestionsAdapter.setSuggestions(null);
    }

    public abstract void considerHidingInputMethod();

    protected SuggestionsAdapter<ListAdapter> createSuggestionsAdapter() {
        return new DelayingSuggestionsAdapter(new SuggestionsListAdapter(getQsbApplication().getSuggestionViewFactory()));
    }

    public void destroy() {
        this.mSuggestionsView.setSuggestionsAdapter(null);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        KeyEvent.DispatcherState keyDispatcherState;
        SearchActivity activity = getActivity();
        if (activity != null && keyEvent.getKeyCode() == 4 && isQueryEmpty() && (keyDispatcherState = getKeyDispatcherState()) != null) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                keyDispatcherState.startTracking(keyEvent, this);
                return true;
            }
            if (keyEvent.getAction() == 1 && !keyEvent.isCanceled() && keyDispatcherState.isTracking(keyEvent)) {
                hideInputMethod();
                activity.onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEventPreIme(keyEvent);
    }

    public void focusQueryTextView() {
        this.mQueryTextView.requestFocus();
    }

    protected SearchActivity getActivity() {
        Context context = getContext();
        if (context instanceof SearchActivity) {
            return (SearchActivity) context;
        }
        return null;
    }

    protected QsbApplication getQsbApplication() {
        return QsbApplication.get(getContext());
    }

    public String getQuery() {
        Editable text = this.mQueryTextView.getText();
        return text == null ? "" : text.toString();
    }

    public Suggestions getSuggestions() {
        return this.mSuggestionsAdapter.getSuggestions();
    }

    protected VoiceSearch getVoiceSearch() {
        return getQsbApplication().getVoiceSearch();
    }

    protected Drawable getVoiceSearchIcon() {
        return getResources().getDrawable(2130837545);
    }

    protected void hideInputMethod() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public boolean isQueryEmpty() {
        return TextUtils.isEmpty(getQuery());
    }

    public void limitResultsToViewHeight() {
    }

    @Override
    protected void onFinishInflate() {
        this.mQueryTextView = (QueryTextView) findViewById(2131689488);
        this.mSuggestionsView = (SuggestionsView) findViewById(2131689486);
        this.mSuggestionsView.setOnScrollListener(new InputMethodCloser());
        this.mSuggestionsView.setOnKeyListener(new SuggestionsViewKeyListener(this));
        this.mSuggestionsView.setOnFocusChangeListener(new SuggestListFocusListener());
        this.mSuggestionsAdapter = createSuggestionsAdapter();
        this.mSuggestionsAdapter.setOnFocusChangeListener(new SuggestListFocusListener());
        this.mSearchGoButton = (ImageButton) findViewById(2131689489);
        this.mVoiceSearchButton = (ImageButton) findViewById(2131689490);
        this.mVoiceSearchButton.setImageDrawable(getVoiceSearchIcon());
        this.mQueryTextView.addTextChangedListener(new SearchTextWatcher());
        this.mQueryTextView.setOnEditorActionListener(new QueryTextEditorActionListener());
        this.mQueryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());
        this.mQueryTextEmptyBg = this.mQueryTextView.getBackground();
        this.mSearchGoButton.setOnClickListener(new SearchGoButtonClickListener());
        this.mButtonsKeyListener = new ButtonsKeyListener();
        this.mSearchGoButton.setOnKeyListener(this.mButtonsKeyListener);
        this.mVoiceSearchButton.setOnKeyListener(this.mButtonsKeyListener);
        this.mUpdateSuggestions = true;
    }

    public void onPause() {
    }

    public abstract void onResume();

    protected boolean onSearchClicked(int i) {
        if (this.mSearchClickListener != null) {
            return this.mSearchClickListener.onSearchClicked(i);
        }
        return false;
    }

    public abstract void onStop();

    protected boolean onSuggestionKeyDown(SuggestionsAdapter<?> suggestionsAdapter, long j, int i, KeyEvent keyEvent) {
        if ((i != 66 && i != 84 && i != 23) || suggestionsAdapter == null) {
            return false;
        }
        suggestionsAdapter.onSuggestionClicked(j);
        return true;
    }

    protected void onSuggestionsChanged() {
        updateInputMethodSuggestions();
    }

    public void setExitClickListener(View.OnClickListener onClickListener) {
        this.mExitClickListener = onClickListener;
    }

    public void setMaxPromotedResults(int i) {
    }

    public void setQuery(String str, boolean z) {
        this.mUpdateSuggestions = false;
        this.mQueryTextView.setText(str);
        this.mQueryTextView.setTextSelection(z);
        this.mUpdateSuggestions = true;
    }

    public void setQueryListener(QueryListener queryListener) {
        this.mQueryListener = queryListener;
    }

    public void setSearchClickListener(SearchClickListener searchClickListener) {
        this.mSearchClickListener = searchClickListener;
    }

    public void setSuggestionClickListener(SuggestionClickListener suggestionClickListener) {
        this.mSuggestionsAdapter.setSuggestionClickListener(suggestionClickListener);
        this.mQueryTextView.setCommitCompletionListener(new QueryTextView.CommitCompletionListener(this) {
            final SearchActivityView this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onCommitCompletion(int i) {
                this.this$0.mSuggestionsAdapter.onSuggestionClicked(i);
            }
        });
    }

    public void setSuggestions(Suggestions suggestions) {
        suggestions.acquire();
        this.mSuggestionsAdapter.setSuggestions(suggestions);
    }

    public void setVoiceSearchButtonClickListener(View.OnClickListener onClickListener) {
        if (this.mVoiceSearchButton != null) {
            this.mVoiceSearchButton.setOnClickListener(onClickListener);
        }
    }

    protected boolean shouldShowVoiceSearch(boolean z) {
        return z;
    }

    public void showInputMethodForQuery() {
        this.mQueryTextView.showInputMethod();
    }

    public void start() {
        this.mSuggestionsAdapter.getListAdapter().registerDataSetObserver(new SuggestionsObserver(this));
        this.mSuggestionsView.setSuggestionsAdapter(this.mSuggestionsAdapter);
    }

    protected void updateInputMethodSuggestions() {
        Suggestions suggestions;
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (inputMethodManager == null || !inputMethodManager.isFullscreenMode() || (suggestions = this.mSuggestionsAdapter.getSuggestions()) == null) {
            return;
        }
        inputMethodManager.displayCompletions(this.mQueryTextView, webSuggestionsToCompletions(suggestions));
    }

    protected void updateQueryTextView(boolean z) {
        if (!z) {
            this.mQueryTextView.setBackgroundResource(2130837585);
        } else {
            this.mQueryTextView.setBackgroundDrawable(this.mQueryTextEmptyBg);
            this.mQueryTextView.setHint((CharSequence) null);
        }
    }

    protected void updateUi(boolean z) {
        updateQueryTextView(z);
        updateSearchGoButton(z);
        updateVoiceSearchButton(z);
    }

    protected void updateVoiceSearchButton(boolean z) {
        if (shouldShowVoiceSearch(z) && getVoiceSearch().shouldShowVoiceSearch()) {
            this.mVoiceSearchButton.setVisibility(0);
            this.mQueryTextView.setPrivateImeOptions("nm");
        } else {
            this.mVoiceSearchButton.setVisibility(8);
            this.mQueryTextView.setPrivateImeOptions(null);
        }
    }
}
