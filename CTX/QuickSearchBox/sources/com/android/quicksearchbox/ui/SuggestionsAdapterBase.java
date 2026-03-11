package com.android.quicksearchbox.ui;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;
import java.util.HashMap;

public abstract class SuggestionsAdapterBase<A> implements SuggestionsAdapter<A> {
    private SuggestionCursor mCurrentSuggestions;
    private DataSetObserver mDataSetObserver;
    private View.OnFocusChangeListener mOnFocusChangeListener;
    private SuggestionClickListener mSuggestionClickListener;
    private Suggestions mSuggestions;
    private final SuggestionViewFactory mViewFactory;
    private boolean mClosed = false;
    private final HashMap<String, Integer> mViewTypeMap = new HashMap<>();

    private class MySuggestionsObserver extends DataSetObserver {
        final SuggestionsAdapterBase this$0;

        private MySuggestionsObserver(SuggestionsAdapterBase suggestionsAdapterBase) {
            this.this$0 = suggestionsAdapterBase;
        }

        @Override
        public void onChanged() {
            this.this$0.onSuggestionsChanged();
        }
    }

    private class SuggestionViewClickListener implements View.OnClickListener {
        private final long mSuggestionId;
        final SuggestionsAdapterBase this$0;

        public SuggestionViewClickListener(SuggestionsAdapterBase suggestionsAdapterBase, long j) {
            this.this$0 = suggestionsAdapterBase;
            this.mSuggestionId = j;
        }

        @Override
        public void onClick(View view) {
            this.this$0.onSuggestionClicked(this.mSuggestionId);
        }
    }

    protected SuggestionsAdapterBase(SuggestionViewFactory suggestionViewFactory) {
        this.mViewFactory = suggestionViewFactory;
        for (String str : this.mViewFactory.getSuggestionViewTypes()) {
            if (!this.mViewTypeMap.containsKey(str)) {
                this.mViewTypeMap.put(str, Integer.valueOf(this.mViewTypeMap.size()));
            }
        }
    }

    private void changeSuggestions(SuggestionCursor suggestionCursor) {
        if (suggestionCursor == this.mCurrentSuggestions) {
            if (suggestionCursor != null) {
                notifyDataSetChanged();
            }
        } else {
            this.mCurrentSuggestions = suggestionCursor;
            if (this.mCurrentSuggestions != null) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

    private String suggestionViewType(Suggestion suggestion) {
        String viewType = this.mViewFactory.getViewType(suggestion);
        if (this.mViewTypeMap.containsKey(viewType)) {
            return viewType;
        }
        throw new IllegalStateException("Unknown viewType " + viewType);
    }

    public SuggestionCursor getCurrentSuggestions() {
        return this.mCurrentSuggestions;
    }

    @Override
    public abstract A getListAdapter();

    protected SuggestionPosition getSuggestion(int i) {
        if (this.mCurrentSuggestions == null) {
            return null;
        }
        return new SuggestionPosition(this.mCurrentSuggestions, i);
    }

    @Override
    public abstract SuggestionPosition getSuggestion(long j);

    protected int getSuggestionViewType(SuggestionCursor suggestionCursor, int i) {
        if (suggestionCursor == null) {
            return 0;
        }
        suggestionCursor.moveTo(i);
        return this.mViewTypeMap.get(suggestionViewType(suggestionCursor)).intValue();
    }

    protected int getSuggestionViewTypeCount() {
        return this.mViewTypeMap.size();
    }

    @Override
    public Suggestions getSuggestions() {
        return this.mSuggestions;
    }

    protected View getView(SuggestionCursor suggestionCursor, int i, long j, View view, ViewGroup viewGroup) {
        suggestionCursor.moveTo(i);
        View view2 = this.mViewFactory.getView(suggestionCursor, suggestionCursor.getUserQuery(), view, viewGroup);
        if (view2 instanceof SuggestionView) {
            ((SuggestionView) view2).bindAdapter(this, j);
        } else {
            view2.setOnClickListener(new SuggestionViewClickListener(this, j));
        }
        if (this.mOnFocusChangeListener != null) {
            view2.setOnFocusChangeListener(this.mOnFocusChangeListener);
        }
        return view2;
    }

    public boolean isClosed() {
        return this.mClosed;
    }

    protected abstract void notifyDataSetChanged();

    protected abstract void notifyDataSetInvalidated();

    @Override
    public void onSuggestionClicked(long j) {
        if (this.mClosed) {
            Log.w("QSB.SuggestionsAdapter", "onSuggestionClicked after close");
        } else if (this.mSuggestionClickListener != null) {
            this.mSuggestionClickListener.onSuggestionClicked(this, j);
        }
    }

    @Override
    public void onSuggestionQueryRefineClicked(long j) {
        if (this.mClosed) {
            Log.w("QSB.SuggestionsAdapter", "onSuggestionQueryRefineClicked after close");
        } else if (this.mSuggestionClickListener != null) {
            this.mSuggestionClickListener.onSuggestionQueryRefineClicked(this, j);
        }
    }

    protected void onSuggestionsChanged() {
        changeSuggestions(this.mSuggestions != null ? this.mSuggestions.getResult() : null);
    }

    @Override
    public void setOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener) {
        this.mOnFocusChangeListener = onFocusChangeListener;
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener suggestionClickListener) {
        this.mSuggestionClickListener = suggestionClickListener;
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        if (this.mSuggestions == suggestions) {
            return;
        }
        if (this.mClosed) {
            if (suggestions != null) {
                suggestions.release();
                return;
            }
            return;
        }
        if (this.mDataSetObserver == null) {
            this.mDataSetObserver = new MySuggestionsObserver();
        }
        if (this.mSuggestions != null) {
            this.mSuggestions.unregisterDataSetObserver(this.mDataSetObserver);
            this.mSuggestions.release();
        }
        this.mSuggestions = suggestions;
        if (this.mSuggestions != null) {
            this.mSuggestions.registerDataSetObserver(this.mDataSetObserver);
        }
        onSuggestionsChanged();
    }
}
