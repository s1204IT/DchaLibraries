package com.googlecode.mp4parser.authoring.container.mp4;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

public class MovieCreator {
    public static Movie build(ReadableByteChannel channel) throws IOException {
        IsoFile isoFile = new IsoFile(channel);
        Movie m = new Movie();
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        for (TrackBox trackBox : trackBoxes) {
            m.addTrack(new Mp4TrackImpl(trackBox));
        }
        return m;
    }
}
