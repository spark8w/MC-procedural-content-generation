package ukc.gdmc;

import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionTest {

    // main() is the method Java runs first when you press Play
    public static void main(String[] args) {

        System.out.println("Testing connection to Minecraft...");

        try {
            // Create a connection to the GDMC HTTP server
            // localhost means "this same computer"
            // 9000 is the port the GDMC mod listens on
            URL url = new URL("http://localhost:9000/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Wait maximum 3 seconds for a response
            conn.setConnectTimeout(3000);
            conn.setRequestMethod("GET");

            // Get the response code — 200 means OK
            int code = conn.getResponseCode();

            // Read what Minecraft sent back
            String response = new String(conn.getInputStream().readAllBytes());

            if (code == 200) {
                System.out.println("SUCCESS - Connected to Minecraft!");
                System.out.println("Minecraft says: " + response);
            } else {
                System.out.println("Connected but got unexpected code: " + code);
            }

            conn.disconnect();

        } catch (java.net.ConnectException e) {
            // This happens if Minecraft is not running
            System.out.println("FAILED - Minecraft is not running.");
            System.out.println("Start Minecraft with the GDMC mod first!");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}