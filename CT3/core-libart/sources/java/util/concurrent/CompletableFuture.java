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

    final boolean internalComplete(Object r) {
        return U.compareAndSwapObject(this, RESULT, (Object) null, r);
    }

    final boolean casStack(Completion cmp, Completion val) {
        return U.compareAndSwapObject(this, STACK, cmp, val);
    }

    final boolean tryPushStack(Completion c) {
        Completion h = this.stack;
        lazySetNext(c, h);
        return U.compareAndSwapObject(this, STACK, h, c);
    }

    final void pushStack(Completion c) {
        while (!tryPushStack(c)) {
        }
    }

    static final class AltResult {
        final Throwable ex;

        AltResult(Throwable x) {
            this.ex = x;
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
        return U.compareAndSwapObject(this, RESULT, (Object) null, NIL);
    }

    final Object encodeValue(T t) {
        return t == null ? NIL : t;
    }

    final boolean completeValue(T t) {
        return U.compareAndSwapObject(this, RESULT, (Object) null, t == null ? NIL : t);
    }

    static AltResult encodeThrowable(Throwable x) {
        if (!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        }
        return new AltResult(x);
    }

    final boolean completeThrowable(Throwable x) {
        return U.compareAndSwapObject(this, RESULT, (Object) null, encodeThrowable(x));
    }

    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        } else if ((r instanceof AltResult) && x == ((AltResult) r).ex) {
            return r;
        }
        return new AltResult(x);
    }

    final boolean completeThrowable(Throwable x, Object r) {
        return U.compareAndSwapObject(this, RESULT, (Object) null, encodeThrowable(x, r));
    }

    Object encodeOutcome(T t, Throwable x) {
        return x == null ? t == null ? NIL : t : encodeThrowable(x);
    }

    static Object encodeRelay(Object r) {
        Throwable x;
        if (!(r instanceof AltResult) || (x = ((AltResult) r).ex) == null || (x instanceof CompletionException)) {
            return r;
        }
        return new AltResult(new CompletionException(x));
    }

    final boolean completeRelay(Object r) {
        return U.compareAndSwapObject(this, RESULT, (Object) null, encodeRelay(r));
    }

    private static <T> T reportGet(Object obj) throws Throwable {
        Throwable cause;
        if (obj == 0) {
            throw new InterruptedException();
        }
        if (obj instanceof AltResult) {
            Throwable x = ((AltResult) obj).ex;
            if (x == null) {
                return null;
            }
            if (x instanceof CancellationException) {
                throw ((CancellationException) x);
            }
            if ((x instanceof CompletionException) && (cause = x.getCause()) != null) {
                x = cause;
            }
            throw new ExecutionException(x);
        }
        return obj;
    }

    private static <T> T reportJoin(Object obj) throws Throwable {
        if (obj instanceof AltResult) {
            Throwable x = ((AltResult) obj).ex;
            if (x == null) {
                return null;
            }
            if (x instanceof CancellationException) {
                throw ((CancellationException) x);
            }
            if (x instanceof CompletionException) {
                throw ((CompletionException) x);
            }
            throw new CompletionException(x);
        }
        return obj;
    }

    static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {
        }

        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    static Executor screenExecutor(Executor e) {
        if (!USE_COMMON_POOL && e == ForkJoinPool.commonPool()) {
            return ASYNC_POOL;
        }
        if (e == null) {
            throw new NullPointerException();
        }
        return e;
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
        public final void setRawResult(Void v) {
        }
    }

    static void lazySetNext(Completion c, Completion next) {
        U.putOrderedObject(c, NEXT, next);
    }

    final void postComplete() {
        CompletableFuture f = this;
        while (true) {
            Completion h = f.stack;
            if (h == null) {
                if (f == this) {
                    return;
                }
                f = this;
                h = this.stack;
                if (h == null) {
                    return;
                }
            }
            Completion t = h.next;
            if (f.casStack(h, t)) {
                if (t != null) {
                    if (f != this) {
                        pushStack(h);
                    } else {
                        h.next = null;
                    }
                }
                CompletableFuture<?> d = h.tryFire(-1);
                f = d == null ? this : d;
            }
        }
    }

    final void cleanStack() {
        Completion p = null;
        Completion q = this.stack;
        while (q != null) {
            Completion s = q.next;
            if (q.isLive()) {
                p = q;
                q = s;
            } else if (p == null) {
                casStack(q, s);
                q = this.stack;
            } else {
                p.next = s;
                if (p.isLive()) {
                    q = s;
                } else {
                    p = null;
                    q = this.stack;
                }
            }
        }
    }

    static abstract class UniCompletion<T, V> extends Completion {
        CompletableFuture<V> dep;
        Executor executor;
        CompletableFuture<T> src;

        UniCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.executor = executor;
            this.dep = dep;
            this.src = src;
        }

        final boolean claim() {
            Executor e = this.executor;
            if (compareAndSetForkJoinTaskTag((short) 0, (short) 1)) {
                if (e == null) {
                    return true;
                }
                this.executor = null;
                e.execute(this);
            }
            return false;
        }

        @Override
        final boolean isLive() {
            return this.dep != null;
        }
    }

    final void push(UniCompletion<?, ?> c) {
        if (c == null) {
            return;
        }
        while (this.result == null && !tryPushStack(c)) {
            lazySetNext(c, null);
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (a != null && a.stack != null) {
            if (mode < 0 || a.result == null) {
                a.cleanStack();
            } else {
                a.postComplete();
            }
        }
        if (this.result != null && this.stack != null) {
            if (mode < 0) {
                return this;
            }
            postComplete();
        }
        return null;
    }

    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn;

        UniApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                if (d.uniApply(a, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    final <S> boolean uniApply(CompletableFuture<S> a, Function<? super S, ? extends T> f, UniApply<S, T> c) {
        Object r;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            completeValue(f.apply(r));
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

        UniAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <S> boolean uniAccept(CompletableFuture<S> a, Consumer<? super S> f, UniAccept<S> c) {
        Object r;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            f.accept(r);
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> uniAcceptStage(Executor e, Consumer<? super T> f) {
        if (f == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (e != null || !completableFutureNewIncompleteFuture.uniAccept(this, f, null)) {
            UniAccept<T> c = new UniAccept<>(e, completableFutureNewIncompleteFuture, this, f);
            push(c);
            c.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable fn;

        UniRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Runnable fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniRun(CompletableFuture<?> a, Runnable f, UniRun<?> c) {
        Object r;
        Throwable x;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if ((r instanceof AltResult) && (x = ((AltResult) r).ex) != null) {
                completeThrowable(x, r);
                return true;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            f.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        if (f == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (e != null || !completableFutureNewIncompleteFuture.uniRun(this, f, null)) {
            UniRun<T> c = new UniRun<>(e, completableFutureNewIncompleteFuture, this, f);
            push(c);
            c.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;

        UniWhenComplete(Executor executor, CompletableFuture<T> dep, CompletableFuture<T> src, BiConsumer<? super T, ? super Throwable> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniWhenComplete.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniWhenComplete(CompletableFuture<T> a, BiConsumer<? super T, ? super Throwable> f, UniWhenComplete<T> c) {
        Object r;
        Object obj;
        Throwable x = null;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    if (x == null) {
                        x = ex;
                    } else if (x != ex) {
                        x.addSuppressed(ex);
                    }
                }
            }
            if (r instanceof AltResult) {
                x = ((AltResult) r).ex;
                obj = null;
            } else {
                obj = r;
            }
            f.accept(obj, x);
            if (x == null) {
                internalComplete(r);
                return true;
            }
            completeThrowable(x, r);
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

        UniHandle(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, BiFunction<? super T, Throwable, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                if (d.uniHandle(a, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    final <S> boolean uniHandle(CompletableFuture<S> a, BiFunction<? super S, Throwable, ? extends T> f, UniHandle<S, T> c) {
        Object r;
        Throwable x;
        Object obj;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            if (r instanceof AltResult) {
                x = ((AltResult) r).ex;
                obj = null;
            } else {
                x = null;
                obj = r;
            }
            completeValue(f.apply(obj, x));
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

        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src, Function<? super Throwable, ? extends T> fn) {
            super(null, dep, src);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r5) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniExceptionally.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniExceptionally(CompletableFuture<T> a, Function<? super Throwable, ? extends T> f, UniExceptionally<T> c) {
        Object r;
        Throwable x;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            try {
                if ((r instanceof AltResult) && (x = ((AltResult) r).ex) != null) {
                    if (c != null && !c.claim()) {
                        return false;
                    }
                    completeValue(f.apply(x));
                    return true;
                }
                internalComplete(r);
                return true;
            } catch (Throwable ex) {
                completeThrowable(ex);
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
        UniRelay(CompletableFuture<T> dep, CompletableFuture<T> src) {
            super(null, dep, src);
        }

        @Override
        final java.util.concurrent.CompletableFuture<T> tryFire(int r5) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.UniRelay.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean uniRelay(CompletableFuture<T> a) {
        Object r;
        if (a == null || (r = a.result) == null) {
            return false;
        }
        if (this.result == null) {
            completeRelay(r);
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
        Object r = this.result;
        if (r != null) {
            return new MinimalStage<>(encodeRelay(r));
        }
        MinimalStage<T> d = new MinimalStage<>();
        UniRelay<T> c = new UniRelay<>(d, this);
        push(c);
        c.tryFire(0);
        return d;
    }

    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> fn;

        UniCompose(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends CompletionStage<V>> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        @Override
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                if (d.uniCompose(a, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    final <S> boolean uniCompose(CompletableFuture<S> a, Function<? super S, ? extends CompletionStage<T>> f, UniCompose<S, T> c) {
        Object r;
        if (a == null || (r = a.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            CompletableFuture<T> g = f.apply(r).toCompletableFuture();
            if (g.result == null || !uniRelay(g)) {
                UniRelay<T> copy = new UniRelay<>(this, g);
                g.push(copy);
                copy.tryFire(0);
                return this.result != null;
            }
            return true;
        }
        return true;
    }

    private <V> CompletableFuture<V> uniComposeStage(Executor executor, Function<? super T, ? extends CompletionStage<V>> function) {
        Object obj;
        if (function == null) {
            throw new NullPointerException();
        }
        CompletableFuture<V> completableFuture = (CompletableFuture<V>) newIncompleteFuture();
        if (executor == null && (obj = this.result) != null) {
            if (obj instanceof AltResult) {
                Throwable th = ((AltResult) obj).ex;
                if (th != null) {
                    completableFuture.result = encodeThrowable(th, obj);
                    return completableFuture;
                }
                obj = null;
            }
            try {
                CompletableFuture<V> completableFuture2 = function.apply(obj).toCompletableFuture();
                Object obj2 = completableFuture2.result;
                if (obj2 != null) {
                    completableFuture.completeRelay(obj2);
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

        BiCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(executor, dep, src);
            this.snd = snd;
        }
    }

    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;

        CoCompletion(BiCompletion<?, ?, ?> base) {
            this.base = base;
        }

        @Override
        final CompletableFuture<?> tryFire(int mode) {
            CompletableFuture<?> d;
            BiCompletion<?, ?, ?> c = this.base;
            if (c == null || (d = c.tryFire(mode)) == null) {
                return null;
            }
            this.base = null;
            return d;
        }

        @Override
        final boolean isLive() {
            BiCompletion<?, ?, ?> c = this.base;
            return (c == null || c.dep == null) ? false : true;
        }
    }

    final void bipush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        Object r;
        if (c == null) {
            return;
        }
        while (true) {
            r = this.result;
            if (r != null || tryPushStack(c)) {
                break;
            } else {
                lazySetNext(c, null);
            }
        }
        if (b == null || b == this || b.result != null) {
            return;
        }
        Completion q = r != null ? c : new CoCompletion(c);
        while (b.result == null && !b.tryPushStack(q)) {
            lazySetNext(q, null);
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> a, CompletableFuture<?> b, int mode) {
        if (b != null && b.stack != null) {
            if (mode < 0 || b.result == null) {
                b.cleanStack();
            } else {
                b.postComplete();
            }
        }
        return postFire(a, mode);
    }

    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> fn;

        BiApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiFunction<? super T, ? super U, ? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                CompletableFuture<U> b = this.snd;
                if (d.biApply(a, b, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    final <R, S> boolean biApply(CompletableFuture<R> a, CompletableFuture<S> b, BiFunction<? super R, ? super S, ? extends T> f, BiApply<R, S, T> c) {
        Object r;
        Object s;
        if (a == null || (r = a.result) == null || b == null || (s = b.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                Throwable x2 = ((AltResult) s).ex;
                if (x2 != null) {
                    completeThrowable(x2, s);
                    return true;
                }
                s = null;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            completeValue(f.apply(r, s));
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

        BiAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiConsumer<? super T, ? super U> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <R, S> boolean biAccept(CompletableFuture<R> a, CompletableFuture<S> b, BiConsumer<? super R, ? super S> f, BiAccept<R, S> c) {
        Object r;
        Object s;
        if (a == null || (r = a.result) == null || b == null || (s = b.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                Throwable x2 = ((AltResult) s).ex;
                if (x2 != null) {
                    completeThrowable(x2, s);
                    return true;
                }
                s = null;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            f.accept(r, s);
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

        BiRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean biRun(CompletableFuture<?> a, CompletableFuture<?> b, Runnable f, BiRun<?, ?> c) {
        Object r;
        Object s;
        Throwable x;
        Throwable x2;
        if (a == null || (r = a.result) == null || b == null || (s = b.result) == null || f == null) {
            return false;
        }
        if (this.result == null) {
            if ((r instanceof AltResult) && (x2 = ((AltResult) r).ex) != null) {
                completeThrowable(x2, r);
                return true;
            }
            if ((s instanceof AltResult) && (x = ((AltResult) s).ex) != null) {
                completeThrowable(x, s);
                return true;
            }
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            f.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> o, Runnable f) {
        CompletableFuture<?> b;
        if (f == null || (b = o.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (e != null || !completableFutureNewIncompleteFuture.biRun(this, b, f, null)) {
            BiRun<T, ?> c = new BiRun<>(e, completableFutureNewIncompleteFuture, this, b, f);
            bipush(b, c);
            c.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> {
        BiRelay(CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r6) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.BiRelay.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    boolean biRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        Object r;
        Object s;
        Throwable x;
        Throwable x2;
        if (a == null || (r = a.result) == null || b == null || (s = b.result) == null) {
            return false;
        }
        if (this.result == null) {
            if ((r instanceof AltResult) && (x2 = ((AltResult) r).ex) != null) {
                completeThrowable(x2, r);
                return true;
            }
            if ((s instanceof AltResult) && (x = ((AltResult) s).ex) != null) {
                completeThrowable(x, s);
                return true;
            }
            completeNull();
            return true;
        }
        return true;
    }

    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<?> b;
        CompletableFuture<Void> d = new CompletableFuture<>();
        if (lo > hi) {
            d.result = NIL;
        } else {
            int mid = (lo + hi) >>> 1;
            CompletableFuture<?> a = lo == mid ? cfs[lo] : andTree(cfs, lo, mid);
            if (a != null) {
                if (lo == hi) {
                    b = a;
                } else {
                    b = hi == mid + 1 ? cfs[hi] : andTree(cfs, mid + 1, hi);
                }
                if (b != null) {
                    if (!d.biRelay(a, b)) {
                        BiRelay<?, ?> c = new BiRelay<>(d, a, b);
                        a.bipush(b, c);
                        c.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return d;
    }

    final void orpush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if (c == null) {
            return;
        }
        while (true) {
            if ((b != null && b.result != null) || this.result != null) {
                return;
            }
            if (tryPushStack(c)) {
                if (b == null || b == this || b.result != null) {
                    return;
                }
                Completion q = new CoCompletion(c);
                while (this.result == null && b.result == null && !b.tryPushStack(q)) {
                    lazySetNext(q, null);
                }
                return;
            }
            lazySetNext(c, null);
        }
    }

    static final class OrApply<T, U extends T, V> extends BiCompletion<T, U, V> {
        Function<? super T, ? extends V> fn;

        OrApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Function<? super T, ? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                CompletableFuture<U> b = this.snd;
                if (d.orApply(a, b, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    final <R, S extends R> boolean orApply(CompletableFuture<R> a, CompletableFuture<S> b, Function<? super R, ? extends T> f, OrApply<R, S, T> c) {
        Object r;
        if (a == null || b == null || (((r = a.result) == null && (r = b.result) == null) || f == null)) {
            return false;
        }
        if (this.result == null) {
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            completeValue(f.apply(r));
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

        OrAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Consumer<? super T> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.OrAccept.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final <R, S extends R> boolean orAccept(CompletableFuture<R> a, CompletableFuture<S> b, Consumer<? super R> f, OrAccept<R, S> c) {
        Object r;
        if (a == null || b == null || (((r = a.result) == null && (r = b.result) == null) || f == null)) {
            return false;
        }
        if (this.result == null) {
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            if (r instanceof AltResult) {
                Throwable x = ((AltResult) r).ex;
                if (x != null) {
                    completeThrowable(x, r);
                    return true;
                }
                r = null;
            }
            f.accept(r);
            completeNull();
            return true;
        }
        return true;
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(Executor e, CompletionStage<U> o, Consumer<? super T> f) {
        CompletableFuture<?> completableFuture;
        if (f == null || (completableFuture = o.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (e != null || !completableFutureNewIncompleteFuture.orAccept(this, completableFuture, f, null)) {
            OrAccept<T, U> c = new OrAccept<>(e, completableFutureNewIncompleteFuture, this, completableFuture, f);
            orpush(completableFuture, c);
            c.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class OrRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;

        OrRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        @Override
        final java.util.concurrent.CompletableFuture<java.lang.Void> tryFire(int r7) {
            throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.CompletableFuture.OrRun.tryFire(int):java.util.concurrent.CompletableFuture");
        }
    }

    final boolean orRun(CompletableFuture<?> a, CompletableFuture<?> b, Runnable f, OrRun<?, ?> c) {
        Object r;
        Throwable x;
        if (a == null || b == null || (((r = a.result) == null && (r = b.result) == null) || f == null)) {
            return false;
        }
        if (this.result == null) {
            if (c != null) {
                try {
                    if (!c.claim()) {
                        return false;
                    }
                } catch (Throwable ex) {
                    completeThrowable(ex);
                    return true;
                }
            }
            if ((r instanceof AltResult) && (x = ((AltResult) r).ex) != null) {
                completeThrowable(x, r);
                return true;
            }
            f.run();
            completeNull();
            return true;
        }
        return true;
    }

    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> o, Runnable f) {
        CompletableFuture<?> b;
        if (f == null || (b = o.toCompletableFuture()) == null) {
            throw new NullPointerException();
        }
        CompletableFuture completableFutureNewIncompleteFuture = newIncompleteFuture();
        if (e != null || !completableFutureNewIncompleteFuture.orRun(this, b, f, null)) {
            OrRun<T, ?> c = new OrRun<>(e, completableFutureNewIncompleteFuture, this, b, f);
            orpush(b, c);
            c.tryFire(0);
        }
        return completableFutureNewIncompleteFuture;
    }

    static final class OrRelay<T, U> extends BiCompletion<T, U, Object> {
        OrRelay(CompletableFuture<Object> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        @Override
        final CompletableFuture<Object> tryFire(int mode) {
            CompletableFuture<Object> d = this.dep;
            if (d != null) {
                CompletableFuture<T> a = this.src;
                CompletableFuture<U> b = this.snd;
                if (d.orRelay(a, b)) {
                    this.src = null;
                    this.snd = null;
                    this.dep = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    final boolean orRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        if (a != null && b != null) {
            Object r = a.result;
            if (r == null && (r = b.result) == null) {
                return false;
            }
            if (this.result == null) {
                completeRelay(r);
                return true;
            }
            return true;
        }
        return false;
    }

    static CompletableFuture<Object> orTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<?> b;
        CompletableFuture<Object> d = new CompletableFuture<>();
        if (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            CompletableFuture<?> a = lo == mid ? cfs[lo] : orTree(cfs, lo, mid);
            if (a != null) {
                if (lo == hi) {
                    b = a;
                } else {
                    b = hi == mid + 1 ? cfs[hi] : orTree(cfs, mid + 1, hi);
                }
                if (b != null) {
                    if (!d.orRelay(a, b)) {
                        OrRelay<?, ?> c = new OrRelay<>(d, a, b);
                        a.orpush(b, c);
                        c.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return d;
    }

    static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep;
        Supplier<? extends T> fn;

        AsyncSupply(CompletableFuture<T> dep, Supplier<? extends T> fn) {
            this.dep = dep;
            this.fn = fn;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void v) {
        }

        @Override
        public final boolean exec() {
            run();
            return true;
        }

        @Override
        public void run() {
            Supplier<? extends T> f;
            CompletableFuture<T> d = this.dep;
            if (d == null || (f = this.fn) == null) {
                return;
            }
            this.dep = null;
            this.fn = null;
            if (d.result == null) {
                try {
                    d.completeValue(f.get());
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            d.postComplete();
        }
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor e, Supplier<U> f) {
        if (f == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> d = new CompletableFuture<>();
        e.execute(new AsyncSupply(d, f));
        return d;
    }

    static final class AsyncRun extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep;
        Runnable fn;

        AsyncRun(CompletableFuture<Void> dep, Runnable fn) {
            this.dep = dep;
            this.fn = fn;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void v) {
        }

        @Override
        public final boolean exec() {
            run();
            return true;
        }

        @Override
        public void run() {
            Runnable f;
            CompletableFuture<Void> d = this.dep;
            if (d == null || (f = this.fn) == null) {
                return;
            }
            this.dep = null;
            this.fn = null;
            if (d.result == null) {
                try {
                    f.run();
                    d.completeNull();
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            d.postComplete();
        }
    }

    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        if (f == null) {
            throw new NullPointerException();
        }
        CompletableFuture<Void> d = new CompletableFuture<>();
        e.execute(new AsyncRun(d, f));
        return d;
    }

    static final class Signaller extends Completion implements ForkJoinPool.ManagedBlocker {
        final long deadline;
        boolean interrupted;
        final boolean interruptible;
        long nanos;
        volatile Thread thread = Thread.currentThread();

        Signaller(boolean interruptible, long nanos, long deadline) {
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.deadline = deadline;
        }

        @Override
        final CompletableFuture<?> tryFire(int ignore) {
            Thread w = this.thread;
            if (w != null) {
                this.thread = null;
                LockSupport.unpark(w);
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

    private Object waitingGet(boolean interruptible) {
        Object r;
        Signaller q = null;
        boolean queued = false;
        int spins = SPINS;
        while (true) {
            r = this.result;
            if (r == null) {
                if (spins > 0) {
                    if (ThreadLocalRandom.nextSecondarySeed() >= 0) {
                        spins--;
                    }
                } else if (q == null) {
                    q = new Signaller(interruptible, 0L, 0L);
                } else if (!queued) {
                    queued = tryPushStack(q);
                } else {
                    try {
                        ForkJoinPool.managedBlock(q);
                    } catch (InterruptedException e) {
                        q.interrupted = true;
                    }
                    if (q.interrupted && interruptible) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        if (q != null) {
            q.thread = null;
            if (q.interrupted) {
                if (interruptible) {
                    cleanStack();
                } else {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (r != null) {
            postComplete();
        }
        return r;
    }

    private Object timedGet(long nanos) throws TimeoutException {
        Object r;
        if (Thread.interrupted()) {
            return null;
        }
        if (nanos > 0) {
            long d = System.nanoTime() + nanos;
            long deadline = d == 0 ? 1L : d;
            Signaller q = null;
            boolean zTryPushStack = false;
            while (true) {
                r = this.result;
                if (r != null) {
                    break;
                }
                if (q == null) {
                    q = new Signaller(true, nanos, deadline);
                } else if (!zTryPushStack) {
                    zTryPushStack = tryPushStack(q);
                } else {
                    if (q.nanos <= 0) {
                        break;
                    }
                    try {
                        ForkJoinPool.managedBlock(q);
                    } catch (InterruptedException e) {
                        q.interrupted = true;
                    }
                    if (q.interrupted) {
                        break;
                    }
                }
            }
            if (q != null) {
                q.thread = null;
            }
            if (r != null) {
                postComplete();
            } else {
                cleanStack();
            }
            if (r != null || (q != null && q.interrupted)) {
                return r;
            }
        }
        throw new TimeoutException();
    }

    public CompletableFuture() {
    }

    CompletableFuture(Object r) {
        this.result = r;
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

    public boolean complete(T value) {
        boolean triggered = completeValue(value);
        postComplete();
        return triggered;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        boolean triggered = internalComplete(new AltResult(ex));
        postComplete();
        return triggered;
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
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return uniRunStage(screenExecutor(executor), action);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(null, completionStage, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(defaultExecutor(), completionStage, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> completionStage, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return biApplyStage(screenExecutor(executor), completionStage, fn);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, completionStage, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(defaultExecutor(), completionStage, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> completionStage, BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), completionStage, action);
    }

    @Override
    public CompletionStage runAfterBoth(CompletionStage other, Runnable action) {
        return runAfterBoth((CompletionStage<?>) other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return biRunStage(null, other, action);
    }

    @Override
    public CompletionStage runAfterBothAsync(CompletionStage other, Runnable action) {
        return runAfterBothAsync((CompletionStage<?>) other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return biRunStage(defaultExecutor(), other, action);
    }

    @Override
    public CompletionStage runAfterBothAsync(CompletionStage other, Runnable action, Executor executor) {
        return runAfterBothAsync((CompletionStage<?>) other, action, executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return biRunStage(screenExecutor(executor), other, action);
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
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> completionStage, Consumer<? super T> action) {
        return orAcceptStage(null, completionStage, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> action) {
        return orAcceptStage(defaultExecutor(), completionStage, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> completionStage, Consumer<? super T> action, Executor executor) {
        return orAcceptStage(screenExecutor(executor), completionStage, action);
    }

    @Override
    public CompletionStage runAfterEither(CompletionStage other, Runnable action) {
        return runAfterEither((CompletionStage<?>) other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return orRunStage(null, other, action);
    }

    @Override
    public CompletionStage runAfterEitherAsync(CompletionStage other, Runnable action) {
        return runAfterEitherAsync((CompletionStage<?>) other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return orRunStage(defaultExecutor(), other, action);
    }

    @Override
    public CompletionStage runAfterEitherAsync(CompletionStage other, Runnable action, Executor executor) {
        return runAfterEitherAsync((CompletionStage<?>) other, action, executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return orRunStage(screenExecutor(executor), other, action);
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
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(defaultExecutor(), action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action);
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
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(fn);
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        return orTree(cfs, 0, cfs.length - 1);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean zInternalComplete;
        if (this.result != null) {
            zInternalComplete = false;
        } else {
            zInternalComplete = internalComplete(new AltResult(new CancellationException()));
        }
        postComplete();
        if (zInternalComplete) {
            return true;
        }
        return isCancelled();
    }

    @Override
    public boolean isCancelled() {
        Object r = this.result;
        if (r instanceof AltResult) {
            return ((AltResult) r).ex instanceof CancellationException;
        }
        return false;
    }

    public boolean isCompletedExceptionally() {
        Object r = this.result;
        return (r instanceof AltResult) && r != NIL;
    }

    public void obtrudeValue(T t) {
        if (t == null) {
            t = (T) NIL;
        }
        this.result = t;
        postComplete();
    }

    public void obtrudeException(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        this.result = new AltResult(ex);
        postComplete();
    }

    public int getNumberOfDependents() {
        int count = 0;
        for (Completion p = this.stack; p != null; p = p.next) {
            count++;
        }
        return count;
    }

    public String toString() {
        String str;
        Object r = this.result;
        int count = 0;
        for (Completion p = this.stack; p != null; p = p.next) {
            count++;
        }
        StringBuilder sbAppend = new StringBuilder().append(super.toString());
        if (r == null) {
            if (count == 0) {
                str = "[Not completed]";
            } else {
                str = "[Not completed, " + count + " dependents]";
            }
        } else if ((r instanceof AltResult) && ((AltResult) r).ex != null) {
            str = "[Completed exceptionally]";
        } else {
            str = "[Completed normally]";
        }
        return sbAppend.append(str).toString();
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

    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (this.result == null) {
            whenComplete((BiConsumer) new Canceller(Delayer.delay(new Timeout(this), timeout, unit)));
        }
        return this;
    }

    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        if (this.result == null) {
            whenComplete((BiConsumer) new Canceller(Delayer.delay(new DelayedCompleter(this, value), timeout, unit)));
        }
        return this;
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        if (unit == null || executor == null) {
            throw new NullPointerException();
        }
        return new DelayedExecutor(delay, unit, executor);
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }
        return new DelayedExecutor(delay, unit, ASYNC_POOL);
    }

    public static <U> CompletionStage<U> completedStage(U u) {
        if (u == null) {
            u = (U) NIL;
        }
        return new MinimalStage(u);
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        return new CompletableFuture<>(new AltResult(ex));
    }

    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        return new MinimalStage(new AltResult(ex));
    }

    static final class Delayer {
        static final ScheduledThreadPoolExecutor delayer;

        Delayer() {
        }

        static ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
            return delayer.schedule(command, delay, unit);
        }

        static final class DaemonThreadFactory implements ThreadFactory {
            DaemonThreadFactory() {
            }

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelayScheduler");
                return t;
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

        DelayedExecutor(long delay, TimeUnit unit, Executor executor) {
            this.delay = delay;
            this.unit = unit;
            this.executor = executor;
        }

        @Override
        public void execute(Runnable r) {
            Delayer.delay(new TaskSubmitter(this.executor, r), this.delay, this.unit);
        }
    }

    static final class TaskSubmitter implements Runnable {
        final Runnable action;
        final Executor executor;

        TaskSubmitter(Executor executor, Runnable action) {
            this.executor = executor;
            this.action = action;
        }

        @Override
        public void run() {
            this.executor.execute(this.action);
        }
    }

    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;

        Timeout(CompletableFuture<?> f) {
            this.f = f;
        }

        @Override
        public void run() {
            if (this.f == null || this.f.isDone()) {
                return;
            }
            this.f.completeExceptionally(new TimeoutException());
        }
    }

    static final class DelayedCompleter<U> implements Runnable {
        final CompletableFuture<U> f;
        final U u;

        DelayedCompleter(CompletableFuture<U> f, U u) {
            this.f = f;
            this.u = u;
        }

        @Override
        public void run() {
            if (this.f == null) {
                return;
            }
            this.f.complete(this.u);
        }
    }

    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;

        Canceller(Future<?> f) {
            this.f = f;
        }

        @Override
        public void accept(Object ignore, Throwable ex) {
            if (ex != null || this.f == null || this.f.isDone()) {
                return;
            }
            this.f.cancel(false);
        }
    }

    static final class MinimalStage<T> extends CompletableFuture<T> {
        MinimalStage() {
        }

        MinimalStage(Object r) {
            super(r);
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
        public T get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T getNow(T valueIfAbsent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T join() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean complete(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtrudeValue(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtrudeException(Throwable ex) {
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
        public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }
}
