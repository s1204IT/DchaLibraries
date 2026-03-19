package com.android.bluetooth.avrcp;

import android.media.session.PlaybackState;
import java.util.List;
import java.util.Objects;

class MediaData {
    public Metadata metadata;
    public List<Metadata> queue;
    public PlaybackState state;

    MediaData(Metadata metadata, PlaybackState playbackState, List<Metadata> list) {
        this.metadata = metadata;
        this.state = playbackState;
        this.queue = list;
    }

    public boolean equals(Object obj) {
        if (obj == 0 || !(obj instanceof MediaData) || !MediaPlayerWrapper.playstateEquals(this.state, obj.state) || !Objects.equals(this.metadata, obj.metadata) || !Objects.equals(this.queue, obj.queue)) {
            return false;
        }
        return true;
    }
}
