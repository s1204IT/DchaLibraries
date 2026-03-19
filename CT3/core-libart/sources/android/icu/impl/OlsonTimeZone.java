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

    static final boolean f14assertionsDisabled;
    private static final boolean DEBUG;
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
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (month < 0 || month > 11) {
            throw new IllegalArgumentException("Month is not in the legal range: " + month);
        }
        return getOffset(era, year, month, day, dayOfWeek, milliseconds, Grego.monthLength(year, month));
    }

    public int getOffset(int era, int year, int month, int dom, int dow, int millis, int monthLength) {
        if ((era != 1 && era != 0) || month < 0 || month > 11 || dom < 1 || dom > monthLength || dow < 1 || dow > 7 || millis < 0 || millis >= 86400000 || monthLength < 28 || monthLength > 31) {
            throw new IllegalArgumentException();
        }
        if (era == 0) {
            year = -year;
        }
        if (this.finalZone != null && year >= this.finalStartYear) {
            return this.finalZone.getOffset(era, year, month, dom, dow, millis);
        }
        long time = (Grego.fieldsToDay(year, month, dom) * 86400000) + ((long) millis);
        int[] offsets = new int[2];
        getHistoricalOffset(time, true, 3, 1, offsets);
        return offsets[0] + offsets[1];
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        DateTimeRule start;
        DateTimeRule end;
        int sav;
        TimeZoneTransition tzt;
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        }
        if (getRawOffset() == offsetMillis) {
            return;
        }
        long current = System.currentTimeMillis();
        if (current < this.finalStartMillis) {
            SimpleTimeZone stz = new SimpleTimeZone(offsetMillis, getID());
            boolean bDst = useDaylightTime();
            if (bDst) {
                TimeZoneRule[] currentRules = getSimpleTimeZoneRulesNear(current);
                if (currentRules.length != 3 && (tzt = getPreviousTransition(current, false)) != null) {
                    currentRules = getSimpleTimeZoneRulesNear(tzt.getTime() - 1);
                }
                if (currentRules.length == 3 && (currentRules[1] instanceof AnnualTimeZoneRule) && (currentRules[2] instanceof AnnualTimeZoneRule)) {
                    AnnualTimeZoneRule r1 = (AnnualTimeZoneRule) currentRules[1];
                    AnnualTimeZoneRule r2 = (AnnualTimeZoneRule) currentRules[2];
                    int offset1 = r1.getRawOffset() + r1.getDSTSavings();
                    int offset2 = r2.getRawOffset() + r2.getDSTSavings();
                    if (offset1 > offset2) {
                        start = r1.getRule();
                        end = r2.getRule();
                        sav = offset1 - offset2;
                    } else {
                        start = r2.getRule();
                        end = r1.getRule();
                        sav = offset2 - offset1;
                    }
                    stz.setStartRule(start.getRuleMonth(), start.getRuleWeekInMonth(), start.getRuleDayOfWeek(), start.getRuleMillisInDay());
                    stz.setEndRule(end.getRuleMonth(), end.getRuleWeekInMonth(), end.getRuleDayOfWeek(), end.getRuleMillisInDay());
                    stz.setDSTSavings(sav);
                } else {
                    stz.setStartRule(0, 1, 0);
                    stz.setEndRule(11, 31, 86399999);
                }
            }
            int[] fields = Grego.timeToFields(current, null);
            this.finalStartYear = fields[0];
            this.finalStartMillis = Grego.fieldsToDay(fields[0], 0, 1);
            if (bDst) {
                stz.setStartYear(this.finalStartYear);
            }
            this.finalZone = stz;
        } else {
            this.finalZone.setRawOffset(offsetMillis);
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
    public void getOffset(long date, boolean local, int[] offsets) {
        if (this.finalZone != null && date >= this.finalStartMillis) {
            this.finalZone.getOffset(date, local, offsets);
        } else {
            getHistoricalOffset(date, local, 4, 12, offsets);
        }
    }

    @Override
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        if (this.finalZone != null && date >= this.finalStartMillis) {
            this.finalZone.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        } else {
            getHistoricalOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
        }
    }

    @Override
    public int getRawOffset() {
        int[] ret = new int[2];
        getOffset(System.currentTimeMillis(), false, ret);
        return ret[0];
    }

    @Override
    public boolean useDaylightTime() {
        long current = System.currentTimeMillis();
        if (this.finalZone != null && current >= this.finalStartMillis) {
            if (this.finalZone != null) {
                return this.finalZone.useDaylightTime();
            }
            return false;
        }
        int[] fields = Grego.timeToFields(current, null);
        long start = Grego.fieldsToDay(fields[0], 0, 1) * 86400;
        long limit = Grego.fieldsToDay(fields[0] + 1, 0, 1) * 86400;
        for (int i = 0; i < this.transitionCount && this.transitionTimes64[i] < limit; i++) {
            if (this.transitionTimes64[i] >= start && dstOffsetAt(i) != 0) {
                return true;
            }
            if (this.transitionTimes64[i] > start && i > 0 && dstOffsetAt(i - 1) != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean observesDaylightTime() {
        long current = System.currentTimeMillis();
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                return true;
            }
            if (current >= this.finalStartMillis) {
                return false;
            }
        }
        long currentSec = Grego.floorDivide(current, 1000L);
        int trsIdx = this.transitionCount - 1;
        if (dstOffsetAt(trsIdx) != 0) {
            return true;
        }
        while (trsIdx >= 0 && this.transitionTimes64[trsIdx] > currentSec) {
            if (dstOffsetAt(trsIdx - 1) != 0) {
                return true;
            }
            trsIdx--;
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
        int[] temp = new int[2];
        getOffset(date.getTime(), false, temp);
        return temp[1] != 0;
    }

    @Override
    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!super.hasSameRules(other) || !(other instanceof OlsonTimeZone)) {
            return false;
        }
        OlsonTimeZone o = (OlsonTimeZone) other;
        if (this.finalZone == null) {
            if (o.finalZone != null) {
                return false;
            }
        } else if (o.finalZone == null || this.finalStartYear != o.finalStartYear || !this.finalZone.hasSameRules(o.finalZone)) {
            return false;
        }
        return this.transitionCount == o.transitionCount && Arrays.equals(this.transitionTimes64, o.transitionTimes64) && this.typeCount == o.typeCount && Arrays.equals(this.typeMapData, o.typeMapData) && Arrays.equals(this.typeOffsets, o.typeOffsets);
    }

    public String getCanonicalID() {
        if (this.canonicalID == null) {
            synchronized (this) {
                if (this.canonicalID == null) {
                    this.canonicalID = getCanonicalID(getID());
                    if (!f14assertionsDisabled) {
                        if (!(this.canonicalID != null)) {
                            throw new AssertionError();
                        }
                    }
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

    public OlsonTimeZone(UResourceBundle top, UResourceBundle res, String id) {
        super(id);
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.finalZone = null;
        this.canonicalID = null;
        this.serialVersionOnStream = 1;
        this.isFrozen = false;
        construct(top, res);
    }

    private void construct(UResourceBundle top, UResourceBundle res) {
        if (top == null || res == null) {
            throw new IllegalArgumentException();
        }
        if (DEBUG) {
            System.out.println("OlsonTimeZone(" + res.getKey() + ")");
        }
        int[] transPost32 = null;
        int[] trans32 = null;
        int[] transPre32 = null;
        this.transitionCount = 0;
        try {
            UResourceBundle r = res.get("transPre32");
            transPre32 = r.getIntVector();
        } catch (MissingResourceException e) {
        }
        if (transPre32.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        this.transitionCount += transPre32.length / 2;
        try {
            UResourceBundle r2 = res.get("trans");
            trans32 = r2.getIntVector();
            this.transitionCount += trans32.length;
        } catch (MissingResourceException e2) {
        }
        try {
            UResourceBundle r3 = res.get("transPost32");
            transPost32 = r3.getIntVector();
        } catch (MissingResourceException e3) {
        }
        if (transPost32.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        this.transitionCount += transPost32.length / 2;
        if (this.transitionCount > 0) {
            this.transitionTimes64 = new long[this.transitionCount];
            int idx = 0;
            if (transPre32 != null) {
                int i = 0;
                while (i < transPre32.length / 2) {
                    this.transitionTimes64[idx] = ((((long) transPre32[i * 2]) & 4294967295L) << 32) | (((long) transPre32[(i * 2) + 1]) & 4294967295L);
                    i++;
                    idx++;
                }
            }
            if (trans32 != null) {
                int i2 = 0;
                while (i2 < trans32.length) {
                    this.transitionTimes64[idx] = trans32[i2];
                    i2++;
                    idx++;
                }
            }
            if (transPost32 != null) {
                int i3 = 0;
                while (i3 < transPost32.length / 2) {
                    this.transitionTimes64[idx] = ((((long) transPost32[i3 * 2]) & 4294967295L) << 32) | (((long) transPost32[(i3 * 2) + 1]) & 4294967295L);
                    i3++;
                    idx++;
                }
            }
        } else {
            this.transitionTimes64 = null;
        }
        UResourceBundle r4 = res.get("typeOffsets");
        this.typeOffsets = r4.getIntVector();
        if (this.typeOffsets.length < 2 || this.typeOffsets.length > 32766 || this.typeOffsets.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid Format");
        }
        this.typeCount = this.typeOffsets.length / 2;
        if (this.transitionCount > 0) {
            UResourceBundle r5 = res.get("typeMap");
            this.typeMapData = r5.getBinary(null);
            if (this.typeMapData.length != this.transitionCount) {
                throw new IllegalArgumentException("Invalid Format");
            }
        } else {
            this.typeMapData = null;
        }
        this.finalZone = null;
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        try {
            String ruleID = res.getString("finalRule");
            UResourceBundle r6 = res.get("finalRaw");
            int ruleRaw = r6.getInt() * 1000;
            UResourceBundle r7 = loadRule(top, ruleID);
            int[] ruleData = r7.getIntVector();
            if (ruleData == null || ruleData.length != 11) {
                throw new IllegalArgumentException("Invalid Format");
            }
            this.finalZone = new SimpleTimeZone(ruleRaw, "", ruleData[0], ruleData[1], ruleData[2], ruleData[3] * 1000, ruleData[4], ruleData[5], ruleData[6], ruleData[7], ruleData[8] * 1000, ruleData[9], ruleData[10] * 1000);
            UResourceBundle r8 = res.get("finalYear");
            this.finalStartYear = r8.getInt();
            this.finalStartMillis = Grego.fieldsToDay(this.finalStartYear, 0, 1) * 86400000;
        } catch (MissingResourceException e4) {
            if (0 == 0) {
            } else {
                throw new IllegalArgumentException("Invalid Format");
            }
        }
    }

    public OlsonTimeZone(String id) {
        super(id);
        this.finalStartYear = Integer.MAX_VALUE;
        this.finalStartMillis = Double.MAX_VALUE;
        this.finalZone = null;
        this.canonicalID = null;
        this.serialVersionOnStream = 1;
        this.isFrozen = false;
        UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle res = ZoneMeta.openOlsonResource(top, id);
        construct(top, res);
        if (this.finalZone == null) {
            return;
        }
        this.finalZone.setID(id);
    }

    @Override
    public void setID(String id) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen OlsonTimeZone instance.");
        }
        if (this.canonicalID == null) {
            this.canonicalID = getCanonicalID(getID());
            if (!f14assertionsDisabled) {
                if (!(this.canonicalID != null)) {
                    throw new AssertionError();
                }
            }
            if (this.canonicalID == null) {
                this.canonicalID = getID();
            }
        }
        if (this.finalZone != null) {
            this.finalZone.setID(id);
        }
        super.setID(id);
        this.transitionRulesInitialized = false;
    }

    private void getHistoricalOffset(long date, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        if (this.transitionCount != 0) {
            long sec = Grego.floorDivide(date, 1000L);
            if (!local && sec < this.transitionTimes64[0]) {
                offsets[0] = initialRawOffset() * 1000;
                offsets[1] = initialDstOffset() * 1000;
                return;
            }
            int transIdx = this.transitionCount - 1;
            while (transIdx >= 0) {
                long transition = this.transitionTimes64[transIdx];
                if (local && sec >= transition - 86400) {
                    int offsetBefore = zoneOffsetAt(transIdx - 1);
                    boolean dstBefore = dstOffsetAt(transIdx + (-1)) != 0;
                    int offsetAfter = zoneOffsetAt(transIdx);
                    boolean dstAfter = dstOffsetAt(transIdx) != 0;
                    boolean dstToStd = dstBefore && !dstAfter;
                    boolean z = !dstBefore ? dstAfter : false;
                    transition = offsetAfter - offsetBefore >= 0 ? (((NonExistingTimeOpt & 3) == 1 && dstToStd) || ((NonExistingTimeOpt & 3) == 3 && z)) ? transition + ((long) offsetBefore) : (!((NonExistingTimeOpt & 3) == 1 && z) && !((NonExistingTimeOpt & 3) == 3 && dstToStd) && (NonExistingTimeOpt & 12) == 12) ? transition + ((long) offsetBefore) : transition + ((long) offsetAfter) : (((DuplicatedTimeOpt & 3) == 1 && dstToStd) || ((DuplicatedTimeOpt & 3) == 3 && z)) ? transition + ((long) offsetAfter) : (((DuplicatedTimeOpt & 3) == 1 && z) || ((DuplicatedTimeOpt & 3) == 3 && dstToStd) || (DuplicatedTimeOpt & 12) == 4) ? transition + ((long) offsetBefore) : transition + ((long) offsetAfter);
                }
                if (sec >= transition) {
                    break;
                } else {
                    transIdx--;
                }
            }
            offsets[0] = rawOffsetAt(transIdx) * 1000;
            offsets[1] = dstOffsetAt(transIdx) * 1000;
            return;
        }
        offsets[0] = initialRawOffset() * 1000;
        offsets[1] = initialDstOffset() * 1000;
    }

    private int getInt(byte val) {
        return val & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
    }

    private int zoneOffsetAt(int transIdx) {
        int typeIdx = transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0;
        return this.typeOffsets[typeIdx] + this.typeOffsets[typeIdx + 1];
    }

    private int rawOffsetAt(int transIdx) {
        int typeIdx = transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0;
        return this.typeOffsets[typeIdx];
    }

    private int dstOffsetAt(int transIdx) {
        int typeIdx = transIdx >= 0 ? getInt(this.typeMapData[transIdx]) * 2 : 0;
        return this.typeOffsets[typeIdx + 1];
    }

    private int initialRawOffset() {
        return this.typeOffsets[0];
    }

    private int initialDstOffset() {
        return this.typeOffsets[1];
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append('[');
        buf.append("transitionCount=").append(this.transitionCount);
        buf.append(",typeCount=").append(this.typeCount);
        buf.append(",transitionTimes=");
        if (this.transitionTimes64 != null) {
            buf.append('[');
            for (int i = 0; i < this.transitionTimes64.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                buf.append(Long.toString(this.transitionTimes64[i]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",typeOffsets=");
        if (this.typeOffsets != null) {
            buf.append('[');
            for (int i2 = 0; i2 < this.typeOffsets.length; i2++) {
                if (i2 > 0) {
                    buf.append(',');
                }
                buf.append(Integer.toString(this.typeOffsets[i2]));
            }
            buf.append(']');
        } else {
            buf.append("null");
        }
        buf.append(",typeMapData=");
        if (this.typeMapData != null) {
            buf.append('[');
            for (int i3 = 0; i3 < this.typeMapData.length; i3++) {
                if (i3 > 0) {
                    buf.append(',');
                }
                buf.append(Byte.toString(this.typeMapData[i3]));
            }
        } else {
            buf.append("null");
        }
        buf.append(",finalStartYear=").append(this.finalStartYear);
        buf.append(",finalStartMillis=").append(this.finalStartMillis);
        buf.append(",finalZone=").append(this.finalZone);
        buf.append(']');
        return buf.toString();
    }

    static {
        f14assertionsDisabled = !OlsonTimeZone.class.desiredAssertionStatus();
        DEBUG = ICUDebug.enabled("olson");
    }

    private static UResourceBundle loadRule(UResourceBundle top, String ruleid) {
        UResourceBundle r = top.get("Rules");
        return r.get(ruleid);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        OlsonTimeZone z = (OlsonTimeZone) obj;
        if (Utility.arrayEquals(this.typeMapData, (Object) z.typeMapData)) {
            return true;
        }
        if (this.finalStartYear != z.finalStartYear) {
            return false;
        }
        if (this.finalZone == null && z.finalZone == null) {
            return true;
        }
        if (this.finalZone != null && z.finalZone != null && this.finalZone.equals(z.finalZone) && this.transitionCount == z.transitionCount && this.typeCount == z.typeCount && Utility.arrayEquals((Object) this.transitionTimes64, (Object) z.transitionTimes64) && Utility.arrayEquals(this.typeOffsets, (Object) z.typeOffsets)) {
            return Utility.arrayEquals(this.typeMapData, (Object) z.typeMapData);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int ret = (int) (((((long) (this.finalZone == null ? 0 : this.finalZone.hashCode())) + (Double.doubleToLongBits(this.finalStartMillis) + ((long) (this.typeCount >>> 8)))) + ((long) super.hashCode())) ^ ((long) ((this.finalStartYear ^ ((this.finalStartYear >>> 4) + this.transitionCount)) ^ ((this.transitionCount >>> 6) + this.typeCount))));
        if (this.transitionTimes64 != null) {
            for (int i = 0; i < this.transitionTimes64.length; i++) {
                ret = (int) (((long) ret) + (this.transitionTimes64[i] ^ (this.transitionTimes64[i] >>> 8)));
            }
        }
        for (int i2 = 0; i2 < this.typeOffsets.length; i2++) {
            ret += this.typeOffsets[i2] ^ (this.typeOffsets[i2] >>> 8);
        }
        if (this.typeMapData != null) {
            for (int i3 = 0; i3 < this.typeMapData.length; i3++) {
                ret += this.typeMapData[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            }
        }
        return ret;
    }

    @Override
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (inclusive && base == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (base >= this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getNextTransition(base, inclusive);
                }
                return null;
            }
        }
        if (this.historicRules != null) {
            int ttidx = this.transitionCount - 1;
            while (ttidx >= this.firstTZTransitionIdx) {
                long t = this.transitionTimes64[ttidx] * 1000;
                if (base > t || (!inclusive && base == t)) {
                    break;
                }
                ttidx--;
            }
            if (ttidx == this.transitionCount - 1) {
                return this.firstFinalTZTransition;
            }
            if (ttidx < this.firstTZTransitionIdx) {
                return this.firstTZTransition;
            }
            TimeZoneRule to = this.historicRules[getInt(this.typeMapData[ttidx + 1])];
            TimeZoneRule from = this.historicRules[getInt(this.typeMapData[ttidx])];
            long startTime = this.transitionTimes64[ttidx + 1] * 1000;
            if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
                return getNextTransition(startTime, false);
            }
            return new TimeZoneTransition(startTime, from, to);
        }
        return null;
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        initTransitionRules();
        if (this.finalZone != null) {
            if (inclusive && base == this.firstFinalTZTransition.getTime()) {
                return this.firstFinalTZTransition;
            }
            if (base > this.firstFinalTZTransition.getTime()) {
                if (this.finalZone.useDaylightTime()) {
                    return this.finalZoneWithStartYear.getPreviousTransition(base, inclusive);
                }
                return this.firstFinalTZTransition;
            }
        }
        if (this.historicRules != null) {
            int ttidx = this.transitionCount - 1;
            while (ttidx >= this.firstTZTransitionIdx) {
                long t = this.transitionTimes64[ttidx] * 1000;
                if (base > t || (inclusive && base == t)) {
                    break;
                }
                ttidx--;
            }
            if (ttidx < this.firstTZTransitionIdx) {
                return null;
            }
            if (ttidx == this.firstTZTransitionIdx) {
                return this.firstTZTransition;
            }
            TimeZoneRule to = this.historicRules[getInt(this.typeMapData[ttidx])];
            TimeZoneRule from = this.historicRules[getInt(this.typeMapData[ttidx - 1])];
            long startTime = this.transitionTimes64[ttidx] * 1000;
            if (from.getName().equals(to.getName()) && from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
                return getPreviousTransition(startTime, false);
            }
            return new TimeZoneTransition(startTime, from, to);
        }
        return null;
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        initTransitionRules();
        int size = 1;
        if (this.historicRules != null) {
            for (int i = 0; i < this.historicRules.length; i++) {
                if (this.historicRules[i] != null) {
                    size++;
                }
            }
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] rules = new TimeZoneRule[size];
        int idx = 1;
        rules[0] = this.initialRule;
        if (this.historicRules != null) {
            for (int i2 = 0; i2 < this.historicRules.length; i2++) {
                if (this.historicRules[i2] != null) {
                    rules[idx] = this.historicRules[i2];
                    idx++;
                }
            }
        }
        if (this.finalZone != null) {
            if (this.finalZone.useDaylightTime()) {
                TimeZoneRule[] stzr = this.finalZoneWithStartYear.getTimeZoneRules();
                int idx2 = idx + 1;
                rules[idx] = stzr[1];
                int i3 = idx2 + 1;
                rules[idx2] = stzr[2];
            } else {
                int i4 = idx + 1;
                rules[idx] = new TimeArrayTimeZoneRule(getID() + "(STD)", this.finalZone.getRawOffset(), 0, new long[]{(long) this.finalStartMillis}, 2);
            }
        }
        return rules;
    }

    private synchronized void initTransitionRules() {
        TimeZoneRule firstFinalRule;
        int nTimes;
        if (this.transitionRulesInitialized) {
            return;
        }
        this.initialRule = null;
        this.firstTZTransition = null;
        this.firstFinalTZTransition = null;
        this.historicRules = null;
        this.firstTZTransitionIdx = 0;
        this.finalZoneWithStartYear = null;
        String stdName = getID() + "(STD)";
        String dstName = getID() + "(DST)";
        int raw = initialRawOffset() * 1000;
        int dst = initialDstOffset() * 1000;
        this.initialRule = new InitialTimeZoneRule(dst == 0 ? stdName : dstName, raw, dst);
        if (this.transitionCount > 0) {
            int transitionIdx = 0;
            while (transitionIdx < this.transitionCount && getInt(this.typeMapData[transitionIdx]) == 0) {
                this.firstTZTransitionIdx++;
                transitionIdx++;
            }
            if (transitionIdx != this.transitionCount) {
                long[] times = new long[this.transitionCount];
                for (int typeIdx = 0; typeIdx < this.typeCount; typeIdx++) {
                    int transitionIdx2 = this.firstTZTransitionIdx;
                    int nTimes2 = 0;
                    while (transitionIdx2 < this.transitionCount) {
                        if (typeIdx == getInt(this.typeMapData[transitionIdx2])) {
                            long tt = this.transitionTimes64[transitionIdx2] * 1000;
                            if (tt < this.finalStartMillis) {
                                nTimes = nTimes2 + 1;
                                times[nTimes2] = tt;
                            } else {
                                nTimes = nTimes2;
                            }
                        }
                        transitionIdx2++;
                        nTimes2 = nTimes;
                    }
                    if (nTimes2 > 0) {
                        long[] startTimes = new long[nTimes2];
                        System.arraycopy(times, 0, startTimes, 0, nTimes2);
                        int raw2 = this.typeOffsets[typeIdx * 2] * 1000;
                        int dst2 = this.typeOffsets[(typeIdx * 2) + 1] * 1000;
                        if (this.historicRules == null) {
                            this.historicRules = new TimeArrayTimeZoneRule[this.typeCount];
                        }
                        this.historicRules[typeIdx] = new TimeArrayTimeZoneRule(dst2 == 0 ? stdName : dstName, raw2, dst2, startTimes, 2);
                    }
                }
                int typeIdx2 = getInt(this.typeMapData[this.firstTZTransitionIdx]);
                this.firstTZTransition = new TimeZoneTransition(this.transitionTimes64[this.firstTZTransitionIdx] * 1000, this.initialRule, this.historicRules[typeIdx2]);
            }
        }
        if (this.finalZone != null) {
            long startTime = (long) this.finalStartMillis;
            if (this.finalZone.useDaylightTime()) {
                this.finalZoneWithStartYear = (SimpleTimeZone) this.finalZone.clone();
                this.finalZoneWithStartYear.setStartYear(this.finalStartYear);
                TimeZoneTransition tzt = this.finalZoneWithStartYear.getNextTransition(startTime, false);
                firstFinalRule = tzt.getTo();
                startTime = tzt.getTime();
            } else {
                this.finalZoneWithStartYear = this.finalZone;
                firstFinalRule = new TimeArrayTimeZoneRule(this.finalZone.getID(), this.finalZone.getRawOffset(), 0, new long[]{startTime}, 2);
            }
            TimeZoneRule prevRule = null;
            if (this.transitionCount > 0) {
                prevRule = this.historicRules[getInt(this.typeMapData[this.transitionCount - 1])];
            }
            if (prevRule == null) {
                prevRule = this.initialRule;
            }
            this.firstFinalTZTransition = new TimeZoneTransition(startTime, prevRule, firstFinalRule);
        }
        this.transitionRulesInitialized = true;
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        if (this.serialVersionOnStream < 1) {
            boolean initialized = false;
            String tzid = getID();
            if (tzid != null) {
                try {
                    UResourceBundle top = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", ZONEINFORES, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    UResourceBundle res = ZoneMeta.openOlsonResource(top, tzid);
                    construct(top, res);
                    if (this.finalZone != null) {
                        this.finalZone.setID(tzid);
                    }
                    initialized = true;
                } catch (Exception e) {
                }
            }
            if (!initialized) {
                constructEmpty();
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
        OlsonTimeZone tz = (OlsonTimeZone) super.cloneAsThawed();
        if (this.finalZone != null) {
            this.finalZone.setID(getID());
            tz.finalZone = (SimpleTimeZone) this.finalZone.clone();
        }
        tz.isFrozen = false;
        return tz;
    }
}
