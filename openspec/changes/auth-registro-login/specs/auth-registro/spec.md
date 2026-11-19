# Especificación auth-registro (HU-001)

## Purpose
Alta de cuenta de Publicador/Solucionador con validación cliente, unicidad de correo y celular, y creación de perfil base.

## Supuestos
- Normalización de celular: quitar espacios, guiones y prefijo opcional `+57`/`57`; resultado `^3\d{9}$`.
- Correo: trim y comparación sin distinguir mayúsculas.
- Edad: 18 cumplidos a la fecha actual del dispositivo.

## Requirements

### Requirement: Campos obligatorios
El sistema MUST exigir nombre, apellido, correo, celular, fecha de nacimiento, contraseña y confirmación; mensaje por campo: "Este campo es obligatorio".

#### Scenario: Todos los campos vacíos
- GIVEN la pantalla de registro sin datos
- WHEN presiona "Registrarse"
- THEN cada campo muestra "Este campo es obligatorio" y no se llama al backend

#### Scenario: Campo solo con espacios
- GIVEN todos los campos válidos salvo apellido "   "
- WHEN presiona "Registrarse"
- THEN apellido muestra "Este campo es obligatorio" y no se crea la cuenta

### Requirement: Formato de correo
El sistema MUST validar el correo con formato `local@dominio.tld`.

#### Scenario: Correo inválido
- GIVEN correo "juan@"
- WHEN presiona "Registrarse"
- THEN muestra "Correo electrónico inválido" y no llama al backend

#### Scenario: Correo con espacios laterales
- GIVEN correo " juan@example.com "
- WHEN se valida
- THEN se acepta y se usa "juan@example.com"

### Requirement: Formato de celular
El sistema MUST aceptar solo celulares colombianos que tras normalizar cumplan `^3\d{9}$`.

#### Scenario: Celular inválido
- GIVEN celular "2001234567" o "300123"
- WHEN presiona "Registrarse"
- THEN muestra "Celular inválido: debe iniciar en 3 y tener 10 dígitos"

#### Scenario: Celular con formato libre
- GIVEN celular "+57 300-123-4567"
- WHEN se valida
- THEN se normaliza a "3001234567" y se acepta

#### Scenario: Celular válido
- GIVEN celular "3001234567"
- WHEN se valida
- THEN se acepta sin cambios

### Requirement: Contraseña robusta
La contraseña MUST tener al menos 8 caracteres con al menos una mayúscula, una minúscula y un dígito.

#### Scenario: Muy corta
- GIVEN contraseña "Ab1"
- WHEN se valida
- THEN muestra "La contraseña debe tener al menos 8 caracteres"

#### Scenario: Sin mayúscula, minúscula o dígito
- GIVEN contraseñas "abcdefg1", "ABCDEFG1", "Abcdefgh"
- WHEN se valida cada una
- THEN muestra "La contraseña debe incluir mayúscula, minúscula y número"

#### Scenario: Contraseña válida
- GIVEN contraseña "Clave1234"
- WHEN se valida
- THEN se acepta

### Requirement: Confirmación de contraseña
La confirmación MUST coincidir exactamente (sensible a mayúsculas).

#### Scenario: No coinciden
- GIVEN contraseña "Clave1234" y confirmación "clave1234"
- WHEN presiona "Registrarse"
- THEN muestra "Las contraseñas no coinciden" y no se crea la cuenta

### Requirement: Edad mínima
La fecha de nacimiento MUST NOT ser futura y el usuario MUST tener 18 años cumplidos.

#### Scenario: Menor de edad
- GIVEN fecha que da 17 años y 364 días
- WHEN presiona "Registrarse"
- THEN muestra "Debes ser mayor de 18 años" y no se crea la cuenta

#### Scenario: Fecha futura
- GIVEN fecha de nacimiento posterior a hoy
- WHEN se valida
- THEN muestra "La fecha de nacimiento no puede ser futura"

#### Scenario: Exactamente 18 hoy
- GIVEN fecha de nacimiento igual a hoy menos 18 años
- WHEN se valida
- THEN se acepta

### Requirement: Unicidad de correo
El sistema MUST rechazar un correo ya registrado.

#### Scenario: Correo ya en uso
- GIVEN existe usuario con "juan@example.com"
- WHEN un nuevo usuario se registra con "juan@example.com"
- THEN muestra "El correo ya está en uso" y no crea la cuenta

#### Scenario: Correo duplicado con otras mayúsculas
- GIVEN existe "juan@example.com"
- WHEN se registra con "JUAN@Example.com"
- THEN muestra "El correo ya está en uso"

### Requirement: Unicidad de celular
El sistema MUST rechazar un celular ya registrado, consultando `telefono_disponible` (solo boolean) antes de crear la cuenta y respaldado por restricción UNIQUE.

#### Scenario: Celular ya en uso
- GIVEN `telefono_disponible("3001234567")` devuelve false
- WHEN presiona "Registrarse"
- THEN muestra "El celular ya está en uso" y no llama al signUp

#### Scenario: Carrera en unicidad
- GIVEN la RPC devolvió true pero el backend rechaza por UNIQUE de teléfono
- WHEN falla el alta
- THEN muestra "El celular ya está en uso" y no queda sesión iniciada

### Requirement: Registro exitoso
Con datos válidos y únicos el sistema MUST crear la cuenta y el perfil (nombre, apellido, celular normalizado, fecha de nacimiento), iniciar sesión y navegar a `perfil-inicial`. "Confirm email" está desactivado.

#### Scenario: Registro válido
- GIVEN datos válidos con correo y celular únicos
- WHEN presiona "Registrarse"
- THEN se crea la cuenta con perfil y se navega a `perfil-inicial`

### Requirement: Errores de red y servidor
El sistema MUST mostrar un error recuperable sin perder lo digitado.

#### Scenario: Sin conexión
- GIVEN no hay red
- WHEN presiona "Registrarse"
- THEN muestra "Sin conexión. Intenta de nuevo" y conserva los campos

#### Scenario: Error inesperado
- GIVEN el backend responde 5xx o error desconocido
- WHEN falla el registro
- THEN muestra "Ocurrió un error. Intenta de nuevo" sin detalles técnicos

### Requirement: Estado de carga
Mientras hay una solicitud en curso el botón MUST estar deshabilitado con indicador de progreso.

#### Scenario: Doble toque
- GIVEN registro en curso
- WHEN presiona "Registrarse" otra vez
- THEN no se envia una segunda solicitud
