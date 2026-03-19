package com.android.contacts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TimingLogger;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.database.SimContactDao;
import com.android.contacts.model.SimCard;
import com.android.contacts.model.SimContact;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.ContactsNotificationChannelsUtil;
import com.android.contactsbind.FeedbackHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimImportService extends Service {
    public static final String EXTRA_ACCOUNT = "account";
    public static final String EXTRA_OPERATION_REQUESTED_AT_TIME = "requestedTime";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_COUNT = "count";
    public static final String EXTRA_SIM_CONTACTS = "simContacts";
    public static final String EXTRA_SIM_SUBSCRIPTION_ID = "simSubscriptionId";
    private static final int NOTIFICATION_ID = 100;
    public static final int RESULT_FAILURE = 2;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_UNKNOWN = 0;
    private static final String TAG = "SimImportService";
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public static final String BROADCAST_SERVICE_STATE_CHANGED = SimImportService.class.getName() + "#serviceStateChanged";
    public static final String BROADCAST_SIM_IMPORT_COMPLETE = SimImportService.class.getName() + "#simImportComplete";
    private static List<ImportTask> sPending = new ArrayList();
    private static StatusProvider sStatusProvider = new StatusProvider() {
        @Override
        public boolean isRunning() {
            return !SimImportService.sPending.isEmpty();
        }

        @Override
        public boolean isImporting(SimCard simCard) {
            return SimImportService.isImporting(simCard);
        }
    };

    public interface StatusProvider {
        boolean isImporting(SimCard simCard);

        boolean isRunning();
    }

    private static boolean isImporting(SimCard simCard) {
        Iterator<ImportTask> it = sPending.iterator();
        while (it.hasNext()) {
            if (it.next().getSim().equals(simCard)) {
                return true;
            }
        }
        return false;
    }

    public static StatusProvider getStatusProvider() {
        return sStatusProvider;
    }

    public static void startImport(Context context, int i, ArrayList<SimContact> arrayList, AccountWithDataSet accountWithDataSet) {
        context.startService(new Intent(context, (Class<?>) SimImportService.class).putExtra(EXTRA_SIM_CONTACTS, arrayList).putExtra(EXTRA_SIM_SUBSCRIPTION_ID, i).putExtra("account", accountWithDataSet));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        ContactsNotificationChannelsUtil.createDefaultChannel(this);
        ImportTask importTaskCreateTaskForIntent = createTaskForIntent(intent, i2);
        if (importTaskCreateTaskForIntent == null) {
            new StopTask(this, i2).executeOnExecutor(this.mExecutor, new Void[0]);
            return 2;
        }
        sPending.add(importTaskCreateTaskForIntent);
        importTaskCreateTaskForIntent.executeOnExecutor(this.mExecutor, new Void[0]);
        notifyStateChanged();
        return 3;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mExecutor.shutdown();
    }

    private ImportTask createTaskForIntent(Intent intent, int i) {
        AccountWithDataSet accountWithDataSet = (AccountWithDataSet) intent.getParcelableExtra("account");
        ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra(EXTRA_SIM_CONTACTS);
        int intExtra = intent.getIntExtra(EXTRA_SIM_SUBSCRIPTION_ID, -1);
        SimContactDao simContactDaoCreate = SimContactDao.create(this);
        SimCard simBySubscriptionId = simContactDaoCreate.getSimBySubscriptionId(intExtra);
        if (simBySubscriptionId != null) {
            return new ImportTask(simBySubscriptionId, parcelableArrayListExtra, accountWithDataSet, simContactDaoCreate, i);
        }
        return null;
    }

    private Notification getCompletedNotification() {
        Intent intent = new Intent(this, (Class<?>) PeopleActivity.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL);
        builder.setOngoing(false).setAutoCancel(true).setContentTitle(getString(R.string.importing_sim_finished_title)).setColor(getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(R.drawable.quantum_ic_done_vd_theme_24).setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        return builder.build();
    }

    private Notification getFailedNotification() {
        Intent intent = new Intent(this, (Class<?>) PeopleActivity.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL);
        builder.setOngoing(false).setAutoCancel(true).setContentTitle(getString(R.string.importing_sim_failed_title)).setContentText(getString(R.string.importing_sim_failed_message)).setColor(getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(R.drawable.quantum_ic_error_vd_theme_24).setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        return builder.build();
    }

    private Notification getImportingNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL);
        builder.setOngoing(true).setProgress(0, 100, true).setContentTitle(getString(R.string.importing_sim_in_progress_title)).setColor(getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(android.R.drawable.stat_sys_download);
        return builder.build();
    }

    private void notifyStateChanged() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_SERVICE_STATE_CHANGED));
    }

    private static class StopTask extends AsyncTask<Void, Void, Void> {
        private Service mHost;
        private final int mStartId;

        private StopTask(Service service, int i) {
            this.mHost = service;
            this.mStartId = i;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            return null;
        }

        @Override
        protected void onPostExecute(Void r2) {
            super.onPostExecute(r2);
            this.mHost.stopSelf(this.mStartId);
        }
    }

    private class ImportTask extends AsyncTask<Void, Void, Boolean> {
        private final List<SimContact> mContacts;
        private final SimContactDao mDao;
        private final NotificationManager mNotificationManager;
        private final SimCard mSim;
        private final int mStartId;
        private final long mStartTime = System.currentTimeMillis();
        private final AccountWithDataSet mTargetAccount;

        public ImportTask(SimCard simCard, List<SimContact> list, AccountWithDataSet accountWithDataSet, SimContactDao simContactDao, int i) {
            this.mSim = simCard;
            this.mContacts = list;
            this.mTargetAccount = accountWithDataSet;
            this.mDao = simContactDao;
            this.mNotificationManager = (NotificationManager) SimImportService.this.getSystemService("notification");
            this.mStartId = i;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SimImportService.this.startForeground(100, SimImportService.this.getImportingNotification());
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            TimingLogger timingLogger = new TimingLogger(SimImportService.TAG, "import");
            try {
                this.mDao.importContacts(this.mContacts, this.mTargetAccount);
                this.mDao.persistSimState(this.mSim.withImportedState(true));
                timingLogger.addSplit("done");
                timingLogger.dumpToLog();
                return true;
            } catch (OperationApplicationException | RemoteException e) {
                FeedbackHelper.sendFeedback(SimImportService.this, SimImportService.TAG, "Failed to import contacts from SIM card", e);
                return false;
            }
        }

        public SimCard getSim() {
            return this.mSim;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            Intent intentPutExtra;
            Notification failedNotification;
            super.onPostExecute(bool);
            SimImportService.this.stopSelf(this.mStartId);
            if (bool.booleanValue()) {
                intentPutExtra = new Intent(SimImportService.BROADCAST_SIM_IMPORT_COMPLETE).putExtra("resultCode", 1).putExtra("count", this.mContacts.size()).putExtra(SimImportService.EXTRA_OPERATION_REQUESTED_AT_TIME, this.mStartTime).putExtra(SimImportService.EXTRA_SIM_SUBSCRIPTION_ID, this.mSim.getSubscriptionId());
                failedNotification = SimImportService.this.getCompletedNotification();
            } else {
                intentPutExtra = new Intent(SimImportService.BROADCAST_SIM_IMPORT_COMPLETE).putExtra("resultCode", 2).putExtra(SimImportService.EXTRA_OPERATION_REQUESTED_AT_TIME, this.mStartTime).putExtra(SimImportService.EXTRA_SIM_SUBSCRIPTION_ID, this.mSim.getSubscriptionId());
                failedNotification = SimImportService.this.getFailedNotification();
            }
            LocalBroadcastManager.getInstance(SimImportService.this).sendBroadcast(intentPutExtra);
            SimImportService.sPending.remove(this);
            if (SimImportService.sPending.isEmpty()) {
                SimImportService.this.stopForeground(false);
                this.mNotificationManager.notify(100, failedNotification);
            }
            SimImportService.this.notifyStateChanged();
        }
    }
}
