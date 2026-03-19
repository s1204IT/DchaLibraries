package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ExportVCardActivity;
import com.mediatek.contacts.list.MultiBasePickerAdapter;
import com.mediatek.contacts.list.service.MultiChoiceHandlerListener;
import com.mediatek.contacts.list.service.MultiChoiceRequest;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.ProgressHandler;
import java.util.ArrayList;
import java.util.List;

public class MultiDuplicationPickerFragment extends MultiBasePickerFragment {
    private AccountWithDataSet mAccountDst;
    private AccountWithDataSet mAccountSrc;
    private CopyRequestConnection mCopyRequestConnection;
    private HandlerThread mHandlerThread;
    private SendRequestHandler mRequestHandler;
    private int mDstStoreType = 0;
    private List<MultiChoiceRequest> mRequests = new ArrayList();
    private int mRetryCount = 20;
    private int mClickCounter = 1;
    private ProgressHandler mProgressHandler = new ProgressHandler();
    private String mCallingActivityName = null;
    private PBHLoadFinishReceiver mPHBLoadFinishReceiver = new PBHLoadFinishReceiver();

    static int access$410(MultiDuplicationPickerFragment multiDuplicationPickerFragment) {
        int i = multiDuplicationPickerFragment.mRetryCount;
        multiDuplicationPickerFragment.mRetryCount = i - 1;
        return i;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = (Intent) getArguments().getParcelable("intent");
        if (intent.getExtras() != null && intent.getExtras().getClassLoader() == null) {
            Log.w("MultiDuplicationPickerFragment", "[onCreate] The ClassLoader of bundle is null, will reset it");
            intent.setExtrasClassLoader(getClass().getClassLoader());
        }
        this.mAccountSrc = (AccountWithDataSet) intent.getParcelableExtra("fromaccount");
        this.mAccountDst = (AccountWithDataSet) intent.getParcelableExtra("toaccount");
        this.mDstStoreType = getStoreType(this.mAccountDst);
        Activity activity = getActivity();
        if (activity == null) {
            Log.e("MultiDuplicationPickerFragment", "[onCreate] getActivity = null and return");
            return;
        }
        this.mCallingActivityName = activity.getIntent().getExtras().getString("CALLING_ACTIVITY");
        Log.i("MultiDuplicationPickerFragment", "[onCreate] callingActivityName = " + this.mCallingActivityName);
        Log.i("MultiDuplicationPickerFragment", "[onCreate]Destination store type is: " + storeTypeToString(this.mDstStoreType));
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        Log.i("MultiDuplicationPickerFragment", "[configureAdapter]mAccountSrc.type: " + this.mAccountSrc.type + ",mAccountSrc.name:" + this.mAccountSrc.name);
        super.setListFilter(ContactListFilter.createAccountFilter(this.mAccountSrc.type, this.mAccountSrc.name, null, null));
    }

    @Override
    public boolean isAccountFilterEnable() {
        return false;
    }

    @Override
    public void onDestroyView() {
        Log.i("MultiDuplicationPickerFragment", "[onDestroyView]");
        super.onDestroyView();
        this.mProgressHandler.dismissDialog(getFragmentManager());
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    @Override
    public void onOptionAction() {
        if (getCheckedItemIds().length == 0) {
            Log.w("MultiDuplicationPickerFragment", "[onOptionAction]length = 0");
            Toast.makeText(getContext(), R.string.multichoice_no_select_alert, 0).show();
            return;
        }
        Log.i("MultiDuplicationPickerFragment", "[onOptionAction]mDstStoreType = " + this.mDstStoreType);
        if (this.mDstStoreType != 4) {
            if (this.mClickCounter <= 0) {
                Log.w("MultiDuplicationPickerFragment", "[onOptionAction]Avoid re-entrence");
                return;
            }
            this.mClickCounter--;
        }
        if (this.mDstStoreType == 4) {
            doExportVCardToSDCard();
            return;
        }
        startCopyService();
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread("MultiDuplicationPickerFragment");
            this.mHandlerThread.start();
            this.mRequestHandler = new SendRequestHandler(this.mHandlerThread.getLooper());
        }
        MultiBasePickerAdapter.PickListItemCache listItemCache = ((MultiBasePickerAdapter) getAdapter()).getListItemCache();
        if (listItemCache.isEmpty()) {
            Log.w("MultiDuplicationPickerFragment", "[onOptionAction]listItemCacher is empty,return.");
            return;
        }
        for (long j : getCheckedItemIds()) {
            MultiBasePickerAdapter.PickListItemCache.PickListItemData itemData = listItemCache.getItemData(j);
            this.mRequests.add(new MultiChoiceRequest(itemData.contactIndicator, itemData.simIndex, (int) j, itemData.displayName));
        }
        if (this.mDstStoreType == 2 || this.mDstStoreType == 3 || this.mDstStoreType == 6 || this.mDstStoreType == 7) {
            int subId = ((AccountWithDataSetEx) this.mAccountDst).getSubId();
            if (SimCardUtils.isPhoneBookReady(subId)) {
                boolean zIsServiceRunning = SimServiceUtils.isServiceRunning(getContext(), subId);
                Log.i("MultiDuplicationPickerFragment", "[onOptionAction]AbstractService state is running? " + zIsServiceRunning);
                if (zIsServiceRunning) {
                    MtkToast.toast(getActivity(), R.string.notifier_fail_copy_title, 0);
                    destroyMyself();
                    return;
                } else {
                    this.mRequestHandler.sendMessage(this.mRequestHandler.obtainMessage(100, this.mRequests));
                    return;
                }
            }
            Log.i("MultiDuplicationPickerFragment", "[onOptionAction] isPhoneBookReady return false.");
            MtkToast.toast(getActivity(), R.string.notifier_fail_copy_title, 0);
            destroyMyself();
            return;
        }
        this.mRequestHandler.sendMessage(this.mRequestHandler.obtainMessage(100, this.mRequests));
    }

    private static int getStoreType(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            Log.w("MultiDuplicationPickerFragment", "[getStoreType]account is null.");
            return 0;
        }
        if ("_STORAGE_ACCOUNT".equals(accountWithDataSet.type)) {
            return 4;
        }
        if ("Local Phone Account".equals(accountWithDataSet.type)) {
            return 1;
        }
        if ("SIM Account".equals(accountWithDataSet.type)) {
            return 2;
        }
        if ("USIM Account".equals(accountWithDataSet.type)) {
            return 3;
        }
        if ("RUIM Account".equals(accountWithDataSet.type)) {
            return 6;
        }
        if ("CSIM Account".equals(accountWithDataSet.type)) {
            return 7;
        }
        return 5;
    }

    private static String storeTypeToString(int i) {
        switch (i) {
            case 0:
                return "DST_STORE_TYPE_NONE";
            case 1:
                return "DST_STORE_TYPE_PHONE";
            case 2:
                return "DST_STORE_TYPE_SIM";
            case 3:
                return "DST_STORE_TYPE_USIM";
            case CompatUtils.TYPE_ASSERT:
                return "DST_STORE_TYPE_STORAGE";
            case 5:
                return "DST_STORE_TYPE_ACCOUNT";
            case 6:
                return "DST_STORE_TYPE_RUIM";
            case 7:
                return "DST_STORE_TYPE_CSIM";
            default:
                return "DST_STORE_TYPE_UNKNOWN";
        }
    }

    private void doExportVCardToSDCard() {
        long[] checkedItemIds = getCheckedItemIds();
        StringBuilder sb = new StringBuilder();
        sb.append("_id IN (");
        int i = 0;
        int length = checkedItemIds.length;
        int i2 = 0;
        while (i < length) {
            long j = checkedItemIds[i];
            int i3 = i2 + 1;
            if (i2 != 0) {
                sb.append("," + j);
            } else {
                sb.append(j);
            }
            i++;
            i2 = i3;
        }
        sb.append(")");
        Log.d("MultiDuplicationPickerFragment", "[doExportVCardToSDCard] exportSelection is " + sb.toString());
        Intent intent = new Intent(getActivity(), (Class<?>) ExportVCardActivity.class);
        Log.i("MultiDuplicationPickerFragment", "[doExportVCardToSDCard] mCallingActivityName = " + this.mCallingActivityName);
        intent.putExtra("CALLING_ACTIVITY", this.mCallingActivityName);
        intent.putExtra("exportselection", sb.toString());
        if (this.mAccountDst instanceof AccountWithDataSet) {
            intent.putExtra("dest_path", this.mAccountDst.dataSet);
        }
        getActivity().startActivityForResult(intent, 11111);
    }

    private class CopyRequestConnection implements ServiceConnection {
        private MultiChoiceService mService;

        private CopyRequestConnection() {
        }

        public boolean sendCopyRequest(List<MultiChoiceRequest> list) {
            Log.d("MultiDuplicationPickerFragment", "[sendCopyRequest]Send an copy request");
            if (this.mService == null) {
                Log.i("MultiDuplicationPickerFragment", "[sendCopyRequest]mService is not ready");
                return false;
            }
            this.mService.handleCopyRequest(list, new MultiChoiceHandlerListener(this.mService, MultiDuplicationPickerFragment.this.mCallingActivityName), MultiDuplicationPickerFragment.this.mAccountSrc, MultiDuplicationPickerFragment.this.mAccountDst);
            return true;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("MultiDuplicationPickerFragment", "[onServiceConnected]");
            this.mService = ((MultiChoiceService.MyBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("MultiDuplicationPickerFragment", "[onServiceDisconnected]");
        }
    }

    private class SendRequestHandler extends Handler {
        public SendRequestHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            Log.d("MultiDuplicationPickerFragment", "[handleMessage]msg.what = " + message.what);
            if (message.what == 100) {
                if (!MultiDuplicationPickerFragment.this.mCopyRequestConnection.sendCopyRequest((List) message.obj)) {
                    if (MultiDuplicationPickerFragment.access$410(MultiDuplicationPickerFragment.this) > 0) {
                        sendMessageDelayed(obtainMessage(message.what, message.obj), 500L);
                        return;
                    } else {
                        sendMessage(obtainMessage(300));
                        return;
                    }
                }
                sendMessage(obtainMessage(300));
                return;
            }
            if (message.what == 300) {
                MultiDuplicationPickerFragment.this.destroyMyself();
                return;
            }
            if (message.what == 200) {
                MultiDuplicationPickerFragment.this.unRegisterReceiver();
                sendMessage(obtainMessage(100, MultiDuplicationPickerFragment.this.mRequests));
                return;
            }
            if (message.what == 400) {
                MultiDuplicationPickerFragment.this.mProgressHandler.showDialog(MultiDuplicationPickerFragment.this.getFragmentManager());
            } else if (message.what == 500) {
                MultiDuplicationPickerFragment.this.mProgressHandler.dismissDialog(MultiDuplicationPickerFragment.this.getFragmentManager());
            }
            super.handleMessage(message);
        }
    }

    void startCopyService() {
        this.mCopyRequestConnection = new CopyRequestConnection();
        Log.i("MultiDuplicationPickerFragment", "[startCopyService]Bind to MultiChoiceService.");
        Intent intent = new Intent(getActivity(), (Class<?>) MultiChoiceService.class);
        getActivity().getApplicationContext().startService(intent);
        getActivity().getApplicationContext().bindService(intent, this.mCopyRequestConnection, 1);
    }

    void destroyMyself() {
        Log.d("MultiDuplicationPickerFragment", "[destroyMyself]");
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
        }
        if (getActivity() != null) {
            getActivity().getApplicationContext().unbindService(this.mCopyRequestConnection);
            getActivity().finish();
        }
    }

    private class PBHLoadFinishReceiver extends BroadcastReceiver {
        private PBHLoadFinishReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("MultiDuplicationPickerFragment", "[onReceive] PBHLoadFinishReceiver action:" + action);
            if (action.equals("com.android.contacts.ACTION_PHB_LOAD_FINISHED")) {
                if (intent.getIntExtra("subscription", -1) == ((AccountWithDataSetEx) MultiDuplicationPickerFragment.this.mAccountDst).getSubId()) {
                    Log.i("MultiDuplicationPickerFragment", "[onReceive] intent subId is " + intent.getIntExtra("subscription", -1));
                    MultiDuplicationPickerFragment.this.mRequestHandler.sendMessage(MultiDuplicationPickerFragment.this.mRequestHandler.obtainMessage(200));
                }
            }
        }
    }

    private void unRegisterReceiver() {
        Log.i("MultiDuplicationPickerFragment", "[unRegisterReceiver]");
        getActivity().unregisterReceiver(this.mPHBLoadFinishReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
