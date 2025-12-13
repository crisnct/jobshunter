open module com.jobshunter.application {
  requires spring.boot;
  requires spring.boot.autoconfigure;
  requires spring.context;
  requires spring.beans;
  requires spring.core;
  requires spring.expression;
  requires spring.web;
  requires spring.webmvc;
  requires spring.aop;

  requires spring.security.config;
  requires spring.security.web;
  requires spring.security.core;
  requires spring.security.crypto;

  requires spring.data.jpa;
  requires spring.data.commons;
  requires spring.orm;
  requires spring.tx;
  requires spring.jdbc;
  requires spring.context.support;

  requires org.hibernate.orm.core;
  requires org.hibernate.validator;

  requires jakarta.validation;
  requires jakarta.annotation;
  requires jakarta.persistence;
  requires jakarta.transaction;
  requires jakarta.servlet;
  requires jakarta.mail;
  requires jakarta.activation;
  requires jakarta.cdi;
  requires jakarta.el;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.fasterxml.jackson.dataformat.xml;

  requires org.apache.logging.log4j;
  requires org.apache.commons.lang3;

  requires org.jspecify;
  requires org.slf4j;
  requires liquibase.core;
  requires twilio;
  requires com.auth0.jwt;
  requires jjwt.api;
  requires java.sql;

  requires static lombok;
  requires static com.jobshunter.processor;

}
