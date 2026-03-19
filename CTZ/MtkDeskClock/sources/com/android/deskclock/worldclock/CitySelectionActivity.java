package com.android.deskclock.worldclock;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.actionbarmenu.MenuItemController;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.SearchMenuItemController;
import com.android.deskclock.actionbarmenu.SettingsMenuItemController;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public final class CitySelectionActivity extends BaseActivity {
    private CityAdapter mCitiesAdapter;
    private ListView mCitiesList;
    private DropShadowController mDropShadowController;
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();
    private SearchMenuItemController mSearchMenuItemController;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.cities_activity);
        this.mSearchMenuItemController = new SearchMenuItemController(getSupportActionBar().getThemedContext(), new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String str) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String str) {
                CitySelectionActivity.this.mCitiesAdapter.filter(str);
                CitySelectionActivity.this.updateFastScrolling();
                return true;
            }
        }, bundle);
        this.mCitiesAdapter = new CityAdapter(this, this.mSearchMenuItemController);
        this.mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this)).addMenuItemController(this.mSearchMenuItemController).addMenuItemController(new SortOrderMenuItemController()).addMenuItemController(new SettingsMenuItemController(this)).addMenuItemController(MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));
        this.mCitiesList = (ListView) findViewById(R.id.cities_list);
        this.mCitiesList.setAdapter((ListAdapter) this.mCitiesAdapter);
        updateFastScrolling();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mSearchMenuItemController.saveInstance(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mCitiesAdapter.refresh();
        this.mDropShadowController = new DropShadowController(findViewById(R.id.drop_shadow), this.mCitiesList);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDropShadowController.stop();
        DataModel.getDataModel().setSelectedCities(this.mCitiesAdapter.getSelectedCities());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return this.mOptionsMenuManager.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    private void updateFastScrolling() {
        boolean z = !this.mCitiesAdapter.isFiltering();
        this.mCitiesList.setFastScrollAlwaysVisible(z);
        this.mCitiesList.setFastScrollEnabled(z);
    }

    private static final class CityAdapter extends BaseAdapter implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SectionIndexer {
        private static final int VIEW_TYPE_CITY = 1;
        private static final int VIEW_TYPE_SELECTED_CITIES_HEADER = 0;
        private final Context mContext;
        private final LayoutInflater mInflater;
        private boolean mIs24HoursMode;
        private int mOriginalUserSelectionCount;
        private final String mPattern12;
        private final String mPattern24;
        private final SearchMenuItemController mSearchMenuItemController;
        private Integer[] mSectionHeaderPositions;
        private String[] mSectionHeaders;
        private List<City> mFilteredCities = Collections.emptyList();
        private final Set<City> mUserSelectedCities = new ArraySet();
        private final Calendar mCalendar = Calendar.getInstance();

        public CityAdapter(Context context, SearchMenuItemController searchMenuItemController) {
            this.mContext = context;
            this.mSearchMenuItemController = searchMenuItemController;
            this.mInflater = LayoutInflater.from(context);
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
            Locale locale = Locale.getDefault();
            this.mPattern24 = DateFormat.getBestDateTimePattern(locale, "Hm");
            String bestDateTimePattern = DateFormat.getBestDateTimePattern(locale, "hma");
            this.mPattern12 = TextUtils.getLayoutDirectionFromLocale(locale) == 1 ? bestDateTimePattern.replaceAll("h", "hh") : bestDateTimePattern;
        }

        @Override
        public int getCount() {
            boolean zHasHeader = hasHeader();
            return (zHasHeader ? 1 : 0) + this.mFilteredCities.size();
        }

        @Override
        public City getItem(int i) {
            if (hasHeader()) {
                int itemViewType = getItemViewType(i);
                switch (itemViewType) {
                    case 0:
                        return null;
                    case 1:
                        return this.mFilteredCities.get(i - 1);
                    default:
                        throw new IllegalStateException("unexpected item view type: " + itemViewType);
                }
            }
            return this.mFilteredCities.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int itemViewType = getItemViewType(i);
            switch (itemViewType) {
                case 0:
                    if (view == null) {
                        return this.mInflater.inflate(R.layout.city_list_header, viewGroup, false);
                    }
                    return view;
                case 1:
                    City item = getItem(i);
                    if (item == null) {
                        throw new IllegalStateException("The desired city does not exist");
                    }
                    TimeZone timeZone = item.getTimeZone();
                    if (view == null) {
                        view = this.mInflater.inflate(R.layout.city_list_item, viewGroup, false);
                        view.setTag(new CityItemHolder((TextView) view.findViewById(R.id.index), (TextView) view.findViewById(R.id.city_name), (TextView) view.findViewById(R.id.city_time), (CheckBox) view.findViewById(R.id.city_onoff)));
                    }
                    CityItemHolder cityItemHolder = (CityItemHolder) view.getTag();
                    cityItemHolder.selected.setTag(item);
                    cityItemHolder.selected.setChecked(this.mUserSelectedCities.contains(item));
                    cityItemHolder.selected.setContentDescription(item.getName());
                    cityItemHolder.selected.setOnCheckedChangeListener(this);
                    cityItemHolder.name.setText(item.getName(), TextView.BufferType.SPANNABLE);
                    cityItemHolder.time.setText(getTimeCharSequence(timeZone));
                    boolean showIndex = getShowIndex(i);
                    cityItemHolder.index.setVisibility(showIndex ? 0 : 4);
                    if (showIndex) {
                        switch (getCitySort()) {
                            case NAME:
                                cityItemHolder.index.setText(item.getIndexString());
                                cityItemHolder.index.setTextSize(2, 24.0f);
                                break;
                            case UTC_OFFSET:
                                cityItemHolder.index.setText(Utils.getGMTHourOffset(timeZone, false));
                                cityItemHolder.index.setTextSize(2, 14.0f);
                                break;
                        }
                    }
                    view.jumpDrawablesToCurrentState();
                    view.setOnClickListener(this);
                    return view;
                default:
                    throw new IllegalStateException("unexpected item view type: " + itemViewType);
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int i) {
            return (hasHeader() && i == 0) ? 0 : 1;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            City city = (City) compoundButton.getTag();
            if (z) {
                this.mUserSelectedCities.add(city);
                compoundButton.announceForAccessibility(this.mContext.getString(R.string.city_checked, city.getName()));
            } else {
                this.mUserSelectedCities.remove(city);
                compoundButton.announceForAccessibility(this.mContext.getString(R.string.city_unchecked, city.getName()));
            }
        }

        @Override
        public void onClick(View view) {
            ((CheckBox) view.findViewById(R.id.city_onoff)).setChecked(!r2.isChecked());
        }

        @Override
        public Object[] getSections() {
            if (this.mSectionHeaders == null) {
                int count = getCount() / 5;
                ArrayList arrayList = new ArrayList(count);
                ArrayList arrayList2 = new ArrayList(count);
                if (hasHeader()) {
                    arrayList.add("+");
                    arrayList2.add(0);
                }
                for (int i = 0; i < getCount(); i++) {
                    if (getShowIndex(i)) {
                        City item = getItem(i);
                        if (item == null) {
                            throw new IllegalStateException("The desired city does not exist");
                        }
                        switch (getCitySort()) {
                            case NAME:
                                arrayList.add(item.getIndexString());
                                break;
                            case UTC_OFFSET:
                                arrayList.add(Utils.getGMTHourOffset(item.getTimeZone(), Utils.isPreL()));
                                break;
                        }
                        arrayList2.add(Integer.valueOf(i));
                    }
                }
                this.mSectionHeaders = (String[]) arrayList.toArray(new String[arrayList.size()]);
                this.mSectionHeaderPositions = (Integer[]) arrayList2.toArray(new Integer[arrayList2.size()]);
            }
            return this.mSectionHeaders;
        }

        @Override
        public int getPositionForSection(int i) {
            if (getSections().length == 0) {
                return 0;
            }
            return this.mSectionHeaderPositions[i].intValue();
        }

        @Override
        public int getSectionForPosition(int i) {
            if (getSections().length == 0) {
                return 0;
            }
            for (int i2 = 0; i2 < this.mSectionHeaderPositions.length - 2; i2++) {
                if (i >= this.mSectionHeaderPositions[i2].intValue() && i < this.mSectionHeaderPositions[i2 + 1].intValue()) {
                    return i2;
                }
            }
            return this.mSectionHeaderPositions.length - 1;
        }

        private void clearSectionHeaders() {
            this.mSectionHeaders = null;
            this.mSectionHeaderPositions = null;
        }

        private void refresh() {
            this.mIs24HoursMode = DateFormat.is24HourFormat(this.mContext);
            List<City> selectedCities = DataModel.getDataModel().getSelectedCities();
            this.mUserSelectedCities.clear();
            this.mUserSelectedCities.addAll(selectedCities);
            this.mOriginalUserSelectionCount = selectedCities.size();
            clearSectionHeaders();
            filter(this.mSearchMenuItemController.getQueryText());
        }

        private void filter(String str) {
            List<City> allCities;
            this.mSearchMenuItemController.setQueryText(str);
            String strRemoveSpecialCharacters = City.removeSpecialCharacters(str.toUpperCase());
            if (TextUtils.isEmpty(strRemoveSpecialCharacters)) {
                allCities = DataModel.getDataModel().getAllCities();
            } else {
                List<City> unselectedCities = DataModel.getDataModel().getUnselectedCities();
                ArrayList arrayList = new ArrayList(unselectedCities.size());
                for (City city : unselectedCities) {
                    if (city.matches(strRemoveSpecialCharacters)) {
                        arrayList.add(city);
                    }
                }
                allCities = arrayList;
            }
            this.mFilteredCities = allCities;
            notifyDataSetChanged();
        }

        private boolean isFiltering() {
            return !TextUtils.isEmpty(this.mSearchMenuItemController.getQueryText().trim());
        }

        private Collection<City> getSelectedCities() {
            return this.mUserSelectedCities;
        }

        private boolean hasHeader() {
            return !isFiltering() && this.mOriginalUserSelectionCount > 0;
        }

        private DataModel.CitySort getCitySort() {
            return DataModel.getDataModel().getCitySort();
        }

        private Comparator<City> getCitySortComparator() {
            return DataModel.getDataModel().getCityIndexComparator();
        }

        private CharSequence getTimeCharSequence(TimeZone timeZone) {
            this.mCalendar.setTimeZone(timeZone);
            return DateFormat.format(this.mIs24HoursMode ? this.mPattern24 : this.mPattern12, this.mCalendar);
        }

        private boolean getShowIndex(int i) {
            if (isFiltering()) {
                return false;
            }
            if (hasHeader()) {
                if (i <= this.mOriginalUserSelectionCount) {
                    return false;
                }
                if (i == this.mOriginalUserSelectionCount + 1) {
                    return true;
                }
            } else {
                if (i < this.mOriginalUserSelectionCount) {
                    return false;
                }
                if (i == this.mOriginalUserSelectionCount) {
                    return true;
                }
            }
            return getCitySortComparator().compare(getItem(i + (-1)), getItem(i)) != 0;
        }

        private static final class CityItemHolder {
            private final TextView index;
            private final TextView name;
            private final CheckBox selected;
            private final TextView time;

            public CityItemHolder(TextView textView, TextView textView2, TextView textView3, CheckBox checkBox) {
                this.index = textView;
                this.name = textView2;
                this.time = textView3;
                this.selected = checkBox;
            }
        }
    }

    private final class SortOrderMenuItemController implements MenuItemController {
        private static final int SORT_MENU_RES_ID = 2131361953;

        private SortOrderMenuItemController() {
        }

        @Override
        public int getId() {
            return R.id.menu_item_sort;
        }

        @Override
        public void onCreateOptionsItem(Menu menu) {
            menu.add(0, R.id.menu_item_sort, 0, R.string.menu_item_sort_by_gmt_offset).setShowAsAction(0);
        }

        @Override
        public void onPrepareOptionsItem(MenuItem menuItem) {
            menuItem.setTitle(DataModel.getDataModel().getCitySort() == DataModel.CitySort.NAME ? R.string.menu_item_sort_by_gmt_offset : R.string.menu_item_sort_by_name);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem) {
            DataModel.getDataModel().toggleCitySort();
            CitySelectionActivity.this.mCitiesAdapter.clearSectionHeaders();
            CitySelectionActivity.this.mCitiesAdapter.filter(CitySelectionActivity.this.mSearchMenuItemController.getQueryText());
            return true;
        }
    }
}
