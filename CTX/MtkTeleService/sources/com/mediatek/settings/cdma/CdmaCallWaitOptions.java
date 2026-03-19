package com.mediatek.settings.cdma;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

public class CdmaCallWaitOptions {
    private static final String[] CW_HEADERS = {"*74", "*740"};
    private Context mContext;
    private Phone mPhone;

    public CdmaCallWaitOptions(Context context, Phone phone) {
        this.mContext = context;
        this.mPhone = phone;
    }

    public Dialog createDialog() {
        final Dialog dialog = new Dialog(this.mContext, R.style.CWDialogTheme);
        dialog.setContentView(R.layout.mtk_cdma_cf_dialog);
        dialog.setTitle(R.string.labelCW);
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.group);
        TextView textView = (TextView) dialog.findViewById(R.id.dialog_sum);
        if (textView != null) {
            textView.setVisibility(8);
        } else {
            Log.d("Settings/CdmaCallWaitOptions", "--------------[text view is null]---------------");
        }
        EditText editText = (EditText) dialog.findViewById(R.id.EditNumber);
        if (editText != null) {
            editText.setVisibility(8);
        }
        ImageButton imageButton = (ImageButton) dialog.findViewById(R.id.select_contact);
        if (imageButton != null) {
            imageButton.setVisibility(8);
        }
        Button button = (Button) dialog.findViewById(R.id.save);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (radioGroup.getCheckedRadioButtonId() != -1) {
                        String str = CdmaCallWaitOptions.CW_HEADERS[radioGroup.getCheckedRadioButtonId() == R.id.enable ? (char) 0 : (char) 1];
                        dialog.dismiss();
                        CdmaCallWaitOptions.this.setCallWait(str);
                        return;
                    }
                    dialog.dismiss();
                }
            });
        }
        Button button2 = (Button) dialog.findViewById(R.id.cancel);
        if (button2 != null) {
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }
        return dialog;
    }

    private void setCallWait(String str) {
        Log.d("Settings/CdmaCallWaitOptions", "[setCallWait][cw = " + str + "], mPhone = " + this.mPhone);
        if (this.mPhone == null || str == null || str.isEmpty()) {
            return;
        }
        Intent intent = new Intent("android.intent.action.CALL");
        intent.setData(Uri.parse("tel:" + str));
        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", PhoneUtils.makePstnPhoneAccountHandle(this.mPhone));
        this.mContext.startActivity(intent);
    }
}
