package ir.shecan.util.server;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class OkHttpLogger {
    public static String resolvedIp = "";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void requestWithIPLogging(String urlString) {
        executor.execute(()-> {
            try {
                // Extract hostname from URL
                URL url = new URL(urlString);
                String host = url.getHost();

                // Resolve the IP address
                InetAddress address = InetAddress.getByName(host);
                String ipAddress = address.getHostAddress();
                resolvedIp = ipAddress;
                System.out.println("Connecting to: " + urlString);
                System.out.println("Resolved IP: " + ipAddress);

                // Open connection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Get the response
                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                reader.close();

            } catch (UnknownHostException e) {
                System.err.println("Failed to resolve host.");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}