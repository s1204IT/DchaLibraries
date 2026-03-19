package com.android.deskclock.ringtone;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.DropShadowController;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.RingtonePreviewKlaxon;
import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NavUpMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.alarms.AlarmUpdateHandler;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.ringtone.AddCustomRingtoneViewHolder;
import com.android.deskclock.ringtone.HeaderViewHolder;
import com.android.deskclock.ringtone.RingtoneViewHolder;
import java.util.List;

public class RingtonePickerActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<ItemAdapter.ItemHolder<Uri>>> {
    private static final String EXTRA_ALARM_ID = "extra_alarm_id";
    private static final String EXTRA_DEFAULT_RINGTONE_NAME = "extra_default_ringtone_name";
    private static final String EXTRA_DEFAULT_RINGTONE_URI = "extra_default_ringtone_uri";
    private static final String EXTRA_DRM_LEVEL = "android.intent.extra.drm_level";
    private static final String EXTRA_RINGTONE_URI = "extra_ringtone_uri";
    private static final String EXTRA_TITLE = "extra_title";
    private static final int LEVEL_FL = 1;
    private static final String STATE_KEY_PLAYING = "extra_is_playing";
    private long mAlarmId;
    private String mDefaultRingtoneTitle;
    private Uri mDefaultRingtoneUri;
    private DropShadowController mDropShadowController;
    private int mIndexOfRingtoneToRemove = -1;
    private boolean mIsPlaying;
    private OptionsMenuManager mOptionsMenuManager;
    private RecyclerView mRecyclerView;
    private ItemAdapter<ItemAdapter.ItemHolder<Uri>> mRingtoneAdapter;
    private Uri mSelectedRingtoneUri;

    public static Intent createAlarmRingtonePickerIntent(Context context, Alarm alarm) {
        return new Intent(context, (Class<?>) RingtonePickerActivity.class).putExtra(EXTRA_TITLE, R.string.alarm_sound).putExtra(EXTRA_ALARM_ID, alarm.id).putExtra(EXTRA_RINGTONE_URI, alarm.alert).putExtra(EXTRA_DEFAULT_RINGTONE_URI, RingtoneManager.getDefaultUri(4)).putExtra(EXTRA_DEFAULT_RINGTONE_NAME, R.string.default_alarm_ringtone_title);
    }

    public static Intent createTimerRingtonePickerIntent(Context context) {
        DataModel dataModel = DataModel.getDataModel();
        return new Intent(context, (Class<?>) RingtonePickerActivity.class).putExtra(EXTRA_TITLE, R.string.timer_sound).putExtra(EXTRA_RINGTONE_URI, dataModel.getTimerRingtoneUri()).putExtra(EXTRA_DEFAULT_RINGTONE_URI, dataModel.getDefaultTimerRingtoneUri()).putExtra(EXTRA_DEFAULT_RINGTONE_NAME, R.string.default_timer_ringtone_title);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.ringtone_picker);
        setVolumeControlStream(4);
        this.mOptionsMenuManager = new OptionsMenuManager();
        this.mOptionsMenuManager.addMenuItemController(new NavUpMenuItemController(this)).addMenuItemController(MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));
        Context applicationContext = getApplicationContext();
        Intent intent = getIntent();
        if (bundle != null) {
            this.mIsPlaying = bundle.getBoolean(STATE_KEY_PLAYING);
            this.mSelectedRingtoneUri = (Uri) bundle.getParcelable(EXTRA_RINGTONE_URI);
        }
        if (this.mSelectedRingtoneUri == null) {
            this.mSelectedRingtoneUri = (Uri) intent.getParcelableExtra(EXTRA_RINGTONE_URI);
        }
        this.mAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L);
        this.mDefaultRingtoneUri = (Uri) intent.getParcelableExtra(EXTRA_DEFAULT_RINGTONE_URI);
        this.mDefaultRingtoneTitle = applicationContext.getString(intent.getIntExtra(EXTRA_DEFAULT_RINGTONE_NAME, 0));
        LayoutInflater layoutInflater = getLayoutInflater();
        ItemClickWatcher itemClickWatcher = new ItemClickWatcher();
        RingtoneViewHolder.Factory factory = new RingtoneViewHolder.Factory(layoutInflater);
        HeaderViewHolder.Factory factory2 = new HeaderViewHolder.Factory(layoutInflater);
        AddCustomRingtoneViewHolder.Factory factory3 = new AddCustomRingtoneViewHolder.Factory(layoutInflater);
        this.mRingtoneAdapter = new ItemAdapter<>();
        this.mRingtoneAdapter.withViewTypes(factory2, null, R.layout.ringtone_item_header).withViewTypes(factory3, itemClickWatcher, Integer.MIN_VALUE).withViewTypes(factory, itemClickWatcher, R.layout.ringtone_item_sound).withViewTypes(factory, itemClickWatcher, -2131558500);
        this.mRecyclerView = (RecyclerView) findViewById(R.id.ringtone_content);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(applicationContext));
        this.mRecyclerView.setAdapter(this.mRingtoneAdapter);
        this.mRecyclerView.setItemAnimator(null);
        this.mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int i) {
                if (RingtonePickerActivity.this.mIndexOfRingtoneToRemove != -1) {
                    RingtonePickerActivity.this.closeContextMenu();
                }
            }
        });
        setTitle(applicationContext.getString(intent.getIntExtra(EXTRA_TITLE, 0)));
        getLoaderManager().initLoader(0, null, this);
        registerForContextMenu(this.mRecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDropShadowController = new DropShadowController(findViewById(R.id.drop_shadow), this.mRecyclerView);
    }

    @Override
    protected void onPause() {
        this.mDropShadowController.stop();
        this.mDropShadowController = null;
        if (this.mSelectedRingtoneUri != null) {
            if (this.mAlarmId != -1) {
                final Context applicationContext = getApplicationContext();
                final ContentResolver contentResolver = getContentResolver();
                new AsyncTask<Void, Void, Alarm>() {
                    @Override
                    protected Alarm doInBackground(Void... voidArr) throws Exception {
                        Alarm alarm = Alarm.getAlarm(contentResolver, RingtonePickerActivity.this.mAlarmId);
                        if (alarm != null) {
                            alarm.alert = RingtonePickerActivity.this.mSelectedRingtoneUri;
                        }
                        return alarm;
                    }

                    @Override
                    protected void onPostExecute(Alarm alarm) {
                        if (alarm != null) {
                            DataModel.getDataModel().setDefaultAlarmRingtoneUri(alarm.alert);
                            new AlarmUpdateHandler(applicationContext, null, null).asyncUpdateAlarm(alarm, false, true);
                        }
                    }
                }.execute(new Void[0]);
            } else {
                DataModel.getDataModel().setTimerRingtoneUri(this.mSelectedRingtoneUri);
            }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) {
            stopPlayingRingtone(getSelectedRingtoneHolder(), false);
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(STATE_KEY_PLAYING, this.mIsPlaying);
        bundle.putParcelable(EXTRA_RINGTONE_URI, this.mSelectedRingtoneUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return this.mOptionsMenuManager.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    @Override
    public Loader<List<ItemAdapter.ItemHolder<Uri>>> onCreateLoader(int i, Bundle bundle) {
        return new RingtoneLoader(getApplicationContext(), this.mDefaultRingtoneUri, this.mDefaultRingtoneTitle);
    }

    @Override
    public void onLoadFinished(Loader<List<ItemAdapter.ItemHolder<Uri>>> loader, List<ItemAdapter.ItemHolder<Uri>> list) {
        this.mRingtoneAdapter.setItems(list);
        RingtoneHolder ringtoneHolder = getRingtoneHolder(this.mSelectedRingtoneUri);
        if (ringtoneHolder != null) {
            ringtoneHolder.setSelected(true);
            this.mSelectedRingtoneUri = ringtoneHolder.getUri();
            ringtoneHolder.notifyItemChanged();
            if (this.mIsPlaying) {
                startPlayingRingtone(ringtoneHolder);
                return;
            }
            return;
        }
        RingtonePreviewKlaxon.stop(this);
        this.mSelectedRingtoneUri = null;
        this.mIsPlaying = false;
    }

    @Override
    public void onLoaderReset(Loader<List<ItemAdapter.ItemHolder<Uri>>> loader) {
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        Uri data;
        if (i2 != -1) {
            return;
        }
        if (intent != null) {
            data = intent.getData();
        } else {
            data = null;
        }
        if (data == null || (intent.getFlags() & 1) != 1) {
            return;
        }
        new AddCustomRingtoneTask(data).execute(new Void[0]);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        RingtoneHolder ringtoneHolder = (RingtoneHolder) this.mRingtoneAdapter.getItems().get(this.mIndexOfRingtoneToRemove);
        this.mIndexOfRingtoneToRemove = -1;
        ConfirmRemoveCustomRingtoneDialogFragment.show(getFragmentManager(), ringtoneHolder.getUri(), ringtoneHolder.hasPermissions());
        return true;
    }

    private RingtoneHolder getRingtoneHolder(Uri uri) {
        if (this.mRingtoneAdapter != null && this.mRingtoneAdapter.getItemCount() > 0) {
            for (T t : this.mRingtoneAdapter.getItems()) {
                if (t instanceof RingtoneHolder) {
                    RingtoneHolder ringtoneHolder = (RingtoneHolder) t;
                    if (ringtoneHolder.getUri().equals(uri)) {
                        return ringtoneHolder;
                    }
                }
            }
            return null;
        }
        return null;
    }

    @VisibleForTesting(otherwise = 2)
    RingtoneHolder getSelectedRingtoneHolder() {
        return getRingtoneHolder(this.mSelectedRingtoneUri);
    }

    private void startPlayingRingtone(RingtoneHolder ringtoneHolder) {
        if (!ringtoneHolder.isPlaying() && !ringtoneHolder.isSilent()) {
            RingtonePreviewKlaxon.start(getApplicationContext(), ringtoneHolder.getUri());
            ringtoneHolder.setPlaying(true);
            this.mIsPlaying = true;
        }
        if (!ringtoneHolder.isSelected()) {
            ringtoneHolder.setSelected(true);
            this.mSelectedRingtoneUri = ringtoneHolder.getUri();
        }
        ringtoneHolder.notifyItemChanged();
    }

    private void stopPlayingRingtone(RingtoneHolder ringtoneHolder, boolean z) {
        if (ringtoneHolder == null) {
            return;
        }
        if (ringtoneHolder.isPlaying()) {
            RingtonePreviewKlaxon.stop(this);
            ringtoneHolder.setPlaying(false);
            this.mIsPlaying = false;
        }
        if (z && ringtoneHolder.isSelected()) {
            ringtoneHolder.setSelected(false);
            this.mSelectedRingtoneUri = null;
        }
        ringtoneHolder.notifyItemChanged();
    }

    private void removeCustomRingtone(Uri uri) {
        new RemoveCustomRingtoneTask(uri).execute(new Void[0]);
    }

    public static class ConfirmRemoveCustomRingtoneDialogFragment extends DialogFragment {
        private static final String ARG_RINGTONE_HAS_PERMISSIONS = "arg_ringtone_has_permissions";
        private static final String ARG_RINGTONE_URI_TO_REMOVE = "arg_ringtone_uri_to_remove";

        static void show(FragmentManager fragmentManager, Uri uri, boolean z) {
            if (fragmentManager.isDestroyed()) {
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable(ARG_RINGTONE_URI_TO_REMOVE, uri);
            bundle.putBoolean(ARG_RINGTONE_HAS_PERMISSIONS, z);
            ConfirmRemoveCustomRingtoneDialogFragment confirmRemoveCustomRingtoneDialogFragment = new ConfirmRemoveCustomRingtoneDialogFragment();
            confirmRemoveCustomRingtoneDialogFragment.setArguments(bundle);
            confirmRemoveCustomRingtoneDialogFragment.setCancelable(z);
            confirmRemoveCustomRingtoneDialogFragment.show(fragmentManager, "confirm_ringtone_remove");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Bundle arguments = getArguments();
            final Uri uri = (Uri) arguments.getParcelable(ARG_RINGTONE_URI_TO_REMOVE);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((RingtonePickerActivity) ConfirmRemoveCustomRingtoneDialogFragment.this.getActivity()).removeCustomRingtone(uri);
                }
            };
            if (arguments.getBoolean(ARG_RINGTONE_HAS_PERMISSIONS)) {
                return new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.remove_sound, onClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setMessage(R.string.confirm_remove_custom_ringtone).create();
            }
            return new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.remove_sound, onClickListener).setMessage(R.string.custom_ringtone_lost_permissions).create();
        }
    }

    private class ItemClickWatcher implements ItemAdapter.OnItemClickedListener {
        private ItemClickWatcher() {
        }

        @Override
        public void onItemClicked(ItemAdapter.ItemViewHolder<?> itemViewHolder, int i) {
            if (i == Integer.MIN_VALUE) {
                RingtonePickerActivity.this.stopPlayingRingtone(RingtonePickerActivity.this.getSelectedRingtoneHolder(), false);
                RingtonePickerActivity.this.startActivityForResult(new Intent("android.intent.action.OPEN_DOCUMENT").addFlags(64).addCategory("android.intent.category.OPENABLE").putExtra(RingtonePickerActivity.EXTRA_DRM_LEVEL, 1).setType("audio/*"), 0);
            }
            switch (i) {
                case -2:
                    ConfirmRemoveCustomRingtoneDialogFragment.show(RingtonePickerActivity.this.getFragmentManager(), ((RingtoneHolder) itemViewHolder.getItemHolder()).getUri(), false);
                    break;
                case -1:
                    RingtonePickerActivity.this.mIndexOfRingtoneToRemove = itemViewHolder.getAdapterPosition();
                    break;
                case 0:
                    RingtoneHolder selectedRingtoneHolder = RingtonePickerActivity.this.getSelectedRingtoneHolder();
                    RingtoneHolder ringtoneHolder = (RingtoneHolder) itemViewHolder.getItemHolder();
                    if (selectedRingtoneHolder != ringtoneHolder) {
                        RingtonePickerActivity.this.stopPlayingRingtone(selectedRingtoneHolder, true);
                        RingtonePickerActivity.this.startPlayingRingtone(ringtoneHolder);
                    } else if (ringtoneHolder.isPlaying()) {
                        RingtonePickerActivity.this.stopPlayingRingtone(ringtoneHolder, false);
                    } else {
                        RingtonePickerActivity.this.startPlayingRingtone(ringtoneHolder);
                    }
                    break;
            }
        }
    }

    private final class AddCustomRingtoneTask extends AsyncTask<Void, Void, String> {
        private final Context mContext;
        private final Uri mUri;

        private AddCustomRingtoneTask(Uri uri) {
            this.mUri = uri;
            this.mContext = RingtonePickerActivity.this.getApplicationContext();
        }

        @Override
        protected String doInBackground(Void... voidArr) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            contentResolver.takePersistableUriPermission(this.mUri, 1);
            try {
                Cursor cursorQuery = contentResolver.query(this.mUri, null, null, null, null);
                try {
                    if (cursorQuery != null) {
                        if (cursorQuery.moveToFirst()) {
                            int columnIndex = cursorQuery.getColumnIndex("title");
                            if (columnIndex != -1) {
                                String string = cursorQuery.getString(columnIndex);
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return string;
                            }
                            int columnIndex2 = cursorQuery.getColumnIndex("_display_name");
                            if (columnIndex2 != -1) {
                                String string2 = cursorQuery.getString(columnIndex2);
                                int iLastIndexOf = string2.lastIndexOf(".");
                                if (iLastIndexOf > 0) {
                                    string2 = string2.substring(0, iLastIndexOf);
                                }
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return string2;
                            }
                        } else {
                            LogUtils.e("No ringtone for uri: %s", this.mUri);
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    }
                } finally {
                }
            } catch (Exception e) {
                LogUtils.e("Unable to locate title for custom ringtone: " + this.mUri, e);
            }
            return this.mContext.getString(R.string.unknown_ringtone_title);
        }

        @Override
        protected void onPostExecute(String str) {
            DataModel.getDataModel().addCustomRingtone(this.mUri, str);
            RingtonePickerActivity.this.mSelectedRingtoneUri = this.mUri;
            RingtonePickerActivity.this.mIsPlaying = true;
            RingtonePickerActivity.this.getLoaderManager().restartLoader(0, null, RingtonePickerActivity.this);
        }
    }

    private final class RemoveCustomRingtoneTask extends AsyncTask<Void, Void, Void> {
        private final Uri mRemoveUri;
        private Uri mSystemDefaultRingtoneUri;

        private RemoveCustomRingtoneTask(Uri uri) {
            this.mRemoveUri = uri;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            this.mSystemDefaultRingtoneUri = RingtoneManager.getDefaultUri(4);
            ContentResolver contentResolver = RingtonePickerActivity.this.getContentResolver();
            for (Alarm alarm : Alarm.getAlarms(contentResolver, null, new String[0])) {
                if (this.mRemoveUri.equals(alarm.alert)) {
                    alarm.alert = this.mSystemDefaultRingtoneUri;
                    new AlarmUpdateHandler(RingtonePickerActivity.this, null, null).asyncUpdateAlarm(alarm, false, true);
                }
            }
            try {
                contentResolver.releasePersistableUriPermission(this.mRemoveUri, 1);
            } catch (SecurityException e) {
                LogUtils.w("SecurityException while releasing read permission for " + this.mRemoveUri, new Object[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r4) {
            if (this.mRemoveUri.equals(DataModel.getDataModel().getDefaultAlarmRingtoneUri())) {
                DataModel.getDataModel().setDefaultAlarmRingtoneUri(this.mSystemDefaultRingtoneUri);
            }
            if (this.mRemoveUri.equals(DataModel.getDataModel().getTimerRingtoneUri())) {
                DataModel.getDataModel().setTimerRingtoneUri(DataModel.getDataModel().getDefaultTimerRingtoneUri());
            }
            DataModel.getDataModel().removeCustomRingtone(this.mRemoveUri);
            RingtoneHolder ringtoneHolder = RingtonePickerActivity.this.getRingtoneHolder(this.mRemoveUri);
            if (ringtoneHolder == null) {
                return;
            }
            if (ringtoneHolder.isSelected()) {
                RingtonePickerActivity.this.stopPlayingRingtone(ringtoneHolder, false);
                RingtoneHolder ringtoneHolder2 = RingtonePickerActivity.this.getRingtoneHolder(RingtonePickerActivity.this.mDefaultRingtoneUri);
                if (ringtoneHolder2 != null) {
                    ringtoneHolder2.setSelected(true);
                    RingtonePickerActivity.this.mSelectedRingtoneUri = ringtoneHolder2.getUri();
                    ringtoneHolder2.notifyItemChanged();
                }
            }
            RingtonePickerActivity.this.mRingtoneAdapter.removeItem(ringtoneHolder);
        }
    }
}
