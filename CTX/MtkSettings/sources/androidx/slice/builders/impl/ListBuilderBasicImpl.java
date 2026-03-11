package androidx.slice.builders.impl;

import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.builders.SliceAction;
import androidx.slice.builders.impl.ListBuilder;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import java.util.List;

public class ListBuilderBasicImpl extends TemplateBuilderImpl implements ListBuilder {
    boolean mIsError;

    public ListBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    @Override
    public void addRow(TemplateBuilderImpl impl) {
    }

    @Override
    public void addInputRange(TemplateBuilderImpl builder) {
    }

    @Override
    public void setColor(int color) {
    }

    @Override
    public void setKeywords(List<String> keywords) {
        Slice.Builder sb = new Slice.Builder(getBuilder());
        for (int i = 0; i < keywords.size(); i++) {
            sb.addText(keywords.get(i), (String) null, new String[0]);
        }
        getBuilder().addSubSlice(sb.addHints("keywords").build());
    }

    @Override
    public void setTtl(long ttl) {
    }

    @Override
    public TemplateBuilderImpl createRowBuilder() {
        return new RowBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createInputRangeBuilder() {
        return new ListBuilderV1Impl.InputRangeBuilderImpl(getBuilder());
    }

    @Override
    public void apply(Slice.Builder builder) {
        if (this.mIsError) {
            builder.addHints("error");
        }
    }

    public static class RowBuilderImpl extends TemplateBuilderImpl implements ListBuilder.RowBuilder {
        public RowBuilderImpl(ListBuilderBasicImpl parent) {
            super(parent.createChildBuilder(), null);
        }

        @Override
        public void addEndItem(SliceAction action, boolean isLoading) {
        }

        @Override
        public void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
        }

        @Override
        public void setPrimaryAction(SliceAction action) {
        }

        @Override
        public void setTitle(CharSequence title) {
        }

        @Override
        public void setSubtitle(CharSequence subtitle, boolean isLoading) {
        }

        @Override
        public void apply(Slice.Builder builder) {
        }
    }
}
