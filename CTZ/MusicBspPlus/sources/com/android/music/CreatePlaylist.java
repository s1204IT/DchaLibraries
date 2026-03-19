package com.android.music;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CreatePlaylist extends DialogFragment {
    private static long[] sTempCursor;
    private Dialog mDialog;
    private EditText mPlaylist;
    private String mPrompt;
    private Button mSaveButton;
    private String mSelectItemId = null;
    private int mStartActivityTab = -1;
    private Intent mIntent = null;
    private String mPlaylistFlag = "";
    private String mPlaylistName = null;
    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            CreatePlaylist.this.setSaveButton();
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CreatePlaylist.this.mDialog != null) {
                CreatePlaylist.this.mDialog.dismiss();
                CreatePlaylist.this.mDialog = null;
            }
            MusicLogUtils.d("CreatePlaylist", "SD card is ejected, finish CreatePlaylist activity!");
        }
    };

    public static CreatePlaylist newInstance(String str, int i, String str2, String str3) {
        CreatePlaylist createPlaylist = new CreatePlaylist();
        Bundle bundle = new Bundle();
        bundle.putString("add_to_playlist_item_id", str);
        bundle.putInt("start_activity_tab_id", i);
        bundle.putString("SAVE_PLAYLIST_FLAG", str2);
        bundle.putString("playlist_name", str3);
        createPlaylist.setArguments(bundle);
        return createPlaylist;
    }

    public static void setCursor(long[] jArr) {
        sTempCursor = jArr;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        MusicLogUtils.d("CreatePlaylist", "onCreate");
        this.mDialog = new Dialog(getActivity());
        this.mDialog.setContentView(R.layout.rename_playlist);
        this.mPlaylist = (EditText) this.mDialog.findViewById(R.id.playlist);
        Button button = (Button) this.mDialog.findViewById(R.id.rename_cancel);
        button.setText(getResources().getString(R.string.cancel));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CreatePlaylist.this.mDialog != null) {
                    CreatePlaylist.this.mDialog.dismiss();
                    CreatePlaylist.this.mDialog = null;
                }
            }
        });
        Button button2 = (Button) this.mDialog.findViewById(R.id.rename_done);
        button2.setText(getResources().getString(R.string.delete_confirm_button_text));
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreatePlaylist.this.savePlaylist();
                if (CreatePlaylist.this.mDialog != null) {
                    CreatePlaylist.this.mDialog.dismiss();
                    CreatePlaylist.this.mDialog = null;
                }
            }
        });
        String string = bundle != null ? bundle.getString("defaultname") : MusicUtils.makePlaylistName(getActivity().getApplicationContext(), getString(R.string.new_playlist_name_template));
        if (string == null) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.save_playlist_error), 0).show();
            if (this.mDialog != null) {
                this.mDialog.dismiss();
                this.mDialog = null;
            }
            return this.mDialog;
        }
        this.mPrompt = String.format(getString(R.string.create_playlist_create_text_prompt), string);
        this.mPlaylist.setText(string);
        this.mPlaylist.setSelection(string.length());
        this.mPlaylist.addTextChangedListener(this.mTextWatcher);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addDataScheme("file");
        this.mIntent = getActivity().registerReceiver(this.mScanListener, intentFilter);
        this.mSelectItemId = getArguments().getString("add_to_playlist_item_id");
        this.mStartActivityTab = getArguments().getInt("start_activity_tab_id");
        this.mPlaylistFlag = getArguments().getString("SAVE_PLAYLIST_FLAG");
        this.mPlaylistName = getArguments().getString("playlist_name");
        this.mDialog.setTitle(this.mPrompt);
        return this.mDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("defaultname", this.mPlaylist.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        setSaveButton();
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(this.mScanListener);
        super.onDestroy();
    }

    private void setSaveButton() {
        String string = this.mPlaylist.getText().toString();
        if (this.mDialog == null) {
            MusicLogUtils.v("CreatePlaylist", "setSaveButton with dialog is null return!");
            return;
        }
        if (this.mSaveButton == null) {
            this.mSaveButton = (Button) this.mDialog.findViewById(R.id.rename_done);
        }
        if (this.mSaveButton != null) {
            if (string.trim().length() == 0) {
                this.mSaveButton.setEnabled(false);
            } else {
                this.mSaveButton.setEnabled(true);
                if (MusicUtils.idForplaylist(getActivity().getApplicationContext(), string.trim()) >= 0) {
                    this.mSaveButton.setText(R.string.create_playlist_overwrite_text);
                } else {
                    this.mSaveButton.setText(R.string.create_playlist_create_text);
                }
            }
        }
        MusicLogUtils.d("CreatePlaylist", "setSaveButton " + this.mSaveButton);
    }

    public void savePlaylist() {
        Uri uriInsert;
        Uri uriWithAppendedId;
        if (getActivity().getApplicationContext().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            Toast.makeText(getActivity().getApplicationContext(), R.string.music_storage_permission_deny, 0).show();
            return;
        }
        String string = this.mPlaylist.getText().toString();
        if (string != null && string.length() > 0) {
            String strTrim = string.trim();
            Intent intent = new Intent();
            ContentResolver contentResolver = getActivity().getContentResolver();
            int iIdForplaylist = MusicUtils.idForplaylist(getActivity().getApplicationContext(), strTrim);
            long[] songListForAlbum = null;
            if (iIdForplaylist >= 0 && this.mPlaylistFlag.equals("new_playlist")) {
                long j = iIdForplaylist;
                uriInsert = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, j);
                if (MusicUtils.clearPlaylist(getActivity().getApplicationContext(), iIdForplaylist) == -1) {
                    if (this.mDialog != null) {
                        this.mDialog.dismiss();
                        this.mDialog = null;
                        return;
                    }
                    return;
                }
                ContentValues contentValues = new ContentValues(1);
                contentValues.put("name", strTrim);
                contentResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues, "_id=?", new String[]{Long.valueOf(j).toString()});
            } else if (iIdForplaylist >= 0 && this.mPlaylistFlag.equals("save_as_playlist")) {
                if (!strTrim.equals(this.mPlaylistName) && strTrim.equalsIgnoreCase(this.mPlaylistName)) {
                    ContentValues contentValues2 = new ContentValues(1);
                    contentValues2.put("name", strTrim);
                    contentResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues2, "_id=?", new String[]{Long.valueOf(iIdForplaylist).toString()});
                } else {
                    if (!strTrim.equals(this.mPlaylistName) && !strTrim.equalsIgnoreCase(this.mPlaylistName)) {
                        try {
                            int iIdForplaylist2 = MusicUtils.idForplaylist(getActivity().getApplicationContext(), strTrim);
                            if (iIdForplaylist2 >= 0) {
                                long j2 = iIdForplaylist2;
                                uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, j2);
                                if (MusicUtils.clearPlaylist(getActivity().getApplicationContext(), iIdForplaylist2) == -1) {
                                    if (this.mDialog != null) {
                                        this.mDialog.dismiss();
                                        this.mDialog = null;
                                        return;
                                    }
                                    return;
                                }
                                ContentValues contentValues3 = new ContentValues(1);
                                contentValues3.put("name", strTrim);
                                contentResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues3, "_id=?", new String[]{Long.valueOf(j2).toString()});
                            } else {
                                uriWithAppendedId = null;
                            }
                        } catch (UnsupportedOperationException e) {
                            MusicLogUtils.d("CreatePlaylist", "OnClickListener() with UnsupportedOperationException:" + e);
                            if (this.mDialog != null) {
                                this.mDialog.dismiss();
                                this.mDialog = null;
                                return;
                            }
                            return;
                        }
                    }
                    intent.putExtra("SAVE_PLAYLIST_FLAG", "save_as_playlist");
                    uriInsert = uriWithAppendedId;
                }
                uriWithAppendedId = null;
                intent.putExtra("SAVE_PLAYLIST_FLAG", "save_as_playlist");
                uriInsert = uriWithAppendedId;
            } else {
                ContentValues contentValues4 = new ContentValues(1);
                contentValues4.put("name", strTrim);
                try {
                    uriInsert = contentResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues4);
                } catch (UnsupportedOperationException e2) {
                    MusicLogUtils.d("CreatePlaylist", "OnClickListener() with UnsupportedOperationException:" + e2);
                    if (this.mDialog != null) {
                        this.mDialog.dismiss();
                        this.mDialog = null;
                        return;
                    }
                    return;
                }
            }
            if (this.mPlaylistFlag.equals("new_playlist")) {
                if (this.mStartActivityTab == 2) {
                    MusicUtils.addToPlaylist(getActivity().getApplicationContext(), new long[]{Long.parseLong(this.mSelectItemId)}, Integer.valueOf(uriInsert.getLastPathSegment()).intValue());
                    return;
                }
                if (this.mStartActivityTab == 1) {
                    if (uriInsert != null) {
                        MusicUtils.addToPlaylist(getActivity().getApplicationContext(), MusicUtils.getSongListForAlbum(getActivity().getApplicationContext(), Long.parseLong(this.mSelectItemId)), Long.parseLong(uriInsert.getLastPathSegment()));
                        return;
                    }
                    return;
                } else {
                    if (this.mStartActivityTab == 0) {
                        if (uriInsert != null && this.mSelectItemId != null) {
                            String strSubstring = this.mSelectItemId.substring(this.mSelectItemId.lastIndexOf("_") + 1);
                            if (this.mSelectItemId.startsWith("selectedartist")) {
                                songListForAlbum = MusicUtils.getSongListForArtist(getActivity().getApplicationContext(), Long.parseLong(strSubstring));
                            } else if (this.mSelectItemId.startsWith("selectedalbum")) {
                                songListForAlbum = MusicUtils.getSongListForAlbum(getActivity().getApplicationContext(), Long.parseLong(strSubstring));
                            }
                            MusicUtils.addToPlaylist(getActivity().getApplicationContext(), songListForAlbum, Long.parseLong(uriInsert.getLastPathSegment()));
                            return;
                        }
                        return;
                    }
                    MusicUtils.addToPlaylist(getActivity().getApplicationContext(), new long[]{Long.parseLong(this.mSelectItemId)}, Integer.valueOf(uriInsert.getLastPathSegment()).intValue());
                    return;
                }
            }
            if (this.mPlaylistFlag.equals("save_as_playlist")) {
                long[] jArr = sTempCursor;
                int length = jArr.length;
                if (uriInsert != null) {
                    MusicUtils.addToPlaylist(getActivity().getApplicationContext(), jArr, Integer.parseInt(uriInsert.getLastPathSegment()));
                } else if (this.mPlaylistFlag.equals("save_as_playlist")) {
                    MusicUtils.showCreatePlaylistToast(length, getActivity().getApplicationContext());
                }
            }
        }
    }
}
