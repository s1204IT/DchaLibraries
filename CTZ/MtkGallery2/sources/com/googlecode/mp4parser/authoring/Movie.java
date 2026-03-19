package com.googlecode.mp4parser.authoring;

import java.util.LinkedList;
import java.util.List;

public class Movie {
    List<Track> tracks = new LinkedList();

    public List<Track> getTracks() {
        return this.tracks;
    }

    public void setTracks(List<Track> list) {
        this.tracks = list;
    }

    public void addTrack(Track track) {
        if (getTrackByTrackId(track.getTrackMetaData().getTrackId()) != null) {
            track.getTrackMetaData().setTrackId(getNextTrackId());
        }
        this.tracks.add(track);
    }

    public String toString() {
        String str = "Movie{ ";
        for (Track track : this.tracks) {
            str = str + "track_" + track.getTrackMetaData().getTrackId() + " (" + track.getHandler() + ") ";
        }
        return str + '}';
    }

    public long getNextTrackId() {
        long trackId = 0;
        for (Track track : this.tracks) {
            if (trackId < track.getTrackMetaData().getTrackId()) {
                trackId = track.getTrackMetaData().getTrackId();
            }
        }
        return trackId + 1;
    }

    public Track getTrackByTrackId(long j) {
        for (Track track : this.tracks) {
            if (track.getTrackMetaData().getTrackId() == j) {
                return track;
            }
        }
        return null;
    }
}
