package com.android.calendar;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
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

        public LoadEventsRequest(int id, int startDay, int numDays, ArrayList<Event> events, Runnable successCallback, Runnable cancelCallback) {
            this.id = id;
            this.startDay = startDay;
            this.numDays = numDays;
            this.events = events;
            this.successCallback = successCallback;
            this.cancelCallback = cancelCallback;
        }

        @Override
        public void processRequest(EventLoader eventLoader) {
            Event.loadEvents(eventLoader.mContext, this.events, this.startDay, this.numDays, this.id, eventLoader.mSequenceNumber);
            if (this.id == eventLoader.mSequenceNumber.get()) {
                eventLoader.mHandler.post(this.successCallback);
            } else {
                eventLoader.mHandler.post(this.cancelCallback);
            }
        }

        @Override
        public void skipRequest(EventLoader eventLoader) {
            eventLoader.mHandler.post(this.cancelCallback);
        }
    }

    private static class LoaderThread extends Thread {
        EventLoader mEventLoader;
        LinkedBlockingQueue<LoadRequest> mQueue;

        public LoaderThread(LinkedBlockingQueue<LoadRequest> queue, EventLoader eventLoader) {
            this.mQueue = queue;
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
            LoadRequest request;
            Process.setThreadPriority(10);
            while (true) {
                try {
                    request = this.mQueue.take();
                    while (!this.mQueue.isEmpty()) {
                        request.skipRequest(this.mEventLoader);
                        LoadRequest request2 = this.mQueue.take();
                        request = request2;
                    }
                } catch (InterruptedException e) {
                    Log.e("Cal", "background LoaderThread interrupted!");
                }
                if (request instanceof ShutdownRequest) {
                    return;
                } else {
                    request.processRequest(this.mEventLoader);
                }
            }
        }
    }

    public EventLoader(Context context) {
        this.mContext = context;
        this.mResolver = context.getContentResolver();
    }

    public void startBackgroundThread() {
        this.mLoaderThread = new LoaderThread(this.mLoaderQueue, this);
        this.mLoaderThread.start();
    }

    public void stopBackgroundThread() {
        this.mLoaderThread.shutdown();
    }

    public void loadEventsInBackground(int numDays, ArrayList<Event> events, int startDay, Runnable successCallback, Runnable cancelCallback) {
        int id = this.mSequenceNumber.incrementAndGet();
        LoadEventsRequest request = new LoadEventsRequest(id, startDay, numDays, events, successCallback, cancelCallback);
        try {
            this.mLoaderQueue.put(request);
        } catch (InterruptedException e) {
            Log.e("Cal", "loadEventsInBackground() interrupted!");
        }
    }
}
