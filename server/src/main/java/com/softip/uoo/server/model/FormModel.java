package com.softip.uoo.server.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class FormModel {

    private String email;
    private String nonce;

    private String infNekalaPraktikaSuvislostVasaPraca;
    private String identifikacia;
    private String opisNekalejPraktiky;
    private String popisOhrozenieVerejnyZaujem;
    private String kedyAkoDlho;
    private String dokumentyKDispozicii;
    private String oznamenieZamestnavatel;
    private String oznamenieNaPoliciu;
    private String oznamenieInyOrgan;
    private String oznamenieInyOrganPopis;
    private String oznamenieNie;
    private String akoPresetrene;
    private String odvetneOpatrenia;
    private String popisOdvetneOpatrenia;
    private String popisOcakavania;
    private String dozvedenieOUrade;
    private String suhlasSpristupnenie;
    private String meno;
    private String telefon;
    private String zaverecnePotvrdenie;

    private MultipartFile[] files;
    private String env;
    private String formAction;
    private boolean tor;

    private String maxFileSize;

}
