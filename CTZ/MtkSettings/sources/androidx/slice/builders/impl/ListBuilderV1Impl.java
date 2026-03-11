package androidx.slice.builders.impl;

import android.app.PendingIntent;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceSpec;
import androidx.slice.builders.SliceAction;
import androidx.slice.builders.impl.ListBuilder;
import java.util.ArrayList;
import java.util.List;

public class ListBuilderV1Impl extends TemplateBuilderImpl implements ListBuilder {
    private boolean mIsError;
    private List<Slice> mSliceActions;
    private Slice mSliceHeader;

    public ListBuilderV1Impl(Slice.Builder b, SliceSpec spec, Clock clock) {
        super(b, spec, clock);
    }

    @Override
    public void apply(Slice.Builder builder) {
        builder.addLong(getClock().currentTimeMillis(), "millis", "last_updated");
        if (this.mSliceHeader != null) {
            builder.addSubSlice(this.mSliceHeader);
        }
        if (this.mSliceActions != null) {
            Slice.Builder sb = new Slice.Builder(builder);
            for (int i = 0; i < this.mSliceActions.size(); i++) {
                sb.addSubSlice(this.mSliceActions.get(i));
            }
            builder.addSubSlice(sb.addHints("actions").build());
        }
        if (this.mIsError) {
            builder.addHints("error");
        }
    }

    @Override
    public void addRow(TemplateBuilderImpl builder) {
        builder.getBuilder().addHints("list_item");
        getBuilder().addSubSlice(builder.build());
    }

    @Override
    public void addInputRange(TemplateBuilderImpl builder) {
        getBuilder().addSubSlice(builder.build(), "range");
    }

    public static class RangeBuilderImpl extends TemplateBuilderImpl implements ListBuilder.RangeBuilder {
        private CharSequence mContentDescr;
        private int mLayoutDir;
        private int mMax;
        private int mMin;
        private SliceAction mPrimaryAction;
        private CharSequence mSubtitle;
        private CharSequence mTitle;
        private int mValue;
        private boolean mValueSet;

        public RangeBuilderImpl(Slice.Builder sb) {
            super(sb, null);
            this.mMin = 0;
            this.mMax = 100;
            this.mValue = 0;
            this.mValueSet = false;
            this.mLayoutDir = -1;
        }

        @Override
        public void setMax(int max) {
            this.mMax = max;
        }

        @Override
        public void setValue(int value) {
            this.mValue = value;
            this.mValueSet = true;
        }

        @Override
        public void setTitle(CharSequence title) {
            this.mTitle = title;
        }

        @Override
        public void setSubtitle(CharSequence title) {
            this.mSubtitle = title;
        }

        @Override
        public void setPrimaryAction(SliceAction action) {
            this.mPrimaryAction = action;
        }

        @Override
        public void apply(Slice.Builder builder) {
            if (!this.mValueSet) {
                this.mValue = this.mMin;
            }
            if (this.mMin > this.mValue || this.mValue > this.mMax || this.mMin >= this.mMax) {
                throw new IllegalArgumentException("Invalid range values, min=" + this.mMin + ", value=" + this.mValue + ", max=" + this.mMax + " ensure value falls within (min, max) and min < max.");
            }
            if (this.mTitle != null) {
                builder.addText(this.mTitle, (String) null, "title");
            }
            if (this.mSubtitle != null) {
                builder.addText(this.mSubtitle, (String) null, new String[0]);
            }
            if (this.mContentDescr != null) {
                builder.addText(this.mContentDescr, "content_description", new String[0]);
            }
            if (this.mPrimaryAction != null) {
                Slice.Builder sb = new Slice.Builder(getBuilder()).addHints("title", "shortcut");
                builder.addSubSlice(this.mPrimaryAction.buildSlice(sb), null);
            }
            if (this.mLayoutDir != -1) {
                builder.addInt(this.mLayoutDir, "layout_direction", new String[0]);
            }
            builder.addHints("list_item").addInt(this.mMin, "min", new String[0]).addInt(this.mMax, "max", new String[0]).addInt(this.mValue, "value", new String[0]);
        }
    }

    public static class InputRangeBuilderImpl extends RangeBuilderImpl implements ListBuilder.InputRangeBuilder {
        private PendingIntent mAction;
        private IconCompat mThumb;

        public InputRangeBuilderImpl(Slice.Builder sb) {
            super(sb);
        }

        @Override
        public void setInputAction(PendingIntent action) {
            this.mAction = action;
        }

        @Override
        public void apply(Slice.Builder builder) {
            if (this.mAction == null) {
                throw new IllegalStateException("Input ranges must have an associated action.");
            }
            Slice.Builder sb = new Slice.Builder(builder);
            super.apply(sb);
            if (this.mThumb != null) {
                sb.addIcon(this.mThumb, (String) null, new String[0]);
            }
            builder.addAction(this.mAction, sb.build(), "range").addHints("list_item");
        }
    }

    @Override
    public void setColor(int color) {
        getBuilder().addInt(color, "color", new String[0]);
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
        long expiry = ttl != -1 ? getClock().currentTimeMillis() + ttl : -1L;
        getBuilder().addTimestamp(expiry, "millis", "ttl");
    }

    @Override
    public TemplateBuilderImpl createRowBuilder() {
        return new RowBuilderImpl(this);
    }

    @Override
    public TemplateBuilderImpl createInputRangeBuilder() {
        return new InputRangeBuilderImpl(createChildBuilder());
    }

    public static class RowBuilderImpl extends TemplateBuilderImpl implements ListBuilder.RowBuilder {
        private CharSequence mContentDescr;
        private ArrayList<Slice> mEndItems;
        private SliceAction mPrimaryAction;
        private Slice mStartItem;
        private SliceItem mSubtitleItem;
        private SliceItem mTitleItem;

        public RowBuilderImpl(ListBuilderV1Impl parent) {
            super(parent.createChildBuilder(), null);
            this.mEndItems = new ArrayList<>();
        }

        @Override
        public void setTitleItem(IconCompat icon, int imageMode, boolean isLoading) {
            ArrayList<String> hints = new ArrayList<>();
            if (imageMode != 0) {
                hints.add("no_tint");
            }
            if (imageMode == 2) {
                hints.add("large");
            }
            if (isLoading) {
                hints.add("partial");
            }
            Slice.Builder sb = new Slice.Builder(getBuilder()).addIcon(icon, (String) null, hints);
            if (isLoading) {
                sb.addHints("partial");
            }
            this.mStartItem = sb.addHints("title").build();
        }

        @Override
        public void setPrimaryAction(SliceAction action) {
            this.mPrimaryAction = action;
        }

        @Override
        public void setTitle(CharSequence title) {
            setTitle(title, false);
        }

        public void setTitle(CharSequence title, boolean isLoading) {
            this.mTitleItem = new SliceItem(title, "text", (String) null, new String[]{"title"});
            if (isLoading) {
                this.mTitleItem.addHint("partial");
            }
        }

        @Override
        public void setSubtitle(CharSequence subtitle, boolean isLoading) {
            this.mSubtitleItem = new SliceItem(subtitle, "text", (String) null, new String[0]);
            if (isLoading) {
                this.mSubtitleItem.addHint("partial");
            }
        }

        @Override
        public void addEndItem(SliceAction action, boolean isLoading) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            if (isLoading) {
                sb.addHints("partial");
            }
            this.mEndItems.add(action.buildSlice(sb));
        }

        @Override
        public void apply(Slice.Builder b) {
            if (this.mStartItem != null) {
                b.addSubSlice(this.mStartItem);
            }
            if (this.mTitleItem != null) {
                b.addItem(this.mTitleItem);
            }
            if (this.mSubtitleItem != null) {
                b.addItem(this.mSubtitleItem);
            }
            for (int i = 0; i < this.mEndItems.size(); i++) {
                Slice item = this.mEndItems.get(i);
                b.addSubSlice(item);
            }
            if (this.mContentDescr != null) {
                b.addText(this.mContentDescr, "content_description", new String[0]);
            }
            if (this.mPrimaryAction != null) {
                Slice.Builder sb = new Slice.Builder(getBuilder()).addHints("title", "shortcut");
                b.addSubSlice(this.mPrimaryAction.buildSlice(sb), null);
            }
        }
    }
}
