package sun.misc;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Cleaner extends PhantomReference<Object> {
    private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue<>();
    private static Cleaner first = null;
    private Cleaner next;
    private Cleaner prev;
    private final Runnable thunk;

    private static synchronized Cleaner add(Cleaner cleaner) {
        if (first != null) {
            cleaner.next = first;
            first.prev = cleaner;
        }
        first = cleaner;
        return cleaner;
    }

    private static synchronized boolean remove(Cleaner cleaner) {
        if (cleaner.next == cleaner) {
            return false;
        }
        if (first == cleaner) {
            if (cleaner.next != null) {
                first = cleaner.next;
            } else {
                first = cleaner.prev;
            }
        }
        if (cleaner.next != null) {
            cleaner.next.prev = cleaner.prev;
        }
        if (cleaner.prev != null) {
            cleaner.prev.next = cleaner.next;
        }
        cleaner.next = cleaner;
        cleaner.prev = cleaner;
        return true;
    }

    private Cleaner(Object obj, Runnable runnable) {
        super(obj, dummyQueue);
        this.next = null;
        this.prev = null;
        this.thunk = runnable;
    }

    public static Cleaner create(Object obj, Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        return add(new Cleaner(obj, runnable));
    }

    public void clean() {
        if (!remove(this)) {
            return;
        }
        try {
            this.thunk.run();
        } catch (Throwable th) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    if (System.err != null) {
                        new Error("Cleaner terminated abnormally", th).printStackTrace();
                    }
                    System.exit(1);
                    return null;
                }
            });
        }
    }
}
