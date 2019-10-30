package com.eShopool.AndroidApp.Library;

import java.util.Base64;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is a tool which is used to decode Bese64 encryption string.
 */
public class DecodeTool {
    public DecodeTool(){}

    // As a static method to be directed access.
    public static String Base64Decoder(String str) {
        byte[] data = Base64.getDecoder().decode(str);
        String decode = null;
        decode = new String(data);
        return decode;
    }

}
