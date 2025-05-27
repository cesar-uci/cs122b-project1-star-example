package uci122b.util;

public class SearchUtil {

    public static int calculateEditDistanceThreshold(String query) {
        if (query == null) {
            return 0;
        }
        int len = query.length();
        if (len == 0) return 0;
        if (len < 4) return 0; // For very short queries, fuzziness might not be desired or could be 1.
        if (len < 8) return 1;
        if (len < 12) return 2;
        return Math.min(3, len / 4);
    }
}