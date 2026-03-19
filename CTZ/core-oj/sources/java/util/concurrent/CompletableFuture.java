package java.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import sun.misc.Unsafe;

public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    static final int ASYNC = 1;
    private static final Executor ASYNC_POOL;
    static final int NESTED = -1;
    private static final long NEXT;
    static final AltResult NIL = new AltResult(null);
    private static final long RESULT;
    static final int SPINS;
    private static final long STACK;
    static final int SYNC = 0;
    private static final Unsafe U;
    private static final boolean USE_COMMON_POOL;
    volatile Object result;
    volatile Completion stack;

    public interface AsynchronousCompletionTask {
    }

    @Override
    public CompletionStage runAfterBoth(CompletionStage completionStage, Runnable runnable) {
        return runAfterBoth((CompletionStage<?>) completionStage, runnable);
    }

    @Override
    public CompletionStage runAfterBothAsync(CompletionStage completionStage, Runnable runnable) {
        return runAfterBothAsync((CompletionStage<?>) completionStage, runnable);
    }

    @Override
    public CompletionStage runAfterBothAsync(CompletionStage completionStage, Runnable runnable, Executor executor) {
        return runAfterBothAsync((CompletionStage<?>) completionStage, runnable, executor);
    }

    @Override
    public CompletionStage runAfterEither(CompletionStage completionStage, Runnable runnable) {
        return runAfterEither((CompletionStage<?>) completionStage, runnable);
    }

    @Override
    public CompletionStage runAfterEitherAsync(CompletionStage completionStage, Runnable runnable) {
        return runAfterEitherAsync((CompletionStage<?>) completionStage, runnable);
    }

    @Override
    public CompletionStage runAfterEitherAsync(CompletionStage completionStage, Runnable runnable, Executor executor) {
        return runAfterEitherAsync((CompletionStage<?>) completionStage, runnable, executor);
    }

    final boolean internalComplete(Object obj) {
        return U.compareAndSwapObject(this, RESULT, null, obj);
    }

    final boolean casStack(Completion completion, Completion completion2) {
        return U.compareAndSwapObject(this, STACK, completion, completion2);
    }

    final boolean tryPushStack(Completion completion) {
        Completion completion2 = this.stack;
        lazySetNext(completion, completion2);
        return U.compareAndSwapObject(this, STACK, completion2, completion);
    }

    final void pushStack(Completion completion) {
        while (!tryPushStack(completion)) {
        }
    }

    static final class AltResult {
        final Throwable ex;

        AltResult(Throwable th) {
            this.ex = th;
        }
    }

    static {
        USE_COMMON_POOL = ForkJoinPool.getCommonPoolParallelism() > 1;
        ASYNC_POOL = USE_COMMON_POOL ? ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
        SPINS = Runtime.getRuntime().availableProcessors() > 1 ? 256 : 0;
        U = Unsafe.getUnsafe();
        try {
            RESULT = U.objectFieldOffset(CompletableFuture.class.getDeclaredField("result"));
            STACK = U.objectFieldOffset(CompletableFuture.class.getDeclaredField("stack"));
            NEXT = U.objectFieldOffset(Completion.class.getDeclaredField("next"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    final boolean completeNull() {
        return U.compareAndSwapObject(this, RESULT, null, NIL);
    }

    final Object encodeValue(T t) {
        return t == null ? NIL : t;
    }

    final boolean completeValue(T t) {
        Unsafe unsafe = U;
        long j = RESULT;
        if (t == null) {
            t = (T) NIL;
        }
        return unsafe.compareAndSwapObject(this, j, null, t);
    }

    static AltResult encodeThrowable(Throwable th) {
        if (!(th instanceof CompletionException)) {
            th = new CompletionException(th);
        }
        return new AltResult(th);
    }

    final boolean completeThrowable(Throwable th) {
        return U.compareAndSwapObject(this, RESULT, null, encodeThrowable(th));
    }

    static Object encodeThrowable(Throwable th, Object obj) {
        if (!(th instanceof CompletionException)) {
            th = new CompletionException(th);
        } else if ((obj instanceof AltResult) && th == ((AltResult) obj).ex) {
            return obj;
        }
        return new AltResult(th);
    }

    final boolean completeThrowable(Throwable th, Object obj) {
        return U.compareAndSwapObject(this, RESULT, null, encodeThrowable(th, obj));
    }

    Object encodeOutcome(T t, Throwable th) {
        return th == null ? t == null ? NIL : t : encodeThrowable(th);
    }

    static Object encodeRelay(Object obj) {
        Throwable th;
        if (!(obj instanceof AltResult) || (th = ((AltResult) obj).ex) == null || (th instanceof CompletionException)) {
            return obj;
        }
        return new AltResult(new CompletionException(th));
    }

    final boolean completeRelay(Object obj) {
        return U.compareAndSwapObject(this, RESULT, null, encodeRelay(obj));
    }

    private static <T> T reportGet(Object obj) throws ExecutionException, InterruptedException {
        Throwable cause;
        if (obj == 0) {
            throw new InterruptedException();
        }
        if (obj instanceof AltResult) {
            Throwable th = ((AltResult) obj).ex;
            if (th == null) {
                return null;
            }
            if (th instanceof CancellationException) {
                throw ((CancellationException) th);
            }
            if ((th instanceof CompletionException) && (cause = th.getCause()) != null) {
                th = cause;
            }
            throw new ExecutionException(th);
        }
        return obj;
    }

    private static <T> T reportJoin(Object obj) {
        if (obj instanceof AltResult) {
            Throwable th = ((AltResult) obj).ex;
            if (th == null) {
                return null;
            }
            if (th instanceof CancellationException) {
                throw ((CancellationException) th);
            }
            if (th instanceof CompletionException) {
                throw ((CompletionException) th);
            }
            throw new CompletionException(th);
        }
        return obj;
    }

    static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {
        }

        @Override
        public void execute(Runnable runnable) {
            new Thread(runnable).start();
        }
    }

    static Executor screenExecutor(Executor executor) {
        if (!USE_COMMON_POOL && executor == ForkJoinPool.commonPool()) {
            return ASYNC_POOL;
        }
        if (executor == null) {
            throw new NullPointerException();
        }
        return executor;
    }

    static abstract class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;

        abstract boolean isLive();

        abstract CompletableFuture<?> tryFire(int i);

        Completion() {
        }

        @Override
        public final void run() {
            tryFire(1);
        }

        @Override
        public final boolean exec() {
            tryFire(1);
            return false;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void r1) {
        }
    }

    static void lazySetNext(Completion completion, Completion completion2) {
        U.putOrderedObject(completion, NEXT, completion2);
    }

    final void postComplete() {
        CompletableFuture completableFutureTryFire = this;
        while (true) {
            Completion completion = completableFutureTryFire.stack;
            if (completion == null) {
                if (completableFutureTryFire != this && (completion = this.stack) != null) {
                    completableFutureTryFire = this;
                } else {
                    return;
                }
            }
            Completion completion2 = completion.next;
            if (completableFutureTryFire.casStack(completion, completion2)) {
                if (completion2 != null) {
                    if (completableFutureTryFire != this) {
                        pushStack(completion);
                    } else {
                        completion.next = null;
                    }
                }
                completableFutureTryFire = completion.tryFire(-1);
                if (completableFutureTryFire == null) {
                    completableFutureTryFire = this;
                }
            }
        }
    }

    final void cleanStack() {
        Completion completion = this.stack;
        Completion completion2 = null;
        while (completion != null) {
            Completion completion3 = completion.next;
            if (!completion.isLive()) {
                if (completion2 == null) {
                    casStack(completion, completion3);
                    completion = this.stack;
                } else {
                    completion2.next = completion3;
                    if (!completion2.isLive()) {
                        completion = this.stack;
                        completion2 = null;
                    }
                }
            } else {
                completion2 = completion;
            }
            completion = completion3;
        }
    }

    static abstract class UniCompletion<T, V> extends Completion {
        CompletableFuture<V> dep;
        Executor executor;
        CompletableFuture<T> src;

        UniCompletion(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2) {
            this.executor = executor;
            this.dep = completableFuture;
            this.src = completableFuture2;
        }

        final boolean claim() {
            Executor executor = this.executor;
            if (compareAndSetForkJoinTaskTag((short) 0, (short) 1)) {
                if (executor == null) {
                    return true;
                }
                this.executor = null;
                executor.execute(this);
            }
            return false;
        }

        @Override
        final boolean isLive() {
            return this.dep != null;
        }
    }

    final void push(UniCompletion<?, ?> uniCompletion) {
        if (uniCompletion != null) {
            while (this.result == null && !tryPushStack(uniCompletion)) {
                lazySetNext(uniCompletion, null);
            }
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> completableFuture, int i) {
        if (completableFuture != null && completableFuture.stack != null) {
            if (i < 0 || completableFuture.result == null) {
                completableFuture.cleanStack();
            } else {
                completableFuture.postComplete();
            }
        }
        if (this.result != null && this.stack != null) {
            if (i < 0) {
                return this;
            }
            postComplete();
            return null;
        }
        return null;
    }

    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn;

        UniApply(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, Function<? super T, ? extends V> function) {
            super(executor, completableFuture, completableFuture2);
            this.fn = function;
        }

        @Override
        final CompletableFuture<V> tryFire(int i) {
            CompletableFuture<V> completableFuture = this.dep;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                if (completableFuture.uniApply((CompletableFuture<S>) completableFuture2, (Function<? super S, ? extends V>) this.fn, (UniApply<S, V>) (i > 0 ? null : this))) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return completableFuture.postFire((CompletableFuture<?>) completableFuture2, i);
                }
            }
            return null;
        }
    }

    final <S> boolean uniApply(CompletableFuture<S> completableFuture, Function<? super S, ? extends T> function, UniApply<S, T> uniApply) {
        AltResult altResult;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || function == null) {
            return false;
        }
        if (this.result == null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completeThrowable(th, altResult);
                    return true;
                }
                altResult = null;
            }
            if (uniApply != null) {
                try {
                    if (!uniApply.claim()) {
                        return false;
                    }
                } catch (Throwable th2) {
                    completeThrowable(th2);
                    return true;
                }
            }
            completeValue(function.apply(altResult));
            return true;
        }
        return true;
    }

    private <V> CompletableFuture<V> uniApplyStage(Executor executor, Function<? super T, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> completableFuture = (CompletableFuture<V>) newIncompleteFuture();
        if (executor != null || !completableFuture.uniApply(this, function, null)) {
            UniApply uniApply = new UniApply(executor, completableFuture, this, function);
            push(uniApply);
            uniApply.tryFire(0);
        }
        return completableFuture;
    }

    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> fn;

        UniAccept(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, Consumer<? super T> consumer) {
            super(executor, completableFuture, completableFuture2);
            this.fn = consumer;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <S> boolean uniAccept(CompletableFuture<S> completableFuture, Consumer<? super S> consumer, UniAccept<S> uniAccept) {
        AltResult altResult;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || consumer == null) {
            return false;
        }
        if (this.result == null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completeThrowable(th, altResult);
                    return true;
                }
                altResult = null;
            }
            if (uniAccept != null) {
                try {
                    if (!uniAccept.claim()) {
                        return false;
                    }
                } catch (Throwable th2) {
                    completeThrowable(th2);
                    return true;
                }
            }
            consumer.accept(altResult);
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> uniAcceptStage(Executor executor, Consumer<? super T> consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.uniAccept(this, consumer, null)) {
            UniAccept uniAccept = new UniAccept(executor, completableFutureNewIncompleteFuture, this, consumer);
            push(uniAccept);
            uniAccept.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable fn;

        UniRun(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, Runnable runnable) {
            super(executor, completableFuture, completableFuture2);
            this.fn = runnable;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniRun(CompletableFuture<?> completableFuture, Runnable runnable, UniRun<?> uniRun) {
        Object obj;
        Throwable th;
        if (completableFuture == null || (obj = completableFuture.result) == null || runnable == null) {
            return false;
        }
        if (this.result == null) {
            if ((obj instanceof AltResult) && (th = ((AltResult) obj).ex) != null) {
                completeThrowable(th, obj);
                return true;
            }
            if (uniRun != null) {
                try {
                    if (!uniRun.claim()) {
                        return false;
                    }
                } catch (Throwable th2) {
                    completeThrowable(th2);
                    return true;
                }
            }
            runnable.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> uniRunStage(Executor executor, Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.uniRun(this, runnable, null)) {
            UniRun uniRun = new UniRun(executor, completableFutureNewIncompleteFuture, this, runnable);
            push(uniRun);
            uniRun.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;

        UniWhenComplete(Executor executor, CompletableFuture<T> completableFuture, CompletableFuture<T> completableFuture2, BiConsumer<? super T, ? super Throwable> biConsumer) {
            super(executor, completableFuture, completableFuture2);
            this.fn = biConsumer;
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniWhenComplete.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniWhenComplete(CompletableFuture<T> completableFuture, BiConsumer<? super T, ? super Throwable> biConsumer, UniWhenComplete<T> uniWhenComplete) {
        Object obj;
        Throwable th;
        Throwable th2;
        if (completableFuture == null || (obj = completableFuture.result) == null || biConsumer == null) {
            return false;
        }
        if (this.result == null) {
            Throwable th3 = null;
            if (uniWhenComplete != null) {
                try {
                    if (!uniWhenComplete.claim()) {
                        return false;
                    }
                } catch (Throwable th4) {
                    th2 = th4;
                    th = th2;
                    if (th3 != null) {
                        if (th3 != th) {
                            th3.addSuppressed(th);
                        }
                        th = th3;
                    }
                    completeThrowable(th, obj);
                    return true;
                }
            }
            if (obj instanceof AltResult) {
                th = ((AltResult) obj).ex;
            } else {
                th = null;
                th3 = (Object) obj;
            }
            try {
                biConsumer.accept(th3, th);
                if (th == null) {
                    internalComplete(obj);
                    return true;
                }
            } catch (Throwable th5) {
                th2 = th5;
                th3 = th;
                th = th2;
                if (th3 != null) {
                }
            }
            completeThrowable(th, obj);
        }
        return true;
    }

    private CompletableFuture<T> uniWhenCompleteStage(Executor executor, BiConsumer<? super T, ? super Throwable> biConsumer) {
        if (biConsumer == null) {
            throw new NullPointerException();
        }
        CompletableFuture<T> completableFuture = (CompletableFuture<T>) newIncompleteFuture();
        if (executor != null || !completableFuture.uniWhenComplete(this, biConsumer, null)) {
            UniWhenComplete uniWhenComplete = new UniWhenComplete(executor, completableFuture, this, biConsumer);
            push(uniWhenComplete);
            uniWhenComplete.tryFire(0);
        }
        return completableFuture;
    }

    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> fn;

        UniHandle(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, BiFunction<? super T, Throwable, ? extends V> biFunction) {
            super(executor, completableFuture, completableFuture2);
            this.fn = biFunction;
        }

        @Override
        final CompletableFuture<V> tryFire(int i) {
            CompletableFuture<V> completableFuture = this.dep;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                if (completableFuture.uniHandle((CompletableFuture<S>) completableFuture2, (BiFunction<? super S, Throwable, ? extends V>) this.fn, (UniHandle<S, V>) (i > 0 ? null : this))) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return completableFuture.postFire((CompletableFuture<?>) completableFuture2, i);
                }
            }
            return null;
        }
    }

    final <S> boolean uniHandle(CompletableFuture<S> completableFuture, BiFunction<? super S, Throwable, ? extends T> biFunction, UniHandle<S, T> uniHandle) {
        AltResult altResult;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || biFunction == null) {
            return false;
        }
        if (this.result == null) {
            if (uniHandle != null) {
                try {
                    if (!uniHandle.claim()) {
                        return false;
                    }
                } catch (Throwable th) {
                    completeThrowable(th);
                    return true;
                }
            }
            Throwable th2 = null;
            if (altResult instanceof AltResult) {
                th2 = altResult.ex;
                altResult = null;
            }
            completeValue(biFunction.apply(altResult, th2));
            return true;
        }
        return true;
    }

    private <V> CompletableFuture<V> uniHandleStage(Executor executor, BiFunction<? super T, Throwable, ? extends V> biFunction) {
        if (biFunction == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> completableFuture = (CompletableFuture<V>) newIncompleteFuture();
        if (executor != null || !completableFuture.uniHandle(this, biFunction, null)) {
            UniHandle uniHandle = new UniHandle(executor, completableFuture, this, biFunction);
            push(uniHandle);
            uniHandle.tryFire(0);
        }
        return completableFuture;
    }

    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> fn;

        UniExceptionally(CompletableFuture<T> completableFuture, CompletableFuture<T> completableFuture2, Function<? super Throwable, ? extends T> function) {
            super(null, completableFuture, completableFuture2);
            this.fn = function;
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r5) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniExceptionally.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniExceptionally(CompletableFuture<T> completableFuture, Function<? super Throwable, ? extends T> function, UniExceptionally<T> uniExceptionally) {
        Object obj;
        Throwable th;
        if (completableFuture == null || (obj = completableFuture.result) == null || function == null) {
            return false;
        }
        if (this.result == null) {
            try {
                if ((obj instanceof AltResult) && (th = ((AltResult) obj).ex) != null) {
                    if (uniExceptionally != null && !uniExceptionally.claim()) {
                        return false;
                    }
                    completeValue(function.apply(th));
                    return true;
                }
                internalComplete(obj);
                return true;
            } catch (Throwable th2) {
                completeThrowable(th2);
                return true;
            }
        }
        return true;
    }

    private CompletableFuture<T> uniExceptionallyStage(Function<Throwable, ? extends T> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        CompletableFuture<T> completableFuture = (CompletableFuture<T>) newIncompleteFuture();
        if (!completableFuture.uniExceptionally(this, function, null)) {
            UniExceptionally uniExceptionally = new UniExceptionally(completableFuture, this, function);
            push(uniExceptionally);
            uniExceptionally.tryFire(0);
        }
        return completableFuture;
    }

    static final class UniRelay<T> extends UniCompletion<T, T> {
        UniRelay(CompletableFuture<T> completableFuture, CompletableFuture<T> completableFuture2) {
            super(null, completableFuture, completableFuture2);
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r5) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniRelay.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniRelay(CompletableFuture<T> completableFuture) {
        Object obj;
        if (completableFuture == null || (obj = completableFuture.result) == null) {
            return false;
        }
        if (this.result == null) {
            completeRelay(obj);
            return true;
        }
        return true;
    }

    private CompletableFuture<T> uniCopyStage() {
        CompletableFuture<T> completableFuture = (CompletableFuture<T>) newIncompleteFuture();
        Object obj = this.result;
        if (obj != null) {
            completableFuture.completeRelay(obj);
        } else {
            UniRelay uniRelay = new UniRelay(completableFuture, this);
            push(uniRelay);
            uniRelay.tryFire(0);
        }
        return completableFuture;
    }

    private MinimalStage<T> uniAsMinimalStage() {
        Object obj = this.result;
        if (obj != null) {
            return new MinimalStage<>(encodeRelay(obj));
        }
        MinimalStage<T> minimalStage = new MinimalStage<>();
        UniRelay uniRelay = new UniRelay(minimalStage, this);
        push(uniRelay);
        uniRelay.tryFire(0);
        return minimalStage;
    }

    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> fn;

        UniCompose(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, Function<? super T, ? extends CompletionStage<V>> function) {
            super(executor, completableFuture, completableFuture2);
            this.fn = function;
        }

        @Override
        final CompletableFuture<V> tryFire(int i) {
            CompletableFuture<V> completableFuture = this.dep;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                if (completableFuture.uniCompose((CompletableFuture<S>) completableFuture2, (Function<? super S, ? extends CompletionStage<V>>) this.fn, (UniCompose<S, V>) (i > 0 ? null : this))) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return completableFuture.postFire((CompletableFuture<?>) completableFuture2, i);
                }
            }
            return null;
        }
    }

    final <S> boolean uniCompose(CompletableFuture<S> completableFuture, Function<? super S, ? extends CompletionStage<T>> function, UniCompose<S, T> uniCompose) {
        AltResult altResult;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || function == null) {
            return false;
        }
        if (this.result == null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completeThrowable(th, altResult);
                    return true;
                }
                altResult = null;
            }
            if (uniCompose != null) {
                try {
                    if (!uniCompose.claim()) {
                        return false;
                    }
                } catch (Throwable th2) {
                    completeThrowable(th2);
                    return true;
                }
            }
            CompletableFuture<T> completableFuture2 = function.apply(altResult).toCompletableFuture();
            if (completableFuture2.result == null || !uniRelay(completableFuture2)) {
                UniRelay uniRelay = new UniRelay(this, completableFuture2);
                completableFuture2.push(uniRelay);
                uniRelay.tryFire(0);
                if (this.result == null) {
                    return false;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    private <V> CompletableFuture<V> uniComposeStage(Executor executor, Function<? super T, ? extends CompletionStage<V>> function) {
        AltResult altResult;
        if (function == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> completableFuture = (CompletableFuture<V>) newIncompleteFuture();
        if (executor == null && (altResult = (Object) this.result) != null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completableFuture.result = encodeThrowable(th, altResult);
                    return completableFuture;
                }
                altResult = null;
            }
            try {
                CompletableFuture<V> completableFuture2 = function.apply(altResult).toCompletableFuture();
                Object obj = completableFuture2.result;
                if (obj != null) {
                    completableFuture.completeRelay(obj);
                } else {
                    UniRelay uniRelay = new UniRelay(completableFuture, completableFuture2);
                    completableFuture2.push(uniRelay);
                    uniRelay.tryFire(0);
                }
                return completableFuture;
            } catch (Throwable th2) {
                completableFuture.result = encodeThrowable(th2);
                return completableFuture;
            }
        }
        UniCompose uniCompose = new UniCompose(executor, completableFuture, this, function);
        push(uniCompose);
        uniCompose.tryFire(0);
        return completableFuture;
    }

    static abstract class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> snd;

        BiCompletion(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3) {
            super(executor, completableFuture, completableFuture2);
            this.snd = completableFuture3;
        }
    }

    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;

        CoCompletion(BiCompletion<?, ?, ?> biCompletion) {
            this.base = biCompletion;
        }

        @Override
        final CompletableFuture<?> tryFire(int i) {
            CompletableFuture<?> completableFutureTryFire;
            BiCompletion<?, ?, ?> biCompletion = this.base;
            if (biCompletion == null || (completableFutureTryFire = biCompletion.tryFire(i)) == null) {
                return null;
            }
            this.base = null;
            return completableFutureTryFire;
        }

        @Override
        final boolean isLive() {
            BiCompletion<?, ?, ?> biCompletion = this.base;
            return (biCompletion == null || biCompletion.dep == null) ? false : true;
        }
    }

    final void bipush(CompletableFuture<?> completableFuture, BiCompletion<?, ?, ?> biCompletion) {
        Object obj;
        if (biCompletion != null) {
            while (true) {
                obj = this.result;
                if (obj != null || tryPushStack(biCompletion)) {
                    break;
                } else {
                    lazySetNext(biCompletion, null);
                }
            }
            if (completableFuture == null || completableFuture == this) {
                return;
            }
            Completion coCompletion = biCompletion;
            if (completableFuture.result == null) {
                if (obj == null) {
                    coCompletion = new CoCompletion(biCompletion);
                }
                while (completableFuture.result == null && !completableFuture.tryPushStack(coCompletion)) {
                    lazySetNext(coCompletion, null);
                }
            }
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> completableFuture, CompletableFuture<?> completableFuture2, int i) {
        if (completableFuture2 != null && completableFuture2.stack != null) {
            if (i < 0 || completableFuture2.result == null) {
                completableFuture2.cleanStack();
            } else {
                completableFuture2.postComplete();
            }
        }
        return postFire(completableFuture, i);
    }

    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> fn;

        BiApply(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, BiFunction<? super T, ? super U, ? extends V> biFunction) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = biFunction;
        }

        @Override
        final CompletableFuture<V> tryFire(int i) {
            CompletableFuture<V> completableFuture = this.dep;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<U> completableFuture3 = this.snd;
                if (completableFuture.biApply((CompletableFuture<R>) completableFuture2, (CompletableFuture<S>) completableFuture3, (BiFunction<? super R, ? super S, ? extends V>) this.fn, (BiApply<R, S, V>) (i > 0 ? null : this))) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return completableFuture.postFire((CompletableFuture<?>) completableFuture2, (CompletableFuture<?>) completableFuture3, i);
                }
            }
            return null;
        }
    }

    final <R, S> boolean biApply(CompletableFuture<R> completableFuture, CompletableFuture<S> completableFuture2, BiFunction<? super R, ? super S, ? extends T> biFunction, BiApply<R, S, T> biApply) {
        AltResult altResult;
        AltResult altResult2;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || completableFuture2 == null || (altResult2 = (Object) completableFuture2.result) == null || biFunction == null) {
            return false;
        }
        if (this.result == null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completeThrowable(th, altResult);
                    return true;
                }
                altResult = null;
            }
            if (altResult2 instanceof AltResult) {
                Throwable th2 = altResult2.ex;
                if (th2 != null) {
                    completeThrowable(th2, altResult2);
                    return true;
                }
                altResult2 = null;
            }
            if (biApply != null) {
                try {
                    if (!biApply.claim()) {
                        return false;
                    }
                } catch (Throwable th3) {
                    completeThrowable(th3);
                    return true;
                }
            }
            completeValue(biFunction.apply(altResult, altResult2));
            return true;
        }
        return true;
    }

    private <U, V> CompletableFuture<V> biApplyStage(Executor executor, CompletionStage<U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        CompletableFuture<U> completableFuture;
        if (biFunction == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.biApply(this, completableFuture, biFunction, null)) {
            BiApply biApply = new BiApply(executor, completableFutureNewIncompleteFuture, this, completableFuture, biFunction);
            bipush(completableFuture, biApply);
            biApply.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class BiAccept<T, U> extends BiCompletion<T, U, Void> {
        BiConsumer<? super T, ? super U> fn;

        BiAccept(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, BiConsumer<? super T, ? super U> biConsumer) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = biConsumer;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <R, S> boolean biAccept(CompletableFuture<R> completableFuture, CompletableFuture<S> completableFuture2, BiConsumer<? super R, ? super S> biConsumer, BiAccept<R, S> biAccept) {
        AltResult altResult;
        AltResult altResult2;
        if (completableFuture == null || (altResult = (Object) completableFuture.result) == null || completableFuture2 == null || (altResult2 = (Object) completableFuture2.result) == null || biConsumer == null) {
            return false;
        }
        if (this.result == null) {
            if (altResult instanceof AltResult) {
                Throwable th = altResult.ex;
                if (th != null) {
                    completeThrowable(th, altResult);
                    return true;
                }
                altResult = null;
            }
            if (altResult2 instanceof AltResult) {
                Throwable th2 = altResult2.ex;
                if (th2 != null) {
                    completeThrowable(th2, altResult2);
                    return true;
                }
                altResult2 = null;
            }
            if (biAccept != null) {
                try {
                    if (!biAccept.claim()) {
                        return false;
                    }
                } catch (Throwable th3) {
                    completeThrowable(th3);
                    return true;
                }
            }
            biConsumer.accept(altResult, altResult2);
            completeNull();
            return true;
        }
        return true;
    }

    private <U> CompletableFuture<Void> biAcceptStage(Executor executor, CompletionStage<U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        CompletableFuture<U> completableFuture;
        if (biConsumer == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.biAccept(this, completableFuture, biConsumer, null)) {
            BiAccept biAccept = new BiAccept(executor, completableFutureNewIncompleteFuture, this, completableFuture, biConsumer);
            bipush(completableFuture, biAccept);
            biAccept.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class BiRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;

        BiRun(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, Runnable runnable) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = runnable;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean biRun(CompletableFuture<?> completableFuture, CompletableFuture<?> completableFuture2, Runnable runnable, BiRun<?, ?> biRun) {
        Object obj;
        Object obj2;
        Throwable th;
        Throwable th2;
        if (completableFuture == null || (obj = completableFuture.result) == null || completableFuture2 == null || (obj2 = completableFuture2.result) == null || runnable == null) {
            return false;
        }
        if (this.result == null) {
            if ((obj instanceof AltResult) && (th2 = ((AltResult) obj).ex) != null) {
                completeThrowable(th2, obj);
                return true;
            }
            if ((obj2 instanceof AltResult) && (th = ((AltResult) obj2).ex) != null) {
                completeThrowable(th, obj2);
                return true;
            }
            if (biRun != null) {
                try {
                    if (!biRun.claim()) {
                        return false;
                    }
                } catch (Throwable th3) {
                    completeThrowable(th3);
                    return true;
                }
            }
            runnable.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> biRunStage(Executor executor, CompletionStage<?> completionStage, Runnable runnable) {
        CompletableFuture<?> completableFuture;
        if (runnable == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.biRun(this, completableFuture, runnable, null)) {
            BiRun biRun = new BiRun(executor, completableFutureNewIncompleteFuture, this, completableFuture, runnable);
            bipush(completableFuture, biRun);
            biRun.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> {
        BiRelay(CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3) {
            super(null, completableFuture, completableFuture2, completableFuture3);
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiRelay.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    boolean biRelay(CompletableFuture<?> completableFuture, CompletableFuture<?> completableFuture2) {
        Object obj;
        Object obj2;
        Throwable th;
        Throwable th2;
        if (completableFuture == null || (obj = completableFuture.result) == null || completableFuture2 == null || (obj2 = completableFuture2.result) == null) {
            return false;
        }
        if (this.result == null) {
            if ((obj instanceof AltResult) && (th2 = ((AltResult) obj).ex) != null) {
                completeThrowable(th2, obj);
                return true;
            }
            if ((obj2 instanceof AltResult) && (th = ((AltResult) obj2).ex) != null) {
                completeThrowable(th, obj2);
                return true;
            }
            completeNull();
            return true;
        }
        return true;
    }

    static CompletableFuture<Void> andTree(CompletableFuture<?>[] completableFutureArr, int i, int i2) {
        CompletableFuture<?> completableFutureAndTree;
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        if (i > i2) {
            completableFuture.result = NIL;
        } else {
            int i3 = (i + i2) >>> 1;
            CompletableFuture<?> completableFutureAndTree2 = i == i3 ? completableFutureArr[i] : andTree(completableFutureArr, i, i3);
            if (completableFutureAndTree2 != null) {
                if (i != i2) {
                    int i4 = i3 + 1;
                    completableFutureAndTree = i2 == i4 ? completableFutureArr[i2] : andTree(completableFutureArr, i4, i2);
                } else {
                    completableFutureAndTree = completableFutureAndTree2;
                }
                if (completableFutureAndTree != null) {
                    if (!completableFuture.biRelay(completableFutureAndTree2, completableFutureAndTree)) {
                        BiRelay biRelay = new BiRelay(completableFuture, completableFutureAndTree2, completableFutureAndTree);
                        completableFutureAndTree2.bipush(completableFutureAndTree, biRelay);
                        biRelay.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return completableFuture;
    }

    final void orpush(CompletableFuture<?> completableFuture, BiCompletion<?, ?, ?> biCompletion) {
        if (biCompletion == null) {
            return;
        }
        while (true) {
            if ((completableFuture == null || completableFuture.result == null) && this.result == null) {
                if (tryPushStack(biCompletion)) {
                    if (completableFuture != null && completableFuture != this && completableFuture.result == null) {
                        CoCompletion coCompletion = new CoCompletion(biCompletion);
                        while (this.result == null && completableFuture.result == null && !completableFuture.tryPushStack(coCompletion)) {
                            lazySetNext(coCompletion, null);
                        }
                        return;
                    }
                    return;
                }
                lazySetNext(biCompletion, null);
            } else {
                return;
            }
        }
    }

    static final class OrApply<T, U extends T, V> extends BiCompletion<T, U, V> {
        Function<? super T, ? extends V> fn;

        OrApply(Executor executor, CompletableFuture<V> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, Function<? super T, ? extends V> function) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = function;
        }

        @Override
        final CompletableFuture<V> tryFire(int i) {
            CompletableFuture<V> completableFuture = this.dep;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<U> completableFuture3 = this.snd;
                if (completableFuture.orApply((CompletableFuture<R>) completableFuture2, (CompletableFuture<S>) completableFuture3, (Function<? super R, ? extends V>) this.fn, (OrApply<R, S, V>) (i > 0 ? null : this))) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return completableFuture.postFire((CompletableFuture<?>) completableFuture2, (CompletableFuture<?>) completableFuture3, i);
                }
            }
            return null;
        }
    }

    final <R, S extends R> boolean orApply(CompletableFuture<R> completableFuture, CompletableFuture<S> completableFuture2, Function<? super R, ? extends T> function, OrApply<R, S, T> orApply) {
        Object obj;
        if (completableFuture == null || completableFuture2 == null || (((obj = completableFuture.result) == null && (obj = completableFuture2.result) == null) || function == null)) {
            return false;
        }
        if (this.result == null) {
            if (orApply != null) {
                try {
                    if (!orApply.claim()) {
                        return false;
                    }
                } catch (Throwable th) {
                    completeThrowable(th);
                    return true;
                }
            }
            if (obj instanceof AltResult) {
                Throwable th2 = ((AltResult) obj).ex;
                if (th2 != null) {
                    completeThrowable(th2, obj);
                    return true;
                }
                obj = null;
            }
            completeValue(function.apply(obj));
            return true;
        }
        return true;
    }

    private <U extends T, V> CompletableFuture<V> orApplyStage(Executor executor, CompletionStage<U> completionStage, Function<? super T, ? extends V> function) {
        CompletableFuture<U> completableFuture;
        if (function == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> completableFuture2 = (CompletableFuture<V>) newIncompleteFuture();
        if (executor != null || !completableFuture2.orApply(this, completableFuture, function, null)) {
            OrApply orApply = new OrApply(executor, completableFuture2, this, completableFuture, function);
            orpush(completableFuture, orApply);
            orApply.tryFire(0);
        }
        return completableFuture2;
    }

    static final class OrAccept<T, U extends T> extends BiCompletion<T, U, Void> {
        Consumer<? super T> fn;

        OrAccept(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, Consumer<? super T> consumer) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = consumer;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.OrAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <R, S extends R> boolean orAccept(CompletableFuture<R> completableFuture, CompletableFuture<S> completableFuture2, Consumer<? super R> consumer, OrAccept<R, S> orAccept) {
        Object obj;
        if (completableFuture == null || completableFuture2 == null || (((obj = completableFuture.result) == null && (obj = completableFuture2.result) == null) || consumer == null)) {
            return false;
        }
        if (this.result == null) {
            if (orAccept != null) {
                try {
                    if (!orAccept.claim()) {
                        return false;
                    }
                } catch (Throwable th) {
                    completeThrowable(th);
                    return true;
                }
            }
            if (obj instanceof AltResult) {
                Throwable th2 = ((AltResult) obj).ex;
                if (th2 != null) {
                    completeThrowable(th2, obj);
                    return true;
                }
                obj = null;
            }
            consumer.accept(obj);
            completeNull();
            return true;
        }
        return true;
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(Executor executor, CompletionStage<U> completionStage, Consumer<? super T> consumer) {
        CompletableFuture<?> completableFuture;
        if (consumer == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.orAccept(this, completableFuture, consumer, null)) {
            OrAccept orAccept = new OrAccept(executor, completableFutureNewIncompleteFuture, this, completableFuture, consumer);
            orpush(completableFuture, orAccept);
            orAccept.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class OrRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;

        OrRun(Executor executor, CompletableFuture<Void> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3, Runnable runnable) {
            super(executor, completableFuture, completableFuture2, completableFuture3);
            this.fn = runnable;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.OrRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean orRun(CompletableFuture<?> completableFuture, CompletableFuture<?> completableFuture2, Runnable runnable, OrRun<?, ?> orRun) {
        Object obj;
        Throwable th;
        if (completableFuture == null || completableFuture2 == null || (((obj = completableFuture.result) == null && (obj = completableFuture2.result) == null) || runnable == null)) {
            return false;
        }
        if (this.result == null) {
            if (orRun != null) {
                try {
                    if (!orRun.claim()) {
                        return false;
                    }
                } catch (Throwable th2) {
                    completeThrowable(th2);
                    return true;
                }
            }
            if ((obj instanceof AltResult) && (th = ((AltResult) obj).ex) != null) {
                completeThrowable(th, obj);
                return true;
            }
            runnable.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> orRunStage(Executor executor, CompletionStage<?> completionStage, Runnable runnable) {
        CompletableFuture<?> completableFuture;
        if (runnable == null || (completableFuture = completionStage.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (executor != null || !completableFutureNewIncompleteFuture.orRun(this, completableFuture, runnable, null)) {
            OrRun orRun = new OrRun(executor, completableFutureNewIncompleteFuture, this, completableFuture, runnable);
            orpush(completableFuture, orRun);
            orRun.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class OrRelay<T, U> extends BiCompletion<T, U, Object> {
        OrRelay(CompletableFuture<Object> completableFuture, CompletableFuture<T> completableFuture2, CompletableFuture<U> completableFuture3) {
            super(null, completableFuture, completableFuture2, completableFuture3);
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Object> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.OrRelay.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean orRelay(CompletableFuture<?> completableFuture, CompletableFuture<?> completableFuture2) {
        if (completableFuture == null || completableFuture2 == null) {
            return false;
        }
        Object obj = completableFuture.result;
        if (obj == null && (obj = completableFuture2.result) == null) {
            return false;
        }
        if (this.result == null) {
            completeRelay(obj);
            return true;
        }
        return true;
    }

    static CompletableFuture<Object> orTree(CompletableFuture<?>[] completableFutureArr, int i, int i2) {
        CompletableFuture<?> completableFutureOrTree;
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        if (i <= i2) {
            int i3 = (i + i2) >>> 1;
            CompletableFuture<?> completableFutureOrTree2 = i == i3 ? completableFutureArr[i] : orTree(completableFutureArr, i, i3);
            if (completableFutureOrTree2 != null) {
                if (i != i2) {
                    int i4 = i3 + 1;
                    completableFutureOrTree = i2 == i4 ? completableFutureArr[i2] : orTree(completableFutureArr, i4, i2);
                } else {
                    completableFutureOrTree = completableFutureOrTree2;
                }
                if (completableFutureOrTree != null) {
                    if (!completableFuture.orRelay(completableFutureOrTree2, completableFutureOrTree)) {
                        OrRelay orRelay = new OrRelay(completableFuture, completableFutureOrTree2, completableFutureOrTree);
                        completableFutureOrTree2.orpush(completableFutureOrTree, orRelay);
                        orRelay.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return completableFuture;
    }

    static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep;
        Supplier<? extends T> fn;

        AsyncSupply(CompletableFuture<T> completableFuture, Supplier<? extends T> supplier) {
            this.dep = completableFuture;
            this.fn = supplier;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void r1) {
        }

        @Override
        public final boolean exec() {
            run();
            return true;
        }

        @Override
        public void run() {
            Supplier<? extends T> supplier;
            CompletableFuture<T> completableFuture = this.dep;
            if (completableFuture != null && (supplier = this.fn) != null) {
                this.dep = null;
                this.fn = null;
                if (completableFuture.result == null) {
                    try {
                        completableFuture.completeValue(supplier.get());
                    } catch (Throwable th) {
                        completableFuture.completeThrowable(th);
                    }
                }
                completableFuture.postComplete();
            }
        }
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor executor, Supplier<U> supplier) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> completableFuture = new CompletableFuture<>();
        executor.execute(new AsyncSupply(completableFuture, supplier));
        return completableFuture;
    }

    static final class AsyncRun extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep;
        Runnable fn;

        AsyncRun(CompletableFuture<Void> completableFuture, Runnable runnable) {
            this.dep = completableFuture;
            this.fn = runnable;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void r1) {
        }

        @Override
        public final boolean exec() {
            run();
            return true;
        }

        @Override
        public void run() {
            Runnable runnable;
            CompletableFuture<Void> completableFuture = this.dep;
            if (completableFuture != null && (runnable = this.fn) != null) {
                this.dep = null;
                this.fn = null;
                if (completableFuture.result == null) {
                    try {
                        runnable.run();
                        completableFuture.completeNull();
                    } catch (Throwable th) {
                        completableFuture.completeThrowable(th);
                    }
                }
                completableFuture.postComplete();
            }
        }
    }

    static CompletableFuture<Void> asyncRunStage(Executor executor, Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        executor.execute(new AsyncRun(completableFuture, runnable));
        return completableFuture;
    }

    static final class Signaller extends Completion implements ForkJoinPool.ManagedBlocker {
        final long deadline;
        boolean interrupted;
        final boolean interruptible;
        long nanos;
        volatile Thread thread = Thread.currentThread();

        Signaller(boolean z, long j, long j2) {
            this.interruptible = z;
            this.nanos = j;
            this.deadline = j2;
        }

        @Override
        final CompletableFuture<?> tryFire(int i) {
            Thread thread = this.thread;
            if (thread != null) {
                this.thread = null;
                LockSupport.unpark(thread);
            }
            return null;
        }

        @Override
        public boolean isReleasable() {
            if (Thread.interrupted()) {
                this.interrupted = true;
            }
            if (this.interrupted && this.interruptible) {
                return true;
            }
            if (this.deadline != 0) {
                if (this.nanos <= 0) {
                    return true;
                }
                long jNanoTime = this.deadline - System.nanoTime();
                this.nanos = jNanoTime;
                if (jNanoTime <= 0) {
                    return true;
                }
            }
            return this.thread == null;
        }

        @Override
        public boolean block() {
            while (!isReleasable()) {
                if (this.deadline == 0) {
                    LockSupport.park(this);
                } else {
                    LockSupport.parkNanos(this, this.nanos);
                }
            }
            return true;
        }

        @Override
        final boolean isLive() {
            return this.thread != null;
        }
    }

    private Object waitingGet(boolean z) {
        Object obj;
        int i = SPINS;
        boolean zTryPushStack = false;
        Signaller signaller = null;
        while (true) {
            obj = this.result;
            if (obj == null) {
                if (i > 0) {
                    if (ThreadLocalRandom.nextSecondarySeed() >= 0) {
                        i--;
                    }
                } else if (signaller == null) {
                    signaller = new Signaller(z, 0L, 0L);
                } else if (!zTryPushStack) {
                    zTryPushStack = tryPushStack(signaller);
                } else {
                    try {
                        ForkJoinPool.managedBlock(signaller);
                    } catch (InterruptedException e) {
                        signaller.interrupted = true;
                    }
                    if (signaller.interrupted && z) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        if (signaller != null) {
            signaller.thread = null;
            if (signaller.interrupted) {
                if (z) {
                    cleanStack();
                } else {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (obj != null) {
            postComplete();
        }
        return obj;
    }

    private Object timedGet(long j) throws TimeoutException {
        Object obj;
        if (Thread.interrupted()) {
            return null;
        }
        if (j > 0) {
            long jNanoTime = System.nanoTime() + j;
            if (jNanoTime == 0) {
                jNanoTime = 1;
            }
            boolean zTryPushStack = false;
            Signaller signaller = null;
            while (true) {
                obj = this.result;
                if (obj != null) {
                    break;
                }
                if (signaller == null) {
                    signaller = new Signaller(true, j, jNanoTime);
                } else if (!zTryPushStack) {
                    zTryPushStack = tryPushStack(signaller);
                } else {
                    if (signaller.nanos <= 0) {
                        break;
                    }
                    try {
                        ForkJoinPool.managedBlock(signaller);
                    } catch (InterruptedException e) {
                        signaller.interrupted = true;
                    }
                    if (signaller.interrupted) {
                        break;
                    }
                }
            }
            if (signaller != null) {
                signaller.thread = null;
            }
            if (obj != null) {
                postComplete();
            } else {
                cleanStack();
            }
            if (obj != null || (signaller != null && signaller.interrupted)) {
                return obj;
            }
        }
        throw new TimeoutException();
    }

    public CompletableFuture() {
    }

    CompletableFuture(Object obj) {
        this.result = obj;
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return asyncSupplyStage(ASYNC_POOL, supplier);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), supplier);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return asyncRunStage(ASYNC_POOL, runnable);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return asyncRunStage(screenExecutor(executor), runnable);
    }

    public static <U> CompletableFuture<U> completedFuture(U u) {
        if (u == null) {
            u = (U) NIL;
        }
        return new CompletableFuture<>(u);
    }

    @Override
    public boolean isDone() {
        return this.result != null;
    }

    @Override
    public T get() throws ExecutionException, InterruptedException {
        Object objWaitingGet = this.result;
        if (objWaitingGet == null) {
            objWaitingGet = waitingGet(true);
        }
        return (T) reportGet(objWaitingGet);
    }

    @Override
    public T get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        long nanos = timeUnit.toNanos(j);
        Object objTimedGet = this.result;
        if (objTimedGet == null) {
            objTimedGet = timedGet(nanos);
        }
        return (T) reportGet(objTimedGet);
    }

    public T join() {
        Object objWaitingGet = this.result;
        if (objWaitingGet == null) {
            objWaitingGet = waitingGet(false);
        }
        return (T) reportJoin(objWaitingGet);
    }

    public T getNow(T t) {
        Object obj = this.result;
        return obj == null ? t : (T) reportJoin(obj);
    }

    public boolean complete(T t) {
        boolean zCompleteValue = completeValue(t);
        postComplete();
        return zCompleteValue;
    }

    public boolean completeExceptionally(Throwable th) {
        if (th == null) {
            throw new NullPointerException();
        }
        boolean zInternalComplete = internalComplete(new AltResult(th));
        postComplete();
        return zInternalComplete;
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> function) {
        return (CompletableFuture<U>) uniApplyStage(null, function);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> function) {
        return (CompletableFuture<U>) uniApplyStage(defaultExecutor(), function);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> function, Executor executor) {
        return (CompletableFuture<U>) uniApplyStage(screenExecutor(executor), function);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> consumer) {
        return uniAcceptStage(null, consumer);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> consumer) {
        return uniAcceptStage(defaultExecutor(), consumer);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> consumer, Executor executor) {
        return uniAcceptStage(screenExecutor(executor), consumer);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable runnable) {
        return uniRunStage(null, runnable);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable runnable) {
        return uniRunStage(defaultExecutor(), runnable);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable runnable, Executor executor) {
        return uniRunStage(screenExecutor(executor), runnable);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return biApplyStage(null, completionStage, biFunction);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction) {
        return biApplyStage(defaultExecutor(), completionStage, biFunction);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> biFunction, Executor executor) {
        return biApplyStage(screenExecutor(executor), completionStage, biFunction);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return biAcceptStage(null, completionStage, biConsumer);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer) {
        return biAcceptStage(defaultExecutor(), completionStage, biConsumer);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> biConsumer, Executor executor) {
        return biAcceptStage(screenExecutor(executor), completionStage, biConsumer);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> completionStage, Runnable runnable) {
        return biRunStage(null, completionStage, runnable);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return biRunStage(defaultExecutor(), completionStage, runnable);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor) {
        return biRunStage(screenExecutor(executor), completionStage, runnable);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> completionStage, Function<? super T, U> function) {
        return (CompletableFuture<U>) orApplyStage(null, completionStage, function);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function) {
        return (CompletableFuture<U>) orApplyStage(defaultExecutor(), completionStage, function);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> completionStage, Function<? super T, U> function, Executor executor) {
        return (CompletableFuture<U>) orApplyStage(screenExecutor(executor), completionStage, function);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return orAcceptStage(null, completionStage, consumer);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer) {
        return orAcceptStage(defaultExecutor(), completionStage, consumer);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> consumer, Executor executor) {
        return orAcceptStage(screenExecutor(executor), completionStage, consumer);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> completionStage, Runnable runnable) {
        return orRunStage(null, completionStage, runnable);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable) {
        return orRunStage(defaultExecutor(), completionStage, runnable);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> completionStage, Runnable runnable, Executor executor) {
        return orRunStage(screenExecutor(executor), completionStage, runnable);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> function) {
        return (CompletableFuture<U>) uniComposeStage(null, function);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function) {
        return (CompletableFuture<U>) uniComposeStage(defaultExecutor(), function);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> function, Executor executor) {
        return (CompletableFuture<U>) uniComposeStage(screenExecutor(executor), function);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return uniWhenCompleteStage(null, biConsumer);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer) {
        return uniWhenCompleteStage(defaultExecutor(), biConsumer);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> biConsumer, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), biConsumer);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return (CompletableFuture<U>) uniHandleStage(null, biFunction);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction) {
        return (CompletableFuture<U>) uniHandleStage(defaultExecutor(), biFunction);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> biFunction, Executor executor) {
        return (CompletableFuture<U>) uniHandleStage(screenExecutor(executor), biFunction);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> function) {
        return uniExceptionallyStage(function);
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... completableFutureArr) {
        return andTree(completableFutureArr, 0, completableFutureArr.length - 1);
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... completableFutureArr) {
        return orTree(completableFutureArr, 0, completableFutureArr.length - 1);
    }

    @Override
    public boolean cancel(boolean z) {
        boolean z2 = this.result == null && internalComplete(new AltResult(new CancellationException()));
        postComplete();
        return z2 || isCancelled();
    }

    @Override
    public boolean isCancelled() {
        Object obj = this.result;
        return (obj instanceof AltResult) && (((AltResult) obj).ex instanceof CancellationException);
    }

    public boolean isCompletedExceptionally() {
        Object obj = this.result;
        return (obj instanceof AltResult) && obj != NIL;
    }

    public void obtrudeValue(T t) {
        if (t == null) {
            t = (T) NIL;
        }
        this.result = t;
        postComplete();
    }

    public void obtrudeException(Throwable th) {
        if (th == null) {
            throw new NullPointerException();
        }
        this.result = new AltResult(th);
        postComplete();
    }

    public int getNumberOfDependents() {
        int i = 0;
        for (Completion completion = this.stack; completion != null; completion = completion.next) {
            i++;
        }
        return i;
    }

    public String toString() {
        String str;
        Object obj = this.result;
        int i = 0;
        for (Completion completion = this.stack; completion != null; completion = completion.next) {
            i++;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (obj == null) {
            str = i == 0 ? "[Not completed]" : "[Not completed, " + i + " dependents]";
        } else if ((obj instanceof AltResult) && ((AltResult) obj).ex != null) {
            str = "[Completed exceptionally]";
        } else {
            str = "[Completed normally]";
        }
        sb.append(str);
        return sb.toString();
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<>();
    }

    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }

    public CompletableFuture<T> copy() {
        return uniCopyStage();
    }

    public CompletionStage<T> minimalCompletionStage() {
        return uniAsMinimalStage();
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        executor.execute(new AsyncSupply(this, supplier));
        return this;
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, defaultExecutor());
    }

    public CompletableFuture<T> orTimeout(long j, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new NullPointerException();
        }
        if (this.result == null) {
            whenComplete((BiConsumer) new Canceller(Delayer.delay(new Timeout(this), j, timeUnit)));
        }
        return this;
    }

    public CompletableFuture<T> completeOnTimeout(T t, long j, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new NullPointerException();
        }
        if (this.result == null) {
            whenComplete((BiConsumer) new Canceller(Delayer.delay(new DelayedCompleter(this, t), j, timeUnit)));
        }
        return this;
    }

    public static Executor delayedExecutor(long j, TimeUnit timeUnit, Executor executor) {
        if (timeUnit == null || executor == null) {
            throw new NullPointerException();
        }
        return new DelayedExecutor(j, timeUnit, executor);
    }

    public static Executor delayedExecutor(long j, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new NullPointerException();
        }
        return new DelayedExecutor(j, timeUnit, ASYNC_POOL);
    }

    public static <U> CompletionStage<U> completedStage(U u) {
        if (u == null) {
            u = (U) NIL;
        }
        return new MinimalStage(u);
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable th) {
        if (th == null) {
            throw new NullPointerException();
        }
        return new CompletableFuture<>(new AltResult(th));
    }

    public static <U> CompletionStage<U> failedStage(Throwable th) {
        if (th == null) {
            throw new NullPointerException();
        }
        return new MinimalStage(new AltResult(th));
    }

    static final class Delayer {
        static final ScheduledThreadPoolExecutor delayer;

        Delayer() {
        }

        static ScheduledFuture<?> delay(Runnable runnable, long j, TimeUnit timeUnit) {
            return delayer.schedule(runnable, j, timeUnit);
        }

        static final class DaemonThreadFactory implements ThreadFactory {
            DaemonThreadFactory() {
            }

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("CompletableFutureDelayScheduler");
                return thread;
            }
        }

        static {
            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory());
            delayer = scheduledThreadPoolExecutor;
            scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        }
    }

    static final class DelayedExecutor implements Executor {
        final long delay;
        final Executor executor;
        final TimeUnit unit;

        DelayedExecutor(long j, TimeUnit timeUnit, Executor executor) {
            this.delay = j;
            this.unit = timeUnit;
            this.executor = executor;
        }

        @Override
        public void execute(Runnable runnable) {
            Delayer.delay(new TaskSubmitter(this.executor, runnable), this.delay, this.unit);
        }
    }

    static final class TaskSubmitter implements Runnable {
        final Runnable action;
        final Executor executor;

        TaskSubmitter(Executor executor, Runnable runnable) {
            this.executor = executor;
            this.action = runnable;
        }

        @Override
        public void run() {
            this.executor.execute(this.action);
        }
    }

    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;

        Timeout(CompletableFuture<?> completableFuture) {
            this.f = completableFuture;
        }

        @Override
        public void run() {
            if (this.f != null && !this.f.isDone()) {
                this.f.completeExceptionally(new TimeoutException());
            }
        }
    }

    static final class DelayedCompleter<U> implements Runnable {
        final CompletableFuture<U> f;
        final U u;

        DelayedCompleter(CompletableFuture<U> completableFuture, U u) {
            this.f = completableFuture;
            this.u = u;
        }

        @Override
        public void run() {
            if (this.f != null) {
                this.f.complete(this.u);
            }
        }
    }

    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;

        Canceller(Future<?> future) {
            this.f = future;
        }

        @Override
        public void accept(Object obj, Throwable th) {
            if (th == null && this.f != null && !this.f.isDone()) {
                this.f.cancel(false);
            }
        }
    }

    static final class MinimalStage<T> extends CompletableFuture<T> {
        MinimalStage() {
        }

        MinimalStage(Object obj) {
            super(obj);
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new MinimalStage();
        }

        @Override
        public T get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T get(long j, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T getNow(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T join() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean complete(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean completeExceptionally(Throwable th) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancel(boolean z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtrudeValue(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtrudeException(Throwable th) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCompletedExceptionally() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getNumberOfDependents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<T> orTimeout(long j, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<T> completeOnTimeout(T t, long j, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }
    }
}
