package br.com.ecommerce.ia;

import br.com.ecommerce.ia.tools.EcommerceTools;
import br.com.ecommerce.model.Interaction;
import br.com.ecommerce.repository.InteractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AssistantAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(AssistantAgent.class);
    
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final EcommerceTools ecommerceTools;
    private final InteractionRepository interactionRepository;

    public AssistantAgent(OpenAiAudioTranscriptionModel transcriptionModel, 
                          ChatClient.Builder chatClientBuilder, 
                          EcommerceTools ecommerceTools,
                          InteractionRepository interactionRepository) {
        this.transcriptionModel = transcriptionModel;
        this.ecommerceTools = ecommerceTools;
        this.interactionRepository = interactionRepository;
        this.chatClient = chatClientBuilder
                .defaultSystem("Você é o Assistente Virtual Inteligente do E-Commerce Premium e Gestor Financeiro. " +
                        "Sua função é ajudar gerentes tanto na administração do e-commerce quanto no registro de despesas e gastos pessoais ou empresariais (como compras em supermercados, farmácias e outros). " +
                        "Seja curto, direto e natural. Use as ferramentas de despesas quando o usuário quiser registrar um gasto ou pedir um relatório financeiro pessoal/empresarial.")
                .build();
    }

    private final java.util.Map<String, LatestInteraction> latestInteractions = new java.util.concurrent.ConcurrentHashMap<>();

    private String getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = (auth != null) ? auth.getName() : null;
        return (name == null || name.equals("anonymousUser")) ? "anonymousUser" : name;
    }

    public String processarComandoDeVoz(Resource arquivoDeAudio) {
        String texto;
        try {
            texto = transcriptionModel.call(new AudioTranscriptionPrompt(arquivoDeAudio)).getResult().getOutput();
        } catch (Exception e) {
            logger.warn("Falha na transcrição por voz (ex: API Key inválida/401). Retornando aviso. Erro original: " + e.getMessage());
            String aviso = "[Aviso] O comando de voz (Whisper) exige uma API Key válida configurada no arquivo .env. Para testar sem a chave, por favor use a barra de digitação por texto na tela.";
            salvarInteracao("VOICE", "[Comando de Voz Falhou]", aviso);
            this.latestInteractions.put(getUsuarioLogado(), new LatestInteraction("VOICE", "[Comando de Voz Falhou]", aviso));
            return aviso;
        }
        
        logger.info("[Voice Command] Transcrição do áudio: \"{}\"", texto);
        
        String resposta;
        if (texto == null || texto.trim().isEmpty() || texto.trim().equals(".")) {
            resposta = "Não consegui ouvir o comando de voz. Por favor, fale novamente.";
        } else {
            resposta = executarChat(texto);
        }

        logger.info("[Voice Command] Resposta do Assistente: \"{}\"", resposta);
        
        salvarInteracao("VOICE", (texto != null ? texto : ""), resposta);
        
        this.latestInteractions.put(getUsuarioLogado(), new LatestInteraction("VOICE", (texto != null ? texto : ""), resposta));
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

        logger.info("[Text Command] Resposta do Assistente: \"{}\"", resposta);
        
        salvarInteracao("TEXT", (texto != null ? texto : ""), resposta);
        
        this.latestInteractions.put(getUsuarioLogado(), new LatestInteraction("TEXT", (texto != null ? texto : ""), resposta));
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
            logger.warn("Falha ao invocar LLM remoto (ex: API Key inválida/401). Utilizando processador local inteligente. Erro original: " + e.getMessage());
            return processarLocalmente(texto);
        }
    }

    private String processarLocalmente(String texto) {
        String query = texto.toLowerCase().trim();
        
        // 1. Resumo de Vendas / Faturamento
        if (query.contains("faturamento") || query.contains("venda") || query.contains("resumo") || query.contains("relatorio") || query.contains("relatório")) {
            return "[Modo Offline] " + ecommerceTools.obterResumoVendas("Consulta local offline");
        }
        
        // 2. Atualizar Status de Pedido
        if (query.contains("pedido") && (query.contains("status") || query.contains("para") || query.contains("atualizar") || query.contains("cancel") || query.contains("conclui"))) {
            try {
                Long idPedido = null;
                java.util.regex.Matcher numberMatcher = java.util.regex.Pattern.compile("\\b\\d+\\b").matcher(query);
                if (numberMatcher.find()) {
                    idPedido = Long.parseLong(numberMatcher.group());
                }
                
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
        }
        
        // 3. Cadastrar Produto
        if (query.contains("cadastrar") || query.contains("cadastro")) {
            try {
                String nome = "";
                java.math.BigDecimal preco = java.math.BigDecimal.ZERO;
                Integer estoque = 0;
                String categoria = "Geral";
                String descricao = "Cadastrado via comando local";
                
                if (query.contains("categoria")) {
                    int catIdx = query.indexOf("categoria");
                    categoria = texto.substring(catIdx + 9).trim();
                    if (categoria.toLowerCase().startsWith("de") || categoria.toLowerCase().startsWith("do") || categoria.toLowerCase().startsWith("da")) {
                        categoria = categoria.substring(2).trim();
                    }
                }
                
                java.util.regex.Matcher priceMatcher = java.util.regex.Pattern.compile("(?i)(?:custando|preco|preço|valor de|r\\$)?\\s*(\\d+(?:[.,]\\d{2})?)").matcher(query);
                if (priceMatcher.find()) {
                    String priceStr = priceMatcher.group(1).replace(",", ".");
                    preco = new java.math.BigDecimal(priceStr);
                }
                
                java.util.regex.Matcher stockMatcher = java.util.regex.Pattern.compile("(?i)(\\d+)\\s*(?:unidades|itens|no estoque|estoque)").matcher(query);
                if (stockMatcher.find()) {
                    estoque = Integer.parseInt(stockMatcher.group(1));
                } else {
                    java.util.regex.Matcher anyNum = java.util.regex.Pattern.compile("\\b\\d+\\b").matcher(query);
                    int lastNum = -1;
                    while (anyNum.find()) {
                        lastNum = Integer.parseInt(anyNum.group());
                    }
                    if (lastNum != -1 && estoque == 0) {
                        estoque = lastNum;
                    }
                }
                
                String cleanText = texto;
                int startIdx = -1;
                if (query.contains("cadastrar produto")) {
                    startIdx = query.indexOf("cadastrar produto") + 17;
                } else if (query.contains("cadastrar")) {
                    startIdx = query.indexOf("cadastrar") + 9;
                }
                
                if (startIdx != -1) {
                    int endIdx = cleanText.length();
                    String[] keywords = {"custando", "preco", "preço", "valor", "r$", "com ", "estoque", "na categoria", "categoria"};
                    for (String kw : keywords) {
                        int idx = query.indexOf(kw, startIdx);
                        if (idx != -1 && idx < endIdx) {
                            endIdx = idx;
                        }
                    }
                    nome = cleanText.substring(startIdx, endIdx).trim();
                }
                
                if (nome.isEmpty()) {
                    nome = "Produto Sem Nome";
                }
                
                return "[Modo Offline] " + ecommerceTools.cadastrarProduto(nome, descricao, preco, estoque, categoria);
            } catch (Exception e) {
                return "[Modo Offline] Erro ao cadastrar produto. Estrutura recomendada: 'Cadastrar produto [Nome], preco [Valor], estoque [Qtd], categoria [Categoria]'.";
            }
        }
        
        // 4. Listar Produtos de Categoria
        if (query.contains("categoria") && (query.contains("listar") || query.contains("produtos") || query.contains("mostrar"))) {
            String categoria = "";
            int catIdx = query.indexOf("categoria");
            if (catIdx != -1) {
                categoria = texto.substring(catIdx + 9).trim();
            } else {
                int deIdx = query.indexOf(" de ");
                if (deIdx != -1) {
                    categoria = texto.substring(deIdx + 4).trim();
                }
            }
            categoria = categoria.replace(":", "").trim();
            if (categoria.toLowerCase().startsWith("de") || categoria.toLowerCase().startsWith("do") || categoria.toLowerCase().startsWith("da")) {
                categoria = categoria.substring(2).trim();
            }
            if (!categoria.isEmpty()) {
                return "[Modo Offline] " + ecommerceTools.listarProdutosPorCategoria(categoria);
            }
        }
        
        // 5. Estoque de Produto
        if (query.contains("estoque") || query.contains("preco") || query.contains("preço") || query.contains("busca") || query.contains("detalhe")) {
            String produto = "";
            int deIdx = query.indexOf(" de ");
            int doIdx = query.indexOf(" do ");
            int daIdx = query.indexOf(" da ");
            int queryStart = -1;
            if (deIdx != -1) queryStart = deIdx + 4;
            else if (doIdx != -1) queryStart = doIdx + 4;
            else if (daIdx != -1) queryStart = daIdx + 4;
            else {
                String tempStr = query.replace("estoque", "").replace("preco", "").replace("preço", "").trim();
                if (tempStr.startsWith("do") || tempStr.startsWith("da") || tempStr.startsWith("de")) {
                    tempStr = tempStr.substring(2).trim();
                }
                produto = tempStr;
            }
            
            if (queryStart != -1) {
                produto = texto.substring(queryStart).replace("?", "").replace(".", "").trim();
            }
            
            if (!produto.isEmpty()) {
                return "[Modo Offline] " + ecommerceTools.obterEstoqueEPrecoProduto(produto);
            }
        }
        
        return "[Modo Offline - Sem API Key da IA]\n" +
               "Não foi possível contatar o modelo Groq/OpenAI, mas você pode usar comandos estruturados off-line:\n" +
               "- 'Faturamento' (exibe resumo de vendas);\n" +
               "- 'Estoque do [Produto]' (busca preço e estoque);\n" +
               "- 'Produtos da categoria [Categoria]' (lista itens);\n" +
               "- 'Cadastrar produto [Nome], preco [Valor], estoque [Qtd], categoria [Categoria]';\n" +
               "- 'Atualizar pedido [ID] para [CONCLUIDO/CANCELADO/PENDENTE]'.";
    }

    private void salvarInteracao(String tipo, String pergunta, String resposta) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuarioLogado = (auth != null) ? auth.getName() : null;
            String cleanUser = (usuarioLogado == null || usuarioLogado.equals("anonymousUser")) ? null : usuarioLogado;
            
            Interaction interaction = new Interaction(tipo, pergunta, resposta, cleanUser);
            interactionRepository.save(interaction);
        } catch (Exception e) {
            logger.error("Erro ao salvar interação no banco de dados", e);
        }
    }

    public LatestInteraction getLatestInteraction(String username) {
        String key = (username == null || username.equals("anonymousUser")) ? "anonymousUser" : username;
        return this.latestInteractions.get(key);
    }
}
