package com.eShopool.AndroidApp.Mainpages;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.LoginPages.SetPasswordActivity;
import com.eShopool.AndroidApp.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show the item information.
 */
public class ItemActivity extends BasicActivity {

    private static final int STATUS_ERROR = -1;
    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;

    private EditText userItemNum;
    private TextView title;
    private TextView itemRest;
    private TextView itemDesc;
    private TextView itemPrice;
    private ImageView itemImg;
    private SharedPreferences sp;
    private String pubkey;
    private String item_info;
    private String imgID;
    private String imgPath;
    private String storageNum;
    private static final String img_regex = "(/cache/)(.*)(.jpg)";
    private static final String item_regex = "<return>(.*)</return>";
    private ToastCenterText text = new ToastCenterText();
    private GoToNextActivity thisActivity = new GoToNextActivity(this);

    /*
     * This handler could handle different status and give the corresponding response.
     */
    private Handler balanceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    String result = (String)msg.obj;
                    Pattern r = Pattern.compile(item_regex);
                    Matcher m = r.matcher(result);
                    if (m.find()) result = m.group(1);
                    if(result.equals("success")) {
                        text.displayToast(ItemActivity.this, "Purchase successfully!");
                        thisActivity.toNextActivityWithoutParameter(HomepageActivity.class);
                    }
                    else text.displayToast(ItemActivity.this,result);
                    break;
                case STATUS_ERROR:
                    text.displayToast(ItemActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(ItemActivity.this,getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * This handler could handle different status and give the corresponding response.
     */
    private Handler infoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Pattern r = Pattern.compile(item_regex);
                    // 现在创建 matcher 对象
                    Matcher m = r.matcher((String)msg.obj);
                    if (m.find()) item_info = m.group(1);
                    String[] arrOfItem = item_info.split(",");
                    title.setText(arrOfItem[0]);
                    itemPrice.setText(arrOfItem[1]);
                    itemRest.setText(arrOfItem[2]);
                    storageNum = arrOfItem[2];
                    String desc = arrOfItem[5];
                    if(desc.equals(" ")) itemDesc.setHeight(0);
                    else itemDesc.setText(arrOfItem[5]);
                    break;
                case STATUS_ERROR:
                    text.displayToast(ItemActivity.this,(String)msg.obj);
                    break;
                case NETWORK_ERROR:
                    text.displayToast(ItemActivity.this,getString(R.string.network_error));
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
        setContentView(R.layout.item_info);
        userItemNum = findViewById(R.id.edt_num);
        title = findViewById(R.id.txt_title);
        itemRest = findViewById(R.id.txt_item_rest);
        itemDesc = findViewById(R.id.txt_desc);
        itemImg = findViewById(R.id.img_item);
        itemPrice = findViewById(R.id.txt_item_price);
        // Get the phone number from previous activity.
        Intent intent = this.getIntent();
        imgPath = intent.getStringExtra("item_id");
        Pattern r = Pattern.compile(img_regex);
        Matcher m = r.matcher(imgPath);
        if (m.find()) imgID = m.group(2);
        sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
        pubkey = sp.getString("pubkey","");
        setImageToView();
        showItemInfo();
    }

    /**
     * This method is used to show the item's information.
     */
    private void showItemInfo() {
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = SoapRequest.request("getCommodityInfor", imgID);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        infoHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Message msg = Message.obtain();
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            infoHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            infoHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    infoHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This function is used to set image view.
     */
    protected void setImageToView() {
        Bitmap mBitmap = BitmapFactory.decodeFile(imgPath);
        itemImg.setImageBitmap(mBitmap);
    }

    /**
     * This method is used to help the user buy some items.
     * @param view current view.
     */
    public void buyItem(View view) {
        String userBuyItems = userItemNum.getText().toString().trim();
        if(Integer.parseInt(userBuyItems)>Integer.parseInt(storageNum)){
            text.displayToast(this,"The amount of purchase is invalid.");
        } else {
            new Thread(){
                @Override
                public void run() {
                    try {
                        HttpURLConnection conn = SoapRequest.request("buyItem", pubkey, userBuyItems, imgID);
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

    // Set cancel button.
    public void cancel(View view) {
        super.onBackPressed();
    }
}
