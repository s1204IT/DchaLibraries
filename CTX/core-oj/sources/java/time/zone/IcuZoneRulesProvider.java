package java.time.zone;

import android.icu.util.AnnualTimeZoneRule;
import android.icu.util.BasicTimeZone;
import android.icu.util.InitialTimeZoneRule;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneRule;
import android.icu.util.TimeZoneTransition;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import libcore.util.BasicLruCache;

public class IcuZoneRulesProvider extends ZoneRulesProvider {
    private static final int MAX_TRANSITIONS = 10000;
    private static final int SECONDS_IN_DAY = 86400;
    private final BasicLruCache<String, ZoneRules> cache = new ZoneRulesCache(8);

    @Override
    protected Set<String> provideZoneIds() {
        HashSet hashSet = new HashSet(TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.ANY, null, null));
        hashSet.remove("GMT+0");
        hashSet.remove("GMT-0");
        return hashSet;
    }

    @Override
    protected ZoneRules provideRules(String str, boolean z) {
        return (ZoneRules) this.cache.get(str);
    }

    @Override
    protected NavigableMap<String, ZoneRules> provideVersions(String str) {
        return new TreeMap(Collections.singletonMap(TimeZone.getTZDataVersion(), provideRules(str, false)));
    }

    static ZoneRules generateZoneRules(String str) {
        ZoneOffset zoneOffset;
        ZoneOffset zoneOffset2;
        int i;
        boolean z;
        AnnualTimeZoneRule annualTimeZoneRule;
        boolean z2;
        int i2;
        BasicTimeZone frozenTimeZone = TimeZone.getFrozenTimeZone(str);
        verify(frozenTimeZone instanceof BasicTimeZone, str, "Unexpected time zone class " + ((Object) frozenTimeZone.getClass()));
        BasicTimeZone basicTimeZone = frozenTimeZone;
        boolean z3 = false;
        InitialTimeZoneRule initialTimeZoneRule = basicTimeZone.getTimeZoneRules()[0];
        ZoneOffset zoneOffsetMillisToOffset = millisToOffset(initialTimeZoneRule.getRawOffset());
        ZoneOffset zoneOffsetMillisToOffset2 = millisToOffset(initialTimeZoneRule.getRawOffset() + initialTimeZoneRule.getDSTSavings());
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        TimeZoneTransition nextTransition = basicTimeZone.getNextTransition(Long.MIN_VALUE, false);
        int i3 = 0;
        AnnualTimeZoneRule annualTimeZoneRule2 = null;
        int i4 = 1;
        while (true) {
            if (nextTransition != null) {
                TimeZoneRule from = nextTransition.getFrom();
                AnnualTimeZoneRule to = nextTransition.getTo();
                if (from.getRawOffset() != to.getRawOffset()) {
                    zoneOffset2 = zoneOffsetMillisToOffset;
                    zoneOffset = zoneOffsetMillisToOffset2;
                    arrayList.add(new ZoneOffsetTransition(TimeUnit.MILLISECONDS.toSeconds(nextTransition.getTime()), millisToOffset(from.getRawOffset()), millisToOffset(to.getRawOffset())));
                    z2 = true;
                } else {
                    zoneOffset = zoneOffsetMillisToOffset2;
                    zoneOffset2 = zoneOffsetMillisToOffset;
                    z2 = false;
                }
                int rawOffset = from.getRawOffset() + from.getDSTSavings();
                int rawOffset2 = to.getRawOffset() + to.getDSTSavings();
                if (rawOffset != rawOffset2) {
                    i2 = i4;
                    i = i3;
                    arrayList2.add(new ZoneOffsetTransition(TimeUnit.MILLISECONDS.toSeconds(nextTransition.getTime()), millisToOffset(rawOffset), millisToOffset(rawOffset2)));
                    z2 = true;
                } else {
                    i2 = i4;
                    i = i3;
                }
                verify(z2, str, "Transition changed neither total nor raw offset.");
                if (!(to instanceof AnnualTimeZoneRule)) {
                    verify(annualTimeZoneRule2 == null, str, "Unexpected rule after AnnualTimeZoneRule.");
                    i3 = i;
                } else if (annualTimeZoneRule2 == null) {
                    int dSTSavings = from.getDSTSavings();
                    AnnualTimeZoneRule annualTimeZoneRule3 = to;
                    verify(annualTimeZoneRule3.getEndYear() == Integer.MAX_VALUE, str, "AnnualTimeZoneRule is not permanent.");
                    i3 = dSTSavings;
                    annualTimeZoneRule2 = annualTimeZoneRule3;
                } else {
                    annualTimeZoneRule = to;
                    verify(annualTimeZoneRule.getEndYear() == Integer.MAX_VALUE, str, "AnnualTimeZoneRule is not permanent.");
                    verify(basicTimeZone.getNextTransition(nextTransition.getTime(), false).getTo() == annualTimeZoneRule2, str, "Unexpected rule after 2 AnnualTimeZoneRules.");
                    z = false;
                }
                int i5 = i2;
                verify(i5 <= MAX_TRANSITIONS, str, "More than 10000 transitions.");
                nextTransition = basicTimeZone.getNextTransition(nextTransition.getTime(), false);
                i4 = i5 + 1;
                z3 = false;
                zoneOffsetMillisToOffset = zoneOffset2;
                zoneOffsetMillisToOffset2 = zoneOffset;
            } else {
                zoneOffset = zoneOffsetMillisToOffset2;
                zoneOffset2 = zoneOffsetMillisToOffset;
                i = i3;
                z = z3;
                annualTimeZoneRule = null;
                break;
            }
        }
        if (annualTimeZoneRule2 != null) {
            if (annualTimeZoneRule != null) {
                z = true;
            }
            verify(z, str, "Only one AnnualTimeZoneRule.");
            arrayList3.add(toZoneOffsetTransitionRule(annualTimeZoneRule2, i));
            arrayList3.add(toZoneOffsetTransitionRule(annualTimeZoneRule, annualTimeZoneRule2.getDSTSavings()));
        }
        return ZoneRules.of(zoneOffset2, zoneOffset, arrayList, arrayList2, arrayList3);
    }

    private static void verify(boolean z, String str, String str2) {
        if (!z) {
            throw new ZoneRulesException(String.format("Failed verification of zone %s: %s", str, str2));
        }
    }

    private static java.time.zone.ZoneOffsetTransitionRule toZoneOffsetTransitionRule(android.icu.util.AnnualTimeZoneRule r13, int r14) {
        r0 = r13.getRule();
        r4 = java.time.Month.JANUARY.plus((long) r0.getRuleMonth());
        r1 = java.time.DayOfWeek.SATURDAY.plus((long) r0.getRuleDayOfWeek());
        switch (r0.getDateRuleType()) {
            case 0:
                r5 = r0.getRuleDayOfMonth();
                r6 = null;
                r1 = (int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds((long) r0.getRuleMillisInDay());
                if (r1 == java.time.zone.IcuZoneRulesProvider.SECONDS_IN_DAY) {
                    r7 = java.time.LocalTime.MIDNIGHT;
                    r8 = true;
                } else {
                    r7 = java.time.LocalTime.ofSecondOfDay((long) r1);
                    r8 = false;
                }
                switch (r0.getTimeRuleType()) {
                    case 0:
                        r0 = java.time.zone.ZoneOffsetTransitionRule.TimeDefinition.WALL;
                        break;
                    case 1:
                        r0 = java.time.zone.ZoneOffsetTransitionRule.TimeDefinition.STANDARD;
                        break;
                    case 2:
                        r0 = java.time.zone.ZoneOffsetTransitionRule.TimeDefinition.UTC;
                        break;
                    default:
                        r14 = new java.lang.StringBuilder();
                        r14.append("Unexpected time rule type ");
                        r14.append(r0.getTimeRuleType());
                        throw new java.time.zone.ZoneRulesException(r14.toString());
                }
                return java.time.zone.ZoneOffsetTransitionRule.of(r4, r5, r6, r7, r8, r0, millisToOffset(r13.getRawOffset()), millisToOffset(r13.getRawOffset() + r14), millisToOffset(r13.getRawOffset() + r13.getDSTSavings()));
            case 1:
                throw new java.time.zone.ZoneRulesException("Date rule type DOW is unsupported");
            case 2:
                r2 = r0.getRuleDayOfMonth();
                r6 = r1;
                r5 = r2;
                r1 = (int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds((long) r0.getRuleMillisInDay());
                if (r1 == java.time.zone.IcuZoneRulesProvider.SECONDS_IN_DAY) {
                }
                switch (r0.getTimeRuleType()) {
                }
                return java.time.zone.ZoneOffsetTransitionRule.of(r4, r5, r6, r7, r8, r0, millisToOffset(r13.getRawOffset()), millisToOffset(r13.getRawOffset() + r14), millisToOffset(r13.getRawOffset() + r13.getDSTSavings()));
            case 3:
                r2 = ((-r4.maxLength()) + r0.getRuleDayOfMonth()) - 1;
                r6 = r1;
                r5 = r2;
                r1 = (int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds((long) r0.getRuleMillisInDay());
                if (r1 == java.time.zone.IcuZoneRulesProvider.SECONDS_IN_DAY) {
                }
                switch (r0.getTimeRuleType()) {
                }
                return java.time.zone.ZoneOffsetTransitionRule.of(r4, r5, r6, r7, r8, r0, millisToOffset(r13.getRawOffset()), millisToOffset(r13.getRawOffset() + r14), millisToOffset(r13.getRawOffset() + r13.getDSTSavings()));
            default:
                r14 = new java.lang.StringBuilder();
                r14.append("Unexpected date rule type: ");
                r14.append(r0.getDateRuleType());
                throw new java.time.zone.ZoneRulesException(r14.toString());
        }
    }

    private static ZoneOffset millisToOffset(int i) {
        return ZoneOffset.ofTotalSeconds((int) TimeUnit.MILLISECONDS.toSeconds(i));
    }

    private static class ZoneRulesCache extends BasicLruCache<String, ZoneRules> {
        ZoneRulesCache(int i) {
            super(i);
        }

        protected ZoneRules create(String str) {
            String canonicalID = TimeZone.getCanonicalID(str);
            if (!canonicalID.equals(str)) {
                return (ZoneRules) get(canonicalID);
            }
            return IcuZoneRulesProvider.generateZoneRules(str);
        }
    }
}
