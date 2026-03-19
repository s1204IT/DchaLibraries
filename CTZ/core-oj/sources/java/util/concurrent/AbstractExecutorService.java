package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractExecutorService implements ExecutorService {
    static final boolean $assertionsDisabled = false;

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T t) {
        return new FutureTask(runnable, t);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask(callable);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        RunnableFuture runnableFutureNewTaskFor = newTaskFor(runnable, null);
        execute(runnableFutureNewTaskFor);
        return runnableFutureNewTaskFor;
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T t) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> runnableFutureNewTaskFor = newTaskFor(runnable, t);
        execute(runnableFutureNewTaskFor);
        return runnableFutureNewTaskFor;
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> runnableFutureNewTaskFor = newTaskFor(callable);
        execute(runnableFutureNewTaskFor);
        return runnableFutureNewTaskFor;
    }

    private <T> T doInvokeAny(Collection<? extends Callable<T>> collection, boolean z, long j) throws ExecutionException, InterruptedException, TimeoutException {
        long jNanoTime;
        if (collection == null) {
            throw new NullPointerException();
        }
        int size = collection.size();
        if (size == 0) {
            throw new IllegalArgumentException();
        }
        ArrayList arrayList = new ArrayList(size);
        ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(this);
        ExecutionException e = null;
        if (!z) {
            jNanoTime = 0;
        } else {
            try {
                jNanoTime = System.nanoTime() + j;
            } catch (Throwable th) {
                cancelAll(arrayList);
                throw th;
            }
        }
        Iterator<? extends Callable<T>> it = collection.iterator();
        arrayList.add(executorCompletionService.submit(it.next()));
        int i = size - 1;
        int i2 = 1;
        while (true) {
            Future futurePoll = executorCompletionService.poll();
            if (futurePoll == null) {
                if (i > 0) {
                    i--;
                    arrayList.add(executorCompletionService.submit(it.next()));
                    i2++;
                } else if (i2 != 0) {
                    if (z) {
                        futurePoll = executorCompletionService.poll(j, TimeUnit.NANOSECONDS);
                        if (futurePoll == null) {
                            throw new TimeoutException();
                        }
                        j = jNanoTime - System.nanoTime();
                    } else {
                        futurePoll = executorCompletionService.take();
                    }
                } else {
                    if (e == null) {
                        throw new ExecutionException();
                    }
                    throw e;
                }
            }
            if (futurePoll != null) {
                i2--;
                try {
                    T t = (T) futurePoll.get();
                    cancelAll(arrayList);
                    return t;
                } catch (RuntimeException e2) {
                    e = new ExecutionException(e2);
                } catch (ExecutionException e3) {
                    e = e3;
                }
            }
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
        try {
            return (T) doInvokeAny(collection, false, 0L);
        } catch (TimeoutException e) {
            return null;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return (T) doInvokeAny(collection, true, timeUnit.toNanos(j));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
        if (collection == null) {
            throw new NullPointerException();
        }
        ArrayList arrayList = new ArrayList(collection.size());
        try {
            Iterator<? extends Callable<T>> it = collection.iterator();
            while (it.hasNext()) {
                RunnableFuture<T> runnableFutureNewTaskFor = newTaskFor(it.next());
                arrayList.add(runnableFutureNewTaskFor);
                execute(runnableFutureNewTaskFor);
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                Future future = (Future) arrayList.get(i);
                if (!future.isDone()) {
                    try {
                        future.get();
                    } catch (CancellationException e) {
                    } catch (ExecutionException e2) {
                    }
                }
            }
            return arrayList;
        } catch (Throwable th) {
            cancelAll(arrayList);
            throw th;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long j, TimeUnit timeUnit) throws InterruptedException {
        int i;
        if (collection == null) {
            throw new NullPointerException();
        }
        long nanos = timeUnit.toNanos(j);
        long jNanoTime = System.nanoTime() + nanos;
        ArrayList arrayList = new ArrayList(collection.size());
        try {
            Iterator<? extends Callable<T>> it = collection.iterator();
            while (it.hasNext()) {
                arrayList.add(newTaskFor(it.next()));
            }
            int size = arrayList.size();
            i = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= size) {
                    break;
                }
                if ((i2 == 0 ? nanos : jNanoTime - System.nanoTime()) <= 0) {
                    break;
                }
                execute((Runnable) arrayList.get(i2));
                i2++;
            }
            cancelAll(arrayList, i);
            return arrayList;
        } catch (Throwable th) {
            cancelAll(arrayList);
            throw th;
        }
        i++;
    }

    private static <T> void cancelAll(ArrayList<Future<T>> arrayList) {
        cancelAll(arrayList, 0);
    }

    private static <T> void cancelAll(ArrayList<Future<T>> arrayList, int i) {
        int size = arrayList.size();
        while (i < size) {
            arrayList.get(i).cancel(true);
            i++;
        }
    }
}
