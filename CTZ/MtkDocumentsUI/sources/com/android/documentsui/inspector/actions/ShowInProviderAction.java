package com.android.documentsui.inspector.actions;

import android.content.Context;
import android.content.pm.PackageManager;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.roots.ProvidersAccess;

public final class ShowInProviderAction extends Action {
    static final boolean $assertionsDisabled = false;
    private ProvidersAccess mProviders;

    public ShowInProviderAction(Context context, PackageManager packageManager, DocumentInfo documentInfo, ProvidersAccess providersAccess) {
        super(context, packageManager, documentInfo);
        this.mProviders = providersAccess;
    }

    @Override
    public String getHeader() {
        return this.mContext.getString(R.string.handler_app_belongs_to);
    }

    @Override
    public int getButtonIcon() {
        return R.drawable.ic_action_open;
    }

    @Override
    public boolean canPerformAction() {
        if ((this.mDoc.flags & 2048) != 0) {
            return true;
        }
        return false;
    }

    @Override
    public String getPackageName() {
        return this.mProviders.getPackageName(this.mDoc.derivedUri.getAuthority());
    }

    @Override
    public int getButtonLabel() {
        return R.string.button_show_provider;
    }
}
