package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.widget.LockPatternUtils;
import java.util.Locale;

public class CryptKeeperConfirm extends InstrumentedFragment {
    private View mContentView;
    private Button mFinalButton;
    private View.OnClickListener mFinalClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            LockPatternUtils utils = new LockPatternUtils(CryptKeeperConfirm.this.getActivity());
            utils.setVisiblePatternEnabled(utils.isVisiblePatternEnabled(0), 0);
            if (utils.isOwnerInfoEnabled(0)) {
                utils.setOwnerInfo(utils.getOwnerInfo(0), 0);
            }
            int value = Settings.System.getInt(CryptKeeperConfirm.this.getContext().getContentResolver(), "show_password", 1);
            utils.setVisiblePasswordEnabled(value != 0, 0);
            Intent intent = new Intent(CryptKeeperConfirm.this.getActivity(), (Class<?>) Blank.class);
            intent.setFlags(268435456);
            intent.putExtras(CryptKeeperConfirm.this.getArguments());
            CryptKeeperConfirm.this.startActivity(intent);
            try {
                IBinder service = ServiceManager.getService("mount");
                IMountService mountService = IMountService.Stub.asInterface(service);
                mountService.setField("SystemLocale", Locale.getDefault().toLanguageTag());
            } catch (Exception e) {
                Log.e("CryptKeeperConfirm", "Error storing locale for decryption UI", e);
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 33;
    }

    public static class Blank extends Activity {
        private Handler mHandler = new Handler();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.crypt_keeper_blank);
            if (Utils.isMonkeyRunning()) {
                finish();
            }
            StatusBarManager sbm = (StatusBarManager) getSystemService("statusbar");
            sbm.disable(58130432);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IBinder service = ServiceManager.getService("mount");
                    if (service == null) {
                        Log.e("CryptKeeper", "Failed to find the mount service");
                        Blank.this.finish();
                        return;
                    }
                    IMountService mountService = IMountService.Stub.asInterface(service);
                    try {
                        Bundle args = Blank.this.getIntent().getExtras();
                        mountService.encryptStorage(args.getInt("type", -1), args.getString("password"));
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mContentView = inflater.inflate(R.layout.crypt_keeper_confirm, (ViewGroup) null);
        establishFinalConfirmationState();
        return this.mContentView;
    }
}
