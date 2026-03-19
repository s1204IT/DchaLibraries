package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
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

        Rotation(int i) {
            this.mValue = i;
        }

        public int value() {
            return this.mValue;
        }

        public static Rotation fromValue(int i) {
            if (i == 0) {
                return ZERO;
            }
            if (i == 90) {
                return NINETY;
            }
            if (i == 180) {
                return ONE_EIGHTY;
            }
            if (i == 270) {
                return TWO_SEVENTY;
            }
            return null;
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

    public FilterRotateRepresentation(FilterRotateRepresentation filterRotateRepresentation) {
        this(filterRotateRepresentation.getRotation());
        setName(filterRotateRepresentation.getName());
    }

    public FilterRotateRepresentation() {
        this(getNil());
    }

    public Rotation getRotation() {
        return this.mRotation;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation = new int[Rotation.values().length];

        static {
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[Rotation.ZERO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[Rotation.NINETY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[Rotation.ONE_EIGHTY.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[Rotation.TWO_SEVENTY.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public void rotateCW() {
        switch (AnonymousClass1.$SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[this.mRotation.ordinal()]) {
            case 1:
                this.mRotation = Rotation.NINETY;
                break;
            case 2:
                this.mRotation = Rotation.ONE_EIGHTY;
                break;
            case 3:
                this.mRotation = Rotation.TWO_SEVENTY;
                break;
            case 4:
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
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterRotateRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterRotateRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setRotation(filterRepresentation.getRotation());
    }

    @Override
    public boolean isNil() {
        return this.mRotation == getNil();
    }

    public static Rotation getNil() {
        return Rotation.ZERO;
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("value").value(this.mRotation.value());
        jsonWriter.endObject();
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return (filterRepresentation instanceof FilterRotateRepresentation) && ((FilterRotateRepresentation) filterRepresentation).mRotation.value() == this.mRotation.value();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        boolean z = true;
        while (jsonReader.hasNext()) {
            if ("value".equals(jsonReader.nextName())) {
                Rotation rotationFromValue = Rotation.fromValue(jsonReader.nextInt());
                if (rotationFromValue != null) {
                    setRotation(rotationFromValue);
                    z = false;
                }
            } else {
                jsonReader.skipValue();
            }
        }
        if (z) {
            Log.w(TAG, "WARNING: bad value when deserializing ROTATION");
        }
        jsonReader.endObject();
    }

    @Override
    public void resetRepresentation() {
        this.mRotation = Rotation.ZERO;
    }
}
