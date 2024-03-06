Popis sa nachádza aj v súbore server/README.md

# UOO - Úrad na ochranu oznamovateľov #

## Buildovanie ##

buildovanie bez vytvorenia docker images

    mvn install 

buildovanie s vytvorenia docker images

    mvn deploy

## Spustenie aplikácie ##

### command line ###

java -jar server-1.0.0-SNAPSHOT-exec.jar


## Kde je dostupná aplikácie ##

|                           | url                                                                                                                                                                                      |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| mailový (úvodný) fromular | http://localhost:9000/form/mail                                                                                                                                                          |
| verzia                    | http://localhost:9000/form/getVersion                                                                                                                                                    |
| formular                  | http://localhost:9000/form?payload=zC%2BBLTBsN1uPrBk8FWm6ROVmbPEOpEJsHg39HtPzDB7iGyXI8FcSrrIpO0eCiIYUA5YPaSUfbH7j2aUMiglRXlOqsBrZFlyzLd8q3KTsAk3h0nISQA%3D%3D&nonce=DOoFY6pmXyz3C7I%2B   |


## Smoke test

http://localhost:9000/form/getVersion

# Nasadenie aplikácii - posielanie tor mailv

Aplikácia sa nasadzuje ako dve inštancie.

Prvá inštancia slúži na prácu s formulármi a poslianie klasických mailov.
V applicatin.yml má vypnutý tor server

            org.tbk.tor:
               enabled: false

Druhá inštancia slúži na posielanie tor mailv a beži tu aj tor server.
Druhá inštancia sa spúšta s profilom --spring.profiles.active=torMail
a beží na porte 9001

            spring:
               profiles: torMail
            server.port: 9001
            org.tbk.tor:
               enabled: true  # default is `true`

Parameter baseUrlTorMail slúž pre inst1 na posilanie tor mailv - prevolanie rest api na inst2

      baseUrlTorMail: http://localhost:9001 # url kde beží aplikácia s profilom torMail - pre posielanie tor mailov

### Onionmail nenasadený

Pokiaľ sa **nenasadzuje druhá inštancia** s profilom torMail tak v aplication.yaml je potrebné nastaviť **app.baseUrlTorMail** na prázdnu hodnotu.
Aplikácia cez tento parameter vie, že nemá onionmail nasadený a na úvodnom formuláry vykoná kontrolu email adresy (Email adresa nesmie končiť na .onion).

### CAPTCHA ###

Aplikácia používa Friendly Captcha
https://friendlycaptcha.com/signup/
https://docs.friendlycaptcha.com/#/

Konfigurácia sa nachádza v application.yml

```
app:
   captcha:
      site:
      secret:
```

Pokiaľ nie je vyplnená captcha.site v appilcation.yml tak sa captcha nezobrazí-nepoužije na stránke.

## **HELM CHARTS**

V values-formular.yaml nastaviť ingress host. (aktuálne je kubernetes.docker.internal)

Nasadenie aplikácie bez torMail

1. upraviť uoo\docker\chart\uoo\values-formular.yaml
   baseUrlTorMail nastaviť prázdne
2. cd uoo\docker\chart\uoo
3. `helm install formular ./ -f values-formular.yaml`

Nasadenie aplikácie a torMail

1. cd uoo\docker\chart\uoo
2. `helm install formular ./ -f values-formular.yaml`
3. `helm install tormail ./ -f values-tormail.yaml`




