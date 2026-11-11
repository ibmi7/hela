# Format de thème Cocoon et correspondance avec Argosy

Relevé du 11/11/2026 d'après https://cocoon-shell.com/wiki/theme-json-reference. La correspondance avec Argosy est estimée à partir de sa doc de thème (`design-handoff/THEMING.md`) et n'a pas encore été vérifiée dans le code.

## Structure d'un pack

```
themes/mon-theme/
├── theme.json        (seul `name` est obligatoire)
├── preview.png
├── wallpapers/       main|bottom, external|top (image, GIF, vidéo)
├── sounds/           22 noms fixes (voir plus bas)
├── music/
├── icon_overlays/{plateforme}/   mask.png, overlay.png
└── smart_folders/{type}/         icon, hero, logo
    └── by_platform/{plateforme}/
```

## Contenu de `theme.json`

- **Métadonnées** : `name`, `author`, `version`, `description`, `credits`, `website`.
- **Base** : `theme_mode` = `LIGHT`, `DARK`, `OLED` ou `SYSTEM`.
- **Fonds** : `wallpaper_main`, `wallpaper_external`. Sans ces champs, Cocoon cherche `main` ou `bottom`, et `external` ou `top`.
- **`color_scheme`** (`#RRGGBB` ou `#AARRGGBB`) : dégradés de fond, de cartes et d'accent, texte primaire et secondaire, teinte des icônes, tuiles, interrupteurs, ombres, `success`, `warning`, `divider`, `accent_glow`, dégradé de sélection optionnel.
- **`settings`** :
  - icônes et tuiles : `icon_roundness`, `icon_scale`, `hover_scale`, `icon_style` ;
  - bandeau et logos : `hero_display_style`, `hero_gradient_style`, `hero_vignette_size`, `hero_logo_scale`, `hero_folder_logo_scale` ;
  - matière : `surface_material` (`SOLID` ou `GLASS`), `glass_blur`, `glass_refraction`, `glass_highlight`, `glass_rim_strength`, `glass_on_tiles`, `glass_tint` ;
  - curseur : `cursor_style`, `highlight_thickness` ;
  - dock : `dock_icon_shape`, `hide_dock_background`, `dock_on_top`, `corner_hints`.
- **Musique** : `music_mode` (`TIME`, `PLAYLIST`, `DISABLED`), `music_time_schedule`, `music_playlist`, `music_playback_mode`.
- **Volumes** : `sfx_volume`, `music_volume`, `jingle_volume` (0.0 à 1.0).
- **`asset_refs`** : dépendances vers des packs partagés de la boutique Silk (`icon_overlays`, `smart_folders`, `sounds`, `music`).

Les couleurs et réglages absents conservent les valeurs de l'utilisateur. Aucun champ ne positionne les éléments à l'écran.

## Constats sur trois packs réels (11/11/2026)

Packs examinés : 3DS Dark Reimagined, Iisu interpreted theme, OLED Observatory (téléchargés depuis Silk, non versionnés).

- Le ZIP contient `themes/<identifiant>/theme.json`, pas un `theme.json` à la racine.
- `wallpaper_main` est une vidéo MP4 H.264 en 1920×1080 (1886×1052 pour OLED Observatory), 60 i/s, avec une piste audio AAC. `wallpaper_external` est un PNG 1240×1080 dans deux packs et une vidéo WebM VP9 1240×1080 dans le troisième.
- Le wiki décrit `main` comme l'écran du bas et `external` comme celui du haut. Les dimensions disent l'inverse sur la Thor : 1920×1080 est son écran du haut, 1240×1080 celui du bas. Hela associe donc `main` à l'écran par défaut d'Android et `external` à l'autre ; à confirmer sur l'appareil.
- Aucun pack ne contient de sons, de musique, d'icônes ni de dossiers intelligents : tout passe par `asset_refs`.
- `theme_mode` est en minuscules (`"dark"`, `"oled"`). Les couleurs apparaissent sous trois formes : `#RRGGBB`, `#AARRGGBB` et `rgba(r,g,b,a)`.
- Les références de fond peuvent omettre l'extension (d'après le wiki ; non observé dans ces packs).

## Packs d'assets Silk (constaté sur huit packs, 11/11/2026)

Packs examinés : deux de musique, deux d'icônes, trois de dossiers intelligents, puis un de sons (3DS Sound Effects : dix fichiers OGG dans `sounds/`, sans `navigate`).

```
assets/<catégorie>/<identifiant>/
├── metadata.json     id, name, author, category, version, description, file_count, settings
└── <catégorie>/      même contenu que le dossier du même nom dans un thème
```

- Le nom de fichier cité dans `music_playlist` peut différer du fichier réel par la casse (`3DS_First_Time_Setup_-_Lost_Music.mp3` contre `3ds_first_time_setup_-_lost_music.mp3`).
- `category` vaut `music`, `icon_overlays`, `smart_folders` ou `sounds`.
- `settings` est `null` sauf pour la musique, où il porte `music_mode`, `music_playback_mode`, `music_playlist` et `music_time_schedule` (chemins relatifs au pack, par exemple `music/track.mp3`).
- Icônes : `icon_overlays/<plateforme>/mask.png` et `overlay.png` ; le pack 3DS n'a qu'un dossier `global`.
- Dossiers intelligents : `smart_folders/by_platform/<plateforme>/icon`, `logo` et parfois `hero`, en PNG ou WebP.
- Hela importe ces ZIP comme les thèmes : le dossier du pack est le parent de `metadata.json`.

## Sons

Noms de base attendus dans `sounds/` : `navigate`, `select`, `back`, `folder_open`, `folder_close`, `launch`, `error`, `notification`, `discord_open`, `discord_close`, `screen_swap`, `grid_zoom_in`, `grid_zoom_out`, `reorder_pickup`, `reorder_place`, `open_edit`, `pop`, `slider_increase`, `slider_decrease`, `saving`, `saving_end`, `retroachievements`. Formats : WAV, MP3, OGG, M4A, FLAC.

## Correspondance avec Argosy

| Élément Cocoon | Dans Argosy | Difficulté |
|---|---|---|
| Fonds d'écran (haut / bas) | Argosy n'avait qu'un fond flouté en 640×360 sur l'accueil et un motif sur le second écran ; Hela ajoute sa propre couche image et vidéo (`ui/hela/theme/`) | Moyenne ; codée, à vérifier sur l'appareil |
| Sons d'interface | 11 des 22 noms Cocoon ont un événement Argosy (`navigate`, `select`, `back`, `folder_open`, `folder_close`, `launch`, `error`, `notification`, `screen_swap`, `pop`, `slider_increase`) ; les sons du pack passent par les sons personnalisés d'Argosy, et un son réglé par l'utilisateur reste prioritaire | Faite ; vérifiée sur la Thor avec 3DS Sound Effects, pas à l'oreille |
| Musique | Les pistes du pack remplacent la liste de lecture du launcher dans `BgmPlaylistCoordinator`, sans la modifier. `music_playlist` donne l'ordre ; le planning horaire (`music_time_schedule`), le mode de lecture et `music_volume` ne sont pas appliqués | Faite ; lecture vérifiée sur la Thor avec 3DS Background Music |
| Mode et couleur d'accent | `theme_mode` force le mode sombre ou clair ; l'accent vient de `selection_gradient_start`, sinon `accent_gradient_start`, sinon `accent_glow`, et la couleur secondaire de la fin du même dégradé. Branché dans `rememberArgosyPalette` | Faite, vérifiée sur la Thor |
| Fond, cartes, texte, séparateurs | Argosy dérive ces surfaces de sa propre rampe ; pas de palette libre | Moyenne : non faite, décision à prendre |
| Arrondi, échelle des icônes, style de curseur | Jetons de dimensions figés à la compilation | Moyenne |
| Masques et surcouches d'icônes par plateforme | Style de jaquette existe ; pas de masque par plateforme | Moyenne ; identifiants de plateforme Cocoon à traduire |
| Matière « verre » (flou, réfraction) | Pas d'équivalent | Élevée |
| Bandeau (hero), dock, dossiers intelligents | Propres à la disposition de Cocoon | Dépend du nouvel écran d'accueil |
| `asset_refs` | Dépend de la boutique Silk | Hors périmètre au départ |

Les identifiants de plateforme de Cocoon sont définis dans les fichiers YAML du dépôt `inssekt/CocoonFE` (dossier `android/`), utiles pour établir la table de traduction vers ceux d'Argosy.
