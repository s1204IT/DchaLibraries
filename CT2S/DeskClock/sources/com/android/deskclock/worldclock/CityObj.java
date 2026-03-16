package com.android.deskclock.worldclock;

import android.content.SharedPreferences;

public class CityObj {
    public boolean isHeader;
    public String mCityId;
    public String mCityName;
    public String mTimeZone;

    public CityObj(String name, String timezone, String id) {
        this.mCityName = name;
        this.mTimeZone = timezone;
        this.mCityId = id;
    }

    public String toString() {
        return "CityObj{name=" + this.mCityName + ", timezone=" + this.mTimeZone + ", id=" + this.mCityId + '}';
    }

    public CityObj(SharedPreferences prefs, int index) {
        this.mCityName = prefs.getString("city_name_" + index, null);
        this.mTimeZone = prefs.getString("city_tz_" + index, null);
        this.mCityId = prefs.getString("city_id_" + index, null);
    }

    public void saveCityToSharedPrefs(SharedPreferences.Editor editor, int index) {
        editor.putString("city_name_" + index, this.mCityName);
        editor.putString("city_tz_" + index, this.mTimeZone);
        editor.putString("city_id_" + index, this.mCityId);
    }
}
