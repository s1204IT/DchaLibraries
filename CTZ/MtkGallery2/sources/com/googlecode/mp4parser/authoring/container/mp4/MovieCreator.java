package com.googlecode.mp4parser.authoring.container.mp4;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

public class MovieCreator {
    public static Movie build(ReadableByteChannel readableByteChannel) throws IOException {
        IsoFile isoFile = new IsoFile(readableByteChannel);
        Movie movie = new Movie();
        Iterator it = isoFile.getMovieBox().getBoxes(TrackBox.class).iterator();
        while (it.hasNext()) {
            movie.addTrack(new Mp4TrackImpl((TrackBox) it.next()));
        }
        return movie;
    }
}
