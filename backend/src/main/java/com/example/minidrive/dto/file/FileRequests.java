package com.example.minidrive.dto.file;

import com.example.minidrive.enums.NodeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FileRequests {
    public record BeginUpload(
            @NotBlank(message = "Имя файла не может быть пустым")
            String fileName,

            @NotNull(message = "Укажите размер файла")
            @Min(value = 1, message = "Размер файла должен быть больше 0")
            Long fileSize,

            String mimeType,
            UUID parentUuid,
            Long requestedChunkSize
    ) {}

    public record CreateFolder(
            @NotBlank String name,
            UUID parentUuid
    ) {}

    public record BatchNodeRequest(
            List<UUID> fileUuids
    ) {}

    public record UpdateFile(
            String newFileName
    ) {}
}
