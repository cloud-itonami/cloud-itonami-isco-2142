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
21 tests / 60 assertions green.

The structural HARD invariants — arithmetic, not engineering
judgement:

1. **Load-capacity margin** — utilization = load / registered
   allowable-capacity must not exceed 1.0.
2. **Material grade membership** — the proposed grade must be a member
   of the structure's registered approved-grades set. This checks the
   proposal against the set; it does **not** check the set, which the
   operator supplies.
3. **Statutory ceilings** (`civileng.facts`) — where the Order fixes a
   number, the operator's *registration* is checked against it too:
   a declared ground type must be a row of the Article 93 table and its
   registered long-term bearing pressure must not exceed that row's
   value; a declared reinforcement type must be one Article 90 table 2
   names and the proposed long-term stress must not exceed its ceiling.
   Declaring a ground type elects the table the article's proviso
   permits — having elected it, the value is no longer a choice. A
   structure that declares neither is on the survey route and is not
   refused here.

Also HARD: unregistered/foreign structure, unregistered organization,
non-`:propose` effect.

Statutory values are ingested from the primary source rather than
recalled — 建築基準法施行令 (Cabinet Order No. 338 of 1950), Articles 90
and 93, read from
<https://elaws.e-gov.go.jp/api/1/lawdata/325CO0000000338>. That endpoint
serves the law as XML and answers 404 for an unknown law id, so a
successful fetch is evidence about this law; the human permalink
<https://laws.e-gov.go.jp/law/325CO0000000338> renders client-side and
is an address, not evidence.

Escalations (always human sign-off): `:approve-occupancy` (issuing an
occupancy/safety certificate), low confidence (< 0.6).

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
