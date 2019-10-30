package com.eShopool.AndroidApp.LoginPages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.CountDownTime;
import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.ImageCodeGenerator;
import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.Mainpages.HomepageActivity;
import com.eShopool.AndroidApp.Mainpages.NewItemActivity;
import com.eShopool.AndroidApp.R;
import com.eShopool.AndroidApp.sdk.CCPRestSDK;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is a tool which is used to help the user set password.
 */
public class SetPasswordActivity extends BasicActivity {

    private static final int STATUS_ERROR = -1;
    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final String regex = "<return>(.*)</return>";

    private EditText edtPhone;
    private String password;
    private String phoneNumber;
    private String encryptedPhone;
    private String testImageCode;
    private String SMSCode;
    private String PIN;
    private EditText edtPwd;
    private EditText edtConfirm;
    private EditText edtCode;
    private TextView edtImgCode;
    private ImageView showImageCode;
    private Button btnSend;
    private String pubkey;
    private String ip;
    private CountDownTime onMin;
    private SharedPreferences sp;
    private HashMap<String, Object> result = null;
    private StringBuffer code = new StringBuffer();
    private CCPRestSDK restAPI = new CCPRestSDK();
    private ToastCenterText text = new ToastCenterText();
    private GoToNextActivity thisActivity = new GoToNextActivity(this);

    /*
     * This handler could handle different SMS status and give the corresponding response.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    edtCode.requestFocus();
                    break;
                case STATUS_ERROR:
                    text.displayToast(SetPasswordActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(SetPasswordActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could handle different public key status and give the corresponding response.
     */
    private Handler keyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(regex);
                    Matcher m = r.matcher((String)msg.obj);
                    if (m.find()) pubkey = m.group(1);
                    else pubkey = "";
                    saveSharePreferences();
                    sendPwd();
                    break;
                case STATUS_ERROR:
                    text.displayToast(SetPasswordActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(SetPasswordActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could handle different account status and give the corresponding response.
     */
    private Handler accountHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    text.displayToast(SetPasswordActivity.this,getString(R.string.pwd_set_successfully));
                    thisActivity.toNextActivityWithoutParameter(HomepageActivity.class);
                    break;
                case STATUS_ERROR:
                    text.displayToast(SetPasswordActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(SetPasswordActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        edtPhone = findViewById(R.id.reg_phone);
        edtPwd = findViewById(R.id.edt_set_pwd);
        edtConfirm = findViewById(R.id.edt_confirm);
        edtCode = findViewById(R.id.edt_register_code);
        btnSend = findViewById(R.id.btn_register_send);
        edtImgCode = findViewById(R.id.edt_image_code);
        showImageCode = findViewById(R.id.image_code);
        sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
        pubkey = sp.getString("pubkey","");
        onMin = new CountDownTime(60000, 1000,btnSend);//初始化对象

        restAPI.init("app.cloopen.com", "8883");// 初始化服务器地址和端口，格式如下，服务器地址不需要写https://
        restAPI.setAccount("Your account sid", "Your account token");// 初始化主帐号和主帐号TOKEN
        restAPI.setAppId("Your app id");// 初始化应用ID

        LoadTomcatBalance lb = new LoadTomcatBalance();
        ip = "";
        while(ip.equals("")){
            ip = lb.getIP();
        }
        Log.e("ip",ip);
        // Display captcha.
        changeCode(showImageCode);
    }

    /**
     * This method is used to verify SMS code.
     * @return matching results.
     */
    public boolean verifyCode() {
        String receiveCode = edtCode.getText().toString().trim();
        if(TextUtils.isEmpty(receiveCode))
        {
            text.displayToast(this,getString(R.string.please_enter_sms_code));
            edtCode.requestFocus();
            return false;
        } else if(receiveCode.equals(SMSCode)) {
            return true;
        } else {
            text.displayToast(this,getString(R.string.sms_code_incorrect));
            edtCode.requestFocus();
            return false;
        }
    }

    /**
     * This method is used to generate SMS code.
     * @return SMS code.
     */
    public String generateCode(){
        code.append((int)(Math.random() * 10))
                .append((int)(Math.random() * 10))
                .append((int)(Math.random() * 10))
                .append((int)(Math.random() * 10));
        PIN = code.toString();
        code = new StringBuffer();
        return PIN;
    }

    /**
     * This method is used to check phone number.
     * @return
     */
    public boolean checkPhone(){
        if(TextUtils.isEmpty(phoneNumber)) {
            text.displayToast(this,getString(R.string.please_enter_phone_number));
            edtPhone.requestFocus();
            return false;
        }
        else if(phoneNumber.length() != 11) {
            text.displayToast(this,getString(R.string.phone_digit_incorrect));
            edtPhone.requestFocus();
            return false;
        }
        else return true;
    }

    /**
     * This method is used to request a SMS code.
     * @param view current view.
     */
    public void requestRegCode(View view){
        phoneNumber = edtPhone.getText().toString().trim();
        if(checkPhone())
        {
            onMin.start(); //开始计时
            new Thread(){
                @Override
                public void run() {
                    try {
                        SMSCode = generateCode();
                        result = restAPI.sendTemplateSMS(phoneNumber,"481105" ,new String[]{SMSCode,"5"});
                        Message msg = Message.obtain();
                        if ("000000".equals(result.get("statusCode"))) {
                            msg.what = STATUS_SUCCESS;
                            mHandler.sendMessage(msg);
                        } else {
                            msg.what = STATUS_ERROR;
                            msg.obj = result.get("statusMsg");
                            mHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        mHandler.sendMessage(msg);
                    }
                }
            }.start();
        }
    }

    /**
     * This function is used to find whether password contains any special character.
     * @param str the given password.
     * @return the match result, true or false.
     */
    public boolean stringFilter(String str) {
        String regex = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~!@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern pattern = Pattern.compile(regex );
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }


    /*
     * If the length of password or confirm password is empty, give the user a warning.
     * Else if the password length is less than 8 or larger than 16, give the user a warning.
     * Else if the password do not contain any special character, give the user a warning.
     * Else access the database to add new account.
     */
    public boolean checkPwd(){
        password = edtPwd.getText().toString().trim();
        String confirmPwd = edtConfirm.getText().toString().trim();
        if(TextUtils.isEmpty(password)|| TextUtils.isEmpty(confirmPwd)){
            text.displayToast(this,getString(R.string.pwd_cannot_empty));
        } else if(password.length()<8 ||password.length()>16){
            text.displayToast(this,getString(R.string.password_length_incorrect));
        } else if(!stringFilter(password)){
            text.displayToast(this,getString(R.string.pwd_require_special_character));
        } else if (password.equals(confirmPwd)){
            return true;
        } else {
            text.displayToast(SetPasswordActivity.this,getString(R.string.confirm_pwd_incorrect));
        }
        return false;
    }

    /**
     * This method is used to request a public key.
     */
    public void requestPublicKey(){
        new Thread(){
            @Override
            public void run() {
                try {
                    Encoder encode = new Encoder();
                    encryptedPhone = encode.Base64Encode(phoneNumber);
                    HttpURLConnection conn = SoapRequest.request("requestPublicKey", encryptedPhone);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        keyHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            keyHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
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
     * This method is used to send the user's password.
     */
    public void sendPwd(){
        new Thread(){
            @Override
            public void run() {

                try {
                    HttpURLConnection conn = SoapRequest.request("updateUserPwd", pubkey, RSAUtil.getCryptoText(pubkey, password));
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        accountHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            accountHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
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
     * This function is used to go back to the homepage.
     * @param view the current context.
     */
    public void login(View view){

        String userEnterCode = edtImgCode.getText().toString().trim().toLowerCase();

        if(verifyCode() && checkPwd()){
            if (userEnterCode.equals(testImageCode)){
                if(pubkey.equals("")) {
                    requestPublicKey();
                    createAccount();
                }
               //2。发送加密后的账号和密码给后端并跳转至下一界面
                saveSharePreferences();
                sendPwd();
            }
            else{
                text.displayToast(SetPasswordActivity.this,getString(R.string.sms_code_incorrect));
            }
        }
    }

    /**
     * This method is used to create a account.
     */
    public void createAccount(){
        Encoder encode = new Encoder();
        encryptedPhone = encode.Base64Encode(phoneNumber);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("phone", encryptedPhone);
        client.post("http://" + ip + ":8080/WebPicStream/CreateAccount", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, org.apache.http.Header[] headers, byte[] bytes) {

            }

            @Override
            public void onFailure(int i, org.apache.http.Header[] headers, byte[] bytes, Throwable throwable) {
            }
        });
    }

    /**
     * This function is used to simulate the android's back control button.
     * This back button could take the user to the login page.
     */
    @Override
    public void onBackPressed(){
        thisActivity.toNextActivityWithoutParameter(LoginActivity.class);
    }

    /**
     * This function is used to go back to the login page.
     * @param view the current context.
     */
    public void cancel(View view){

        super.onBackPressed();

    }

    /**
     * This method is used to save SharePreferences.
     */
    public void saveSharePreferences(){
        // Need to store the phone number and password if user clicks "remember" button.
        // Save these data to the SharedPreferences file.
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("phone", phoneNumber);
        editor.putBoolean("remember",true);
        editor.putString("password", password);
        editor.putString("pubkey",pubkey);
        editor.apply();
    }

    /**
     * This function is used to change the captcha if the user finds the code is hard to tell the differences.
     * @param view the current context.
     */
    public void changeCode(View view){
        showImageCode.setImageBitmap(ImageCodeGenerator.getInstance().createBitmap());
        testImageCode = ImageCodeGenerator.getInstance().getCode().toLowerCase();
    }
}
