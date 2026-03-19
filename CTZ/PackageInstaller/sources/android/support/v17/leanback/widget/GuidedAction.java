package android.support.v17.leanback.widget;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import java.util.List;

public class GuidedAction extends Action {
    int mActionFlags;
    String[] mAutofillHints;
    int mCheckSetId;
    int mDescriptionEditInputType;
    int mDescriptionInputType;
    private CharSequence mEditDescription;
    int mEditInputType;
    private CharSequence mEditTitle;
    int mEditable;
    int mInputType;
    Intent mIntent;
    List<GuidedAction> mSubActions;

    public static abstract class BuilderBase<B extends BuilderBase> {
        private String[] mAutofillHints;
        private Context mContext;
        private CharSequence mDescription;
        private CharSequence mEditDescription;
        private CharSequence mEditTitle;
        private Drawable mIcon;
        private long mId;
        private Intent mIntent;
        private List<GuidedAction> mSubActions;
        private CharSequence mTitle;
        private int mEditable = 0;
        private int mInputType = 524289;
        private int mDescriptionInputType = 524289;
        private int mEditInputType = 1;
        private int mDescriptionEditInputType = 1;
        private int mCheckSetId = 0;
        private int mActionFlags = 112;

        public BuilderBase(Context context) {
            this.mContext = context;
        }

        protected final void applyValues(GuidedAction action) {
            action.setId(this.mId);
            action.setLabel1(this.mTitle);
            action.setEditTitle(this.mEditTitle);
            action.setLabel2(this.mDescription);
            action.setEditDescription(this.mEditDescription);
            action.setIcon(this.mIcon);
            action.mIntent = this.mIntent;
            action.mEditable = this.mEditable;
            action.mInputType = this.mInputType;
            action.mDescriptionInputType = this.mDescriptionInputType;
            action.mAutofillHints = this.mAutofillHints;
            action.mEditInputType = this.mEditInputType;
            action.mDescriptionEditInputType = this.mDescriptionEditInputType;
            action.mActionFlags = this.mActionFlags;
            action.mCheckSetId = this.mCheckSetId;
            action.mSubActions = this.mSubActions;
        }

        public B clickAction(long id) {
            if (id == -4) {
                this.mId = -4L;
                this.mTitle = this.mContext.getString(R.string.ok);
            } else if (id == -5) {
                this.mId = -5L;
                this.mTitle = this.mContext.getString(R.string.cancel);
            } else if (id == -6) {
                this.mId = -6L;
                this.mTitle = this.mContext.getString(android.support.v17.leanback.R.string.lb_guidedaction_finish_title);
            } else if (id == -7) {
                this.mId = -7L;
                this.mTitle = this.mContext.getString(android.support.v17.leanback.R.string.lb_guidedaction_continue_title);
            } else if (id == -8) {
                this.mId = -8L;
                this.mTitle = this.mContext.getString(R.string.ok);
            } else if (id == -9) {
                this.mId = -9L;
                this.mTitle = this.mContext.getString(R.string.cancel);
            }
            return this;
        }
    }

    public static class Builder extends BuilderBase<Builder> {
        @Deprecated
        public Builder() {
            super(null);
        }

        public Builder(Context context) {
            super(context);
        }

        public GuidedAction build() {
            GuidedAction action = new GuidedAction();
            applyValues(action);
            return action;
        }
    }

    protected GuidedAction() {
        super(0L);
    }

    private void setFlags(int flag, int mask) {
        this.mActionFlags = (this.mActionFlags & (~mask)) | (flag & mask);
    }

    public CharSequence getTitle() {
        return getLabel1();
    }

    public void setTitle(CharSequence title) {
        setLabel1(title);
    }

    public CharSequence getEditTitle() {
        return this.mEditTitle;
    }

    public void setEditTitle(CharSequence editTitle) {
        this.mEditTitle = editTitle;
    }

    public CharSequence getEditDescription() {
        return this.mEditDescription;
    }

    public void setEditDescription(CharSequence editDescription) {
        this.mEditDescription = editDescription;
    }

    public CharSequence getDescription() {
        return getLabel2();
    }

    public void setDescription(CharSequence description) {
        setLabel2(description);
    }

    public boolean isEditable() {
        return this.mEditable == 1;
    }

    public boolean isDescriptionEditable() {
        return this.mEditable == 2;
    }

    public boolean hasTextEditable() {
        return this.mEditable == 1 || this.mEditable == 2;
    }

    public boolean hasEditableActivatorView() {
        return this.mEditable == 3;
    }

    public int getEditInputType() {
        return this.mEditInputType;
    }

    public int getDescriptionEditInputType() {
        return this.mDescriptionEditInputType;
    }

    public int getInputType() {
        return this.mInputType;
    }

    public int getDescriptionInputType() {
        return this.mDescriptionInputType;
    }

    public boolean isChecked() {
        return (this.mActionFlags & 1) == 1;
    }

    public void setChecked(boolean z) {
        setFlags(z ? 1 : 0, 1);
    }

    public int getCheckSetId() {
        return this.mCheckSetId;
    }

    public boolean hasMultilineDescription() {
        return (this.mActionFlags & 2) == 2;
    }

    public boolean isEnabled() {
        return (this.mActionFlags & 16) == 16;
    }

    public boolean isFocusable() {
        return (this.mActionFlags & 32) == 32;
    }

    public String[] getAutofillHints() {
        return this.mAutofillHints;
    }

    public boolean hasNext() {
        return (this.mActionFlags & 4) == 4;
    }

    public boolean infoOnly() {
        return (this.mActionFlags & 8) == 8;
    }

    public List<GuidedAction> getSubActions() {
        return this.mSubActions;
    }

    public boolean hasSubActions() {
        return this.mSubActions != null;
    }

    public final boolean isAutoSaveRestoreEnabled() {
        return (this.mActionFlags & 64) == 64;
    }

    public void onSaveInstanceState(Bundle bundle, String key) {
        if (needAutoSaveTitle() && getTitle() != null) {
            bundle.putString(key, getTitle().toString());
            return;
        }
        if (needAutoSaveDescription() && getDescription() != null) {
            bundle.putString(key, getDescription().toString());
        } else if (getCheckSetId() != 0) {
            bundle.putBoolean(key, isChecked());
        }
    }

    public void onRestoreInstanceState(Bundle bundle, String key) {
        if (needAutoSaveTitle()) {
            String title = bundle.getString(key);
            if (title != null) {
                setTitle(title);
                return;
            }
            return;
        }
        if (!needAutoSaveDescription()) {
            if (getCheckSetId() != 0) {
                setChecked(bundle.getBoolean(key, isChecked()));
            }
        } else {
            String description = bundle.getString(key);
            if (description != null) {
                setDescription(description);
            }
        }
    }

    static boolean isPasswordVariant(int inputType) {
        int variation = inputType & 4080;
        return variation == 128 || variation == 144 || variation == 224;
    }

    final boolean needAutoSaveTitle() {
        return isEditable() && !isPasswordVariant(getEditInputType());
    }

    final boolean needAutoSaveDescription() {
        return isDescriptionEditable() && !isPasswordVariant(getDescriptionEditInputType());
    }
}
