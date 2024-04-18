Popis sa nachádza aj v súbore `server/README.md`

# Nahlasovací formulár

## Buildovanie

 **Java 11 JDK**

 **Maven ver. 3.3.x a vyššia**

Buildovanie bez vytvorenia Docker images:

    mvn install

Buildovanie s vytvorením Docker images:

    mvn deploy

## Spustenie aplikácie

### Command line

    java -jar server-1.0.0-SNAPSHOT-exec.jar


## Kde je dostupná aplikácia

|                           | URL                                                                                                                                                                                      |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Mailový (úvodný) formulár | http://localhost:9000/form/mail                                                                                                                                                          |
| Verzia                    | http://localhost:9000/form/getVersion                                                                                                                                                    |
| Formulár                  | http://localhost:9000/form?payload=zC%2BBLTBsN1uPrBk8FWm6ROVmbPEOpEJsHg39HtPzDB7iGyXI8FcSrrIpO0eCiIYUA5YPaSUfbH7j2aUMiglRXlOqsBrZFlyzLd8q3KTsAk3h0nISQA%3D%3D&nonce=DOoFY6pmXyz3C7I%2B   |


## Smoke test

    http://localhost:9000/form/getVersion

# Nasadenie aplikácie - posielanie Tor mailov

Aplikácia sa nasadzuje ako dve inštancie.

Prvá inštancia slúži na prácu s formulármi a posielanie klasických mailov.
V `application.yml` má vypnutý Tor server:

    org.tbk.tor:
       enabled: false

Druhá inštancia slúži na posielanie Tor mailov a beží tu aj Tor server.
Druhá inštancia sa spúšťa s profilom `--spring.profiles.active=torMail` a beží na porte 9001:

    spring:
       profiles: torMail
    server.port: 9001
    org.tbk.tor:
       enabled: true  # default is `true`

Parameter `baseUrlTorMail` slúži pre inštanciu 1 na posielanie Tor mailov - prevolanie REST API na inštanciu 2.

    baseUrlTorMail: http://localhost:9001 # URL, kde beží aplikácia s profilom torMail - pre posielanie Tor mailov

### Onionmail nenasadený

Pokiaľ sa **nenasadzuje druhá inštancia** s profilom torMail, tak v `application.yaml` je potrebné nastaviť **app.baseUrlTorMail** na prázdnu hodnotu.

Aplikácia cez tento parameter vie, že nemá Onionmail nasadený, a na úvodnom formulári vykoná kontrolu emailovej adresy (Email adresa nesmie končiť na `.onion`).

### CAPTCHA

Aplikácia používa Friendly Captcha:
https://friendlycaptcha.com/signup/
https://docs.friendlycaptcha.com/#/

Konfigurácia sa nachádza v `application.yml`:

```
app:
   captcha:
      site:
      secret:
```

Pokiaľ nie je vyplnená `captcha.site` v `application.yml`, tak sa captcha nezobrazí/nepoužije na stránke.

## Pridanie submodulu do projektu

### Pridanie `uuo_webres` submodulu do adresára `resources` hlavného projektu `uuo`

1. Pridanie submodulu:
   v `uoo/server`:
   `git submodule add https://bitbucket.org/todo/uoo_webres.git src/main/resources/uoo_webres`

2. Inicializácia a aktualizácia submodulu:
   `git submodule update --init`

#### Zmena verzie submodulu

v `uoo/server/src/main/resources/uoo_webres`:
`git checkout [vetva]`

### Dotiahnutie hlavného projektu - clone

`git clone -b [vetva] https://bitbucket.org/todo/uoo.git --recursive`

**--recursive** - dotiahnu sa aj submoduly.

Pokiaľ sa nepoužije `--recursive`, tak sa submoduly dotiahnu príkazom:
`git submodule update --init --recursive`

### Aktualizácia submodulu
`git submodule update --remote`

### Zmena repository submodulu

1. Zmena URL repozitára submodulu:
   `git submodule set-url <submodule-path> <new-repository-url>`
2. Aktualizácia submodulu:
   `git submodule update --init --recursive`
3. Commit a push zmien:
   `git add .`
   `git commit -m "Updated submodule to new repository"`
   `git push`

## **HELM CHARTS**

Vo `values-formular.yaml` nastaviť `ingress host` (aktuálne je `kubernetes.docker.internal`).

Nasadenie aplikácie bez `torMail`:

1. Upraviť `uoo\docker\chart\uoo\values-formular.yaml`
   `baseUrlTorMail` nastaviť na prázdne
2. `cd uoo\docker\chart\uoo`
3. `helm install formular ./ -f values-formular.yaml`

Nasadenie aplikácie a `torMail`:

1. `cd uoo\docker\chart\uoo`
2. `helm install formular ./ -f values-formular.yaml`
3. `helm install tormail ./ -f values-tormail.yaml`

# **Rýchly štart v K3s**

1. Nainštalovať Rancher desktop https://rancherdesktop.io/ - obsahuje K3s
2. Nainštalovať Helm charts https://helm.sh/
4. Dotiahnuť zdrojový kód z gitu: `git clone uoo_public.git` plus aktualizácia submodulov `git submodule update --init --recursive`
5. Build aplikácie + vytvoriť image -> `mvn deploy`

Počas buildu sa vytvorí jar súbor, ktorý sa uloží do lokálneho maven repository.
Následne sa vytvorí Docker image pomocou lokálneho Docker daemon.
(Pozn. SOFTIP: `Built image to Docker daemon as nexus.softip.sk/sk.softip.uoo/server:1.0.5-SNAPSHOT`)

## Naštartovanie aplikácie v K3s bez TorMail pomocou helm
6. Úprava [values-formular.yaml](docker%2Fchart%2Fuoo%2Fvalues-formular.yaml) nastaviť:
    - sendEncryptedFormTo: `zmenitAdresu@todo.sk`
    - baseUrlTorMail: # PRÁZDNE
    - host: `smtp.todo.sk`
7. `cd docker/chart/uoo`
8. `helm install formular ./ -f values-formular.yaml`

## Naštartovanie aplikácie v K3s s TorMail pomocou helm

6. Úprava [values-formular.yaml](docker%2Fchart%2Fuoo%2Fvalues-formular.yaml) nastaviť:
   - sendEncryptedFormTo: `zmenitAdresu@todo.sk`
   - host: `smtp.todo.sk`
7. `cd docker/chart/uoo`
8. `helm install formular ./ -f values-formular.yaml`

9. Úprava [values-tormail.yaml](docker%2Fchart%2Fuoo%2Fvalues-tormail.yaml) nastaviť:
   - sendEncryptedFormTo: `zmenitAdresu@todo.sk`
   - torMail:
     username: `username`
     password: `password`
     host: `host.onion`
     port: 25
     from: `todo@todo.onion`
   - mailFrom: `noreply@noreply.sk`
10. `cd docker/chart/uoo`
11. `helm install tormail ./ -f values-formular.yaml`

Aplikácia beží na `http://kubernetes.docker.internal/form/mail`

---

Kontakt: [Úrad na ochranu oznamovateľov](https://www.oznamovatelia.sk/). Softvér je zverejnený pod otvorenou licenciou [EUPL](LICENSE.txt).
