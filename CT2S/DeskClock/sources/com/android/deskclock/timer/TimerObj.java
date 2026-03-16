package com.android.deskclock.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class TimerObj implements Parcelable {
    public static final Parcelable.Creator<TimerObj> CREATOR = new Parcelable.Creator<TimerObj>() {
        @Override
        public TimerObj createFromParcel(Parcel p) {
            return new TimerObj(p);
        }

        @Override
        public TimerObj[] newArray(int size) {
            return new TimerObj[size];
        }
    };
    public boolean mDeleteAfterUse;
    public String mLabel;
    public long mOriginalLength;
    public long mSetupLength;
    public long mStartTime;
    public int mState;
    public long mTimeLeft;
    public int mTimerId;
    public TimerListItem mView;

    public void writeToSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        String id = Integer.toString(this.mTimerId);
        editor.putInt("timer_id_" + id, this.mTimerId);
        editor.putLong("timer_start_time_" + id, this.mStartTime);
        editor.putLong("timer_time_left_" + id, this.mTimeLeft);
        editor.putLong("timer_original_timet_" + id, this.mOriginalLength);
        editor.putLong("timer_setup_timet_" + id, this.mSetupLength);
        editor.putInt("timer_state_" + id, this.mState);
        Set<String> timersList = prefs.getStringSet("timers_list", new HashSet());
        timersList.add(id);
        editor.putStringSet("timers_list", timersList);
        editor.putString("timer_label_" + id, this.mLabel);
        editor.putBoolean("delete_after_use_" + id, this.mDeleteAfterUse);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs) {
        String id = Integer.toString(this.mTimerId);
        String key = "timer_start_time_" + id;
        this.mStartTime = prefs.getLong(key, 0L);
        String key2 = "timer_time_left_" + id;
        this.mTimeLeft = prefs.getLong(key2, 0L);
        String key3 = "timer_original_timet_" + id;
        this.mOriginalLength = prefs.getLong(key3, 0L);
        String key4 = "timer_setup_timet_" + id;
        this.mSetupLength = prefs.getLong(key4, 0L);
        String key5 = "timer_state_" + id;
        this.mState = prefs.getInt(key5, 0);
        String key6 = "timer_label_" + id;
        this.mLabel = prefs.getString(key6, "");
        String key7 = "delete_after_use_" + id;
        this.mDeleteAfterUse = prefs.getBoolean(key7, false);
    }

    public void deleteFromSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = "timer_id_" + Integer.toString(this.mTimerId);
        String id = Integer.toString(this.mTimerId);
        editor.remove(key);
        String key2 = "timer_start_time_" + id;
        editor.remove(key2);
        String key3 = "timer_time_left_" + id;
        editor.remove(key3);
        String key4 = "timer_original_timet_" + id;
        editor.remove(key4);
        String key5 = "timer_setup_timet_" + id;
        editor.remove(key5);
        String key6 = "timer_state_" + id;
        editor.remove(key6);
        Set<String> timersList = prefs.getStringSet("timers_list", new HashSet());
        timersList.remove(id);
        editor.putStringSet("timers_list", timersList);
        String key7 = "timer_label_" + id;
        editor.remove(key7);
        String key8 = "delete_after_use_" + id;
        editor.remove(key8);
        if (timersList.isEmpty()) {
            editor.remove("next_timer_id");
        }
        editor.commit();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mTimerId);
        dest.writeLong(this.mStartTime);
        dest.writeLong(this.mTimeLeft);
        dest.writeLong(this.mOriginalLength);
        dest.writeLong(this.mSetupLength);
        dest.writeInt(this.mState);
        dest.writeString(this.mLabel);
    }

    public TimerObj(Parcel p) {
        this.mTimerId = p.readInt();
        this.mStartTime = p.readLong();
        this.mTimeLeft = p.readLong();
        this.mOriginalLength = p.readLong();
        this.mSetupLength = p.readLong();
        this.mState = p.readInt();
        this.mLabel = p.readString();
    }

    private TimerObj() {
        this(0L, 0);
    }

    public TimerObj(long timerLength, int timerId) {
        init(timerLength, timerId);
    }

    public TimerObj(long timerLength, Context context) {
        init(timerLength, getNextTimerId(context));
    }

    public TimerObj(long length, String label, Context context) {
        this(length, context);
        this.mLabel = label == null ? "" : label;
    }

    private void init(long length, int timerId) {
        this.mTimerId = timerId;
        this.mStartTime = Utils.getTimeNow();
        this.mSetupLength = length;
        this.mOriginalLength = length;
        this.mTimeLeft = length;
        this.mLabel = "";
    }

    private int getNextTimerId(Context context) {
        int nextTimerId;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        synchronized (TimerObj.class) {
            nextTimerId = prefs.getInt("next_timer_id", 0);
            prefs.edit().putInt("next_timer_id", nextTimerId + 1).apply();
        }
        return nextTimerId;
    }

    public long updateTimeLeft(boolean forceUpdate) {
        if (isTicking() || forceUpdate) {
            long millis = Utils.getTimeNow();
            this.mTimeLeft = this.mOriginalLength - (millis - this.mStartTime);
        }
        return this.mTimeLeft;
    }

    public String getLabelOrDefault(Context context) {
        return (this.mLabel == null || this.mLabel.length() == 0) ? context.getString(R.string.timer_notification_label) : this.mLabel;
    }

    public boolean isTicking() {
        return this.mState == 1 || this.mState == 3;
    }

    public boolean isInUse() {
        return this.mState == 1 || this.mState == 2;
    }

    public void addTime(long time) {
        this.mTimeLeft = this.mOriginalLength - (Utils.getTimeNow() - this.mStartTime);
        if (this.mTimeLeft < 38439000 - time) {
            this.mOriginalLength += time;
        }
    }

    public boolean getDeleteAfterUse() {
        return this.mDeleteAfterUse;
    }

    public long getTimesupTime() {
        return this.mStartTime + this.mOriginalLength;
    }

    public static void getTimersFromSharedPrefs(SharedPreferences prefs, ArrayList<TimerObj> timers) {
        Object[] timerStrings = prefs.getStringSet("timers_list", new HashSet()).toArray();
        if (timerStrings.length > 0) {
            for (Object obj : timerStrings) {
                TimerObj t = new TimerObj();
                t.mTimerId = Integer.parseInt((String) obj);
                t.readFromSharedPref(prefs);
                timers.add(t);
            }
            Collections.sort(timers, new Comparator<TimerObj>() {
                @Override
                public int compare(TimerObj timerObj1, TimerObj timerObj2) {
                    return timerObj1.mTimerId - timerObj2.mTimerId;
                }
            });
        }
    }

    public static void getTimersFromSharedPrefs(SharedPreferences prefs, ArrayList<TimerObj> timers, int match) {
        Object[] timerStrings = prefs.getStringSet("timers_list", new HashSet()).toArray();
        if (timerStrings.length > 0) {
            for (Object obj : timerStrings) {
                TimerObj t = new TimerObj();
                t.mTimerId = Integer.parseInt((String) obj);
                t.readFromSharedPref(prefs);
                if (t.mState == match) {
                    timers.add(t);
                }
            }
        }
    }

    public static void putTimersInSharedPrefs(SharedPreferences prefs, ArrayList<TimerObj> timers) {
        if (timers.size() > 0) {
            for (int i = 0; i < timers.size(); i++) {
                timers.get(i).writeToSharedPref(prefs);
            }
        }
    }

    public static void resetTimersInSharedPrefs(SharedPreferences prefs) {
        ArrayList<TimerObj> timers = new ArrayList<>();
        getTimersFromSharedPrefs(prefs, timers);
        for (TimerObj t : timers) {
            t.mState = 5;
            long j = t.mSetupLength;
            t.mOriginalLength = j;
            t.mTimeLeft = j;
            t.writeToSharedPref(prefs);
        }
    }
}
