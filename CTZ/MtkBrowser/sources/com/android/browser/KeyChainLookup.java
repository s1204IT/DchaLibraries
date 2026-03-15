package com.android.browser;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.webkit.ClientCertRequest;

final class KeyChainLookup extends AsyncTask<Void, Void, Void> {
    private final String mAlias;
    private final Context mContext;
    private final ClientCertRequest mHandler;

    KeyChainLookup(Context context, ClientCertRequest clientCertRequest, String str) {
        this.mContext = context.getApplicationContext();
        this.mHandler = clientCertRequest;
        this.mAlias = str;
    }

    @Override
    protected Void doInBackground(Void... voidArr) {
        try {
            this.mHandler.proceed(KeyChain.getPrivateKey(this.mContext, this.mAlias), KeyChain.getCertificateChain(this.mContext, this.mAlias));
        } catch (KeyChainException e) {
            this.mHandler.ignore();
        } catch (InterruptedException e2) {
            this.mHandler.ignore();
        }
        return null;
    }
}
