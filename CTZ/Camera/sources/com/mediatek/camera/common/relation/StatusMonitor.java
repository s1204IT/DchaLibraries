package com.mediatek.camera.common.relation;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatusMonitor {
    private final ConcurrentHashMap<String, StatusResponder> mResponders = new ConcurrentHashMap<>();
    private final Object mResponderCreateDestroyLock = new Object();

    public interface StatusChangeListener {
        void onStatusChanged(String str, String str2);
    }

    public class StatusResponder {
        private final CopyOnWriteArrayList<StatusChangeListener> mListeners;
        private final String mResponderName;

        private StatusResponder(String str) {
            this.mListeners = new CopyOnWriteArrayList<>();
            this.mResponderName = str;
        }

        public String getResponderName() {
            return this.mResponderName;
        }

        public void statusChanged(String str, String str2) {
            Iterator<StatusChangeListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onStatusChanged(str, str2);
            }
        }

        private void addListener(StatusChangeListener statusChangeListener) {
            if (!this.mListeners.contains(statusChangeListener)) {
                this.mListeners.add(statusChangeListener);
            }
        }

        private void removeListener(StatusChangeListener statusChangeListener) {
            this.mListeners.remove(statusChangeListener);
        }
    }

    public void registerValueChangedListener(String str, StatusChangeListener statusChangeListener) {
        getStatusResponderSync(str).addListener(statusChangeListener);
    }

    public void unregisterValueChangedListener(String str, StatusChangeListener statusChangeListener) {
        removeStatusResponderSync(str, statusChangeListener);
    }

    public StatusResponder getStatusResponder(String str) {
        return getStatusResponderSync(str);
    }

    private StatusResponder getStatusResponderSync(String str) {
        StatusResponder statusResponder;
        synchronized (this.mResponderCreateDestroyLock) {
            statusResponder = this.mResponders.get(str);
            if (statusResponder == null) {
                statusResponder = new StatusResponder(str);
                this.mResponders.put(str, statusResponder);
            }
        }
        return statusResponder;
    }

    private void removeStatusResponderSync(String str, StatusChangeListener statusChangeListener) {
        synchronized (this.mResponderCreateDestroyLock) {
            StatusResponder statusResponder = this.mResponders.get(str);
            if (statusResponder != null) {
                statusResponder.removeListener(statusChangeListener);
            }
        }
    }
}
