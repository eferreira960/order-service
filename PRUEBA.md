## **Caso Práctico: Procesamiento de pedidos** 

## **Objetivo:** 

Crear un proyecto simple integrando diferentes tecnologías que se usan en proyectos del tipo Telco en HACOM. Queremos evaluar tu capacidad para poner en marcha un proyecto simple. 

## **Requisitos:** 

1. Crear un proyecto con Spring boot, Gradle y java 17. 

2. Utilizar Spring Data mongodb reactive, Spring Webflux, Spring log4j2 y Spring actuator 

3. Utilizar application.yml, en vez de application.properties. 

4. Crear 3 variables en el application.yml que deben ser usadas posteriormente para la conexión con mongodb y setear el puerto de servidor de Webflux 

```
mongodbDatabase: exampleDb
mongodbUri: "mongodb://127.0.0.1:27017"
apiPort: 9898
```

**Importante:** 

Configurar la conexión a mongodb de forma programática, no utilizar la configuración por defecto de Spring. 

Configurar el puerto de Webflux de forma programática también. 

5. Integrar gRPC con gradle. 

   - Crear un servicio gRPC para insertar pedidos, debe contar con ID del pedido, ID de cliente, número de teléfono del cliente y lista de ítems del pedido. 

   - La respuesta debe contar con el ID del pedido y un estado. 

6. Integrar Akka Classic Actors. 

   - Crear un actor que procese los pedidos ingresados por gRPC, el actor debe enviar la respuesta grpc cuando finalice de procesar el pedido. 

7. Integrar con Mongodb. 

   - El actor finaliza el pedido insertando la información del pedido en mongodb, debe contener lo siguiente: 

```
public class Order {
  @Id
  private ObjectId _id;
```

```
  private String orderId;
  private String customerId;
  private String customerPhoneNumber;
  private String status;
  private List<String> items;
  private OffsetDateTime ts;
}
```

8. Integrar la librería SMPP https://github.com/fizzed/cloudhopper-smpp para envió de SMS por SMPP. 

   - Crear un cliente SMPP para el envió de SMS. 

   - Se debe enviar un SMS con el texto: "Your order " + request.getOrderId() + " has been processed", una vez el actor termina de procesar el pedido. 

## 9. Crear una API. 

   - Un endpoint para consultar el estado del pedido. 

   - Un endpoint para consultar el total de pedidos por rango de fecha, usar OffsetDateTime para el rango. 

10. Insertar logs convenientemente en cualquier parte del código. 

11. Para la configuración de Log4j2 utilizar un archivo log4j2.yml y no log4j2.xml. 

12. Utilizar Spring actuator y configurar para que exponga métricas prometheus, colocar al menos un contador. 

## **Entregables:** 

1. Estructura del proyecto. 

2. Un README que indique una breve documentación y/o instrucciones de uso. 

3. Subir y compartir el repositorio en GitHub al correo freddy.mendoza@hacom-tech.com 

## **Criterios de Evaluación:** 

1. Claridad en la estructura del proyecto, y en las configuraciones del proyecto. 

2. Entendimiento de las diferentes partes que componen el proyecto. 

3. Correcta integración y uso de las librerías. 

4. Buenas prácticas y código entendible. 

