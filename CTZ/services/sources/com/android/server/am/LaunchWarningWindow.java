package com.android.server.am;

import android.R;
import android.app.Dialog;
import android.content.Context;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

public final class LaunchWarningWindow extends Dialog {
    public LaunchWarningWindow(Context context, ActivityRecord activityRecord, ActivityRecord activityRecord2) {
        super(context, R.style.Theme.DeviceDefault.Light.SearchBar);
        requestWindowFeature(3);
        getWindow().setType(2003);
        getWindow().addFlags(24);
        setContentView(R.layout.dialog_title_material);
        setTitle(context.getText(R.string.config_wlan_network_service_class));
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.alertDialogIcon, typedValue, true);
        getWindow().setFeatureDrawableResource(3, typedValue.resourceId);
        ((ImageView) findViewById(R.id.knownSigner)).setImageDrawable(activityRecord2.info.applicationInfo.loadIcon(context.getPackageManager()));
        ((TextView) findViewById(R.id.label)).setText(context.getResources().getString(R.string.config_wlan_data_service_package, activityRecord2.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
        ((ImageView) findViewById(R.id.hide_from_picker)).setImageDrawable(activityRecord.info.applicationInfo.loadIcon(context.getPackageManager()));
        ((TextView) findViewById(R.id.high)).setText(context.getResources().getString(R.string.config_wlan_data_service_class, activityRecord.info.applicationInfo.loadLabel(context.getPackageManager()).toString()));
    }
}
