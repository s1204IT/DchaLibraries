package com.android.bluetooth.ftp;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerOperation;
import javax.obex.ServerRequestHandler;

public class BluetoothFtpObexServer extends ServerRequestHandler {
    private static final boolean D = true;
    private static final String TAG = "BluetoothFtpObexServer";
    private static final String TYPE_LISTING = "x-obex/folder-listing";
    private static final int UUID_LENGTH = 16;
    private static final boolean V = true;
    private Handler mCallback;
    private Context mContext;
    public static boolean sIsAborted = false;
    private static final byte[] FTP_TARGET = {-7, -20, 123, -60, -107, 60, 17, -46, -104, 78, 82, 84, 0, -36, -98, 9};
    private static final String ROOT_FOLDER_PATH = "/sdcard";
    private static final String[] LEGAL_PATH = {ROOT_FOLDER_PATH};
    private String mCurrentPath = "";
    private long mConnectionId = -1;

    public BluetoothFtpObexServer(Handler callback, Context context) {
        this.mCallback = null;
        this.mCallback = callback;
        this.mContext = context;
        Log.d(TAG, "Initialize FtpObexServer");
    }

    public int onConnect(HeaderSet request, HeaderSet reply) {
        Log.d(TAG, "onConnect()+");
        try {
            byte[] uuid = (byte[]) request.getHeader(70);
            if (uuid == null) {
                return 198;
            }
            Log.d(TAG, "onConnect(): uuid=" + Arrays.toString(uuid));
            if (uuid.length != 16) {
                Log.w(TAG, "Wrong UUID length");
                return 198;
            }
            for (int i = 0; i < 16; i++) {
                if (uuid[i] != FTP_TARGET[i]) {
                    Log.w(TAG, "Wrong UUID");
                    return 198;
                }
            }
            reply.setHeader(74, uuid);
            try {
                byte[] remote = (byte[]) request.getHeader(74);
                if (remote != null) {
                    Log.d(TAG, "onConnect(): remote=" + Arrays.toString(remote));
                    reply.setHeader(70, remote);
                }
                Log.v(TAG, "onConnect(): uuid is ok, will send out MSG_SESSION_ESTABLISHED msg.");
                Message msg = Message.obtain(this.mCallback);
                msg.what = 5005;
                msg.sendToTarget();
                this.mCurrentPath = ROOT_FOLDER_PATH;
                Log.d(TAG, "onConnect() -");
                return 160;
            } catch (IOException e) {
                Log.e(TAG, "onConnect " + e.toString());
                return 208;
            }
        } catch (IOException e2) {
            Log.e(TAG, "onConnect " + e2.toString());
            return 208;
        }
    }

    public void onDisconnect(HeaderSet req, HeaderSet resp) {
        Log.d(TAG, "onDisconnect() +");
        resp.responseCode = 160;
        if (this.mCallback != null) {
            Message msg = Message.obtain(this.mCallback);
            msg.what = 5006;
            msg.sendToTarget();
            Log.v(TAG, "onDisconnect(): msg MSG_SESSION_DISCONNECTED sent out.");
        }
        Log.d(TAG, "onDisconnect() -");
    }

    public int onAbort(HeaderSet request, HeaderSet reply) {
        Log.d(TAG, "onAbort() +");
        sIsAborted = true;
        Log.d(TAG, "onAbort() -");
        return 160;
    }

    public int onDelete(HeaderSet request, HeaderSet reply) {
        int i;
        Log.d(TAG, "onDelete() +");
        if (!checkMountedState()) {
            Log.e(TAG, "SD card not Mounted");
            return 164;
        }
        try {
            String name = (String) request.getHeader(1);
            Log.d(TAG, "OnDelete File = " + name + "mCurrentPath = " + this.mCurrentPath);
            File deleteFile = new File(this.mCurrentPath + "/" + name);
            if (deleteFile.exists()) {
                Log.d(TAG, "onDelete(): Found File" + name + "in folder " + this.mCurrentPath);
                if (!deleteFile.canWrite()) {
                    i = 193;
                } else if (deleteFile.isDirectory()) {
                    if (!deleteDirectory(deleteFile)) {
                        Log.d(TAG, "Directory  delete unsuccessful");
                        i = 193;
                    } else {
                        Log.d(TAG, "onDelete() -");
                        i = 160;
                    }
                } else if (!deleteFile.delete()) {
                    Log.d(TAG, "File delete unsuccessful");
                    i = 193;
                }
            } else {
                Log.d(TAG, "File doesnot exist");
                i = 196;
            }
            return i;
        } catch (IOException e) {
            Log.e(TAG, "onDelete " + e.toString());
            Log.d(TAG, "Delete operation failed");
            return 208;
        }
    }

    public int onPut(Operation op) {
        Log.d(TAG, "onPut() +");
        if (!checkMountedState()) {
            Log.e(TAG, "SD card not Mounted");
            return 164;
        }
        try {
            HeaderSet request = op.getReceivedHeader();
            long length = ((Long) request.getHeader(195)).longValue();
            String name = (String) request.getHeader(1);
            String filetype = (String) request.getHeader(66);
            Log.d(TAG, "type = " + filetype + " name = " + name + " Current Path = " + this.mCurrentPath + "length = " + length);
            if (length == 0) {
                Log.d(TAG, "length is 0,proceeding with the transfer");
            }
            if (name == null || name.equals("")) {
                Log.d(TAG, "name is null or empty, reject the transfer");
                return BluetoothShare.STATUS_RUNNING;
            }
            if (!checkAvailableSpace(length)) {
                Log.d(TAG, "No Space Available");
                return 205;
            }
            try {
                InputStream in_stream = op.openInputStream();
                int positioninfile = 0;
                File fileinfo = new File(this.mCurrentPath + "/" + name);
                if (!fileinfo.getParentFile().canWrite()) {
                    Log.d(TAG, "Dir " + fileinfo.getParent() + "is read-only");
                    return 225;
                }
                if (fileinfo.exists()) {
                    fileinfo.delete();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(fileinfo);
                BufferedOutputStream buff_op_stream = new BufferedOutputStream(fileOutputStream, 16384);
                int outputBufferSize = op.getMaxPacketSize();
                byte[] buff = new byte[outputBufferSize];
                long starttimestamp = System.currentTimeMillis();
                while (true) {
                    if (positioninfile == length) {
                        break;
                    }
                    try {
                        if (sIsAborted) {
                            ((ServerOperation) op).isAborted = true;
                            sIsAborted = false;
                            break;
                        }
                        long timestamp = System.currentTimeMillis();
                        Log.v(TAG, "Read Socket >");
                        int readLength = in_stream.read(buff);
                        Log.v(TAG, "Read Socket <");
                        if (readLength == -1) {
                            Log.d(TAG, "File reached end at position" + positioninfile);
                            break;
                        }
                        buff_op_stream.write(buff, 0, readLength);
                        positioninfile += readLength;
                        Log.v(TAG, "Receive file position = " + positioninfile + " readLength " + readLength + " bytes took " + (System.currentTimeMillis() - timestamp) + " ms");
                    } catch (IOException e1) {
                        Log.e(TAG, "onPut File receive" + e1.toString());
                        Log.d(TAG, "Error when receiving file");
                        ((ServerOperation) op).isAborted = true;
                        return BluetoothShare.STATUS_RUNNING;
                    }
                }
                long finishtimestamp = System.currentTimeMillis();
                Log.i(TAG, "Put Request TP analysis : Received  " + positioninfile + " bytes in " + (finishtimestamp - starttimestamp) + "ms");
                if (buff_op_stream != null) {
                    try {
                        buff_op_stream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "onPut close stream " + e.toString());
                        Log.d(TAG, "Error when closing stream after send");
                        return 208;
                    }
                }
                Log.d(TAG, "close Stream >");
                if (!closeStream(in_stream, op)) {
                    Log.d(TAG, "Failed to close Input stream");
                    return 208;
                }
                Log.d(TAG, "close Stream <");
                Log.d(TAG, "onPut() -");
                return 160;
            } catch (IOException e12) {
                Log.e(TAG, "onPut open input stream " + e12.toString());
                Log.d(TAG, "Error while openInputStream");
                return 208;
            }
        } catch (IOException e2) {
            Log.e(TAG, "onPut headers error " + e2.toString());
            Log.d(TAG, "request headers error");
            return 208;
        }
    }

    public int onSetPath(HeaderSet request, HeaderSet reply, boolean backup, boolean create) {
        Log.d(TAG, "onSetPath() +");
        String current_path_tmp = this.mCurrentPath;
        if (!checkMountedState()) {
            Log.e(TAG, "SD card not Mounted");
            return 164;
        }
        try {
            String tmp_path = (String) request.getHeader(1);
            Log.d(TAG, "backup=" + backup + " create=" + create + " name=" + tmp_path + "mCurrentPath = " + this.mCurrentPath);
            if (backup) {
                if (current_path_tmp.length() != 0) {
                    current_path_tmp = current_path_tmp.substring(0, current_path_tmp.lastIndexOf("/"));
                }
            } else if (tmp_path == null) {
                current_path_tmp = ROOT_FOLDER_PATH;
            } else {
                current_path_tmp = current_path_tmp + "/" + tmp_path;
            }
            if (current_path_tmp.length() != 0 && !doesPathExist(current_path_tmp)) {
                Log.d(TAG, "Current path has valid length ");
                if (create) {
                    Log.d(TAG, "path create is not forbidden!");
                    File filecreate = new File(current_path_tmp);
                    filecreate.mkdir();
                    this.mCurrentPath = current_path_tmp;
                    return 160;
                }
                Log.d(TAG, "path not found error");
                return 196;
            }
            if (current_path_tmp.length() == 0) {
                current_path_tmp = ROOT_FOLDER_PATH;
            }
            this.mCurrentPath = current_path_tmp;
            Log.v(TAG, "after setPath, mCurrentPath ==  " + this.mCurrentPath);
            Log.d(TAG, "onSetPath() -");
            return 160;
        } catch (IOException e) {
            Log.e(TAG, "onSetPath  get header" + e.toString());
            Log.d(TAG, "Get name header fail");
            return 208;
        }
    }

    public void onClose() {
        Log.d(TAG, "onClose() +");
        if (this.mCallback != null) {
            Message msg = Message.obtain(this.mCallback);
            msg.what = 5004;
            msg.sendToTarget();
            Log.d(TAG, "onClose(): msg MSG_SERVERSESSION_CLOSE sent out.");
        }
        Log.d(TAG, "onClose() -");
    }

    public int onGet(Operation op) {
        Log.d(TAG, "onGet() +");
        sIsAborted = false;
        if (!checkMountedState()) {
            Log.e(TAG, "SD card not Mounted");
            return 164;
        }
        try {
            HeaderSet request = op.getReceivedHeader();
            String type = (String) request.getHeader(66);
            String name = (String) request.getHeader(1);
            Log.d(TAG, "type = " + type + " name = " + name + " Current Path = " + this.mCurrentPath);
            boolean validName = true;
            if (TextUtils.isEmpty(name)) {
                validName = false;
            }
            Log.d(TAG, "validName = " + validName);
            if (type != null) {
                if (type.equals(TYPE_LISTING)) {
                    if (!validName) {
                        Log.d(TAG, "Not having a name");
                        File rootfolder = new File(this.mCurrentPath);
                        File[] files = rootfolder.listFiles();
                        for (File file : files) {
                            Log.d(TAG, "Folder listing =" + file);
                        }
                        return sendFolderListingXml(0, op, files);
                    }
                    Log.d(TAG, "Non Root Folder");
                    if (type.equals(TYPE_LISTING)) {
                        File currentfolder = new File(this.mCurrentPath);
                        Log.d(TAG, "Current folder name = " + currentfolder.getName() + "Requested subFolder =" + name);
                        if (currentfolder.getName().compareTo(name) != 0) {
                            Log.d(TAG, "Not currently in this folder");
                            File subFolder = new File(this.mCurrentPath + "/" + name);
                            if (subFolder.exists()) {
                                return sendFolderListingXml(0, op, subFolder.listFiles());
                            }
                            Log.e(TAG, "ResponseCodes.OBEX_HTTP_NO_CONTENT");
                            return 164;
                        }
                        File[] files2 = currentfolder.listFiles();
                        for (File file2 : files2) {
                            Log.d(TAG, "Non Root Folder listing =" + file2);
                        }
                        return sendFolderListingXml(0, op, files2);
                    }
                }
                Log.d(TAG, "onGet() -");
                return BluetoothShare.STATUS_RUNNING;
            }
            Log.d(TAG, "File get request");
            File fileinfo = new File(this.mCurrentPath + "/" + name);
            return sendFileContents(op, fileinfo);
        } catch (IOException e) {
            Log.e(TAG, "onGet request headers " + e.toString());
            Log.d(TAG, "request headers error");
            return 208;
        }
    }

    private final boolean deleteDirectory(File dir) {
        Log.d(TAG, "deleteDirectory() +");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                    Log.d(TAG, "Dir Delete =" + files[i].getName());
                } else {
                    Log.d(TAG, "File Delete =" + files[i].getName());
                    files[i].delete();
                }
            }
        }
        Log.d(TAG, "deleteDirectory() -");
        return dir.delete();
    }

    private final int sendFileContents(Operation op, File fileinfo) {
        Log.d(TAG, "sendFile + = " + fileinfo.getName());
        int position = 0;
        int readLength = 0;
        int outputBufferSize = op.getMaxPacketSize();
        byte[] buffer = new byte[outputBufferSize];
        try {
            FileInputStream fileInputStream = new FileInputStream(fileinfo);
            OutputStream outputStream = op.openOutputStream();
            BufferedInputStream bis = new BufferedInputStream(fileInputStream, 16384);
            long starttimestamp = System.currentTimeMillis();
            while (true) {
                try {
                    if (position == fileinfo.length()) {
                        break;
                    }
                    if (sIsAborted) {
                        break;
                    }
                    long timestamp = System.currentTimeMillis();
                    if (position != fileinfo.length()) {
                        readLength = bis.read(buffer, 0, outputBufferSize);
                    }
                    Log.d(TAG, "Read File");
                    outputStream.write(buffer, 0, readLength);
                    position += readLength;
                    Log.v(TAG, "Sending file position = " + position + " readLength " + readLength + " bytes took " + (System.currentTimeMillis() - timestamp) + " ms");
                } catch (IOException e) {
                    Log.e(TAG, "Write aborted " + e.toString());
                    Log.d(TAG, "Write Abort Received");
                    ((ServerOperation) op).isAborted = true;
                    return BluetoothShare.STATUS_RUNNING;
                }
            }
            long finishtimestamp = System.currentTimeMillis();
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e2) {
                    Log.e(TAG, "input stream close" + e2.toString());
                    Log.d(TAG, "Error when closing stream after send");
                    return 208;
                }
            }
            if (!closeStream(outputStream, op)) {
                return 208;
            }
            Log.d(TAG, "sendFile - position = " + position);
            if (position == fileinfo.length()) {
                Log.i(TAG, "Get Request TP analysis : Transmitted " + position + " bytes in" + (finishtimestamp - starttimestamp) + "ms");
                return 160;
            }
            return 144;
        } catch (IOException e3) {
            Log.e(TAG, "SendFilecontents open stream " + e3.toString());
            return 208;
        }
    }

    private final boolean doesPathExist(String str) {
        Log.d(TAG, "doesPathExist + = " + str);
        File searchfolder = new File(str);
        return searchfolder.exists();
    }

    private final boolean checkMountedState() {
        String state = Environment.getExternalStorageState();
        if ("mounted".equals(state)) {
            return true;
        }
        Log.d(TAG, "SD card Media not mounted");
        return false;
    }

    private final boolean checkAvailableSpace(long filelength) {
        StatFs stat = new StatFs(ROOT_FOLDER_PATH);
        Log.d(TAG, "stat.getAvailableBlocks() " + stat.getAvailableBlocks());
        Log.d(TAG, "stat.getBlockSize() =" + stat.getBlockSize());
        long availabledisksize = ((long) stat.getBlockSize()) * (((long) stat.getAvailableBlocks()) - 4);
        Log.d(TAG, "Disk size = " + availabledisksize + "File length = " + filelength);
        if (((long) stat.getBlockSize()) * (((long) stat.getAvailableBlocks()) - 4) >= filelength) {
            return true;
        }
        Log.d(TAG, "Not Enough Space hence can't receive the file");
        return false;
    }

    private final int pushBytes(Operation op, String folderlistString) {
        Log.d(TAG, "pushBytes +");
        if (folderlistString == null) {
            Log.d(TAG, "folderlistString is null!");
            return 160;
        }
        int folderlistStringLen = folderlistString.length();
        Log.d(TAG, "Send Data: len=" + folderlistStringLen);
        int pushResult = 160;
        try {
            OutputStream outputStream = op.openOutputStream();
            int position = 0;
            int outputBufferSize = op.getMaxPacketSize();
            Log.v(TAG, "outputBufferSize = " + outputBufferSize);
            while (true) {
                if (position == folderlistStringLen) {
                    break;
                }
                if (sIsAborted) {
                    ((ServerOperation) op).isAborted = true;
                    sIsAborted = false;
                    break;
                }
                long timestamp = System.currentTimeMillis();
                int readLength = outputBufferSize;
                if (folderlistStringLen - position < outputBufferSize) {
                    readLength = folderlistStringLen - position;
                }
                String subStr = folderlistString.substring(position, position + readLength);
                try {
                    outputStream.write(subStr.getBytes(), 0, subStr.getBytes().length);
                    Log.d(TAG, "Sending folderlist String position = " + position + " readLength " + readLength + " bytes took " + (System.currentTimeMillis() - timestamp) + " ms");
                    position += readLength;
                } catch (IOException e) {
                    Log.e(TAG, "write outputstrem failed" + e.toString());
                    pushResult = 208;
                }
            }
            Log.v(TAG, "Send Data complete!");
            if (!closeStream(outputStream, op)) {
                pushResult = 208;
            }
            Log.v(TAG, "pushBytes - result = " + pushResult);
            return pushResult;
        } catch (IOException e2) {
            Log.e(TAG, "open outputstrem failed" + e2.toString());
            return 208;
        }
    }

    private final String convertMonthtoDigit(String Month) {
        if (Month.compareTo("Jan") == 0) {
            return "01";
        }
        if (Month.compareTo("Feb") == 0) {
            return "02";
        }
        if (Month.compareTo("Mar") == 0) {
            return "03";
        }
        if (Month.compareTo("Apr") == 0) {
            return "04";
        }
        if (Month.compareTo("May") == 0) {
            return "05";
        }
        if (Month.compareTo("Jun") == 0) {
            return "06";
        }
        if (Month.compareTo("Jul") == 0) {
            return "07";
        }
        if (Month.compareTo("Aug") == 0) {
            return "08";
        }
        if (Month.compareTo("Sep") == 0) {
            return "09";
        }
        if (Month.compareTo("Oct") == 0) {
            return "10";
        }
        if (Month.compareTo("Nov") == 0) {
            return "11";
        }
        if (Month.compareTo("Dec") == 0) {
            return "12";
        }
        return "00";
    }

    private final int sendFolderListingXml(int type, Operation op, File[] files) {
        Log.v(TAG, "sendFolderListingXml =" + files.length);
        StringBuilder result = new StringBuilder();
        result.append("<?xml version=\"1.0\"?>");
        result.append('\r');
        result.append('\n');
        result.append("<!DOCTYPE folder-listing SYSTEM \"obex-folder-listing.dtd\">");
        result.append('\r');
        result.append('\n');
        result.append("<folder-listing version=\"1.0\">");
        result.append('\r');
        result.append('\n');
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                String dirperm = "";
                if (files[i].canRead() && files[i].canWrite()) {
                    dirperm = "RW";
                } else if (files[i].canRead()) {
                    dirperm = "R";
                } else if (files[i].canWrite()) {
                    dirperm = "W";
                }
                Date date = new Date(files[i].lastModified());
                int len = date.toString().length();
                StringBuffer xmldateformat = new StringBuffer(date.toString().substring(len - 4, len));
                xmldateformat.append(convertMonthtoDigit(date.toString().substring(4, 7)));
                xmldateformat.append(date.toString().substring(8, 10));
                xmldateformat.append("T");
                xmldateformat.append(date.toString().substring(11, 13));
                xmldateformat.append(date.toString().substring(14, 16));
                xmldateformat.append("00Z");
                Log.d(TAG, "<folder name = " + files[i].getName() + " size = " + files[i].length() + "modified = " + date.toString() + "xmldateformat.toString() = " + xmldateformat.toString());
                result.append("<folder name=\"" + files[i].getName() + "\" size=\"" + files[i].length() + "\" user-perm=\"" + dirperm + "\" modified=\"" + xmldateformat.toString() + "\"/>");
                result.append('\r');
                result.append('\n');
            } else {
                String userperm = "";
                if (files[i].canRead() && files[i].canWrite()) {
                    userperm = "RW";
                } else if (files[i].canRead()) {
                    userperm = "R";
                } else if (files[i].canWrite()) {
                    userperm = "W";
                }
                Date date2 = new Date(files[i].lastModified());
                int len2 = date2.toString().length();
                StringBuffer xmldateformat2 = new StringBuffer(date2.toString().substring(len2 - 4, len2));
                xmldateformat2.append(convertMonthtoDigit(date2.toString().substring(4, 7)));
                xmldateformat2.append(date2.toString().substring(8, 10));
                xmldateformat2.append("T");
                xmldateformat2.append(date2.toString().substring(11, 13));
                xmldateformat2.append(date2.toString().substring(14, 16));
                xmldateformat2.append("00Z");
                Log.d(TAG, "<file name = " + files[i].getName() + "size = " + files[i].length() + "Append user-perm = " + userperm + "Date in string format = " + date2.toString() + "files[i].modifieddate = " + xmldateformat2.toString());
                result.append("<file name=\"" + files[i].getName() + "\" size=\"" + files[i].length() + "\" user-perm=\"" + userperm + "\" modified=\"" + xmldateformat2.toString() + "\"/>");
                result.append('\r');
                result.append('\n');
            }
        }
        result.append("</folder-listing>");
        result.append('\r');
        result.append('\n');
        Log.d(TAG, "sendFolderListingXml -");
        return pushBytes(op, result.toString());
    }

    public static boolean closeStream(OutputStream out, Operation op) {
        boolean returnvalue = true;
        Log.d(TAG, "closeoutStream +");
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "outputStream close failed" + e.toString());
                returnvalue = false;
            }
        }
        if (op != null) {
            try {
                op.close();
            } catch (IOException e2) {
                Log.e(TAG, "operation close failed" + e2.toString());
                returnvalue = false;
            }
        }
        Log.d(TAG, "closeoutStream -");
        return returnvalue;
    }

    public static boolean closeStream(InputStream in, Operation op) {
        boolean returnvalue = true;
        Log.d(TAG, "closeinStream +");
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "inputStream close failed" + e.toString());
                returnvalue = false;
            }
        }
        if (op != null) {
            try {
                op.close();
            } catch (IOException e2) {
                Log.e(TAG, "operation close failed" + e2.toString());
                returnvalue = false;
            }
        }
        Log.d(TAG, "closeinStream -");
        return returnvalue;
    }
}
