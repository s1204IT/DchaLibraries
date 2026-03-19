package com.android.music;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RenamePlaylist extends DialogFragment {
    private Dialog mDialog;
    private long mExistingId;
    private String mOriginalName;
    private EditText mPlaylist;
    private String mPrompt;
    private long mRenameId;
    private Button mSaveButton;
    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            RenamePlaylist.this.setSaveButton();
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };
    private final BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RenamePlaylist.this.mDialog == null) {
                RenamePlaylist.this.mDialog.dismiss();
                RenamePlaylist.this.mDialog = null;
            }
            MusicLogUtils.v("RenamePlaylist", "SD card is ejected, finish RenamePlaylist activity!");
        }
    };

    public static RenamePlaylist newInstance(Boolean bool) {
        RenamePlaylist renamePlaylist = new RenamePlaylist();
        renamePlaylist.setArguments(new Bundle());
        return renamePlaylist;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        String string;
        MusicLogUtils.v("RenamePlaylist", "onCreate");
        this.mDialog = new Dialog(getActivity());
        this.mDialog.setContentView(R.layout.rename_playlist);
        this.mPlaylist = (EditText) this.mDialog.findViewById(R.id.playlist);
        Button button = (Button) this.mDialog.findViewById(R.id.rename_cancel);
        button.setText(getResources().getString(R.string.cancel));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RenamePlaylist.this.mDialog != null) {
                    RenamePlaylist.this.mDialog.dismiss();
                    RenamePlaylist.this.mDialog = null;
                }
            }
        });
        Button button2 = (Button) this.mDialog.findViewById(R.id.rename_done);
        button2.setText(getResources().getString(R.string.delete_confirm_button_text));
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string2 = RenamePlaylist.this.mPlaylist.getText().toString();
                if (string2 != null && string2.length() > 0) {
                    String strTrim = string2.trim();
                    ContentResolver contentResolver = RenamePlaylist.this.getActivity().getContentResolver();
                    if (RenamePlaylist.this.mExistingId >= 0) {
                        contentResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, "_id=?", new String[]{Long.valueOf(RenamePlaylist.this.mExistingId).toString()});
                        MusicLogUtils.v("RenamePlaylist", "to overwrite, delete the existing one");
                    }
                    String strDataForId = RenamePlaylist.this.dataForId(RenamePlaylist.this.mRenameId);
                    if (strDataForId == null) {
                        MusicLogUtils.v("RenamePlaylist", "oldData is null");
                        return;
                    }
                    String strReplace = strDataForId.replace(strDataForId.substring(strDataForId.lastIndexOf("/") + 1), strTrim);
                    MusicLogUtils.v("RenamePlaylist", "oldData:" + strDataForId + ",mRenameId:" + RenamePlaylist.this.mRenameId + ",name:" + strTrim + ",newData:" + strReplace);
                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put("name", strTrim);
                    contentValues.put("_data", strReplace);
                    contentResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues, "_id=?", new String[]{Long.valueOf(RenamePlaylist.this.mRenameId).toString()});
                    Toast.makeText(RenamePlaylist.this.getActivity().getApplicationContext(), R.string.playlist_renamed_message, 0).show();
                }
                if (RenamePlaylist.this.mDialog != null) {
                    RenamePlaylist.this.mDialog.dismiss();
                    RenamePlaylist.this.mDialog = null;
                }
            }
        });
        this.mDialog.setCanceledOnTouchOutside(true);
        this.mDialog.setCancelable(true);
        if (bundle == null) {
            this.mRenameId = MusicUtils.getLongPref(getActivity().getApplicationContext(), "rename", 1L);
        }
        this.mOriginalName = nameForId(this.mRenameId);
        String string2 = bundle != null ? bundle.getString("defaultname") : this.mOriginalName;
        if (this.mRenameId < 0 || this.mOriginalName == null || string2 == null) {
            MusicLogUtils.v("RenamePlaylist", "Rename failed: " + this.mRenameId + "/" + string2);
            this.mPlaylist.setText(string2);
            this.mPlaylist.setSelection(string2.length());
            this.mPlaylist.addTextChangedListener(this.mTextWatcher);
            return this.mDialog;
        }
        if (this.mOriginalName.equals(string2)) {
            string = getString(R.string.rename_playlist_same_prompt);
        } else {
            string = getString(R.string.rename_playlist_diff_prompt);
        }
        this.mPrompt = String.format(string, this.mOriginalName, string2);
        this.mPlaylist.setText(string2);
        this.mPlaylist.setSelection(string2.length());
        this.mPlaylist.addTextChangedListener(this.mTextWatcher);
        this.mDialog.setTitle(this.mPrompt);
        return this.mDialog;
    }

    private void setSaveButton() {
        String strTrim = this.mPlaylist.getText().toString().trim();
        MusicLogUtils.v("RenamePlaylist", "setSaveButton " + this.mSaveButton);
        if (this.mSaveButton == null) {
            if (this.mDialog == null) {
                return;
            } else {
                this.mSaveButton = (Button) this.mDialog.findViewById(R.id.rename_done);
            }
        }
        if (this.mSaveButton != null) {
            if (strTrim.length() == 0) {
                this.mSaveButton.setEnabled(false);
                return;
            }
            this.mSaveButton.setEnabled(true);
            long jIdForplaylist = MusicUtils.idForplaylist(getActivity().getApplicationContext(), strTrim);
            MusicLogUtils.v("RenamePlaylist", "id " + jIdForplaylist + ",mOriginalName:" + this.mOriginalName + ",typedname:" + strTrim);
            if (jIdForplaylist >= 0 && !this.mOriginalName.equals(strTrim) && !this.mOriginalName.equalsIgnoreCase(strTrim)) {
                this.mSaveButton.setText(R.string.create_playlist_overwrite_text);
                this.mExistingId = jIdForplaylist;
            } else {
                this.mSaveButton.setText(R.string.create_playlist_create_text);
                this.mExistingId = -1L;
            }
        }
    }

    private String nameForId(long j) {
        Cursor cursorQuery = MusicUtils.query(getActivity().getApplicationContext(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"name"}, "_id=?", new String[]{Long.valueOf(j).toString()}, "name");
        String string = null;
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            if (!cursorQuery.isAfterLast()) {
                string = cursorQuery.getString(0);
            }
            cursorQuery.close();
        }
        return string;
    }

    private String dataForId(long j) {
        Cursor cursorQuery = MusicUtils.query(getActivity().getApplicationContext(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"_data"}, "_id=?", new String[]{Long.valueOf(j).toString()}, "_data");
        String string = null;
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            if (!cursorQuery.isAfterLast()) {
                string = cursorQuery.getString(0);
            }
            cursorQuery.close();
        }
        return string;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("defaultname", this.mPlaylist.getText().toString());
        bundle.putLong("rename", this.mRenameId);
        bundle.putLong("existing", this.mExistingId);
    }

    @Override
    public void onResume() {
        super.onResume();
        setSaveButton();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
