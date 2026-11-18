// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/service/ImagemStorageService.java
package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.exception.UploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ImagemStorageService {

    /** Prefixo público: é o que fica gravado no banco, ex.: /assets/jogos/<uuid>.jpg */
    public static final String CAMINHO_PUBLICO = "/assets/jogos";

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> EXTENSOES = Set.of("jpg", "jpeg");
    private static final String MIME_JPEG = "image/jpeg";

    private final Path pasta;

    public ImagemStorageService(@Value("${app.upload.dir:./src/main/resources/static/assets/jogos}") String dir) {
        this.pasta = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de upload: " + pasta, e);
        }
        log.info("Upload de imagens: gravando em {} (URL pública {}/**)", pasta, CAMINHO_PUBLICO);
    }

    public Path getPasta() {
        return pasta;
    }

    /**
     * true se o cliente mandou um arquivo. Part ausente, ou part vazia SEM nome de arquivo
     * (navegador sem arquivo escolhido), conta como "sem imagem". Part vazia COM nome é erro.
     */
    public boolean foiEnviado(MultipartFile arquivo) {
        return arquivo != null && !(arquivo.isEmpty() && !StringUtils.hasText(arquivo.getOriginalFilename()));
    }

    /** Valida, grava com nome UUID e devolve o caminho relativo (ex.: /assets/jogos/<uuid>.jpg). */
    public String salvar(MultipartFile arquivo) {
        validar(arquivo);
        String nome = UUID.randomUUID() + "." + extensao(arquivo.getOriginalFilename());
        Path destino = pasta.resolve(nome);
        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, destino);
        } catch (IOException e) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível gravar a imagem no servidor.", e);
        }
        log.info("Imagem salva: {} ({} bytes, original='{}')", destino, arquivo.getSize(), arquivo.getOriginalFilename());
        return CAMINHO_PUBLICO + "/" + nome;
    }

    /** Apaga o arquivo de um caminho público. Só mexe em .jpg/.jpeg dentro da pasta de upload. */
    public void remover(String caminho) {
        if (caminho == null || !caminho.startsWith(CAMINHO_PUBLICO + "/")) return;
        String nome = caminho.substring(CAMINHO_PUBLICO.length() + 1);
        if (!EXTENSOES.contains(extensao(nome))) return;          // nunca apaga os .svg do seed
        Path alvo = pasta.resolve(nome).normalize();
        if (!alvo.startsWith(pasta)) return;                       // proteção contra ../
        try {
            if (Files.deleteIfExists(alvo)) log.info("Imagem removida: {}", alvo);
        } catch (IOException e) {
            log.warn("Não foi possível remover {}: {}", alvo, e.getMessage());
        }
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "O arquivo enviado está vazio.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem excede o tamanho máximo de 5 MB.");
        }
        if (!EXTENSOES.contains(extensao(arquivo.getOriginalFilename()))) {
            throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Extensão inválida. Envie um arquivo .jpg ou .jpeg.");
        }
        String mime = arquivo.getContentType();
        if (!MIME_JPEG.equalsIgnoreCase(mime)) {
            throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de arquivo inválido (" + (mime == null ? "desconhecido" : mime)
                            + "). Apenas image/jpeg é aceito.");
        }
        if (!temAssinaturaJpeg(arquivo)) {
            throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "O conteúdo do arquivo não é uma imagem JPEG válida.");
        }
    }

    /** Todo JPEG começa com os bytes FF D8 FF. Extensão e Content-Type vêm do cliente e podem ser forjados. */
    private boolean temAssinaturaJpeg(MultipartFile arquivo) {
        try (InputStream in = arquivo.getInputStream()) {
            byte[] h = in.readNBytes(3);
            return h.length == 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        } catch (IOException e) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível ler o arquivo enviado.", e);
        }
    }

    private static String extensao(String nomeOriginal) {
        String ext = StringUtils.getFilenameExtension(StringUtils.cleanPath(nomeOriginal == null ? "" : nomeOriginal));
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }
}