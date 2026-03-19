package sun.nio.ch;

class NativeThreadSet {
    static final boolean $assertionsDisabled = false;
    private long[] elts;
    private int used = 0;
    private boolean waitingToEmpty;

    NativeThreadSet(int i) {
        this.elts = new long[i];
    }

    int add() {
        int length;
        long jCurrent = NativeThread.current();
        if (jCurrent == 0) {
            jCurrent = -1;
        }
        synchronized (this) {
            if (this.used >= this.elts.length) {
                length = this.elts.length;
                long[] jArr = new long[length * 2];
                System.arraycopy((Object) this.elts, 0, (Object) jArr, 0, length);
                this.elts = jArr;
            } else {
                length = 0;
            }
            while (length < this.elts.length) {
                if (this.elts[length] != 0) {
                    length++;
                } else {
                    this.elts[length] = jCurrent;
                    this.used++;
                    return length;
                }
            }
            return -1;
        }
    }

    void remove(int i) {
        synchronized (this) {
            this.elts[i] = 0;
            this.used--;
            if (this.used == 0 && this.waitingToEmpty) {
                notifyAll();
            }
        }
    }

    synchronized void signalAndWait() {
        boolean z = false;
        while (this.used > 0) {
            int i = this.used;
            int length = this.elts.length;
            int i2 = i;
            for (int i3 = 0; i3 < length; i3++) {
                long j = this.elts[i3];
                if (j != 0) {
                    if (j != -1) {
                        NativeThread.signal(j);
                    }
                    i2--;
                    if (i2 == 0) {
                        break;
                    }
                }
            }
            this.waitingToEmpty = true;
            try {
                wait(50L);
                this.waitingToEmpty = false;
            } catch (InterruptedException e) {
                this.waitingToEmpty = false;
                z = true;
            } catch (Throwable th) {
                this.waitingToEmpty = false;
                throw th;
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
    }
}
