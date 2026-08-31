package com.example.support.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("new") new_,
    in_progress,
    waiting_customer,
    resolved,
    closed
}
