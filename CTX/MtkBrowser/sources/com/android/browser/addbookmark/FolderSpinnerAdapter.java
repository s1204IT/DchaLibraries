package com.android.browser.addbookmark;

import android.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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

    private void bindView(int i, View view, boolean z) {
        int i2;
        int i3;
        if (!this.mIncludeHomeScreen) {
            i++;
        }
        switch (i) {
            case 0:
                i2 = 2130837561;
                i3 = 2131492996;
                break;
            case 1:
                i2 = 2130837543;
                i3 = 2131492995;
                break;
            case 2:
            case 3:
                i2 = 2130837551;
                i3 = 2131492997;
                break;
            default:
                i2 = 0;
                i3 = 0;
                break;
        }
        TextView textView = (TextView) view;
        if (i == 3) {
            textView.setText(this.mRecentFolderName);
        } else if (i != 2 || z || this.mOtherFolderDisplayText == null) {
            textView.setText(i3);
        } else {
            textView.setText(this.mOtherFolderDisplayText);
        }
        textView.setGravity(16);
        textView.setCompoundDrawablesWithIntrinsicBounds(this.mContext.getResources().getDrawable(i2), (Drawable) null, (Drawable) null, (Drawable) null);
    }

    public void addRecentFolder(long j, String str) {
        this.mIncludesRecentFolder = true;
        this.mRecentFolderId = j;
        this.mRecentFolderName = str;
    }

    public void clearRecentFolder() {
        if (this.mIncludesRecentFolder) {
            this.mIncludesRecentFolder = false;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        int i = this.mIncludeHomeScreen ? 3 : 2;
        return this.mIncludesRecentFolder ? i + 1 : i;
    }

    @Override
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.simple_spinner_dropdown_item, viewGroup, false);
        }
        bindView(i, view, true);
        return view;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        long j = i;
        return !this.mIncludeHomeScreen ? j + 1 : j;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.simple_spinner_item, viewGroup, false);
        }
        bindView(i, view, false);
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public long recentFolderId() {
        return this.mRecentFolderId;
    }

    public void setOtherFolderDisplayText(String str) {
        this.mOtherFolderDisplayText = str;
        notifyDataSetChanged();
    }
}
