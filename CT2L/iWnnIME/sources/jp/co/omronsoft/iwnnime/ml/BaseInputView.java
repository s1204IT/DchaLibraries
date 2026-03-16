package jp.co.omronsoft.iwnnime.ml;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class BaseInputView extends FrameLayout {
    private DialogInterface.OnDismissListener mOnDismissListener;
    private ViewTreeObserver.OnWindowFocusChangeListener mOnWindowFocusChangeListener;
    private AlertDialog mOptionsDialog;

    public BaseInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOptionsDialog = null;
        this.mOnWindowFocusChangeListener = new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                if (!hasFocus) {
                    BaseInputView.this.closeDialog();
                }
            }
        };
        this.mOnDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ViewTreeObserver viewTreeObserver;
                if ((dialog instanceof AlertDialog) && (viewTreeObserver = BaseInputView.this.getViewTreeObserver((AlertDialog) dialog)) != null) {
                    viewTreeObserver.removeOnWindowFocusChangeListener(BaseInputView.this.mOnWindowFocusChangeListener);
                }
            }
        };
    }

    BaseInputView(Context context) {
        super(context);
        this.mOptionsDialog = null;
        this.mOnWindowFocusChangeListener = new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                if (!hasFocus) {
                    BaseInputView.this.closeDialog();
                }
            }
        };
        this.mOnDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ViewTreeObserver viewTreeObserver;
                if ((dialog instanceof AlertDialog) && (viewTreeObserver = BaseInputView.this.getViewTreeObserver((AlertDialog) dialog)) != null) {
                    viewTreeObserver.removeOnWindowFocusChangeListener(BaseInputView.this.mOnWindowFocusChangeListener);
                }
            }
        };
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            closeDialog();
        }
    }

    public void showDialog(AlertDialog.Builder builder) {
        closeDialog();
        this.mOptionsDialog = builder.create();
        Window window = this.mOptionsDialog.getWindow();
        WindowManager.LayoutParams dialogLayoutParams = window.getAttributes();
        dialogLayoutParams.type = 1003;
        dialogLayoutParams.token = getWindowToken();
        window.setAttributes(dialogLayoutParams);
        WnnUtility.addFlagsForDialog(IWnnIME.getCurrentIme(), window);
        this.mOptionsDialog.setOnDismissListener(this.mOnDismissListener);
        this.mOptionsDialog.show();
        ViewTreeObserver viewTreeObserver = getViewTreeObserver(this.mOptionsDialog);
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnWindowFocusChangeListener(this.mOnWindowFocusChangeListener);
        }
    }

    public void closeDialog() {
        if (this.mOptionsDialog != null) {
            this.mOptionsDialog.dismiss();
            this.mOptionsDialog = null;
        }
    }

    private ViewTreeObserver getViewTreeObserver(AlertDialog dialog) {
        if (dialog == null) {
            return null;
        }
        View dialogView = dialog.getListView();
        if (dialogView == null) {
            dialogView = dialog.findViewById(android.R.id.message);
        }
        if (dialogView == null) {
            return null;
        }
        ViewTreeObserver ret = dialogView.getViewTreeObserver();
        return ret;
    }

    public boolean isDialogShowing() {
        if (this.mOptionsDialog == null) {
            return false;
        }
        boolean ret = this.mOptionsDialog.isShowing();
        return ret;
    }
}
