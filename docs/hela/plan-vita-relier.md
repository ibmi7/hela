# Plan : relier les jeux installés dans Vita3K

Rédigé le 15/11/2026 sur la branche `hela`. Fait le même jour, les trois étapes vérifiées sur la Thor (détail dans `etat-des-travaux.md`, section Lancement Vita). Écarts avec ce plan : les lignes de la liste sont des interrupteurs (`SwitchPreference`, comme `DownloadDefaultsModal`) plutôt que des cases ; l'état de la liste vit dans `HelaVitaLinkController` et non dans l'état des réglages ; `GameDao.updateLocalPath` acceptait déjà source et origine, sans nouvelle requête. Suite de `plan-vita-sans-zip.md` : un jeu PS Vita installé dans Vita3K (`<pref-path>/ux0/app/<identifiant>`) dont le zip n'a jamais été téléchargé devient jouable depuis Hela, sans téléchargement.

## But

Pour chaque jeu installé dans Vita3K, retrouver le jeu RomM de la bibliothèque, écrire son témoin `.vita3k` et pointer `localPath` dessus. Le jeu passe en « Jouer » partout, se lance avec `-r <identifiant>`, et ses sauvegardes se synchronisent comme celles d'un jeu téléchargé.

## Décisions prises

| Question | Choix |
|---|---|
| Déclenchement | Bouton manuel « Relier les jeux installés dans Vita3K » dans Réglages > Plateformes > PlayStation Vita |
| Correspondances | Identifiant sûr : reliées directement. Par le titre seul : liste à cocher, rien n'est relié sans validation |
| Emplacement du témoin | Là où irait le zip d'un téléchargement Hela |

## Ce que le code impose

- **Chemin d'un téléchargement** (`DownloadGameUseCase` l. 108-150, `DownloadManager.getDownloadDir`) : dossier de la plateforme (`platform.customRomPath ?: <romStoragePath>/<slug>`), fichier à plat. Nom : `FileNames.sanitize(...)` de, selon le cas, l'unique fichier sélectionné, `"<fs_name>.zip"` (rom à plusieurs fichiers, cas de toute la Vita) ou le plus gros fichier. Le sous-dossier du jeu n'apparaît qu'au téléchargement d'un DLC (`resolveAddonFolder`, qui y déplace le fichier de base, témoin compris). Les zips actuels de Killzone, Uncharted et des Trails ne suivent pas ce nom : ils viennent d'un autre outil (mai 2026), comme les `.zip.m3u` et les `.psvita`, qu'aucun code de Hela n'écrit.
- Le témoin s'écrit donc à plat : `psvita/<fs_name>.vita3k` (ex. `Persona 4 Golden.vita3k`). Le canal de sauvegarde (`localPath.nameWithoutExtension`) sera le même qu'après un vrai téléchargement Hela. Les dossiers déjà présents (`Catherine_ Full Body/`, `Virtua Tennis 4_ World Tour Edition/`, `Ys VIII - Lacrimosa of Dana/`) restent intacts.
- **Suppression** : `DeleteGameUseCase` (l. 95-97) efface le dossier parent entier quand le fichier est dans un sous-dossier de la plateforme, et seulement le fichier quand il est à plat. Un témoin à plat ne peut donc rien emporter d'autre.
- **Ce qui fait un jeu « téléchargé »** : `localPath IS NOT NULL` (`INSTALLED_SQL`, `getDownloadedRommGameIds`, gardes de `SyncCoordinator`), plus `source = ROMM_SYNCED` pour les listes « jouables » (`GameDao` l. 229, 390, 1270). `GameRepository.updateLocalPath` garde la source actuelle (`ROMM_REMOTE`) : il faut écrire `ROMM_SYNCED` explicitement. `fileOrigin = ADOPTED` (fichier que Hela n'a pas téléchargé, qu'une réinitialisation complète ne supprime pas). `game_files.localPath` reste vide.
- **Passes qui effacent `localPath`** (démarrage, fiche, analyse de plateforme, synchro RomM, lancement) : aucune ne touche un témoin existant ; `BaseRomFileResolver` sort tout de suite pour un `.vita3k`. La synchro RomM recalcule `ROMM_SYNCED` dès que `localPath` existe.
- **Téléchargement en attente** : une ligne `download_queue` du jeu écraserait `localPath` à la fin. Un jeu dans la file n'est pas relié.
- **Événement de téléchargement** : `DownloadCompletionEvent` n'est pas émis (pas de téléchargement). La récupération des sauvegardes du serveur passera par la négociation avant lancement, comme pour tout jeu.
- **Sources d'identifiant**, dans l'ordre : `game.titleId` / `game.saveId` ; identifiant entre crochets dans `rommFileName` (`FIFA 15 [PCSB00604] [EUR].zip`) ; identifiant de contenu dans le nom d'un `.pkg` listé par RomM (`EP1063-PCSB01128_00-…pkg`, `game_files.fileName`). Aucune n'existe pour les `.pkg` au nom aléatoire (Persona 4 Golden, Danganronpa, Virtua Tennis 4).
- **Titres côté Vita3K** : aucun lecteur de `param.sfo` ni de `apps-cache.xml` dans le dépôt. `apps-cache.xml` est sous `Android/data` (lecture directe refusée depuis Android 11) ; `ux0/app/<id>/sce_sys/param.sfo` est sous le pref-path (carte SD sur la Thor) et toujours présent. Choix : un petit lecteur SFO (format documenté sur psdevwiki : en-tête `\0PSF`, table d'index de 16 octets, clés `TITLE` et `TITLE_ID`).
- `Vita3kInstalledApps` liste `ux0/app` avec `File.list()` : suffisant sur la Thor (pref-path sur la carte SD). Pour le chemin par défaut sous `Android/data`, le résultat est « inconnu » ; le bouton affiche alors une erreur au lieu de ne rien relier en silence.

## Correspondance

Jeux considérés : PS Vita, `rommId` connu, `localPath` vide, absents de la file de téléchargement. Identifiants considérés : dossiers de `ux0/app` (hors `_dec`), moins ceux déjà portés par un jeu téléchargé ou relié.

1. **Sûre** : un identifiant de la liste ci-dessus correspond à exactement un jeu, et ce jeu à exactement un identifiant. Reliée directement.
2. **Par le titre** : `TITLE` du `param.sfo` et titre RomM normalisés (casse, ™ ®, ponctuation, espaces) égaux, ou l'un est un préfixe de mots complets de l'autre (« Virtua Tennis 4 » / « Virtua Tennis 4: World Tour Edition »). Seulement si la paire est unique dans les deux sens ; sinon le jeu n'est pas proposé. Proposée dans la liste, non cochée par défaut.
3. Le reste n'est pas proposé (ex. un jeu installé absent de RomM).

État attendu sur la Thor : sûrs = FIFA 15, MLB 14, Ys VIII, Catherine ; par le titre = Persona 4 Golden, Danganronpa: Trigger Happy Havoc, Virtua Tennis 4.

## Fichiers

### Nouveaux

- `data/emulator/VitaParamSfo.kt` : lecture de `TITLE` et `TITLE_ID` d'un `param.sfo` (octets en entrée, pas d'accès disque).
- `data/emulator/Vita3kLinkMatcher.kt` : correspondance pure (jeux, identifiants installés, titres) → paires sûres et paires par le titre.
- `data/emulator/Vita3kLink.kt` (`@Singleton`) : `scan()` (liste `ux0/app`, lit les `param.sfo`, appelle le matcher) et `link(gameId, titleId)`. Dans l'ordre : revérifier les conditions, refuser si un fichier existe déjà au chemin du témoin, écrire le témoin, enregistrer titleId/saveId (`setTitleAndSaveIdWithLock`), écrire `localPath` + `ROMM_SYNCED` + `ADOPTED` (nouvelle méthode de DAO, sans changement de schéma).
- `ui/hela/vita/` : état et délégué du bouton et de la liste (ouvrir, déplacer le focus, cocher, valider, annuler), sur le modèle de `DownloadDefaultsModal` (coque `Modal`, interception dans `ModalInputRouter`) avec des lignes à case à cocher et des boutons Valider / Annuler comme `FilePickerModal`.
- Tests : `VitaParamSfoTest`, `Vita3kLinkMatcherTest` (les écarts de titres relevés sur la Thor, l'ambiguïté, le japonais de Catherine qui ne correspond pas par le titre mais par le `.pkg`), `Vita3kLinkTest` (chemin du témoin, ordre des écritures, refus : jeu en file, fichier déjà présent, identifiant déjà relié).

### Retouches

- `PlatformDetailSection` : item `VitaLinkInstalled` (`ActionPreference`), visible aux mêmes conditions que `VitaZipRelease` ; routage manette dans `SettingsConfirmRouter`.
- `GameDao` : une requête `linkLocalStub(id, path, source, origin)` (ou réutilisation de `updateLocalPath` s'il accepte déjà ces champs).
- `strings_hela.xml` (en, fr) : libellé et sous-titre du bouton, titre de la liste, Valider / Annuler, « N jeux reliés », « Aucun jeu à relier », « Dossier de Vita3K illisible ».

## États

- Chargement : la lecture des `param.sfo` se fait sur `Dispatchers.IO` ; le bouton reste actif, un second appui pendant la lecture est ignoré.
- Vide : « Aucun jeu à relier » (notification).
- Erreur : dossier `ux0/app` illisible ou inconnu → « Dossier de Vita3K illisible » ; rien n'est relié.
- Succès : « N jeux reliés » après les sûrs, puis la liste s'il y a des paires par le titre ; nouvelle notification après validation.

## Écrans

Les réglages ne s'affichent que sur l'écran de contrôle (comme `DownloadDefaultsModal`) : pas de version pour le second écran. Toucher et manette pour le bouton, les cases et les boutons.

## Étapes, chacune vérifiée sur la Thor

1. **Lecture et correspondance** : `VitaParamSfo`, matcher, `scan()` journalisé sans rien écrire. Preuve : le journal donne les 4 sûrs et les 3 par le titre ci-dessus.
2. **Liaison des sûrs** : `link`, bouton, notification. Preuve : FIFA, MLB, Ys VIII et Catherine passent en « Jouer » (accueil, bibliothèque, fiche) ; témoins à plat dans `psvita/`, dossiers existants intacts ; restent « Jouer » après un redémarrage de Hela, une synchro RomM de la plateforme et « Analyser les fichiers ». Lancement de Ys VIII (`-r PCSB01128`).
3. **Liste par le titre** : la liste, toucher et manette. Preuve : Persona 4 Golden et Danganronpa cochés et reliés, Virtua Tennis 4 laissé non coché puis relié dans un second passage.

## Vérifications avant de dire « fait »

- `./gradlew assembleDebug` et `./gradlew testDebugUnitTest`.
- Sauvegardes (zone fragile), sur Persona 4 Golden (rom 504) et Danganronpa (rom 489), serveur vide aujourd'hui : avant le premier lancement, `GET /api/saves?rom_id=504` vide ; le premier lancement envoie la sauvegarde locale de Vita3K ; `GET /api/saves` la montre, sous le canal `Persona 4 Golden` ; deux relances : négociation `NO_OP` deux fois.
- « Supprimer le téléchargement » sur un jeu relié : seul le témoin disparaît, le jeu repasse en « Télécharger », le dossier `psvita/` et les autres jeux sont intacts.
- Un jeu relié qui se télécharge ensuite (non prévu par le bouton, mais possible à la main après suppression) retombe sur le même nom de canal.

## Hors périmètre

- Installer un jeu dans Vita3K depuis Hela (impossible avec Vita3K Android 0.2.1, voir `etat-des-travaux.md`).
- Nettoyer les restes d'anciennes installations (`.m3u`, `.psvita`, dossiers vides).
- Lire `ux0/app` sous `Android/data` par `FileAccessLayer` (pref-path par défaut) : `Vita3kInstalledApps` reste tel quel, le bouton signale l'échec.
- Liaison automatique au démarrage ou à la synchro.
