# Comparatif des bases possibles pour Hela

Relevé du 11/11/2026. Dépôts clonés et structure lue ; pas d'audit de code. Voir aussi `argosy-cartographie.md`.

| | Code de l'app | Licence | Stack | RomM | Thèmes |
|---|---|---|---|---|---|
| Argosy 2.31 | Publié | GPL-3.0 | Kotlin / Compose, ~390 000 lignes | Client officiel | Packs couleurs/fonds/sons/polices ; pas de dispositions communautaires (décision écrite) |
| NeoStation | Publié | GPL-3.0 | Flutter / Dart, ~220 000 lignes | ~20 700 lignes, ajoutée par des contributeurs | Thèmes de couleurs compilés en Dart + packs d'illustrations système téléchargeables |
| Cocoon 3.11 | Non publié | Aucun fichier de licence | Inconnue | Non trouvée | Packs `theme.json` + images/sons/musiques, boutique Silk Pod |
| iiSU | Non publié | Aucun fichier de licence | Inconnue | Expérimentale | Non examinés |

## Constats

- Le dépôt `inssekt/CocoonFE` ne contient que 116 définitions de plateformes en YAML, des outils et le catalogue de mises à jour. Le dépôt `iisu-network/iiSU` ne contient qu'un README et un catalogue (dernier commit 20/06/2026). Aucun des deux n'est forkable.
- Un thème Cocoon est un dossier `theme.json` + `preview.png` + ressources. Il règle couleurs, base clair/sombre/OLED, fonds d'écran, forme et taille des icônes, style du bandeau, sons et musiques. Aucun champ ne positionne les éléments à l'écran (voir `format-theme-cocoon.md`).
- Seuls Argosy et NeoStation peuvent servir de base. Argosy a été retenu : natif, intégration RomM de première main, BIOS, synchro de sauvegardes.

## Piste retenue

Fork d'Argosy, puis :

1. Importeur de packs au format Cocoon, branché sur les réglages de thème d'Argosy (fonds, couleurs, sons, musique, icônes).
2. Nouvel écran d'accueil inspiré de Cocoon ou d'iiSU, ajouté à côté des écrans existants. Le code de ces deux apps étant fermé, il s'agit d'une réécriture inspirée de leur agencement, pas d'une reprise.

Les deux sont voulus : les packs (couleurs, fonds, sons) et la disposition des écrans.
