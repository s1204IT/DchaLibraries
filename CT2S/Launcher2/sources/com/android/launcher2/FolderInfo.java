package com.android.launcher2;

import android.content.ContentValues;
import android.content.Context;
import java.util.ArrayList;

class FolderInfo extends ItemInfo {
    ArrayList<ShortcutInfo> contents = new ArrayList<>();
    ArrayList<FolderListener> listeners = new ArrayList<>();
    boolean opened;

    interface FolderListener {
        void onAdd(ShortcutInfo shortcutInfo);

        void onItemsChanged();

        void onRemove(ShortcutInfo shortcutInfo);

        void onTitleChanged(CharSequence charSequence);
    }

    FolderInfo() {
        this.itemType = 2;
    }

    public void add(ShortcutInfo item) {
        this.contents.add(item);
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onAdd(item);
        }
        itemsChanged();
    }

    public void remove(ShortcutInfo item) {
        this.contents.remove(item);
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onRemove(item);
        }
        itemsChanged();
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onTitleChanged(title);
        }
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put("title", this.title.toString());
    }

    void addListener(FolderListener listener) {
        this.listeners.add(listener);
    }

    void itemsChanged() {
        for (int i = 0; i < this.listeners.size(); i++) {
            this.listeners.get(i).onItemsChanged();
        }
    }

    @Override
    void unbind() {
        super.unbind();
        this.listeners.clear();
    }
}
