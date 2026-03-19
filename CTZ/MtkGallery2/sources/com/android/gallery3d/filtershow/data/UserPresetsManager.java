package com.android.gallery3d.filtershow.data;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.ArrayList;

public class UserPresetsManager implements Handler.Callback {
    private FilterShowActivity mActivity;
    private HandlerThread mHandlerThread;
    private Handler mProcessingHandler;
    private ArrayList<FilterUserPresetRepresentation> mRepresentations;
    private final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 2) {
                UserPresetsManager.this.resultLoad(message);
            }
        }
    };
    private FilterStackSource mUserPresets;

    @Override
    public boolean handleMessage(Message message) throws Throwable {
        int i = message.what;
        if (i == 1) {
            processLoad();
            return true;
        }
        switch (i) {
            case 3:
                processSave(message);
                break;
            case 4:
                processDelete(message);
                break;
            case 5:
                processUpdate(message);
                break;
        }
        return true;
    }

    public UserPresetsManager(FilterShowActivity filterShowActivity) {
        this.mHandlerThread = null;
        this.mProcessingHandler = null;
        this.mActivity = filterShowActivity;
        this.mHandlerThread = new HandlerThread("UserPresetsManager", 10);
        this.mHandlerThread.start();
        this.mProcessingHandler = new Handler(this.mHandlerThread.getLooper(), this);
        this.mUserPresets = new FilterStackSource(this.mActivity);
        this.mUserPresets.open();
    }

    public ArrayList<FilterUserPresetRepresentation> getRepresentations() {
        return this.mRepresentations;
    }

    public void load() {
        this.mProcessingHandler.sendMessage(this.mProcessingHandler.obtainMessage(1));
    }

    public void close() {
        this.mUserPresets.close();
        this.mHandlerThread.quit();
    }

    static class SaveOperation {
        String json;
        String name;

        SaveOperation() {
        }
    }

    public void save(ImagePreset imagePreset, String str) {
        if (imagePreset == null) {
            return;
        }
        Message messageObtainMessage = this.mProcessingHandler.obtainMessage(3);
        SaveOperation saveOperation = new SaveOperation();
        saveOperation.json = imagePreset.getJsonString("Saved");
        saveOperation.name = str;
        messageObtainMessage.obj = saveOperation;
        this.mProcessingHandler.sendMessage(messageObtainMessage);
    }

    public void delete(int i) {
        Message messageObtainMessage = this.mProcessingHandler.obtainMessage(4);
        messageObtainMessage.arg1 = i;
        this.mProcessingHandler.sendMessage(messageObtainMessage);
    }

    static class UpdateOperation {
        int id;
        String name;

        UpdateOperation() {
        }
    }

    public void update(FilterUserPresetRepresentation filterUserPresetRepresentation) {
        Message messageObtainMessage = this.mProcessingHandler.obtainMessage(5);
        UpdateOperation updateOperation = new UpdateOperation();
        updateOperation.id = filterUserPresetRepresentation.getId();
        updateOperation.name = filterUserPresetRepresentation.getName();
        messageObtainMessage.obj = updateOperation;
        this.mProcessingHandler.sendMessage(messageObtainMessage);
    }

    private void processLoad() throws Throwable {
        ArrayList<FilterUserPresetRepresentation> allUserPresets = this.mUserPresets.getAllUserPresets();
        Message messageObtainMessage = this.mResultHandler.obtainMessage(2);
        messageObtainMessage.obj = allUserPresets;
        this.mResultHandler.sendMessage(messageObtainMessage);
    }

    private void resultLoad(Message message) {
        this.mRepresentations = (ArrayList) message.obj;
        this.mActivity.updateUserPresetsFromManager();
    }

    private void processSave(Message message) throws Throwable {
        SaveOperation saveOperation = (SaveOperation) message.obj;
        this.mUserPresets.insertStack(saveOperation.name, saveOperation.json.getBytes());
        processLoad();
    }

    private void processDelete(Message message) throws Throwable {
        this.mUserPresets.removeStack(message.arg1);
        processLoad();
    }

    private void processUpdate(Message message) throws Throwable {
        UpdateOperation updateOperation = (UpdateOperation) message.obj;
        this.mUserPresets.updateStackName(updateOperation.id, updateOperation.name);
        processLoad();
    }
}
