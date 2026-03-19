package android.icu.util;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Pair;
import android.icu.impl.locale.LanguageTag;
import android.icu.text.PluralRules;
import android.icu.text.UnicodeSet;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class MeasureUnit implements Serializable {
    public static final MeasureUnit ACRE;
    public static final MeasureUnit ACRE_FOOT;
    public static final MeasureUnit AMPERE;
    public static final MeasureUnit ARC_MINUTE;
    public static final MeasureUnit ARC_SECOND;
    public static final MeasureUnit ASTRONOMICAL_UNIT;
    public static final MeasureUnit BIT;
    public static final MeasureUnit BUSHEL;
    public static final MeasureUnit BYTE;
    public static final MeasureUnit CALORIE;
    public static final MeasureUnit CARAT;
    public static final MeasureUnit CELSIUS;
    public static final MeasureUnit CENTILITER;
    public static final MeasureUnit CENTIMETER;
    public static final MeasureUnit CENTURY;
    public static final MeasureUnit CUBIC_CENTIMETER;
    public static final MeasureUnit CUBIC_FOOT;
    public static final MeasureUnit CUBIC_INCH;
    public static final MeasureUnit CUBIC_KILOMETER;
    public static final MeasureUnit CUBIC_METER;
    public static final MeasureUnit CUBIC_MILE;
    public static final MeasureUnit CUBIC_YARD;
    public static final MeasureUnit CUP;
    public static final MeasureUnit CUP_METRIC;
    public static final TimeUnit DAY;
    public static final MeasureUnit DECILITER;
    public static final MeasureUnit DECIMETER;
    public static final MeasureUnit DEGREE;
    public static final MeasureUnit FAHRENHEIT;
    public static final MeasureUnit FATHOM;
    public static final MeasureUnit FLUID_OUNCE;
    public static final MeasureUnit FOODCALORIE;
    public static final MeasureUnit FOOT;
    public static final MeasureUnit FURLONG;
    public static final MeasureUnit GALLON;
    public static final MeasureUnit GENERIC_TEMPERATURE;
    public static final MeasureUnit GIGABIT;
    public static final MeasureUnit GIGABYTE;
    public static final MeasureUnit GIGAHERTZ;
    public static final MeasureUnit GIGAWATT;
    public static final MeasureUnit GRAM;
    public static final MeasureUnit G_FORCE;
    public static final MeasureUnit HECTARE;
    public static final MeasureUnit HECTOLITER;
    public static final MeasureUnit HECTOPASCAL;
    public static final MeasureUnit HERTZ;
    public static final MeasureUnit HORSEPOWER;
    public static final TimeUnit HOUR;
    public static final MeasureUnit INCH;
    public static final MeasureUnit INCH_HG;
    public static final MeasureUnit JOULE;
    public static final MeasureUnit KARAT;
    public static final MeasureUnit KELVIN;
    public static final MeasureUnit KILOBIT;
    public static final MeasureUnit KILOBYTE;
    public static final MeasureUnit KILOCALORIE;
    public static final MeasureUnit KILOGRAM;
    public static final MeasureUnit KILOHERTZ;
    public static final MeasureUnit KILOJOULE;
    public static final MeasureUnit KILOMETER;
    public static final MeasureUnit KILOMETER_PER_HOUR;
    public static final MeasureUnit KILOWATT;
    public static final MeasureUnit KILOWATT_HOUR;
    public static final MeasureUnit KNOT;
    public static final MeasureUnit LIGHT_YEAR;
    public static final MeasureUnit LITER;
    public static final MeasureUnit LITER_PER_100KILOMETERS;
    public static final MeasureUnit LITER_PER_KILOMETER;
    public static final MeasureUnit LUX;
    public static final MeasureUnit MEGABIT;
    public static final MeasureUnit MEGABYTE;
    public static final MeasureUnit MEGAHERTZ;
    public static final MeasureUnit MEGALITER;
    public static final MeasureUnit MEGAWATT;
    public static final MeasureUnit METER;
    public static final MeasureUnit METER_PER_SECOND;
    public static final MeasureUnit METER_PER_SECOND_SQUARED;
    public static final MeasureUnit METRIC_TON;
    public static final MeasureUnit MICROGRAM;
    public static final MeasureUnit MICROMETER;
    public static final MeasureUnit MICROSECOND;
    public static final MeasureUnit MILE;
    public static final MeasureUnit MILE_PER_GALLON;
    public static final MeasureUnit MILE_PER_HOUR;
    public static final MeasureUnit MILE_SCANDINAVIAN;
    public static final MeasureUnit MILLIAMPERE;
    public static final MeasureUnit MILLIBAR;
    public static final MeasureUnit MILLIGRAM;
    public static final MeasureUnit MILLILITER;
    public static final MeasureUnit MILLIMETER;
    public static final MeasureUnit MILLIMETER_OF_MERCURY;
    public static final MeasureUnit MILLISECOND;
    public static final MeasureUnit MILLIWATT;
    public static final TimeUnit MINUTE;
    public static final TimeUnit MONTH;
    public static final MeasureUnit NANOMETER;
    public static final MeasureUnit NANOSECOND;
    public static final MeasureUnit NAUTICAL_MILE;
    public static final MeasureUnit OHM;
    public static final MeasureUnit OUNCE;
    public static final MeasureUnit OUNCE_TROY;
    public static final MeasureUnit PARSEC;
    public static final MeasureUnit PICOMETER;
    public static final MeasureUnit PINT;
    public static final MeasureUnit PINT_METRIC;
    public static final MeasureUnit POUND;
    public static final MeasureUnit POUND_PER_SQUARE_INCH;
    public static final MeasureUnit QUART;
    public static final MeasureUnit RADIAN;
    public static final MeasureUnit REVOLUTION_ANGLE;
    public static final TimeUnit SECOND;
    public static final MeasureUnit SQUARE_CENTIMETER;
    public static final MeasureUnit SQUARE_FOOT;
    public static final MeasureUnit SQUARE_INCH;
    public static final MeasureUnit SQUARE_KILOMETER;
    public static final MeasureUnit SQUARE_METER;
    public static final MeasureUnit SQUARE_MILE;
    public static final MeasureUnit SQUARE_YARD;
    public static final MeasureUnit STONE;
    public static final MeasureUnit TABLESPOON;
    public static final MeasureUnit TEASPOON;
    public static final MeasureUnit TERABIT;
    public static final MeasureUnit TERABYTE;
    public static final MeasureUnit TON;
    public static final MeasureUnit VOLT;
    public static final MeasureUnit WATT;
    public static final TimeUnit WEEK;
    public static final MeasureUnit YARD;
    public static final TimeUnit YEAR;
    private static final long serialVersionUID = -1839973855554750484L;
    private static HashMap<Pair<MeasureUnit, MeasureUnit>, MeasureUnit> unitPerUnitToSingleUnit;

    @Deprecated
    protected final String subType;

    @Deprecated
    protected final String type;
    private static final String[] unitKeys = {"units", "unitsShort", "unitsNarrow"};
    private static final Map<String, Map<String, MeasureUnit>> cache = new HashMap();
    static final UnicodeSet ASCII = new UnicodeSet(97, 122).freeze();
    static final UnicodeSet ASCII_HYPHEN_DIGITS = new UnicodeSet(45, 45, 48, 57, 97, 122).freeze();
    private static Factory UNIT_FACTORY = new Factory() {
        @Override
        public MeasureUnit create(String type, String subType) {
            return new MeasureUnit(type, subType);
        }
    };
    static Factory CURRENCY_FACTORY = new Factory() {
        @Override
        public MeasureUnit create(String unusedType, String subType) {
            return new Currency(subType);
        }
    };
    static Factory TIMEUNIT_FACTORY = new Factory() {
        @Override
        public MeasureUnit create(String type, String subType) {
            return new TimeUnit(type, subType);
        }
    };

    @Deprecated
    protected interface Factory {
        @Deprecated
        MeasureUnit create(String str, String str2);
    }

    static {
        ICUResourceBundle resource = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "en");
        for (String key : unitKeys) {
            try {
                ICUResourceBundle unitsTypeRes = resource.getWithFallback(key);
                int size = unitsTypeRes.getSize();
                for (int index = 0; index < size; index++) {
                    UResourceBundle unitsRes = unitsTypeRes.get(index);
                    String type = unitsRes.getKey();
                    if (!type.equals("compound")) {
                        int unitsSize = unitsRes.getSize();
                        for (int index2 = 0; index2 < unitsSize; index2++) {
                            ICUResourceBundle unitNameRes = (ICUResourceBundle) unitsRes.get(index2);
                            if (unitNameRes.get(PluralRules.KEYWORD_OTHER) != null) {
                                internalGetInstance(type, unitNameRes.getKey());
                            }
                        }
                    }
                }
            } catch (MissingResourceException e) {
            }
        }
        try {
            UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "currencyNumericCodes", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle codeMap = bundle.get("codeMap");
            Enumeration<String> it = codeMap.getKeys();
            while (it.hasMoreElements()) {
                internalGetInstance("currency", it.nextElement());
            }
        } catch (MissingResourceException e2) {
        }
        G_FORCE = internalGetInstance("acceleration", "g-force");
        METER_PER_SECOND_SQUARED = internalGetInstance("acceleration", "meter-per-second-squared");
        ARC_MINUTE = internalGetInstance("angle", "arc-minute");
        ARC_SECOND = internalGetInstance("angle", "arc-second");
        DEGREE = internalGetInstance("angle", "degree");
        RADIAN = internalGetInstance("angle", "radian");
        REVOLUTION_ANGLE = internalGetInstance("angle", "revolution");
        ACRE = internalGetInstance("area", "acre");
        HECTARE = internalGetInstance("area", "hectare");
        SQUARE_CENTIMETER = internalGetInstance("area", "square-centimeter");
        SQUARE_FOOT = internalGetInstance("area", "square-foot");
        SQUARE_INCH = internalGetInstance("area", "square-inch");
        SQUARE_KILOMETER = internalGetInstance("area", "square-kilometer");
        SQUARE_METER = internalGetInstance("area", "square-meter");
        SQUARE_MILE = internalGetInstance("area", "square-mile");
        SQUARE_YARD = internalGetInstance("area", "square-yard");
        LITER_PER_100KILOMETERS = internalGetInstance("consumption", "liter-per-100kilometers");
        LITER_PER_KILOMETER = internalGetInstance("consumption", "liter-per-kilometer");
        MILE_PER_GALLON = internalGetInstance("consumption", "mile-per-gallon");
        BIT = internalGetInstance("digital", "bit");
        BYTE = internalGetInstance("digital", "byte");
        GIGABIT = internalGetInstance("digital", "gigabit");
        GIGABYTE = internalGetInstance("digital", "gigabyte");
        KILOBIT = internalGetInstance("digital", "kilobit");
        KILOBYTE = internalGetInstance("digital", "kilobyte");
        MEGABIT = internalGetInstance("digital", "megabit");
        MEGABYTE = internalGetInstance("digital", "megabyte");
        TERABIT = internalGetInstance("digital", "terabit");
        TERABYTE = internalGetInstance("digital", "terabyte");
        CENTURY = internalGetInstance("duration", "century");
        DAY = (TimeUnit) internalGetInstance("duration", "day");
        HOUR = (TimeUnit) internalGetInstance("duration", "hour");
        MICROSECOND = internalGetInstance("duration", "microsecond");
        MILLISECOND = internalGetInstance("duration", "millisecond");
        MINUTE = (TimeUnit) internalGetInstance("duration", "minute");
        MONTH = (TimeUnit) internalGetInstance("duration", "month");
        NANOSECOND = internalGetInstance("duration", "nanosecond");
        SECOND = (TimeUnit) internalGetInstance("duration", "second");
        WEEK = (TimeUnit) internalGetInstance("duration", "week");
        YEAR = (TimeUnit) internalGetInstance("duration", "year");
        AMPERE = internalGetInstance("electric", "ampere");
        MILLIAMPERE = internalGetInstance("electric", "milliampere");
        OHM = internalGetInstance("electric", "ohm");
        VOLT = internalGetInstance("electric", "volt");
        CALORIE = internalGetInstance("energy", "calorie");
        FOODCALORIE = internalGetInstance("energy", "foodcalorie");
        JOULE = internalGetInstance("energy", "joule");
        KILOCALORIE = internalGetInstance("energy", "kilocalorie");
        KILOJOULE = internalGetInstance("energy", "kilojoule");
        KILOWATT_HOUR = internalGetInstance("energy", "kilowatt-hour");
        GIGAHERTZ = internalGetInstance("frequency", "gigahertz");
        HERTZ = internalGetInstance("frequency", "hertz");
        KILOHERTZ = internalGetInstance("frequency", "kilohertz");
        MEGAHERTZ = internalGetInstance("frequency", "megahertz");
        ASTRONOMICAL_UNIT = internalGetInstance("length", "astronomical-unit");
        CENTIMETER = internalGetInstance("length", "centimeter");
        DECIMETER = internalGetInstance("length", "decimeter");
        FATHOM = internalGetInstance("length", "fathom");
        FOOT = internalGetInstance("length", "foot");
        FURLONG = internalGetInstance("length", "furlong");
        INCH = internalGetInstance("length", "inch");
        KILOMETER = internalGetInstance("length", "kilometer");
        LIGHT_YEAR = internalGetInstance("length", "light-year");
        METER = internalGetInstance("length", "meter");
        MICROMETER = internalGetInstance("length", "micrometer");
        MILE = internalGetInstance("length", "mile");
        MILE_SCANDINAVIAN = internalGetInstance("length", "mile-scandinavian");
        MILLIMETER = internalGetInstance("length", "millimeter");
        NANOMETER = internalGetInstance("length", "nanometer");
        NAUTICAL_MILE = internalGetInstance("length", "nautical-mile");
        PARSEC = internalGetInstance("length", "parsec");
        PICOMETER = internalGetInstance("length", "picometer");
        YARD = internalGetInstance("length", "yard");
        LUX = internalGetInstance("light", "lux");
        CARAT = internalGetInstance("mass", "carat");
        GRAM = internalGetInstance("mass", "gram");
        KILOGRAM = internalGetInstance("mass", "kilogram");
        METRIC_TON = internalGetInstance("mass", "metric-ton");
        MICROGRAM = internalGetInstance("mass", "microgram");
        MILLIGRAM = internalGetInstance("mass", "milligram");
        OUNCE = internalGetInstance("mass", "ounce");
        OUNCE_TROY = internalGetInstance("mass", "ounce-troy");
        POUND = internalGetInstance("mass", "pound");
        STONE = internalGetInstance("mass", "stone");
        TON = internalGetInstance("mass", "ton");
        GIGAWATT = internalGetInstance("power", "gigawatt");
        HORSEPOWER = internalGetInstance("power", "horsepower");
        KILOWATT = internalGetInstance("power", "kilowatt");
        MEGAWATT = internalGetInstance("power", "megawatt");
        MILLIWATT = internalGetInstance("power", "milliwatt");
        WATT = internalGetInstance("power", "watt");
        HECTOPASCAL = internalGetInstance("pressure", "hectopascal");
        INCH_HG = internalGetInstance("pressure", "inch-hg");
        MILLIBAR = internalGetInstance("pressure", "millibar");
        MILLIMETER_OF_MERCURY = internalGetInstance("pressure", "millimeter-of-mercury");
        POUND_PER_SQUARE_INCH = internalGetInstance("pressure", "pound-per-square-inch");
        KARAT = internalGetInstance("proportion", "karat");
        KILOMETER_PER_HOUR = internalGetInstance("speed", "kilometer-per-hour");
        KNOT = internalGetInstance("speed", "knot");
        METER_PER_SECOND = internalGetInstance("speed", "meter-per-second");
        MILE_PER_HOUR = internalGetInstance("speed", "mile-per-hour");
        CELSIUS = internalGetInstance("temperature", "celsius");
        FAHRENHEIT = internalGetInstance("temperature", "fahrenheit");
        GENERIC_TEMPERATURE = internalGetInstance("temperature", "generic");
        KELVIN = internalGetInstance("temperature", "kelvin");
        ACRE_FOOT = internalGetInstance("volume", "acre-foot");
        BUSHEL = internalGetInstance("volume", "bushel");
        CENTILITER = internalGetInstance("volume", "centiliter");
        CUBIC_CENTIMETER = internalGetInstance("volume", "cubic-centimeter");
        CUBIC_FOOT = internalGetInstance("volume", "cubic-foot");
        CUBIC_INCH = internalGetInstance("volume", "cubic-inch");
        CUBIC_KILOMETER = internalGetInstance("volume", "cubic-kilometer");
        CUBIC_METER = internalGetInstance("volume", "cubic-meter");
        CUBIC_MILE = internalGetInstance("volume", "cubic-mile");
        CUBIC_YARD = internalGetInstance("volume", "cubic-yard");
        CUP = internalGetInstance("volume", "cup");
        CUP_METRIC = internalGetInstance("volume", "cup-metric");
        DECILITER = internalGetInstance("volume", "deciliter");
        FLUID_OUNCE = internalGetInstance("volume", "fluid-ounce");
        GALLON = internalGetInstance("volume", "gallon");
        HECTOLITER = internalGetInstance("volume", "hectoliter");
        LITER = internalGetInstance("volume", "liter");
        MEGALITER = internalGetInstance("volume", "megaliter");
        MILLILITER = internalGetInstance("volume", "milliliter");
        PINT = internalGetInstance("volume", "pint");
        PINT_METRIC = internalGetInstance("volume", "pint-metric");
        QUART = internalGetInstance("volume", "quart");
        TABLESPOON = internalGetInstance("volume", "tablespoon");
        TEASPOON = internalGetInstance("volume", "teaspoon");
        unitPerUnitToSingleUnit = new HashMap<>();
        unitPerUnitToSingleUnit.put(Pair.of(KILOMETER, HOUR), KILOMETER_PER_HOUR);
        unitPerUnitToSingleUnit.put(Pair.of(MILE, GALLON), MILE_PER_GALLON);
        unitPerUnitToSingleUnit.put(Pair.of(MILE, HOUR), MILE_PER_HOUR);
        unitPerUnitToSingleUnit.put(Pair.of(METER, SECOND), METER_PER_SECOND);
        unitPerUnitToSingleUnit.put(Pair.of(LITER, KILOMETER), LITER_PER_KILOMETER);
        unitPerUnitToSingleUnit.put(Pair.of(POUND, SQUARE_INCH), POUND_PER_SQUARE_INCH);
    }

    @Deprecated
    protected MeasureUnit(String type, String subType) {
        this.type = type;
        this.subType = subType;
    }

    public String getType() {
        return this.type;
    }

    public String getSubtype() {
        return this.subType;
    }

    public int hashCode() {
        return (this.type.hashCode() * 31) + this.subType.hashCode();
    }

    public boolean equals(Object rhs) {
        if (rhs == this) {
            return true;
        }
        if (!(rhs instanceof MeasureUnit)) {
            return false;
        }
        MeasureUnit c = (MeasureUnit) rhs;
        if (this.type.equals(c.type)) {
            return this.subType.equals(c.subType);
        }
        return false;
    }

    public String toString() {
        return this.type + LanguageTag.SEP + this.subType;
    }

    public static synchronized Set<String> getAvailableTypes() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    public static synchronized Set<MeasureUnit> getAvailable(String type) {
        Map<String, MeasureUnit> units;
        units = cache.get(type);
        return units == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet(units.values()));
    }

    public static synchronized Set<MeasureUnit> getAvailable() {
        Set<MeasureUnit> result;
        result = new HashSet<>();
        for (String type : new HashSet(getAvailableTypes())) {
            for (MeasureUnit unit : getAvailable(type)) {
                result.add(unit);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Deprecated
    public static MeasureUnit internalGetInstance(String type, String subType) {
        Factory factory;
        if (type == null || subType == null) {
            throw new NullPointerException("Type and subType must be non-null");
        }
        if (!"currency".equals(type) && (!ASCII.containsAll(type) || !ASCII_HYPHEN_DIGITS.containsAll(subType))) {
            throw new IllegalArgumentException("The type or subType are invalid.");
        }
        if ("currency".equals(type)) {
            factory = CURRENCY_FACTORY;
        } else if ("duration".equals(type)) {
            factory = TIMEUNIT_FACTORY;
        } else {
            factory = UNIT_FACTORY;
        }
        return addUnit(type, subType, factory);
    }

    @Deprecated
    public static MeasureUnit resolveUnitPerUnit(MeasureUnit unit, MeasureUnit perUnit) {
        return unitPerUnitToSingleUnit.get(Pair.of(unit, perUnit));
    }

    @Deprecated
    protected static synchronized MeasureUnit addUnit(String type, String unitName, Factory factory) {
        MeasureUnit unit;
        Map<String, MeasureUnit> tmp = cache.get(type);
        if (tmp == null) {
            Map<String, Map<String, MeasureUnit>> map = cache;
            tmp = new HashMap<>();
            map.put(type, tmp);
        } else {
            type = tmp.entrySet().iterator().next().getValue().type;
        }
        unit = tmp.get(unitName);
        if (unit == null) {
            unit = factory.create(type, unitName);
            tmp.put(unitName, unit);
        }
        return unit;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnitProxy(this.type, this.subType);
    }

    static final class MeasureUnitProxy implements Externalizable {
        private static final long serialVersionUID = -3910681415330989598L;
        private String subType;
        private String type;

        public MeasureUnitProxy(String type, String subType) {
            this.type = type;
            this.subType = subType;
        }

        public MeasureUnitProxy() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0);
            out.writeUTF(this.type);
            out.writeUTF(this.subType);
            out.writeShort(0);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            in.readByte();
            this.type = in.readUTF();
            this.subType = in.readUTF();
            int extra = in.readShort();
            if (extra <= 0) {
                return;
            }
            byte[] extraBytes = new byte[extra];
            in.read(extraBytes, 0, extra);
        }

        private Object readResolve() throws ObjectStreamException {
            return MeasureUnit.internalGetInstance(this.type, this.subType);
        }
    }
}
