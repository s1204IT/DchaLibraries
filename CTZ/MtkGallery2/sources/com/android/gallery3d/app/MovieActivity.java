package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.video.BookmarkActivity;
import com.mediatek.gallery3d.video.DefaultMovieItem;
import com.mediatek.gallery3d.video.ExtensionHelper;
import com.mediatek.gallery3d.video.IMovieItem;
import com.mediatek.gallery3d.video.MovieUtils;
import com.mediatek.gallery3d.video.MtkVideoFeature;
import com.mediatek.gallery3d.video.RequestPermissionActivity;
import com.mediatek.gallery3d.video.VideoTitleHooker;
import com.mediatek.plugin.preload.SoOperater;
import mf.org.apache.xerces.impl.xpath.XPath;

public class MovieActivity extends Activity {
    private boolean mFinishOnCompletion;
    private IActivityHooker mMovieHooker;
    private IMovieItem mMovieItem;
    private MoviePlayer mPlayer;
    private MenuItem mShareMenu;
    private ShareActionProvider mShareProvider;
    private boolean mTreatUpAsBack;

    @TargetApi(16)
    private void setSystemUiVisibility(View view) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            view.setSystemUiVisibility(1792);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        int intExtra;
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onCreate()");
        super.onCreate(bundle);
        if (RequestPermissionActivity.startPermissionActivity(this)) {
            com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onCreate(), need start permission activity, return");
            return;
        }
        requestWindowFeature(8);
        requestWindowFeature(9);
        setVolumeControlStream(3);
        setContentView(R.layout.movie_view);
        View viewFindViewById = findViewById(R.id.movie_view_root);
        setSystemUiVisibility(viewFindViewById);
        Intent intent = getIntent();
        if (!initMovieInfo(intent)) {
            com.mediatek.gallery3d.util.Log.e("VP_MovieActivity", "finish activity");
            finish();
            return;
        }
        initializeActionBar(intent);
        this.mMovieHooker = ExtensionHelper.getHooker(this);
        this.mPlayer = new MoviePlayer(viewFindViewById, this, this.mMovieItem, bundle, !this.mFinishOnCompletion, intent.getStringExtra("Cookie")) {
            @Override
            public void onCompletion() {
                com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onCompletion() mFinishOnCompletion=" + MovieActivity.this.mFinishOnCompletion);
                if (MovieActivity.this.mFinishOnCompletion) {
                    MovieActivity.this.finish();
                }
            }
        };
        if (intent.hasExtra("android.intent.extra.screenOrientation") && (intExtra = intent.getIntExtra("android.intent.extra.screenOrientation", -1)) != getRequestedOrientation()) {
            setRequestedOrientation(intExtra);
        }
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.buttonBrightness = 0.0f;
        if (!isMultiWindowMode()) {
            com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onCreate(), add FLAG_FULLSCREEN");
            attributes.flags |= SoOperater.STEP;
        }
        window.setAttributes(attributes);
        window.setFormat(-3);
        window.setBackgroundDrawable(null);
        if (this.mMovieHooker != null) {
            this.mMovieHooker.init(this, intent);
            this.mMovieHooker.setParameter(null, this.mMovieItem);
            this.mMovieHooker.setParameter(null, this.mPlayer);
            this.mMovieHooker.setParameter(null, this.mPlayer.getVideoSurface());
            this.mMovieHooker.setParameter(null, this.mPlayer.getPlayerWrapper());
            this.mMovieHooker.onCreate(bundle);
        }
    }

    @Override
    public void onStart() {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onStart()");
        super.onStart();
        if (this.mMovieHooker != null) {
            this.mMovieHooker.onStart();
        }
    }

    @Override
    public void onResume() {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onResume()");
        super.onResume();
        if (isMultiWindowMode()) {
            refreshShareProvider(this.mMovieItem);
        }
        if (this.mPlayer != null) {
            this.mPlayer.onResume();
        }
        if (this.mMovieHooker != null) {
            this.mMovieHooker.onResume();
        }
    }

    @Override
    public void onPause() {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onPause()");
        super.onPause();
        if (this.mPlayer != null) {
            this.mPlayer.onPause();
        }
        if (this.mMovieHooker != null) {
            this.mMovieHooker.onPause();
        }
    }

    @Override
    protected void onStop() {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onStop()");
        super.onStop();
        if (this.mPlayer != null) {
            this.mPlayer.onStop();
        }
        if (this.mMovieHooker != null) {
            this.mMovieHooker.onStop();
        }
    }

    @Override
    public void onDestroy() {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onDestroy()");
        if (this.mPlayer != null) {
            this.mPlayer.onDestroy();
        }
        if (this.mMovieHooker != null) {
            this.mMovieHooker.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onCreateOptionsMenu");
        if (this.mMovieItem == null) {
            com.mediatek.gallery3d.util.Log.w("VP_MovieActivity", "onCreateOptionsMenu, mMovieItem is null, return");
            return false;
        }
        boolean zIsLocalFile = MovieUtils.isLocalFile(this.mMovieItem.getUri(), this.mMovieItem.getMimeType());
        if (!MovieUtils.canShare(getIntent().getExtras()) || ((zIsLocalFile && !ExtensionHelper.getMovieDrmExtension(this).canShare(this, this.mMovieItem)) || getIntent().getBooleanExtra(VideoTitleHooker.SCREEN_ORIENTATION_LANDSCAPE, false) || !this.mMovieItem.canShare())) {
            com.mediatek.gallery3d.util.Log.w("VP_MovieActivity", "do not show share");
        } else {
            getMenuInflater().inflate(R.menu.movie, menu);
            this.mShareMenu = menu.findItem(R.id.action_share);
            this.mShareProvider = (ShareActionProvider) this.mShareMenu.getActionProvider();
            refreshShareProvider(this.mMovieItem);
        }
        if (this.mMovieHooker != null) {
            return this.mMovieHooker.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onPrepareOptionsMenu");
        if (this.mMovieHooker != null) {
            return this.mMovieHooker.onPrepareOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            if (this.mTreatUpAsBack) {
                finish();
            } else {
                startActivity(new Intent(this, (Class<?>) GalleryActivity.class));
                finish();
            }
            return true;
        }
        if (itemId == R.id.action_share || this.mMovieHooker == null) {
            return true;
        }
        return this.mMovieHooker.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "onSaveInstanceState()");
        super.onSaveInstanceState(bundle);
        if (this.mPlayer != null) {
            this.mPlayer.onSaveInstanceState(bundle);
        }
    }

    private boolean initMovieInfo(Intent intent) {
        Uri data = intent.getData();
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "initMovieInfo, original uri is " + data);
        if (data == null) {
            com.mediatek.gallery3d.util.Log.e("VP_MovieActivity", "initMovieInfo, acquired uri is null");
            return false;
        }
        this.mMovieItem = new DefaultMovieItem(getApplicationContext(), data, intent.getType(), (String) null);
        this.mFinishOnCompletion = intent.getBooleanExtra("android.intent.extra.finishOnCompletion", true);
        this.mTreatUpAsBack = intent.getBooleanExtra("treat-up-as-back", false);
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "initMovieInfo() mMovieInfo = " + this.mMovieItem + ", mFinishOnCompletion = " + this.mFinishOnCompletion + ", mTreatUpAsBack = " + this.mTreatUpAsBack);
        return true;
    }

    private void setActionBarLogoFromIntent(Intent intent) {
        Bitmap bitmap = (Bitmap) intent.getParcelableExtra(BookmarkActivity.KEY_LOGO_BITMAP);
        if (bitmap != null) {
            getActionBar().setLogo(new BitmapDrawable(getResources(), bitmap));
        }
    }

    private void initializeActionBar(Intent intent) {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        setActionBarLogoFromIntent(intent);
        actionBar.setDisplayOptions(4, 4);
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | 8);
    }

    private void setActionBarTitle(String str) {
        ActionBar actionBar = getActionBar();
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "setActionBarTitle(" + str + ") actionBar = " + actionBar);
        if (actionBar != null && str != null) {
            actionBar.setTitle(str);
        }
    }

    public boolean isMultiWindowMode() {
        boolean z;
        if (MtkVideoFeature.isMultiWindowSupport() && isInMultiWindowMode()) {
            z = true;
        } else {
            z = false;
        }
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "isInMultiWindowMode = " + z + ", sdk version = " + Build.VERSION.SDK_INT);
        return z;
    }

    public void refreshMovieInfo(IMovieItem iMovieItem) {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "refreshMovieInfo(" + iMovieItem + ")");
        this.mMovieItem = iMovieItem;
        setActionBarTitle(iMovieItem.getTitle());
        refreshShareProvider(iMovieItem);
        this.mMovieHooker.setParameter(null, this.mMovieItem);
    }

    private void refreshShareProvider(IMovieItem iMovieItem) {
        if (this.mShareProvider != null) {
            Intent intent = new Intent("android.intent.action.SEND");
            if (MovieUtils.isLocalFile(iMovieItem.getUri(), iMovieItem.getMimeType())) {
                intent.setType("video/*");
                intent.putExtra("android.intent.extra.STREAM", iMovieItem.getUri());
            } else {
                intent.setType("text/plain");
                intent.putExtra("android.intent.extra.TEXT", String.valueOf(iMovieItem.getUri()));
                com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "share as text/plain, info.getUri() = " + iMovieItem.getUri());
            }
            this.mShareProvider.setShareIntent(intent);
        }
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "refreshShareProvider() mShareProvider=" + this.mShareProvider);
    }

    public void setMovieHookerParameter(String str, Object obj) {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "setMovieHookerParameter key = " + str + " value = " + obj);
        if (this.mMovieHooker != null) {
            this.mMovieHooker.setParameter(str, obj);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mPlayer != null) {
            return this.mPlayer.onKeyDown(i, keyEvent) || super.onKeyDown(i, keyEvent);
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mPlayer != null) {
            return this.mPlayer.onKeyUp(i, keyEvent) || super.onKeyUp(i, keyEvent);
        }
        return false;
    }

    @Override
    @TargetApi(XPath.Tokens.EXPRTOKEN_OPERATOR_PLUS)
    public void onMultiWindowModeChanged(boolean z) {
        com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "[onMultiWindowModeChanged] isInMultiWIndowMode = " + z);
        if (z) {
            com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "[onMultiWindowModeChanged] clear FLAG_FULLSCREEN");
            getWindow().clearFlags(SoOperater.STEP);
        } else {
            com.mediatek.gallery3d.util.Log.d("VP_MovieActivity", "[onMultiWindowModeChanged] add FLAG_FULLSCREEN");
            getWindow().addFlags(SoOperater.STEP);
        }
    }
}
