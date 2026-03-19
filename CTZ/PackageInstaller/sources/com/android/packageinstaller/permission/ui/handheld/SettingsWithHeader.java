package com.android.packageinstaller.permission.ui.handheld;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.R;

public abstract class SettingsWithHeader extends PermissionsFrameFragment implements View.OnClickListener {
    private View mHeader;
    protected Drawable mIcon;
    protected Intent mInfoIntent;
    protected CharSequence mLabel;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        ViewGroup viewGroup2 = (ViewGroup) super.onCreateView(layoutInflater, viewGroup, bundle);
        if (!DeviceUtils.isTelevision(getContext())) {
            this.mHeader = layoutInflater.inflate(R.layout.header, viewGroup2, false);
            getPreferencesContainer().addView(this.mHeader, 0);
            updateHeader();
        }
        return viewGroup2;
    }

    public void setHeader(Drawable drawable, CharSequence charSequence, Intent intent) {
        this.mIcon = drawable;
        this.mLabel = charSequence;
        this.mInfoIntent = intent;
        updateHeader();
    }

    private void updateHeader() {
        if (this.mHeader != null) {
            ((ImageView) this.mHeader.findViewById(R.id.icon)).setImageDrawable(this.mIcon);
            TextView textView = (TextView) this.mHeader.findViewById(R.id.name);
            textView.setText(this.mLabel);
            textView.setTextColor(-16777216);
            View viewFindViewById = this.mHeader.findViewById(R.id.info);
            if (this.mInfoIntent == null) {
                viewFindViewById.setVisibility(8);
                return;
            }
            viewFindViewById.setVisibility(0);
            viewFindViewById.setClickable(true);
            viewFindViewById.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        getActivity().startActivity(this.mInfoIntent);
    }
}
