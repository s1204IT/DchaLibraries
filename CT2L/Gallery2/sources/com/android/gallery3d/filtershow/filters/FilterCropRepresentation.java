package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import java.io.IOException;

public class FilterCropRepresentation extends FilterRepresentation {
    public static final String[] BOUNDS = {"C0", "C1", "C2", "C3"};
    private static final String TAG = FilterCropRepresentation.class.getSimpleName();
    private static final RectF sNilRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    RectF mCrop;

    public FilterCropRepresentation(RectF crop) {
        super("CROP");
        this.mCrop = getNil();
        setSerializationName("CROP");
        setShowParameterValue(true);
        setFilterClass(FilterCropRepresentation.class);
        setFilterType(7);
        setSupportsPartialRendering(true);
        setTextId(R.string.crop);
        setEditorId(R.id.editorCrop);
        setCrop(crop);
    }

    public FilterCropRepresentation(FilterCropRepresentation m) {
        this(m.mCrop);
        setName(m.getName());
    }

    public FilterCropRepresentation() {
        this(sNilRect);
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterCropRepresentation)) {
            return false;
        }
        FilterCropRepresentation crop = (FilterCropRepresentation) rep;
        return this.mCrop.bottom == crop.mCrop.bottom && this.mCrop.left == crop.mCrop.left && this.mCrop.right == crop.mCrop.right && this.mCrop.top == crop.mCrop.top;
    }

    public RectF getCrop() {
        return new RectF(this.mCrop);
    }

    public void getCrop(RectF r) {
        r.set(this.mCrop);
    }

    public void setCrop(RectF crop) {
        if (crop == null) {
            throw new IllegalArgumentException("Argument to setCrop is null");
        }
        this.mCrop.set(crop);
    }

    public static void findScaledCrop(RectF crop, int bitmapWidth, int bitmapHeight) {
        crop.left *= bitmapWidth;
        crop.top *= bitmapHeight;
        crop.right *= bitmapWidth;
        crop.bottom *= bitmapHeight;
    }

    public static void findNormalizedCrop(RectF crop, int bitmapWidth, int bitmapHeight) {
        crop.left /= bitmapWidth;
        crop.top /= bitmapHeight;
        crop.right /= bitmapWidth;
        crop.bottom /= bitmapHeight;
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
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setCrop(((FilterCropRepresentation) a).mCrop);
    }

    @Override
    public boolean isNil() {
        return this.mCrop.equals(sNilRect);
    }

    public static RectF getNil() {
        return new RectF(sNilRect);
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(BOUNDS[0]).value(this.mCrop.left);
        writer.name(BOUNDS[1]).value(this.mCrop.top);
        writer.name(BOUNDS[2]).value(this.mCrop.right);
        writer.name(BOUNDS[3]).value(this.mCrop.bottom);
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (BOUNDS[0].equals(name)) {
                this.mCrop.left = (float) reader.nextDouble();
            } else if (BOUNDS[1].equals(name)) {
                this.mCrop.top = (float) reader.nextDouble();
            } else if (BOUNDS[2].equals(name)) {
                this.mCrop.right = (float) reader.nextDouble();
            } else if (BOUNDS[3].equals(name)) {
                this.mCrop.bottom = (float) reader.nextDouble();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}
