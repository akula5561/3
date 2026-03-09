package com.danil.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateBookRequest {
    @NotBlank
    private String title;
    private Integer publishedYear;
    private Boolean available = Boolean.TRUE;
    @NotNull
    private Long authorId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}
