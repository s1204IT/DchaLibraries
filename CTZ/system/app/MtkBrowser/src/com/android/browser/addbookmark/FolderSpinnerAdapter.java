package com.android.browser.addbookmark;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.browser.R;

/* loaded from: classes.dex */
public class FolderSpinnerAdapter extends BaseAdapter {
    private Context mContext;
    private boolean mIncludeHomeScreen;
    private boolean mIncludesRecentFolder;
    private LayoutInflater mInflater;
    private String mOtherFolderDisplayText;
    private long mRecentFolderId;
    private String mRecentFolderName;

    public FolderSpinnerAdapter(Context context, boolean z) {
        this.mIncludeHomeScreen = z;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);
    }

    public void addRecentFolder(long j, String str) {
        this.mIncludesRecentFolder = true;
        this.mRecentFolderId = j;
        this.mRecentFolderName = str;
    }

    public long recentFolderId() {
        return this.mRecentFolderId;
    }

    private void bindView(int i, View view, boolean z) {
        int i2;
        if (!this.mIncludeHomeScreen) {
            i++;
        }
        int i3 = 0;
        switch (i) {
            case 0:
                i3 = R.string.add_to_homescreen_menu_option;
                i2 = R.drawable.ic_home_holo_dark;
                break;
            case 1:
                i3 = R.string.add_to_bookmarks_menu_option;
                i2 = R.drawable.ic_bookmarks_holo_dark;
                break;
            case 2:
            case 3:
                i3 = R.string.add_to_other_folder_menu_option;
                i2 = R.drawable.ic_folder_holo_dark;
                break;
            default:
                i2 = 0;
                break;
        }
        TextView textView = (TextView) view;
        if (i == 3) {
            textView.setText(this.mRecentFolderName);
        } else if (i == 2 && !z && this.mOtherFolderDisplayText != null) {
            textView.setText(this.mOtherFolderDisplayText);
        } else {
            textView.setText(i3);
        }
        textView.setGravity(16);
        textView.setCompoundDrawablesWithIntrinsicBounds(this.mContext.getResources().getDrawable(i2), (Drawable) null, (Drawable) null, (Drawable) null);
    }

    @Override // android.widget.BaseAdapter, android.widget.SpinnerAdapter
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, viewGroup, false);
        }
        bindView(i, view, true);
        return view;
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(android.R.layout.simple_spinner_item, viewGroup, false);
        }
        bindView(i, view, false);
        return view;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        int i = this.mIncludeHomeScreen ? 3 : 2;
        return this.mIncludesRecentFolder ? i + 1 : i;
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return null;
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        long j = i;
        if (!this.mIncludeHomeScreen) {
            return j + 1;
        }
        return j;
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public boolean hasStableIds() {
        return true;
    }

    public void setOtherFolderDisplayText(String str) {
        this.mOtherFolderDisplayText = str;
        notifyDataSetChanged();
    }

    public void clearRecentFolder() {
        if (this.mIncludesRecentFolder) {
            this.mIncludesRecentFolder = false;
            notifyDataSetChanged();
        }
    }
}
