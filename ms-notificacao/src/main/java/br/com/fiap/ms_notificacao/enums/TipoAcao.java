package br.com.fiap.ms_notificacao.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoAcao {
    CRIADA,
    EDITADA;

    @JsonCreator
    public static TipoAcao fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (TipoAcao tipoAcao : values()) {
            if (tipoAcao.name().equalsIgnoreCase(value.trim())) {
                return tipoAcao;
            }
        }

        throw new IllegalArgumentException("Valor inválido para tipoAcao: " + value);
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
