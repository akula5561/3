package com.danil.library.dto;

public class BookDto {
    private Long id;
    private String title;
    private Integer publishedYear;
    private boolean available;
    private Long authorId;

    public BookDto() {}
    public BookDto(Long id, String title, Integer publishedYear, boolean available, Long authorId) {
        this.id = id; this.title = title; this.publishedYear = publishedYear; this.available = available; this.authorId = authorId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}
