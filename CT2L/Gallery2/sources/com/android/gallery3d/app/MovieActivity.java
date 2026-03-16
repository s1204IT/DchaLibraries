package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;

public class MovieActivity extends Activity {
    private boolean mFinishOnCompletion;
    private MoviePlayer mPlayer;
    private boolean mTreatUpAsBack;
    private Uri mUri;

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void setSystemUiVisibility(View rootView) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            rootView.setSystemUiVisibility(1792);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int orientation;
        super.onCreate(savedInstanceState);
        requestWindowFeature(8);
        requestWindowFeature(9);
        setContentView(R.layout.movie_view);
        View rootView = findViewById(R.id.movie_view_root);
        setSystemUiVisibility(rootView);
        Intent intent = getIntent();
        initializeActionBar(intent);
        this.mFinishOnCompletion = intent.getBooleanExtra("android.intent.extra.finishOnCompletion", true);
        this.mTreatUpAsBack = intent.getBooleanExtra("treat-up-as-back", false);
        this.mPlayer = new MoviePlayer(rootView, this, intent.getData(), savedInstanceState, this.mFinishOnCompletion ? false : true) {
            @Override
            public void onCompletion() {
                if (MovieActivity.this.mFinishOnCompletion) {
                    MovieActivity.this.finish();
                }
            }
        };
        if (intent.hasExtra("android.intent.extra.screenOrientation") && (orientation = intent.getIntExtra("android.intent.extra.screenOrientation", -1)) != getRequestedOrientation()) {
            setRequestedOrientation(orientation);
        }
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = 0.0f;
        winParams.flags |= 1024;
        win.setAttributes(winParams);
        win.setBackgroundDrawable(null);
    }

    private void setActionBarLogoFromIntent(Intent intent) {
        Bitmap logo = (Bitmap) intent.getParcelableExtra("logo-bitmap");
        if (logo != null) {
            getActionBar().setLogo(new BitmapDrawable(getResources(), logo));
        }
    }

    private void initializeActionBar(Intent intent) {
        this.mUri = intent.getData();
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            setActionBarLogoFromIntent(intent);
            actionBar.setDisplayOptions(4, 4);
            String title = intent.getStringExtra("android.intent.extra.TITLE");
            if (title != null) {
                actionBar.setTitle(title);
            } else {
                AsyncQueryHandler queryHandler = new AsyncQueryHandler(getContentResolver()) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                        if (cursor != null) {
                            try {
                                if (cursor.moveToFirst()) {
                                    String displayName = cursor.getString(0);
                                    ActionBar actionBar2 = actionBar;
                                    if (displayName == null) {
                                        displayName = "";
                                    }
                                    actionBar2.setTitle(displayName);
                                }
                            } finally {
                                Utils.closeSilently(cursor);
                            }
                        }
                    }
                };
                queryHandler.startQuery(0, null, this.mUri, new String[]{"_display_name"}, null, null, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.movie, menu);
        MenuItem shareItem = menu.findItem(R.id.action_share);
        if ("content".equals(this.mUri.getScheme())) {
            shareItem.setVisible(true);
            ((ShareActionProvider) shareItem.getActionProvider()).setShareIntent(createShareIntent());
        } else {
            shareItem.setVisible(false);
        }
        return true;
    }

    private Intent createShareIntent() {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("video/*");
        intent.putExtra("android.intent.extra.STREAM", this.mUri);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 16908332) {
            if (this.mTreatUpAsBack) {
                finish();
                return true;
            }
            startActivity(new Intent(this, (Class<?>) GalleryActivity.class));
            finish();
            return true;
        }
        if (id == R.id.action_share) {
            startActivity(Intent.createChooser(createShareIntent(), getString(R.string.share)));
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        ((AudioManager) getSystemService("audio")).requestAudioFocus(null, 3, 2);
        super.onStart();
    }

    @Override
    protected void onStop() {
        ((AudioManager) getSystemService("audio")).abandonAudioFocus(null);
        super.onStop();
    }

    @Override
    public void onPause() {
        this.mPlayer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        this.mPlayer.onResume();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mPlayer.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        this.mPlayer.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mPlayer.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mPlayer.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }
}
