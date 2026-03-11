package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.net.ChartData;
import libcore.util.Objects;

public class CycleAdapter extends ArrayAdapter<CycleItem> {
    private final AdapterView.OnItemSelectedListener mListener;
    private final SpinnerInterface mSpinner;

    public interface SpinnerInterface {
        Object getSelectedItem();

        void setAdapter(CycleAdapter cycleAdapter);

        void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener);

        void setSelection(int i);
    }

    public CycleAdapter(Context context, SpinnerInterface spinner, AdapterView.OnItemSelectedListener listener, boolean isHeader) {
        super(context, isHeader ? R.layout.filter_spinner_item : R.layout.data_usage_cycle_item);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mSpinner = spinner;
        this.mListener = listener;
        this.mSpinner.setAdapter(this);
        this.mSpinner.setOnItemSelectedListener(this.mListener);
    }

    public int findNearestPosition(CycleItem target) {
        if (target != null) {
            int count = getCount();
            for (int i = count - 1; i >= 0; i--) {
                CycleItem item = getItem(i);
                if (item.compareTo(target) >= 0) {
                    return i;
                }
            }
        }
        return 0;
    }

    public boolean updateCycleList(NetworkPolicy policy, ChartData chartData) {
        boolean includeCycle;
        boolean includeCycle2;
        CycleItem previousItem = (CycleItem) this.mSpinner.getSelectedItem();
        clear();
        Context context = getContext();
        NetworkStatsHistory.Entry entry = null;
        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (chartData != null) {
            historyStart = chartData.network.getStart();
            historyEnd = chartData.network.getEnd();
        }
        long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) {
            historyStart = now;
        }
        if (historyEnd == Long.MIN_VALUE) {
            historyEnd = now + 1;
        }
        boolean hasCycles = false;
        if (policy != null) {
            long cycleEnd = NetworkPolicyManager.computeNextCycleBoundary(historyEnd, policy);
            while (cycleEnd > historyStart) {
                long cycleStart = NetworkPolicyManager.computeLastCycleBoundary(cycleEnd, policy);
                if (chartData != null) {
                    entry = chartData.network.getValues(cycleStart, cycleEnd, entry);
                    includeCycle2 = entry.rxBytes + entry.txBytes > 0;
                } else {
                    includeCycle2 = true;
                }
                if (includeCycle2) {
                    add(new CycleItem(context, cycleStart, cycleEnd));
                    hasCycles = true;
                }
                cycleEnd = cycleStart;
            }
        }
        if (!hasCycles) {
            long cycleEnd2 = historyEnd;
            while (cycleEnd2 > historyStart) {
                long cycleStart2 = cycleEnd2 - 2419200000L;
                if (chartData != null) {
                    entry = chartData.network.getValues(cycleStart2, cycleEnd2, entry);
                    includeCycle = entry.rxBytes + entry.txBytes > 0;
                } else {
                    includeCycle = true;
                }
                if (includeCycle) {
                    add(new CycleItem(context, cycleStart2, cycleEnd2));
                }
                cycleEnd2 = cycleStart2;
            }
        }
        if (getCount() > 0) {
            int position = findNearestPosition(previousItem);
            this.mSpinner.setSelection(position);
            CycleItem selectedItem = getItem(position);
            if (!Objects.equal(selectedItem, previousItem)) {
                this.mListener.onItemSelected(null, null, position, 0L);
                return false;
            }
            return true;
        }
        return true;
    }

    public static class CycleItem implements Comparable<CycleItem> {
        public long end;
        public CharSequence label;
        public long start;

        public CycleItem(Context context, long start, long end) {
            this.label = Utils.formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        public String toString() {
            return this.label.toString();
        }

        public boolean equals(Object o) {
            if (!(o instanceof CycleItem)) {
                return false;
            }
            CycleItem another = (CycleItem) o;
            return this.start == another.start && this.end == another.end;
        }

        @Override
        public int compareTo(CycleItem another) {
            return Long.compare(this.start, another.start);
        }
    }
}
