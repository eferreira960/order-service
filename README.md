# Orders Processing Backend

Backend reactivo con Spring Boot para procesamiento de órdenes, utilizando MongoDB, gRPC, Akka Classic Actors, integración SMPP para el envio de mensajes y métricas con Prometheus.

## Tecnologías Utilizadas

- **Java 17** - Lenguaje
- **Spring Boot 3.2.5** con **Spring WebFlux** - API REST reactiva
- **Spring Data MongoDB Reactive** - Acceso reactivo a MongoDB
- **gRPC** - Comunicación entre servicios para creación de órdenes
- **Akka Classic Actors** - Procesamiento asíncrono de órdenes
- **CloudHopper SMPP** - Envío de SMS vía protocolo SMPP
- **Log4j2** con configuración YAML - Logging
- **Spring Actuator** con **Micrometer Prometheus** - Métricas
- **SpringDoc OpenAPI (Swagger)** - Documentación de API
- **GitHub Actions** - Pipeline CI/CD
- **Gradle 8.7** - Herramienta de compilación

## Arquitectura

El proyecto sigue los principios de **Arquitectura Limpia (Clean Architecture)** con tres capas:

```
com.hacom.orders
├── application/        → Configuración, casos de uso, métricas
│   ├── config/         → Configuraciones programáticas de MongoDB, WebFlux, Akka, OpenAPI
│   └── metrics/        → Contadores personalizados de Prometheus
├── domain/             → Reglas de negocio de la empresa
│   ├── model/          → Entidad Order
│   └── port/           → Interfaces de repositorio y envío de SMS
├── infrastructure/     → Frameworks, drivers, herramientas externas
│   ├── persistence/    → Implementación del repositorio reactivo MongoDB
│   ├── grpc/           → Implementación del servicio gRPC
│   ├── actor/          → Actor Akka Classic para procesamiento de órdenes
│   ├── smpp/           → Cliente SMPP CloudHopper
│   └── rest/           → Controladores de la API REST
└── shared/dto/         → DTOs compartidos (uso futuro)
```

## Flujo

1. **Cliente gRPC** envía una solicitud `CreateOrder` → `GrpcOrderService`
2. `GrpcOrderService` crea un **Akka Actor** y envía un `ProcessOrderMessage`
3. **Akka Actor** (`OrderProcessorActor`) procesa la orden:
   - Guarda la orden en **MongoDB** con estado `PROCESSED`
   - Envía una notificación **SMS** vía el cliente **SMPP**
   - Incrementa la métrica de **Prometheus**
   - Devuelve una respuesta gRPC con el ID y estado de la orden
4. **API REST** permite consultar:
   - `GET /api/orders/{orderId}/status` - Consultar estado de una orden
   - `GET /api/orders/count?from=...&to=...` - Contar órdenes por rango de fechas

## Prerrequisitos

- Java 17 (JDK 17+)
- MongoDB 7.0+ (ejecutándose en localhost:27017 o URI configurada)
- Gradle 8.7+ (o usar el Gradle Wrapper)

## Configuración

### application.yml

La configuración principal se encuentra en `src/main/resources/application.yml`:

```yaml
mongodbDatabase: exampleDb
mongodbUri: "mongodb://127.0.0.1:27017"
apiPort: 9898
```

> **Nota:** El puerto de MongoDB y WebFlux se configuran **programáticamente** a través de las clases `MongoConfig` y `WebFluxConfig`, no mediante la auto-configuración de Spring Boot.

### Configuración SMPP

```yaml
smpp:
  host: localhost       # Host del SMSC
  port: 2775            # Puerto del SMSC (por defecto para smppsim)
  system-id: smppclient # ID del sistema SMPP
  password: password    # Contraseña SMPP
```

## Compilar y Ejecutar

### Ejecución Manual (sin Docker)

#### 1. Usando Gradle Wrapper

```bash
# Generar el Gradle Wrapper (solo la primera vez)
gradle wrapper --gradle-version 8.7

# Compilar el proyecto
./gradlew clean build

# Ejecutar pruebas
./gradlew test

# Iniciar la aplicación
./gradlew bootRun
```

#### 2. Requisitos previos para ejecución manual

Antes de ejecutar la aplicación, asegúrate de tener MongoDB corriendo:

```bash
# Opción A: Usando Docker para MongoDB
docker run -d --name mongodb -p 27017:27017 mongo:7.0

# Opción B: MongoDB instalado localmente en el puerto por defecto 27017
```

Una vez que MongoDB esté corriendo, inicia la aplicación:

```bash
./gradlew bootRun
```

La aplicación estará disponible en:
- **API REST:** http://localhost:9898
- **gRPC:** puerto 9090
- **Swagger UI:** http://localhost:9898/swagger-ui.html

### Ejecución con Docker Compose

Docker Compose orquesta todos los servicios necesarios (MongoDB, la aplicación y opcionalmente un simulador SMPP).

#### Servicios incluidos:

| Servicio | Imagen | Puerto | Descripción |
|----------|--------|--------|-------------|
| `mongodb` | `mongo:7.0` | `27017` | Base de datos MongoDB |
| `app` | `hacom-orders-backend` (build local) | `9898:9898` (REST), `9090:9090` (gRPC) | Aplicación Spring Boot |
| `smppsim` | `hacom-smpp-simulator` (perfil smpp) | `2775` | Simulador SMSC SMPP (opcional) |

#### Comandos básicos

```bash
# Compilar y arrancar todos los servicios (MongoDB + App)
docker-compose up --build

# Ejecutar en segundo plano (modo detached)
docker-compose up --build -d

# Incluir el simulador SMPP
docker-compose --profile smpp up --build

# Detener todos los servicios
docker-compose down

# Ver logs de la aplicación
docker-compose logs -f app

# Ver logs de todos los servicios
docker-compose logs -f
```

#### Compilar y ejecutar manualmente con Docker

```bash
# Compilar la imagen Docker
docker build -t hacom-orders-backend .

# Iniciar MongoDB
docker run -d --name hacom-mongodb -p 27017:27017 mongo:7.0

# Iniciar la aplicación (conectándola a MongoDB)
docker run -d --name hacom-orders-app \
  --link hacom-mongodb \
  -p 9898:9898 \
  -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e mongodbUri=mongodb://hacom-mongodb:27017 \
  hacom-orders-backend
```

#### Arquitectura Docker

```
MongoDB:27017 <-  hacom-orders-app:9898 (API REST):9090 (gRPC)
Servidor SMPP:2775
```

## Cómo Probar los Servicios

### 1. Endpoints REST

```bash
# Obtener estado de una orden
curl http://localhost:9898/api/orders/ORD-001/status

# Contar órdenes en un rango de fechas
curl "http://localhost:9898/api/orders/count?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z"
```

**Respuestas esperadas:**

```json
// GET /api/orders/ORD-001/status
{
  "orderId": "ORD-001",
  "status": "PROCESSED"
}

// GET /api/orders/count?from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z
{
  "from": "2024-01-01T00:00:00Z",
  "to": "2024-12-31T23:59:59Z",
  "total": 5
}
```

### 2. Servicio gRPC

El servidor gRPC corre en el puerto **9090** con la siguiente definición de servicio:

```protobuf
service OrderService {
  rpc CreateOrder (OrderRequest) returns (OrderResponse);
}

message OrderRequest {
  string order_id = 1;
  string customer_id = 2;
  string customer_phone_number = 3;
  repeated string items = 4;
}

message OrderResponse {
  string order_id = 1;
  string status = 2;
}
```

#### Probar gRPC usando grpcurl

```bash
# Enviar una orden vía gRPC usando grpcurl
grpcurl -plaintext -d '{
  "order_id": "ORD-001",
  "customer_id": "CUST-001",
  "customer_phone_number": "+1234567890",
  "items": ["item1", "item2"]
}' localhost:9090 com.hacom.orders.grpc.OrderService/CreateOrder
```

### 3. Swagger UI

Accede a la documentación interactiva de la API en:
```
http://localhost:9898/swagger-ui.html
```

Desde Swagger UI puedes probar los endpoints REST directamente desde el navegador.

### 4. Endpoints de Actuator (Monitoreo)

| Endpoint | Descripción |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/info` | Información de la aplicación |
| `/actuator/metrics` | Métricas de la aplicación |
| `/actuator/prometheus` | Métricas formato Prometheus |

```bash
# Verificar salud de la aplicación
curl http://localhost:9898/actuator/health

# Obtener métricas de Prometheus
curl http://localhost:9898/actuator/prometheus
```

### 5. Métricas Personalizadas de Prometheus

| Métrica | Descripción |
|---------|-------------|
| `hacom_orders_processed_total{status="success"}` | Total de órdenes procesadas exitosamente |
| `hacom_orders_processed_total{status="failed"}` | Total de órdenes fallidas |

Puedes consultar estas métricas en:
```
http://localhost:9898/actuator/prometheus
```

## Ejecución de Pruebas

```bash
# Ejecutar todas las pruebas
./gradlew test

# Ejecutar una clase de prueba específica
./gradlew test --tests "*OrderControllerTest"

# Ejecutar pruebas con reporte de cobertura (si está configurado JaCoCo)
./gradlew test jacocoTestReport
```

Las pruebas utilizan:
- **JUnit 5** - Framework de pruebas
- **Mockito** - Framework de mocking
- **Flapdoodle Embedded MongoDB** - MongoDB en memoria para pruebas de integración
- **WebTestClient** - Cliente REST reactivo para pruebas de controladores