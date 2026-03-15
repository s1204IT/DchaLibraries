package com.android.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.Enumeration;
import java.util.Vector;

public class PermissionsPrompt extends RelativeLayout {
    private Button mAllowButton;
    private Button mDenyButton;
    private TextView mMessage;
    private CheckBox mRemember;
    private PermissionRequest mRequest;

    public PermissionsPrompt(Context context) {
        this(context, null);
    }

    public PermissionsPrompt(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void handleButtonClick(boolean z) {
        hide();
        if (z) {
            this.mRequest.grant(this.mRequest.getResources());
        } else {
            this.mRequest.deny();
        }
    }

    private void init() {
        this.mMessage = (TextView) findViewById(2131558480);
        this.mAllowButton = (Button) findViewById(2131558502);
        this.mDenyButton = (Button) findViewById(2131558501);
        this.mRemember = (CheckBox) findViewById(2131558481);
        this.mAllowButton.setOnClickListener(new View.OnClickListener(this) {
            final PermissionsPrompt this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.handleButtonClick(true);
            }
        });
        this.mDenyButton.setOnClickListener(new View.OnClickListener(this) {
            final PermissionsPrompt this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                this.this$0.handleButtonClick(false);
            }
        });
    }

    public void hide() {
        setVisibility(8);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void setMessage() {
        String[] resources = this.mRequest.getResources();
        Vector vector = new Vector();
        for (String str : resources) {
            if (str.equals("android.webkit.resource.VIDEO_CAPTURE")) {
                vector.add(getResources().getString(2131493241));
            } else if (str.equals("android.webkit.resource.AUDIO_CAPTURE")) {
                vector.add(getResources().getString(2131493242));
            } else if (str.equals("android.webkit.resource.PROTECTED_MEDIA_ID")) {
                vector.add(getResources().getString(2131493240));
            }
        }
        if (vector.isEmpty()) {
            return;
        }
        Enumeration enumerationElements = vector.elements();
        StringBuilder sb = new StringBuilder((String) enumerationElements.nextElement());
        if (enumerationElements.hasMoreElements()) {
            sb.append(", ");
            sb.append((String) enumerationElements.nextElement());
        }
        this.mMessage.setText(String.format(getResources().getString(2131493236), this.mRequest.getOrigin(), sb.toString()));
    }

    public void show(PermissionRequest permissionRequest) {
        this.mRequest = permissionRequest;
        setMessage();
        this.mRemember.setChecked(true);
        setVisibility(0);
    }
}
