package android.icu.impl;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

public abstract class ICUNotifier {
    private List<EventListener> listeners;
    private final Object notifyLock = new Object();
    private NotifyThread notifyThread;

    protected abstract boolean acceptsListener(EventListener eventListener);

    protected abstract void notifyListener(EventListener eventListener);

    public void addListener(EventListener eventListener) {
        if (eventListener == null) {
            throw new NullPointerException();
        }
        if (acceptsListener(eventListener)) {
            synchronized (this.notifyLock) {
                if (this.listeners == null) {
                    this.listeners = new ArrayList();
                } else {
                    Iterator<EventListener> it = this.listeners.iterator();
                    while (it.hasNext()) {
                        if (it.next() == eventListener) {
                            return;
                        }
                    }
                }
                this.listeners.add(eventListener);
                return;
            }
        }
        throw new IllegalStateException("Listener invalid for this notifier.");
    }

    public void removeListener(EventListener eventListener) {
        if (eventListener == null) {
            throw new NullPointerException();
        }
        synchronized (this.notifyLock) {
            if (this.listeners != null) {
                Iterator<EventListener> it = this.listeners.iterator();
                while (it.hasNext()) {
                    if (it.next() == eventListener) {
                        it.remove();
                        if (this.listeners.size() == 0) {
                            this.listeners = null;
                        }
                        return;
                    }
                }
            }
        }
    }

    public void notifyChanged() {
        synchronized (this.notifyLock) {
            if (this.listeners != null) {
                if (this.notifyThread == null) {
                    this.notifyThread = new NotifyThread(this);
                    this.notifyThread.setDaemon(true);
                    this.notifyThread.start();
                }
                this.notifyThread.queue((EventListener[]) this.listeners.toArray(new EventListener[this.listeners.size()]));
            }
        }
    }

    private static class NotifyThread extends Thread {
        private final ICUNotifier notifier;
        private final List<EventListener[]> queue = new ArrayList();

        NotifyThread(ICUNotifier iCUNotifier) {
            this.notifier = iCUNotifier;
        }

        public void queue(EventListener[] eventListenerArr) {
            synchronized (this) {
                this.queue.add(eventListenerArr);
                notify();
            }
        }

        @Override
        public void run() {
            int i;
            EventListener[] eventListenerArrRemove;
            while (true) {
                try {
                    synchronized (this) {
                        while (this.queue.isEmpty()) {
                            wait();
                        }
                        eventListenerArrRemove = this.queue.remove(0);
                    }
                    for (EventListener eventListener : eventListenerArrRemove) {
                        this.notifier.notifyListener(eventListener);
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
