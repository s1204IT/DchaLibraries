package com.android.documentsui;

import android.os.AsyncTask;
import com.android.internal.annotations.GuardedBy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class ProviderExecutor extends Thread implements Executor {
    static final boolean $assertionsDisabled = false;

    @GuardedBy("sExecutors")
    private static HashMap<String, ProviderExecutor> sExecutors = new HashMap<>();
    private final LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();
    private final ArrayList<WeakReference<Preemptable>> mPreemptable = new ArrayList<>();
    private Executor mNonPreemptingExecutor = new Executor() {
        static final boolean $assertionsDisabled = false;

        @Override
        public void execute(Runnable runnable) {
            ProviderExecutor.this.mQueue.add(runnable);
        }
    };

    public interface Preemptable {
        void preempt();
    }

    public static ProviderExecutor forAuthority(String str) {
        ProviderExecutor providerExecutor;
        synchronized (sExecutors) {
            providerExecutor = sExecutors.get(str);
            if (providerExecutor == null) {
                providerExecutor = new ProviderExecutor();
                providerExecutor.setName("ProviderExecutor: " + str);
                providerExecutor.start();
                sExecutors.put(str, providerExecutor);
            }
        }
        return providerExecutor;
    }

    private void preempt() {
        synchronized (this.mPreemptable) {
            Iterator<WeakReference<Preemptable>> it = this.mPreemptable.iterator();
            while (it.hasNext()) {
                Preemptable preemptable = it.next().get();
                if (preemptable != null) {
                    preemptable.preempt();
                }
            }
            this.mPreemptable.clear();
        }
    }

    public <P> void execute(AsyncTask<P, ?, ?> asyncTask, P... pArr) {
        if (asyncTask instanceof Preemptable) {
            synchronized (this.mPreemptable) {
                this.mPreemptable.add(new WeakReference<>((Preemptable) asyncTask));
            }
            asyncTask.executeOnExecutor(this.mNonPreemptingExecutor, pArr);
            return;
        }
        asyncTask.executeOnExecutor(this, pArr);
    }

    @Override
    public void execute(Runnable runnable) {
        preempt();
        this.mQueue.add(runnable);
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.mQueue.take().run();
            } catch (InterruptedException e) {
            }
        }
    }
}
