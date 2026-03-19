package android.telephony.mbms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class MbmsStreamingSessionCallback {

    @Retention(RetentionPolicy.SOURCE)
    private @interface StreamingError {
    }

    public void onError(int i, String str) {
    }

    public void onStreamingServicesUpdated(List<StreamingServiceInfo> list) {
    }

    public void onMiddlewareReady() {
    }
}
