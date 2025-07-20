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

public class AddImgController {

    private final Context context;
    private final Cloudinary cloudinary;
    private static final String TAG = "CloudinaryUpload";

    public AddImgController(Context context) {
        this.context = context;

        cloudinary = new Cloudinary(ObjectUtils.asMap(
        ));
    }

    public void uploadImageToCloudinary(Uri imageUri, String userId, com.google.android.gms.tasks.OnCompleteListener<Uri> listener) {
        TaskCompletionSource<Uri> taskSource = new TaskCompletionSource<>();

        new Thread(() -> {
            try {
                File file = uriToFile(imageUri);
                if (file == null) {
                    taskSource.setException(new Exception("Lỗi chuyển Uri → File"));
                    return;
                }

                Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                        "folder", "avatars/",
                        "public_id", userId + "_avatar",
                        "resource_type", "image"
                ));

                String imageUrl = uploadResult.get("secure_url").toString();
                taskSource.setResult(Uri.parse(imageUrl));
                Log.d(TAG, "Upload thành công: " + imageUrl);
            } catch (Exception e) {
                Log.e(TAG, "Upload thất bại: " + e.getMessage(), e);
                taskSource.setException(e);
            }
        }).start();

        Task<Uri> task = taskSource.getTask();
        task.addOnCompleteListener(listener);
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File file = File.createTempFile("upload_", ".jpg", context.getCacheDir());
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            inputStream.close();
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.e("FILE_CONVERT", "Lỗi chuyển Uri -> File: " + e.getMessage());
            return null;
        }
    }
}
