package br.com.payments.events.support;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuracao compartilhada do modulo events (scheduling do relay do outbox e
 * repositorios/entidades em br.com.payments). As classes deste modulo sao varridas
 * via scanBasePackages = "br.com.payments".
 */
@Configuration
@EnableScheduling
@EnableJpaRepositories(basePackages = "br.com.payments")
@EntityScan(basePackages = "br.com.payments")
public class EventsAutoConfiguration {
}