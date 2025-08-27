package com.android.browser;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/* loaded from: classes.dex */
public class GeolocationPermissionsPrompt extends RelativeLayout {
    private GeolocationPermissions.Callback mCallback;
    private Button mDontShareButton;
    private TextView mMessage;
    private String mOrigin;
    private CheckBox mRemember;
    private Button mShareButton;

    public GeolocationPermissionsPrompt(Context context) {
        this(context, null);
    }

    public GeolocationPermissionsPrompt(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        this.mMessage = (TextView) findViewById(R.id.message);
        this.mShareButton = (Button) findViewById(R.id.share_button);
        this.mDontShareButton = (Button) findViewById(R.id.dont_share_button);
        this.mRemember = (CheckBox) findViewById(R.id.remember);
        this.mShareButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.GeolocationPermissionsPrompt.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) throws Resources.NotFoundException {
                GeolocationPermissionsPrompt.this.handleButtonClick(true);
            }
        });
        this.mDontShareButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.GeolocationPermissionsPrompt.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) throws Resources.NotFoundException {
                GeolocationPermissionsPrompt.this.handleButtonClick(false);
            }
        });
    }

    public void show(String str, GeolocationPermissions.Callback callback) {
        this.mOrigin = str;
        this.mCallback = callback;
        setMessage("http".equals(Uri.parse(this.mOrigin).getScheme()) ? this.mOrigin.substring(7) : this.mOrigin);
        this.mRemember.setChecked(true);
        setVisibility(0);
    }

    public void hide() {
        setVisibility(8);
    }

    private void handleButtonClick(boolean z) throws Resources.NotFoundException {
        hide();
        boolean zIsChecked = this.mRemember.isChecked();
        if (zIsChecked) {
            Toast toastMakeText = Toast.makeText(getContext(), z ? R.string.geolocation_permissions_prompt_toast_allowed : R.string.geolocation_permissions_prompt_toast_disallowed, 1);
            toastMakeText.setGravity(80, 0, 0);
            toastMakeText.show();
        }
        this.mCallback.invoke(this.mOrigin, z, zIsChecked);
    }

    private void setMessage(CharSequence charSequence) {
        this.mMessage.setText(String.format(getResources().getString(R.string.geolocation_permissions_prompt_message), charSequence));
    }
}
