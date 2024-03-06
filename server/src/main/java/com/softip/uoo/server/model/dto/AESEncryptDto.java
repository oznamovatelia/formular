package com.softip.uoo.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AESEncryptDto {

    private byte[] iv;
    private byte[] cipherText;
}
