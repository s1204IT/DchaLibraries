package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.server.input.InputManagerService;

final class UserSwitchingDialog extends AlertDialog implements ViewTreeObserver.OnWindowShownListener {
    private static final String TAG = "ActivityManagerUserSwitchingDialog";
    private final ActivityManagerService mService;
    private final int mUserId;

    public UserSwitchingDialog(ActivityManagerService service, Context context, int userId, String userName, boolean aboveSystem) {
        super(context);
        this.mService = service;
        this.mUserId = userId;
        setCancelable(false);
        Resources res = getContext().getResources();
        View view = LayoutInflater.from(getContext()).inflate(R.layout.notification_template_material_conversation, (ViewGroup) null);
        ((TextView) view.findViewById(R.id.message)).setText(res.getString(R.string.mediasize_iso_c6, userName));
        setView(view);
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
    }

    @Override
    public void show() {
        super.show();
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            decorView.getViewTreeObserver().addOnWindowShownListener(this);
        }
    }

    public void onWindowShown() {
        this.mService.startUserInForeground(this.mUserId, this);
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            decorView.getViewTreeObserver().removeOnWindowShownListener(this);
        }
    }
}
