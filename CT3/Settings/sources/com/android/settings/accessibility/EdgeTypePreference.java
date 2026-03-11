package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.internal.widget.SubtitleView;
import com.android.settings.R;

public class EdgeTypePreference extends ListDialogPreference {
    public EdgeTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        setValues(res.getIntArray(R.array.captioning_edge_type_selector_values));
        setTitles(res.getStringArray(R.array.captioning_edge_type_selector_titles));
        setDialogLayoutResource(R.layout.grid_picker_dialog);
        setListItemLayoutResource(R.layout.preset_picker_item);
    }

    @Override
    public boolean shouldDisableDependents() {
        if (getValue() != 0) {
            return super.shouldDisableDependents();
        }
        return true;
    }

    @Override
    protected void onBindListItem(View view, int index) {
        SubtitleView preview = view.findViewById(R.id.preview);
        preview.setForegroundColor(-1);
        preview.setBackgroundColor(0);
        float density = getContext().getResources().getDisplayMetrics().density;
        preview.setTextSize(32.0f * density);
        int value = getValueAt(index);
        preview.setEdgeType(value);
        preview.setEdgeColor(-16777216);
        CharSequence title = getTitleAt(index);
        if (title == null) {
            return;
        }
        TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(title);
    }
}
