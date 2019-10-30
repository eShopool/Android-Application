package com.eShopool.AndroidApp.Mainpages;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;


import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.ToastCenterText;

import androidx.core.content.FileProvider;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.LoginPages.SetPasswordActivity;
import com.eShopool.AndroidApp.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import ytx.org.apache.http.Header;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to publish an item.
 */
public class NewItemActivity extends BasicActivity {
    protected static final int CHOOSE_PICTURE = 0;
    protected static final int TAKE_PICTURE = 1;
    private static final int STATUS_ERROR = -1;
    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    protected static Uri tempUri;
    private ImageView mImage;
    private Bitmap mBitmap;
    private EditText edtTitle;
    private EditText edtPrice;
    private EditText edtAmt;
    private EditText edtDesc;
    private RadioButton btnNecessities;
    private RadioButton btnElectronics;
    private RadioButton btnCosmetic;
    private String ip;
    private ToastCenterText text = new ToastCenterText();
    private GoToNextActivity thisActivity = new GoToNextActivity(this);

    /*
     * This handler could handle different status and give the corresponding response.
     */
    private Handler itemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    text.displayToast(NewItemActivity.this,"Publish successfully!");
                    thisActivity.toNextActivityWithoutParameter(HomepageActivity.class);
                    break;
                case STATUS_ERROR:
                    text.displayToast(NewItemActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(NewItemActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the splash.
     * The duration is 3 seconds.
     * @param savedInstanceState current state of the instance.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);
        mImage = findViewById(R.id.iv_image);
        edtTitle = findViewById(R.id.edt_title);
        edtPrice = findViewById(R.id.et_price);
        edtAmt = findViewById(R.id.et_amount);
        edtDesc = findViewById(R.id.edt_desc);
        btnCosmetic = findViewById(R.id.btn_cosmetic);
        btnNecessities = findViewById(R.id.btn_necessity);
        btnElectronics = findViewById(R.id.btn_electronic);

        LoadTomcatBalance lb = new LoadTomcatBalance();
        ip = "";
        while(ip.equals("")){
            ip = lb.getIP();
        }
    }

    // Set cancel button.
    public void cancel(View view) {
        super.onBackPressed();
    }

    // Show a dialog to choose photo.
    public void selectAPhoto(View view) {
        showChoosePicDialog();
    }

    /**
     * This function is used to display a dialog for the user to choose a photo.
     */
    protected void showChoosePicDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_a_photo));
        String[] items = { getString(R.string.select_from_album), getString(R.string.camera) };
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case CHOOSE_PICTURE: // // Select a local photo.
                    Intent openAlbumIntent = new Intent(
                            Intent.ACTION_GET_CONTENT);
                    openAlbumIntent.setType("image/*");
                    /*
                     * Use the startActivityForResult method and rewrite the onActivityResult() method
                     * then get the image to do the cropping operation.
                     */
                    startActivityForResult(openAlbumIntent, CHOOSE_PICTURE);
                    break;
                case TAKE_PICTURE: // take a photo.
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/test/" + System.currentTimeMillis() + ".jpg");
//                    file.getParentFile().mkdirs();
                    Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    tempUri = FileProvider.getUriForFile(this, "com.eShopool.AndroidApp.Mainpages.NewItemActivity.fileprovider", file);
                    // Add permissions.
                    openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
                    startActivityForResult(openCameraIntent, TAKE_PICTURE);
                    break;
            }
        });
        builder.show();
    }

    /**
     * This function is used to respond to the photo crop request.
     * @param requestCode the request code.
     * @param resultCode the result code.
     * @param data the metadata
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                setImageToView(result.getUri());
                return;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        switch (requestCode) {
            case TAKE_PICTURE:
                break;
            case CHOOSE_PICTURE:
                // Original resource address of the photo.
                tempUri = data.getData();
                Log.e("pic",tempUri.toString());
                break;
        }
        if(resultCode == NewItemActivity.RESULT_OK){
            CropImage.activity(tempUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }

    }

    /**
     * This function is used to set image view.
     * @param uri the given url address.
     */
    protected void setImageToView(Uri uri) {

        ContentResolver resolver = getContentResolver();
        try {
            mBitmap = MediaStore.Images.Media.getBitmap(resolver, tempUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mImage.setImageBitmap(mBitmap);
    }

    /**
     * This method is used to send image to the Tomcat server.
     * @param bmap the chosen Bitmap.
     * @param name the UUID of the Bitmap.
     */
    private void sendImage(Bitmap bmap, String name) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        byte[] bytes = stream.toByteArray();
        String img = Base64.encodeToString(bytes, Base64.DEFAULT);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("filename", name);
        params.add("img", img);
        client.post("http://" + ip + ":8080/WebPicStream/UpdatePhoto", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, org.apache.http.Header[] headers, byte[] bytes) {

            }

            @Override
            public void onFailure(int i, org.apache.http.Header[] headers, byte[] bytes, Throwable throwable) {
                Toast.makeText(NewItemActivity.this, Integer.toString(i), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This method is used to send publish request to SOAP web service.
     * @param view
     */
    public void saveItem(View view) {
        String title = edtTitle.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String amount = edtAmt.getText().toString().trim();
        String desc = edtDesc.getText().toString().trim();
        String img_id = UUID.randomUUID().toString();
        String label = (btnNecessities.isChecked())?"Necessities":(btnElectronics.isChecked())?"Electronics":(btnCosmetic.isChecked())?"Cosmetic":"Homepage";
        if(desc.equals("")) desc = " ";
        if(TextUtils.isEmpty(title)) text.displayToast(this,getString(R.string.title_cannot_empty));
        else if(TextUtils.isEmpty(price)) text.displayToast(this,getString(R.string.price_cannot_empty));
        else if(Double.parseDouble(price) < 0.01) text.displayToast(this,getString(R.string.price_is_invalid));
        else if(amount.equals("") || Integer.parseInt(amount) < 1) text.displayToast(this,getString(R.string.amount_is_invalid));
        else {
            SharedPreferences sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
            String pubkey = sp.getString("pubkey","");
            sendImage(mBitmap, img_id);
            String finalDesc = desc;
            new Thread(){
                @Override
                public void run() {

                    try {
                        HttpURLConnection conn = SoapRequest.request("addItem", pubkey, title, label, price, amount, img_id, finalDesc);
                        if (conn == null){
                            Message msg = Message.obtain();
                            msg.what = NETWORK_ERROR;
                            itemHandler.sendMessage(msg);
                        } else {
                            int code = conn.getResponseCode();
                            if (code == 200) {
                                Message msg = Message.obtain();
                                msg.what = STATUS_SUCCESS;
                                itemHandler.sendMessage(msg);
                            }
                            else {
                                Message msg = Message.obtain();
                                msg.what = STATUS_ERROR;
                                msg.obj = code;
                                itemHandler.sendMessage(msg);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        itemHandler.sendMessage(msg);
                    }
                }
            }.start();
        }

    }
}
