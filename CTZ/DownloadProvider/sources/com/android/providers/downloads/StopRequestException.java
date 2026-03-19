package com.android.providers.downloads;

class StopRequestException extends Exception {
    private final int mFinalStatus;

    public StopRequestException(int i, String str) {
        super(str);
        this.mFinalStatus = i;
    }

    public StopRequestException(int i, Throwable th) {
        this(i, th.getMessage());
        initCause(th);
    }

    public StopRequestException(int i, String str, Throwable th) {
        this(i, str);
        initCause(th);
    }

    public int getFinalStatus() {
        return this.mFinalStatus;
    }

    public static StopRequestException throwUnhandledHttpError(int i, String str) throws StopRequestException {
        String str2 = "Unhandled HTTP response: " + i + " " + str;
        if (i >= 400 && i < 600) {
            throw new StopRequestException(i, str2);
        }
        if (i >= 300 && i < 400) {
            throw new StopRequestException(493, str2);
        }
        throw new StopRequestException(494, str2);
    }
}
