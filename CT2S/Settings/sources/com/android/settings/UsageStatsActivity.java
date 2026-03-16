package com.android.settings;

import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class UsageStatsActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private UsageStatsAdapter mAdapter;
    private LayoutInflater mInflater;
    private PackageManager mPm;
    private UsageStatsManager mUsageStatsManager;

    public static class AppNameComparator implements Comparator<UsageStats> {
        private Map<String, String> mAppLabelList;

        AppNameComparator(Map<String, String> appList) {
            this.mAppLabelList = appList;
        }

        @Override
        public final int compare(UsageStats a, UsageStats b) {
            String alabel = this.mAppLabelList.get(a.getPackageName());
            String blabel = this.mAppLabelList.get(b.getPackageName());
            return alabel.compareTo(blabel);
        }
    }

    public static class LastTimeUsedComparator implements Comparator<UsageStats> {
        @Override
        public final int compare(UsageStats a, UsageStats b) {
            return (int) (b.getLastTimeUsed() - a.getLastTimeUsed());
        }
    }

    public static class UsageTimeComparator implements Comparator<UsageStats> {
        @Override
        public final int compare(UsageStats a, UsageStats b) {
            return (int) (b.getTotalTimeInForeground() - a.getTotalTimeInForeground());
        }
    }

    static class AppViewHolder {
        TextView lastTimeUsed;
        TextView pkgName;
        TextView usageTime;

        AppViewHolder() {
        }
    }

    class UsageStatsAdapter extends BaseAdapter {
        private AppNameComparator mAppLabelComparator;
        private int mDisplayOrder = 0;
        private LastTimeUsedComparator mLastTimeUsedComparator = new LastTimeUsedComparator();
        private UsageTimeComparator mUsageTimeComparator = new UsageTimeComparator();
        private final ArrayMap<String, String> mAppLabelMap = new ArrayMap<>();
        private final ArrayList<UsageStats> mPackageStats = new ArrayList<>();

        UsageStatsAdapter() {
            Calendar cal = Calendar.getInstance();
            cal.add(6, -5);
            List<UsageStats> stats = UsageStatsActivity.this.mUsageStatsManager.queryUsageStats(4, cal.getTimeInMillis(), System.currentTimeMillis());
            if (stats != null) {
                ArrayMap<String, UsageStats> map = new ArrayMap<>();
                int statCount = stats.size();
                for (int i = 0; i < statCount; i++) {
                    UsageStats pkgStats = stats.get(i);
                    try {
                        ApplicationInfo appInfo = UsageStatsActivity.this.mPm.getApplicationInfo(pkgStats.getPackageName(), 0);
                        String label = appInfo.loadLabel(UsageStatsActivity.this.mPm).toString();
                        this.mAppLabelMap.put(pkgStats.getPackageName(), label);
                        UsageStats existingStats = map.get(pkgStats.getPackageName());
                        if (existingStats == null) {
                            map.put(pkgStats.getPackageName(), pkgStats);
                        } else {
                            existingStats.add(pkgStats);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                this.mPackageStats.addAll(map.values());
                this.mAppLabelComparator = new AppNameComparator(this.mAppLabelMap);
                sortList();
            }
        }

        @Override
        public int getCount() {
            return this.mPackageStats.size();
        }

        @Override
        public Object getItem(int position) {
            return this.mPackageStats.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder;
            if (convertView == null) {
                convertView = UsageStatsActivity.this.mInflater.inflate(R.layout.usage_stats_item, (ViewGroup) null);
                holder = new AppViewHolder();
                holder.pkgName = (TextView) convertView.findViewById(R.id.package_name);
                holder.lastTimeUsed = (TextView) convertView.findViewById(R.id.last_time_used);
                holder.usageTime = (TextView) convertView.findViewById(R.id.usage_time);
                convertView.setTag(holder);
            } else {
                holder = (AppViewHolder) convertView.getTag();
            }
            UsageStats pkgStats = this.mPackageStats.get(position);
            if (pkgStats != null) {
                String label = this.mAppLabelMap.get(pkgStats.getPackageName());
                holder.pkgName.setText(label);
                holder.lastTimeUsed.setText(DateUtils.formatSameDayTime(pkgStats.getLastTimeUsed(), System.currentTimeMillis(), 2, 2));
                holder.usageTime.setText(DateUtils.formatElapsedTime(pkgStats.getTotalTimeInForeground() / 1000));
            } else {
                Log.w("UsageStatsActivity", "No usage stats info for package:" + position);
            }
            return convertView;
        }

        void sortList(int sortOrder) {
            if (this.mDisplayOrder != sortOrder) {
                this.mDisplayOrder = sortOrder;
                sortList();
            }
        }

        private void sortList() {
            if (this.mDisplayOrder == 0) {
                Collections.sort(this.mPackageStats, this.mUsageTimeComparator);
            } else if (this.mDisplayOrder == 1) {
                Collections.sort(this.mPackageStats, this.mLastTimeUsedComparator);
            } else if (this.mDisplayOrder == 2) {
                Collections.sort(this.mPackageStats, this.mAppLabelComparator);
            }
            notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.usage_stats);
        this.mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats");
        this.mInflater = (LayoutInflater) getSystemService("layout_inflater");
        this.mPm = getPackageManager();
        Spinner typeSpinner = (Spinner) findViewById(R.id.typeSpinner);
        typeSpinner.setOnItemSelectedListener(this);
        ListView listView = (ListView) findViewById(R.id.pkg_list);
        this.mAdapter = new UsageStatsAdapter();
        listView.setAdapter((ListAdapter) this.mAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.mAdapter.sortList(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
