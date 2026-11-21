package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/** Les étapes d'ingestion restent explicites, sans la dépendance easy-rag. */
public final class RagSupport {
    // Ces tailles sont en caractères ; l'overload sans TokenCountEstimator compte les caractères.
    public static final int TAILLE_SEGMENT = 1000;
    public static final int CHEVAUCHEMENT = 150;
    public static final int MAX_RESULTATS = 2;
    public static final double SCORE_MINIMAL = 0.5;

    private RagSupport() { }

    public static EmbeddingModel modeleEmbeddings() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    public static Index ingerer(String ressource, EmbeddingModel modele) {
        // Phase 1 : parser, découper, calculer les embeddings, puis les stocker.
        Document document = ClassPathDocumentLoader.loadDocument(
                ressource, new ApacheTikaDocumentParser());
        DocumentSplitter splitter = DocumentSplitters.recursive(TAILLE_SEGMENT, CHEVAUCHEMENT);
        List<TextSegment> segments = splitter.split(document);
        Response<List<Embedding>> reponse = modele.embedAll(segments);
        List<Embedding> embeddings = reponse.content();
        InMemoryEmbeddingStore<TextSegment> magasin = new InMemoryEmbeddingStore<>();
        magasin.addAll(embeddings, segments);
        Configuration.LOG.info(() -> ressource + " : " + segments.size() + " segments indexés.");
        return new Index(magasin, modele, List.copyOf(segments));
    }

    public static Assistant assistant(ChatModel modele, ContentRetriever retriever) {
        return AiServices.builder(Assistant.class)
                .chatModel(modele)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(retriever)
                .build();
    }

    public static Assistant assistant(ChatModel modele, RetrievalAugmentor augmentor) {
        return AiServices.builder(Assistant.class)
                .chatModel(modele)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(augmentor)
                .build();
    }

    public record Index(InMemoryEmbeddingStore<TextSegment> magasin,
                        EmbeddingModel modele, List<TextSegment> segments) {
        public ContentRetriever retriever() {
            // Phase 2 : les deux meilleurs résultats, de score >= 0,5.
            return EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(magasin)
                    .embeddingModel(modele)
                    .maxResults(MAX_RESULTATS)
                    .minScore(SCORE_MINIMAL)
                    .build();
        }
    }
}
