package androidx.slice.builders.impl;

import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.builders.SliceAction;

public interface ListBuilder {

    public interface RowBuilder {
        void addEndItem(IconCompat iconCompat, int i, boolean z);

        void setContentDescription(CharSequence charSequence);

        void setPrimaryAction(SliceAction sliceAction);

        void setTitle(CharSequence charSequence);
    }

    void addRow(TemplateBuilderImpl templateBuilderImpl);

    TemplateBuilderImpl createRowBuilder(Uri uri);
}
