# 1. generovanie AES kľúča pre šifrovanie linku

```
Java\jdk-11.0.12\bin
keytool -genseckey -keystore link-aes.p12 -storetype PKCS12 -storepass keystorePassword -keyalg AES -keysize 256 -alias link -keypass keystorePassword
```
kľúč **nakopírujem do projektu server->resources**

zmením parametre (filename, password...) v **application.yml v sekcii aesKey**

potrebné nastaviť v application.yml
aesKey:
fileName: link-aes.p12
keyPassword: keystorePassword
storePassword: keystorePassword
alias: link

chyba v keytool = keyPassword a storePassword musia byť rovnaké (možno následne zmeniť)

# 2. PGP šifrovanie zip

stiahni program Kleopatra pre správu PGP šifier
```
https://gpg4win.org/get-gpg4win.html
```

```
File->New Key Pair-> Create Personal OpenPGP key pair-> zadám heslo-> vygenerujem kľúče a uložím si ich
```
verejný kľúč **nakopírujem do projektu server->resources**

zmením parameter **pgpPublicKeyFilename v application.yml**


# Workflow a popis

1.  http://localhost:9000/swagger-ui/index.html
2.  API **/form/send-link**

    Vytvorí sa objekt s parametrami dateTime a email. Objekt sa serializuje na string a tento "string" sa potom zašifruje pomocou **AES kľúča**

    Metóda, ktorá zašifruje tento "string" vráti naspäť aj **IV vektor, ktorý je potrebný na rozšifrovanie**

    Tento vektor **nezašifrovaný** pošleme v linku ako parameter nonce

    **Príklad linku:**
    ```
    http://localhost:9000/form?payload={zašifrovanýPayload}&nonce={IV vektor}
    ```

    Takýto link sa odošle na zadaný email

    Do **rateLimit** si zaznamenám dateTime, email, nonce. Viď kód.

3.  príde mi email s linkom na formulár
4.  som presmerovaný na **GET /form**

    Rozšifrujem si **payload** z requestu pomocou **toho istého AES kľúča ako v bode 2. a nonce (IV vektor) z requestu**.

    Zdeserializujem "string" na objekt s dateTime a email

    Skontrolujem **rateLimit**, či formulár už nebol predtým dotazovaný. Viď kód.

    Redirect na **thymeleaf** template s formulárom /form

5.  Vyplním údaje, nahrám súbory a **submitnem** formulár
6.  som presmerovaný na **POST /form**

    Skontrolujem rateLimit či nebol formulár predtým už odoslaný. Vid kód.

   Zoberiem **template.docx (šablóna s premennými, ktoré budem nahradzovať)**

   Nahradím premenné v template.docx s premennými z formulára

   Vytvorím ZIP a vložím do neho tento **docx a všetky prílohy**

   Tento ZIP zašifrujem pomocou **PGP kľúča**

   Odošlem na email

7.  Rozšifrujem

   otvorím **Kleopatru**

   klik **Decrypt/Verify** a vyberiem zip súbor, ktorý prišiel v maile

   Zadám heslo

   **Hotovo**

# LOGY

**Logy sú vypnuté** v application.yml.

```
logging:
  level:
    root: OFF
```

# 3. Nastavenie veľkosti upload súboru

## Nginx

    /etc/nginx/nginx.conf
       parameter client_max_body_size

Pokiaľ je prekročená veľkosť, tak sa objaví exception 413 Request Entity Too Large (nginx1.23.1)
https://www.cyberciti.biz/faq/linux-unix-bsd-nginx-413-request-entity-too-large/

## Aplikácia - Tomcat

nastavenie veľkosti v application.yml
spring:
servlet:
multipart:
max-file-size: 250MB
max-request-size: 250MB

Pokiaľ je prekročená veľkosť, tak sa objaví exception org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException

Frontend kontroluje veľkosť príloh a nedovolí odoslanie formulára, ktorý obsahuje súbory s väčšou sumárnou veľkosťou ako max-file-size.

## SMTP server

SMTP server má nastavenú max veľkosť.
Pokiaľ je prekročená veľkosť, tak sa objaví exception org.springframework.mail.MailSendException: Failed messages: com.sun.mail.smtp.SMTPSendFailedException: 552 Message size exceeds maximum permitted

Aplikácia má parameter maxMailSize. Ak je príloha väčšia ako maxMailSize, tak sa vytvára multipart zip a súbory sú posielané po jednom.

# 4. Multijazyčnosť

Základný jazyk je sk. Texty sa nachádzajú v messages.properties. Na formulári je možné prepnúť jazyk na en. Texty pre en verziu sa nachádzajú v messages_en.properties.

    en: http://localhost:9000/form/mail?lang=en
    sk: http://localhost:9000/form/mail?lang=sk
    sk: http://localhost:9000/form/mail

    en: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3&lang=en
    sk: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3&lang=sk
    sk: http://localhost:9000/form?payload=%2B8pLcU877SGQYxI699pZHrrdoMWc2saN1B7XlTo4BxjyJkOibwtU10kKxYvLpaPO%2BbsMVpxfmH6FzlnXZw5PoekBvColq5bIvAw2utX8PQEMKHHWRGM%3D&nonce=uZwzYcDbnf3%2BNsj3

# 5. Konfigurácia - application.yml

**timeBetweenLinkRequests: 2** ---> čas, po akom je možné znovu požiadať o link (minúty)

**linkTimeValidity: 60** ---> časová platnosť linku – čas, za ktorý je možné link použiť (minúty)

**rateLimitSchedulerDelay: 60000** --->  Perióda, v akej sa spúšťa RateLimitScheduler, ktorý kontroluje zoznam s mailami.
Proces pri kontrole:

1. vyhodí zo zoznamu tie záznamy, ktoré prekročili časovú platnosť linku (linkTimeValidity)
2. vyhodí zo zoznamu tie záznamy, ktoré boli použité a uplynul predpísaný čas potrebný pre opätovné požiadenie o link (timeBetweenLinkRequests)

Pri odosielaní formulára sa kontroluje zoznam, či obsahuje link a či link už nebol použitý.

#### maxMailSize - maximálna veľkosť prílohy v maile.

Po vytvorení formular.zip.enc (zašifrovaný zip, ktorý obsahuje formulár a priložené súbory) sa skontroluje veľkosť súboru. Pokiaľ prekračuje maxMailSize, tak je súbor rozdelený do čiastkových zip súborov a poslaný po častiach.
