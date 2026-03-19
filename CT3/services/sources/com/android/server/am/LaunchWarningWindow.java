package com.android.server.am;

import android.R;
import android.app.Dialog;
import android.content.Context;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

public final class LaunchWarningWindow extends Dialog {
    public LaunchWarningWindow(Context context, ActivityRecord cur, ActivityRecord next) {
        super(context, R.style.Widget.DeviceDefault.Light.FragmentBreadCrumbs);
        requestWindowFeature(3);
        getWindow().setType(2003);
        getWindow().addFlags(24);
        setContentView(R.layout.dialog_custom_title_material);
        setTitle(context.getText(R.string.emailTypeCustom));
        TypedValue out = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.alertDialogIcon, out, true);
        getWindow().setFeatureDrawableResource(3, out.resourceId);
        ImageView icon = (ImageView) findViewById(R.id.input_method_nav_horizontal);
        icon.setImageDrawable(next.info.applicationInfo.loadIcon(context.getPackageManager()));
        TextView text = (TextView) findViewById(R.id.input_method_nav_ime_switcher);
        text.setText(context.getResources().getString(R.string.emailTypeHome, next.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
        ImageView icon2 = (ImageView) findViewById(R.id.input_method_nav_inflater);
        icon2.setImageDrawable(cur.info.applicationInfo.loadIcon(context.getPackageManager()));
        TextView text2 = (TextView) findViewById(R.id.input_method_navigation_bar_view);
        text2.setText(context.getResources().getString(R.string.emailTypeMobile, cur.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
    }
}
