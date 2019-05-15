package com.lwy.ocrdemo.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    public static void assets2SDCard(Context context, String assetsFileName, String destFilePath) throws IOException {
        File trainFile = new File(destFilePath);
        if (!trainFile.exists()) {
            trainFile.createNewFile();
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                is = context.getAssets().open(assetsFileName);
                byte[] bytes = new byte[1024];
                int length = 0;
                fos = new FileOutputStream(trainFile);
                while ((length = is.read(bytes)) != -1) {

                    fos.write(bytes, 0, length);

                }
                fos.flush();
            } catch (IOException e) {
                throw e;
            } finally {
                try {
                    if (is != null)
                        is.close();
                    if (fos != null) {

                        fos.close();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
