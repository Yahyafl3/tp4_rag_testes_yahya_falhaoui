package ma.emsi.yahyafalhaoui.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Configuration {
    static final Logger LOG = Logger.getLogger("dev.langchain4j.tp4");

    private Configuration() { }

    public static String cle(String nom) {
        String valeur = System.getenv(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("Définissez la variable d'environnement " + nom
                    + " avant de lancer ce test. Voir README.md.");
        }
        return valeur;
    }

    public static ChatModel modele(boolean logging) {
        if (logging) {
            configureLogger();
        }
        return GoogleGenAiChatModel.builder()
                .apiKey(cle("GEMINI_API_KEY"))
                .modelName(System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-flash"))
                .temperature(0.0)
                .listeners(logging ? List.of(new JournalModele()) : List.of())
                .build();
    }

    public static void configureLogger() {
        Logger logger = Logger.getLogger("dev.langchain4j");
        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        // Plusieurs assistants peuvent être créés dans une même JVM, sans doubler les logs.
        for (var handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        logger.addHandler(handler);
    }

    /** Journalise les messages réels, y compris les prompts augmentés et ceux du routage.
     * Les en-têtes HTTP et les clés d'API ne font pas partie de ce journal.
     */
    private static final class JournalModele implements ChatModelListener {
        @Override
        public void onRequest(ChatModelRequestContext contexte) {
            LOG.fine(() -> "REQUETE LLM\n" + contexte.chatRequest().messages());
        }

        @Override
        public void onResponse(ChatModelResponseContext contexte) {
            LOG.fine(() -> "REPONSE LLM\n" + contexte.chatResponse().aiMessage().text());
        }
    }
}
