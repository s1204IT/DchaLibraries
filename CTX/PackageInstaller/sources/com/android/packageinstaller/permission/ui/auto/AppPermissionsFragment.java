package com.android.packageinstaller.permission.ui.auto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
import androidx.car.widget.PagedListView;
import com.android.car.list.IconToggleLineItem;
import com.android.car.list.TypedPagedListAdapter;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.ui.auto.AppPermissionsFragment;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;

public final class AppPermissionsFragment extends Fragment {
    private AppPermissions mAppPermissions;
    protected PagedListView mListView;
    private String mPackageName;
    protected TypedPagedListAdapter mPagedListAdapter;

    public static AppPermissionsFragment newInstance(String str) {
        AppPermissionsFragment appPermissionsFragment = new AppPermissionsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("extra_layout", R.layout.car_app_permissions);
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        appPermissionsFragment.setArguments(bundle);
        return appPermissionsFragment;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getView().findViewById(R.id.action_bar_icon_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.getActivity().onBackPressed();
            }
        });
        this.mListView = (PagedListView) getView().findViewById(R.id.list);
        this.mPagedListAdapter = new TypedPagedListAdapter(getLineItems());
        this.mListView.setAdapter(this.mPagedListAdapter);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null && bundle.containsKey("android.intent.extra.PACKAGE_NAME")) {
            this.mPackageName = bundle.getString("android.intent.extra.PACKAGE_NAME");
        } else if (getArguments() != null && getArguments().containsKey("android.intent.extra.PACKAGE_NAME")) {
            this.mPackageName = getArguments().getString("android.intent.extra.PACKAGE_NAME");
        }
        if (this.mPackageName == null) {
            Log.e("ManagePermsFragment", "package name is missing");
            return;
        }
        final Activity activity = getActivity();
        PackageInfo packageInfo = getPackageInfo(activity, this.mPackageName);
        if (packageInfo == null) {
            Toast.makeText(activity, R.string.app_not_found_dlg_title, 1).show();
            activity.finish();
        } else {
            this.mAppPermissions = new AppPermissions(activity, packageInfo, null, true, new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("android.intent.extra.PACKAGE_NAME", this.mPackageName);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(getArguments().getInt("extra_layout"), viewGroup, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAppPermissions.refresh();
    }

    public ArrayList<TypedPagedListAdapter.LineItem> getLineItems() {
        ArrayList<TypedPagedListAdapter.LineItem> arrayList = new ArrayList<>();
        Context context = getContext();
        if (context == null) {
            return arrayList;
        }
        for (AppPermissionGroup appPermissionGroup : this.mAppPermissions.getPermissionGroups()) {
            if (Utils.shouldShowPermission(appPermissionGroup, this.mAppPermissions.getPackageInfo().packageName)) {
                arrayList.add(new PermissionLineItem(appPermissionGroup, context));
            }
        }
        return arrayList;
    }

    private static PackageInfo getPackageInfo(Activity activity, String str) {
        try {
            return activity.getPackageManager().getPackageInfo(str, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            if (Log.isLoggable("ManagePermsFragment", 4)) {
                Log.i("ManagePermsFragment", "No package:" + activity.getCallingPackage(), e);
                return null;
            }
            return null;
        }
    }

    private class PermissionLineItem extends IconToggleLineItem {
        private final Context mContext;
        private final AppPermissionGroup mPermissionGroup;

        PermissionLineItem(AppPermissionGroup appPermissionGroup, Context context) {
            super(appPermissionGroup.getLabel(), context);
            this.mContext = context;
            this.mPermissionGroup = appPermissionGroup;
        }

        @Override
        public boolean onToggleTouched(final Switch r4, MotionEvent motionEvent) {
            if (motionEvent.getAction() != 0) {
                return true;
            }
            if (!isChecked()) {
                this.mPermissionGroup.grantRuntimePermissions(false);
                r4.performClick();
            } else {
                boolean zHasGrantedByDefaultPermission = this.mPermissionGroup.hasGrantedByDefaultPermission();
                if (zHasGrantedByDefaultPermission || !this.mPermissionGroup.doesSupportRuntimePermissions()) {
                    new AlertDialog.Builder(this.mContext).setMessage(zHasGrantedByDefaultPermission ? R.string.system_warning : R.string.old_sdk_deny_warning).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            AppPermissionsFragment.PermissionLineItem.lambda$onToggleTouched$0(this.f$0, r4, dialogInterface, i);
                        }
                    }).show();
                } else {
                    this.mPermissionGroup.revokeRuntimePermissions(false);
                    r4.performClick();
                }
            }
            return true;
        }

        public static void lambda$onToggleTouched$0(PermissionLineItem permissionLineItem, Switch r1, DialogInterface dialogInterface, int i) {
            permissionLineItem.mPermissionGroup.revokeRuntimePermissions(false);
            r1.performClick();
        }

        @Override
        public int getIcon() {
            return this.mPermissionGroup.getIconResId();
        }

        @Override
        public boolean isChecked() {
            return this.mPermissionGroup.areRuntimePermissionsGranted();
        }

        @Override
        public CharSequence getDesc() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean isExpandable() {
            return false;
        }
    }
}
