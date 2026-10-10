# Ma semaine — widget to-do Android

6 widgets (Lun, Mar, Mer, Jeu, Ven, Week-end) à poser sur une page de l'écran d'accueil.
- Toucher une tâche : la cocher / décocher
- « + Ajouter » : ajouter une tâche au jour
- Toucher le nom du jour : ouvrir l'appli sur ce jour
- Le jour en cours est marqué d'un ●

Les tâches sont stockées uniquement sur le téléphone.

## Obtenir l'APK sans rien installer (GitHub, gratuit)

1. Crée un compte sur github.com si besoin.
2. « New repository » → nom `ma-semaine` → Create.
3. « uploading an existing file » → glisse tout le contenu du dossier dézippé → Commit.
   - Si le dossier `.github` n'a pas été envoyé (dossier caché) : Add file → Create new file,
     nom `.github/workflows/build.yml`, colle le contenu du fichier du même nom → Commit.
4. Onglet **Actions** : la compilation se lance (environ 5 min).
5. Quand c'est vert : page d'accueil du dépôt → **Releases** → télécharge `ma-semaine.apk` depuis ton téléphone.

## Installer sur le téléphone

1. Ouvre `ma-semaine.apk` depuis les téléchargements.
2. Autorise « Installer des applis inconnues » pour le navigateur quand Android le demande.
3. Installe (Play Protect peut avertir : appli non publiée sur le Play Store → « Installer quand même »).

## Créer la page

1. Appui long sur une page vide de l'écran d'accueil → **Widgets** → **Ma semaine**.
2. Pose le widget, choisis le jour. Recommence pour les 6 jours (grille 2 × 3).
3. Pour changer le jour d'un widget : appui long dessus → Paramètres.

## Alternative : Android Studio

Ouvre ce dossier dans Android Studio → Build → Build APK(s).
