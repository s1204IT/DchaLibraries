package com.android.deskclock.stopwatch;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.uidata.UiDataModel;
import java.text.DecimalFormatSymbols;
import java.util.List;

class LapsAdapter extends RecyclerView.Adapter<LapItemHolder> {
    private static final long HOUR = 3600000;
    private static final long HUNDRED_HOURS = 360000000;
    private static final String LRM_SPACE = "\u200e ";
    private static final long TEN_HOURS = 36000000;
    private static final long TEN_MINUTES = 600000;
    private static final StringBuilder sTimeBuilder = new StringBuilder(12);
    private final Context mContext;
    private final LayoutInflater mInflater;
    private int mLastFormattedAccumulatedTimeLength;
    private int mLastFormattedLapTimeLength;

    LapsAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        int size = getLaps().size();
        return (size == 0 ? 0 : 1) + size;
    }

    @Override
    public LapItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new LapItemHolder(this.mInflater.inflate(R.layout.lap_view, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(LapItemHolder lapItemHolder, int i) {
        long totalTime;
        long currentLapTime;
        int size;
        Lap lap = i == 0 ? null : getLaps().get(i - 1);
        if (lap != null) {
            currentLapTime = lap.getLapTime();
            size = lap.getLapNumber();
            totalTime = lap.getAccumulatedTime();
        } else {
            totalTime = getStopwatch().getTotalTime();
            currentLapTime = DataModel.getDataModel().getCurrentLapTime(totalTime);
            size = getLaps().size() + 1;
        }
        lapItemHolder.lapTime.setText(formatLapTime(currentLapTime, true));
        lapItemHolder.accumulatedTime.setText(formatAccumulatedTime(totalTime, true));
        lapItemHolder.lapNumber.setText(formatLapNumber(getLaps().size() + 1, size));
    }

    @Override
    public long getItemId(int i) {
        List<Lap> laps = getLaps();
        if (i == 0) {
            return laps.size() + 1;
        }
        return laps.get(i - 1).getLapNumber();
    }

    void updateCurrentLap(RecyclerView recyclerView, long j) {
        View childAt;
        if (getItemCount() != 0 && (childAt = recyclerView.getChildAt(0)) != null) {
            long currentLapTime = DataModel.getDataModel().getCurrentLapTime(j);
            LapItemHolder lapItemHolder = (LapItemHolder) recyclerView.getChildViewHolder(childAt);
            lapItemHolder.lapTime.setText(formatLapTime(currentLapTime, false));
            lapItemHolder.accumulatedTime.setText(formatAccumulatedTime(j, false));
        }
    }

    Lap addLap() {
        Lap lapAddLap = DataModel.getDataModel().addLap();
        if (getItemCount() == 10) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(0);
            notifyItemChanged(1);
        }
        return lapAddLap;
    }

    void clearLaps() {
        this.mLastFormattedLapTimeLength = 0;
        this.mLastFormattedAccumulatedTimeLength = 0;
        notifyDataSetChanged();
    }

    String getShareText() {
        long totalTime = getStopwatch().getTotalTime();
        String time = formatTime(totalTime, totalTime, ":");
        StringBuilder sb = new StringBuilder(1000);
        sb.append(this.mContext.getString(R.string.sw_share_main, time));
        sb.append("\n");
        List<Lap> laps = getLaps();
        if (!laps.isEmpty()) {
            sb.append(this.mContext.getString(R.string.sw_share_laps));
            sb.append("\n");
            String str = DecimalFormatSymbols.getInstance().getDecimalSeparator() + " ";
            for (int size = laps.size() - 1; size >= 0; size--) {
                Lap lap = laps.get(size);
                sb.append(lap.getLapNumber());
                sb.append(str);
                long lapTime = lap.getLapTime();
                sb.append(formatTime(lapTime, lapTime, " "));
                sb.append("\n");
            }
            sb.append(laps.size() + 1);
            sb.append(str);
            long currentLapTime = DataModel.getDataModel().getCurrentLapTime(totalTime);
            sb.append(formatTime(currentLapTime, currentLapTime, " "));
            sb.append("\n");
        }
        return sb.toString();
    }

    @VisibleForTesting
    String formatLapNumber(int i, int i2) {
        return i < 10 ? this.mContext.getString(R.string.lap_number_single_digit, Integer.valueOf(i2)) : this.mContext.getString(R.string.lap_number_double_digit, Integer.valueOf(i2));
    }

    @VisibleForTesting
    static String formatTime(long j, long j2, String str) {
        int i;
        int i2;
        int i3;
        int i4;
        if (j2 > 0) {
            i = (int) (j2 / HOUR);
            long j3 = (int) (j2 % HOUR);
            i2 = (int) (j3 / 60000);
            long j4 = (int) (j3 % 60000);
            i3 = (int) (j4 / 1000);
            i4 = ((int) (j4 % 1000)) / 10;
        } else {
            i4 = 0;
            i = 0;
            i2 = 0;
            i3 = 0;
        }
        char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        sTimeBuilder.setLength(0);
        if (j < TEN_MINUTES) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i2, 1));
        } else if (j < HOUR) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i2, 2));
        } else if (j < TEN_HOURS) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i, 1));
            sTimeBuilder.append(str);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i2, 2));
        } else if (j < HUNDRED_HOURS) {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i, 2));
            sTimeBuilder.append(str);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i2, 2));
        } else {
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i, 3));
            sTimeBuilder.append(str);
            sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i2, 2));
        }
        sTimeBuilder.append(str);
        sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i3, 2));
        sTimeBuilder.append(decimalSeparator);
        sTimeBuilder.append(UiDataModel.getUiDataModel().getFormattedNumber(i4, 2));
        return sTimeBuilder.toString();
    }

    private String formatLapTime(long j, boolean z) {
        String time = formatTime(Math.max(DataModel.getDataModel().getLongestLapTime(), j), j, LRM_SPACE);
        int length = time.length();
        if (!z && this.mLastFormattedLapTimeLength != length) {
            this.mLastFormattedLapTimeLength = length;
            notifyDataSetChanged();
        }
        return time;
    }

    private String formatAccumulatedTime(long j, boolean z) {
        String time = formatTime(Math.max(getStopwatch().getTotalTime(), j), j, LRM_SPACE);
        int length = time.length();
        if (!z && this.mLastFormattedAccumulatedTimeLength != length) {
            this.mLastFormattedAccumulatedTimeLength = length;
            notifyDataSetChanged();
        }
        return time;
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private List<Lap> getLaps() {
        return DataModel.getDataModel().getLaps();
    }

    static final class LapItemHolder extends RecyclerView.ViewHolder {
        private final TextView accumulatedTime;
        private final TextView lapNumber;
        private final TextView lapTime;

        LapItemHolder(View view) {
            super(view);
            this.lapTime = (TextView) view.findViewById(R.id.lap_time);
            this.lapNumber = (TextView) view.findViewById(R.id.lap_number);
            this.accumulatedTime = (TextView) view.findViewById(R.id.lap_total);
        }
    }
}
