package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractContainerBox;

public class TrackBox extends AbstractContainerBox {
    public TrackBox() {
        super("trak");
    }

    public TrackHeaderBox getTrackHeaderBox() {
        for (Box box : this.boxes) {
            if (box instanceof TrackHeaderBox) {
                return box;
            }
        }
        return null;
    }

    public SampleTableBox getSampleTableBox() {
        MediaInformationBox mediaInformationBox;
        MediaBox mediaBox = getMediaBox();
        if (mediaBox != null && (mediaInformationBox = mediaBox.getMediaInformationBox()) != null) {
            return mediaInformationBox.getSampleTableBox();
        }
        return null;
    }

    public MediaBox getMediaBox() {
        for (Box box : this.boxes) {
            if (box instanceof MediaBox) {
                return box;
            }
        }
        return null;
    }
}
