package br.com.fiap.ms_notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class MsNotificacaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsNotificacaoApplication.class, args);
	}

}
