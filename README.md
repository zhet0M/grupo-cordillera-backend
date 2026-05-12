# 🚀 Cordillera Backend

## 📋 Descripción

El **GrupoCordillera Backend** es un sistema de gestión empresarial basado en arquitectura de microservicios. Proporciona una plataforma robusta y escalable para manejar operaciones críticas como autenticación de usuarios, gestión de clientes, finanzas, inventario y ventas. Diseñado con Spring Boot y Spring Cloud, este backend asegura alta disponibilidad, seguridad y facilidad de mantenimiento.

Este proyecto forma parte del ecosistema Cordillera FullStack, integrándose perfectamente con el frontend para ofrecer una experiencia completa de gestión empresarial.

## ✨ Características

- 🔐 **Autenticación Segura**: Servicio dedicado para manejo de usuarios y tokens JWT
- 🌐 **API Gateway**: Punto de entrada unificado para todos los microservicios
- 👥 **Gestión de Clientes**: CRUD completo para información de clientes
- 💰 **Módulo Financiero**: Control de transacciones y reportes financieros
- 📦 **Sistema de Inventario**: Seguimiento de productos y stock en tiempo real
- 🛒 **Módulo de Ventas**: Procesamiento de pedidos y facturación
- 🐳 **Contenedorización**: Despliegue completo con Docker y Docker Compose
- 🔄 **Arquitectura de Microservicios**: Escalabilidad y mantenibilidad óptimas

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: Java 17 ☕
- **Framework**: Spring Boot 4.0.6 🌱
- **Microservicios**: Spring Cloud Gateway 2025.1.1 ☁️
- **Base de Datos**: PostgreSQL 15 🐘
- **Contenedorización**: Docker & Docker Compose 🐳
- **Build Tool**: Maven 📦
- **Utilidades**: Lombok, JJWT 🔧

## 📋 Pre-requisitos

Antes de comenzar, asegúrate de tener instalados:

- ☕ **Java 17** o superior
- 📦 **Maven 3.6+**
- 🐳 **Docker** y **Docker Compose**
- 💾 **Git** para clonar el repositorio

## 🚀 Instalación

1. **Clona el repositorio**:
   ```bash
   git clone https://github.com/tu-usuario/cordillera-backend.git
   cd cordillera-backend
   ```

2. **Instala dependencias**:
   ```bash
   mvn clean install
   ```

3. **Construye las imágenes Docker** (opcional, si no usas docker-compose directamente):
   ```bash
   docker-compose build
   ```

## 🎯 Uso

### Ejecutar con Docker Compose (Recomendado)

Para levantar todos los microservicios y bases de datos:

```bash
docker-compose up -d
```

Los servicios estarán disponibles en:
- 🌐 **API Gateway**: http://localhost:8080
- 🔐 **Autenticación**: http://localhost:8081
- 👥 **Clientes**: http://localhost:8082
- 💰 **Finanzas**: http://localhost:8083
- 📦 **Inventario**: http://localhost:8084
- 🛒 **Ventas**: http://localhost:8085

### Ejecutar Individualmente

Para ejecutar un servicio específico:

```bash
cd [nombre-del-servicio]
mvn spring-boot:run
```

### Ejemplo de Uso

1. **Autenticación**:
   ```bash
   POST http://localhost:8081/auth/login
   Content-Type: application/json

   {
     "username": "admin",
     "password": "password"
   }
   ```

2. **Obtener Clientes**:
   ```bash
   GET http://localhost:8080/clientes
   Authorization: Bearer [tu-token-jwt]
   ```