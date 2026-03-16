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

    public PermissionsPrompt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        this.mMessage = (TextView) findViewById(R.id.message);
        this.mAllowButton = (Button) findViewById(R.id.allow_button);
        this.mDenyButton = (Button) findViewById(R.id.deny_button);
        this.mRemember = (CheckBox) findViewById(R.id.remember);
        this.mAllowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionsPrompt.this.handleButtonClick(true);
            }
        });
        this.mDenyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionsPrompt.this.handleButtonClick(false);
            }
        });
    }

    public void show(PermissionRequest request) {
        this.mRequest = request;
        setMessage();
        this.mRemember.setChecked(true);
        setVisibility(0);
    }

    public void setMessage() {
        String[] resources = this.mRequest.getResources();
        Vector<String> strings = new Vector<>();
        for (String resource : resources) {
            if (resource.equals("android.webkit.resource.VIDEO_CAPTURE")) {
                strings.add(getResources().getString(R.string.resource_video_capture));
            } else if (resource.equals("android.webkit.resource.AUDIO_CAPTURE")) {
                strings.add(getResources().getString(R.string.resource_audio_capture));
            } else if (resource.equals("android.webkit.resource.PROTECTED_MEDIA_ID")) {
                strings.add(getResources().getString(R.string.resource_protected_media_id));
            }
        }
        if (!strings.isEmpty()) {
            Enumeration<String> e = strings.elements();
            StringBuilder sb = new StringBuilder(e.nextElement());
            if (e.hasMoreElements()) {
                sb.append(", ");
                sb.append(e.nextElement());
            }
            this.mMessage.setText(String.format(getResources().getString(R.string.permissions_prompt_message), this.mRequest.getOrigin(), sb.toString()));
        }
    }

    public void hide() {
        setVisibility(8);
    }

    private void handleButtonClick(boolean allow) {
        hide();
        if (allow) {
            this.mRequest.grant(this.mRequest.getResources());
        } else {
            this.mRequest.deny();
        }
    }
}
