package br.com.gamestore.pi_3.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * (OPCIONAL) Upload de capa. Grava em ./uploads e devolve { "url": "/uploads/<arquivo>" }.
 * Essa url é o que você salva no campo "imagem" do jogo.
 */
@RestController
@RequestMapping("/api/admin")
public class UploadController {

    private static final Set<String> EXTENSOES = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final Path PASTA = Path.of("uploads");

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        String original = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        int ponto = original.lastIndexOf('.');
        String ext = ponto >= 0 ? original.substring(ponto + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSOES.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem não permitido");
        }
        Files.createDirectories(PASTA);
        // Nome gerado por nós (UUID): evita sobrescrever arquivos e ataques via nome (../)
        String nome = UUID.randomUUID() + "." + ext;
        Files.copy(arquivo.getInputStream(), PASTA.resolve(nome));
        return Map.of("url", "/uploads/" + nome);
    }
}
