package com.android.systemui.recents.misc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import java.util.ArrayList;

public class ReferenceCountedTrigger {
    Context mContext;
    int mCount;
    Runnable mErrorRunnable;
    ArrayList<Runnable> mFirstIncRunnables = new ArrayList<>();
    ArrayList<Runnable> mLastDecRunnables = new ArrayList<>();
    Runnable mIncrementRunnable = new Runnable() {
        @Override
        public void run() {
            ReferenceCountedTrigger.this.increment();
        }
    };
    Runnable mDecrementRunnable = new Runnable() {
        @Override
        public void run() {
            ReferenceCountedTrigger.this.decrement();
        }
    };

    public ReferenceCountedTrigger(Context context, Runnable firstIncRunnable, Runnable lastDecRunnable, Runnable errorRunanable) {
        this.mContext = context;
        if (firstIncRunnable != null) {
            this.mFirstIncRunnables.add(firstIncRunnable);
        }
        if (lastDecRunnable != null) {
            this.mLastDecRunnables.add(lastDecRunnable);
        }
        this.mErrorRunnable = errorRunanable;
    }

    public void increment() {
        if (this.mCount == 0 && !this.mFirstIncRunnables.isEmpty()) {
            int numRunnables = this.mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                this.mFirstIncRunnables.get(i).run();
            }
        }
        this.mCount++;
    }

    public void addLastDecrementRunnable(Runnable r) {
        boolean ensureLastDecrement = this.mCount == 0;
        if (ensureLastDecrement) {
            increment();
        }
        this.mLastDecRunnables.add(r);
        if (ensureLastDecrement) {
            decrement();
        }
    }

    public void decrement() {
        this.mCount--;
        if (this.mCount == 0 && !this.mLastDecRunnables.isEmpty()) {
            int numRunnables = this.mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                this.mLastDecRunnables.get(i).run();
            }
            return;
        }
        if (this.mCount < 0) {
            if (this.mErrorRunnable != null) {
                this.mErrorRunnable.run();
            } else {
                new Throwable("Invalid ref count").printStackTrace();
                Console.logError(this.mContext, "Invalid ref count");
            }
        }
    }

    public Runnable decrementAsRunnable() {
        return this.mDecrementRunnable;
    }

    public Animator.AnimatorListener decrementOnAnimationEnd() {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ReferenceCountedTrigger.this.decrement();
            }
        };
    }
}
