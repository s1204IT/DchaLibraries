package com.android.browser;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.webkit.ClientCertRequest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

final class KeyChainLookup extends AsyncTask<Void, Void, Void> {
    private final String mAlias;
    private final Context mContext;
    private final ClientCertRequest mHandler;

    KeyChainLookup(Context context, ClientCertRequest handler, String alias) {
        this.mContext = context.getApplicationContext();
        this.mHandler = handler;
        this.mAlias = alias;
    }

    @Override
    public Void doInBackground(Void... params) {
        try {
            PrivateKey privateKey = KeyChain.getPrivateKey(this.mContext, this.mAlias);
            X509Certificate[] certificateChain = KeyChain.getCertificateChain(this.mContext, this.mAlias);
            this.mHandler.proceed(privateKey, certificateChain);
            return null;
        } catch (KeyChainException e) {
            this.mHandler.ignore();
            return null;
        } catch (InterruptedException e2) {
            this.mHandler.ignore();
            return null;
        }
    }
}
