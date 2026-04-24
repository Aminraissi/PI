# Explorer 3D Microservice

This service hosts the `folio-2019-green` build as static content and registers to Eureka as `explorer-service`.
It is exposed through Gateway at:

- `http://localhost:8089/explorer/`

## 1) Build the 3D app

```bash
cd /home/amine/Desktop/1/folio-2019-green/folio-2019
npm install
npm run build
```

## 2) Copy build output into the microservice static folder

```bash
rm -rf /home/amine/Desktop/1/pi-cloud-Explorer/Explorer/src/main/resources/static/*
cp -r /home/amine/Desktop/1/folio-2019-green/folio-2019/dist/* /home/amine/Desktop/1/pi-cloud-Explorer/Explorer/src/main/resources/static/
```

## 3) Run services

Start in this order:

1. Eureka (`:8761`)
2. Gateway (`:8089`)
3. Explorer (`:8087`)

```bash
cd /home/amine/Desktop/1/pi-cloud-Explorer/Explorer
./mvnw spring-boot:run
```

Then open:

- `http://localhost:8089/explorer/`

