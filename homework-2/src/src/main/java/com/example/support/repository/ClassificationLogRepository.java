package com.example.support.repository;

import com.example.support.model.ClassificationLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClassificationLogRepository extends JpaRepository<ClassificationLogEntry, UUID> {
}
