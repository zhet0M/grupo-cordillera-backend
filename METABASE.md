# Metabase Local

## Levantar

```bash
docker compose -f docker-compose.metabase.yml up -d
```

## Acceso

- URL: `http://localhost:3000`
- Crea tu usuario inicial en el wizard de Metabase.

## Uso con el frontend

1. Conecta una base de datos del proyecto en Metabase.
2. Crea un dashboard con los KPIs que quieras mostrar.
3. Publica el dashboard o genera un embed público.
4. Copia la URL y reemplaza `REEMPLAZAR` en:

```ts
src/app/core/config/metabase.config.ts
```

La landing mostrará ese panel embebido en vez de la demo CSS.
