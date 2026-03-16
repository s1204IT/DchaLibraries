package com.android.wallpaper.livepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.WallpaperInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.android.wallpaper.livepicker.LiveWallpaperListAdapter;

public class LiveWallpaperActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_wallpaper_base);
        Fragment fragmentView = getFragmentManager().findFragmentById(R.id.live_wallpaper_fragment);
        if (fragmentView == null) {
            DialogFragment fragment = WallpaperDialog.newInstance();
            fragment.show(getFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 100 || resultCode != -1) {
            return;
        }
        finish();
    }

    public static class WallpaperDialog extends DialogFragment implements AdapterView.OnItemClickListener {
        private LiveWallpaperListAdapter mAdapter;
        private boolean mEmbedded;

        public static WallpaperDialog newInstance() {
            WallpaperDialog dialog = new WallpaperDialog();
            dialog.setCancelable(true);
            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null && savedInstanceState.containsKey("com.android.wallpaper.livepicker.LiveWallpaperActivity$WallpaperDialog.EMBEDDED_KEY")) {
                this.mEmbedded = savedInstanceState.getBoolean("com.android.wallpaper.livepicker.LiveWallpaperActivity$WallpaperDialog.EMBEDDED_KEY");
            } else {
                this.mEmbedded = isInLayout();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putBoolean("com.android.wallpaper.livepicker.LiveWallpaperActivity$WallpaperDialog.EMBEDDED_KEY", this.mEmbedded);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int contentInset = getResources().getDimensionPixelSize(R.dimen.dialog_content_inset);
            View view = generateView(getActivity().getLayoutInflater(), null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setNegativeButton(R.string.wallpaper_cancel, (DialogInterface.OnClickListener) null);
            builder.setTitle(R.string.live_wallpaper_picker_title);
            builder.setView(view, contentInset, contentInset, contentInset, contentInset);
            return builder.create();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (this.mEmbedded) {
                return generateView(inflater, container);
            }
            return null;
        }

        private View generateView(LayoutInflater inflater, ViewGroup container) {
            View layout = inflater.inflate(R.layout.live_wallpaper_list, container, false);
            this.mAdapter = new LiveWallpaperListAdapter(getActivity());
            AdapterView<BaseAdapter> adapterView = (AdapterView) layout.findViewById(android.R.id.list);
            adapterView.setAdapter(this.mAdapter);
            adapterView.setOnItemClickListener(this);
            adapterView.setEmptyView(layout.findViewById(android.R.id.empty));
            return layout;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            LiveWallpaperListAdapter.LiveWallpaperInfo wallpaperInfo = (LiveWallpaperListAdapter.LiveWallpaperInfo) this.mAdapter.getItem(position);
            Intent intent = wallpaperInfo.intent;
            WallpaperInfo info = wallpaperInfo.info;
            LiveWallpaperPreview.showPreview(getActivity(), 100, intent, info);
        }
    }
}
