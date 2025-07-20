package com.example.tradeup.Controller;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SendGridEmailSender {
    private static final String TAG = "SendGridEmailSender";
    // Lưu API Key trong BuildConfig (thêm vào build.gradle)
    private static final String SENDGRID_API_KEY = "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";
    private static final String FROM_EMAIL = "ungdungtradeup@gmail.com";

    public interface EmailCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void sendEmail(Context context, String toEmail, String subject, String message, EmailCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://api.sendgrid.com/v3/mail/send";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Email sent successfully");
                    callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Error sending email: " + error.toString());
                    callback.onError(error.toString());
                }) {

            @Override
            public byte[] getBody() {
                try {
                    JSONObject body = new JSONObject();
                    JSONObject from = new JSONObject().put("email", FROM_EMAIL);
                    JSONObject to = new JSONObject().put("email", toEmail);
                    JSONArray tos = new JSONArray().put(to);
                    JSONObject personalization = new JSONObject().put("to", tos);
                    JSONArray personalizations = new JSONArray().put(personalization);
                    JSONArray content = new JSONArray();
                    JSONObject contentItem = new JSONObject()
                            .put("type", "text/plain")
                            .put("value", message);
                    content.put(contentItem);

                    body.put("from", from)
                            .put("subject", subject)
                            .put("personalizations", personalizations)
                            .put("content", content);

                    return body.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "JSON Error", e);
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + SENDGRID_API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        queue.add(request);
    }
}