# Cryptography Security Code Review Guidelines
### Standards for algorithms, random numbers, key management, and TLS/SSL

**Description:** Guidelines for cryptographic implementation covering algorithms, key management, and secure communication protocols.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, etc.) and files containing `*crypto*`, `*cipher*`, `*encrypt*`, `*hash*`, `*ssl*`, `*tls*`, or `*cert*`.

---

## 🛡️ Algorithm Standards

### [use-approved-algorithms] - Severity: Critical
Only use NIST-approved or industry-standard algorithms:
- **Symmetric:** AES-256-GCM (Preferred for Authenticated Encryption).
- **Hashing:** SHA-256, SHA-512, or SHA-3.
- **Asymmetric:** RSA-3072+ or ECDSA (P-256 or higher).
- **Key Derivation (Passwords):** Argon2 (Preferred), bcrypt, or PBKDF2.

### [avoid-deprecated-algorithms] - Severity: Critical
**Never** use broken or weak algorithms:
- **Broken:** MD5, SHA-1, RC4.
- **Weak:** DES, 3DES, Blowfish (small block size).
- **Insecure Modes:** Never use **ECB** mode for symmetric encryption.

### [avoid-custom-cryptography] - Severity: Critical
**Never implement custom cryptographic algorithms or protocols.**
- Use well-vetted libraries: **BouncyCastle** or **JCA** (Java), **Web Crypto API** (JS/TS), or **libsodium**.
- Even small implementation errors can lead to total security failure.

---

## 🎲 Random Numbers & Entropy

### [use-csprng] - Severity: Critical
Always use **Cryptographically Secure Pseudo-Random Number Generators (CSPRNG)** for security values (keys, tokens, IVs).
- **Java:** `java.security.SecureRandom`.
- **Node.js/TS:** `crypto.randomBytes()`.
- **Browser:** `window.crypto.getRandomValues()`.
- **❌ Avoid:** `Math.random()`, `java.util.Random`, or timestamp-based seeds.

### [sufficient-random-length] - Severity: High
- **Session IDs/Tokens:** Minimum 128 bits of entropy.
- **API Keys:** Minimum 256 bits.
- **Nonces/IVs:** Follow the specific algorithm requirements (e.g., 96-bit for AES-GCM).

---

## 🔑 Key Management

### [secure-key-storage] - Severity: Critical
- **Production:** Use a **Key Management Service (KMS)** (AWS KMS, HashiCorp Vault, Azure Key Vault) or HSM.
- **Separation:** Never store encryption keys in the same database or file system as the encrypted data.
- **Secrets:** Never hardcode keys in source code. Reference them via Dapr Secret Stores or K8s Secrets.

### [implement-key-rotation] - Severity: High
Implement automated key rotation. Ensure the application can handle "overlap" periods where multiple keys are active (new key for encryption, old key for decryption of legacy data).

---

## 🌐 TLS/SSL & Transit Security

### [enforce-tls-version] - Severity: High
- **Minimum:** Require **TLS 1.2**.
- **Preferred:** **TLS 1.3**.
- **Disabled:** Explicitly disable SSL 3.0, TLS 1.0, and TLS 1.1 in your server/ingress configurations.

### [implement-certificate-validation] - Severity: Critical
**Never disable certificate validation in production.**
- Always verify the chain of trust, expiration, and hostname.
- Use **HSTS (HTTP Strict Transport Security)** headers to prevent SSL-stripping attacks.

### [configure-secure-cipher-suites] - Severity: High
Prioritize **AEAD** ciphers (AES-GCM, ChaCha20-Poly1305) and ensure **Forward Secrecy (ECDHE)** is enabled to protect past sessions if a long-term key is compromised.

---

## 📝 Implementation Patterns

### Authenticated Encryption (Java Example)
```java
// ✅ GOOD: Using AES-GCM for combined Confidentiality and Integrity
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit authentication tag
cipher.init(Cipher.ENCRYPT_MODE, key, spec);
