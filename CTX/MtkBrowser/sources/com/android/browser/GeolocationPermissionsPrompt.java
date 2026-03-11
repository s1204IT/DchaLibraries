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

    public GeolocationPermissionsPrompt(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void handleButtonClick(boolean z) {
        hide();
        boolean zIsChecked = this.mRemember.isChecked();
        if (zIsChecked) {
            Toast toastMakeText = Toast.makeText(getContext(), z ? 2131493247 : 2131493248, 1);
            toastMakeText.setGravity(80, 0, 0);
            toastMakeText.show();
        }
        this.mCallback.invoke(this.mOrigin, z, zIsChecked);
    }

    private void init() {
        this.mMessage = (TextView) findViewById(2131558480);
        this.mShareButton = (Button) findViewById(2131558483);
        this.mDontShareButton = (Button) findViewById(2131558482);
        this.mRemember = (CheckBox) findViewById(2131558481);
        this.mShareButton.setOnClickListener(new View.OnClickListener(this) {
            final GeolocationPermissionsPrompt this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.handleButtonClick(true);
            }
        });
        this.mDontShareButton.setOnClickListener(new View.OnClickListener(this) {
            final GeolocationPermissionsPrompt this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.handleButtonClick(false);
            }
        });
    }

    private void setMessage(CharSequence charSequence) {
        this.mMessage.setText(String.format(getResources().getString(2131493243), charSequence));
    }

    public void hide() {
        setVisibility(8);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void show(String str, GeolocationPermissions.Callback callback) {
        this.mOrigin = str;
        this.mCallback = callback;
        setMessage("http".equals(Uri.parse(this.mOrigin).getScheme()) ? this.mOrigin.substring(7) : this.mOrigin);
        this.mRemember.setChecked(true);
        setVisibility(0);
    }
}
