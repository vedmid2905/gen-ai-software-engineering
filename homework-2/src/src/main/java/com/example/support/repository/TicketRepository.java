package com.example.support.repository;

import com.example.support.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, UUID>,
        JpaSpecificationExecutor<TicketEntity> {

    List<TicketEntity> findByCategory(Category category);

    List<TicketEntity> findByPriority(Priority priority);

    List<TicketEntity> findByStatus(Status status);
}
