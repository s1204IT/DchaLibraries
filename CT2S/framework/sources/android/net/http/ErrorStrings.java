package android.net.http;

import android.content.Context;
import android.util.Log;
import com.android.internal.R;

public class ErrorStrings {
    private static final String LOGTAG = "Http";

    private ErrorStrings() {
    }

    public static String getString(int errorCode, Context context) {
        return context.getText(getResource(errorCode)).toString();
    }

    public static int getResource(int errorCode) {
        switch (errorCode) {
            case -15:
                break;
            case -14:
                break;
            case -13:
                break;
            case -12:
                break;
            case -11:
                break;
            case -10:
                break;
            case -9:
                break;
            case -8:
                break;
            case -7:
                break;
            case -6:
                break;
            case -5:
                break;
            case -4:
                break;
            case -3:
                break;
            case -2:
                break;
            case -1:
                break;
            case 0:
                break;
            default:
                Log.w(LOGTAG, "Using generic message for unknown error code: " + errorCode);
                break;
        }
        return R.string.httpError;
    }
}
