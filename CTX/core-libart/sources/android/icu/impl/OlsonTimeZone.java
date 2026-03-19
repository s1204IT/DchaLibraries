package android.icu.impl;

import android.icu.lang.UCharacterEnums;
import android.icu.util.AnnualTimeZoneRule;
import android.icu.util.BasicTimeZone;
import android.icu.util.DateTimeRule;
import android.icu.util.InitialTimeZoneRule;
import android.icu.util.SimpleTimeZone;
import android.icu.util.TimeArrayTimeZoneRule;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneRule;
import android.icu.util.TimeZoneTransition;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.MissingResourceException;

public class OlsonTimeZone extends BasicTimeZone {
    static final boolean $assertionsDisabled = false;
    private static final boolean DEBUG = ICUDebug.enabled("olson");
    private static final int MAX_OFFSET_SECONDS = 86400;
    private static final int SECONDS_PER_DAY = 86400;
    private static final String ZONEINFORES = "zoneinfo64";
    private static final int currentSerialVersion = 1;
    static final long serialVersionUID = -6281977362477515376L;
    private volatile String canonicalID;
    private double finalStartMillis;
    private int finalStartYear;
    private SimpleTimeZone finalZone;
    private transient SimpleTimeZone finalZoneWithStartYear;
    private transient TimeZoneTransition firstFinalTZTransition;
    private transient TimeZoneTransition firstTZTransition;
    private transient int firstTZTransitionIdx;
    private transient TimeArrayTimeZoneRule[] historicRules;
    private transient InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen;
    private int serialVersionOnStream;
    private int transitionCount;
    private transient boolean transitionRulesInitialized;
    private long[] transitionTimes64;
    private int typeCount;
    private byte[] typeMapData;
    private int[] typeOffsets;

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        if (i3 < 0 || i3 > 11) {
            throw new IllegalArgumentException("Month is not in the legal range: " + i3);
        }
        return getOffset(i, i2, i3, i4, i5, i6, Grego.monthLength(i2, i3));
    }

    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        if ((i != 1 && i != 0) || i3 < 0 || i3 > 11 || i4 < 1 || i4 > i7 || i5 < 1 || i5 > 7 || i6 < 0 || i6 >= 86400000 || i7 < 28 || i7 > 31) {
            throw new IllegalArgumentException();
        }
        if (i == 0) {
            i2 = -i2;
        }
        int i8 = i2;
        if (this.finalZone != null && i8 >= this.finalStartYear) {
            return this.finalZone.getOffset(i, i8, i3, i4, i5, i6);
        }
        int[] iArr = new int[2];
        getHistoricalOffset((Grego.fieldsToDay(i8, i3, i4) * 86400000) + ((long) i6), true, 3, 1, iArr);
        return iArr[0] + iArr[1];
    }

    @Override
    public void setRawOffset(int i) {
        DateTimeRule rule;
        DateTimeRule rule2;
        int i2;
        TimeZoneTransition previousTransition;
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        }
        if (getRawOffset() == i) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis < this.finalStartMillis) {
            SimpleTimeZone simpleTimeZone = new SimpleTimeZone(i, getID());
            boolean zUseDaylightTime = useDaylightTime();
            if (zUseDaylightTime) {
                TimeZoneRule[] simpleTimeZoneRulesNear = getSimpleTimeZoneRulesNear(jCurrentTimeMillis);
                if (simpleTimeZoneRulesNear.length != 3 && (previousTransition = getPreviousTransition(jCurrentTimeMillis, false)) != null) {
                    simpleTimeZoneRulesNear = getSimpleTimeZoneRulesNear(previousTransition.getTime() - 1);
                }
                if (simpleTimeZoneRulesNear.length == 3 && (simpleTimeZoneRulesNear[1] instanceof AnnualTimeZoneRule) && (simpleTimeZoneRulesNear[2] instanceof AnnualTimeZoneRule)) {
                    AnnualTimeZoneRule annualTimeZoneRule = (AnnualTimeZoneRule) simpleTimeZoneRulesNear[1];
                    AnnualTimeZoneRule annualTimeZoneRule2 = (AnnualTimeZoneRule) simpleTimeZoneRulesNear[2];
                    int rawOffset = annualTimeZoneRule.getRawOffset() + annualTimeZoneRule.getDSTSavings();
                    int rawOffset2 = annualTimeZoneRule2.getRawOffset() + annualTimeZoneRule2.getDSTSavings();
                    if (rawOffset > rawOffset2) {
                        DateTimeRule rule3 = annualTimeZoneRule.getRule();
                        i2 = rawOffset - rawOffset2;
                        rule2 = annualTimeZoneRule2.getRule();
                        rule = rule3;
                    } else {
                        rule = annualTimeZoneRule2.getRule();
                        rule2 = annualTimeZoneRule.getRule();
                        i2 = rawOffset2 - rawOffset;
                    }
                    simpleTimeZone.setStartRule(rule.getRuleMonth(), rule.getRuleWeekInMonth(), rule.getRuleDayOfWeek(), rule.getRuleMillisInDay());
                    simpleTimeZone.setEndRule(rule2.getRuleMonth(), rule2.getRuleWeekInMonth(), rule2.getRuleDayOfWeek(), rule2.getRuleMillisInDay());
                    simpleTimeZone.setDSTSavings(i2);
                } else {
                    simpleTimeZone.setStartRule(0, 1, 0);
                    simpleTimeZone.setEndRule(11, 31, 86399999);
                }
            }
            this.finalStartYear = Grego.timeToFields(jCurrentTimeMillis, null)[0];
            this.finalStartMillis = Grego.fieldsToDay(r0[0], 0, 1);
            if (zUseDaylightTime) {
                simpleTimeZone.setStartYear(this.finalStartYear);
            }
            this.finalZone = simpleTimeZone;
        } else {
            this.finalZone.setRawOffset(i);
        }
        this.transitionRulesInitialized = false;
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    @Override
    public void getOffset(long j, boolean z, int[] iArr) {
        if (this.finalZone != null && j >= this.finalStartMillis) {
            this.finalZone.getOffset(j, z, iArr);
        } else {
            getHistoricalOffset(j, z, 4, 12, iArr);
        }
    }

    @Override
    public void getOffsetFromLocal(long j, int i, int i2, int[] iArr) {
        if (this.finalZone != null && j >= this.finalStartMillis) {
            this.finalZone.getOffsetFromLocal(j, i, i2, iArr);
        } else {
            getHistoricalOffset(j, true, i, i2, iArr);
        }
    }

    @Override
    public int getRawOffset() {
        int[] iArr = new int[2];
        getOffset(System.currentTimeMillis(), false, iArr);
        return iArr[0];
    }

    @Override
    public boolean useDaylightTime() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.finalZone != null && jCurrentTimeMillis >= this.finalStartMillis) {
            return this.finalZone != null && this.finalZone.useDaylightTime();
        }
        int[] iArrTimeToFields = Grego.timeToFields(jCurrentTimeMillis, null);
        long jFieldsToDay = Grego.fieldsToDay(iArrTimeToFields[0], 0, 1) * 86400;
        long jFieldsToDay2 = Grego.fieldsToDay(iArrTimeToFields[0] + 1, 0, 1) * 86400;
        for (int i = 0; i < this.transitionCount && this.transitionTimes64[i] < jFieldsToDay2; i++) {
            if ((this.transitionTimes64[i] >= jFieldsToDay && dstOffsetAt(i) != 0) || (this.transitionTimes64[i] > jFieldsToDay && i > 0 && dstOffsetAt(i - 1) != 0)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean observesDaylightTime() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                return true;
            }
            if (jCurrentTimeMillis >= this.finalStartMillis) {
                return false;
            }
        }
        long jFloorDivide = Grego.floorDivide(jCurrentTimeMillis, 1000L);
        int i = this.transitionCount - 1;
        if (dstOffsetAt(i) != 0) {
            return true;
        }
        while (i >= 0 && this.transitionTimes64[i] > jFloorDivide) {
            if (dstOffsetAt(i - 1) != 0) {
                return true;
            }
            i--;
        }
        return false;
    }

    @Override
    public int getDSTSavings() {
        if (this.finalZone != null) {
            return this.finalZone.getDSTSavings();
        }
        return super.getDSTSavings();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        int[] iArr = new int[2];
        getOffset(date.getTime(), false, iArr);
        return iArr[1] != 0;
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (this == timeZone) {
            return true;
        }
        if (!super.hasSameRules(timeZone) || !(timeZone instanceof OlsonTimeZone)) {
            return false;
        }
        OlsonTimeZone olsonTimeZone = (OlsonTimeZone) timeZone;
        if (this.finalZone == null) {
            if (olsonTimeZone.finalZone != null) {
                return false;
            }
        } else if (olsonTimeZone.finalZone == null || this.finalStartYear != olsonTimeZone.finalStartYear || !this.finalZone.hasSameRules(olsonTimeZone.finalZone)) {
            return false;
        }
        return this.transitionCount == olsonTimeZone.transitionCount && Arrays.equals(this.transitionTimes64, olsonTimeZone.transitionTimes64) && this.typeCount == olsonTimeZone.typeCount && Arrays.equals(this.typeMapData, olsonTimeZone.typeMapData) && Arrays.equals(this.typeOffsets, olsonTimeZone.typeOffsets);
    }

    public String getCanonicalID() {
        if (this.canonicalID == null) {
            synchronized (this) {
                if (this.canonicalID == null) {
                    this.canonicalID = getCanonicalID(getID());
                    if (this.canonicalID == null) {
                        this.canonicalID = getID();
                    }
                }
            }
        }
        return this.canonicalID;
    }

    private void constructEmpty() {
        this.transitionCount = 0;
        this.transitionTimes64 = null;
        this.typeMapData = null;
        this.typeCount = 1;
        this.typeOffsets = new int[]{0, 0};
        this.finalZone = null;
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.transitionRulesInitialized = false;
    }

    public OlsonTimeZone(UResourceBundle uResourceBundle, UResourceBundle uResourceBundle2, String str) {
        super(str);
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.finalZone = null;
        this.canonicalID = null;
        this.serialVersionOnStream = 1;
        this.isFrozen = false;
        construct(uResourceBundle, uResourceBundle2);
    }

    private void construct(UResourceBundle uResourceBundle, UResourceBundle uResourceBundle2) {
        int[] intVector;
        int[] intVector2;
        int[] intVector3;
        SimpleTimeZone simpleTimeZone;
        ?? string;
        int i;
        OlsonTimeZone olsonTimeZone = this;
        if (uResourceBundle == null || uResourceBundle2 == null) {
            throw new IllegalArgumentException();
        }
        if (DEBUG) {
            System.out.println("OlsonTimeZone(" + uResourceBundle2.getKey() + ")");
        }
        olsonTimeZone.transitionCount = 0;
        try {
            intVector = uResourceBundle2.get("transPre32").getIntVector();
        } catch (MissingResourceException e) {
            intVector = null;
        }
        if (intVector.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        olsonTimeZone.transitionCount += intVector.length / 2;
        try {
            intVector2 = uResourceBundle2.get("trans").getIntVector();
            try {
                olsonTimeZone.transitionCount += intVector2.length;
            } catch (MissingResourceException e2) {
            }
        } catch (MissingResourceException e3) {
            intVector2 = null;
        }
        try {
            intVector3 = uResourceBundle2.get("transPost32").getIntVector();
        } catch (MissingResourceException e4) {
            intVector3 = null;
        }
        if (intVector3.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        olsonTimeZone.transitionCount += intVector3.length / 2;
        if (olsonTimeZone.transitionCount > 0) {
            olsonTimeZone.transitionTimes64 = new long[olsonTimeZone.transitionCount];
            char c = ' ';
            if (intVector != null) {
                int i2 = 0;
                i = 0;
                for (int i3 = 2; i2 < intVector.length / i3; i3 = 2) {
                    int i4 = i2 * 2;
                    olsonTimeZone.transitionTimes64[i] = (((long) intVector[i4 + 1]) & 4294967295L) | ((((long) intVector[i4]) & 4294967295L) << c);
                    i2++;
                    i++;
                    c = ' ';
                    olsonTimeZone = this;
                }
            } else {
                i = 0;
            }
            if (intVector2 != null) {
                int i5 = 0;
                while (i5 < intVector2.length) {
                    this.transitionTimes64[i] = intVector2[i5];
                    i5++;
                    i++;
                }
            }
            olsonTimeZone = this;
            if (intVector3 != null) {
                int i6 = 0;
                while (i6 < intVector3.length / 2) {
                    int i7 = i6 * 2;
                    olsonTimeZone.transitionTimes64[i] = ((((long) intVector3[i7]) & 4294967295L) << 32) | (((long) intVector3[i7 + 1]) & 4294967295L);
                    i6++;
                    i++;
                    intVector3 = intVector3;
                }
            }
        } else {
            olsonTimeZone.transitionTimes64 = null;
        }
        olsonTimeZone.typeOffsets = uResourceBundle2.get("typeOffsets").getIntVector();
        if (olsonTimeZone.typeOffsets.length < 2 || olsonTimeZone.typeOffsets.length > 32766 || olsonTimeZone.typeOffsets.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        olsonTimeZone.typeCount = olsonTimeZone.typeOffsets.length / 2;
        if (olsonTimeZone.transitionCount > 0) {
            olsonTimeZone.typeMapData = uResourceBundle2.get("typeMap").getBinary(null);
            if (olsonTimeZone.typeMapData == null || olsonTimeZone.typeMapData.length != olsonTimeZone.transitionCount) {
                throw new IllegalArgumentException("Invalid Format");
            }
            simpleTimeZone = null;
        } else {
            simpleTimeZone = null;
            olsonTimeZone.typeMapData = null;
        }
        olsonTimeZone.finalZone = simpleTimeZone;
        olsonTimeZone.finalStartYear = Integer.MAX_VALUE;
        olsonTimeZone.finalStartMillis = Double.MAX_VALUE;
        try {
            string = uResourceBundle2.getString("finalRule");
            try {
                int i8 = uResourceBundle2.get("finalRaw").getInt() * 1000;
                int[] intVector4 = loadRule(uResourceBundle, string).getIntVector();
                if (intVector4 == null || intVector4.length != 11) {
                    throw new IllegalArgumentException("Invalid Format");
                }
                olsonTimeZone.finalZone = new SimpleTimeZone(i8, "", intVector4[0], intVector4[1], intVector4[2], intVector4[3] * 1000, intVector4[4], intVector4[5], intVector4[6], intVector4[7], intVector4[8] * 1000, intVector4[9], intVector4[10] * 1000);
                olsonTimeZone.finalStartYear = uResourceBundle2.get("finalYear").getInt();
                olsonTimeZone.finalStartMillis = Grego.fieldsToDay(olsonTimeZone.finalStartYear, 0, 1) * 86400000;
            } catch (MissingResourceException e5) {
                if (string != 0) {
                    throw new IllegalArgumentException("Invalid Format");
                }
            }
        } catch (MissingResourceException e6) {
            string = simpleTimeZone;
        }
    }

    public OlsonTimeZone(String str) {
        super(str);
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.finalZone = null;
        this.canonicalID = null;
        this.serialVersionOnStream = 1;
        this.isFrozen = false;
        UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        construct(bundleInstance, ZoneMeta.openOlsonResource(bundleInstance, str));
        if (this.finalZone != null) {
            this.finalZone.setID(str);
        }
    }

    @Override
    public void setID(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        }
        if (this.canonicalID == null) {
            this.canonicalID = getCanonicalID(getID());
            if (this.canonicalID == null) {
                this.canonicalID = getID();
            }
        }
        if (this.finalZone != null) {
            this.finalZone.setID(str);
        }
        super.setID(str);
        this.transitionRulesInitialized = false;
    }

    private void getHistoricalOffset(long j, boolean z, int i, int i2, int[] iArr) {
        boolean z2 = false;
        if (this.transitionCount != 0) {
            long jFloorDivide = Grego.floorDivide(j, 1000L);
            if (!z && jFloorDivide < this.transitionTimes64[0]) {
                iArr[0] = initialRawOffset() * 1000;
                iArr[1] = initialDstOffset() * 1000;
                return;
            }
            int i3 = this.transitionCount - 1;
            while (i3 >= 0) {
                long j2 = this.transitionTimes64[i3];
                if (z && jFloorDivide >= j2 - 86400) {
                    int i4 = i3 - 1;
                    int iZoneOffsetAt = zoneOffsetAt(i4);
                    boolean z3 = dstOffsetAt(i4) != 0 ? true : z2;
                    int iZoneOffsetAt2 = zoneOffsetAt(i3);
                    boolean z4 = dstOffsetAt(i3) != 0 ? true : z2;
                    boolean z5 = (!z3 || z4) ? z2 : true;
                    boolean z6 = (z3 || !z4) ? z2 : true;
                    if (iZoneOffsetAt2 - iZoneOffsetAt >= 0) {
                        int i5 = i & 3;
                        j2 = ((i5 == 1 && z5) || (i5 == 3 && z6)) ? j2 + ((long) iZoneOffsetAt) : (!(i5 == 1 && z6) && !(i5 == 3 && z5) && (i & 12) == 12) ? j2 + ((long) iZoneOffsetAt) : j2 + ((long) iZoneOffsetAt2);
                    } else {
                        int i6 = i2 & 3;
                        j2 = ((i6 == 1 && z5) || (i6 == 3 && z6)) ? j2 + ((long) iZoneOffsetAt2) : ((i6 == 1 && z6) || (i6 == 3 && z5) || (i2 & 12) == 4) ? j2 + ((long) iZoneOffsetAt) : j2 + ((long) iZoneOffsetAt2);
                    }
                }
                if (jFloorDivide >= j2) {
                    break;
                }
                i3--;
                z2 = false;
            }
            iArr[0] = rawOffsetAt(i3) * 1000;
            iArr[1] = dstOffsetAt(i3) * 1000;
            return;
        }
        iArr[0] = initialRawOffset() * 1000;
        iArr[1] = initialDstOffset() * 1000;
    }

    private int getInt(byte b) {
        return b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
    }

    private int zoneOffsetAt(int i) {
        int i2 = i >= 0 ? getInt(this.typeMapData[i]) * 2 : 0;
        return this.typeOffsets[i2] + this.typeOffsets[i2 + 1];
    }

    private int rawOffsetAt(int i) {
        return this.typeOffsets[i >= 0 ? getInt(this.typeMapData[i]) * 2 : 0];
    }

    private int dstOffsetAt(int i) {
        return this.typeOffsets[(i >= 0 ? getInt(this.typeMapData[i]) * 2 : 0) + 1];
    }

    private int initialRawOffset() {
        return this.typeOffsets[0];
    }

    private int initialDstOffset() {
        return this.typeOffsets[1];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append('[');
        sb.append("transitionCount=" + this.transitionCount);
        sb.append(",typeCount=" + this.typeCount);
        sb.append(",transitionTimes=");
        if (this.transitionTimes64 != null) {
            sb.append('[');
            for (int i = 0; i < this.transitionTimes64.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(Long.toString(this.transitionTimes64[i]));
            }
            sb.append(']');
        } else {
            sb.append("null");
        }
        sb.append(",typeOffsets=");
        if (this.typeOffsets != null) {
            sb.append('[');
            for (int i2 = 0; i2 < this.typeOffsets.length; i2++) {
                if (i2 > 0) {
                    sb.append(',');
                }
                sb.append(Integer.toString(this.typeOffsets[i2]));
            }
            sb.append(']');
        } else {
            sb.append("null");
        }
        sb.append(",typeMapData=");
        if (this.typeMapData != null) {
            sb.append('[');
            for (int i3 = 0; i3 < this.typeMapData.length; i3++) {
                if (i3 > 0) {
                    sb.append(',');
                }
                sb.append(Byte.toString(this.typeMapData[i3]));
            }
        } else {
            sb.append("null");
        }
        sb.append(",finalStartYear=" + this.finalStartYear);
        sb.append(",finalStartMillis=" + this.finalStartMillis);
        sb.append(",finalZone=" + this.finalZone);
        sb.append(']');
        return sb.toString();
    }

    private static UResourceBundle loadRule(UResourceBundle uResourceBundle, String str) {
        return uResourceBundle.get("Rules").get(str);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        OlsonTimeZone olsonTimeZone = (OlsonTimeZone) obj;
        if (!Utility.arrayEquals(this.typeMapData, (Object) olsonTimeZone.typeMapData)) {
            if (this.finalStartYear != olsonTimeZone.finalStartYear) {
                return false;
            }
            if ((this.finalZone != null || olsonTimeZone.finalZone != null) && (this.finalZone == null || olsonTimeZone.finalZone == null || !this.finalZone.equals(olsonTimeZone.finalZone) || this.transitionCount != olsonTimeZone.transitionCount || this.typeCount != olsonTimeZone.typeCount || !Utility.arrayEquals((Object) this.transitionTimes64, (Object) olsonTimeZone.transitionTimes64) || !Utility.arrayEquals(this.typeOffsets, (Object) olsonTimeZone.typeOffsets) || !Utility.arrayEquals(this.typeMapData, (Object) olsonTimeZone.typeMapData))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int iDoubleToLongBits = (int) (((long) ((this.finalStartYear ^ ((this.finalStartYear >>> 4) + this.transitionCount)) ^ ((this.transitionCount >>> 6) + this.typeCount))) ^ (((((long) (this.typeCount >>> 8)) + Double.doubleToLongBits(this.finalStartMillis)) + ((long) (this.finalZone == null ? 0 : this.finalZone.hashCode()))) + ((long) super.hashCode())));
        if (this.transitionTimes64 != null) {
            int i = iDoubleToLongBits;
            for (int i2 = 0; i2 < this.transitionTimes64.length; i2++) {
                i = (int) (((long) i) + (this.transitionTimes64[i2] ^ (this.transitionTimes64[i2] >>> 8)));
            }
            iDoubleToLongBits = i;
        }
        int i3 = iDoubleToLongBits;
        for (int i4 = 0; i4 < this.typeOffsets.length; i4++) {
            i3 += this.typeOffsets[i4] ^ (this.typeOffsets[i4] >>> 8);
        }
        if (this.typeMapData != null) {
            for (int i5 = 0; i5 < this.typeMapData.length; i5++) {
                i3 += this.typeMapData[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            }
        }
        return i3;
    }

    @Override
    public TimeZoneTransition getNextTransition(long j, boolean z) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (z && j == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (j >= this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getNextTransition(j, z);
                }
                return null;
            }
        }
        if (this.historicRules == null) {
            return null;
        }
        int i = this.transitionCount;
        while (true) {
            i--;
            if (i < this.firstTZTransitionIdx) {
                break;
            }
            long j2 = this.transitionTimes64[i] * 1000;
            if (j > j2 || (!z && j == j2)) {
                break;
            }
        }
        if (i == this.transitionCount - 1) {
            return this.firstFinalTZTransition;
        }
        if (i < this.firstTZTransitionIdx) {
            return this.firstTZTransition;
        }
        int i2 = i + 1;
        TimeArrayTimeZoneRule timeArrayTimeZoneRule = this.historicRules[getInt(this.typeMapData[i2])];
        TimeArrayTimeZoneRule timeArrayTimeZoneRule2 = this.historicRules[getInt(this.typeMapData[i])];
        long j3 = this.transitionTimes64[i2] * 1000;
        if (timeArrayTimeZoneRule2.getName().equals(timeArrayTimeZoneRule.getName()) && timeArrayTimeZoneRule2.getRawOffset() == timeArrayTimeZoneRule.getRawOffset() && timeArrayTimeZoneRule2.getDSTSavings() == timeArrayTimeZoneRule.getDSTSavings()) {
            return getNextTransition(j3, false);
        }
        return new TimeZoneTransition(j3, timeArrayTimeZoneRule2, timeArrayTimeZoneRule);
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long j, boolean z) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (z && j == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (j > this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getPreviousTransition(j, z);
                }
                return this.firstFinalTZTransition;
            }
        }
        if (this.historicRules == null) {
            return null;
        }
        int i = this.transitionCount;
        while (true) {
            i--;
            if (i < this.firstTZTransitionIdx) {
                break;
            }
            long j2 = this.transitionTimes64[i] * 1000;
            if (j > j2 || (z && j == j2)) {
                break;
            }
        }
        if (i < this.firstTZTransitionIdx) {
            return null;
        }
        if (i == this.firstTZTransitionIdx) {
            return this.firstTZTransition;
        }
        TimeArrayTimeZoneRule timeArrayTimeZoneRule = this.historicRules[getInt(this.typeMapData[i])];
        TimeArrayTimeZoneRule timeArrayTimeZoneRule2 = this.historicRules[getInt(this.typeMapData[i - 1])];
        long j3 = this.transitionTimes64[i] * 1000;
        if (timeArrayTimeZoneRule2.getName().equals(timeArrayTimeZoneRule.getName()) && timeArrayTimeZoneRule2.getRawOffset() == timeArrayTimeZoneRule.getRawOffset() && timeArrayTimeZoneRule2.getDSTSavings() == timeArrayTimeZoneRule.getDSTSavings()) {
            return getPreviousTransition(j3, false);
        }
        return new TimeZoneTransition(j3, timeArrayTimeZoneRule2, timeArrayTimeZoneRule);
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        int i;
        int i2;
        initTransitionRules();
        if (this.historicRules != null) {
            i = 1;
            for (int i3 = 0; i3 < this.historicRules.length; i3++) {
                if (this.historicRules[i3] != null) {
                    i++;
                }
            }
        } else {
            i = 1;
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                i += 2;
            } else {
                i++;
            }
        }
        TimeZoneRule[] timeZoneRuleArr = new TimeZoneRule[i];
        timeZoneRuleArr[0] = this.initialRule;
        if (this.historicRules != null) {
            i2 = 1;
            for (int i4 = 0; i4 < this.historicRules.length; i4++) {
                if (this.historicRules[i4] != null) {
                    timeZoneRuleArr[i2] = this.historicRules[i4];
                    i2++;
                }
            }
        } else {
            i2 = 1;
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                TimeZoneRule[] timeZoneRules = this.finalZoneWithStartYear.getTimeZoneRules();
                timeZoneRuleArr[i2] = timeZoneRules[1];
                timeZoneRuleArr[i2 + 1] = timeZoneRules[2];
            } else {
                timeZoneRuleArr[i2] = new TimeArrayTimeZoneRule(getID() + "(STD)", this.finalZone.getRawOffset(), 0, new long[]{(long) this.finalStartMillis}, 2);
            }
        }
        return timeZoneRuleArr;
    }

    private synchronized void initTransitionRules() {
        TimeZoneRule timeArrayTimeZoneRule;
        long time;
        if (this.transitionRulesInitialized) {
            return;
        }
        TimeZoneRule timeZoneRule = null;
        this.initialRule = null;
        this.firstTZTransition = null;
        this.firstFinalTZTransition = null;
        this.historicRules = null;
        this.firstTZTransitionIdx = 0;
        this.finalZoneWithStartYear = null;
        String str = getID() + "(STD)";
        String str2 = getID() + "(DST)";
        int iInitialRawOffset = initialRawOffset() * 1000;
        int iInitialDstOffset = initialDstOffset() * 1000;
        this.initialRule = new InitialTimeZoneRule(iInitialDstOffset == 0 ? str : str2, iInitialRawOffset, iInitialDstOffset);
        if (this.transitionCount > 0) {
            int i = 0;
            while (i < this.transitionCount && getInt(this.typeMapData[i]) == 0) {
                this.firstTZTransitionIdx++;
                i++;
            }
            if (i != this.transitionCount) {
                long[] jArr = new long[this.transitionCount];
                int i2 = 0;
                while (true) {
                    long j = 1000;
                    if (i2 >= this.typeCount) {
                        break;
                    }
                    int i3 = this.firstTZTransitionIdx;
                    int i4 = 0;
                    while (i3 < this.transitionCount) {
                        if (i2 == getInt(this.typeMapData[i3])) {
                            long j2 = this.transitionTimes64[i3] * j;
                            if (j2 < this.finalStartMillis) {
                                jArr[i4] = j2;
                                i4++;
                            }
                        }
                        i3++;
                        j = 1000;
                    }
                    if (i4 > 0) {
                        long[] jArr2 = new long[i4];
                        System.arraycopy(jArr, 0, jArr2, 0, i4);
                        int i5 = i2 * 2;
                        int i6 = this.typeOffsets[i5] * 1000;
                        int i7 = this.typeOffsets[i5 + 1] * 1000;
                        if (this.historicRules == null) {
                            this.historicRules = new TimeArrayTimeZoneRule[this.typeCount];
                        }
                        this.historicRules[i2] = new TimeArrayTimeZoneRule(i7 == 0 ? str : str2, i6, i7, jArr2, 2);
                    }
                    i2++;
                }
                this.firstTZTransition = new TimeZoneTransition(this.transitionTimes64[this.firstTZTransitionIdx] * 1000, this.initialRule, this.historicRules[getInt(this.typeMapData[this.firstTZTransitionIdx])]);
            }
        }
        if (this.finalZone != null) {
            long j3 = (long) this.finalStartMillis;
            if (this.finalZone.useDaylightTime()) {
                this.finalZoneWithStartYear = (SimpleTimeZone) this.finalZone.clone();
                this.finalZoneWithStartYear.setStartYear(this.finalStartYear);
                TimeZoneTransition nextTransition = this.finalZoneWithStartYear.getNextTransition(j3, false);
                timeArrayTimeZoneRule = nextTransition.getTo();
                time = nextTransition.getTime();
            } else {
                this.finalZoneWithStartYear = this.finalZone;
                timeArrayTimeZoneRule = new TimeArrayTimeZoneRule(this.finalZone.getID(), this.finalZone.getRawOffset(), 0, new long[]{j3}, 2);
                time = j3;
            }
            if (this.transitionCount > 0) {
                timeZoneRule = this.historicRules[getInt(this.typeMapData[this.transitionCount - 1])];
            }
            if (timeZoneRule == null) {
                timeZoneRule = this.initialRule;
            }
            this.firstFinalTZTransition = new TimeZoneTransition(time, timeZoneRule, timeArrayTimeZoneRule);
        }
        this.transitionRulesInitialized = true;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        boolean z = true;
        if (this.serialVersionOnStream < 1) {
            String id = getID();
            if (id != null) {
                try {
                    UResourceBundle bundleInstance = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    construct(bundleInstance, ZoneMeta.openOlsonResource(bundleInstance, id));
                    if (this.finalZone != null) {
                        this.finalZone.setID(id);
                    }
                } catch (Exception e) {
                    z = false;
                }
                if (!z) {
                    constructEmpty();
                }
            } else {
                z = false;
                if (!z) {
                }
            }
        }
        this.transitionRulesInitialized = false;
    }

    @Override
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    @Override
    public TimeZone cloneAsThawed() {
        OlsonTimeZone olsonTimeZone = (OlsonTimeZone) super.cloneAsThawed();
        if (this.finalZone != null) {
            this.finalZone.setID(getID());
            olsonTimeZone.finalZone = (SimpleTimeZone) this.finalZone.clone();
        }
        olsonTimeZone.isFrozen = false;
        return olsonTimeZone;
    }
}
