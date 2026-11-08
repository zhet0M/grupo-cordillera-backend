# Avance y evidencias del Encargo 3

Fecha: 6 de noviembre de 2026. Este documento consolida el bloque de desarrollo y pruebas realizado; no es todavía el informe final de la evaluación.

Actualización posterior: el bloque de mitigaciones aprobó **311 pruebas Java y 10 métodos globales**. SCA pasó de 709 ocurrencias a cero en nueve módulos, Sonar de dos hotspots a cero y ZAP de tres tipos bajos a uno. Los resultados, límites, cobertura y rutas actuales están en [MITIGACIONES_Y_REVERIFICACION.md](MITIGACIONES_Y_REVERIFICACION.md). Las cifras 303/8, carga y respaldos descritos a continuación corresponden al bloque anterior y se conservan como historia de la evaluación.

## Resultado del bloque previo

Se reprodujo la línea base de 244 pruebas Java. Después de las mitigaciones se ejecutaron 303 pruebas, todas aprobadas, sin fallos, errores u omisiones. Se añadieron 59 pruebas Java; los casos existentes de caracterización modificados no se cuentan como casos nuevos.

La suite HTTP aprobó ocho métodos de prueba con subcasos por rol, ruta y recurso. En esa corrida realizó 99 solicitudes y comprobó servicios y persistencia reales, incluidos reportes PDF y la recuperación de finanzas. La duración informada por unittest fue 53,197 segundos, incluyendo preparación y recuperación de servicios.

Las pruebas de carga y restauración se registran por separado: sus muestras no se suman a las pruebas Java ni se presentan como cientos de casos de prueba diferentes.

## Regresión por módulo

| Módulo | Línea base | Corrida final | Estado final |
| --- | ---: | ---: | --- |
| authentication | 22 | 28 | Aprobado |
| clientes | 14 | 16 | Aprobado |
| finanzas | 4 | 4 | Aprobado |
| ventas | 18 | 22 | Aprobado |
| inventario | 16 | 19 | Aprobado |
| alertas | 32 | 32 | Aprobado |
| kpis | 28 | 28 | Aprobado |
| reportes | 11 | 11 | Aprobado |
| api-gateway | 99 | 143 | Aprobado |
| Total | 244 | 303 | Sin fallos, errores u omisiones |

Parte de la suite heredada caracteriza comportamientos existentes. Además, varios contextLoads heredados son métodos vacíos, no arranques reales del sistema. Por eso esta tabla no acredita por sí sola conformidad con todos los requisitos; las pruebas HTTP y la revisión de incidentes aportan evidencia adicional.

## Comparación anterior y posterior

| Hallazgo o ajuste | Evidencia anterior | Mitigación y resultado |
| --- | --- | --- |
| INC-01 Rutas no declaradas autorizadas | Los nuevos casos de seguridad mostraron permisos sobre rutas desconocidas. | Lista de dominios y denegación por defecto; casos unitarios y HTTP de ruta no declarada aprobados. |
| INC-04 Rol ausente | Un token válido sin rol podía leer dominios. | Rechazo 401 para sujeto ausente y rol ausente o no reconocido; pruebas con mocks y JWT reales aprobadas. |
| Coincidencias ambiguas de rutas | Parte de las rutas parecidas a las públicas omitía autenticación en el filtro. No se demostró un endpoint sensible desplegado bajo esos nombres. | Coincidencia exacta o por segmento, probada en componentes y por HTTP. No se atribuye una explotación no observada. |
| INC-02 Credenciales de administrador fijas | Revisión del inicializador y hallazgo heredado. | Inicialización opcional y configuración externa, verificada con BCrypt real. No rota cuentas ya creadas en otros ambientes. |
| Clave JWT fija en configuración | Presente en autenticación, respaldo del Gateway y blueprint Render. | Retirada de esas configuraciones, clave externa compartida y rechazo de claves débiles al iniciar. No se modificó ningún despliegue externo. |
| INC-06 Cliente inexistente devuelve 500 | SYS-05 recibió 500 al solicitar un ID inexistente. | Excepción específica y traducción a 404; pruebas de controlador y SYS-05 aprobadas. |
| Errores técnicos de clientes | El manejador exponía el mensaje de RuntimeException. | Respuesta 500 con mensaje genérico, verificada mediante HTTP de componente. |
| Cantidad negativa aceptada en descuento | SYS-08 recibió 200 para un descuento de -1. El servicio restaba esa cantidad del stock. | Rechazo previo a efectos en inventario y ventas; 7 pruebas Java nuevas y SYS-08 aprobados. La validación de todos los demás campos sigue pendiente. |
| Dato de prueba POS con cliente | La primera versión de SYS-03 esperaba actualizar compras desde una venta POS. | Se corrigió el dato a ECOMMERCE según el flujo existente y las pruebas heredadas. No se cambió producción para adaptar ese supuesto. El ERS debe aclarar si POS también debe admitir cliente. |

Antes de corregir el Gateway, sus 33 casos nuevos registraron 31 fallos, un error y un aprobado. Después, los 33 aprobaron. El error correspondía a una excepción al extraer claims que escapaba del filtro; no fue un error de compilación.

Los primeros intentos de línea base fallaron por la ubicación de la caché Maven y permisos de lectura del entorno. Otro intento ejecutó pruebas correctamente, pero el ejecutor no encontraba reportes porque Surefire ignoraba su propiedad de carpeta. Esos intentos se conservan como diagnóstico; no son resultados finales ni se cuentan como fallas del producto.

## Casos globales ejecutados

| Caso | Requisitos principales | Comprobación | Resultado |
| --- | --- | --- | --- |
| SYS-01 | RF-1.2, RF-2.1, RB-2 | Registro pendiente y rechazo de login mediante respuesta 4xx | Aprobado; no verifica todavía el texto específico de rechazo |
| SYS-02 | RF-2.3, RF-3.3, RB-4, RB-5 | Aprobación, login con rol, lectura y rechazo de escritura y administración fuera del permiso | Aprobado |
| SYS-03 | RF-4.3, RF-5.1 a RF-5.3 | Venta ECOMMERCE, stock 10 a 8, una compra del cliente por 20000, un movimiento financiero con ingresos 20000, costo 12000 y margen 8000 | Aprobado |
| SYS-04 | NFR-SEG-4, NFR-SEG-9 | Sin token, token inválido, rutas parecidas a públicas y ruta no declarada | Aprobado |
| SYS-05 | RF-4.1, NFR-SEG-5 y criterios HTTP | Cliente inválido devuelve 400 e inexistente devuelve 404 | Aprobado |
| SYS-06 | RF-6.2 | Consulta de KPIs y alertas y descarga de tres reportes con tipo PDF y firma de archivo | Aprobado; no analiza todavía el contenido completo de cada PDF |
| SYS-07 | RF-5.3, RF-5.4 | Finanzas QA detenido, venta pendiente, reinicio y reproceso con un único movimiento y un único descuento de stock | Aprobado |
| SYS-08 | RF-5.2, NFR-SEG-5 | Descuento negativo y ventas con cantidad cero o negativa devuelven 400 sin alterar el stock del caso | Aprobado |

## Carga local final

Endpoint: GET /clientes por el Gateway QA. Dataset ficticio pequeño. Diez solicitudes de calentamiento, cincuenta muestras secuenciales y cien clientes concurrentes con cinco solicitudes cada uno. La corrida final se hizo después de terminar la compilación y las pruebas de escritura para evitar esa carga adicional durante la medición.

| Métrica | Carga normal | Cien clientes |
| --- | ---: | ---: |
| Solicitudes medidas | 50 | 500 |
| Respuestas distintas de 200 o errores de red | 0 | 0 |
| Mediana | 14,29 ms | 1048,55 ms |
| Percentil 95 | 31,09 ms | 2870,12 ms |
| Máximo | 38,28 ms | 6142,63 ms |

Las cincuenta muestras normales cumplen el umbral de 300 ms para esta consulta. Se observaron cien solicitudes de clientes simultáneamente en curso y se completaron las quinientas sin errores. La latencia aumenta sustancialmente con concurrencia; esto es un resultado a analizar, no se presenta como rendimiento satisfactorio para cualquier escenario. No se demostró que todos los CRUD cumplan 300 ms ni capacidad sostenida, carga productiva o un objetivo mensual de disponibilidad.

Entorno registrado: Java local 17.0.12, Java de contenedores 17.0.21, Maven 3.9.14 y Docker 29.8.2. Docker informó doce CPU y aproximadamente 12,48 GB de memoria asignada. Los servicios Java QA utilizan un máximo de heap de 256 MB cada uno. Estos datos deben acompañar la interpretación de las métricas.

## Respaldo y restauración

Se ejecutó pg_dump en formato personalizado y pg_restore sobre bases nuevas para bd_auth, bd_clientes, bd_inventario, bd_ventas, bd_finanzas y bd_alertas. Las comparaciones de cantidades y huellas de filas aprobaron en las seis bases, tanto en la corrida inicial como en la final, esta última con los datos producidos por SYS-03 y SYS-07. En la final cada respaldo, restauración y comparación tardó entre 2,82 y 4,49 segundos; no se interpreta ese tiempo de laboratorio como RTO productivo.

La verificación utiliza bases temporales distintas de las originales y elimina únicamente las que creó cuando la comparación pasa. Conserva los dumps para revisión. No acredita cifrado de respaldos, retención externa, recuperación de roles, secuencias, recuperación a un instante ni RPO/RTO productivos.

## Ubicación de evidencia local

Todos los destinos siguientes se encuentran bajo evidence/encargo3, excluido de Git. Sus rutas son relativas al backend.

| Evidencia | Ruta |
| --- | --- |
| Línea base original 244 | evidence/encargo3/baseline/20261106T194737053Z/summary.json y SUMMARY.md |
| Regresión final 303 | evidence/encargo3/baseline/20261106T201211105Z/summary.json y SUMMARY.md |
| Seguridad Gateway antes | evidence/encargo3/gateway-security-before.log y gateway-security-before/ |
| Seguridad Gateway después y JWT reales | evidence/encargo3/gateway-jwt-real-after.log; además, XML de la regresión final |
| Primera corrida global con dos fallos | evidence/encargo3/system/20261106T200233625759Z/results.json |
| Cantidad negativa antes | evidence/encargo3/system/20261106T201115196787Z/results.json y quantity-before.log |
| Ocho casos globales aprobados | evidence/encargo3/system/20261106T201617398985Z/results.json y system-final.log |
| Carga final | evidence/encargo3/load/20261106T202058129403Z/results.json y load-final.log |
| Restauración final de seis bases | evidence/encargo3/backup/20261106T202231086921Z/results.json y backup-final.log |
| Estado y recursos de Docker | evidence/encargo3/qa-containers.json y docker-resources.json |
| Captura del ambiente entregada por el estudiante | evidence/encargo3/capturas/EV-AMB-01_Docker_QA_2026-11-06.png; descripción y huella en capturas/REGISTRO.md |
| Capturas ZAP entregadas por el estudiante | evidence/encargo3/capturas/EV-SEG-01_ZAP_Resumen_2026-11-06.png y EV-SEG-02_ZAP_Detalle_2026-11-06.png; descripción y huellas en capturas/REGISTRO.md |
| Captura SonarQube authentication | evidence/encargo3/capturas/EV-SAST-01_Sonar_Authentication_2026-11-06.png; descripción en capturas/REGISTRO.md |
| Capturas SonarQube alertas | evidence/encargo3/capturas/EV-SAST-02_Sonar_Alertas_Overview_2026-11-06.png y EV-SAST-03_Sonar_Alertas_Hotspots_2026-11-06.png; descripciones y huellas en capturas/REGISTRO.md |
| SonarQube alertas después de mitigar | evidence/encargo3/capturas/EV-SAST-04_Sonar_Alertas_Mitigado_2026-11-06.png; 0 hotspots, 34 pruebas y 86,5 % de cobertura; descripción y huella en capturas/REGISTRO.md |
| Detalle ZAP después de mitigar | evidence/encargo3/capturas/EV-SEG-03_ZAP_Alertas_Mitigado_2026-11-06.png; un tipo bajo y dos informativos; descripción, límites y huella en capturas/REGISTRO.md |
| Identificación de ejecutables generados | evidence/encargo3/artifact-manifest.json |

Los hashes del manifiesto identifican los JAR generados y sus fuentes principales actuales. Los contenedores se construyeron durante las corridas de QA y se reconstruyeron cuando cambió su lógica; no se debe confundir el hash de un JAR reconstruido después con el de un contenedor previo sin comprobarlo.

qa.env y los dumps no son material para publicar sin revisión. Para la entrega se seleccionarán resultados y capturas legibles, sin credenciales, tokens o información personal real.

## Pendientes para completar el encargo

Actualización del 8 de noviembre: el laboratorio se restableció sin borrar volúmenes y los diez métodos globales aprobaron de nuevo (57,385 s), en evidence/encargo3/system/20261108T185501675626Z. SonarQube informó UP; estado de contenedores en evidence/encargo3/resume/2026-11-08. Se completaron las matrices documentales de 34 requisitos y 21 incidentes heredados en TRAZABILIDAD_REQUISITOS.md y REVALIDACION_INCIDENTES.md. Esto cierra su inventario/revisión, no todas las brechas. La revisión agrega INC-22 de configuración heredada. Los pendientes siguientes se conservan como historia; su estado actual debe leerse junto a esas matrices.

1. Revalidar todos los incidentes heredados y completar la trazabilidad de los 34 ítems del alcance; no declarar cerradas las brechas que no se evaluaron.
2. Revisar y mitigar los resultados de los escaneos ya ejecutados: SAST y SCA en nueve módulos y DAST autenticado en nueve consultas. El detalle está en ESCANEOS_SEGURIDAD.md. Ampliar DAST si se necesita cubrir otros métodos, roles o vectores.
3. Profundizar en alertas periódicas, validación del resto de campos, errores de otros servicios, acceso directo a microservicios y política de revocación de tokens de cuentas bloqueadas.
4. Obtener evidencia específica de cifrado, resguardo de logs y aspectos organizativos de tratamiento de datos. Un checklist técnico no certifica cumplimiento legal.
5. Subir el workflow mediante el proceso acordado, comprobar su ejecución en GitHub y, si corresponde, configurar checks obligatorios en la rama.
6. Incorporar los resultados, diagramas y mantenimiento al formato Word entregado, preparar capturas y defensa de diez minutos con cinco de preguntas.

La documentación complementaria está en GUIA_DE_EJECUCION.md, MANTENIMIENTO_Y_CONTROLES.md, ESCANEOS_SEGURIDAD.md y CAMBIOS_REALIZADOS.md. El laboratorio queda disponible para revisar evidencia; su detención segura está documentada en la guía.
