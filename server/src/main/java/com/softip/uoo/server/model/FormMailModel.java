package com.softip.uoo.server.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class FormMailModel implements Serializable {

    private String email;
    private String env;
    private String formAction;
    private boolean tor;
}
