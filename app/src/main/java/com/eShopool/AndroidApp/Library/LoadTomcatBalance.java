package com.eShopool.AndroidApp.Library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Group 10
 * @version 1.0.1
 * @since 27/9/2019
 * This class is used to load tomcat balance.
 */
public class LoadTomcatBalance {
    private int[] weight;
    private int[] effectiveWeight;
    private int[] currentWeight;
    static final String[] TomcatServers = {"192.168.0.243","192.168.0.230"};
    static final String ping_regex = "(time=)(.*)(ms)";

    public LoadTomcatBalance()
    {

        int numOfSlave = TomcatServers.length;

        //measure latency between databases
        Double[] latency = new Double[numOfSlave];
        weight = new int[numOfSlave];
        effectiveWeight = new int[numOfSlave];
        currentWeight = new int[numOfSlave];
        double sum = 0.0;

        for(int i = 0; i< numOfSlave; i++) {
            latency[i] = pingIP(TomcatServers[i]);
            sum += latency[i];
            currentWeight[i] = 0;
        }


        //give weights to write databases
        for(int i = 0; i < latency.length; i++)
        {
            if(latency[i] == 100)
            {
                weight[i] = 0;
                continue;
            }
            if(sum > latency[i]) weight[i] = (int)Math.round(sum - latency[i]);
            else weight[i] = (int)Math.round(latency[i]);
        }

        effectiveWeight = weight.clone();

    }
    /**Get the IP of one database for reading data<P/>
     * @return IP of one database for reading
     * */
    public String getIP()
    {
        int max= 0;
        int maxIndex = 0;
        int totalWeight = 0;
        for(int i = 0; i < TomcatServers.length; i++)
        {
            currentWeight[i] += effectiveWeight[i];
            totalWeight += effectiveWeight[i];
            if(currentWeight[i] > max)
            {
                max = currentWeight[i];
                maxIndex = i;
            }
        }

        currentWeight[maxIndex] -=  totalWeight;

        return TomcatServers[maxIndex];
    }

    public Double pingIP(String ip){
        Runtime runtime = Runtime.getRuntime();
        Double result = 0.0;
        try {
            String line = null;
            Process process = runtime.exec("ping " + ip);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("timeout")) {
                    result = 100.0;
                    System.out.println("result: " + result.toString());
                    break;
                } else if(line.contains("ttl")) {
                    Pattern r = Pattern.compile(ping_regex);
                    Matcher m = r.matcher(line);
                    if (m.find()) result = Double.parseDouble(m.group(2));
                    System.out.println("result: " + result.toString());
                    break;
                }
            }
        } catch (IOException e) {
            result = 100.0;
        }
        return result;
    }

}
