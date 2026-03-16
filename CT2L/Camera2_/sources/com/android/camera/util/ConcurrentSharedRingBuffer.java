package com.android.camera.util;

import android.os.Handler;
import android.util.Pair;
import com.android.camera.debug.Log;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class ConcurrentSharedRingBuffer<E> {
    private static final Log.Tag TAG = new Log.Tag("CncrrntShrdRingBuf");
    private final Semaphore mCapacitySemaphore;
    private TreeMap<Long, Pinnable<E>> mElements;
    private final Semaphore mPinSemaphore;
    private TreeMap<Long, Pinnable<E>> mUnpinnedElements;
    private final Object mSwapLock = new Object();
    private final Object mLock = new Object();
    private boolean mClosed = false;
    private Handler mPinStateHandler = null;
    private PinStateListener mPinStateListener = null;

    public interface PinStateListener {
        void onPinStateChange(boolean z);
    }

    public interface Selector<E> {
        boolean select(E e);
    }

    public interface SwapTask<E> {
        E create();

        E swap(E e);

        void update(E e);
    }

    private static class Pinnable<E> {
        private E mElement;
        private int mPins = 0;

        static int access$208(Pinnable x0) {
            int i = x0.mPins;
            x0.mPins = i + 1;
            return i;
        }

        static int access$210(Pinnable x0) {
            int i = x0.mPins;
            x0.mPins = i - 1;
            return i;
        }

        public Pinnable(E element) {
            this.mElement = element;
        }

        public E getElement() {
            return this.mElement;
        }

        private boolean isPinned() {
            return this.mPins > 0;
        }
    }

    public ConcurrentSharedRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.mElements = new TreeMap<>();
        this.mUnpinnedElements = new TreeMap<>();
        this.mCapacitySemaphore = new Semaphore(capacity);
        this.mPinSemaphore = new Semaphore(-1);
    }

    public void setListener(Handler handler, PinStateListener listener) {
        synchronized (this.mLock) {
            this.mPinStateHandler = handler;
            this.mPinStateListener = listener;
        }
    }

    public boolean swapLeast(long newKey, SwapTask<E> swapTask) {
        boolean z;
        synchronized (this.mSwapLock) {
            synchronized (this.mLock) {
                if (this.mClosed) {
                    return false;
                }
                Pinnable<E> existingElement = this.mElements.get(Long.valueOf(newKey));
                if (existingElement != null) {
                    swapTask.update(existingElement.getElement());
                    return true;
                }
                if (this.mCapacitySemaphore.tryAcquire()) {
                    Pinnable<E> p = new Pinnable<>(swapTask.create());
                    synchronized (this.mLock) {
                        if (this.mClosed) {
                            return false;
                        }
                        this.mElements.put(Long.valueOf(newKey), p);
                        this.mUnpinnedElements.put(Long.valueOf(newKey), p);
                        this.mPinSemaphore.release();
                        if (this.mPinSemaphore.availablePermits() == 1) {
                            notifyPinStateChange(true);
                        }
                        return true;
                    }
                }
                synchronized (this.mLock) {
                    if (this.mClosed) {
                        return false;
                    }
                    Map.Entry<Long, Pinnable<E>> toSwapEntry = this.mUnpinnedElements.pollFirstEntry();
                    if (toSwapEntry == null) {
                        throw new RuntimeException("No unpinned element available.");
                    }
                    Pinnable<E> toSwap = toSwapEntry.getValue();
                    this.mElements.remove(toSwapEntry.getKey());
                    try {
                        ((Pinnable) toSwap).mElement = swapTask.swap(((Pinnable) toSwap).mElement);
                        synchronized (this.mLock) {
                            if (this.mClosed) {
                                z = false;
                            } else {
                                this.mElements.put(Long.valueOf(newKey), toSwap);
                                this.mUnpinnedElements.put(Long.valueOf(newKey), toSwap);
                                z = true;
                            }
                        }
                        return z;
                    } catch (Throwable th) {
                        synchronized (this.mLock) {
                            if (this.mClosed) {
                                return false;
                            }
                            this.mElements.put(Long.valueOf(newKey), toSwap);
                            this.mUnpinnedElements.put(Long.valueOf(newKey), toSwap);
                            throw th;
                        }
                    }
                }
            }
        }
    }

    public Pair<Long, E> tryPin(long key) {
        boolean acquiredLastPin = false;
        synchronized (this.mLock) {
            if (this.mClosed) {
                return null;
            }
            if (this.mElements.isEmpty()) {
                return null;
            }
            Pinnable<E> entry = this.mElements.get(Long.valueOf(key));
            if (entry == null) {
                return null;
            }
            if (entry.isPinned()) {
                Pinnable.access$208(entry);
            } else {
                if (!this.mPinSemaphore.tryAcquire()) {
                    return null;
                }
                this.mUnpinnedElements.remove(Long.valueOf(key));
                Pinnable.access$208(entry);
                acquiredLastPin = this.mPinSemaphore.availablePermits() <= 0;
            }
            if (acquiredLastPin) {
                notifyPinStateChange(false);
            }
            return Pair.create(Long.valueOf(key), entry.getElement());
        }
    }

    public void release(long key) {
        synchronized (this.mLock) {
            Pinnable<E> element = this.mElements.get(Long.valueOf(key));
            if (element != null) {
                if (!element.isPinned()) {
                    throw new IllegalArgumentException("Calling release() with unpinned element.");
                }
                Pinnable.access$210(element);
                if (!element.isPinned()) {
                    this.mUnpinnedElements.put(Long.valueOf(key), element);
                    this.mPinSemaphore.release();
                    if (this.mPinSemaphore.availablePermits() == 1) {
                        notifyPinStateChange(true);
                    }
                }
            } else {
                throw new InvalidParameterException("No entry found for the given key.");
            }
        }
    }

    public Pair<Long, E> tryPinGreatest() {
        Pair<Long, E> pairTryPin = null;
        synchronized (this.mLock) {
            if (!this.mClosed) {
                if (!this.mElements.isEmpty()) {
                    pairTryPin = tryPin(this.mElements.lastKey().longValue());
                }
            }
        }
        return pairTryPin;
    }

    public Pair<Long, E> tryPinGreatestSelected(Selector<E> selector) {
        ArrayList<Long> keys = new ArrayList<>();
        synchronized (this.mLock) {
            if (this.mClosed) {
                return null;
            }
            if (this.mElements.isEmpty()) {
                return null;
            }
            keys.addAll(this.mElements.keySet());
            Collections.sort(keys);
            for (int i = keys.size() - 1; i >= 0; i--) {
                Pair<Long, E> pinnedCandidate = tryPin(keys.get(i).longValue());
                if (pinnedCandidate != null) {
                    try {
                        boolean selected = selector.select(pinnedCandidate.second);
                        if (!selected) {
                            release(((Long) pinnedCandidate.first).longValue());
                        } else {
                            return pinnedCandidate;
                        }
                    } finally {
                        if (!selected) {
                        }
                    }
                }
            }
            return null;
        }
    }

    public void close(Task<E> task) throws InterruptedException {
        int numPinnedElements;
        synchronized (this.mSwapLock) {
            synchronized (this.mLock) {
                this.mClosed = true;
                numPinnedElements = this.mElements.size() - this.mUnpinnedElements.size();
            }
        }
        notifyPinStateChange(false);
        if (numPinnedElements > 0) {
            this.mPinSemaphore.acquire(numPinnedElements);
        }
        for (Pinnable<E> element : this.mElements.values()) {
            task.run(((Pinnable) element).mElement);
        }
        this.mUnpinnedElements.clear();
        this.mElements.clear();
    }

    private void notifyPinStateChange(final boolean pinsAvailable) {
        synchronized (this.mLock) {
            if (this.mPinStateHandler != null) {
                final PinStateListener listener = this.mPinStateListener;
                this.mPinStateHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPinStateChange(pinsAvailable);
                    }
                });
            }
        }
    }
}
