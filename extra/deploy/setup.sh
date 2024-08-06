#!/bin/bash

# Prompt for input with default value and help text
prompt_for_input() {
    local placeholder="$1"
    local default_value="$2"
    local help_text="$3"

    local green=$(tput setaf 2)
    local blue=$(tput setaf 4)
    local reset=$(tput sgr0)

    local prompt="Zadajte hodnotu pre ${green}$placeholder${reset} $blue($help_text)${reset}"
    read -p "$prompt [$default_value]: " input
    echo "${input:-$default_value}"
}

# Create a backup of the existing application.yml if it exists
[ -f application.yml ] && cp application.yml application.yml.tmp

# Copy the example file to start the customization
cp application.example application.yml

# Define placeholders, their default values, and help texts
declare -A placeholders=(
    ["{SERVER_PORT}"]="9000|Očakáva sa číselná hodnota. Toto je port, na ktorom bude aplikácia dostupná, napr. pri zadaní 9000 bude aplikácia dostupná na http://localhost:9000"
    ["{CAPTCHA_SITE}"]="FCMG3GQNR1DAEFS1|SITE_KEY, ktorý získate registráciou na friendlycaptcha.com"
    ["{CAPTCHA_SECRET}"]="A1QFR7PV0CO12RKCKJ22UD7QSFIAOM1VTD55530BK7SA2H4AK20E0FQDSV|SECRET_KEY, ktorý získate registráciou na friendlycaptcha.com"
    ["{ENCRYPTED_FORM_RECIPIENTS}"]="martin.kovanic@oznamovatelia.sk,juraj.jando@oznamovatelia.sk,binconf@oznamovatelia.sk|Príjemcovia zašifrovaného emailu"
    ["{ENVIRONMENT_NAME}"]="UOO-TEST|Názov prostredia"
    ["{BASE_URL}"]="https://testovaci-formular.oznamovatelia.sk|Verejné URL, kde bude formulár bežať"
    ["{TORMAIL_ENABLED}"]="true|Zapnúť tormail? (defalt: true)"
    ["{TORMAIL_SERVER_PORT}"]="9001|Port tormail servera"
    ["{TORMAIL_BASE_URL}"]="http://localhost:9001|Tormail: URL"
    ["{TORMAIL_USERNAME}"]="testuoosoftip|Tormail: používateľské meno"
    ["{TORMAIL_PASSWORD}"]="TorMailHeslo|Tormail: heslo"
    ["{TORMAIL_HOST}"]="i57h5nz5xbc4ut5wtdanah2uzap36figz2cf7kln4clnibg26ksgyoyd.onion|Tormail: host adresa servera"
    ["{TORMAIL_PORT}"]="25|Tormail: SMTP port"
    ["{TORMAIL_FROM}"]="testuoosoftip@i57h5nz5xbc4ut5wtdanah2uzap36figz2cf7kln4clnibg26ksgyoyd.onion|Tormail: mailová adresa odosielateľa"
    ["{TORMAIL_AUTOPUBLISH_ENABLED}"]="true|Tormail: automatické publikovanie (východzia hodnota: true)"
    ["{TORMAIL_WORKING_DIRECTORY}"]="'tor-working-dir'|Tormail: pracovný adresár (východzia hodnota: tor-working-dir)"
    ["{TORMAIL_STARTUP_TIMEOUT}"]="60s|Tormail: timeout pri štartovaní služby (východzia hodnota: 60s)"
    ["{MAIL_FROM}"]="noreply@formular.oznamovatelia.sk|Mailová adresa odosielateľa pre TORMAIL"
    ["{MAIL_HOST}"]="smtp.eu.mailgun.org|DNS servera pre odosielanie SMTP správ"
    ["{MAIL_PORT}"]="587|Číslo portu na SMTP serveri pre oddosielanie správ"
    ["{MAIL_ENCODING}"]="UTF-8|Kódovanie správ (UTF-8)"
    ["{MAIL_USERNAME}"]="noreply@formular.oznamovatelia.sk|Mailová adresa odosielateľa pre SMTPMAIL"
    ["{MAIL_PASSWORD}"]="TajneMailHeslo|Heslo pre email"
    ["{MAIL_SMTP_AUTH}"]="true|Používať SMTP autorizáciu? (východzia hodnota: true)"
    ["{MAIL_PROTOCOL}"]="smtp|Protokol na odosielanie pošty (východzia hodnota: smtp)"
    ["{MAIL_STARTTLS_ENABLED}"]="true|Zapnúť STARTTLS?"
    ["{MAIL_TEST_CONNECTION}"]="false|Použiť testovacie spojenie pri mailoch?"
    ["{MAIL_MAXSIZE}"]="10485760|10MB maximálna velkosť prilohy. Pokial je prekročená, tak príloha sa rozdelí do viacerých zip súborov. Každý súbor je poslaný v samostatnom maili."
    ["{RATELIMIT_SCHEDULER_DELAY}"]="60000|Perióda, v akej sa kontroluje zoznam s mailami (v milisekundách)"
    ["{RATELIMIT_SCHEDULER_MAXDELAY}"]="300|Čas, po akom sa mail vyhodí zo zoznamu (sekundy)"
    ["{TIME_BETWEEN_LINK_REQUESTS}"]="2|Čas, po akom je možné znovu požiadať o link (minúty)"
    ["{LINK_TIME_VALIDITY}"]="60|Časová platnosť linku - čas, za ktorý je možné link použíť (minúty)"
    ["{AESKEY_FILENAME}"]="./config/link-aes.p12|Cesta ku konfigurácii šifrovacieho kľúča"
    ["{AESKEY_KEY_PASSWORD}"]="keystorePassword|Heslo na ochranu privátneho kľúča."
    ["{AESKEY_STORE_PASSWORD}"]="keystorePassword|Heslo na ochranu celého súboru PKCS#12."
    ["{AESKEY_ALIAS}"]="link|Alias pre AES kľúč (ktorý bol použitý pri generovaní kľúča)."
    ["{PGP_PUBLICKEY_FILENAME}"]="./config/pgp_public.asc|Cesta k verejnému PGP kľúču"
    ["{SPRING_ADD_MAPPINGS}"]="false|Konfiguračný parameter SPRING_ADD_MAPPINGS"
    ["{SPRING_FILESIZE_THRESHOLD}"]="50MB|Konfiguračný parameter SPRING_FILESIZE_THRESHOLD"
    ["{SPRING_LOCATION}"]="D:/temp|Konfiguračný parameter SPRING_LOCATION"
    ["{SPRING_MAX_FILE_SIZE}"]="250MB|Konfiguračný parameter SPRING_MAX_FILE_SIZE"
    ["{SPRING_MAX_REQUEST_SIZE}"]="250MB|Konfiguračný parameter SPRING_MAX_REQUEST_SIZE"
    ["{PROXY_HOST}"]="empty|Východzia hodnota: reťazec 'empty'"
    ["{PROXY_PORT}"]=" |Východzia hodnota: prázdny reťazec (stlačte enter)"
    ["{LOGGING_LEVEL_ROOT}"]="INFO|Úroveň logovania"
)

echo "$(tput setaf 2)----------------------$(tput sgr0)"
echo "$(tput setaf 2)formular-oznamovatelia$(tput sgr0)"
echo "$(tput setaf 2)----------------------$(tput sgr0)"
echo ""
echo "Začína sa sprievodca vytvorením konfiguračného súboru application.yml."
echo "Budete vyzvaní na odpovedanie na sériu otázok na nastavenie vašej konfigurácie."
echo "Na konci tohto procesu bude vytvorený alebo aktualizovaný súbor application.yml s vašimi nastaveniami."

echo ""
echo "Ak chcete použiť východziu hodnotu, stlačte [Enter]"
echo ""
echo "$(tput setaf 3)!!!!! Pred tým, ako začnete, vygenerujte si privátny a verejný kľúč (bližšie informácie v dokumentácii) a uložte súbor link-aes.p12 do adresára config. !!!!!$(tput sgr0)"
echo ""

# Iterate over placeholders and prompt for user input
for placeholder in "${!placeholders[@]}"; do
    IFS='|' read -r default_value help_text <<< "${placeholders[$placeholder]}"
    value=$(prompt_for_input "$placeholder" "$default_value" "$help_text")
    sed -i "s|$placeholder|$value|g" application.yml
done

# Check for diff and ask for confirmation to overwrite
if [ -f application.yml.tmp ]; then
    echo "$(tput setaf 3)Rozdiely oproti minulej verzii application.yml:$(tput sgr0)"
    diff application.yml application.yml.tmp
    rm application.yml.tmp
    echo "$(tput setaf 2)Súbor application.yml s vašimi vlastnými konfiguráciami bol úspešne vytvorený.$(tput sgr0)"
fi
