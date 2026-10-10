package com.example.minidrive.repository;

import com.example.minidrive.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Доступ к объектам истории версий нужен плановой очистке корзины:
 * FK file_versions.node_id запрещает физически удалить Node, пока версии есть.
 */
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findAllByNode_Id(Long nodeId);
}
