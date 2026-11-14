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

## Accueil Hela, étapes 1 et 2

Réglages > Thème > « Accueil Hela » remplace l'accueil d'Argosy par celui de Hela sur la route `home`, donc sur l'écran qui pilote, quel qu'il soit. L'écran réutilise `HomeViewModel` : chaque page de la grille est une rangée d'Argosy (Récents, Sélection, Favoris, dans l'ordre d'Argosy), ce qui garde intactes la fiche de l'autre écran et la position à l'inversion.

- Écran : barre d'onglets (Bibliothèque, Collections, Applis, Réglages ; icônes, libellé sur l'onglet focalisé), grille de tuiles carrées (cadres du pack d'icônes Cocoon s'il y en a un), tuile large « Reprendre » en tête de Récents, tuile « Tout voir », titre de page et points.
- Manette : croix dans la grille, haut depuis la première rangée vers les onglets, gauche/droite au bord et L1/R1/L2/R2 pour changer de page ; A, B, X, Y, Start et Select restent ceux d'Argosy. Toucher : tuiles, onglets, points.
- Code : `data/hela/home/HelaHomeStore.kt` (clé `hela_home_enabled`) ; `ui/hela/home/` : `HelaHomeScreen`, `HelaHomeRoute`, `HelaHomeEntry`, `HelaHomeViewModel` (zone, onglet, colonnes, pages), `HelaHomeInputHandler` (croix, A, B, gâchettes ; le reste délégué à `HomeInputHandler`), `HelaGridLayout` (placement et déplacements, testés), `HelaHomeGrid`, `HelaHomeTiles`, `HelaHomeTabBar`, `HelaHomeOverlays`.
- Jetons : bloc `components.helaHome` dans `tokens.json` (régénérer avec `node scripts/gen-tokens.mjs` ; Node est dans `~/.local/node` sous Linux).
- Retouches d'Argosy : `NavGraph` (aiguillage), `ThemeSection`, `SettingsConfirmRouter`, `SettingsViewModel` (interrupteur), `HomeScreen` (`GameSelectOverlay` passé en `internal`), `SettingsBackupKeysTest` (clés `hela_*` classées hors sauvegarde ; le test échouait déjà sur les clés des thèmes).
- À reporter lors d'une fusion : `HelaHomeScreen` recopie l'effet de cycle de vie et le collecteur d'événements de `HomeScreen`, `HelaHomeOverlays` recopie ses fenêtres (menu du jeu, collections, disques, carte mémoire, changelog, reprise média).
- Vérifié sur la Thor : interrupteur, navigation manette et toucher, onglet Collections, changement de page, menu du jeu affiché et fermé, fiche de l'autre écran qui suit, deux inversions avec Select maintenu.
- Suppose la disposition carrousel d'Argosy (Réglages > Interface) : en grille personnalisée, `focusedGame` vient de la grille d'Argosy et la fiche ne suivrait pas.
- Reste (étape 4) : appui long au toucher pour le menu du jeu, indications de boutons, états vide et chargement ; au retour d'un onglet, le focus revient parfois sur une autre tuile.

## Ce qui reste

1. Décider si les pastilles de favori et de téléchargement reviennent sur les tuiles carrées.
2. Nouvel écran d'accueil inspiré d'iiSU : plan dans `plan-accueil-iisu.md`. Étapes 1 et 2 faites (voir plus haut), étapes 3 et 4 à faire.
3. Dossiers intelligents des packs (`smart_folders/`) : dépendent du nouvel écran d'accueil.
4. Problèmes de lancement Vita et Switch (voir `CLAUDE.md`).

## Précautions apprises

- Sur l'appareil, faire une capture avant chaque appui de validation : le navigateur de fichiers rouvre le dernier dossier, et une suite d'appuis à l'aveugle a déjà supprimé un pack.
- Dans Git Bash, `export MSYS_NO_PATHCONV=1` avant toute commande `adb` avec un chemin `/sdcard`.
- Le wiki Cocoon dit que `main` est l'écran du bas ; sur la Thor, les dimensions des packs montrent l'inverse (`main` = 1920×1080 = écran du haut).
