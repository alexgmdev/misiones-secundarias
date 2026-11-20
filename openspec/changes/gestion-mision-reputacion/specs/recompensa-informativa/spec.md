# Spec: Recompensa informativa (HU-024)

## Requisitos

- El formulario de creación incluye un campo de texto libre opcional para describir refrigerios, certificados o insumos.
- La interfaz aclara que las recompensas son informativas y no existe pasarela de pago.
- La recompensa se guarda en `missions.reward`, se previsualiza y se muestra en la tarjeta y el detalle.

## Escenarios

### Registro de recompensa escrita

```gherkin
Dado que el publicador desea ofrecer refrigerio
Cuando ingresa en el campo recompensa "Se entregará hidratación y fruta"
Entonces la información se guarda con la misión
Y se muestra en la tarjeta indicando su carácter informativo
```

### Aviso de no transaccionalidad

```gherkin
Dado que el publicador está en el formulario
Cuando consulta la sección de recompensas
Entonces lee que la plataforma no procesa pagos ni transacciones económicas
```
