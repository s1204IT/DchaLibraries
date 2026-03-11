package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import libcore.icu.TimeZoneNames;
import org.xmlpull.v1.XmlPullParserException;

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

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean sortedByName, int layoutId) {
        String[] from = {"name", "gmt"};
        int[] to = {android.R.id.text1, android.R.id.text2};
        String sortKey = sortedByName ? "name" : "offset";
        MyComparator comparator = new MyComparator(sortKey);
        ZoneGetter zoneGetter = new ZoneGetter();
        List<HashMap<String, Object>> sortedList = zoneGetter.getZones(context);
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
            case 1:
                setSorting(false);
                return true;
            case 2:
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
        if (defaultIndex >= 0) {
            setSelection(defaultIndex);
        }
    }

    static class ZoneGetter {
        private final List<HashMap<String, Object>> mZones = new ArrayList();
        private final HashSet<String> mLocalZones = new HashSet<>();
        private final Date mNow = Calendar.getInstance().getTime();
        private final SimpleDateFormat mZoneNameFormatter = new SimpleDateFormat("zzzz");

        ZoneGetter() {
        }

        public List<HashMap<String, Object>> getZones(Context context) {
            String[] arr$ = TimeZoneNames.forLocale(Locale.getDefault());
            for (String olsonId : arr$) {
                this.mLocalZones.add(olsonId);
            }
            try {
                XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones);
                while (xrp.next() != 2) {
                }
                xrp.next();
                while (xrp.getEventType() != 3) {
                    while (xrp.getEventType() != 2) {
                        if (xrp.getEventType() == 1) {
                            return this.mZones;
                        }
                        xrp.next();
                    }
                    if (xrp.getName().equals("timezone")) {
                        String olsonId2 = xrp.getAttributeValue(0);
                        addTimeZone(olsonId2);
                    }
                    while (xrp.getEventType() != 3) {
                        xrp.next();
                    }
                    xrp.next();
                }
                xrp.close();
            } catch (IOException e) {
                Log.e("ZonePicker", "Unable to read timezones.xml file");
            } catch (XmlPullParserException e2) {
                Log.e("ZonePicker", "Ill-formatted timezones.xml file");
            }
            return this.mZones;
        }

        private void addTimeZone(String olsonId) {
            String displayName;
            TimeZone tz = TimeZone.getTimeZone(olsonId);
            if (this.mLocalZones.contains(olsonId)) {
                this.mZoneNameFormatter.setTimeZone(tz);
                displayName = this.mZoneNameFormatter.format(this.mNow);
            } else {
                String localeName = Locale.getDefault().toString();
                displayName = TimeZoneNames.getExemplarLocation(localeName, olsonId);
            }
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", olsonId);
            map.put("name", displayName);
            map.put("gmt", DateTimeSettings.getTimeZoneText(tz, false));
            map.put("offset", Integer.valueOf(tz.getOffset(this.mNow.getTime())));
            this.mZones.add(map);
        }
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

    private static class MyComparator implements Comparator<HashMap<?, ?>> {
        private String mSortingKey;

        public MyComparator(String sortingKey) {
            this.mSortingKey = sortingKey;
        }

        @Override
        public int compare(HashMap<?, ?> map1, HashMap<?, ?> map2) {
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
            return value != null && (value instanceof Comparable);
        }
    }
}
