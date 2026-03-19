package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.Handler;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ApduSender {
    private static final int INS_GET_MORE_RESPONSE = 192;
    private static final String LOG_TAG = "ApduSender";
    private static final int STATUS_NO_ERROR = 36864;
    private static final int SW1_MORE_RESPONSE = 97;
    private final String mAid;
    private final Object mChannelLock = new Object();
    private boolean mChannelOpened;
    private final CloseLogicalChannelInvocation mCloseChannel;
    private final OpenLogicalChannelInvocation mOpenChannel;
    private final boolean mSupportExtendedApdu;
    private final TransmitApduLogicalChannelInvocation mTransmitApdu;

    private static void logv(String str) {
        Rlog.v(LOG_TAG, str);
    }

    public ApduSender(CommandsInterface commandsInterface, String str, boolean z) {
        this.mAid = str;
        this.mSupportExtendedApdu = z;
        this.mOpenChannel = new OpenLogicalChannelInvocation(commandsInterface);
        this.mCloseChannel = new CloseLogicalChannelInvocation(commandsInterface);
        this.mTransmitApdu = new TransmitApduLogicalChannelInvocation(commandsInterface);
    }

    public void send(final RequestProvider requestProvider, final AsyncResultCallback<byte[]> asyncResultCallback, final Handler handler) {
        synchronized (this.mChannelLock) {
            if (this.mChannelOpened) {
                AsyncResultHelper.throwException(new ApduException("Logical channel has already been opened."), asyncResultCallback, handler);
            } else {
                this.mChannelOpened = true;
                this.mOpenChannel.invoke(this.mAid, new AsyncResultCallback<IccOpenLogicalChannelResponse>() {
                    @Override
                    public void onResult(IccOpenLogicalChannelResponse iccOpenLogicalChannelResponse) {
                        int channel = iccOpenLogicalChannelResponse.getChannel();
                        int status = iccOpenLogicalChannelResponse.getStatus();
                        if (channel == -1 || status != 1) {
                            synchronized (ApduSender.this.mChannelLock) {
                                ApduSender.this.mChannelOpened = false;
                            }
                            asyncResultCallback.onException(new ApduException("Failed to open logical channel opened for AID: " + ApduSender.this.mAid + ", with status: " + status));
                            return;
                        }
                        RequestBuilder requestBuilder = new RequestBuilder(channel, ApduSender.this.mSupportExtendedApdu);
                        Throwable th = null;
                        try {
                            requestProvider.buildRequest(iccOpenLogicalChannelResponse.getSelectResponse(), requestBuilder);
                        } catch (Throwable th2) {
                            th = th2;
                        }
                        if (requestBuilder.getCommands().isEmpty() || th != null) {
                            ApduSender.this.closeAndReturn(channel, null, th, asyncResultCallback, handler);
                        } else {
                            ApduSender.this.sendCommand(requestBuilder.getCommands(), 0, asyncResultCallback, handler);
                        }
                    }
                }, handler);
            }
        }
    }

    private void sendCommand(final List<ApduCommand> list, final int i, final AsyncResultCallback<byte[]> asyncResultCallback, final Handler handler) {
        final ApduCommand apduCommand = list.get(i);
        this.mTransmitApdu.invoke(apduCommand, new AsyncResultCallback<IccIoResult>() {
            @Override
            public void onResult(IccIoResult iccIoResult) {
                ApduSender.this.getCompleteResponse(apduCommand.channel, iccIoResult, null, new AsyncResultCallback<IccIoResult>() {
                    @Override
                    public void onResult(IccIoResult iccIoResult2) {
                        ApduSender.logv("Full APDU response: " + iccIoResult2);
                        int i2 = (iccIoResult2.sw1 << 8) | iccIoResult2.sw2;
                        if (i2 != ApduSender.STATUS_NO_ERROR) {
                            ApduSender.this.closeAndReturn(apduCommand.channel, null, new ApduException(i2), asyncResultCallback, handler);
                        } else if (i == list.size() - 1) {
                            ApduSender.this.closeAndReturn(apduCommand.channel, iccIoResult2.payload, null, asyncResultCallback, handler);
                        } else {
                            ApduSender.this.sendCommand(list, i + 1, asyncResultCallback, handler);
                        }
                    }
                }, handler);
            }
        }, handler);
    }

    private void getCompleteResponse(final int i, IccIoResult iccIoResult, ByteArrayOutputStream byteArrayOutputStream, final AsyncResultCallback<IccIoResult> asyncResultCallback, final Handler handler) {
        final ByteArrayOutputStream byteArrayOutputStream2 = byteArrayOutputStream == null ? new ByteArrayOutputStream() : byteArrayOutputStream;
        if (iccIoResult.payload != null) {
            try {
                byteArrayOutputStream2.write(iccIoResult.payload);
            } catch (IOException e) {
            }
        }
        if (iccIoResult.sw1 != 97) {
            iccIoResult.payload = byteArrayOutputStream2.toByteArray();
            asyncResultCallback.onResult(iccIoResult);
        } else {
            this.mTransmitApdu.invoke(new ApduCommand(i, 0, 192, 0, 0, iccIoResult.sw2, ""), new AsyncResultCallback<IccIoResult>() {
                @Override
                public void onResult(IccIoResult iccIoResult2) {
                    ApduSender.this.getCompleteResponse(i, iccIoResult2, byteArrayOutputStream2, asyncResultCallback, handler);
                }
            }, handler);
        }
    }

    private void closeAndReturn(int i, final byte[] bArr, final Throwable th, final AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        this.mCloseChannel.invoke(Integer.valueOf(i), new AsyncResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean bool) {
                synchronized (ApduSender.this.mChannelLock) {
                    ApduSender.this.mChannelOpened = false;
                }
                if (th == null) {
                    asyncResultCallback.onResult(bArr);
                } else {
                    asyncResultCallback.onException(th);
                }
            }
        }, handler);
    }
}
