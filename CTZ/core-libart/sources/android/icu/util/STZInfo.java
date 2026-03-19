package android.icu.util;

import java.io.Serializable;

final class STZInfo implements Serializable {
    private static final long serialVersionUID = -7849612037842370168L;
    boolean ea;
    int edm;
    int edw;
    int edwm;
    int et;
    boolean sa;
    int sdm;
    int sdw;
    int sdwm;
    int st;
    int sy = -1;
    int sm = -1;
    int em = -1;

    STZInfo() {
    }

    void setStart(int i, int i2, int i3, int i4, int i5, boolean z) {
        this.sm = i;
        this.sdwm = i2;
        this.sdw = i3;
        this.st = i4;
        this.sdm = i5;
        this.sa = z;
    }

    void setEnd(int i, int i2, int i3, int i4, int i5, boolean z) {
        this.em = i;
        this.edwm = i2;
        this.edw = i3;
        this.et = i4;
        this.edm = i5;
        this.ea = z;
    }

    void applyTo(SimpleTimeZone simpleTimeZone) {
        if (this.sy != -1) {
            simpleTimeZone.setStartYear(this.sy);
        }
        if (this.sm != -1) {
            if (this.sdm == -1) {
                simpleTimeZone.setStartRule(this.sm, this.sdwm, this.sdw, this.st);
            } else if (this.sdw == -1) {
                simpleTimeZone.setStartRule(this.sm, this.sdm, this.st);
            } else {
                simpleTimeZone.setStartRule(this.sm, this.sdm, this.sdw, this.st, this.sa);
            }
        }
        if (this.em != -1) {
            if (this.edm == -1) {
                simpleTimeZone.setEndRule(this.em, this.edwm, this.edw, this.et);
            } else if (this.edw == -1) {
                simpleTimeZone.setEndRule(this.em, this.edm, this.et);
            } else {
                simpleTimeZone.setEndRule(this.em, this.edm, this.edw, this.et, this.ea);
            }
        }
    }
}
