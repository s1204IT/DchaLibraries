package com.android.okhttp;

import com.android.okhttp.Call;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.http.HttpEngine;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Dispatcher {
    private ExecutorService executorService;
    private int maxRequests = 64;
    private int maxRequestsPerHost = 5;
    private final Deque<Call.AsyncCall> readyCalls = new ArrayDeque();
    private final Deque<Call.AsyncCall> runningCalls = new ArrayDeque();
    private final Deque<Call> executedCalls = new ArrayDeque();

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Dispatcher() {
    }

    public synchronized ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp Dispatcher", false));
        }
        return this.executorService;
    }

    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        this.maxRequests = maxRequests;
        promoteCalls();
    }

    public synchronized int getMaxRequests() {
        return this.maxRequests;
    }

    public synchronized void setMaxRequestsPerHost(int maxRequestsPerHost) {
        if (maxRequestsPerHost < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
        }
        this.maxRequestsPerHost = maxRequestsPerHost;
        promoteCalls();
    }

    public synchronized int getMaxRequestsPerHost() {
        return this.maxRequestsPerHost;
    }

    synchronized void enqueue(Call.AsyncCall call) {
        if (this.runningCalls.size() < this.maxRequests && runningCallsForHost(call) < this.maxRequestsPerHost) {
            this.runningCalls.add(call);
            getExecutorService().execute(call);
        } else {
            this.readyCalls.add(call);
        }
    }

    public synchronized void cancel(Object tag) {
        for (Call.AsyncCall call : this.readyCalls) {
            if (Util.equal(tag, call.tag())) {
                call.cancel();
            }
        }
        for (Call.AsyncCall call2 : this.runningCalls) {
            if (Util.equal(tag, call2.tag())) {
                call2.get().canceled = true;
                HttpEngine engine = call2.get().engine;
                if (engine != null) {
                    engine.disconnect();
                }
            }
        }
        for (Call call3 : this.executedCalls) {
            if (Util.equal(tag, call3.tag())) {
                call3.cancel();
            }
        }
    }

    synchronized void finished(Call.AsyncCall call) {
        if (!this.runningCalls.remove(call)) {
            throw new AssertionError("AsyncCall wasn't running!");
        }
        promoteCalls();
    }

    private void promoteCalls() {
        if (this.runningCalls.size() < this.maxRequests && !this.readyCalls.isEmpty()) {
            Iterator<Call.AsyncCall> it = this.readyCalls.iterator();
            while (it.hasNext()) {
                Call.AsyncCall call = it.next();
                if (runningCallsForHost(call) < this.maxRequestsPerHost) {
                    it.remove();
                    this.runningCalls.add(call);
                    getExecutorService().execute(call);
                }
                if (this.runningCalls.size() >= this.maxRequests) {
                    return;
                }
            }
        }
    }

    private int runningCallsForHost(Call.AsyncCall call) {
        int result = 0;
        for (Call.AsyncCall c : this.runningCalls) {
            if (c.host().equals(call.host())) {
                result++;
            }
        }
        return result;
    }

    synchronized void executed(Call call) {
        this.executedCalls.add(call);
    }

    synchronized void finished(Call call) {
        if (!this.executedCalls.remove(call)) {
            throw new AssertionError("Call wasn't in-flight!");
        }
    }

    public synchronized int getRunningCallCount() {
        return this.runningCalls.size();
    }

    public synchronized int getQueuedCallCount() {
        return this.readyCalls.size();
    }
}
