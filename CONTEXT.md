# Capacitación

# **Proyecto de capacitación: Sistema de Biblioteca**

## **Contexto**

Este es un proyecto de práctica de 1 semana, pensado para que se suelten con el stack técnico del proyecto principal antes de empezar a escribir código.

## **Qué van a construir**

Un sistema simple de préstamo de libros de una biblioteca, con dos roles: **associate** (pide libros prestados) y **librarian** (gestiona el catálogo y las devoluciones).

### **Entidades principales**

* **User**: persona con rol `ASSOCIATE` o `LIBRARIAN`  
* **Book**: título, autor, cantidad total de copias, copias disponibles  
* **Loan**: vincula un libro con un socio, fecha de préstamo, fecha límite de devolución, fecha de devolución real (nula si sigue prestado)

## **Funcionalidades a implementar**

### **Como socio**

1. Registrarme e iniciar sesión  
2. Ver el catálogo de libros con su disponibilidad  
3. Pedir prestado un libro (si hay copias disponibles)  
4. Ver mis préstamos activos y su fecha límite  
5. Devolver un libro

### **Como bibliotecario**

1. Iniciar sesión  
2. Dar de alta, editar y eliminar libros del catálogo  
3. Ver todos los préstamos activos del sistema  
4. Ver un reporte de préstamos vencidos (fecha límite pasada y sin devolver)  
5. Registrar la devolución de un libro en nombre de un socio

## 

## **Reglas de negocio**

Estas son las validaciones que le dan sentido al ejercicio, no es solo CRUD:

* No se puede prestar un libro si no hay copias disponibles.  
* Al prestar un libro, las copias disponibles se disminuyen en 1; al devolverlo, se vuelve a sumar.  
* Un socio no puede tener el mismo libro prestado dos veces al mismo tiempo.  
* La fecha límite de devolución se calcula automáticamente (por ejemplo, 14 días desde el préstamo), no la carga el usuario a mano.  
* Un préstamo con fecha límite vencida y sin devolver aparece en el reporte de vencidos del bibliotecario.

## **Stack a usar**

| Capa | Tecnología |
| :---- | :---- |
| Front-end | Next.js (App Router) \+ React |
| Back-end | Spring Boot \+ Spring Data JPA |
| Autenticación | Spring Security \+ JWT |
| Base de datos | PostgreSQL vía `docker-compose` local |
| Testing | Tests unitarios (JUnit) \+ Playwright (E2E) |

## **Requerimientos técnicos**

* **Auth:** login con JWT. Los endpoints de gestión de catálogo y reportes solo accesibles para `LIBRARIAN`; el resto, para cualquier usuario autenticado.  
* **Persistencia:** entidades mapeadas con JPA, relaciones entre Usuario, Libro y Préstamo bien modeladas (no todo en una sola tabla).  
* **Validaciones de negocio en el back-end**, no solo en el front — el front puede prevenir errores, pero el back tiene que rechazar operaciones inválidas igual.  
* **Al menos 2 tests unitarios** sobre las reglas de negocio (ej: no prestar sin stock, no duplicar préstamo).  
* **Al menos 1 test E2E con Playwright** que cubra el flujo completo: login → ver catálogo → pedir préstamo → ver mi préstamo.

## 

## 

## **Entregables**

Al final de la capacitación, entregan:

1. Repositorio con el código (front \+ back)  
2. `docker-compose.yml` funcional para levantar el backend y Postgres local  
3. Instrucciones cortas de cómo correr el proyecto (README)  
4. Los tests unitarios y el test E2E pasando

# Conceptos importantes

## **Conceptos básicos por tecnología**

Antes de entrar en las recomendaciones, un repaso rápido de los términos que van a aparecer todo el tiempo. Si alguno les suena a chino es normal, para eso está esta sección y las capacitaciones.

## **Conceptos básicos: Docker**

Este es el más nuevo y el que más dudas les va a generar. Pregunten todo lo que necesiten sobre esto. 

* **Imagen:** una plantilla inmutable con todo lo necesario para correr una app (código, dependencias, runtime). Se construye una vez a partir de un `Dockerfile` y después se puede levantar tantas veces como quieran, siempre igual. Que sea inmutable significa que cada vez que hagan un cambio en su código van a tener que rebuildear la imagen para que este actualizada.  
  * Leer recomendaciones sobre esto.   
* **Contenedor:** una instancia corriendo (en ejecución) de una imagen. Es efímero, si lo borran y lo vuelven a levantar, arranca de cero (salvo lo que esté en un volumen).  
* **`Dockerfile`:** el archivo con los pasos para construir una imagen (qué base usar, qué copiar, qué comando correr). Piénselo cómo una receta. Para el backend suele ser multi-stage: una etapa que compila con Gradle y otra, más liviana, que solo corre el `.jar` resultante.  
* **`docker-compose.yml`:** describe **varios** contenedores que trabajan juntos (por ejemplo, backend \+ base de datos) y cómo se conectan entre sí, para no tener que levantar cada uno a mano con comandos sueltos.  
* **Volumen:** almacenamiento que persiste aunque el contenedor se borre. Sin volumen, cada vez que reinician el contenedor de Postgres pierden todos los datos cargados.  
* **Red interna de Docker Compose:** los servicios definidos en el mismo `docker-compose.yml` se ven entre sí por su **nombre de servicio**, no por `localhost`. Si el backend corre dentro de Docker y quiere hablarle a la base, la URL es `db:5432`, no `localhost:5432` (este es el error del quickstart, pisa a casi todo el mundo la primera vez).  
* **Mapeo de puertos (`"5432:5432"`):** el primer número es el puerto en tu máquina (host), el segundo es el puerto dentro del contenedor. Les permite acceder desde afuera (por ejemplo, con Postman o un cliente de base de datos) a algo que corre adentro del contenedor.  
  * **Importante**: usar este mapeo permite que se le pueda pegar directamente al servicio desde afuera. A veces esto no es deseable por temas de seguridad (ej: no quiero que cualquiera pueda pegarle a mi BD).   
* **`depends_on`:** le dice a Docker Compose el orden de arranque (por ejemplo, que la base arranque antes que el backend), pero **no espera a que la base esté realmente lista para aceptar conexiones**, solo a que el contenedor haya arrancado. Es una causa común de que el backend falle al conectar en el primer `docker compose up`, a veces alcanza con reintentar.

### **Next.js \+ React**

* **Componente:** una función que genera UI (“algo visual”). Es la unidad básica con la que se arma toda la interfaz: un botón, un formulario, una tarjeta de libro son todos componentes.  
* **Props:** los datos que un componente recibe "de afuera", desde el componente que lo usa. Son de solo lectura para quien las recibe.   
* **Estado (state):** los datos que un componente maneja **internamente** y que pueden cambiar (por ejemplo, el texto que el usuario está tipando en un input). Se maneja con el hook `useState`.  
* **Server Components vs Client Components:** en el App Router, por defecto **todo componente es un Server Component** (se ejecuta en el servidor y no manda JavaScript al navegador para ese componente). No puede usar `useState`, `useEffect` ni manejar eventos de click. Cuando necesitan interactividad real (formularios, botones con estado, hooks), marcan el archivo con `"use client"` arriba de todo, y ese componente pasa a ser un **Client Component**, que sí se hidrata en el navegador.  
* **App Router:** el sistema de rutas de Next.js basado en carpetas. Un archivo `app/books/page.jsx` automáticamente expone la ruta `/books`,no hay que configurar rutas a mano.

### **Spring Boot**

* **Controller (`@RestController`):** la capa que recibe los requests HTTP y los traduce en llamadas a la lógica de negocio. Define los endpoints (`@GetMapping`, `@PostMapping`, etc.). No debería tener lógica de negocio, solo orquestar. Estos ‘Mappings’ se llaman **HTTP Methods** y hay reglas para usar cada uno.  
  * **GET** (`@GetMapping`): obtener datos, no modifica nada en el servidor. Ej: listar catálogo de libros, ver mis préstamos activos.  
  * **POST** (`@PostMapping`): crear un recurso nuevo. Ej: registrar un préstamo, dar de alta un libro.  
  * **PUT** (`@PutMapping`): reemplazar un recurso existente completo (todos sus campos). Ej: editar los datos de un libro.  
  * **PATCH** (`@PatchMapping`): actualizar parcialmente un recurso, solo algunos campos. Ej: marcar la fecha de devolución de un préstamo sin tocar el resto.  
  * **DELETE** (`@DeleteMapping`): eliminar un recurso. Ej: dar de baja un libro del catálogo.  
  * **IMPORTANTE**: deben definir los ‘allowedMethods’ en la config de la app.  
* **Service:** donde vive la lógica de negocio real, las validaciones (¿hay stock?, ¿el préstamo ya existe?), los cálculos, las reglas del dominio. El Controller le pide cosas al Service, no las resuelve él mismo.  
* **Repository:** una interfaz que Spring Data JPA implementa automáticamente para hablar con la base de datos (`JpaRepository<Book, Long>` les da métodos como `findById`, `save`, `delete` sin escribir SQL). El primer parámetro hace referencia a la entidad, el segundo hace referencia al tipo de dato que es el ID de la entidad.   
* **Entidad (`@Entity`):** una clase que representa una tabla de la base de datos. Cada instancia es una fila; y cada campo una columna.  
* **DTO (Data Transfer Object):** un objeto simple, separado de la entidad, que se usa para mandar o recibir datos por la API. Evita exponer la entidad completa (con campos internos que no deberían viajar por la red).  
* **Inyección de dependencias:** en vez de hacer `new BookService()` a mano, Spring arma y conecta los objetos por ustedes, declaran lo que necesitan (típicamente en el constructor con `@Autowired`) y Spring se los provee.

### **Spring Security \+ JWT**

* **Autenticación vs autorización:** autenticación responde "¿quién sos?" (el login). Autorización responde "¿qué podés hacer, dado quién sos?" (por ejemplo, solo el bibliotecario puede borrar libros).  
* **JWT (JSON Web Token):** un token firmado digitalmente que contiene información del usuario (llamada "claims", como su id o su rol). El servidor no necesita guardar sesión en memoria: en cada request valida que la firma del token sea correcta y confía en lo que dice adentro.  
* **Filtro de seguridad (SecurityFilter):** un componente que intercepta cada request *antes* de que llegue al Controller, para validar el token y, si es válido, dejar pasar la request con la identidad del usuario ya resuelta.  
* **`@PreAuthorize` / roles:** anotaciones para restringir el acceso a un endpoint según el rol del usuario autenticado (por ejemplo, `@PreAuthorize("hasRole('LIBRARIAN')")`).

### **PostgreSQL \+ JPA**

* **Relación uno-a-muchos (1-N):** por ejemplo, un Book puede tener muchos préstamos a lo largo del tiempo. En JPA se modela con `@OneToMany` (del lado de Libro) y `@ManyToOne` (del lado de Préstamo).   
  * Esto es un ejemplo, **no todas las relaciones 1-N deben ser bidireccionales** (y no es recomendado, es una causa bastante común de errores de loops infinitos al serializar a JSON la respuesta y además de que requiere mantener la consistencia de ambos lados al hacer una operación sobre una entidad).   
* **Índice:** una estructura que la base mantiene para acelerar búsquedas por una columna específica (por ejemplo, buscar todos los préstamos de un usuario). Sin índice, esa búsqueda recorre toda la tabla.

### **Testing**

* **Test unitario:** prueba una pieza de lógica aislada y chica (por ejemplo, "¿la función de calcular disponibilidad devuelve false si no hay copias?"). Corre rápido y no necesita levantar toda la aplicación ni la base real.  
* **Test E2E (end-to-end):** simula a un usuario real usando la aplicación completa, de punta a punta. Playwright abre un navegador de verdad, clickea botones, escribe en formularios y verifica lo que aparece en pantalla.  
* **Mock:** un objeto "falso" que simula el comportamiento de una dependencia real, para poder testear una pieza sin depender de todo el sistema (por ejemplo, mockear el Repository en un test unitario del Service, para no necesitar una base de datos real corriendo).  
  * Tengan cuidado usando esto porque a veces se empieza a mockear todo y no se testea funcionalidad real. Denle a la IA parámetros claros de qué se puede mockear y que no. 

# Quick Start

## **Quickstart: cómo arrancar el proyecto**

La idea de este quickstart es levantar un circuito mínimo (backend \+ base \+ frontend hablando entre sí) **antes** de meterse con Book/Loan/JWT. Primero confirman que todo el cableado funciona, después construyen las features sobre esa base.

### **Backend (Spring Boot)**

1. **Crear el proyecto** en Spring Initializr ([start.spring.io](https://start.spring.io/)): Project `Gradle - Groovy`, Language `Java`, Spring Boot `4.1.0`, Java `21`. Dependencias: `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Spring Boot DevTools`, `Validation`. (JWT/Security lo suman más adelante, cuando ya tengan endpoints reales que proteger.)  
2. Descomprimir y abrir en el IDE.  
3.  Configurar `src/main/resources/application.properties`:

![][image1]  
En la primera línea, lo que va después de “localhost:5432” es el **nombre de la BD**, pueden poner lo que quieran. 

4. **Levantar:** `./gradlew bootRun` (o el botón de correr del IDE).

**Crear un endpoint de prueba** antes que nada, para validar que el circuito arranca:  
![][image2]

5. **Testear con Postman:** nueva request `GET http://localhost:8080/api/ping` → debería devolver `pong` con status `200`. Si no arranca, lo más común es que Postgres todavía no esté levantado (ver sección Docker).

#### Postman

1. Van a Postman, se registran y descargan: [https://www.postman.com/](https://www.postman.com/)  
2. Se crean una “Collection” que se llame Lab2. Acá van a poder guardar todas las requests que le hagan al backend para probar.   
3. Agregan una request nueva y le ponen nombre (ej: controllerTest).  
4. Completan los parámetros de la siguiente forma y envían la request. 

![][image3]  
	En la parte de abajo donde dice “Response” les debería llegar un status code 200 con el mensaje “Pong”. 

### **Docker**

#### Instalación

**Windows**

1. Requisito previo: WSL2 habilitado (`wsl --install` desde PowerLShell como administrador si no lo tienen, y reiniciar).  
2. Descargar Docker Desktop desde [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) e instalar.  
3. Abrir Docker Desktop una vez y dejar que termine de inicializar (el ícono de la ballena en la barra de tareas tiene que quedar quieto, no "cargando").

**macOS**

1. Descargar Docker Desktop desde el mismo link, elegir la versión según el chip (Apple Silicon o Intel).  
2. Instalar y abrir la app una vez para que arranque el daemon.

**Linux**

1. Instalar Docker Engine siguiendo la guía oficial para su distro: [docs.docker.com/engine/install](https://docs.docker.com/engine/install/) (no usar el paquete `docker.io` del repositorio de la distro, suele estar desactualizado — usar el repo oficial de Docker).  
2. Agregar el usuario al grupo `docker` para no tener que usar `sudo` en cada comando: `sudo usermod -aG docker $USER` (después hay que cerrar sesión y volver a entrar para que tome efecto).  
3. `docker compose` viene incluido como plugin en instalaciones recientes — no hace falta instalarlo aparte.

**Verificar que quedó bien instalado (todas las plataformas):**

docker \--version  
docker compose version  
docker run hello-world

Si el último comando descarga una imagen chica y muestra un mensaje de bienvenida, está todo funcionando.

#### Uso

1. **Base de datos primero.** `docker-compose.yml` con solo Postgres:  
   services:  
        db:  
          image: postgres:16  
          environment:  
            POSTGRES\_DB: biblioteca  
            POSTGRES\_USER: postgres  
            POSTGRES\_PASSWORD: postgres  
          ports:  
            \- "5432:5432"  
          volumes:  
            \- db\_data:/var/lib/postgresql/data  
      volumes:  
        db\_data:  
2. Levantar solo la base: `docker compose up -d db`. Así pueden seguir corriendo el backend desde el IDE contra esa base mientras desarrollan (es más cómodo que rebuildear la imagen del backend en cada cambio).  
3. Verificar conexión: `docker exec -it <nombre_contenedor> psql -U postgres -d biblioteca`, o conectarse con un cliente tipo DBeaver/TablePlus a `localhost:5432`.  
4. **Contenerizar el backend** recién cuando el punto anterior ande bien, un `Dockerfile` simple multi-stage (build con Groovy, runtime con una JRE liviana).

   FROM gradle:8.14-jdk21 AS build

   WORKDIR /app

   COPY . .

   RUN gradle build \-x test \--no-daemon

   

   FROM eclipse-temurin:21-jre

   WORKDIR /app

   COPY \--from=build /app/build/libs/\*.jar app.jar

   ENTRYPOINT \["java", "-jar", "app.jar"\]

5. Agregar el servicio del backend al mismo `docker-compose.yml`, con `depends_on: [db]`. **Ojo acá:** dentro de la red de Docker, el host de la base ya no es `localhost`, es el nombre del servicio (`db`). Esto rompe a todo el mundo la primera vez.  
6. Levantar todo junto: `docker compose up --build`.

### **Frontend (Next.js)**

**Crear el proyecto:**  
 npx create-next-app@latest

1. Elegir: TypeScript sí, App Router sí, Tailwind sí, ESLint sí.  
2. Sumar lo que ya definieron en el stack: `npx shadcn@latest init` para shadcn/ui, y `npm install react-hook-form zod @hookform/resolvers` para formularios.  
3. Configurar la URL del backend como variable de entorno en `.env.local`, nunca hardcodeada:

	NEXT\_PUBLIC\_API\_URL=http://localhost:8080

4. **Iniciar:** `npm run dev`, abrir `http://localhost:3000`.

### **Prueba de integración: el circuito completo**

Con el backend corriendo (local o en Docker) y el frontend en `npm run dev`, hagan esta prueba antes de tocar Book/Loan, confirma que las tres piezas realmente se hablan:

1. Backend solo: `GET http://localhost:8080/api/ping` desde Postman → `pong`.

2. Desde el frontend, una página simple que haga fetch a ese endpoint y lo muestre en pantalla:

   "use client";

      import { useEffect, useState } from "react";

   

      export default function PingTest() {

        const \[msg, setMsg\] \= useState("cargando...");

   

        useEffect(() \=\> {

          fetch(\`${process.env.NEXT\_PUBLIC\_API\_URL}/api/ping\`)

            .then((res) \=\> res.text())

            .then(setMsg);

        }, \[\]);

   

        return \<p\>Respuesta del backend: {msg}\</p\>;

      }

3. Si en la consola del navegador aparece un error de CORS acá, es normal, es el primer error de troubleshooting que van a pisar, y ya está documentado en la sección de arriba (`allowedMethods`/config de CORS).

Si este paso 2 muestra "pong" en pantalla, todo el circuito (frontend → backend → tipado de respuesta) está andando, y recién ahí arrancan con las entidades reales del proyecto.

# Recomendaciones

## **Recomendaciones y buenas prácticas**

Estas son cosas que van a ir chocando a medida que avanzan. Si lo leen al principio, mucho de esto no les va a sonar, así que inicien por leer la capacitación. Si algo de esto no queda claro, pregunten antes de seguir, es más rápido resolver la duda ahora que cambiar todo el código después.   
También pueden usar IA para que les de ejemplos de las recomendaciones que vamos dando.

### **Spring Boot \+ JPA**

* **Cuidado con las queries N+1.** Si tienen una lista de préstamos y por cada uno acceden a `loan.getBook()`, JPA puede terminar haciendo una query por cada préstamo en vez de una sola. Usen `JOIN FETCH` en la query o `@EntityGraph` cuando sepan que van a necesitar la relación. Activen `spring.jpa.show-sql=true` en desarrollo para ver qué SQL se está generando realmente, no asuman.  
* **No devuelvan entidades de JPA directamente en los endpoints.** Usen DTOs para la request/response. Si devuelven la entidad tal cual, corren el riesgo de exponer campos que no deberían (como el hash de la contraseña) o de romper por relaciones lazy no inicializadas.  
* **Separen las capas.** Controller recibe la request, delega al Service y responde; Service tiene la lógica de negocio (las validaciones de stock, fechas, etc.); Repository solo accede a datos. Si la validación de "no prestar sin stock" termina en el Controller, es una señal de que se están mezclando responsabilidades.  
* **Verifiquen que el Service que hace una operación CRUD sobre una entidad sea el de la misma entidad**. Ejemplo: UserService no puede modificar la disponibilidad de un libro, tiene que interactuar con el BookService.    
* **Manejo de errores centralizado** con `@ControllerAdvice` en vez de try/catch repetido en cada endpoint.  
* **Hagan las queries con el Repository**. Eviten patrones del estilo `repository.findAll` y después filtrar por ID o algún campo, esto hace que se cargue toda la tabla en memoria. En tablas grandes esto degrada mucho la performance.  
* **Hagan un buen diseño de API**. Ante cualquier operación, usen el HTTP status code que corresponda a lo que se hizo. Ejemplo con errores: si no hay JWT → 401 (Unauthorized), si no tiene permisos → 403 (Forbidden), si está duplicado → 409 (Conflict). Esto los va a ayudar a debuggear mejor sus errores. 

### **Docker**

* Las imágenes son inmutables. Esto quiere decir que una vez construida la imagen no se pueden hacer cambios sobre ella. Esto es poco práctico si están desarrollando y quieren ir probando los cambios, para esto conviene ejecutar el back desde el IDE y construir la imagen cuando hayan terminado.    
* Si solo cambiaron código de la app (una clase Java, un componente de React) y **no** cambiaron dependencias ni el `Dockerfile`, no necesitan rebuildear si están corriendo el backend/frontend directo con `./gradlew bootRun` / `npm run dev` contra el Postgres dockerizado (que es como armamos el flujo de desarrollo). Ahí Docker solo sostiene la base de datos.  
* Recién necesitan `docker compose up --build` cuando: cambió el `build.gradle`/`package.json` (nuevas dependencias), cambió el `Dockerfile` mismo, o van a correr **todo** el circuito contenerizado (para probar que el build de producción funciona antes de una demo).  
* `docker compose up` sin `--build` reutiliza la imagen ya construida: más rápido, pero si cambiaron algo que afecta la imagen y se olvidan del `--build`, van a estar corriendo código viejo sin darse cuenta. Es una fuente común de "pero yo ya arreglé eso" cuando en realidad el contenedor sigue con la versión anterior.

**Orden en el Dockerfile para aprovechar el cache:**  
Copien primero el archivo de dependencias (`build.gradle`, `package.json`) e instalen dependencias, y recién después copien el resto del código:

dockerfile  
COPY build.gradle settings.gradle ./  
RUN gradle dependencies \--no-daemon  
COPY . .  
RUN gradle build \-x test \--no-daemon

Así, si solo cambió código y no las dependencias, Docker reutiliza la capa de dependencias ya instaladas (que es la parte lenta) en vez de reinstalar todo de cero en cada build.

**`.dockerignore`:** agreguen uno en la raíz del proyecto con `node_modules`, `build/`, `.git`, `.env` — evita que Docker copie carpetas pesadas o innecesarias al contexto de build, lo que hace el build más rápido y la imagen más chica.

**Comandos que van a usar seguido:**

* `docker compose up -d`: levanta todo en segundo plano (no bloquea la terminal).  
* `docker compose logs -f <servicio>`:  ver logs en vivo de un servicio puntual (por ejemplo, `docker compose logs -f backend` si algo no arranca).  
* `docker compose down`: para y elimina los contenedores (los datos del volumen de Postgres **se mantienen**).  
* `docker compose down -v`: además borra los volúmenes. **Ojo con este:** borra los datos de la base también. Solo úsenlo cuando realmente quieran arrancar de cero.  
* `docker compose stop:`Frena los contenedores.  
* `docker system prune`: limpia imágenes/contenedores viejos sin usar, para liberar espacio en disco cuando la máquina se llena de imágenes de builds anteriores. Revisen qué va a borrar antes de confirmar.

### 

### **Spring Security \+ JWT**

* Definan una expiración de token razonable (por ejemplo, unas horas) y prueben qué pasa cuando expira, no debería romper la app de forma confusa para el usuario.  
* Nunca logueen el token completo ni la contraseña en la consola. Si quieren imprimirlo por consola para debuggear algo, dejense notas explícitas de borrarlo sí o sí después.  
* Guarden el hash de la contraseña (BCrypt, que ya viene con Spring Security), nunca la contraseña en texto plano.

### **Next.js \+ React**

* **Eviten componentes de React que hacen demasiado.** Si un componente carga datos, maneja estado de formulario y renderiza tres secciones distintas, es candidato a dividirse. Una buena señal: si te cuesta nombrar el componente en una frase corta, probablemente está haciendo más de una cosa.  
* **Separen la lógica del fetching de la UI.** Un custom hook (`useBooks()`, `useLoans()`) que maneje el fetch y el estado, y un componente que solo reciba los datos y los muestre, es más fácil de testear y de leer.  
* **Manejen explícitamente los estados de carga y error.** No asuman que el fetch siempre va a andar, el usuario necesita ver algo mientras carga y si falla algo.  
* Usen Server Components por defecto y `"use client"` solo donde haga falta interactividad (formularios, botones con estado), no todo el árbol necesita ser client.

### **PostgreSQL**

* Agreguen índices en las columnas que van a filtrar seguido (por ejemplo, `user_id` en préstamos, para el listado "mis préstamos"). Sin índice, esa query escanea toda la tabla.  
* Si modifican la estructura de alguna tabla de la base de datos, van a tener que reconstruirla debido a que lo que tenían ya no les sirve. También van a perder los datos que habían. Esto se hace (cuando estén seguros de que es necesario) cambiando el parámetro de ‘spring.jpa.hibernate.ddl\-auto’ a ‘create-drop’, reinician la app; y después lo cambian a ‘update’ y reinician nuevamente.  

### **Testing**

* Los tests unitarios de las reglas de negocio (stock, préstamo duplicado, fecha límite) tienen que poder correr rápido y sin depender de una base de datos real, usen mocks del repository o una base en memoria (H2) para tests.  
* El test E2E de Playwright no tiene que cubrir cada detalle visual, solo el flujo crítico de punta a punta. Es más valioso un test que cubra "login → préstamo → devolución" que diez tests chicos de detalles de UI.  
* Nombren los tests de forma descriptiva (`shouldFailIfThereIsNoStock` en vez de `test1`), el nombre del test les va a decir qué se rompió con sólo leerlo. 

### 

### **Buenas prácticas de equipo**

* **Commits y PRs chicos.** Un PR de 500 líneas es difícil de revisar bien; varios PRs chicos y enfocados se revisan más rápido y generan menos fricción.  
* **Armen un buen `AGENTS.md` y README desde el día 1**, con: cómo levantar el proyecto (`docker compose up` \+ comandos exactos), estructura de carpetas, convenciones de nombres, y cómo correr los tests. Esto no es solo para ustedes, también sirve si en algún momento usan un asistente de IA para ayudarles con el código: cuanto más claro el contexto del proyecto, mejor la ayuda que reciben. Esto se lo pueden delegar a la IA usando un comando /init o similar.   
* **Pregunten temprano.** Si llevan más de 30-40 minutos trabados en algo que no es el objetivo central del ejercicio (una config, un error críptico), consulten antes de seguir perdiendo tiempo solos.

# Troubleshooting

## **Troubleshooting: errores comunes**

### **Backend (Spring Boot)**

* **CORS bloqueado aunque el endpoint "funcione"** — si `allowedMethods` no incluye el método HTTP que están usando (PATCH y DELETE son los que más se olvidan), el navegador bloquea la request con un error de CORS aunque el backend esté bien. Revisen la config de CORS (`@CrossOrigin` o `WebMvcConfigurer`) y agreguen el método faltante.  
* **Serialización JSON en loop infinito / `StackOverflowError`** — pasa con relaciones bidireccionales (`@OneToMany` \+ `@ManyToOne`) sin `@JsonManagedReference`/`@JsonBackReference`: el libro serializa sus préstamos, cada préstamo serializa su libro, y así infinito. La solución más prolija es no usar estas relaciones bidireccionales si se puede evitar, y si no, no usar la entidad directamente en la respuesta, devolver DTOs evita el problema de raíz.  
* **`LazyInitializationException` ("failed to lazily initialize...")** ocurre cuando acceden a una relación lazy fuera de una transacción activa (por ejemplo, al serializar la respuesta después de que el Service ya cerró la sesión). Se resuelve con DTOs, `JOIN FETCH` en la query, o marcando el método con `@Transactional`.  
* **401 vs 403 confundidos**, 401 significa "no estás autenticado" (token faltante o inválido); 403 significa "estás autenticado pero no tenés permiso" (rol incorrecto). Si les da 403 aunque el token esté bien, el problema está en los roles configurados, no en el JWT.  
* **`@Valid` faltante en el Controller**: si no ponen `@Valid` en el parámetro del endpoint, las anotaciones del DTO (`@NotNull`, `@Size`, etc.) nunca se validan, aunque estén bien escritas.  
* **La app no arranca por conexión a Postgres**: si el `docker compose` no está levantado, o el usuario/puerto en `application.properties` no coincide con el del compose, la app falla al iniciar con un error de conexión.

### **Frontend (Next.js / React)**

* **"This React hook only works in a Client Component"**: aparece al usar `useState`, `useEffect` o manejar eventos de click en un Server Component. Se soluciona agregando `"use client"` arriba del archivo.  
* **Fetch al backend falla sin explicación clara**: si es un error de CORS, va a aparecer acá primero (en la consola del navegador), aunque el problema esté del lado del backend (ver arriba).  
* **La app "no hace nada" cuando el backend devuelve error**: pasa cuando no chequean `response.ok` antes de parsear el JSON de la respuesta. Sin ese chequeo, un 400/500 se trata como si fuera exitoso.

* **Hydration mismatch** ("Text content does not match server-rendered HTML"): cuando lo que se renderiza en el servidor difiere de lo que se renderiza en el cliente (por ejemplo, usar una fecha/hora directamente en el render). Mover esa lógica a un `useEffect`.  
* **401 en un endpoint protegido aunque el login funcionó**: casi siempre es que se olvidaron de mandar el header `Authorization: Bearer <token>` en el fetch.  
* **`useEffect` en loop infinito**: dependencias mal puestas en el array (un objeto o función nueva en cada render dispara el efecto de nuevo sin fin).  
  * Cuidado con este error sobre todo porque React no avisa de este error,  sino que entra en un loop infinito de fetching que les consume recursos. 

### **Base de datos (Postgres)**

* **"port is already allocated"**: ya tienen un Postgres corriendo local en el mismo puerto (5432) fuera del docker-compose del proyecto.

### **Testing**

* **Tests unitarios que fallan de forma intermitente**: normalmente porque dependen del estado dejado por otro test. Cada test tiene que dejar su mock/estado limpio, sin asumir un orden de ejecución.  
* **Playwright: "element not found" por timing**: el test intenta interactuar con un elemento antes de que termine de cargar. Usar `await expect(locator).toBeVisible()` en vez de esperas fijas (`sleep`).  
* **Tests unitarios lentos por levantar todo Spring**: usar `@SpringBootTest` para testear una sola regla de negocio es innecesario; mockear el Repository y testear el Service aislado es más rápido y más enfocado.

### **Docker / setup local**

* **Cambios en el código no se reflejan**: falta rebuildear la imagen después de cambiar dependencias: `docker compose up --build`.  
* **Variables de entorno no cargadas** — la app no encuentra las credenciales de la base porque el `.env` no está en la carpeta esperada o el `docker-compose.yml` no lo referencia.

[image1]: <data:image/png;base64,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>

[image2]: <data:image/png;base64,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>

[image3]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAloAAABQCAYAAADBX9ulAAAWfElEQVR4Xu3diXMU1doG8PuvgGwWm2AJKFUqoFbJ4geFXoqL1EeJgMomqOyKASEgl49F2WST5bIjlx3ZEVmUfYeEHYOEkAQCIRgSzudzUu/h9JmeLZMZkpnnV3Wqu9/epnsm009Od+Aff/31lyIiIiKiivcPBi0iIiKi+GDQIiIiIooTBi0iIiKiOGHQIiIiIooTBi0iIiJ6Ju7cuaPOnj2riouL3VlVypMnT1RWVpY+FheDFhERESVcTk6Oys7OdstVHo7LxqBFRERECYfeH/QEJRu3V6vCg9acXQfdEhEREUVg8uTJqnfv3gEN9WTjBpLKYP/+/W4pau5xVXjQgmYjk+8DQUREFG8ZGRluyUDgSiZuIKkMIg1a1apV082Pe1xxCVrltWTJErfk68GDB27JrOtuw51OhMLCwqBvgB9Z9sUXXwz5Qybeeecd8yZLu379uruYr2hfGxERJV6wUBWs3qdPH3XixAm1ffv2oMtUNm4gceFadeTIEdWoUSP1/vvvm1o8hQta6Fm0X4Pf63GPK6aghduER67c0EO/Fi2/UHT48GFd37Bhg6khaKG2dOlSU5N1b9y4YWoPHz5UoY4PH8hQJMTk5+fr6ZKSEh2GUCsqKjLLjB8/XtWtW1e98cYbnvXkDcCwSZMmql27dp75QsbtoHX8+HFd/+STTzzLvfXWWyZo4pi7dOli5g8YMEAv07VrV1PD8qi9++67etp9bUREVPls3Lgx6C1DvxquQ37k1iN+yX78+LG6du2aZx4sXrxYj8+YMcPMSwQ3kNjwQLlce224dr322mvqueeeUwUFBbqGB+pRb9WqlVmuQ4cOqkaNGmYa83GNDSeSoBVqGtzjiilo4RYhgpYMP5m/KmA6Gn5Ba/PmzXpov3BZDq/94MGyQGevi5ACsm4woYIW3qChQ4fqD54dmJCs7USLYe3atdWcOXNMDfvF+M6dO80y6IX64YcfdOBCOOrVq5eqX7++mQ920ELt0KFDqnr16uYvGFBr3ry5evTokZ62gxbOBebv2bNH1atXT39YEAZRu3jxYtDXRkREVYtfjxVq7p2N/v37m4fNZZ3hw4eb+cuWLdPDadOm6SE6NEpLS838cDp37hxyOhw3kLhwrUIHh1sDXOPs6zDMmjVLtW3b1lODhg0b6uGlS5fU1q1bTd1PuKDlsvcj3OMqd9BCmJJnsWQovViYRsiKtlfLL2ghVOzbt08tX75c5eXl6Vpubq6Z73fL0K8WTLBl/E6eXbPf4J49e5pxJHD39lyocUniIEEL70mtWrV0Im/fvr169dVXA9YFO2h16tRJtWnTRq+DhmXx2wuGCF7oGQT3tRERUeWDa4H7QLzwC1pix44dnvm7d+/WQUpquMUIK1as0MPVq1fra4m0hQsXmnUjgXAlLVpuIPGD6z46Puy7MsIenzhxoqpTp46ptWjRQg/R69W4cWPdcYIW7voXLGjZ13zA8Qbblntc5Q5aECpolYcbehCypPcGJGghlQq/UIX5SPEIFaG4+7PZJ3D69OkBNfuky+09jEcbtOyh3aOFblGX+6baQeubb75Ru3bt8sy34YO6du3agNdGRESVj1+YkttUfvPs3qxt27bpoV84wzUV/zioTP/2229mmURzA4kNAdHmXi/tcXRMuDUJWmDPD8cvaPldM0MFS/e4YgpaFQ3BAWFAGuA5LNzmwv1qCVqZmZn6liHmSTenG5rcadepU6fcksfKlSv1yUVr2bKlrg0aNMjUPvzwQ13DuBu0ZNzvg3Hu3DkzT257ynw7aOG2oiwnXafum20HLZDl0dDrh/XsmrscERFVTn5hSmrB5k2ZMkWlp6erwYMH69qQIUPUggULVFpaWkDounDhgmca1xPp7UoUN5C4cJ3C7U8MFy1aZGr2fBmOGjXKPJMMdtBCaOvYsaNq0KCBOnPmjKn7iSRoudMu97hiClpy+9CvERERUfmgc8ElNb+gVRW5gaQy8AtagHAV6nahzT2umIKW/NUhERERxc7vr9hsuOvhF8KqIjeQVAbBghbIXahw3OOKKWgRERERlYcbSCqDUEErUu5xMWgRERFRwl29etX8m5TJBMdlY9AiIiKiZwL/dhf+CjJZHDhwwC0xaBEREdGzhV4g3HKryi3YPynFoEVEREQUJwxaRERERHHCoEVEREQUJwxaRERERHHCoEVEREQUJwxaRERERHHCoEVEREQUJwxaRERERHFSrqC1c+dOtWTJEjY2NjY2NrYUaD/99JO6d++eGwd8rfjlrur67+uq3ddXUqrhuP1EFbRwsomIiCj13L17N2wOcMNHKjZXxEEr0iRLREREyenkyZNBw1a7tMDQkZLt7/NgizhoBTuxRERElDr27t3rlrSAwJHCzcaglQA3b950S0RERFXSkydP3JLmho1UbjYGrQRg0CIiomTnho1UbjYGrQRg0CIiomTnho1UbjYGrQRg0CIiomTnho1UbjYGrQQIF7SWL1/ulmJSo0YNM16tWjU9jHUf+fn5erhjxw518eJFZ27spkyZoo4cOeKp7dq1S/3nP//x1HJyctTgwYM9NRg5cqS6ffu2pzZu3DjPdDidO3c24/fv37fmeOFn5tKlS26ZiCiluWEjmnYzt1jl3Hus3vGZF227W1gSUEt0s1V40Hp9RHtPg98zjwbUD1w4HFBLXzXF2VpyCBe0WrRo4ZZMQHLHI/H888+bcQQY8NtHNCRc9e3bV61du9aZG7mpU6e6JXN8RUVFav/+/Xo8LS1NFRcX6/FatWqZZeVziGN8+PChHpf1J02apN5++22zbLTnzdaqVSu3REREIbhhI9Jmrwv/91NOwDLRtFheS0U1W4UGrTdHvqdW7l9nptun/6/acnSnDlr/nPChtaSXBLLKaOPGjZ7pyZMne6YjES5ovf766+qjjz4KCFeDBg3SPScyLvV69eqppk2bmp6rNm3aqJKSErPumTNn9NDudZGg9fLLL+t9YV0EG+jatavq1auXqlu3rulVwn5GjBhhXpMdtCZOnKiGDh3qeb2yXfcYxowZo2rWrKmncQzt2rVTFy5cUKNHj9ZDWQ7wlywnTpzw1KB27dq6FwnwvxJAo0aN1OPHj/X44cOHzbL2ej179tRDbBPH1rBhQ3X58mWznJxH+VPll156yawr28HxItTh2Jo1a2bm37p1S5+T/v37B5wLjA8YMEBVr15ddejQwdQPHTpkxomIko0bNiJtwdbNu29d164VmWWv3HqaW+xt2NxtVURbsjs/oBas2So0aNmB6WbeLd1y7uUG9Ggt3LXCWqtyBy2QcNW7d29nTmTCBS1c8MW2bdv00L1w+42fPn3ajItZs2aZ8fr165txCVp2cJRtpaenm5rrwYMH6vz5856gNXv2bD1+7NgxPRw2bJhnuwsXLtRD+7UKvx6tefPm6WWDHWenTp30bURAYELr0aOHnkZYLC0tNcsi3IAdPG0IaOC3LztorVy5Ug9xvGLu3LlmXIKWLSMjQy1btsxTY9AiolThho1IW9cJ1802Ri/J1rX/SbuiA5UsI9u39/PfA/fUnC25qlP6NXX2+qOAZePR7G2fulIW/vyaLW5BS0LVJ7MG66D19qjOavL6Wbr9lnHUWqtqBK3y9GSJcEHLvq23fv16PfQLAu44ZGdne6br1Kljxu1lZR+PHj0yNZnv94/PoccL8z/44AMd6PxuHSJYAPZpb7d9+7L3c+bMmXob6NUSfkHLfqbslVde0UP7tTdu3Fjl5uaqNWvWqIKCAl374osv1MCBA/W49IyBrGe/X5mZmbrevHlzEz7t7Tdp0kQPJWjZx2IHLbuH0C9o4V9M7tatm6dmBy0iomTmho1o28jFt/R20pdnq0FzA6+bWEaGevlFt9SyPfk6bH2/7o6p28vEo9mvJ1izVWjQem/Ch+qrpePNdKd/91Q/H9tVpW8dVoR4Ba1+/fqZcWGHFvthcNlHx44dTU221aBBA1PDM052TxnCVLigtXTpUrPdvLw8dfz4cT1+9GhZoMatTTF27FgzLt58800zjt4qaN26tanJ65SeNNiyZYvq0qWLHsetRfjjjz/M81zBzplf0JJxCVpt27Y183C88ocAuMUrggWtrKwsHexkGQYtIkoVbtiIpLUfdVUV/VXqCSh/3CnWPVrZ+Y8Dlrf3I0HrvTFXfXu/nmWzVWjQAvsWYYf0st/u3VuHbrByp5NNeYIWnleSAIC/gLMDBAIGhuvWlT0Ph2emAH+NJ89YufuUfaD3B+vagQxBAjW7NwyBBDVsJ1zQAtmuHf4wjtrWrVtNDc9oLVq0SG3fvt08I4Zwhlt+diADBDD79id0795db3PVqlWmhh4o1PCslMBzZwL7wXw8F2YHLTmPQoKWXcPx4hkv1E6dOmXqwYIW4Hzh2LFfBi0iShVu2Ii02f/Q/O27T8NVVm7ZH0TBg6KyMAYyX4KW1G3uPhLdbBUetCiQG3piYYcAF26nCb+eo1QS7L+IEKHOozxwD/atw0gUFhaaZ7nQ6+b3HB0RUTJyw0YqNxuDVgJUZNCiqoHvORGlGjdspHKzMWglAC+6RESU7NywkcrNxqBFREREMXPDRio3G4MWERERRSxYbnDDRio3G4MWERERRSxYHnDDRio3W8RBC65du+aWiIiIKEUgZLn/tI34+UhBQOBIxYbzYIsqaOHkbtq0yS0TERFREkNWCBWyRLu0wOCRas0VVdAiIiIiosgxaBERERHFCYMWERERUZwwaBERERHFCYMWERERUZwwaBERERHFSVIHrYKCgkrViIiIKLUkbdByQ05laURERJQ6GLQS3IiIiCh1MGgluBEREVHqYNBKcCMiIqLUEVPQ6t27d9Dm2rt3r9q+fbtuhYWF7uwK5waco0ePqu+++07NmjUrYF4iW6RwnqIR7fJw+PBhtxSxYcOGuaUK1a9fP8+032cqUrGsGw/yc3DkyBF3VkjleY+DKSkpUYsWLVK3b992ZwWFn6FEuXTpklq9erVbjtjatWvdEhGlmH+NOKpe7blPt/uFj93ZIf13959uSW+nPGIKWtEYMWKE/vKEu3fvqk8//dRZomK5AWfhwoVmfPDgwapVq1YByySiRWrq1KluKYAdIBIRJhKxDxEqaOXl5VlzyvgFhqtXr+qhvS4+78XFxWZa3hN8JoU9Hs6dO3fcUljlfd+iWTaUBQsWqHXr1ulxnMsrV654FyiHc+fOuaVys49z+PDh1pxAffr0MeNTpkyx5hBRKvso/aQqflxqpjt8cciMFz0qVWev3DfT4vSlp9doO2hheci79/TaAdl5jzzTpaVPVIFPoIspaLm9WNL82EEL8GUvsA6CkP2liQvt4sWLPdtD7dtvv1U3btwwtWDcgGMHLbRq1aqZ4aBBg8w02ldffaV69Oihx2vWrKm6devmmf/ZZ5+p/v3761qjRo1U9+7dVd26dfW8Jk2a6PkvvPBCwGtAi4T0Fv3444+mZp8H/A/qcm4wlPkIZ9OmTVMzZ87UNVz8hg4dqtLT09WKFSvMcgiaWAa9jICejQkTJugm+8E5mDhxoh6iJ+PWrVsB+wu1j9GjR+vXP3DgQF2LVrCg9fnnn+uhvB573rZt2/TrsGs4V/Y4QtaTJ0886+bm5urxjIwMVVRUpMdXrlyph6Fge+Vhv5cDBgxQjx6V/bCijs+pPR/nb/78+fozKnX73NjLRgqfLwlatk2bNunPkGwTn4958+bpzxQcOnRID+X97du3799fLKX6vcAy8tnAz8aqVav0sDyCHRPq9vuJ/eE7Y8uWLXocxyWvQd6bHTt26NdmbxPjI0eO1L/s2Z+DyZMne76DiKjqCtb7NHZ+prp7vywwyTKZNx6oXYfLfml+d3DZnR4JWrfz/lLz11/3LH/15kP1+5l8T23eurJlSv4OW78cLbumiJiCVjTcoGXfhjhx4oQe4otebNy4UQ/379+vh+fPnzfzInnNbsCxgxZ+823atKke37dvnx62bt1aX6gxjouLLLts2TI9XL9+vakhXGFYq1YtU7ODm9T8WiTsL3+3BnIRCTbfb325gNg1CVoCIeTnn3/W4ydPnjR1XFAhmn3Ie2TPjwbWcxvYPaESugRCkixnByW/17pz586A2q+//qqysrLMdDi7d+92SxHBPhFqv/zyS3Xw4EFTl58DmD59uh7aPZt+xyEhKFo4d/iF4P79p7/V2ds9duyY/nzgNQo7aAl5z+0erVjDyu+//673gdv8Aj1b0muJn0URrEfLDlrizJkzeohgJuRYJKATUXIIFrTs+qGzZXcv/vWl97GIy1mFOmjl3iv2LC/jXazl3+7/mx6mzb4Q0OMlYgpa7oXQbi43aK1Zs0YP5cIu8GV4+fJlT02gdwXbtm/9BOMGHAQtXKRxgcGtIanXr19fh6O2bdvqixZqeJ0yH3XM//jjj00N28CwZcuWpiYBCxcojAcLXOHgt2r0OI0dO9YTKuxzGmnQwsVcIETa88AOWteuXfP0lMyZM0cva/eklGcffp+FSATr0UpLS1M5OTmmyTz5zMhye/bsKVvRqmHot64Nzy7hout+Ll2bN292SxGz9zljxgwdCt39yTL2Z11qFy5c0EP7GMsL7zt+qcH+3V4ufD7sXju/oCXjdtBCYETdfQ/L4+uvv9ZDbM9uIlzQsm8D4xk39zapbAufXYy74Z2IqiY3aP2RXXa3wq7nFXh7tsTGX7N10Dp4Ks83aGGYlVNkmsBzYLhFeSLznqlBTEErGm7Qsr8sT58+rYf2xWbDhg16iB4n+PPPp/dL3YujHzfguLcOpV28eFEPm7z0UkDQwjR+68f47NmzzTqhgtakSZNMrX379gH7C8c9Nvt2HOD9ijRo2TW/Xik7aI0aNcqMg1zgwG97fjW/fbjHEyn3Ii3bwa02QCByXwNuZ7k1+1YceiofPnyox/16+KT3CLcWwz0jF0vIsfeJnl3plZKfA5Aabt8Ke70xY8aU+9xiPTvAoddW6kJ6tMIFLTmPdo8zeqREeR5KR7gX8gvZkCFDTG+jHZbs1zJ+/HgzHixogd8tefuXu7Nnz5pxIqqaih8/Ue0Gln0XPXhYYkLS6DkZ5jkqqWVcf6B+OVZ2u++fQ8v+SMl+RssOWHD+6n118LT31qH0bGG/CzZ4H2+KKWjhS8puoSBoYRlcjN0vX3TlY579BY0LEGp2+MK6kd6WcANOsKDVrFkzHZJwa8INWmh4DZhv10IFLTx7hvGGDRsG7AstFIQH6a0Qcl7ltlh+fr65iLjPGrnryDganqVx50nQ8nsf5Tf877//PuA1yDoi1D7s8YqCcyCBSWRmZnqmwa9nFJ/3UA+w4z2S44gXOV/4HNm3C+XnAM8PCQQY1MaNG+c5lwiiBw4cMNPRmjt3rt6e3QsknyeEOAgVtPDaJVgLCccIR1gm3IPsweCZL/ycYxt26MF3AWr2bWH0xqEHGKQXFkIFLfzVM8IlfqZleYRwjCOsE1HywPNXrsKiEs+D7/D3119AT1Q4mde92/7zjvfheBFT0KrM3IBTWRpRrBDIpWfvWYhHeE4k6fnCM1/yhyNERPHCoJXgRhQrPKuI3qdnBbfxqjI8TI+waN8eJyKKFwatBDciIiJKHQxaCW5ERESUOpI2aIEbcp51IyIiotSS1EGLiIiI6Fli0CIiIiKKEwYtIiIiojhh0CIiIiKKk/8HumhRGaolTysAAAAASUVORK5CYII=>