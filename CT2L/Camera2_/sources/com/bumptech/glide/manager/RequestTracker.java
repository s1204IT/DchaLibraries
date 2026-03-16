package com.bumptech.glide.manager;

import com.bumptech.glide.request.Request;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class RequestTracker {
    private final Set<Request> requests = Collections.newSetFromMap(new WeakHashMap());

    public void addRequest(Request request) {
        this.requests.add(request);
    }

    public void removeRequest(Request request) {
        this.requests.remove(request);
    }

    public void pauseRequests() {
        for (Request request : this.requests) {
            if (!request.isComplete() && !request.isFailed()) {
                request.clear();
            }
        }
    }

    public void resumeRequests() {
        for (Request request : this.requests) {
            if (!request.isComplete() && !request.isRunning()) {
                request.run();
            }
        }
    }

    public void clearRequests() {
        for (Request request : this.requests) {
            request.clear();
        }
    }

    public void restartRequests() {
        for (Request request : this.requests) {
            if (request.isFailed()) {
                request.run();
            } else if (!request.isComplete()) {
                request.clear();
                request.run();
            }
        }
    }
}
