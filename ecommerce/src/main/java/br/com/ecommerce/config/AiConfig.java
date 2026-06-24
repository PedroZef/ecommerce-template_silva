package br.com.ecommerce.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import com.openai.core.http.HttpClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Bean
    public OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel() {
        // Constrói o cliente HTTP necessário exigido pela SDK do OpenAI no Spring AI 2.0.0
        HttpClient httpClient = SpringAiOpenAiHttpClient.builder().build();

        // Inicializa as opções da API com a URL base do Groq e a respectiva API Key
        ClientOptions clientOptions = ClientOptions.builder()
                .httpClient(httpClient)
                .baseUrl("https://api.groq.com/openai/v1/")
                .apiKey(groqApiKey)
                .build();

        // Cria a instância do cliente da SDK oficial do OpenAI
        OpenAIClient openAiClient = new OpenAIClientImpl(clientOptions);

        // Retorna o bean do modelo de transcrição utilizando o Whisper-large-v3 do Groq
        return OpenAiAudioTranscriptionModel.builder()
                .openAiClient(openAiClient)
                .options(OpenAiAudioTranscriptionOptions.builder()
                        .model("whisper-large-v3")
                        .build())
                .build();
    }
}
