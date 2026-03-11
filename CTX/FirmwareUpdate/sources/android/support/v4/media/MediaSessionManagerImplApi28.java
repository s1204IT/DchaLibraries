package android.support.v4.media;

import android.media.session.MediaSessionManager;
import android.support.v4.media.MediaSessionManager;

class MediaSessionManagerImplApi28 extends MediaSessionManagerImplApi21 {

    static final class RemoteUserInfo implements MediaSessionManager.RemoteUserInfoImpl {
        MediaSessionManager.RemoteUserInfo mObject;

        RemoteUserInfo(String str, int i, int i2) {
            this.mObject = new MediaSessionManager.RemoteUserInfo(str, i, i2);
        }
    }
}
