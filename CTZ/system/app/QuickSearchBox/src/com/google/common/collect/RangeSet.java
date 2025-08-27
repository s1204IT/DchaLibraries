package com.google.common.collect;

import java.lang.Comparable;
import java.util.Set;

/* loaded from: classes.dex */
public interface RangeSet<C extends Comparable> {
    Set<Range<C>> asRanges();
}
