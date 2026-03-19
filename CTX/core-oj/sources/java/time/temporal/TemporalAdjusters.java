package java.time.temporal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class TemporalAdjusters {
    private TemporalAdjusters() {
    }

    public static TemporalAdjuster ofDateAdjuster(final UnaryOperator<LocalDate> unaryOperator) {
        Objects.requireNonNull(unaryOperator, "dateBasedAdjuster");
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with((LocalDate) unaryOperator.apply(LocalDate.from((TemporalAccessor) temporal)));
            }
        };
    }

    public static TemporalAdjuster firstDayOfMonth() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_MONTH, 1L);
            }
        };
    }

    public static TemporalAdjuster lastDayOfMonth() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_MONTH, temporal.range(ChronoField.DAY_OF_MONTH).getMaximum());
            }
        };
    }

    public static TemporalAdjuster firstDayOfNextMonth() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_MONTH, 1L).plus(1L, ChronoUnit.MONTHS);
            }
        };
    }

    public static TemporalAdjuster firstDayOfYear() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_YEAR, 1L);
            }
        };
    }

    public static TemporalAdjuster lastDayOfYear() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_YEAR, temporal.range(ChronoField.DAY_OF_YEAR).getMaximum());
            }
        };
    }

    public static TemporalAdjuster firstDayOfNextYear() {
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.with(ChronoField.DAY_OF_YEAR, 1L).plus(1L, ChronoUnit.YEARS);
            }
        };
    }

    public static TemporalAdjuster firstInMonth(DayOfWeek dayOfWeek) {
        return dayOfWeekInMonth(1, dayOfWeek);
    }

    public static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
        return dayOfWeekInMonth(-1, dayOfWeek);
    }

    public static TemporalAdjuster dayOfWeekInMonth(final int i, DayOfWeek dayOfWeek) {
        Objects.requireNonNull(dayOfWeek, "dayOfWeek");
        final int value = dayOfWeek.getValue();
        if (i >= 0) {
            return new TemporalAdjuster() {
                @Override
                public final Temporal adjustInto(Temporal temporal) {
                    int i2 = value;
                    int i3 = i;
                    return temporal.with(ChronoField.DAY_OF_MONTH, 1L).plus((int) (((long) (((i2 - temporal.get(ChronoField.DAY_OF_WEEK)) + 7) % 7)) + ((((long) i3) - 1) * 7)), ChronoUnit.DAYS);
                }
            };
        }
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return TemporalAdjusters.lambda$dayOfWeekInMonth$8(value, i, temporal);
            }
        };
    }

    static Temporal lambda$dayOfWeekInMonth$8(int i, int i2, Temporal temporal) {
        Temporal temporalWith = temporal.with(ChronoField.DAY_OF_MONTH, temporal.range(ChronoField.DAY_OF_MONTH).getMaximum());
        int i3 = i - temporalWith.get(ChronoField.DAY_OF_WEEK);
        if (i3 == 0) {
            i3 = 0;
        } else if (i3 > 0) {
            i3 -= 7;
        }
        return temporalWith.plus((int) (((long) i3) - ((((long) (-i2)) - 1) * 7)), ChronoUnit.DAYS);
    }

    public static TemporalAdjuster next(DayOfWeek dayOfWeek) {
        final int value = dayOfWeek.getValue();
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return TemporalAdjusters.lambda$next$9(value, temporal);
            }
        };
    }

    static Temporal lambda$next$9(int i, Temporal temporal) {
        return temporal.plus(temporal.get(ChronoField.DAY_OF_WEEK) - i >= 0 ? 7 - r0 : -r0, ChronoUnit.DAYS);
    }

    public static TemporalAdjuster nextOrSame(DayOfWeek dayOfWeek) {
        final int value = dayOfWeek.getValue();
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return TemporalAdjusters.lambda$nextOrSame$10(value, temporal);
            }
        };
    }

    static Temporal lambda$nextOrSame$10(int i, Temporal temporal) {
        int i2 = temporal.get(ChronoField.DAY_OF_WEEK);
        if (i2 == i) {
            return temporal;
        }
        return temporal.plus(i2 - i >= 0 ? 7 - r0 : -r0, ChronoUnit.DAYS);
    }

    public static TemporalAdjuster previous(DayOfWeek dayOfWeek) {
        final int value = dayOfWeek.getValue();
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return temporal.minus(value - temporal.get(ChronoField.DAY_OF_WEEK) >= 0 ? 7 - r0 : -r0, ChronoUnit.DAYS);
            }
        };
    }

    public static TemporalAdjuster previousOrSame(DayOfWeek dayOfWeek) {
        final int value = dayOfWeek.getValue();
        return new TemporalAdjuster() {
            @Override
            public final Temporal adjustInto(Temporal temporal) {
                return TemporalAdjusters.lambda$previousOrSame$12(value, temporal);
            }
        };
    }

    static Temporal lambda$previousOrSame$12(int i, Temporal temporal) {
        int i2 = temporal.get(ChronoField.DAY_OF_WEEK);
        if (i2 == i) {
            return temporal;
        }
        return temporal.minus(i - i2 >= 0 ? 7 - r2 : -r2, ChronoUnit.DAYS);
    }
}
