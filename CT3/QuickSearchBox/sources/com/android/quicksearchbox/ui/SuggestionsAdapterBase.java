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

    @Override
    public abstract A getListAdapter();

    @Override
    public abstract SuggestionPosition getSuggestion(long j);

    protected abstract void notifyDataSetChanged();

    protected abstract void notifyDataSetInvalidated();

    protected SuggestionsAdapterBase(SuggestionViewFactory viewFactory) {
        this.mViewFactory = viewFactory;
        for (String viewType : this.mViewFactory.getSuggestionViewTypes()) {
            if (!this.mViewTypeMap.containsKey(viewType)) {
                this.mViewTypeMap.put(viewType, Integer.valueOf(this.mViewTypeMap.size()));
            }
        }
    }

    public boolean isClosed() {
        return this.mClosed;
    }

    @Override
    public void setSuggestionClickListener(SuggestionClickListener listener) {
        this.mSuggestionClickListener = listener;
    }

    @Override
    public void setOnFocusChangeListener(View.OnFocusChangeListener l) {
        this.mOnFocusChangeListener = l;
    }

    @Override
    public void setSuggestions(Suggestions suggestions) {
        MySuggestionsObserver mySuggestionsObserver = null;
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
            this.mDataSetObserver = new MySuggestionsObserver(this, mySuggestionsObserver);
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

    @Override
    public Suggestions getSuggestions() {
        return this.mSuggestions;
    }

    protected SuggestionPosition getSuggestion(int position) {
        if (this.mCurrentSuggestions == null) {
            return null;
        }
        return new SuggestionPosition(this.mCurrentSuggestions, position);
    }

    private String suggestionViewType(Suggestion suggestion) {
        String viewType = this.mViewFactory.getViewType(suggestion);
        if (!this.mViewTypeMap.containsKey(viewType)) {
            throw new IllegalStateException("Unknown viewType " + viewType);
        }
        return viewType;
    }

    protected int getSuggestionViewType(SuggestionCursor cursor, int position) {
        if (cursor == null) {
            return 0;
        }
        cursor.moveTo(position);
        return this.mViewTypeMap.get(suggestionViewType(cursor)).intValue();
    }

    protected int getSuggestionViewTypeCount() {
        return this.mViewTypeMap.size();
    }

    protected View getView(SuggestionCursor suggestions, int position, long suggestionId, View convertView, ViewGroup parent) {
        suggestions.moveTo(position);
        View view = this.mViewFactory.getView(suggestions, suggestions.getUserQuery(), convertView, parent);
        if (view instanceof SuggestionView) {
            ((SuggestionView) view).bindAdapter(this, suggestionId);
        } else {
            SuggestionsAdapterBase<A>.SuggestionViewClickListener l = new SuggestionViewClickListener(suggestionId);
            view.setOnClickListener(l);
        }
        if (this.mOnFocusChangeListener != null) {
            view.setOnFocusChangeListener(this.mOnFocusChangeListener);
        }
        return view;
    }

    protected void onSuggestionsChanged() {
        SuggestionCursor cursor = null;
        if (this.mSuggestions != null) {
            cursor = this.mSuggestions.getResult();
        }
        changeSuggestions(cursor);
    }

    public SuggestionCursor getCurrentSuggestions() {
        return this.mCurrentSuggestions;
    }

    private void changeSuggestions(SuggestionCursor newCursor) {
        if (newCursor == this.mCurrentSuggestions) {
            if (newCursor != null) {
                notifyDataSetChanged();
            }
        } else {
            this.mCurrentSuggestions = newCursor;
            if (this.mCurrentSuggestions != null) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

    @Override
    public void onSuggestionClicked(long suggestionId) {
        if (this.mClosed) {
            Log.w("QSB.SuggestionsAdapter", "onSuggestionClicked after close");
        } else {
            if (this.mSuggestionClickListener == null) {
                return;
            }
            this.mSuggestionClickListener.onSuggestionClicked(this, suggestionId);
        }
    }

    @Override
    public void onSuggestionQueryRefineClicked(long suggestionId) {
        if (this.mClosed) {
            Log.w("QSB.SuggestionsAdapter", "onSuggestionQueryRefineClicked after close");
        } else {
            if (this.mSuggestionClickListener == null) {
                return;
            }
            this.mSuggestionClickListener.onSuggestionQueryRefineClicked(this, suggestionId);
        }
    }

    private class MySuggestionsObserver extends DataSetObserver {
        MySuggestionsObserver(SuggestionsAdapterBase this$0, MySuggestionsObserver mySuggestionsObserver) {
            this();
        }

        private MySuggestionsObserver() {
        }

        @Override
        public void onChanged() {
            SuggestionsAdapterBase.this.onSuggestionsChanged();
        }
    }

    private class SuggestionViewClickListener implements View.OnClickListener {
        private final long mSuggestionId;

        public SuggestionViewClickListener(long suggestionId) {
            this.mSuggestionId = suggestionId;
        }

        @Override
        public void onClick(View v) {
            SuggestionsAdapterBase.this.onSuggestionClicked(this.mSuggestionId);
        }
    }
}
