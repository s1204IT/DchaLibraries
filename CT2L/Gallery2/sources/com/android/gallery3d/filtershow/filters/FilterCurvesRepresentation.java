package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.ControlPoint;
import com.android.gallery3d.filtershow.imageshow.Spline;
import java.io.IOException;

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
        FilterCurvesRepresentation representation = new FilterCurvesRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterCurvesRepresentation)) {
            Log.v("FilterCurvesRepresentation", "cannot use parameters from " + a);
            return;
        }
        FilterCurvesRepresentation representation = (FilterCurvesRepresentation) a;
        Spline[] spline = new Spline[4];
        for (int i = 0; i < spline.length; i++) {
            Spline sp = representation.mSplines[i];
            if (sp != null) {
                spline[i] = new Spline(sp);
            } else {
                spline[i] = new Spline();
            }
        }
        this.mSplines = spline;
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
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterCurvesRepresentation)) {
            return false;
        }
        FilterCurvesRepresentation curve = (FilterCurvesRepresentation) representation;
        for (int i = 0; i < 4; i++) {
            if (!getSpline(i).sameValues(curve.getSpline(i))) {
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

    public void setSpline(int splineIndex, Spline s) {
        this.mSplines[splineIndex] = s;
    }

    public Spline getSpline(int splineIndex) {
        return this.mSplines[splineIndex];
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("Name");
        writer.value(getName());
        for (int i = 0; i < this.mSplines.length; i++) {
            writer.name("Curve" + i);
            writer.beginArray();
            int nop = this.mSplines[i].getNbPoints();
            for (int j = 0; j < nop; j++) {
                ControlPoint p = this.mSplines[i].getPoint(j);
                writer.beginArray();
                writer.value(p.x);
                writer.value(p.y);
                writer.endArray();
            }
            writer.endArray();
        }
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        Spline[] spline = new Spline[4];
        while (sreader.hasNext()) {
            String name = sreader.nextName();
            if ("Name".equals(name)) {
                setName(sreader.nextString());
            } else if (name.startsWith("Curve")) {
                int curveNo = Integer.parseInt(name.substring("Curve".length()));
                spline[curveNo] = new Spline();
                sreader.beginArray();
                while (sreader.hasNext()) {
                    sreader.beginArray();
                    sreader.hasNext();
                    float x = (float) sreader.nextDouble();
                    sreader.hasNext();
                    float y = (float) sreader.nextDouble();
                    sreader.endArray();
                    spline[curveNo].addPoint(x, y);
                }
                sreader.endArray();
            }
        }
        this.mSplines = spline;
        sreader.endObject();
    }
}
