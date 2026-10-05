package com.jcaa.usersmanagement.infrastructure.adapter.persistence.config;

public record DatabaseConfig(
    String host,
    int port,
    String databaseName,
    String username,
    String password,
    String sslMode) {
  private static final String URL_TEMPLATE = "jdbc:mysql://%s:%d/%s?sslMode=%s&serverTimezone=UTC&allowPublicKeyRetrieval=true";

  public String buildJdbcUrl() {
    return String.format(URL_TEMPLATE, host, port, databaseName, sslMode);
  }

  private static final String POSTGRESQL_URL_TEMPLATE = "jdbc:postgresql://%s:%d/%s?sslmode=%s";

  public String buildPostgreSqlJdbcUrl() {
    return String.format(POSTGRESQL_URL_TEMPLATE, host, port, databaseName, sslMode);
  }
}
