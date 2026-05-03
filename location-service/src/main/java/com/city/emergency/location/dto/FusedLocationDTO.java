package com.city.emergency.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FusedLocationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long resourceId;
    private Double longitude;
    private Double latitude;
    private Double altitude;
    private Double speed;
    private Double direction;
    private Double accuracy;
    private Integer fusedSources;
    private LocalDateTime timestamp;
    private Boolean isHighAccurate;
    private Double confidence;
}
