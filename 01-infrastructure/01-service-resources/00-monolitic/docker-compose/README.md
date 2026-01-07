# Docker Compose Generation Script

Script to automatically generate `docker-compose.yml` file from `.env` file.

## Usage

### Linux/Mac

```bash
cd docker-compose
chmod +x generate-docker-compose.sh
./generate-docker-compose.sh
```

### Windows (PowerShell)

```powershell
cd docker-compose
.\generate-docker-compose.ps1
```

## How It Works

1. Reads `../.env` file and loads environment variables.
2. Creates required directories (`mysql/conf.d`, `mysql/init`) if they don't exist.
3. Creates default `my.cnf` file if it doesn't exist.
4. Generates `docker-compose.yml` file using environment variable values as defaults.

## Generated Files

The script generates the following structure:

```
docker-compose/
├── docker-compose.yml
└── mysql/
    ├── conf.d/
    │   └── my.cnf          # Default MySQL configuration (with binlog)
    └── init/
        └── .gitkeep        # Keep directory in Git
```

## Example

```bash
# Generate docker-compose.yml
./generate-docker-compose.sh

# Use generated docker-compose.yml
docker compose up -d
```

## Notes

- The script will fail if `.env` file doesn't exist.
- `.env` file is excluded from version control (in `.gitignore`).
- Create `.env` file from `.env.example` if needed.
