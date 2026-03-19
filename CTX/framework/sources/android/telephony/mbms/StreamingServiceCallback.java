package android.telephony.mbms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class StreamingServiceCallback {
    public static final int SIGNAL_STRENGTH_UNAVAILABLE = -1;

    @Retention(RetentionPolicy.SOURCE)
    private @interface StreamingServiceError {
    }

    public void onError(int i, String str) {
    }

    public void onStreamStateUpdated(int i, int i2) {
    }

    public void onMediaDescriptionUpdated() {
    }

    public void onBroadcastSignalStrengthUpdated(int i) {
    }

    public void onStreamMethodUpdated(int i) {
    }
}
