package br.com.ecommerce.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.ecommerce.ia.AssistantAgent;
import br.com.ecommerce.ia.LatestInteraction;
import br.com.ecommerce.model.Interaction;
import br.com.ecommerce.repository.InteractionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/assistant")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);
    private static final long MAX_AUDIO_SIZE = 25 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("ogg", "wav", "mp3", "webm", "m4a");

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
    public ResponseEntity<String> handleVoiceCommand(
            @Parameter(description = "Arquivo de áudio (.ogg, .wav, .mp3) com o comando de voz")
            @RequestPart("audio") MultipartFile audioFile) {

        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Arquivo de áudio vazio.");
        }
        if (audioFile.getSize() > MAX_AUDIO_SIZE) {
            return ResponseEntity.badRequest().body("Erro: Arquivo muito grande. Máximo permitido: 25MB.");
        }

        String ext = obterExtensao(audioFile.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return ResponseEntity.badRequest().body("Erro: Formato de áudio não suportado. Use: " + ALLOWED_EXTENSIONS);
        }

        try {
            File tempFile = File.createTempFile("voice_", "." + ext);
            try {
                audioFile.transferTo(tempFile);
                String resposta = agent.processarComandoDeVoz(new FileSystemResource(tempFile));
                logger.info("Comando de voz processado com sucesso.");
                return ResponseEntity.ok(resposta);
            } finally {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        } catch (IOException e) {
            logger.error("Erro de IO ao processar áudio", e);
            return ResponseEntity.internalServerError().body("Erro ao processar arquivo de áudio.");
        } catch (Exception e) {
            logger.error("Erro ao processar comando de voz", e);
            return ResponseEntity.internalServerError().body("Erro interno ao processar comando de voz.");
        }
    }

    @Operation(
        summary = "Consultas inteligentes e comandos por texto",
        description = "Recebe uma pergunta ou comando por escrito em texto simples, processa via IA usando as ferramentas disponíveis do e-commerce (consulta de estoque, cadastro de produto, vendas etc.), e retorna a resposta."
    )
    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleTextCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pergunta ou comando do e-commerce")
            @RequestBody String commandText) {
        if (commandText == null || commandText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Comando de texto vazio.");
        }
        try {
            String resposta = agent.processarComandoDeTexto(commandText);
            logger.info("Comando de texto processado com sucesso.");
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            logger.error("Erro ao processar comando de texto", e);
            return ResponseEntity.internalServerError().body("Erro interno ao processar comando.");
        }
    }

    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LatestInteraction> getLatestInteraction() {
        String usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        LatestInteraction interaction = agent.getLatestInteraction(usuarioLogado);
        if (interaction == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(interaction);
    }

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

    private static String obterExtensao(String filename) {
        if (filename == null || !filename.contains(".")) return "webm";
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (ext.contains("x-m4a") || ext.contains("mp4")) return "m4a";
        return ext;
    }
}
