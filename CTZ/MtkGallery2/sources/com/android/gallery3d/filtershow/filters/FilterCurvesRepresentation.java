package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.ControlPoint;
import com.android.gallery3d.filtershow.imageshow.Spline;
import com.mediatek.gallery3d.util.Log;
import java.io.IOException;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FilterCurvesRepresentation extends FilterRepresentation {
    private Spline[] mSplines;

    public FilterCurvesRepresentation() {
        super("Curves");
        this.mSplines = new Spline[4];
        setSerializationName("CURVES");
        setFilterClass(ImageFilterCurves.class);
        setTextId(R.string.curvesRGB);
        setOverlayId(R.drawable.filtershow_button_colors_curve);
        setEditorId(R.id.imageCurves);
        setShowParameterValue(false);
        setSupportsPartialRendering(true);
        reset();
    }

    @Override
    public FilterRepresentation copy() {
        FilterCurvesRepresentation filterCurvesRepresentation = new FilterCurvesRepresentation();
        copyAllParameters(filterCurvesRepresentation);
        return filterCurvesRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterCurvesRepresentation)) {
            Log.v("FilterCurvesRepresentation", "cannot use parameters from " + filterRepresentation);
            return;
        }
        Spline[] splineArr = new Spline[4];
        for (int i = 0; i < splineArr.length; i++) {
            Spline spline = filterRepresentation.mSplines[i];
            if (spline != null) {
                splineArr[i] = new Spline(spline);
            } else {
                splineArr[i] = new Spline();
            }
        }
        this.mSplines = splineArr;
    }

    @Override
    public boolean isNil() {
        for (int i = 0; i < 4; i++) {
            if (getSpline(i) != null && !getSpline(i).isOriginal()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (!super.equals(filterRepresentation) || !(filterRepresentation instanceof FilterCurvesRepresentation)) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!getSpline(i).sameValues(filterRepresentation.getSpline(i))) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        Spline spline = new Spline();
        spline.addPoint(0.0f, 1.0f);
        spline.addPoint(1.0f, 0.0f);
        for (int i = 0; i < 4; i++) {
            this.mSplines[i] = new Spline(spline);
        }
    }

    public void setSpline(int i, Spline spline) {
        this.mSplines[i] = spline;
    }

    public Spline getSpline(int i) {
        return this.mSplines[i];
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(SchemaSymbols.ATTVAL_NAME);
        jsonWriter.value(getName());
        for (int i = 0; i < this.mSplines.length; i++) {
            jsonWriter.name("Curve" + i);
            jsonWriter.beginArray();
            int nbPoints = this.mSplines[i].getNbPoints();
            for (int i2 = 0; i2 < nbPoints; i2++) {
                ControlPoint point = this.mSplines[i].getPoint(i2);
                jsonWriter.beginArray();
                jsonWriter.value(point.x);
                jsonWriter.value(point.y);
                jsonWriter.endArray();
            }
            jsonWriter.endArray();
        }
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        Spline[] splineArr = new Spline[4];
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (SchemaSymbols.ATTVAL_NAME.equals(strNextName)) {
                setName(jsonReader.nextString());
            } else if (strNextName.startsWith("Curve")) {
                int i = Integer.parseInt(strNextName.substring("Curve".length()));
                splineArr[i] = new Spline();
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    jsonReader.beginArray();
                    jsonReader.hasNext();
                    float fNextDouble = (float) jsonReader.nextDouble();
                    jsonReader.hasNext();
                    float fNextDouble2 = (float) jsonReader.nextDouble();
                    jsonReader.endArray();
                    splineArr[i].addPoint(fNextDouble, fNextDouble2);
                }
                jsonReader.endArray();
            }
        }
        this.mSplines = splineArr;
        jsonReader.endObject();
    }
}
