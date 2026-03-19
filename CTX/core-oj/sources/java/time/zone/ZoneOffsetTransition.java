package java.time.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ZoneOffsetTransition implements Comparable<ZoneOffsetTransition>, Serializable {
    private static final long serialVersionUID = -6946044323557704546L;
    private final ZoneOffset offsetAfter;
    private final ZoneOffset offsetBefore;
    private final LocalDateTime transition;

    public static ZoneOffsetTransition of(LocalDateTime localDateTime, ZoneOffset zoneOffset, ZoneOffset zoneOffset2) {
        Objects.requireNonNull(localDateTime, "transition");
        Objects.requireNonNull(zoneOffset, "offsetBefore");
        Objects.requireNonNull(zoneOffset2, "offsetAfter");
        if (zoneOffset.equals(zoneOffset2)) {
            throw new IllegalArgumentException("Offsets must not be equal");
        }
        if (localDateTime.getNano() != 0) {
            throw new IllegalArgumentException("Nano-of-second must be zero");
        }
        return new ZoneOffsetTransition(localDateTime, zoneOffset, zoneOffset2);
    }

    ZoneOffsetTransition(LocalDateTime localDateTime, ZoneOffset zoneOffset, ZoneOffset zoneOffset2) {
        this.transition = localDateTime;
        this.offsetBefore = zoneOffset;
        this.offsetAfter = zoneOffset2;
    }

    ZoneOffsetTransition(long j, ZoneOffset zoneOffset, ZoneOffset zoneOffset2) {
        this.transition = LocalDateTime.ofEpochSecond(j, 0, zoneOffset);
        this.offsetBefore = zoneOffset;
        this.offsetAfter = zoneOffset2;
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 2, this);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        Ser.writeEpochSec(toEpochSecond(), dataOutput);
        Ser.writeOffset(this.offsetBefore, dataOutput);
        Ser.writeOffset(this.offsetAfter, dataOutput);
    }

    static ZoneOffsetTransition readExternal(DataInput dataInput) throws IOException {
        long epochSec = Ser.readEpochSec(dataInput);
        ZoneOffset offset = Ser.readOffset(dataInput);
        ZoneOffset offset2 = Ser.readOffset(dataInput);
        if (offset.equals(offset2)) {
            throw new IllegalArgumentException("Offsets must not be equal");
        }
        return new ZoneOffsetTransition(epochSec, offset, offset2);
    }

    public Instant getInstant() {
        return this.transition.toInstant(this.offsetBefore);
    }

    public long toEpochSecond() {
        return this.transition.toEpochSecond(this.offsetBefore);
    }

    public LocalDateTime getDateTimeBefore() {
        return this.transition;
    }

    public LocalDateTime getDateTimeAfter() {
        return this.transition.plusSeconds(getDurationSeconds());
    }

    public ZoneOffset getOffsetBefore() {
        return this.offsetBefore;
    }

    public ZoneOffset getOffsetAfter() {
        return this.offsetAfter;
    }

    public Duration getDuration() {
        return Duration.ofSeconds(getDurationSeconds());
    }

    private int getDurationSeconds() {
        return getOffsetAfter().getTotalSeconds() - getOffsetBefore().getTotalSeconds();
    }

    public boolean isGap() {
        return getOffsetAfter().getTotalSeconds() > getOffsetBefore().getTotalSeconds();
    }

    public boolean isOverlap() {
        return getOffsetAfter().getTotalSeconds() < getOffsetBefore().getTotalSeconds();
    }

    public boolean isValidOffset(ZoneOffset zoneOffset) {
        if (isGap()) {
            return false;
        }
        return getOffsetBefore().equals(zoneOffset) || getOffsetAfter().equals(zoneOffset);
    }

    List<ZoneOffset> getValidOffsets() {
        return isGap() ? Collections.emptyList() : Arrays.asList(getOffsetBefore(), getOffsetAfter());
    }

    @Override
    public int compareTo(ZoneOffsetTransition zoneOffsetTransition) {
        return getInstant().compareTo(zoneOffsetTransition.getInstant());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ZoneOffsetTransition)) {
            return false;
        }
        ZoneOffsetTransition zoneOffsetTransition = (ZoneOffsetTransition) obj;
        return this.transition.equals(zoneOffsetTransition.transition) && this.offsetBefore.equals(zoneOffsetTransition.offsetBefore) && this.offsetAfter.equals(zoneOffsetTransition.offsetAfter);
    }

    public int hashCode() {
        return (this.transition.hashCode() ^ this.offsetBefore.hashCode()) ^ Integer.rotateLeft(this.offsetAfter.hashCode(), 16);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transition[");
        sb.append(isGap() ? "Gap" : "Overlap");
        sb.append(" at ");
        sb.append((Object) this.transition);
        sb.append((Object) this.offsetBefore);
        sb.append(" to ");
        sb.append((Object) this.offsetAfter);
        sb.append(']');
        return sb.toString();
    }
}
