package com.android.phone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.NetworkQueryService;
import com.android.phone.RoamingDialogFragment;
import com.android.phone.settings.PhoneAccountSettingsFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.Enhanced4GLteSwitchPreference;
import com.mediatek.settings.MobileNetworkSettingsOmEx;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.CdmaNetworkSettings;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MobileNetworkSettings extends Activity {
    private static final String KEY_CID = "ro.boot.cid";
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT = "esim.enable_esim_system_ui_by_default";
    private static final String KEY_ESIM_CID_IGNORE = "ro.setupwizard.esim_cid_ignore";

    private enum TabState {
        NO_TABS,
        UPDATE,
        DO_NOTHING
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        MobileNetworkFragment mobileNetworkFragment = (MobileNetworkFragment) getFragmentManager().findFragmentById(R.id.network_setting_content);
        if (mobileNetworkFragment != null) {
            mobileNetworkFragment.onIntentUpdate(intent);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.network_setting);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentById(R.id.network_setting_content) == null) {
            fragmentManager.beginTransaction().add(R.id.network_setting_content, new MobileNetworkFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static boolean showEuiccSettings(Context context) {
        if (!((EuiccManager) context.getSystemService("euicc")).isEnabled()) {
            return false;
        }
        ContentResolver contentResolver = context.getContentResolver();
        String lowerCase = ((TelephonyManager) context.getSystemService("phone")).getNetworkCountryIso().toLowerCase();
        String string = Settings.Global.getString(contentResolver, "euicc_supported_countries");
        boolean z = TextUtils.isEmpty(lowerCase) || (!TextUtils.isEmpty(string) && Arrays.asList(TextUtils.split(string.toLowerCase(), ",")).contains(lowerCase));
        boolean zContains = Arrays.asList(TextUtils.split(SystemProperties.get(KEY_ESIM_CID_IGNORE, ""), ",")).contains(SystemProperties.get(KEY_CID, (String) null));
        boolean z2 = SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true);
        return (Settings.Global.getInt(contentResolver, "development_settings_enabled", 0) != 0) || (Settings.Global.getInt(contentResolver, "euicc_provisioned", 0) != 0) || (!zContains && z2 && z);
    }

    public static boolean hideEnhanced4gLteSettings(Context context) {
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                ImsManager imsManager = ImsManager.getInstance(context, subscriptionInfo.getSimSlotIndex());
                PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(subscriptionInfo.getSubscriptionId());
                if ((imsManager.isVolteEnabledByPlatform() && imsManager.isVolteProvisionedOnDevice()) || carrierConfigForSubId.getBoolean("hide_enhanced_4g_lte_bool")) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public static boolean isDpcApnEnforced(Context context) {
        Cursor cursorQuery = context.getContentResolver().query(Telephony.Carriers.ENFORCE_MANAGED_URI, null, null, null, null);
        if (cursorQuery != null) {
            Throwable th = null;
            try {
                try {
                    if (cursorQuery.getCount() == 1) {
                        cursorQuery.moveToFirst();
                        boolean z = cursorQuery.getInt(0) > 0;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return z;
                    }
                } finally {
                }
            } catch (Throwable th2) {
                if (cursorQuery != null) {
                }
                throw th2;
            }
            if (cursorQuery != null) {
                if (th != null) {
                    try {
                        cursorQuery.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    cursorQuery.close();
                }
            }
            throw th2;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return false;
    }

    public static class MobileNetworkFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, RoamingDialogFragment.RoamingDialogListener {
        private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
        private static final String BUTTON_ADVANCED_OPTIONS_KEY = "advanced_options";
        private static final String BUTTON_CARRIER_SETTINGS_EUICC_KEY = "carrier_settings_euicc_key";
        private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
        private static final String BUTTON_CDMA_ACTIVATE_DEVICE_KEY = "cdma_activate_device_key";
        private static final String BUTTON_CDMA_APN_EXPAND_KEY = "button_cdma_apn_key";
        private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
        private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";
        private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
        private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
        private static final String BUTTON_DATA_USAGE_KEY = "data_usage_summary";
        public static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
        private static final String BUTTON_GSM_APN_EXPAND_KEY = "button_gsm_apn_key";
        private static final String BUTTON_MOBILE_DATA_ENABLE_KEY = "mobile_data_enable";
        public static final String BUTTON_PLMN_LIST = "button_plmn_key";
        public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
        private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
        private static final String BUTTON_SPRINT_ROAMING_SETTINGS = "sprint_roaming_settings";
        private static final String BUTTON_VIDEO_CALLING_KEY = "video_calling_key";
        private static final String BUTTON_WIFI_CALLING_KEY = "wifi_calling_key";
        private static final String CATEGORY_CALLING_KEY = "calling";
        private static final String CATEGORY_CDMA_APN_EXPAND_KEY = "category_cdma_apn_key";
        private static final String CATEGORY_GSM_APN_EXPAND_KEY = "category_gsm_apn_key";
        private static final String CURRENT_TAB = "current_tab";
        private static final String ENHANCED_4G_MODE_ENABLED_SIM2 = "volte_vt_enabled_sim2";
        private static final String EXPAND_ADVANCED_FIELDS = "expand_advanced_fields";
        private static final String EXPAND_EXTRA = "expandable";
        private static final String LOG_TAG = "NetworkSettings";
        private static final int NUM_LAST_PHONE_DIGITS = 4;
        private static final String PROPERTY_MIMS_SUPPORT = "persist.vendor.mims_support";
        public static final int REQUEST_CODE_EXIT_ECM = 17;
        private static final String ROAMING_TAG = "RoamingDialogFragment";
        private static final String SINGLE_LTE_DATA = "single_lte_data";
        private static final int TAB_THRESHOLD = 2;
        private static final String UP_ACTIVITY_CLASS = "com.android.settings.Settings$WirelessSettingsActivity";
        private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
        private static final String iface = "rmnet0";
        private List<SubscriptionInfo> mActiveSubInfos;
        private AdvancedOptionsPreference mAdvancedOptions;
        private SwitchPreference mButton4glte;
        private RestrictedSwitchPreference mButtonDataRoam;
        private ListPreference mButtonEnabledNetworks;
        private NetworkSelectListPreference mButtonNetworkSelect;
        private ListPreference mButtonPreferredNetworkMode;
        private PreferenceCategory mCallingCategory;
        private CdmaNetworkSettings mCdmaNetworkSettings;
        CdmaOptions mCdmaOptions;
        private Preference mClickedPreference;
        private DataUsagePreference mDataUsagePref;
        private Dialog mDialog;
        private Enhanced4GLteSwitchPreference mEnhancedButton4glte;
        private Preference mEuiccSettingsPref;
        private boolean mExpandAdvancedFields;
        GsmUmtsOptions mGsmUmtsOptions;
        private MyHandler mHandler;
        private ImsManager mImsMgr;
        private IntentFilter mIntentFilter;
        private boolean mIsGlobalCdma;
        private Preference mLteDataServicePref;
        private MobileDataPreference mMobileDataPref;
        private boolean mOkClicked;
        private MobileNetworkSettingsOmEx mOmEx;
        private Preference mPLMNPreference;
        private Phone mPhone;
        private boolean mShow4GForLTE;
        private SubscriptionManager mSubscriptionManager;
        private TabHost mTabHost;
        private TelephonyManager mTelephonyManager;
        private UserManager mUm;
        private boolean mUnavailable;
        private SwitchPreference mVideoCallingPref;
        private Preference mWiFiCallingPref;
        private static final boolean DBG = "eng".equals(Build.TYPE);
        static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
        private final ContentObserver mDpcEnforcedContentObserver = new DpcApnEnforcedObserver();
        private final PhoneCallStateListener mPhoneStateListener = new PhoneCallStateListener(this, null);
        private INetworkQueryService mNetworkQueryService = null;
        private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("connection created, binding local service.");
                }
                MobileNetworkFragment.this.mNetworkQueryService = ((NetworkQueryService.LocalBinder) iBinder).getService();
                MobileNetworkFragment.this.setNetworkQueryService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("connection disconnected, cleaning local binding.");
                }
                MobileNetworkFragment.this.mNetworkQueryService = null;
                MobileNetworkFragment.this.setNetworkQueryService();
            }
        };
        private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("onSubscriptionsChanged:");
                }
                if (TelephonyUtils.isHotSwapHanppened(MobileNetworkFragment.this.mActiveSubInfos, PhoneUtils.getActiveSubInfoList())) {
                    if (MobileNetworkFragment.DBG) {
                        MobileNetworkFragment.log("onSubscriptionsChanged:hot swap hanppened");
                    }
                    MobileNetworkFragment.this.dissmissDialog(MobileNetworkFragment.this.mButtonPreferredNetworkMode);
                    MobileNetworkFragment.this.dissmissDialog(MobileNetworkFragment.this.mButtonEnabledNetworks);
                    Activity activity = MobileNetworkFragment.this.getActivity();
                    if (activity != null) {
                        activity.finish();
                        return;
                    }
                    return;
                }
                MobileNetworkFragment.this.initializeSubscriptions();
            }
        };
        private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String str) {
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("onTabChanged...:");
                }
                MobileNetworkFragment.this.updatePhone(MobileNetworkFragment.this.convertTabToSlot(Integer.parseInt(str)));
                MobileNetworkFragment.this.mCurrentTab = Integer.parseInt(str);
                MobileNetworkFragment.this.updateBody();
                MobileNetworkFragment.this.updateScreenStatus();
            }
        };
        private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String str) {
                return new View(MobileNetworkFragment.this.mTabHost.getContext());
            }
        };
        private int mCurrentTab = 0;
        private int mPreNetworkMode = -1;
        private boolean mNetworkRegister = false;
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("action: " + action);
                }
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    MobileNetworkFragment.this.getActivity().finish();
                    return;
                }
                if (action.equals("com.mediatek.intent.action.MSIM_MODE") || action.equals("mediatek.intent.action.ACTION_MD_TYPE_CHANGE") || action.equals("mediatek.intent.action.LOCATED_PLMN_CHANGED")) {
                    MobileNetworkFragment.this.updateScreenStatus();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                    if (MobileNetworkFragment.DBG) {
                        MobileNetworkFragment.log("Siwtch done Action ACTION_SET_PHONE_RAT_FAMILY_DONE received ");
                    }
                    MobileNetworkFragment.this.mPhone = PhoneUtils.getPhoneUsingSubId(MobileNetworkFragment.this.mPhone.getSubId());
                    MobileNetworkFragment.this.updateScreenStatus();
                    return;
                }
                if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                    MobileNetworkFragment.this.mGsmUmtsOptions = null;
                    MobileNetworkFragment.this.mCdmaOptions = null;
                    MobileNetworkFragment.this.updateBody();
                } else if (ExtensionManager.getMobileNetworkSettingsExt().customizeDualVolteReceiveIntent(action) && MobileNetworkFragment.this.mPhone != null) {
                    ExtensionManager.getMobileNetworkSettingsExt().customizeDataEnable(MobileNetworkFragment.this.mPhone.getSubId(), MobileNetworkFragment.this.mMobileDataPref);
                    MobileNetworkFragment.this.updateScreenStatus();
                }
            }
        };
        private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                MobileNetworkFragment.log("onChange...");
                MobileNetworkFragment.this.updateEnhanced4GLteSwitchPreference();
            }
        };
        private ContentObserver mNetworkObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                MobileNetworkFragment.log("mNetworkObserver onChange...");
                MobileNetworkFragment.this.updateBody();
            }
        };

        private class PhoneCallStateListener extends PhoneStateListener {
            private PhoneCallStateListener() {
            }

            PhoneCallStateListener(MobileNetworkFragment mobileNetworkFragment, AnonymousClass1 anonymousClass1) {
                this();
            }

            @Override
            public void onCallStateChanged(int i, String str) {
                if (MobileNetworkFragment.DBG) {
                    MobileNetworkFragment.log("PhoneStateListener.onCallStateChanged: state=" + i);
                }
                MobileNetworkFragment.this.updateWiFiCallState();
                MobileNetworkFragment.this.updateVideoCallState();
                MobileNetworkFragment.this.updateScreenStatus();
                MobileNetworkFragment.this.updateEnhanced4glteEnableState();
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (ExtensionManager.getMobileNetworkSettingsExt().customizeCUVolte()) {
                    MobileNetworkFragment.this.updateEnhanced4glteEnableState();
                }
            }

            protected void updatePhone() {
                int subId;
                if (MobileNetworkFragment.this.mPhone != null && SubscriptionManager.isValidSubscriptionId(MobileNetworkFragment.this.mPhone.getSubId())) {
                    subId = MobileNetworkFragment.this.mPhone.getSubId();
                } else {
                    subId = -1;
                }
                MobileNetworkFragment.this.mTelephonyManager.listen(this, 0);
                this.mSubId = Integer.valueOf(subId);
                if (SubscriptionManager.isValidSubscriptionId(this.mSubId.intValue())) {
                    MobileNetworkFragment.this.mTelephonyManager.listen(this, 32);
                }
            }
        }

        private void setNetworkQueryService() {
            this.mButtonNetworkSelect = (NetworkSelectListPreference) getPreferenceScreen().findPreference(NetworkOperators.BUTTON_NETWORK_SELECT_KEY);
            if (this.mButtonNetworkSelect != null) {
                this.mButtonNetworkSelect.setNetworkQueryService(this.mNetworkQueryService);
            }
        }

        private void bindNetworkQueryService() {
            getContext().startService(new Intent(getContext(), (Class<?>) NetworkQueryService.class));
            getContext().bindService(new Intent(getContext(), (Class<?>) NetworkQueryService.class).setAction("com.android.phone.intent.action.LOCAL_BINDER"), this.mNetworkQueryServiceConnection, 1);
        }

        private void unbindNetworkQueryService() {
            getContext().unbindService(this.mNetworkQueryServiceConnection);
        }

        @Override
        public void onPositiveButtonClick(DialogFragment dialogFragment) {
            this.mPhone.setDataRoamingEnabled(true);
            this.mButtonDataRoam.setChecked(true);
            MetricsLogger.action(getContext(), getMetricsEventCategory(getPreferenceScreen(), this.mButtonDataRoam), true);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            if (getListView() != null) {
                getListView().setDivider(null);
            }
        }

        public void onIntentUpdate(Intent intent) {
            if (!this.mUnavailable) {
                updateCurrentTab(intent);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            sendMetricsEventPreferenceClicked(preferenceScreen, preference);
            int subId = this.mPhone.getSubId();
            if ((this.mCdmaNetworkSettings != null && this.mCdmaNetworkSettings.onPreferenceTreeClick(preferenceScreen, preference)) || ExtensionManager.getMobileNetworkSettingsExt().onPreferenceTreeClick(preferenceScreen, preference) || preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
                return true;
            }
            if (this.mGsmUmtsOptions != null && this.mGsmUmtsOptions.preferenceTreeClick(preference)) {
                return true;
            }
            if (this.mCdmaOptions != null && this.mCdmaOptions.preferenceTreeClick(preference)) {
                if (this.mPhone.isInEcm()) {
                    this.mClickedPreference = preference;
                    startActivityForResult(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null), 17);
                }
                return true;
            }
            if (preference == this.mButtonPreferredNetworkMode) {
                this.mButtonPreferredNetworkMode.setValue(Integer.toString(Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, preferredNetworkMode)));
                return true;
            }
            if (preference == this.mLteDataServicePref) {
                String string = Settings.Global.getString(getActivity().getContentResolver(), "setup_prepaid_data_service_url");
                if (!TextUtils.isEmpty(string)) {
                    if (BenesseExtension.getDchaState() != 0) {
                        return true;
                    }
                    String subscriberId = this.mTelephonyManager.getSubscriberId();
                    if (subscriberId == null) {
                        subscriberId = "";
                    }
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse(TextUtils.isEmpty(string) ? null : TextUtils.expandTemplate(string, subscriberId).toString())));
                } else {
                    Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
                }
                return true;
            }
            if (preference == this.mButtonEnabledNetworks) {
                log("onPreferenceTreeClick settingsNetworkMode: " + Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, preferredNetworkMode));
                return true;
            }
            if (preference == this.mButtonDataRoam) {
                return true;
            }
            if (preference == this.mEuiccSettingsPref) {
                startActivity(new Intent("android.telephony.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS"));
                return true;
            }
            if (preference == this.mWiFiCallingPref || preference == this.mVideoCallingPref || preference == this.mMobileDataPref || preference == this.mDataUsagePref) {
                return false;
            }
            if (preference == this.mAdvancedOptions) {
                this.mExpandAdvancedFields = true;
                updateBody();
                return true;
            }
            preferenceScreen.setEnabled(false);
            return false;
        }

        private int getSlotIdFromIntent(Intent intent) {
            Bundle extras = intent.getExtras();
            return SubscriptionManager.getSlotIndex(extras != null ? extras.getInt("android.provider.extra.SUB_ID", -1) : -1);
        }

        private void initializeSubscriptions() {
            Activity activity = getActivity();
            if (activity == null || activity.isDestroyed()) {
                return;
            }
            if (DBG) {
                log("initializeSubscriptions:+");
            }
            List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
            TabState tabStateIsUpdateTabsNeeded = isUpdateTabsNeeded(activeSubscriptionInfoList);
            this.mActiveSubInfos.clear();
            if (activeSubscriptionInfoList != null) {
                this.mActiveSubInfos.addAll(activeSubscriptionInfoList);
            }
            int i = AnonymousClass1.$SwitchMap$com$android$phone$MobileNetworkSettings$TabState[tabStateIsUpdateTabsNeeded.ordinal()];
            int currentTab = 0;
            switch (i) {
                case 1:
                    if (DBG) {
                        log("initializeSubscriptions: UPDATE");
                    }
                    int currentTab2 = this.mTabHost != null ? this.mTabHost.getCurrentTab() : this.mCurrentTab;
                    if (this.mTabHost != null) {
                        this.mTabHost.clearAllTabs();
                        log("TabHost Clear.");
                    }
                    this.mTabHost = (TabHost) getActivity().findViewById(android.R.id.tabhost);
                    this.mTabHost.setup();
                    while (currentTab < this.mActiveSubInfos.size()) {
                        String strValueOf = String.valueOf(this.mActiveSubInfos.get(currentTab).getDisplayName());
                        if (DBG) {
                            log("initializeSubscriptions:tab=" + currentTab + " name=" + strValueOf);
                        }
                        this.mTabHost.addTab(buildTabSpec(String.valueOf(currentTab), strValueOf));
                        currentTab++;
                    }
                    this.mTabHost.setOnTabChangedListener(this.mTabListener);
                    this.mTabHost.setCurrentTab(currentTab2);
                    currentTab = currentTab2;
                    break;
                case 2:
                    if (DBG) {
                        log("initializeSubscriptions: NO_TABS");
                    }
                    if (this.mTabHost != null) {
                        this.mTabHost.clearAllTabs();
                        this.mTabHost = null;
                    }
                    break;
                case 3:
                    if (DBG) {
                        log("initializeSubscriptions: DO_NOTHING");
                    }
                    if (this.mTabHost != null) {
                        currentTab = this.mTabHost.getCurrentTab();
                    }
                    break;
            }
            updatePhone(convertTabToSlot(currentTab));
            updateBody();
            if (DBG) {
                log("initializeSubscriptions:-");
            }
        }

        private TabState isUpdateTabsNeeded(List<SubscriptionInfo> list) {
            TabState tabState = TabState.DO_NOTHING;
            if (list == null) {
                if (this.mActiveSubInfos.size() >= 2) {
                    if (DBG) {
                        log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                    }
                    tabState = TabState.NO_TABS;
                }
            } else if (list.size() >= 2 || this.mActiveSubInfos.size() < 2) {
                if (list.size() < 2 || this.mActiveSubInfos.size() >= 2) {
                    if (list.size() >= 2) {
                        Iterator<SubscriptionInfo> it = this.mActiveSubInfos.iterator();
                        Iterator<SubscriptionInfo> it2 = list.iterator();
                        while (true) {
                            if (!it2.hasNext()) {
                                break;
                            }
                            SubscriptionInfo next = it2.next();
                            if (!next.getDisplayName().equals(it.next().getDisplayName())) {
                                if (DBG) {
                                    log("isUpdateTabsNeeded: UPDATE, new name=" + ((Object) next.getDisplayName()));
                                }
                                tabState = TabState.UPDATE;
                            }
                        }
                    }
                } else {
                    if (DBG) {
                        log("isUpdateTabsNeeded: UPDATE, size changed");
                    }
                    tabState = TabState.UPDATE;
                }
            } else {
                if (DBG) {
                    log("isUpdateTabsNeeded: NO_TABS, size went to small");
                }
                tabState = TabState.NO_TABS;
            }
            if (DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("isUpdateTabsNeeded:- ");
                sb.append(tabState);
                sb.append(" newSil.size()=");
                sb.append(list != null ? list.size() : 0);
                sb.append(" mActiveSubInfos.size()=");
                sb.append(this.mActiveSubInfos.size());
                Log.i(LOG_TAG, sb.toString());
            }
            return tabState;
        }

        private void updatePhone(int i) {
            SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
            if (activeSubscriptionInfoForSimSlotIndex != null) {
                int phoneId = SubscriptionManager.getPhoneId(activeSubscriptionInfoForSimSlotIndex.getSubscriptionId());
                if (SubscriptionManager.isValidPhoneId(phoneId)) {
                    this.mPhone = PhoneFactory.getPhone(phoneId);
                }
            }
            if (this.mPhone == null) {
                this.mPhone = PhoneGlobals.getPhone();
            }
            log("updatePhone:- slotId=" + i + " sir=" + activeSubscriptionInfoForSimSlotIndex);
            this.mImsMgr = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
            this.mTelephonyManager = new TelephonyManager(this.mPhone.getContext(), this.mPhone.getSubId());
            if (this.mImsMgr == null) {
                log("updatePhone :: Could not get ImsManager instance!");
            } else if (DBG) {
                log("updatePhone :: mImsMgr=" + this.mImsMgr);
            }
        }

        private TabHost.TabSpec buildTabSpec(String str, String str2) {
            return this.mTabHost.newTabSpec(str).setIndicator(str2).setContent(this.mEmptyTabContent);
        }

        private void updateCurrentTab(Intent intent) {
            int slotIdFromIntent = getSlotIdFromIntent(intent);
            if (slotIdFromIntent >= 0 && this.mTabHost != null && this.mTabHost.getCurrentTab() != slotIdFromIntent) {
                this.mTabHost.setCurrentTab(slotIdFromIntent);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putBoolean(EXPAND_ADVANCED_FIELDS, this.mExpandAdvancedFields);
            bundle.putInt(CURRENT_TAB, this.mCurrentTab);
        }

        @Override
        public void onCreate(Bundle bundle) {
            log("onCreate:+++++++++++");
            super.onCreate(bundle);
            Activity activity = getActivity();
            if (activity == null || activity.isDestroyed()) {
                Log.e(LOG_TAG, "onCreate:- with no valid activity.");
                return;
            }
            this.mOmEx = new MobileNetworkSettingsOmEx(activity);
            this.mHandler = new MyHandler(this, null);
            this.mUm = (UserManager) activity.getSystemService("user");
            this.mSubscriptionManager = SubscriptionManager.from(activity);
            this.mTelephonyManager = (TelephonyManager) activity.getSystemService("phone");
            if (bundle != null) {
                this.mExpandAdvancedFields = bundle.getBoolean(EXPAND_ADVANCED_FIELDS, false);
            } else if (getActivity().getIntent().getBooleanExtra(EXPAND_EXTRA, false)) {
                this.mExpandAdvancedFields = true;
            }
            bindNetworkQueryService();
            addPreferencesFromResource(R.xml.network_setting_fragment);
            this.mButton4glte = (SwitchPreference) findPreference(BUTTON_4G_LTE_KEY);
            this.mButton4glte.setOnPreferenceChangeListener(this);
            this.mCallingCategory = (PreferenceCategory) findPreference(CATEGORY_CALLING_KEY);
            this.mWiFiCallingPref = findPreference(BUTTON_WIFI_CALLING_KEY);
            this.mVideoCallingPref = (SwitchPreference) findPreference(BUTTON_VIDEO_CALLING_KEY);
            this.mMobileDataPref = (MobileDataPreference) findPreference(BUTTON_MOBILE_DATA_ENABLE_KEY);
            this.mDataUsagePref = (DataUsagePreference) findPreference(BUTTON_DATA_USAGE_KEY);
            try {
                Context contextCreatePackageContext = activity.createPackageContext("com.android.systemui", 0);
                this.mShow4GForLTE = contextCreatePackageContext.getResources().getBoolean(contextCreatePackageContext.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui"));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "NameNotFoundException for show4GFotLTE");
                this.mShow4GForLTE = false;
            }
            log("mShow4GForLTE: " + this.mShow4GForLTE);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            this.mButtonDataRoam = (RestrictedSwitchPreference) preferenceScreen.findPreference(BUTTON_ROAMING_KEY);
            this.mButtonPreferredNetworkMode = (ListPreference) preferenceScreen.findPreference(BUTTON_PREFERED_NETWORK_MODE);
            this.mButtonEnabledNetworks = (ListPreference) preferenceScreen.findPreference(BUTTON_ENABLED_NETWORKS_KEY);
            this.mAdvancedOptions = (AdvancedOptionsPreference) preferenceScreen.findPreference(BUTTON_ADVANCED_OPTIONS_KEY);
            this.mButtonDataRoam.setOnPreferenceChangeListener(this);
            this.mLteDataServicePref = preferenceScreen.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);
            this.mEuiccSettingsPref = preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_EUICC_KEY);
            this.mEuiccSettingsPref.setOnPreferenceChangeListener(this);
            this.mActiveSubInfos = new ArrayList(this.mSubscriptionManager.getActiveSubscriptionInfoCountMax());
            initIntentFilter();
            try {
                activity.registerReceiver(this.mReceiver, this.mIntentFilter);
            } catch (Exception e2) {
                Log.e(LOG_TAG, "Receiver Already registred");
            }
            this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
            this.mTelephonyManager.listen(this.mPhoneStateListener, 33);
            if (bundle != null) {
                this.mCurrentTab = bundle.getInt(CURRENT_TAB);
            }
            activity.getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled"), true, this.mContentObserver);
            activity.getContentResolver().registerContentObserver(Settings.Global.getUriFor(ENHANCED_4G_MODE_ENABLED_SIM2), true, this.mContentObserver);
            activity.getContentResolver().registerContentObserver(Telephony.Carriers.ENFORCE_MANAGED_URI, false, this.mDpcEnforcedContentObserver);
            log("onCreate:-");
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(android.R.layout.breadcrumbs_in_fragment, viewGroup, false);
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            if (this.mUm.hasUserRestriction("no_config_mobile_networks") || !this.mUm.isSystemUser()) {
                this.mUnavailable = true;
                getActivity().setContentView(R.layout.telephony_disallowed_preference_screen);
            } else {
                initializeSubscriptions();
                updateCurrentTab(getActivity().getIntent());
            }
        }

        private class DpcApnEnforcedObserver extends ContentObserver {
            DpcApnEnforcedObserver() {
                super(null);
            }

            @Override
            public void onChange(boolean z) {
                Log.i(MobileNetworkFragment.LOG_TAG, "DPC enforced onChange:");
            }
        }

        @Override
        public void onDestroy() {
            unbindNetworkQueryService();
            super.onDestroy();
            Activity activity = getActivity();
            if (activity == null) {
                Log.d(LOG_TAG, "onDestroy, activity = null");
                return;
            }
            try {
                activity.unregisterReceiver(this.mReceiver);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Receiver Already unregistred");
            }
            getActivity().getContentResolver().unregisterContentObserver(this.mDpcEnforcedContentObserver);
            ExtensionManager.getMobileNetworkSettingsExt().unRegister();
            log("onDestroy ");
            if (this.mCdmaNetworkSettings != null) {
                this.mCdmaNetworkSettings.onDestroy();
                this.mCdmaNetworkSettings = null;
            }
            if (this.mSubscriptionManager != null) {
                this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
            }
            if (this.mTelephonyManager != null) {
                this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            }
            activity.getContentResolver().unregisterContentObserver(this.mContentObserver);
            if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId())) {
                activity.getContentResolver().unregisterContentObserver(this.mNetworkObserver);
            }
            if (this.mDialog != null && this.mDialog.isShowing()) {
                this.mDialog.dismiss();
            }
            if (this.mOmEx != null) {
                this.mOmEx.unRegister();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            log("onResume:+");
            if (this.mUnavailable) {
                Log.i(LOG_TAG, "onResume:- ignore mUnavailable == false");
                return;
            }
            if (this.mCdmaNetworkSettings != null) {
                this.mCdmaNetworkSettings.onResume();
            }
            this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
            if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null || getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                updatePreferredNetworkUIFromDb();
            }
            updateCallingCategory();
            updateScreenStatus();
            ExtensionManager.getMobileNetworkSettingsExt().onResume();
            log("onResume:-");
        }

        private boolean hasActiveSubscriptions() {
            return this.mActiveSubInfos.size() > 0;
        }

        private void updateBodyBasicFields(Activity activity, PreferenceScreen preferenceScreen, int i, boolean z) {
            Context applicationContext = activity.getApplicationContext();
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            preferenceScreen.addPreference(this.mMobileDataPref);
            preferenceScreen.addPreference(this.mButtonDataRoam);
            preferenceScreen.addPreference(this.mDataUsagePref);
            this.mMobileDataPref.initialize(i);
            this.mDataUsagePref.initialize(i);
            this.mMobileDataPref.setEnabled(z);
            ExtensionManager.getMobileNetworkSettingsExt().customizeDataEnable(i, this.mMobileDataPref);
            this.mButtonDataRoam.setEnabled(z);
            this.mDataUsagePref.setEnabled(z);
            this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
            this.mButtonDataRoam.setDisabledByAdmin(false);
            if (this.mButtonDataRoam.isEnabled()) {
                if (RestrictedLockUtils.hasBaseUserRestriction(applicationContext, "no_data_roaming", UserHandle.myUserId())) {
                    this.mButtonDataRoam.setEnabled(false);
                } else {
                    this.mButtonDataRoam.checkRestrictionAndSetDisabled("no_data_roaming");
                }
            }
            if (this.mPhone != null) {
                ExtensionManager.getMobileNetworkSettingsExt().customizeBasicMobileNetworkSettings(preferenceScreen, this.mPhone.getSubId());
            }
        }

        private void updateBody() {
            Activity activity = getActivity();
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            int subId = this.mPhone.getSubId();
            boolean zHasActiveSubscriptions = hasActiveSubscriptions();
            if (activity == null || activity.isDestroyed()) {
                Log.e(LOG_TAG, "updateBody with no valid activity.");
                return;
            }
            if (preferenceScreen == null) {
                Log.e(LOG_TAG, "updateBody with no null prefSet.");
                return;
            }
            preferenceScreen.removeAll();
            updateBodyBasicFields(activity, preferenceScreen, subId, zHasActiveSubscriptions);
            if (this.mExpandAdvancedFields) {
                updateBodyAdvancedFields(activity, preferenceScreen, subId, zHasActiveSubscriptions);
            } else {
                preferenceScreen.addPreference(this.mAdvancedOptions);
            }
        }

        private void updateBodyAdvancedFields(Activity activity, PreferenceScreen preferenceScreen, int i, boolean z) {
            int i2;
            boolean z2 = this.mPhone.getLteOnCdmaMode() == 1;
            if (DBG) {
                log("updateBody: isLteOnCdma=" + z2 + " phoneSubId=" + i);
            }
            preferenceScreen.addPreference(this.mButtonPreferredNetworkMode);
            preferenceScreen.addPreference(this.mButtonEnabledNetworks);
            preferenceScreen.addPreference(this.mButton4glte);
            if (MobileNetworkSettings.showEuiccSettings(getActivity())) {
                preferenceScreen.addPreference(this.mEuiccSettingsPref);
                String simOperatorName = this.mTelephonyManager.getSimOperatorName();
                if (TextUtils.isEmpty(simOperatorName)) {
                    this.mEuiccSettingsPref.setSummary((CharSequence) null);
                } else {
                    this.mEuiccSettingsPref.setSummary(simOperatorName);
                }
            }
            int i3 = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + i, preferredNetworkMode);
            PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
            this.mIsGlobalCdma = z2 && carrierConfigForSubId.getBoolean("show_cdma_choices_bool");
            if (carrierConfigForSubId.getBoolean("hide_carrier_network_settings_bool")) {
                preferenceScreen.removePreference(this.mButtonPreferredNetworkMode);
                preferenceScreen.removePreference(this.mButtonEnabledNetworks);
                preferenceScreen.removePreference(this.mLteDataServicePref);
            } else if (carrierConfigForSubId.getBoolean("hide_preferred_network_type_bool") && !this.mPhone.getServiceState().getRoaming() && this.mPhone.getServiceState().getDataRegState() == 0) {
                preferenceScreen.removePreference(this.mButtonPreferredNetworkMode);
                preferenceScreen.removePreference(this.mButtonEnabledNetworks);
                int phoneType = this.mPhone.getPhoneType();
                if (phoneType == 2) {
                    updateCdmaOptions(this, preferenceScreen, this.mPhone);
                } else if (phoneType == 1) {
                    updateGsmUmtsOptions(this, preferenceScreen, i, this.mNetworkQueryService);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
                i3 = preferredNetworkMode;
            } else if (carrierConfigForSubId.getBoolean("world_phone_bool")) {
                preferenceScreen.removePreference(this.mButtonEnabledNetworks);
                this.mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                updateCdmaOptions(this, preferenceScreen, this.mPhone);
                updateGsmUmtsOptions(this, preferenceScreen, i, this.mNetworkQueryService);
            } else {
                preferenceScreen.removePreference(this.mButtonPreferredNetworkMode);
                int phoneType2 = this.mPhone.getPhoneType();
                int mainCapabilityPhoneId = -1;
                IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
                if (iMtkTelephonyExAsInterface != null) {
                    try {
                        mainCapabilityPhoneId = iMtkTelephonyExAsInterface.getMainCapabilityPhoneId();
                    } catch (RemoteException e) {
                        log("getMainCapabilityPhoneId: remote exception");
                    }
                } else {
                    log("IMtkTelephonyEx service not ready!");
                    mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                }
                if (TelephonyUtilsEx.isCDMAPhone(this.mPhone) || (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId()) && !TelephonyUtilsEx.isRoaming(this.mPhone) && (!TelephonyUtilsEx.isBothslotCtSim(this.mSubscriptionManager) || mainCapabilityPhoneId == this.mPhone.getPhoneId()))) {
                    int i4 = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "lte_service_forced" + this.mPhone.getSubId(), 0);
                    log("phoneType == PhoneConstants.PHONE_TYPE_CDMA, lteForced = " + i4);
                    if (z2) {
                        if (i4 == 0) {
                            this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                            this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                        } else {
                            switch (i3) {
                                case 4:
                                case 5:
                                case 6:
                                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_no_lte_choices);
                                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_no_lte_values);
                                    break;
                                case 7:
                                case 8:
                                case 10:
                                case 11:
                                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_only_lte_choices);
                                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_only_lte_values);
                                    break;
                                case 9:
                                default:
                                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                                    break;
                            }
                        }
                    }
                    updateCdmaOptions(this, preferenceScreen, this.mPhone);
                } else if (phoneType2 == 1) {
                    if (isSupportTdscdma()) {
                        this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_tdscdma_choices);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_tdscdma_values);
                    } else if (!carrierConfigForSubId.getBoolean("prefer_2g_bool") && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                        this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_gsm_lte_choices);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_lte_values);
                    } else if (!carrierConfigForSubId.getBoolean("prefer_2g_bool")) {
                        if (this.mShow4GForLTE) {
                            i2 = R.array.enabled_networks_except_gsm_4g_choices;
                        } else {
                            i2 = R.array.enabled_networks_except_gsm_choices;
                        }
                        this.mButtonEnabledNetworks.setEntries(i2);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_values);
                        log("!KEY_PREFER_2G_BOOL");
                    } else if (!PhoneFeatureConstants.FeatureOption.isMtkLteSupport()) {
                        this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values);
                    } else if (this.mIsGlobalCdma) {
                        this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                    } else {
                        this.mButtonEnabledNetworks.setEntries(this.mShow4GForLTE ? R.array.enabled_networks_4g_choices : R.array.enabled_networks_choices);
                        ExtensionManager.getMobileNetworkSettingsExt().changeEntries(this.mButtonEnabledNetworks);
                        if (isC2kLteSupport()) {
                            if (DBG) {
                                log("Change to C2K values");
                            }
                            this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values_c2k);
                        } else {
                            log("!isC2kLteSupport");
                            this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values);
                        }
                    }
                    updateGsmUmtsOptions(this, preferenceScreen, i, this.mNetworkQueryService);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType2);
                }
                if (isWorldMode()) {
                    this.mButtonEnabledNetworks.setEntries(R.array.preferred_network_mode_choices_world_mode);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.preferred_network_mode_values_world_mode);
                }
                this.mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
                if (DBG) {
                    log("settingsNetworkMode: " + i3);
                }
            }
            boolean zIsEmpty = TextUtils.isEmpty(Settings.Global.getString(activity.getContentResolver(), "setup_prepaid_data_service_url"));
            if (!z2 || zIsEmpty) {
                preferenceScreen.removePreference(this.mLteDataServicePref);
            } else {
                Log.d(LOG_TAG, "keep ltePref");
            }
            onCreateMTK(preferenceScreen);
            updateCallingCategory();
            if (carrierConfigForSubId.getBoolean("mtk_key_roaming_bar_guard_bool")) {
                int order = this.mButtonDataRoam.getOrder();
                preferenceScreen.removePreference(this.mButtonDataRoam);
                Preference preference = new Preference(activity);
                preference.setKey(BUTTON_SPRINT_ROAMING_SETTINGS);
                preference.setTitle(R.string.roaming_settings);
                Intent intent = new Intent();
                intent.setClassName("com.android.phone", "com.mediatek.services.telephony.RoamingSettings");
                intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mPhone.getSubId());
                preference.setIntent(intent);
                preference.setOrder(order);
                preferenceScreen.addPreference(preference);
            }
            boolean z3 = activity.getResources().getBoolean(android.R.^attr-private.controllerType);
            if (!this.mUm.isAdminUser() || !z3 || this.mUm.hasUserRestriction("no_config_cell_broadcasts")) {
                PreferenceScreen preferenceScreen2 = getPreferenceScreen();
                Preference preferenceFindPreference = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
                if (preferenceFindPreference != null) {
                    preferenceScreen2.removePreference(preferenceFindPreference);
                }
            }
            if (preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY) != null) {
                preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY).setOnPreferenceChangeListener(this);
            }
            if (preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY) != null) {
                preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY).setOnPreferenceChangeListener(this);
            }
            this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
            this.mButtonEnabledNetworks.setValue(Integer.toString(i3));
            this.mButtonPreferredNetworkMode.setValue(Integer.toString(i3));
            UpdatePreferredNetworkModeSummary(i3);
            UpdateEnabledNetworksValueAndSummary(i3);
            Preference preferenceFindPreference2 = findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
            if (preferenceFindPreference2 != null) {
                preferenceFindPreference2.setEnabled(z);
            }
            Preference preferenceFindPreference3 = findPreference(CATEGORY_CALLING_KEY);
            if (preferenceFindPreference3 != null) {
                preferenceFindPreference3.setEnabled(z);
            }
            this.mOmEx.updateNetworkTypeSummary(this.mButtonEnabledNetworks);
            if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkModeSettingNeeded()) {
                updateNetworkModeForLwDsds();
            }
            if (this.mButtonEnabledNetworks != null) {
                log("Enter plug-in update updateNetworkTypeSummary - Enabled again!");
                ExtensionManager.getMobileNetworkSettingsExt().updateNetworkTypeSummary(this.mButtonEnabledNetworks);
            }
        }

        @Override
        public void onPause() {
            ExtensionManager.getMobileNetworkSettingsExt().onPause();
            super.onPause();
            if (DBG) {
                log("onPause:+");
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object obj) {
            sendMetricsEventPreferenceChanged(getPreferenceScreen(), preference, obj);
            int subId = this.mPhone.getSubId();
            if (onPreferenceChangeMTK(preference, obj)) {
                return true;
            }
            if (preference == this.mButtonPreferredNetworkMode) {
                String str = (String) obj;
                this.mButtonPreferredNetworkMode.setValue(str);
                int i = Integer.parseInt(str);
                int i2 = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, preferredNetworkMode);
                log("onPreferenceChange buttonNetworkMode:" + i + " settingsNetworkMode:" + i2);
                if (i != i2) {
                    switch (i) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                        case 20:
                        case 21:
                        case 22:
                            this.mButtonPreferredNetworkMode.setValue(Integer.toString(i));
                            this.mButtonPreferredNetworkMode.setSummary(this.mButtonPreferredNetworkMode.getEntry());
                            this.mPreNetworkMode = i2;
                            Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, i);
                            if (DBG) {
                                log("setPreferredNetworkType, networkType: " + i);
                            }
                            this.mPhone.setPreferredNetworkType(i, this.mHandler.obtainMessage(0));
                            break;
                        default:
                            loge("Invalid Network Mode (" + i + ") chosen. Ignore.");
                            return true;
                    }
                }
            } else if (preference == this.mButtonEnabledNetworks) {
                String str2 = (String) obj;
                this.mButtonEnabledNetworks.setValue(str2);
                int i3 = Integer.parseInt(str2);
                if (DBG) {
                    log("onPreferenceChange buttonNetworkMode: " + i3);
                }
                int i4 = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, preferredNetworkMode);
                if (DBG) {
                    log("buttonNetworkMode: " + i3 + "settingsNetworkMode: " + i4);
                }
                if (i3 != i4 || ExtensionManager.getMobileNetworkSettingsExt().isNetworkChanged(this.mButtonEnabledNetworks, i3, i4, this.mPhone)) {
                    switch (i3) {
                        case 0:
                        case 1:
                        case 2:
                        case 4:
                        case 5:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                        case 20:
                        case 21:
                        case 22:
                            UpdateEnabledNetworksValueAndSummary(i3);
                            this.mPreNetworkMode = i4;
                            if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkUpdateNeeded(this.mButtonEnabledNetworks, i3, i4, this.mPhone, this.mPhone.getContext().getContentResolver(), subId, this.mHandler)) {
                                UpdateEnabledNetworksValueAndSummary(i3);
                                Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, i3);
                                log("setPreferredNetworkType, networkType: " + i3);
                                this.mPhone.setPreferredNetworkType(i3, this.mHandler.obtainMessage(0));
                            }
                            break;
                        case 3:
                        case 6:
                        default:
                            loge("Invalid Network Mode (" + i3 + ") chosen. Ignore.");
                            return true;
                    }
                }
            } else if (preference == this.mButton4glte) {
                SwitchPreference switchPreference = (SwitchPreference) preference;
                switchPreference.setChecked(!switchPreference.isChecked());
                MtkImsManager.setEnhanced4gLteModeSetting(getActivity(), switchPreference.isChecked(), this.mPhone.getPhoneId());
            } else if (preference == this.mButtonDataRoam) {
                if (DBG) {
                    log("onPreferenceTreeClick: preference == mButtonDataRoam.");
                }
                if (!this.mButtonDataRoam.isChecked()) {
                    PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
                    if (carrierConfigForSubId != null && carrierConfigForSubId.getBoolean("disable_charge_indication_bool")) {
                        this.mPhone.setDataRoamingEnabled(true);
                        MetricsLogger.action(getContext(), getMetricsEventCategory(getPreferenceScreen(), this.mButtonDataRoam), true);
                    } else {
                        MetricsLogger.action(getContext(), getMetricsEventCategory(getPreferenceScreen(), this.mButtonDataRoam));
                        this.mOkClicked = false;
                        RoamingDialogFragment roamingDialogFragment = new RoamingDialogFragment();
                        roamingDialogFragment.setPhone(this.mPhone);
                        roamingDialogFragment.show(getFragmentManager(), ROAMING_TAG);
                        return false;
                    }
                } else {
                    this.mPhone.setDataRoamingEnabled(false);
                    MetricsLogger.action(getContext(), getMetricsEventCategory(getPreferenceScreen(), this.mButtonDataRoam), false);
                    return true;
                }
            } else {
                if (preference == this.mVideoCallingPref) {
                    if (this.mEnhancedButton4glte != null && this.mEnhancedButton4glte.isChecked()) {
                        this.mImsMgr.setVtSetting(((Boolean) obj).booleanValue());
                        return true;
                    }
                    loge("mVideoCallingPref should be disabled if mButton4glte is not checked.");
                    this.mVideoCallingPref.setEnabled(false);
                    return false;
                }
                if (preference == getPreferenceScreen().findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY) || preference == getPreferenceScreen().findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
                    return true;
                }
            }
            ExtensionManager.getMobileNetworkSettingsExt().onPreferenceChange(preference, obj);
            return true;
        }

        private boolean is4gLtePrefEnabled(PersistableBundle persistableBundle) {
            return this.mTelephonyManager.getCallState(this.mPhone.getSubId()) == 0 && this.mImsMgr != null && this.mImsMgr.isNonTtyOrTtyOnVolteEnabled() && persistableBundle.getBoolean("editable_enhanced_4g_lte_bool");
        }

        private class MyHandler extends Handler {
            static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

            private MyHandler() {
            }

            MyHandler(MobileNetworkFragment mobileNetworkFragment, AnonymousClass1 anonymousClass1) {
                this();
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    handleSetPreferredNetworkTypeResponse(message);
                }
            }

            private void handleSetPreferredNetworkTypeResponse(Message message) {
                MobileNetworkFragment.this.restorePreferredNetworkTypeIfNeeded(message);
                Activity activity = MobileNetworkFragment.this.getActivity();
                if (activity == null || activity.isDestroyed()) {
                    return;
                }
                AsyncResult asyncResult = (AsyncResult) message.obj;
                int subId = MobileNetworkFragment.this.mPhone.getSubId();
                if (asyncResult.exception == null) {
                    if (MobileNetworkFragment.this.getPreferenceScreen().findPreference(MobileNetworkFragment.BUTTON_PREFERED_NETWORK_MODE) != null) {
                        int i = Integer.parseInt(MobileNetworkFragment.this.mButtonPreferredNetworkMode.getValue());
                        if (MobileNetworkFragment.DBG) {
                            MobileNetworkFragment.log("handleSetPreferredNetwrokTypeResponse1: networkMode:" + i);
                        }
                        Settings.Global.putInt(MobileNetworkFragment.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, i);
                    }
                    if (MobileNetworkFragment.this.getPreferenceScreen().findPreference(MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY) != null) {
                        int i2 = Integer.parseInt(MobileNetworkFragment.this.mButtonEnabledNetworks.getValue());
                        if (MobileNetworkFragment.DBG) {
                            MobileNetworkFragment.log("handleSetPreferredNetwrokTypeResponse2: networkMode:" + i2);
                        }
                        Settings.Global.putInt(MobileNetworkFragment.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, i2);
                    }
                    MobileNetworkFragment.log("Start Network updated intent");
                    activity.sendBroadcast(new Intent("com.mediatek.intent.action.ACTION_NETWORK_CHANGED"));
                    return;
                }
                Log.i(MobileNetworkFragment.LOG_TAG, "handleSetPreferredNetworkTypeResponse:exception in setting network mode.");
                MobileNetworkFragment.this.updatePreferredNetworkUIFromDb();
            }
        }

        private void updatePreferredNetworkUIFromDb() {
            int subId = this.mPhone.getSubId();
            int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, preferredNetworkMode);
            if (DBG) {
                log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " + i);
            }
            UpdatePreferredNetworkModeSummary(i);
            UpdateEnabledNetworksValueAndSummary(i);
            this.mButtonPreferredNetworkMode.setValue(Integer.toString(i));
        }

        private void UpdatePreferredNetworkModeSummary(int i) {
            if (!isCapabilityPhone(this.mPhone)) {
                log("init PreferredNetworkMode with gsm only");
                i = 1;
            }
            switch (i) {
                case 0:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_perf_summary);
                    break;
                case 1:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_only_summary);
                    break;
                case 2:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_only_summary);
                    break;
                case 3:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_wcdma_summary);
                    break;
                case 4:
                    if (this.mPhone.getLteOnCdmaMode() == 1) {
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_summary);
                    } else {
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_summary);
                    }
                    break;
                case 5:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_only_summary);
                    break;
                case 6:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_evdo_only_summary);
                    break;
                case 7:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                    break;
                case 8:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_cdma_evdo_summary);
                    break;
                case 9:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                    break;
                case 10:
                    if (this.mPhone.getPhoneType() == 2 || this.mIsGlobalCdma || isWorldMode()) {
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                    } else {
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                    }
                    break;
                case 11:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                    break;
                case 12:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_wcdma_summary);
                    break;
                case 13:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_summary);
                    break;
                case 14:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_wcdma_summary);
                    break;
                case 15:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_summary);
                    break;
                case 16:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_gsm_summary);
                    break;
                case 17:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_gsm_summary);
                    break;
                case 18:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_gsm_wcdma_summary);
                    break;
                case 19:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_wcdma_summary);
                    break;
                case 20:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_gsm_wcdma_summary);
                    break;
                case 21:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_tdscdma_cdma_evdo_gsm_wcdma_summary);
                    break;
                case 22:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_tdscdma_cdma_evdo_gsm_wcdma_summary);
                    break;
                default:
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                    break;
            }
            ExtensionManager.getMobileNetworkSettingsExt().updateNetworkTypeSummary(this.mButtonPreferredNetworkMode);
            this.mOmEx.updateNetworkTypeSummary(this.mButtonPreferredNetworkMode);
        }

        private void UpdateEnabledNetworksValueAndSummary(int i) {
            if (DBG) {
                Log.d(LOG_TAG, "NetworkMode: " + i);
            }
            if (!isCapabilityPhone(this.mPhone)) {
                log("init EnabledNetworks with gsm only");
                i = 1;
            }
            int i2 = R.string.network_4G;
            switch (i) {
                case 0:
                case 2:
                case 3:
                    if (!this.mIsGlobalCdma) {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(0));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    } else {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case 1:
                    if (!this.mIsGlobalCdma) {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(1));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    } else {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case 4:
                case 6:
                case 7:
                    if (isC2kLteSupport()) {
                        log("Update value to Global for c2k project");
                        this.mButtonEnabledNetworks.setValue(Integer.toString(7));
                    } else {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(4));
                    }
                    this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case 5:
                    this.mButtonEnabledNetworks.setValue(Integer.toString(5));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_1x);
                    break;
                case 8:
                    if (isWorldMode()) {
                        this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_cdma_summary);
                        controlCdmaOptions(true);
                        controlGsmOptions(false);
                    } else {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(8));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    }
                    break;
                case 9:
                    if (isWorldMode()) {
                        this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_gsm_umts_summary);
                        controlCdmaOptions(false);
                        controlGsmOptions(true);
                    } else if (!this.mIsGlobalCdma) {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(9));
                        ListPreference listPreference = this.mButtonEnabledNetworks;
                        if (!this.mShow4GForLTE) {
                            i2 = R.string.network_lte;
                        }
                        listPreference.setSummary(i2);
                    } else {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case 10:
                case 15:
                case 17:
                case 19:
                case 20:
                case 22:
                    if (isSupportTdscdma()) {
                        this.mButtonEnabledNetworks.setValue(Integer.toString(22));
                        this.mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    } else {
                        if (isWorldMode()) {
                            controlCdmaOptions(true);
                            controlGsmOptions(false);
                        }
                        this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                        if (this.mPhone.getPhoneType() == 2 || this.mIsGlobalCdma || isWorldMode()) {
                            this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                        } else {
                            ListPreference listPreference2 = this.mButtonEnabledNetworks;
                            if (!this.mShow4GForLTE) {
                                i2 = R.string.network_lte;
                            }
                            listPreference2.setSummary(i2);
                        }
                    }
                    break;
                case 11:
                case 12:
                    break;
                case 13:
                    this.mButtonEnabledNetworks.setValue(Integer.toString(13));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case 14:
                case 16:
                case 18:
                    this.mButtonEnabledNetworks.setValue(Integer.toString(18));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case 21:
                    this.mButtonEnabledNetworks.setValue(Integer.toString(21));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                default:
                    String str = "Invalid Network Mode (" + i + "). Ignore.";
                    loge(str);
                    this.mButtonEnabledNetworks.setSummary(str);
                    break;
            }
            ExtensionManager.getMobileNetworkSettingsExt().updatePreferredNetworkValueAndSummary(this.mButtonEnabledNetworks, i);
            if (this.mButtonEnabledNetworks != null) {
                log("Enter plug-in update updateNetworkTypeSummary - Enabled.");
                ExtensionManager.getMobileNetworkSettingsExt().updateNetworkTypeSummary(this.mButtonEnabledNetworks);
                this.mOmEx.updateNetworkTypeSummary(this.mButtonEnabledNetworks);
            }
        }

        @Override
        public void onActivityResult(int i, int i2, Intent intent) {
            if (i == 17 && Boolean.valueOf(intent.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false)).booleanValue() && this.mClickedPreference != null) {
                this.mCdmaOptions.showDialog(this.mClickedPreference);
            }
        }

        private void updateWiFiCallState() {
            boolean z;
            Context context = getContext();
            if (this.mWiFiCallingPref == null || this.mCallingCategory == null || context == null || !this.mExpandAdvancedFields) {
                return;
            }
            PhoneAccountHandle simCallManager = TelecomManager.from(context).getSimCallManager();
            boolean z2 = true;
            if (simCallManager != null) {
                Intent intentBuildPhoneAccountConfigureIntent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(context, simCallManager);
                if (intentBuildPhoneAccountConfigureIntent != null && this.mPhone != null) {
                    PackageManager packageManager = this.mPhone.getContext().getPackageManager();
                    List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intentBuildPhoneAccountConfigureIntent, 0);
                    if (!listQueryIntentActivities.isEmpty()) {
                        this.mWiFiCallingPref.setTitle(listQueryIntentActivities.get(0).loadLabel(packageManager));
                        this.mWiFiCallingPref.setSummary((CharSequence) null);
                        this.mWiFiCallingPref.setIntent(intentBuildPhoneAccountConfigureIntent);
                        z = false;
                    } else {
                        z = true;
                    }
                } else {
                    z = true;
                }
            } else if (this.mImsMgr != null && this.mImsMgr.isWfcEnabledByPlatform() && this.mImsMgr.isWfcProvisionedOnDevice() && ExtensionManager.getMobileNetworkSettingsExt().isWfcProvisioned(context, this.mPhone.getPhoneId())) {
                int i = android.R.string.notification_channel_network_alerts;
                if (this.mImsMgr.isWfcEnabledByUser()) {
                    int wfcMode = this.mImsMgr.getWfcMode(this.mTelephonyManager.isNetworkRoaming());
                    switch (wfcMode) {
                        case 0:
                            i = android.R.string.news_notification_channel_label;
                            break;
                        case 1:
                            i = android.R.string.new_sms_notification_title;
                            break;
                        case 2:
                            i = android.R.string.next_button_label;
                            break;
                        default:
                            if (DBG) {
                                log("Unexpected WFC mode value: " + wfcMode);
                            }
                            break;
                    }
                }
                this.mWiFiCallingPref.setSummary(ExtensionManager.getMobileNetworkSettingsExt().customizeWfcSummary(context, i, this.mPhone.getPhoneId()));
                z = false;
            } else {
                z = true;
            }
            if (z) {
                this.mCallingCategory.removePreference(this.mWiFiCallingPref);
                return;
            }
            this.mCallingCategory.addPreference(this.mWiFiCallingPref);
            Preference preference = this.mWiFiCallingPref;
            if (this.mTelephonyManager.getCallState(this.mPhone.getSubId()) != 0 || !hasActiveSubscriptions()) {
                z2 = false;
            }
            preference.setEnabled(z2);
            ExtensionManager.getMobileNetworkSettingsExt().customizeWfcPreference(context, getPreferenceScreen(), this.mCallingCategory, this.mPhone.getPhoneId());
        }

        private void updateEnhanced4gLteState() {
            if (this.mButton4glte == null) {
                return;
            }
            PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
            try {
                if (this.mImsMgr != null && this.mImsMgr.getImsServiceState() == 2 && this.mImsMgr.isVolteEnabledByPlatform() && this.mImsMgr.isVolteProvisionedOnDevice() && !carrierConfigForSubId.getBoolean("hide_enhanced_4g_lte_bool")) {
                    boolean z = false;
                    this.mButton4glte.setEnabled(is4gLtePrefEnabled(carrierConfigForSubId) && hasActiveSubscriptions());
                    if (this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUser() && this.mImsMgr.isNonTtyOrTtyOnVolteEnabled()) {
                        z = true;
                    }
                    this.mButton4glte.setChecked(z);
                    return;
                }
                getPreferenceScreen().removePreference(this.mButton4glte);
            } catch (ImsException e) {
                log("Exception when trying to get ImsServiceStatus: " + e);
                getPreferenceScreen().removePreference(this.mButton4glte);
            }
        }

        private void updateVideoCallState() {
            if (this.mVideoCallingPref == null || this.mCallingCategory == null || !this.mExpandAdvancedFields) {
                return;
            }
            log("updateVideoCallState");
            PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
            if (this.mImsMgr != null && this.mImsMgr.isVtEnabledByPlatform() && this.mImsMgr.isVtProvisionedOnDevice() && (carrierConfigForSubId.getBoolean("ignore_data_enabled_changed_for_video_calls") || this.mPhone.mDcTracker.isDataEnabled())) {
                this.mCallingCategory.addPreference(this.mVideoCallingPref);
                boolean z = false;
                if (this.mEnhancedButton4glte == null || !this.mEnhancedButton4glte.isChecked()) {
                    log("state false");
                    this.mVideoCallingPref.setEnabled(false);
                    this.mVideoCallingPref.setChecked(false);
                    return;
                }
                SwitchPreference switchPreference = this.mVideoCallingPref;
                if (this.mTelephonyManager.getCallState(this.mPhone.getSubId()) == 0 && hasActiveSubscriptions()) {
                    z = true;
                }
                switchPreference.setEnabled(z);
                log("state true");
                this.mVideoCallingPref.setChecked(this.mImsMgr.isVtEnabledByUser());
                this.mVideoCallingPref.setOnPreferenceChangeListener(this);
                return;
            }
            this.mCallingCategory.removePreference(this.mVideoCallingPref);
        }

        private void updateCallingCategory() {
            if (this.mCallingCategory == null || !this.mExpandAdvancedFields) {
                return;
            }
            updateWiFiCallState();
            updateVideoCallState();
            if (this.mCallingCategory.getPreferenceCount() == 0) {
                getPreferenceScreen().removePreference(this.mCallingCategory);
            } else {
                getPreferenceScreen().addPreference(this.mCallingCategory);
            }
        }

        private static void log(String str) {
            if (DBG) {
                Log.d(LOG_TAG, str);
            }
        }

        private static void loge(String str) {
            Log.e(LOG_TAG, str);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem) {
            if (menuItem.getItemId() == 16908332) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(menuItem);
        }

        private boolean isWorldMode() {
            String[] strArrSplit;
            String string = getResources().getString(R.string.config_world_mode);
            boolean z = false;
            if (!TextUtils.isEmpty(string) && (strArrSplit = string.split(";")) != null && ((strArrSplit.length == 1 && strArrSplit[0].equalsIgnoreCase("true")) || (strArrSplit.length == 2 && !TextUtils.isEmpty(strArrSplit[1]) && this.mTelephonyManager != null && strArrSplit[1].equalsIgnoreCase(this.mTelephonyManager.getGroupIdLevel1())))) {
                z = true;
            }
            if (DBG) {
                Log.d(LOG_TAG, "isWorldMode=" + z);
            }
            return z;
        }

        private void controlGsmOptions(boolean z) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen == null) {
                return;
            }
            updateGsmUmtsOptions(this, preferenceScreen, this.mPhone.getSubId(), this.mNetworkQueryService);
            PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceScreen.findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
            Preference preferenceFindPreference = preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (preferenceCategory != null) {
                if (z) {
                    preferenceCategory.setEnabled(true);
                } else {
                    preferenceScreen.removePreference(preferenceCategory);
                }
            }
            if (preferenceFindPreference != null) {
                preferenceScreen.removePreference(preferenceFindPreference);
            }
        }

        private void controlCdmaOptions(boolean z) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen == null) {
                return;
            }
            updateCdmaOptions(this, preferenceScreen, this.mPhone);
            CdmaSystemSelectListPreference cdmaSystemSelectListPreference = (CdmaSystemSelectListPreference) preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
            if (cdmaSystemSelectListPreference != null) {
                cdmaSystemSelectListPreference.setEnabled(z);
            }
        }

        private boolean isSupportTdscdma() {
            return false;
        }

        private void sendMetricsEventPreferenceClicked(PreferenceScreen preferenceScreen, Preference preference) {
            int metricsEventCategory = getMetricsEventCategory(preferenceScreen, preference);
            if (metricsEventCategory == 0) {
                return;
            }
            if (preference == this.mLteDataServicePref || preference == this.mDataUsagePref || preference == this.mEuiccSettingsPref || preference == this.mAdvancedOptions || preference == this.mWiFiCallingPref || preference == this.mButtonPreferredNetworkMode || preference == this.mButtonEnabledNetworks || preference == preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY) || preference == preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY) || preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY) || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY) || preference == preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
                MetricsLogger.action(getContext(), metricsEventCategory);
            }
        }

        private void sendMetricsEventPreferenceChanged(PreferenceScreen preferenceScreen, Preference preference, Object obj) {
            int metricsEventCategory = getMetricsEventCategory(preferenceScreen, preference);
            if (metricsEventCategory == 0) {
                return;
            }
            if (preference == this.mButton4glte || preference == this.mVideoCallingPref) {
                MetricsLogger.action(getContext(), metricsEventCategory, ((Boolean) obj).booleanValue());
            } else if (preference == this.mButtonPreferredNetworkMode || preference == this.mButtonEnabledNetworks || preference == preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY) || preference == preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
                MetricsLogger.action(getContext(), metricsEventCategory, Integer.valueOf((String) obj).intValue());
            }
        }

        private int getMetricsEventCategory(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == null) {
                return 0;
            }
            if (preference == this.mMobileDataPref) {
                return 1081;
            }
            if (preference == this.mButtonDataRoam) {
                return 1201;
            }
            if (preference == this.mDataUsagePref) {
                return 1082;
            }
            if (preference == this.mLteDataServicePref) {
                return 1216;
            }
            if (preference == this.mAdvancedOptions) {
                return 1202;
            }
            if (preference == this.mButton4glte) {
                return 1203;
            }
            if (preference == this.mButtonPreferredNetworkMode) {
                return 1204;
            }
            if (preference == this.mButtonEnabledNetworks) {
                return 1205;
            }
            if (preference == this.mEuiccSettingsPref) {
                return 1206;
            }
            if (preference == this.mWiFiCallingPref) {
                return 1207;
            }
            if (preference == this.mVideoCallingPref) {
                return 1208;
            }
            if (preference == preferenceScreen.findPreference(NetworkOperators.BUTTON_AUTO_SELECT_KEY)) {
                return 1209;
            }
            if (preference == preferenceScreen.findPreference(NetworkOperators.BUTTON_NETWORK_SELECT_KEY)) {
                return 1210;
            }
            if (preference == preferenceScreen.findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY)) {
                return 1214;
            }
            if (preference == preferenceScreen.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY)) {
                return 1215;
            }
            if (preference == preferenceScreen.findPreference(BUTTON_GSM_APN_EXPAND_KEY) || preference == preferenceScreen.findPreference(BUTTON_CDMA_APN_EXPAND_KEY)) {
                return 1212;
            }
            if (preference != preferenceScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY)) {
                return 0;
            }
            return 1213;
        }

        private void updateGsmUmtsOptions(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen, int i, INetworkQueryService iNetworkQueryService) {
            if (this.mGsmUmtsOptions == null) {
                this.mGsmUmtsOptions = new GsmUmtsOptions(preferenceFragment, preferenceScreen, i, iNetworkQueryService);
            } else {
                this.mGsmUmtsOptions.update(i, iNetworkQueryService);
            }
        }

        private void updateCdmaOptions(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen, Phone phone) {
            if (this.mCdmaOptions == null) {
                this.mCdmaOptions = new CdmaOptions(preferenceFragment, preferenceScreen, phone);
            } else {
                this.mCdmaOptions.update(phone);
            }
        }

        private void dissmissDialog(ListPreference listPreference) {
            Dialog dialog;
            if (listPreference != null && (dialog = listPreference.getDialog()) != null) {
                dialog.dismiss();
            }
        }

        private void onCreateMTK(PreferenceScreen preferenceScreen) {
            Activity activity = getActivity();
            addEnhanced4GLteSwitchPreference(preferenceScreen);
            if (PhoneFeatureConstants.FeatureOption.isMtkCtaSet() && !TelephonyUtilsEx.isCDMAPhone(this.mPhone) && (!TelephonyUtilsEx.isCtVolteEnabled() || !TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId()))) {
                if (DBG) {
                    log("---addPLMNList---");
                }
                addPLMNList(preferenceScreen);
            }
            if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId())) {
                if (this.mNetworkRegister) {
                    activity.getContentResolver().unregisterContentObserver(this.mNetworkObserver);
                }
                activity.getContentResolver().registerContentObserver(Settings.Global.getUriFor("preferred_network_mode" + this.mPhone.getSubId()), true, this.mNetworkObserver);
                this.mNetworkRegister = true;
            }
            int mainCapabilityPhoneId = -1;
            IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            if (iMtkTelephonyExAsInterface != null) {
                try {
                    mainCapabilityPhoneId = iMtkTelephonyExAsInterface.getMainCapabilityPhoneId();
                } catch (RemoteException e) {
                    log("getMainCapabilityPhoneId: remote exception");
                }
            } else {
                log("IMtkTelephonyEx service not ready!");
                mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            }
            if (PhoneFeatureConstants.FeatureOption.isMtkLteSupport() && isC2kLteSupport() && ((TelephonyUtilsEx.isCdmaCardInserted(this.mPhone) || TelephonyUtils.isCTLteTddTestSupport() || (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId()) && (!TelephonyUtilsEx.isBothslotCt4gSim(this.mSubscriptionManager) || mainCapabilityPhoneId == this.mPhone.getPhoneId()))) && !ExtensionManager.getMobileNetworkSettingsExt().isCtPlugin())) {
                if (this.mCdmaNetworkSettings != null) {
                    log("CdmaNetworkSettings destroy " + this);
                    this.mCdmaNetworkSettings.onDestroy();
                    this.mCdmaNetworkSettings = null;
                }
                this.mCdmaNetworkSettings = new CdmaNetworkSettings(activity, preferenceScreen, this.mPhone);
                this.mCdmaNetworkSettings.onResume();
            }
            if (this.mPhone != null) {
                ExtensionManager.getMobileNetworkSettingsExt().initOtherMobileNetworkSettings(getPreferenceScreen(), this.mPhone.getSubId());
            }
            if (this.mActiveSubInfos.size() > 0) {
                this.mOmEx.initMobileNetworkSettings(getPreferenceScreen(), convertTabToSlot(this.mCurrentTab));
            }
            updateScreenStatus();
            handleC2k3MScreen(preferenceScreen);
            handleC2k4MScreen(preferenceScreen);
            handleC2k5MScreen(preferenceScreen);
        }

        private int convertTabToSlot(int i) {
            int simSlotIndex = this.mActiveSubInfos.size() > i ? this.mActiveSubInfos.get(i).getSimSlotIndex() : 0;
            if (DBG) {
                log("convertTabToSlot: info size=" + this.mActiveSubInfos.size() + " currentTab=" + i + " slotId=" + simSlotIndex);
            }
            return simSlotIndex;
        }

        private void initIntentFilter() {
            this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
            this.mIntentFilter.addAction("com.mediatek.intent.action.MSIM_MODE");
            this.mIntentFilter.addAction("mediatek.intent.action.ACTION_MD_TYPE_CHANGE");
            this.mIntentFilter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
            this.mIntentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
            this.mIntentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            ExtensionManager.getMobileNetworkSettingsExt().customizeDualVolteIntentFilter(this.mIntentFilter);
        }

        private boolean isCapabilityPhone(Phone phone) {
            return phone != null && (phone.getRadioAccessFamily() & 16392) > 0;
        }

        private void addEnhanced4GLteSwitchPreference(PreferenceScreen preferenceScreen) {
            int i;
            Activity activity = getActivity();
            SubscriptionManager.getPhoneId(this.mPhone.getSubId());
            boolean zIsVolteEnabled = isVolteEnabled();
            log("[addEnhanced4GLteSwitchPreference] volteEnabled :" + zIsVolteEnabled);
            if (this.mButton4glte != null) {
                log("[addEnhanced4GLteSwitchPreference] Remove mButton4glte!");
                preferenceScreen.removePreference(this.mButton4glte);
            }
            boolean zIsCtPlugin = ExtensionManager.getMobileNetworkSettingsExt().isCtPlugin();
            log("[addEnhanced4GLteSwitchPreference] ss :" + zIsCtPlugin);
            if (zIsVolteEnabled && !zIsCtPlugin) {
                int order = this.mButtonEnabledNetworks.getOrder() + 1;
                this.mEnhancedButton4glte = new Enhanced4GLteSwitchPreference(activity, this.mPhone.getSubId());
                this.mEnhancedButton4glte.setKey(BUTTON_4G_LTE_KEY);
                if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(this.mPhone.getSubId())) {
                    this.mEnhancedButton4glte.setTitle(R.string.hd_voice_switch_title);
                    this.mEnhancedButton4glte.setSummary(R.string.hd_voice_switch_summary);
                } else {
                    if (PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId()).getBoolean("enhanced_4g_lte_title_variant_bool")) {
                        i = R.string.enhanced_4g_lte_mode_title_variant;
                    } else {
                        i = R.string.enhanced_4g_lte_mode_title;
                    }
                    this.mEnhancedButton4glte.setTitle(i);
                }
                if (!TelephonyUtilsEx.isCtVolteEnabled() || !TelephonyUtilsEx.isCtSim(this.mPhone.getSubId())) {
                    this.mEnhancedButton4glte.setSummary(R.string.enhanced_4g_lte_mode_summary);
                }
                this.mEnhancedButton4glte.setOnPreferenceChangeListener(this);
                this.mEnhancedButton4glte.setOrder(order);
                ExtensionManager.getMobileNetworkSettingsExt().customizeEnhanced4GLteSwitchPreference(preferenceScreen, this.mEnhancedButton4glte);
                return;
            }
            this.mEnhancedButton4glte = null;
        }

        private void updateCapabilityRelatedPreference(boolean z) {
            boolean z2 = z && isCapabilityPhone(this.mPhone);
            log("updateNetworkModePreference:isNWModeEnabled = " + z2);
            if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkModeSettingNeeded()) {
                updateNetworkModePreference(this.mButtonPreferredNetworkMode, z2);
                updateNetworkModePreference(this.mButtonEnabledNetworks, z2);
                updateNetworkModeForLwDsds();
            }
            updateEnhanced4GLteSwitchPreference();
            if (TelephonyUtilsEx.isCDMAPhone(this.mPhone) && this.mCdmaNetworkSettings != null) {
                this.mCdmaNetworkSettings.onResume();
            } else {
                log("updateCapabilityRelatedPreference don't update cdma settings");
            }
        }

        private void updateEnhanced4GLteSwitchPreference() {
            Activity activity = getActivity();
            int phoneId = SubscriptionManager.getPhoneId(this.mPhone.getSubId());
            if (this.mEnhancedButton4glte != null) {
                boolean z = false;
                if (ExtensionManager.getMobileNetworkSettingsExt().isEnhancedLTENeedToAdd(isVolteEnabled() && ((SystemProperties.getInt(PROPERTY_MIMS_SUPPORT, 1) == 1 && TelephonyUtilsEx.getMainPhoneId() == this.mPhone.getPhoneId()) || (SystemProperties.getInt(PROPERTY_MIMS_SUPPORT, 1) > 1 && isCapabilityPhone(this.mPhone))), this.mPhone.getPhoneId())) {
                    if (findPreference(BUTTON_4G_LTE_KEY) == null) {
                        log("updateEnhanced4GLteSwitchPreference add switcher");
                        getPreferenceScreen().addPreference(this.mEnhancedButton4glte);
                    }
                } else if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                    log("updateEnhanced4G removed");
                    getPreferenceScreen().removePreference(this.mEnhancedButton4glte);
                }
                if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                    this.mEnhancedButton4glte.setSubId(this.mPhone.getSubId());
                    if (MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(activity, phoneId) && MtkImsManager.isNonTtyOrTtyOnVolteEnabled(activity, phoneId)) {
                        z = true;
                    }
                    this.mEnhancedButton4glte.setChecked(z);
                    log("[updateEnhanced4GLteSwitchPreference] SubId = " + this.mPhone.getSubId() + ", enh4glteMode=" + z);
                }
                updateEnhanced4glteEnableState();
            }
        }

        private void updateEnhanced4glteEnableState() {
            Activity activity;
            boolean z;
            if (this.mEnhancedButton4glte == null || (activity = getActivity()) == null) {
                return;
            }
            boolean zIsInCall = TelecomManager.from(activity).isInCall();
            boolean zIsNonTtyOrTtyOnVolteEnabled = MtkImsManager.isNonTtyOrTtyOnVolteEnabled(activity.getApplicationContext(), this.mPhone.getPhoneId());
            int subId = this.mPhone.getSubId();
            boolean zIsCtSim = TelephonyUtilsEx.isCtSim(subId);
            boolean z2 = false;
            if (TelephonyUtilsEx.isCtVolteEnabled() && zIsCtSim) {
                int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + subId, Phone.PREFERRED_NT_MODE);
                z = TelephonyUtilsEx.isCt4gSim(subId) && (i == 10 || i == 9 || i == 8 || i == 11 || i == 12);
                if (TelephonyUtilsEx.isCtAutoVolteEnabled()) {
                }
            } else {
                z = true;
            }
            boolean zCustomizeDualVolteOpDisable = ExtensionManager.getMobileNetworkSettingsExt().customizeDualVolteOpDisable(subId, z);
            boolean z3 = !zIsCtSim ? !TelephonyManager.getDefault().getSimOperator(subId).isEmpty() : true;
            boolean zCustomizeDualCCcard = ExtensionManager.getMobileNetworkSettingsExt().customizeDualCCcard(this.mPhone.getPhoneId());
            log("updateEnhanced4glteEnableState, incall = " + zIsInCall + ", nontty = " + zIsNonTtyOrTtyOnVolteEnabled + ", enableForCtVolte = " + zCustomizeDualVolteOpDisable + ", simReady = " + z3 + ", secondEnabled = " + zCustomizeDualCCcard);
            Enhanced4GLteSwitchPreference enhanced4GLteSwitchPreference = this.mEnhancedButton4glte;
            if (!zIsInCall && zIsNonTtyOrTtyOnVolteEnabled && hasActiveSubscriptions() && zCustomizeDualVolteOpDisable && z3 && zCustomizeDualCCcard) {
                z2 = true;
            }
            enhanced4GLteSwitchPreference.setEnabled(z2);
            ExtensionManager.getMobileNetworkSettingsExt().customizeDualVolteOpHide(getPreferenceScreen(), this.mEnhancedButton4glte, zCustomizeDualVolteOpDisable);
        }

        private boolean onPreferenceChangeMTK(Preference preference, Object obj) {
            String string = getResources().getString(R.string.hd_voice_switch_title);
            String string2 = getResources().getString(R.string.enhanced_4g_lte_mode_title);
            log("[onPreferenceChangeMTK] Preference = " + ((Object) preference.getTitle()));
            if (this.mEnhancedButton4glte != preference && !preference.getTitle().equals(string) && !preference.getTitle().equals(string2)) {
                return false;
            }
            Enhanced4GLteSwitchPreference enhanced4GLteSwitchPreference = (Enhanced4GLteSwitchPreference) preference;
            log("[onPreferenceChangeMTK] IsChecked = " + enhanced4GLteSwitchPreference.isChecked());
            if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(this.mPhone.getSubId()) && !enhanced4GLteSwitchPreference.isChecked()) {
                int networkType = TelephonyManager.getDefault().getNetworkType(this.mPhone.getSubId());
                log("network type = " + networkType);
                if (13 != networkType && !TelephonyUtilsEx.isRoaming(this.mPhone) && ((TelephonyUtilsEx.getMainPhoneId() == this.mPhone.getPhoneId() || TelephonyUtilsEx.isBothslotCt4gSim(this.mSubscriptionManager)) && !TelephonyUtilsEx.isCtAutoVolteEnabled())) {
                    showVolteUnavailableDialog();
                    return false;
                }
            }
            boolean z = !enhanced4GLteSwitchPreference.isChecked();
            enhanced4GLteSwitchPreference.setChecked(z);
            this.mEnhancedButton4glte.setChecked(z);
            log("[onPreferenceChangeMTK] IsChecked2 = " + z);
            log("[onPreferenceChangeMTK] mEnhancedButton4glte2 = " + this.mEnhancedButton4glte.isChecked());
            MtkImsManager.setEnhanced4gLteModeSetting(getActivity(), enhanced4GLteSwitchPreference.isChecked(), this.mPhone.getPhoneId());
            updateVideoCallState();
            return true;
        }

        private void showVolteUnavailableDialog() {
            log("showVolteUnavailableDialog ...");
            AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.alert_ct_volte_unavailable, new Object[]{PhoneUtils.getSubDisplayName(this.mPhone.getSubId())})).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("dialog cancel mEnhanced4GLteSwitchPreference.setchecked  = ");
                    sb.append(!MobileNetworkFragment.this.mEnhancedButton4glte.isChecked());
                    MobileNetworkFragment.log(sb.toString());
                    MobileNetworkFragment.this.mEnhancedButton4glte.setChecked(false);
                }
            }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MobileNetworkFragment.this.mEnhancedButton4glte.setChecked(true);
                    MobileNetworkFragment.log("dialog ok ims set " + MobileNetworkFragment.this.mEnhancedButton4glte.isChecked() + " mSlotId = " + SubscriptionManager.getPhoneId(MobileNetworkFragment.this.mPhone.getSubId()));
                    MtkImsManager.setEnhanced4gLteModeSetting(MobileNetworkFragment.this.getActivity(), MobileNetworkFragment.this.mEnhancedButton4glte.isChecked(), MobileNetworkFragment.this.mPhone.getPhoneId());
                }
            }).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            alertDialogCreate.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                    if (4 != i || dialogInterface == null) {
                        return false;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("onKey keycode = backdialog cancel mEnhanced4GLteSwitchPreference.setchecked  = ");
                    sb.append(!MobileNetworkFragment.this.mEnhancedButton4glte.isChecked());
                    MobileNetworkFragment.log(sb.toString());
                    MobileNetworkFragment.this.mEnhancedButton4glte.setChecked(false);
                    dialogInterface.dismiss();
                    return true;
                }
            });
            this.mDialog = alertDialogCreate;
            alertDialogCreate.show();
        }

        private void addPLMNList(PreferenceScreen preferenceScreen) {
            int order = preferenceScreen.findPreference(SINGLE_LTE_DATA) != null ? preferenceScreen.findPreference(SINGLE_LTE_DATA).getOrder() : this.mButtonDataRoam.getOrder();
            this.mPLMNPreference = new Preference(getActivity());
            this.mPLMNPreference.setKey(BUTTON_PLMN_LIST);
            this.mPLMNPreference.setTitle(R.string.plmn_list_setting_title);
            Intent intent = new Intent();
            intent.setClassName("com.android.phone", "com.mediatek.settings.PLMNListPreference");
            intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mPhone.getSubId());
            this.mPLMNPreference.setIntent(intent);
            this.mPLMNPreference.setOrder(order + 1);
            preferenceScreen.addPreference(this.mPLMNPreference);
        }

        private void updateScreenStatus() {
            Activity activity = getActivity();
            if (activity == null) {
                log("updateScreenStatus, activity = null");
                return;
            }
            boolean z = false;
            boolean z2 = TelephonyManager.getDefault().getCallState() == 0;
            if (z2 && TelephonyUtils.isRadioOn(this.mPhone.getSubId(), activity)) {
                z = true;
            }
            if (DBG) {
                log("updateScreenStatus:isShouldEnabled = " + z + ", isIdle = " + z2);
            }
            getPreferenceScreen().setEnabled(z);
            updateCapabilityRelatedPreference(z);
        }

        private boolean isC2kLteSupport() {
            return PhoneFeatureConstants.FeatureOption.isMtkSrlteSupport() || PhoneFeatureConstants.FeatureOption.isMtkSvlteSupport();
        }

        private void updateNetworkModeForLwDsds() {
            int mainCapabilityPhoneId = getMainCapabilityPhoneId();
            log("handleLwDsdsNetworkMode mainPhoneId = " + mainCapabilityPhoneId);
            if (mainCapabilityPhoneId != this.mPhone.getPhoneId()) {
                int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), 1);
                int radioAccessFamily = this.mPhone.getRadioAccessFamily();
                log("updateNetworkModeForLwDsds settingsNetworkMode = " + i + "; currRat = " + radioAccessFamily);
                if ((radioAccessFamily & 16384) == 16384) {
                    this.mButtonEnabledNetworks.setEntries(this.mShow4GForLTE ? R.array.enabled_networks_4g_choices : R.array.enabled_networks_choices);
                    this.mButtonEnabledNetworks.setEntryValues(isC2kLteSupport() ? R.array.enabled_networks_values_c2k : R.array.enabled_networks_values);
                    log("updateNetworkModeForLwDsds mShow4GForLTE = " + this.mShow4GForLTE);
                    return;
                }
                if ((radioAccessFamily & 8) == 8) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                    if (isC2kLteSupport()) {
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values_c2k);
                    } else {
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values);
                    }
                    if (i > 8) {
                        log("updateNetworkModeForLwDsds set network mode to 3G");
                        if (isC2kLteSupport()) {
                            this.mButtonEnabledNetworks.setValue(Integer.toString(7));
                        } else {
                            this.mButtonEnabledNetworks.setValue(Integer.toString(0));
                        }
                        this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                        return;
                    }
                    log("updateNetworkModeForLwDsds set to what user select. ");
                    UpdateEnabledNetworksValueAndSummary(i);
                    return;
                }
                log("updateNetworkModeForLwDsds set to 2G only.");
                this.mButtonEnabledNetworks.setSummary(R.string.network_2G);
                this.mButtonEnabledNetworks.setEnabled(false);
            }
        }

        private void updateNetworkModePreference(ListPreference listPreference, boolean z) {
            if (listPreference != null) {
                listPreference.setEnabled(z);
                if (!z) {
                    dissmissDialog(listPreference);
                }
                if (getPreferenceScreen().findPreference(listPreference.getKey()) != null) {
                    updatePreferredNetworkUIFromDb();
                }
                this.mOmEx.updateLTEModeStatus(listPreference);
            }
        }

        private void handleC2kCommonScreen(PreferenceScreen preferenceScreen) {
            log("--- go to C2k Common (3M, 5M) screen ---");
            if (preferenceScreen.findPreference(BUTTON_PREFERED_NETWORK_MODE) != null) {
                preferenceScreen.removePreference(preferenceScreen.findPreference(BUTTON_PREFERED_NETWORK_MODE));
            }
            if (TelephonyUtilsEx.isCDMAPhone(this.mPhone) && preferenceScreen.findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                preferenceScreen.removePreference(preferenceScreen.findPreference(BUTTON_ENABLED_NETWORKS_KEY));
            }
        }

        private void handleC2k3MScreen(PreferenceScreen preferenceScreen) {
            if (!PhoneFeatureConstants.FeatureOption.isMtkLteSupport() && PhoneFeatureConstants.FeatureOption.isMtkC2k3MSupport()) {
                handleC2kCommonScreen(preferenceScreen);
                log("--- go to C2k 3M ---");
                if (!TelephonyUtilsEx.isCDMAPhone(this.mPhone)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values_c2k);
                }
            }
        }

        private void handleC2k4MScreen(PreferenceScreen preferenceScreen) {
            if (PhoneFeatureConstants.FeatureOption.isMtkLteSupport() && PhoneFeatureConstants.FeatureOption.isMtkC2k4MSupport()) {
                log("--- go to C2k 4M ---");
                if (1 == this.mPhone.getPhoneType()) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_td_cdma_3g_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_td_cdma_3g_values);
                }
            }
        }

        private void handleC2k5MScreen(PreferenceScreen preferenceScreen) {
            if (PhoneFeatureConstants.FeatureOption.isMtkLteSupport() && PhoneFeatureConstants.FeatureOption.isMtkC2k5MSupport()) {
                handleC2kCommonScreen(preferenceScreen);
                log("--- go to c2k 5M ---");
                if (!TelephonyUtilsEx.isCDMAPhone(this.mPhone)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_4g_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values_c2k);
                }
            }
        }

        public static int getMainCapabilityPhoneId() {
            return SystemProperties.getInt("persist.vendor.radio.simswitch", 1) - 1;
        }

        private void restorePreferredNetworkTypeIfNeeded(Message message) {
            if (((AsyncResult) message.obj).exception != null && this.mPreNetworkMode != -1 && this.mPhone != null) {
                int subId = this.mPhone.getSubId();
                log("set failed, reset preferred network mode to " + this.mPreNetworkMode + ", sub id = " + subId);
                ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
                StringBuilder sb = new StringBuilder();
                sb.append("preferred_network_mode");
                sb.append(subId);
                Settings.Global.putInt(contentResolver, sb.toString(), this.mPreNetworkMode);
            }
            this.mPreNetworkMode = -1;
        }

        private boolean isVolteEnabled() {
            boolean zIsVolteEnabledByPlatform;
            if (this.mImsMgr != null) {
                zIsVolteEnabledByPlatform = this.mImsMgr.isVolteEnabledByPlatform();
            } else {
                zIsVolteEnabledByPlatform = false;
            }
            log("isVolteEnabled = " + zIsVolteEnabledByPlatform);
            return zIsVolteEnabledByPlatform;
        }

        private boolean isWfcEnabled() {
            boolean zIsWfcEnabledByPlatform;
            if (this.mImsMgr != null) {
                zIsWfcEnabledByPlatform = this.mImsMgr.isWfcEnabledByPlatform();
            } else {
                zIsWfcEnabledByPlatform = false;
            }
            log("isWfcEnabled = " + zIsWfcEnabledByPlatform);
            return zIsWfcEnabledByPlatform;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$phone$MobileNetworkSettings$TabState = new int[TabState.values().length];

        static {
            try {
                $SwitchMap$com$android$phone$MobileNetworkSettings$TabState[TabState.UPDATE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$phone$MobileNetworkSettings$TabState[TabState.NO_TABS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$phone$MobileNetworkSettings$TabState[TabState.DO_NOTHING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }
}
