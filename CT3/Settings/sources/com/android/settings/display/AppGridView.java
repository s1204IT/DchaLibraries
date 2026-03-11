package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppGridView extends GridView {
    public AppGridView(Context context) {
        this(context, null);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleResId) {
        super(context, attrs, defStyleAttr, defStyleResId);
        setNumColumns(-1);
        int columnWidth = getResources().getDimensionPixelSize(R.dimen.screen_zoom_preview_app_icon_width);
        setColumnWidth(columnWidth);
        setAdapter((ListAdapter) new AppsAdapter(context, R.layout.screen_zoom_preview_app_icon, android.R.id.text1, android.R.id.icon1));
    }

    private static class AppsAdapter extends ArrayAdapter<ActivityEntry> {
        private final int mIconResId;
        private final PackageManager mPackageManager;

        public AppsAdapter(Context context, int layout, int textResId, int iconResId) {
            super(context, layout, textResId);
            this.mIconResId = iconResId;
            this.mPackageManager = context.getPackageManager();
            loadAllApps();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ActivityEntry entry = getItem(position);
            ImageView iconView = (ImageView) view.findViewById(this.mIconResId);
            Drawable icon = entry.info.loadIcon(this.mPackageManager);
            iconView.setImageDrawable(icon);
            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        private void loadAllApps() {
            Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            PackageManager pm = this.mPackageManager;
            ArrayList<ActivityEntry> results = new ArrayList<>();
            List<ResolveInfo> infos = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : infos) {
                CharSequence label = info.loadLabel(pm);
                if (label != null) {
                    results.add(new ActivityEntry(info, label.toString()));
                }
            }
            Collections.sort(results);
            addAll(results);
        }
    }

    private static class ActivityEntry implements Comparable<ActivityEntry> {
        public final ResolveInfo info;
        public final String label;

        public ActivityEntry(ResolveInfo info, String label) {
            this.info = info;
            this.label = label;
        }

        @Override
        public int compareTo(ActivityEntry entry) {
            return this.label.compareToIgnoreCase(entry.label);
        }

        public String toString() {
            return this.label;
        }
    }
}
