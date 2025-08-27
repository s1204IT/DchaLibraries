package com.android.settings.sim;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.SimHotSwapHandler;

/* loaded from: classes.dex */
public class SimPreferenceDialog extends Activity {
    private final String SIM_NAME = "sim_name";
    private final String TINT_POS = "tint_pos";
    AlertDialog.Builder mBuilder;
    private String[] mColorStrings;
    private Context mContext;
    private Dialog mDialog;
    View mDialogLayout;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private int mSlotId;
    private SubscriptionInfo mSubInfoRecord;
    private SubscriptionManager mSubscriptionManager;
    private int[] mTintArr;
    private int mTintSelectorPos;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = this;
        this.mSlotId = getIntent().getExtras().getInt("slot_id", -1);
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mSubInfoRecord = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(this.mSlotId);
        if (this.mSubInfoRecord == null) {
            Log.w("SimPreferenceDialog", "Sub info record is null, finish the activity.");
            finish();
            return;
        }
        this.mTintArr = this.mContext.getResources().getIntArray(R.array.config_ephemeralResolverPackage);
        this.mColorStrings = this.mContext.getResources().getStringArray(com.android.settings.R.array.color_picker);
        this.mTintSelectorPos = 0;
        this.mBuilder = new AlertDialog.Builder(this.mContext);
        this.mDialogLayout = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(com.android.settings.R.layout.multi_sim_dialog, (ViewGroup) null);
        this.mBuilder.setView(this.mDialogLayout);
        this.mSimManagementExt = UtilsExt.getSimManagementExt(getApplicationContext());
        createEditDialog(bundle);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() { // from class: com.android.settings.sim.SimPreferenceDialog.1
            @Override // com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener
            public void onSimHotSwap() {
                Log.d("SimPreferenceDialog", "onSimHotSwap, finish Activity.");
                SimPreferenceDialog.this.finish();
            }
        });
    }

    @Override // android.app.Activity
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("tint_pos", this.mTintSelectorPos);
        bundle.putString("sim_name", ((EditText) this.mDialogLayout.findViewById(com.android.settings.R.id.sim_name)).getText().toString());
        super.onSaveInstanceState(bundle);
    }

    @Override // android.app.Activity
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        int i = bundle.getInt("tint_pos");
        ((Spinner) this.mDialogLayout.findViewById(com.android.settings.R.id.spinner)).setSelection(i);
        this.mTintSelectorPos = i;
        EditText editText = (EditText) this.mDialogLayout.findViewById(com.android.settings.R.id.sim_name);
        editText.setText(bundle.getString("sim_name"));
        Utils.setEditTextCursorPosition(editText);
    }

    private void createEditDialog(Bundle bundle) {
        Resources resources = this.mContext.getResources();
        EditText editText = (EditText) this.mDialogLayout.findViewById(com.android.settings.R.id.sim_name);
        editText.setText(this.mSubInfoRecord.getDisplayName());
        Utils.setEditTextCursorPosition(editText);
        final Spinner spinner = (Spinner) this.mDialogLayout.findViewById(com.android.settings.R.id.spinner);
        SelectColorAdapter selectColorAdapter = new SelectColorAdapter(this.mContext, com.android.settings.R.layout.settings_color_picker_item, this.mColorStrings);
        selectColorAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) selectColorAdapter);
        int i = 0;
        while (true) {
            if (i >= this.mTintArr.length) {
                break;
            }
            if (this.mTintArr[i] != this.mSubInfoRecord.getIconTint()) {
                i++;
            } else {
                spinner.setSelection(i);
                this.mTintSelectorPos = i;
                break;
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.android.settings.sim.SimPreferenceDialog.2
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> adapterView, View view, int i2, long j) {
                spinner.setSelection(i2);
                SimPreferenceDialog.this.mTintSelectorPos = i2;
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        TextView textView = (TextView) this.mDialogLayout.findViewById(com.android.settings.R.id.number);
        String line1Number = telephonyManager.getLine1Number(this.mSubInfoRecord.getSubscriptionId());
        if (TextUtils.isEmpty(line1Number)) {
            textView.setText(resources.getString(R.string.unknownName));
        } else {
            textView.setText(PhoneNumberUtils.formatNumber(line1Number));
        }
        String simOperatorName = telephonyManager.getSimOperatorName(this.mSubInfoRecord.getSubscriptionId());
        TextView textView2 = (TextView) this.mDialogLayout.findViewById(com.android.settings.R.id.carrier);
        if (TextUtils.isEmpty(simOperatorName)) {
            simOperatorName = this.mContext.getString(R.string.unknownName);
        }
        textView2.setText(simOperatorName);
        this.mBuilder.setTitle(String.format(resources.getString(com.android.settings.R.string.sim_editor_title), Integer.valueOf(this.mSubInfoRecord.getSimSlotIndex() + 1)));
        this.mBuilder.setPositiveButton(com.android.settings.R.string.okay, new DialogInterface.OnClickListener() { // from class: com.android.settings.sim.SimPreferenceDialog.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                EditText editText2 = (EditText) SimPreferenceDialog.this.mDialogLayout.findViewById(com.android.settings.R.id.sim_name);
                Utils.setEditTextCursorPosition(editText2);
                String string = editText2.getText().toString();
                int subscriptionId = SimPreferenceDialog.this.mSubInfoRecord.getSubscriptionId();
                SimPreferenceDialog.this.mSubInfoRecord.setDisplayName(string);
                SimPreferenceDialog.this.mSubscriptionManager.setDisplayName(string, subscriptionId, 2L);
                int selectedItemPosition = spinner.getSelectedItemPosition();
                int subscriptionId2 = SimPreferenceDialog.this.mSubInfoRecord.getSubscriptionId();
                int i3 = SimPreferenceDialog.this.mTintArr[selectedItemPosition];
                SimPreferenceDialog.this.mSubInfoRecord.setIconTint(i3);
                SimPreferenceDialog.this.mSubscriptionManager.setIconTint(i3, subscriptionId2);
                dialogInterface.dismiss();
            }
        });
        this.mBuilder.setNegativeButton(com.android.settings.R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.android.settings.sim.SimPreferenceDialog.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                dialogInterface.dismiss();
            }
        });
        this.mBuilder.setOnKeyListener(new DialogInterface.OnKeyListener() { // from class: com.android.settings.sim.SimPreferenceDialog.5
            @Override // android.content.DialogInterface.OnKeyListener
            public boolean onKey(DialogInterface dialogInterface, int i2, KeyEvent keyEvent) {
                if (i2 == 4) {
                    dialogInterface.dismiss();
                    return true;
                }
                return false;
            }
        });
        this.mBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.android.settings.sim.SimPreferenceDialog.6
            @Override // android.content.DialogInterface.OnCancelListener
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
            }
        });
        this.mBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.settings.sim.SimPreferenceDialog.7
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                SimPreferenceDialog.this.finish();
            }
        });
        this.mSimManagementExt.hideSimEditorView(this.mDialogLayout, this.mContext);
        this.mDialog = this.mBuilder.create();
        this.mDialog.show();
    }

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectColorAdapter(Context context, int i, String[] strArr) {
            super(context, i, strArr);
            this.mContext = context;
            this.mResId = i;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) throws Resources.NotFoundException {
            ViewHolder viewHolder;
            LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            Resources resources = this.mContext.getResources();
            int dimensionPixelSize = resources.getDimensionPixelSize(com.android.settings.R.dimen.color_swatch_size);
            int dimensionPixelSize2 = resources.getDimensionPixelSize(com.android.settings.R.dimen.color_swatch_stroke_width);
            if (view == null) {
                view = layoutInflater.inflate(this.mResId, (ViewGroup) null);
                viewHolder = new ViewHolder();
                ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
                shapeDrawable.setIntrinsicHeight(dimensionPixelSize);
                shapeDrawable.setIntrinsicWidth(dimensionPixelSize);
                shapeDrawable.getPaint().setStrokeWidth(dimensionPixelSize2);
                viewHolder.label = (TextView) view.findViewById(com.android.settings.R.id.color_text);
                viewHolder.icon = (ImageView) view.findViewById(com.android.settings.R.id.color_icon);
                viewHolder.swatch = shapeDrawable;
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.label.setText(getItem(i));
            viewHolder.swatch.getPaint().setColor(SimPreferenceDialog.this.mTintArr[i]);
            viewHolder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            viewHolder.icon.setVisibility(0);
            viewHolder.icon.setImageDrawable(viewHolder.swatch);
            return view;
        }

        @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter, android.widget.SpinnerAdapter
        public View getDropDownView(int i, View view, ViewGroup viewGroup) throws Resources.NotFoundException {
            View view2 = getView(i, view, viewGroup);
            ViewHolder viewHolder = (ViewHolder) view2.getTag();
            if (SimPreferenceDialog.this.mTintSelectorPos == i) {
                viewHolder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                viewHolder.swatch.getPaint().setStyle(Paint.Style.STROKE);
            }
            viewHolder.icon.setVisibility(0);
            return view2;
        }

        private class ViewHolder {
            ImageView icon;
            TextView label;
            ShapeDrawable swatch;

            private ViewHolder() {
            }
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        super.onDestroy();
    }
}
