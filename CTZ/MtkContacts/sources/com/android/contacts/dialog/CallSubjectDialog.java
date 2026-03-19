package com.android.contacts.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.contacts.CallUtil;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.PhoneAccountSdkCompat;
import com.android.contacts.compat.telecom.TelecomManagerCompat;
import com.android.contacts.util.UriUtils;
import com.android.phone.common.animation.AnimUtils;
import com.mediatek.contacts.util.Log;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

public class CallSubjectDialog extends Activity {
    private int mAnimationDuration;
    private View mBackgroundView;
    private EditText mCallSubjectView;
    private TextView mCharacterLimitView;
    private QuickContactBadge mContactPhoto;
    private Uri mContactUri;
    private View mDialogView;
    private String mDisplayNumber;
    private View mHistoryButton;
    private boolean mIsBusiness;
    private Charset mMessageEncoding;
    private String mNameOrNumber;
    private TextView mNameView;
    private String mNumber;
    private String mNumberLabel;
    private TextView mNumberView;
    private PhoneAccountHandle mPhoneAccountHandle;
    private long mPhotoID;
    private int mPhotoSize;
    private Uri mPhotoUri;
    private SharedPreferences mPrefs;
    private View mSendAndCallButton;
    private List<String> mSubjectHistory;
    private ListView mSubjectList;
    private int mLimit = 16;
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            CallSubjectDialog.this.updateCharacterLimit();
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };
    private View.OnClickListener mBackgroundListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CallSubjectDialog.this.finish();
        }
    };
    private final View.OnClickListener mHistoryOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CallSubjectDialog.this.hideSoftKeyboard(CallSubjectDialog.this, CallSubjectDialog.this.mCallSubjectView);
            CallSubjectDialog.this.showCallHistory(CallSubjectDialog.this.mSubjectList.getVisibility() == 8);
        }
    };
    private final View.OnClickListener mSendAndCallOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String string = CallSubjectDialog.this.mCallSubjectView.getText().toString();
            TelecomManagerCompat.placeCall(CallSubjectDialog.this, (TelecomManager) CallSubjectDialog.this.getSystemService("telecom"), CallUtil.getCallWithSubjectIntent(CallSubjectDialog.this.mNumber, CallSubjectDialog.this.mPhoneAccountHandle, string));
            CallSubjectDialog.this.mSubjectHistory.add(string);
            CallSubjectDialog.this.saveSubjectHistory(CallSubjectDialog.this.mSubjectHistory);
            CallSubjectDialog.this.finish();
        }
    };
    private final View.OnClickListener mCallSubjectClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (CallSubjectDialog.this.mSubjectList.getVisibility() == 0) {
                CallSubjectDialog.this.showCallHistory(false);
            }
        }
    };
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            CallSubjectDialog.this.mCallSubjectView.setText((CharSequence) CallSubjectDialog.this.mSubjectHistory.get(i));
            CallSubjectDialog.this.showCallHistory(false);
        }
    };

    public static void start(Activity activity, Bundle bundle) {
        Intent intent = new Intent(activity, (Class<?>) CallSubjectDialog.class);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAnimationDuration = getResources().getInteger(R.integer.call_subject_animation_duration);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.mPhotoSize = getResources().getDimensionPixelSize(R.dimen.call_subject_dialog_contact_photo_size);
        readArguments();
        loadConfiguration();
        this.mSubjectHistory = loadSubjectHistory(this.mPrefs);
        setContentView(R.layout.dialog_call_subject);
        getWindow().setLayout(-1, -1);
        this.mBackgroundView = findViewById(R.id.call_subject_dialog);
        this.mBackgroundView.setOnClickListener(this.mBackgroundListener);
        this.mDialogView = findViewById(R.id.dialog_view);
        this.mContactPhoto = (QuickContactBadge) findViewById(R.id.contact_photo);
        this.mNameView = (TextView) findViewById(R.id.name);
        this.mNumberView = (TextView) findViewById(R.id.number);
        this.mCallSubjectView = (EditText) findViewById(R.id.call_subject);
        this.mCallSubjectView.addTextChangedListener(this.mTextWatcher);
        this.mCallSubjectView.setOnClickListener(this.mCallSubjectClickListener);
        this.mCallSubjectView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(this.mLimit)});
        this.mCharacterLimitView = (TextView) findViewById(R.id.character_limit);
        this.mHistoryButton = findViewById(R.id.history_button);
        this.mHistoryButton.setOnClickListener(this.mHistoryOnClickListener);
        this.mHistoryButton.setVisibility(this.mSubjectHistory.isEmpty() ? 8 : 0);
        this.mSendAndCallButton = findViewById(R.id.send_and_call_button);
        this.mSendAndCallButton.setOnClickListener(this.mSendAndCallOnClickListener);
        this.mSubjectList = (ListView) findViewById(R.id.subject_list);
        this.mSubjectList.setOnItemClickListener(this.mItemClickListener);
        this.mSubjectList.setVisibility(8);
        updateContactInfo();
        updateCharacterLimit();
    }

    private void updateContactInfo() {
        if (this.mContactUri != null) {
            setPhoto(this.mPhotoID, this.mPhotoUri, this.mContactUri, this.mNameOrNumber, this.mIsBusiness);
        } else {
            this.mContactPhoto.setVisibility(8);
        }
        this.mNameView.setText(this.mNameOrNumber);
        if (!TextUtils.isEmpty(this.mNumberLabel) && !TextUtils.isEmpty(this.mDisplayNumber)) {
            this.mNumberView.setVisibility(0);
            this.mNumberView.setText(getString(R.string.call_subject_type_and_number, new Object[]{this.mNumberLabel, this.mDisplayNumber}));
        } else {
            this.mNumberView.setVisibility(8);
            this.mNumberView.setText((CharSequence) null);
        }
    }

    private void readArguments() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e("CallSubjectDialog", "Arguments cannot be null.");
            return;
        }
        this.mPhotoID = extras.getLong("PHOTO_ID");
        this.mPhotoUri = (Uri) extras.getParcelable("PHOTO_URI");
        this.mContactUri = (Uri) extras.getParcelable("CONTACT_URI");
        this.mNameOrNumber = extras.getString("NAME_OR_NUMBER");
        this.mIsBusiness = extras.getBoolean("IS_BUSINESS");
        this.mNumber = extras.getString("NUMBER");
        this.mDisplayNumber = extras.getString("DISPLAY_NUMBER");
        this.mNumberLabel = extras.getString("NUMBER_LABEL");
        this.mPhoneAccountHandle = (PhoneAccountHandle) extras.getParcelable("PHONE_ACCOUNT_HANDLE");
    }

    private void updateCharacterLimit() {
        int length;
        String string = this.mCallSubjectView.getText().toString();
        if (this.mMessageEncoding != null) {
            length = string.getBytes(this.mMessageEncoding).length;
        } else {
            length = string.length();
        }
        this.mCharacterLimitView.setText(getString(R.string.call_subject_limit, new Object[]{Integer.valueOf(length), Integer.valueOf(this.mLimit)}));
        if (length >= this.mLimit) {
            this.mCharacterLimitView.setTextColor(getResources().getColor(R.color.call_subject_limit_exceeded));
        } else {
            this.mCharacterLimitView.setTextColor(getResources().getColor(R.color.dialtacts_secondary_text_color));
        }
    }

    private void setPhoto(long j, Uri uri, Uri uri2, String str, boolean z) {
        int i;
        this.mContactPhoto.assignContactUri(uri2);
        if (CompatUtils.isLollipopCompatible()) {
            this.mContactPhoto.setOverlay(null);
        }
        if (z) {
            i = 2;
        } else {
            i = 1;
        }
        ContactPhotoManager.DefaultImageRequest defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(str, uri2 != null ? UriUtils.getLookupKeyFromUri(uri2) : null, i, true);
        if (j == 0 && uri != null) {
            ContactPhotoManager.getInstance(this).loadPhoto(this.mContactPhoto, uri, this.mPhotoSize, false, true, defaultImageRequest);
        } else {
            ContactPhotoManager.getInstance(this).loadThumbnail(this.mContactPhoto, j, false, true, defaultImageRequest);
        }
    }

    public static List<String> loadSubjectHistory(SharedPreferences sharedPreferences) {
        int i = sharedPreferences.getInt("subject_history_count", 0);
        ArrayList arrayList = new ArrayList(i);
        for (int i2 = 0; i2 < i; i2++) {
            String string = sharedPreferences.getString("subject_history_item" + i2, null);
            if (!TextUtils.isEmpty(string)) {
                arrayList.add(string);
            }
        }
        return arrayList;
    }

    private void saveSubjectHistory(List<String> list) {
        int i;
        while (true) {
            i = 0;
            if (list.size() <= 5) {
                break;
            } else {
                list.remove(0);
            }
        }
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        for (String str : list) {
            if (!TextUtils.isEmpty(str)) {
                editorEdit.putString("subject_history_item" + i, str);
                i++;
            }
        }
        editorEdit.putInt("subject_history_count", i);
        editorEdit.apply();
    }

    public void hideSoftKeyboard(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 2);
        }
    }

    private void showCallHistory(final boolean z) {
        if (!z || this.mSubjectList.getVisibility() != 0) {
            if (!z && this.mSubjectList.getVisibility() == 8) {
                return;
            }
            final int bottom = this.mDialogView.getBottom();
            if (z) {
                this.mSubjectList.setAdapter((ListAdapter) new ArrayAdapter(this, R.layout.call_subject_history_list_item, this.mSubjectHistory));
                this.mSubjectList.setVisibility(0);
            } else {
                this.mSubjectList.setVisibility(8);
            }
            final ViewTreeObserver viewTreeObserver = this.mBackgroundView.getViewTreeObserver();
            viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (viewTreeObserver.isAlive()) {
                        viewTreeObserver.removeOnPreDrawListener(this);
                    }
                    int bottom2 = bottom - CallSubjectDialog.this.mDialogView.getBottom();
                    if (bottom2 != 0) {
                        CallSubjectDialog.this.mDialogView.setTranslationY(bottom2);
                        CallSubjectDialog.this.mDialogView.animate().translationY(ContactPhotoManager.OFFSET_DEFAULT).setInterpolator(AnimUtils.EASE_OUT_EASE_IN).setDuration(CallSubjectDialog.this.mAnimationDuration).start();
                    }
                    if (z) {
                        CallSubjectDialog.this.mSubjectList.setTranslationY(CallSubjectDialog.this.mSubjectList.getHeight());
                        CallSubjectDialog.this.mSubjectList.animate().translationY(ContactPhotoManager.OFFSET_DEFAULT).setInterpolator(AnimUtils.EASE_OUT_EASE_IN).setDuration(CallSubjectDialog.this.mAnimationDuration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                super.onAnimationEnd(animator);
                            }

                            @Override
                            public void onAnimationStart(Animator animator) {
                                super.onAnimationStart(animator);
                                CallSubjectDialog.this.mSubjectList.setVisibility(0);
                            }
                        }).start();
                        return true;
                    }
                    CallSubjectDialog.this.mSubjectList.setTranslationY(ContactPhotoManager.OFFSET_DEFAULT);
                    CallSubjectDialog.this.mSubjectList.animate().translationY(CallSubjectDialog.this.mSubjectList.getHeight()).setInterpolator(AnimUtils.EASE_OUT_EASE_IN).setDuration(CallSubjectDialog.this.mAnimationDuration).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            super.onAnimationEnd(animator);
                            CallSubjectDialog.this.mSubjectList.setVisibility(8);
                        }

                        @Override
                        public void onAnimationStart(Animator animator) {
                            super.onAnimationStart(animator);
                        }
                    }).start();
                    return true;
                }
            });
        }
    }

    private void loadConfiguration() {
        Bundle extras;
        if (Build.VERSION.SDK_INT <= 23 || this.mPhoneAccountHandle == null || (extras = PhoneAccountSdkCompat.getExtras(((TelecomManager) getSystemService("telecom")).getPhoneAccount(this.mPhoneAccountHandle))) == null) {
            return;
        }
        this.mLimit = extras.getInt("android.telecom.extra.CALL_SUBJECT_MAX_LENGTH", this.mLimit);
        String string = extras.getString("android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING");
        if (!TextUtils.isEmpty(string)) {
            try {
                this.mMessageEncoding = Charset.forName(string);
                return;
            } catch (UnsupportedCharsetException e) {
                Log.w("CallSubjectDialog", "Invalid charset: " + string);
                this.mMessageEncoding = null;
                return;
            }
        }
        this.mMessageEncoding = null;
    }
}
