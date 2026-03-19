package com.android.okhttp.internal;

public abstract class NamedRunnable implements Runnable {
    protected final String name;

    protected abstract void execute();

    public NamedRunnable(String str, Object... objArr) {
        this.name = String.format(str, objArr);
    }

    @Override
    public final void run() {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(this.name);
        try {
            execute();
        } finally {
            Thread.currentThread().setName(name);
        }
    }
}
