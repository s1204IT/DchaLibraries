package android.icu.impl.number;

import android.icu.impl.CurrencyData;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource;
import android.icu.number.NumberFormatter;
import android.icu.text.NumberFormat;
import android.icu.text.PluralRules;
import android.icu.util.Currency;
import android.icu.util.ICUException;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.EnumMap;
import java.util.Map;

public class LongNameHandler implements MicroPropsGenerator {
    private final Map<StandardPlural, SimpleModifier> modifiers;
    private final MicroPropsGenerator parent;
    private final PluralRules rules;

    private static final class PluralTableSink extends UResource.Sink {
        Map<StandardPlural, String> output;

        public PluralTableSink(Map<StandardPlural, String> map) {
            this.output = map;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (!key.contentEquals("dnam") && !key.contentEquals("per")) {
                    StandardPlural standardPluralFromString = StandardPlural.fromString(key);
                    if (!this.output.containsKey(standardPluralFromString)) {
                        this.output.put(standardPluralFromString, value.getString());
                    }
                }
            }
        }
    }

    private static void getMeasureData(ULocale uLocale, MeasureUnit measureUnit, NumberFormatter.UnitWidth unitWidth, Map<StandardPlural, String> map) {
        PluralTableSink pluralTableSink = new PluralTableSink(map);
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, uLocale);
        StringBuilder sb = new StringBuilder();
        sb.append("units");
        if (unitWidth == NumberFormatter.UnitWidth.NARROW) {
            sb.append("Narrow");
        } else if (unitWidth == NumberFormatter.UnitWidth.SHORT) {
            sb.append("Short");
        }
        sb.append("/");
        sb.append(measureUnit.getType());
        sb.append("/");
        sb.append(measureUnit.getSubtype());
        iCUResourceBundle.getAllItemsWithFallback(sb.toString(), pluralTableSink);
    }

    private static void getCurrencyLongNameData(ULocale uLocale, Currency currency, Map<StandardPlural, String> map) {
        for (Map.Entry<String, String> entry : CurrencyData.provider.getInstance(uLocale, true).getUnitPatterns().entrySet()) {
            String key = entry.getKey();
            map.put(StandardPlural.fromString(entry.getKey()), entry.getValue().replace("{1}", currency.getName(uLocale, 2, key, (boolean[]) null)));
        }
    }

    private LongNameHandler(Map<StandardPlural, SimpleModifier> map, PluralRules pluralRules, MicroPropsGenerator microPropsGenerator) {
        this.modifiers = map;
        this.rules = pluralRules;
        this.parent = microPropsGenerator;
    }

    public static LongNameHandler forCurrencyLongNames(ULocale uLocale, Currency currency, PluralRules pluralRules, MicroPropsGenerator microPropsGenerator) {
        EnumMap enumMap = new EnumMap(StandardPlural.class);
        getCurrencyLongNameData(uLocale, currency, enumMap);
        EnumMap enumMap2 = new EnumMap(StandardPlural.class);
        simpleFormatsToModifiers(enumMap, null, enumMap2);
        return new LongNameHandler(enumMap2, pluralRules, microPropsGenerator);
    }

    public static LongNameHandler forMeasureUnit(ULocale uLocale, MeasureUnit measureUnit, NumberFormatter.UnitWidth unitWidth, PluralRules pluralRules, MicroPropsGenerator microPropsGenerator) {
        EnumMap enumMap = new EnumMap(StandardPlural.class);
        getMeasureData(uLocale, measureUnit, unitWidth, enumMap);
        EnumMap enumMap2 = new EnumMap(StandardPlural.class);
        simpleFormatsToModifiers(enumMap, null, enumMap2);
        return new LongNameHandler(enumMap2, pluralRules, microPropsGenerator);
    }

    private static void simpleFormatsToModifiers(Map<StandardPlural, String> map, NumberFormat.Field field, Map<StandardPlural, SimpleModifier> map2) {
        StringBuilder sb = new StringBuilder();
        for (StandardPlural standardPlural : StandardPlural.VALUES) {
            String str = map.get(standardPlural);
            if (str == null) {
                str = map.get(StandardPlural.OTHER);
            }
            if (str == null) {
                throw new ICUException("Could not find data in 'other' plural variant with field " + field);
            }
            map2.put(standardPlural, new SimpleModifier(SimpleFormatterImpl.compileToStringMinMaxArguments(str, sb, 1, 1), null, false));
        }
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
        MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
        DecimalQuantity decimalQuantityCreateCopy = decimalQuantity.createCopy();
        microPropsProcessQuantity.rounding.apply(decimalQuantityCreateCopy);
        microPropsProcessQuantity.modOuter = this.modifiers.get(decimalQuantityCreateCopy.getStandardPlural(this.rules));
        return microPropsProcessQuantity;
    }
}
