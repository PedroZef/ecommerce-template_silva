package br.com.ecommerce.controller;

import br.com.ecommerce.ia.AssistantAgent;
import br.com.ecommerce.ia.LatestInteraction;
import br.com.ecommerce.model.Interaction;
import br.com.ecommerce.repository.InteractionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);
    private final AssistantAgent agent;
    private final InteractionRepository interactionRepository;

    public AssistantController(AssistantAgent agent, InteractionRepository interactionRepository) {
        this.agent = agent;
        this.interactionRepository = interactionRepository;
    }

    @Operation(
        summary = "Processar comando de voz",
        description = "Recebe um arquivo de áudio contendo um comando do e-commerce, transcreve para texto, usa a IA para interpretar e executa a ação/ferramenta correspondente."
    )
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SuppressWarnings("UseSpecificCatch")
    public ResponseEntity<String> handleVoiceCommand(
            @Parameter(description = "Arquivo de áudio (.ogg, .wav, .mp3) com o comando de voz")
            @RequestPart("audio") MultipartFile audioFile) {
        try {
            String originalFilename = audioFile.getOriginalFilename();
            String extension = ".ogg"; // fallback default
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            java.io.File tempFile = java.io.File.createTempFile("voice_", extension);
            audioFile.transferTo(tempFile);

            String resposta = agent.processarComandoDeVoz(new org.springframework.core.io.FileSystemResource(tempFile));
            
            tempFile.delete(); // Limpa o arquivo temp depois de usar
            
            logger.info("Operação concluída com sucesso: Comando de voz processado.");
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            e.printStackTrace();
            String detalhe = e.getMessage();
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
                rootCause = rootCause.getCause();
            }
            detalhe += " | Erro Real: " + rootCause.getMessage();
            return ResponseEntity.internalServerError().body("Erro: " + detalhe);
        }
    }

    @Operation(
        summary = "Consultas inteligentes e comandos por texto",
        description = "Recebe uma pergunta ou comando por escrito em texto simples, processa via IA usando as ferramentas disponíveis do e-commerce (consulta de estoque, cadastro de produto, vendas etc.), e retorna a resposta."
    )
    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleTextCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pergunta ou comando do e-commerce. Exemplos: 'Como está o estoque do Smartphone?', 'Cadastrar produto Camisa Polo custando 120 reais com 10 no estoque na categoria Roupas', 'Qual o faturamento total?'")
            @RequestBody String commandText) {
        try {
            String resposta = agent.processarComandoDeTexto(commandText);
            logger.info("Operação concluída com sucesso: Comando de texto processado.");
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Obter a última interação do assistente",
        description = "Retorna os detalhes da última interação (de texto ou voz) que foi processada."
    )
    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LatestInteraction> getLatestInteraction() {
        String usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        LatestInteraction interaction = agent.getLatestInteraction(usuarioLogado);
        if (interaction == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(interaction);
    }

    @Operation(
        summary = "Listar histórico de interações do assistente",
        description = "Retorna todos os detalhes das interações (de texto ou voz) cadastradas."
    )
    @GetMapping(value = "/interactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Interaction>> listarInteracoes() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String usuarioLogado = (auth != null) ? auth.getName() : null;
        
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                
        List<Interaction> interacoes;
        if (isAdmin || usuarioLogado == null || usuarioLogado.equals("anonymousUser")) {
            interacoes = interactionRepository.findAllByOrderByTimestampDesc();
        } else {
            interacoes = interactionRepository.findByUsuarioOrUsuarioIsNullOrderByTimestampDesc(usuarioLogado);
        }
        return ResponseEntity.ok(interacoes);
    }
}
