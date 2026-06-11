# SonarQube Local

## Levantar SonarQube

```bash
docker compose -f docker-compose.sonarqube.yml up -d
```

## Acceso

- URL: `http://localhost:9000`
- Usuario: `admin`
- Password inicial: `admin`

## Análisis por microservicio

Ejecuta esto dentro de cada carpeta del microservicio:

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN -Dsonar.projectKey=TU_CLAVE_UNICA
```

## Microservicios sugeridos

- `inventario`
- `ventas`
- `authentication`
- `finanzas`

## Nota

El reporte de cobertura de JaCoCo se genera durante `verify` y SonarQube lo puede consumir automáticamente desde cada módulo.
