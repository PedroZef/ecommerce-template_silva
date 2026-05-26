CREATE DATABASE IF NOT EXISTS ecommerce_db;
USE ecommerce_db;

CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

INSERT INTO categoria (nome) VALUES ('Eletrônicos'), ('Roupas'), ('Livros');
