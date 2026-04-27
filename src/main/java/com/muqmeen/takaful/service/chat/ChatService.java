package com.muqmeen.takaful.service.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final int MAX_HISTORY_TURNS = 6;

    private static final Pattern JAILBREAK_PATTERN = Pattern.compile(
            "(?i)(ignore (all |the )?(previous|prior|above) (instruction|prompt|rule)"
                    + "|disregard (all |the )?(previous|prior|above)"
                    + "|reveal (your )?(system|developer) (prompt|instruction)"
                    + "|act as (?!a takaful)"
                    + "|pretend (you are|to be) (?!a takaful)"
                    + "|jailbreak"
                    + "|\\bDAN\\b mode"
                    + "|do anything now)"
    );

    private static final String DECLINE_REPLY =
            "I can only help with Takaful and Muqmeen Group questions. "
                    + "Would you like to know about our products or schedule a consultation?";

    private static final String UNAVAILABLE_REPLY =
            "Our chat assistant is unavailable right now. "
                    + "Please book a consultation directly using the form on the landing page "
                    + "and an agent will follow up with you.";

    private final ChatKnowledgeBase knowledgeBase;
    private final GeminiClient geminiClient;

    public ChatService(ChatKnowledgeBase knowledgeBase, GeminiClient geminiClient) {
        this.knowledgeBase = knowledgeBase;
        this.geminiClient = geminiClient;
    }

    public String reply(String userMessage, List<ChatTurn> history) {
        if (!geminiClient.isConfigured()) {
            return UNAVAILABLE_REPLY;
        }
        if (looksLikeJailbreak(userMessage)) {
            log.info("Chat: rejected message matching jailbreak deny-list");
            return DECLINE_REPLY;
        }

        List<GeminiContent> contents = buildContents(userMessage, history);
        try {
            return geminiClient.generate(knowledgeBase.systemPrompt(), contents);
        } catch (GeminiException e) {
            log.warn("Chat: Gemini call failed, returning fallback. cause={}", e.getMessage());
            return UNAVAILABLE_REPLY;
        }
    }

    private boolean looksLikeJailbreak(String message) {
        return message != null && JAILBREAK_PATTERN.matcher(message).find();
    }

    private List<GeminiContent> buildContents(String userMessage, List<ChatTurn> history) {
        List<GeminiContent> contents = new ArrayList<>();
        if (history != null) {
            int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (int i = start; i < history.size(); i++) {
                ChatTurn turn = history.get(i);
                if (turn == null || turn.text() == null || turn.text().isBlank()) continue;
                String role = "assistant".equalsIgnoreCase(turn.role()) ? "model" : "user";
                contents.add(new GeminiContent(role, List.of(new GeminiPart(turn.text()))));
            }
        }
        contents.add(new GeminiContent("user", List.of(new GeminiPart(userMessage))));
        return contents;
    }

    public record ChatTurn(String role, String text) {
    }
}
