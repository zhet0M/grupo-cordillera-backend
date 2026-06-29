
# 🏔️ Plataforma de Monitoreo Ejecutivo - Backend

<div align="center">
  <p>
    <strong>Backend empresarial basado en microservicios para la gestión integral de Grupo Cordillera.</strong>
  </p>
  <p>
    <img alt="Java 17" src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=oracle">
    <img alt="Spring Boot 4.0.6" src="https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=spring">
    <img alt="PostgreSQL 15" src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql">
    <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
  </p>
</div>

---

## 📋 Descripción

El **Backend Grupo Cordillera** es un sistema de gestión empresarial diseñado con una arquitectura de microservicios robusta y escalable. Proporciona una plataforma centralizada para manejar operaciones críticas como:

- Autenticación y autorización de usuarios
- Gestión de clientes
- Control de finanzas y movimientos
- Administración de inventario
- Procesamiento de ventas
- Alertas operativas
- Cálculo de KPIs
- Generación de reportes

Este proyecto se integra perfectamente con el **Frontend Grupo Cordillera** para ofrecer una experiencia de usuario completa.

---

## ✨ Características Principales

| Característica | Descripción |
|----------------|-------------|
| 🔐 **Autenticación Segura** | Servicio dedicado con JWT y roles de usuario |
| 🌐 **API Gateway** | Punto de entrada unificado y filtro de seguridad |
| 👥 **Gestión de Clientes** | CRUD completo, compras y seguimiento de clientes |
| 💰 **Módulo Financiero** | Control de ingresos, costos, margen y reportes |
| 📦 **Sistema de Inventario** | Seguimiento de productos, stock y SKUs por categoría |
| 🛒 **Módulo de Ventas** | Procesamiento de ventas por canal y sincronización |
| 🚨 **Alertas Operativas** | Detección y seguimiento de eventos críticos |
| 📊 **KPIs y Reportes** | Cálculo de métricas y generación de reportes PDF |
| 🐳 **Contenerización** | Despliegue completo con Docker y Docker Compose |
| 📚 **Documentación** | Swagger UI automático para todos los endpoints |

---

## 🛠️ Stack Tecnológico

### Lenguajes y Frameworks
- **Java 17** ☕
- **Spring Boot 4.0.6** 🌱
- **Spring Cloud Gateway 2025.1.1** ☁️
- **Spring Cloud OpenFeign** 📡
- **Spring Data JPA** 🗃️
- **Spring Security + JJWT** 🔐
- **Lombok** 🔧
- **SpringDoc OpenAPI 2.8.8** 📄

### Bases de Datos
- **PostgreSQL 15** 🐘

### Testing y Calidad
- **JUnit 5** 🧪
- **Mockito** 🎭
- **JaCoCo** 📊
- **SonarQube** 🔍

### Contenerización
- **Docker** 🐳
- **Docker Compose** 🐙

### Business Intelligence
- **Metabase** 📈

---

## 📦 Microservicios y Puertos

| Microservicio | Puerto Local | Contenedor Docker | Descripción |
|---------------|--------------|-------------------|-------------|
| `api-gateway` | 8080 | `api-gateway` | Punto de entrada y proxy de todas las APIs |
| `authentication` | 8081 | `authentication` | Autenticación, roles y gestión de usuarios |
| `ventas` | 8082 | `ventas` | Procesamiento y seguimiento de ventas |
| `inventario` | 8083 | `inventario` | Control de stock y productos |
| `finanzas` | 8084 | `finanzas` | Movimientos financieros y reportes |
| `clientes` | 8085 | `clientes` | Gestión de clientes y seguimiento de compras |
| `kpis` | 8086 | `ms-kpis` | Cálculo y agrupación de KPIs |
| `alertas` | 8087 | `alertas` | Generación y seguimiento de alertas |
| `reportes` | 8088 | `reportes` | Generación de reportes PDF |

---

## 🎨 Patrones de Diseño Implementados

| Patrón | Uso | Microservicio |
|--------|-----|---------------|
| **Factory Method** | Creación de productos por categoría y calculadoras de KPIs | `inventario`, `kpis` |
| **Strategy Pattern** | Lógica variable para el cálculo de KPIs | `kpis` |
| **Repository Pattern** | Abstracción de acceso a datos | *Todos los microservicios* |
| **Circuit Breaker** | Resiliencia ante fallos en comunicaciones entre servicios | `kpis` |
| **DTO Pattern** | Transferencia de datos entre capas y servicios | *Todos los microservicios* |

---

## 🚀 Instalación y Ejecución

### 📋 Pre-requisitos

Antes de comenzar, asegúrate de tener instalados:

- ☕ **Java 17** o superior
- 📦 **Maven 3.6+**
- 🐳 **Docker** y **Docker Compose**
- 💾 **Git**

---

### 1. Levantar Todo con Docker Compose (Recomendado)

Este comando levanta **todos los microservicios, bases de datos, SonarQube y Metabase** de una sola vez:

```bash
docker compose -f docker-compose.yml -f docker-compose.sonarqube.yml -f docker-compose.metabase.yml up -d
```

---

### 2. Ejecutar Microservicios Individualmente

Si prefieres ejecutar servicios de forma local:

```bash
# Primero navega al directorio del microservicio
cd authentication

# Instala dependencias y ejecuta
mvn clean install
mvn spring-boot:run
```

---

## 📚 Documentación de APIs (Swagger UI)

Una vez que los servicios estén corriendo, puedes acceder a la documentación interactiva de Swagger UI:

- **API Gateway (Todas las APIs)**: http://localhost:8080/swagger-ui.html
- **Authentication**: http://localhost:8081/swagger-ui.html
- **Ventas**: http://localhost:8082/swagger-ui.html
- **Inventario**: http://localhost:8083/swagger-ui.html
- **Finanzas**: http://localhost:8084/swagger-ui.html
- **Clientes**: http://localhost:8085/swagger-ui.html
- **KPIs**: http://localhost:8086/swagger-ui.html
- **Alertas**: http://localhost:8087/swagger-ui.html
- **Reportes**: http://localhost:8088/swagger-ui.html

---

## 🧪 Pruebas y Cobertura

### Ejecutar Pruebas Unitarias

Para ejecutar las pruebas de todos los microservicios:

```bash
mvn clean test
```

Para ejecutar pruebas de un microservicio específico:

```bash
cd authentication
mvn clean test
```

### Generar Reporte de Cobertura (JaCoCo)

```bash
mvn clean verify
```

El reporte HTML se genera en `[microservicio]/target/site/jacoco/index.html`.

---

## 🔎 SonarQube Local

Para análisis estático de código y cobertura:

### 1. Levantar SonarQube

```bash
docker compose -f docker-compose.sonarqube.yml up -d
```

### 2. Analizar un Microservicio

```bash
cd [nombre-del-microservicio]
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=TU_TOKEN \
  -Dsonar.projectKey=TU_CLAVE_UNICA
```

Las credenciales por defecto son:
- **Usuario**: `admin`
- **Contraseña**: `admin` (se te pedirá cambiarla en el primer inicio)

---

## 📊 Metabase Local

Para Business Intelligence y dashboards:

```bash
docker compose -f docker-compose.metabase.yml up -d
```

Revisa el archivo `METABASE.md` para configurar el embed visible en el frontend.

Las credenciales por defecto son:
- **URL**: http://localhost:3000
- **Usuario**: `admin@example.com`
- **Contraseña**: `admin123`

---

## 📝 Ejemplo de Uso Rápido

### 1. Iniciar Sesión (Obtener Token JWT)

```bash
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "admin@grupocordillera.com",
  "password": "admin123"
}
```

### 2. Consultar Clientes

```bash
GET http://localhost:8080/clientes
Authorization: Bearer [TU_TOKEN_JWT]
```

---

## 📂 Estructura del Proyecto

```
grupo-cordillera-backend/
├── api-gateway/          # Punto de entrada y proxy
├── authentication/       # Autenticación y usuarios
├── clientes/             # Gestión de clientes
├── finanzas/             # Control financiero
├── inventario/           # Control de stock
├── ventas/               # Procesamiento de ventas
├── kpis/                 # Cálculo de métricas
├── alertas/              # Alertas operativas
├── reportes/             # Reportes PDF
├── docker-compose.yml    # Orquestación principal
├── METABASE.md           # Guía de Metabase
└── SONARQUBE.md          # Guía de SonarQube
```

---

## 👥 Equipo

Proyecto desarrollado por el **Equipo Grupo Cordillera**.

---

## 📄 Licencia

Este proyecto es propiedad exclusiva del Grupo Cordillera.
