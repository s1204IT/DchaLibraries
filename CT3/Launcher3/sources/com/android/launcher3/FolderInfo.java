package com.android.launcher3;

import android.content.ContentValues;
import android.content.Context;
import com.android.launcher3.compat.UserHandleCompat;
import java.util.ArrayList;
import java.util.Arrays;

public class FolderInfo extends ItemInfo {
    public ArrayList<ShortcutInfo> contents = new ArrayList<>();
    ArrayList<FolderListener> listeners = new ArrayList<>();
    boolean opened;
    public int options;

    interface FolderListener {
        void onAdd(ShortcutInfo shortcutInfo);

        void onItemsChanged();

        void onRemove(ShortcutInfo shortcutInfo);

        void onTitleChanged(CharSequence charSequence);
    }

    public FolderInfo() {
        this.itemType = 2;
        this.user = UserHandleCompat.myUserHandle();
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
        values.put("options", Integer.valueOf(this.options));
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

    @Override
    public String toString() {
        return "FolderInfo(id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + ")";
    }

    public boolean hasOption(int optionFlag) {
        return (this.options & optionFlag) != 0;
    }

    public void setOption(int option, boolean isEnabled, Context context) {
        int oldOptions = this.options;
        if (isEnabled) {
            this.options |= option;
        } else {
            this.options &= ~option;
        }
        if (context == null || oldOptions == this.options) {
            return;
        }
        LauncherModel.updateItemInDatabase(context, this);
    }
}
