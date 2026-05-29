# Performance Baseline

This folder keeps the manual pre-release performance smoke check for M7.4.

## k6 baseline

```powershell
k6 run .\k6-baseline.js -e BASE_URL=http://localhost:8080 -e SCENARIO_ID=1 -e VUS=10 -e DURATION=2m
```

For authenticated admin APIs, pass a valid session cookie when needed:

```powershell
k6 run .\k6-baseline.js -e ADMIN_COOKIE="JSESSIONID=..." -e BASE_URL=http://localhost:8080
```

The script is not part of the Maven test gate. Use it before release or after performance-sensitive changes, then inspect `api.log` and `slow-query.log` for repeated slow paths.
