package com.android.settings.intelligence.search;

import android.support.v7.util.DiffUtil;
import java.util.List;

public class SearchResultDiffCallback extends DiffUtil.Callback {
    private List<? extends SearchResult> mNewList;
    private List<? extends SearchResult> mOldList;

    public SearchResultDiffCallback(List<? extends SearchResult> list, List<? extends SearchResult> list2) {
        this.mOldList = list;
        this.mNewList = list2;
    }

    @Override
    public int getOldListSize() {
        return this.mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return this.mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int i, int i2) {
        return this.mOldList.get(i).equals(this.mNewList.get(i2));
    }

    @Override
    public boolean areContentsTheSame(int i, int i2) {
        return this.mOldList.get(i).equals(this.mNewList.get(i2));
    }
}
