package com.android.settings.wifi.details;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.text.BidiFormatter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.wifi.WifiDetailPreference;
import com.android.settings.wifi.WifiDialog;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.wifi.AccessPoint;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WifiDetailPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, WifiDialog.WifiDialogListener, LifecycleObserver, OnPause, OnResume {
    private static final boolean DEBUG = Log.isLoggable("WifiDetailsPrefCtrl", 3);

    @VisibleForTesting
    static final String KEY_BUTTONS_PREF = "buttons";

    @VisibleForTesting
    static final String KEY_DNS_PREF = "dns";

    @VisibleForTesting
    static final String KEY_FREQUENCY_PREF = "frequency";

    @VisibleForTesting
    static final String KEY_GATEWAY_PREF = "gateway";

    @VisibleForTesting
    static final String KEY_HEADER = "connection_header";

    @VisibleForTesting
    static final String KEY_IPV6_ADDRESSES_PREF = "ipv6_addresses";

    @VisibleForTesting
    static final String KEY_IPV6_CATEGORY = "ipv6_category";

    @VisibleForTesting
    static final String KEY_IP_ADDRESS_PREF = "ip_address";

    @VisibleForTesting
    static final String KEY_LINK_SPEED = "link_speed";

    @VisibleForTesting
    static final String KEY_MAC_ADDRESS_PREF = "mac_address";

    @VisibleForTesting
    static final String KEY_SECURITY_PREF = "security";

    @VisibleForTesting
    static final String KEY_SIGNAL_STRENGTH_PREF = "signal_strength";

    @VisibleForTesting
    static final String KEY_SUBNET_MASK_PREF = "subnet_mask";
    private AccessPoint mAccessPoint;
    private ActionButtonPreference mButtonsPref;
    private final ConnectivityManager mConnectivityManager;
    private WifiDetailPreference mDnsPref;
    private EntityHeaderController mEntityHeaderController;
    private final IntentFilter mFilter;
    private final Fragment mFragment;
    private WifiDetailPreference mFrequencyPref;
    private WifiDetailPreference mGatewayPref;
    private final Handler mHandler;
    private final IconInjector mIconInjector;
    private WifiDetailPreference mIpAddressPref;
    private Preference mIpv6AddressPref;
    private PreferenceCategory mIpv6Category;
    private LinkProperties mLinkProperties;
    private WifiDetailPreference mLinkSpeedPref;
    private WifiDetailPreference mMacAddressPref;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Network mNetwork;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private NetworkCapabilities mNetworkCapabilities;
    private NetworkInfo mNetworkInfo;
    private final NetworkRequest mNetworkRequest;
    private final BroadcastReceiver mReceiver;
    private int mRssiSignalLevel;
    private WifiDetailPreference mSecurityPref;
    private String[] mSignalStr;
    private WifiDetailPreference mSignalStrengthPref;
    private WifiDetailPreference mSubnetPref;
    private WifiConfiguration mWifiConfig;
    private WifiInfo mWifiInfo;
    private final WifiManager mWifiManager;

    public static WifiDetailPreferenceController newInstance(AccessPoint accessPoint, ConnectivityManager connectivityManager, Context context, Fragment fragment, Handler handler, Lifecycle lifecycle, WifiManager wifiManager, MetricsFeatureProvider metricsFeatureProvider) {
        return new WifiDetailPreferenceController(accessPoint, connectivityManager, context, fragment, handler, lifecycle, wifiManager, metricsFeatureProvider, new IconInjector(context));
    }

    @VisibleForTesting
    WifiDetailPreferenceController(AccessPoint accessPoint, ConnectivityManager connectivityManager, Context context, Fragment fragment, Handler handler, Lifecycle lifecycle, WifiManager wifiManager, MetricsFeatureProvider metricsFeatureProvider, IconInjector iconInjector) {
        super(context);
        this.mRssiSignalLevel = -1;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                byte b;
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                if (iHashCode != -385684331) {
                    if (iHashCode != -343630553) {
                        b = (iHashCode == 1625920338 && action.equals("android.net.wifi.CONFIGURED_NETWORKS_CHANGE")) ? (byte) 0 : (byte) -1;
                    } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                        b = 1;
                    }
                } else if (action.equals("android.net.wifi.RSSI_CHANGED")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        if (!intent.getBooleanExtra("multipleChanges", false)) {
                            WifiConfiguration wifiConfiguration = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
                            if (WifiDetailPreferenceController.this.mAccessPoint.matches(wifiConfiguration)) {
                                WifiDetailPreferenceController.this.mWifiConfig = wifiConfiguration;
                            }
                        }
                        break;
                    case 1:
                    case 2:
                        break;
                    default:
                        return;
                }
                WifiDetailPreferenceController.this.updateInfo();
            }
        };
        this.mNetworkRequest = new NetworkRequest.Builder().clearCapabilities().addTransportType(1).build();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                if (network.equals(WifiDetailPreferenceController.this.mNetwork) && !linkProperties.equals(WifiDetailPreferenceController.this.mLinkProperties)) {
                    WifiDetailPreferenceController.this.mLinkProperties = linkProperties;
                    WifiDetailPreferenceController.this.updateIpLayerInfo();
                }
            }

            private boolean hasCapabilityChanged(NetworkCapabilities networkCapabilities, int i) {
                return WifiDetailPreferenceController.this.mNetworkCapabilities == null || WifiDetailPreferenceController.this.mNetworkCapabilities.hasCapability(i) != networkCapabilities.hasCapability(i);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (network.equals(WifiDetailPreferenceController.this.mNetwork) && !networkCapabilities.equals(WifiDetailPreferenceController.this.mNetworkCapabilities)) {
                    if (hasCapabilityChanged(networkCapabilities, 16) || hasCapabilityChanged(networkCapabilities, 17)) {
                        WifiDetailPreferenceController.this.refreshNetworkState();
                    }
                    WifiDetailPreferenceController.this.mNetworkCapabilities = networkCapabilities;
                    WifiDetailPreferenceController.this.updateIpLayerInfo();
                }
            }

            @Override
            public void onLost(Network network) {
                if (network.equals(WifiDetailPreferenceController.this.mNetwork)) {
                    WifiDetailPreferenceController.this.exitActivity();
                }
            }
        };
        this.mAccessPoint = accessPoint;
        this.mConnectivityManager = connectivityManager;
        this.mFragment = fragment;
        this.mHandler = handler;
        this.mSignalStr = context.getResources().getStringArray(R.array.wifi_signal);
        this.mWifiConfig = accessPoint.getConfig();
        this.mWifiManager = wifiManager;
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mIconInjector = iconInjector;
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        setupEntityHeader(preferenceScreen);
        this.mButtonsPref = ((ActionButtonPreference) preferenceScreen.findPreference(KEY_BUTTONS_PREF)).setButton1Text(R.string.forget).setButton1Positive(false).setButton1OnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.forgetNetwork();
            }
        }).setButton2Text(R.string.wifi_sign_in_button_text).setButton2Positive(true).setButton2OnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.signIntoNetwork();
            }
        });
        this.mSignalStrengthPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_SIGNAL_STRENGTH_PREF);
        this.mLinkSpeedPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_LINK_SPEED);
        this.mFrequencyPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_FREQUENCY_PREF);
        this.mSecurityPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_SECURITY_PREF);
        this.mMacAddressPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_MAC_ADDRESS_PREF);
        this.mIpAddressPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_IP_ADDRESS_PREF);
        this.mGatewayPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_GATEWAY_PREF);
        this.mSubnetPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_SUBNET_MASK_PREF);
        this.mDnsPref = (WifiDetailPreference) preferenceScreen.findPreference(KEY_DNS_PREF);
        this.mIpv6Category = (PreferenceCategory) preferenceScreen.findPreference(KEY_IPV6_CATEGORY);
        this.mIpv6AddressPref = preferenceScreen.findPreference(KEY_IPV6_ADDRESSES_PREF);
        this.mSecurityPref.setDetailText(this.mAccessPoint.getSecurityString(false));
    }

    private void setupEntityHeader(PreferenceScreen preferenceScreen) {
        LayoutPreference layoutPreference = (LayoutPreference) preferenceScreen.findPreference(KEY_HEADER);
        this.mEntityHeaderController = EntityHeaderController.newInstance(this.mFragment.getActivity(), this.mFragment, layoutPreference.findViewById(R.id.entity_header));
        ImageView imageView = (ImageView) layoutPreference.findViewById(R.id.entity_header_icon);
        imageView.setBackground(this.mContext.getDrawable(R.drawable.ic_settings_widget_background));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        this.mEntityHeaderController.setLabel(this.mAccessPoint.getSsidStr());
    }

    @Override
    public void onResume() {
        this.mNetwork = this.mWifiManager.getCurrentNetwork();
        this.mLinkProperties = this.mConnectivityManager.getLinkProperties(this.mNetwork);
        this.mNetworkCapabilities = this.mConnectivityManager.getNetworkCapabilities(this.mNetwork);
        updateInfo();
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
        this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback, this.mHandler);
    }

    @Override
    public void onPause() {
        this.mNetwork = null;
        this.mLinkProperties = null;
        this.mNetworkCapabilities = null;
        this.mNetworkInfo = null;
        this.mWifiInfo = null;
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
    }

    private void updateInfo() {
        this.mNetworkInfo = this.mConnectivityManager.getNetworkInfo(this.mNetwork);
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mNetwork == null || this.mNetworkInfo == null || this.mWifiInfo == null) {
            exitActivity();
            return;
        }
        this.mButtonsPref.setButton1Visible(canForgetNetwork());
        refreshNetworkState();
        refreshRssiViews();
        this.mMacAddressPref.setDetailText(this.mWifiInfo.getMacAddress());
        this.mLinkSpeedPref.setVisible(this.mWifiInfo.getLinkSpeed() >= 0);
        this.mLinkSpeedPref.setDetailText(this.mContext.getString(R.string.link_speed, Integer.valueOf(this.mWifiInfo.getLinkSpeed())));
        int frequency = this.mWifiInfo.getFrequency();
        String string = null;
        if (frequency >= 2400 && frequency < 2500) {
            string = this.mContext.getResources().getString(R.string.wifi_band_24ghz);
        } else if (frequency >= 4900 && frequency < 5900) {
            string = this.mContext.getResources().getString(R.string.wifi_band_5ghz);
        } else {
            Log.e("WifiDetailsPrefCtrl", "Unexpected frequency " + frequency);
        }
        this.mFrequencyPref.setDetailText(string);
        updateIpLayerInfo();
    }

    private void exitActivity() {
        if (DEBUG) {
            Log.d("WifiDetailsPrefCtrl", "Exiting the WifiNetworkDetailsPage");
        }
        this.mFragment.getActivity().finish();
    }

    private void refreshNetworkState() {
        this.mAccessPoint.update(this.mWifiConfig, this.mWifiInfo, this.mNetworkInfo);
        this.mEntityHeaderController.setSummary(this.mAccessPoint.getSettingsSummary()).done(this.mFragment.getActivity(), true);
    }

    private void refreshRssiViews() {
        int level = this.mAccessPoint.getLevel();
        if (this.mRssiSignalLevel == level) {
            return;
        }
        this.mRssiSignalLevel = level;
        Drawable icon = this.mIconInjector.getIcon(this.mRssiSignalLevel);
        icon.setTint(Utils.getColorAccent(this.mContext));
        this.mEntityHeaderController.setIcon(icon).done(this.mFragment.getActivity(), true);
        Drawable drawableMutate = icon.getConstantState().newDrawable().mutate();
        drawableMutate.setTint(this.mContext.getResources().getColor(R.color.wifi_details_icon_color, this.mContext.getTheme()));
        this.mSignalStrengthPref.setIcon(drawableMutate);
        this.mSignalStrengthPref.setDetailText(this.mSignalStr[this.mRssiSignalLevel]);
    }

    private void updatePreference(WifiDetailPreference wifiDetailPreference, String str) {
        if (!TextUtils.isEmpty(str)) {
            wifiDetailPreference.setDetailText(str);
            wifiDetailPreference.setVisible(true);
        } else {
            wifiDetailPreference.setVisible(false);
        }
    }

    private void updateIpLayerInfo() {
        this.mButtonsPref.setButton2Visible(canSignIntoNetwork());
        this.mButtonsPref.setVisible(canSignIntoNetwork() || canForgetNetwork());
        if (this.mNetwork == null || this.mLinkProperties == null) {
            this.mIpAddressPref.setVisible(false);
            this.mSubnetPref.setVisible(false);
            this.mGatewayPref.setVisible(false);
            this.mDnsPref.setVisible(false);
            this.mIpv6Category.setVisible(false);
            return;
        }
        StringJoiner stringJoiner = new StringJoiner("\n");
        String hostAddress = null;
        String hostAddress2 = null;
        String strIpv4PrefixLengthToSubnetMask = null;
        for (LinkAddress linkAddress : this.mLinkProperties.getLinkAddresses()) {
            if (linkAddress.getAddress() instanceof Inet4Address) {
                hostAddress2 = linkAddress.getAddress().getHostAddress();
                strIpv4PrefixLengthToSubnetMask = ipv4PrefixLengthToSubnetMask(linkAddress.getPrefixLength());
            } else if (linkAddress.getAddress() instanceof Inet6Address) {
                stringJoiner.add(linkAddress.getAddress().getHostAddress());
            }
        }
        Iterator<RouteInfo> it = this.mLinkProperties.getRoutes().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            RouteInfo next = it.next();
            if (next.isIPv4Default() && next.hasGateway()) {
                hostAddress = next.getGateway().getHostAddress();
                break;
            }
        }
        String str = (String) this.mLinkProperties.getDnsServers().stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((InetAddress) obj).getHostAddress();
            }
        }).collect(Collectors.joining("\n"));
        updatePreference(this.mIpAddressPref, hostAddress2);
        updatePreference(this.mSubnetPref, strIpv4PrefixLengthToSubnetMask);
        updatePreference(this.mGatewayPref, hostAddress);
        updatePreference(this.mDnsPref, str);
        if (stringJoiner.length() > 0) {
            this.mIpv6AddressPref.setSummary(BidiFormatter.getInstance().unicodeWrap(stringJoiner.toString()));
            this.mIpv6Category.setVisible(true);
        } else {
            this.mIpv6Category.setVisible(false);
        }
    }

    private static String ipv4PrefixLengthToSubnetMask(int i) {
        try {
            return NetworkUtils.getNetworkPart(InetAddress.getByAddress(new byte[]{-1, -1, -1, -1}), i).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private boolean canForgetNetwork() {
        return (this.mWifiInfo != null && this.mWifiInfo.isEphemeral()) || canModifyNetwork();
    }

    public boolean canModifyNetwork() {
        return (this.mWifiConfig == null || WifiUtils.isNetworkLockedDown(this.mContext, this.mWifiConfig)) ? false : true;
    }

    private boolean canSignIntoNetwork() {
        return WifiUtils.canSignIntoNetwork(this.mNetworkCapabilities);
    }

    private void forgetNetwork() {
        if (this.mWifiInfo != null && this.mWifiInfo.isEphemeral()) {
            this.mWifiManager.disableEphemeralNetwork(this.mWifiInfo.getSSID());
        } else if (this.mWifiConfig != null) {
            if (this.mWifiConfig.isPasspoint()) {
                this.mWifiManager.removePasspointConfiguration(this.mWifiConfig.FQDN);
            } else {
                this.mWifiManager.forget(this.mWifiConfig.networkId, null);
            }
        }
        this.mMetricsFeatureProvider.action(this.mFragment.getActivity(), 137, new Pair[0]);
        this.mFragment.getActivity().finish();
    }

    private void signIntoNetwork() {
        this.mMetricsFeatureProvider.action(this.mFragment.getActivity(), 1008, new Pair[0]);
        this.mConnectivityManager.startCaptivePortalApp(this.mNetwork);
    }

    @Override
    public void onForget(WifiDialog wifiDialog) {
    }

    @Override
    public void onSubmit(WifiDialog wifiDialog) {
        if (wifiDialog.getController() != null) {
            this.mWifiManager.save(wifiDialog.getController().getConfig(), new WifiManager.ActionListener() {
                public void onSuccess() {
                }

                public void onFailure(int i) {
                    Activity activity = WifiDetailPreferenceController.this.mFragment.getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
                    }
                }
            });
        }
    }

    @VisibleForTesting
    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            this.mContext = context;
        }

        public Drawable getIcon(int i) {
            return this.mContext.getDrawable(Utils.getWifiIconResource(i)).mutate();
        }
    }
}
