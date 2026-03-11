package com.android.browser.addbookmark;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.browser.R;

public class FolderSpinnerAdapter extends BaseAdapter {
    private Context mContext;
    private boolean mIncludeHomeScreen;
    private boolean mIncludesRecentFolder;
    private LayoutInflater mInflater;
    private String mOtherFolderDisplayText;
    private long mRecentFolderId;
    private String mRecentFolderName;

    public FolderSpinnerAdapter(Context context, boolean includeHomeScreen) {
        this.mIncludeHomeScreen = includeHomeScreen;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);
    }

    public void addRecentFolder(long folderId, String folderName) {
        this.mIncludesRecentFolder = true;
        this.mRecentFolderId = folderId;
        this.mRecentFolderName = folderName;
    }

    public long recentFolderId() {
        return this.mRecentFolderId;
    }

    private void bindView(int position, View view, boolean isDropDown) {
        int labelResource;
        int drawableResource;
        if (!this.mIncludeHomeScreen) {
            position++;
        }
        switch (position) {
            case 0:
                labelResource = R.string.add_to_homescreen_menu_option;
                drawableResource = R.drawable.ic_home_holo_dark;
                break;
            case 1:
                labelResource = R.string.add_to_bookmarks_menu_option;
                drawableResource = R.drawable.ic_bookmarks_holo_dark;
                break;
            case 2:
            case 3:
                labelResource = R.string.add_to_other_folder_menu_option;
                drawableResource = R.drawable.ic_folder_holo_dark;
                break;
            default:
                labelResource = 0;
                drawableResource = 0;
                break;
        }
        TextView textView = (TextView) view;
        if (position == 3) {
            textView.setText(this.mRecentFolderName);
        } else if (position == 2 && !isDropDown && this.mOtherFolderDisplayText != null) {
            textView.setText(this.mOtherFolderDisplayText);
        } else {
            textView.setText(labelResource);
        }
        textView.setGravity(16);
        Drawable drawable = this.mContext.getResources().getDrawable(drawableResource);
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        bindView(position, convertView, true);
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }
        bindView(position, convertView, false);
        return convertView;
    }

    @Override
    public int getCount() {
        int count = this.mIncludeHomeScreen ? 3 : 2;
        return this.mIncludesRecentFolder ? count + 1 : count;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        long id = position;
        if (!this.mIncludeHomeScreen) {
            return id + 1;
        }
        return id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void setOtherFolderDisplayText(String parentTitle) {
        this.mOtherFolderDisplayText = parentTitle;
        notifyDataSetChanged();
    }

    public void clearRecentFolder() {
        if (!this.mIncludesRecentFolder) {
            return;
        }
        this.mIncludesRecentFolder = false;
        notifyDataSetChanged();
    }
}
