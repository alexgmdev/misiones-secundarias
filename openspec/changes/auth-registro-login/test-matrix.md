# Matriz de pruebas: Registro, login y recuperación

- **Cambio SDD**: `auth-registro-login`
- **Historias de usuario**: HU-001 (registro), HU-002 (login), HU-003 (solicitar código), HU-004 (cambiar contraseña)
- **Fecha**: 2026-11-06
- **Commit evaluado**: `ca8a217` (HEAD de `dev/david-chicaiza`, incluye los cambios del equipo ya integrados en `develop`)
- **Especificaciones**: `specs/auth-registro/spec.md` (22 escenarios), `specs/auth-login/spec.md` (19), `specs/auth-recuperacion/spec.md` (14); total 55.
- **Ejecución automatizada (2026-11-06)**: `./gradlew :app:testDebugUnitTest` = 187 pruebas, 187 pasan, 0 fallan, 0 omitidas (154 de autenticación; el resto son de misiones). `ktlintCheck`, `detekt` y `assembleDebug` terminan en BUILD SUCCESSFUL.

## Leyenda

**Tipo**

| Tipo | Significado |
|------|-------------|
| Unit | Prueba JVM en `app/src/test` (JUnit 4 + MockK). Se ejecutó realmente. |
| Manual-dispositivo | Se ejecuta en un teléfono o emulador contra el Supabase compartido (sección "Pruebas manuales en dispositivo"). |
| Manual-curl | Se ejecuta con `curl` contra la Edge Function `login-telefono` (sección "Pruebas de la Edge Function"). |
| Manual-dashboard | Se verifica o configura en el dashboard de Supabase / SQL editor. |

**Estado**

| Estado | Significado |
|--------|-------------|
| ✅ Pasa | Ejecutado y con resultado esperado. En filas con varias partes se indica cuál (por ejemplo "✅ Unit"). |
| ❌ Falla | Ejecutado y con resultado distinto del esperado. |
| ⬜ Pendiente | Aún no ejecutado (todas las pruebas manuales están pendientes en esta fecha). |
| ⚠️ Desviación | El comportamiento actual del código difiere del texto de la spec por un cambio del equipo; requiere actualizar la spec. |

Nota de nomenclatura: en la columna Cobertura, `Clase.método` son pruebas unitarias existentes (todas ✅). `MD-nn` remite a la checklist de dispositivo y `CU-nn` a la lista de curl.

## Requisitos previos de entorno

| ID | Requisito | Verificación | Estado conocido |
|----|-----------|--------------|-----------------|
| ENV-1 | SQL `supabase/changes/2026-11-03_auth_profiles_privacy.sql` aplicado (tabla `profile_private`, `first_name`/`last_name`, `handle_new_user`, RPC `telefono_disponible`, RLS) | SQL editor: `select public.telefono_disponible('3000000000');` devuelve `true`; existe `public.profile_private` | ❌ NO aplicado al 2026-11-05 (RPC `telefono_disponible` y tabla `profile_private` respondían 404) |
| ENV-2 | SQL `supabase/changes/2026-11-03_login_attempts.sql` aplicado (tabla `login_attempts`, índices, `purge_login_attempts`) | SQL editor: `select count(*) from public.login_attempts;` | ❌ NO aplicado al 2026-11-05 (404) |
| ENV-3 | Edge Function `login-telefono` desplegada (`supabase functions deploy login-telefono`); `SUPABASE_URL`, `SUPABASE_ANON_KEY` y `SUPABASE_SERVICE_ROLE_KEY` los inyecta Supabase; `--no-verify-jwt` si la clave es `sb_publishable_...` | `curl` del paso CU-01 responde 200 | ⬜ No verificado (depende de ENV-1 y ENV-2) |
| ENV-4 | Auth: "Confirm email" DESACTIVADO | Dashboard > Authentication > Providers > Email | ⬜ No verificado |
| ENV-5 | Plantilla "Reset Password" con `{{ .Token }}` (código numérico de 6 dígitos), expiración (design.md sugiere 900 s) y SMTP propio | Dashboard > Authentication > Email Templates; llega un correo con código | ⬜ No verificado |
| ENV-6 | Política de contraseña: mínimo 8 con minúscula, mayúscula y dígitos | Dashboard > Authentication > Policies | ⬜ No verificado |
| ENV-7 | `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `local.properties` (APK de prueba construido con ellas) | `./gradlew assembleDebug` y la app llega al servidor | ✅ Build OK localmente; credenciales válidas pendientes de confirmar con la prueba MD-04 |

Estado actual conocido: mientras ENV-1 y ENV-2 no estén aplicados, el registro en dispositivo muestra "Ocurrió un error. Intenta de nuevo" (la llamada a `telefono_disponible` devuelve 404, que el cliente trata como error de servidor) y el login por celular tampoco puede funcionar. Todas las filas que dependen de ENV-1/ENV-2/ENV-3 están bloqueadas para su verificación manual hasta que el tech lead los aplique.

Usuarios de prueba sugeridos (crear tras aplicar ENV-1): U1 `ana@example.com` / celular `3001234567` / clave `Clave1234`; U2 `pablo@example.com` (correo real para recibir códigos) / celular `3109876543`.

## Capacidad auth-registro (HU-001)

| ID | HU | Requisito | Escenario | Tipo | Cobertura | Prerrequisitos | Estado | Observaciones |
|----|----|-----------|-----------|------|-----------|----------------|--------|---------------|
| AR-01 | HU-001 | Campos obligatorios | Todos los campos vacíos | Unit + Manual-dispositivo | Unit: `RegisterViewModelTest.allEmptyFieldsShowRequiredAndDoNotCallBackend`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-11 | ENV-7 | ✅ Unit / ⬜ Manual | Las 7 claves de `RegisterField` reciben `Required` y no se invoca el caso de uso. |
| AR-02 | HU-001 | Campos obligatorios | Campo solo con espacios (apellido) | Unit + Manual-dispositivo | Unit: `RegisterViewModelTest.blankLastNameShowsRequiredOnlyOnThatField`, `NameValidatorTest.blankNameIsRequired`, `NameValidatorTest.emptyNameIsRequired`. Manual: MD-12 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-03 | HU-001 | Formato de correo | Correo inválido ("juan@") | Unit + Manual-dispositivo | Unit: `EmailValidatorTest.emailWithoutDomainIsInvalid`, `EmailValidatorTest.emailWithoutTldIsInvalid`, `EmailValidatorTest.emailWithInnerSpacesIsInvalid`, `RegisterViewModelTest.invalidEmailPhonePasswordAndMismatchAreReportedTogether`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-13 ("Correo electrónico inválido") | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-04 | HU-001 | Formato de correo | Correo con espacios laterales | Unit | `EmailValidatorTest.emailWithSurroundingSpacesIsAccepted`, `EmailValidatorTest.normalizeTrimsSurroundingSpaces`, `RegisterViewModelTest.validFormCallsUseCaseWithNormalizedDataAndMarksRegistered` (el correo llega recortado al caso de uso) | ninguno | ✅ Unit | Se ejercita de forma indirecta también en MD-15. |
| AR-05 | HU-001 | Formato de celular | Celular inválido ("2001234567", "300123") | Unit + Manual-dispositivo | Unit: `PhoneValidatorTest.phoneNotStartingWithThreeIsRejected`, `PhoneValidatorTest.shortPhoneIsRejected`, `PhoneValidatorTest.invalidPhoneReportsInvalidPhone`, `RegisterViewModelTest.invalidEmailPhonePasswordAndMismatchAreReportedTogether`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-14 ("Celular inválido: debe iniciar en 3 y tener 10 dígitos") | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-06 | HU-001 | Formato de celular | Celular con formato libre ("+57 300-123-4567") | Unit | `PhoneValidatorTest.freeFormatWithPrefixSpacesAndDashesIsNormalized`, `PhoneValidatorTest.prefixWithoutPlusIsRemoved`, `PhoneValidatorTest.freeFormatPhoneIsValid`, `RegisterViewModelTest.validFormCallsUseCaseWithNormalizedDataAndMarksRegistered` (envía "3001234567") | ninguno | ✅ Unit | MD-15 lo usa como dato de entrada del registro real. |
| AR-07 | HU-001 | Formato de celular | Celular válido ("3001234567") | Unit | `PhoneValidatorTest.validPhoneIsUnchanged` | ninguno | ✅ Unit | |
| AR-08 | HU-001 | Contraseña robusta | Muy corta ("Ab1") | Unit + Manual-dispositivo | Unit: `PasswordValidatorTest.tooShortPasswordReportsLengthError`, `PasswordValidatorTest.shortPasswordUnmetRulesIncludeLength`, `RegisterViewModelTest.invalidEmailPhonePasswordAndMismatchAreReportedTogether`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-16 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-09 | HU-001 | Contraseña robusta | Sin mayúscula, minúscula o dígito | Unit + Manual-dispositivo | Unit: `PasswordValidatorTest.passwordsMissingAClassReportClassesError`, `PasswordValidatorTest.missingUppercaseIsReported`, `PasswordValidatorTest.missingLowercaseIsReported`, `PasswordValidatorTest.missingDigitIsReported`. Manual: MD-16 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-10 | HU-001 | Contraseña robusta | Contraseña válida ("Clave1234") | Unit | `PasswordValidatorTest.validPasswordIsAccepted`, `PasswordValidatorTest.validPasswordHasNoUnmetRules` | ninguno | ✅ Unit | |
| AR-11 | HU-001 | Confirmación de contraseña | No coinciden (sensible a mayúsculas) | Unit + Manual-dispositivo | Unit: `PasswordValidatorTest.confirmationIsCaseSensitive`, `PasswordValidatorTest.matchingConfirmationIsAccepted`, `PasswordValidatorTest.emptyConfirmationIsRequired`, `RegisterViewModelTest.invalidEmailPhonePasswordAndMismatchAreReportedTogether` (usa "otra", no la variante de mayúsculas). Manual: MD-17 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-12 | HU-001 | Edad mínima | Menor de edad (17 años y 364 días) | Unit + Manual-dispositivo | Unit: `BirthDateValidatorTest.seventeenYearsAnd364DaysIsUnderAge`, `RegisterViewModelTest.underAgeAndFutureBirthDatesAreReported`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-18 | ENV-7 | ✅ Unit / ⬜ Manual | El unit usa reloj fijo 2026-11-03; en dispositivo se calcula con la fecha real. |
| AR-13 | HU-001 | Edad mínima | Fecha futura | Unit | `BirthDateValidatorTest.futureDateIsRejected`, `RegisterViewModelTest.underAgeAndFutureBirthDatesAreReported`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages` | ninguno | ✅ Unit | En dispositivo el selector puede impedir elegir fechas futuras; verificar en MD-18. |
| AR-14 | HU-001 | Edad mínima | Exactamente 18 hoy | Unit | `BirthDateValidatorTest.exactlyEighteenTodayIsAccepted`, `BirthDateValidatorTest.adultIsAccepted` | ninguno | ✅ Unit | |
| AR-15 | HU-001 | Unicidad de correo | Correo ya en uso | Unit + Manual-dispositivo | Unit: `RegisterUseCaseTest.emailInUseIsPropagated`, `RegisterViewModelTest.emailInUseAndUnexpectedFailuresAreMapped`, `AuthErrorMapperTest.mapsUserAlreadyExistsToEmailInUse`, `AuthErrorMapperTest.mapsEmailExistsToEmailInUse`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-19 ("El correo ya está en uso") | ENV-1, ENV-4, ENV-7 | ✅ Unit / ⬜ Manual | Bloqueado en dispositivo mientras ENV-1 no esté aplicado. |
| AR-16 | HU-001 | Unicidad de correo | Correo duplicado con otras mayúsculas ("JUAN@Example.com") | Manual-dispositivo | Manual: MD-20 | ENV-1, ENV-4, ENV-7 | ⬜ Pendiente | SIN prueba unitaria: la comparación sin distinguir mayúsculas la hace GoTrue (el cliente solo hace `trim`). Hallazgo: verificar el comportamiento real en el dispositivo. |
| AR-17 | HU-001 | Unicidad de celular | Celular ya en uso (RPC devuelve false) | Unit + Manual-dispositivo | Unit: `RegisterUseCaseTest.phoneAlreadyInUseFailsWithoutCallingSignUp`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-21 ("El celular ya está en uso") | ENV-1, ENV-4, ENV-7 | ✅ Unit / ⬜ Manual | La RPC hoy responde 404: ahí el usuario ve "Ocurrió un error. Intenta de nuevo". |
| AR-18 | HU-001 | Unicidad de celular | Carrera en unicidad (UNIQUE rechaza tras RPC true) | Unit + Manual-dashboard | Unit: `RegisterUseCaseTest.serverErrorBecomesPhoneInUseWhenRaceTookThePhone` (segunda consulta devuelve false y se mapea a `PhoneInUse`). Manual: MD-22 (difícil de reproducir; ver pasos) | ENV-1, ENV-4 | ✅ Unit parcial / ⬜ Manual | El unit no comprueba "no queda sesión iniciada"; confirmar en dispositivo. La spec habla de rechazo por UNIQUE; el código lo resuelve volviendo a consultar la RPC tras un `ServerError`. |
| AR-19 | HU-001 | Registro exitoso | Registro válido: cuenta, perfil, sesión y navegación a `perfil-inicial` | Unit + Manual-dispositivo + Manual-dashboard | Unit: `RegisterUseCaseTest.validDataSignsUpAndSucceeds`, `RegisterViewModelTest.validFormCallsUseCaseWithNormalizedDataAndMarksRegistered`. Manual: MD-15 y verificación en Dashboard (`profiles` y `profile_private`) | ENV-1, ENV-4, ENV-6, ENV-7 | ✅ Unit / ⬜ Manual | La navegación a `InitialProfile` (`AppNavigation`) y la creación del perfil por trigger NO tienen prueba automatizada. En dispositivo hoy falla por ENV-1. |
| AR-20 | HU-001 | Errores de red y servidor | Sin conexión | Unit + Manual-dispositivo | Unit: `RegisterUseCaseTest.signUpWithoutConnectionIsPropagated`, `RegisterUseCaseTest.phoneCheckWithoutConnectionFailsWithNoConnection`, `RegisterViewModelTest.failureExposesTypedErrorKeepsFieldsAndAllowsRetry`, `AuthErrorMapperTest.mapsIoExceptionAndTimeoutToNoConnection`, `AuthErrorMapperTest.mapsWrappedIoExceptionToNoConnection`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-23 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AR-21 | HU-001 | Errores de red y servidor | Error inesperado (5xx / desconocido) | Unit + Manual-dispositivo | Unit: `RegisterUseCaseTest.serverErrorStaysServerErrorWhenPhoneIsStillAvailable`, `RegisterViewModelTest.emailInUseAndUnexpectedFailuresAreMapped`, `AuthErrorMapperTest.mapsServerErrorsToServerErrorEvenInLogin`, `AuthErrorMapperTest.mapsUnknownThrowableToServerError`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-15 hoy reproduce este mensaje por ENV-1 | ENV-7 | ✅ Unit / ⬜ Manual | Observado el 2026-11-05 en dispositivo: "Ocurrió un error. Intenta de nuevo" por RPC 404 (coincide con el texto de la spec, pero por una causa de entorno). |
| AR-22 | HU-001 | Estado de carga | Doble toque | Unit + Manual-dispositivo | Unit: `RegisterViewModelTest.secondTapWhileLoadingDoesNotSendASecondRequest`. Manual: MD-24 (botón deshabilitado con indicador) | ENV-7 | ✅ Unit / ⬜ Manual | El unit no cubre la UI (botón deshabilitado e indicador). |

## Capacidad auth-login (HU-002)

| ID | HU | Requisito | Escenario | Tipo | Cobertura | Prerrequisitos | Estado | Observaciones |
|----|----|-----------|-----------|------|-----------|----------------|--------|---------------|
| AL-01 | HU-002 | Campos obligatorios | Campos vacíos | Unit + Manual-dispositivo | Unit: `LoginViewModelTest.emptyFieldsShowRequiredPerFieldAndDoNotCallBackend`, `LoginViewModelTest.blankIdentifierOnlyFlagsThatField`, `LoginViewModelTest.missingPasswordOnlyFlagsThatField`, `AuthMessagesTest.fieldErrorsUseExactSpecMessages`. Manual: MD-31 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AL-02 | HU-002 | Login con correo | Éxito con correo y navegación a `menu-principal` | Unit + Manual-dispositivo | Unit: `SignInUseCaseTest.identifierWithAtSignsInWithEmailAndNeverTouchesPhoneLogin`, `SignInUseCaseTest.emailIsTrimmedBeforeSignIn`, `LoginViewModelTest.validCredentialsSignInAndMarkSignedIn`, `LoginViewModelTest.handledSignInDoesNotRemainTrueWhenLoginScreenIsOpenedAgain`. Manual: MD-35 | ENV-1, ENV-7 | ✅ Unit / ⬜ Manual | `AuthRepositoryImpl.signIn` y la navegación a `MainMenu` no tienen prueba automatizada. |
| AL-03 | HU-002 | Login con celular | Éxito con celular (importa la sesión) | Unit + Manual-dispositivo + Manual-curl | Unit: `SignInUseCaseTest.freeFormatPhoneIsNormalizedBeforeSending` (llama a `signInWithPhone`), `LoginViewModelTest.validCredentialsSignInAndMarkSignedIn`. Manual: MD-36 y CU-01 | ENV-1, ENV-2, ENV-3, ENV-7 | ✅ Unit parcial / ⬜ Manual | `AuthRepositoryImpl.importPhoneSession` (ahora solo `importAuthToken(..., retrieveUser = true)` + `checkNotNull(currentUserOrNull())`) NO tiene prueba; riesgo por el cambio del equipo, validar en dispositivo. |
| AL-04 | HU-002 | Login con celular | Celular con formato libre ("300 123 4567") | Unit + Manual-curl | Unit: `SignInUseCaseTest.freeFormatPhoneIsNormalizedBeforeSending`, `SignInUseCaseTest.phonePrefixedWithCountryCodeIsNormalized`. Curl: CU-04 (la función también normaliza) | ENV-3 (curl) | ✅ Unit / ⬜ Curl | |
| AL-05 | HU-002 | Error genérico | Clave incorrecta con correo | Unit + Manual-dispositivo | Unit: `SignInUseCaseTest.wrongPasswordOnEmailIsInvalidCredentials`, `LoginViewModelTest.failureExposesTypedErrorKeepsFieldsAndStaysSignedOut`, `AuthErrorMapperTest.mapsInvalidCredentialsDependingOnOperation`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-32 ("Credenciales incorrectas") | ENV-1, ENV-7 | ✅ Unit / ⬜ Manual | |
| AL-06 | HU-002 | Error genérico | Clave incorrecta con celular | Unit + Manual-dispositivo + Manual-curl | Unit: `AuthErrorMapperTest.mapsEdgeFunctionGenericErrorsWithoutCodeToInvalidCredentialsInLogin`, `LoginViewModelTest.failureExposesTypedErrorKeepsFieldsAndStaysSignedOut` (usa celular), `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-33; CU-02 | ENV-1, ENV-2, ENV-3 | ✅ Unit / ⬜ Manual | |
| AL-07 | HU-002 | Error genérico | Correo inexistente | Unit + Manual-dispositivo | Unit: `SignInUseCaseTest.wrongPasswordOnEmailIsInvalidCredentials`, `AuthErrorMapperTest.mapsInvalidCredentialsDependingOnOperation` (GoTrue responde `invalid_credentials` para ambos casos). Manual: MD-34 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AL-08 | HU-002 | Celular desconocido | Celular sin cuenta ("3109999999") | Unit + Manual-dispositivo + Manual-curl | Unit: `SignInUseCaseTest.unknownPhoneIsInvalidCredentialsLikeWrongPassword`, `SignInUseCaseTest.invalidPhoneFormatFailsWithInvalidCredentialsWithoutNetwork`. Manual: MD-34b; CU-03 (cuerpo idéntico al de CU-02) | ENV-1, ENV-2, ENV-3 | ✅ Unit / ⬜ Manual | |
| AL-09 | HU-002 | Correo no expuesto | La respuesta de la función solo contiene sesión o error genérico | Manual-curl | CU-01, CU-02, CU-03, CU-05 y revisión de `supabase/functions/login-telefono/index.ts` | ENV-2, ENV-3 | ⬜ Pendiente | SIN prueba automatizada (la Edge Function no tiene pruebas Deno ni de integración). |
| AL-10 | HU-002 | Sesión persistida | Reapertura con sesión abre `menu-principal` | Unit + Manual-dispositivo | Unit: `SessionViewModelTest.goesToMainMenuWhenThereIsASession`, `ObserveSessionUseCaseTest.emitsTheStatesOfTheRepositoryInOrder`. Manual: MD-37 | ENV-1, ENV-7 | ✅ Unit / ⬜ Manual | `AuthRepositoryImpl.observeSession` (mapeo de `NetworkError` a AUTHENTICATED solo si hay usuario actual) NO tiene prueba automatizada. |
| AL-11 | HU-002 | Sesión persistida | Sin sesión: la spec dice que se muestra login | Unit + Manual-dispositivo | Unit: `SessionViewModelTest.goesToLoginWhenThereIsNoSession` (devuelve `StartDestination.LOGIN`, que `AppNavigation` traduce a `AppDestination.Welcome`), `SessionViewModelTest.staysLoadingWhileSessionIsLoadingFromStorage`. Manual: MD-01 | ENV-7 | ⚠️ Desviación | Desviación: pasa por Welcome (portada con "Iniciar sesión" / "Crear cuenta"); el login se alcanza con un toque adicional. Requiere actualizar la spec: "Sin sesión: muestra Welcome". Ningún unit cubre `AppNavigation.toAppDestination`. |
| AL-12 | HU-002 | Límite de intentos | Bloqueo tras 5 fallos por celular (429, la app muestra "Credenciales incorrectas") | Manual-curl + Manual-dispositivo | Curl: CU-05. Manual: MD-38. Unit parcial (solo cliente): `AuthErrorMapperTest.mapsOther4xxInLoginToInvalidCredentials` (429 en LOGIN se muestra como credenciales incorrectas) | ENV-2, ENV-3 | ⬜ Pendiente | La lógica de bloqueo vive solo en la Edge Function y NO tiene prueba automatizada. |
| AL-13 | HU-002 | Límite de intentos | Bloqueo por IP con celulares distintos | Manual-curl | CU-06. Unit parcial (cliente): `AuthErrorMapperTest.mapsOther4xxInLoginToInvalidCredentials` | ENV-2, ENV-3 | ⬜ Pendiente | Sin prueba automatizada de la función. Requiere 5 celulares distintos desde la misma IP. |
| AL-14 | HU-002 | Límite de intentos | Bloqueo indistinguible de clave incorrecta | Manual-curl + Manual-dispositivo | Curl: comparar cuerpo y código de CU-02 (401) con CU-05 (429): cuerpo idéntico `{"error":"credenciales_invalidas"}`. Manual: MD-38 (mismo mensaje y apariencia). Unit parcial: `AuthErrorMapperTest.mapsOther4xxInLoginToInvalidCredentials`, `AuthErrorMapperTest.mapsEdgeFunctionGenericErrorsWithoutCodeToInvalidCredentialsInLogin` | ENV-2, ENV-3 | ⬜ Pendiente | El código HTTP difiere (401 vs 429) pero la app los mapea igual. |
| AL-15 | HU-002 | Límite de intentos | Ventana vencida (más de 15 minutos) | Manual-curl + Manual-dashboard | CU-08 (se retroceden los intentos con SQL) | ENV-2, ENV-3 | ⬜ Pendiente | Sin prueba automatizada. |
| AL-16 | HU-002 | Límite de intentos | Login por correo no afectado con el celular bloqueado | Manual-dispositivo + Manual-curl | Manual: MD-39. Curl: CU-07. Unit parcial: `SignInUseCaseTest.identifierWithAtSignsInWithEmailAndNeverTouchesPhoneLogin` (el correo nunca invoca la función) | ENV-2, ENV-3 | ⬜ Pendiente | |
| AL-17 | HU-002 | Errores de red y servidor | Sin conexión (no "Credenciales incorrectas") | Unit + Manual-dispositivo | Unit: `SignInUseCaseTest.noConnectionIsPropagated`, `LoginViewModelTest.noConnectionAndUnexpectedFailuresAreMapped`, `AuthErrorMapperTest.mapsIoExceptionAndTimeoutToNoConnection`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-40 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AL-18 | HU-002 | Errores de red y servidor | Error de servidor (5xx) | Unit | `SignInUseCaseTest.serverErrorIsPropagated`, `SignInUseCaseTest.unexpectedExceptionIsReducedToServerError`, `SignInUseCaseTest.unrelatedAuthErrorsAreReducedToServerError`, `LoginViewModelTest.noConnectionAndUnexpectedFailuresAreMapped`, `AuthErrorMapperTest.mapsServerErrorsToServerErrorEvenInLogin` | ninguno | ✅ Unit | No se reproduce de forma sencilla en dispositivo (se puede intentar con la función no desplegada, ver observación en MD-41). |
| AL-19 | HU-002 | Estado de carga | Doble toque | Unit + Manual-dispositivo | Unit: `LoginViewModelTest.secondTapWhileLoadingDoesNotSendASecondRequest`. Manual: MD-42 | ENV-7 | ✅ Unit / ⬜ Manual | |

## Capacidad auth-recuperacion (HU-003, HU-004)

| ID | HU | Requisito | Escenario | Tipo | Cobertura | Prerrequisitos | Estado | Observaciones |
|----|----|-----------|-----------|------|-----------|----------------|--------|---------------|
| AC-01 | HU-003 | Solicitud de código | Envío exitoso y paso a la pantalla de código | Unit + Manual-dispositivo | Unit: `RequestPasswordResetUseCaseTest.registeredEmailSucceedsAndSendsTheTrimmedEmail`, `RequestCodeViewModelTest.validEmailSendsTrimmedEmailAndMarksCodeSent`. Manual: MD-51 (llega correo con código de 6 dígitos) | ENV-1, ENV-5, ENV-7 | ✅ Unit / ⬜ Manual | Requiere un correo real de un usuario existente (U2). La entrega depende del SMTP configurado. |
| AC-02 | HU-003 | Solicitud de código | Correo vacío | Unit + Manual-dispositivo | Unit: `RequestCodeViewModelTest.emptyEmailShowsRequiredAndDoesNotCallBackend`. Manual: MD-50 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-03 | HU-003 | Solicitud de código | Correo con formato inválido | Unit + Manual-dispositivo | Unit: `RequestCodeViewModelTest.malformedEmailShowsInvalidEmailAndDoesNotCallBackend`, `RequestPasswordResetUseCaseTest.invalidEmailFormatFailsWithoutCallingTheBackend`, `EmailValidatorTest.emailWithoutDomainIsInvalid`. Manual: MD-50 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-04 | HU-003 | Sin divulgación de cuentas | Correo no registrado: misma confirmación y avanza | Unit + Manual-dispositivo | Unit: `RequestPasswordResetUseCaseTest.anyServerSideFailureIsReportedAsTheSameNeutralSuccess`, `AuthMessagesTest.recoveryMessagesUseExactSpecText`. Manual: MD-52 | ENV-5, ENV-7 | ✅ Unit / ⬜ Manual | El mensaje "Si el correo está registrado, recibirás un código" se muestra en `ResetPasswordScreen`; ningún unit cubre la pantalla. |
| AC-05 | HU-003 | Reenvío de código | Reenvío con espera de 60 s | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.resendIsDisabledWithCountdownForSixtySecondsAndThenEnabled`, `ResetPasswordViewModelTest.resendAfterCooldownRequestsANewCodeAndRestartsTheCountdown`, `ResetPasswordViewModelTest.resendWithoutConnectionShowsErrorAndDoesNotRestartCountdown`. Manual: MD-53 ("Reenviar código en N s" y luego "Reenviar código") | ENV-5, ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-06 | HU-004 | Código inválido o expirado | Código vencido | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.invalidOrExpiredCodeShowsErrorKeepsDataAndDoesNotChangePassword`, `VerifyRecoveryCodeUseCaseTest.rejectedCodeIsInvalidOrExpired`, `AuthErrorMapperTest.mapsOtpExpiredToInvalidOrExpiredCodeInAnyOperation`, `AuthMessagesTest.authErrorsUseExactSpecMessages`. Manual: MD-55 | ENV-5, ENV-7 | ✅ Unit / ⬜ Manual | Requiere esperar la expiración configurada (por defecto 1 h; usar 900 s o una expiración corta de prueba). |
| AC-07 | HU-004 | Código inválido o expirado | Código incorrecto ("111111") | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.invalidOrExpiredCodeShowsErrorKeepsDataAndDoesNotChangePassword` (usa "111111"), `VerifyRecoveryCodeUseCaseTest.rejectedCodeIsInvalidOrExpired`, `AuthErrorMapperTest.mapsClientRejectionsWhileVerifyingCodeToInvalidOrExpiredCode`. Manual: MD-54 ("Código inválido o expirado") | ENV-5, ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-08 | HU-004 | Código inválido o expirado | Código vacío o no numérico | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.emptyOrNonNumericCodeShowsInvalidCodeAndDoesNotCallBackend`, `RecoveryCodeValidatorTest.emptyOrBlankCodeIsInvalid`, `RecoveryCodeValidatorTest.nonNumericOrOutOfRangeCodeIsInvalid`, `VerifyRecoveryCodeUseCaseTest.nonNumericCodeFailsWithoutCallingTheBackend`, `AuthMessagesTest.recoveryMessagesUseExactSpecText`. Manual: MD-56 ("Ingresa el código numérico") | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-09 | HU-004 | Nueva contraseña válida | Contraseña débil ("abc123") | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.weakPasswordShowsErrorAndDoesNotCallBackend` (espera `PasswordTooShort`), `ChangePasswordUseCaseTest.weakPasswordFailsWithoutCallingTheBackend`, `PasswordValidatorTest.tooShortPasswordReportsLengthError`. Manual: MD-57 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-10 | HU-004 | Nueva contraseña válida | Confirmación distinta | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.mismatchedConfirmationShowsMismatch`, `PasswordValidatorTest.confirmationIsCaseSensitive`. Manual: MD-58 ("Las contraseñas no coinciden") | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-11 | HU-004 | Restablecimiento exitoso | Cambio exitoso, vuelve a login; la nueva clave inicia sesión y la anterior es rechazada | Unit + Manual-dispositivo | Unit: `ResetPasswordViewModelTest.successVerifiesThenChangesPassword`, `ChangePasswordUseCaseTest.successUpdatesPasswordAndThenSignsOut`, `ChangePasswordUseCaseTest.failingSignOutDoesNotUndoASuccessfulChange`, `VerifyRecoveryCodeUseCaseTest.validCodeIsVerifiedWithTrimmedValues`. Manual: MD-59 | ENV-1, ENV-5, ENV-6, ENV-7 | ✅ Unit / ⬜ Manual | El aviso "Contraseña actualizada. Inicia sesión con tu nueva contraseña" y la navegación a Login están en `AppNavigation` sin prueba unitaria. |
| AC-12 | HU-003 | Errores de red y servidor | Sin conexión al solicitar | Unit + Manual-dispositivo | Unit: `RequestPasswordResetUseCaseTest.noConnectionIsTheOnlyFailureReported`, `RequestCodeViewModelTest.noConnectionStaysOnScreenKeepsEmailAndShowsError`, `RequestCodeViewModelTest.unexpectedFailureIsShownAsServerError`. Manual: MD-60 | ENV-7 | ✅ Unit / ⬜ Manual | |
| AC-13 | HU-004 | Errores de red y servidor | Error de servidor al guardar y conserva los datos | Unit | `ResetPasswordViewModelTest.serverErrorWhileSavingKeepsDataAndShowsError`, `ResetPasswordViewModelTest.failureAfterVerifyingOnlyRetriesThePasswordWithoutReverifyingTheCode`, `ResetPasswordViewModelTest.samePasswordErrorIsExposed`, `VerifyRecoveryCodeUseCaseTest.errorsAreReducedToInvalidCodeNoConnectionOrServerError`, `ChangePasswordUseCaseTest.serverWeakPasswordNetworkAndUnexpectedErrorsAreMapped` | ninguno | ✅ Unit | No se reproduce de forma sencilla en dispositivo. |
| AC-14 | HU-003/HU-004 | Estado de carga | Doble toque en ambas pantallas | Unit + Manual-dispositivo | Unit: `RequestCodeViewModelTest.secondTapWhileLoadingDoesNotSendASecondRequest`, `ResetPasswordViewModelTest.secondTapWhileLoadingDoesNotSendASecondRequest`. Manual: MD-61 | ENV-7 | ✅ Unit / ⬜ Manual | |

### Mitigaciones del propio cambio (design.md, tarea 4.7; no son escenarios de la spec)

| ID | HU | Comportamiento | Tipo | Cobertura | Estado | Observaciones |
|----|----|----------------|------|-----------|--------|---------------|
| AX-01 | HU-004 | Al abandonar la pantalla tras verificar el código (sin cambiar la clave) se cierra la sesión temporal creada por el OTP | Unit + Manual-dispositivo | Unit: `CancelRecoveryUseCaseTest.signsOutTheTemporarySession`, `CancelRecoveryUseCaseTest.signOutFailureIsReturnedAsFailureWithoutThrowing`, `ResetPasswordViewModelTest.abandoningAfterVerifyingTheCodeSignsOutTheTemporarySession`, `ResetPasswordViewModelTest.abandoningBeforeVerifyingOrAfterChangingThePasswordDoesNothing`, `ResetPasswordViewModelTest.failingSignOutOnAbandonIsIgnored`. Manual: MD-62 | ✅ Unit / ⬜ Manual | Riesgo residual documentado: proceso matado a mitad de la recuperación. Con `SessionViewModel` ahora observando la sesión, verificar que esa sesión temporal no deja al usuario "logueado" ni lo lleva a `menu-principal` (MD-62). |

## Comportamientos añadidos por el equipo (fuera de la spec original)

Estos comportamientos se integraron en `develop` después de nuestros commits. No tienen escenarios en las specs; se listan con su cobertura real.

| ID | HU | Comportamiento | Tipo | Cobertura | Prerrequisitos | Estado | Observaciones |
|----|----|----------------|------|-----------|----------------|--------|---------------|
| TM-01 | HU-002 | Sin sesión, la app arranca en `WelcomeScreen` (portada con "Iniciar Sesión" y "Crear cuenta") | Unit parcial + Manual-dispositivo | Unit: `SessionViewModelTest.goesToLoginWhenThereIsNoSession` (solo el valor `StartDestination.LOGIN`). Manual: MD-01 | ENV-7 | ⚠️ Desviación / ⬜ Manual | Reemplaza "abre Login" de la spec (ver AL-11). La traducción `LOGIN -> Welcome` está en `AppNavigation` (privada, sin prueba). |
| TM-02 | HU-001/002 | Atrás desde Login o Register vuelve a Welcome; atrás desde RequestCode vuelve a Login | Manual-dispositivo | MD-02 | ENV-7 | ⬜ Pendiente | Sin prueba automatizada (`BackHandler` en `AppNavigation`). |
| TM-03 | HU-002 | `SessionViewModel` observa la sesión de forma continua (`isLoggedIn`, `isSessionResolved`, `updateLoginState`) | Unit | `SessionViewModelTest.staysLoadingWhileSessionIsLoadingFromStorage`, `SessionViewModelTest.goesToMainMenuWhenThereIsASession`, `SessionViewModelTest.decidesOnlyOnceAfterLoading`, `SessionViewModelTest.exposesLogoutAfterTheInitialDestinationWasResolved`, `SessionViewModelTest.updatesTheBooleanStateExplicitlyAfterLoginAndLogout` | ninguno | ✅ Unit | |
| TM-04 | HU-002 | Redirección de destinos protegidos a Welcome cuando no hay sesión, con aviso "Debes iniciar sesión para ingresar a la aplicación" | Manual-dispositivo | MD-43 | ENV-1, ENV-7 | ⬜ Pendiente | SIN prueba automatizada: `shouldRedirectToWelcome` y los avisos son privados en `AppNavigation.kt`. |
| TM-05 | HU-002 | Cerrar sesión desde Ajustes (`SignOutUseCase`, `SettingsScreen`, `SettingsViewModel`): vuelve a Welcome con aviso "Sesión cerrada correctamente" | Unit + Manual-dispositivo | Unit: `SignOutUseCaseTest.closesTheCurrentSession`, `SignOutUseCaseTest.exposesRepositoryFailure`, `SettingsViewModelTest.successfulSignOutEmitsAOneTimeEvent`, `SettingsViewModelTest.failedSignOutShowsAndDismissesTheError`. Manual: MD-44 | ENV-1, ENV-7 | ✅ Unit / ⬜ Manual | El mensaje de error mostrado al fallar el cierre ("No fue posible cerrar sesión") no se verifica con texto exacto en el unit. |
| TM-06 | HU-002 | `LoginViewModel.onSignInHandled()` limpia `isSignedIn` y la clave al volver a abrir Login | Unit | `LoginViewModelTest.handledSignInDoesNotRemainTrueWhenLoginScreenIsOpenedAgain` | ninguno | ✅ Unit | Verificar también en MD-45 (tras cerrar sesión el formulario de Login aparece vacío). |
| TM-07 | HU-002 | `AuthRepositoryImpl.importPhoneSession` usa solo `importAuthToken(..., retrieveUser = true)` + `checkNotNull(currentUserOrNull())` | Manual-dispositivo + Manual-curl | MD-36, CU-01 | ENV-3 | ⬜ Pendiente | SIN prueba automatizada del repositorio. Dependencia de la deserialización de la sesión sin `user`. |
| TM-08 | HU-002 | `observeSession` mapea `NetworkError` a AUTHENTICATED solo si existe un usuario actual | Manual-dispositivo | MD-46 (reabrir sin red con sesión guardada y sin sesión) | ENV-7 | ⬜ Pendiente | SIN prueba automatizada del repositorio. |
| TM-09 | HU-003/004 | Aviso en Login tras restablecer la contraseña: "Contraseña actualizada. Inicia sesión con tu nueva contraseña" | Manual-dispositivo | MD-59 (paso final) | ENV-5 | ⬜ Pendiente | Sin prueba automatizada (constante privada en `AppNavigation.kt`). |

## Pruebas manuales en dispositivo

Checklist consolidada para ejecutar en una sola sesión (tester: Helkin, QA). Orden pensado para no bloquear datos: primero lo que no necesita cuentas, luego registro, cierre de sesión, login, recuperación y, al final, bloqueo por intentos (deja el celular de U1 bloqueado 15 minutos).

Antes de empezar: confirmar ENV-1 a ENV-7 (sin ENV-1 y ENV-2 los bloques B, C, D, E y F no se pueden completar). APK debug instalado con credenciales Supabase correctas. Llevar un correo real para U2 y poder activar el modo avión.

Las columnas "Resultado" y "Observaciones" se dejan vacías para el tester (Pasa / Falla / Bloqueado).

### Bloque A: Arranque y navegación inicial (sin cuentas)

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-01 | AL-11, TM-01 | 1. Instalar o borrar datos de la app. 2. Abrir la app. | Spinner breve y luego `WelcomeScreen` con "Tu aventura comunitaria comienza aquí", botón "Iniciar Sesión" y enlace "Crear cuenta". (La spec original decía Login: desviación.) | | |
| MD-02 | TM-02 | 1. Desde Welcome tocar "Iniciar Sesión". 2. Botón atrás del sistema. 3. Tocar "Crear cuenta". 4. Atrás. 5. Desde Login tocar "¿Olvidaste tu contraseña?". 6. Atrás. | Login y Register vuelven a Welcome; RequestCode vuelve a Login. | | |

### Bloque B: Registro (HU-001)

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-11 | AR-01 | En Register, sin escribir nada, tocar "Registrarse". | Cada campo (nombre, apellido, correo, celular, fecha, contraseña, confirmación) muestra "Este campo es obligatorio"; no hay llamada a red. | | |
| MD-12 | AR-02 | Completar todo válido salvo apellido "   " (espacios). Tocar "Registrarse". | Apellido muestra "Este campo es obligatorio"; no se crea la cuenta. | | |
| MD-13 | AR-03 | Correo "juan@". | "Correo electrónico inválido". | | |
| MD-14 | AR-05 | Celular "2001234567" y luego "300123". | "Celular inválido: debe iniciar en 3 y tener 10 dígitos" en ambos. | | |
| MD-16 | AR-08, AR-09 | Contraseña "Ab1"; luego "abcdefg1", "ABCDEFG1" y "Abcdefgh". | Primero "La contraseña debe tener al menos 8 caracteres"; después "La contraseña debe incluir mayúscula, minúscula y número" en las tres. | | |
| MD-17 | AR-11 | Contraseña "Clave1234" y confirmación "clave1234". | "Las contraseñas no coinciden". | | |
| MD-18 | AR-12, AR-13 | Fecha de nacimiento que dé 17 años y 364 días (mañana hace 18 años). Intentar una fecha posterior a hoy. Elegir exactamente hoy menos 18 años. | Menor: "Debes ser mayor de 18 años". Futura: "La fecha de nacimiento no puede ser futura" (o el selector no la permite). Exactamente 18: sin error de fecha. | | |
| MD-15 | AR-04, AR-06, AR-19, AR-21 | Registrar U1 con correo " ana@example.com " (con espacios), celular "+57 300-123-4567", clave "Clave1234", mayor de 18. Tocar "Registrarse". En el dashboard revisar `auth.users`, `profiles` y `profile_private`. | Se crea la cuenta, se inicia sesión y se abre "Configuración inicial de perfil" (`perfil-inicial`). Perfil con nombre, apellido, celular "3001234567" y fecha de nacimiento; correo guardado sin espacios. Si ENV-1 no está aplicado: "Ocurrió un error. Intenta de nuevo" (bloqueado). | | |
| MD-19 | AR-15 | Cerrar sesión (MD-44) y registrar otra cuenta con "ana@example.com" y otro celular. | "El correo ya está en uso"; no se crea la cuenta. | | |
| MD-20 | AR-16 | Registrar con "ANA@Example.com" y otro celular. | "El correo ya está en uso". Si crea la cuenta: registrar hallazgo (la spec exige rechazo). | | |
| MD-21 | AR-17 | Registrar con correo nuevo y celular "3001234567" (de U1). | "El celular ya está en uso"; no se crea cuenta. | | |
| MD-22 | AR-18 | Prueba de carrera (solo si es viable): dos dispositivos, mismo celular nuevo y mismo instante; o forzar en SQL que el celular exista justo tras la RPC. | La segunda cuenta muestra "El celular ya está en uso" y no queda sesión iniciada (volver a Welcome sin sesión al reabrir). Si no es reproducible, registrar como "No ejecutable". | | |
| MD-23 | AR-20 | Activar modo avión, llenar el formulario válido y tocar "Registrarse". | "Sin conexión. Intenta de nuevo"; los campos conservan lo digitado. | | |
| MD-24 | AR-22 | Con red lenta (o throttling), tocar "Registrarse" dos veces seguidas. | El botón se deshabilita con indicador de progreso; solo se crea una cuenta/solicitud. | | |

### Bloque C: Cierre de sesión y navegación protegida (comportamientos del equipo)

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-44 | TM-05 | Con sesión (tras MD-15) ir a Menú principal > Ajustes > "Cerrar sesión". | Se vuelve a Welcome con el aviso "Sesión cerrada correctamente". | | |
| MD-45 | TM-06 | Tras MD-44 tocar "Iniciar Sesión". | El formulario de Login aparece vacío (sin clave previa). | | |
| MD-43 | TM-04 | Con la sesión cerrada, intentar llegar a una pantalla protegida (por ejemplo, abrir el menú con sesión caducada o reabrir la app desde recientes tras cerrar sesión desde otro dispositivo; si no es alcanzable, anotar "No ejecutable" y proponer prueba instrumentada). | Se redirige a Welcome con el aviso "Debes iniciar sesión para ingresar a la aplicación". | | |

### Bloque D: Login (HU-002)

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-31 | AL-01 | En Login tocar "Iniciar Sesión" sin datos. | "Este campo es obligatorio" en identificador y contraseña; sin llamada a red. | | |
| MD-32 | AL-05 | "ana@example.com" con clave incorrecta. | "Credenciales incorrectas"; permanece en Login. | | |
| MD-33 | AL-06 | "3001234567" con clave incorrecta (1 intento; no repetir 5 veces todavía). | Exactamente "Credenciales incorrectas". | | |
| MD-34 | AL-07 | "x@example.com" (inexistente) con cualquier clave. | El mismo "Credenciales incorrectas". | | |
| MD-34b | AL-08 | "3109999999" (sin cuenta). | "Credenciales incorrectas", misma apariencia que MD-33. | | |
| MD-35 | AL-02 | "ana@example.com" con la clave correcta. | Inicia sesión y abre el Menú principal. | | |
| MD-36 | AL-03, AL-04, TM-07 | Cerrar sesión (MD-44). Login con "3001234567" y clave correcta. Repetir con "300 123 4567" y con "+57 300-123-4567". | Inicia sesión y abre el Menú principal en los tres casos. | | |
| MD-37 | AL-10 | Con sesión iniciada, matar la app (deslizar desde recientes) y reabrirla. | Abre directamente el Menú principal sin pedir credenciales. | | |
| MD-46 | TM-08, AL-10 | Con sesión guardada, activar modo avión, matar y reabrir la app. Después cerrar sesión, modo avión, reabrir. | Con sesión guardada: Menú principal pese a no tener red. Sin sesión: Welcome. | | |
| MD-40 | AL-17 | Modo avión; intentar login con credenciales correctas. | "Sin conexión. Intenta de nuevo" (no "Credenciales incorrectas"). | | |
| MD-41 | AL-18 | Solo si es viable (función no desplegada o respondiendo 5xx): login por celular. | "Ocurrió un error. Intenta de nuevo". Si no es reproducible, "No ejecutable" (cubierto por unit). | | |
| MD-42 | AL-19 | Con red lenta tocar "Iniciar Sesión" dos veces. | Botón deshabilitado; una sola solicitud. | | |

### Bloque E: Recuperación (HU-003, HU-004)

Usar U2 con correo real para los pasos con código.

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-50 | AC-02, AC-03 | En Login tocar "¿Olvidaste tu contraseña?". Tocar "Enviar Código" con el campo vacío. Luego escribir "pablo@" y tocar "Enviar Código". | Vacío: "Este campo es obligatorio". Inválido: "Correo electrónico inválido". Sin llamada a red. | | |
| MD-51 | AC-01 | Escribir el correo real de U2 y tocar "Enviar Código". | Pasa a la pantalla de ingreso del código con el mensaje "Si el correo está registrado, recibirás un código"; llega un correo con código de 6 dígitos. | | |
| MD-52 | AC-04 | Repetir con "nadie@example.com" (sin cuenta). | Mismo mensaje y misma pantalla; no llega correo. | | |
| MD-53 | AC-05 | En la pantalla de código observar el botón de reenvío. | Texto "Reenviar código en N s" deshabilitado con cuenta regresiva desde 60; al llegar a 0 se habilita "Reenviar código" y llega un código nuevo al pulsarlo. | | |
| MD-54 | AC-07 | Ingresar "111111", clave "Clave5678" y confirmación igual, tocar "Guardar Nueva Contraseña". | "Código inválido o expirado"; la clave no cambia; los campos conservan el valor. | | |
| MD-55 | AC-06 | Con un código ya expirado (esperar la expiración configurada o pedir uno corto), ingresarlo con clave válida. | "Código inválido o expirado"; la clave no cambia. | | |
| MD-56 | AC-08 | Código vacío y luego "84a201". | "Ingresa el código numérico"; sin llamada a red. | | |
| MD-57 | AC-09 | Código válido, clave "abc123". | Mensaje de contraseña débil (hoy "La contraseña debe tener al menos 8 caracteres"); no cambia la clave. | | |
| MD-58 | AC-10 | Clave "Clave1234" y confirmación "Clave12345". | "Las contraseñas no coinciden". | | |
| MD-59 | AC-11, TM-09 | Código vigente, clave "Clave5678" y confirmación igual; guardar. Luego iniciar sesión con la nueva y con la anterior. | Vuelve a Login con el aviso "Contraseña actualizada. Inicia sesión con tu nueva contraseña"; la nueva clave inicia sesión; la anterior muestra "Credenciales incorrectas". | | |
| MD-60 | AC-12 | Modo avión, correo válido, "Enviar Código". | "Sin conexión. Intenta de nuevo"; se permanece en la pantalla. | | |
| MD-61 | AC-14 | Con red lenta tocar dos veces "Enviar Código" y luego dos veces "Guardar Nueva Contraseña". | Botones deshabilitados durante la solicitud; una sola solicitud en cada pantalla. | | |
| MD-62 | AX-01 | Pedir código, ingresarlo correcto con clave débil en servidor o forzando fallo de red al guardar para quedar con el código verificado; luego pulsar atrás (botón de la pantalla y botón del sistema). Comprobar que la app no queda con sesión activa (reabrirla). | Al abandonar tras verificar el código se cierra la sesión temporal: la app muestra Welcome/Login y NO abre el Menú principal. | | |

### Bloque F: Bloqueo por intentos (al final)

| ID | Escenarios | Pasos | Resultado esperado | Resultado | Observaciones |
|----|-----------|-------|--------------------|-----------|---------------|
| MD-38 | AL-12, AL-14 | Con U1: 5 intentos de login por celular con clave incorrecta; 6.º intento con la clave CORRECTA. | El 6.º intento muestra exactamente "Credenciales incorrectas", con la misma apariencia que un fallo normal; no inicia sesión ni revela bloqueo. | | |
| MD-39 | AL-16 | Con el celular de U1 aún bloqueado, iniciar sesión con su correo y clave correcta. | Inicia sesión y abre el Menú principal. | | |

## Pruebas de la Edge Function (curl)

Procedimiento completo en `supabase/functions/login-telefono/README.md` (sección "Pruebas manuales con curl", tarea 2.6). Definir `URL`, `ANON` y la función `call` como indica el README. Ejecutar con U1 y ENV-1/ENV-2/ENV-3 aplicados. Cada fila "Resultado" queda para el tester.

| ID | Escenarios | Paso del README | Comando / acción | Resultado esperado | Resultado |
|----|-----------|-----------------|------------------|--------------------|-----------|
| CU-01 | AL-03, AL-09, TM-07 | 1. Éxito | `call 3001234567 '<clave correcta>'` | HTTP 200 con `access_token`, `refresh_token`, `expires_in`, `expires_at`, `token_type`; sin `user` ni correo. | |
| CU-02 | AL-06, AL-09, AL-14 | 2. Clave incorrecta | `call 3001234567 'mala'` | HTTP 401 `{"error":"credenciales_invalidas"}`. | |
| CU-03 | AL-08, AL-09 | 3. Celular sin cuenta | `call 3109999999 'cualquiera'` | HTTP 401 con cuerpo EXACTAMENTE igual al de CU-02. | |
| CU-04 | AL-04 | 4. Formato libre | `call '300 123 4567' '<clave correcta>'` y `call '+57 300-123-4567' ...` | HTTP 200 en ambos. | |
| CU-05 | AL-09, AL-12, AL-14 | 5. Bloqueo | Repetir CU-02 cinco veces; 6.º intento con la clave CORRECTA | HTTP 429 con el mismo cuerpo genérico, sin sesión. | |
| CU-06 | AL-13 | 6. Bloqueo por IP | Desde la misma IP, 5 fallos con celulares distintos y luego un celular nuevo | HTTP 429. | |
| CU-07 | AL-16 | 7. Login por correo no afectado | Con el celular bloqueado, login en la app con el correo (MD-39) | Inicia sesión. | |
| CU-08 | AL-15 | 8. Ventana vencida | SQL: `update public.login_attempts set attempted_at = now() - interval '16 minutes' where phone = '3001234567';` y luego `call 3001234567 '<clave correcta>'` | HTTP 200 y los fallos de ese celular quedan borrados. | |
| CU-09 | AL-09 | 9. Entrada inválida | `call 12345 x` y un cuerpo que no sea JSON | HTTP 400 con el mismo cuerpo genérico. | |
| CU-10 | (hardening) | 10. CORS | `curl -i -X OPTIONS "$URL"` | HTTP 204 con cabeceras `Access-Control-Allow-*`. | |

Orden recomendado: CU-01, CU-02, CU-03, CU-04, CU-09, CU-10, luego CU-05, CU-06, CU-07 y CU-08 (los bloqueos afectan a MD-33 y MD-36 si se ejecutan antes).

## Resumen

Definiciones: "Automatizado" = existe al menos una prueba unitaria que afirma el comportamiento principal del escenario (puede requerir además verificación manual de UI o entorno). "Solo manual/curl" = ninguna prueba unitaria cubre el comportamiento central. "Gap" = escenario sin cobertura automatizada del comportamiento central. "Desviación" = el código actual difiere del texto de la spec por cambios del equipo.

| Capacidad | Escenarios | Automatizados | Solo manual/curl | Gaps | Desviaciones | Pruebas manuales/curl asociadas |
|-----------|-----------:|--------------:|-----------------:|-----:|-------------:|--------------------------------|
| auth-registro (HU-001) | 22 | 21 | 1 (AR-16) | 1 | 0 | 16 con paso manual/curl propio (AR-04 y AR-06 de forma indirecta vía MD-15) |
| auth-login (HU-002) | 19 | 13 | 6 (AL-09, AL-12, AL-13, AL-14, AL-15, AL-16) | 6 | 1 (AL-11) | 18 (todos menos AL-18) |
| auth-recuperación (HU-003, HU-004) | 14 | 14 | 0 | 0 | 0 | 13 (todos menos AC-13) |
| **Total** | **55** | **48** | **7** | **7** | **1** | |

Estado de ejecución: pruebas unitarias 187/187 pasan (154 de autenticación); `ktlintCheck`, `detekt`, `assembleDebug` OK. Pruebas manuales: 0 ejecutadas, todas ⬜ Pendiente; las que dependen de ENV-1/ENV-2/ENV-3 están bloqueadas por el entorno.

### Gaps (sin cobertura automatizada)

1. AR-16: correo duplicado con otras mayúsculas (depende de GoTrue; no hay prueba).
2. AL-09: correo no expuesto por la Edge Function (solo curl y revisión de código).
3. AL-12, AL-13, AL-14, AL-15, AL-16: toda la lógica de límite de intentos vive en `login-telefono` (TypeScript) sin pruebas Deno ni de integración; solo está cubierto el mapeo del lado cliente (429 como "Credenciales incorrectas").
4. Comportamientos del equipo sin prueba: redirección de rutas protegidas y avisos en `AppNavigation` (TM-02, TM-04, TM-09), `AuthRepositoryImpl.importPhoneSession` (TM-07) y `observeSession` con `NetworkError` (TM-08).
5. `AuthRepositoryImpl` completo (signIn, signUp, `isPhoneAvailable`, `verifyRecoveryCode`, `updatePassword`) sin pruebas: toda la capa de datos se valida solo en dispositivo.
6. Pantallas Compose (botón deshabilitado con indicador, mensajes mostrados, navegación a `perfil-inicial` y a `menu-principal`) sin pruebas de UI; solo verificación manual.

### Desviaciones

1. AL-11 / TM-01: sin sesión la app muestra Welcome, no Login; Back desde Login/Register vuelve a Welcome (TM-02). Actualizar `specs/auth-login/spec.md` (escenario "Sin sesión") y el diagrama de navegación en `design.md`. El test `SessionViewModelTest.goesToLoginWhenThereIsNoSession` conserva el nombre `LOGIN` aunque el destino real es Welcome; conviene renombrar `StartDestination.LOGIN` o aclararlo en KDoc.
2. Specs sin actualizar para el comportamiento nuevo: cierre de sesión desde Ajustes, redirección de rutas protegidas con avisos, y el aviso "Contraseña actualizada..." tras recuperar la clave.
3. Nota de la spec de recuperación: AC-09 espera "el mensaje de contraseña débil"; la implementación muestra el mensaje de longitud mínima para "abc123" (coherente con AR-08/AR-09, no una desviación funcional).

### Recomendaciones

1. Aplicar ENV-1 y ENV-2 en el Supabase compartido (tarea 0.15) y desplegar la función (2.7) antes de la sesión de QA; sin ello AR-15 a AR-19 y todo AL por celular quedan bloqueados.
2. Ejecutar la checklist manual (bloques A a F) y la lista curl, anotar resultados y marcar las tareas 2.6, 5.1 y las verificaciones manuales de 1.8, 2.8, 3.5 y 4.6 en `tasks.md`.
3. Añadir pruebas unitarias para `AuthRepositoryImpl` (mock de `SupabaseClient`/Ktor `MockEngine`) cubriendo `importPhoneSession`, `observeSession` con `NetworkError` y el mapeo de errores de `signUp`.
4. Extraer `shouldRedirectToWelcome` y `resolveInitialDestination` de `AppNavigation.kt` a funciones `internal` y probarlas (TM-04), o añadir una prueba de UI con `ComposeTestRule`.
5. Añadir pruebas Deno para `login-telefono` (límite por celular/IP, ventana, cuerpos idénticos) o un script de integración que automatice CU-01 a CU-09.
6. Actualizar las specs según las desviaciones, y añadir un requisito para logout y rutas protegidas antes de `/sdd-verify` y archivo del cambio.
7. Confirmar con el equipo la caducidad de OTP (900 s según el checklist de dashboard) para dimensionar MD-55.
