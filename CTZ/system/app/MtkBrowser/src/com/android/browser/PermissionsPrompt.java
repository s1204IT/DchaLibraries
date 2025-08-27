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

/* loaded from: classes.dex */
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

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        this.mMessage = (TextView) findViewById(R.id.message);
        this.mAllowButton = (Button) findViewById(R.id.allow_button);
        this.mDenyButton = (Button) findViewById(R.id.deny_button);
        this.mRemember = (CheckBox) findViewById(R.id.remember);
        this.mAllowButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.PermissionsPrompt.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PermissionsPrompt.this.handleButtonClick(true);
            }
        });
        this.mDenyButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.browser.PermissionsPrompt.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PermissionsPrompt.this.handleButtonClick(false);
            }
        });
    }

    public void show(PermissionRequest permissionRequest) {
        this.mRequest = permissionRequest;
        setMessage();
        this.mRemember.setChecked(true);
        setVisibility(0);
    }

    public void setMessage() {
        String[] resources = this.mRequest.getResources();
        Vector vector = new Vector();
        for (String str : resources) {
            if (str.equals("android.webkit.resource.VIDEO_CAPTURE")) {
                vector.add(getResources().getString(R.string.resource_video_capture));
            } else if (str.equals("android.webkit.resource.AUDIO_CAPTURE")) {
                vector.add(getResources().getString(R.string.resource_audio_capture));
            } else if (str.equals("android.webkit.resource.PROTECTED_MEDIA_ID")) {
                vector.add(getResources().getString(R.string.resource_protected_media_id));
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
        this.mMessage.setText(String.format(getResources().getString(R.string.permissions_prompt_message), this.mRequest.getOrigin(), sb.toString()));
    }

    public void hide() {
        setVisibility(8);
    }

    private void handleButtonClick(boolean z) {
        hide();
        if (z) {
            this.mRequest.grant(this.mRequest.getResources());
        } else {
            this.mRequest.deny();
        }
    }
}
