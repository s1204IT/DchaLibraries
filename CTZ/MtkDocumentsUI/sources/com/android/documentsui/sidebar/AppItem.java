package com.android.documentsui.sidebar;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.ActionHandler;
import com.android.documentsui.R;

class AppItem extends Item {
    public final ResolveInfo info;
    private final ActionHandler mActionHandler;

    public AppItem(ResolveInfo resolveInfo, ActionHandler actionHandler) {
        super(R.layout.item_root, getStringId(resolveInfo));
        this.info = resolveInfo;
        this.mActionHandler = actionHandler;
    }

    private static String getStringId(ResolveInfo resolveInfo) {
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        return String.format("AppItem{%s/%s}", activityInfo.applicationInfo.packageName, activityInfo.name);
    }

    @Override
    boolean showAppDetails() {
        this.mActionHandler.showAppDetails(this.info);
        return true;
    }

    @Override
    void bindView(View view) {
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        TextView textView2 = (TextView) view.findViewById(android.R.id.summary);
        PackageManager packageManager = view.getContext().getPackageManager();
        imageView.setImageDrawable(this.info.loadIcon(packageManager));
        textView.setText(this.info.loadLabel(packageManager));
        textView2.setVisibility(8);
    }

    @Override
    boolean isRoot() {
        return false;
    }

    @Override
    void open() {
        this.mActionHandler.openRoot(this.info);
    }

    public String toString() {
        return "AppItem{id=" + this.stringId + ", resolveInfo=" + this.info + "}";
    }
}
