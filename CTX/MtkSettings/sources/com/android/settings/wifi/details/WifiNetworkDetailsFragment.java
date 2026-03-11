package com.android.settings.wifi.details;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.List;

public class WifiNetworkDetailsFragment extends DashboardFragment {
    private AccessPoint mAccessPoint;
    private WifiDetailPreferenceController mWifiDetailPreferenceController;

    @Override
    public void onAttach(Context context) {
        this.mAccessPoint = new AccessPoint(context, getArguments());
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return 849;
    }

    @Override
    protected String getLogTag() {
        return "WifiNetworkDetailsFrg";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_network_details_fragment;
    }

    @Override
    public int getDialogMetricsCategory(int i) {
        if (i == 1) {
            return 603;
        }
        return 0;
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (getActivity() == null || this.mWifiDetailPreferenceController == null || this.mAccessPoint == null) {
            return null;
        }
        return WifiDialog.createModal(getActivity(), this.mWifiDetailPreferenceController, this.mAccessPoint, 2);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        MenuItem menuItemAdd = menu.add(0, 1, 0, R.string.wifi_modify);
        menuItemAdd.setIcon(R.drawable.ic_mode_edit);
        menuItemAdd.setShowAsAction(2);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            if (!this.mWifiDetailPreferenceController.canModifyNetwork()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), RestrictedLockUtils.getDeviceOwner(getContext()));
            } else {
                showDialog(1);
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mWifiDetailPreferenceController = WifiDetailPreferenceController.newInstance(this.mAccessPoint, (ConnectivityManager) context.getSystemService(ConnectivityManager.class), context, this, new Handler(Looper.getMainLooper()), getLifecycle(), (WifiManager) context.getSystemService(WifiManager.class), this.mMetricsFeatureProvider);
        arrayList.add(this.mWifiDetailPreferenceController);
        arrayList.add(new WifiMeteredPreferenceController(context, this.mAccessPoint.getConfig()));
        return arrayList;
    }
}
