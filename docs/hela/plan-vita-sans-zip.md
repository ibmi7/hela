# Plan : jeux Vita jouables sans leur zip

Rédigé le 15/11/2026 sur la branche `hela`. Fait le même jour (voir `etat-des-travaux.md`, section Lancement Vita) ; l'action n'a été ajoutée qu'au menu rapide de l'accueil, pas au menu de la bibliothèque.

## But

Un jeu PS Vita installé dans Vita3K (`<VitaFS>/ux0/app/<identifiant>`) reste jouable depuis Hela quand son zip est supprimé. Le zip peut partir à la main, ou automatiquement après une session si l'option est activée.

## Décisions prises

| Question | Choix |
|---|---|
| Ce qui remplace le zip | Un fichier témoin `<nom du zip sans .zip>.vita3k`, dans le même dossier, qui contient l'identifiant |
| Suppression manuelle | Nouvelle action « Libérer le zip » dans le menu du jeu, visible seulement pour un jeu Vita installé ; « Supprimer le fichier local » d'Argosy reste inchangé |
| Suppression automatique | Option par plateforme, désactivée par défaut, déclenchée en fin de session (après la synchro des sauvegardes) |
| Jeux installés sans zip jamais téléchargé | Plus tard, chantier séparé (reconnaissance par le nom) |

## Ce que le code impose

- Argosy considère un jeu téléchargé quand `games.localPath` est renseigné (`resolveDownloaded` dans `GameUiMapper`, `INSTALLED_SQL` et une quarantaine de requêtes de `GameDao`). La synchro des sauvegardes est filtrée de la même façon (`SyncCoordinator`, `SaveSyncOrchestrator`, `SaveDownloader`, `getDownloadedRommGameIds`…).
- Cinq passes effacent `localPath` quand le fichier manque : validation au démarrage (`runValidation`), ouverture d'une fiche (`validateAndDiscoverGame`), analyse d'une plateforme et synchro RomM (`validateLocalFilesForPlatform`, `syncRom`), lancement (`GameLauncher` l. 272-283). Le témoin existe, donc aucune ne se déclenche.
- `.vita3k` n'est pas dans les extensions de travail que `cleanupInvalidExtensionGames` efface (`RomMSyncFilter.NON_GAME_EXTENSIONS`).
- Le nom de canal des sauvegardes vient de `File(localPath).nameWithoutExtension` (`SaveSyncApiClient` l. 274, `SaveSyncConflictResolver` l. 238). `X.zip` et `X.vita3k` donnent tous deux `X` : les canaux existants restent les mêmes.
- `BaseRomFileResolver` réélit, pour la Vita, le plus gros fichier du dossier du jeu. Une fois le zip parti, ce serait le `.zip.m3u` voisin (Argosy en a créé un par jeu, qui liste le zip et les DLC) : le nom de canal deviendrait `X.zip`. Le lanceur doit donc garder le témoin et sauter cette réélection pour un `.vita3k`.
- `DeleteGameUseCase` efface aussi l'état de synchro des sauvegardes (caches, lignes `save_sync`, file d'attente) : la libération du zip ne passe pas par lui.
- L'identifiant n'est enregistré que pour Killzone et FEZ (RomM ne le fournit pas pour ces jeux). La libération l'écrit en base (`setTitleAndSaveIdWithLock`) en plus du témoin.

## Fichiers

### Nouveaux

- `data/emulator/Vita3kStub.kt` : nom du témoin à partir du zip, écriture (identifiant en texte), lecture.
- `data/emulator/Vita3kZipRelease.kt` (`@Singleton`) : « libérer le zip » d'un jeu. Conditions : plateforme Vita, émulateur de la famille Vita3K, identifiant connu, jeu installé (`Vita3kInstalledApps`), `localPath` = un zip existant. Étapes, dans cet ordre : écrire le témoin, enregistrer l'identifiant, pointer `localPath` sur le témoin (`updateLocalPath`, `fileOrigin` inchangé), supprimer le zip. Refus explicite dans tous les autres cas. Les DLC (`dlc/`), mises à jour et `.m3u` ne sont pas touchés.
- `data/hela/vita/HelaVitaStore.kt` : clé DataStore `hela_vita_auto_release_zip` (booléen, `false` par défaut).
- Tests : `Vita3kStubTest`, `Vita3kZipReleaseTest` (conditions de refus, ordre des étapes, aucun effet sur la synchro).

### Retouches

- `GameLauncher.kt` (déjà retouché par Hela) : `commandForVita3K` lit l'identifiant dans l'ordre fichier lancé, puis témoin, puis `game.saveId ?: game.titleId` ; `BaseRomFileResolver` n'est pas appelé quand le fichier lancé est un `.vita3k`.
- `VitaPkgHeader.titleIdResultOf` : lit aussi un `.vita3k`, pour que `TitleIdExtractor` (synchro) retrouve l'identifiant sans le zip.
- Menu du jeu : action « Libérer le zip » dans `GameSelectOverlay` (accueil Argosy et accueil Hela) et dans le menu de la bibliothèque ; visible seulement si `Vita3kZipRelease` l'autorise. Toucher et manette.
- Fin de session : après le résultat de la synchro des sauvegardes (`SessionEndCoordinator.endSession`, branche non courte), appel à `Vita3kZipRelease` si l'option est active. Une session courte annulée ne libère rien.
- `PlatformDetailSection` : interrupteur « Libérer le zip une fois le jeu installé », visible pour la PS Vita avec Vita3K, plus son routage manette (`SettingsConfirmRouter`).
- `SettingsBackupKeysTest` : classer la nouvelle clé.
- Chaînes `strings_hela.xml` (en, fr).

## Étapes, chacune vérifiée sur la Thor

1. **Témoin et lanceur** : `Vita3kStub`, lecture de l'identifiant par le lanceur et par `TitleIdExtractor`, saut de la réélection. Preuve : un témoin écrit à la main pour Killzone (zip déplacé de côté, pas supprimé), lancement depuis Hela, puis zip remis.
2. **Action manuelle** : `Vita3kZipRelease` et l'entrée du menu. Preuve : Killzone libéré depuis le menu, le jeu reste « Jouer » sur l'accueil, la bibliothèque et la fiche, après un redémarrage de Hela et une synchro RomM ; lancement depuis Hela.
3. **Option automatique** : réglage et fin de session. Preuve : option activée, une session de plus de 30 s sur Uncharted libère son zip ; une session courte ne libère rien ; option désactivée, rien ne se passe.

## Vérifications avant de dire « fait »

- `./gradlew assembleDebug` et `./gradlew testDebugUnitTest`.
- Sauvegardes (zone fragile), sur Killzone après libération : `GET /api/saves?rom_id=500` montre le même canal, la négociation avant lancement répond `NO_OP` deux fois, une fin de session avec écriture envoie bien la sauvegarde.
- Le jeu libéré ne repasse pas en « Télécharger » après : redémarrage de Hela, ouverture de la fiche, « Analyser les fichiers » de la plateforme, synchro RomM.
- Un jeu non installé (FEZ) ne propose pas « Libérer le zip ».

## Hors périmètre

- Jeux installés dans Vita3K sans zip téléchargé (Persona 4 Golden, Ys VIII…) : reconnaissance par le nom à partir des fichiers de Vita3K, chantier séparé.
- Zips de DLC et de mises à jour.
- Retéléchargement : « Supprimer le fichier local » sur un jeu libéré supprime le témoin et remet le jeu en « Télécharger », comme pour n'importe quel jeu.
