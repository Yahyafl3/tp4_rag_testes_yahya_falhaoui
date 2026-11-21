package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;

import java.util.Arrays;

/** Test 2 : reformuler une question de suivi avec l'historique de conversation. */
public final class TestTransformation {
    public static void main(String[] args) {
        var options = Arrays.asList(args);
        ChatModel modele = Configuration.modele(true);
        var index = RagSupport.ingerer("rag.pdf", RagSupport.modeleEmbeddings());
        boolean transformer = !options.contains("--sans-transformation");
        var builder = DefaultRetrievalAugmentor.builder().contentRetriever(index.retriever());
        if (transformer) {
            QueryTransformer compression = new CompressingQueryTransformer(modele);
            builder.queryTransformer(query -> {
                var reformulees = compression.transform(query);
                reformulees.forEach(q -> Configuration.LOG.fine(
                        "QUESTION POUR LA RECHERCHE : " + q.text()));
                return reformulees;
            });
        }
        Assistant assistant = RagSupport.assistant(modele, builder.build());
        System.out.println("Transformation de la question : " + transformer);
        if (options.contains("--scenario")) {
            for (String question : new String[]{
                    "Qu'est-ce que le fine-tuning ?",
                    "Dans le support de cours fourni, à quoi c'est comparé ?"}) {
                System.out.println("Vous : " + question);
                System.out.println("Assistant : " + assistant.chat(question));
            }
        } else {
            Conversation.avec(assistant);
        }
    }
}
