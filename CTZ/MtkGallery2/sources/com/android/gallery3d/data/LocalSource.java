package com.android.gallery3d.data;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class LocalSource extends MediaSource {
    public static final Comparator<MediaSource.PathId> sIdComparator = new IdComparator();
    private GalleryApp mApplication;
    private ContentProviderClient mClient;
    private PathMatcher mMatcher;
    private final UriMatcher mUriMatcher;

    public LocalSource(GalleryApp galleryApp) {
        super("local");
        this.mUriMatcher = new UriMatcher(-1);
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/local/image", 0);
        this.mMatcher.add("/local/video", 1);
        this.mMatcher.add("/local/all", 6);
        this.mMatcher.add("/local/image/*", 2);
        this.mMatcher.add("/local/video/*", 3);
        this.mMatcher.add("/local/all/*", 7);
        this.mMatcher.add("/local/image/item/*", 4);
        this.mMatcher.add("/local/video/item/*", 5);
        this.mUriMatcher.addURI("media", "external/images/media/#", 4);
        this.mUriMatcher.addURI("media", "external/video/media/#", 5);
        this.mUriMatcher.addURI("media", "external/images/media", 2);
        this.mUriMatcher.addURI("media", "external/video/media", 3);
        this.mUriMatcher.addURI("media", "external/file", 7);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        GalleryApp galleryApp = this.mApplication;
        switch (this.mMatcher.match(path)) {
            case 0:
            case 1:
            case 6:
                return new LocalAlbumSet(path, this.mApplication);
            case 2:
                return new LocalAlbum(path, galleryApp, this.mMatcher.getIntVar(0), true);
            case 3:
                return new LocalAlbum(path, galleryApp, this.mMatcher.getIntVar(0), false);
            case 4:
                return new LocalImage(path, this.mApplication, this.mMatcher.getIntVar(0));
            case 5:
                return new LocalVideo(path, this.mApplication, this.mMatcher.getIntVar(0));
            case 7:
                int intVar = this.mMatcher.getIntVar(0);
                DataManager dataManager = galleryApp.getDataManager();
                return new LocalMergeAlbum(path, DataManager.sDateTakenComparator, new MediaSet[]{(MediaSet) dataManager.getMediaObject(LocalAlbumSet.PATH_IMAGE.getChild(intVar)), (MediaSet) dataManager.getMediaObject(LocalAlbumSet.PATH_VIDEO.getChild(intVar))}, intVar);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    private static int getMediaType(String str, int i) {
        int i2;
        if (str == null) {
            return i;
        }
        try {
            i2 = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Log.w("Gallery2/LocalSource", "invalid type: " + str, e);
        }
        if ((i2 & 5) != 0) {
            return i2;
        }
        return i;
    }

    private Path getAlbumPath(Uri uri, int i) {
        int mediaType = getMediaType(uri.getQueryParameter("mediaTypes"), i);
        String queryParameter = uri.getQueryParameter("bucketId");
        try {
            int i2 = Integer.parseInt(queryParameter);
            if (mediaType == 1) {
                return Path.fromString("/local/image").getChild(i2);
            }
            if (mediaType == 4) {
                return Path.fromString("/local/video").getChild(i2);
            }
            return Path.fromString("/local/all").getChild(i2);
        } catch (NumberFormatException e) {
            Log.w("Gallery2/LocalSource", "invalid bucket id: " + queryParameter, e);
            return null;
        }
    }

    @Override
    public Path findPathByUri(Uri uri, String str) {
        int iMatch;
        try {
            iMatch = this.mUriMatcher.match(uri);
        } catch (NumberFormatException e) {
            Log.w("Gallery2/LocalSource", "uri: " + uri.toString(), e);
        }
        if (iMatch != 7) {
            switch (iMatch) {
                case 2:
                    return getAlbumPath(uri, 1);
                case 3:
                    return getAlbumPath(uri, 4);
                case 4:
                    long id = ContentUris.parseId(uri);
                    if (id >= 0) {
                        return LocalImage.ITEM_PATH.getChild(id);
                    }
                    return null;
                case 5:
                    long id2 = ContentUris.parseId(uri);
                    if (id2 >= 0) {
                        return LocalVideo.ITEM_PATH.getChild(id2);
                    }
                    return null;
                default:
                    return null;
            }
        }
        return getAlbumPath(uri, 0);
    }

    @Override
    public Path getDefaultSetOf(Path path) {
        ?? mediaObject = this.mApplication.getDataManager().getMediaObject(path);
        if (mediaObject instanceof LocalMediaItem) {
            return Path.fromString("/local/all").getChild(String.valueOf(mediaObject.getBucketId()));
        }
        return null;
    }

    @Override
    public void mapMediaItems(ArrayList<MediaSource.PathId> arrayList, MediaSet.ItemConsumer itemConsumer) {
        ArrayList<MediaSource.PathId> arrayList2 = new ArrayList<>();
        ArrayList<MediaSource.PathId> arrayList3 = new ArrayList<>();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            MediaSource.PathId pathId = arrayList.get(i);
            Path parent = pathId.path.getParent();
            if (parent == LocalImage.ITEM_PATH) {
                arrayList2.add(pathId);
            } else if (parent == LocalVideo.ITEM_PATH) {
                arrayList3.add(pathId);
            }
        }
        processMapMediaItems(arrayList2, itemConsumer, true);
        processMapMediaItems(arrayList3, itemConsumer, false);
    }

    private void processMapMediaItems(ArrayList<MediaSource.PathId> arrayList, MediaSet.ItemConsumer itemConsumer, boolean z) {
        Collections.sort(arrayList, sIdComparator);
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            MediaSource.PathId pathId = arrayList.get(i);
            ArrayList arrayList2 = new ArrayList();
            int i2 = Integer.parseInt(pathId.path.getSuffix());
            arrayList2.add(Integer.valueOf(i2));
            int i3 = i + 1;
            while (i3 < size) {
                int i4 = Integer.parseInt(arrayList.get(i3).path.getSuffix());
                if (i4 - i2 >= 500) {
                    break;
                }
                arrayList2.add(Integer.valueOf(i4));
                i3++;
            }
            MediaItem[] mediaItemById = LocalAlbum.getMediaItemById(this.mApplication, z, arrayList2);
            for (int i5 = i; i5 < i3; i5++) {
                itemConsumer.consume(arrayList.get(i5).id, mediaItemById[i5 - i]);
            }
            i = i3;
        }
    }

    private static class IdComparator implements Comparator<MediaSource.PathId> {
        private IdComparator() {
        }

        @Override
        public int compare(MediaSource.PathId pathId, MediaSource.PathId pathId2) {
            String suffix = pathId.path.getSuffix();
            String suffix2 = pathId2.path.getSuffix();
            int length = suffix.length();
            int length2 = suffix2.length();
            if (length < length2) {
                return -1;
            }
            if (length > length2) {
                return 1;
            }
            return suffix.compareTo(suffix2);
        }
    }

    @Override
    public void resume() {
        this.mClient = this.mApplication.getContentResolver().acquireContentProviderClient("media");
    }

    @Override
    public void pause() {
        if (this.mClient != null) {
            this.mClient.release();
            this.mClient = null;
        }
    }
}
