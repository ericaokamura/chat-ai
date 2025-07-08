package io.fiap.erp.chat_ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/ai")
    public String chat() {
        String question = "Quais são os itens do estoque que precisam ser reabastecidos e em que quantidade?";
        List<Document> relevantDocs = pgVectorStore.similaritySearch(question);
        String context = relevantDocs.stream()
                .map(Document::getFormattedContent)
                .reduce("", (a, b) -> a + "\n" + b);

        String promptText = String.format("""
                Based on the following context, answer the question. 
                If you cannot answer based on the context, say "I don't have enough information."
                
                Context: %s
                
                Question: %s
                """, context, question);

        return chatClient.prompt(new Prompt(promptText))
                .user(question)
                .call()
                .content();
    }

}
