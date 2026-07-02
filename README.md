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

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341` and
`-1349`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/restaurant_management/store.cljc` — `Store` protocol +
  `MemStore`: registered locations, committed records, an append-only
  audit ledger.
- `src/restaurant_management/advisor.cljc` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a restaurant
  operation from a request; `llm-advisor` wraps a
  `langchain.model/ChatModel` — either way the advisor only ever
  produces a `:propose`-effect proposal, never a committed record, and
  LLM parse failures always yield `confidence 0.0` (forces escalation,
  never fabricated confidence).
- `src/restaurant_management/governor.cljc` —
  `RestaurantManagementGovernor/check`: a pure function, wired as its
  own `:govern` node. Hard invariants (unregistered location, a
  proposal whose `:effect` isn't `:propose`) always route to `:hold`.
  Escalation invariants (`:operate-near-flame`,
  `:clear-food-safety-hold`, or low advisor confidence) always route to
  `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that operating near open flame or hot surfaces and clearing a
  food-safety hold always require human sign-off.
- `src/restaurant_management/actor.cljc` — `build-graph`,
  `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring
  itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
