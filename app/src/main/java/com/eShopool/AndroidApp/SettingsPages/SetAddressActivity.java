package com.eShopool.AndroidApp.SettingsPages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.DecodeTool;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.LoginPages.SetPasswordActivity;
import com.eShopool.AndroidApp.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show the user's address.
 */
public class SetAddressActivity extends BasicActivity {
    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final int STATUS_ERROR = -1;
    private EditText txtAddr;
    private String addr;
    private String pubkey;
    private static final String item_regex = "<return>(.*)</return>";
    private ToastCenterText text = new ToastCenterText();

    /*
     * This handler could display the current address.
     */
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(item_regex);
                    String result = (String)msg.obj;
                    Matcher m = r.matcher(result);
                    if (m.find()) result = DecodeTool.Base64Decoder((m.group(1)));
                    else result = "";
                    txtAddr.setText(result);
                    break;
                case STATUS_ERROR:
                    text.displayToast(SetAddressActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could display the address update results.
     */
    private Handler addrHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(item_regex);
                    String result = (String)msg.obj;
                    Matcher m = r.matcher(result);
                    if (m.find()) result = m.group(1);
                    if(result.equals("success")) text.displayToast(SetAddressActivity.this,getString(R.string.update_successfully));
                    else text.displayToast(SetAddressActivity.this,getString(R.string.network_error));
                    break;
                case STATUS_ERROR:
                    text.displayToast(SetAddressActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the address page.
     * @param savedInstanceState current state of the instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);
        txtAddr = findViewById(R.id.et_addr);
        SharedPreferences sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
        pubkey = sp.getString("pubkey","");

        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("getAddress", pubkey);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        handler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            handler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            handler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    // Set cancel button.
    public void cancel(View view) {
        super.onBackPressed();
    }

    /**
     * This method is used to send an update address request to SOAP web service.
     * @param view the current view.
     */
    public void saveAddress(View view) {
        addr = txtAddr.getText().toString().trim();
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("setAddress", pubkey, RSAUtil.getCryptoText(pubkey, addr));
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        addrHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            addrHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            addrHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    addrHandler.sendMessage(msg);
                }
            }
        }.start();
    }
}
