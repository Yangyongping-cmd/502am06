package com.city.emergency.reuse.dto;

import com.city.emergency.reuse.enums.ResourceModeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReuseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long resourceId;
    private String resourceName;
    private ResourceModeEnum currentMode;
    
    private Boolean allowReuse;
    private List<String> compatibleSkills;
    
    private LocalDateTime lastRescueTime;
    private LocalDateTime lastReuseTime;
    
    private Integer totalRescueCount;
    private Integer totalReuseCount;
    
    private Double reuseEfficiency;
    private Double utilizationRate;
    
    private Boolean isInPeakHour;
    private Integer peakHourReuseRatio;
    
    private LocalDateTime modeSwitchTime;
    private String currentOrderType;
    private Long currentOrderId;
    
    private Double estimatedReturnTime;
    private String statusMessage;
}
