# Plan del Encargo 3 para Grupo Cordillera

Fecha de revisión: 6 de noviembre de 2026.

## Propósito y estado

Validar el backend completo, ejecutar pruebas de seguridad y automatizar controles de calidad con evidencias reproducibles. Este archivo es un plan de ejecución; no acredita pruebas nuevas aprobadas. El informe final utilizará la plantilla entregada por el docente.

Fuentes revisadas: pauta EP5 ISY1102, formato de informe, informe del Encargo 2, ERS v1.4 y estructura del repositorio backend. La revisión no modifica el frontend ni el código de producción.

La pauta se titula Evaluación Parcial Nº5, indica 11% y contiene una referencia a Evaluación Parcial 3. La planificación del semestre comunicada por el estudiante denomina esta etapa Encargo 3 y le asigna 35%. Se aplican los indicadores de la pauta adjunta, sin inferir que esos porcentajes externos sean equivalentes. Esta diferencia no impide el desarrollo técnico.

## Alcance

- API Gateway y ocho servicios de dominio: authentication, clientes, ventas, inventario, finanzas, alertas, kpis y reportes.
- PostgreSQL e interacciones reales entre servicios en un ambiente de pruebas aislado con datos ficticios.
- Pruebas globales del backend por HTTP a través del Gateway. El término E2E backend se usará solo para recorridos que atraviesen servicios reales y persistencia real. Las dependencias simuladas se identificarán como pruebas unitarias o de integración de componentes.
- Regresión de lo evaluado anteriormente, seguridad, validación de entradas, errores, rendimiento local y recuperación de respaldos.
- No se requiere modificar el frontend. Usabilidad, accesibilidad, carga del dashboard y compatibilidad visual siguen fuera de la verificación de este backend, en coherencia con la sección 3.5 del ERS.
- TLS y cifrado en reposo se documentarán según la evidencia disponible de configuración e infraestructura. No se acreditarán por la sola existencia de pruebas unitarias ni por la ausencia de código de cifrado en entidades.
- La disponibilidad mensual no se acreditará con una prueba puntual. Ventanas de mantenimiento y despliegue se abordarán en el plan operativo.

## Matriz de cobertura de la rúbrica

| Indicador | Peso | Trabajo necesario | Evidencia y ubicación propuesta |
| --- | --- | --- | --- |
| IE1 Enfoques y herramientas de seguridad | 30% | Explicar revisión manual, SAST, DAST y análisis de dependencias; justificar herramientas y sus límites con ejemplos del sistema. Ejecutar pruebas de seguridad y analizar hallazgos. | Secciones 3.2, 3.4, 5 y 6. Casos HTTP, reportes de análisis y registro de incidentes reproducibles. |
| IE3 Diagramas de controles automatizados | 14% | Diagramar el proceso de integración continua y los flujos de datos y controles del backend. Vincular cada control con requisitos y criterios normativos. | Sección 3.1 y nueva subsección de controles automatizados; diagramas acompañados de una matriz control, requisito, evidencia y limitación. |
| IE5 Automatización y eficiencia | 24% | Diseñar y ejecutar regresión, pruebas globales y de seguridad. Medir duración, resultados, repetibilidad y tareas automatizadas. | Secciones 4, 5.1 y 5.2; configuración ejecutable, logs, informes y métricas obtenidas. |
| IE7 Análisis del mantenimiento | 20% | Comparar mantenimiento correctivo, preventivo, adaptativo y perfectivo. Priorizar acciones con responsables por rol, periodicidad, métricas y condiciones de revisión. | Sección 6 ampliada con mantenimiento y continuidad de calidad, seguridad y cumplimiento. |
| IE9 Documentación y mejora continua | 12% | Comunicar resultados, limitaciones, mitigaciones y evolución respecto de entregas previas, con trazabilidad. | Informe completo, anexos de evidencia, conclusiones y material de defensa. |

Los pesos suman 100%. Seguridad y automatización suman 54%; diagramas y mantenimiento también requieren entregables explícitos.

## Secuencia de trabajo

### Fase 1 Revisar y reproducir la base

1. Registrar revisión de Git, versiones de Java, Maven y Docker y condiciones del equipo.
2. Ejecutar las pruebas existentes de los nueve módulos y conservar resultados originales de Surefire, duración y errores.
3. Confirmar el conteo reportado de 244 pruebas; el valor del informe anterior no se tomará como ejecución actual.
4. Clasificar pruebas que verifican requisitos y pruebas que caracterizan defectos conocidos. Una prueba que confirma un defecto no acredita conformidad.
5. Revalidar los 21 incidentes anteriores sin asumir que todos sean vulnerabilidades explotables o que permanezcan abiertos.
6. Verificar rutas, comunicación entre servicios y orquestación. El Compose principal está en api-gateway/docker-compose.yml; el README referencia un archivo raíz que no está presente.

Salida: línea base ejecutada, inventario de evidencia y matriz de brechas.

### Fase 2 Diseñar y ejecutar pruebas globales

Preparar datos ficticios identificables, precondiciones, resultados esperados y aislamiento entre ejecuciones.

| Familia | Recorrido previsto | Requisitos principales |
| --- | --- | --- |
| Identidad | Registro corporativo, cuenta pendiente, rechazo de login, aprobación, login y acceso según rol. | RF-1.1, RF-1.2, RF-2.1 a RF-2.3, RF-3.1 a RF-3.3, RB-1 y RB-2 |
| Negocio | Crear producto y cliente con los roles correspondientes; registrar venta; verificar descuento de stock, compra del cliente y movimiento financiero. | RF-4.1 a RF-4.3, RF-5.1 a RF-5.4, RB-3 |
| Resiliencia | Indisponibilidad controlada de finanzas en el ambiente de prueba; venta pendiente, reproceso y estado final; revisar duplicación de movimientos. | RF-5.3 y RF-5.4 |
| Información ejecutiva | Consultar KPIs y alertas y descargar reportes PDF con datos conocidos; verificar generación periódica de alertas. | RF-6.1 y RF-6.2 |
| Validación | Entradas inválidas, recursos inexistentes, rangos y errores de servicios dependientes. | Criterios de aceptación del ERS y NFR-SEG-5 |

Las pruebas globales verificarán efectos persistidos además del código HTTP. La interrupción de servicios se hará únicamente en el entorno aislado.

### Fase 3 Ejecutar seguridad y analizar vulnerabilidades

- Autenticación: ausencia de token, token malformado, expirado o con firma alterada; claims requeridos y cuentas bloqueadas. Definir expresamente la política para tokens ya emitidos al bloquear cuentas.
- Autorización: matriz de roles por ruta y método, límites de dominios, rutas desconocidas y acceso a funciones administrativas.
- Exposición: respuestas con datos personales, campos sensibles, errores y logs. No conservar contraseñas o tokens en evidencias publicables.
- Entradas: validación server-side, límites, campos inválidos y payloads controlados de inyección. Guardar una cadena con SQL o HTML no demuestra por sí solo SQLi o XSS; se verificará el efecto real y el contexto de salida.
- Configuración: secretos fijos, inicialización administrativa, publicación de puertos, acceso directo a microservicios y protección de las comunicaciones.
- Análisis estático: aprovechar SonarQube si puede ejecutarse y explicar qué detecta y qué requiere revisión humana.
- Dependencias: incorporar una herramienta de composición de software si el entorno permite obtener su base de vulnerabilidades. Documentar versiones, estado de actualización y resultados.
- Análisis dinámico: usar pruebas HTTP dirigidas y evaluar OWASP ZAP para ampliar el análisis. Un escaneo sin autenticación no cubrirá por sí solo las rutas protegidas ni la lógica de negocio.

Por hallazgo registrar ID, origen, requisito, pasos, esperado, observado, evidencia, impacto, probabilidad, severidad, alternativas de mitigación, acción elegida y resultado de la reverificación. Conservar los IDs INC anteriores y asignar nuevos IDs sin reutilizarlos.

Cuando corresponda una corrección, capturar primero la falla del criterio esperado, implementar la mitigación y repetir la prueba junto con la regresión. No modificar aserciones para aceptar una brecha.

### Fase 4 Automatizar y cerrar pendientes no funcionales

1. Unificar la ejecución de los módulos con resultados y códigos de salida verificables.
2. Automatizar pruebas globales HTTP y validaciones de seguridad repetibles.
3. Completar mediciones de CRUD simple respecto del umbral de 300 ms. Registrar calentamiento, número de muestras, percentiles, errores y condiciones de carga; los percentiles serán una convención del plan, no un umbral inventado del ERS.
4. Probar 100 usuarios concurrentes según NFR-PERF-3, documentando duración, mezcla de operaciones y tasa de error. Acordar los criterios operativos antes de ejecutar; no confundir usuarios con solicitudes por segundo.
5. Realizar respaldo y restauración de las bases persistentes en destinos de prueba; verificar datos restaurados y tiempo de recuperación. Un archivo de respaldo creado no prueba restaurabilidad.
6. Configurar integración continua para ejecutar regresión y controles automáticos, conservar reportes y bloquear la promoción cuando fallen los criterios acordados.
7. Diferenciar la configuración de CI creada de una ejecución real en GitHub. Preparar los pasos de activación y evidencia para el estudiante si requieren interacción con su cuenta.
8. Tratar el despliegue continuo como diseño o propuesta mientras no se ejecute en un destino autorizado. No afirmar un CD operativo por disponer de un flujo de pruebas.

Herramientas base previstas: JUnit, Mockito, Maven/Surefire, JaCoCo, Docker y pruebas HTTP automatizadas. Se elegirá la herramienta de carga y los analizadores adicionales después de comprobar disponibilidad y compatibilidad. No son marcas obligatorias de la rúbrica.

Salida: ejecuciones repetibles, resultados estructurados y diagramas del proceso realmente implementado.

### Fase 5 Analizar mantenimiento y cumplimiento

El plan comparará las alternativas de mantenimiento y justificará el orden elegido por impacto y esfuerzo. Cada acción incluirá responsable por rol, frecuencia o disparador, evidencia y criterio de cumplimiento.

- Correctivo: mitigación de accesos indebidos y defectos de negocio, con reverificación y regresión.
- Preventivo: revisión de dependencias, secretos, logs, cobertura útil y restauración de respaldos.
- Adaptativo: cambios regulatorios, versiones de Java/Spring/PostgreSQL y contratos entre servicios.
- Perfectivo: claridad de errores, observabilidad y rendimiento medido.
- Operación: ventanas, migraciones, verificación posterior al despliegue y reversión segura.

La matriz normativa separará requisitos del ERS, políticas internas, referencias técnicas OWASP y obligaciones legales. Un checklist técnico automatizado aporta evidencia parcial; no certifica cumplimiento legal integral.

Verificar el marco aplicable en fuentes oficiales a la fecha de entrega. La BCN publica la Ley 19.628 y la Ley 21.719; también existe una propuesta gubernamental de modificación de plazos, por lo que se debe revisar el texto promulgado y vigente antes del cierre. Una propuesta no se tratará como ley aprobada.

Fuentes oficiales para esta revisión:

- https://www.bcn.cl/leychile/Navegar?dt=open&idLey=19628
- https://www.bcn.cl/leychile/Navegar?idNorma=1209272
- https://www.economia.gob.cl/2026/09/01/gobierno-propone-ampliar-plazo-para-implementar-nueva-ley-de-proteccion-de-datos-y-institucionalidad.htm

### Fase 6 Completar el informe y preparar la defensa

Preservar las seis secciones del formato entregado:

1. Introducción: sistema, continuidad de encargos previos, alcance y ajustes por feedback solo cuando exista.
2. Criterios de calidad: requisitos, seguridad, normativa y criterios medibles.
3. Plan de pruebas: estrategia, tipos, aceptación, herramientas, recursos y entorno. Incorporar diagramas y matriz de controles automatizados.
4. Diseño de casos: mantener ID, nombre, requisito, descripción, precondiciones, pasos, resultado esperado, criterio de aceptación y datos.
5. Ejecución: resumen, resultados por caso, evidencia, incidentes, fecha y ejecutor; agregar métricas de automatización y comparación antes y después.
6. Propuesta de mejora: mitigaciones, análisis de mantenimiento, seguimiento y recomendaciones para futuras versiones.

Agregar conclusiones, referencias y anexos como apoyo sin sustituir las secciones anteriores. No entregar el plan como si fuera el informe final.

Formato exigido: títulos Arial 12, cuerpo Arial 11, interlineado 1,5, texto justificado, lenguaje técnico formal y capturas legibles. Portada con nombres completos, carrera, sección, docente y fecha. Los datos de la portada anterior se comprobarán antes de cerrar.

La pauta exige entrega del informe y material de apoyo en AVA. Preparar una defensa de 10 minutos más 5 de preguntas; la defensa se evalúa individualmente aunque el trabajo sea grupal. Secuencia sugerida: contexto y alcance 1 minuto, seguridad 3, automatización y diagramas 3, mantenimiento y conclusiones 3. El material de presentación se elaborará después de obtener resultados verificables.

## Ajustes respecto de las entregas anteriores

- Separar cobertura de requisitos, conformidad y ejecución: son métricas distintas.
- El Encargo 2 describe 244 pruebas y presenta capturas, pero conserva texto que deja pendientes tres módulos. La nueva ejecución resolverá la inconsistencia sin atribuir resultados actuales a esas capturas.
- Recalcular la matriz: 18 requisitos funcionales + 6 reglas de negocio + 8 requisitos NFR-SEG-2 a NFR-SEG-9 + 2 de rendimiento son 34 ítems, aunque el informe anterior indica 35.
- Mantener identificados los pendientes de respaldo/restauración, 300 ms y 100 usuarios concurrentes hasta disponer de evidencia real.
- Reevaluar los incidentes según el contrato y la explotabilidad. Políticas de acceso esperadas, diferencias de escala BigDecimal y ausencia de cifrado en código requieren interpretación específica, no una clasificación automática como vulnerabilidad.
- Analizar cumplimiento con evidencia verificable; la evaluación documental previa no equivale a una auditoría legal completa.

## Criterios de cierre

- Cada caso del alcance tiene requisito, esperado, estado y evidencia identificada.
- Las cifras del resumen coinciden con los reportes reales; bloqueados, omitidos y no ejecutados aparecen por separado.
- Ningún hallazgo crítico pendiente se oculta con una prueba de caracterización aprobada. Si permanece abierto, el sistema no se declarará aceptado según el criterio de seguridad del ERS.
- Las mitigaciones implementadas tienen prueba anterior y posterior y regresión ejecutada.
- Diagramas, configuración, informe y comportamiento observado son coherentes.
- El mantenimiento tiene prioridades, responsables por rol, periodicidad y métricas.
- El informe respeta el formato y distingue resultados, propuestas y limitaciones.

## Participación del estudiante

La implementación, ejecución local y consolidación de evidencia avanzarán desde el repositorio. Se guiarán paso a paso las acciones que requieran al estudiante: activar o revisar GitHub Actions, validar datos académicos de portada, practicar la defensa y subir los archivos a AVA. No se necesita intervenir el frontend para comenzar.

## Avance del 6 de noviembre de 2026

Continuación del 8 de noviembre: restaurados los servicios existentes, diez métodos globales aprobados tras reinicio y SonarQube UP. Las matrices TRAZABILIDAD_REQUISITOS.md (34 ítems) y REVALIDACION_INCIDENTES.md (21 IDs anteriores e INC-22 nuevo) distinguen conformidad, evidencia parcial y pendientes. El informe y CI remoto siguen pendientes; la revisión documental no cierra los defectos identificados.

Actualización de cierre del bloque técnico: se implementaron las mitigaciones y se reverificaron 311 pruebas Java y diez métodos globales. Ver MITIGACIONES_Y_REVERIFICACION.md para la comparación SAST/SCA/DAST y los riesgos pendientes. La descripción siguiente registra el avance anterior a esta reverificación.

La línea base de 244 pruebas fue reproducida. Tras las primeras mitigaciones, la regresión completa aprobó 303 pruebas y la suite HTTP aprobó ocho casos globales con subcasos. Se ejecutaron mediciones de carga y restauración de las seis bases QA. El detalle y las limitaciones están en AVANCE_Y_EVIDENCIAS.md y los cambios por archivo en CAMBIOS_REALIZADOS.md.

La infraestructura QA, los scripts y el workflow de regresión están preparados. SAST y SCA se ejecutaron en nueve módulos y DAST autenticado en un alcance de nueve consultas; los resultados y límites están en ESCANEOS_SEGURIDAD.md. Continúan pendientes la revalidación completa de incidentes anteriores, revisión y mitigación de avisos, trazabilidad de todo el alcance, evidencia de CI en GitHub, capturas seleccionadas y el informe con defensa. El avance no equivale a la aceptación global del producto.
