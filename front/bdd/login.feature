# language: es
Característica: Formulario de inicio de sesión
  Escenario: Login exitoso
    Dado que estoy en el formulario de "login"
    Y la API acepta el login
    Cuando completo el login con correo "ana@example.com" y contraseña "password123"
    Y envío el formulario de "login"
    Entonces veo la Home autenticada con el saludo "Hola, Ana"

  Escenario: Credenciales incorrectas
    Dado que estoy en el formulario de "login"
    Y la API rechaza el login por credenciales incorrectas
    Cuando completo el login con correo "ana@example.com" y contraseña "incorrecta123"
    Y envío el formulario de "login"
    Entonces veo el error "Invalid credentials"
    Y veo el formulario de "login"
    Y no veo la opción de cerrar sesión

  Escenario: Correo inválido
    Dado que estoy en el formulario de "login"
    Cuando completo el login con correo "correo-invalido" y contraseña "password123"
    Y envío el formulario de "login"
    Entonces el navegador marca un campo inválido
    Y no se envían solicitudes de autenticación
