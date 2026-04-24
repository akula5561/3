package com.danil.library.signature.impl;

import com.danil.library.signature.CanonicalizationService;
import com.danil.library.signature.SignatureErrorCode;
import com.danil.library.signature.SignatureModuleException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
/** Канонизация JSON в детерминированную строку/байты перед подписью. */
public class Rfc8785CanonicalizationService implements CanonicalizationService {

    private final ObjectMapper serializer;
    private final ObjectMapper strictParser;

    public Rfc8785CanonicalizationService(ObjectMapper serializer) {
        this.serializer = serializer;
        JsonFactory jsonFactory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        this.strictParser = new ObjectMapper(jsonFactory);
    }

    @Override
    public byte[] canonicalize(Object payload) {
        try {
            // 1) Сериализуем payload в JSON.
            String rawJson = serializer.writeValueAsString(payload);
            // 2) Парсим со строгой проверкой дубликатов полей.
            JsonNode root = strictParser.readTree(rawJson);
            // 3) Собираем канонический JSON (сортировка полей, без лишних пробелов).
            StringBuilder sb = new StringBuilder(rawJson.length());
            appendCanonicalJson(root, sb);
            // 4) Для криптографии фиксируем UTF-8 байты.
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SignatureModuleException(
                    SignatureErrorCode.CANONICALIZATION_ERROR,
                    "Failed to canonicalize payload",
                    e
            );
        }
    }

    private void appendCanonicalJson(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) {
            out.append("null");
            return;
        }

        if (node.isObject()) {
            out.append('{');
            List<String> names = new ArrayList<>();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                names.add(fieldNames.next());
            }
            // Ключевой момент детерминизма: одинаковый порядок полей.
            Collections.sort(names);

            boolean first = true;
            for (String name : names) {
                if (!first) {
                    out.append(',');
                }
                appendQuotedString(name, out);
                out.append(':');
                appendCanonicalJson(node.get(name), out);
                first = false;
            }
            out.append('}');
            return;
        }

        if (node.isArray()) {
            out.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                appendCanonicalJson(node.get(i), out);
            }
            out.append(']');
            return;
        }

        if (node.isTextual()) {
            appendQuotedString(node.textValue(), out);
            return;
        }

        if (node.isNumber()) {
            if (node.isFloatingPointNumber()) {
                double value = node.doubleValue();
                // RFC-подход: NaN/Infinity недопустимы для подписи.
                if (!Double.isFinite(value)) {
                    throw new SignatureModuleException(
                            SignatureErrorCode.CANONICALIZATION_ERROR,
                            "NaN and Infinity are not allowed in canonical JSON"
                    );
                }
            }
            out.append(node.asText());
            return;
        }

        if (node.isBoolean()) {
            out.append(node.booleanValue() ? "true" : "false");
            return;
        }

        throw new SignatureModuleException(
                SignatureErrorCode.CANONICALIZATION_ERROR,
                "Unsupported JSON node type: " + node.getNodeType()
        );
    }

    private void appendQuotedString(String value, StringBuilder out) {
        // Экранирование строк по правилам JSON, чтобы байтовое представление было стабильным.
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\f' -> out.append("\\f");
                case '\r' -> out.append("\\r");
                default -> {
                    if (c <= 0x1F) {
                        out.append("\\u");
                        out.append(Character.forDigit((c >> 12) & 0xF, 16));
                        out.append(Character.forDigit((c >> 8) & 0xF, 16));
                        out.append(Character.forDigit((c >> 4) & 0xF, 16));
                        out.append(Character.forDigit(c & 0xF, 16));
                    } else if (Character.isSurrogate(c)) {
                        if (Character.isHighSurrogate(c)
                                && i + 1 < value.length()
                                && Character.isLowSurrogate(value.charAt(i + 1))) {
                            out.append(c);
                            out.append(value.charAt(i + 1));
                            i++;
                        } else {
                            throw new SignatureModuleException(
                                    SignatureErrorCode.CANONICALIZATION_ERROR,
                                    "Lone surrogate is not allowed in canonical JSON"
                            );
                        }
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
