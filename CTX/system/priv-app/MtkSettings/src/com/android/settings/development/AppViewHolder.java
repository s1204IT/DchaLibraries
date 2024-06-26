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
            View inflate = layoutInflater.inflate(R.layout.preference_app, (ViewGroup) null);
            AppViewHolder appViewHolder = new AppViewHolder();
            appViewHolder.rootView = inflate;
            appViewHolder.appName = (TextView) inflate.findViewById(16908310);
            appViewHolder.appIcon = (ImageView) inflate.findViewById(16908294);
            appViewHolder.summary = (TextView) inflate.findViewById(16908304);
            appViewHolder.disabled = (TextView) inflate.findViewById(R.id.appendix);
            inflate.setTag(appViewHolder);
            return appViewHolder;
        }
        return (AppViewHolder) view.getTag();
    }
}
