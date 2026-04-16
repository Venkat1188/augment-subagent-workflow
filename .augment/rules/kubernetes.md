# Kubernetes Code Review Guidelines
### Standards for K8s security, reliability, and resource management

**Description:** Guidelines for reviewing Kubernetes manifests and Helm charts to ensure secure, resilient, and optimized cluster deployments.  
**Applicable Files:** `**/k8s/*.yaml`, `**/manifests/*.yaml`, `**/helm/*.yaml`, `**/charts/*.yaml`, `**/kustomization.yaml`.

---

## 🛡️ Security

### [use-pod-security-standards] - Severity: High
Implement a `securityContext` for all pods.
- **Requirements:** Set `runAsNonRoot: true`, `readOnlyRootFilesystem: true`, and `allowPrivilegeEscalation: false`.
- Always **drop all capabilities** and add back only the specific Linux capabilities needed.

### [implement-rbac-least-privilege] - Severity: High
Follow the principle of least privilege for RBAC.
- Avoid `ClusterRoleBindings` if a namespaced `RoleBinding` suffices.
- Never grant `cluster-admin` to application workloads.
- Create dedicated `ServiceAccounts` per workload; never use the `default` service account.

### [implement-network-policies] - Severity: High
By default, Kubernetes allows all pod-to-pod communication.
- Define `NetworkPolicies` to **deny all** ingress/egress by default.
- Explicitly allow only required traffic between tiers (e.g., Backend → DB).

### [encrypt-secrets-at-rest] - Severity: High
Never commit unencrypted secrets to Git.
- Use external secret management like **Vault**, **AWS Secrets Manager**, or **Sealed Secrets**.
- Prefer mounting secrets as files rather than exposing them as environment variables to prevent accidental leakage in logs.

---

## ⚡ Resource Management

### [set-resource-requests-limits] - Severity: High
Always define CPU and memory `requests` and `limits`.
- **Requests:** Ensure the scheduler finds a node with enough space.
- **Limits:** Prevent a single pod from exhausting node resources (noisy neighbour).
- **Pro Tip:** For production Java workloads, set requests equal to limits to ensure the **Guaranteed QoS** class.

### [configure-hpa-and-pdb] - Severity: Medium
- **Autoscaling:** Use `HorizontalPodAutoscaler` (HPA) with meaningful metrics. Set `minReplicas >= 2`.
- **Disruption:** Define `PodDisruptionBudgets` (PDB) to ensure a minimum number of replicas remain available during cluster upgrades or node maintenance.

---

## 🏗️ Reliability & Availability

### [configure-probes-correctly] - Severity: High
- **LivenessProbe:** Detects if the container needs a restart.
- **ReadinessProbe:** Detects if the container is ready to receive traffic (used by Services).
- **StartupProbe:** Use for slow-starting Java apps to prevent liveness checks from killing the container during boot.

### [implement-anti-affinity] - Severity: Medium
Use `podAntiAffinity` or `topologySpreadConstraints` to ensure replicas are spread across different nodes or availability zones. This prevents a single node failure from taking down your entire service.

---

## 🌐 Networking & Configuration

### [use-internal-dns] - Severity: Low
Reference services by their DNS name (e.g., `my-service.my-namespace.svc.cluster.local`) rather than IP addresses. This ensures portability and resilience as pods are rescheduled.

### [use-configmaps-for-config] - Severity: Low
Store non-sensitive configuration in `ConfigMaps`. Use **immutable ConfigMaps** where possible to prevent accidental configuration drift and improve performance.

### [apply-standard-labels] - Severity: Low
Use recommended labels (e.g., `app.kubernetes.io/name`, `app.kubernetes.io/version`, `app.kubernetes.io/component`) for better observability and integration with monitoring tools like Prometheus.
