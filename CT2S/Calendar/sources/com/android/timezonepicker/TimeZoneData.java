package com.android.timezonepicker;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneData {
    public static boolean is24HourFormat;
    private static String[] mBackupCountryCodes;
    private static Locale mBackupCountryLocale;
    private static String[] mBackupCountryNames;
    private String mAlternateDefaultTimeZoneId;
    private Context mContext;
    private String mDefaultTimeZoneCountry;
    public String mDefaultTimeZoneId;
    private TimeZoneInfo mDefaultTimeZoneInfo;
    private String mPalestineDisplayName;
    private long mTimeMillis;
    ArrayList<TimeZoneInfo> mTimeZones;
    LinkedHashMap<String, ArrayList<Integer>> mTimeZonesByCountry;
    private HashMap<String, TimeZoneInfo> mTimeZonesById;
    SparseArray<ArrayList<Integer>> mTimeZonesByOffsets;
    HashSet<String> mTimeZoneNames = new HashSet<>();
    private HashMap<String, String> mCountryCodeToNameMap = new HashMap<>();
    private boolean[] mHasTimeZonesInHrOffset = new boolean[40];

    public TimeZoneData(Context context, String defaultTimeZoneId, long timeMillis) {
        this.mContext = context;
        boolean zIs24HourFormat = DateFormat.is24HourFormat(context);
        TimeZoneInfo.is24HourFormat = zIs24HourFormat;
        is24HourFormat = zIs24HourFormat;
        this.mAlternateDefaultTimeZoneId = defaultTimeZoneId;
        this.mDefaultTimeZoneId = defaultTimeZoneId;
        long now = System.currentTimeMillis();
        if (timeMillis == 0) {
            this.mTimeMillis = now;
        } else {
            this.mTimeMillis = timeMillis;
        }
        this.mPalestineDisplayName = context.getResources().getString(R.string.palestine_display_name);
        loadTzs(context);
        Log.i("TimeZoneData", "Time to load time zones (ms): " + (System.currentTimeMillis() - now));
    }

    public TimeZoneInfo get(int position) {
        return this.mTimeZones.get(position);
    }

    public int size() {
        return this.mTimeZones.size();
    }

    public int getDefaultTimeZoneIndex() {
        return this.mTimeZones.indexOf(this.mDefaultTimeZoneInfo);
    }

    public int findIndexByTimeZoneIdSlow(String timeZoneId) {
        int idx = 0;
        for (TimeZoneInfo tzi : this.mTimeZones) {
            if (!timeZoneId.equals(tzi.mTzId)) {
                idx++;
            } else {
                return idx;
            }
        }
        return -1;
    }

    void loadTzs(Context context) {
        this.mTimeZones = new ArrayList<>();
        HashSet<String> processedTimeZones = loadTzsInZoneTab(context);
        String[] tzIds = TimeZone.getAvailableIDs();
        for (String tzId : tzIds) {
            if (!processedTimeZones.contains(tzId) && tzId.startsWith("Etc/GMT")) {
                TimeZone tz = TimeZone.getTimeZone(tzId);
                if (tz == null) {
                    Log.e("TimeZoneData", "Timezone not found: " + tzId);
                } else {
                    TimeZoneInfo tzInfo = new TimeZoneInfo(tz, null);
                    if (getIdenticalTimeZoneInTheCountry(tzInfo) == -1) {
                        this.mTimeZones.add(tzInfo);
                    }
                }
            }
        }
        Collections.sort(this.mTimeZones);
        this.mTimeZonesByCountry = new LinkedHashMap<>();
        this.mTimeZonesByOffsets = new SparseArray<>(this.mHasTimeZonesInHrOffset.length);
        this.mTimeZonesById = new HashMap<>(this.mTimeZones.size());
        for (TimeZoneInfo tz2 : this.mTimeZones) {
            this.mTimeZonesById.put(tz2.mTzId, tz2);
        }
        populateDisplayNameOverrides(this.mContext.getResources());
        Date date = new Date(this.mTimeMillis);
        Locale defaultLocal = Locale.getDefault();
        int idx = 0;
        for (TimeZoneInfo tz3 : this.mTimeZones) {
            if (tz3.mDisplayName == null) {
                tz3.mDisplayName = tz3.mTz.getDisplayName(tz3.mTz.inDaylightTime(date), 1, defaultLocal);
            }
            ArrayList<Integer> group = this.mTimeZonesByCountry.get(tz3.mCountry);
            if (group == null) {
                group = new ArrayList<>();
                this.mTimeZonesByCountry.put(tz3.mCountry, group);
            }
            group.add(Integer.valueOf(idx));
            indexByOffsets(idx, tz3);
            if (!tz3.mDisplayName.endsWith(":00")) {
                this.mTimeZoneNames.add(tz3.mDisplayName);
            }
            idx++;
        }
    }

    private void populateDisplayNameOverrides(Resources resources) {
        String[] ids = resources.getStringArray(R.array.timezone_rename_ids);
        String[] labels = resources.getStringArray(R.array.timezone_rename_labels);
        int length = ids.length;
        if (ids.length != labels.length) {
            Log.e("TimeZoneData", "timezone_rename_ids len=" + ids.length + " timezone_rename_labels len=" + labels.length);
            length = Math.min(ids.length, labels.length);
        }
        for (int i = 0; i < length; i++) {
            TimeZoneInfo tzi = this.mTimeZonesById.get(ids[i]);
            if (tzi != null) {
                tzi.mDisplayName = labels[i];
            } else {
                Log.e("TimeZoneData", "Could not find timezone with label: " + labels[i]);
            }
        }
    }

    public boolean hasTimeZonesInHrOffset(int offsetHr) {
        int index = offsetHr + 20;
        if (index >= this.mHasTimeZonesInHrOffset.length || index < 0) {
            return false;
        }
        return this.mHasTimeZonesInHrOffset[index];
    }

    private void indexByOffsets(int idx, TimeZoneInfo tzi) {
        int offsetMillis = tzi.getNowOffsetMillis();
        int index = ((int) (((long) offsetMillis) / 3600000)) + 20;
        this.mHasTimeZonesInHrOffset[index] = true;
        ArrayList<Integer> group = this.mTimeZonesByOffsets.get(index);
        if (group == null) {
            group = new ArrayList<>();
            this.mTimeZonesByOffsets.put(index, group);
        }
        group.add(Integer.valueOf(idx));
    }

    public ArrayList<Integer> getTimeZonesByOffset(int offsetHr) {
        int index = offsetHr + 20;
        if (index >= this.mHasTimeZonesInHrOffset.length || index < 0) {
            return null;
        }
        return this.mTimeZonesByOffsets.get(index);
    }

    private HashSet<String> loadTzsInZoneTab(Context context) {
        HashSet<String> processedTimeZones = new HashSet<>();
        AssetManager am = context.getAssets();
        InputStream is = null;
        try {
            try {
                is = am.open("backward");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (!line.startsWith("#") && line.length() > 0) {
                        String[] fields = line.split("\t+");
                        String newTzId = fields[1];
                        String oldTzId = fields[fields.length - 1];
                        if (TimeZone.getTimeZone(newTzId) == null) {
                            Log.e("TimeZoneData", "Timezone not found: " + newTzId);
                        } else {
                            processedTimeZones.add(oldTzId);
                            if (this.mDefaultTimeZoneId != null && this.mDefaultTimeZoneId.equals(oldTzId)) {
                                this.mAlternateDefaultTimeZoneId = newTzId;
                            }
                        }
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } catch (IOException e3) {
            Log.e("TimeZoneData", "Failed to read 'backward' file.");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                }
            }
        }
        try {
            try {
                String lang = Locale.getDefault().getLanguage();
                is = am.open("zone.tab");
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(is));
                while (true) {
                    String line2 = reader2.readLine();
                    if (line2 == null) {
                        break;
                    }
                    if (!line2.startsWith("#")) {
                        String[] fields2 = line2.split("\t");
                        String timeZoneId = fields2[2];
                        String countryCode = fields2[0];
                        TimeZone tz = TimeZone.getTimeZone(timeZoneId);
                        if (tz == null) {
                            Log.e("TimeZoneData", "Timezone not found: " + timeZoneId);
                        } else if (countryCode == null && !timeZoneId.startsWith("Etc/GMT")) {
                            processedTimeZones.add(timeZoneId);
                        } else {
                            String country = this.mCountryCodeToNameMap.get(countryCode);
                            if (country == null) {
                                country = getCountryNames(lang, countryCode);
                                this.mCountryCodeToNameMap.put(countryCode, country);
                            }
                            if (this.mDefaultTimeZoneId != null && this.mDefaultTimeZoneCountry == null && timeZoneId.equals(this.mAlternateDefaultTimeZoneId)) {
                                this.mDefaultTimeZoneCountry = country;
                                TimeZone defaultTz = TimeZone.getTimeZone(this.mDefaultTimeZoneId);
                                if (defaultTz != null) {
                                    this.mDefaultTimeZoneInfo = new TimeZoneInfo(defaultTz, country);
                                    int tzToOverride = getIdenticalTimeZoneInTheCountry(this.mDefaultTimeZoneInfo);
                                    if (tzToOverride == -1) {
                                        this.mTimeZones.add(this.mDefaultTimeZoneInfo);
                                    } else {
                                        this.mTimeZones.add(tzToOverride, this.mDefaultTimeZoneInfo);
                                    }
                                }
                            }
                            TimeZoneInfo timeZoneInfo = new TimeZoneInfo(tz, country);
                            int identicalTzIdx = getIdenticalTimeZoneInTheCountry(timeZoneInfo);
                            if (identicalTzIdx == -1) {
                                this.mTimeZones.add(timeZoneInfo);
                            }
                            processedTimeZones.add(timeZoneId);
                        }
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (IOException e6) {
                Log.e("TimeZoneData", "Failed to read 'zone.tab'.");
            }
            return processedTimeZones;
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e7) {
                }
            }
            throw th;
        }
    }

    private String getCountryNames(String lang, String countryCode) {
        String countryDisplayName;
        Locale defaultLocale = Locale.getDefault();
        if ("PS".equalsIgnoreCase(countryCode)) {
            countryDisplayName = this.mPalestineDisplayName;
        } else {
            countryDisplayName = new Locale(lang, countryCode).getDisplayCountry(defaultLocale);
        }
        if (countryCode.equals(countryDisplayName)) {
            if (mBackupCountryCodes == null || !defaultLocale.equals(mBackupCountryLocale)) {
                mBackupCountryLocale = defaultLocale;
                mBackupCountryCodes = this.mContext.getResources().getStringArray(R.array.backup_country_codes);
                mBackupCountryNames = this.mContext.getResources().getStringArray(R.array.backup_country_names);
            }
            int length = Math.min(mBackupCountryCodes.length, mBackupCountryNames.length);
            for (int i = 0; i < length; i++) {
                if (mBackupCountryCodes[i].equals(countryCode)) {
                    String countryDisplayName2 = mBackupCountryNames[i];
                    return countryDisplayName2;
                }
            }
            return countryCode;
        }
        return countryDisplayName;
    }

    private int getIdenticalTimeZoneInTheCountry(TimeZoneInfo timeZoneInfo) {
        int idx = 0;
        for (TimeZoneInfo tzi : this.mTimeZones) {
            if (tzi.hasSameRules(timeZoneInfo)) {
                if (tzi.mCountry == null) {
                    if (timeZoneInfo.mCountry == null) {
                        return idx;
                    }
                } else if (tzi.mCountry.equals(timeZoneInfo.mCountry)) {
                    return idx;
                }
            }
            idx++;
        }
        return -1;
    }
}
