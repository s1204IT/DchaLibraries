package org.junit.runner.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class RunNotifier {
    private final List<RunListener> listeners = new CopyOnWriteArrayList();
    private volatile boolean pleaseStop = false;

    public void addListener(RunListener runListener) {
        if (runListener == null) {
            throw new NullPointerException("Cannot add a null listener");
        }
        this.listeners.add(wrapIfNotThreadSafe(runListener));
    }

    public void removeListener(RunListener runListener) {
        if (runListener == null) {
            throw new NullPointerException("Cannot remove a null listener");
        }
        this.listeners.remove(wrapIfNotThreadSafe(runListener));
    }

    RunListener wrapIfNotThreadSafe(RunListener runListener) {
        return runListener.getClass().isAnnotationPresent(RunListener.ThreadSafe.class) ? runListener : new SynchronizedRunListener(runListener, this);
    }

    private abstract class SafeNotifier {
        private final List<RunListener> currentListeners;

        protected abstract void notifyListener(RunListener runListener) throws Exception;

        SafeNotifier(RunNotifier runNotifier) {
            this(runNotifier.listeners);
        }

        SafeNotifier(List<RunListener> list) {
            this.currentListeners = list;
        }

        void run() {
            int size = this.currentListeners.size();
            ArrayList arrayList = new ArrayList(size);
            ArrayList arrayList2 = new ArrayList(size);
            for (RunListener runListener : this.currentListeners) {
                try {
                    notifyListener(runListener);
                    arrayList.add(runListener);
                } catch (Exception e) {
                    arrayList2.add(new Failure(Description.TEST_MECHANISM, e));
                }
            }
            RunNotifier.this.fireTestFailures(arrayList, arrayList2);
        }
    }

    public void fireTestRunStarted(final Description description) {
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testRunStarted(description);
            }
        }.run();
    }

    public void fireTestRunFinished(final Result result) {
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testRunFinished(result);
            }
        }.run();
    }

    public void fireTestStarted(final Description description) throws StoppedByUserException {
        if (this.pleaseStop) {
            throw new StoppedByUserException();
        }
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testStarted(description);
            }
        }.run();
    }

    public void fireTestFailure(Failure failure) {
        fireTestFailures(this.listeners, Arrays.asList(failure));
    }

    private void fireTestFailures(List<RunListener> list, final List<Failure> list2) {
        if (!list2.isEmpty()) {
            new SafeNotifier(list) {
                @Override
                protected void notifyListener(RunListener runListener) throws Exception {
                    Iterator it = list2.iterator();
                    while (it.hasNext()) {
                        runListener.testFailure((Failure) it.next());
                    }
                }
            }.run();
        }
    }

    public void fireTestAssumptionFailed(final Failure failure) {
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testAssumptionFailure(failure);
            }
        }.run();
    }

    public void fireTestIgnored(final Description description) {
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testIgnored(description);
            }
        }.run();
    }

    public void fireTestFinished(final Description description) {
        new SafeNotifier() {
            {
                super(RunNotifier.this);
            }

            @Override
            protected void notifyListener(RunListener runListener) throws Exception {
                runListener.testFinished(description);
            }
        }.run();
    }

    public void pleaseStop() {
        this.pleaseStop = true;
    }

    public void addFirstListener(RunListener runListener) {
        if (runListener == null) {
            throw new NullPointerException("Cannot add a null listener");
        }
        this.listeners.add(0, wrapIfNotThreadSafe(runListener));
    }
}
