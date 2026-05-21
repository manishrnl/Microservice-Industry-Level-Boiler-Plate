# Config Repository Files

These are the per-service Spring Cloud Config files used for local fallback and as the template
for the Git-backed config repository.

- `application.yml`
- `api-gateway.yml`
- `auth-service.yml`
- `user-service.yml`
- `notification-service.yml`
- `payment-service.yml`
- `file-service.yml`
- `ai-service.yml`
- `audit-service.yml`

The running Config Server reads from `CONFIG_GIT_URI` and then loads this folder through
`CONFIG_GIT_SEARCH_PATHS`.

For this project, root `.env` uses:

- `CONFIG_GIT_URI=https://github.com/manishrnl/Microservice-Industry-Level-Boiler-Plate.git`
- `CONFIG_GIT_SEARCH_PATHS=backend/config-server-repo`

The GitHub browser URL for these files is:

`https://github.com/manishrnl/Microservice-Industry-Level-Boiler-Plate/tree/main/backend/config-server-repo`
