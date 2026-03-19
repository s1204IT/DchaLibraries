package com.googlecode.mp4parser.boxes.piff;

import com.googlecode.mp4parser.boxes.AbstractTrackEncryptionBox;

public class PiffTrackEncryptionBox extends AbstractTrackEncryptionBox {
    public PiffTrackEncryptionBox() {
        super("uuid");
    }

    @Override
    public byte[] getUserType() {
        return new byte[]{-119, 116, -37, -50, 123, -25, 76, 81, -124, -7, 113, 72, -7, -120, 37, 84};
    }

    @Override
    public int getFlags() {
        return 0;
    }
}
