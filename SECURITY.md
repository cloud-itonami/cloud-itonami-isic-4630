# Security Policy

This project handles counterparty-credit, food-safety/alcohol-excise/
tobacco-excise certification and sanctions-screening workflows for food,
beverage and tobacco consignments. Treat vulnerabilities as potentially high
impact even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real counterparty, credit or food-safety/alcohol-excise/tobacco-excise
  certification data exposure
- authorization bypass
- Provision Trading Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
gftdcojp organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on counterparty data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real counterparty, credit, food-safety/alcohol-excise/tobacco-excise
  certification and sanctions-screening data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
