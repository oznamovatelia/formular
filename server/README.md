# 1. generovnie AES kluca pre sifrovanie linku

```
Java\jdk-11.0.12\bin
keytool -genseckey -keystore link-aes.p12 -storetype PKCS12 -storepass keystorePassword -keyalg AES -keysize 256 -alias link -keypass keystorePassword

```
kluc **nakopirujem do projektu server->resources**

zmenim parametre(filename, password...) v **application.yml v sekcii aesKey**

potrebné nastaviť v application.yml
aesKey:
fileName: link-aes.p12
keyPassword: keystorePassword
storePassword: keystorePassword  
alias: link

chyba v keytool = keyPassword a storePassword musia byť rovnaké (možno nasledne zmeniť)

# 2. pgp sifrovanie zip

download Kleopatra pre spravu pgp sifier 
```
https://gpg4win.org/get-gpg4win.html
```

```
File->New Key Pair-> Create Personal OpenPGP key pair-> zadam heslo-> vygenerujem kluce a ulozim si ich
```
public kluc **nakopirujem do projektu server->resources**

zmenim parameter **pgpPublicKeyFilename v application.yml**


# Workflow a popis

1.  http://localhost:9000/swagger-ui/index.html
2.  API **/form/send-link**
    
    Vytvori sa objekt s parametrami dateTime a email. Objekt sa serializuje na string a tento "string" sa potom zasifruje pomocou **AES kluca**
    
    Metoda, ktora zasifruje tento "string" vrati naspat aj **IV vektor, ktory je potrebny na rozsifrovanie**
    
    Tento vektor **nezasifrovany** posleme v linku ako parameter nonce
    
    **Priklad linku:**
    ```
    http://localhost:9000/form?payload={zasifrovanyPayload}&nonce={IV vektor}
    ```
    
    Takyto link sa odosle na zadany email
    
    Do **rateLimit** si zaznamenam dateTime, email, nonce. Vid kod.
    
3.  pride mi email s linkom na formular
4.  som presmerovany na **GET /form**
    
    Rozsifrujem si **payload** z requestu pomocou **toho isteho AES kluca ako v bode 2. a nonce(IV vektor) z requestu**.
    
    Zdeserializujem "string" na objekt s dateTime a email
    
    Skontrolujem **rateLimit**, ci formular uz nebol predtym dotazovany. Vid kod.
    
    Redirect na **thymeleaf** template s formularom /form
    
5.  Vyplnim udaje, nahram subory a **submitnem** formular
6. som presmerovany na **POST /form**
    Skontrolujem rateLimit ci nebol formular predtym uz odoslany. Vid kod.

   Zoberiem **template.docx (sablona s premennymi, ktore budem replacovat)**

   Replacnem premenne v template.docx s premennymi z formulara

   Vytvorim ZIP a vlozim do neho tento **docx a vsetky prilohy**

   Tento ZIP zasifrujem pomocou **PGP kluca**

   Odoslem na email

7. Rozsifrujem

   otvorim **Kleopatru**

   klik **Decrypt/Verify** a vyberiem zip subor, ktory prisiel v maily

   Zadam heslo

   **Hotovo**

# LOGY

**Logy su vypnuté** v application.yml.

```
logging:
  level:
    root: OFF
```

# 3. Nastavenie veľkosti upload súboru

## Nginx

    /etc/nginx/nginx.conf
       parameter client_max_body_size 

Pokiaľ je prekročená velkosť tak sa objavi exception 413 Request Entity Too Large (nginx1.23.1)
https://www.cyberciti.biz/faq/linux-unix-bsd-nginx-413-request-entity-too-large/

## Aplikácia - Tomcat

nastavenie velkosti v application.yml
spring:
servlet:
multipart:
max-file-size: 250MB
max-request-size: 250MB

Pokiaľ je prekročená veľkosť tak sa objavi exception org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException

Frontend kontroluje veľkosť príloh a nedovolí odoslanie formu, ktorý obsahuje súbory s vačšou sumárnou velikostou ako max-file-size.

## SMTP server

SMTP server má nastavenú max velkosť.
Pokiaľ je prekročená velkosť, tak sa objavi exception org.springframework.mail.MailSendException: Failed messages: com.sun.mail.smtp.SMTPSendFailedException: 552 Message size exceeds maximum permitted

Aplikácia má parameter maxMailSize. Ak je príloha väčšia ako maxMailSize tak sa vytvára multipart zip a súbory sú posielané po jednom.

# 4. Multijazyčnosť

Základný jazyk je sk. Texty sa nachádzajú v messages.properties. Na formuláry je možné prepnúť jazyk na en. Texty pre en verziu sa nachádzajú
v messages_en.properties.

    en: http://localhost:9000/form/mail?lang=en
    sk: http://localhost:9000/form/mail?lang=sk
    sk: http://localhost:9000/form/mail
    
    en: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3&lang=en
    sk: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3&lang=sk
    sk: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3

# 5. Konfigurácia - application.yml

**timeBetweenLinkRequests: 2** ---> čas po akom je možné znovu požiadať o link (minúty)

**linkTimeValidiry: 60** ---> ćasová platnosť linku – čas za ktorý je možné link použíť (minúty)

**rateLimitSchedulerDelay: 60000** --->  Perioda v akej sa spúšťa RateLimitScheduler, ktorý kontroluje zoznam s mailami.
Proces pri kontrole:

1. vyhodí zo zoznamu tie záznamy ktoré prekročili časovú platnosť linku (linkTimeValidiry)
2. vyhodí zo zoznamu tie záznamy ktoré boli použité a uplynul predpísaný čas potrebný pre opatovné poźiadanie o link (timeBetweenLinkRequests)

Pri odosielaní formulára sa kontroluje zoznam či obsahuje link a či link už nebol použitý.

#### maxMailSize - maximálna velkosť prilohy v maily.

Po vytvorení formular.zip.enc (zašifrovaný zip, ktorý obsahuje formulár a priložené súbory) sa skontroluje velkosť súboru. Pokiaľ prekračuje maxMailSize tak je súbor rozdelený do čiastkových zip
suborov a poslaný po častiach.

## Pridanie submodulu do projektu

### Pridanie uuo_webres submodulu do adresara resources hlavneho projektu uuo

1. Pridanie submodulu:
   v uoo/server
   ` git submodule add https://bitbucket.org/todo/uoo_webres.git src/main/resources/uoo_webres`

2. Inicializácia a aktualizácia submodulu:
   `git submodule update --init`

#### Zmena verzie submodulu

v uoo/server/src/main/resources/uoo_webres
`git checkout [vetva]`

### Dotiahnutie hlavného projektu - clone

`git clone -b [vetva] https://bitbucket.org/todo/uoo.git --recursive`

**--recursive** - dotiahnu sa aj submoduly

Pokiaľ sa nepoužije --recursive tak sa submoduly dotiahnu príkazom:
`git submodule update --init --recursive`

### Aktualizácia submodulu
`git submodule update --remote`

### Zmean repository submodulu

1. Zmena URL repozitára submodulu:
    `git submodule set-url <submodule-path> <new-repository-url>`
   3. Aktualizácia submodulu:
    `git submodule update --init --recursive`
4. Commit a push zmien
   `git add .
   git commit -m "Updated submodule to new repository"
   git push`

