package com.android.deskclock.stopwatch;

import android.animation.LayoutTransition;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.CircleTimerView;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.timer.CountingTimerView;
import java.util.ArrayList;

public class StopwatchFragment extends DeskClockFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private View mBottomSpace;
    private CircleButtonsLayout mCircleLayout;
    private LayoutTransition mCircleLayoutTransition;
    private View mEndSpace;
    LapsListAdapter mLapsAdapter;
    private ListView mLapsList;
    private LayoutTransition mLayoutTransition;
    private boolean mSpacersUsed;
    private View mStartSpace;
    private CircleTimerView mTime;
    private CountingTimerView mTimeText;
    private PowerManager.WakeLock mWakeLock;
    int mState = 0;
    long mStartTime = 0;
    long mAccumulatedTime = 0;
    Runnable mTimeUpdateThread = new Runnable() {
        @Override
        public void run() {
            long curTime = Utils.getTimeNow();
            long totalTime = StopwatchFragment.this.mAccumulatedTime + (curTime - StopwatchFragment.this.mStartTime);
            if (StopwatchFragment.this.mTime != null) {
                StopwatchFragment.this.mTimeText.setTime(totalTime, true, true);
            }
            if (StopwatchFragment.this.mLapsAdapter.getCount() > 0) {
                StopwatchFragment.this.updateCurrentLap(totalTime);
            }
            StopwatchFragment.this.mTime.postDelayed(StopwatchFragment.this.mTimeUpdateThread, 25L);
        }
    };

    class Lap {
        public long mLapTime;
        public long mTotalTime;

        Lap(long time, long total) {
            this.mLapTime = time;
            this.mTotalTime = total;
        }

        public void updateView() {
            View lapInfo = StopwatchFragment.this.mLapsList.findViewWithTag(this);
            if (lapInfo != null) {
                StopwatchFragment.this.mLapsAdapter.setTimeText(lapInfo, this);
            }
        }
    }

    class LapsListAdapter extends BaseAdapter {
        private final String[] mFormats;
        private final LayoutInflater mInflater;
        private String mLapFormat;
        private final String[] mLapFormatSet;
        ArrayList<Lap> mLaps = new ArrayList<>();
        private final long[] mThresholds = {600000, 3600000, 36000000, 360000000, 3600000000L};
        private int mLapIndex = 0;
        private int mTotalIndex = 0;

        public LapsListAdapter(Context context) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mFormats = context.getResources().getStringArray(R.array.stopwatch_format_set);
            this.mLapFormatSet = context.getResources().getStringArray(R.array.sw_lap_number_set);
            updateLapFormat();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return position < this.mLaps.size() ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getCount() == 0) {
                return null;
            }
            if (getItemViewType(position) == 1) {
                return convertView == null ? this.mInflater.inflate(R.layout.stopwatch_spacer, parent, false) : convertView;
            }
            View lapInfo = convertView != null ? convertView : this.mInflater.inflate(R.layout.lap_view, parent, false);
            Lap lap = getItem(position);
            lapInfo.setTag(lap);
            TextView count = (TextView) lapInfo.findViewById(R.id.lap_number);
            count.setText(String.format(this.mLapFormat, Integer.valueOf(this.mLaps.size() - position)).toUpperCase());
            setTimeText(lapInfo, lap);
            return lapInfo;
        }

        protected void setTimeText(View lapInfo, Lap lap) {
            TextView lapTime = (TextView) lapInfo.findViewById(R.id.lap_time);
            TextView totalTime = (TextView) lapInfo.findViewById(R.id.lap_total);
            lapTime.setText(Stopwatches.formatTimeText(lap.mLapTime, this.mFormats[this.mLapIndex]));
            totalTime.setText(Stopwatches.formatTimeText(lap.mTotalTime, this.mFormats[this.mTotalIndex]));
        }

        @Override
        public int getCount() {
            if (this.mLaps.isEmpty()) {
                return 0;
            }
            return this.mLaps.size() + 1;
        }

        @Override
        public Lap getItem(int position) {
            if (position >= this.mLaps.size()) {
                return null;
            }
            return this.mLaps.get(position);
        }

        private void updateLapFormat() {
            this.mLapFormat = this.mLapFormatSet[this.mLaps.size() < 10 ? (char) 0 : (char) 1];
        }

        private void resetTimeFormats() {
            this.mTotalIndex = 0;
            this.mLapIndex = 0;
        }

        public boolean updateTimeFormats(Lap lap) {
            boolean formatChanged = false;
            while (this.mLapIndex + 1 < this.mThresholds.length && lap.mLapTime >= this.mThresholds[this.mLapIndex]) {
                this.mLapIndex++;
                formatChanged = true;
            }
            while (this.mTotalIndex + 1 < this.mThresholds.length && lap.mTotalTime >= this.mThresholds[this.mTotalIndex]) {
                this.mTotalIndex++;
                formatChanged = true;
            }
            return formatChanged;
        }

        public void addLap(Lap l) {
            this.mLaps.add(0, l);
        }

        public void clearLaps() {
            this.mLaps.clear();
            updateLapFormat();
            resetTimeFormats();
            notifyDataSetChanged();
        }

        public long[] getLapTimes() {
            int size = this.mLaps.size();
            if (size == 0) {
                return null;
            }
            long[] laps = new long[size];
            for (int i = 0; i < size; i++) {
                laps[i] = this.mLaps.get(i).mTotalTime;
            }
            return laps;
        }

        public void setLapTimes(long[] laps) {
            if (laps != null && laps.length != 0) {
                int size = laps.length;
                this.mLaps.clear();
                for (long lap : laps) {
                    this.mLaps.add(StopwatchFragment.this.new Lap(lap, 0L));
                }
                long totalTime = 0;
                for (int i = size - 1; i >= 0; i--) {
                    totalTime += laps[i];
                    this.mLaps.get(i).mTotalTime = totalTime;
                    updateTimeFormats(this.mLaps.get(i));
                }
                updateLapFormat();
                StopwatchFragment.this.showLaps();
                notifyDataSetChanged();
            }
        }
    }

    private void toggleStopwatchState() {
        long time = Utils.getTimeNow();
        Context context = getActivity().getApplicationContext();
        Intent intent = new Intent(context, (Class<?>) StopwatchService.class);
        intent.putExtra("message_time", time);
        intent.putExtra("show_notification", false);
        switch (this.mState) {
            case 0:
            case 2:
                doStart(time);
                intent.setAction("start_stopwatch");
                context.startService(intent);
                acquireWakeLock();
                break;
            case 1:
                long curTime = Utils.getTimeNow();
                this.mAccumulatedTime += curTime - this.mStartTime;
                doStop();
                intent.setAction("stop_stopwatch");
                context.startService(intent);
                releaseWakeLock();
                break;
            default:
                LogUtils.wtf("Illegal state " + this.mState + " while pressing the right stopwatch button", new Object[0]);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.stopwatch_fragment, container, false);
        this.mTime = (CircleTimerView) v.findViewById(R.id.stopwatch_time);
        this.mTimeText = (CountingTimerView) v.findViewById(R.id.stopwatch_time_text);
        this.mLapsList = (ListView) v.findViewById(R.id.laps_list);
        this.mLapsList.setDividerHeight(0);
        this.mLapsAdapter = new LapsListAdapter(getActivity());
        this.mLapsList.setAdapter((ListAdapter) this.mLapsAdapter);
        this.mTimeText.registerVirtualButtonAction(new Runnable() {
            @Override
            public void run() {
                StopwatchFragment.this.toggleStopwatchState();
            }
        });
        this.mTimeText.setVirtualButtonEnabled(true);
        this.mCircleLayout = (CircleButtonsLayout) v.findViewById(R.id.stopwatch_circle);
        this.mCircleLayout.setCircleTimerViewIds(R.id.stopwatch_time, 0, 0, 0);
        this.mLayoutTransition = new LayoutTransition();
        this.mCircleLayoutTransition = new LayoutTransition();
        this.mCircleLayoutTransition.enableTransitionType(4);
        this.mCircleLayoutTransition.disableTransitionType(2);
        this.mCircleLayoutTransition.disableTransitionType(3);
        this.mCircleLayoutTransition.disableTransitionType(0);
        this.mCircleLayoutTransition.disableTransitionType(1);
        this.mCircleLayoutTransition.setAnimateParentHierarchy(false);
        this.mStartSpace = v.findViewById(R.id.start_space);
        this.mEndSpace = v.findViewById(R.id.end_space);
        this.mSpacersUsed = (this.mStartSpace == null && this.mEndSpace == null) ? false : true;
        this.mBottomSpace = v.findViewById(R.id.bottom_space);
        this.mLayoutTransition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container2, View view, int transitionType) {
                if (view == StopwatchFragment.this.mLapsList && transitionType == 3) {
                    boolean shiftX = view.getResources().getConfiguration().orientation == 2;
                    int first = StopwatchFragment.this.mLapsList.getFirstVisiblePosition();
                    int last = StopwatchFragment.this.mLapsList.getLastVisiblePosition();
                    if (last < first) {
                        last = first;
                    }
                    long duration = transition.getDuration(3);
                    long offset = (duration / ((long) ((last - first) + 1))) / 5;
                    for (int visibleIndex = first; visibleIndex <= last; visibleIndex++) {
                        View lapView = StopwatchFragment.this.mLapsList.getChildAt(visibleIndex - first);
                        if (lapView != null) {
                            float toXValue = shiftX ? 1.0f * ((visibleIndex - first) + 1) : 0.0f;
                            float toYValue = shiftX ? 0.0f : 4.0f * ((visibleIndex - first) + 1);
                            TranslateAnimation animation = new TranslateAnimation(1, 0.0f, 1, toXValue, 1, 0.0f, 1, toYValue);
                            animation.setStartOffset(((long) (last - visibleIndex)) * offset);
                            animation.setDuration(duration);
                            lapView.startAnimation(animation);
                        }
                    }
                }
            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container2, View view, int transitionType) {
                Animation animation;
                if (transitionType == 3) {
                    int last = StopwatchFragment.this.mLapsList.getLastVisiblePosition();
                    for (int visibleIndex = StopwatchFragment.this.mLapsList.getFirstVisiblePosition(); visibleIndex <= last; visibleIndex++) {
                        View lapView = StopwatchFragment.this.mLapsList.getChildAt(visibleIndex);
                        if (lapView != null && (animation = lapView.getAnimation()) != null) {
                            animation.cancel();
                        }
                    }
                }
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean lapsVisible = this.mLapsAdapter.getCount() > 0;
        this.mLapsList.setVisibility(lapsVisible ? 0 : 8);
        if (this.mSpacersUsed) {
            showSpacerVisibility(lapsVisible);
        }
        showBottomSpacerVisibility(lapsVisible);
        ((ViewGroup) getView()).setLayoutTransition(this.mLayoutTransition);
        this.mCircleLayout.setLayoutTransition(this.mCircleLayoutTransition);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        readFromSharedPref(prefs);
        this.mTime.readFromSharedPref(prefs, "sw");
        this.mTime.postInvalidate();
        setFabAppearance();
        setLeftRightButtonAppearance();
        this.mTimeText.setTime(this.mAccumulatedTime, true, true);
        if (this.mState == 1) {
            acquireWakeLock();
            startUpdateThread();
        } else if (this.mState == 2 && this.mAccumulatedTime != 0) {
            this.mTimeText.blinkTimeStr(true);
        }
        showLaps();
        ((DeskClock) getActivity()).registerPageChangedListener(this);
        View v = getView();
        if (v != null) {
            v.setVisibility(0);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (this.mState == 1) {
            stopUpdateThread();
            View v = getView();
            if (v != null) {
                v.setVisibility(4);
            }
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        writeToSharedPref(prefs);
        this.mTime.writeToSharedPref(prefs, "sw");
        this.mTimeText.blinkTimeStr(false);
        ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        releaseWakeLock();
        super.onPause();
    }

    @Override
    public void onPageChanged(int page) {
        if (page == 3 && this.mState == 1) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
    }

    private void doStop() {
        stopUpdateThread();
        this.mTime.pauseIntervalAnimation();
        this.mTimeText.setTime(this.mAccumulatedTime, true, true);
        this.mTimeText.blinkTimeStr(true);
        updateCurrentLap(this.mAccumulatedTime);
        this.mState = 2;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doStart(long time) {
        this.mStartTime = time;
        startUpdateThread();
        this.mTimeText.blinkTimeStr(false);
        if (this.mTime.isAnimating()) {
            this.mTime.startIntervalAnimation();
        }
        this.mState = 1;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doLap() {
        showLaps();
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void doReset() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Utils.clearSwSharedPref(prefs);
        this.mTime.clearSharedPref(prefs, "sw");
        this.mAccumulatedTime = 0L;
        this.mLapsAdapter.clearLaps();
        showLaps();
        this.mTime.stopIntervalAnimation();
        this.mTime.reset();
        this.mTimeText.setTime(this.mAccumulatedTime, true, true);
        this.mTimeText.blinkTimeStr(false);
        this.mState = 0;
        setFabAppearance();
        setLeftRightButtonAppearance();
    }

    private void shareResults() {
        Context context = getActivity();
        Intent shareIntent = new Intent("android.intent.action.SEND");
        shareIntent.setType("text/plain");
        shareIntent.addFlags(524288);
        shareIntent.putExtra("android.intent.extra.SUBJECT", Stopwatches.getShareTitle(context.getApplicationContext()));
        shareIntent.putExtra("android.intent.extra.TEXT", Stopwatches.buildShareResults(getActivity().getApplicationContext(), this.mTimeText.getTimeString(), getLapShareTimes(this.mLapsAdapter.getLapTimes())));
        Intent launchIntent = Intent.createChooser(shareIntent, context.getString(R.string.sw_share_button));
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            LogUtils.e("No compatible receiver is found", new Object[0]);
        }
    }

    private long[] getLapShareTimes(long[] input) {
        if (input == null) {
            return null;
        }
        int numLaps = input.length;
        long[] output = new long[numLaps];
        long prevLapElapsedTime = 0;
        for (int lap_i = numLaps - 1; lap_i >= 0; lap_i--) {
            long lap = input[lap_i];
            LogUtils.v("lap " + lap_i + ": " + lap, new Object[0]);
            output[lap_i] = lap - prevLapElapsedTime;
            prevLapElapsedTime = lap;
        }
        return output;
    }

    private boolean reachedMaxLaps() {
        return this.mLapsAdapter.getCount() >= 99;
    }

    private void addLapTime(long time) {
        long curTime = (time - this.mStartTime) + this.mAccumulatedTime;
        int size = this.mLapsAdapter.getCount();
        if (size == 0) {
            Lap firstLap = new Lap(curTime, curTime);
            this.mLapsAdapter.addLap(firstLap);
            this.mLapsAdapter.addLap(new Lap(0L, curTime));
            this.mTime.setIntervalTime(curTime);
            this.mLapsAdapter.updateTimeFormats(firstLap);
        } else {
            long lapTime = curTime - this.mLapsAdapter.getItem(1).mTotalTime;
            this.mLapsAdapter.getItem(0).mLapTime = lapTime;
            this.mLapsAdapter.getItem(0).mTotalTime = curTime;
            this.mLapsAdapter.addLap(new Lap(0L, curTime));
            this.mTime.setMarkerTime(lapTime);
            this.mLapsAdapter.updateLapFormat();
        }
        this.mLapsAdapter.notifyDataSetChanged();
        this.mTime.stopIntervalAnimation();
        if (!reachedMaxLaps()) {
            this.mTime.startIntervalAnimation();
        }
    }

    private void updateCurrentLap(long totalTime) {
        if (this.mLapsAdapter.getCount() > 0) {
            Lap curLap = this.mLapsAdapter.getItem(0);
            curLap.mLapTime = totalTime - this.mLapsAdapter.getItem(1).mTotalTime;
            curLap.mTotalTime = totalTime;
            if (this.mLapsAdapter.updateTimeFormats(curLap)) {
                this.mLapsAdapter.notifyDataSetChanged();
            } else {
                curLap.updateView();
            }
        }
    }

    private void showLaps() {
        ViewGroup rootView;
        boolean lapsVisible = this.mLapsAdapter.getCount() > 0;
        if (this.mSpacersUsed && (rootView = (ViewGroup) getView()) != null) {
            rootView.setLayoutTransition(null);
            showSpacerVisibility(lapsVisible);
            rootView.setLayoutTransition(this.mLayoutTransition);
        }
        showBottomSpacerVisibility(lapsVisible);
        if (lapsVisible) {
            this.mCircleLayoutTransition.setStartDelay(4, 0L);
            this.mLapsList.setVisibility(0);
        } else {
            long startDelay = this.mLayoutTransition.getStartDelay(3) + this.mLayoutTransition.getDuration(3);
            this.mCircleLayoutTransition.setStartDelay(4, startDelay);
            this.mLapsList.setVisibility(8);
        }
    }

    private void showSpacerVisibility(boolean lapsVisible) {
        int spacersVisibility = lapsVisible ? 8 : 0;
        if (this.mStartSpace != null) {
            this.mStartSpace.setVisibility(spacersVisibility);
        }
        if (this.mEndSpace != null) {
            this.mEndSpace.setVisibility(spacersVisibility);
        }
    }

    private void showBottomSpacerVisibility(boolean lapsVisible) {
        if (this.mBottomSpace != null) {
            this.mBottomSpace.setVisibility(lapsVisible ? 8 : 0);
        }
    }

    private void startUpdateThread() {
        this.mTime.post(this.mTimeUpdateThread);
    }

    private void stopUpdateThread() {
        this.mTime.removeCallbacks(this.mTimeUpdateThread);
    }

    private void writeToSharedPref(SharedPreferences prefs) {
        long[] laps;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("sw_start_time", this.mStartTime);
        editor.putLong("sw_accum_time", this.mAccumulatedTime);
        editor.putInt("sw_state", this.mState);
        if (this.mLapsAdapter != null && (laps = this.mLapsAdapter.getLapTimes()) != null) {
            editor.putInt("sw_lap_num", laps.length);
            for (int i = 0; i < laps.length; i++) {
                String key = "sw_lap_time_" + Integer.toString(laps.length - i);
                editor.putLong(key, laps[i]);
            }
        }
        if (this.mState == 1) {
            editor.putLong("notif_clock_base", this.mStartTime - this.mAccumulatedTime);
            editor.putLong("notif_clock_elapsed", -1L);
            editor.putBoolean("notif_clock_running", true);
        } else if (this.mState == 2) {
            editor.putLong("notif_clock_elapsed", this.mAccumulatedTime);
            editor.putLong("notif_clock_base", -1L);
            editor.putBoolean("notif_clock_running", false);
        } else if (this.mState == 0) {
            editor.remove("notif_clock_base");
            editor.remove("notif_clock_running");
            editor.remove("notif_clock_elapsed");
        }
        editor.putBoolean("sw_update_circle", false);
        editor.apply();
    }

    private void readFromSharedPref(SharedPreferences prefs) {
        long[] oldLaps;
        this.mStartTime = prefs.getLong("sw_start_time", 0L);
        this.mAccumulatedTime = prefs.getLong("sw_accum_time", 0L);
        this.mState = prefs.getInt("sw_state", 0);
        int numLaps = prefs.getInt("sw_lap_num", 0);
        if (this.mLapsAdapter != null && ((oldLaps = this.mLapsAdapter.getLapTimes()) == null || oldLaps.length < numLaps)) {
            long[] laps = new long[numLaps];
            long prevLapElapsedTime = 0;
            for (int lap_i = 0; lap_i < numLaps; lap_i++) {
                String key = "sw_lap_time_" + Integer.toString(lap_i + 1);
                long lap = prefs.getLong(key, 0L);
                laps[(numLaps - lap_i) - 1] = lap - prevLapElapsedTime;
                prevLapElapsedTime = lap;
            }
            this.mLapsAdapter.setLapTimes(laps);
        }
        if (prefs.getBoolean("sw_update_circle", true)) {
            if (this.mState == 2) {
                doStop();
            } else if (this.mState == 1) {
                doStart(this.mStartTime);
            } else if (this.mState == 0) {
                doReset();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(PreferenceManager.getDefaultSharedPreferences(getActivity())) && !key.equals("sw_lap_num") && !key.startsWith("sw_lap_time_")) {
            readFromSharedPref(prefs);
            if (prefs.getBoolean("sw_update_circle", true)) {
                this.mTime.readFromSharedPref(prefs, "sw");
            }
        }
    }

    private void acquireWakeLock() {
        if (this.mWakeLock == null) {
            PowerManager pm = (PowerManager) getActivity().getSystemService("power");
            this.mWakeLock = pm.newWakeLock(536870922, "StopwatchFragment");
            this.mWakeLock.setReferenceCounted(false);
        }
        this.mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    @Override
    public void onFabClick(View view) {
        toggleStopwatchState();
    }

    @Override
    public void onLeftButtonClick(View view) {
        long time = Utils.getTimeNow();
        Context context = getActivity().getApplicationContext();
        Intent intent = new Intent(context, (Class<?>) StopwatchService.class);
        intent.putExtra("message_time", time);
        intent.putExtra("show_notification", false);
        switch (this.mState) {
            case 1:
                addLapTime(time);
                doLap();
                intent.setAction("lap_stopwatch");
                context.startService(intent);
                break;
            case 2:
                doReset();
                intent.setAction("reset_stopwatch");
                context.startService(intent);
                releaseWakeLock();
                break;
            default:
                LogUtils.i("Illegal state " + this.mState + " while pressing the left stopwatch button", new Object[0]);
                break;
        }
    }

    @Override
    public void onRightButtonClick(View view) {
        shareResults();
    }

    @Override
    public void setFabAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mFab != null && activity.getSelectedTab() == 3) {
            if (this.mState == 1) {
                this.mFab.setImageResource(R.drawable.ic_fab_pause);
                this.mFab.setContentDescription(getString(R.string.sw_stop_button));
            } else {
                this.mFab.setImageResource(R.drawable.ic_fab_play);
                this.mFab.setContentDescription(getString(R.string.sw_start_button));
            }
            this.mFab.setVisibility(0);
        }
    }

    @Override
    public void setLeftRightButtonAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mLeftButton != null && this.mRightButton != null && activity.getSelectedTab() == 3) {
            this.mRightButton.setImageResource(R.drawable.ic_share);
            this.mRightButton.setContentDescription(getString(R.string.sw_share_button));
            switch (this.mState) {
                case 0:
                    this.mLeftButton.setImageResource(R.drawable.ic_lap);
                    this.mLeftButton.setContentDescription(getString(R.string.sw_lap_button));
                    this.mLeftButton.setEnabled(false);
                    this.mLeftButton.setVisibility(4);
                    this.mRightButton.setVisibility(4);
                    break;
                case 1:
                    this.mLeftButton.setImageResource(R.drawable.ic_lap);
                    this.mLeftButton.setContentDescription(getString(R.string.sw_lap_button));
                    this.mLeftButton.setEnabled(reachedMaxLaps() ? false : true);
                    this.mLeftButton.setVisibility(0);
                    this.mRightButton.setVisibility(4);
                    break;
                case 2:
                    this.mLeftButton.setImageResource(R.drawable.ic_reset);
                    this.mLeftButton.setContentDescription(getString(R.string.sw_reset_button));
                    this.mLeftButton.setEnabled(true);
                    this.mLeftButton.setVisibility(0);
                    this.mRightButton.setVisibility(0);
                    break;
            }
        }
    }
}
