// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/config/WebConfig.java
package br.com.gamestore.pi_3.config;

import br.com.gamestore.pi_3.security.AuthInterceptor;
import br.com.gamestore.pi_3.service.ImagemStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImagemStorageService imagens;
    private final AuthInterceptor authInterceptor;

    public WebConfig(ImagemStorageService imagens, AuthInterceptor authInterceptor) {
        this.imagens = imagens;
        this.authInterceptor = authInterceptor;
    }

    /** Protege /api/admin/** (somente ADMIN) e /api/pedidos/** (usuário autenticado). O resto segue público. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/admin/**", "/api/pedidos/**");
    }

    /** CORS global: libera só a origem do Angular (ng serve) para /api/** (inclui /api/assets/**). */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /**
     * Serve as capas em /assets/jogos/** e /api/assets/jogos/** (esta passa pelo proxy do ng serve,
     * então o Angular usa environment.apiUrl + caminho). Lê direto da pasta em disco, sem rebuild.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pastaFisica = imagens.getPasta().toUri().toString();
        if (!pastaFisica.endsWith("/")) pastaFisica += "/";

        registry.addResourceHandler(
                        ImagemStorageService.CAMINHO_PUBLICO + "/**",
                        "/api" + ImagemStorageService.CAMINHO_PUBLICO + "/**")
                .addResourceLocations(
                        pastaFisica,
                        "classpath:/static" + ImagemStorageService.CAMINHO_PUBLICO + "/");
    }
}