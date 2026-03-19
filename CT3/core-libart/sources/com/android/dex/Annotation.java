package com.android.dex;

import com.android.dex.Dex;

public final class Annotation implements Comparable<Annotation> {
    private final Dex dex;
    private final EncodedValue encodedAnnotation;
    private final byte visibility;

    public Annotation(Dex dex, byte visibility, EncodedValue encodedAnnotation) {
        this.dex = dex;
        this.visibility = visibility;
        this.encodedAnnotation = encodedAnnotation;
    }

    public byte getVisibility() {
        return this.visibility;
    }

    public EncodedValueReader getReader() {
        return new EncodedValueReader(this.encodedAnnotation, 29);
    }

    public int getTypeIndex() {
        EncodedValueReader reader = getReader();
        reader.readAnnotation();
        return reader.getAnnotationType();
    }

    public void writeTo(Dex.Section out) {
        out.writeByte(this.visibility);
        this.encodedAnnotation.writeTo(out);
    }

    @Override
    public int compareTo(Annotation other) {
        return this.encodedAnnotation.compareTo(other.encodedAnnotation);
    }

    public String toString() {
        if (this.dex == null) {
            return ((int) this.visibility) + " " + getTypeIndex();
        }
        return ((int) this.visibility) + " " + this.dex.typeNames().get(getTypeIndex());
    }
}
