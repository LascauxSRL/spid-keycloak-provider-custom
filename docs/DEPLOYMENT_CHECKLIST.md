# Checklist per il Deploy del Provider SPID

## Problema: Le modifiche non sembrano essere applicate

Se dopo aver modificato il codice e deployato il provider, le modifiche non sono visibili, segui questa checklist:

## 1. Verifica Compilazione

Assicurati che il codice compili correttamente:

```bash
mvn clean compile
```

Se ci sono errori, risolvili prima di procedere.

## 2. Build del JAR

Compila il JAR completo:

```bash
mvn clean package
```

Verifica che il file `target/spid-provider.jar` sia stato generato e che la data di modifica sia recente.

## 3. Verifica Configurazione Client

Controlla se il client ha un attributo `spid.attributeConsumingServiceIndex` configurato:

```bash
# Via REST API
curl -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=soggetto-aggregato-3" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.[0].attributes'
```

Se vedi `"spid.attributeConsumingServiceIndex": "1"`, questo spiega perché l'index è 1 invece di 0.

## 4. Deploy del Provider

### Opzione A: Deploy in `providers/` (Keycloak 17+)

```bash
# Copia il JAR nella directory providers
sudo cp target/spid-provider.jar ${KEYCLOAK_HOME}/providers/

# Imposta i permessi corretti
sudo chown keycloak:keycloak ${KEYCLOAK_HOME}/providers/spid-provider.jar
sudo chmod 644 ${KEYCLOAK_HOME}/providers/spid-provider.jar
```

### Opzione B: Deploy in `deployments/` (Keycloak standalone)

```bash
sudo cp target/spid-provider.jar ${KEYCLOAK_HOME}/standalone/deployments/
sudo chown keycloak:keycloak ${KEYCLOAK_HOME}/standalone/deployments/spid-provider.jar
```

## 5. Riavvio di Keycloak

**IMPORTANTE**: Dopo il deploy, **riavvia completamente Keycloak**:

```bash
# Se usi systemd
sudo systemctl restart keycloak

# Se usi Docker
docker restart keycloak-container

# Se usi standalone
${KEYCLOAK_HOME}/bin/kc.sh stop
${KEYCLOAK_HOME}/bin/kc.sh start
```

## 6. Verifica Caricamento del Provider

Dopo il riavvio, verifica nei log che il provider sia stato caricato:

```bash
# Cerca nei log
grep -i "spid" ${KEYCLOAK_HOME}/logs/server.log | tail -20
```

Dovresti vedere messaggi relativi al caricamento del provider SPID.

## 7. Pulizia Cache Browser

**IMPORTANTE**: Pulisci la cache del browser o usa la modalità incognito quando verifichi le modifiche:

- Chrome/Edge: `Ctrl+Shift+Delete` → Seleziona "Immagini e file in cache" → "Cancella dati"
- Firefox: `Ctrl+Shift+Delete` → Seleziona "Cache" → "Cancella"

Oppure usa la modalità incognito/privata.

## 8. Verifica Metadata Generato

Controlla il metadata generato:

```bash
curl "http://localhost:10001/realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-3" | xmllint --format -
```

Verifica che:
- L'`AttributeConsumingService` abbia `index="0"` (se non configurato diversamente)
- Gli endpoint includano il path `/clients/soggetto-aggregato-3`
- I `RequestedAttribute` siano presenti

## 9. Debug: Verifica Versione del JAR

Per verificare che il JAR deployato contenga le tue modifiche:

```bash
# Estrai il JAR e verifica le classi
cd /tmp
jar xf ${KEYCLOAK_HOME}/providers/spid-provider.jar
strings org/keycloak/broker/spid/metadata/SpidSpMetadataResourceProvider.class | grep -i "attributeConsumingServiceIndex.*0"
```

Oppure verifica la data di modifica del JAR:

```bash
ls -lh ${KEYCLOAK_HOME}/providers/spid-provider.jar
```

## 10. Problemi Comuni

### Il metadata mostra ancora `index="1"`

**Possibili cause:**
1. Il client ha `spid.attributeConsumingServiceIndex="1"` configurato esplicitamente
2. Il JAR non è stato ricompilato correttamente
3. Keycloak sta usando una versione cached del provider
4. Il JAR non è stato deployato nella directory corretta

**Soluzione:**
1. Verifica la configurazione del client (vedi punto 3)
2. Ricompila e redeploya il JAR
3. Riavvia Keycloak completamente
4. Pulisci la cache del browser

### Il provider non viene caricato

**Possibili cause:**
1. Permessi errati sul file JAR
2. JAR corrotto o incompleto
3. Versione di Keycloak incompatibile

**Soluzione:**
1. Verifica i permessi: `ls -l ${KEYCLOAK_HOME}/providers/spid-provider.jar`
2. Ricompila il JAR: `mvn clean package`
3. Verifica la versione di Keycloak: deve essere 26.1.4

## 11. Comando Completo di Deploy

Ecco un comando completo che fa tutto in una volta:

```bash
# 1. Compila
mvn clean package

# 2. Backup del vecchio JAR (opzionale ma consigliato)
sudo cp ${KEYCLOAK_HOME}/providers/spid-provider.jar ${KEYCLOAK_HOME}/providers/spid-provider.jar.backup

# 3. Deploy
sudo install -C -o keycloak -g keycloak target/spid-provider.jar ${KEYCLOAK_HOME}/providers/

# 4. Riavvia Keycloak
sudo systemctl restart keycloak

# 5. Attendi che Keycloak si avvii (circa 30-60 secondi)
sleep 60

# 6. Verifica nei log
sudo tail -f ${KEYCLOAK_HOME}/logs/server.log | grep -i spid
```

## 12. Verifica Finale

Dopo il deploy, verifica che tutto funzioni:

1. **Metadata endpoint**: Accedi a `/realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-3`
2. **Verifica index**: L'`AttributeConsumingService` dovrebbe avere `index="0"` (a meno che non sia configurato diversamente)
3. **Verifica endpoint**: Gli endpoint dovrebbero includere `/clients/soggetto-aggregato-3`

Se tutto è corretto, le modifiche sono state applicate con successo!
