package com.example.minidrive.repository;

import com.example.minidrive.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findAllByNode_IdIn(Collection<Long> nodeIds);

    void deleteAllByNode_IdIn(Collection<Long> nodeIds);
}
