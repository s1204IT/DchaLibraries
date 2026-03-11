package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.android.settingslib.datetime.ZoneGetter;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ZonePicker extends ListFragment {
    private SimpleAdapter mAlphabeticalAdapter;
    private ZoneSelectionListener mListener;
    private boolean mSortedByTimezone;
    private SimpleAdapter mTimezoneSortedAdapter;

    public interface ZoneSelectionListener {
        void onZoneSelected(TimeZone timeZone);
    }

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean sortedByName) {
        return constructTimezoneAdapter(context, sortedByName, R.layout.date_time_setup_custom_list_item_2);
    }

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean sortedByName, int layoutId) throws Throwable {
        String[] from = {"name", "gmt"};
        int[] to = {android.R.id.text1, android.R.id.text2};
        String sortKey = sortedByName ? "name" : "offset";
        MyComparator comparator = new MyComparator(sortKey);
        List<Map<String, Object>> sortedList = ZoneGetter.getZonesList(context);
        Collections.sort(sortedList, comparator);
        SimpleAdapter adapter = new SimpleAdapter(context, sortedList, layoutId, from, to);
        return adapter;
    }

    public static int getTimeZoneIndex(SimpleAdapter adapter, TimeZone tz) {
        String defaultId = tz.getID();
        int listSize = adapter.getCount();
        for (int i = 0; i < listSize; i++) {
            HashMap<?, ?> map = (HashMap) adapter.getItem(i);
            String id = (String) map.get("id");
            if (defaultId.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public static TimeZone obtainTimeZoneFromItem(Object item) {
        return TimeZone.getTimeZone((String) ((Map) item).get("id"));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        this.mTimezoneSortedAdapter = constructTimezoneAdapter(activity, false);
        this.mAlphabeticalAdapter = constructTimezoneAdapter(activity, true);
        setSorting(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.forcePrepareCustomPreferencesList(container, view, list, false);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.zone_list_menu_sort_alphabetically).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, 2, 0, R.string.zone_list_menu_sort_by_timezone).setIcon(R.drawable.ic_menu_3d_globe);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (this.mSortedByTimezone) {
            menu.findItem(2).setVisible(false);
            menu.findItem(1).setVisible(true);
        } else {
            menu.findItem(2).setVisible(true);
            menu.findItem(1).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                setSorting(false);
                return true;
            case DefaultWfcSettingsExt.CREATE:
                setSorting(true);
                return true;
            default:
                return false;
        }
    }

    private void setSorting(boolean sortByTimezone) {
        SimpleAdapter adapter = sortByTimezone ? this.mTimezoneSortedAdapter : this.mAlphabeticalAdapter;
        setListAdapter(adapter);
        this.mSortedByTimezone = sortByTimezone;
        int defaultIndex = getTimeZoneIndex(adapter, TimeZone.getDefault());
        if (defaultIndex < 0) {
            return;
        }
        setSelection(defaultIndex);
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        if (isResumed()) {
            Map<?, ?> map = (Map) listView.getItemAtPosition(position);
            String tzId = (String) map.get("id");
            Activity activity = getActivity();
            AlarmManager alarm = (AlarmManager) activity.getSystemService("alarm");
            alarm.setTimeZone(tzId);
            TimeZone tz = TimeZone.getTimeZone(tzId);
            if (this.mListener != null) {
                this.mListener.onZoneSelected(tz);
            } else {
                getActivity().onBackPressed();
            }
        }
    }

    private static class MyComparator implements Comparator<Map<?, ?>> {
        private String mSortingKey;

        public MyComparator(String sortingKey) {
            this.mSortingKey = sortingKey;
        }

        @Override
        public int compare(Map<?, ?> map1, Map<?, ?> map2) {
            Object value1 = map1.get(this.mSortingKey);
            Object value2 = map2.get(this.mSortingKey);
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            }
            if (!isComparable(value2)) {
                return -1;
            }
            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            if (value != null) {
                return value instanceof Comparable;
            }
            return false;
        }
    }
}
