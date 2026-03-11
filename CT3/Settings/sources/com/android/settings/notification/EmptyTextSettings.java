package com.android.settings.notification;

import android.R;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.SettingsPreferenceFragment;

public abstract class EmptyTextSettings extends SettingsPreferenceFragment {
    private TextView mEmpty;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mEmpty = new TextView(getContext());
        this.mEmpty.setGravity(17);
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.textAppearanceMedium, value, true);
        this.mEmpty.setTextAppearance(value.resourceId);
        ((ViewGroup) view.findViewById(R.id.list_container)).addView(this.mEmpty, new ViewGroup.LayoutParams(-1, -1));
        setEmptyView(this.mEmpty);
    }

    protected void setEmptyText(int text) {
        this.mEmpty.setText(text);
    }
}
