package com.example.minidrive.repository;

import com.example.minidrive.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StorageRepository extends JpaRepository<Storage, Long> {
    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Storage s SET s.isDefault = false WHERE s.isDefault = true")
    void clearDefault();

    @Query("SELECT COUNT(n) > 0 FROM Node n WHERE n.storage = :storageId")
    boolean isStorageInUse(@Param("storageId") Long storageId);

    @Query("SELECT s FROM Storage s WHERE s.isDefault = true LIMIT 1")
    Optional<Storage> getDefaultStorage();
}
