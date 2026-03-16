package com.android.settings.notification;

import android.R;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.List;

public abstract class ManagedServiceSettings extends ListFragment {
    private ContentResolver mCR;
    private ServiceListAdapter mListAdapter;
    private PackageManager mPM;
    private final HashSet<ComponentName> mEnabledServices = new HashSet<>();
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ManagedServiceSettings.this.updateList();
        }
    };
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ManagedServiceSettings.this.updateList();
        }
    };
    private final Config mConfig = getConfig();

    protected abstract Config getConfig();

    public class ScaryWarningDialogFragment extends DialogFragment {
        public ScaryWarningDialogFragment() {
        }

        public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, String label) {
            Bundle args = new Bundle();
            args.putString("c", cn.flattenToString());
            args.putString("l", label);
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            String label = args.getString("l");
            final ComponentName cn = ComponentName.unflattenFromString(args.getString("c"));
            String title = getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogTitle, label);
            String summary = getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogSummary, label);
            return new AlertDialog.Builder(getActivity()).setMessage(summary).setTitle(title).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    ManagedServiceSettings.this.mEnabledServices.add(cn);
                    ManagedServiceSettings.this.saveEnabledServices();
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPM = getActivity().getPackageManager();
        this.mCR = getActivity().getContentResolver();
        this.mListAdapter = new ServiceListAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(com.android.settings.R.layout.managed_service_settings, container, false);
        TextView empty = (TextView) v.findViewById(R.id.empty);
        empty.setText(this.mConfig.emptyText);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme("package");
        getActivity().registerReceiver(this.mPackageReceiver, filter);
        this.mCR.registerContentObserver(Settings.Secure.getUriFor(this.mConfig.setting), false, this.mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mPackageReceiver);
        this.mCR.unregisterContentObserver(this.mSettingsObserver);
    }

    private void loadEnabledServices() {
        this.mEnabledServices.clear();
        String flat = Settings.Secure.getString(this.mCR, this.mConfig.setting);
        if (flat != null && !"".equals(flat)) {
            String[] names = flat.split(":");
            for (String str : names) {
                ComponentName cn = ComponentName.unflattenFromString(str);
                if (cn != null) {
                    this.mEnabledServices.add(cn);
                }
            }
        }
    }

    private void saveEnabledServices() {
        StringBuilder sb = null;
        for (ComponentName cn : this.mEnabledServices) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(':');
            }
            sb.append(cn.flattenToString());
        }
        Settings.Secure.putString(this.mCR, this.mConfig.setting, sb != null ? sb.toString() : "");
    }

    private void updateList() {
        loadEnabledServices();
        getServices(this.mConfig, this.mListAdapter, this.mPM);
        this.mListAdapter.sort(new PackageItemInfo.DisplayNameComparator(this.mPM));
        getListView().setAdapter((ListAdapter) this.mListAdapter);
    }

    protected static int getEnabledServicesCount(Config config, Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), config.setting);
        if (flat == null || "".equals(flat)) {
            return 0;
        }
        String[] components = flat.split(":");
        return components.length;
    }

    protected static int getServicesCount(Config c, PackageManager pm) {
        return getServices(c, null, pm);
    }

    private static int getServices(Config c, ArrayAdapter<ServiceInfo> adapter, PackageManager pm) {
        int services = 0;
        if (adapter != null) {
            adapter.clear();
        }
        int user = ActivityManager.getCurrentUser();
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(new Intent(c.intentAction), 132, user);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo info = resolveInfo.serviceInfo;
            if (!c.permission.equals(info.permission)) {
                Slog.w(c.tag, "Skipping " + c.noun + " service " + info.packageName + "/" + info.name + ": it does not require the permission " + c.permission);
            } else {
                if (adapter != null) {
                    adapter.add(info);
                }
                services++;
            }
        }
        return services;
    }

    private boolean isServiceEnabled(ServiceInfo info) {
        ComponentName cn = new ComponentName(info.packageName, info.name);
        return this.mEnabledServices.contains(cn);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ServiceInfo info = this.mListAdapter.getItem(position);
        ComponentName cn = new ComponentName(info.packageName, info.name);
        if (this.mEnabledServices.contains(cn)) {
            this.mEnabledServices.remove(cn);
            saveEnabledServices();
        } else {
            new ScaryWarningDialogFragment().setServiceInfo(cn, info.loadLabel(this.mPM).toString()).show(getFragmentManager(), "dialog");
        }
    }

    private static class ViewHolder {
        CheckBox checkbox;
        TextView description;
        ImageView icon;
        TextView name;

        private ViewHolder() {
        }
    }

    private class ServiceListAdapter extends ArrayAdapter<ServiceInfo> {
        final LayoutInflater mInflater;

        ServiceListAdapter(Context context) {
            super(context, 0, 0);
            this.mInflater = (LayoutInflater) ManagedServiceSettings.this.getActivity().getSystemService("layout_inflater");
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
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent) {
            View v = this.mInflater.inflate(com.android.settings.R.layout.managed_service_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(com.android.settings.R.id.icon);
            h.name = (TextView) v.findViewById(com.android.settings.R.id.name);
            h.checkbox = (CheckBox) v.findViewById(com.android.settings.R.id.checkbox);
            h.description = (TextView) v.findViewById(com.android.settings.R.id.description);
            v.setTag(h);
            return v;
        }

        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            ServiceInfo info = getItem(position);
            vh.icon.setImageDrawable(info.loadIcon(ManagedServiceSettings.this.mPM));
            vh.name.setText(info.loadLabel(ManagedServiceSettings.this.mPM));
            vh.description.setVisibility(8);
            vh.checkbox.setChecked(ManagedServiceSettings.this.isServiceEnabled(info));
        }
    }

    protected static class Config {
        int emptyText;
        String intentAction;
        String noun;
        String permission;
        String setting;
        String tag;
        int warningDialogSummary;
        int warningDialogTitle;

        protected Config() {
        }
    }
}
