package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.InstrumentedFragment;
import java.util.Locale;

/* loaded from: classes.dex */
public class CryptKeeperConfirm extends InstrumentedFragment {
    private View mContentView;
    private Button mFinalButton;
    private View.OnClickListener mFinalClickListener = new View.OnClickListener() { // from class: com.android.settings.CryptKeeperConfirm.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(CryptKeeperConfirm.this.getActivity());
            lockPatternUtils.setVisiblePatternEnabled(lockPatternUtils.isVisiblePatternEnabled(0), 0);
            if (lockPatternUtils.isOwnerInfoEnabled(0)) {
                lockPatternUtils.setOwnerInfo(lockPatternUtils.getOwnerInfo(0), 0);
            }
            lockPatternUtils.setVisiblePasswordEnabled(Settings.System.getInt(CryptKeeperConfirm.this.getContext().getContentResolver(), "show_password", 1) != 0, 0);
            Intent intent = new Intent(CryptKeeperConfirm.this.getActivity(), (Class<?>) Blank.class);
            intent.putExtras(CryptKeeperConfirm.this.getArguments());
            CryptKeeperConfirm.this.startActivity(intent);
            try {
                IStorageManager.Stub.asInterface(ServiceManager.getService("mount")).setField("SystemLocale", Locale.getDefault().toLanguageTag());
            } catch (Exception e) {
                Log.e("CryptKeeperConfirm", "Error storing locale for decryption UI", e);
            }
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 33;
    }

    public static class Blank extends Activity {
        private Handler mHandler = new Handler();

        @Override // android.app.Activity
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            setContentView(R.layout.crypt_keeper_blank);
            if (Utils.isMonkeyRunning()) {
                finish();
            }
            ((StatusBarManager) getSystemService("statusbar")).disable(58130432);
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.settings.CryptKeeperConfirm.Blank.1
                @Override // java.lang.Runnable
                public void run() {
                    IBinder service = ServiceManager.getService("mount");
                    if (service == null) {
                        Log.e("CryptKeeper", "Failed to find the mount service");
                        Blank.this.finish();
                        return;
                    }
                    IStorageManager iStorageManagerAsInterface = IStorageManager.Stub.asInterface(service);
                    try {
                        Bundle extras = Blank.this.getIntent().getExtras();
                        iStorageManagerAsInterface.encryptStorage(extras.getInt("type", -1), extras.getString("password"));
                    } catch (Exception e) {
                        Log.e("CryptKeeper", "Error while encrypting...", e);
                    }
                }
            }, 700L);
        }
    }

    private void establishFinalConfirmationState() {
        this.mFinalButton = (Button) this.mContentView.findViewById(R.id.execute_encrypt);
        this.mFinalButton.setOnClickListener(this.mFinalClickListener);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.crypt_keeper_confirm_title);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mContentView = layoutInflater.inflate(R.layout.crypt_keeper_confirm, (ViewGroup) null);
        establishFinalConfirmationState();
        return this.mContentView;
    }
}
