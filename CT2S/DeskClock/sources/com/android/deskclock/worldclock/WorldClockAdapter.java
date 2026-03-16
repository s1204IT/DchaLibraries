package com.android.deskclock.worldclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.AnalogClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import java.text.Collator;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class WorldClockAdapter extends BaseAdapter {
    protected Object[] mCitiesList;
    private String mClockStyle;
    protected int mClocksPerRow;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final Collator mCollator = Collator.getInstance();
    protected HashMap<String, CityObj> mCitiesDb = new HashMap<>();

    public WorldClockAdapter(Context context) {
        this.mContext = context;
        loadData(context);
        loadCitiesDb(context);
        this.mInflater = LayoutInflater.from(context);
        this.mClocksPerRow = context.getResources().getInteger(R.integer.world_clocks_per_row);
    }

    public void reloadData(Context context) {
        loadData(context);
        notifyDataSetChanged();
    }

    public void loadData(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mClockStyle = prefs.getString("clock_style", this.mContext.getResources().getString(R.string.default_clock_style));
        this.mCitiesList = Cities.readCitiesFromSharedPrefs(prefs).values().toArray();
        sortList();
        this.mCitiesList = addHomeCity();
    }

    public void loadCitiesDb(Context context) {
        this.mCitiesDb.clear();
        CityObj[] cities = Utils.loadCitiesFromXml(context);
        if (cities != null) {
            for (int i = 0; i < cities.length; i++) {
                this.mCitiesDb.put(cities[i].mCityId, cities[i]);
            }
        }
    }

    private Object[] addHomeCity() {
        if (needHomeCity()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            String homeTZ = sharedPref.getString("home_time_zone", "");
            CityObj c = new CityObj(this.mContext.getResources().getString(R.string.home_label), homeTZ, null);
            Object[] temp = new Object[this.mCitiesList.length + 1];
            temp[0] = c;
            for (int i = 0; i < this.mCitiesList.length; i++) {
                temp[i + 1] = this.mCitiesList[i];
            }
            return temp;
        }
        return this.mCitiesList;
    }

    public void updateHomeLabel(Context context) {
        if (needHomeCity() && this.mCitiesList.length > 0) {
            ((CityObj) this.mCitiesList[0]).mCityName = context.getResources().getString(R.string.home_label);
        }
    }

    public boolean needHomeCity() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        if (!sharedPref.getBoolean("automatic_home_clock", false)) {
            return false;
        }
        String homeTZ = sharedPref.getString("home_time_zone", TimeZone.getDefault().getID());
        Date now = new Date();
        return TimeZone.getTimeZone(homeTZ).getOffset(now.getTime()) != TimeZone.getDefault().getOffset(now.getTime());
    }

    public boolean hasHomeCity() {
        return this.mCitiesList != null && this.mCitiesList.length > 0 && ((CityObj) this.mCitiesList[0]).mCityId == null;
    }

    private void sortList() {
        final Date now = new Date();
        Arrays.sort(this.mCitiesList, new Comparator<Object>() {
            private int safeCityNameCompare(CityObj city1, CityObj city2) {
                if (city1.mCityName == null && city2.mCityName == null) {
                    return 0;
                }
                if (city1.mCityName == null) {
                    return -1;
                }
                if (city2.mCityName != null) {
                    return WorldClockAdapter.this.mCollator.compare(city1.mCityName, city2.mCityName);
                }
                return 1;
            }

            @Override
            public int compare(Object object1, Object object2) {
                CityObj city1 = (CityObj) object1;
                CityObj city2 = (CityObj) object2;
                if (city1.mTimeZone == null && city2.mTimeZone == null) {
                    return safeCityNameCompare(city1, city2);
                }
                if (city1.mTimeZone == null) {
                    return -1;
                }
                if (city2.mTimeZone == null) {
                    return 1;
                }
                int gmOffset1 = TimeZone.getTimeZone(city1.mTimeZone).getOffset(now.getTime());
                int gmOffset2 = TimeZone.getTimeZone(city2.mTimeZone).getOffset(now.getTime());
                if (gmOffset1 == gmOffset2) {
                    return safeCityNameCompare(city1, city2);
                }
                return gmOffset1 - gmOffset2;
            }
        });
    }

    @Override
    public int getCount() {
        return this.mClocksPerRow == 1 ? this.mCitiesList.length : (this.mCitiesList.length + 1) / 2;
    }

    @Override
    public Object getItem(int p) {
        return null;
    }

    @Override
    public long getItemId(int p) {
        return p;
    }

    @Override
    public boolean isEnabled(int p) {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int index = position * this.mClocksPerRow;
        if (index < 0 || index >= this.mCitiesList.length) {
            return null;
        }
        if (view == null) {
            view = this.mInflater.inflate(R.layout.world_clock_list_item, parent, false);
        }
        updateView(view.findViewById(R.id.city_left), (CityObj) this.mCitiesList[index]);
        return view;
    }

    private void updateView(View clock, CityObj cityObj) {
        View nameLayout = clock.findViewById(R.id.city_name_layout);
        TextView name = (TextView) nameLayout.findViewById(R.id.city_name);
        TextView dayOfWeek = (TextView) nameLayout.findViewById(R.id.city_day);
        TextClock dclock = (TextClock) clock.findViewById(R.id.digital_clock);
        AnalogClock aclock = (AnalogClock) clock.findViewById(R.id.analog_clock);
        if (this.mClockStyle.equals("analog")) {
            dclock.setVisibility(8);
            aclock.setVisibility(0);
            aclock.setTimeZone(cityObj.mTimeZone);
            aclock.enableSeconds(false);
        } else {
            dclock.setVisibility(0);
            aclock.setVisibility(8);
            dclock.setTimeZone(cityObj.mTimeZone);
            Utils.setTimeFormat(dclock, (int) this.mContext.getResources().getDimension(R.dimen.label_font_size));
        }
        CityObj cityInDb = this.mCitiesDb.get(cityObj.mCityId);
        name.setText(Utils.getCityName(cityObj, cityInDb));
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());
        int myDayOfWeek = now.get(7);
        String cityTZ = cityInDb != null ? cityInDb.mTimeZone : cityObj.mTimeZone;
        now.setTimeZone(TimeZone.getTimeZone(cityTZ));
        int cityDayOfWeek = now.get(7);
        if (myDayOfWeek != cityDayOfWeek) {
            dayOfWeek.setText(this.mContext.getString(R.string.world_day_of_week_label, now.getDisplayName(7, 1, Locale.getDefault())));
            dayOfWeek.setVisibility(0);
        } else {
            dayOfWeek.setVisibility(8);
        }
    }
}
