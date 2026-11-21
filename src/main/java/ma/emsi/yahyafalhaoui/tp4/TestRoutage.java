package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Test 3 : le même LLM sélectionne les sources et répond à la question. */
public final class TestRoutage {
    public static void main(String[] args) {
        ChatModel modele = Configuration.modele(true);
        EmbeddingModel embeddings = RagSupport.modeleEmbeddings();
        var cours = RagSupport.ingerer("rag.pdf", embeddings);
        var bibliotheque = RagSupport.ingerer("bibliotheque.txt", embeddings);
        Map<ContentRetriever, String> sources = new LinkedHashMap<>();
        sources.put(cours.retriever(), "Cours d'intelligence artificielle : RAG, fine-tuning, "
                + "embeddings, bases vectorielles, récupération et augmentation de prompts.");
        sources.put(bibliotheque.retriever(), "Règlement d'une bibliothèque fictive : "
                + "horaires, emprunts, inscriptions, retours et salle de lecture. Aucun contenu sur l'IA.");
        var routeur = new LanguageModelQueryRouter(modele, sources);
        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        Conversation.avec(RagSupport.assistant(modele, augmentor));
    }
}
