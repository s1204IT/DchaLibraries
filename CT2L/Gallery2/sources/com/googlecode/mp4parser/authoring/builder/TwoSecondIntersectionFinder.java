package com.googlecode.mp4parser.authoring.builder;

import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import java.util.Arrays;
import java.util.List;

public class TwoSecondIntersectionFinder implements FragmentIntersectionFinder {
    protected long getDuration(Track track) {
        long duration = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            duration += entry.getCount() * entry.getDelta();
        }
        return duration;
    }

    @Override
    public long[] sampleNumbers(Track track, Movie movie) {
        int currentFragment;
        List<TimeToSampleBox.Entry> entries = track.getDecodingTimeEntries();
        double trackLength = 0.0d;
        for (Track thisTrack : movie.getTracks()) {
            double thisTracksLength = getDuration(thisTrack) / thisTrack.getTrackMetaData().getTimescale();
            if (trackLength < thisTracksLength) {
                trackLength = thisTracksLength;
            }
        }
        int fragmentCount = ((int) Math.ceil(trackLength / 2.0d)) - 1;
        if (fragmentCount < 1) {
            fragmentCount = 1;
        }
        long[] fragments = new long[fragmentCount];
        Arrays.fill(fragments, -1L);
        fragments[0] = 1;
        long time = 0;
        int samples = 0;
        for (TimeToSampleBox.Entry entry : entries) {
            int i = 0;
            while (i < entry.getCount() && (currentFragment = ((int) ((time / track.getTrackMetaData().getTimescale()) / 2)) + 1) < fragments.length) {
                fragments[currentFragment] = samples + 1;
                time += entry.getDelta();
                i++;
                samples++;
            }
        }
        long last = samples + 1;
        for (int i2 = fragments.length - 1; i2 >= 0; i2--) {
            if (fragments[i2] == -1) {
                fragments[i2] = last;
            }
            last = fragments[i2];
        }
        return fragments;
    }
}
