package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.os.Bundle;
import com.mediatek.galleryportable.Log;
import com.mediatek.omadrm.OmaDrmUtils;

public class CTAHooker extends MovieHooker {
    private static final String CTA_ACTION = "com.mediatek.dataprotection.ACTION_VIEW_LOCKED_FILE";
    private static final String TAG = "VP_CTAHooker";
    private Activity mActivity;
    private DrmManagerClient mDrmClient;
    private boolean mIsCtaPlayback;
    private String mToken;
    private String mTokenKey;

    @Override
    public void onCreate(Bundle bundle) {
        this.mActivity = getContext();
        checkIntentAndToken();
    }

    @Override
    public void onPause() {
        finishPlayIfNeed();
    }

    public void checkIntentAndToken() {
        this.mDrmClient = new DrmManagerClient(this.mActivity.getApplicationContext());
        Intent intent = this.mActivity.getIntent();
        String action = intent.getAction();
        Log.d(TAG, "checkIntentAndToken action = " + action);
        if (CTA_ACTION.equals(action)) {
            this.mToken = intent.getStringExtra("TOKEN");
            this.mTokenKey = intent.getStringExtra("TOKEN_KEY");
            if (this.mToken == null || !OmaDrmUtils.isTokenValid(this.mDrmClient, this.mTokenKey, this.mToken)) {
                this.mDrmClient.release();
                this.mDrmClient = null;
                this.mActivity.finish();
                return;
            }
            this.mIsCtaPlayback = true;
        }
    }

    public void finishPlayIfNeed() {
        Log.d(TAG, "finishPlayIfNeed mIsCtaPlayback = " + this.mIsCtaPlayback);
        if (this.mIsCtaPlayback) {
            OmaDrmUtils.clearToken(this.mDrmClient, this.mTokenKey, this.mToken);
            this.mTokenKey = null;
            this.mToken = null;
            this.mIsCtaPlayback = false;
            this.mDrmClient.release();
            this.mDrmClient = null;
            this.mActivity.finish();
            return;
        }
        if (this.mDrmClient != null) {
            this.mDrmClient.release();
            this.mDrmClient = null;
        }
    }
}
