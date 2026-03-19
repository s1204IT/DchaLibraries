package com.android.gallery3d.data;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.picasasource.PicasaSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
        return ((GalleryApp) context.getApplicationContext()).getDataManager();
    }

    private static class DateTakenComparator implements Comparator<MediaItem> {
        private DateTakenComparator() {
        }

        @Override
        public int compare(MediaItem mediaItem, MediaItem mediaItem2) {
            return -Utils.compare(mediaItem.getDateInMs(), mediaItem2.getDateInMs());
        }
    }

    public DataManager(GalleryApp galleryApp) {
        this.mApplication = galleryApp;
        this.mDefaultMainHandler = new Handler(galleryApp.getMainLooper());
        com.android.gallery3d.app.Log.d("Gallery2/DataManager", "<DataManager> this = " + this);
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
                Iterator<MediaSource> it = this.mSourceMap.values().iterator();
                while (it.hasNext()) {
                    it.next().resume();
                }
            }
        }
    }

    public String getTopSetPath(int i) {
        switch (i) {
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
            case 6:
                return "/local/video";
            case 7:
                return "/local/all";
        }
    }

    void addSource(MediaSource mediaSource) {
        if (mediaSource == null) {
            return;
        }
        this.mSourceMap.put(mediaSource.getPrefix(), mediaSource);
    }

    public MediaObject peekMediaObject(Path path) {
        return path.getObject();
    }

    public MediaObject getMediaObject(Path path) {
        synchronized (LOCK) {
            MediaObject object = path.getObject();
            if (object != null) {
                return object;
            }
            MediaSource mediaSource = this.mSourceMap.get(path.getPrefix());
            if (mediaSource == null) {
                com.android.gallery3d.app.Log.w("Gallery2/DataManager", "cannot find media source for path: " + path);
                return null;
            }
            try {
                MediaObject mediaObjectCreateMediaObject = mediaSource.createMediaObject(path);
                if (mediaObjectCreateMediaObject == null) {
                    com.android.gallery3d.app.Log.w("Gallery2/DataManager", "cannot create media object: " + path);
                }
                return mediaObjectCreateMediaObject;
            } catch (Throwable th) {
                com.android.gallery3d.app.Log.w("Gallery2/DataManager", "exception in creating media object: " + path, th);
                path.clearObject();
                return null;
            }
        }
    }

    public MediaObject getMediaObject(String str) {
        return getMediaObject(Path.fromString(str));
    }

    public MediaSet getMediaSet(Path path) {
        return (MediaSet) getMediaObject(path);
    }

    public MediaSet getMediaSet(String str) {
        return (MediaSet) getMediaObject(str);
    }

    public MediaSet[] getMediaSetsFromString(String str) {
        String[] strArrSplitSequence = Path.splitSequence(str);
        int length = strArrSplitSequence.length;
        MediaSet[] mediaSetArr = new MediaSet[length];
        for (int i = 0; i < length; i++) {
            mediaSetArr[i] = getMediaSet(strArrSplitSequence[i]);
        }
        return mediaSetArr;
    }

    public void mapMediaItems(ArrayList<Path> arrayList, MediaSet.ItemConsumer itemConsumer, int i) {
        HashMap map = new HashMap();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            Path path = arrayList.get(i2);
            String prefix = path.getPrefix();
            ArrayList arrayList2 = (ArrayList) map.get(prefix);
            if (arrayList2 == null) {
                arrayList2 = new ArrayList();
                map.put(prefix, arrayList2);
            }
            arrayList2.add(new MediaSource.PathId(path, i2 + i));
        }
        for (Map.Entry entry : map.entrySet()) {
            this.mSourceMap.get((String) entry.getKey()).mapMediaItems((ArrayList) entry.getValue(), itemConsumer);
        }
    }

    public int getSupportedOperations(Path path) {
        return getMediaObject(path).getSupportedOperations();
    }

    public void delete(Path path) {
        getMediaObject(path).delete();
    }

    public void rotate(Path path, int i) {
        getMediaObject(path).rotate(i);
    }

    public Uri getContentUri(Path path) {
        return getMediaObject(path).getContentUri();
    }

    public int getMediaType(Path path) {
        return getMediaObject(path).getMediaType();
    }

    public Path findPathByUri(Uri uri, String str) {
        if (uri == null) {
            return null;
        }
        Iterator<MediaSource> it = this.mSourceMap.values().iterator();
        while (it.hasNext()) {
            Path pathFindPathByUri = it.next().findPathByUri(uri, str);
            if (pathFindPathByUri != null) {
                return pathFindPathByUri;
            }
        }
        return null;
    }

    public Path getDefaultSetOf(Path path) {
        MediaSource mediaSource = this.mSourceMap.get(path.getPrefix());
        if (mediaSource == null) {
            return null;
        }
        return mediaSource.getDefaultSetOf(path);
    }

    public long getTotalUsedCacheSize() {
        Iterator<MediaSource> it = this.mSourceMap.values().iterator();
        long totalUsedCacheSize = 0;
        while (it.hasNext()) {
            totalUsedCacheSize += it.next().getTotalUsedCacheSize();
        }
        return totalUsedCacheSize;
    }

    public long getTotalTargetCacheSize() {
        Iterator<MediaSource> it = this.mSourceMap.values().iterator();
        long totalTargetCacheSize = 0;
        while (it.hasNext()) {
            totalTargetCacheSize += it.next().getTotalTargetCacheSize();
        }
        return totalTargetCacheSize;
    }

    public void registerChangeNotifier(Uri uri, ChangeNotifier changeNotifier) {
        NotifyBroker notifyBroker;
        synchronized (this.mNotifierMap) {
            notifyBroker = this.mNotifierMap.get(uri);
            if (notifyBroker == null) {
                notifyBroker = new NotifyBroker(this.mDefaultMainHandler);
                this.mApplication.getContentResolver().registerContentObserver(uri, true, notifyBroker);
                this.mNotifierMap.put(uri, notifyBroker);
            }
        }
        notifyBroker.registerNotifier(changeNotifier);
    }

    public void resume() {
        int i = this.mActiveCount + 1;
        this.mActiveCount = i;
        if (i == 1) {
            Iterator<MediaSource> it = this.mSourceMap.values().iterator();
            while (it.hasNext()) {
                it.next().resume();
            }
        }
    }

    public void pause() {
        int i = this.mActiveCount - 1;
        this.mActiveCount = i;
        if (i == 0) {
            Iterator<MediaSource> it = this.mSourceMap.values().iterator();
            while (it.hasNext()) {
                it.next().pause();
            }
        }
    }

    private static class NotifyBroker extends ContentObserver {
        private WeakHashMap<ChangeNotifier, Object> mNotifiers;

        public NotifyBroker(Handler handler) {
            super(handler);
            this.mNotifiers = new WeakHashMap<>();
        }

        public synchronized void registerNotifier(ChangeNotifier changeNotifier) {
            this.mNotifiers.put(changeNotifier, null);
        }

        @Override
        public synchronized void onChange(boolean z) {
            Iterator<ChangeNotifier> it = this.mNotifiers.keySet().iterator();
            while (it.hasNext()) {
                it.next().onChange(z);
            }
        }
    }

    public void broadcastUpdatePicture() {
        LocalBroadcastManager.getInstance(this.mApplication.getAndroidContext()).sendBroadcast(new Intent("com.android.gallery3d.action.UPDATE_PICTURE"));
    }

    public void forceRefreshAll() {
        ArrayList arrayList;
        com.android.gallery3d.app.Log.d("Gallery2/DataManager", "<forceRefreshAll>");
        synchronized (this.mNotifierMap) {
            arrayList = new ArrayList(this.mNotifierMap.size());
            Iterator<NotifyBroker> it = this.mNotifierMap.values().iterator();
            while (it.hasNext()) {
                arrayList.add(it.next());
            }
        }
        Iterator it2 = arrayList.iterator();
        while (it2.hasNext()) {
            ((NotifyBroker) it2.next()).onChange(false);
        }
    }

    public boolean reuseDataManager(String str, String str2) {
        String string = toString();
        String strValueOf = String.valueOf(Process.myPid());
        com.android.gallery3d.app.Log.v("Gallery2/DataManager", "<reuseDataManager> lastDataManager = " + str + ", newDataManager = " + string);
        com.android.gallery3d.app.Log.v("Gallery2/DataManager", "<reuseDataManager> lastProcessId = " + str2 + ", newProcessId = " + strValueOf);
        if (str != null && string.equals(str) && str2 != null && strValueOf.equals(str2)) {
            com.android.gallery3d.app.Log.v("Gallery2/DataManager", "<reuseDataManager> return true");
            return true;
        }
        com.android.gallery3d.app.Log.v("Gallery2/DataManager", "<reuseDataManager> return false");
        return false;
    }
}
