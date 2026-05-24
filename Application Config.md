## Kill Port if it is being occupied

```bash
    npx kill-port 5173 -Y
```


## Start / Run Backend ( docker )  ( ALL LOGS ARE VISIBLE )
```bash
   docker-compose down && cls && docker-compose up --build
```

### Start frontend
```bash

    cd .\frontend\ && npm run build  && cls && npm run dev

```

## services user name and password when prompted at http://localhost:8761 
```bash

    EUREKA_USER=eureka
    EUREKA_PASSWORD=eureka_password

    MINIO_ROOT_USER=platform
    MINIO_ROOT_PASSWORD=platform_password

    OBSERVABILITY_USERNAME=admin
    OBSERVABILITY_PASSWORD=observability_admin

```    
