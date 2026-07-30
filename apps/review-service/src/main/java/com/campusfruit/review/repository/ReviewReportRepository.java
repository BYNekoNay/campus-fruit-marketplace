package com.campusfruit.review.repository;

import com.campusfruit.review.entity.ReviewReport;
import com.campusfruit.review.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    List<ReviewReport> findByStatus(ReportStatus status);
}
