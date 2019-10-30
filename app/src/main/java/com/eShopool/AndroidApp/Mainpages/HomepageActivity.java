package com.eShopool.AndroidApp.Mainpages;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.eShopool.AndroidApp.BasicPages.BasicActivity;
import com.eShopool.AndroidApp.Library.Encoder;
import com.eShopool.AndroidApp.Library.GoToNextActivity;
import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.SimpleAdapter;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.Menu.HomeFragment;
import com.eShopool.AndroidApp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Handler;
import android.os.Message;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show the homepage.
 */
public class HomepageActivity extends BasicActivity {


    private AppBarConfiguration mAppBarConfiguration;
    private Bitmap bitmap;
    private ImageView mImage;
    private TextView txtPhone;
    private NavController navController;
    private ToastCenterText text = new ToastCenterText();
    private String ip;
    private GoToNextActivity thisActivity = new GoToNextActivity(this);

    /*
     * This handler could make a bitmap if the file exists.
     */
    private Handler mhandler = new Handler(){
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


    /**
     * This method implements the super method.
     * The main purpose of this method is to show the homepage.
     * @param savedInstanceState current state of the instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
            thisActivity.toNextActivityWithoutParameter(NewItemActivity.class)
        );
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_homepage, R.id.nav_necessity, R.id.nav_electronic,
                R.id.nav_makeup, R.id.nav_sales, R.id.nav_history, R.id.nav_setting)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.navigate(R.id.action_homeFragment_to_homeFragment);

        LoadTomcatBalance lb = new LoadTomcatBalance();
        ip = "";
        while(ip.equals("")){
            ip = lb.getIP();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        SearchView sv = (SearchView) menu.findItem(R.id.ab_search).getActionView();
        // Set Listener.
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Encoder encode = new Encoder();
                String encryptedQuery = encode.Base64Encode(query);
                Bundle bundle = new Bundle();
                bundle.putString("query",encryptedQuery);
                try {
                    navController.navigate(R.id.action_homeFragment_to_homeFragment,bundle);
                } catch (IllegalArgumentException e_necessity){
                    try {
                        navController.navigate(R.id.action_necessityFragment_to_homeFragment,bundle);
                    } catch (IllegalArgumentException e_electronic){
                        try{
                            navController.navigate(R.id.action_electronicFragment_to_homeFragment,bundle);
                        } catch (IllegalArgumentException e_makeup) {
                            navController.navigate(R.id.action_makeupFragment_to_homeFragment,bundle);
                        }

                    }

                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mImage = findViewById(R.id.image_user);
        SharedPreferences sp = this.getSharedPreferences("config", this.MODE_PRIVATE);
        String phoneNum = sp.getString("phone", "");
        txtPhone = findViewById(R.id.txt_username);
        txtPhone.setText("+86 " + phoneNum);
        new Thread(){
            @Override
            public void run() {
                Message message = mhandler.obtainMessage();
                Encoder encode = new Encoder();
                String encryptedPhone = encode.Base64Encode(phoneNum);
                message.obj = downloadImg("http://" + ip + ":8080/WebPicStream/GetPortrait?phone=" + encryptedPhone);
                message.sendToTarget();
            }
        }.start();
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}
