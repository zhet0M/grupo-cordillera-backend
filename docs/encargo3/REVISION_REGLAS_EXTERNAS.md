# Revisión de reglas externas de programación

Fecha: 6 de noviembre de 2026.

## Conclusión

No se identificaron instrucciones maliciosas evidentes en las reglas de Java, Spring Boot, seguridad y alcance leídas en detalle. El examen estático general tampoco identificó, mediante los patrones utilizados, comandos de extracción de credenciales, descarga y ejecución encadenadas o desactivación de TLS en los archivos de reglas. Esto no certifica que todo el repositorio sea seguro ni demuestra ausencia de actividad maliciosa histórica.

Es adecuado utilizar una selección revisada como referencia de programación. No se recomienda cargar toda la colección ni ejecutar sus herramientas de mantenimiento para este proyecto.

El repositorio contiene reglas Cursor en formato .mdc; la copia revisada no contiene ningún SKILL.md. No se instaló una skill de Codex ni se activaron reglas automáticamente.

## Copia examinada

- Repositorio: https://github.com/PatrickJS/awesome-cursorrules
- Commit obtenido de la API pública de GitHub: b044f956f021b6e8877f16781bcfc466a6a120e9
- Fecha del commit declarada por GitHub: 2026-05-30T18:01:29Z.
- SHA256 del ZIP descargado: ec178c1069271e2e4afafd6700ac2fbb7e391421fec41b965ceac54d2b1b4072.
- Inventario: 295 archivos, incluidos 257 .mdc, 19 .mjs y 5 PNG. No se encontraron archivos .gitmodules ni scripts SKILL.md.
- La copia y el inventario por archivo se conservaron en tmp/security-review/awesome-cursorrules, fuera del repositorio backend.

El hash identifica lo examinado; no constituye una firma de confianza ni valida al autor.

## Método y límites

1. Consultar el repositorio público y descargar un archivo por commit fijo desde GitHub. Validar rutas y límites de tamaño antes de escribir su contenido como archivos locales.
2. Inventariar extensiones y calcular hashes sin ejecutar los archivos descargados.
3. Buscar patrones en todos los archivos de reglas para lectura de credenciales, ejecución remota, persistencia, desactivación de controles y sustitución de instrucciones superiores. Revisar coincidencias en su contexto.
4. Leer íntegramente las reglas seleccionadas de Java/Spring, Java general, seguridad, alcance, pautas de código y revisión de cambios.
5. Examinar package.json, lockfile, workflow y scripts relevantes a comunicaciones, escritura y borrado.

No se ejecutaron npm, pnpm, los scripts del repositorio ni sus pruebas. No se hizo análisis antivirus de imágenes, revisión semántica completa de las 257 reglas, auditoría de todas las versiones históricas, análisis de dependencias transitivas de herramientas remotas ni auditoría de servicios enlazados. Los enlaces publicitarios y de terceros no se consideran aprobados por esta revisión.

## Observaciones de seguridad y compatibilidad

| Archivo | Observación | Evaluación y decisión |
| --- | --- | --- |
| package.json y pnpm-lock.yaml | No declaran dependencias de aplicación ni hooks preinstall, install, postinstall o prepare. Existe un comando explícito pnpm dlx awesome-lint@2.3.0. | No se observó instalación automática maliciosa. El comando dlx descarga y ejecuta código externo; no es necesario para consumir las reglas y no se ejecutó. |
| scripts/check-pr-author.mjs | Lee GITHUB_TOKEN o GH_TOKEN del entorno y, si existe, lo utiliza como Authorization al consultar api.github.com/users. | Uso coherente con comprobar autores de contribuciones. No se observó un destino de recolección ajeno a GitHub en esa función. No se ejecutó ni se le proporcionaron tokens. |
| scripts/cleanup-legacy-artifacts.mjs | Puede eliminar recursivamente rules-legacy, rules-new y .cursorrules bajo la raíz indicada; valida los destinos calculados contra esa raíz. | Operación destructiva explícita de mantenimiento, no necesaria para el backend. No se ejecutó. La validación de rutas no equivale a una auditoría de todas las variantes de enlaces del sistema de archivos. |
| scripts/convert-readme-links.mjs | Reescribe README.md o el archivo indicado mediante --file. | Operación de mantenimiento con escritura explícita; no se ejecutó. |
| .github/workflows/main.yml | Declara contents: read, acciones fijadas por SHA y persist-credentials: false. En pull_request_target ejecuta verificadores de la base y trata el contenido de la contribución como entrada. | Son medidas favorables observadas. La presencia del workflow no prueba que esté habilitado ni que todas las ejecuciones pasadas fueran seguras. No se copió al backend. |
| scripts/check-repo-security.test.mjs | Contiene ejemplos de exfiltración, hosts attacker.invalid y comandos inseguros dentro de datos de prueba. | Su contexto es verificar detectores de instrucciones peligrosas. No se interpretaron esos ejemplos como instrucciones ejecutables. |
| rules/nestjs-anti-hallucination-cursorrules-prompt-file.mdc, línea 8 | Afirma que sus reglas sustituyen todo otro comportamiento de generación. | Afirmación de prioridad excesiva, sin prueba de exfiltración. No corresponde a Java/Spring y no se adoptará. Nunca sustituye las instrucciones del usuario o del entorno. |
| rules/code-guidelines-cursorrules-prompt-file.mdc | Incluye una regla de no resumir cambios y otras restricciones de comunicación. | Contradice la solicitud del estudiante de informar archivos y modificaciones. Se descarta esa restricción. |
| rules/java-general-purpose-cursorrules-prompt-file.mdc | Propone Java 24 y bibliotecas adicionales. | No compatible con imponer cambios al proyecto Java 17. No se adoptarán esas versiones ni dependencias por la mera existencia de la regla. |
| rules/java-springboot-jpa-cursorrules-prompt-file.mdc | Propone Spring Boot 3, inyección por campos, interfaces ServiceImpl obligatorias y una envoltura de respuestas. Sus ejemplos de ApiResponse no son consistentes entre sí. | No copiar literalmente. El proyecto utiliza Spring Boot 4.0.6 y contratos existentes. Se preservarán su versión, arquitectura y respuestas salvo necesidad justificada por los requisitos. |

La búsqueda propia detectó la afirmación de prioridad en la regla de NestJS y no detectó caracteres de control bidireccional en los archivos de texto examinados. Las búsquedas por patrones son auxiliares y pueden producir falsos negativos.

## Referencias que se utilizarán en este trabajo

Las siguientes reglas se consultan como referencia externa revisada y subordinada a las instrucciones del estudiante, el ERS y el entorno; no como una skill instalada:

- Java y Spring Boot: separar responsabilidades de controlador, servicio y persistencia; validar entradas; utilizar transacciones cuando corresponda. Adaptar ejemplos a Java 17 y Spring Boot 4.0.6.
- Seguridad: no introducir secretos fijos ni registrarlos en logs; emplear consultas parametrizadas, hash seguro, autorización y mínimo privilegio; documentar controles y sus límites.
- Alcance: implementar cambios pequeños y justificados, conservar funcionalidades ajenas al encargo y evitar dependencias innecesarias.
- Revisión: contrastar aserciones y comportamiento real; citar archivos para los hallazgos; diferenciar riesgos confirmados y pendientes de verificación.
- Comunicación solicitada por el estudiante: antes de cada bloque de cambios explicar su propósito; después indicar por archivo qué se agregó, modificó o quitó y qué se verificó.

No se incorporarán herramientas de terceros, servicios MCP, despliegues, cambios globales del equipo o acciones destructivas solo porque aparezcan sugeridos en otra regla de la colección.

## Fuentes fijadas a la copia revisada

- [Paquete y comandos](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/package.json)
- [Regla de Java y Spring](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/rules/java-springboot-jpa-cursorrules-prompt-file.mdc)
- [Regla de seguridad](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/rules/security-devsecops-ssdls-appsec.mdc)
- [Regla de alcance](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/rules/anti-overengineering.mdc)
- [Pautas de código](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/rules/code-guidelines-cursorrules-prompt-file.mdc)
- [Workflow](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/.github/workflows/main.yml)
- [Limpieza de archivos](https://github.com/PatrickJS/awesome-cursorrules/blob/b044f956f021b6e8877f16781bcfc466a6a120e9/scripts/cleanup-legacy-artifacts.mjs)

Esta revisión debe repetirse antes de incorporar nuevas reglas o actualizar la copia.
