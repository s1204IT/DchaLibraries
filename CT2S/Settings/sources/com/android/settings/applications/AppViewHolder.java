package com.android.settings.applications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.applications.ApplicationsState;

public class AppViewHolder {
    public ImageView appIcon;
    public TextView appName;
    public TextView appSize;
    public CheckBox checkBox;
    public TextView disabled;
    public ApplicationsState.AppEntry entry;
    public View rootView;

    public static AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
        if (convertView != null) {
            return (AppViewHolder) convertView.getTag();
        }
        View convertView2 = inflater.inflate(R.layout.manage_applications_item, (ViewGroup) null);
        AppViewHolder holder = new AppViewHolder();
        holder.rootView = convertView2;
        holder.appName = (TextView) convertView2.findViewById(R.id.app_name);
        holder.appIcon = (ImageView) convertView2.findViewById(R.id.app_icon);
        holder.appSize = (TextView) convertView2.findViewById(R.id.app_size);
        holder.disabled = (TextView) convertView2.findViewById(R.id.app_disabled);
        holder.checkBox = (CheckBox) convertView2.findViewById(R.id.app_on_sdcard);
        convertView2.setTag(holder);
        return holder;
    }

    void updateSizeText(CharSequence invalidSizeStr, int whichSize) {
        if (this.entry.sizeStr == null) {
            if (this.entry.size == -2) {
                this.appSize.setText(invalidSizeStr);
                return;
            }
            return;
        }
        switch (whichSize) {
            case 1:
                this.appSize.setText(this.entry.internalSizeStr);
                break;
            case 2:
                this.appSize.setText(this.entry.externalSizeStr);
                break;
            default:
                this.appSize.setText(this.entry.sizeStr);
                break;
        }
    }
}
