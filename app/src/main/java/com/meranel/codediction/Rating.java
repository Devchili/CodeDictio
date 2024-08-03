package com.meranel.codediction;

public class Rating {
    private String meaningId;
    private int rating;

    public Rating(String meaningId, int rating) {
        this.meaningId = meaningId;
        this.rating = rating;
    }

    public String getMeaningId() {
        return meaningId;
    }

    public int getRating() {
        return rating;
    }
}
