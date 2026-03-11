package androidx.slice.builders;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.SliceSpec;
import androidx.slice.SliceSpecs;
import androidx.slice.SystemClock;
import androidx.slice.builders.impl.TemplateBuilderImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TemplateSliceBuilder {
    private final Slice.Builder mBuilder;
    private final Context mContext;
    private final TemplateBuilderImpl mImpl;
    private List<SliceSpec> mSpecs;

    abstract void setImpl(TemplateBuilderImpl templateBuilderImpl);

    protected TemplateSliceBuilder(TemplateBuilderImpl impl) {
        this.mContext = null;
        this.mBuilder = null;
        this.mImpl = impl;
        setImpl(impl);
    }

    public TemplateSliceBuilder(Context context, Uri uri) {
        this.mBuilder = new Slice.Builder(uri);
        this.mContext = context;
        this.mSpecs = getSpecs();
        this.mImpl = selectImpl();
        if (this.mImpl == null) {
            throw new IllegalArgumentException("No valid specs found");
        }
        setImpl(this.mImpl);
    }

    public Slice build() {
        return this.mImpl.build();
    }

    protected Slice.Builder getBuilder() {
        return this.mBuilder;
    }

    protected TemplateBuilderImpl selectImpl() {
        return null;
    }

    protected boolean checkCompatible(SliceSpec candidate) {
        int size = this.mSpecs.size();
        for (int i = 0; i < size; i++) {
            if (this.mSpecs.get(i).canRender(candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<SliceSpec> getSpecs() {
        if (SliceProvider.getCurrentSpecs() != null) {
            return new ArrayList(SliceProvider.getCurrentSpecs());
        }
        Log.w("TemplateSliceBuilder", "Not currently bunding a slice");
        return Arrays.asList(SliceSpecs.BASIC);
    }

    protected Clock getClock() {
        if (SliceProvider.getClock() != null) {
            return SliceProvider.getClock();
        }
        return new SystemClock();
    }
}
