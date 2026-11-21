package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Vrais PDF, parseur Tika, modèle ONNX et magasin vectoriel ; aucun appel distant. */
class RagLocalTest {
    private static EmbeddingModel modele;
    private static RagSupport.Index cours;

    @BeforeAll
    static void preparer() {
        modele = RagSupport.modeleEmbeddings();
        cours = RagSupport.ingerer("rag.pdf", modele);
    }

    @Test
    void lePdfEstLisibleEtIndexe() {
        assertTrue(cours.segments().size() > 1, "Le vrai support doit produire plusieurs segments.");
        assertTrue(cours.segments().stream().anyMatch(s -> s.text().contains("RAG")));
        assertTrue(cours.segments().stream().allMatch(s -> !s.text().isBlank()));
        assertEquals(384, modele.embed("RAG").content().dimension());
    }

    @Test
    void leRetrieverRespecteLeSeuilEtLaLimiteAvecLesVraisEmbeddings() {
        // Un segment existant garantit une correspondance forte, indépendamment de la langue.
        String question = cours.segments().get(0).text();
        var matches = cours.magasin().search(EmbeddingSearchRequest.builder()
                .queryEmbedding(modele.embed(question).content())
                .maxResults(2).minScore(0.5).build()).matches();
        var contenus = cours.retriever().retrieve(Query.from(question));
        assertFalse(matches.isEmpty());
        assertTrue(matches.size() <= 2);
        assertTrue(matches.stream().allMatch(m -> m.score() >= 0.5));
        assertEquals(matches.stream().map(m -> m.embedded().text()).toList(),
                contenus.stream().map(c -> c.textSegment().text()).toList());
    }

    @Test
    void lesDeuxSourcesSontStockeesDansDesMagasinsDistincts() {
        var bibliotheque = RagSupport.ingerer("bibliotheque.txt", modele);
        assertNotSame(cours.magasin(), bibliotheque.magasin());
        var resultats = bibliotheque.retriever().retrieve(Query.from(
                bibliotheque.segments().get(0).text()));
        assertFalse(resultats.isEmpty());
        assertTrue(resultats.get(0).textSegment().text().contains("quatorze"));
    }
}
