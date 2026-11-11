# Guía de Despliegue en Render (Spring Boot 3 + PostgreSQL)

Instrucciones para el despliegue del backend en la nube utilizando Render y Docker.

---

## 1. Requisitos Previos

- Cuenta activa en [Render](https://render.com).
- Repositorio del backend sincronizado en GitHub.

---

## 2. Base de Datos PostgreSQL

1. Crear un servicio de base de datos **PostgreSQL** en Render:
   - Nombre: `vehiculos-db`
   - Database: `vehiculos_db`
   - User: `postgres`
   - Plan: `Free`
2. Copiar la **Internal Database URL** o los parámetros individuales de conexión.

---

## 3. Web Service (Docker)

1. Crear un nuevo **Web Service** conectado al repositorio.
2. Configurar entorno **Docker**.
3. Definir las variables de entorno requeridas:
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<HOST>:<PORT>/vehiculos_db`
   - `SPRING_DATASOURCE_USERNAME`: `<DB_USER>`
   - `SPRING_DATASOURCE_PASSWORD`: `<DB_PASS>`
   - `JWT_SECRET`: `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
   - `AI_API_KEY`: `demo-key`
   - `JPA_DDL_AUTO`: `update`
4. Desplegar y copiar la URL pública asignada.

