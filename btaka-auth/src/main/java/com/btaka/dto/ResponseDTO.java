package com.btaka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor
public class ResponseDTO implements Serializable {

    private final boolean success;
    private final String error;
    private final String errorMessage;
    private final int statusCode;
    private final Map<String, Object> dataMap;

    private ResponseDTO(Builder builder) {
        this.success = builder.success;
        this.error = builder.error;
        this.errorMessage = builder.errorMessage;
        this.statusCode = builder.statusCode;
        this.dataMap = Collections.unmodifiableMap(builder.dataMap);
    }

    public class Builder {
        private boolean success;
        private String error;
        private String errorMessage;
        private int statusCode;
        private Map<String, Object> dataMap = new ConcurrentHashMap<>();

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder error(String error) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder set(String key, Object value) {
            this.dataMap.put(key, value);
            return this;
        }

        public ResponseDTO build() {
            return new ResponseDTO(this);
        }
    }

}