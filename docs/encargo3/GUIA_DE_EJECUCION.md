# Ejecución del backend y sus pruebas del Encargo 3

Ejecutar desde la raíz del backend. Se requiere Java 17, PowerShell 7, Docker Desktop iniciado y Python 3.10 o posterior para las pruebas globales. Los scripts Python agregados utilizan únicamente la biblioteca estándar.

## Pruebas y ejecutables

```powershell
.\scripts\Invoke-BackendBaseline.ps1 -Goal verify
```

El ejecutor recorre los nueve módulos, compila desde cero, ejecuta las pruebas, genera JAR y guarda logs, XML, JSON y CSV bajo evidence/encargo3/baseline con un identificador UTC distinto en cada corrida. Un módulo sin reportes válidos no se marca aprobado. Las omisiones se distinguen de las aprobaciones.

Si el entorno resuelve incorrectamente la carpeta de dependencias, se puede indicar la ubicación existente mediante -LocalRepository y el ejecutable Maven mediante -MavenCommand. -Offline permite utilizar únicamente dependencias ya descargadas. No se debe interpretar una falla de permisos o dependencias como una prueba funcional fallida.

## Ambiente QA separado

Primera preparación:

```powershell
python scripts/prepare_qa.py
docker compose --env-file evidence/encargo3/qa.env -f docker-compose.qa.yml up --build -d
```

La primera instrucción crea claves aleatorias y credenciales de administrador de prueba sin mostrarlas. Si el archivo ya existe, el script se detiene y lo conserva para evitar cambiar claves de una base existente. Las imágenes QA se construyen desde los JAR generados con verify; después de cambiar Java es necesario repetir verify y reconstruir los servicios afectados.

El proyecto Docker se llama cordillera-encargo3-qa. Solo publica el Gateway en http://127.0.0.1:18080. Los servicios de dominio y PostgreSQL no publican puertos. Los contenedores Java ejecutan con un usuario sin privilegios de administrador. .dockerignore permite enviar al constructor únicamente el Dockerfile QA y los JAR; excluye credenciales y evidencias.

Las seis bases lógicas están en una instancia PostgreSQL QA dedicada. Comparten un usuario administrador solo para simplificar este laboratorio aislado con datos ficticios. Este diseño no acredita separación de privilegios de una infraestructura productiva ni cifrado en reposo.

## Pruebas globales

```powershell
python qa/test_system.py
```

La suite espera que los servicios estén listos, crea usuarios y datos nuevos identificables y opera por HTTP contra el Gateway. Solo admite localhost o 127.0.0.1 en el puerto 18080; no sigue redirecciones ni utiliza proxies del entorno. No conserva tokens, contraseñas o cuerpos de respuesta en su reporte JSON.

El caso SYS-07 detiene temporalmente únicamente el servicio finanzas de este proyecto QA y lo vuelve a iniciar incluso si falla la verificación. No ejecutar esta suite sobre un entorno compartido; no sustituir su archivo Compose por el de desarrollo.

Para repetir un caso concreto:

```powershell
python qa/test_system.py --case test_08_cantidades_invalidas_no_modifican_stock
```

## Mediciones de carga y recuperación

```powershell
python qa/measure_load.py
python qa/verify_backup.py
```

La carga utiliza GET /clientes: diez solicitudes de calentamiento, cincuenta muestras secuenciales y cien clientes concurrentes con cinco solicitudes cada uno. El umbral de 300 ms se comprueba sobre todas las muestras de carga normal; los percentiles con concurrencia se documentan por separado. El resultado de este endpoint y dataset no acredita todos los CRUD ni carga sostenida.

El respaldo genera dumps de las seis bases QA, los restaura en bases nuevas con nombres aleatorios y compara cantidades y huellas de filas de todas sus tablas públicas. Solo elimina las bases temporales creadas por el propio script cuando la comparación pasa. Los dumps y resultados quedan en evidence/encargo3/backup. Ejecutarlo cuando no haya otra prueba escribiendo datos.

## Configuración de seguridad necesaria

- JWT_SECRET debe contener una clave aleatoria de al menos 32 bytes y tener exactamente el mismo valor en authentication y api-gateway. No existe clave productiva por defecto. La biblioteca rechaza claves HMAC demasiado cortas al iniciar.
- BOOTSTRAP_ADMIN_ENABLED está desactivado por defecto. Para inicializar una base nueva, habilitarlo temporalmente y proporcionar BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_USERNAME y BOOTSTRAP_ADMIN_PASSWORD. Desactivarlo tras crear la cuenta. La inicialización no modifica usuarios existentes.
- Si las credenciales o la clave anteriores se utilizaron fuera de este laboratorio, su retirada del código no las revoca: se requiere rotarlas en ese ambiente. Esta tarea no ha accedido ni cambiado despliegues externos.
- En Render, JWT_SECRET se solicita externamente para los dos servicios. No generar valores independientes: ambos deben compartir la misma clave.
- Conservar qa.env, dumps y logs completos fuera de Git y de la entrega pública. Preparar evidencia seleccionada y anonimizada para el informe. .gitignore excluye evidence, .env, .env.local, .env.qa y caché Python.

## Integración continua y acciones del estudiante

Continuación acordada el 8 de noviembre: el estudiante usa CMD y GitHub Flow. La rama local observada es main. Antes de publicar se trabaja en una rama del encargo y se revisan los cambios. En CMD:

```cmd
cd /d "C:\Users\sapbe\Documents\Calidad y seguridad de Software\grupo-cordillera-backend"
git switch -c encargo3/seguridad-calidad
git status --short
```

Si esa rama ya existe, usar `git switch encargo3/seguridad-calidad`. Crear/cambiar la rama conserva los cambios sin confirmar. Este paso no hace commit, push, merge ni despliegue. Revisar CAMBIOS_REALIZADOS.md y las matrices de requisitos/incidentes antes de preparar el commit. No agregar forzadamente evidence: contiene qa.env, tokens, contraseñas y respaldos. Las capturas del informe se seleccionarán y anonimizarán por separado. El trabajo debe publicarse como rama y PR para obtener una ejecución real del workflow; el vínculo y los nueve resultados serán la evidencia de CI. Aún no se ha realizado esa publicación.

El archivo .github/workflows/backend-quality.yml configura nueve verificaciones independientes para push, pull request y ejecución manual, con Java 17 y reportes conservados durante catorce días. Las acciones externas se fijan por commit. El trabajo conserva los reportes incluso si Maven falla.

Este archivo está preparado localmente; una ejecución local no demuestra una ejecución en GitHub. Para obtener esa evidencia:

1. Revisar el registro de archivos cambiados y los resultados locales.
2. Subir los cambios a la rama acordada del repositorio.
3. Abrir la pestaña Actions y seleccionar Backend quality and regression.
4. Revisar los nueve trabajos y descargar sus reportes; guardar el enlace y la revisión ejecutada.
5. Si se desea impedir integraciones cuando fallen las pruebas, configurar la protección de rama con esos checks obligatorios. El workflow por sí solo no configura esa protección.

No se ha realizado publicación, push, merge ni despliegue automático. SAST y SCA se ejecutaron en nueve módulos y ZAP en un alcance de nueve consultas autenticadas. El detalle y sus límites están en ESCANEOS_SEGURIDAD.md; estos análisis todavía no forman parte del workflow remoto.

## Repetir análisis de seguridad

Para generar cobertura antes de Sonar, ejecutar la regresión con Goal verify. El resultado posterior verificado está en MITIGACIONES_Y_REVERIFICACION.md. `qa/run_sonar.py --project-version <etiqueta>` permite distinguir la revisión en Sonar. `qa/run_dependency_scan.py --reuse-db` conserva el catálogo descargado para una comparación controlada; omitir esa opción en revisiones periódicas para actualizarlo. No ejecutar ZAP mientras SYS-07 detiene finanzas: esperar a que terminen las pruebas globales.

```powershell
docker compose -f docker-compose.security.yml up -d
python qa/run_sonar.py
python qa/run_dependency_scan.py
python qa/run_zap_scan.py
```

Sonar y el inventario de dependencias admiten las opciones `--maven-command` y `--local-repository` para indicar herramientas existentes si el entorno no resuelve las predeterminadas. No requieren editar los POM. Para ZAP debe estar iniciado el laboratorio QA y descargada la imagen oficial; el script identifica y registra su digest. Trivy usa la imagen fijada en su ejecutor.

Los resultados se guardan bajo evidence/encargo3/security. No compartir credentials.json ni el subdirectorio private de ZAP. Usar únicamente los reportes saneados para capturas y entrega.

## Detener el laboratorio

```powershell
docker compose --env-file evidence/encargo3/qa.env -f docker-compose.qa.yml stop
```

La orden detiene únicamente este proyecto y conserva los datos QA. No es necesario borrar volúmenes para detenerlo.
