package com.campusfruit.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CanonicalFruitRequest {

    @NotBlank(message = "品类不能为空")
    @Size(max = 100)
    private String category;

    @NotBlank(message = "品种不能为空")
    @Size(max = 200)
    private String variety;

    @NotBlank(message = "等级不能为空")
    @Size(max = 50)
    private String grade;

    @Size(max = 200)
    private String origin;

    @Size(max = 20)
    private String defaultUnit;

    private Long comparisonGroupId;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }

    public Long getComparisonGroupId() { return comparisonGroupId; }
    public void setComparisonGroupId(Long comparisonGroupId) { this.comparisonGroupId = comparisonGroupId; }
}
