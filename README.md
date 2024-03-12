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




# **Rýchly štart**

1. Nainštalovať Rancher desktop https://rancherdesktop.io/ - obsahuje K3s 
2. Nainštalovať Helm charts https://helm.sh/
3. vytvoriť image - mvn deploy


## Bez TorMail
4. Úprava  [values-formular.yaml](docker%2Fchart%2Fuoo%2Fvalues-formular.yaml) nastavit:
    - sendEncryptedFormTo: zmenitAdresu@todo.sk
    - baseUrlTorMail: # PRAZDNE 
    - host: smtp.todo.sk 
5. cd docker/chart/uoo
6. helm install formular ./ -f values-formular.yaml

## S TorMail

4. Úprava  [values-formular.yaml](docker%2Fchart%2Fuoo%2Fvalues-formular.yaml) nastavit:
   - sendEncryptedFormTo: zmenitAdresu@todo.sk
   - host: smtp.todo.sk
5. cd docker/chart/uoo
6. helm install formular ./ -f values-formular.yaml

7. Úprava [values-tormail.yaml](docker%2Fchart%2Fuoo%2Fvalues-tormail.yaml)  nastavit:
   - sendEncryptedFormTo: zmenitAdresu@todo.sk
   - torMail:
     username: username
     password: password
     host: host.onion
     port: 25
     from: todo@todo.onion
   -  mailFrom: noreply@noreply.sk
8. cd docker/chart/uoo
9. helm install tormail ./ -f values-formular.yaml

aplikácia beží na http://kubernetes.docker.internal/form/mail
