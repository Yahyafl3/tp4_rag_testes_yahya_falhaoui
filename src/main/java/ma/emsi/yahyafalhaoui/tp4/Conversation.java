package ma.emsi.yahyafalhaoui.tp4;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Conversation {
    private Conversation() { }

    /** Boucle inspirée de l'aide conversation du TP2, citée dans le README. */
    public static void avec(Assistant assistant) {
        System.out.println("Posez vos questions (fin pour quitter).");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                System.out.print("Vous : ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String question = scanner.nextLine().trim();
                if ("fin".equalsIgnoreCase(question)) {
                    break;
                }
                if (!question.isBlank()) {
                    System.out.println("Assistant : " + assistant.chat(question));
                }
            }
        }
    }
}
