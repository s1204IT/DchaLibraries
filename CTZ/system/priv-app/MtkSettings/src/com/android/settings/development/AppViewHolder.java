package com.android.settings.development;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

/* loaded from: classes.dex */
public class AppViewHolder {
    public ImageView appIcon;
    public TextView appName;
    public TextView disabled;
    public View rootView;
    public TextView summary;

    public static AppViewHolder createOrRecycle(LayoutInflater layoutInflater, View view) {
        if (view == null) {
            View viewInflate = layoutInflater.inflate(R.layout.preference_app, (ViewGroup) null);
            AppViewHolder appViewHolder = new AppViewHolder();
            appViewHolder.rootView = viewInflate;
            appViewHolder.appName = (TextView) viewInflate.findViewById(android.R.id.title);
            appViewHolder.appIcon = (ImageView) viewInflate.findViewById(android.R.id.icon);
            appViewHolder.summary = (TextView) viewInflate.findViewById(android.R.id.summary);
            appViewHolder.disabled = (TextView) viewInflate.findViewById(R.id.appendix);
            viewInflate.setTag(appViewHolder);
            return appViewHolder;
        }
        return (AppViewHolder) view.getTag();
    }
}
