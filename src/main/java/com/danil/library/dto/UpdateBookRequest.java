package com.danil.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateBookRequest {
    @NotBlank
    private String title;
    private Integer publishedYear;
    @NotNull
    private Long authorId;
    @NotNull
    private Boolean available;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
