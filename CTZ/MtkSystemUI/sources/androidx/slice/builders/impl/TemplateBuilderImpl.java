package androidx.slice.builders.impl;

import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.SystemClock;

public abstract class TemplateBuilderImpl {
    private Clock mClock;
    private final Slice.Builder mSliceBuilder;
    private final SliceSpec mSpec;

    public abstract void apply(Slice.Builder builder);

    protected TemplateBuilderImpl(Slice.Builder b, SliceSpec spec) {
        this(b, spec, new SystemClock());
    }

    protected TemplateBuilderImpl(Slice.Builder b, SliceSpec spec, Clock clock) {
        this.mSliceBuilder = b;
        this.mSpec = spec;
        this.mClock = clock;
    }

    public Slice build() {
        this.mSliceBuilder.setSpec(this.mSpec);
        apply(this.mSliceBuilder);
        return this.mSliceBuilder.build();
    }

    public Slice.Builder getBuilder() {
        return this.mSliceBuilder;
    }

    public Clock getClock() {
        return this.mClock;
    }
}
