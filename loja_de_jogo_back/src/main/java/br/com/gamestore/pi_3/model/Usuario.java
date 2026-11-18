// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/model/Usuario.java
package br.com.gamestore.pi_3.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String username; // para clientes é o e-mail (minúsculo)

    @Column(nullable = false)
    private String password; // hash BCrypt

    private String role; // Ex: "ROLE_USER", "ROLE_ADMIN"
}