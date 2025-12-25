package com.project.apirental.modules.auth.dto;

public record RegisterRequest(
    String firstname,
    String lastname,
    String email,
    String password) {}
