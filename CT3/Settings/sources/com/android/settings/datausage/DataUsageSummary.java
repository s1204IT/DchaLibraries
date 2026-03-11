package com.android.settings.datausage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
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
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.net.DataUsageController;
import com.mediatek.cta.CtaUtils;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.ArrayList;
import java.util.List;

public class DataUsageSummary extends DataUsageBase implements Indexable {
    private DataUsageController mDataUsageController;
    private int mDataUsageTemplate;
    private NetworkTemplate mDefaultTemplate;
    private Preference mLimitPreference;
    private SimHotSwapHandler mSimHotSwapHandler;
    private SummaryPreference mSummaryPreference;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            ArrayList<SearchIndexableResource> resources = new ArrayList<>();
            SearchIndexableResource resource = new SearchIndexableResource(context);
            resource.xmlResId = R.xml.data_usage;
            resources.add(resource);
            if (DataUsageSummary.hasMobileData(context)) {
                SearchIndexableResource resource2 = new SearchIndexableResource(context);
                resource2.xmlResId = R.xml.data_usage_cellular;
                resources.add(resource2);
            }
            if (DataUsageSummary.hasWifiRadio(context)) {
                SearchIndexableResource resource3 = new SearchIndexableResource(context);
                resource3.xmlResId = R.xml.data_usage_wifi;
                resources.add(resource3);
            }
            return resources;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> keys = new ArrayList<>();
            boolean hasMobileData = ConnectivityManager.from(context).isNetworkSupported(0);
            if (hasMobileData) {
                keys.add("restrict_background");
            }
            return keys;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        int i;
        super.onCreate(icicle);
        boolean hasMobileData = hasMobileData(getContext());
        this.mDataUsageController = new DataUsageController(getContext());
        addPreferencesFromResource(R.xml.data_usage);
        int defaultSubId = getDefaultSubscriptionId(getContext());
        if (defaultSubId == -1) {
            hasMobileData = false;
        }
        this.mDefaultTemplate = getDefaultTemplate(getContext(), defaultSubId);
        if (hasMobileData) {
            this.mLimitPreference = findPreference("limit_summary");
        } else {
            removePreference("limit_summary");
        }
        if (!hasMobileData || !isAdmin()) {
            removePreference("restrict_background");
        }
        if (hasMobileData) {
            List<SubscriptionInfo> subscriptions = this.services.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.size() == 0) {
                addMobileSection(defaultSubId);
            }
            for (int i2 = 0; subscriptions != null && i2 < subscriptions.size(); i2++) {
                addMobileSection(subscriptions.get(i2).getSubscriptionId());
            }
        }
        boolean hasWifiRadio = hasWifiRadio(getContext());
        if (hasWifiRadio) {
            addWifiSection();
        }
        if (hasEthernet(getContext())) {
            addEthernetSection();
        }
        if (hasMobileData) {
            i = R.string.cell_data_template;
        } else {
            i = hasWifiRadio ? R.string.wifi_data_template : R.string.ethernet_data_template;
        }
        this.mDataUsageTemplate = i;
        this.mSummaryPreference = (SummaryPreference) findPreference("status_header");
        setHasOptionsMenu(true);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (DataUsageSummary.this.getActivity() == null) {
                    return;
                }
                Log.d("DataUsageSummary", "onSimHotSwap, finish Activity~~");
                DataUsageSummary.this.getActivity().finish();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (UserManager.get(getContext()).isAdminUser()) {
            inflater.inflate(R.menu.data_usage, menu);
        }
        if (Utils.isWifiOnly(getActivity())) {
            menu.removeItem(R.id.data_usage_menu_cellular_networks);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_cellular_data_control:
                Log.d("DataUsageSummary", "select CELLULAR_DATA");
                try {
                    startActivity(new Intent("com.mediatek.security.CELLULAR_DATA"));
                } catch (ActivityNotFoundException e) {
                    Log.e("DataUsageSummary", "cellular data control activity not found!!!");
                }
                break;
            case R.id.data_usage_menu_cellular_networks:
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                break;
        }
        return true;
        return true;
    }

    private void addMobileSection(int subId) {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_cellular);
        category.setTemplate(getNetworkTemplate(subId), subId, this.services);
        category.pushTemplates(this.services);
    }

    private void addWifiSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_wifi);
        category.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0, this.services);
    }

    private void addEthernetSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_ethernet);
        category.setTemplate(NetworkTemplate.buildTemplateEthernet(), 0, this.services);
    }

    private Preference inflatePreferences(int resId) {
        PreferenceScreen rootPreferences = getPreferenceManager().inflateFromResource(getPrefContext(), resId, null);
        Preference pref = rootPreferences.getPreference(0);
        rootPreferences.removeAll();
        PreferenceScreen screen = getPreferenceScreen();
        pref.setOrder(screen.getPreferenceCount());
        screen.addPreference(pref);
        return pref;
    }

    private NetworkTemplate getNetworkTemplate(int subscriptionId) {
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(this.services.mTelephonyManager.getSubscriberId(subscriptionId));
        return NetworkTemplate.normalize(mobileAll, this.services.mTelephonyManager.getMergedSubscriberIds());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private static void verySmallSpanExcept(SpannableString s, CharSequence exception) {
        int exceptionStart = TextUtils.indexOf(s, exception);
        if (exceptionStart == -1) {
            s.setSpan(new RelativeSizeSpan(0.64000005f), 0, s.length(), 18);
            return;
        }
        if (exceptionStart > 0) {
            s.setSpan(new RelativeSizeSpan(0.64000005f), 0, exceptionStart, 18);
        }
        int exceptionEnd = exceptionStart + exception.length();
        if (exceptionEnd >= s.length()) {
            return;
        }
        s.setSpan(new RelativeSizeSpan(0.64000005f), exceptionEnd, s.length(), 18);
    }

    private static CharSequence formatTitle(Context context, String template, long usageLevel) {
        SpannableString amountTemplate = new SpannableString(context.getString(android.R.string.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK_IN_PROGRESS).replace("%1$s", "^1").replace("%2$s", "^2"));
        verySmallSpanExcept(amountTemplate, "^1");
        Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(), usageLevel, 1);
        CharSequence formattedUsage = TextUtils.expandTemplate(amountTemplate, usedResult.value, usedResult.units);
        SpannableString fullTemplate = new SpannableString(template.replace("%1$s", "^1"));
        verySmallSpanExcept(fullTemplate, "^1");
        return TextUtils.expandTemplate(fullTemplate, BidiFormatter.getInstance().unicodeWrap(formattedUsage));
    }

    private void updateState() {
        DataUsageController.DataUsageInfo info = this.mDataUsageController.getDataUsageInfo(this.mDefaultTemplate);
        Context context = getContext();
        if (this.mSummaryPreference != null) {
            this.mSummaryPreference.setTitle(formatTitle(context, getString(this.mDataUsageTemplate), info.usageLevel));
            long limit = info.limitLevel;
            if (limit <= 0) {
                limit = info.warningLevel;
            }
            if (info.usageLevel > limit) {
                limit = info.usageLevel;
            }
            this.mSummaryPreference.setSummary(info.period);
            this.mSummaryPreference.setLabels(Formatter.formatFileSize(context, 0L), Formatter.formatFileSize(context, limit));
            this.mSummaryPreference.setRatios(info.usageLevel / limit, 0.0f, (limit - info.usageLevel) / limit);
        }
        if (this.mLimitPreference != null) {
            String warning = Formatter.formatFileSize(context, info.warningLevel);
            this.mLimitPreference.setSummary(getString(info.limitLevel <= 0 ? R.string.cell_warning_only : R.string.cell_warning_and_limit, new Object[]{warning, Formatter.formatFileSize(context, info.limitLevel)}));
        }
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1; i < screen.getPreferenceCount(); i++) {
            ((TemplatePreferenceCategory) screen.getPreference(i)).pushTemplates(this.services);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 37;
    }

    public boolean hasEthernet(Context context) {
        long ethernetBytes;
        ConnectivityManager conn = ConnectivityManager.from(context);
        boolean hasEthernet = conn.isNetworkSupported(9);
        try {
            INetworkStatsSession statsSession = this.services.mStatsService.openSession();
            if (statsSession != null) {
                ethernetBytes = statsSession.getSummaryForNetwork(NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE).getTotalBytes();
                TrafficStats.closeQuietly(statsSession);
            } else {
                ethernetBytes = 0;
            }
            return hasEthernet && ethernetBytes > 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMobileData(Context context) {
        return ConnectivityManager.from(context).isNetworkSupported(0);
    }

    public static boolean hasWifiRadio(Context context) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(1);
    }

    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager subManager = SubscriptionManager.from(context);
        if (subManager == null) {
            return -1;
        }
        SubscriptionInfo subscriptionInfo = subManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = subManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return -1;
            }
            subscriptionInfo = list.get(0);
        }
        Log.d("DataUsageSummary", "getDefaultSubscriptionId = " + subscriptionInfo);
        return subscriptionInfo.getSubscriptionId();
    }

    public static NetworkTemplate getDefaultTemplate(Context context, int defaultSubId) {
        if (hasMobileData(context) && defaultSubId != -1) {
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(telephonyManager.getSubscriberId(defaultSubId));
            return NetworkTemplate.normalize(mobileAll, telephonyManager.getMergedSubscriberIds());
        }
        if (hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
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
        public void setListening(boolean listening) {
            String used;
            if (!listening) {
                return;
            }
            DataUsageController.DataUsageInfo info = this.mDataController.getDataUsageInfo();
            if (info == null) {
                used = Formatter.formatFileSize(this.mActivity, 0L);
            } else if (info.limitLevel <= 0) {
                used = Formatter.formatFileSize(this.mActivity, info.usageLevel);
            } else {
                used = Utils.formatPercentage(info.usageLevel, info.limitLevel);
            }
            this.mSummaryLoader.setSummary(this, this.mActivity.getString(R.string.data_usage_summary_format, new Object[]{used}));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem cellularDataControl = menu.findItem(R.id.data_usage_menu_cellular_data_control);
        if (cellularDataControl == null) {
            return;
        }
        cellularDataControl.setVisible(CtaUtils.isCtaSupported());
    }
}
