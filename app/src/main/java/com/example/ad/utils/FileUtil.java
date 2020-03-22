package com.example.ad.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import com.example.ad.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtil {
    static {

    }
    private FileUtil() {
    }

    public static void writeToFile(byte[] mix, String path) throws Exception {
        final File f = new File(path);
        if (!f.exists()) {
            f.createNewFile();
        }
        final FileOutputStream os = new FileOutputStream(f);
        os.write(mix);
        os.flush();
    }

    public static byte[] getFile(String path) {
        byte[] result = null;
        try {
            File file = new File(path);
            FileInputStream in = new FileInputStream(file);
            //获取文件的字节数
            int lenght = in.available();
            //创建byte数组
            result = new byte[lenght];
            //将文件中的数据读到byte数组中
            in.read(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] getFromRaw(Activity context, int id) {
        byte[] result = null;
        try {
            InputStream in = context.getResources().openRawResource(id);
            //获取文件的字节数
            int lenght = in.available();
            //创建byte数组
            result = new byte[lenght];
            //将文件中的数据读到byte数组中
            in.read(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
