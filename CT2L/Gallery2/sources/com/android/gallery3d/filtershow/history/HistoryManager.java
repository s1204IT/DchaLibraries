package com.android.gallery3d.filtershow.history;

import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import java.util.Vector;

public class HistoryManager {
    private Vector<HistoryItem> mHistoryItems = new Vector<>();
    private int mCurrentPresetPosition = 0;
    private MenuItem mUndoMenuItem = null;
    private MenuItem mRedoMenuItem = null;
    private MenuItem mResetMenuItem = null;

    public void setMenuItems(MenuItem undoItem, MenuItem redoItem, MenuItem resetItem) {
        this.mUndoMenuItem = undoItem;
        this.mRedoMenuItem = redoItem;
        this.mResetMenuItem = resetItem;
        updateMenuItems();
    }

    private int getCount() {
        return this.mHistoryItems.size();
    }

    public HistoryItem getItem(int position) {
        if (position > this.mHistoryItems.size() - 1) {
            return null;
        }
        return this.mHistoryItems.elementAt(position);
    }

    private void clear() {
        this.mHistoryItems.clear();
    }

    private void add(HistoryItem item) {
        this.mHistoryItems.add(item);
    }

    private void notifyDataSetChanged() {
    }

    public boolean canReset() {
        return getCount() > 0;
    }

    public boolean canUndo() {
        return this.mCurrentPresetPosition != getCount() + (-1);
    }

    public boolean canRedo() {
        return this.mCurrentPresetPosition != 0;
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

    private void setEnabled(MenuItem item, boolean enabled) {
        item.setEnabled(enabled);
        Drawable drawable = item.getIcon();
        if (drawable != null) {
            drawable.setAlpha(enabled ? 255 : 80);
        }
    }

    public void setCurrentPreset(int n) {
        this.mCurrentPresetPosition = n;
        updateMenuItems();
        notifyDataSetChanged();
    }

    public void reset() {
        if (getCount() != 0) {
            clear();
            updateMenuItems();
        }
    }

    public void addHistoryItem(HistoryItem preset) {
        insert(preset, 0);
        updateMenuItems();
    }

    private void insert(HistoryItem preset, int position) {
        if (this.mCurrentPresetPosition != 0) {
            Vector<HistoryItem> oldItems = new Vector<>();
            for (int i = this.mCurrentPresetPosition; i < getCount(); i++) {
                oldItems.add(getItem(i));
            }
            clear();
            for (int i2 = 0; i2 < oldItems.size(); i2++) {
                add(oldItems.elementAt(i2));
            }
            this.mCurrentPresetPosition = position;
            notifyDataSetChanged();
        }
        this.mHistoryItems.insertElementAt(preset, position);
        this.mCurrentPresetPosition = position;
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
}
