package com.softip.uoo.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TorMailDto {
    String to;
    List<String> cc;
    List<String> bcc;
    String subject;
    String text;
    Boolean html;
}
