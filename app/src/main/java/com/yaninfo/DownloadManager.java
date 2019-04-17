package com.yaninfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.webkit.CookieManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: zhangyan
 * @Date: 2019/4/16 11:20
 * @Description:
 * @Version: 1.0
 */
public class DownloadManager {

    private android.app.DownloadManager mDownloadManager;
    private Context mContext = null;
    private DownloadChangeObserver mDownloadObserver = null;
    private ScheduledExecutorService mScheduledExecutorService = null;
    private DownLoadBroadcast mDownLoadBroadcast = null;

    public static final int HANDLE_DOWNLOAD = 0x001;
    public static final float DOWN_COMPELETE = 2.0F;

    private long mDownloadId = 0;
    private DownloadStatus onDownloadStatus = null;

    public DownloadManager(Context context) {
        mContext = context;
        mDownLoadBroadcast = new DownLoadBroadcast();
    }

    /**
     * 定义handler更新UI
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (onDownloadStatus != null && HANDLE_DOWNLOAD == msg.what) {
                //被除数可以为0，除数必须大于0
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    onDownloadStatus.onProgress(msg.arg1 / (float) msg.arg2);
                }
            }
        }
    };

    /**
     * 下载
     * @param contentDisposition
     * @param url
     */
    public void download(String contentDisposition, String url) {
        mDownloadManager = (android.app.DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadObserver = new DownloadChangeObserver();

        registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, mDownloadObserver);

        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
        /**设置用于下载时的网络状态*/
        request.setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI | android.app.DownloadManager.Request.NETWORK_MOBILE);
        /**设置通知栏是否可见*/
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        /**设置漫游状态下是否可以下载*/
        request.setAllowedOverRoaming(false);
        /**如果我们希望下载的文件可以被系统的Downloads应用扫描到并管理，
         我们需要调用Request对象的setVisibleInDownloadsUi方法，传递参数true.*/
        request.setVisibleInDownloadsUi(true);
        /**设置文件保存路径*/
        String cookie = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("Cookie", cookie);
        String fileName = "Default";
        if (contentDisposition.indexOf("filename*=UTF-8") != -1) {
            fileName = contentDisposition.substring(contentDisposition.indexOf("'") + 2, contentDisposition.length());
        } else {
            fileName = contentDisposition.substring(contentDisposition.indexOf("=") + 1, contentDisposition.length());
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        // 将下载请求放入队列，return下载任务的id
        mDownloadId = mDownloadManager.enqueue(request);

        registerBroadcast();

    }

    /**
     * 对外开发的方法
     *
     * @param downloadStatus
     */
    public void setOnProgressListener(DownloadStatus downloadStatus) {
        this.onDownloadStatus = downloadStatus;
    }


    /**
     * 创建ContentObserve实例，监听下载进度
     */
    private class DownloadChangeObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         */
        public DownloadChangeObserver() {
            super(mHandler);
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        @Override
        public void onChange(boolean selfChange) {
            mScheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0, 2, TimeUnit.SECONDS);
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver(Uri url, boolean notifyForDescendants, DownloadChangeObserver contentResolver) {
        /** observer download change **/
        if (mDownloadObserver != null) {
            mContext.getContentResolver().registerContentObserver(url, notifyForDescendants, contentResolver);
        }
    }

    /**
     * 注册广播
     */
    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED);
        mContext.registerReceiver(mDownLoadBroadcast, filter);
    }

    /**
     * 接受下载完成广播
     */
    private class DownLoadBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    /**
     * 发送Handler消息更新进度和状态
     */
    private void updateProgress() {
        int[] bytesAndStatus = getBytesAndStatus(mDownloadId);
        mHandler.sendMessage(mHandler.obtainMessage(HANDLE_DOWNLOAD, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]));
    }

    public void stop() {
        mContext.unregisterReceiver(mDownLoadBroadcast);
        if (mDownloadObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mDownloadObserver);
        }
    }


    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId
     * @return
     */
    private int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{
                -1, -1, 0
        };
        android.app.DownloadManager.Query query = new android.app.DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = mDownloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载文件大小
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //下载文件的总大小
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                //下载状态
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bytesAndStatus;
    }

}
