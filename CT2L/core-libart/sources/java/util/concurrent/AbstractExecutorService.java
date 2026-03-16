package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractExecutorService implements ExecutorService {
    static final boolean $assertionsDisabled;

    static {
        $assertionsDisabled = !AbstractExecutorService.class.desiredAssertionStatus();
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask(callable);
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }

    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos) throws Throwable {
        long deadline;
        Throwable th;
        int i;
        int size;
        ExecutionException ee;
        if (tasks == null) {
            throw new NullPointerException();
        }
        int ntasks = tasks.size();
        if (ntasks == 0) {
            throw new IllegalArgumentException();
        }
        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);
        ExecutorCompletionService<T> ecs = new ExecutorCompletionService<>(this);
        if (timed) {
            try {
                deadline = System.nanoTime() + nanos;
            } catch (Throwable th2) {
                th = th2;
                size = futures.size();
                while (i < size) {
                }
                throw th;
            }
        } else {
            deadline = 0;
        }
        Iterator<? extends Callable<T>> it = tasks.iterator();
        futures.add(ecs.submit(it.next()));
        int ntasks2 = ntasks - 1;
        int active = 1;
        ExecutionException ee2 = null;
        while (true) {
            try {
                Future<T> f = ecs.poll();
                if (f == null) {
                    if (ntasks2 > 0) {
                        ntasks2--;
                        futures.add(ecs.submit(it.next()));
                        active++;
                    } else {
                        if (active == 0) {
                            break;
                        }
                        if (timed) {
                            f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                            if (f == null) {
                                throw new TimeoutException();
                            }
                            nanos = deadline - System.nanoTime();
                        } else {
                            f = ecs.take();
                        }
                    }
                }
                if (f != null) {
                    active--;
                    try {
                        T t = f.get();
                        int size2 = futures.size();
                        for (int i2 = 0; i2 < size2; i2++) {
                            futures.get(i2).cancel(true);
                        }
                        return t;
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    } catch (ExecutionException eex) {
                        ee = eex;
                    }
                } else {
                    ee = ee2;
                }
                ee2 = ee;
            } catch (Throwable th3) {
                th = th3;
                size = futures.size();
                for (i = 0; i < size; i++) {
                    futures.get(i).cancel(true);
                }
                throw th;
            }
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
        try {
            return (T) doInvokeAny(collection, false, 0L);
        } catch (TimeoutException e) {
            if ($assertionsDisabled) {
                return null;
            }
            throw new AssertionError();
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return (T) doInvokeAny(collection, true, timeUnit.toNanos(j));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = newTaskFor(t);
                futures.add(f);
                execute(f);
            }
            int size = futures.size();
            for (int i = 0; i < size; i++) {
                Future<T> f2 = futures.get(i);
                if (!f2.isDone()) {
                    try {
                        f2.get();
                    } catch (CancellationException e) {
                    } catch (ExecutionException e2) {
                    }
                }
            }
            if (1 == 0) {
                int size2 = futures.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    futures.get(i2).cancel(true);
                }
            }
            return futures;
        } catch (Throwable th) {
            if (0 != 0) {
                throw th;
            }
            int size3 = futures.size();
            for (int i3 = 0; i3 < size3; i3++) {
                futures.get(i3).cancel(true);
            }
            throw th;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos;
        long deadline;
        if (tasks == null) {
            throw new NullPointerException();
        }
        nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                futures.add(newTaskFor(t));
            }
            deadline = System.nanoTime() + nanos;
            int size = futures.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                execute((Runnable) futures.get(i));
                nanos = deadline - System.nanoTime();
                if (nanos > 0) {
                    i++;
                } else if (0 == 0) {
                    int size2 = futures.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        futures.get(i2).cancel(true);
                    }
                }
            }
            return futures;
        } catch (Throwable th) {
            if (0 != 0) {
                throw th;
            }
            int size3 = futures.size();
            for (int i3 = 0; i3 < size3; i3++) {
                futures.get(i3).cancel(true);
            }
            throw th;
        }
        nanos = deadline - System.nanoTime();
        int i4 = i4 + 1;
    }
}
