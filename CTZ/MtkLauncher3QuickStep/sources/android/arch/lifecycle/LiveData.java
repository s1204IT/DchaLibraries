package android.arch.lifecycle;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.internal.SafeIterableMap;
import android.arch.lifecycle.Lifecycle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

public abstract class LiveData<T> {
    private static final Object NOT_SET = new Object();
    static final int START_VERSION = -1;
    private boolean mDispatchInvalidated;
    private boolean mDispatchingValue;
    private final Object mDataLock = new Object();
    private SafeIterableMap<Observer<? super T>, LiveData<T>.ObserverWrapper> mObservers = new SafeIterableMap<>();
    private int mActiveCount = 0;
    private volatile Object mData = NOT_SET;
    private volatile Object mPendingData = NOT_SET;
    private int mVersion = -1;
    private final Runnable mPostValueRunnable = new Runnable() {
        @Override
        public void run() {
            Object newValue;
            synchronized (LiveData.this.mDataLock) {
                newValue = LiveData.this.mPendingData;
                LiveData.this.mPendingData = LiveData.NOT_SET;
            }
            LiveData.this.setValue(newValue);
        }
    };

    private void considerNotify(LiveData<T>.ObserverWrapper observerWrapper) {
        if (!observerWrapper.mActive) {
            return;
        }
        if (!observerWrapper.shouldBeActive()) {
            observerWrapper.activeStateChanged(false);
        } else {
            if (observerWrapper.mLastVersion >= this.mVersion) {
                return;
            }
            observerWrapper.mLastVersion = this.mVersion;
            observerWrapper.mObserver.onChanged((Object) this.mData);
        }
    }

    public void dispatchingValue(@Nullable LiveData<T>.ObserverWrapper initiator) {
        if (this.mDispatchingValue) {
            this.mDispatchInvalidated = true;
            return;
        }
        this.mDispatchingValue = true;
        do {
            this.mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                Iterator<Map.Entry<Observer<? super T>, LiveData<T>.ObserverWrapper>> iterator = this.mObservers.iteratorWithAdditions();
                while (iterator.hasNext()) {
                    considerNotify(iterator.next().getValue());
                    if (this.mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (this.mDispatchInvalidated);
        this.mDispatchingValue = false;
    }

    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        LiveData<T>.LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        LiveData<T>.ObserverWrapper existing = this.mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        assertMainThread("observeForever");
        LiveData<T>.AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        LiveData<T>.ObserverWrapper existing = this.mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && (existing instanceof LifecycleBoundObserver)) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    @MainThread
    public void removeObserver(@NonNull Observer<? super T> observer) {
        assertMainThread("removeObserver");
        LiveData<T>.ObserverWrapper removed = this.mObservers.remove(observer);
        if (removed == null) {
            return;
        }
        removed.detachObserver();
        removed.activeStateChanged(false);
    }

    @MainThread
    public void removeObservers(@NonNull LifecycleOwner owner) {
        assertMainThread("removeObservers");
        for (Map.Entry<Observer<? super T>, LiveData<T>.ObserverWrapper> entry : this.mObservers) {
            if (entry.getValue().isAttachedTo(owner)) {
                removeObserver(entry.getKey());
            }
        }
    }

    protected void postValue(T value) {
        boolean postTask;
        synchronized (this.mDataLock) {
            postTask = this.mPendingData == NOT_SET;
            this.mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(this.mPostValueRunnable);
    }

    @MainThread
    protected void setValue(T value) {
        assertMainThread("setValue");
        this.mVersion++;
        this.mData = value;
        dispatchingValue(null);
    }

    @Nullable
    public T getValue() {
        T t = (T) this.mData;
        if (t != NOT_SET) {
            return t;
        }
        return null;
    }

    int getVersion() {
        return this.mVersion;
    }

    protected void onActive() {
    }

    protected void onInactive() {
    }

    public boolean hasObservers() {
        return this.mObservers.size() > 0;
    }

    public boolean hasActiveObservers() {
        return this.mActiveCount > 0;
    }

    class LifecycleBoundObserver extends LiveData<T>.ObserverWrapper implements GenericLifecycleObserver {

        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            this.mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return this.mOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (this.mOwner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                LiveData.this.removeObserver(this.mObserver);
            } else {
                activeStateChanged(shouldBeActive());
            }
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return this.mOwner == owner;
        }

        @Override
        void detachObserver() {
            this.mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ObserverWrapper {
        boolean mActive;
        int mLastVersion = -1;
        final Observer<? super T> mObserver;

        abstract boolean shouldBeActive();

        ObserverWrapper(Observer<? super T> observer) {
            this.mObserver = observer;
        }

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == this.mActive) {
                return;
            }
            this.mActive = newActive;
            boolean wasInactive = LiveData.this.mActiveCount == 0;
            LiveData.this.mActiveCount += this.mActive ? 1 : -1;
            if (wasInactive && this.mActive) {
                LiveData.this.onActive();
            }
            if (LiveData.this.mActiveCount == 0 && !this.mActive) {
                LiveData.this.onInactive();
            }
            if (this.mActive) {
                LiveData.this.dispatchingValue(this);
            }
        }
    }

    private class AlwaysActiveObserver extends LiveData<T>.ObserverWrapper {
        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    private static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background thread");
        }
    }
}
