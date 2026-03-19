package com.android.gallery3d.filtershow.filters;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.BasicParameterStyle;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.mediatek.gallery3d.util.Log;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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

    public void setPramMode(int i) {
        this.mParamMode = i;
        this.mCurrentParam = this.mAllParam[this.mParamMode];
    }

    public Parameter getCurrentParam() {
        return this.mAllParam[this.mParamMode];
    }

    public Parameter getParam(int i) {
        return this.mAllParam[i];
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

        public StrokeData(StrokeData strokeData) {
            this.noPoints = 0;
            this.mPoints = new float[20];
            this.mType = strokeData.mType;
            this.mPath = new Path(strokeData.mPath);
            this.mRadius = strokeData.mRadius;
            this.mColor = strokeData.mColor;
            this.noPoints = strokeData.noPoints;
            this.mPoints = Arrays.copyOf(strokeData.mPoints, strokeData.mPoints.length);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof StrokeData)) {
                return false;
            }
            StrokeData strokeData = (StrokeData) obj;
            if (this.mType == strokeData.mType && this.mRadius == strokeData.mRadius && this.noPoints == strokeData.noPoints && this.mColor == strokeData.mColor) {
                return this.mPath.equals(strokeData.mPath);
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
                int value = ((BasicParameterInt) this.mAllParam[this.mParamMode]).getValue();
                StringBuilder sb = new StringBuilder();
                sb.append(value > 0 ? " +" : " ");
                sb.append(value);
                return sb.toString();
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
        this.mParamColor.setValue(DEFAULT_MENU_COLOR1);
    }

    @Override
    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" : strokes=");
        sb.append(this.mDrawing.size());
        if (this.mCurrent == null) {
            str = " no current ";
        } else {
            str = "draw=" + ((int) this.mCurrent.mType) + " " + this.mCurrent.noPoints;
        }
        sb.append(str);
        return sb.toString();
    }

    public Vector<StrokeData> getDrawing() {
        return this.mDrawing;
    }

    public StrokeData getCurrentDrawing() {
        return this.mCurrent;
    }

    @Override
    public FilterRepresentation copy() {
        FilterDrawRepresentation filterDrawRepresentation = new FilterDrawRepresentation();
        copyAllParameters(filterDrawRepresentation);
        return filterDrawRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public boolean isNil() {
        return getDrawing().isEmpty();
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterDrawRepresentation) {
            this.mParamColor.copyPalletFrom(filterRepresentation.mParamColor);
            try {
                if (filterRepresentation.mCurrent != null) {
                    this.mCurrent = filterRepresentation.mCurrent.m4clone();
                } else {
                    this.mCurrent = null;
                }
                if (filterRepresentation.mDrawing != null) {
                    this.mDrawing = new Vector<>();
                    Iterator<StrokeData> it = filterRepresentation.mDrawing.iterator();
                    while (it.hasNext()) {
                        this.mDrawing.add(new StrokeData(it.next()));
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
        Log.v("FilterDrawRepresentation", "cannot use parameters from " + filterRepresentation);
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (!super.equals(filterRepresentation) || !(filterRepresentation instanceof FilterDrawRepresentation) || filterRepresentation.mDrawing.size() != this.mDrawing.size()) {
            return false;
        }
        if ((filterRepresentation.mCurrent == null) ^ (this.mCurrent == null || this.mCurrent.mPath == null)) {
            return false;
        }
        if (filterRepresentation.mCurrent != null && this.mCurrent != null && this.mCurrent.mPath != null) {
            return filterRepresentation.mCurrent.noPoints == this.mCurrent.noPoints;
        }
        int size = this.mDrawing.size();
        for (int i = 0; i < size; i++) {
            if (!this.mDrawing.get(i).equals(this.mDrawing.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int computeCurrentColor() {
        return this.mParamColor.getValue();
    }

    public void fillStrokeParameters(StrokeData strokeData) {
        byte selected = (byte) this.mParamStyle.getSelected();
        int iComputeCurrentColor = computeCurrentColor();
        float value = this.mParamSize.getValue();
        strokeData.mColor = iComputeCurrentColor;
        strokeData.mRadius = value;
        strokeData.mType = selected;
    }

    public void startNewSection(float f, float f2) {
        this.mCurrent = new StrokeData();
        fillStrokeParameters(this.mCurrent);
        this.mCurrent.mPath = new Path();
        this.mCurrent.mPath.moveTo(f, f2);
        this.mCurrent.mPoints[0] = f;
        this.mCurrent.mPoints[1] = f2;
        this.mCurrent.noPoints = 1;
    }

    public void addPoint(float f, float f2) {
        int i = this.mCurrent.noPoints * 2;
        this.mCurrent.mPath.lineTo(f, f2);
        if (i + 2 > this.mCurrent.mPoints.length) {
            this.mCurrent.mPoints = Arrays.copyOf(this.mCurrent.mPoints, this.mCurrent.mPoints.length * 2);
        }
        this.mCurrent.mPoints[i] = f;
        this.mCurrent.mPoints[i + 1] = f2;
        this.mCurrent.noPoints++;
    }

    public void endSection(float f, float f2) {
        addPoint(f, f2);
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
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        int size = this.mDrawing.size();
        float[] fArr = new float[2];
        float[] fArr2 = new float[2];
        new PathMeasure();
        for (int i = 0; i < size; i++) {
            jsonWriter.name("path" + i);
            jsonWriter.beginObject();
            StrokeData strokeData = this.mDrawing.get(i);
            jsonWriter.name("color").value((long) strokeData.mColor);
            jsonWriter.name("radius").value(strokeData.mRadius);
            jsonWriter.name("type").value(strokeData.mType);
            jsonWriter.name("point_count").value(strokeData.noPoints);
            jsonWriter.name("points");
            jsonWriter.beginArray();
            int i2 = strokeData.noPoints * 2;
            for (int i3 = 0; i3 < i2; i3++) {
                jsonWriter.value(strokeData.mPoints[i3]);
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        Vector<StrokeData> vector = new Vector<>();
        while (jsonReader.hasNext()) {
            jsonReader.nextName();
            jsonReader.beginObject();
            StrokeData strokeData = new StrokeData();
            while (jsonReader.hasNext()) {
                String strNextName = jsonReader.nextName();
                if (strNextName.equals("color")) {
                    strokeData.mColor = jsonReader.nextInt();
                } else if (strNextName.equals("radius")) {
                    strokeData.mRadius = (float) jsonReader.nextDouble();
                } else if (strNextName.equals("type")) {
                    strokeData.mType = (byte) jsonReader.nextInt();
                } else if (strNextName.equals("point_count")) {
                    strokeData.noPoints = jsonReader.nextInt();
                } else if (strNextName.equals("points")) {
                    jsonReader.beginArray();
                    int i = 0;
                    while (jsonReader.hasNext()) {
                        int i2 = i + 1;
                        if (i2 > strokeData.mPoints.length) {
                            strokeData.mPoints = Arrays.copyOf(strokeData.mPoints, i * 2);
                        }
                        strokeData.mPoints[i] = (float) jsonReader.nextDouble();
                        i = i2;
                    }
                    strokeData.mPath = new Path();
                    strokeData.mPath.moveTo(strokeData.mPoints[0], strokeData.mPoints[1]);
                    for (int i3 = 0; i3 < i; i3 += 2) {
                        strokeData.mPath.lineTo(strokeData.mPoints[i3], strokeData.mPoints[i3 + 1]);
                    }
                    jsonReader.endArray();
                    vector.add(strokeData);
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
        }
        this.mDrawing = vector;
        jsonReader.endObject();
    }
}
