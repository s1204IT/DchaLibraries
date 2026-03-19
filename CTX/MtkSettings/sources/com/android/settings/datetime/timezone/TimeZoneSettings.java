package com.android.settings.datetime.timezone;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.datetime.timezone.TimeZoneInfo;
import com.android.settings.datetime.timezone.model.FilteredCountryTimeZones;
import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.datetime.timezone.model.TimeZoneDataLoader;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class TimeZoneSettings extends DashboardFragment {
    private Locale mLocale;
    private boolean mSelectByRegion;
    private String mSelectedTimeZoneId;
    private TimeZoneData mTimeZoneData;
    private TimeZoneInfo.Formatter mTimeZoneInfoFormatter;

    @Override
    public int getMetricsCategory() {
        return 515;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.time_zone_prefs;
    }

    @Override
    protected String getLogTag() {
        return "TimeZoneSettings";
    }

    @Override
    public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mLocale = context.getResources().getConfiguration().getLocales().get(0);
        this.mTimeZoneInfoFormatter = new TimeZoneInfo.Formatter(this.mLocale, new Date());
        ArrayList arrayList = new ArrayList();
        RegionPreferenceController regionPreferenceController = new RegionPreferenceController(context);
        regionPreferenceController.setOnClickListener(new OnPreferenceClickListener() {
            @Override
            public final void onClick() {
                this.f$0.startRegionPicker();
            }
        });
        RegionZonePreferenceController regionZonePreferenceController = new RegionZonePreferenceController(context);
        regionZonePreferenceController.setOnClickListener(new OnPreferenceClickListener() {
            @Override
            public final void onClick() {
                this.f$0.onRegionZonePreferenceClicked();
            }
        });
        FixedOffsetPreferenceController fixedOffsetPreferenceController = new FixedOffsetPreferenceController(context);
        fixedOffsetPreferenceController.setOnClickListener(new OnPreferenceClickListener() {
            @Override
            public final void onClick() {
                this.f$0.startFixedOffsetPicker();
            }
        });
        arrayList.add(regionPreferenceController);
        arrayList.add(regionZonePreferenceController);
        arrayList.add(fixedOffsetPreferenceController);
        return arrayList;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setPreferenceCategoryVisible((PreferenceCategory) findPreference("time_zone_region_preference_category"), false);
        setPreferenceCategoryVisible((PreferenceCategory) findPreference("time_zone_fixed_offset_preference_category"), false);
        getLoaderManager().initLoader(0, null, new TimeZoneDataLoader.LoaderCreator(getContext(), new TimeZoneDataLoader.OnDataReadyCallback() {
            @Override
            public final void onTimeZoneDataReady(TimeZoneData timeZoneData) {
                this.f$0.onTimeZoneDataReady(timeZoneData);
            }
        }));
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i2 != -1 || intent == null) {
            return;
        }
        switch (i) {
            case 1:
            case 2:
                String stringExtra = intent.getStringExtra("com.android.settings.datetime.timezone.result_region_id");
                String stringExtra2 = intent.getStringExtra("com.android.settings.datetime.timezone.result_time_zone_id");
                if (!Objects.equals(stringExtra, ((RegionPreferenceController) use(RegionPreferenceController.class)).getRegionId()) || !Objects.equals(stringExtra2, this.mSelectedTimeZoneId)) {
                    onRegionZoneChanged(stringExtra, stringExtra2);
                }
                break;
            case 3:
                String stringExtra3 = intent.getStringExtra("com.android.settings.datetime.timezone.result_time_zone_id");
                if (stringExtra3 != null && !stringExtra3.equals(this.mSelectedTimeZoneId)) {
                    onFixedOffsetZoneChanged(stringExtra3);
                    break;
                }
                break;
        }
    }

    void setTimeZoneData(TimeZoneData timeZoneData) {
        this.mTimeZoneData = timeZoneData;
    }

    private void onTimeZoneDataReady(TimeZoneData timeZoneData) {
        if (this.mTimeZoneData == null && timeZoneData != null) {
            this.mTimeZoneData = timeZoneData;
            setupForCurrentTimeZone();
            getActivity().invalidateOptionsMenu();
        }
    }

    private void startRegionPicker() {
        startPickerFragment(RegionSearchPicker.class, new Bundle(), 1);
    }

    private void onRegionZonePreferenceClicked() {
        Bundle bundle = new Bundle();
        bundle.putString("com.android.settings.datetime.timezone.region_id", ((RegionPreferenceController) use(RegionPreferenceController.class)).getRegionId());
        startPickerFragment(RegionZonePicker.class, bundle, 2);
    }

    private void startFixedOffsetPicker() {
        startPickerFragment(FixedOffsetPicker.class, new Bundle(), 3);
    }

    private void startPickerFragment(Class<? extends BaseTimeZonePicker> cls, Bundle bundle, int i) {
        new SubSettingLauncher(getContext()).setDestination(cls.getCanonicalName()).setArguments(bundle).setSourceMetricsCategory(getMetricsCategory()).setResultListener(this, i).launch();
    }

    private void setDisplayedRegion(String str) {
        ((RegionPreferenceController) use(RegionPreferenceController.class)).setRegionId(str);
        updatePreferenceStates();
    }

    private void setDisplayedTimeZoneInfo(String str, String str2) {
        TimeZoneInfo timeZoneInfo = str2 == null ? null : this.mTimeZoneInfoFormatter.format(str2);
        FilteredCountryTimeZones filteredCountryTimeZonesLookupCountryTimeZones = this.mTimeZoneData.lookupCountryTimeZones(str);
        ((RegionZonePreferenceController) use(RegionZonePreferenceController.class)).setTimeZoneInfo(timeZoneInfo);
        RegionZonePreferenceController regionZonePreferenceController = (RegionZonePreferenceController) use(RegionZonePreferenceController.class);
        boolean z = true;
        if (timeZoneInfo != null && (filteredCountryTimeZonesLookupCountryTimeZones == null || filteredCountryTimeZonesLookupCountryTimeZones.getTimeZoneIds().size() <= 1)) {
            z = false;
        }
        regionZonePreferenceController.setClickable(z);
        ((TimeZoneInfoPreferenceController) use(TimeZoneInfoPreferenceController.class)).setTimeZoneInfo(timeZoneInfo);
        updatePreferenceStates();
    }

    private void setDisplayedFixedOffsetTimeZoneInfo(String str) {
        if (isFixedOffset(str)) {
            ((FixedOffsetPreferenceController) use(FixedOffsetPreferenceController.class)).setTimeZoneInfo(this.mTimeZoneInfoFormatter.format(str));
        } else {
            ((FixedOffsetPreferenceController) use(FixedOffsetPreferenceController.class)).setTimeZoneInfo(null);
        }
        updatePreferenceStates();
    }

    private void onRegionZoneChanged(String str, String str2) {
        FilteredCountryTimeZones filteredCountryTimeZonesLookupCountryTimeZones = this.mTimeZoneData.lookupCountryTimeZones(str);
        if (filteredCountryTimeZonesLookupCountryTimeZones == null || !filteredCountryTimeZonesLookupCountryTimeZones.getTimeZoneIds().contains(str2)) {
            Log.e("TimeZoneSettings", "Unknown time zone id is selected: " + str2);
            return;
        }
        this.mSelectedTimeZoneId = str2;
        setDisplayedRegion(str);
        setDisplayedTimeZoneInfo(str, this.mSelectedTimeZoneId);
        saveTimeZone(str, this.mSelectedTimeZoneId);
        setSelectByRegion(true);
    }

    private void onFixedOffsetZoneChanged(String str) {
        this.mSelectedTimeZoneId = str;
        setDisplayedFixedOffsetTimeZoneInfo(str);
        saveTimeZone(null, this.mSelectedTimeZoneId);
        setSelectByRegion(false);
    }

    private void saveTimeZone(String str, String str2) {
        SharedPreferences.Editor editorEdit = getPreferenceManager().getSharedPreferences().edit();
        if (str == null) {
            editorEdit.remove("time_zone_region");
        } else {
            editorEdit.putString("time_zone_region", str);
        }
        editorEdit.apply();
        ((AlarmManager) getActivity().getSystemService(AlarmManager.class)).setTimeZone(str2);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 1, 0, R.string.zone_menu_by_region);
        menu.add(0, 2, 0, R.string.zone_menu_by_offset);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(1).setVisible((this.mTimeZoneData == null || this.mSelectByRegion) ? false : true);
        menu.findItem(2).setVisible(this.mTimeZoneData != null && this.mSelectByRegion);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                startRegionPicker();
                break;
            case 2:
                startFixedOffsetPicker();
                break;
        }
        return true;
    }

    private void setupForCurrentTimeZone() {
        this.mSelectedTimeZoneId = TimeZone.getDefault().getID();
        setSelectByRegion(!isFixedOffset(this.mSelectedTimeZoneId));
    }

    private static boolean isFixedOffset(String str) {
        return str.startsWith("Etc/GMT") || str.equals("Etc/UTC");
    }

    private void setSelectByRegion(boolean z) {
        this.mSelectByRegion = z;
        setPreferenceCategoryVisible((PreferenceCategory) findPreference("time_zone_region_preference_category"), z);
        setPreferenceCategoryVisible((PreferenceCategory) findPreference("time_zone_fixed_offset_preference_category"), !z);
        String localeRegionId = getLocaleRegionId();
        if (!this.mTimeZoneData.getRegionIds().contains(localeRegionId)) {
            localeRegionId = null;
        }
        setDisplayedRegion(localeRegionId);
        setDisplayedTimeZoneInfo(localeRegionId, null);
        if (!this.mSelectByRegion) {
            setDisplayedFixedOffsetTimeZoneInfo(this.mSelectedTimeZoneId);
            return;
        }
        String strFindRegionIdForTzId = findRegionIdForTzId(this.mSelectedTimeZoneId);
        if (strFindRegionIdForTzId != null) {
            setDisplayedRegion(strFindRegionIdForTzId);
            setDisplayedTimeZoneInfo(strFindRegionIdForTzId, this.mSelectedTimeZoneId);
        }
    }

    private String findRegionIdForTzId(String str) {
        return findRegionIdForTzId(str, getPreferenceManager().getSharedPreferences().getString("time_zone_region", null), getLocaleRegionId());
    }

    String findRegionIdForTzId(String str, String str2, String str3) {
        Set<String> setLookupCountryCodesForZoneId = this.mTimeZoneData.lookupCountryCodesForZoneId(str);
        if (setLookupCountryCodesForZoneId.size() == 0) {
            return null;
        }
        if (str2 != null && setLookupCountryCodesForZoneId.contains(str2)) {
            return str2;
        }
        if (str3 != null && setLookupCountryCodesForZoneId.contains(str3)) {
            return str3;
        }
        return ((String[]) setLookupCountryCodesForZoneId.toArray(new String[setLookupCountryCodesForZoneId.size()]))[0];
    }

    private void setPreferenceCategoryVisible(PreferenceCategory preferenceCategory, boolean z) {
        preferenceCategory.setVisible(z);
        for (int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
            preferenceCategory.getPreference(i).setVisible(z);
        }
    }

    private String getLocaleRegionId() {
        return this.mLocale.getCountry().toUpperCase(Locale.US);
    }
}
