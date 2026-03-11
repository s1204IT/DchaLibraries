package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.InstalledAppDetails;

public class AppHeader {
    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, String pkgName, int uid) {
        createAppHeader(fragment, icon, label, pkgName, uid, 0, null);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, String pkgName, int uid, Intent externalSettings) {
        createAppHeader(fragment, icon, label, pkgName, uid, 0, externalSettings);
    }

    public static void createAppHeader(Activity activity, Drawable icon, CharSequence label, String pkgName, int uid, ViewGroup pinnedHeader) {
        View bar = activity.getLayoutInflater().inflate(R.layout.app_header, pinnedHeader, false);
        setupHeaderView(activity, icon, label, pkgName, uid, false, 0, bar, null);
        pinnedHeader.addView(bar);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, String pkgName, int uid, int tintColorRes) {
        createAppHeader(fragment, icon, label, pkgName, uid, tintColorRes, null);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, String pkgName, int uid, int tintColorRes, Intent externalSettings) {
        View bar = fragment.setPinnedHeaderView(R.layout.app_header);
        setupHeaderView(fragment.getActivity(), icon, label, pkgName, uid, includeAppInfo(fragment), tintColorRes, bar, externalSettings);
    }

    public static View setupHeaderView(final Activity activity, Drawable icon, CharSequence label, final String pkgName, final int uid, final boolean includeAppInfo, int tintColorRes, View bar, final Intent externalSettings) {
        ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
        appIcon.setImageDrawable(icon);
        if (tintColorRes != 0) {
            appIcon.setImageTintList(ColorStateList.valueOf(activity.getColor(tintColorRes)));
        }
        TextView appName = (TextView) bar.findViewById(R.id.app_name);
        appName.setText(label);
        if (pkgName != null && !pkgName.equals("os")) {
            bar.setClickable(true);
            bar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (includeAppInfo) {
                        AppInfoBase.startAppInfoFragment((Class<?>) InstalledAppDetails.class, R.string.application_info_label, pkgName, uid, activity, 1);
                    } else {
                        activity.finish();
                    }
                }
            });
            if (externalSettings != null) {
                View appSettings = bar.findViewById(R.id.app_settings);
                appSettings.setVisibility(0);
                appSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.startActivity(externalSettings);
                    }
                });
            }
        }
        return bar;
    }

    public static boolean includeAppInfo(Fragment fragment) {
        Bundle args = fragment.getArguments();
        boolean showInfo = true;
        if (args != null && args.getBoolean("hideInfoButton", false)) {
            showInfo = false;
        }
        Intent intent = fragment.getActivity().getIntent();
        if (intent != null && intent.getBooleanExtra("hideInfoButton", false)) {
            return false;
        }
        return showInfo;
    }
}
