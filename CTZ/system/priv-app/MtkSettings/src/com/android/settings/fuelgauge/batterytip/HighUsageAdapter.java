package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.support.v7.widget.RecyclerView;
import android.util.IconDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.utils.StringUtil;
import java.util.List;

/* loaded from: classes.dex */
public class HighUsageAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final Context mContext;
    private final List<AppInfo> mHighUsageAppList;
    private final IconDrawableFactory mIconDrawableFactory;
    private final PackageManager mPackageManager;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView appTime;
        public View view;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.appIcon = (ImageView) view.findViewById(R.id.app_icon);
            this.appName = (TextView) view.findViewById(R.id.app_name);
            this.appTime = (TextView) view.findViewById(R.id.app_screen_time);
        }
    }

    public HighUsageAdapter(Context context, List<AppInfo> list) {
        this.mContext = context;
        this.mHighUsageAppList = list;
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        this.mPackageManager = context.getPackageManager();
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.mContext).inflate(R.layout.app_high_usage_item, viewGroup, false));
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        AppInfo appInfo = this.mHighUsageAppList.get(i);
        viewHolder.appIcon.setImageDrawable(Utils.getBadgedIcon(this.mIconDrawableFactory, this.mPackageManager, appInfo.packageName, UserHandle.getUserId(appInfo.uid)));
        viewHolder.appName.setText(Utils.getApplicationLabel(this.mContext, appInfo.packageName));
        if (appInfo.screenOnTimeMs != 0) {
            viewHolder.appTime.setText(StringUtil.formatElapsedTime(this.mContext, appInfo.screenOnTimeMs, false));
        }
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mHighUsageAppList.size();
    }
}
