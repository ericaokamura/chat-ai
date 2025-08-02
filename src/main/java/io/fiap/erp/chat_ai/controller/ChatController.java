package io.fiap.erp.chat_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    private final PgVectorStore pgVectorStore;

    public ChatController(ChatClient.Builder builder, PgVectorStore pgVectorStore) {
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(pgVectorStore))
                .build();
        this.pgVectorStore = pgVectorStore;

    }

    @GetMapping("/ai/{question}")
    @CrossOrigin(value="http://localhost:5173, http://localhost:5174")
    public String chat(@PathVariable("question") String question) {

        List<Document> relevantDocs = pgVectorStore.similaritySearch(question);

        if (relevantDocs.isEmpty()) {
            return "⚠️ Nenhum documento relevante foi encontrado no vetor. Não é possível responder à pergunta.";
        }

        System.out.println("📄 Documentos retornados pelo pgVector:");
        relevantDocs.forEach(doc -> System.out.println(doc.getFormattedContent()));

        String context = relevantDocs.stream()
                .map(Document::getFormattedContent)
                .reduce("", (a, b) -> a + "\n" + b);

        String promptText = String.format("""
        Baseando-se no seguinte contexto, responda à pergunta.
        Se não puder responder com base no contexto, diga "Não tenho informação suficiente."

        Contexto: %s

        Pergunta: %s
        """, context, question);

        return chatClient.prompt(new Prompt(promptText))
                .user(question)
                .call()
                .content();
    }
}
