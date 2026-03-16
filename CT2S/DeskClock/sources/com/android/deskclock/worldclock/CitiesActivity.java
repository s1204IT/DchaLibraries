package com.android.deskclock.worldclock;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CitiesActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SearchView.OnQueryTextListener {
    private CityAdapter mAdapter;
    private Calendar mCalendar;
    private ListView mCitiesList;
    private LayoutInflater mFactory;
    private SharedPreferences mPrefs;
    private boolean mSearchMode;
    private SearchView mSearchView;
    private String mSelectedCitiesHeaderString;
    private int mSortType;
    private HashMap<String, CityObj> mUserSelectedCities;
    private StringBuffer mQueryTextBuffer = new StringBuffer();
    private int mPosition = -1;

    private class CityAdapter extends BaseAdapter implements Filterable, SectionIndexer {
        private CityObj[] mCities;
        private List<CityObj> mDisplayedCitiesList;
        private final LayoutInflater mInflater;
        private boolean mIs24HoursMode;
        private final int mLayoutDirection;
        private final String mPattern12;
        private final String mPattern24;
        private String[] mSectionHeaders;
        private Integer[] mSectionPositions;
        private CityObj[] mSelectedCities;
        private HashMap<String, String> mCityNameMap = new HashMap<>();
        private CityNameComparator mSortByNameComparator = new CityNameComparator();
        private CityGmtOffsetComparator mSortByTimeComparator = new CityGmtOffsetComparator();
        private int mSelectedEndPosition = 0;
        private Filter mFilter = new Filter() {
            @Override
            protected synchronized Filter.FilterResults performFiltering(CharSequence constraint) {
                Filter.FilterResults results;
                results = new Filter.FilterResults();
                String modifiedQuery = constraint.toString().trim().toUpperCase();
                ArrayList<CityObj> filteredList = new ArrayList<>();
                ArrayList<String> sectionHeaders = new ArrayList<>();
                ArrayList<Integer> sectionPositions = new ArrayList<>();
                Collection<CityObj> selectedCities = CitiesActivity.this.mUserSelectedCities.values();
                CityAdapter.this.mSelectedCities = (CityObj[]) selectedCities.toArray(new CityObj[selectedCities.size()]);
                if (TextUtils.isEmpty(modifiedQuery) && CityAdapter.this.mSelectedCities != null) {
                    if (CityAdapter.this.mSelectedCities.length > 0) {
                        sectionHeaders.add("+");
                        sectionPositions.add(0);
                        filteredList.add(new CityObj(CitiesActivity.this.mSelectedCitiesHeaderString, CitiesActivity.this.mSelectedCitiesHeaderString, null));
                    }
                    CityObj[] arr$ = CityAdapter.this.mSelectedCities;
                    for (CityObj city : arr$) {
                        city.isHeader = false;
                        filteredList.add(city);
                    }
                }
                HashSet<String> selectedCityIds = new HashSet<>();
                CityObj[] arr$2 = CityAdapter.this.mSelectedCities;
                for (CityObj c : arr$2) {
                    selectedCityIds.add(c.mCityId);
                }
                CityAdapter.this.mSelectedEndPosition = filteredList.size();
                long currentTime = System.currentTimeMillis();
                String val = null;
                int offset = -100000;
                CityObj[] arr$3 = CityAdapter.this.mCities;
                for (CityObj city2 : arr$3) {
                    if (!city2.mCityId.equals("C0")) {
                        if (TextUtils.isEmpty(modifiedQuery)) {
                            if (!selectedCityIds.contains(city2.mCityId)) {
                                if (CitiesActivity.this.mSortType == 0 && !city2.mCityName.substring(0, 1).equals(val)) {
                                    val = city2.mCityName.substring(0, 1).toUpperCase();
                                    sectionHeaders.add(val);
                                    sectionPositions.add(Integer.valueOf(filteredList.size()));
                                    city2.isHeader = true;
                                } else {
                                    city2.isHeader = false;
                                }
                                if (CitiesActivity.this.mSortType == 1) {
                                    TimeZone timezone = TimeZone.getTimeZone(city2.mTimeZone);
                                    int newOffset = timezone.getOffset(currentTime);
                                    if (offset != newOffset) {
                                        offset = newOffset;
                                        String offsetString = Utils.getGMTHourOffset(timezone, true);
                                        sectionHeaders.add(offsetString);
                                        sectionPositions.add(Integer.valueOf(filteredList.size()));
                                        city2.isHeader = true;
                                    } else {
                                        city2.isHeader = false;
                                    }
                                }
                                filteredList.add(city2);
                            }
                        } else {
                            String cityName = city2.mCityName.trim().toUpperCase();
                            if (city2.mCityId != null && cityName.startsWith(modifiedQuery)) {
                                city2.isHeader = false;
                                filteredList.add(city2);
                            }
                        }
                    }
                }
                CityAdapter.this.mSectionHeaders = (String[]) sectionHeaders.toArray(new String[sectionHeaders.size()]);
                CityAdapter.this.mSectionPositions = (Integer[]) sectionPositions.toArray(new Integer[sectionPositions.size()]);
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                CityAdapter.this.mDisplayedCitiesList = (ArrayList) results.values;
                if (CitiesActivity.this.mPosition >= 0) {
                    CitiesActivity.this.mCitiesList.setSelectionFromTop(CitiesActivity.this.mPosition, 0);
                    CitiesActivity.this.mPosition = -1;
                }
                CityAdapter.this.notifyDataSetChanged();
            }
        };

        public CityAdapter(Context context, LayoutInflater factory) {
            CitiesActivity.this.mCalendar = Calendar.getInstance();
            CitiesActivity.this.mCalendar.setTimeInMillis(System.currentTimeMillis());
            this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
            this.mInflater = factory;
            this.mCities = Utils.loadCitiesFromXml(context);
            this.mCityNameMap.clear();
            CityObj[] arr$ = this.mCities;
            for (CityObj city : arr$) {
                this.mCityNameMap.put(city.mCityId, city.mCityName);
            }
            Collection<CityObj> selectedCities = CitiesActivity.this.mUserSelectedCities.values();
            this.mSelectedCities = (CityObj[]) selectedCities.toArray(new CityObj[selectedCities.size()]);
            CityObj[] arr$2 = this.mSelectedCities;
            for (CityObj city2 : arr$2) {
                String newCityName = this.mCityNameMap.get(city2.mCityId);
                if (newCityName != null) {
                    city2.mCityName = newCityName;
                }
            }
            this.mPattern24 = DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm");
            String pattern12 = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma");
            this.mPattern12 = this.mLayoutDirection == 1 ? pattern12.replaceAll("h", "hh") : pattern12;
            sortCities(CitiesActivity.this.mSortType);
            set24HoursMode(context);
        }

        public void toggleSort() {
            if (CitiesActivity.this.mSortType == 0) {
                sortCities(1);
            } else {
                sortCities(0);
            }
        }

        private void sortCities(int sortType) {
            CitiesActivity.this.mSortType = sortType;
            Arrays.sort(this.mCities, sortType == 0 ? this.mSortByNameComparator : this.mSortByTimeComparator);
            if (this.mSelectedCities != null) {
                Arrays.sort(this.mSelectedCities, sortType == 0 ? this.mSortByNameComparator : this.mSortByTimeComparator);
            }
            CitiesActivity.this.mPrefs.edit().putInt("sort_preference", sortType).commit();
            this.mFilter.filter(CitiesActivity.this.mQueryTextBuffer.toString());
        }

        @Override
        public int getCount() {
            if (this.mDisplayedCitiesList != null) {
                return this.mDisplayedCitiesList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int p) {
            if (this.mDisplayedCitiesList == null || p < 0 || p >= this.mDisplayedCitiesList.size()) {
                return null;
            }
            return this.mDisplayedCitiesList.get(p);
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public boolean isEnabled(int p) {
            return (this.mDisplayedCitiesList == null || this.mDisplayedCitiesList.get(p).mCityId == null) ? false : true;
        }

        @Override
        public synchronized View getView(int position, View view, ViewGroup parent) {
            View view2 = null;
            synchronized (this) {
                if (this.mDisplayedCitiesList != null && position >= 0 && position < this.mDisplayedCitiesList.size()) {
                    CityObj c = this.mDisplayedCitiesList.get(position);
                    if (c.mCityId == null) {
                        if (view == null) {
                            view = this.mInflater.inflate(R.layout.city_list_header, parent, false);
                        }
                    } else {
                        if (view == null) {
                            view = this.mInflater.inflate(R.layout.city_list_item, parent, false);
                            CityViewHolder holder = new CityViewHolder();
                            holder.index = (TextView) view.findViewById(R.id.index);
                            holder.name = (TextView) view.findViewById(R.id.city_name);
                            holder.time = (TextView) view.findViewById(R.id.city_time);
                            holder.selected = (CheckBox) view.findViewById(R.id.city_onoff);
                            view.setTag(holder);
                        }
                        view.setOnClickListener(CitiesActivity.this);
                        CityViewHolder holder2 = (CityViewHolder) view.getTag();
                        holder2.selected.setTag(c);
                        holder2.selected.setChecked(CitiesActivity.this.mUserSelectedCities.containsKey(c.mCityId));
                        holder2.selected.setOnCheckedChangeListener(CitiesActivity.this);
                        holder2.name.setText(c.mCityName, TextView.BufferType.SPANNABLE);
                        holder2.time.setText(getTimeCharSequence(c.mTimeZone));
                        if (c.isHeader) {
                            holder2.index.setVisibility(0);
                            if (CitiesActivity.this.mSortType == 0) {
                                holder2.index.setText(c.mCityName.substring(0, 1));
                                holder2.index.setTextSize(2, 24.0f);
                            } else {
                                holder2.index.setTextSize(2, 14.0f);
                                holder2.index.setText(Utils.getGMTHourOffset(TimeZone.getTimeZone(c.mTimeZone), true));
                            }
                        } else {
                            holder2.index.setVisibility(4);
                        }
                        view.jumpDrawablesToCurrentState();
                    }
                    view2 = view;
                }
            }
            return view2;
        }

        private CharSequence getTimeCharSequence(String timeZone) {
            CitiesActivity.this.mCalendar.setTimeZone(TimeZone.getTimeZone(timeZone));
            return DateFormat.format(this.mIs24HoursMode ? this.mPattern24 : this.mPattern12, CitiesActivity.this.mCalendar);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return this.mDisplayedCitiesList.get(position).mCityId != null ? 0 : 1;
        }

        private class CityViewHolder {
            TextView index;
            TextView name;
            CheckBox selected;
            TextView time;

            private CityViewHolder() {
            }
        }

        public void set24HoursMode(Context c) {
            this.mIs24HoursMode = DateFormat.is24HourFormat(c);
            notifyDataSetChanged();
        }

        @Override
        public int getPositionForSection(int section) {
            if (isEmpty(this.mSectionPositions)) {
                return 0;
            }
            return this.mSectionPositions[section].intValue();
        }

        @Override
        public int getSectionForPosition(int p) {
            Integer[] positions = this.mSectionPositions;
            if (!isEmpty(positions)) {
                for (int i = 0; i < positions.length - 1; i++) {
                    if (p >= positions[i].intValue() && p < positions[i + 1].intValue()) {
                        return i;
                    }
                }
                if (p >= positions[positions.length - 1].intValue()) {
                    int i2 = positions.length - 1;
                    return i2;
                }
            }
            return 0;
        }

        @Override
        public Object[] getSections() {
            return this.mSectionHeaders;
        }

        @Override
        public Filter getFilter() {
            return this.mFilter;
        }

        private boolean isEmpty(Object[] array) {
            return array == null || array.length == 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(4);
        this.mFactory = LayoutInflater.from(this);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.mSortType = this.mPrefs.getInt("sort_preference", 0);
        this.mSelectedCitiesHeaderString = getString(R.string.selected_cities_label);
        if (savedInstanceState != null) {
            this.mQueryTextBuffer.append(savedInstanceState.getString("search_query"));
            this.mSearchMode = savedInstanceState.getBoolean("search_mode");
            this.mPosition = savedInstanceState.getInt("list_position");
        }
        updateLayout();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("search_query", this.mQueryTextBuffer.toString());
        bundle.putBoolean("search_mode", this.mSearchMode);
        bundle.putInt("list_position", this.mCitiesList.getFirstVisiblePosition());
    }

    private void updateLayout() {
        setContentView(R.layout.cities_activity);
        this.mCitiesList = (ListView) findViewById(R.id.cities_list);
        setFastScroll(TextUtils.isEmpty(this.mQueryTextBuffer.toString().trim()));
        this.mCitiesList.setScrollBarStyle(16777216);
        this.mUserSelectedCities = Cities.readCitiesFromSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this));
        this.mAdapter = new CityAdapter(this, this.mFactory);
        this.mCitiesList.setAdapter((ListAdapter) this.mAdapter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(4, 4);
        }
    }

    private void setFastScroll(boolean enabled) {
        if (this.mCitiesList != null) {
            this.mCitiesList.setFastScrollAlwaysVisible(enabled);
            this.mCitiesList.setFastScrollEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mAdapter != null) {
            this.mAdapter.set24HoursMode(this);
        }
        getWindow().getDecorView().setBackgroundColor(Utils.getCurrentHourColor());
    }

    @Override
    public void onPause() {
        super.onPause();
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this), this.mUserSelectedCities);
        Intent i = new Intent("com.android.deskclock.worldclock.update");
        sendBroadcast(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_item_sort:
                if (this.mAdapter == null) {
                    return true;
                }
                this.mAdapter.toggleSort();
                setFastScroll(TextUtils.isEmpty(this.mQueryTextBuffer.toString().trim()));
                return true;
            case R.id.menu_item_settings:
                startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i == null) {
                    return true;
                }
                try {
                    startActivity(i);
                    return true;
                } catch (ActivityNotFoundException e) {
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cities_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        MenuItem searchMenu = menu.findItem(R.id.menu_item_search);
        this.mSearchView = (SearchView) searchMenu.getActionView();
        this.mSearchView.setImeOptions(268435456);
        this.mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                CitiesActivity.this.mSearchMode = true;
            }
        });
        this.mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                CitiesActivity.this.mSearchMode = false;
                return false;
            }
        });
        if (this.mSearchView != null) {
            this.mSearchView.setOnQueryTextListener(this);
            this.mSearchView.setQuery(this.mQueryTextBuffer.toString(), false);
            if (this.mSearchMode) {
                this.mSearchView.requestFocus();
                this.mSearchView.setIconified(false);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sortMenuItem = menu.findItem(R.id.menu_item_sort);
        if (this.mSortType == 0) {
            sortMenuItem.setTitle(getString(R.string.menu_item_sort_by_gmt_offset));
        } else {
            sortMenuItem.setTitle(getString(R.string.menu_item_sort_by_name));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCheckedChanged(CompoundButton b, boolean checked) {
        CityObj c = (CityObj) b.getTag();
        if (checked) {
            this.mUserSelectedCities.put(c.mCityId, c);
        } else {
            this.mUserSelectedCities.remove(c.mCityId);
        }
    }

    @Override
    public void onClick(View v) {
        CompoundButton b = (CompoundButton) v.findViewById(R.id.city_onoff);
        boolean checked = b.isChecked();
        onCheckedChanged(b, checked);
        b.setChecked(!checked);
    }

    @Override
    public boolean onQueryTextChange(String queryText) {
        this.mQueryTextBuffer.setLength(0);
        this.mQueryTextBuffer.append(queryText);
        this.mCitiesList.setFastScrollEnabled(TextUtils.isEmpty(this.mQueryTextBuffer.toString().trim()));
        this.mAdapter.getFilter().filter(queryText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String arg0) {
        return false;
    }
}
