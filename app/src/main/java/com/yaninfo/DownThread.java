package com.yaninfo;

/**
 * @Author: zhangyan
 * @Date: 2019/4/2 10:46
 * @Description: 下载线程
 * @Version: 1.0
 */

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class DownThread implements Runnable {


    private final static String TAG = "DownThread";
    private String mUrl;
    private Random mRandom = new Random();
    private DownloadManager mDownloadManager;
    private Context mContext;



    public DownThread(String dlUrl) {
        this.mUrl = dlUrl;
    }

    @Override
    public void run() {
        Log.e(TAG, "开始下载#########");
        InputStream in = null;
        FileOutputStream out = null;

        try {

            URL httpUrl = new URL(mUrl);
            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();
            in = conn.getInputStream();
            String downloadFile;
            File file;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.e(TAG, "SD卡可写#########");
                // 存到系统download文件夹
                downloadFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                // 获取十以内的随机数
                int random = mRandom.nextInt(10);
                // 合并成文件名
                file = new File(downloadFile, "/test"+random+".apk");
                out = new FileOutputStream(file);
            } else {
                Log.e(TAG, "SD卡不存在或者不可读写#########");
            }
            byte[] buffer = new byte[1024];
            int len;
            // 遍历
            while ((len = in.read(buffer)) != -1) {
                if (out != null) {
                    out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.e(TAG, "下载完毕#########");
    }



    /**
     * 模拟多线程
     *
     * @throws Exception
     */
    public void mulDownload() throws Exception {

        //设置URL的地址和下载后的文件名
        String filename = "meitu.exe";
        String path = "http://10.13.20.32:8080/Test/XiuXiu_Green.exe";
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        //获得需要下载的文件的长度(大小)
        int filelength = conn.getContentLength();
        System.out.println("要下载的文件长度" + filelength);
        //生成一个大小相同的本地文件
        RandomAccessFile file = new RandomAccessFile(filename, "rwd");
        file.setLength(filelength);
        file.close();
        conn.disconnect();
        //设置有多少条线程下载
        int threadsize = 3;
        //计算每个线程下载的量
        int threadlength = filelength % 3 == 0 ? filelength / 3 : filelength + 1;
        for (int i = 0; i < threadsize; i++) {
            //设置每条线程从哪个位置开始下载
            int startposition = i * threadlength;
            //从文件的什么位置开始写入数据
            RandomAccessFile threadfile = new RandomAccessFile(filename, "rwd");
            threadfile.seek(startposition);
            //启动三条线程分别从startposition位置开始下载文件
            //  new DownLoadThread(i,startposition,threadfile,threadlength,path).start();
        }
        int quit = System.in.read();
        while ('q' != quit) {
            Thread.sleep(2000);
        }
    }

}
