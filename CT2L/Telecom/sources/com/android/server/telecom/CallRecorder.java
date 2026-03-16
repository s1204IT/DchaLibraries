package com.android.server.telecom;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallRecorder {
    public static CallRecorder gInstance = null;
    public static boolean recorderOn = false;
    private File file = null;
    private MediaRecorder mediaRecorder;

    public static CallRecorder getRecorder() {
        if (gInstance == null) {
            gInstance = new CallRecorder();
        }
        return gInstance;
    }

    public void startRecording() throws Exception {
        if (!recorderOn) {
            this.mediaRecorder = new MediaRecorder();
            this.mediaRecorder.setAudioSource(4);
            this.mediaRecorder.setOutputFormat(3);
            this.mediaRecorder.setAudioEncoder(1);
            this.file = File.createTempFile("CallRecord", ".amr", Environment.getExternalStorageDirectory());
            this.mediaRecorder.setOutputFile(this.file.getAbsolutePath());
            this.mediaRecorder.prepare();
            this.mediaRecorder.start();
        }
    }

    public void stopRecording() {
        if (this.mediaRecorder != null && recorderOn) {
            this.mediaRecorder.stop();
            this.mediaRecorder.release();
            this.mediaRecorder = null;
        }
    }

    public String saveToDB(Context context) {
        ContentValues contentValues = new ContentValues(3);
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jLastModified = this.file.lastModified();
        String str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(jCurrentTimeMillis));
        contentValues.put("is_music", "0");
        contentValues.put("title", str);
        contentValues.put("_data", this.file.getAbsolutePath());
        contentValues.put("date_added", Integer.valueOf((int) (jCurrentTimeMillis / 1000)));
        contentValues.put("date_modified", Integer.valueOf((int) (jLastModified / 1000)));
        contentValues.put("mime_type", "audio/amr_nb");
        contentValues.put("artist", "CallRecord");
        contentValues.put("album", "CallRecorder");
        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)));
        return this.file.getAbsolutePath();
    }

    public void deleteRecordFile() {
        if (this.file.exists()) {
            this.file.delete();
        }
    }
}
