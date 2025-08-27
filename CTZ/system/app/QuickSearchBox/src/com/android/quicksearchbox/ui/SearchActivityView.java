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
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchActivity;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.Suggestions;
import com.android.quicksearchbox.VoiceSearch;
import com.android.quicksearchbox.ui.QueryTextView;
import java.util.ArrayList;

/* loaded from: classes.dex */
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

    public interface QueryListener {
        void onQueryChanged();
    }

    public interface SearchClickListener {
        boolean onSearchClicked(int i);
    }

    public abstract void considerHidingInputMethod();

    public abstract void onResume();

    public abstract void onStop();

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

    @Override // android.view.View
    protected void onFinishInflate() {
        this.mQueryTextView = (QueryTextView) findViewById(R.id.search_src_text);
        this.mSuggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        this.mSuggestionsView.setOnScrollListener(new InputMethodCloser());
        this.mSuggestionsView.setOnKeyListener(new SuggestionsViewKeyListener());
        this.mSuggestionsView.setOnFocusChangeListener(new SuggestListFocusListener());
        this.mSuggestionsAdapter = createSuggestionsAdapter();
        this.mSuggestionsAdapter.setOnFocusChangeListener(new SuggestListFocusListener());
        this.mSearchGoButton = (ImageButton) findViewById(R.id.search_go_btn);
        this.mVoiceSearchButton = (ImageButton) findViewById(R.id.search_voice_btn);
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

    public void start() {
        this.mSuggestionsAdapter.getListAdapter().registerDataSetObserver(new SuggestionsObserver());
        this.mSuggestionsView.setSuggestionsAdapter(this.mSuggestionsAdapter);
    }

    public void destroy() {
        this.mSuggestionsView.setSuggestionsAdapter(null);
    }

    protected QsbApplication getQsbApplication() {
        return QsbApplication.get(getContext());
    }

    protected Drawable getVoiceSearchIcon() {
        return getResources().getDrawable(R.drawable.ic_btn_speak_now);
    }

    protected VoiceSearch getVoiceSearch() {
        return getQsbApplication().getVoiceSearch();
    }

    protected SuggestionsAdapter<ListAdapter> createSuggestionsAdapter() {
        return new DelayingSuggestionsAdapter(new SuggestionsListAdapter(getQsbApplication().getSuggestionViewFactory()));
    }

    public void setMaxPromotedResults(int i) {
    }

    public void limitResultsToViewHeight() {
    }

    public void setQueryListener(QueryListener queryListener) {
        this.mQueryListener = queryListener;
    }

    public void setSearchClickListener(SearchClickListener searchClickListener) {
        this.mSearchClickListener = searchClickListener;
    }

    public void setVoiceSearchButtonClickListener(View.OnClickListener onClickListener) {
        if (this.mVoiceSearchButton != null) {
            this.mVoiceSearchButton.setOnClickListener(onClickListener);
        }
    }

    public void setSuggestionClickListener(SuggestionClickListener suggestionClickListener) {
        this.mSuggestionsAdapter.setSuggestionClickListener(suggestionClickListener);
        this.mQueryTextView.setCommitCompletionListener(new QueryTextView.CommitCompletionListener() { // from class: com.android.quicksearchbox.ui.SearchActivityView.1
            @Override // com.android.quicksearchbox.ui.QueryTextView.CommitCompletionListener
            public void onCommitCompletion(int i) {
                SearchActivityView.this.mSuggestionsAdapter.onSuggestionClicked(i);
            }
        });
    }

    public void setExitClickListener(View.OnClickListener onClickListener) {
        this.mExitClickListener = onClickListener;
    }

    public Suggestions getSuggestions() {
        return this.mSuggestionsAdapter.getSuggestions();
    }

    public void setSuggestions(Suggestions suggestions) {
        suggestions.acquire();
        this.mSuggestionsAdapter.setSuggestions(suggestions);
    }

    public void clearSuggestions() {
        this.mSuggestionsAdapter.setSuggestions(null);
    }

    public String getQuery() {
        Editable text = this.mQueryTextView.getText();
        return text == null ? "" : text.toString();
    }

    public boolean isQueryEmpty() {
        return TextUtils.isEmpty(getQuery());
    }

    public void setQuery(String str, boolean z) {
        this.mUpdateSuggestions = false;
        this.mQueryTextView.setText(str);
        this.mQueryTextView.setTextSelection(z);
        this.mUpdateSuggestions = true;
    }

    protected SearchActivity getActivity() {
        Context context = getContext();
        if (context instanceof SearchActivity) {
            return (SearchActivity) context;
        }
        return null;
    }

    public void focusQueryTextView() {
        this.mQueryTextView.requestFocus();
    }

    protected void updateUi(boolean z) {
        updateQueryTextView(z);
        updateSearchGoButton(z);
        updateVoiceSearchButton(z);
    }

    protected void updateQueryTextView(boolean z) {
        if (z) {
            this.mQueryTextView.setBackgroundDrawable(this.mQueryTextEmptyBg);
            this.mQueryTextView.setHint((CharSequence) null);
        } else {
            this.mQueryTextView.setBackgroundResource(R.drawable.textfield_search);
        }
    }

    private void updateSearchGoButton(boolean z) {
        if (z) {
            this.mSearchGoButton.setVisibility(8);
        } else {
            this.mSearchGoButton.setVisibility(0);
        }
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

    protected boolean shouldShowVoiceSearch(boolean z) {
        return z;
    }

    protected void hideInputMethod() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public void showInputMethodForQuery() {
        this.mQueryTextView.showInputMethod();
    }

    @Override // android.view.ViewGroup, android.view.View
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

    protected void updateInputMethodSuggestions() {
        Suggestions suggestions;
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (inputMethodManager == null || !inputMethodManager.isFullscreenMode() || (suggestions = this.mSuggestionsAdapter.getSuggestions()) == null) {
            return;
        }
        inputMethodManager.displayCompletions(this.mQueryTextView, webSuggestionsToCompletions(suggestions));
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

    protected void onSuggestionsChanged() {
        updateInputMethodSuggestions();
    }

    protected boolean onSuggestionKeyDown(SuggestionsAdapter<?> suggestionsAdapter, long j, int i, KeyEvent keyEvent) {
        if ((i != 66 && i != 84 && i != 23) || suggestionsAdapter == null) {
            return false;
        }
        suggestionsAdapter.onSuggestionClicked(j);
        return true;
    }

    protected boolean onSearchClicked(int i) {
        if (this.mSearchClickListener != null) {
            return this.mSearchClickListener.onSearchClicked(i);
        }
        return false;
    }

    private class SearchTextWatcher implements TextWatcher {
        private SearchTextWatcher() {
        }

        @Override // android.text.TextWatcher
        public void afterTextChanged(Editable editable) {
            boolean z = editable.length() == 0;
            if (z != SearchActivityView.this.mQueryWasEmpty) {
                SearchActivityView.this.mQueryWasEmpty = z;
                SearchActivityView.this.updateUi(z);
            }
            if (SearchActivityView.this.mUpdateSuggestions && SearchActivityView.this.mQueryListener != null) {
                SearchActivityView.this.mQueryListener.onQueryChanged();
            }
        }

        @Override // android.text.TextWatcher
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override // android.text.TextWatcher
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    protected class SuggestionsViewKeyListener implements View.OnKeyListener {
        protected SuggestionsViewKeyListener() {
        }

        /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: android.view.View */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.view.View.OnKeyListener
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 0 && (view instanceof SuggestionsListView)) {
                SuggestionsListView suggestionsListView = (SuggestionsListView) view;
                if (SearchActivityView.this.onSuggestionKeyDown(suggestionsListView.getSuggestionsAdapter(), suggestionsListView.getSelectedItemId(), i, keyEvent)) {
                    return true;
                }
            }
            return SearchActivityView.this.forwardKeyToQueryTextView(i, keyEvent);
        }
    }

    private class InputMethodCloser implements AbsListView.OnScrollListener {
        private InputMethodCloser() {
        }

        @Override // android.widget.AbsListView.OnScrollListener
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        }

        @Override // android.widget.AbsListView.OnScrollListener
        public void onScrollStateChanged(AbsListView absListView, int i) {
            SearchActivityView.this.considerHidingInputMethod();
        }
    }

    private class SearchGoButtonClickListener implements View.OnClickListener {
        private SearchGoButtonClickListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            SearchActivityView.this.onSearchClicked(0);
        }
    }

    private class QueryTextEditorActionListener implements TextView.OnEditorActionListener {
        private QueryTextEditorActionListener() {
        }

        @Override // android.widget.TextView.OnEditorActionListener
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (keyEvent != null) {
                if (keyEvent.getAction() == 1) {
                    return SearchActivityView.this.onSearchClicked(1);
                }
                if (keyEvent.getAction() == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private class ButtonsKeyListener implements View.OnKeyListener {
        private ButtonsKeyListener() {
        }

        @Override // android.view.View.OnKeyListener
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            return SearchActivityView.this.forwardKeyToQueryTextView(i, keyEvent);
        }
    }

    private boolean forwardKeyToQueryTextView(int i, KeyEvent keyEvent) {
        if (!keyEvent.isSystem() && shouldForwardToQueryTextView(i) && this.mQueryTextView.requestFocus()) {
            return this.mQueryTextView.dispatchKeyEvent(keyEvent);
        }
        return false;
    }

    private boolean shouldForwardToQueryTextView(int i) {
        if (i == 66 || i == 84) {
            return false;
        }
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return false;
            default:
                return true;
        }
    }

    private class SuggestListFocusListener implements View.OnFocusChangeListener {
        private SuggestListFocusListener() {
        }

        @Override // android.view.View.OnFocusChangeListener
        public void onFocusChange(View view, boolean z) {
            if (z) {
                SearchActivityView.this.considerHidingInputMethod();
            }
        }
    }

    private class QueryTextViewFocusListener implements View.OnFocusChangeListener {
        private QueryTextViewFocusListener() {
        }

        @Override // android.view.View.OnFocusChangeListener
        public void onFocusChange(View view, boolean z) {
            if (z) {
                SearchActivityView.this.showInputMethodForQuery();
            }
        }
    }

    protected class SuggestionsObserver extends DataSetObserver {
        protected SuggestionsObserver() {
        }

        @Override // android.database.DataSetObserver
        public void onChanged() {
            SearchActivityView.this.onSuggestionsChanged();
        }
    }
}
