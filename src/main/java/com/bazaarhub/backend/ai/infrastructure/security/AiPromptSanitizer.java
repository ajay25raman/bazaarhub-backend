package com.bazaarhub.backend.ai.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sanitizes input queries to prevent prompt injection, jailbreaks, and sensitive data exfiltration.
 */
@Component
public class AiPromptSanitizer {

    private static final Logger log = LoggerFactory.getLogger(AiPromptSanitizer.class);

    private static final int MAX_INPUT_LENGTH = 2000;

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (all )?(previous|prior) (instructions|directions|prompts)"),
            Pattern.compile("(?i)system prompt"),
            Pattern.compile("(?i)reveal (your|the) (secret|instructions|api key|credentials|database)"),
            Pattern.compile("(?i)you are now in (DAN|developer|unrestricted|jailbreak) mode"),
            Pattern.compile("(?i)disregard (safety|guidelines|rules)"),
            Pattern.compile("(?i)bypass (security|checks|authentication|authorization)")
    );

    /**
     * Sanitizes and validates user query text.
     *
     * @param rawInput user-provided string
     * @return cleaned and bounded text
     */
    public String sanitize(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "";
        }

        String input = rawInput.trim();
        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }

        // Detect potential prompt injection attempts
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("[AI SECURITY ALERT] Potential prompt injection pattern detected in user query: '{}'", input);
                // Neutralize the injection by replacing matching text with a neutral indicator
                input = pattern.matcher(input).replaceAll("[FILTERED]");
            }
        }

        // Clean control characters while preserving valid punctuation and whitespace
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    }
}
