package br.com.ecommerce.model;

public enum MeioPagamento {
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    PIX("Pix"),
    BOLETO("Boleto");

    private final String descricao;

    MeioPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
