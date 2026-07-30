package com.campusfruit.discovery.ranking;

import java.util.Map;

/**
 * 排序解释 VO：不含精确坐标，提供人性化排序理由。
 */
public class RankingExplanation {

    private Double overallScore;
    private Map<String, Double> subScores;
    private String algorithmVersion;
    private boolean coldStart;
    private String rankingReason;

    public RankingExplanation() {
    }

    public RankingExplanation(Double overallScore, Map<String, Double> subScores,
                               String algorithmVersion, boolean coldStart, String rankingReason) {
        this.overallScore = overallScore;
        this.subScores = subScores;
        this.algorithmVersion = algorithmVersion;
        this.coldStart = coldStart;
        this.rankingReason = rankingReason;
    }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Map<String, Double> getSubScores() { return subScores; }
    public void setSubScores(Map<String, Double> subScores) { this.subScores = subScores; }

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }

    public boolean isColdStart() { return coldStart; }
    public void setColdStart(boolean coldStart) { this.coldStart = coldStart; }

    public String getRankingReason() { return rankingReason; }
    public void setRankingReason(String rankingReason) { this.rankingReason = rankingReason; }
}
