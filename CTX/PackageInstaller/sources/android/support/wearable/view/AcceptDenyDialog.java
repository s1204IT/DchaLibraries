package android.support.wearable.view;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;
import com.android.packageinstaller.R;

@TargetApi(21)
public class AcceptDenyDialog extends Dialog {
    private final View.OnClickListener mButtonHandler;
    protected View mButtonPanel;
    protected ImageView mIcon;
    protected TextView mMessage;
    protected ImageButton mNegativeButton;
    protected DialogInterface.OnClickListener mNegativeButtonListener;
    protected ImageButton mPositiveButton;
    protected DialogInterface.OnClickListener mPositiveButtonListener;
    protected View mSpacer;
    protected TextView mTitle;

    public static void lambda$new$0(AcceptDenyDialog acceptDenyDialog, View view) {
        if (view == acceptDenyDialog.mPositiveButton && acceptDenyDialog.mPositiveButtonListener != null) {
            acceptDenyDialog.mPositiveButtonListener.onClick(acceptDenyDialog, -1);
            acceptDenyDialog.dismiss();
        } else if (view == acceptDenyDialog.mNegativeButton && acceptDenyDialog.mNegativeButtonListener != null) {
            acceptDenyDialog.mNegativeButtonListener.onClick(acceptDenyDialog, -2);
            acceptDenyDialog.dismiss();
        }
    }

    public AcceptDenyDialog(Context context) {
        this(context, 0);
    }

    public AcceptDenyDialog(Context context, int i) {
        super(context, i);
        this.mButtonHandler = new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AcceptDenyDialog.lambda$new$0(this.f$0, view);
            }
        };
        setContentView(R.layout.accept_deny_dialog);
        this.mTitle = (TextView) findViewById(android.R.id.title);
        this.mMessage = (TextView) findViewById(android.R.id.message);
        this.mIcon = (ImageView) findViewById(android.R.id.icon);
        this.mPositiveButton = (ImageButton) findViewById(android.R.id.button1);
        this.mPositiveButton.setOnClickListener(this.mButtonHandler);
        this.mNegativeButton = (ImageButton) findViewById(android.R.id.button2);
        this.mNegativeButton.setOnClickListener(this.mButtonHandler);
        this.mSpacer = (Space) findViewById(R.id.spacer);
        this.mButtonPanel = findViewById(R.id.buttonPanel);
    }

    public ImageButton getButton(int i) {
        switch (i) {
            case -2:
                return this.mNegativeButton;
            case -1:
                return this.mPositiveButton;
            default:
                return null;
        }
    }

    public void setIcon(Drawable drawable) {
        this.mIcon.setVisibility(drawable == null ? 8 : 0);
        this.mIcon.setImageDrawable(drawable);
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mTitle.setText(charSequence);
    }

    public void setButton(int i, DialogInterface.OnClickListener onClickListener) {
        int i2;
        switch (i) {
            case -2:
                this.mNegativeButtonListener = onClickListener;
                break;
            case -1:
                this.mPositiveButtonListener = onClickListener;
                break;
            default:
                return;
        }
        View view = this.mSpacer;
        if (this.mPositiveButtonListener == null || this.mNegativeButtonListener == null) {
            i2 = 8;
        } else {
            i2 = 4;
        }
        view.setVisibility(i2);
        this.mPositiveButton.setVisibility(this.mPositiveButtonListener == null ? 8 : 0);
        this.mNegativeButton.setVisibility(this.mNegativeButtonListener == null ? 8 : 0);
        this.mButtonPanel.setVisibility((this.mPositiveButtonListener == null && this.mNegativeButtonListener == null) ? 8 : 0);
    }

    public void setPositiveButton(DialogInterface.OnClickListener onClickListener) {
        setButton(-1, onClickListener);
    }

    public void setNegativeButton(DialogInterface.OnClickListener onClickListener) {
        setButton(-2, onClickListener);
    }
}
