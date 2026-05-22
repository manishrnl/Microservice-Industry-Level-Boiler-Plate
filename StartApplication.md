## Eureka user name and password when prompted at http://localhost:8761
```bash
    EUREKA_USER=eureka
    EUREKA_PASSWORD=eureka_password
```    

## Kill Port if it is being occupied

```bash
    npx kill-port 5173 -Y
```


## Run Docker in detach mode ( NO LOGS VISIBLE IN CONSOLE )
```bash
    docker-compose up -d --build
```


## Run docker ( ALL LOGS ARE VISIBLE )
```bash
    docker-compose up --build
```