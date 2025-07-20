package com.example.tradeup.Controller;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class UploadImgItem {

    private final Cloudinary cloudinary;

    public UploadImgItem() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
        ));
    }

    public Task<String> uploadImageToItemFolder(Context context, Uri imageUri) {
        TaskCompletionSource<String> taskSource = new TaskCompletionSource<>();

        new Thread(() -> {
            try {
                File file = createTempFileFromUri(context, imageUri);
                Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                        "folder", "item"
                ));
                String imageUrl = (String) uploadResult.get("secure_url");
                taskSource.setResult(imageUrl);
            } catch (Exception e) {
                Log.e("UploadImgItem", "Upload failed: " + e.getMessage());
                taskSource.setException(e);
            }
        }).start();

        return taskSource.getTask();
    }

    private File createTempFileFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }

        outputStream.close();
        inputStream.close();

        return tempFile;
    }
}
