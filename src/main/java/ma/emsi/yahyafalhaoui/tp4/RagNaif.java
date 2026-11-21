package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;

/** Test 1 : ingestion explicite du PDF et recherche vectorielle simple. */
public final class RagNaif {
    public static void main(String[] args) {
        executer(false);
    }

    static void executer(boolean logging) {
        ChatModel modele = Configuration.modele(logging);
        RagSupport.Index index = RagSupport.ingerer("rag.pdf", RagSupport.modeleEmbeddings());
        Assistant assistant = RagSupport.assistant(modele, index.retriever());
        Conversation.avec(assistant);
    }
}
