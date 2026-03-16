package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
    private final SparseArray<ArrayList<DeviceAdminInfo>> mAdminsByProfile = new SparseArray<>();
    private SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<>();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(intent.getAction())) {
                DeviceAdminSettings.this.updateList();
            }
        }
    };

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
        Utils.forceCustomPadding(getListView(), true);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getActivity().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mDeviceOwnerPkg = this.mDPM.getDeviceOwner();
        if (this.mDeviceOwnerPkg != null && !this.mDPM.isDeviceOwner(this.mDeviceOwnerPkg)) {
            this.mDeviceOwnerPkg = null;
        }
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
        this.mAdminsByProfile.clear();
        List<UserHandle> profiles = this.mUm.getUserProfiles();
        int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; i++) {
            int profileId = profiles.get(i).getIdentifier();
            updateAvailableAdminsForProfile(profileId);
        }
        getListView().setAdapter((ListAdapter) new PolicyListAdapter());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getAdapter().getItem(position);
        if (o instanceof DeviceAdminInfo) {
            DeviceAdminInfo dpi = (DeviceAdminInfo) o;
            Activity activity = getActivity();
            int userId = getUserId(dpi);
            if (userId == UserHandle.myUserId() || !isProfileOwner(dpi)) {
                Intent intent = new Intent();
                intent.setClass(activity, DeviceAdminAdd.class);
                intent.putExtra("android.app.extra.DEVICE_ADMIN", dpi.getComponent());
                activity.startActivityAsUser(intent, new UserHandle(userId));
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getString(R.string.managed_profile_device_admin_info, new Object[]{dpi.loadLabel(activity.getPackageManager())}));
            builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
            builder.create().show();
        }
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
            int adminCount = 0;
            int profileCount = DeviceAdminSettings.this.mAdminsByProfile.size();
            for (int i = 0; i < profileCount; i++) {
                adminCount += ((ArrayList) DeviceAdminSettings.this.mAdminsByProfile.valueAt(i)).size();
            }
            return adminCount + profileCount;
        }

        @Override
        public Object getItem(int position) {
            if (position < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }
            int adminPosition = position;
            int n = DeviceAdminSettings.this.mAdminsByProfile.size();
            int i = 0;
            while (i < n) {
                int listSize = ((ArrayList) DeviceAdminSettings.this.mAdminsByProfile.valueAt(i)).size() + 1;
                if (adminPosition < listSize) {
                    break;
                }
                adminPosition -= listSize;
                i++;
            }
            if (i == n) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if (adminPosition != 0) {
                return ((ArrayList) DeviceAdminSettings.this.mAdminsByProfile.valueAt(i)).get(adminPosition - 1);
            }
            Resources res = DeviceAdminSettings.this.getActivity().getResources();
            if (DeviceAdminSettings.this.mAdminsByProfile.keyAt(i) == UserHandle.myUserId()) {
                return res.getString(R.string.personal_device_admin_title);
            }
            return res.getString(R.string.managed_device_admin_title);
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
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            Object o = getItem(position);
            return o instanceof String ? 1 : 0;
        }

        @Override
        public boolean isEnabled(int position) {
            Object o = getItem(position);
            return isEnabled(o);
        }

        private boolean isEnabled(Object o) {
            if (!(o instanceof DeviceAdminInfo)) {
                return false;
            }
            DeviceAdminInfo info = (DeviceAdminInfo) o;
            return ((DeviceAdminSettings.this.isActiveAdmin(info) && DeviceAdminSettings.this.getUserId(info) == UserHandle.myUserId() && (DeviceAdminSettings.this.isDeviceOwner(info) || DeviceAdminSettings.this.isProfileOwner(info))) || DeviceAdminSettings.this.isRemovingAdmin(info)) ? false : true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object o = getItem(position);
            if (o instanceof DeviceAdminInfo) {
                if (convertView == null) {
                    convertView = newDeviceAdminView(parent);
                }
                bindView(convertView, (DeviceAdminInfo) o);
            } else {
                if (convertView == null) {
                    convertView = newTitleView(parent);
                }
                TextView title = (TextView) convertView.findViewById(android.R.id.title);
                title.setText((String) o);
            }
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

        private View newTitleView(ViewGroup parent) {
            TypedArray a = this.mInflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
            int resId = a.getResourceId(3, 0);
            return this.mInflater.inflate(resId, parent, false);
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

    private boolean isDeviceOwner(DeviceAdminInfo item) {
        return getUserId(item) == UserHandle.myUserId() && item.getPackageName().equals(this.mDeviceOwnerPkg);
    }

    private boolean isProfileOwner(DeviceAdminInfo item) {
        ComponentName profileOwner = this.mProfileOwnerComponents.get(getUserId(item));
        return item.getComponent().equals(profileOwner);
    }

    private boolean isActiveAdmin(DeviceAdminInfo item) {
        return this.mDPM.isAdminActiveAsUser(item.getComponent(), getUserId(item));
    }

    private boolean isRemovingAdmin(DeviceAdminInfo item) {
        return this.mDPM.isRemovingAdmin(item.getComponent(), getUserId(item));
    }

    private void updateAvailableAdminsForProfile(int profileId) {
        List activeAdminsListForProfile = this.mDPM.getActiveAdminsAsUser(profileId);
        addActiveAdminsForProfile(activeAdminsListForProfile, profileId);
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsListForProfile, profileId);
    }

    private void addDeviceAdminBroadcastReceiversForProfile(Collection<ComponentName> alreadyAddedComponents, int profileId) {
        DeviceAdminInfo deviceAdminInfo;
        PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> enabledForProfile = pm.queryBroadcastReceivers(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32896, profileId);
        if (enabledForProfile == null) {
            enabledForProfile = Collections.emptyList();
        }
        int n = enabledForProfile.size();
        ArrayList<DeviceAdminInfo> deviceAdmins = this.mAdminsByProfile.get(profileId);
        if (deviceAdmins == null) {
            deviceAdmins = new ArrayList<>(n);
        }
        for (int i = 0; i < n; i++) {
            ResolveInfo resolveInfo = enabledForProfile.get(i);
            ComponentName riComponentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            if ((alreadyAddedComponents == null || !alreadyAddedComponents.contains(riComponentName)) && (deviceAdminInfo = createDeviceAdminInfo(resolveInfo)) != null && deviceAdminInfo.isVisible()) {
                deviceAdmins.add(deviceAdminInfo);
            }
        }
        if (!deviceAdmins.isEmpty()) {
            this.mAdminsByProfile.put(profileId, deviceAdmins);
        }
    }

    private void addActiveAdminsForProfile(List<ComponentName> activeAdmins, int profileId) {
        if (activeAdmins != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            int n = activeAdmins.size();
            ArrayList<DeviceAdminInfo> deviceAdmins = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                ComponentName activeAdmin = activeAdmins.get(i);
                List<ResolveInfo> resolved = packageManager.queryBroadcastReceivers(new Intent().setComponent(activeAdmin), 32896, profileId);
                if (resolved != null) {
                    int resolvedMax = resolved.size();
                    for (int j = 0; j < resolvedMax; j++) {
                        DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(resolved.get(j));
                        if (deviceAdminInfo != null) {
                            deviceAdmins.add(deviceAdminInfo);
                        }
                    }
                }
            }
            if (!deviceAdmins.isEmpty()) {
                this.mAdminsByProfile.put(profileId, deviceAdmins);
            }
        }
    }

    private DeviceAdminInfo createDeviceAdminInfo(ResolveInfo resolved) {
        try {
            return new DeviceAdminInfo(getActivity(), resolved);
        } catch (IOException e) {
            Log.w("DeviceAdminSettings", "Skipping " + resolved.activityInfo, e);
            return null;
        } catch (XmlPullParserException e2) {
            Log.w("DeviceAdminSettings", "Skipping " + resolved.activityInfo, e2);
            return null;
        }
    }

    private int getUserId(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
