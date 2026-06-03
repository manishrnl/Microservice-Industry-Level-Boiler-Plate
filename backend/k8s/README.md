# Kubernetes deployment

These manifests run the backend stack on Kubernetes:

- Spring services: config server, discovery server, API gateway, auth, user, notification, payment, file, AI, audit.
- Infrastructure: Postgres, Redis, Kafka, MinIO.
- Autoscaling: HPA for the stateless Spring services.
- Public entry point: API gateway ingress only.

Kubernetes scales pods, not Docker images. Build and push images first, then the HPA duplicates pods from those images when CPU load rises.

## Build images

From the repository root:

```powershell
.\backend\k8s\build-images.ps1
```

For an internet cluster, push to a registry such as GHCR or Docker Hub:

```powershell
.\backend\k8s\build-images.ps1 -Registry ghcr.io/your-github-user -Push
```

Then replace `ghcr.io/your-github-user` in `overlays/production/kustomization.yaml` with your real registry path.

## Deploy locally or to a cluster

```powershell
kubectl apply -k backend/k8s/overlays/dev
```

Production:

```powershell
kubectl apply -k backend/k8s/overlays/production
```

Check rollout:

```powershell
kubectl -n microservice-platform get pods
kubectl -n microservice-platform get hpa
kubectl -n microservice-platform get ingress
```

## Required before production

Edit or override these values before public deployment:

- `overlays/production/kustomization.yaml`: image registry names.
- `overlays/production/config-patch.yaml`: backend URL, frontend URL, cookie settings.
- `base/secret.yaml`: passwords, API keys, OAuth secrets, Stripe keys, SMTP credentials.

You can replace the placeholder secret from your `.env` file:

```powershell
kubectl -n microservice-platform create secret generic platform-secrets --from-env-file=.env --dry-run=client -o yaml | kubectl apply -f -
```

## Autoscaling

HPA requires Kubernetes resource metrics. Verify metrics-server is available:

```powershell
kubectl top nodes
kubectl -n microservice-platform get hpa
```

If `kubectl top nodes` fails, install metrics-server for your cluster before expecting HPA to scale pods.

## Public access

The manifests expose only `api-gateway` through Kubernetes Ingress. Keep the React frontend on Netlify or Cloudflare Pages and set its API URL to the public gateway URL.

For a free VM-based cluster such as k3s, use one of these options for internet access:

- Point your domain to the VM and use the bundled ingress controller or nginx.
- Use Cloudflare Tunnel to avoid paying for a cloud load balancer.
