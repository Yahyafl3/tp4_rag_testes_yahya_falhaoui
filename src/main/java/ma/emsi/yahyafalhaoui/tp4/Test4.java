package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test 5 Web ; nom Test4 conservé conformément à l'énoncé. */
public final class Test4 {
    public static void main(String[] args) {
        String cleTavily = Configuration.cle("TAVILY_API_KEY");
        ChatModel modele = Configuration.modele(true);
        var index = RagSupport.ingerer("rag.pdf", RagSupport.modeleEmbeddings());
        ContentRetriever pdf = index.retriever();
        WebSearchEngine moteur = TavilyWebSearchEngine.builder().apiKey(cleTavily).build();
        ContentRetriever web = WebSearchContentRetriever.builder().webSearchEngine(moteur).build();

        boolean bonus = Arrays.asList(args).contains("--bonus");
        QueryRouter routeur = bonus
                ? routeurBonus(modele, pdf, web)
                : new DefaultQueryRouter(pdf, web);
        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        System.out.println(bonus ? "Bonus : sélection PDF / Web par le LLM."
                : "Test 5 : recherche systématique dans le PDF et sur le Web.");
        Conversation.avec(RagSupport.assistant(modele, augmentor));
    }

    static QueryRouter routeurBonus(ChatModel modele, ContentRetriever pdf, ContentRetriever web) {
        // Code initial conservé pour le correcteur :
        // QueryRouter routeur = new DefaultQueryRouter(pdf, web);
        // Il consulte toujours les DEUX sources, même pour une question d'actualité.
        Map<ContentRetriever, String> sources = new LinkedHashMap<>();
        sources.put(pdf, "Support de cours sur RAG, fine-tuning, embeddings et bases vectorielles. "
                + "À sélectionner uniquement si la question concerne ce cours. "
                + "Ce document ne contient aucune actualité ni événement récent.");
        sources.put(web, "Recherche Web pour l'actualité, les événements récents et toute "
                + "information extérieure au cours. Pour une question d'actualité sans rapport "
                + "avec le cours, sélectionner uniquement cette source.");
        return new LanguageModelQueryRouter(modele, sources);
    }
}
