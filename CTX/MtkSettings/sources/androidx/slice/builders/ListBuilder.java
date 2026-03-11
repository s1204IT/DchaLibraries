package androidx.slice.builders;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.Consumer;
import androidx.slice.SliceSpecs;
import androidx.slice.builders.impl.ListBuilder;
import androidx.slice.builders.impl.ListBuilderBasicImpl;
import androidx.slice.builders.impl.ListBuilderV1Impl;
import androidx.slice.builders.impl.TemplateBuilderImpl;
import java.util.List;

public class ListBuilder extends TemplateSliceBuilder {
    private androidx.slice.builders.impl.ListBuilder mImpl;

    public ListBuilder(Context context, Uri uri, long ttl) {
        super(context, uri);
        this.mImpl.setTtl(ttl);
    }

    @Override
    void setImpl(TemplateBuilderImpl templateBuilderImpl) {
        this.mImpl = (androidx.slice.builders.impl.ListBuilder) templateBuilderImpl;
    }

    public ListBuilder addRow(RowBuilder builder) {
        this.mImpl.addRow((TemplateBuilderImpl) builder.mImpl);
        return this;
    }

    public ListBuilder addRow(Consumer<RowBuilder> c) {
        RowBuilder b = new RowBuilder(this);
        c.accept(b);
        return addRow(b);
    }

    @Deprecated
    public ListBuilder setColor(int color) {
        return setAccentColor(color);
    }

    public ListBuilder setAccentColor(int color) {
        this.mImpl.setColor(color);
        return this;
    }

    public ListBuilder setKeywords(List<String> keywords) {
        this.mImpl.setKeywords(keywords);
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

    public ListBuilder addInputRange(InputRangeBuilder b) {
        this.mImpl.addInputRange((TemplateBuilderImpl) b.mImpl);
        return this;
    }

    public ListBuilder addInputRange(Consumer<InputRangeBuilder> c) {
        InputRangeBuilder inputRangeBuilder = new InputRangeBuilder(this);
        c.accept(inputRangeBuilder);
        return addInputRange(inputRangeBuilder);
    }

    public static class InputRangeBuilder extends TemplateSliceBuilder {
        private ListBuilder.InputRangeBuilder mImpl;

        public InputRangeBuilder(ListBuilder parent) {
            super(parent.mImpl.createInputRangeBuilder());
        }

        public InputRangeBuilder setMax(int max) {
            this.mImpl.setMax(max);
            return this;
        }

        public InputRangeBuilder setValue(int value) {
            this.mImpl.setValue(value);
            return this;
        }

        public InputRangeBuilder setTitle(CharSequence title) {
            this.mImpl.setTitle(title);
            return this;
        }

        public InputRangeBuilder setSubtitle(CharSequence title) {
            this.mImpl.setSubtitle(title);
            return this;
        }

        public InputRangeBuilder setInputAction(PendingIntent action) {
            this.mImpl.setInputAction(action);
            return this;
        }

        public InputRangeBuilder setPrimaryAction(SliceAction action) {
            this.mImpl.setPrimaryAction(action);
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl templateBuilderImpl) {
            this.mImpl = (ListBuilder.InputRangeBuilder) templateBuilderImpl;
        }
    }

    public static class RowBuilder extends TemplateSliceBuilder {
        private boolean mHasDefaultToggle;
        private boolean mHasEndActionOrToggle;
        private boolean mHasEndImage;
        private ListBuilder.RowBuilder mImpl;

        public RowBuilder(ListBuilder parent) {
            super(parent.mImpl.createRowBuilder());
        }

        @Deprecated
        public RowBuilder setTitleItem(IconCompat icon) {
            return setTitleItem(icon, 0);
        }

        public RowBuilder setTitleItem(IconCompat icon, int imageMode) {
            this.mImpl.setTitleItem(icon, imageMode, false);
            return this;
        }

        public RowBuilder setPrimaryAction(SliceAction action) {
            this.mImpl.setPrimaryAction(action);
            return this;
        }

        public RowBuilder setTitle(CharSequence title) {
            this.mImpl.setTitle(title);
            return this;
        }

        public RowBuilder setSubtitle(CharSequence subtitle) {
            return setSubtitle(subtitle, false);
        }

        public RowBuilder setSubtitle(CharSequence subtitle, boolean isLoading) {
            this.mImpl.setSubtitle(subtitle, isLoading);
            return this;
        }

        public RowBuilder addEndItem(SliceAction action) {
            return addEndItem(action, false);
        }

        public RowBuilder addEndItem(SliceAction action, boolean isLoading) {
            if (this.mHasEndImage) {
                throw new IllegalArgumentException("Trying to add an action to end items when anicon has already been added. End items cannot have a mixture of actions and icons.");
            }
            if (this.mHasDefaultToggle) {
                throw new IllegalStateException("Only one non-custom toggle can be added in a single row. If you would like to include multiple toggles in a row, set a custom icon for each toggle.");
            }
            this.mImpl.addEndItem(action, isLoading);
            this.mHasDefaultToggle = action.getImpl().isDefaultToggle();
            this.mHasEndActionOrToggle = true;
            return this;
        }

        @Override
        void setImpl(TemplateBuilderImpl templateBuilderImpl) {
            this.mImpl = (ListBuilder.RowBuilder) templateBuilderImpl;
        }
    }
}
