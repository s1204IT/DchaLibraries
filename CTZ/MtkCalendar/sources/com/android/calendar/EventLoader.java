package com.android.calendar;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.mediatek.calendar.PDebug;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoader {
    private Context mContext;
    private LoaderThread mLoaderThread;
    private ContentResolver mResolver;
    private Handler mHandler = new Handler();
    private AtomicInteger mSequenceNumber = new AtomicInteger();
    private LinkedBlockingQueue<LoadRequest> mLoaderQueue = new LinkedBlockingQueue<>();

    private interface LoadRequest {
        void processRequest(EventLoader eventLoader);

        void skipRequest(EventLoader eventLoader);
    }

    private static class ShutdownRequest implements LoadRequest {
        private ShutdownRequest() {
        }

        @Override
        public void processRequest(EventLoader eventLoader) {
        }

        @Override
        public void skipRequest(EventLoader eventLoader) {
        }
    }

    private static class LoadEventsRequest implements LoadRequest {
        public Runnable cancelCallback;
        public ArrayList<Event> events;
        public int id;
        public int numDays;
        public int startDay;
        public Runnable successCallback;

        public LoadEventsRequest(int i, int i2, int i3, ArrayList<Event> arrayList, Runnable runnable, Runnable runnable2) {
            this.id = i;
            this.startDay = i2;
            this.numDays = i3;
            this.events = arrayList;
            this.successCallback = runnable;
            this.cancelCallback = runnable2;
        }

        @Override
        public void processRequest(EventLoader eventLoader) throws Throwable {
            PDebug.Start("EventLoader.LoadEventsRequest.processRequest");
            Event.loadEvents(eventLoader.mContext, this.events, this.startDay, this.numDays, this.id, eventLoader.mSequenceNumber);
            if (this.id == eventLoader.mSequenceNumber.get()) {
                eventLoader.mHandler.post(this.successCallback);
            } else {
                eventLoader.mHandler.post(this.cancelCallback);
            }
            PDebug.EndAndStart("EventLoader.LoadEventsRequest.processRequest", "EventLoader.LoadEventsRequest.processRequest->DayView.eventsLoadCallback");
        }

        @Override
        public void skipRequest(EventLoader eventLoader) {
            eventLoader.mHandler.post(this.cancelCallback);
        }
    }

    private static class LoaderThread extends Thread {
        EventLoader mEventLoader;
        LinkedBlockingQueue<LoadRequest> mQueue;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> linkedBlockingQueue, EventLoader eventLoader) {
            this.mQueue = linkedBlockingQueue;
            this.mEventLoader = eventLoader;
        }

        public void shutdown() {
            try {
                this.mQueue.put(new ShutdownRequest());
            } catch (InterruptedException e) {
                Log.e("Cal", "LoaderThread.shutdown() interrupted!");
            }
        }

        @Override
        public void run() {
            LoadRequest loadRequestTake;
            PDebug.End("EventLoader.startBackgroundThread->startLoaderThread");
            Process.setThreadPriority(10);
            while (true) {
                try {
                    PDebug.Start("EventLoader.LoaderThread.runLoop");
                    loadRequestTake = this.mQueue.take();
                    while (!this.mQueue.isEmpty()) {
                        loadRequestTake.skipRequest(this.mEventLoader);
                        loadRequestTake = this.mQueue.take();
                    }
                } catch (InterruptedException e) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                } catch (SecurityException e2) {
                    Log.d("Calendar", "Security exception, permission denied!");
                }
                if (loadRequestTake instanceof ShutdownRequest) {
                    return;
                }
                loadRequestTake.processRequest(this.mEventLoader);
                PDebug.End("EventLoader.LoaderThread.runLoop");
            }
        }
    }

    public EventLoader(Context context) {
        this.mContext = context;
        this.mResolver = context.getContentResolver();
    }

    public void startBackgroundThread() {
        PDebug.Start("EventLoader.startBackgroundThread->startLoaderThread");
        this.mLoaderThread = new LoaderThread(this.mLoaderQueue, this);
        this.mLoaderThread.start();
    }

    public void stopBackgroundThread() {
        this.mLoaderThread.shutdown();
    }

    public void loadEventsInBackground(int i, ArrayList<Event> arrayList, int i2, Runnable runnable, Runnable runnable2) {
        PDebug.Start("EventLoader.loadEventsInBackground");
        try {
            this.mLoaderQueue.put(new LoadEventsRequest(this.mSequenceNumber.incrementAndGet(), i2, i, arrayList, runnable, runnable2));
        } catch (InterruptedException e) {
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
        PDebug.End("EventLoader.loadEventsInBackground");
    }
}
