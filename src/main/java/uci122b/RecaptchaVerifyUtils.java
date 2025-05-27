package uci122b;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.net.URL;

public class RecaptchaVerifyUtils {
    private static final String VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify";

    public static class RecaptchaResponse {
        boolean success;
        double score;
        String action;
    }

    /**
     * Returns the parsed JSON response.
     */
    public static RecaptchaResponse verify(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new Exception("Missing reCAPTCHA token");
        }

        String params = "secret=" + URLEncoder.encode(RecaptchaConstants.SECRET_KEY, "UTF-8")
                + "&response=" + URLEncoder.encode(token, "UTF-8");

        URL url = new URL(VERIFY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes());
        }

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            RecaptchaResponse resp = new RecaptchaResponse();
            resp.success = json.get("success").getAsBoolean();
            resp.score   = json.has("score")   ? json.get("score").getAsDouble()    : 0.0;
            resp.action  = json.has("action")  ? json.get("action").getAsString()    : "";
            return resp;
        }
    }
}
