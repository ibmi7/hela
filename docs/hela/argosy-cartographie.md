# Cartographie d'Argosy Launcher (base de Hela)

Relevé du 11/11/2026 sur `rommapp/argosy-launcher`, commit `29e350f` (version 2.31.0). Lecture de la structure et de la doc interne, pas d'audit du code.

## Chiffres

- Kotlin + Jetpack Compose, GPL-3.0. Environ 390 000 lignes dans `app/`.
- Couches `ui/ -> domain/ -> data/`, dépendances vers l'intérieur uniquement.
  - `ui/` : ~206 000 lignes (dont `screens/settings` 60 000, `gamedetail` 16 600, `home` 9 900, `library` 5 500, `components` 36 800, `dualscreen` 5 000).
  - `data/` : ~126 000 lignes (`remote`, `sync`, `download`, `emulator`, `repository`, `local`).
  - `libretro/` : ~40 000 lignes (cœurs intégrés), plus un module natif `libretrodroid`.
- Dans `ui/`, environ 59 000 lignes sont de la logique (ViewModels, delegates, routers, états) et le reste du rendu Compose.

## Points structurants

- **Bibliothèque** : RomM est synchronisé dans une base locale (Room) ; l'interface lit cette base via des repositories. Le hors-ligne en découle.
- **Lancement** : un seul résolveur (`EmulatorResolver`) et un seul lanceur (`GameLauncher`). Switch pris en charge via Yuzu, Eden, Eden Duo et dérivés. Le lanceur contient des appels à `killBackgroundProcesses` ; leurs conditions de déclenchement n'ont pas été vérifiées.
- **Navigation** : modèle d'entrée maison (`InputDispatcher` + `InputHandler` par écran, index de focus dans le ViewModel), sans le focus Compose. Tout nouvel écran doit s'y brancher.
- **Double écran** : `DualScreenManager` comme source unique d'état.
- **Thèmes** : décision écrite des mainteneurs (`design-handoff/THEMING.md`) : pas de moteur de layouts communautaires. Les thèmes sont des packs (couleurs, fonds, sons, polices) ; les variantes de disposition restent faites main par l'équipe.

## Conséquences pour Hela

- L'intégration RomM, le téléchargement et le lancement sont réutilisables tels quels.
- L'écart avec le besoin est l'interface, et surtout les thèmes communautaires de type Cocoon / iiSU, qu'Argosy exclut par choix.
- Surface minimale à refaire pour changer l'apparence : accueil, bibliothèque, fiche de jeu (~32 000 lignes, logique comprise).
- Risque principal : rythme de l'amont très élevé ; ajouter des écrans à côté plutôt que modifier les fichiers existants pour limiter les conflits de fusion.
