# cloud-itonami-isco-2142

Open Business Blueprint for **ISCO-08 2142**: Civil Engineers — an ISCO
**Wave 1 (design & governance)** occupation per ADR-2607121000. This
is the SECOND wave-1 blueprint batch (21xx engineering design
professions): the design/analysis work is cognitive; physical
execution remains robotics-gated and out of the actor's scope.

**Maturity: `:implemented`** — CivilEngineersAdvisor ⊣
CivilEngineersGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
13 tests / 27 assertions green.

The structural HARD invariants — arithmetic, not engineering
judgement:

1. **Load-capacity margin** — utilization = load / registered
   allowable-capacity must not exceed 1.0.
2. **Material grade membership** — the proposed grade must be a member
   of the structure's registered approved-grades set (no invented or
   unapproved material).

Also HARD: unregistered/foreign structure, unregistered organization,
non-`:propose` effect. Escalations (always human sign-off):
`:approve-occupancy` (issuing an occupancy/safety certificate), low
confidence (< 0.6).

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
