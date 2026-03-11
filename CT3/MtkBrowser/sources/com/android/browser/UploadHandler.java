package com.android.browser;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;

public class UploadHandler {

    static final boolean f5assertionsDisabled;
    private Uri mCapturedMedia;
    private Controller mController;
    private boolean mHandled;
    private WebChromeClient.FileChooserParams mParams;
    private ValueCallback<Uri[]> mUploadMessage;

    static {
        f5assertionsDisabled = !UploadHandler.class.desiredAssertionStatus();
    }

    public UploadHandler(Controller controller) {
        this.mController = controller;
    }

    boolean handled() {
        return this.mHandled;
    }

    void onResult(int resultCode, Intent intent) {
        Uri[] uris = parseResult(resultCode, intent);
        this.mUploadMessage.onReceiveValue(uris);
        this.mHandled = true;
    }

    void openFileChooser(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams fileChooserParams) {
        Intent intent;
        if (this.mUploadMessage != null) {
            return;
        }
        this.mUploadMessage = callback;
        this.mParams = fileChooserParams;
        Intent[] captureIntents = createCaptureIntent();
        if (!f5assertionsDisabled) {
            if (!(captureIntents != null && captureIntents.length > 0)) {
                throw new AssertionError();
            }
        }
        if (fileChooserParams.isCaptureEnabled() && captureIntents.length == 1) {
            intent = captureIntents[0];
        } else {
            intent = new Intent("android.intent.action.CHOOSER");
            intent.putExtra("android.intent.extra.INITIAL_INTENTS", captureIntents);
            intent.putExtra("android.intent.extra.INTENT", fileChooserParams.createIntent());
        }
        startActivity(intent);
    }

    private Uri[] parseResult(int resultCode, Intent intent) {
        Uri result = null;
        if (resultCode == 0) {
            return null;
        }
        if (intent != null && resultCode == -1) {
            result = intent.getData();
        }
        if (result == null && intent == null && resultCode == -1 && this.mCapturedMedia != null) {
            result = this.mCapturedMedia;
        }
        if (result == null) {
            return null;
        }
        Uri[] uris = {result};
        return uris;
    }

    private void startActivity(Intent intent) {
        try {
            this.mController.getActivity().startActivityForResult(intent, 4);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.mController.getActivity(), R.string.uploads_disabled, 1).show();
        }
    }

    private Intent[] createCaptureIntent() {
        String mimeType = "*/*";
        String[] acceptTypes = this.mParams.getAcceptTypes();
        if (acceptTypes != null && acceptTypes.length > 0) {
            mimeType = acceptTypes[0];
        }
        if (mimeType.equals("image/*")) {
            Intent[] intents = {createCameraIntent(createTempFileContentUri(".jpg"))};
            return intents;
        }
        if (mimeType.equals("video/*")) {
            Intent[] intents2 = {createCamcorderIntent()};
            return intents2;
        }
        if (mimeType.equals("audio/*")) {
            Intent[] intents3 = {createSoundRecorderIntent()};
            return intents3;
        }
        Intent[] intents4 = {createCameraIntent(createTempFileContentUri(".jpg")), createCamcorderIntent(), createSoundRecorderIntent()};
        return intents4;
    }

    private Uri createTempFileContentUri(String suffix) {
        try {
            File mediaPath = new File(this.mController.getActivity().getFilesDir(), "captured_media");
            if (!mediaPath.exists() && !mediaPath.mkdir()) {
                throw new RuntimeException("Folder cannot be created.");
            }
            File mediaFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), suffix, mediaPath);
            return FileProvider.getUriForFile(this.mController.getActivity(), "com.android.browser-classic.file", mediaFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Intent createCameraIntent(Uri contentUri) {
        if (contentUri == null) {
            throw new IllegalArgumentException();
        }
        this.mCapturedMedia = contentUri;
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.setFlags(3);
        intent.putExtra("output", this.mCapturedMedia);
        intent.setClipData(ClipData.newUri(this.mController.getActivity().getContentResolver(), "com.android.browser-classic.file", this.mCapturedMedia));
        return intent;
    }

    private Intent createCamcorderIntent() {
        return new Intent("android.media.action.VIDEO_CAPTURE");
    }

    private Intent createSoundRecorderIntent() {
        return new Intent("android.provider.MediaStore.RECORD_SOUND");
    }
}
