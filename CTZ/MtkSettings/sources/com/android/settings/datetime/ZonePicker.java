package com.android.settings.datetime;

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
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.datetime.ZoneGetter;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ZonePicker extends ListFragment implements Instrumentable {
    private SimpleAdapter mAlphabeticalAdapter;
    private boolean mSortedByTimezone;
    private SimpleAdapter mTimezoneSortedAdapter;
    private VisibilityLoggerMixin mVisibilityLoggerMixin;

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean z) {
        return constructTimezoneAdapter(context, z, R.layout.date_time_custom_list_item_2);
    }

    public static SimpleAdapter constructTimezoneAdapter(Context context, boolean z, int i) {
        String str;
        String[] strArr = {"display_label", "offset_label"};
        int[] iArr = {android.R.id.text1, android.R.id.text2};
        if (z) {
            str = "display_label";
        } else {
            str = "offset";
        }
        MyComparator myComparator = new MyComparator(str);
        List<Map<String, Object>> zonesList = ZoneGetter.getZonesList(context);
        Collections.sort(zonesList, myComparator);
        SimpleAdapter simpleAdapter = new SimpleAdapter(context, zonesList, i, strArr, iArr);
        simpleAdapter.setViewBinder(new TimeZoneViewBinder());
        return simpleAdapter;
    }

    private static class TimeZoneViewBinder implements SimpleAdapter.ViewBinder {
        private TimeZoneViewBinder() {
        }

        @Override
        public boolean setViewValue(View view, Object obj, String str) {
            ((TextView) view).setText((CharSequence) obj);
            return true;
        }
    }

    public static int getTimeZoneIndex(SimpleAdapter simpleAdapter, TimeZone timeZone) {
        String id = timeZone.getID();
        int count = simpleAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (id.equals((String) ((HashMap) simpleAdapter.getItem(i)).get("id"))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getMetricsCategory() {
        return 515;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Activity activity = getActivity();
        this.mTimezoneSortedAdapter = constructTimezoneAdapter(activity, false);
        this.mAlphabeticalAdapter = constructTimezoneAdapter(activity, true);
        setSorting(true);
        setHasOptionsMenu(true);
        activity.setTitle(R.string.date_time_set_timezone);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(), FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        prepareCustomPreferencesList((ListView) viewOnCreateView.findViewById(android.R.id.list));
        return viewOnCreateView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 1, 0, R.string.zone_list_menu_sort_alphabetically).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, 2, 0, R.string.zone_list_menu_sort_by_timezone).setIcon(R.drawable.ic_menu_3d_globe);
        super.onCreateOptionsMenu(menu, menuInflater);
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
    public void onResume() {
        super.onResume();
        this.mVisibilityLoggerMixin.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
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

    static void prepareCustomPreferencesList(ListView listView) {
        listView.setScrollBarStyle(33554432);
        listView.setClipToPadding(false);
        listView.setDivider(null);
    }

    private void setSorting(boolean z) {
        SimpleAdapter simpleAdapter = z ? this.mTimezoneSortedAdapter : this.mAlphabeticalAdapter;
        setListAdapter(simpleAdapter);
        this.mSortedByTimezone = z;
        int timeZoneIndex = getTimeZoneIndex(simpleAdapter, TimeZone.getDefault());
        if (timeZoneIndex >= 0) {
            setSelection(timeZoneIndex);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int i, long j) {
        if (isResumed()) {
            ((AlarmManager) getActivity().getSystemService("alarm")).setTimeZone((String) ((Map) listView.getItemAtPosition(i)).get("id"));
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mVisibilityLoggerMixin.onPause();
    }

    static class MyComparator implements Comparator<Map<?, ?>> {
        private final Collator mCollator = Collator.getInstance();
        private boolean mSortedByName;
        private String mSortingKey;

        public MyComparator(String str) {
            this.mSortingKey = str;
            this.mSortedByName = "display_label".equals(str);
        }

        @Override
        public int compare(Map<?, ?> map, Map<?, ?> map2) {
            Object obj = map.get(this.mSortingKey);
            Object obj2 = map2.get(this.mSortingKey);
            if (!isComparable(obj)) {
                return isComparable(obj2) ? 1 : 0;
            }
            if (!isComparable(obj2)) {
                return -1;
            }
            if (this.mSortedByName) {
                return this.mCollator.compare(obj, obj2);
            }
            return ((Comparable) obj).compareTo(obj2);
        }

        private boolean isComparable(Object obj) {
            return obj != null && (obj instanceof Comparable);
        }
    }
}
