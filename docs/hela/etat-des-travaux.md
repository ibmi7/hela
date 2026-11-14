# État des travaux Hela

État au 14/11/2026, branche `hela`, au-dessus d'Argosy 2.31.0 (`29e350f7b`).

## Reprendre sur un autre ordinateur

1. `git clone https://github.com/ibmi7/hela.git`, puis `git checkout hela`.
2. Récupérer les sous-modules, sans lesquels le build échoue : `git submodule update --init --recursive`.
3. Ajouter l'amont, absent d'un clone neuf : `git remote add upstream https://github.com/rommapp/argosy-launcher.git`.
4. Installer un JDK 21 (Temurin), le SDK Android, le NDK et CMake : versions et commandes Windows/Linux dans la section « Environnement de build » de `CLAUDE.md`.
5. Compiler et installer : `gradlew.bat installDebug` sous Windows, `./gradlew installDebug` sous Linux (paquet `com.nendo.argosy.debug`).
6. Tests du chantier thèmes : `testDebugUnitTest --tests "com.nendo.argosy.data.hela.theme.*"` avec le même lanceur (64 tests).

Ce qui ne suit pas le dépôt :

- Les packs ZIP de `docs/hela/` sont ignorés par git (`docs/hela/*.zip`). Ils sont aussi sur la Thor dans `/sdcard/Download/hela-packs`, et se retéléchargent depuis Silk.
- Les données de l'application (packs importés, sélection des emplacements) sont sur la Thor, pas dans le dépôt.
- La mémoire et l'historique de conversation de Claude Code sont propres à chaque machine ; ce fichier et `CLAUDE.md` les remplacent.

## Ce qui est fait : thèmes Cocoon

Réglages > Thème > « Packs Cocoon » ouvre l'écran d'import. Un ZIP de thème ou de pack d'assets Silk s'importe par le navigateur de fichiers interne. Six emplacements se choisissent séparément, comme dans Cocoon ; « Tout appliquer » remplit ceux que le pack fournit, « Retirer le thème Cocoon » rend tout aux réglages d'Argosy, qui ne sont jamais écrasés.

| Emplacement | Effet | Commit |
|---|---|---|
| Fond de l'écran du haut (`main`) et du bas (`external`) | Image ou vidéo en boucle sur l'accueil, la bibliothèque et le second écran ; interrupteur d'animation | `d7aae2251` |
| Couleurs | `theme_mode` force sombre ou clair ; accent et couleur secondaire tirés de `color_scheme` | `fa3d0f0d5` |
| Sons | 11 des 22 sons Cocoon remplacent les sons d'interface ; un son réglé par l'utilisateur reste prioritaire | `bc1751fe6` |
| Musique | Les pistes du pack remplacent la liste de lecture du launcher, dans l'ordre de `music_playlist` | `4a52b01a5` |
| Icônes | Tuiles de jeux carrées sur l'accueil et la bibliothèque : jaquette découpée par `mask.png`, `overlay.png` par-dessus, par plateforme avec repli sur `global` | `7f730084b` |

La suppression d'un pack demande une confirmation. Le détail du format et la table de correspondance sont dans `format-theme-cocoon.md`.

### Où est le code

Tout est sous `app/src/main/kotlin/com/nendo/argosy/`.

- `data/model/hela/CocoonThemePack.kt` : modèles (pack, emplacements, sélection).
- `data/hela/theme/` : `CocoonPackImporter` (ZIP), `CocoonThemeParser`, `CocoonWallpaperResolver`, `CocoonColors`, `CocoonSounds`, `CocoonMusic`, `CocoonIcons`, et `HelaThemeStore` (singleton Hilt, packs dans `filesDir/hela/themes`, sélection dans DataStore sous les clés `hela_*`).
- `ui/hela/theme/` : `HelaThemesScreen`, `HelaThemesViewModel`, `HelaWallpaperLayer`, `HelaIcons`.
- Chaînes : `res/values/strings_hela.xml` et `values-fr/strings_hela.xml` (anglais et français seulement).
- Tests : `app/src/test/kotlin/com/nendo/argosy/data/hela/theme/`.

Fichiers d'Argosy retouchés, chaque fois par quelques lignes, à surveiller lors d'une fusion depuis l'amont : `HomeScreen`, `LibraryScreen`, `PresentationSlotContent`, `GameShowcase`, `ThemeSection`, `SettingsConfirmRouter`, `SettingsViewModel`, `SettingsScreen`, `Theme.kt`, `SoundFeedback`, `BgmPlaylistCoordinator`, `GameCard`, `NavGraph`.

### Vérifié sur la Thor

Import de packs réels (iiSU, 3DS Dark Reimagined, OLED Observatory, 3DS Sound Effects, 3DS Background Music, icônes iiSU), fonds sur les deux écrans, mode et accent, lecture de la musique, tuiles carrées avec cadre par plateforme.

### Non vérifié ou non fait

- Pause de la vidéo de fond pendant un jeu ; consommation de batterie.
- Sons et musique non écoutés à l'oreille (seuls les journaux et l'état du lecteur ont été contrôlés).
- `sfx_volume`, `music_volume`, `music_time_schedule` et le mode de lecture ne sont pas appliqués.
- De `color_scheme`, seuls le mode et l'accent sont repris ; fond, cartes et texte restent ceux d'Argosy.
- Avec un pack d'icônes actif, les pastilles de favori et de jeu téléchargé ne sont plus dessinées sur les tuiles.
- Icônes : pack à cadre `global` seul, dispositions en grille de l'accueil, second écran et retrait du pack non contrôlés.
- Lint non lancé ; chaînes non traduites hors anglais et français.

### État laissé sur la Thor

Thème iiSU pour les fonds et les couleurs, 3DS Sound Effects, 3DS Background Music, cadres d'icônes iiSU. Les sons d'interface et la musique de fond sont désactivés dans les réglages d'Argosy.

## Accueil Hela (étapes 1 à 4 du plan)

Réglages > Thème > « Accueil Hela » remplace l'accueil d'Argosy par celui de Hela sur la route `home`, donc sur l'écran qui pilote, quel qu'il soit. L'écran réutilise `HomeViewModel` : chaque page de la grille est une rangée d'Argosy (Récents, Sélection, Favoris, dans l'ordre d'Argosy), ce qui garde intactes la fiche de l'autre écran et la position à l'inversion.

- Écran : barre d'onglets (Bibliothèque, Collections, Applis, Réglages ; icônes, libellé sur l'onglet focalisé), grille de tuiles carrées (cadres du pack d'icônes Cocoon s'il y en a un), tuile large « Reprendre » en tête de Récents, tuile « Tout voir », titre de page et points.
- Finitions (étape 4) : appui long au toucher pour le menu du jeu ; indications de boutons (L1/R1 pages, A, Y favori, X détails), relayées sur l'autre écran quand il les affiche ; écran de démarrage d'Argosy pendant le chargement ; état vide d'Argosy (avec synchronisation par A) quand Récents, Sélection et Favoris sont tous vides.
- Manette : croix dans la grille, haut depuis la première rangée vers les onglets, gauche/droite au bord et L1/R1/L2/R2 pour changer de page ; A, B, X, Y, Start et Select restent ceux d'Argosy. Toucher : tuiles, onglets, points.
- Code : `data/hela/home/HelaHomeStore.kt` (clé `hela_home_enabled`) ; `ui/hela/home/` : `HelaHomeScreen`, `HelaHomeRoute`, `HelaHomeEntry`, `HelaHomeViewModel` (zone, onglet, colonnes, pages), `HelaHomeInputHandler` (croix, A, B, gâchettes ; le reste délégué à `HomeInputHandler`), `HelaGridLayout` (placement et déplacements, testés), `HelaHomeGrid`, `HelaHomeTiles`, `HelaHomeTabBar`, `HelaHomeOverlays`, `HelaHomeFooter`, `HelaGameDetail`.
- Fiche de l'autre écran (étape 3, `HelaGameDetail`) : quand l'accueil Hela est actif et que le jeu focalisé vient de l'accueil, `PresentationSlotContent` affiche le fond du jeu en plein écran sous un voile, le logo au centre (le titre en gras, ombré, quand RomM n'a pas de logo), la jaquette en carte empilée en bas à gauche et la pastille de succès RA en bas à droite. La bibliothèque garde sa vitrine de plateforme d'Argosy.
- Jetons : bloc `components.helaHome` dans `tokens.json` (régénérer avec `node scripts/gen-tokens.mjs` ; Node est dans `~/.local/node` sous Linux).
- Retouches d'Argosy : `NavGraph` (aiguillage), `PresentationSlotContent` (fiche Hela dans le slot `Detail`), `ThemeSection`, `SettingsConfirmRouter`, `SettingsViewModel` (interrupteur), `HomeScreen` (`GameSelectOverlay`, `SplashOverlay` et `EmptyState` passés en `internal`), `SettingsBackupKeysTest` (clés `hela_*` classées hors sauvegarde ; le test échouait déjà sur les clés des thèmes).
- À reporter lors d'une fusion : `HelaHomeScreen` recopie l'effet de cycle de vie et le collecteur d'événements de `HomeScreen`, `HelaHomeOverlays` recopie ses fenêtres (menu du jeu, collections, disques, carte mémoire, changelog, reprise média).
- Vérifié sur la Thor : interrupteur, navigation manette et toucher, onglet Collections, changement de page, menu du jeu affiché et fermé, fiche de l'autre écran qui suit, deux inversions avec Select maintenu.
- Suppose la disposition carrousel d'Argosy (Réglages > Interface) : en grille personnalisée, `focusedGame` vient de la grille d'Argosy et la fiche ne suivrait pas.
- Vérifié aussi : pastille RA (Crisis Core mis en favori), fiche sur l'écran du bas après inversion, appui long, indications relayées sur l'autre écran.
- Non vérifié : l'état vide (bibliothèque jamais vide sur la Thor), les sons à l'oreille, le mode un seul écran (indications en bas du même écran).

## Dossiers intelligents des packs Cocoon

Un pack `smart_folders` (asset Silk) contient `smart_folders/by_platform/<plateforme>/` et des dossiers spéciaux (`favorites`, `recent`, parfois `most_played`, `newly_added`, `android_games`), chacun avec `icon` (1024×1024), `logo` et `hero` (1920×1080, souvent une bande sur fond transparent), en PNG, WebP, JPG ou GIF. Le pack ne fournit que des visuels.

- Import : même écran « Packs Cocoon » ; nouvel emplacement « dossiers » (« Utiliser ses dossiers », clé `hela_folders_pack`), indépendant des autres. `CocoonSmartFolders` lit le pack ; les noms de plateforme passent par `CocoonIcons.cocoonNames` (psvita, n3ds…).
- Accueil Hela : page « Consoles » après les pages de jeux (L1/R1, points). Tuiles Favoris et Récents (elles sautent à ces pages), puis une tuile par plateforme de la bibliothèque, qui ouvre la bibliothèque d'Argosy sur cette console. Sans pack, ou pour une plateforme absente du pack, la tuile affiche le nom. B revient aux pages de jeux ; Y et X ne font rien sur cette page.
- Autre écran : `HelaGameDetail` affiche la bannière `hero` du dossier sur le fond d'écran Cocoon, et son `logo` au centre (le nom à défaut). Le dossier est publié par `HelaHomeViewModel` sous un propriétaire à lui (`setCompanionDetail`), la fiche de jeu de `HomeViewModel` étant retirée pendant ce temps ; marqueur `platformSlug = "hela:folder"`.
- La position sur la page Consoles est gardée en mémoire dans `HelaHomeStore` : Argosy recrée l'accueil au retour de la bibliothèque, et chaque écran a son propre ViewModel.
- Code : `data/hela/theme/CocoonSmartFolders.kt`, `PackSmartFolders` et `PackFolderArt` dans `CocoonThemePack.kt`, `ui/hela/home/HelaHomeFolders.kt`, plus des ajouts dans `HelaHomeViewModel`, `HelaHomeInputHandler`, `HelaHomeScreen`, `HelaHomeGrid`, `HelaHomeFooter`, `HelaGameDetail`, `HelaThemeStore`, `HelaThemesViewModel`, `HelaThemesScreen`, `CocoonPackImporter`. Retouche d'Argosy : une condition dans `PresentationSlotContent`. Tests : `CocoonSmartFoldersTest`.
- Vérifié sur la Thor avec « Iisu interpreted - Smart Folders » (233 Mo, importé en moins de 30 s) : import, choix de l'emplacement, page Consoles, bannière et logo sur l'autre écran, ouverture d'une console, retour sur la page Consoles, tuile Favoris, retour de la fiche de jeu sur les pages de jeux.
- Non vérifié : inversion des écrans sur la page Consoles, toucher sur une tuile de console, les sept autres packs (dont les GIF animés de Pixel UI et les fichiers parasites de Cocoonix), retrait du pack. Non fait : dossiers Plus joués, Nouveautés et Jeux Android ; visuels des dossiers dans la bibliothèque.

## Lancement Vita

- Cause : Vita3K 0.2.1 n'accepte plus l'activité `org.vita3k.emulator.Emulator` sans identifiant de jeu (`No title ID provided`, fermeture en deux secondes), et les zips No-Intro de la bibliothèque (`work.bin` + un `.pkg` au nom aléatoire) n'exposaient l'identifiant ni dans leur nom ni dans leurs entrées.
- Correctif : `data/emulator/VitaPkgHeader.kt` lit les 84 premiers octets du `.pkg` du zip (signature `7F 50 4B 47`, identifiant de contenu ASCII à 0x30, title ID aux caractères 7 à 15) ; `GameLauncher.extractTitleIdFromZip` s'en sert quand aucune entrée ne commence par un identifiant. Sans identifiant, `commandForVita3K` ouvre l'activité de lancement du paquet (`getLaunchIntentForPackage`, `MainActivity` pour Vita3K), drapeaux `NEW_TASK | CLEAR_TASK`. Tests : `VitaPkgHeaderTest`.
- Vérifié sur la Thor depuis Hela : Killzone (`PCSF00243`) et Uncharted (`PCSF00012`) démarrent ; lecture de l'en-tête en moins de 15 ms, y compris sur les zips de 2,8 Go des Trails. Repli sans identifiant vérifié par `adb` avec l'intent exact (aucun jeu de la bibliothèque n'y passe) : liste des jeux de Vita3K, qui reste ouverte.
- Hors Hela : Trails of Cold Steel (`PCSB00866`) et II (`PCSB01016`) reçoivent le bon identifiant et démarrent, puis Vita3K plante (`std::terminate()`, SIGABRT dans « BGM Update Thread » pour le premier) ; même plantage avec un lancement par `adb`, donc compatibilité de Vita3K.
- Sauvegardes Vita : deux causes. (1) Argosy ne connaît que `Android/data/org.vita3k.emulator/files/VITA/ux0/user/00/savedata`, alors que Vita3K écrit ici sous `/storage/4A21-0000/Vita3k/` (`pref-path` de son `config.yml`) : réglé à la main dans Réglages > Plateformes > PlayStation Vita > Chemin de sauvegarde, sur le dossier exact `.../Vita3k/ux0/user/00/savedata` (le dossier `Vita3k` ou `ux0` ne conviendrait pas). (2) `sigil` ne lit pas l'identifiant des zips No-Intro ; `TitleIdExtractor` se rabat sur `VitaPkgHeader.titleIdResultOf` quand `sigil` renvoie `null` (Vita, `.zip`), ce qui sert toutes les étapes de la synchro.
- Preuve sur données réelles (RomM 5.4.1, donc gestionnaire de dossiers et négociation, pas Sigil), Killzone, rom 500 : serveur vide avant ; fin de session de 50 s : `Result=UPLOADED`, `GET /api/saves?rom_id=500` montre la sauvegarde 9 (`vita3k`, `autosave`, hash identique au local) ; deux relances : négociation `NO_OP, Content is identical` deux fois. Une session de moins de 30 s sans écriture n'est pas archivée (comportement d'Argosy).
- Non vérifié : restauration depuis le serveur, conflit, les autres jeux Vita, la route Sigil (RomM 5.5+). Non fait : lecture automatique de `pref-path` (le réglage manuel est à refaire sur une autre installation).
- Non traité : FEZ (`[PCSB00456]` dans le nom) n'est pas installé dans Vita3K.

## Ce qui reste

1. Décider si les pastilles de favori et de téléchargement reviennent sur les tuiles carrées.
2. Nouvel écran d'accueil inspiré d'iiSU : plan dans `plan-accueil-iisu.md`. Les quatre étapes du plan sont faites (voir plus haut). Suite : grille modifiable, liste par console, dossiers intelligents.
3. Dossiers intelligents des packs : faits pour les consoles, Favoris et Récents (voir plus haut) ; restent Plus joués, Nouveautés, Jeux Android.
4. Problème de lancement Switch (voir `CLAUDE.md`) ; Vita corrigé côté Hela, sauvegardes comprises (restauration à vérifier).

## Précautions apprises

- Sur l'appareil, faire une capture avant chaque appui de validation : le navigateur de fichiers rouvre le dernier dossier, et une suite d'appuis à l'aveugle a déjà supprimé un pack.
- Dans Git Bash, `export MSYS_NO_PATHCONV=1` avant toute commande `adb` avec un chemin `/sdcard`.
- Le wiki Cocoon dit que `main` est l'écran du bas ; sur la Thor, les dimensions des packs montrent l'inverse (`main` = 1920×1080 = écran du haut).
