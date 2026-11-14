# Plan : accueil Hela inspiré d'iiSU

Rédigé le 14/11/2026 sur la branche `hela` (au-dessus d'Argosy 2.31.0). Rien n'est encore codé.

## Référence visuelle

Aucune capture fournie ; la référence vient de captures publiées dans la presse (Retro Handhelds, SteamDeckHQ, novembre 2025), dont le mode double écran d'iiSU sur la Thor :

- Écran interactif (bas) : barre d'onglets en pastilles, puis grille de tuiles carrées, une tuile large « Jump back in! » pour le dernier jeu, pagination par points.
- Écran de détail (haut) : illustration du jeu en plein écran, grand logo centré, carte « Media » en bas à gauche, pastille de succès RA en bas à droite, heure et batterie en haut, indications de boutons.

Code d'iiSU fermé : il s'agit d'une réécriture inspirée de l'agencement, sans reprise d'éléments graphiques.

## Décisions prises

| Question | Choix |
|---|---|
| Cohabitation | Un réglage choisit l'accueil Argosy ou l'accueil Hela |
| Écran actif | Comme Argosy : l'écran du bas pilote au démarrage, Select inverse les rôles |
| Premier niveau | Les jeux d'abord |
| Périmètre v1 | Grille interactive et écran de détail façon iiSU |
| Grille | Rangement automatique, rien de stocké |
| Onglets | Bibliothèque, Collections, Applis Android, Réglages |

## Ce que le code impose

- Les deux activités affichent le même `ArgosyApp`. L'écran qui pilote montre la route `home`, l'autre affiche `PresentationSlotContent`. Il n'existe plus d'accueil séparé pour l'écran secondaire (`coupling-map.md` est périmé sur ce point). Un seul aiguillage dans `NavGraph.kt` couvre donc les deux écrans et l'inversion.
- `HomeViewModel` porte le chargement (récents, favoris, recommandés, plateformes), les actions (lancer, télécharger, favori, menu du jeu), la publication du jeu sélectionné vers l'autre écran (`publishCompanionDetail`, déclenchée par `focusedGame`) et la conservation de la position à l'inversion (`HomeFirstFrameCache`). L'accueil Hela le réutilise tel quel ; le dupliquer ferait diverger ~2 400 lignes à chaque fusion.
- `focusedGame` se déduit de `currentRow` + `focusedGameIndex`. Pour que la publication et la conservation de position marchent sans toucher au ViewModel, chaque page de la grille Hela correspond à une rangée d'Argosy : page 1 « Reprendre » (`HomeRow.Continue`, première tuile en large), page 2 Favoris, page 3 Recommandés. Naviguer dans une page appelle `setFocusIndex`, changer de page appelle `selectRow` / `nextRow` / `previousRow`.
- `CompanionDetail` contient déjà le fond (`backdropUrl`), le logo (`logoUrl`, slot `LOGO` venant de RomM/ScreenScraper), la jaquette et les compteurs RA (`stats.achievementCount` / `earnedAchievementCount`). Les captures d'écran n'y sont pas (`GameScreenshotEntity`, accessible seulement par DAO).
- L'heure et la batterie sont déjà superposées à l'écran de présentation (`SystemStatusBar`, `PresentationSlotContent.kt:136`). Pas de date.
- Les réglages Hela contournent la chaîne de réglages d'Argosy : clés `hela_*` dans `HelaThemeStore`, lues par `rememberHelaThemeStore()`. On suit ce précédent.

## Fichiers

### Nouveaux, sous `ui/hela/home/`

- `HelaHomeScreen.kt` : composable de la route `home` quand le réglage est actif. Reprend l'effet de cycle de vie de `HomeScreen` (abonnement de l'`InputHandler` à `ROUTE_HOME`, `onResume`, `republishCompanionDetail`, `onHomeShown` / `clearCompanionDetail` / `onHomeHidden`), le collecteur `viewModel.events`, les fenêtres (menu du jeu, choix de disque, carte mémoire, changelog), le fond Cocoon, les indications de pied de page, les états chargement / vide.
- `HelaHomeViewModel.kt` : petit ViewModel qui possède l'état propre à Hela (zone focalisée : onglets ou grille, index d'onglet). La loi « index de focus dans un ViewModel » est respectée sans toucher à `HomeViewModel`.
- `HelaHomeTabBar.kt` : barre de pastilles (Bibliothèque, Collections, Applis, Réglages), tactile et manette.
- `HelaHomeGrid.kt` : grille paginée de tuiles carrées, tuile large pour le premier jeu de « Reprendre », points de pagination. Tuiles : `HelaIconTile` si un pack d'icônes est actif, sinon carré arrondi Hela (indépendant de `LocalBoxArtStyle`).
- `HelaGridLayout.kt` : fonctions pures (placement des tuiles selon la largeur, déplacement à la croix : tuile voisine, page précédente/suivante, remontée vers les onglets). Testables sans Android.
- `HelaHomeInputHandler.kt` : gère croix, A et L/R (onglets + grille) et délègue le reste à `HomeInputHandler` (fenêtres, B, Start, Select pour l'inversion, Y favori, X menu).
- `HelaGameDetail.kt` : écran de détail façon iiSU (fond plein écran, logo centré, carte Médias, pastille RA), réutilise `rememberResolvedArt` / `withLiveArt` pour que le logo arrive une fois mis en cache.

### Retouches d'Argosy (quelques lignes chacune)

- `NavGraph.kt:76` : `if (accueilHela) HelaHomeScreen(...) else HomeScreen(...)`, mêmes lambdas, route inchangée.
- `PresentationSlotContent.kt:208` : dans le slot `Detail`, `HelaGameDetail` si `isGameTitle` et accueil Hela actif.
- `ThemeSection.kt` + `SettingsConfirmRouter.kt` : interrupteur « Accueil Hela », sur le modèle de l'entrée « Packs Cocoon ».
- `HomeScreen.kt` : passer en `internal` les composables privés réutilisés (états vide/chargement, fenêtres), sans les déplacer.
- `GameShowcase.kt` : rendre `withLiveArt` accessible.
- `HelaThemeStore.kt` : clé `hela_home` + flux + setter (précédent : `ANIMATE`).
- `design-system-docs/tokens.json` + régénération : bloc `components.helaHome` (taille de tuile, rayon, barre d'onglets).
- `strings_hela.xml` (en et fr).

À ajouter à la liste « Fichiers d'Argosy retouchés » de `etat-des-travaux.md`. Point de vigilance à chaque fusion : l'effet de cycle de vie et les fenêtres de `HomeScreen`, recopiés dans `HelaHomeScreen`.

## Étapes, chacune compilée et vérifiée sur la Thor

1. **Aiguillage** : réglage, `HelaHomeScreen` minimal (liste brute des jeux de `currentItems` + effet de cycle de vie), branchement `NavGraph`. Preuve : le réglage bascule l'accueil, l'autre écran suit la sélection, Select inverse deux fois sans perdre la manette.
2. **Grille et onglets** : `HelaGridLayout` (avec tests), grille paginée, barre d'onglets, `HelaHomeInputHandler`, tactile. Preuve : tout atteignable à la manette et au toucher, lancement d'un jeu, menu du jeu, favori.
3. **Écran de détail** : `HelaGameDetail` et branchement `PresentationSlotContent`. Preuve : captures des deux écrans avant/après inversion, logo et compteur RA affichés.
4. **Finitions** : jetons, états vide et chargement, sons (défauts de l'`InputDispatcher`), indications de pied de page, chaînes fr/en, mise à jour de `etat-des-travaux.md`.

## Vérifications avant de dire « fait »

- `./gradlew assembleDebug` et `./gradlew testDebugUnitTest` (le premier ne compile pas les tests).
- Sur la Thor : captures des deux écrans, navigation manette et tactile, inversion répétée, réglage désactivé = accueil Argosy intact.
- Aucune dimension ni couleur en dur.

## Hors v1

- Grille modifiable (Ⓨ Modifier, tailles libres) ; `CustomGridSurface` d'Argosy sait déjà faire des pages et des tuiles multi-cases, à étudier pour une v2.
- Liste verticale par console, dossiers intelligents des packs (`smart_folders/`).
- Pile de captures dans la carte Médias : demande un accès aux captures par un dépôt (l'interface ne lit pas les DAO). En v1, la carte montre le fond s'il s'agit d'une capture, sinon la jaquette.
- Date dans la barre d'état, widgets, social, boutique.
- Android TV : Hela ne vise que la Thor ; report assumé.
