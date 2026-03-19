package java.util.stream;

import java.util.EnumMap;
import java.util.Map;
import java.util.Spliterator;

public enum StreamOpFlag {
    DISTINCT(0, set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP)),
    SORTED(1, set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP)),
    ORDERED(2, set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP).clear(Type.TERMINAL_OP).clear(Type.UPSTREAM_TERMINAL_OP)),
    SIZED(3, set(Type.SPLITERATOR).set(Type.STREAM).clear(Type.OP)),
    SHORT_CIRCUIT(12, set(Type.OP).set(Type.TERMINAL_OP));

    private static final int CLEAR_BITS = 2;
    private static final int PRESERVE_BITS = 3;
    private static final int SET_BITS = 1;
    private final int bitPosition;
    private final int clear;
    private final Map<Type, Integer> maskTable;
    private final int preserve;
    private final int set;
    public static final int SPLITERATOR_CHARACTERISTICS_MASK = createMask(Type.SPLITERATOR);
    public static final int STREAM_MASK = createMask(Type.STREAM);
    public static final int OP_MASK = createMask(Type.OP);
    public static final int TERMINAL_OP_MASK = createMask(Type.TERMINAL_OP);
    public static final int UPSTREAM_TERMINAL_OP_MASK = createMask(Type.UPSTREAM_TERMINAL_OP);
    private static final int FLAG_MASK = createFlagMask();
    private static final int FLAG_MASK_IS = STREAM_MASK;
    private static final int FLAG_MASK_NOT = STREAM_MASK << 1;
    public static final int INITIAL_OPS_VALUE = FLAG_MASK_IS | FLAG_MASK_NOT;
    public static final int IS_DISTINCT = DISTINCT.set;
    public static final int NOT_DISTINCT = DISTINCT.clear;
    public static final int IS_SORTED = SORTED.set;
    public static final int NOT_SORTED = SORTED.clear;
    public static final int IS_ORDERED = ORDERED.set;
    public static final int NOT_ORDERED = ORDERED.clear;
    public static final int IS_SIZED = SIZED.set;
    public static final int NOT_SIZED = SIZED.clear;
    public static final int IS_SHORT_CIRCUIT = SHORT_CIRCUIT.set;

    enum Type {
        SPLITERATOR,
        STREAM,
        OP,
        TERMINAL_OP,
        UPSTREAM_TERMINAL_OP
    }

    private static MaskBuilder set(Type type) {
        return new MaskBuilder(new EnumMap(Type.class)).set(type);
    }

    private static class MaskBuilder {
        final Map<Type, Integer> map;

        MaskBuilder(Map<Type, Integer> map) {
            this.map = map;
        }

        MaskBuilder mask(Type type, Integer num) {
            this.map.put(type, num);
            return this;
        }

        MaskBuilder set(Type type) {
            return mask(type, 1);
        }

        MaskBuilder clear(Type type) {
            return mask(type, 2);
        }

        MaskBuilder setAndClear(Type type) {
            return mask(type, 3);
        }

        Map<Type, Integer> build() {
            for (Type type : Type.values()) {
                this.map.putIfAbsent(type, 0);
            }
            return this.map;
        }
    }

    StreamOpFlag(int i, MaskBuilder maskBuilder) {
        this.maskTable = maskBuilder.build();
        int i2 = i * 2;
        this.bitPosition = i2;
        this.set = 1 << i2;
        this.clear = 2 << i2;
        this.preserve = 3 << i2;
    }

    public int set() {
        return this.set;
    }

    public int clear() {
        return this.clear;
    }

    public boolean isStreamFlag() {
        return this.maskTable.get(Type.STREAM).intValue() > 0;
    }

    public boolean isKnown(int i) {
        return (i & this.preserve) == this.set;
    }

    public boolean isCleared(int i) {
        return (i & this.preserve) == this.clear;
    }

    public boolean isPreserved(int i) {
        return (i & this.preserve) == this.preserve;
    }

    public boolean canSet(Type type) {
        return (this.maskTable.get(type).intValue() & 1) > 0;
    }

    private static int createMask(Type type) {
        int iIntValue = 0;
        for (StreamOpFlag streamOpFlag : values()) {
            iIntValue |= streamOpFlag.maskTable.get(type).intValue() << streamOpFlag.bitPosition;
        }
        return iIntValue;
    }

    private static int createFlagMask() {
        int i = 0;
        for (StreamOpFlag streamOpFlag : values()) {
            i |= streamOpFlag.preserve;
        }
        return i;
    }

    private static int getMask(int i) {
        if (i == 0) {
            return FLAG_MASK;
        }
        return ~(((i & FLAG_MASK_NOT) >> 1) | ((FLAG_MASK_IS & i) << 1) | i);
    }

    public static int combineOpFlags(int i, int i2) {
        return i | (i2 & getMask(i));
    }

    public static int toStreamFlags(int i) {
        return i & ((~i) >> 1) & FLAG_MASK_IS;
    }

    public static int toCharacteristics(int i) {
        return i & SPLITERATOR_CHARACTERISTICS_MASK;
    }

    public static int fromCharacteristics(Spliterator<?> spliterator) {
        int iCharacteristics = spliterator.characteristics();
        if ((iCharacteristics & 4) != 0 && spliterator.getComparator() != null) {
            return SPLITERATOR_CHARACTERISTICS_MASK & iCharacteristics & (-5);
        }
        return SPLITERATOR_CHARACTERISTICS_MASK & iCharacteristics;
    }

    public static int fromCharacteristics(int i) {
        return i & SPLITERATOR_CHARACTERISTICS_MASK;
    }
}
