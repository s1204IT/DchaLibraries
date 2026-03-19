package com.android.gallery3d.data;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class ClusterAlbumSet extends MediaSet implements ContentListener {
    private static boolean sIsDeleteOperation = false;
    private Clustering clustering;
    public int currentIndexOfSet;
    private ArrayList<ClusterAlbum> mAlbums;
    private GalleryApp mApplication;
    private MediaSet mBaseSet;
    private String mCurrentLanguage;
    private boolean mFirstReloadDone;
    private int mKind;
    private boolean mStopReload;
    private String mTimeFormat;

    public ClusterAlbumSet(Path path, GalleryApp galleryApp, MediaSet mediaSet, int i) {
        super(path, -1L);
        this.mAlbums = new ArrayList<>();
        this.mCurrentLanguage = Locale.getDefault().getLanguage().toString();
        this.mApplication = galleryApp;
        this.mBaseSet = mediaSet;
        this.mKind = i;
        mediaSet.addContentListener(this);
    }

    @Override
    public MediaSet getSubMediaSet(int i) {
        if (i >= this.mAlbums.size()) {
            Log.d("Gallery2/ClusterAlbumSet", "<getSubMediaSet> index = " + i + ", mAlbums.size() = " + this.mAlbums.size() + ", return null");
            return null;
        }
        return this.mAlbums.get(i);
    }

    @Override
    public int getSubMediaSetCount() {
        return this.mAlbums.size();
    }

    @Override
    public String getName() {
        return this.mBaseSet.getName();
    }

    @Override
    public synchronized long reload() {
        boolean z = this.mFirstReloadDone && ((this.mBaseSet instanceof LocalAlbumSet) || (this.mBaseSet instanceof ComboAlbumSet));
        if (this.mBaseSet instanceof ClusterAlbum) {
            this.mBaseSet.offsetInStack = this.offsetInStack + 1;
        }
        this.mStopReload = false;
        if (((this.offsetInStack % 2 == 1 && z) ? this.mBaseSet.synchronizedAlbumData() : this.mBaseSet.reload()) > this.mDataVersion) {
            Log.d("Gallery2/ClusterAlbumSet", "<reload>total media item count: " + this.mBaseSet.getTotalMediaItemCount());
            if (this.mFirstReloadDone) {
                if (!sIsDeleteOperation) {
                    updateClustersContents();
                } else {
                    updateClustersContentsForDeleteOperation();
                }
            } else {
                updateClusters();
                this.mFirstReloadDone = true;
            }
            if (!this.mStopReload) {
                this.mDataVersion = nextVersionNumber();
            }
        } else {
            reloadName();
            Log.d("Gallery2/ClusterAlbumSet", "<reload>ClusterAlbumSet: mBaseSet.reload() <= mDataVersion");
        }
        this.mCurrentClusterAlbum = null;
        this.offsetInStack = 0;
        return this.mDataVersion;
    }

    @Override
    public boolean isLoading() {
        return this.mBaseSet.isLoading();
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateClusters() {
        Path child;
        ClusterAlbum clusterAlbum;
        Log.d("Gallery2/ClusterAlbumSet", "<updateClusters>");
        Context androidContext = this.mApplication.getAndroidContext();
        int i = this.mKind;
        if (i != 4) {
            switch (i) {
                case 0:
                    this.clustering = new TimeClustering(androidContext);
                    break;
                case 1:
                    this.clustering = new LocationClustering(androidContext);
                    break;
                case 2:
                    this.clustering = new TagClustering(androidContext);
                    break;
                default:
                    this.clustering = new SizeClustering(androidContext);
                    break;
            }
        } else {
            this.clustering = new FaceClustering(androidContext);
        }
        this.clustering.run(this.mBaseSet);
        int numberOfClusters = this.clustering.getNumberOfClusters();
        Log.d("Gallery2/ClusterAlbumSet", "<updateClusters>number of clusters: " + numberOfClusters);
        this.mAlbums.clear();
        DataManager dataManager = this.mApplication.getDataManager();
        for (int i2 = 0; i2 < numberOfClusters; i2++) {
            String clusterName = this.clustering.getClusterName(i2);
            if (this.mKind == 2) {
                child = this.mPath.getChild(Uri.encode(clusterName));
            } else if (this.mKind == 3) {
                child = this.mPath.getChild(((SizeClustering) this.clustering).getMinSize(i2));
            } else {
                child = this.mPath.getChild(i2);
            }
            synchronized (DataManager.LOCK) {
                clusterAlbum = (ClusterAlbum) dataManager.peekMediaObject(child);
                if (clusterAlbum == null) {
                    clusterAlbum = new ClusterAlbum(child, dataManager, this);
                }
            }
            clusterAlbum.setMediaItems(this.clustering.getCluster(i2));
            clusterAlbum.pathSet();
            sIsDeleteOperation = false;
            clusterAlbum.setNumberOfDeletedImage(0);
            clusterAlbum.setName(clusterName);
            clusterAlbum.setCoverMediaItem(this.clustering.getClusterCover(i2));
            this.mAlbums.add(clusterAlbum);
        }
    }

    private void updateClustersContents() {
        final HashSet hashSet = new HashSet();
        final HashMap<Path, MediaItem> map = new HashMap<>();
        this.mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                if (mediaItem == null) {
                    Log.d("Gallery2/ClusterAlbumSet", "<updateClustersContents> consume, item is null, return");
                } else {
                    hashSet.add(mediaItem.getPath());
                    map.put(mediaItem.getPath(), mediaItem);
                }
            }

            @Override
            public boolean stopConsume() {
                return ClusterAlbumSet.this.mStopReload;
            }
        });
        HashSet hashSet2 = new HashSet();
        int i = 0;
        for (int size = this.mAlbums.size() - 1; size >= 0; size--) {
            ArrayList<Path> mediaItems = this.mAlbums.get(size).getMediaItems();
            ArrayList<Path> arrayList = new ArrayList<>();
            int size2 = mediaItems.size();
            int i2 = 0;
            for (int i3 = 0; i3 < size2; i3++) {
                Path path = mediaItems.get(i3);
                hashSet2.add(path);
                if (hashSet.contains(path)) {
                    arrayList.add(path);
                } else {
                    i2++;
                }
            }
            i += i2;
            this.mAlbums.get(size).setMediaItems(arrayList);
            if (i2 > 0) {
                this.mAlbums.get(size).nextVersion();
            }
            if (arrayList.isEmpty()) {
                this.mAlbums.remove(size);
            }
        }
        if (hashSet.size() + i > hashSet2.size()) {
            Log.d("Gallery2/ClusterAlbumSet", "<updateClustersContents> offsetOFStack==" + this.offsetInStack + " currentIndexOfSet=" + this.currentIndexOfSet);
            if (this.offsetInStack >= 1) {
                ArrayList<Path> arrayList2 = new ArrayList<>();
                ArrayList arrayList3 = new ArrayList(hashSet);
                int size3 = arrayList3.size();
                for (int i4 = 0; i4 < size3; i4++) {
                    if (!hashSet2.contains(arrayList3.get(i4))) {
                        arrayList2.add((Path) arrayList3.get(i4));
                    }
                }
                setCurrentIndexOfSet();
                try {
                    updateAlbumInClusters(rollBackAlbumInClusters(arrayList2, map), map);
                    return;
                } catch (ConcurrentModificationException e) {
                    e.printStackTrace();
                    return;
                }
            }
            updateClusters();
            for (int size4 = this.mAlbums.size() - 1; size4 >= 0; size4--) {
                this.mAlbums.get(size4).nextVersion();
            }
        }
    }

    private void reloadName() {
        String string = Locale.getDefault().getLanguage().toString();
        String string2 = Settings.System.getString(this.mApplication.getContentResolver(), "time_12_24");
        if ((string != null && !string.equals(this.mCurrentLanguage)) || (string2 != null && !string2.equals(this.mTimeFormat))) {
            Log.d("Gallery2/ClusterAlbumSet", "<reloadName> Change Language > current language = " + this.mCurrentLanguage + " New language = " + string + " current timeFormat = " + this.mTimeFormat + " New timeFormat = " + string2);
            synchronized (this.clustering) {
                this.clustering.reGenerateName();
                for (int i = 0; i < this.mAlbums.size(); i++) {
                    this.mAlbums.get(i).setName(this.clustering.getClusterName(i));
                }
                this.mCurrentLanguage = string;
                this.mTimeFormat = string2;
                this.mDataVersion = nextVersionNumber();
            }
        }
    }

    private void updateClustersContentsForDeleteOperation() {
        for (int size = this.mAlbums.size() - 1; size >= 0; size--) {
            if (this.mAlbums.get(size).getNumberOfDeletedImage() == this.mAlbums.get(size).getMediaItemCount()) {
                this.mAlbums.get(size).setNumberOfDeletedImage(0);
                this.mAlbums.remove(size);
            }
        }
    }

    @Override
    public long reloadForSlideShow() {
        boolean z = this.mFirstReloadDone && ((this.mBaseSet instanceof LocalAlbumSet) || (this.mBaseSet instanceof ComboAlbumSet));
        if (this.mBaseSet instanceof ClusterAlbum) {
            this.mBaseSet.offsetInStack = this.offsetInStack + 1;
        }
        if (((this.offsetInStack % 2 == 1 && z) ? this.mBaseSet.synchronizedAlbumData() : this.mBaseSet.reloadForSlideShow()) > this.mDataVersion) {
            Log.d("Gallery2/ClusterAlbumSet", "<reloadForSlideShow>total media item count: " + this.mBaseSet.getTotalMediaItemCount());
            if (this.mFirstReloadDone) {
                if (!sIsDeleteOperation) {
                    updateClustersContents();
                } else {
                    updateClustersContentsForDeleteOperation();
                }
            } else {
                updateClusters();
                this.mFirstReloadDone = true;
            }
            this.mDataVersion = nextVersionNumber();
        } else {
            Log.d("Gallery2/ClusterAlbumSet", "<reloadForSlideShow>ClusterAlbumSet: mBaseSet.reload() <= mDataVersion");
        }
        this.mCurrentClusterAlbum = null;
        this.offsetInStack = 0;
        return this.mDataVersion;
    }

    public void setCurrentIndexOfSet() {
        boolean z;
        int size = this.mAlbums.size();
        if (this.mCurrentClusterAlbum != null) {
            int i = 0;
            while (true) {
                if (i < size) {
                    if (this.mAlbums.get(i) != this.mCurrentClusterAlbum) {
                        i++;
                    } else {
                        this.currentIndexOfSet = i;
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                this.currentIndexOfSet = 0;
                Log.d("Gallery2/ClusterAlbumSet", "[setCurrentIndexOfSet]: has not find set");
            }
        }
    }

    public void updateAlbumInClusters(ArrayList<Path> arrayList, HashMap<Path, MediaItem> map) {
        if (this.mAlbums != null) {
            if (this.currentIndexOfSet < this.mAlbums.size() && this.currentIndexOfSet >= 0) {
                try {
                    addNewPathToAlbum(this.mAlbums.get(this.currentIndexOfSet), arrayList, map);
                } catch (OutOfMemoryError e) {
                    Log.w("Gallery2/ClusterAlbumSet", "<updateAlbumInClusters> maybe sizeOldMediaItems is too big:" + e);
                }
            }
            Log.d("Gallery2/ClusterAlbumSet", "<updateAlbumInClusters>currentIndexOfSet==" + this.currentIndexOfSet);
        }
    }

    public ArrayList<Path> rollBackAlbumInClusters(ArrayList<Path> arrayList, HashMap<Path, MediaItem> map) {
        if (this.mAlbums != null) {
            try {
                int size = this.mAlbums.size();
                for (int i = 0; i < size; i++) {
                    ArrayList<Path> arrayList2 = new ArrayList<>();
                    ClusterAlbum clusterAlbum = this.mAlbums.get(i);
                    if (clusterAlbum != null) {
                        HashSet<Path> pathSet = clusterAlbum.getPathSet();
                        for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                            if (pathSet.contains(arrayList.get(size2))) {
                                arrayList2.add(arrayList.remove(size2));
                            }
                        }
                        addNewPathToAlbum(clusterAlbum, arrayList2, map);
                    }
                }
            } catch (OutOfMemoryError e) {
                Log.w("Gallery2/ClusterAlbumSet", "<rollBackAlbumInClusters> maybe sizeOldMediaItems is too big:" + e);
            }
        }
        return arrayList;
    }

    private void addNewPathToAlbum(ClusterAlbum clusterAlbum, ArrayList<Path> arrayList, HashMap<Path, MediaItem> map) {
        MediaSet mediaSet = this.mApplication.getDataManager().getMediaSet(clusterAlbum.mPath);
        if (mediaSet == null) {
            return;
        }
        ArrayList<MediaItem> mediaItem = mediaSet.getMediaItem(0, mediaSet.getMediaItemCount());
        int size = mediaItem.size();
        int size2 = arrayList.size();
        for (int i = 0; i < size2; i++) {
            MediaItem mediaItem2 = map.get(arrayList.get(i));
            if (mediaItem2 != null) {
                int i2 = 0;
                while (true) {
                    if (i2 >= size) {
                        break;
                    }
                    if (mediaItem2.getDateInMs() != mediaItem.get(i2).getDateInMs()) {
                        i2++;
                    } else {
                        clusterAlbum.addMediaItems(arrayList.get(i), i2);
                        break;
                    }
                }
                if (i2 == size) {
                    clusterAlbum.addMediaItems(arrayList.get(i), 0);
                }
            }
        }
    }

    @Override
    public long synchronizedAlbumData() {
        if (this.mBaseSet.synchronizedAlbumData() > this.mDataVersion) {
            updateClustersContents();
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    public static void setClusterDeleteOperation(boolean z) {
        Log.d("Gallery2/ClusterAlbumSet", "<setClusterDeleteOperation>setClusterDeleteOperation sIsDeleteOperation: " + z);
        sIsDeleteOperation = z;
    }

    public static boolean getClusterDeleteOperation() {
        Log.d("Gallery2/ClusterAlbumSet", "<getClusterDeleteOperation> sIsDeleteOperation: " + sIsDeleteOperation);
        return sIsDeleteOperation;
    }

    @Override
    public void stopReload() {
        Log.d("Gallery2/ClusterAlbumSet", "<stopReload> ClusterAlbumSet ------------------------");
        this.mStopReload = true;
        if (this.clustering != null) {
            this.clustering.mStopEnumerate = true;
        }
    }
}
