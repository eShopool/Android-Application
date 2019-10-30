package com.eShopool.AndroidApp.SettingsPages;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.DecodeTool;
import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.LoginPages.LoginActivity;
import com.eShopool.AndroidApp.Mainpages.HomepageActivity;
import com.eShopool.AndroidApp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show the user's balance.
 */
public class WalletActivity extends BasicActivity {

    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final int STATUS_ERROR = -1;
    private TextView txtWallet;
    private String pubkey;
    private String balance;
    private static final String item_regex = "<return>(.*)</return>";
    private GoToNextActivity thisActivity = new GoToNextActivity(this);
    private ToastCenterText text = new ToastCenterText();

    /*
     * This handler could display the current balance.
     */
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(item_regex);
                    String result = (String)msg.obj;
                    Matcher m = r.matcher(result);
                    if (m.find()) result = DecodeTool.Base64Decoder(m.group(1));
                    else result = "100";
                    txtWallet.setText(result);
                    break;
                case STATUS_ERROR:
                    text.displayToast(WalletActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could display the balance update results.
     */
    private Handler balanceHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(item_regex);
                    String result = (String)msg.obj;
                    Matcher m = r.matcher(result);
                    if (m.find()) result = m.group(1);
                    if(result.equals("success")) text.displayToast(WalletActivity.this,getString(R.string.update_successfully));
                    else text.displayToast(WalletActivity.this,getString(R.string.network_error));
                    txtWallet.setText(balance);
                    break;
                case STATUS_ERROR:
                    text.displayToast(WalletActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the personal information page.
     * @param savedInstanceState current state of the instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        txtWallet = findViewById(R.id.et_balance);
        SharedPreferences sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
        pubkey = sp.getString("pubkey","");

        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("getBalance", pubkey);
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
     * This method is used to send a top-up balance request to SOAP web service.
     * @param view the current view.
     */
    public void ok(View view) {
        balance = txtWallet.getText().toString().trim();
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("updateUserBalance", pubkey, balance);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        balanceHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();

                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            balanceHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            balanceHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    balanceHandler.sendMessage(msg);
                }
            }
        }.start();
    }
}
