package android.database;

import android.net.Uri;
import java.util.Iterator;

public class ContentObservable extends Observable<ContentObserver> {
    @Override
    public void registerObserver(ContentObserver contentObserver) {
        super.registerObserver(contentObserver);
    }

    @Deprecated
    public void dispatchChange(boolean z) {
        dispatchChange(z, null);
    }

    public void dispatchChange(boolean z, Uri uri) {
        synchronized (this.mObservers) {
            for (T t : this.mObservers) {
                if (!z || t.deliverSelfNotifications()) {
                    t.dispatchChange(z, uri);
                }
            }
        }
    }

    @Deprecated
    public void notifyChange(boolean z) {
        synchronized (this.mObservers) {
            Iterator it = this.mObservers.iterator();
            while (it.hasNext()) {
                ((ContentObserver) it.next()).onChange(z, null);
            }
        }
    }
}
