package br.com.gamestore.pi_3.model;

/**
 * Toda compra da loja é simulada (nada é cobrado), então ela já nasce CONCLUIDA.
 * O enum existe para permitir novos estados no futuro (ex.: CANCELADA) sem mudar o schema.
 */
public enum StatusPedido {
    CONCLUIDA
}
