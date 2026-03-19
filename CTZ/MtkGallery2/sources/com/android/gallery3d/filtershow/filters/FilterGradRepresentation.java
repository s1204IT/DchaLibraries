package com.android.gallery3d.filtershow.filters;

import android.graphics.Rect;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.Line;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

public class FilterGradRepresentation extends FilterRepresentation implements Line {
    private static String LINE_NAME = "Point";
    Vector<Band> mBands;
    Band mCurrentBand;

    public FilterGradRepresentation() {
        super("Grad");
        this.mBands = new Vector<>();
        setSerializationName("grad");
        creatExample();
        setOverlayId(R.drawable.filtershow_button_grad);
        setFilterClass(ImageFilterGrad.class);
        setTextId(R.string.grad);
        setEditorId(R.id.editorGrad);
    }

    public void trimVector() {
        int i;
        int size = this.mBands.size();
        int i2 = size;
        while (true) {
            if (i2 >= 16) {
                break;
            }
            this.mBands.add(new Band());
            i2++;
        }
        for (i = 16; i < size; i++) {
            this.mBands.remove(i);
        }
    }

    static class Band {
        private int brightness;
        private int contrast;
        private boolean mask;
        private int saturation;
        private int xPos1;
        private int xPos2;
        private int yPos1;
        private int yPos2;

        static int access$118(Band band, double d) {
            int i = (int) (((double) band.xPos1) + d);
            band.xPos1 = i;
            return i;
        }

        static int access$218(Band band, double d) {
            int i = (int) (((double) band.yPos1) + d);
            band.yPos1 = i;
            return i;
        }

        static int access$318(Band band, double d) {
            int i = (int) (((double) band.xPos2) + d);
            band.xPos2 = i;
            return i;
        }

        static int access$418(Band band, double d) {
            int i = (int) (((double) band.yPos2) + d);
            band.yPos2 = i;
            return i;
        }

        public Band() {
            this.mask = true;
            this.xPos1 = -1;
            this.yPos1 = 100;
            this.xPos2 = -1;
            this.yPos2 = 100;
            this.brightness = -40;
            this.contrast = 0;
            this.saturation = 0;
        }

        public Band(int i, int i2) {
            this.mask = true;
            this.xPos1 = -1;
            this.yPos1 = 100;
            this.xPos2 = -1;
            this.yPos2 = 100;
            this.brightness = -40;
            this.contrast = 0;
            this.saturation = 0;
            this.xPos1 = i;
            this.yPos1 = i2 + 30;
            this.xPos2 = i;
            this.yPos2 = i2 - 30;
        }

        public Band(Band band) {
            this.mask = true;
            this.xPos1 = -1;
            this.yPos1 = 100;
            this.xPos2 = -1;
            this.yPos2 = 100;
            this.brightness = -40;
            this.contrast = 0;
            this.saturation = 0;
            this.mask = band.mask;
            this.xPos1 = band.xPos1;
            this.yPos1 = band.yPos1;
            this.xPos2 = band.xPos2;
            this.yPos2 = band.yPos2;
            this.brightness = band.brightness;
            this.contrast = band.contrast;
            this.saturation = band.saturation;
        }
    }

    @Override
    public String toString() {
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (!it.next().mask) {
                i++;
            }
        }
        return "c=" + this.mBands.indexOf(this.mBands) + "[" + this.mBands.size() + "]" + i;
    }

    private void creatExample() {
        Band band = new Band();
        band.mask = false;
        band.xPos1 = -1;
        band.yPos1 = 100;
        band.xPos2 = -1;
        band.yPos2 = 100;
        band.brightness = -50;
        band.contrast = 0;
        band.saturation = 0;
        this.mBands.add(0, band);
        this.mCurrentBand = band;
        trimVector();
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        FilterGradRepresentation filterGradRepresentation = (FilterGradRepresentation) filterRepresentation;
        Vector<Band> vector = new Vector<>();
        int iIndexOf = filterGradRepresentation.mCurrentBand == null ? 0 : filterGradRepresentation.mBands.indexOf(filterGradRepresentation.mCurrentBand);
        Iterator<Band> it = filterGradRepresentation.mBands.iterator();
        while (it.hasNext()) {
            vector.add(new Band(it.next()));
        }
        this.mCurrentBand = null;
        this.mBands = vector;
        this.mCurrentBand = this.mBands.elementAt(iIndexOf);
    }

    @Override
    public FilterRepresentation copy() {
        FilterGradRepresentation filterGradRepresentation = new FilterGradRepresentation();
        copyAllParameters(filterGradRepresentation);
        return filterGradRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (!(filterRepresentation instanceof FilterGradRepresentation)) {
            return false;
        }
        FilterGradRepresentation filterGradRepresentation = (FilterGradRepresentation) filterRepresentation;
        if (filterGradRepresentation.getNumberOfBands() != getNumberOfBands()) {
            return false;
        }
        for (int i = 0; i < this.mBands.size(); i++) {
            Band band = this.mBands.get(i);
            Band band2 = filterGradRepresentation.mBands.get(i);
            if (band.mask != band2.mask || band.brightness != band2.brightness || band.contrast != band2.contrast || band.saturation != band2.saturation || band.xPos1 != band2.xPos1 || band.xPos2 != band2.xPos2 || band.yPos1 != band2.yPos1 || band.yPos2 != band2.yPos2) {
                return false;
            }
        }
        return true;
    }

    public int getNumberOfBands() {
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (!it.next().mask) {
                i++;
            }
        }
        return i;
    }

    public int addBand(Rect rect) {
        int i;
        Vector<Band> vector = this.mBands;
        Band band = new Band(rect.centerX(), rect.centerY());
        this.mCurrentBand = band;
        boolean z = false;
        vector.add(0, band);
        this.mCurrentBand.mask = false;
        int i2 = (this.mCurrentBand.xPos1 + this.mCurrentBand.xPos2) / 2;
        int i3 = (this.mCurrentBand.yPos1 + this.mCurrentBand.yPos2) / 2;
        double dMax = 0.05d * ((double) Math.max(rect.width(), rect.height()));
        int iIndexOf = this.mBands.indexOf(this.mCurrentBand);
        int i4 = i2;
        int i5 = i3;
        int i6 = 0;
        boolean z2 = true;
        while (z2) {
            i6++;
            if (i6 > 14) {
                break;
            }
            Iterator<Band> it = this.mBands.iterator();
            while (it.hasNext() && !it.next().mask) {
            }
            int i7 = i5;
            int i8 = i4;
            boolean z3 = z;
            for (Band band2 : this.mBands) {
                if (band2.mask) {
                    break;
                }
                if (iIndexOf != this.mBands.indexOf(band2)) {
                    i = iIndexOf;
                    if (Math.hypot(band2.xPos1 - i8, band2.yPos1 - i7) < dMax) {
                        Band.access$118(this.mCurrentBand, dMax);
                        Band.access$218(this.mCurrentBand, dMax);
                        Band.access$318(this.mCurrentBand, dMax);
                        Band.access$418(this.mCurrentBand, dMax);
                        i8 = (this.mCurrentBand.xPos1 + this.mCurrentBand.xPos2) / 2;
                        i7 = (this.mCurrentBand.yPos1 + this.mCurrentBand.yPos2) / 2;
                        if (this.mCurrentBand.yPos1 > rect.bottom) {
                            this.mCurrentBand.yPos1 = (int) (((double) rect.top) + dMax);
                        }
                        if (this.mCurrentBand.xPos1 > rect.right) {
                            this.mCurrentBand.xPos1 = (int) (((double) rect.left) + dMax);
                        }
                        z3 = true;
                    }
                } else {
                    i = iIndexOf;
                }
                iIndexOf = i;
            }
            z2 = z3;
            i4 = i8;
            i5 = i7;
            iIndexOf = iIndexOf;
            z = false;
        }
        trimVector();
        return 0;
    }

    public void deleteCurrentBand() {
        this.mBands.indexOf(this.mCurrentBand);
        this.mBands.remove(this.mCurrentBand);
        trimVector();
        if (getNumberOfBands() == 0) {
            addBand(MasterImage.getImage().getOriginalBounds());
        }
        this.mCurrentBand = this.mBands.get(0);
    }

    public void setSelectedPoint(int i) {
        this.mCurrentBand = this.mBands.get(i);
    }

    public int getSelectedPoint() {
        return this.mBands.indexOf(this.mCurrentBand);
    }

    public boolean[] getMask() {
        boolean[] zArr = new boolean[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            zArr[i] = !it.next().mask;
            i++;
        }
        return zArr;
    }

    public int[] getXPos1() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().xPos1;
            i++;
        }
        return iArr;
    }

    public int[] getYPos1() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().yPos1;
            i++;
        }
        return iArr;
    }

    public int[] getXPos2() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().xPos2;
            i++;
        }
        return iArr;
    }

    public int[] getYPos2() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().yPos2;
            i++;
        }
        return iArr;
    }

    public int[] getBrightness() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().brightness;
            i++;
        }
        return iArr;
    }

    public int[] getContrast() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().contrast;
            i++;
        }
        return iArr;
    }

    public int[] getSaturation() {
        int[] iArr = new int[this.mBands.size()];
        Iterator<Band> it = this.mBands.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().saturation;
            i++;
        }
        return iArr;
    }

    public int getParameter(int i) {
        switch (i) {
            case 0:
                return this.mCurrentBand.brightness;
            case 1:
                return this.mCurrentBand.saturation;
            case 2:
                return this.mCurrentBand.contrast;
            default:
                throw new IllegalArgumentException("no such type " + i);
        }
    }

    public int getParameterMax(int i) {
        switch (i) {
            case 0:
                return 100;
            case 1:
                return 100;
            case 2:
                return 100;
            default:
                throw new IllegalArgumentException("no such type " + i);
        }
    }

    public int getParameterMin(int i) {
        switch (i) {
            case 0:
                return -100;
            case 1:
                return -100;
            case 2:
                return -100;
            default:
                throw new IllegalArgumentException("no such type " + i);
        }
    }

    public void setParameter(int i, int i2) {
        this.mCurrentBand.mask = false;
        switch (i) {
            case 0:
                this.mCurrentBand.brightness = i2;
                return;
            case 1:
                this.mCurrentBand.saturation = i2;
                return;
            case 2:
                this.mCurrentBand.contrast = i2;
                return;
            default:
                throw new IllegalArgumentException("no such type " + i);
        }
    }

    @Override
    public void setPoint1(float f, float f2) {
        this.mCurrentBand.xPos1 = (int) f;
        this.mCurrentBand.yPos1 = (int) f2;
    }

    @Override
    public void setPoint2(float f, float f2) {
        this.mCurrentBand.xPos2 = (int) f;
        this.mCurrentBand.yPos2 = (int) f2;
    }

    @Override
    public float getPoint1X() {
        return this.mCurrentBand.xPos1;
    }

    @Override
    public float getPoint1Y() {
        return this.mCurrentBand.yPos1;
    }

    @Override
    public float getPoint2X() {
        return this.mCurrentBand.xPos2;
    }

    @Override
    public float getPoint2Y() {
        return this.mCurrentBand.yPos2;
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        int size = this.mBands.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            Band band = this.mBands.get(i2);
            if (!band.mask) {
                jsonWriter.name(LINE_NAME + i);
                i++;
                jsonWriter.beginArray();
                jsonWriter.value((long) band.xPos1);
                jsonWriter.value(band.yPos1);
                jsonWriter.value(band.xPos2);
                jsonWriter.value(band.yPos2);
                jsonWriter.value(band.brightness);
                jsonWriter.value(band.contrast);
                jsonWriter.value(band.saturation);
                jsonWriter.endArray();
            }
        }
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        Vector<Band> vector = new Vector<>();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (strNextName.startsWith(LINE_NAME)) {
                Integer.parseInt(strNextName.substring(LINE_NAME.length()));
                jsonReader.beginArray();
                Band band = new Band();
                band.mask = false;
                jsonReader.hasNext();
                band.xPos1 = jsonReader.nextInt();
                jsonReader.hasNext();
                band.yPos1 = jsonReader.nextInt();
                jsonReader.hasNext();
                band.xPos2 = jsonReader.nextInt();
                jsonReader.hasNext();
                band.yPos2 = jsonReader.nextInt();
                jsonReader.hasNext();
                band.brightness = jsonReader.nextInt();
                jsonReader.hasNext();
                band.contrast = jsonReader.nextInt();
                jsonReader.hasNext();
                band.saturation = jsonReader.nextInt();
                jsonReader.hasNext();
                jsonReader.endArray();
                vector.add(band);
            } else {
                jsonReader.skipValue();
            }
        }
        this.mBands = vector;
        trimVector();
        this.mCurrentBand = this.mBands.get(0);
        jsonReader.endObject();
    }
}
