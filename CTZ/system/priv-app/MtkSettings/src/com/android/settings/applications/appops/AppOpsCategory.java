package com.android.settings.applications.appops;

import android.app.AppOpsManager;
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
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.applications.appops.AppOpsState;
import java.util.List;

/* loaded from: classes.dex */
public class AppOpsCategory extends ListFragment implements LoaderManager.LoaderCallbacks<List<AppOpsState.AppOpEntry>> {
    AppListAdapter mAdapter;
    AppOpsState mState;

    public AppOpsCategory() {
    }

    public AppOpsCategory(AppOpsState.OpsTemplate opsTemplate) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("template", opsTemplate);
        setArguments(bundle);
    }

    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources resources) {
            int iUpdateFrom = this.mLastConfiguration.updateFrom(resources.getConfiguration());
            if (!(this.mLastDensity != resources.getDisplayMetrics().densityDpi) && (iUpdateFrom & 772) == 0) {
                return false;
            }
            this.mLastDensity = resources.getDisplayMetrics().densityDpi;
            return true;
        }
    }

    public static class PackageIntentReceiver extends BroadcastReceiver {
        final AppListLoader mLoader;

        public PackageIntentReceiver(AppListLoader appListLoader) {
            this.mLoader = appListLoader;
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addDataScheme("package");
            this.mLoader.getContext().registerReceiver(this, intentFilter);
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            this.mLoader.getContext().registerReceiver(this, intentFilter2);
        }

        @Override // android.content.BroadcastReceiver
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

        public AppListLoader(Context context, AppOpsState appOpsState, AppOpsState.OpsTemplate opsTemplate) {
            super(context);
            this.mLastConfig = new InterestingConfigChanges();
            this.mState = appOpsState;
            this.mTemplate = opsTemplate;
        }

        /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
        @Override // android.content.AsyncTaskLoader
        public List<AppOpsState.AppOpEntry> loadInBackground() {
            return this.mState.buildState(this.mTemplate, 0, null, AppOpsState.LABEL_COMPARATOR);
        }

        /* JADX DEBUG: Method merged with bridge method: deliverResult(Ljava/lang/Object;)V */
        @Override // android.content.Loader
        public void deliverResult(List<AppOpsState.AppOpEntry> list) {
            if (isReset() && list != null) {
                onReleaseResources(list);
            }
            this.mApps = list;
            if (isStarted()) {
                super.deliverResult((AppListLoader) list);
            }
            if (list != null) {
                onReleaseResources(list);
            }
        }

        @Override // android.content.Loader
        protected void onStartLoading() {
            onContentChanged();
            if (this.mApps != null) {
                deliverResult(this.mApps);
            }
            if (this.mPackageObserver == null) {
                this.mPackageObserver = new PackageIntentReceiver(this);
            }
            boolean zApplyNewConfig = this.mLastConfig.applyNewConfig(getContext().getResources());
            if (takeContentChanged() || this.mApps == null || zApplyNewConfig) {
                forceLoad();
            }
        }

        @Override // android.content.Loader
        protected void onStopLoading() {
            cancelLoad();
        }

        /* JADX DEBUG: Method merged with bridge method: onCanceled(Ljava/lang/Object;)V */
        @Override // android.content.AsyncTaskLoader
        public void onCanceled(List<AppOpsState.AppOpEntry> list) {
            super.onCanceled((AppListLoader) list);
            onReleaseResources(list);
        }

        @Override // android.content.Loader
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

        protected void onReleaseResources(List<AppOpsState.AppOpEntry> list) {
        }
    }

    public static class AppListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        List<AppOpsState.AppOpEntry> mList;
        private final Resources mResources;
        private final AppOpsState mState;

        public AppListAdapter(Context context, AppOpsState appOpsState) {
            this.mResources = context.getResources();
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mState = appOpsState;
        }

        public void setData(List<AppOpsState.AppOpEntry> list) {
            this.mList = list;
            notifyDataSetChanged();
        }

        @Override // android.widget.Adapter
        public int getCount() {
            if (this.mList != null) {
                return this.mList.size();
            }
            return 0;
        }

        /* JADX DEBUG: Method merged with bridge method: getItem(I)Ljava/lang/Object; */
        @Override // android.widget.Adapter
        public AppOpsState.AppOpEntry getItem(int i) {
            return this.mList.get(i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(R.layout.app_ops_item, viewGroup, false);
            }
            AppOpsState.AppOpEntry item = getItem(i);
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(item.getAppEntry().getIcon());
            ((TextView) view.findViewById(R.id.app_name)).setText(item.getAppEntry().getLabel());
            ((TextView) view.findViewById(R.id.op_name)).setText(item.getTimeText(this.mResources, false));
            view.findViewById(R.id.op_time).setVisibility(8);
            ((Switch) view.findViewById(R.id.op_switch)).setChecked(item.getPrimaryOpMode() == 0);
            return view;
        }
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mState = new AppOpsState(getActivity());
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setEmptyText("No applications");
        setHasOptionsMenu(true);
        this.mAdapter = new AppListAdapter(getActivity(), this.mState);
        setListAdapter(this.mAdapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override // android.app.ListFragment
    public void onListItemClick(ListView listView, View view, int i, long j) {
        AppOpsState.AppOpEntry item = this.mAdapter.getItem(i);
        if (item != null) {
            Switch r3 = (Switch) view.findViewById(R.id.op_switch);
            boolean z = !r3.isChecked();
            r3.setChecked(z);
            int i2 = 0;
            AppOpsManager.OpEntry opEntry = item.getOpEntry(0);
            if (!z) {
                i2 = 1;
            }
            this.mState.getAppOpsManager().setMode(opEntry.getOp(), item.getAppEntry().getApplicationInfo().uid, item.getAppEntry().getApplicationInfo().packageName, i2);
            item.overridePrimaryOpMode(i2);
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<List<AppOpsState.AppOpEntry>> onCreateLoader(int i, Bundle bundle) {
        AppOpsState.OpsTemplate opsTemplate;
        Bundle arguments = getArguments();
        if (arguments != null) {
            opsTemplate = (AppOpsState.OpsTemplate) arguments.getParcelable("template");
        } else {
            opsTemplate = null;
        }
        return new AppListLoader(getActivity(), this.mState, opsTemplate);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<List<AppOpsState.AppOpEntry>> loader, List<AppOpsState.AppOpEntry> list) {
        this.mAdapter.setData(list);
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<List<AppOpsState.AppOpEntry>> loader) {
        this.mAdapter.setData(null);
    }
}
