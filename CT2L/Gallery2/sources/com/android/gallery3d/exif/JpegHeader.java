package com.android.gallery3d.exif;

class JpegHeader {
    public static final boolean isSofMarker(short marker) {
        return (marker < -64 || marker > -49 || marker == -60 || marker == -56 || marker == -52) ? false : true;
    }
}
