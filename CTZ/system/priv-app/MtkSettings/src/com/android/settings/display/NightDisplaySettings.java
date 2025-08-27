package com.android.settings.display;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class NightDisplaySettings extends DashboardFragment implements ColorDisplayController.Callback, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.display.NightDisplaySettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.night_display_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        protected boolean isPageSearchEnabled(Context context) {
            return ColorDisplayController.isAvailable(context);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return NightDisplaySettings.buildPreferenceControllers(context);
        }
    };
    private ColorDisplayController mController;

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mController = new ColorDisplayController(getContext());
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
        this.mController.setListener(this);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        super.onStop();
        this.mController.setListener((ColorDisplayController.Callback) null);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if ("night_display_end_time".equals(preference.getKey())) {
            showDialog(1);
            return true;
        }
        if ("night_display_start_time".equals(preference.getKey())) {
            showDialog(0);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(final int i) {
        LocalTime customEndTime;
        if (i == 0 || i == 1) {
            if (i == 0) {
                customEndTime = this.mController.getCustomStartTime();
            } else {
                customEndTime = this.mController.getCustomEndTime();
            }
            Context context = getContext();
            return new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() { // from class: com.android.settings.display.-$$Lambda$NightDisplaySettings$EHQrigX4B__bQ2Ww7B-DCA-KncQ
                @Override // android.app.TimePickerDialog.OnTimeSetListener
                public final void onTimeSet(TimePicker timePicker, int i2, int i3) {
                    NightDisplaySettings.lambda$onCreateDialog$0(this.f$0, i, timePicker, i2, i3);
                }
            }, customEndTime.getHour(), customEndTime.getMinute(), DateFormat.is24HourFormat(context));
        }
        return super.onCreateDialog(i);
    }

    public static /* synthetic */ void lambda$onCreateDialog$0(NightDisplaySettings nightDisplaySettings, int i, TimePicker timePicker, int i2, int i3) {
        LocalTime localTimeOf = LocalTime.of(i2, i3);
        if (i == 0) {
            nightDisplaySettings.mController.setCustomStartTime(localTimeOf);
        } else {
            nightDisplaySettings.mController.setCustomEndTime(localTimeOf);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        switch (i) {
            case 0:
                return 588;
            case 1:
                return 589;
            default:
                return 0;
        }
    }

    public void onActivated(boolean z) {
        updatePreferenceStates();
    }

    public void onAutoModeChanged(int i) {
        updatePreferenceStates();
    }

    public void onColorTemperatureChanged(int i) {
        updatePreferenceStates();
    }

    public void onCustomStartTimeChanged(LocalTime localTime) {
        updatePreferenceStates();
    }

    public void onCustomEndTimeChanged(LocalTime localTime) {
        updatePreferenceStates();
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.night_display_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 488;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_night_display;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "NightDisplaySettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList(1);
        arrayList.add(new NightDisplayFooterPreferenceController(context));
        return arrayList;
    }
}
