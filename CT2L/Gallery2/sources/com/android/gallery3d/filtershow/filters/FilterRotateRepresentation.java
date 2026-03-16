package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import java.io.IOException;

public class FilterRotateRepresentation extends FilterRepresentation {
    private static final String TAG = FilterRotateRepresentation.class.getSimpleName();
    Rotation mRotation;

    public enum Rotation {
        ZERO(0),
        NINETY(90),
        ONE_EIGHTY(180),
        TWO_SEVENTY(270);

        private final int mValue;

        Rotation(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }

        public static Rotation fromValue(int value) {
            switch (value) {
                case 0:
                    return ZERO;
                case 90:
                    return NINETY;
                case 180:
                    return ONE_EIGHTY;
                case 270:
                    return TWO_SEVENTY;
                default:
                    return null;
            }
        }
    }

    public FilterRotateRepresentation(Rotation rotation) {
        super("ROTATION");
        setSerializationName("ROTATION");
        setShowParameterValue(false);
        setFilterClass(FilterRotateRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.rotate);
        setEditorId(R.id.imageOnlyEditor);
        setRotation(rotation);
    }

    public FilterRotateRepresentation(FilterRotateRepresentation r) {
        this(r.getRotation());
        setName(r.getName());
    }

    public FilterRotateRepresentation() {
        this(getNil());
    }

    public Rotation getRotation() {
        return this.mRotation;
    }

    public void rotateCW() {
        switch (this.mRotation) {
            case ZERO:
                this.mRotation = Rotation.NINETY;
                break;
            case NINETY:
                this.mRotation = Rotation.ONE_EIGHTY;
                break;
            case ONE_EIGHTY:
                this.mRotation = Rotation.TWO_SEVENTY;
                break;
            case TWO_SEVENTY:
                this.mRotation = Rotation.ZERO;
                break;
        }
    }

    public void setRotation(Rotation rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("Argument to setRotation is null");
        }
        this.mRotation = rotation;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterRotateRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterRotateRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterRotateRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setRotation(((FilterRotateRepresentation) a).getRotation());
    }

    @Override
    public boolean isNil() {
        return this.mRotation == getNil();
    }

    public static Rotation getNil() {
        return Rotation.ZERO;
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("value").value(this.mRotation.value());
        writer.endObject();
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterRotateRepresentation)) {
            return false;
        }
        FilterRotateRepresentation rotate = (FilterRotateRepresentation) rep;
        return rotate.mRotation.value() == this.mRotation.value();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        boolean unset = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("value".equals(name)) {
                Rotation r = Rotation.fromValue(reader.nextInt());
                if (r != null) {
                    setRotation(r);
                    unset = false;
                }
            } else {
                reader.skipValue();
            }
        }
        if (unset) {
            Log.w(TAG, "WARNING: bad value when deserializing ROTATION");
        }
        reader.endObject();
    }
}
