package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Test 4 « Pas de RAG » ; nom Test3 conservé conformément à l'énoncé. */
public final class Test3 {
    public static void main(String[] args) {
        ChatModel modele = Configuration.modele(true);
        var index = RagSupport.ingerer("rag.pdf", RagSupport.modeleEmbeddings());
        var routeur = new RouteurConditionnel(modele, index.retriever());
        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        Conversation.avec(RagSupport.assistant(modele, augmentor));
    }

    /** Une liste vide signifie : transmettre le message sans ajouter d'extraits. */
    static final class RouteurConditionnel implements QueryRouter {
        private static final PromptTemplate TEMPLATE = PromptTemplate.from("""
                Est-ce que la requête suivante porte sur l'intelligence artificielle,
                le RAG, le fine-tuning ou les embeddings ?
                Réponds seulement par 'oui', 'non' ou 'peut-être'.
                Une simple salutation comme Bonjour ne porte pas sur l'IA.
                Requête : {{question}}
                """);
        private final ChatModel modele;
        private final ContentRetriever retriever;

        RouteurConditionnel(ChatModel modele, ContentRetriever retriever) {
            this.modele = modele;
            this.retriever = retriever;
        }

        @Override
        public Collection<ContentRetriever> route(Query query) {
            String prompt = TEMPLATE.apply(Map.of("question", query.text())).text();
            // Appel direct au ChatModel, sans assistant ni RAG récursif.
            String decision = modele.chat(prompt).strip().toLowerCase(Locale.ROOT)
                    .replaceAll("^[\\p{P}\\s]+|[\\p{P}\\s]+$", "");
            boolean utiliserRag = !"non".equals(decision);
            Configuration.LOG.fine(() -> "DECISION RAG : " + decision
                    + " ; retrievers sélectionnés : " + (utiliserRag ? 1 : 0));
            return utiliserRag ? List.of(retriever) : List.of();
        }
    }
}
