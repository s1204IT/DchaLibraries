package com.android.bluetooth.avrcp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Util {
    private static final String GPM_KEY = "com.google.android.music.mediasession.music_metadata";
    private static final int MAX_ATTRID_TITLE_ALBUM_ARTIST_LENGTH = 155;
    private static final int MAX_BROWSE_ATTRID_TITLE_ALBUM_ARTIST_LENGTH = 60;
    public static final String NOW_PLAYING_PREFIX = "NowPlayingId";
    public static String TAG = "NewAvrcpUtil";
    public static boolean DEBUG = false;

    Util() {
    }

    public static final Metadata empty_data() {
        Metadata metadata = new Metadata();
        metadata.mediaId = "Not Provided";
        metadata.title = "Not Provided";
        metadata.artist = "";
        metadata.album = "";
        metadata.genre = "";
        metadata.trackNum = "1";
        metadata.numTracks = "1";
        metadata.duration = "0";
        return metadata;
    }

    public static Metadata bundleToMetadata(Bundle bundle, int i) {
        if (bundle == null) {
            return empty_data();
        }
        Metadata metadata = new Metadata();
        metadata.title = bundle.getString("android.media.metadata.TITLE", "Not Provided");
        if (metadata.title.length() > i) {
            Log.d(TAG, "title too long " + metadata.title.length());
            metadata.title = metadata.title.substring(0, i);
        }
        metadata.artist = bundle.getString("android.media.metadata.ARTIST", "");
        if (metadata.artist.length() > i) {
            Log.d(TAG, "artist too long " + metadata.artist.length());
            metadata.artist = metadata.artist.substring(0, i);
        }
        metadata.album = bundle.getString("android.media.metadata.ALBUM", "");
        if (metadata.album.length() > i) {
            Log.d(TAG, "album too long " + metadata.album.length());
            metadata.album = metadata.album.substring(0, i);
        }
        metadata.trackNum = "" + bundle.getLong("android.media.metadata.TRACK_NUMBER", 1L);
        metadata.numTracks = "" + bundle.getLong("android.media.metadata.NUM_TRACKS", 1L);
        metadata.genre = bundle.getString("android.media.metadata.GENRE", "");
        if (metadata.genre.length() > i) {
            Log.d(TAG, "genre too long " + metadata.genre.length());
            metadata.genre = metadata.genre.substring(0, i);
        }
        metadata.duration = "" + bundle.getLong("android.media.metadata.DURATION", 0L);
        return metadata;
    }

    public static Bundle descriptionToBundle(MediaDescription mediaDescription) {
        Bundle bundle = new Bundle();
        if (mediaDescription == null) {
            return bundle;
        }
        if (mediaDescription.getTitle() != null) {
            bundle.putString("android.media.metadata.TITLE", mediaDescription.getTitle().toString());
        }
        if (mediaDescription.getSubtitle() != null) {
            bundle.putString("android.media.metadata.ARTIST", mediaDescription.getSubtitle().toString());
        }
        if (mediaDescription.getDescription() != null) {
            bundle.putString("android.media.metadata.ALBUM", mediaDescription.getDescription().toString());
        }
        if (mediaDescription.getExtras() != null) {
            bundle.putAll(mediaDescription.getExtras());
        }
        if (bundle.containsKey(GPM_KEY)) {
            if (DEBUG) {
                Log.d(TAG, "MediaDescription contains GPM data");
            }
            bundle.putAll(mediaMetadataToBundle((MediaMetadata) bundle.get(GPM_KEY)));
        }
        return bundle;
    }

    public static Bundle mediaMetadataToBundle(MediaMetadata mediaMetadata) {
        Bundle bundle = new Bundle();
        if (mediaMetadata == null) {
            return bundle;
        }
        if (mediaMetadata.containsKey("android.media.metadata.TITLE")) {
            bundle.putString("android.media.metadata.TITLE", mediaMetadata.getString("android.media.metadata.TITLE"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.ARTIST")) {
            bundle.putString("android.media.metadata.ARTIST", mediaMetadata.getString("android.media.metadata.ARTIST"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.ALBUM")) {
            bundle.putString("android.media.metadata.ALBUM", mediaMetadata.getString("android.media.metadata.ALBUM"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.TRACK_NUMBER")) {
            bundle.putLong("android.media.metadata.TRACK_NUMBER", mediaMetadata.getLong("android.media.metadata.TRACK_NUMBER"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.NUM_TRACKS")) {
            bundle.putLong("android.media.metadata.NUM_TRACKS", mediaMetadata.getLong("android.media.metadata.NUM_TRACKS"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.GENRE")) {
            bundle.putString("android.media.metadata.GENRE", mediaMetadata.getString("android.media.metadata.GENRE"));
        }
        if (mediaMetadata.containsKey("android.media.metadata.DURATION")) {
            bundle.putLong("android.media.metadata.DURATION", mediaMetadata.getLong("android.media.metadata.DURATION"));
        }
        return bundle;
    }

    public static Metadata toMetadata(MediaSession.QueueItem queueItem) {
        if (queueItem == null) {
            return empty_data();
        }
        Bundle bundleDescriptionToBundle = descriptionToBundle(queueItem.getDescription());
        if (DEBUG) {
            for (String str : bundleDescriptionToBundle.keySet()) {
                Log.d(TAG, "toMetadata: QueueItem: ContainsKey: " + str);
            }
        }
        Metadata metadataBundleToMetadata = bundleToMetadata(bundleDescriptionToBundle, 60);
        metadataBundleToMetadata.mediaId = NOW_PLAYING_PREFIX + queueItem.getQueueId();
        return metadataBundleToMetadata;
    }

    public static Metadata toMetadata(MediaMetadata mediaMetadata) {
        if (mediaMetadata == null) {
            return empty_data();
        }
        mediaMetadata.getDescription();
        Bundle bundleMediaMetadataToBundle = mediaMetadataToBundle(mediaMetadata);
        Bundle bundleDescriptionToBundle = descriptionToBundle(mediaMetadata.getDescription());
        bundleDescriptionToBundle.putAll(bundleMediaMetadataToBundle);
        if (DEBUG) {
            for (String str : bundleDescriptionToBundle.keySet()) {
                Log.d(TAG, "toMetadata: MediaMetadata: ContainsKey: " + str);
            }
        }
        Metadata metadataBundleToMetadata = bundleToMetadata(bundleDescriptionToBundle, 155);
        metadataBundleToMetadata.mediaId = "currsong";
        return metadataBundleToMetadata;
    }

    public static Metadata toMetadata(MediaBrowser.MediaItem mediaItem) {
        if (mediaItem == null) {
            return empty_data();
        }
        Bundle bundleDescriptionToBundle = descriptionToBundle(mediaItem.getDescription());
        if (DEBUG) {
            for (String str : bundleDescriptionToBundle.keySet()) {
                Log.d(TAG, "toMetadata: MediaItem: ContainsKey: " + str);
            }
        }
        Metadata metadataBundleToMetadata = bundleToMetadata(bundleDescriptionToBundle, 155);
        metadataBundleToMetadata.mediaId = mediaItem.getMediaId();
        return metadataBundleToMetadata;
    }

    public static Metadata toMetadata(MediaBrowser.MediaItem mediaItem, boolean z) {
        if (mediaItem == null) {
            return empty_data();
        }
        Bundle bundleDescriptionToBundle = descriptionToBundle(mediaItem.getDescription());
        if (DEBUG) {
            for (String str : bundleDescriptionToBundle.keySet()) {
                Log.d(TAG, "toMetadata: MediaItem: ContainsKey: " + str);
            }
        }
        int i = 60;
        if (z) {
            i = 155;
        }
        Metadata metadataBundleToMetadata = bundleToMetadata(bundleDescriptionToBundle, i);
        metadataBundleToMetadata.mediaId = mediaItem.getMediaId();
        return metadataBundleToMetadata;
    }

    public static List<Metadata> toMetadataList(List<MediaSession.QueueItem> list) {
        ArrayList arrayList = new ArrayList();
        if (list == null) {
            return arrayList;
        }
        int i = 0;
        while (i < list.size()) {
            Metadata metadata = toMetadata(list.get(i));
            StringBuilder sb = new StringBuilder();
            sb.append("");
            i++;
            sb.append(i);
            metadata.trackNum = sb.toString();
            metadata.numTracks = "" + list.size();
            arrayList.add(metadata);
        }
        return arrayList;
    }

    public static List<ListItem> cloneList(List<ListItem> list) {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<ListItem> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().m7clone());
        }
        return arrayList;
    }

    public static String getDisplayName(Context context, String str) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(str, 0)).toString();
        } catch (Exception e) {
            Log.w(TAG, "Name Not Found using package name: " + str);
            return str;
        }
    }
}
