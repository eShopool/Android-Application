package com.eShopool.AndroidApp.Menu;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.RSAUtil;
import com.eShopool.AndroidApp.Library.SoapRequest;
import com.eShopool.AndroidApp.Library.StreamUtils;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.LoginPages.LoginActivity;
import com.eShopool.AndroidApp.LoginPages.SetPasswordActivity;
import com.eShopool.AndroidApp.Mainpages.HomepageActivity;
import com.eShopool.AndroidApp.R;
import com.eShopool.AndroidApp.SettingsPages.ChangePhotoActivity;
import com.eShopool.AndroidApp.SettingsPages.SetAddressActivity;
import com.eShopool.AndroidApp.SettingsPages.WalletActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show the settings page.
 */
public class SettingsFragment extends Fragment {

    private static final int STATUS_SUCCESS = 1;
    private static final int NETWORK_ERROR = -2;
    private static final int STATUS_ERROR = -1;

    private Button btnChangePhoto;
    private Button btnChangePwd;
    private Button btnWallet;
    private Button btnLogout;
    private Button btnDeleteAccount;
    private Button btnChangeAddr;
    private String ip;
    private ToastCenterText text = new ToastCenterText();

    /*
     * This handler could handle the delete event.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    Log.e("ok","ok");
                    text.displayToast(getContext(),"Your account has been deleted successfully!");
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    intent.setClass(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    break;
                case STATUS_ERROR:
                    text.displayToast(getContext(),"Network Error!\n"+"Try again!");
                    break;
                default:
                    break;
            }
        }
    };
    /*
     * This handler could handle the delete event.
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_SUCCESS:
                    text.displayToast(getContext(),"Your account has been deleted successfully!");
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    intent.setClass(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    break;
                case STATUS_ERROR:
                    text.displayToast(getContext(),"Network Error!\n"+"Try again!");
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the settings page.
     * @param savedInstanceState current state of the instance.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setting, container, false);
        btnChangePhoto = root.findViewById(R.id.btn_change_img);
        btnWallet = root.findViewById(R.id.btn_change_wallet);
        btnChangePwd = root.findViewById(R.id.btn_set_pwd);
        btnLogout = root.findViewById(R.id.btn_logout);
        btnDeleteAccount = root.findViewById(R.id.btn_delete_id);
        btnChangeAddr = root.findViewById(R.id.btn_set_addr);

        // Go to change photo page.
        btnChangePhoto.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClass(getActivity(), ChangePhotoActivity.class);
            startActivity(intent);
        });

        // Go to wallet page.
        btnWallet.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClass(getActivity(), WalletActivity.class);
            startActivity(intent);
        });

        // Go to change password page.
        btnChangePwd.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClass(getActivity(), SetPasswordActivity.class);
            startActivity(intent);
        });

        // Go to change address page.
        btnChangeAddr.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClass(getActivity(), SetAddressActivity.class);
            startActivity(intent);
        });

        // Go to login page.
        btnLogout.setOnClickListener(view -> {
            text.displayToast(getContext(),getString(R.string.logout_successfully));
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.setClass(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        // Show warning dialogue.
        btnDeleteAccount.setOnClickListener(view -> {
            SharedPreferences sp;
            // Find the SharedPreferences.
            sp = getActivity().getSharedPreferences("config", getContext().MODE_PRIVATE);
            String phoneNumber = sp.getString("phone", "");
            Encoder encode = new Encoder();
            String encryptedPhone = encode.Base64Encode(phoneNumber);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setIcon(android.R.drawable.ic_dialog_info);
            builder.setMessage(getString(R.string.delete_account_warning));
            builder.setCancelable(true);
            builder.setPositiveButton(getString(R.string.Continue), (dialog, which) -> {
                new Thread(){
                    @Override
                    public void run() {
                        // Access the database.
                        try {
                            HttpURLConnection conn = SoapRequest.request("deleteAccount", encryptedPhone);
                            if (conn == null){
                                Message msg = Message.obtain();
                                msg.what = NETWORK_ERROR;
                                mHandler.sendMessage(msg);
                            } else {
                                int code = conn.getResponseCode();
                                Message msg = Message.obtain();
                                if (code == 200) {
                                    InputStream is = conn.getInputStream();
                                    String result = StreamUtils.readStream(is);
                                    if(result.equals("success")) msg.what = STATUS_SUCCESS;
                                    else msg.what = STATUS_ERROR;
                                    mHandler.sendMessage(msg);
                                } else {
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

                        // Access the gallery to delete the folder
                        try {

                            LoadTomcatBalance lb = new LoadTomcatBalance();
                            ip = "";
                            while(ip.equals("")){
                                ip = lb.getIP();
                            }
                            String urlPath = "http://" + ip + ":8080/WebPicStream/DeleteAccount?phone=" + encryptedPhone;
                            URL url = new URL(urlPath);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(5000);
                            int code = conn.getResponseCode();
                            Message msg = Message.obtain();
                            if (code == 200) {
                                msg.what = STATUS_SUCCESS;
                                handler.sendMessage(msg);
                            }
                            else {
                                msg.what = STATUS_ERROR;
                                handler.sendMessage(msg);
                            }
                        } catch (Exception e) {
                            Message msg = Message.obtain();
                            msg.what = STATUS_ERROR;
                            handler.sendMessage(msg);
                        }
                    }
                }.start();
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                // do nothing
            });
            builder.create().show();
        });
        return root;

    }

}