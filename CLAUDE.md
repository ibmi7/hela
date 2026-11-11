# Hela

Hela est un fork personnel d'Argosy Launcher (`rommapp/argosy-launcher`, GPL-3.0), destiné à un usage privé sur une AYN Thor (double écran, 8 Go de RAM) avec un serveur RomM auto-hébergé. Il n'est pas redistribué.

Ce fichier donne le contexte du fork. Les règles d'architecture du code sont dans `AGENTS.md` (écrit par l'équipe d'Argosy) et restent valables : les respecter limite les conflits lors des mises à jour depuis l'amont.

## Objectif

Garder l'intégration RomM d'Argosy (parcours de la bibliothèque, téléchargement, lancement, synchro des sauvegardes) et y ajouter ce qu'Argosy ne fera pas en amont :

1. L'import de thèmes communautaires au format Cocoon (`theme.json` + ressources).
2. Un nouvel écran d'accueil inspiré de Cocoon ou d'iiSU (choix entre les deux non arrêté).
3. Des corrections de lancement pour les plateformes qui posent problème (voir « Problèmes ouverts »).

## Décisions prises

- Base : Argosy, pas NeoStation (Flutter, plus lourd), ni Cocoon ou iiSU (code de l'app non publié). Détail dans `docs/hela/comparatif-bases.md`.
- Branches : `main` reste identique à l'amont ; tout le travail se fait sur `hela`. Remote `upstream` = `rommapp/argosy-launcher`.
- Pour limiter les conflits de fusion, ajouter de nouveaux fichiers et écrans à côté de l'existant plutôt que modifier en profondeur les fichiers d'Argosy. L'amont avance très vite.
- Ordre des chantiers : thèmes Cocoon d'abord (lecture du pack, fonds d'écran, couleurs, sons, musique), écran d'accueil ensuite.
- Ne pas toucher sans nécessité aux zones signalées comme fragiles dans `AGENTS.md` : synchro des sauvegardes, conformité RetroAchievements, module natif `libretrodroid`.

## Environnement de build

- Windows, dépôt dans `D:\Workspace\hela`, SDK Android en ligne de commande (pas d'Android Studio).
- JDK 17 obligatoire : Gradle 8.9 échoue avec un JDK 25 (erreur réduite à `25.0.4`).
- Compiler : `gradlew.bat assembleDebug`. Compiler et installer : `gradlew.bat installDebug`.
- La version debug s'installe sous `com.nendo.argosy.debug`, à côté de l'Argosy officiel, avec ses propres données. Ne jamais désinstaller l'officiel.
- Journaux de l'appareil : `adb logcat`.

## Problèmes ouverts

- **Vita** : au lancement depuis Argosy, l'émulateur ne s'ouvre pas ou se referme aussitôt. Dans `GameLauncher.commandForVita3K`, Argosy n'envoie à Vita3K qu'un identifiant de jeu (`-r PCSB00000`), extrait du nom du fichier (entre crochets ou en préfixe) ou de la racine du zip ; sans identifiant, il ouvre seulement l'émulateur. Hypothèse à vérifier : le jeu n'est pas installé dans Vita3K, ou l'identifiant est introuvable. Test à faire : lancer le jeu directement depuis Vita3K, puis lire `adb logcat` pendant un lancement depuis Hela.
- **Switch** : écran noir quelques secondes puis fermeture, y compris en lançant le jeu directement depuis Eden. Ce n'est donc pas un problème de frontend. Pistes côté émulateur : clés et firmware, pilote graphique, intégrité du fichier, essai avec un autre jeu.

## Documents

- `docs/hela/argosy-cartographie.md` : structure et tailles du code d'Argosy.
- `docs/hela/comparatif-bases.md` : comparaison Argosy / NeoStation / Cocoon / iiSU.
- `docs/hela/format-theme-cocoon.md` : format des thèmes Cocoon et correspondance avec Argosy.

Ces analyses datent du 11/11/2026 (Argosy 2.31.0, commit `29e350f`) et reposent sur une lecture de la structure et de la documentation, pas sur un audit du code. Les correspondances de thème sont à vérifier dans le code avant de s'y fier.

## Prochaine étape

Lire le système de thème d'Argosy (`ui/theme/`, `design-handoff/THEMING.md`, `design-system-docs/04-theming.md`, préférences associées) et proposer un plan fichier par fichier pour l'import d'un pack Cocoon, en commençant par la lecture du ZIP et les fonds d'écran haut et bas.
