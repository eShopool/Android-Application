package com.eShopool.AndroidApp.Library;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to send SOAP requests.
 */
public class SoapRequest {

    /**
     * This method is used to set HTTP connection to SOAP web service.
     * @param opt operation name.
     * @param parameters parameters.
     * @return HTTP connection.
     */
    public static HttpURLConnection request(String opt, String ...parameters){
        String parameter_label = "";
        for(int i = 0;i<parameters.length;i++)
        {
            parameter_label +=  "<arg" + i + ">" + parameters[i] + "</arg" + i + ">";
        }

        try {
            // Web Service address.
            LoadSOAPBalance soap = new LoadSOAPBalance();
            String ip = "";
            while (ip.equals("")){
                ip = soap.getIP();
            }
            String bestURL = "http://" + ip + ":8080/SoapForeShopool-war/SoapService";
            URL url = new URL(bestURL);
            Log.e("balance", ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            // Set the request header. (note that it must be in xml format)
            conn.setRequestProperty("content-type", "text/xml;charset=utf-8");
            // Construct the request body, in accordance with the SOAP specification.
            String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<SOAP-ENV:Header/>"
                    + "<S:Body xmlns:ns2=\"http://backend.eShopool.com/\">"
                    + "<ns2:" + opt + ">"
                    + parameter_label
                    + "</ns2:" + opt + ">"
                    + "</S:Body>"
                    + "</S:Envelope>";
            System.out.println(requestBody);
            // Get a output stream.
            OutputStream out = conn.getOutputStream();
            out.write(requestBody.getBytes());
            out.close();
            return conn;
        } catch (Exception e) {
            return null;
        }
    }

}
