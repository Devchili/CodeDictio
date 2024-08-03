package com.meranel.codediction;

public class AuthorMeaning {
    private String id;
    private String author;
    private String meaning;
    private int rating;
    private long ratingCount;

    public AuthorMeaning(String id, String author, String meaning, int rating, long ratingCount) {
        this.id = id;
        this.author = author;
        this.meaning = meaning;
        this.rating = rating;
        this.ratingCount = ratingCount;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getMeaning() {
        return meaning;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }
}
