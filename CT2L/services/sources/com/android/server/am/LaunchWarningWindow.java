package com.android.server.am;

import android.R;
import android.app.Dialog;
import android.content.Context;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

public final class LaunchWarningWindow extends Dialog {
    public LaunchWarningWindow(Context context, ActivityRecord cur, ActivityRecord next) {
        super(context, R.style.Widget.DeviceDefault.Light.ExpandableListView.White);
        requestWindowFeature(3);
        getWindow().setType(2003);
        getWindow().addFlags(24);
        setContentView(R.layout.chooser_grid_preview_file);
        setTitle(context.getText(R.string.guest_name));
        TypedValue out = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.alertDialogIcon, out, true);
        getWindow().setFeatureDrawableResource(3, out.resourceId);
        ImageView icon = (ImageView) findViewById(R.id.fitXY);
        icon.setImageDrawable(next.info.applicationInfo.loadIcon(context.getPackageManager()));
        TextView text = (TextView) findViewById(R.id.five);
        text.setText(context.getResources().getString(R.string.hardware, next.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
        ImageView icon2 = (ImageView) findViewById(R.id.flagDefault);
        icon2.setImageDrawable(cur.info.applicationInfo.loadIcon(context.getPackageManager()));
        TextView text2 = (TextView) findViewById(R.id.flagEnableAccessibilityVolume);
        text2.setText(context.getResources().getString(R.string.harmful_app_warning_open_anyway, cur.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
    }
}
