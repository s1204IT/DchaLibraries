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
        public void onClick(View view) {
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

        static void setupViewHolder(View view) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.strTextView = (TextView) view.findViewById(R.id.value);
            view.setTag(viewHolder);
        }
    }

    class FilterTypeResult {
        String constraint;
        public int time;
        int type;

        public FilterTypeResult(int i, String str, int i2) {
            this.type = i;
            this.constraint = str;
            this.time = i2;
        }

        public String toString() {
            return this.constraint;
        }
    }

    public TimeZoneFilterTypeAdapter(Context context, TimeZoneData timeZoneData, OnSetFilterListener onSetFilterListener) {
        this.mTimeZoneData = timeZoneData;
        this.mListener = onSetFilterListener;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    public int getCount() {
        return this.mLiveResultsCount;
    }

    @Override
    public FilterTypeResult getItem(int i) {
        return this.mLiveResults.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(R.layout.time_zone_filter_item, (ViewGroup) null);
            ViewHolder.setupViewHolder(view);
        }
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (i >= this.mLiveResults.size()) {
            Log.e("TimeZoneFilterTypeAdapter", "getView: " + i + " of " + this.mLiveResults.size());
        }
        FilterTypeResult filterTypeResult = this.mLiveResults.get(i);
        viewHolder.filterType = filterTypeResult.type;
        viewHolder.str = filterTypeResult.constraint;
        viewHolder.time = filterTypeResult.time;
        viewHolder.strTextView.setText(filterTypeResult.constraint);
        return view;
    }

    @Override
    public void onClick(View view) {
        if (this.mListener != null && view != null) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            this.mListener.onSetFilter(viewHolder.filterType, viewHolder.str, viewHolder.time);
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
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            String lowerCase;
            int i;
            boolean z;
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (charSequence != null) {
                lowerCase = charSequence.toString().trim().toLowerCase();
            } else {
                lowerCase = null;
            }
            if (TextUtils.isEmpty(lowerCase)) {
                filterResults.values = null;
                filterResults.count = 0;
                return filterResults;
            }
            ArrayList<FilterTypeResult> arrayList = new ArrayList<>();
            if (lowerCase.charAt(0) == '+' || lowerCase.charAt(0) == '-') {
            }
            if (lowerCase.startsWith("gmt")) {
                i = 3;
            } else {
                i = 0;
            }
            int num = parseNum(lowerCase, i);
            if (num != Integer.MIN_VALUE) {
                handleSearchByGmt(arrayList, num, lowerCase.length() > i && lowerCase.charAt(i) == '+');
            }
            ArrayList arrayList2 = new ArrayList();
            for (String str : TimeZoneFilterTypeAdapter.this.mTimeZoneData.mTimeZonesByCountry.keySet()) {
                if (!TextUtils.isEmpty(str)) {
                    String lowerCase2 = str.toLowerCase();
                    if (!lowerCase2.startsWith(lowerCase) && (lowerCase2.charAt(0) != lowerCase.charAt(0) || !isStartingInitialsFor(lowerCase, lowerCase2))) {
                        if (lowerCase2.contains(" ")) {
                            for (String str2 : lowerCase2.split(" ")) {
                                if (str2.startsWith(lowerCase)) {
                                    z = true;
                                    break;
                                }
                            }
                        }
                        z = false;
                        if (!z) {
                        }
                    } else {
                        z = true;
                        if (!z) {
                            arrayList2.add(str);
                        }
                    }
                }
            }
            if (arrayList2.size() > 0) {
                Collections.sort(arrayList2);
                Iterator it = arrayList2.iterator();
                while (it.hasNext()) {
                    arrayList.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(1, (String) it.next(), 0));
                }
            }
            filterResults.values = arrayList;
            filterResults.count = arrayList.size();
            return filterResults;
        }

        private boolean isStartingInitialsFor(String str, String str2) {
            int length = str.length();
            int length2 = str2.length();
            int i = 0;
            boolean z = true;
            for (int i2 = 0; i2 < length2; i2++) {
                if (!Character.isLetter(str2.charAt(i2))) {
                    z = true;
                } else if (z) {
                    int i3 = i + 1;
                    if (str.charAt(i) != str2.charAt(i2)) {
                        return false;
                    }
                    if (i3 == length) {
                        return true;
                    }
                    i = i3;
                    z = false;
                } else {
                    continue;
                }
            }
            return str.equals("usa") && str2.equals("united states");
        }

        private void handleSearchByGmt(ArrayList<FilterTypeResult> arrayList, int i, boolean z) {
            if (i >= 0) {
                if (i == 1) {
                    for (int i2 = 19; i2 >= 10; i2--) {
                        if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i2)) {
                            arrayList.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT+" + i2, i2));
                        }
                    }
                }
                if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                    arrayList.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT+" + i, i));
                }
                i *= -1;
            }
            if (!z && i != 0) {
                if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                    arrayList.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT" + i, i));
                }
                if (i == -1) {
                    for (int i3 = -10; i3 >= -19; i3--) {
                        if (TimeZoneFilterTypeAdapter.this.mTimeZoneData.hasTimeZonesInHrOffset(i3)) {
                            arrayList.add(TimeZoneFilterTypeAdapter.this.new FilterTypeResult(3, "GMT" + i3, i3));
                        }
                    }
                }
            }
        }

        public int parseNum(String str, int i) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            int i3 = 1;
            if (cCharAt == '+') {
                if (i2 < str.length()) {
                    return Integer.MIN_VALUE;
                }
                int i4 = i2 + 1;
                char cCharAt2 = str.charAt(i2);
                i2 = i4;
                cCharAt = cCharAt2;
            } else if (cCharAt == '-') {
                i3 = -1;
                if (i2 < str.length()) {
                }
            }
            if (!Character.isDigit(cCharAt)) {
                return Integer.MIN_VALUE;
            }
            int iDigit = Character.digit(cCharAt, 10);
            if (i2 < str.length()) {
                int i5 = i2 + 1;
                char cCharAt3 = str.charAt(i2);
                if (!Character.isDigit(cCharAt3)) {
                    return Integer.MIN_VALUE;
                }
                iDigit = (iDigit * 10) + Character.digit(cCharAt3, 10);
                i2 = i5;
            }
            if (i2 != str.length()) {
                return Integer.MIN_VALUE;
            }
            return i3 * iDigit;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            int i;
            if (filterResults.values == null || filterResults.count == 0) {
                if (TimeZoneFilterTypeAdapter.this.mListener != null) {
                    if (!TextUtils.isEmpty(charSequence)) {
                        i = -1;
                    } else {
                        i = 0;
                    }
                    TimeZoneFilterTypeAdapter.this.mListener.onSetFilter(i, null, 0);
                }
            } else {
                TimeZoneFilterTypeAdapter.this.mLiveResults = (ArrayList) filterResults.values;
            }
            TimeZoneFilterTypeAdapter.this.mLiveResultsCount = filterResults.count;
            if (filterResults.count > 0) {
                TimeZoneFilterTypeAdapter.this.notifyDataSetChanged();
            } else {
                TimeZoneFilterTypeAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
