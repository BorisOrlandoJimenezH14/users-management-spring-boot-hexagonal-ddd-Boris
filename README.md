# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Secretos para desarrollo local

1. Copiar `.env.example` como `.env` en la raíz del proyecto y reemplazar los valores
   de ejemplo: `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `BREVO_API_KEY` y
   `BREVO_FROM_ADDRESS`. Ese archivo lo leen tanto Docker Compose (`compose.yaml`) como
   la aplicación (`spring.config.import` en `application.properties`).

   Alternativa opcional: copiar `application-secrets.properties.example` como
   `.environments/application-secrets.properties` con las mismas claves (estilo
   `db.*`, `jwt.secret`, `brevo.local.*`). La app lo carga al arrancar si existe y sus
   valores tienen prioridad sobre el `.env`. La carpeta `.environments` está excluida
   de Git y del contexto de Docker.

En producción no se utiliza ninguno de estos archivos: los secretos se configuran con
las variables de entorno `DB_*`, `JWT_SECRET` y `BREVO_*` del proveedor de despliegue.

## Build y run local

Requisitos: Java 17, Maven (`mvnw`) y Docker (solo para levantar MySQL local).

```bash
# 1) Crear los secretos y completar los valores reales
cp .env.example .env

# 2) Levantar MySQL 8.4 en el puerto 3306
docker compose up -d mysql

# 3) Compilar (descarga dependencias y compila tests)
./mvnw clean package

# 4) Ejecutar la aplicación
./mvnw spring-boot:run
```

Con la app corriendo:

- Health / OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Login: `POST http://localhost:8080/api/auth/login` con el administrador creado por
  `schema.sql` (`admin@example.com` / `Admin1234!`)
- Colección de Postman con el entorno **Users Management - Local** (ver más abajo).

En Windows se puede utilizar `mvnw.cmd`.

## Docker Compose local

Copiar `.env.example` como `.env` (o `compose.env.example` si solo quieres el archivo
mínimo para Compose), reemplazar todos los valores y ejecutar:

```bash
docker compose up --build
```

Compose se detiene con un error si falta `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`,
`JWT_SECRET`, `BREVO_API_KEY` o `BREVO_FROM_ADDRESS`. El archivo `.env` está excluido
de Git y del contexto de construcción Docker. Los puertos de MySQL y la API se publican
solo en `127.0.0.1`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`. La aplicación usa el motor
MySQL (`DB_ENGINE=mysql`), por lo que funciona tanto con un MySQL propio como con
TiDB, que habla el mismo protocolo MySQL.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos solicitados: `DB_HOST`, `DB_PORT`, `DB_NAME`,
   `DB_USERNAME`, `DB_PASSWORD`, `BREVO_API_KEY`, `BREVO_FROM_ADDRESS` y `JWT_SECRET`
   (este último también se puede generar desde el dashboard).
3. Asegurar que la base de datos sea accesible desde Internet y crear la tabla
   `users` ejecutando `src/main/resources/schema.sql` una vez.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.

### Conexión con TiDB

TiDB es compatible con MySQL, así que no hace falta cambiar código: solo las
variables de entorno (y sincronizar el CA si se quiere validar el certificado).

| Variable | Valor para TiDB Cloud Serverless |
| --- | --- |
| `DB_ENGINE` | `mysql` |
| `DB_HOST` | `gateway01.<region>.prod.aws.tidbcloud.com` |
| `DB_PORT` | `4000` |
| `DB_NAME` | la base de datos que ya creaste |
| `DB_USERNAME` | `<prefijo>.root` (el prefijo lo muestra la consola) |
| `DB_PASSWORD` | la contraseña del clúster |
| `DB_SSL_MODE` | `REQUIRED` |

Pasos:

1. En TiDB Cloud abre el clúster → **Connect** y copia el host, el puerto `4000`,
   el usuario (`<prefijo>.root`) y la contraseña. Esos mismos datos los usas en
   VS Code.
2. En TiDB Cloud → **Networking / Allowed IP Addresses** agrega `0.0.0.0/0`. Los
   servicios con plan gratuito de Render no tienen IP de salida fija, por lo que no
   es posible permitir una IP concreta.
3. Como la base de datos ya existe, ejecuta **solo** la creación de la tabla
   (`CREATE TABLE users ...` y el `INSERT` del administrador de `schema.sql`)
   dentro de tu base de datos. Omite `CREATE DATABASE` y `USE` (o ajusta el nombre
   `crud_usuarios` por el tuyo).
4. Carga las variables de la tabla en Render (blueprint o dashboard) y haz redeploy.

Notas:

- `DB_SSL_MODE=REQUIRED` cifra la conexión sin validar el nombre del certificado;
  es lo recomendado para TiDB Cloud. Con `VERIFY_IDENTITY` habría que importar el
  CA de TiDB al truststore del JRE del contenedor.
- Para un MySQL local sin TLS usa `DB_SSL_MODE=DISABLED` y `DB_PORT=3306`.

### Conexión desde VS Code

La extensión de MySQL (por ejemplo **MySQL** de cweijan, o **SQLTools + MySQL/MariaDB**)
se configura con los mismos datos que exportas a Render:

- **Host**: `gateway01.<region>.prod.aws.tidbcloud.com`
- **Port**: `4000`
- **User**: `<prefijo>.root`
- **Password**: la del clúster
- **Database**: tu base de datos
- **SSL/TLS**: activado (TiDB Cloud lo exige; en SQLTools suele ser
  `Require`, en la extensión MySQL un toggle `SSL`). Si el editor pide el CA para
  TiDB Cloud Serverless, basta con permitir TLS sin verificar; o descarga el CA del
  clúster desde la consola si tu cliente exige `Verify CA`.

## Pruebas con Postman

La carpeta [`postman/`](postman) contiene una colección con pruebas para cada operación
del CRUD y para la obtención del token del administrador:

- `users-management.postman_collection.json`: peticiones y validaciones (`pm.test`).
- `users-management-local.postman_environment.json`: entorno con `baseUrl` en
  `http://localhost:8080`.
- `users-management-render.postman_environment.json`: entorno para probar la API
  desplegada en Render.

Importar en Postman:

1. **Import** → arrastra los archivos JSON de `postman/`.
2. Selecciona el entorno **Users Management - Local** (desarrollo) o
   **Users Management - Render** (API desplegada).
3. Ejecuta **Run collection** (Collection Runner). El orden importa: *1. Autenticacion*
   obtiene el token, luego el CRUD y por último los casos negativos.

| Carpeta | Petición | Qué valida |
| --- | --- | --- |
| 1. Autenticación | Login administrador | Guarda el JWT del admin en la variable `token` |
| 2. CRUD Usuarios | Crear usuario | `201` y estado inicial `PENDING` |
| | Listar usuarios | `200` y que el usuario creado aparezca |
| | Obtener usuario por ID | `200` con los datos del usuario |
| | Actualizar usuario | `200` con nombre y estado `ACTIVE` |
| | Eliminar usuario | `204` sin cuerpo |
| | Verificar eliminación | `404` tras el borrado |
| 3. Casos negativos | Login inválido | `401` |
| | Listar sin token | `401` |
| | Usuario inexistente | `404` |
| | Email duplicado | `409` |

El token se obtiene automáticamente del login del administrador definido en
`schema.sql` (`admin@example.com` / `Admin1234!`); las peticiones del CRUD lo heredan
mediante la autenticación Bearer a nivel de colección. Los entornos incluidos ya
traen el `baseUrl` correcto para local y para la API desplegada.
