package br.com.fiap.ms_notificacao.service;

import br.com.fiap.ms_notificacao.dto.ConsultaNotificacaoDTO;

public interface NotificacaoLogger {

    void print(ConsultaNotificacaoDTO dto);
}