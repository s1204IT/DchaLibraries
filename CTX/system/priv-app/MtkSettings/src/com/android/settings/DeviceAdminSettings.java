package com.android.settings;

import android.app.Activity;
import android.app.AppGlobals;
import android.app.ListFragment;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DeviceAdminSettings extends ListFragment implements Instrumentable {
    private DevicePolicyManager mDPM;
    private String mDeviceOwnerPkg;
    private UserManager mUm;
    private VisibilityLoggerMixin mVisibilityLoggerMixin;
    private final ArrayList<DeviceAdminListItem> mAdmins = new ArrayList<>();
    private SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<>();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.settings.DeviceAdminSettings.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction())) {
                DeviceAdminSettings.this.updateList();
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceAdminListItem implements Comparable<DeviceAdminListItem> {
        public boolean active;
        public DeviceAdminInfo info;
        public String name;

        private DeviceAdminListItem() {
        }

        @Override // java.lang.Comparable
        public int compareTo(DeviceAdminListItem deviceAdminListItem) {
            if (this.active != deviceAdminListItem.active) {
                return this.active ? -1 : 1;
            }
            return this.name.compareTo(deviceAdminListItem.name);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 516;
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(), FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider());
    }

    @Override // android.app.ListFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mDPM = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getActivity().getSystemService("user");
        return layoutInflater.inflate(R.layout.device_admin_settings, viewGroup, false);
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setHasOptionsMenu(true);
        Utils.forceCustomPadding(getListView(), true);
        getActivity().setTitle(R.string.manage_device_admin);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        this.mVisibilityLoggerMixin.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        activity.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        ComponentName deviceOwnerComponentOnAnyUser = this.mDPM.getDeviceOwnerComponentOnAnyUser();
        this.mDeviceOwnerPkg = deviceOwnerComponentOnAnyUser != null ? deviceOwnerComponentOnAnyUser.getPackageName() : null;
        this.mProfileOwnerComponents.clear();
        List<UserHandle> userProfiles = this.mUm.getUserProfiles();
        int size = userProfiles.size();
        for (int i = 0; i < size; i++) {
            int identifier = userProfiles.get(i).getIdentifier();
            this.mProfileOwnerComponents.put(identifier, this.mDPM.getProfileOwnerAsUser(identifier));
        }
        updateList();
    }

    @Override // android.app.Fragment
    public void onPause() {
        getActivity().unregisterReceiver(this.mBroadcastReceiver);
        this.mVisibilityLoggerMixin.onPause();
        super.onPause();
    }

    void updateList() {
        this.mAdmins.clear();
        List<UserHandle> userProfiles = this.mUm.getUserProfiles();
        int size = userProfiles.size();
        for (int i = 0; i < size; i++) {
            updateAvailableAdminsForProfile(userProfiles.get(i).getIdentifier());
        }
        Collections.sort(this.mAdmins);
        getListView().setAdapter((ListAdapter) new PolicyListAdapter());
    }

    @Override // android.app.ListFragment
    public void onListItemClick(ListView listView, View view, int i, long j) {
        DeviceAdminInfo deviceAdminInfo = (DeviceAdminInfo) listView.getAdapter().getItem(i);
        UserHandle userHandle = new UserHandle(getUserId(deviceAdminInfo));
        Activity activity = getActivity();
        Intent intent = new Intent(activity, DeviceAdminAdd.class);
        intent.putExtra("android.app.extra.DEVICE_ADMIN", deviceAdminInfo.getComponent());
        activity.startActivityAsUser(intent, userHandle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class ViewHolder {
        Switch checkbox;
        TextView description;
        ImageView icon;
        TextView name;

        ViewHolder() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PolicyListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;

        PolicyListAdapter() {
            this.mInflater = (LayoutInflater) DeviceAdminSettings.this.getActivity().getSystemService("layout_inflater");
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public boolean hasStableIds() {
            return false;
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return DeviceAdminSettings.this.mAdmins.size();
        }

        @Override // android.widget.Adapter
        public Object getItem(int i) {
            return ((DeviceAdminListItem) DeviceAdminSettings.this.mAdmins.get(i)).info;
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }

        @Override // android.widget.BaseAdapter, android.widget.ListAdapter
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getViewTypeCount() {
            return 1;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public int getItemViewType(int i) {
            return 0;
        }

        @Override // android.widget.BaseAdapter, android.widget.ListAdapter
        public boolean isEnabled(int i) {
            return isEnabled(getItem(i));
        }

        private boolean isEnabled(Object obj) {
            if (DeviceAdminSettings.this.isRemovingAdmin((DeviceAdminInfo) obj)) {
                return false;
            }
            return true;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            Object item = getItem(i);
            if (view == null) {
                view = newDeviceAdminView(viewGroup);
            }
            bindView(view, (DeviceAdminInfo) item);
            return view;
        }

        private View newDeviceAdminView(ViewGroup viewGroup) {
            View inflate = this.mInflater.inflate(R.layout.device_admin_item, viewGroup, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) inflate.findViewById(R.id.icon);
            viewHolder.name = (TextView) inflate.findViewById(R.id.name);
            viewHolder.checkbox = (Switch) inflate.findViewById(R.id.checkbox);
            viewHolder.description = (TextView) inflate.findViewById(R.id.description);
            inflate.setTag(viewHolder);
            return inflate;
        }

        private void bindView(View view, DeviceAdminInfo deviceAdminInfo) {
            Activity activity = DeviceAdminSettings.this.getActivity();
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.icon.setImageDrawable(activity.getPackageManager().getUserBadgedIcon(deviceAdminInfo.loadIcon(activity.getPackageManager()), new UserHandle(DeviceAdminSettings.this.getUserId(deviceAdminInfo))));
            viewHolder.name.setText(deviceAdminInfo.loadLabel(activity.getPackageManager()));
            viewHolder.checkbox.setChecked(DeviceAdminSettings.this.isActiveAdmin(deviceAdminInfo));
            boolean isEnabled = isEnabled(deviceAdminInfo);
            try {
                viewHolder.description.setText(deviceAdminInfo.loadDescription(activity.getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
            viewHolder.checkbox.setEnabled(isEnabled);
            viewHolder.name.setEnabled(isEnabled);
            viewHolder.description.setEnabled(isEnabled);
            viewHolder.icon.setEnabled(isEnabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isActiveAdmin(DeviceAdminInfo deviceAdminInfo) {
        return this.mDPM.isAdminActiveAsUser(deviceAdminInfo.getComponent(), getUserId(deviceAdminInfo));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRemovingAdmin(DeviceAdminInfo deviceAdminInfo) {
        return this.mDPM.isRemovingAdmin(deviceAdminInfo.getComponent(), getUserId(deviceAdminInfo));
    }

    private void updateAvailableAdminsForProfile(int i) {
        List<ComponentName> activeAdminsAsUser = this.mDPM.getActiveAdminsAsUser(i);
        addActiveAdminsForProfile(activeAdminsAsUser, i);
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsAsUser, i);
    }

    private void addDeviceAdminBroadcastReceiversForProfile(Collection<ComponentName> collection, int i) {
        DeviceAdminInfo createDeviceAdminInfo;
        PackageManager packageManager = getActivity().getPackageManager();
        List queryBroadcastReceiversAsUser = packageManager.queryBroadcastReceiversAsUser(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32896, i);
        if (queryBroadcastReceiversAsUser == null) {
            queryBroadcastReceiversAsUser = Collections.emptyList();
        }
        int size = queryBroadcastReceiversAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = (ResolveInfo) queryBroadcastReceiversAsUser.get(i2);
            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            if ((collection == null || !collection.contains(componentName)) && (createDeviceAdminInfo = createDeviceAdminInfo(resolveInfo.activityInfo)) != null && createDeviceAdminInfo.isVisible() && createDeviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
                DeviceAdminListItem deviceAdminListItem = new DeviceAdminListItem();
                deviceAdminListItem.info = createDeviceAdminInfo;
                deviceAdminListItem.name = createDeviceAdminInfo.loadLabel(packageManager).toString();
                deviceAdminListItem.active = false;
                this.mAdmins.add(deviceAdminListItem);
            }
        }
    }

    private void addActiveAdminsForProfile(List<ComponentName> list, int i) {
        if (list != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            IPackageManager packageManager2 = AppGlobals.getPackageManager();
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                ComponentName componentName = list.get(i2);
                try {
                    DeviceAdminInfo createDeviceAdminInfo = createDeviceAdminInfo(packageManager2.getReceiverInfo(componentName, 819328, i));
                    if (createDeviceAdminInfo != null) {
                        DeviceAdminListItem deviceAdminListItem = new DeviceAdminListItem();
                        deviceAdminListItem.info = createDeviceAdminInfo;
                        deviceAdminListItem.name = createDeviceAdminInfo.loadLabel(packageManager).toString();
                        deviceAdminListItem.active = true;
                        this.mAdmins.add(deviceAdminListItem);
                    }
                } catch (RemoteException e) {
                    Log.w("DeviceAdminSettings", "Unable to load component: " + componentName);
                }
            }
        }
    }

    private DeviceAdminInfo createDeviceAdminInfo(ActivityInfo activityInfo) {
        try {
            return new DeviceAdminInfo(getActivity(), activityInfo);
        } catch (IOException | XmlPullParserException e) {
            Log.w("DeviceAdminSettings", "Skipping " + activityInfo, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getUserId(DeviceAdminInfo deviceAdminInfo) {
        return UserHandle.getUserId(deviceAdminInfo.getActivityInfo().applicationInfo.uid);
    }
}
