package com.android.gallery3d.data;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import android.support.v4.app.FragmentManagerImpl;
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

    public LocalSource(GalleryApp context) {
        super("local");
        this.mUriMatcher = new UriMatcher(-1);
        this.mApplication = context;
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
        GalleryApp app = this.mApplication;
        switch (this.mMatcher.match(path)) {
            case 0:
            case 1:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return new LocalAlbumSet(path, this.mApplication);
            case 2:
                return new LocalAlbum(path, app, this.mMatcher.getIntVar(0), true);
            case 3:
                return new LocalAlbum(path, app, this.mMatcher.getIntVar(0), false);
            case 4:
                return new LocalImage(path, this.mApplication, this.mMatcher.getIntVar(0));
            case 5:
                return new LocalVideo(path, this.mApplication, this.mMatcher.getIntVar(0));
            case 7:
                int bucketId = this.mMatcher.getIntVar(0);
                DataManager dataManager = app.getDataManager();
                MediaSet imageSet = (MediaSet) dataManager.getMediaObject(LocalAlbumSet.PATH_IMAGE.getChild(bucketId));
                MediaSet videoSet = (MediaSet) dataManager.getMediaObject(LocalAlbumSet.PATH_VIDEO.getChild(bucketId));
                Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                return new LocalMergeAlbum(path, comp, new MediaSet[]{imageSet, videoSet}, bucketId);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    private static int getMediaType(String type, int defaultType) {
        if (type != null) {
            try {
                int value = Integer.parseInt(type);
                return (value & 5) != 0 ? value : defaultType;
            } catch (NumberFormatException e) {
                Log.w("LocalSource", "invalid type: " + type, e);
                return defaultType;
            }
        }
        return defaultType;
    }

    private Path getAlbumPath(Uri uri, int defaultType) {
        int mediaType = getMediaType(uri.getQueryParameter("mediaTypes"), defaultType);
        String bucketId = uri.getQueryParameter("bucketId");
        try {
            int id = Integer.parseInt(bucketId);
            switch (mediaType) {
                case 1:
                    return Path.fromString("/local/image").getChild(id);
                case 2:
                case 3:
                default:
                    return Path.fromString("/local/all").getChild(id);
                case 4:
                    return Path.fromString("/local/video").getChild(id);
            }
        } catch (NumberFormatException e) {
            Log.w("LocalSource", "invalid bucket id: " + bucketId, e);
            return null;
        }
    }

    @Override
    public Path findPathByUri(Uri uri, String type) {
        Path albumPath = null;
        try {
            switch (this.mUriMatcher.match(uri)) {
                case 2:
                    albumPath = getAlbumPath(uri, 1);
                    break;
                case 3:
                    albumPath = getAlbumPath(uri, 4);
                    break;
                case 4:
                    long id = ContentUris.parseId(uri);
                    if (id >= 0) {
                        albumPath = LocalImage.ITEM_PATH.getChild(id);
                    }
                    break;
                case 5:
                    long id2 = ContentUris.parseId(uri);
                    if (id2 >= 0) {
                        albumPath = LocalVideo.ITEM_PATH.getChild(id2);
                    }
                    break;
                case 7:
                    albumPath = getAlbumPath(uri, 0);
                    break;
            }
        } catch (NumberFormatException e) {
            Log.w("LocalSource", "uri: " + uri.toString(), e);
        }
        return albumPath;
    }

    @Override
    public Path getDefaultSetOf(Path item) {
        MediaObject object = this.mApplication.getDataManager().getMediaObject(item);
        if (object instanceof LocalMediaItem) {
            return Path.fromString("/local/all").getChild(String.valueOf(((LocalMediaItem) object).getBucketId()));
        }
        return null;
    }

    @Override
    public void mapMediaItems(ArrayList<MediaSource.PathId> list, MediaSet.ItemConsumer consumer) {
        ArrayList<MediaSource.PathId> imageList = new ArrayList<>();
        ArrayList<MediaSource.PathId> videoList = new ArrayList<>();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            MediaSource.PathId pid = list.get(i);
            Path parent = pid.path.getParent();
            if (parent == LocalImage.ITEM_PATH) {
                imageList.add(pid);
            } else if (parent == LocalVideo.ITEM_PATH) {
                videoList.add(pid);
            }
        }
        processMapMediaItems(imageList, consumer, true);
        processMapMediaItems(videoList, consumer, false);
    }

    private void processMapMediaItems(ArrayList<MediaSource.PathId> list, MediaSet.ItemConsumer consumer, boolean isImage) {
        Collections.sort(list, sIdComparator);
        int n = list.size();
        int i = 0;
        while (i < n) {
            MediaSource.PathId pid = list.get(i);
            ArrayList<Integer> ids = new ArrayList<>();
            int startId = Integer.parseInt(pid.path.getSuffix());
            ids.add(Integer.valueOf(startId));
            int j = i + 1;
            while (j < n) {
                MediaSource.PathId pid2 = list.get(j);
                int curId = Integer.parseInt(pid2.path.getSuffix());
                if (curId - startId >= 500) {
                    break;
                }
                ids.add(Integer.valueOf(curId));
                j++;
            }
            MediaItem[] items = LocalAlbum.getMediaItemById(this.mApplication, isImage, ids);
            for (int k = i; k < j; k++) {
                MediaSource.PathId pid22 = list.get(k);
                consumer.consume(pid22.id, items[k - i]);
            }
            i = j;
        }
    }

    private static class IdComparator implements Comparator<MediaSource.PathId> {
        private IdComparator() {
        }

        @Override
        public int compare(MediaSource.PathId p1, MediaSource.PathId p2) {
            String s1 = p1.path.getSuffix();
            String s2 = p2.path.getSuffix();
            int len1 = s1.length();
            int len2 = s2.length();
            if (len1 < len2) {
                return -1;
            }
            if (len1 > len2) {
                return 1;
            }
            return s1.compareTo(s2);
        }
    }

    @Override
    public void resume() {
        this.mClient = this.mApplication.getContentResolver().acquireContentProviderClient("media");
    }

    @Override
    public void pause() {
        this.mClient.release();
        this.mClient = null;
    }
}
