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
        int n = this.mBands.size();
        for (int i = n; i < 16; i++) {
            this.mBands.add(new Band());
        }
        for (int i2 = 16; i2 < n; i2++) {
            this.mBands.remove(i2);
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

        static int access$118(Band x0, double x1) {
            int i = (int) (((double) x0.xPos1) + x1);
            x0.xPos1 = i;
            return i;
        }

        static int access$218(Band x0, double x1) {
            int i = (int) (((double) x0.yPos1) + x1);
            x0.yPos1 = i;
            return i;
        }

        static int access$318(Band x0, double x1) {
            int i = (int) (((double) x0.xPos2) + x1);
            x0.xPos2 = i;
            return i;
        }

        static int access$418(Band x0, double x1) {
            int i = (int) (((double) x0.yPos2) + x1);
            x0.yPos2 = i;
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

        public Band(int x, int y) {
            this.mask = true;
            this.xPos1 = -1;
            this.yPos1 = 100;
            this.xPos2 = -1;
            this.yPos2 = 100;
            this.brightness = -40;
            this.contrast = 0;
            this.saturation = 0;
            this.xPos1 = x;
            this.yPos1 = y + 30;
            this.xPos2 = x;
            this.yPos2 = y - 30;
        }

        public Band(Band copy) {
            this.mask = true;
            this.xPos1 = -1;
            this.yPos1 = 100;
            this.xPos2 = -1;
            this.yPos2 = 100;
            this.brightness = -40;
            this.contrast = 0;
            this.saturation = 0;
            this.mask = copy.mask;
            this.xPos1 = copy.xPos1;
            this.yPos1 = copy.yPos1;
            this.xPos2 = copy.xPos2;
            this.yPos2 = copy.yPos2;
            this.brightness = copy.brightness;
            this.contrast = copy.contrast;
            this.saturation = copy.saturation;
        }
    }

    @Override
    public String toString() {
        int count = 0;
        for (Band point : this.mBands) {
            if (!point.mask) {
                count++;
            }
        }
        return "c=" + this.mBands.indexOf(this.mBands) + "[" + this.mBands.size() + "]" + count;
    }

    private void creatExample() {
        Band p = new Band();
        p.mask = false;
        p.xPos1 = -1;
        p.yPos1 = 100;
        p.xPos2 = -1;
        p.yPos2 = 100;
        p.brightness = -50;
        p.contrast = 0;
        p.saturation = 0;
        this.mBands.add(0, p);
        this.mCurrentBand = p;
        trimVector();
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        FilterGradRepresentation rep = (FilterGradRepresentation) a;
        Vector<Band> tmpBands = new Vector<>();
        int n = rep.mCurrentBand == null ? 0 : rep.mBands.indexOf(rep.mCurrentBand);
        for (Band band : rep.mBands) {
            tmpBands.add(new Band(band));
        }
        this.mCurrentBand = null;
        this.mBands = tmpBands;
        this.mCurrentBand = this.mBands.elementAt(n);
    }

    @Override
    public FilterRepresentation copy() {
        FilterGradRepresentation representation = new FilterGradRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!(representation instanceof FilterGradRepresentation)) {
            return false;
        }
        FilterGradRepresentation rep = (FilterGradRepresentation) representation;
        int n = getNumberOfBands();
        if (rep.getNumberOfBands() != n) {
            return false;
        }
        for (int i = 0; i < this.mBands.size(); i++) {
            Band b1 = this.mBands.get(i);
            Band b2 = rep.mBands.get(i);
            if (b1.mask != b2.mask || b1.brightness != b2.brightness || b1.contrast != b2.contrast || b1.saturation != b2.saturation || b1.xPos1 != b2.xPos1 || b1.xPos2 != b2.xPos2 || b1.yPos1 != b2.yPos1 || b1.yPos2 != b2.yPos2) {
                return false;
            }
        }
        return true;
    }

    public int getNumberOfBands() {
        int count = 0;
        for (Band point : this.mBands) {
            if (!point.mask) {
                count++;
            }
        }
        return count;
    }

    public int addBand(Rect rect) {
        Vector<Band> vector = this.mBands;
        Band band = new Band(rect.centerX(), rect.centerY());
        this.mCurrentBand = band;
        vector.add(0, band);
        this.mCurrentBand.mask = false;
        int x = (this.mCurrentBand.xPos1 + this.mCurrentBand.xPos2) / 2;
        int y = (this.mCurrentBand.yPos1 + this.mCurrentBand.yPos2) / 2;
        double addDelta = 0.05d * ((double) Math.max(rect.width(), rect.height()));
        boolean moved = true;
        int count = 0;
        int toMove = this.mBands.indexOf(this.mCurrentBand);
        while (moved) {
            moved = false;
            count++;
            if (count > 14) {
                break;
            }
            Iterator<Band> it = this.mBands.iterator();
            while (it.hasNext() && !it.next().mask) {
            }
            for (Band point : this.mBands) {
                if (!point.mask) {
                    int index = this.mBands.indexOf(point);
                    if (toMove != index) {
                        double dist = Math.hypot(point.xPos1 - x, point.yPos1 - y);
                        if (dist < addDelta) {
                            moved = true;
                            Band.access$118(this.mCurrentBand, addDelta);
                            Band.access$218(this.mCurrentBand, addDelta);
                            Band.access$318(this.mCurrentBand, addDelta);
                            Band.access$418(this.mCurrentBand, addDelta);
                            x = (this.mCurrentBand.xPos1 + this.mCurrentBand.xPos2) / 2;
                            y = (this.mCurrentBand.yPos1 + this.mCurrentBand.yPos2) / 2;
                            if (this.mCurrentBand.yPos1 > rect.bottom) {
                                this.mCurrentBand.yPos1 = (int) (((double) rect.top) + addDelta);
                            }
                            if (this.mCurrentBand.xPos1 > rect.right) {
                                this.mCurrentBand.xPos1 = (int) (((double) rect.left) + addDelta);
                            }
                        }
                    }
                }
            }
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

    public void setSelectedPoint(int pos) {
        this.mCurrentBand = this.mBands.get(pos);
    }

    public int getSelectedPoint() {
        return this.mBands.indexOf(this.mCurrentBand);
    }

    public boolean[] getMask() {
        boolean[] ret = new boolean[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            int i2 = i + 1;
            ret[i] = !point.mask;
            i = i2;
        }
        return ret;
    }

    public int[] getXPos1() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.xPos1;
            i++;
        }
        return ret;
    }

    public int[] getYPos1() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.yPos1;
            i++;
        }
        return ret;
    }

    public int[] getXPos2() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.xPos2;
            i++;
        }
        return ret;
    }

    public int[] getYPos2() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.yPos2;
            i++;
        }
        return ret;
    }

    public int[] getBrightness() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.brightness;
            i++;
        }
        return ret;
    }

    public int[] getContrast() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.contrast;
            i++;
        }
        return ret;
    }

    public int[] getSaturation() {
        int[] ret = new int[this.mBands.size()];
        int i = 0;
        for (Band point : this.mBands) {
            ret[i] = point.saturation;
            i++;
        }
        return ret;
    }

    public int getParameter(int type) {
        switch (type) {
            case 0:
                return this.mCurrentBand.brightness;
            case 1:
                return this.mCurrentBand.saturation;
            case 2:
                return this.mCurrentBand.contrast;
            default:
                throw new IllegalArgumentException("no such type " + type);
        }
    }

    public int getParameterMax(int type) {
        switch (type) {
            case 0:
            case 1:
            case 2:
                return 100;
            default:
                throw new IllegalArgumentException("no such type " + type);
        }
    }

    public int getParameterMin(int type) {
        switch (type) {
            case 0:
            case 1:
            case 2:
                return -100;
            default:
                throw new IllegalArgumentException("no such type " + type);
        }
    }

    public void setParameter(int type, int value) {
        this.mCurrentBand.mask = false;
        switch (type) {
            case 0:
                this.mCurrentBand.brightness = value;
                return;
            case 1:
                this.mCurrentBand.saturation = value;
                return;
            case 2:
                this.mCurrentBand.contrast = value;
                return;
            default:
                throw new IllegalArgumentException("no such type " + type);
        }
    }

    @Override
    public void setPoint1(float x, float y) {
        this.mCurrentBand.xPos1 = (int) x;
        this.mCurrentBand.yPos1 = (int) y;
    }

    @Override
    public void setPoint2(float x, float y) {
        this.mCurrentBand.xPos2 = (int) x;
        this.mCurrentBand.yPos2 = (int) y;
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
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        int len = this.mBands.size();
        int count = 0;
        for (int i = 0; i < len; i++) {
            Band point = this.mBands.get(i);
            if (!point.mask) {
                writer.name(LINE_NAME + count);
                count++;
                writer.beginArray();
                writer.value(point.xPos1);
                writer.value(point.yPos1);
                writer.value(point.xPos2);
                writer.value(point.yPos2);
                writer.value(point.brightness);
                writer.value(point.contrast);
                writer.value(point.saturation);
                writer.endArray();
            }
        }
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        Vector<Band> points = new Vector<>();
        while (sreader.hasNext()) {
            String name = sreader.nextName();
            if (name.startsWith(LINE_NAME)) {
                Integer.parseInt(name.substring(LINE_NAME.length()));
                sreader.beginArray();
                Band p = new Band();
                p.mask = false;
                sreader.hasNext();
                p.xPos1 = sreader.nextInt();
                sreader.hasNext();
                p.yPos1 = sreader.nextInt();
                sreader.hasNext();
                p.xPos2 = sreader.nextInt();
                sreader.hasNext();
                p.yPos2 = sreader.nextInt();
                sreader.hasNext();
                p.brightness = sreader.nextInt();
                sreader.hasNext();
                p.contrast = sreader.nextInt();
                sreader.hasNext();
                p.saturation = sreader.nextInt();
                sreader.hasNext();
                sreader.endArray();
                points.add(p);
            } else {
                sreader.skipValue();
            }
        }
        this.mBands = points;
        trimVector();
        this.mCurrentBand = this.mBands.get(0);
        sreader.endObject();
    }
}
