package com.android.documentsui.dirlist;

import android.app.AuthenticationRequiredException;
import android.graphics.drawable.Drawable;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.Model;
import com.android.documentsui.R;
import com.android.documentsui.dirlist.DocumentsAdapter;

abstract class Message {
    private CharSequence mButtonString;
    protected Runnable mCallback;
    protected final Runnable mDefaultCallback;
    protected final DocumentsAdapter.Environment mEnv;
    private Drawable mIcon;
    private CharSequence mMessageString;
    private boolean mShouldShow = false;

    abstract void update(Model.Update update);

    Message(DocumentsAdapter.Environment environment, Runnable runnable) {
        this.mEnv = environment;
        this.mDefaultCallback = runnable;
    }

    protected void update(CharSequence charSequence, CharSequence charSequence2, Drawable drawable) {
        if (charSequence == null) {
            return;
        }
        this.mMessageString = charSequence;
        this.mButtonString = charSequence2;
        this.mIcon = drawable;
        this.mShouldShow = true;
    }

    void reset() {
        this.mMessageString = null;
        this.mIcon = null;
        this.mShouldShow = false;
    }

    void runCallback() {
        if (this.mCallback != null) {
            this.mCallback.run();
        } else {
            this.mDefaultCallback.run();
        }
    }

    Drawable getIcon() {
        return this.mIcon;
    }

    boolean shouldShow() {
        return this.mShouldShow;
    }

    CharSequence getMessageString() {
        return this.mMessageString;
    }

    CharSequence getButtonString() {
        return this.mButtonString;
    }

    static final class HeaderMessage extends Message {
        static final boolean $assertionsDisabled = false;

        HeaderMessage(DocumentsAdapter.Environment environment, Runnable runnable) {
            super(environment, runnable);
        }

        @Override
        void update(Model.Update update) {
            reset();
            if (update.hasAuthenticationException()) {
                updateToAuthenticationExceptionHeader(update);
            } else if (this.mEnv.getModel().error != null) {
                update(this.mEnv.getModel().error, null, this.mEnv.getContext().getDrawable(R.drawable.ic_dialog_alert));
            } else if (this.mEnv.getModel().info != null) {
                update(this.mEnv.getModel().info, null, this.mEnv.getContext().getDrawable(R.drawable.ic_dialog_info));
            }
        }

        private void updateToAuthenticationExceptionHeader(final Model.Update update) {
            update(this.mEnv.getContext().getString(R.string.authentication_required, DocumentsApplication.getProvidersCache(this.mEnv.getContext()).getApplicationName(this.mEnv.getDisplayState().stack.getRoot().authority)), this.mEnv.getContext().getResources().getText(R.string.sign_in), this.mEnv.getContext().getDrawable(R.drawable.ic_dialog_info));
            this.mCallback = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mEnv.getActionHandler().startAuthentication(((AuthenticationRequiredException) update.getException()).getUserAction());
                }
            };
        }
    }

    static final class InflateMessage extends Message {
        InflateMessage(DocumentsAdapter.Environment environment, Runnable runnable) {
            super(environment, runnable);
        }

        @Override
        void update(Model.Update update) {
            reset();
            if (update.hasException() && !update.hasAuthenticationException()) {
                updateToInflatedErrorMesage();
            } else if (update.hasAuthenticationException()) {
                updateToCantDisplayContentMessage();
            } else if (this.mEnv.getModel().getModelIds().length == 0) {
                updateToInflatedEmptyMessage();
            }
        }

        private void updateToInflatedErrorMesage() {
            update(this.mEnv.getContext().getResources().getText(R.string.query_error), null, this.mEnv.getContext().getDrawable(R.drawable.hourglass));
        }

        private void updateToCantDisplayContentMessage() {
            update(this.mEnv.getContext().getResources().getText(R.string.cant_display_content), null, this.mEnv.getContext().getDrawable(R.drawable.empty));
        }

        private void updateToInflatedEmptyMessage() {
            CharSequence text;
            if (this.mEnv.isInSearchMode()) {
                text = String.format(String.valueOf(this.mEnv.getContext().getResources().getText(R.string.no_results)), this.mEnv.getDisplayState().stack.getRoot().title);
            } else {
                text = this.mEnv.getContext().getResources().getText(R.string.empty);
            }
            update(text, null, this.mEnv.getContext().getDrawable(R.drawable.empty));
        }
    }
}
