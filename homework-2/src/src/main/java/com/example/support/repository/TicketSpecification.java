package com.example.support.repository;

import com.example.support.model.*;
import org.springframework.data.jpa.domain.Specification;

public class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<TicketEntity> withCategory(Category category) {
        return (root, query, cb) ->
                category == null ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<TicketEntity> withPriority(Priority priority) {
        return (root, query, cb) ->
                priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<TicketEntity> withStatus(Status status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<TicketEntity> withFilter(Category category, Priority priority, Status status) {
        return Specification.where(withCategory(category))
                .and(withPriority(priority))
                .and(withStatus(status));
    }
}
