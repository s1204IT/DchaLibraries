package com.android.gallery3d.data;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentManagerImpl;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.picasasource.PicasaSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class DataManager {
    public static final Object LOCK = new Object();
    public static final Comparator<MediaItem> sDateTakenComparator = new DateTakenComparator();
    private GalleryApp mApplication;
    private final Handler mDefaultMainHandler;
    private int mActiveCount = 0;
    private HashMap<Uri, NotifyBroker> mNotifierMap = new HashMap<>();
    private HashMap<String, MediaSource> mSourceMap = new LinkedHashMap();

    public static DataManager from(Context context) {
        GalleryApp app = (GalleryApp) context.getApplicationContext();
        return app.getDataManager();
    }

    private static class DateTakenComparator implements Comparator<MediaItem> {
        private DateTakenComparator() {
        }

        @Override
        public int compare(MediaItem item1, MediaItem item2) {
            return -Utils.compare(item1.getDateInMs(), item2.getDateInMs());
        }
    }

    public DataManager(GalleryApp application) {
        this.mApplication = application;
        this.mDefaultMainHandler = new Handler(application.getMainLooper());
    }

    public synchronized void initializeSourceMap() {
        if (this.mSourceMap.isEmpty()) {
            addSource(new LocalSource(this.mApplication));
            addSource(new PicasaSource(this.mApplication));
            addSource(new ComboSource(this.mApplication));
            addSource(new ClusterSource(this.mApplication));
            addSource(new FilterSource(this.mApplication));
            addSource(new SecureSource(this.mApplication));
            addSource(new UriSource(this.mApplication));
            addSource(new SnailSource(this.mApplication));
            if (this.mActiveCount > 0) {
                for (MediaSource source : this.mSourceMap.values()) {
                    source.resume();
                }
            }
        }
    }

    public String getTopSetPath(int typeBits) {
        switch (typeBits) {
            case 1:
                return "/combo/{/local/image,/picasa/image}";
            case 2:
                return "/combo/{/local/video,/picasa/video}";
            case 3:
                return "/combo/{/local/all,/picasa/all}";
            case 4:
            default:
                throw new IllegalArgumentException();
            case 5:
                return "/local/image";
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return "/local/video";
            case 7:
                return "/local/all";
        }
    }

    void addSource(MediaSource source) {
        if (source != null) {
            this.mSourceMap.put(source.getPrefix(), source);
        }
    }

    public MediaObject peekMediaObject(Path path) {
        return path.getObject();
    }

    public MediaObject getMediaObject(Path path) {
        synchronized (LOCK) {
            MediaObject obj = path.getObject();
            if (obj == null) {
                MediaSource source = this.mSourceMap.get(path.getPrefix());
                if (source == null) {
                    Log.w("DataManager", "cannot find media source for path: " + path);
                    return null;
                }
                try {
                    MediaObject object = source.createMediaObject(path);
                    if (object == null) {
                        Log.w("DataManager", "cannot create media object: " + path);
                    }
                    return object;
                } catch (Throwable t) {
                    Log.w("DataManager", "exception in creating media object: " + path, t);
                    return null;
                }
            }
            return obj;
        }
    }

    public MediaObject getMediaObject(String s) {
        return getMediaObject(Path.fromString(s));
    }

    public MediaSet getMediaSet(Path path) {
        return (MediaSet) getMediaObject(path);
    }

    public MediaSet getMediaSet(String s) {
        return (MediaSet) getMediaObject(s);
    }

    public MediaSet[] getMediaSetsFromString(String segment) {
        String[] seq = Path.splitSequence(segment);
        int n = seq.length;
        MediaSet[] sets = new MediaSet[n];
        for (int i = 0; i < n; i++) {
            sets[i] = getMediaSet(seq[i]);
        }
        return sets;
    }

    public void mapMediaItems(ArrayList<Path> list, MediaSet.ItemConsumer consumer, int startIndex) {
        HashMap<String, ArrayList<MediaSource.PathId>> map = new HashMap<>();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Path path = list.get(i);
            String prefix = path.getPrefix();
            ArrayList<MediaSource.PathId> group = map.get(prefix);
            if (group == null) {
                group = new ArrayList<>();
                map.put(prefix, group);
            }
            group.add(new MediaSource.PathId(path, i + startIndex));
        }
        for (Map.Entry<String, ArrayList<MediaSource.PathId>> entry : map.entrySet()) {
            MediaSource source = this.mSourceMap.get(entry.getKey());
            source.mapMediaItems(entry.getValue(), consumer);
        }
    }

    public int getSupportedOperations(Path path) {
        return getMediaObject(path).getSupportedOperations();
    }

    public void delete(Path path) {
        getMediaObject(path).delete();
    }

    public void rotate(Path path, int degrees) {
        getMediaObject(path).rotate(degrees);
    }

    public Uri getContentUri(Path path) {
        return getMediaObject(path).getContentUri();
    }

    public int getMediaType(Path path) {
        return getMediaObject(path).getMediaType();
    }

    public Path findPathByUri(Uri uri, String type) {
        if (uri == null) {
            return null;
        }
        for (MediaSource source : this.mSourceMap.values()) {
            Path path = source.findPathByUri(uri, type);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    public Path getDefaultSetOf(Path item) {
        MediaSource source = this.mSourceMap.get(item.getPrefix());
        if (source == null) {
            return null;
        }
        return source.getDefaultSetOf(item);
    }

    public long getTotalUsedCacheSize() {
        long sum = 0;
        for (MediaSource source : this.mSourceMap.values()) {
            sum += source.getTotalUsedCacheSize();
        }
        return sum;
    }

    public long getTotalTargetCacheSize() {
        long sum = 0;
        for (MediaSource source : this.mSourceMap.values()) {
            sum += source.getTotalTargetCacheSize();
        }
        return sum;
    }

    public void registerChangeNotifier(Uri uri, ChangeNotifier notifier) throws Throwable {
        synchronized (this.mNotifierMap) {
            try {
                NotifyBroker broker = this.mNotifierMap.get(uri);
                if (broker == null) {
                    NotifyBroker broker2 = new NotifyBroker(this.mDefaultMainHandler);
                    try {
                        this.mApplication.getContentResolver().registerContentObserver(uri, true, broker2);
                        this.mNotifierMap.put(uri, broker2);
                        broker = broker2;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                broker.registerNotifier(notifier);
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void resume() {
        int i = this.mActiveCount + 1;
        this.mActiveCount = i;
        if (i == 1) {
            for (MediaSource source : this.mSourceMap.values()) {
                source.resume();
            }
        }
    }

    public void pause() {
        int i = this.mActiveCount - 1;
        this.mActiveCount = i;
        if (i == 0) {
            for (MediaSource source : this.mSourceMap.values()) {
                source.pause();
            }
        }
    }

    private static class NotifyBroker extends ContentObserver {
        private WeakHashMap<ChangeNotifier, Object> mNotifiers;

        public NotifyBroker(Handler handler) {
            super(handler);
            this.mNotifiers = new WeakHashMap<>();
        }

        public synchronized void registerNotifier(ChangeNotifier notifier) {
            this.mNotifiers.put(notifier, null);
        }

        @Override
        public synchronized void onChange(boolean selfChange) {
            for (ChangeNotifier notifier : this.mNotifiers.keySet()) {
                notifier.onChange(selfChange);
            }
        }
    }
}
