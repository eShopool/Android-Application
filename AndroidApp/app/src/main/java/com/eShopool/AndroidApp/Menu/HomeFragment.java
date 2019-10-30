package com.eShopool.AndroidApp.Menu;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eShopool.AndroidApp.Library.LoadTomcatBalance;
import com.eShopool.AndroidApp.Library.SimpleAdapter;
import com.eShopool.AndroidApp.Library.ToastCenterText;
import com.eShopool.AndroidApp.Mainpages.ItemActivity;
import com.eShopool.AndroidApp.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.eShopool.AndroidApp.Library.FileOperation.writeFile;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to show all available items.
 */
public class HomeFragment extends Fragment {

    private static final int LOAD_ERROR = 2;
    private static final int LOAD_SUCCESS = 1;

    private URL imgAccessAddr;
    private File tempFile;
    private String line;
    private String ip;
    private RecyclerView mListView;
    private SimpleAdapter mAdapter;
    private List<String> photoSelect = new ArrayList<>();
    private List<String> fileNameList = new ArrayList<>();
    private ToastCenterText text = new ToastCenterText();


    /*
     * This handler could add a new photo address into the list and display the photo as well.
     */
    public Handler handler = new Handler(msg -> {
        switch (msg.what){
            case LOAD_SUCCESS:
                photoSelect.add(msg.obj.toString());
                showPicture();
                break;
            case LOAD_ERROR:
                text.displayToast(getContext(), msg.obj.toString());
                photoSelect = new ArrayList<>();
                break;
        }
        return false;
    });

    /**
     * This method implements the super method.
     * The main purpose of this method is to show the default page.
     * @param savedInstanceState current state of the instance.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_homepage, container, false);
        mListView = root.findViewById(R.id.rlv_list);
        mAdapter = new SimpleAdapter(getContext(), photoSelect);
        mAdapter.setOnItemClickListener(new SimpleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, final int position) {
                String target = mAdapter.getPath(position);
                Intent intent = new Intent(getActivity(), ItemActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                intent.putExtra("item_id", target);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, final int position) {

            }
        });

        LoadTomcatBalance lb = new LoadTomcatBalance();
        ip = "";
        while(ip.equals("")){
            ip = lb.getIP();
        }

        Bundle bundle = getArguments();
        try{
            String data = bundle.getString("query");
            loadAllImagePath(data);
        } catch(NullPointerException e){
            loadAllImagePath();
        }

        return root;
    }

    /**
     * This method is used to query all items that satisfied the requirements.
     * @param query the keywords.
     */
    public void loadAllImagePath(String query){
        new Thread(){
            @Override
            public void run() {

                try {
                    imgAccessAddr = new URL("http://" + ip + ":8080/WebPicStream/SearchItems?item=" + query);
                    HttpURLConnection conn = (HttpURLConnection) imgAccessAddr.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    int code = conn.getResponseCode();
                    Message msg = Message.obtain();

                    if(code == 200){
                        writeFile(conn, getContext(),"Search.txt");
                        beginLoadImage("Search.txt");
                    }
                    else if (code == 404){
                        msg.what = LOAD_ERROR;
                        msg.obj = getString(R.string.HTTP_404);
                        handler.sendMessage(msg);
                    }
                    else{
                        msg.what = LOAD_ERROR;
                        msg.obj = getString(R.string.network_error);
                        handler.sendMessage(msg);
                    }
                } catch (MalformedURLException e) {
                    Message msg = Message.obtain();
                    msg.what = LOAD_ERROR;
                    msg.obj = getString(R.string.url_error);
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = LOAD_ERROR;
                    msg.obj = getString(R.string.io_error);
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This function is used to load all images by their address.
     */
    public void loadAllImagePath() {
        new Thread(){
            @Override
            public void run() {

                try {
                    imgAccessAddr = new URL("http://" + ip + ":8080/WebPicStream/GetHomepageTXT?");
                    HttpURLConnection conn = (HttpURLConnection) imgAccessAddr.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    int code = conn.getResponseCode();
                    Message msg = Message.obtain();
                    if(code == 200){
                        writeFile(conn, getContext(),"HomepageList.txt");
                        beginLoadImage("HomepageList.txt");
                    }
                    else if (code == 404){
                        msg.what = LOAD_ERROR;
                        msg.obj = getString(R.string.HTTP_404);
                        handler.sendMessage(msg);
                    }
                    else{
                        msg.what = LOAD_ERROR;
                        msg.obj = getString(R.string.network_error);
                        handler.sendMessage(msg);
                    }
                } catch (MalformedURLException e) {
                    Message msg = Message.obtain();
                    msg.what = LOAD_ERROR;
                    msg.obj = getString(R.string.url_error);
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = LOAD_ERROR;
                    msg.obj = getString(R.string.io_error);
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This function is used to open the file and read the address one line by one line.
     */
    public void beginLoadImage(String filename) {
        try {
            tempFile = new File(getContext().getCacheDir(), filename);
            FileInputStream fis = new FileInputStream(tempFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            while((line = br.readLine())!= null){
                if(line.length() == 0) return;
                fileNameList.add(line);
                loadImageByPath(line);
            }
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function is used to load a image from the responding url address.
     * @param line the responding url address.
     */
    public void loadImageByPath(String line) {
        new Thread(){
            @Override
            public void run(){
                File file = new File(getContext().getCacheDir(),line);
                try{
                    imgAccessAddr = new URL("http://" + ip + ":8080/WebPicStream/GetPhoto?path=" + line);
                    HttpURLConnection conn = (HttpURLConnection) imgAccessAddr.openConnection();
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    Message msg = Message.obtain();
                    if (code == 200){
                        InputStream is = conn.getInputStream();

                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        fos.close();
                        is.close();

                        msg.what = LOAD_SUCCESS;
                        msg.obj = file.getAbsolutePath();
                        handler.sendMessage(msg);
                    }
                    else{
                        msg.what = LOAD_ERROR;
                        msg.obj = getString(R.string.cannot_open_pic);
                        handler.sendMessage(msg);
                    }
                } catch (Exception e){
                    Message msg = Message.obtain();
                    msg.what = LOAD_ERROR;
                    msg.obj = getString(R.string.network_error);
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * This function could display all selected photos on the page.
     */
    public void showPicture() {
        if (photoSelect!= null) {
            mListView.setAdapter(mAdapter);
            mListView.setLayoutManager(new GridLayoutManager(getContext(),3));
        }
    }

}