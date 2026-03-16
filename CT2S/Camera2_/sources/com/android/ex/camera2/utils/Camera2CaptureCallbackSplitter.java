package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import java.util.LinkedList;
import java.util.List;

public class Camera2CaptureCallbackSplitter extends CameraCaptureSession.CaptureCallback {
    private final List<CameraCaptureSession.CaptureCallback> mRecipients = new LinkedList();

    public Camera2CaptureCallbackSplitter(CameraCaptureSession.CaptureCallback... recipients) {
        for (CameraCaptureSession.CaptureCallback listener : recipients) {
            if (listener != null) {
                this.mRecipients.add(listener);
            }
        }
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureCompleted(session, request, result);
        }
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureFailed(session, request, failure);
        }
    }

    @Override
    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureProgressed(session, request, partialResult);
        }
    }

    @Override
    public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureSequenceAborted(session, sequenceId);
        }
    }

    @Override
    public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }
    }

    @Override
    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
        for (CameraCaptureSession.CaptureCallback target : this.mRecipients) {
            target.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    }
}
