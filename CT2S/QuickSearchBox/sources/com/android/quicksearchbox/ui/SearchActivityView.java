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

    public SearchActivityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mQueryWasEmpty = true;
    }

    public SearchActivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mQueryWasEmpty = true;
    }

    @Override
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

    public void setMaxPromotedResults(int maxPromoted) {
    }

    public void limitResultsToViewHeight() {
    }

    public void setQueryListener(QueryListener listener) {
        this.mQueryListener = listener;
    }

    public void setSearchClickListener(SearchClickListener listener) {
        this.mSearchClickListener = listener;
    }

    public void setVoiceSearchButtonClickListener(View.OnClickListener listener) {
        if (this.mVoiceSearchButton != null) {
            this.mVoiceSearchButton.setOnClickListener(listener);
        }
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        this.mSuggestionsAdapter.setSuggestionClickListener(listener);
        this.mQueryTextView.setCommitCompletionListener(new QueryTextView.CommitCompletionListener() {
            @Override
            public void onCommitCompletion(int position) {
                SearchActivityView.this.mSuggestionsAdapter.onSuggestionClicked(position);
            }
        });
    }

    public void setExitClickListener(View.OnClickListener listener) {
        this.mExitClickListener = listener;
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
        CharSequence q = this.mQueryTextView.getText();
        return q == null ? "" : q.toString();
    }

    public boolean isQueryEmpty() {
        return TextUtils.isEmpty(getQuery());
    }

    public void setQuery(String query, boolean selectAll) {
        this.mUpdateSuggestions = false;
        this.mQueryTextView.setText(query);
        this.mQueryTextView.setTextSelection(selectAll);
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

    protected void updateUi(boolean queryEmpty) {
        updateQueryTextView(queryEmpty);
        updateSearchGoButton(queryEmpty);
        updateVoiceSearchButton(queryEmpty);
    }

    protected void updateQueryTextView(boolean queryEmpty) {
        if (queryEmpty) {
            this.mQueryTextView.setBackgroundDrawable(this.mQueryTextEmptyBg);
            this.mQueryTextView.setHint((CharSequence) null);
        } else {
            this.mQueryTextView.setBackgroundResource(R.drawable.textfield_search);
        }
    }

    private void updateSearchGoButton(boolean queryEmpty) {
        if (queryEmpty) {
            this.mSearchGoButton.setVisibility(8);
        } else {
            this.mSearchGoButton.setVisibility(0);
        }
    }

    protected void updateVoiceSearchButton(boolean queryEmpty) {
        if (shouldShowVoiceSearch(queryEmpty) && getVoiceSearch().shouldShowVoiceSearch()) {
            this.mVoiceSearchButton.setVisibility(0);
            this.mQueryTextView.setPrivateImeOptions("nm");
        } else {
            this.mVoiceSearchButton.setVisibility(8);
            this.mQueryTextView.setPrivateImeOptions(null);
        }
    }

    protected boolean shouldShowVoiceSearch(boolean queryEmpty) {
        return queryEmpty;
    }

    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService("input_method");
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public void showInputMethodForQuery() {
        this.mQueryTextView.showInputMethod();
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        KeyEvent.DispatcherState state;
        SearchActivity activity = getActivity();
        if (activity != null && event.getKeyCode() == 4 && isQueryEmpty() && (state = getKeyDispatcherState()) != null) {
            if (event.getAction() == 0 && event.getRepeatCount() == 0) {
                state.startTracking(event, this);
                return true;
            }
            if (event.getAction() == 1 && !event.isCanceled() && state.isTracking(event)) {
                hideInputMethod();
                activity.onBackPressed();
                return true;
            }
        }
        return super.dispatchKeyEventPreIme(event);
    }

    protected void updateInputMethodSuggestions() {
        Suggestions suggestions;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService("input_method");
        if (imm != null && imm.isFullscreenMode() && (suggestions = this.mSuggestionsAdapter.getSuggestions()) != null) {
            CompletionInfo[] completions = webSuggestionsToCompletions(suggestions);
            imm.displayCompletions(this.mQueryTextView, completions);
        }
    }

    private CompletionInfo[] webSuggestionsToCompletions(Suggestions suggestions) {
        SourceResult cursor = suggestions.getWebResult();
        if (cursor == null) {
            return null;
        }
        int count = cursor.getCount();
        ArrayList<CompletionInfo> completions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            cursor.moveTo(i);
            String text1 = cursor.getSuggestionText1();
            completions.add(new CompletionInfo(i, i, text1));
        }
        return (CompletionInfo[]) completions.toArray(new CompletionInfo[completions.size()]);
    }

    protected void onSuggestionsChanged() {
        updateInputMethodSuggestions();
    }

    protected boolean onSuggestionKeyDown(SuggestionsAdapter<?> adapter, long suggestionId, int keyCode, KeyEvent event) {
        if ((keyCode != 66 && keyCode != 84 && keyCode != 23) || adapter == null) {
            return false;
        }
        adapter.onSuggestionClicked(suggestionId);
        return true;
    }

    protected boolean onSearchClicked(int method) {
        if (this.mSearchClickListener != null) {
            return this.mSearchClickListener.onSearchClicked(method);
        }
        return false;
    }

    private class SearchTextWatcher implements TextWatcher {
        private SearchTextWatcher() {
        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean empty = s.length() == 0;
            if (empty != SearchActivityView.this.mQueryWasEmpty) {
                SearchActivityView.this.mQueryWasEmpty = empty;
                SearchActivityView.this.updateUi(empty);
            }
            if (SearchActivityView.this.mUpdateSuggestions && SearchActivityView.this.mQueryListener != null) {
                SearchActivityView.this.mQueryListener.onQueryChanged();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    protected class SuggestionsViewKeyListener implements View.OnKeyListener {
        protected SuggestionsViewKeyListener() {
        }

        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            if (event.getAction() == 0 && (view instanceof SuggestionsListView)) {
                SuggestionsListView<?> listView = (SuggestionsListView) view;
                if (SearchActivityView.this.onSuggestionKeyDown(listView.getSuggestionsAdapter(), listView.getSelectedItemId(), keyCode, event)) {
                    return true;
                }
            }
            return SearchActivityView.this.forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private class InputMethodCloser implements AbsListView.OnScrollListener {
        private InputMethodCloser() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            SearchActivityView.this.considerHidingInputMethod();
        }
    }

    private class SearchGoButtonClickListener implements View.OnClickListener {
        private SearchGoButtonClickListener() {
        }

        @Override
        public void onClick(View view) {
            SearchActivityView.this.onSearchClicked(0);
        }
    }

    private class QueryTextEditorActionListener implements TextView.OnEditorActionListener {
        private QueryTextEditorActionListener() {
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null) {
                return false;
            }
            if (event.getAction() == 1) {
                boolean consumed = SearchActivityView.this.onSearchClicked(1);
                return consumed;
            }
            if (event.getAction() != 0) {
                return false;
            }
            return true;
        }
    }

    private class ButtonsKeyListener implements View.OnKeyListener {
        private ButtonsKeyListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return SearchActivityView.this.forwardKeyToQueryTextView(keyCode, event);
        }
    }

    private boolean forwardKeyToQueryTextView(int keyCode, KeyEvent event) {
        if (!event.isSystem() && shouldForwardToQueryTextView(keyCode) && this.mQueryTextView.requestFocus()) {
            return this.mQueryTextView.dispatchKeyEvent(event);
        }
        return false;
    }

    private boolean shouldForwardToQueryTextView(int keyCode) {
        switch (keyCode) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 66:
            case 84:
                return false;
            default:
                return true;
        }
    }

    private class SuggestListFocusListener implements View.OnFocusChangeListener {
        private SuggestListFocusListener() {
        }

        @Override
        public void onFocusChange(View v, boolean focused) {
            if (focused) {
                SearchActivityView.this.considerHidingInputMethod();
            }
        }
    }

    private class QueryTextViewFocusListener implements View.OnFocusChangeListener {
        private QueryTextViewFocusListener() {
        }

        @Override
        public void onFocusChange(View v, boolean focused) {
            if (focused) {
                SearchActivityView.this.showInputMethodForQuery();
            }
        }
    }

    protected class SuggestionsObserver extends DataSetObserver {
        protected SuggestionsObserver() {
        }

        @Override
        public void onChanged() {
            SearchActivityView.this.onSuggestionsChanged();
        }
    }
}
