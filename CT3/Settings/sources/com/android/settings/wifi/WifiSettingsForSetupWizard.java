package com.android.settings.wifi;

import android.app.Dialog;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;

public class WifiSettingsForSetupWizard extends WifiSettings {
    private View mAddOtherNetworkItem;
    private TextView mEmptyFooter;
    private SetupWizardListLayout mLayout;
    private boolean mListLastEmpty = false;
    private View mMacAddressFooter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mLayout = (SetupWizardListLayout) inflater.inflate(R.layout.setup_wifi_layout, container, false);
        ListView list = this.mLayout.getListView();
        this.mAddOtherNetworkItem = inflater.inflate(R.layout.setup_wifi_add_network, (ViewGroup) list, false);
        list.addFooterView(this.mAddOtherNetworkItem, null, true);
        this.mAddOtherNetworkItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!WifiSettingsForSetupWizard.this.mWifiManager.isWifiEnabled()) {
                    return;
                }
                WifiSettingsForSetupWizard.this.onAddNetworkPressed();
            }
        });
        this.mMacAddressFooter = inflater.inflate(R.layout.setup_wifi_mac_address, (ViewGroup) list, false);
        list.addFooterView(this.mMacAddressFooter, null, false);
        NavigationBar navigationBar = this.mLayout.getNavigationBar();
        if (navigationBar != null) {
            WifiSetupActivity activity = (WifiSetupActivity) getActivity();
            activity.onNavigationBarCreated(navigationBar);
        }
        return this.mLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (hasNextButton()) {
            getNextButton().setVisibility(8);
        }
        updateMacAddress();
    }

    @Override
    public void onAccessPointsChanged() {
        boolean z = true;
        super.onAccessPointsChanged();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null && preferenceScreen.getPreferenceCount() != 0) {
            z = false;
        }
        updateFooter(z);
    }

    @Override
    public void onWifiStateChanged(int state) {
        super.onWifiStateChanged(state);
        updateMacAddress();
    }

    @Override
    public void registerForContextMenu(View view) {
    }

    @Override
    WifiEnabler createWifiEnabler() {
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Dialog dialog = super.onCreateDialog(dialogId);
        SetupWizardUtils.applyImmersiveFlags(dialog);
        return dialog;
    }

    @Override
    protected void connect(WifiConfiguration config) {
        WifiSetupActivity activity = (WifiSetupActivity) getActivity();
        activity.networkSelected();
        super.connect(config);
    }

    @Override
    protected TextView initEmptyTextView() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        this.mEmptyFooter = (TextView) inflater.inflate(R.layout.setup_wifi_empty, (ViewGroup) getListView(), false);
        return this.mEmptyFooter;
    }

    protected void updateFooter(boolean isEmpty) {
        if (getView() == null) {
            Log.d("WifiSettingsForSetupWizard", "exceptional life cycle that may cause JE");
            return;
        }
        if (isEmpty == this.mListLastEmpty) {
            return;
        }
        if (isEmpty) {
            setFooterView(this.mEmptyFooter);
        } else {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(1);
            layout.addView(this.mAddOtherNetworkItem);
            layout.addView(this.mMacAddressFooter);
            setFooterView(layout);
        }
        this.mListLastEmpty = isEmpty;
    }

    @Override
    public View setPinnedHeaderView(int layoutResId) {
        return null;
    }

    @Override
    public void setPinnedHeaderView(View pinnedHeader) {
    }

    @Override
    protected void setProgressBarVisible(boolean visible) {
        if (this.mLayout == null) {
            return;
        }
        if (visible) {
            this.mLayout.showProgressBar();
        } else {
            this.mLayout.hideProgressBar();
        }
    }

    private void updateMacAddress() {
        android.net.wifi.WifiInfo connectionInfo;
        if (this.mMacAddressFooter == null) {
            return;
        }
        String macAddress = null;
        if (this.mWifiManager != null && (connectionInfo = this.mWifiManager.getConnectionInfo()) != null) {
            macAddress = connectionInfo.getMacAddress();
        }
        TextView macAddressTextView = (TextView) this.mMacAddressFooter.findViewById(R.id.mac_address);
        if (TextUtils.isEmpty(macAddress)) {
            macAddress = getString(R.string.status_unavailable);
        }
        macAddressTextView.setText(macAddress);
    }
}
