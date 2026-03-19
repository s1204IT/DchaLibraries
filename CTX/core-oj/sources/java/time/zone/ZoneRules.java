package java.time.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ZoneRules implements Serializable {
    private static final int LAST_CACHED_YEAR = 2100;
    private static final long serialVersionUID = 3044319355680032515L;
    private final ZoneOffsetTransitionRule[] lastRules;
    private final transient ConcurrentMap<Integer, ZoneOffsetTransition[]> lastRulesCache;
    private final long[] savingsInstantTransitions;
    private final LocalDateTime[] savingsLocalTransitions;
    private final ZoneOffset[] standardOffsets;
    private final long[] standardTransitions;
    private final ZoneOffset[] wallOffsets;
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final ZoneOffsetTransitionRule[] EMPTY_LASTRULES = new ZoneOffsetTransitionRule[0];
    private static final LocalDateTime[] EMPTY_LDT_ARRAY = new LocalDateTime[0];

    public static ZoneRules of(ZoneOffset zoneOffset, ZoneOffset zoneOffset2, List<ZoneOffsetTransition> list, List<ZoneOffsetTransition> list2, List<ZoneOffsetTransitionRule> list3) {
        Objects.requireNonNull(zoneOffset, "baseStandardOffset");
        Objects.requireNonNull(zoneOffset2, "baseWallOffset");
        Objects.requireNonNull(list, "standardOffsetTransitionList");
        Objects.requireNonNull(list2, "transitionList");
        Objects.requireNonNull(list3, "lastRules");
        return new ZoneRules(zoneOffset, zoneOffset2, list, list2, list3);
    }

    public static ZoneRules of(ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "offset");
        return new ZoneRules(zoneOffset);
    }

    ZoneRules(ZoneOffset zoneOffset, ZoneOffset zoneOffset2, List<ZoneOffsetTransition> list, List<ZoneOffsetTransition> list2, List<ZoneOffsetTransitionRule> list3) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardTransitions = new long[list.size()];
        this.standardOffsets = new ZoneOffset[list.size() + 1];
        this.standardOffsets[0] = zoneOffset;
        int i = 0;
        while (i < list.size()) {
            this.standardTransitions[i] = list.get(i).toEpochSecond();
            int i2 = i + 1;
            this.standardOffsets[i2] = list.get(i).getOffsetAfter();
            i = i2;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(zoneOffset2);
        for (ZoneOffsetTransition zoneOffsetTransition : list2) {
            if (zoneOffsetTransition.isGap()) {
                arrayList.add(zoneOffsetTransition.getDateTimeBefore());
                arrayList.add(zoneOffsetTransition.getDateTimeAfter());
            } else {
                arrayList.add(zoneOffsetTransition.getDateTimeAfter());
                arrayList.add(zoneOffsetTransition.getDateTimeBefore());
            }
            arrayList2.add(zoneOffsetTransition.getOffsetAfter());
        }
        this.savingsLocalTransitions = (LocalDateTime[]) arrayList.toArray(new LocalDateTime[arrayList.size()]);
        this.wallOffsets = (ZoneOffset[]) arrayList2.toArray(new ZoneOffset[arrayList2.size()]);
        this.savingsInstantTransitions = new long[list2.size()];
        for (int i3 = 0; i3 < list2.size(); i3++) {
            this.savingsInstantTransitions[i3] = list2.get(i3).toEpochSecond();
        }
        if (list3.size() > 16) {
            throw new IllegalArgumentException("Too many transition rules");
        }
        this.lastRules = (ZoneOffsetTransitionRule[]) list3.toArray(new ZoneOffsetTransitionRule[list3.size()]);
    }

    private ZoneRules(long[] jArr, ZoneOffset[] zoneOffsetArr, long[] jArr2, ZoneOffset[] zoneOffsetArr2, ZoneOffsetTransitionRule[] zoneOffsetTransitionRuleArr) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardTransitions = jArr;
        this.standardOffsets = zoneOffsetArr;
        this.savingsInstantTransitions = jArr2;
        this.wallOffsets = zoneOffsetArr2;
        this.lastRules = zoneOffsetTransitionRuleArr;
        if (jArr2.length == 0) {
            this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
            return;
        }
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i < jArr2.length) {
            int i2 = i + 1;
            ZoneOffsetTransition zoneOffsetTransition = new ZoneOffsetTransition(jArr2[i], zoneOffsetArr2[i], zoneOffsetArr2[i2]);
            if (zoneOffsetTransition.isGap()) {
                arrayList.add(zoneOffsetTransition.getDateTimeBefore());
                arrayList.add(zoneOffsetTransition.getDateTimeAfter());
            } else {
                arrayList.add(zoneOffsetTransition.getDateTimeAfter());
                arrayList.add(zoneOffsetTransition.getDateTimeBefore());
            }
            i = i2;
        }
        this.savingsLocalTransitions = (LocalDateTime[]) arrayList.toArray(new LocalDateTime[arrayList.size()]);
    }

    private ZoneRules(ZoneOffset zoneOffset) {
        this.lastRulesCache = new ConcurrentHashMap();
        this.standardOffsets = new ZoneOffset[1];
        this.standardOffsets[0] = zoneOffset;
        this.standardTransitions = EMPTY_LONG_ARRAY;
        this.savingsInstantTransitions = EMPTY_LONG_ARRAY;
        this.savingsLocalTransitions = EMPTY_LDT_ARRAY;
        this.wallOffsets = this.standardOffsets;
        this.lastRules = EMPTY_LASTRULES;
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 1, this);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.standardTransitions.length);
        for (long j : this.standardTransitions) {
            Ser.writeEpochSec(j, dataOutput);
        }
        for (ZoneOffset zoneOffset : this.standardOffsets) {
            Ser.writeOffset(zoneOffset, dataOutput);
        }
        dataOutput.writeInt(this.savingsInstantTransitions.length);
        for (long j2 : this.savingsInstantTransitions) {
            Ser.writeEpochSec(j2, dataOutput);
        }
        for (ZoneOffset zoneOffset2 : this.wallOffsets) {
            Ser.writeOffset(zoneOffset2, dataOutput);
        }
        dataOutput.writeByte(this.lastRules.length);
        for (ZoneOffsetTransitionRule zoneOffsetTransitionRule : this.lastRules) {
            zoneOffsetTransitionRule.writeExternal(dataOutput);
        }
    }

    static ZoneRules readExternal(DataInput dataInput) throws IOException, ClassNotFoundException {
        int i = dataInput.readInt();
        long[] jArr = i == 0 ? EMPTY_LONG_ARRAY : new long[i];
        for (int i2 = 0; i2 < i; i2++) {
            jArr[i2] = Ser.readEpochSec(dataInput);
        }
        ZoneOffset[] zoneOffsetArr = new ZoneOffset[i + 1];
        for (int i3 = 0; i3 < zoneOffsetArr.length; i3++) {
            zoneOffsetArr[i3] = Ser.readOffset(dataInput);
        }
        int i4 = dataInput.readInt();
        long[] jArr2 = i4 == 0 ? EMPTY_LONG_ARRAY : new long[i4];
        for (int i5 = 0; i5 < i4; i5++) {
            jArr2[i5] = Ser.readEpochSec(dataInput);
        }
        ZoneOffset[] zoneOffsetArr2 = new ZoneOffset[i4 + 1];
        for (int i6 = 0; i6 < zoneOffsetArr2.length; i6++) {
            zoneOffsetArr2[i6] = Ser.readOffset(dataInput);
        }
        int i7 = dataInput.readByte();
        ZoneOffsetTransitionRule[] zoneOffsetTransitionRuleArr = i7 == 0 ? EMPTY_LASTRULES : new ZoneOffsetTransitionRule[i7];
        for (int i8 = 0; i8 < i7; i8++) {
            zoneOffsetTransitionRuleArr[i8] = ZoneOffsetTransitionRule.readExternal(dataInput);
        }
        return new ZoneRules(jArr, zoneOffsetArr, jArr2, zoneOffsetArr2, zoneOffsetTransitionRuleArr);
    }

    public boolean isFixedOffset() {
        return this.savingsInstantTransitions.length == 0;
    }

    public ZoneOffset getOffset(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        long epochSecond = instant.getEpochSecond();
        if (this.lastRules.length > 0 && epochSecond > this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
            ZoneOffsetTransition[] zoneOffsetTransitionArrFindTransitionArray = findTransitionArray(findYear(epochSecond, this.wallOffsets[this.wallOffsets.length - 1]));
            ZoneOffsetTransition zoneOffsetTransition = null;
            for (int i = 0; i < zoneOffsetTransitionArrFindTransitionArray.length; i++) {
                zoneOffsetTransition = zoneOffsetTransitionArrFindTransitionArray[i];
                if (epochSecond < zoneOffsetTransition.toEpochSecond()) {
                    return zoneOffsetTransition.getOffsetBefore();
                }
            }
            return zoneOffsetTransition.getOffsetAfter();
        }
        int iBinarySearch = Arrays.binarySearch(this.savingsInstantTransitions, epochSecond);
        if (iBinarySearch < 0) {
            iBinarySearch = (-iBinarySearch) - 2;
        }
        return this.wallOffsets[iBinarySearch + 1];
    }

    public ZoneOffset getOffset(LocalDateTime localDateTime) {
        Object offsetInfo = getOffsetInfo(localDateTime);
        if (offsetInfo instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) offsetInfo).getOffsetBefore();
        }
        return (ZoneOffset) offsetInfo;
    }

    public List<ZoneOffset> getValidOffsets(LocalDateTime localDateTime) {
        Object offsetInfo = getOffsetInfo(localDateTime);
        if (offsetInfo instanceof ZoneOffsetTransition) {
            return ((ZoneOffsetTransition) offsetInfo).getValidOffsets();
        }
        return Collections.singletonList((ZoneOffset) offsetInfo);
    }

    public ZoneOffsetTransition getTransition(LocalDateTime localDateTime) {
        Object offsetInfo = getOffsetInfo(localDateTime);
        if (offsetInfo instanceof ZoneOffsetTransition) {
            return (ZoneOffsetTransition) offsetInfo;
        }
        return null;
    }

    private Object getOffsetInfo(LocalDateTime localDateTime) {
        int i = 0;
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        if (this.lastRules.length > 0 && localDateTime.isAfter(this.savingsLocalTransitions[this.savingsLocalTransitions.length - 1])) {
            ZoneOffsetTransition[] zoneOffsetTransitionArrFindTransitionArray = findTransitionArray(localDateTime.getYear());
            Object obj = null;
            int length = zoneOffsetTransitionArrFindTransitionArray.length;
            while (i < length) {
                ZoneOffsetTransition zoneOffsetTransition = zoneOffsetTransitionArrFindTransitionArray[i];
                Object objFindOffsetInfo = findOffsetInfo(localDateTime, zoneOffsetTransition);
                if (!(objFindOffsetInfo instanceof ZoneOffsetTransition) && !objFindOffsetInfo.equals(zoneOffsetTransition.getOffsetBefore())) {
                    i++;
                    obj = objFindOffsetInfo;
                } else {
                    return objFindOffsetInfo;
                }
            }
            return obj;
        }
        int iBinarySearch = Arrays.binarySearch(this.savingsLocalTransitions, localDateTime);
        if (iBinarySearch == -1) {
            return this.wallOffsets[0];
        }
        if (iBinarySearch < 0) {
            iBinarySearch = (-iBinarySearch) - 2;
        } else if (iBinarySearch < this.savingsLocalTransitions.length - 1) {
            int i2 = iBinarySearch + 1;
            if (this.savingsLocalTransitions[iBinarySearch].equals(this.savingsLocalTransitions[i2])) {
                iBinarySearch = i2;
            }
        }
        if ((iBinarySearch & 1) == 0) {
            LocalDateTime localDateTime2 = this.savingsLocalTransitions[iBinarySearch];
            LocalDateTime localDateTime3 = this.savingsLocalTransitions[iBinarySearch + 1];
            int i3 = iBinarySearch / 2;
            ZoneOffset zoneOffset = this.wallOffsets[i3];
            ZoneOffset zoneOffset2 = this.wallOffsets[i3 + 1];
            if (zoneOffset2.getTotalSeconds() > zoneOffset.getTotalSeconds()) {
                return new ZoneOffsetTransition(localDateTime2, zoneOffset, zoneOffset2);
            }
            return new ZoneOffsetTransition(localDateTime3, zoneOffset, zoneOffset2);
        }
        return this.wallOffsets[(iBinarySearch / 2) + 1];
    }

    private Object findOffsetInfo(LocalDateTime localDateTime, ZoneOffsetTransition zoneOffsetTransition) {
        LocalDateTime dateTimeBefore = zoneOffsetTransition.getDateTimeBefore();
        if (zoneOffsetTransition.isGap()) {
            if (localDateTime.isBefore(dateTimeBefore)) {
                return zoneOffsetTransition.getOffsetBefore();
            }
            if (localDateTime.isBefore(zoneOffsetTransition.getDateTimeAfter())) {
                return zoneOffsetTransition;
            }
            return zoneOffsetTransition.getOffsetAfter();
        }
        if (!localDateTime.isBefore(dateTimeBefore)) {
            return zoneOffsetTransition.getOffsetAfter();
        }
        if (localDateTime.isBefore(zoneOffsetTransition.getDateTimeAfter())) {
            return zoneOffsetTransition.getOffsetBefore();
        }
        return zoneOffsetTransition;
    }

    private ZoneOffsetTransition[] findTransitionArray(int i) {
        Integer numValueOf = Integer.valueOf(i);
        ZoneOffsetTransition[] zoneOffsetTransitionArr = this.lastRulesCache.get(numValueOf);
        if (zoneOffsetTransitionArr != null) {
            return zoneOffsetTransitionArr;
        }
        ZoneOffsetTransitionRule[] zoneOffsetTransitionRuleArr = this.lastRules;
        ZoneOffsetTransition[] zoneOffsetTransitionArr2 = new ZoneOffsetTransition[zoneOffsetTransitionRuleArr.length];
        for (int i2 = 0; i2 < zoneOffsetTransitionRuleArr.length; i2++) {
            zoneOffsetTransitionArr2[i2] = zoneOffsetTransitionRuleArr[i2].createTransition(i);
        }
        if (i < LAST_CACHED_YEAR) {
            this.lastRulesCache.putIfAbsent(numValueOf, zoneOffsetTransitionArr2);
        }
        return zoneOffsetTransitionArr2;
    }

    public ZoneOffset getStandardOffset(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return this.standardOffsets[0];
        }
        int iBinarySearch = Arrays.binarySearch(this.standardTransitions, instant.getEpochSecond());
        if (iBinarySearch < 0) {
            iBinarySearch = (-iBinarySearch) - 2;
        }
        return this.standardOffsets[iBinarySearch + 1];
    }

    public Duration getDaylightSavings(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(getOffset(instant).getTotalSeconds() - getStandardOffset(instant).getTotalSeconds());
    }

    public boolean isDaylightSavings(Instant instant) {
        return !getStandardOffset(instant).equals(getOffset(instant));
    }

    public boolean isValidOffset(LocalDateTime localDateTime, ZoneOffset zoneOffset) {
        return getValidOffsets(localDateTime).contains(zoneOffset);
    }

    public ZoneOffsetTransition nextTransition(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return null;
        }
        long epochSecond = instant.getEpochSecond();
        if (epochSecond >= this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1]) {
            if (this.lastRules.length == 0) {
                return null;
            }
            int iFindYear = findYear(epochSecond, this.wallOffsets[this.wallOffsets.length - 1]);
            for (ZoneOffsetTransition zoneOffsetTransition : findTransitionArray(iFindYear)) {
                if (epochSecond < zoneOffsetTransition.toEpochSecond()) {
                    return zoneOffsetTransition;
                }
            }
            if (iFindYear < 999999999) {
                return findTransitionArray(iFindYear + 1)[0];
            }
            return null;
        }
        int iBinarySearch = Arrays.binarySearch(this.savingsInstantTransitions, epochSecond);
        int i = iBinarySearch < 0 ? (-iBinarySearch) - 1 : iBinarySearch + 1;
        return new ZoneOffsetTransition(this.savingsInstantTransitions[i], this.wallOffsets[i], this.wallOffsets[i + 1]);
    }

    public ZoneOffsetTransition previousTransition(Instant instant) {
        if (this.savingsInstantTransitions.length == 0) {
            return null;
        }
        long epochSecond = instant.getEpochSecond();
        if (instant.getNano() > 0 && epochSecond < Long.MAX_VALUE) {
            epochSecond++;
        }
        long j = this.savingsInstantTransitions[this.savingsInstantTransitions.length - 1];
        if (this.lastRules.length > 0 && epochSecond > j) {
            ZoneOffset zoneOffset = this.wallOffsets[this.wallOffsets.length - 1];
            int iFindYear = findYear(epochSecond, zoneOffset);
            ZoneOffsetTransition[] zoneOffsetTransitionArrFindTransitionArray = findTransitionArray(iFindYear);
            for (int length = zoneOffsetTransitionArrFindTransitionArray.length - 1; length >= 0; length--) {
                if (epochSecond > zoneOffsetTransitionArrFindTransitionArray[length].toEpochSecond()) {
                    return zoneOffsetTransitionArrFindTransitionArray[length];
                }
            }
            int i = iFindYear - 1;
            if (i > findYear(j, zoneOffset)) {
                return findTransitionArray(i)[r11.length - 1];
            }
        }
        int iBinarySearch = Arrays.binarySearch(this.savingsInstantTransitions, epochSecond);
        if (iBinarySearch < 0) {
            iBinarySearch = (-iBinarySearch) - 1;
        }
        if (iBinarySearch <= 0) {
            return null;
        }
        int i2 = iBinarySearch - 1;
        return new ZoneOffsetTransition(this.savingsInstantTransitions[i2], this.wallOffsets[i2], this.wallOffsets[iBinarySearch]);
    }

    private int findYear(long j, ZoneOffset zoneOffset) {
        return LocalDate.ofEpochDay(Math.floorDiv(j + ((long) zoneOffset.getTotalSeconds()), 86400L)).getYear();
    }

    public List<ZoneOffsetTransition> getTransitions() {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i < this.savingsInstantTransitions.length) {
            long j = this.savingsInstantTransitions[i];
            ZoneOffset zoneOffset = this.wallOffsets[i];
            i++;
            arrayList.add(new ZoneOffsetTransition(j, zoneOffset, this.wallOffsets[i]));
        }
        return Collections.unmodifiableList(arrayList);
    }

    public List<ZoneOffsetTransitionRule> getTransitionRules() {
        return Collections.unmodifiableList(Arrays.asList(this.lastRules));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneRules)) {
            return false;
        }
        ZoneRules zoneRules = (ZoneRules) obj;
        return Arrays.equals(this.standardTransitions, zoneRules.standardTransitions) && Arrays.equals(this.standardOffsets, zoneRules.standardOffsets) && Arrays.equals(this.savingsInstantTransitions, zoneRules.savingsInstantTransitions) && Arrays.equals(this.wallOffsets, zoneRules.wallOffsets) && Arrays.equals(this.lastRules, zoneRules.lastRules);
    }

    public int hashCode() {
        return (((Arrays.hashCode(this.standardTransitions) ^ Arrays.hashCode(this.standardOffsets)) ^ Arrays.hashCode(this.savingsInstantTransitions)) ^ Arrays.hashCode(this.wallOffsets)) ^ Arrays.hashCode(this.lastRules);
    }

    public String toString() {
        return "ZoneRules[currentStandardOffset=" + ((Object) this.standardOffsets[this.standardOffsets.length - 1]) + "]";
    }
}
