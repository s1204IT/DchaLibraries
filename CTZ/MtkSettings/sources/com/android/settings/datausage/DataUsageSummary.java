package com.android.settings.datausage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.datausage.BillingCycleSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.net.DataUsageController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.datausage.TempDataServiceDialogActivity;
import com.mediatek.settings.ext.IDataUsageSummaryExt;
import com.mediatek.settings.sim.TelephonyUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DataUsageSummary extends DataUsageBaseFragment implements DataUsageEditController, Indexable {
    private Context mContext;
    private IDataUsageSummaryExt mDataUsageSummaryExt;
    private int mDefaultSubId;
    private NetworkTemplate mDefaultTemplate;
    private SwitchPreference mEnableDataService;
    private boolean mIsAirplaneModeOn;
    private PhoneStateListener mPhoneStateListener;
    private DataUsageSummaryPreferenceController mSummaryController;
    private DataUsageSummaryPreference mSummaryPreference;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public final SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new DataUsageSummary.SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.data_usage;
            arrayList.add(searchIndexableResource);
            SearchIndexableResource searchIndexableResource2 = new SearchIndexableResource(context);
            searchIndexableResource2.xmlResId = R.xml.data_usage_cellular;
            arrayList.add(searchIndexableResource2);
            SearchIndexableResource searchIndexableResource3 = new SearchIndexableResource(context);
            searchIndexableResource3.xmlResId = R.xml.data_usage_wifi;
            arrayList.add(searchIndexableResource3);
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!DataUsageUtils.hasMobileData(context)) {
                nonIndexableKeys.add("mobile_category");
                nonIndexableKeys.add("data_usage_enable");
                nonIndexableKeys.add("cellular_data_usage");
                nonIndexableKeys.add("billing_preference");
            }
            if (!DataUsageUtils.hasWifiRadio(context)) {
                nonIndexableKeys.add("wifi_data_usage");
            }
            nonIndexableKeys.add("wifi_category");
            return nonIndexableKeys;
        }
    };
    private int mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
    int mTempPhoneid = 0;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            if (DataUsageSummary.this.mEnableDataService != null) {
                boolean dataService = DataUsageSummary.this.getDataService();
                Log.d("DataUsageSummary", "onChange dataService = " + dataService + ", isChecked = " + DataUsageSummary.this.mEnableDataService.isChecked());
                if (dataService != DataUsageSummary.this.mEnableDataService.isChecked()) {
                    DataUsageSummary.this.mEnableDataService.setChecked(dataService);
                    return;
                }
                return;
            }
            Log.d("DataUsageSummary", "onChange mEnableDataService == null");
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("DataUsageSummary", "mReceiver action = " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                DataUsageSummary.this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                DataUsageSummary.this.updateScreenEnabled();
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED") || DataUsageSummary.this.mDataUsageSummaryExt.customDualReceiver(action)) {
                DataUsageSummary.this.updateScreenEnabled();
            } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED")) {
                DataUsageSummary.this.updateScreenEnabled();
            }
        }
    };

    @Override
    public int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d("DataUsageSummary", "onCreate");
        this.mContext = getContext();
        this.mDataUsageSummaryExt = UtilsExt.getDataUsageSummaryExt(this.mContext.getApplicationContext());
        boolean zHasMobileData = DataUsageUtils.hasMobileData(this.mContext);
        this.mDefaultSubId = DataUsageUtils.getDefaultSubscriptionId(this.mContext);
        if (this.mDefaultSubId == -1) {
            Log.d("DataUsageSummary", "onCreate INVALID_SUBSCRIPTION_ID Mobile data false");
            zHasMobileData = false;
        }
        this.mDefaultTemplate = DataUsageUtils.getDefaultTemplate(this.mContext, this.mDefaultSubId);
        this.mSummaryPreference = (DataUsageSummaryPreference) findPreference("status_header");
        if (!zHasMobileData || !isAdmin()) {
            removePreference("restrict_background");
        }
        boolean zHasWifiRadio = DataUsageUtils.hasWifiRadio(this.mContext);
        if (zHasMobileData) {
            addMobileSection(this.mDefaultSubId);
            List<SubscriptionInfo> activeSubscriptionInfoList = this.services.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() == 2) {
                addDataServiceSection(activeSubscriptionInfoList);
            }
            if (DataUsageUtils.hasSim(this.mContext) && zHasWifiRadio) {
                addWifiSection();
            }
        } else if (zHasWifiRadio) {
            addWifiSection();
        }
        if (DataUsageUtils.hasEthernet(this.mContext)) {
            addEthernetSection();
        }
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (UserManager.get(getContext()).isAdminUser()) {
            menuInflater.inflate(R.menu.data_usage, menu);
        }
        if (Utils.isWifiOnly(getActivity())) {
            menu.removeItem(R.id.data_usage_menu_cellular_networks);
        }
        menu.removeItem(R.id.data_usage_menu_cellular_networks);
        menu.removeItem(R.id.data_usage_menu_cellular_data_control);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.data_usage_menu_cellular_data_control:
                Log.d("DataUsageSummary", "select CELLULAR_DATA");
                try {
                    startActivity(new Intent("com.mediatek.security.CELLULAR_DATA"));
                } catch (ActivityNotFoundException e) {
                    Log.e("DataUsageSummary", "cellular data control activity not found!!!");
                }
                break;
            case R.id.data_usage_menu_cellular_networks:
                Log.d("DataUsageSummary", "select CELLULAR_NETWORKDATA");
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                break;
        }
        return true;
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference("status_header")) {
            BillingCycleSettings.BytesEditorFragment.show((DataUsageEditController) this, false);
            return false;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.data_usage;
    }

    @Override
    protected String getLogTag() {
        return "DataUsageSummary";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        Activity activity = getActivity();
        ArrayList arrayList = new ArrayList();
        this.mSummaryController = new DataUsageSummaryPreferenceController(activity, getLifecycle(), this);
        arrayList.add(this.mSummaryController);
        getLifecycle().addObserver(this.mSummaryController);
        return arrayList;
    }

    void addMobileSection(int i) {
        addMobileSection(i, (SubscriptionInfo) null);
    }

    void addMobileSection(int i, int i2) {
        addMobileSection(i, null, i2);
    }

    private void addMobileSection(int i, SubscriptionInfo subscriptionInfo, int i2) {
        TemplatePreferenceCategory templatePreferenceCategory = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_cellular, i2);
        Log.d("DataUsageSummary", "addMobileSection with subID: " + i + " orderd = " + i2);
        templatePreferenceCategory.setTemplate(getNetworkTemplate(i), i, this.services);
        templatePreferenceCategory.pushTemplates(this.services);
        if (subscriptionInfo != null && !TextUtils.isEmpty(subscriptionInfo.getDisplayName())) {
            templatePreferenceCategory.findPreference("mobile_category").setTitle(subscriptionInfo.getDisplayName());
        }
    }

    private Preference inflatePreferences(int i, int i2) {
        PreferenceScreen preferenceScreenInflateFromResource = getPreferenceManager().inflateFromResource(getPrefContext(), i, null);
        Preference preference = preferenceScreenInflateFromResource.getPreference(0);
        preferenceScreenInflateFromResource.removeAll();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preference.setOrder(i2);
        preferenceScreen.addPreference(preference);
        return preference;
    }

    private void addMobileSection(int i, SubscriptionInfo subscriptionInfo) {
        TemplatePreferenceCategory templatePreferenceCategory = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_cellular);
        Log.d("DataUsageSummary", "addMobileSection with subID: " + i);
        templatePreferenceCategory.setTemplate(getNetworkTemplate(i), i, this.services);
        templatePreferenceCategory.pushTemplates(this.services);
        if (subscriptionInfo != null && !TextUtils.isEmpty(subscriptionInfo.getDisplayName())) {
            templatePreferenceCategory.findPreference("mobile_category").setTitle(subscriptionInfo.getDisplayName());
        }
    }

    void addWifiSection() {
        ((TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_wifi)).setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0, this.services);
    }

    private void addEthernetSection() {
        ((TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_ethernet)).setTemplate(NetworkTemplate.buildTemplateEthernet(), 0, this.services);
    }

    private Preference inflatePreferences(int i) {
        PreferenceScreen preferenceScreenInflateFromResource = getPreferenceManager().inflateFromResource(getPrefContext(), i, null);
        Preference preference = preferenceScreenInflateFromResource.getPreference(0);
        preferenceScreenInflateFromResource.removeAll();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preference.setOrder(preferenceScreen.getPreferenceCount());
        preferenceScreen.addPreference(preference);
        return preference;
    }

    private NetworkTemplate getNetworkTemplate(int i) {
        NetworkTemplate networkTemplateBuildTemplateMobileAll = NetworkTemplate.buildTemplateMobileAll(this.services.mTelephonyManager.getSubscriberId(i));
        Log.d("DataUsageSummary", "getNetworkTemplate with subID: " + i);
        return NetworkTemplate.normalize(networkTemplateBuildTemplateMobileAll, this.services.mTelephonyManager.getMergedSubscriberIds());
    }

    @Override
    public void onResume() {
        TemplatePreferenceCategory templatePreferenceCategory;
        super.onResume();
        int defaultSubscriptionId = DataUsageUtils.getDefaultSubscriptionId(this.mContext);
        Log.d("DataUsageSummary", "onResumed mDefaultSubId = " + this.mDefaultSubId + " newDefaultSubId = " + defaultSubscriptionId);
        boolean zHasMobileData = DataUsageUtils.hasMobileData(this.mContext);
        if (this.mDefaultSubId == -1) {
            Log.d("DataUsageSummary", "onResume INVALID_SUBSCRIPTION_ID Mobile data false");
            zHasMobileData = false;
        }
        if (zHasMobileData && this.mDefaultSubId != defaultSubscriptionId && (templatePreferenceCategory = (TemplatePreferenceCategory) getPreferenceScreen().findPreference("mobile_category")) != null) {
            int order = templatePreferenceCategory.getOrder();
            getPreferenceScreen().removePreference(templatePreferenceCategory);
            Log.d("DataUsageSummary", "removePreferencedd and add (data_usage_cellular_screen) order = " + order);
            addMobileSection(defaultSubscriptionId, order);
        }
        if (TelephonyUtils.getMainCapabilityPhoneId() == 0) {
            this.mTempPhoneid = 1;
        } else {
            this.mTempPhoneid = 0;
        }
        updateScreenEnabled();
        updateState();
    }

    static CharSequence formatUsage(Context context, String str, long j) {
        return formatUsage(context, str, j, 1.5625f, 0.64f);
    }

    static CharSequence formatUsage(Context context, String str, long j, float f, float f2) {
        Formatter.BytesResult bytes = Formatter.formatBytes(context.getResources(), j, 10);
        SpannableString spannableString = new SpannableString(bytes.value);
        spannableString.setSpan(new RelativeSizeSpan(f), 0, spannableString.length(), 18);
        CharSequence charSequenceExpandTemplate = TextUtils.expandTemplate(new SpannableString(context.getString(android.R.string.config_carrierAppInstallDialogComponent).replace("%1$s", "^1").replace("%2$s", "^2")), spannableString, bytes.units);
        SpannableString spannableString2 = new SpannableString(str);
        spannableString2.setSpan(new RelativeSizeSpan(f2), 0, spannableString2.length(), 18);
        return TextUtils.expandTemplate(spannableString2, BidiFormatter.getInstance().unicodeWrap(charSequenceExpandTemplate.toString()));
    }

    private void updateState() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 1; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if ((preference instanceof PreferenceCategory) && ((PreferenceCategory) preference).getKey().equals("service_category")) {
                if (this.mEnableDataService != null) {
                    boolean dataService = getDataService();
                    this.mEnableDataService.setChecked(dataService);
                    Log.d("DataUsageSummary", "updateState, dataService=" + dataService);
                }
            } else if (preference instanceof TemplatePreferenceCategory) {
                ((TemplatePreferenceCategory) preference).pushTemplates(this.services);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return 37;
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return this.services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        Log.d("DataUsageSummary", "getNetworkTemplate without subID: DefaultTemplate");
        return this.mDefaultTemplate;
    }

    @Override
    public void updateDataUsage() {
        updateState();
        this.mSummaryController.updateState(this.mSummaryPreference);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Activity mActivity;
        private final DataUsageController mDataController;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            this.mActivity = activity;
            this.mSummaryLoader = summaryLoader;
            this.mDataController = new DataUsageController(activity);
        }

        @Override
        public void setListening(boolean z) {
            if (z) {
                if (DataUsageUtils.hasSim(this.mActivity)) {
                    this.mSummaryLoader.setSummary(this, this.mActivity.getString(R.string.data_usage_summary_format, new Object[]{formatUsedData()}));
                    return;
                }
                DataUsageController.DataUsageInfo dataUsageInfo = this.mDataController.getDataUsageInfo(NetworkTemplate.buildTemplateWifiWildcard());
                if (dataUsageInfo == null) {
                    this.mSummaryLoader.setSummary(this, null);
                    return;
                }
                this.mSummaryLoader.setSummary(this, TextUtils.expandTemplate(this.mActivity.getText(R.string.data_usage_wifi_format), DataUsageUtils.formatDataUsage(this.mActivity, dataUsageInfo.usageLevel)));
            }
        }

        private CharSequence formatUsedData() {
            SubscriptionManager subscriptionManager = (SubscriptionManager) this.mActivity.getSystemService("telephony_subscription_service");
            int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
            if (defaultSubscriptionId == -1) {
                return formatFallbackData();
            }
            SubscriptionPlan primaryPlan = DataUsageSummaryPreferenceController.getPrimaryPlan(subscriptionManager, defaultSubscriptionId);
            if (primaryPlan == null) {
                return formatFallbackData();
            }
            if (DataUsageSummaryPreferenceController.unlimited(primaryPlan.getDataLimitBytes())) {
                return DataUsageUtils.formatDataUsage(this.mActivity, primaryPlan.getDataUsageBytes());
            }
            return Utils.formatPercentage(primaryPlan.getDataUsageBytes(), primaryPlan.getDataLimitBytes());
        }

        private CharSequence formatFallbackData() {
            DataUsageController.DataUsageInfo dataUsageInfo = this.mDataController.getDataUsageInfo();
            if (dataUsageInfo == null) {
                return DataUsageUtils.formatDataUsage(this.mActivity, 0L);
            }
            if (dataUsageInfo.limitLevel <= 0) {
                return DataUsageUtils.formatDataUsage(this.mActivity, dataUsageInfo.usageLevel);
            }
            return Utils.formatPercentage(dataUsageInfo.usageLevel, dataUsageInfo.limitLevel);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.data_usage_menu_cellular_data_control);
        try {
            Class<?> cls = Class.forName("com.mediatek.cta.CtaUtils", false, ClassLoader.getSystemClassLoader());
            Method declaredMethod = cls.getDeclaredMethod("isCtaSupported", new Class[0]);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(cls, new Object[0]);
            if (menuItemFindItem != null) {
                menuItemFindItem.setVisible(Boolean.valueOf(objInvoke.toString()).booleanValue());
            }
        } catch (Exception e) {
            if (menuItemFindItem != null) {
                menuItemFindItem.setVisible(false);
            }
            e.printStackTrace();
        }
    }

    private void addDataServiceSection(List<SubscriptionInfo> list) {
        if (!isDataServiceSupport()) {
            return;
        }
        Log.d("DataUsageSummary", "addDataServiceSection");
        if (list == null || list.size() != 2) {
            Log.d("DataUsageSummary", "subscriptions size != 2");
            return;
        }
        this.mEnableDataService = (SwitchPreference) findPreference("data_service_enable");
        this.mEnableDataService.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                Log.d("DataUsageSummary", "onPreferenceChange, preference=" + ((Object) preference.getTitle()));
                if (preference == DataUsageSummary.this.mEnableDataService) {
                    if (!DataUsageSummary.this.mEnableDataService.isChecked()) {
                        DataUsageSummary.this.showDataServiceDialog();
                        DataUsageSummary.this.mEnableDataService.setEnabled(false);
                        return false;
                    }
                    DataUsageSummary.this.setDataService(0);
                    return true;
                }
                return true;
            }
        });
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getContext());
        if (TelephonyUtils.getMainCapabilityPhoneId() == 0) {
            this.mTempPhoneid = 1;
        } else {
            this.mTempPhoneid = 0;
        }
        updateScreenEnabled();
        this.mEnableDataService.setChecked(getDataService());
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("data_service_enabled"), true, this.mContentObserver);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        this.mDataUsageSummaryExt.customReceiver(intentFilter);
        ((TelephonyManager) getSystemService("phone")).listen(getPhoneStateListener(this.mTempPhoneid, SubscriptionManager.getSubId(this.mTempPhoneid)[0]), 32);
        getContext().registerReceiver(this.mReceiver, intentFilter);
    }

    private PhoneStateListener getPhoneStateListener(int i, int i2) {
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(i2)) {
            @Override
            public void onCallStateChanged(int i3, String str) {
                Log.d("DataUsageSummary", "onCallStateChanged state = " + i3);
                DataUsageSummary.this.updateScreenEnabled();
            }
        };
        return this.mPhoneStateListener;
    }

    private void showDataServiceDialog() {
        Log.d("DataUsageSummary", "showDataServiceDialog");
        startActivity(new Intent(getContext(), (Class<?>) TempDataServiceDialogActivity.class));
    }

    private void updateScreenEnabled() {
        boolean zIsCapabilitySwitching = TelephonyUtils.isCapabilitySwitching();
        Log.d("DataUsageSummary", "updateScreenEnabled, mIsAirplaneModeOn = " + this.mIsAirplaneModeOn + ", isSwitching = " + zIsCapabilitySwitching + ", mTempPhoneid = " + this.mTempPhoneid);
        if (this.mEnableDataService != null) {
            this.mEnableDataService.setEnabled((this.mIsAirplaneModeOn || zIsCapabilitySwitching || this.mDataUsageSummaryExt.customTempdata(this.mTempPhoneid)) ? false : true);
            this.mDataUsageSummaryExt.customTempdataHide(this.mEnableDataService);
        } else {
            Log.d("DataUsageSummary", "mEnableDataService == null");
        }
    }

    private boolean getDataService() {
        int i;
        Context context = getContext();
        if (context != null) {
            i = Settings.Global.getInt(context.getContentResolver(), "data_service_enabled", 0);
        } else {
            i = 0;
        }
        Log.d("DataUsageSummary", "getDataService =" + i);
        return i != 0;
    }

    private void setDataService(int i) {
        Log.d("DataUsageSummary", "setDataService =" + i);
        Settings.Global.putInt(getContext().getContentResolver(), "data_service_enabled", i);
    }

    private static boolean isDataServiceSupport() {
        return "1".equals(SystemProperties.get("persist.vendor.radio.smart.data.switch"));
    }

    @Override
    public void onDestroy() {
        Log.d("DataUsageSummary", "onDestroy");
        super.onDestroy();
        if (!isDataServiceSupport()) {
            return;
        }
        if (this.mEnableDataService != null) {
            getContentResolver().unregisterContentObserver(this.mContentObserver);
            getContext().unregisterReceiver(this.mReceiver);
            this.mEnableDataService = null;
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        if (this.mPhoneStateListener != null) {
            telephonyManager.listen(this.mPhoneStateListener, 0);
        }
    }
}
