package com.android.packageinstaller.permission.ui.television;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.packageinstaller.R;

public abstract class SettingsWithHeader extends PermissionsFrameFragment implements View.OnClickListener {
    protected CharSequence mDecorTitle;
    private View mHeader;
    protected Drawable mIcon;
    protected Intent mInfoIntent;
    protected CharSequence mLabel;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        ViewGroup viewGroup2 = (ViewGroup) super.onCreateView(layoutInflater, viewGroup, bundle);
        this.mHeader = layoutInflater.inflate(R.layout.header, viewGroup2, false);
        getPreferencesContainer().addView(this.mHeader, 0);
        updateHeader();
        return viewGroup2;
    }

    public void setHeader(Drawable drawable, CharSequence charSequence, Intent intent, CharSequence charSequence2) {
        this.mIcon = drawable;
        this.mLabel = charSequence;
        this.mInfoIntent = intent;
        this.mDecorTitle = charSequence2;
        updateHeader();
    }

    protected void updateHeader() {
        ((TextView) this.mHeader.findViewById(R.id.decor_title)).setText(this.mDecorTitle);
    }

    @Override
    public void onClick(View view) {
        getActivity().startActivity(this.mInfoIntent);
    }
}
