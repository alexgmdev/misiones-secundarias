# Especificación auth-login (HU-002)

## Purpose
Inicio de sesión con correo o celular, error siempre genérico y sesión persistida.

## Supuestos
- Si el identificador contiene "@" es correo; si no, celular (normalizado como en auth-registro).
- Cuentas suspendidas/inactivas: FUERA DE ALCANCE (no existe estado de suspensión en el MVP).
- Límite de intentos: el login por celular lo aplica la Edge Function `login-telefono` (5 fallos en 15 minutos por celular o por IP del cliente); el login por correo usa solo el límite de GoTrue.

## Requirements

### Requirement: Campos obligatorios
Identificador y contraseña MUST ser no vacíos.

#### Scenario: Campos vacíos
- GIVEN login sin datos
- WHEN presiona "Iniciar Sesión"
- THEN muestra "Este campo es obligatorio" por campo y no llama al backend

### Requirement: Login con correo
El sistema MUST autenticar con correo y contraseña.

#### Scenario: Éxito con correo
- GIVEN cuenta con "ana@example.com"
- WHEN ingresa "ana@example.com" y su contraseña correcta
- THEN inicia sesión y navega a `menu-principal`

### Requirement: Login con celular
El sistema MUST autenticar con celular y contraseña vía Edge Function `login-telefono`, que devuelve solo la sesión.

#### Scenario: Éxito con celular
- GIVEN cuenta con celular "3001234567"
- WHEN ingresa "3001234567" y su contraseña correcta
- THEN la app importa la sesión y navega a `menu-principal`

#### Scenario: Celular con formato libre
- GIVEN identificador "300 123 4567"
- WHEN presiona "Iniciar Sesión"
- THEN se envia "3001234567" a la función

### Requirement: Error genérico
Ante credenciales incorrectas el sistema MUST mostrar siempre "Credenciales incorrectas", idéntico para correo y celular, sin indicar que campo fallo ni si la cuenta existe.

#### Scenario: Clave incorrecta con correo
- GIVEN cuenta "ana@example.com"
- WHEN ingresa clave equivocada
- THEN muestra "Credenciales incorrectas" y permanece en login

#### Scenario: Clave incorrecta con celular
- GIVEN cuenta con celular "3001234567"
- WHEN ingresa clave equivocada
- THEN muestra exactamente "Credenciales incorrectas"

#### Scenario: Correo inexistente
- GIVEN no existe "x@example.com"
- WHEN intenta iniciar sesión
- THEN muestra el mismo "Credenciales incorrectas"

### Requirement: Celular desconocido
Un celular no registrado MUST producir la misma respuesta y apariencia que una clave incorrecta.

#### Scenario: Celular sin cuenta
- GIVEN celular "3109999999" sin cuenta
- WHEN intenta iniciar sesión
- THEN muestra "Credenciales incorrectas"

### Requirement: Correo no expuesto
La Edge Function MUST NOT devolver el correo ni otros datos personales al cliente, ni en éxito ni en error.

#### Scenario: Respuesta de la función
- GIVEN login por celular exitoso o fallido
- WHEN se inspecciona la respuesta
- THEN solo contiene sesión (éxito) o error genérico (fallo)

### Requirement: Sesión persistida
La sesión MUST persistir entre reinicios; con sesión válida la app SHOULD abrir en `menu-principal`.

#### Scenario: Reapertura con sesión
- GIVEN usuario con sesión iniciada
- WHEN reabre la app
- THEN abre en `menu-principal` sin pedir credenciales

#### Scenario: Sin sesión
- GIVEN no hay sesión guardada
- WHEN abre la app
- THEN muestra login

### Requirement: Límite de intentos por celular
La Edge Function `login-telefono` MUST bloquear el login por celular tras 5 intentos fallidos en 15 minutos para el mismo celular o la misma IP de cliente, y MUST responder con el mismo error genérico, sin que la app pueda distinguir el bloqueo de unas credenciales incorrectas. El login por correo MUST NOT usar este límite (aplica el de GoTrue).

#### Scenario: Bloqueo tras 5 fallos
- GIVEN el celular "3001234567" acumula 5 intentos fallidos en los últimos 15 minutos
- WHEN intenta iniciar sesión de nuevo, incluso con la contraseña correcta
- THEN la función responde 429 sin autenticar y la app muestra "Credenciales incorrectas"

#### Scenario: Bloqueo por IP de cliente
- GIVEN una misma IP de cliente acumula 5 intentos fallidos en 15 minutos con celulares distintos
- WHEN intenta iniciar sesión con otro celular
- THEN la función responde 429 y la app muestra "Credenciales incorrectas"

#### Scenario: Bloqueo indistinguible
- GIVEN un intento bloqueado y un intento con clave incorrecta
- WHEN se comparan las respuestas que ve el usuario
- THEN el mensaje y la apariencia son idénticos y no se revela que exista un bloqueo ni la cuenta

#### Scenario: Ventana vencida
- GIVEN el último de los 5 fallos ocurrió hace más de 15 minutos
- WHEN inicia sesión con credenciales correctas
- THEN inicia sesión y navega a `menu-principal`

#### Scenario: Login por correo no afectado
- GIVEN el celular de la cuenta está bloqueado por la función
- WHEN inicia sesión con su correo y contraseña correctos
- THEN inicia sesión (solo aplica el límite de GoTrue)

### Requirement: Errores de red y servidor
El sistema MUST distinguir fallos de conectividad/servidor de credenciales incorrectas.

#### Scenario: Sin conexión
- GIVEN no hay red
- WHEN presiona "Iniciar Sesión"
- THEN muestra "Sin conexión. Intenta de nuevo" (no "Credenciales incorrectas")

#### Scenario: Error de servidor
- GIVEN la función o Auth responden 5xx
- WHEN falla el login
- THEN muestra "Ocurrió un error. Intenta de nuevo"

### Requirement: Estado de carga
El botón MUST deshabilitarse mientras la solicitud este en curso.

#### Scenario: Doble toque
- GIVEN login en curso
- WHEN presiona "Iniciar Sesión" de nuevo
- THEN no se envia otra solicitud
