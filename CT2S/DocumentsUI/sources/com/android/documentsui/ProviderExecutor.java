package com.android.documentsui;

import android.os.AsyncTask;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class ProviderExecutor extends Thread implements Executor {

    @GuardedBy("sExecutors")
    private static HashMap<String, ProviderExecutor> sExecutors = Maps.newHashMap();
    private final LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();
    private final ArrayList<WeakReference<Preemptable>> mPreemptable = Lists.newArrayList();
    private Executor mNonPreemptingExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            Preconditions.checkNotNull(command);
            ProviderExecutor.this.mQueue.add(command);
        }
    };

    public interface Preemptable {
        void preempt();
    }

    public static ProviderExecutor forAuthority(String authority) {
        ProviderExecutor executor;
        synchronized (sExecutors) {
            executor = sExecutors.get(authority);
            if (executor == null) {
                executor = new ProviderExecutor();
                executor.setName("ProviderExecutor: " + authority);
                executor.start();
                sExecutors.put(authority, executor);
            }
        }
        return executor;
    }

    private void preempt() {
        synchronized (this.mPreemptable) {
            int count = 0;
            for (WeakReference<Preemptable> ref : this.mPreemptable) {
                Preemptable p = ref.get();
                if (p != null) {
                    count++;
                    p.preempt();
                }
            }
            this.mPreemptable.clear();
        }
    }

    public <P> void execute(AsyncTask<P, ?, ?> asyncTask, P... params) {
        if (asyncTask instanceof Preemptable) {
            synchronized (this.mPreemptable) {
                this.mPreemptable.add(new WeakReference<>((Preemptable) asyncTask));
            }
            asyncTask.executeOnExecutor(this.mNonPreemptingExecutor, params);
            return;
        }
        asyncTask.executeOnExecutor(this, params);
    }

    @Override
    public void execute(Runnable command) {
        preempt();
        Preconditions.checkNotNull(command);
        this.mQueue.add(command);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Runnable command = this.mQueue.take();
                command.run();
            } catch (InterruptedException e) {
            }
        }
    }
}
