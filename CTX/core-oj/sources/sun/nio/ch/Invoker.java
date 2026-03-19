package sun.nio.ch;

import java.nio.channels.AsynchronousChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.AccessController;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import sun.security.action.GetIntegerAction;

class Invoker {
    static final boolean $assertionsDisabled = false;
    private static final int maxHandlerInvokeCount = ((Integer) AccessController.doPrivileged(new GetIntegerAction("sun.nio.ch.maxCompletionHandlersOnStack", 16))).intValue();
    private static final ThreadLocal<GroupAndInvokeCount> myGroupAndInvokeCount = new ThreadLocal<GroupAndInvokeCount>() {
        @Override
        protected GroupAndInvokeCount initialValue() {
            return null;
        }
    };

    private Invoker() {
    }

    static class GroupAndInvokeCount {
        private final AsynchronousChannelGroupImpl group;
        private int handlerInvokeCount;

        GroupAndInvokeCount(AsynchronousChannelGroupImpl asynchronousChannelGroupImpl) {
            this.group = asynchronousChannelGroupImpl;
        }

        AsynchronousChannelGroupImpl group() {
            return this.group;
        }

        int invokeCount() {
            return this.handlerInvokeCount;
        }

        void setInvokeCount(int i) {
            this.handlerInvokeCount = i;
        }

        void resetInvokeCount() {
            this.handlerInvokeCount = 0;
        }

        void incrementInvokeCount() {
            this.handlerInvokeCount++;
        }
    }

    static void bindToGroup(AsynchronousChannelGroupImpl asynchronousChannelGroupImpl) {
        myGroupAndInvokeCount.set(new GroupAndInvokeCount(asynchronousChannelGroupImpl));
    }

    static GroupAndInvokeCount getGroupAndInvokeCount() {
        return myGroupAndInvokeCount.get();
    }

    static boolean isBoundToAnyGroup() {
        return myGroupAndInvokeCount.get() != null;
    }

    static boolean mayInvokeDirect(GroupAndInvokeCount groupAndInvokeCount, AsynchronousChannelGroupImpl asynchronousChannelGroupImpl) {
        if (groupAndInvokeCount != null && groupAndInvokeCount.group() == asynchronousChannelGroupImpl && groupAndInvokeCount.invokeCount() < maxHandlerInvokeCount) {
            return true;
        }
        return false;
    }

    static <V, A> void invokeUnchecked(CompletionHandler<V, ? super A> completionHandler, A a, V v, Throwable th) {
        if (th == null) {
            completionHandler.completed(v, a);
        } else {
            completionHandler.failed(th, a);
        }
        Thread.interrupted();
    }

    static <V, A> void invokeDirect(GroupAndInvokeCount groupAndInvokeCount, CompletionHandler<V, ? super A> completionHandler, A a, V v, Throwable th) {
        groupAndInvokeCount.incrementInvokeCount();
        invokeUnchecked(completionHandler, a, v, th);
    }

    static <V, A> void invoke(AsynchronousChannel asynchronousChannel, CompletionHandler<V, ? super A> completionHandler, A a, V v, Throwable th) {
        boolean z;
        GroupAndInvokeCount groupAndInvokeCount = myGroupAndInvokeCount.get();
        boolean z2 = false;
        if (groupAndInvokeCount != null) {
            z = groupAndInvokeCount.group() == ((Groupable) asynchronousChannel).group();
            if (z && groupAndInvokeCount.invokeCount() < maxHandlerInvokeCount) {
                z2 = true;
            }
        } else {
            z = false;
        }
        if (z2) {
            invokeDirect(groupAndInvokeCount, completionHandler, a, v, th);
            return;
        }
        try {
            invokeIndirectly(asynchronousChannel, completionHandler, a, v, th);
        } catch (RejectedExecutionException e) {
            if (z) {
                invokeDirect(groupAndInvokeCount, completionHandler, a, v, th);
                return;
            }
            throw new ShutdownChannelGroupException();
        }
    }

    static <V, A> void invokeIndirectly(AsynchronousChannel asynchronousChannel, final CompletionHandler<V, ? super A> completionHandler, final A a, final V v, final Throwable th) {
        try {
            ((Groupable) asynchronousChannel).group().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    GroupAndInvokeCount groupAndInvokeCount = (GroupAndInvokeCount) Invoker.myGroupAndInvokeCount.get();
                    if (groupAndInvokeCount != null) {
                        groupAndInvokeCount.setInvokeCount(1);
                    }
                    Invoker.invokeUnchecked(completionHandler, a, v, th);
                }
            });
        } catch (RejectedExecutionException e) {
            throw new ShutdownChannelGroupException();
        }
    }

    static <V, A> void invokeIndirectly(final CompletionHandler<V, ? super A> completionHandler, final A a, final V v, final Throwable th, Executor executor) {
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Invoker.invokeUnchecked(completionHandler, a, v, th);
                }
            });
        } catch (RejectedExecutionException e) {
            throw new ShutdownChannelGroupException();
        }
    }

    static void invokeOnThreadInThreadPool(Groupable groupable, Runnable runnable) {
        GroupAndInvokeCount groupAndInvokeCount = myGroupAndInvokeCount.get();
        AsynchronousChannelGroupImpl asynchronousChannelGroupImplGroup = groupable.group();
        boolean z = false;
        if (groupAndInvokeCount != null && groupAndInvokeCount.group == asynchronousChannelGroupImplGroup) {
            z = true;
        }
        try {
            if (z) {
                runnable.run();
            } else {
                asynchronousChannelGroupImplGroup.executeOnPooledThread(runnable);
            }
        } catch (RejectedExecutionException e) {
            throw new ShutdownChannelGroupException();
        }
    }

    static <V, A> void invokeUnchecked(PendingFuture<V, A> pendingFuture) {
        CompletionHandler<V, ? super A> completionHandlerHandler = pendingFuture.handler();
        if (completionHandlerHandler != null) {
            invokeUnchecked(completionHandlerHandler, pendingFuture.attachment(), pendingFuture.value(), pendingFuture.exception());
        }
    }

    static <V, A> void invoke(PendingFuture<V, A> pendingFuture) {
        CompletionHandler<V, ? super A> completionHandlerHandler = pendingFuture.handler();
        if (completionHandlerHandler != null) {
            invoke(pendingFuture.channel(), completionHandlerHandler, pendingFuture.attachment(), pendingFuture.value(), pendingFuture.exception());
        }
    }

    static <V, A> void invokeIndirectly(PendingFuture<V, A> pendingFuture) {
        CompletionHandler<V, ? super A> completionHandlerHandler = pendingFuture.handler();
        if (completionHandlerHandler != null) {
            invokeIndirectly(pendingFuture.channel(), completionHandlerHandler, pendingFuture.attachment(), pendingFuture.value(), pendingFuture.exception());
        }
    }
}
