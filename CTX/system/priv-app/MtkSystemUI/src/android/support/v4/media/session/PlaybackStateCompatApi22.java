package android.support.v4.media.session;

import android.media.session.PlaybackState;
import android.os.Bundle;
/* loaded from: classes.dex */
class PlaybackStateCompatApi22 {
    public static Bundle getExtras(Object stateObj) {
        return ((PlaybackState) stateObj).getExtras();
    }
}
