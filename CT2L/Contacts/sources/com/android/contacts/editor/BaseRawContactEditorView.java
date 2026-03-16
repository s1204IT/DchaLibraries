package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;

public abstract class BaseRawContactEditorView extends LinearLayout {
    private View mAccountHeaderContainer;
    private TextView mAccountName;
    private TextView mAccountType;
    private LinearLayout mCollapsibleSection;
    private ImageView mExpandAccountButton;
    protected Listener mListener;
    private PhotoEditorView mPhoto;

    public interface Listener {
        void onEditorExpansionChanged();

        void onExternalEditorRequest(AccountWithDataSet accountWithDataSet, Uri uri);
    }

    public abstract long getRawContactId();

    public abstract void setState(RawContactDelta rawContactDelta, AccountType accountType, ViewIdGenerator viewIdGenerator, boolean z);

    public BaseRawContactEditorView(Context context) {
        super(context);
    }

    public BaseRawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoto = (PhotoEditorView) findViewById(R.id.edit_photo);
        this.mPhoto.setEnabled(isEnabled());
        this.mAccountHeaderContainer = findViewById(R.id.account_header_container);
        this.mExpandAccountButton = (ImageView) findViewById(R.id.expand_account_button);
        this.mCollapsibleSection = (LinearLayout) findViewById(R.id.collapsable_section);
        this.mAccountName = (TextView) findViewById(R.id.account_name);
        this.mAccountType = (TextView) findViewById(R.id.account_type);
        setCollapsed(false);
        setCollapsible(true);
    }

    public void setGroupMetaData(Cursor groupMetaData) {
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setPhotoEntry(Bitmap bitmap) {
        this.mPhoto.setPhotoEntry(bitmap);
    }

    public void setFullSizedPhoto(Uri uri) {
        this.mPhoto.setFullSizedPhoto(uri);
    }

    protected void setHasPhotoEditor(boolean hasPhotoEditor) {
        this.mPhoto.setVisibility(hasPhotoEditor ? 0 : 8);
    }

    public boolean hasSetPhoto() {
        return this.mPhoto.hasSetPhoto();
    }

    public PhotoEditorView getPhotoEditor() {
        return this.mPhoto;
    }

    public void setCollapsible(boolean isCollapsible) {
        if (isCollapsible) {
            this.mAccountHeaderContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int startingHeight = BaseRawContactEditorView.this.mCollapsibleSection.getMeasuredHeight();
                    boolean isCollapsed = BaseRawContactEditorView.this.isCollapsed();
                    BaseRawContactEditorView.this.setCollapsed(!isCollapsed);
                    if (!isCollapsed) {
                        EditorAnimator.getInstance().slideAndFadeIn(BaseRawContactEditorView.this.mCollapsibleSection, startingHeight);
                        EditorAnimator.placeFocusAtTopOfScreenAfterReLayout(BaseRawContactEditorView.this.mCollapsibleSection);
                    } else {
                        EditorAnimator.getInstance().scrollViewToTop(BaseRawContactEditorView.this.mAccountHeaderContainer);
                        BaseRawContactEditorView.this.mCollapsibleSection.requestFocus();
                    }
                    if (BaseRawContactEditorView.this.mListener != null) {
                        BaseRawContactEditorView.this.mListener.onEditorExpansionChanged();
                    }
                    BaseRawContactEditorView.this.updateAccountHeaderContentDescription();
                }
            });
            this.mExpandAccountButton.setVisibility(0);
            this.mAccountHeaderContainer.setClickable(true);
        } else {
            this.mAccountHeaderContainer.setOnClickListener(null);
            this.mExpandAccountButton.setVisibility(8);
            this.mAccountHeaderContainer.setClickable(false);
        }
    }

    public boolean isCollapsed() {
        return this.mCollapsibleSection.getLayoutParams().height == 0;
    }

    public void setCollapsed(boolean isCollapsed) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) this.mCollapsibleSection.getLayoutParams();
        if (isCollapsed) {
            params.height = 0;
            this.mCollapsibleSection.setLayoutParams(params);
            this.mExpandAccountButton.setImageResource(R.drawable.ic_menu_expander_minimized_holo_light);
        } else {
            params.height = -2;
            this.mCollapsibleSection.setLayoutParams(params);
            this.mExpandAccountButton.setImageResource(R.drawable.ic_menu_expander_maximized_holo_light);
        }
    }

    protected void updateAccountHeaderContentDescription() {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(this.mAccountType.getText())) {
            builder.append(this.mAccountType.getText()).append('\n');
        }
        if (!TextUtils.isEmpty(this.mAccountName.getText())) {
            builder.append(this.mAccountName.getText()).append('\n');
        }
        if (this.mExpandAccountButton.getVisibility() == 0) {
            builder.append(getResources().getString(isCollapsed() ? R.string.content_description_expand_editor : R.string.content_description_collapse_editor));
        }
        this.mAccountHeaderContainer.setContentDescription(builder);
    }
}
