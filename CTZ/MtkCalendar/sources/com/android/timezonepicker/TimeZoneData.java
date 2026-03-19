package com.android.timezonepicker;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    public TimeZoneData(Context context, String str, long j) throws Throwable {
        this.mContext = context;
        boolean zIs24HourFormat = DateFormat.is24HourFormat(context);
        TimeZoneInfo.is24HourFormat = zIs24HourFormat;
        is24HourFormat = zIs24HourFormat;
        this.mAlternateDefaultTimeZoneId = str;
        this.mDefaultTimeZoneId = str;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (j == 0) {
            this.mTimeMillis = jCurrentTimeMillis;
        } else {
            this.mTimeMillis = j;
        }
        this.mPalestineDisplayName = context.getResources().getString(R.string.palestine_display_name);
        loadTzs(context);
        Log.i("TimeZoneData", "Time to load time zones (ms): " + (System.currentTimeMillis() - jCurrentTimeMillis));
    }

    public TimeZoneInfo get(int i) {
        return this.mTimeZones.get(i);
    }

    public int size() {
        return this.mTimeZones.size();
    }

    public int getDefaultTimeZoneIndex() {
        return this.mTimeZones.indexOf(this.mDefaultTimeZoneInfo);
    }

    public int findIndexByTimeZoneIdSlow(String str) {
        Iterator<TimeZoneInfo> it = this.mTimeZones.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (str.equals(it.next().mTzId)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    void loadTzs(Context context) throws Throwable {
        this.mTimeZones = new ArrayList<>();
        HashSet<String> hashSetLoadTzsInZoneTab = loadTzsInZoneTab(context);
        int i = 0;
        for (String str : TimeZone.getAvailableIDs()) {
            if (!hashSetLoadTzsInZoneTab.contains(str) && str.startsWith("Etc/GMT")) {
                TimeZone timeZone = TimeZone.getTimeZone(str);
                if (timeZone == null) {
                    Log.e("TimeZoneData", "Timezone not found: " + str);
                } else {
                    TimeZoneInfo timeZoneInfo = new TimeZoneInfo(timeZone, null);
                    if (getIdenticalTimeZoneInTheCountry(timeZoneInfo) == -1) {
                        this.mTimeZones.add(timeZoneInfo);
                    }
                }
            }
        }
        Collections.sort(this.mTimeZones);
        this.mTimeZonesByCountry = new LinkedHashMap<>();
        this.mTimeZonesByOffsets = new SparseArray<>(this.mHasTimeZonesInHrOffset.length);
        this.mTimeZonesById = new HashMap<>(this.mTimeZones.size());
        for (TimeZoneInfo timeZoneInfo2 : this.mTimeZones) {
            this.mTimeZonesById.put(timeZoneInfo2.mTzId, timeZoneInfo2);
        }
        populateDisplayNameOverrides(this.mContext.getResources());
        Date date = new Date(this.mTimeMillis);
        Locale locale = Locale.getDefault();
        for (TimeZoneInfo timeZoneInfo3 : this.mTimeZones) {
            if (timeZoneInfo3.mDisplayName == null) {
                timeZoneInfo3.mDisplayName = timeZoneInfo3.mTz.getDisplayName(timeZoneInfo3.mTz.inDaylightTime(date), 1, locale);
            }
            ArrayList<Integer> arrayList = this.mTimeZonesByCountry.get(timeZoneInfo3.mCountry);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mTimeZonesByCountry.put(timeZoneInfo3.mCountry, arrayList);
            }
            arrayList.add(Integer.valueOf(i));
            indexByOffsets(i, timeZoneInfo3);
            if (!timeZoneInfo3.mDisplayName.endsWith(":00")) {
                this.mTimeZoneNames.add(timeZoneInfo3.mDisplayName);
            }
            i++;
        }
    }

    private void populateDisplayNameOverrides(Resources resources) {
        String[] stringArray = resources.getStringArray(R.array.timezone_rename_ids);
        String[] stringArray2 = resources.getStringArray(R.array.timezone_rename_labels);
        int length = stringArray.length;
        if (stringArray.length != stringArray2.length) {
            Log.e("TimeZoneData", "timezone_rename_ids len=" + stringArray.length + " timezone_rename_labels len=" + stringArray2.length);
            length = Math.min(stringArray.length, stringArray2.length);
        }
        for (int i = 0; i < length; i++) {
            TimeZoneInfo timeZoneInfo = this.mTimeZonesById.get(stringArray[i]);
            if (timeZoneInfo != null) {
                timeZoneInfo.mDisplayName = stringArray2[i];
            } else {
                Log.e("TimeZoneData", "Could not find timezone with label: " + stringArray2[i]);
            }
        }
    }

    public boolean hasTimeZonesInHrOffset(int i) {
        int i2 = 20 + i;
        if (i2 >= this.mHasTimeZonesInHrOffset.length || i2 < 0) {
            return false;
        }
        return this.mHasTimeZonesInHrOffset[i2];
    }

    private void indexByOffsets(int i, TimeZoneInfo timeZoneInfo) {
        int nowOffsetMillis = 20 + ((int) (((long) timeZoneInfo.getNowOffsetMillis()) / 3600000));
        this.mHasTimeZonesInHrOffset[nowOffsetMillis] = true;
        ArrayList<Integer> arrayList = this.mTimeZonesByOffsets.get(nowOffsetMillis);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mTimeZonesByOffsets.put(nowOffsetMillis, arrayList);
        }
        arrayList.add(Integer.valueOf(i));
    }

    public ArrayList<Integer> getTimeZonesByOffset(int i) {
        int i2 = 20 + i;
        if (i2 >= this.mHasTimeZonesInHrOffset.length || i2 < 0) {
            return null;
        }
        return this.mTimeZonesByOffsets.get(i2);
    }

    private HashSet<String> loadTzsInZoneTab(Context context) throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2;
        HashSet<String> hashSet = new HashSet<>();
        AssetManager assets = context.getAssets();
        BufferedReader bufferedReader3 = null;
        try {
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(assets.open("backward")));
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (!line.startsWith("#") && line.length() > 0) {
                                String[] strArrSplit = line.split("\t+");
                                String str = strArrSplit[1];
                                String str2 = strArrSplit[strArrSplit.length - 1];
                                if (TimeZone.getTimeZone(str) == null) {
                                    Log.e("TimeZoneData", "Timezone not found: " + str);
                                } else {
                                    hashSet.add(str2);
                                    if (this.mDefaultTimeZoneId != null && this.mDefaultTimeZoneId.equals(str2)) {
                                        this.mAlternateDefaultTimeZoneId = str;
                                    }
                                }
                            }
                        } else {
                            try {
                                break;
                            } catch (IOException e) {
                                Log.e("TimeZoneData", e.toString());
                            }
                        }
                    } catch (IOException e2) {
                        bufferedReader3 = bufferedReader;
                        Log.e("TimeZoneData", "Failed to read 'backward' file.");
                        if (bufferedReader3 != null) {
                            try {
                                bufferedReader3.close();
                            } catch (IOException e3) {
                                Log.e("TimeZoneData", e3.toString());
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e4) {
                                Log.e("TimeZoneData", e4.toString());
                            }
                        }
                        throw th;
                    }
                }
                bufferedReader.close();
                bufferedReader3 = bufferedReader;
            } catch (IOException e5) {
            }
            try {
                try {
                    try {
                        String language = Locale.getDefault().getLanguage();
                        bufferedReader2 = new BufferedReader(new InputStreamReader(assets.open("zone.tab")));
                        while (true) {
                            try {
                                String line2 = bufferedReader2.readLine();
                                if (line2 == null) {
                                    break;
                                }
                                if (!line2.startsWith("#")) {
                                    String[] strArrSplit2 = line2.split("\t");
                                    String str3 = strArrSplit2[2];
                                    String str4 = strArrSplit2[0];
                                    TimeZone timeZone = TimeZone.getTimeZone(str3);
                                    if (timeZone == null) {
                                        Log.e("TimeZoneData", "Timezone not found: " + str3);
                                    } else if (str4 == null && !str3.startsWith("Etc/GMT")) {
                                        hashSet.add(str3);
                                    } else {
                                        String countryNames = this.mCountryCodeToNameMap.get(str4);
                                        if (countryNames == null) {
                                            countryNames = getCountryNames(language, str4);
                                            this.mCountryCodeToNameMap.put(str4, countryNames);
                                        }
                                        if (this.mDefaultTimeZoneId != null && this.mDefaultTimeZoneCountry == null && str3.equals(this.mAlternateDefaultTimeZoneId)) {
                                            this.mDefaultTimeZoneCountry = countryNames;
                                            TimeZone timeZone2 = TimeZone.getTimeZone(this.mDefaultTimeZoneId);
                                            if (timeZone2 != null) {
                                                this.mDefaultTimeZoneInfo = new TimeZoneInfo(timeZone2, countryNames);
                                                int identicalTimeZoneInTheCountry = getIdenticalTimeZoneInTheCountry(this.mDefaultTimeZoneInfo);
                                                if (identicalTimeZoneInTheCountry == -1) {
                                                    this.mTimeZones.add(this.mDefaultTimeZoneInfo);
                                                } else {
                                                    this.mTimeZones.add(identicalTimeZoneInTheCountry, this.mDefaultTimeZoneInfo);
                                                }
                                            }
                                        }
                                        TimeZoneInfo timeZoneInfo = new TimeZoneInfo(timeZone, countryNames);
                                        if (getIdenticalTimeZoneInTheCountry(timeZoneInfo) == -1) {
                                            this.mTimeZones.add(timeZoneInfo);
                                        }
                                        hashSet.add(str3);
                                    }
                                }
                            } catch (IOException e6) {
                                bufferedReader3 = bufferedReader2;
                                Log.e("TimeZoneData", "Failed to read 'zone.tab'.");
                                if (bufferedReader3 != null) {
                                    bufferedReader3.close();
                                }
                                return hashSet;
                            } catch (Throwable th2) {
                                th = th2;
                                if (bufferedReader2 != null) {
                                    try {
                                        bufferedReader2.close();
                                    } catch (IOException e7) {
                                        Log.e("TimeZoneData", e7.toString());
                                    }
                                }
                                throw th;
                            }
                        }
                        bufferedReader2.close();
                    } catch (Throwable th3) {
                        th = th3;
                        bufferedReader2 = bufferedReader3;
                    }
                } catch (IOException e8) {
                }
            } catch (IOException e9) {
                Log.e("TimeZoneData", e9.toString());
            }
            return hashSet;
        } catch (Throwable th4) {
            th = th4;
            bufferedReader = bufferedReader3;
        }
    }

    private String getCountryNames(String str, String str2) {
        String displayCountry;
        Locale locale = Locale.getDefault();
        if ("PS".equalsIgnoreCase(str2)) {
            displayCountry = this.mPalestineDisplayName;
        } else {
            displayCountry = new Locale(str, str2).getDisplayCountry(locale);
        }
        if (!str2.equals(displayCountry)) {
            return displayCountry;
        }
        if (mBackupCountryCodes == null || !locale.equals(mBackupCountryLocale)) {
            mBackupCountryLocale = locale;
            mBackupCountryCodes = this.mContext.getResources().getStringArray(R.array.backup_country_codes);
            mBackupCountryNames = this.mContext.getResources().getStringArray(R.array.backup_country_names);
        }
        int iMin = Math.min(mBackupCountryCodes.length, mBackupCountryNames.length);
        for (int i = 0; i < iMin; i++) {
            if (mBackupCountryCodes[i].equals(str2)) {
                return mBackupCountryNames[i];
            }
        }
        return str2;
    }

    private int getIdenticalTimeZoneInTheCountry(TimeZoneInfo timeZoneInfo) {
        int i = 0;
        for (TimeZoneInfo timeZoneInfo2 : this.mTimeZones) {
            if (timeZoneInfo2.hasSameRules(timeZoneInfo)) {
                if (timeZoneInfo2.mCountry == null) {
                    if (timeZoneInfo.mCountry == null) {
                        return i;
                    }
                } else if (timeZoneInfo2.mCountry.equals(timeZoneInfo.mCountry)) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }
}
