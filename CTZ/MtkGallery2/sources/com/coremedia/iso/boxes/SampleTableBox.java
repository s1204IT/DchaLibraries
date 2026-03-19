package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractContainerBox;

public class SampleTableBox extends AbstractContainerBox {
    public SampleTableBox() {
        super("stbl");
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        for (Box box : this.boxes) {
            if (box instanceof SampleDescriptionBox) {
                return box;
            }
        }
        return null;
    }

    public SampleSizeBox getSampleSizeBox() {
        for (Box box : this.boxes) {
            if (box instanceof SampleSizeBox) {
                return box;
            }
        }
        return null;
    }

    public SampleToChunkBox getSampleToChunkBox() {
        for (Box box : this.boxes) {
            if (box instanceof SampleToChunkBox) {
                return box;
            }
        }
        return null;
    }

    public ChunkOffsetBox getChunkOffsetBox() {
        for (Box box : this.boxes) {
            if (box instanceof ChunkOffsetBox) {
                return box;
            }
        }
        return null;
    }

    public TimeToSampleBox getTimeToSampleBox() {
        for (Box box : this.boxes) {
            if (box instanceof TimeToSampleBox) {
                return box;
            }
        }
        return null;
    }

    public SyncSampleBox getSyncSampleBox() {
        for (Box box : this.boxes) {
            if (box instanceof SyncSampleBox) {
                return box;
            }
        }
        return null;
    }

    public CompositionTimeToSample getCompositionTimeToSample() {
        for (Box box : this.boxes) {
            if (box instanceof CompositionTimeToSample) {
                return box;
            }
        }
        return null;
    }

    public SampleDependencyTypeBox getSampleDependencyTypeBox() {
        for (Box box : this.boxes) {
            if (box instanceof SampleDependencyTypeBox) {
                return box;
            }
        }
        return null;
    }
}
