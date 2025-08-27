package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionFormatter;

/* loaded from: classes.dex */
public class WebSearchSuggestionView extends BaseSuggestionView {
    private final SuggestionFormatter mSuggestionFormatter;

    public WebSearchSuggestionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    @Override // com.android.quicksearchbox.ui.BaseSuggestionView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        KeyListener keyListener = new KeyListener();
        setOnKeyListener(keyListener);
        this.mIcon2.setOnKeyListener(keyListener);
        this.mIcon2.setOnClickListener(new View.OnClickListener() { // from class: com.android.quicksearchbox.ui.WebSearchSuggestionView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                WebSearchSuggestionView.this.onSuggestionQueryRefineClicked();
            }
        });
        this.mIcon2.setFocusable(true);
    }

    @Override // com.android.quicksearchbox.ui.BaseSuggestionView, com.android.quicksearchbox.ui.SuggestionView
    public void bindAsSuggestion(Suggestion suggestion, String str) {
        super.bindAsSuggestion(suggestion, str);
        setText1(this.mSuggestionFormatter.formatSuggestion(str, suggestion.getSuggestionText1()));
        setIsHistorySuggestion(suggestion.isHistorySuggestion());
    }

    private void setIsHistorySuggestion(boolean z) {
        if (z) {
            this.mIcon1.setImageResource(R.drawable.ic_history_suggestion);
            this.mIcon1.setVisibility(0);
        } else {
            this.mIcon1.setVisibility(4);
        }
    }

    private class KeyListener implements View.OnKeyListener {
        private KeyListener() {
        }

        @Override // android.view.View.OnKeyListener
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 0) {
                if (i == 22 && view != WebSearchSuggestionView.this.mIcon2) {
                    return WebSearchSuggestionView.this.mIcon2.requestFocus();
                }
                if (i == 21 && view == WebSearchSuggestionView.this.mIcon2) {
                    return WebSearchSuggestionView.this.requestFocus();
                }
            }
            return false;
        }
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super("web_search", WebSearchSuggestionView.class, R.layout.web_search_suggestion, context);
        }

        @Override // com.android.quicksearchbox.ui.SuggestionViewInflater, com.android.quicksearchbox.ui.SuggestionViewFactory
        public boolean canCreateView(Suggestion suggestion) {
            return suggestion.isWebSearchSuggestion();
        }
    }
}
