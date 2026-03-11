package com.android.settings.applications;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppOpsState;
import java.util.List;

public class AppOpsCategory extends ListFragment implements LoaderManager.LoaderCallbacks<List<AppOpsState.AppOpEntry>> {
    AppListAdapter mAdapter;
    String mCurrentPkgName;
    AppOpsState mState;

    public AppOpsCategory() {
    }

    public AppOpsCategory(AppOpsState.OpsTemplate template) {
        Bundle args = new Bundle();
        args.putParcelable("template", template);
        setArguments(args);
    }

    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = this.mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = this.mLastDensity != res.getDisplayMetrics().densityDpi;
            if (!densityChanged && (configChanges & 772) == 0) {
                return false;
            }
            this.mLastDensity = res.getDisplayMetrics().densityDpi;
            return true;
        }
    }

    public static class PackageIntentReceiver extends BroadcastReceiver {
        final AppListLoader mLoader;

        public PackageIntentReceiver(AppListLoader loader) {
            this.mLoader = loader;
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme("package");
            this.mLoader.getContext().registerReceiver(this, filter);
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            this.mLoader.getContext().registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mLoader.onContentChanged();
        }
    }

    public static class AppListLoader extends AsyncTaskLoader<List<AppOpsState.AppOpEntry>> {
        List<AppOpsState.AppOpEntry> mApps;
        final InterestingConfigChanges mLastConfig;
        PackageIntentReceiver mPackageObserver;
        final AppOpsState mState;
        final AppOpsState.OpsTemplate mTemplate;

        public AppListLoader(Context context, AppOpsState state, AppOpsState.OpsTemplate template) {
            super(context);
            this.mLastConfig = new InterestingConfigChanges();
            this.mState = state;
            this.mTemplate = template;
        }

        @Override
        public List<AppOpsState.AppOpEntry> loadInBackground() {
            return this.mState.buildState(this.mTemplate);
        }

        @Override
        public void deliverResult(List<AppOpsState.AppOpEntry> apps) {
            if (isReset() && apps != null) {
                onReleaseResources(apps);
            }
            this.mApps = apps;
            if (isStarted()) {
                super.deliverResult(apps);
            }
            if (apps != null) {
                onReleaseResources(apps);
            }
        }

        @Override
        protected void onStartLoading() {
            onContentChanged();
            if (this.mApps != null) {
                deliverResult(this.mApps);
            }
            if (this.mPackageObserver == null) {
                this.mPackageObserver = new PackageIntentReceiver(this);
            }
            boolean configChange = this.mLastConfig.applyNewConfig(getContext().getResources());
            if (takeContentChanged() || this.mApps == null || configChange) {
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public void onCanceled(List<AppOpsState.AppOpEntry> apps) {
            super.onCanceled(apps);
            onReleaseResources(apps);
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
            if (this.mApps != null) {
                onReleaseResources(this.mApps);
                this.mApps = null;
            }
            if (this.mPackageObserver != null) {
                getContext().unregisterReceiver(this.mPackageObserver);
                this.mPackageObserver = null;
            }
        }

        protected void onReleaseResources(List<AppOpsState.AppOpEntry> apps) {
        }
    }

    public static class AppListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        List<AppOpsState.AppOpEntry> mList;
        private final Resources mResources;
        private final AppOpsState mState;

        public AppListAdapter(Context context, AppOpsState state) {
            this.mResources = context.getResources();
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mState = state;
        }

        public void setData(List<AppOpsState.AppOpEntry> data) {
            this.mList = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (this.mList != null) {
                return this.mList.size();
            }
            return 0;
        }

        @Override
        public AppOpsState.AppOpEntry getItem(int position) {
            return this.mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.app_ops_item, parent, false);
            } else {
                view = convertView;
            }
            AppOpsState.AppOpEntry item = getItem(position);
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(item.getAppEntry().getIcon());
            ((TextView) view.findViewById(R.id.app_name)).setText(item.getAppEntry().getLabel());
            ((TextView) view.findViewById(R.id.op_name)).setText(item.getSummaryText(this.mState));
            ((TextView) view.findViewById(R.id.op_time)).setText(item.getTimeText(this.mResources, false));
            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mState = new AppOpsState(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No applications");
        setHasOptionsMenu(true);
        this.mAdapter = new AppListAdapter(getActivity(), this.mState);
        setListAdapter(this.mAdapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    private void startApplicationDetailsActivity() {
        Bundle args = new Bundle();
        args.putString("package", this.mCurrentPkgName);
        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.startPreferencePanel(AppOpsDetails.class.getName(), args, R.string.app_ops_settings, null, this, 1);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AppOpsState.AppOpEntry entry = this.mAdapter.getItem(position);
        if (entry != null) {
            this.mCurrentPkgName = entry.getAppEntry().getApplicationInfo().packageName;
            startApplicationDetailsActivity();
        }
    }

    @Override
    public Loader<List<AppOpsState.AppOpEntry>> onCreateLoader(int id, Bundle args) {
        Bundle fargs = getArguments();
        AppOpsState.OpsTemplate template = null;
        if (fargs != null) {
            template = (AppOpsState.OpsTemplate) fargs.getParcelable("template");
        }
        return new AppListLoader(getActivity(), this.mState, template);
    }

    @Override
    public void onLoadFinished(Loader<List<AppOpsState.AppOpEntry>> loader, List<AppOpsState.AppOpEntry> data) {
        this.mAdapter.setData(data);
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AppOpsState.AppOpEntry>> loader) {
        this.mAdapter.setData(null);
    }
}
