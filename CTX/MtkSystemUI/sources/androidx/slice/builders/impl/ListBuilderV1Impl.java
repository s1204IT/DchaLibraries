package androidx.slice.builders.impl;

import android.net.Uri;
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
    public TemplateBuilderImpl createRowBuilder(Uri uri) {
        return new RowBuilderImpl(uri);
    }

    public static class RowBuilderImpl extends TemplateBuilderImpl implements ListBuilder.RowBuilder {
        private CharSequence mContentDescr;
        private ArrayList<Slice> mEndItems;
        private SliceAction mPrimaryAction;
        private Slice mStartItem;
        private SliceItem mSubtitleItem;
        private SliceItem mTitleItem;

        public RowBuilderImpl(Uri uri) {
            super(new Slice.Builder(uri), null);
            this.mEndItems = new ArrayList<>();
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
        public void addEndItem(IconCompat icon, int imageMode, boolean isLoading) {
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
            this.mEndItems.add(sb.build());
        }

        @Override
        public void setContentDescription(CharSequence description) {
            this.mContentDescr = description;
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
