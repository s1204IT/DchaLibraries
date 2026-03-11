package androidx.slice.builders;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.ListBuilder;
import androidx.slice.builders.impl.ListBuilderBasicImpl;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;

public class ListBuilder extends TemplateSliceBuilder {
    private androidx.slice.builders.impl.ListBuilder mImpl;

    @Deprecated
    public ListBuilder(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    void setImpl(TemplateBuilderImpl templateBuilderImpl) {
        this.mImpl = (androidx.slice.builders.impl.ListBuilder) templateBuilderImpl;
    }

    public ListBuilder addRow(RowBuilder builder) {
        this.mImpl.addRow((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    @Override
    protected TemplateBuilderImpl selectImpl() {
        if (checkCompatible(SliceSpecs.LIST)) {
            return new ListBuilderV1Impl(getBuilder(), SliceSpecs.LIST, getClock());
        }
        if (checkCompatible(SliceSpecs.BASIC)) {
            return new ListBuilderBasicImpl(getBuilder(), SliceSpecs.BASIC);
        }
        return null;
    }

    public static class RowBuilder extends TemplateSliceBuilder {
        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private ListBuilder.RowBuilder mImpl;

        public RowBuilder(ListBuilder parent, Uri uri) {
            super(parent.mImpl.createRowBuilder(uri));
        }

        public RowBuilder setPrimaryAction(SliceAction action) {
            this.mImpl.setPrimaryAction(action);
            return this;
        }

        public RowBuilder setTitle(CharSequence title) {
            this.mImpl.setTitle(title);
            return this;
        }

        @Deprecated
        public RowBuilder addEndItem(Icon icon) {
            return addEndItem(icon, 0, false);
        }

        @Deprecated
        public RowBuilder addEndItem(Icon icon, int imageMode, boolean isLoading) {
            if (this.mHasEndActionOrToggle) {
                throw new IllegalArgumentException("Trying to add an icon to end items when anaction has already been added. End items cannot have a mixture of actions and icons.");
            }
            this.mImpl.addEndItem(IconCompat.createFromIcon(icon), imageMode, isLoading);
            this.mHasEndImage = true;
            return this;
        }

        public RowBuilder setContentDescription(CharSequence description) {
            this.mImpl.setContentDescription(description);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl templateBuilderImpl) {
            this.mImpl = (ListBuilder.RowBuilder) templateBuilderImpl;
        }
    }
}
