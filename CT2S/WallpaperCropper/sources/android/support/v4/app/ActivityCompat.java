package android.support.v4.app;

import android.app.Activity;
import android.os.Build;
import android.support.v4.content.ContextCompat;

public class ActivityCompat extends ContextCompat {
    public static void finishAfterTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.finishAfterTransition(activity);
        } else {
            activity.finish();
        }
    }
}
