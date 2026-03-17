# Partage de la discussion

## Contexte
Le backend Spring Boot de l'application rencontre des erreurs lors du démarrage et de l'interaction avec Keycloak.

## Étapes principales et résolution

1. **Problème initial** : Conflit de beans `KeycloakConfig` dû à deux classes dans des packages différents. Le fichier incorrect dans `entities/` a été supprimé.
2. **Erreur 401 lors de la connexion au client admin Keycloak** : Le bean `KeycloakConfig` testait la connexion à chaque démarrage et levait une erreur lorsqu'il n'obtenait pas de jeton. Le test a été retiré et la configuration sécurisée.
3. **Propriétés manquantes** : Ajout de la propriété `keycloak.realm` dans `application.yml` et explication des credentials.
4. **Erreur d'inscription front/backend** : L'inscription échouait car l'admin Keycloak n'avait pas les rôles nécessaires (`realm-admin`/`manage-users`). Des commentaires explicatifs ont été rajoutés dans `application.yml` et la gestion des exceptions affinée.
5. **Erreur frontend Keycloak (`401 Unauthorized`)** : Probablement due à un client Keycloak mal configuré (type, redirect URIs, PKCE) ou l'absence de droits.

## Erreur exacte recueillie côté navigateur
```
Download the React DevTools for a better development experience: https://react.dev/link/react-devtools
keycloak.js:21 Nouvelle instance Keycloak créée avec URL: http://localhost:8088
:8088/realms/mutuelle-realm/protocol/openid-connect/token:1   Failed to load resource: the server responded with a status of 401 (Unauthorized)
keycloak.js:51  Erreur init Keycloak: NetworkError: Server responded with an invalid status.
    at fetchWithErrorHandling (keycloak.js:2141:1)
    at async fetchJSON (keycloak.js:2124:1)
    at async fetchAccessToken (keycloak.js:2086:1)
    at async #processCallback (keycloak.js:1164:1)
    at async #processInit (keycloak.js:811:1)
    at async Keycloak.init (keycloak.js:278:1)
(anonymous) @ keycloak.js:51
...
```

## Recommandations pour corriger le 401 frontend
- Vérifier le client `mutuelle-client` dans Keycloak : **public**, autorisation code + PKCE, URIs valides (`http://localhost:8081/*`), Web Origins.
- Assurer que le realm et l'URL sont corrects dans la configuration React (URL, clientId, realm).
- Vérifier le rôle du compte admin du backend dans Keycloak (master realm -> user -> Role Mappings -> `realm-admin` ou `manage-users`).


Ce fichier contient un résumé de la session et peut être partagé ou archivé.