package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractContainerBox;

public class TrackBox extends AbstractContainerBox {
    public TrackBox() {
        super("trak");
    }

    public TrackHeaderBox getTrackHeaderBox() {
        for (Box box : this.boxes) {
            if (box instanceof TrackHeaderBox) {
                return (TrackHeaderBox) box;
            }
        }
        return null;
    }

    public SampleTableBox getSampleTableBox() {
        MediaInformationBox minf;
        MediaBox mdia = getMediaBox();
        if (mdia == null || (minf = mdia.getMediaInformationBox()) == null) {
            return null;
        }
        return minf.getSampleTableBox();
    }

    public MediaBox getMediaBox() {
        for (Box box : this.boxes) {
            if (box instanceof MediaBox) {
                return (MediaBox) box;
            }
        }
        return null;
    }
}
