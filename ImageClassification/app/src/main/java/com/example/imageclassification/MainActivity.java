package com.example.imageclassification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Bitmap bitmap;
    private NotificationManager notificationManager;
    private boolean background;
    private Uri photoURI;
    private String toGetKey = "com.API.Key";
    private String userAPIKeyValue = "";
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.imageclassification";
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.rotate).setEnabled(false);
        findViewById(R.id.upload).setEnabled(false);
        createNotificationChannel();
        background = false;
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        userAPIKeyValue = mPreferences.getString(toGetKey,userAPIKeyValue);

        if(userAPIKeyValue.equals(""))
            popup("Authorization");


    }

    @Override
    protected void onResume() {
        super.onResume();
        background = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        background = true;

        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.putString(toGetKey,userAPIKeyValue);
        preferencesEditor.apply();
    }

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Internet Permission Granted", Toast.LENGTH_SHORT) .show();
            }
            else {
                Toast.makeText(MainActivity.this, "Internet Permission Denied", Toast.LENGTH_SHORT) .show();
            }
        }

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT) .show();
            }
            else {
                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT) .show();
            }
        }
    }


    public void uploadImg(View view){
        findViewById(R.id.upload).setEnabled(false);
        checkPermission(Manifest.permission.INTERNET, 100);
        String postUrl= "https://192.168.7.150/";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "predict.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        TextView responseText = findViewById(R.id.responseText);
        responseText.setText("Please wait ...");

        postRequest(postUrl, postBodyImage);
    }

    private void postRequest(String postUrl, RequestBody postBody) {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");

        } catch (Exception e) {
            e.printStackTrace();
        }
//        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.hostnameVerifier((hostname, session) -> true);
        OkHttpClient client = builder.build();
        //OkHttpClient client = new OkHttpClient().newBuilder().hostnameVerifier((hostname, session) -> true).build();


        Request request = new Request.Builder().url(postUrl).post(postBody).header("Authorization",userAPIKeyValue).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();
                e.printStackTrace();
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        responseText.setText("Failed to Connect to Server");
                        if(background)
                            sendNotification("Connection failed", "Could not connect to the server.");
                        findViewById(R.id.upload).setEnabled(true);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                String reply = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        try {

                            responseText.setText(reply);
                            if(background)
                                if (reply != "Unauthorized")
                                    sendNotification("Classification ready", "The object is " + reply);
                                else
                                    sendNotification("Connection failed", reply);
                            if(reply.equals("Unauthorized"))
                                popup("Authorization Failed");


                            findViewById(R.id.upload).setEnabled(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void getImg(View view) {
        checkPermission(Manifest.permission.CAMERA, 101);
        final CharSequence[] options = {"Capture Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Upload Picture");
        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Capture Photo")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createTempFile();
                            if(photoFile == null)
                                throw  new IOException();
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Error occurred while creating the File
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            photoURI = FileProvider.getUriForFile(MainActivity.this,
                                    "com.example.imageclassification.FileProvider",
                                    photoFile);
                            takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(takePicture, 0);
                        }
                    }

                else if (options[item].equals("Choose from Gallery")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , 1);

                }
            }
        });
        builder.show();
    }

    public File createTempFile() throws IOException {

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String imageFileName = "/JPEG_PIC.jpg";
        return new File(storageDir.getPath()+imageFileName);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        switch (reqCode) {
            case 0:
                if (resCode == RESULT_OK && data != null) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    this.bitmap = bitmap;
                    displayImage();
                    findViewById(R.id.rotate).setEnabled(true);
                    findViewById(R.id.upload).setEnabled(true);
                    TextView responseText = findViewById(R.id.responseText);
                    responseText.setText("");
                }
                    break;
            case 1:
                if (resCode == RESULT_OK && data != null) {
                    Uri imageUri = data.getData();
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    this.bitmap = bitmap;
                    displayImage();
                    findViewById(R.id.rotate).setEnabled(true);
                    findViewById(R.id.upload).setEnabled(true);
                    TextView responseText = findViewById(R.id.responseText);
                    responseText.setText("");
                }
        }
    }

    public void rotate(View view) {
        Matrix m = new Matrix();
        m.preRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0 ,bitmap.getWidth(),bitmap.getHeight(), m, true);
        displayImage();
    }

    public void sendNotification(String title, String text){
        NotificationCompat.Builder build = getNotificationBuilder(title, text);
        notificationManager.notify(0, build.build());
    }

    public void createNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel("primary_notification_channel", "Notification", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setDescription("Notification from Image Classification");
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private NotificationCompat.Builder getNotificationBuilder(String title, String text){
        final Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationIntent = PendingIntent.getActivity(this, 0, intent, 0);


        return new NotificationCompat.Builder(this, "primary_notification_channel")
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_not)
                .setContentIntent(notificationIntent)
                .setAutoCancel(true);
    }

    private void popup(String title){
        EditText e = new EditText(MainActivity.this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage("Enter Key:")
                .setView(e)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        userAPIKeyValue = e.getText().toString();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();



    }

    private void displayImage(){
        Bitmap b = bitmap;
        if(b.getHeight()>1450){
            double scale = 1450.0/b.getHeight();
            b = Bitmap.createScaledBitmap(b, (int)(b.getWidth()*scale), (int)(b.getHeight()*scale), true);
        }
        ImageView my_img_view = (ImageView) findViewById (R.id.img);
        my_img_view.setImageBitmap(b);
    }
}