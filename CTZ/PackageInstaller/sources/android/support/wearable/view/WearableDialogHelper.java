package android.support.wearable.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Button;

public class WearableDialogHelper {
    private Drawable mNegativeIcon;
    private int mNegativeIconId;
    private Drawable mNeutralIcon;
    private int mNeutralIconId;
    private Drawable mPositiveIcon;
    private int mPositiveIconId;
    Resources mResources;
    Resources.Theme mTheme;

    public WearableDialogHelper(Resources resources, Resources.Theme theme) {
        this.mResources = resources;
        this.mTheme = theme;
    }

    public Drawable getPositiveIcon() {
        return resolveDrawable(this.mPositiveIcon, this.mPositiveIconId);
    }

    public Drawable getNegativeIcon() {
        return resolveDrawable(this.mNegativeIcon, this.mNegativeIconId);
    }

    public Drawable getNeutralIcon() {
        return resolveDrawable(this.mNeutralIcon, this.mNeutralIconId);
    }

    public WearableDialogHelper setPositiveIcon(int i) {
        this.mPositiveIconId = i;
        this.mPositiveIcon = null;
        return this;
    }

    public WearableDialogHelper setNegativeIcon(int i) {
        this.mNegativeIconId = i;
        this.mNegativeIcon = null;
        return this;
    }

    public WearableDialogHelper setNeutralIcon(int i) {
        this.mNeutralIconId = i;
        this.mNeutralIcon = null;
        return this;
    }

    public void apply(AlertDialog alertDialog) {
        applyButton(alertDialog.getButton(-1), getPositiveIcon());
        applyButton(alertDialog.getButton(-2), getNegativeIcon());
        applyButton(alertDialog.getButton(-3), getNeutralIcon());
    }

    void applyButton(Button button, Drawable drawable) {
        if (button != null) {
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
            button.setAllCaps(false);
        } else if (drawable != null) {
            Log.w("WearableDialogHelper", "non-null drawable used with missing button, did you call AlertDialog.create()?");
        }
    }

    Drawable resolveDrawable(Drawable drawable, int i) {
        return (drawable != null || i == 0) ? drawable : this.mResources.getDrawable(i, this.mTheme);
    }

    public static class DialogBuilder extends AlertDialog.Builder {
        private final WearableDialogHelper mHelper;

        public DialogBuilder(Context context) {
            super(context);
            this.mHelper = new WearableDialogHelper(context.getResources(), context.getTheme());
        }

        public DialogBuilder setPositiveIcon(int i) {
            this.mHelper.setPositiveIcon(i);
            return this;
        }

        public DialogBuilder setNegativeIcon(int i) {
            this.mHelper.setNegativeIcon(i);
            return this;
        }

        public DialogBuilder setNeutralIcon(int i) {
            this.mHelper.setNeutralIcon(i);
            return this;
        }

        @Override
        public AlertDialog create() {
            AlertDialog alertDialogCreate = super.create();
            alertDialogCreate.create();
            this.mHelper.apply(alertDialogCreate);
            return alertDialogCreate;
        }

        @Override
        public AlertDialog show() {
            AlertDialog alertDialogCreate = create();
            alertDialogCreate.show();
            return alertDialogCreate;
        }
    }
}
