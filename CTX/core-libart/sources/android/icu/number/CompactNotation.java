package android.icu.number;

import android.icu.impl.number.CompactData;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.MutablePatternModifier;
import android.icu.impl.number.PatternStringParser;
import android.icu.text.CompactDecimalFormat;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CompactNotation extends Notation {
    final Map<String, Map<String, String>> compactCustomData;
    final CompactDecimalFormat.CompactStyle compactStyle;

    CompactNotation(CompactDecimalFormat.CompactStyle compactStyle) {
        this.compactCustomData = null;
        this.compactStyle = compactStyle;
    }

    CompactNotation(Map<String, Map<String, String>> map) {
        this.compactStyle = null;
        this.compactCustomData = map;
    }

    MicroPropsGenerator withLocaleData(ULocale uLocale, String str, CompactData.CompactType compactType, PluralRules pluralRules, MutablePatternModifier mutablePatternModifier, MicroPropsGenerator microPropsGenerator) {
        return new CompactHandler(uLocale, str, compactType, pluralRules, mutablePatternModifier, microPropsGenerator);
    }

    private static class CompactHandler implements MicroPropsGenerator {
        static final boolean $assertionsDisabled = false;
        final CompactData data;
        final MicroPropsGenerator parent;
        final Map<String, CompactModInfo> precomputedMods;
        final PluralRules rules;

        private static class CompactModInfo {
            public MutablePatternModifier.ImmutablePatternModifier mod;
            public int numDigits;

            private CompactModInfo() {
            }
        }

        private CompactHandler(CompactNotation compactNotation, ULocale uLocale, String str, CompactData.CompactType compactType, PluralRules pluralRules, MutablePatternModifier mutablePatternModifier, MicroPropsGenerator microPropsGenerator) {
            this.rules = pluralRules;
            this.parent = microPropsGenerator;
            this.data = new CompactData();
            if (compactNotation.compactStyle != null) {
                this.data.populate(uLocale, str, compactNotation.compactStyle, compactType);
            } else {
                this.data.populate(compactNotation.compactCustomData);
            }
            if (mutablePatternModifier != null) {
                this.precomputedMods = new HashMap();
                precomputeAllModifiers(mutablePatternModifier);
            } else {
                this.precomputedMods = null;
            }
        }

        private void precomputeAllModifiers(MutablePatternModifier mutablePatternModifier) {
            HashSet<String> hashSet = new HashSet();
            this.data.getUniquePatterns(hashSet);
            for (String str : hashSet) {
                CompactModInfo compactModInfo = new CompactModInfo();
                PatternStringParser.ParsedPatternInfo toPatternInfo = PatternStringParser.parseToPatternInfo(str);
                mutablePatternModifier.setPatternInfo(toPatternInfo);
                compactModInfo.mod = mutablePatternModifier.createImmutable();
                compactModInfo.numDigits = toPatternInfo.positive.integerTotal;
                this.precomputedMods.put(str, compactModInfo);
            }
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
            MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
            if (decimalQuantity.isZero()) {
                microPropsProcessQuantity.rounding.apply(decimalQuantity);
            } else {
                magnitude = (decimalQuantity.isZero() ? 0 : decimalQuantity.getMagnitude()) - microPropsProcessQuantity.rounding.chooseMultiplierAndApply(decimalQuantity, this.data);
            }
            String pattern = this.data.getPattern(magnitude, decimalQuantity.getStandardPlural(this.rules));
            if (pattern != null) {
                if (this.precomputedMods != null) {
                    CompactModInfo compactModInfo = this.precomputedMods.get(pattern);
                    compactModInfo.mod.applyToMicros(microPropsProcessQuantity, decimalQuantity);
                    int i = compactModInfo.numDigits;
                } else {
                    PatternStringParser.ParsedPatternInfo toPatternInfo = PatternStringParser.parseToPatternInfo(pattern);
                    ((MutablePatternModifier) microPropsProcessQuantity.modMiddle).setPatternInfo(toPatternInfo);
                    int i2 = toPatternInfo.positive.integerTotal;
                }
            }
            microPropsProcessQuantity.rounding = Rounder.constructPassThrough();
            return microPropsProcessQuantity;
        }
    }
}
