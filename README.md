# TP4 RAG — Tests — Yahya Falhaoui

Projet Java console consacré uniquement à la partie **Quelques tests** du
[TP4 RAG de Richard Grin](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/index.html).

Le TP demande de créer un nouveau projet Java pour les tests. Il renvoie au TP2
pour les dépendances, la conversation console et le logging. Aucun code de mon
ancien dépôt TP2 n'est copié ; la reprise de l'application Web TP2 appartient à
la partie Web, hors du périmètre de ce dépôt.

Prérequis : JDK 17 ou supérieur et Maven 3.9 ou supérieur.

## Configuration

Le projet utilise LangChain4j 1.23.0 avec son BOM, le modèle local
`AllMiniLmL6V2EmbeddingModel`, Tika pour le PDF et Gemini pour le dialogue.
Le BOM aligne les modules stables et leurs versions bêta associées.
Il n'y a pas de dépendance `langchain4j-easy-rag`.

Dans PowerShell, définir les clés dans le terminal qui lancera les programmes :

```powershell
$env:GEMINI_API_KEY = "votre-cle-Gemini"
$env:TAVILY_API_KEY = "votre-cle-Tavily" # seulement pour le test Web
$env:GEMINI_MODEL = "gemini-2.5-flash"  # facultatif, valeur par défaut
mvn clean verify
```

Les clés ne doivent jamais être commitées. Aucun fichier `.env` n'est chargé
automatiquement. Le modèle Gemini est configurable si son nom ou sa disponibilité
change pour votre compte. Dans IntelliJ, ouvrir le `pom.xml`, synchroniser Maven,
puis renseigner les variables d'environnement dans la configuration d'exécution.

## Les expériences du TP

Toutes les classes se trouvent dans `ma.emsi.yahyafalhaoui.tp4`.

| Partie de l'énoncé | Classe principale | Comportement |
| --- | --- | --- |
| Test 1 | `RagNaif` | Chargement du PDF, découpage, embeddings, magasin, récupération puis conversation |
| Test 1 avec logging | `RagNaifLogging` | Même assistant avec journal des messages réellement envoyés et reçus |
| Diagnostic optionnel | `InspectionRag` | Segments et scores, sans clé API |
| Test 2 | `TestTransformation` | `CompressingQueryTransformer` utilise l'historique pour la recherche |
| Test 3 | `TestRoutage` | `LanguageModelQueryRouter` choisit cours ou bibliothèque fictive |
| Test 4 — Pas de RAG | `Test3` | Routeur personnalisé : `non` renvoie une liste vide, sinon utilisation du PDF |
| Test 5 — Web | `Test4` | `DefaultQueryRouter` interroge PDF et Tavily ; `--bonus` sélectionne les sources par LLM |

Les noms `Test3` et `Test4` suivent les noms demandés dans l'énoncé, malgré le
décalage avec les numéros des tests.

Depuis la racine du dépôt, après `mvn compile`, lancer par exemple :

```powershell
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.RagNaif"
```

Remplacer `RagNaif` par la classe voulue dans le tableau. La conversation accepte
plusieurs questions ; saisir `fin` pour quitter. La fin de l'entrée standard
termine aussi proprement la boucle.

Le fichier `src/main/resources/rag.pdf` est le support fourni par l'enseignant.
Le second document, `bibliotheque.txt`, est fictif et indépendant de l'IA.
Leurs sources sont précisées dans `src/main/resources/SOURCES.md`.

## Scénarios à essayer

**Test 1.** Poser successivement les cinq questions de l'énoncé :

1. Quelle est la signification de « RAG » ?
2. Pourquoi la plupart des applications d'entreprise qui utilisent un LLM ont-elles besoin de faire du RAG ?
3. J'ai enregistré des textes et leur embedding dans un magasin d'embeddings, comment récupérer les deux textes les plus similaires à une question ?
4. Peux-tu me donner du code pour cela avec LangChain4j ?
5. Quels slides as-tu utilisés pour me donner ce code ?

Le découpage est de 1 000 **caractères** avec 150 caractères de chevauchement.
Le retriever conserve au plus deux résultats de score supérieur ou égal à 0,5,
et la mémoire conserve dix messages. Les constantes sont dans `RagSupport`.
Les références exactes aux diapositives ne sont pas garanties : Tika extrait le
texte sans conserver ici une association fiable de chaque passage à sa page.

Pour inspecter les passages et les scores sans Gemini :

```powershell
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.InspectionRag"
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.InspectionRag" "-Dexec.args=--segments"
```

**Test 2.** Exécuter le même scénario sans, puis avec transformation, dans deux
processus séparés pour repartir avec une mémoire vide :

```powershell
New-Item -ItemType Directory -Force logs | Out-Null
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.TestTransformation" "-Dexec.args=--sans-transformation --scenario" 2>&1 | Tee-Object logs/transformation-avant.log
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.TestTransformation" "-Dexec.args=--scenario" 2>&1 | Tee-Object logs/transformation-apres.log
```

Le scénario pose « Qu'est-ce que le fine-tuning ? », puis « Dans le support de
cours fourni, à quoi c'est comparé ? ». Sans `--scenario`, le dialogue est libre.
Comparer les réponses, la `QUESTION POUR LA RECHERCHE` et la dernière
`REQUETE LLM` contenant le message augmenté. L'historique aide déjà le LLM final,
mais le retriever naïf recherche à partir de la seule question courante.

**Test 3.** Lancer `TestRoutage`, demander « À quoi sert le RAG ? », puis
« Combien de livres peut-on emprunter à la bibliothèque ? ». Les logs montrent
les deux descriptions numérotées, le choix du LLM et le prompt augmenté.
Essayer aussi « Bonjour » pour observer le comportement de ce routeur.

**Test 4.** Lancer `Test3`, saisir « Bonjour », puis « À quoi sert le RAG ? ».
Le journal `DECISION RAG` indique le nombre de retrievers sélectionnés. Une
réponse `non` du modèle doit donner zéro retriever. `oui`, `peut-être` ou une
réponse inattendue conservent le retriever, comme la règle de l'énoncé.

**Test 5.** Lancer `Test4` avec les deux clés, poser une question sur le cours
puis une question sur un événement récent. Refaire cette dernière question avec
le bonus dans une conversation neuve :

```powershell
mvn exec:java "-Dexec.mainClass=ma.emsi.yahyafalhaoui.tp4.Test4" "-Dexec.args=--bonus"
```

Le routeur initial interroge systématiquement les deux sources. Le seuil peut
filtrer tous les passages du PDF : leur présence dans le prompt dépend donc de
la question et des scores. Le bonus utilise des descriptions pour que le LLM
choisisse le Web seul quand le cours ne convient pas ; ce choix reste à vérifier
dans les logs. Le code initial est conservé en commentaire dans `routeurBonus`,
et reste exécutable sans `--bonus` pour la comparaison.

## Vérifications et compte rendu

```powershell
mvn clean verify
```

Les dix tests automatisés n'utilisent aucune clé : ils vérifient le vrai PDF,
Tika, les embeddings locaux et la recherche vectorielle, puis la compression,
la mémoire et les branches des routeurs avec un LLM simulé. Ils ne prouvent pas
la qualité des réponses de Gemini ni la disponibilité des services distants.

Validation effectuée le 21 novembre 2026 : compilation réussie et dix tests
réussis. Le PDF produit 95 segments avec les paramètres ci-dessus. Aucun appel
réel Gemini ou Tavily n'a été exécuté : leurs clés n'étaient pas configurées.

`COMPTE_RENDU.md` donne les explications et la liste des traces réelles à
recueillir pour le bonus. L'énoncé demande de copier ces éléments dans le corps
de l'email à l'enseignant. Aucun email n'est envoyé par ce projet.

## Références utilisées

- [Énoncé du TP4](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/index.html)
- [Dépendances et BOM du TP2](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp2-langchain4j/dependance.html)
- [Boucle de conversation du TP2](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp2-langchain4j/conversation.html)
- [Modèle d'embeddings imposé par l'aide du TP4](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/aide/modeleembeddings.html)
- [Documentation RAG de LangChain4j](https://docs.langchain4j.dev/tutorials/rag/)

Les pages de correction du serveur de cours répondaient HTTP 403 lors de la
réalisation ; l'implémentation s'appuie sur l'énoncé, ses aides accessibles et
les API/sources publiées de LangChain4j 1.23.0.
