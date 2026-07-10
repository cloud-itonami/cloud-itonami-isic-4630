# Contributing

`cloud-itonami-isic-4630` accepts contributions to the OSS blueprint, the
Provision Trading Governor, decision-rule tests, documentation and operator
model.

## Development
The capability layer is SELF-CONTAINED. There is no pre-existing bespoke
food/beverage/tobacco wholesale capability library to wrap; the counterparty-
credit / contract-on-file / food-safety-certificate / alcohol-excise-license /
tobacco-excise-registration / age-verification / sanctions-screening checks
live directly in `provisiontrade.governor`. This repo holds the business
blueprint, the langgraph-clj actor and the operator contracts.

```bash
clojure -M:dev:test
clojure -M:lint
```

## Rules
- Do not commit real counterparty, credit, food-safety/alcohol-excise/
  tobacco-excise certification or sanctions-screening data.
- Keep delivery dispatch and invoice settlement behind the Provision Trading
  Governor.
- Treat food/beverage/tobacco wholesale workflows as high-risk: add tests for
  spec-basis, evidence completeness, credit clearance, contract-on-file,
  food-safety-certificate verification, alcohol-excise-license verification,
  tobacco-excise-registration and age-verification, sanctions screening and
  audit logging.
- Never fabricate a jurisdiction's food-safety, alcohol-excise or tobacco-
  excise requirements in `provisiontrade.facts` -- cite a real official
  source or leave the jurisdiction (or the regulatory class) out of the
  catalog.
- Never blur the food-safety / alcohol-excise / tobacco-excise distinction: a
  processed-food or non-alcoholic-beverage consignment's
  `:food-safety-certificate?`, an alcoholic-beverage consignment's
  `:alcohol-excise-license?`, and a tobacco consignment's
  `:tobacco-excise-registration?`/`:age-verification-on-file?` are three
  different regulatory regimes, not interchangeable names for the same
  check. New code and docs must keep `provisiontrade.governor`'s
  `food-safety-certificate-missing-violations`,
  `alcohol-excise-license-missing-violations` and
  `tobacco-excise-age-verification-missing-violations` as separate checks,
  gated on `:consignment-category`.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which governor invariant is
affected, how it was tested, whether operator or certification docs need
updates.
