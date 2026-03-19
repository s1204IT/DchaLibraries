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

        static void setupViewHolder(View view) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.timeZone = (TextView) view.findViewById(R.id.time_zone);
            viewHolder.timeOffset = (TextView) view.findViewById(R.id.time_offset);
            viewHolder.location = (TextView) view.findViewById(R.id.location);
            view.setTag(viewHolder);
        }
    }

    public TimeZoneResultAdapter(Context context, TimeZoneData timeZoneData, TimeZonePickerView.OnTimeZoneSetListener onTimeZoneSetListener) {
        this.mContext = context;
        this.mTimeZoneData = timeZoneData;
        this.mTimeZoneSetListener = onTimeZoneSetListener;
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

    public int getLastFilterTime() {
        return this.mLastFilterTime;
    }

    @Override
    public void onSetFilter(int i, String str, int i2) {
        int iFindIndexByTimeZoneIdSlow;
        this.mLastFilterType = i;
        this.mLastFilterString = str;
        this.mLastFilterTime = i2;
        boolean z = false;
        this.mFilteredTimeZoneLength = 0;
        switch (i) {
            case -1:
                int[] iArr = this.mFilteredTimeZoneIndices;
                int i3 = this.mFilteredTimeZoneLength;
                this.mFilteredTimeZoneLength = i3 + 1;
                iArr[i3] = -100;
                break;
            case 0:
                int defaultTimeZoneIndex = this.mTimeZoneData.getDefaultTimeZoneIndex();
                if (defaultTimeZoneIndex != -1) {
                    int[] iArr2 = this.mFilteredTimeZoneIndices;
                    int i4 = this.mFilteredTimeZoneLength;
                    this.mFilteredTimeZoneLength = i4 + 1;
                    iArr2[i4] = defaultTimeZoneIndex;
                }
                String string = this.mContext.getSharedPreferences("com.android.calendar_preferences", 0).getString("preferences_recent_timezones", null);
                if (!TextUtils.isEmpty(string)) {
                    String[] strArrSplit = string.split(",");
                    for (int length = strArrSplit.length - 1; length >= 0; length--) {
                        if (!TextUtils.isEmpty(strArrSplit[length]) && !strArrSplit[length].equals(this.mTimeZoneData.mDefaultTimeZoneId) && (iFindIndexByTimeZoneIdSlow = this.mTimeZoneData.findIndexByTimeZoneIdSlow(strArrSplit[length])) != -1) {
                            int[] iArr3 = this.mFilteredTimeZoneIndices;
                            int i5 = this.mFilteredTimeZoneLength;
                            this.mFilteredTimeZoneLength = i5 + 1;
                            iArr3[i5] = iFindIndexByTimeZoneIdSlow;
                        }
                    }
                }
                break;
            case 1:
                ArrayList<Integer> arrayList = this.mTimeZoneData.mTimeZonesByCountry.get(str);
                if (arrayList != null) {
                    for (Integer num : arrayList) {
                        int[] iArr4 = this.mFilteredTimeZoneIndices;
                        int i6 = this.mFilteredTimeZoneLength;
                        this.mFilteredTimeZoneLength = i6 + 1;
                        iArr4[i6] = num.intValue();
                    }
                }
                break;
            case 2:
                break;
            case 3:
                ArrayList<Integer> timeZonesByOffset = this.mTimeZoneData.getTimeZonesByOffset(i2);
                if (timeZonesByOffset != null) {
                    for (Integer num2 : timeZonesByOffset) {
                        int[] iArr5 = this.mFilteredTimeZoneIndices;
                        int i7 = this.mFilteredTimeZoneLength;
                        this.mFilteredTimeZoneLength = i7 + 1;
                        iArr5[i7] = num2.intValue();
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (this.mFilteredTimeZoneLength > 0) {
            z = true;
        }
        this.mHasResults = z;
        notifyDataSetChanged();
    }

    public void saveRecentTimezone(String str) {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("com.android.calendar_preferences", 0);
        String string = sharedPreferences.getString("preferences_recent_timezones", null);
        if (string != null) {
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
            for (String str2 : string.split(",")) {
                if (!linkedHashSet.contains(str2) && !str.equals(str2)) {
                    linkedHashSet.add(str2);
                }
            }
            Iterator it = linkedHashSet.iterator();
            while (linkedHashSet.size() >= 3 && it.hasNext()) {
                it.next();
                it.remove();
            }
            linkedHashSet.add(str);
            StringBuilder sb = new StringBuilder();
            boolean z = true;
            for (String str3 : linkedHashSet) {
                if (!z) {
                    sb.append(",");
                } else {
                    z = false;
                }
                sb.append(str3);
            }
            str = sb.toString();
        }
        sharedPreferences.edit().putString("preferences_recent_timezones", str).apply();
    }

    @Override
    public int getCount() {
        return this.mFilteredTimeZoneLength;
    }

    @Override
    public Object getItem(int i) {
        if (i < 0 || i >= this.mFilteredTimeZoneLength) {
            return null;
        }
        return this.mTimeZoneData.get(this.mFilteredTimeZoneIndices[i]);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return this.mFilteredTimeZoneIndices[i] >= 0;
    }

    @Override
    public long getItemId(int i) {
        return this.mFilteredTimeZoneIndices[i];
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (this.mFilteredTimeZoneIndices[i] == -100) {
            return this.mInflater.inflate(R.layout.empty_time_zone_item, (ViewGroup) null);
        }
        if (view == null || view.findViewById(R.id.empty_item) != null) {
            view = this.mInflater.inflate(R.layout.time_zone_item, (ViewGroup) null);
            ViewHolder.setupViewHolder(view);
        }
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        TimeZoneInfo timeZoneInfo = this.mTimeZoneData.get(this.mFilteredTimeZoneIndices[i]);
        view.setTag(VIEW_TAG_TIME_ZONE, timeZoneInfo);
        viewHolder.timeZone.setText(timeZoneInfo.mDisplayName);
        viewHolder.timeOffset.setText(timeZoneInfo.getGmtDisplayName(this.mContext));
        String str = timeZoneInfo.mCountry;
        if (str == null) {
            viewHolder.location.setVisibility(4);
        } else {
            viewHolder.location.setText(str);
            viewHolder.location.setVisibility(0);
        }
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        TimeZoneInfo timeZoneInfo;
        if (this.mTimeZoneSetListener != null && (timeZoneInfo = (TimeZoneInfo) view.getTag(VIEW_TAG_TIME_ZONE)) != null) {
            this.mTimeZoneSetListener.onTimeZoneSet(timeZoneInfo);
            saveRecentTimezone(timeZoneInfo.mTzId);
        }
    }
}
