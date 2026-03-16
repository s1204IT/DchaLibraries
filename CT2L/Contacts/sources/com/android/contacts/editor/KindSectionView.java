package com.android.contacts.editor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.contacts.R;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.editor.Editor;
import java.util.ArrayList;
import java.util.List;

public class KindSectionView extends LinearLayout implements Editor.EditorListener {
    private ViewGroup mEditors;
    private ImageView mIcon;
    private LayoutInflater mInflater;
    private DataKind mKind;
    private boolean mReadOnly;
    private RawContactDelta mState;
    private ViewIdGenerator mViewIdGenerator;

    public KindSectionView(Context context) {
        this(context, null);
    }

    public KindSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mEditors != null) {
            int childCount = this.mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mEditors.getChildAt(i).setEnabled(enabled);
            }
        }
        updateEmptyEditors(true);
    }

    public boolean isReadOnly() {
        return this.mReadOnly;
    }

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);
        this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        this.mIcon = (ImageView) findViewById(R.id.kind_icon);
    }

    @Override
    public void onDeleteRequested(Editor editor) {
        if (getEditorCount() == 1) {
            editor.clearAllFields();
        } else {
            editor.deleteEditor();
        }
    }

    @Override
    public void onRequest(int request) {
        if (request == 3 || request == 4) {
            updateEmptyEditors(true);
        }
    }

    public void setState(DataKind kind, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        this.mKind = kind;
        this.mState = state;
        this.mReadOnly = readOnly;
        this.mViewIdGenerator = vig;
        setId(this.mViewIdGenerator.getId(state, kind, null, -1));
        String titleString = (kind.titleRes == -1 || kind.titleRes == 0) ? "" : getResources().getString(kind.titleRes);
        this.mIcon.setContentDescription(titleString);
        this.mIcon.setImageDrawable(getMimeTypeDrawable(kind.mimeType));
        if (this.mIcon.getDrawable() == null) {
            this.mIcon.setContentDescription(null);
        }
        rebuildFromState();
        updateEmptyEditors(false);
    }

    private void rebuildFromState() {
        this.mEditors.removeAllViews();
        boolean hasEntries = this.mState.hasMimeEntries(this.mKind.mimeType);
        if (hasEntries) {
            for (ValuesDelta entry : this.mState.getMimeEntries(this.mKind.mimeType)) {
                if (entry.isVisible()) {
                    createEditorView(entry);
                }
            }
        }
    }

    private View createEditorView(ValuesDelta entry) {
        int layoutResId = EditorUiUtils.getLayoutResourceId(this.mKind.mimeType);
        try {
            View viewInflate = this.mInflater.inflate(layoutResId, this.mEditors, false);
            viewInflate.setEnabled(isEnabled());
            if (viewInflate instanceof Editor) {
                Editor editor = (Editor) viewInflate;
                editor.setDeletable(true);
                editor.setValues(this.mKind, entry, this.mState, this.mReadOnly, this.mViewIdGenerator);
                editor.setEditorListener(this);
            }
            this.mEditors.addView(viewInflate);
            return viewInflate;
        } catch (Exception e) {
            throw new RuntimeException("Cannot allocate editor with layout resource ID " + layoutResId + " for MIME type " + this.mKind.mimeType + " with error " + e.toString());
        }
    }

    private void updateEmptyEditors(boolean shouldAnimate) {
        List<View> emptyEditors = getEmptyEditors();
        if (emptyEditors.size() > 1) {
            for (View view : emptyEditors) {
                if (view.findFocus() == null) {
                    Editor editor = (Editor) view;
                    if (shouldAnimate) {
                        editor.deleteEditor();
                    } else {
                        this.mEditors.removeView(view);
                    }
                }
            }
            return;
        }
        if (this.mKind != null && !isReadOnly()) {
            if ((this.mKind.typeOverallMax != getEditorCount() || this.mKind.typeOverallMax == 0) && emptyEditors.size() != 1) {
                ValuesDelta values = RawContactModifier.insertChild(this.mState, this.mKind);
                View newField = createEditorView(values);
                if (shouldAnimate) {
                    newField.setVisibility(8);
                    EditorAnimator.getInstance().showFieldFooter(newField);
                }
            }
        }
    }

    private List<View> getEmptyEditors() {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mEditors.getChildCount(); i++) {
            KeyEvent.Callback childAt = this.mEditors.getChildAt(i);
            if (((Editor) childAt).isEmpty()) {
                arrayList.add(childAt);
            }
        }
        return arrayList;
    }

    public int getEditorCount() {
        return this.mEditors.getChildCount();
    }

    private Drawable getMimeTypeDrawable(String mimeType) {
        switch (mimeType) {
            case "vnd.android.cursor.item/postal-address_v2":
                return getResources().getDrawable(R.drawable.ic_place_24dp);
            case "vnd.android.cursor.item/sip_address":
                return getResources().getDrawable(R.drawable.ic_dialer_sip_black_24dp);
            case "vnd.android.cursor.item/phone_v2":
                return getResources().getDrawable(R.drawable.ic_phone_24dp);
            case "vnd.android.cursor.item/im":
                return getResources().getDrawable(R.drawable.ic_message_24dp);
            case "vnd.android.cursor.item/contact_event":
                return getResources().getDrawable(R.drawable.ic_event_24dp);
            case "vnd.android.cursor.item/email_v2":
                return getResources().getDrawable(R.drawable.ic_email_24dp);
            case "vnd.android.cursor.item/website":
                return getResources().getDrawable(R.drawable.ic_public_black_24dp);
            case "vnd.android.cursor.item/photo":
                return getResources().getDrawable(R.drawable.ic_camera_alt_black_24dp);
            case "vnd.android.cursor.item/contactsgroupmembership":
                return getResources().getDrawable(R.drawable.ic_people_black_24dp);
            case "vnd.android.cursor.item/organization":
                return getResources().getDrawable(R.drawable.ic_business_black_24dp);
            case "vnd.android.cursor.item/note":
                return getResources().getDrawable(R.drawable.ic_insert_comment_black_24dp);
            case "vnd.android.cursor.item/relation":
                return getResources().getDrawable(R.drawable.ic_circles_extended_black_24dp);
            default:
                return null;
        }
    }
}
