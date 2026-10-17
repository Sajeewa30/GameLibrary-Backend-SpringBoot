package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.service.DriveBackupService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {

    private final DriveBackupService driveBackupService;

    public BackupController(DriveBackupService driveBackupService) {
        this.driveBackupService = driveBackupService;
    }

    @PostMapping
    @Operation(summary = "Back up to Google Drive", description = "Starts mirroring the database and uploads to Google Drive. Returns immediately; poll the status endpoint.")
    public ResponseEntity<Map<String, Object>> start() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(driveBackupService.start());
    }

    @GetMapping("/status")
    @Operation(summary = "Google Drive backup status")
    public Map<String, Object> status() {
        return driveBackupService.getStatus();
    }
}
