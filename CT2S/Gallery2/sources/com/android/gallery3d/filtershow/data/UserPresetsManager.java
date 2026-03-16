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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    UserPresetsManager.this.resultLoad(msg);
                    break;
            }
        }
    };
    private FilterStackSource mUserPresets;

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                processLoad();
                break;
            case 3:
                processSave(msg);
                break;
            case 4:
                processDelete(msg);
                break;
            case 5:
                processUpdate(msg);
                break;
        }
        return true;
    }

    public UserPresetsManager(FilterShowActivity context) {
        this.mHandlerThread = null;
        this.mProcessingHandler = null;
        this.mActivity = context;
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
        Message msg = this.mProcessingHandler.obtainMessage(1);
        this.mProcessingHandler.sendMessage(msg);
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

    public void save(ImagePreset preset, String name) {
        Message msg = this.mProcessingHandler.obtainMessage(3);
        SaveOperation op = new SaveOperation();
        op.json = preset.getJsonString("Saved");
        op.name = name;
        msg.obj = op;
        this.mProcessingHandler.sendMessage(msg);
    }

    public void delete(int id) {
        Message msg = this.mProcessingHandler.obtainMessage(4);
        msg.arg1 = id;
        this.mProcessingHandler.sendMessage(msg);
    }

    static class UpdateOperation {
        int id;
        String name;

        UpdateOperation() {
        }
    }

    public void update(FilterUserPresetRepresentation representation) {
        Message msg = this.mProcessingHandler.obtainMessage(5);
        UpdateOperation op = new UpdateOperation();
        op.id = representation.getId();
        op.name = representation.getName();
        msg.obj = op;
        this.mProcessingHandler.sendMessage(msg);
    }

    private void processLoad() {
        ArrayList<FilterUserPresetRepresentation> list = this.mUserPresets.getAllUserPresets();
        Message msg = this.mResultHandler.obtainMessage(2);
        msg.obj = list;
        this.mResultHandler.sendMessage(msg);
    }

    private void resultLoad(Message msg) {
        this.mRepresentations = (ArrayList) msg.obj;
        this.mActivity.updateUserPresetsFromManager();
    }

    private void processSave(Message msg) {
        SaveOperation op = (SaveOperation) msg.obj;
        this.mUserPresets.insertStack(op.name, op.json.getBytes());
        processLoad();
    }

    private void processDelete(Message msg) {
        int id = msg.arg1;
        this.mUserPresets.removeStack(id);
        processLoad();
    }

    private void processUpdate(Message msg) {
        UpdateOperation op = (UpdateOperation) msg.obj;
        this.mUserPresets.updateStackName(op.id, op.name);
        processLoad();
    }
}
