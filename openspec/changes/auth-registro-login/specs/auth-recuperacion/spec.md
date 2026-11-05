# Especificación auth-recuperación (HU-003, HU-004)

## Purpose
Recuperar acceso: solicitar código numérico por correo (OTP), verificarlo y definir nueva contraseña.

## Supuestos (el backlog no los define)
- Duración de expiración del código: la configurada en Supabase (por defecto 1 hora); la app no la fija ni la muestra.
- Longitud del código: la que emita Supabase (numérico, 6 dígitos esperado).
- Rate limit: el del backend; la app SHOULD aplicar espera de 60 s para reenviar (supuesto).
- Mensaje de confirmación único: "Si el correo está registrado, recibirás un código".

## Requirements

### Requirement: Solicitud de código
El sistema MUST ofrecer "¿Olvidaste tu contraseña?" desde login, validar el correo y enviar el código.

#### Scenario: Envio exitoso
- GIVEN usuario en login
- WHEN elige "¿Olvidaste tu contraseña?", ingresa "pablo@example.com" y presiona "Enviar Código"
- THEN se envia el código a ese correo y se muestra la pantalla de ingreso del código

#### Scenario: Correo vacío
- GIVEN correo vacío
- WHEN presiona "Enviar Código"
- THEN muestra "Este campo es obligatorio" y no llama al backend

#### Scenario: Correo con formato inválido
- GIVEN correo "pablo@"
- WHEN presiona "Enviar Código"
- THEN muestra "Correo electrónico inválido" y no llama al backend

### Requirement: Sin divulgación de cuentas
La respuesta MUST ser idéntica exista o no el correo.

#### Scenario: Correo no registrado
- GIVEN "nadie@example.com" sin cuenta
- WHEN presiona "Enviar Código"
- THEN muestra el mismo mensaje de confirmación y avanza a la pantalla de código

### Requirement: Reenvio de código
El sistema SHOULD permitir reenviar el código tras una espera.

#### Scenario: Reenvio con espera
- GIVEN se envio un código hace menos de 60 s
- WHEN intenta reenviar
- THEN la acción está deshabilitada con cuenta regresiva; pasada la espera se habilita

### Requirement: Código inválido o expirado
El código MUST ser numérico y coincidir con el emitido y vigente. Un código incorrecto y uno expirado MUST rechazarse con el mismo mensaje "Código inválido o expirado" (Supabase no distingue ambos casos).

#### Scenario: Código vencido
- GIVEN código emitido y ya expirado
- WHEN ingresa el código y una nueva clave válida y guarda
- THEN muestra "Código inválido o expirado" y no cambia la clave

#### Scenario: Código incorrecto
- GIVEN código emitido "849201"
- WHEN ingresa "111111" y una nueva clave válida y guarda
- THEN muestra "Código inválido o expirado" y no cambia la clave

#### Scenario: Código vacío o no numérico
- GIVEN código "" o "84a201"
- WHEN presiona "Guardar Nueva Contraseña"
- THEN muestra "Ingresa el código numérico" y no llama al backend

### Requirement: Nueva contraseña válida
La nueva contraseña MUST cumplir las reglas de auth-registro (>=8, mayúscula, minúscula, dígito) y coincidir con su confirmación.

#### Scenario: Contraseña débil
- GIVEN código válido y nueva clave "abc123"
- WHEN presiona "Guardar Nueva Contraseña"
- THEN muestra el mensaje de contraseña débil y no cambia la clave

#### Scenario: Confirmación distinta
- GIVEN nueva clave "Clave1234" y confirmación "Clave12345"
- WHEN presiona "Guardar Nueva Contraseña"
- THEN muestra "Las contraseñas no coinciden"

### Requirement: Restablecimiento exitoso
Con código válido y clave válida el sistema MUST actualizar la contraseña.

#### Scenario: Cambio exitoso
- GIVEN código "849201" recibido y vigente
- WHEN ingresa "849201", nueva clave y confirmación válidas y guarda
- THEN se actualiza la clave, se vuelve a login y la nueva clave inicia sesión mientras la anterior es rechazada

### Requirement: Errores de red y servidor
El sistema MUST informar fallos recuperables en ambas pantallas.

#### Scenario: Sin conexión al solicitar
- GIVEN no hay red
- WHEN presiona "Enviar Código"
- THEN muestra "Sin conexión. Intenta de nuevo" y permanece en la pantalla

#### Scenario: Error de servidor al guardar
- GIVEN el backend responde 5xx
- WHEN presiona "Guardar Nueva Contraseña"
- THEN muestra "Ocurrió un error. Intenta de nuevo" y conserva los datos

### Requirement: Estado de carga
Los botones MUST deshabilitarse durante solicitudes en curso.

#### Scenario: Doble toque
- GIVEN solicitud en curso
- WHEN presiona de nuevo el botón
- THEN no se envia otra solicitud
