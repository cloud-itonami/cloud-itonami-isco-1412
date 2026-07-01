# cloud-itonami-isco-1412

Open Occupation Blueprint for **ISCO-08 1412**: Restaurant Managers.

This repository designs a forkable OSS business for an independent restaurant manager: a kitchen-walkthrough robot performs food-safety checklist inspection under a governor-gated actor, so the practice keeps its own staffing and safety records instead of renting a closed restaurant-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a kitchen-walkthrough robot performs food-safety checklist inspection and equipment-temperature checks under an actor that proposes
actions and an independent **Restaurant Management Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near open flame or hot surfaces, or clearing a food-safety hold) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
staffing plan + food-safety checklist + supplier order
        |
        v
Restaurant Management Advisor -> Restaurant Management Governor -> staff/review, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `1412`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
