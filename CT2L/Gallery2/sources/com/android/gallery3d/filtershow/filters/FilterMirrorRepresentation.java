package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import java.io.IOException;

public class FilterMirrorRepresentation extends FilterRepresentation {
    private static final String TAG = FilterMirrorRepresentation.class.getSimpleName();
    Mirror mMirror;

    public enum Mirror {
        NONE('N'),
        VERTICAL('V'),
        HORIZONTAL('H'),
        BOTH('B');

        char mValue;

        Mirror(char value) {
            this.mValue = value;
        }

        public char value() {
            return this.mValue;
        }

        public static Mirror fromValue(char value) {
            switch (value) {
                case 'B':
                    return BOTH;
                case 'H':
                    return HORIZONTAL;
                case 'N':
                    return NONE;
                case 'V':
                    return VERTICAL;
                default:
                    return null;
            }
        }
    }

    public FilterMirrorRepresentation(Mirror mirror) {
        super("MIRROR");
        setSerializationName("MIRROR");
        setShowParameterValue(false);
        setFilterClass(FilterMirrorRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.mirror);
        setEditorId(R.id.imageOnlyEditor);
        setMirror(mirror);
    }

    public FilterMirrorRepresentation(FilterMirrorRepresentation m) {
        this(m.getMirror());
        setName(m.getName());
    }

    public FilterMirrorRepresentation() {
        this(getNil());
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterMirrorRepresentation)) {
            return false;
        }
        FilterMirrorRepresentation mirror = (FilterMirrorRepresentation) rep;
        return this.mMirror == mirror.mMirror;
    }

    public Mirror getMirror() {
        return this.mMirror;
    }

    public void setMirror(Mirror mirror) {
        if (mirror == null) {
            throw new IllegalArgumentException("Argument to setMirror is null");
        }
        this.mMirror = mirror;
    }

    public boolean isHorizontal() {
        return this.mMirror == Mirror.BOTH || this.mMirror == Mirror.HORIZONTAL;
    }

    public boolean isVertical() {
        return this.mMirror == Mirror.BOTH || this.mMirror == Mirror.VERTICAL;
    }

    public void cycle() {
        switch (this.mMirror) {
            case NONE:
                this.mMirror = Mirror.HORIZONTAL;
                break;
            case HORIZONTAL:
                this.mMirror = Mirror.BOTH;
                break;
            case BOTH:
                this.mMirror = Mirror.VERTICAL;
                break;
            case VERTICAL:
                this.mMirror = Mirror.NONE;
                break;
        }
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterMirrorRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterMirrorRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterMirrorRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setMirror(((FilterMirrorRepresentation) a).getMirror());
    }

    @Override
    public boolean isNil() {
        return this.mMirror == getNil();
    }

    public static Mirror getNil() {
        return Mirror.NONE;
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("value").value(this.mMirror.value());
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        boolean unset = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("value".equals(name)) {
                Mirror r = Mirror.fromValue((char) reader.nextInt());
                if (r != null) {
                    setMirror(r);
                    unset = false;
                }
            } else {
                reader.skipValue();
            }
        }
        if (unset) {
            Log.w(TAG, "WARNING: bad value when deserializing MIRROR");
        }
        reader.endObject();
    }
}
