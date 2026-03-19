package com.android.server.audio;

import android.util.Log;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class AudioEventLogger {
    private final LinkedList<Event> mEvents = new LinkedList<>();
    private final int mMemSize;
    private final String mTitle;

    public static abstract class Event {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final long mTimestamp = System.currentTimeMillis();

        public abstract String eventToString();

        Event() {
        }

        public String toString() {
            return sFormat.format(new Date(this.mTimestamp)) + " " + eventToString();
        }

        public Event printLog(String str) {
            Log.i(str, eventToString());
            return this;
        }
    }

    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String str) {
            this.mMsg = str;
        }

        @Override
        public String eventToString() {
            return this.mMsg;
        }
    }

    public AudioEventLogger(int i, String str) {
        this.mMemSize = i;
        this.mTitle = str;
    }

    public synchronized void log(Event event) {
        if (this.mEvents.size() >= this.mMemSize) {
            this.mEvents.removeFirst();
        }
        this.mEvents.add(event);
    }

    public synchronized void dump(PrintWriter printWriter) {
        printWriter.println("Audio event log: " + this.mTitle);
        Iterator<Event> it = this.mEvents.iterator();
        while (it.hasNext()) {
            printWriter.println(it.next().toString());
        }
    }
}
