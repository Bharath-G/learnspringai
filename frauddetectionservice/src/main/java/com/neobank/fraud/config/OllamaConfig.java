package com.neobank.fraud.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    /**
     * ChatClient is the main Spring AI interface
     * Spring AI auto-configures OllamaChatModel from application.yml
     * We just wrap it in a ChatClient bean
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
            .defaultSystem("""
                You are a fraud detection AI for NeoBank India.
                You ALWAYS respond with valid JSON only.
                You NEVER include explanations outside the JSON.
                You NEVER include markdown code blocks.
                Your JSON ALWAYS matches the exact schema requested.
                """)
            .build();
    }
}