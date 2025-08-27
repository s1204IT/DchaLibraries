package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.storage.StorageManager;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class AccessibilityServiceWarning {
    public static Dialog createCapabilitiesDialog(Activity activity, AccessibilityServiceInfo accessibilityServiceInfo, DialogInterface.OnClickListener onClickListener) {
        AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.enable_service_title, new Object[]{getServiceName(activity, accessibilityServiceInfo)})).setView(createEnableDialogContentView(activity, accessibilityServiceInfo)).setPositiveButton(android.R.string.ok, onClickListener).setNegativeButton(android.R.string.cancel, onClickListener).create();
        $$Lambda$AccessibilityServiceWarning$D3xqJyTKInilYjQAxG1fpVU1D1M __lambda_accessibilityservicewarning_d3xqjytkinilyjqaxg1fpvu1d1m = new View.OnTouchListener() { // from class: com.android.settings.accessibility.-$$Lambda$AccessibilityServiceWarning$D3xqJyTKInilYjQAxG1fpVU1D1M
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return AccessibilityServiceWarning.lambda$createCapabilitiesDialog$0(view, motionEvent);
            }
        };
        Window window = alertDialogCreate.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.privateFlags |= 524288;
        window.setAttributes(attributes);
        alertDialogCreate.create();
        alertDialogCreate.getButton(-1).setOnTouchListener(__lambda_accessibilityservicewarning_d3xqjytkinilyjqaxg1fpvu1d1m);
        alertDialogCreate.setCanceledOnTouchOutside(true);
        return alertDialogCreate;
    }

    static /* synthetic */ boolean lambda$createCapabilitiesDialog$0(View view, MotionEvent motionEvent) {
        if ((motionEvent.getFlags() & 1) == 0 && (motionEvent.getFlags() & 2) == 0) {
            return false;
        }
        if (motionEvent.getAction() == 1) {
            Toast.makeText(view.getContext(), R.string.touch_filtered_warning, 0).show();
        }
        return true;
    }

    private static boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    private static View createEnableDialogContentView(Context context, AccessibilityServiceInfo accessibilityServiceInfo) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        ViewGroup viewGroup = null;
        View viewInflate = layoutInflater.inflate(R.layout.enable_accessibility_service_dialog_content, (ViewGroup) null);
        TextView textView = (TextView) viewInflate.findViewById(R.id.encryption_warning);
        int i = 0;
        if (isFullDiskEncrypted()) {
            textView.setText(context.getString(R.string.enable_service_encryption_warning, getServiceName(context, accessibilityServiceInfo)));
            textView.setVisibility(0);
        } else {
            textView.setVisibility(8);
        }
        ((TextView) viewInflate.findViewById(R.id.capabilities_header)).setText(context.getString(R.string.capabilities_list_title, getServiceName(context, accessibilityServiceInfo)));
        LinearLayout linearLayout = (LinearLayout) viewInflate.findViewById(R.id.capabilities);
        View viewInflate2 = layoutInflater.inflate(android.R.layout.app_error_dialog, (ViewGroup) null);
        ((ImageView) viewInflate2.findViewById(android.R.id.instant)).setImageDrawable(context.getDrawable(android.R.drawable.ic_media_route_dark_material));
        ((TextView) viewInflate2.findViewById(android.R.id.inter_word)).setText(context.getString(R.string.capability_title_receiveAccessibilityEvents));
        ((TextView) viewInflate2.findViewById(android.R.id.internalEmpty)).setText(context.getString(R.string.capability_desc_receiveAccessibilityEvents));
        List capabilityInfos = accessibilityServiceInfo.getCapabilityInfos(context);
        linearLayout.addView(viewInflate2);
        int size = capabilityInfos.size();
        while (i < size) {
            AccessibilityServiceInfo.CapabilityInfo capabilityInfo = (AccessibilityServiceInfo.CapabilityInfo) capabilityInfos.get(i);
            View viewInflate3 = layoutInflater.inflate(android.R.layout.app_error_dialog, viewGroup);
            ((ImageView) viewInflate3.findViewById(android.R.id.instant)).setImageDrawable(context.getDrawable(android.R.drawable.ic_media_route_dark_material));
            ((TextView) viewInflate3.findViewById(android.R.id.inter_word)).setText(context.getString(capabilityInfo.titleResId));
            ((TextView) viewInflate3.findViewById(android.R.id.internalEmpty)).setText(context.getString(capabilityInfo.descResId));
            linearLayout.addView(viewInflate3);
            i++;
            viewGroup = null;
        }
        return viewInflate;
    }

    private static CharSequence getServiceName(Context context, AccessibilityServiceInfo accessibilityServiceInfo) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return BidiFormatter.getInstance(locale).unicodeWrap(accessibilityServiceInfo.getResolveInfo().loadLabel(context.getPackageManager()));
    }
}
