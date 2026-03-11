package com.android.settings;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.android.settings.applications.AppViewHolder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppPicker extends ListActivity {
    private static final Comparator<MyApplicationInfo> sDisplayNameComparator = new Comparator<MyApplicationInfo>() {
        private final Collator collator = Collator.getInstance();

        @Override
        public final int compare(MyApplicationInfo a, MyApplicationInfo b) {
            return this.collator.compare(a.label, b.label);
        }
    };
    private AppListAdapter mAdapter;
    private boolean mDebuggableOnly;
    private String mPermissionName;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPermissionName = getIntent().getStringExtra("com.android.settings.extra.REQUESTIING_PERMISSION");
        this.mDebuggableOnly = getIntent().getBooleanExtra("com.android.settings.extra.DEBUGGABLE", false);
        this.mAdapter = new AppListAdapter(this);
        if (this.mAdapter.getCount() <= 0) {
            finish();
        } else {
            setListAdapter(this.mAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MyApplicationInfo app = this.mAdapter.getItem(position);
        Intent intent = new Intent();
        if (app.info != null) {
            intent.setAction(app.info.packageName);
        }
        setResult(-1, intent);
        finish();
    }

    class MyApplicationInfo {
        ApplicationInfo info;
        CharSequence label;

        MyApplicationInfo() {
        }
    }

    public class AppListAdapter extends ArrayAdapter<MyApplicationInfo> {
        private final LayoutInflater mInflater;
        private final List<MyApplicationInfo> mPackageInfoList;

        public AppListAdapter(Context context) {
            super(context, 0);
            this.mPackageInfoList = new ArrayList();
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            List<ApplicationInfo> pkgs = context.getPackageManager().getInstalledApplications(0);
            for (int i = 0; i < pkgs.size(); i++) {
                ApplicationInfo ai = pkgs.get(i);
                if (ai.uid != 1000 && (!AppPicker.this.mDebuggableOnly || (ai.flags & 2) != 0 || !"user".equals(Build.TYPE))) {
                    if (AppPicker.this.mPermissionName != null) {
                        boolean requestsPermission = false;
                        try {
                            PackageInfo pi = AppPicker.this.getPackageManager().getPackageInfo(ai.packageName, 4096);
                            if (pi.requestedPermissions != null) {
                                String[] strArr = pi.requestedPermissions;
                                int length = strArr.length;
                                int i2 = 0;
                                while (true) {
                                    if (i2 >= length) {
                                        break;
                                    }
                                    String requestedPermission = strArr[i2];
                                    if (!requestedPermission.equals(AppPicker.this.mPermissionName)) {
                                        i2++;
                                    } else {
                                        requestsPermission = true;
                                        break;
                                    }
                                }
                                if (requestsPermission) {
                                    MyApplicationInfo info = AppPicker.this.new MyApplicationInfo();
                                    info.info = ai;
                                    info.label = info.info.loadLabel(AppPicker.this.getPackageManager()).toString();
                                    this.mPackageInfoList.add(info);
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                }
            }
            Collections.sort(this.mPackageInfoList, AppPicker.sDisplayNameComparator);
            MyApplicationInfo info2 = AppPicker.this.new MyApplicationInfo();
            info2.label = context.getText(R.string.no_application);
            this.mPackageInfoList.add(0, info2);
            addAll(this.mPackageInfoList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(this.mInflater, convertView);
            View convertView2 = holder.rootView;
            MyApplicationInfo info = getItem(position);
            holder.appName.setText(info.label);
            if (info.info != null) {
                holder.appIcon.setImageDrawable(info.info.loadIcon(AppPicker.this.getPackageManager()));
                holder.summary.setText(info.info.packageName);
            } else {
                holder.appIcon.setImageDrawable(null);
                holder.summary.setText("");
            }
            holder.disabled.setVisibility(8);
            return convertView2;
        }
    }
}
