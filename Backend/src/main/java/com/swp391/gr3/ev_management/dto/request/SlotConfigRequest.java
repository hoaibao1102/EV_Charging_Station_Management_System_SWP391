package com.swp391.gr3.ev_management.dto.request;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotConfigRequest {

    @NotNull(message = "Thời lượng slot không được để trống")
    @Positive(message = "Thời lượng slot phải là số dương")
    private Integer slotDurationMin;

    @NotNull(message = "ID trạm không được để trống")
    @Positive(message = "ID trạm phải là số dương")
    private Long stationId;

    @NotNull(message = "Thời gian bắt đầu hiệu lực không được để trống")
    private LocalDateTime activeFrom;

    private LocalDateTime activeExpire;

    @NotNull(message = "Trạng thái không được để trống")
    private SlotConfigStatus isActive;
}
