package com.android.deskclock.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.deskclock.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CityDAO {
    private static final String CITY_ID = "city_id_";
    private static final String NUMBER_OF_CITIES = "number_of_cities";
    private static final Pattern NUMERIC_INDEX_REGEX = Pattern.compile("\\d+");

    private CityDAO() {
    }

    static List<City> getSelectedCities(SharedPreferences sharedPreferences, Map<String, City> map) {
        int i = sharedPreferences.getInt(NUMBER_OF_CITIES, 0);
        ArrayList arrayList = new ArrayList(i);
        for (int i2 = 0; i2 < i; i2++) {
            City city = map.get(sharedPreferences.getString(CITY_ID + i2, null));
            if (city != null) {
                arrayList.add(city);
            }
        }
        return arrayList;
    }

    static void setSelectedCities(SharedPreferences sharedPreferences, Collection<City> collection) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putInt(NUMBER_OF_CITIES, collection.size());
        Iterator<City> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            editorEdit.putString(CITY_ID + i, it.next().getId());
            i++;
        }
        editorEdit.apply();
    }

    static Map<String, City> getCities(Context context) {
        Resources resources = context.getResources();
        TypedArray typedArrayObtainTypedArray = resources.obtainTypedArray(R.array.city_ids);
        int length = typedArrayObtainTypedArray.length();
        ArrayMap arrayMap = new ArrayMap(length);
        for (int i = 0; i < length; i++) {
            try {
                int resourceId = typedArrayObtainTypedArray.getResourceId(i, 0);
                if (resourceId == 0) {
                    throw new IllegalStateException(String.format(Locale.ENGLISH, "Unable to locate city resource id for index %d", Integer.valueOf(i)));
                }
                String resourceEntryName = resources.getResourceEntryName(resourceId);
                String string = typedArrayObtainTypedArray.getString(i);
                if (string == null) {
                    throw new IllegalStateException(String.format("Unable to locate city with id %s", resourceEntryName));
                }
                String[] strArrSplit = string.split("[|]");
                if (strArrSplit.length == 2) {
                    City cityCreateCity = createCity(resourceEntryName, strArrSplit[0], strArrSplit[1]);
                    if (cityCreateCity != null) {
                        arrayMap.put(resourceEntryName, cityCreateCity);
                    }
                } else {
                    throw new IllegalStateException(String.format("Error parsing malformed city %s", string));
                }
            } catch (Throwable th) {
                typedArrayObtainTypedArray.recycle();
                throw th;
            }
        }
        typedArrayObtainTypedArray.recycle();
        return Collections.unmodifiableMap(arrayMap);
    }

    @VisibleForTesting
    static City createCity(String str, String str2, String str3) {
        TimeZone timeZone = TimeZone.getTimeZone(str3);
        if ("GMT".equals(timeZone.getID())) {
            return null;
        }
        String[] strArrSplit = str2.split("[=:]");
        String str4 = strArrSplit[1];
        String strSubstring = TextUtils.isEmpty(strArrSplit[0]) ? str4.substring(0, 1) : strArrSplit[0];
        String str5 = strArrSplit.length == 3 ? strArrSplit[2] : str4;
        Matcher matcher = NUMERIC_INDEX_REGEX.matcher(strSubstring);
        return new City(str, matcher.find() ? Integer.parseInt(matcher.group()) : -1, strSubstring, str4, str5, timeZone);
    }
}
