package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionFormatter;

public class WebSearchSuggestionView extends BaseSuggestionView {
    private final SuggestionFormatter mSuggestionFormatter;

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super("web_search", WebSearchSuggestionView.class, 2130968585, context);
        }

        @Override
        public boolean canCreateView(Suggestion suggestion) {
            return suggestion.isWebSearchSuggestion();
        }
    }

    private class KeyListener implements View.OnKeyListener {
        final WebSearchSuggestionView this$0;

        private KeyListener(WebSearchSuggestionView webSearchSuggestionView) {
            this.this$0 = webSearchSuggestionView;
        }

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 0) {
                if (i == 22 && view != this.this$0.mIcon2) {
                    return this.this$0.mIcon2.requestFocus();
                }
                if (i == 21 && view == this.this$0.mIcon2) {
                    return this.this$0.requestFocus();
                }
            }
            return false;
        }
    }

    public WebSearchSuggestionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSuggestionFormatter = QsbApplication.get(context).getSuggestionFormatter();
    }

    private void setIsHistorySuggestion(boolean z) {
        if (!z) {
            this.mIcon1.setVisibility(4);
        } else {
            this.mIcon1.setImageResource(2130837555);
            this.mIcon1.setVisibility(0);
        }
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String str) {
        super.bindAsSuggestion(suggestion, str);
        setText1(this.mSuggestionFormatter.formatSuggestion(str, suggestion.getSuggestionText1()));
        setIsHistorySuggestion(suggestion.isHistorySuggestion());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        KeyListener keyListener = new KeyListener();
        setOnKeyListener(keyListener);
        this.mIcon2.setOnKeyListener(keyListener);
        this.mIcon2.setOnClickListener(new View.OnClickListener(this) {
            final WebSearchSuggestionView this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.onSuggestionQueryRefineClicked();
            }
        });
        this.mIcon2.setFocusable(true);
    }
}
