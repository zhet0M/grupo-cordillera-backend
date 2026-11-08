# Mitigaciones y reverificación del backend

Ejecutado el 6 de noviembre de 2026, hora de Santiago. Los identificadores de corridas usan UTC y algunos comienzan el 7 de noviembre. Este documento complementa el diagnóstico inicial de ESCANEOS_SEGURIDAD.md; conserva sus resultados como evidencia anterior.

## Resultado comprobado

| Verificación | Antes de este bloque | Después de este bloque |
| --- | --- | --- |
| Regresión Java, nueve módulos | 303 aprobadas | 311 aprobadas, cero fallos, errores u omisiones |
| Pruebas globales HTTP | 8 métodos aprobados | 10 métodos aprobados, 27,704 segundos |
| SCA, nueve módulos | 709 ocurrencias, 66 identificadores distintos y 84 combinaciones aviso/paquete/versión | Cero ocurrencias detectadas |
| SAST, nueve módulos | 2 hotspots, 100 code smells, 0 bugs y 0 vulnerabilidades | 0 hotspots, 79 code smells, 0 bugs y 0 vulnerabilidades |
| ZAP, nueve consultas autenticadas | 3 tipos de alerta baja y 2 informativos | 1 tipo de alerta baja y 2 informativos; cero altos o medios |

Las ocurrencias de SCA incluyen bibliotecas compartidas entre servicios: no equivalen a 709 vulnerabilidades diferentes ni a ataques demostrados. Se reutilizó la misma base Trivy para comparar el cambio de dependencias sin mezclarlo con una actualización del catálogo. Un análisis futuro debe actualizar la base y puede producir resultados nuevos. Los nueve Quality Gates de SonarQube aprobaron; ese indicador tampoco acredita seguridad completa.

## Cambios y alternativas consideradas

| Hallazgo | Medida implementada y motivo | Alternativa y seguimiento |
| --- | --- | --- |
| Avisos en dependencias de ejecución | Actualización coordinada de Spring Boot 4.0.6 a 4.0.9, Spring Cloud 2025.1.1 a 2025.1.4 donde aplica y Springdoc 2.8.8 a 3.0.3. Se mantiene Java 17 y la rama compatible. La compilación y las pruebas reales comprobaron la actualización. | Fijar versiones transitivas individualmente permitiría cambios puntuales, pero fragmentaría el conjunto gestionado por Boot. Una migración mayor requiere evaluación aparte. |
| ZAP: ausencia de nosniff y CORP | Filtro del Gateway con X-Content-Type-Options: nosniff y Cross-Origin-Resource-Policy: same-origin, ejecutado antes de CORS y JWT, incluso en rechazos. SYS-09 verifica respuestas 200, 401, 403 y preflight permitido. ZAP deja de informar ambos tipos de alerta. | Configurar cabeceras en un proxy externo es posible, pero no estaba disponible como control verificable del backend local. CORP restringe solicitudes no-cors; el preflight permitido sigue funcionando. No se probó la incrustación de PDF en el navegador del frontend. |
| Hotspots en alertas y diagnóstico expuesto | Retiro de /alertas/test-inventario y de su dependencia innecesaria. Sustitución de printStackTrace y salidas de depuración por avisos con operación y clase de excepción, sin mensajes sensibles ni payloads. Sonar ya no detecta esos hotspots; SYS-10 comprueba 404. | Limitar el endpoint a un perfil de desarrollo sería viable, pero no era una función del ERS. Ocultar o marcar hotspots como seguros sin corregir el código no aportaba la misma evidencia. |
| Cobertura ausente en cinco módulos | JaCoCo en alertas, gateway, clientes, kpis y reportes, con generación de XML en verify. Se importaron reportes actuales en Sonar. | El 0 % anterior se debía a ausencia de reporte importable: no permite afirmar que esas pruebas cubrían cero líneas. El porcentaje nuevo mide la cobertura observada y no garantiza ausencia de errores. |

Además se aplica Cache-Control: no-store en el Gateway para respuestas de la API. La alerta informativa de contenido no almacenable puede mantenerse como reflejo de esta política; no se habilitó caché pública para silenciarla.

## Alertas residuales y alcance

ZAP conserva el tipo bajo «Unexpected Content-Type was returned» en los PDF y dos tipos informativos: respuestas de error de cliente y contenido no almacenable. Los tres PDF devolvieron application/pdf en las comprobaciones previas y su firma fue validada por SYS-06. Se clasifica la advertencia PDF como comportamiento esperado para RF-6.2 en este alcance; la alerta original permanece visible. Los rechazos 4xx se deben revisar por ruta y contexto: no se relaja la autorización para eliminarlos.

SCA analiza dependencias compile/runtime, excluyendo test/provided. No analiza el sistema operativo de las imágenes ni demuestra explotabilidad o ausencia de vulnerabilidades desconocidas. ZAP cubre nueve operaciones GET con ANALISTA y escaneo activo acotado; no todos los métodos, roles, parámetros, TLS productivo ni frontend. Persisten 79 code smells para priorización por mantenibilidad.

No se declara cerrado INC-03 de alertas de ventas bajas cuando desaparecen las ventas actuales, ni todos los incidentes heredados. La trazabilidad completa, revocación de JWT, acceso directo a servicios, evidencia organizativa de protección de datos y CI remoto siguen pendientes. La carga y restauración de seis bases pertenecen al bloque previo: no se repitieron tras actualizar dependencias y sus tiempos no se atribuyen a estos JAR.

## Cobertura importada después

| Módulo | Cobertura SonarQube |
| --- | ---: |
| authentication | 54,6 % |
| clientes | 67,9 % |
| finanzas | 50,7 % |
| ventas | 71,3 % |
| inventario | 65,9 % |
| alertas | 86,5 % |
| kpis | 79,7 % |
| reportes | 87,6 % |
| api-gateway | 95,1 % |

## Evidencias posteriores

Rutas relativas al backend, excluidas de Git:

- Java: evidence/encargo3/baseline/20261107T011753840Z/summary.json y reportes por módulo.
- Sistema: evidence/encargo3/system/20261107T012533650156Z/results.json.
- Sonar: evidence/encargo3/security/sonar/20261107T012132927082Z/summary.json y JSON por módulo.
- SCA: evidence/encargo3/security/dependencies/20261107T012132960828Z/summary.json, inventarios y reportes Trivy.
- ZAP: evidence/encargo3/security/zap/20261107T012612209161Z/report.html, report.json y summary.json. Compartir solo estos reportes saneados; el subdirectorio private contiene credenciales.
- Comparación reproducible: evidence/encargo3/security/mitigation/comparison.json y sonar-before-after.csv; incluyen identificadores de las corridas anteriores.
- Versiones y JAR: evidence/encargo3/security/mitigation/selected-versions.json, spring-boot-dependencies.pom y artifacts-after.json. La reconstrucción QA consta en qa-rebuild.log.

Las primeras capturas representan el estado anterior y se conservan. La captura posterior de alertas entregada por el estudiante está en evidence/encargo3/capturas/EV-SAST-04_Sonar_Alertas_Mitigado_2026-11-06.png: muestra 34 pruebas, 0 hotspots, 4 code smells y 86,5 % de cobertura. La tabla de alertas ZAP posterior está en evidence/encargo3/capturas/EV-SEG-03_ZAP_Alertas_Mitigado_2026-11-06.png: un tipo bajo y dos informativos. Esta última no incluye el encabezado ni Summary of Alerts; el reporte HTML/JSON conserva la identificación y los totales. Las huellas y pies de figura están en capturas/REGISTRO.md. No reemplazar las imágenes anteriores ni rotularlas como resultados posteriores.

## Fuentes técnicas de selección

La [matriz oficial de Spring Cloud](https://spring.io/projects/spring-cloud/) relaciona la rama 2025.1 con Boot 4.0/4.1. La [matriz oficial de Springdoc](https://springdoc.org/) relaciona Boot 4.0 con Springdoc 3.0.x. Las versiones concretas disponibles se verificaron en Maven Central y se conservaron localmente. La [documentación de CORP](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cross-Origin-Resource-Policy) explica su alcance sobre solicitudes no-cors. No se escogieron versiones transitivas aisladas a partir de la primera corrección sugerida por un escáner.
