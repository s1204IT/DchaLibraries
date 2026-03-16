package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import java.io.IOException;

public class FilterStraightenRepresentation extends FilterRepresentation {
    private static final String TAG = FilterStraightenRepresentation.class.getSimpleName();
    float mStraighten;

    public FilterStraightenRepresentation(float straighten) {
        super("STRAIGHTEN");
        setSerializationName("STRAIGHTEN");
        setShowParameterValue(true);
        setFilterClass(FilterStraightenRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.straighten);
        setEditorId(R.id.editorStraighten);
        setStraighten(straighten);
    }

    public FilterStraightenRepresentation(FilterStraightenRepresentation s) {
        this(s.getStraighten());
        setName(s.getName());
    }

    public FilterStraightenRepresentation() {
        this(getNil());
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterStraightenRepresentation)) {
            return false;
        }
        FilterStraightenRepresentation straighten = (FilterStraightenRepresentation) rep;
        return straighten.mStraighten == this.mStraighten;
    }

    public float getStraighten() {
        return this.mStraighten;
    }

    public void setStraighten(float straighten) {
        if (!rangeCheck(straighten)) {
            straighten = Math.min(Math.max(straighten, -45.0f), 45.0f);
        }
        this.mStraighten = straighten;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterStraightenRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setStraighten(((FilterStraightenRepresentation) a).getStraighten());
    }

    @Override
    public boolean isNil() {
        return this.mStraighten == getNil();
    }

    public static float getNil() {
        return 0.0f;
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("value").value(this.mStraighten);
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        boolean unset = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("value".equals(name)) {
                float s = (float) reader.nextDouble();
                if (rangeCheck(s)) {
                    setStraighten(s);
                    unset = false;
                }
            } else {
                reader.skipValue();
            }
        }
        if (unset) {
            Log.w(TAG, "WARNING: bad value when deserializing STRAIGHTEN");
        }
        reader.endObject();
    }

    private boolean rangeCheck(double s) {
        return s >= -45.0d && s <= 45.0d;
    }
}
