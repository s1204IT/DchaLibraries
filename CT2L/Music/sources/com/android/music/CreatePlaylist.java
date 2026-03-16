package com.android.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CreatePlaylist extends Activity {
    private EditText mPlaylist;
    private TextView mPrompt;
    private Button mSaveButton;
    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String newText = CreatePlaylist.this.mPlaylist.getText().toString();
            if (newText.trim().length() == 0) {
                CreatePlaylist.this.mSaveButton.setEnabled(false);
                return;
            }
            CreatePlaylist.this.mSaveButton.setEnabled(true);
            if (CreatePlaylist.this.idForplaylist(newText) >= 0) {
                CreatePlaylist.this.mSaveButton.setText(R.string.create_playlist_overwrite_text);
            } else {
                CreatePlaylist.this.mSaveButton.setText(R.string.create_playlist_create_text);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String name = CreatePlaylist.this.mPlaylist.getText().toString();
            if (name != null && name.length() > 0) {
                ContentResolver resolver = CreatePlaylist.this.getContentResolver();
                int id = CreatePlaylist.this.idForplaylist(name);
                if (id >= 0) {
                    Toast.makeText(CreatePlaylist.this, R.string.cancel, 0).show();
                } else {
                    ContentValues values = new ContentValues(1);
                    values.put("name", name);
                    Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                    CreatePlaylist.this.setResult(-1, new Intent().setData(uri));
                }
                CreatePlaylist.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
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
                CreatePlaylist.this.finish();
            }
        });
        String defaultname = icicle != null ? icicle.getString("defaultname") : makePlaylistName();
        if (defaultname == null) {
            finish();
            return;
        }
        String promptformat = getString(R.string.create_playlist_create_text_prompt);
        String prompt = String.format(promptformat, defaultname);
        this.mPrompt.setText(prompt);
        this.mPlaylist.setText(defaultname);
        this.mPlaylist.setSelection(defaultname.length());
        this.mPlaylist.addTextChangedListener(this.mTextWatcher);
    }

    private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "name=?", new String[]{name}, "name");
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", this.mPlaylist.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private String makePlaylistName() {
        String template = getString(R.string.new_playlist_name_template);
        String[] cols = {"name"};
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols, "name != ''", null, "name");
        if (c == null) {
            return null;
        }
        int num = 1 + 1;
        String suggestedname = String.format(template, 1);
        boolean done = false;
        int num2 = num;
        while (!done) {
            done = true;
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String playlistname = c.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, Integer.valueOf(num2));
                    done = false;
                    num2++;
                }
                c.moveToNext();
            }
        }
        c.close();
        return suggestedname;
    }
}
