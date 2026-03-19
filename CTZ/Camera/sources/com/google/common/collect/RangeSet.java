package com.google.common.collect;

import java.lang.Comparable;
import java.util.Set;

public interface RangeSet<C extends Comparable> {
    Set<Range<C>> asRanges();
}
