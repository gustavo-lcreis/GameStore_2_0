package br.com.gamestore.pi_3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria de ações administrativas. Sem FK para usuarios DE PROPÓSITO: o alvo costuma ser um
 * usuário que acabou de ser excluído, e o registro precisa sobreviver a ele (guarda id e e-mail como "foto").
 * Esta tabela só recebe INSERT; nenhum endpoint altera ou apaga linhas.
 */
@Entity
@Table(name = "auditoria_admin")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaAdmin {

    public static final String EXCLUSAO_USUARIO = "EXCLUSAO_USUARIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String acao;

    private Long adminId;

    @Column(length = 150)
    private String adminEmail;

    private Long alvoId;

    @Column(length = 150)
    private String alvoEmail;

    @Column(length = 255)
    private String detalhe;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public static AuditoriaAdmin exclusaoUsuario(Usuario admin, Usuario alvo, int pedidosPreservados) {
        AuditoriaAdmin a = new AuditoriaAdmin();
        a.setAcao(EXCLUSAO_USUARIO);
        a.setAdminId(admin != null ? admin.getId() : null);
        a.setAdminEmail(admin != null ? admin.getUsername() : null);
        a.setAlvoId(alvo.getId());
        a.setAlvoEmail(alvo.getUsername());
        a.setDetalhe("pedidos preservados: " + pedidosPreservados);
        a.setDataHora(LocalDateTime.now());
        return a;
    }
}
