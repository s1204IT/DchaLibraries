package com.android.deskclock.timer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TimerFragmentAdapter extends FragmentStatePagerAdapter2 {
    private final SharedPreferences mSharedPrefs;
    private final ArrayList<TimerObj> mTimerList;

    public TimerFragmentAdapter(FragmentManager fm, SharedPreferences sharedPreferences) {
        super(fm);
        this.mTimerList = new ArrayList<>();
        this.mSharedPrefs = sharedPreferences;
    }

    @Override
    public int getItemPosition(Object object) {
        return -2;
    }

    @Override
    public int getCount() {
        return this.mTimerList.size();
    }

    @Override
    public Fragment getItem(int position) {
        return TimerItemFragment.newInstance(this.mTimerList.get(position));
    }

    public void addTimer(TimerObj timer) {
        this.mTimerList.add(0, timer);
        notifyDataSetChanged();
    }

    public TimerObj getTimerAt(int position) {
        return this.mTimerList.get(position);
    }

    public void saveTimersToSharedPrefs() {
        TimerObj.putTimersInSharedPrefs(this.mSharedPrefs, this.mTimerList);
    }

    public void populateTimersFromPref() {
        this.mTimerList.clear();
        TimerObj.getTimersFromSharedPrefs(this.mSharedPrefs, this.mTimerList);
        Collections.sort(this.mTimerList, new Comparator<TimerObj>() {
            @Override
            public int compare(TimerObj o1, TimerObj o2) {
                return o2.mTimerId < o1.mTimerId ? -1 : 1;
            }
        });
        notifyDataSetChanged();
    }

    public void deleteTimer(int id) {
        int i = 0;
        while (true) {
            if (i >= this.mTimerList.size()) {
                break;
            }
            TimerObj timer = this.mTimerList.get(i);
            if (timer.mTimerId != id) {
                i++;
            } else {
                if (timer.mView != null) {
                    timer.mView.stop();
                }
                timer.deleteFromSharedPref(this.mSharedPrefs);
                this.mTimerList.remove(i);
            }
        }
        notifyDataSetChanged();
    }
}
