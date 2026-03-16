package com.android.timezonepicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.timezonepicker.TimeZoneFilterTypeAdapter;
import com.android.timezonepicker.TimeZonePickerView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class TimeZoneResultAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, TimeZoneFilterTypeAdapter.OnSetFilterListener {
    private static final int VIEW_TAG_TIME_ZONE = R.id.time_zone;
    private Context mContext;
    private int[] mFilteredTimeZoneIndices;
    private LayoutInflater mInflater;
    private String mLastFilterString;
    private int mLastFilterTime;
    private int mLastFilterType;
    private TimeZoneData mTimeZoneData;
    private TimeZonePickerView.OnTimeZoneSetListener mTimeZoneSetListener;
    private boolean mHasResults = false;
    private int mFilteredTimeZoneLength = 0;

    static class ViewHolder {
        TextView location;
        TextView timeOffset;
        TextView timeZone;

        ViewHolder() {
        }

        static void setupViewHolder(View v) {
            ViewHolder vh = new ViewHolder();
            vh.timeZone = (TextView) v.findViewById(R.id.time_zone);
            vh.timeOffset = (TextView) v.findViewById(R.id.time_offset);
            vh.location = (TextView) v.findViewById(R.id.location);
            v.setTag(vh);
        }
    }

    public TimeZoneResultAdapter(Context context, TimeZoneData tzd, TimeZonePickerView.OnTimeZoneSetListener l) {
        this.mContext = context;
        this.mTimeZoneData = tzd;
        this.mTimeZoneSetListener = l;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mFilteredTimeZoneIndices = new int[this.mTimeZoneData.size()];
        onSetFilter(0, null, 0);
    }

    public boolean hasResults() {
        return this.mHasResults;
    }

    public int getLastFilterType() {
        return this.mLastFilterType;
    }

    public String getLastFilterString() {
        return this.mLastFilterString;
    }

    @Override
    public void onSetFilter(int filterType, String str, int time) {
        int index;
        this.mLastFilterType = filterType;
        this.mLastFilterString = str;
        this.mLastFilterTime = time;
        this.mFilteredTimeZoneLength = 0;
        switch (filterType) {
            case -1:
                int[] iArr = this.mFilteredTimeZoneIndices;
                int i = this.mFilteredTimeZoneLength;
                this.mFilteredTimeZoneLength = i + 1;
                iArr[i] = -100;
                break;
            case 0:
                int defaultTzIndex = this.mTimeZoneData.getDefaultTimeZoneIndex();
                if (defaultTzIndex != -1) {
                    int[] iArr2 = this.mFilteredTimeZoneIndices;
                    int i2 = this.mFilteredTimeZoneLength;
                    this.mFilteredTimeZoneLength = i2 + 1;
                    iArr2[i2] = defaultTzIndex;
                }
                SharedPreferences prefs = this.mContext.getSharedPreferences("com.android.calendar_preferences", 0);
                String recentsString = prefs.getString("preferences_recent_timezones", null);
                if (!TextUtils.isEmpty(recentsString)) {
                    String[] recents = recentsString.split(",");
                    for (int i3 = recents.length - 1; i3 >= 0; i3--) {
                        if (!TextUtils.isEmpty(recents[i3]) && !recents[i3].equals(this.mTimeZoneData.mDefaultTimeZoneId) && (index = this.mTimeZoneData.findIndexByTimeZoneIdSlow(recents[i3])) != -1) {
                            int[] iArr3 = this.mFilteredTimeZoneIndices;
                            int i4 = this.mFilteredTimeZoneLength;
                            this.mFilteredTimeZoneLength = i4 + 1;
                            iArr3[i4] = index;
                        }
                    }
                }
                break;
            case 1:
                ArrayList<Integer> tzIds = this.mTimeZoneData.mTimeZonesByCountry.get(str);
                if (tzIds != null) {
                    for (Integer tzi : tzIds) {
                        int[] iArr4 = this.mFilteredTimeZoneIndices;
                        int i5 = this.mFilteredTimeZoneLength;
                        this.mFilteredTimeZoneLength = i5 + 1;
                        iArr4[i5] = tzi.intValue();
                    }
                }
                break;
            case 2:
                break;
            case 3:
                ArrayList<Integer> indices = this.mTimeZoneData.getTimeZonesByOffset(time);
                if (indices != null) {
                    for (Integer i6 : indices) {
                        int[] iArr5 = this.mFilteredTimeZoneIndices;
                        int i7 = this.mFilteredTimeZoneLength;
                        this.mFilteredTimeZoneLength = i7 + 1;
                        iArr5[i7] = i6.intValue();
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.mHasResults = this.mFilteredTimeZoneLength > 0;
        notifyDataSetChanged();
    }

    public void saveRecentTimezone(String id) {
        String recentsString;
        SharedPreferences prefs = this.mContext.getSharedPreferences("com.android.calendar_preferences", 0);
        String recentsString2 = prefs.getString("preferences_recent_timezones", null);
        if (recentsString2 == null) {
            recentsString = id;
        } else {
            LinkedHashSet<String> recents = new LinkedHashSet<>();
            String[] arr$ = recentsString2.split(",");
            for (String tzId : arr$) {
                if (!recents.contains(tzId) && !id.equals(tzId)) {
                    recents.add(tzId);
                }
            }
            Iterator<String> it = recents.iterator();
            while (recents.size() >= 3 && it.hasNext()) {
                it.next();
                it.remove();
            }
            recents.add(id);
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String recent : recents) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(recent);
            }
            recentsString = builder.toString();
        }
        prefs.edit().putString("preferences_recent_timezones", recentsString).apply();
    }

    @Override
    public int getCount() {
        return this.mFilteredTimeZoneLength;
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= this.mFilteredTimeZoneLength) {
            return null;
        }
        return this.mTimeZoneData.get(this.mFilteredTimeZoneIndices[position]);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return this.mFilteredTimeZoneIndices[position] >= 0;
    }

    @Override
    public long getItemId(int position) {
        return this.mFilteredTimeZoneIndices[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (this.mFilteredTimeZoneIndices[position] == -100) {
            return this.mInflater.inflate(R.layout.empty_time_zone_item, (ViewGroup) null);
        }
        if (v == null || v.findViewById(R.id.empty_item) != null) {
            v = this.mInflater.inflate(R.layout.time_zone_item, (ViewGroup) null);
            ViewHolder.setupViewHolder(v);
        }
        ViewHolder vh = (ViewHolder) v.getTag();
        TimeZoneInfo tzi = this.mTimeZoneData.get(this.mFilteredTimeZoneIndices[position]);
        v.setTag(VIEW_TAG_TIME_ZONE, tzi);
        vh.timeZone.setText(tzi.mDisplayName);
        vh.timeOffset.setText(tzi.getGmtDisplayName(this.mContext));
        String location = tzi.mCountry;
        if (location == null) {
            vh.location.setVisibility(4);
        } else {
            vh.location.setText(location);
            vh.location.setVisibility(0);
        }
        return v;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        TimeZoneInfo tzi;
        if (this.mTimeZoneSetListener != null && (tzi = (TimeZoneInfo) v.getTag(VIEW_TAG_TIME_ZONE)) != null) {
            this.mTimeZoneSetListener.onTimeZoneSet(tzi);
            saveRecentTimezone(tzi.mTzId);
        }
    }
}
