package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractContainerBox;

public class MediaBox extends AbstractContainerBox {
    public MediaBox() {
        super("mdia");
    }

    public MediaInformationBox getMediaInformationBox() {
        for (Box box : this.boxes) {
            if (box instanceof MediaInformationBox) {
                return box;
            }
        }
        return null;
    }

    public MediaHeaderBox getMediaHeaderBox() {
        for (Box box : this.boxes) {
            if (box instanceof MediaHeaderBox) {
                return box;
            }
        }
        return null;
    }

    public HandlerBox getHandlerBox() {
        for (Box box : this.boxes) {
            if (box instanceof HandlerBox) {
                return box;
            }
        }
        return null;
    }
}
