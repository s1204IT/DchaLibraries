package com.android.settings.wfd;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.app.MediaRouteDialogPresenter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public final class WifiDisplaySettings extends SettingsPreferenceFragment {
    private boolean mAutoGO;
    private PreferenceGroup mCertCategory;
    private DisplayManager mDisplayManager;
    private TextView mEmptyView;
    private boolean mListen;
    private int mListenChannel;
    private int mOperatingChannel;
    private int mPendingChanges;
    private MediaRouter mRouter;
    private boolean mStarted;
    private boolean mWifiDisplayCertificationOn;
    private boolean mWifiDisplayOnSetting;
    private WifiDisplayStatus mWifiDisplayStatus;
    private WifiP2pManager.Channel mWifiP2pChannel;
    private WifiP2pManager mWifiP2pManager;
    private int mWpsConfig = 4;
    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            int changes = WifiDisplaySettings.this.mPendingChanges;
            WifiDisplaySettings.this.mPendingChanges = 0;
            WifiDisplaySettings.this.update(changes);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED")) {
                WifiDisplaySettings.this.scheduleUpdate(4);
            }
        }
    };
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            WifiDisplaySettings.this.scheduleUpdate(1);
        }
    };
    private final MediaRouter.Callback mRouterCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }
    };
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mRouter = (MediaRouter) context.getSystemService("media_router");
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, Looper.getMainLooper(), null);
        addPreferencesFromResource(R.xml.wifi_display_settings);
        setHasOptionsMenu(true);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_remote_display;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        this.mEmptyView.setText(R.string.wifi_display_no_devices_found);
        getListView().setEmptyView(this.mEmptyView);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mStarted = true;
        Context context = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        context.registerReceiver(this.mReceiver, filter);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_on"), false, this.mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_certification_on"), false, this.mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_wps_config"), false, this.mSettingsObserver);
        this.mRouter.addCallback(4, this.mRouterCallback, 1);
        update(-1);
        Settings.System.putInt(getContentResolver(), "skip_sta_scan_in_p2p_ui", 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mStarted = false;
        Context context = getActivity();
        context.unregisterReceiver(this.mReceiver);
        getContentResolver().unregisterContentObserver(this.mSettingsObserver);
        this.mRouter.removeCallback(this.mRouterCallback);
        unscheduleUpdate();
        Settings.System.putInt(getContentResolver(), "skip_sta_scan_in_p2p_ui", 0);
        WifiP2pManager mWifiP2pManager = (WifiP2pManager) getSystemService("wifip2p");
        if (mWifiP2pManager != null) {
            WifiP2pManager.Channel mChannel = mWifiP2pManager.initialize(getActivity(), getActivity().getMainLooper(), null);
            if (mChannel == null) {
                Log.e("WifiDisplaySettings", "Failed to set up connection with wifi p2p service");
                return;
            } else {
                Log.i("WifiDisplaySettings", "Stop peer discovery");
                mWifiP2pManager.stopPeerDiscovery(mChannel, null);
                return;
            }
        }
        Log.e("WifiDisplaySettings", "mWifiP2pManager is null !");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mWifiDisplayStatus != null && this.mWifiDisplayStatus.getFeatureState() != 0) {
            MenuItem item = menu.add(0, 1, 0, R.string.wifi_display_enable_menu_item);
            item.setCheckable(true);
            item.setChecked(this.mWifiDisplayOnSetting);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                this.mWifiDisplayOnSetting = !item.isChecked();
                item.setChecked(this.mWifiDisplayOnSetting);
                Settings.Global.putInt(getContentResolver(), "wifi_display_on", this.mWifiDisplayOnSetting ? 1 : 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scheduleUpdate(int changes) {
        if (this.mStarted) {
            if (this.mPendingChanges == 0) {
                this.mHandler.post(this.mUpdateRunnable);
            }
            this.mPendingChanges |= changes;
        }
    }

    private void unscheduleUpdate() {
        if (this.mPendingChanges != 0) {
            this.mPendingChanges = 0;
            this.mHandler.removeCallbacks(this.mUpdateRunnable);
        }
    }

    private void update(int changes) {
        boolean invalidateOptions = false;
        if ((changes & 1) != 0) {
            this.mWifiDisplayOnSetting = Settings.Global.getInt(getContentResolver(), "wifi_display_on", 0) != 0;
            this.mWifiDisplayCertificationOn = Settings.Global.getInt(getContentResolver(), "wifi_display_certification_on", 0) != 0;
            this.mWpsConfig = Settings.Global.getInt(getContentResolver(), "wifi_display_wps_config", 4);
            invalidateOptions = true;
        }
        if ((changes & 4) != 0) {
            this.mWifiDisplayStatus = this.mDisplayManager.getWifiDisplayStatus();
            invalidateOptions = true;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        int routeCount = this.mRouter.getRouteCount();
        for (int i = 0; i < routeCount; i++) {
            MediaRouter.RouteInfo route = this.mRouter.getRouteAt(i);
            if (route.matchesTypes(4)) {
                preferenceScreen.addPreference(createRoutePreference(route));
            }
        }
        if (this.mWifiDisplayStatus != null && this.mWifiDisplayStatus.getFeatureState() == 3) {
            WifiDisplay[] arr$ = this.mWifiDisplayStatus.getDisplays();
            for (WifiDisplay display : arr$) {
                if (!display.isRemembered() && display.isAvailable() && !display.equals(this.mWifiDisplayStatus.getActiveDisplay())) {
                    preferenceScreen.addPreference(new UnpairedWifiDisplayPreference(getActivity(), display));
                }
            }
            if (this.mWifiDisplayCertificationOn) {
                buildCertificationMenu(preferenceScreen);
            }
        }
        if (invalidateOptions) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private RoutePreference createRoutePreference(MediaRouter.RouteInfo route) {
        WifiDisplay display = findWifiDisplay(route.getDeviceAddress());
        return display != null ? new WifiDisplayRoutePreference(getActivity(), route, display) : new RoutePreference(getActivity(), route);
    }

    private WifiDisplay findWifiDisplay(String deviceAddress) {
        if (this.mWifiDisplayStatus != null && deviceAddress != null) {
            WifiDisplay[] arr$ = this.mWifiDisplayStatus.getDisplays();
            for (WifiDisplay display : arr$) {
                if (display.getDeviceAddress().equals(deviceAddress)) {
                    return display;
                }
            }
        }
        return null;
    }

    private void buildCertificationMenu(PreferenceScreen preferenceScreen) {
        if (this.mCertCategory == null) {
            this.mCertCategory = new PreferenceCategory(getActivity());
            this.mCertCategory.setTitle(R.string.wifi_display_certification_heading);
            this.mCertCategory.setOrder(1);
        } else {
            this.mCertCategory.removeAll();
        }
        preferenceScreen.addPreference(this.mCertCategory);
        if (!this.mWifiDisplayStatus.getSessionInfo().getGroupId().isEmpty()) {
            Preference p = new Preference(getActivity());
            p.setTitle(R.string.wifi_display_session_info);
            p.setSummary(this.mWifiDisplayStatus.getSessionInfo().toString());
            this.mCertCategory.addPreference(p);
            if (this.mWifiDisplayStatus.getSessionInfo().getSessionId() != 0) {
                this.mCertCategory.addPreference(new Preference(getActivity()) {
                    @Override
                    public View getView(View convertView, ViewGroup parent) {
                        View v;
                        if (convertView == null) {
                            LayoutInflater li = (LayoutInflater) WifiDisplaySettings.this.getActivity().getSystemService("layout_inflater");
                            v = li.inflate(R.layout.two_buttons_panel, (ViewGroup) null);
                        } else {
                            v = convertView;
                        }
                        Button b = (Button) v.findViewById(R.id.left_button);
                        b.setText(R.string.wifi_display_pause);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v2) {
                                WifiDisplaySettings.this.mDisplayManager.pauseWifiDisplay();
                            }
                        });
                        Button b2 = (Button) v.findViewById(R.id.right_button);
                        b2.setText(R.string.wifi_display_resume);
                        b2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v2) {
                                WifiDisplaySettings.this.mDisplayManager.resumeWifiDisplay();
                            }
                        });
                        return v;
                    }
                });
            }
        }
        SwitchPreference pref = new SwitchPreference(getActivity()) {
            @Override
            protected void onClick() {
                WifiDisplaySettings.this.mListen = !WifiDisplaySettings.this.mListen;
                WifiDisplaySettings.this.setListenMode(WifiDisplaySettings.this.mListen);
                setChecked(WifiDisplaySettings.this.mListen);
            }
        };
        pref.setTitle(R.string.wifi_display_listen_mode);
        pref.setChecked(this.mListen);
        this.mCertCategory.addPreference(pref);
        SwitchPreference pref2 = new SwitchPreference(getActivity()) {
            @Override
            protected void onClick() {
                WifiDisplaySettings.this.mAutoGO = !WifiDisplaySettings.this.mAutoGO;
                if (WifiDisplaySettings.this.mAutoGO) {
                    WifiDisplaySettings.this.startAutoGO();
                } else {
                    WifiDisplaySettings.this.stopAutoGO();
                }
                setChecked(WifiDisplaySettings.this.mAutoGO);
            }
        };
        pref2.setTitle(R.string.wifi_display_autonomous_go);
        pref2.setChecked(this.mAutoGO);
        this.mCertCategory.addPreference(pref2);
        ListPreference lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mWpsConfig = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    Settings.Global.putInt(WifiDisplaySettings.this.getActivity().getContentResolver(), "wifi_display_wps_config", WifiDisplaySettings.this.mWpsConfig);
                }
            }
        };
        this.mWpsConfig = Settings.Global.getInt(getActivity().getContentResolver(), "wifi_display_wps_config", 4);
        String[] wpsEntries = {"Default", "PBC", "KEYPAD", "DISPLAY"};
        String[] wpsValues = {"4", "0", "2", "1"};
        lp.setTitle(R.string.wifi_display_wps_config);
        lp.setEntries(wpsEntries);
        lp.setEntryValues(wpsValues);
        lp.setValue("" + this.mWpsConfig);
        lp.setSummary("%1$s");
        this.mCertCategory.addPreference(lp);
        ListPreference lp2 = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mListenChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    WifiDisplaySettings.this.setWifiP2pChannels(WifiDisplaySettings.this.mListenChannel, WifiDisplaySettings.this.mOperatingChannel);
                }
            }
        };
        String[] lcEntries = {"Auto", "1", "6", "11"};
        String[] lcValues = {"0", "1", "6", "11"};
        lp2.setTitle(R.string.wifi_display_listen_channel);
        lp2.setEntries(lcEntries);
        lp2.setEntryValues(lcValues);
        lp2.setValue("" + this.mListenChannel);
        lp2.setSummary("%1$s");
        this.mCertCategory.addPreference(lp2);
        ListPreference lp3 = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mOperatingChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    WifiDisplaySettings.this.setWifiP2pChannels(WifiDisplaySettings.this.mListenChannel, WifiDisplaySettings.this.mOperatingChannel);
                }
            }
        };
        String[] ocEntries = {"Auto", "1", "6", "11", "36"};
        String[] ocValues = {"0", "1", "6", "11", "36"};
        lp3.setTitle(R.string.wifi_display_operating_channel);
        lp3.setEntries(ocEntries);
        lp3.setEntryValues(ocValues);
        lp3.setValue("" + this.mOperatingChannel);
        lp3.setSummary("%1$s");
        this.mCertCategory.addPreference(lp3);
    }

    private void startAutoGO() {
        this.mWifiP2pManager.createGroup(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to start AutoGO with reason " + reason + ".");
            }
        });
    }

    private void stopAutoGO() {
        this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to stop AutoGO with reason " + reason + ".");
            }
        });
    }

    private void setListenMode(final boolean enable) {
        this.mWifiP2pManager.listen(this.mWifiP2pChannel, enable, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to " + (enable ? "entered" : "exited") + " listen mode with reason " + reason + ".");
            }
        });
    }

    private void setWifiP2pChannels(int lc, int oc) {
        this.mWifiP2pManager.setWifiP2pChannels(this.mWifiP2pChannel, lc, oc, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to set wifi p2p channels with reason " + reason + ".");
            }
        });
    }

    private void toggleRoute(MediaRouter.RouteInfo route) {
        if (route.isSelected()) {
            MediaRouteDialogPresenter.showDialogFragment(getActivity(), 4, (View.OnClickListener) null);
        } else {
            route.select();
        }
    }

    private void pairWifiDisplay(WifiDisplay display) {
        if (display.canConnect()) {
            this.mDisplayManager.connectWifiDisplay(display.getDeviceAddress());
        }
    }

    private void showWifiDisplayOptionsDialog(final WifiDisplay display) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.wifi_display_options, (ViewGroup) null);
        final EditText nameEditText = (EditText) view.findViewById(R.id.name);
        nameEditText.setText(display.getFriendlyDisplayName());
        DialogInterface.OnClickListener done = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEditText.getText().toString().trim();
                if (name.isEmpty() || name.equals(display.getDeviceName())) {
                    name = null;
                }
                WifiDisplaySettings.this.mDisplayManager.renameWifiDisplay(display.getDeviceAddress(), name);
            }
        };
        DialogInterface.OnClickListener forget = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiDisplaySettings.this.mDisplayManager.forgetWifiDisplay(display.getDeviceAddress());
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setCancelable(true).setTitle(R.string.wifi_display_options_title).setView(view).setPositiveButton(R.string.wifi_display_options_done, done).setNegativeButton(R.string.wifi_display_options_forget, forget).create();
        dialog.show();
    }

    private class RoutePreference extends Preference implements Preference.OnPreferenceClickListener {
        private final MediaRouter.RouteInfo mRoute;

        public RoutePreference(Context context, MediaRouter.RouteInfo route) {
            super(context);
            this.mRoute = route;
            setTitle(route.getName());
            setSummary(route.getDescription());
            setEnabled(route.isEnabled());
            if (route.isSelected()) {
                setOrder(2);
                if (route.isConnecting()) {
                    setSummary(R.string.wifi_display_status_connecting);
                } else {
                    setSummary(R.string.wifi_display_status_connected);
                }
            } else if (isEnabled()) {
                setOrder(3);
            } else {
                setOrder(4);
                if (route.getStatusCode() == 5) {
                    setSummary(R.string.wifi_display_status_in_use);
                } else {
                    setSummary(R.string.wifi_display_status_not_available);
                }
            }
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            WifiDisplaySettings.this.toggleRoute(this.mRoute);
            return true;
        }
    }

    private class WifiDisplayRoutePreference extends RoutePreference implements View.OnClickListener {
        private final WifiDisplay mDisplay;

        public WifiDisplayRoutePreference(Context context, MediaRouter.RouteInfo route, WifiDisplay display) {
            super(context, route);
            this.mDisplay = display;
            setWidgetLayoutResource(R.layout.wifi_display_preference);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            ImageView deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails);
            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
                if (!isEnabled()) {
                    TypedValue value = new TypedValue();
                    getContext().getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true);
                    deviceDetails.setImageAlpha((int) (value.getFloat() * 255.0f));
                    deviceDetails.setEnabled(true);
                }
            }
        }

        @Override
        public void onClick(View v) {
            WifiDisplaySettings.this.showWifiDisplayOptionsDialog(this.mDisplay);
        }
    }

    private class UnpairedWifiDisplayPreference extends Preference implements Preference.OnPreferenceClickListener {
        private final WifiDisplay mDisplay;

        public UnpairedWifiDisplayPreference(Context context, WifiDisplay display) {
            super(context);
            this.mDisplay = display;
            setTitle(display.getFriendlyDisplayName());
            setSummary(android.R.string.me);
            setEnabled(display.canConnect());
            if (isEnabled()) {
                setOrder(3);
            } else {
                setOrder(4);
                setSummary(R.string.wifi_display_status_in_use);
            }
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            WifiDisplaySettings.this.pairWifiDisplay(this.mDisplay);
            return true;
        }
    }
}
