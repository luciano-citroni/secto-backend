package com.bridge.secto.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
@Profile("!docker") // Não executa no profile docker (container usa variáveis de ambiente)
public class DotenvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".") // Diretório raiz do projeto
                    .filename(".env")
                    .ignoreIfMissing() // Não falha se .env não existir
                    .load();

            // Carrega todas as variáveis do .env como propriedades do sistema
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Só define se a variável ainda não foi definida
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    System.out.println("Loaded from .env: " + key);
                }
            });
            
        } catch (Exception e) {
            System.out.println("Could not load .env file: " + e.getMessage());
        }
    }
}