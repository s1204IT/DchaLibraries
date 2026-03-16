package com.android.deskclock.worldclock;

import android.content.SharedPreferences;
import java.util.Collection;
import java.util.HashMap;

public class Cities {
    public static void saveCitiesToSharedPrefs(SharedPreferences prefs, HashMap<String, CityObj> cities) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("number_of_cities", cities.size());
        Collection<CityObj> col = cities.values();
        int count = 0;
        for (CityObj c : col) {
            c.saveCityToSharedPrefs(editor, count);
            count++;
        }
        editor.apply();
    }

    public static HashMap<String, CityObj> readCitiesFromSharedPrefs(SharedPreferences prefs) {
        int size = prefs.getInt("number_of_cities", -1);
        HashMap<String, CityObj> c = new HashMap<>();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                CityObj o = new CityObj(prefs, i);
                if (o.mCityName != null && o.mTimeZone != null) {
                    c.put(o.mCityId, o);
                }
            }
        }
        return c;
    }
}
