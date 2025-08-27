package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class GestureRecorder {
    public static final String TAG = GestureRecorder.class.getSimpleName();
    private Gesture mCurrentGesture;
    private LinkedList<Gesture> mGestures;
    private Handler mHandler;

    public class Gesture {
        private LinkedList<Record> mRecords = new LinkedList<>();
        private HashSet<String> mTags = new HashSet<>();
        long mDownTime = -1;
        boolean mComplete = false;

        public abstract class Record {
            long time;

            public Record() {
            }
        }

        public Gesture() {
        }

        public class TagRecord extends Record {
            public String info;
            public String tag;

            public TagRecord(long j, String str, String str2) {
                super();
                this.time = j;
                this.tag = str;
                this.info = str2;
            }
        }

        public void tag(long j, String str, String str2) {
            this.mRecords.add(new TagRecord(j, str, str2));
            this.mTags.add(str);
        }
    }

    public void tag(long j, String str, String str2) {
        synchronized (this.mGestures) {
            if (this.mCurrentGesture == null) {
                this.mCurrentGesture = new Gesture();
                this.mGestures.add(this.mCurrentGesture);
            }
            this.mCurrentGesture.tag(j, str, str2);
        }
        saveLater();
    }

    public void tag(String str, String str2) {
        tag(SystemClock.uptimeMillis(), str, str2);
    }

    public void saveLater() {
        this.mHandler.removeMessages(6351);
        this.mHandler.sendEmptyMessageDelayed(6351, 5000L);
    }
}
