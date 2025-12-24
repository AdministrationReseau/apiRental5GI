package com.project.apirental.shared.events;

public record AuditEvent(String action, String module, String details) {}
