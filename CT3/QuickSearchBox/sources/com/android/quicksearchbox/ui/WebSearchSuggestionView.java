package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionFormatter;

public class WebSearchSuggestionView extends BaseSuggestionView {
    private final SuggestionFormatter mSuggestionFormatter;

    public WebSearchSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        KeyListener keyListener = new KeyListener(this, null);
        setOnKeyListener(keyListener);
        this.mIcon2.setOnKeyListener(keyListener);
        this.mIcon2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebSearchSuggestionView.this.onSuggestionQueryRefineClicked();
            }
        });
        this.mIcon2.setFocusable(true);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        super.bindAsSuggestion(suggestion, userQuery);
        CharSequence text1 = this.mSuggestionFormatter.formatSuggestion(userQuery, suggestion.getSuggestionText1());
        setText1(text1);
        setIsHistorySuggestion(suggestion.isHistorySuggestion());
    }

    private void setIsHistorySuggestion(boolean isHistory) {
        if (isHistory) {
            this.mIcon1.setImageResource(R.drawable.ic_history_suggestion);
            this.mIcon1.setVisibility(0);
        } else {
            this.mIcon1.setVisibility(4);
        }
    }

    private class KeyListener implements View.OnKeyListener {
        KeyListener(WebSearchSuggestionView this$0, KeyListener keyListener) {
            this();
        }

        private KeyListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != 0) {
                return false;
            }
            if (keyCode == 22 && v != WebSearchSuggestionView.this.mIcon2) {
                boolean consumed = WebSearchSuggestionView.this.mIcon2.requestFocus();
                return consumed;
            }
            if (keyCode != 21 || v != WebSearchSuggestionView.this.mIcon2) {
                return false;
            }
            boolean consumed2 = WebSearchSuggestionView.this.requestFocus();
            return consumed2;
        }
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super("web_search", WebSearchSuggestionView.class, R.layout.web_search_suggestion, context);
        }

        @Override
        public boolean canCreateView(Suggestion suggestion) {
            return suggestion.isWebSearchSuggestion();
        }
    }
}
