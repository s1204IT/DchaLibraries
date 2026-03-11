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

public class DeviceAdminSettings extends ListFragment implements Instrumentable {
    private DevicePolicyManager mDPM;
    private String mDeviceOwnerPkg;
    private UserManager mUm;
    private VisibilityLoggerMixin mVisibilityLoggerMixin;
    private final ArrayList<DeviceAdminListItem> mAdmins = new ArrayList<>();
    private SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<>();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction())) {
                DeviceAdminSettings.this.updateList();
            }
        }
    };

    private static class DeviceAdminListItem implements Comparable<DeviceAdminListItem> {
        public boolean active;
        public DeviceAdminInfo info;
        public String name;

        private DeviceAdminListItem() {
        }

        @Override
        public int compareTo(DeviceAdminListItem deviceAdminListItem) {
            if (this.active != deviceAdminListItem.active) {
                return this.active ? -1 : 1;
            }
            return this.name.compareTo(deviceAdminListItem.name);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 516;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(), FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mDPM = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getActivity().getSystemService("user");
        return layoutInflater.inflate(R.layout.device_admin_settings, viewGroup, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setHasOptionsMenu(true);
        Utils.forceCustomPadding(getListView(), true);
        getActivity().setTitle(R.string.manage_device_admin);
    }

    @Override
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

    @Override
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

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        DeviceAdminInfo deviceAdminInfo = (DeviceAdminInfo) listView.getAdapter().getItem(i);
        UserHandle userHandle = new UserHandle(getUserId(deviceAdminInfo));
        Activity activity = getActivity();
        Intent intent = new Intent(activity, (Class<?>) DeviceAdminAdd.class);
        intent.putExtra("android.app.extra.DEVICE_ADMIN", deviceAdminInfo.getComponent());
        activity.startActivityAsUser(intent, userHandle);
    }

    static class ViewHolder {
        Switch checkbox;
        TextView description;
        ImageView icon;
        TextView name;

        ViewHolder() {
        }
    }

    class PolicyListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;

        PolicyListAdapter() {
            this.mInflater = (LayoutInflater) DeviceAdminSettings.this.getActivity().getSystemService("layout_inflater");
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public int getCount() {
            return DeviceAdminSettings.this.mAdmins.size();
        }

        @Override
        public Object getItem(int i) {
            return ((DeviceAdminListItem) DeviceAdminSettings.this.mAdmins.get(i)).info;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public boolean isEnabled(int i) {
            return isEnabled(getItem(i));
        }

        private boolean isEnabled(Object obj) {
            if (DeviceAdminSettings.this.isRemovingAdmin((DeviceAdminInfo) obj)) {
                return false;
            }
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Object item = getItem(i);
            if (view == null) {
                view = newDeviceAdminView(viewGroup);
            }
            bindView(view, (DeviceAdminInfo) item);
            return view;
        }

        private View newDeviceAdminView(ViewGroup viewGroup) {
            View viewInflate = this.mInflater.inflate(R.layout.device_admin_item, viewGroup, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) viewInflate.findViewById(R.id.icon);
            viewHolder.name = (TextView) viewInflate.findViewById(R.id.name);
            viewHolder.checkbox = (Switch) viewInflate.findViewById(R.id.checkbox);
            viewHolder.description = (TextView) viewInflate.findViewById(R.id.description);
            viewInflate.setTag(viewHolder);
            return viewInflate;
        }

        private void bindView(View view, DeviceAdminInfo deviceAdminInfo) {
            Activity activity = DeviceAdminSettings.this.getActivity();
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.icon.setImageDrawable(activity.getPackageManager().getUserBadgedIcon(deviceAdminInfo.loadIcon(activity.getPackageManager()), new UserHandle(DeviceAdminSettings.this.getUserId(deviceAdminInfo))));
            viewHolder.name.setText(deviceAdminInfo.loadLabel(activity.getPackageManager()));
            viewHolder.checkbox.setChecked(DeviceAdminSettings.this.isActiveAdmin(deviceAdminInfo));
            boolean zIsEnabled = isEnabled(deviceAdminInfo);
            try {
                viewHolder.description.setText(deviceAdminInfo.loadDescription(activity.getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
            viewHolder.checkbox.setEnabled(zIsEnabled);
            viewHolder.name.setEnabled(zIsEnabled);
            viewHolder.description.setEnabled(zIsEnabled);
            viewHolder.icon.setEnabled(zIsEnabled);
        }
    }

    public boolean isActiveAdmin(DeviceAdminInfo deviceAdminInfo) {
        return this.mDPM.isAdminActiveAsUser(deviceAdminInfo.getComponent(), getUserId(deviceAdminInfo));
    }

    public boolean isRemovingAdmin(DeviceAdminInfo deviceAdminInfo) {
        return this.mDPM.isRemovingAdmin(deviceAdminInfo.getComponent(), getUserId(deviceAdminInfo));
    }

    private void updateAvailableAdminsForProfile(int i) {
        List activeAdminsAsUser = this.mDPM.getActiveAdminsAsUser(i);
        addActiveAdminsForProfile(activeAdminsAsUser, i);
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsAsUser, i);
    }

    private void addDeviceAdminBroadcastReceiversForProfile(Collection<ComponentName> collection, int i) {
        DeviceAdminInfo deviceAdminInfoCreateDeviceAdminInfo;
        PackageManager packageManager = getActivity().getPackageManager();
        List listQueryBroadcastReceiversAsUser = packageManager.queryBroadcastReceiversAsUser(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32896, i);
        if (listQueryBroadcastReceiversAsUser == null) {
            listQueryBroadcastReceiversAsUser = Collections.emptyList();
        }
        int size = listQueryBroadcastReceiversAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = (ResolveInfo) listQueryBroadcastReceiversAsUser.get(i2);
            ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            if ((collection == null || !collection.contains(componentName)) && (deviceAdminInfoCreateDeviceAdminInfo = createDeviceAdminInfo(resolveInfo.activityInfo)) != null && deviceAdminInfoCreateDeviceAdminInfo.isVisible() && deviceAdminInfoCreateDeviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
                DeviceAdminListItem deviceAdminListItem = new DeviceAdminListItem();
                deviceAdminListItem.info = deviceAdminInfoCreateDeviceAdminInfo;
                deviceAdminListItem.name = deviceAdminInfoCreateDeviceAdminInfo.loadLabel(packageManager).toString();
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
                    DeviceAdminInfo deviceAdminInfoCreateDeviceAdminInfo = createDeviceAdminInfo(packageManager2.getReceiverInfo(componentName, 819328, i));
                    if (deviceAdminInfoCreateDeviceAdminInfo != null) {
                        DeviceAdminListItem deviceAdminListItem = new DeviceAdminListItem();
                        deviceAdminListItem.info = deviceAdminInfoCreateDeviceAdminInfo;
                        deviceAdminListItem.name = deviceAdminInfoCreateDeviceAdminInfo.loadLabel(packageManager).toString();
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

    public int getUserId(DeviceAdminInfo deviceAdminInfo) {
        return UserHandle.getUserId(deviceAdminInfo.getActivityInfo().applicationInfo.uid);
    }
}
