package com.eShopool.AndroidApp.LoginPages;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.Mainpages.HomepageActivity;
import com.eShopool.AndroidApp.R;
import com.eShopool.AndroidApp.sdk.CCPRestSDK;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is a tool which is used to help the user login.
 */
public class LoginActivity extends BasicActivity {

    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final int STATUS_ERROR = -1;
    private static final int requestStorageCode = 7;
    private static final String regex = "<return>(.*)</return>";

    private EditText userPhoneNum;
    private EditText userPwd;
    private ImageView imgEye;
    private ImageView mImage;
    private SharedPreferences sp;
    private Switch remember_me;
    private Bitmap bitmap;
    private String pubkey;
    private String phoneNum;
    private String password;
    private String encryptedPhone;
    private String ip;
    private Pattern r = Pattern.compile(regex);
    private boolean isOpenEye = false;
    private GoToNextActivity thisActivity = new GoToNextActivity(this);
    private ToastCenterText text = new ToastCenterText();

    /*
     * This handler could make a bitmap if the file exists.
     */
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            File file = (File)msg.obj;
            try {
                bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                mImage.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                mImage.setImageResource(R.mipmap.user);
            } catch (NullPointerException e) {
                mImage.setImageResource(R.mipmap.user);
            }
        }
    };
    /*
     * This handler could handle different password matching status and give the corresponding response.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    String result = (String)msg.obj;
                    Matcher m = r.matcher(result);
                    if (m.find()) result = m.group(1);
                    else result = "";
                    if (result.equals("success")) thisActivity.toNextActivityWithoutParameter(HomepageActivity.class);
                    else text.displayToast(LoginActivity.this, "Your password is incorrect.");
                    break;
                case STATUS_ERROR:
                    text.displayToast(LoginActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could handle different public key status and give the corresponding response.
     */
    private Handler accountHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    if(pubkey.equals("")) requestPublicKey();
                    else {
                        if(remember_me.isChecked()) saveSharePreferences();
                        requestLogin();
                    }
                    break;
                case STATUS_ERROR:
                    text.displayToast(LoginActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(LoginActivity.this,getString(R.string.network_error));
                default:
                    break;
            }
        }
    };

    /*
     * This handler could handle different SharePreferences status and give the corresponding response.
     */
    private Handler keyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    // Create match object.
                    Matcher m = r.matcher((String)msg.obj);
                    if (m.find()) pubkey = m.group(1);
                    else pubkey = "";
                    if(remember_me.isChecked()) saveSharePreferences();
                    requestLogin();
                    break;
                case STATUS_ERROR:
                    text.displayToast(LoginActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(LoginActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the login page.
     * @param savedInstanceState current state of the instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userPhoneNum = findViewById(R.id.et_phone);
        userPwd = findViewById(R.id.et_pwd);
        remember_me = findViewById(R.id.re_me);
        imgEye = findViewById(R.id.btn_close);
        mImage = findViewById(R.id.img_user);

        // Find the SharedPreferences.
        sp = this.getSharedPreferences("config", this.MODE_PRIVATE);

        LoadTomcatBalance lb = new LoadTomcatBalance();
        ip = "";
        while(ip.equals("")){
            ip = lb.getIP();
        }
        // Add "hide password" button listener.
        imgEye.setOnClickListener(v -> {
            if (!isOpenEye) {
                imgEye.setSelected(true);
                isOpenEye = true;
                // Make password visible.
                userPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                imgEye.setSelected(false);
                isOpenEye = false;
                //Make password invisible.
                userPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        // Check whether external storage permission has been allowed or not.
        checkStoragePermission();

        // Store the password and phone number.
        restoreInfo();
    }

    /**
     * This function could help to remind users to open external storage permission.
     */
    private void checkStoragePermission() {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestStorageCode);
    }

    /**
     * The function is used to remember the user if TA clicks "remember me" button.
     */
    public void restoreInfo(){
        if (sp.getBoolean("remember",false)) remember_me.setChecked(true);
        phoneNum = sp.getString("phone", "");
        password = sp.getString("password", "");
        pubkey = sp.getString("pubkey","");
        userPhoneNum.setText(phoneNum);
        userPwd.setText(password);
        if(!TextUtils.isEmpty(phoneNum)){
            new Thread(){
                @Override
                public void run() {
                    Message message = handler.obtainMessage();
                    Encoder encode = new Encoder();
                    encryptedPhone = encode.Base64Encode(phoneNum);
                    message.obj = downloadImg("http://" + ip + ":8080/WebPicStream/GetPortrait?phone=" + encryptedPhone);
                    message.sendToTarget();
                }
            }.start();
        }

    }

    /**
     * This function is used to download the user's portrait from our web server.
     * @param urlStr the address where stores the user's portrait.
     * @return a temp file which contains the user's portrait.
     */
    public File downloadImg(String urlStr) {
        File file = null ;
        try {
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(3000);
            InputStream input = urlConnection.getInputStream();
            file = File.createTempFile("temp_head", "jpg");
            OutputStream output = new FileOutputStream(file) ;
            byte[] byt = new byte[1024];
            int length = 0;
            // Start reading.
            while ((length = input.read(byt)) != -1) {
                output.write(byt, 0, length);
            }
            input.close();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * This method is used to request a public key from SOAP web service.
     */
    public void requestPublicKey(){
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("requestPublicKey", encryptedPhone);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        keyHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        Message msg = Message.obtain();
                        InputStream is = conn.getInputStream();
                        String result = StreamUtils.readStream(is);
                        if (code == 200) {
                            msg.what = STATUS_SUCCESS;
                            msg.obj= result;
                            keyHandler.sendMessage(msg);
                        }
                        else {
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            keyHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    keyHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This method is used to send a login request to SOAP web service.
     */
    public void requestLogin(){
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("checkPwd",
                            pubkey,
                            RSAUtil.getCryptoText(pubkey, password));
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        mHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            mHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            mHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    mHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This function is used to check the user's input data and make a decision whether let user in
     * or not.
     * @param view the current context.
     */
    public void login(View view) {
        // Get the user's phone number.
        phoneNum = userPhoneNum.getText().toString().trim();

        // Get the user's password.
        password = userPwd.getText().toString().trim();

        // If user enters any empty data entry, alert the user with a warning.
        if(TextUtils.isEmpty(phoneNum)||TextUtils.isEmpty(password)){
            text.displayToast(this,getString(R.string.phone_pwd_cannot_empty));
        } else{
            checkAccountExist();
        }
    }

    /**
     * This method is used to check account status.
     */
    private void checkAccountExist() {
        Encoder encode = new Encoder();
        encryptedPhone = encode.Base64Encode(phoneNum);
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("checkPhoneNumber", encryptedPhone);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        accountHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        Message msg = Message.obtain();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Matcher m = r.matcher(result);
                            if (m.find()) result = m.group(1);
                            else result = "";
                            if(result.equals("success")){
                                msg.what = STATUS_SUCCESS;
                            } else {
                                msg.what = STATUS_ERROR;
                                msg.obj = "Your account does not exist!";
                            }
                            accountHandler.sendMessage(msg);
                        }
                        else {
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            accountHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    accountHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This method is used to save SharePreferences.
     */
    public void saveSharePreferences(){
        // Need to store the phone number and password if user clicks "remember" button.
        // Save these data to the SharedPreferences file.
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("phone", phoneNum);
        editor.putBoolean("remember",true);
        editor.putString("password", password);
        editor.putString("pubkey",pubkey);
        editor.apply();
    }

    /**
     * This function extends super method.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * This function is used to click "forget" button then app turn to another page.
     * @param view the current context.
     */
    public void forget(View view) {
        thisActivity.toNextActivityWithoutParameter(SetPasswordActivity.class);
    }

    /**
     * This function is used to click "sign up" button then app turn to another page.
     * @param view the current context.
     */
    public void signUp(View view) {
        thisActivity.toNextActivityWithoutParameter(SetPasswordActivity.class);
    }
}
