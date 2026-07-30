package com.campusfruit.review.moderation;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 审核日志实体。
 * 记录所有审核操作（举报处理、评价隐藏/恢复）的完整追踪。
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作类型：REPORT_REVIEW, HIDE_REVIEW, RESTORE_REVIEW */
    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    /** 相关评价 ID */
    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    /** 相关举报 ID（可选） */
    @Column(name = "report_id")
    private Long reportId;

    /** 操作人 ID */
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    /** 操作备注/审核意见 */
    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    /** 操作前状态 */
    @Column(name = "previous_state", length = 100)
    private String previousState;

    /** 操作后状态 */
    @Column(name = "new_state", length = 100)
    private String newState;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- getters / setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public Instant getCreatedAt() { return createdAt; }
}
