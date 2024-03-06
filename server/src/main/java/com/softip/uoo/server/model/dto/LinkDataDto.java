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

/**
 * udaje zasielane zasifrovane ako link na mail
 */
public class LinkDataDto implements Serializable {

    private String email;

    private LocalDateTime dateTime;

}
