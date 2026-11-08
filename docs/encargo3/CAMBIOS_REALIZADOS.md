# Registro de cambios del Encargo 3

Fecha: 6 de noviembre de 2026. Los cambios están en el directorio de trabajo y no se han publicado ni confirmado mediante commit. No se eliminaron archivos originales ni se modificó el frontend.

## Código de producción

| Archivo | Modificación | Elementos quitados o sustituidos |
| --- | --- | --- |
| api-gateway/src/main/java/com/grupocordillera/api_gateway/filter/JwtAuthFilter.java | Agregadas listas de roles y dominios, comparación por segmentos, rechazo de sujeto o rol inválido, captura de errores JWT y denegación por defecto. | Sustituidos los prefijos ambiguos para rutas públicas y dominios y el permiso automático para rutas desconocidas. |
| api-gateway/src/main/java/com/grupocordillera/api_gateway/config/JwtService.java | Agregada validación de clave al iniciar y codificación UTF-8 explícita. | Sustituida la codificación dependiente de la plataforma. |
| authentication/src/main/java/com/autenticacion/authentication/service/JwtService.java | Agregada validación de clave al iniciar y codificación UTF-8 explícita. | Sustituida la codificación dependiente de la plataforma. |
| authentication/src/main/java/com/autenticacion/authentication/config/DataInitializer.java | Inicialización condicional, desactivada por defecto; usuario y contraseña obtenidos de configuración, con validación de campos requeridos. Conserva el hash y no sobrescribe usuarios existentes. | Quitadas las credenciales administrativas fijas y el mensaje por System.out. |
| clientes/src/main/java/com/grupocordillera/clientes/exception/ClienteNotFoundException.java | Archivo nuevo: excepción de recurso inexistente. | Ninguno. |
| clientes/src/main/java/com/grupocordillera/clientes/service/ClientesService.java | obtenerPorId utiliza la excepción específica. | Sustituida la RuntimeException genérica de ese caso. |
| clientes/src/main/java/com/grupocordillera/clientes/config/GlobalExceptionHandler.java | Agregada traducción de recurso inexistente a 404 y mensaje genérico para errores inesperados. | Quitada la exposición directa del mensaje técnico de RuntimeException en respuestas 500. |
| inventario/src/main/java/com/grupocordillera/inventario/service/InventarioService.java | Rechaza descuentos con cantidad cero o negativa antes de consultar o guardar datos. | Eliminada la aceptación de esas cantidades en el descuento. |
| ventas/src/main/java/com/grupocordillera/ventas/service/VentasService.java | Rechaza cantidad nula, cero o negativa antes de llamar a otros servicios. | Eliminada la posibilidad de continuar ese flujo con cantidades inválidas. |

## Configuración de ejecución

| Archivo | Modificación | Elementos quitados o sustituidos |
| --- | --- | --- |
| api-gateway/src/main/resources/application.properties | JWT_SECRET externo obligatorio. | Quitada la clave JWT de respaldo fija. |
| authentication/src/main/resources/application.properties | JWT_SECRET externo y variables de inicialización administrativa. | Quitada la clave JWT fija. |
| api-gateway/docker-compose.yml | Pasa la clave externa a autenticación y Gateway y las opciones de inicialización a autenticación. | Ningún servicio original eliminado. |
| authentication/docker-compose.yml | Pasa la clave y opciones administrativas externas. | Ningún servicio original eliminado. |
| render.yaml | Los dos JWT_SECRET se solicitan externamente mediante sync: false. | Quitados los dos valores JWT fijos. Requiere configurar el mismo valor en ambos servicios. |
| docker-compose.qa.yml | Archivo nuevo: diez contenedores aislados, seis bases lógicas, claves externas, Gateway en localhost:18080 y red interna para dominios y datos. | Ninguno. |
| Dockerfile.qa | Archivo nuevo: imagen de ejecución desde el JAR ya verificado, con usuario 10001. | Ninguno. |
| .dockerignore | Archivo nuevo: permite enviar únicamente el Dockerfile QA y JAR requeridos al constructor. | Excluye del contexto fuentes, secretos y evidencias. |
| qa/init-databases.sql | Archivo nuevo: crea las seis bases del laboratorio. | Ninguno. |
| .gitignore | Excluye evidence, archivos .env locales y caché Python. La exclusión de evidence se había iniciado en el bloque anterior. | Ninguna regla original retirada. |

## Pruebas y automatización

| Archivo | Modificación | Elementos quitados o sustituidos |
| --- | --- | --- |
| api-gateway/src/test/java/com/grupocordillera/api_gateway/ApiGatewayApplicationTests.java | Clave efímera exclusiva del contexto de pruebas, manteniendo la configuración principal. | Ya no depende de una clave productiva por defecto. |
| api-gateway/src/test/java/com/grupocordillera/api_gateway/filter/JwtAuthFilterTest.java | Actualizados dos casos para exigir rechazo de rol ausente y rutas desconocidas. | Sustituidas aserciones que esperaban el comportamiento inseguro. No se quitaron casos de la suite. |
| api-gateway/src/test/java/com/grupocordillera/api_gateway/filter/JwtAuthFilterSecurityTest.java | Archivo nuevo: 33 casos de rol, sujeto, rutas y errores del parser; se conservó evidencia anterior y posterior. | Ninguno. |
| api-gateway/src/test/java/com/grupocordillera/api_gateway/filter/GatewayJwtSecurityTest.java | Archivo nuevo: 11 casos con firmas JWT reales, expiración, claims incorrectos y clave débil. | Ninguno. |
| authentication/src/test/java/com/autenticacion/authentication/config/DataInitializerTest.java | Archivo nuevo: 6 casos de configuración, inicialización desactivada, conservación de usuario y BCrypt real. | Ninguno. |
| clientes/src/test/java/com/grupocordillera/clientes/service/ClientesServiceTest.java | Exige la excepción específica y actualiza el comentario del incidente corregido. | Quitada la descripción obsoleta que afirmaba que no se corregía el error 500. |
| clientes/src/test/java/com/grupocordillera/clientes/config/GlobalExceptionHandlerTest.java | Archivo nuevo: 2 pruebas HTTP de 404 y ausencia de mensaje técnico en 500. | Ninguno. |
| inventario/src/test/java/com/grupocordillera/inventario/service/CantidadStockTest.java | Archivo nuevo: 3 casos que verifican rechazo sin acceso a persistencia. | Ninguno. |
| ventas/src/test/java/com/grupocordillera/ventas/service/CantidadVentaTest.java | Archivo nuevo: 4 casos que verifican rechazo sin llamadas a dependencias. | Ninguno. |
| scripts/Invoke-BackendBaseline.ps1 | Archivo iniciado previamente y corregido en este bloque: reportes con identificador único, compilación desde cero, resúmenes y opción verify para generar JAR. | Sustituida la propiedad reportsDirectory que Surefire ignoraba por reportNameSuffix y copia de reportes actuales. |
| scripts/prepare_qa.py | Archivo nuevo: genera credenciales QA aleatorias sin imprimirlas y conserva un archivo ya existente. | Ninguno. |
| qa/test_system.py | Archivo nuevo: 8 casos HTTP trazados a requisitos, efectos persistidos, roles reales, recuperación financiera y cantidades inválidas. Admite reverificación individual. | El dato inicial de venta POS se corrigió a ECOMMERCE para el flujo que exige actualización de compras. La diferencia de especificación permanece documentada. |
| qa/measure_load.py | Archivo nuevo: 50 muestras normales y 500 solicitudes con 100 clientes, métricas y criterios separados. | Ninguno. |
| qa/verify_backup.py | Archivo nuevo: respaldo y restauración real de las seis bases QA con comparación de datos. | Solo elimina sus propias bases temporales cuando la comparación pasa; conserva los dumps. |
| .github/workflows/backend-quality.yml | Archivo nuevo: nueve trabajos de regresión Java, acciones fijadas por commit y conservación de reportes. | Ninguno. Todavía no ejecutado en GitHub. |

## Documentación

| Archivo | Estado y contenido |
| --- | --- |
| docs/encargo3/PLAN_ENCARGO3.md | Creado en el bloque de planificación; en este bloque se agrega el estado de avance. |
| docs/encargo3/REVISION_REGLAS_EXTERNAS.md | Creado en el bloque anterior; conservado como referencia revisada, sin modificaciones adicionales. |
| docs/encargo3/GUIA_DE_EJECUCION.md | Nuevo: comandos, configuración necesaria, activación de CI, límites y detención segura del laboratorio. |
| docs/encargo3/MANTENIMIENTO_Y_CONTROLES.md | Nuevo: diagramas, relación control-requisito-evidencia y análisis del mantenimiento. |
| docs/encargo3/AVANCE_Y_EVIDENCIAS.md | Nuevo: resultados consolidados, comparación anterior y posterior y pendientes. |
| docs/encargo3/CAMBIOS_REALIZADOS.md | Este registro, con detalle por archivo. |

## Verificación

La última corrida de los nueve módulos ejecutó 303 pruebas: 303 aprobadas, cero fallos, cero errores y cero omisiones. La suite HTTP ejecutó 8 métodos de prueba y subcasos por rol y recurso, todos aprobados. La carga y los respaldos tienen reportes separados y límites explícitos en el documento de evidencias.

No se afirma que los 21 incidentes heredados estén cerrados ni que el encargo completo esté terminado. La revisión completa de aplicabilidad, la trazabilidad, la ejecución remota de CI y el informe final continúan pendientes.

## Bloque de análisis de seguridad del 6 de noviembre de 2026

| Archivo | Qué se agregó o modificó | Qué se quitó |
| --- | --- | --- |
| docker-compose.security.yml | Nuevo servidor SonarQube de laboratorio separado, imagen fijada por digest y puerto solo local 9001. | Nada de la infraestructura original. |
| qa/run_sonar.py | Nuevo ejecutor para nueve módulos, credenciales locales, procesamiento de tareas y exportación de métricas, issues y hotspots. | Ningún código Java. |
| qa/run_dependency_scan.py | Nuevo inventario CycloneDX y análisis Trivy por módulo, con versiones de herramientas fijadas y resúmenes. | No cambia las versiones de bibliotecas ni los POM. |
| qa/run_zap_scan.py | Nuevo escaneo autenticado de nueve GET QA, comprobación previa de acceso, tiempo acotado y saneamiento de credenciales en reportes. | No elimina ni silencia alertas. |
| docs/encargo3/ESCANEOS_SEGURIDAD.md | Nuevo informe técnico de alcance, resultados, clasificación inicial, alternativas y pasos manuales. | Nada. |
| docs/encargo3/AVANCE_Y_EVIDENCIAS.md | Actualizado el estado de escaneos y enlace al análisis. | Sustituida la indicación de que seguían sin ejecutarse. |
| docs/encargo3/GUIA_DE_EJECUCION.md | Agregada la repetición de escaneos y su alcance. | Sustituida la indicación de ejecución pendiente. |
| docs/encargo3/PLAN_ENCARGO3.md | Actualizado el avance y los pendientes de revisión y mitigación. | Sustituido el estado anterior de escaneos no ejecutados. |
| docs/encargo3/MANTENIMIENTO_Y_CONTROLES.md | Actualizado el diagrama y distinguida la ejecución local de la integración en CI. | Sustituida la marca de escaneos sin ejecutar. |

Se ejecutaron las herramientas y se verificó la sintaxis de los scripts. No se repitió la regresión Java porque este bloque no modificó producción, tests Java o dependencias del producto. Los resultados de 303 pruebas y ocho casos globales corresponden al bloque anterior, no se presentan como corridas nuevas.
## Bloque de mitigaciones y reverificación — 6 de noviembre de 2026

Este bloque es posterior a los registros 303/8 que se conservan más abajo. Resultado actual: 311 pruebas Java y diez métodos globales aprobados. Detalle de análisis y limitaciones en MITIGACIONES_Y_REVERIFICACION.md. No se modificó el frontend.

| Archivo | Qué se agregó o modificó | Qué se quitó o sustituyó |
| --- | --- | --- |
| api-gateway/src/main/java/com/grupocordillera/api_gateway/filter/SecurityHeadersFilter.java | Nuevo filtro: nosniff, CORP same-origin y Cache-Control no-store. | Nada. |
| api-gateway/src/main/java/com/grupocordillera/api_gateway/config/GatewayConfig.java | Registro del filtro de cabeceras en orden 0, antes de CORS y JWT. | Nada de la política CORS existente. |
| alertas/src/main/java/com/grupocordillera/alertas/controller/AlertaController.java | Ajustados imports y dependencias del constructor al retirar diagnóstico. | Endpoint /alertas/test-inventario, dependencia InventarioClient y salida de depuración asociada. |
| alertas/src/main/java/com/grupocordillera/alertas/service/DeteccionAlertasService.java | Logging SLF4J con operación y clase de excepción en tres capturas de error. | System.out, System.err, printStackTrace y logs de payload. Se conserva la lógica de negocio. |
| api-gateway/src/test/java/com/grupocordillera/api_gateway/filter/SecurityHeadersFilterTest.java | Nuevo: seis casos de cabeceras, respuestas exitosas/rechazadas y CORS permitido/denegado. | Nada. |
| alertas/src/test/java/com/grupocordillera/alertas/controller/AlertaControllerSecurityTest.java | Nuevo: endpoint retirado devuelve 404. | Nada. |
| alertas/src/test/java/com/grupocordillera/alertas/service/DeteccionLoggingTest.java | Nuevo: fallo de dependencia genera aviso sin mensaje sensible ni stacktrace. | Nada. |
| authentication/pom.xml | Boot 4.0.9; Cloud 2025.1.4 donde se declara; Springdoc 3.0.3. JaCoCo existente conservado. | Versiones anteriores sustituidas. |
| clientes/pom.xml | Boot 4.0.9, Springdoc 3.0.3 y Cloud 2025.1.4 donde se declara; añadido JaCoCo 0.8.12 con prepare-agent/report. | Versiones anteriores sustituidas. |
| finanzas/pom.xml | Boot 4.0.9; Cloud 2025.1.4 donde se declara; Springdoc 3.0.3. JaCoCo existente conservado. | Versiones anteriores sustituidas. |
| ventas/pom.xml | Boot 4.0.9; Cloud 2025.1.4 donde se declara; Springdoc 3.0.3. JaCoCo existente conservado. | Versiones anteriores sustituidas. |
| inventario/pom.xml | Boot 4.0.9; Cloud 2025.1.4 donde se declara; Springdoc 3.0.3. JaCoCo existente conservado. | Versiones anteriores sustituidas. |
| alertas/pom.xml | Boot 4.0.9, Springdoc 3.0.3 y Cloud 2025.1.4 donde se declara; añadido JaCoCo 0.8.12 con prepare-agent/report. | Versiones anteriores sustituidas. |
| kpis/pom.xml | Boot 4.0.9, Springdoc 3.0.3 y Cloud 2025.1.4 donde se declara; añadido JaCoCo 0.8.12 con prepare-agent/report. | Versiones anteriores sustituidas. |
| reportes/pom.xml | Boot 4.0.9, Springdoc 3.0.3 y Cloud 2025.1.4 donde se declara; añadido JaCoCo 0.8.12 con prepare-agent/report. | Versiones anteriores sustituidas. |
| api-gateway/pom.xml | Boot 4.0.9, Springdoc 3.0.3 y Cloud 2025.1.4 donde se declara; añadido JaCoCo 0.8.12 con prepare-agent/report. | Versiones anteriores sustituidas. |
| qa/test_system.py | SYS-09 cabeceras/CORS y SYS-10 diagnóstico no publicado; soporte de cabeceras de solicitud y registro selectivo de cabeceras de respuesta. | Nada de los ocho casos anteriores. No registra tokens ni cookies. |
| qa/run_sonar.py | Opción --project-version y etiqueta en resumen. | Etiqueta fija sustituida por valor configurable con predeterminado. |
| qa/run_dependency_scan.py | Opción --reuse-db y registro de su uso para comparación con mismo catálogo. | Nada: la actualización de base sigue siendo el comportamiento predeterminado. |
| README.md | Versiones actuales, ruta Compose, ejecución verify y credenciales por configuración. | Versiones obsoletas y ejemplo de contraseña administrativa fija. |
| docs/encargo3/MITIGACIONES_Y_REVERIFICACION.md | Nuevo análisis de cambios, alternativas, comparación, cobertura, evidencia y límites. | Nada. |
| docs/encargo3/AVANCE_Y_EVIDENCIAS.md | Aviso de estado actual; bloque anterior identificado como histórico. | Nada de los resultados anteriores. |
| docs/encargo3/ESCANEOS_SEGURIDAD.md | Enlace y aviso de reverificación posterior. | Nada de los reportes iniciales. |
| docs/encargo3/PLAN_ENCARGO3.md | Avance actual y enlace al cierre de mitigaciones. | Nada del historial. |
| docs/encargo3/GUIA_DE_EJECUCION.md | Opciones nuevas, generación de cobertura y orden seguro de pruebas globales/ZAP. | Nada. |
| docs/encargo3/MANTENIMIENTO_Y_CONTROLES.md | Estado posterior y monitoreo con catálogo actualizado. | Nada. |
| docs/encargo3/CAMBIOS_REALIZADOS.md | Este detalle por archivo. | Nada. |

La compilación desde cero con verify generó los nueve JAR y reportes JaCoCo. Se reconstruyeron los nueve servicios QA sin borrar sus datos. Se ejecutaron después diez métodos globales y los tres enfoques de escaneo. No se repitieron las mediciones anteriores de carga ni restauración, ni se ejecutó CI remoto. Las capturas anteriores permanecen sin alteración.

## Continuación después del reinicio — 8 de noviembre de 2026

No se modificaron fuentes Java, tests, dependencias, workflow ni frontend. Se iniciaron los dos proyectos Docker existentes conservando volúmenes. SonarQube quedó UP y qa/test_system.py volvió a aprobar los diez métodos (57,385 s). Evidencia: system/20261108T185501675626Z y resume/2026-11-08 bajo evidence/encargo3. La regresión Java de 311 sigue siendo la del 6 de noviembre, no una ejecución nueva.

| Archivo | Agregado o modificado | Quitado |
| --- | --- | --- |
| docs/encargo3/TRAZABILIDAD_REQUISITOS.md | Nuevo: 34 ítems del alcance, pruebas, controles, estados, exclusiones y límites. | Nada. |
| docs/encargo3/REVALIDACION_INCIDENTES.md | Nuevo: revisión de los 21 IDs heredados, evidencia, acciones de cierre e INC-22 adicional de configuración original. | Nada. No se marcan todos los incidentes como corregidos. |
| docs/encargo3/AVANCE_Y_EVIDENCIAS.md | Estado tras reinicio y enlace a matrices actuales. | Nada del historial. |
| docs/encargo3/PLAN_ENCARGO3.md | Avance de trazabilidad/revisión y pendientes de CI/informe. | Nada. |
| docs/encargo3/GUIA_DE_EJECUCION.md | Inicio de GitHub Flow desde CMD, rama propuesta y cuidado de evidencia privada. | Nada. |
| docs/encargo3/CAMBIOS_REALIZADOS.md | Este registro. | Nada. |

