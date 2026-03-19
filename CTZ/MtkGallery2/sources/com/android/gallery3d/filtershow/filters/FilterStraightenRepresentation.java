package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
import java.io.IOException;

public class FilterStraightenRepresentation extends FilterRepresentation {
    private static final String TAG = FilterStraightenRepresentation.class.getSimpleName();
    float mStraighten;

    public FilterStraightenRepresentation(float f) {
        super("STRAIGHTEN");
        setSerializationName("STRAIGHTEN");
        setShowParameterValue(true);
        setFilterClass(FilterStraightenRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.straighten);
        setEditorId(R.id.editorStraighten);
        setStraighten(f);
    }

    public FilterStraightenRepresentation(FilterStraightenRepresentation filterStraightenRepresentation) {
        this(filterStraightenRepresentation.getStraighten());
        setName(filterStraightenRepresentation.getName());
    }

    public FilterStraightenRepresentation() {
        this(getNil());
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return (filterRepresentation instanceof FilterStraightenRepresentation) && ((FilterStraightenRepresentation) filterRepresentation).mStraighten == this.mStraighten;
    }

    public float getStraighten() {
        return this.mStraighten;
    }

    public void setStraighten(float f) {
        if (!rangeCheck(f)) {
            f = Math.min(Math.max(f, -45.0f), 45.0f);
        }
        this.mStraighten = f;
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
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setStraighten(filterRepresentation.getStraighten());
    }

    @Override
    public boolean isNil() {
        return this.mStraighten == getNil();
    }

    public static float getNil() {
        return 0.0f;
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("value").value(this.mStraighten);
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        boolean z = true;
        while (jsonReader.hasNext()) {
            if ("value".equals(jsonReader.nextName())) {
                float fNextDouble = (float) jsonReader.nextDouble();
                if (rangeCheck(fNextDouble)) {
                    setStraighten(fNextDouble);
                    z = false;
                }
            } else {
                jsonReader.skipValue();
            }
        }
        if (z) {
            Log.w(TAG, "WARNING: bad value when deserializing STRAIGHTEN");
        }
        jsonReader.endObject();
    }

    private boolean rangeCheck(double d) {
        if (d < -45.0d || d > 45.0d) {
            return false;
        }
        return true;
    }
}
