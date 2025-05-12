package uci122b;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

public class RecaptchaVerifyUtils {
    private static final String VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify";

    /**
     * Throws Exception if verification fails or token is missing.
     */
    public static void verify(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new Exception("Missing reCAPTCHA token");
        }

        URL url = new URL(VERIFY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent","Mozilla/5.0");
        conn.setRequestProperty("Accept-Language","en-US,en;q=0.5");
        conn.setDoOutput(true);

        String params = "secret=" + RecaptchaConstants.SECRET_KEY
                + "&response=" + token;
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes());
        }

        try (InputStreamReader reader =
                     new InputStreamReader(conn.getInputStream())) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            boolean success = json.get("success").getAsBoolean();
            if (!success) {
                throw new Exception("reCAPTCHA failed: " + json);
            }
        }
    }
}
