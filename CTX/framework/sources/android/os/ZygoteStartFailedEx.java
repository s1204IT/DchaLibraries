package android.os;

class ZygoteStartFailedEx extends Exception {
    ZygoteStartFailedEx(String str) {
        super(str);
    }

    ZygoteStartFailedEx(Throwable th) {
        super(th);
    }

    ZygoteStartFailedEx(String str, Throwable th) {
        super(str, th);
    }
}
