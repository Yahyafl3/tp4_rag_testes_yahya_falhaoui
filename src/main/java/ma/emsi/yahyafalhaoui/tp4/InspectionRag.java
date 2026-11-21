package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

import java.util.Arrays;

/** Diagnostic optionnel du test 1 : aucune clé ni API distante nécessaire. */
public final class InspectionRag {
    public static void main(String[] args) {
        Configuration.configureLogger();
        RagSupport.Index index = RagSupport.ingerer("rag.pdf", RagSupport.modeleEmbeddings());
        if (Arrays.asList(args).contains("--segments")) {
            for (int i = 0; i < index.segments().size(); i++) {
                System.out.printf("SEGMENT %d : %s%n", i, index.segments().get(i).text());
            }
        }
        String question = args.length == 0 || args[0].startsWith("--")
                ? "Quelle est la signification de RAG ?" : args[0];
        System.out.println("Question : " + question);
        var contenus = index.retriever().retrieve(Query.from(question));
        System.out.println("ContentRetriever : " + contenus.size() + " résultat(s).");
        var recherche = EmbeddingSearchRequest.builder()
                .queryEmbedding(index.modele().embed(question).content())
                .maxResults(RagSupport.MAX_RESULTATS)
                .minScore(RagSupport.SCORE_MINIMAL)
                .build();
        index.magasin().search(recherche).matches().forEach(match ->
                System.out.printf("Score %.4f | %s%n", match.score(), match.embedded().text()));
    }
}
