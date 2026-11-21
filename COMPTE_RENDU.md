# TP4 — Notes pour le compte rendu des tests

Auteur : Yahya Falhaoui. Périmètre : applications Java console uniquement.

## Statut des observations

Compilation et dix tests automatisés validés le 21 novembre 2026, avec Java 25
et Maven 3.9.12, pour une cible Java 17. Le PDF réel a été parsé et indexé en
95 segments. La recherche vectorielle locale a été exécutée.

Les appels Gemini et Tavily n'ont pas été exécutés faute de clés configurées.
Les tests de logique de dialogue utilisent des réponses simulées explicitement.
Aucune réponse simulée ne doit être présentée comme une réponse de Gemini.
Les sections ci-dessous expliquent le fonctionnement ; leurs emplacements de
traces restent à compléter après les exécutions réelles décrites dans le README.

## Test 2 — Transformation de la question

La question « à quoi c'est comparé ? » dépend du sujet de l'échange précédent.
Dans le RAG naïf, la mémoire est envoyée au LLM final mais la recherche de
similarité part de la question seule. Elle peut donc récupérer des passages qui
ne concernent pas le fine-tuning. Une réponse finale correcte reste possible
grâce à l'historique, aux connaissances du modèle ou à des passages retrouvés
par hasard : cela ne prouve pas que la recherche est pertinente.

`CompressingQueryTransformer` fournit au modèle l'historique et la question
actuelle pour produire une requête autonome mentionnant le fine-tuning.
Cette requête sert à la recherche ; le message utilisateur initial est ensuite
augmenté des passages retrouvés. La reformulation et le prompt augmenté sont
deux étapes distinctes à identifier dans les logs.

À copier dans le corps de l'email après exécution :

- Réponse réelle sans transformation : **non recueillie**.
- Question augmentée réellement envoyée au LLM sans transformation : **non recueillie**.
- Nouvelle réponse réelle avec transformation : **non recueillie**.
- Question augmentée réellement envoyée au LLM avec transformation : **non recueillie**.
- Comparaison précise des passages retrouvés et de la réponse : **à compléter**.

## Test 3 — Routage

Deux magasins indépendants contiennent le support de cours et le règlement
fictif d'une bibliothèque. Le même modèle lit leurs descriptions et choisit
une ou plusieurs sources. `LanguageModelQueryRouter` attend un numéro ou des
numéros séparés par des virgules. Seuls les retrievers sélectionnés cherchent
des passages. Le journal des requêtes expose le prompt exact de sélection.

Texte réel envoyé au modèle pour choisir entre les deux retrievers :
**non recueilli**. À copier depuis `REQUETE LLM` dans l'email ; ne pas remplacer
cette trace par un exemple inventé.

## Test 4 — Pas de RAG

Le routeur personnalisé demande au modèle de classer la question : `oui`, `non`
ou `peut-être` pour son rapport avec l'IA. Après normalisation de la casse et
de la ponctuation, `non` retourne une liste vide. L'augmentor ne récupère alors
aucun document. Dans les autres cas, il interroge le support de cours.

Le test automatisé vérifie l'enchaînement « Bonjour » puis une question IA,
avec un modèle simulé : le retriever est d'abord ignoré, puis appelé, et la
mémoire conserve l'échange précédent. Ce résultat ne remplace pas le test réel
de la décision prise par Gemini.

## Test 5 et bonus — Web et PDF

`DefaultQueryRouter` consulte toujours les deux retrievers. Il ne détermine pas
si le sujet relève du cours. Le retriever PDF calcule la similarité et peut
retourner des passages qui dépassent 0,5 même s'ils sont peu utiles pour une
question d'actualité. Dans ce cas, ces passages sont ajoutés au prompt avec
les résultats Tavily. Si aucun passage ne dépasse le seuil, il n'y a pas
d'extrait PDF ajouté ; il faut décrire ce qui s'est réellement produit.

Le bonus remplace cette sélection systématique par `LanguageModelQueryRouter`.
Les descriptions réservent le PDF aux sujets du cours et dirigent l'actualité
vers le Web. Cela évite de consulter le PDF quand le modèle choisit seulement
la source Web. Comme ce choix est effectué par un LLM, sa pertinence doit être
vérifiée sur plusieurs questions.

À recueillir et copier dans le corps de l'email :

- Question portant sur un événement récent et date de l'essai : **à compléter**.
- Log initial montrant les passages PDF effectivement ajoutés, s'il y en a : **non recueilli**.
- Log du choix de source avec `--bonus` et prompt augmenté correspondant : **non recueilli**.
- Comparaison des réponses avant et après routage : **à compléter**.

## Vérifications déjà exécutées

| Vérification | Résultat |
| --- | --- |
| Compilation du code principal et des tests | Réussie |
| Lecture du PDF avec Tika et génération des embeddings locaux | Réussie, 95 segments |
| Recherche réelle : seuil 0,5 et limite de deux résultats | Réussie |
| Deux sources dans deux magasins distincts | Réussie |
| Compression avec historique et premier message sans compression | Réussie, LLM simulé |
| Sélection de la bibliothèque sans consulter le cours | Réussie, LLM simulé |
| Absence de RAG pour `non`, activation pour `oui` et `peut-être` | Réussie, LLM simulé |
| Conservation de la mémoire entre deux questions | Réussie, LLM simulé |
| Routeur Web initial : deux sources ; bonus : Web seul | Réussie, LLM simulé |
| Appels Gemini et Tavily réels | Non exécutés, clés absentes |
