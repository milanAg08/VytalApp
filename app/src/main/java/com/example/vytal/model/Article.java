package com.example.vytal.model;

/*
 Simple Article model in Java (title, subtitle, url)
 */
public class Article {
    private String title;
    private String subtitle;
    private String url;

    // Constructor
    public Article(String title, String subtitle, String url) {
        this.title = title;
        this.subtitle = subtitle;
        this.url = url;
    }

    // Getter for title
    public String getTitle() {
        return title;
    }

    // Getter for subtitle
    public String getSubtitle() {
        return subtitle;
    }

    // Getter for url
    public String getUrl() {
        return url;
    }
}
