package androidx.slice.builders.impl;

import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.builders.SliceAction;
import androidx.slice.builders.impl.ListBuilder;

public class ListBuilderBasicImpl extends TemplateBuilderImpl implements ListBuilder {
    boolean mIsError;

    public ListBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    @Override
    public void addRow(TemplateBuilderImpl impl) {
    }

    @Override
    public TemplateBuilderImpl createRowBuilder(Uri uri) {
        return new RowBuilderImpl(uri);
    }

    @Override
    public void apply(Slice.Builder builder) {
        if (this.mIsError) {
            builder.addHints("error");
        }
    }

    public static class RowBuilderImpl extends TemplateBuilderImpl implements ListBuilder.RowBuilder {
        public RowBuilderImpl(Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        @Override
        public void setContentDescription(CharSequence description) {
        }

        @Override
        public void setPrimaryAction(SliceAction action) {
        }

        @Override
        public void setTitle(CharSequence title) {
        }

        @Override
        public void addEndItem(IconCompat icon, int imageMode, boolean isLoading) {
        }

        @Override
        public void apply(Slice.Builder builder) {
        }
    }
}
