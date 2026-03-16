package com.android.timezonepicker;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class TimeZoneFilterTypeAdapter extends BaseAdapter implements View.OnClickListener, Filterable {
    private ArrayFilter mFilter;
    private LayoutInflater mInflater;
    private OnSetFilterListener mListener;
    private TimeZoneData mTimeZoneData;
    private ArrayList<FilterTypeResult> mLiveResults = new ArrayList<>();
    private int mLiveResultsCount = 0;
    View.OnClickListener mDummyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    public interface OnSetFilterListener {
        void onSetFilter(int i, String str, int i2);
    }

    static class ViewHolder {
        int filterType;
        String str;
        TextView strTextView;
        int time;

        ViewHolder() {
        }

        static void setupViewHolder(View v) {
            ViewHolder vh = new ViewHolder();
            vh.strTextView = (TextView) v.findViewById(R.id.value);
            v.setTag(vh);
        }
    }

    class FilterTypeResult {
        String constraint;
        public int time;
        int type;

        public FilterTypeResult(int type, String constraint, int time) {
            this.type = type;
            this.constraint = constraint;
            this.time = time;
        }

        public String toString() {
            return this.constraint;
        }
    }

    public TimeZoneFilterTypeAdapter(Context context, TimeZoneData tzd, OnSetFilterListener l) {
        this.mTimeZoneData = tzd;
        this.mListener = l;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    public int getCount() {
        return this.mLiveResultsCount;
    }

    @Override
    public FilterTypeResult getItem(int position) {
        return this.mLiveResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView != null) {
            v = convertView;
        } else {
            v = this.mInflater.inflate(R.layout.time_zone_filter_item, (ViewGroup) null);
            ViewHolder.setupViewHolder(v);
        }
        ViewHolder vh = (ViewHolder) v.getTag();
        if (position >= this.mLiveResults.size()) {
            Log.e("TimeZoneFilterTypeAdapter", "getView: " + position + " of " + this.mLiveResults.size());
        }
        FilterTypeResult filter = this.mLiveResults.get(position);
        vh.filterType = filter.type;
        vh.str = filter.constraint;
        vh.time = filter.time;
        vh.strTextView.setText(filter.constraint);
        return v;
    }

    @Override
    public void onClick(View v) {
        if (this.mListener != null && v != null) {
            ViewHolder vh = (ViewHolder) v.getTag();
            this.mListener.onSetFilter(vh.filterType, vh.str, vh.time);
        }
        notifyDataSetInvalidated();
    }

    @Override
    public Filter getFilter() {
        if (this.mFilter == null) {
            this.mFilter = new ArrayFilter();
        }
        return this.mFilter;
    }

    private class ArrayFilter extends Filter {
        private ArrayFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence prefix) {
            Filter.FilterResults results = new Filter.FilterResults();
            String prefixString = null;
            if (prefix != null) {
                prefixString = prefix.toString().trim().toLowerCase();
            }
            if (TextUtils.isEmpty(prefixString)) {
                results.values = null;
                results.count = 0;
            } else {
                ArrayList<FilterTypeResult> filtered = new ArrayList<>();
                int startParsePosition = 0;
                if (prefixString.charAt(0) == '+' || prefixString.charAt(0) == '-') {
                }
                if (prefixString.startsWith("gmt")) {
                    startParsePosition = 3;
                }
                int num = parseNum(prefixString, startParsePosition);
                if (num != Integer.MIN_VALUE) {
                    boolean positiveOnly = prefixString.length() > startParsePosition && prefixString.charAt(startParsePosition) == '+';
                    handleSearchByGmt(filtered, num, positiveOnly);
                }
                ArrayList<String> countries = new ArrayList<>();
                for (String country : TimeZoneFilterTypeAdapter.this.mTimeZoneData.mTimeZonesByCountry.keySet()) {
                    if (!TextUtils.isEmpty(country)) {
                        String lowerCaseCountry = country.toLowerCase();
                        boolean isMatch = false;
                        if (lowerCaseCountry.startsWith(prefixString) || (lowerCaseCountry.charAt(0) == prefixString.charAt(0) && isStartingInitialsFor(prefixString, lowerCaseCountry))) {
                            isMatch = true;
                        } else if (lowerCaseCountry.contains(" ")) {
                            String[] arr$ = lowerCaseCountry.split(" ");
                            int len$ = arr$.length;
                            int i$ = 0;
                            while (true) {
                                if (i$ >= len$) {
                                    break;
                                }
                                String word = arr$[i$];
                                if (!word.startsWith(prefixString)) {
                                    i$++;
                                } else {
                                    isMatch = true;
                                    break;
                                }
                            }
                        }
                        if (isMatch) {
                            countries.add(country);
                        }
                    }
                }
                if (countries.size() > 0) {
                    Collections.sort(countries);
                    Iterator<String> it = countries.iterator();
                    while (it.hasNext()) {
                        filtered.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(1, it.next(), 0));
                    }
                }
                results.values = filtered;
                results.count = filtered.size();
            }
            return results;
        }

        private boolean isStartingInitialsFor(String prefixString, String string) {
            int initialIdx;
            int initialLen = prefixString.length();
            int strLen = string.length();
            boolean wasWordBreak = true;
            int i = 0;
            int initialIdx2 = 0;
            while (i < strLen) {
                if (!Character.isLetter(string.charAt(i))) {
                    wasWordBreak = true;
                    initialIdx = initialIdx2;
                } else if (wasWordBreak) {
                    initialIdx = initialIdx2 + 1;
                    if (prefixString.charAt(initialIdx2) != string.charAt(i)) {
                        return false;
                    }
                    if (initialIdx == initialLen) {
                        return true;
                    }
                    wasWordBreak = false;
                } else {
                    initialIdx = initialIdx2;
                }
                i++;
                initialIdx2 = initialIdx;
            }
            return prefixString.equals("usa") && string.equals("united states");
        }

        private void handleSearchByGmt(ArrayList<FilterTypeResult> filtered, int num, boolean positiveOnly) {
            if (num >= 0) {
                if (num == 1) {
                    for (int i = 19; i >= 10; i--) {
                        if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                            FilterTypeResult r = TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT+" + i, i);
                            filtered.add(r);
                        }
                    }
                }
                if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    FilterTypeResult r2 = TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT+" + num, num);
                    filtered.add(r2);
                }
                num *= -1;
            }
            if (!positiveOnly && num != 0) {
                if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    FilterTypeResult r3 = TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT" + num, num);
                    filtered.add(r3);
                }
                if (num == -1) {
                    for (int i2 = -10; i2 >= -19; i2--) {
                        if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i2)) {
                            FilterTypeResult r4 = TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT" + i2, i2);
                            filtered.add(r4);
                        }
                    }
                }
            }
        }

        public int parseNum(String str, int startIndex) {
            int idx;
            int negativeMultiplier = 1;
            int idx2 = startIndex + 1;
            char ch = str.charAt(startIndex);
            switch (ch) {
                case '+':
                    if (idx2 < str.length()) {
                        break;
                    } else {
                        idx = idx2 + 1;
                        ch = str.charAt(idx2);
                        if (!Character.isDigit(ch)) {
                            int num = Character.digit(ch, 10);
                            if (idx < str.length()) {
                                int idx3 = idx + 1;
                                char ch2 = str.charAt(idx);
                                if (Character.isDigit(ch2)) {
                                    num = (num * 10) + Character.digit(ch2, 10);
                                    idx = idx3;
                                }
                            }
                            if (idx == str.length()) {
                            }
                            break;
                        }
                    }
                    break;
                case ',':
                default:
                    idx = idx2;
                    if (!Character.isDigit(ch)) {
                    }
                    break;
                case '-':
                    negativeMultiplier = -1;
                    if (idx2 < str.length()) {
                    }
                    break;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            int filterType;
            if (results.values == null || results.count == 0) {
                if (TimeZoneFilterTypeAdapter.this.mListener != null) {
                    if (TextUtils.isEmpty(constraint)) {
                        filterType = 0;
                    } else {
                        filterType = -1;
                    }
                    TimeZoneFilterTypeAdapter.this.mListener.onSetFilter(filterType, null, 0);
                }
            } else {
                TimeZoneFilterTypeAdapter.this.mLiveResults = (ArrayList) results.values;
            }
            TimeZoneFilterTypeAdapter.this.mLiveResultsCount = results.count;
            if (results.count > 0) {
                TimeZoneFilterTypeAdapter.this.notifyDataSetChanged();
            } else {
                TimeZoneFilterTypeAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
