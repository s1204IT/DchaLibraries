package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationErrorReport;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.RunningProcessesView;
import com.android.settings.applications.RunningState;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.utils.ThreadUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RunningServiceDetails extends InstrumentedFragment implements RunningState.OnRefreshUiListener {
    ViewGroup mAllDetails;
    ActivityManager mAm;
    boolean mHaveData;
    LayoutInflater mInflater;
    RunningState.MergedItem mMergedItem;
    int mNumProcesses;
    int mNumServices;
    String mProcessName;
    TextView mProcessesHeader;
    View mRootView;
    TextView mServicesHeader;
    boolean mShowBackground;
    ViewGroup mSnippet;
    RunningProcessesView.ActiveItem mSnippetActiveItem;
    RunningProcessesView.ViewHolder mSnippetViewHolder;
    RunningState mState;
    int mUid;
    int mUserId;
    final ArrayList<ActiveDetail> mActiveDetails = new ArrayList<>();
    StringBuilder mBuilder = new StringBuilder(128);

    class ActiveDetail implements View.OnClickListener {
        RunningProcessesView.ActiveItem mActiveItem;
        ComponentName mInstaller;
        PendingIntent mManageIntent;
        Button mReportButton;
        View mRootView;
        RunningState.ServiceItem mServiceItem;
        Button mStopButton;
        RunningProcessesView.ViewHolder mViewHolder;

        ActiveDetail() {
        }

        void stopActiveService(boolean z) {
            RunningState.ServiceItem serviceItem = this.mServiceItem;
            if (!z && (serviceItem.mServiceInfo.applicationInfo.flags & 1) != 0) {
                RunningServiceDetails.this.showConfirmStopDialog(serviceItem.mRunningService.service);
                return;
            }
            RunningServiceDetails.this.getActivity().stopService(new Intent().setComponent(serviceItem.mRunningService.service));
            if (RunningServiceDetails.this.mMergedItem == null) {
                RunningServiceDetails.this.mState.updateNow();
                RunningServiceDetails.this.finish();
            } else if (!RunningServiceDetails.this.mShowBackground && RunningServiceDetails.this.mMergedItem.mServices.size() <= 1) {
                RunningServiceDetails.this.mState.updateNow();
                RunningServiceDetails.this.finish();
            } else {
                RunningServiceDetails.this.mState.updateNow();
            }
        }

        @Override
        public void onClick(View view) throws Throwable {
            FileOutputStream fileOutputStream;
            FileInputStream fileInputStream;
            if (view != this.mReportButton) {
                if (this.mManageIntent == null) {
                    if (this.mServiceItem != null) {
                        stopActiveService(false);
                        return;
                    } else if (this.mActiveItem.mItem.mBackground) {
                        RunningServiceDetails.this.mAm.killBackgroundProcesses(this.mActiveItem.mItem.mPackageInfo.packageName);
                        RunningServiceDetails.this.finish();
                        return;
                    } else {
                        RunningServiceDetails.this.mAm.forceStopPackage(this.mActiveItem.mItem.mPackageInfo.packageName);
                        RunningServiceDetails.this.finish();
                        return;
                    }
                }
                try {
                    RunningServiceDetails.this.getActivity().startIntentSender(this.mManageIntent.getIntentSender(), null, 268959744, 524288, 0);
                    return;
                } catch (ActivityNotFoundException e) {
                    Log.w("RunningServicesDetails", e);
                    return;
                } catch (IntentSender.SendIntentException e2) {
                    Log.w("RunningServicesDetails", e2);
                    return;
                } catch (IllegalArgumentException e3) {
                    Log.w("RunningServicesDetails", e3);
                    return;
                }
            }
            ApplicationErrorReport applicationErrorReport = new ApplicationErrorReport();
            applicationErrorReport.type = 5;
            applicationErrorReport.packageName = this.mServiceItem.mServiceInfo.packageName;
            applicationErrorReport.installerPackageName = this.mInstaller.getPackageName();
            applicationErrorReport.processName = this.mServiceItem.mRunningService.process;
            applicationErrorReport.time = System.currentTimeMillis();
            applicationErrorReport.systemApp = (this.mServiceItem.mServiceInfo.applicationInfo.flags & 1) != 0;
            ApplicationErrorReport.RunningServiceInfo runningServiceInfo = new ApplicationErrorReport.RunningServiceInfo();
            if (this.mActiveItem.mFirstRunTime >= 0) {
                runningServiceInfo.durationMillis = SystemClock.elapsedRealtime() - this.mActiveItem.mFirstRunTime;
            } else {
                runningServiceInfo.durationMillis = -1L;
            }
            ComponentName componentName = new ComponentName(this.mServiceItem.mServiceInfo.packageName, this.mServiceItem.mServiceInfo.name);
            File fileStreamPath = RunningServiceDetails.this.getActivity().getFileStreamPath("service_dump.txt");
            FileInputStream fileInputStream2 = null;
            try {
                try {
                    fileOutputStream = new FileOutputStream(fileStreamPath);
                    try {
                        Debug.dumpService("activity", fileOutputStream.getFD(), new String[]{"-a", "service", componentName.flattenToString()});
                    } catch (IOException e4) {
                        e = e4;
                        Log.w("RunningServicesDetails", "Can't dump service: " + componentName, e);
                        if (fileOutputStream != null) {
                        }
                        try {
                            try {
                                fileInputStream = new FileInputStream(fileStreamPath);
                                byte[] bArr = new byte[(int) fileStreamPath.length()];
                                fileInputStream.read(bArr);
                                runningServiceInfo.serviceDetails = new String(bArr);
                                fileInputStream.close();
                            } catch (Throwable th) {
                                th = th;
                            }
                        } catch (IOException e5) {
                            e = e5;
                        }
                        fileStreamPath.delete();
                        Log.i("RunningServicesDetails", "Details: " + runningServiceInfo.serviceDetails);
                        applicationErrorReport.runningServiceInfo = runningServiceInfo;
                        Intent intent = new Intent("android.intent.action.APP_ERROR");
                        intent.setComponent(this.mInstaller);
                        intent.putExtra("android.intent.extra.BUG_REPORT", applicationErrorReport);
                        intent.addFlags(268435456);
                        RunningServiceDetails.this.startActivity(intent);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e6) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e7) {
                e = e7;
                fileOutputStream = null;
            } catch (Throwable th3) {
                th = th3;
                fileOutputStream = null;
                if (fileOutputStream != null) {
                }
                throw th;
            }
            try {
                fileOutputStream.close();
            } catch (IOException e8) {
            }
            try {
                fileInputStream = new FileInputStream(fileStreamPath);
            } catch (IOException e9) {
            }
            try {
                byte[] bArr2 = new byte[(int) fileStreamPath.length()];
                fileInputStream.read(bArr2);
                runningServiceInfo.serviceDetails = new String(bArr2);
                fileInputStream.close();
            } catch (IOException e10) {
                e = e10;
                fileInputStream2 = fileInputStream;
                Log.w("RunningServicesDetails", "Can't read service dump: " + componentName, e);
                if (fileInputStream2 != null) {
                    fileInputStream2.close();
                }
                fileStreamPath.delete();
                Log.i("RunningServicesDetails", "Details: " + runningServiceInfo.serviceDetails);
                applicationErrorReport.runningServiceInfo = runningServiceInfo;
                Intent intent2 = new Intent("android.intent.action.APP_ERROR");
                intent2.setComponent(this.mInstaller);
                intent2.putExtra("android.intent.extra.BUG_REPORT", applicationErrorReport);
                intent2.addFlags(268435456);
                RunningServiceDetails.this.startActivity(intent2);
            } catch (Throwable th4) {
                th = th4;
                fileInputStream2 = fileInputStream;
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (IOException e11) {
                    }
                }
                throw th;
            }
            fileStreamPath.delete();
            Log.i("RunningServicesDetails", "Details: " + runningServiceInfo.serviceDetails);
            applicationErrorReport.runningServiceInfo = runningServiceInfo;
            Intent intent22 = new Intent("android.intent.action.APP_ERROR");
            intent22.setComponent(this.mInstaller);
            intent22.putExtra("android.intent.extra.BUG_REPORT", applicationErrorReport);
            intent22.addFlags(268435456);
            RunningServiceDetails.this.startActivity(intent22);
        }
    }

    boolean findMergedItem() {
        RunningState.MergedItem mergedItem;
        ArrayList<RunningState.MergedItem> currentBackgroundItems = this.mShowBackground ? this.mState.getCurrentBackgroundItems() : this.mState.getCurrentMergedItems();
        if (currentBackgroundItems != null) {
            for (int i = 0; i < currentBackgroundItems.size(); i++) {
                mergedItem = currentBackgroundItems.get(i);
                if (mergedItem.mUserId == this.mUserId && ((this.mUid < 0 || mergedItem.mProcess == null || mergedItem.mProcess.mUid == this.mUid) && (this.mProcessName == null || (mergedItem.mProcess != null && this.mProcessName.equals(mergedItem.mProcess.mProcessName))))) {
                    break;
                }
            }
            mergedItem = null;
        } else {
            mergedItem = null;
        }
        if (this.mMergedItem == mergedItem) {
            return false;
        }
        this.mMergedItem = mergedItem;
        return true;
    }

    void addServicesHeader() {
        if (this.mNumServices == 0) {
            this.mServicesHeader = (TextView) this.mInflater.inflate(R.layout.separator_label, this.mAllDetails, false);
            this.mServicesHeader.setText(R.string.runningservicedetails_services_title);
            this.mAllDetails.addView(this.mServicesHeader);
        }
        this.mNumServices++;
    }

    void addProcessesHeader() {
        if (this.mNumProcesses == 0) {
            this.mProcessesHeader = (TextView) this.mInflater.inflate(R.layout.separator_label, this.mAllDetails, false);
            this.mProcessesHeader.setText(R.string.runningservicedetails_processes_title);
            this.mAllDetails.addView(this.mProcessesHeader);
        }
        this.mNumProcesses++;
    }

    void addServiceDetailsView(RunningState.ServiceItem serviceItem, RunningState.MergedItem mergedItem, boolean z, boolean z2) {
        int i;
        if (z) {
            addServicesHeader();
        } else if (mergedItem.mUserId != UserHandle.myUserId()) {
            addProcessesHeader();
        }
        RunningState.BaseItem baseItem = serviceItem != null ? serviceItem : mergedItem;
        ActiveDetail activeDetail = new ActiveDetail();
        View viewInflate = this.mInflater.inflate(R.layout.running_service_details_service, this.mAllDetails, false);
        this.mAllDetails.addView(viewInflate);
        activeDetail.mRootView = viewInflate;
        activeDetail.mServiceItem = serviceItem;
        activeDetail.mViewHolder = new RunningProcessesView.ViewHolder(viewInflate);
        activeDetail.mActiveItem = activeDetail.mViewHolder.bind(this.mState, baseItem, this.mBuilder);
        if (!z2) {
            viewInflate.findViewById(R.id.service).setVisibility(8);
        }
        if (serviceItem != null && serviceItem.mRunningService.clientLabel != 0) {
            activeDetail.mManageIntent = this.mAm.getRunningServiceControlPanel(serviceItem.mRunningService.service);
        }
        TextView textView = (TextView) viewInflate.findViewById(R.id.comp_description);
        activeDetail.mStopButton = (Button) viewInflate.findViewById(R.id.left_button);
        activeDetail.mReportButton = (Button) viewInflate.findViewById(R.id.right_button);
        if (z && mergedItem.mUserId != UserHandle.myUserId()) {
            textView.setVisibility(8);
            viewInflate.findViewById(R.id.control_buttons_panel).setVisibility(8);
        } else {
            if (serviceItem != null && serviceItem.mServiceInfo.descriptionRes != 0) {
                textView.setText(getActivity().getPackageManager().getText(serviceItem.mServiceInfo.packageName, serviceItem.mServiceInfo.descriptionRes, serviceItem.mServiceInfo.applicationInfo));
            } else if (mergedItem.mBackground) {
                textView.setText(R.string.background_process_stop_description);
            } else if (activeDetail.mManageIntent != null) {
                try {
                    textView.setText(getActivity().getString(R.string.service_manage_description, new Object[]{getActivity().getPackageManager().getResourcesForApplication(serviceItem.mRunningService.clientPackage).getString(serviceItem.mRunningService.clientLabel)}));
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                Activity activity = getActivity();
                if (serviceItem != null) {
                    i = R.string.service_stop_description;
                } else {
                    i = R.string.heavy_weight_stop_description;
                }
                textView.setText(activity.getText(i));
            }
            activeDetail.mStopButton.setOnClickListener(activeDetail);
            activeDetail.mStopButton.setText(getActivity().getText(activeDetail.mManageIntent != null ? R.string.service_manage : R.string.service_stop));
            activeDetail.mReportButton.setOnClickListener(activeDetail);
            activeDetail.mReportButton.setText(android.R.string.kg_sim_pin_instructions);
            if (Settings.Global.getInt(getActivity().getContentResolver(), "send_action_app_error", 0) != 0 && serviceItem != null) {
                activeDetail.mInstaller = ApplicationErrorReport.getErrorReportReceiver(getActivity(), serviceItem.mServiceInfo.packageName, serviceItem.mServiceInfo.applicationInfo.flags);
                activeDetail.mReportButton.setEnabled(activeDetail.mInstaller != null);
            } else {
                activeDetail.mReportButton.setEnabled(false);
            }
        }
        this.mActiveDetails.add(activeDetail);
    }

    void addProcessDetailsView(RunningState.ProcessItem processItem, boolean z) {
        int i;
        CharSequence charSequenceMakeLabel;
        addProcessesHeader();
        ActiveDetail activeDetail = new ActiveDetail();
        View viewInflate = this.mInflater.inflate(R.layout.running_service_details_process, this.mAllDetails, false);
        this.mAllDetails.addView(viewInflate);
        activeDetail.mRootView = viewInflate;
        activeDetail.mViewHolder = new RunningProcessesView.ViewHolder(viewInflate);
        activeDetail.mActiveItem = activeDetail.mViewHolder.bind(this.mState, processItem, this.mBuilder);
        TextView textView = (TextView) viewInflate.findViewById(R.id.comp_description);
        if (processItem.mUserId != UserHandle.myUserId()) {
            textView.setVisibility(8);
        } else if (z) {
            textView.setText(R.string.main_running_process_description);
        } else {
            CharSequence charSequence = null;
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = processItem.mRunningProcessInfo;
            ComponentName componentName = runningAppProcessInfo.importanceReasonComponent;
            switch (runningAppProcessInfo.importanceReasonCode) {
                case 1:
                    i = R.string.process_provider_in_use_description;
                    if (runningAppProcessInfo.importanceReasonComponent != null) {
                        try {
                            ProviderInfo providerInfo = getActivity().getPackageManager().getProviderInfo(runningAppProcessInfo.importanceReasonComponent, 0);
                            charSequenceMakeLabel = RunningState.makeLabel(getActivity().getPackageManager(), providerInfo.name, providerInfo);
                            charSequence = charSequenceMakeLabel;
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                    break;
                case 2:
                    i = R.string.process_service_in_use_description;
                    if (runningAppProcessInfo.importanceReasonComponent != null) {
                        try {
                            ServiceInfo serviceInfo = getActivity().getPackageManager().getServiceInfo(runningAppProcessInfo.importanceReasonComponent, 0);
                            charSequenceMakeLabel = RunningState.makeLabel(getActivity().getPackageManager(), serviceInfo.name, serviceInfo);
                            charSequence = charSequenceMakeLabel;
                        } catch (PackageManager.NameNotFoundException e2) {
                        }
                    }
                    break;
                default:
                    i = 0;
                    break;
            }
            if (i != 0 && charSequence != null) {
                textView.setText(getActivity().getString(i, new Object[]{charSequence}));
            }
        }
        this.mActiveDetails.add(activeDetail);
    }

    void addDetailsViews(RunningState.MergedItem mergedItem, boolean z, boolean z2) {
        if (mergedItem != null) {
            if (z) {
                for (int i = 0; i < mergedItem.mServices.size(); i++) {
                    addServiceDetailsView(mergedItem.mServices.get(i), mergedItem, true, true);
                }
            }
            if (z2) {
                if (mergedItem.mServices.size() <= 0) {
                    addServiceDetailsView(null, mergedItem, false, mergedItem.mUserId != UserHandle.myUserId());
                    return;
                }
                int i2 = -1;
                while (i2 < mergedItem.mOtherProcesses.size()) {
                    RunningState.ProcessItem processItem = i2 < 0 ? mergedItem.mProcess : mergedItem.mOtherProcesses.get(i2);
                    if (processItem == null || processItem.mPid > 0) {
                        addProcessDetailsView(processItem, i2 < 0);
                    }
                    i2++;
                }
            }
        }
    }

    void addDetailViews() {
        ArrayList<RunningState.MergedItem> arrayList;
        for (int size = this.mActiveDetails.size() - 1; size >= 0; size--) {
            this.mAllDetails.removeView(this.mActiveDetails.get(size).mRootView);
        }
        this.mActiveDetails.clear();
        if (this.mServicesHeader != null) {
            this.mAllDetails.removeView(this.mServicesHeader);
            this.mServicesHeader = null;
        }
        if (this.mProcessesHeader != null) {
            this.mAllDetails.removeView(this.mProcessesHeader);
            this.mProcessesHeader = null;
        }
        this.mNumProcesses = 0;
        this.mNumServices = 0;
        if (this.mMergedItem != null) {
            if (this.mMergedItem.mUser != null) {
                if (this.mShowBackground) {
                    arrayList = new ArrayList<>(this.mMergedItem.mChildren);
                    Collections.sort(arrayList, this.mState.mBackgroundComparator);
                } else {
                    arrayList = this.mMergedItem.mChildren;
                }
                for (int i = 0; i < arrayList.size(); i++) {
                    addDetailsViews(arrayList.get(i), true, false);
                }
                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                    addDetailsViews(arrayList.get(i2), false, true);
                }
                return;
            }
            addDetailsViews(this.mMergedItem, true, true);
        }
    }

    void refreshUi(boolean z) {
        if (findMergedItem()) {
            z = true;
        }
        if (z) {
            if (this.mMergedItem != null) {
                this.mSnippetActiveItem = this.mSnippetViewHolder.bind(this.mState, this.mMergedItem, this.mBuilder);
            } else if (this.mSnippetActiveItem != null) {
                this.mSnippetActiveItem.mHolder.size.setText("");
                this.mSnippetActiveItem.mHolder.uptime.setText("");
                this.mSnippetActiveItem.mHolder.description.setText(R.string.no_services);
            } else {
                finish();
                return;
            }
            addDetailViews();
        }
    }

    public void finish() {
        ThreadUtils.postOnMainThread(new Runnable() {
            @Override
            public final void run() {
                RunningServiceDetails.lambda$finish$0(this.f$0);
            }
        });
    }

    public static void lambda$finish$0(RunningServiceDetails runningServiceDetails) {
        Activity activity = runningServiceDetails.getActivity();
        if (activity != null) {
            activity.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        this.mUid = getArguments().getInt("uid", -1);
        this.mUserId = getArguments().getInt("user_id", 0);
        this.mProcessName = getArguments().getString("process", null);
        this.mShowBackground = getArguments().getBoolean("background", false);
        this.mAm = (ActivityManager) getActivity().getSystemService("activity");
        this.mInflater = (LayoutInflater) getActivity().getSystemService("layout_inflater");
        this.mState = RunningState.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.running_service_details, viewGroup, false);
        Utils.prepareCustomPreferencesList(viewGroup, viewInflate, viewInflate, false);
        this.mRootView = viewInflate;
        this.mAllDetails = (ViewGroup) viewInflate.findViewById(R.id.all_details);
        this.mSnippet = (ViewGroup) viewInflate.findViewById(R.id.snippet);
        this.mSnippetViewHolder = new RunningProcessesView.ViewHolder(this.mSnippet);
        ensureData();
        return viewInflate;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mHaveData = false;
        this.mState.pause();
    }

    @Override
    public int getMetricsCategory() {
        return 85;
    }

    @Override
    public void onResume() {
        super.onResume();
        ensureData();
    }

    ActiveDetail activeDetailForService(ComponentName componentName) {
        for (int i = 0; i < this.mActiveDetails.size(); i++) {
            ActiveDetail activeDetail = this.mActiveDetails.get(i);
            if (activeDetail.mServiceItem != null && activeDetail.mServiceItem.mRunningService != null && componentName.equals(activeDetail.mServiceItem.mRunningService.service)) {
                return activeDetail;
            }
        }
        return null;
    }

    public void showConfirmStopDialog(ComponentName componentName) {
        MyAlertDialogFragment myAlertDialogFragmentNewConfirmStop = MyAlertDialogFragment.newConfirmStop(1, componentName);
        myAlertDialogFragmentNewConfirmStop.setTargetFragment(this, 0);
        myAlertDialogFragmentNewConfirmStop.show(getFragmentManager(), "confirmstop");
    }

    public static class MyAlertDialogFragment extends InstrumentedDialogFragment {
        public static MyAlertDialogFragment newConfirmStop(int i, ComponentName componentName) {
            MyAlertDialogFragment myAlertDialogFragment = new MyAlertDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("id", i);
            bundle.putParcelable("comp", componentName);
            myAlertDialogFragment.setArguments(bundle);
            return myAlertDialogFragment;
        }

        RunningServiceDetails getOwner() {
            return (RunningServiceDetails) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            int i = getArguments().getInt("id");
            if (i == 1) {
                final ComponentName componentName = (ComponentName) getArguments().getParcelable("comp");
                if (getOwner().activeDetailForService(componentName) == null) {
                    return null;
                }
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getString(R.string.runningservicedetails_stop_dlg_title)).setMessage(getActivity().getString(R.string.runningservicedetails_stop_dlg_text)).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        ActiveDetail activeDetailActiveDetailForService = MyAlertDialogFragment.this.getOwner().activeDetailForService(componentName);
                        if (activeDetailActiveDetailForService != null) {
                            activeDetailActiveDetailForService.stopActiveService(true);
                        }
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            }
            throw new IllegalArgumentException("unknown id " + i);
        }

        @Override
        public int getMetricsCategory() {
            return 536;
        }
    }

    void ensureData() {
        if (!this.mHaveData) {
            this.mHaveData = true;
            this.mState.resume(this);
            this.mState.waitForData();
            refreshUi(true);
        }
    }

    void updateTimes() {
        if (this.mSnippetActiveItem != null) {
            this.mSnippetActiveItem.updateTime(getActivity(), this.mBuilder);
        }
        for (int i = 0; i < this.mActiveDetails.size(); i++) {
            this.mActiveDetails.get(i).mActiveItem.updateTime(getActivity(), this.mBuilder);
        }
    }

    @Override
    public void onRefreshUi(int i) {
        if (getActivity() == null) {
        }
        switch (i) {
            case 0:
                updateTimes();
                break;
            case 1:
                refreshUi(false);
                updateTimes();
                break;
            case 2:
                refreshUi(true);
                updateTimes();
                break;
        }
    }
}
