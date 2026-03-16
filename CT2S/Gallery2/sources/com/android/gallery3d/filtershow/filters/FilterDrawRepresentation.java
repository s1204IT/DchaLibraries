package com.android.gallery3d.filtershow.filters;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.BasicParameterStyle;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class FilterDrawRepresentation extends FilterRepresentation {
    public static int DEFAULT_MENU_COLOR1 = -2130771968;
    public static int DEFAULT_MENU_COLOR2 = -2147418368;
    public static int DEFAULT_MENU_COLOR3 = -2147483393;
    public static int DEFAULT_MENU_COLOR4 = Integer.MIN_VALUE;
    public static int DEFAULT_MENU_COLOR5 = -2130706433;
    private Parameter[] mAllParam;
    private StrokeData mCurrent;
    Parameter mCurrentParam;
    private Vector<StrokeData> mDrawing;
    ParameterColor mParamColor;
    int mParamMode;
    private BasicParameterInt mParamSize;
    private BasicParameterStyle mParamStyle;

    public void setPramMode(int mode) {
        this.mParamMode = mode;
        this.mCurrentParam = this.mAllParam[this.mParamMode];
    }

    public Parameter getCurrentParam() {
        return this.mAllParam[this.mParamMode];
    }

    public Parameter getParam(int type) {
        return this.mAllParam[type];
    }

    public static class StrokeData implements Cloneable {
        public int mColor;
        public Path mPath;
        public float[] mPoints;
        public float mRadius;
        public byte mType;
        public int noPoints;

        public StrokeData() {
            this.noPoints = 0;
            this.mPoints = new float[20];
        }

        public StrokeData(StrokeData copy) {
            this.noPoints = 0;
            this.mPoints = new float[20];
            this.mType = copy.mType;
            this.mPath = new Path(copy.mPath);
            this.mRadius = copy.mRadius;
            this.mColor = copy.mColor;
            this.noPoints = copy.noPoints;
            this.mPoints = Arrays.copyOf(copy.mPoints, copy.mPoints.length);
        }

        public boolean equals(Object o) {
            if (!(o instanceof StrokeData)) {
                return false;
            }
            StrokeData sd = (StrokeData) o;
            if (this.mType == sd.mType && this.mRadius == sd.mRadius && this.noPoints == sd.noPoints && this.mColor == sd.mColor) {
                return this.mPath.equals(sd.mPath);
            }
            return false;
        }

        public String toString() {
            return "stroke(" + ((int) this.mType) + ", path(" + this.mPath + "), " + this.mRadius + " , " + Integer.toHexString(this.mColor) + ")";
        }

        public StrokeData m4clone() throws CloneNotSupportedException {
            return (StrokeData) super.clone();
        }
    }

    public String getValueString() {
        switch (this.mParamMode) {
            case 0:
                int val = ((BasicParameterInt) this.mAllParam[this.mParamMode]).getValue();
                return (val > 0 ? " +" : " ") + val;
            case 1:
                return "";
            case 2:
                ((ParameterColor) this.mAllParam[this.mParamMode]).getValue();
                return "";
            default:
                return "";
        }
    }

    public FilterDrawRepresentation() {
        super("Draw");
        this.mParamSize = new BasicParameterInt(0, 30, 2, 300);
        this.mParamStyle = new BasicParameterStyle(1, 5);
        this.mParamColor = new ParameterColor(2, DEFAULT_MENU_COLOR1);
        this.mCurrentParam = this.mParamSize;
        this.mAllParam = new Parameter[]{this.mParamSize, this.mParamStyle, this.mParamColor};
        this.mDrawing = new Vector<>();
        setFilterClass(ImageFilterDraw.class);
        setSerializationName("DRAW");
        setFilterType(4);
        setTextId(R.string.imageDraw);
        setEditorId(R.id.editorDraw);
        setOverlayId(R.drawable.filtershow_drawing);
        setOverlayOnly(true);
    }

    @Override
    public String toString() {
        return getName() + " : strokes=" + this.mDrawing.size() + (this.mCurrent == null ? " no current " : "draw=" + ((int) this.mCurrent.mType) + " " + this.mCurrent.noPoints);
    }

    public Vector<StrokeData> getDrawing() {
        return this.mDrawing;
    }

    public StrokeData getCurrentDrawing() {
        return this.mCurrent;
    }

    @Override
    public FilterRepresentation copy() {
        FilterDrawRepresentation representation = new FilterDrawRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public boolean isNil() {
        return getDrawing().isEmpty();
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterDrawRepresentation) {
            FilterDrawRepresentation representation = (FilterDrawRepresentation) a;
            this.mParamColor.copyPalletFrom(representation.mParamColor);
            try {
                if (representation.mCurrent != null) {
                    this.mCurrent = representation.mCurrent.m4clone();
                } else {
                    this.mCurrent = null;
                }
                if (representation.mDrawing != null) {
                    this.mDrawing = new Vector<>();
                    for (StrokeData next : representation.mDrawing) {
                        this.mDrawing.add(new StrokeData(next));
                    }
                    return;
                }
                this.mDrawing = null;
                return;
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return;
            }
        }
        Log.v("FilterDrawRepresentation", "cannot use parameters from " + a);
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterDrawRepresentation)) {
            return false;
        }
        FilterDrawRepresentation fdRep = (FilterDrawRepresentation) representation;
        if (fdRep.mDrawing.size() != this.mDrawing.size()) {
            return false;
        }
        if ((fdRep.mCurrent == null) ^ (this.mCurrent == null || this.mCurrent.mPath == null)) {
            return false;
        }
        if (fdRep.mCurrent != null && this.mCurrent != null && this.mCurrent.mPath != null) {
            return fdRep.mCurrent.noPoints == this.mCurrent.noPoints;
        }
        int n = this.mDrawing.size();
        for (int i = 0; i < n; i++) {
            StrokeData a = this.mDrawing.get(i);
            StrokeData b = this.mDrawing.get(i);
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    private int computeCurrentColor() {
        return this.mParamColor.getValue();
    }

    public void fillStrokeParameters(StrokeData sd) {
        byte type = (byte) this.mParamStyle.getSelected();
        int color = computeCurrentColor();
        float size = this.mParamSize.getValue();
        sd.mColor = color;
        sd.mRadius = size;
        sd.mType = type;
    }

    public void startNewSection(float x, float y) {
        this.mCurrent = new StrokeData();
        fillStrokeParameters(this.mCurrent);
        this.mCurrent.mPath = new Path();
        this.mCurrent.mPath.moveTo(x, y);
        this.mCurrent.mPoints[0] = x;
        this.mCurrent.mPoints[1] = y;
        this.mCurrent.noPoints = 1;
    }

    public void addPoint(float x, float y) {
        int len = this.mCurrent.noPoints * 2;
        this.mCurrent.mPath.lineTo(x, y);
        if (len + 2 > this.mCurrent.mPoints.length) {
            this.mCurrent.mPoints = Arrays.copyOf(this.mCurrent.mPoints, this.mCurrent.mPoints.length * 2);
        }
        this.mCurrent.mPoints[len] = x;
        this.mCurrent.mPoints[len + 1] = y;
        this.mCurrent.noPoints++;
    }

    public void endSection(float x, float y) {
        addPoint(x, y);
        this.mDrawing.add(this.mCurrent);
        this.mCurrent = null;
    }

    public void clearCurrentSection() {
        this.mCurrent = null;
    }

    public void clear() {
        this.mCurrent = null;
        this.mDrawing.clear();
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        int len = this.mDrawing.size();
        float[] fArr = new float[2];
        float[] fArr2 = new float[2];
        new PathMeasure();
        for (int i = 0; i < len; i++) {
            writer.name("path" + i);
            writer.beginObject();
            StrokeData mark = this.mDrawing.get(i);
            writer.name("color").value(mark.mColor);
            writer.name("radius").value(mark.mRadius);
            writer.name("type").value(mark.mType);
            writer.name("point_count").value(mark.noPoints);
            writer.name("points");
            writer.beginArray();
            int npoints = mark.noPoints * 2;
            for (int j = 0; j < npoints; j++) {
                writer.value(mark.mPoints[j]);
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        Vector<StrokeData> strokes = new Vector<>();
        while (sreader.hasNext()) {
            sreader.nextName();
            sreader.beginObject();
            StrokeData stroke = new StrokeData();
            while (sreader.hasNext()) {
                String name = sreader.nextName();
                if (name.equals("color")) {
                    stroke.mColor = sreader.nextInt();
                } else if (name.equals("radius")) {
                    stroke.mRadius = (float) sreader.nextDouble();
                } else if (name.equals("type")) {
                    stroke.mType = (byte) sreader.nextInt();
                } else if (name.equals("point_count")) {
                    stroke.noPoints = sreader.nextInt();
                } else if (name.equals("points")) {
                    int count = 0;
                    sreader.beginArray();
                    while (sreader.hasNext()) {
                        if (count + 1 > stroke.mPoints.length) {
                            stroke.mPoints = Arrays.copyOf(stroke.mPoints, count * 2);
                        }
                        stroke.mPoints[count] = (float) sreader.nextDouble();
                        count++;
                    }
                    stroke.mPath = new Path();
                    stroke.mPath.moveTo(stroke.mPoints[0], stroke.mPoints[1]);
                    for (int i = 0; i < count; i += 2) {
                        stroke.mPath.lineTo(stroke.mPoints[i], stroke.mPoints[i + 1]);
                    }
                    sreader.endArray();
                    strokes.add(stroke);
                } else {
                    sreader.skipValue();
                }
            }
            sreader.endObject();
        }
        this.mDrawing = strokes;
        sreader.endObject();
    }
}
