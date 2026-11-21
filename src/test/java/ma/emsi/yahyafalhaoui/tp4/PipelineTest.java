package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Le LLM est simulé pour vérifier les branchements sans clé, quota ou réponse aléatoire. */
class PipelineTest {
    @Test
    void laCompressionUtiliseLHistoriquePourUneQuestionDeSuivi() {
        var modele = new ModeleSimule("À quoi le fine-tuning est-il comparé dans le cours ?");
        var suivi = UserMessage.from("Dans le support de cours fourni, à quoi c'est comparé ?");
        var historique = List.of(UserMessage.from("Qu'est-ce que le fine-tuning ?"),
                AiMessage.from("C'est un entraînement complémentaire."));
        var query = Query.from(suivi.singleText(), Metadata.from(suivi, "test", historique));
        var resultat = new CompressingQueryTransformer(modele).transform(query);
        assertEquals("À quoi le fine-tuning est-il comparé dans le cours ?",
                resultat.iterator().next().text());
        assertTrue(modele.requetes.get(0).messages().toString().contains("fine-tuning"));
        assertEquals(query.metadata(), resultat.iterator().next().metadata());
    }

    @Test
    void unePremiereQuestionNeNecessitePasDeCompression() {
        var modele = new ModeleSimule();
        var originale = Query.from("Qu'est-ce que le RAG ?");
        assertEquals(List.of(originale), new ArrayList<>(
                new CompressingQueryTransformer(modele).transform(originale)));
        assertTrue(modele.requetes.isEmpty());
    }

    @Test
    void leRoutageVersLaBibliothequeNeConsultePasLeCours() {
        var coursAppels = new AtomicInteger();
        var biblioAppels = new AtomicInteger();
        ContentRetriever cours = q -> { coursAppels.incrementAndGet(); return List.of(Content.from("COURS_RAG")); };
        ContentRetriever biblio = q -> { biblioAppels.incrementAndGet(); return List.of(Content.from("HORAIRE_BIBLIO")); };
        Map<ContentRetriever, String> sources = new LinkedHashMap<>();
        sources.put(cours, "Cours IA");
        sources.put(biblio, "Horaires bibliothèque");
        var modele = new ModeleSimule("2", "Elle ouvre à 9 h.");
        var augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new LanguageModelQueryRouter(modele, sources)).build();
        RagSupport.assistant(modele, augmentor).chat("À quelle heure ouvre la bibliothèque ?");
        assertEquals(0, coursAppels.get());
        assertEquals(1, biblioAppels.get());
        assertTrue(modele.requetes.get(0).messages().toString().contains("1: Cours IA"));
        assertTrue(modele.requetes.get(1).messages().toString().contains("HORAIRE_BIBLIO"));
        assertFalse(modele.requetes.get(1).messages().toString().contains("COURS_RAG"));
    }

    @Test
    void bonjourPuisQuestionIaDesactivePuisActiveLeRag() {
        var appels = new AtomicInteger();
        ContentRetriever retriever = q -> {
            appels.incrementAndGet();
            return List.of(Content.from("EXTRAIT_COURS_RAG"));
        };
        var modele = new ModeleSimule("Non.", "Bonjour !", "oui", "Le RAG utilise des documents.");
        var augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new Test3.RouteurConditionnel(modele, retriever)).build();
        var assistant = RagSupport.assistant(modele, augmentor);
        assertEquals("Bonjour !", assistant.chat("Bonjour"));
        assertEquals(0, appels.get());
        assertFalse(modele.requetes.get(1).messages().toString().contains("EXTRAIT_COURS_RAG"));
        assistant.chat("À quoi sert le RAG ?");
        assertEquals(1, appels.get());
        String derniereRequete = modele.requetes.get(3).messages().toString();
        assertTrue(derniereRequete.contains("EXTRAIT_COURS_RAG"));
        assertTrue(derniereRequete.contains("Bonjour !"), "La mémoire doit conserver l'échange précédent.");
    }

    @Test
    void leDouteConserveLeRagCommeDemandeDansLEnonce() {
        ContentRetriever retriever = q -> List.of();
        var routeur = new Test3.RouteurConditionnel(new ModeleSimule("peut-être"), retriever);
        assertEquals(List.of(retriever), new ArrayList<>(routeur.route(Query.from("Et les modèles ?"))));
    }

    @Test
    void leRouteurWebInitialConsulteToujoursLesDeuxSources() {
        var pdfAppels = new AtomicInteger();
        var webAppels = new AtomicInteger();
        ContentRetriever pdf = q -> { pdfAppels.incrementAndGet(); return List.of(Content.from("EXTRAIT_PDF")); };
        ContentRetriever web = q -> { webAppels.incrementAndGet(); return List.of(Content.from("EXTRAIT_WEB")); };
        var modele = new ModeleSimule("Voici l'actualité.");
        var augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new DefaultQueryRouter(pdf, web)).build();
        RagSupport.assistant(modele, augmentor).chat("Quel événement a eu lieu hier ?");
        assertEquals(1, pdfAppels.get());
        assertEquals(1, webAppels.get());
        String prompt = modele.requetes.get(0).messages().toString();
        assertTrue(prompt.contains("EXTRAIT_PDF"));
        assertTrue(prompt.contains("EXTRAIT_WEB"));
    }

    @Test
    void leBonusPeutChoisirLeWebSeulPourLActualite() {
        var pdfAppels = new AtomicInteger();
        var webAppels = new AtomicInteger();
        ContentRetriever pdf = q -> { pdfAppels.incrementAndGet(); return List.of(Content.from("EXTRAIT_PDF")); };
        ContentRetriever web = q -> { webAppels.incrementAndGet(); return List.of(Content.from("EXTRAIT_WEB")); };
        var modele = new ModeleSimule("2", "Voici l'actualité.");
        var augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(Test4.routeurBonus(modele, pdf, web)).build();
        RagSupport.assistant(modele, augmentor).chat("Quel événement a eu lieu hier ?");
        assertEquals(0, pdfAppels.get());
        assertEquals(1, webAppels.get());
        assertFalse(modele.requetes.get(1).messages().toString().contains("EXTRAIT_PDF"));
        assertTrue(modele.requetes.get(1).messages().toString().contains("EXTRAIT_WEB"));
    }

    private static final class ModeleSimule implements ChatModel {
        private final ArrayDeque<String> reponses;
        private final List<ChatRequest> requetes = new ArrayList<>();

        ModeleSimule(String... reponses) {
            this.reponses = new ArrayDeque<>(Arrays.asList(reponses));
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requetes.add(request);
            if (reponses.isEmpty()) {
                throw new AssertionError("Appel LLM imprévu : " + request.messages());
            }
            return ChatResponse.builder().aiMessage(AiMessage.from(reponses.removeFirst())).build();
        }
    }
}
