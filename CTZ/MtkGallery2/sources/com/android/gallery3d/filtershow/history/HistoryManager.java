package com.android.gallery3d.filtershow.history;

import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import com.mediatek.gallery3d.util.Log;
import java.util.Vector;

public class HistoryManager {
    private Vector<HistoryItem> mHistoryItems = new Vector<>();
    private int mCurrentPresetPosition = 0;
    private MenuItem mUndoMenuItem = null;
    private MenuItem mRedoMenuItem = null;
    private MenuItem mResetMenuItem = null;

    public void setMenuItems(MenuItem menuItem, MenuItem menuItem2, MenuItem menuItem3) {
        this.mUndoMenuItem = menuItem;
        this.mRedoMenuItem = menuItem2;
        this.mResetMenuItem = menuItem3;
        updateMenuItems();
    }

    private int getCount() {
        return this.mHistoryItems.size();
    }

    public HistoryItem getItem(int i) {
        if (i < 0 || i > this.mHistoryItems.size() - 1) {
            return null;
        }
        return this.mHistoryItems.elementAt(i);
    }

    private void clear() {
        this.mHistoryItems.clear();
    }

    private void add(HistoryItem historyItem) {
        this.mHistoryItems.add(historyItem);
    }

    private void notifyDataSetChanged() {
    }

    public boolean canReset() {
        if (getCount() <= 0) {
            return false;
        }
        return true;
    }

    public boolean canUndo() {
        return this.mCurrentPresetPosition != getCount() - 1;
    }

    public boolean canRedo() {
        if (this.mCurrentPresetPosition == 0) {
            return false;
        }
        return true;
    }

    public void updateMenuItems() {
        if (this.mUndoMenuItem != null) {
            setEnabled(this.mUndoMenuItem, canUndo());
        }
        if (this.mRedoMenuItem != null) {
            setEnabled(this.mRedoMenuItem, canRedo());
        }
        if (this.mResetMenuItem != null) {
            setEnabled(this.mResetMenuItem, canReset());
        }
    }

    private void setEnabled(MenuItem menuItem, boolean z) {
        menuItem.setEnabled(z);
        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.setAlpha(z ? 255 : 80);
        }
    }

    public void setCurrentPreset(int i) {
        this.mCurrentPresetPosition = i;
        updateMenuItems();
        notifyDataSetChanged();
    }

    public void reset() {
        if (getCount() == 0) {
            return;
        }
        clear();
        updateMenuItems();
    }

    public void addHistoryItem(HistoryItem historyItem) {
        insert(historyItem, 0);
        updateMenuItems();
    }

    private void insert(HistoryItem historyItem, int i) {
        if (this.mCurrentPresetPosition != 0) {
            Vector vector = new Vector();
            for (int i2 = this.mCurrentPresetPosition; i2 < getCount(); i2++) {
                vector.add(getItem(i2));
            }
            clear();
            for (int i3 = 0; i3 < vector.size(); i3++) {
                add((HistoryItem) vector.elementAt(i3));
            }
            this.mCurrentPresetPosition = i;
            notifyDataSetChanged();
        }
        this.mHistoryItems.insertElementAt(historyItem, i);
        this.mCurrentPresetPosition = i;
        notifyDataSetChanged();
    }

    public int redo() {
        this.mCurrentPresetPosition--;
        if (this.mCurrentPresetPosition < 0) {
            this.mCurrentPresetPosition = 0;
        }
        notifyDataSetChanged();
        updateMenuItems();
        return this.mCurrentPresetPosition;
    }

    public int undo() {
        this.mCurrentPresetPosition++;
        if (this.mCurrentPresetPosition >= getCount()) {
            this.mCurrentPresetPosition = getCount() - 1;
        }
        notifyDataSetChanged();
        updateMenuItems();
        return this.mCurrentPresetPosition;
    }

    public void removeLast() {
        if (this.mHistoryItems == null || this.mHistoryItems.size() == 0) {
            return;
        }
        Log.d("HistoryManager", "<removeLast>");
        this.mHistoryItems.removeElementAt(0);
        notifyDataSetChanged();
        updateMenuItems();
    }
}
