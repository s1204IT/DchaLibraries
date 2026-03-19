package com.android.internal.telephony.uicc;

public class IccRefreshResponse {
    public static final int REFRESH_INIT_FILE_UPDATED = 5;
    public static final int REFRESH_INIT_FULL_FILE_UPDATED = 4;
    public static final int REFRESH_RESULT_APP_INIT = 3;
    public static final int REFRESH_RESULT_FILE_UPDATE = 0;
    public static final int REFRESH_RESULT_INIT = 1;
    public static final int REFRESH_RESULT_RESET = 2;
    public static final int REFRESH_SESSION_RESET = 6;
    public String aid;
    public int efId;
    public int refreshResult;

    public String toString() {
        return "{" + this.refreshResult + ", " + this.aid + ", " + this.efId + "}";
    }
}
