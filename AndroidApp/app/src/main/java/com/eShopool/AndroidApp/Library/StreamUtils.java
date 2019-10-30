package com.eShopool.AndroidApp.Library;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is allow to display Chinese.
 */
public class StreamUtils {
    public static String readStream(InputStream is){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while(( len = is.read(buffer))!=-1){
                baos.write(buffer, 0, len);
            }
            is.close();
            String result = baos.toString();

            if(result.contains("gb2312")){
                return baos.toString("gb2312");
            }else{
                return result;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
