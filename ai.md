# TP4 RAG — Utilisation de Codex

## Test 1 — Indexer le PDF et interroger le cours

**Prompt :**

Peux-tu relier le PDF du cours à l’assistant pour que je puisse poser plusieurs questions de suite ?

**Réponse de Codex :**

Dans `RagNaif`, `RagSupport.ingerer` charge `rag.pdf` avec Apache Tika. Le découpage limite les segments à 1 000 caractères et leur chevauchement à 150 caractères. `AllMiniLmL6V2EmbeddingModel` calcule les vecteurs localement et `addAll` associe chaque vecteur à son texte dans le magasin en mémoire. Pour enrichir la question, le récupérateur fournit au plus deux passages dont le score est d’au moins 0,5. `Conversation.avec` réutilise le même assistant, avec une mémoire de dix messages.

## Test 1 avec journalisation — Examiner les passages retrouvés

**Prompt :**

Comment voir les extraits ajoutés à ma question et vérifier s’ils contiennent la réponse ?

**Réponse de Codex :**

`RagNaifLogging` active un `ChatModelListener` qui journalise la question enrichie et la réponse du modèle. `InspectionRag` affiche les passages et leurs scores en local. Pour « Quelle est la signification de RAG ? », il a retrouvé un extrait sur le routage (0,7883) et un autre sur le « RAG token model » (0,7701). Malgré ces scores, la définition du sigle manquait dans les deux passages.

## Test 2 — Reformuler une question de suivi

**Prompt :**

Si je demande ensuite « à quoi c’est comparé ? », comment garder le fine-tuning comme sujet de la recherche ?

**Réponse de Codex :**

Dans `TestTransformation`, `CompressingQueryTransformer` s’appuie sur l’historique pour rendre la question autonome avant la recherche. Le test unitaire vérifie que le message adressé au modèle simulé contient « fine-tuning » et que le transformateur renvoie la reformulation attendue. L’option `--scenario` rejoue les deux questions de l’énoncé. Ajouter `--sans-transformation` permet de comparer les deux modes sur la même relance.

## Test 3 — Choisir entre le cours et la bibliothèque

**Prompt :**

Si je demande les horaires de la bibliothèque, comment vérifier que la recherche consulte uniquement son règlement ?

**Réponse de Codex :**

`TestRoutage` indexe `rag.pdf` et `bibliotheque.txt` dans deux magasins distincts. `LanguageModelQueryRouter` présente les descriptions des sources au modèle, qui choisit où chercher. Le même modèle rédige ensuite la réponse. Dans `PipelineTest`, le choix programmé `2` désigne la bibliothèque. Le test compte un appel à son récupérateur simulé et zéro à celui du cours.

## Test 4 — Répondre à une salutation sans consulter le PDF

**Prompt :**

Comment éviter de chercher dans le PDF pour « Bonjour », puis le consulter si la question suivante porte sur le RAG ?

**Réponse de Codex :**

`Test3.RouteurConditionnel` utilise un `PromptTemplate` et interroge directement `ChatModel.chat`. Après normalisation de la décision, le routeur renvoie une liste vide pour `non` et garde le récupérateur dans les autres cas, y compris `peut-être`. Dans le test, les décisions simulées `Non.` puis `oui` déclenchent un seul appel au récupérateur, à la deuxième question. La salutation reste en mémoire.

## Test 5 — Ajouter la recherche Web avec Tavily

**Prompt :**

Je voudrais compléter le cours avec une recherche Web. Comment réunir les extraits des deux sources dans le même message ?

**Réponse de Codex :**

Dans `Test4`, `WebSearchContentRetriever` utilise `TavilyWebSearchEngine` pour la source Web. Par défaut, `DefaultQueryRouter` sélectionne le PDF et le Web à chaque question. Le test hors ligne compte un appel à chacun des deux récupérateurs simulés et vérifie que leurs extraits figurent ensemble dans le message envoyé au modèle.

## Bonus du test 5 — Choisir le Web seul pour une actualité hors cours

**Prompt :**

Si ma question porte sur un événement récent hors cours, comment écarter le PDF avant la recherche ?

**Réponse de Codex :**

L’option `--bonus` active `Test4.routeurBonus`. Les descriptions fournies à `LanguageModelQueryRouter` réservent le PDF au contenu du cours et demandent le Web seul pour une actualité extérieure. Avec le choix programmé du Web, le test observe un appel au récupérateur Web simulé, zéro à celui du PDF et uniquement l’extrait Web dans le message final.

## Problème rencontré et correction

Le projet Maven cible Java 17 avec le BOM LangChain4j `1.23.0`. La compilation de `Configuration` échouait avec `package dev.langchain4j.model.googleai does not exist`. Codex a corrigé l’import en utilisant `dev.langchain4j.model.google.genai.GoogleGenAiChatModel`, la classe fournie par `langchain4j-google-genai`, et a appelé `builder()` sur cette classe. La compilation a ensuite réussi.

## Résultats des tests

Les rapports Maven Surefire de `mvn clean verify` confirment **10 tests JUnit réussis**, sans échec, erreur ni test ignoré.

Les **3 tests de `RagLocalTest`** utilisent le PDF, les embeddings locaux et le magasin vectoriel. Le journal d’ingestion indique **95 segments** pour `rag.pdf`. Les **7 tests de `PipelineTest`** vérifient la reformulation, la mémoire et le routage avec un modèle de dialogue simulé.

Les clés Gemini et Tavily étaient indisponibles lors des essais. Aucun appel réel à ces services n’a été effectué.

## Références techniques

[Énoncé officiel du TP4](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/index.html#tests) · [Dépendances Maven du TP2](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp2-langchain4j/dependance.html) · [Aide du TP4 sur le modèle d’embeddings](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/aide/modeleembeddings.html) · [Guide RAG de LangChain4j](https://docs.langchain4j.dev/tutorials/rag/).
