package com.mediatek.gallery3d.video;

import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;

public class DefaultMovieList implements IMovieList {
    private static final String TAG = "VP_DefaultMovieList";
    private static final int UNKNOWN = -1;
    private final ArrayList<IMovieItem> mItems = new ArrayList<>();

    @Override
    public void add(IMovieItem iMovieItem) {
        Log.v(TAG, "add(" + iMovieItem + ")");
        this.mItems.add(iMovieItem);
    }

    @Override
    public int index(IMovieItem iMovieItem) {
        int size = this.mItems.size();
        int i = 0;
        while (true) {
            if (i < size) {
                if (iMovieItem == this.mItems.get(i)) {
                    break;
                }
                i++;
            } else {
                i = -1;
                break;
            }
        }
        Log.v(TAG, "index(" + iMovieItem + ") return " + i);
        return i;
    }

    @Override
    public int size() {
        return this.mItems.size();
    }

    @Override
    public IMovieItem getNext(IMovieItem iMovieItem) {
        int iIndex = index(iMovieItem);
        if (iIndex >= 0 && iIndex < size() - 1) {
            return this.mItems.get(iIndex + 1);
        }
        return null;
    }

    @Override
    public IMovieItem getPrevious(IMovieItem iMovieItem) {
        int iIndex = index(iMovieItem);
        if (iIndex > 0 && iIndex < size()) {
            return this.mItems.get(iIndex - 1);
        }
        return null;
    }
}
