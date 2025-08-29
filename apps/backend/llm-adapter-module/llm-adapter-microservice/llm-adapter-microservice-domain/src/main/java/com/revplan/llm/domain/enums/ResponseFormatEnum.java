package com.revplan.llm.domain.enums;

public enum ResponseFormatEnum {
    JSON_SCHEMA("json_schema");

    private final String value;
    ResponseFormatEnum(String value) { this.value = value; }
    public String getValue() { return value; }
    @Override public String toString() { return value; }
}
