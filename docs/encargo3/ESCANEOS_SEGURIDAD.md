# Análisis de seguridad del Encargo 3

**Estado posterior:** se implementaron y verificaron mitigaciones. Consultar [MITIGACIONES_Y_REVERIFICACION.md](MITIGACIONES_Y_REVERIFICACION.md) para los resultados actuales. Este documento conserva el diagnóstico anterior.

Fecha de ejecución: 6 de noviembre de 2026. Se ejecutaron análisis estático, de dependencias y dinámico. Este bloque agrega herramientas y evidencia; no modifica la lógica Java ni actualiza versiones de las dependencias del producto.

## Qué se hizo y qué aporta cada herramienta

| Enfoque | Herramienta | Alcance real | Evidencia |
| --- | --- | --- | --- |
| SAST y calidad de código | SonarQube 9.9.8.100196 y SonarScanner Maven 5.8.0.7211 | Nueve módulos, con las reglas instaladas en el servidor local separado | Métricas, issues, hotspots, quality gate y logs por módulo |
| SCA de bibliotecas | CycloneDX Maven 2.9.3 y Trivy 0.76.2 | Grafo Maven resuelto de compilación y ejecución; excluye pruebas y provided | Nueve SBOM JSON, resultados CVE/GHSA, versiones informadas y tabla de revisión deduplicada |
| DAST de API | OWASP ZAP 2.17.0 | Nueve consultas GET verificadas con HTTP 200, usuario ANALISTA, únicamente Gateway QA | Reporte HTML, JSON, Markdown, sondas previas y log saneado |

SonarQube se inició en un proyecto Docker distinto del backend, con acceso solo por localhost:9001 y base H2 de laboratorio. Se cambió su contraseña inicial y se creó un token de análisis local, sin imprimirlos. La contraseña y el token permanecen en un archivo local excluido de Git; no constituyen un almacén cifrado productivo.

La imagen de Trivy está fijada por digest. ZAP se ejecutó con el digest resuelto de la imagen oficial descargada. Los dos digest quedan registrados en sus reportes. La base de vulnerabilidades se descargó para el primer módulo y se reutilizó sin actualizar durante los siguientes ocho, para mantener la misma referencia de datos en esa corrida.

## SonarQube

| Módulo | Observaciones de mantenibilidad | Hotspots de seguridad | Quality Gate |
| --- | ---: | ---: | --- |
| authentication | 26 | 0 | OK |
| clientes | 4 | 0 | OK |
| finanzas | 7 | 0 | OK |
| ventas | 6 | 0 | OK |
| inventario | 3 | 0 | OK |
| alertas | 25 | 2 | OK |
| kpis | 12 | 0 | OK |
| reportes | 10 | 0 | OK |
| api-gateway | 7 | 0 | OK |
| Total | 100 | 2 | Nueve análisis procesados |

Los 100 issues exportados son CODE_SMELL; no se reclasifican automáticamente como bugs o vulnerabilidades. Los dos hotspots permanecen TO_REVIEW y señalan e.printStackTrace() en AlertaController.java, línea 95, y DeteccionAlertasService.java, línea 102. La revisión confirma salida de depuración; su impacto depende del acceso y tratamiento de los logs. Se propone sustituirla por registro controlado y revisar el endpoint de diagnóstico antes de producción.

Un Quality Gate OK no significa ausencia de defectos ni aceptación completa del ERS. Es el resultado de las condiciones configuradas en ese servidor y no incluye por sí solo las vulnerabilidades de dependencias detectadas por Trivy. La antigüedad y el perfil del analizador también limitan los hallazgos.

SonarQube muestra cobertura 54,6% en authentication, 50,7% en finanzas, 65,9% en inventario y 71,3% en ventas. En los otros cinco módulos muestra 0%, pero no hay informe JaCoCo configurado para importar: se considera falta de medición, no prueba de que sus tests no ejecuten código. Esos módulos sí participaron en la regresión de 303 pruebas.

## Dependencias

| Módulo | Componentes SBOM | Coincidencias totales | Críticas según Trivy | Altas según Trivy |
| --- | ---: | ---: | ---: | ---: |
| authentication | 102 | 76 | 10 | 27 |
| clientes | 108 | 81 | 12 | 30 |
| finanzas | 108 | 81 | 12 | 30 |
| ventas | 129 | 83 | 12 | 32 |
| inventario | 108 | 81 | 12 | 30 |
| alertas | 110 | 81 | 12 | 30 |
| kpis | 97 | 77 | 12 | 28 |
| reportes | 81 | 75 | 12 | 26 |
| api-gateway | 73 | 74 | 12 | 25 |

Las 709 ocurrencias corresponden a 66 identificadores distintos CVE/GHSA y 84 combinaciones distintas de aviso, paquete y versión. Estas combinaciones se distribuyen en 12 críticas, 32 altas, 35 medias y 5 bajas según Trivy. No son 709 vulnerabilidades independientes ni 709 explotaciones demostradas.

La tabla dependencies-triage.csv conserva los 84 registros con bibliotecas, versiones instaladas, versiones corregidas informadas, severidad, módulos y enlace del aviso. Su estado inicial es coincidencia de versión con explotabilidad pendiente de revisión. Un SBOM de otra herramienta puede limitar la identificación de Trivy; hay que contrastar inventario, empaquetado y condiciones del aviso. No se analizaron el sistema operativo de las imágenes ni las dependencias de pruebas o provided en esta corrida.

### Revisión contextual de dos ejemplos

| Aviso | Coincidencia | Condición y evaluación inicial | Alternativas |
| --- | --- | --- | --- |
| CVE-2026-43512 | Tomcat embed core 11.0.21; Trivy la etiqueta CRITICAL | Apache la clasifica Moderate y describe una condición de autenticación DIGEST. El código revisado usa JWT y no muestra DIGEST configurado; esto limita la evidencia de explotación, sin eliminar la coincidencia de versión. | Actualizar la línea compatible de Tomcat mediante un BOM mantenido; verificar que DIGEST no esté habilitado como contención específica. |
| CVE-2026-41293 | Tomcat embed core 11.0.21; Trivy la etiqueta CRITICAL | Apache la clasifica Low y la vincula con validación de cabeceras HTTP/2. Debe comprobarse si HTTP/2 está habilitado efectivamente antes de atribuir impacto al sistema. | Actualizar a una versión mantenida que resuelva el conjunto de avisos; revisar la necesidad de HTTP/2 como contención temporal. |

Fuente primaria para ambas condiciones y clasificación del fabricante: [Apache Tomcat 11 security](https://tomcat.apache.org/security-11.html). La diferencia de severidades se conserva; no se sustituye silenciosamente la del escáner.

También aparecen Jackson 2 y 3, Spring y otras bibliotecas compartidas. Sus avisos requieren revisar tipos de entrada, deserialización, endpoints y configuración. No se afirma ejecución remota ni bypass real por el solo título del CVE.

La opción preferida es actualizar dependencias mediante versiones compatibles y mantenidas, ejecutar la regresión y repetir SCA. Una sustitución temporal de una versión transitiva puede reducir un riesgo concreto, pero exige revisar compatibilidad y los demás avisos; la primera versión FixedVersion de un aviso puede seguir afectada por otros. No se aplicaron cambios de versión en este bloque.

## Análisis dinámico con ZAP

Se verificó primero que el Gateway publica OpenAPI, pero su definición propia enumera únicamente /kpis y /kpis/{tipo}. Para no presentar ese documento como cobertura de todo el backend, se creó un alcance explícito de nueve GET, basado en controladores y comprobado por HTTP con el usuario de prueba.

Rutas: /clientes, /inventario, /ventas, /finanzas, /kpis, /alertas/resumen y los tres /reportes/{dominio}/pdf para ventas, inventario y finanzas.

El escaneo activo se limitó a tres minutos, con dos hilos por host. No se publicaron puertos de control de ZAP. La credencial se envió como cabecera limitada al host api-gateway. Los reportes compartibles fueron saneados y se comprobó que no contienen el token del escaneo. Los originales y auth.env permanecen en el subdirectorio private y no se deben usar como evidencia compartida.

| Regla ZAP | Nivel del reporte | Interpretación y acción propuesta |
| --- | --- | --- |
| 10021 X-Content-Type-Options ausente | Bajo | Cabecera ausente confirmada en respuestas analizadas. Proponer nosniff de forma centralizada y repetir la comprobación de JSON y PDF. |
| 90004 Cross-Origin-Resource-Policy ausente o inválida | Bajo | Evaluar una política explícita según la arquitectura y CORS del frontend. No aplicar same-origin de forma indiscriminada. |
| 100001 Tipo application/pdf inesperado | Bajo | Comportamiento esperado por RF-6.2 y verificado en SYS-06. Es un falso positivo respecto del contrato de esos tres endpoints; se conserva la alerta original y la justificación. |
| 100000 Respuesta 403 | Informativo | No demuestra un error 500 ni un bypass. Contrastar las sondas con las restricciones de método, ruta y rol; no relajar permisos para eliminar la alerta. |
| 10049 Contenido no almacenable | Informativo | No habilitar caché pública sobre datos personales para silenciar el aviso. Revisar la política explícita de almacenamiento de respuestas. |

El reporte contiene cinco tipos de alerta: tres bajos y dos informativos, sin tipos altos o medios dentro de este alcance. El código de salida 2 de ZAP indica advertencias. La ejecución se completó, pero no significa que el sistema esté libre de vulnerabilidades.

Este DAST no cubre escrituras, todos los roles, todos los vectores de entrada, lógica de negocio, XSS del frontend o TLS productivo. Las pruebas Java y HTTP negativas del bloque anterior complementan parte de esas limitaciones. El límite de tiempo impide asumir exhaustividad.

## Evidencia guardada

Rutas relativas al backend, bajo evidence/encargo3/security, excluido de Git:

| Resultado | Ruta |
| --- | --- |
| SonarQube por módulo y logs | sonar/20261106T230922182661Z/ |
| SBOM y Trivy por módulo | dependencies/20261106T231018369935Z/ |
| Reporte ZAP HTML saneado | zap/20261106T231536797441Z/report.html |
| Reporte ZAP JSON saneado | zap/20261106T231536797441Z/report.json |
| Resumen conjunto | consolidated/summary.json |
| Revisión deduplicada de dependencias | consolidated/dependencies-triage.csv |
| Métricas Sonar tabuladas | consolidated/sonar-summary.csv |
| Versión del servidor Sonar | consolidated/sonar-server.json |

## Pasos manuales para el estudiante

1. Abrir el reporte HTML saneado de ZAP y guardar una captura legible del resumen de alertas. Utilizar el archivo del nivel principal, no el de private.
2. Abrir http://localhost:9001. El usuario es admin; la contraseña generada está en el campo adminPassword de evidence/encargo3/security/sonar/credentials.json. Copiar solo ese valor al inicio de sesión, sin publicarlo en el chat o en capturas.
3. En Projects, abrir Encargo 3 - authentication y luego Encargo 3 - alertas. Guardar capturas de Overview y Security Hotspots con el nombre del proyecto y resultados visibles.
4. Conservar las capturas junto con las ya recibidas del laboratorio. Las cifras del informe deben provenir de los JSON y CSV; la captura las complementa.

Los scripts para repetir los análisis son qa/run_sonar.py, qa/run_dependency_scan.py y qa/run_zap_scan.py. Sonar requiere su proyecto Docker iniciado; los otros dos requieren sus imágenes oficiales descargadas. Las credenciales se conservan en archivos locales excluidos de Git y nunca se pasan por parámetros con sus valores visibles.

## Qué falta después de esta ejecución

- Revisar y priorizar las combinaciones de avisos según aplicabilidad, impacto y mitigación; cerrar o aceptar explícitamente los hallazgos, sin suprimirlos para obtener un reporte verde.
- Implementar las mitigaciones elegidas y repetir los análisis afectados junto con regresión.
- Incorporar los controles a CI cuando se defina cómo alojar Sonar y ejecutar DAST en un ambiente reproducible. La ejecución local todavía no es evidencia de CI remoto.
- Completar la trazabilidad del ERS, registrar las capturas, consolidar el informe Word y preparar la defensa.

## Fuentes técnicas oficiales

- [SonarScanner para Maven](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven).
- [SonarQube y base de laboratorio](https://docs.sonarsource.com/sonarqube-community-build/server-installation/installing-the-database).
- [CycloneDX makeBom](https://cyclonedx.github.io/cyclonedx-maven-plugin/makeBom-mojo.html).
- [Trivy y SBOM](https://trivy.dev/docs/latest/target/sbom/), [base de vulnerabilidades](https://trivy.dev/docs/dev/configuration/db/).
- [ZAP API Scan y códigos de salida](https://www.zaproxy.org/docs/docker/api-scan/), [autenticación con cabeceras](https://www.zaproxy.org/docs/getting-further/authentication/handling-auth-yourself/).
## Actualización de mitigaciones

Este archivo conserva el diagnóstico inicial. La corrección y los nuevos resultados comprobados se documentan en [MITIGACIONES_Y_REVERIFICACION.md](MITIGACIONES_Y_REVERIFICACION.md): 311 pruebas Java, 10 globales, SCA sin avisos detectados, cero hotspots y dos tipos de alerta baja ZAP eliminados. No interpretar las cifras anteriores como estado actual.

