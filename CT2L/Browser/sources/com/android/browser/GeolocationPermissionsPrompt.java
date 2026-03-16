package com.android.browser;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    public GeolocationPermissionsPrompt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        this.mMessage = (TextView) findViewById(R.id.message);
        this.mShareButton = (Button) findViewById(R.id.share_button);
        this.mDontShareButton = (Button) findViewById(R.id.dont_share_button);
        this.mRemember = (CheckBox) findViewById(R.id.remember);
        this.mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeolocationPermissionsPrompt.this.handleButtonClick(true);
            }
        });
        this.mDontShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeolocationPermissionsPrompt.this.handleButtonClick(false);
            }
        });
    }

    public void show(String origin, GeolocationPermissions.Callback callback) {
        this.mOrigin = origin;
        this.mCallback = callback;
        Uri uri = Uri.parse(this.mOrigin);
        setMessage("http".equals(uri.getScheme()) ? this.mOrigin.substring(7) : this.mOrigin);
        this.mRemember.setChecked(true);
        setVisibility(0);
    }

    public void hide() {
        setVisibility(8);
    }

    private void handleButtonClick(boolean allow) {
        hide();
        boolean remember = this.mRemember.isChecked();
        if (remember) {
            Toast toast = Toast.makeText(getContext(), allow ? R.string.geolocation_permissions_prompt_toast_allowed : R.string.geolocation_permissions_prompt_toast_disallowed, 1);
            toast.setGravity(80, 0, 0);
            toast.show();
        }
        this.mCallback.invoke(this.mOrigin, allow, remember);
    }

    private void setMessage(CharSequence origin) {
        this.mMessage.setText(String.format(getResources().getString(R.string.geolocation_permissions_prompt_message), origin));
    }
}
