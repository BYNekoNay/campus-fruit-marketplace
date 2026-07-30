package com.campusfruit.discovery.ranking;

import java.util.Map;
import java.util.UUID;

public class RankingResult {

    private final double score;
    private final Map<String, Double> subScores;
    private final String rankingTraceId;

    public RankingResult(double score, Map<String, Double> subScores) {
        this.score = score;
        this.subScores = subScores;
        this.rankingTraceId = UUID.randomUUID().toString();
    }

    public RankingResult(double score, Map<String, Double> subScores, String rankingTraceId) {
        this.score = score;
        this.subScores = subScores;
        this.rankingTraceId = rankingTraceId;
    }

    public double getScore() { return score; }
    public Map<String, Double> getSubScores() { return subScores; }
    public String getRankingTraceId() { return rankingTraceId; }

    @Override
    public String toString() {
        return "RankingResult{score=" + score + ", traceId=" + rankingTraceId + "}";
    }
}
