package android.support.v7.media;

import android.os.Messenger;

abstract class MediaRouteProviderProtocol {
    public static boolean isValidRemoteMessenger(Messenger messenger) {
        if (messenger != null) {
            try {
                return messenger.getBinder() != null;
            } catch (NullPointerException e) {
                return false;
            }
        }
        return false;
    }
}
