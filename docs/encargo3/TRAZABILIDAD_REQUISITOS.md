# Trazabilidad de requisitos y evidencia

Revisión documental y de código: 8 de noviembre de 2026. Fuente: ERS Grupo Cordillera v1.4, secciones 3.2 a 3.5. El alcance local contiene **34 ítems: 18 RF, 6 RB, 8 NFR-SEG y 2 NFR-PERF**. La existencia de una prueba y su aprobación no equivalen a conformidad completa del requisito.

## Estados y referencias

- **V — Verificado en el alcance indicado:** evidencia positiva de los comportamientos citados; no implica verificación exhaustiva de entradas, infraestructura o escenarios futuros.
- **P — Parcial:** existe evidencia, pero queda una condición relevante del requisito sin verificar o una brecha conocida.
- **B — Brecha funcional confirmada:** comportamiento que contradice el objetivo funcional; una prueba de caracterización aprobada no lo cierra.
- **N — No acreditado:** falta evidencia suficiente para declarar el requisito satisfecho.

Referencias relativas a evidence/encargo3:

| Clave | Evidencia y alcance |
| --- | --- |
| J | baseline/20261107T011753840Z: 311 pruebas Java aprobadas en nueve módulos; consultar summary.json y XML por módulo. Ejecutadas el 6 de noviembre local; no se repitieron el 8 porque no cambió el código Java. |
| S | system/20261107T012533650156Z/results.json: diez métodos HTTP aprobados antes del reinicio. |
| R | system/20261108T185501675626Z/results.json: diez métodos HTTP aprobados tras restablecer Docker, duración unittest 57,385 s. Usa datos ficticios nuevos; SYS-07 interrumpe y recupera únicamente finanzas QA. |
| L | load/20261106T202058129403Z/results.json: consulta GET clientes, 50 muestras normales y 500 solicitudes con cien clientes concurrentes. Anterior a la actualización de dependencias. |
| D | backup/20261106T202231086921Z/results.json: restauración y comparación de filas de las seis bases. Anterior a la actualización de dependencias. |
| A | security/sonar/20261107T012132927082Z: SAST y cobertura; security/dependencies/20261107T012132960828Z: SCA de ejecución; security/zap/20261107T012612209161Z: nueve consultas GET autenticadas. |

Las suites se identifican por nombre de clase bajo el módulo señalado. Las limitaciones de cada escáner están en MITIGACIONES_Y_REVERIFICACION.md. Los controles enumerados a continuación son técnicos; no certifican cumplimiento legal.

## Requisitos funcionales

| ID | Comportamiento del ERS | Prueba o revisión vinculada | Control | Estado y pendiente |
| --- | --- | --- | --- | --- |
| RF-1.1 | Login con email/contraseña emite JWT con rol | J authentication/AuthServiceTest; gateway/GatewayJwtSecurityTest; R SYS-02 y preparación de usuarios | Credenciales, firma y claims | V: login real y acceso por rol; pruebas de tokens alterados/expirados en Java. |
| RF-1.2 | Rechazo de login pendiente/rechazado/bloqueado con mensaje específico | J AuthServiceTest; R SYS-01 | Estado de cuenta | P: los tres estados se prueban en servicio; HTTP comprueba 4xx pendiente, sin acreditar el mensaje específico de todos los estados. |
| RF-2.1 | Registro crea cuenta pendiente | J AuthServiceTest; R SYS-01/02 | Aprobar antes de acceder | V: creación y rechazo previo a aprobación. |
| RF-2.2 | Solo correo del dominio corporativo | J AuthServiceTest.registrarUsuarioConCorreoNoCorporativoFalla; revisión AuthService/AuthExceptionHandler | Restricción de registro | P: rechazo unitario; INC-05 sobre contrato HTTP permanece pendiente. |
| RF-2.3 | ADMIN_USUARIOS/SUPER_ADMIN aprueba con rol o rechaza | J AuthServiceTest; R SYS-02 con SUPER_ADMIN | Autorización administrativa | P: aprobación real y rechazo unitario; no recorrido HTTP completo de ambos actores y todas las transiciones. |
| RF-3.1 | Listar pendientes y total de usuarios | J AuthServiceTest; R preparación de usuarios y consultas administrativas SYS-02 | Minimización de respuesta y permisos | P: listado de pendientes usado realmente; completar el recorrido total con ADMIN_USUARIOS. |
| RF-3.2 | Bloquear y desbloquear otra cuenta | J AuthServiceTest; revisión AuthService | Gestión del ciclo de cuenta | P: servicio probado; falta recorrido HTTP completo y decisión sobre tokens previamente emitidos. |
| RF-3.3 | Matriz centralizada rol/dominio/método | J JwtAuthFilterTest/JwtAuthFilterSecurityTest/GatewayJwtSecurityTest; R SYS-02/04 | Denegación por defecto y límites de rutas | V: matriz Java y subcasos HTTP; no acredita control de llamadas que eviten el Gateway. |
| RF-4.1 | Crear cliente con campos válidos y correo único | J ClientesServiceTest/GlobalExceptionHandlerTest; R SYS-03/05 | Validación y errores controlados | P: alta válida, entrada inválida y duplicados unitarios; no todos los campos/límites ni defensa en cada capa. |
| RF-4.2 | Listar y consultar por tipo/estado | J ClientesServiceTest; R GET clientes en SYS-02/03 | Lectura autorizada | P: listado real y filtros de servicio; faltan filtros HTTP completos. |
| RF-4.3 | Compra actualiza cantidad, monto y clasificación | J ClientesServiceTest; R SYS-03 | Integridad entre servicios | V en ECOMMERCE: compra y monto persistidos; clasificación en Java. La expectativa de actualización para POS debe aclararse. |
| RF-5.1 | Registrar venta con producto y cliente opcional | J VentasServiceTest; R SYS-03/07 | Autorización e integridad | V para flujos documentados; no es cobertura de todas las combinaciones de datos. |
| RF-5.2 | Descontar stock al vender | J VentasServiceTest/CantidadVentaTest/InventarioServiceTest/CantidadStockTest; R SYS-03/07/08 | Evitar cantidades no positivas y doble descuento | V: efectos reales y rechazo de cantidades inválidas. No se ha demostrado corrección bajo ventas concurrentes sobre el mismo stock. |
| RF-5.3 | Sincronizar finanzas, tolerar falla y limitar intentos | J FinanzasSyncServiceTest; R SYS-07 | Reintento y recuperación | P: falla/recuperación real y límite ERROR unitario; falta alcanzar el límite mediante HTTP con indisponibilidad sostenida. |
| RF-5.4 | Exponer PENDIENTE/SINCRONIZADO/ERROR | J VentasServiceTest/FinanzasSyncServiceTest; R SYS-07 | Visibilidad de fallos | P: PENDIENTE/SINCRONIZADO reales, ERROR probado en Java. |
| RF-5.5 | Gestionar productos por categoría y atributos propios | J ProductoFactoryTest/InventarioServiceTest; R creación de producto SYS-03/08 | Contrato de categoría | P: recorrido de tecnología; INC-15/16 y atributos de ambas categorías requieren revisión contractual y escenarios adicionales. |
| RF-6.1 | Detectar periódicamente stock crítico, ventas bajas y margen anómalo | J DeteccionAlertasServiceTest; revisión @Scheduled y @EnableScheduling | Monitoreo automático | B: INC-03 omite sucursales con ventas anteriores y ninguna actual; falta prueba global de periodicidad y aclarar margen anómalo frente a margen normalizado. |
| RF-6.2 | Consultar alertas/KPIs y generar/descargar PDF | J AlertaServiceTest, calculadoras KPI y ReporteServiceTest; R SYS-06; A ZAP | Información ejecutiva y acceso | P: endpoints y tres firmas PDF comprobados; no se compara integralmente el contenido PDF con datos esperados ni se cierran INC-08/12. |

## Reglas de negocio

| ID | Regla | Evidencia | Estado y límite |
| --- | --- | --- | --- |
| RB-1 | Un rol activo por usuario | J AuthServiceTest; Usuario.Rol y campo único; R SYS-02 | V: asignación única y claims utilizados. |
| RB-2 | Solo APROBADO inicia sesión | J AuthServiceTest, tres estados rechazados; R SYS-01/02 | V en los casos probados; no implica revocación automática de JWT ya emitidos. |
| RB-3 | Una venta tiene un producto y cliente opcional | J VentasServiceTest; R SYS-03/07 | V para ventas del alcance y modelo actual. |
| RB-4 | EJECUTIVO/ANALISTA solo leen dominios de negocio | J JwtAuthFilterTest; R SYS-02 | V sobre ventas, inventario, finanzas y clientes; los permisos específicos de alertas/reportes se documentan aparte. |
| RB-5 | ADMIN escribe únicamente en su dominio | J matriz JwtAuthFilterTest; R SYS-02 y creación de negocio | V en dominios implementados; no acredita rutas administrativas futuras. |
| RB-6 | SUPER_ADMIN accede a todos los dominios | J JwtAuthFilterTest/JwtAuthFilterSecurityTest; R SYS-02/07 | V en dominios declarados; una ruta desconocida se deniega y no cuenta como dominio autorizado. |

## Seguridad y rendimiento

| ID | Requisito | Evidencia/control | Estado y condición pendiente |
| --- | --- | --- | --- |
| NFR-SEG-2 | Hash seguro de contraseñas | J AuthServiceTest con BCrypt real y DataInitializerTest; PasswordEncoder | V: hash y comprobación; no se publican contraseñas QA. |
| NFR-SEG-3 | Cifrar datos sensibles en reposo | Revisión de Compose/entidades; INC-20 | N: no se obtuvo evidencia de cifrado de disco/volumen/columnas. Ausencia de cifrado en entidad no basta para concluir el estado de infraestructura. |
| NFR-SEG-4 | Control centralizado de acceso | J GatewayJwtSecurityTest y matriz; R SYS-02/04 | V a través del Gateway; los microservicios QA sin puertos publicados no prueban la configuración de otro despliegue. |
| NFR-SEG-5 | Validación y protección CSRF/XSS/SQLi | R SYS-05/08/09; J validaciones y filtros; A DAST limitado | P: rechazo de entradas concretas y cabeceras; INC-07/19 parciales. Guardar SQL/HTML no demuestra ejecución SQLi/XSS. No se evaluó el contexto de salida frontend ni todos los vectores CSRF. |
| NFR-SEG-6 | Logs sin contraseñas/tokens y gestión segura | J DeteccionLoggingTest; R SYS-10; revisión de alertas y saneamiento de reportes | P: debug retirado y logs acotados; falta política verificable de acceso, retención, centralización y revisión de logs de todos los servicios. |
| NFR-SEG-7 | Política de respaldo/recuperación por base | D seis bases restauradas y comparadas; MANTENIMIENTO_Y_CONTROLES.md | P: restaurabilidad probada y política propuesta; faltan aprobación operativa, retención externa, cifrado y RPO/RTO comprometidos. |
| NFR-SEG-8 | Cumplimiento de protección de datos del ERS | Revisión técnica y plan de mantenimiento | N: no consta auditoría de finalidades, bases de tratamiento, derechos, retención y responsabilidades. Los tests no certifican cumplimiento legal. |
| NFR-SEG-9 | Sesión mediante JWT sin estado | J GatewayJwtSecurityTest; R SYS-02/04; authentication/SecurityConfig STATELESS | P: JWT real y configuración sin sesión; completar política de vigencia/revocación tras bloqueo y evaluación integral de cookies/sesiones. |
| NFR-PERF-1 | CRUD simples <300 ms con carga normal | L GET clientes: máximo 38,28 ms en 50 muestras | P: satisface esa consulta y entorno anteriores; no todos los CRUD/microservicios ni la versión posterior de dependencias. |
| NFR-PERF-3 | Soportar cien usuarios concurrentes | L cien clientes, 500 solicitudes, cero errores; máximo 6142,63 ms | P: cien clientes HTTP de lectura, sin mezcla completa de usuarios/operaciones ni objetivo de latencia acordado para concurrencia. |

## Exclusiones explícitas del ERS 3.5

NFR-SEG-1 (TLS), NFR-PERF-2 (dashboard), NFR-USAB-1 a 4, NFR-DIS-1/2 y NFR-COMPAT-1/2 están fuera de la verificación ejecutable local definida en el ERS. No se marcan como aprobados ni se suman a los 34 ítems. Las ventanas de mantenimiento se proponen documentalmente. El frontend sigue sin modificaciones.

## Criterio para el informe

Reportar estados por requisito y evidencia, separándolos de los conteos de pruebas y la cobertura de líneas. No presentar 311/311 o diez casos globales como 100 % de conformidad. El producto todavía tiene brechas funcionales y evidencia pendiente; no corresponde declarar aceptación global sin resolverlas o documentar la decisión y el alcance académico de los riesgos residuales.
