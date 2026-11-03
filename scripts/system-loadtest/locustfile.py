from locust import HttpUser, constant, task


class DotaOpsHealthUser(HttpUser):
    """Low-rate load profile for the public backend health endpoint."""

    wait_time = constant(1)

    @task
    def check_backend_health(self):
        with self.client.get(
            "/api/health",
            name="GET /api/health",
            timeout=3,
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Expected HTTP 200, received {response.status_code}")
                return

            try:
                payload = response.json()
            except ValueError:
                response.failure("Response body was not valid JSON")
                return

            if payload.get("status") != "ok" or payload.get("service") != "dotaops-backend":
                response.failure("Health response did not report the expected service status")
