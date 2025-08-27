package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.util.Pair;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.net.ChartData;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
public class CycleAdapter extends ArrayAdapter<CycleItem> {
    private final AdapterView.OnItemSelectedListener mListener;
    private final SpinnerInterface mSpinner;

    public interface SpinnerInterface {
        Object getSelectedItem();

        void setAdapter(CycleAdapter cycleAdapter);

        void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener);

        void setSelection(int i);
    }

    public CycleAdapter(Context context, SpinnerInterface spinnerInterface, AdapterView.OnItemSelectedListener onItemSelectedListener, boolean z) {
        super(context, z ? R.layout.filter_spinner_item : R.layout.data_usage_cycle_item);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mSpinner = spinnerInterface;
        this.mListener = onItemSelectedListener;
        this.mSpinner.setAdapter(this);
        this.mSpinner.setOnItemSelectedListener(this.mListener);
    }

    public int findNearestPosition(CycleItem cycleItem) {
        if (cycleItem != null) {
            for (int count = getCount() - 1; count >= 0; count--) {
                if (getItem(count).compareTo(cycleItem) >= 0) {
                    return count;
                }
            }
            return 0;
        }
        return 0;
    }

    public boolean updateCycleList(NetworkPolicy networkPolicy, ChartData chartData) {
        long end;
        long start;
        NetworkStatsHistory.Entry entry;
        boolean z;
        boolean z2;
        Iterator it;
        boolean z3;
        Iterator it2;
        ChartData chartData2 = chartData;
        CycleItem cycleItem = (CycleItem) this.mSpinner.getSelectedItem();
        clear();
        Context context = getContext();
        if (chartData2 != null) {
            start = chartData2.network.getStart();
            end = chartData2.network.getEnd();
        } else {
            end = Long.MIN_VALUE;
            start = Long.MAX_VALUE;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = start == Long.MAX_VALUE ? jCurrentTimeMillis : start;
        if (end == Long.MIN_VALUE) {
            end = jCurrentTimeMillis + 1;
        }
        if (networkPolicy != null) {
            Iterator itCycleIterator = NetworkPolicyManager.cycleIterator(networkPolicy);
            entry = null;
            z = false;
            while (itCycleIterator.hasNext()) {
                Pair pair = (Pair) itCycleIterator.next();
                long epochMilli = ((ZonedDateTime) pair.first).toInstant().toEpochMilli();
                long epochMilli2 = ((ZonedDateTime) pair.second).toInstant().toEpochMilli();
                if (chartData2 != null) {
                    NetworkStatsHistory.Entry values = chartData2.network.getValues(epochMilli, epochMilli2, entry);
                    it = itCycleIterator;
                    z3 = values.rxBytes + values.txBytes > 0;
                    entry = values;
                } else {
                    it = itCycleIterator;
                    z3 = true;
                }
                if (z3) {
                    it2 = it;
                    add(new CycleItem(context, epochMilli, epochMilli2));
                    z = true;
                } else {
                    it2 = it;
                }
                itCycleIterator = it2;
            }
        } else {
            entry = null;
            z = false;
        }
        if (!z) {
            NetworkStatsHistory.Entry entry2 = entry;
            while (end > j) {
                long j2 = end - 2419200000L;
                if (chartData2 != null) {
                    NetworkStatsHistory.Entry values2 = chartData2.network.getValues(j2, end, entry2);
                    z2 = values2.rxBytes + values2.txBytes > 0;
                    entry2 = values2;
                } else {
                    z2 = true;
                }
                if (z2) {
                    add(new CycleItem(context, j2, end));
                }
                end = j2;
                chartData2 = chartData;
            }
        }
        if (getCount() > 0) {
            int iFindNearestPosition = findNearestPosition(cycleItem);
            this.mSpinner.setSelection(iFindNearestPosition);
            if (!Objects.equals(getItem(iFindNearestPosition), cycleItem)) {
                this.mListener.onItemSelected(null, null, iFindNearestPosition, 0L);
                return false;
            }
        }
        return true;
    }

    public static class CycleItem implements Comparable<CycleItem> {
        public long end;
        public CharSequence label;
        public long start;

        public CycleItem(Context context, long j, long j2) {
            this.label = Utils.formatDateRange(context, j, j2);
            this.start = j;
            this.end = j2;
        }

        public String toString() {
            return this.label.toString();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof CycleItem)) {
                return false;
            }
            CycleItem cycleItem = (CycleItem) obj;
            return this.start == cycleItem.start && this.end == cycleItem.end;
        }

        /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
        @Override // java.lang.Comparable
        public int compareTo(CycleItem cycleItem) {
            return Long.compare(this.start, cycleItem.start);
        }
    }
}
