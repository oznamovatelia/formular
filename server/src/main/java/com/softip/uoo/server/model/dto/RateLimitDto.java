package com.softip.uoo.server.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class RateLimitDto implements Serializable {

    private String email;

    private String nonce; // iv vektor

    private LocalDateTime dateTime;

    private boolean used;
}
