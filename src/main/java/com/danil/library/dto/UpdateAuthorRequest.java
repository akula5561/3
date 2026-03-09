package com.danil.library.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAuthorRequest {
    @NotBlank
    private String name;
    private String bio;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
