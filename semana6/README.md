# Proyecto final · Semana 6 · GitHub Copilot


**Alumno:** `Hilario Sebastian Espinoza Garcia` · **Usuario de GitHub:** `SebasKaze`

## 1. Qué construí



| | Feature | Especificación |
|---|---|---|
| [ X ] | `GET /tasks/search?q=` — buscar tareas por título | [`specs/search.md`](../specs/search.md) |


## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/SebasKaze/taskflow-copilot-SebasKaze/pull/5`
- **Commit del merge en `main`:** `a9cf3bc (HEAD -> main, origin/main, origin/HEAD) Merge pull request #5 from SebasKaze/feature/search`
- **Comentarios de Copilot code review:** `1`

## 3. Cómo lo hice



| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/search` y copié la spec a `specs/` | `git log --oneline main..feature/search` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto:`) |
| Tests | `mvn test` en verde | `RESULTADO: 14/14 OK` |
| Comprobación REST | `verificar.ps1` con `casos-search.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo



| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `No bloqueante: specs/search.md:55–58 pide @MockitoBean, como el slice existente src/test/java/com/taskflow/slice/TaskControllerTest.java:21,67–75. Este test usa @MockBean en su lugar.` | `yo` | `prompt de corrección` |

**Lo que el agente hizo bien a la primera** (una o dos líneas): `El PF-2, se autocorrigio en 2 ocasiones `

## 5. Comprobaciones REST


```text
Repositorio: C:\Users\hilar\taskflow-copilot-SebasKaze
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 14068). Esperando a que /info responda...
App lista en 5 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /tasks/search?q=api devuelve 9 y 5, en ese orden
[OK]    GET /tasks/search?q=API devuelve lo mismo (no distingue mayúsculas)
[OK]    GET /tasks/search?q=zzz devuelve 200 con []
[OK]    GET /tasks/search?q=(espacios) responde 400 con el mensaje de la spec
[OK]    GET /tasks/search sin q responde 400 con el mensaje de la spec
[OK]    GET /tasks/search sin token responde 401
App detenida (PID 14068).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 14/14 OK
```

## 6. Créditos de la semana


| Qué | AI credits |
|---|---|
| Usados en octubre según github.com (incluye semanas anteriores si usaste Copilot antes) | `473.69 credits` |
| Implementación con la skill (`AI Credits` del PF-2) | `AI Credits 5.48` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `AI Credits 2.03` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `AI Credits 1.57` |
