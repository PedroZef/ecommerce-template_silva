package br.com.ecommerce.config;

import java.time.Duration;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.core.http.HttpClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnProperty(name = "groq.api-key")
    public OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel(
            @Value("${groq.api-key}") String groqApiKey) {
        HttpClient httpClient = SpringAiOpenAiHttpClient.builder().build();

        ClientOptions clientOptions = ClientOptions.builder()
                .httpClient(httpClient)
                .baseUrl("https://api.groq.com/openai/v1/")
                .apiKey(groqApiKey)
                .build();

        OpenAIClient openAiClient = new OpenAIClientImpl(clientOptions);

        return OpenAiAudioTranscriptionModel.builder()
                .openAiClient(openAiClient)
                .options(OpenAiAudioTranscriptionOptions.builder()
                        .model("whisper-large-v3")
                        .language("pt")
                        .temperature(0.0f)
                        .responseFormat(com.openai.models.audio.AudioResponseFormat.TEXT)
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }
}
