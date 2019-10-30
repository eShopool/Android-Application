package com.eShopool.AndroidApp.Menu;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.eShopool.AndroidApp.Library.AESUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show all purchase transcripts.
 */
public class HistoryFragment extends Fragment {

    private static final int STATUS_ERROR = -1;
    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final String item_regex = "<return>(.*)</return>";

    private LinearLayout rootLayout;
    private ToastCenterText text = new ToastCenterText();
    private TextView tv;
    private String phoneNum;
    private LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400);

    /*
     * This handler could handle different status and give the corresponding response.
     */
    private Handler transHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    String result = (String)msg.obj;
                    Pattern r = Pattern.compile(item_regex,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    Matcher m = r.matcher(result);
                    if (m.find()) result= m.group(1);
                    String myOriginalMessage = AESUtil.decryptHex(result, phoneNum);
                    String transcripts = myOriginalMessage.replace("    ","");
                    String[] transcript = transcripts.split(";");
                    if(transcript.length != 0)  generateLayout(transcript);
                    break;
                case STATUS_ERROR:
                    text.displayToast(getContext(),String.valueOf(msg.obj));
                    break;
                case NETWORK_ERROR:
                    text.displayToast(getContext(),getString(R.string.network_error));
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the purchase transcript page.
     * @param savedInstanceState current state of the instance.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_history, container, false);
        rootLayout = root.findViewById(R.id.container);
        SharedPreferences sp = getContext().getSharedPreferences("config", getContext().MODE_PRIVATE);
        phoneNum = sp.getString("phone", "");
        findTranscripts();
        return root;
    }

    /**
     * This method is used to find the user's purchase transcripts.
     */
    private void findTranscripts() {
        SharedPreferences sp = getActivity().getSharedPreferences("config", getActivity().MODE_PRIVATE);
        String publicKey = sp.getString("pubkey", "");
        Log.e("pubkey",publicKey);
        new Thread(){
            @Override
            public void run() {

                try {
                    HttpURLConnection conn = SoapRequest.request("searchBuyBill", publicKey);
                    if (conn == null){
                        Message msg = Message.obtain();
                        msg.what = NETWORK_ERROR;
                        transHandler.sendMessage(msg);
                    } else {
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            Message msg = Message.obtain();
                            InputStream is = conn.getInputStream();
                            String result = StreamUtils.readStream(is);
                            Log.e("result", result);
                            msg.what = STATUS_SUCCESS;
                            msg.obj = result;
                            transHandler.sendMessage(msg);
                        }
                        else {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            msg.obj = code;
                            transHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    transHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This method is used to generate dynamic layouts.
     * @param trans the transcript.
     */
    private void generateLayout(String[] trans){
        for(int record = 0; record < trans.length; record++){
            tv = new TextView(getContext());
            tv.setPadding(60,50,60,50);
            tv.setText(trans[record]);
            tv.setBackgroundColor(Color.parseColor("#FFFFFF"));
            tv.setHeight(360);
            tv.setTextColor(Color.parseColor("#000000"));
            lp.setMargins(0, 20, 0, 0);
            rootLayout.addView(tv,lp);
        }
    }
}