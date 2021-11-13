# MinerAgent
Ce projet a été réalisé dans le cadre de l’enseignement Systèmes Multi-Agents du M2 IA à l’université Lyon 1. L’objectif était de réaliser une simulation multi-agents en Java avec le Framework JADE. Un vaisseau contenant des robots autonomes se pose sur une planète composée de minerais à extraire. Les robots doivent trouver ces minerais, communiquer sur leurs positions et les récupérer afin de les charger dans le vaisseau. Une fois le vaisseau plein, ils doivent y retourner pour que celui-ci puisse décoller.  
Pour plus d'informations, voir [rapport.pdf](rapport.pdf).



# Build
Prérequis : [maven](https://maven.apache.org/), [java](https://www.java.com/fr/)

### Installation
- `mvn install` (dont lib JADE)

### Compilation
- `mvn compile`

### Exécution
- `mvn exec:java`

### JAR
Des scénarios sont disponibles dans le dossier `/build`. Pour les exécuter : `java -jar <path>`  
- Scénario 1 (`build/MinerAgent-LowScale.jar`) : Scénario complet avec 5 robots (planète de taille 13x13)
- Scénario 2 (`build/MinerAgent-LargeScale.jar`) : Scénario complet avec 20 robots (planète de taille 21x21)
- Scénario 3 (`build/MinerAgent-FastTakeOff.jar`) : Le vaisseau demande aux robots de revenir après 10s sur la planète
- Scénario 4 (`build/MinerAgent-BigRobots.jar`) : Scénario avec des BigRobots (plus de stockage, plus lent, stratégie d'aller chercher les plus gros minerais en priorité)
- Scénario 5 (`build/MinerAgent-FastRobots.jar`) : Scénario avec des FastRobots (peu de stockage, plus rapide, stratégie d'aller chercher les minerais les plus près en priorité)



# Auteurs
* **Titouan Knockaert** _alias_ [p2004365](https://forge.univ-lyon1.fr/p2004365)
* **Gaspard Goupy** _alias_ [p1708658](https://forge.univ-lyon1.fr/p1708658)
