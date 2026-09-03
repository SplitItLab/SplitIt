---
name: reparar-secuencia-de-prs
owner: "@lucasmonteverdi1"
role: tl
created: 2026-09-03
description: Repara una secuencia de dos PRs dependientes mergeadas en orden incorrecto cuando un revert hizo desaparecer funcionalidad de main, sin reescribir la historia. Usar cuando una PR desapareció tras un revert o cuando reabrir su rama vieja no trae los cambios.
---

# Reparar secuencia de PRs

Repara el caso en que dos Pull Requests debían mergearse a la rama
principal en un orden específico (por dependencia funcional, no solo
cronológica) pero se mergearon al revés, y alguien ya intentó corregirlo
revirtiendo una de ellas. El síntoma típico: una PR que sabías mergeada ya
no está en el código, o al intentar volver a mergear su rama, git no trae
ningún cambio (o trae conflictos que no tienen que ver con el contenido
real).

No asume ningún repositorio, lenguaje ni ticket en particular. Corre en
**el repo de aplicación** donde ocurrió el mergeo, no en el catálogo de
skills.

## Cuándo no usarla

Si la PR a recuperar nunca se mergeó (sigue abierta, o se cerró sin
mergear), esto no es un problema de orden — es simplemente mergearla ahora;
no hace falta esta skill. Tampoco aplica si el "orden incorrecto" es solo
una preferencia estética sobre el historial (squash vs merge commit, orden
de aparición en el log) sin pérdida de funcionalidad real: ahí no hay nada
que reparar. Si no hubo un revert ni hay funcionalidad perdida, tampoco hay
una reparación que hacer.

Si no hay manera de determinar el orden correcto (nadie sabe cuál PR
dependía de cuál), preguntar antes de asumir uno.

## Invocación

```
/reparar-secuencia-de-prs
/reparar-secuencia-de-prs <PR-o-rama-A> <PR-o-rama-B>
/reparar-secuencia-de-prs --dry-run
```

`A` y `B` identifican las PRs o ramas afectadas, en el orden correcto
deseado (A antes que B). Si se omiten, la skill los infiere del historial
(paso 1) y pide confirmación explícita antes de tocar nada. `--dry-run` corre los pasos
1-3 (diagnóstico y plan) y termina: no crea ramas, no commitea, no pushea.

## Procedimiento

### 1. Reconstruir qué pasó

Sobre la rama principal (`main`/`master`/`develop`, la que el repo use):

```
git log --oneline --graph --all
git log --oneline --merges
```

Identificar, en orden cronológico real:

- Los commits/merges de cada PR involucrada (`git log --grep`, o
  `gh pr view <N> --json mergeCommit,commits` si hay GitHub CLI y remoto).
- Si alguna PR fue revertida (`git log --grep="Revert"` o buscar el
  mensaje), y qué commit exacto reasegura el revert — `git show
  --stat <revert>` muestra qué archivos tocó.
- Si tras el revert se mergeó otra rama que además tocó archivos
  compartidos con la PR revertida (superposición de archivos entre
  `git show --stat` del revert y de la otra PR).

Armar una línea de tiempo explícita: qué se mergeó, en qué orden, qué se
revirtió, qué se mergeó después. Mostrarla antes de seguir — es la base de
todas las decisiones siguientes.

### 2. Confirmar el orden correcto y qué se perdió

El orden correcto es el que declaró quien pidió la skill (parámetros `A`
`B`) o el que surge de la dependencia funcional real (ej. una pantalla de
login que usa rutas creadas por la PR de registro depende de que esa PR
exista primero). Si no es evidente por el código ni por lo que dijo el
usuario, preguntar — no asumir por orden cronológico de apertura de PR.

A partir del revert identificado en el paso 1
(`git show --stat <revert-commit>`), listar los archivos y funcionalidad
que se perdieron. Esto es lo que hay que restaurar — ni más ni menos: no
es "traer la rama entera de nuevo" si parte de esos cambios ya fueron
reintroducidos de otra forma por PRs posteriores.

### 3. Elegir la estrategia de recuperación y mostrarla

Dos vías posibles; elegir la que restaure exactamente la funcionalidad
perdida:

| Vía | Cuándo aplica |
|---|---|
| **Revert del revert** (`git revert <revert-commit>`) | Hay un revert explícito y hay que restaurar íntegramente lo que deshizo. Es la operación más directa; los cambios posteriores que se superpongan se resuelven con el criterio del paso 6. |
| **Cherry-pick de commits originales** | La restauración debe ser parcial porque cambios de la PR ya fueron reintroducidos de otra forma, o porque el revert-del-revert reintroduciría paths que no deben volver. Usar los commits identificados, incluido el primero (`<primer-commit>^..<último-commit>`). |

Mostrar la línea de tiempo, la funcionalidad a restaurar y la vía elegida,
y pedir confirmación explícita antes de ejecutar. `--dry-run` termina acá.

### 4. Preparar el terreno

Confirmar working tree limpio (`git status`); si hay cambios sin commitear,
parar y avisar — no stashear ni descartar nada por cuenta propia.

Si la rama principal tiene remoto de tracking, correr `git fetch --prune` y
confirmar que el HEAD local es la base actual del remoto antes de crear la
rama. Registrar ese SHA como base de la reparación.

Crear una rama nueva desde el HEAD actual de la rama principal, con nombre
descriptivo (ej. `fix/reordenar-merge-<A>-<B>`). Nunca reescribir la rama
principal directamente ni hacer force-push sobre ella.

### 5. Ejecutar la vía elegida

**Revert del revert:**

```
git revert <revert-commit> --no-commit
```

Revisar el resultado antes de commitear y, si no hay conflictos pendientes:

```
git diff --cached --check
git diff --cached
git commit -m "fix: restaurar cambios revertidos de <PR-o-commit>"
```

**Cherry-pick:**

```
git cherry-pick <primer-commit>^..<último-commit>
```

Si la PR original tenía sus propios merge commits internos, usar
`-m 1` en esos puntos o aplanar a los commits no-merge reales — lo que
mantenga el historial legible sin perder cambios.

En cualquier vía, si git para por conflicto, seguir al paso 6 antes de
continuar (`git revert --continue` o `git cherry-pick --continue`).

### 6. Resolver conflictos con criterio

Para cada archivo en conflicto:

1. Leer ambos lados del conflicto (`git diff` marca `<<<<<<<`/`>>>>>>>`) y
   entender qué intención tenía cada uno — no solo el texto, el propósito
   funcional de cada cambio (identificado en el paso 2).
2. Si los dos cambios son compatibles (tocan la misma zona pero no se
   excluyen — ej. dos imports distintos agregados a la misma lista, dos
   entradas de config independientes), **unir ambos**, preservando la
   funcionalidad completa de los dos lados. Esto incluye conflictos
   mecánicos: lockfiles (regenerar corriendo el instalador del gestor de
   paquetes en vez de editar a mano), archivos generados, formato.
3. Si los dos cambios son mutuamente excluyentes (mismo comportamiento
   implementado de dos formas distintas, uno claramente reemplaza al
   otro), priorizar la versión que corresponde al estado **final**
   deseado según el orden correcto del paso 2 — no la que "ganó"
   cronológicamente por accidente del mal orden de merge.
4. Si no se puede determinar con confianza cuál de las dos formas es la
   correcta (ambigüedad real de negocio, no técnica), detener la
   reparación: registrar el archivo y ambos lados del conflicto, abortar la
   operación (`git revert --abort`, `git cherry-pick --abort` o `git merge
   --abort`) y pedir una decisión al equipo. No dejar marcadores de
   conflicto, no commitear y no pushear una reparación ambigua.

Registrar cada conflicto resuelto: archivo, qué decisión se tomó, por qué
(criterio del punto 2 o 3). Esta lista alimenta el informe final.

Tras resolver todos los archivos de un conflicto, continuar la operación
en curso (`--continue`) y seguir con el siguiente commit si aplica.

### 7. Verificar

Confirmar que la funcionalidad de **ambas** PRs (A y B) está presente y
funciona:

- Si el repo tiene build/compilación, correrlo.
- Si el repo tiene tests automatizados que cubran alguna de las dos PRs,
  correrlos.
- Revisar manualmente (`git show`/`git diff` contra la rama principal
  original) que ningún archivo de la PR B — la que ya estaba bien
  mergeada — perdió contenido en el proceso.

Si algo no compila o un test falla, diagnosticar antes de asumir que el
merge está mal: puede ser un conflicto mal resuelto (volver al paso 6) o
un problema preexistente no relacionado (anotarlo, no forzar un fix fuera
de alcance).

### 8. Entregar

No pushear a la rama principal directamente. Dejar la rama nueva lista y
pusheada (`git push -u origin <rama-nueva>`), y si hay CLI de PRs
disponible (`gh`, `glab`, etc.) abrir una PR contra la rama principal con
el informe de abajo en la descripción. Si no hay CLI o remoto configurado,
entregar el informe en el chat con el nombre de la rama para que el dev la
revise y la abra manualmente.

```
## Reparación de secuencia de PRs

**Orden restaurado:** <A> → <B>

**Vía usada:** revert-del-revert | cherry-pick — <por qué>

**Conflictos resueltos**
- <archivo>: <decisión> — <criterio>
- … | ninguno

**Verificación**
- build/compilación: ok | FAIL — <síntoma>
- tests: ok | FAIL — <síntoma> | no hay tests aplicables
- contenido de la PR B intacto: sí | no — <qué se perdió>
```

## Fuera de alcance

Decidir el orden correcto cuando nadie lo sabe (preguntar, no inventar).
Reescribir historia ya pusheada a la rama principal (`push --force` sobre
`main`/`master`). Resolver conflictos semánticamente ambiguos sin
señalarlos. Cambios de funcionalidad más allá de restaurar lo perdido —
si al reparar el merge aparece la tentación de "aprovechar y mejorar"
algo, eso es un cambio aparte, fuera de esta skill.
