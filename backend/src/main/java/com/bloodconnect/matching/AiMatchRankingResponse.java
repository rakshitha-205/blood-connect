package com.bloodconnect.matching;

import java.util.List;

public record AiMatchRankingResponse(String rankedBy, String model, String note, List<RankedDonorMatch> matches) {
    public static final String RANKED_BY_AI = "AI";
    public static final String RANKED_BY_DISTANCE = "DISTANCE";
}
