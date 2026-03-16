package com.android.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RenamePlaylist extends Activity {
    private String mOriginalName;
    private EditText mPlaylist;
    private TextView mPrompt;
    private long mRenameId;
    private Button mSaveButton;
    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            RenamePlaylist.this.setSaveButton();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String name = RenamePlaylist.this.mPlaylist.getText().toString();
            if (name != null && name.length() > 0) {
                int id = RenamePlaylist.this.idForplaylist(name);
                if (id == -1) {
                    ContentResolver resolver = RenamePlaylist.this.getContentResolver();
                    ContentValues values = new ContentValues(1);
                    values.put("name", name);
                    String oldPath = RenamePlaylist.this.pathForUri(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
                    int index = oldPath.lastIndexOf("/");
                    String newPath = oldPath.substring(0, index) + "/" + name;
                    values.put("_data", newPath);
                    resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values, "_id=?", new String[]{Long.valueOf(RenamePlaylist.this.mRenameId).toString()});
                    RenamePlaylist.this.setResult(-1);
                    Toast.makeText(RenamePlaylist.this, R.string.playlist_renamed_message, 0).show();
                } else {
                    Toast.makeText(RenamePlaylist.this, R.string.cancel, 0).show();
                }
                RenamePlaylist.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        String promptformat;
        super.onCreate(icicle);
        setVolumeControlStream(3);
        requestWindowFeature(1);
        setContentView(R.layout.create_playlist);
        getWindow().setLayout(-1, -2);
        this.mPrompt = (TextView) findViewById(R.id.prompt);
        this.mPlaylist = (EditText) findViewById(R.id.playlist);
        this.mSaveButton = (Button) findViewById(R.id.create);
        this.mSaveButton.setOnClickListener(this.mOpenClicked);
        ((Button) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RenamePlaylist.this.finish();
            }
        });
        this.mRenameId = icicle != null ? icicle.getLong("rename") : getIntent().getLongExtra("rename", -1L);
        this.mOriginalName = nameForId(this.mRenameId);
        String defaultname = icicle != null ? icicle.getString("defaultname") : this.mOriginalName;
        if (this.mRenameId < 0 || this.mOriginalName == null || defaultname == null) {
            Log.i("@@@@", "Rename failed: " + this.mRenameId + "/" + defaultname);
            finish();
            return;
        }
        if (this.mOriginalName.equals(defaultname)) {
            promptformat = getString(R.string.rename_playlist_same_prompt);
        } else {
            promptformat = getString(R.string.rename_playlist_diff_prompt);
        }
        String prompt = String.format(promptformat, this.mOriginalName, defaultname);
        this.mPrompt.setText(prompt);
        this.mPlaylist.setText(defaultname);
        this.mPlaylist.setSelection(defaultname.length());
        this.mPlaylist.addTextChangedListener(this.mTextWatcher);
        setSaveButton();
    }

    private void setSaveButton() {
        String typedname = this.mPlaylist.getText().toString();
        if (typedname.trim().length() == 0) {
            this.mSaveButton.setEnabled(false);
            return;
        }
        this.mSaveButton.setEnabled(true);
        if (idForplaylist(typedname) >= 0 && !this.mOriginalName.equals(typedname)) {
            this.mSaveButton.setText(R.string.create_playlist_overwrite_text);
        } else {
            this.mSaveButton.setText(R.string.create_playlist_create_text);
        }
    }

    private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "name=?", new String[]{name}, "name");
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
        }
        c.close();
        return id;
    }

    private String nameForId(long id) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"name"}, "_id=?", new String[]{Long.valueOf(id).toString()}, "name");
        String name = null;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                name = c.getString(0);
            }
        }
        c.close();
        return name;
    }

    private String pathForUri(Uri uri) {
        Cursor c = MusicUtils.query(this, uri, new String[]{"_id", "_data"}, null, null, "_id");
        if (c == null) {
            return null;
        }
        c.moveToFirst();
        int cid = c.getColumnIndex("_data");
        return c.getString(cid);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", this.mPlaylist.getText().toString());
        outcicle.putLong("rename", this.mRenameId);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
