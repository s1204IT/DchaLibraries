package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.LocationManager;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.util.Log;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.datetime.AutoTimeFormatPreferenceController;
import com.android.settings.datetime.AutoTimePreferenceController;
import com.android.settings.datetime.AutoTimeZonePreferenceController;
import com.android.settings.datetime.DatePreferenceController;
import com.android.settings.datetime.TimeChangeListenerMixin;
import com.android.settings.datetime.TimeFormatPreferenceController;
import com.android.settings.datetime.TimePreferenceController;
import com.android.settings.datetime.TimeZonePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datetime.ZoneGetter;
import com.mediatek.settings.datetime.AutoTimeExtPreferenceController;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/* loaded from: classes.dex */
public class DateTimeSettings extends DashboardFragment implements DialogInterface.OnCancelListener, DatePreferenceController.DatePreferenceHost, TimePreferenceController.TimePreferenceHost, AutoTimeExtPreferenceController.GPSPreferenceHost {
    private boolean isGPSSupport;
    private LocationManager mLocationManager = null;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.DateTimeSettings.1
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new DateTimeSearchIndexProvider();

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 38;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "DateTimeSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        Log.d("DateTimeSettings", "getPreferenceScreenResId, isGPSSupport= " + this.isGPSSupport);
        if (this.isGPSSupport) {
            return R.xml.date_time_ext_prefs;
        }
        return R.xml.date_time_prefs;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        this.mLocationManager = (LocationManager) getSystemService("location");
        this.isGPSSupport = this.mLocationManager.getProvider("gps") != null;
        super.onAttach(context);
        getLifecycle().addObserver(new TimeChangeListenerMixin(context, this));
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        AutoTimePreferenceController autoTimePreferenceController;
        ArrayList arrayList = new ArrayList();
        Activity activity = getActivity();
        boolean booleanExtra = activity.getIntent().getBooleanExtra("firstRun", false);
        AutoTimeZonePreferenceController autoTimeZonePreferenceController = new AutoTimeZonePreferenceController(activity, this, booleanExtra);
        if (this.isGPSSupport) {
            autoTimePreferenceController = new AutoTimeExtPreferenceController(activity, this);
        } else {
            autoTimePreferenceController = new AutoTimePreferenceController(activity, this);
        }
        AutoTimeFormatPreferenceController autoTimeFormatPreferenceController = new AutoTimeFormatPreferenceController(activity, this);
        arrayList.add(autoTimeZonePreferenceController);
        arrayList.add(autoTimePreferenceController);
        arrayList.add(autoTimeFormatPreferenceController);
        arrayList.add(new TimeFormatPreferenceController(activity, this, booleanExtra));
        arrayList.add(new TimeZonePreferenceController(activity, autoTimeZonePreferenceController));
        arrayList.add(new TimePreferenceController(activity, this, autoTimePreferenceController));
        arrayList.add(new DatePreferenceController(activity, this, autoTimePreferenceController));
        return arrayList;
    }

    @Override // com.android.settings.datetime.UpdateTimeAndDateCallback
    public void updateTimeAndDateDisplay(Context context) {
        updatePreferenceStates();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        switch (i) {
            case 0:
                return ((DatePreferenceController) use(DatePreferenceController.class)).buildDatePicker(getActivity());
            case 1:
                return ((TimePreferenceController) use(TimePreferenceController.class)).buildTimePicker(getActivity());
            case 2:
                return ((AutoTimeExtPreferenceController) use(AutoTimeExtPreferenceController.class)).buildGPSConfirmDialog(getActivity());
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        switch (i) {
            case 0:
                return 607;
            case 1:
                return 608;
            case 2:
                return 38;
            default:
                return 0;
        }
    }

    @Override // com.android.settings.datetime.TimePreferenceController.TimePreferenceHost
    public void showTimePicker() {
        removeDialog(1);
        showDialog(1);
    }

    @Override // com.android.settings.datetime.DatePreferenceController.DatePreferenceHost
    public void showDatePicker() {
        showDialog(0);
    }

    @Override // com.mediatek.settings.datetime.AutoTimeExtPreferenceController.GPSPreferenceHost
    public void showGPSConfirmDialog() {
        removeDialog(2);
        showDialog(2);
        setOnCancelListener(this);
    }

    @Override // android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialogInterface) {
        if (this.isGPSSupport) {
            Log.d("DateTimeSettings", "onCancel Dialog, Reset AutoTime Settings");
            ((AutoTimeExtPreferenceController) use(AutoTimeExtPreferenceController.class)).reSetAutoTimePref();
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                Calendar calendar = Calendar.getInstance();
                this.mSummaryLoader.setSummary(this, ZoneGetter.getTimeZoneOffsetAndName(this.mContext, calendar.getTimeZone(), calendar.getTime()));
            }
        }
    }

    private static class DateTimeSearchIndexProvider extends BaseSearchIndexProvider {
        private DateTimeSearchIndexProvider() {
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            if (UserManager.isDeviceInDemoMode(context)) {
                return arrayList;
            }
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.date_time_prefs;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    }
}
