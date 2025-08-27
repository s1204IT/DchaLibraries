package com.android.settings.wifi.p2p;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class WifiP2pSettings extends DashboardFragment implements WifiP2pManager.PeerListListener, WifiP2pManager.PersistentGroupInfoListener {
    private DialogInterface.OnClickListener mCancelConnectListener;
    private WifiP2pManager.Channel mChannel;
    private int mConnectedDevices;
    private DialogInterface.OnClickListener mDeleteGroupListener;
    private EditText mDeviceNameText;
    private DialogInterface.OnClickListener mDisconnectListener;
    private P2pPeerCategoryPreferenceController mPeerCategoryController;
    private P2pPersistentCategoryPreferenceController mPersistentCategoryController;
    private DialogInterface.OnClickListener mRenameListener;
    private String mSavedDeviceName;
    private WifiP2pPersistentGroup mSelectedGroup;
    private String mSelectedGroupName;
    private WifiP2pPeer mSelectedWifiPeer;
    private WifiP2pDevice mThisDevice;
    private P2pThisDevicePreferenceController mThisDevicePreferenceController;
    private boolean mWifiP2pEnabled;
    private WifiP2pManager mWifiP2pManager;
    private boolean mWifiP2pSearching;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private boolean mLastGroupFormed = false;
    private WifiP2pDeviceList mPeers = new WifiP2pDeviceList();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("WifiP2pSettings", "receive action: " + action);
            if ("android.net.wifi.p2p.STATE_CHANGED".equals(action)) {
                WifiP2pSettings.this.mWifiP2pEnabled = intent.getIntExtra("wifi_p2p_state", 1) == 2;
                WifiP2pSettings.this.handleP2pStateChanged();
                return;
            }
            if ("android.net.wifi.p2p.PEERS_CHANGED".equals(action)) {
                WifiP2pSettings.this.mPeers = (WifiP2pDeviceList) intent.getParcelableExtra("wifiP2pDeviceList");
                WifiP2pSettings.this.handlePeersChanged();
                return;
            }
            if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(action)) {
                if (WifiP2pSettings.this.mWifiP2pManager == null) {
                    return;
                }
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) intent.getParcelableExtra("wifiP2pInfo");
                if (!networkInfo.isConnected()) {
                    if (!WifiP2pSettings.this.mLastGroupFormed) {
                        WifiP2pSettings.this.startSearch();
                    }
                } else {
                    Log.d("WifiP2pSettings", "Connected");
                }
                WifiP2pSettings.this.mLastGroupFormed = wifiP2pInfo.groupFormed;
                return;
            }
            if ("android.net.wifi.p2p.THIS_DEVICE_CHANGED".equals(action)) {
                WifiP2pSettings.this.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                Log.d("WifiP2pSettings", "Update device info: " + WifiP2pSettings.this.mThisDevice);
                WifiP2pSettings.this.mThisDevicePreferenceController.updateDeviceName(WifiP2pSettings.this.mThisDevice);
                return;
            }
            if (!"android.net.wifi.p2p.DISCOVERY_STATE_CHANGE".equals(action)) {
                if ("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED".equals(action) && WifiP2pSettings.this.mWifiP2pManager != null) {
                    WifiP2pSettings.this.mWifiP2pManager.requestPersistentGroupInfo(WifiP2pSettings.this.mChannel, WifiP2pSettings.this);
                    return;
                }
                return;
            }
            int intExtra = intent.getIntExtra("discoveryState", 1);
            Log.d("WifiP2pSettings", "Discovery state changed: " + intExtra);
            if (intExtra == 2) {
                WifiP2pSettings.this.updateSearchMenu(true);
            } else {
                WifiP2pSettings.this.updateSearchMenu(false);
            }
        }
    };

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "WifiP2pSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_p2p_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 109;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_wifi_p2p;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mPersistentCategoryController = new P2pPersistentCategoryPreferenceController(context);
        this.mPeerCategoryController = new P2pPeerCategoryPreferenceController(context);
        this.mThisDevicePreferenceController = new P2pThisDevicePreferenceController(context);
        arrayList.add(this.mPersistentCategoryController);
        arrayList.add(this.mPeerCategoryController);
        arrayList.add(this.mThisDevicePreferenceController);
        return arrayList;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        Activity activity = getActivity();
        this.mWifiP2pManager = (WifiP2pManager) getSystemService("wifip2p");
        if (this.mWifiP2pManager != null) {
            this.mChannel = this.mWifiP2pManager.initialize(activity.getApplicationContext(), getActivity().getMainLooper(), null);
            if (this.mChannel == null) {
                Log.e("WifiP2pSettings", "Failed to set up connection with wifi p2p service");
                this.mWifiP2pManager = null;
            }
        } else {
            Log.e("WifiP2pSettings", "mWifiP2pManager is null !");
        }
        if (bundle != null && bundle.containsKey("PEER_STATE")) {
            this.mSelectedWifiPeer = new WifiP2pPeer(getPrefContext(), (WifiP2pDevice) bundle.getParcelable("PEER_STATE"));
        }
        if (bundle != null && bundle.containsKey("DEV_NAME")) {
            this.mSavedDeviceName = bundle.getString("DEV_NAME");
        }
        if (bundle != null && bundle.containsKey("GROUP_NAME")) {
            this.mSelectedGroupName = bundle.getString("GROUP_NAME");
        }
        this.mRenameListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1 && WifiP2pSettings.this.mWifiP2pManager != null) {
                    String string = WifiP2pSettings.this.mDeviceNameText.getText().toString();
                    if (string != null) {
                        for (int i2 = 0; i2 < string.length(); i2++) {
                            char cCharAt = string.charAt(i2);
                            if (!Character.isDigit(cCharAt) && !Character.isLetter(cCharAt) && cCharAt != '-' && cCharAt != '_' && cCharAt != ' ') {
                                Toast.makeText(WifiP2pSettings.this.getActivity(), R.string.wifi_p2p_failed_rename_message, 1).show();
                                return;
                            }
                        }
                    }
                    WifiP2pSettings.this.mWifiP2pManager.setDeviceName(WifiP2pSettings.this.mChannel, WifiP2pSettings.this.mDeviceNameText.getText().toString(), new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.2.1
                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onSuccess() {
                            Log.d("WifiP2pSettings", " device rename success");
                        }

                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onFailure(int i3) {
                            Toast.makeText(WifiP2pSettings.this.getActivity(), R.string.wifi_p2p_failed_rename_message, 1).show();
                        }
                    });
                }
            }
        };
        this.mDisconnectListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1 && WifiP2pSettings.this.mWifiP2pManager != null) {
                    WifiP2pSettings.this.mWifiP2pManager.removeGroup(WifiP2pSettings.this.mChannel, new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.3.1
                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onSuccess() {
                            Log.d("WifiP2pSettings", " remove group success");
                        }

                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onFailure(int i2) {
                            Log.d("WifiP2pSettings", " remove group fail " + i2);
                        }
                    });
                }
            }
        };
        this.mCancelConnectListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1 && WifiP2pSettings.this.mWifiP2pManager != null) {
                    WifiP2pSettings.this.mWifiP2pManager.cancelConnect(WifiP2pSettings.this.mChannel, new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.4.1
                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onSuccess() {
                            Log.d("WifiP2pSettings", " cancel connect success");
                        }

                        @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                        public void onFailure(int i2) {
                            Log.d("WifiP2pSettings", " cancel connect fail " + i2);
                        }
                    });
                }
            }
        };
        this.mDeleteGroupListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.5
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1) {
                    if (WifiP2pSettings.this.mWifiP2pManager != null) {
                        if (WifiP2pSettings.this.mSelectedGroup != null) {
                            Log.d("WifiP2pSettings", " deleting group " + WifiP2pSettings.this.mSelectedGroup.getGroupName());
                            WifiP2pSettings.this.mWifiP2pManager.deletePersistentGroup(WifiP2pSettings.this.mChannel, WifiP2pSettings.this.mSelectedGroup.getNetworkId(), new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.5.1
                                @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                                public void onSuccess() {
                                    Log.d("WifiP2pSettings", " delete group success");
                                }

                                @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                                public void onFailure(int i2) {
                                    Log.d("WifiP2pSettings", " delete group fail " + i2);
                                }
                            });
                            WifiP2pSettings.this.mSelectedGroup = null;
                            return;
                        }
                        Log.w("WifiP2pSettings", " No selected group to delete!");
                        return;
                    }
                    return;
                }
                if (i == -2) {
                    Log.d("WifiP2pSettings", " forgetting selected group " + WifiP2pSettings.this.mSelectedGroup.getGroupName());
                    WifiP2pSettings.this.mSelectedGroup = null;
                }
            }
        };
        super.onActivityCreated(bundle);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mIntentFilter.addAction("android.net.wifi.p2p.STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
        getPreferenceScreen();
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        if (this.mWifiP2pManager != null) {
            this.mWifiP2pManager.requestPeers(this.mChannel, this);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mWifiP2pManager != null) {
            this.mWifiP2pManager.stopPeerDiscovery(this.mChannel, null);
        }
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 1, 0, this.mWifiP2pSearching ? R.string.wifi_p2p_menu_searching : R.string.wifi_p2p_menu_search).setEnabled(this.mWifiP2pEnabled).setShowAsAction(1);
        menu.add(0, 2, 0, R.string.wifi_p2p_menu_rename).setEnabled(this.mWifiP2pEnabled).setShowAsAction(1);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(1);
        MenuItem menuItemFindItem2 = menu.findItem(2);
        menuItemFindItem.setEnabled(this.mWifiP2pEnabled && !this.mWifiP2pSearching);
        menuItemFindItem2.setEnabled(this.mWifiP2pEnabled);
        if (this.mWifiP2pSearching) {
            menuItemFindItem.setTitle(R.string.wifi_p2p_menu_searching);
        } else {
            menuItemFindItem.setTitle(R.string.wifi_p2p_menu_search);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                startSearch();
                return true;
            case 2:
                showDialog(3);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof WifiP2pPeer) {
            this.mSelectedWifiPeer = (WifiP2pPeer) preference;
            if (this.mSelectedWifiPeer.device.status == 0) {
                showDialog(1);
            } else if (this.mSelectedWifiPeer.device.status == 1) {
                showDialog(2);
            } else {
                WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                wifiP2pConfig.deviceAddress = this.mSelectedWifiPeer.device.deviceAddress;
                int i = SystemProperties.getInt("wifidirect.wps", -1);
                if (i != -1) {
                    wifiP2pConfig.wps.setup = i;
                } else if (this.mSelectedWifiPeer.device.wpsPbcSupported()) {
                    wifiP2pConfig.wps.setup = 0;
                } else if (this.mSelectedWifiPeer.device.wpsKeypadSupported()) {
                    wifiP2pConfig.wps.setup = 2;
                } else {
                    wifiP2pConfig.wps.setup = 1;
                }
                this.mWifiP2pManager.connect(this.mChannel, wifiP2pConfig, new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.6
                    @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                    public void onSuccess() {
                        Log.d("WifiP2pSettings", " connect success");
                    }

                    @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                    public void onFailure(int i2) {
                        Log.e("WifiP2pSettings", " connect fail " + i2);
                        Toast.makeText(WifiP2pSettings.this.getActivity(), R.string.wifi_p2p_failed_connect_message, 0).show();
                    }
                });
            }
        } else if (preference instanceof WifiP2pPersistentGroup) {
            this.mSelectedGroup = (WifiP2pPersistentGroup) preference;
            showDialog(4);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        String str;
        String str2;
        String string;
        if (i == 1) {
            if (TextUtils.isEmpty(this.mSelectedWifiPeer.device.deviceName)) {
                str2 = this.mSelectedWifiPeer.device.deviceAddress;
            } else {
                str2 = this.mSelectedWifiPeer.device.deviceName;
            }
            if (this.mConnectedDevices > 1) {
                string = getActivity().getString(R.string.wifi_p2p_disconnect_multiple_message, new Object[]{str2, Integer.valueOf(this.mConnectedDevices - 1)});
            } else {
                string = getActivity().getString(R.string.wifi_p2p_disconnect_message, new Object[]{str2});
            }
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.wifi_p2p_disconnect_title).setMessage(string).setPositiveButton(getActivity().getString(R.string.dlg_ok), this.mDisconnectListener).setNegativeButton(getActivity().getString(R.string.dlg_cancel), (DialogInterface.OnClickListener) null).create();
        }
        if (i == 2) {
            if (TextUtils.isEmpty(this.mSelectedWifiPeer.device.deviceName)) {
                str = this.mSelectedWifiPeer.device.deviceAddress;
            } else {
                str = this.mSelectedWifiPeer.device.deviceName;
            }
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.wifi_p2p_cancel_connect_title).setMessage(getActivity().getString(R.string.wifi_p2p_cancel_connect_message, new Object[]{str})).setPositiveButton(getActivity().getString(R.string.dlg_ok), this.mCancelConnectListener).setNegativeButton(getActivity().getString(R.string.dlg_cancel), (DialogInterface.OnClickListener) null).create();
        }
        if (i == 3) {
            this.mDeviceNameText = new EditText(getActivity());
            this.mDeviceNameText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
            this.mDeviceNameText.setTextDirection(5);
            if (this.mSavedDeviceName != null) {
                this.mDeviceNameText.setText(this.mSavedDeviceName);
                this.mDeviceNameText.setSelection(this.mSavedDeviceName.length());
            } else if (this.mThisDevice != null && !TextUtils.isEmpty(this.mThisDevice.deviceName)) {
                this.mDeviceNameText.setText(this.mThisDevice.deviceName);
                this.mDeviceNameText.setSelection(0, this.mThisDevice.deviceName.length());
            }
            this.mSavedDeviceName = null;
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.wifi_p2p_menu_rename).setView(this.mDeviceNameText).setPositiveButton(getActivity().getString(R.string.dlg_ok), this.mRenameListener).setNegativeButton(getActivity().getString(R.string.dlg_cancel), (DialogInterface.OnClickListener) null).create();
        }
        if (i == 4) {
            return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getString(R.string.wifi_p2p_delete_group_message)).setPositiveButton(getActivity().getString(R.string.dlg_ok), this.mDeleteGroupListener).setNegativeButton(getActivity().getString(R.string.dlg_cancel), this.mDeleteGroupListener).create();
        }
        return null;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        switch (i) {
            case 1:
                return 575;
            case 2:
                return 576;
            case 3:
                return 577;
            case 4:
                return 578;
            default:
                return 0;
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        if (this.mSelectedWifiPeer != null) {
            bundle.putParcelable("PEER_STATE", this.mSelectedWifiPeer.device);
        }
        if (this.mDeviceNameText != null) {
            bundle.putString("DEV_NAME", this.mDeviceNameText.getText().toString());
        }
        if (this.mSelectedGroup != null) {
            bundle.putString("GROUP_NAME", this.mSelectedGroup.getGroupName());
        }
    }

    private void handlePeersChanged() {
        this.mPeerCategoryController.removeAllChildren();
        this.mConnectedDevices = 0;
        Log.d("WifiP2pSettings", "List of available peers");
        for (WifiP2pDevice wifiP2pDevice : this.mPeers.getDeviceList()) {
            Log.d("WifiP2pSettings", "-> " + wifiP2pDevice);
            this.mPeerCategoryController.addChild(new WifiP2pPeer(getPrefContext(), wifiP2pDevice));
            if (wifiP2pDevice.status == 0) {
                this.mConnectedDevices++;
            }
        }
        Log.d("WifiP2pSettings", " mConnectedDevices " + this.mConnectedDevices);
    }

    public void onPersistentGroupInfoAvailable(WifiP2pGroupList wifiP2pGroupList) {
        this.mPersistentCategoryController.removeAllChildren();
        for (WifiP2pGroup wifiP2pGroup : wifiP2pGroupList.getGroupList()) {
            Log.d("WifiP2pSettings", " group " + wifiP2pGroup);
            WifiP2pPersistentGroup wifiP2pPersistentGroup = new WifiP2pPersistentGroup(getPrefContext(), wifiP2pGroup);
            this.mPersistentCategoryController.addChild(wifiP2pPersistentGroup);
            if (wifiP2pPersistentGroup.getGroupName().equals(this.mSelectedGroupName)) {
                Log.d("WifiP2pSettings", "Selecting group " + wifiP2pPersistentGroup.getGroupName());
                this.mSelectedGroup = wifiP2pPersistentGroup;
                this.mSelectedGroupName = null;
            }
        }
        if (this.mSelectedGroupName != null) {
            Log.w("WifiP2pSettings", " Selected group " + this.mSelectedGroupName + " disappered on next query ");
        }
    }

    @Override // android.net.wifi.p2p.WifiP2pManager.PeerListListener
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        Log.d("WifiP2pSettings", "Requested peers are available");
        this.mPeers = wifiP2pDeviceList;
        handlePeersChanged();
    }

    private void handleP2pStateChanged() {
        updateSearchMenu(false);
        this.mThisDevicePreferenceController.setEnabled(this.mWifiP2pEnabled);
        this.mPersistentCategoryController.setEnabled(this.mWifiP2pEnabled);
        this.mPeerCategoryController.setEnabled(this.mWifiP2pEnabled);
    }

    private void updateSearchMenu(boolean z) {
        this.mWifiP2pSearching = z;
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void startSearch() {
        if (this.mWifiP2pManager != null && !this.mWifiP2pSearching) {
            this.mWifiP2pManager.discoverPeers(this.mChannel, new WifiP2pManager.ActionListener() { // from class: com.android.settings.wifi.p2p.WifiP2pSettings.7
                @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                public void onSuccess() {
                }

                @Override // android.net.wifi.p2p.WifiP2pManager.ActionListener
                public void onFailure(int i) {
                    Log.d("WifiP2pSettings", " discover fail " + i);
                }
            });
        }
    }
}
