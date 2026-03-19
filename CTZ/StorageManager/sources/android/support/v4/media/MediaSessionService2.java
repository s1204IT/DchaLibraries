package android.support.v4.media;

import android.app.Service;

public abstract class MediaSessionService2 extends Service {
    private final SupportLibraryImpl mImpl = createImpl();

    interface SupportLibraryImpl {
    }

    SupportLibraryImpl createImpl() {
        return new MediaSessionService2ImplBase();
    }
}
