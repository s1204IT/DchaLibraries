package com.android.managedprovisioning.preprovisioning;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.AccessibilityContextMenuMaker;
import com.android.managedprovisioning.common.ClickableSpanFactory;
import com.android.managedprovisioning.common.DialogBuilder;
import com.android.managedprovisioning.common.LogoUtils;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SetupGlifLayoutActivity;
import com.android.managedprovisioning.common.SimpleDialog;
import com.android.managedprovisioning.common.StringConcatenator;
import com.android.managedprovisioning.common.TouchTargetEnforcer;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.CustomizationParams;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.PreProvisioningController;
import com.android.managedprovisioning.preprovisioning.anim.BenefitsAnimation;
import com.android.managedprovisioning.preprovisioning.terms.TermsActivity;
import com.android.managedprovisioning.provisioning.ProvisioningActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreProvisioningActivity extends SetupGlifLayoutActivity implements SimpleDialog.SimpleDialogListener, PreProvisioningController.Ui {
    protected static final int PROVISIONING_REQUEST_CODE = 2;
    private static final List<Integer> SLIDE_CAPTIONS = createImmutableList(R.string.info_anim_title_0, R.string.info_anim_title_1, R.string.info_anim_title_2);
    private static final List<Integer> SLIDE_CAPTIONS_COMP = createImmutableList(R.string.info_anim_title_0, R.string.one_place_for_work_apps, R.string.info_anim_title_2);
    private BenefitsAnimation mBenefitsAnimation;
    private ClickableSpanFactory mClickableSpanFactory;
    private final AccessibilityContextMenuMaker mContextMenuMaker;
    private PreProvisioningController mController;
    private ControllerProvider mControllerProvider;
    private TouchTargetEnforcer mTouchTargetEnforcer;

    interface ControllerProvider {
        PreProvisioningController getInstance(PreProvisioningActivity preProvisioningActivity);
    }

    public PreProvisioningActivity() {
        this(new ControllerProvider() {
            @Override
            public final PreProvisioningController getInstance(PreProvisioningActivity preProvisioningActivity) {
                return PreProvisioningActivity.lambda$new$0(preProvisioningActivity);
            }
        }, null, new Utils());
    }

    static PreProvisioningController lambda$new$0(PreProvisioningActivity preProvisioningActivity) {
        return new PreProvisioningController(preProvisioningActivity, preProvisioningActivity);
    }

    public PreProvisioningActivity(ControllerProvider controllerProvider, AccessibilityContextMenuMaker accessibilityContextMenuMaker, Utils utils) {
        super(utils);
        this.mControllerProvider = controllerProvider;
        this.mContextMenuMaker = accessibilityContextMenuMaker == null ? new AccessibilityContextMenuMaker(this) : accessibilityContextMenuMaker;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mClickableSpanFactory = new ClickableSpanFactory(getColor(R.color.blue));
        this.mTouchTargetEnforcer = new TouchTargetEnforcer(getResources().getDisplayMetrics().density);
        this.mController = this.mControllerProvider.getInstance(this);
        this.mController.initiateProvisioning(getIntent(), bundle == null ? null : (ProvisioningParams) bundle.getParcelable("saved_provisioning_params"), getCallingPackage());
    }

    @Override
    public void finish() {
        LogoUtils.cleanUp(this);
        ProvisioningParams params = this.mController.getParams();
        if (params != null) {
            params.cleanUp();
        }
        EncryptionController.getInstance(this).cancelEncryptionReminder();
        super.finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("saved_provisioning_params", this.mController.getParams());
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        switch (i) {
            case EncryptionController.NOTIFICATION_ID:
                if (i2 == 0) {
                    ProvisionLogger.loge("User canceled device encryption.");
                }
                break;
            case PROVISIONING_REQUEST_CODE:
                setResult(i2);
                finish();
                break;
            case 3:
                if (i2 == 0) {
                    ProvisionLogger.loge("User canceled wifi picking.");
                } else if (i2 == -1) {
                    ProvisionLogger.logd("Wifi request result is OK");
                }
                this.mController.initiateProvisioning(getIntent(), null, getCallingPackage());
                break;
            case 4:
                this.mController.continueProvisioningAfterUserConsent();
                break;
            default:
                ProvisionLogger.logw("Unknown result code :" + i2);
                break;
        }
    }

    @Override
    public void showErrorAndClose(Integer num, int i, String str) {
        ProvisionLogger.loge(str);
        showDialog(new SimpleDialog.Builder().setTitle(num).setMessage(i).setCancelable(false).setPositiveButtonMessage(R.string.device_owner_error_ok), "PreProvErrorAndCloseDialog");
    }

    @Override
    public void onNegativeButtonClick(DialogFragment dialogFragment) {
        byte b;
        String tag = dialogFragment.getTag();
        int iHashCode = tag.hashCode();
        if (iHashCode != -1194926359) {
            if (iHashCode != 94835986) {
                if (iHashCode != 838661085) {
                    b = (iHashCode == 1387831095 && tag.equals("PreProvBackPressedDialog")) ? (byte) 1 : (byte) -1;
                } else if (tag.equals("PreProvCancelledConsentDialog")) {
                    b = 0;
                }
            } else if (tag.equals("PreProvCurrentLauncherInvalidDialog")) {
                b = 2;
            }
        } else if (tag.equals("PreProvDeleteManagedProfileDialog")) {
            b = 3;
        }
        switch (b) {
            case 0:
            case EncryptionController.NOTIFICATION_ID:
                break;
            case PROVISIONING_REQUEST_CODE:
                dialogFragment.dismiss();
                break;
            case 3:
                setResult(0);
                finish();
                break;
            default:
                SimpleDialog.throwButtonClickHandlerNotImplemented(dialogFragment);
                break;
        }
    }

    @Override
    public void onPositiveButtonClick(DialogFragment dialogFragment) {
        switch (dialogFragment.getTag()) {
            case "PreProvErrorAndCloseDialog":
            case "PreProvBackPressedDialog":
                setResult(0);
                this.mController.logPreProvisioningCancelled();
                finish();
                break;
            case "PreProvCancelledConsentDialog":
                this.mUtils.sendFactoryResetBroadcast(this, "Device owner setup cancelled");
                break;
            case "PreProvCurrentLauncherInvalidDialog":
                requestLauncherPick();
                break;
            case "PreProvDeleteManagedProfileDialog":
                this.mController.removeUser(((DeleteManagedProfileDialog) dialogFragment).getUserId());
                this.mController.checkResumeSilentProvisioning();
                break;
            default:
                SimpleDialog.throwButtonClickHandlerNotImplemented(dialogFragment);
                break;
        }
    }

    @Override
    public void requestEncryption(ProvisioningParams provisioningParams) {
        Intent intent = new Intent(this, (Class<?>) EncryptDeviceActivity.class);
        intent.putExtra("provisioningParams", provisioningParams);
        startActivityForResult(intent, 1);
    }

    @Override
    public void requestWifiPick() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        startActivityForResult(this.mUtils.getWifiPickIntent(), 3);
    }

    @Override
    public void showCurrentLauncherInvalid() {
        showDialog(new SimpleDialog.Builder().setCancelable(false).setTitle(Integer.valueOf(R.string.change_device_launcher)).setMessage(R.string.launcher_app_cant_be_used_by_work_profile).setNegativeButtonMessage(R.string.cancel_provisioning).setPositiveButtonMessage(R.string.pick_launcher), "PreProvCurrentLauncherInvalidDialog");
    }

    private void requestLauncherPick() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.HOME_SETTINGS");
        intent.putExtra("support_managed_profiles", true);
        startActivityForResult(intent, 4);
    }

    @Override
    public void startProvisioning(int i, ProvisioningParams provisioningParams) {
        Intent intent = new Intent(this, (Class<?>) ProvisioningActivity.class);
        intent.putExtra("provisioningParams", provisioningParams);
        startActivityForResultAsUser(intent, PROVISIONING_REQUEST_CODE, new UserHandle(i));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void initiateUi(int i, int i2, String str, Drawable drawable, boolean z, boolean z2, List<String> list, CustomizationParams customizationParams) {
        initializeLayoutParams(i, z ? null : Integer.valueOf(R.string.set_up_your_device), customizationParams.mainColor, customizationParams.statusBarColor);
        Button button = (Button) findViewById(R.id.next_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PreProvisioningActivity.lambda$initiateUi$1(this.f$0, view);
            }
        });
        button.setBackgroundTintList(ColorStateList.valueOf(customizationParams.mainColor));
        if (this.mUtils.isBrightColor(customizationParams.mainColor)) {
            button.setTextColor(getColor(R.color.gray_button_text));
        }
        setTitle(i2);
        String strJoin = new StringConcatenator(getResources()).join(list);
        if (z) {
            initiateUIProfileOwner(strJoin, z2, customizationParams);
        } else {
            initiateUIDeviceOwner(str, drawable, strJoin, customizationParams);
        }
    }

    public static void lambda$initiateUi$1(PreProvisioningActivity preProvisioningActivity, View view) {
        ProvisionLogger.logi("Next button (next_button) is clicked.");
        preProvisioningActivity.mController.continueProvisioningAfterUserConsent();
    }

    private void initiateUIProfileOwner(String str, boolean z, CustomizationParams customizationParams) {
        String string;
        List<Integer> list;
        int i;
        ((Button) findViewById(R.id.close_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PreProvisioningActivity.lambda$initiateUIProfileOwner$2(this.f$0, view);
            }
        });
        int i2 = z ? R.string.profile_owner_info_comp : R.string.profile_owner_info;
        int i3 = z ? R.string.profile_owner_info_with_terms_headers_comp : R.string.profile_owner_info_with_terms_headers;
        TextView textView = (TextView) findViewById(R.id.profile_owner_short_info);
        if (str.isEmpty()) {
            string = getString(i2);
        } else {
            string = getResources().getString(i3, str);
        }
        textView.setText(string);
        View viewFindViewById = findViewById(R.id.show_terms_button);
        viewFindViewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.startViewTermsActivity(view);
            }
        });
        this.mTouchTargetEnforcer.enforce(viewFindViewById, (View) viewFindViewById.getParent());
        if (z) {
            list = SLIDE_CAPTIONS_COMP;
        } else {
            list = SLIDE_CAPTIONS;
        }
        if (z) {
            i = R.string.comp_profile_benefits_description;
        } else {
            i = R.string.profile_benefits_description;
        }
        this.mBenefitsAnimation = new BenefitsAnimation(this, list, i, customizationParams);
    }

    public static void lambda$initiateUIProfileOwner$2(PreProvisioningActivity preProvisioningActivity, View view) {
        ProvisionLogger.logi("Close button (close_button) is clicked.");
        preProvisioningActivity.onBackPressed();
    }

    private void initiateUIDeviceOwner(String str, Drawable drawable, String str2, CustomizationParams customizationParams) {
        TextView textView = (TextView) findViewById(R.id.device_owner_terms_info);
        textView.setText(assembleDOTermsMessage(str2, customizationParams.orgName));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        this.mContextMenuMaker.registerWithActivity(textView);
        if (customizationParams.supportUrl != null) {
            TextView textView2 = (TextView) findViewById(R.id.device_owner_provider_info);
            textView2.setVisibility(0);
            String string = getString(R.string.organization_admin);
            String string2 = getString(R.string.contact_device_provider, new Object[]{string});
            SpannableString spannableString = new SpannableString(string2);
            Intent intentCreateIntent = WebActivity.createIntent(this, customizationParams.supportUrl, customizationParams.statusBarColor);
            if (intentCreateIntent != null) {
                ClickableSpan clickableSpanCreate = this.mClickableSpanFactory.create(intentCreateIntent);
                int iIndexOf = string2.indexOf(string);
                spannableString.setSpan(clickableSpanCreate, iIndexOf, string.length() + iIndexOf, 33);
                textView2.setMovementMethod(LinkMovementMethod.getInstance());
            }
            textView2.setText(spannableString);
            this.mContextMenuMaker.registerWithActivity(textView2);
        }
        setDpcIconAndLabel(str, drawable, customizationParams.orgName);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        if (view instanceof TextView) {
            this.mContextMenuMaker.populateMenuContent(contextMenu, (TextView) view);
        }
    }

    private void startViewTermsActivity(View view) {
        startActivity(createViewTermsIntent());
    }

    private Intent createViewTermsIntent() {
        return new Intent(this, (Class<?>) TermsActivity.class).putExtra("provisioningParams", this.mController.getParams());
    }

    private Spannable assembleDOTermsMessage(String str, String str2) {
        String string;
        String string2 = getString(R.string.view_terms);
        if (TextUtils.isEmpty(str2)) {
            str2 = getString(R.string.your_organization_middle);
        }
        if (str.isEmpty()) {
            string = getString(R.string.device_owner_info, new Object[]{str2, string2});
        } else {
            string = getString(R.string.device_owner_info_with_terms_headers, new Object[]{str2, str, string2});
        }
        SpannableString spannableString = new SpannableString(string);
        int iIndexOf = string.indexOf(string2);
        spannableString.setSpan(this.mClickableSpanFactory.create(createViewTermsIntent()), iIndexOf, string2.length() + iIndexOf, 33);
        return spannableString;
    }

    private void setDpcIconAndLabel(String str, Drawable drawable, String str2) {
        if (drawable == null || TextUtils.isEmpty(str)) {
            return;
        }
        findViewById(R.id.intro_device_owner_app_info_container).setVisibility(0);
        if (TextUtils.isEmpty(str2)) {
            str2 = getString(R.string.your_organization_beginning);
        }
        ((TextView) findViewById(R.id.device_owner_app_info_text)).setText(getString(R.string.your_org_app_used, new Object[]{str2}));
        ImageView imageView = (ImageView) findViewById(R.id.device_manager_icon_view);
        imageView.setImageDrawable(drawable);
        imageView.setContentDescription(getResources().getString(R.string.mdm_icon_label, str));
        ((TextView) findViewById(R.id.device_manager_name)).setText(str);
    }

    @Override
    public void showDeleteManagedProfileDialog(final ComponentName componentName, final String str, final int i) {
        showDialog(new DialogBuilder() {
            @Override
            public final DialogFragment build() {
                return DeleteManagedProfileDialog.newInstance(i, componentName, str);
            }
        }, "PreProvDeleteManagedProfileDialog");
    }

    @Override
    public void onBackPressed() {
        this.mController.logPreProvisioningCancelled();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mBenefitsAnimation != null) {
            this.mBenefitsAnimation.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mBenefitsAnimation != null) {
            this.mBenefitsAnimation.stop();
        }
    }

    private static List<Integer> createImmutableList(int... iArr) {
        if (iArr == null || iArr.length == 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        return Collections.unmodifiableList(arrayList);
    }
}
