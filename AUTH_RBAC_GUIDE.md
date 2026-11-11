# Guía de Autenticación y Autorización Basada en Roles (RBAC) con JWT

Este documento detalla la arquitectura de seguridad implementada en el backend de Spring Boot 3 para el **Sistema Predictivo de Mantenimiento y Fallas Vehiculares**.

---

## 1. Roles del Sistema

- **`ROLE_DRIVER` (Conductor)**:
  - Registro de síntomas y diagnósticos predictivos (`POST /api/vehicles/diagnose`).
  - Registro y gestión de sus vehículos asociados (`GET /api/vehicles/fleet/my-vehicles`).
  - Consulta del historial exclusivo de sus placas asignadas (`GET /api/vehicles/history/{plate}`).

- **`ROLE_MECHANIC` (Mecánico de Taller)**:
  - Diagnóstico general de cualquier vehículo (`POST /api/vehicles/diagnose`).
  - Auditoría global de la flota completa y cualquier placa (`GET /api/vehicles/history/{plate}`).
  - Emisión y consulta de órdenes de taller (`POST /api/workshop-orders`).
  - Consulta general de flota (`GET /api/vehicles/fleet/all`).

---

## 2. Flujo de Autenticación JWT

1. El usuario envía sus credenciales a `POST /api/auth/login` o se registra en `POST /api/auth/register`.
2. El servidor valida las credenciales con BCrypt (`BCryptPasswordEncoder`).
3. Se genera un token JWT firmado mediante HMAC-SHA256 que incluye en sus claims:
   - `sub`: Correo electrónico.
   - `role`: Rol asignado (`ROLE_DRIVER` o `ROLE_MECHANIC`).
   - `fullName`: Nombre completo del operador.
   - `userId`: Identificador único de usuario.
4. El cliente incluye el token en la cabecera HTTP:
   ```http
   Authorization: Bearer <TOKEN_JWT>
   ```
5. `JwtFilter` valida la firma, extrae las autoridades y carga el `SecurityContextHolder`.

---

## 3. Endpoints y Reglas de Acceso

| Endpoint | Método | Rol Requerido | Descripción |
| :--- | :---: | :--- | :--- |
| `/api/auth/register` | `POST` | Público | Registro de nuevos usuarios con rol |
| `/api/auth/login` | `POST` | Público | Autenticación y obtención de JWT |
| `/api/auth/me` | `GET` | Autenticado | Perfil del usuario activo |
| `/api/vehicles/diagnose` | `POST` | `DRIVER`, `MECHANIC` | Diagnóstico con modelo ONNX |
| `/api/vehicles/history/{plate}` | `GET` | `DRIVER` (solo asignado) o `MECHANIC` | Histórico vehicular |
| `/api/workshop-orders` | `POST` | `MECHANIC` | Emisión de orden de taller |
| `/api/workshop-orders/plate/{plate}` | `GET` | `DRIVER`, `MECHANIC` | Órdenes por placa |
| `/api/vehicles/fleet/my-vehicles` | `GET` | `DRIVER` | Lista de vehículos del conductor |
| `/api/vehicles/fleet/all` | `GET` | `MECHANIC` | Auditoría de toda la flota |

