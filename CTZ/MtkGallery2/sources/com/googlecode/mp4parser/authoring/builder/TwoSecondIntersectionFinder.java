package com.googlecode.mp4parser.authoring.builder;

import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import java.util.Arrays;
import java.util.List;

public class TwoSecondIntersectionFinder implements FragmentIntersectionFinder {
    protected long getDuration(Track track) {
        long count = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            count += entry.getCount() * entry.getDelta();
        }
        return count;
    }

    @Override
    public long[] sampleNumbers(Track track, Movie movie) {
        int timescale;
        List<TimeToSampleBox.Entry> decodingTimeEntries = track.getDecodingTimeEntries();
        double d = 0.0d;
        for (Track track2 : movie.getTracks()) {
            double duration = getDuration(track2) / track2.getTrackMetaData().getTimescale();
            if (d < duration) {
                d = duration;
            }
        }
        int iCeil = ((int) Math.ceil(d / 2.0d)) - 1;
        if (iCeil < 1) {
            iCeil = 1;
        }
        long[] jArr = new long[iCeil];
        Arrays.fill(jArr, -1L);
        jArr[0] = 1;
        long delta = 0;
        int i = 0;
        for (TimeToSampleBox.Entry entry : decodingTimeEntries) {
            int i2 = i;
            for (int i3 = 0; i3 < entry.getCount() && (timescale = ((int) ((delta / track.getTrackMetaData().getTimescale()) / 2)) + 1) < jArr.length; i3++) {
                i2++;
                jArr[timescale] = i2;
                delta += entry.getDelta();
            }
            i = i2;
        }
        long j = i + 1;
        for (int length = jArr.length - 1; length >= 0; length--) {
            if (jArr[length] == -1) {
                jArr[length] = j;
            }
            j = jArr[length];
        }
        return jArr;
    }
}
