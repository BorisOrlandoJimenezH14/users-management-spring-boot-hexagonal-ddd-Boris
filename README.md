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

Copiar `application-secrets.properties.example` como
`.environments/application-secrets.properties` y reemplazar los valores de ejemplo.
Este archivo contiene la conexion local a MySQL, la semilla JWT y las credenciales de
Brevo. La carpeta `.environments` esta excluida de Git y del contexto de Docker.

En produccion no se utiliza este archivo: los secretos deben configurarse mediante las
variables de entorno `DB_*`, `JWT_SECRET` y `BREVO_*` del proveedor de despliegue.

## Docker Compose local

Copiar `compose.env.example` como `.env`, reemplazar todos los valores y ejecutar:

```bash
docker compose up --build
```

Compose se detiene con un error si falta `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`,
`JWT_SECRET`, `BREVO_API_KEY` o `BREVO_FROM_ADDRESS`. El archivo `.env` esta excluido
de Git y del contexto de construccion Docker. Los puertos de MySQL y la API se publican
solo en `127.0.0.1`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`. La aplicación usa el motor
MySQL (`DB_ENGINE=mysql`), por lo que funciona tanto con un MySQL propio como con
TiDB, que habla el mismo protocolo MySQL.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos solicitados: `DB_HOST`, `DB_PORT`, `DB_NAME`,
   `DB_USERNAME`, `DB_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD` y
   `SMTP_FROM_ADDRESS`.
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
