package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
import java.io.IOException;

public class FilterCropRepresentation extends FilterRepresentation {
    public static final String[] BOUNDS = {"C0", "C1", "C2", "C3"};
    private static final String TAG = FilterCropRepresentation.class.getSimpleName();
    private static final RectF sNilRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    RectF mCrop;

    public FilterCropRepresentation(RectF rectF) {
        super("CROP");
        this.mCrop = getNil();
        setSerializationName("CROP");
        setShowParameterValue(true);
        setFilterClass(FilterCropRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.crop);
        setEditorId(R.id.editorCrop);
        setCrop(rectF);
    }

    public FilterCropRepresentation(FilterCropRepresentation filterCropRepresentation) {
        this(filterCropRepresentation.mCrop);
        setName(filterCropRepresentation.getName());
    }

    public FilterCropRepresentation() {
        this(sNilRect);
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterCropRepresentation)) {
            return false;
        }
        FilterCropRepresentation filterCropRepresentation = (FilterCropRepresentation) filterRepresentation;
        return this.mCrop.bottom == filterCropRepresentation.mCrop.bottom && this.mCrop.left == filterCropRepresentation.mCrop.left && this.mCrop.right == filterCropRepresentation.mCrop.right && this.mCrop.top == filterCropRepresentation.mCrop.top;
    }

    public RectF getCrop() {
        return new RectF(this.mCrop);
    }

    public void getCrop(RectF rectF) {
        rectF.set(this.mCrop);
    }

    public void setCrop(RectF rectF) {
        if (rectF == null) {
            throw new IllegalArgumentException("Argument to setCrop is null");
        }
        this.mCrop.set(rectF);
    }

    public static void findScaledCrop(RectF rectF, int i, int i2) {
        float f = i;
        rectF.left *= f;
        float f2 = i2;
        rectF.top *= f2;
        rectF.right *= f;
        rectF.bottom *= f2;
    }

    public static void findNormalizedCrop(RectF rectF, int i, int i2) {
        float f = i;
        rectF.left /= f;
        float f2 = i2;
        rectF.top /= f2;
        rectF.right /= f;
        rectF.bottom /= f2;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterCropRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setCrop(filterRepresentation.mCrop);
    }

    @Override
    public boolean isNil() {
        float fAbs = Math.abs(this.mCrop.left - sNilRect.left);
        float fAbs2 = Math.abs(this.mCrop.right - sNilRect.right);
        float fAbs3 = Math.abs(this.mCrop.top - sNilRect.top);
        float fAbs4 = Math.abs(this.mCrop.bottom - sNilRect.bottom);
        if (fAbs < 0.001f && fAbs2 < 0.001f && fAbs3 < 0.001f && fAbs4 < 0.001f) {
            return true;
        }
        Log.d("CROP", "mCrop = " + this.mCrop.toShortString());
        return false;
    }

    public static RectF getNil() {
        return new RectF(sNilRect);
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(BOUNDS[0]).value(this.mCrop.left);
        jsonWriter.name(BOUNDS[1]).value(this.mCrop.top);
        jsonWriter.name(BOUNDS[2]).value(this.mCrop.right);
        jsonWriter.name(BOUNDS[3]).value(this.mCrop.bottom);
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (BOUNDS[0].equals(strNextName)) {
                this.mCrop.left = (float) jsonReader.nextDouble();
            } else if (BOUNDS[1].equals(strNextName)) {
                this.mCrop.top = (float) jsonReader.nextDouble();
            } else if (BOUNDS[2].equals(strNextName)) {
                this.mCrop.right = (float) jsonReader.nextDouble();
            } else if (BOUNDS[3].equals(strNextName)) {
                this.mCrop.bottom = (float) jsonReader.nextDouble();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }
}
