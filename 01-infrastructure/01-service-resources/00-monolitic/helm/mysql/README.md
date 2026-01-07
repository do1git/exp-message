# MySQL Helm Chart

MySQL database chart for monolithic service.

## Installation

### Default Installation
```bash
helm install mysql ./mysql
```

### Development Environment
```bash
# Note: When used as a subchart, use the umbrella chart's values-dev.yaml
# For standalone installation, override values directly:
helm install mysql . --set mysql.rootPassword=dev_password
```

### Production Environment
```bash
# Note: When used as a subchart, use the umbrella chart's values-prod.yaml
# For standalone installation, override values directly:
helm install mysql . --set mysql.rootPassword=secure-password
```

### Custom Values
```bash
helm install mysql ./mysql --set mysql.rootPassword=mysecretpassword
```

### With Namespace
```bash
helm install mysql ./mysql --namespace production --create-namespace
```

## Upgrade

```bash
helm upgrade mysql ./mysql
```

## Rollback

```bash
# View release history
helm history mysql

# Rollback to previous version
helm rollback mysql

# Rollback to specific revision
helm rollback mysql 1
```

## Uninstall

```bash
helm uninstall mysql
```

## Values

See `values.yaml` for all available configuration options.

### Key Configuration Options

- `mysql.rootPassword`: MySQL root password
- `mysql.database`: Database name
- `mysql.user`: MySQL user
- `mysql.password`: MySQL user password
- `persistence.size`: Storage size for PVC
- `resources`: CPU and memory limits/requests
- `replicas`: Number of MySQL replicas

## External Access

### NodePort (Local Development)
```bash
helm install mysql ./mysql \
  --set service.type=NodePort \
  --set service.nodePort=30306
```

### LoadBalancer (Cloud)
```bash
helm install mysql ./mysql --set service.type=LoadBalancer
```

### Port Forwarding (Debugging)
```bash
kubectl port-forward svc/mysql 3306:3306
```

## Examples

### Install with custom storage size
```bash
helm install mysql ./mysql --set persistence.size=20Gi
```

### Install with custom resources
```bash
helm install mysql ./mysql \
  --set resources.requests.memory=512Mi \
  --set resources.limits.memory=1Gi
```

### Install with external access (NodePort)
```bash
helm install mysql ./mysql \
  --set service.type=NodePort \
  --set service.nodePort=30306
```

### Install with multiple value files
```bash
helm install mysql ./mysql -f custom-overrides.yaml
```

