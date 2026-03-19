package com.android.documentsui.inspector.actions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.inspector.InspectorController;

public final class ActionView extends LinearLayout implements InspectorController.ActionDisplay {
    private Action mAction;
    private final ImageButton mActionButton;
    private final ImageView mAppIcon;
    private final TextView mAppName;
    private Context mContext;
    private final TextView mHeader;

    public ActionView(Context context) {
        this(context, null);
    }

    public ActionView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ActionView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        addView(((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.inspector_action_view, (ViewGroup) null));
        this.mContext = context;
        this.mHeader = getSectionTitle();
        this.mAppIcon = (ImageView) findViewById(R.id.app_icon);
        this.mAppName = (TextView) findViewById(R.id.app_name);
        this.mActionButton = (ImageButton) findViewById(R.id.inspector_action_button);
    }

    public TextView getSectionTitle() {
        return (TextView) ((LinearLayout) findViewById(R.id.action_header)).findViewById(R.id.inspector_header_title);
    }

    @Override
    public void init(Action action, View.OnClickListener onClickListener) {
        this.mAction = action;
        setActionHeader(this.mAction.getHeader());
        setAppIcon(this.mAction.getAppIcon());
        setAppName(this.mAction.getAppName());
        this.mActionButton.setContentDescription(this.mContext.getString(action.getButtonLabel()));
        this.mActionButton.setOnClickListener(onClickListener);
        showAction(true);
    }

    @Override
    public void setVisible(boolean z) {
        setVisibility(z ? 0 : 8);
    }

    public void setActionHeader(String str) {
        this.mHeader.setText(str);
    }

    @Override
    public void setAppIcon(Drawable drawable) {
        if (drawable != null) {
            this.mAppIcon.setVisibility(0);
            this.mAppIcon.setImageDrawable(drawable);
        } else {
            this.mAppIcon.setVisibility(8);
        }
    }

    @Override
    public void setAppName(String str) {
        this.mAppName.setText(str);
    }

    @Override
    public void showAction(boolean z) {
        if (z) {
            this.mActionButton.setImageResource(this.mAction.getButtonIcon());
            this.mActionButton.setVisibility(0);
        } else {
            this.mActionButton.setVisibility(8);
        }
    }
}
