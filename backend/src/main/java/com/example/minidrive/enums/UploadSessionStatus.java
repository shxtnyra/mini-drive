package com.example.minidrive.enums;

public enum UploadSessionStatus {
    INITIALIZING,
    UPLOADING,
    FINALIZING,     // Статус обозначает начало сборки файла из частей
    COMPLETED,
    ABORTED
}
