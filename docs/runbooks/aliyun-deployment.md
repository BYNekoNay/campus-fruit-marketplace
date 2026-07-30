# Alibaba Cloud ECS Deployment

`Deploy Production` is intentionally manual. It deploys an immutable image tag to the GitHub `production` environment and cannot deploy `latest`.

## One-time server setup

The ECS instance needs Docker Engine, the Docker Compose plugin, and an account that can run `docker` without `sudo`.

Create the deployment directory and create the server-only environment file. The deployment workflow signs the ECS Docker daemon into ACR before each pull, so do not put ACR credentials in this repository or the server `.env` file.

```bash
mkdir -p /opt/campus-fruit
```

After the first manifest upload, create the server-only environment file and replace every placeholder:

```bash
cp /opt/campus-fruit/deploy/compose/.env.example /opt/campus-fruit/.env
chmod 600 /opt/campus-fruit/.env
```

The workflow checks for the required values and does not upload or overwrite this file.

## GitHub production environment

Create a `production` environment in the repository settings and configure required reviewers before enabling deployments. Add these environment secrets:

| Secret | Value |
| --- | --- |
| `ECS_HOST` | ECS public IP address or DNS name |
| `ECS_USER` | SSH deployment user |
| `ECS_SSH_KEY` | Private key for the deployment user |
| `ECS_SSH_KNOWN_HOSTS` | The exact host-key line from the ECS server's `known_hosts` file |
| `ALIYUN_ACR_REGISTRY` | ACR registry endpoint, for example `registry.cn-hangzhou.aliyuncs.com` |
| `ALIYUN_ACR_USERNAME` | ACR access username |
| `ALIYUN_ACR_PASSWORD` | ACR access password or access token |

Set the optional environment variable `PRODUCTION_URL` to the public application URL so GitHub can link to the deployed site.

Set the repository Actions variable `ALIYUN_ACR_NAMESPACE` to the ACR namespace. `CD Build & Push` uses the same registry credentials to publish immutable SHA tags.

Obtain the host-key line from a trusted administrator or the ECS console, rather than accepting it during a deployment run.

## Deploy and roll back

Open Actions, select `Deploy Production`, and provide the full 40-character commit SHA that completed `CD Build & Push`. The workflow uploads only the deployment manifests, pulls the matching images, restarts the Compose stack, and checks gateway health.

To roll back, dispatch the same workflow with a previous successful commit SHA. The service images are tagged with every commit SHA, so rollback does not depend on the mutable `latest` tag.
