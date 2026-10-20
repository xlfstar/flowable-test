package com.example.flowable.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveApplyRequest {

    @NotNull(message = "请假天数不能为空")
    @Min(value = 1, message = "请假天数至少为1天")
    private Integer days;

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotBlank(message = "请假事由不能为空")
    private String reason;
}
