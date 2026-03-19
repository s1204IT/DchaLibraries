package android.support.v4.media;

import android.content.Intent;
import android.os.IBinder;
import android.support.v4.media.MediaLibraryService2;

class MediaLibraryService2ImplBase extends MediaSessionService2ImplBase {
    MediaLibraryService2ImplBase() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        byte b;
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != 901933117) {
            b = (iHashCode == 1665850838 && action.equals(MediaBrowserServiceCompat.SERVICE_INTERFACE)) ? (byte) 1 : (byte) -1;
        } else if (action.equals(MediaLibraryService2.SERVICE_INTERFACE)) {
            b = 0;
        }
        switch (b) {
            case 0:
                return getSession().getSessionBinder();
            case 1:
                return getSession().getImpl().getLegacySessionBinder();
            default:
                return super.onBind(intent);
        }
    }

    @Override
    public MediaLibraryService2.MediaLibrarySession getSession() {
        return (MediaLibraryService2.MediaLibrarySession) super.getSession();
    }

    @Override
    public int getSessionType() {
        return 2;
    }
}
