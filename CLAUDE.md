# Hela

Hela est un fork personnel d'Argosy Launcher (`rommapp/argosy-launcher`, GPL-3.0), destiné à un usage privé sur une AYN Thor (double écran, 8 Go de RAM) avec un serveur RomM auto-hébergé. Il n'est pas redistribué.

Ce fichier donne le contexte du fork. Les règles d'architecture du code sont dans `AGENTS.md` (écrit par l'équipe d'Argosy) et restent valables : les respecter limite les conflits lors des mises à jour depuis l'amont.

## Objectif

Garder l'intégration RomM d'Argosy (parcours de la bibliothèque, téléchargement, lancement, synchro des sauvegardes) et y ajouter ce qu'Argosy ne fera pas en amont :

1. L'import de thèmes communautaires au format Cocoon (`theme.json` + ressources).
2. Un nouvel écran d'accueil inspiré d'iiSU (plan dans `docs/hela/plan-accueil-iisu.md`).
3. Des corrections de lancement pour les plateformes qui posent problème (voir « Problèmes ouverts »).

## Décisions prises

- Base : Argosy, pas NeoStation (Flutter, plus lourd), ni Cocoon ou iiSU (code de l'app non publié). Détail dans `docs/hela/comparatif-bases.md`.
- Branches : `main` reste identique à l'amont ; tout le travail se fait sur `hela`. Remote `upstream` = `rommapp/argosy-launcher`.
- Pour limiter les conflits de fusion, ajouter de nouveaux fichiers et écrans à côté de l'existant plutôt que modifier en profondeur les fichiers d'Argosy. L'amont avance très vite.
- Ordre des chantiers : thèmes Cocoon d'abord (lecture du pack, fonds d'écran, couleurs, sons, musique), écran d'accueil ensuite.
- Ne pas toucher sans nécessité aux zones signalées comme fragiles dans `AGENTS.md` : synchro des sauvegardes, conformité RetroAchievements, module natif `libretrodroid`.

## Environnement de build

Deux machines, SDK Android en ligne de commande dans les deux cas (Android Studio n'est pas nécessaire).

- JDK 21, comme la CI d'Argosy (`.github/workflows/build.yml`). Le JDK 17 compile mais ne fait pas tourner les tests netplay (lazysodium exige Java 21). Gradle 8.9 échoue avec un JDK 25 (erreur réduite à `25.0.4`).
- Composants requis : plateforme `android-35`, `build-tools;34.0.0` et `35.0.0`, `platform-tools`, `ndk;27.0.12077973` et `cmake;3.22.1` (modules natifs `libretrodroid` et `sigil`).
- Sous-modules git obligatoires (`sigil`, `rcheevos`, `libadrenotools`) : `git submodule update --init --recursive` après un clone, sinon le build échoue.

### Windows

- Dépôt dans `D:\Workspace\hela`.
- Compiler : `gradlew.bat assembleDebug`. Compiler et installer : `gradlew.bat installDebug`.

### Linux (Fedora)

- Dépôt dans `~/Workspace/Hela`. Installer Temurin 21 à la main dans `~/.local/jdk-21` (archive depuis `api.adoptium.net`). Le JDK système (25) reste en place ; un Temurin 17 traîne aussi dans `~/.local/jdk-17`.
- SDK dans `~/Android/Sdk` : extraire les command-line tools de Google dans `~/Android/Sdk/cmdline-tools/latest`, puis :
  ```
  export JAVA_HOME=~/.local/jdk-21 ANDROID_HOME=~/Android/Sdk
  yes | sdkmanager --licenses
  sdkmanager "platform-tools" "platforms;android-35" "build-tools;34.0.0" "build-tools;35.0.0" "ndk;27.0.12077973" "cmake;3.22.1"
  ```
- `local.properties` (ignoré par git) : `sdk.dir=/home/brams/Android/Sdk`.
- `~/.bashrc` exporte `JAVA_HOME`, `ANDROID_HOME` et ajoute `platform-tools` et `cmdline-tools/latest/bin` au `PATH`.
- Compiler : `./gradlew assembleDebug`. Compiler et installer : `./gradlew installDebug`. Tests : `./gradlew testDebugUnitTest` (`assembleDebug` ne compile pas les tests).
- La clé de debug `~/.android/debug.keystore` est celle du PC Windows (empreinte SHA-256 `94:36:99:CF:…:D7:22`) : sans elle, `installDebug` échoue sur la Thor avec `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

### Commun
- La version debug s'installe sous `com.nendo.argosy.debug`, à côté de l'Argosy officiel, avec ses propres données. Ne jamais désinstaller l'officiel.
- Journaux de l'appareil : `adb logcat`.

## Problèmes ouverts

- **Vita** : corrigé côté Hela (voir `docs/hela/etat-des-travaux.md`). Les zips No-Intro (un `.pkg` au nom aléatoire et `work.bin`) ne portaient pas d'identifiant visible ; Hela le lit maintenant dans l'en-tête du `.pkg` (`VitaPkgHeader`) et l'envoie à Vita3K. Sans identifiant, il ouvre la liste de jeux de Vita3K au lieu de l'activité `Emulator`, qui se ferme à vide depuis Vita3K 0.2.1. Les sauvegardes Vita se synchronisent (envoi et négociation vérifiés sur Killzone) à condition de régler le chemin de sauvegarde de la plateforme sur `<pref-path de Vita3K>/ux0/user/00/savedata` ; restauration non vérifiée. Restent hors de Hela : Trails of Cold Steel I et II plantent dans Vita3K 0.2.1 quelques secondes après le démarrage (`std::terminate()`, même plantage lancé par `adb`) ; un jeu doit être installé dans Vita3K (`ux0/app`), Hela ne passe jamais le fichier.
- **Switch** : écran noir quelques secondes puis fermeture, y compris en lançant le jeu directement depuis Eden. Ce n'est donc pas un problème de frontend. Pistes côté émulateur : clés et firmware, pilote graphique, intégrité du fichier, essai avec un autre jeu.

## Documents

- `docs/hela/etat-des-travaux.md` : ce qui est fait, ce qui reste, comment reprendre sur une autre machine.
- `docs/hela/argosy-cartographie.md` : structure et tailles du code d'Argosy.
- `docs/hela/comparatif-bases.md` : comparaison Argosy / NeoStation / Cocoon / iiSU.
- `docs/hela/format-theme-cocoon.md` : format des thèmes Cocoon et correspondance avec Argosy.
- `docs/hela/plan-accueil-iisu.md` : plan de l'accueil inspiré d'iiSU (référence visuelle, décisions, fichiers, étapes).

Ces analyses datent du 11/11/2026 (Argosy 2.31.0, commit `29e350f`) et reposent sur une lecture de la structure et de la documentation, pas sur un audit du code. Les correspondances de thème sont à vérifier dans le code avant de s'y fier.

## Prochaine étape

L'accueil inspiré d'iiSU est fait (les quatre étapes de `docs/hela/plan-accueil-iisu.md`, vérifiées sur la Thor) ; détail dans `docs/hela/etat-des-travaux.md`. Les dossiers intelligents des packs Cocoon alimentent une page « Consoles » de cet accueil. Pistes suivantes, à choisir : grille modifiable, dossiers Plus joués et Nouveautés, problème de lancement Switch, lecture automatique du `pref-path` de Vita3K.
