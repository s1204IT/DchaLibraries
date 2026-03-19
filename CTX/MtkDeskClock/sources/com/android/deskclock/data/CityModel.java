package com.android.deskclock.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.ArraySet;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.settings.SettingsActivity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class CityModel {
    private List<City> mAllCities;
    private final List<CityListener> mCityListeners = new ArrayList();
    private Map<String, City> mCityMap;
    private final Context mContext;
    private City mHomeCity;
    private final BroadcastReceiver mLocaleChangedReceiver;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener;
    private final SharedPreferences mPrefs;
    private List<City> mSelectedCities;
    private final SettingsModel mSettingsModel;
    private List<City> mUnselectedCities;

    CityModel(Context context, SharedPreferences sharedPreferences, SettingsModel settingsModel) {
        this.mPreferenceListener = new PreferenceListener();
        this.mLocaleChangedReceiver = new LocaleChangedReceiver();
        this.mContext = context;
        this.mPrefs = sharedPreferences;
        this.mSettingsModel = settingsModel;
        this.mContext.registerReceiver(this.mLocaleChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        sharedPreferences.registerOnSharedPreferenceChangeListener(this.mPreferenceListener);
    }

    void addCityListener(CityListener cityListener) {
        this.mCityListeners.add(cityListener);
    }

    void removeCityListener(CityListener cityListener) {
        this.mCityListeners.remove(cityListener);
    }

    List<City> getAllCities() {
        if (this.mAllCities == null) {
            ArrayList arrayList = new ArrayList(getSelectedCities());
            Collections.sort(arrayList, new City.NameComparator());
            ArrayList arrayList2 = new ArrayList(getCityMap().size());
            arrayList2.addAll(arrayList);
            arrayList2.addAll(getUnselectedCities());
            this.mAllCities = Collections.unmodifiableList(arrayList2);
        }
        return this.mAllCities;
    }

    City getHomeCity() {
        if (this.mHomeCity == null) {
            String string = this.mContext.getString(R.string.home_label);
            this.mHomeCity = new City(null, -1, null, string, string, this.mSettingsModel.getHomeTimeZone());
        }
        return this.mHomeCity;
    }

    List<City> getUnselectedCities() {
        if (this.mUnselectedCities == null) {
            ArraySet arraySetNewArraySet = Utils.newArraySet(new ArrayList(getSelectedCities()));
            Collection<City> collectionValues = getCityMap().values();
            ArrayList arrayList = new ArrayList(collectionValues.size() - arraySetNewArraySet.size());
            for (City city : collectionValues) {
                if (!arraySetNewArraySet.contains(city)) {
                    arrayList.add(city);
                }
            }
            Collections.sort(arrayList, getCitySortComparator());
            this.mUnselectedCities = Collections.unmodifiableList(arrayList);
        }
        return this.mUnselectedCities;
    }

    List<City> getSelectedCities() {
        if (this.mSelectedCities == null) {
            List<City> selectedCities = CityDAO.getSelectedCities(this.mPrefs, getCityMap());
            Collections.sort(selectedCities, new City.UtcOffsetComparator());
            this.mSelectedCities = Collections.unmodifiableList(selectedCities);
        }
        return this.mSelectedCities;
    }

    void setSelectedCities(Collection<City> collection) {
        List<City> allCities = getAllCities();
        CityDAO.setSelectedCities(this.mPrefs, collection);
        this.mAllCities = null;
        this.mSelectedCities = null;
        this.mUnselectedCities = null;
        fireCitiesChanged(allCities, getAllCities());
    }

    Comparator<City> getCityIndexComparator() {
        DataModel.CitySort citySort = this.mSettingsModel.getCitySort();
        switch (citySort) {
            case NAME:
                return new City.NameIndexComparator();
            case UTC_OFFSET:
                return new City.UtcOffsetIndexComparator();
            default:
                throw new IllegalStateException("unexpected city sort: " + citySort);
        }
    }

    DataModel.CitySort getCitySort() {
        return this.mSettingsModel.getCitySort();
    }

    void toggleCitySort() {
        this.mSettingsModel.toggleCitySort();
        this.mAllCities = null;
        this.mUnselectedCities = null;
    }

    private Map<String, City> getCityMap() {
        if (this.mCityMap == null) {
            this.mCityMap = CityDAO.getCities(this.mContext);
        }
        return this.mCityMap;
    }

    private Comparator<City> getCitySortComparator() {
        DataModel.CitySort citySort = this.mSettingsModel.getCitySort();
        switch (citySort) {
            case NAME:
                return new City.NameComparator();
            case UTC_OFFSET:
                return new City.UtcOffsetComparator();
            default:
                throw new IllegalStateException("unexpected city sort: " + citySort);
        }
    }

    private void fireCitiesChanged(List<City> list, List<City> list2) {
        Intent intent = new Intent(DataModel.ACTION_WORLD_CITIES_CHANGED);
        intent.addFlags(16777216);
        this.mContext.sendBroadcast(intent);
        Iterator<CityListener> it = this.mCityListeners.iterator();
        while (it.hasNext()) {
            it.next().citiesChanged(list, list2);
        }
    }

    private final class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            CityModel.this.mCityMap = null;
            CityModel.this.mHomeCity = null;
            CityModel.this.mAllCities = null;
            CityModel.this.mSelectedCities = null;
            CityModel.this.mUnselectedCities = null;
        }
    }

    private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private PreferenceListener() {
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            byte b;
            int iHashCode = str.hashCode();
            if (iHashCode != -1778812094) {
                b = (iHashCode == -616896898 && str.equals(SettingsActivity.KEY_HOME_TZ)) ? (byte) 0 : (byte) -1;
            } else if (str.equals(SettingsActivity.KEY_AUTO_HOME_CLOCK)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    CityModel.this.mHomeCity = null;
                    break;
                case 1:
                    break;
                default:
                    return;
            }
            List<City> allCities = CityModel.this.getAllCities();
            CityModel.this.fireCitiesChanged(allCities, allCities);
        }
    }
}
