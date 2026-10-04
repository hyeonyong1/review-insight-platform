# API Examples

## Python

### Health

```http
GET http://localhost:8001/health
```

### Analyze

```http
POST http://localhost:8001/analyze
Content-Type: application/json
```

```json
{
  "reviews": [
    {
      "id": 1,
      "text": "质量很差，而且客服一直不回复"
    }
  ]
}
```

---

## Java

### Upload

```http
POST /api/datasets/upload?name=demo
Content-Type: multipart/form-data
```

### Review page

```http
GET /api/datasets/{datasetId}/reviews?page=0&size=20
```

### Create task

```http
POST /api/analysis-tasks?datasetId=1
```

### Task status

```http
GET /api/analysis-tasks/{taskId}
```

### Result

```http
GET /api/analysis-tasks/{taskId}/result
```
