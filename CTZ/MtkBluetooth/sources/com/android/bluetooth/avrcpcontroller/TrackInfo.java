package com.android.bluetooth.avrcpcontroller;

import android.media.MediaMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class TrackInfo {
    private static final int MEDIA_ATTRIBUTE_ALBUM_NAME = 3;
    private static final int MEDIA_ATTRIBUTE_ARTIST_NAME = 2;
    private static final int MEDIA_ATTRIBUTE_GENRE = 6;
    private static final int MEDIA_ATTRIBUTE_PLAYING_TIME = 7;
    private static final int MEDIA_ATTRIBUTE_TITLE = 1;
    private static final int MEDIA_ATTRIBUTE_TOTAL_TRACK_NUMBER = 5;
    private static final int MEDIA_ATTRIBUTE_TRACK_NUMBER = 4;
    private static final String TAG = "AvrcpTrackInfo";
    private static final int TOTAL_TRACKS_INVALID = -1;
    private static final int TOTAL_TRACK_TIME_INVALID = -1;
    private static final int TRACK_NUM_INVALID = -1;
    private static final String UNPOPULATED_ATTRIBUTE = "";
    private static final boolean VDBG = false;
    private final String mAlbumTitle;
    private final String mArtistName;
    private final String mGenre;
    private final long mTotalTracks;
    private final long mTrackLen;
    private final long mTrackNum;
    private final String mTrackTitle;

    TrackInfo() {
        this(new ArrayList(), new ArrayList());
    }

    TrackInfo(List<Integer> list, List<String> list2) {
        HashMap map = new HashMap();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), list2.get(i));
        }
        this.mTrackTitle = (String) map.getOrDefault(1, UNPOPULATED_ATTRIBUTE);
        this.mArtistName = (String) map.getOrDefault(2, UNPOPULATED_ATTRIBUTE);
        this.mAlbumTitle = (String) map.getOrDefault(3, UNPOPULATED_ATTRIBUTE);
        String str = (String) map.get(4);
        long jLongValue = -1;
        this.mTrackNum = (str == null || str.isEmpty()) ? -1L : Long.valueOf(str).longValue();
        String str2 = (String) map.get(5);
        this.mTotalTracks = (str2 == null || str2.isEmpty()) ? -1L : Long.valueOf(str2).longValue();
        this.mGenre = (String) map.getOrDefault(6, UNPOPULATED_ATTRIBUTE);
        String str3 = (String) map.get(7);
        if (str3 != null && !str3.isEmpty()) {
            jLongValue = Long.valueOf(str3).longValue();
        }
        this.mTrackLen = jLongValue;
    }

    public String toString() {
        return "Metadata [artist=" + this.mArtistName + " trackTitle= " + this.mTrackTitle + " albumTitle= " + this.mAlbumTitle + " genre= " + this.mGenre + " trackNum= " + Long.toString(this.mTrackNum) + " track_len : " + Long.toString(this.mTrackLen) + " TotalTracks " + Long.toString(this.mTotalTracks) + "]";
    }

    public MediaMetadata getMediaMetaData() {
        MediaMetadata.Builder builder = new MediaMetadata.Builder();
        builder.putString("android.media.metadata.ARTIST", this.mArtistName);
        builder.putString("android.media.metadata.TITLE", this.mTrackTitle);
        builder.putString("android.media.metadata.ALBUM", this.mAlbumTitle);
        builder.putString("android.media.metadata.GENRE", this.mGenre);
        builder.putLong("android.media.metadata.TRACK_NUMBER", this.mTrackNum);
        builder.putLong("android.media.metadata.NUM_TRACKS", this.mTotalTracks);
        builder.putLong("android.media.metadata.DURATION", this.mTrackLen);
        return builder.build();
    }

    public String displayMetaData() {
        MediaMetadata mediaMetaData = getMediaMetaData();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(mediaMetaData.getDescription().toString() + " ");
        if (mediaMetaData.containsKey("android.media.metadata.GENRE")) {
            stringBuffer.append(mediaMetaData.getString("android.media.metadata.GENRE") + " ");
        }
        if (mediaMetaData.containsKey("android.media.metadata.MEDIA_ID")) {
            stringBuffer.append(mediaMetaData.getString("android.media.metadata.MEDIA_ID") + " ");
        }
        if (mediaMetaData.containsKey("android.media.metadata.TRACK_NUMBER")) {
            stringBuffer.append(Long.toString(mediaMetaData.getLong("android.media.metadata.TRACK_NUMBER")) + " ");
        }
        if (mediaMetaData.containsKey("android.media.metadata.NUM_TRACKS")) {
            stringBuffer.append(Long.toString(mediaMetaData.getLong("android.media.metadata.NUM_TRACKS")) + " ");
        }
        if (mediaMetaData.containsKey("android.media.metadata.TRACK_NUMBER")) {
            stringBuffer.append(Long.toString(mediaMetaData.getLong("android.media.metadata.DURATION")) + " ");
        }
        if (mediaMetaData.containsKey("android.media.metadata.TRACK_NUMBER")) {
            stringBuffer.append(Long.toString(mediaMetaData.getLong("android.media.metadata.DURATION")) + " ");
        }
        return stringBuffer.toString();
    }
}
