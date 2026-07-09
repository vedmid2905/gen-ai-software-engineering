package com.example.support.classifier;

import com.example.support.dto.ClassificationResult;

public interface Classifier {
    ClassificationResult classify(String subject, String description);
}
