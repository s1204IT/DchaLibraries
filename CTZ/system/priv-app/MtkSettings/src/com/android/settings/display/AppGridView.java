package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.IconDrawableFactory;
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

/* loaded from: classes.dex */
public class AppGridView extends GridView {
    public AppGridView(Context context) {
        this(context, null);
    }

    public AppGridView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AppGridView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AppGridView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setNumColumns(-1);
        setColumnWidth(getResources().getDimensionPixelSize(R.dimen.screen_zoom_preview_app_icon_width));
        setAdapter((ListAdapter) new AppsAdapter(context, R.layout.screen_zoom_preview_app_icon, android.R.id.text1, android.R.id.icon1));
    }

    public static class AppsAdapter extends ArrayAdapter<ActivityEntry> {
        private final int mIconResId;
        private final PackageManager mPackageManager;

        public AppsAdapter(Context context, int i, int i2, int i3) {
            super(context, i, i2);
            this.mIconResId = i3;
            this.mPackageManager = context.getPackageManager();
            loadAllApps();
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            View view2 = super.getView(i, view, viewGroup);
            ((ImageView) view2.findViewById(this.mIconResId)).setImageDrawable(getItem(i).getIcon());
            return view2;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public boolean hasStableIds() {
            return true;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.BaseAdapter, android.widget.ListAdapter
        public boolean isEnabled(int i) {
            return false;
        }

        private void loadAllApps() {
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.addCategory("android.intent.category.LAUNCHER");
            PackageManager packageManager = this.mPackageManager;
            ArrayList arrayList = new ArrayList();
            List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 0);
            IconDrawableFactory iconDrawableFactoryNewInstance = IconDrawableFactory.newInstance(getContext());
            for (ResolveInfo resolveInfo : listQueryIntentActivities) {
                CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(packageManager);
                if (charSequenceLoadLabel != null) {
                    arrayList.add(new ActivityEntry(resolveInfo, charSequenceLoadLabel.toString(), iconDrawableFactoryNewInstance));
                }
            }
            Collections.sort(arrayList);
            addAll(arrayList);
        }
    }

    public static class ActivityEntry implements Comparable<ActivityEntry> {
        public final ResolveInfo info;
        public final String label;
        private final IconDrawableFactory mIconFactory;
        private final int mUserId = UserHandle.myUserId();

        public ActivityEntry(ResolveInfo resolveInfo, String str, IconDrawableFactory iconDrawableFactory) {
            this.info = resolveInfo;
            this.label = str;
            this.mIconFactory = iconDrawableFactory;
        }

        /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
        @Override // java.lang.Comparable
        public int compareTo(ActivityEntry activityEntry) {
            return this.label.compareToIgnoreCase(activityEntry.label);
        }

        public String toString() {
            return this.label;
        }

        public Drawable getIcon() {
            return this.mIconFactory.getBadgedIcon(this.info.activityInfo, this.info.activityInfo.applicationInfo, this.mUserId);
        }
    }
}
