# Spec: Políticas de privacidad (HU-122)

## Requisitos

- Ajustes ofrece la entrada "Políticas de Privacidad".
- La pantalla explica el tratamiento de la ubicación en Cali y de los datos personales.
- La información es de solo lectura, sin acciones transaccionales.

## Escenarios

### Lectura de políticas de datos y ubicación

```gherkin
Dado que el usuario desea saber cómo se maneja su ubicación
Cuando ingresa a "Políticas de Privacidad"
Entonces lee la explicación sobre la privacidad de geolocalización y datos personales
```
