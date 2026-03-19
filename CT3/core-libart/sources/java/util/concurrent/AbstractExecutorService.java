package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractExecutorService implements ExecutorService {

    static final boolean f125assertionsDisabled;

    static {
        f125assertionsDisabled = !AbstractExecutorService.class.desiredAssertionStatus();
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
        if (tasks == null) {
            throw new NullPointerException();
        }
        int ntasks = tasks.size();
        if (ntasks == 0) {
            throw new IllegalArgumentException();
        }
        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);
        ExecutorCompletionService<T> ecs = new ExecutorCompletionService<>(this);
        ExecutionException ee = null;
        if (!timed) {
            deadline = 0;
        } else {
            try {
                deadline = System.nanoTime() + nanos;
            } catch (Throwable th2) {
                th = th2;
                cancelAll(futures);
                throw th;
            }
        }
        Iterator<? extends Callable<T>> it = tasks.iterator();
        futures.add(ecs.submit(it.next()));
        int ntasks2 = ntasks - 1;
        int active = 1;
        while (true) {
            try {
                ExecutionException ee2 = ee;
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
                        cancelAll(futures);
                        return t;
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    } catch (ExecutionException eex) {
                        ee = eex;
                    }
                } else {
                    ee = ee2;
                }
            } catch (Throwable th3) {
                th = th3;
                cancelAll(futures);
                throw th;
            }
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
        try {
            return (T) doInvokeAny(collection, false, 0L);
        } catch (TimeoutException e) {
            if (f125assertionsDisabled) {
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
            return futures;
        } catch (Throwable t2) {
            cancelAll(futures);
            throw t2;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        int j;
        if (tasks == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        j = 0;
        try {
            for (Callable<T> t : tasks) {
                futures.add(newTaskFor(t));
            }
            int size = futures.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                if ((i == 0 ? nanos : deadline - System.nanoTime()) <= 0) {
                    break;
                }
                execute((Runnable) futures.get(i));
                i++;
            }
            cancelAll(futures, j);
            return futures;
        } catch (Throwable t2) {
            cancelAll(futures);
            throw t2;
        }
        j++;
    }

    private static <T> void cancelAll(ArrayList<Future<T>> futures) {
        cancelAll(futures, 0);
    }

    private static <T> void cancelAll(ArrayList<Future<T>> futures, int j) {
        int size = futures.size();
        while (j < size) {
            futures.get(j).cancel(true);
            j++;
        }
    }
}
