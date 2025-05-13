package uci122b;

public class Movie {
    private final String id;
    private final String title;
    private final int year;
    private final String director;
    private final float rating;

    public Movie(String id, String title, int year, String director, float rating) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public int getYear() {
        return year;
    }
    public String getDirector() {
        return director;
    }
    public float getRating() {
        return rating;
    }
}
