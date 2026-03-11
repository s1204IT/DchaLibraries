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
import android.graphics.drawable.Drawable;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DeviceAdminSettings extends ListFragment {
    private DevicePolicyManager mDPM;
    private String mDeviceOwnerPkg;
    private UserManager mUm;
    private final ArrayList<DeviceAdminListItem> mAdmins = new ArrayList<>();
    private SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<>();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            DeviceAdminSettings.this.updateList();
        }
    };

    private static class DeviceAdminListItem implements Comparable<DeviceAdminListItem> {
        public boolean active;
        public DeviceAdminInfo info;
        public String name;

        DeviceAdminListItem(DeviceAdminListItem deviceAdminListItem) {
            this();
        }

        private DeviceAdminListItem() {
        }

        @Override
        public int compareTo(DeviceAdminListItem other) {
            if (this.active != other.active) {
                return this.active ? -1 : 1;
            }
            return this.name.compareTo(other.name);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mDPM = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getActivity().getSystemService("user");
        return inflater.inflate(R.layout.device_admin_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        Utils.forceCustomPadding(getListView(), true);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getActivity().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        ComponentName deviceOwnerComponent = this.mDPM.getDeviceOwnerComponentOnAnyUser();
        this.mDeviceOwnerPkg = deviceOwnerComponent != null ? deviceOwnerComponent.getPackageName() : null;
        this.mProfileOwnerComponents.clear();
        List<UserHandle> profiles = this.mUm.getUserProfiles();
        int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; i++) {
            int profileId = profiles.get(i).getIdentifier();
            this.mProfileOwnerComponents.put(profileId, this.mDPM.getProfileOwnerAsUser(profileId));
        }
        updateList();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(this.mBroadcastReceiver);
        super.onPause();
    }

    void updateList() {
        this.mAdmins.clear();
        List<UserHandle> profiles = this.mUm.getUserProfiles();
        int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; i++) {
            int profileId = profiles.get(i).getIdentifier();
            updateAvailableAdminsForProfile(profileId);
        }
        Collections.sort(this.mAdmins);
        getListView().setAdapter((ListAdapter) new PolicyListAdapter());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getAdapter().getItem(position);
        DeviceAdminInfo dpi = (DeviceAdminInfo) o;
        UserHandle user = new UserHandle(getUserId(dpi));
        Activity activity = getActivity();
        Intent intent = new Intent(activity, (Class<?>) DeviceAdminAdd.class);
        intent.putExtra("android.app.extra.DEVICE_ADMIN", dpi.getComponent());
        activity.startActivityAsUser(intent, user);
    }

    static class ViewHolder {
        CheckBox checkbox;
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
        public Object getItem(int position) {
            return ((DeviceAdminListItem) DeviceAdminSettings.this.mAdmins.get(position)).info;
        }

        @Override
        public long getItemId(int position) {
            return position;
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
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public boolean isEnabled(int position) {
            Object o = getItem(position);
            return isEnabled(o);
        }

        private boolean isEnabled(Object o) {
            DeviceAdminInfo info = (DeviceAdminInfo) o;
            if (DeviceAdminSettings.this.isRemovingAdmin(info)) {
                return false;
            }
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object o = getItem(position);
            if (convertView == null) {
                convertView = newDeviceAdminView(parent);
            }
            bindView(convertView, (DeviceAdminInfo) o);
            return convertView;
        }

        private View newDeviceAdminView(ViewGroup parent) {
            View v = this.mInflater.inflate(R.layout.device_admin_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(R.id.icon);
            h.name = (TextView) v.findViewById(R.id.name);
            h.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            h.description = (TextView) v.findViewById(R.id.description);
            v.setTag(h);
            return v;
        }

        private void bindView(View view, DeviceAdminInfo item) {
            Activity activity = DeviceAdminSettings.this.getActivity();
            ViewHolder vh = (ViewHolder) view.getTag();
            Drawable activityIcon = item.loadIcon(activity.getPackageManager());
            Drawable badgedIcon = activity.getPackageManager().getUserBadgedIcon(activityIcon, new UserHandle(DeviceAdminSettings.this.getUserId(item)));
            vh.icon.setImageDrawable(badgedIcon);
            vh.name.setText(item.loadLabel(activity.getPackageManager()));
            vh.checkbox.setChecked(DeviceAdminSettings.this.isActiveAdmin(item));
            boolean enabled = isEnabled(item);
            try {
                vh.description.setText(item.loadDescription(activity.getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
            vh.checkbox.setEnabled(enabled);
            vh.name.setEnabled(enabled);
            vh.description.setEnabled(enabled);
            vh.icon.setEnabled(enabled);
        }
    }

    public boolean isActiveAdmin(DeviceAdminInfo item) {
        return this.mDPM.isAdminActiveAsUser(item.getComponent(), getUserId(item));
    }

    public boolean isRemovingAdmin(DeviceAdminInfo item) {
        return this.mDPM.isRemovingAdmin(item.getComponent(), getUserId(item));
    }

    private void updateAvailableAdminsForProfile(int profileId) {
        List activeAdminsListForProfile = this.mDPM.getActiveAdminsAsUser(profileId);
        addActiveAdminsForProfile(activeAdminsListForProfile, profileId);
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsListForProfile, profileId);
    }

    private void addDeviceAdminBroadcastReceiversForProfile(Collection<ComponentName> alreadyAddedComponents, int profileId) {
        DeviceAdminInfo deviceAdminInfo;
        DeviceAdminListItem deviceAdminListItem = null;
        PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> enabledForProfile = pm.queryBroadcastReceiversAsUser(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32896, profileId);
        if (enabledForProfile == null) {
            enabledForProfile = Collections.emptyList();
        }
        int n = enabledForProfile.size();
        for (int i = 0; i < n; i++) {
            ResolveInfo resolveInfo = enabledForProfile.get(i);
            ComponentName riComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            if ((alreadyAddedComponents == null || !alreadyAddedComponents.contains(riComponentName)) && (deviceAdminInfo = createDeviceAdminInfo(resolveInfo.activityInfo)) != null && deviceAdminInfo.isVisible() && deviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
                DeviceAdminListItem item = new DeviceAdminListItem(deviceAdminListItem);
                item.info = deviceAdminInfo;
                item.name = deviceAdminInfo.loadLabel(pm).toString();
                item.active = false;
                this.mAdmins.add(item);
            }
        }
    }

    private void addActiveAdminsForProfile(List<ComponentName> activeAdmins, int profileId) {
        DeviceAdminListItem deviceAdminListItem = null;
        if (activeAdmins == null) {
            return;
        }
        PackageManager packageManager = getActivity().getPackageManager();
        IPackageManager iPackageManager = AppGlobals.getPackageManager();
        int n = activeAdmins.size();
        for (int i = 0; i < n; i++) {
            ComponentName activeAdmin = activeAdmins.get(i);
            try {
                ActivityInfo ai = iPackageManager.getReceiverInfo(activeAdmin, 819328, profileId);
                DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(ai);
                if (deviceAdminInfo != null) {
                    DeviceAdminListItem item = new DeviceAdminListItem(deviceAdminListItem);
                    item.info = deviceAdminInfo;
                    item.name = deviceAdminInfo.loadLabel(packageManager).toString();
                    item.active = true;
                    this.mAdmins.add(item);
                }
            } catch (RemoteException e) {
                Log.w("DeviceAdminSettings", "Unable to load component: " + activeAdmin);
            }
        }
    }

    private DeviceAdminInfo createDeviceAdminInfo(ActivityInfo ai) {
        try {
            return new DeviceAdminInfo(getActivity(), ai);
        } catch (IOException | XmlPullParserException e) {
            Log.w("DeviceAdminSettings", "Skipping " + ai, e);
            return null;
        }
    }

    public int getUserId(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
