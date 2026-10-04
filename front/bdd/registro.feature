# language: es
Característica: Formulario de registro
  Escenario: Registro exitoso
    Dado que estoy en el formulario de "registro"
    Y la API acepta el registro
    Cuando completo el registro con correo "ana@example.com" y contraseña "password123"
    Y envío el formulario de "registro"
    Entonces veo el formulario de "login"
    Y veo el mensaje "Cuenta creada. Iniciá sesión para continuar."

  Escenario: Correo ya registrado
    Dado que estoy en el formulario de "registro"
    Y la API rechaza el registro por correo duplicado
    Cuando completo el registro con correo "ana@example.com" y contraseña "password123"
    Y envío el formulario de "registro"
    Entonces veo el error "Email already registered"
    Y veo el formulario de "registro"

  Escenario: Campos obligatorios vacíos
    Dado que estoy en el formulario de "registro"
    Cuando envío el formulario de "registro"
    Entonces el navegador marca un campo inválido
    Y no se envían solicitudes de autenticación
