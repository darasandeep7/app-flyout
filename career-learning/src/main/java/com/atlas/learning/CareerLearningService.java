package com.atlas.learning;

import org.springframework.stereotype.Service;

@Service
public class CareerLearningService {
    public CareerLearningStats emptyStats() {
        return new CareerLearningStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
