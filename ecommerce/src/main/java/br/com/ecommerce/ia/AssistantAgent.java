package br.com.ecommerce.ia;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.ecommerce.ia.tools.EcommerceTools;
import br.com.ecommerce.model.Interaction;
import br.com.ecommerce.repository.InteractionRepository;

@Service
public class AssistantAgent {

    private static final Logger logger = LoggerFactory.getLogger(AssistantAgent.class);
    private static final long CLEANUP_INTERVAL = 30 * 60 * 1000L;

    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final EcommerceTools ecommerceTools;
    private final InteractionRepository interactionRepository;
    private final Map<String, LatestInteraction> latestInteractions = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    public AssistantAgent(Optional<OpenAiAudioTranscriptionModel> transcriptionModelOpt,
                          ChatClient.Builder chatClientBuilder,
                          EcommerceTools ecommerceTools,
                          InteractionRepository interactionRepository) {
        this.transcriptionModel = transcriptionModelOpt.orElse(null);
        this.ecommerceTools = ecommerceTools;
        this.interactionRepository = interactionRepository;
        this.chatClient = chatClientBuilder
                .defaultSystem("Você é o Assistente Virtual Inteligente do E-Commerce Premium e Gestor Financeiro. " +
                        "Sua função é ajudar gerentes tanto na administração do e-commerce quanto no registro de despesas e gastos pessoais ou empresariais. " +
                        "Regras essenciais:\n" +
                        "1. Se o usuário solicitar para gerar, exibir ou desenhar gráficos ou relatórios visuais, explique de forma clara e simpática que os gráficos interativos de faturamento e categorias já estão renderizados em tempo real diretamente na tela principal do Dashboard. Ofereça-se para fornecer os dados numéricos em texto usando o resumo de vendas ou despesas.\n" +
                        "2. Apenas sugira ou ofereça o cadastro de despesas ou registro de gastos quando o usuário solicitar explicitamente o registro de um gasto, compra, pagamento ou custo. Nunca ofereça registrar despesas em resposta a dúvidas sobre gráficos ou relatórios gerais.\n" +
                        "3. Seja curto, direto e natural.")
                .build();
    }

    private String getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = (auth != null) ? auth.getName() : null;
        return (name == null || name.equals("anonymousUser")) ? "anonymousUser" : name;
    }

    public String processarComandoDeVoz(Resource arquivoDeAudio) {
        if (transcriptionModel == null) {
            return avisoSemChaveVoz();
        }
        String texto;
        try {
            texto = transcriptionModel.call(new AudioTranscriptionPrompt(arquivoDeAudio)).getResult().getOutput();
        } catch (Exception e) {
            logger.warn("Falha na transcrição por voz: {}", e.getMessage());
            return avisoSemChaveVoz();
        }
        logger.info("[Voice Command] Transcrição: \"{}\"", texto);
        String resposta;
        if (texto == null || texto.trim().isEmpty() || texto.trim().equals(".")) {
            resposta = "Não consegui ouvir o comando de voz. Por favor, fale novamente.";
        } else {
            resposta = executarChat(texto);
        }
        logger.info("[Voice Command] Resposta: \"{}\"", resposta);
        salvarEAtualizar("VOICE", texto, resposta);
        return resposta;
    }

    public String processarComandoDeTexto(String texto) {
        logger.info("[Text Command] Recebido: \"{}\"", texto);
        String resposta;
        if (texto == null || texto.trim().isEmpty() || texto.trim().equals(".")) {
            resposta = "Por favor, digite uma pergunta ou comando válido.";
        } else {
            resposta = executarChat(texto);
        }
        logger.info("[Text Command] Resposta: \"{}\"", resposta);
        salvarEAtualizar("TEXT", texto, resposta);
        return resposta;
    }

    private String executarChat(String texto) {
        try {
            return chatClient.prompt()
                    .user(texto)
                    .tools(ecommerceTools)
                    .call()
                    .content();
        } catch (Exception e) {
            logger.warn("Falha ao invocar LLM remoto: {}. Usando fallback local.", e.getMessage());
            return processarLocalmente(texto);
        }
    }

    private String processarLocalmente(String texto) {
        String query = texto.toLowerCase().trim();

        String resposta;
        resposta = handleGraficos(query);
        if (resposta != null) return resposta;

        resposta = handleFaturamento(query);
        if (resposta != null) return resposta;

        resposta = handlePedidoStatus(query, texto);
        if (resposta != null) return resposta;

        resposta = handleCadastroProduto(query, texto);
        if (resposta != null) return resposta;

        resposta = handleListarCategoria(query, texto);
        if (resposta != null) return resposta;

        resposta = handleEstoqueProduto(query, texto);
        if (resposta != null) return resposta;

        return MENSAGEM_FALLBACK;
    }

    private String handleGraficos(String query) {
        if (query.contains("grafico") || query.contains("gráfico") || query.contains("graficos") || query.contains("gráficos")) {
            return "[Modo Offline] Não consigo desenhar gráficos em formato de imagem. No entanto, os gráficos interativos de faturamento e categorias já estão renderizados em tempo real na tela principal do Dashboard. Se preferir ver os dados consolidados em texto, por favor, digite 'Faturamento'.";
        }
        return null;
    }

    private static final String MENSAGEM_FALLBACK =
            "[Modo Offline - Sem API Key da IA]\n" +
            "Não foi possível contatar o modelo Groq/OpenAI, mas você pode usar comandos estruturados off-line:\n" +
            "- 'Faturamento' (exibe resumo de vendas);\n" +
            "- 'Estoque do [Produto]' (busca preço e estoque);\n" +
            "- 'Produtos da categoria [Categoria]' (lista itens);\n" +
            "- 'Cadastrar produto [Nome], preco [Valor], estoque [Qtd], categoria [Categoria]';\n" +
            "- 'Atualizar pedido [ID] para [CONCLUIDO/CANCELADO/PENDENTE]'.";

    private String handleFaturamento(String query) {
        if (query.contains("faturamento") || query.contains("venda") || query.contains("resumo")
                || query.contains("relatorio") || query.contains("relatório")) {
            return "[Modo Offline] " + ecommerceTools.obterResumoVendas("Consulta local offline");
        }
        return null;
    }

    private String handlePedidoStatus(String query, String textoOriginal) {
        if (!query.contains("pedido") || !(query.contains("status") || query.contains("para")
                || query.contains("atualizar") || query.contains("cancel") || query.contains("conclui"))) {
            return null;
        }
        try {
            Matcher numberMatcher = Pattern.compile("\\b\\d+\\b").matcher(query);
            Long idPedido = numberMatcher.find() ? Long.parseLong(numberMatcher.group()) : null;

            String novoStatus = null;
            if (query.contains("concluido") || query.contains("concluído")) {
                novoStatus = "CONCLUIDO";
            } else if (query.contains("cancelado") || query.contains("cancelar")) {
                novoStatus = "CANCELADO";
            } else if (query.contains("pendente")) {
                novoStatus = "PENDENTE";
            }

            if (idPedido != null && novoStatus != null) {
                return "[Modo Offline] " + ecommerceTools.atualizarStatusPedido(idPedido, novoStatus);
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private String handleCadastroProduto(String query, String textoOriginal) {
        if (!query.contains("cadastrar") && !query.contains("cadastro")) {
            return null;
        }
        try {
            String nome = "";
            BigDecimal preco = BigDecimal.ZERO;
            Integer estoque = 0;
            String categoria = "Geral";
            String descricao = "Cadastrado via comando local";

            if (query.contains("categoria")) {
                int catIdx = query.indexOf("categoria");
                categoria = textoOriginal.substring(catIdx + 9).trim();
                categoria = stripPrefix(categoria);
            }

            Matcher priceMatcher = Pattern.compile("(?i)(?:custando|preco|preço|valor de|r\\$)?\\s*(\\d+(?:[.,]\\d{2})?)")
                    .matcher(query);
            if (priceMatcher.find()) {
                String priceStr = priceMatcher.group(1).replace(",", ".");
                preco = new BigDecimal(priceStr);
            }

            Matcher stockMatcher = Pattern.compile("(?i)(\\d+)\\s*(?:unidades|itens|no estoque|estoque)").matcher(query);
            if (stockMatcher.find()) {
                estoque = Integer.parseInt(stockMatcher.group(1));
            } else {
                Matcher anyNum = Pattern.compile("\\b\\d+\\b").matcher(query);
                int lastNum = -1;
                while (anyNum.find()) {
                    lastNum = Integer.parseInt(anyNum.group());
                }
                if (lastNum != -1 && estoque == 0) {
                    estoque = lastNum;
                }
            }

            int startIdx = -1;
            if (query.contains("cadastrar produto")) {
                startIdx = query.indexOf("cadastrar produto") + 17;
            } else if (query.contains("cadastrar")) {
                startIdx = query.indexOf("cadastrar") + 9;
            }

            if (startIdx != -1) {
                int endIdx = textoOriginal.length();
                String[] keywords = {"custando", "preco", "preço", "valor", "r$", "com ", "estoque", "na categoria", "categoria"};
                for (String kw : keywords) {
                    int idx = query.indexOf(kw, startIdx);
                    if (idx != -1 && idx < endIdx) {
                        endIdx = idx;
                    }
                }
                nome = textoOriginal.substring(startIdx, endIdx).trim();
            }

            if (nome.isEmpty()) {
                nome = "Produto Sem Nome";
            }

            return "[Modo Offline] " + ecommerceTools.cadastrarProduto(nome, descricao, preco, estoque, categoria);
        } catch (Exception e) {
            return "[Modo Offline] Erro ao cadastrar produto. Estrutura recomendada: " +
                    "'Cadastrar produto [Nome], preco [Valor], estoque [Qtd], categoria [Categoria]'.";
        }
    }

    private String handleListarCategoria(String query, String textoOriginal) {
        if (!query.contains("categoria") || !(query.contains("listar") || query.contains("produtos") || query.contains("mostrar"))) {
            return null;
        }
        String categoria = "";
        int catIdx = query.indexOf("categoria");
        if (catIdx != -1) {
            categoria = textoOriginal.substring(catIdx + 9).trim();
        } else {
            int deIdx = query.indexOf(" de ");
            if (deIdx != -1) {
                categoria = textoOriginal.substring(deIdx + 4).trim();
            }
        }
        categoria = categoria.replace(":", "").trim();
        categoria = stripPrefix(categoria);
        if (!categoria.isEmpty()) {
            return "[Modo Offline] " + ecommerceTools.listarProdutosPorCategoria(categoria);
        }
        return null;
    }

    private String handleEstoqueProduto(String query, String textoOriginal) {
        if (!query.contains("estoque") && !query.contains("preco") && !query.contains("preço")
                && !query.contains("busca") && !query.contains("detalhe")) {
            return null;
        }
        String produto = "";
        int[] positions = {query.indexOf(" de "), query.indexOf(" do "), query.indexOf(" da ")};
        int queryStart = -1;
        for (int p : positions) {
            if (p != -1) {
                queryStart = p + 4;
                break;
            }
        }
        if (queryStart != -1) {
            produto = textoOriginal.substring(queryStart).replace("?", "").replace(".", "").trim();
        } else {
            String tempStr = query.replace("estoque", "").replace("preco", "").replace("preço", "").trim();
            produto = stripPrefix(tempStr);
        }
        if (!produto.isEmpty()) {
            return "[Modo Offline] " + ecommerceTools.obterEstoqueEPrecoProduto(produto);
        }
        return null;
    }

    private static String stripPrefix(String s) {
        String lower = s.toLowerCase();
        if (lower.startsWith("de ")) return s.substring(3).trim();
        if (lower.startsWith("do ")) return s.substring(3).trim();
        if (lower.startsWith("da ")) return s.substring(3).trim();
        return s;
    }

    private String avisoSemChaveVoz() {
        String aviso = "[Aviso] O comando de voz (Whisper) exige uma chave GROQ_API_KEY configurada no .env. Use o campo de texto como alternativa.";
        salvarEAtualizar("VOICE", "[Comando de Voz Falhou]", aviso);
        return aviso;
    }

    private void salvarEAtualizar(String tipo, String pergunta, String resposta) {
        String user = getUsuarioLogado();
        String query = pergunta != null ? pergunta : "";
        salvarInteracao(tipo, query, resposta);
        evictOldEntries();
        latestInteractions.put(user, new LatestInteraction(tipo, query, resposta));
    }

    private void salvarInteracao(String tipo, String pergunta, String resposta) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioLogado = (auth != null) ? auth.getName() : null;
            String cleanUser = (usuarioLogado == null || usuarioLogado.equals("anonymousUser")) ? null : usuarioLogado;
            interactionRepository.save(new Interaction(tipo, pergunta, resposta, cleanUser));
        } catch (Exception e) {
            logger.error("Erro ao salvar interação no banco de dados", e);
        }
    }

    private void evictOldEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL) return;
        lastCleanup = now;
        latestInteractions.entrySet().removeIf(entry -> {
            LatestInteraction li = entry.getValue();
            return li != null && (now - li.getTimestamp()) > CLEANUP_INTERVAL;
        });
    }

    public LatestInteraction getLatestInteraction(String username) {
        String key = (username == null || username.equals("anonymousUser")) ? "anonymousUser" : username;
        return latestInteractions.get(key);
    }
}
