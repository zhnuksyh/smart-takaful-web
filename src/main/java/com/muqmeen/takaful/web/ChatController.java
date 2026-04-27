package com.muqmeen.takaful.web;

import com.muqmeen.takaful.service.chat.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int RATE_LIMIT_MAX_REQUESTS = 6;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final ChatService chatService;
    private final ConcurrentHashMap<String, Deque<Instant>> ipHits = new ConcurrentHashMap<>();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        String clientIp = clientIp(http);
        if (!allowRequest(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("reply",
                            "You're sending messages a bit too fast. Please wait a minute and try again."));
        }

        List<ChatService.ChatTurn> history = request.history() == null
                ? List.of()
                : request.history().stream()
                        .map(t -> new ChatService.ChatTurn(t.role(), t.text()))
                        .toList();

        String reply = chatService.reply(request.message(), history);
        return ResponseEntity.ok(new ChatReply(reply));
    }

    private boolean allowRequest(String ip) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(RATE_LIMIT_WINDOW);
        Deque<Instant> hits = ipHits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                hits.pollFirst();
            }
            if (hits.size() >= RATE_LIMIT_MAX_REQUESTS) {
                return false;
            }
            hits.offerLast(now);
            return true;
        }
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return http.getRemoteAddr() == null ? "unknown" : http.getRemoteAddr();
    }

    public record ChatRequest(
            @NotBlank @Size(max = 500) String message,
            List<ChatTurnDto> history
    ) {
    }

    public record ChatTurnDto(String role, String text) {
    }

    public record ChatReply(String reply) {
    }
}
