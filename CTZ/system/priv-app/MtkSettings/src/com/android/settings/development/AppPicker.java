package com.android.settings.development;

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
import com.android.settings.R;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes.dex */
public class AppPicker extends ListActivity {
    private static final Comparator<MyApplicationInfo> sDisplayNameComparator = new Comparator<MyApplicationInfo>() { // from class: com.android.settings.development.AppPicker.1
        private final Collator collator = Collator.getInstance();

        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public final int compare(MyApplicationInfo myApplicationInfo, MyApplicationInfo myApplicationInfo2) {
            return this.collator.compare(myApplicationInfo.label, myApplicationInfo2.label);
        }
    };
    private AppListAdapter mAdapter;
    private boolean mDebuggableOnly;
    private String mPermissionName;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPermissionName = getIntent().getStringExtra("com.android.settings.extra.REQUESTIING_PERMISSION");
        this.mDebuggableOnly = getIntent().getBooleanExtra("com.android.settings.extra.DEBUGGABLE", false);
        this.mAdapter = new AppListAdapter(this);
        if (this.mAdapter.getCount() <= 0) {
            finish();
        } else {
            setListAdapter(this.mAdapter);
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
    }

    @Override // android.app.Activity
    protected void onStop() {
        super.onStop();
    }

    @Override // android.app.ListActivity
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        MyApplicationInfo item = this.mAdapter.getItem(i);
        Intent intent = new Intent();
        if (item.info != null) {
            intent.setAction(item.info.packageName);
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

        /* JADX WARN: Removed duplicated region for block: B:32:0x0083  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public AppListAdapter(Context context) throws PackageManager.NameNotFoundException {
            boolean z;
            super(context, 0);
            this.mPackageInfoList = new ArrayList();
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            List<ApplicationInfo> installedApplications = context.getPackageManager().getInstalledApplications(0);
            for (int i = 0; i < installedApplications.size(); i++) {
                ApplicationInfo applicationInfo = installedApplications.get(i);
                if (applicationInfo.uid != 1000 && (!AppPicker.this.mDebuggableOnly || (applicationInfo.flags & 2) != 0 || !"user".equals(Build.TYPE))) {
                    if (AppPicker.this.mPermissionName != null) {
                        try {
                            PackageInfo packageInfo = AppPicker.this.getPackageManager().getPackageInfo(applicationInfo.packageName, 4096);
                            if (packageInfo.requestedPermissions != null) {
                                String[] strArr = packageInfo.requestedPermissions;
                                int length = strArr.length;
                                int i2 = 0;
                                while (true) {
                                    if (i2 < length) {
                                        if (!strArr[i2].equals(AppPicker.this.mPermissionName)) {
                                            i2++;
                                        } else {
                                            z = true;
                                            break;
                                        }
                                    } else {
                                        z = false;
                                        break;
                                    }
                                }
                                if (z) {
                                    MyApplicationInfo myApplicationInfo = AppPicker.this.new MyApplicationInfo();
                                    myApplicationInfo.info = applicationInfo;
                                    myApplicationInfo.label = myApplicationInfo.info.loadLabel(AppPicker.this.getPackageManager()).toString();
                                    this.mPackageInfoList.add(myApplicationInfo);
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                }
            }
            Collections.sort(this.mPackageInfoList, AppPicker.sDisplayNameComparator);
            MyApplicationInfo myApplicationInfo2 = AppPicker.this.new MyApplicationInfo();
            myApplicationInfo2.label = context.getText(R.string.no_application);
            this.mPackageInfoList.add(0, myApplicationInfo2);
            addAll(this.mPackageInfoList);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            AppViewHolder appViewHolderCreateOrRecycle = AppViewHolder.createOrRecycle(this.mInflater, view);
            View view2 = appViewHolderCreateOrRecycle.rootView;
            MyApplicationInfo item = getItem(i);
            appViewHolderCreateOrRecycle.appName.setText(item.label);
            if (item.info != null) {
                appViewHolderCreateOrRecycle.appIcon.setImageDrawable(item.info.loadIcon(AppPicker.this.getPackageManager()));
                appViewHolderCreateOrRecycle.summary.setText(item.info.packageName);
            } else {
                appViewHolderCreateOrRecycle.appIcon.setImageDrawable(null);
                appViewHolderCreateOrRecycle.summary.setText("");
            }
            appViewHolderCreateOrRecycle.disabled.setVisibility(8);
            return view2;
        }
    }
}
